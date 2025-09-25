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

@DisplayName("Simplified NoaaMetarData Tests")
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
        assertNotNull(metar.getWindInformation());
        assertNotNull(metar.getWeatherConditions());
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
    @DisplayName("Wind information composition works correctly")
    void testWindInformationComposition() {
        // Get the wind information object
        WindInformation wind = metarData.getWindInformation();
        assertNotNull(wind);
        
        // Set wind data through the composition object
        wind.setWindDirectionDegrees(270);
        wind.setWindSpeedKnots(15);
        wind.setWindGustKnots(22);
        wind.setWindVariableDirection("240V300");
        
        // Verify data is accessible through the same object
        assertEquals(270, metarData.getWindInformation().getWindDirectionDegrees());
        assertEquals(15, metarData.getWindInformation().getWindSpeedKnots());
        assertEquals(22, metarData.getWindInformation().getWindGustKnots());
        assertEquals("240V300", metarData.getWindInformation().getWindVariableDirection());
        
        // Test business logic methods
        assertFalse(metarData.getWindInformation().isCalm());
        assertTrue(metarData.getWindInformation().hasGusts());
        assertEquals("W", metarData.getWindInformation().getWindDirectionCardinal());
    }

    @Test
    @DisplayName("Setting wind information to null creates new instance")
    void testSetWindInformationNull() {
        metarData.setWindInformation(null);
        assertNotNull(metarData.getWindInformation());
    }

    @Test
    @DisplayName("Setting custom wind information object works")
    void testSetCustomWindInformation() {
        WindInformation customWind = new WindInformation(180, 12, null);
        customWind.setWindVariableDirection("160V200");
        
        metarData.setWindInformation(customWind);
        
        assertEquals(180, metarData.getWindInformation().getWindDirectionDegrees());
        assertEquals(12, metarData.getWindInformation().getWindSpeedKnots());
        assertNull(metarData.getWindInformation().getWindGustKnots());
        assertEquals("160V200", metarData.getWindInformation().getWindVariableDirection());
    }

    @Test
    @DisplayName("Weather conditions composition works correctly")
    void testWeatherConditionsComposition() {
        // Get the weather conditions object
        WeatherConditions weather = metarData.getWeatherConditions();
        assertNotNull(weather);
        
        // Set weather data through the composition object
        weather.setVisibilityStatuteMiles(5.0);
        weather.setWeatherString("RA BR");
        weather.setSkyCondition("OVC010");
        
        // Verify data is accessible through the same object
        assertEquals(5.0, metarData.getWeatherConditions().getVisibilityStatuteMiles());
        assertEquals("RA BR", metarData.getWeatherConditions().getWeatherString());
        assertEquals("OVC010", metarData.getWeatherConditions().getSkyCondition());
        
        // Test business logic methods
        assertTrue(metarData.getWeatherConditions().hasGoodVisibility());
        assertTrue(metarData.getWeatherConditions().hasActiveWeather());
    }

    @Test
    @DisplayName("Setting weather conditions to null creates new instance")
    void testSetWeatherConditionsNull() {
        metarData.setWeatherConditions(null);
        assertNotNull(metarData.getWeatherConditions());
    }

    @Test
    @DisplayName("Setting custom weather conditions object works")
    void testSetCustomWeatherConditions() {
        WeatherConditions customWeather = new WeatherConditions(2.5, "FG", "OVC005");
        
        metarData.setWeatherConditions(customWeather);
        
        assertEquals(2.5, metarData.getWeatherConditions().getVisibilityStatuteMiles());
        assertEquals("FG", metarData.getWeatherConditions().getWeatherString());
        assertEquals("OVC005", metarData.getWeatherConditions().getSkyCondition());
        assertFalse(metarData.getWeatherConditions().hasGoodVisibility());
        assertTrue(metarData.getWeatherConditions().hasActiveWeather());
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
        metar1.getWindInformation().setWindDirectionDegrees(270);
        metar1.getWindInformation().setWindSpeedKnots(12);
        metar1.getWeatherConditions().setVisibilityStatuteMiles(10.0);
        
        NoaaMetarData metar2 = new NoaaMetarData("METAR KJFK 251851Z", "KJFK", time);
        metar2.setTemperatureCelsius(15.0);
        metar2.getWindInformation().setWindDirectionDegrees(270);
        metar2.getWindInformation().setWindSpeedKnots(12);
        metar2.getWeatherConditions().setVisibilityStatuteMiles(10.0);
        
        assertEquals(metar1, metar2);
        assertEquals(metar1.hashCode(), metar2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different composition objects")
    void testEqualsNotEqual() {
        LocalDateTime time = LocalDateTime.now();
        
        NoaaMetarData metar1 = new NoaaMetarData("METAR KJFK 251851Z", "KJFK", time);
        metar1.setTemperatureCelsius(15.0);
        metar1.getWindInformation().setWindDirectionDegrees(270);
        
        NoaaMetarData metar2 = new NoaaMetarData("METAR KJFK 251851Z", "KJFK", time);
        metar2.setTemperatureCelsius(15.0);
        metar2.getWindInformation().setWindDirectionDegrees(180); // Different wind direction
        
        assertNotEquals(metar1, metar2);
    }

    @Test
    @DisplayName("hashCode is consistent with composition objects")
    void testHashCodeConsistency() {
        metarData.setTemperatureCelsius(20.0);
        metarData.getWindInformation().setWindDirectionDegrees(90);
        metarData.getWeatherConditions().setVisibilityStatuteMiles(8.0);
        
        int hashCode1 = metarData.hashCode();
        int hashCode2 = metarData.hashCode();
        
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("Complete METAR data workflow with composition objects")
    void testCompleteMetarWorkflow() {
        // Create a complete METAR report
        String rawText = "METAR AUTO KJFK 251851Z 28015G22KT 10SM FEW250 16/M03 A3012 RMK AO2";
        NoaaMetarData metar = new NoaaMetarData(rawText, "KJFK", testTime);
        
        // Set all the data using new API
        metar.setTemperatureCelsius(16.0);
        metar.setDewpointCelsius(-3.0);
        metar.setAltimeterInHg(30.12);
        
        // Set wind information through composition object
        WindInformation wind = metar.getWindInformation();
        wind.setWindDirectionDegrees(280);
        wind.setWindSpeedKnots(15);
        wind.setWindGustKnots(22);
        
        // Set weather conditions through composition object
        WeatherConditions weather = metar.getWeatherConditions();
        weather.setVisibilityStatuteMiles(10.0);
        weather.setWeatherString("");
        weather.setSkyCondition("FEW250");
        
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
        
        // Verify wind information
        WindInformation retrievedWind = metar.getWindInformation();
        assertEquals(280, retrievedWind.getWindDirectionDegrees());
        assertEquals(15, retrievedWind.getWindSpeedKnots());
        assertEquals(22, retrievedWind.getWindGustKnots());
        assertEquals("W", retrievedWind.getWindDirectionCardinal());
        assertTrue(retrievedWind.hasGusts());
        assertFalse(retrievedWind.isCalm());
        
        // Verify weather conditions
        WeatherConditions retrievedWeather = metar.getWeatherConditions();
        assertEquals(10.0, retrievedWeather.getVisibilityStatuteMiles());
        assertEquals("", retrievedWeather.getWeatherString());
        assertEquals("FEW250", retrievedWeather.getSkyCondition());
        assertTrue(retrievedWeather.hasGoodVisibility());
        assertFalse(retrievedWeather.hasActiveWeather());
        
        assertEquals("VFR", metar.getFlightCategory());
        assertTrue(metar.isCurrent());
    }

    @Test
    @DisplayName("Composition objects maintain independence")
    void testCompositionObjectIndependence() {
        // Create two METAR objects
        NoaaMetarData metar1 = new NoaaMetarData();
        NoaaMetarData metar2 = new NoaaMetarData();
        
        // Modify wind information in first METAR
        metar1.getWindInformation().setWindDirectionDegrees(270);
        metar1.getWindInformation().setWindSpeedKnots(15);
        
        // Verify second METAR is unaffected
        assertNull(metar2.getWindInformation().getWindDirectionDegrees());
        assertNull(metar2.getWindInformation().getWindSpeedKnots());
        
        // Modify weather conditions in second METAR
        metar2.getWeatherConditions().setVisibilityStatuteMiles(5.0);
        metar2.getWeatherConditions().setWeatherString("RA");
        
        // Verify first METAR is unaffected
        assertNull(metar1.getWeatherConditions().getVisibilityStatuteMiles());
        assertNull(metar1.getWeatherConditions().getWeatherString());
    }
}
