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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * THUNDERSTORM LOCATION TESTS
 * ============================
 * <p>
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
        assertThat(location.directionSegments()).containsExactly(new DirectionSegment(List.of("SE")));
        assertThat(location.locationQualifier()).isNull();
        assertThat(location.movingDirection()).isNull();
    }

    @Test
    @DisplayName("Should create thunderstorm location with movement")
    void testWithMovement() {
        ThunderstormLocation location = ThunderstormLocation.withMovement("CB", "W", "E");

        assertThat(location.cloudType()).isEqualTo("CB");
        assertThat(location.directionSegments()).containsExactly(new DirectionSegment(List.of("W")));
        assertThat(location.movingDirection()).isEqualTo("E");
        assertThat(location.isMoving()).isTrue();
    }

    @Test
    @DisplayName("Should create full thunderstorm location with a two-point range")
    void testFullLocation() {
        ThunderstormLocation location = new ThunderstormLocation(
                "TS", "VC", List.of(new DirectionSegment(List.of("N", "NE"))), "E"
        );

        assertThat(location.cloudType()).isEqualTo("TS");
        assertThat(location.locationQualifier()).isEqualTo("VC");
        assertThat(location.directionSegments()).containsExactly(new DirectionSegment(List.of("N", "NE")));
        assertThat(location.movingDirection()).isEqualTo("E");

        assertThat(location.isThunderstorm()).isTrue();
        assertThat(location.hasDirections()).isTrue();
        assertThat(location.hasLocationQualifier()).isTrue();
        assertThat(location.isMoving()).isTrue();
    }

    @Test
    @DisplayName("Should handle no direction information")
    void testNoDirections() {
        ThunderstormLocation location = new ThunderstormLocation("TS", "OHD", null, null);

        assertThat(location.directionSegments()).isEmpty();
        assertThat(location.hasDirections()).isFalse();
        assertThat(location.getDirectionsSummary()).isEmpty();
    }

    @Test
    @DisplayName("Should defensively copy the direction segments list")
    void testImmutability_DefensiveCopy() {
        List<DirectionSegment> mutable = new java.util.ArrayList<>();
        mutable.add(new DirectionSegment(List.of("N")));

        ThunderstormLocation location = new ThunderstormLocation("TS", null, mutable, null);
        mutable.add(new DirectionSegment(List.of("S")));

        assertThat(location.directionSegments()).hasSize(1);
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
        ThunderstormLocation location = new ThunderstormLocation("TS", "OHD", null, null);

        assertThat(location.getSummary()).isEqualTo("Thunderstorm Overhead");
    }

    @Test
    @DisplayName("Should generate summary with a two-point direction range")
    void testGetSummary_WithRange() {
        ThunderstormLocation location = new ThunderstormLocation(
                "CB", "DSNT", List.of(new DirectionSegment(List.of("N", "NE"))), null
        );

        assertThat(location.getSummary())
                .contains("Cumulonimbus")
                .contains("Distant")
                .contains("N-NE");
    }

    @Test
    @DisplayName("Should generate summary with a three-point arc - KDFW real-world")
    void testGetSummary_WithThreePointArc() {
        ThunderstormLocation location = new ThunderstormLocation(
                "CB", "DSNT", List.of(new DirectionSegment(List.of("E", "S", "SW"))), null
        );

        assertThat(location.getSummary())
                .contains("Cumulonimbus")
                .contains("Distant")
                .contains("E-S-SW");
    }

    @Test
    @DisplayName("Should generate summary with AND-chained single directions - KMIA real-world")
    void testGetSummary_WithAndChainedSingleDirections() {
        ThunderstormLocation location = new ThunderstormLocation(
                "TCU", null,
                List.of(new DirectionSegment(List.of("N")), new DirectionSegment(List.of("SW"))),
                null
        );

        assertThat(location.getSummary())
                .contains("Towering Cumulus")
                .contains("N AND SW");
    }

    @Test
    @DisplayName("Should generate summary with AND-chained ranges - KPHX real-world")
    void testGetSummary_WithAndChainedRanges() {
        ThunderstormLocation location = new ThunderstormLocation(
                "CB", "DSNT",
                List.of(
                        new DirectionSegment(List.of("N", "E")),
                        new DirectionSegment(List.of("SE", "S"))
                ),
                null
        );

        assertThat(location.getSummary())
                .contains("Cumulonimbus")
                .contains("Distant")
                .contains("N-E AND SE-S");
    }

    @Test
    @DisplayName("Should generate summary with movement")
    void testGetSummary_WithMovement() {
        ThunderstormLocation location = new ThunderstormLocation(
                "TS", null, List.of(new DirectionSegment(List.of("SE"))), "E"
        );

        assertThat(location.getSummary())
                .contains("Thunderstorm")
                .contains("SE")
                .contains("Moving E");
    }

    @Test
    @DisplayName("Should generate complete summary")
    void testGetSummary_Complete() {
        ThunderstormLocation location = new ThunderstormLocation(
                "TCU", "VC", List.of(new DirectionSegment(List.of("W", "NW"))), "N"
        );

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
