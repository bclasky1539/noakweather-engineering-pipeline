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
import java.time.temporal.ChronoUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NoaaWeatherData
 * 
 * @author bclasky1539
 * 
 */
class NoaaWeatherDataTest {
    
    private NoaaWeatherData weatherData;
    private Instant now;
    
    @BeforeEach
    void setUp() {
        now = Instant.now();
        weatherData = new NoaaWeatherData("KJFK", now, "METAR");
    }
    
    @Test
    @DisplayName("Should create NoaaWeatherData with required fields")
    void testConstructor() {
        assertNotNull(weatherData);
        assertEquals("KJFK", weatherData.getStationId());
        assertEquals(now, weatherData.getObservationTime());
        assertEquals("METAR", weatherData.getReportType());
        assertEquals(WeatherDataSource.NOAA, weatherData.getSource());
    }
    
    @Test
    @DisplayName("Should generate unique ID on creation")
    void testUniqueId() {
        NoaaWeatherData data1 = new NoaaWeatherData("KJFK", now, "METAR");
        NoaaWeatherData data2 = new NoaaWeatherData("KJFK", now, "METAR");
        
        assertNotNull(data1.getId());
        assertNotNull(data2.getId());
        assertNotEquals(data1.getId(), data2.getId());
    }
    
    @Test
    @DisplayName("Should set ingestion time automatically")
    void testIngestionTime() {
        Instant before = Instant.now();
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        Instant after = Instant.now();
        
        assertNotNull(data.getIngestionTime());
        assertFalse(data.getIngestionTime().isBefore(before));
        assertFalse(data.getIngestionTime().isAfter(after));
    }
    
    @Test
    @DisplayName("Should default to SPEED_LAYER processing layer")
    void testDefaultProcessingLayer() {
        assertEquals(ProcessingLayer.SPEED_LAYER, weatherData.getProcessingLayer());
    }
    
    @Test
    @DisplayName("Should consider data current if less than 2 hours old")
    void testIsCurrentWhenRecent() {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        NoaaWeatherData data = new NoaaWeatherData("KJFK", oneHourAgo, "METAR");
        
        assertTrue(data.isCurrent());
    }
    
    @Test
    @DisplayName("Should consider data not current if more than 2 hours old")
    void testIsCurrentWhenOld() {
        Instant threeHoursAgo = Instant.now().minus(3, ChronoUnit.HOURS);
        NoaaWeatherData data = new NoaaWeatherData("KJFK", threeHoursAgo, "METAR");
        
        assertFalse(data.isCurrent());
    }
    
    @Test
    @DisplayName("Should handle null observation time in isCurrent")
    void testIsCurrentWithNullObservationTime() {
        NoaaWeatherData data = new NoaaWeatherData();
        data.setObservationTime(null);
        
        assertFalse(data.isCurrent());
    }
    
    @Test
    @DisplayName("Should return correct data type")
    void testGetDataType() {
        assertEquals("METAR", weatherData.getDataType());
        
        NoaaWeatherData tafData = new NoaaWeatherData("KJFK", now, "TAF");
        assertEquals("TAF", tafData.getDataType());
    }
    
    @Test
    @DisplayName("Should handle null report type in getDataType")
    void testGetDataTypeWithNullReportType() {
        NoaaWeatherData data = new NoaaWeatherData();
        assertEquals("NOAA", data.getDataType());
    }
    
    @Test
    @DisplayName("Should generate meaningful summary")
    void testGetSummary() {
        String summary = weatherData.getSummary();
        
        assertTrue(summary.contains("METAR"));
        assertTrue(summary.contains("KJFK"));
        assertTrue(summary.contains(now.toString()));
    }
    
    @Test
    @DisplayName("Should handle null report type in getSummary")
    void testGetSummaryWithNullReportType() {
        NoaaWeatherData data = new NoaaWeatherData();
        data.setStationId("KJFK");
        data.setObservationTime(now);
        
        String summary = data.getSummary();
        assertTrue(summary.contains("NOAA Report"));
    }
    
    @Test
    @DisplayName("Should set and get report type")
    void testSetReportType() {
        weatherData.setReportType("TAF");
        assertEquals("TAF", weatherData.getReportType());
    }
    
    @Test
    @DisplayName("Should set and get location")
    void testSetLocation() {
        GeoLocation location = new GeoLocation(40.6413, -73.7781, 4);
        weatherData.setLocation(location);
        
        assertEquals(location, weatherData.getLocation());
    }
    
    @Test
    @DisplayName("Should set and get raw data")
    void testSetRawData() {
        String rawText = "METAR KJFK 191651Z 28016KT 10SM FEW250 22/12 A3015";
        weatherData.setRawData(rawText);
        
        assertEquals(rawText, weatherData.getRawData());
    }
    
    @Test
    @DisplayName("Should set and get quality flags")
    void testSetQualityFlags() {
        weatherData.setQualityFlags("AUTO");
        assertEquals("AUTO", weatherData.getQualityFlags());
    }
    
    @Test
    @DisplayName("Should set and get processing layer")
    void testSetProcessingLayer() {
        weatherData.setProcessingLayer(ProcessingLayer.BATCH_LAYER);
        assertEquals(ProcessingLayer.BATCH_LAYER, weatherData.getProcessingLayer());
    }
    
    @Test
    @DisplayName("Should handle metadata")
    void testMetadata() {
        weatherData.addMetadata("source_api", "AWC");
        weatherData.addMetadata("version", "1.0");
        
        assertEquals("AWC", weatherData.getMetadata().get("source_api"));
        assertEquals("1.0", weatherData.getMetadata().get("version"));
        assertEquals(2, weatherData.getMetadata().size());
    }
    
    @Test
    @DisplayName("Should implement equals based on ID")
    void testEquals() {
        NoaaWeatherData data1 = new NoaaWeatherData("KJFK", now, "METAR");
        NoaaWeatherData data2 = new NoaaWeatherData("KJFK", now, "METAR");
        
        // Different objects with different IDs
        assertNotEquals(data1, data2);
        
        // Same object
        assertEquals(data1, data1);
    }
    
    @Test
    @DisplayName("Should implement hashCode based on ID")
    void testHashCode() {
        int hash1 = weatherData.hashCode();
        int hash2 = weatherData.hashCode();
        
        // Same object should have same hash
        assertEquals(hash1, hash2);
    }
    
    @Test
    @DisplayName("Should have meaningful toString")
    void testToString() {
        String str = weatherData.toString();
        
        assertTrue(str.contains("NoaaWeatherData"));
        assertTrue(str.contains("KJFK"));
        assertTrue(str.contains("NOAA"));
    }
    
    @Test
    @DisplayName("Should consider reportType in equality")
    void testEqualsWithDifferentReportType() {
        // Create two objects with same parent fields but different reportType
        // Since they have different auto-generated IDs, they won't be equal anyway
        // But this documents the expected behavior
        NoaaWeatherData metar = new NoaaWeatherData("KJFK", now, "METAR");
        NoaaWeatherData taf = new NoaaWeatherData("KJFK", now, "TAF");
        
        assertNotEquals(metar, taf);
        assertNotEquals(metar.hashCode(), taf.hashCode());
    }

    @Test
    @DisplayName("Should implement equals correctly with same reportType")
    void testEqualsWithSameReportType() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        // Same object
        assertEquals(data, data);
        
        // Not equal to null
        // FIXED: Swapped arguments to put expected value first
        assertNotEquals(null, data);
        
        // Not equal to different type
        // FIXED: Swapped arguments to put expected value first
        assertNotEquals("not a NoaaWeatherData", data);
    }
    
    @Test
    @DisplayName("Should maintain consistent hashCode")
    void testHashCodeConsistency() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        int hash1 = data.hashCode();
        int hash2 = data.hashCode();
        
        assertEquals(hash1, hash2, "hashCode should be consistent");
    }
    
    @Test
    @DisplayName("Should handle equals with null reportType")
    void testEqualsWithNullReportType() {
        NoaaWeatherData data1 = new NoaaWeatherData("KJFK", now, null);
        NoaaWeatherData data2 = new NoaaWeatherData("KJFK", now, null);
        
        // Both have null reportType - they differ by ID though
        assertNotEquals(data1, data2);
    }
    
    @Test
    @DisplayName("Should add metadata when metadata map is null")
    void testAddMetadataWhenMapIsNull() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        data.setMetadata(null); // Force metadata to be null
        
        data.addMetadata("test_key", "test_value");
        
        assertNotNull(data.getMetadata());
        assertEquals("test_value", data.getMetadata().get("test_key"));
    }
    
    @Test
    @DisplayName("Should add metadata when metadata map already exists")
    void testAddMetadataWhenMapExists() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        data.addMetadata("key1", "value1");
        data.addMetadata("key2", "value2");
        
        assertEquals(2, data.getMetadata().size());
        assertEquals("value1", data.getMetadata().get("key1"));
        assertEquals("value2", data.getMetadata().get("key2"));
    }
    
    @Test
    @DisplayName("Should overwrite existing metadata key")
    void testAddMetadataOverwrite() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        data.addMetadata("key", "value1");
        data.addMetadata("key", "value2");
        
        assertEquals(1, data.getMetadata().size());
        assertEquals("value2", data.getMetadata().get("key"));
    }

    @Test
    @DisplayName("Should set entire metadata map")
    void testSetMetadata() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", 123);
        metadata.put("key3", true);
        
        data.setMetadata(metadata);
        
        assertEquals(3, data.getMetadata().size());
        assertEquals("value1", data.getMetadata().get("key1"));
        assertEquals(123, data.getMetadata().get("key2"));
        assertEquals(true, data.getMetadata().get("key3"));
    }
    
    @Test
    @DisplayName("Should set source using setSource method")
    void testSetSource() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        // Constructor sets it to NOAA by default
        assertEquals(WeatherDataSource.NOAA, data.getSource());
        
        // Change to different source (edge case, but tests the setter)
        data.setSource(WeatherDataSource.INTERNAL);
        assertEquals(WeatherDataSource.INTERNAL, data.getSource());
    }
    
    @Test
    @DisplayName("Should set source to null")
    void testSetSourceNull() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        data.setSource(null);
        
        assertNull(data.getSource());
    }

    @Test
    @DisplayName("Should handle equals with same ID")
    void testEqualsWithSameId() {
        NoaaWeatherData data1 = new NoaaWeatherData("KJFK", now, "METAR");
        NoaaWeatherData data2 = new NoaaWeatherData("KLGA", now, "TAF");
        
        // They have different IDs (auto-generated), so not equal
        assertNotEquals(data1, data2);
        
        // Same object reference
        assertEquals(data1, data1);
    }
    
    @Test
    @DisplayName("Should handle equals with null")
    void testEqualsWithNull() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        assertNotEquals(null, data);
    }
    
    @Test
    @DisplayName("Should handle equals with different class")
    void testEqualsWithDifferentClass() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        String notWeatherData = "not a WeatherData";
        
        assertNotEquals(data, notWeatherData);
    }
    
    @Test
    @DisplayName("Should set and get station ID")
    void testSetStationId() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        data.setStationId("KLGA");
        assertEquals("KLGA", data.getStationId());
    }

    @Test
    @DisplayName("Should set and get observation time")
    void testSetObservationTime() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        Instant newTime = now.plus(1, ChronoUnit.HOURS);
        data.setObservationTime(newTime);
        assertEquals(newTime, data.getObservationTime());
    }
    
    @Test
    @DisplayName("Should get immutable ID")
    void testIdIsImmutable() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        String id = data.getId();
        
        assertNotNull(id);
        // Verify ID doesn't change
        assertEquals(id, data.getId());
    }
    
    @Test
    @DisplayName("Should get immutable ingestion time")
    void testIngestionTimeIsImmutable() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        Instant ingestionTime = data.getIngestionTime();
        
        assertNotNull(ingestionTime);
        // Verify ingestion time doesn't change
        assertEquals(ingestionTime, data.getIngestionTime());
    }
    
    @Test
    @DisplayName("Should handle equals when comparing different WeatherData subclasses")
    void testEqualsWithDifferentSubclass() {
        NoaaWeatherData noaaData = new NoaaWeatherData("KJFK", now, "METAR");
        
        // Since WeatherData is sealed and only permits NoaaWeatherData currently,
        // we can only test with NoaaWeatherData instances
        // But we can test that two different NoaaWeatherData objects are not equal
        NoaaWeatherData otherData = new NoaaWeatherData("KJFK", now, "METAR");
        
        assertNotEquals(noaaData, otherData, 
                       "Two NoaaWeatherData instances should not be equal (different IDs)");
    }
    
    @Test
    @DisplayName("Should return false when comparing with object of non-WeatherData type")
    void testEqualsWithNonWeatherDataType() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        Object notWeatherData = new Object();
        
        assertNotEquals(data, notWeatherData);
        assertNotEquals(notWeatherData, data);
    }
    
    @Test
    @DisplayName("Should return true for same object reference")
    void testEqualsSameReference() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        assertEquals(data, data);
        assertEquals(data, data);
    }

    @Test
    @DisplayName("Should verify equals symmetry")
    void testEqualsSymmetry() {
        NoaaWeatherData data1 = new NoaaWeatherData("KJFK", now, "METAR");
        NoaaWeatherData data2 = new NoaaWeatherData("KLGA", now, "TAF");
        
        // If x.equals(y) == false, then y.equals(x) should also == false
        assertNotEquals(data1, data2);
        assertNotEquals(data2, data1);
    }
    
    @Test
    @DisplayName("Should verify equals transitivity")
    void testEqualsTransitivity() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        // For same reference: if x.equals(x) and x.equals(x), then x.equals(x)
        assertEquals(data, data);
        assertEquals(data, data);
        assertEquals(data, data);
    }
    
    @Test
    @DisplayName("Should verify equals consistency")
    void testEqualsConsistency() {
        NoaaWeatherData data1 = new NoaaWeatherData("KJFK", now, "METAR");
        NoaaWeatherData data2 = new NoaaWeatherData("KLGA", now, "TAF");
        
        // Multiple calls should return same result
        boolean firstCall = data1.equals(data2);
        boolean secondCall = data1.equals(data2);
        boolean thirdCall = data1.equals(data2);
        
        assertEquals(firstCall, secondCall);
        assertEquals(secondCall, thirdCall);
    }

    @Test
    @DisplayName("Should handle equals with WeatherData cast")
    void testEqualsWithExplicitCast() {
        NoaaWeatherData data1 = new NoaaWeatherData("KJFK", now, "METAR");
        WeatherData data2 = new NoaaWeatherData("KLGA", now, "TAF");
        
        // Test polymorphic comparison
        assertNotEquals(data1, data2);
        assertNotEquals(data2, data1);
    }
    
    @Test
    @DisplayName("Should handle equals when IDs are compared")
    void testEqualsIdComparison() throws Exception {
        NoaaWeatherData data1 = new NoaaWeatherData("KJFK", now, "METAR");
        NoaaWeatherData data2 = new NoaaWeatherData("KJFK", now, "METAR");
        
        // Get the IDs
        String id1 = data1.getId();
        String id2 = data2.getId();
        
        // Verify IDs are different
        assertNotEquals(id1, id2);
        
        // Therefore objects are not equal
        assertNotEquals(data1, data2);
    }
}
