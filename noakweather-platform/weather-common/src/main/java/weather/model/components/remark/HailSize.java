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

/**
 * Immutable value object representing hail size from METAR remarks.
 *
 * Hail size in METAR remarks is reported in inches following the GR indicator.
 *
 * Examples:
 * - GR 1/2 → 0.5 inches (marble-sized)
 * - GR 1 3/4 → 1.75 inches (golf ball-sized)
 * - GR 2 → 2.0 inches (tennis ball-sized, significantly severe)
 *
 * Severe weather criteria:
 * - >= 1.0 inch: Severe hail
 * - >= 2.0 inches: Significantly severe hail
 *
 * @param inches Hail size in inches (must be positive and <= 10)
 *
 * @author bclasky1539
 *
 */
public record HailSize(double inches) {

    /**
     * Compact constructor with validation.
     */
    public HailSize {
        if (inches <= 0) {
            throw new IllegalArgumentException(
                    "Hail size must be positive, got: " + inches
            );
        }

        // Reasonable upper limit (softball is ~3.5", grapefruit is ~4-5")
        if (inches > 10) {
            throw new IllegalArgumentException(
                    "Hail size unreasonably large, got: " + inches
            );
        }
    }

    // ==================== Conversion Methods ====================

    /**
     * Get hail size in centimeters.
     *
     * @return size in cm
     */
    public double toCentimeters() {
        return inches * 2.54;
    }

    /**
     * Get hail size in millimeters.
     *
     * @return size in mm
     */
    public double toMillimeters() {
        return inches * 25.4;
    }

    // ==================== Size Category Methods ====================

    /**
     * Get descriptive size category based on common objects.
     *
     * Categories based on National Weather Service guidelines:
     * - Pea: < 0.25 inch
     * - Marble: 0.25 - 0.49 inch
     * - Penny: 0.50 - 0.74 inch
     * - Nickel: 0.75 - 0.87 inch
     * - Quarter: 0.88 - 1.49 inch (severe threshold at 1.0")
     * - Golf ball: 1.50 - 1.74 inch
     * - Tennis ball: 1.75 - 2.49 inch (significantly severe at 2.0")
     * - Baseball: 2.50 - 2.74 inch
     * - Softball: 2.75 - 3.99 inch
     * - Grapefruit: 4.0+ inch
     *
     * @return size description
     */
    public String getSizeCategory() {
        if (inches < 0.25) {
            return "Pea-sized";
        } else if (inches < 0.50) {
            return "Marble-sized";
        } else if (inches < 0.75) {
            return "Penny-sized";
        } else if (inches < 0.88) {
            return "Nickel-sized";
        } else if (inches < 1.50) {
            return "Quarter-sized";
        } else if (inches < 1.75) {
            return "Golf ball-sized";
        } else if (inches < 2.50) {
            return "Tennis ball-sized";
        } else if (inches < 2.75) {
            return "Baseball-sized";
        } else if (inches < 4.0) {
            return "Softball-sized";
        } else {
            return "Grapefruit-sized or larger";
        }
    }

    /**
     * Check if hail meets severe weather criteria (>= 1.0 inch).
     *
     * The National Weather Service defines severe hail as 1 inch diameter or larger,
     * approximately the size of a quarter.
     *
     * @return true if severe hail (>= 1.0 inch)
     */
    public boolean isSevere() {
        return inches >= 1.0;
    }

    /**
     * Check if hail meets significantly severe criteria (>= 2.0 inches).
     *
     * Hail 2 inches or larger (tennis ball size) is considered significantly severe
     * and can cause major damage to property and vehicles.
     *
     * @return true if significantly severe (>= 2.0 inches)
     */
    public boolean isSignificantlySevere() {
        return inches >= 2.0;
    }

    /**
     * Get human-readable description.
     *
     * @return formatted description with size and category
     */
    public String getDescription() {
        return String.format("%.2f inches (%s)", inches, getSizeCategory());
    }

    /**
     * Get summary for display purposes.
     *
     * @return brief summary string
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%.2f\"", inches));

        if (isSignificantlySevere()) {
            sb.append(" (significantly severe)");
        } else if (isSevere()) {
            sb.append(" (severe)");
        }

        return sb.toString();
    }

    // ==================== Factory Methods ====================

    /**
     * Create hail size from inches.
     *
     * @param inches size in inches
     * @return HailSize instance
     * @throws IllegalArgumentException if inches is invalid
     */
    public static HailSize inches(double inches) {
        return new HailSize(inches);
    }

    /**
     * Create hail size from centimeters.
     *
     * @param cm size in centimeters
     * @return HailSize instance
     * @throws IllegalArgumentException if converted size is invalid
     */
    public static HailSize centimeters(double cm) {
        return new HailSize(cm / 2.54);
    }

    /**
     * Create hail size from millimeters.
     *
     * @param mm size in millimeters
     * @return HailSize instance
     * @throws IllegalArgumentException if converted size is invalid
     */
    public static HailSize millimeters(double mm) {
        return new HailSize(mm / 25.4);
    }
}
