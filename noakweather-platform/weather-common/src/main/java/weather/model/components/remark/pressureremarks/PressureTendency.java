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
package weather.model.components.remark.pressureremarks;

import weather.exception.InvalidObservationException;
import weather.model.components.Pressure;

/**
 * Immutable value object representing 3-hour pressure tendency from METAR remarks.
 * <p>
 * Format: 5TCCC where:
 * - 5 = Indicator (always 5)
 * - T = Tendency code (0-8, WMO Code 0200), or "/" when missing
 * - CCC = Pressure change in tenths of hPa (3 digits), or "///" when missing
 * <p>
 * WMO Code 0200 - Pressure Tendency Characteristic:
 * - 0 = Rising, then falling (pressure the same or higher than 3 hours ago)
 * - 1 = Rising, then steady; or rising, then rising more slowly (higher)
 * - 2 = Rising (steadily or unsteadily) (higher)
 * - 3 = Falling or steady, then rising; or rising, then rising more rapidly (higher)
 * - 4 = Steady (the same as 3 hours ago)
 * - 5 = Falling, then rising (the same or lower than 3 hours ago)
 * - 6 = Falling, then steady; or falling, then falling more slowly (lower)
 * - 7 = Falling (steadily or unsteadily) (lower)
 * - 8 = Steady or rising, then falling; or falling, then falling more rapidly (lower)
 * <p>
 * The code describes how pressure changed over the 3 hours; the net direction is
 * rising for codes 0-3, steady for 4 and falling for 5-8. The size of the change
 * (none, slight, moderate, significant, rapid) comes only from the change value,
 * classified once against the thresholds below (#218).
 * <p>
 * Examples:
 * - "52032" → Rising then steady, +3.2 hPa
 * - "57045" → Falling steadily, -4.5 hPa
 * - "54000" → Steady, 0.0 hPa change
 * - "54///" → Steady, change not reported
 * - "5/123" → Tendency not reported, change of 12.3 hPa
 * - "5////" → Group reported with every element missing (see {@link #missing()})
 * <p>
 * Each element is reported independently, so either component may be null when
 * that element was sent as solidi. A report with no "5" group at all has no
 * PressureTendency; a "5////" group is a PressureTendency with both components
 * null, so the two cases stay distinct (#198).
 * <p>
 * Methods that need the tendency code (direction) return false or "Not reported"
 * when it is missing; methods that need the change (magnitude) do the same when
 * the change is missing. A signed change needs both.
 *
 * @param tendencyCode       WMO tendency code (0-8), or null when reported as "/"
 * @param changeHectopascals Pressure change in hectopascals (can be 0.0), or null when reported as "///"
 * @author bclasky1539
 *
 */
public record PressureTendency(
        Integer tendencyCode,
        Double changeHectopascals
) {

    // ==================== Constants ====================

    /**
     * Threshold for moderate pressure change (1.0 hPa)
     */
    public static final double MODERATE_CHANGE_HPA = 1.0;

    /**
     * Threshold for significant pressure change (3.0 hPa)
     */
    public static final double SIGNIFICANT_CHANGE_HPA = 3.0;

    /**
     * Threshold for rapid pressure change (6.0 hPa)
     */
    public static final double RAPID_CHANGE_HPA = 6.0;

    /**
     * METAR text of a missing tendency code element
     */
    private static final String MISSING_TENDENCY_CODE = "/";

    /**
     * METAR text of a missing pressure change element
     */
    private static final String MISSING_CHANGE_CODE = "///";

    /**
     * Description returned for an element that was not reported
     */
    private static final String MISSING_DESCRIPTION = "Not reported";

    private static final String SUMMARY_PREFIX = "Pressure tendency: ";

    /**
     * Weather implication for no change, and for a steady tendency (code 4) with a
     * moderate or smaller change
     */
    private static final String STEADY_IMPLICATION = "Pressure steady - no significant weather change expected";

    // ==================== Change Magnitude ====================

    /**
     * Size of a pressure change, classified once against the thresholds above so that
     * every derived value (isSignificant, isRapidChange, isNegligible, getChangeMagnitude,
     * getWeatherImplication) agrees (#218).
     */
    private enum Magnitude {
        NONE("None"),
        SLIGHT("Slight"),
        MODERATE("Moderate"),
        SIGNIFICANT("Significant"),
        RAPID("Rapid");

        private final String label;

        Magnitude(String label) {
            this.label = label;
        }

        static Magnitude fromChange(double changeHpa) {
            if (changeHpa >= RAPID_CHANGE_HPA) {
                return RAPID;
            } else if (changeHpa >= SIGNIFICANT_CHANGE_HPA) {
                return SIGNIFICANT;
            } else if (changeHpa >= MODERATE_CHANGE_HPA) {
                return MODERATE;
            } else if (changeHpa > 0.0) {
                return SLIGHT;
            }
            return NONE;
        }
    }

    // ==================== Compact Constructor ====================

    /**
     * Compact constructor with validation.
     * Either component may be null (element reported as solidi); a component that
     * is present must be valid.
     */
    public PressureTendency {
        if (tendencyCode != null) {
            validateTendencyCode(tendencyCode);
        }
        if (changeHectopascals != null) {
            validateChangeValue(changeHectopascals);
        }
    }

    // ==================== Validation ====================

    /**
     * Validate tendency code is in valid range (0-8).
     */
    private static void validateTendencyCode(int code) {
        if (code < 0 || code > 8) {
            throw new InvalidObservationException(
                    "PressureTendency.tendencyCode", String.valueOf(code), "Tendency code must be 0-8");
        }
    }

    /**
     * Validate pressure change value.
     */
    private static void validateChangeValue(double change) {
        if (change < 0.0) {
            throw new InvalidObservationException(
                    "PressureTendency.changeHectopascals", String.valueOf(change), "Pressure change cannot be negative");
        }

        // Sanity check: pressure change > 50 hPa in 3 hours is unrealistic
        if (change > 50.0) {
            throw new InvalidObservationException(
                    "PressureTendency.changeHectopascals", String.valueOf(change), "Pressure change too large (> 50 hPa)");
        }
    }

    // ==================== Factory Methods ====================

    /**
     * Factory method to create from METAR remark code.
     *
     * @param tendencyCode WMO tendency code (0-8)
     * @param changeCode   3-digit code representing tenths of hPa (e.g., "032" = 3.2 hPa)
     * @return PressureTendency instance
     */
    public static PressureTendency fromMetar(int tendencyCode, String changeCode) {
        if (changeCode == null) {
            throw new IllegalArgumentException("Change code cannot be null");
        }

        if (!changeCode.matches("\\d{3}")) {
            throw new InvalidObservationException(
                    "PressureTendency.changeCode", changeCode, "Change code must be 3 digits");
        }

        double change = Integer.parseInt(changeCode) / 10.0;
        return new PressureTendency(tendencyCode, change);
    }

    /**
     * Factory method to create from the two elements of a METAR "5TCCC" group as
     * reported, where either element may be sent as solidi when missing (#198).
     * <p>
     * Examples:
     * - ("7", "045") → code 7, 4.5 hPa
     * - ("4", "///") → code 4, change not reported
     * - ("/", "123") → code not reported, 12.3 hPa
     * - ("/", "///") → every element missing
     *
     * @param tendencyCodeText the tendency code element: one digit, or "/"
     * @param changeCodeText   the change element: 3 digits in tenths of hPa, or "///"
     * @return PressureTendency instance
     * @throws InvalidObservationException if an element is not in one of those forms,
     *                                     or a present value fails validation
     */
    public static PressureTendency fromMetarGroup(String tendencyCodeText, String changeCodeText) {
        if (tendencyCodeText == null || changeCodeText == null) {
            throw new IllegalArgumentException("Tendency code and change code text cannot be null");
        }

        return new PressureTendency(parseTendencyCodeText(tendencyCodeText), parseChangeCodeText(changeCodeText));
    }

    /**
     * Factory method with explicit values.
     *
     * @param tendencyCode WMO tendency code (0-8)
     * @param changeHpa    Pressure change in hectopascals
     * @return PressureTendency instance
     */
    public static PressureTendency of(int tendencyCode, double changeHpa) {
        return new PressureTendency(tendencyCode, changeHpa);
    }

    /**
     * Factory method for a pressure tendency group reported with every element
     * missing ("5////"). The station sent the group, but neither the tendency code
     * nor the change was available (#198).
     *
     * @return a PressureTendency with both components null
     */
    public static PressureTendency missing() {
        return new PressureTendency(null, null);
    }

    private static Integer parseTendencyCodeText(String text) {
        if (MISSING_TENDENCY_CODE.equals(text)) {
            return null;
        }
        if (!text.matches("\\d")) {
            throw new InvalidObservationException(
                    "PressureTendency.tendencyCode", text, "Tendency code must be one digit or /");
        }
        return Integer.parseInt(text);
    }

    private static Double parseChangeCodeText(String text) {
        if (MISSING_CHANGE_CODE.equals(text)) {
            return null;
        }
        if (!text.matches("\\d{3}")) {
            throw new InvalidObservationException(
                    "PressureTendency.changeCode", text, "Change code must be 3 digits or ///");
        }
        return Integer.parseInt(text) / 10.0;
    }

    // ==================== Element Presence ====================

    /**
     * Check if the tendency code element was reported (not "/").
     *
     * @return true if the tendency code is available
     */
    public boolean hasTendencyCode() {
        return tendencyCode != null;
    }

    /**
     * Check if the pressure change element was reported (not "///").
     *
     * @return true if the pressure change is available
     */
    public boolean hasChange() {
        return changeHectopascals != null;
    }

    /**
     * Check if the group was reported with every element missing (5////).
     *
     * @return true if neither the tendency code nor the change is available
     */
    public boolean isMissing() {
        return !hasTendencyCode() && !hasChange();
    }

    // ==================== Query Methods ====================

    /**
     * Check if pressure is rising (codes 0-3).
     *
     * @return true if pressure is rising; false if the tendency code is missing
     */
    public boolean isRising() {
        return hasTendencyCode() && tendencyCode >= 0 && tendencyCode <= 3;
    }

    /**
     * Check if pressure is falling (codes 5-8).
     *
     * @return true if pressure is falling; false if the tendency code is missing
     */
    public boolean isFalling() {
        return hasTendencyCode() && tendencyCode >= 5 && tendencyCode <= 8;
    }

    /**
     * Check if pressure is steady (code 4).
     *
     * @return true if pressure is steady; false if the tendency code is missing
     */
    public boolean isSteady() {
        return hasTendencyCode() && tendencyCode == 4;
    }

    /**
     * Check if this represents a significant change (>= 3.0 hPa).
     *
     * @return true if change is significant; false if the change is missing
     */
    public boolean isSignificant() {
        return hasChange() && magnitude().compareTo(Magnitude.SIGNIFICANT) >= 0;
    }

    /**
     * Check if this represents a rapid change (>= 6.0 hPa).
     * Rapid pressure changes are meteorologically significant.
     *
     * @return true if change is rapid; false if the change is missing
     */
    public boolean isRapidChange() {
        return hasChange() && magnitude() == Magnitude.RAPID;
    }

    /**
     * Check if pressure change is negligible (< 1.0 hPa).
     * An unknown change is not treated as negligible.
     *
     * @return true if change is minimal; false if the change is missing
     */
    public boolean isNegligible() {
        return hasChange() && magnitude().compareTo(Magnitude.SLIGHT) <= 0;
    }

    /**
     * Classify the change against the thresholds. Call only when {@link #hasChange()} is true.
     */
    private Magnitude magnitude() {
        return Magnitude.fromChange(changeHectopascals);
    }

    // ==================== Conversion Methods ====================

    /**
     * Get pressure change in inches of mercury.
     *
     * @return pressure change in inHg, or null if the change is missing
     */
    public Double getChangeInchesHg() {
        if (!hasChange()) {
            return null;
        }
        return changeHectopascals * Pressure.HPA_TO_INHG;
    }

    /**
     * Get signed pressure change (positive for rising, negative for falling).
     * This represents the actual direction of change, which comes from the tendency
     * code, so both elements are needed.
     *
     * @return signed pressure change in hPa, or null if the tendency code or the change is missing
     */
    public Double getSignedChange() {
        if (!hasTendencyCode() || !hasChange()) {
            return null;
        }
        if (isFalling()) {
            return -changeHectopascals;
        }
        return changeHectopascals;
    }

    // ==================== Description Methods ====================

    /**
     * Get human-readable description of the tendency code, as worded in WMO code
     * table 0200 (rising/falling terminology).
     *
     * @return tendency description, or "Not reported" if the tendency code is missing
     */
    public String getTendencyDescription() {
        if (!hasTendencyCode()) {
            return MISSING_DESCRIPTION;
        }
        return switch (tendencyCode) {
            case 0 -> "Rising, then falling";
            case 1 -> "Rising, then steady; or rising, then rising more slowly";
            case 2 -> "Rising (steadily or unsteadily)";
            case 3 -> "Falling or steady, then rising; or rising, then rising more rapidly";
            case 4 -> "Steady";
            case 5 -> "Falling, then rising";
            case 6 -> "Falling, then steady; or falling, then falling more slowly";
            case 7 -> "Falling (steadily or unsteadily)";
            case 8 -> "Steady or rising, then falling; or falling, then falling more rapidly";
            default -> "Unknown";
        };
    }

    /**
     * Get short tendency description (single word).
     *
     * @return short description, or "Not reported" if the tendency code is missing
     */
    public String getShortDescription() {
        if (!hasTendencyCode()) {
            return MISSING_DESCRIPTION;
        }
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
     * @return magnitude description, or "Not reported" if the change is missing
     */
    public String getChangeMagnitude() {
        if (!hasChange()) {
            return MISSING_DESCRIPTION;
        }
        return magnitude().label;
    }

    /**
     * Get complete human-readable summary. A group with a missing element ends with
     * its METAR code, e.g. "(54///)".
     *
     * @return formatted summary
     */
    public String getSummary() {
        if (isMissing()) {
            return SUMMARY_PREFIX + MISSING_DESCRIPTION + " (" + toMetarCode() + ")";
        }

        StringBuilder sb = new StringBuilder(SUMMARY_PREFIX);
        sb.append(getTendencyDescription());
        appendChangeSummary(sb);

        if (!hasTendencyCode() || !hasChange()) {
            sb.append(" (").append(toMetarCode()).append(")");
        }

        return sb.toString();
    }

    private void appendChangeSummary(StringBuilder sb) {
        if (!hasChange()) {
            sb.append(", change not reported");
            return;
        }

        sb.append(", change of ");
        sb.append(getFormattedChange());
        sb.append(String.format(" (%.2f inHg)", getChangeInchesHg()));

        if (isRapidChange()) {
            sb.append(" [RAPID]");
        } else if (isSignificant()) {
            sb.append(" [SIGNIFICANT]");
        }
    }

    /**
     * Get weather implications based on tendency. Both the direction (tendency code)
     * and the size (change) are needed. The level used here is the same one returned
     * by {@link #getChangeMagnitude()} (#218).
     *
     * @return weather implication description
     */
    public String getWeatherImplication() {
        if (!hasTendencyCode() || !hasChange()) {
            return "Pressure tendency not fully reported - no implication available";
        }

        return switch (magnitude()) {
            case RAPID -> byDirection(
                    "Rapid pressure rise - weather likely improving quickly, storm clearing",
                    "Rapid pressure fall - severe weather approaching, deteriorating conditions",
                    "Steady pressure - stable weather conditions");
            case SIGNIFICANT -> byDirection(
                    "Rising pressure - improving weather, clearing conditions",
                    "Falling pressure - weather deteriorating, potential storm development",
                    "Steady pressure - weather conditions stable");
            case MODERATE -> byDirection(
                    "Moderate pressure rise - weather becoming more settled",
                    "Moderate pressure fall - weather may deteriorate",
                    STEADY_IMPLICATION);
            case SLIGHT -> byDirection(
                    "Slight pressure rise - little weather change expected",
                    "Slight pressure fall - little weather change expected",
                    STEADY_IMPLICATION);
            case NONE -> STEADY_IMPLICATION;
        };
    }

    /**
     * Pick the wording that matches the net direction of the tendency code.
     */
    private String byDirection(String rising, String falling, String steady) {
        if (isRising()) {
            return rising;
        } else if (isFalling()) {
            return falling;
        }
        return steady;
    }

    // ==================== Formatting Methods ====================

    /**
     * Format as METAR remark code (5TCCC). Each element is written on its own, with
     * solidi for a missing element, so the result matches the group as reported:
     * "57045", "54///", "5/123" or "5////".
     *
     * @return METAR format string
     */
    public String toMetarCode() {
        String codePart = hasTendencyCode() ? String.valueOf(tendencyCode) : MISSING_TENDENCY_CODE;
        String changePart = hasChange()
                ? String.format("%03d", (int) Math.round(changeHectopascals * 10))
                : MISSING_CHANGE_CODE;
        return "5" + codePart + changePart;
    }

    /**
     * Format with unit label. The sign comes from the tendency code, so a change
     * without a tendency code is shown unsigned.
     *
     * @return formatted string with unit, or "Not reported" if the change is missing
     */
    public String getFormattedChange() {
        if (!hasChange()) {
            return MISSING_DESCRIPTION;
        } else if (isFalling()) {
            return String.format("-%.1f hPa", changeHectopascals);
        } else if (isRising()) {
            return String.format("+%.1f hPa", changeHectopascals);
        } else {
            return String.format("%.1f hPa", changeHectopascals);
        }
    }
}
