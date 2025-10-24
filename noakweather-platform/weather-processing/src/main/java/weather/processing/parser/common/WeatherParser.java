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

import weather.model.WeatherData;

/**
 * Universal interface for parsing weather data from any source.
 * 
 * This interface defines the contract that all weather parsers must implement,
 * enabling a polymorphic approach to parsing different weather data formats.
 * 
 * Design Pattern: Strategy Pattern
 * - Each weather source (NOAA, OpenWeatherMap, etc.) implements this interface
 * - Client code (UniversalWeatherParserService) can work with any parser
 * - New parsers can be added without changing existing code
 * 
 * @param <T> The specific type of WeatherData this parser produces
 * 
 * @author bclasky1539
 *
 */
public interface WeatherParser<T extends WeatherData> {
    
    /**
     * Parse raw weather data into structured WeatherData object.
     * 
     * @param rawData The raw text/data from the weather source
     * @return ParseResult containing either the parsed data or error information
     */
    ParseResult<T> parse(String rawData);
    
    /**
     * Validate if the raw data can be parsed by this parser.
     * This is a "pre-check" before attempting full parsing.
     * 
     * @param rawData The raw text to validate
     * @return true if this parser can handle the data
     */
    boolean canParse(String rawData);
    
    /**
     * Get the source type identifier for this parser.
     * 
     * @return The weather data source identifier (e.g., "NOAA_METAR", "OPENWEATHER_CURRENT")
     */
    String getSourceType();
}
