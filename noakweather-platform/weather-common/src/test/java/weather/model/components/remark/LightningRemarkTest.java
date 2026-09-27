package weather.model.components.remark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for LightningRemark value object.
 *
 * @author bclasky1539
 *
 */
class LightningRemarkTest {

    // ==================== Construction Tests ====================

    @Test
    @DisplayName("Should construct with all fields")
    void testConstructor_AllFields() {
        DirectionSegment dir = new DirectionSegment(List.of("N"));
        LightningRemark remark = new LightningRemark(
                LightningFrequency.OCCASIONAL, List.of("IC"), "DSNT", dir, false
        );

        assertThat(remark.frequency()).isEqualTo(LightningFrequency.OCCASIONAL);
        assertThat(remark.types()).containsExactly("IC");
        assertThat(remark.location()).isEqualTo("DSNT");
        assertThat(remark.directionSegment()).isEqualTo(dir);
        assertThat(remark.allQuadrants()).isFalse();
    }

    @Test
    @DisplayName("Should construct with null frequency, types, location, and direction")
    void testConstructor_MinimalFields() {
        LightningRemark remark = new LightningRemark(null, null, null, null, false);

        assertThat(remark.frequency()).isNull();
        assertThat(remark.types()).isEmpty();
        assertThat(remark.location()).isNull();
        assertThat(remark.directionSegment()).isNull();
        assertThat(remark.allQuadrants()).isFalse();
    }

    @Test
    @DisplayName("Should default null types to an empty list")
    void testConstructor_NullTypesDefaultsToEmpty() {
        LightningRemark remark = new LightningRemark(null, null, null, null, false);

        assertThat(remark.types()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should construct with allQuadrants true and no direction segment")
    void testConstructor_AllQuadrants() {
        LightningRemark remark = new LightningRemark(
                LightningFrequency.CONTINUOUS, List.of("CW", "CA"), null, null, true
        );

        assertThat(remark.allQuadrants()).isTrue();
        assertThat(remark.directionSegment()).isNull();
    }

    @Test
    @DisplayName("Should defensively copy the types list")
    void testImmutability_DefensiveCopy() {
        List<String> mutable = new java.util.ArrayList<>(List.of("IC"));
        LightningRemark remark = new LightningRemark(null, mutable, null, null, false);

        mutable.add("CG");

        assertThat(remark.types()).containsExactly("IC");
    }

    // ==================== getSummary() Tests ====================

    @Test
    @DisplayName("Should summarize with frequency, type, location, and direction")
    void testGetSummary_Complete() {
        DirectionSegment dir = new DirectionSegment(List.of("SE", "S"));
        LightningRemark remark = new LightningRemark(
                LightningFrequency.FREQUENT, List.of("CC", "CG"), "VC", dir, false
        );

        assertThat(remark.getSummary())
                .contains("Frequent")
                .contains("cloud-to-cloud")
                .contains("cloud-to-ground")
                .contains("in vicinity")
                .contains("SE-S");
    }

    @Test
    @DisplayName("Should summarize with no frequency, no types, no location")
    void testGetSummary_Minimal() {
        DirectionSegment dir = new DirectionSegment(List.of("SW"));
        LightningRemark remark = new LightningRemark(null, null, null, dir, false);

        assertThat(remark.getSummary())
                .isEqualTo("lightning SW");
    }

    @Test
    @DisplayName("Should summarize all quadrants without a direction segment")
    void testGetSummary_AllQuadrants() {
        LightningRemark remark = new LightningRemark(
                LightningFrequency.CONTINUOUS, List.of("CW", "CA"), null, null, true
        );

        assertThat(remark.getSummary())
                .contains("Continuous")
                .contains("cloud-to-water")
                .contains("cloud-to-air")
                .contains("all quadrants");
    }

    @ParameterizedTest
    @CsvSource({
            "OHD, overhead",
            "VC, 'in vicinity'",
            "DSNT, distant"
    })
    @DisplayName("Should describe known location qualifiers")
    void testGetSummary_KnownLocations(String location, String expectedDescription) {
        LightningRemark remark = new LightningRemark(null, null, location, null, false);

        assertThat(remark.getSummary()).contains(expectedDescription);
    }

    @Test
    @DisplayName("Should describe unknown location qualifier as-is")
    void testGetSummary_UnknownLocation() {
        LightningRemark remark = new LightningRemark(null, null, "AT AP", null, false);

        assertThat(remark.getSummary()).contains("AT AP");
    }

    @Test
    @DisplayName("Should describe all five discharge types correctly")
    void testGetSummary_AllTypes() {
        LightningRemark remark = new LightningRemark(
                null, List.of("CG", "IC", "CC", "CA", "CW"), null, null, false
        );

        assertThat(remark.getSummary())
                .contains("cloud-to-ground")
                .contains("in-cloud")
                .contains("cloud-to-cloud")
                .contains("cloud-to-air")
                .contains("cloud-to-water");
    }

    // ==================== hasTypes() / hasLocation() / hasDirection() Tests ====================

    @Test
    @DisplayName("Should report hasTypes true when types present")
    void testHasTypes_True() {
        LightningRemark remark = new LightningRemark(null, List.of("IC"), null, null, false);

        assertThat(remark.hasTypes()).isTrue();
    }

    @Test
    @DisplayName("Should report hasTypes false when types absent")
    void testHasTypes_False() {
        LightningRemark remark = new LightningRemark(null, null, null, null, false);

        assertThat(remark.hasTypes()).isFalse();
    }

    @Test
    @DisplayName("Should report hasLocation true when location present")
    void testHasLocation_True() {
        LightningRemark remark = new LightningRemark(null, null, "OHD", null, false);

        assertThat(remark.hasLocation()).isTrue();
    }

    @Test
    @DisplayName("Should report hasLocation false when location absent")
    void testHasLocation_False() {
        LightningRemark remark = new LightningRemark(null, null, null, null, false);

        assertThat(remark.hasLocation()).isFalse();
    }

    @Test
    @DisplayName("Should report hasDirection true when directionSegment present")
    void testHasDirection_TrueWithSegment() {
        LightningRemark remark = new LightningRemark(
                null, null, null, new DirectionSegment(List.of("N")), false
        );

        assertThat(remark.hasDirection()).isTrue();
    }

    @Test
    @DisplayName("Should report hasDirection true when allQuadrants is true")
    void testHasDirection_TrueWithAllQuadrants() {
        LightningRemark remark = new LightningRemark(null, null, null, null, true);

        assertThat(remark.hasDirection()).isTrue();
    }

    @Test
    @DisplayName("Should report hasDirection false when neither present")
    void testHasDirection_False() {
        LightningRemark remark = new LightningRemark(null, null, null, null, false);

        assertThat(remark.hasDirection()).isFalse();
    }

    // ==================== Record Equality Tests ====================

    @Test
    @DisplayName("Should be equal when all fields match")
    void testEquality_SameFields() {
        DirectionSegment dir = new DirectionSegment(List.of("N"));
        LightningRemark remark1 = new LightningRemark(LightningFrequency.OCCASIONAL, List.of("IC"), "DSNT", dir, false);
        LightningRemark remark2 = new LightningRemark(LightningFrequency.OCCASIONAL, List.of("IC"), "DSNT", dir, false);

        assertThat(remark1).isEqualTo(remark2).hasSameHashCodeAs(remark2);
    }

    @Test
    @DisplayName("Should not be equal when types differ")
    void testInequality_DifferentTypes() {
        LightningRemark remark1 = new LightningRemark(null, List.of("IC"), null, null, false);
        LightningRemark remark2 = new LightningRemark(null, List.of("CG"), null, null, false);

        assertThat(remark1).isNotEqualTo(remark2);
    }

    @Test
    @DisplayName("Should not be equal when allQuadrants differs")
    void testInequality_DifferentAllQuadrants() {
        LightningRemark remark1 = new LightningRemark(null, null, null, null, true);
        LightningRemark remark2 = new LightningRemark(null, null, null, null, false);

        assertThat(remark1).isNotEqualTo(remark2);
    }
}
