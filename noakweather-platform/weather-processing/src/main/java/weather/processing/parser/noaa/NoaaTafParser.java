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

import weather.model.NoaaTafData;
import weather.model.NoaaWeatherData;
import weather.model.WeatherConditions;
import weather.model.components.*;
import weather.model.enums.ChangeIndicator;
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
 * Parser for NOAA TAF (Terminal Aerodrome Forecast) data.
 *
 * Extends NoaaAviationWeatherParser to inherit shared aviation weather parsing logic
 * for wind, visibility, present weather, sky conditions, and RVR.
 *
 * TAF Format Example:
 * "TAF AMD KCLT 151953Z 1520/1624 VRB02KT P6SM FEW250
 *       FM152100 21005KT P6SM SCT250
 *       TEMPO 3003/3011 P6SM -SHSN BKN040"
 *
 * Components:
 * - TAF: Report type
 * - AMD: Amendment indicator (optional)
 * - KCLT: Station identifier
 * - 151953Z: Issue time (15th at 19:53 UTC)
 * - 1520/1624: Validity period (15th 2000Z to 16th 2400Z)
 * - BASE forecast: VRB02KT P6SM FEW250
 * - FM (From): Permanent change at exact time
 * - TEMPO: Temporary fluctuations during period
 * - BECMG: Gradual change during period
 * - PROB30/PROB40: Probabilistic conditions
 *
 * Architecture:
 * - Parses TAF into base forecast + change groups
 * - Each period (base, FM, TEMPO, etc.) becomes a ForecastPeriod
 * - Inherits aviation weather handlers from NoaaAviationWeatherParser
 * - Stores result in NoaaTafData model
 *
 * @author bclasky1539
 */
public class NoaaTafParser extends NoaaAviationWeatherParser<NoaaTafData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoaaTafParser.class);

    // Pattern registry for METAR-like weather elements
    private final NoaaAviationWeatherPatternRegistry patternRegistry;

    // TAF-specific state
    private Instant issueTime;
    private LocalDateTime issueDateTime;
    private ValidityPeriod validityPeriod;

    // Current forecast period being built
    private ChangeIndicator currentChangeIndicator;
    private Instant currentChangeTime;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Integer currentProbability;

    public NoaaTafParser() {
        this.patternRegistry = new NoaaAviationWeatherPatternRegistry();
    }

    @Override
    public ParseResult<NoaaWeatherData> parse(String rawData) {
        if (rawData == null || rawData.trim().isEmpty()) {
            return ParseResult.failure("Raw data cannot be null or empty");
        }

        if (!canParse(rawData)) {
            return ParseResult.failure("Data is not a valid TAF report");
        }

        try {
            initializeParsingState();

            String token = rawData.trim();
            String[] parts = splitMainBodyAndRemarks(token);
            String mainBody = parts[0];
            String remarks = parts[1];

            // Parse TAF-specific header
            mainBody = parseTafHeader(mainBody);

            // Parse forecast periods (BASE + change groups)
            mainBody = parseForecastPeriods(mainBody);

            // Parse temperature forecasts (TX/TN)
            mainBody = parseTemperatureForecasts(mainBody);

            // Parse remarks if present
            if (!remarks.isEmpty()) {
                parseRemarks(remarks);
            }

            validateParsedData();

            weatherData.setRawText(rawData.trim());

            logUnparsedTokens(mainBody, remarks);

            return ParseResult.success(weatherData);

        } catch (IllegalStateException | IllegalArgumentException e) {
            return ParseResult.failure(
                    "Failed to parse TAF data: " + e.getMessage(), e
            );
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected error parsing TAF data", e);
            return ParseResult.failure(
                    "Unexpected parsing error: " + e.getMessage(), e
            );
        }
    }

    @Override
    public boolean canParse(String rawData) {
        if (rawData == null || rawData.trim().isEmpty()) {
            return false;
        }

        String trimmed = rawData.trim();

        // Check if starts with date/time pattern followed by TAF
        if (TAF_WITH_DATE_PREFIX.matcher(trimmed).find()) {
            return true;
        }

        // Check if TAF appears at the start
        return TAF_KEYWORD_START.matcher(trimmed).lookingAt();
    }

    @Override
    public String getSourceType() {
        return "NOAA_TAF";
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initialize all parsing state fields.
     * Calls base class to initialize shared state.
     */
    private void initializeParsingState() {
        initializeSharedState();  // Initialize base class state
        this.weatherData = new NoaaTafData();
        this.issueTime = null;
        this.issueDateTime = null;
        this.validityPeriod = null;
        resetCurrentPeriod();
    }

    /**
     * Reset state for parsing a new forecast period.
     * Calls base class to reset shared condition state.
     */
    private void resetCurrentPeriod() {
        initializeSharedState();  // Reset base class state for new period
        this.currentChangeIndicator = null;
        this.currentChangeTime = null;
        this.currentPeriodStart = null;
        this.currentPeriodEnd = null;
        this.currentProbability = null;
    }

    // ==================== MAIN PARSING LOGIC ====================

    /**
     * Split TAF into main body and remarks sections.
     */
    private String[] splitMainBodyAndRemarks(String token) {
        // TAF remarks are introduced by "RMK"
        String[] parts = REMARKS_SEPARATOR.split(token, 2);
        String mainBody = parts[0];
        String remarks = parts.length > 1 ? parts[1] : "";
        return new String[]{mainBody, remarks};
    }

    /**
     * Parse TAF-specific header: type, modifier, station, issue time, validity.
     *
     * Example: "TAF AMD KCLT 151953Z 1520/1624"
     */
    private String parseTafHeader(String token) {
        // Parse optional issue date/time at beginning
        token = parseIssueDateTime(token);

        // Parse report type (TAF)
        token = parseReportType(token);

        // Parse optional modifier (AMD, COR)
        token = parseModifier(token);

        // Parse station ID and issue time
        token = parseStationAndIssueTime(token);

        // Parse validity period (required for TAF)
        token = parseValidityPeriod(token);

        return token;
    }

    /**
     * Parse optional issue date/time: "2025/12/15 20:57"
     */
    private String parseIssueDateTime(String token) {
        Matcher matcher = MONTH_DAY_YEAR_PATTERN.matcher(token);

        if (matcher.find()) {
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

            this.issueDateTime = LocalDateTime.of(year, month, day, hour, minute);
            this.issueTime = issueDateTime.toInstant(ZoneOffset.UTC);

            LOGGER.debug("Parsed external issue time: {}", issueTime);

            return token.substring(matcher.end()).trim();
        }

        return token;
    }

    /**
     * Parse report type: "TAF"
     */
    private String parseReportType(String token) {
        if (token.startsWith("TAF ")) {
            weatherData.setReportType("TAF");
            LOGGER.debug("Report type: TAF");
            return token.substring(4).trim();
        }
        return token;
    }

    /**
     * Parse optional modifier: "AMD" or "COR"
     */
    private String parseModifier(String token) {
        if (token.startsWith("AMD ")) {
            weatherData.setReportModifier("AMD");
            LOGGER.debug("Modifier: AMD");
            return token.substring(4).trim();
        } else if (token.startsWith("COR ")) {
            weatherData.setReportModifier("COR");
            LOGGER.debug("Modifier: COR");
            return token.substring(4).trim();
        }
        return token;
    }

    /**
     * Parse station ID and issue time: "KCLT 151953Z"
     */
    private String parseStationAndIssueTime(String token) {
        Matcher matcher = STATION_AND_ISSUE_TIME_PATTERN.matcher(token);

        if (!matcher.find()) {
            throw new IllegalStateException("Could not parse station ID and issue time");
        }

        String stationId = matcher.group("station");
        int day = Integer.parseInt(matcher.group("zday"));
        int hour = Integer.parseInt(matcher.group("zhour"));
        int minute = Integer.parseInt(matcher.group("zmin"));

        // Determine year and month from external issue time or current time
        LocalDateTime referenceTime = issueDateTime != null ?
                issueDateTime :
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

        LocalDateTime issueLocalDateTime = LocalDateTime.of(year, month, day, hour, minute);
        this.issueTime = issueLocalDateTime.toInstant(ZoneOffset.UTC);

        weatherData.setStationId(stationId);
        weatherData.setIssueTime(issueTime);
        weatherData.setObservationTime(issueTime); // For base class

        LOGGER.debug("Station: {}, Issue time: {}", stationId, issueTime);

        return token.substring(matcher.end()).trim();
    }

    /**
     * Parse validity period: "1520/1624"
     */
    private String parseValidityPeriod(String token) {
        Matcher matcher = VALIDITY_PERIOD_PATTERN.matcher(token);

        if (!matcher.find()) {
            throw new IllegalStateException("Could not parse validity period");
        }

        String fromStr = matcher.group("from");
        String toStr = matcher.group("to");

        Instant validFrom = parseValidityTime(fromStr);
        Instant validTo = parseValidityTime(toStr);

        this.validityPeriod = new ValidityPeriod(validFrom, validTo);
        weatherData.setValidityPeriod(validityPeriod);

        LOGGER.debug("Validity period: {} to {}", validFrom, validTo);

        return token.substring(matcher.end()).trim();
    }

    /**
     * Parse a validity time in DDHH format.
     */
    private Instant parseValidityTime(String timeStr) {
        int day = Integer.parseInt(timeStr.substring(0, 2));
        int hour = Integer.parseInt(timeStr.substring(2, 4));

        // Use issue time to determine year and month
        LocalDateTime referenceTime = issueTime != null ?
                LocalDateTime.ofInstant(issueTime, ZoneOffset.UTC) :
                LocalDateTime.now(ZoneOffset.UTC);

        int year = referenceTime.getYear();
        int month = referenceTime.getMonthValue();

        // Handle month wrap-around when day is earlier than reference day
        // This indicates the validity time is in the next month
        if (day < referenceTime.getDayOfMonth()) {
            month++;
            if (month > 12) {
                month = 1;
                year++;
            }
        }

        // Handle 24:00 as next day 00:00
        if (hour == 24) {
            hour = 0;
            day++;

            // Check for month end
            LocalDateTime temp = LocalDateTime.of(year, month, 1, 0, 0);
            int lastDayOfMonth = temp.toLocalDate().lengthOfMonth();

            if (day > lastDayOfMonth) {
                day = 1;
                month++;
                if (month > 12) {
                    month = 1;
                    year++;
                }
            }
        }

        LocalDateTime validityTime = LocalDateTime.of(year, month, day, hour, 0);
        return validityTime.toInstant(ZoneOffset.UTC);
    }

    // ==================== FORECAST PERIOD PARSING ====================

    /**
     * Parse all forecast periods: BASE + change groups (FM, TEMPO, BECMG, PROB).
     */
    private String parseForecastPeriods(String token) {
        // First period is the BASE forecast
        token = parseBaseForecast(token);

        // Parse change groups until no more matches
        String remaining = token;
        String previous;

        do {
            previous = remaining;

            // Try each change group type - stop after first successful parse
            remaining = tryParseFM(remaining);

            if (remaining.equals(previous)) {
                remaining = tryParseTEMPO(remaining);
            }

            if (remaining.equals(previous)) {
                remaining = tryParseBECMG(remaining);
            }

            if (remaining.equals(previous)) {
                remaining = tryParsePROB(remaining);
            }

        } while (!remaining.equals(previous));

        return remaining;
    }

    /**
     * Parse the base forecast (initial conditions for validity period).
     */
    private String parseBaseForecast(String token) {
        LOGGER.debug("Parsing BASE forecast");

        // Initialize BASE period
        currentChangeIndicator = ChangeIndicator.BASE;
        currentPeriodStart = validityPeriod.validFrom();
        currentPeriodEnd = validityPeriod.validTo();

        // Parse weather conditions until we hit a change group or end
        token = parseWeatherConditions(token, this::isChangeGroupStart);

        // Save BASE forecast period
        saveForecastPeriod();

        return token;
    }

    /**
     * Try to parse an FM (From) change group.
     */
    private String tryParseFM(String token) {
        Matcher matcher = FM_PATTERN.matcher(token);

        if (!matcher.find()) {
            return token;
        }

        LOGGER.debug("Parsing FM change group");

        String timeStr = matcher.group("time");
        currentChangeIndicator = ChangeIndicator.FM;
        currentChangeTime = parseFMTime(timeStr);
        currentPeriodStart = null;
        currentPeriodEnd = null;

        // Parse weather conditions
        String remaining = token.substring(matcher.end()).trim();
        remaining = parseWeatherConditions(remaining, this::isChangeGroupStart);

        // Save FM period
        saveForecastPeriod();

        return remaining;
    }

    /**
     * Parse FM time in DDHHmm format.
     */
    private Instant parseFMTime(String timeStr) {
        int day = Integer.parseInt(timeStr.substring(0, 2));
        int hour = Integer.parseInt(timeStr.substring(2, 4));
        int minute = Integer.parseInt(timeStr.substring(4, 6));

        LocalDateTime referenceTime = issueTime != null ?
                LocalDateTime.ofInstant(issueTime, ZoneOffset.UTC) :
                LocalDateTime.now(ZoneOffset.UTC);

        int year = referenceTime.getYear();
        int month = referenceTime.getMonthValue();

        LocalDateTime changeTime = LocalDateTime.of(year, month, day, hour, minute);
        return changeTime.toInstant(ZoneOffset.UTC);
    }

    /**
     * Try to parse a TEMPO change group.
     */
    private String tryParseTEMPO(String token) {
        Matcher matcher = TEMPO_PATTERN.matcher(token);

        if (!matcher.find()) {
            return token;
        }

        LOGGER.debug("Parsing TEMPO change group");

        String fromStr = matcher.group("from");
        String toStr = matcher.group("to");

        currentChangeIndicator = ChangeIndicator.TEMPO;
        currentChangeTime = null;
        currentPeriodStart = parseValidityTime(fromStr);
        currentPeriodEnd = parseValidityTime(toStr);

        // Parse weather conditions
        String remaining = token.substring(matcher.end()).trim();
        remaining = parseWeatherConditions(remaining, this::isChangeGroupStart);

        // Save TEMPO period
        saveForecastPeriod();

        return remaining;
    }

    /**
     * Try to parse a BECMG change group.
     */
    private String tryParseBECMG(String token) {
        Matcher matcher = BECMG_PATTERN.matcher(token);

        if (!matcher.find()) {
            return token;
        }

        LOGGER.debug("Parsing BECMG change group");

        String fromStr = matcher.group("from");
        String toStr = matcher.group("to");

        currentChangeIndicator = ChangeIndicator.BECMG;
        currentChangeTime = null;
        currentPeriodStart = parseValidityTime(fromStr);
        currentPeriodEnd = parseValidityTime(toStr);

        // Parse weather conditions
        String remaining = token.substring(matcher.end()).trim();
        remaining = parseWeatherConditions(remaining, this::isChangeGroupStart);

        // Save BECMG period
        saveForecastPeriod();

        return remaining;
    }

    /**
     * Try to parse a PROB change group.
     */
    private String tryParsePROB(String token) {
        Matcher matcher = PROB_PATTERN.matcher(token);

        if (!matcher.find()) {
            return token;
        }

        LOGGER.debug("Parsing PROB change group");

        String probStr = matcher.group("prob");
        String fromStr = matcher.group("from");
        String toStr = matcher.group("to");

        currentChangeIndicator = ChangeIndicator.PROB;
        currentChangeTime = null;
        currentPeriodStart = parseValidityTime(fromStr);
        currentPeriodEnd = parseValidityTime(toStr);
        currentProbability = Integer.parseInt(probStr);

        // Parse weather conditions
        String remaining = token.substring(matcher.end()).trim();
        remaining = parseWeatherConditions(remaining, this::isChangeGroupStart);

        // Save PROB period
        saveForecastPeriod();

        return remaining;
    }

    /**
     * Check if token starts with a change group indicator.
     */
    private boolean isChangeGroupStart(String token) {
        return token.startsWith("FM") ||
                token.startsWith("TEMPO ") ||
                token.startsWith("BECMG ") ||
                token.startsWith("PROB") ||
                token.startsWith("TX") ||
                token.startsWith("TN");
    }

    /**
     * Parse weather conditions (wind, visibility, clouds, etc.) for current period.
     * Stops when stopCondition is met.
     */
    private String parseWeatherConditions(String token, java.util.function.Predicate<String> stopCondition) {
        IndexedLinkedHashMap<Pattern, NoaaAviationWeatherPatternHandler> mainHandlers =
                patternRegistry.getMainHandlers();

        return parseWithHandlers(token, mainHandlers, stopCondition);
    }

    /**
     * Parse token using METAR pattern handlers, stopping when condition is met.
     */
    private String parseWithHandlers(
            String token,
            IndexedLinkedHashMap<Pattern, NoaaAviationWeatherPatternHandler> handlers,
            java.util.function.Predicate<String> stopCondition) {

        String currentToken = token;

        while (!currentToken.isEmpty() && !stopCondition.test(currentToken)) {
            LOGGER.debug("Processing token: '{}'", currentToken);

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
     * Try all patterns against token and return updated token after first match.
     */
    private String tryAllPatterns(
            String token,
            IndexedLinkedHashMap<Pattern, NoaaAviationWeatherPatternHandler> handlers) {

        for (Map.Entry<Pattern, NoaaAviationWeatherPatternHandler> entry : handlers.entrySet()) {
            Pattern pattern = entry.getKey();
            NoaaAviationWeatherPatternHandler handlerInfo = entry.getValue();

            LOGGER.debug("Trying pattern: {} ({})",
                    handlerInfo.handlerName(), pattern.pattern());

            String updatedToken = tryPattern(token, pattern, handlerInfo);

            if (!updatedToken.equals(token)) {
                return updatedToken;
            }
        }

        return token;
    }

    /**
     * Try a single pattern against token.
     */
    private String tryPattern(String token, Pattern pattern, NoaaAviationWeatherPatternHandler handlerInfo) {
        Matcher matcher = pattern.matcher(token);

        if (!matcher.find() || matcher.group(0).isEmpty()) {
            return token;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Match found: '{}'", matcher.group(0));
        }

        // Execute handler
        handlePattern(handlerInfo.handlerName(), matcher);

        // Remove matched portion
        String updatedToken = matcher.replaceFirst("").trim();

        if (!updatedToken.isEmpty()) {
            updatedToken += " ";
        }

        return updatedToken;
    }

    /**
     * Save current forecast period to TAF data.
     */
    private void saveForecastPeriod() {
        // Build conditions using base class method
        WeatherConditions conditions = buildConditions();

        // Create forecast period
        ForecastPeriod period = new ForecastPeriod(
                currentChangeIndicator,
                currentChangeTime,
                currentPeriodStart,
                currentPeriodEnd,
                currentProbability,
                conditions
        );

        weatherData.addForecastPeriod(period);

        LOGGER.debug("Saved forecast period: {}", currentChangeIndicator);

        // Reset for next period
        resetCurrentPeriod();
    }

    // ==================== PATTERN HANDLERS ====================

    /**
     * Route to appropriate handler based on handler name.
     * Handlers for shared aviation weather elements (wind, visibility, present weather,
     * sky conditions) are inherited from NoaaAviationWeatherParser.
     */
    private void handlePattern(String handlerName, Matcher matcher) {
        try {
            switch (handlerName) {
                case "wind" -> handleWind(matcher);              // Inherited from base
                case "visibility" -> handleVisibility(matcher);  // Inherited from base
                case "runway" -> handleRunway(matcher);          // Overridden below
                case "presentWeather" -> handlePresentWeather(matcher);  // Inherited from base
                case "skyCondition" -> handleSkyCondition(matcher);      // Inherited from base
                default -> LOGGER.debug("No handler implemented for: {}", handlerName);
            }
        } catch (Exception e) {
            LOGGER.warn("Error in handler '{}': {}", handlerName, e.getMessage(), e);
        }
    }

    /**
     * Override base class with minimal TAF RVR support.
     * RVR is rare in TAF forecasts - just log if encountered.
     */
    @Override
    protected void handleRunway(Matcher matcher) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("RVR in TAF (unusual): {}", matcher.group(0));
        }
        // RVR is rare in TAF, just log it
    }

    // ==================== TAF-SPECIFIC HANDLERS ====================

    /**
     * Parse temperature forecasts: TX15/1518Z TN05/1510Z
     */
    private String parseTemperatureForecasts(String token) {
        String remaining = token;
        String previous;

        do {
            previous = remaining;
            remaining = tryParseTemperatureForecast(remaining);
        } while (!remaining.equals(previous));

        return remaining;
    }

    /**
     * Try to parse a single temperature forecast.
     */
    private String tryParseTemperatureForecast(String token) {
        Matcher matcher = TEMP_FORECAST_PATTERN.matcher(token);

        if (!matcher.find()) {
            return token;
        }

        String type = matcher.group("type");
        String sign = matcher.group("sign");
        String tempStr = matcher.group("temp");
        String dayStr = matcher.group("day");
        String hourStr = matcher.group("hour");

        try {
            int temperature = Integer.parseInt(tempStr);
            if ("M".equals(sign)) {
                temperature = -temperature;
            }

            int day = Integer.parseInt(dayStr);
            int hour = Integer.parseInt(hourStr);

            Instant forecastTime = parseTemperatureForecastTime(day, hour);

            if ("X".equals(type)) {
                weatherData.setMaxTemperatureForecast(temperature, forecastTime);
                LOGGER.debug("Max temperature: {}°C at {}", temperature, forecastTime);
            } else {
                weatherData.setMinTemperatureForecast(temperature, forecastTime);
                LOGGER.debug("Min temperature: {}°C at {}", temperature, forecastTime);
            }

        } catch (NumberFormatException e) {
            LOGGER.warn("Failed to parse temperature forecast: {}", matcher.group(0));
        }

        return token.substring(matcher.end()).trim();
    }

    /**
     * Parse temperature forecast time in DDHH format.
     */
    private Instant parseTemperatureForecastTime(int day, int hour) {
        LocalDateTime referenceTime = issueTime != null ?
                LocalDateTime.ofInstant(issueTime, ZoneOffset.UTC) :
                LocalDateTime.now(ZoneOffset.UTC);

        int year = referenceTime.getYear();
        int month = referenceTime.getMonthValue();

        LocalDateTime forecastTime = LocalDateTime.of(year, month, day, hour, 0);
        return forecastTime.toInstant(ZoneOffset.UTC);
    }

    /**
     * Parse remarks section (minimal TAF remarks support).
     */
    private void parseRemarks(String remarks) {
        LOGGER.debug("TAF remarks: {}", remarks);
        // TAF remarks are simpler than METAR, typically just "NXT FCST BY DDHHmmZ"
        // For now, just log them
    }

    // ==================== VALIDATION ====================

    /**
     * Validate parsed TAF data.
     */
    private void validateParsedData() {
        if (weatherData.getStationId() == null) {
            throw new IllegalStateException("Could not extract station ID from TAF");
        }

        if (weatherData.getValidityPeriod() == null) {
            throw new IllegalStateException("Could not extract validity period from TAF");
        }

        if (weatherData.getForecastPeriods().isEmpty()) {
            LOGGER.warn("No forecast periods parsed from TAF");
        }
    }
}
