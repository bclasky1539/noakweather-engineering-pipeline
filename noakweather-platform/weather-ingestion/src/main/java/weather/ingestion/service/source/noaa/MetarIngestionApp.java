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

/**
 * Command-line application for METAR data ingestion from NOAA.
 * <p>
 * Dramatically simplified by extending AbstractNoaaIngestionApp.
 * Only METAR-specific configuration is defined here.
 *
 * @author bclasky1539
 *
 */
public class MetarIngestionApp extends AbstractNoaaIngestionApp {

    public static void main(String[] args) {
        new MetarIngestionApp().run(args);
    }

    @Override
    protected AbstractNoaaIngestionOrchestrator createOrchestrator(
            NoaaAviationWeatherClient noaaClient,
            S3UploadService s3Service,
            int maxConcurrentFetches) {
        return new MetarIngestionOrchestrator(noaaClient, s3Service, maxConcurrentFetches);
    }

    @Override
    protected String getDataType() {
        return "METAR";
    }
}
