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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for the runway state group code enums: RunwayDeposit,
 * RunwayContaminationExtent, DepthOfDeposit and FrictionCoefficient.
 */
class RunwayStateEnumsTest {

    // ========== RunwayDeposit ==========

    @ParameterizedTest
    @EnumSource(RunwayDeposit.class)
    @DisplayName("RunwayDeposit should round-trip every code")
    void testRunwayDepositRoundTrip(RunwayDeposit deposit) {
        assertThat(RunwayDeposit.fromCode(deposit.getCode())).isEqualTo(deposit);
        assertThat(deposit.getSummary()).isNotBlank();
    }

    @Test
    @DisplayName("RunwayDeposit should map / to NOT_REPORTED")
    void testRunwayDepositNotReported() {
        assertThat(RunwayDeposit.fromCode("/")).isEqualTo(RunwayDeposit.NOT_REPORTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"X", "10", "//", ""})
    @DisplayName("RunwayDeposit should reject undefined codes")
    void testRunwayDepositInvalid(String code) {
        assertThatThrownBy(() -> RunwayDeposit.fromCode(code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid runway deposit code");
    }

    // ========== RunwayContaminationExtent ==========

    @ParameterizedTest
    @EnumSource(RunwayContaminationExtent.class)
    @DisplayName("RunwayContaminationExtent should round-trip every code")
    void testExtentRoundTrip(RunwayContaminationExtent extent) {
        assertThat(RunwayContaminationExtent.fromCode(extent.getCode())).isEqualTo(extent);
        assertThat(extent.getSummary()).isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "3", "4", "6", "7", "8", "//"})
    @DisplayName("RunwayContaminationExtent should reject undefined codes")
    void testExtentInvalid(String code) {
        assertThatThrownBy(() -> RunwayContaminationExtent.fromCode(code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid runway contamination extent code");
    }

    // ========== DepthOfDeposit ==========

    @ParameterizedTest
    @EnumSource(value = DepthOfDeposit.class, mode = EnumSource.Mode.EXCLUDE, names = "MILLIMETRES")
    @DisplayName("DepthOfDeposit should round-trip every single-code constant")
    void testDepthRoundTrip(DepthOfDeposit depth) {
        assertThat(DepthOfDeposit.fromCode(depth.getCode())).isEqualTo(depth);
        assertThat(depth.getSummary()).isNotBlank();
    }

    @ParameterizedTest(name = "{0} -> {1} / {2} mm")
    @CsvSource({
            "00, LESS_THAN_1_MM,         0",
            "01, MILLIMETRES,            1",
            "45, MILLIMETRES,            45",
            "90, MILLIMETRES,            90",
            "92, CM_10,                  100",
            "93, CM_15,                  150",
            "94, CM_20,                  200",
            "95, CM_25,                  250",
            "96, CM_30,                  300",
            "97, CM_35,                  350",
            "98, CM_40_OR_MORE,          400",
            "99, RUNWAY_NOT_OPERATIONAL,    ",
            "//, NOT_SIGNIFICANT,           "
    })
    @DisplayName("DepthOfDeposit should decode codes and convert to millimetres")
    void testDepthDecode(String code, DepthOfDeposit expected, Integer expectedMm) {
        DepthOfDeposit depth = DepthOfDeposit.fromCode(code);

        assertThat(depth).isEqualTo(expected);
        assertThat(depth.toMillimetres(code)).isEqualTo(expectedMm);
        assertThat(depth.hasDepthValue()).isEqualTo(expectedMm != null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"91", "/", "1", "100", "AB", "9/", ""})
    @DisplayName("DepthOfDeposit should reject undefined codes including 91 (not used)")
    void testDepthInvalid(String code) {
        assertThatThrownBy(() -> DepthOfDeposit.fromCode(code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid runway deposit depth code");
    }

    @Test
    @DisplayName("DepthOfDeposit should reject null")
    void testDepthNull() {
        assertThatThrownBy(() -> DepthOfDeposit.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("DepthOfDeposit should flag only 40 cm or more as a lower bound")
    void testDepthLowerBound() {
        assertThat(DepthOfDeposit.CM_40_OR_MORE.isLowerBound()).isTrue();
        assertThat(DepthOfDeposit.CM_35.isLowerBound()).isFalse();
        assertThat(DepthOfDeposit.MILLIMETRES.isLowerBound()).isFalse();
    }

    @Test
    @DisplayName("DepthOfDeposit should expose fixed millimetres only for fixed codes")
    void testDepthFixedMillimetres() {
        assertThat(DepthOfDeposit.CM_25.getFixedMillimetres()).isEqualTo(250);
        assertThat(DepthOfDeposit.LESS_THAN_1_MM.getFixedMillimetres()).isZero();
        assertThat(DepthOfDeposit.MILLIMETRES.getFixedMillimetres()).isNull();
        assertThat(DepthOfDeposit.NOT_SIGNIFICANT.getFixedMillimetres()).isNull();
        assertThat(DepthOfDeposit.MILLIMETRES.getCode()).isEqualTo("01-90");
    }

    // ========== FrictionCoefficient ==========

    @ParameterizedTest
    @EnumSource(value = FrictionCoefficient.class, mode = EnumSource.Mode.EXCLUDE, names = "MEASURED")
    @DisplayName("FrictionCoefficient should round-trip every single-code constant")
    void testFrictionRoundTrip(FrictionCoefficient friction) {
        assertThat(FrictionCoefficient.fromCode(friction.getCode())).isEqualTo(friction);
        assertThat(friction.getSummary()).isNotBlank();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "01, MEASURED,            0.01",
            "28, MEASURED,            0.28",
            "90, MEASURED,            0.90",
            "91, BRAKING_POOR,            ",
            "92, BRAKING_MEDIUM_POOR,     ",
            "93, BRAKING_MEDIUM,          ",
            "94, BRAKING_MEDIUM_GOOD,     ",
            "95, BRAKING_GOOD,            ",
            "99, UNRELIABLE,              ",
            "//, NOT_REPORTED,            "
    })
    @DisplayName("FrictionCoefficient should decode codes and convert measured values")
    void testFrictionDecode(String code, FrictionCoefficient expected, Double expectedCoefficient) {
        FrictionCoefficient friction = FrictionCoefficient.fromCode(code);

        assertThat(friction).isEqualTo(expected);
        if (expectedCoefficient == null) {
            assertThat(friction.toCoefficient(code)).isNull();
        } else {
            assertThat(friction.toCoefficient(code)).isCloseTo(expectedCoefficient, within(0.001));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"00", "96", "97", "98", "/", "5", "100", "AB", ""})
    @DisplayName("FrictionCoefficient should reject undefined codes")
    void testFrictionInvalid(String code) {
        assertThatThrownBy(() -> FrictionCoefficient.fromCode(code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid runway friction code");
    }

    @Test
    @DisplayName("FrictionCoefficient should reject null")
    void testFrictionNull() {
        assertThatThrownBy(() -> FrictionCoefficient.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(value = FrictionCoefficient.class,
            names = {"BRAKING_POOR", "BRAKING_MEDIUM_POOR", "BRAKING_MEDIUM", "BRAKING_MEDIUM_GOOD", "BRAKING_GOOD"})
    @DisplayName("FrictionCoefficient should identify braking action constants")
    void testIsBrakingAction(FrictionCoefficient friction) {
        assertThat(friction.isBrakingAction()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = FrictionCoefficient.class, names = {"MEASURED", "UNRELIABLE", "NOT_REPORTED"})
    @DisplayName("FrictionCoefficient should not treat measured, unreliable or not reported as braking action")
    void testIsNotBrakingAction(FrictionCoefficient friction) {
        assertThat(friction.isBrakingAction()).isFalse();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "0.01, BRAKING_POOR",
            "0.25, BRAKING_POOR",
            "0.26, BRAKING_MEDIUM_POOR",
            "0.29, BRAKING_MEDIUM_POOR",
            "0.30, BRAKING_MEDIUM",
            "0.35, BRAKING_MEDIUM",
            "0.36, BRAKING_MEDIUM_GOOD",
            "0.39, BRAKING_MEDIUM_GOOD",
            "0.40, BRAKING_GOOD",
            "0.90, BRAKING_GOOD"
    })
    @DisplayName("FrictionCoefficient should map measured coefficients to braking action bands")
    void testEquivalentBrakingAction(double coefficient, FrictionCoefficient expected) {
        assertThat(FrictionCoefficient.equivalentBrakingAction(coefficient)).isEqualTo(expected);
    }
}
