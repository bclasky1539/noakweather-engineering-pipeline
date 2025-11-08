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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for PeakWind record.
 * 
 * @author bclasky1539
 *
 */
class PeakWindTest {
    
    @Test
    void testValidPeakWind() {
        PeakWind peakWind = new PeakWind(280, 32, 15, 30);
        
        assertThat(peakWind.directionDegrees()).isEqualTo(280);
        assertThat(peakWind.speedKnots()).isEqualTo(32);
        assertThat(peakWind.hour()).isEqualTo(15);
        assertThat(peakWind.minute()).isEqualTo(30);
    }
    
    @Test
    void testValidPeakWind_WithoutHour() {
        PeakWind peakWind = new PeakWind(280, 32, null, 30);
        
        assertThat(peakWind.directionDegrees()).isEqualTo(280);
        assertThat(peakWind.speedKnots()).isEqualTo(32);
        assertThat(peakWind.hour()).isNull();
        assertThat(peakWind.minute()).isEqualTo(30);
    }
    
    @Test
    void testValidPeakWind_NullValues() {
        PeakWind peakWind = new PeakWind(null, null, null, null);
        
        assertThat(peakWind.directionDegrees()).isNull();
        assertThat(peakWind.speedKnots()).isNull();
        assertThat(peakWind.hour()).isNull();
        assertThat(peakWind.minute()).isNull();
    }
    
    @Test
    void testInvalidPeakWind_DirectionNegative() {
        assertThatThrownBy(() -> new PeakWind(-1, 32, 15, 30))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Peak wind direction must be between 0 and 360");
    }
    
    @Test
    void testInvalidPeakWind_DirectionTooHigh() {
        assertThatThrownBy(() -> new PeakWind(361, 32, 15, 30))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Peak wind direction must be between 0 and 360");
    }
    
    @Test
    void testInvalidPeakWind_SpeedNegative() {
        assertThatThrownBy(() -> new PeakWind(280, -5, 15, 30))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Peak wind speed cannot be negative");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {-1, 24, 25, 30})
    void testInvalidPeakWind_HourOutOfRange(int invalidHour) {
        assertThatThrownBy(() -> new PeakWind(280, 32, invalidHour, 30))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Hour must be between 0 and 23");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {-1, 60, 65})
    void testInvalidPeakWind_MinuteOutOfRange(int invalidMinute) {
        assertThatThrownBy(() -> new PeakWind(280, 32, 15, invalidMinute))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Minute must be between 0 and 59");
    }
    
    @Test
    void testValidPeakWind_BoundaryDirection_Zero() {
        PeakWind peakWind = new PeakWind(0, 32, 15, 30);
        
        assertThat(peakWind.directionDegrees()).isEqualTo(0);
    }
    
    @Test
    void testValidPeakWind_BoundaryDirection_360() {
        PeakWind peakWind = new PeakWind(360, 32, 15, 30);
        
        assertThat(peakWind.directionDegrees()).isEqualTo(360);
    }
    
    @Test
    void testValidPeakWind_BoundaryHour_Zero() {
        PeakWind peakWind = new PeakWind(280, 32, 0, 30);
        
        assertThat(peakWind.hour()).isEqualTo(0);
    }
    
    @Test
    void testValidPeakWind_BoundaryHour_23() {
        PeakWind peakWind = new PeakWind(280, 32, 23, 30);
        
        assertThat(peakWind.hour()).isEqualTo(23);
    }
    
    @Test
    void testValidPeakWind_BoundaryMinute_Zero() {
        PeakWind peakWind = new PeakWind(280, 32, 15, 0);
        
        assertThat(peakWind.minute()).isEqualTo(0);
    }
    
    @Test
    void testValidPeakWind_BoundaryMinute_59() {
        PeakWind peakWind = new PeakWind(280, 32, 15, 59);
        
        assertThat(peakWind.minute()).isEqualTo(59);
    }
    
    @Test
    void testToMph_ValidSpeed() {
        PeakWind peakWind = new PeakWind(280, 32, 15, 30);
        
        // 32 knots * 1.1508 = 36.8256, rounded = 37.0
        assertThat(peakWind.toMph()).isEqualTo(37.0);
    }
    
    @Test
    void testToMph_NullSpeed() {
        PeakWind peakWind = new PeakWind(280, null, 15, 30);
        
        assertThat(peakWind.toMph()).isNull();
    }
    
    @Test
    void testToMph_ZeroSpeed() {
        PeakWind peakWind = new PeakWind(280, 0, 15, 30);
        
        assertThat(peakWind.toMph()).isEqualTo(0.0);
    }
    
    @Test
    void testToMph_RoundingUp() {
        PeakWind peakWind = new PeakWind(280, 10, 15, 30);
        
        // 10 knots * 1.1508 = 11.508, rounded = 12.0
        assertThat(peakWind.toMph()).isEqualTo(12.0);
    }
    
    @Test
    void testToMph_RoundingDown() {
        PeakWind peakWind = new PeakWind(280, 9, 15, 30);
        
        // 9 knots * 1.1508 = 10.3572, rounded = 10.0
        assertThat(peakWind.toMph()).isEqualTo(10.0);
    }
    
    @Test
    void testGetCardinalDirection_North() {
        PeakWind peakWind = new PeakWind(0, 32, 15, 30);
        
        assertThat(peakWind.getCardinalDirection()).isEqualTo("N");
    }
    
    @Test
    void testGetCardinalDirection_Northeast() {
        PeakWind peakWind = new PeakWind(45, 32, 15, 30);
        
        assertThat(peakWind.getCardinalDirection()).isEqualTo("NE");
    }
    
    @Test
    void testGetCardinalDirection_East() {
        PeakWind peakWind = new PeakWind(90, 32, 15, 30);
        
        assertThat(peakWind.getCardinalDirection()).isEqualTo("E");
    }
    
    @Test
    void testGetCardinalDirection_Southeast() {
        PeakWind peakWind = new PeakWind(135, 32, 15, 30);
        
        assertThat(peakWind.getCardinalDirection()).isEqualTo("SE");
    }
    
    @Test
    void testGetCardinalDirection_South() {
        PeakWind peakWind = new PeakWind(180, 32, 15, 30);
        
        assertThat(peakWind.getCardinalDirection()).isEqualTo("S");
    }
    
    @Test
    void testGetCardinalDirection_Southwest() {
        PeakWind peakWind = new PeakWind(225, 32, 15, 30);
        
        assertThat(peakWind.getCardinalDirection()).isEqualTo("SW");
    }
    
    @Test
    void testGetCardinalDirection_West() {
        PeakWind peakWind = new PeakWind(270, 32, 15, 30);
        
        assertThat(peakWind.getCardinalDirection()).isEqualTo("W");
    }
    
    @Test
    void testGetCardinalDirection_Northwest() {
        PeakWind peakWind = new PeakWind(315, 32, 15, 30);
        
        assertThat(peakWind.getCardinalDirection()).isEqualTo("NW");
    }
    
    @Test
    void testGetCardinalDirection_360() {
        PeakWind peakWind = new PeakWind(360, 32, 15, 30);
        
        assertThat(peakWind.getCardinalDirection()).isEqualTo("N");
    }
    
    @Test
    void testGetCardinalDirection_Null() {
        PeakWind peakWind = new PeakWind(null, 32, 15, 30);
        
        assertThat(peakWind.getCardinalDirection()).isEqualTo("UNKNOWN");
    }
    
    @Test
    void testGetCardinalDirection_NNE() {
        PeakWind peakWind = new PeakWind(22, 32, 15, 30);
        
        assertThat(peakWind.getCardinalDirection()).isEqualTo("NNE");
    }
    
    @Test
    void testGetCardinalDirection_SSW() {
        PeakWind peakWind = new PeakWind(202, 32, 15, 30);
        
        assertThat(peakWind.getCardinalDirection()).isEqualTo("SSW");
    }
}
