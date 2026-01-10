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
package weather.processing.parser.noaa;

import weather.model.NoaaMetarData;
import weather.model.NoaaWeatherData;
import weather.model.components.*;
import weather.model.components.remark.*;
import weather.model.enums.AutomatedStationType;
import weather.model.enums.PressureUnit;
import weather.processing.parser.common.ParseResult;
import weather.utils.IndexedLinkedHashMap;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static weather.processing.parser.noaa.RegExprConst.*;

/**
 * Parser for NOAA METAR (Meteorological Aerodrome Report) data.
 *
 * Extends NoaaAviationWeatherParser to inherit shared aviation weather parsing logic
 * for wind, visibility, present weather, sky conditions, and RVR.
 *
 * METAR Format Example:
 * "2025/11/14 22:52
 *  METAR KJFK 142252Z 19005KT 10SM FEW100 FEW250 16/M03 A3012 RMK AO2 SLP214 T01611028"
 *
 * Components:
 * - METAR: Report type
 * - KJFK: Station identifier (JFK Airport)
 * - 142252Z: Day (14th) and time (22:52 UTC)
 * - 19005KT: Wind from 190° at 5 knots
 * - 10SM: Visibility 10 statute miles
 * - FEW100 FEW250: Few clouds at 10,000 and 25,000 feet
 * - 16/M03: Temperature 16°C, Dewpoint -3°C
 * - A3012: Altimeter setting 30.12 inHg
 * - RMK: Remarks section
 *
 * @author bclasky1539
 */
public class NoaaMetarParser extends NoaaAviationWeatherParser<NoaaMetarData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoaaMetarParser.class);

    // Pressure Tendency Pattern Groups
    private static final String GROUP_TENDENCY_CODE = "tend";
    private static final String GROUP_PRESSURE_CHANGE = "press";
    private static final String GROUP_HEIGHT_CODE = "height";

    // Pattern registry for METAR parsing
    private final NoaaAviationWeatherPatternRegistry patternRegistry;

    // METAR-specific state
    private Instant issueTime;
    private String reportType;

    public NoaaMetarParser() {
        this.patternRegistry = new NoaaAviationWeatherPatternRegistry();
    }

    @Override
    public ParseResult<NoaaWeatherData> parse(String rawData) {
        if (rawData == null || rawData.trim().isEmpty()) {
            return ParseResult.failure("Raw data cannot be null or empty");
        }

        if (!canParse(rawData)) {
            return ParseResult.failure("Data is not a valid METAR report");
        }

        try {
            initializeParsingState();

            String token = rawData.trim();
            String[] parts = splitMainBodyAndRemarks(token);
            String mainBody = parts[0];
            String remarks = parts[1];

            mainBody = parseMainBody(mainBody);
            remarks = parseRemarks(remarks);

            validateParsedData();
            buildAndSetConditions();

            weatherData.setRawText(rawData.trim());

            logUnparsedTokens(mainBody, remarks);

            return ParseResult.success(weatherData);

        } catch (IllegalStateException | IllegalArgumentException e) {
            return ParseResult.failure(
                    "Failed to parse METAR data: " + e.getMessage(), e
            );
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected error parsing METAR data", e);
            return ParseResult.failure(
                    "Unexpected parsing error: " + e.getMessage(), e
            );
        }
    }

    /**
     * Initialize all parsing state fields.
     * Calls base class to initialize shared state.
     */
    private void initializeParsingState() {
        initializeSharedState();  // Initialize base class state
        this.weatherData = null;
        this.issueTime = null;
        this.reportType = "METAR";
    }

    /**
     * Split METAR into main body and remarks sections.
     *
     * @param token the raw METAR string
     * @return array with [mainBody, remarks]
     */
    private String[] splitMainBodyAndRemarks(String token) {
        String[] parts = token.split("\\s+RMK\\s+", 2);
        String mainBody = parts[0];
        String remarks = parts.length > 1 ? parts[1] : "";
        return new String[]{mainBody, remarks};
    }

    /**
     * Parse the main METAR body (before RMK).
     *
     * @param mainBody the main body section
     * @return remaining unparsed tokens
     */
    private String parseMainBody(String mainBody) {
        IndexedLinkedHashMap<Pattern, NoaaAviationWeatherPatternHandler> mainHandlers =
                patternRegistry.getMainHandlers();
        return parseWithHandlers(mainBody, mainHandlers, "MAIN");
    }

    /**
     * Parse the remarks section (after RMK).
     *
     * @param remarks the remarks section
     * @return remaining unparsed tokens
     */
    private String parseRemarks(String remarks) {
        if (remarks.isEmpty()) {
            return remarks;
        }

        String originalRemarks = remarks;
        IndexedLinkedHashMap<Pattern, NoaaAviationWeatherPatternHandler> remarkHandlers =
                patternRegistry.getRemarksHandlers();

        remarks = parseWithHandlers(remarks, remarkHandlers, "REMARK");

        // Also do sequential parsing of remarks for components not in registry
        handleRemarks(originalRemarks, weatherData);

        return remarks;
    }

    /**
     * Validate that minimum required data was extracted.
     *
     * @throws IllegalStateException if validation fails
     */
    private void validateParsedData() {
        if (weatherData == null || weatherData.getStationId() == null) {
            throw new IllegalStateException("Could not extract station ID from METAR");
        }
    }

    /**
     * Build WeatherConditions from accumulated data and set on weather data.
     * Uses base class buildConditions() method.
     */
    private void buildAndSetConditions() {
        if (weatherData != null) {
            weatherData.setConditions(buildConditions());  // Call base class method
        }
    }

    @Override
    public boolean canParse(String rawData) {
        if (rawData == null || rawData.trim().isEmpty()) {
            return false;
        }

        String trimmed = rawData.trim();

        // Check if starts with date/time pattern (YYYY/MM/DD HH:MM format)
        if (trimmed.matches("^\\d{4}/\\d{2}/\\d{2}\\s+.*")) {
            return true;
        }

        // Check if METAR or SPECI appears at the start (not just anywhere)
        return trimmed.matches("^\\s*(METAR|SPECI)\\s+.*");
    }

    @Override
    public String getSourceType() {
        return "NOAA_METAR";
    }

    /**
     * Parse token string using ordered pattern handlers from registry.
     *
     * @param token The token string to parse
     * @param handlers Ordered map of patterns and their handlers from NoaaAviationWeatherPatternRegistry
     * @param handlersType Type of handlers ("MAIN" or "REMARK") for logging
     * @return Remaining unparsed token string
     */
    private String parseWithHandlers(String token,
                                     IndexedLinkedHashMap<Pattern, NoaaAviationWeatherPatternHandler> handlers,
                                     String handlersType) {
        String currentToken = token;

        while (!currentToken.isEmpty()) {
            LOGGER.debug("\n{} - Processing token: '{}'", handlersType, currentToken);

            String updatedToken = tryAllPatterns(currentToken, handlers);

            // If no match found, exit loop
            if (updatedToken.equals(currentToken)) {
                break;
            }

            currentToken = updatedToken;
        }

        return currentToken;
    }

    /**
     * Try all patterns against the token and return updated token after first match.
     *
     * @param token Current token to parse
     * @param handlers Pattern handlers to try
     * @return Updated token after match, or original token if no match
     */
    private String tryAllPatterns(String token,
                                  IndexedLinkedHashMap<Pattern, NoaaAviationWeatherPatternHandler> handlers) {
        for (Map.Entry<Pattern, NoaaAviationWeatherPatternHandler> entry : handlers.entrySet()) {
            Pattern pattern = entry.getKey();
            NoaaAviationWeatherPatternHandler handlerInfo = entry.getValue();

            LOGGER.debug("Trying pattern: {} ({})",
                    handlerInfo.handlerName(), pattern.pattern());

            String updatedToken = tryPattern(token, pattern, handlerInfo);

            // If pattern matched (token changed), return updated token
            if (!updatedToken.equals(token)) {
                return updatedToken;
            }
        }

        // No pattern matched
        return token;
    }

    /**
     * Try a single pattern against the token, applying it repeatedly if configured.
     *
     * @param token Current token to parse
     * @param pattern Pattern to try
     * @param handlerInfo Handler metadata
     * @return Updated token after all matches, or original token if no match
     */
    private String tryPattern(String token, Pattern pattern, NoaaAviationWeatherPatternHandler handlerInfo) {
        String currentToken = token;
        Matcher matcher = pattern.matcher(currentToken);

        while (matcher.find() && !matcher.group(0).isEmpty()) {
            logMatchDetails(matcher);

            // Execute handler to extract data
            handlePattern(handlerInfo.handlerName(), matcher);

            // Remove matched portion and prepare for next iteration
            currentToken = prepareTokenForNextMatch(matcher);

            LOGGER.debug("Token after match: '{}'", currentToken);

            // If not repeating, stop after first match
            if (!handlerInfo.canRepeat()) {
                break;
            }

            // Re-apply pattern for repeating handlers
            matcher = pattern.matcher(currentToken);
        }

        return currentToken;
    }

    /**
     * Log details about a successful pattern match.
     *
     * @param matcher The matcher with a successful match
     */
    private void logMatchDetails(Matcher matcher) {
        LOGGER.debug("Match found - Group count: {}", matcher.groupCount());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Match group(0): '{}'", matcher.group(0));
        }

        // Log all capture groups
        if (LOGGER.isDebugEnabled()) {
            for (int j = 1; j <= matcher.groupCount(); j++) {
                LOGGER.debug("  Capture group {}: '{}'", j, matcher.group(j));
            }
        }
    }

    /**
     * Remove matched portion from token and add trailing space if needed.
     *
     * @param matcher The matcher with a successful match
     * @return Updated token ready for next match
     */
    private String prepareTokenForNextMatch(Matcher matcher) {
        String updatedToken = matcher.replaceFirst("").trim();

        // Add trailing space for proper next match (legacy behavior)
        if (!updatedToken.isEmpty()) {
            updatedToken += " ";
        }

        return updatedToken;
    }

    /**
     * Route to appropriate handler method based on handler name from registry.
     *
     * Handlers for shared aviation weather elements (wind, visibility, present weather,
     * sky conditions) are inherited from NoaaAviationWeatherParser.
     *
     * @param handlerName The name of the handler from NoaaAviationWeatherPatternRegistry
     * @param matcher The regex matcher with captured groups
     */
    private void handlePattern(String handlerName, Matcher matcher) {
        try {
            switch (handlerName) {
                // METAR-specific handlers
                case "reportType" -> handleReportType(matcher);
                case "monthDayYear" -> handleIssueDateTime(matcher);
                case "station" -> handleStationAndObsTime(matcher);
                case "reportModifier" -> handleReportModifier(matcher);
                case "tempDewpoint" -> handleTempDewpoint(matcher);
                case "altimeter" -> handleAltimeter(matcher);
                case "noSigChange" -> handleNoSigChange(matcher);
                case "unparsed" -> handleUnparsed(matcher);

                // Shared handlers (from base class)
                case "wind" -> handleWind(matcher);
                case "visibility" -> handleVisibility(matcher);
                case "runway" -> handleRunway(matcher);  // Override for full RVR support
                case "presentWeather" -> handlePresentWeather(matcher);
                case "skyCondition" -> handleSkyCondition(matcher);

                // Remarks handlers
                case "autoType" -> handleAutoType(matcher);
                case "seaLevelPressure" -> handleRemarkRegistry(matcher, "Sea level pressure");
                case "hourlyTemperature" -> handleRemarkRegistry(matcher, "Hourly temperature");
                case "peakWind" -> handleRemarkRegistry(matcher, "Peak wind");
                case "windShift" -> handleRemarkRegistry(matcher, "Wind shift");
                case "variableVis" -> handleRemarkRegistry(matcher, "Variable visibility");
                case "towerSurfaceVis" -> handleRemarkRegistry(matcher, "Tower/Surface visibility");
                case "precip1Hour" -> handleRemarkRegistry(matcher, "Hourly precipitation");
                case "precip3Hr24Hr" -> handleRemarkRegistry(matcher, "Multi-hour precipitation");
                case "hailSize" -> handleRemarkRegistry(matcher, "Hail size");

                default -> LOGGER.debug("No handler implemented for: {}", handlerName);
            }
        } catch (Exception e) {
            LOGGER.warn("Error in handler '{}': {}", handlerName, e.getMessage(), e);
        }
    }

    // ==================== METAR-SPECIFIC PATTERN HANDLERS ====================

    /**
     * Handle report type: "METAR" or "SPECI"
     * Captures the type to be used when creating NoaaWeatherData.
     */
    private void handleReportType(Matcher matcher) {
        this.reportType = matcher.group(0).trim();
        LOGGER.debug("Report type: {}", reportType);
    }

    /**
     * Handle issue date/time: "2025/11/14 22:52"
     */
    private void handleIssueDateTime(Matcher matcher) {
        int year = Integer.parseInt(matcher.group("year"));
        int month = Integer.parseInt(matcher.group("month"));
        int day = Integer.parseInt(matcher.group("day"));

        String time = matcher.group("time");
        int hour = 0;
        int minute = 0;

        if (time != null) {
            String[] timeParts = time.split(":");
            hour = Integer.parseInt(timeParts[0]);
            minute = Integer.parseInt(timeParts[1]);
        }

        LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hour, minute);
        this.issueTime = localDateTime.toInstant(ZoneOffset.UTC);

        LOGGER.debug("Parsed issue time: {}", issueTime);
    }

    /**
     * Handle station ID and observation time: "KCLT 142252Z"
     */
    private void handleStationAndObsTime(Matcher matcher) {
        String stationId = matcher.group("station");
        int day = Integer.parseInt(matcher.group("zday"));
        int hour = Integer.parseInt(matcher.group("zhour"));
        int minute = Integer.parseInt(matcher.group("zmin"));

        // Determine year and month from issue time or current time
        LocalDateTime referenceTime = issueTime != null ?
                LocalDateTime.ofInstant(issueTime, ZoneOffset.UTC) :
                LocalDateTime.now(ZoneOffset.UTC);

        int year = referenceTime.getYear();
        int month = referenceTime.getMonthValue();

        // Handle month wrap-around
        if (day > referenceTime.getDayOfMonth()) {
            month--;
            if (month < 1) {
                month = 12;
                year--;
            }
        }

        LocalDateTime obsDateTime = LocalDateTime.of(year, month, day, hour, minute);
        Instant observationTime = obsDateTime.toInstant(ZoneOffset.UTC);

        // Create NoaaMetarData instance (assign to inherited weatherData field)
        this.weatherData = new NoaaMetarData(stationId, observationTime);
        this.weatherData.setReportType(this.reportType);

        LOGGER.debug("Station: {}, Observation time: {}, Report type: {}",
                stationId, observationTime, reportType);
    }

    /**
     * Handle report modifier: "AUTO", "COR", etc.
     */
    private void handleReportModifier(Matcher matcher) {
        String modifier = matcher.group(0).trim();
        if (weatherData != null) {
            weatherData.setReportModifier(modifier);
            LOGGER.debug("Report modifier: {}", modifier);
        }
    }

    // ==================== RUNWAY VISUAL RANGE (RVR) HANDLER ====================

    /**
     * Internal helper record to pass RVR range values between methods.
     *
     * @param visualRange Single value (null if variable)
     * @param variableLow Variable low value (null if single)
     * @param variableHigh Variable high value (null if single)
     */
    private record RvrRange(Integer visualRange, Integer variableLow, Integer variableHigh) {}

    /**
     * Override base class to provide full RVR handling for METAR.
     * Handle runway visual range (RVR): "R22R/0400N", "R24/P2000N", "R23L/0900V6000FT", etc.
     */
    @Override
    protected void handleRunway(Matcher matcher) {
        String runwayName = matcher.group("name");

        if (weatherData == null) {
            return;
        }

        // Check for special cases (RVRNO, CLRD)
        if (handleSpecialRvrCases(matcher)) {
            return;
        }

        // Extract and validate runway identifier
        if (runwayName == null) {
            LOGGER.warn("RVR: Missing runway identifier");
            return;
        }

        // Parse RVR values and create object
        parseAndAddRvr(matcher, runwayName);
    }

    /**
     * Handle special RVR cases (RVRNO, CLRD).
     *
     * @param matcher The regex matcher
     * @return true if special case was handled
     */
    private boolean handleSpecialRvrCases(Matcher matcher) {
        String fullMatch = matcher.group(0);
        if (fullMatch != null && fullMatch.trim().startsWith("RVRNO")) {
            LOGGER.debug("RVR: Not available (RVRNO)");
            return true;
        }

        String lowValue = matcher.group("lvalue");
        if ("CLRD".equals(lowValue)) {
            String runwayName = matcher.group("name");

            // Create a cleared RVR
            RunwayVisualRange clearedRvr = RunwayVisualRange.cleared(runwayName);
            weatherData.addRunwayVisualRange(clearedRvr);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("RVR: Cleared for runway {}", runwayName);
            }
            return true;
        }

        return false;
    }

    /**
     * Parse RVR values from matcher and add to weather data.
     *
     * @param matcher The regex matcher
     * @param runwayName The runway identifier
     */
    private void parseAndAddRvr(Matcher matcher, String runwayName) {
        String lowValue = matcher.group("lvalue");
        if (lowValue == null) {
            LOGGER.warn("RVR: Missing low value for runway {}", runwayName);
            return;
        }

        // Parse low value
        Integer visualRange = parseRvrValue(lowValue, runwayName);
        if (visualRange == null) {
            return;
        }

        // Determine prefix
        String prefix = extractRvrPrefix(matcher.group("low"));

        // Parse variable range if present
        RvrRange range = parseVariableRange(matcher, visualRange);

        // Extract trend
        String trend = extractRvrTrend(matcher.group("unit"), runwayName);

        // Create and add RVR
        createAndAddRvr(runwayName, range, prefix, trend);
    }

    /**
     * Parse RVR numeric value.
     *
     * @param value The string value to parse
     * @param runwayName The runway identifier (for logging)
     * @return Parsed integer or null if parsing fails
     */
    private Integer parseRvrValue(String value, String runwayName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("Failed to parse RVR value '{}' for runway {}: {}",
                    value, runwayName, e.getMessage());
            return null;
        }
    }

    /**
     * Extract M/P prefix from low value group.
     *
     * @param lowGroup The low value group from regex
     * @return "M", "P", or null
     */
    private String extractRvrPrefix(String lowGroup) {
        if (lowGroup == null) {
            return null;
        }

        if (lowGroup.startsWith("M")) {
            return "M";
        }
        if (lowGroup.startsWith("P")) {
            return "P";
        }

        return null;
    }

    /**
     * Parse variable range from high value if present.
     *
     * @param matcher The regex matcher
     * @param lowValue The low value already parsed
     * @return RvrRange object with appropriate values
     */
    private RvrRange parseVariableRange(Matcher matcher, Integer lowValue) {
        String highStr = matcher.group("high");

        if (highStr == null) {
            // Not variable - return single value range
            return new RvrRange(lowValue, null, null);
        }

        // Variable range: parse high value
        String highNumStr = highStr.replaceFirst("^[MP]", "");
        try {
            Integer highValue = Integer.parseInt(highNumStr);
            return new RvrRange(null, lowValue, highValue);
        } catch (NumberFormatException e) {
            LOGGER.warn("Failed to parse RVR high value '{}': {}", highStr, e.getMessage());
            return new RvrRange(lowValue, null, null); // Fallback to single value
        }
    }

    /**
     * Extract trend indicator from unit group.
     *
     * @param unit The unit group from regex
     * @param runwayName The runway identifier (for logging)
     * @return Trend indicator (N/D/U) or null
     */
    private String extractRvrTrend(String unit, String runwayName) {
        if (unit == null) {
            return null;
        }

        if ("N".equals(unit) || "D".equals(unit) || "U".equals(unit)) {
            return unit;
        }

        if ("FT".equals(unit)) {
            LOGGER.debug("RVR: Explicit FT unit for runway {}", runwayName);
        } else if (unit.matches("\\d{2,4}")) {
            LOGGER.debug("RVR: Numeric suffix '{}' for runway {}", unit, runwayName);
        }

        return null;
    }

    /**
     * Create RunwayVisualRange object and add to weather data.
     *
     * @param runwayName The runway identifier
     * @param range The RVR range values
     * @param prefix The M/P prefix
     * @param trend The trend indicator
     */
    private void createAndAddRvr(String runwayName, RvrRange range, String prefix, String trend) {
        try {
            RunwayVisualRange rvr = new RunwayVisualRange(
                    runwayName,
                    range.visualRange(),
                    range.variableLow(),
                    range.variableHigh(),
                    prefix,
                    trend
            );

            weatherData.addRunwayVisualRange(rvr);

            LOGGER.debug("RVR: Runway {}, Visual Range: {}, Variable: {}-{}, Prefix: {}, Trend: {}",
                    runwayName, range.visualRange(), range.variableLow(), range.variableHigh(), prefix, trend);

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to create RVR for runway {}: {}", runwayName, e.getMessage());
        }
    }

    /**
     * Handle temperature and dewpoint.
     *
     * Format: TT/DD or M[TT]/M[DD]
     * Examples: 22/12, M05/M12, 15/, //, XX/XX
     *
     * The 'M' prefix indicates negative (minus) temperature.
     * Special values (//, XX, MM) indicate missing data.
     */
    private void handleTempDewpoint(Matcher matcher) {
        if (weatherData == null) {
            return;
        }

        String tempSign = matcher.group("signt");
        String tempDigits = matcher.group("temp");
        String dewpointSign = matcher.group("signd");
        String dewpointDigits = matcher.group("dewpt");

        try {
            // Parse temperature
            Double temperatureCelsius = parseTemperatureValue(tempSign, tempDigits);

            // Parse dewpoint (optional)
            Double dewpointCelsius = parseTemperatureValue(dewpointSign, dewpointDigits);

            // Only create Temperature object if we have at least temperature
            if (temperatureCelsius != null) {
                Temperature temperature = Temperature.ofCurrent(temperatureCelsius, dewpointCelsius);
                conditionsBuilder.temperature(temperature);

                LOGGER.debug("Temperature: {}°C, Dewpoint: {}°C",
                        temperatureCelsius,
                        dewpointCelsius != null ? dewpointCelsius : "N/A");
            } else {
                LOGGER.debug("Temperature: Missing/Unknown");
            }

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to parse temperature/dewpoint '{}': {}",
                    matcher.group(0).trim(), e.getMessage());
        }
    }

    /**
     * Parse a temperature or dewpoint value from METAR format.
     *
     * Handles:
     * - M prefix for negative (e.g., "M05" → -5.0)
     * - Hyphen prefix for negative (e.g., "-05" → -5.0)
     * - Missing indicators (//, XX, MM) → null
     * - Null or blank → null
     *
     * @param sign the sign indicator ("M", "-", or null)
     * @param digits the temperature digits (e.g., "05", "22")
     * @return temperature in Celsius, or null if missing/unknown
     */
    private Double parseTemperatureValue(String sign, String digits) {
        // Handle missing/unknown values
        if (digits == null || digits.isBlank() ||
                "//".equals(digits) || "XX".equals(digits) || "MM".equals(digits)) {
            return null;
        }

        try {
            double value = Double.parseDouble(digits);

            // Apply negative sign if present
            if ("M".equals(sign) || "-".equals(sign)) {
                value = -value;
            }

            return value;

        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid temperature format: {}", digits);
            return null;
        }
    }

    /**
     * Handle altimeter/pressure setting.
     *
     * Formats:
     * - A3015 → 30.15 inHg (North America)
     * - Q1013 → 1013 hPa (International)
     * - QNH1013 → 1013 hPa
     * - 2992INS → 29.92 inHg (older format)
     * - //// → Missing
     */
    private void handleAltimeter(Matcher matcher) {
        if (weatherData == null) {
            return;
        }

        String unit1 = matcher.group("unit");
        String pressureStr = matcher.group(GROUP_PRESSURE_CHANGE);
        String unit2 = matcher.group("unit2");

        // Handle missing pressure
        if ("////".equals(pressureStr)) {
            LOGGER.debug("Altimeter: Missing (////)");
            return;
        }

        try {
            // Determine unit and parse pressure
            Pressure pressure = parsePressure(unit1, pressureStr, unit2);

            if (pressure != null) {
                conditionsBuilder.pressure(pressure);

                LOGGER.debug("Altimeter: {}", pressure.getSummary());
            }

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to parse altimeter '{}': {}",
                    matcher.group(0).trim(), e.getMessage());
        }
    }

    /**
     * Parse pressure value and determine unit.
     *
     * Logic:
     * - "A" or "AA" prefix → inches of mercury (divide by 100)
     * - "Q" or "QNH" prefix → hectopascals
     * - "INS" suffix → inches of mercury (divide by 100)
     * - 4 digits starting with 2 or 3 → inches of mercury (divide by 100)
     * - 4 digits starting with 0 or 1 → hectopascals
     * - 3 digits → hectopascals
     *
     * @param unit1 Prefix unit indicator
     * @param pressureStr Pressure digits
     * @param unit2 Suffix unit indicator
     * @return Pressure object, or null if invalid
     */
    private Pressure parsePressure(String unit1, String pressureStr, String unit2) {
        if (pressureStr == null || pressureStr.isBlank()) {
            return null;
        }

        // Handle OCR error: O → 0
        String normalized = pressureStr.replace('O', '0');

        try {
            int pressureValue = Integer.parseInt(normalized);

            // Determine unit based on prefix
            if (unit1 != null && !unit1.isBlank()) {
                return parsePressureWithPrefix(unit1, pressureValue);
            }

            // Determine unit based on suffix
            if ("INS".equals(unit2)) {
                return parseInchesHg(pressureValue);
            }

            // Determine unit based on value
            return parsePressureByValue(pressureValue);

        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid pressure format: {}", pressureStr);
            return null;
        }
    }

    /**
     * Parse pressure with unit prefix (A, AA, Q, QNH).
     *
     * @param prefix Unit prefix
     * @param value Pressure value
     * @return Pressure object
     */
    private Pressure parsePressureWithPrefix(String prefix, int value) {
        return switch (prefix.toUpperCase()) {
            case "A", "AA" -> parseInchesHg(value);
            case "Q", "QNH" -> parseHectopascals(value);
            default -> {
                LOGGER.warn("Unknown pressure unit prefix: {}", prefix);
                yield parsePressureByValue(value);
            }
        };
    }

    /**
     * Parse pressure as inches of mercury.
     * Value is divided by 100 (e.g., 3015 → 30.15 inHg).
     *
     * @param value Pressure value (e.g., 3015)
     * @return Pressure in inches of mercury
     */
    private Pressure parseInchesHg(int value) {
        double inHg = value / 100.0;
        return new Pressure(inHg, PressureUnit.INCHES_HG);
    }

    /**
     * Parse pressure as hectopascals.
     *
     * @param value Pressure value (e.g., 1013)
     * @return Pressure in hectopascals
     */
    private Pressure parseHectopascals(int value) {
        return new Pressure((double) value, PressureUnit.HECTOPASCALS);
    }

    /**
     * Determine pressure unit based on value heuristics.
     *
     * - 4 digits starting with 2 or 3 → inches Hg (e.g., 2992, 3015)
     * - 4 digits starting with 0 or 1 → hPa (e.g., 1013, 0998)
     * - 3 digits → hPa (e.g., 998)
     *
     * @param value Pressure value
     * @return Pressure object
     */
    private Pressure parsePressureByValue(int value) {
        // 4-digit values
        if (value >= 1000) {
            // 2xxx or 3xxx → inches of mercury
            if (value >= 2000 && value < 4000) {
                return parseInchesHg(value);
            }
            // 0xxx or 1xxx → hectopascals
            return parseHectopascals(value);
        }

        // 3-digit values → assume hectopascals
        return parseHectopascals(value);
    }

    /**
     * Handle NOSIG (No Significant Change) indicator in main METAR body.
     * Appears after altimeter setting and before RMK.
     *
     * @param matcher the regex matcher positioned at NOSIG
     */
    private void handleNoSigChange(Matcher matcher) {
        if (weatherData == null) {
            return;
        }

        weatherData.setNoSignificantChange(true);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("NOSIG (No Significant Change) indicator found: '{}'", matcher.group(0));
        }
    }

    /**
     * Generic handler for registry-based remark patterns.
     * This is a stub that logs when a pattern is matched by the registry.
     * The actual parsing is done by sequential handlers in handleRemarks().
     *
     * @param matcher The regex matcher with captured groups
     * @param remarkType The type of remark for logging
     */
    private void handleRemarkRegistry(Matcher matcher, String remarkType) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} pattern matched: {}", remarkType, matcher.group(0));
        }
    }

    /**
     * Handle automated station type from remarks (AO1 or AO2).
     *
     * This is called by the pattern registry when AUTO_PATTERN matches.
     * The actual work is delegated to handleAutomatedStationType which
     * can be called standalone for sequential parsing.
     *
     * @param matcher The regex matcher with captured groups
     */
    private void handleAutoType(Matcher matcher) {
        if (weatherData == null) {
            return;
        }

        try {
            String typeDigit = matcher.group("type");
            AutomatedStationType stationType = AutomatedStationType.fromDigit(typeDigit);

            // For now, just log it - full integration will store in remarks
            logAutomatedStationType(stationType);

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid automated station type: {}", matcher.group(0), e);
        }
    }

    // ==================== REMARKS HANDLERS ====================
    // (Keeping all the existing remarks handlers - they are METAR-specific)
    // These are NOT moved to base class as they are specific to METAR observations

    /**
     * Handle the remarks section of the METAR using sequential parsing.
     *
     * This method processes remarks in order and stores any unparsed content
     * as free text. Called after the main body has been parsed.
     *
     * @param remarksText the remarks text (everything after RMK)
     * @param metarData the METAR data object to populate
     */
    private void handleRemarks(String remarksText, NoaaMetarData metarData) {
        if (remarksText == null || remarksText.isBlank()) {
            return;
        }

        NoaaMetarRemarks.Builder remarksBuilder = NoaaMetarRemarks.builder();
        String remaining = remarksText.trim();
        String previous;

        // Multi-pass parsing to handle any order
        do {
            previous = remaining;

            // Try each handler in sequence
            remaining = handleAutomatedStationType(remaining, remarksBuilder);
            remaining = handleSeaLevelPressureSequential(remaining, remarksBuilder);
            remaining = handleHourlyTemperatureSequential(remaining, remarksBuilder);
            remaining = handlePeakWindSequential(remaining, remarksBuilder);
            remaining = handleWindShiftSequential(remaining, remarksBuilder);
            remaining = handleVariableVisibilitySequential(remaining, remarksBuilder);
            remaining = handleVariableCeilingSequential(remaining, remarksBuilder);
            remaining = handleCeilingSecondSiteSequential(remaining, remarksBuilder);
            remaining = handleObscurationSequential(remaining, remarksBuilder);
            remaining = handleThunderstormLocationSequential(remaining, remarksBuilder);
            remaining = handleCloudTypeSequential(remaining, remarksBuilder);
            remaining = handleTowerSurfaceVisibilitySequential(remaining, remarksBuilder);
            remaining = handleHourlyPrecipitationSequential(remaining, remarksBuilder);
            remaining = handleMultiHourPrecipitationSequential(remaining, remarksBuilder);
            remaining = handleHailSizeSequential(remaining, remarksBuilder);
            remaining = handleWeatherEventsSequential(remaining, remarksBuilder);
            remaining = handlePressureTendencySequential(remaining, remarksBuilder);
            remaining = handle6HourMaxMinTemperatureSequential(remaining, remarksBuilder);
            remaining = handle24HourMaxMinTemperatureSequential(remaining, remarksBuilder);
            remaining = handleAutomatedMaintenanceSequential(remaining, remarksBuilder);

            // Continue while we're making progress
        } while (!Objects.equals(remaining, previous));

        // Store any unparsed remarks as free text
        if (remaining != null && !remaining.isBlank()) {
            remarksBuilder.freeText(remaining.trim());
        }

        metarData.setRemarks(remarksBuilder.build());
    }

    /**
     * Handle automated station type remark for sequential parsing.
     *
     * Format: AO1 or AO2
     * - AO1 = Automated station WITHOUT precipitation discriminator
     * - AO2 = Automated station WITH precipitation discriminator
     * - A01/A02 = OCR error variants (O misread as 0)
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleAutomatedStationType(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Use the AUTO_PATTERN from the registry
        Pattern autoPattern = Pattern.compile("^A[O0](?<type>\\d)\\s*");
        Matcher matcher = autoPattern.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        try {
            String typeDigit = matcher.group("type");
            AutomatedStationType stationType = AutomatedStationType.fromDigit(typeDigit);

            remarks.automatedStationType(stationType);

            logAutomatedStationType(stationType);

            return remarksText.substring(matcher.end()).trim();

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid automated station type in remarks: {}",
                    remarksText.substring(0, Math.min(20, remarksText.length())), e);
            return remarksText.substring(matcher.end()).trim();
        }
    }

    private void logAutomatedStationType(AutomatedStationType stationType) {
        LOGGER.debug("Automated Station Type: {} ({})",
                stationType.getCode(),
                stationType.getDescription());
    }

    /**
     * Handle sea level pressure remark for sequential parsing.
     *
     * Format: SLPppp where ppp is pressure in tenths of hectopascals
     *
     * Examples:
     * - SLP210 → 1021.0 hPa (ppp=210 < 500, so 1000 + 21.0)
     * - SLP982 → 998.2 hPa (ppp=982 >= 500, so 900 + 98.2)
     * - SLP145 → 1014.5 hPa
     * - SLPNO → Sea level pressure not available
     *
     * Decoding rules per Federal Meteorological Handbook No. 1:
     * - If ppp >= 500: Sea level pressure = 900 + (ppp / 10) hPa
     * - If ppp < 500: Sea level pressure = 1000 + (ppp / 10) hPa
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleSeaLevelPressureSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank (validated by handleRemarks())

        Matcher matcher = SEALVL_PRESS_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        try {
            String pressureStr = matcher.group(GROUP_PRESSURE_CHANGE);

            // Handle SLPNO (pressure not available)
            if ("NO".equals(pressureStr)) {
                LOGGER.debug("Sea level pressure: Not available (SLPNO)");
                return remarksText.substring(matcher.end()).trim();
            }

            // Parse pressure value
            if (pressureStr != null && !pressureStr.isEmpty()) {
                int ppp = Integer.parseInt(pressureStr);

                // Apply decoding rules
                double pressureHPa;
                if (ppp >= 500) {
                    // High values (500-999): add 900
                    pressureHPa = 900.0 + (ppp / 10.0);
                } else {
                    // Low values (000-499): add 1000
                    pressureHPa = 1000.0 + (ppp / 10.0);
                }

                // Create Pressure object using factory method
                Pressure seaLevelPressure = Pressure.hectopascals(pressureHPa);

                // Store in remarks builder
                remarks.seaLevelPressure(seaLevelPressure);

                LOGGER.debug("Sea level pressure: {} hPa (from SLP{})", pressureHPa, ppp);
            }

            return remarksText.substring(matcher.end()).trim();

        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid sea level pressure format in remarks: {}",
                    remarksText.substring(0, Math.min(20, remarksText.length())), e);
            return remarksText.substring(matcher.end()).trim();
        }
    }

    /**
     * Handle hourly temperature and dewpoint remark for sequential parsing.
     *
     * Format: TsnT'T'T'snT'dT'dT'd
     * - sn = sign (0=positive, 1=negative)
     * - T'T'T' = temperature in tenths of degrees (e.g., 233 = 23.3°C)
     * - snT'dT'dT'd = optional dewpoint with sign and tenths
     *
     * Examples:
     * - T02330139 → temp=+23.3°C, dewpoint=+13.9°C
     * - T10281015 → temp=-2.8°C, dewpoint=-1.5°C
     * - T0233 → temp=+23.3°C, dewpoint=not reported
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleHourlyTemperatureSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank

        Matcher matcher = TEMP_1HR_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        try {
            // Parse temperature
            String tempSign = matcher.group("tsign");
            String tempStr = matcher.group("temp");

            if (tempStr != null && !tempStr.isEmpty()) {
                int tempTenths = Integer.parseInt(tempStr);
                double tempCelsius = tempTenths / 10.0;

                // Apply sign (0=positive, 1=negative)
                if ("1".equals(tempSign)) {
                    tempCelsius = -tempCelsius;
                }

                // Parse dewpoint (optional)
                String dewptSign = matcher.group("dsign");
                String dewptStr = matcher.group("dewpt");

                if (dewptStr != null && !dewptStr.isEmpty()) {
                    // Both temperature and dewpoint present
                    int dewptTenths = Integer.parseInt(dewptStr);
                    double dewptCelsius = dewptTenths / 10.0;

                    // Apply sign
                    if ("1".equals(dewptSign)) {
                        dewptCelsius = -dewptCelsius;
                    }

                    // Create Temperature with both values
                    Temperature preciseTemp = Temperature.of(tempCelsius, dewptCelsius);
                    remarks.preciseTemperature(preciseTemp);
                    remarks.preciseDewpoint(preciseTemp); // Store the same object for dewpoint access

                    LOGGER.debug("Precise temperature: {}°C, dewpoint: {}°C",
                            tempCelsius, dewptCelsius);

                } else {
                    // Only temperature present, no dewpoint
                    Temperature preciseTemp = Temperature.of(tempCelsius);
                    remarks.preciseTemperature(preciseTemp);

                    LOGGER.debug("Precise temperature: {}°C (no dewpoint)", tempCelsius);
                }
            }

            return remarksText.substring(matcher.end()).trim();

        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid hourly temperature format in remarks: {}",
                    remarksText.substring(0, Math.min(20, remarksText.length())), e);
            return remarksText.substring(matcher.end()).trim();
        }
    }

    /**
     * Handle peak wind remark for sequential parsing.
     *
     * Format: PK WND dddff(f)/(hh)mm
     * - ddd = wind direction in degrees (optional)
     * - ff(f) = wind speed in knots, 2-3 digits (P prefix for speeds >99 knots)
     * - hh = hour of occurrence (optional)
     * - mm = minute of occurrence (optional)
     *
     * Examples:
     * - PK WND 28032/1530 → dir=280°, speed=32kt, time=15:30 UTC
     * - PK WND 32035/15 → dir=320°, speed=35kt, time=XX:15 UTC (hour missing)
     * - PK WND P100/2145 → dir=unknown, speed=100kt, time=21:45 UTC
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handlePeakWindSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank

        Matcher matcher = PEAK_WIND_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        try {
            // Parse direction (optional)
            String dirStr = matcher.group("dir");
            Integer direction = null;
            if (dirStr != null && !dirStr.isEmpty()) {
                direction = Integer.parseInt(dirStr);
            }

            // Parse speed (required, but handle P prefix for >99 knots)
            String speedStr = matcher.group("speed");
            Integer speed = null;
            if (speedStr != null && !speedStr.isEmpty()) {
                // Remove 'P' prefix if present (indicates speed >99 knots)
                speedStr = speedStr.replace("P", "");
                speed = Integer.parseInt(speedStr);
            }

            // Parse hour (optional)
            String hourStr = matcher.group("hour");
            Integer hour = null;
            if (hourStr != null && !hourStr.isEmpty()) {
                hour = Integer.parseInt(hourStr);
            }

            // Parse minute (optional)
            String minStr = matcher.group("min");
            Integer minute = null;
            if (minStr != null && !minStr.isEmpty()) {
                minute = Integer.parseInt(minStr);
            }

            // Create PeakWind object
            PeakWind peakWind = new PeakWind(direction, speed, hour, minute);
            remarks.peakWind(peakWind);

            LOGGER.debug("Peak wind: dir={}°, speed={}kt, time={}:{}",
                    direction, speed,
                    hour != null ? String.format("%02d", hour) : "XX",
                    minute != null ? String.format("%02d", minute) : "XX");

            return remarksText.substring(matcher.end()).trim();

        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid peak wind format in remarks: {}",
                    remarksText.substring(0, Math.min(20, remarksText.length())), e);
            return remarksText.substring(matcher.end()).trim();
        }
    }

    /**
     * Handle wind shift remark for sequential parsing.
     *
     * Format: WSHFT (hh)mm [FROPA]
     * - hh = hour of wind shift (optional)
     * - mm = minute of wind shift (required)
     * - FROPA = frontal passage indicator (optional)
     *
     * A wind shift is reported when wind direction changes by 45° or more
     * in less than 15 minutes, with sustained winds of 10 knots or more.
     *
     * Examples:
     * - WSHFT 1530 → hour=15, minute=30, no frontal passage
     * - WSHFT 1530 FROPA → hour=15, minute=30, with frontal passage
     * - WSHFT 30 → hour=null, minute=30, no frontal passage
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleWindShiftSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank

        Matcher matcher = WIND_SHIFT_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        try {
            // Parse hour (optional)
            String hourStr = matcher.group("hour");
            Integer hour = null;
            if (hourStr != null && !hourStr.isEmpty()) {
                hour = Integer.parseInt(hourStr);
            }

            // Parse minute (required)
            String minStr = matcher.group("min");
            Integer minute = null;
            if (minStr != null && !minStr.isEmpty()) {
                minute = Integer.parseInt(minStr);
            }

            // Check for frontal passage (FROPA)
            String frontStr = matcher.group("front");
            boolean frontalPassage = "FROPA".equals(frontStr);

            // Create WindShift object
            WindShift windShift = new WindShift(hour, minute, frontalPassage);
            remarks.windShift(windShift);

            LOGGER.debug("Wind shift: time={}:{}, frontal passage={}",
                    hour != null ? String.format("%02d", hour) : "XX",
                    minute != null ? String.format("%02d", minute) : "XX",
                    frontalPassage);

            return remarksText.substring(matcher.end()).trim();

        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid wind shift format in remarks: {}",
                    remarksText.substring(0, Math.min(20, remarksText.length())), e);
            return remarksText.substring(matcher.end()).trim();
        }
    }

    /**
     * Handle variable visibility remark for sequential parsing.
     *
     * Format: VIS [DIR] minVmax [RWY]
     * - DIR = Optional direction (N, NE, E, SE, S, SW, W, NW)
     * - min = Minimum visibility (fraction, mixed, or whole number)
     * - V = "V" separator
     * - max = Maximum visibility (same formats as min)
     * - RWY = Optional runway/location qualifier
     *
     * Examples:
     * - VIS 1/2V2 → min=1/2 SM, max=2 SM
     * - VIS NE 2V4 → Northeast, min=2 SM, max=4 SM
     * - VIS 1 1/4V3 → min=1.25 SM, max=3 SM
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleVariableVisibilitySequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank

        Matcher matcher = VPV_SV_VSL_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        try {
            // Extract direction (optional)
            String direction = matcher.group("dir");

            // Extract minimum visibility
            String dist1Str = matcher.group("dist1");
            if (dist1Str == null || dist1Str.isBlank()) {
                LOGGER.debug("Variable visibility missing minimum distance, skipping");
                return remarksText.substring(matcher.end()).trim();
            }

            // Extract maximum visibility indicator and value
            String addStr = matcher.group("add");
            String dist2Str = matcher.group("dist2");

            // "V" indicates variable visibility, "RWY" is for runway
            if (!"V".equals(addStr)) {
                LOGGER.debug("Variable visibility not marked with 'V', skipping");
                return remarksText.substring(matcher.end()).trim();
            }

            if (dist2Str == null || dist2Str.isBlank()) {
                LOGGER.debug("Variable visibility missing maximum distance, skipping");
                return remarksText.substring(matcher.end()).trim();
            }

            // Parse minimum visibility
            Visibility minVisibility = parseVisibilityDistance(dist1Str);
            if (minVisibility == null) {
                LOGGER.warn("Failed to parse minimum visibility: {}", dist1Str);
                return remarksText.substring(matcher.end()).trim();
            }

            // Parse maximum visibility
            Visibility maxVisibility = parseVisibilityDistance(dist2Str);
            if (maxVisibility == null) {
                LOGGER.warn("Failed to parse maximum visibility: {}", dist2Str);
                return remarksText.substring(matcher.end()).trim();
            }

            // Determine location (currently not used in pattern, but keeping for future)
            String location = null; // Pattern doesn't capture RWY separately in current regex

            // Create VariableVisibility object
            VariableVisibility variableVisibility = new VariableVisibility(
                    minVisibility,
                    maxVisibility,
                    direction,
                    location
            );

            remarks.variableVisibility(variableVisibility);

            LOGGER.debug("Variable visibility: {} varying to {}{}",
                    dist1Str,
                    dist2Str,
                    direction != null ? " (" + direction + ")" : "");

            return remarksText.substring(matcher.end()).trim();

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid variable visibility in remarks: {}",
                    remarksText.substring(0, Math.min(30, remarksText.length())), e);
            return remarksText.substring(matcher.end()).trim();
        }
    }

    /**
     * Parse a visibility distance string into a Visibility object.
     *
     * Handles formats:
     * - Fractions: "1/2", "3/4"
     * - Mixed: "1 1/2", "2 1/4"
     * - Whole numbers: "1", "10"
     *
     * @param distStr the distance string
     * @return Visibility object, or null if parsing fails
     */
    private Visibility parseVisibilityDistance(String distStr) {
        if (distStr == null || distStr.isBlank()) {
            return null;
        }

        try {
            distStr = distStr.trim();

            // Check for fraction: "1/2"
            if (distStr.contains("/")) {
                // Could be mixed: "1 1/2" or simple: "1/2"
                String[] parts = distStr.split("\\s+");

                if (parts.length == 2) {
                    // Mixed fraction: "1 1/2"
                    int wholePart = Integer.parseInt(parts[0]);
                    String[] fractionParts = parts[1].split("/");
                    int numerator = Integer.parseInt(fractionParts[0]);
                    int denominator = Integer.parseInt(fractionParts[1]);
                    double value = wholePart + ((double) numerator / denominator);
                    return Visibility.statuteMiles(value);

                } else {
                    // Simple fraction: "1/2"
                    String[] fractionParts = distStr.split("/");
                    int numerator = Integer.parseInt(fractionParts[0]);
                    int denominator = Integer.parseInt(fractionParts[1]);
                    double value = (double) numerator / denominator;
                    return Visibility.statuteMiles(value);
                }

            } else {
                // Whole number: "10"
                double value = Double.parseDouble(distStr);
                return Visibility.statuteMiles(value);
            }

        } catch (NumberFormatException e) {
            LOGGER.warn("Failed to parse visibility distance: {}", distStr, e);
            return null;
        }
    }

    /**
     * Handle tower or surface visibility remark for sequential parsing.
     *
     * Format: TWR VIS value OR SFC VIS value
     * - TWR VIS = Tower visibility
     * - SFC VIS = Surface visibility
     * - value = Visibility distance (fraction, mixed, or whole number)
     *
     * Examples:
     * - TWR VIS 1 1/2 → Tower visibility 1.5 SM
     * - SFC VIS 1/4 → Surface visibility 0.25 SM
     * - TWR VIS 2 → Tower visibility 2 SM
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleTowerSurfaceVisibilitySequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText != null ? remarksText : "";
        }

        String remaining = remarksText.trim();
        Matcher matcher = TWR_SFC_VIS_PATTERN.matcher(remaining);

        while (matcher.find() && matcher.start() == 0) {
            processTowerSurfaceVisibility(matcher, remarks);
            remaining = remaining.substring(matcher.end()).trim();
            matcher = TWR_SFC_VIS_PATTERN.matcher(remaining);
        }

        return remaining;
    }

    /**
     * Process a single tower or surface visibility match.
     *
     * @param matcher the regex matcher positioned at a match
     * @param remarks the remarks builder to populate
     */
    private void processTowerSurfaceVisibility(Matcher matcher, NoaaMetarRemarks.Builder remarks) {
        try {
            String type = matcher.group("type");
            String distStr = matcher.group("dist");

            if (isValidDistanceString(distStr)) {
                Visibility visibility = parseVisibilityDistance(distStr);

                if (visibility != null) {
                    setVisibilityByType(type, visibility, distStr, remarks);
                } else {
                    LOGGER.warn("Failed to parse tower/surface visibility: {}", distStr);
                }
            } else {
                LOGGER.debug("Tower/Surface visibility missing distance, skipping");
            }

        } catch (IllegalArgumentException e) {
            String matchText = matcher.group(0);
            LOGGER.warn("Invalid tower/surface visibility in remarks: {}",
                    matchText.substring(0, Math.min(30, matchText.length())), e);
        }
    }

    /**
     * Check if distance string is valid (non-null and non-blank).
     *
     * @param distStr the distance string to validate
     * @return true if valid
     */
    private boolean isValidDistanceString(String distStr) {
        return distStr != null && !distStr.isBlank();
    }

    /**
     * Set the appropriate visibility field based on type.
     *
     * @param type the visibility type (TWR VIS or SFC VIS)
     * @param visibility the parsed visibility
     * @param distStr the original distance string (for logging)
     * @param remarks the remarks builder to populate
     */
    private void setVisibilityByType(String type, Visibility visibility, String distStr,
                                     NoaaMetarRemarks.Builder remarks) {
        if ("TWR VIS".equals(type)) {
            remarks.towerVisibility(visibility);
            LOGGER.debug("Tower visibility: {}", distStr);
        } else if ("SFC VIS".equals(type)) {
            remarks.surfaceVisibility(visibility);
            LOGGER.debug("Surface visibility: {}", distStr);
        }
    }

    /**
     * Handle hourly precipitation amount remark.
     *
     * Format: P0015 = 0.15 inches in last hour
     * - P = Hourly precipitation indicator
     * - 4 digits = Amount in hundredths of inches
     * - //// = Trace precipitation
     *
     * Examples:
     * - P0015 → 0.15 inches
     * - P0009 → 0.09 inches
     * - P//// → Trace
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleHourlyPrecipitationSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank

        Matcher matcher = PRECIP_1HR_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        try {
            String precipStr = matcher.group("precip");

            if (precipStr == null || precipStr.isBlank()) {
                LOGGER.debug("Hourly precipitation missing value, skipping");
                return remarksText.substring(matcher.end()).trim();
            }

            PrecipitationAmount precip = PrecipitationAmount.fromEncoded(precipStr, 1);
            remarks.hourlyPrecipitation(precip);

            LOGGER.debug("Hourly precipitation: {}",
                    precip.isTrace() ? "trace" : String.format("%.2f inches", precip.inches()));

            return remarksText.substring(matcher.end()).trim();

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid hourly precipitation: {}",
                    remarksText.substring(0, Math.min(20, remarksText.length())), e);
            return remarksText.substring(matcher.end()).trim();
        }
    }

    /**
     * Handle 6-hour or 24-hour precipitation amount remark.
     *
     * Format: 60009 (6-hour) or 70125 (24-hour)
     * - 6 = 6-hour precipitation indicator
     * - 7 = 24-hour precipitation indicator
     * - 4-5 digits = Amount in hundredths of inches
     * - //// or ///// = Trace precipitation
     *
     * Examples:
     * - 60009 → 0.09 inches (6-hour)
     * - 70125 → 1.25 inches (24-hour)
     * - 6//// → Trace (6-hour)
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleMultiHourPrecipitationSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank

        Matcher matcher = PRECIP_3HR_24HR_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        try {
            String type = matcher.group("type");
            String precipStr = matcher.group("precip");

            if (precipStr == null || precipStr.isBlank()) {
                LOGGER.debug("Multi-hour precipitation missing value, skipping");
                return remarksText.substring(matcher.end()).trim();
            }

            // Determine period: 6 = 6-hour, 7 = 24-hour
            int periodHours = "6".equals(type) ? 6 : 24;

            PrecipitationAmount precip = PrecipitationAmount.fromEncoded(precipStr, periodHours);

            if (periodHours == 6) {
                remarks.sixHourPrecipitation(precip);
                LOGGER.debug("6-hour precipitation: {}", precip.getDescription());
            } else {
                remarks.twentyFourHourPrecipitation(precip);
                LOGGER.debug("24-hour precipitation: {}", precip.getDescription());
            }

            return remarksText.substring(matcher.end()).trim();

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid multi-hour precipitation: {}",
                    remarksText.substring(0, Math.min(20, remarksText.length())), e);
            return remarksText.substring(matcher.end()).trim();
        }
    }

    /**
     * Handle hail size remark for sequential parsing.
     *
     * Format: GR followed by size in inches
     * Examples:
     * - GR 1/2 → 0.5 inch hail
     * - GR 1 3/4 → 1.75 inch hail
     * - GR 2 → 2 inch hail
     *
     * Uses parseVisibilityDistance() to parse the size value since
     * the format is identical (fractions, mixed numbers, whole numbers).
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleHailSizeSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank

        Matcher matcher = HAIL_SIZE_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        try {
            String sizeStr = matcher.group("size");

            if (sizeStr == null || sizeStr.isBlank()) {
                LOGGER.debug("Hail size missing value, skipping");
                return remarksText.substring(matcher.end()).trim();
            }

            // Reuse parseVisibilityDistance - same format!
            Visibility visibility = parseVisibilityDistance(sizeStr);

            if (visibility == null) {
                LOGGER.warn("Failed to parse hail size: {}", sizeStr);
                return remarksText.substring(matcher.end()).trim();
            }

            // Convert visibility to hail size
            Double sizeInches = visibility.toStatuteMiles();
            if (sizeInches == null) {
                LOGGER.warn("Hail size conversion failed: {}", sizeStr);
                return remarksText.substring(matcher.end()).trim();
            }

            HailSize hailSize = HailSize.inches(sizeInches);
            remarks.hailSize(hailSize);

            LOGGER.debug("Hail size: {} inches ({})",
                    sizeInches, hailSize.getSizeCategory());

            return remarksText.substring(matcher.end()).trim();

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid hail size: {}",
                    remarksText.substring(0, Math.min(20, remarksText.length())), e);
            return remarksText.substring(matcher.end()).trim();
        }
    }

    /**
     * Handle weather begin/end time events for sequential parsing.
     *
     * Uses the existing BEGIN_END_WEATHER_PATTERN which captures:
     * - Intensity: int, int2
     * - Descriptor: desc (MI, PR, BC, DR, BL, SH, TS, FZ)
     * - Precipitation: prec (DZ, RA, SN, SG, IC, PL, GR, GS, UP)
     * - Obscuration: obsc (BR, FG, FU, VA, DU, SA, HZ, PY)
     * - Other: other (PO, SQ, FC, SS, DS, NSW)
     * - Begin time: begin (B marker), begint (time digits)
     * - End time: end (E marker), endt (time digits)
     *
     * Time format:
     * - 2 digits (05) → minute only (:05)
     * - 4 digits (1159) → hour and minute (11:59)
     *
     * Examples:
     * - RAB05 → Rain began at :05
     * - FZRAB1159E1240 → Freezing rain began 11:59, ended 12:40
     * - RAB15E30SNB30 → Rain began :15 ended :30; Snow began :30 (parsed as 2 events)
     * - -RAB05 → Light rain began :05
     * - +TSRAB20E45 → Heavy thunderstorm with rain began :20, ended :45
     *
     * Multiple events can be chained together (e.g., RAB15E30SNB30 contains two events).
     * This method will parse all chained events in a single pass.
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after all chained events are processed
     */
    private String handleWeatherEventsSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText;
        }

        String remaining = remarksText;
        Matcher matcher = BEGIN_END_WEATHER_PATTERN.matcher(remaining);

        // Process all chained events
        while (matcher.find() && matcher.start() == 0) {
            try {
                WeatherEvent event = parseWeatherEventFromExistingPattern(matcher);

                if (event != null) {
                    remarks.addWeatherEvent(event);
                    LOGGER.debug("Weather event: {}", event.getSummary());

                    // Remove matched portion and continue
                    remaining = remaining.substring(matcher.end()).trim();
                    matcher = BEGIN_END_WEATHER_PATTERN.matcher(remaining);
                } else {
                    // No valid event parsed, stop processing
                    break;
                }

            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid weather event: {}",
                        remaining.substring(0, Math.min(30, remaining.length())), e);
                // Skip this match and continue
                remaining = remaining.substring(matcher.end()).trim();
                matcher = BEGIN_END_WEATHER_PATTERN.matcher(remaining);
            }
        }

        return remaining;
    }

    /**
     * Parse a single weather event from the existing BEGIN_END_WEATHER_PATTERN matcher.
     *
     * Extracts data from your existing capture groups:
     * - int, int2: intensity markers
     * - desc: descriptor (TS, FZ, etc.)
     * - prec: precipitation (RA, SN, etc.)
     * - obsc: obscuration (BR, FG, etc.)
     * - begin, begint: begin marker and time
     * - end, endt: end marker and time
     *
     * @param matcher the matcher positioned at a weather event
     * @return WeatherEvent object, or null if the match doesn't represent a valid event
     */
    private WeatherEvent parseWeatherEventFromExistingPattern(Matcher matcher) {
        // Extract intensity - prefer int2 (at end), fallback to int (at start)
        String intensityEnd = matcher.group("int2");
        String intensityStart = matcher.group("int");
        String intensity = null;

        if (intensityEnd != null && intensityEnd.matches("[-+]")) {
            intensity = intensityEnd;
        } else if (intensityStart != null && intensityStart.matches("[-+]")) {
            intensity = intensityStart;
        }

        // Extract weather components
        String descriptor = matcher.group("desc");
        String precipitation = matcher.group("prec");
        String obscuration = matcher.group("obsc");
        String other = matcher.group("other");

        // Build weather code from available components
        String weatherCode = buildWeatherCodeFromExistingGroups(
                descriptor, precipitation, obscuration, other
        );

        // If no weather code components, this isn't a valid weather event
        if (weatherCode.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No weather code found in match: {}", matcher.group(0));
            }
            return null;
        }

        // Extract begin time
        String beginMarker = matcher.group("begin");
        String beginTimeStr = matcher.group("begint");

        Integer beginHour = null;
        Integer beginMinute = null;

        if (beginMarker != null && beginTimeStr != null) {
            TimeComponents beginTime = parseTimeDigits(beginTimeStr);
            beginHour = beginTime.hour();
            beginMinute = beginTime.minute();
        }

        // Extract end time
        String endMarker = matcher.group("end");
        String endTimeStr = matcher.group("endt");

        Integer endHour = null;
        Integer endMinute = null;

        if (endMarker != null && endTimeStr != null) {
            TimeComponents endTime = parseTimeDigits(endTimeStr);
            endHour = endTime.hour();
            endMinute = endTime.minute();
        }

        // Must have at least a begin or end time
        if (beginMinute == null && endMinute == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No begin or end time found in weather event: {}", matcher.group(0));
            }
            return null;
        }

        return new WeatherEvent(
                weatherCode,
                intensity,
                beginHour,
                beginMinute,
                endHour,
                endMinute
        );
    }

    /**
     * Helper record to return hour and minute components.
     */
    private record TimeComponents(Integer hour, Integer minute) {}

    /**
     * Parse time digits which can be either 2 digits (mm) or 4 digits (hhmm).
     *
     * Format:
     * - 2 digits (05) → hour=null, minute=5
     * - 4 digits (1159) → hour=11, minute=59
     *
     * @param timeStr the time string (2 or 4 digits)
     * @return TimeComponents with hour (if 4 digits) and minute
     */
    private TimeComponents parseTimeDigits(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return new TimeComponents(null, null);
        }

        try {
            if (timeStr.length() == 2) {
                // 2 digits: minute only (e.g., "05" = :05)
                int minute = Integer.parseInt(timeStr);
                return new TimeComponents(null, minute);

            } else if (timeStr.length() == 4) {
                // 4 digits: hour and minute (e.g., "1159" = 11:59)
                int hour = Integer.parseInt(timeStr.substring(0, 2));
                int minute = Integer.parseInt(timeStr.substring(2, 4));
                return new TimeComponents(hour, minute);

            } else {
                LOGGER.warn("Invalid time digit length: {}", timeStr);
                return new TimeComponents(null, null);
            }

        } catch (NumberFormatException e) {
            LOGGER.warn("Failed to parse time digits '{}': {}", timeStr, e.getMessage());
            return new TimeComponents(null, null);
        }
    }

    /**
     * Build the weather code from your existing pattern's capture groups.
     *
     * Combines descriptor, precipitation, obscuration, and other components
     * in the order they appear in the pattern.
     *
     * Weather codes can be:
     * - Just descriptor (e.g., "TS" for thunderstorm)
     * - Descriptor + precipitation (e.g., "FZRA", "TSRA")
     * - Just precipitation (e.g., "RA", "SN")
     * - Precipitation + obscuration (e.g., "RABR")
     * - Just obscuration (e.g., "BR", "FG")
     *
     * @param descriptor Weather descriptor (may be null)
     * @param precipitation Weather precipitation (may be null)
     * @param obscuration Weather obscuration (may be null)
     * @param other Other weather phenomena (may be null)
     * @return Combined weather code, or empty string if all are null/empty
     */
    private String buildWeatherCodeFromExistingGroups(
            String descriptor, String precipitation, String obscuration, String other) {

        StringBuilder code = new StringBuilder();

        // Add components in order
        if (descriptor != null && !descriptor.isEmpty()) {
            code.append(descriptor);
        }

        if (precipitation != null && !precipitation.isEmpty() && !precipitation.equals("/")) {
            code.append(precipitation);
        }

        if (obscuration != null && !obscuration.isEmpty()) {
            code.append(obscuration);
        }

        if (other != null && !other.isEmpty() && !other.equals("/") && other.matches("PO|SQ|FC|SS|DS|NSW")) {
            code.append(other);
        }

        return code.toString();
    }

    /**
     * Handle thunderstorm and cloud location remarks for sequential parsing.
     *
     * Uses the existing TS_CLD_LOC_PATTERN which captures:
     * - type: Cloud/phenomenon type (TS, CB, TCU, ACC, CBMAM, VIRGA)
     * - loc: Location qualifier (OHD, VC, DSNT, DSIPTD, TOP, TR)
     * - dir: Primary direction (N, NE, E, SE, S, SW, W, NW)
     * - dir2: Secondary direction for range (e.g., N-NE)
     * - dirm: Movement direction (if MOV present)
     *
     * Examples:
     * - TS SE → Thunderstorm Southeast
     * - CB OHD MOV E → Cumulonimbus Overhead Moving East
     * - TCU DSNT N-NE → Towering Cumulus Distant North to Northeast
     *
     * Multiple occurrences can be present (e.g., "TS SE CB W").
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after all cloud locations are processed
     */
    private String handleThunderstormLocationSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText;
        }

        String remaining = remarksText;
        Matcher matcher = TS_CLD_LOC_PATTERN.matcher(remaining);

        // Process all cloud locations
        while (matcher.find() && matcher.start() == 0) {
            int matchEnd = matcher.end();

            try {
                ThunderstormLocation location = parseThunderstormLocationFromMatcher(matcher);
                remarks.addThunderstormLocation(location);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Thunderstorm/Cloud location: {}", location.getSummary());
                }

                // SUCCESS: advance
                remaining = remaining.substring(matchEnd).trim();
                matcher = TS_CLD_LOC_PATTERN.matcher(remaining);

            } catch (RuntimeException e) {
                // Handle unexpected errors (indicates parser or regex bug)
                String matchedText = remaining.substring(0, Math.min(matchEnd, remaining.length()));
                LOGGER.error("Unexpected error parsing thunderstorm location from '{}'. " +
                                "This may indicate a bug in the regex or parser logic.",
                        matchedText, e);

                // FAILURE: Skip this match and continue processing
                remaining = remaining.substring(matchEnd).trim();
                matcher = TS_CLD_LOC_PATTERN.matcher(remaining);
            }
        }

        return remaining;
    }

    /**
     * Parse ThunderstormLocation from regex matcher.
     *
     * Extracts all captured groups from TS_CLD_LOC_PATTERN and creates
     * a ThunderstormLocation object.
     *
     * @param matcher the regex matcher positioned at a thunderstorm/cloud location
     * @return ThunderstormLocation object with all extracted fields
     */
    private ThunderstormLocation parseThunderstormLocationFromMatcher(Matcher matcher) {
        String cloudType = matcher.group("type");
        String locationQualifier = matcher.group("loc");
        String direction = matcher.group("dir");
        String directionRange = matcher.group("dir2");
        String movingDirection = matcher.group("dirm");

        return new ThunderstormLocation(
                cloudType,
                locationQualifier,
                direction,
                directionRange,
                movingDirection
        );
    }

    /**
     * Handle 3-hour pressure tendency remark.
     *
     * <p>Format: 5TCCC where:
     * <ul>
     *   <li>5 = Indicator (always 5)</li>
     *   <li>T = Tendency code (0-8, WMO Code 0200)</li>
     *   <li>CCC = Pressure change in tenths of hPa (3 digits)</li>
     * </ul>
     *
     * <p>Examples:
     * <ul>
     *   <li>52032 → Increasing then steady, +3.2 hPa</li>
     *   <li>57045 → Decreasing steadily, -4.5 hPa</li>
     *   <li>54000 → Steady, 0.0 hPa change</li>
     * </ul>
     *
     * <p>Note: This remark appears at most once per METAR.
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after pressure tendency is processed
     */
    private String handlePressureTendencySequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText;
        }

        String remaining = remarksText.trim();
        Matcher matcher = PRESS_3HR_PATTERN.matcher(remaining);

        // Only process first match (single occurrence)
        if (matcher.find() && matcher.start() == 0) {
            int matchEnd = matcher.end();

            try {
                // Extract pressure tendency from matcher
                PressureTendency tendency = parsePressureTendencyFromMatcher(matcher);

                // Add to builder
                remarks.pressureTendency(tendency);

                // Log success
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("3-hour pressure tendency: {}", tendency.getSummary());
                }

                // SUCCESS: Move past this match
                remaining = remaining.substring(matchEnd).trim();
            } catch (IllegalArgumentException e) {
                // Handle both parsing and validation errors
                String matchedText = remaining.substring(0, Math.min(matchEnd, remaining.length()));
                LOGGER.warn("Error parsing pressure tendency from '{}': {}",
                        matchedText, e.getMessage());

                remaining = remaining.substring(matchEnd).trim();
            }
        }

        return remaining;
    }

    /**
     * Helper method to parse PressureTendency from a Matcher.
     *
     * @param matcher the matcher with a successful match
     * @return a new PressureTendency instance
     * @throws NumberFormatException if tendency code or pressure cannot be parsed
     */
    private PressureTendency parsePressureTendencyFromMatcher(Matcher matcher) {
        String tendencyCodeStr = matcher.group(GROUP_TENDENCY_CODE);
        String pressureChangeStr = matcher.group(GROUP_PRESSURE_CHANGE);

        int tendencyCode = Integer.parseInt(tendencyCodeStr);

        // Use PressureTendency.fromMetar factory method
        return PressureTendency.fromMetar(tendencyCode, pressureChangeStr);
    }

    /**
     * Handle 6-hour maximum/minimum temperature.
     * Format: 1sTTT (max) or 2sTTT (min)
     * where s=sign (0=positive, 1=negative), TTT=temp in tenths of degrees C.
     *
     * Examples:
     * - 10142 → Maximum: 14.2°C
     * - 11023 → Maximum: -2.3°C
     * - 20012 → Minimum: 1.2°C
     * - 21001 → Minimum: -0.1°C
     *
     * @param remarksText remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after processing
     */
    private String handle6HourMaxMinTemperatureSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText;
        }

        String remaining = remarksText.trim();
        Matcher matcher = TEMP_6HR_MAX_MIN_PATTERN.matcher(remaining);

        // Process all matches (can have both max AND min in same METAR)
        while (matcher.find() && matcher.start() == 0) {
            int matchEnd = matcher.end();

            try {
                Temperature temperature = parse6HourTemperatureFromMatcher(matcher);
                String type = matcher.group("type");
                add6HourTemperatureToRemarks(type, temperature, remarks);

                // SUCCESS: Move past this match
                remaining = remaining.substring(matchEnd).trim();
                matcher = TEMP_6HR_MAX_MIN_PATTERN.matcher(remaining);

            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid 6-hour max/min temperature in remarks: {}",
                        remaining.substring(0, Math.min(matchEnd, remaining.length())), e);
                remaining = remaining.substring(matchEnd).trim();
                matcher = TEMP_6HR_MAX_MIN_PATTERN.matcher(remaining);
            }
        }

        return remaining;
    }

    /**
     * Parse temperature from 6-hour max/min pattern matcher.
     *
     * @param matcher the regex matcher with captured groups
     * @return Temperature object
     * @throws IllegalArgumentException if parsing fails
     */
    private Temperature parse6HourTemperatureFromMatcher(Matcher matcher) {
        String sign = matcher.group("sign");
        String tempStr = matcher.group("temp");

        // Parse temperature value (in tenths of degrees)
        int tempTenths = Integer.parseInt(tempStr);
        double tempCelsius = tempTenths / 10.0;

        // Apply sign (0 = positive, 1 = negative)
        if ("1".equals(sign)) {
            tempCelsius = -tempCelsius;
        }

        return Temperature.of(tempCelsius);
    }

    /**
     * Add parsed 6-hour temperature to appropriate remarks field.
     *
     * @param type temperature type ("1" = max, "2" = min)
     * @param temperature the parsed temperature
     * @param remarks the remarks builder to populate
     */
    private void add6HourTemperatureToRemarks(String type, Temperature temperature, NoaaMetarRemarks.Builder remarks) {
        if ("1".equals(type)) {
            // Type 1 = Maximum temperature
            remarks.sixHourMaxTemperature(temperature);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("6-hour maximum temperature: {}", temperature.getSummary());
            }
        } else if ("2".equals(type)) {
            // Type 2 = Minimum temperature
            remarks.sixHourMinTemperature(temperature);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("6-hour minimum temperature: {}", temperature.getSummary());
            }
        }
    }

    /**
     * Handle 24-hour maximum/minimum temperature.
     * Format: 4sTTTsTTT where s=sign (0=positive, 1=negative), TTT=temp in tenths °C
     * Reported at midnight local standard time.
     *
     * Examples:
     * - 400461006 → Max: 4.6°C, Min: -0.6°C
     * - 411231089 → Max: -12.3°C, Min: -8.9°C
     * - 400001000 → Max: 0.0°C, Min: -0.0°C
     *
     * @param remarksText remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after processing
     */
    private String handle24HourMaxMinTemperatureSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText;
        }

        String remaining = remarksText.trim();
        Matcher matcher = TEMP_24HR_PATTERN.matcher(remaining);

        // Only process first match (single occurrence per METAR)
        if (matcher.find() && matcher.start() == 0) {
            int matchEnd = matcher.end();

            try {
                // Parse both temperatures from the single code
                Temperature maxTemp = parse24HourMaxTemperatureFromMatcher(matcher);
                Temperature minTemp = parse24HourMinTemperatureFromMatcher(matcher);

                // Add to builder
                remarks.twentyFourHourMaxTemperature(maxTemp);
                remarks.twentyFourHourMinTemperature(minTemp);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("24-hour max/min temperature: {} / {}",
                            maxTemp.getSummary(), minTemp.getSummary());
                }

                // SUCCESS: Move past this match
                remaining = remaining.substring(matchEnd).trim();

            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid 24-hour max/min temperature in remarks: {}",
                        remaining.substring(0, Math.min(matchEnd, remaining.length())), e);
                remaining = remaining.substring(matchEnd).trim();
            }
        }

        return remaining;
    }

    /**
     * Parse 24-hour maximum temperature from matcher.
     *
     * @param matcher the regex matcher with captured groups
     * @return Temperature object for maximum
     * @throws IllegalArgumentException if parsing fails
     */
    private Temperature parse24HourMaxTemperatureFromMatcher(Matcher matcher) {
        String maxSign = matcher.group("maxsign");
        String maxTempStr = matcher.group("maxtemp");

        // Parse temperature value (in tenths of degrees)
        int maxTempTenths = Integer.parseInt(maxTempStr);
        double maxTempCelsius = maxTempTenths / 10.0;

        // Apply sign (0 = positive, 1 = negative)
        if ("1".equals(maxSign)) {
            maxTempCelsius = -maxTempCelsius;
        }

        return Temperature.of(maxTempCelsius);
    }

    /**
     * Parse 24-hour minimum temperature from matcher.
     *
     * @param matcher the regex matcher with captured groups
     * @return Temperature object for minimum
     * @throws IllegalArgumentException if parsing fails
     */
    private Temperature parse24HourMinTemperatureFromMatcher(Matcher matcher) {
        String minSign = matcher.group("minsign");
        String minTempStr = matcher.group("mintemp");

        // Parse temperature value (in tenths of degrees)
        int minTempTenths = Integer.parseInt(minTempStr);
        double minTempCelsius = minTempTenths / 10.0;

        // Apply sign (0 = positive, 1 = negative)
        if ("1".equals(minSign)) {
            minTempCelsius = -minTempCelsius;
        }

        return Temperature.of(minTempCelsius);
    }

    /**
     * Handle variable ceiling.
     * Format: CIG minVmax where values are in hundreds of feet.
     *
     * Examples:
     * - CIG 005V010 → 500-1000 feet
     * - CIG 020V035 → 2000-3500 feet
     *
     * @param remarksText remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after processing
     */
    private String handleVariableCeilingSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText;
        }

        String remaining = remarksText.trim();
        Matcher matcher = VARIABLE_CEILING_PATTERN.matcher(remaining);

        if (matcher.find() && matcher.start() == 0) {
            int matchEnd = matcher.end();

            try {
                String minStr = matcher.group("min");
                String maxStr = matcher.group("max");

                int minHundreds = Integer.parseInt(minStr);
                int maxHundreds = Integer.parseInt(maxStr);

                VariableCeiling variableCeiling = VariableCeiling.fromHundreds(minHundreds, maxHundreds);
                remarks.variableCeiling(variableCeiling);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Variable ceiling: {}", variableCeiling.getSummary());
                }

                remaining = remaining.substring(matchEnd).trim();

            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid variable ceiling in remarks: {}",
                        remaining.substring(0, Math.min(matchEnd, remaining.length())), e);
                remaining = remaining.substring(matchEnd).trim();
            }
        }

        return remaining;
    }

    /**
     * Handle ceiling height at second site.
     * Format: CIG height [LOC] where height is in hundreds of feet.
     *
     * Examples:
     * - CIG 002 RY11 → 200 ft at runway 11
     * - CIG 005 RWY06 → 500 ft at runway 06
     * - CIG 010 → 1000 ft (no location)
     *
     * IMPORTANT: This handler must be called AFTER handleVariableCeilingSequential
     * to avoid matching variable ceiling patterns (e.g., CIG 005V010).
     *
     * @param remarksText remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after processing
     */
    private String handleCeilingSecondSiteSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText;
        }

        String remaining = remarksText.trim();
        Matcher matcher = CEILING_SECOND_SITE_PATTERN.matcher(remaining);

        if (matcher.find() && matcher.start() == 0) {
            int matchEnd = matcher.end();

            try {
                String heightStr = matcher.group(GROUP_HEIGHT_CODE);
                String location = matcher.group("loc");

                int hundreds = Integer.parseInt(heightStr);

                CeilingSecondSite ceilingSecondSite = CeilingSecondSite.fromHundreds(hundreds, location);
                remarks.ceilingSecondSite(ceilingSecondSite);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Ceiling second site: {}", ceilingSecondSite.getSummary());
                }

                remaining = remaining.substring(matchEnd).trim();

            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid ceiling second site in remarks: {}",
                        remaining.substring(0, Math.min(matchEnd, remaining.length())), e);
                remaining = remaining.substring(matchEnd).trim();
            }
        }

        return remaining;
    }

    /**
     * Handle obscuration layers.
     * Format: [Coverage] [Phenomenon] [Height]
     *
     * Examples:
     * - FEW FG 000 → Few fog at ground level
     * - SCT FU 010 → Scattered smoke at 1000 feet
     *
     * Multiple layers can be present: FEW FG 000 SCT FU 010
     *
     * @param remarksText remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after processing
     */
    private String handleObscurationSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText;
        }

        String remaining = remarksText.trim();
        Matcher matcher = OBSCURATION_PATTERN.matcher(remaining);

        // Process all obscuration layers (repeating pattern)
        while (matcher.find() && matcher.start() == 0) {
            int matchEnd = matcher.end();

            try {
                String coverage = matcher.group("coverage");
                String phenomenon = matcher.group("phenomenon");
                String heightStr = matcher.group(GROUP_HEIGHT_CODE);

                int hundreds = Integer.parseInt(heightStr);

                ObscurationLayer layer = ObscurationLayer.fromHundreds(coverage, phenomenon, hundreds);
                remarks.addObscurationLayer(layer);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Obscuration layer: {}", layer.getSummary());
                }

                remaining = remaining.substring(matchEnd).trim();
                matcher = OBSCURATION_PATTERN.matcher(remaining);

            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid obscuration layer in remarks: {}",
                        remaining.substring(0, Math.min(matchEnd, remaining.length())), e);
                remaining = remaining.substring(matchEnd).trim();
                matcher = OBSCURATION_PATTERN.matcher(remaining);
            }
        }

        return remaining;
    }

    /**
     * Handle cloud type observations in okta format.
     * Format: [Intensity] CloudType [Oktas] [Location/Movement]
     *
     * Examples:
     * - SC1 → Stratocumulus 1 okta
     * - SC TR → Stratocumulus trace
     * - MDT CU OHD → Moderate cumulus overhead
     * - CI MOVG NE → Cirrus moving northeast
     *
     * Multiple cloud types can be present: SC1 AC2 CI
     *
     * @param remarksText remaining remarks text to process
     * @param remarks the remarks builder to populate
     * @return the remaining text after processing (never null)
     */
    private String handleCloudTypeSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText != null ? remarksText : "";
        }

        String remaining = remarksText.trim();
        Matcher matcher = CLOUD_OKTA_PATTERN.matcher(remaining);

        // Process all cloud type observations (repeating pattern)
        while (matcher.find() && matcher.start() == 0) {
            int matchEnd = matcher.end();
            remaining = processCloudTypeMatch(matcher, matchEnd, remaining, remarks);

            // IMPORTANT: Trim again before next iteration!
            remaining = remaining.trim();
            matcher = CLOUD_OKTA_PATTERN.matcher(remaining);
        }

        return remaining;
    }

    /**
     * Process a single cloud type pattern match.
     *
     * @param matcher the pattern matcher (must not be null)
     * @param matchEnd the end position of the match
     * @param remaining the remaining text (must not be null)
     * @param remarks the remarks builder (must not be null)
     * @return the remaining text after processing this match (never null)
     */
    private String processCloudTypeMatch(Matcher matcher, int matchEnd, String remaining,
                                         NoaaMetarRemarks.Builder remarks) {
        try {
            CloudType cloudType = extractCloudTypeFromMatcher(matcher);
            remarks.addCloudType(cloudType);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Cloud type: {}", cloudType.getSummary());
            }

        } catch (IllegalArgumentException e) {
            int endIndex = Math.min(matchEnd, remaining.length());
            LOGGER.warn("Invalid cloud type in remarks: {}",
                    remaining.substring(0, endIndex), e);
        }

        // Always return remaining text (never null)
        return remaining.substring(matchEnd).trim();
    }

    /**
     * Extract cloud type components from a pattern matcher.
     * Validates that at least one optional field (oktas, intensity, location, movement) is present.
     *
     * @param matcher the pattern matcher (must not be null)
     * @return the constructed CloudType (never null)
     * @throws IllegalArgumentException if cloud type data is invalid or no optional fields present
     */
    private CloudType extractCloudTypeFromMatcher(Matcher matcher) {
        String intensity = matcher.group("intensity");
        String cloudTypeCode = matcher.group("cloud");
        String oktaStr = matcher.group("okta");
        String verb = matcher.group("verb");
        String directionMovement = matcher.group("dirm");
        String directionLocation = matcher.group("direction");

        Integer oktas = parseOktas(oktaStr);
        String[] locationAndMovement = determineLocationAndMovement(verb, directionMovement, directionLocation);

        String location = locationAndMovement[0];
        String movement = locationAndMovement[1];
        String trimmedIntensity = trimOrNull(intensity);

        // VALIDATION: At least one optional field must be present
        // This prevents matching "SC HEAVY" or "SC XXX" as valid cloud types
        // where HEAVY/XXX are not recognized qualifiers
        boolean hasValidOptionalField = (oktas != null) ||
                (trimmedIntensity != null) ||
                (location != null) ||
                (movement != null);

        if (!hasValidOptionalField) {
            // Cloud type alone is not valid - must have at least one qualifier
            throw new IllegalArgumentException("Cloud type must have at least one optional field (oktas, intensity, location, or movement): " + cloudTypeCode);
        }

        return new CloudType(
                cloudTypeCode,
                oktas,
                trimmedIntensity,
                location,
                movement
        );
    }

    /**
     * Parse oktas string to Integer.
     *
     * @param oktaStr the oktas string from the pattern (may be null)
     * @return the parsed Integer or null if not present
     */
    private Integer parseOktas(String oktaStr) {
        if (oktaStr != null && !oktaStr.isEmpty()) {
            return Integer.parseInt(oktaStr);
        }
        return null;
    }

    /**
     * Determine location and movement from pattern groups.
     *
     * @param verb the verb group (may be null)
     * @param directionMovement the movement direction (may be null)
     * @param directionLocation the location qualifier (may be null)
     * @return array with [location, movement] - never null, but elements may be null
     */
    private String[] determineLocationAndMovement(String verb, String directionMovement, String directionLocation) {
        String location = null;
        String movement = null;

        if ("MOVG".equals(verb)) {
            movement = directionMovement;
        } else if (directionLocation != null) {
            location = directionLocation;
        }

        return new String[]{location, movement};
    }

    /**
     * Trim a string or return null if it's null or blank.
     *
     * @param str the string to trim (may be null)
     * @return the trimmed string or null if blank/null
     */
    private String trimOrNull(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        return str.trim();
    }

    /**
     * Handle automated maintenance indicators sequentially.
     * Handles: RVRNO, PWINO, PNO, FZRANO, TSNO, VISNO [LOC], CHINO [LOC], $
     *
     * @param remarksText the remaining remarks text
     * @param remarks the remarks builder
     * @return remaining text after processing
     */
    private String handleAutomatedMaintenanceSequential(String remarksText,
                                                        NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText != null ? remarksText : "";
        }

        String remaining = remarksText.trim();
        Matcher matcher = AUTOMATED_MAINTENANCE_PATTERN.matcher(remaining);

        while (matcher.find() && matcher.start() == 0) {
            processAutomatedMaintenance(matcher, remarks);
            remaining = remaining.substring(matcher.end()).trim();
            matcher = AUTOMATED_MAINTENANCE_PATTERN.matcher(remaining);
        }

        return remaining;
    }

    /**
     * Process a single automated maintenance indicator match.
     *
     * @param matcher the regex matcher positioned at a match
     * @param remarks the remarks builder to populate
     */
    private void processAutomatedMaintenance(Matcher matcher, NoaaMetarRemarks.Builder remarks) {
        try {
            String typeAM = matcher.group("typeam");  // Automated maintenance type
            String typeMC = matcher.group("typemc");  // Maintenance check ($)
            String location = matcher.group("loc");   // Optional location

            if (typeMC != null) {
                // Maintenance check indicator ($)
                remarks.maintenanceRequired(true);
                remarks.addAutomatedMaintenanceIndicator(AutomatedMaintenanceIndicator.maintenanceCheck());
                LOGGER.debug("Maintenance check indicator ($) found");
            } else if (typeAM != null) {
                // Automated maintenance type (RVRNO, PWINO, etc.)
                AutomatedMaintenanceIndicator indicator =
                        location != null
                                ? AutomatedMaintenanceIndicator.of(typeAM, location)
                                : AutomatedMaintenanceIndicator.of(typeAM);

                remarks.addAutomatedMaintenanceIndicator(indicator);
                LOGGER.debug("Automated maintenance indicator: {} {}",
                        typeAM,
                        location != null ? location : "(no location)");
            }

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid automated maintenance indicator: {}", matcher.group(0), e);
        }
    }

    // ==================== STUB HANDLERS (to be implemented) ====================

    private void handleUnparsed(Matcher matcher) {
        LOGGER.debug("Unparsed token: '{}'", matcher);
    }
}
