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

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WeatherConditions Tests")
class WeatherConditionsTest {

    private WeatherConditions weatherConditions;

    @BeforeEach
    void setUp() {
        weatherConditions = new WeatherConditions();
    }

    @Test
    @DisplayName("Default constructor creates empty weather conditions")
    void testDefaultConstructor() {
        assertNull(weatherConditions.getVisibilityStatuteMiles());
        assertNull(weatherConditions.getWeatherString());
        assertNull(weatherConditions.getSkyCondition());
    }

    @Test
    @DisplayName("Parameterized constructor sets weather values")
    void testParameterizedConstructor() {
        WeatherConditions weather = new WeatherConditions(10.0, "RA BR", "OVC010");
        
        assertEquals(10.0, weather.getVisibilityStatuteMiles());
        assertEquals("RA BR", weather.getWeatherString());
        assertEquals("OVC010", weather.getSkyCondition());
    }

    @Test
    @DisplayName("Visibility setter and getter work correctly")
    void testVisibilitySetterGetter() {
        weatherConditions.setVisibilityStatuteMiles(5.5);
        assertEquals(5.5, weatherConditions.getVisibilityStatuteMiles());
        
        weatherConditions.setVisibilityStatuteMiles(null);
        assertNull(weatherConditions.getVisibilityStatuteMiles());
    }

    @Test
    @DisplayName("Weather string setter and getter work correctly")
    void testWeatherStringSetterGetter() {
        weatherConditions.setWeatherString("-SN");
        assertEquals("-SN", weatherConditions.getWeatherString());
        
        weatherConditions.setWeatherString(null);
        assertNull(weatherConditions.getWeatherString());
    }

    @Test
    @DisplayName("Sky condition setter and getter work correctly")
    void testSkyConditionSetterGetter() {
        weatherConditions.setSkyCondition("BKN020");
        assertEquals("BKN020", weatherConditions.getSkyCondition());
        
        weatherConditions.setSkyCondition(null);
        assertNull(weatherConditions.getSkyCondition());
    }

    @Test
    @DisplayName("hasGoodVisibility returns false for null visibility")
    void testHasGoodVisibilityWithNull() {
        assertFalse(weatherConditions.hasGoodVisibility());
    }

    @Test
    @DisplayName("hasGoodVisibility returns false for visibility less than 3 miles")
    void testHasGoodVisibilityWithLowVisibility() {
        weatherConditions.setVisibilityStatuteMiles(2.5);
        assertFalse(weatherConditions.hasGoodVisibility());
    }

    @Test
    @DisplayName("hasGoodVisibility returns true for visibility 3 miles or greater")
    void testHasGoodVisibilityWithGoodVisibility() {
        weatherConditions.setVisibilityStatuteMiles(3.0);
        assertTrue(weatherConditions.hasGoodVisibility());
        
        weatherConditions.setVisibilityStatuteMiles(10.0);
        assertTrue(weatherConditions.hasGoodVisibility());
    }

    @Test
    @DisplayName("hasActiveWeather returns false for null weather string")
    void testHasActiveWeatherWithNull() {
        assertFalse(weatherConditions.hasActiveWeather());
    }

    @Test
    @DisplayName("hasActiveWeather returns false for empty weather string")
    void testHasActiveWeatherWithEmpty() {
        weatherConditions.setWeatherString("");
        assertFalse(weatherConditions.hasActiveWeather());
        
        weatherConditions.setWeatherString("   ");
        assertFalse(weatherConditions.hasActiveWeather());
    }

    @Test
    @DisplayName("hasActiveWeather returns true for non-empty weather string")
    void testHasActiveWeatherWithWeather() {
        weatherConditions.setWeatherString("RA");
        assertTrue(weatherConditions.hasActiveWeather());
        
        weatherConditions.setWeatherString("  -SN BR  ");
        assertTrue(weatherConditions.hasActiveWeather());
    }

    @Test
    @DisplayName("toString includes all weather information")
    void testToStringComplete() {
        weatherConditions.setVisibilityStatuteMiles(7.5);
        weatherConditions.setWeatherString("RA BR");
        weatherConditions.setSkyCondition("OVC015");
        
        String result = weatherConditions.toString();
        assertEquals("Weather Conditions: Visibility 7.5 miles, Weather: RA BR, Sky: OVC015", result);
    }

    @Test
    @DisplayName("toString works with only visibility")
    void testToStringVisibilityOnly() {
        weatherConditions.setVisibilityStatuteMiles(10.0);
        
        String result = weatherConditions.toString();
        assertEquals("Weather Conditions: Visibility 10.0 miles, ", result);
    }

    @Test
    @DisplayName("toString works with only weather string")
    void testToStringWeatherOnly() {
        weatherConditions.setWeatherString("TS");
        
        String result = weatherConditions.toString();
        assertEquals("Weather Conditions: Weather: TS, ", result);
    }

    @Test
    @DisplayName("toString works with only sky condition")
    void testToStringSkyOnly() {
        weatherConditions.setSkyCondition("CLR");
        
        String result = weatherConditions.toString();
        assertEquals("Weather Conditions: Sky: CLR", result);
    }

    @Test
    @DisplayName("toString works with no weather information")
    void testToStringEmpty() {
        String result = weatherConditions.toString();
        assertEquals("Weather Conditions: ", result);
    }

    @Test
    @DisplayName("equals returns true for same object")
    void testEqualsSameObject() {
        assertEquals(weatherConditions, weatherConditions);
    }

    @Test
    @DisplayName("equals returns false for null")
    void testEqualsNull() {
        assertNotEquals(weatherConditions, null);
    }

    @Test
    @DisplayName("equals returns false for different class")
    void testEqualsDifferentClass() {
        assertNotEquals(weatherConditions,"not a weather object");
    }

    @Test
    @DisplayName("equals returns true for equal weather conditions")
    void testEqualsEqual() {
        WeatherConditions weather1 = new WeatherConditions(5.0, "RA", "OVC010");
        WeatherConditions weather2 = new WeatherConditions(5.0, "RA", "OVC010");
        
        assertEquals(weather1,weather2);
        assertEquals(weather1.hashCode(), weather2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different weather conditions")
    void testEqualsNotEqual() {
        WeatherConditions weather1 = new WeatherConditions(5.0, "RA", "OVC010");
        WeatherConditions weather2 = new WeatherConditions(10.0, "RA", "OVC010");
        
        assertNotEquals(weather1,weather2);
    }

    @Test
    @DisplayName("equals handles null fields correctly")
    void testEqualsWithNullFields() {
        WeatherConditions weather1 = new WeatherConditions();
        WeatherConditions weather2 = new WeatherConditions();
        
        assertEquals(weather1,weather2);
        
        weather1.setVisibilityStatuteMiles(5.0);
        assertNotEquals(weather1,weather2);
        
        weather2.setVisibilityStatuteMiles(5.0);
        assertEquals(weather1,weather2);
    }

    @Test
    @DisplayName("hashCode is consistent")
    void testHashCodeConsistency() {
        weatherConditions.setVisibilityStatuteMiles(8.0);
        weatherConditions.setWeatherString("FG");
        
        int hashCode1 = weatherConditions.hashCode();
        int hashCode2 = weatherConditions.hashCode();
        
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("hashCode is equal for equal objects")
    void testHashCodeEquality() {
        WeatherConditions weather1 = new WeatherConditions(3.0, "BR", "SCT020");
        WeatherConditions weather2 = new WeatherConditions(3.0, "BR", "SCT020");
        
        assertEquals(weather1.hashCode(), weather2.hashCode());
    }

    @Test
    @DisplayName("hashCode handles null fields correctly")
    void testHashCodeWithNullFields() {
        WeatherConditions weather1 = new WeatherConditions();
        WeatherConditions weather2 = new WeatherConditions();
        
        assertEquals(weather1.hashCode(), weather2.hashCode());
    }
}
