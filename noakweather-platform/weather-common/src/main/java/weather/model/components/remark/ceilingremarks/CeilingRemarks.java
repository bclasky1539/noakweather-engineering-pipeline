package weather.model.components.remark.ceilingremarks;

/**
 * Groups ceiling-related remarks: variable ceiling and ceiling height at
 * a second observation site.
 * <p>
 * Each field is independently optional.
 *
 * @param variableCeiling   variable ceiling observation, or null if not reported
 * @param ceilingSecondSite ceiling height at a second observation site, or null if not reported
 */
public record CeilingRemarks(
        VariableCeiling variableCeiling,
        CeilingSecondSite ceilingSecondSite
) {

    /**
     * @return an empty CeilingRemarks with all fields null
     */
    public static CeilingRemarks empty() {
        return new CeilingRemarks(null, null);
    }

    /**
     * @return true if none of the ceiling remark fields are present
     */
    public boolean isEmpty() {
        return variableCeiling == null && ceilingSecondSite == null;
    }
}
