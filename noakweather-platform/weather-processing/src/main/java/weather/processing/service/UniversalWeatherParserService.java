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

import weather.model.WeatherData;
import weather.model.WeatherDataSource;
import weather.processing.parser.common.WeatherParser;
import weather.processing.parser.common.ParseResult;
import weather.processing.parser.noaa.NoaaMetarParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Universal weather parser service that routes to the appropriate parser.
 * 
 * Design Pattern: Strategy Pattern + Factory Pattern
 * - Strategy: Different parsers for different sources
 * - Factory: Creates/retrieves the right parser based on input
 * 
 * This service acts as a dispatcher/router:
 * 1. Receives raw weather data
 * 2. Determines which parser to use (based on source or auto-detection)
 * 3. Delegates to the appropriate parser
 * 4. Returns standardized ParseResult
 * 
 * Benefits:
 * - Easy to add new sources (just register a new parser)
 * - Centralized parsing logic
 * - Type-safe with generics
 * - Consistent interface for all weather sources
 * 
 * @author bclasky1539
 *
 */
public class UniversalWeatherParserService {
    
    private final Map<String, WeatherParser<? extends WeatherData>> parsers;
    
    public UniversalWeatherParserService() {
        this.parsers = new HashMap<>();
        registerDefaultParsers();
    }
    
    /**
     * Register the default set of parsers.
     * Called automatically during construction.
     */
    private void registerDefaultParsers() {
        // Register NOAA METAR parser
        NoaaMetarParser metarParser = new NoaaMetarParser();
        parsers.put(metarParser.getSourceType(), metarParser);
        
        // Additional parsers can be registered here as they are implemented
    }
    
    /**
     * Parse weather data from a specific known source.
     * Use this when you know the source of your data.
     * 
     * @param rawData The raw weather data string
     * @param source The known source of the data (NOAA, OpenWeatherMap, etc.)
     * @return ParseResult containing the parsed WeatherData or error information
     */
    public ParseResult<? extends WeatherData> parse(String rawData, WeatherDataSource source) {
        // Map the source to a specific parser type
        String parserType = mapSourceToParserType(rawData, source);
        
        WeatherParser<? extends WeatherData> parser = parsers.get(parserType);
        
        if (parser == null) {
            return ParseResult.failure("No parser registered for source: " + source + " (parser type: " + parserType + ")");
        }
        
        return parser.parse(rawData);
    }
    
    /**
     * Auto-detect and parse weather data.
     * Use this when you don't know the source/format.
     * Tries each registered parser until one succeeds.
     * 
     * @param rawData The raw weather data string
     * @return ParseResult containing the parsed WeatherData or error information
     */
    public ParseResult<? extends WeatherData> parseAuto(String rawData) {
        if (rawData == null || rawData.trim().isEmpty()) {
            return ParseResult.failure("Raw data cannot be null or empty");
        }
        
        // Try each parser in order
        for (WeatherParser<? extends WeatherData> parser : parsers.values()) {
            if (parser.canParse(rawData)) {
                return parser.parse(rawData);
            }
        }
        
        return ParseResult.failure(
            "No parser could handle the provided data. Registered parsers: " + 
            String.join(", ", parsers.keySet())
        );
    }
    
    /**
     * Register a custom parser.
     * Allows extending the service with new parsers at runtime.
     * 
     * @param parserType Unique identifier for this parser (e.g., "CUSTOM_WEATHER")
     * @param parser The parser implementation
     */
    public void registerParser(String parserType, WeatherParser<? extends WeatherData> parser) {
        if (parserType == null || parserType.trim().isEmpty()) {
            throw new IllegalArgumentException("Parser type cannot be null or empty");
        }
        if (parser == null) {
            throw new IllegalArgumentException("Parser cannot be null");
        }
        parsers.put(parserType, parser);
    }
    
    /**
     * Unregister a parser.
     * 
     * @param parserType The parser type to remove
     * @return true if the parser was removed, false if it didn't exist
     */
    public boolean unregisterParser(String parserType) {
        return parsers.remove(parserType) != null;
    }
    
    /**
     * Map a WeatherDataSource to a specific parser type.
     * Handles sources that may have multiple report types (like NOAA having METAR and TAF).
     * 
     * @param rawData The raw data (used to determine specific report type)
     * @param source The general source
     * @return The specific parser type identifier
     */
    private String mapSourceToParserType(String rawData, WeatherDataSource source) {
        if (source == WeatherDataSource.NOAA) {
            // NOAA can have multiple report types - determine which one
            if (rawData.trim().startsWith("METAR")) {
                return "NOAA_METAR";
            } else if (rawData.trim().startsWith("TAF")) {
                return "NOAA_TAF";
            }
            // Default to METAR for NOAA
            return "NOAA_METAR";
        }
        
        // For other sources, use the source name as parser type
        // Additional sources will be handled here as they are added
        return source.name();
    }
    
    /**
     * Get all registered parser types.
     * Useful for debugging or displaying available parsers.
     * 
     * @return Set of parser type identifiers
     */
    public Set<String> getRegisteredParsers() {
        return parsers.keySet();
    }
    
    /**
     * Check if a specific parser type is registered.
     * 
     * @param parserType The parser type to check
     * @return true if registered, false otherwise
     */
    public boolean hasParser(String parserType) {
        return parsers.containsKey(parserType);
    }
    
    /**
     * Get the count of registered parsers.
     * 
     * @return Number of registered parsers
     */
    public int getParserCount() {
        return parsers.size();
    }
}
