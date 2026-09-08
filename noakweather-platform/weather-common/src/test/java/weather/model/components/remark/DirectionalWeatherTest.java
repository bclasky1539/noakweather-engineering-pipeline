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
import weather.model.components.PresentWeather;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for DirectionalWeather.
 *
 * @author bclasky1539
 *
 */
class DirectionalWeatherTest {

    @Test
    @DisplayName("Should create directional weather with directions via canonical constructor")
    void testConstructor_WithDirections() {
        PresentWeather weather = PresentWeather.parse("VCSH");
        List<String> directions = List.of("E", "SE");

        DirectionalWeather directionalWeather = new DirectionalWeather(weather, directions);

        assertThat(directionalWeather.presentWeather()).isEqualTo(weather);
        assertThat(directionalWeather.directions()).containsExactly("E", "SE");
    }

    @Test
    @DisplayName("Should create directional weather with null directions via canonical constructor")
    void testConstructor_NullDirections() {
        PresentWeather weather = PresentWeather.parse("VCSH");

        DirectionalWeather directionalWeather = new DirectionalWeather(weather, null);

        assertThat(directionalWeather.presentWeather()).isEqualTo(weather);
        assertThat(directionalWeather.directions()).isNull();
    }

    @Test
    @DisplayName("Should normalize empty directions list to null")
    void testConstructor_EmptyDirectionsNormalizedToNull() {
        PresentWeather weather = PresentWeather.parse("VCSH");

        DirectionalWeather directionalWeather = new DirectionalWeather(weather, List.of());

        assertThat(directionalWeather.directions()).isNull();
    }

    @Test
    @DisplayName("Should throw when presentWeather is null")
    void testConstructor_NullPresentWeather_Throws() {
        List<String> directions = List.of("E");

        assertThatThrownBy(() -> new DirectionalWeather(null, directions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Present weather cannot be null");
    }

    @Test
    @DisplayName("Should make defensive copy of directions list")
    void testConstructor_DefensiveCopyOfDirections() {
        PresentWeather weather = PresentWeather.parse("VCSH");
        List<String> originalList = new ArrayList<>();
        originalList.add("E");

        DirectionalWeather directionalWeather = new DirectionalWeather(weather, originalList);

        originalList.add("SE");

        assertThat(directionalWeather.directions()).containsExactly("E");
    }

    @Test
    @DisplayName("hasDirections should return true when directions are present")
    void testHasDirections_True() {
        DirectionalWeather directionalWeather = new DirectionalWeather(
                PresentWeather.parse("VCSH"), List.of("E", "SE"));

        assertThat(directionalWeather.hasDirections()).isTrue();
    }

    @Test
    @DisplayName("hasDirections should return false when directions are null")
    void testHasDirections_False() {
        DirectionalWeather directionalWeather = new DirectionalWeather(
                PresentWeather.parse("VCSH"), null);

        assertThat(directionalWeather.hasDirections()).isFalse();
    }

    @Test
    @DisplayName("Should generate correct summary with directions")
    void testGetSummary_WithDirections() {
        DirectionalWeather directionalWeather = new DirectionalWeather(
                PresentWeather.parse("VCSH"), List.of("E", "SE"));

        assertThat(directionalWeather.getSummary())
                .isEqualTo(PresentWeather.parse("VCSH").getDescription() + ": E, SE");
    }

    @Test
    @DisplayName("Should generate correct summary without directions")
    void testGetSummary_WithoutDirections() {
        DirectionalWeather directionalWeather = new DirectionalWeather(
                PresentWeather.parse("VCSH"), null);

        assertThat(directionalWeather.getSummary())
                .isEqualTo(PresentWeather.parse("VCSH").getDescription());
    }

    @Test
    @DisplayName("Should be equal when presentWeather and directions match")
    void testEquality() {
        DirectionalWeather directionalWeather1 = new DirectionalWeather(
                PresentWeather.parse("VCSH"), List.of("E", "SE"));
        DirectionalWeather directionalWeather2 = new DirectionalWeather(
                PresentWeather.parse("VCSH"), List.of("E", "SE"));

        assertThat(directionalWeather1)
                .isEqualTo(directionalWeather2)
                .hasSameHashCodeAs(directionalWeather2);
    }

    @Test
    @DisplayName("Should not be equal when directions differ")
    void testInequality_DifferentDirections() {
        DirectionalWeather directionalWeather1 = new DirectionalWeather(
                PresentWeather.parse("VCSH"), List.of("E", "SE"));
        DirectionalWeather directionalWeather2 = new DirectionalWeather(
                PresentWeather.parse("VCSH"), List.of("N", "NE"));

        assertThat(directionalWeather1).isNotEqualTo(directionalWeather2);
    }

    @Test
    @DisplayName("Should not be equal when presentWeather differs")
    void testInequality_DifferentPresentWeather() {
        DirectionalWeather directionalWeather1 = new DirectionalWeather(
                PresentWeather.parse("VCSH"), List.of("E"));
        DirectionalWeather directionalWeather2 = new DirectionalWeather(
                PresentWeather.parse("TSRA"), List.of("E"));

        assertThat(directionalWeather1).isNotEqualTo(directionalWeather2);
    }

    @Test
    @DisplayName("Should not be equal when one has directions and the other does not")
    void testInequality_OneWithDirectionsOneWithout() {
        DirectionalWeather directionalWeather1 = new DirectionalWeather(
                PresentWeather.parse("VCSH"), List.of("E"));
        DirectionalWeather directionalWeather2 = new DirectionalWeather(
                PresentWeather.parse("VCSH"), null);

        assertThat(directionalWeather1).isNotEqualTo(directionalWeather2);
    }
}
