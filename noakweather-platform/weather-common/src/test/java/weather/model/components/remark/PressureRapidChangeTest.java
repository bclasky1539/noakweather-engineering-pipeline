package weather.model.components.remark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PressureRapidChange}.
 */
class PressureRapidChangeTest {

    @ParameterizedTest
    @CsvSource({
            "R, RISING",
            "F, FALLING"
    })
    void ofParsesValidCodes(String code, PressureRapidChange expected) {
        assertThat(PressureRapidChange.of(code)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "R, RISING",
            "F, FALLING"
    })
    void fromCodeParsesValidCodes(String code, PressureRapidChange expected) {
        assertThat(PressureRapidChange.fromCode(code)).isEqualTo(expected);
    }

    @Test
    void ofRejectsInvalidCode() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PressureRapidChange.of("X"))
                .withMessageContaining("Invalid pressure rapid change code");
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void ofRejectsNullCode() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PressureRapidChange.of(null))
                .withMessageContaining("cannot be null");
    }

    @ParameterizedTest
    @CsvSource({
            "RISING, Rising rapidly",
            "FALLING, Falling rapidly"
    })
    void getSummaryReturnsHumanReadableText(PressureRapidChange value, String expectedSummary) {
        assertThat(value.getSummary()).isEqualTo(expectedSummary);
    }

    @Test
    void hasExactlyTwoValues() {
        assertThat(PressureRapidChange.values()).containsExactly(
                PressureRapidChange.RISING,
                PressureRapidChange.FALLING
        );
    }
}
