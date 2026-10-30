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

import weather.exception.InvalidObservationException;
import weather.model.NoaaMetarData;
import weather.model.NoaaWeatherData;
import weather.model.components.*;
import weather.model.components.remark.*;
import weather.model.components.remark.ceilingremarks.CeilingSecondSite;
import weather.model.components.remark.ceilingremarks.VariableCeiling;
import weather.model.components.remark.maintenanceremarks.AutomatedMaintenanceIndicator;
import weather.model.components.remark.pressureremarks.PressureRapidChange;
import weather.model.components.remark.pressureremarks.PressureTendency;
import weather.model.components.remark.visibilityremarks.VariableVisibility;
import weather.model.components.remark.windremarks.PeakWind;
import weather.model.components.remark.windremarks.WindAtLocation;
import weather.model.components.remark.windremarks.WindShift;
import weather.model.enums.AutomatedStationType;
import weather.model.enums.HighCloudType;
import weather.model.enums.LowCloudType;
import weather.model.enums.MiddleCloudType;
import weather.processing.parser.common.ParseResult;
import weather.utils.IndexedLinkedHashMap;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static weather.processing.parser.noaa.RegExprConst.*;

/**
 * Parser for NOAA METAR (Meteorological Aerodrome Report) data.
 * <p>
 * Extends NoaaAviationWeatherParser to inherit shared aviation weather parsing logic
 * for wind, visibility, present weather, sky conditions, and RVR.
 * <p>
 * METAR Format Example:
 * "2025/11/14 22:52
 * METAR KJFK 142252Z 19005KT 10SM FEW100 FEW250 16/M03 A3012 RMK AO2 SLP214 T01611028"
 * <p>
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
 *
 */
public class NoaaMetarParser extends NoaaAviationWeatherParser<NoaaMetarData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoaaMetarParser.class);

    // Pressure Tendency Pattern Groups
    private static final String GROUP_TENDENCY_CODE = "tend";
    private static final String GROUP_HEIGHT_CODE = "height";

    private static final String DIRECTION_CHAIN_CODE = "dirchain";

    // Splits a direction chain into segments: segments are separated by
    // whitespace, optionally with the word AND (e.g. "N AND SW" or "N NE S SW")
    private static final Pattern SEGMENT_SEPARATOR_PATTERN = Pattern.compile("\\s+(?:AND\\s+)?");
    private static final Pattern RANGE_SEPARATOR_PATTERN = Pattern.compile("-");

    /**
     * Weather codes that are also valid thunderstorm/cloud-location type codes
     * (TS_CLD_LOC_PATTERN). When PRESENT_WEATHER_PATTERN matches one of these
     * bare (no intensity/precipitation/obscuration attached), the ambiguity is
     * resolved in favor of the more specific thunderstorm-location handler.
     */
    private static final Set<String> THUNDERSTORM_LOCATION_CODES =
            Set.of("TS", "CB", "TCU", "ACC", "CBMAM", "VIRGA");

    private static final Pattern REPORT_TYPE_PATTERN = Pattern.compile("^(METAR|SPECI)\\s+");

    /**
     * Main body: the registry's behavior (#153 phase 2). First matching handler
     * wins, rejected groups are dropped, and parsing stops at the first group no
     * handler recognizes; the rest becomes unparsedMainBody.
     */
    private static final SectionPolicy MAIN_BODY_POLICY = SectionPolicy.of(
            SectionPolicy.Scheduling.FIRST_MATCH_RESTART,
            SectionPolicy.RejectedText.CONSUME,
            SectionPolicy.UnrecognizedToken.STOP);

    // Main body groups, run through the shared group parsing engine
    private final SectionParser<NoaaMetarParser> mainBodyParser;
    // Pattern registry, still used for the remarks pass until #153 phase 3
    private final NoaaAviationWeatherPatternRegistry patternRegistry;
    // Sequential remark handlers, run per report through a RemarkHandlerChain
    private final List<RemarkHandler> sequentialRemarkHandlers;

    // METAR-specific state
    private String reportType;

    public NoaaMetarParser() {
        this.mainBodyParser = new SectionParser<>("METAR main body", createMainBodyHandlers(), MAIN_BODY_POLICY);
        this.patternRegistry = new NoaaAviationWeatherPatternRegistry();
        this.sequentialRemarkHandlers = createRemarkHandlers();
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
            // Store unparsed tokens from main body (similar to freeText in remarks)
            if (!mainBody.isBlank() && weatherData != null) {
                weatherData.setUnparsedMainBody(mainBody.trim());
            }

            remarks = parseRemarks(remarks);
            copyRemarksToTopLevel();

            validateParsedData();
            buildAndSetConditions();

            weatherData.setRawText(rawData.trim());

            logUnparsedTokens(mainBody, remarks);

            return ParseResult.success(weatherData);

        } catch (InvalidObservationException | IllegalStateException e) {
            // The report as a whole is unusable (e.g. no station): expected bad data, not a bug
            LOGGER.info("METAR rejected: {} [{}]", e.getMessage(), rawData.trim());
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
        @SuppressWarnings("java:S8786")
        String[] parts = token.split("\\s++RMK\\s++", 2);
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
        mainBody = parseIssueDateTime(mainBody);
        return mainBodyParser.parse(mainBody, this).remaining();
    }

    /**
     * Main body handlers in report order (#153 phase 2): the METAR header groups, the
     * condition groups shared with TAF, then temperature/dewpoint, altimeter and NOSIG.
     * <p>
     * Names and order are the pattern registry's, so parsed output and log text are
     * unchanged. The month/day/year and station/issue-time entries have never had a
     * handler: they consume a matching group without storing it, as before.
     *
     * @return the handlers, in order
     */
    private static List<GroupHandler<NoaaMetarParser>> createMainBodyHandlers() {
        List<GroupHandler<NoaaMetarParser>> handlers = new ArrayList<>(List.of(
                PatternGroupHandler.single("reportType", REPORT_TYPE_PATTERN, NoaaMetarParser::handleReportType),
                PatternGroupHandler.consumeOnly("monthDayYear", MONTH_DAY_YEAR_PATTERN),
                PatternGroupHandler.single("station", STATION_DAY_TIME_VALTMPER_PATTERN,
                        NoaaMetarParser::handleStationAndObsTime),
                PatternGroupHandler.consumeOnly("handleStationAndIssueTime", STATION_AND_ISSUE_TIME_PATTERN),
                PatternGroupHandler.single("reportModifier", REPORT_MODIFIER_PATTERN,
                        NoaaMetarParser::handleReportModifier)));
        handlers.addAll(conditionHandlers());
        handlers.addAll(List.of(
                PatternGroupHandler.single("tempDewpoint", TEMP_DEWPOINT_PATTERN, NoaaMetarParser::handleTempDewpoint),
                PatternGroupHandler.single("altimeter", ALTIMETER_PATTERN, NoaaMetarParser::handleAltimeter),
                PatternGroupHandler.single("noSigChange", NO_SIG_CHANGE_PATTERN, NoaaMetarParser::handleNoSigChange)));
        return handlers;
    }

    /**
     * Parse the remarks section (after RMK).
     *
     * @param remarks the remarks section
     * @return remaining unparsed tokens
     */
    private String parseRemarks(String remarks) {
        // No station means no weatherData; there is nothing to attach remarks to, and
        // validateParsedData() rejects the report with "Could not extract station ID"
        if (remarks.isEmpty() || weatherData == null) {
            return remarks;
        }

        String originalRemarks = remarks;
        IndexedLinkedHashMap<Pattern, NoaaAviationWeatherPatternHandler> remarkHandlers =
                patternRegistry.getRemarksHandlers();

        remarks = parseWithHandlers(remarks, remarkHandlers);

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
        // Fixed: Use \\S.* instead of .* to prevent ReDoS (require non-space after whitespace)
        // DOTALL so a header line followed by a newline-separated body still matches
        if (trimmed.matches("(?s)^\\d{4}/\\d{2}/\\d{2}\\s+\\S.*")) {
            return true;
        }

        // Check if METAR or SPECI appears at the start (not just anywhere)
        // Fixed: Use \\S.* instead of .* to prevent ReDoS
        if (trimmed.matches("(?s)^\\s*(METAR|SPECI)\\s+\\S.*")) {
            return true;
        }

        // Check if starts with ICAO station code + observation time (KCLT 062252Z format)
        // Fixed: Use \\S.* instead of .* to prevent ReDoS
        return trimmed.matches("(?s)^[A-Z][A-Z0-9]{2,3}\\s+\\d{6}Z\\s+\\S.*");
    }

    @Override
    public String getSourceType() {
        return "NOAA_METAR";
    }

    /**
     * Parse token string using ordered pattern handlers from registry.
     *
     * @param token    The token string to parse
     * @param handlers Ordered map of patterns and their handlers from NoaaAviationWeatherPatternRegistry
     * @return Remaining unparsed token string
     */
    private String parseWithHandlers(String token,
                                     IndexedLinkedHashMap<Pattern, NoaaAviationWeatherPatternHandler> handlers) {
        String currentToken = token;

        while (!currentToken.isEmpty()) {
            LOGGER.debug("\nREMARK - Processing token: '{}'", currentToken);

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
     * @param token    Current token to parse
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
     * @param token       Current token to parse
     * @param pattern     Pattern to try
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
     * Logs a remark group that matched its pattern but was rejected. The caller
     * leaves the text in place so it surfaces in freeText (#92, #115). Delegates to
     * the shared {@link #logRejected} so remarks and main body use one category (#136).
     */
    private static void logRejectedRemark(String remarkType, String matchedText, IllegalArgumentException e) {
        logRejected(remarkType, matchedText, e);
    }

    /**
     * Route a remark registry match to its handler by name. Only the remarks pass
     * uses this now; the main body runs through {@link SectionParser} (#153 phase 2),
     * and this method goes with the remark registry pass in phase 3.
     * <p>
     * Rejected data that reaches here is logged at DEBUG on the shared Rejected
     * category; any other exception is a bug, logged at ERROR with its stack trace.
     *
     * @param handlerName The name of the handler from NoaaAviationWeatherPatternRegistry
     * @param matcher     The regex matcher with captured groups
     */
    private void handlePattern(String handlerName, Matcher matcher) {
        try {
            switch (handlerName) {
                case "autoType" -> handleAutoType(matcher);
                case "seaLevelPressure" -> handleRemarkRegistry(matcher, "Sea level pressure");
                case "peakWind" -> handleRemarkRegistry(matcher, "Peak wind");
                case "windShift" -> handleRemarkRegistry(matcher, "Wind shift");
                case "variableVis" -> handleRemarkRegistry(matcher, "Variable visibility");
                case "precip1Hour" -> handleRemarkRegistry(matcher, "Hourly precipitation");
                case "precip3Hr24Hr" -> handleRemarkRegistry(matcher, "Multi-hour precipitation");
                case "hailSize" -> handleRemarkRegistry(matcher, "Hail size");

                default -> LOGGER.debug("No handler implemented for: {}", handlerName);
            }
        } catch (InvalidObservationException e) {
            // Rejected data a handler did not catch itself (e.g. Wind direction 400)
            logRejected(handlerName, matcher.group(0).trim(), e);
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected error in handler '{}' on '{}'", handlerName, matcher.group(0).trim(), e);
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
     * @param visualRange  Single value (null if variable)
     * @param variableLow  Variable low value (null if single)
     * @param variableHigh Variable high value (null if single)
     */
    private record RvrRange(Integer visualRange, Integer variableLow, Integer variableHigh) {
    }

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
     * Handle special RVR case (RVRNO).
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
        return false;
    }

    /**
     * Parse RVR values from matcher and add to weather data.
     *
     * @param matcher    The regex matcher
     * @param runwayName The runway identifier
     */
    private void parseAndAddRvr(Matcher matcher, String runwayName) {
        String lowValue = matcher.group("lvalue");
        if (lowValue == null) {
            LOGGER.warn("RVR: Missing low value for runway {}", runwayName);
            return;
        }

        // Parse low value (the pattern guarantees 1-4 digits)
        Integer visualRange = Integer.parseInt(lowValue);

        // Determine prefix
        String prefix = extractRvrPrefix(matcher.group("low"));

        // Parse variable range if present
        RvrRange range = parseVariableRange(matcher, visualRange);

        // Extract trend (slash form after FT, or bare letter after a metre value)
        String trend = extractRvrTrend(matcher.group("trend"), matcher.group("unit"), runwayName);

        // Create and add RVR
        createAndAddRvr(runwayName, range, prefix, trend, matcher.group(0).trim());
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
     * @param matcher  The regex matcher
     * @param lowValue The low value already parsed
     * @return RvrRange object with appropriate values
     */
    private RvrRange parseVariableRange(Matcher matcher, Integer lowValue) {
        String highStr = matcher.group("high");

        if (highStr == null) {
            // Not variable - return single value range
            return new RvrRange(lowValue, null, null);
        }

        // Variable range: parse high value (the pattern guarantees 4 digits)
        String highNumStr = highStr.replaceFirst("^[MP]", "");
        Integer highValue = Integer.parseInt(highNumStr);
        return new RvrRange(null, lowValue, highValue);
    }

    /**
     * Extract trend indicator (U/D/N).
     * The slash form ("1000FT/N") is captured in the trend group; the metre form
     * ("P1500N") puts the trend letter in the unit group.
     *
     * @param trendGroup The trend group from regex (slash form), may be null
     * @param unit       The unit group from regex
     * @param runwayName The runway identifier (for logging)
     * @return Trend indicator (N/D/U) or null
     */
    private String extractRvrTrend(String trendGroup, String unit, String runwayName) {
        if (trendGroup != null) {
            return trendGroup;
        }

        if (unit == null) {
            return null;
        }

        if ("N".equals(unit) || "D".equals(unit) || "U".equals(unit)) {
            return unit;
        }

        if ("FT".equals(unit)) {
            LOGGER.debug("RVR: Explicit FT unit for runway {}", runwayName);
        }

        return null;
    }

    /**
     * Create RunwayVisualRange object and add to weather data.
     *
     * @param runwayName The runway identifier
     * @param range      The RVR range values
     * @param prefix     The M/P prefix
     * @param trend      The trend indicator
     * @param rawGroup   The RVR group as reported, for the rejection log
     */
    private void createAndAddRvr(String runwayName, RvrRange range, String prefix, String trend, String rawGroup) {
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

        } catch (InvalidObservationException e) {
            logRejected("RVR", rawGroup, e);
        }
    }

    // ==================== RUNWAY STATE HANDLER ====================

    /**
     * Handle runway state group: "R23R/1///60", "R05R/090060", "R34L/CLRD70", "R24///////".
     * Decoding of the code fields is done by RunwayState.
     *
     * @param matcher The regex matcher
     */
    @Override
    protected void handleRunwayState(Matcher matcher) {
        if (weatherData == null) {
            return;
        }

        String runwayName = matcher.group("name");
        try {
            RunwayState state = matcher.group("clrd") != null
                    ? RunwayState.clearedFromCode(runwayName, matcher.group("friction"))
                    : RunwayState.fromCodes(runwayName, matcher.group("deposit"), matcher.group("extent"),
                    matcher.group("depth"), matcher.group("friction"));
            weatherData.addRunwayState(state);
            LOGGER.debug("Runway state: {}", state);
        } catch (InvalidObservationException e) {
            logRejected("runway state", matcher.group(0).trim(), e);
        }
    }

    /**
     * Handle temperature and dewpoint.
     * <p>
     * Format: TT/DD or M[TT]/M[DD]
     * Examples: 22/12, M05/M12, 15/, //, XX/XX
     * <p>
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

        } catch (InvalidObservationException e) {
            logRejected("temperature/dewpoint", matcher.group(0).trim(), e);
        }
    }

    /**
     * Parse a temperature or dewpoint value from METAR format.
     * <p>
     * Handles:
     * - M prefix for negative (e.g., "M05" → -5.0)
     * - Hyphen prefix for negative (e.g., "-05" → -5.0)
     * - Missing indicators (//, XX, MM) → null
     * - Null or blank → null
     *
     * @param sign   the sign indicator ("M", "-", or null)
     * @param digits the temperature digits (e.g., "05", "22")
     * @return temperature in Celsius, or null if missing/unknown
     */
    private Double parseTemperatureValue(String sign, String digits) {
        // Handle missing/unknown values
        if (digits == null || digits.isBlank() ||
                "//".equals(digits) || "XX".equals(digits) || "MM".equals(digits)) {
            return null;
        }

        // The pattern guarantees digits here
        double value = Double.parseDouble(digits);

        // Apply negative sign if present
        if ("M".equals(sign) || "-".equals(sign)) {
            value = -value;
        }

        return value;
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
     * @param matcher    The regex matcher with captured groups
     * @param remarkType The type of remark for logging
     */
    private void handleRemarkRegistry(Matcher matcher, String remarkType) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} pattern matched: {}", remarkType, matcher.group(0));
        }
    }

    /**
     * Handle automated station type from remarks (AO1 or AO2).
     * <p>
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

        // AUTO_PATTERN only matches type 1 or 2, so fromDigit cannot reject it here
        String typeDigit = matcher.group("type");
        AutomatedStationType stationType = AutomatedStationType.fromDigit(typeDigit);

        // For now, just log it - full integration will store in remarks
        logAutomatedStationType(stationType);
    }

    // ==================== REMARKS HANDLERS ====================
    // (Keeping all the existing remarks handlers - they are METAR-specific)
    // These are NOT moved to base class as they are specific to METAR observations

    /**
     * Handle the remarks section of the METAR using sequential parsing.
     * <p>
     * This method processes remarks in order and stores any unparsed content
     * as free text. Called after the main body has been parsed.
     *
     * @param remarksText the remarks text (everything after RMK)
     * @param metarData   the METAR data object to populate
     */
    private void handleRemarks(String remarksText, NoaaMetarData metarData) {
        if (remarksText == null || remarksText.isBlank()) {
            return;
        }

        NoaaMetarRemarks.Builder remarksBuilder = NoaaMetarRemarks.builder();
        String remaining = remarksText.trim();
        List<String> unparsedTokens = new ArrayList<>();
        RemarkHandlerChain chain = new RemarkHandlerChain(sequentialRemarkHandlers);

        while (!remaining.isBlank()) {
            remaining = chain.runPasses(remaining, remarksBuilder);

            if (remaining.isBlank()) {
                break;
            }

            // No handler could make progress against the current leading token.
            // Peel off one whitespace-delimited token as unparsed, then retry
            // the full handler set against whatever follows — an unrecognized
            // token shouldn't prevent later, otherwise-parseable tokens from
            // being recognized.
            int spaceIdx = remaining.indexOf(' ');
            if (spaceIdx < 0) {
                unparsedTokens.add(remaining);
                remaining = "";
            } else {
                unparsedTokens.add(remaining.substring(0, spaceIdx));
                remaining = remaining.substring(spaceIdx + 1).trim();
            }
        }

        // Store any unparsed remarks as free text
        if (!unparsedTokens.isEmpty()) {
            remarksBuilder.freeText(String.join(" ", unparsedTokens));
        }

        metarData.setRemarks(remarksBuilder.build());
    }

    /**
     * Build the ordered list of remark handlers run by {@link RemarkHandlerChain}.
     * The order is the order handlers run within each pass; names appear in breaker logs.
     *
     * @return the remark handlers, in run order
     */
    private List<RemarkHandler> createRemarkHandlers() {
        return List.of(
                new RemarkHandler("automated station type", this::handleAutomatedStationType),
                new RemarkHandler("sea level pressure", this::handleSeaLevelPressureSequential),
                new RemarkHandler("hourly temperature", this::handleHourlyTemperatureSequential),
                new RemarkHandler("peak wind", this::handlePeakWindSequential),
                new RemarkHandler("wind shift", this::handleWindShiftSequential),
                new RemarkHandler("wind-at-location", this::handleWindAtLocationSequential),
                new RemarkHandler("variable visibility", this::handleVariableVisibilitySequential),
                new RemarkHandler("variable ceiling", this::handleVariableCeilingSequential),
                new RemarkHandler("ceiling second site", this::handleCeilingSecondSiteSequential),
                new RemarkHandler("obscuration", this::handleObscurationSequential),
                new RemarkHandler("lightning", this::handleLightningSequential),
                new RemarkHandler("thunderstorm location", this::handleThunderstormLocationSequential),
                new RemarkHandler("cloud type", this::handleCloudTypeSequential),
                new RemarkHandler("tower/surface visibility", this::handleTowerSurfaceVisibilitySequential),
                new RemarkHandler("hourly precipitation", this::handleHourlyPrecipitationSequential),
                new RemarkHandler("PP group", this::handlePpGroupSequential),
                new RemarkHandler("predominant cloud type", this::handlePredominantCloudTypeSequential),
                new RemarkHandler("multi-hour precipitation", this::handleMultiHourPrecipitationSequential),
                new RemarkHandler("hail size", this::handleHailSizeSequential),
                new RemarkHandler("weather events", this::handleWeatherEventsSequential),
                new RemarkHandler("pressure tendency", this::handlePressureTendencySequential),
                new RemarkHandler("pressure rapid change", this::handlePressureRapidChangeSequential),
                new RemarkHandler("icing", this::handleIcingSequential),
                new RemarkHandler("6-hour max/min temperature", this::handle6HourMaxMinTemperatureSequential),
                new RemarkHandler("24-hour max/min temperature", this::handle24HourMaxMinTemperatureSequential),
                new RemarkHandler("density altitude", this::handleDensityAltitudeSequential),
                new RemarkHandler("automated maintenance", this::handleAutomatedMaintenanceSequential),
                new RemarkHandler("secondary altimeter", this::handleSecondaryAltimeterSequential),
                new RemarkHandler("directional weather", this::handleDirectionalWeatherSequential),
                new RemarkHandler("observation program status", this::handleObservationProgramStatusSequential),
                new RemarkHandler("except-direction", this::handleExceptDirectionSequential));
    }

    /**
     * Copy commonly-accessed remark fields to top-level NoaaMetarData fields
     * for convenient access without navigating through remarks object.
     * Extracts values from complex remark types into simple types.
     */
    private void copyRemarksToTopLevel() {
        // Guard against null weatherData (parsing failed before initialization)
        if (weatherData == null) {
            return;
        }

        NoaaMetarRemarks remarks = weatherData.getRemarks();
        if (remarks == null) {
            return;
        }

        // Copy automated station type (convert enum to String: "AO1" or "AO2")
        if (remarks.automatedStationType() != null) {
            weatherData.setAutomatedStation(remarks.automatedStationType().toString());
        }

        // Copy sea level pressure (extract hPa value from Pressure record)
        if (remarks.seaLevelPressure() != null) {
            weatherData.setSeaLevelPressure(remarks.seaLevelPressure().toHectopascals());
        }

        // Copy peak wind (PeakWind type matches - direct copy)
        if (remarks.peakWind() != null) {
            weatherData.setPeakWind(remarks.peakWind());
        }

        // Copy hourly precipitation (extract inches from PrecipitationAmount record)
        if (remarks.hourlyPrecipitation() != null) {
            weatherData.setHourlyPrecipitation(remarks.hourlyPrecipitation().inches());
        }

        // Copy 6-hour max temperature (extract Celsius from Temperature record)
        if (remarks.sixHourMaxTemperature() != null) {
            weatherData.setSixHourMaxTemp(remarks.sixHourMaxTemperature().celsius());
        }

        // Copy 6-hour min temperature (extract Celsius from Temperature record)
        if (remarks.sixHourMinTemperature() != null) {
            weatherData.setSixHourMinTemp(remarks.sixHourMinTemperature().celsius());
        }

        // Copy 24-hour max temperature (extract Celsius from Temperature record)
        if (remarks.twentyFourHourMaxTemperature() != null) {
            weatherData.setTwentyFourHourMaxTemp(remarks.twentyFourHourMaxTemperature().celsius());
        }

        // Copy 24-hour min temperature (extract Celsius from Temperature record)
        if (remarks.twentyFourHourMinTemperature() != null) {
            weatherData.setTwentyFourHourMinTemp(remarks.twentyFourHourMinTemperature().celsius());
        }

        // Copy 3-hour pressure tendency (extract hPa change from PressureTendency record)
        if (remarks.pressureTendency() != null) {
            weatherData.setThreeHourPressureTendency(remarks.pressureTendency().changeHectopascals());
        }

        // Copy pressure rapid change (PressureRapidChange type matches - direct copy)
        if (remarks.pressureRapidChange() != null) {
            weatherData.setPressureRapidChange(remarks.pressureRapidChange());
        }

        // Copy icing (Icing type matches - direct copy)
        if (remarks.icing() != null) {
            weatherData.setIcing(remarks.icing());
        }

        // Copy wind shift (WindShift type matches - direct copy)
        if (remarks.windShift() != null) {
            weatherData.setWindShift(remarks.windShift());
        }
    }

    /**
     * Handle automated station type remark for sequential parsing.
     * <p>
     * Format: AO1, AO2, AO1A, or AO2A
     * - AO1 = Automated station WITHOUT precipitation discriminator
     * - AO2 = Automated station WITH precipitation discriminator
     * - A01/A02 = OCR error variants (O misread as 0)
     * - Trailing "A" (AO1A, AO2A) = manually augmented by a human observer
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleAutomatedStationType(String remarksText, NoaaMetarRemarks.Builder remarks) {
        Matcher matcher = RegExprConst.AUTO_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        String matchedText = matcher.group();
        try {
            String typeDigit = matcher.group("type");
            boolean augmented = matcher.group("augmented") != null;
            AutomatedStationType stationType =
                    AutomatedStationType.fromDigitAndAugmentation(Integer.parseInt(typeDigit), augmented);

            remarks.automatedStationType(stationType);

            logAutomatedStationType(stationType);

            return remarksText.substring(matcher.end()).trim();

        } catch (InvalidObservationException | NumberFormatException e) {
            logRejectedRemark("automated station type", matchedText, e);
            return remarksText;
        }
    }

    private void logAutomatedStationType(AutomatedStationType stationType) {
        LOGGER.debug("Automated Station Type: {} ({})",
                stationType.getCode(),
                stationType.getDescription());
    }

    /**
     * Handle sea level pressure remark for sequential parsing.
     * <p>
     * Format: SLPppp where ppp is pressure in tenths of hectopascals
     * <p>
     * Examples:
     * - SLP210 → 1021.0 hPa (ppp=210 < 500, so 1000 + 21.0)
     * - SLP982 → 998.2 hPa (ppp=982 >= 500, so 900 + 98.2)
     * - SLP145 → 1014.5 hPa
     * - SLPNO → Sea level pressure not available
     * <p>
     * Decoding rules per Federal Meteorological Handbook No. 1:
     * - If ppp >= 500: Sea level pressure = 900 + (ppp / 10) hPa
     * - If ppp < 500: Sea level pressure = 1000 + (ppp / 10) hPa
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleSeaLevelPressureSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank (validated by handleRemarks())

        Matcher matcher = SEALVL_PRESS_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        String matchedText = matcher.group();
        try {
            String pressureStr = matcher.group(GROUP_PRESSURE_CHANGE);

            // Handle SLPNO (pressure not available)
            if ("NO".equals(pressureStr)) {
                LOGGER.debug("Sea level pressure: Not available (SLPNO)");
                return remarksText.substring(matcher.end()).trim();
            }

            // Pattern guarantees exactly three digits here
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

            Pressure seaLevelPressure = Pressure.hectopascals(pressureHPa);
            remarks.seaLevelPressure(seaLevelPressure);

            LOGGER.debug("Sea level pressure: {} hPa (from SLP{})", pressureHPa, ppp);

            return remarksText.substring(matcher.end()).trim();

        } catch (InvalidObservationException | NumberFormatException e) {
            logRejectedRemark("sea level pressure", matchedText, e);
            return remarksText;
        }
    }

    /**
     * Handle hourly temperature and dewpoint remark for sequential parsing.
     * <p>
     * Format: TsnT'T'T'snT'dT'dT'd
     * - sn = sign (0=positive, 1=negative)
     * - T'T'T' = temperature in tenths of degrees (e.g., 233 = 23.3°C)
     * - snT'dT'dT'd = optional dewpoint with sign and tenths
     * <p>
     * Examples:
     * - T02330139 → temp=+23.3°C, dewpoint=+13.9°C
     * - T10281015 → temp=-2.8°C, dewpoint=-1.5°C
     * - T0233 → temp=+23.3°C, dewpoint=not reported
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleHourlyTemperatureSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank

        Matcher matcher = TEMP_1HR_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        String matchedText = matcher.group();
        try {
            // Pattern guarantees sign and three digits for temperature
            int tempTenths = Integer.parseInt(matcher.group("temp"));
            double tempCelsius = tempTenths / 10.0;
            if ("1".equals(matcher.group("tsign"))) {
                tempCelsius = -tempCelsius;
            }

            // Dewpoint is optional
            String dewptStr = matcher.group("dewpt");

            if (dewptStr != null) {
                double dewptCelsius = Integer.parseInt(dewptStr) / 10.0;
                if ("1".equals(matcher.group("dsign"))) {
                    dewptCelsius = -dewptCelsius;
                }

                Temperature preciseTemp = Temperature.of(tempCelsius, dewptCelsius);
                remarks.preciseTemperature(preciseTemp);
                remarks.preciseDewpoint(preciseTemp); // Store the same object for dewpoint access

                LOGGER.debug("Precise temperature: {}°C, dewpoint: {}°C", tempCelsius, dewptCelsius);

            } else {
                Temperature preciseTemp = Temperature.of(tempCelsius);
                remarks.preciseTemperature(preciseTemp);

                LOGGER.debug("Precise temperature: {}°C (no dewpoint)", tempCelsius);
            }

            return remarksText.substring(matcher.end()).trim();

        } catch (InvalidObservationException | NumberFormatException e) {
            logRejectedRemark("hourly temperature", matchedText, e);
            return remarksText;
        }
    }

    /**
     * Handle peak wind remark for sequential parsing.
     * <p>
     * Format: PK WND dddff(f)/(hh)mm
     * - ddd = wind direction in degrees (optional)
     * - ff(f) = wind speed in knots, 2-3 digits (P prefix for speeds >99 knots)
     * - hh = hour of occurrence (optional)
     * - mm = minute of occurrence (optional)
     * <p>
     * Examples:
     * - PK WND 28032/1530 → dir=280°, speed=32kt, time=15:30 UTC
     * - PK WND 32035/15 → dir=320°, speed=35kt, time=XX:15 UTC (hour missing)
     * - PK WND P100/2145 → dir=unknown, speed=100kt, time=21:45 UTC
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handlePeakWindSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank

        Matcher matcher = PEAK_WIND_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        String matchedText = matcher.group();
        try {
            String dirStr = matcher.group("dir");
            Integer direction = dirStr != null ? Integer.parseInt(dirStr) : null;

            // Speed is required; 'P' prefix indicates speed >99 knots
            int speed = Integer.parseInt(matcher.group("speed").replace("P", ""));

            String hourStr = matcher.group("hour");
            Integer hour = hourStr != null ? Integer.parseInt(hourStr) : null;

            // Minute is required by the pattern
            int minute = Integer.parseInt(matcher.group("min"));

            PeakWind peakWind = new PeakWind(direction, speed, hour, minute);
            remarks.updateWind(w -> w.withPeakWind(peakWind));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Peak wind: dir={}°, speed={}kt, time={}:{}",
                        direction, speed,
                        hour != null ? String.format("%02d", hour) : "XX",
                        String.format("%02d", minute));
            }

            return remarksText.substring(matcher.end()).trim();

        } catch (InvalidObservationException | NumberFormatException e) {
            logRejectedRemark("peak wind", matchedText, e);
            return remarksText;
        }
    }

    /**
     * Handle wind shift remark for sequential parsing.
     * <p>
     * Format: WSHFT (hh)mm [FROPA]
     * - hh = hour of wind shift (optional)
     * - mm = minute of wind shift (required)
     * - FROPA = frontal passage indicator (optional)
     * <p>
     * A wind shift is reported when wind direction changes by 45° or more
     * in less than 15 minutes, with sustained winds of 10 knots or more.
     * <p>
     * Examples:
     * - WSHFT 1530 → hour=15, minute=30, no frontal passage
     * - WSHFT 1530 FROPA → hour=15, minute=30, with frontal passage
     * - WSHFT 30 → hour=null, minute=30, no frontal passage
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleWindShiftSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank

        Matcher matcher = WIND_SHIFT_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        String matchedText = matcher.group();
        try {
            String hourStr = matcher.group("hour");
            Integer hour = hourStr != null ? Integer.parseInt(hourStr) : null;

            // Minute is required by the pattern
            int minute = Integer.parseInt(matcher.group("min"));

            boolean frontalPassage = "FROPA".equals(matcher.group("front"));

            WindShift windShift = new WindShift(hour, minute, frontalPassage);
            remarks.updateWind(w -> w.withWindShift(windShift));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Wind shift: time={}:{}, frontal passage={}",
                        hour != null ? String.format("%02d", hour) : "XX",
                        String.format("%02d", minute),
                        frontalPassage);
            }

            return remarksText.substring(matcher.end()).trim();

        } catch (InvalidObservationException | NumberFormatException e) {
            logRejectedRemark("wind shift", matchedText, e);
            return remarksText;
        }
    }

    /**
     * Handle wind-at-location remarks (altitude or runway-specific wind readings).
     * Format: WIND ####FT ddd(dd)KT or WIND RWY ## ddd(dd)KT
     * Multiple entries may be chained in sequence.
     * <p>
     * Examples:
     * - WIND 1400FT 23010KT → wind at 1400ft altitude
     * - WIND RWY 26 00000KT → wind at runway 26
     *
     * @param remarksText remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after processing (never null)
     */
    private String handleWindAtLocationSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText != null ? remarksText : "";
        }

        String remaining = remarksText.trim();
        Matcher matcher = WIND_AT_LOCATION_PATTERN.matcher(remaining);

        while (matcher.find() && matcher.start() == 0) {
            if (!processWindAtLocationMatch(matcher, remarks)) {
                // Rejected: leave the group in place for freeText and stop,
                // otherwise the loop would re-match the same token forever
                break;
            }
            remaining = remaining.substring(matcher.end()).trim();
            matcher = WIND_AT_LOCATION_PATTERN.matcher(remaining);
        }

        return remaining;
    }

    /**
     * Process a single wind-at-location pattern match.
     *
     * @param matcher the pattern matcher positioned at a match (must not be null)
     * @param remarks the remarks builder (must not be null)
     * @return true if the match was stored; false if it was rejected and should
     * be left in the remarks text for freeText
     */
    private boolean processWindAtLocationMatch(Matcher matcher, NoaaMetarRemarks.Builder remarks) {
        String matchedText = matcher.group();
        try {
            WindAtLocation windAtLocation = buildWindAtLocation(matcher);
            remarks.updateWind(w -> w.addWindAtLocation(windAtLocation));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Wind at location: {}", windAtLocation.getSummary());
            }
            return true;

        } catch (InvalidObservationException e) {
            logRejectedRemark("wind-at-location", matchedText, e);
            return false;
        }
    }

    /**
     * Extract wind-at-location components from a pattern matcher and construct
     * the corresponding WindAtLocation value object.
     *
     * @param matcher the pattern matcher (must not be null)
     * @return the constructed WindAtLocation (never null)
     * @throws IllegalArgumentException if the matched data is invalid
     */
    private WindAtLocation buildWindAtLocation(Matcher matcher) {
        String heightStr = matcher.group(GROUP_HEIGHT_CODE);
        String runway = matcher.group("runway");
        String dirStr = matcher.group("dir");
        String speedStr = matcher.group("speed");
        String gustStr = matcher.group("gust");
        String unit = matcher.group("unit");

        Integer height = heightStr != null ? Integer.parseInt(heightStr) : null;
        Integer direction = "VRB".equals(dirStr) ? null : Integer.parseInt(dirStr);
        int speed = Integer.parseInt(speedStr);
        Integer gust = gustStr != null ? Integer.parseInt(gustStr) : null;

        Wind wind = new Wind(direction, speed, gust, null, null, unit);

        return new WindAtLocation(height, runway, wind);
    }

    /**
     * Handle a present-weather phenomenon restated in remarks with associated
     * compass direction(s) — typically a vicinity phenomenon reported nearby.
     * Reuses the same PRESENT_WEATHER_PATTERN and PresentWeather parsing as the
     * main body, since the weather-code grammar is identical; only the trailing
     * direction list is new. Also accepts the hyphen-joined form VCSH-NE.
     * <p>
     * Example: RMK VCSH E SE → showers in vicinity, east and southeast
     *
     * @param remarksText remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after processing (never null)
     */
    private String handleDirectionalWeatherSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText != null ? remarksText : "";
        }

        String remaining = remarksText.trim();

        // "VCSH-NE": match with the hyphen read as a space, so the code and direction
        // parse as usual; the text is the same length, so offsets line up (#129)
        String parseText = joinHyphenatedDirection(remaining);

        // PRESENT_WEATHER_PATTERN requires trailing whitespace, which is absent when
        // the weather code is the last token in the remarks string. Pad with a
        // synthetic trailing space so a bare trailing code (e.g. "VCSH") still
        // matches, without modifying the shared pattern itself.
        String searchText = parseText.contains(" ") ? parseText : parseText + " ";

        Matcher weatherMatcher = PRESENT_WEATHER_PATTERN.matcher(searchText);

        if (weatherMatcher.find() && weatherMatcher.start() == 0) {
            String result = processDirectionalWeatherMatch(weatherMatcher, parseText, remarks);
            // Declined: return the remark exactly as reported (e.g. "VCSH-NE", not "VCSH NE")
            return result.equals(parseText) ? remaining : result;
        }

        return remaining;
    }

    /**
     * Replace the hyphen in a leading "CODE-POINT" token (e.g. "VCSH-NE") with a space.
     *
     * @param text the remaining remarks text
     * @return the text with that one hyphen replaced, or the text unchanged
     */
    private static String joinHyphenatedDirection(String text) {
        Matcher matcher = WEATHER_HYPHEN_DIRECTION_PATTERN.matcher(text);
        if (!matcher.find()) {
            return text;
        }
        int hyphen = matcher.end("wx");
        return text.substring(0, hyphen) + " " + text.substring(hyphen + 1);
    }

    /**
     * Process a present-weather match in remarks, then attempt to consume a
     * trailing direction list.
     *
     * @param weatherMatcher the present-weather pattern matcher (must not be null)
     * @param remaining      the remaining text (must not be null)
     * @param remarks        the remarks builder (must not be null)
     * @return the remaining text after processing (never null)
     */
    private String processDirectionalWeatherMatch(Matcher weatherMatcher, String remaining,
                                                  NoaaMetarRemarks.Builder remarks) {
        String weatherCode = weatherMatcher.group(0).trim();

        // Ambiguous with thunderstorm/cloud-location remarks (e.g. "TS SE") —
        // defer to handleThunderstormLocationSequential rather than claiming it here.
        if (THUNDERSTORM_LOCATION_CODES.contains(weatherCode)) {
            return remaining;
        }

        int matchEnd = Math.min(weatherMatcher.end(), remaining.length());
        String afterWeather = remaining.substring(matchEnd).trim();

        // Optional location qualifier: "SH DSNT N", "SHRA OHD" (#129)
        String location = null;
        Matcher locationMatcher = REMARK_WEATHER_LOCATION_PATTERN.matcher(afterWeather);
        if (locationMatcher.find()) {
            location = locationMatcher.group("loc");
            afterWeather = afterWeather.substring(locationMatcher.end()).trim();
        }

        List<String> directions = null;
        boolean allQuadrants = false;
        String finalRemaining = afterWeather;

        Matcher directionMatcher = DIRECTION_LIST_PATTERN.matcher(afterWeather);
        Matcher allQuadrantsMatcher = ALL_QUADRANTS_PATTERN.matcher(afterWeather);
        if (directionMatcher.find() && directionMatcher.start() == 0) {
            directions = Arrays.asList(directionMatcher.group(1).split("\\s+"));
            finalRemaining = afterWeather.substring(directionMatcher.end()).trim();
        } else if (allQuadrantsMatcher.find() && allQuadrantsMatcher.start() == 0) {
            allQuadrants = true;
            finalRemaining = afterWeather.substring(allQuadrantsMatcher.end()).trim();
        }

        // A remark weather code must say where: a location qualifier, a direction, ALQDS,
        // or the VC (vicinity) prefix, which is itself a location. Without one (e.g. the GR
        // of a rejected "GR 0") it is a fragment of another group: leave it for freeText (#129).
        String intensity = weatherMatcher.group("int");
        boolean vicinity = intensity != null && intensity.contains("VC");
        if (location == null && directions == null && !allQuadrants && !vicinity) {
            return remaining;
        }

        try {
            PresentWeather presentWeather = PresentWeather.parse(weatherCode);
            DirectionalWeather directionalWeather =
                    new DirectionalWeather(presentWeather, location, directions, allQuadrants);
            remarks.directionalWeather(directionalWeather);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Directional weather: {}", directionalWeather.getSummary());
            }
        } catch (InvalidObservationException e) {
            // Rejected: leave the weather code (and any directions) for freeText
            logRejectedRemark("directional weather", weatherCode, e);
            return remaining;
        }

        return finalRemaining;
    }

    /**
     * Handle Canadian MANOBS observation program status remark (LAST STFD
     * OBS/NEXT or LAST OBS/NEXT).
     * <p>
     * Format: LAST [STFD] OBS/NEXT ddhhmm(Z|UTC) [VIA <station-id>]
     * - STFD = optional staffed-program qualifier
     * - ddhhmm = day, hour, minute of the next observation (UTC)
     * - Z or UTC = time-zone suffix (fused or space-separated); not preserved,
     * since MANOBS times are always UTC
     * - VIA <station-id> = optional relay-station qualifier, indicating the
     * next observation will be relayed via the named station rather than
     * issued by the reporting station itself
     * <p>
     * Examples:
     * - LAST STFD OBS/NEXT 261200Z → staffed, next observation day 26 at 12:00
     * - LAST OBS/NEXT 101300UTC → not staffed, next observation day 10 at 13:00
     * - LAST STFD OBS / NEXT 271200 UTC → staffed, next observation day 27 at 12:00
     * - LAST STFD OBS/NEXT 021200 UTC VIA CYQB → staffed, next observation day 02
     * at 12:00, relayed via CYQB
     *
     * @param remarksText remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after processing (never null)
     */
    private String handleObservationProgramStatusSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText != null ? remarksText : "";
        }

        String remaining = remarksText.trim();
        Matcher matcher = LAST_OBS_PATTERN.matcher(remaining);

        if (matcher.find() && matcher.start() == 0) {
            String matchedText = matcher.group();
            try {
                boolean staffed = matcher.group("stfd") != null;
                int day = Integer.parseInt(matcher.group("day"));
                int hour = Integer.parseInt(matcher.group("hour"));
                int minute = Integer.parseInt(matcher.group("minute"));
                String viaStation = matcher.group("via");

                ObservationProgramStatus status = ObservationProgramStatus.of(staffed, day, hour, minute, viaStation);
                remarks.observationProgramStatus(status);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Observation program status: {}", status.getSummary());
                }

                remaining = remaining.substring(matcher.end()).trim();

            } catch (InvalidObservationException | NumberFormatException e) {
                logRejectedRemark("observation program status", matchedText, e);
            }
        }

        return remaining;
    }

    /**
     * Handle variable visibility remark for sequential parsing.
     * <p>
     * Format: VIS [DIR] minVmax
     * - DIR = Optional direction (N, NE, E, SE, S, SW, W, NW)
     * - min = Minimum visibility (fraction, mixed, or whole number)
     * - V = "V" separator
     * - max = Maximum visibility (same formats as min)
     * <p>
     * Examples:
     * - VIS 1/2V2 → min=1/2 SM, max=2 SM
     * - VIS NE 2V4 → Northeast, min=2 SM, max=4 SM
     * - VIS 1 1/4V3 → min=1.25 SM, max=3 SM
     * <p>
     * The pattern requires both distances and the V separator, so sector
     * visibility and visibility at a second location do not match and are left
     * for freeText. If a match is rejected (a distance fails to parse, or the
     * values fail validation), the text is returned unchanged so the remark
     * surfaces in freeText rather than being dropped (#92 rule, #115).
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after this remark, or remarksText unchanged if
     * there is no match or the match is rejected
     */
    private String handleVariableVisibilitySequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank

        Matcher matcher = VPV_SV_VSL_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        String matchedText = matcher.group();
        String direction = matcher.group("dir");
        String dist1Str = matcher.group("dist1");
        String dist2Str = matcher.group("dist2");

        try {
            Visibility minVisibility = parseVisibilityDistance(dist1Str);
            Visibility maxVisibility = parseVisibilityDistance(dist2Str);
            if (minVisibility == null || maxVisibility == null) {
                LOGGER.debug("Variable visibility distance could not be parsed, leaving for freeText: {}",
                        matchedText);
                return remarksText;
            }

            VariableVisibility variableVisibility = new VariableVisibility(
                    minVisibility,
                    maxVisibility,
                    direction,
                    null // location is not part of this remark form
            );

            remarks.updateVisibility(v -> v.withVariableVisibility(variableVisibility));

            LOGGER.debug("Variable visibility: {} varying to {}{}",
                    dist1Str,
                    dist2Str,
                    direction != null ? " (" + direction + ")" : "");

            return remarksText.substring(matcher.end()).trim();

        } catch (InvalidObservationException e) {
            logRejectedRemark("variable visibility", matchedText, e);
            return remarksText;
        }
    }

    /**
     * Parse a visibility distance string into a Visibility object.
     * <p>
     * Handles formats:
     * - Fractions: "1/2", "3/4"
     * - Mixed: "1 1/2", "2 1/4"
     * - Whole numbers: "1", "10"
     * <p>
     * A zero denominator (e.g. "1/0") is not a valid distance and returns null,
     * so callers leave the group for freeText (#115).
     *
     * @param distStr the distance string
     * @return Visibility object, or null if parsing fails
     */
    private Visibility parseVisibilityDistance(String distStr) {
        if (distStr == null || distStr.isBlank()) {
            return null;
        }

        String trimmed = distStr.trim();
        try {
            Double statuteMiles = parseDistanceStatuteMiles(trimmed);
            return statuteMiles != null ? Visibility.statuteMiles(statuteMiles) : null;

        } catch (NumberFormatException e) {
            logRejectedRemark("visibility distance", trimmed, e);
            return null;
        }
    }

    /**
     * Convert a whole ("10"), simple fraction ("1/2") or mixed ("1 1/2")
     * distance to statute miles.
     *
     * @param text the trimmed distance text
     * @return the distance in statute miles, or null if a fraction has a zero denominator
     * @throws NumberFormatException if a numeric part cannot be parsed
     */
    private static Double parseDistanceStatuteMiles(String text) {
        if (!text.contains("/")) {
            return Double.parseDouble(text);
        }

        String[] parts = text.split("\\s+");
        if (parts.length == 2) {
            // Mixed fraction: "1 1/2"
            Double fraction = parseVisFraction(parts[1]);
            return fraction != null ? Integer.parseInt(parts[0]) + fraction : null;
        }

        // Simple fraction: "1/2"
        return parseVisFraction(text);
    }

    /**
     * Parse a simple fraction such as "3/4".
     *
     * @param fraction the fraction text
     * @return the fraction's value, or null if the denominator is zero
     * @throws NumberFormatException if the numerator or denominator cannot be parsed
     */
    private static Double parseVisFraction(String fraction) {
        String[] parts = fraction.split("/");
        int denominator = Integer.parseInt(parts[1]);
        if (denominator == 0) {
            return null; // e.g. "1/0" is not a valid distance (#115)
        }
        return (double) Integer.parseInt(parts[0]) / denominator;
    }

    /**
     * Handle tower or surface visibility remark for sequential parsing.
     * <p>
     * Format: TWR VIS value OR SFC VIS value
     * - TWR VIS = Tower visibility
     * - SFC VIS = Surface visibility
     * - value = Visibility distance (fraction, mixed, or whole number)
     * <p>
     * Examples:
     * - TWR VIS 1 1/2 → Tower visibility 1.5 SM
     * - SFC VIS 1/4 → Surface visibility 0.25 SM
     * - TWR VIS 2 → Tower visibility 2 SM
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleTowerSurfaceVisibilitySequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText != null ? remarksText : "";
        }

        String remaining = remarksText.trim();
        Matcher matcher = TWR_SFC_VIS_PATTERN.matcher(remaining);

        while (matcher.find() && matcher.start() == 0) {
            if (!processTowerSurfaceVisibility(matcher, remarks)) {
                break; // rejected: leave for freeText rather than re-matching it forever
            }
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
     * @return true if stored; false if rejected and left for freeText
     */
    private boolean processTowerSurfaceVisibility(Matcher matcher, NoaaMetarRemarks.Builder remarks) {
        String matchedText = matcher.group();
        String type = matcher.group("type");
        String distStr = matcher.group("dist"); // required by the pattern

        try {
            Visibility visibility = parseVisibilityDistance(distStr);
            if (visibility == null) {
                LOGGER.debug("Tower/surface visibility could not be parsed, leaving for freeText: {}", matchedText);
                return false;
            }
            setVisibilityByType(type, visibility, distStr, remarks);
            return true;

        } catch (InvalidObservationException e) {
            logRejectedRemark("tower/surface visibility", matchedText, e);
            return false;
        }
    }

    /**
     * Set the appropriate visibility field based on type.
     *
     * @param type       the visibility type (TWR VIS or SFC VIS)
     * @param visibility the parsed visibility
     * @param distStr    the original distance string (for logging)
     * @param remarks    the remarks builder to populate
     */
    private void setVisibilityByType(String type, Visibility visibility, String distStr,
                                     NoaaMetarRemarks.Builder remarks) {
        if ("TWR VIS".equals(type)) {
            remarks.updateVisibility(v -> v.withTowerVisibility(visibility));
            LOGGER.debug("Tower visibility: {}", distStr);
        } else if ("SFC VIS".equals(type)) {
            remarks.updateVisibility(v -> v.withSurfaceVisibility(visibility));
            LOGGER.debug("Surface visibility: {}", distStr);
        }
    }

    /**
     * Handle hourly precipitation amount remark.
     * <p>
     * Format: P0015 = 0.15 inches in last hour
     * - P = Hourly precipitation indicator
     * - 4 digits = Amount in hundredths of inches
     * - //// = Trace precipitation
     * <p>
     * Examples:
     * - P0015 → 0.15 inches
     * - P0009 → 0.09 inches
     * - P//// → Trace
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleHourlyPrecipitationSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank

        Matcher matcher = PRECIP_1HR_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        String matchedText = matcher.group();
        try {
            PrecipitationAmount precip = PrecipitationAmount.fromEncoded(matcher.group("precip"), 1);
            remarks.hourlyPrecipitation(precip);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Hourly precipitation: {}",
                        precip.isTrace() ? "trace" : String.format("%.2f inches", precip.inches()));
            }

            return remarksText.substring(matcher.end()).trim();

        } catch (InvalidObservationException e) {
            logRejectedRemark("hourly precipitation", matchedText, e);
            return remarksText;
        }
    }

    /**
     * Handles the predominant cloud type remark (8/C_L C_M C_H), reporting the
     * predominant low, middle, and high cloud type per WMO Cloud Atlas coding
     * instructions.
     *
     * @param remarksText remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after this token is consumed
     */
    private String handlePredominantCloudTypeSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText != null ? remarksText : "";
        }

        String remaining = remarksText.trim();
        Matcher matcher = PREDOMINANT_CLOUD_TYPE_PATTERN.matcher(remaining);

        if (matcher.find() && matcher.start() == 0) {
            String matchedText = matcher.group();
            try {
                LowCloudType low = LowCloudType.fromCode(matcher.group("low"));
                MiddleCloudType middle = MiddleCloudType.fromCode(matcher.group("middle"));
                HighCloudType high = HighCloudType.fromCode(matcher.group("high"));

                remarks.predominantCloudTypes(new PredominantCloudTypes(low, middle, high));

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Predominant cloud types: low={}, middle={}, high={}", low, middle, high);
                }

                remaining = remaining.substring(matcher.end()).trim();

            } catch (InvalidObservationException e) {
                logRejectedRemark("predominant cloud type group", matchedText, e);
            }
        }

        return remaining;
    }

    /**
     * Handle the "PP" precipitation-amount group. Format and unit/scale are
     * not fully confirmed at this time (research inconclusive) — the raw
     * 3-digit value is captured as-is rather than converted to a specific
     * unit. Distinct from the US-style single-P hourly precipitation group.
     * <p>
     * Example: PP000
     *
     * @param remarksText remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after processing (never null)
     */
    private String handlePpGroupSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText != null ? remarksText : "";
        }

        String remaining = remarksText.trim();
        Matcher matcher = PP_GROUP_PATTERN.matcher(remaining);

        if (matcher.find() && matcher.start() == 0) {
            String matchedText = matcher.group();
            try {
                int value = Integer.parseInt(matcher.group("value"));
                remarks.ppGroupValue(value);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("PP group value (raw, unit unconfirmed): {}", value);
                }

                remaining = remaining.substring(matcher.end()).trim();

            } catch (InvalidObservationException | NumberFormatException e) {
                logRejectedRemark("PP group value", matchedText, e);
            }
        }

        return remaining;
    }

    /**
     * Handle 6-hour or 24-hour precipitation amount remark.
     * <p>
     * Format: 60009 (6-hour) or 70125 (24-hour)
     * - 6 = 6-hour precipitation indicator
     * - 7 = 24-hour precipitation indicator
     * - 4-5 digits = Amount in hundredths of inches
     * - //// or ///// = Trace precipitation
     * <p>
     * Examples:
     * - 60009 → 0.09 inches (6-hour)
     * - 70125 → 1.25 inches (24-hour)
     * - 6//// → Trace (6-hour)
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleMultiHourPrecipitationSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank

        Matcher matcher = PRECIP_3HR_24HR_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        String matchedText = matcher.group();
        try {
            // Determine period: 6 = 6-hour, 7 = 24-hour
            int periodHours = "6".equals(matcher.group("type")) ? 6 : 24;

            PrecipitationAmount precip = PrecipitationAmount.fromEncoded(matcher.group("precip"), periodHours);

            if (periodHours == 6) {
                remarks.sixHourPrecipitation(precip);
            } else {
                remarks.twentyFourHourPrecipitation(precip);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{}-hour precipitation: {}", periodHours, precip.getDescription());
            }

            return remarksText.substring(matcher.end()).trim();

        } catch (InvalidObservationException e) {
            logRejectedRemark("multi-hour precipitation", matchedText, e);
            return remarksText;
        }
    }

    /**
     * Handle density altitude remarks.
     * Format: DENSITY ALT ####FT
     * <p>
     * Example: DENSITY ALT 1500FT → 1500 feet density altitude
     *
     * @param remarksText remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after processing (never null)
     */
    private String handleDensityAltitudeSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText != null ? remarksText : "";
        }

        String remaining = remarksText.trim();
        Matcher matcher = DENSITY_ALTITUDE_PATTERN.matcher(remaining);

        if (matcher.find() && matcher.start() == 0) {
            String matchedText = matcher.group();
            try {
                int densityAlt = Integer.parseInt(matcher.group("denalt"));
                remarks.densityAltitudeFeet(densityAlt);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Density altitude: {}FT", densityAlt);
                }

                remaining = remaining.substring(matcher.end()).trim();

            } catch (NumberFormatException e) {
                logRejectedRemark("density altitude value", matchedText, e);
            }
        }

        return remaining;
    }

    /**
     * Handle hail size remark for sequential parsing.
     * <p>
     * Format: GR followed by size in inches
     * Examples:
     * - GR 1/2 → 0.5 inch hail
     * - GR 1 3/4 → 1.75 inch hail
     * - GR 2 → 2 inch hail
     * <p>
     * Uses parseVisibilityDistance() to parse the size value since
     * the format is identical (fractions, mixed numbers, whole numbers).
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after this remark is processed
     */
    private String handleHailSizeSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank

        Matcher matcher = HAIL_SIZE_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        String matchedText = matcher.group();
        try {
            // Reuse parseVisibilityDistance - same format (fraction, mixed, whole)
            Visibility parsed = parseVisibilityDistance(matcher.group("size"));
            Double sizeInches = parsed != null ? parsed.toStatuteMiles() : null;
            if (sizeInches == null) {
                LOGGER.debug("Hail size could not be parsed, leaving for freeText: {}", matchedText);
                return remarksText;
            }

            HailSize hailSize = HailSize.inches(sizeInches);
            remarks.hailSize(hailSize);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Hail size: {} inches ({})", sizeInches, hailSize.getSizeCategory());
            }

            return remarksText.substring(matcher.end()).trim();

        } catch (InvalidObservationException e) {
            logRejectedRemark("hail size", matchedText, e);
            return remarksText;
        }
    }

    /**
     * Handle weather begin/end time events for sequential parsing.
     * <p>
     * Uses the existing BEGIN_END_WEATHER_PATTERN which captures:
     * - Intensity: int, int2
     * - Descriptor: desc (MI, PR, BC, DR, BL, SH, TS, FZ)
     * - Precipitation: prec (DZ, RA, SN, SG, IC, PL, GR, GS, UP)
     * - Obscuration: obsc (BR, FG, FU, VA, DU, SA, HZ, PY)
     * - Other: other (PO, SQ, FC, SS, DS, NSW)
     * - Begin time: begin (B marker), begint (time digits)
     * - End time: end (E marker), endt (time digits)
     * <p>
     * Time format:
     * - 2 digits (05) → minute only (:05)
     * - 4 digits (1159) → hour and minute (11:59)
     * <p>
     * Examples:
     * - RAB05 → Rain began at :05
     * - FZRAB1159E1240 → Freezing rain began 11:59, ended 12:40
     * - RAB15E30SNB30 → Rain began :15 ended :30; Snow began :30 (parsed as 2 events)
     * - -RAB05 → Light rain began :05
     * - +TSRAB20E45 → Heavy thunderstorm with rain began :20, ended :45
     * <p>
     * Multiple events can be chained together (e.g., RAB15E30SNB30 contains two events).
     * This method will parse all chained events in a single pass.
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after all chained events are processed
     */
    private String handleWeatherEventsSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        // Precondition: remarksText is non-null and non-blank
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText;
        }

        String remaining = remarksText;
        Matcher matcher = BEGIN_END_WEATHER_PATTERN.matcher(remaining);
        String lastWeatherCode = null;

        // Process all chained events. matcher.end() > 0 guards against a
        // zero-width match (every group in BEGIN_END_WEATHER_PATTERN is
        // optional) so the loop can't spin without consuming input.
        while (matcher.find() && matcher.start() == 0 && matcher.end() > 0) {
            WeatherEvent event = tryParseWeatherEvent(matcher, lastWeatherCode);

            if (event == null) {
                // No valid event, or the event was rejected: stop and leave the
                // segment for another handler or the freeText peeler
                break;
            }

            remarks.addWeatherEvent(event);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Weather event: {}", event.getSummary());
            }

            // Only advance lastWeatherCode from an *explicit* code on
            // this segment, not one that was just carried forward - so
            // a chain of multiple bare continuations all keep referring
            // back to the original type.
            String explicitCode = buildWeatherCodeFromExistingGroups(
                    matcher.group("desc"), matcher.group("prec"),
                    matcher.group("obsc"), matcher.group("other"));
            if (!explicitCode.isEmpty()) {
                lastWeatherCode = explicitCode;
            }

            // Remove matched portion and continue
            remaining = remaining.substring(matcher.end()).trim();
            matcher = BEGIN_END_WEATHER_PATTERN.matcher(remaining);
        }

        return remaining;
    }

    /**
     * Parse a weather event, treating a validation failure the same as "no
     * valid event": both return null so the caller stops without consuming
     * the segment (#92 rule, #115).
     *
     * @param matcher             the matcher positioned at a weather event
     * @param fallbackWeatherCode the last explicit weather code seen in this
     *                            chain, or null if none
     * @return the parsed WeatherEvent, or null if the segment is not a valid
     * event or was rejected by validation
     */
    private WeatherEvent tryParseWeatherEvent(Matcher matcher, String fallbackWeatherCode) {
        String matchedText = matcher.group();
        try {
            return parseWeatherEventFromExistingPattern(matcher, fallbackWeatherCode);
        } catch (InvalidObservationException e) {
            logRejectedRemark("weather event", matchedText, e);
            return null;
        }
    }

    /**
     * Parse a single weather event from the existing BEGIN_END_WEATHER_PATTERN matcher.
     * <p>
     * Extracts data from your existing capture groups:
     * - int, int2: intensity markers
     * - desc: descriptor (TS, FZ, etc.)
     * - prec: precipitation (RA, SN, etc.)
     * - obsc: obscuration (BR, FG, etc.)
     * - begin, begint: begin marker and time
     * - end, endt: end marker and time
     *
     * @param matcher             the matcher positioned at a weather event
     * @param fallbackWeatherCode the last explicit weather code seen in this
     *                            chain, or null if none
     * @return WeatherEvent object, or null if the match doesn't represent a valid event
     */
    private WeatherEvent parseWeatherEventFromExistingPattern(Matcher matcher, String fallbackWeatherCode) {
        String intensity = extractWeatherEventIntensity(matcher);

        String weatherCode = resolveWeatherEventCode(matcher, fallbackWeatherCode);
        if (weatherCode == null) {
            return null;
        }

        TimeComponents beginTime = extractWeatherEventTime(matcher, "begin", "begint");
        TimeComponents endTime = extractWeatherEventTime(matcher, "end", "endt");

        // Must have at least a 'begin' or 'end' time
        if (beginTime.minute() == null && endTime.minute() == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No begin or end time found in weather event: {}", matcher.group(0));
            }
            return null;
        }

        return new WeatherEvent(
                weatherCode,
                intensity,
                beginTime.hour(),
                beginTime.minute(),
                endTime.hour(),
                endTime.minute()
        );
    }

    /**
     * Extract intensity from a weather event match, preferring the trailing
     * marker (int2) over the leading one (int).
     *
     * @param matcher the regex matcher with captured groups
     * @return "-" or "+" if present, otherwise null
     */
    private String extractWeatherEventIntensity(Matcher matcher) {
        String intensityEnd = matcher.group("int2");
        String intensityStart = matcher.group("int");

        if (intensityEnd != null && intensityEnd.matches("[-+]")) {
            return intensityEnd;
        }
        if (intensityStart != null && intensityStart.matches("[-+]")) {
            return intensityStart;
        }
        return null;
    }

    /**
     * Resolve the weather code for a matched segment, falling back to the
     * most recently seen explicit type in the same chain when this segment
     * has no weather-code components of its own (an implied continuation,
     * e.g. "B13E46" following "TSE09").
     *
     * @param matcher             the regex matcher with captured groups
     * @param fallbackWeatherCode the last explicit weather code seen in this
     *                            chain, or null if none
     * @return the resolved weather code, or null if this segment has no code
     * of its own and no fallback is available
     */
    private String resolveWeatherEventCode(Matcher matcher, String fallbackWeatherCode) {
        String weatherCode = buildWeatherCodeFromExistingGroups(
                matcher.group("desc"), matcher.group("prec"),
                matcher.group("obsc"), matcher.group("other")
        );

        if (!weatherCode.isEmpty()) {
            return weatherCode;
        }

        if (fallbackWeatherCode == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No weather code found and no prior type to carry forward: {}", matcher.group(0));
            }
            return null;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Implied continuation - carrying forward '{}': {}", fallbackWeatherCode, matcher.group(0));
        }
        return fallbackWeatherCode;
    }

    /**
     * Extract a 'begin' or 'end' time component from a weather event match.
     *
     * @param matcher     the regex matcher with captured groups
     * @param markerGroup the marker group name ("begin" or "end")
     * @param timeGroup   the time digits group name ("begint" or "endt")
     * @return the parsed TimeComponents, with null hour/minute if the marker
     * or time digits are absent
     */
    private TimeComponents extractWeatherEventTime(Matcher matcher, String markerGroup, String timeGroup) {
        String marker = matcher.group(markerGroup);
        String timeStr = matcher.group(timeGroup);

        if (marker != null && timeStr != null) {
            return parseTimeDigits(timeStr);
        }

        return new TimeComponents(null, null);
    }

    /**
     * Helper record to return hour and minute components.
     */
    private record TimeComponents(Integer hour, Integer minute) {
    }

    /**
     * Parse time digits which can be either 2 digits (mm) or 4 digits (hhmm).
     * <p>
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

        // The pattern guarantees 2-4 digits; only the length can be wrong
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
            LOGGER.debug("Invalid time digit length: {}", timeStr);
            return new TimeComponents(null, null);
        }
    }

    /**
     * Build the weather code from your existing pattern's capture groups.
     * <p>
     * Combines descriptor, precipitation, obscuration, and other components
     * in the order they appear in the pattern.
     * <p>
     * Weather codes can be:
     * - Just descriptor (e.g., "TS" for thunderstorm)
     * - Descriptor + precipitation (e.g., "FZRA", "TSRA")
     * - Just precipitation (e.g., "RA", "SN")
     * - Precipitation + obscuration (e.g., "RABR")
     * - Just obscuration (e.g., "BR", "FG")
     *
     * @param descriptor    Weather descriptor (it may be null)
     * @param precipitation Weather precipitation (it may be null)
     * @param obscuration   Weather obscuration (it may be null)
     * @param other         Other weather phenomena (it may be null)
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
     * <p>
     * Uses the existing TS_CLD_LOC_PATTERN which captures:
     * - type: Cloud/phenomenon type (TS, CB, TCU, ACC, CBMAM, VIRGA)
     * - loc: Location qualifier (OHD, VC, DSNT, DSIPTD, TOP, TR)
     * - dir: Primary direction (N, NE, E, SE, S, SW, W, NW)
     * - dir2: Secondary direction for range (e.g., N-NE)
     * - dirm: Movement direction (if MOV present)
     * <p>
     * Examples:
     * - TS SE → Thunderstorm Southeast
     * - CB OHD MOV E → Cumulonimbus Overhead Moving East
     * - TCU DSNT N-NE → Towering Cumulus Distant North to Northeast
     * <p>
     * Multiple occurrences can be present (e.g., "TS SE CB W").
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after all cloud locations are processed
     */
    private String handleThunderstormLocationSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText;
        }

        String remaining = remarksText;
        String lastCloudType = null;
        Matcher matcher = nextThunderstormMatcher(remaining, null); // first clause cannot be a continuation

        while (matcher != null) {
            ThunderstormLocation location = tryParseThunderstormLocation(matcher, lastCloudType);
            if (location == null) {
                break; // rejected: leave for freeText rather than silently dropping (#115)
            }

            remarks.addThunderstormLocation(location);
            lastCloudType = location.cloudType();

            remaining = remaining.substring(matcher.end()).trim();
            matcher = nextThunderstormMatcher(remaining, lastCloudType);
        }

        return remaining;
    }

    /**
     * Parse one thunderstorm/cloud location clause (a full clause or a
     * continuation that carries the previous cloud type forward).
     *
     * @param matcher       the matcher positioned at a clause
     * @param lastCloudType the cloud type to carry into a continuation, or null
     * @return the parsed location, or null if it was rejected and should be
     * left in the remarks text for freeText (#92 rule, #115)
     */
    private ThunderstormLocation tryParseThunderstormLocation(Matcher matcher, String lastCloudType) {
        String matchedText = matcher.group();
        boolean continuation = matcher.pattern() == TS_CLD_LOC_CONTINUATION_PATTERN
                || matcher.pattern() == TS_CLD_LOC_AND_QUALIFIER_PATTERN;

        try {
            ThunderstormLocation location = continuation
                    ? parseThunderstormContinuationFromMatcher(matcher, lastCloudType)
                    : parseThunderstormLocationFromMatcher(matcher);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Thunderstorm/Cloud location{}: {}",
                        continuation ? " (continuation)" : "", location.getSummary());
            }
            return location;

        } catch (InvalidObservationException e) {
            logRejectedRemark("thunderstorm location", matchedText, e);
            return null;
        }
    }

    /**
     * Main pattern first; continuation only if a prior clause supplied a type to carry forward.
     */
    private Matcher nextThunderstormMatcher(String text, String lastCloudType) {
        Matcher matcher = matchAtStart(TS_CLD_LOC_PATTERN, text);
        if (matcher == null && lastCloudType != null) {
            matcher = matchAtStart(TS_CLD_LOC_CONTINUATION_PATTERN, text);
            if (matcher == null) {
                matcher = matchAtStart(TS_CLD_LOC_AND_QUALIFIER_PATTERN, text);
            }
        }
        return matcher;
    }

    private static Matcher matchAtStart(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() && matcher.start() == 0 ? matcher : null;
    }

    private ThunderstormLocation parseThunderstormContinuationFromMatcher(Matcher matcher, String carriedCloudType) {
        boolean qualifierOnly = matcher.pattern() == TS_CLD_LOC_AND_QUALIFIER_PATTERN;
        return new ThunderstormLocation(
                carriedCloudType,
                matcher.group("loc"),
                qualifierOnly ? List.of() : parseDirectionChain(matcher.group(DIRECTION_CHAIN_CODE)),
                qualifierOnly ? null : matcher.group("dirm")
        );
    }

    /**
     * Parse ThunderstormLocation from regex matcher.
     * <p>
     * Extracts all captured groups from TS_CLD_LOC_PATTERN and creates
     * a ThunderstormLocation object.
     *
     * @param matcher the regex matcher positioned at a thunderstorm/cloud location
     * @return ThunderstormLocation object with all extracted fields
     */
    private ThunderstormLocation parseThunderstormLocationFromMatcher(Matcher matcher) {
        String cloudType = matcher.group("type");
        String locationQualifier = matcher.group("loc");
        String dirchain = matcher.group(DIRECTION_CHAIN_CODE);
        String movingDirection = matcher.group("dirm");

        List<DirectionSegment> directionSegments = parseDirectionChain(dirchain);

        return new ThunderstormLocation(
                cloudType,
                locationQualifier,
                directionSegments,
                movingDirection
        );
    }

    /**
     * Parse a raw direction chain into a list of DirectionSegments.
     * <p>
     * A chain may contain one or more segments, separated by whitespace
     * or by "AND" (each segment describing a separate reported location
     * for the same cloud type). Each segment may itself be a single
     * compass point or a multipoint arc/range joined by hyphens.
     * <p>
     * Examples:
     * - "E-S-SW" → one segment, a 3-point arc: [E, S, SW]
     * - "N AND SW" → two segments, each a single point: [N], [SW]
     * - "N-E AND SE-S" → two segments, each a 2-point range: [N, E], [SE, S]
     * - "N NE S SW" → four segments, each a single point: [N], [NE], [S], [SW]
     *
     * @param dirchain the raw direction chain text (it may be null or blank)
     * @return list of DirectionSegments, empty if dirchain has no content
     */
    private List<DirectionSegment> parseDirectionChain(String dirchain) {
        if (dirchain == null || dirchain.isBlank()) {
            return List.of();
        }

        return SEGMENT_SEPARATOR_PATTERN.splitAsStream(dirchain.trim())
                .map(segment -> new DirectionSegment(
                        Arrays.asList(RANGE_SEPARATOR_PATTERN.split(segment))
                ))
                .toList();
    }

    /**
     * Handle "except [direction]" remarks for sequential parsing.
     * <p>
     * Uses EXCEPT_DIRECTION_PATTERN, which captures:
     * - dirchain: the direction chain (single point, range, or AND-chain of
     * either), using the same grammar as TS_CLD_LOC_PATTERN
     * <p>
     * XCPT, XCP, EXCP, and EXC are all accepted as equivalent forms of
     * "except"; which form appeared in the raw text is not preserved.
     * EXPCD/EXPCTD/EXPTD/EXP ("expected") have a different meaning and are
     * not recognized by this pattern.
     * <p>
     * Example:
     * - CB ALQDS XCPT N → Cumulonimbus in all quadrants, except to the north
     * <p>
     * Multiple occurrences can be present in one remarks string.
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after all except-direction remarks are processed
     */
    private String handleExceptDirectionSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText;
        }

        String remaining = remarksText;
        Matcher matcher = EXCEPT_DIRECTION_PATTERN.matcher(remaining);

        while (matcher.find() && matcher.start() == 0) {
            ExceptDirection exceptDirection = tryParseExceptDirection(matcher);
            if (exceptDirection == null) {
                break; // rejected: leave for freeText rather than silently dropping (#115)
            }

            remarks.addExceptDirection(exceptDirection);

            remaining = remaining.substring(matcher.end()).trim();
            matcher = EXCEPT_DIRECTION_PATTERN.matcher(remaining);
        }

        return remaining;
    }

    /**
     * Parse one except-direction remark.
     *
     * @param matcher the matcher positioned at an except-direction remark
     * @return the parsed ExceptDirection, or null if it was rejected and should
     * be left in the remarks text for freeText (#92 rule, #115)
     */
    private ExceptDirection tryParseExceptDirection(Matcher matcher) {
        String matchedText = matcher.group();
        try {
            List<DirectionSegment> directionSegments = parseDirectionChain(matcher.group(DIRECTION_CHAIN_CODE));
            ExceptDirection exceptDirection = ExceptDirection.of(directionSegments);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Except direction: {}", exceptDirection.getSummary());
            }
            return exceptDirection;

        } catch (InvalidObservationException e) {
            logRejectedRemark("except-direction", matchedText, e);
            return null;
        }
    }

    /**
     * Handle lightning remarks (LTG) for sequential parsing.
     * <p>
     * Format: [freq] LTG[types] [loc] [dir[-dir2]]
     * - freq: OCNL, FRQ, CONS, or CONTUS (optional)
     * - types: one or more of IC, CC, CG, CA, CW concatenated with no separator (optional)
     * - loc: OHD, VC, DSNT, or "AT AP" (optional)
     * - dir: ALQDS (all quadrants) or a compass point, optionally as a range
     * with dir2 (optional)
     * <p>
     * Multiple independent lightning remarks can appear in the same METAR,
     * each describing lightning at a different time/location - this method
     * loops to capture all of them.
     * <p>
     * Examples:
     * - OCNL LTGIC DSNT N → occasional in-cloud lightning, distant, north
     * - FRQ LTGCCCG VC W → frequent cloud-to-cloud and cloud-to-ground
     * lightning, in vicinity, west
     * - LTG DSNT ALQDS → lightning, distant, all quadrants
     * - LTGIC SW → in-cloud lightning, southwest (no frequency, no location)
     * - LTG DSNT S AND SW → lightning, distant, south and southwest (one remark, two segments)
     * - OCNL LTGIC DSNT SE AND S-SW → occasional in-cloud lightning, distant, SE and the S-SW range
     *
     * @param remarksText the remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after all lightning remarks are processed
     */
    private String handleLightningSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText != null ? remarksText : "";
        }

        String remaining = remarksText.trim();
        LightningMatcher matcher = new LightningMatcher(remaining);

        while (matcher.find()) {
            String matchedText = matcher.group(0);
            try {
                LightningRemark lightning = parseLightningFromMatcher(matcher);
                remarks.addLightningRemark(lightning);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Lightning: {}", lightning.getSummary());
                }

                remaining = remaining.substring(matchedText.length()).trim();
                matcher = new LightningMatcher(remaining);

            } catch (InvalidObservationException e) {
                logRejectedRemark("lightning remark", matchedText, e);
                break; // rejected: leave for freeText
            }
        }

        return remaining;
    }

    /**
     * Parse LightningRemark from a LightningMatcher.
     * <p>
     * The direction is either ALQDS (alqds group, sets allQuadrants) or a
     * direction chain (dirchain group) of one or more segments joined by AND,
     * each a single point or a hyphenated range. The chain is split with the
     * same {@link #parseDirectionChain(String)} used for thunderstorm locations,
     * so "SE AND S-SW" becomes [SE], [S, SW].
     *
     * @param matcher the lightning matcher positioned at a match
     * @return LightningRemark object with all extracted fields
     */
    private LightningRemark parseLightningFromMatcher(LightningMatcher matcher) {
        String freqStr = matcher.group("freq");
        LightningFrequency frequency = freqStr != null ? LightningFrequency.fromCode(freqStr) : null;

        List<String> types = extractLightningTypes(matcher);

        String location = matcher.group("loc");

        boolean allQuadrants = matcher.group("alqds") != null;
        List<DirectionSegment> directionSegments = parseDirectionChain(matcher.group(DIRECTION_CHAIN_CODE));

        return new LightningRemark(frequency, types, location, directionSegments, allQuadrants);
    }

    /**
     * Extract the ordered list of lightning discharge types from a matcher,
     * splitting LightningMatcher's raw types string (e.g. "CCCG") into its
     * individual 2-letter codes in the order they were reported.
     *
     * @param matcher the lightning matcher positioned at a match
     * @return list of type codes in source order, or empty list if none present
     */
    private List<String> extractLightningTypes(LightningMatcher matcher) {
        String typesStr = matcher.getTypesString();

        if (typesStr == null || typesStr.isEmpty()) {
            return List.of();
        }

        if (typesStr.length() % 2 != 0) {
            throw new IllegalArgumentException("Lightning types string has odd length, expected complete 2-letter codes: " + typesStr);
        }

        List<String> types = new ArrayList<>();
        for (int i = 0; i < typesStr.length(); i += 2) {
            types.add(typesStr.substring(i, i + 2));
        }

        return types;
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
     * @param remarks     the remarks builder to populate
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
            String matchedText = matcher.group();
            try {
                PressureTendency tendency = parsePressureTendencyFromMatcher(matcher);
                remarks.updatePressure(p -> p.withPressureTendency(tendency));

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("3-hour pressure tendency: {}", tendency.getSummary());
                }

                remaining = remaining.substring(matcher.end()).trim();

            } catch (InvalidObservationException e) {
                logRejectedRemark("pressure tendency", matchedText, e);
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
     * Handle pressure rising or falling rapidly (PRESRR/PRESFR).
     * <p>
     * Format: PRES followed by R (rising) or F (falling), then a second
     * letter that isn't semantically distinguished (commonly R, as in
     * PRESRR/PRESFR).
     * <p>
     * Examples:
     * - PRESFR → pressure falling rapidly
     * - PRESRR → pressure rising rapidly
     *
     * @param remarksText remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after processing (never null)
     */
    private String handlePressureRapidChangeSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText != null ? remarksText : "";
        }

        String remaining = remarksText.trim();
        Matcher matcher = PRES_RF_RAPDLY_PATTERN.matcher(remaining);

        if (matcher.find() && matcher.start() == 0) {
            String matchedText = matcher.group();
            try {
                PressureRapidChange change = PressureRapidChange.fromCode(matcher.group("presrisfal"));
                remarks.updatePressure(p -> p.withPressureRapidChange(change));

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Pressure rapid change: {}", change);
                }

                remaining = remaining.substring(matcher.end()).trim();

            } catch (InvalidObservationException e) {
                logRejectedRemark("pressure rapid change", matchedText, e);
            }
        }

        return remaining;
    }

    /**
     * Handle icing remark (ICG).
     * <p>
     * Format: ICG optionally followed by IC (icing in clouds) and/or IP
     * (icing in precipitation), then a free-form qualifier.
     * <p>
     * Example:
     * - ICG PAST HR → icing, qualifier "PAST HR"
     *
     * @param remarksText remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after processing (never null)
     */
    private String handleIcingSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText != null ? remarksText : "";
        }

        String remaining = remarksText.trim();
        Matcher matcher = ICING_PATTERN.matcher(remaining);

        if (matcher.find() && matcher.start() == 0) {
            String matchedText = matcher.group();
            try {
                Icing icing = new Icing(
                        matcher.group("typeic") != null,
                        matcher.group("typeip") != null,
                        matcher.group("extra"));
                remarks.icing(icing);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Icing: {}", icing.getSummary());
                }

                remaining = remaining.substring(matcher.end()).trim();

            } catch (InvalidObservationException e) {
                logRejectedRemark("icing remark", matchedText, e);
            }
        }

        return remaining;
    }

    /**
     * Handle secondary altimeter setting repeated inside the remarks section.
     * Common in the Philippines/Taiwan-region METARs where the main body reports
     * altimeter in ICAO hPa format (Q####) and remarks repeat it in US-style
     * inches of mercury (A####) as a redundant confirmation.
     * <p>
     * Example: RMK A2977 → 29.77 inHg
     *
     * @param remarksText remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after processing (never null)
     */
    private String handleSecondaryAltimeterSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText != null ? remarksText : "";
        }

        String remaining = remarksText.trim();
        Matcher matcher = SECONDARY_ALTIMETER_PATTERN.matcher(remaining);

        if (matcher.find() && matcher.start() == 0 && processSecondaryAltimeterMatch(matcher, remarks)) {
            remaining = remaining.substring(matcher.end()).trim();
        }

        return remaining;
    }

    /**
     * Process a single secondary altimeter pattern match, storing the parsed
     * pressure on the remarks builder if valid.
     *
     * @param matcher the pattern matcher positioned at a match (must not be null)
     * @param remarks the remarks builder (must not be null)
     * @return true if the group was recognized (stored, or reported as not
     * available with "////"); false if it was rejected and should be
     * left in the remarks text for freeText (#115)
     */
    private boolean processSecondaryAltimeterMatch(Matcher matcher, NoaaMetarRemarks.Builder remarks) {
        String matchedText = matcher.group();
        String pressureStr = matcher.group(GROUP_PRESSURE_CHANGE);

        if ("////".equals(pressureStr)) {
            // e.g. "A////": a recognized "not available" report, nothing to store
            return true;
        }

        try {
            Pressure pressure = parsePressure(matcher.group("unit"), pressureStr, matcher.group("unit2"));
            if (pressure == null) {
                LOGGER.debug("Secondary altimeter could not be parsed, leaving for freeText: {}", matchedText);
                return false;
            }

            remarks.secondaryAltimeter(pressure);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Secondary altimeter: {}", pressure.getSummary());
            }
            return true;

        } catch (InvalidObservationException e) {
            logRejectedRemark("secondary altimeter", matchedText, e);
            return false;
        }
    }

    /**
     * Handle 6-hour maximum/minimum temperature.
     * Format: 1sTTT (max) or 2sTTT (min)
     * where s=sign (0=positive, 1=negative), TTT=temp in tenths of degrees C.
     * <p>
     * Examples:
     * - 10142 → Maximum: 14.2°C
     * - 11023 → Maximum: -2.3°C
     * - 20012 → Minimum: 1.2°C
     * - 21001 → Minimum: -0.1°C
     *
     * @param remarksText remaining remarks text to process
     * @param remarks     the remarks builder to populate
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
            String matchedText = matcher.group();
            try {
                Temperature temperature = parse6HourTemperatureFromMatcher(matcher);
                add6HourTemperatureToRemarks(matcher.group("type"), temperature, remarks);

                remaining = remaining.substring(matcher.end()).trim();
                matcher = TEMP_6HR_MAX_MIN_PATTERN.matcher(remaining);

            } catch (InvalidObservationException e) {
                logRejectedRemark("6-hour max/min temperature", matchedText, e);
                break; // rejected: leave for freeText rather than re-matching it forever
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
     * @param type        temperature type ("1" = max, "2" = min)
     * @param temperature the parsed temperature
     * @param remarks     the remarks builder to populate
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
     * <p>
     * Examples:
     * - 400461006 → Max: 4.6°C, Min: -0.6°C
     * - 411231089 → Max: -12.3°C, Min: -8.9°C
     * - 400001000 → Max: 0.0°C, Min: -0.0°C
     *
     * @param remarksText remaining remarks text to process
     * @param remarks     the remarks builder to populate
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
            String matchedText = matcher.group();
            try {
                Temperature maxTemp = parse24HourMaxTemperatureFromMatcher(matcher);
                Temperature minTemp = parse24HourMinTemperatureFromMatcher(matcher);

                remarks.twentyFourHourMaxTemperature(maxTemp);
                remarks.twentyFourHourMinTemperature(minTemp);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("24-hour max/min temperature: {} / {}",
                            maxTemp.getSummary(), minTemp.getSummary());
                }

                remaining = remaining.substring(matcher.end()).trim();

            } catch (InvalidObservationException e) {
                logRejectedRemark("24-hour max/min temperature", matchedText, e);
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
     * <p>
     * Examples:
     * - CIG 005V010 → 500-1000 feet
     * - CIG 020V035 → 2000-3500 feet
     *
     * @param remarksText remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after processing
     */
    private String handleVariableCeilingSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText;
        }

        String remaining = remarksText.trim();
        Matcher matcher = VARIABLE_CEILING_PATTERN.matcher(remaining);

        if (matcher.find() && matcher.start() == 0) {
            String matchedText = matcher.group();
            try {
                int minHundreds = Integer.parseInt(matcher.group("min"));
                int maxHundreds = Integer.parseInt(matcher.group("max"));

                VariableCeiling variableCeiling = VariableCeiling.fromHundreds(minHundreds, maxHundreds);
                remarks.updateCeiling(c -> c.withVariableCeiling(variableCeiling));

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Variable ceiling: {}", variableCeiling.getSummary());
                }

                remaining = remaining.substring(matcher.end()).trim();

            } catch (InvalidObservationException | NumberFormatException e) {
                logRejectedRemark("variable ceiling", matchedText, e);
            }
        }

        return remaining;
    }

    /**
     * Handle ceiling height at second site.
     * Format: CIG height [LOC] where height is in hundreds of feet.
     * <p>
     * Examples:
     * - CIG 002 RY11 → 200 ft at runway 11
     * - CIG 005 RWY06 → 500 ft at runway 06
     * - CIG 010 → 1000 ft (no location)
     * <p>
     * IMPORTANT: This handler must be called AFTER handleVariableCeilingSequential
     * to avoid matching variable ceiling patterns (e.g., CIG 005V010).
     *
     * @param remarksText remaining remarks text to process
     * @param remarks     the remarks builder to populate
     * @return the remaining text after processing
     */
    private String handleCeilingSecondSiteSequential(String remarksText, NoaaMetarRemarks.Builder remarks) {
        if (remarksText == null || remarksText.trim().isEmpty()) {
            return remarksText;
        }

        String remaining = remarksText.trim();
        Matcher matcher = CEILING_SECOND_SITE_PATTERN.matcher(remaining);

        if (matcher.find() && matcher.start() == 0) {
            String matchedText = matcher.group();
            try {
                int hundreds = Integer.parseInt(matcher.group(GROUP_HEIGHT_CODE));
                String location = matcher.group("loc");

                CeilingSecondSite ceilingSecondSite = CeilingSecondSite.fromHundreds(hundreds, location);
                remarks.updateCeiling(c -> c.withCeilingSecondSite(ceilingSecondSite));

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Ceiling second site: {}", ceilingSecondSite.getSummary());
                }

                remaining = remaining.substring(matcher.end()).trim();

            } catch (InvalidObservationException | NumberFormatException e) {
                logRejectedRemark("ceiling second site", matchedText, e);
            }
        }

        return remaining;
    }

    /**
     * Handle obscuration layers.
     * Format: [Coverage] [Phenomenon] [Height]
     * <p>
     * Examples:
     * - FEW FG 000 → Few fog at ground level
     * - SCT FU 010 → Scattered smoke at 1000 feet
     * <p>
     * Multiple layers can be present: FEW FG 000 SCT FU 010
     *
     * @param remarksText remaining remarks text to process
     * @param remarks     the remarks builder to populate
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
            String matchedText = matcher.group();
            try {
                int hundreds = Integer.parseInt(matcher.group(GROUP_HEIGHT_CODE));
                ObscurationLayer layer = ObscurationLayer.fromHundreds(
                        matcher.group("coverage"), matcher.group("phenomenon"), hundreds);
                remarks.addObscurationLayer(layer);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Obscuration layer: {}", layer.getSummary());
                }

                remaining = remaining.substring(matcher.end()).trim();
                matcher = OBSCURATION_PATTERN.matcher(remaining);

            } catch (InvalidObservationException | NumberFormatException e) {
                logRejectedRemark("obscuration layer", matchedText, e);
                break; // rejected: leave for freeText rather than re-matching it forever
            }
        }

        return remaining;
    }

    /**
     * Handle cloud type observations in okta format.
     * Format: [Intensity] CloudType [Oktas] [Location/Movement]
     * <p>
     * Examples:
     * - SC1 → Stratocumulus 1 okta
     * - SC TR → Stratocumulus trace
     * - MDT CU OHD → Moderate cumulus overhead
     * - CI MOVG NE → Cirrus moving northeast
     * <p>
     * Multiple cloud types can be present: SC1 AC2 CI
     *
     * @param remarksText remaining remarks text to process
     * @param remarks     the remarks builder to populate
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
            String next = processCloudTypeMatch(matcher, matcher.end(), remaining, remarks).trim();
            if (next.equals(remaining)) {
                break; // rejected: leave the text for another handler or the freeText peeler
            }
            remaining = next;
            matcher = CLOUD_OKTA_PATTERN.matcher(remaining);
        }

        return remaining;
    }

    /**
     * Process a single cloud type pattern match.
     *
     * @param matcher   the pattern matcher (must not be null)
     * @param matchEnd  the end position of the match
     * @param remaining the remaining text (must not be null)
     * @param remarks   the remarks builder (must not be null)
     * @return the remaining text after processing this match (never null)
     */
    private String processCloudTypeMatch(Matcher matcher, int matchEnd, String remaining,
                                         NoaaMetarRemarks.Builder remarks) {
        String result = remaining;   // default: rejected, leave text in place
        try {
            CloudType cloudType = extractCloudTypeFromMatcher(matcher);
            remarks.addCloudType(cloudType);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Cloud type: {}", cloudType.getSummary());
            }
            result = remaining.substring(matchEnd).trim();   // accepted: advance

        } catch (InvalidObservationException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Cloud type match rejected, leaving in place: {}",
                        remaining.substring(0, Math.min(matchEnd, remaining.length())));
            }
        }

        // Always return remaining text (never null)
        return result;
    }

    /**
     * Extract cloud type components from a pattern matcher.
     * Validates that at least one optional field (oktas, intensity, location, movement) is present.
     *
     * @param matcher the pattern matcher (must not be null)
     * @return the constructed CloudType (never null)
     * @throws InvalidObservationException if no optional field (oktas, intensity, location, movement) is present
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
            throw new InvalidObservationException("CloudType.qualifiers", cloudTypeCode,
                    "Cloud type must have at least one optional field (oktas, intensity, location, or movement)");
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
     * @param oktaStr the oktas string from the pattern (it may be null)
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
     * @param verb              the verb group (it may be null)
     * @param directionMovement the movement direction (it may be null)
     * @param directionLocation the location qualifier (it may be null)
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
     * @param str the string to trim (it may be null)
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
     * @param remarks     the remarks builder
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
            if (!processAutomatedMaintenance(matcher, remarks)) {
                break; // rejected: leave for freeText rather than re-matching it forever
            }
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
     * @return true if stored; false if rejected and left for freeText (#115)
     */
    private boolean processAutomatedMaintenance(Matcher matcher, NoaaMetarRemarks.Builder remarks) {
        String matchedText = matcher.group();
        try {
            String typeAM = matcher.group("typeam");  // Automated maintenance type
            String typeMC = matcher.group("typemc");  // Maintenance check ($)
            String location = matcher.group("loc");   // Optional location

            if (typeMC != null) {
                remarks.updateMaintenance(m -> m.withMaintenanceRequired(true));
                remarks.updateMaintenance(m ->
                        m.addAutomatedMaintenanceIndicator(AutomatedMaintenanceIndicator.maintenanceCheck()));
                LOGGER.debug("Maintenance check indicator ($) found");
            } else if (typeAM != null) {
                AutomatedMaintenanceIndicator indicator =
                        location != null
                                ? AutomatedMaintenanceIndicator.of(typeAM, location)
                                : AutomatedMaintenanceIndicator.of(typeAM);

                remarks.updateMaintenance(m -> m.addAutomatedMaintenanceIndicator(indicator));
                LOGGER.debug("Automated maintenance indicator: {} {}",
                        typeAM,
                        location != null ? location : "(no location)");
            }
            return true;

        } catch (InvalidObservationException e) {
            logRejectedRemark("automated maintenance indicator", matchedText, e);
            return false;
        }
    }
}
