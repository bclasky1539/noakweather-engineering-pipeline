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
package weather.model.components.remark.pressureremarks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import weather.exception.InvalidObservationException;

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
    @DisplayName("Should accept null tendency code as a missing element (5/ccc) - Issue #198")
    void testValidation_NullTendencyCode() {
        PressureTendency tendency = new PressureTendency(null, 3.2);

        assertThat(tendency.tendencyCode()).isNull();
        assertThat(tendency.changeHectopascals()).isEqualTo(3.2);
    }

    @Test
    @DisplayName("Should accept null change value as a missing element (5a///) - Issue #198")
    void testValidation_NullChange() {
        PressureTendency tendency = new PressureTendency(2, null);

        assertThat(tendency.tendencyCode()).isEqualTo(2);
        assertThat(tendency.changeHectopascals()).isNull();
    }

    @Test
    @DisplayName("Should still validate the present element when the other is missing - Issue #198")
    void testValidation_PresentElementStillValidated() {
        assertThatThrownBy(() -> new PressureTendency(9, null))
                .isInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("Tendency code must be 0-8");

        assertThatThrownBy(() -> new PressureTendency(null, 51.0))
                .isInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("Pressure change too large");

        assertThatThrownBy(() -> new PressureTendency(null, -1.0))
                .isInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("Pressure change cannot be negative");
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
    void testValidation_InvalidMetarFormat() {
        assertThatThrownBy(() -> PressureTendency.fromMetar(2, "12"))
                .isInstanceOf(InvalidObservationException.class)
                .hasMessage("Change code must be 3 digits: 12")
                .hasFieldOrPropertyWithValue("component", "PressureTendency.changeCode")
                .hasFieldOrPropertyWithValue("rejectedValue", "12");

        assertThatThrownBy(() -> PressureTendency.fromMetar(2, "ABCD"))
                .isInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("Change code must be 3 digits");
    }

    @Test
    @DisplayName("Should reject null METAR change code")
    @SuppressWarnings("DataFlowIssue")
        // Intentionally testing null handling
    void testValidation_NullMetarChangeCode() {
        assertThatThrownBy(() -> PressureTendency.fromMetar(2, null))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("Change code cannot be null");
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
    void testTendencyDirection(int code, boolean expectedRising,
                               boolean expectedSteady, boolean expectedFalling) {
        PressureTendency tendency = PressureTendency.of(code, 3.0);

        assertThat(tendency.isRising()).isEqualTo(expectedRising);
        assertThat(tendency.isSteady()).isEqualTo(expectedSteady);
        assertThat(tendency.isFalling()).isEqualTo(expectedFalling);
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

        // 3.4 hPa * 0.02953 ≈ 0.1 inHg
        assertThat(tendency.getChangeInchesHg()).isCloseTo(0.1, org.assertj.core.data.Offset.offset(0.01));
    }

    @ParameterizedTest
    @CsvSource({
            "2, 3.2, 3.2",      // Rising: positive
            "7, 4.5, -4.5",     // Falling: negative
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
            "0, 'Rising, then falling'",
            "1, 'Rising, then steady; or rising, then rising more slowly'",
            "2, 'Rising (steadily or unsteadily)'",
            "3, 'Falling or steady, then rising; or rising, then rising more rapidly'",
            "4, 'Steady'",
            "5, 'Falling, then rising'",
            "6, 'Falling, then steady; or falling, then falling more slowly'",
            "7, 'Falling (steadily or unsteadily)'",
            "8, 'Steady or rising, then falling; or falling, then falling more rapidly'"
    })
    @DisplayName("Should return the WMO code 0200 description for every tendency code - Issue #218")
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
    @DisplayName("Should generate complete summary for rising tendency")
    void testGetSummary_Rising() {
        PressureTendency tendency = PressureTendency.of(2, 3.2);

        String summary = tendency.getSummary();

        assertThat(summary)
                .contains("Pressure tendency")
                .contains("Rising")
                .contains("+3.2 hPa")
                .contains("inHg")
                .contains("[SIGNIFICANT]");
    }

    @Test
    @DisplayName("Should generate complete summary for falling tendency")
    void testGetSummary_Falling() {
        PressureTendency tendency = PressureTendency.of(7, 4.5);

        String summary = tendency.getSummary();

        assertThat(summary)
                .contains("Pressure tendency")
                .contains("Falling")
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
    @DisplayName("Should provide weather implication for rapid rising")
    void testWeatherImplication_RapidRising() {
        PressureTendency tendency = PressureTendency.of(2, 6.5);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("rapid")
                .containsIgnoringCase("improving")
                .containsIgnoringCase("clearing");
    }

    @Test
    @DisplayName("Should provide weather implication for rapid falling")
    void testWeatherImplication_RapidFalling() {
        PressureTendency tendency = PressureTendency.of(7, 7.0);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("rapid")
                .containsIgnoringCase("severe")
                .containsIgnoringCase("deteriorating");
    }

    @Test
    @DisplayName("Should provide weather implication for significant rising")
    void testWeatherImplication_SignificantRising() {
        PressureTendency tendency = PressureTendency.of(2, 4.0);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("rising")
                .containsIgnoringCase("improving");
    }

    @Test
    @DisplayName("Should provide weather implication for significant falling")
    void testWeatherImplication_SignificantFalling() {
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

    // ========== Coverage Tests for Moderate and Slight Implications ==========

    @Test
    @DisplayName("Should provide implication for moderate rising pressure")
    void testWeatherImplication_ModerateRising() {
        // 1.0 <= change < 3.0 hPa and isRising() = true
        PressureTendency tendency = PressureTendency.of(2, 1.5);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("moderate")
                .containsIgnoringCase("pressure rise")
                .containsIgnoringCase("more settled");
    }

    @Test
    @DisplayName("Should provide implication for moderate falling pressure")
    void testWeatherImplication_ModerateFalling() {
        // 1.0 <= change < 3.0 hPa and isFalling() = true
        PressureTendency tendency = PressureTendency.of(7, 2.0);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("moderate")
                .containsIgnoringCase("pressure fall")
                .containsIgnoringCase("may deteriorate");
    }

    @ParameterizedTest
    @CsvSource({
            "2, 0.5, 'Slight pressure rise - little weather change expected'",
            "7, 0.5, 'Slight pressure fall - little weather change expected'"
    })
    @DisplayName("Should provide implication for slight rising and falling pressure - Issue #218")
    void testWeatherImplication_Slight(int code, double change, String expectedImplication) {
        // 0.0 < change < 1.0 hPa
        PressureTendency tendency = PressureTendency.of(code, change);

        assertThat(tendency.getWeatherImplication()).isEqualTo(expectedImplication);
    }

    @Test
    @DisplayName("Should provide steady implication for no change with a rising or falling code - Issue #218")
    void testWeatherImplication_NoChange() {
        // Codes 0 and 5 allow a net change of zero
        assertThat(PressureTendency.of(0, 0.0).getWeatherImplication())
                .isEqualTo("Pressure steady - no significant weather change expected");
        assertThat(PressureTendency.of(5, 0.0).getWeatherImplication())
                .isEqualTo("Pressure steady - no significant weather change expected");
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

    // ========== Coverage Tests for Rapid Implications ==========

    @Test
    @DisplayName("Should provide rapid rising implication")
    void testWeatherImplication_RapidRising_Covered() {
        // Change >= 6.0 hPa and isRising() = true
        PressureTendency tendency = PressureTendency.of(2, 6.5);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("rapid")
                .containsIgnoringCase("pressure rise")
                .containsIgnoringCase("improving quickly")
                .containsIgnoringCase("clearing");
    }

    @Test
    @DisplayName("Should provide rapid falling implication")
    void testWeatherImplication_RapidFalling_Covered() {
        // Change >= 6.0 hPa and isFalling() = true
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
        // This covers the steady wording for the rapid level
        PressureTendency tendency = PressureTendency.of(4, 6.5);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("steady")
                .containsIgnoringCase("stable weather");
    }

    // ========== Coverage Tests for Significant Implications ==========

    @Test
    @DisplayName("Should provide significant rising implication")
    void testWeatherImplication_SignificantRising_Covered() {
        // 3.0 <= change < 6.0 and isRising() = true
        PressureTendency tendency = PressureTendency.of(2, 4.0);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("rising")
                .containsIgnoringCase("improving weather")
                .containsIgnoringCase("clearing");
    }

    @Test
    @DisplayName("Should provide significant falling implication")
    void testWeatherImplication_SignificantFalling_Covered() {
        // 3.0 <= change < 6.0 and isFalling() = true
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
        // This covers the steady wording for the significant level
        PressureTendency tendency = PressureTendency.of(4, 4.0);

        String implication = tendency.getWeatherImplication();

        assertThat(implication)
                .containsIgnoringCase("Steady pressure")
                .containsIgnoringCase("weather conditions stable");
    }

    // ========== Missing Elements (5////, 5a///, 5/ccc) Tests ==========

    @ParameterizedTest
    @CsvSource({
            "7, 045, 7,    4.5,  57045",
            "4, ///, 4,       ,  54///",
            "/, 123,  ,   12.3,  5/123",
            "/, ///,  ,       ,  5////"
    })
    @DisplayName("Should create from METAR group elements and format back to the same group - Issue #198")
    void testFromMetarGroup_RoundTrip(String codeText, String changeText,
                                      Integer expectedCode, Double expectedChange, String expectedMetar) {
        PressureTendency tendency = PressureTendency.fromMetarGroup(codeText, changeText);

        assertThat(tendency.tendencyCode()).isEqualTo(expectedCode);
        assertThat(tendency.changeHectopascals()).isEqualTo(expectedChange);
        assertThat(tendency.toMetarCode()).isEqualTo(expectedMetar);
    }

    @ParameterizedTest
    @CsvSource({
            "A, 045, 'Tendency code must be one digit or /'",
            "//, 045, 'Tendency code must be one digit or /'",
            "7, //, 'Change code must be 3 digits or ///'",
            "7, 4/5, 'Change code must be 3 digits or ///'",
            "7, 12, 'Change code must be 3 digits or ///'",
            "9, ///, 'Tendency code must be 0-8'",
            "/, 999, 'Pressure change too large'"
    })
    @DisplayName("Should reject invalid METAR group elements - Issue #198")
    void testFromMetarGroup_InvalidElements(String codeText, String changeText, String expectedMessage) {
        assertThatThrownBy(() -> PressureTendency.fromMetarGroup(codeText, changeText))
                .isInstanceOf(InvalidObservationException.class)
                .hasMessageContaining(expectedMessage);
    }

    @Test
    @DisplayName("Should reject null METAR group element text - Issue #198")
    @SuppressWarnings("DataFlowIssue")
        // Intentionally testing null handling
    void testFromMetarGroup_NullText() {
        assertThatThrownBy(() -> PressureTendency.fromMetarGroup(null, "045"))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("cannot be null");

        assertThatThrownBy(() -> PressureTendency.fromMetarGroup("7", null))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("cannot be null");
    }

    @ParameterizedTest
    @CsvSource({
            "7,    4.5, true,  true,  false",
            "4,       , true,  false, false",
            " ,   12.3, false, true,  false",
            " ,       , false, false, true"
    })
    @DisplayName("Should report which elements are present - Issue #198")
    void testElementPresence(Integer code, Double change,
                             boolean expectedHasCode, boolean expectedHasChange, boolean expectedMissing) {
        PressureTendency tendency = new PressureTendency(code, change);

        assertThat(tendency.hasTendencyCode()).isEqualTo(expectedHasCode);
        assertThat(tendency.hasChange()).isEqualTo(expectedHasChange);
        assertThat(tendency.isMissing()).isEqualTo(expectedMissing);
    }

    @Test
    @DisplayName("missing() should equal a group with both elements missing - Issue #198")
    void testMissing() {
        PressureTendency tendency = PressureTendency.missing();

        assertThat(tendency.isMissing()).isTrue();
        assertThat(tendency)
                .isEqualTo(new PressureTendency(null, null))
                .isEqualTo(PressureTendency.fromMetarGroup("/", "///"))
                .isNotEqualTo(PressureTendency.of(4, 0.0));
        assertThat(tendency.toString())
                .contains("tendencyCode=null")
                .contains("changeHectopascals=null");
    }

    @Test
    @DisplayName("Missing tendency code: direction unknown, change still usable (5/123) - Issue #198")
    void testMissingTendencyCode() {
        PressureTendency tendency = PressureTendency.fromMetarGroup("/", "123");

        assertThat(tendency.isRising()).isFalse();
        assertThat(tendency.isFalling()).isFalse();
        assertThat(tendency.isSteady()).isFalse();
        assertThat(tendency.isSignificant()).isTrue();
        assertThat(tendency.isRapidChange()).isTrue();
        assertThat(tendency.isNegligible()).isFalse();

        assertThat(tendency.getChangeInchesHg()).isCloseTo(0.363, org.assertj.core.data.Offset.offset(0.01));
        assertThat(tendency).extracting(PressureTendency::getSignedChange).isNull();

        assertThat(tendency.getTendencyDescription()).isEqualTo("Not reported");
        assertThat(tendency.getShortDescription()).isEqualTo("Not reported");
        assertThat(tendency.getChangeMagnitude()).isEqualTo("Rapid");
        assertThat(tendency.getFormattedChange()).isEqualTo("12.3 hPa");
        assertThat(tendency.getSummary())
                .isEqualTo("Pressure tendency: Not reported, change of 12.3 hPa (0.36 inHg) [RAPID] (5/123)");
        assertThat(tendency.getWeatherImplication()).containsIgnoringCase("no implication");
    }

    @Test
    @DisplayName("Missing change: direction still usable, size unknown (54///) - Issue #198")
    void testMissingChange() {
        PressureTendency tendency = PressureTendency.fromMetarGroup("4", "///");

        assertThat(tendency.isSteady()).isTrue();
        assertThat(tendency.isRising()).isFalse();
        assertThat(tendency.isFalling()).isFalse();
        assertThat(tendency.isSignificant()).isFalse();
        assertThat(tendency.isRapidChange()).isFalse();
        assertThat(tendency.isNegligible()).isFalse();

        assertThat(tendency)
                .extracting(PressureTendency::getChangeInchesHg, PressureTendency::getSignedChange)
                .containsOnlyNulls();

        assertThat(tendency.getTendencyDescription()).isEqualTo("Steady");
        assertThat(tendency.getShortDescription()).isEqualTo("Steady");
        assertThat(tendency.getChangeMagnitude()).isEqualTo("Not reported");
        assertThat(tendency.getFormattedChange()).isEqualTo("Not reported");
        assertThat(tendency.getSummary())
                .isEqualTo("Pressure tendency: Steady, change not reported (54///)");
        assertThat(tendency.getWeatherImplication()).containsIgnoringCase("no implication");
    }

    @Test
    @DisplayName("Every element missing: nothing to report (5////) - Issue #198")
    void testEveryElementMissing() {
        PressureTendency tendency = PressureTendency.missing();

        assertThat(tendency.isRising()).isFalse();
        assertThat(tendency.isFalling()).isFalse();
        assertThat(tendency.isSteady()).isFalse();
        assertThat(tendency.isSignificant()).isFalse();
        assertThat(tendency.isRapidChange()).isFalse();
        assertThat(tendency.isNegligible()).isFalse();

        assertThat(tendency)
                .extracting(PressureTendency::getChangeInchesHg, PressureTendency::getSignedChange)
                .containsOnlyNulls();

        assertThat(tendency.getTendencyDescription()).isEqualTo("Not reported");
        assertThat(tendency.getChangeMagnitude()).isEqualTo("Not reported");
        assertThat(tendency.getFormattedChange()).isEqualTo("Not reported");
        assertThat(tendency.getSummary()).isEqualTo("Pressure tendency: Not reported (5////)");
        assertThat(tendency.getWeatherImplication()).containsIgnoringCase("no implication");
    }

    @Test
    @DisplayName("Complete group summary should not end with its METAR code - Issue #198")
    void testSummary_CompleteGroupHasNoCodeSuffix() {
        assertThat(PressureTendency.of(7, 4.5).getSummary())
                .isEqualTo("Pressure tendency: Falling (steadily or unsteadily), change of -4.5 hPa (0.13 inHg) [SIGNIFICANT]");
    }

    // ========== Derived Field Consistency Tests - Issue #218 ==========

    @Test
    @DisplayName("Code 8 derived fields should agree with each other - KDJT real-world (Issue #218)")
    void testCode8DerivedFields_KDJT() {
        // KDJT 171753Z ... 58021: code 8, change 2.1 hPa
        PressureTendency tendency = PressureTendency.fromMetarGroup("8", "021");

        assertThat(tendency.getTendencyDescription())
                .isEqualTo("Steady or rising, then falling; or falling, then falling more rapidly");
        assertThat(tendency.isFalling()).isTrue();
        assertThat(tendency.getSignedChange()).isEqualTo(-2.1);
        assertThat(tendency.getChangeMagnitude()).isEqualTo("Moderate");
        assertThat(tendency.isSignificant()).isFalse();
        assertThat(tendency.isRapidChange()).isFalse();
        assertThat(tendency.isNegligible()).isFalse();
        assertThat(tendency.getWeatherImplication())
                .isEqualTo("Moderate pressure fall - weather may deteriorate");
        assertThat(tendency.getSummary())
                .isEqualTo("Pressure tendency: Steady or rising, then falling; or falling, then falling more rapidly, "
                        + "change of -2.1 hPa (0.06 inHg)");
    }

    @ParameterizedTest
    @CsvSource({
            "0.0, None,        true,  false, false, 'Pressure steady'",
            "0.5, Slight,      true,  false, false, 'Slight pressure rise'",
            "0.9, Slight,      true,  false, false, 'Slight pressure rise'",
            "1.0, Moderate,    false, false, false, 'Moderate pressure rise'",
            "2.9, Moderate,    false, false, false, 'Moderate pressure rise'",
            "3.0, Significant, false, true,  false, 'Rising pressure'",
            "5.9, Significant, false, true,  false, 'Rising pressure'",
            "6.0, Rapid,       false, true,  true,  'Rapid pressure rise'"
    })
    @DisplayName("Derived fields should agree at every threshold boundary - Issue #218")
    void testDerivedFieldsAgreeAtThresholds(double change, String expectedMagnitude,
                                            boolean expectedNegligible, boolean expectedSignificant,
                                            boolean expectedRapid, String expectedImplicationStart) {
        PressureTendency tendency = PressureTendency.of(2, change);

        assertThat(tendency.getChangeMagnitude()).isEqualTo(expectedMagnitude);
        assertThat(tendency.isNegligible()).isEqualTo(expectedNegligible);
        assertThat(tendency.isSignificant()).isEqualTo(expectedSignificant);
        assertThat(tendency.isRapidChange()).isEqualTo(expectedRapid);
        assertThat(tendency.getWeatherImplication()).startsWith(expectedImplicationStart);
    }
}
