package weather.model.components.remark.ceilingremarks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CeilingRemarksTest {

    @Test
    @DisplayName("empty() should have all fields null")
    void testEmpty() {
        CeilingRemarks remarks = CeilingRemarks.empty();

        assertThat(remarks.variableCeiling()).isNull();
        assertThat(remarks.ceilingSecondSite()).isNull();
        assertThat(remarks.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should be non-empty when only variableCeiling is present")
    void testIsEmpty_OnlyVariableCeiling() {
        VariableCeiling ceiling = VariableCeiling.fromHundreds(5, 10);
        CeilingRemarks remarks = new CeilingRemarks(ceiling, null);

        assertThat(remarks.isEmpty()).isFalse();
        assertThat(remarks.variableCeiling()).isEqualTo(ceiling);
    }
}
