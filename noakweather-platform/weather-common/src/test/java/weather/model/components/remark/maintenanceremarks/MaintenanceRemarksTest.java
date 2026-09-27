package weather.model.components.remark.maintenanceremarks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceRemarksTest {

    @Test
    @DisplayName("empty() should have no indicators and maintenanceRequired null")
    void testEmpty() {
        MaintenanceRemarks remarks = MaintenanceRemarks.empty();

        assertThat(remarks.automatedMaintenanceIndicators()).isEmpty();
        assertThat(remarks.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("maintenanceRequired() should return false when null")
    void testMaintenanceRequired_Null() {
        MaintenanceRemarks remarks = new MaintenanceRemarks(null, null);

        assertThat(remarks.maintenanceRequired()).isFalse();
    }

    @Test
    @DisplayName("maintenanceRequired() should return true when explicitly set")
    void testMaintenanceRequired_True() {
        MaintenanceRemarks remarks = new MaintenanceRemarks(null, true);

        assertThat(remarks.maintenanceRequired()).isTrue();
        assertThat(remarks.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should defensively copy indicators list")
    void testDefensiveCopy() {
        java.util.List<AutomatedMaintenanceIndicator> mutable = new java.util.ArrayList<>();
        mutable.add(AutomatedMaintenanceIndicator.of("TSNO"));

        MaintenanceRemarks remarks = new MaintenanceRemarks(mutable, null);
        mutable.add(AutomatedMaintenanceIndicator.of("PWINO"));

        assertThat(remarks.automatedMaintenanceIndicators()).hasSize(1);
    }
}
