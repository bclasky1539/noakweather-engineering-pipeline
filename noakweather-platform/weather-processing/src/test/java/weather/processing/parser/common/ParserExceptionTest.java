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
package weather.processing.parser.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ParserException custom exception class.
 * Verifies exception creation, context preservation, and formatting.
 * 
 * @author bclasky1539
 *
 */
class ParserExceptionTest {
    
    @Test
    @DisplayName("Should create exception with message and context")
    void testCreateWithMessageAndContext() {
        String message = "Failed to parse METAR";
        String rawData = "METAR KJFK 251651Z";
        String parserType = "NOAA_METAR";
        
        ParserException exception = new ParserException(message, rawData, parserType);
        
        assertEquals(message, exception.getMessage());
        assertEquals(rawData, exception.getRawData());
        assertEquals(parserType, exception.getParserType());
        assertNull(exception.getCause());
    }
    
    @Test
    @DisplayName("Should create exception with cause")
    void testCreateWithCause() {
        String message = "Parse failed";
        String rawData = "invalid data";
        String parserType = "NOAA_METAR";
        RuntimeException cause = new RuntimeException("Root cause");
        
        ParserException exception = new ParserException(message, cause, rawData, parserType);
        
        assertEquals(message, exception.getMessage());
        assertEquals(rawData, exception.getRawData());
        assertEquals(parserType, exception.getParserType());
        assertEquals(cause, exception.getCause());
    }
    
    @Test
    @DisplayName("Should handle null raw data")
    void testWithNullRawData() {
        ParserException exception = new ParserException("error", null, "NOAA_METAR");
        
        assertNull(exception.getRawData());
        assertNotNull(exception.getMessage());
    }
    
    @Test
    @DisplayName("Should handle null parser type")
    void testWithNullParserType() {
        ParserException exception = new ParserException("error", "data", null);
        
        assertNull(exception.getParserType());
        assertNotNull(exception.getMessage());
    }
    
    @Test
    @DisplayName("Should truncate long raw data in toString")
    void testToStringWithLongRawData() {
        String message = "Parse error";
        String longData = "A".repeat(100); // 100 characters
        String parserType = "NOAA_METAR";
        
        ParserException exception = new ParserException(message, longData, parserType);
        String str = exception.toString();
        
        assertTrue(str.contains("NOAA_METAR"));
        assertTrue(str.contains("Parse error"));
        assertTrue(str.contains("...")); // Should be truncated
        assertFalse(str.contains(longData)); // Full data should not be present
    }
    
    @Test
    @DisplayName("Should not truncate short raw data in toString")
    void testToStringWithShortRawData() {
        String message = "Parse error";
        String shortData = "METAR KJFK";
        String parserType = "NOAA_METAR";
        
        ParserException exception = new ParserException(message, shortData, parserType);
        String str = exception.toString();
        
        assertTrue(str.contains("NOAA_METAR"));
        assertTrue(str.contains("Parse error"));
        assertTrue(str.contains(shortData));
        assertFalse(str.contains("...")); // Should not be truncated
    }
    
    @Test
    @DisplayName("Should handle null raw data in toString")
    void testToStringWithNullRawData() {
        ParserException exception = new ParserException("error", null, "NOAA_METAR");
        String str = exception.toString();
        
        assertTrue(str.contains("NOAA_METAR"));
        assertTrue(str.contains("error"));
        assertTrue(str.contains("rawData='null'"));
    }
    
    @Test
    @DisplayName("Should include all context in toString")
    void testToStringFormat() {
        ParserException exception = new ParserException(
            "Invalid format",
            "METAR KJFK",
            "NOAA_METAR"
        );
        
        String str = exception.toString();
        
        assertTrue(str.startsWith("ParserException{"));
        assertTrue(str.contains("parser='NOAA_METAR'"));
        assertTrue(str.contains("message='Invalid format'"));
        assertTrue(str.contains("rawData='METAR KJFK'"));
    }
    
    @Test
    @DisplayName("Should be throwable as standard exception")
    void testThrowable() {
        ParserException exception = assertThrows(ParserException.class, () -> {
            throw new ParserException("error", "data", "parser");
        });
    
        // Verify the thrown exception has correct properties
        assertEquals("error", exception.getMessage());
        assertEquals("data", exception.getRawData());
        assertEquals("parser", exception.getParserType());
    }
    
    @Test
    @DisplayName("Should preserve cause chain")
    void testCauseChain() {
        RuntimeException rootCause = new RuntimeException("root");
        IllegalArgumentException middleCause = new IllegalArgumentException("middle", rootCause);
        ParserException exception = new ParserException("top", middleCause, "data", "parser");
        
        assertEquals(middleCause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
    }
}
