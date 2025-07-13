/*
 * noakweather(TM) is a Java library for parsing weather data
 * Copyright (C) 2025 bclasky1539
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package noakweather.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Resource Bundle Weather Configuration Service Test
 *
 * 
 * @author bclasky1539
 *
 */
class ResourceBundleWeatherConfigurationServiceTest {
    
    private ResourceBundleWeatherConfigurationService service;
    
    @BeforeEach
    void setUp() {
        // Create service with default locale
        service = new ResourceBundleWeatherConfigurationService();
    }
    
    @Test
    void testDefaultConstructor() {
        ResourceBundleWeatherConfigurationService defaultService = new ResourceBundleWeatherConfigurationService();
        assertNotNull(defaultService);
        
        // Should be able to access some configuration
        assertDoesNotThrow(() -> {
            defaultService.getRawString("MISC_WEATHER_DATA");
        });
    }
    
    @Test
    void testConstructorWithLocale() {
        Locale testLocale = Locale.US;
        ResourceBundleWeatherConfigurationService localeService = 
            new ResourceBundleWeatherConfigurationService(testLocale);
        assertNotNull(localeService);
        
        // Should be able to access configuration
        assertDoesNotThrow(() -> {
            localeService.getRawString("MISC_WEATHER_DATA");
        });
    }
    
    @Test
    void testSetLocale() {
        Locale originalLocale = Locale.getDefault();
        
        try {
            // Test setting different locale
            service.setLocale(Locale.FRENCH);
            assertEquals(Locale.FRENCH, Locale.getDefault());
            
            // Should still be able to access configuration
            assertDoesNotThrow(() -> {
                service.getRawString("MISC_WEATHER_DATA");
            });
            
        } finally {
            // Restore original locale
            Locale.setDefault(originalLocale);
        }
    }
    
    @Test
    void testGetWeatherCondition() {
        // Test with a known weather condition from configs.properties
        String result = service.getWeatherCondition("HEAVY");
        assertNotNull(result);
        assertEquals("+", result); // From WEATHER_HEAVY=+
    }
    
    @Test
    void testGetWeatherConditionMissing() {
        // Test with non-existent weather condition
        String result = service.getWeatherCondition("NONEXISTENT");
        assertNotNull(result);
        assertTrue(result.contains("Missing config: WEATHER_NONEXISTENT"));
    }
    
    @Test
    void testGetWeatherDescription() {
        // Test with a known weather description
        String result = service.getWeatherDescription("HEAVY");
        assertNotNull(result);
        assertEquals("Heavy", result); // From WEATHER_DECODED_HEAVY=Heavy
    }
    
    @Test
    void testGetWeatherDescriptionMissing() {
        // Test with non-existent weather description
        String result = service.getWeatherDescription("NONEXISTENT");
        assertNotNull(result);
        assertTrue(result.contains("Missing config: WEATHER_DECODED_NONEXISTENT"));
    }
    
    @Test
    void testGetIntensityDescription() {
        // Test with a known intensity
        String result = service.getIntensityDescription("LIGHT");
        assertNotNull(result);
        assertEquals("Light", result); // From WEATHER_DECODED_LIGHT=Light
    }
    
    @Test
    void testGetIntensityDescriptionMissing() {
        // Test with non-existent intensity
        String result = service.getIntensityDescription("NONEXISTENT");
        assertNotNull(result);
        assertTrue(result.contains("Missing config: WEATHER_DECODED_NONEXISTENT"));
    }
    
    @Test
    void testGetCloudType() {
        // Test with a known cloud type
        String result = service.getCloudType("CUMULONIMBUS");
        assertNotNull(result);
        assertEquals("CB", result); // From CLOUD_CUMULONIMBUS=CB
    }
    
    @Test
    void testGetCloudTypeMissing() {
        // Test with non-existent cloud type
        String result = service.getCloudType("NONEXISTENT");
        assertNotNull(result);
        assertTrue(result.contains("Missing config: CLOUD_NONEXISTENT"));
    }
    
    @Test
    void testGetCloudDescription() {
        // Test with a known cloud description
        String result = service.getCloudDescription("CUMULONIMBUS");
        assertNotNull(result);
        assertEquals("Cumulonimbus", result); // From CLOUD_DECODED_CUMULONIMBUS=Cumulonimbus
    }
    
    @Test
    void testGetCloudDescriptionMissing() {
        // Test with non-existent cloud description
        String result = service.getCloudDescription("NONEXISTENT");
        assertNotNull(result);
        assertTrue(result.contains("Missing config: CLOUD_DECODED_NONEXISTENT"));
    }
    
    @Test
    void testGetWindDirection() {
        // Test with a known wind direction
        String result = service.getWindDirection("NORTH");
        assertNotNull(result);
        assertEquals("N", result); // From WIND_DIR_NORTH=N
    }
    
    @Test
    void testGetWindDirectionMissing() {
        // Test with non-existent wind direction
        String result = service.getWindDirection("NONEXISTENT");
        assertNotNull(result);
        assertTrue(result.contains("Missing config: WIND_DIR_NONEXISTENT"));
    }
    
    @Test
    void testGetWindUnit() {
        // Test with a known wind unit
        String result = service.getWindUnit("KNOTS_1");
        assertNotNull(result);
        assertEquals("KT", result); // From WIND_KNOTS_1=KT
    }
    
    @Test
    void testGetWindUnitMissing() {
        // Test with non-existent wind unit
        String result = service.getWindUnit("NONEXISTENT");
        assertNotNull(result);
        assertTrue(result.contains("Missing config: WIND_NONEXISTENT"));
    }
    
    @Test
    void testGetWindDescription() {
        // Test with a known wind description
        String result = service.getWindDescription("KNOTS");
        assertNotNull(result);
        assertEquals("knots", result); // From WIND_DECODED_KNOTS=knots
    }
    
    @Test
    void testGetWindDescriptionMissing() {
        // Test with non-existent wind description
        String result = service.getWindDescription("NONEXISTENT");
        assertNotNull(result);
        assertTrue(result.contains("Missing config: WIND_DECODED_NONEXISTENT"));
    }
    
    @Test
    void testGetExceptionMessage() {
        // Test with a known exception message
        String result = service.getExceptionMessage("NULL_POINTER_EXCEPTION");
        assertNotNull(result);
        assertEquals("Null Pointer exception:", result); // From EXCEP_NULL_POINTER_EXCEPTION=Null Pointer exception:
    }
    
    @Test
    void testGetExceptionMessageMissing() {
        // Test with non-existent exception message
        String result = service.getExceptionMessage("NONEXISTENT");
        assertNotNull(result);
        assertTrue(result.contains("Missing config: EXCEP_NONEXISTENT"));
    }
    
    @Test
    void testGetLogMessage() {
        // Test with a known log message
        String result = service.getLogMessage("INFO");
        assertNotNull(result);
        assertEquals("noakweather logging is INFO", result); // From LOG_DECODED_INFO=noakweather logging is INFO
    }
    
    @Test
    void testGetLogMessageMissing() {
        // Test with non-existent log message
        String result = service.getLogMessage("NONEXISTENT");
        assertNotNull(result);
        assertTrue(result.contains("Missing config: LOG_DECODED_NONEXISTENT"));
    }
    
    @Test
    void testGetRawStringSimple() {
        // Test with a known raw string
        String result = service.getRawString("MISC_WEATHER_DATA");
        assertNotNull(result);
        assertEquals("Weather Data:", result); // From MISC_WEATHER_DATA=Weather Data:
    }
    
    @Test
    void testGetRawStringMissing() {
        // Test with non-existent key
        String result = service.getRawString("NONEXISTENT_KEY");
        assertNotNull(result);
        assertTrue(result.contains("Missing config: NONEXISTENT_KEY"));
    }
    
    @Test
    void testGetRawStringWithArguments() {
        // Test message formatting with arguments
        // Using a key that contains {0} placeholder
        String result = service.getRawString("EXCEP_FAILED_FETCH_STATION", "KJFK");
        assertNotNull(result);
        // Should format the message with the station code
        assertTrue(result.contains("Failed to fetch weather data for station"));
    }
    
    @Test
    void testGetRawStringWithArgumentsMissing() {
        // Test formatting with non-existent key
        String result = service.getRawString("NONEXISTENT_KEY", "arg1", "arg2");
        assertNotNull(result);
        assertTrue(result.contains("Missing config: NONEXISTENT_KEY"));
    }
    
    @Test
    void testGetRawStringWithNullArguments() {
        // Test with null arguments array
        String result = service.getRawString("MISC_WEATHER_DATA", (Object[]) null);
        assertNotNull(result);
        assertEquals("Weather Data:", result);
    }
    
    @Test
    void testGetRawStringWithEmptyArguments() {
        // Test with empty arguments array
        String result = service.getRawString("MISC_WEATHER_DATA", new Object[]{});
        assertNotNull(result);
        assertEquals("Weather Data:", result);
    }
    
    @Test
    void testMultipleMethodCallsConsistency() {
        // Test that multiple calls return consistent results
        String first = service.getRawString("MISC_WEATHER_DATA");
        String second = service.getRawString("MISC_WEATHER_DATA");
        assertEquals(first, second);
        
        String weatherFirst = service.getWeatherCondition("HEAVY");
        String weatherSecond = service.getWeatherCondition("HEAVY");
        assertEquals(weatherFirst, weatherSecond);
    }
    
    @Test
    void testErrorHandlingWithNullKey() {
        // Test behavior with null key - should handle gracefully
        assertThrows(Exception.class, () -> {
            service.getRawString(null);
        });
    }
    
    @Test
    void testErrorHandlingWithEmptyKey() {
        // Test behavior with empty key
        String result = service.getRawString("");
        assertNotNull(result);
        assertTrue(result.contains("Missing config:"));
    }
    
    @Test
    void testSetLocaleWithNull() {
        // Test setting null locale - should throw NullPointerException
        assertThrows(NullPointerException.class, () -> {
            service.setLocale(null);
        });
    }
    
    @Test
    void testDomainMethodsIntegration() {
        // Test that domain-specific methods work together
        String cloudType = service.getCloudType("CUMULUS");
        String cloudDesc = service.getCloudDescription("CUMULUS");
        
        assertNotNull(cloudType);
        assertNotNull(cloudDesc);
        assertEquals("CU", cloudType);
        assertEquals("Cumulus", cloudDesc);
    }
    
    @Test
    void testAllDomainMethodsWithValidData() {
        // Comprehensive test of all domain methods with known valid data
        assertDoesNotThrow(() -> {
            service.getWeatherCondition("HEAVY");
            service.getWeatherDescription("HEAVY");
            service.getIntensityDescription("LIGHT");
            service.getCloudType("CUMULUS");
            service.getCloudDescription("CUMULUS");
            service.getWindDirection("NORTH");
            service.getWindUnit("KNOTS_1");
            service.getWindDescription("KNOTS");
            service.getExceptionMessage("NULL_POINTER_EXCEPTION");
            service.getLogMessage("INFO");
        });
    }
}
