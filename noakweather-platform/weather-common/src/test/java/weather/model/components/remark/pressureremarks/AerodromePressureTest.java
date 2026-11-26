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
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for AerodromePressure (QFE) - Issue #193.
 */
class AerodromePressureTest {

    @Test
    @DisplayName("Should create QFE reported in mmHg only")
    void testOf_MillimetersHgOnly() {
        AerodromePressure qfe = AerodromePressure.of(759);

        assertThat(qfe.millimetersHg()).isEqualTo(759.0);
        assertThat(qfe.hasReportedHectopascals()).isFalse();
        assertThat(qfe.toHectopascals()).isCloseTo(1011.9, within(0.1));
    }

    @Test
    @DisplayName("Should create QFE reported in mmHg and hPa")
    void testOf_BothUnits() {
        AerodromePressure qfe = AerodromePressure.of(752, 1003);

        assertThat(qfe.millimetersHg()).isEqualTo(752.0);
        assertThat(qfe.hectopascals()).isEqualTo(1003.0);
        assertThat(qfe.hasReportedHectopascals()).isTrue();
        assertThat(qfe.toHectopascals())
                .as("The reported hPa value is used, not the conversion")
                .isEqualTo(1003.0);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "759, NULL, 759.0,       , QFE759",
            "752, 1003, 752.0, 1003.0, QFE752/1003",
            "751, 1002, 751.0, 1002.0, QFE751/1002",
            "640,  853, 640.0,  853.0, QFE640/853"
    }, nullValues = "NULL")
    @DisplayName("fromMetarGroup should parse the digit groups and round-trip to METAR form")
    void testFromMetarGroup(String mmHgDigits, String hPaDigits, double expectedMmHg,
                            Double expectedHpa, String expectedCode) {
        AerodromePressure qfe = AerodromePressure.fromMetarGroup(mmHgDigits, hPaDigits);

        assertThat(qfe.millimetersHg()).isEqualTo(expectedMmHg);
        assertThat(qfe.hectopascals()).isEqualTo(expectedHpa);
        assertThat(qfe.toMetarCode()).isEqualTo(expectedCode);
    }

    @Test
    @DisplayName("Should accept a high-aerodrome QFE that Pressure would reject")
    void testHighAerodromeQfe() {
        // About 2,800 m elevation: below the 850 hPa sea-level minimum in Pressure
        AerodromePressure qfe = AerodromePressure.of(540);

        assertThat(qfe.toHectopascals()).isCloseTo(719.9, within(0.1));
    }

    @ParameterizedTest
    @ValueSource(doubles = {400.0, 820.0})
    @DisplayName("Should accept mmHg values at the range limits")
    void testRangeLimits(double mmHg) {
        assertThat(AerodromePressure.of(mmHg).millimetersHg()).isEqualTo(mmHg);
    }

    @ParameterizedTest
    @ValueSource(doubles = {399.0, 821.0, 100.0})
    @DisplayName("Should reject mmHg values outside the range")
    void testOutOfRange(double mmHg) {
        assertThatThrownBy(() -> AerodromePressure.of(mmHg))
                .isInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("QFE out of reasonable range (400-820 mmHg)");
    }

    @ParameterizedTest
    @CsvSource({
            "752, 1010, 'hPa too high'",
            "752,  998, 'hPa too low'",
            "752,  100, 'garbled hPa'"
    })
    @DisplayName("Should reject an hPa value that disagrees with the mmHg value")
    void testUnitDisagreement(double mmHg, double hPa, String scenario) {
        assertThatThrownBy(() -> AerodromePressure.of(mmHg, hPa))
                .as(scenario)
                .isInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("disagrees with 752 mmHg");
    }

    @Test
    @DisplayName("Should accept rounding differences up to the tolerance")
    void testUnitAgreementWithinTolerance() {
        // 752 mmHg converts to 1002.58 hPa; 1004 is 1.42 hPa away
        assertThat(AerodromePressure.of(752, 1004).hectopascals()).isEqualTo(1004.0);
    }

    @Test
    @DisplayName("Should reject a null mmHg value")
    void testNullMillimetersHg() {
        assertThatThrownBy(() -> new AerodromePressure(null, 1003.0))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("QFE millimetersHg cannot be null");
    }

    @Test
    @DisplayName("Summary should show mmHg and hPa")
    void testGetSummary() {
        assertThat(AerodromePressure.of(752, 1003).getSummary()).isEqualTo("QFE 752 mmHg (1003 hPa)");
        assertThat(AerodromePressure.of(759).getSummary()).isEqualTo("QFE 759 mmHg (1012 hPa)");
    }
}
