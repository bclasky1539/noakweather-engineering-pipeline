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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for WeatherServiceException.
 * 
 * This test suite validates the weather service exception including
 * all constructor variations, error type functionality, and message formatting.
 * 
 * @author bclasky1539
 */
public class WeatherServiceExceptionTest {
    
    private static final String TEST_MESSAGE = "Test error message";
    private static final String TEST_STATION = "KJFK";
    private static final RuntimeException TEST_CAUSE = new RuntimeException("Root cause");
    
    // ===== Constructor Tests =====
    
    @Test
    @DisplayName("Constructor with error type and message should work correctly")
    void testConstructorWithErrorTypeAndMessage() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.INVALID_STATION_CODE, 
            TEST_MESSAGE
        );
        
        assertEquals(WeatherServiceException.ErrorType.INVALID_STATION_CODE, exception.getErrorType());
        assertEquals(TEST_MESSAGE, exception.getMessage());
        assertNull(exception.getStationCode());
        assertNull(exception.getCause());
    }
    
    @Test
    @DisplayName("Constructor with error type, message, and station code should work correctly")
    void testConstructorWithErrorTypeMessageAndStation() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.STATION_NOT_FOUND, 
            TEST_MESSAGE, 
            TEST_STATION
        );
        
        assertEquals(WeatherServiceException.ErrorType.STATION_NOT_FOUND, exception.getErrorType());
        assertEquals(TEST_MESSAGE, exception.getMessage());
        assertEquals(TEST_STATION, exception.getStationCode());
        assertNull(exception.getCause());
    }
    
    @Test
    @DisplayName("Constructor with error type, message, and cause should work correctly")
    void testConstructorWithErrorTypeMessageAndCause() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.NETWORK_ERROR, 
            TEST_MESSAGE, 
            TEST_CAUSE
        );
        
        assertEquals(WeatherServiceException.ErrorType.NETWORK_ERROR, exception.getErrorType());
        assertEquals(TEST_MESSAGE, exception.getMessage());
        assertNull(exception.getStationCode());
        assertEquals(TEST_CAUSE, exception.getCause());
    }
    
    @Test
    @DisplayName("Constructor with all parameters should work correctly")
    void testConstructorWithAllParameters() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.SERVICE_UNAVAILABLE, 
            TEST_MESSAGE, 
            TEST_STATION, 
            TEST_CAUSE
        );
        
        assertEquals(WeatherServiceException.ErrorType.SERVICE_UNAVAILABLE, exception.getErrorType());
        assertEquals(TEST_MESSAGE, exception.getMessage());
        assertEquals(TEST_STATION, exception.getStationCode());
        assertEquals(TEST_CAUSE, exception.getCause());
    }
    
    // ===== Constructor Edge Cases =====
    
    @Test
    @DisplayName("Constructor should handle null message")
    void testConstructorWithNullMessage() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.UNKNOWN_ERROR, 
            null
        );
        
        assertEquals(WeatherServiceException.ErrorType.UNKNOWN_ERROR, exception.getErrorType());
        assertNull(exception.getMessage());
        assertNull(exception.getStationCode());
    }
    
    @Test
    @DisplayName("Constructor should handle null station code")
    void testConstructorWithNullStationCode() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.INVALID_STATION_CODE, 
            TEST_MESSAGE, 
            (String) null
        );
        
        assertEquals(WeatherServiceException.ErrorType.INVALID_STATION_CODE, exception.getErrorType());
        assertEquals(TEST_MESSAGE, exception.getMessage());
        assertNull(exception.getStationCode());
    }
    
    @Test
    @DisplayName("Constructor should handle null cause")
    void testConstructorWithNullCause() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.DATA_PARSING_ERROR, 
            TEST_MESSAGE, 
            (Throwable) null
        );
        
        assertEquals(WeatherServiceException.ErrorType.DATA_PARSING_ERROR, exception.getErrorType());
        assertEquals(TEST_MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }
    
    @Test
    @DisplayName("Constructor should handle empty message")
    void testConstructorWithEmptyMessage() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.CONFIGURATION_ERROR, 
            ""
        );
        
        assertEquals(WeatherServiceException.ErrorType.CONFIGURATION_ERROR, exception.getErrorType());
        assertEquals("", exception.getMessage());
    }
    
    @Test
    @DisplayName("Constructor should handle empty station code")
    void testConstructorWithEmptyStationCode() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.STATION_NOT_FOUND, 
            TEST_MESSAGE, 
            ""
        );
        
        assertEquals(WeatherServiceException.ErrorType.STATION_NOT_FOUND, exception.getErrorType());
        assertEquals("", exception.getStationCode());
    }
    
    // ===== ErrorType Enum Tests =====
    
    @ParameterizedTest
    @EnumSource(WeatherServiceException.ErrorType.class)
    @DisplayName("All error types should have descriptions")
    void testAllErrorTypesHaveDescriptions(WeatherServiceException.ErrorType errorType) {
        String description = errorType.getDescription();
        
        assertNotNull(description);
        assertFalse(description.isEmpty());
        assertFalse(description.isBlank());
    }
    
    @Test
    @DisplayName("ErrorType.INVALID_STATION_CODE should have correct description")
    void testInvalidStationCodeDescription() {
        assertEquals("Invalid station code format", 
                    WeatherServiceException.ErrorType.INVALID_STATION_CODE.getDescription());
    }
    
    @Test
    @DisplayName("ErrorType.STATION_NOT_FOUND should have correct description")
    void testStationNotFoundDescription() {
        assertEquals("Station not found", 
                    WeatherServiceException.ErrorType.STATION_NOT_FOUND.getDescription());
    }
    
    @Test
    @DisplayName("ErrorType.NETWORK_ERROR should have correct description")
    void testNetworkErrorDescription() {
        assertEquals("Network communication error", 
                    WeatherServiceException.ErrorType.NETWORK_ERROR.getDescription());
    }
    
    @Test
    @DisplayName("ErrorType.SERVICE_UNAVAILABLE should have correct description")
    void testServiceUnavailableDescription() {
        assertEquals("Weather service is unavailable", 
                    WeatherServiceException.ErrorType.SERVICE_UNAVAILABLE.getDescription());
    }
    
    @Test
    @DisplayName("ErrorType.DATA_PARSING_ERROR should have correct description")
    void testDataParsingErrorDescription() {
        assertEquals("Error parsing weather data", 
                    WeatherServiceException.ErrorType.DATA_PARSING_ERROR.getDescription());
    }
    
    @Test
    @DisplayName("ErrorType.CONFIGURATION_ERROR should have correct description")
    void testConfigurationErrorDescription() {
        assertEquals("Service configuration error", 
                    WeatherServiceException.ErrorType.CONFIGURATION_ERROR.getDescription());
    }
    
    @Test
    @DisplayName("ErrorType.UNKNOWN_ERROR should have correct description")
    void testUnknownErrorDescription() {
        assertEquals("Unknown error occurred", 
                    WeatherServiceException.ErrorType.UNKNOWN_ERROR.getDescription());
    }
    
    // ===== User-Friendly Message Tests =====
    
    @Test
    @DisplayName("getUserFriendlyMessage should return error type description only")
    void testGetUserFriendlyMessageErrorTypeOnly() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.NETWORK_ERROR, 
            null
        );
        
        String userMessage = exception.getUserFriendlyMessage();
        assertEquals("Network communication error", userMessage);
    }
    
    @Test
    @DisplayName("getUserFriendlyMessage should include station code when present")
    void testGetUserFriendlyMessageWithStationCode() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.STATION_NOT_FOUND, 
            null, 
            TEST_STATION
        );
        
        String userMessage = exception.getUserFriendlyMessage();
        assertEquals("Station not found for station KJFK", userMessage);
    }
    
    @Test
    @DisplayName("getUserFriendlyMessage should include detailed message when present")
    void testGetUserFriendlyMessageWithDetailedMessage() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.DATA_PARSING_ERROR, 
            "Unexpected data format"
        );
        
        String userMessage = exception.getUserFriendlyMessage();
        assertEquals("Error parsing weather data: Unexpected data format", userMessage);
    }
    
    @Test
    @DisplayName("getUserFriendlyMessage should include both station code and detailed message")
    void testGetUserFriendlyMessageWithStationAndMessage() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.INVALID_STATION_CODE, 
            "Station code must be 3-4 characters", 
            "XY"
        );
        
        String userMessage = exception.getUserFriendlyMessage();
        assertEquals("Invalid station code format for station XY: Station code must be 3-4 characters", userMessage);
    }
    
    @Test
    @DisplayName("getUserFriendlyMessage should handle empty message gracefully")
    void testGetUserFriendlyMessageWithEmptyMessage() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.SERVICE_UNAVAILABLE, 
            ""
        );
        
        String userMessage = exception.getUserFriendlyMessage();
        assertEquals("Weather service is unavailable", userMessage);
    }
    
    @Test
    @DisplayName("getUserFriendlyMessage should handle empty station code gracefully")
    void testGetUserFriendlyMessageWithEmptyStationCode() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.CONFIGURATION_ERROR, 
            TEST_MESSAGE, 
            ""
        );
        
        String userMessage = exception.getUserFriendlyMessage();
        assertEquals("Service configuration error for station : Test error message", userMessage);
    }
    
    // ===== toString() Tests =====
    
    @Test
    @DisplayName("toString should format exception details correctly")
    void testToStringWithAllDetails() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.NETWORK_ERROR, 
            TEST_MESSAGE, 
            TEST_STATION
        );
        
        String toString = exception.toString();
        assertTrue(toString.contains("NETWORK_ERROR"));
        assertTrue(toString.contains("KJFK"));
        assertTrue(toString.contains("Test error message"));
        assertTrue(toString.startsWith("WeatherServiceException{"));
    }
    
    @Test
    @DisplayName("toString should handle null station code")
    void testToStringWithNullStationCode() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.DATA_PARSING_ERROR, 
            TEST_MESSAGE
        );
        
        String toString = exception.toString();
        assertTrue(toString.contains("DATA_PARSING_ERROR"));
        assertTrue(toString.contains("stationCode='null'"));
        assertTrue(toString.contains("Test error message"));
    }
    
    @Test
    @DisplayName("toString should handle null message")
    void testToStringWithNullMessage() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.UNKNOWN_ERROR, 
            null, 
            TEST_STATION
        );
        
        String toString = exception.toString();
        assertTrue(toString.contains("UNKNOWN_ERROR"));
        assertTrue(toString.contains("KJFK"));
        assertTrue(toString.contains("message='null'"));
    }
    
    // ===== Integration Tests =====
    
    @Test
    @DisplayName("Exception should be throwable and catchable")
    void testExceptionThrowableAndCatchable() {
        WeatherServiceException thrownException = assertThrows(
            WeatherServiceException.class,
            () -> {
                throw new WeatherServiceException(
                    WeatherServiceException.ErrorType.SERVICE_UNAVAILABLE,
                    "Service is down",
                    "KLAX"
                );
            }
        );
        
        assertEquals(WeatherServiceException.ErrorType.SERVICE_UNAVAILABLE, thrownException.getErrorType());
        assertEquals("Service is down", thrownException.getMessage());
        assertEquals("KLAX", thrownException.getStationCode());
    }
    
    @Test
    @DisplayName("Exception should work in try-catch blocks")
    void testExceptionInTryCatch() {
        WeatherServiceException caughtException = null;
        
        try {
            throw new WeatherServiceException(
                WeatherServiceException.ErrorType.INVALID_STATION_CODE,
                "Invalid format",
                "123"
            );
        } catch (WeatherServiceException e) {
            caughtException = e;
        }
        
        assertNotNull(caughtException);
        assertEquals(WeatherServiceException.ErrorType.INVALID_STATION_CODE, caughtException.getErrorType());
        assertEquals("Invalid format", caughtException.getMessage());
        assertEquals("123", caughtException.getStationCode());
    }
    
    @Test
    @DisplayName("Exception should inherit from Exception correctly")
    void testExceptionInheritance() {
        WeatherServiceException exception = new WeatherServiceException(
            WeatherServiceException.ErrorType.NETWORK_ERROR,
            "Connection failed"
        );
        
        // Should be instance of Exception
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
        
        // Should work as Exception
        Exception genericException = exception;
        assertEquals("Connection failed", genericException.getMessage());
    }
    
    // ===== Error Type Coverage Tests =====
    
    @Test
    @DisplayName("All error types should be testable")
    void testAllErrorTypes() {
        // Test each error type can be used in exception creation
        for (WeatherServiceException.ErrorType errorType : WeatherServiceException.ErrorType.values()) {
            WeatherServiceException exception = new WeatherServiceException(
                errorType,
                "Test message for " + errorType.name()
            );
            
            assertEquals(errorType, exception.getErrorType());
            assertNotNull(exception.getUserFriendlyMessage());
            assertTrue(exception.getUserFriendlyMessage().contains(errorType.getDescription()));
        }
    }
    
    @Test
    @DisplayName("Error type enum should have expected number of values")
    void testErrorTypeEnumSize() {
        WeatherServiceException.ErrorType[] errorTypes = WeatherServiceException.ErrorType.values();
        assertEquals(7, errorTypes.length);
    }
    
    @Test
    @DisplayName("Error type valueOf should work correctly")
    void testErrorTypeValueOf() {
        assertEquals(WeatherServiceException.ErrorType.INVALID_STATION_CODE, 
                    WeatherServiceException.ErrorType.valueOf("INVALID_STATION_CODE"));
        assertEquals(WeatherServiceException.ErrorType.NETWORK_ERROR, 
                    WeatherServiceException.ErrorType.valueOf("NETWORK_ERROR"));
        assertEquals(WeatherServiceException.ErrorType.UNKNOWN_ERROR, 
                    WeatherServiceException.ErrorType.valueOf("UNKNOWN_ERROR"));
    }
    
    @Test
    @DisplayName("Error type name() should work correctly")
    void testErrorTypeName() {
        assertEquals("INVALID_STATION_CODE", 
                    WeatherServiceException.ErrorType.INVALID_STATION_CODE.name());
        assertEquals("SERVICE_UNAVAILABLE", 
                    WeatherServiceException.ErrorType.SERVICE_UNAVAILABLE.name());
        assertEquals("DATA_PARSING_ERROR", 
                    WeatherServiceException.ErrorType.DATA_PARSING_ERROR.name());
    }
}
