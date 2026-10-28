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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CompassDirection}: the shared compass direction vocabulary (#141).
 */
class CompassDirectionTest {

    // ==================== Constant set ====================

    @Test
    void testConstantsPinnedInClockwiseOrder() {
        // fromDegrees indexes values() in 22.5-degree steps, so order matters
        assertThat(Arrays.stream(CompassDirection.values()).map(Enum::name))
                .containsExactly("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW");
    }

    @Test
    void testPrincipalPointsPinned() {
        assertThat(Arrays.stream(CompassDirection.values())
                .filter(CompassDirection::isPrincipal)
                .map(Enum::name))
                .containsExactly("N", "NE", "E", "SE", "S", "SW", "W", "NW");
    }

    @Test
    void testPrincipalCodesDisplay() {
        assertThat(CompassDirection.principalCodesDisplay())
                .isEqualTo("N, NE, E, SE, S, SW, W, NW");
    }

    @ParameterizedTest
    @CsvSource({
            "N, North",
            "NNE, North-northeast",
            "NE, Northeast",
            "ENE, East-northeast",
            "E, East",
            "ESE, East-southeast",
            "SE, Southeast",
            "SSE, South-southeast",
            "S, South",
            "SSW, South-southwest",
            "SW, Southwest",
            "WSW, West-southwest",
            "W, West",
            "WNW, West-northwest",
            "NW, Northwest",
            "NNW, North-northwest"
    })
    void testDescriptions(String code, String description) {
        assertThat(CompassDirection.valueOf(code).getDescription()).isEqualTo(description);
    }

    // ==================== fromDegrees ====================

    @ParameterizedTest
    @CsvSource({
            "0, N",
            "22, NNE",
            "45, NE",
            "68, ENE",
            "90, E",
            "112, ESE",
            "135, SE",
            "158, SSE",
            "180, S",
            "202, SSW",
            "225, SW",
            "248, WSW",
            "270, W",
            "292, WNW",
            "315, NW",
            "338, NNW",
            "360, N"
    })
    void testFromDegrees_EachPoint(int degrees, CompassDirection expected) {
        assertThat(CompassDirection.fromDegrees(degrees)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            // Same rounding as the Wind/PeakWind arrays it replaces
            // (degrees / 22.5, rounded): the edges of NNE, NE and the wrap to N
            "11, N",
            "12, NNE",
            "33, NNE",
            "34, NE",
            "348, NNW",
            "349, N",
            "359, N"
    })
    void testFromDegrees_Boundaries(int degrees, CompassDirection expected) {
        assertThat(CompassDirection.fromDegrees(degrees)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 361, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void testFromDegrees_OutOfRangeIsAProgrammingError(int degrees) {
        assertThatThrownBy(() -> CompassDirection.fromDegrees(degrees))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Degrees must be between 0 and 360: " + degrees);
    }

    // ==================== find ====================

    @Test
    void testFind() {
        assertThat(CompassDirection.find("NE")).contains(CompassDirection.NE);
        assertThat(CompassDirection.find("ESE")).contains(CompassDirection.ESE);
        assertThat(CompassDirection.find("XX")).isEmpty();
        assertThat(CompassDirection.find("ne")).isEmpty();
        assertThat(CompassDirection.find(null)).isEmpty();
    }

    // ==================== isPrincipal(String) ====================

    @ParameterizedTest
    @ValueSource(strings = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"})
    void testIsPrincipal_AcceptsTheEightPoints(String code) {
        assertThat(CompassDirection.isPrincipal(code)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"NNE", "ENE", "ESE", "SSE", "SSW", "WSW", "WNW", "NNW"})
    void testIsPrincipal_RejectsInBetweenPoints(String code) {
        assertThat(CompassDirection.isPrincipal(code)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "ne", " N", "N ", "NORTH", "OHD", "ALQDS", "VC", "X"})
    void testIsPrincipal_RejectsUnknownOrUnnormalized(String code) {
        assertThat(CompassDirection.isPrincipal(code)).isFalse();
    }
}
