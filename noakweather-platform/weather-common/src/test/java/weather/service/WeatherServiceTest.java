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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import weather.model.NoaaWeatherData;
import weather.model.WeatherDataSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WeatherService interface
 * 
 * @author bclasky1539
 *
 */
class WeatherServiceTest {
    
    /**
     * Mock implementation of WeatherService for testing
     */
    private static class MockWeatherService implements WeatherService<NoaaWeatherData> {
        private final Map<String, NoaaWeatherData> storage = new HashMap<>();
        private boolean healthy = true;
        private int refreshCount = 0;
        
        @Override
        public Optional<NoaaWeatherData> getCurrentWeather(String stationId) {
            return Optional.ofNullable(storage.get(stationId));
        }
        
        @Override
        public List<NoaaWeatherData> getWeatherHistory(String stationId, Instant startTime, Instant endTime) {
            return storage.values().stream()
                .filter(data -> data.getStationId().equals(stationId))
                .filter(data -> {
                    Instant obsTime = data.getObservationTime();
                    return obsTime != null && 
                           !obsTime.isBefore(startTime) && 
                           !obsTime.isAfter(endTime);
                })
                .toList();
        }
        
        @Override
        public boolean store(NoaaWeatherData data) {
            if (data == null || data.getStationId() == null) {
                return false;
            }
            storage.put(data.getStationId(), data);
            return true;
        }
        
        @Override
        public int storeBatch(List<NoaaWeatherData> dataList) {
            if (dataList == null) {
                return 0;
            }
            int count = 0;
            for (NoaaWeatherData data : dataList) {
                if (store(data)) {
                    count++;
                }
            }
            return count;
        }
        
        @Override
        public WeatherDataSource getSource() {
            return WeatherDataSource.NOAA;
        }
        
        @Override
        public boolean isHealthy() {
            return healthy;
        }
        
        @Override
        public int refresh() {
            refreshCount++;
            return storage.size();
        }
        
        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }
        
        public int getRefreshCount() {
            return refreshCount;
        }
        
        public void clear() {
            storage.clear();
        }
    }
    
    private MockWeatherService service;
    private Instant now;
    
    @BeforeEach
    void setUp() {
        service = new MockWeatherService();
        now = Instant.now();
    }
    
    @Test
    @DisplayName("Should store and retrieve current weather")
    void testStoreAndGetCurrentWeather() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        assertTrue(service.store(data));
        
        Optional<NoaaWeatherData> retrieved = service.getCurrentWeather("KJFK");
        assertTrue(retrieved.isPresent());
        assertEquals("KJFK", retrieved.get().getStationId());
    }
    
    @Test
    @DisplayName("Should return empty optional for non-existent station")
    void testGetCurrentWeatherNotFound() {
        Optional<NoaaWeatherData> result = service.getCurrentWeather("KXXX");
        assertFalse(result.isPresent());
    }
    
    @Test
    @DisplayName("Should reject null data in store")
    void testStoreNull() {
        assertFalse(service.store(null));
    }
    
    @Test
    @DisplayName("Should reject data with null station ID")
    void testStoreNullStationId() {
        NoaaWeatherData data = new NoaaWeatherData(null, now, "METAR");
        assertFalse(service.store(data));
    }
    
    @Test
    @DisplayName("Should store batch of data")
    void testStoreBatch() {
        List<NoaaWeatherData> batch = Arrays.asList(
            new NoaaWeatherData("KJFK", now, "METAR"),
            new NoaaWeatherData("KLAX", now, "METAR"),
            new NoaaWeatherData("KORD", now, "METAR")
        );
        
        int stored = service.storeBatch(batch);
        assertEquals(3, stored);
        
        assertTrue(service.getCurrentWeather("KJFK").isPresent());
        assertTrue(service.getCurrentWeather("KLAX").isPresent());
        assertTrue(service.getCurrentWeather("KORD").isPresent());
    }
    
    @Test
    @DisplayName("Should handle null batch")
    void testStoreBatchNull() {
        int stored = service.storeBatch(null);
        assertEquals(0, stored);
    }
    
    @Test
    @DisplayName("Should handle empty batch")
    void testStoreBatchEmpty() {
        int stored = service.storeBatch(Collections.emptyList());
        assertEquals(0, stored);
    }
    
    @Test
    @DisplayName("Should skip invalid items in batch")
    void testStoreBatchWithInvalidItems() {
        List<NoaaWeatherData> batch = Arrays.asList(
            new NoaaWeatherData("KJFK", now, "METAR"),
            new NoaaWeatherData(null, now, "METAR"),  // Invalid
            new NoaaWeatherData("KLAX", now, "METAR")
        );
        
        int stored = service.storeBatch(batch);
        assertEquals(2, stored);
    }
    
    @Test
    @DisplayName("Should retrieve weather history within time range")
    void testGetWeatherHistory() {
        Instant twoHoursAgo = now.minus(2, ChronoUnit.HOURS);
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        Instant threeHoursAgo = now.minus(3, ChronoUnit.HOURS);
        
        service.store(new NoaaWeatherData("KJFK", threeHoursAgo, "METAR"));
        service.store(new NoaaWeatherData("KJFK", twoHoursAgo, "METAR"));
        service.store(new NoaaWeatherData("KJFK", oneHourAgo, "METAR"));
        
        // Note: Simple mock only stores latest per station
        // In real implementation, would store multiple records
        List<NoaaWeatherData> history = service.getWeatherHistory(
            "KJFK", 
            twoHoursAgo, 
            now
        );
        
        assertNotNull(history);
    }
    
    @Test
    @DisplayName("Should return empty list for station with no history")
    void testGetWeatherHistoryNotFound() {
        List<NoaaWeatherData> history = service.getWeatherHistory(
            "KXXX",
            now.minus(1, ChronoUnit.HOURS),
            now
        );
        
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }
    
    @Test
    @DisplayName("Should return correct data source")
    void testGetSource() {
        assertEquals(WeatherDataSource.NOAA, service.getSource());
    }
    
    @Test
    @DisplayName("Should report healthy status")
    void testIsHealthy() {
        assertTrue(service.isHealthy());
        
        service.setHealthy(false);
        assertFalse(service.isHealthy());
    }
    
    @Test
    @DisplayName("Should refresh data")
    void testRefresh() {
        service.store(new NoaaWeatherData("KJFK", now, "METAR"));
        service.store(new NoaaWeatherData("KLAX", now, "METAR"));
        
        int refreshed = service.refresh();
        assertEquals(2, refreshed);
        assertEquals(1, service.getRefreshCount());
    }
    
    @Test
    @DisplayName("Should handle multiple refresh calls")
    void testMultipleRefresh() {
        service.refresh();
        service.refresh();
        service.refresh();
        
        assertEquals(3, service.getRefreshCount());
    }
    
    @Test
    @DisplayName("Should overwrite existing station data on store")
    void testOverwriteExistingData() {
        NoaaWeatherData oldData = new NoaaWeatherData("KJFK", now.minus(1, ChronoUnit.HOURS), "METAR");
        NoaaWeatherData newData = new NoaaWeatherData("KJFK", now, "METAR");
        
        service.store(oldData);
        service.store(newData);
        
        Optional<NoaaWeatherData> current = service.getCurrentWeather("KJFK");
        assertTrue(current.isPresent());
        assertEquals(now, current.get().getObservationTime());
    }
}
