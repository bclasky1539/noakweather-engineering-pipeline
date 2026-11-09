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
import java.util.stream.Collectors;

/**
 * Immutable value object representing a present-weather phenomenon reported
 * in remarks with where it is: a location qualifier, compass direction(s),
 * all quadrants, or the vicinity (VC) prefix of the weather code itself.
 * <p>
 * Directions use the same {@link DirectionSegment}s as thunderstorm locations and
 * lightning remarks: a segment is a single point or a range.
 * <p>
 * Examples:
 * - "VCSH E SE"  → DirectionalWeather(VCSH, null, [E], [SE], false)
 * - "VCSH S-SW"  → DirectionalWeather(VCSH, null, [S-SW], false)
 * - "VCSH-NE"    → DirectionalWeather(VCSH, null, [NE], false)
 * - "SH DSNT N"  → DirectionalWeather(SH, "DSNT", [N], false)
 * - "SHRA OHD"   → DirectionalWeather(SHRA, "OHD", null, false)
 * - "-RA ALQDS"  → DirectionalWeather(-RA, null, null, true)
 * - "VCSH"       → DirectionalWeather(VCSH, null, null, false); VC is the location
 *
 * @param presentWeather    The present-weather phenomenon (reuses existing PresentWeather parsing)
 * @param location          Location qualifier (OHD, VC, DSNT, DSIPTD, TOP, TR), or null if none given
 * @param directionSegments One or more direction segments (points or ranges), or null if none given
 * @param allQuadrants      True if reported in all quadrants (ALQDS); mutually exclusive with directions
 * @author bclasky1539
 *
 */
public record DirectionalWeather(
        PresentWeather presentWeather,
        String location,
        List<DirectionSegment> directionSegments,
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
        directionSegments = directionSegments != null && !directionSegments.isEmpty()
                ? List.copyOf(directionSegments) : null;
        if (allQuadrants && directionSegments != null) {
            throw new IllegalArgumentException("Directions and all-quadrants are mutually exclusive");
        }
    }

    /**
     * Convenience constructor without a location qualifier.
     */
    public DirectionalWeather(PresentWeather presentWeather, List<DirectionSegment> directionSegments,
                              boolean allQuadrants) {
        this(presentWeather, null, directionSegments, allQuadrants);
    }

    /**
     * Convenience constructor for the common case: directions only (or none).
     */
    public DirectionalWeather(PresentWeather presentWeather, List<DirectionSegment> directionSegments) {
        this(presentWeather, null, directionSegments, false);
    }

    /**
     * @return true if one or more direction segments are recorded
     */
    public boolean hasDirections() {
        return directionSegments != null;
    }

    /**
     * @return true if a location qualifier (e.g. DSNT, OHD) is recorded
     */
    public boolean hasLocation() {
        return location != null;
    }

    /**
     * The directions as reported, segments joined with AND, as for thunderstorm locations
     * and lightning remarks: e.g. "E AND SE", "S-SW AND W".
     *
     * @return the directions summary, or an empty string if there are none
     */
    public String getDirectionsSummary() {
        return hasDirections()
                ? directionSegments.stream().map(DirectionSegment::getSummary).collect(Collectors.joining(" AND "))
                : "";
    }

    /**
     * Get a human-readable summary.
     * Examples: "Vicinity Showers: E AND SE", "Showers (DSNT): N", "Vicinity Showers: all quadrants"
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
        return hasDirections() ? base + ": " + getDirectionsSummary() : base;
    }
}
