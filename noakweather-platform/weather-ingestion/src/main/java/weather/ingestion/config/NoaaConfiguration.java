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
package weather.ingestion.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration for NOAA API endpoints.
 * Adapted from legacy WeatherConfigurationService pattern.
 * 
 * Reads configuration from noaa.properties file.
 * 
 * @author bclasky1539
 *
 */
public class NoaaConfiguration {
    
    private static final Logger logger = LogManager.getLogger(NoaaConfiguration.class);
    
    private static final String DEFAULT_METAR_BASE_URL = "https://aviationweather.gov/api/data/metar";
    private static final String DEFAULT_TAF_BASE_URL = "https://aviationweather.gov/api/data/taf";
    private static final String DEFAULT_FORMAT = "json";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    
    private final Properties properties;
    
    /**
     * Creates configuration by loading from properties file
     */
    public NoaaConfiguration() {
        this.properties = new Properties();
        loadProperties();
    }
    
    /**
     * Creates configuration with custom properties (for testing)
     * @param properties
     */
    public NoaaConfiguration(Properties properties) {
        this.properties = properties;
    }
    
    /**
     * Loads configuration from noaa.properties on classpath
     */
    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("noaa.properties")) {
            
            if (input != null) {
                properties.load(input);
                logger.info("Loaded NOAA configuration from noaa.properties");
            } else {
                logger.warn("noaa.properties not found, using default configuration");
            }
            
        } catch (IOException e) {
            logger.warn("Error loading noaa.properties, using defaults: {}", e.getMessage());
        }
    }
    
    /**
     * Gets the METAR base URL
     * @return 
     */
    public String getMetarBaseUrl() {
        return properties.getProperty("noaa.metar.base.url", DEFAULT_METAR_BASE_URL);
    }
    
    /**
     * Gets the TAF base URL
     * @return 
     */
    public String getTafBaseUrl() {
        return properties.getProperty("noaa.taf.base.url", DEFAULT_TAF_BASE_URL);
    }
    
    /**
     * Gets the response format (json or xml)
     * @return 
     */
    public String getFormat() {
        return properties.getProperty("noaa.format", DEFAULT_FORMAT);
    }
    
    /**
     * Gets the request timeout in seconds
     * @return 
     */
    public int getTimeoutSeconds() {
        String timeout = properties.getProperty("noaa.timeout.seconds", 
                String.valueOf(DEFAULT_TIMEOUT_SECONDS));
        try {
            return Integer.parseInt(timeout);
        } catch (NumberFormatException e) {
            logger.warn("Invalid timeout value: {}, using default", timeout);
            return DEFAULT_TIMEOUT_SECONDS;
        }
    }
    
    /**
     * Builds TAF URL for multiple stations
     * @param stationIds
     * @return 
     */
    public String buildTafUrl(String... stationIds) {
        String stationQuery = String.join(",", stationIds);
        return String.format("%s?ids=%s&format=%s&taf=true&hours=6",
                getTafBaseUrl(), stationQuery, getFormat());
    }
    
    /**
     * Builds METAR URL for multiple stations
     * @param stationIds
     * @return 
     */
    public String buildMetarUrl(String... stationIds) {
        String stationQuery = String.join(",", stationIds);
        return String.format("%s?ids=%s&format=%s&taf=false&hours=3",
                getMetarBaseUrl(), stationQuery, getFormat());
    }
    
    /**
     * Builds METAR URL for bounding box
     * @param minLat
     * @param minLon
     * @param maxLat
     * @param maxLon
     * @return 
     */
    public String buildMetarBboxUrl(double minLat, double minLon, double maxLat, double maxLon) {
        return String.format("%s?bbox=%f,%f,%f,%f&format=%s&hours=1",
                getMetarBaseUrl(), minLon, minLat, maxLon, maxLat, getFormat());
    }
}
