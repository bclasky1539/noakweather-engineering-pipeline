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
package weather.model.components.remark;

import weather.model.components.Pressure;

/**
 * Immutable value object representing 3-hour pressure tendency from METAR remarks.
 *
 * Format: 5TCCC where:
 * - 5 = Indicator (always 5)
 * - T = Tendency code (0-8, WMO Code 0200)
 * - CCC = Pressure change in tenths of hPa (3 digits)
 *
 * WMO Code 0200 - Pressure Tendency Characteristic:
 * - 0 = Increasing, then decreasing
 * - 1 = Increasing, then steady; or increasing more slowly
 * - 2 = Increasing (steadily or unsteadily)
 * - 3 = Decreasing or steady, then increasing; or increasing more rapidly
 * - 4 = Steady
 * - 5 = Decreasing, then increasing
 * - 6 = Decreasing, then steady; or decreasing more slowly
 * - 7 = Decreasing (steadily or unsteadily)
 * - 8 = Steady or increasing, then decreasing; or decreasing more rapidly
 *
 * Examples:
 * - "52032" → Increasing then steady, +3.2 hPa
 * - "57045" → Decreasing steadily, -4.5 hPa
 * - "54000" → Steady, 0.0 hPa change
 *
 * @param tendencyCode WMO tendency code (0-8)
 * @param changeHectopascals Pressure change in hectopascals (can be 0.0)
 *
 * @author bclasky1539
 *
 */
public record PressureTendency(
        Integer tendencyCode,
        Double changeHectopascals
) {

    // ==================== Constants ====================

    /** Threshold for significant pressure change (3.0 hPa) */
    public static final double SIGNIFICANT_CHANGE_HPA = 3.0;

    /** Threshold for rapid pressure change (6.0 hPa) */
    public static final double RAPID_CHANGE_HPA = 6.0;

    // ==================== Compact Constructor ====================

    /**
     * Compact constructor with validation.
     */
    public PressureTendency {
        validateTendencyCode(tendencyCode);
        validateChangeValue(changeHectopascals);
    }

    // ==================== Validation ====================

    /**
     * Validate tendency code is in valid range (0-8).
     */
    private static void validateTendencyCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("Tendency code cannot be null");
        }

        if (code < 0 || code > 8) {
            throw new IllegalArgumentException(
                    String.format("Tendency code must be 0-8, got: %d", code)
            );
        }
    }

    /**
     * Validate pressure change value.
     */
    private static void validateChangeValue(Double change) {
        if (change == null) {
            throw new IllegalArgumentException("Pressure change cannot be null");
        }

        if (change < 0.0) {
            throw new IllegalArgumentException(
                    String.format("Pressure change cannot be negative, got: %.1f", change)
            );
        }

        // Sanity check: pressure change > 50 hPa in 3 hours is unrealistic
        if (change > 50.0) {
            throw new IllegalArgumentException(
                    String.format("Pressure change too large (> 50 hPa), got: %.1f", change)
            );
        }
    }

    // ==================== Factory Methods ====================

    /**
     * Factory method to create from METAR remark code.
     *
     * @param tendencyCode WMO tendency code (0-8)
     * @param changeCode 3-digit code representing tenths of hPa (e.g., "032" = 3.2 hPa)
     * @return PressureTendency instance
     */
    public static PressureTendency fromMetar(int tendencyCode, String changeCode) {
        if (changeCode == null || !changeCode.matches("\\d{3}")) {
            throw new IllegalArgumentException(
                    "Change code must be 3 digits, got: " + changeCode
            );
        }

        double change = Integer.parseInt(changeCode) / 10.0;
        return new PressureTendency(tendencyCode, change);
    }

    /**
     * Factory method with explicit values.
     *
     * @param tendencyCode WMO tendency code (0-8)
     * @param changeHpa Pressure change in hectopascals
     * @return PressureTendency instance
     */
    public static PressureTendency of(int tendencyCode, double changeHpa) {
        return new PressureTendency(tendencyCode, changeHpa);
    }

    // ==================== Query Methods ====================

    /**
     * Check if pressure is increasing (codes 0-3).
     *
     * @return true if pressure is increasing
     */
    public boolean isIncreasing() {
        return tendencyCode >= 0 && tendencyCode <= 3;
    }

    /**
     * Check if pressure is decreasing (codes 5-8).
     *
     * @return true if pressure is decreasing
     */
    public boolean isDecreasing() {
        return tendencyCode >= 5 && tendencyCode <= 8;
    }

    /**
     * Check if pressure is steady (code 4).
     *
     * @return true if pressure is steady
     */
    public boolean isSteady() {
        return tendencyCode == 4;
    }

    /**
     * Check if this represents a significant change (>= 3.0 hPa).
     *
     * @return true if change is significant
     */
    public boolean isSignificant() {
        return changeHectopascals >= SIGNIFICANT_CHANGE_HPA;
    }

    /**
     * Check if this represents a rapid change (>= 6.0 hPa).
     * Rapid pressure changes are meteorologically significant.
     *
     * @return true if change is rapid
     */
    public boolean isRapidChange() {
        return changeHectopascals >= RAPID_CHANGE_HPA;
    }

    /**
     * Check if pressure change is negligible (< 1.0 hPa).
     *
     * @return true if change is minimal
     */
    public boolean isNegligible() {
        return changeHectopascals < 1.0;
    }

    // ==================== Conversion Methods ====================

    /**
     * Get pressure change in inches of mercury.
     *
     * @return pressure change in inHg
     */
    public double getChangeInchesHg() {
        return changeHectopascals * Pressure.HPA_TO_INHG;
    }

    /**
     * Get signed pressure change (positive for increasing, negative for decreasing).
     * This represents the actual direction of change.
     *
     * @return signed pressure change in hPa
     */
    public double getSignedChange() {
        if (isDecreasing()) {
            return -changeHectopascals;
        }
        return changeHectopascals;
    }

    // ==================== Description Methods ====================

    /**
     * Get human-readable description of the tendency code.
     *
     * @return tendency description
     */
    public String getTendencyDescription() {
        return switch (tendencyCode) {
            case 0 -> "Increasing, then decreasing";
            case 1 -> "Increasing, then steady";
            case 2 -> "Increasing";
            case 3 -> "Increasing rapidly";
            case 4 -> "Steady";
            case 5 -> "Decreasing, then increasing";
            case 6 -> "Decreasing, then steady";
            case 7 -> "Decreasing";
            case 8 -> "Decreasing rapidly";
            default -> "Unknown";
        };
    }

    /**
     * Get short tendency description (single word).
     *
     * @return short description
     */
    public String getShortDescription() {
        return switch (tendencyCode) {
            case 0, 1, 2, 3 -> "Rising";
            case 4 -> "Steady";
            case 5, 6, 7, 8 -> "Falling";
            default -> "Unknown";
        };
    }

    /**
     * Get change magnitude description.
     *
     * @return magnitude description
     */
    public String getChangeMagnitude() {
        if (changeHectopascals >= RAPID_CHANGE_HPA) {
            return "Rapid";
        } else if (changeHectopascals >= SIGNIFICANT_CHANGE_HPA) {
            return "Significant";
        } else if (changeHectopascals >= 1.0) {
            return "Moderate";
        } else if (changeHectopascals > 0.0) {
            return "Slight";
        } else {
            return "None";
        }
    }

    /**
     * Get complete human-readable summary.
     *
     * @return formatted summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();

        sb.append("Pressure tendency: ");
        sb.append(getTendencyDescription());
        sb.append(", change of ");

        if (isDecreasing()) {
            sb.append("-");
        } else if (isIncreasing()) {
            sb.append("+");
        }

        sb.append(String.format("%.1f hPa", changeHectopascals));
        sb.append(String.format(" (%.2f inHg)", getChangeInchesHg()));

        if (isRapidChange()) {
            sb.append(" [RAPID]");
        } else if (isSignificant()) {
            sb.append(" [SIGNIFICANT]");
        }

        return sb.toString();
    }

    /**
     * Get weather implications based on tendency.
     *
     * @return weather implication description
     */
    public String getWeatherImplication() {
        if (isRapidChange()) {
            return getRapidChangeImplication();
        }

        if (isSignificant()) {
            return getSignificantChangeImplication();
        }

        return getModerateChangeImplication();
    }

    private String getRapidChangeImplication() {
        if (isIncreasing()) {
            return "Rapid pressure rise - weather likely improving quickly, storm clearing";
        } else if (isDecreasing()) {
            return "Rapid pressure fall - severe weather approaching, deteriorating conditions";
        }
        return "Steady pressure - stable weather conditions";
    }

    private String getSignificantChangeImplication() {
        if (isIncreasing()) {
            return "Rising pressure - improving weather, clearing conditions";
        } else if (isDecreasing()) {
            return "Falling pressure - weather deteriorating, potential storm development";
        }
        return "Steady pressure - weather conditions stable";
    }

    private String getModerateChangeImplication() {
        if (isIncreasing()) {
            return "Slight pressure rise - weather becoming more settled";
        } else if (isDecreasing()) {
            return "Slight pressure fall - weather may deteriorate slightly";
        }
        return "Pressure steady - no significant weather change expected";
    }

    // ==================== Formatting Methods ====================

    /**
     * Format as METAR remark code (5TCCC).
     *
     * @return METAR format string
     */
    public String toMetarCode() {
        int changeCode = (int) Math.round(changeHectopascals * 10);
        return String.format("5%d%03d", tendencyCode, changeCode);
    }

    /**
     * Format with unit label.
     *
     * @return formatted string with unit
     */
    public String getFormattedChange() {
        if (isDecreasing()) {
            return String.format("-%.1f hPa", changeHectopascals);
        } else if (isIncreasing()) {
            return String.format("+%.1f hPa", changeHectopascals);
        } else {
            return String.format("%.1f hPa", changeHectopascals);
        }
    }
}
