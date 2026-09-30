package weather.model.components.remark.windremarks;

import java.util.ArrayList;
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

    /**
     * Returns a copy of this record with the peak wind replaced.
     *
     * @param peakWind the new peak wind, or null to clear it
     * @return a new WindRemarks with the given peak wind and this record's other fields unchanged
     */
    public WindRemarks withPeakWind(PeakWind peakWind) {
        return new WindRemarks(peakWind, windShift, windsAtLocation);
    }

    /**
     * Returns a copy of this record with the wind shift replaced.
     *
     * @param windShift the new wind shift, or null to clear it
     * @return a new WindRemarks with the given wind shift and this record's other fields unchanged
     */
    public WindRemarks withWindShift(WindShift windShift) {
        return new WindRemarks(peakWind, windShift, windsAtLocation);
    }

    /**
     * Returns a copy of this record with the entire winds-at-location list replaced.
     *
     * @param winds the new list of winds at location; null is treated as empty
     * @return a new WindRemarks with the given list and this record's other fields unchanged
     */
    public WindRemarks withWindsAtLocation(List<WindAtLocation> winds) {
        return new WindRemarks(peakWind, windShift, winds);
    }

    /**
     * Returns a copy of this record with one wind-at-location entry appended.
     *
     * @param wind the entry to append; if null, no change is made
     * @return a new WindRemarks with the entry appended, or this same instance if wind is null
     */
    public WindRemarks addWindAtLocation(WindAtLocation wind) {
        if (wind == null) return this;
        List<WindAtLocation> updated = new ArrayList<>(windsAtLocation);
        updated.add(wind);
        return new WindRemarks(peakWind, windShift, updated);
    }
}
