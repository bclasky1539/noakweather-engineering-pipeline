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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NoaaMetarData Tests")
class NoaaMetarDataTest {

    private NoaaMetarData metarData;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now().minusHours(1);
        metarData = new NoaaMetarData();
    }

    @Test
    @DisplayName("Default constructor initializes wind and weather objects")
    void testDefaultConstructor() {
        assertNotNull(metarData.getWindInformation());
        assertNotNull(metarData.getWeatherConditions());
        assertEquals("METAR", metarData.getReportType());
    }

    @Test
    @DisplayName("Parameterized constructor sets basic fields and parses raw text")
    void testParameterizedConstructor() {
        String rawText = "METAR AUTO KJFK 251851Z 28015G22KT 10SM FEW250 16/M03 A3012";
        NoaaMetarData metar = new NoaaMetarData(rawText, "KJFK", testTime);
        
        assertEquals(rawText, metar.getRawText());
        assertEquals("KJFK", metar.getStationId());
        assertEquals(testTime, metar.getObservationTime());
        assertEquals("METAR", metar.getMetarType());
        assertEquals("METAR", metar.getReportType());
        assertTrue(metar.getIsAutoReport());
    }

    @Test
    @DisplayName("Constructor handles SPECI reports")
    void testSpeciConstructor() {
        String rawText = "SPECI KJFK 251851Z 28015G22KT 10SM FEW250 16/M03 A3012";
        NoaaMetarData metar = new NoaaMetarData(rawText, "KJFK", testTime);
        
        assertEquals("SPECI", metar.getMetarType());
        assertEquals("SPECI", metar.getReportType());
    }

    @Test
    @DisplayName("Temperature setters and getters work correctly")
    void testTemperatureFields() {
        metarData.setTemperatureCelsius(15.5);
        assertEquals(15.5, metarData.getTemperatureCelsius());
        
        metarData.setDewpointCelsius(-3.2);
        assertEquals(-3.2, metarData.getDewpointCelsius());
    }

    @Test
    @DisplayName("Temperature conversion to Fahrenheit works correctly")
    void testTemperatureConversion() {
        metarData.setTemperatureCelsius(0.0);
        assertEquals(32.0, metarData.getTemperatureFahrenheit());
        
        metarData.setTemperatureCelsius(20.0);
        assertEquals(68.0, metarData.getTemperatureFahrenheit());
        
        metarData.setTemperatureCelsius(-10.0);
        assertEquals(14.0, metarData.getTemperatureFahrenheit());
        
        // Test null handling
        metarData.setTemperatureCelsius(null);
        assertNull(metarData.getTemperatureFahrenheit());
    }

    @Test
    @DisplayName("Dewpoint conversion to Fahrenheit works correctly")
    void testDewpointConversion() {
        metarData.setDewpointCelsius(0.0);
        assertEquals(32.0, metarData.getDewpointFahrenheit());
        
        metarData.setDewpointCelsius(15.0);
        assertEquals(59.0, metarData.getDewpointFahrenheit());
        
        // Test null handling
        metarData.setDewpointCelsius(null);
        assertNull(metarData.getDewpointFahrenheit());
    }

    @Test
    @DisplayName("Altimeter setter and getter work correctly")
    void testAltimeterField() {
        metarData.setAltimeterInHg(30.12);
        assertEquals(30.12, metarData.getAltimeterInHg());
    }

    @Test
    @DisplayName("Wind information delegation works correctly")
    void testWindInformationDelegation() {
        // Test direct access to wind information object
        WindInformation wind = new WindInformation(270, 15, 22);
        metarData.setWindInformation(wind);
        assertEquals(wind, metarData.getWindInformation());
        
        // Test convenience methods delegate correctly
        assertEquals(270, metarData.getWindDirectionDegrees());
        assertEquals(15, metarData.getWindSpeedKnots());
        assertEquals(22, metarData.getWindGustKnots());
    }

    @Test
    @DisplayName("Wind convenience setters work correctly")
    void testWindConvenienceSetters() {
        metarData.setWindDirectionDegrees(180);
        metarData.setWindSpeedKnots(12);
        metarData.setWindGustKnots(18);
        metarData.setWindVariableDirection("160V200");
        
        assertEquals(180, metarData.getWindDirectionDegrees());
        assertEquals(12, metarData.getWindSpeedKnots());
        assertEquals(18, metarData.getWindGustKnots());
        assertEquals("160V200", metarData.getWindVariableDirection());
        
        // Verify they're stored in the wind information object
        WindInformation wind = metarData.getWindInformation();
        assertEquals(180, wind.getWindDirectionDegrees());
        assertEquals(12, wind.getWindSpeedKnots());
        assertEquals(18, wind.getWindGustKnots());
        assertEquals("160V200", wind.getWindVariableDirection());
    }

    @Test
    @DisplayName("Wind setters create wind information object when null")
    void testWindSettersWithNullObject() {
        metarData.setWindInformation(null);
        
        metarData.setWindDirectionDegrees(90);
        assertNotNull(metarData.getWindInformation());
        assertEquals(90, metarData.getWindDirectionDegrees());
    }

    @Test
    @DisplayName("Weather conditions delegation works correctly")
    void testWeatherConditionsDelegation() {
        // Test direct access to weather conditions object
        WeatherConditions weather = new WeatherConditions(5.0, "RA BR", "OVC010");
        metarData.setWeatherConditions(weather);
        assertEquals(weather, metarData.getWeatherConditions());
        
        // Test convenience methods delegate correctly
        assertEquals(5.0, metarData.getVisibilityStatuteMiles());
        assertEquals("RA BR", metarData.getWeatherString());
        assertEquals("OVC010", metarData.getSkyCondition());
    }

    @Test
    @DisplayName("Weather convenience setters work correctly")
    void testWeatherConvenienceSetters() {
        metarData.setVisibilityStatuteMiles(7.5);
        metarData.setWeatherString("-SN");
        metarData.setSkyCondition("BKN020");
        
        assertEquals(7.5, metarData.getVisibilityStatuteMiles());
        assertEquals("-SN", metarData.getWeatherString());
        assertEquals("BKN020", metarData.getSkyCondition());
        
        // Verify they're stored in the weather conditions object
        WeatherConditions weather = metarData.getWeatherConditions();
        assertEquals(7.5, weather.getVisibilityStatuteMiles());
        assertEquals("-SN", weather.getWeatherString());
        assertEquals("BKN020", weather.getSkyCondition());
    }

    @Test
    @DisplayName("Weather setters create weather conditions object when null")
    void testWeatherSettersWithNullObject() {
        metarData.setWeatherConditions(null);
        
        metarData.setVisibilityStatuteMiles(10.0);
        assertNotNull(metarData.getWeatherConditions());
        assertEquals(10.0, metarData.getVisibilityStatuteMiles());
    }

    @Test
    @DisplayName("Flight category setter and getter work correctly")
    void testFlightCategory() {
        metarData.setFlightCategory("VFR");
        assertEquals("VFR", metarData.getFlightCategory());
        
        metarData.setFlightCategory("IFR");
        assertEquals("IFR", metarData.getFlightCategory());
    }

    @Test
    @DisplayName("Precipitation fields work correctly")
    void testPrecipitationFields() {
        metarData.setPrecipitationLastHourInches(0.25);
        metarData.setPrecipitationLast3HoursInches(0.75);
        metarData.setPrecipitationLast6HoursInches(1.5);
        
        assertEquals(0.25, metarData.getPrecipitationLastHourInches());
        assertEquals(0.75, metarData.getPrecipitationLast3HoursInches());
        assertEquals(1.5, metarData.getPrecipitationLast6HoursInches());
    }

    @Test
    @DisplayName("Special fields work correctly")
    void testSpecialFields() {
        metarData.setMetarType("SPECI");
        metarData.setIsAutoReport(true);
        
        assertEquals("SPECI", metarData.getMetarType());
        assertEquals("SPECI", metarData.getReportType());
        assertTrue(metarData.getIsAutoReport());
    }

    @Test
    @DisplayName("isCurrent returns true for recent observations")
    void testIsCurrentRecent() {
        metarData.setObservationTime(LocalDateTime.now().minusHours(2));
        assertTrue(metarData.isCurrent());
    }

    @Test
    @DisplayName("isCurrent returns false for old observations")
    void testIsCurrentOld() {
        metarData.setObservationTime(LocalDateTime.now().minusHours(4));
        assertFalse(metarData.isCurrent());
    }

    @Test
    @DisplayName("isCurrent returns false for null observation time")
    void testIsCurrentNullTime() {
        metarData.setObservationTime(null);
        assertFalse(metarData.isCurrent());
    }

    @Test
    @DisplayName("equals works correctly with composition objects")
    void testEquals() {
        LocalDateTime time = LocalDateTime.now();
        
        NoaaMetarData metar1 = new NoaaMetarData("METAR KJFK 251851Z", "KJFK", time);
        metar1.setTemperatureCelsius(15.0);
        metar1.setWindDirectionDegrees(270);
        metar1.setWindSpeedKnots(12);
        metar1.setVisibilityStatuteMiles(10.0);
        
        NoaaMetarData metar2 = new NoaaMetarData("METAR KJFK 251851Z", "KJFK", time);
        metar2.setTemperatureCelsius(15.0);
        metar2.setWindDirectionDegrees(270);
        metar2.setWindSpeedKnots(12);
        metar2.setVisibilityStatuteMiles(10.0);
        
        assertTrue(metar1.equals(metar2));
        assertEquals(metar1.hashCode(), metar2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different composition objects")
    void testEqualsNotEqual() {
        LocalDateTime time = LocalDateTime.now();
        
        NoaaMetarData metar1 = new NoaaMetarData("METAR KJFK 251851Z", "KJFK", time);
        metar1.setTemperatureCelsius(15.0);
        metar1.setWindDirectionDegrees(270);
        
        NoaaMetarData metar2 = new NoaaMetarData("METAR KJFK 251851Z", "KJFK", time);
        metar2.setTemperatureCelsius(15.0);
        metar2.setWindDirectionDegrees(180); // Different wind direction
        
        assertFalse(metar1.equals(metar2));
    }

    @Test
    @DisplayName("equals handles null composition objects")
    void testEqualsWithNullComposition() {
        NoaaMetarData metar1 = new NoaaMetarData();
        metar1.setWindInformation(null);
        
        NoaaMetarData metar2 = new NoaaMetarData();
        metar2.setWindInformation(null);
        
        assertTrue(metar1.equals(metar2));
    }

    @Test
    @DisplayName("hashCode is consistent with composition objects")
    void testHashCodeConsistency() {
        metarData.setTemperatureCelsius(20.0);
        metarData.setWindDirectionDegrees(90);
        metarData.setVisibilityStatuteMiles(8.0);
        
        int hashCode1 = metarData.hashCode();
        int hashCode2 = metarData.hashCode();
        
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("Setting wind information to null creates new instance")
    void testSetWindInformationNull() {
        metarData.setWindInformation(null);
        assertNotNull(metarData.getWindInformation());
    }

    @Test
    @DisplayName("Setting weather conditions to null creates new instance")
    void testSetWeatherConditionsNull() {
        metarData.setWeatherConditions(null);
        assertNotNull(metarData.getWeatherConditions());
    }

    @Test
    @DisplayName("Wind getters return null when wind information is null")
    void testWindGettersWithNullWindInformation() {
        metarData.setWindInformation(null);
        // Even though we create a new instance in setter, test the null case in getter
        metarData = new NoaaMetarData() {
            @Override
            public WindInformation getWindInformation() {
                return null; // Force null for testing
            }
        };
        
        assertNull(metarData.getWindDirectionDegrees());
        assertNull(metarData.getWindSpeedKnots());
        assertNull(metarData.getWindGustKnots());
        assertNull(metarData.getWindVariableDirection());
    }

    @Test
    @DisplayName("Weather getters return null when weather conditions is null")
    void testWeatherGettersWithNullWeatherConditions() {
        metarData = new NoaaMetarData() {
            @Override
            public WeatherConditions getWeatherConditions() {
                return null; // Force null for testing
            }
        };
        
        assertNull(metarData.getVisibilityStatuteMiles());
        assertNull(metarData.getWeatherString());
        assertNull(metarData.getSkyCondition());
    }

    @Test
    @DisplayName("Complete METAR data workflow")
    void testCompleteMetarWorkflow() {
        // Create a complete METAR report
        String rawText = "METAR AUTO KJFK 251851Z 28015G22KT 10SM FEW250 16/M03 A3012 RMK AO2";
        NoaaMetarData metar = new NoaaMetarData(rawText, "KJFK", testTime);
        
        // Set all the data
        metar.setTemperatureCelsius(16.0);
        metar.setDewpointCelsius(-3.0);
        metar.setAltimeterInHg(30.12);
        
        metar.setWindDirectionDegrees(280);
        metar.setWindSpeedKnots(15);
        metar.setWindGustKnots(22);
        
        metar.setVisibilityStatuteMiles(10.0);
        metar.setWeatherString("");
        metar.setSkyCondition("FEW250");
        metar.setFlightCategory("VFR");
        
        // Verify all data is accessible
        assertEquals("KJFK", metar.getStationId());
        assertEquals(rawText, metar.getRawText());
        assertEquals(testTime, metar.getObservationTime());
        assertEquals("METAR", metar.getReportType());
        assertTrue(metar.getIsAutoReport());
        
        assertEquals(16.0, metar.getTemperatureCelsius());
        assertEquals(60.8, metar.getTemperatureFahrenheit(), 0.01);
        assertEquals(-3.0, metar.getDewpointCelsius());
        assertEquals(26.6, metar.getDewpointFahrenheit(), 0.01);
        assertEquals(30.12, metar.getAltimeterInHg());
        
        assertEquals(280, metar.getWindDirectionDegrees());
        assertEquals(15, metar.getWindSpeedKnots());
        assertEquals(22, metar.getWindGustKnots());
        
        assertEquals(10.0, metar.getVisibilityStatuteMiles());
        assertEquals("", metar.getWeatherString());
        assertEquals("FEW250", metar.getSkyCondition());
        assertEquals("VFR", metar.getFlightCategory());
        
        // Verify composition objects have the data
        WindInformation wind = metar.getWindInformation();
        assertEquals(280, wind.getWindDirectionDegrees());
        assertEquals("W", wind.getWindDirectionCardinal());
        assertTrue(wind.hasGusts());
        assertFalse(wind.isCalm());
        
        WeatherConditions weather = metar.getWeatherConditions();
        assertEquals(10.0, weather.getVisibilityStatuteMiles());
        assertTrue(weather.hasGoodVisibility());
        assertFalse(weather.hasActiveWeather());
        
        assertTrue(metar.isCurrent());
    }
}