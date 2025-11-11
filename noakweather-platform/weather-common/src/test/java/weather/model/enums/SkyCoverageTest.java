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
package weather.model.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for SkyCoverage enum.
 * 
 * @author bclasky1539
 *
 */
class SkyCoverageTest {
    
    @ParameterizedTest
    @CsvSource({
        "SKC, 0",
        "CLR, 0",
        "NSC, 0",
        "FEW, 1",
        "SCT, 3",
        "BKN, 5",
        "OVC, 8",
        "VV, 8"
    })
    void testOktasValues(String code, int expectedOktas) {
        SkyCoverage coverage = SkyCoverage.fromCode(code);
        assertThat(coverage.getOktas()).isEqualTo(expectedOktas);
    }
    
    @Test
    void testFromCode_ValidCodes() {
        assertThat(SkyCoverage.fromCode("SKC")).isEqualTo(SkyCoverage.SKC);
        assertThat(SkyCoverage.fromCode("CLR")).isEqualTo(SkyCoverage.CLR);
        assertThat(SkyCoverage.fromCode("FEW")).isEqualTo(SkyCoverage.FEW);
        assertThat(SkyCoverage.fromCode("SCT")).isEqualTo(SkyCoverage.SCATTERED);
        assertThat(SkyCoverage.fromCode("BKN")).isEqualTo(SkyCoverage.BROKEN);
        assertThat(SkyCoverage.fromCode("OVC")).isEqualTo(SkyCoverage.OVERCAST);
        assertThat(SkyCoverage.fromCode("VV")).isEqualTo(SkyCoverage.VERTICAL_VISIBILITY);
    }
    
    @Test
    void testFromCode_InvalidCode() {
        assertThatThrownBy(() -> SkyCoverage.fromCode("INVALID"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown sky coverage code: INVALID");
    }
    
    @Test
    void testGetCode() {
        assertThat(SkyCoverage.FEW.getCode()).isEqualTo("FEW");
        assertThat(SkyCoverage.SCATTERED.getCode()).isEqualTo("SCT");
        assertThat(SkyCoverage.BROKEN.getCode()).isEqualTo("BKN");
        assertThat(SkyCoverage.OVERCAST.getCode()).isEqualTo("OVC");
    }
    
    @Test
    void testClearSkyOktas() {
        assertThat(SkyCoverage.SKC.getOktas()).isZero();
        assertThat(SkyCoverage.CLR.getOktas()).isZero();
        assertThat(SkyCoverage.NSC.getOktas()).isZero();
    }
    
    @Test
    void testFullCoverageOktas() {
        assertThat(SkyCoverage.OVERCAST.getOktas()).isEqualTo(8);
        assertThat(SkyCoverage.VERTICAL_VISIBILITY.getOktas()).isEqualTo(8);
    }
}
