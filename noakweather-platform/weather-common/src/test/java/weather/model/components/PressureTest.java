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
import weather.model.enums.PressureUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for Pressure record.
 * 
 * @author bclasky1539
 *
 */
class PressureTest {
    
    @Test
    void testValidPressure_InchesHg() {
        Pressure pressure = new Pressure(30.15, PressureUnit.INCHES_HG);
        
        assertThat(pressure.value()).isEqualTo(30.15);
        assertThat(pressure.unit()).isEqualTo(PressureUnit.INCHES_HG);
    }
    
    @Test
    void testValidPressure_Hectopascals() {
        Pressure pressure = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
        
        assertThat(pressure.value()).isEqualTo(1013.0);
        assertThat(pressure.unit()).isEqualTo(PressureUnit.HECTOPASCALS);
    }
    
    @Test
    void testInvalidPressure_NullValue() {
        assertThatThrownBy(() -> new Pressure(null, PressureUnit.INCHES_HG))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Pressure value cannot be null");
    }
    
    @Test
    void testInvalidPressure_NullUnit() {
        assertThatThrownBy(() -> new Pressure(30.0, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Pressure unit cannot be null");
    }
    
    @Test
    void testInvalidPressure_InchesHg_TooLow() {
        assertThatThrownBy(() -> new Pressure(24.0, PressureUnit.INCHES_HG))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Pressure out of reasonable range (25-35 inHg)");
    }
    
    @Test
    void testInvalidPressure_InchesHg_TooHigh() {
        assertThatThrownBy(() -> new Pressure(36.0, PressureUnit.INCHES_HG))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Pressure out of reasonable range (25-35 inHg)");
    }
    
    @Test
    void testInvalidPressure_Hectopascals_TooLow() {
        assertThatThrownBy(() -> new Pressure(800.0, PressureUnit.HECTOPASCALS))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Pressure out of reasonable range (850-1085 hPa)");
    }
    
    @Test
    void testInvalidPressure_Hectopascals_TooHigh() {
        assertThatThrownBy(() -> new Pressure(1100.0, PressureUnit.HECTOPASCALS))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Pressure out of reasonable range (850-1085 hPa)");
    }
    
    @Test
    void testToInchesHg_FromInchesHg() {
        Pressure pressure = new Pressure(30.15, PressureUnit.INCHES_HG);
        
        assertThat(pressure.toInchesHg()).isEqualTo(30.15);
    }
    
    @Test
    void testToInchesHg_FromHectopascals() {
        Pressure pressure = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
        
        assertThat(pressure.toInchesHg()).isCloseTo(29.92, within(0.01));
    }
    
    @Test
    void testToHectopascals_FromHectopascals() {
        Pressure pressure = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
        
        assertThat(pressure.toHectopascals()).isEqualTo(1013.0);
    }
    
    @Test
    void testToHectopascals_FromInchesHg() {
        Pressure pressure = new Pressure(29.92, PressureUnit.INCHES_HG);
        
        assertThat(pressure.toHectopascals()).isCloseTo(1013.0, within(1.0));
    }
    
    @Test
    void testGetFormattedValue_InchesHg() {
        Pressure pressure = new Pressure(30.15, PressureUnit.INCHES_HG);
        
        assertThat(pressure.getFormattedValue()).isEqualTo("30.15 inHg");
    }
    
    @Test
    void testGetFormattedValue_Hectopascals() {
        Pressure pressure = new Pressure(1013.25, PressureUnit.HECTOPASCALS);
        
        assertThat(pressure.getFormattedValue()).isEqualTo("1013 hPa");
    }
    
    @Test
    void testFactoryMethod_InchesHg() {
        Pressure pressure = Pressure.inchesHg(30.15);
        
        assertThat(pressure.value()).isEqualTo(30.15);
        assertThat(pressure.unit()).isEqualTo(PressureUnit.INCHES_HG);
    }
    
    @Test
    void testFactoryMethod_Hectopascals() {
        Pressure pressure = Pressure.hectopascals(1013.0);
        
        assertThat(pressure.value()).isEqualTo(1013.0);
        assertThat(pressure.unit()).isEqualTo(PressureUnit.HECTOPASCALS);
    }
    
    @Test
    void testConversion_RoundTrip_InchesHg() {
        Pressure pressure = new Pressure(30.0, PressureUnit.INCHES_HG);
        
        double hPa = pressure.toHectopascals();
        Pressure converted = new Pressure(hPa, PressureUnit.HECTOPASCALS);
        
        assertThat(converted.toInchesHg()).isCloseTo(30.0, within(0.01));
    }
    
    @Test
    void testConversion_RoundTrip_Hectopascals() {
        Pressure pressure = new Pressure(1000.0, PressureUnit.HECTOPASCALS);
        
        double inHg = pressure.toInchesHg();
        Pressure converted = new Pressure(inHg, PressureUnit.INCHES_HG);
        
        assertThat(converted.toHectopascals()).isCloseTo(1000.0, within(1.0));
    }
}
