package weather.model.components.remark.pressureremarks;

/**
 * Groups pressure-anomaly remarks: 3-hour pressure tendency and rapid
 * pressure change (rising/falling rapidly).
 * <p>
 * Each field is independently optional. Distinct from the station's
 * routine sea-level and altimeter pressure readings, which remain
 * direct fields on NoaaMetarRemarks.
 *
 * @param pressureTendency    3-hour pressure tendency, or null if not reported
 * @param pressureRapidChange rapid pressure change indicator, or null if not reported
 */
public record PressureRemarks(
        PressureTendency pressureTendency,
        PressureRapidChange pressureRapidChange
) {

    /**
     * @return an empty PressureRemarks with all fields null
     */
    public static PressureRemarks empty() {
        return new PressureRemarks(null, null);
    }

    /**
     * @return true if none of the pressure anomaly fields are present
     */
    public boolean isEmpty() {
        return pressureTendency == null && pressureRapidChange == null;
    }

    /**
     * Returns a copy of this record with the pressure tendency replaced.
     *
     * @param t the new 3-hour pressure tendency, or null to clear it
     * @return a new PressureRemarks with the given tendency and other fields unchanged
     */
    public PressureRemarks withPressureTendency(PressureTendency t) {
        return new PressureRemarks(t, pressureRapidChange);
    }

    /**
     * Returns a copy of this record with the rapid pressure change replaced.
     *
     * @param c the new rapid pressure change indicator, or null to clear it
     * @return a new PressureRemarks with the given rapid change and other fields unchanged
     */
    public PressureRemarks withPressureRapidChange(PressureRapidChange c) {
        return new PressureRemarks(pressureTendency, c);
    }
}
