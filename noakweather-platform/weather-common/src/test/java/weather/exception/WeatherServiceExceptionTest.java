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
 * Unit tests for WeatherServiceException.
 * 
 * @author bclasky1539
 *
 */
class WeatherServiceExceptionTest {
    
    @Test
    void testConstructorWithErrorTypeAndMessage() {
        WeatherServiceException exception = new WeatherServiceException(
                ErrorType.NETWORK_ERROR,
                "Connection failed"
        );
        
        assertEquals(ErrorType.NETWORK_ERROR, exception.getErrorType());
        assertNull(exception.getStationCode());
        assertEquals("Connection failed", exception.getMessage());
    }
    
    @Test
    void testConstructorWithErrorTypeMessageAndStation() {
        WeatherServiceException exception = new WeatherServiceException(
                ErrorType.INVALID_STATION_CODE,
                "Invalid format",
                "KJFK"
        );
        
        assertEquals(ErrorType.INVALID_STATION_CODE, exception.getErrorType());
        assertEquals("KJFK", exception.getStationCode());
        assertTrue(exception.getMessage().contains("Invalid format"));
        assertTrue(exception.getMessage().contains("KJFK"));
    }
    
    @Test
    void testConstructorWithErrorTypeMessageStationAndCause() {
        Exception cause = new RuntimeException("Root cause");
        
        WeatherServiceException exception = new WeatherServiceException(
                ErrorType.SERVICE_UNAVAILABLE,
                "Service down",
                "KLGA",
                cause
        );
        
        assertEquals(ErrorType.SERVICE_UNAVAILABLE, exception.getErrorType());
        assertEquals("KLGA", exception.getStationCode());
        assertTrue(exception.getMessage().contains("Service down"));
        assertTrue(exception.getMessage().contains("KLGA"));
        assertEquals(cause, exception.getCause());
    }
    
    @Test
    void testMessageFormattingWithStation() {
        WeatherServiceException exception = new WeatherServiceException(
                ErrorType.TIMEOUT,
                "Request timed out",
                "KEWR"
        );
        
        String message = exception.getMessage();
        assertTrue(message.contains("Request timed out"));
        assertTrue(message.contains("[Station: KEWR]"));
    }
    
    @Test
    void testToString() {
        WeatherServiceException exception = new WeatherServiceException(
                ErrorType.NETWORK_ERROR,
                "Connection failed",
                "KJFK"
        );
        
        String toString = exception.toString();
        assertTrue(toString.contains("WeatherServiceException"));
        assertTrue(toString.contains("NETWORK_ERROR"));
        assertTrue(toString.contains("KJFK"));
        assertTrue(toString.contains("Connection failed"));
    }
    
    @Test
    void testToStringWithoutStation() {
        WeatherServiceException exception = new WeatherServiceException(
                ErrorType.CONFIGURATION_ERROR,
                "Missing configuration"
        );
        
        String toString = exception.toString();
        assertTrue(toString.contains("WeatherServiceException"));
        assertTrue(toString.contains("CONFIGURATION_ERROR"));
        assertTrue(toString.contains("null")); // station should be null
    }
    
    @Test
    void testExceptionChaining() {
        Exception rootCause = new RuntimeException("Network timeout");
        Exception intermediate = new WeatherServiceException(
                ErrorType.NETWORK_ERROR,
                "API failed",
                "KJFK",
                rootCause
        );
        
        assertEquals(rootCause, intermediate.getCause());
        assertTrue(intermediate.getMessage().contains("API failed"));
    }
    
    @Test
    void testNullStationCode() {
        WeatherServiceException exception = new WeatherServiceException(
                ErrorType.SERVICE_UNAVAILABLE,
                "Service down",
                null  // null station code
        );
        
        assertNull(exception.getStationCode());
        assertTrue(exception.getMessage().contains("Service down"));
    }
    
    @Test
    void testAllErrorTypes() {
        // Test that exception works with all error types
        for (ErrorType errorType : ErrorType.values()) {
            WeatherServiceException exception = new WeatherServiceException(
                    errorType,
                    "Test message"
            );
            assertEquals(errorType, exception.getErrorType());
        }
    }
    
    @Test
    void testIsCheckedException() {
        // WeatherServiceException extends Exception, so it's a checked exception
        WeatherServiceException exception = new WeatherServiceException(
                ErrorType.TIMEOUT,
                "Test"
        );
    
        assertTrue(exception instanceof Exception);
        // Since it extends Exception (not RuntimeException), it's a checked exception
    }
}
