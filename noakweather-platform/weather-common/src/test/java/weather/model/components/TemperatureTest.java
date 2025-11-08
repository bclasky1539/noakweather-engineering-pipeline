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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for Temperature record.
 * 
 * @author bclasky1539
 *
 */
class TemperatureTest {
    
    @Test
    void testValidTemperature() {
        Temperature temp = new Temperature(22.0, 12.0);
        
        assertThat(temp.celsius()).isEqualTo(22.0);
        assertThat(temp.dewpointCelsius()).isEqualTo(12.0);
    }
    
    @Test
    void testValidTemperature_NegativeValues() {
        Temperature temp = new Temperature(-5.0, -12.0);
        
        assertThat(temp.celsius()).isEqualTo(-5.0);
        assertThat(temp.dewpointCelsius()).isEqualTo(-12.0);
    }
    
    @Test
    void testValidTemperature_NullDewpoint() {
        Temperature temp = new Temperature(15.0, null);
        
        assertThat(temp.celsius()).isEqualTo(15.0);
        assertThat(temp.dewpointCelsius()).isNull();
    }
    
    @Test
    void testInvalidTemperature_TooHigh() {
        assertThatThrownBy(() -> new Temperature(61.0, 10.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Temperature out of reasonable range");
    }
    
    @Test
    void testInvalidTemperature_TooLow() {
        assertThatThrownBy(() -> new Temperature(-101.0, 10.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Temperature out of reasonable range");
    }
    
    @Test
    void testInvalidDewpoint_TooHigh() {
        assertThatThrownBy(() -> new Temperature(20.0, 61.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Dewpoint out of reasonable range");
    }
    
    @Test
    void testInvalidDewpoint_TooLow() {
        assertThatThrownBy(() -> new Temperature(20.0, -101.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Dewpoint out of reasonable range");
    }
    
    @Test
    void testInvalidDewpoint_HigherThanTemperature() {
        assertThatThrownBy(() -> new Temperature(20.0, 25.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Dewpoint (25.0°C) cannot be higher than temperature (20.0°C)");
    }
    
    @Test
    void testToFahrenheit() {
        Temperature temp = new Temperature(0.0, null);
        
        assertThat(temp.toFahrenheit()).isCloseTo(32.0, within(0.01));
    }
    
    @Test
    void testToFahrenheit_Positive() {
        Temperature temp = new Temperature(22.0, null);
        
        assertThat(temp.toFahrenheit()).isCloseTo(71.6, within(0.01));
    }
    
    @Test
    void testToFahrenheit_Negative() {
        Temperature temp = new Temperature(-5.0, null);
        
        assertThat(temp.toFahrenheit()).isCloseTo(23.0, within(0.01));
    }
    
    @Test
    void testToFahrenheit_Null() {
        Temperature temp = new Temperature(null, 10.0);
        
        assertThat(temp.toFahrenheit()).isNull();
    }
    
    @Test
    void testDewpointToFahrenheit() {
        Temperature temp = new Temperature(22.0, 12.0);
        
        assertThat(temp.dewpointToFahrenheit()).isCloseTo(53.6, within(0.01));
    }
    
    @Test
    void testDewpointToFahrenheit_Null() {
        Temperature temp = new Temperature(22.0, null);
        
        assertThat(temp.dewpointToFahrenheit()).isNull();
    }
    
    @Test
    void testGetSpread() {
        Temperature temp = new Temperature(22.0, 12.0);
        
        assertThat(temp.getSpread()).isCloseTo(10.0, within(0.01));
    }
    
    @Test
    void testGetSpread_Null() {
        Temperature temp = new Temperature(22.0, null);
        
        assertThat(temp.getSpread()).isNull();
    }
    
    @Test
    void testIsFogLikely_True() {
        Temperature temp = new Temperature(15.0, 14.0);
        
        assertThat(temp.isFogLikely()).isTrue();
    }
    
    @Test
    void testIsFogLikely_False() {
        Temperature temp = new Temperature(22.0, 12.0);
        
        assertThat(temp.isFogLikely()).isFalse();
    }
    
    @Test
    void testIsFogLikely_NullDewpoint() {
        Temperature temp = new Temperature(22.0, null);
        
        assertThat(temp.isFogLikely()).isFalse();
    }
    
    @Test
    void testGetRelativeHumidity() {
        Temperature temp = new Temperature(20.0, 10.0);
        
        // Basic sanity check - should be between 0 and 100
        Double rh = temp.getRelativeHumidity();
        assertThat(rh).isNotNull();
        assertThat(rh).isBetween(0.0, 100.0);
    }
    
    @Test
    void testGetRelativeHumidity_NullDewpoint() {
        Temperature temp = new Temperature(20.0, null);
        
        assertThat(temp.getRelativeHumidity()).isNull();
    }
    
    @Test
    void testGetRelativeHumidity_NullTemperature() {
        Temperature temp = new Temperature(null, 10.0);
        
        assertThat(temp.getRelativeHumidity()).isNull();
    }
    
    @Test
    void testFromFahrenheit() {
        Temperature temp = Temperature.fromFahrenheit(32.0, 20.0);
        
        assertThat(temp.celsius()).isCloseTo(0.0, within(0.01));
        assertThat(temp.dewpointCelsius()).isCloseTo(-6.67, within(0.01));
    }
    
    @Test
    void testFromFahrenheit_NullDewpoint() {
        Temperature temp = Temperature.fromFahrenheit(68.0, null);
        
        assertThat(temp.celsius()).isCloseTo(20.0, within(0.01));
        assertThat(temp.dewpointCelsius()).isNull();
    }
    
    @Test
    void testEqualDewpointAndTemp() {
        Temperature temp = new Temperature(15.0, 15.0);
        
        assertThat(temp.getSpread()).isCloseTo(0.0, within(0.01));
        assertThat(temp.isFogLikely()).isTrue();
    }
}
