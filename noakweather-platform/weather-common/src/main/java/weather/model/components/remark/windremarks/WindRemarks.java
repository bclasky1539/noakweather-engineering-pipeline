package weather.model.components.remark.windremarks;

import java.util.List;

/**
 * Groups wind-related remarks: peak wind, wind shift, and winds reported
 * at a specific altitude or runway location.
 * <p>
 * Each field is independently optional, matching the standalone optionality
 * these remarks had before being grouped into this record — a METAR may
 * report a wind shift with no peak wind, or vice versa.
 *
 * @param peakWind        peak wind data from the PK WND group, or null if not reported
 * @param windShift       wind shift data from the WSHFT group, or null if not reported
 * @param windsAtLocation winds reported at a specific altitude/runway location; empty if none
 */
public record WindRemarks(
        PeakWind peakWind,
        WindShift windShift,
        List<WindAtLocation> windsAtLocation
) {

    public WindRemarks {
        windsAtLocation = windsAtLocation == null ? List.of() : List.copyOf(windsAtLocation);
    }

    /**
     * @return an empty WindRemarks with all fields null/empty
     */
    public static WindRemarks empty() {
        return new WindRemarks(null, null, List.of());
    }

    /**
     * @return true if none of the wind remark fields are present
     */
    public boolean isEmpty() {
        return peakWind == null && windShift == null && windsAtLocation.isEmpty();
    }
}
