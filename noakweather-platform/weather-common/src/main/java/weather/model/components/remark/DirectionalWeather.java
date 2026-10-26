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

import weather.exception.InvalidObservationException;
import weather.model.components.PresentWeather;

import java.util.List;

/**
 * Immutable value object representing a present-weather phenomenon reported
 * in remarks with where it is: a location qualifier, compass direction(s),
 * all quadrants, or the vicinity (VC) prefix of the weather code itself.
 * <p>
 * Examples:
 * - "VCSH E SE" → DirectionalWeather(VCSH, null, ["E", "SE"], false)
 * - "VCSH-NE"   → DirectionalWeather(VCSH, null, ["NE"], false)
 * - "SH DSNT N" → DirectionalWeather(SH, "DSNT", ["N"], false)
 * - "SHRA OHD"  → DirectionalWeather(SHRA, "OHD", null, false)
 * - "-RA ALQDS" → DirectionalWeather(-RA, null, null, true)
 * - "VCSH"      → DirectionalWeather(VCSH, null, null, false); VC is the location
 *
 * @param presentWeather The present-weather phenomenon (reuses existing PresentWeather parsing)
 * @param location       Location qualifier (OHD, VC, DSNT, DSIPTD, TOP, TR), or null if none given
 * @param directions     One or more compass directions associated with the phenomenon, or null if none given
 * @param allQuadrants   True if reported in all quadrants (ALQDS); mutually exclusive with directions
 * @author bclasky1539
 *
 */
public record DirectionalWeather(
        PresentWeather presentWeather,
        String location,
        List<String> directions,
        boolean allQuadrants
) {

    /**
     * Location qualifiers a remark weather code may carry (#129). Same vocabulary as
     * thunderstorm locations, except ALQDS, which is the {@code allQuadrants} flag.
     */
    static final List<String> VALID_LOCATIONS = List.of("OHD", "VC", "DSNT", "DSIPTD", "TOP", "TR");

    /**
     * Compact constructor with validation and defensive copying.
     *
     * @throws IllegalArgumentException    if presentWeather is null, or directions are combined with allQuadrants
     * @throws InvalidObservationException if location is not a known qualifier
     */
    public DirectionalWeather {
        if (presentWeather == null) {
            throw new IllegalArgumentException("Present weather cannot be null");
        }
        if (location != null && !VALID_LOCATIONS.contains(location)) {
            throw new InvalidObservationException("DirectionalWeather.location", location,
                    "Invalid location qualifier (valid: " + String.join(", ", VALID_LOCATIONS) + ")");
        }
        directions = directions != null && !directions.isEmpty() ? List.copyOf(directions) : null;
        if (allQuadrants && directions != null) {
            throw new IllegalArgumentException("Directions and all-quadrants are mutually exclusive");
        }
    }

    /**
     * Convenience constructor without a location qualifier.
     */
    public DirectionalWeather(PresentWeather presentWeather, List<String> directions, boolean allQuadrants) {
        this(presentWeather, null, directions, allQuadrants);
    }

    /**
     * Convenience constructor for the common case: directions only (or none).
     */
    public DirectionalWeather(PresentWeather presentWeather, List<String> directions) {
        this(presentWeather, null, directions, false);
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
     * Check if a location qualifier is present.
     *
     * @return true if a location qualifier (e.g. DSNT, OHD) is recorded
     */
    public boolean hasLocation() {
        return location != null;
    }

    /**
     * Get a human-readable summary.
     * Examples: "Showers in vicinity: E, SE", "Showers (DSNT): N", "Showers in vicinity"
     *
     * @return formatted summary string
     */
    public String getSummary() {
        String base = presentWeather.getDescription();
        if (hasLocation()) {
            base = base + " (" + location + ")";
        }
        if (allQuadrants) {
            return base + ": all quadrants";
        }
        return hasDirections() ? base + ": " + String.join(", ", directions) : base;
    }
}
