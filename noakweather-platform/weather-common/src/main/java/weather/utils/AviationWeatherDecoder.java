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

import java.util.Map;

/**
 * Utility class for decoding standard ICAO aviation weather codes into human-readable descriptions.
 * 
 * This decoder handles standard codes as defined by ICAO Annex 3 and WMO standards,
 * used across METAR, TAF, SPECI, and other aviation weather products. All codes follow
 * international aviation weather reporting standards.
 * 
 * All methods are static and thread-safe. The decoder uses immutable lookup maps
 * initialized at class loading time for optimal performance.
 * 
 * Supported Code Types:
 * 
 *   Sky Coverage: SKC, CLR, FEW, SCT, BKN, OVC, VV
 *   Weather Phenomena: RA, SN, FG, BR, DZ, GR, TS, etc.
 *   Intensity: + (heavy), - (light), VC (vicinity)
 *   Descriptors: SH, TS, FZ, BL, MI, BC, DR, PR
 *   Cloud Types: CB, TCU
 * 
 * 
 * Usage Examples:
 * {@code
 * // Sky coverage
 * String sky = AviationWeatherDecoder.decodeSkyCoverage("BKN");  // "Broken"
 * 
 * // Weather phenomena
 * String wx = AviationWeatherDecoder.decodeWeatherPhenomenon("RA");  // "Rain"
 * 
 * // Complete weather string
 * String full = AviationWeatherDecoder.decodeWeather("-SHRA");  // "Light Shower(s) Rain"
 * 
 * // Cloud type
 * String cloud = AviationWeatherDecoder.decodeCloudType("CB");  // "Cumulonimbus"
 * }
 * 
 * @author bclasky1539
 *
 */
public final class AviationWeatherDecoder {
    
    private static final String IN_THE_VICINITY = "In the Vicinity";
    
    // Sky Coverage codes (ICAO Annex 3, Section 4.5)
    private static final Map<String, String> SKY_COVERAGE = Map.ofEntries(
        Map.entry("SKC", "Sky Clear"),
        Map.entry("CLR", "Clear"),
        Map.entry("NSC", "No Significant Clouds"),
        Map.entry("NCD", "No Clouds Detected"),
        Map.entry("FEW", "Few"),
        Map.entry("SCT", "Scattered"),
        Map.entry("BKN", "Broken"),
        Map.entry("OVC", "Overcast"),
        Map.entry("VV", "Vertical Visibility"),
        Map.entry("///", "Sky Obscured")
    );
    
    // Weather Phenomena codes (ICAO Annex 3, Table 4678)
    private static final Map<String, String> WEATHER_PHENOMENA = Map.ofEntries(
        // Precipitation
        Map.entry("DZ", "Drizzle"),
        Map.entry("RA", "Rain"),
        Map.entry("SN", "Snow"),
        Map.entry("SG", "Snow Grains"),
        Map.entry("IC", "Ice Crystals"),
        Map.entry("PL", "Ice Pellets"),
        Map.entry("GR", "Hail"),
        Map.entry("GS", "Small Hail"),
        Map.entry("UP", "Unknown Precipitation"),
        
        // Obscuration
        Map.entry("BR", "Mist"),
        Map.entry("FG", "Fog"),
        Map.entry("FU", "Smoke"),
        Map.entry("VA", "Volcanic Ash"),
        Map.entry("DU", "Widespread Dust"),
        Map.entry("SA", "Sand"),
        Map.entry("HZ", "Haze"),
        Map.entry("PY", "Spray"),
        
        // Other phenomena
        Map.entry("PO", "Dust/Sand Whirls"),
        Map.entry("SQ", "Squalls"),
        Map.entry("FC", "Funnel Cloud"),
        Map.entry("SS", "Sandstorm"),
        Map.entry("DS", "Duststorm")
    );
    
    // Intensity/Proximity indicators (ICAO Annex 3, Section 4.4.2.5)
    private static final Map<String, String> INTENSITY = Map.of(
        "-", "Light",
        "+", "Heavy",
        "VC", IN_THE_VICINITY
    );
    
    // Weather descriptors (ICAO Annex 3, Table 4678)
    private static final Map<String, String> DESCRIPTORS = Map.ofEntries(
        Map.entry("MI", "Shallow"),
        Map.entry("PR", "Partial"),
        Map.entry("BC", "Patches"),
        Map.entry("DR", "Low Drifting"),
        Map.entry("BL", "Blowing"),
        Map.entry("SH", "Shower(s)"),
        Map.entry("TS", "Thunderstorm"),
        Map.entry("FZ", "Freezing")
    );
    
    // Cloud types (ICAO Annex 3, Section 4.5.4.3)
    private static final Map<String, String> CLOUD_TYPES = Map.ofEntries(
        Map.entry("CB", "Cumulonimbus"),
        Map.entry("TCU", "Towering Cumulus"),
        Map.entry("CU", "Cumulus"),
        Map.entry("SC", "Stratocumulus"),
        Map.entry("ST", "Stratus"),
        Map.entry("NS", "Nimbostratus"),
        Map.entry("AS", "Altostratus"),
        Map.entry("AC", "Altocumulus"),
        Map.entry("CI", "Cirrus"),
        Map.entry("CC", "Cirrocumulus"),
        Map.entry("CS", "Cirrostratus")
    );
    
    /**
     * Private constructor to prevent instantiation of utility class.
     * 
     * @throws AssertionError if instantiation is attempted
     */
    private AviationWeatherDecoder() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Decode a sky coverage code to human-readable description.
     * 
     * Handles standard ICAO sky coverage codes including special conditions
     * like sky clear, vertical visibility, and cloud amount codes.
     * 
     * Coverage Meanings:
     * 
     *   SKC/CLR: 0 oktas (0/8 coverage)
     *   FEW: 1-2 oktas (1/8 - 2/8 coverage)
     *   SCT: 3-4 oktas (3/8 - 4/8 coverage)
     *   BKN: 5-7 oktas (5/8 - 7/8 coverage)
     *   OVC: 8 oktas (8/8 coverage)
     * 
     * 
     * @param code the sky coverage code (e.g., "BKN", "OVC", "FEW")
     * @return human-readable description, or the original code if not recognized
     */
    public static String decodeSkyCoverage(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return SKY_COVERAGE.getOrDefault(code.toUpperCase().trim(), code);
    }
    
    /**
     * Decode a weather phenomenon code to human-readable description.
     * 
     * Handles all standard ICAO weather phenomenon codes for precipitation,
     * obscuration, and other weather events.
     * 
     * @param code the weather phenomenon code (e.g., "RA", "SN", "FG")
     * @return human-readable description, or the original code if not recognized
     */
    public static String decodeWeatherPhenomenon(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return WEATHER_PHENOMENA.getOrDefault(code.toUpperCase().trim(), code);
    }
    
    /**
     * Decode an intensity or proximity indicator.
     * 
     * Intensity indicators modify the severity of weather phenomena:
     * 
     *   "-" : Light intensity
     *   (no modifier) : Moderate intensity (default)
     *   "+" : Heavy intensity
     *   "VC" : In the vicinity (5-10 SM from the station)
     * 
     * 
     * @param code the intensity code ("+", "-", "VC")
     * @return human-readable description, or empty string if not recognized
     */
    public static String decodeIntensity(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return INTENSITY.getOrDefault(code.trim(), "");
    }
    
    /**
     * Decode a weather descriptor.
     * 
     * Descriptors provide additional detail about weather phenomena,
     * such as the character (showers, thunderstorm) or state (freezing, blowing).
     * 
     * @param code the descriptor code (e.g., "SH", "TS", "FZ")
     * @return human-readable description, or the original code if not recognized
     */
    public static String decodeDescriptor(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return DESCRIPTORS.getOrDefault(code.toUpperCase().trim(), code);
    }
    
    /**
     * Decode a cloud type code to human-readable description.
     * 
     * Cloud types indicate significant convective clouds or cloud genus.
     * CB (Cumulonimbus) and TCU (Towering Cumulus) are most commonly reported
     * in aviation weather due to their significance for flight operations.
     * 
     * @param code the cloud type code (e.g., "CB", "TCU")
     * @return human-readable description, or the original code if not recognized
     */
    public static String decodeCloudType(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return CLOUD_TYPES.getOrDefault(code.toUpperCase().trim(), code);
    }
    
    /**
     * Decode a complete weather string including intensity, descriptor, and phenomena.
     * 
     * Parses and translates complex weather codes that may include:
     * 
     *   Intensity indicator (prefix)
     *   One or more descriptors
     *   One or more weather phenomena
     *   Vicinity indicator (prefix or standalone)
     * 
     * 
     * Examples:
     * {@code
     * "-RA"      → "Light Rain"
     * "+TSRA"    → "Heavy Thunderstorm Rain"
     * "VCFG"     → "Fog In the Vicinity"
     * "FZRA"     → "Freezing Rain"
     * "-SHRA"    → "Light Shower(s) Rain"
     * "BLSN"     → "Blowing Snow"
     * "+TSRAGR"  → "Heavy Thunderstorm Rain Hail"
     * }
     * 
     * @param weatherCode the complete weather code string
     * @return human-readable description of the weather condition
     */
    public static String decodeWeather(String weatherCode) {
        if (weatherCode == null || weatherCode.isBlank()) {
            return "";
        }
        
        WeatherParser parser = new WeatherParser(weatherCode.trim().toUpperCase());
        return parser.parse();
    }
    
    /**
     * Internal helper class to parse weather codes with reduced complexity.
     * Breaks down the parsing into discrete steps for better maintainability.
     */
    private static class WeatherParser {
        private final StringBuilder result = new StringBuilder();
        private String remaining;
        private boolean vicinityAtStart = false;
        
        WeatherParser(String weatherCode) {
            this.remaining = weatherCode;
        }
        
        String parse() {
            extractIntensity();
            extractDescriptors();
            extractPhenomena();
            extractVicinity();
            appendUnrecognized();
            return result.toString().trim();
        }
        
        private void extractIntensity() {
            if (remaining.startsWith("+")) {
                result.append("Heavy ");
                remaining = remaining.substring(1);
            } else if (remaining.startsWith("-")) {
                result.append("Light ");
                remaining = remaining.substring(1);
            } else if (remaining.startsWith("VC")) {
                vicinityAtStart = true;
                remaining = remaining.substring(2);
            }
        }
        
        private void extractDescriptors() {
            remaining = extractCodes(remaining, DESCRIPTORS, result);
        }
        
        private void extractPhenomena() {
            remaining = extractCodes(remaining, WEATHER_PHENOMENA, result);
        }
        
        private void extractVicinity() {
            // VC (vicinity) was extracted at the start, append it now after phenomena
            // According to ICAO Annex 3, VC always precedes weather phenomena (e.g., VCFG, VCTS)
            if (vicinityAtStart) {
                result.append(IN_THE_VICINITY);
            }
        }
        
        private void appendUnrecognized() {
            if (!remaining.isEmpty()) {
                result.append(remaining);
            }
        }
        
        /**
         * Extract codes from a map and append their decoded values to the result.
         * 
         * @param input the string to process
         * @param codeMap the map of codes to descriptions
         * @param output the StringBuilder to append decoded values to
         * @return the remaining unprocessed string
         */
        private static String extractCodes(String input, Map<String, String> codeMap, StringBuilder output) {
            String current = input;
            while (current.length() >= 2) {
                String twoChar = current.substring(0, 2);
                String decoded = codeMap.get(twoChar);
                if (decoded == null) {
                    break;
                }
                output.append(decoded).append(" ");
                current = current.substring(2);
            }
            return current;
        }
    }
}
