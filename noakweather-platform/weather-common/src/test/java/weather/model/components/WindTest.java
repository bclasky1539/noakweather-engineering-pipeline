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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for Wind record.
 * 
 * @author bclasky1539
 *
 */
class WindTest {
    
    @Test
    void testValidWind() {
        Wind wind = new Wind(280, 16, null, null, null, "KT");
        
        assertThat(wind.directionDegrees()).isEqualTo(280);
        assertThat(wind.speedValue()).isEqualTo(16);
        assertThat(wind.gustValue()).isNull();
        assertThat(wind.unit()).isEqualTo("KT");
    }
    
    @Test
    void testWindWithGusts() {
        Wind wind = new Wind(180, 16, 28, null, null, "KT");
        
        assertThat(wind.directionDegrees()).isEqualTo(180);
        assertThat(wind.speedValue()).isEqualTo(16);
        assertThat(wind.gustValue()).isEqualTo(28);
        assertThat(wind.hasGusts()).isTrue();
    }
    
    @Test
    void testWindWithVariability() {
        Wind wind = new Wind(280, 16, null, 240, 320, "KT");
        
        assertThat(wind.variabilityFrom()).isEqualTo(240);
        assertThat(wind.variabilityTo()).isEqualTo(320);
        assertThat(wind.isVariable()).isTrue();
    }
    
    @Test
    void testVariableWind() {
        Wind wind = new Wind(null, 3, null, null, null, "KT");
        
        assertThat(wind.directionDegrees()).isNull();
        assertThat(wind.speedValue()).isEqualTo(3);
    }
    
    @Test
    void testCalmWind() {
        Wind wind = Wind.calm();
        
        assertThat(wind.isCalm()).isTrue();
        assertThat(wind.directionDegrees()).isNull();
        assertThat(wind.speedValue()).isEqualTo(0);
    }
    
    @Test
    void testInvalidDirection_TooHigh() {
        assertThatThrownBy(() -> new Wind(361, 10, null, null, null, "KT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Wind direction must be between 0 and 360");
    }
    
    @Test
    void testInvalidDirection_Negative() {
        assertThatThrownBy(() -> new Wind(-1, 10, null, null, null, "KT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Wind direction must be between 0 and 360");
    }
    
    @Test
    void testInvalidSpeed_Negative() {
        assertThatThrownBy(() -> new Wind(280, -5, null, null, null, "KT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Wind speed cannot be negative");
    }
    
    @Test
    void testInvalidGust_Negative() {
        assertThatThrownBy(() -> new Wind(280, 10, -5, null, null, "KT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Gust speed cannot be negative");
    }
    
    @Test
    void testInvalidVariability_OnlyFrom() {
        assertThatThrownBy(() -> new Wind(280, 10, null, 240, null, "KT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Both variabilityFrom and variabilityTo must be provided together");
    }
    
    @Test
    void testInvalidVariability_OnlyTo() {
        assertThatThrownBy(() -> new Wind(280, 10, null, null, 320, "KT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Both variabilityFrom and variabilityTo must be provided together");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {-1, 361, 400})
    void testInvalidVariabilityFrom(int invalidDegrees) {
        assertThatThrownBy(() -> new Wind(280, 10, null, invalidDegrees, 320, "KT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Variability from must be between 0 and 360");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {-1, 361, 400})
    void testInvalidVariabilityTo(int invalidDegrees) {
        assertThatThrownBy(() -> new Wind(280, 10, null, 240, invalidDegrees, "KT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Variability to must be between 0 and 360");
    }
    
    @Test
    void testGetCardinalDirection_North() {
        Wind wind = new Wind(0, 10, null, null, null, "KT");
        assertThat(wind.getCardinalDirection()).isEqualTo("N");
    }
    
    @Test
    void testGetCardinalDirection_East() {
        Wind wind = new Wind(90, 10, null, null, null, "KT");
        assertThat(wind.getCardinalDirection()).isEqualTo("E");
    }
    
    @Test
    void testGetCardinalDirection_South() {
        Wind wind = new Wind(180, 10, null, null, null, "KT");
        assertThat(wind.getCardinalDirection()).isEqualTo("S");
    }
    
    @Test
    void testGetCardinalDirection_West() {
        Wind wind = new Wind(270, 10, null, null, null, "KT");
        assertThat(wind.getCardinalDirection()).isEqualTo("W");
    }
    
    @Test
    void testGetCardinalDirection_Variable() {
        Wind wind = new Wind(null, 10, null, null, null, "KT");
        assertThat(wind.getCardinalDirection()).isEqualTo("VRB");
    }
    
    @Test
    void testGetCardinalDirection_Calm() {
        Wind wind = Wind.calm();
        assertThat(wind.getCardinalDirection()).isEqualTo("CALM");
    }
    
    @Test
    void testFactoryMethod_Variable() {
        Wind wind = Wind.variable(5, "KT");
        
        assertThat(wind.directionDegrees()).isNull();
        assertThat(wind.speedValue()).isEqualTo(5);
        assertThat(wind.unit()).isEqualTo("KT");
    }
    
    @Test
    void testIsCalm_WithNullDirection() {
        Wind wind = new Wind(null, 0, null, null, null, "KT");
        assertThat(wind.isCalm()).isTrue();
    }
    
    @Test
    void testIsCalm_WithNullSpeed() {
        Wind wind = new Wind(null, null, null, null, null, "KT");
        assertThat(wind.isCalm()).isTrue();
    }
    
    @Test
    void testHasGusts_True() {
        Wind wind = new Wind(280, 16, 28, null, null, "KT");
        assertThat(wind.hasGusts()).isTrue();
    }
    
    @Test
    void testHasGusts_False() {
        Wind wind = new Wind(280, 16, null, null, null, "KT");
        assertThat(wind.hasGusts()).isFalse();
    }
}
