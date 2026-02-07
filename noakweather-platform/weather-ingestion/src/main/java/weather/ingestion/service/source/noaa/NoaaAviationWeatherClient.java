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
package weather.ingestion.service.source.noaa;

import weather.model.*;
import weather.exception.WeatherServiceException;
import weather.exception.ErrorType;
import weather.ingestion.config.NoaaConfiguration;
import weather.processing.parser.noaa.NoaaMetarParser;
import weather.processing.parser.noaa.NoaaTafParser;
import weather.processing.parser.common.ParseResult;

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
import java.util.Optional;

/**
 * Client for NOAA Aviation Weather TG FTP service.
 * Fetches raw METAR and TAF reports from NOAA's text data service.
 * <p>
 * UPDATED v2.0: Now integrates comprehensive METAR/TAF parsers from weather-processing module!
 * <p>
 * NOAA TG FTP Service:
 * - METAR: <a href="https://tgftp.nws.noaa.gov/data/observations/metar/stations/">...</a>{STATION_ID}.TXT
 * - TAF: <a href="https://tgftp.nws.noaa.gov/data/forecasts/taf/stations/">...</a>{STATION_ID}.TXT
 * <p>
 * Each file contains the raw aviation weather report in standard format.
 * This is the official, stable data source used by aviation weather applications.
 * <p>
 * CORRECTED from legacy incorrect API implementation.
 * <p>
 * Example METAR response:
 * 2025/01/11 14:56
 * KCLT 111456Z 27008KT 10SM FEW250 06/M07 A3034 RMK AO2 SLP278 T00561072
 * <p>
 * Example TAF response:
 * 2025/01/11 11:25
 * TAF KBUF 111125Z 1112/1212 31012G20KT P6SM BKN030
 *     FM111900 30015G25KT P6SM BKN020
 * <p>
 * NEW: Parsed Data Structure
 * - Returns NoaaMetarData with fully populated conditions (wind, temp, pressure, etc.)
 * - Returns NoaaTafData with forecast periods parsed
 * - Preserves raw text in rawData field for audit/reprocessing
 * - Falls back to unparsed data if parsing fails
 *
 * @author bclasky1539
 * @version 2.0 - Integrated comprehensive parsers
 *
 */
public class NoaaAviationWeatherClient {

    private static final Logger logger = LogManager.getLogger(NoaaAviationWeatherClient.class);

    // Error message constants
    private static final String MSG_REQUEST_INTERRUPTED = "Request interrupted";
    private static final String MSG_FAILED_METAR = "Failed to fetch METAR data";
    private static final String MSG_FAILED_TAF = "Failed to fetch TAF data";

    private static final String FULL_RESPONSE = "full_response";
    private static final String FETCH_TIMESTAMP = "fetch_timestamp";
    private static final String PARSED = "parsed";

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
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // Validate configuration on startup
        if (!config.validateConfiguration()) {
            logger.warn("Configuration validation failed - check noaa.properties");
        }

        logger.info("NoaaAviationWeatherClient initialized with parsing enabled: {}",
                config.getConfigurationSummary());
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
     * Validates station code format.
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
     * Fetches current METAR report for a single station.
     * NEW: Returns fully parsed NoaaMetarData with all weather conditions populated!
     *
     * @param stationId ICAO station identifier (e.g., "KJFK", "KCLT")
     * @return WeatherData object containing fully parsed METAR report, or null if no data available
     * @throws WeatherServiceException if station code is invalid or request fails
     */
    public WeatherData fetchMetarReport(String stationId) throws WeatherServiceException {
        if (!isValidStationCode(stationId)) {
            throw new WeatherServiceException(
                    ErrorType.INVALID_STATION_CODE,
                    "Station code must be 3-4 alphabetic characters",
                    stationId
            );
        }

        String url = config.buildMetarUrl(stationId);
        logger.info("Fetching METAR report for station: {} from {}", stationId, url);

        try {
            String rawText = fetchRawData(url, stationId);

            if (rawText == null || rawText.trim().isEmpty()) {
                logger.warn("No METAR data available for station: {}", stationId);
                return null;
            }

            return parseMetarResponse(rawText, stationId);

        } catch (IOException e) {
            throw new WeatherServiceException(
                    ErrorType.NETWORK_ERROR,
                    MSG_FAILED_METAR,
                    stationId,
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherServiceException(
                    ErrorType.TIMEOUT,
                    MSG_REQUEST_INTERRUPTED,
                    stationId,
                    e
            );
        }
    }

    /**
     * Fetches METAR reports for multiple stations.
     * More efficient than calling fetchMetarReport repeatedly.
     *
     * @param stationIds ICAO station identifiers
     * @return list of WeatherData objects (excludes stations with no data)
     * @throws WeatherServiceException if any station code is invalid
     */
    public List<WeatherData> fetchMetarReports(String... stationIds) throws WeatherServiceException {
        if (stationIds == null || stationIds.length == 0) {
            throw new WeatherServiceException(
                    ErrorType.INVALID_STATION_CODE,
                    "At least one station ID must be provided"
            );
        }

        // Validate all station codes first
        for (String stationId : stationIds) {
            if (!isValidStationCode(stationId)) {
                throw new WeatherServiceException(
                        ErrorType.INVALID_STATION_CODE,
                        "Invalid station code: " + stationId
                );
            }
        }

        List<WeatherData> results = new ArrayList<>();

        for (String stationId : stationIds) {
            try {
                WeatherData data = fetchMetarReport(stationId);
                if (data != null) {
                    results.add(data);
                }
            } catch (WeatherServiceException e) {
                // Log error but continue with other stations
                logger.error("Failed to fetch METAR for {}: {}", stationId, e.getMessage());
            }
        }

        logger.info("Fetched {} METAR reports out of {} stations",
                results.size(), stationIds.length);

        return results;
    }

    /**
     * Fetches current TAF (Terminal Aerodrome Forecast) for a single station.
     * NEW: Returns fully parsed NoaaTafData with forecast periods!
     *
     * @param stationId ICAO station identifier (e.g., "KJFK", "KCLT")
     * @return WeatherData object containing fully parsed TAF forecast, or null if no data available
     * @throws WeatherServiceException if station code is invalid or request fails
     */
    public WeatherData fetchTafReport(String stationId) throws WeatherServiceException {
        if (!isValidStationCode(stationId)) {
            throw new WeatherServiceException(
                    ErrorType.INVALID_STATION_CODE,
                    "Station code must be 3-4 alphabetic characters",
                    stationId
            );
        }

        String url = config.buildTafUrl(stationId);
        logger.info("Fetching TAF report for station: {} from {}", stationId, url);

        try {
            String rawText = fetchRawData(url, stationId);

            if (rawText == null || rawText.trim().isEmpty()) {
                logger.warn("No TAF data available for station: {}", stationId);
                return null;
            }

            return parseTafResponse(rawText, stationId);

        } catch (IOException e) {
            throw new WeatherServiceException(
                    ErrorType.NETWORK_ERROR,
                    MSG_FAILED_TAF,
                    stationId,
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherServiceException(
                    ErrorType.TIMEOUT,
                    MSG_REQUEST_INTERRUPTED,
                    stationId,
                    e
            );
        }
    }

    /**
     * Fetches TAF reports for multiple stations.
     *
     * @param stationIds ICAO station identifiers
     * @return list of WeatherData objects (excludes stations with no data)
     * @throws WeatherServiceException if any station code is invalid
     */
    public List<WeatherData> fetchTafReports(String... stationIds) throws WeatherServiceException {
        if (stationIds == null || stationIds.length == 0) {
            throw new WeatherServiceException(
                    ErrorType.INVALID_STATION_CODE,
                    "At least one station ID must be provided"
            );
        }

        // Validate all station codes first
        for (String stationId : stationIds) {
            if (!isValidStationCode(stationId)) {
                throw new WeatherServiceException(
                        ErrorType.INVALID_STATION_CODE,
                        "Invalid station code: " + stationId
                );
            }
        }

        List<WeatherData> results = new ArrayList<>();

        for (String stationId : stationIds) {
            try {
                WeatherData data = fetchTafReport(stationId);
                if (data != null) {
                    results.add(data);
                }
            } catch (WeatherServiceException e) {
                // Log error but continue with other stations
                logger.error("Failed to fetch TAF for {}: {}", stationId, e.getMessage());
            }
        }

        logger.info("Fetched {} TAF reports out of {} stations",
                results.size(), stationIds.length);

        return results;
    }

    /**
     * Alias for fetchMetarReport for backward compatibility.
     *
     * @param stationId ICAO station identifier
     * @return WeatherData object or null if no data available
     * @throws WeatherServiceException if request fails
     */
    public WeatherData fetchLatestMetar(String stationId) throws WeatherServiceException {
        return fetchMetarReport(stationId);
    }

    /**
     * Core method that executes HTTP request with retry logic.
     * Returns the raw text response from NOAA.
     *
     * @param url the URL to fetch
     * @param stationId the station identifier (for logging)
     * @return raw text response
     * @throws IOException if all retry attempts fail
     * @throws InterruptedException if request is interrupted
     */
    private String fetchRawData(String url, String stationId)
            throws IOException, InterruptedException {

        int attempts = 0;
        int maxAttempts = config.getRetryAttempts();
        IOException lastException = null;

        while (attempts < maxAttempts) {
            attempts++;

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "text/plain")
                        .header("User-Agent", "NoakWeather-Platform/2.0")
                        .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                // HTTP 200 = success
                if (response.statusCode() == 200) {
                    String body = response.body();
                    logger.debug("Fetched {} bytes for station {}",
                            body.length(), stationId);
                    return body;
                }

                // HTTP 404 = station exists but no current data available
                if (response.statusCode() == 404) {
                    logger.warn("No data available for station {} (HTTP 404)", stationId);
                    return null;
                }

                // Other error codes
                throw new IOException(String.format(
                        "NOAA request failed: HTTP %d for station %s",
                        response.statusCode(), stationId
                ));

            } catch (IOException e) {
                lastException = e;
                logger.warn("Attempt {}/{} failed for station {}: {}",
                        attempts, maxAttempts, stationId, e.getMessage());

                // If not last attempt, wait before retry
                if (attempts < maxAttempts) {
                    Thread.sleep(config.getRetryDelayMs());
                }
            }
        }

        // All attempts failed
        throw new IOException(
                String.format("Failed to fetch data after %d attempts", maxAttempts),
                lastException
        );
    }

    /**
     * Parses METAR raw text response into fully structured WeatherData object.
     * NEW: Uses comprehensive NoaaMetarParser from weather-processing module!
     * <p>
     * Example input:
     * 2025/01/11 14:56
     * KCLT 111456Z 27008KT 10SM FEW250 06/M07 A3034 RMK AO2 SLP278 T00561072
     * <p>
     * Parsing includes:
     * - Wind (direction, speed, gusts, variability)
     * - Visibility (distance, unit, CAVOK)
     * - Temperature and dewpoint
     * - Pressure (altimeter, sea level)
     * - Sky conditions (cloud layers)
     * - Present weather (rain, snow, fog, etc.)
     * - Remarks (automated station, peak wind, precipitation, etc.)
     *
     * @param rawText the raw response from NOAA
     * @param stationId the station identifier
     * @return WeatherData object (NoaaMetarData with fully populated conditions)
     */
    private WeatherData parseMetarResponse(String rawText, String stationId) {
        // Extract the METAR line (typically second line after timestamp)
        String metarLine = extractMetarLine(rawText, stationId);

        logger.debug("Parsing METAR for {}: {}", stationId, metarLine);

        // Create new parser instance for thread safety
        NoaaMetarParser metarParser = new NoaaMetarParser();
        ParseResult<NoaaWeatherData> parseResult = metarParser.parse(metarLine);

        if (parseResult.isSuccess()) {
            Optional<NoaaWeatherData> optionalData = parseResult.getData();
            if (optionalData.isPresent()) {
                NoaaWeatherData parsedData = optionalData.get();

                //  Set the fields that tests expect!
                parsedData.setRawData(metarLine);
                parsedData.setSource(WeatherDataSource.NOAA);
                parsedData.setProcessingLayer(ProcessingLayer.SPEED_LAYER);

                // Add metadata about fetch
                parsedData.addMetadata( FULL_RESPONSE, rawText);
                parsedData.addMetadata(FETCH_TIMESTAMP, Instant.now().toString());
                parsedData.addMetadata(PARSED, "true");
                parsedData.addMetadata("parser_version", "2.0");

                logger.info("Successfully parsed METAR for {} with all conditions", stationId);
                return parsedData;
            }
        }

        // Parsing failed - fallback to unparsed data
        logger.warn("Failed to parse METAR for {}: {}. Falling back to unparsed data.",
                stationId, parseResult.getErrorMessage());

        // Create NoaaMetarData (specific subclass) as fallback
        NoaaMetarData fallbackData = new NoaaMetarData();
        fallbackData.setStationId(stationId.toUpperCase());
        fallbackData.setObservationTime(Instant.now());

        fallbackData.setRawData(metarLine);
        fallbackData.setSource(WeatherDataSource.NOAA);
        fallbackData.setProcessingLayer(ProcessingLayer.SPEED_LAYER);
        fallbackData.addMetadata("format", "TEXT");
        fallbackData.addMetadata( FULL_RESPONSE, rawText);
        fallbackData.addMetadata(FETCH_TIMESTAMP, Instant.now().toString());
        fallbackData.addMetadata(PARSED, "false");
        fallbackData.addMetadata("parse_error", parseResult.getErrorMessage());

        return fallbackData;
    }

    /**
     * Parses TAF raw text response into fully structured WeatherData object.
     * NEW: Uses comprehensive NoaaTafParser from weather-processing module!
     * <p>
     * Example input:
     * 2025/01/11 11:25
     * TAF KBUF 111125Z 1112/1212 31012G20KT P6SM BKN030
     *     FM111900 30015G25KT P6SM BKN020
     * <p>
     * Parsing includes:
     * - Base forecast conditions
     * - Forecast periods (FM, TEMPO, PROB, BECMG)
     * - Valid time periods
     * - Change indicators
     *
     * @param rawText the raw response from NOAA
     * @param stationId the station identifier
     * @return WeatherData object (NoaaTafData with forecast periods parsed)
     */
    private WeatherData parseTafResponse(String rawText, String stationId) {
        // Extract the complete TAF text (can be multi-line)
        String tafText = extractTafText(rawText);

        logger.debug("Parsing TAF for {}: {}", stationId, tafText);

        // Create new parser instance for thread safety
        NoaaTafParser tafParser = new NoaaTafParser();
        ParseResult<NoaaWeatherData> parseResult = tafParser.parse(tafText);

        if (parseResult.isSuccess()) {
            Optional<NoaaWeatherData> optionalData = parseResult.getData();
            if (optionalData.isPresent()) {
                NoaaWeatherData parsedData = optionalData.get();

                //  Set the fields that tests expect!
                parsedData.setRawData(tafText);
                parsedData.setSource(WeatherDataSource.NOAA);
                parsedData.setProcessingLayer(ProcessingLayer.SPEED_LAYER);

                // Add metadata about fetch
                parsedData.addMetadata( FULL_RESPONSE, rawText);
                parsedData.addMetadata(FETCH_TIMESTAMP, Instant.now().toString());
                parsedData.addMetadata(PARSED, "true");
                parsedData.addMetadata("parser_version", "2.0");

                logger.info("Successfully parsed TAF for {} with forecast periods", stationId);
                return parsedData;
            }
        }

        // Parsing failed - fallback to unparsed data
        logger.warn("Failed to parse TAF for {}: {}. Falling back to unparsed data.",
                stationId, parseResult.getErrorMessage());

        // Create NoaaTafData (specific subclass) as fallback
        NoaaTafData fallbackData = new NoaaTafData();
        fallbackData.setStationId(stationId.toUpperCase());
        fallbackData.setObservationTime(Instant.now());

        fallbackData.setRawData(tafText);
        fallbackData.setSource(WeatherDataSource.NOAA);
        fallbackData.setProcessingLayer(ProcessingLayer.SPEED_LAYER);
        fallbackData.addMetadata("format", "TEXT");
        fallbackData.addMetadata( FULL_RESPONSE, rawText);
        fallbackData.addMetadata(FETCH_TIMESTAMP, Instant.now().toString());
        fallbackData.addMetadata(PARSED, "false");
        fallbackData.addMetadata("parse_error", parseResult.getErrorMessage());

        return fallbackData;
    }

    /**
     * Extracts the METAR line from the raw NOAA response.
     * The METAR line typically starts with the station ID.
     *
     * @param rawText the raw response
     * @param stationId the station identifier
     * @return the extracted METAR line
     */
    private String extractMetarLine(String rawText, String stationId) {
        String[] lines = rawText.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            // METAR line starts with station ID
            if (trimmed.startsWith(stationId.toUpperCase())) {
                return trimmed;
            }
        }

        // Fallback: return everything as one line
        logger.warn("Could not extract METAR line for station {}, using full response", stationId);
        return rawText.replace("\n", " ").trim();
    }

    /**
     * Extracts the complete TAF text from the raw NOAA response.
     * TAF can be multi-line, so we need to capture all lines.
     *
     * @param rawText the raw response
     * @return the extracted TAF text
     */
    private String extractTafText(String rawText) {
        String[] lines = rawText.split("\n");
        StringBuilder tafBuilder = new StringBuilder();
        boolean inTaf = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // TAF starts with "TAF" or "TAF AMD"
            if (trimmed.startsWith("TAF")) {
                inTaf = true;
            }

            if (inTaf && !trimmed.isEmpty()) {
                tafBuilder.append(trimmed).append(" ");
            }
        }

        String tafText = tafBuilder.toString().trim();

        if (tafText.isEmpty()) {
            logger.warn("Could not extract TAF text, using full response");
            tafText = rawText.replace("\n", " ").trim();
        }

        return tafText;
    }

    /**
     * Closes the HTTP client and releases resources.
     */
    public void close() {
        logger.info("NoaaAviationWeatherClient closed");
    }
}
