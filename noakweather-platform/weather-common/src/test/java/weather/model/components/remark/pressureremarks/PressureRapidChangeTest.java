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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import weather.exception.InvalidObservationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PressureRapidChange}.
 */
class PressureRapidChangeTest {

    @ParameterizedTest
    @CsvSource({
            "R, RISING",
            "F, FALLING"
    })
    void ofParsesValidCodes(String code, PressureRapidChange expected) {
        assertThat(PressureRapidChange.of(code)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "R, RISING",
            "F, FALLING"
    })
    void fromCodeParsesValidCodes(String code, PressureRapidChange expected) {
        assertThat(PressureRapidChange.fromCode(code)).isEqualTo(expected);
    }

    @Test
    void ofRejectsInvalidCode() {
        assertThatThrownBy(() -> PressureRapidChange.of("X"))
                .isInstanceOf(InvalidObservationException.class)
                .hasMessage("Invalid pressure rapid change code (valid codes: R, F): X")
                .hasFieldOrPropertyWithValue("component", "PressureRapidChange.code")
                .hasFieldOrPropertyWithValue("rejectedValue", "X");
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void ofRejectsNullCode() {
        assertThatThrownBy(() -> PressureRapidChange.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("cannot be null");
    }

    @ParameterizedTest
    @CsvSource({
            "RISING, Rising rapidly",
            "FALLING, Falling rapidly"
    })
    void getSummaryReturnsHumanReadableText(PressureRapidChange value, String expectedSummary) {
        assertThat(value.getSummary()).isEqualTo(expectedSummary);
    }

    @Test
    void hasExactlyTwoValues() {
        assertThat(PressureRapidChange.values()).containsExactly(
                PressureRapidChange.RISING,
                PressureRapidChange.FALLING
        );
    }
}
