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
package weather.processing.parser.noaa;

import weather.model.NoaaMetarData;
import weather.model.NoaaWeatherData;
import weather.model.components.*;
import weather.model.components.remark.*;
import weather.model.enums.AutomatedStationType;
import weather.model.enums.PressureUnit;
import weather.model.enums.SkyCoverage;
import weather.processing.parser.common.WeatherParser;
import weather.processing.parser.common.ParseResult;
import weather.utils.IndexedLinkedHashMap;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static weather.processing.parser.noaa.RegExprConst.*;

/**
 * Parser for NOAA METAR (Meteorological Aerodrome Report) data.
 * 
 * Uses pattern-driven parsing approach via MetarPatternRegistry where patterns 
 * are applied in order to progressively consume the METAR token string.
 * 
 * METAR Format Example:
 * "2025/11/14 22:52
 *  METAR KJFK 142252Z 19005KT 10SM FEW100 FEW250 16/M03 A3012 RMK AO2 SLP214 T01611028"
 * 
 * Components:
 * - METAR: Report type
 * - KJFK: Station identifier (JFK Airport)
 * - 251651Z: Day (25th) and time (16:51 UTC)
 * - 28016KT: Wind from 280° at 16 knots
 * - 10SM: Visibility 10 statute miles
 * - FEW250: Few clouds at 25,000 feet
 * - 22/12: Temperature 22°C, Dewpoint 12°C
 * - A3015: Altimeter setting 30.15 inHg
 * - RMK: Remarks section
 * 
 * @author bclasky1539
 *
 */
public class NoaaMetarParser implements WeatherParser<NoaaWeatherData> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NoaaMetarParser.class);
    private static final String RVR_PREFIX_PATTERN = "^[MP]";
    
    // Pattern registry for METAR parsing
    private final MetarPatternRegistry patternRegistry;
    
    // Working data structure during parsing
    private NoaaWeatherData weatherData;
    private Instant issueTime;
    private String reportType;
    
    public NoaaMetarParser() {
        this.patternRegistry = new MetarPatternRegistry();
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
            // Initialize working data
            this.weatherData = null;
            this.issueTime = null;
            this.reportType = "METAR";  // default to METAR
            
            String token = rawData.trim();
            
            // Parse main METAR body (everything before RMK)
            String[] parts = token.split("\\s+RMK\\s+", 2);
            String mainBody = parts[0];
            String remarks = parts.length > 1 ? parts[1] : "";
            
            // Get handlers from registry
            IndexedLinkedHashMap<Pattern, MetarPatternHandler> mainHandlers = 
                patternRegistry.getMainHandlers();
            IndexedLinkedHashMap<Pattern, MetarPatternHandler> remarkHandlers = 
                patternRegistry.getRemarksHandlers();
            
            // Process main handlers
            mainBody = parseWithHandlers(mainBody, mainHandlers, "MAIN");
            
            // Process remark handlers if present
            if (!remarks.isEmpty()) {
                String originalRemarks = remarks;
                remarks = parseWithHandlers(remarks, remarkHandlers, "REMARK");
                //System.out.println("\n=== DEBUG INFO ===");
                //System.out.println("Remarks: " + remarks);
                //System.out.println("originalRemarks: " + originalRemarks);
                //System.out.println("=== DEBUG INFO ===");
                // Also do sequential parsing of remarks for components not in registry
                handleRemarks(originalRemarks, (NoaaMetarData) weatherData);
            }
            
            // Validate that we extracted minimum required data
            if (weatherData == null || weatherData.getStationId() == null) {
                return ParseResult.failure("Could not extract station ID from METAR");
            }
            
            // Store raw data
            weatherData.setRawData(rawData.trim());
            
            // Log any unparsed tokens
            if (LOGGER.isDebugEnabled() && !mainBody.trim().isEmpty()) {
                    LOGGER.debug("Unparsed main body tokens: '{}'", mainBody.trim());
            }

            if (LOGGER.isDebugEnabled() && !remarks.trim().isEmpty()) {
                    LOGGER.debug("Unparsed remark tokens: '{}'", remarks.trim());
            }

            return ParseResult.success(weatherData);
            
        } catch (Exception e) {
            return ParseResult.failure(
                "Failed to parse METAR data: " + e.getMessage(), 
                e
            );
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
     * Similar to legacy parseAviaHandlers method.
     * 
     * @param token The token string to parse
     * @param handlers Ordered map of patterns and their handlers from MetarPatternRegistry
     * @param handlersType Type of handlers ("MAIN" or "REMARK") for logging
     * @return Remaining unparsed token string
     */
    private String parseWithHandlers(String token, 
                                     IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers, 
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
                                   IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers) {
        for (Map.Entry<Pattern, MetarPatternHandler> entry : handlers.entrySet()) {
            Pattern pattern = entry.getKey();                     // Get key from entry
            MetarPatternHandler handlerInfo = entry.getValue();  // Get value from entry

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
    private String tryPattern(String token, Pattern pattern, MetarPatternHandler handlerInfo) {
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
     * @param handlerName The name of the handler from MetarPatternRegistry
     * @param matcher The regex matcher with captured groups
     */
    private void handlePattern(String handlerName, Matcher matcher) {

        try {
            switch (handlerName) {
                case "reportType" -> handleReportType(matcher);
                case "monthDayYear" -> handleIssueDateTime(matcher);
                case "station" -> handleStationAndObsTime(matcher);
                case "reportModifier" -> handleReportModifier(matcher);
                case "wind" -> handleWind(matcher);
                case "visibility" -> handleVisibility(matcher);
                case "runway" -> handleRunway(matcher);
                case "presentWeather" -> handlePresentWeather(matcher);
                case "skyCondition" -> handleSkyCondition(matcher);
                case "tempDewpoint" -> handleTempDewpoint(matcher);
                case "altimeter" -> handleAltimeter(matcher);
                case "noSigChange" -> handleNoSigChange(matcher);
                case "unparsed" -> handleUnparsed(matcher);
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
    
    // ==================== PATTERN HANDLERS ====================

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
        
        // Create NoaaWeatherData instance
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
    
    /**
     * Handle wind: "19005KT" or "19005G15KT"
     */
    private void handleWind(Matcher matcher) {
        if (weatherData == null) {
            return;
        }
        
        String directionStr = matcher.group("dir");
        String speedStr = matcher.group("speed");
        String gustStr = matcher.group("gust");
        String unitStr = matcher.group("units");
        
        // Parse direction (handle VRB = variable, null for calm)
        Integer direction = null;
        if (directionStr != null && !directionStr.equals("VRB")) {
            direction = Integer.parseInt(directionStr);
        }
        
        // Parse speed (required)
        Integer speed = speedStr != null ? Integer.parseInt(speedStr) : null;
        
        // Parse gust (optional)
        Integer gust = null;
        if (gustStr != null && !gustStr.isEmpty()) {
            gust = Integer.parseInt(gustStr);
        }
        
        // Parse unit (default to KT if not specified)
        String unit = unitStr != null ? unitStr : "KT";
        
        // Let WeatherData create the Wind object (encapsulation)
        weatherData.setWind(direction, speed, gust, unit);
        
        LOGGER.debug("Wind - Dir: {}, Speed: {}, Gust: {}, Unit: {}", 
            direction, speed, gust, unit);
    }

    /**
     * Handle visibility: "10SM", "9999", "1/2SM", "CAVOK", etc.
     */
    private void handleVisibility(Matcher matcher) {
        if (weatherData == null) {
            return;
        }

        // Try special conditions first (CAVOK, NDV)
        if (handleSpecialVisibility(matcher)) {
            return;
        }

        // Try international format (meters)
        if (handleInternationalVisibility(matcher)) {
            return;
        }

        // Try US format (statute miles, etc.)
        handleUSVisibility(matcher);
    }

    /**
     * Handle special visibility conditions (CAVOK, NDV).
     *
     * @param matcher Regex matcher
     * @return true if special condition was handled
     */
    private boolean handleSpecialVisibility(Matcher matcher) {
        String visGroup = matcher.group("vis");
        if (visGroup == null) {
            return false;
        }

        if ("CAVOK".equals(visGroup)) {
            weatherData.setVisibilityCavok();
            LOGGER.debug("Visibility: CAVOK");
            return true;
        }

        if ("NDV".equals(visGroup)) {
            weatherData.setVisibility(null, null, false, false, "NDV");
            LOGGER.debug("Visibility: NDV");
            return true;
        }

        return false;
    }

    /**
     * Handle international visibility format (meters with optional M/P prefix).
     *
     * @param matcher Regex matcher
     * @return true if international format was handled
     */
    private boolean handleInternationalVisibility(Matcher matcher) {
        String distStr = matcher.group("dist");
        if (distStr == null || distStr.equals("////")) {
            return false;
        }

        boolean lessThan = distStr.startsWith("M");
        boolean greaterThan = distStr.startsWith("P");

        // Remove M/P prefix if present
        String numStr = distStr.replaceFirst(RVR_PREFIX_PATTERN, "");

        try {
            double distance = Double.parseDouble(numStr);
            weatherData.setVisibility(distance, "M", lessThan, greaterThan);

            LOGGER.debug("Visibility: {} meters (lessThan={}, greaterThan={})",
                    distance, lessThan, greaterThan);
            return true;
        } catch (NumberFormatException e) {
            logVisibilityParseError(distStr, e);
            return false;
        }
    }

    /**
     * Handle US visibility format (statute miles with fractions).
     *
     * @param matcher Regex matcher
     */
    private void handleUSVisibility(Matcher matcher) {
        String distU = matcher.group("distu");
        String unit = matcher.group("units");

        if (distU == null || unit == null) {
            return;
        }

        boolean lessThan = distU.startsWith("M");
        boolean greaterThan = distU.startsWith("P");

        // Remove M/P prefix if present
        String numStr = distU.replaceFirst(RVR_PREFIX_PATTERN, "").trim();

        try {
            double distance = parseFractionalDistance(numStr);
            weatherData.setVisibility(distance, unit, lessThan, greaterThan);

            LOGGER.debug("Visibility: {} {} (lessThan={}, greaterThan={})",
                    distance, unit, lessThan, greaterThan);
        } catch (NumberFormatException e) {
            logVisibilityParseError(distU, e);
        }
    }

    /**
     * Log visibility parsing error with consistent message format.
     *
     * @param distanceStr The distance string that failed to parse
     * @param e The NumberFormatException that occurred
     */
    private void logVisibilityParseError(String distanceStr, NumberFormatException e) {
        LOGGER.warn("Failed to parse visibility distance: '{}' - {}", distanceStr, e.getMessage());
    }

    /**
     * Parse fractional distance values like "1/2", "1 1/2", or "10".
     *
     * @param distStr Distance string (e.g., "10", "1/2", "1 1/2")
     * @return Parsed distance as double
     */
    private double parseFractionalDistance(String distStr) {
        // Handle mixed fractions: "1 1/2" → 1.5
        if (distStr.contains(" ")) {
            String[] parts = distStr.split("\\s+");
            double whole = Double.parseDouble(parts[0]);
            double fraction = parseFraction(parts[1]);
            return whole + fraction;
        }

        // Handle pure fractions: "1/2" → 0.5
        if (distStr.contains("/")) {
            return parseFraction(distStr);
        }

        // Handle whole numbers: "10" → 10.0
        return Double.parseDouble(distStr);
    }

    /**
     * Parse a fraction string like "1/2" to decimal.
     *
     * @param fraction Fraction string (e.g., "1/2", "3/4")
     * @return Decimal value
     */
    private double parseFraction(String fraction) {
        String[] parts = fraction.split("/");
        double numerator = Double.parseDouble(parts[0]);
        double denominator = Double.parseDouble(parts[1]);
        return numerator / denominator;
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
     * Handle runway visual range (RVR): "R22R/0400N", "R24/P2000N", "R23L/0900V6000FT", etc.
     */
    private void handleRunway(Matcher matcher) {
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
        String highNumStr = highStr.replaceFirst(RVR_PREFIX_PATTERN, "");
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
     * Handle present weather phenomena.
     * Parses weather codes like: -RA, +TSRA, VCFG, BR, NSW
     */
    private void handlePresentWeather(Matcher matcher) {
        if (weatherData == null) {
            return;
        }

        String weatherString = matcher.group(0).trim();

        try {
            PresentWeather presentWeather = PresentWeather.parse(weatherString);
            weatherData.addPresentWeather(presentWeather);

            // Determine primary phenomenon for logging
            String phenomenon = getPrimaryPhenomenon(presentWeather);

            LOGGER.debug("Present Weather: {} (Intensity: {}, Descriptor: {}, Phenomena: {})",
                    weatherString,
                    presentWeather.intensity(),
                    presentWeather.descriptor(),
                    phenomenon);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to parse present weather '{}': {}", weatherString, e.getMessage());
        }
    }

    /**
     * Get the primary weather phenomenon for logging.
     */
    private String getPrimaryPhenomenon(PresentWeather weather) {
        if (weather.hasPrecipitation()) {
            return weather.precipitation();
        }
        if (weather.hasObscuration()) {
            return weather.obscuration();
        }
        return weather.other();
    }

    /**
     * Handle sky condition (cloud layers).
     * Parses sky condition codes like: FEW250, SCT100, BKN050CB, OVC020, SKC, VV008
     *
     * Format: [COVERAGE][HEIGHT][TYPE]
     * - Coverage: SKC, CLR, FEW, SCT, BKN, OVC, VV, etc.
     * - Height: 3-4 digits (in hundreds of feet)
     * - Type: CB (cumulonimbus), TCU (towering cumulus), etc.
     */
    private void handleSkyCondition(Matcher matcher) {
        if (weatherData == null) {
            return;
        }

        String coverageStr = matcher.group("cover");
        String heightStr = matcher.group("height");
        String cloudTypeStr = matcher.group("cloud");

        // Handle unknown/missing coverage (///)
        if ("///".equals(coverageStr)) {
            LOGGER.debug("Sky condition: Unknown coverage (///)");
            return;
        }

        try {
            // Parse coverage
            SkyCoverage coverage = parseCoverage(coverageStr);

            // Parse height (if present and not unknown)
            Integer heightFeet = parseHeight(heightStr, coverage);

            // Parse cloud type (if present and not unknown)
            String cloudType = parseCloudType(cloudTypeStr);

            // Create and add sky condition
            SkyCondition skyCondition = new SkyCondition(coverage, heightFeet, cloudType);
            weatherData.addSkyCondition(skyCondition);

            LOGGER.debug("Sky Condition: {} at {} feet{}",
                    coverage,
                    heightFeet != null ? heightFeet : "N/A",
                    cloudType != null ? " (" + cloudType + ")" : "");

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to parse sky condition '{}': {}", matcher.group(0).trim(), e.getMessage());
        }
    }

    /**
     * Parse sky coverage string to SkyCoverage enum.
     * Handles various formats and common OCR errors (O→0, SCK→SKC).
     *
     * @param coverageStr the coverage string from METAR
     * @return SkyCoverage enum
     * @throws IllegalArgumentException if coverage is invalid
     */
    private SkyCoverage parseCoverage(String coverageStr) {
        if (coverageStr == null || coverageStr.isBlank()) {
            throw new IllegalArgumentException("Coverage cannot be null or blank");
        }

        String normalized = coverageStr.trim().toUpperCase();

        // Handle common OCR/parsing errors
        normalized = normalized.replace("0VC", "OVC")  // 0→O
                .replace("SCK", "SKC"); // K→C

        return switch (normalized) {
            case "SKC" -> SkyCoverage.SKC;           // Sky Clear
            case "CLR" -> SkyCoverage.CLR;           // Clear
            case "NSC" -> SkyCoverage.NSC;           // No Significant Clouds
            case "NCD" -> SkyCoverage.NSC;           // No Cloud Detected (treat as NSC)
            case "FEW" -> SkyCoverage.FEW;           // Few (1/8 to 2/8)
            case "SCT" -> SkyCoverage.SCATTERED;     // Scattered (3/8 to 4/8)
            case "BKN" -> SkyCoverage.BROKEN;        // Broken (5/8 to 7/8)
            case "OVC" -> SkyCoverage.OVERCAST;      // Overcast (8/8)
            case "VV" -> SkyCoverage.VERTICAL_VISIBILITY;  // Vertical Visibility
            default -> throw new IllegalArgumentException("Unknown sky coverage: " + coverageStr);
        };
    }

    /**
     * Parse cloud height from METAR format.
     * Heights are encoded as hundreds of feet (e.g., "050" = 5000 feet).
     *
     * @param heightStr the height string from METAR (could be null)
     * @param coverage the sky coverage (used for validation)
     * @return height in feet, or null if not applicable
     */
    private Integer parseHeight(String heightStr, SkyCoverage coverage) {
        // Clear sky conditions should not have height
        if (coverage == SkyCoverage.SKC || coverage == SkyCoverage.CLR || coverage == SkyCoverage.NSC) {
            return null;
        }

        // Handle unknown/missing height (///)
        if (heightStr == null || heightStr.isBlank() || "///".equals(heightStr)) {
            // Vertical visibility requires height
            if (coverage == SkyCoverage.VERTICAL_VISIBILITY) {
                throw new IllegalArgumentException("Vertical visibility must have height specified");
            }
            return null;
        }

        try {
            // Replace common OCR error: O→0
            String normalized = heightStr.replace('O', '0');

            // Parse the number and convert to feet
            int heightCode = Integer.parseInt(normalized);

            // Height code is in hundreds of feet
            return heightCode * 100;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid cloud height format: " + heightStr);
        }
    }

    /**
     * Parse cloud type from METAR format.
     * Common types: CB (cumulonimbus), TCU (towering cumulus).
     *
     * @param cloudTypeStr the cloud type string from METAR (could be null)
     * @return cloud type, or null if not present
     */
    private String parseCloudType(String cloudTypeStr) {
        // Handle unknown/missing cloud type (///)
        if (cloudTypeStr == null || cloudTypeStr.isBlank() || "///".equals(cloudTypeStr)) {
            return null;
        }

        return cloudTypeStr.trim().toUpperCase();
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
                Temperature temperature = new Temperature(temperatureCelsius, dewpointCelsius);
                weatherData.setTemperature(temperature);

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
        String pressureStr = matcher.group("press");
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
                weatherData.setPressure(pressure);

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

            // Future: Store in NoaaMetarRemarks once sequential parsing is implemented

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid automated station type: {}", matcher.group(0), e);
        }
    }

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
            remaining = handleTowerSurfaceVisibilitySequential(remaining, remarksBuilder);
            remaining = handleHourlyPrecipitationSequential(remaining, remarksBuilder);
            remaining = handleMultiHourPrecipitationSequential(remaining, remarksBuilder);
            remaining = handleHailSizeSequential(remaining, remarksBuilder);

            // Continue while we're making progress
        } while (!remaining.equals(previous));

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
            String pressureStr = matcher.group("press");

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
        // Precondition: remarksText is non-null and non-blank

        Matcher matcher = TWR_SFC_VIS_PATTERN.matcher(remarksText);

        if (!matcher.find()) {
            return remarksText;
        }

        try {
            // Extract type (TWR VIS or SFC VIS)
            String type = matcher.group("type");

            // Extract distance
            String distStr = matcher.group("dist");
            if (distStr == null || distStr.isBlank()) {
                LOGGER.debug("Tower/Surface visibility missing distance, skipping");
                return remarksText.substring(matcher.end()).trim();
            }

            // Parse visibility distance (reuse existing helper!)
            Visibility visibility = parseVisibilityDistance(distStr);
            if (visibility == null) {
                LOGGER.warn("Failed to parse tower/surface visibility: {}", distStr);
                return remarksText.substring(matcher.end()).trim();
            }

            // Determine which type and set appropriately
            if ("TWR VIS".equals(type)) {
                remarks.towerVisibility(visibility);
                LOGGER.debug("Tower visibility: {}", distStr);
            } else if ("SFC VIS".equals(type)) {
                remarks.surfaceVisibility(visibility);
                LOGGER.debug("Surface visibility: {}", distStr);
            }

            return remarksText.substring(matcher.end()).trim();

        } catch (Exception e) {
            LOGGER.warn("Invalid tower/surface visibility in remarks: {}",
                    remarksText.substring(0, Math.min(30, remarksText.length())), e);
            return remarksText.substring(matcher.end()).trim();
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

        } catch (Exception e) {
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

        } catch (Exception e) {
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

        } catch (Exception e) {
            LOGGER.warn("Invalid hail size: {}",
                    remarksText.substring(0, Math.min(20, remarksText.length())), e);
            return remarksText.substring(matcher.end()).trim();
        }
    }

    // ==================== STUB HANDLERS (to be implemented) ====================

    private void handleNoSigChange(Matcher matcher) {
        LOGGER.debug("TODO: Implement no significant change handler {}", matcher);
    }
    
    private void handleUnparsed(Matcher matcher) {
        LOGGER.debug("Unparsed token: '{}'", matcher);
    }
}
