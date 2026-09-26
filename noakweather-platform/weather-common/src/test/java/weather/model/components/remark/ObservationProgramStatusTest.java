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
package weather.model.components.remark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ObservationProgramStatus value object.
 *
 * @author bclasky1539
 *
 */
class ObservationProgramStatusTest {

    // ==================== Construction Tests ====================

    @Test
    @DisplayName("Should construct a staffed status")
    void testConstructor_Staffed() {
        ObservationProgramStatus status = new ObservationProgramStatus(true, 26, 12, 0);

        assertThat(status.staffed()).isTrue();
        assertThat(status.day()).isEqualTo(26);
        assertThat(status.hour()).isEqualTo(12);
        assertThat(status.minute()).isZero();
    }

    @Test
    @DisplayName("Should construct a non-staffed status")
    void testConstructor_NotStaffed() {
        ObservationProgramStatus status = new ObservationProgramStatus(false, 10, 13, 0);

        assertThat(status.staffed()).isFalse();
        assertThat(status.day()).isEqualTo(10);
        assertThat(status.hour()).isEqualTo(13);
        assertThat(status.minute()).isZero();
    }

    // ==================== Factory Method Tests ====================

    @Test
    @DisplayName("Should create via of() factory method")
    void testOf() {
        ObservationProgramStatus status = ObservationProgramStatus.of(true, 27, 12, 0);

        assertThat(status.staffed()).isTrue();
        assertThat(status.day()).isEqualTo(27);
        assertThat(status.hour()).isEqualTo(12);
        assertThat(status.minute()).isZero();
    }

    // ==================== getSummary() Tests ====================

    @Test
    @DisplayName("Should summarize a staffed status")
    void testGetSummary_Staffed() {
        ObservationProgramStatus status = ObservationProgramStatus.of(true, 26, 12, 0);

        assertThat(status.getSummary()).isEqualTo("Staffed, next observation day 26 at 12:00 UTC");
    }

    @Test
    @DisplayName("Should summarize a non-staffed status")
    void testGetSummary_NotStaffed() {
        ObservationProgramStatus status = ObservationProgramStatus.of(false, 10, 13, 0);

        assertThat(status.getSummary()).isEqualTo("Not staffed, next observation day 10 at 13:00 UTC");
    }

    @ParameterizedTest
    @CsvSource({
            "1, 0, 0, '01', '00', '00'",
            "31, 23, 59, '31', '23', '59'"
    })
    @DisplayName("Should zero-pad day/hour/minute in summary")
    void testGetSummary_ZeroPadding(int day, int hour, int minute,
                                    String expectedDay, String expectedHour, String expectedMinute) {
        ObservationProgramStatus status = ObservationProgramStatus.of(true, day, hour, minute);

        assertThat(status.getSummary())
                .contains("day " + expectedDay)
                .contains(expectedHour + ":" + expectedMinute);
    }

    // ==================== Record Equality Tests ====================

    @Test
    @DisplayName("Should be equal when all fields match")
    void testEquality_SameFields() {
        ObservationProgramStatus status1 = ObservationProgramStatus.of(true, 26, 12, 0);
        ObservationProgramStatus status2 = ObservationProgramStatus.of(true, 26, 12, 0);

        assertThat(status1).isEqualTo(status2).hasSameHashCodeAs(status2);
    }

    @Test
    @DisplayName("Should not be equal when staffed flag differs")
    void testInequality_DifferentStaffed() {
        ObservationProgramStatus status1 = ObservationProgramStatus.of(true, 26, 12, 0);
        ObservationProgramStatus status2 = ObservationProgramStatus.of(false, 26, 12, 0);

        assertThat(status1).isNotEqualTo(status2);
    }

    @Test
    @DisplayName("Should not be equal when time differs")
    void testInequality_DifferentTime() {
        ObservationProgramStatus status1 = ObservationProgramStatus.of(true, 26, 12, 0);
        ObservationProgramStatus status2 = ObservationProgramStatus.of(true, 27, 12, 0);

        assertThat(status1).isNotEqualTo(status2);
    }
}
