package weather.model.components.remark.pressureremarks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PressureRemarksTest {

    @Test
    @DisplayName("empty() should have all fields null")
    void testEmpty() {
        PressureRemarks remarks = PressureRemarks.empty();

        assertThat(remarks.pressureTendency()).isNull();
        assertThat(remarks.pressureRapidChange()).isNull();
        assertThat(remarks.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should be non-empty when only pressureRapidChange is present")
    void testIsEmpty_OnlyPressureRapidChange() {
        PressureRemarks remarks = new PressureRemarks(null, PressureRapidChange.of("F"));

        assertThat(remarks.isEmpty()).isFalse();
        assertThat(remarks.pressureRapidChange()).isEqualTo(PressureRapidChange.FALLING);
    }
}
