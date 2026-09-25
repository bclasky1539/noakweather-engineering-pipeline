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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents location information for thunderstorms and significant cloud types
 * in METAR remarks.
 * <p>
 * Examples:
 * - TS SE → Thunderstorm Southeast
 * - CB OHD MOV E → Cumulonimbus Overhead Moving East
 * - TCU DSNT N-NE → Towering Cumulus Distant North to Northeast
 * - ACC VC W → Altocumulus Castellanus in Vicinity West
 * - CB DSNT E-S-SW → Cumulonimbus Distant, arc from East through South to Southwest
 * - TCU N AND SW → Towering Cumulus reported both North and Southwest
 * - CB DSNT N-E AND SE-S → Cumulonimbus Distant, two arcs: North-East and Southeast-South
 * <p>
 * Cloud types:
 * - TS: Thunderstorm
 * - CB: Cumulonimbus
 * - TCU: Towering Cumulus
 * - ACC: Altocumulus Castellanus
 * - CBMAM: Cumulonimbus Mammatus
 * - VIRGA: Virga
 * <p>
 * Location qualifiers:
 * - OHD: Overhead
 * - VC: In vicinity (5-10 miles)
 * - DSNT: Distant (10-30 miles)
 * - DSIPTD: Dissipated
 * - TOP: At or above reference level
 * - TR: At all quadrants
 *
 * @param cloudType         Type of cloud or phenomenon (TS, CB, TCU, ACC, CBMAM, VIRGA)
 * @param locationQualifier Optional qualifier (OHD, VC, DSNT, DSIPTD, TOP, TR)
 * @param directionSegments Directions reported for this location. One or more
 *                          segments, each a single compass point or a multi-point arc/range;
 *                          multiple segments mean the same cloud type was reported at more
 *                          than one place (AND-chained), e.g. "N-E AND SE-S". Empty if no
 *                          direction was reported.
 * @param movingDirection   Direction of movement (if MOV present)
 * @author bclasky1539
 *
 */
public record ThunderstormLocation(
        String cloudType,
        String locationQualifier,
        List<DirectionSegment> directionSegments,
        String movingDirection
) {
    public ThunderstormLocation {
        directionSegments = directionSegments == null
                ? List.of()
                : List.copyOf(directionSegments);
    }

    /**
     * Create a simple thunderstorm location with just type and a single direction.
     *
     * @param cloudType the cloud type (TS, CB, etc.)
     * @param direction the direction (N, SE, etc.)
     * @return new ThunderstormLocation
     */
    public static ThunderstormLocation of(String cloudType, String direction) {
        return new ThunderstormLocation(
                cloudType, null,
                List.of(new DirectionSegment(List.of(direction))),
                null
        );
    }

    /**
     * Create a thunderstorm location with a single direction and movement.
     *
     * @param cloudType       the cloud type
     * @param direction       the direction
     * @param movingDirection the direction of movement
     * @return new ThunderstormLocation
     */
    public static ThunderstormLocation withMovement(String cloudType, String direction, String movingDirection) {
        return new ThunderstormLocation(
                cloudType, null,
                List.of(new DirectionSegment(List.of(direction))),
                movingDirection
        );
    }

    /**
     * Get a human-readable summary of this location.
     *
     * @return summary string (e.g., "Thunderstorm in vicinity SE Moving E")
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();

        sb.append(getCloudTypeDescription());

        if (locationQualifier != null) {
            sb.append(" ").append(getLocationQualifierDescription());
        }

        if (hasDirections()) {
            sb.append(" ").append(getDirectionsSummary());
        }

        if (movingDirection != null) {
            sb.append(" Moving ").append(movingDirection);
        }

        return sb.toString();
    }

    /**
     * Get description of cloud type.
     */
    private String getCloudTypeDescription() {
        return switch (cloudType) {
            case "TS" -> "Thunderstorm";
            case "CB" -> "Cumulonimbus";
            case "TCU" -> "Towering Cumulus";
            case "ACC" -> "Altocumulus Castellanus";
            case "CBMAM" -> "Cumulonimbus Mammatus";
            case "VIRGA" -> "Virga";
            default -> cloudType;
        };
    }

    /**
     * Get description of location qualifier.
     */
    private String getLocationQualifierDescription() {
        return switch (locationQualifier) {
            case "OHD" -> "Overhead";
            case "VC" -> "In vicinity";
            case "DSNT" -> "Distant";
            case "DSIPTD" -> "Dissipated";
            case "TOP" -> "At or above level";
            case "TR" -> "At all quadrants";
            default -> locationQualifier;
        };
    }

    /**
     * Check if this location has movement information.
     *
     * @return true if MOV direction is present
     */
    public boolean isMoving() {
        return movingDirection != null;
    }

    /**
     * Check if this is a thunderstorm (TS).
     *
     * @return true if cloud type is TS
     */
    public boolean isThunderstorm() {
        return "TS".equals(cloudType);
    }

    /**
     * Check if this has a location qualifier.
     *
     * @return true if locationQualifier is present
     */
    public boolean hasLocationQualifier() {
        return locationQualifier != null;
    }

    /**
     * Check if any direction information is present.
     *
     * @return true if at least one direction segment is present
     */
    public boolean hasDirections() {
        return !directionSegments.isEmpty();
    }

    /**
     * Get the AND-joined, human-readable directions, e.g. "N-E AND SE-S".
     *
     * @return the directions summary, or empty string if none
     */
    public String getDirectionsSummary() {
        return directionSegments.stream()
                .map(DirectionSegment::getSummary)
                .collect(Collectors.joining(" AND "));
    }
}
