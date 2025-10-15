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

/**
 * Main service interface for weather data operations.
 * 
 * This interface defines the contract for weather data retrieval operations,
 * following the service layer pattern from your architecture. Think of this as
 * the "menu" of weather services available - it promises what can be done,
 * but doesn't specify how it's implemented.
 * 
 * This interface serves as the boundary between your presentation layer 
 * (NoakWeatherMain) and the business logic layer, allowing for different
 * implementations (NOAA, OpenWeatherMap, etc.) without changing the client code.
 * 
 * @author bclasky1539
 *
 */
public interface WeatherService {
    /**
     * Retrieves METAR (Meteorological Aerodrome Report) data for the specified station.
     * 
     * METAR reports provide current weather observations from airports and are
     * typically updated hourly or when significant weather changes occur.
     * 
     * @param stationCode The ICAO airport code (e.g., "KJFK", "KCLT")
     * @return The raw METAR data as a string
     * @throws WeatherServiceException if the station code is invalid, 
     *         the station is not found, or there's an error retrieving the data
     */
    String getMetarData(String stationCode) throws WeatherServiceException;
    
    /**
     * Retrieves TAF (Terminal Aerodrome Forecast) data for the specified station.
     * 
     * TAF reports provide weather forecasts for airports, typically covering
     * 24-30 hours and including information about expected weather changes.
     * 
     * @param stationCode The ICAO airport code (e.g., "KJFK", "KCLT")
     * @return The raw TAF data as a string
     * @throws WeatherServiceException if the station code is invalid,
     *         the station is not found, or there's an error retrieving the data
     */
    String getTafData(String stationCode) throws WeatherServiceException;
    
    /**
     * Validates whether a station code is in the correct format.
     * 
     * This method checks if the provided station code follows the ICAO
     * airport code format (3-4 alphabetic characters).
     * 
     * @param stationCode The station code to validate
     * @return true if the format is valid, false otherwise
     */
    boolean isValidStationCode(String stationCode);
    
    /**
     * Gets the name of the weather service provider.
     * 
     * This is useful for logging, debugging, and when multiple weather
     * service implementations are available.
     * 
     * @return The name of the weather service provider (e.g., "NOAA", "OpenWeatherMap")
     */
    String getServiceProviderName();
}
