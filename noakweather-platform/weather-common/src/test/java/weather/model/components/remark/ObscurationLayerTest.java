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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ObscurationLayer value object.
 *
 * @author bclasky1539
 *
 */
class ObscurationLayerTest {

    @Test
    @DisplayName("Should create ObscurationLayer from hundreds")
    void testFromHundreds() {
        // FEW FG 000 → Few fog at ground level
        ObscurationLayer layer = ObscurationLayer.fromHundreds("FEW", "FG", 0);

        assertThat(layer.coverage()).isEqualTo("FEW");
        assertThat(layer.phenomenon()).isEqualTo("FG");
        assertThat(layer.heightFeet()).isZero();
    }

    @Test
    @DisplayName("Should create ObscurationLayer with direct values")
    void testDirectConstruction() {
        ObscurationLayer layer = new ObscurationLayer("SCT", "FU", 1000);

        assertThat(layer.coverage()).isEqualTo("SCT");
        assertThat(layer.phenomenon()).isEqualTo("FU");
        assertThat(layer.heightFeet()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Should identify ground level correctly")
    void testIsGroundLevel() {
        // Ground level (0 feet)
        ObscurationLayer groundLevel = ObscurationLayer.fromHundreds("FEW", "FG", 0);
        assertThat(groundLevel.isGroundLevel()).isTrue();

        // Above ground
        ObscurationLayer elevated = ObscurationLayer.fromHundreds("SCT", "FU", 10);
        assertThat(elevated.isGroundLevel()).isFalse();
    }

    @Test
    @DisplayName("Should identify low level correctly")
    void testIsLowLevel() {
        // Below 1000 feet - low level
        ObscurationLayer low = ObscurationLayer.fromHundreds("BKN", "BR", 5);
        assertThat(low.isLowLevel()).isTrue();

        // At or above 1000 feet - not low
        ObscurationLayer high = ObscurationLayer.fromHundreds("OVC", "HZ", 10);
        assertThat(high.isLowLevel()).isFalse();

        // Exactly 1000 feet - not low (boundary)
        ObscurationLayer boundary = new ObscurationLayer("SCT", "DU", 1000);
        assertThat(boundary.isLowLevel()).isFalse();
    }

    @Test
    @DisplayName("Should get correct coverage description")
    void testGetCoverageDescription() {
        assertThat(ObscurationLayer.fromHundreds("FEW", "FG", 0).getCoverageDescription()).isEqualTo("Few");
        assertThat(ObscurationLayer.fromHundreds("SCT", "FU", 10).getCoverageDescription()).isEqualTo("Scattered");
        assertThat(ObscurationLayer.fromHundreds("BKN", "BR", 5).getCoverageDescription()).isEqualTo("Broken");
        assertThat(ObscurationLayer.fromHundreds("OVC", "HZ", 20).getCoverageDescription()).isEqualTo("Overcast");
    }

    @Test
    @DisplayName("Should get correct phenomenon description")
    void testGetPhenomenonDescription() {
        assertThat(ObscurationLayer.fromHundreds("FEW", "FG", 0).getPhenomenonDescription()).isEqualTo("Fog");
        assertThat(ObscurationLayer.fromHundreds("SCT", "BR", 5).getPhenomenonDescription()).isEqualTo("Mist");
        assertThat(ObscurationLayer.fromHundreds("BKN", "FU", 10).getPhenomenonDescription()).isEqualTo("Smoke");
        assertThat(ObscurationLayer.fromHundreds("OVC", "HZ", 15).getPhenomenonDescription()).isEqualTo("Haze");
        assertThat(ObscurationLayer.fromHundreds("FEW", "DU", 20).getPhenomenonDescription()).isEqualTo("Dust");
        assertThat(ObscurationLayer.fromHundreds("SCT", "SA", 25).getPhenomenonDescription()).isEqualTo("Sand");
        assertThat(ObscurationLayer.fromHundreds("BKN", "VA", 30).getPhenomenonDescription()).isEqualTo("Volcanic Ash");
        assertThat(ObscurationLayer.fromHundreds("OVC", "PY", 10).getPhenomenonDescription()).isEqualTo("Spray");
    }

    @Test
    @DisplayName("Should generate correct summary for ground level")
    void testGetSummary_GroundLevel() {
        ObscurationLayer layer = ObscurationLayer.fromHundreds("FEW", "FG", 0);

        assertThat(layer.getSummary()).isEqualTo("Few Fog at ground level");
    }

    @Test
    @DisplayName("Should generate correct summary for elevated layer")
    void testGetSummary_Elevated() {
        ObscurationLayer layer = ObscurationLayer.fromHundreds("SCT", "FU", 10);

        assertThat(layer.getSummary()).isEqualTo("Scattered Smoke at 1000 ft");
    }

    @Test
    @DisplayName("Should generate correct toString")
    void testToString() {
        ObscurationLayer layer = ObscurationLayer.fromHundreds("BKN", "BR", 5);

        assertThat(layer).hasToString("ObscurationLayer{BKN BR 500 ft}");
    }

    @Test
    @DisplayName("Should reject null coverage")
    void testRejectNullCoverage() {
        assertThatThrownBy(() -> new ObscurationLayer(null, "FG", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coverage cannot be null or blank");
    }

    @Test
    @DisplayName("Should reject blank coverage")
    void testRejectBlankCoverage() {
        assertThatThrownBy(() -> new ObscurationLayer("", "FG", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coverage cannot be null or blank");
    }

    @Test
    @DisplayName("Should reject null phenomenon")
    void testRejectNullPhenomenon() {
        assertThatThrownBy(() -> new ObscurationLayer("FEW", null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phenomenon cannot be null or blank");
    }

    @Test
    @DisplayName("Should reject blank phenomenon")
    void testRejectBlankPhenomenon() {
        assertThatThrownBy(() -> new ObscurationLayer("FEW", "  ", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phenomenon cannot be null or blank");
    }

    @Test
    @DisplayName("Should reject negative height")
    void testRejectNegativeHeight() {
        assertThatThrownBy(() -> new ObscurationLayer("FEW", "FG", -100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Height cannot be negative");
    }

    @Test
    @DisplayName("Should handle large heights")
    void testLargeHeights() {
        ObscurationLayer layer = ObscurationLayer.fromHundreds("OVC", "HZ", 250);

        assertThat(layer.heightFeet()).isEqualTo(25000);
        assertThat(layer.isLowLevel()).isFalse();
        assertThat(layer.isGroundLevel()).isFalse();
    }

    @Test
    @DisplayName("Should support equality comparison")
    void testEquality() {
        ObscurationLayer layer1 = ObscurationLayer.fromHundreds("FEW", "FG", 0);
        ObscurationLayer layer2 = ObscurationLayer.fromHundreds("FEW", "FG", 0);
        ObscurationLayer layer3 = new ObscurationLayer("FEW", "FG", 0);
        ObscurationLayer different = ObscurationLayer.fromHundreds("SCT", "FU", 10);

        assertThat(layer1)
                .isEqualTo(layer2)
                .isEqualTo(layer3)
                .isNotEqualTo(different);
    }

    @Test
    @DisplayName("Should handle unknown coverage codes")
    void testUnknownCoverage() {
        ObscurationLayer layer = new ObscurationLayer("XXX", "FG", 0);

        assertThat(layer.getCoverageDescription()).isEqualTo("XXX");
    }

    @Test
    @DisplayName("Should handle unknown phenomenon codes")
    void testUnknownPhenomenon() {
        ObscurationLayer layer = new ObscurationLayer("FEW", "XX", 0);

        assertThat(layer.getPhenomenonDescription()).isEqualTo("XX");
    }
}
