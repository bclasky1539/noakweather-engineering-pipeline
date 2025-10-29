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
import weather.storage.repository.snowflake.SnowflakeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UniversalWeatherRepository implementations.
 * 
 * These tests verify the contract of the repository interface.
 * During Week 1, most tests verify stub behavior (empty returns, exceptions).
 * 
 * As implementations are completed in Week 2+, these tests will verify
 * actual functionality.
 * 
 * @author bclasky1539
 *
 */
class UniversalWeatherRepositoryTest {
    
    private UniversalWeatherRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = new SnowflakeRepository();
    }
    
    @Test
    void testRepositoryIsInitialized() {
        assertNotNull(repository, "Repository should be initialized");
    }
    
    @Test
    void testIsHealthyReturnsFalseForStub() {
        assertFalse(repository.isHealthy(), 
                   "Stub repository should not be healthy");
    }
    
    @Test
    void testGetStatsReturnsValidStats() {
        RepositoryStats stats = repository.getStats();
        
        assertNotNull(stats, "Stats should not be null");
        assertEquals(0L, stats.getTotalRecordCount(), 
                    "Stub should have zero records");
        assertEquals(0, stats.getUniqueStationCount(), 
                    "Stub should have zero stations");
        assertNull(stats.getOldestRecordTime(), 
                  "Stub should have no oldest record time");
        assertNull(stats.getNewestRecordTime(), 
                  "Stub should have no newest record time");
    }
    
    @Test
    void testFindByStationAndTimeReturnsEmpty() {
        Optional<WeatherData> result = repository.findByStationAndTime(
            "KJFK", 
            LocalDateTime.now()
        );
        
        assertFalse(result.isPresent(), 
                   "Stub repository should return empty for queries");
    }
    
    @Test
    void testFindByStationAndTimeRangeReturnsEmptyList() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        
        List<WeatherData> results = repository.findByStationAndTimeRange(
            "KJFK", 
            start, 
            end
        );
        
        assertNotNull(results, "Results should not be null");
        assertTrue(results.isEmpty(), 
                  "Stub repository should return empty list");
    }
    
    @Test
    void testFindLatestByStationReturnsEmpty() {
        Optional<WeatherData> result = repository.findLatestByStation("KJFK");
        
        assertFalse(result.isPresent(), 
                   "Stub repository should return empty for latest query");
    }
    
    @Test
    void testFindBySourceAndTimeRangeReturnsEmptyList() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        
        List<WeatherData> results = repository.findBySourceAndTimeRange(
            WeatherDataSource.NOAA, 
            start, 
            end
        );
        
        assertNotNull(results, "Results should not be null");
        assertTrue(results.isEmpty(), 
                  "Stub repository should return empty list");
    }
    
    @Test
    void testFindByStationsAndTimeReturnsEmptyList() {
        List<String> stations = Arrays.asList("KJFK", "KLGA", "KEWR");
        LocalDateTime time = LocalDateTime.now();
        
        List<WeatherData> results = repository.findByStationsAndTime(
            stations, 
            time
        );
        
        assertNotNull(results, "Results should not be null");
        assertTrue(results.isEmpty(), 
                  "Stub repository should return empty list");
    }
    
    @Test
    void testFindByStationsAndTimeWithEmptyListReturnsEmpty() {
        List<String> stations = Collections.emptyList();
        LocalDateTime time = LocalDateTime.now();
        
        List<WeatherData> results = repository.findByStationsAndTime(
            stations, 
            time
        );
        
        assertNotNull(results, "Results should not be null");
        assertTrue(results.isEmpty(), 
                  "Query with empty station list should return empty");
    }
    
    @Test
    void testSaveThrowsUnsupportedOperation() {
        WeatherData data = null;
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class, 
            () -> repository.save(data),
            "Stub save() should throw UnsupportedOperationException"
        );
        
        assertNotNull(exception, "Exception should not be null");
    }
    
    @Test
    void testSaveBatchThrowsUnsupportedOperation() {
        List<WeatherData> dataList = Collections.emptyList();
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class, 
            () -> repository.saveBatch(dataList),
            "Stub saveBatch() should throw UnsupportedOperationException"
        );
        
        assertNotNull(exception, "Exception should not be null");
    }
    
    @Test
    void testDeleteOlderThanThrowsUnsupportedOperation() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(6);
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class, 
            () -> repository.deleteOlderThan(cutoff),
            "Stub deleteOlderThan() should throw UnsupportedOperationException"
        );
        
        assertNotNull(exception, "Exception should not be null");
    }
    
    @Test
    void testRepositoryStatsToString() {
        RepositoryStats stats = repository.getStats();
        String result = stats.toString();
        
        assertNotNull(result, "toString should not return null");
        assertTrue(result.contains("RepositoryStats"), 
                  "toString should contain class name");
    }
    
    @Test
    void testRepositoryStatsEqualsAndHashCode() {
        RepositoryStats stats1 = new RepositoryStats(100L, 
            LocalDateTime.now(), LocalDateTime.now(), 10L, 50L, 5, 1024L);
        RepositoryStats stats2 = new RepositoryStats(100L, 
            stats1.getOldestRecordTime(), stats1.getNewestRecordTime(), 
            10L, 50L, 5, 1024L);
        
        assertEquals(stats1, stats2, "Equal stats should be equal");
        assertEquals(stats1.hashCode(), stats2.hashCode(), 
                    "Equal stats should have same hash code");
    }
}
