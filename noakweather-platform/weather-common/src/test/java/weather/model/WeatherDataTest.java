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
package weather.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WeatherData base class.
 * Uses TestWeatherData to test abstract parent behavior without subclass overrides.
 * 
 * @author bclasky1539
 *
 */
class WeatherDataTest {
    
    private Instant now;
    
    @BeforeEach
    void setUp() {
        now = Instant.now();
    }
    
    @Test
    @DisplayName("Should return true when forcing same ID via reflection")
    void testEqualsForcedSameIdViaReflection() throws Exception {
        TestWeatherData data1 = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        TestWeatherData data2 = new TestWeatherData(WeatherDataSource.NOAA, "KLGA", now);
        
        // Use reflection to force the same ID
        java.lang.reflect.Field idField = WeatherData.class.getDeclaredField("id");
        idField.setAccessible(true);
        
        String sameId = "test-id-12345";
        idField.set(data1, sameId);
        idField.set(data2, sameId);
        
        // Now they should be equal (same ID)
        assertEquals(data1, data2, "Objects with same ID should be equal");
        assertEquals(data1.hashCode(), data2.hashCode(), 
                    "Objects with same ID should have same hash code");
    }
    
    @Test
    @DisplayName("Should handle equals with both IDs null via reflection")
    void testEqualsBothNullIdsViaReflection() throws Exception {
        TestWeatherData data1 = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        TestWeatherData data2 = new TestWeatherData(WeatherDataSource.NOAA, "KLGA", now);
        
        // Use reflection to set null IDs
        java.lang.reflect.Field idField = WeatherData.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(data1, null);
        idField.set(data2, null);
        
        // Both have null IDs, should be equal
        assertEquals(data1, data2, "Objects with both null IDs should be equal");
    }
    
    @Test
    @DisplayName("Should handle equals when one ID is null via reflection")
    void testEqualsOneNullIdViaReflection() throws Exception {
        TestWeatherData data1 = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        TestWeatherData data2 = new TestWeatherData(WeatherDataSource.NOAA, "KLGA", now);
        
        // Use reflection to set one ID to null
        java.lang.reflect.Field idField = WeatherData.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(data1, null);
        
        // One null, one not - should not be equal
        assertNotEquals(data1, data2, 
                       "Object with null ID should not equal object with non-null ID");
    }
    
    @Test
    @DisplayName("Should return false when comparing with null")
    void testEqualsWithNull() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        assertNotEquals(data, null);
    }
    
    @Test
    @DisplayName("Should return true for same reference")
    void testEqualsSameReference() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        assertEquals(data, data);
    }
    
    @Test
    @DisplayName("Should set and get source")
    void testSetSource() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        data.setSource(WeatherDataSource.INTERNAL);
        assertEquals(WeatherDataSource.INTERNAL, data.getSource());
    }
    
    @Test
    @DisplayName("Should add metadata when map is null")
    void testAddMetadataWhenMapIsNull() throws Exception {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        // Force metadata to null using reflection
        java.lang.reflect.Field metadataField = WeatherData.class.getDeclaredField("metadata");
        metadataField.setAccessible(true);
        metadataField.set(data, null);
        
        data.addMetadata("test_key", "test_value");
        
        assertNotNull(data.getMetadata());
        assertEquals("test_value", data.getMetadata().get("test_key"));
    }
    
    @Test
    @DisplayName("Should add metadata when map already exists")
    void testAddMetadataWhenMapExists() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        data.addMetadata("key1", "value1");
        data.addMetadata("key2", "value2");
        
        assertEquals(2, data.getMetadata().size());
        assertEquals("value1", data.getMetadata().get("key1"));
        assertEquals("value2", data.getMetadata().get("key2"));
    }
    
    @Test
    @DisplayName("Should set entire metadata map")
    void testSetMetadata() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", 123);
        
        data.setMetadata(metadata);
        
        assertEquals(2, data.getMetadata().size());
        assertEquals("value1", data.getMetadata().get("key1"));
        assertEquals(123, data.getMetadata().get("key2"));
    }
}
