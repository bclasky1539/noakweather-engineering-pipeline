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
package noakweather.service;

import noakweather.config.WeatherConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for WeatherServiceImpl.
 * 
 * This test suite validates the weather service implementation including
 * data retrieval, station code validation, URL building, and error handling.
 * 
 * @author bclasky1539
 * 
 */
class WeatherServiceImplTest {
    
    private TestConfigurationService testConfigService;
    private WeatherServiceImpl weatherService;
    
    @BeforeEach
    void setUp() {
        testConfigService = new TestConfigurationService();
        
        // Set up default configuration values
        testConfigService.setRawString("MISC_METAR_URL", "https://tgftp.nws.noaa.gov/data/observations/metar/stations/");
        testConfigService.setRawString("MISC_METAR_EXT", ".TXT");
        testConfigService.setRawString("MISC_TAF_URL", "https://tgftp.nws.noaa.gov/data/forecasts/taf/stations/");
        testConfigService.setRawString("MISC_TAF_EXT", ".TXT");
        
        weatherService = new WeatherServiceImpl(testConfigService);
    }
    
    // ===== Constructor Tests =====
    
    @Test
    @DisplayName("Constructor should accept configuration service")
    void testConstructor() {
        WeatherServiceImpl service = new WeatherServiceImpl(testConfigService);
        assertNotNull(service);
        assertEquals("NoakWeather Core Service", service.getServiceProviderName());
    }
    
    @Test
    @DisplayName("Constructor should handle null configuration service")
    void testConstructorWithNullConfig() {
        assertDoesNotThrow(() -> new WeatherServiceImpl(null));
    }
    
    // ===== METAR Data Tests =====
    
    @Test
    @DisplayName("getMetarData should return formatted METAR data for valid station")
    void testGetMetarDataValidStation() throws WeatherServiceException {
        String result = weatherService.getMetarData("KJFK");
        
        assertNotNull(result);
        assertTrue(result.startsWith("METAR KJFK"));
        assertTrue(result.contains("141753Z"));
        assertTrue(result.contains("24012KT"));
        
        // Verify configuration was accessed
        assertTrue(testConfigService.wasConfigRequested("MISC_METAR_URL"));
        assertTrue(testConfigService.wasConfigRequested("MISC_METAR_EXT"));
    }
    
    @Test
    @DisplayName("getMetarData should normalize station code to uppercase")
    void testGetMetarDataNormalizesStationCode() throws WeatherServiceException {
        String result = weatherService.getMetarData("kjfk");
        
        assertNotNull(result);
        assertTrue(result.startsWith("METAR KJFK"));
    }
    
    @Test
    @DisplayName("getMetarData should handle station code with whitespace")
    void testGetMetarDataTrimsWhitespace() throws WeatherServiceException {
        String result = weatherService.getMetarData("  KJFK  ");
        
        assertNotNull(result);
        assertTrue(result.startsWith("METAR KJFK"));
    }
    
    @ParameterizedTest
    @DisplayName("getMetarData should throw exception for invalid station codes")
    @ValueSource(strings = {"", "KJ", "KJFKK", "K1FK", "KJ-K", "123", "INVALID123"})
    @NullAndEmptySource
    void testGetMetarDataInvalidStationCodes(String invalidStation) {
        WeatherServiceException exception = assertThrows(
            WeatherServiceException.class,
            () -> weatherService.getMetarData(invalidStation)
        );
        
        assertEquals(WeatherServiceException.ErrorType.INVALID_STATION_CODE, exception.getErrorType());
        assertEquals("Station code must be 3-4 alphabetic characters", exception.getMessage());
        assertEquals(invalidStation, exception.getStationCode());
    }
    
    @Test
    @DisplayName("getMetarData should handle configuration service errors gracefully")
    void testGetMetarDataConfigurationError() {
        testConfigService.setShouldThrowException(true);
        
        // Should not throw exception - should use fallback URL
        assertDoesNotThrow(() -> weatherService.getMetarData("KJFK"));
    }
    
    // ===== TAF Data Tests =====
    
    @Test
    @DisplayName("getTafData should return formatted TAF data for valid station")
    void testGetTafDataValidStation() throws WeatherServiceException {
        String result = weatherService.getTafData("KJFK");
        
        assertNotNull(result);
        assertTrue(result.startsWith("TAF KJFK"));
        assertTrue(result.contains("141152Z"));
        assertTrue(result.contains("24012KT"));
        assertTrue(result.contains("FM1600"));
        
        // Verify configuration was accessed
        assertTrue(testConfigService.wasConfigRequested("MISC_TAF_URL"));
        assertTrue(testConfigService.wasConfigRequested("MISC_TAF_EXT"));
    }
    
    @Test
    @DisplayName("getTafData should normalize station code to uppercase")
    void testGetTafDataNormalizesStationCode() throws WeatherServiceException {
        String result = weatherService.getTafData("klax");
        
        assertNotNull(result);
        assertTrue(result.startsWith("TAF KLAX"));
    }
    
    @Test
    @DisplayName("getTafData should handle station code with whitespace")
    void testGetTafDataTrimsWhitespace() throws WeatherServiceException {
        String result = weatherService.getTafData("  KLAX  ");
        
        assertNotNull(result);
        assertTrue(result.startsWith("TAF KLAX"));
    }
    
    @ParameterizedTest
    @DisplayName("getTafData should throw exception for invalid station codes")
    @ValueSource(strings = {"", "KJ", "KJFKK", "K1FK", "KJ-K", "123", "INVALID123"})
    @NullAndEmptySource
    void testGetTafDataInvalidStationCodes(String invalidStation) {
        WeatherServiceException exception = assertThrows(
            WeatherServiceException.class,
            () -> weatherService.getTafData(invalidStation)
        );
        
        assertEquals(WeatherServiceException.ErrorType.INVALID_STATION_CODE, exception.getErrorType());
        assertEquals("Station code must be 3-4 alphabetic characters", exception.getMessage());
        assertEquals(invalidStation, exception.getStationCode());
    }
    
    @Test
    @DisplayName("getTafData should handle configuration service errors gracefully")
    void testGetTafDataConfigurationError() {
        testConfigService.setShouldThrowException(true);
        
        // Should not throw exception - should use fallback URL
        assertDoesNotThrow(() -> weatherService.getTafData("KJFK"));
    }
    
    // ===== Station Code Validation Tests =====
    
    @ParameterizedTest
    @DisplayName("isValidStationCode should return true for valid station codes")
    @ValueSource(strings = {"KJFK", "LAX", "EGLL", "LFPG", "kjfk", "lax", " KJFK ", "\tLAX\t"})
    void testIsValidStationCodeValid(String validStation) {
        assertTrue(weatherService.isValidStationCode(validStation));
    }
    
    @ParameterizedTest
    @DisplayName("isValidStationCode should return false for invalid station codes")
    @ValueSource(strings = {"", "   ", "KJ", "KJFKK", "K1FK", "KJ-K", "KJ FK", "123", "12AB", "AB12"})
    @NullAndEmptySource
    void testIsValidStationCodeInvalid(String invalidStation) {
        assertFalse(weatherService.isValidStationCode(invalidStation));
    }
    
    @Test
    @DisplayName("isValidStationCode should handle edge cases")
    void testIsValidStationCodeEdgeCases() {
        // Exactly 3 characters (valid)
        assertTrue(weatherService.isValidStationCode("ABC"));
        
        // Exactly 4 characters (valid)
        assertTrue(weatherService.isValidStationCode("ABCD"));
        
        // 2 characters (invalid)
        assertFalse(weatherService.isValidStationCode("AB"));
        
        // 5 characters (invalid)
        assertFalse(weatherService.isValidStationCode("ABCDE"));
        
        // Mixed case should work
        assertTrue(weatherService.isValidStationCode("aBc"));
        assertTrue(weatherService.isValidStationCode("AbCd"));
    }
    
    // ===== Service Provider Tests =====
    
    @Test
    @DisplayName("getServiceProviderName should return correct name")
    void testGetServiceProviderName() {
        String providerName = weatherService.getServiceProviderName();
        
        assertNotNull(providerName);
        assertEquals("NoakWeather Core Service", providerName);
    }
    
    // ===== URL Building Tests =====
    
    @Test
    @DisplayName("METAR URL building should use configuration values")
    void testMetarUrlBuilding() throws WeatherServiceException {
        weatherService.getMetarData("TEST");
        
        assertTrue(testConfigService.wasConfigRequested("MISC_METAR_URL"));
        assertTrue(testConfigService.wasConfigRequested("MISC_METAR_EXT"));
    }
    
    @Test
    @DisplayName("TAF URL building should use configuration values")
    void testTafUrlBuilding() throws WeatherServiceException {
        weatherService.getTafData("TEST");
        
        assertTrue(testConfigService.wasConfigRequested("MISC_TAF_URL"));
        assertTrue(testConfigService.wasConfigRequested("MISC_TAF_EXT"));
    }
    
    @Test
    @DisplayName("Should handle missing METAR configuration with fallback")
    void testMetarUrlFallback() throws WeatherServiceException {
        testConfigService.setShouldThrowException(true);
        
        String result = weatherService.getMetarData("KJFK");
        
        assertNotNull(result);
        assertTrue(result.startsWith("METAR KJFK"));
        // Should still work with fallback URL
    }
    
    @Test
    @DisplayName("Should handle missing TAF configuration with fallback")
    void testTafUrlFallback() throws WeatherServiceException {
        testConfigService.setShouldThrowException(true);
        
        String result = weatherService.getTafData("KJFK");
        
        assertNotNull(result);
        assertTrue(result.startsWith("TAF KJFK"));
        // Should still work with fallback URL
    }
    
    // ===== Integration Tests =====
    
    @Test
    @DisplayName("Should handle multiple consecutive requests")
    void testMultipleRequests() throws WeatherServiceException {
        String metar1 = weatherService.getMetarData("KJFK");
        String taf1 = weatherService.getTafData("KJFK");
        String metar2 = weatherService.getMetarData("KLAX");
        String taf2 = weatherService.getTafData("KLAX");
        
        assertNotNull(metar1);
        assertNotNull(taf1);
        assertNotNull(metar2);
        assertNotNull(taf2);
        
        assertTrue(metar1.contains("KJFK"));
        assertTrue(taf1.contains("KJFK"));
        assertTrue(metar2.contains("KLAX"));
        assertTrue(taf2.contains("KLAX"));
    }
    
    @Test
    @DisplayName("Should maintain consistent behavior across calls")
    void testConsistentBehavior() throws WeatherServiceException {
        String result1 = weatherService.getMetarData("KJFK");
        String result2 = weatherService.getMetarData("KJFK");
        
        // Results should be consistent (same format, though content might vary in real implementation)
        assertTrue(result1.startsWith("METAR KJFK"));
        assertTrue(result2.startsWith("METAR KJFK"));
        
        // Both should contain expected elements
        assertTrue(result1.contains("141753Z"));
        assertTrue(result2.contains("141753Z"));
    }
    
    // ===== Error Scenario Tests =====
    
    @Test
    @DisplayName("Should handle configuration service returning null")
    void testConfigurationReturnsNull() {
        testConfigService.setShouldReturnNull(true);
        
        // Should handle gracefully and use fallback
        assertDoesNotThrow(() -> weatherService.getMetarData("KJFK"));
        assertDoesNotThrow(() -> weatherService.getTafData("KJFK"));
    }
    
    @Test
    @DisplayName("Should handle configuration service throwing various exceptions")
    void testConfigurationThrowsException() {
        testConfigService.setShouldThrowException(true);
        
        // Should handle gracefully and use fallback
        assertDoesNotThrow(() -> weatherService.getMetarData("KJFK"));
        assertDoesNotThrow(() -> weatherService.getTafData("KJFK"));
    }
    
    // ===== Test Configuration Service Implementation =====
    
    /**
     * Test implementation of WeatherConfigurationService for testing WeatherServiceImpl.
     * This replaces the need for Mockito by providing a controllable test double.
     */
    private static class TestConfigurationService implements WeatherConfigurationService {
        private final Map<String, String> configurations = new HashMap<>();
        private final Map<String, Boolean> requestTracker = new HashMap<>();
        private boolean shouldThrowException = false;
        private boolean shouldReturnNull = false;
        
        public void setRawString(String key, String value) {
            configurations.put(key, value);
        }
        
        public void setShouldThrowException(boolean shouldThrow) {
            this.shouldThrowException = shouldThrow;
        }
        
        public void setShouldReturnNull(boolean shouldReturnNull) {
            this.shouldReturnNull = shouldReturnNull;
        }
        
        public boolean wasConfigRequested(String key) {
            return requestTracker.getOrDefault(key, false);
        }
        
        @Override
        public String getRawString(String key) {
            requestTracker.put(key, true);
            
            if (shouldThrowException) {
                throw new RuntimeException("Test configuration error");
            }
            
            if (shouldReturnNull) {
                return null;
            }
            
            return configurations.get(key);
        }
        
        @Override
        public String getRawString(String key, Object... arguments) {
            String template = getRawString(key);
            if (template != null && arguments.length > 0) {
                return String.format(template, arguments);
            }
            return template;
        }
        
        // Weather domain methods - minimal implementation for testing
        @Override
        public String getWeatherCondition(String condition) {
            return "Test weather condition: " + condition;
        }
        
        @Override
        public String getWeatherDescription(String condition) {
            return "Test weather description: " + condition;
        }
        
        @Override
        public String getIntensityDescription(String intensity) {
            return "Test intensity: " + intensity;
        }
        
        @Override
        public String getCloudType(String type) {
            return "Test cloud type: " + type;
        }
        
        @Override
        public String getCloudDescription(String type) {
            return "Test cloud description: " + type;
        }
        
        @Override
        public String getWindDirection(String direction) {
            return "Test wind direction: " + direction;
        }
        
        @Override
        public String getWindUnit(String unit) {
            return "Test wind unit: " + unit;
        }
        
        @Override
        public String getWindDescription(String condition) {
            return "Test wind description: " + condition;
        }
        
        @Override
        public String getExceptionMessage(String type) {
            return "Test exception message: " + type;
        }
        
        @Override
        public String getLogMessage(String level) {
            return "Test log message: " + level;
        }
        
        @Override
        public void setLocale(Locale locale) {
            // Test implementation - no-op
        }
    }
}
