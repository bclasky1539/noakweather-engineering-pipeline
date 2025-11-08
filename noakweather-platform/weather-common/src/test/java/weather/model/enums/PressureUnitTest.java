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
package weather.model.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PressureUnit enum.
 * 
 * @author bclasky1539
 *
 */
class PressureUnitTest {
    
    @Test
    void testInchesHgSymbol() {
        assertThat(PressureUnit.INCHES_HG.getSymbol()).isEqualTo("inHg");
    }
    
    @Test
    void testHectopascalsSymbol() {
        assertThat(PressureUnit.HECTOPASCALS.getSymbol()).isEqualTo("hPa");
    }
    
    @Test
    void testEnumValues() {
        assertThat(PressureUnit.values()).hasSize(2);
        assertThat(PressureUnit.values()).containsExactly(
            PressureUnit.INCHES_HG,
            PressureUnit.HECTOPASCALS
        );
    }
    
    @Test
    void testValueOf() {
        assertThat(PressureUnit.valueOf("INCHES_HG")).isEqualTo(PressureUnit.INCHES_HG);
        assertThat(PressureUnit.valueOf("HECTOPASCALS")).isEqualTo(PressureUnit.HECTOPASCALS);
    }
}
