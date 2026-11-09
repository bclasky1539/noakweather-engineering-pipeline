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
import weather.exception.InvalidObservationException;
import weather.model.components.PresentWeather;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for DirectionalWeather.
 *
 * @author bclasky1539
 *
 */
class DirectionalWeatherTest {

    private static List<DirectionSegment> segments(String... points) {
        return Arrays.stream(points).map(p -> new DirectionSegment(List.of(p))).toList();
    }

    @Test
    @DisplayName("Should create directional weather with directions via canonical constructor")
    void testConstructor_WithDirections() {
        PresentWeather weather = PresentWeather.parse("VCSH");
        List<DirectionSegment> directionSegments = segments("E", "SE");

        DirectionalWeather directionalWeather = new DirectionalWeather(weather, directionSegments);

        assertThat(directionalWeather.presentWeather()).isEqualTo(weather);
        assertThat(directionalWeather.directionSegments())
                .extracting(DirectionSegment::getSummary).containsExactly("E", "SE");
    }

    @Test
    @DisplayName("Should create directional weather with null directions via canonical constructor")
    void testConstructor_NullDirections() {
        PresentWeather weather = PresentWeather.parse("VCSH");

        DirectionalWeather directionalWeather = new DirectionalWeather(weather, null);

        assertThat(directionalWeather.presentWeather()).isEqualTo(weather);
        assertThat(directionalWeather.directionSegments()).isNull();
    }

    @Test
    @DisplayName("Should normalize empty directions list to null")
    void testConstructor_EmptyDirectionsNormalizedToNull() {
        PresentWeather weather = PresentWeather.parse("VCSH");

        DirectionalWeather directionalWeather = new DirectionalWeather(weather, List.of());

        assertThat(directionalWeather.directionSegments()).isNull();
    }

    @Test
    @DisplayName("Should throw when presentWeather is null")
    void testConstructor_NullPresentWeather_Throws() {
        List<DirectionSegment> directionSegments = segments("E");

        assertThatThrownBy(() -> new DirectionalWeather(null, directionSegments))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("Present weather cannot be null");
    }

    @Test
    @DisplayName("Should make defensive copy of directions list")
    void testConstructor_DefensiveCopyOfDirections() {
        PresentWeather weather = PresentWeather.parse("VCSH");
        List<DirectionSegment> originalList = new ArrayList<>(segments("E"));

        DirectionalWeather directionalWeather = new DirectionalWeather(weather, originalList);

        originalList.addAll(segments("SE"));

        assertThat(directionalWeather.directionSegments())
                .extracting(DirectionSegment::getSummary)
                .containsExactly("E");
    }

    @Test
    @DisplayName("hasDirections should return true when directions are present")
    void testHasDirections_True() {
        DirectionalWeather directionalWeather = new DirectionalWeather(
                PresentWeather.parse("VCSH"), segments("E", "SE"));

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
                PresentWeather.parse("VCSH"), segments("E", "SE"));

        assertThat(directionalWeather.getSummary())
                .isEqualTo(PresentWeather.parse("VCSH").getDescription() + ": E AND SE");
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
                PresentWeather.parse("VCSH"), segments("E", "SE"));
        DirectionalWeather directionalWeather2 = new DirectionalWeather(
                PresentWeather.parse("VCSH"), segments("E", "SE"));

        assertThat(directionalWeather1)
                .isEqualTo(directionalWeather2)
                .hasSameHashCodeAs(directionalWeather2);
    }

    @Test
    @DisplayName("Should not be equal when directions differ")
    void testInequality_DifferentDirections() {
        DirectionalWeather directionalWeather1 = new DirectionalWeather(
                PresentWeather.parse("VCSH"), segments("E", "SE"));
        DirectionalWeather directionalWeather2 = new DirectionalWeather(
                PresentWeather.parse("VCSH"), segments("N", "NE"));

        assertThat(directionalWeather1).isNotEqualTo(directionalWeather2);
    }

    @Test
    @DisplayName("Should not be equal when presentWeather differs")
    void testInequality_DifferentPresentWeather() {
        DirectionalWeather directionalWeather1 = new DirectionalWeather(
                PresentWeather.parse("VCSH"), segments("E"));
        DirectionalWeather directionalWeather2 = new DirectionalWeather(
                PresentWeather.parse("TSRA"), segments("E"));

        assertThat(directionalWeather1).isNotEqualTo(directionalWeather2);
    }

    @Test
    @DisplayName("Should not be equal when one has directions and the other does not")
    void testInequality_OneWithDirectionsOneWithout() {
        DirectionalWeather directionalWeather1 = new DirectionalWeather(
                PresentWeather.parse("VCSH"), segments("E"));
        DirectionalWeather directionalWeather2 = new DirectionalWeather(
                PresentWeather.parse("VCSH"), null);

        assertThat(directionalWeather1).isNotEqualTo(directionalWeather2);
    }

    @Test
    @DisplayName("Should create directional weather with allQuadrants via canonical constructor")
    void testConstructor_AllQuadrants() {
        PresentWeather weather = PresentWeather.parse("VCSH");

        DirectionalWeather directionalWeather = new DirectionalWeather(weather, null, true);

        assertThat(directionalWeather.allQuadrants()).isTrue();
        assertThat(directionalWeather.directionSegments()).isNull();
        assertThat(directionalWeather.hasDirections()).isFalse();
    }

    @Test
    @DisplayName("Two-argument constructor should default allQuadrants to false")
    void testConstructor_TwoArgDefaultsAllQuadrantsFalse() {
        DirectionalWeather directionalWeather = new DirectionalWeather(
                PresentWeather.parse("VCSH"), segments("E"));

        assertThat(directionalWeather.allQuadrants()).isFalse();
    }

    @Test
    @DisplayName("Should reject allQuadrants combined with directions")
    void testConstructor_AllQuadrantsWithDirections_Throws() {
        PresentWeather weather = PresentWeather.parse("VCSH");
        List<DirectionSegment> directionSegments = segments("E");

        assertThatThrownBy(() -> new DirectionalWeather(weather, directionSegments, true))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidObservationException.class)
                .hasMessageContaining("mutually exclusive");
    }

    @Test
    @DisplayName("allQuadrants with an empty directions list is allowed (normalized to null)")
    void testConstructor_AllQuadrantsWithEmptyDirections() {
        DirectionalWeather directionalWeather = new DirectionalWeather(
                PresentWeather.parse("VCSH"), List.of(), true);

        assertThat(directionalWeather.allQuadrants()).isTrue();
        assertThat(directionalWeather.directionSegments()).isNull();
    }

    @Test
    @DisplayName("Should generate correct summary for all quadrants")
    void testGetSummary_AllQuadrants() {
        DirectionalWeather directionalWeather = new DirectionalWeather(
                PresentWeather.parse("VCSH"), null, true);

        assertThat(directionalWeather.getSummary())
                .isEqualTo(PresentWeather.parse("VCSH").getDescription() + ": all quadrants");
    }

    @Test
    @DisplayName("Should not be equal when allQuadrants differs")
    void testInequality_AllQuadrants() {
        DirectionalWeather withFlag = new DirectionalWeather(PresentWeather.parse("VCSH"), null, true);
        DirectionalWeather withoutFlag = new DirectionalWeather(PresentWeather.parse("VCSH"), null);

        assertThat(withFlag).isNotEqualTo(withoutFlag);
    }

    @Test
    @DisplayName("getDirectionsSummary joins segments with AND, keeping ranges")
    void testGetDirectionsSummary() {
        DirectionalWeather directionalWeather = new DirectionalWeather(PresentWeather.parse("VCSH"),
                List.of(new DirectionSegment(List.of("S", "SW")), new DirectionSegment(List.of("W"))));

        assertThat(directionalWeather.getDirectionsSummary()).isEqualTo("S-SW AND W");
    }

    @Test
    @DisplayName("getDirectionsSummary is empty without directions")
    void testGetDirectionsSummary_NoDirections() {
        DirectionalWeather directionalWeather = new DirectionalWeather(PresentWeather.parse("VCSH"), null);

        assertThat(directionalWeather.getDirectionsSummary()).isEmpty();
    }

    // ==================== Location qualifier ====================

    @Test
    @DisplayName("Should store a location qualifier with directions (SH DSNT N)")
    void testConstructor_LocationWithDirections() {
        PresentWeather weather = PresentWeather.parse("SH");

        DirectionalWeather directionalWeather = new DirectionalWeather(weather, "DSNT",
                segments("N"), false);

        assertThat(directionalWeather.location()).isEqualTo("DSNT");
        assertThat(directionalWeather.hasLocation()).isTrue();
        assertThat(directionalWeather.directionSegments())
                .extracting(DirectionSegment::getSummary).containsExactly("N");
    }

    @Test
    @DisplayName("Should store a location qualifier alone (SHRA OHD)")
    void testConstructor_LocationOnly() {
        DirectionalWeather directionalWeather = new DirectionalWeather(
                PresentWeather.parse("SHRA"), "OHD", null, false);

        assertThat(directionalWeather.location()).isEqualTo("OHD");
        assertThat(directionalWeather.hasDirections()).isFalse();
    }

    @Test
    @DisplayName("Convenience constructors should default location to null")
    void testConstructor_ConvenienceConstructorsDefaultLocationNull() {
        PresentWeather weather = PresentWeather.parse("VCSH");

        assertThat(new DirectionalWeather(weather, segments("E")).location()).isNull();
        assertThat(new DirectionalWeather(weather, null, true).location()).isNull();
        assertThat(new DirectionalWeather(weather, null, true).hasLocation()).isFalse();
    }

    @Test
    @DisplayName("Should reject an unknown location qualifier as invalid report data")
    void testConstructor_InvalidLocation_Throws() {
        PresentWeather weather = PresentWeather.parse("SH");
        List<DirectionSegment> directionSegments = segments("N");

        assertThatThrownBy(() -> new DirectionalWeather(weather, "FAR", directionSegments, false))
                .isInstanceOfSatisfying(InvalidObservationException.class, e -> {
                    assertThat(e.getComponent()).isEqualTo("DirectionalWeather.location");
                    assertThat(e.getRejectedValue()).isEqualTo("FAR");
                    assertThat(e.getMessage())
                            .isEqualTo("Invalid location qualifier (valid: OHD, VC, DSNT, DSIPTD, TOP, TR): FAR");
                });
    }

    @Test
    @DisplayName("Should generate summary with location and directions")
    void testGetSummary_WithLocationAndDirections() {
        DirectionalWeather directionalWeather = new DirectionalWeather(
                PresentWeather.parse("SH"), "DSNT", segments("N"), false);

        assertThat(directionalWeather.getSummary())
                .isEqualTo(PresentWeather.parse("SH").getDescription() + " (DSNT): N");
    }

    @Test
    @DisplayName("Should generate summary with location only")
    void testGetSummary_WithLocationOnly() {
        DirectionalWeather directionalWeather = new DirectionalWeather(
                PresentWeather.parse("SHRA"), "OHD", null, false);

        assertThat(directionalWeather.getSummary())
                .isEqualTo(PresentWeather.parse("SHRA").getDescription() + " (OHD)");
    }

    @Test
    @DisplayName("Should not be equal when location differs")
    void testInequality_DifferentLocation() {
        DirectionalWeather distant = new DirectionalWeather(
                PresentWeather.parse("SH"), "DSNT", segments("N"), false);
        DirectionalWeather noLocation = new DirectionalWeather(
                PresentWeather.parse("SH"), null, segments("N"), false);

        assertThat(distant).isNotEqualTo(noLocation);
    }
}
