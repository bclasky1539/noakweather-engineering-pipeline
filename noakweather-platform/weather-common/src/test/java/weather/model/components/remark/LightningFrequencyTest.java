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
import weather.exception.InvalidObservationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for LightningFrequency enum.
 *
 * @author bclasky1539
 *
 */
class LightningFrequencyTest {

    @ParameterizedTest
    @CsvSource({
            "OCNL, OCCASIONAL",
            "FRQ, FREQUENT",
            "CONS, CONTINUOUS",
            "CONTUS, CONTINUOUS"
    })
    @DisplayName("Should parse all valid frequency codes, normalizing CONS/CONTUS to the same value")
    void testFromCode_ValidCodes(String code, LightningFrequency expected) {
        assertThat(LightningFrequency.fromCode(code)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should throw for null code")
    void testFromCode_Null() {
        assertThatThrownBy(() -> LightningFrequency.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Should throw for invalid code")
    void testFromCode_Invalid() {
        assertThatThrownBy(() -> LightningFrequency.fromCode("XXX"))
                .isInstanceOf(InvalidObservationException.class)
                .hasMessage("Invalid lightning frequency code (valid codes: OCNL, FRQ, CONS, CONTUS): XXX")
                .hasFieldOrPropertyWithValue("component", "LightningFrequency.code")
                .hasFieldOrPropertyWithValue("rejectedValue", "XXX");
    }

    @ParameterizedTest
    @CsvSource({
            "OCCASIONAL, Occasional",
            "FREQUENT, Frequent",
            "CONTINUOUS, Continuous"
    })
    @DisplayName("Should generate correct description for each value")
    void testGetDescription(LightningFrequency frequency, String expectedDescription) {
        assertThat(frequency.getDescription()).isEqualTo(expectedDescription);
    }

    @Test
    @DisplayName("CONS and CONTUS should both normalize to CONTINUOUS")
    void testConsAndContusNormalizeToSameValue() {
        assertThat(LightningFrequency.fromCode("CONS"))
                .isEqualTo(LightningFrequency.fromCode("CONTUS"));
    }
}
