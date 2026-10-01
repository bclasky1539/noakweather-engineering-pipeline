package weather.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HighCloudTypeTest {

    @Nested
    @DisplayName("fromCode - valid digits")
    class FromCodeValidDigits {

        @ParameterizedTest
        @CsvSource({
                "0, NONE",
                "1, CIRRUS_FIBRATUS_OR_UNCINUS",
                "2, CIRRUS_SPISSATUS_OR_CASTELLANUS_OR_FLOCCUS",
                "3, CIRRUS_SPISSATUS_FROM_CUMULONIMBUS",
                "4, CIRRUS_INVADING",
                "5, CIRROSTRATUS_INVADING_BELOW_45_DEGREES",
                "6, CIRROSTRATUS_INVADING_ABOVE_45_DEGREES",
                "7, CIRROSTRATUS_COVERING_WHOLE_SKY",
                "8, CIRROSTRATUS_NOT_INVADING_NOT_COVERING",
                "9, CIRROCUMULUS_PREDOMINANT"
        })
        void shouldParseEachDigitToExpectedConstant(String code, String expectedName) {
            HighCloudType result = HighCloudType.fromCode(code);
            assertThat(result.name()).isEqualTo(expectedName);
        }
    }

    @Nested
    @DisplayName("fromCode - obscured")
    class FromCodeObscured {

        @Test
        @DisplayName("Should parse slash as OBSCURED")
        void shouldParseSlashAsObscured() {
            assertThat(HighCloudType.fromCode("/")).isEqualTo(HighCloudType.OBSCURED);
        }
    }

    @Nested
    @DisplayName("fromCode - invalid input")
    class FromCodeInvalid {

        @Test
        @DisplayName("Should throw on null code")
        void shouldThrowOnNullCode() {
            assertThatThrownBy(() -> HighCloudType.fromCode(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }

        @ParameterizedTest
        @CsvSource({"X", "10", "-1", "''", "A9"})
        void shouldThrowOnInvalidCode(String code) {
            assertThatThrownBy(() -> HighCloudType.fromCode(code))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid high cloud type code");
        }
    }

    @Nested
    @DisplayName("getSummary")
    class GetSummary {

        @Test
        @DisplayName("Should return non-blank summary for every constant")
        void shouldReturnNonBlankSummaryForEveryConstant() {
            for (HighCloudType type : HighCloudType.values()) {
                assertThat(type.getSummary()).isNotBlank();
            }
        }

        @Test
        @DisplayName("Should describe NONE as no high cloud")
        void shouldDescribeNoneAsNoHighCloud() {
            assertThat(HighCloudType.NONE.getSummary()).isEqualTo("No high cloud");
        }

        @Test
        @DisplayName("Should describe OBSCURED consistently")
        void shouldDescribeObscured() {
            assertThat(HighCloudType.OBSCURED.getSummary())
                    .isEqualTo("Obscured by overcast layer below");
        }
    }

    @Nested
    @DisplayName("Live data cross-checks")
    class LiveDataCrossChecks {

        @Test
        @DisplayName("PTYA 8/378 high cloud code 8 should resolve to Cirrostratus not invading/not covering")
        void ptyaHighCloudShouldBeCirrostratusNotInvadingNotCovering() {
            assertThat(HighCloudType.fromCode("8"))
                    .isEqualTo(HighCloudType.CIRROSTRATUS_NOT_INVADING_NOT_COVERING);
        }

        @Test
        @DisplayName("NSTU 8/87/ high cloud position obscured should resolve to OBSCURED, consistent with reported OVC100")
        void nstuHighCloudShouldBeObscured() {
            assertThat(HighCloudType.fromCode("/")).isEqualTo(HighCloudType.OBSCURED);
        }
    }
}
