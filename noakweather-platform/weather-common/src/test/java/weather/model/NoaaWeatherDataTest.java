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
}
