/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025-2026 bclasky1539
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package weather.model.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for ChangeIndicator enum.
 * Tests TAF forecast change indicator types.
 *
 * @author bclasky1539
 *
 */
class ChangeIndicatorTest {

    // ==================== Basic Enum Tests ====================

    @Test
    @DisplayName("Enum has exactly 5 values")
    void testEnumValues() {
        ChangeIndicator[] values = ChangeIndicator.values();
        assertThat(values)
                .hasSize(5)
                .containsExactly(
                        ChangeIndicator.BASE,
                        ChangeIndicator.FM,
                        ChangeIndicator.TEMPO,
                        ChangeIndicator.BECMG,
                        ChangeIndicator.PROB
                        );
    }

    @Test
    @DisplayName("valueOf works correctly")
    void testValueOf() {
        assertThat(ChangeIndicator.valueOf("BASE")).isEqualTo(ChangeIndicator.BASE);
        assertThat(ChangeIndicator.valueOf("FM")).isEqualTo(ChangeIndicator.FM);
        assertThat(ChangeIndicator.valueOf("TEMPO")).isEqualTo(ChangeIndicator.TEMPO);
        assertThat(ChangeIndicator.valueOf("BECMG")).isEqualTo(ChangeIndicator.BECMG);
        assertThat(ChangeIndicator.valueOf("PROB")).isEqualTo(ChangeIndicator.PROB);
    }

    // ==================== Code Tests ====================

    @Nested
    @DisplayName("Code Getters")
    class CodeTests {

        @Test
        void testGetCode_BASE() {
            assertThat(ChangeIndicator.BASE.getCode()).isEqualTo("BASE");
        }

        @Test
        void testGetCode_FM() {
            assertThat(ChangeIndicator.FM.getCode()).isEqualTo("FM");
        }

        @Test
        void testGetCode_TEMPO() {
            assertThat(ChangeIndicator.TEMPO.getCode()).isEqualTo("TEMPO");
        }

        @Test
        void testGetCode_BECMG() {
            assertThat(ChangeIndicator.BECMG.getCode()).isEqualTo("BECMG");
        }

        @Test
        void testGetCode_PROB() {
            assertThat(ChangeIndicator.PROB.getCode()).isEqualTo("PROB");
        }
    }

    // ==================== Display Name Tests ====================

    @Nested
    @DisplayName("Display Names")
    class DisplayNameTests {

        @Test
        void testGetDisplayName_BASE() {
            assertThat(ChangeIndicator.BASE.getDisplayName()).isEqualTo("Base Forecast");
        }

        @Test
        void testGetDisplayName_FM() {
            assertThat(ChangeIndicator.FM.getDisplayName()).isEqualTo("From");
        }

        @Test
        void testGetDisplayName_TEMPO() {
            assertThat(ChangeIndicator.TEMPO.getDisplayName()).isEqualTo("Temporary");
        }

        @Test
        void testGetDisplayName_BECMG() {
            assertThat(ChangeIndicator.BECMG.getDisplayName()).isEqualTo("Becoming");
        }

        @Test
        void testGetDisplayName_PROB() {
            assertThat(ChangeIndicator.PROB.getDisplayName()).isEqualTo("Probability");
        }
    }

    // ==================== Description Tests ====================

    @Nested
    @DisplayName("Descriptions")
    class DescriptionTests {

        @ParameterizedTest
        @EnumSource(ChangeIndicator.class)
        void testGetDescription_NotNull(ChangeIndicator indicator) {
            assertThat(indicator.getDescription()).isNotNull();
            assertThat(indicator.getDescription()).isNotEmpty();
        }

        @Test
        void testGetDescription_BASE() {
            assertThat(ChangeIndicator.BASE.getDescription())
                    .isEqualTo("Initial conditions for the forecast period");
        }

        @Test
        void testGetDescription_FM() {
            assertThat(ChangeIndicator.FM.getDescription())
                    .isEqualTo("Permanent change starting at exact time");
        }

        @Test
        void testGetDescription_TEMPO() {
            assertThat(ChangeIndicator.TEMPO.getDescription())
                    .isEqualTo("Temporary fluctuations (< 1hr at a time, < half of period)");
        }

        @Test
        void testGetDescription_BECMG() {
            assertThat(ChangeIndicator.BECMG.getDescription())
                    .isEqualTo("Gradual change over the specified period");
        }

        @Test
        void testGetDescription_PROB() {
            assertThat(ChangeIndicator.PROB.getDescription())
                    .isEqualTo("Probabilistic forecast (PROB30 or PROB40)");
        }
    }

    // ==================== Permanent Tests ====================

    @Nested
    @DisplayName("Permanent Change Tests")
    class PermanentTests {

        @Test
        void testIsPermanent_BASE_True() {
            assertThat(ChangeIndicator.BASE.isPermanent()).isTrue();
        }

        @Test
        void testIsPermanent_FM_True() {
            assertThat(ChangeIndicator.FM.isPermanent()).isTrue();
        }

        @Test
        void testIsPermanent_TEMPO_False() {
            assertThat(ChangeIndicator.TEMPO.isPermanent()).isFalse();
        }

        @Test
        void testIsPermanent_BECMG_False() {
            assertThat(ChangeIndicator.BECMG.isPermanent()).isFalse();
        }

        @Test
        void testIsPermanent_PROB_False() {
            assertThat(ChangeIndicator.PROB.isPermanent()).isFalse();
        }
    }

    // ==================== Temporary Tests ====================

    @Nested
    @DisplayName("Temporary Change Tests")
    class TemporaryTests {

        @Test
        void testIsTemporary_TEMPO_True() {
            assertThat(ChangeIndicator.TEMPO.isTemporary()).isTrue();
        }

        @Test
        void testIsTemporary_BASE_False() {
            assertThat(ChangeIndicator.BASE.isTemporary()).isFalse();
        }

        @Test
        void testIsTemporary_FM_False() {
            assertThat(ChangeIndicator.FM.isTemporary()).isFalse();
        }

        @Test
        void testIsTemporary_BECMG_False() {
            assertThat(ChangeIndicator.BECMG.isTemporary()).isFalse();
        }

        @Test
        void testIsTemporary_PROB_False() {
            assertThat(ChangeIndicator.PROB.isTemporary()).isFalse();
        }
    }

    // ==================== Gradual Tests ====================

    @Nested
    @DisplayName("Gradual Change Tests")
    class GradualTests {

        @Test
        void testIsGradual_BECMG_True() {
            assertThat(ChangeIndicator.BECMG.isGradual()).isTrue();
        }

        @Test
        void testIsGradual_BASE_False() {
            assertThat(ChangeIndicator.BASE.isGradual()).isFalse();
        }

        @Test
        void testIsGradual_FM_False() {
            assertThat(ChangeIndicator.FM.isGradual()).isFalse();
        }

        @Test
        void testIsGradual_TEMPO_False() {
            assertThat(ChangeIndicator.TEMPO.isGradual()).isFalse();
        }

        @Test
        void testIsGradual_PROB_False() {
            assertThat(ChangeIndicator.PROB.isGradual()).isFalse();
        }
    }

    // ==================== Probabilistic Tests ====================

    @Nested
    @DisplayName("Probabilistic Tests")
    class ProbabilisticTests {

        @Test
        void testIsProbabilistic_PROB_True() {
            assertThat(ChangeIndicator.PROB.isProbabilistic()).isTrue();
        }

        @Test
        void testIsProbabilistic_BASE_False() {
            assertThat(ChangeIndicator.BASE.isProbabilistic()).isFalse();
        }

        @Test
        void testIsProbabilistic_FM_False() {
            assertThat(ChangeIndicator.FM.isProbabilistic()).isFalse();
        }

        @Test
        void testIsProbabilistic_TEMPO_False() {
            assertThat(ChangeIndicator.TEMPO.isProbabilistic()).isFalse();
        }

        @Test
        void testIsProbabilistic_BECMG_False() {
            assertThat(ChangeIndicator.BECMG.isProbabilistic()).isFalse();
        }
    }

    // ==================== Time Period Tests ====================

    @Nested
    @DisplayName("Time Period Requirement Tests")
    class TimePeriodTests {

        @Test
        void testRequiresTimePeriod_TEMPO_True() {
            assertThat(ChangeIndicator.TEMPO.requiresTimePeriod()).isTrue();
        }

        @Test
        void testRequiresTimePeriod_BECMG_True() {
            assertThat(ChangeIndicator.BECMG.requiresTimePeriod()).isTrue();
        }

        @Test
        void testRequiresTimePeriod_PROB_True() {
            assertThat(ChangeIndicator.PROB.requiresTimePeriod()).isTrue();
        }

        @Test
        void testRequiresTimePeriod_BASE_False() {
            assertThat(ChangeIndicator.BASE.requiresTimePeriod()).isFalse();
        }

        @Test
        void testRequiresTimePeriod_FM_False() {
            assertThat(ChangeIndicator.FM.requiresTimePeriod()).isFalse();
        }
    }

    // ==================== Exact Time Tests ====================

    @Nested
    @DisplayName("Exact Time Usage Tests")
    class ExactTimeTests {

        @Test
        void testUsesExactTime_FM_True() {
            assertThat(ChangeIndicator.FM.usesExactTime()).isTrue();
        }

        @Test
        void testUsesExactTime_BASE_False() {
            assertThat(ChangeIndicator.BASE.usesExactTime()).isFalse();
        }

        @Test
        void testUsesExactTime_TEMPO_False() {
            assertThat(ChangeIndicator.TEMPO.usesExactTime()).isFalse();
        }

        @Test
        void testUsesExactTime_BECMG_False() {
            assertThat(ChangeIndicator.BECMG.usesExactTime()).isFalse();
        }

        @Test
        void testUsesExactTime_PROB_False() {
            assertThat(ChangeIndicator.PROB.usesExactTime()).isFalse();
        }
    }

    // ==================== FromCode Tests ====================

    @Nested
    @DisplayName("FromCode Parsing Tests")
    class FromCodeTests {

        @Test
        void testFromCode_BASE() {
            assertThat(ChangeIndicator.fromCode("BASE")).isEqualTo(ChangeIndicator.BASE);
        }

        @Test
        void testFromCode_FM() {
            assertThat(ChangeIndicator.fromCode("FM")).isEqualTo(ChangeIndicator.FM);
        }

        @Test
        void testFromCode_TEMPO() {
            assertThat(ChangeIndicator.fromCode("TEMPO")).isEqualTo(ChangeIndicator.TEMPO);
        }

        @Test
        void testFromCode_BECMG() {
            assertThat(ChangeIndicator.fromCode("BECMG")).isEqualTo(ChangeIndicator.BECMG);
        }

        @Test
        void testFromCode_PROB() {
            assertThat(ChangeIndicator.fromCode("PROB")).isEqualTo(ChangeIndicator.PROB);
        }

        @Test
        void testFromCode_PROB30() {
            assertThat(ChangeIndicator.fromCode("PROB30")).isEqualTo(ChangeIndicator.PROB);
        }

        @Test
        void testFromCode_PROB40() {
            assertThat(ChangeIndicator.fromCode("PROB40")).isEqualTo(ChangeIndicator.PROB);
        }

        @ParameterizedTest
        @ValueSource(strings = {"base", "fm", "tempo", "becmg", "prob"})
        void testFromCode_CaseInsensitive(String code) {
            assertThat(ChangeIndicator.fromCode(code)).isNotNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {" BASE ", " FM ", " TEMPO "})
        void testFromCode_Trimming(String code) {
            assertThat(ChangeIndicator.fromCode(code)).isNotNull();
        }

        @Test
        void testFromCode_Null() {
            assertThat(ChangeIndicator.fromCode(null)).isNull();
        }

        @Test
        void testFromCode_Empty() {
            assertThat(ChangeIndicator.fromCode("")).isNull();
        }

        @Test
        void testFromCode_Blank() {
            assertThat(ChangeIndicator.fromCode("   ")).isNull();
        }

        @Test
        void testFromCode_Invalid() {
            assertThat(ChangeIndicator.fromCode("INVALID")).isNull();
        }

        @Test
        void testFromCode_InvalidPROB() {
            assertThat(ChangeIndicator.fromCode("PROB50")).isEqualTo(ChangeIndicator.PROB);
        }
    }

    // ==================== Summary Tests ====================

    @Nested
    @DisplayName("Summary Generation Tests")
    class SummaryTests {

        @ParameterizedTest
        @EnumSource(ChangeIndicator.class)
        void testGetSummary_NotNull(ChangeIndicator indicator) {
            assertThat(indicator.getSummary()).isNotNull();
            assertThat(indicator.getSummary()).isNotEmpty();
        }

        @Test
        void testGetSummary_BASE() {
            String summary = ChangeIndicator.BASE.getSummary();
            assertThat(summary)
                    .contains("BASE")
                    .contains("Base Forecast")
                    .contains("Initial conditions");
        }

        @Test
        void testGetSummary_FM() {
            String summary = ChangeIndicator.FM.getSummary();
            assertThat(summary)
                    .contains("FM")
                    .contains("From")
                    .contains("Permanent change");
        }

        @Test
        void testGetSummary_TEMPO() {
            String summary = ChangeIndicator.TEMPO.getSummary();
            assertThat(summary)
                    .contains("TEMPO")
                    .contains("Temporary")
                    .contains("fluctuations");
        }

        @Test
        void testGetSummary_BECMG() {
            String summary = ChangeIndicator.BECMG.getSummary();
            assertThat(summary)
                    .contains("BECMG")
                    .contains("Becoming")
                    .contains("Gradual");
        }

        @Test
        void testGetSummary_PROB() {
            String summary = ChangeIndicator.PROB.getSummary();
            assertThat(summary)
                    .contains("PROB")
                    .contains("Probability")
                    .contains("Probabilistic");
        }
    }

    // ==================== ToString Tests ====================

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        void testToString_BASE() {
            assertThat(ChangeIndicator.BASE.toString()).hasToString("BASE");
        }

        @Test
        void testToString_FM() {
            assertThat(ChangeIndicator.FM.toString()).hasToString("FM");
        }

        @Test
        void testToString_TEMPO() {
            assertThat(ChangeIndicator.TEMPO.toString()).hasToString("TEMPO");
        }

        @Test
        void testToString_BECMG() {
            assertThat(ChangeIndicator.BECMG.toString()).hasToString("BECMG");
        }

        @Test
        void testToString_PROB() {
            assertThat(ChangeIndicator.PROB.toString()).hasToString("PROB");
        }
    }

    // ==================== Combination Tests ====================

    @Nested
    @DisplayName("Logical Combination Tests")
    class CombinationTests {

        @Test
        void testMutualExclusivity_PermanentAndTemporary() {
            // A change cannot be both permanent and temporary
            for (ChangeIndicator indicator : ChangeIndicator.values()) {
                if (indicator.isPermanent()) {
                    assertThat(indicator.isTemporary()).isFalse();
                    assertThat(indicator.isGradual()).isFalse();
                    assertThat(indicator.isProbabilistic()).isFalse();
                }
            }
        }

        @Test
        void testMutualExclusivity_TimePeriodAndExactTime() {
            // A change cannot require both time period and exact time
            for (ChangeIndicator indicator : ChangeIndicator.values()) {
                if (indicator.requiresTimePeriod()) {
                    assertThat(indicator.usesExactTime()).isFalse();
                }
                if (indicator.usesExactTime()) {
                    assertThat(indicator.requiresTimePeriod()).isFalse();
                }
            }
        }

        @Test
        void testBASE_Properties() {
            ChangeIndicator base = ChangeIndicator.BASE;
            assertThat(base.isPermanent()).isTrue();
            assertThat(base.isTemporary()).isFalse();
            assertThat(base.isGradual()).isFalse();
            assertThat(base.isProbabilistic()).isFalse();
            assertThat(base.requiresTimePeriod()).isFalse();
            assertThat(base.usesExactTime()).isFalse();
        }

        @Test
        void testFM_Properties() {
            ChangeIndicator fm = ChangeIndicator.FM;
            assertThat(fm.isPermanent()).isTrue();
            assertThat(fm.isTemporary()).isFalse();
            assertThat(fm.isGradual()).isFalse();
            assertThat(fm.isProbabilistic()).isFalse();
            assertThat(fm.requiresTimePeriod()).isFalse();
            assertThat(fm.usesExactTime()).isTrue();
        }

        @Test
        void testTEMPO_Properties() {
            ChangeIndicator tempo = ChangeIndicator.TEMPO;
            assertThat(tempo.isPermanent()).isFalse();
            assertThat(tempo.isTemporary()).isTrue();
            assertThat(tempo.isGradual()).isFalse();
            assertThat(tempo.isProbabilistic()).isFalse();
            assertThat(tempo.requiresTimePeriod()).isTrue();
            assertThat(tempo.usesExactTime()).isFalse();
        }

        @Test
        void testBECMG_Properties() {
            ChangeIndicator becmg = ChangeIndicator.BECMG;
            assertThat(becmg.isPermanent()).isFalse();
            assertThat(becmg.isTemporary()).isFalse();
            assertThat(becmg.isGradual()).isTrue();
            assertThat(becmg.isProbabilistic()).isFalse();
            assertThat(becmg.requiresTimePeriod()).isTrue();
            assertThat(becmg.usesExactTime()).isFalse();
        }

        @Test
        void testPROB_Properties() {
            ChangeIndicator prob = ChangeIndicator.PROB;
            assertThat(prob.isPermanent()).isFalse();
            assertThat(prob.isTemporary()).isFalse();
            assertThat(prob.isGradual()).isFalse();
            assertThat(prob.isProbabilistic()).isTrue();
            assertThat(prob.requiresTimePeriod()).isTrue();
            assertThat(prob.usesExactTime()).isFalse();
        }
    }
}
