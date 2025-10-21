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
package weather.service;

import weather.model.WeatherData;
import weather.model.WeatherDataSource;

/**
 * Universal interface for parsing weather data from any source.
 * 
 * Design Pattern: Strategy Pattern
 * 
 * Each weather source (NOAA, OpenWeatherMap, etc.) will have its own
 * implementation of this interface, allowing the system to parse data
 * from multiple sources using a common API.
 * 
 * Analogy: Like having different "readers" for different file formats
 * (PDF reader, Word reader, etc.) that all implement a common "Document Reader"
 * interface.
 * 
 * @param <T> the specific type of WeatherData this parser produces
 * 
 * @author bclasky1539
 *
 */
public interface WeatherParser<T extends WeatherData> {
    
    /**
     * Parse raw weather data string into a structured WeatherData object.
     * 
     * @param rawData the raw data string (XML, JSON, text, etc.)
     * @return parsed WeatherData object
     * @throws WeatherParseException if parsing fails
     */
    T parse(String rawData) throws WeatherParseException;
    
    /**
     * Check if this parser can handle the given raw data.
     * Useful for auto-detecting the correct parser.
     * 
     * @param rawData the raw data to check
     * @return true if this parser can handle the data
     */
    boolean canParse(String rawData);
    
    /**
     * Get the weather data source this parser handles.
     * 
     * @return the WeatherDataSource enum value
     */
    WeatherDataSource getSource();
    
    /**
     * Get the format this parser expects (e.g., "XML", "JSON", "TEXT").
     * 
     * @return the expected format string
     */
    String getExpectedFormat();
}
