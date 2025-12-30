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
 * Unit tests for VariableCeiling value object.
 *
 * @author bclasky1539
 *
 */
class VariableCeilingTest {

    @Test
    @DisplayName("Should create VariableCeiling from hundreds of feet")
    void testFromHundreds() {
        // CIG 005V010 → 500-1000 feet
        VariableCeiling ceiling = VariableCeiling.fromHundreds(5, 10);

        assertThat(ceiling.minimumHeightFeet()).isEqualTo(500);
        assertThat(ceiling.maximumHeightFeet()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Should create VariableCeiling with direct values")
    void testDirectConstruction() {
        VariableCeiling ceiling = new VariableCeiling(500, 1000);

        assertThat(ceiling.minimumHeightFeet()).isEqualTo(500);
        assertThat(ceiling.maximumHeightFeet()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Should calculate ceiling range correctly")
    void testGetRangeFeet() {
        VariableCeiling ceiling = VariableCeiling.fromHundreds(5, 10);

        assertThat(ceiling.getRangeFeet()).isEqualTo(500);
    }

    @Test
    @DisplayName("Should identify low ceiling correctly")
    void testIsLowCeiling() {
        // Below 1000 feet - low ceiling
        VariableCeiling lowCeiling = VariableCeiling.fromHundreds(5, 8);
        assertThat(lowCeiling.isLowCeiling()).isTrue();

        // At or above 1000 feet - not low ceiling
        VariableCeiling normalCeiling = VariableCeiling.fromHundreds(10, 15);
        assertThat(normalCeiling.isLowCeiling()).isFalse();

        // Min at 1000 feet - not low (boundary)
        VariableCeiling boundaryCeiling = VariableCeiling.fromHundreds(10, 20);
        assertThat(boundaryCeiling.isLowCeiling()).isFalse();
    }

    @Test
    @DisplayName("Should identify significant variation correctly")
    void testIsSignificantVariation() {
        // Range >= 500 feet - significant
        VariableCeiling significant = VariableCeiling.fromHundreds(5, 10);
        assertThat(significant.isSignificantVariation()).isTrue();

        // Range < 500 feet - not significant
        VariableCeiling minor = VariableCeiling.fromHundreds(10, 14);
        assertThat(minor.isSignificantVariation()).isFalse();

        // Range exactly 500 feet - significant (boundary)
        VariableCeiling boundary = VariableCeiling.fromHundreds(10, 15);
        assertThat(boundary.isSignificantVariation()).isTrue();
    }

    @Test
    @DisplayName("Should generate correct summary")
    void testGetSummary() {
        VariableCeiling ceiling = VariableCeiling.fromHundreds(5, 10);

        assertThat(ceiling.getSummary()).isEqualTo("Variable ceiling: 500-1000 ft");
    }

    @Test
    @DisplayName("Should generate correct toString")
    void testToString() {
        VariableCeiling ceiling = VariableCeiling.fromHundreds(20, 35);

        assertThat(ceiling).hasToString("VariableCeiling{2000-3500 ft}");
    }

    @Test
    @DisplayName("Should reject negative minimum height")
    void testRejectNegativeMinimum() {
        assertThatThrownBy(() -> new VariableCeiling(-100, 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ceiling heights cannot be negative");
    }

    @Test
    @DisplayName("Should reject negative maximum height")
    void testRejectNegativeMaximum() {
        assertThatThrownBy(() -> new VariableCeiling(500, -100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ceiling heights cannot be negative");
    }

    @Test
    @DisplayName("Should reject minimum greater than maximum")
    void testRejectMinGreaterThanMax() {
        assertThatThrownBy(() -> new VariableCeiling(1500, 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minimum ceiling (1500 ft) cannot exceed maximum (1000 ft)");
    }

    @Test
    @DisplayName("Should allow minimum equal to maximum")
    void testAllowMinEqualToMax() {
        // Edge case - variable but currently same value
        VariableCeiling ceiling = new VariableCeiling(1000, 1000);

        assertThat(ceiling.minimumHeightFeet()).isEqualTo(1000);
        assertThat(ceiling.maximumHeightFeet()).isEqualTo(1000);
        assertThat(ceiling.getRangeFeet()).isZero();
    }

    @Test
    @DisplayName("Should handle zero ceiling (ground level fog)")
    void testZeroCeiling() {
        VariableCeiling ceiling = VariableCeiling.fromHundreds(0, 2);

        assertThat(ceiling.minimumHeightFeet()).isZero();
        assertThat(ceiling.maximumHeightFeet()).isEqualTo(200);
        assertThat(ceiling.isLowCeiling()).isTrue();
    }

    @Test
    @DisplayName("Should handle large ceiling heights")
    void testLargeCeilingHeights() {
        VariableCeiling ceiling = VariableCeiling.fromHundreds(250, 300);

        assertThat(ceiling.minimumHeightFeet()).isEqualTo(25000);
        assertThat(ceiling.maximumHeightFeet()).isEqualTo(30000);
        assertThat(ceiling.getRangeFeet()).isEqualTo(5000);
        assertThat(ceiling.isLowCeiling()).isFalse();
    }

    @Test
    @DisplayName("Should support equality comparison")
    void testEquality() {
        VariableCeiling ceiling1 = VariableCeiling.fromHundreds(5, 10);
        VariableCeiling ceiling2 = VariableCeiling.fromHundreds(5, 10);
        VariableCeiling ceiling3 = new VariableCeiling(500, 1000);
        VariableCeiling different = VariableCeiling.fromHundreds(10, 15);

        assertThat(ceiling1)
                .isEqualTo(ceiling2)
                .isEqualTo(ceiling3)
                .isNotEqualTo(different);
    }
}
