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

import java.util.Objects;

/**
 * Immutable value object representing precipitation amount from METAR remarks.
 *
 * Precipitation amounts in METAR remarks are reported in hundredths of inches
 * over specific time periods (1, 3, 6, or 24 hours).
 *
 * Examples:
 * - P0015 → 0.15 inches in last hour
 * - 60009 → 0.09 inches in last 6 hours
 * - 70125 → 1.25 inches in last 24 hours
 * - P//// → Trace precipitation (< 0.01 inches)
 *
 * @param inches Precipitation amount in inches, null for trace/missing
 * @param periodHours Time period (1, 3, 6, or 24 hours)
 * @param isTrace True if trace precipitation (< 0.01 inches)
 *
 * @author bclasky1539
 *
 */
public record PrecipitationAmount(
        Double inches,
        int periodHours,
        boolean isTrace
) {

    /**
     * Compact constructor with validation.
     */
    public PrecipitationAmount {
        validatePeriodHours(periodHours);
        validateInchesAndTrace(inches, isTrace);
    }

    // ==================== Validation Helper Methods ====================

    private static void validatePeriodHours(int periodHours) {
        if (periodHours != 1 && periodHours != 3 && periodHours != 6 && periodHours != 24) {
            throw new IllegalArgumentException(
                    "Period must be 1, 3, 6, or 24 hours, got: " + periodHours
            );
        }
    }

    private static void validateInchesAndTrace(Double inches, boolean isTrace) {
        if (inches != null && inches < 0) {
            throw new IllegalArgumentException(
                    "Precipitation amount cannot be negative: " + inches
            );
        }

        // If trace, inches should be null or very small
        if (isTrace && inches != null && inches >= 0.01) {
            throw new IllegalArgumentException(
                    "Trace precipitation should have null or < 0.01 inches, got: " + inches
            );
        }
    }

    // ==================== Query Methods ====================

    /**
     * Get precipitation amount in millimeters.
     *
     * @return precipitation in mm, or null if trace/missing
     */
    public Double toMillimeters() {
        if (inches == null) {
            return null;
        }
        return inches * 25.4;  // 1 inch = 25.4 mm
    }

    /**
     * Get human-readable description.
     *
     * @return formatted description
     */
    public String getDescription() {
        if (isTrace) {
            return String.format("Trace precipitation (%d hour)", periodHours);
        }

        if (inches == null) {
            return String.format("Precipitation data missing (%d hour)", periodHours);
        }

        return String.format("%.2f inches (%d hour)", inches, periodHours);
    }

    /**
     * Check if precipitation is measurable (not trace).
     *
     * @return true if measurable amount
     */
    public boolean isMeasurable() {
        return !isTrace && inches != null && inches >= 0.01;
    }

    /**
     * Check if this is hourly precipitation.
     *
     * @return true if 1-hour period
     */
    public boolean isHourly() {
        return periodHours == 1;
    }

    /**
     * Check if this is 6-hour precipitation.
     *
     * @return true if 6-hour period
     */
    public boolean isSixHour() {
        return periodHours == 6;
    }

    /**
     * Check if this is 24-hour precipitation.
     *
     * @return true if 24-hour period
     */
    public boolean isTwentyFourHour() {
        return periodHours == 24;
    }

    // ==================== Factory Methods ====================

    /**
     * Create precipitation amount from encoded value.
     *
     * @param encodedValue 4-5 digit value (e.g., "0015" = 0.15 inches)
     * @param periodHours Time period (1, 3, 6, or 24)
     * @return PrecipitationAmount instance
     * @throws IllegalArgumentException if encodedValue is invalid
     */
    public static PrecipitationAmount fromEncoded(String encodedValue, int periodHours) {
        Objects.requireNonNull(encodedValue, "Encoded value cannot be null");

        if (encodedValue.isBlank()) {
            throw new IllegalArgumentException("Encoded value cannot be blank");
        }

        // Check for trace (all slashes)
        if (encodedValue.matches("/+")) {
            return new PrecipitationAmount(null, periodHours, true);
        }

        // Parse as hundredths of inches
        try {
            int hundredths = Integer.parseInt(encodedValue);
            double inches = hundredths / 100.0;
            return new PrecipitationAmount(inches, periodHours, false);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid encoded value: " + encodedValue, e);
        }
    }

    /**
     * Create trace precipitation.
     *
     * @param periodHours Time period (1, 3, 6, or 24)
     * @return PrecipitationAmount for trace
     */
    public static PrecipitationAmount trace(int periodHours) {
        return new PrecipitationAmount(null, periodHours, true);
    }

    /**
     * Create precipitation amount in inches.
     *
     * @param inches Amount in inches
     * @param periodHours Time period (1, 3, 6, or 24)
     * @return PrecipitationAmount instance
     */
    public static PrecipitationAmount inches(double inches, int periodHours) {
        return new PrecipitationAmount(inches, periodHours, false);
    }
}
