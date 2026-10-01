package weather.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LowCloudTypeTest {

    @Nested
    @DisplayName("fromCode - valid digits")
    class FromCodeValidDigits {

        @ParameterizedTest
        @CsvSource({
                "0, NONE",
                "1, CUMULUS_HUMILIS_OR_FRACTUS_FAIR_WEATHER",
                "2, CUMULUS_MEDIOCRIS_OR_CONGESTUS",
                "3, CUMULONIMBUS_CALVUS",
                "4, STRATOCUMULUS_CUMULOGENITUS",
                "5, STRATOCUMULUS_NON_CUMULOGENITUS",
                "6, STRATUS_NEBULOSUS_OR_FRACTUS_FAIR_WEATHER",
                "7, STRATUS_FRACTUS_OR_CUMULUS_FRACTUS_BAD_WEATHER",
                "8, CUMULUS_AND_STRATOCUMULUS_DIFFERENT_LEVELS",
                "9, CUMULONIMBUS_CAPILLATUS"
        })
        void shouldParseEachDigitToExpectedConstant(String code, String expectedName) {
            LowCloudType result = LowCloudType.fromCode(code);
            assertThat(result.name()).isEqualTo(expectedName);
        }
    }

    @Nested
    @DisplayName("fromCode - obscured")
    class FromCodeObscured {

        @Test
        @DisplayName("Should parse slash as OBSCURED")
        void shouldParseSlashAsObscured() {
            assertThat(LowCloudType.fromCode("/")).isEqualTo(LowCloudType.OBSCURED);
        }
    }

    @Nested
    @DisplayName("fromCode - invalid input")
    class FromCodeInvalid {

        @Test
        @DisplayName("Should throw on null code")
        void shouldThrowOnNullCode() {
            assertThatThrownBy(() -> LowCloudType.fromCode(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }

        @ParameterizedTest
        @CsvSource({"X", "10", "-1", "''", "A9"})
        void shouldThrowOnInvalidCode(String code) {
            assertThatThrownBy(() -> LowCloudType.fromCode(code))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid low cloud type code");
        }
    }

    @Nested
    @DisplayName("getSummary")
    class GetSummary {

        @Test
        @DisplayName("Should return non-blank summary for every constant")
        void shouldReturnNonBlankSummaryForEveryConstant() {
            for (LowCloudType type : LowCloudType.values()) {
                assertThat(type.getSummary()).isNotBlank();
            }
        }

        @Test
        @DisplayName("Should describe NONE as no low cloud")
        void shouldDescribeNoneAsNoLowCloud() {
            assertThat(LowCloudType.NONE.getSummary()).isEqualTo("No low cloud");
        }

        @Test
        @DisplayName("Should describe OBSCURED consistently")
        void shouldDescribeObscured() {
            assertThat(LowCloudType.OBSCURED.getSummary())
                    .isEqualTo("Obscured by overcast layer below");
        }
    }

    @Nested
    @DisplayName("Live data cross-checks")
    class LiveDataCrossChecks {

        @Test
        @DisplayName("PTYA 8/378 low cloud code 3 should resolve to Cumulonimbus calvus")
        void ptyaLowCloudShouldBeCumulonimbusCalvus() {
            assertThat(LowCloudType.fromCode("3")).isEqualTo(LowCloudType.CUMULONIMBUS_CALVUS);
        }

        @Test
        @DisplayName("NSTU 8/87/ low cloud code 8 should resolve to Cumulus and Stratocumulus at different levels")
        void nstuLowCloudShouldBeCumulusAndStratocumulusDifferentLevels() {
            assertThat(LowCloudType.fromCode("8"))
                    .isEqualTo(LowCloudType.CUMULUS_AND_STRATOCUMULUS_DIFFERENT_LEVELS);
        }
    }
}
