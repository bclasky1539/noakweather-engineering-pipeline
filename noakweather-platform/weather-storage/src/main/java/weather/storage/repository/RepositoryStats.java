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
package weather.storage.repository;

import java.time.Instant;

/**
 * Statistics and metrics about a weather data repository.
 * <p>
 * This record provides insight into the repository's contents and health,
 * useful for monitoring and operational dashboards.
 * <p>
 * UPDATED v1.12.0-SNAPSHOT:
 * - Changed from LocalDateTime to Instant for consistency with WeatherData domain model
 * - Converted from class to record for immutability and conciseness
 *
 * @param totalRecordCount total number of records in the repository
 * @param oldestRecordTime timestamp of the oldest record (UTC), or null if no records
 * @param newestRecordTime timestamp of the newest record (UTC), or null if no records
 * @param recordsLast24Hours number of records ingested in the last 24 hours
 * @param recordsLast7Days number of records ingested in the last 7 days
 * @param uniqueStationCount number of unique weather stations in the repository
 * @param storageSize total storage size in bytes, if applicable
 *
 * @author bclasky1539
 *
 */
public record RepositoryStats(
        long totalRecordCount,
        Instant oldestRecordTime,
        Instant newestRecordTime,
        long recordsLast24Hours,
        long recordsLast7Days,
        int uniqueStationCount,
        long storageSize
) {
    /**
     * Custom toString with formatted output for monitoring dashboards.
     *
     * @return formatted string representation
     */
    @Override
    public String toString() {
        return String.format(
                "RepositoryStats{totalRecords=%d, stations=%d, oldest=%s, newest=%s, last24h=%d, last7d=%d, size=%d bytes}",
                totalRecordCount, uniqueStationCount, oldestRecordTime, newestRecordTime,
                recordsLast24Hours, recordsLast7Days, storageSize
        );
    }
}
