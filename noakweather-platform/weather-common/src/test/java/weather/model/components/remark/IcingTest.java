package weather.model.components.remark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Icing}.
 */
class IcingTest {

    @Test
    @DisplayName("Should construct icing with all fields set")
    void testConstructor_AllFieldsSet() {
        Icing icing = new Icing(true, true, "PAST HR");

        assertThat(icing.inClouds()).isTrue();
        assertThat(icing.inPrecipitation()).isTrue();
        assertThat(icing.qualifier()).isEqualTo("PAST HR");
    }

    @Test
    @DisplayName("Should construct icing with only inClouds true")
    void testConstructor_OnlyInClouds() {
        Icing icing = new Icing(true, false, null);

        assertThat(icing.inClouds()).isTrue();
        assertThat(icing.inPrecipitation()).isFalse();
        assertThat(icing.qualifier()).isNull();
    }

    @Test
    @DisplayName("Should construct icing with only inPrecipitation true")
    void testConstructor_OnlyInPrecipitation() {
        Icing icing = new Icing(false, true, null);

        assertThat(icing.inClouds()).isFalse();
        assertThat(icing.inPrecipitation()).isTrue();
        assertThat(icing.qualifier()).isNull();
    }

    @Test
    @DisplayName("Should construct icing with neither type set (bare ICG)")
    void testConstructor_NeitherTypeSet() {
        Icing icing = new Icing(false, false, "PAST HR");

        assertThat(icing.inClouds()).isFalse();
        assertThat(icing.inPrecipitation()).isFalse();
        assertThat(icing.qualifier()).isEqualTo("PAST HR");
    }

    // ========== getSummary() TESTS ==========

    @Test
    @DisplayName("Should summarize icing in clouds with qualifier")
    void testGetSummary_InCloudsWithQualifier() {
        Icing icing = new Icing(true, false, "PAST HR");

        assertThat(icing.getSummary()).isEqualTo("Icing in clouds (PAST HR)");
    }

    @Test
    @DisplayName("Should summarize icing in precipitation with qualifier")
    void testGetSummary_InPrecipitationWithQualifier() {
        Icing icing = new Icing(false, true, "PAST HR");

        assertThat(icing.getSummary()).isEqualTo("Icing in precipitation (PAST HR)");
    }

    @Test
    @DisplayName("Should summarize icing in clouds and precipitation with qualifier")
    void testGetSummary_InCloudsAndPrecipitationWithQualifier() {
        Icing icing = new Icing(true, true, "PAST HR");

        assertThat(icing.getSummary()).isEqualTo("Icing in clouds and precipitation (PAST HR)");
    }

    @Test
    @DisplayName("Should summarize bare icing with qualifier only")
    void testGetSummary_BareIcingWithQualifier() {
        Icing icing = new Icing(false, false, "PAST HR");

        assertThat(icing.getSummary()).isEqualTo("Icing (PAST HR)");
    }

    @Test
    @DisplayName("Should summarize icing in clouds without qualifier")
    void testGetSummary_InCloudsNoQualifier() {
        Icing icing = new Icing(true, false, null);

        assertThat(icing.getSummary()).isEqualTo("Icing in clouds");
    }

    @Test
    @DisplayName("Should summarize bare icing without qualifier")
    void testGetSummary_BareIcingNoQualifier() {
        Icing icing = new Icing(false, false, null);

        assertThat(icing.getSummary()).isEqualTo("Icing");
    }

    @ParameterizedTest
    @CsvSource({
            "true, false, '', Icing in clouds",
            "false, true, '', Icing in precipitation",
            "true, true, '', Icing in clouds and precipitation",
            "false, false, '', Icing"
    })
    @DisplayName("Should summarize icing correctly with blank qualifier treated as absent")
    void testGetSummary_BlankQualifierTreatedAsAbsent(boolean inClouds, boolean inPrecipitation,
                                                      String qualifier, String expectedSummary) {
        Icing icing = new Icing(inClouds, inPrecipitation, qualifier);

        assertThat(icing.getSummary()).isEqualTo(expectedSummary);
    }

    // ========== hasQualifier() TESTS ==========

    @Test
    @DisplayName("Should return true when qualifier is present")
    void testHasQualifier_True() {
        Icing icing = new Icing(true, false, "PAST HR");

        assertThat(icing.hasQualifier()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("Should return false when qualifier is blank or empty")
    void testHasQualifier_BlankOrEmptyFalse(String qualifier) {
        Icing icing = new Icing(true, false, qualifier);

        assertThat(icing.hasQualifier()).isFalse();
    }

    @Test
    @DisplayName("Should return false when qualifier is null")
    void testHasQualifier_NullFalse() {
        Icing icing = new Icing(true, false, null);

        assertThat(icing.hasQualifier()).isFalse();
    }

    // ========== EQUALITY TESTS (record-generated equals/hashCode) ==========

    @Test
    @DisplayName("Should be equal when all fields match")
    void testEquality_SameFields() {
        Icing icing1 = new Icing(true, false, "PAST HR");
        Icing icing2 = new Icing(true, false, "PAST HR");

        assertThat(icing1).isEqualTo(icing2).hasSameHashCodeAs(icing2);
    }

    @Test
    @DisplayName("Should not be equal when inClouds differs")
    void testInequality_DifferentInClouds() {
        Icing icing1 = new Icing(true, false, "PAST HR");
        Icing icing2 = new Icing(false, false, "PAST HR");

        assertThat(icing1).isNotEqualTo(icing2);
    }

    @Test
    @DisplayName("Should not be equal when qualifier differs")
    void testInequality_DifferentQualifier() {
        Icing icing1 = new Icing(true, false, "PAST HR");
        Icing icing2 = new Icing(true, false, "PAST 2HR");

        assertThat(icing1).isNotEqualTo(icing2);
    }

    @Test
    @DisplayName("Should construct icing via of() factory method matching canonical constructor")
    void testOf_MatchesConstructor() {
        Icing viaOf = Icing.of(true, false, "PAST HR");
        Icing viaConstructor = new Icing(true, false, "PAST HR");

        assertThat(viaOf).isEqualTo(viaConstructor);
        assertThat(viaOf.inClouds()).isTrue();
        assertThat(viaOf.inPrecipitation()).isFalse();
        assertThat(viaOf.qualifier()).isEqualTo("PAST HR");
    }
}
