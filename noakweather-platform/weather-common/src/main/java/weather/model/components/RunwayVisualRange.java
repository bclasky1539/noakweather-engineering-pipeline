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
package weather.model.components;

import weather.utils.ValidationPatterns;

/**
 * Immutable value object representing runway visual range (RVR) conditions.
 *
 * RVR indicates the horizontal distance a pilot can see down the runway,
 * typically reported when visibility is poor. This is critical information for
 * takeoff and landing decisions.
 *
 * RVR is measured by transmissometers installed alongside runways and is
 * more precise than prevailing visibility. Values are reported in feet in the
 * United States and meters internationally.
 *
 * Can be in one of two states:
 * 1. Active RVR measurement with visual range values
 * 2. Cleared (CLRD) - conditions have improved, RVR no longer needed
 *
 * Format Examples:
 *
 * R04L/2200FT        → Runway 04 Left, 2200 feet
 * R04L/1800V2400FT   → Runway 04 Left, variable 1800-2400 feet
 * R22R/P6000FT       → Runway 22 Right, greater than 6000 feet
 * R04L/M0600FT       → Runway 04 Left, less than 600 feet
 * R04L/2200FT/D      → Runway 04 Left, 2200 feet, decreasing
 * R18/1200V1800FT/U  → Runway 18, variable 1200-1800 feet, increasing
 * R22R/CLRD          → Runway 22 Right, cleared (conditions improved)
 * R34L/CLRD70        → Runway 34 Left, cleared
 *
 *
 * @param runway Runway identifier (e.g., "04L", "22R", "18", "09")
 * @param visualRangeFeet Primary visual range value in feet (null if variable or cleared)
 * @param variableLow Lower bound of variable range in feet (null if not variable or cleared)
 * @param variableHigh Upper bound of variable range in feet (null if not variable or cleared)
 * @param prefix Prefix modifier: "P" (greater than), "M" (less than), or null
 * @param trend Trend indicator: "D" (decreasing), "N" (no change), "U" (increasing), or null
 * @param cleared True if this represents a CLRD (cleared) condition
 *
 * @author bclasky1539
 *
 */
public record RunwayVisualRange(
        String runway,
        Integer visualRangeFeet,
        Integer variableLow,
        Integer variableHigh,
        String prefix,
        String trend,
        boolean cleared
) {

    /**
     * Compact constructor with validation.
     */
    public RunwayVisualRange {
        // Always require runway identifier
        validateRunway(runway);

        if (cleared) {
            // CLRD state: all numeric values must be null
            if (visualRangeFeet != null || variableLow != null || variableHigh != null) {
                throw new IllegalArgumentException(
                        "CLRD runway cannot have visual range values");
            }
            if (prefix != null && !prefix.isBlank()) {
                throw new IllegalArgumentException(
                        "CLRD runway cannot have prefix (M/P)");
            }
            // trend can be anything when cleared (often null or ignored)
        } else {
            // Active RVR: require valid visual range data
            validateVisualRange(visualRangeFeet, variableLow, variableHigh);
            validatePrefix(prefix);
            validateTrend(trend);
        }
    }

    /**
     * Convenience constructor for active (non-cleared) RVR.
     * Use this for normal RVR measurements.
     *
     * @param runway Runway identifier
     * @param visualRangeFeet Visual range in feet (null if variable)
     * @param variableLow Variable range low bound (null if not variable)
     * @param variableHigh Variable range high bound (null if not variable)
     * @param prefix Prefix modifier (M/P or null)
     * @param trend Trend indicator (D/N/U or null)
     */
    public RunwayVisualRange(String runway, Integer visualRangeFeet,
                             Integer variableLow, Integer variableHigh,
                             String prefix, String trend) {
        this(runway, visualRangeFeet, variableLow, variableHigh, prefix, trend, false);
    }

    /**
     * Factory method for creating a CLRD (cleared) RVR.
     *
     * CLRD indicates that RVR conditions have improved and are no longer
     * being reported. This is meaningful information indicating improved visibility.
     *
     * @param runway The runway identifier (e.g., "04L", "22R")
     * @return A cleared RVR instance
     */
    public static RunwayVisualRange cleared(String runway) {
        return new RunwayVisualRange(runway, null, null, null, null, null, true);
    }

    /**
     * Factory method for creating a simple RVR with just a runway and value.
     *
     * @param runway runway identifier
     * @param visualRangeFeet visual range in feet
     * @return new RunwayVisualRange instance
     */
    public static RunwayVisualRange of(String runway, int visualRangeFeet) {
        return new RunwayVisualRange(runway, visualRangeFeet, null, null, null, null, false);
    }

    /**
     * Factory method for creating a variable RVR.
     *
     * @param runway runway identifier
     * @param varLow lower bound in feet
     * @param varHigh upper bound in feet
     * @return new RunwayVisualRange instance
     */
    public static RunwayVisualRange variable(String runway, int varLow, int varHigh) {
        return new RunwayVisualRange(runway, null, varLow, varHigh, null, null, false);
    }

    /**
     * Validate runway identifier format.
     */
    private static void validateRunway(String runway) {
        if (runway == null || runway.isBlank()) {
            throw new IllegalArgumentException("Runway identifier cannot be null or blank");
        }

        // Runway format: 01-36 optionally followed by L/C/R
        String trimmed = runway.trim().toUpperCase();
        if (!ValidationPatterns.RUNWAY_IDENTIFIER.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "Invalid runway identifier format: " + runway +
                            " (expected format: 01-36 optionally followed by L, C, or R)"
            );
        }
    }

    /**
     * Validate visual range values.
     */
    private static void validateVisualRange(Integer visualRange, Integer varLow, Integer varHigh) {
        // Must have either a single value or a variable range
        if (visualRange == null && (varLow == null || varHigh == null)) {
            throw new IllegalArgumentException(
                    "Must have either visualRangeFeet or both variableLow and variableHigh"
            );
        }

        // Validate visual range if present
        if (visualRange != null && visualRange < 0) {
            throw new IllegalArgumentException("Visual range cannot be negative: " + visualRange);
        }

        // Validate variable range if present
        if (varLow != null || varHigh != null) {
            validateVariableRange(varLow, varHigh);
        }
    }

    /**
     * Validate variable range bounds.
     */
    private static void validateVariableRange(Integer varLow, Integer varHigh) {
        if (varLow == null || varHigh == null) {
            throw new IllegalArgumentException(
                    "Both variableLow and variableHigh must be provided together"
            );
        }

        if (varLow < 0) {
            throw new IllegalArgumentException("Variable low cannot be negative: " + varLow);
        }

        if (varHigh < 0) {
            throw new IllegalArgumentException("Variable high cannot be negative: " + varHigh);
        }

        if (varLow > varHigh) {
            throw new IllegalArgumentException(
                    "Variable low (" + varLow + ") must be less than variable high (" + varHigh + ")"
            );
        }
    }

    /**
     * Validate prefix modifier.
     */
    private static void validatePrefix(String prefix) {
        if (prefix != null && !prefix.isBlank()) {
            String upper = prefix.trim().toUpperCase();
            if (!upper.equals("P") && !upper.equals("M")) {
                throw new IllegalArgumentException(
                        "Invalid prefix: " + prefix + " (valid values: P, M, or null)"
                );
            }
        }
    }

    /**
     * Validate trend indicator.
     */
    private static void validateTrend(String trend) {
        if (trend != null && !trend.isBlank()) {
            String upper = trend.trim().toUpperCase();
            if (!upper.equals("D") && !upper.equals("N") && !upper.equals("U")) {
                throw new IllegalArgumentException(
                        "Invalid trend: " + trend + " (valid values: D, N, U, or null)"
                );
            }
        }
    }

    /**
     * Check if this RVR represents a cleared condition.
     *
     * CLRD indicates visibility has improved and RVR is no longer being reported.
     *
     * @return true if this is a CLRD RVR
     */
    public boolean isCleared() {
        return cleared;
    }

    /**
     * Check if this RVR has variable range values.
     *
     * @return true if the range is variable (e.g., 1800V2400), false if cleared or fixed
     */
    public boolean isVariable() {
        return !cleared && variableLow != null && variableHigh != null;
    }

    /**
     * Check if visibility is reported as greater than the measured value.
     *
     * @return true if prefix is "P" (greater than), false if cleared or no prefix
     */
    public boolean isGreaterThan() {
        return !cleared && "P".equalsIgnoreCase(prefix);
    }

    /**
     * Check if visibility is reported as less than the measured value.
     *
     * @return true if prefix is "M" (less than), false if cleared or no prefix
     */
    public boolean isLessThan() {
        return !cleared && "M".equalsIgnoreCase(prefix);
    }

    /**
     * Get the trend description in human-readable format.
     *
     * @return "Decreasing", "No Change", "Increasing", "Cleared", or "Unknown"
     */
    public String getTrendDescription() {
        if (cleared) {
            return "Cleared";
        }

        if (trend == null || trend.isBlank()) {
            return "Unknown";
        }

        return switch (trend.trim().toUpperCase()) {
            case "D" -> "Decreasing";
            case "N" -> "No Change";
            case "U" -> "Increasing";
            default -> "Unknown";
        };
    }

    /**
     * Get visual range in statute miles.
     *
     * If range is variable, returns the midpoint of the range.
     * If cleared, returns 0.0.
     *
     * @return visual range in statute miles
     */
    public double getVisualRangeStatuteMiles() {
        if (cleared) {
            return 0.0;
        }

        if (isVariable()) {
            // Return midpoint of variable range
            double avgFeet = (variableLow + variableHigh) / 2.0;
            return avgFeet / 5280.0;
        } else if (visualRangeFeet != null) {
            return visualRangeFeet / 5280.0;
        }
        return 0.0;
    }

    /**
     * Get visual range in meters.
     *
     * If range is variable, returns the midpoint of the range.
     * If cleared, returns 0.0.
     *
     * @return visual range in meters
     */
    public double getVisualRangeMeters() {
        if (cleared) {
            return 0.0;
        }

        if (isVariable()) {
            // Return midpoint of variable range
            double avgFeet = (variableLow + variableHigh) / 2.0;
            return avgFeet * 0.3048;
        } else if (visualRangeFeet != null) {
            return visualRangeFeet * 0.3048;
        }
        return 0.0;
    }

    /**
     * Get a human-readable summary of the RVR.
     *
     * @return formatted string describing the RVR
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Runway ").append(runway).append(": ");

        if (cleared) {
            sb.append("CLEARED (conditions improved)");
            return sb.toString();
        }

        if (isVariable()) {
            if (isGreaterThan()) {
                sb.append("Greater than ");
            } else if (isLessThan()) {
                sb.append("Less than ");
            }
            sb.append(variableLow).append(" to ").append(variableHigh).append(" feet");
        } else {
            if (isGreaterThan()) {
                sb.append("Greater than ");
            } else if (isLessThan()) {
                sb.append("Less than ");
            }
            sb.append(visualRangeFeet).append(" feet");
        }

        if (trend != null && !trend.isBlank()) {
            sb.append(", ").append(getTrendDescription());
        }

        return sb.toString();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        if (cleared) {
            return "RunwayVisualRange{runway='" + runway + "', CLEARED}";
        }

        StringBuilder sb = new StringBuilder("RunwayVisualRange{runway='").append(runway).append("'");

        if (visualRangeFeet != null) {
            sb.append(", visualRange=");
            if (prefix != null && !prefix.isBlank()) sb.append(prefix);
            sb.append(visualRangeFeet).append("ft");
        } else if (isVariable()) {
            sb.append(", variable=");
            if (prefix != null && !prefix.isBlank()) sb.append(prefix);
            sb.append(variableLow).append("-").append(variableHigh).append("ft");
        }

        if (trend != null && !trend.isBlank()) {
            sb.append(", trend=").append(trend);
        }

        sb.append("}");
        return sb.toString();
    }
}
