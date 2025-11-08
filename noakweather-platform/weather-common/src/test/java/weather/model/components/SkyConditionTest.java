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
import weather.model.enums.SkyCoverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for SkyCondition record.
 * 
 * @author bclasky1539
 *
 */
class SkyConditionTest {
    
    @Test
    void testValidSkyCondition() {
        SkyCondition sky = new SkyCondition(SkyCoverage.FEW, 25000, null);
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.FEW);
        assertThat(sky.heightFeet()).isEqualTo(25000);
        assertThat(sky.cloudType()).isNull();
    }
    
    @Test
    void testValidSkyCondition_WithCloudType() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, "CB");
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.BROKEN);
        assertThat(sky.heightFeet()).isEqualTo(5000);
        assertThat(sky.cloudType()).isEqualTo("CB");
        assertThat(sky.isCumulonimbus()).isTrue();
    }
    
    @Test
    void testValidSkyCondition_Clear() {
        SkyCondition sky = new SkyCondition(SkyCoverage.CLR, null, null);
        
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.CLR);
        assertThat(sky.heightFeet()).isNull();
        assertThat(sky.isClear()).isTrue();
    }
    
    @Test
    void testInvalidSkyCondition_NullCoverage() {
        assertThatThrownBy(() -> new SkyCondition(null, 5000, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Sky coverage cannot be null");
    }
    
    @Test
    void testInvalidSkyCondition_HeightNegative() {
        assertThatThrownBy(() -> new SkyCondition(SkyCoverage.BROKEN, -100, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cloud height out of reasonable range");
    }
    
    @Test
    void testInvalidSkyCondition_HeightTooHigh() {
        assertThatThrownBy(() -> new SkyCondition(SkyCoverage.BROKEN, 150000, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cloud height out of reasonable range");
    }
    
    @Test
    void testInvalidSkyCondition_ClearWithHeight() {
        assertThatThrownBy(() -> new SkyCondition(SkyCoverage.CLR, 5000, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Clear sky conditions should not have height");
    }
    
    @Test
    void testInvalidSkyCondition_SkyClearWithHeight() {
        assertThatThrownBy(() -> new SkyCondition(SkyCoverage.SKC, 5000, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Clear sky conditions should not have height");
    }
    
    @Test
    void testInvalidSkyCondition_NSCWithHeight() {
        assertThatThrownBy(() -> new SkyCondition(SkyCoverage.NSC, 5000, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Clear sky conditions should not have height");
    }
    
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
    void testIsClear_False() {
        SkyCondition sky = new SkyCondition(SkyCoverage.FEW, 10000, null);
        assertThat(sky.isClear()).isFalse();
    }
    
    @Test
    void testIsCeiling_Broken() {
        SkyCondition sky = new SkyCondition(SkyCoverage.BROKEN, 5000, null);
        assertThat(sky.isCeiling()).isTrue();
    }
    
    @Test
    void testIsCeiling_Overcast() {
        SkyCondition sky = new SkyCondition(SkyCoverage.OVERCAST, 2000, null);
        assertThat(sky.isCeiling()).isTrue();
    }
    
    @Test
    void testIsCeiling_VerticalVisibility() {
        SkyCondition sky = new SkyCondition(SkyCoverage.VERTICAL_VISIBILITY, 500, null);
        assertThat(sky.isCeiling()).isTrue();
    }
    
    @Test
    void testIsCeiling_False_Few() {
        SkyCondition sky = new SkyCondition(SkyCoverage.FEW, 10000, null);
        assertThat(sky.isCeiling()).isFalse();
    }
    
    @Test
    void testIsCeiling_False_Scattered() {
        SkyCondition sky = new SkyCondition(SkyCoverage.SCATTERED, 5000, null);
        assertThat(sky.isCeiling()).isFalse();
    }
    
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
    void testIsCumulonimbus_Null() {
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
}
