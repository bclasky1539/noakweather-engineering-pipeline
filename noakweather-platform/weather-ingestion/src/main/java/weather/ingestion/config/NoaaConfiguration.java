/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025-2026 bclasky1539
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
 * Configuration for NOAA Aviation Weather TG FTP service.
 * <p>
 * NOAA provides raw METAR and TAF data through their TG FTP service:
 * - METAR: <a href="https://tgftp.nws.noaa.gov/data/observations/metar/stations/">...</a>{STATION_ID}.TXT
 * - TAF: <a href="https://tgftp.nws.noaa.gov/data/forecasts/taf/stations/">...</a>{STATION_ID}.TXT
 * <p>
 * This is the official, stable NOAA data source used by aviation applications.
 * Each file contains the raw text report in standard METAR/TAF format.
 * <p>
 * Updated from legacy incorrect API endpoints to use proper TG FTP service.
 *
 * @author bclasky1539
 *
 */
public class NoaaConfiguration {

    private static final Logger logger = LogManager.getLogger(NoaaConfiguration.class);

    // Correct NOAA TG FTP endpoints
    private static final String DEFAULT_METAR_BASE_URL =
            "https://tgftp.nws.noaa.gov/data/observations/metar/stations";
    private static final String DEFAULT_TAF_BASE_URL =
            "https://tgftp.nws.noaa.gov/data/forecasts/taf/stations";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_RETRY_ATTEMPTS = 3;
    private static final int DEFAULT_RETRY_DELAY_MS = 1000;

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
     * @param properties custom properties
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
     * Gets the METAR base URL (station directory)
     * @return base URL for METAR station files
     */
    public String getMetarBaseUrl() {
        return properties.getProperty("noaa.metar.base.url", DEFAULT_METAR_BASE_URL);
    }

    /**
     * Gets the TAF base URL (station directory)
     * @return base URL for TAF station files
     */
    public String getTafBaseUrl() {
        return properties.getProperty("noaa.taf.base.url", DEFAULT_TAF_BASE_URL);
    }

    /**
     * Gets the request timeout in seconds
     * @return timeout in seconds
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
     * Gets the number of retry attempts for failed requests
     * @return number of retry attempts
     */
    public int getRetryAttempts() {
        String retries = properties.getProperty("noaa.retry.attempts",
                String.valueOf(DEFAULT_RETRY_ATTEMPTS));
        try {
            return Integer.parseInt(retries);
        } catch (NumberFormatException e) {
            logger.warn("Invalid retry attempts value: {}, using default", retries);
            return DEFAULT_RETRY_ATTEMPTS;
        }
    }

    /**
     * Gets the delay between retry attempts in milliseconds
     * @return retry delay in milliseconds
     */
    public int getRetryDelayMs() {
        String delay = properties.getProperty("noaa.retry.delay.ms",
                String.valueOf(DEFAULT_RETRY_DELAY_MS));
        try {
            return Integer.parseInt(delay);
        } catch (NumberFormatException e) {
            logger.warn("Invalid retry delay value: {}, using default", delay);
            return DEFAULT_RETRY_DELAY_MS;
        }
    }

    /**
     * Builds METAR URL for a specific station.
     *
     * @param stationId ICAO station identifier (e.g., "KJFK", "KCLT")
     * @return complete URL to fetch METAR data
     */
    public String buildMetarUrl(String stationId) {
        return String.format("%s/%s.TXT", getMetarBaseUrl(), stationId.toUpperCase());
    }

    /**
     * Builds TAF URL for a specific station.
     *
     * @param stationId ICAO station identifier (e.g., "KJFK", "KCLT")
     * @return complete URL to fetch TAF data
     */
    public String buildTafUrl(String stationId) {
        return String.format("%s/%s.TXT", getTafBaseUrl(), stationId.toUpperCase());
    }

    /**
     * Validates that the configuration URLs are using the correct TG FTP endpoints.
     * Logs warnings if legacy/incorrect endpoints are detected.
     *
     * @return true if configuration appears valid
     */
    public boolean validateConfiguration() {
        boolean valid = true;

        String metarUrl = getMetarBaseUrl();
        String tafUrl = getTafBaseUrl();

        // Check for incorrect aviationweather.gov API endpoints
        if (metarUrl.contains("aviationweather.gov/api")) {
            logger.error("INCORRECT METAR URL detected: {}. Should use tgftp.nws.noaa.gov", metarUrl);
            valid = false;
        }

        if (tafUrl.contains("aviationweather.gov/api")) {
            logger.error("INCORRECT TAF URL detected: {}. Should use tgftp.nws.noaa.gov", tafUrl);
            valid = false;
        }

        // Check for correct TG FTP endpoints
        if (!metarUrl.contains("tgftp.nws.noaa.gov")) {
            logger.warn("METAR URL does not use standard TG FTP endpoint: {}", metarUrl);
        }

        if (!tafUrl.contains("tgftp.nws.noaa.gov")) {
            logger.warn("TAF URL does not use standard TG FTP endpoint: {}", tafUrl);
        }

        if (valid) {
            logger.info("Configuration validation passed");
        }

        return valid;
    }

    /**
     * Gets a summary of the current configuration for logging/debugging
     *
     * @return configuration summary string
     */
    public String getConfigurationSummary() {
        return String.format(
                "NoaaConfiguration{metarBase='%s', tafBase='%s', timeout=%ds, retries=%d}",
                getMetarBaseUrl(),
                getTafBaseUrl(),
                getTimeoutSeconds(),
                getRetryAttempts()
        );
    }
}
