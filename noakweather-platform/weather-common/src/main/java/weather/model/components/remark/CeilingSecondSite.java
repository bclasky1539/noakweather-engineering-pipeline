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
 * Represents ceiling height at a second observation site.
 * Format: CIG height [LOC] where height is in hundreds of feet.
 *
 * Examples:
 * - CIG 002 RY11 → Ceiling 200 ft at runway 11
 * - CIG 005 RWY06 → Ceiling 500 ft at runway 06
 * - CIG 010 → Ceiling 1000 ft (location unspecified)
 *
 * @param heightFeet ceiling height in feet
 * @param location optional location identifier (e.g., "RY11", "RWY06", "TWR")
 *
 * @author bclasky1539
 *
 */
public record CeilingSecondSite(
        int heightFeet,
        String location
) {

    /**
     * Compact constructor with validation.
     */
    public CeilingSecondSite {
        if (heightFeet < 0) {
            throw new IllegalArgumentException("Ceiling height cannot be negative");
        }
    }

    /**
     * Create a CeilingSecondSite from encoded value (in hundreds of feet).
     *
     * @param hundreds ceiling in hundreds of feet (e.g., 002 = 200 ft)
     * @param location optional location identifier (can be null)
     * @return CeilingSecondSite instance
     * @throws IllegalArgumentException if height is negative
     */
    public static CeilingSecondSite fromHundreds(int hundreds, String location) {
        int heightFeet = hundreds * 100;
        return new CeilingSecondSite(heightFeet, location);
    }

    /**
     * Check if ceiling is low (below 1000 feet).
     * IFR (Instrument Flight Rules) conditions exist when ceiling is below 1000 feet.
     *
     * @return true if ceiling is below 1000 feet
     */
    public boolean isLowCeiling() {
        return heightFeet < 1000;
    }

    /**
     * Check if location is specified.
     *
     * @return true if location is not null and not blank
     */
    public boolean hasLocation() {
        return location != null && !location.isBlank();
    }

    /**
     * Get a human-readable summary.
     *
     * @return formatted summary string
     */
    public String getSummary() {
        if (hasLocation()) {
            return String.format("Ceiling %d ft at %s", heightFeet, location);
        }
        return String.format("Ceiling %d ft", heightFeet);
    }

    @Override
    public String toString() {
        if (hasLocation()) {
            return String.format("CeilingSecondSite{%d ft at %s}", heightFeet, location);
        }
        return String.format("CeilingSecondSite{%d ft}", heightFeet);
    }
}
