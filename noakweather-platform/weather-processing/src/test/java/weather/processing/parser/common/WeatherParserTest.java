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
import weather.model.NoaaWeatherData;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
/**
 *
 * @author bdeveloper
 */



/**
 * Tests for WeatherParser interface.
 * Uses a concrete mock implementation to verify interface contract.
 * 
 * @author bclasky1539
 *
 */
class WeatherParserTest {
    
    /**
     * Mock implementation of WeatherParser for testing.
     */
    private static class MockWeatherParser implements WeatherParser<NoaaWeatherData> {
        
        private final boolean shouldSucceed;
        private final String expectedPrefix;
        
        public MockWeatherParser(boolean shouldSucceed, String expectedPrefix) {
            this.shouldSucceed = shouldSucceed;
            this.expectedPrefix = expectedPrefix;
        }
        
        @Override
        public ParseResult<NoaaWeatherData> parse(String rawData) {
            if (rawData == null || rawData.trim().isEmpty()) {
                return ParseResult.failure("Data cannot be null or empty");
            }
            
            if (!canParse(rawData)) {
                return ParseResult.failure("Cannot parse this data format");
            }
            
            if (shouldSucceed) {
                NoaaWeatherData data = new NoaaWeatherData("TEST", Instant.now(), "MOCK");
                data.setRawData(rawData);
                return ParseResult.success(data);
            } else {
                return ParseResult.failure("Mock parser configured to fail");
            }
        }
        
        @Override
        public boolean canParse(String rawData) {
            return rawData != null && rawData.startsWith(expectedPrefix);
        }
        
        @Override
        public String getSourceType() {
            return "MOCK_PARSER";
        }
    }
    
    @Test
    @DisplayName("Should parse data successfully when implementation succeeds")
    void testParseSuccess() {
        WeatherParser<NoaaWeatherData> parser = new MockWeatherParser(true, "TEST");
        
        ParseResult<NoaaWeatherData> result = parser.parse("TEST DATA");
        
        assertTrue(result.isSuccess());
        assertTrue(result.getData().isPresent());
        assertEquals("TEST", result.getData().get().getStationId());
    }
    
    @Test
    @DisplayName("Should fail to parse when implementation fails")
    void testParseFailure() {
        WeatherParser<NoaaWeatherData> parser = new MockWeatherParser(false, "TEST");
        
        ParseResult<NoaaWeatherData> result = parser.parse("TEST DATA");
        
        assertTrue(result.isFailure());
        assertEquals("Mock parser configured to fail", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should validate parseable data correctly")
    void testCanParse() {
        WeatherParser<NoaaWeatherData> parser = new MockWeatherParser(true, "VALID");
        
        assertTrue(parser.canParse("VALID DATA"));
        assertFalse(parser.canParse("INVALID DATA"));
        assertFalse(parser.canParse(null));
    }
    
    @Test
    @DisplayName("Should return source type identifier")
    void testGetSourceType() {
        WeatherParser<NoaaWeatherData> parser = new MockWeatherParser(true, "TEST");
        
        assertEquals("MOCK_PARSER", parser.getSourceType());
    }
    
    @Test
    @DisplayName("Should handle null input gracefully")
    void testParseNull() {
        WeatherParser<NoaaWeatherData> parser = new MockWeatherParser(true, "TEST");
        
        ParseResult<NoaaWeatherData> result = parser.parse(null);
        
        assertTrue(result.isFailure());
        assertEquals("Data cannot be null or empty", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should handle empty input gracefully")
    void testParseEmpty() {
        WeatherParser<NoaaWeatherData> parser = new MockWeatherParser(true, "TEST");
        
        ParseResult<NoaaWeatherData> result = parser.parse("");
        
        assertTrue(result.isFailure());
        assertEquals("Data cannot be null or empty", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should handle whitespace input gracefully")
    void testParseWhitespace() {
        WeatherParser<NoaaWeatherData> parser = new MockWeatherParser(true, "TEST");
        
        ParseResult<NoaaWeatherData> result = parser.parse("   ");
        
        assertTrue(result.isFailure());
        assertEquals("Data cannot be null or empty", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should fail when data format is not supported")
    void testParseUnsupportedFormat() {
        WeatherParser<NoaaWeatherData> parser = new MockWeatherParser(true, "EXPECTED");
        
        ParseResult<NoaaWeatherData> result = parser.parse("UNEXPECTED DATA");
        
        assertTrue(result.isFailure());
        assertEquals("Cannot parse this data format", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should work with different parser configurations")
    void testDifferentConfigurations() {
        WeatherParser<NoaaWeatherData> parser1 = new MockWeatherParser(true, "TYPE1");
        WeatherParser<NoaaWeatherData> parser2 = new MockWeatherParser(true, "TYPE2");
        
        assertTrue(parser1.canParse("TYPE1 data"));
        assertFalse(parser1.canParse("TYPE2 data"));
        
        assertFalse(parser2.canParse("TYPE1 data"));
        assertTrue(parser2.canParse("TYPE2 data"));
    }
    
    @Test
    @DisplayName("Should store raw data in parsed result")
    void testRawDataPreserved() {
        WeatherParser<NoaaWeatherData> parser = new MockWeatherParser(true, "TEST");
        String rawData = "TEST SAMPLE DATA";
        
        ParseResult<NoaaWeatherData> result = parser.parse(rawData);
        
        assertTrue(result.isSuccess());
        assertEquals(rawData, result.getData().get().getRawData());
    }
    
    @Test
    @DisplayName("Should be usable in polymorphic context")
    void testPolymorphicUsage() {
        // Create array of different parser types
        WeatherParser<?>[] parsers = new WeatherParser[]{
            new MockWeatherParser(true, "TYPE1"),
            new MockWeatherParser(true, "TYPE2"),
            new MockWeatherParser(true, "TYPE3")
        };
        
        // Verify all implement the interface correctly
        for (WeatherParser<?> parser : parsers) {
            assertNotNull(parser.getSourceType());
            assertNotNull(parser.parse("TYPE1 data")); // Will succeed or fail based on type
        }
    }
    
    @Test
    @DisplayName("Should support parser chaining logic")
    void testParserChaining() {
        WeatherParser<NoaaWeatherData> parser1 = new MockWeatherParser(true, "FORMAT1");
        WeatherParser<NoaaWeatherData> parser2 = new MockWeatherParser(true, "FORMAT2");
        
        String data1 = "FORMAT1 data";
        String data2 = "FORMAT2 data";
        
        // Simulate trying parsers in sequence
        ParseResult<NoaaWeatherData> result1 = parser1.canParse(data1) 
            ? parser1.parse(data1) 
            : parser2.parse(data1);
        
        ParseResult<NoaaWeatherData> result2 = parser1.canParse(data2) 
            ? parser1.parse(data2) 
            : parser2.parse(data2);
        
        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
    }
    
    @Test
    @DisplayName("Should maintain consistent source type")
    void testSourceTypeConsistency() {
        WeatherParser<NoaaWeatherData> parser = new MockWeatherParser(true, "TEST");
        
        String type1 = parser.getSourceType();
        String type2 = parser.getSourceType();
        
        assertEquals(type1, type2);
        assertEquals("MOCK_PARSER", type1);
    }
    
    @Test
    @DisplayName("Should work with case-sensitive prefixes")
    void testCaseSensitivity() {
        WeatherParser<NoaaWeatherData> parser = new MockWeatherParser(true, "TEST");
        
        assertTrue(parser.canParse("TEST data"));
        assertFalse(parser.canParse("test data")); // lowercase
        assertFalse(parser.canParse("Test data")); // mixed case
    }
    
    @Test
    @DisplayName("Should handle very long input data")
    void testLongInputData() {
        WeatherParser<NoaaWeatherData> parser = new MockWeatherParser(true, "LONG");
        String longData = "LONG" + "A".repeat(10000);
        
        ParseResult<NoaaWeatherData> result = parser.parse(longData);
        
        assertTrue(result.isSuccess());
        assertEquals(longData, result.getData().get().getRawData());
    }
    
    @Test
    @DisplayName("Should work with minimal valid input")
    void testMinimalInput() {
        WeatherParser<NoaaWeatherData> parser = new MockWeatherParser(true, "X");
        
        ParseResult<NoaaWeatherData> result = parser.parse("X");
        
        assertTrue(result.isSuccess());
        assertEquals("X", result.getData().get().getRawData());
    }
}
