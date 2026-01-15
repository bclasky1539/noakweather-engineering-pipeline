/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025-2026 bclasky1539
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
import org.junit.jupiter.api.DisplayName;
import weather.model.WeatherDataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WeatherParser.WeatherParseException
 * 
 * @author bclasky1539
 *
 */
class WeatherParseExceptionTest {
    
    @Test
    @DisplayName("Should create parse exception with message")
    void testParseExceptionWithMessage() {
        String rawData = "INVALID DATA";
        WeatherDataSource source = WeatherDataSource.NOAA;
        
        WeatherParseException exception =
            new WeatherParseException(
                "Failed to parse", 
                rawData, 
                source
            );
        
        assertEquals("Failed to parse", exception.getMessage());
        assertEquals(rawData, exception.getRawData());
        assertEquals(source, exception.getSource());
        assertNull(exception.getCause());
    }
    
    @Test
    @DisplayName("Should create parse exception with cause")
    void testParseExceptionWithCause() {
        String rawData = "INVALID DATA";
        WeatherDataSource source = WeatherDataSource.OPENWEATHERMAP;
        Exception cause = new RuntimeException("XML parse error");
        
        WeatherParseException exception = 
            new WeatherParseException(
                "Failed to parse", 
                cause,
                rawData, 
                source
            );
        
        assertEquals("Failed to parse", exception.getMessage());
        assertEquals(rawData, exception.getRawData());
        assertEquals(source, exception.getSource());
        assertEquals(cause, exception.getCause());
    }
    
    @Test
    @DisplayName("Should preserve raw data in exception")
    void testRawDataPreservation() {
        String complexRawData = """
            {
                "weather": "invalid",
                "temp": "not a number"
            }
            """;
        
        WeatherParseException exception = 
            new WeatherParseException(
                "JSON parse error",
                complexRawData,
                WeatherDataSource.OPENWEATHERMAP
            );
        
        assertEquals(complexRawData, exception.getRawData());
    }
    
    @Test
    @DisplayName("Should handle null raw data")
    void testNullRawData() {
        WeatherParseException exception = 
            new WeatherParseException(
                "No data provided",
                null,
                WeatherDataSource.NOAA
            );
        
        assertNull(exception.getRawData());
    }
    
    @Test
    @DisplayName("Should have meaningful toString")
    void testToString() {
        WeatherParseException exception = 
            new WeatherParseException(
                "Parse failed",
                "some raw data",
                WeatherDataSource.NOAA
            );
        
        String str = exception.toString();
        assertTrue(str.contains("NOAA"));
        assertTrue(str.contains("Parse failed"));
    }
    
    @Test
    @DisplayName("Should handle null raw data in toString")
    void testToStringNullRawData() {
        WeatherParseException exception = 
            new WeatherParseException(
                "Parse failed",
                null,
                WeatherDataSource.NOAA
            );
        
        String str = exception.toString();
        assertTrue(str.contains("rawDataLength=0"));
    }
}
