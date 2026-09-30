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

    @Test
    @DisplayName("withPressureTendency should replace tendency and preserve rapid change")
    void testWithPressureTendency() {
        PressureTendency tendency = PressureTendency.of(2, 3.2);
        PressureRemarks original = new PressureRemarks(null, PressureRapidChange.FALLING);

        PressureRemarks updated = original.withPressureTendency(tendency);

        assertThat(updated.pressureTendency()).isEqualTo(tendency);
        assertThat(updated.pressureRapidChange()).isEqualTo(PressureRapidChange.FALLING);
        assertThat(original.pressureTendency()).isNull();
    }

    @Test
    @DisplayName("withPressureRapidChange should replace rapid change and preserve tendency")
    void testWithPressureRapidChange() {
        PressureTendency tendency = PressureTendency.of(2, 3.2);
        PressureRemarks original = new PressureRemarks(tendency, null);

        PressureRemarks updated = original.withPressureRapidChange(PressureRapidChange.RISING);

        assertThat(updated.pressureRapidChange()).isEqualTo(PressureRapidChange.RISING);
        assertThat(updated.pressureTendency()).isEqualTo(tendency);
        assertThat(original.pressureRapidChange()).isNull();
    }
}
