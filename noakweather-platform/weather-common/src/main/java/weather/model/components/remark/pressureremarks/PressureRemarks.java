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
}
