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

import weather.model.NoaaWeatherData;
import weather.model.WeatherConditions;
import weather.model.components.*;
import weather.model.enums.PressureUnit;
import weather.model.enums.SkyCoverage;
import weather.processing.parser.common.WeatherParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for NOAA aviation weather parsers (METAR and TAF).
 * <p>
 * Consolidates shared parsing logic for common aviation weather elements:
 * - Wind
 * - Visibility
 * - Present weather phenomena
 * - Sky conditions (clouds)
 * - Runway visual range
 * <p>
 * Design Philosophy:
 * - Single source of truth for aviation weather parsing
 * - Eliminates code duplication between METAR and TAF parsers
 * - Shared handlers implemented once, maintained once
 * - Subclasses handle report-specific logic (remarks, forecast periods, etc.)
 * <p>
 * Generic Type Parameter:
 * - T extends NoaaWeatherData: Allows type-safe access to specific data types
 * (NoaaMetarData or NoaaTafData) without casting
 *
 * @param <T> The specific weather data type (NoaaMetarData or NoaaTafData)
 * @author bclasky1539
 *
 */
public abstract class NoaaAviationWeatherParser<T extends NoaaWeatherData>
        implements WeatherParser<NoaaWeatherData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoaaAviationWeatherParser.class);

    // Pattern for RVR M/P prefix
    private static final String RVR_PREFIX_PATTERN = "^[MP]";

    // ==================== SHARED STATE FOR BUILDING CONDITIONS ====================

    /**
     * Builder for accumulating weather conditions during parsing.
     * Each parser builds conditions incrementally as patterns are matched.
     */
    protected WeatherConditions.Builder conditionsBuilder;

    /**
     * Accumulator for present weather phenomena.
     * Multiple weather phenomena can be present (e.g., -RA BR = light rain and mist).
     */
    protected List<PresentWeather> presentWeatherList;

    /**
     * Accumulator for sky condition layers.
     * Multiple cloud layers can be present (e.g., FEW100 SCT250 = few at 10k, scattered at 25k).
     */
    protected List<SkyCondition> skyConditionsList;

    /**
     * The weather data object being built.
     * Subclasses provide the specific type (NoaaMetarData or NoaaTafData).
     */
    protected T weatherData;

    protected static final String GROUP_PRESSURE_CHANGE = "press";

    // ==================== INITIALIZATION ====================

    /**
     * Initialize or reset shared parsing state for building weather conditions.
     * <p>
     * Called by subclasses in two scenarios:
     * 1. During parse initialization (METAR and TAF)
     * 2. When starting a new forecast period (TAF only)
     * <p>
     * This method resets the conditions builder and clears accumulated lists,
     * preparing to parse a new set of weather conditions.
     */
    protected void initializeSharedState() {
        this.conditionsBuilder = WeatherConditions.builder();
        this.presentWeatherList = new ArrayList<>();
        this.skyConditionsList = new ArrayList<>();
    }

    // ==================== SHARED HANDLER METHODS ====================

    /**
     * Handle wind: "19005KT" or "19005G15KT" or "VRB02KT"
     * <p>
     * Creates Wind object and adds to conditions builder.
     * Handles:
     * - Direction (degrees, VRB for variable, or null for calm)
     * - Speed (knots, meters per second, etc.)
     * - Gusts (optional)
     * - Units (KT, MPS, KMH)
     *
     * @param matcher Regex matcher positioned at wind group
     */
    protected void handleWind(Matcher matcher) {
        if (weatherData == null) {
            return;
        }

        String directionStr = matcher.group("dir");
        String speedStr = matcher.group("speed");
        String gustStr = matcher.group("gust");
        String unitStr = matcher.group("units");

        Integer direction = parseWindDirection(directionStr);
        Integer speed = parseWindSpeed(speedStr);
        Integer gust = parseWindGust(gustStr);
        String unit = parseWindUnit(unitStr);

        Wind wind = createWind(directionStr, direction, speed, gust, unit);

        if (wind != null) {
            conditionsBuilder.wind(wind);
            logWindData(direction, speed, gust, unit);
        } else {
            LOGGER.debug("Wind: Missing speed or direction, not setting wind data");
        }
    }

    /**
     * Parse wind direction from string.
     *
     * @param directionStr direction string (degrees or VRB)
     * @return direction in degrees, or null if VRB or invalid
     */
    private Integer parseWindDirection(String directionStr) {
        if (directionStr == null || "VRB".equals(directionStr)) {
            return null;
        }
        return Integer.parseInt(directionStr);
    }

    /**
     * Parse wind speed from string.
     *
     * @param speedStr speed string
     * @return speed value, or null if invalid
     */
    private Integer parseWindSpeed(String speedStr) {
        return speedStr != null ? Integer.parseInt(speedStr) : null;
    }

    /**
     * Parse wind gust from string.
     *
     * @param gustStr gust string
     * @return gust value, or null if not present
     */
    private Integer parseWindGust(String gustStr) {
        if (gustStr != null && !gustStr.isEmpty()) {
            return Integer.parseInt(gustStr);
        }
        return null;
    }

    /**
     * Parse wind unit from string.
     *
     * @param unitStr unit string
     * @return unit (defaults to KT if not specified)
     */
    private String parseWindUnit(String unitStr) {
        return unitStr != null ? unitStr : "KT";
    }

    /**
     * Create appropriate Wind object based on parsed values.
     *
     * @param directionStr original direction string for VRB check
     * @param direction    parsed direction value
     * @param speed        wind speed
     * @param gust         gust speed (optional)
     * @param unit         wind unit
     * @return Wind object, or null if invalid
     */
    private Wind createWind(String directionStr, Integer direction, Integer speed,
                            Integer gust, String unit) {
        if (speed == null) {
            return null;
        }

        // Variable direction (VRB)
        if ("VRB".equals(directionStr)) {
            return Wind.variable(speed, unit);
        }

        // Missing direction
        if (direction == null) {
            return null;
        }

        // Calm wind (00000KT)
        if (direction == 0 && speed == 0) {
            LOGGER.debug("wind - Calm");
            return Wind.calm();
        }

        // Wind with gusts
        if (gust != null) {
            return Wind.ofWithGusts(direction, speed, gust, unit);
        }

        // Regular wind
        return Wind.of(direction, speed, unit);
    }

    /**
     * Log wind data for debugging.
     *
     * @param direction wind direction
     * @param speed     wind speed
     * @param gust      gust speed
     * @param unit      wind unit
     */
    private void logWindData(Integer direction, Integer speed, Integer gust, String unit) {
        if (direction != null && speed != null) {
            LOGGER.debug("Wind - Dir: {}, Speed: {}, Gust: {}, Unit: {}",
                    direction, speed, gust, unit);
        } else if (speed != null) {
            LOGGER.debug("wind - Variable direction, Speed: {}, Unit: {}", speed, unit);
        }
    }

    /**
     * Handle visibility: "10SM", "9999", "1/2SM", "CAVOK", etc.
     * <p>
     * Creates Visibility object and adds to conditions builder.
     * Handles:
     * - Special conditions (CAVOK, NDV)
     * - International format (meters with M/P prefix)
     * - US format (statute miles with fractions)
     *
     * @param matcher Regex matcher positioned at visibility group
     */
    protected void handleVisibility(Matcher matcher) {
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
            Visibility cavokVisibility = Visibility.cavok();
            conditionsBuilder.visibility(cavokVisibility);
            LOGGER.debug("Visibility: CAVOK");
            return true;
        }

        if ("NDV".equals(visGroup)) {
            // NDV = No Directional Variation - no visibility to set
            LOGGER.debug("Visibility: NDV (not set)");
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
            Visibility visibility = new Visibility(distance, "M", lessThan, greaterThan, null);
            conditionsBuilder.visibility(visibility);

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
            Visibility visibility = new Visibility(distance, unit, lessThan, greaterThan, null);
            conditionsBuilder.visibility(visibility);

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
     * @param e           The NumberFormatException that occurred
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
    protected double parseFractionalDistance(String distStr) {
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
    protected double parseFraction(String fraction) {
        String[] parts = fraction.split("/");
        double numerator = Double.parseDouble(parts[0]);
        double denominator = Double.parseDouble(parts[1]);
        return numerator / denominator;
    }

    /**
     * Handle present weather phenomena.
     * Parses weather codes like: -RA, +TSRA, VCFG, BR, NSW
     * <p>
     * Creates PresentWeather object and adds to list.
     * Multiple weather phenomena can be present in a single report.
     *
     * @param matcher Regex matcher positioned at weather group
     */
    protected void handlePresentWeather(Matcher matcher) {
        if (weatherData == null) {
            return;
        }

        String weatherString = matcher.group(0).trim();

        // ICAO missing-data placeholder: the entire token is slashes (e.g. "//////"),
        // not a real weather phenomenon with an unknown subcomponent. Skip it rather
        // than creating a bogus PresentWeather entry from the placeholder text itself.
        if (!weatherString.isEmpty() && weatherString.chars().allMatch(c -> c == '/')) {
            LOGGER.debug("Skipping missing-data placeholder in present weather: '{}'", weatherString);
            return;
        }

        try {
            PresentWeather presentWeather = PresentWeather.parse(weatherString);
            presentWeatherList.add(presentWeather);

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
     * <p>
     * Format: [COVERAGE][HEIGHT][TYPE]
     * - Coverage: SKC, CLR, FEW, SCT, BKN, OVC, VV, etc.
     * - Height: 3-4 digits (in hundreds of feet)
     * - Type: CB (cumulonimbus), TCU (towering cumulus), etc.
     * <p>
     * Creates SkyCondition object and adds to list.
     * Multiple cloud layers can be present.
     *
     * @param matcher Regex matcher positioned at sky condition group
     */
    protected void handleSkyCondition(Matcher matcher) {
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
            skyConditionsList.add(skyCondition);

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
     * @param coverageStr the coverage string from METAR/TAF
     * @return SkyCoverage enum
     * @throws IllegalArgumentException if coverage is invalid
     */
    protected SkyCoverage parseCoverage(String coverageStr) {
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
     * Parse cloud height from METAR/TAF format.
     * Heights are encoded as hundreds of feet (e.g., "050" = 5000 feet).
     *
     * @param heightStr the height string from METAR/TAF (could be null)
     * @param coverage  the sky coverage (used for validation)
     * @return height in feet, or null if not applicable
     */
    protected Integer parseHeight(String heightStr, SkyCoverage coverage) {
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
     * Parse cloud type from METAR/TAF format.
     * Common types: CB (cumulonimbus), TCU (towering cumulus).
     *
     * @param cloudTypeStr the cloud type string from METAR/TAF (could be null)
     * @return cloud type, or null if not present
     */
    protected String parseCloudType(String cloudTypeStr) {
        // Handle unknown/missing cloud type (///)
        if (cloudTypeStr == null || cloudTypeStr.isBlank() || "///".equals(cloudTypeStr)) {
            return null;
        }

        return cloudTypeStr.trim().toUpperCase();
    }

    /**
     * Handle altimeter/pressure setting.
     * <p>
     * Formats:
     * - A3015 → 30.15 inHg (North America)
     * - Q1013 → 1013 hPa (International)
     * - QNH1013 → 1013 hPa
     * - 2992INS → 29.92 inHg (older format)
     * - //// → Missing
     */
    protected void handleAltimeter(Matcher matcher) {
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
     * <p>
     * Logic:
     * - "A" or "AA" prefix → inches of mercury (divide by 100)
     * - "Q" or "QNH" prefix → hectopascals
     * - "INS" suffix → inches of mercury (divide by 100)
     * - 4 digits starting with 2 or 3 → inches of mercury (divide by 100)
     * - 4 digits starting with 0 or 1 → hectopascals
     * - 3 digits → hectopascals
     *
     * @param unit1       Prefix unit indicator
     * @param pressureStr Pressure digits
     * @param unit2       Suffix unit indicator
     * @return Pressure object, or null if invalid
     */
    protected Pressure parsePressure(String unit1, String pressureStr, String unit2) {
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
     * @param value  Pressure value
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
     * <p>
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
     * Handle runway visual range (RVR).
     * Note: RVR is common in METAR but rare in TAF.
     * <p>
     * Subclasses can override for specific RVR handling.
     *
     * @param matcher Regex matcher positioned at RVR group
     */
    protected void handleRunway(Matcher matcher) {
        // Default implementation logs and does nothing
        // METAR parser overrides this with full RVR handling
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("RVR pattern matched: {}", matcher.group(0));
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Build WeatherConditions from accumulated data.
     * Adds accumulated lists to builder and builds the final conditions object.
     *
     * @return Built WeatherConditions object
     */
    protected WeatherConditions buildConditions() {
        // Set accumulated lists in builder
        if (!presentWeatherList.isEmpty()) {
            conditionsBuilder.presentWeather(presentWeatherList);
        }
        if (!skyConditionsList.isEmpty()) {
            conditionsBuilder.skyConditions(skyConditionsList);
        }

        return conditionsBuilder.build();
    }

    /**
     * Log unparsed tokens for debugging.
     *
     * @param mainBody remaining main body tokens
     * @param remarks  remaining remark tokens
     */
    protected void logUnparsedTokens(String mainBody, String remarks) {
        if (LOGGER.isDebugEnabled() && mainBody != null && !mainBody.trim().isEmpty()) {
            LOGGER.debug("Unparsed main body tokens: '{}'", mainBody.trim());
        }

        if (LOGGER.isDebugEnabled() && remarks != null && !remarks.trim().isEmpty()) {
            LOGGER.debug("Unparsed remark tokens: '{}'", remarks.trim());
        }
    }
}
