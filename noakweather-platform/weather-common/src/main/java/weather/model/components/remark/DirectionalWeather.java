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
package weather.model.components.remark;

import weather.model.components.PresentWeather;

import java.util.List;

/**
 * Immutable value object representing a present-weather phenomenon reported
 * in remarks, optionally with associated compass direction(s), typically
 * restating a vicinity or nearby-occurring phenomenon.
 * <p>
 * Examples:
 * - "VCSH E SE" → DirectionalWeather(PresentWeather("VCSH"), ["E", "SE"])
 * - "VCSH" → DirectionalWeather(PresentWeather("VCSH"), null)
 *
 * @param presentWeather The present-weather phenomenon (reuses existing PresentWeather parsing)
 * @param directions     One or more compass directions associated with the phenomenon, or null if none given
 * @author bclasky1539
 *
 */
public record DirectionalWeather(
        PresentWeather presentWeather,
        List<String> directions,
        boolean allQuadrants
) {

    /**
     * Compact constructor with validation and defensive copying.
     */
    public DirectionalWeather {
        if (presentWeather == null) {
            throw new IllegalArgumentException("Present weather cannot be null");
        }
        directions = directions != null && !directions.isEmpty() ? List.copyOf(directions) : null;
        if (allQuadrants && directions != null) {
            throw new IllegalArgumentException("Directions and all-quadrants are mutually exclusive");
        }
    }

    /**
     * Convenience constructor for the common case: directions only (or none).
     */
    public DirectionalWeather(PresentWeather presentWeather, List<String> directions) {
        this(presentWeather, directions, false);
    }

    /**
     * Check if direction information is present.
     *
     * @return true if one or more directions are recorded
     */
    public boolean hasDirections() {
        return directions != null;
    }

    /**
     * Get a human-readable summary.
     * Examples: "Showers in vicinity: E, SE", "Showers in vicinity"
     *
     * @return formatted summary string
     */
    public String getSummary() {
        String base = presentWeather.getDescription();
        if (allQuadrants) {
            return base + ": all quadrants";
        }
        return hasDirections() ? base + ": " + String.join(", ", directions) : base;
    }
}
