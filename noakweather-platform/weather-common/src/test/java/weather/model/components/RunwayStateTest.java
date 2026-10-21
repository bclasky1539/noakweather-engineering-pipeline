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
package weather.model.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import weather.exception.InvalidObservationException;
import weather.model.enums.DepthOfDeposit;
import weather.model.enums.FrictionCoefficient;
import weather.model.enums.RunwayContaminationExtent;
import weather.model.enums.RunwayDeposit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for RunwayState: decoding of runway state group code fields, the
 * cleared and not-updated special forms, and constructor validation.
 */
class RunwayStateTest {

    // ========== DECODING FROM CODES ==========

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            // label, runway, deposit, extent, depth, friction, expected deposit, extent, depth, depthMm, friction, frictionValue
            "'UAAA R23R/1///60', 23R, 1, /, //, 60, DAMP,                     NOT_REPORTED,      NOT_SIGNIFICANT,        ,    MEASURED,            0.60",
            "'UAAA R23L/2/0150', 23L, 2, /, 01, 50, WET_OR_WATER_PATCHES,     NOT_REPORTED,      MILLIMETRES,            1,   MEASURED,            0.50",
            "'UHHH R05R/090060', 05R, 0, 9, 00, 60, CLEAR_AND_DRY,            PERCENT_51_TO_100, LESS_THAN_1_MM,         0,   MEASURED,            0.60",
            "'R27/451291',       27,  4, 5, 12, 91, DRY_SNOW,                 PERCENT_26_TO_50,  MILLIMETRES,            12,  BRAKING_POOR,            ",
            "'R09/899299',       09,  8, 9, 92, 99, COMPACTED_OR_ROLLED_SNOW, PERCENT_51_TO_100, CM_10,                  100, UNRELIABLE,              ",
            "'R10/5998//',       10,  5, 9, 98, //, WET_SNOW,                 PERCENT_51_TO_100, CM_40_OR_MORE,          400, NOT_REPORTED,            ",
            "'R10///99//',       10,  /, /, 99, //, NOT_REPORTED,             NOT_REPORTED,      RUNWAY_NOT_OPERATIONAL, ,    NOT_REPORTED,            ",
            "'R18/621095',       18,  6, 2, 10, 95, SLUSH,                    PERCENT_11_TO_25,  MILLIMETRES,            10,  BRAKING_GOOD,            ",
            "'R36/719090',       36,  7, 1, 90, 90, ICE,                      UP_TO_10_PERCENT,  MILLIMETRES,            90,  MEASURED,            0.90",
            "'R01/3197//',       01,  3, 1, 97, //, RIME_OR_FROST,            UP_TO_10_PERCENT,  CM_35,                  350, NOT_REPORTED,            ",
            "'R02/990001',       02,  9, 9, 00, 01, FROZEN_RUTS_OR_RIDGES,    PERCENT_51_TO_100, LESS_THAN_1_MM,         0,   MEASURED,            0.01"
    })
    @DisplayName("Should decode runway state code fields")
    void testFromCodes(String label, String runway, String depositCode, String extentCode,
                       String depthCode, String frictionCode,
                       RunwayDeposit expectedDeposit, RunwayContaminationExtent expectedExtent,
                       DepthOfDeposit expectedDepth, Integer expectedDepthMm,
                       FrictionCoefficient expectedFriction, Double expectedFrictionValue) {
        RunwayState state = RunwayState.fromCodes(runway, depositCode, extentCode, depthCode, frictionCode);

        assertThat(state.runway()).isEqualTo(runway);
        assertThat(state.deposit()).isEqualTo(expectedDeposit);
        assertThat(state.extent()).isEqualTo(expectedExtent);
        assertThat(state.depth()).isEqualTo(expectedDepth);
        assertThat(state.depthMm()).isEqualTo(expectedDepthMm);
        assertThat(state.friction()).isEqualTo(expectedFriction);
        if (expectedFrictionValue == null) {
            assertThat(state.frictionValue()).isNull();
        } else {
            assertThat(state.frictionValue()).isCloseTo(expectedFrictionValue, within(0.001));
        }
        assertThat(state.cleared()).isFalse();
        assertThat(state.reportNotUpdated()).isFalse();
    }

    @Test
    @DisplayName("Should decode six slashes as report not updated")
    void testFromCodesAllSlashesIsNotUpdated() {
        RunwayState state = RunwayState.fromCodes("24", "/", "/", "//", "//");

        assertThat(state.reportNotUpdated()).isTrue();
        assertThat(state.cleared()).isFalse();
        assertThat(state.deposit()).isNull();
        assertThat(state.extent()).isNull();
        assertThat(state.depth()).isNull();
        assertThat(state.depthMm()).isNull();
        assertThat(state.friction()).isNull();
        assertThat(state.frictionValue()).isNull();
        assertThat(state).isEqualTo(RunwayState.notUpdated("24"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "'deposit not a code',  X, 1, 10, 50",
            "'extent 3 undefined',  1, 3, 10, 50",
            "'depth 91 not used',   1, 1, 91, 50",
            "'depth not numeric',   1, 1, 1X, 50",
            "'friction 00',         1, 1, 10, 00",
            "'friction 96',         1, 1, 10, 96",
            "'friction 98',         1, 1, 10, 98",
            "'friction single /',   1, 1, 10, /"
    })
    @DisplayName("Should reject undefined runway state codes")
    void testFromCodesRejectsUndefinedCodes(String label, String depositCode, String extentCode,
                                            String depthCode, String frictionCode) {
        assertThatThrownBy(() -> RunwayState.fromCodes("10", depositCode, extentCode, depthCode, frictionCode))
                .isInstanceOf(InvalidObservationException.class);
    }

    // ========== CLEARED (CLRD) ==========

    @ParameterizedTest(name = "CLRD{1}")
    @CsvSource({
            "34L, 70, MEASURED,       0.70",
            "24,  93, BRAKING_MEDIUM,     ",
            "22R, //, NOT_REPORTED,       ",
            "05,  99, UNRELIABLE,         "
    })
    @DisplayName("Should decode cleared runway state with its friction")
    void testClearedFromCode(String runway, String frictionCode,
                             FrictionCoefficient expectedFriction, Double expectedFrictionValue) {
        RunwayState state = RunwayState.clearedFromCode(runway, frictionCode);

        assertThat(state.cleared()).isTrue();
        assertThat(state.reportNotUpdated()).isFalse();
        assertThat(state.deposit()).isNull();
        assertThat(state.extent()).isNull();
        assertThat(state.depth()).isNull();
        assertThat(state.depthMm()).isNull();
        assertThat(state.friction()).isEqualTo(expectedFriction);
        if (expectedFrictionValue == null) {
            assertThat(state.frictionValue()).isNull();
        } else {
            assertThat(state.frictionValue()).isCloseTo(expectedFrictionValue, within(0.001));
        }
    }

    // ========== EQUIVALENT BRAKING ACTION ==========

    @ParameterizedTest(name = "friction {0} -> {1}")
    @CsvSource({
            "01, BRAKING_POOR",
            "25, BRAKING_POOR",
            "26, BRAKING_MEDIUM_POOR",
            "29, BRAKING_MEDIUM_POOR",
            "30, BRAKING_MEDIUM",
            "35, BRAKING_MEDIUM",
            "36, BRAKING_MEDIUM_GOOD",
            "39, BRAKING_MEDIUM_GOOD",
            "40, BRAKING_GOOD",
            "90, BRAKING_GOOD",
            "91, BRAKING_POOR",
            "94, BRAKING_MEDIUM_GOOD",
            "99, ",
            "//, "
    })
    @DisplayName("Should map friction to equivalent braking action")
    void testEquivalentBrakingAction(String frictionCode, FrictionCoefficient expected) {
        RunwayState state = RunwayState.fromCodes("10", "1", "9", "00", frictionCode);

        assertThat(state.getEquivalentBrakingAction()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should have no equivalent braking action when report not updated")
    void testEquivalentBrakingActionNotUpdated() {
        assertThat(RunwayState.notUpdated("10").getEquivalentBrakingAction()).isNull();
    }

    // ========== SUMMARY ==========

    @Test
    @DisplayName("Should summarise a full runway state report")
    void testSummaryFullReport() {
        RunwayState state = RunwayState.fromCodes("27", "4", "5", "12", "91");

        assertThat(state.getSummary())
                .isEqualTo("Runway 27: Dry snow; 26% to 50% of runway covered; Depth 12 mm; Braking action poor");
    }

    @Test
    @DisplayName("Should summarise fixed depth and measured friction")
    void testSummaryFixedDepthMeasuredFriction() {
        RunwayState state = RunwayState.fromCodes("05R", "0", "9", "00", "60");

        assertThat(state.getSummary())
                .isEqualTo("Runway 05R: Clear and dry; 51% to 100% of runway covered; "
                        + "Depth less than 1 mm; Friction coefficient 0.60");
    }

    @Test
    @DisplayName("Should summarise a cleared runway state")
    void testSummaryCleared() {
        assertThat(RunwayState.clearedFromCode("34L", "70").getSummary())
                .isEqualTo("Runway 34L: Contamination cleared; Friction coefficient 0.70");
        assertThat(RunwayState.clearedFromCode("24", "93").getSummary())
                .isEqualTo("Runway 24: Contamination cleared; Braking action medium");
    }

    @Test
    @DisplayName("Should summarise a not-updated runway state")
    void testSummaryNotUpdated() {
        assertThat(RunwayState.notUpdated("24").getSummary())
                .isEqualTo("Runway 24: Report not updated");
    }

    // ========== VALIDATION ==========

    @ParameterizedTest
    @CsvSource(value = {"NULL", "''", "'  '"}, nullValues = "NULL")
    @DisplayName("Should reject null or blank runway identifiers")
    void testNullOrBlankRunway(String runway) {
        assertThatThrownBy(() -> RunwayState.fromCodes(runway, "1", "9", "00", "60"))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("Runway identifier cannot be null or blank");
    }

    @ParameterizedTest
    @CsvSource({"23X", "R23", "123"})
    @DisplayName("Should reject badly formatted runway identifiers")
    void testInvalidRunwayFormat(String runway) {
        assertThatThrownBy(() -> RunwayState.fromCodes(runway, "1", "9", "00", "60"))
                .isInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("Invalid runway identifier format")
                .hasFieldOrPropertyWithValue("component", "RunwayState.runway")
                .hasFieldOrPropertyWithValue("rejectedValue", runway);
    }

    @Test
    @DisplayName("Should reject a report that is both cleared and not updated")
    void testClearedAndNotUpdated() {
        assertThatThrownBy(() -> new RunwayState("10", null, null, null, null, null, null, true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("both cleared and not updated");
    }

    @Test
    @DisplayName("Should reject a not-updated report that carries values")
    void testNotUpdatedWithValues() {
        assertThatThrownBy(() -> new RunwayState("10", RunwayDeposit.DAMP, null, null, null,
                null, null, false, true))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("cannot have any values");
    }

    @Test
    @DisplayName("Should reject a cleared report that carries deposit values")
    void testClearedWithDeposit() {
        assertThatThrownBy(() -> new RunwayState("10", RunwayDeposit.DAMP, null, null, null,
                FrictionCoefficient.NOT_REPORTED, null, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("Cleared runway state");
    }

    @Test
    @DisplayName("Should reject a cleared report without friction")
    void testClearedWithoutFriction() {
        assertThatThrownBy(() -> new RunwayState("10", null, null, null, null, null, null, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("Friction is required");
    }

    @Test
    @DisplayName("Should reject a normal report missing a field")
    void testReportedMissingField() {
        assertThatThrownBy(() -> new RunwayState("10", null, RunwayContaminationExtent.NOT_REPORTED,
                DepthOfDeposit.NOT_SIGNIFICANT, null, FrictionCoefficient.NOT_REPORTED, null, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("requires deposit, extent, depth and friction");
    }

    @ParameterizedTest(name = "{0} with {1} mm")
    @CsvSource({
            "NOT_SIGNIFICANT,        5",
            "RUNWAY_NOT_OPERATIONAL, 5",
            "MILLIMETRES,             ",
            "MILLIMETRES,            0",
            "MILLIMETRES,            91",
            "CM_10,                   ",
            "CM_10,                  150",
            "LESS_THAN_1_MM,         1"
    })
    @DisplayName("Should reject a depth value inconsistent with the depth code")
    void testInconsistentDepth(DepthOfDeposit depth, Integer depthMm) {
        assertThatThrownBy(() -> new RunwayState("10", RunwayDeposit.WET_SNOW,
                RunwayContaminationExtent.PERCENT_51_TO_100, depth, depthMm,
                FrictionCoefficient.NOT_REPORTED, null, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class);
    }

    @ParameterizedTest(name = "{0} with value {1}")
    @CsvSource({
            "MEASURED,       ",
            "MEASURED,       0.0",
            "MEASURED,       0.95",
            "BRAKING_GOOD,   0.50",
            "UNRELIABLE,     0.50",
            "NOT_REPORTED,   0.50"
    })
    @DisplayName("Should reject a friction value inconsistent with the friction code")
    void testInconsistentFriction(FrictionCoefficient friction, Double frictionValue) {
        assertThatThrownBy(() -> new RunwayState("10", RunwayDeposit.WET_SNOW,
                RunwayContaminationExtent.PERCENT_51_TO_100, DepthOfDeposit.MILLIMETRES, 5,
                friction, frictionValue, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class);
    }

    @Test
    @DisplayName("Should treat equal runway states as equal")
    void testEquality() {
        assertThat(RunwayState.fromCodes("23R", "1", "/", "//", "60"))
                .isEqualTo(RunwayState.fromCodes("23R", "1", "/", "//", "60"))
                .hasSameHashCodeAs(RunwayState.fromCodes("23R", "1", "/", "//", "60"))
                .isNotEqualTo(RunwayState.fromCodes("23L", "1", "/", "//", "60"));
    }
}
