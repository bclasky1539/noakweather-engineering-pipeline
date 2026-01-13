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
import weather.model.WeatherData;
import weather.exception.WeatherServiceException;

/**
 * Orchestrates TAF data ingestion from NOAA to S3 storage.
 * <p>
 * This class extends AbstractNoaaIngestionOrchestrator.
 * The only TAF-specific logic is the fetch method.
 * <p>
 * All common functionality (validation, metrics, scheduling, batch processing, etc.)
 * is inherited from the base class.
 *
 * @author bclasky1539
 *
 */
public class TafIngestionOrchestrator extends AbstractNoaaIngestionOrchestrator {

    /**
     * Creates orchestrator with default concurrency.
     *
     * @param noaaClient the NOAA weather client
     * @param s3Service the S3 upload service
     */
    public TafIngestionOrchestrator(NoaaAviationWeatherClient noaaClient,
                                    S3UploadService s3Service) {
        this(noaaClient, s3Service, 10);
    }

    /**
     * Creates orchestrator with custom concurrency.
     *
     * @param noaaClient the NOAA weather client
     * @param s3Service the S3 upload service
     * @param maxConcurrentFetches maximum number of concurrent NOAA requests
     */
    public TafIngestionOrchestrator(NoaaAviationWeatherClient noaaClient,
                                    S3UploadService s3Service,
                                    int maxConcurrentFetches) {
        super(noaaClient, s3Service, maxConcurrentFetches, "TAF");
    }

    /**
     * Fetches TAF data from NOAA for a specific station.
     * <p>
     * This is the ONLY TAF-specific method - everything else is inherited.
     *
     * @param stationId ICAO station identifier
     * @return WeatherData or null if no data available
     * @throws WeatherServiceException if fetch fails
     */
    @Override
    protected WeatherData fetchFromNoaa(String stationId) throws WeatherServiceException {
        return noaaClient.fetchTafReport(stationId);
    }
}
