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
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for DirectionSegment value object.
 *
 * @author bclasky1539
 *
 */
class DirectionSegmentTest {

    // ==================== Constructor and Validation Tests ====================

    @Test
    @DisplayName("Should construct a single-point segment")
    void testConstructor_SinglePoint() {
        DirectionSegment segment = new DirectionSegment(List.of("N"));

        assertThat(segment.points()).containsExactly("N");
    }

    @Test
    @DisplayName("Should construct a two-point range segment")
    void testConstructor_TwoPointRange() {
        DirectionSegment segment = new DirectionSegment(List.of("N", "E"));

        assertThat(segment.points()).containsExactly("N", "E");
    }

    @Test
    @DisplayName("Should construct a three-point arc segment")
    void testConstructor_ThreePointArc() {
        DirectionSegment segment = new DirectionSegment(List.of("E", "S", "SW"));

        assertThat(segment.points()).containsExactly("E", "S", "SW");
    }

    @ParameterizedTest
    @ValueSource(strings = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"})
    @DisplayName("Should accept all 8 standard compass points")
    void testConstructor_AllValidCompassPoints(String point) {
        DirectionSegment segment = new DirectionSegment(List.of(point));

        assertThat(segment.points()).containsExactly(point);
    }

    @Test
    @DisplayName("Should normalize point case and whitespace")
    void testConstructor_NormalizesCaseAndWhitespace() {
        DirectionSegment segment = new DirectionSegment(List.of("  ne  ", "sw"));

        assertThat(segment.points()).containsExactly("NE", "SW");
    }

    @Test
    @DisplayName("Should reject null points list")
    void testConstructor_NullPointsList() {
        assertThatThrownBy(() -> new DirectionSegment(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one point");
    }

    @Test
    @DisplayName("Should reject empty points list")
    void testConstructor_EmptyPointsList() {
        List<String> emptyPoints = List.of();

        assertThatThrownBy(() -> new DirectionSegment(emptyPoints))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one point");
    }

    @Test
    @DisplayName("Should reject a null point within the list")
    void testConstructor_NullPointInList() {
        List<String> withNull = new java.util.ArrayList<>();
        withNull.add("N");
        withNull.add(null);

        assertThatThrownBy(() -> new DirectionSegment(withNull))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or blank");
    }

    @Test
    @DisplayName("Should reject a blank point within the list")
    void testConstructor_BlankPointInList() {
        List<String> blankPoint = List.of("N", "   ");

        assertThatThrownBy(() -> new DirectionSegment(blankPoint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ESE", "NNE", "XX", "NORTH", "1"})
    @DisplayName("Should reject non-standard or invalid compass points")
    void testConstructor_InvalidCompassPoint(String invalidPoint) {
        List<String> singlePoint = List.of(invalidPoint);

        assertThatThrownBy(() -> new DirectionSegment(singlePoint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid compass point");
    }

    @Test
    @DisplayName("Should reject an invalid point mixed in with valid ones")
    void testConstructor_InvalidPointMixedWithValid() {
        List<String> mixedPoints = List.of("N", "ESE", "S");

        assertThatThrownBy(() -> new DirectionSegment(mixedPoints))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid compass point");
    }

    // ==================== isRange() Tests ====================

    @Test
    @DisplayName("Should report isRange false for a single point")
    void testIsRange_SinglePoint() {
        DirectionSegment segment = new DirectionSegment(List.of("N"));

        assertThat(segment.isRange()).isFalse();
    }

    @Test
    @DisplayName("Should report isRange true for a two-point range")
    void testIsRange_TwoPoints() {
        DirectionSegment segment = new DirectionSegment(List.of("N", "E"));

        assertThat(segment.isRange()).isTrue();
    }

    @Test
    @DisplayName("Should report isRange true for a three-point arc")
    void testIsRange_ThreePoints() {
        DirectionSegment segment = new DirectionSegment(List.of("E", "S", "SW"));

        assertThat(segment.isRange()).isTrue();
    }

    // ==================== getSummary() Tests ====================

    @Test
    @DisplayName("Should summarize a single point without hyphens")
    void testGetSummary_SinglePoint() {
        DirectionSegment segment = new DirectionSegment(List.of("N"));

        assertThat(segment.getSummary()).isEqualTo("N");
    }

    @Test
    @DisplayName("Should summarize a two-point range with a hyphen")
    void testGetSummary_TwoPointRange() {
        DirectionSegment segment = new DirectionSegment(List.of("N", "E"));

        assertThat(segment.getSummary()).isEqualTo("N-E");
    }

    @Test
    @DisplayName("Should summarize a three-point arc with hyphens - KDFW real-world")
    void testGetSummary_ThreePointArc() {
        DirectionSegment segment = new DirectionSegment(List.of("E", "S", "SW"));

        assertThat(segment.getSummary()).isEqualTo("E-S-SW");
    }

    // ==================== Record Equality Tests ====================

    @Test
    @DisplayName("Should be equal when points match")
    void testEquality_SamePoints() {
        DirectionSegment segment1 = new DirectionSegment(List.of("N", "E"));
        DirectionSegment segment2 = new DirectionSegment(List.of("N", "E"));

        assertThat(segment1).isEqualTo(segment2).hasSameHashCodeAs(segment2);
    }

    @Test
    @DisplayName("Should not be equal when points differ")
    void testInequality_DifferentPoints() {
        DirectionSegment segment1 = new DirectionSegment(List.of("N", "E"));
        DirectionSegment segment2 = new DirectionSegment(List.of("N", "W"));

        assertThat(segment1).isNotEqualTo(segment2);
    }

    @Test
    @DisplayName("Should not be equal when point count differs")
    void testInequality_DifferentPointCount() {
        DirectionSegment segment1 = new DirectionSegment(List.of("N"));
        DirectionSegment segment2 = new DirectionSegment(List.of("N", "E"));

        assertThat(segment1).isNotEqualTo(segment2);
    }

    // ==================== Immutability Test ====================

    @Test
    @DisplayName("Should defensively copy the points list")
    void testImmutability_DefensiveCopy() {
        List<String> mutable = new java.util.ArrayList<>(List.of("N", "E"));
        DirectionSegment segment = new DirectionSegment(mutable);

        mutable.add("S");

        assertThat(segment.points()).containsExactly("N", "E");
    }
}
