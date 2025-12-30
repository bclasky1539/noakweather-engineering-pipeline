/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025 bclasky1539
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
package weather.model.components.remark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for PressureTendency record.
 *
 * @author bclasky1539
 *
 */
class PressureTendencyTest {

    // ==================== Factory Method Tests ====================

    @Test
    @DisplayName("Should create from METAR format")
    void testFromMetar() {
        PressureTendency tendency = PressureTendency.fromMetar(2, "032");

        assertThat(tendency.tendencyCode()).isEqualTo(2);
        assertThat(tendency.changeHectopascals()).isEqualTo(3.2);
    }

    @Test
    @DisplayName("Should create from explicit values")
    void testOf() {
        PressureTendency tendency = PressureTendency.of(7, 4.5);

        assertThat(tendency.tendencyCode()).isEqualTo(7);
        assertThat(tendency.changeHectopascals()).isEqualTo(4.5);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 000, 0.0",
            "2, 015, 1.5",
            "7, 045, 4.5",
            "4, 100, 10.0",
            "8, 500, 50.0"
    })
    @DisplayName("Should parse various METAR change codes")
    void testFromMetar_VariousCodes(int tendencyCode, String changeCode, double expectedChange) {
        PressureTendency tendency = PressureTendency.fromMetar(tendencyCode, changeCode);

        assertThat(tendency.tendencyCode()).isEqualTo(tendencyCode);
        assertThat(tendency.changeHectopascals()).isEqualTo(expectedChange);
    }

    // ==================== Validation Tests ====================

    @Test
    @DisplayName("Should reject null tendency code")
    void testValidation_NullTendencyCode() {
        assertThatThrownBy(() -> new PressureTendency(null, 3.2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tendency code cannot be null");
    }

    @Test
    @DisplayName("Should reject null change value")
    void testValidation_NullChange() {
        assertThatThrownBy(() -> new PressureTendency(2, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pressure change cannot be null");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 9, 10, 100})
    @DisplayName("Should reject invalid tendency codes")
    void testValidation_InvalidTendencyCode(int invalidCode) {
        assertThatThrownBy(() -> new PressureTendency(invalidCode, 3.2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tendency code must be 0-8");
    }

    @Test
    @DisplayName("Should reject negative change value")
    void testValidation_NegativeChange() {
        assertThatThrownBy(() -> new PressureTendency(2, -1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pressure change cannot be negative");
    }

    @Test
    @DisplayName("Should reject unrealistic change value")
    void testValidation_UnrealisticChange() {
        assertThatThrownBy(() -> new PressureTendency(2, 51.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pressure change too large");
    }

    @Test
    @DisplayName("Should reject invalid METAR change code format")
    @SuppressWarnings("DataFlowIssue")  // Intentionally testing null handling
    void testValidation_InvalidMetarFormat() {
        assertThatThrownBy(() -> PressureTendency.fromMetar(2, "12"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Change code must be 3 digits");

        assertThatThrownBy(() -> PressureTendency.fromMetar(2, "ABCD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Change code must be 3 digits");

        assertThatThrownBy(() -> PressureTendency.fromMetar(2, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Change code must be 3 digits");
    }

    // ==================== Tendency Code Tests ====================

    @ParameterizedTest
    @CsvSource({
            "0, true, false, false",
            "1, true, false, false",
            "2, true, false, false",
            "3, true, false, false",
            "4, false, true, false",
            "5, false, false, true",
            "6, false, false, true",
            "7, false, false, true",
            "8, false, false, true"
    })
    @DisplayName("Should identify tendency direction correctly")
    void testTendencyDirection(int code, boolean expectedIncreasing,
                               boolean expectedSteady, boolean expectedDecreasing) {
        PressureTendency tendency = PressureTendency.of(code, 3.0);

        assertThat(tendency.isIncreasing()).isEqualTo(expectedIncreasing);
        assertThat(tendency.isSteady()).isEqualTo(expectedSteady);
        assertThat(tendency.isDecreasing()).isEqualTo(expectedDecreasing);
    }

    // ==================== Change Magnitude Tests ====================

    @Test
    @DisplayName("Should identify significant change")
    void testIsSignificant() {
        assertThat(PressureTendency.of(2, 3.0).isSignificant()).isTrue();
        assertThat(PressureTendency.of(2, 5.0).isSignificant()).isTrue();
        assertThat(PressureTendency.of(2, 2.9).isSignificant()).isFalse();
    }

    @Test
    @DisplayName("Should identify rapid change")
    void testIsRapidChange() {
        assertThat(PressureTendency.of(2, 6.0).isRapidChange()).isTrue();
        assertThat(PressureTendency.of(2, 8.0).isRapidChange()).isTrue();
        assertThat(PressureTendency.of(2, 5.9).isRapidChange()).isFalse();
    }

    @Test
    @DisplayName("Should identify negligible change")
    void testIsNegligible() {
        assertThat(PressureTendency.of(4, 0.0).isNegligible()).isTrue();
        assertThat(PressureTendency.of(2, 0.5).isNegligible()).isTrue();
        assertThat(PressureTendency.of(2, 0.9).isNegligible()).isTrue();
        assertThat(PressureTendency.of(2, 1.0).isNegligible()).isFalse();
    }

    // ==================== Conversion Tests ====================

    @Test
    @DisplayName("Should convert change to inches of mercury")
    void testGetChangeInchesHg() {
        PressureTendency tendency = PressureTendency.of(2, 3.4);

        double inHg = tendency.getChangeInchesHg();

        // 3.4 hPa * 0.02953 ≈ 0.1 inHg
        assertThat(inHg).isCloseTo(0.1, org.assertj.core.data.Offset.offset(0.01));
    }

    @ParameterizedTest
    @CsvSource({
            "2, 3.2, 3.2",      // Increasing: positive
            "7, 4.5, -4.5",     // Decreasing: negative
            "4, 0.0, 0.0"       // Steady: zero
    })
    @DisplayName("Should calculate signed change correctly")
    void testGetSignedChange(int code, double change, double expectedSigned) {
        PressureTendency tendency = PressureTendency.of(code, change);

        assertThat(tendency.getSignedChange()).isEqualTo(expectedSigned);
    }

    // ==================== Description Tests ====================

    @ParameterizedTest
    @CsvSource({
            "0, 'Increasing, then decreasing'",
            "1, 'Increasing, then steady'",
            "2, 'Increasing'",
            "3, 'Increasing rapidly'",
            "4, 'Steady'",
            "5, 'Decreasing, then increasing'",
            "6, 'Decreasing, then steady'",
            "7, 'Decreasing'",
            "8, 'Decreasing rapidly'"
    })
    @DisplayName("Should return correct tendency description")
    void testGetTendencyDescription(int code, String expectedDescription) {
        PressureTendency tendency = PressureTendency.of(code, 3.0);

        assertThat(tendency.getTendencyDescription()).isEqualTo(expectedDescription);
    }

    @ParameterizedTest
    @CsvSource({
            "0, Rising",
            "1, Rising",
            "2, Rising",
            "3, Rising",
            "4, Steady",
            "5, Falling",
            "6, Falling",
            "7, Falling",
            "8, Falling"
    })
    @DisplayName("Should return correct short description")
    void testGetShortDescription(int code, String expectedShort) {
        PressureTendency tendency = PressureTendency.of(code, 3.0);

        assertThat(tendency.getShortDescription()).isEqualTo(expectedShort);
    }

    @ParameterizedTest
    @CsvSource({
            "0.0, None",
            "0.5, Slight",
            "1.0, Moderate",
            "2.5, Moderate",
            "3.0, Significant",
            "5.0, Significant",
            "6.0, Rapid",
            "10.0, Rapid"
    })
    @DisplayName("Should return correct change magnitude")
    void testGetChangeMagnitude(double change, String expectedMagnitude) {
        PressureTendency tendency = PressureTendency.of(2, change);

        assertThat(tendency.getChangeMagnitude()).isEqualTo(expectedMagnitude);
    }

    @Test
    @DisplayName("Should generate complete summary for increasing tendency")
    void testGetSummary_Increasing() {
        PressureTendency tendency = PressureTendency.of(2, 3.2);

        String summary = tendency.getSummary();

        assertThat(summary)
                .contains("Pressure tendency")
                .contains("Increasing")
                .contains("+3.2 hPa")
                .contains("inHg")
                .contains("[SIGNIFICANT]");
    }

    @Test
    @DisplayName("Should generate complete summary for decreasing tendency")
    void testGetSummary_Decreasing() {
        PressureTendency tendency = PressureTendency.of(7, 4.5);

        String summary = tendency.getSummary();

        assertThat(summary)
                .contains("Pressure tendency")
                .contains("Decreasing")
                .contains("-4.5 hPa")
                .contains("inHg")
                .contains("[SIGNIFICANT]");
    }

    @Test
    @DisplayName("Should generate complete summary for steady tendency")
    void testGetSummary_Steady() {
        PressureTendency tendency = PressureTendency.of(4, 0.0);

        String summary = tendency.getSummary();

        assertThat(summary)
                .contains("Pressure tendency")
                .contains("Steady")
                .contains("0.0 hPa")
                .doesNotContain("[SIGNIFICANT]")
                .doesNotContain("[RAPID]");
    }

    @Test
    @DisplayName("Should generate summary with rapid change flag")
    void testGetSummary_RapidChange() {
        PressureTendency tendency = PressureTendency.of(7, 8.0);

        String summary = tendency.getSummary();

        assertThat(summary).contains("[RAPID]");
    }

    // ==================== Weather Implication Tests ====================

    @Test
    @DisplayName("Should provide weather implication for rapid increasing")
    void testWeatherImplication_RapidIncreasing() {
        PressureTendency tendency = PressureTendency.of(2, 6.5);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("rapid")
                .containsIgnoringCase("improving")
                .containsIgnoringCase("clearing");
    }

    @Test
    @DisplayName("Should provide weather implication for rapid decreasing")
    void testWeatherImplication_RapidDecreasing() {
        PressureTendency tendency = PressureTendency.of(7, 7.0);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("rapid")
                .containsIgnoringCase("severe")
                .containsIgnoringCase("deteriorating");
    }

    @Test
    @DisplayName("Should provide weather implication for significant increasing")
    void testWeatherImplication_SignificantIncreasing() {
        PressureTendency tendency = PressureTendency.of(2, 4.0);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("rising")
                .containsIgnoringCase("improving");
    }

    @Test
    @DisplayName("Should provide weather implication for significant decreasing")
    void testWeatherImplication_SignificantDecreasing() {
        PressureTendency tendency = PressureTendency.of(7, 4.0);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("falling")
                .containsIgnoringCase("deteriorating");
    }

    @Test
    @DisplayName("Should provide weather implication for steady")
    void testWeatherImplication_Steady() {
        PressureTendency tendency = PressureTendency.of(4, 0.5);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("steady")
                .containsIgnoringCase("no significant weather change");
    }

    // ==================== Formatting Tests ====================

    @ParameterizedTest
    @CsvSource({
            "2, 3.2, 52032",
            "7, 4.5, 57045",
            "4, 0.0, 54000",
            "1, 1.5, 51015",
            "8, 10.0, 58100"
    })
    @DisplayName("Should format as METAR code")
    void testToMetarCode(int code, double change, String expectedMetar) {
        PressureTendency tendency = PressureTendency.of(code, change);

        assertThat(tendency.toMetarCode()).isEqualTo(expectedMetar);
    }

    @ParameterizedTest
    @CsvSource({
            "2, 3.2, '+3.2 hPa'",
            "7, 4.5, '-4.5 hPa'",
            "4, 0.0, '0.0 hPa'"
    })
    @DisplayName("Should format change with sign")
    void testGetFormattedChange(int code, double change, String expectedFormat) {
        PressureTendency tendency = PressureTendency.of(code, change);

        assertThat(tendency.getFormattedChange()).isEqualTo(expectedFormat);
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle zero change")
    void testZeroChange() {
        PressureTendency tendency = PressureTendency.of(4, 0.0);

        assertThat(tendency.changeHectopascals()).isEqualTo(0.0);
        assertThat(tendency.isSteady()).isTrue();
        assertThat(tendency.isNegligible()).isTrue();
        assertThat(tendency.isSignificant()).isFalse();
        assertThat(tendency.getSignedChange()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should handle maximum reasonable change")
    void testMaximumChange() {
        PressureTendency tendency = PressureTendency.of(7, 50.0);

        assertThat(tendency.changeHectopascals()).isEqualTo(50.0);
        assertThat(tendency.isRapidChange()).isTrue();
        assertThat(tendency.isSignificant()).isTrue();
    }

    @Test
    @DisplayName("Should handle all valid tendency codes")
    void testAllValidCodes() {
        for (int code = 0; code <= 8; code++) {
            PressureTendency tendency = PressureTendency.of(code, 3.0);

            assertThat(tendency.tendencyCode()).isEqualTo(code);
            assertThat(tendency.getTendencyDescription()).isNotEmpty();
            assertThat(tendency.getShortDescription()).isNotEmpty();
        }
    }

    // ==================== Equality and ToString Tests ====================

    @Test
    @DisplayName("Should have correct equality")
    void testEquality() {
        PressureTendency tendency1 = PressureTendency.of(2, 3.2);
        PressureTendency tendency2 = PressureTendency.of(2, 3.2);
        PressureTendency tendency3 = PressureTendency.of(7, 3.2);
        PressureTendency tendency4 = PressureTendency.of(2, 4.5);

        assertThat(tendency1)
                .isEqualTo(tendency2)
                .isNotEqualTo(tendency3)
                .isNotEqualTo(tendency4);
        assertThat(tendency1.hashCode()).hasSameHashCodeAs(tendency2.hashCode());
    }

    @Test
    @DisplayName("Should have meaningful toString")
    void testToString() {
        PressureTendency tendency = PressureTendency.of(2, 3.2);

        String str = tendency.toString();

        assertThat(str)
                .contains("PressureTendency")
                .contains("tendencyCode=2")
                .contains("changeHectopascals=3.2");
    }

    // ========== Additional Coverage Tests for getModerateChangeImplication() ==========

    @Test
    @DisplayName("Should provide implication for moderate increasing pressure")
    void testWeatherImplication_ModerateIncreasing() {
        // Change < 3.0 hPa (not significant), but isIncreasing() = true
        PressureTendency tendency = PressureTendency.of(2, 1.5);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("slight")
                .containsIgnoringCase("pressure rise")
                .containsIgnoringCase("more settled");
    }

    @Test
    @DisplayName("Should provide implication for moderate decreasing pressure")
    void testWeatherImplication_ModerateDecreasing() {
        // Change < 3.0 hPa (not significant), but isDecreasing() = true
        PressureTendency tendency = PressureTendency.of(7, 2.0);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("slight")
                .containsIgnoringCase("pressure fall")
                .containsIgnoringCase("may deteriorate");
    }

    @Test
    @DisplayName("Should provide implication for very small steady pressure")
    void testWeatherImplication_VerySmallSteady() {
        // Change < 1.0 hPa and isSteady() = true
        PressureTendency tendency = PressureTendency.of(4, 0.2);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("pressure steady")
                .containsIgnoringCase("no significant weather change");
    }

    // ========== Coverage Tests for getRapidChangeImplication() ==========

    @Test
    @DisplayName("Should provide rapid increasing implication")
    void testWeatherImplication_RapidIncreasing_Covered() {
        // Change >= 6.0 hPa and isIncreasing() = true
        PressureTendency tendency = PressureTendency.of(2, 6.5);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("rapid")
                .containsIgnoringCase("pressure rise")
                .containsIgnoringCase("improving quickly")
                .containsIgnoringCase("clearing");
    }

    @Test
    @DisplayName("Should provide rapid decreasing implication")
    void testWeatherImplication_RapidDecreasing_Covered() {
        // Change >= 6.0 hPa and isDecreasing() = true
        PressureTendency tendency = PressureTendency.of(7, 7.0);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("rapid")
                .containsIgnoringCase("pressure fall")
                .containsIgnoringCase("severe weather")
                .containsIgnoringCase("deteriorating");
    }

    @Test
    @DisplayName("Should provide steady implication for rapid steady pressure")
    void testWeatherImplication_RapidSteady() {
        // Change >= 6.0 hPa but isSteady() = true (edge case)
        // This covers the else branch in getRapidChangeImplication
        PressureTendency tendency = PressureTendency.of(4, 6.5);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("steady")
                .containsIgnoringCase("stable weather");
    }

    // ========== Coverage Tests for getSignificantChangeImplication() ==========

    @Test
    @DisplayName("Should provide significant increasing implication")
    void testWeatherImplication_SignificantIncreasing_Covered() {
        // 3.0 <= change < 6.0 and isIncreasing() = true
        PressureTendency tendency = PressureTendency.of(2, 4.0);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("rising")
                .containsIgnoringCase("improving weather")
                .containsIgnoringCase("clearing");
    }

    @Test
    @DisplayName("Should provide significant decreasing implication")
    void testWeatherImplication_SignificantDecreasing_Covered() {
        // 3.0 <= change < 6.0 and isDecreasing() = true
        PressureTendency tendency = PressureTendency.of(7, 4.0);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("falling")
                .containsIgnoringCase("weather deteriorating")
                .containsIgnoringCase("storm development");
    }

    @Test
    @DisplayName("Should provide steady implication for significant steady pressure")
    void testWeatherImplication_SignificantSteady() {
        // 3.0 <= change < 6.0 but isSteady() = true (edge case)
        // This covers the else branch in getSignificantChangeImplication
        PressureTendency tendency = PressureTendency.of(4, 4.0);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("Steady pressure")
                .containsIgnoringCase("weather conditions stable");
    }
}
