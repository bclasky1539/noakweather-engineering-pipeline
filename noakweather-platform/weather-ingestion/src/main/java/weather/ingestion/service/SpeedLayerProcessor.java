/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025-2026 bclasky1539
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package weather.ingestion.service;

import weather.model.ProcessingLayer;
import weather.model.WeatherData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Speed Layer Processor for Lambda Architecture.
 * <p>
 * REFACTORED to be completely generic - works with ANY weather data source.
 * <p>
 * Responsibilities (GENERIC ONLY):
 * 1. Enrich weather data with generic metadata
 * 2. Tag data with ProcessingLayer.SPEED_LAYER
 * 3. Upload to S3 for low-latency access
 * <p>
 * NOT responsible for:
 * - Source-specific validation (handled by orchestrators)
 * - Fetching data from sources (handled by clients)
 * - Business logic (handled by orchestrators)
 * <p>
 * The Speed Layer prioritizes:
 * - Low latency (seconds to minutes)
 * - Recent data only (last 24-48 hours)
 * - Minimal processing (validation, basic enrichment)
 * <p>
 * Think of this as the "express checkout lane" - it processes items quickly
 * without worrying about what specific items they are.
 *
 * @author bclasky1539
 *
 */
public class SpeedLayerProcessor {

    private static final Logger logger = LogManager.getLogger(SpeedLayerProcessor.class);

    private final S3UploadService s3Service;
    private final ExecutorService executorService;
    private final int maxConcurrentRequests;

    /**
     * Creates a SpeedLayerProcessor with default concurrency.
     *
     * @param s3Service the S3 upload service
     */
    public SpeedLayerProcessor(S3UploadService s3Service) {
        this(s3Service, 10); // Default: 10 concurrent uploads
    }

    /**
     * Creates a SpeedLayerProcessor with custom concurrency limit.
     *
     * @param s3Service the S3 upload service
     * @param maxConcurrentRequests maximum number of concurrent S3 uploads
     */
    public SpeedLayerProcessor(S3UploadService s3Service, int maxConcurrentRequests) {
        this.s3Service = s3Service;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.executorService = Executors.newFixedThreadPool(maxConcurrentRequests);

        logger.info("SpeedLayerProcessor initialized with {} concurrent threads",
                maxConcurrentRequests);
    }

    /**
     * Processes a single weather data record through the speed layer.
     * <p>
     * This is the core processing method - completely generic.
     * <p>
     * Flow:
     * 1. Enrich with generic metadata (timestamp, processor info)
     * 2. Tag with SPEED_LAYER
     * 3. Upload to S3
     * 4. Return processed data with S3 location
     *
     * @param weatherData the weather data to process (already validated by caller)
     * @return the processed WeatherData with S3 location added
     * @throws IOException if S3 upload fails
     */
    public WeatherData processWeatherData(WeatherData weatherData) throws IOException {
        if (weatherData == null) {
            throw new IOException("Weather data cannot be null");
        }

        logger.debug("Processing weather data for station: {}", weatherData.getStationId());

        Instant startTime = Instant.now();

        // Step 1: Enrich with generic metadata
        enrichWithMetadata(weatherData);

        // Step 2: Tag with Speed Layer
        weatherData.setProcessingLayer(ProcessingLayer.SPEED_LAYER);

        // Step 3: Upload to S3
        String s3Key = s3Service.uploadWeatherData(weatherData);
        weatherData.addMetadata("s3_key", s3Key);
        weatherData.addMetadata("processing_duration_ms",
                Duration.between(startTime, Instant.now()).toMillis());

        Duration duration = Duration.between(startTime, Instant.now());
        logger.info("Processed weather data for station {} in {}ms (S3: {})",
                weatherData.getStationId(), duration.toMillis(), s3Key);

        return weatherData;
    }

    /**
     * Processes multiple weather data records in parallel.
     * <p>
     * More efficient than processing one at a time when you have multiple
     * records already fetched and validated.
     *
     * @param weatherDataList list of weather data to process
     * @return list of successfully processed WeatherData
     */
    public List<WeatherData> processWeatherDataBatch(List<WeatherData> weatherDataList) {
        if (weatherDataList == null || weatherDataList.isEmpty()) {
            logger.warn("Empty weather data list provided for batch processing");
            return new ArrayList<>();
        }

        logger.info("Starting batch processing for {} weather records", weatherDataList.size());

        Instant startTime = Instant.now();

        // Create async tasks for each record
        List<CompletableFuture<WeatherData>> futures = weatherDataList.stream()
                .map(data -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return processWeatherData(data);
                    } catch (IOException e) {
                        logger.error("Failed to process weather data for station {}: {}",
                                data.getStationId(), e.getMessage());
                        return null;
                    }
                }, executorService))
                .toList();

        // Wait for all tasks to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        // Collect results
        List<WeatherData> results = new ArrayList<>();
        try {
            // Wait up to 5 minutes for all uploads to complete
            allFutures.get(5, TimeUnit.MINUTES);

            for (CompletableFuture<WeatherData> future : futures) {
                WeatherData data = future.get();
                if (data != null) {
                    results.add(data);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Batch processing interrupted: {}", e.getMessage());
        } catch (ExecutionException e) {
            logger.error("Batch processing execution failed: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.error("Batch processing timed out after 5 minutes");
        }

        Duration duration = Duration.between(startTime, Instant.now());
        logger.info("Batch processing complete: {}/{} succeeded in {}ms",
                results.size(), weatherDataList.size(), duration.toMillis());

        return results;
    }

    /**
     * Enriches weather data with generic metadata.
     * <p>
     * This is NOT source-specific validation - just adding metadata
     * that applies to all weather data regardless of source.
     * <p>
     * Adds:
     * - validated: "true" (assumes caller already validated)
     * - validation_timestamp: current timestamp
     * - processor: "SpeedLayerProcessor"
     * - processor_version: version identifier
     *
     * @param weatherData the weather data to enrich
     */
    private void enrichWithMetadata(WeatherData weatherData) {
        weatherData.addMetadata("validated", "true");
        weatherData.addMetadata("validation_timestamp", LocalDateTime.now().toString());
        weatherData.addMetadata("processor", "SpeedLayerProcessor");
        weatherData.addMetadata("processor_version", "2.0");

        logger.debug("Enriched weather data with generic metadata for station: {}",
                weatherData.getStationId());
    }

    /**
     * Returns statistics about the speed layer processor.
     *
     * @return statistics map
     */
    public java.util.Map<String, Object> getStatistics() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) executorService;

        return java.util.Map.of(
                "max_concurrent_requests", maxConcurrentRequests,
                "executor_active_threads", executor.getActiveCount(),
                "executor_queue_size", executor.getQueue().size(),
                "executor_completed_tasks", executor.getCompletedTaskCount()
        );
    }

    /**
     * Checks if the speed layer processor is healthy.
     * Verifies S3 connectivity.
     *
     * @return true if S3 is accessible
     */
    public boolean isHealthy() {
        return s3Service.isBucketAccessible();
    }

    /**
     * Shuts down the speed layer processor gracefully.
     * Waits for in-flight processing to complete.
     */
    public void shutdown() {
        logger.info("Shutting down SpeedLayerProcessor");
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate in time, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("SpeedLayerProcessor shutdown complete");
    }
}
