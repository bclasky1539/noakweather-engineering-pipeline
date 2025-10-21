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
package weather.service;

import weather.model.WeatherData;
import weather.model.WeatherDataSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Universal interface for weather data services.
 * 
 * Design Pattern: Facade Pattern
 * 
 * Provides a unified API for accessing weather data regardless of the
 * underlying source (NOAA, OpenWeatherMap, etc.) or storage layer
 * (S3, DynamoDB, Snowflake).
 * 
 * Analogy: Like a universal remote control that works with any TV brand -
 * you press "power" and it figures out the right signal for your specific TV.
 * 
 * @param <T> the specific type of WeatherData this service handles
 * 
 * @author bclasky1539
 *
 */
public interface WeatherService<T extends WeatherData> {
    
    /**
     * Get current weather data for a station/location.
     * 
     * @param stationId the station identifier (e.g., "KJFK")
     * @return current weather data, or empty if not available
     */
    Optional<T> getCurrentWeather(String stationId);
    
    /**
     * Get historical weather data for a station within a time range.
     * 
     * @param stationId the station identifier
     * @param startTime start of time range (inclusive)
     * @param endTime end of time range (inclusive)
     * @return list of weather data within the time range
     */
    List<T> getWeatherHistory(String stationId, Instant startTime, Instant endTime);
    
    /**
     * Store weather data.
     * 
     * @param data the weather data to store
     * @return true if stored successfully
     */
    boolean store(T data);
    
    /**
     * Batch store multiple weather data objects.
     * More efficient than calling store() repeatedly.
     * 
     * @param dataList list of weather data to store
     * @return number of successfully stored records
     */
    int storeBatch(List<T> dataList);
    
    /**
     * Get the data source this service handles.
     * 
     * @return the WeatherDataSource enum value
     */
    WeatherDataSource getSource();
    
    /**
     * Check if this service is available and operational.
     * Useful for health checks and circuit breaker patterns.
     * 
     * @return true if service is healthy
     */
    boolean isHealthy();
    
    /**
     * Refresh/reload weather data from the source.
     * Typically called on a schedule or triggered by events.
     * 
     * @return number of records refreshed
     */
    int refresh();
}
