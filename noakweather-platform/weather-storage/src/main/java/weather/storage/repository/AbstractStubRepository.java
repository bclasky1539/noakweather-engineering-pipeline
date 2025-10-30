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
package weather.storage.repository;

import weather.model.WeatherData;
import weather.model.WeatherDataSource;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Abstract base class for stub repository implementations.
 * Provides default stub behavior that can be overridden when implemented.
 * 
 * This eliminates code duplication between DynamoDbRepository and SnowflakeRepository
 * stub implementations while maintaining the same behavior.
 * 
 * All methods provide sensible stub defaults:
 * - Write operations throw UnsupportedOperationException
 * - Read operations return empty Optional/Collections
 * - Health check returns false (not operational)
 * - Stats return zeros
 * 
 * @author bclasky1539
 *
 */
public abstract class AbstractStubRepository implements UniversalWeatherRepository {
    
    private final String repositoryName;
    
    /**
     * Constructor for stub repository.
     * 
     * @param repositoryName the name of the repository (for error messages)
     */
    protected AbstractStubRepository(String repositoryName) {
        this.repositoryName = repositoryName;
    }
    
    /**
     * Gets the "not implemented" message for this repository.
     * 
     * @return error message indicating stub status
     */
    protected String getNotImplementedMessage() {
        return repositoryName + " not yet implemented - scheduled for later";
    }
    
    @Override
    public WeatherData save(WeatherData weatherData) {
        throw new UnsupportedOperationException(getNotImplementedMessage());
    }
    
    @Override
    public int saveBatch(List<WeatherData> weatherDataList) {
        throw new UnsupportedOperationException(getNotImplementedMessage());
    }
    
    @Override
    public Optional<WeatherData> findByStationAndTime(String stationId, LocalDateTime observationTime) {
        return Optional.empty();
    }
    
    @Override
    public List<WeatherData> findByStationAndTimeRange(String stationId, 
                                                        LocalDateTime startTime, 
                                                        LocalDateTime endTime) {
        return Collections.emptyList();
    }
    
    @Override
    public Optional<WeatherData> findLatestByStation(String stationId) {
        return Optional.empty();
    }
    
    @Override
    public List<WeatherData> findBySourceAndTimeRange(WeatherDataSource source,
                                                       LocalDateTime startTime,
                                                       LocalDateTime endTime) {
        return Collections.emptyList();
    }
    
    @Override
    public List<WeatherData> findByStationsAndTime(List<String> stationIds, 
                                                    LocalDateTime observationTime) {
        return Collections.emptyList();
    }
    
    @Override
    public int deleteOlderThan(LocalDateTime cutoffDate) {
        throw new UnsupportedOperationException(getNotImplementedMessage());
    }
    
    @Override
    public boolean isHealthy() {
        return false; // Not operational yet
    }
    
    @Override
    public RepositoryStats getStats() {
        return new RepositoryStats(
            0L,           // totalRecordCount
            null,         // oldestRecordTime
            null,         // newestRecordTime
            0L,           // recordsLast24Hours
            0L,           // recordsLast7Days
            0,            // uniqueStationCount
            0L            // storageSize
        );
    }
}
