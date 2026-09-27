package weather.model.components.remark.maintenanceremarks;

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
}
