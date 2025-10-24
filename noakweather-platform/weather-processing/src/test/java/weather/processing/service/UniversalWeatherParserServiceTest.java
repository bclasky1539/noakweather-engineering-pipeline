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
package weather.processing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import weather.model.NoaaWeatherData;
import weather.model.WeatherData;
import weather.model.WeatherDataSource;
import weather.processing.parser.common.ParseResult;
import weather.processing.parser.common.WeatherParser;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for UniversalWeatherParserService.
 * Tests parser routing, registration, and auto-detection.
 * 
 * @author bclasky1539
 *
 */
class UniversalWeatherParserServiceTest {
    
    private UniversalWeatherParserService service;
    
    @BeforeEach
    void setUp() {
        service = new UniversalWeatherParserService();
    }
    
    @Test
    @DisplayName("Should register default parsers on construction")
    void testDefaultParsersRegistered() {
        assertTrue(service.hasParser("NOAA_METAR"));
        assertTrue(service.getParserCount() >= 1);
    }
    
    @Test
    @DisplayName("Should get registered parser types")
    void testGetRegisteredParsers() {
        Set<String> parsers = service.getRegisteredParsers();
        
        assertNotNull(parsers);
        assertTrue(parsers.contains("NOAA_METAR"));
    }
    
    @Test
    @DisplayName("Should return parser count")
    void testGetParserCount() {
        int count = service.getParserCount();
        
        assertTrue(count >= 1);
        assertEquals(service.getRegisteredParsers().size(), count);
    }
    
    @Test
    @DisplayName("Should check if parser exists")
    void testHasParser() {
        assertTrue(service.hasParser("NOAA_METAR"));
        assertFalse(service.hasParser("NONEXISTENT_PARSER"));
    }
    
    @Test
    @DisplayName("Should parse NOAA METAR data with known source")
    void testParseNoaaMetarWithKnownSource() {
        String metar = "METAR KJFK 251651Z 28016KT 10SM FEW250 22/12 A3015";
        
        ParseResult<? extends WeatherData> result = service.parse(metar, WeatherDataSource.NOAA);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getData().isPresent());
        
        WeatherData data = result.getData().get();
        assertTrue(data instanceof NoaaWeatherData);
        assertEquals("KJFK", ((NoaaWeatherData) data).getStationId());
    }
    
    @Test
    @DisplayName("Should auto-detect and parse METAR data")
    void testParseAuto() {
        String metar = "METAR KLAX 251651Z 28016KT 10SM";
        
        ParseResult<? extends WeatherData> result = service.parseAuto(metar);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getData().isPresent());
        assertEquals("KLAX", ((NoaaWeatherData) result.getData().get()).getStationId());
    }
    
    @Test
    @DisplayName("Should fail auto-parse with unrecognized data")
    void testParseAutoUnrecognizedData() {
        String unknown = "UNKNOWN DATA FORMAT";
        
        ParseResult<? extends WeatherData> result = service.parseAuto(unknown);
        
        assertTrue(result.isFailure());
        assertTrue(result.getErrorMessage().contains("No parser could handle"));
    }
    
    @Test
    @DisplayName("Should fail auto-parse with null data")
    void testParseAutoNull() {
        ParseResult<? extends WeatherData> result = service.parseAuto(null);
        
        assertTrue(result.isFailure());
        assertEquals("Raw data cannot be null or empty", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should fail auto-parse with empty data")
    void testParseAutoEmpty() {
        ParseResult<? extends WeatherData> result = service.parseAuto("");
        
        assertTrue(result.isFailure());
        assertEquals("Raw data cannot be null or empty", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should fail parse with unregistered source")
    void testParseUnregisteredSource() {
        String data = "SOME DATA";
        
        // Using a source that doesn't have a registered parser
        ParseResult<? extends WeatherData> result = service.parse(data, WeatherDataSource.UNKNOWN);
        
        assertTrue(result.isFailure());
        assertTrue(result.getErrorMessage().contains("No parser registered"));
    }
    
    @Test
    @DisplayName("Should register custom parser")
    void testRegisterCustomParser() {
        // Create a simple mock parser
        WeatherParser<NoaaWeatherData> customParser = new WeatherParser<>() {
            @Override
            public ParseResult<NoaaWeatherData> parse(String rawData) {
                return ParseResult.failure("Not implemented");
            }
            
            @Override
            public boolean canParse(String rawData) {
                return rawData != null && rawData.startsWith("CUSTOM");
            }
            
            @Override
            public String getSourceType() {
                return "CUSTOM_PARSER";
            }
        };
        
        service.registerParser("CUSTOM_PARSER", customParser);
        
        assertTrue(service.hasParser("CUSTOM_PARSER"));
        assertEquals(2, service.getParserCount()); // Default + custom
    }
    
    @Test
    @DisplayName("Should throw exception when registering parser with null type")
    void testRegisterParserWithNullType() {
        WeatherParser<NoaaWeatherData> parser = new WeatherParser<>() {
            @Override
            public ParseResult<NoaaWeatherData> parse(String rawData) {
                return null;
            }
        
            @Override
            public boolean canParse(String rawData) {
                return false;
            }
        
            @Override
            public String getSourceType() {
                return "TEST";
            }
        };
    
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerParser(null, parser);
        });
    
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Parser type cannot be null"));
    }
    
    @Test
    @DisplayName("Should throw exception when registering parser with empty type")
    void testRegisterParserWithEmptyType() {
        WeatherParser<NoaaWeatherData> parser = new WeatherParser<>() {
            @Override
            public ParseResult<NoaaWeatherData> parse(String rawData) {
                return null;
            }
        
            @Override
            public boolean canParse(String rawData) {
                return false;
            }
        
            @Override
            public String getSourceType() {
                return "TEST";
            }
        };
    
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerParser("", parser);
        });
    
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Parser type cannot be null"));
    }
    
    @Test
    @DisplayName("Should throw exception when registering null parser")
    void testRegisterNullParser() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerParser("TEST", null);
        });

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Parser cannot be null"));
    }
    
    @Test
    @DisplayName("Should unregister parser")
    void testUnregisterParser() {
        assertTrue(service.hasParser("NOAA_METAR"));
        
        boolean removed = service.unregisterParser("NOAA_METAR");
        
        assertTrue(removed);
        assertFalse(service.hasParser("NOAA_METAR"));
    }
    
    @Test
    @DisplayName("Should return false when unregistering nonexistent parser")
    void testUnregisterNonexistentParser() {
        boolean removed = service.unregisterParser("NONEXISTENT");
        
        assertFalse(removed);
    }
    
    @Test
    @DisplayName("Should handle NOAA TAF data when TAF is in raw data")
    void testParseNoaaWithTafKeyword() {
        String taf = "TAF KJFK 251651Z 2517/2618 28016KT";
        
        // Even though TAF parser isn't implemented, it should route to NOAA_TAF
        ParseResult<? extends WeatherData> result = service.parse(taf, WeatherDataSource.NOAA);
        
        assertTrue(result.isFailure());
        assertTrue(result.getErrorMessage().contains("NOAA_TAF") || 
                   result.getErrorMessage().contains("No parser registered"));
    }
    
    @Test
    @DisplayName("Should default to METAR for NOAA when format is ambiguous")
    void testParseNoaaDefaultsToMetar() {
        String ambiguous = "KJFK 251651Z 28016KT"; // Missing METAR/TAF keyword
        
        ParseResult<? extends WeatherData> result = service.parse(ambiguous, WeatherDataSource.NOAA);
        
        // Should attempt METAR parsing
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("Should use source name as fallback parser type")
    void testMapSourceToParserTypeFallback() {
        String data = "SOME DATA";
        
        // For UNKNOWN source, should use source name as parser type
        ParseResult<? extends WeatherData> result = service.parse(data, WeatherDataSource.UNKNOWN);
        
        assertTrue(result.isFailure());
        // Should fail because UNKNOWN parser doesn't exist
    }
    
    @Test
    @DisplayName("Should allow replacing existing parser")
    void testReplaceParser() {
        // Register custom parser with same key
        WeatherParser<NoaaWeatherData> replacementParser = new WeatherParser<>() {
            @Override
            public ParseResult<NoaaWeatherData> parse(String rawData) {
                return ParseResult.failure("Replaced");
            }
            
            @Override
            public boolean canParse(String rawData) {
                return true;
            }
            
            @Override
            public String getSourceType() {
                return "NOAA_METAR";
            }
        };
        
        service.registerParser("NOAA_METAR", replacementParser);
        
        String metar = "METAR KJFK 251651Z";
        ParseResult<? extends WeatherData> result = service.parse(metar, WeatherDataSource.NOAA);
        
        assertTrue(result.isFailure());
        assertEquals("Replaced", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should maintain parser count after register and unregister")
    void testParserCountConsistency() {
        int initialCount = service.getParserCount();
        
        // Create mock parser
        WeatherParser<NoaaWeatherData> testParser = new WeatherParser<>() {
            @Override
            public ParseResult<NoaaWeatherData> parse(String rawData) {
                return ParseResult.failure("Test");
            }
            
            @Override
            public boolean canParse(String rawData) {
                return false;
            }
            
            @Override
            public String getSourceType() {
                return "TEST";
            }
        };
        
        service.registerParser("TEST", testParser);
        assertEquals(initialCount + 1, service.getParserCount());
        
        service.unregisterParser("TEST");
        assertEquals(initialCount, service.getParserCount());
    }
    
    @Test
    @DisplayName("Should iterate through all parsers in auto-detect")
    void testAutoDetectTriesAllParsers() {
        // Register a custom parser that can parse specific format
        WeatherParser<NoaaWeatherData> customParser = new WeatherParser<>() {
            @Override
            public ParseResult<NoaaWeatherData> parse(String rawData) {
                return ParseResult.failure("Custom parse attempted");
            }
            
            @Override
            public boolean canParse(String rawData) {
                return rawData != null && rawData.startsWith("CUSTOM");
            }
            
            @Override
            public String getSourceType() {
                return "CUSTOM";
            }
        };
        
        service.registerParser("CUSTOM", customParser);
        
        // Data that only custom parser can recognize
        String customData = "CUSTOM DATA FORMAT";
        ParseResult<? extends WeatherData> result = service.parseAuto(customData);
        
        // Should have tried custom parser
        assertTrue(result.isFailure());
        assertEquals("Custom parse attempted", result.getErrorMessage());
    }
}
