/*
 * Copyright 2025 bdeveloper.
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
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WeatherDataSource enum
 * 
 * @author bclasky1539
 *
 */
class WeatherDataSourceTest {
    
    @Test
    @DisplayName("Should have correct display names")
    void testDisplayNames() {
        assertEquals("NOAA Aviation Weather Center", WeatherDataSource.NOAA.getDisplayName());
        assertEquals("OpenWeatherMap", WeatherDataSource.OPENWEATHERMAP.getDisplayName());
        assertEquals("WeatherAPI", WeatherDataSource.WEATHERAPI.getDisplayName());
    }
    
    @Test
    @DisplayName("Should have correct base URLs")
    void testBaseUrls() {
        assertEquals("https://aviationweather.gov", WeatherDataSource.NOAA.getBaseUrl());
        assertEquals("https://openweathermap.org", WeatherDataSource.OPENWEATHERMAP.getBaseUrl());
        assertEquals("https://weatherapi.com", WeatherDataSource.WEATHERAPI.getBaseUrl());
    }
    
    @Test
    @DisplayName("Should correctly identify government sources")
    void testIsGovernmentSource() {
        assertTrue(WeatherDataSource.NOAA.isGovernmentSource());
        assertTrue(WeatherDataSource.INTERNAL.isGovernmentSource());
        
        assertFalse(WeatherDataSource.OPENWEATHERMAP.isGovernmentSource());
        assertFalse(WeatherDataSource.WEATHERAPI.isGovernmentSource());
        assertFalse(WeatherDataSource.VISUAL_CROSSING.isGovernmentSource());
    }
    
    @Test
    @DisplayName("Should parse valid source strings")
    void testFromStringValid() {
        assertEquals(WeatherDataSource.NOAA, WeatherDataSource.fromString("NOAA"));
        assertEquals(WeatherDataSource.NOAA, WeatherDataSource.fromString("noaa"));
        assertEquals(WeatherDataSource.OPENWEATHERMAP, WeatherDataSource.fromString("OPENWEATHERMAP"));
        assertEquals(WeatherDataSource.OPENWEATHERMAP, WeatherDataSource.fromString("openweathermap"));
    }
    
    @Test
    @DisplayName("Should handle null and blank strings")
    void testFromStringNullAndBlank() {
        assertEquals(WeatherDataSource.UNKNOWN, WeatherDataSource.fromString(null));
        assertEquals(WeatherDataSource.UNKNOWN, WeatherDataSource.fromString(""));
        assertEquals(WeatherDataSource.UNKNOWN, WeatherDataSource.fromString("   "));
    }
    
    @Test
    @DisplayName("Should return UNKNOWN for invalid source strings")
    void testFromStringInvalid() {
        assertEquals(WeatherDataSource.UNKNOWN, WeatherDataSource.fromString("INVALID"));
        assertEquals(WeatherDataSource.UNKNOWN, WeatherDataSource.fromString("random"));
        assertEquals(WeatherDataSource.UNKNOWN, WeatherDataSource.fromString("123"));
    }
    
    @Test
    @DisplayName("Should handle source strings with spaces")
    void testFromStringWithSpaces() {
        // The implementation replaces spaces with underscores
        // This test documents that behavior
        assertEquals(WeatherDataSource.UNKNOWN, WeatherDataSource.fromString("WEATHER API"));
    }
    
    @Test
    @DisplayName("Should have all expected enum values")
    void testAllEnumValues() {
        WeatherDataSource[] sources = WeatherDataSource.values();
        
        assertTrue(sources.length >= 6, "Should have at least 6 weather sources");
        
        // Verify key sources exist
        assertNotNull(WeatherDataSource.valueOf("NOAA"));
        assertNotNull(WeatherDataSource.valueOf("OPENWEATHERMAP"));
        assertNotNull(WeatherDataSource.valueOf("WEATHERAPI"));
        assertNotNull(WeatherDataSource.valueOf("VISUAL_CROSSING"));
        assertNotNull(WeatherDataSource.valueOf("INTERNAL"));
        assertNotNull(WeatherDataSource.valueOf("UNKNOWN"));
    }
}
