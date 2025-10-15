/*
 * noakweather(TM) is a Java library for parsing weather data
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
package noakweather.noaa_api.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NoaaAviationWeatherData base class.
 * 
 * Since NoaaAviationWeatherData is abstract, we'll test it through a concrete
 * test implementation. This ensures that the base functionality works correctly
 * for all subclasses.
 * 
 * @author bclasky1539
 */
@DisplayName("NOAA Aviation Weather Data Tests")
class NoaaAviationWeatherDataTest {
    
    private TestNoaaAviationWeatherData testData;
    private LocalDateTime testObservationTime;
    
    // Concrete test implementation of the abstract class
    private static class TestNoaaAviationWeatherData extends NoaaAviationWeatherData {
        private boolean isCurrentValue = true;
        private String reportTypeValue = "TEST";
        
        public TestNoaaAviationWeatherData() {
            super();
        }
        
        public TestNoaaAviationWeatherData(String rawText, String stationId, LocalDateTime observationTime) {
            super(rawText, stationId, observationTime);
        }
        
        @Override
        public boolean isCurrent() {
            return isCurrentValue;
        }
        
        @Override
        public String getReportType() {
            return reportTypeValue;
        }
        
        // Test helpers
        public void setCurrentValue(boolean current) {
            this.isCurrentValue = current;
        }
        
        public void setReportTypeValue(String reportType) {
            this.reportTypeValue = reportType;
        }
    }
    
    @BeforeEach
    void setUp() {
        testObservationTime = LocalDateTime.of(2024, 1, 15, 18, 30);
        testData = new TestNoaaAviationWeatherData();
    }
    
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Default constructor creates empty object")
        void defaultConstructor() {
            TestNoaaAviationWeatherData data = new TestNoaaAviationWeatherData();
            
            assertNull(data.getRawText());
            assertNull(data.getStationId());
            assertNull(data.getObservationTime());
            assertNull(data.getLatitude());
            assertNull(data.getLongitude());
            assertNull(data.getElevationFeet());
            assertNull(data.getQualityControlFlags());
        }
        
        @Test
        @DisplayName("Parameterized constructor sets basic fields")
        void parameterizedConstructor() {
            String rawText = "TEST KJFK 151830Z 28016KT 10SM FEW250 22/12 A3015";
            String stationId = "KJFK";
            
            TestNoaaAviationWeatherData data = new TestNoaaAviationWeatherData(rawText, stationId, testObservationTime);
            
            assertEquals(rawText, data.getRawText());
            assertEquals(stationId, data.getStationId());
            assertEquals(testObservationTime, data.getObservationTime());
        }
        
        @Test
        @DisplayName("Constructor handles null values gracefully")
        void constructorWithNulls() {
            TestNoaaAviationWeatherData data = new TestNoaaAviationWeatherData(null, null, null);
            
            assertNull(data.getRawText());
            assertNull(data.getStationId());
            assertNull(data.getObservationTime());
        }
    }
    
    @Nested
    @DisplayName("Basic Field Tests")
    class BasicFieldTests {
        
        @Test
        @DisplayName("Raw text field works correctly")
        void rawTextField() {
            String rawText = "METAR KJFK 151830Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2";
            
            testData.setRawText(rawText);
            assertEquals(rawText, testData.getRawText());
            
            testData.setRawText(null);
            assertNull(testData.getRawText());
        }
        
        @Test
        @DisplayName("Station ID field works correctly")
        void stationIdField() {
            String stationId = "KJFK";
            
            testData.setStationId(stationId);
            assertEquals(stationId, testData.getStationId());
            
            testData.setStationId(null);
            assertNull(testData.getStationId());
        }
        
        @Test
        @DisplayName("Observation time field works correctly")
        void observationTimeField() {
            testData.setObservationTime(testObservationTime);
            assertEquals(testObservationTime, testData.getObservationTime());
            
            testData.setObservationTime(null);
            assertNull(testData.getObservationTime());
        }
        
        @Test
        @DisplayName("Latitude field works correctly")
        void latitudeField() {
            Double latitude = 40.6413;
            
            testData.setLatitude(latitude);
            assertEquals(latitude, testData.getLatitude());
            
            testData.setLatitude(null);
            assertNull(testData.getLatitude());
        }
        
        @Test
        @DisplayName("Longitude field works correctly")
        void longitudeField() {
            Double longitude = -73.7781;
            
            testData.setLongitude(longitude);
            assertEquals(longitude, testData.getLongitude());
            
            testData.setLongitude(null);
            assertNull(testData.getLongitude());
        }
        
        @Test
        @DisplayName("Elevation field works correctly")
        void elevationField() {
            Integer elevation = 13;
            
            testData.setElevationFeet(elevation);
            assertEquals(elevation, testData.getElevationFeet());
            
            testData.setElevationFeet(null);
            assertNull(testData.getElevationFeet());
        }
        
        @Test
        @DisplayName("Quality control flags field works correctly")
        void qualityControlFlagsField() {
            String flags = "AUTO";
            
            testData.setQualityControlFlags(flags);
            assertEquals(flags, testData.getQualityControlFlags());
            
            testData.setQualityControlFlags(null);
            assertNull(testData.getQualityControlFlags());
        }
    }
    
    @Nested
    @DisplayName("Abstract Method Tests")
    class AbstractMethodTests {
        
        @Test
        @DisplayName("isCurrent method works correctly")
        void isCurrentMethod() {
            assertTrue(testData.isCurrent()); // Default is true
            
            testData.setCurrentValue(false);
            assertFalse(testData.isCurrent());
            
            testData.setCurrentValue(true);
            assertTrue(testData.isCurrent());
        }
        
        @Test
        @DisplayName("getReportType method works correctly")
        void getReportTypeMethod() {
            assertEquals("TEST", testData.getReportType()); // Default value
            
            testData.setReportTypeValue("METAR");
            assertEquals("METAR", testData.getReportType());
            
            testData.setReportTypeValue("TAF");
            assertEquals("TAF", testData.getReportType());
        }
    }
    
    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {
        
        @Test
        @DisplayName("Objects with same data are equal")
        void equalObjects() {
            TestNoaaAviationWeatherData data1 = new TestNoaaAviationWeatherData("RAW", "KJFK", testObservationTime);
            TestNoaaAviationWeatherData data2 = new TestNoaaAviationWeatherData("RAW", "KJFK", testObservationTime);
            
            assertEquals(data1, data2);
            assertEquals(data1.hashCode(), data2.hashCode());
        }
        
        @Test
        @DisplayName("Objects with different station IDs are not equal")
        void differentStationIds() {
            TestNoaaAviationWeatherData data1 = new TestNoaaAviationWeatherData("RAW", "KJFK", testObservationTime);
            TestNoaaAviationWeatherData data2 = new TestNoaaAviationWeatherData("RAW", "KLGA", testObservationTime);
            
            assertNotEquals(data1, data2);
        }
        
        @Test
        @DisplayName("Objects with different observation times are not equal")
        void differentObservationTimes() {
            LocalDateTime time1 = LocalDateTime.of(2024, 1, 15, 18, 30);
            LocalDateTime time2 = LocalDateTime.of(2024, 1, 15, 19, 30);
            
            TestNoaaAviationWeatherData data1 = new TestNoaaAviationWeatherData("RAW", "KJFK", time1);
            TestNoaaAviationWeatherData data2 = new TestNoaaAviationWeatherData("RAW", "KJFK", time2);
            
            assertNotEquals(data1, data2);
        }
        
        @Test
        @DisplayName("Objects with different raw text are not equal")
        void differentRawText() {
            TestNoaaAviationWeatherData data1 = new TestNoaaAviationWeatherData("RAW1", "KJFK", testObservationTime);
            TestNoaaAviationWeatherData data2 = new TestNoaaAviationWeatherData("RAW2", "KJFK", testObservationTime);
            
            assertNotEquals(data1, data2);
        }
        
        @Test
        @DisplayName("Object equals itself")
        void objectEqualsItself() {
            TestNoaaAviationWeatherData data = new TestNoaaAviationWeatherData("RAW", "KJFK", testObservationTime);
            
            assertEquals(data, data);
        }
        
        @Test
        @DisplayName("Object does not equal null")
        void objectNotEqualsNull() {
            TestNoaaAviationWeatherData data = new TestNoaaAviationWeatherData("RAW", "KJFK", testObservationTime);
            
            assertNotEquals(data, null);
        }
        
        @Test
        @DisplayName("Object does not equal different class")
        void objectNotEqualsDifferentClass() {
            TestNoaaAviationWeatherData data = new TestNoaaAviationWeatherData("RAW", "KJFK", testObservationTime);
            String differentClass = "Not weather data";
            
            assertNotEquals(data, differentClass);
        }
        
        @Test
        @DisplayName("Objects with null fields handle equals correctly")
        void equalsWithNullFields() {
            TestNoaaAviationWeatherData data1 = new TestNoaaAviationWeatherData();
            TestNoaaAviationWeatherData data2 = new TestNoaaAviationWeatherData();
            
            assertEquals(data1, data2);
            
            data1.setStationId("KJFK");
            assertNotEquals(data1, data2);
            
            data2.setStationId("KJFK");
            assertEquals(data1, data2);
        }
    }
    
    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {
        
        @Test
        @DisplayName("toString includes class name and key fields")
        void toStringFormat() {
            testData.setStationId("KJFK");
            testData.setObservationTime(testObservationTime);
            testData.setRawText("TEST RAW TEXT");
            
            String result = testData.toString();
            
            assertTrue(result.contains("TestNoaaAviationWeatherData"));
            assertTrue(result.contains("KJFK"));
            assertTrue(result.contains("TEST RAW TEXT"));
            assertTrue(result.contains(testObservationTime.toString()));
        }
        
        @Test
        @DisplayName("toString handles null values gracefully")
        void toStringWithNulls() {
            String result = testData.toString();
            
            assertTrue(result.contains("TestNoaaAviationWeatherData"));
            assertTrue(result.contains("null"));
        }
    }
    
    @Nested
    @DisplayName("Boundary Value Tests")
    class BoundaryValueTests {
        
        @Test
        @DisplayName("Latitude boundary values")
        void latitudeBoundaryValues() {
            // Valid latitude range is -90 to +90
            testData.setLatitude(-90.0);
            assertEquals(-90.0, testData.getLatitude());
            
            testData.setLatitude(90.0);
            assertEquals(90.0, testData.getLatitude());
            
            testData.setLatitude(0.0);
            assertEquals(0.0, testData.getLatitude());
        }
        
        @Test
        @DisplayName("Longitude boundary values")
        void longitudeBoundaryValues() {
            // Valid longitude range is -180 to +180
            testData.setLongitude(-180.0);
            assertEquals(-180.0, testData.getLongitude());
            
            testData.setLongitude(180.0);
            assertEquals(180.0, testData.getLongitude());
            
            testData.setLongitude(0.0);
            assertEquals(0.0, testData.getLongitude());
        }
        
        @Test
        @DisplayName("Elevation extreme values")
        void elevationExtremeValues() {
            // Test extreme elevation values
            testData.setElevationFeet(-1000); // Below sea level
            assertEquals(-1000, testData.getElevationFeet());
            
            testData.setElevationFeet(30000); // Very high elevation
            assertEquals(30000, testData.getElevationFeet());
            
            testData.setElevationFeet(0); // Sea level
            assertEquals(0, testData.getElevationFeet());
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Empty string handling")
        void emptyStringHandling() {
            testData.setRawText("");
            assertEquals("", testData.getRawText());
            
            testData.setStationId("");
            assertEquals("", testData.getStationId());
            
            testData.setQualityControlFlags("");
            assertEquals("", testData.getQualityControlFlags());
        }
        
        @Test
        @DisplayName("Very long strings")
        void veryLongStrings() {
            String longString = "A".repeat(10000);
            
            testData.setRawText(longString);
            assertEquals(longString, testData.getRawText());
            
            testData.setQualityControlFlags(longString);
            assertEquals(longString, testData.getQualityControlFlags());
        }
        
        @Test
        @DisplayName("Special characters in strings")
        void specialCharactersInStrings() {
            String specialChars = "Special: äöü ñ 中文 🌤️ @#$%^&*()";
            
            testData.setRawText(specialChars);
            assertEquals(specialChars, testData.getRawText());
            
            testData.setStationId("K1@#");
            assertEquals("K1@#", testData.getStationId());
        }
    }
}
