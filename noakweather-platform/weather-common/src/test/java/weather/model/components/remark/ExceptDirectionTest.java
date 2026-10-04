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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ExceptDirection value object.
 *
 * @author bclasky1539
 *
 */
class ExceptDirectionTest {

    // ==================== Construction Tests ====================

    @Test
    @DisplayName("Should construct with a single-point direction segment")
    void testConstructor_SinglePoint() {
        ExceptDirection except = new ExceptDirection(List.of(new DirectionSegment(List.of("N"))));

        assertThat(except.directionSegments()).hasSize(1);
        assertThat(except.directionSegments().get(0).points()).containsExactly("N");
    }

    @Test
    @DisplayName("Should construct with a range direction segment")
    void testConstructor_Range() {
        ExceptDirection except = new ExceptDirection(List.of(new DirectionSegment(List.of("N", "NE"))));

        assertThat(except.directionSegments()).hasSize(1);
        assertThat(except.directionSegments().get(0).points()).containsExactly("N", "NE");
        assertThat(except.directionSegments().get(0).isRange()).isTrue();
    }

    @Test
    @DisplayName("Should construct with multiple AND-chained direction segments")
    void testConstructor_AndChain() {
        ExceptDirection except = new ExceptDirection(List.of(
                new DirectionSegment(List.of("N")),
                new DirectionSegment(List.of("SW"))
        ));

        assertThat(except.directionSegments()).hasSize(2);
        assertThat(except.directionSegments().get(0).points()).containsExactly("N");
        assertThat(except.directionSegments().get(1).points()).containsExactly("SW");
    }

    // ==================== Factory Method Tests ====================

    @Test
    @DisplayName("Should create via of() factory method")
    void testOf() {
        List<DirectionSegment> segments = List.of(new DirectionSegment(List.of("N")));
        ExceptDirection except = ExceptDirection.of(segments);

        assertThat(except.directionSegments()).isEqualTo(segments);
    }

    // ==================== hasDirections() Tests ====================

    @Test
    @DisplayName("Should report hasDirections true when segments present")
    void testHasDirections_Present() {
        ExceptDirection except = ExceptDirection.of(List.of(new DirectionSegment(List.of("N"))));

        assertThat(except.hasDirections()).isTrue();
    }

    @Test
    @DisplayName("Should report hasDirections false when segments empty")
    void testHasDirections_Empty() {
        ExceptDirection except = ExceptDirection.of(List.of());

        assertThat(except.hasDirections()).isFalse();
    }

    @Test
    @DisplayName("Should report hasDirections false when segments null")
    void testHasDirections_Null() {
        ExceptDirection except = ExceptDirection.of(null);

        assertThat(except.hasDirections()).isFalse();
    }

    // ==================== getSummary() Tests ====================

    @Test
    @DisplayName("Should summarize a single-point direction - TTPP real-world (CB ALQDS XCPT N)")
    void testGetSummary_SinglePoint() {
        ExceptDirection except = ExceptDirection.of(List.of(new DirectionSegment(List.of("N"))));

        assertThat(except.getSummary()).isEqualTo("Except N");
    }

    @Test
    @DisplayName("Should summarize a range direction")
    void testGetSummary_Range() {
        ExceptDirection except = ExceptDirection.of(List.of(new DirectionSegment(List.of("N", "NE"))));

        assertThat(except.getSummary()).isEqualTo("Except N-NE");
    }

    @Test
    @DisplayName("Should summarize AND-chained segments")
    void testGetSummary_AndChain() {
        ExceptDirection except = ExceptDirection.of(List.of(
                new DirectionSegment(List.of("N")),
                new DirectionSegment(List.of("SW"))
        ));

        assertThat(except.getSummary()).isEqualTo("Except N AND SW");
    }

    @Test
    @DisplayName("Should summarize AND-chained range segments")
    void testGetSummary_AndChainOfRanges() {
        ExceptDirection except = ExceptDirection.of(List.of(
                new DirectionSegment(List.of("N", "E")),
                new DirectionSegment(List.of("SE", "S"))
        ));

        assertThat(except.getSummary()).isEqualTo("Except N-E AND SE-S");
    }

    @Test
    @DisplayName("Should summarize when no directions present")
    void testGetSummary_NoDirections() {
        ExceptDirection except = ExceptDirection.of(List.of());

        assertThat(except.getSummary()).isEqualTo("Except (no direction)");
    }

    // ==================== Record Equality Tests ====================

    @Test
    @DisplayName("Should be equal when direction segments match")
    void testEquality_SameSegments() {
        ExceptDirection except1 = ExceptDirection.of(List.of(new DirectionSegment(List.of("N"))));
        ExceptDirection except2 = ExceptDirection.of(List.of(new DirectionSegment(List.of("N"))));

        assertThat(except1).isEqualTo(except2).hasSameHashCodeAs(except2);
    }

    @Test
    @DisplayName("Should not be equal when direction segments differ")
    void testInequality_DifferentSegments() {
        ExceptDirection except1 = ExceptDirection.of(List.of(new DirectionSegment(List.of("N"))));
        ExceptDirection except2 = ExceptDirection.of(List.of(new DirectionSegment(List.of("S"))));

        assertThat(except1).isNotEqualTo(except2);
    }

    @Test
    @DisplayName("Should not be equal when one has an AND-chain and the other a single segment")
    void testInequality_DifferentSegmentCount() {
        ExceptDirection except1 = ExceptDirection.of(List.of(new DirectionSegment(List.of("N"))));
        ExceptDirection except2 = ExceptDirection.of(List.of(
                new DirectionSegment(List.of("N")),
                new DirectionSegment(List.of("SW"))
        ));

        assertThat(except1).isNotEqualTo(except2);
    }
}
