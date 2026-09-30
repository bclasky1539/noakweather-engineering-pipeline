package weather.model.components.remark.maintenanceremarks;

import java.util.ArrayList;
import java.util.List;

/**
 * Groups automated-station maintenance remarks: individual maintenance
 * indicators (RVRNO, PWINO, etc.) and the overall maintenance-required
 * flag ($ indicator).
 *
 * @param automatedMaintenanceIndicators list of individual maintenance
 *                                       indicators; empty if none reported
 * @param maintenanceRequired            true if the $ indicator was present;
 *                                       null if not explicitly set
 */
public record MaintenanceRemarks(
        List<AutomatedMaintenanceIndicator> automatedMaintenanceIndicators,
        Boolean maintenanceRequired
) {

    public MaintenanceRemarks {
        automatedMaintenanceIndicators = automatedMaintenanceIndicators == null
                ? List.of() : List.copyOf(automatedMaintenanceIndicators);
    }

    /**
     * @return an empty MaintenanceRemarks with no indicators and maintenanceRequired null
     */
    public static MaintenanceRemarks empty() {
        return new MaintenanceRemarks(List.of(), null);
    }

    /**
     * @return true if no maintenance indicators are present and
     * maintenanceRequired is not set
     */
    public boolean isEmpty() {
        return automatedMaintenanceIndicators.isEmpty() && maintenanceRequired == null;
    }

    /**
     * Returns whether maintenance is required. Returns false if not
     * explicitly set to true, matching the null-safe convention used
     * elsewhere in the remarks model.
     *
     * @return true if maintenance is required, false otherwise
     */
    public Boolean maintenanceRequired() {
        return maintenanceRequired != null && maintenanceRequired;
    }

    /**
     * Returns a copy of this record with the entire indicator list replaced.
     *
     * @param l the new list of maintenance indicators; null is treated as empty
     * @return a new MaintenanceRemarks with the given list and maintenanceRequired unchanged
     */
    public MaintenanceRemarks withAutomatedMaintenanceIndicators(List<AutomatedMaintenanceIndicator> l) {
        return new MaintenanceRemarks(l, maintenanceRequired);
    }

    /**
     * Returns a copy of this record with one maintenance indicator appended.
     *
     * @param i the indicator to append; if null, no change is made
     * @return a new MaintenanceRemarks with the indicator appended, or this same instance if 'i' is null
     */
    public MaintenanceRemarks addAutomatedMaintenanceIndicator(AutomatedMaintenanceIndicator i) {
        if (i == null) return this;
        List<AutomatedMaintenanceIndicator> updated = new ArrayList<>(automatedMaintenanceIndicators);
        updated.add(i);
        return new MaintenanceRemarks(updated, maintenanceRequired);
    }

    /**
     * Returns a copy of this record with the maintenance-required flag replaced.
     *
     * @param required the new flag value; null means "not explicitly set"
     * @return a new MaintenanceRemarks with the given flag and the indicator list unchanged
     */
    public MaintenanceRemarks withMaintenanceRequired(Boolean required) {
        return new MaintenanceRemarks(automatedMaintenanceIndicators, required);
    }

    /**
     * Returns whether the maintenance-required flag was explicitly set
     * (as opposed to defaulting to false via {@link #maintenanceRequired()}).
     *
     * @return true if the raw maintenanceRequired value is non-null
     */
    public boolean hasMaintenanceRequired() {
        return maintenanceRequired != null;
    }
}
