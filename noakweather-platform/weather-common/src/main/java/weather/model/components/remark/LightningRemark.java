package weather.model.components.remark;

import java.util.List;

/**
 * Lightning remark (LTG). Reports observed lightning by frequency, one or
 * more discharge types, and location/direction.
 * <p>
 * Format: [freq] LTG[types] [loc] [dir[-dir2]]
 * Examples:
 * - OCNL LTGIC DSNT N → occasional in-cloud lightning, distant, north
 * - FRQ LTGCCCG VC W → frequent cloud-to-cloud and cloud-to-ground
 * lightning, in vicinity, west
 * - LTGIC SW → in-cloud lightning, southwest (no frequency, no location)
 *
 * @param frequency        how often lightning is occurring, or null if not reported
 * @param types            one or more discharge types (CG, IC, CC, CA, CW), in the
 *                         order reported; empty if no types were captured
 * @param location         location qualifier (OHD, VC, DSNT), or null if none
 * @param directionSegment compass direction, possibly a range (e.g. SE-S); null if
 *                         no direction was reported or allQuadrants is true
 * @param allQuadrants     true if ALQDS (all quadrants) was reported instead of a
 *                         specific compass direction
 */
public record LightningRemark(
        LightningFrequency frequency,
        List<String> types,
        String location,
        DirectionSegment directionSegment,
        boolean allQuadrants
) {

    public LightningRemark {
        types = types == null ? List.of() : List.copyOf(types);
    }

    /**
     * @return a human-readable summary, e.g. "Frequent lightning
     * (cloud-to-cloud, cloud-to-ground) in vicinity W"
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
        } else if (directionSegment != null) {
            sb.append(" ").append(directionSegment.getSummary());
        }

        return sb.toString();
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
     * @return true if any direction information is present (a specific
     * compass direction/range, or all quadrants)
     */
    public boolean hasDirection() {
        return allQuadrants || directionSegment != null;
    }
}
