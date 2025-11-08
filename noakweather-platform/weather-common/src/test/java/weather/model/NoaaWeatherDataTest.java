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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for NoaaWeatherData base class.
 * 
 * @author bclasky1539
 */
class NoaaWeatherDataTest {
    
    private NoaaWeatherData weatherData;
    private Instant now;
    
    @BeforeEach
    void setUp() {
        now = Instant.now();
        weatherData = new NoaaWeatherData("KJFK", now, "METAR");
    }
    
    // ========== CONSTRUCTOR TESTS ==========
    
    @Test
    @DisplayName("Should create NoaaWeatherData with default constructor")
    void testDefaultConstructor() {
        NoaaWeatherData data = new NoaaWeatherData();
        
        assertNotNull(data);
        assertNotNull(data.getId());
        assertNotNull(data.getIngestionTime());
    }
    
    @Test
    @DisplayName("Should create NoaaWeatherData with parameterized constructor")
    void testParameterizedConstructor() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        assertNotNull(data);
        assertEquals("KJFK", data.getStationId());
        assertEquals(now, data.getObservationTime());
        assertEquals("METAR", data.getReportType());
        assertEquals(WeatherDataSource.NOAA, data.getSource());
    }
    
    // ========== EQUALS TESTS ==========
    
    @Test
    @DisplayName("Should test equals with same object instance")
    void testEquals_SameInstance() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        assertTrue(data.equals(data));
    }
    
    @Test
    @DisplayName("Should test equals with matching reportType")
    void testEquals_MatchingReportType() {
        NoaaWeatherData data1 = new NoaaWeatherData("KJFK", now, "METAR");
        NoaaWeatherData data2 = new NoaaWeatherData("KJFK", now, "METAR");
        
        // Different IDs, so not equal
        assertNotEquals(data1, data2);
    }
    
    @Test
    @DisplayName("Should test equals with null reportType in both objects")
    void testEquals_BothNullReportTypes() {
        NoaaWeatherData data1 = new NoaaWeatherData("KJFK", now, null);
        NoaaWeatherData data2 = new NoaaWeatherData("KJFK", now, null);
        
        assertNotEquals(data1, data2);
    }
    
    @Test
    @DisplayName("Should test equals with one null reportType")
    void testEquals_OneNullReportType() {
        NoaaWeatherData data1 = new NoaaWeatherData("KJFK", now, "METAR");
        NoaaWeatherData data2 = new NoaaWeatherData("KJFK", now, null);
        
        assertNotEquals(data1, data2);
    }
    
    @Test
    @DisplayName("Should return false when comparing NoaaWeatherData with different class type")
    void testEquals_DifferentClassType() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        assertFalse(data.equals("Not a NoaaWeatherData"));
        assertFalse(data.equals(null));
        assertFalse(data.equals(new Object()));
    }
    
    @Test
    @DisplayName("Should return false when super.equals fails due to different base fields")
    void testEquals_SuperEqualsFails() {
        NoaaWeatherData data1 = new NoaaWeatherData("KJFK", now, "METAR");
        NoaaWeatherData data2 = new NoaaWeatherData("KLGA", now, "METAR");
        
        assertFalse(data1.equals(data2));
    }
    
    @Test
    @DisplayName("Should return false when observation times differ")
    void testEquals_DifferentObservationTimes() {
        Instant time1 = Instant.now();
        Instant time2 = time1.plus(1, ChronoUnit.HOURS);
        
        NoaaWeatherData data1 = new NoaaWeatherData("KJFK", time1, "METAR");
        NoaaWeatherData data2 = new NoaaWeatherData("KJFK", time2, "METAR");
        
        assertFalse(data1.equals(data2));
    }
    
    @Test
    @DisplayName("Should handle equals when comparing with WeatherData parent type")
    void testEquals_WithParentType() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        WeatherData parentReference = data;
        assertTrue(data.equals(parentReference));
        assertTrue(parentReference.equals(data));
    }
    
    // ========== HASHCODE TESTS ==========
    
    @Test
    @DisplayName("Should call hashCode on NoaaWeatherData directly")
    void testHashCode_DirectCall() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        int hash1 = data.hashCode();
        int hash2 = data.hashCode();
        
        assertEquals(hash1, hash2, "hashCode should be consistent");
    }
    
    @Test
    @DisplayName("Should call hashCode with different reportTypes")
    void testHashCode_DifferentReportTypes() {
        NoaaWeatherData metar = new NoaaWeatherData("KJFK", now, "METAR");
        NoaaWeatherData taf = new NoaaWeatherData("KJFK", now, "TAF");
        
        int metarHash = metar.hashCode();
        int tafHash = taf.hashCode();
        
        assertNotEquals(metarHash, tafHash);
    }

    @Test
    @DisplayName("Should call hashCode with null reportType")
    void testHashCode_NullReportType() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, null);
        
        int hash = data.hashCode();
        
        assertNotNull(hash);
    }
    
    // ========== GETSUMMARY TESTS ==========
    
    @Test
    @DisplayName("Should call getSummary on NoaaWeatherData directly")
    void testGetSummary_DirectCall() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        String summary = data.getSummary();
        
        assertNotNull(summary);
        assertTrue(summary.contains("METAR"));
        assertTrue(summary.contains("KJFK"));
    }
    
    @Test
    @DisplayName("Should call getSummary with null reportType on NoaaWeatherData")
    void testGetSummary_NullReportType_DirectCall() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, null);
        
        String summary = data.getSummary();
        
        assertNotNull(summary);
        assertTrue(summary.contains("NOAA Report"));
        assertTrue(summary.contains("KJFK"));
    }

    // ========== ISCURRENT TESTS ==========

    @Test
    @DisplayName("Should call isCurrent on NoaaWeatherData directly with recent time")
    void testIsCurrent_Recent_DirectCall() {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        NoaaWeatherData data = new NoaaWeatherData("KJFK", oneHourAgo, "METAR");
        
        boolean current = data.isCurrent();
        
        assertTrue(current);
    }
    
    @Test
    @DisplayName("Should call isCurrent on NoaaWeatherData directly with old time")
    void testIsCurrent_Old_DirectCall() {
        Instant threeHoursAgo = Instant.now().minus(3, ChronoUnit.HOURS);
        NoaaWeatherData data = new NoaaWeatherData("KJFK", threeHoursAgo, "METAR");
        
        boolean current = data.isCurrent();
        
        assertFalse(current);
    }
    
    @Test
    @DisplayName("Should call isCurrent on NoaaWeatherData directly with null time")
    void testIsCurrent_Null_DirectCall() {
        NoaaWeatherData data = new NoaaWeatherData();
        data.setObservationTime(null);
        
        boolean current = data.isCurrent();
        
        assertFalse(current);
    }
    
    @Test
    @DisplayName("Should test isCurrent at exact 2 hour boundary")
    void testIsCurrent_ExactlyTwoHours() {
        Instant exactlyTwoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);
        NoaaWeatherData data = new NoaaWeatherData("KJFK", exactlyTwoHoursAgo, "METAR");
        
        boolean current = data.isCurrent();
        
        assertFalse(current);
    }
    
    @Test
    @DisplayName("Should test isCurrent just under 2 hours")
    void testIsCurrent_JustUnderTwoHours() {
        Instant justUnderTwoHours = Instant.now().minus(119, ChronoUnit.MINUTES);
        NoaaWeatherData data = new NoaaWeatherData("KJFK", justUnderTwoHours, "METAR");
        
        boolean current = data.isCurrent();
        
        assertTrue(current);
    }
    
    // ========== GETDATATYPE TESTS ==========
    
    @Test
    @DisplayName("Should test constructor branch with null reportType")
    void testConstructor_NullReportType() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, null);
        
        assertNotNull(data);
        assertEquals("KJFK", data.getStationId());
        assertEquals(now, data.getObservationTime());
        assertNull(data.getReportType());
    }
    
    @Test
    @DisplayName("Should test constructor branch with empty reportType")
    void testConstructor_EmptyReportType() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "");
        
        assertNotNull(data);
        assertEquals("", data.getReportType());
    }
    
    @Test
    @DisplayName("Should test getDataType with various reportTypes")
    void testGetDataType_VariousTypes() {
        NoaaWeatherData metar = new NoaaWeatherData("KJFK", now, "METAR");
        NoaaWeatherData taf = new NoaaWeatherData("KJFK", now, "TAF");
        NoaaWeatherData pirep = new NoaaWeatherData("KJFK", now, "PIREP");
        
        assertEquals("METAR", metar.getDataType());
        assertEquals("TAF", taf.getDataType());
        assertEquals("PIREP", pirep.getDataType());
    }
    
    @Test
    @DisplayName("Should return NOAA when reportType is null - direct call")
    void testGetDataType_NullReportType_DirectCall() {
        NoaaWeatherData data = new NoaaWeatherData();
        data.setReportType(null);
        
        String dataType = data.getDataType();
        
        assertEquals("NOAA", dataType);
    }

    @Test
    @DisplayName("Should return reportType when not null - direct call")
    void testGetDataType_NotNullReportType_DirectCall() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        String dataType = data.getDataType();
        
        assertEquals("METAR", dataType);
    }
    
    @Test
    @DisplayName("Should return NOAA for default constructed object")
    void testGetDataType_DefaultConstructor() {
        NoaaWeatherData data = new NoaaWeatherData();
        
        String dataType = data.getDataType();
        
        assertEquals("NOAA", dataType);
    }
    
    @Test
    @DisplayName("Should handle empty string reportType")
    void testGetDataType_EmptyStringReportType() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "");
        
        String dataType = data.getDataType();
        
        assertEquals("", dataType);
    }
    
    @Test
    @DisplayName("Should return correct type after setting reportType to null")
    void testGetDataType_SetToNull() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        
        assertEquals("METAR", data.getDataType());
        
        data.setReportType(null);
        
        assertEquals("NOAA", data.getDataType());
    }
}
