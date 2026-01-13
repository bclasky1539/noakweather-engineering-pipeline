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
package weather.ingestion.service.source.noaa;

import weather.ingestion.service.S3UploadService;
import weather.ingestion.service.SpeedLayerProcessor;
import weather.model.WeatherData;
import weather.exception.WeatherServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class for NOAA weather data ingestion orchestrators.
 * <p>
 * Provides common ingestion logic for METAR, TAF, and any future NOAA data types.
 * Subclasses only need to implement the data type-specific fetch method.
 * <p>
 * Template Method Pattern: The algorithm is defined here, but specific steps
 * (like fetching data) are delegated to subclasses.
 *
 * @author bclasky1539
 *
 */
public abstract class AbstractNoaaIngestionOrchestrator {

    private static final Logger logger = LogManager.getLogger(AbstractNoaaIngestionOrchestrator.class);

    protected final NoaaAviationWeatherClient noaaClient;
    protected final SpeedLayerProcessor speedLayerProcessor;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduler;
    private final MetricsCollector metrics;
    private final String dataType; // "METAR" or "TAF"

    /**
     * Creates orchestrator with custom concurrency.
     *
     * @param noaaClient the NOAA weather client
     * @param s3Service the S3 upload service
     * @param maxConcurrentFetches maximum number of concurrent NOAA requests
     * @param dataType the type of data being ingested (e.g., "METAR", "TAF")
     */
    protected AbstractNoaaIngestionOrchestrator(NoaaAviationWeatherClient noaaClient,
                                                S3UploadService s3Service,
                                                int maxConcurrentFetches,
                                                String dataType) {
        this.noaaClient = noaaClient;
        this.speedLayerProcessor = new SpeedLayerProcessor(s3Service, maxConcurrentFetches);
        this.executorService = Executors.newFixedThreadPool(maxConcurrentFetches);
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.metrics = new MetricsCollector();
        this.dataType = dataType;

        logger.info("{} ingestion orchestrator initialized with {} concurrent fetches",
                dataType, maxConcurrentFetches);
    }

    /**
     * Template method: Fetches data from NOAA for a specific station.
     * Subclasses must implement this to call the appropriate fetch method
     * (fetchMetarReport, fetchTafReport, etc.)
     *
     * @param stationId ICAO station identifier
     * @return WeatherData or null if no data available
     * @throws WeatherServiceException if fetch fails
     */
    protected abstract WeatherData fetchFromNoaa(String stationId) throws WeatherServiceException;

    /**
     * Ingests weather data for a single station.
     * <p>
     * This is the main template method that defines the ingestion algorithm.
     *
     * @param stationId ICAO station identifier
     * @return ingested WeatherData with S3 location
     * @throws WeatherServiceException if ingestion fails
     */
    public WeatherData ingestStation(String stationId) throws WeatherServiceException {
        logger.info("Starting {} ingestion for station: {}", dataType, stationId);

        Instant startTime = Instant.now();

        try {
            // Step 1: Fetch from NOAA (delegated to subclass)
            metrics.incrementFetchAttempts();
            WeatherData weatherData = fetchFromNoaa(stationId);

            if (weatherData == null) {
                metrics.incrementNoDataCount();
                logger.warn("No {} data available for station: {}", dataType, stationId);
                throw new WeatherServiceException(
                        weather.exception.ErrorType.NO_DATA,
                        "No " + dataType + " data available",
                        stationId
                );
            }

            metrics.incrementFetchSuccesses();

            // Step 2: NOAA-specific validation
            validateNoaaWeatherData(weatherData);

            // Step 3: Process through speed layer (generic enrichment + S3 upload)
            weatherData = speedLayerProcessor.processWeatherData(weatherData);
            weatherData.addMetadata("ingestion_duration_ms",
                    Duration.between(startTime, Instant.now()).toMillis());

            metrics.incrementUploadSuccesses();

            Duration duration = Duration.between(startTime, Instant.now());
            logger.info("Successfully ingested {} for {} in {}ms",
                    dataType, stationId, duration.toMillis());

            return weatherData;

        } catch (IOException e) {
            metrics.incrementUploadFailures();
            throw new WeatherServiceException(
                    weather.exception.ErrorType.STORAGE_ERROR,
                    "Failed to process through speed layer",
                    stationId,
                    e
            );
        } catch (WeatherServiceException e) {
            metrics.incrementFetchFailures();
            throw e;
        }
    }

    /**
     * Ingests weather data for multiple stations in parallel.
     *
     * @param stationIds list of ICAO station identifiers
     * @return list of successfully ingested WeatherData
     */
    public List<WeatherData> ingestStationsBatch(List<String> stationIds) {
        logger.info("Starting batch {} ingestion for {} stations", dataType, stationIds.size());

        Instant startTime = Instant.now();

        List<CompletableFuture<WeatherData>> futures = stationIds.stream()
                .map(stationId -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return ingestStation(stationId);
                    } catch (WeatherServiceException e) {
                        logIngestionFailure(stationId, e);
                        return null;
                    }
                }, executorService))
                .toList();

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        List<WeatherData> results = new ArrayList<>();
        try {
            allFutures.get(2, TimeUnit.MINUTES);

            for (CompletableFuture<WeatherData> future : futures) {
                WeatherData data = future.get();
                if (data != null) {
                    results.add(data);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Batch ingestion interrupted: {}", e.getMessage());
        } catch (ExecutionException e) {
            logger.error("Batch ingestion execution failed: {}", e.getMessage());
        } catch (TimeoutException e) {
            logger.error("Batch ingestion timed out after 2 minutes");
        }

        Duration duration = Duration.between(startTime, Instant.now());
        logger.info("Batch ingestion complete: {}/{} succeeded in {}ms",
                results.size(), stationIds.size(), duration.toMillis());

        return results;
    }

    /**
     * Ingests weather data for multiple stations sequentially.
     *
     * @param stationIds list of ICAO station identifiers
     * @return ingestion results with success/failure details
     */
    public IngestionResult ingestStationsSequential(List<String> stationIds) {
        logger.info("Starting sequential {} ingestion for {} stations", dataType, stationIds.size());

        IngestionResult result = new IngestionResult();
        Instant startTime = Instant.now();

        for (String stationId : stationIds) {
            try {
                WeatherData data = ingestStation(stationId);
                result.addSuccess(stationId, data);
            } catch (WeatherServiceException e) {
                result.addFailure(stationId, e);
                logIngestionFailure(stationId, e);
            }
        }

        Duration duration = Duration.between(startTime, Instant.now());
        result.setDuration(duration);

        logger.info("Sequential ingestion complete: {} succeeded, {} failed in {}ms",
                result.getSuccessCount(), result.getFailureCount(), duration.toMillis());

        return result;
    }

    /**
     * Logs an ingestion failure with consistent formatting.
     *
     * @param stationId the station that failed
     * @param e the exception that occurred
     */
    private void logIngestionFailure(String stationId, WeatherServiceException e) {
        logger.error("Failed to ingest station {}: {}", stationId, e.getMessage());
    }

    /**
     * Schedules periodic ingestion for specified stations.
     *
     * @param stationIds list of ICAO station identifiers
     * @param intervalMinutes interval between ingestion runs (in minutes)
     * @return ScheduledFuture that can be used to cancel the scheduled ingestion
     */
    public ScheduledFuture<Void> schedulePeriodicIngestion(List<String> stationIds,
                                                           int intervalMinutes) {
        logger.info("Scheduling periodic {} ingestion for {} stations every {} minutes",
                dataType, stationIds.size(), intervalMinutes);

        Runnable ingestionTask = () -> {
            try {
                logger.info("Running scheduled {} ingestion", dataType);
                List<WeatherData> results = ingestStationsBatch(stationIds);
                logger.info("Scheduled ingestion complete: {} stations processed",
                        results.size());
            } catch (Exception e) {
                logger.error("Error in scheduled ingestion: {}", e.getMessage());
            }
        };

        @SuppressWarnings("unchecked")
        ScheduledFuture<Void> future = (ScheduledFuture<Void>) scheduler.scheduleAtFixedRate(
                ingestionTask,
                0,
                intervalMinutes,
                TimeUnit.MINUTES
        );

        return future;
    }

    /**
     * Validates NOAA-specific required fields.
     * <p>
     * This is SOURCE-SPECIFIC validation for NOAA data.
     * Different sources (OpenWeather, Weather.gov) would have different validations.
     *
     * @param weatherData the weather data to validate
     * @throws WeatherServiceException if validation fails
     */
    private void validateNoaaWeatherData(WeatherData weatherData) throws WeatherServiceException {
        if (weatherData.getStationId() == null || weatherData.getStationId().isEmpty()) {
            throw new WeatherServiceException(
                    weather.exception.ErrorType.INVALID_DATA,
                    "NOAA weather data missing required field: stationId"
            );
        }

        if (weatherData.getRawData() == null || weatherData.getRawData().isEmpty()) {
            throw new WeatherServiceException(
                    weather.exception.ErrorType.INVALID_DATA,
                    "NOAA weather data missing required field: rawData",
                    weatherData.getStationId()
            );
        }

        if (weatherData.getSource() == null) {
            throw new WeatherServiceException(
                    weather.exception.ErrorType.INVALID_DATA,
                    "NOAA weather data missing required field: source",
                    weatherData.getStationId()
            );
        }

        logger.debug("NOAA-specific validation passed for station: {}",
                weatherData.getStationId());
    }

    public Map<String, Object> getMetrics() {
        return metrics.getSnapshot();
    }

    public boolean isHealthy() {
        return speedLayerProcessor.isHealthy();
    }

    public void shutdown() {
        logger.info("Shutting down {} ingestion orchestrator", dataType);

        scheduler.shutdown();
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate in time, forcing shutdown");
                executorService.shutdownNow();
            }

            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }

        } catch (InterruptedException e) {
            logger.error("Shutdown interrupted");
            executorService.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        noaaClient.close();
        speedLayerProcessor.shutdown();

        logger.info("{} ingestion orchestrator shutdown complete", dataType);
    }

    /**
     * Inner class for tracking ingestion results.
     */
    public static class IngestionResult {
        private final List<String> successfulStations = new ArrayList<>();
        private final List<WeatherData> successfulData = new ArrayList<>();
        private final Map<String, WeatherServiceException> failures = new ConcurrentHashMap<>();
        private Duration duration;

        public void addSuccess(String stationId, WeatherData data) {
            successfulStations.add(stationId);
            successfulData.add(data);
        }

        public void addFailure(String stationId, WeatherServiceException exception) {
            failures.put(stationId, exception);
        }

        public void setDuration(Duration duration) {
            this.duration = duration;
        }

        public int getSuccessCount() {
            return successfulStations.size();
        }

        public int getFailureCount() {
            return failures.size();
        }

        public List<String> getSuccessfulStations() {
            return new ArrayList<>(successfulStations);
        }

        public List<WeatherData> getSuccessfulData() {
            return new ArrayList<>(successfulData);
        }

        public Map<String, WeatherServiceException> getFailures() {
            return new ConcurrentHashMap<>(failures);
        }

        public Duration getDuration() {
            return duration;
        }

        public double getSuccessRate() {
            int total = getSuccessCount() + getFailureCount();
            return total == 0 ? 0.0 : (double) getSuccessCount() / total;
        }

        @Override
        public String toString() {
            return String.format(
                    "IngestionResult{success=%d, failed=%d, rate=%.2f%%, duration=%dms}",
                    getSuccessCount(),
                    getFailureCount(),
                    getSuccessRate() * 100,
                    duration != null ? duration.toMillis() : 0
            );
        }
    }

    /**
     * Inner class for collecting ingestion metrics.
     */
    protected static class MetricsCollector {
        private final AtomicLong fetchAttempts = new AtomicLong(0);
        private final AtomicLong fetchSuccesses = new AtomicLong(0);
        private final AtomicLong fetchFailures = new AtomicLong(0);
        private final AtomicLong noDataCount = new AtomicLong(0);
        private final AtomicLong uploadSuccesses = new AtomicLong(0);
        private final AtomicLong uploadFailures = new AtomicLong(0);

        public void incrementFetchAttempts() {
            fetchAttempts.incrementAndGet();
        }

        public void incrementFetchSuccesses() {
            fetchSuccesses.incrementAndGet();
        }

        public void incrementFetchFailures() {
            fetchFailures.incrementAndGet();
        }

        public void incrementNoDataCount() {
            noDataCount.incrementAndGet();
        }

        public void incrementUploadSuccesses() {
            uploadSuccesses.incrementAndGet();
        }

        public void incrementUploadFailures() {
            uploadFailures.incrementAndGet();
        }

        public Map<String, Object> getSnapshot() {
            return Map.of(
                    "fetch_attempts", fetchAttempts.get(),
                    "fetch_successes", fetchSuccesses.get(),
                    "fetch_failures", fetchFailures.get(),
                    "no_data_count", noDataCount.get(),
                    "upload_successes", uploadSuccesses.get(),
                    "upload_failures", uploadFailures.get(),
                    "fetch_success_rate", calculateRate(fetchSuccesses.get(), fetchAttempts.get()),
                    "upload_success_rate", calculateRate(uploadSuccesses.get(),
                            uploadSuccesses.get() + uploadFailures.get())
            );
        }

        private double calculateRate(long successes, long total) {
            return total == 0 ? 0.0 : (double) successes / total;
        }
    }
}
