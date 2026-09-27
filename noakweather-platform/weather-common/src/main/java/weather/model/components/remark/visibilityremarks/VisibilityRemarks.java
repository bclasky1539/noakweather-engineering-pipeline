package weather.model.components.remark.visibilityremarks;

import weather.model.components.Visibility;

/**
 * Groups visibility-related remarks: tower visibility, surface visibility,
 * and variable visibility.
 * <p>
 * Each field is independently optional.
 *
 * @param towerVisibility   tower visibility from the TWR VIS group, or null if not reported
 * @param surfaceVisibility surface visibility from the SFC VIS group, or null if not reported
 * @param variableVisibility variable visibility from the VIS group, or null if not reported
 */
public record VisibilityRemarks(
        Visibility towerVisibility,
        Visibility surfaceVisibility,
        VariableVisibility variableVisibility
) {

    /**
     * @return an empty VisibilityRemarks with all fields null
     */
    public static VisibilityRemarks empty() {
        return new VisibilityRemarks(null, null, null);
    }

    /**
     * @return true if none of the visibility remark fields are present
     */
    public boolean isEmpty() {
        return towerVisibility == null && surfaceVisibility == null && variableVisibility == null;
    }
}
