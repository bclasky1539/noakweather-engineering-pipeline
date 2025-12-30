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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * THUNDERSTORM LOCATION TESTS
 * ============================
 *
 * Tests for ThunderstormLocation record and parsing functionality.
 *
 * @author bclasky1539
 *
 */
class ThunderstormLocationTest {

    @Test
    @DisplayName("Should create simple thunderstorm location")
    void testOf() {
        ThunderstormLocation location = ThunderstormLocation.of("TS", "SE");

        assertThat(location.cloudType()).isEqualTo("TS");
        assertThat(location.direction()).isEqualTo("SE");
        assertThat(location.locationQualifier()).isNull();
        assertThat(location.directionRange()).isNull();
        assertThat(location.movingDirection()).isNull();
    }

    @Test
    @DisplayName("Should create thunderstorm location with movement")
    void testWithMovement() {
        ThunderstormLocation location = ThunderstormLocation.withMovement("CB", "W", "E");

        assertThat(location.cloudType()).isEqualTo("CB");
        assertThat(location.direction()).isEqualTo("W");
        assertThat(location.movingDirection()).isEqualTo("E");
        assertThat(location.isMoving()).isTrue();
    }

    @Test
    @DisplayName("Should create full thunderstorm location")
    void testFullLocation() {
        ThunderstormLocation location = new ThunderstormLocation(
                "TS", "VC", "N", "NE", "E"
        );

        assertThat(location.cloudType()).isEqualTo("TS");
        assertThat(location.locationQualifier()).isEqualTo("VC");
        assertThat(location.direction()).isEqualTo("N");
        assertThat(location.directionRange()).isEqualTo("NE");
        assertThat(location.movingDirection()).isEqualTo("E");

        assertThat(location.isThunderstorm()).isTrue();
        assertThat(location.hasDirectionRange()).isTrue();
        assertThat(location.hasLocationQualifier()).isTrue();
        assertThat(location.isMoving()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "TS, Thunderstorm",
            "CB, Cumulonimbus",
            "TCU, 'Towering Cumulus'",
            "ACC, 'Altocumulus Castellanus'",
            "CBMAM, 'Cumulonimbus Mammatus'",
            "VIRGA, Virga"
    })
    @DisplayName("Should generate correct summary for cloud types")
    void testGetSummary_CloudTypes(String cloudType, String expectedDescription) {
        ThunderstormLocation location = ThunderstormLocation.of(cloudType, "SE");

        assertThat(location.getSummary())
                .startsWith(expectedDescription)
                .contains("SE");
    }

    @Test
    @DisplayName("Should generate summary with location qualifier")
    void testGetSummary_WithQualifier() {
        ThunderstormLocation location = new ThunderstormLocation("TS", "OHD", null, null, null);

        assertThat(location.getSummary()).isEqualTo("Thunderstorm Overhead");
    }

    @Test
    @DisplayName("Should generate summary with direction range")
    void testGetSummary_WithRange() {
        ThunderstormLocation location = new ThunderstormLocation("CB", "DSNT", "N", "NE", null);

        assertThat(location.getSummary())
                .contains("Cumulonimbus")
                .contains("Distant")
                .contains("N-NE");
    }

    @Test
    @DisplayName("Should generate summary with movement")
    void testGetSummary_WithMovement() {
        ThunderstormLocation location = new ThunderstormLocation("TS", null, "SE", null, "E");

        assertThat(location.getSummary())
                .contains("Thunderstorm")
                .contains("SE")
                .contains("Moving E");
    }

    @Test
    @DisplayName("Should generate complete summary")
    void testGetSummary_Complete() {
        ThunderstormLocation location = new ThunderstormLocation("TCU", "VC", "W", "NW", "N");

        String summary = location.getSummary();
        assertThat(summary)
                .contains("Towering Cumulus")
                .contains("In vicinity")
                .contains("W-NW")
                .contains("Moving N");
    }

    @Test
    @DisplayName("Should identify thunderstorm correctly")
    void testIsThunderstorm() {
        assertThat(ThunderstormLocation.of("TS", "SE").isThunderstorm()).isTrue();
        assertThat(ThunderstormLocation.of("CB", "SE").isThunderstorm()).isFalse();
        assertThat(ThunderstormLocation.of("TCU", "SE").isThunderstorm()).isFalse();
    }

    @Test
    @DisplayName("Should identify movement correctly")
    void testIsMoving() {
        assertThat(ThunderstormLocation.withMovement("TS", "SE", "E").isMoving()).isTrue();
        assertThat(ThunderstormLocation.of("TS", "SE").isMoving()).isFalse();
    }
}
