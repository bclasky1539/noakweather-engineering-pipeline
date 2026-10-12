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
 * Lightning remark (LTG). Reports observed lightning by frequency, one or
 * more discharge types, and location/direction.
 * <p>
 * Format: [freq] LTG[types] [loc] [dir[-dir...] [AND dir[-dir...]]...]
 * Examples:
 * - OCNL LTGIC DSNT N → occasional in-cloud lightning, distant, north
 * - FRQ LTGCCCG VC W → frequent cloud-to-cloud and cloud-to-ground
 * lightning, in vicinity, west
 * - LTGIC SW → in-cloud lightning, southwest (no frequency, no location)
 * - LTG DSNT S AND SW → lightning, distant, reported both south and southwest
 * - OCNL LTGIC DSNT SE AND S-SW → occasional in-cloud lightning, distant,
 * southeast and the south-southwest range
 * <p>
 * An AND chain describes one lightning observation seen in more than one
 * sector, so it is a single remark with several direction segments (the same
 * model as {@link ThunderstormLocation#directionSegments()}).
 *
 * @param frequency         how often lightning is occurring, or null if not reported
 * @param types             one or more discharge types (CG, IC, CC, CA, CW), in the
 *                          order reported; empty if no types were captured
 * @param location          location qualifier (OHD, VC, DSNT, AT AP), or null if none
 * @param directionSegments directions reported for this remark, in the order
 *                          reported; each a single compass point or a range
 *                          (e.g. SE-S). Multiple segments come from an AND chain.
 *                          Empty if no direction was reported or allQuadrants is true
 * @param allQuadrants      true if ALQDS (all quadrants) was reported instead of a
 *                          specific compass direction
 */
public record LightningRemark(
        LightningFrequency frequency,
        List<String> types,
        String location,
        List<DirectionSegment> directionSegments,
        boolean allQuadrants
) {

    public LightningRemark {
        types = types == null ? List.of() : List.copyOf(types);
        directionSegments = directionSegments == null ? List.of() : List.copyOf(directionSegments);
    }

    /**
     * @return a human-readable summary, e.g. "Frequent lightning
     * (cloud-to-cloud, cloud-to-ground) in vicinity W", or
     * "Lightning distant S AND SW" for an AND chain
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();

        if (frequency != null) {
            sb.append(frequency.getDescription()).append(" ");
        }

        sb.append("lightning");

        if (!types.isEmpty()) {
            sb.append(" (")
                    .append(String.join(", ", types.stream().map(LightningRemark::describeType).toList()))
                    .append(")");
        }

        if (location != null) {
            sb.append(" ").append(describeLocation(location));
        }

        if (allQuadrants) {
            sb.append(" all quadrants");
        } else if (!directionSegments.isEmpty()) {
            sb.append(" ").append(getDirectionsSummary());
        }

        return sb.toString();
    }

    /**
     * @return the AND-joined directions, e.g. "SE AND S-SW", or empty string if none
     */
    public String getDirectionsSummary() {
        return directionSegments.stream()
                .map(DirectionSegment::getSummary)
                .collect(Collectors.joining(" AND "));
    }

    private static String describeType(String type) {
        return switch (type) {
            case "CG" -> "cloud-to-ground";
            case "IC" -> "in-cloud";
            case "CC" -> "cloud-to-cloud";
            case "CA" -> "cloud-to-air";
            case "CW" -> "cloud-to-water";
            default -> type;
        };
    }

    private static String describeLocation(String loc) {
        return switch (loc) {
            case "OHD" -> "overhead";
            case "VC" -> "in vicinity";
            case "DSNT" -> "distant";
            default -> loc;
        };
    }

    /**
     * @return true if any discharge types were reported
     */
    public boolean hasTypes() {
        return !types.isEmpty();
    }

    /**
     * @return true if a location qualifier is present
     */
    public boolean hasLocation() {
        return location != null;
    }

    /**
     * @return true if any direction information is present (one or more
     * compass directions/ranges, or all quadrants)
     */
    public boolean hasDirection() {
        return allQuadrants || !directionSegments.isEmpty();
    }
}
