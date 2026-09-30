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

    @Test
    @DisplayName("withVariableCeiling should replace variable ceiling and preserve second site")
    void testWithVariableCeiling() {
        CeilingSecondSite secondSite = CeilingSecondSite.fromHundreds(2, "RY11");
        VariableCeiling variable = VariableCeiling.fromHundreds(5, 10);
        CeilingRemarks original = new CeilingRemarks(null, secondSite);

        CeilingRemarks updated = original.withVariableCeiling(variable);

        assertThat(updated.variableCeiling()).isEqualTo(variable);
        assertThat(updated.ceilingSecondSite()).isEqualTo(secondSite);
        assertThat(original.variableCeiling()).isNull();
    }

    @Test
    @DisplayName("withCeilingSecondSite should replace second site and preserve variable ceiling")
    void testWithCeilingSecondSite() {
        VariableCeiling variable = VariableCeiling.fromHundreds(5, 10);
        CeilingSecondSite secondSite = CeilingSecondSite.fromHundreds(2, "RY11");
        CeilingRemarks original = new CeilingRemarks(variable, null);

        CeilingRemarks updated = original.withCeilingSecondSite(secondSite);

        assertThat(updated.ceilingSecondSite()).isEqualTo(secondSite);
        assertThat(updated.variableCeiling()).isEqualTo(variable);
        assertThat(original.ceilingSecondSite()).isNull();
    }
}
