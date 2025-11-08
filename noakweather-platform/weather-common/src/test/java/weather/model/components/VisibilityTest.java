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
 * Tests for Visibility record.
 * 
 * @author bclasky1539
 *
 */
class VisibilityTest {
    
    @Test
    void testValidVisibility_StatuteMiles() {
        Visibility visibility = new Visibility(10.0, "SM", null);
        
        assertThat(visibility.distanceValue()).isEqualTo(10.0);
        assertThat(visibility.unit()).isEqualTo("SM");
        assertThat(visibility.specialCondition()).isNull();
        assertThat(visibility.isSpecialCondition()).isFalse();
    }
    
    @Test
    void testValidVisibility_Meters() {
        Visibility visibility = new Visibility(9999.0, "M", null);
        
        assertThat(visibility.distanceValue()).isEqualTo(9999.0);
        assertThat(visibility.unit()).isEqualTo("M");
    }
    
    @Test
    void testValidVisibility_Cavok() {
        Visibility visibility = new Visibility(null, null, "CAVOK");
        
        assertThat(visibility.distanceValue()).isNull();
        assertThat(visibility.unit()).isNull();
        assertThat(visibility.specialCondition()).isEqualTo("CAVOK");
        assertThat(visibility.isSpecialCondition()).isTrue();
        assertThat(visibility.isCavok()).isTrue();
    }
    
    @Test
    void testInvalidVisibility_NegativeDistance() {
        assertThatThrownBy(() -> new Visibility(-1.0, "SM", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Visibility distance cannot be negative");
    }
    
    @Test
    void testInvalidVisibility_NoDistanceOrSpecialCondition() {
        assertThatThrownBy(() -> new Visibility(null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Visibility must have either distance or special condition");
    }
    
    @Test
    void testInvalidVisibility_DistanceWithoutUnit() {
        assertThatThrownBy(() -> new Visibility(10.0, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Visibility unit must be specified when distance is provided");
    }
    
    @Test
    void testInvalidVisibility_DistanceWithBlankUnit() {
        assertThatThrownBy(() -> new Visibility(10.0, "  ", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Visibility unit must be specified when distance is provided");
    }
    
    @Test
    void testToMeters_FromStatuteMiles() {
        Visibility visibility = new Visibility(1.0, "SM", null);
        
        assertThat(visibility.toMeters()).isCloseTo(1609.34, within(0.01));
    }
    
    @Test
    void testToMeters_FromMeters() {
        Visibility visibility = new Visibility(5000.0, "M", null);
        
        assertThat(visibility.toMeters()).isEqualTo(5000.0);
    }
    
    @Test
    void testToMeters_FromKilometers() {
        Visibility visibility = new Visibility(10.0, "KM", null);
        
        assertThat(visibility.toMeters()).isEqualTo(10000.0);
    }
    
    @Test
    void testToMeters_SpecialCondition() {
        Visibility visibility = Visibility.cavok();
        
        assertThat(visibility.toMeters()).isNull();
    }
    
    @Test
    void testToStatuteMiles_FromStatuteMiles() {
        Visibility visibility = new Visibility(10.0, "SM", null);
        
        assertThat(visibility.toStatuteMiles()).isEqualTo(10.0);
    }
    
    @Test
    void testToStatuteMiles_FromMeters() {
        Visibility visibility = new Visibility(1609.34, "M", null);
        
        assertThat(visibility.toStatuteMiles()).isCloseTo(1.0, within(0.01));
    }
    
    @Test
    void testToStatuteMiles_FromKilometers() {
        Visibility visibility = new Visibility(1.60934, "KM", null);
        
        assertThat(visibility.toStatuteMiles()).isCloseTo(1.0, within(0.01));
    }
    
    @Test
    void testToStatuteMiles_SpecialCondition() {
        Visibility visibility = Visibility.cavok();
        
        assertThat(visibility.toStatuteMiles()).isNull();
    }
    
    @Test
    void testFactoryMethod_Cavok() {
        Visibility visibility = Visibility.cavok();
        
        assertThat(visibility.isCavok()).isTrue();
        assertThat(visibility.isSpecialCondition()).isTrue();
        assertThat(visibility.specialCondition()).isEqualTo("CAVOK");
    }
    
    @Test
    void testFactoryMethod_StatuteMiles() {
        Visibility visibility = Visibility.statuteMiles(5.0);
        
        assertThat(visibility.distanceValue()).isEqualTo(5.0);
        assertThat(visibility.unit()).isEqualTo("SM");
        assertThat(visibility.specialCondition()).isNull();
    }
    
    @Test
    void testFactoryMethod_Meters() {
        Visibility visibility = Visibility.meters(9999.0);
        
        assertThat(visibility.distanceValue()).isEqualTo(9999.0);
        assertThat(visibility.unit()).isEqualTo("M");
        assertThat(visibility.specialCondition()).isNull();
    }
    
    @Test
    void testFractionalVisibility() {
        Visibility visibility = new Visibility(0.5, "SM", null);
        
        assertThat(visibility.distanceValue()).isEqualTo(0.5);
        assertThat(visibility.toMeters()).isCloseTo(804.67, within(0.01));
    }
}
