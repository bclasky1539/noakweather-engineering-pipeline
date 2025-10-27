/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025 bclasky1539
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

import weather.ingestion.service.source.noaa.NoaaAviationWeatherClient;
import weather.model.ProcessingLayer;
import weather.model.WeatherData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import weather.exception.WeatherServiceException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Speed Layer Processor for Lambda Architecture.
 * 
 * Responsibilities:
 * 1. Fetch weather data from external APIs (NOAA, OpenWeatherMap, etc.)
 * 2. Perform minimal processing/validation
 * 3. Upload to S3 for low-latency access
 * 4. Tag data with ProcessingLayer.SPEED_LAYER
 * 
 * The Speed Layer prioritizes:
 * - Low latency (seconds to minutes)
 * - Recent data only (last 24-48 hours)
 * - Minimal processing (validation, basic enrichment)
 * 
 * This complements the Batch Layer which provides historical analysis.
 * 
 * Think of this as a "fast lane" - like express checkout at a grocery store.
 * It handles recent transactions quickly, while the main checkout (Batch Layer)
 * handles comprehensive inventory analysis.
 * 
 * NEW FUNCTIONALITY - Not present in legacy system
 * 
 * @author bclasky1539
 *
 */
public class SpeedLayerProcessor {
    
    private static final Logger logger = LogManager.getLogger(SpeedLayerProcessor.class);
    
    private final NoaaAviationWeatherClient noaaClient;
    private final S3UploadService s3Service;
    private final ExecutorService executorService;
    private final int maxConcurrentRequests;
    
    /**
     * Creates a SpeedLayerProcessor with specified services.
     * 
     * @param noaaClient the NOAA weather client
     * @param s3Service the S3 upload service
     */
    public SpeedLayerProcessor(NoaaAviationWeatherClient noaaClient, S3UploadService s3Service) {
        this(noaaClient, s3Service, 5); // Default: 5 concurrent requests
    }
    
    /**
     * Creates a SpeedLayerProcessor with custom concurrency limit.
     * 
     * @param noaaClient the NOAA weather client
     * @param s3Service the S3 upload service
     * @param maxConcurrentRequests maximum number of concurrent API requests
     */
    public SpeedLayerProcessor(NoaaAviationWeatherClient noaaClient, 
                               S3UploadService s3Service,
                               int maxConcurrentRequests) {
        this.noaaClient = noaaClient;
        this.s3Service = s3Service;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.executorService = Executors.newFixedThreadPool(maxConcurrentRequests);
        
        logger.info("SpeedLayerProcessor initialized with {} concurrent threads", 
                maxConcurrentRequests);
    }
    
    /**
     * Processes weather data for a single station through the speed layer.
     * 
     * Flow:
     * 1. Fetch from NOAA API
     * 2. Validate data
     * 3. Tag with SPEED_LAYER
     * 4. Upload to S3
     * 5. Return processed data
     * 
     * @param stationId the ICAO station identifier
     * @return the processed WeatherData
     * @throws IOException if ingestion or upload fails
     */
    public WeatherData processStation(String stationId) throws IOException {
        logger.info("Processing station {} through Speed Layer", stationId);
        
        long startTime = System.currentTimeMillis();
    
        try {
            // Step 1: Fetch from NOAA API
            WeatherData weatherData = noaaClient.fetchLatestMetar(stationId);
        
            if (weatherData == null) {
                throw new IOException("No weather data available for station: " + stationId);
            }
        
            // Step 2: Validate and enrich
            validateAndEnrich(weatherData);
        
            // Step 3: Tag with Speed Layer
            weatherData.setProcessingLayer(ProcessingLayer.SPEED_LAYER);
        
            // Step 4: Upload to S3
            String s3Key = s3Service.uploadWeatherData(weatherData);
            weatherData.addMetadata("storage_location", s3Key);
        
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Processed station {} in {}ms (S3: {})", stationId, duration, s3Key);
        
            return weatherData;

        } catch (WeatherServiceException e) {
            logger.error("Failed to fetch weather data for station {}: {}", stationId, e.getMessage());
            throw new IOException("Failed to process station: " + stationId, e);
        }
    }

    /**
     * Processes multiple stations in parallel through the speed layer.
     * More efficient than processing one at a time.
     * 
     * @param stationIds list of ICAO station identifiers
     * @return list of processed WeatherData
     */
    public List<WeatherData> processStationsBatch(List<String> stationIds) {
        logger.info("Processing {} stations in batch through Speed Layer", stationIds.size());
        
        long startTime = System.currentTimeMillis();
        
        // Create async tasks for each station
        List<CompletableFuture<WeatherData>> futures = stationIds.stream()
                .map(stationId -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return processStation(stationId);
                    } catch (IOException e) {
                        logger.error("Failed to process station {}: {}", stationId, e.getMessage());
                        return null;
                    }
                }, executorService))
                .collect(Collectors.toList());
        
        // Wait for all tasks to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(CompletableFuture[]::new));
        
        // Collect results
        List<WeatherData> results = new ArrayList<>();
        try {
            allFutures.get(60, TimeUnit.SECONDS); // 60 second timeout
            
            for (CompletableFuture<WeatherData> future : futures) {
                WeatherData data = future.get();
                if (data != null) {
                    results.add(data);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            logger.error("Batch processing interrupted: {}", e.getMessage());
        } catch (ExecutionException e) {
            logger.error("Batch processing execution failed: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.error("Batch processing timed out after 60 seconds: {}", e.getMessage());
}
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Batch processing complete: {}/{} succeeded in {}ms", 
                results.size(), stationIds.size(), duration);
        
        return results;
    }
    
    /**
     * Processes weather data for a geographic region (bounding box).
     * 
     * @param minLat minimum latitude
     * @param minLon minimum longitude
     * @param maxLat maximum latitude
     * @param maxLon maximum longitude
     * @return list of processed WeatherData
     * @throws IOException if ingestion or upload fails
     */
    public List<WeatherData> processRegion(double minLat, double minLon, 
                                           double maxLat, double maxLon) throws IOException {
        logger.info("Processing region through Speed Layer: ({},{}) to ({},{})", 
                minLat, minLon, maxLat, maxLon);
        
        try {
            // Step 1: Fetch all stations in bounding box
            List<WeatherData> weatherDataList = noaaClient.fetchMetarByBoundingBox(
                    minLat, minLon, maxLat, maxLon);
        
            if (weatherDataList.isEmpty()) {
                logger.warn("No weather data found in specified region");
                return weatherDataList;
            }
        
            // Step 2: Process and upload to S3
            for (WeatherData data : weatherDataList) {
                validateAndEnrich(data);
                data.setProcessingLayer(ProcessingLayer.SPEED_LAYER);
            }
        
            List<String> s3Keys = s3Service.uploadWeatherDataBatch(weatherDataList);
        
            // Step 3: Update storage locations
            for (int i = 0; i < Math.min(weatherDataList.size(), s3Keys.size()); i++) {
                weatherDataList.get(i).addMetadata("storage_location", s3Keys.get(i));
            }
        
            logger.info("Processed {} stations in region", weatherDataList.size());

            return weatherDataList;
        
        } catch (WeatherServiceException e) {
            logger.error("Failed to fetch weather data for region: {}", e.getMessage());
            throw new IOException("Failed to process region", e);
        }
    }
    
    /**
     * Validates weather data and adds enrichment metadata.
     * 
     * @param weatherData the weather data to validate
     * @throws IOException if validation fails
     */
    private void validateAndEnrich(WeatherData weatherData) throws IOException {
        // Validate required fields
        if (weatherData.getStationId() == null || weatherData.getStationId().isEmpty()) {
            throw new IOException("Weather data missing required field: stationId");
        }
        
        if (weatherData.getSource() == null) {
            throw new IOException("Weather data missing required field: dataSource");
        }
        
        // Mark as validated
        weatherData.addMetadata("validated", "true");
        weatherData.addMetadata("validation_timestamp", LocalDateTime.now().toString());
        weatherData.addMetadata("processor", "SpeedLayerProcessor");
    }
    
    /**
     * Runs a continuous ingestion loop for specified stations.
     * Useful for real-time monitoring scenarios.
     * 
     * @param stationIds list of stations to monitor
     * @param intervalSeconds interval between ingestion cycles (in seconds)
     * @param durationMinutes how long to run (in minutes)
     */
    public void runContinuousIngestion(List<String> stationIds, 
                                       int intervalSeconds, 
                                       int durationMinutes) {
        logger.info("Starting continuous ingestion for {} stations (interval: {}s, duration: {}m)",
                stationIds.size(), intervalSeconds, durationMinutes);
        
        long endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);
        int cycleCount = 0;
        
        while (System.currentTimeMillis() < endTime) {
            cycleCount++;
            logger.info("Starting ingestion cycle #{}", cycleCount);
            
            List<WeatherData> results = processStationsBatch(stationIds);
            logger.info("Cycle #{} complete: {} stations processed", cycleCount, results.size());
            
            // Sleep until next cycle
            try {
                Thread.sleep(intervalSeconds * 1000L);
            } catch (InterruptedException e) {
                logger.warn("Continuous ingestion interrupted");
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        logger.info("Continuous ingestion complete: {} cycles executed", cycleCount);
    }
    
    /**
     * Returns statistics about the speed layer processor.
     * 
     * @return statistics map
     */
    public java.util.Map<String, Object> getStatistics() {
        return java.util.Map.of(
                "max_concurrent_requests", maxConcurrentRequests,
                "executor_active_threads", ((java.util.concurrent.ThreadPoolExecutor) executorService).getActiveCount(),
                "executor_queue_size", ((java.util.concurrent.ThreadPoolExecutor) executorService).getQueue().size()
        );
    }
    
    /**
     * Shuts down the speed layer processor gracefully.
     */
    public void shutdown() {
        logger.info("Shutting down SpeedLayerProcessor");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("SpeedLayerProcessor shutdown complete");
    }
}
