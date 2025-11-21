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
package weather.utils;

import java.util.regex.Pattern;

/**
 * Regular expression patterns for validating aviation weather data.
 * 
 * These patterns are used by domain objects in the weather-common module
 * to validate input data during object construction. For patterns used in
 * parsing METAR/TAF strings, see RegExprConst in the weather-processing module.
 * 
 * All patterns are compiled once at class loading time for optimal performance.
 * 
 * @author bclasky1539
 * @see weather.processing.parser.noaa.RegExprConst
 * 
 */
public final class ValidationPatterns {
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ValidationPatterns() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Runway Identifier (standalone validation)
     * 
     * Validates runway designator format used in aviation weather reports.
     * 
     * Format: 01-36 optionally followed by L (Left), C (Center), or R (Right)
     * 
     * Valid Examples:
     *   "04L" - Runway 04 Left
     *   "22R" - Runway 22 Right
     *   "18" - Runway 18 (no suffix)
     *   "09C" - Runway 09 Center
     * 
     * Invalid Examples:
     *   "00" - Runway 00 doesn't exist
     *   "37" - Runway numbers only go up to 36
     *   "4L" - Must be zero-padded (04L)
     *   "18X" - Only L, C, R suffixes allowed
     * 
     * Used by:
     *   weather.model.components.RunwayVisualRange
     *   Runway condition reports
     *   Takeoff/landing runway assignments
     */
    public static final Pattern RUNWAY_IDENTIFIER = Pattern.compile(
        "^(0[1-9]|[12]\\d|3[0-6])[LCR]?$"
    );
    
    /**
     * Cloud type abbreviations
     * 
     * Standard ICAO cloud type abbreviations used in METAR/TAF reports.
     * 
     * Valid cloud types:
     *   CB - Cumulonimbus (thunderstorm clouds)
     *   TCU - Towering Cumulus
     *   CU - Cumulus
     *   SC - Stratocumulus
     *   ST - Stratus
     *   NS - Nimbostratus
     *   AS - Altostratus
     *   AC - Altocumulus
     *   CI - Cirrus
     *   CC - Cirrocumulus
     *   CS - Cirrostratus
     * 
     * Used by:
     *   weather.model.components.SkyCondition
     */
    public static final Pattern CLOUD_TYPE = Pattern.compile(
        "^(CB|TCU|CU|SC|ST|NS|AS|AC|CI|CC|CS)$"
    );
    
    /**
     * Wind speed units
     * 
     * Standard wind speed units used in METAR/TAF reports.
     * 
     * Valid units:
     *   KT - Knots (nautical miles per hour)
     *   MPS - Meters per second
     *   KMH - Kilometers per hour
     * 
     * Used by:
     *   weather.model.components.Wind
     */
    public static final Pattern WIND_UNIT = Pattern.compile(
        "^(KT|MPS|KMH)$"
    );
    
    /**
     * Temperature value with optional sign
     * 
     * Validates temperature format with optional minus sign or 'M' prefix.
     * 
     * Examples: "22", "M05", "-10", "05"
     */
    public static final Pattern TEMPERATURE_VALUE = Pattern.compile(
        "^[M-]?\\d{1,2}$"
    );
    
    /**
     * Wind direction
     * 
     * Validates wind direction in degrees (000-360) or VRB (variable).
     * 
     * Examples: "280", "360", "010", "VRB"
     */
    public static final Pattern WIND_DIRECTION = Pattern.compile(
        "^(0\\d{2}|[1-2]\\d{2}|3[0-5]\\d|360|VRB)$"
    );
    
    /**
     * Wind speed/gust value
     * 
     * Validates wind speed as 2-3 digits.
     * 
     * Examples: "16", "032", "05"
     */
    public static final Pattern WIND_SPEED = Pattern.compile(
        "^\\d{2,3}$"
    );
    
    /**
     * Altitude/Height in hundreds of feet (3 digits)
     * 
     * Used for cloud heights, visibility vertical limits, etc.
     * 
     * Examples: "015" (1,500ft), "250" (25,000ft), "005" (500ft)
     */
    public static final Pattern ALTITUDE_HUNDREDS_FEET = Pattern.compile(
        "^\\d{3}$"
    );
    
    /**
     * Pressure value (3-4 digits)
     * 
     * Validates altimeter/pressure values in various units.
     * 
     * Examples: "3015" (30.15 inHg), "1013" (1013 hPa), "996"
     */
    public static final Pattern PRESSURE_VALUE = Pattern.compile(
        "^\\d{3,4}$"
    );
    
    /**
     * Visibility distance units
     * 
     * Standard visibility distance units used in METAR/TAF reports.
     * 
     * Valid units:
     *   SM - Statute Miles
     *   M - Meters
     *   KM - Kilometers
     * 
     * Used by:
     *   weather.model.components.Visibility
     */
    public static final Pattern VISIBILITY_UNIT = Pattern.compile(
        "^(SM|M|KM)$"
    );
    
    /**
     * Visibility special conditions
     * 
     * Special visibility conditions used in METAR/TAF reports.
     * 
     * Valid conditions:
     *   CAVOK - Ceiling And Visibility OK
     *   NDV - No Directional Variation
     * 
     * Used by:
     *   weather.model.components.Visibility
     */
    public static final Pattern VISIBILITY_SPECIAL_CONDITION = Pattern.compile(
        "^(CAVOK|NDV)$"
    );
}
