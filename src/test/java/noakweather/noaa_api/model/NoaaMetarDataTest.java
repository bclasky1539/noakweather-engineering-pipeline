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
 * Unit tests for NoaaMetarData class.
 * 
 * Tests METAR-specific functionality including temperature conversions,
 * wind data, visibility, precipitation, and the isCurrent() business logic.
 * 
 * @author bclasky1539
 */
@DisplayName("NOAA METAR Data Tests")
class NoaaMetarDataTest {
    
    private NoaaMetarData metarData;
    private LocalDateTime testObservationTime;
    
    @BeforeEach
    void setUp() {
        metarData = new NoaaMetarData();
        testObservationTime = LocalDateTime.now().minusHours(1); // 1 hour ago
    }
    
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Default constructor creates empty METAR")
        void defaultConstructor() {
            NoaaMetarData data = new NoaaMetarData();
            
            assertNull(data.getRawText());
            assertNull(data.getStationId());
            assertNull(data.getObservationTime());
            assertNull(data.getTemperatureCelsius());
            assertNull(data.getWindSpeedKnots());
            assertNull(data.getMetarType());
            assertNull(data.getIsAutoReport());
        }
        
        @Test
        @DisplayName("Parameterized constructor parses METAR type from raw text")
        void parameterizedConstructorWithMetar() {
            String rawText = "METAR KJFK 151830Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2";
            
            NoaaMetarData data = new NoaaMetarData(rawText, "KJFK", testObservationTime);
            
            assertEquals(rawText, data.getRawText());
            assertEquals("KJFK", data.getStationId());
            assertEquals(testObservationTime, data.getObservationTime());
            assertEquals("METAR", data.getMetarType());
            assertFalse(data.getIsAutoReport()); // No AUTO in raw text
        }
        
        @Test
        @DisplayName("Parameterized constructor recognizes SPECI reports")
        void parameterizedConstructorWithSpeci() {
            String rawText = "SPECI KJFK 151845Z AUTO 28020G25KT 10SM CLR 23/11 A3016";
            
            NoaaMetarData data = new NoaaMetarData(rawText, "KJFK", testObservationTime);
            
            assertEquals("SPECI", data.getMetarType());
            assertTrue(data.getIsAutoReport()); // Contains AUTO
        }
        
        @Test
        @DisplayName("Constructor handles null raw text gracefully")
        void constructorWithNullRawText() {
            NoaaMetarData data = new NoaaMetarData(null, "KJFK", testObservationTime);
            
            assertNull(data.getMetarType());
            assertNull(data.getIsAutoReport());
        }
    }
    
    @Nested
    @DisplayName("Temperature Tests")
    class TemperatureTests {
        
        @Test
        @DisplayName("Temperature Celsius field works correctly")
        void temperatureCelsiusField() {
            Double tempC = 22.0;
            
            metarData.setTemperatureCelsius(tempC);
            assertEquals(tempC, metarData.getTemperatureCelsius());
            
            metarData.setTemperatureCelsius(null);
            assertNull(metarData.getTemperatureCelsius());
        }
        
        @Test
        @DisplayName("Temperature Fahrenheit conversion works correctly")
        void temperatureFahrenheitConversion() {
            // Test freezing point
            metarData.setTemperatureCelsius(0.0);
            assertEquals(32.0, metarData.getTemperatureFahrenheit(), 0.01);
            
            // Test room temperature
            metarData.setTemperatureCelsius(22.0);
            assertEquals(71.6, metarData.getTemperatureFahrenheit(), 0.01);
            
            // Test negative temperature
            metarData.setTemperatureCelsius(-10.0);
            assertEquals(14.0, metarData.getTemperatureFahrenheit(), 0.01);
        }
        
        @Test
        @DisplayName("Temperature Fahrenheit returns null when Celsius is null")
        void temperatureFahrenheitWithNullCelsius() {
            metarData.setTemperatureCelsius(null);
            assertNull(metarData.getTemperatureFahrenheit());
        }
        
        @Test
        @DisplayName("Dewpoint Celsius field works correctly")
        void dewpointCelsiusField() {
            Double dewpointC = 12.0;
            
            metarData.setDewpointCelsius(dewpointC);
            assertEquals(dewpointC, metarData.getDewpointCelsius());
        }
        
        @Test
        @DisplayName("Dewpoint Fahrenheit conversion works correctly")
        void dewpointFahrenheitConversion() {
            metarData.setDewpointCelsius(12.0);
            assertEquals(53.6, metarData.getDewpointFahrenheit(), 0.01);
            
            metarData.setDewpointCelsius(null);
            assertNull(metarData.getDewpointFahrenheit());
        }
    }
    
    @Nested
    @DisplayName("Wind Tests")
    class WindTests {
        
        @Test
        @DisplayName("Wind direction field works correctly")
        void windDirectionField() {
            Integer direction = 280;
            
            metarData.setWindDirectionDegrees(direction);
            assertEquals(direction, metarData.getWindDirectionDegrees());
            
            metarData.setWindDirectionDegrees(null);
            assertNull(metarData.getWindDirectionDegrees());
        }
        
        @Test
        @DisplayName("Wind speed field works correctly")
        void windSpeedField() {
            Integer speed = 16;
            
            metarData.setWindSpeedKnots(speed);
            assertEquals(speed, metarData.getWindSpeedKnots());
        }
        
        @Test
        @DisplayName("Wind gust field works correctly")
        void windGustField() {
            Integer gust = 25;
            
            metarData.setWindGustKnots(gust);
            assertEquals(gust, metarData.getWindGustKnots());
        }
        
        @Test
        @DisplayName("Wind variable direction field works correctly")
        void windVariableDirectionField() {
            String variableDir = "240V300";
            
            metarData.setWindVariableDirection(variableDir);
            assertEquals(variableDir, metarData.getWindVariableDirection());
        }
        
        @Test
        @DisplayName("Wind direction boundary values")
        void windDirectionBoundaryValues() {
            // Valid wind direction is 0-360 degrees
            metarData.setWindDirectionDegrees(0);
            assertEquals(0, metarData.getWindDirectionDegrees());
            
            metarData.setWindDirectionDegrees(360);
            assertEquals(360, metarData.getWindDirectionDegrees());
            
            metarData.setWindDirectionDegrees(180);
            assertEquals(180, metarData.getWindDirectionDegrees());
        }
    }
    
    @Nested
    @DisplayName("Visibility and Conditions Tests")
    class VisibilityAndConditionsTests {
        
        @Test
        @DisplayName("Visibility field works correctly")
        void visibilityField() {
            Double visibility = 10.0;
            
            metarData.setVisibilityStatuteMiles(visibility);
            assertEquals(visibility, metarData.getVisibilityStatuteMiles());
        }
        
        @Test
        @DisplayName("Weather string field works correctly")
        void weatherStringField() {
            String weather = "RA BR"; // Rain and mist
            
            metarData.setWeatherString(weather);
            assertEquals(weather, metarData.getWeatherString());
        }
        
        @Test
        @DisplayName("Sky condition field works correctly")
        void skyConditionField() {
            String skyCondition = "FEW250";
            
            metarData.setSkyCondition(skyCondition);
            assertEquals(skyCondition, metarData.getSkyCondition());
        }
        
        @Test
        @DisplayName("Flight category field works correctly")
        void flightCategoryField() {
            String category = "VFR";
            
            metarData.setFlightCategory(category);
            assertEquals(category, metarData.getFlightCategory());
            
            // Test all common flight categories
            String[] categories = {"VFR", "MVFR", "IFR", "LIFR"};
            for (String cat : categories) {
                metarData.setFlightCategory(cat);
                assertEquals(cat, metarData.getFlightCategory());
            }
        }
        
        @Test
        @DisplayName("Altimeter field works correctly")
        void altimeterField() {
            Double altimeter = 30.15;
            
            metarData.setAltimeterInHg(altimeter);
            assertEquals(altimeter, metarData.getAltimeterInHg());
        }
    }
    
    @Nested
    @DisplayName("Precipitation Tests")
    class PrecipitationTests {
        
        @Test
        @DisplayName("Precipitation last hour field works correctly")
        void precipitationLastHourField() {
            Double precip = 0.05;
            
            metarData.setPrecipitationLastHourInches(precip);
            assertEquals(precip, metarData.getPrecipitationLastHourInches());
        }
        
        @Test
        @DisplayName("Precipitation last 3 hours field works correctly")
        void precipitationLast3HoursField() {
            Double precip = 0.12;
            
            metarData.setPrecipitationLast3HoursInches(precip);
            assertEquals(precip, metarData.getPrecipitationLast3HoursInches());
        }
        
        @Test
        @DisplayName("Precipitation last 6 hours field works correctly")
        void precipitationLast6HoursField() {
            Double precip = 0.25;
            
            metarData.setPrecipitationLast6HoursInches(precip);
            assertEquals(precip, metarData.getPrecipitationLast6HoursInches());
        }
        
        @Test
        @DisplayName("Zero precipitation values")
        void zeroPrecipitationValues() {
            metarData.setPrecipitationLastHourInches(0.0);
            assertEquals(0.0, metarData.getPrecipitationLastHourInches());
        }
    }
    
    @Nested
    @DisplayName("METAR Type Tests")
    class MetarTypeTests {
        
        @Test
        @DisplayName("METAR type field works correctly")
        void metarTypeField() {
            metarData.setMetarType("METAR");
            assertEquals("METAR", metarData.getMetarType());
            
            metarData.setMetarType("SPECI");
            assertEquals("SPECI", metarData.getMetarType());
        }
        
        @Test
        @DisplayName("Auto report flag works correctly")
        void autoReportFlag() {
            metarData.setIsAutoReport(true);
            assertTrue(metarData.getIsAutoReport());
            
            metarData.setIsAutoReport(false);
            assertFalse(metarData.getIsAutoReport());
            
            metarData.setIsAutoReport(null);
            assertNull(metarData.getIsAutoReport());
        }
    }
    
    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {
        
        @Test
        @DisplayName("isCurrent returns true for recent observations")
        void isCurrentWithRecentObservation() {
            // Set observation time to 1 hour ago
            metarData.setObservationTime(LocalDateTime.now().minusHours(1));
            assertTrue(metarData.isCurrent());
            
            // Set observation time to 2 hours ago
            metarData.setObservationTime(LocalDateTime.now().minusHours(2));
            assertTrue(metarData.isCurrent());
            
            // Set observation time to exactly 3 hours ago
            metarData.setObservationTime(LocalDateTime.now().minusHours(3));
            assertTrue(metarData.isCurrent());
        }
        
        @Test
        @DisplayName("isCurrent returns false for old observations")
        void isCurrentWithOldObservation() {
            // Set observation time to 4 hours ago
            metarData.setObservationTime(LocalDateTime.now().minusHours(4));
            assertFalse(metarData.isCurrent());
            
            // Set observation time to 24 hours ago
            metarData.setObservationTime(LocalDateTime.now().minusHours(24));
            assertFalse(metarData.isCurrent());
        }
        
        @Test
        @DisplayName("isCurrent returns false for null observation time")
        void isCurrentWithNullObservationTime() {
            metarData.setObservationTime(null);
            assertFalse(metarData.isCurrent());
        }
        
        @Test
        @DisplayName("getReportType returns correct type")
        void getReportTypeMethod() {
            metarData.setMetarType("METAR");
            assertEquals("METAR", metarData.getReportType());
            
            metarData.setMetarType("SPECI");
            assertEquals("SPECI", metarData.getReportType());
            
            metarData.setMetarType(null);
            assertEquals("METAR", metarData.getReportType()); // Default fallback
        }
    }
    
    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {
        
        @Test
        @DisplayName("Objects with same METAR data are equal")
        void equalMetarObjects() {
            NoaaMetarData metar1 = new NoaaMetarData("METAR KJFK 151830Z", "KJFK", testObservationTime);
            metar1.setTemperatureCelsius(22.0);
            metar1.setWindDirectionDegrees(280);
            metar1.setWindSpeedKnots(16);
            
            NoaaMetarData metar2 = new NoaaMetarData("METAR KJFK 151830Z", "KJFK", testObservationTime);
            metar2.setTemperatureCelsius(22.0);
            metar2.setWindDirectionDegrees(280);
            metar2.setWindSpeedKnots(16);
            
            assertEquals(metar1, metar2);
            assertEquals(metar1.hashCode(), metar2.hashCode());
        }
        
        @Test
        @DisplayName("Objects with different temperatures are not equal")
        void differentTemperatures() {
            NoaaMetarData metar1 = new NoaaMetarData("METAR KJFK 151830Z", "KJFK", testObservationTime);
            metar1.setTemperatureCelsius(22.0);
            
            NoaaMetarData metar2 = new NoaaMetarData("METAR KJFK 151830Z", "KJFK", testObservationTime);
            metar2.setTemperatureCelsius(25.0);
            
            assertNotEquals(metar1, metar2);
        }
        
        @Test
        @DisplayName("Objects with different wind data are not equal")
        void differentWindData() {
            NoaaMetarData metar1 = new NoaaMetarData("METAR KJFK 151830Z", "KJFK", testObservationTime);
            metar1.setWindDirectionDegrees(280);
            metar1.setWindSpeedKnots(16);
            
            NoaaMetarData metar2 = new NoaaMetarData("METAR KJFK 151830Z", "KJFK", testObservationTime);
            metar2.setWindDirectionDegrees(290);
            metar2.setWindSpeedKnots(16);
            
            assertNotEquals(metar1, metar2);
        }
        
        @Test
        @DisplayName("Objects with different METAR types are not equal")
        void differentMetarTypes() {
            NoaaMetarData metar1 = new NoaaMetarData("METAR KJFK 151830Z", "KJFK", testObservationTime);
            metar1.setMetarType("METAR");
            
            NoaaMetarData metar2 = new NoaaMetarData("SPECI KJFK 151830Z", "KJFK", testObservationTime);
            metar2.setMetarType("SPECI");
            
            assertNotEquals(metar1, metar2);
        }
        
        @Test
        @DisplayName("Equals calls super.equals() correctly")
        void equalsCallsSuper() {
            // Test that base class fields are also considered in equals
            NoaaMetarData metar1 = new NoaaMetarData("METAR KJFK 151830Z", "KJFK", testObservationTime);
            NoaaMetarData metar2 = new NoaaMetarData("METAR KLGA 151830Z", "KLGA", testObservationTime);
            
            assertNotEquals(metar1, metar2); // Different station IDs from base class
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Boundary Values")
    class EdgeCasesAndBoundaryValues {
        
        @Test
        @DisplayName("Extreme temperature values")
        void extremeTemperatureValues() {
            // Test very cold temperature
            metarData.setTemperatureCelsius(-50.0);
            assertEquals(-50.0, metarData.getTemperatureCelsius());
            assertEquals(-58.0, metarData.getTemperatureFahrenheit(), 0.01);
            
            // Test very hot temperature
            metarData.setTemperatureCelsius(50.0);
            assertEquals(50.0, metarData.getTemperatureCelsius());
            assertEquals(122.0, metarData.getTemperatureFahrenheit(), 0.01);
        }
        
        @Test
        @DisplayName("Edge case wind speeds")
        void edgeWindSpeeds() {
            // Calm winds
            metarData.setWindSpeedKnots(0);
            assertEquals(0, metarData.getWindSpeedKnots());
            
            // Very high winds
            metarData.setWindSpeedKnots(200);
            assertEquals(200, metarData.getWindSpeedKnots());
        }
        
        @Test
        @DisplayName("Edge case visibility values")
        void edgeVisibilityValues() {
            // Zero visibility
            metarData.setVisibilityStatuteMiles(0.0);
            assertEquals(0.0, metarData.getVisibilityStatuteMiles());
            
            // Unlimited visibility
            metarData.setVisibilityStatuteMiles(99.0);
            assertEquals(99.0, metarData.getVisibilityStatuteMiles());
            
            // Fractional visibility
            metarData.setVisibilityStatuteMiles(0.25);
            assertEquals(0.25, metarData.getVisibilityStatuteMiles());
        }
        
        @Test
        @DisplayName("Edge case altimeter values")
        void edgeAltimeterValues() {
            // Very low pressure
            metarData.setAltimeterInHg(25.00);
            assertEquals(25.00, metarData.getAltimeterInHg());
            
            // Very high pressure
            metarData.setAltimeterInHg(35.00);
            assertEquals(35.00, metarData.getAltimeterInHg());
        }
        
        @Test
        @DisplayName("Complex weather string")
        void complexWeatherString() {
            String complexWeather = "-RASN BR FG";
            metarData.setWeatherString(complexWeather);
            assertEquals(complexWeather, metarData.getWeatherString());
        }
        
        @Test
        @DisplayName("Multiple sky condition layers")
        void multipleSkyConditionLayers() {
            String skyCondition = "FEW015 SCT030 BKN080 OVC250";
            metarData.setSkyCondition(skyCondition);
            assertEquals(skyCondition, metarData.getSkyCondition());
        }
    }
    
    @Nested
    @DisplayName("Real World Data Tests")
    class RealWorldDataTests {
        
        @Test
        @DisplayName("Typical JFK METAR data")
        void typicalJfkMetar() {
            String rawText = "METAR KJFK 151851Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 SLP210 T02220122";
            
            NoaaMetarData metar = new NoaaMetarData(rawText, "KJFK", testObservationTime);
            metar.setTemperatureCelsius(22.0);
            metar.setDewpointCelsius(12.0);
            metar.setWindDirectionDegrees(280);
            metar.setWindSpeedKnots(16);
            metar.setVisibilityStatuteMiles(10.0);
            metar.setSkyCondition("FEW250");
            metar.setAltimeterInHg(30.15);
            metar.setFlightCategory("VFR");
            
            assertEquals("KJFK", metar.getStationId());
            assertEquals("METAR", metar.getMetarType());
            assertFalse(metar.getIsAutoReport());
            assertEquals(71.6, metar.getTemperatureFahrenheit(), 0.01);
            assertEquals(53.6, metar.getDewpointFahrenheit(), 0.01);
            assertTrue(metar.isCurrent());
        }
        
        @Test
        @DisplayName("SPECI with gusty winds")
        void speciWithGustyWinds() {
            String rawText = "SPECI KJFK 151915Z AUTO 29025G35KT 3SM -RA BR BKN008 OVC015 18/17 A2995";
            
            NoaaMetarData metar = new NoaaMetarData(rawText, "KJFK", testObservationTime);
            metar.setTemperatureCelsius(18.0);
            metar.setDewpointCelsius(17.0);
            metar.setWindDirectionDegrees(290);
            metar.setWindSpeedKnots(25);
            metar.setWindGustKnots(35);
            metar.setVisibilityStatuteMiles(3.0);
            metar.setWeatherString("-RA BR");
            metar.setSkyCondition("BKN008 OVC015");
            metar.setAltimeterInHg(29.95);
            metar.setFlightCategory("IFR");
            
            assertEquals("SPECI", metar.getMetarType());
            assertTrue(metar.getIsAutoReport());
            assertEquals(35, metar.getWindGustKnots());
            assertEquals("IFR", metar.getFlightCategory());
        }
        
        @Test
        @DisplayName("METAR with variable wind direction")
        void metarWithVariableWind() {
            NoaaMetarData metar = new NoaaMetarData();
            metar.setStationId("KLGA");
            metar.setObservationTime(testObservationTime);
            metar.setWindDirectionDegrees(240);
            metar.setWindSpeedKnots(8);
            metar.setWindVariableDirection("210V270");
            
            assertEquals("210V270", metar.getWindVariableDirection());
        }
        
        @Test
        @DisplayName("METAR with precipitation data")
        void metarWithPrecipitation() {
            NoaaMetarData metar = new NoaaMetarData();
            metar.setStationId("KEWR");
            metar.setObservationTime(testObservationTime);
            metar.setPrecipitationLastHourInches(0.05);
            metar.setPrecipitationLast3HoursInches(0.12);
            metar.setPrecipitationLast6HoursInches(0.25);
            
            assertEquals(0.05, metar.getPrecipitationLastHourInches());
            assertEquals(0.12, metar.getPrecipitationLast3HoursInches());
            assertEquals(0.25, metar.getPrecipitationLast6HoursInches());
        }
    }
    
    @Nested
    @DisplayName("Integration with Base Class")
    class IntegrationWithBaseClass {
        
        @Test
        @DisplayName("Base class fields work correctly in METAR context")
        void baseClassFieldsInMetarContext() {
            metarData.setStationId("KJFK");
            metarData.setObservationTime(testObservationTime);
            metarData.setLatitude(40.6413);
            metarData.setLongitude(-73.7781);
            metarData.setElevationFeet(13);
            metarData.setQualityControlFlags("AUTO A02");
            
            // Verify base class fields
            assertEquals("KJFK", metarData.getStationId());
            assertEquals(testObservationTime, metarData.getObservationTime());
            assertEquals(40.6413, metarData.getLatitude());
            assertEquals(-73.7781, metarData.getLongitude());
            assertEquals(13, metarData.getElevationFeet());
            assertEquals("AUTO A02", metarData.getQualityControlFlags());
            
            // Verify METAR-specific behavior
            assertTrue(metarData.isCurrent());
            assertEquals("METAR", metarData.getReportType());
        }
        
        @Test
        @DisplayName("toString includes both base and METAR fields")
        void toStringIncludesBothTypes() {
            metarData.setStationId("KJFK");
            metarData.setObservationTime(testObservationTime);
            metarData.setRawText("METAR KJFK 151830Z 28016KT");
            metarData.setTemperatureCelsius(22.0);
            metarData.setWindSpeedKnots(16);
            
            String result = metarData.toString();
            
            assertTrue(result.contains("NoaaMetarData"));
            assertTrue(result.contains("KJFK"));
            assertTrue(result.contains("METAR"));
        }
    }
}
