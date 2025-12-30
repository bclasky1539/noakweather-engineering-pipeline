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
 * Represents a variable ceiling height observation.
 * Format: CIG minVmax where values are in hundreds of feet.
 *
 * Examples:
 * - CIG 005V010 → Ceiling varies between 500 and 1000 feet
 * - CIG 020V035 → Ceiling varies between 2000 and 3500 feet
 * - CIG 010V015 → Ceiling varies between 1000 and 1500 feet
 *
 * @param minimumHeightFeet minimum ceiling height in feet
 * @param maximumHeightFeet maximum ceiling height in feet
 *
 * @author bclasky1539
 *
 */
public record VariableCeiling(
        int minimumHeightFeet,
        int maximumHeightFeet
) {

    /**
     * Compact constructor with validation.
     */
    public VariableCeiling {
        if (minimumHeightFeet < 0 || maximumHeightFeet < 0) {
            throw new IllegalArgumentException("Ceiling heights cannot be negative");
        }

        if (minimumHeightFeet > maximumHeightFeet) {
            throw new IllegalArgumentException(
                    String.format("Minimum ceiling (%d ft) cannot exceed maximum (%d ft)",
                            minimumHeightFeet, maximumHeightFeet));
        }
    }

    /**
     * Create a VariableCeiling from encoded values (in hundreds of feet).
     *
     * @param minHundreds minimum ceiling in hundreds of feet (e.g., 005 = 500 ft)
     * @param maxHundreds maximum ceiling in hundreds of feet (e.g., 010 = 1000 ft)
     * @return VariableCeiling instance
     * @throws IllegalArgumentException if min > max or values are negative
     */
    public static VariableCeiling fromHundreds(int minHundreds, int maxHundreds) {
        int minFeet = minHundreds * 100;
        int maxFeet = maxHundreds * 100;

        return new VariableCeiling(minFeet, maxFeet);
    }

    /**
     * Get the range of the variable ceiling in feet.
     *
     * @return ceiling range in feet
     */
    public int getRangeFeet() {
        return maximumHeightFeet - minimumHeightFeet;
    }

    /**
     * Check if ceiling is low (below 1000 feet).
     * IFR (Instrument Flight Rules) conditions exist when ceiling is below 1000 feet.
     *
     * @return true if minimum ceiling is below 1000 feet
     */
    public boolean isLowCeiling() {
        return minimumHeightFeet < 1000;
    }

    /**
     * Check if ceiling varies significantly (range >= 500 feet).
     *
     * @return true if ceiling range is 500 feet or more
     */
    public boolean isSignificantVariation() {
        return getRangeFeet() >= 500;
    }

    /**
     * Get a human-readable summary.
     *
     * @return formatted summary string
     */
    public String getSummary() {
        return String.format("Variable ceiling: %d-%d ft", minimumHeightFeet, maximumHeightFeet);
    }

    @Override
    public String toString() {
        return String.format("VariableCeiling{%d-%d ft}", minimumHeightFeet, maximumHeightFeet);
    }
}
