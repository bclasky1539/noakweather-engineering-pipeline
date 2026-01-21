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

import weather.model.WeatherData;
import weather.model.WeatherDataSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Universal repository interface for weather data storage.
 * <p>
 * This serves as the primary data access contract for the Lambda Architecture's
 * batch and serving layers. Think of this as your universal DAO interface that
 * can be implemented by different storage backends (Snowflake, DynamoDB, etc.).
 * <p>
 * Design Pattern: Repository Pattern
 * Lambda Architecture Layer: Batch + Serving Layer
 * <p>
 * UPDATED v1.12.0-SNAPSHOT: Changed from LocalDateTime to Instant for consistency
 * with WeatherData domain model. Instant provides:
 * - UTC-based timestamps (no timezone confusion)
 * - Nanosecond precision
 * - Direct alignment with WeatherData.observationTime
 *
 * @author bclasky1539
 *
 */
public interface UniversalWeatherRepository {

    /**
     * Saves a single weather data record to the repository.
     *
     * @param weatherData the weather data to save
     * @return the saved weather data with any generated IDs or timestamps
     * @implNote Implementations may modify the returned instance with database-generated
     *           fields. Callers should use the returned instance for subsequent operations
     */
    WeatherData save(WeatherData weatherData);

    /**
     * Saves multiple weather data records in batch.
     * This is optimized for the batch layer processing.
     *
     * @param weatherDataList list of weather data to save
     * @return number of records successfully saved
     * @implNote The return value is critical for monitoring batch job success rates,
     *           detecting partial failures, and triggering alerts when save rates drop
     *           below expected thresholds. Implementations should return the actual count
     *           of successfully persisted records, not the input list size
     */
    int saveBatch(List<WeatherData> weatherDataList);

    /**
     * Finds weather data by station ID and observation time.
     *
     * @param stationId ICAO station identifier (e.g., "KJFK")
     * @param observationTime the observation time (UTC)
     * @return Optional containing the weather data if found
     */
    Optional<WeatherData> findByStationAndTime(String stationId, Instant observationTime);

    /**
     * Retrieves all weather data for a given station within a time range.
     *
     * @param stationId ICAO station identifier
     * @param startTime start of time range (inclusive, UTC)
     * @param endTime end of time range (inclusive, UTC)
     * @return list of weather data records
     */
    List<WeatherData> findByStationAndTimeRange(String stationId,
                                                Instant startTime,
                                                Instant endTime);

    /**
     * Retrieves the most recent weather data for a given station.
     * Optimized for the serving layer's fast queries.
     *
     * @param stationId ICAO station identifier
     * @return Optional containing the most recent weather data
     */
    Optional<WeatherData> findLatestByStation(String stationId);

    /**
     * Retrieves weather data by source within a time range.
     * Useful for comparing data from different providers (NOAA, OpenWeatherMap, etc.)
     *
     * @param source the weather data source
     * @param startTime start of time range (UTC)
     * @param endTime end of time range (UTC)
     * @return list of weather data from the specified source
     */
    List<WeatherData> findBySourceAndTimeRange(WeatherDataSource source,
                                               Instant startTime,
                                               Instant endTime);

    /**
     * Retrieves weather data for multiple stations at once.
     * Optimized for batch queries in the serving layer.
     *
     * @param stationIds list of station identifiers
     * @param observationTime the observation time to query (UTC)
     * @return list of weather data for the specified stations
     */
    List<WeatherData> findByStationsAndTime(List<String> stationIds, Instant observationTime);

    /**
     * Deletes weather data older than the specified date.
     * Used for data retention policy enforcement.
     *
     * @param cutoffDate data older than this date will be deleted (UTC)
     * @return number of records deleted
     * @implNote The return value is essential for audit trails, compliance reporting,
     *           and storage capacity planning. Callers should log this value for
     *           data retention policy enforcement tracking and cost optimization metrics
     */
    int deleteOlderThan(Instant cutoffDate);

    /**
     * Checks if the repository is healthy and accessible.
     *
     * @return true if the repository is operational
     */
    boolean isHealthy();

    /**
     * Returns statistics about the stored data.
     *
     * @return repository statistics (record count, oldest/newest records, etc.)
     */
    RepositoryStats getStats();
}
