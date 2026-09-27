package weather.model.components.remark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for LightningFrequency enum.
 *
 * @author bclasky1539
 *
 */
class LightningFrequencyTest {

    @ParameterizedTest
    @CsvSource({
            "OCNL, OCCASIONAL",
            "FRQ, FREQUENT",
            "CONS, CONTINUOUS",
            "CONTUS, CONTINUOUS"
    })
    @DisplayName("Should parse all valid frequency codes, normalizing CONS/CONTUS to the same value")
    void testFromCode_ValidCodes(String code, LightningFrequency expected) {
        assertThat(LightningFrequency.fromCode(code)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should throw for null code")
    void testFromCode_Null() {
        assertThatThrownBy(() -> LightningFrequency.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Should throw for invalid code")
    void testFromCode_Invalid() {
        assertThatThrownBy(() -> LightningFrequency.fromCode("XXX"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid lightning frequency code");
    }

    @ParameterizedTest
    @CsvSource({
            "OCCASIONAL, Occasional",
            "FREQUENT, Frequent",
            "CONTINUOUS, Continuous"
    })
    @DisplayName("Should generate correct description for each value")
    void testGetDescription(LightningFrequency frequency, String expectedDescription) {
        assertThat(frequency.getDescription()).isEqualTo(expectedDescription);
    }

    @Test
    @DisplayName("CONS and CONTUS should both normalize to CONTINUOUS")
    void testConsAndContusNormalizeToSameValue() {
        assertThat(LightningFrequency.fromCode("CONS"))
                .isEqualTo(LightningFrequency.fromCode("CONTUS"));
    }
}
