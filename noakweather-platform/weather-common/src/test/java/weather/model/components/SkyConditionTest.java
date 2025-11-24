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
package weather.model.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import weather.model.enums.SkyCoverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Comprehensive tests for SkyCondition record.
 * 
 * Tests cover:
 * - Validation (coverage, height, cloud type)
 * - Query methods (isClear, isCeiling, etc.)
 * - Conversion methods (getHeightMeters, getSummary)
 * - Factory methods (of, clear, skyClear, etc.)
 * - Real-world METAR examples
 * 
 * @author bclasky1539
 * 
 */
class SkyConditionTest {
    
    // ==================== Basic Construction Tests ====================
    
    @Test
    void testBasicConstruction_Valid() {
        SkyCondition sky = new SkyCondition(SkyCoverage.SCATTERED, 5000, null);
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.SCATTERED);
        assertThat(sky.heightFeet()).isEqualTo(5000);
        assertThat(sky.cloudType()).isNull();
    }
    
    @Test
    void testConstruction_WithCloudType() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 3000, "CB");
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.BROKEN);
        assertThat(sky.heightFeet()).isEqualTo(3000);
        assertThat(sky.cloudType()).isEqualTo("CB");
    }
    
    // ==================== Validation Tests ====================
    
    @Test
    void testValidation_NullCoverage() {
        assertThatThrownBy(() -> new SkyCondition(null, 5000, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Sky coverage cannot be null");
    }
    
    @Test
    void testValidation_NegativeHeight() {
        assertThatThrownBy(() -> new SkyCondition(SkyCoverage.SCATTERED, -100, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cloud height out of reasonable range");
    }
    
    @Test
    void testValidation_ExcessiveHeight() {
        assertThatThrownBy(() -> new SkyCondition(SkyCoverage.SCATTERED, 150000, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cloud height out of reasonable range");
    }
    
    @Test
    void testValidation_ClearWithHeight() {
        assertThatThrownBy(() -> new SkyCondition(SkyCoverage.CLR, 5000, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Clear sky conditions should not have height");
    }
    
    @Test
    void testValidation_SkyClearWithHeight() {
        assertThatThrownBy(() -> new SkyCondition(SkyCoverage.SKC, 5000, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Clear sky conditions should not have height");
    }
    
    @Test
    void testValidation_NoSignificantCloudsWithHeight() {
        assertThatThrownBy(() -> new SkyCondition(SkyCoverage.NSC, 5000, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Clear sky conditions should not have height");
    }
    
    @Test
    void testValidation_VerticalVisibilityWithoutHeight() {
        assertThatThrownBy(() -> new SkyCondition(SkyCoverage.VERTICAL_VISIBILITY, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Vertical visibility must have height specified");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"XX", "ABC", "ZZ", "Q", "123"})
    void testValidation_InvalidCloudType(String invalidType) {
        assertThatThrownBy(() -> new SkyCondition(SkyCoverage.BROKEN, 5000, invalidType))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid cloud type");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"CB", "TCU", "CU", "SC", "ST", "NS", "AS", "AC", "CI", "CC", "CS"})
    void testValidation_ValidCloudTypes(String validType) {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, validType);
        
        assertThat(sky.cloudType()).isEqualTo(validType);
    }
    
    @Test
    void testValidation_NullCloudType_Allowed() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, null);
        
        assertThat(sky.cloudType()).isNull();
    }
    
    @Test
    void testValidation_BlankCloudType_Allowed() {
        // Blank cloud type is allowed (treated as null)
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, "   ");
        
        assertThat(sky.cloudType()).isEqualTo("   ");
    }
    
    // ==================== Boundary Tests ====================
    
    @Test
    void testBoundary_MinHeight() {
        SkyCondition sky = new SkyCondition(SkyCoverage.OVERCAST, 0, null);
        
        assertThat(sky.heightFeet()).isZero();
    }
    
    @Test
    void testBoundary_MaxHeight() {
        SkyCondition sky = new SkyCondition(SkyCoverage.SCATTERED, 100000, null);
        
        assertThat(sky.heightFeet()).isEqualTo(100000);
    }
    
    // ==================== isClear() Tests ====================
    
    @Test
    void testIsClear_CLR() {
        SkyCondition sky = new SkyCondition(SkyCoverage.CLR, null, null);
        assertThat(sky.isClear()).isTrue();
    }
    
    @Test
    void testIsClear_SKC() {
        SkyCondition sky = new SkyCondition(SkyCoverage.SKC, null, null);
        assertThat(sky.isClear()).isTrue();
    }
    
    @Test
    void testIsClear_NSC() {
        SkyCondition sky = new SkyCondition(SkyCoverage.NSC, null, null);
        assertThat(sky.isClear()).isTrue();
    }
    
    @Test
    void testIsClear_FEW_False() {
        SkyCondition sky = new SkyCondition(SkyCoverage.FEW, 5000, null);
        assertThat(sky.isClear()).isFalse();
    }
    
    @Test
    void testIsClear_SCATTERED_False() {
        SkyCondition sky = new SkyCondition(SkyCoverage.SCATTERED, 5000, null);
        assertThat(sky.isClear()).isFalse();
    }
    
    // ==================== isCeiling() Tests ====================
    
    @Test
    void testIsCeiling_BROKEN() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, null);
        assertThat(sky.isCeiling()).isTrue();
    }
    
    @Test
    void testIsCeiling_OVERCAST() {
        SkyCondition sky = new SkyCondition(SkyCoverage.OVERCAST, 2000, null);
        assertThat(sky.isCeiling()).isTrue();
    }
    
    @Test
    void testIsCeiling_VERTICAL_VISIBILITY() {
        SkyCondition sky = new SkyCondition(SkyCoverage.VERTICAL_VISIBILITY, 200, null);
        assertThat(sky.isCeiling()).isTrue();
    }
    
    @Test
    void testIsCeiling_FEW_False() {
        SkyCondition sky = new SkyCondition(SkyCoverage.FEW, 5000, null);
        assertThat(sky.isCeiling()).isFalse();
    }
    
    @Test
    void testIsCeiling_SCATTERED_False() {
        SkyCondition sky = new SkyCondition(SkyCoverage.SCATTERED, 5000, null);
        assertThat(sky.isCeiling()).isFalse();
    }
    
    @Test
    void testIsCeiling_CLEAR_False() {
        SkyCondition sky = new SkyCondition(SkyCoverage.CLR, null, null);
        assertThat(sky.isCeiling()).isFalse();
    }
    
    // ==================== Cloud Type Tests ====================
    
    @Test
    void testIsCumulonimbus_True() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, "CB");
        assertThat(sky.isCumulonimbus()).isTrue();
    }
    
    @Test
    void testIsCumulonimbus_False() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, "TCU");
        assertThat(sky.isCumulonimbus()).isFalse();
    }
    
    @Test
    void testIsCumulonimbus_NullType() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, null);
        assertThat(sky.isCumulonimbus()).isFalse();
    }
    
    @Test
    void testIsToweringCumulus_True() {
        SkyCondition sky = new SkyCondition(SkyCoverage.SCATTERED, 4000, "TCU");
        assertThat(sky.isToweringCumulus()).isTrue();
    }
    
    @Test
    void testIsToweringCumulus_False() {
        SkyCondition sky = new SkyCondition(SkyCoverage.SCATTERED, 4000, "CB");
        assertThat(sky.isToweringCumulus()).isFalse();
    }
    
    @Test
    void testIsToweringCumulus_NullType() {
        SkyCondition sky = new SkyCondition(SkyCoverage.SCATTERED, 4000, null);
        assertThat(sky.isToweringCumulus()).isFalse();
    }
    
    @Test
    void testIsConvective_CB() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, "CB");
        assertThat(sky.isConvective()).isTrue();
    }
    
    @Test
    void testIsConvective_TCU() {
        SkyCondition sky = new SkyCondition(SkyCoverage.SCATTERED, 4000, "TCU");
        assertThat(sky.isConvective()).isTrue();
    }
    
    @Test
    void testIsConvective_OtherType() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, "CU");
        assertThat(sky.isConvective()).isFalse();
    }
    
    @Test
    void testIsConvective_NullType() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, null);
        assertThat(sky.isConvective()).isFalse();
    }
    
    // ==================== isBelowAltitude() Tests ====================
    
    @Test
    void testIsBelowAltitude_True() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, null);
        assertThat(sky.isBelowAltitude(10000)).isTrue();
    }
    
    @Test
    void testIsBelowAltitude_False() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, null);
        assertThat(sky.isBelowAltitude(3000)).isFalse();
    }
    
    @Test
    void testIsBelowAltitude_Equal() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, null);
        assertThat(sky.isBelowAltitude(5000)).isFalse();
    }
    
    @Test
    void testIsBelowAltitude_NullHeight() {
        SkyCondition sky = new SkyCondition(SkyCoverage.CLR, null, null);
        assertThat(sky.isBelowAltitude(10000)).isFalse();
    }
    
    // ==================== Conversion Tests ====================
    
    @Test
    void testGetHeightMeters() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, null);
        
        assertThat(sky.getHeightMeters()).isCloseTo(1524, within(1));
    }
    
    @Test
    void testGetHeightMeters_Null() {
        SkyCondition sky = new SkyCondition(SkyCoverage.CLR, null, null);
        
        assertThat(sky.getHeightMeters()).isNull();
    }
    
    @Test
    void testGetHeightMeters_VariousHeights() {
        assertThat(new SkyCondition(SkyCoverage.BROKEN, 1000, null).getHeightMeters())
            .isCloseTo(305, within(1));
        assertThat(new SkyCondition(SkyCoverage.BROKEN, 10000, null).getHeightMeters())
            .isCloseTo(3048, within(1));
        assertThat(new SkyCondition(SkyCoverage.BROKEN, 25000, null).getHeightMeters())
            .isCloseTo(7620, within(1));
    }
    
    // ==================== getSummary() Tests ====================
    
    @Test
    void testGetSummary_Clear() {
        SkyCondition sky = new SkyCondition(SkyCoverage.CLR, null, null);
        assertThat(sky.getSummary()).isEqualTo("Clear");
    }
    
    @Test
    void testGetSummary_SkyClear() {
        SkyCondition sky = new SkyCondition(SkyCoverage.SKC, null, null);
        assertThat(sky.getSummary()).isEqualTo("Sky Clear");
    }
    
    @Test
    void testGetSummary_ScatteredNoType() {
        SkyCondition sky = new SkyCondition(SkyCoverage.SCATTERED, 10000, null);
        assertThat(sky.getSummary()).isEqualTo("Scattered at 10000 feet");
    }
    
    @Test
    void testGetSummary_BrokenWithCB() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, "CB");
        assertThat(sky.getSummary()).isEqualTo("Broken at 5000 feet (CB)");
    }
    
    @Test
    void testGetSummary_OvercastNoType() {
        SkyCondition sky = new SkyCondition(SkyCoverage.OVERCAST, 2000, null);
        assertThat(sky.getSummary()).isEqualTo("Overcast at 2000 feet");
    }
    
    @Test
    void testGetSummary_FewWithTCU() {
        SkyCondition sky = new SkyCondition(SkyCoverage.FEW, 8000, "TCU");
        assertThat(sky.getSummary()).isEqualTo("Few at 8000 feet (TCU)");
    }
    
    @Test
    void testGetSummary_NoSignificantClouds() {
        SkyCondition sky = new SkyCondition(SkyCoverage.NSC, null, null);
        assertThat(sky.getSummary()).isEqualTo("No Significant Clouds");
    }
    
    @Test
    void testGetSummary_VerticalVisibility() {
        SkyCondition sky = new SkyCondition(SkyCoverage.VERTICAL_VISIBILITY, 200, null);
        assertThat(sky.getSummary()).isEqualTo("Vertical Visibility at 200 feet");
    }
    
    @Test
    void testGetSummary_WithBlankCloudType() {
        // Blank cloud type should be treated as no cloud type (validation allows it)
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, "  ");
        assertThat(sky.getSummary()).isEqualTo("Broken at 5000 feet");
    }
    
    // ==================== Factory Method Tests ====================
    
    @Test
    void testFactoryMethod_Of_TwoArgs() {
        SkyCondition sky = SkyCondition.of(SkyCoverage.SCATTERED, 5000);
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.SCATTERED);
        assertThat(sky.heightFeet()).isEqualTo(5000);
        assertThat(sky.cloudType()).isNull();
    }
    
    @Test
    void testFactoryMethod_Of_ThreeArgs() {
        SkyCondition sky = SkyCondition.of(SkyCoverage.BROKEN, 3000, "CB");
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.BROKEN);
        assertThat(sky.heightFeet()).isEqualTo(3000);
        assertThat(sky.cloudType()).isEqualTo("CB");
    }
    
    @Test
    void testFactoryMethod_VerticalVisibility() {
        SkyCondition sky = SkyCondition.verticalVisibility(200);
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.VERTICAL_VISIBILITY);
        assertThat(sky.heightFeet()).isEqualTo(200);
        assertThat(sky.cloudType()).isNull();
        assertThat(sky.isCeiling()).isTrue();
    }
    
    @Test
    void testFactoryMethod_Clear() {
        SkyCondition sky = SkyCondition.clear();
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.CLR);
        assertThat(sky.heightFeet()).isNull();
        assertThat(sky.cloudType()).isNull();
        assertThat(sky.isClear()).isTrue();
    }
    
    @Test
    void testFactoryMethod_SkyClear() {
        SkyCondition sky = SkyCondition.skyClear();
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.SKC);
        assertThat(sky.heightFeet()).isNull();
        assertThat(sky.cloudType()).isNull();
        assertThat(sky.isClear()).isTrue();
    }
    
    // ==================== Real-World METAR Examples ====================
    
    @Test
    void testRealWorldExample_FEW250() {
        // FEW250 - Few clouds at 25,000 feet
        SkyCondition sky = SkyCondition.of(SkyCoverage.FEW, 25000);
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.FEW);
        assertThat(sky.heightFeet()).isEqualTo(25000);
        assertThat(sky.isCeiling()).isFalse();
        assertThat(sky.getSummary()).isEqualTo("Few at 25000 feet");
    }
    
    @Test
    void testRealWorldExample_SCT100() {
        // SCT100 - Scattered clouds at 10,000 feet
        SkyCondition sky = SkyCondition.of(SkyCoverage.SCATTERED, 10000);
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.SCATTERED);
        assertThat(sky.heightFeet()).isEqualTo(10000);
        assertThat(sky.isCeiling()).isFalse();
    }
    
    @Test
    void testRealWorldExample_BKN050CB() {
        // BKN050CB - Broken cumulonimbus at 5,000 feet
        SkyCondition sky = SkyCondition.of(SkyCoverage.BROKEN, 5000, "CB");
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.BROKEN);
        assertThat(sky.heightFeet()).isEqualTo(5000);
        assertThat(sky.cloudType()).isEqualTo("CB");
        assertThat(sky.isCeiling()).isTrue();
        assertThat(sky.isCumulonimbus()).isTrue();
        assertThat(sky.isConvective()).isTrue();
    }
    
    @Test
    void testRealWorldExample_OVC020() {
        // OVC020 - Overcast at 2,000 feet
        SkyCondition sky = SkyCondition.of(SkyCoverage.OVERCAST, 2000);
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.OVERCAST);
        assertThat(sky.heightFeet()).isEqualTo(2000);
        assertThat(sky.isCeiling()).isTrue();
        assertThat(sky.getSummary()).isEqualTo("Overcast at 2000 feet");
    }
    
    @Test
    void testRealWorldExample_VV002() {
        // VV002 - Vertical visibility 200 feet (indefinite ceiling)
        SkyCondition sky = SkyCondition.verticalVisibility(200);
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.VERTICAL_VISIBILITY);
        assertThat(sky.heightFeet()).isEqualTo(200);
        assertThat(sky.isCeiling()).isTrue();
    }
    
    @Test
    void testRealWorldExample_SKC() {
        // SKC - Sky clear
        SkyCondition sky = SkyCondition.skyClear();
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.SKC);
        assertThat(sky.heightFeet()).isNull();
        assertThat(sky.isClear()).isTrue();
        assertThat(sky.isCeiling()).isFalse();
    }
    
    @Test
    void testRealWorldExample_MultipleLayersScenario() {
        // Simulating a METAR with multiple layers: FEW020 SCT050 BKN100 OVC250
        SkyCondition few = SkyCondition.of(SkyCoverage.FEW, 2000);
        SkyCondition scattered = SkyCondition.of(SkyCoverage.SCATTERED, 5000);
        SkyCondition broken = SkyCondition.of(SkyCoverage.BROKEN, 10000);
        SkyCondition overcast = SkyCondition.of(SkyCoverage.OVERCAST, 25000);
        
        // Verify the lowest ceiling would be the broken layer
        assertThat(few.isCeiling()).isFalse();
        assertThat(scattered.isCeiling()).isFalse();
        assertThat(broken.isCeiling()).isTrue();
        assertThat(overcast.isCeiling()).isTrue();
        
        // The broken layer at 10000 is the first ceiling
        assertThat(broken.heightFeet()).isLessThan(overcast.heightFeet());
    }
    
    // ==================== Equality and ToString Tests ====================
    
    @Test
    void testEquality_SameValues() {
        SkyCondition sky1 = SkyCondition.of(SkyCoverage.BROKEN, 5000, "CB");
        SkyCondition sky2 = SkyCondition.of(SkyCoverage.BROKEN, 5000, "CB");
        
        assertThat(sky1).isEqualTo(sky2);
        assertThat(sky1.hashCode()).hasSameHashCodeAs(sky2.hashCode());
    }
    
    @Test
    void testEquality_DifferentCoverage() {
        SkyCondition sky1 = SkyCondition.of(SkyCoverage.BROKEN, 5000, "CB");
        SkyCondition sky2 = SkyCondition.of(SkyCoverage.SCATTERED, 5000, "CB");
        
        assertThat(sky1).isNotEqualTo(sky2);
    }
    
    @Test
    void testEquality_DifferentHeight() {
        SkyCondition sky1 = SkyCondition.of(SkyCoverage.BROKEN, 5000, "CB");
        SkyCondition sky2 = SkyCondition.of(SkyCoverage.BROKEN, 6000, "CB");
        
        assertThat(sky1).isNotEqualTo(sky2);
    }
    
    @Test
    void testEquality_DifferentCloudType() {
        SkyCondition sky1 = SkyCondition.of(SkyCoverage.BROKEN, 5000, "CB");
        SkyCondition sky2 = SkyCondition.of(SkyCoverage.BROKEN, 5000, "TCU");
        
        assertThat(sky1).isNotEqualTo(sky2);
    }
    
    @Test
    void testToString_ContainsFields() {
        SkyCondition sky = SkyCondition.of(SkyCoverage.BROKEN, 5000, "CB");
        String str = sky.toString();
        
        assertThat(str).contains("BROKEN")
                .contains("5000")
                .contains("CB");
    }
    
    @Test
    void testToString_NullFields() {
        SkyCondition sky = SkyCondition.of(SkyCoverage.SCATTERED, 10000);
        String str = sky.toString();
        
        assertThat(str).contains("SCATTERED")
                .contains("10000");
    }
}
