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
package weather.ingestion.service.source.noaa;

import weather.model.WeatherData;
import weather.model.NoaaWeatherData;
import weather.model.ProcessingLayer;
import weather.exception.WeatherServiceException;
import weather.exception.ErrorType;
import weather.ingestion.config.NoaaConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for NOAA Aviation Weather Center API.
 * Fetches METAR and TAF reports from NOAA's Aviation Weather REST API.
 * 
 * ADAPTED from legacy noakweather.service.WeatherService
 * Enhanced with real HTTP client implementation and configuration-based URLs.
 * 
 * API Documentation: https://aviationweather.gov/data/api/
 */
public class NoaaAviationWeatherClient {
    
    private static final Logger logger = LogManager.getLogger(NoaaAviationWeatherClient.class);
    
    // Error message constants
    private static final String MSG_REQUEST_INTERRUPTED = "Request interrupted";
    private static final String MSG_FAILED_METAR = "Failed to fetch METAR data";
    private static final String MSG_FAILED_TAF = "Failed to fetch TAF data";
    private static final String MSG_FAILED_BBOX = "Failed to fetch bounding box data";

    private final HttpClient httpClient;
    private final NoaaConfiguration config;
    
    /**
     * Creates a new NOAA Aviation Weather client with default configuration.
     */
    public NoaaAviationWeatherClient() {
        this(new NoaaConfiguration());
    }
    
    /**
     * Creates a new NOAA Aviation Weather client with custom configuration.
     * 
     * @param config the NOAA configuration
     */
    public NoaaAviationWeatherClient(NoaaConfiguration config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
        
        logger.info("NoaaAviationWeatherClient initialized with METAR URL: {}", 
                config.getMetarBaseUrl());
    }
    
    /**
     * Constructor for dependency injection with custom HttpClient (for testing).
     * 
     * @param httpClient custom HTTP client
     * @param config the NOAA configuration
     */
    public NoaaAviationWeatherClient(HttpClient httpClient, NoaaConfiguration config) {
        this.httpClient = httpClient;
        this.config = config;
    }
    
    /**
     * Validates station code format (copied from legacy).
     * Station codes are typically 3-4 characters, all letters (ICAO format).
     * Examples: KJFK, KCLT, EGLL, LFPG
     * 
     * @param stationCode the station code to validate
     * @return true if valid format, false otherwise
     */
    public boolean isValidStationCode(String stationCode) {
        if (stationCode == null || stationCode.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = stationCode.trim().toUpperCase();
        boolean isValid = trimmed.matches("[A-Z]{3,4}");
        
        logger.debug("Station code validation for '{}': {}", stationCode, isValid);
        return isValid;
    }
    
    /**
     * Fetches current METAR reports for the specified station IDs.
     * 
     * @param stationIds ICAO station identifiers (e.g., "KJFK", "KLGA")
     * @return list of WeatherData objects containing METAR reports
     * @throws WeatherServiceException if station code is invalid or request fails
     */
    public List<WeatherData> fetchMetarReports(String... stationIds) throws WeatherServiceException {
        if (stationIds == null || stationIds.length == 0) {
            throw new WeatherServiceException(
                    ErrorType.INVALID_STATION_CODE,
                    "At least one station ID must be provided"
            );
        }
        
        // Validate all station codes
        for (String stationId : stationIds) {
            if (!isValidStationCode(stationId)) {
                throw new WeatherServiceException(
                        ErrorType.INVALID_STATION_CODE,
                        "Station code must be 3-4 alphabetic characters",
                        stationId
                );
            }
        }
        
        String url = config.buildMetarUrl(stationIds);
        if (logger.isInfoEnabled()) {
            logger.info("Fetching METAR reports for stations: {}", String.join(",", stationIds));
        }
        
        try {
            return fetchWeatherData(url, "METAR", stationIds);
        } catch (IOException e) {
            throw new WeatherServiceException(
                    ErrorType.NETWORK_ERROR,
                    MSG_FAILED_METAR,
                    String.join(",", stationIds),
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherServiceException(
                    ErrorType.TIMEOUT,
                    MSG_REQUEST_INTERRUPTED,
                    String.join(",", stationIds),
                    e
            );
        }
    }
    
    /**
     * Fetches Terminal Aerodrome Forecast (TAF) reports for the specified station IDs.
     * 
     * @param stationIds ICAO station identifiers (e.g., "KJFK", "KLGA")
     * @return list of WeatherData objects containing TAF reports
     * @throws WeatherServiceException if station code is invalid or request fails
     */
    public List<WeatherData> fetchTafReports(String... stationIds) throws WeatherServiceException {
        if (stationIds == null || stationIds.length == 0) {
            throw new WeatherServiceException(
                    ErrorType.INVALID_STATION_CODE,
                    "At least one station ID must be provided"
            );
        }
        
        // Validate all station codes
        for (String stationId : stationIds) {
            if (!isValidStationCode(stationId)) {
                throw new WeatherServiceException(
                        ErrorType.INVALID_STATION_CODE,
                        "Station code must be 3-4 alphabetic characters",
                        stationId
                );
            }
        }
        
        String url = config.buildTafUrl(stationIds);
        if (logger.isInfoEnabled()) {
            logger.info("Fetching TAF reports for stations: {}", String.join(",", stationIds));
        }
        
        try {
            return fetchWeatherData(url, "TAF", stationIds);
        } catch (IOException e) {
            throw new WeatherServiceException(
                    ErrorType.NETWORK_ERROR,
                    MSG_FAILED_TAF,
                    String.join(",", stationIds),
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherServiceException(
                    ErrorType.TIMEOUT,
                    MSG_REQUEST_INTERRUPTED,
                    String.join(",", stationIds),
                    e
            );
        }
    }
    
    /**
     * Fetches the latest METAR report for a single station.
     * 
     * @param stationId ICAO station identifier
     * @return WeatherData object or null if no data available
     * @throws WeatherServiceException if station code is invalid or request fails
     */
    public WeatherData fetchLatestMetar(String stationId) throws WeatherServiceException {
        List<WeatherData> reports = fetchMetarReports(stationId);
        return reports.isEmpty() ? null : reports.get(0);
    }
    
    /**
     * Fetches all METAR reports within a geographic bounding box.
     * 
     * @param minLat minimum latitude
     * @param minLon minimum longitude
     * @param maxLat maximum latitude
     * @param maxLon maximum longitude
     * @return list of WeatherData objects
     * @throws WeatherServiceException if request fails
     */
    public List<WeatherData> fetchMetarByBoundingBox(double minLat, double minLon, 
                                                      double maxLat, double maxLon) 
            throws WeatherServiceException {
        String url = config.buildMetarBboxUrl(minLat, minLon, maxLat, maxLon);
        
        logger.info("Fetching METAR reports for bounding box: ({},{}) to ({},{})", 
                minLat, minLon, maxLat, maxLon);
        
        try {
            return fetchWeatherData(url, "METAR", new String[]{"BBOX_QUERY"});
        } catch (IOException e) {
            throw new WeatherServiceException(
                    ErrorType.NETWORK_ERROR,
                    MSG_FAILED_BBOX,
                    "bbox",
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherServiceException(
                    ErrorType.TIMEOUT,
                    MSG_REQUEST_INTERRUPTED,
                    "bbox",
                    e
            );
        }
    }
    
    /**
     * Core method that executes HTTP request and converts response to WeatherData objects.
     */
    private List<WeatherData> fetchWeatherData(String url, String reportType, String[] stationIds) 
            throws IOException, InterruptedException {
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "NoakWeather-Platform/2.0")
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("NOAA API request failed: HTTP " + response.statusCode());
        }
        
        String responseBody = response.body();
        logger.debug("Received NOAA API response: {} bytes", responseBody.length());
        
        return parseNoaaResponse(responseBody, reportType, stationIds);
    }
    
    /**
     * Parses NOAA JSON response and converts to WeatherData objects.
     */
    private List<WeatherData> parseNoaaResponse(String jsonResponse, String reportType, String[] stationIds) {
        List<WeatherData> weatherDataList = new ArrayList<>();
        
        // Check if response is empty
        if (jsonResponse == null || jsonResponse.trim().equals("[]") || jsonResponse.trim().isEmpty()) {
            logger.info("No data returned from NOAA API");
            return weatherDataList;
        }
        
        // For now, create basic WeatherData objects for each station
        for (String stationId : stationIds) {
            NoaaWeatherData data = new NoaaWeatherData(
                    stationId,
                    Instant.now(),
                    reportType
            );
            
            data.setRawData(jsonResponse);
            data.setProcessingLayer(ProcessingLayer.SPEED_LAYER);
            data.addMetadata("format", "JSON");
            
            weatherDataList.add(data);
        }
        
        logger.info("Parsed {} {} report(s) from NOAA API", weatherDataList.size(), reportType);
        
        return weatherDataList;
    }
    
    /**
     * Closes the HTTP client and releases resources.
     */
    public void close() {
        logger.info("NoaaAviationWeatherClient closed");
    }
}
