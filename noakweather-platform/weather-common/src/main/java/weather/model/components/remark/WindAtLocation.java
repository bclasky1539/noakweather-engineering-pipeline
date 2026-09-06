package weather.model.components.remark;

import weather.model.components.Wind;

/**
 * Immutable value object representing wind conditions reported in remarks
 * at a specific location — either a specific altitude or a specific runway —
 * distinct from surface wind reported in the main body.
 * <p>
 * Common in Norwegian/Arctic-region METARs where surface conditions
 * (e.g. calm, or a runway-specific reading) differ meaningfully from
 * conditions at altitude or at a specific runway.
 * <p>
 * Examples:
 * - "WIND 1400FT 23010KT" → WindAtLocation(1400, null, Wind.of(230, 10, "KT"))
 * - "WIND RWY 26 00000KT" → WindAtLocation(null, "26", Wind.of(0, 0, "KT"))
 *
 * @param heightFeet Altitude in feet, or null if this is a runway-specific reading
 * @param runway     Runway designator (e.g. "26"), or null if this is an altitude reading
 * @param wind       Wind conditions at that location
 * @author bclasky1539
 *
 */
public record WindAtLocation(
        Integer heightFeet,
        String runway,
        Wind wind
) {

    /**
     * Compact constructor with validation.
     */
    public WindAtLocation {
        if (heightFeet == null && (runway == null || runway.isBlank())) {
            throw new IllegalArgumentException("Either heightFeet or runway must be provided");
        }
        if (heightFeet != null && runway != null) {
            throw new IllegalArgumentException("Cannot specify both heightFeet and runway");
        }
        if (wind == null) {
            throw new IllegalArgumentException("Wind cannot be null");
        }
    }

    /**
     * Check if this reading is at a specific altitude rather than a runway.
     *
     * @return true if this is an altitude-based reading
     */
    public boolean isAtAltitude() {
        return heightFeet != null;
    }

    /**
     * Check if this reading is at a specific runway rather than an altitude.
     *
     * @return true if this is a runway-based reading
     */
    public boolean isAtRunway() {
        return runway != null;
    }

    /**
     * Get a human-readable summary.
     * Examples: "Wind at 1400ft: 230° at 10 KT", "Wind at RWY 26: CALM"
     *
     * @return formatted summary string
     */
    public String getSummary() {
        String location = isAtAltitude() ? heightFeet + "ft" : "RWY " + runway;
        return "Wind at " + location + ": " + wind.getSummary();
    }

    // ==================== Factory Methods ====================

    /**
     * Factory method for a wind reading at a specific altitude.
     *
     * @param heightFeet altitude in feet
     * @param wind wind conditions at that altitude
     * @return WindAtLocation instance representing an altitude-based reading
     */
    public static WindAtLocation atAltitude(int heightFeet, Wind wind) {
        return new WindAtLocation(heightFeet, null, wind);
    }

    /**
     * Factory method for a wind reading at a specific runway.
     *
     * @param runway runway designator (e.g. "26")
     * @param wind wind conditions at that runway
     * @return WindAtLocation instance representing a runway-based reading
     */
    public static WindAtLocation atRunway(String runway, Wind wind) {
        return new WindAtLocation(null, runway, wind);
    }
}
