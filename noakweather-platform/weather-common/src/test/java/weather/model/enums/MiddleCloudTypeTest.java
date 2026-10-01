package weather.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MiddleCloudTypeTest {

    @Nested
    @DisplayName("fromCode - valid digits")
    class FromCodeValidDigits {

        @ParameterizedTest
        @CsvSource({
                "0, NONE",
                "1, ALTOSTRATUS_TRANSLUCIDUS",
                "2, ALTOSTRATUS_DENSE_OR_NIMBOSTRATUS",
                "3, ALTOCUMULUS_TRANSLUCIDUS_SINGLE_LEVEL",
                "4, ALTOCUMULUS_PATCHES_CHANGING",
                "5, ALTOCUMULUS_INVADING",
                "6, ALTOCUMULUS_CUMULOGENITUS",
                "7, ALTOCUMULUS_MULTI_LEVEL_OR_WITH_ALTOSTRATUS_OR_OPACUS",
                "8, ALTOCUMULUS_CASTELLANUS_OR_FLOCCUS",
                "9, ALTOCUMULUS_CHAOTIC_SKY"
        })
        void shouldParseEachDigitToExpectedConstant(String code, String expectedName) {
            MiddleCloudType result = MiddleCloudType.fromCode(code);
            assertThat(result.name()).isEqualTo(expectedName);
        }
    }

    @Nested
    @DisplayName("fromCode - obscured")
    class FromCodeObscured {

        @Test
        @DisplayName("Should parse slash as OBSCURED")
        void shouldParseSlashAsObscured() {
            assertThat(MiddleCloudType.fromCode("/")).isEqualTo(MiddleCloudType.OBSCURED);
        }
    }

    @Nested
    @DisplayName("fromCode - invalid input")
    class FromCodeInvalid {

        @Test
        @DisplayName("Should throw on null code")
        void shouldThrowOnNullCode() {
            assertThatThrownBy(() -> MiddleCloudType.fromCode(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }

        @ParameterizedTest
        @CsvSource({"X", "10", "-1", "''", "A9"})
        void shouldThrowOnInvalidCode(String code) {
            assertThatThrownBy(() -> MiddleCloudType.fromCode(code))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid middle cloud type code");
        }
    }

    @Nested
    @DisplayName("getSummary")
    class GetSummary {

        @Test
        @DisplayName("Should return non-blank summary for every constant")
        void shouldReturnNonBlankSummaryForEveryConstant() {
            for (MiddleCloudType type : MiddleCloudType.values()) {
                assertThat(type.getSummary()).isNotBlank();
            }
        }

        @Test
        @DisplayName("Should describe NONE as no middle cloud")
        void shouldDescribeNoneAsNoMiddleCloud() {
            assertThat(MiddleCloudType.NONE.getSummary()).isEqualTo("No middle cloud");
        }

        @Test
        @DisplayName("Should describe OBSCURED consistently")
        void shouldDescribeObscured() {
            assertThat(MiddleCloudType.OBSCURED.getSummary())
                    .isEqualTo("Obscured by overcast layer below");
        }
    }

    @Nested
    @DisplayName("Live data cross-checks")
    class LiveDataCrossChecks {

        @Test
        @DisplayName("PTYA 8/378 middle cloud code 7 should resolve to the shared-digit-7 constant")
        void ptyaMiddleCloudShouldResolveToDigitSevenConstant() {
            assertThat(MiddleCloudType.fromCode("7"))
                    .isEqualTo(MiddleCloudType.ALTOCUMULUS_MULTI_LEVEL_OR_WITH_ALTOSTRATUS_OR_OPACUS);
        }

        @Test
        @DisplayName("NSTU 8/87/ middle cloud code 7 should resolve to the shared-digit-7 constant")
        void nstuMiddleCloudShouldResolveToDigitSevenConstant() {
            assertThat(MiddleCloudType.fromCode("7"))
                    .isEqualTo(MiddleCloudType.ALTOCUMULUS_MULTI_LEVEL_OR_WITH_ALTOSTRATUS_OR_OPACUS);
        }
    }
}
