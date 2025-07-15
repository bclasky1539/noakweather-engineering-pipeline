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

import noakweather.config.WeatherConfigurationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implementation of the WeatherService interface.
 * 
 * This class serves as the main coordinator for weather data operations,
 * acting like a "travel agent" that knows how to get weather information
 * from various sources and present it in a consistent format.
 * 
 * Currently, this is a foundational implementation that will be extended
 * to integrate with specific weather providers (NOAA, OpenWeatherMap, etc.)
 * as shown in your architecture diagrams.
 *
 * @author bclasky1539
 */
public class WeatherServiceImpl implements WeatherService {
    
    private static final Logger LOGGER = LogManager.getLogger(WeatherServiceImpl.class);
    
    private final WeatherConfigurationService configService;
    
    /**
     * Creates a new WeatherServiceImpl with the specified configuration service.
     * 
     * @param configService The configuration service for accessing weather-related settings
     */
    public WeatherServiceImpl(WeatherConfigurationService configService) {
        this.configService = configService;
        LOGGER.debug("WeatherServiceImpl initialized with configuration service");
    }
    
    @Override
    public String getMetarData(String stationCode) throws WeatherServiceException {
        LOGGER.debug("Retrieving METAR data for station: {}", stationCode);
        
        // Validate station code format
        if (!isValidStationCode(stationCode)) {
            throw new WeatherServiceException(
                WeatherServiceException.ErrorType.INVALID_STATION_CODE,
                "Station code must be 3-4 alphabetic characters",
                stationCode
            );
        }
        
        String normalizedStation = stationCode.toUpperCase().trim();
        
        try {
            // Build the METAR URL using configuration
            String metarUrl = buildMetarUrl(normalizedStation);
            LOGGER.info("METAR URL for {}: {}", normalizedStation, metarUrl);
            
            // Current implementation returns placeholder data
            // Real HTTP client integration will be implemented in future iterations
            String placeholderData = String.format(
                "METAR %s 141753Z 24012KT 10SM FEW250 25/18 A3012 RMK AO2 SLP205 T02500183=", 
                normalizedStation
            );
            
            LOGGER.info("METAR data retrieved successfully for station: {}", normalizedStation);
            LOGGER.debug("METAR data: {}", placeholderData);
            
            return placeholderData;
            
        } catch (Exception e) {
            LOGGER.error("Error retrieving METAR data for station: {}", normalizedStation, e);
            throw new WeatherServiceException(
                WeatherServiceException.ErrorType.SERVICE_UNAVAILABLE,
                "Failed to retrieve METAR data",
                normalizedStation,
                e
            );
        }
    }
    
    @Override
    public String getTafData(String stationCode) throws WeatherServiceException {
        LOGGER.debug("Retrieving TAF data for station: {}", stationCode);
        
        // Validate station code format
        if (!isValidStationCode(stationCode)) {
            throw new WeatherServiceException(
                WeatherServiceException.ErrorType.INVALID_STATION_CODE,
                "Station code must be 3-4 alphabetic characters",
                stationCode
            );
        }
        
        String normalizedStation = stationCode.toUpperCase().trim();
        
        try {
            // Build the TAF URL using configuration
            String tafUrl = buildTafUrl(normalizedStation);
            LOGGER.info("TAF URL for {}: {}", normalizedStation, tafUrl);
            
            // Current implementation returns placeholder data
            // Real HTTP client integration will be implemented in future iterations
            String placeholderData = String.format(
                "TAF %s 141152Z 141212 24012KT P6SM FEW250 " +
                "FM1600 25015G25KT P6SM SCT250 " +
                "FM0000 23008KT P6SM FEW250=",
                normalizedStation
            );
            
            LOGGER.info("TAF data retrieved successfully for station: {}", normalizedStation);
            LOGGER.debug("TAF data: {}", placeholderData);
            
            return placeholderData;
            
        } catch (Exception e) {
            LOGGER.error("Error retrieving TAF data for station: {}", normalizedStation, e);
            throw new WeatherServiceException(
                WeatherServiceException.ErrorType.SERVICE_UNAVAILABLE,
                "Failed to retrieve TAF data",
                normalizedStation,
                e
            );
        }
    }
    
    @Override
    public boolean isValidStationCode(String stationCode) {
        if (stationCode == null || stationCode.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = stationCode.trim().toUpperCase();
        
        // Station codes are typically 3-4 characters, all letters
        // Examples: KJFK, KCLT, EGLL, LFPG
        boolean isValid = trimmed.matches("[A-Z]{3,4}");
        
        LOGGER.debug("Station code validation for '{}': {}", stationCode, isValid);
        return isValid;
    }
    
    @Override
    public String getServiceProviderName() {
        return "NoakWeather Core Service";
    }
    
    /**
     * Builds the METAR URL for the specified station using configuration.
     * 
     * @param stationCode The normalized station code
     * @return The complete METAR URL
     */
    private String buildMetarUrl(String stationCode) {
        try {
            String baseUrl = configService.getRawString("MISC_METAR_URL");
            String extension = configService.getRawString("MISC_METAR_EXT");
            
            String url = baseUrl + stationCode + extension;
            LOGGER.debug("Built METAR URL: {}", url);
            return url;
            
        } catch (Exception e) {
            LOGGER.warn("Error building METAR URL, using fallback", e);
            // Fallback URL structure
            return "https://aviationweather.gov/api/data/metar?ids=" + stationCode + "&format=raw";
        }
    }
    
    /**
     * Builds the TAF URL for the specified station using configuration.
     * 
     * @param stationCode The normalized station code
     * @return The complete TAF URL
     */
    private String buildTafUrl(String stationCode) {
        try {
            String baseUrl = configService.getRawString("MISC_TAF_URL");
            String extension = configService.getRawString("MISC_TAF_EXT");
            
            String url = baseUrl + stationCode + extension;
            LOGGER.debug("Built TAF URL: {}", url);
            return url;
            
        } catch (Exception e) {
            LOGGER.warn("Error building TAF URL, using fallback", e);
            // Fallback URL structure
            return "https://aviationweather.gov/api/data/taf?ids=" + stationCode + "&format=raw";
        }
    }
}
