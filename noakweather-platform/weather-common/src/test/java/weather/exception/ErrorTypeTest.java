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
package weather.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ErrorType enum.
 * 
 * @author bclasky1539
 *
 */
class ErrorTypeTest {
    
    @Test
    void testAllErrorTypesExist() {
        // Verify all expected error types are defined
        ErrorType[] types = ErrorType.values();
        assertEquals(7, types.length, "Should have 7 error types");
    }
    
    @Test
    void testInvalidStationCode() {
        ErrorType type = ErrorType.INVALID_STATION_CODE;
        assertNotNull(type);
        assertEquals("Invalid station code format", type.getDescription());
    }
    
    @Test
    void testStationNotFound() {
        ErrorType type = ErrorType.STATION_NOT_FOUND;
        assertNotNull(type);
        assertEquals("Station not found", type.getDescription());
    }
    
    @Test
    void testServiceUnavailable() {
        ErrorType type = ErrorType.SERVICE_UNAVAILABLE;
        assertNotNull(type);
        assertEquals("Weather service unavailable", type.getDescription());
    }
    
    @Test
    void testNetworkError() {
        ErrorType type = ErrorType.NETWORK_ERROR;
        assertNotNull(type);
        assertEquals("Network communication error", type.getDescription());
    }
    
    @Test
    void testInvalidResponse() {
        ErrorType type = ErrorType.INVALID_RESPONSE;
        assertNotNull(type);
        assertEquals("Invalid response from weather service", type.getDescription());
    }
    
    @Test
    void testTimeout() {
        ErrorType type = ErrorType.TIMEOUT;
        assertNotNull(type);
        assertEquals("Request timeout", type.getDescription());
    }
    
    @Test
    void testConfigurationError() {
        ErrorType type = ErrorType.CONFIGURATION_ERROR;
        assertNotNull(type);
        assertEquals("Configuration error", type.getDescription());
    }
    
    @Test
    void testValueOf() {
        // Test that valueOf works correctly
        ErrorType type = ErrorType.valueOf("NETWORK_ERROR");
        assertEquals(ErrorType.NETWORK_ERROR, type);
    }
    
    @Test
    void testValueOfInvalid() {
        // Test that invalid valueOf throws exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ErrorType.valueOf("INVALID_TYPE");
        });
    
        // Verify exception message
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("INVALID_TYPE") || 
                   exception.getMessage().contains("No enum constant"));
    }
    
    @Test
    void testEnumEquality() {
        ErrorType type1 = ErrorType.TIMEOUT;
        ErrorType type2 = ErrorType.TIMEOUT;
        assertSame(type1, type2, "Enum instances should be same object");
    }
}
