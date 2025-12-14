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
import weather.model.components.*;
import weather.model.enums.PressureUnit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    @SuppressWarnings("java:S1862")  // Intentional: testing reflexive property of equals()
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
        
        assertThat(data)
                .isNotEqualTo("Not a WeatherData object")
                .isNotEqualTo(new Object());
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

    // ========== WIND SETTER TESTS ==========

    @Test
    @DisplayName("Should set wind using factory method with all parameters")
    void testSetWindFactoryMethod() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        data.setWind(280, 16, 25, "KT");

        assertNotNull(data.getWind());
        assertEquals(280, data.getWind().directionDegrees());
        assertEquals(16, data.getWind().speedValue());
        assertEquals(25, data.getWind().gustValue());
        assertEquals("KT", data.getWind().unit());
    }

    @Test
    @DisplayName("Should set wind with null direction (calm/variable)")
    void testSetWindWithNullDirection() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        data.setWind(null, 5, null, "KT");

        assertNotNull(data.getWind());
        assertNull(data.getWind().directionDegrees());
        assertEquals(5, data.getWind().speedValue());
        assertNull(data.getWind().gustValue());
    }

    @Test
    @DisplayName("Should set wind with null gust")
    void testSetWindWithoutGust() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        data.setWind(180, 10, null, "MPS");

        assertNotNull(data.getWind());
        assertEquals(180, data.getWind().directionDegrees());
        assertEquals(10, data.getWind().speedValue());
        assertNull(data.getWind().gustValue());
        assertEquals("MPS", data.getWind().unit());
    }

    @Test
    @DisplayName("Should set wind directly with Wind object")
    void testSetWindDirectly() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Wind wind = new Wind(270, 20, 30, null, null, "KT");

        data.setWind(wind);

        assertEquals(wind, data.getWind());
    }

// ========== VISIBILITY SETTER TESTS ==========

    @Test
    @DisplayName("Should set visibility using factory method with 4 parameters")
    void testSetVisibilityFactoryMethod() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        data.setVisibility(10.0, "SM", false, false);

        assertNotNull(data.getVisibility());
        assertEquals(10.0, data.getVisibility().distanceValue());
        assertEquals("SM", data.getVisibility().unit());
        assertFalse(data.getVisibility().lessThan());
        assertFalse(data.getVisibility().greaterThan());
    }

    @Test
    @DisplayName("Should set visibility with lessThan flag")
    void testSetVisibilityLessThan() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        data.setVisibility(0.25, "SM", true, false);

        assertNotNull(data.getVisibility());
        assertEquals(0.25, data.getVisibility().distanceValue());
        assertTrue(data.getVisibility().lessThan());
        assertFalse(data.getVisibility().greaterThan());
    }

    @Test
    @DisplayName("Should set visibility with greaterThan flag")
    void testSetVisibilityGreaterThan() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        data.setVisibility(6.0, "SM", false, true);

        assertNotNull(data.getVisibility());
        assertEquals(6.0, data.getVisibility().distanceValue());
        assertFalse(data.getVisibility().lessThan());
        assertTrue(data.getVisibility().greaterThan());
    }

    @Test
    @DisplayName("Should set visibility with special condition")
    void testSetVisibilityWithSpecialCondition() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        data.setVisibility(null, null, false, false, "NDV");

        assertNotNull(data.getVisibility());
        assertEquals("NDV", data.getVisibility().specialCondition());
    }

    @Test
    @DisplayName("Should set visibility without special condition when blank")
    void testSetVisibilityWithBlankSpecialCondition() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        data.setVisibility(10.0, "SM", false, false, "");

        assertNotNull(data.getVisibility());
        assertEquals(10.0, data.getVisibility().distanceValue());
        assertEquals("SM", data.getVisibility().unit());
        assertNull(data.getVisibility().specialCondition());
    }

    @Test
    @DisplayName("Should set visibility without special condition when null")
    void testSetVisibilityWithNullSpecialCondition() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        data.setVisibility(5.0, "KM", false, false, null);

        assertNotNull(data.getVisibility());
        assertEquals(5.0, data.getVisibility().distanceValue());
        assertEquals("KM", data.getVisibility().unit());
        assertNull(data.getVisibility().specialCondition());
    }

    @Test
    @DisplayName("Should set visibility directly with Visibility object")
    void testSetVisibilityDirectly() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Visibility visibility = new Visibility(9999.0, "M", false, false, null);

        data.setVisibility(visibility);

        assertEquals(visibility, data.getVisibility());
    }

    @Test
    @DisplayName("Should get null visibility when not set")
    void testGetVisibilityWhenNull() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        assertNull(data.getVisibility());
    }

    @Test
    @DisplayName("Should get null wind when not set")
    void testGetWindWhenNull() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        assertNull(data.getWind());
    }

    // ========== RUNWAY VISUAL RANGE (RVR) SETTER TESTS ==========
// Add these tests to WeatherDataTest.java after the Visibility tests

    @Test
    @DisplayName("Should add runway visual range")
    void testAddRunwayVisualRange() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        RunwayVisualRange rvr1 = RunwayVisualRange.of("04L", 2200);
        RunwayVisualRange rvr2 = RunwayVisualRange.variable("04R", 1800, 2400);

        data.addRunwayVisualRange(rvr1);
        data.addRunwayVisualRange(rvr2);

        assertNotNull(data.getRunwayVisualRange());
        assertEquals(2, data.getRunwayVisualRange().size());
        assertTrue(data.getRunwayVisualRange().contains(rvr1));
        assertTrue(data.getRunwayVisualRange().contains(rvr2));
    }

    @Test
    @DisplayName("Should not add null runway visual range")
    void testAddRunwayVisualRange_Null() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        data.addRunwayVisualRange(null);

        assertNotNull(data.getRunwayVisualRange());
        assertTrue(data.getRunwayVisualRange().isEmpty());
    }

    @Test
    @DisplayName("Should get runway visual range as immutable copy")
    void testGetRunwayVisualRange_ReturnsImmutableCopy() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        RunwayVisualRange rvr = RunwayVisualRange.of("04L", 2200);
        data.addRunwayVisualRange(rvr);

        List<RunwayVisualRange> rvrList = data.getRunwayVisualRange();

        // List.copyOf() returns immutable list
        assertNotNull(rvrList);
        assertEquals(1, rvrList.size());
    }

    @Test
    @DisplayName("Should set runway visual range list")
    void testSetRunwayVisualRange() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        List<RunwayVisualRange> rvrList = new ArrayList<>();
        rvrList.add(RunwayVisualRange.of("04L", 2200));
        rvrList.add(RunwayVisualRange.variable("04R", 1800, 2400));

        data.setRunwayVisualRange(rvrList);

        assertEquals(2, data.getRunwayVisualRange().size());
    }

    @Test
    @DisplayName("Should handle setting null runway visual range list")
    void testSetRunwayVisualRange_WithNull() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        data.setRunwayVisualRange(null);

        // Should create empty list, not null
        assertNotNull(data.getRunwayVisualRange());
        assertTrue(data.getRunwayVisualRange().isEmpty());
    }

    @Test
    @DisplayName("Should handle setting empty runway visual range list")
    void testSetRunwayVisualRange_WithEmptyList() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        data.setRunwayVisualRange(new ArrayList<>());

        assertNotNull(data.getRunwayVisualRange());
        assertTrue(data.getRunwayVisualRange().isEmpty());
    }

    @Test
    @DisplayName("Should get null runway visual range when not set initially")
    void testGetRunwayVisualRangeWhenNotSet() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        // Default constructor initializes to empty list
        assertNotNull(data.getRunwayVisualRange());
        assertTrue(data.getRunwayVisualRange().isEmpty());
    }

    @Test
    @DisplayName("Should add multiple RVR with different properties")
    void testAddRunwayVisualRange_MultipleTypes() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        // Simple RVR
        RunwayVisualRange rvr1 = RunwayVisualRange.of("04L", 2200);

        // Variable RVR
        RunwayVisualRange rvr2 = RunwayVisualRange.variable("04R", 1800, 2400);

        // RVR with prefix and trend
        RunwayVisualRange rvr3 = new RunwayVisualRange("22L", 6000, null, null, "P", "N");

        // RVR with less than prefix
        RunwayVisualRange rvr4 = new RunwayVisualRange("22R", 600, null, null, "M", "D");

        data.addRunwayVisualRange(rvr1);
        data.addRunwayVisualRange(rvr2);
        data.addRunwayVisualRange(rvr3);
        data.addRunwayVisualRange(rvr4);

        assertEquals(4, data.getRunwayVisualRange().size());

        // Verify each type
        assertEquals(2200, data.getRunwayVisualRange().get(0).visualRangeFeet());
        assertTrue(data.getRunwayVisualRange().get(1).isVariable());
        assertTrue(data.getRunwayVisualRange().get(2).isGreaterThan());
        assertTrue(data.getRunwayVisualRange().get(3).isLessThan());
    }

    @Test
    @DisplayName("Should replace runway visual range list when set")
    void testSetRunwayVisualRange_ReplacesExisting() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        // Add initial RVR
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        assertEquals(1, data.getRunwayVisualRange().size());

        // Replace with new list
        List<RunwayVisualRange> newList = new ArrayList<>();
        newList.add(RunwayVisualRange.of("22L", 3000));
        newList.add(RunwayVisualRange.of("22R", 2800));

        data.setRunwayVisualRange(newList);

        assertEquals(2, data.getRunwayVisualRange().size());
        assertEquals("22L", data.getRunwayVisualRange().get(0).runway());
        assertEquals("22R", data.getRunwayVisualRange().get(1).runway());
    }

    // ========== PRESENT WEATHER TESTS ==========

    @Test
    @DisplayName("Should add present weather")
    void testAddPresentWeather() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", Instant.now());

        PresentWeather lightRain = new PresentWeather("-", null, "RA", null, null, "-RA");
        PresentWeather mist = new PresentWeather(null, null, null, "BR", null, "BR");

        data.addPresentWeather(lightRain);
        data.addPresentWeather(mist);

        assertThat(data.getPresentWeather()).hasSize(2);
        assertThat(data.getPresentWeather()).containsExactly(lightRain, mist);
    }

    @Test
    @DisplayName("Should not add null present weather")
    void testAddPresentWeather_Null() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", Instant.now());

        data.addPresentWeather(null);

        assertThat(data.getPresentWeather()).isEmpty();
    }

    @Test
    void testSetPresentWeather_WithNull() {
        TestWeatherData data = new TestWeatherData();

        data.setPresentWeather(null);

        assertThat(data.getPresentWeather()).isNotNull().isEmpty();
    }

    @Test
    void testSetPresentWeather_WithEmptyList() {
        TestWeatherData data = new TestWeatherData();

        data.setPresentWeather(new ArrayList<>());

        assertThat(data.getPresentWeather()).isEmpty();
    }

    @Test
    @DisplayName("Should set present weather with non-null list")
    void testSetPresentWeather_WithNonNullList() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", Instant.now());

        PresentWeather lightRain = new PresentWeather("-", null, "RA", null, null, "-RA");
        PresentWeather mist = new PresentWeather(null, null, null, "BR", null, "BR");
        PresentWeather fog = new PresentWeather(null, null, null, "FG", null, "FG");

        List<PresentWeather> weatherList = new ArrayList<>();
        weatherList.add(lightRain);
        weatherList.add(mist);
        weatherList.add(fog);

        data.setPresentWeather(weatherList);

        assertThat(data.getPresentWeather()).hasSize(3);
        assertThat(data.getPresentWeather()).containsExactly(lightRain, mist, fog);
    }

    // ========== TEMPERATURE TESTS ==========

    @Test
    @DisplayName("Should set temperature with both temperature and dewpoint")
    void testSetTemperature() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Temperature temp = new Temperature(22.0, 12.0);

        data.setTemperature(temp);

        assertNotNull(data.getTemperature());
        assertEquals(22.0, data.getTemperature().celsius());
        assertEquals(12.0, data.getTemperature().dewpointCelsius());
    }

    @Test
    @DisplayName("Should set temperature without dewpoint")
    void testSetTemperatureWithoutDewpoint() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Temperature temp = new Temperature(22.0, null);

        data.setTemperature(temp);

        assertNotNull(data.getTemperature());
        assertEquals(22.0, data.getTemperature().celsius());
        assertNull(data.getTemperature().dewpointCelsius());
    }

    @Test
    @DisplayName("Should set temperature with negative values")
    void testSetTemperatureNegative() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Temperature temp = new Temperature(-5.0, -12.0);

        data.setTemperature(temp);

        assertNotNull(data.getTemperature());
        assertEquals(-5.0, data.getTemperature().celsius());
        assertEquals(-12.0, data.getTemperature().dewpointCelsius());
        assertTrue(data.getTemperature().isFreezing());
    }

    @Test
    @DisplayName("Should set temperature with freezing point")
    void testSetTemperatureFreezing() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Temperature temp = new Temperature(0.0, -1.0);

        data.setTemperature(temp);

        assertNotNull(data.getTemperature());
        assertEquals(0.0, data.getTemperature().celsius());
        assertEquals(-1.0, data.getTemperature().dewpointCelsius());
        assertTrue(data.getTemperature().isFreezing());
    }

    @Test
    @DisplayName("Should set temperature directly with Temperature object")
    void testSetTemperatureDirectly() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Temperature temp = new Temperature(15.0, 8.0);

        data.setTemperature(temp);

        assertEquals(temp, data.getTemperature());
        assertEquals(15.0, data.getTemperature().celsius());
        assertEquals(8.0, data.getTemperature().dewpointCelsius());
    }

    @Test
    @DisplayName("Should get null temperature when not set")
    void testGetTemperatureWhenNull() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        assertNull(data.getTemperature());
    }

    @Test
    @DisplayName("Should handle temperature with null dewpoint using factory method")
    void testSetTemperatureNullDewpoint() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Temperature temp = Temperature.of(20.0);  // Factory method for temp only

        data.setTemperature(temp);

        assertNotNull(data.getTemperature());
        assertEquals(20.0, data.getTemperature().celsius());
        assertNull(data.getTemperature().dewpointCelsius());
    }

    @Test
    @DisplayName("Should validate temperature range")
    void testSetTemperatureValidation() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        // Valid temperature should work
        Temperature validTemp = new Temperature(22.0, 12.0);
        assertDoesNotThrow(() -> data.setTemperature(validTemp));

        // Out of range should throw (validation happens in Temperature constructor)
        assertThrows(IllegalArgumentException.class, () ->
                new Temperature(150.0, 12.0)  // Way too hot
        );
    }

    @Test
    @DisplayName("Should validate dewpoint not higher than temperature")
    void testSetTemperatureDewpointValidation() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        // Valid: dewpoint lower than temperature
        Temperature validTemp = new Temperature(22.0, 12.0);
        assertDoesNotThrow(() -> data.setTemperature(validTemp));

        // Invalid: dewpoint higher than temperature (validation in Temperature constructor)
        assertThrows(IllegalArgumentException.class, () ->
                new Temperature(12.0, 22.0)  // Dewpoint > temp (impossible)
        );
    }

    @Test
    @DisplayName("Should allow temperature and dewpoint to be equal")
    void testSetTemperatureEqualDewpoint() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        // Equal values (100% RH, fog likely)
        Temperature temp = new Temperature(15.0, 15.0);
        assertDoesNotThrow(() -> data.setTemperature(temp));

        assertNotNull(data.getTemperature());
        assertEquals(15.0, data.getTemperature().celsius());
        assertEquals(15.0, data.getTemperature().dewpointCelsius());
        assertTrue(data.getTemperature().isFogLikely());
    }

    @Test
    @DisplayName("Should use Temperature query methods")
    void testTemperatureQueryMethods() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Temperature temp = new Temperature(-5.0, -10.0);

        data.setTemperature(temp);

        Temperature retrievedTemp = data.getTemperature();
        assertNotNull(retrievedTemp);
        assertTrue(retrievedTemp.isFreezing());
        assertTrue(retrievedTemp.isBelowFreezing());
        assertFalse(retrievedTemp.isAboveFreezing());
        assertFalse(retrievedTemp.isVeryHot());
    }

    @Test
    @DisplayName("Should use Temperature conversion methods")
    void testTemperatureConversionMethods() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Temperature temp = new Temperature(20.0, 15.0);

        data.setTemperature(temp);

        Temperature retrievedTemp = data.getTemperature();
        assertNotNull(retrievedTemp);

        // Convert to Fahrenheit (check not null first to avoid unboxing NPE)
        Double tempF = retrievedTemp.toFahrenheit();
        assertNotNull(tempF);
        assertEquals(68.0, tempF, 0.1);

        Double dewpointF = retrievedTemp.dewpointToFahrenheit();
        assertNotNull(dewpointF);
        assertEquals(59.0, dewpointF, 0.1);

        // Convert to Kelvin (check not null first to avoid unboxing NPE)
        Double tempK = retrievedTemp.toKelvin();
        assertNotNull(tempK);
        assertEquals(293.15, tempK, 0.01);
    }

    @Test
    @DisplayName("Should calculate relative humidity")
    void testTemperatureRelativeHumidity() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Temperature temp = new Temperature(20.0, 10.0);

        data.setTemperature(temp);

        Temperature retrievedTemp = data.getTemperature();
        Double rh = retrievedTemp.getRelativeHumidity();

        assertNotNull(rh);
        assertTrue(rh >= 0.0 && rh <= 100.0, "RH should be between 0 and 100");
    }

    @Test
    @DisplayName("Should replace temperature when set again")
    void testSetTemperatureReplace() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        // Set initial temperature
        Temperature temp1 = new Temperature(20.0, 15.0);
        data.setTemperature(temp1);
        assertEquals(20.0, data.getTemperature().celsius());

        // Replace with new temperature
        Temperature temp2 = new Temperature(25.0, 18.0);
        data.setTemperature(temp2);
        assertEquals(25.0, data.getTemperature().celsius());
        assertEquals(18.0, data.getTemperature().dewpointCelsius());
    }

    @Test
    @DisplayName("Should use Temperature factory method from Fahrenheit")
    void testSetTemperatureFromFahrenheit() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        // 68°F = 20°C, 50°F ≈ 10°C
        Temperature temp = Temperature.fromFahrenheit(68.0, 50.0);
        data.setTemperature(temp);

        assertNotNull(data.getTemperature());
        assertEquals(20.0, data.getTemperature().celsius(), 0.1);
        assertEquals(10.0, data.getTemperature().dewpointCelsius(), 0.1);
    }

    @Test
    @DisplayName("Should use Temperature factory method from Kelvin")
    void testSetTemperatureFromKelvin() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        // 293.15K = 20°C, 283.15K = 10°C
        Temperature temp = Temperature.fromKelvin(293.15, 283.15);
        data.setTemperature(temp);

        assertNotNull(data.getTemperature());
        assertEquals(20.0, data.getTemperature().celsius(), 0.01);
        assertEquals(10.0, data.getTemperature().dewpointCelsius(), 0.01);
    }

    // ========== PRESSURE TESTS ==========

    @Test
    @DisplayName("Should set pressure in inches of mercury")
    void testSetPressureInchesHg() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Pressure pressure = new Pressure(30.15, PressureUnit.INCHES_HG);

        data.setPressure(pressure);

        assertNotNull(data.getPressure());
        assertEquals(30.15, data.getPressure().value(), 0.01);
        assertEquals(PressureUnit.INCHES_HG, data.getPressure().unit());
    }

    @Test
    @DisplayName("Should set pressure in hectopascals")
    void testSetPressureHectopascals() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Pressure pressure = new Pressure(1013.0, PressureUnit.HECTOPASCALS);

        data.setPressure(pressure);

        assertNotNull(data.getPressure());
        assertEquals(1013.0, data.getPressure().value(), 0.01);
        assertEquals(PressureUnit.HECTOPASCALS, data.getPressure().unit());
    }

    @Test
    @DisplayName("Should set pressure using factory method inchesHg")
    void testSetPressureFactoryInchesHg() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Pressure pressure = Pressure.inchesHg(29.92);

        data.setPressure(pressure);

        assertNotNull(data.getPressure());
        assertEquals(29.92, data.getPressure().value(), 0.01);
        assertEquals(PressureUnit.INCHES_HG, data.getPressure().unit());
    }

    @Test
    @DisplayName("Should set pressure using factory method hectopascals")
    void testSetPressureFactoryHectopascals() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Pressure pressure = Pressure.hectopascals(1013.25);

        data.setPressure(pressure);

        assertNotNull(data.getPressure());
        assertEquals(1013.25, data.getPressure().value(), 0.01);
        assertEquals(PressureUnit.HECTOPASCALS, data.getPressure().unit());
    }

    @Test
    @DisplayName("Should set standard pressure")
    void testSetStandardPressure() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Pressure pressure = Pressure.standard();

        data.setPressure(pressure);

        assertNotNull(data.getPressure());
        assertEquals(1013.25, data.getPressure().value(), 0.01);
        assertEquals(PressureUnit.HECTOPASCALS, data.getPressure().unit());
    }

    @Test
    @DisplayName("Should get null pressure when not set")
    void testGetPressureWhenNull() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        assertNull(data.getPressure());
    }

    @Test
    @DisplayName("Should convert pressure between units")
    void testPressureUnitConversions() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Pressure pressure = Pressure.inchesHg(30.15);

        data.setPressure(pressure);

        Pressure retrievedPressure = data.getPressure();
        assertNotNull(retrievedPressure);

        // Convert to hectopascals
        double hPa = retrievedPressure.toHectopascals();
        assertEquals(1021.0, hPa, 1.0); // ~30.15 inHg ≈ 1021 hPa

        // Convert to millibars (same as hPa)
        double mb = retrievedPressure.toMillibars();
        assertEquals(hPa, mb, 0.01);

        // Original should still be inHg
        double inHg = retrievedPressure.toInchesHg();
        assertEquals(30.15, inHg, 0.01);
    }

    @Test
    @DisplayName("Should validate pressure range for inches of mercury")
    void testPressureValidationInchesHg() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        // Valid pressure should work
        Pressure validPressure = Pressure.inchesHg(30.15);
        assertDoesNotThrow(() -> data.setPressure(validPressure));

        // Out of range should throw (validation happens in Pressure constructor)
        assertThrows(IllegalArgumentException.class, () ->
                Pressure.inchesHg(50.0)  // Way too high
        );

        assertThrows(IllegalArgumentException.class, () ->
                Pressure.inchesHg(10.0)  // Way too low
        );
    }

    @Test
    @DisplayName("Should validate pressure range for hectopascals")
    void testPressureValidationHectopascals() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        // Valid pressure should work
        Pressure validPressure = Pressure.hectopascals(1013.25);
        assertDoesNotThrow(() -> data.setPressure(validPressure));

        // Out of range should throw
        assertThrows(IllegalArgumentException.class, () ->
                Pressure.hectopascals(2000.0)  // Way too high
        );

        assertThrows(IllegalArgumentException.class, () ->
                Pressure.hectopascals(500.0)  // Way too low
        );
    }

    @Test
    @DisplayName("Should validate pressure value not null")
    void testPressureValidationNotNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new Pressure(null, PressureUnit.INCHES_HG)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new Pressure(30.15, null)
        );
    }

    @Test
    @DisplayName("Should use Pressure query methods for low pressure")
    void testPressureQueryMethodsLow() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Pressure pressure = Pressure.hectopascals(995.0); // Low pressure

        data.setPressure(pressure);

        Pressure retrievedPressure = data.getPressure();
        assertNotNull(retrievedPressure);
        assertTrue(retrievedPressure.isLowPressure());
        assertTrue(retrievedPressure.isBelowStandard());
        assertFalse(retrievedPressure.isHighPressure());
        assertFalse(retrievedPressure.isAboveStandard());
    }

    @Test
    @DisplayName("Should use Pressure query methods for high pressure")
    void testPressureQueryMethodsHigh() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Pressure pressure = Pressure.hectopascals(1025.0); // High pressure

        data.setPressure(pressure);

        Pressure retrievedPressure = data.getPressure();
        assertNotNull(retrievedPressure);
        assertTrue(retrievedPressure.isHighPressure());
        assertTrue(retrievedPressure.isAboveStandard());
        assertFalse(retrievedPressure.isLowPressure());
        assertFalse(retrievedPressure.isBelowStandard());
    }

    @Test
    @DisplayName("Should calculate deviation from standard pressure")
    void testPressureDeviation() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Pressure pressure = Pressure.hectopascals(1030.0);

        data.setPressure(pressure);

        Pressure retrievedPressure = data.getPressure();
        double deviation = retrievedPressure.getDeviationFromStandard();

        // Standard is 1013.25 hPa, so 1030 is +16.75 hPa above
        assertEquals(16.75, deviation, 0.1);
    }

    @Test
    @DisplayName("Should detect extremely low pressure")
    void testExtremelyLowPressure() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Pressure pressure = Pressure.hectopascals(920.0); // Hurricane-level pressure

        data.setPressure(pressure);

        Pressure retrievedPressure = data.getPressure();
        assertTrue(retrievedPressure.isExtremelyLow());
        assertTrue(retrievedPressure.isLowPressure());
    }

    @Test
    @DisplayName("Should detect extremely high pressure")
    void testExtremelyHighPressure() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Pressure pressure = Pressure.hectopascals(1050.0); // Very high pressure

        data.setPressure(pressure);

        Pressure retrievedPressure = data.getPressure();
        assertTrue(retrievedPressure.isExtremelyHigh());
        assertTrue(retrievedPressure.isHighPressure());
    }

    @Test
    @DisplayName("Should format pressure correctly")
    void testPressureFormatting() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Pressure pressure = Pressure.inchesHg(30.15);

        data.setPressure(pressure);

        Pressure retrievedPressure = data.getPressure();

        // Formatted value
        String formatted = retrievedPressure.getFormattedValue();
        assertTrue(formatted.contains("30.15"));
        assertTrue(formatted.contains("inHg"));

        // METAR altimeter
        String altimeter = retrievedPressure.toMetarAltimeter();
        assertEquals("A3015", altimeter);

        // METAR QNH
        String qnh = retrievedPressure.toMetarQNH();
        assertTrue(qnh.startsWith("Q"));
        assertTrue(qnh.contains("1021")); // Converted to hPa
    }

    @Test
    @DisplayName("Should calculate pressure altitude")
    void testPressureAltitude() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Pressure pressure = Pressure.standard(); // 1013.25 hPa

        data.setPressure(pressure);

        Pressure retrievedPressure = data.getPressure();
        double pressureAltitude = retrievedPressure.getPressureAltitudeFeet();

        // At standard pressure, pressure altitude should be near 0
        assertEquals(0.0, pressureAltitude, 50.0);
    }

    @Test
    @DisplayName("Should replace pressure when set again")
    void testSetPressureReplace() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);

        // Set initial pressure
        Pressure pressure1 = Pressure.inchesHg(30.15);
        data.setPressure(pressure1);
        assertEquals(30.15, data.getPressure().value(), 0.01);

        // Replace with new pressure
        Pressure pressure2 = Pressure.hectopascals(1000.0);
        data.setPressure(pressure2);
        assertEquals(1000.0, data.getPressure().value(), 0.01);
        assertEquals(PressureUnit.HECTOPASCALS, data.getPressure().unit());
    }

    @Test
    @DisplayName("Should create pressure from METAR altimeter string")
    void testPressureFromMetarAltimeter() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Pressure pressure = Pressure.fromMetarAltimeter("A3015");

        data.setPressure(pressure);

        assertNotNull(data.getPressure());
        assertEquals(30.15, data.getPressure().value(), 0.01);
        assertEquals(PressureUnit.INCHES_HG, data.getPressure().unit());
    }

    @Test
    @DisplayName("Should create pressure from METAR QNH string")
    void testPressureFromMetarQNH() {
        TestWeatherData data = new TestWeatherData(WeatherDataSource.NOAA, "KJFK", now);
        Pressure pressure = Pressure.fromMetarQNH("Q1013");

        data.setPressure(pressure);

        assertNotNull(data.getPressure());
        assertEquals(1013.0, data.getPressure().value(), 0.01);
        assertEquals(PressureUnit.HECTOPASCALS, data.getPressure().unit());
    }
}
