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
import static org.assertj.core.api.Assertions.assertThat;

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
    @SuppressWarnings({"java:S5838", "java:S3415"}) // Intentional: argument order needed to test equals() implementation
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
    
    // ========== CONSTRUCTOR TESTS ==========

    @Test
    @DisplayName("Should create WeatherData with default constructor")
    void testDefaultConstructor() {
        TestWeatherData data = new TestWeatherData();
        
        assertNotNull(data.getId(), "ID should be auto-generated");
        assertNotNull(data.getIngestionTime(), "Ingestion time should be auto-set");
        assertNotNull(data.getMetadata(), "Metadata map should be initialized");
    }
    
    @Test
    @DisplayName("Should create WeatherData with parameterized constructor")
    void testParameterizedConstructor() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        assertNotNull(data.getId());
        assertNotNull(data.getIngestionTime());
        assertEquals(WeatherDataSource.NOAA, data.getSource());
        assertEquals("KJFK", data.getStationId());
        assertEquals(now, data.getObservationTime());
        assertEquals(ProcessingLayer.SPEED_LAYER, data.getProcessingLayer(), 
                    "Should default to SPEED_LAYER");
    }
    
    @Test
    @DisplayName("Should generate unique IDs for different instances")
    void testUniqueIds() {
        TestWeatherData data1 = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        TestWeatherData data2 = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        assertNotEquals(data1.getId(), data2.getId(), "Each instance should have unique ID");
    }
    
    @Test
    @DisplayName("Should set ingestion time close to creation time")
    void testIngestionTimeSetAutomatically() {
        Instant before = Instant.now();
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Instant after = Instant.now();
        
        Instant ingestionTime = data.getIngestionTime();
        
        assertNotNull(ingestionTime);
        assertFalse(ingestionTime.isBefore(before), "Ingestion time should not be before creation");
        assertFalse(ingestionTime.isAfter(after), "Ingestion time should not be after creation");
    }
    
    // ========== GETTER/SETTER TESTS ==========
    
    @Test
    @DisplayName("Should get and set processing layer")
    void testSetProcessingLayer() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        // Default should be SPEED_LAYER
        assertEquals(ProcessingLayer.SPEED_LAYER, data.getProcessingLayer());
        
        // Change to BATCH_LAYER
        data.setProcessingLayer(ProcessingLayer.BATCH_LAYER);
        assertEquals(ProcessingLayer.BATCH_LAYER, data.getProcessingLayer());
        
        // Change to SERVING_LAYER
        data.setProcessingLayer(ProcessingLayer.SERVING_LAYER);
        assertEquals(ProcessingLayer.SERVING_LAYER, data.getProcessingLayer());
    }

    @Test
    @DisplayName("Should get and set station ID")
    void testSetStationId() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        assertEquals("KJFK", data.getStationId());
        
        data.setStationId("KLGA");
        assertEquals("KLGA", data.getStationId());
    }
    
    @Test
    @DisplayName("Should get and set observation time")
    void testSetObservationTime() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        assertEquals(now, data.getObservationTime());
        
        Instant newTime = now.plusSeconds(3600);
        data.setObservationTime(newTime);
        assertEquals(newTime, data.getObservationTime());
    }
    
    @Test
    @DisplayName("Should get and set location")
    void testSetLocation() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        assertNull(data.getLocation(), "Location should initially be null");
        
        GeoLocation location = new GeoLocation(40.6413, -73.7781, 13);
        data.setLocation(location);
        
        assertEquals(location, data.getLocation());
        assertEquals(40.6413, data.getLocation().latitude());
        assertEquals(-73.7781, data.getLocation().longitude());
        assertEquals(13, data.getLocation().elevationMeters());
    }
    
    @Test
    @DisplayName("Should get and set raw data")
    void testSetRawData() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        assertNull(data.getRawData(), "Raw data should initially be null");
        
        String rawData = "METAR KJFK 121251Z 31008KT 10SM FEW250 M04/M17 A3034";
        data.setRawData(rawData);
        
        assertEquals(rawData, data.getRawData());
    }
    
    @Test
    @DisplayName("Should get and set quality flags")
    void testSetQualityFlags() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        assertNull(data.getQualityFlags(), "Quality flags should initially be null");
        
        data.setQualityFlags("AUTO");
        assertEquals("AUTO", data.getQualityFlags());
        
        data.setQualityFlags("CORRECTED");
        assertEquals("CORRECTED", data.getQualityFlags());
    }

    // ========== IMMUTABLE FIELDS TESTS ==========
    
    @Test
    @DisplayName("Should have immutable ID")
    void testIdIsImmutable() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        String id = data.getId();
        
        // Get ID again - should be same
        assertEquals(id, data.getId(), "ID should remain constant");
    }
    
    @Test
    @DisplayName("Should have immutable ingestion time")
    void testIngestionTimeIsImmutable() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        Instant ingestionTime = data.getIngestionTime();
        
        // Get ingestion time again - should be same
        assertEquals(ingestionTime, data.getIngestionTime(), "Ingestion time should remain constant");
    }
    
    // ========== TOSTRING TESTS ==========
    
    @Test
    @DisplayName("Should generate toString with all key fields")
    void testToString() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        data.setProcessingLayer(ProcessingLayer.BATCH_LAYER);
        
        String toString = data.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("TestWeatherData"), "Should contain class name");
        assertTrue(toString.contains("id="), "Should contain ID");
        assertTrue(toString.contains("NOAA"), "Should contain source");
        assertTrue(toString.contains("KJFK"), "Should contain station ID");
        assertTrue(toString.contains("BATCH_LAYER"), "Should contain processing layer");
    }
    
    @Test
    @DisplayName("Should handle toString with null fields")
    void testToStringWithNullFields() {
        TestWeatherData data = new TestWeatherData();
        
        String toString = data.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("TestWeatherData"));
        // Should not throw exception even with null fields
    }
    
    // ========== EQUALS AND HASHCODE ADDITIONAL TESTS ==========
    
    @Test
    @DisplayName("Should return false when comparing with different class")
    void testEqualsWithDifferentClass() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        // REMOVE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        //assertThat(data).isNotEqualTo("Not a WeatherData object");
        //assertThat(data).isNotEqualTo(42);
        
        assertThat(data)
                .isNotEqualTo("Not a WeatherData object")
                .isNotEqualTo(42);
    }

    @Test
    @DisplayName("Should return false when IDs differ")
    void testEqualsWithDifferentIds() {
        TestWeatherData data1 = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        TestWeatherData data2 = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        // Different auto-generated IDs
        assertNotEquals(data1, data2);
        assertNotEquals(data1.hashCode(), data2.hashCode());
    }
    
    @Test
    @DisplayName("Should have consistent hashCode")
    void testHashCodeConsistency() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        int hash1 = data.hashCode();
        int hash2 = data.hashCode();
        
        assertEquals(hash1, hash2, "hashCode should be consistent");
    }
    
    // ========== NULL HANDLING TESTS ==========
    
    @Test
    @DisplayName("Should handle setting null values")
    void testSetNullValues() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        // Should not throw exceptions
        data.setSource(null);
        data.setProcessingLayer(null);
        data.setStationId(null);
        data.setObservationTime(null);
        data.setLocation(null);
        data.setRawData(null);
        data.setQualityFlags(null);
        data.setMetadata(null);
        
        assertNull(data.getSource());
        assertNull(data.getProcessingLayer());
        assertNull(data.getStationId());
        assertNull(data.getObservationTime());
        assertNull(data.getLocation());
        assertNull(data.getRawData());
        assertNull(data.getQualityFlags());
        assertNull(data.getMetadata());
    }
    
    // ========== METADATA EDGE CASES ==========
    
    @Test
    @DisplayName("Should handle overwriting metadata values")
    void testMetadataOverwrite() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        data.addMetadata("key", "value1");
        assertEquals("value1", data.getMetadata().get("key"));
        
        data.addMetadata("key", "value2");
        assertEquals("value2", data.getMetadata().get("key"));
        assertEquals(1, data.getMetadata().size(), "Should still have only one key");
    }

    @Test
    @DisplayName("Should handle setting empty metadata map")
    void testSetEmptyMetadata() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        
        data.addMetadata("key", "value");
        assertEquals(1, data.getMetadata().size());
        
        data.setMetadata(new HashMap<>());
        assertEquals(0, data.getMetadata().size());
    }
}
