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

    @Test
    @DisplayName("withAutomatedMaintenanceIndicators should replace the list; null becomes empty")
    void testWithAutomatedMaintenanceIndicators() {
        AutomatedMaintenanceIndicator tsno = AutomatedMaintenanceIndicator.of("TSNO");
        AutomatedMaintenanceIndicator pwino = AutomatedMaintenanceIndicator.of("PWINO");
        MaintenanceRemarks original = new MaintenanceRemarks(java.util.List.of(tsno), true);

        MaintenanceRemarks updated = original.withAutomatedMaintenanceIndicators(java.util.List.of(pwino));

        assertThat(updated.automatedMaintenanceIndicators()).containsExactly(pwino);
        assertThat(updated.maintenanceRequired()).isTrue();
        assertThat(original.withAutomatedMaintenanceIndicators(null).automatedMaintenanceIndicators())
                .isEmpty();
    }

    @Test
    @DisplayName("addAutomatedMaintenanceIndicator should append without mutating the original")
    void testAddAutomatedMaintenanceIndicator() {
        AutomatedMaintenanceIndicator tsno = AutomatedMaintenanceIndicator.of("TSNO");
        AutomatedMaintenanceIndicator pwino = AutomatedMaintenanceIndicator.of("PWINO");
        MaintenanceRemarks original = new MaintenanceRemarks(java.util.List.of(tsno), null);

        MaintenanceRemarks updated = original.addAutomatedMaintenanceIndicator(pwino);

        assertThat(updated.automatedMaintenanceIndicators()).containsExactly(tsno, pwino);
        assertThat(original.automatedMaintenanceIndicators()).containsExactly(tsno);
    }

    @Test
    @DisplayName("addAutomatedMaintenanceIndicator(null) should return the same instance")
    void testAddAutomatedMaintenanceIndicator_Null() {
        MaintenanceRemarks original = new MaintenanceRemarks(null, null);

        assertThat(original.addAutomatedMaintenanceIndicator(null)).isSameAs(original);
    }

    @Test
    @DisplayName("withMaintenanceRequired should replace the flag and preserve indicators")
    void testWithMaintenanceRequired() {
        AutomatedMaintenanceIndicator tsno = AutomatedMaintenanceIndicator.of("TSNO");
        MaintenanceRemarks original = new MaintenanceRemarks(java.util.List.of(tsno), null);

        MaintenanceRemarks updated = original.withMaintenanceRequired(true);

        assertThat(updated.maintenanceRequired()).isTrue();
        assertThat(updated.hasMaintenanceRequired()).isTrue();
        assertThat(updated.automatedMaintenanceIndicators()).containsExactly(tsno);
        assertThat(original.hasMaintenanceRequired()).isFalse();
        assertThat(updated.withMaintenanceRequired(null).hasMaintenanceRequired()).isFalse();
    }

    @Test
    @DisplayName("hasMaintenanceRequired() should distinguish unset from explicitly false")
    void testHasMaintenanceRequired() {
        assertThat(new MaintenanceRemarks(null, null).hasMaintenanceRequired()).isFalse();
        assertThat(new MaintenanceRemarks(null, false).hasMaintenanceRequired()).isTrue();
        assertThat(new MaintenanceRemarks(null, true).hasMaintenanceRequired()).isTrue();
    }
}
