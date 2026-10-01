package weather.model.components.remark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import weather.model.enums.HighCloudType;
import weather.model.enums.LowCloudType;
import weather.model.enums.MiddleCloudType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PredominantCloudTypesTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("Should construct with all three layers populated")
        void shouldConstructWithAllThreeLayersPopulated() {
            PredominantCloudTypes types = new PredominantCloudTypes(
                    LowCloudType.CUMULONIMBUS_CALVUS,
                    MiddleCloudType.ALTOCUMULUS_MULTI_LEVEL_OR_WITH_ALTOSTRATUS_OR_OPACUS,
                    HighCloudType.CIRROSTRATUS_NOT_INVADING_NOT_COVERING
            );

            assertThat(types.lowCloud()).isEqualTo(LowCloudType.CUMULONIMBUS_CALVUS);
            assertThat(types.middleCloud()).isEqualTo(MiddleCloudType.ALTOCUMULUS_MULTI_LEVEL_OR_WITH_ALTOSTRATUS_OR_OPACUS);
            assertThat(types.highCloud()).isEqualTo(HighCloudType.CIRROSTRATUS_NOT_INVADING_NOT_COVERING);
        }

        @Test
        @DisplayName("Should construct with an obscured layer")
        void shouldConstructWithAnObscuredLayer() {
            PredominantCloudTypes types = new PredominantCloudTypes(
                    LowCloudType.CUMULUS_AND_STRATOCUMULUS_DIFFERENT_LEVELS,
                    MiddleCloudType.ALTOCUMULUS_MULTI_LEVEL_OR_WITH_ALTOSTRATUS_OR_OPACUS,
                    HighCloudType.OBSCURED
            );

            assertThat(types.highCloud()).isEqualTo(HighCloudType.OBSCURED);
        }

        @Test
        @DisplayName("Should throw when lowCloud is null")
        void shouldThrowWhenLowCloudIsNull() {
            assertThatThrownBy(() -> new PredominantCloudTypes(
                    null, MiddleCloudType.NONE, HighCloudType.NONE))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw when middleCloud is null")
        void shouldThrowWhenMiddleCloudIsNull() {
            assertThatThrownBy(() -> new PredominantCloudTypes(
                    LowCloudType.NONE, null, HighCloudType.NONE))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw when highCloud is null")
        void shouldThrowWhenHighCloudIsNull() {
            assertThatThrownBy(() -> new PredominantCloudTypes(
                    LowCloudType.NONE, MiddleCloudType.NONE, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("getSummary")
    class GetSummary {

        @Test
        @DisplayName("Should combine all three layer summaries")
        void shouldCombineAllThreeLayerSummaries() {
            PredominantCloudTypes types = new PredominantCloudTypes(
                    LowCloudType.NONE, MiddleCloudType.NONE, HighCloudType.NONE);

            assertThat(types.getSummary())
                    .isEqualTo("Low: No low cloud; Middle: No middle cloud; High: No high cloud");
        }

        @Test
        @DisplayName("Should include obscured text when a layer is obscured")
        void shouldIncludeObscuredTextWhenLayerIsObscured() {
            PredominantCloudTypes types = new PredominantCloudTypes(
                    LowCloudType.CUMULUS_AND_STRATOCUMULUS_DIFFERENT_LEVELS,
                    MiddleCloudType.ALTOCUMULUS_MULTI_LEVEL_OR_WITH_ALTOSTRATUS_OR_OPACUS,
                    HighCloudType.OBSCURED);

            assertThat(types.getSummary()).contains("Obscured by overcast layer below");
        }
    }

    @Nested
    @DisplayName("Live data cross-checks")
    class LiveDataCrossChecks {

        @Test
        @DisplayName("PTYA 8/378 should decode to Cumulonimbus calvus / Ac(shared-7) / Cirrostratus not invading")
        void ptyaShouldDecodeAllThreeLayersCorrectly() {
            PredominantCloudTypes types = new PredominantCloudTypes(
                    LowCloudType.fromCode("3"),
                    MiddleCloudType.fromCode("7"),
                    HighCloudType.fromCode("8")
            );

            assertThat(types.lowCloud()).isEqualTo(LowCloudType.CUMULONIMBUS_CALVUS);
            assertThat(types.middleCloud()).isEqualTo(MiddleCloudType.ALTOCUMULUS_MULTI_LEVEL_OR_WITH_ALTOSTRATUS_OR_OPACUS);
            assertThat(types.highCloud()).isEqualTo(HighCloudType.CIRROSTRATUS_NOT_INVADING_NOT_COVERING);
        }

        @Test
        @DisplayName("NSTU 8/87/ should decode to Cu+Sc different levels / Ac(shared-7) / Obscured, consistent with reported OVC100")
        void nstuShouldDecodeAllThreeLayersCorrectly() {
            PredominantCloudTypes types = new PredominantCloudTypes(
                    LowCloudType.fromCode("8"),
                    MiddleCloudType.fromCode("7"),
                    HighCloudType.fromCode("/")
            );

            assertThat(types.lowCloud()).isEqualTo(LowCloudType.CUMULUS_AND_STRATOCUMULUS_DIFFERENT_LEVELS);
            assertThat(types.middleCloud()).isEqualTo(MiddleCloudType.ALTOCUMULUS_MULTI_LEVEL_OR_WITH_ALTOSTRATUS_OR_OPACUS);
            assertThat(types.highCloud()).isEqualTo(HighCloudType.OBSCURED);
        }
    }
}
