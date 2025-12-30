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
 * Unit tests for CeilingSecondSite value object.
 *
 * @author bclasky1539
 *
 */
class CeilingSecondSiteTest {

    @Test
    @DisplayName("Should create CeilingSecondSite from hundreds with location")
    void testFromHundreds_WithLocation() {
        // CIG 002 RY11 → 200 ft at runway 11
        CeilingSecondSite ceiling = CeilingSecondSite.fromHundreds(2, "RY11");

        assertThat(ceiling.heightFeet()).isEqualTo(200);
        assertThat(ceiling.location()).isEqualTo("RY11");
        assertThat(ceiling.hasLocation()).isTrue();
    }

    @Test
    @DisplayName("Should create CeilingSecondSite from hundreds without location")
    void testFromHundreds_WithoutLocation() {
        // CIG 010 → 1000 ft (no location)
        CeilingSecondSite ceiling = CeilingSecondSite.fromHundreds(10, null);

        assertThat(ceiling.heightFeet()).isEqualTo(1000);
        assertThat(ceiling.location()).isNull();
        assertThat(ceiling.hasLocation()).isFalse();
    }

    @Test
    @DisplayName("Should create CeilingSecondSite with direct values")
    void testDirectConstruction() {
        CeilingSecondSite ceiling = new CeilingSecondSite(500, "RWY06");

        assertThat(ceiling.heightFeet()).isEqualTo(500);
        assertThat(ceiling.location()).isEqualTo("RWY06");
    }

    @Test
    @DisplayName("Should identify low ceiling correctly")
    void testIsLowCeiling() {
        // Below 1000 feet - low ceiling
        CeilingSecondSite lowCeiling = CeilingSecondSite.fromHundreds(5, "RY11");
        assertThat(lowCeiling.isLowCeiling()).isTrue();

        // At or above 1000 feet - not low ceiling
        CeilingSecondSite normalCeiling = CeilingSecondSite.fromHundreds(10, "RY11");
        assertThat(normalCeiling.isLowCeiling()).isFalse();

        // Exactly 1000 feet - not low (boundary)
        CeilingSecondSite boundaryCeiling = new CeilingSecondSite(1000, "TWR");
        assertThat(boundaryCeiling.isLowCeiling()).isFalse();
    }

    @Test
    @DisplayName("Should identify location presence correctly")
    void testHasLocation() {
        // With location
        assertThat(CeilingSecondSite.fromHundreds(5, "RY11").hasLocation()).isTrue();
        assertThat(CeilingSecondSite.fromHundreds(5, "RWY06").hasLocation()).isTrue();

        // Without location (null)
        assertThat(CeilingSecondSite.fromHundreds(5, null).hasLocation()).isFalse();

        // Without location (blank)
        assertThat(new CeilingSecondSite(500, "").hasLocation()).isFalse();
        assertThat(new CeilingSecondSite(500, "   ").hasLocation()).isFalse();
    }

    @Test
    @DisplayName("Should generate correct summary with location")
    void testGetSummary_WithLocation() {
        CeilingSecondSite ceiling = CeilingSecondSite.fromHundreds(2, "RY11");

        assertThat(ceiling.getSummary()).isEqualTo("Ceiling 200 ft at RY11");
    }

    @Test
    @DisplayName("Should generate correct summary without location")
    void testGetSummary_WithoutLocation() {
        CeilingSecondSite ceiling = CeilingSecondSite.fromHundreds(10, null);

        assertThat(ceiling.getSummary()).isEqualTo("Ceiling 1000 ft");
    }

    @Test
    @DisplayName("Should generate correct toString with location")
    void testToString_WithLocation() {
        CeilingSecondSite ceiling = CeilingSecondSite.fromHundreds(5, "RWY06");

        assertThat(ceiling).hasToString("CeilingSecondSite{500 ft at RWY06}");
    }

    @Test
    @DisplayName("Should generate correct toString without location")
    void testToString_WithoutLocation() {
        CeilingSecondSite ceiling = CeilingSecondSite.fromHundreds(20, null);

        assertThat(ceiling).hasToString("CeilingSecondSite{2000 ft}");
    }

    @Test
    @DisplayName("Should reject negative height")
    void testRejectNegativeHeight() {
        assertThatThrownBy(() -> new CeilingSecondSite(-100, "RY11"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ceiling height cannot be negative");
    }

    @Test
    @DisplayName("Should allow zero height")
    void testAllowZeroHeight() {
        // Ground-level ceiling (fog)
        CeilingSecondSite ceiling = CeilingSecondSite.fromHundreds(0, "RY11");

        assertThat(ceiling.heightFeet()).isZero();
        assertThat(ceiling.isLowCeiling()).isTrue();
    }

    @Test
    @DisplayName("Should handle various location formats")
    void testVariousLocationFormats() {
        assertThat(CeilingSecondSite.fromHundreds(5, "RY11").location()).isEqualTo("RY11");
        assertThat(CeilingSecondSite.fromHundreds(5, "RWY06").location()).isEqualTo("RWY06");
        assertThat(CeilingSecondSite.fromHundreds(5, "TWR").location()).isEqualTo("TWR");
        assertThat(CeilingSecondSite.fromHundreds(5, "APCH").location()).isEqualTo("APCH");
    }

    @Test
    @DisplayName("Should handle large ceiling heights")
    void testLargeCeilingHeights() {
        CeilingSecondSite ceiling = CeilingSecondSite.fromHundreds(250, "RY11");

        assertThat(ceiling.heightFeet()).isEqualTo(25000);
        assertThat(ceiling.isLowCeiling()).isFalse();
    }

    @Test
    @DisplayName("Should support equality comparison")
    void testEquality() {
        CeilingSecondSite ceiling1 = CeilingSecondSite.fromHundreds(2, "RY11");
        CeilingSecondSite ceiling2 = CeilingSecondSite.fromHundreds(2, "RY11");
        CeilingSecondSite ceiling3 = new CeilingSecondSite(200, "RY11");
        CeilingSecondSite different = CeilingSecondSite.fromHundreds(5, "RY11");
        CeilingSecondSite differentLoc = CeilingSecondSite.fromHundreds(2, "RWY06");

        assertThat(ceiling1)
                .isEqualTo(ceiling2)
                .isEqualTo(ceiling3)
                .isNotEqualTo(different)
                .isNotEqualTo(differentLoc);
    }

    @Test
    @DisplayName("Should handle null and non-null locations differently")
    void testNullVsNonNullLocation() {
        CeilingSecondSite withLocation = CeilingSecondSite.fromHundreds(5, "RY11");
        CeilingSecondSite withoutLocation = CeilingSecondSite.fromHundreds(5, null);

        assertThat(withLocation).isNotEqualTo(withoutLocation);
    }
}
