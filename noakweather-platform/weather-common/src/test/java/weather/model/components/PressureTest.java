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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import weather.model.enums.PressureUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Comprehensive test suite for Pressure record.
 * 
 * @author bclasky1539
 * 
 */
@SuppressWarnings("unused") // Nested test classes are used by JUnit 5 via reflection
class PressureTest {
    
    // ==================== Validation Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")
    @DisplayName("Pressure Validation")
    class PressureValidationTests {
        
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
        void testValidPressure_InchesHg_LowerBoundary() {
            Pressure pressure = new Pressure(25.0, PressureUnit.INCHES_HG);
            assertThat(pressure.value()).isEqualTo(25.0);
        }
        
        @Test
        void testValidPressure_InchesHg_UpperBoundary() {
            Pressure pressure = new Pressure(35.0, PressureUnit.INCHES_HG);
            assertThat(pressure.value()).isEqualTo(35.0);
        }
        
        @Test
        void testValidPressure_Hectopascals_LowerBoundary() {
            Pressure pressure = new Pressure(850.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.value()).isEqualTo(850.0);
        }
        
        @Test
        void testValidPressure_Hectopascals_UpperBoundary() {
            Pressure pressure = new Pressure(1085.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.value()).isEqualTo(1085.0);
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
            assertThatThrownBy(() -> new Pressure(24.9, PressureUnit.INCHES_HG))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pressure out of reasonable range")
                .hasMessageContaining("25.0-35.0 inHg");
        }
        
        @Test
        void testInvalidPressure_InchesHg_TooHigh() {
            assertThatThrownBy(() -> new Pressure(35.1, PressureUnit.INCHES_HG))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pressure out of reasonable range")
                .hasMessageContaining("25.0-35.0 inHg");
        }
        
        @Test
        void testInvalidPressure_Hectopascals_TooLow() {
            assertThatThrownBy(() -> new Pressure(849.9, PressureUnit.HECTOPASCALS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pressure out of reasonable range")
                .hasMessageContaining("850-1085 hPa");
        }
        
        @Test
        void testInvalidPressure_Hectopascals_TooHigh() {
            assertThatThrownBy(() -> new Pressure(1085.1, PressureUnit.HECTOPASCALS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pressure out of reasonable range")
                .hasMessageContaining("850-1085 hPa");
        }
        
        @ParameterizedTest
        @ValueSource(doubles = {24.0, 20.0, 10.0, 36.0, 40.0})
        void testInvalidPressure_InchesHg_OutOfRange(double invalidPressure) {
            assertThatThrownBy(() -> new Pressure(invalidPressure, PressureUnit.INCHES_HG))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pressure out of reasonable range");
        }
        
        @ParameterizedTest
        @ValueSource(doubles = {800.0, 700.0, 1100.0, 1200.0})
        void testInvalidPressure_Hectopascals_OutOfRange(double invalidPressure) {
            assertThatThrownBy(() -> new Pressure(invalidPressure, PressureUnit.HECTOPASCALS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pressure out of reasonable range");
        }
    }
    
    // ==================== Conversion Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")
    @DisplayName("Unit Conversions")
    class ConversionTests {
        
        @Test
        void testToInchesHg_FromInchesHg() {
            Pressure pressure = new Pressure(30.15, PressureUnit.INCHES_HG);
            assertThat(pressure.toInchesHg()).isEqualTo(30.15);
        }
        
        @Test
        void testToInchesHg_FromHectopascals() {
            Pressure pressure = new Pressure(1013.25, PressureUnit.HECTOPASCALS);
            assertThat(pressure.toInchesHg()).isCloseTo(29.92, within(0.01));
        }
        
        @Test
        void testToHectopascals_FromHectopascals() {
            Pressure pressure = new Pressure(1013.25, PressureUnit.HECTOPASCALS);
            assertThat(pressure.toHectopascals()).isEqualTo(1013.25);
        }
        
        @Test
        void testToHectopascals_FromInchesHg() {
            Pressure pressure = new Pressure(29.92, PressureUnit.INCHES_HG);
            assertThat(pressure.toHectopascals()).isCloseTo(1013.25, within(1.0));
        }
        
        @Test
        void testToMillibars_FromHectopascals() {
            Pressure pressure = new Pressure(1013.25, PressureUnit.HECTOPASCALS);
            assertThat(pressure.toMillibars()).isCloseTo(1013.25, within(0.01));
        }
        
        @Test
        void testToMillibars_FromInchesHg() {
            Pressure pressure = new Pressure(29.92, PressureUnit.INCHES_HG);
            assertThat(pressure.toMillibars()).isCloseTo(1013.25, within(1.0));
        }
        
        @ParameterizedTest
        @CsvSource({
            "29.92, 1013.25",
            "30.00, 1015.92",
            "29.00, 981.85",
            "28.00, 947.98"
        })
        void testConversion_InchesHg_To_Hectopascals(double inHg, double expectedHpa) {
            Pressure pressure = new Pressure(inHg, PressureUnit.INCHES_HG);
            assertThat(pressure.toHectopascals()).isCloseTo(expectedHpa, within(1.0));
        }
        
        @ParameterizedTest
        @CsvSource({
            "1013.25, 29.92",
            "1000.00, 29.53",
            "1020.00, 30.12",
            "980.00, 28.94"
        })
        void testConversion_Hectopascals_To_InchesHg(double hPa, double expectedInHg) {
            Pressure pressure = new Pressure(hPa, PressureUnit.HECTOPASCALS);
            assertThat(pressure.toInchesHg()).isCloseTo(expectedInHg, within(0.01));
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
    
    // ==================== Query Methods Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")
    @DisplayName("Pressure Query Methods")
    class QueryMethodsTests {
        
        @Test
        void testGetDeviationFromStandard_Positive() {
            Pressure pressure = new Pressure(1020.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.getDeviationFromStandard()).isCloseTo(6.75, within(0.1));
        }
        
        @Test
        void testGetDeviationFromStandard_Negative() {
            Pressure pressure = new Pressure(1000.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.getDeviationFromStandard()).isCloseTo(-13.25, within(0.1));
        }
        
        @Test
        void testGetDeviationFromStandard_Zero() {
            Pressure pressure = new Pressure(1013.25, PressureUnit.HECTOPASCALS);
            assertThat(pressure.getDeviationFromStandard()).isCloseTo(0.0, within(0.01));
        }
        
        @Test
        void testGetDeviationFromStandard_FromInchesHg() {
            Pressure pressure = new Pressure(29.92, PressureUnit.INCHES_HG);
            assertThat(pressure.getDeviationFromStandard()).isCloseTo(0.0, within(0.5));
        }
        
        @Test
        void testIsBelowStandard_True() {
            Pressure pressure = new Pressure(1000.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isBelowStandard()).isTrue();
        }
        
        @Test
        void testIsBelowStandard_False() {
            Pressure pressure = new Pressure(1020.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isBelowStandard()).isFalse();
        }
        
        @Test
        void testIsBelowStandard_AtStandard() {
            Pressure pressure = new Pressure(1013.25, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isBelowStandard()).isFalse();
        }
        
        @Test
        void testIsAboveStandard_True() {
            Pressure pressure = new Pressure(1020.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isAboveStandard()).isTrue();
        }
        
        @Test
        void testIsAboveStandard_False() {
            Pressure pressure = new Pressure(1000.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isAboveStandard()).isFalse();
        }
        
        @Test
        void testIsAboveStandard_AtStandard() {
            Pressure pressure = new Pressure(1013.25, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isAboveStandard()).isFalse();
        }
        
        @Test
        void testIsLowPressure_True() {
            Pressure pressure = new Pressure(990.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isLowPressure()).isTrue();
        }
        
        @Test
        void testIsLowPressure_False() {
            Pressure pressure = new Pressure(1010.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isLowPressure()).isFalse();
        }
        
        @Test
        void testIsLowPressure_AtThreshold() {
            Pressure pressure = new Pressure(1000.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isLowPressure()).isFalse();
        }
        
        @Test
        void testIsHighPressure_True() {
            Pressure pressure = new Pressure(1025.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isHighPressure()).isTrue();
        }
        
        @Test
        void testIsHighPressure_False() {
            Pressure pressure = new Pressure(1010.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isHighPressure()).isFalse();
        }
        
        @Test
        void testIsHighPressure_AtThreshold() {
            Pressure pressure = new Pressure(1020.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isHighPressure()).isFalse();
        }
        
        @Test
        void testIsExtremelyLow_True() {
            Pressure pressure = new Pressure(940.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isExtremelyLow()).isTrue();
        }
        
        @Test
        void testIsExtremelyLow_False() {
            Pressure pressure = new Pressure(960.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isExtremelyLow()).isFalse();
        }
        
        @Test
        void testIsExtremelyLow_AtThreshold() {
            Pressure pressure = new Pressure(950.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isExtremelyLow()).isFalse();
        }
        
        @Test
        void testIsExtremelyHigh_True() {
            Pressure pressure = new Pressure(1050.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isExtremelyHigh()).isTrue();
        }
        
        @Test
        void testIsExtremelyHigh_False() {
            Pressure pressure = new Pressure(1030.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isExtremelyHigh()).isFalse();
        }
        
        @Test
        void testIsExtremelyHigh_AtThreshold() {
            Pressure pressure = new Pressure(1040.0, PressureUnit.HECTOPASCALS);
            assertThat(pressure.isExtremelyHigh()).isFalse();
        }
    }
    
    // ==================== Aviation Methods Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")
    @DisplayName("Aviation Calculations")
    class AviationMethodsTests {
        
        @Test
        void testGetAltimeterSetting_SeaLevel() {
            Pressure pressure = new Pressure(30.0, PressureUnit.INCHES_HG);
            double altimeter = pressure.getAltimeterSetting(0.0);
            assertThat(altimeter).isCloseTo(30.0, within(0.01));
        }
        
        @Test
        void testGetAltimeterSetting_1000Feet() {
            Pressure pressure = new Pressure(30.0, PressureUnit.INCHES_HG);
            double altimeter = pressure.getAltimeterSetting(1000.0);
            assertThat(altimeter).isCloseTo(31.0, within(0.1));
        }
        
        @Test
        void testGetAltimeterSetting_5000Feet() {
            Pressure pressure = new Pressure(30.0, PressureUnit.INCHES_HG);
            double altimeter = pressure.getAltimeterSetting(5000.0);
            assertThat(altimeter).isCloseTo(35.0, within(0.5));
        }
        
        @Test
        void testGetPressureAltitudeFeet_StandardPressure() {
            Pressure pressure = new Pressure(1013.25, PressureUnit.HECTOPASCALS);
            double altitude = pressure.getPressureAltitudeFeet();
            assertThat(altitude).isCloseTo(0.0, within(10.0));
        }
        
        @Test
        void testGetPressureAltitudeFeet_LowerPressure() {
            Pressure pressure = new Pressure(1000.0, PressureUnit.HECTOPASCALS);
            double altitude = pressure.getPressureAltitudeFeet();
            assertThat(altitude).isGreaterThan(300.0);
            assertThat(altitude).isLessThan(400.0);
        }
        
        @Test
        void testGetPressureAltitudeFeet_HigherPressure() {
            Pressure pressure = new Pressure(1030.0, PressureUnit.HECTOPASCALS);
            double altitude = pressure.getPressureAltitudeFeet();
            assertThat(altitude).isLessThan(0.0);
            assertThat(altitude).isGreaterThan(-500.0);
        }
        
        @Test
        void testGetDensityAltitude_StandardConditions() {
            // Standard conditions: 1013.25 hPa, 15°C at sea level
            Pressure pressure = new Pressure(1013.25, PressureUnit.HECTOPASCALS);
            double densityAlt = pressure.getDensityAltitude(15.0);
            assertThat(densityAlt).isCloseTo(0.0, within(50.0));
        }
        
        @Test
        void testGetDensityAltitude_HotDay() {
            // Hot day increases density altitude
            Pressure pressure = new Pressure(1013.25, PressureUnit.HECTOPASCALS);
            double densityAlt = pressure.getDensityAltitude(30.0);
            assertThat(densityAlt).isGreaterThan(1000.0);
        }
        
        @Test
        void testGetDensityAltitude_ColdDay() {
            // Cold day decreases density altitude
            Pressure pressure = new Pressure(1013.25, PressureUnit.HECTOPASCALS);
            double densityAlt = pressure.getDensityAltitude(0.0);
            assertThat(densityAlt).isLessThan(-1000.0);
        }
        
        @Test
        void testGetDensityAltitude_HighAltitudeHot() {
            // High altitude (low pressure) + hot = very high density altitude
            Pressure pressure = new Pressure(850.0, PressureUnit.HECTOPASCALS);
            double densityAlt = pressure.getDensityAltitude(25.0);
            assertThat(densityAlt).isGreaterThan(4000.0);
        }
    }
    
    // ==================== Meteorological Analysis Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")
    @DisplayName("Meteorological Analysis")
    class MeteorologicalAnalysisTests {
        
        @Test
        void testGetPressureTendency_Rising() {
            Pressure current = new Pressure(1015.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1010.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getPressureTendency(previous)).isCloseTo(5.0, within(0.1));
        }
        
        @Test
        void testGetPressureTendency_Falling() {
            Pressure current = new Pressure(1010.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1015.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getPressureTendency(previous)).isCloseTo(-5.0, within(0.1));
        }
        
        @Test
        void testGetPressureTendency_Steady() {
            Pressure current = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getPressureTendency(previous)).isCloseTo(0.0, within(0.1));
        }
        
        @Test
        void testGetPressureTendency_DifferentUnits() {
            Pressure current = new Pressure(30.0, PressureUnit.INCHES_HG);
            Pressure previous = new Pressure(1000.0, PressureUnit.HECTOPASCALS);
            
            double tendency = current.getPressureTendency(previous);
            assertThat(tendency).isGreaterThan(0.0); // 30 inHg > 1000 hPa
        }
        
        @Test
        void testGetPressureTendency_NullPrevious() {
            Pressure current = new Pressure(1015.0, PressureUnit.HECTOPASCALS);
            
            assertThatThrownBy(() -> current.getPressureTendency(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Previous pressure cannot be null");
        }
        
        @Test
        void testIsRapidPressureChange_True_Rising() {
            Pressure current = new Pressure(1020.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.isRapidPressureChange(previous)).isTrue();
        }
        
        @Test
        void testIsRapidPressureChange_True_Falling() {
            Pressure current = new Pressure(1006.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.isRapidPressureChange(previous)).isTrue();
        }
        
        @Test
        void testIsRapidPressureChange_False() {
            Pressure current = new Pressure(1015.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.isRapidPressureChange(previous)).isFalse();
        }
        
        @Test
        void testIsRapidPressureChange_AtThreshold() {
            Pressure current = new Pressure(1019.25, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1013.25, PressureUnit.HECTOPASCALS);
            
            assertThat(current.isRapidPressureChange(previous)).isTrue();
        }
        
        @Test
        void testIsRapidPressureChange_NullPrevious() {
            Pressure current = new Pressure(1020.0, PressureUnit.HECTOPASCALS);
            assertThat(current.isRapidPressureChange(null)).isFalse();
        }
        
        @Test
        void testGetPressureTendencyDescription_RapidlyRising() {
            Pressure current = new Pressure(1020.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1015.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getPressureTendencyDescription(previous)).isEqualTo("Rapidly rising");
        }
        
        @Test
        void testGetPressureTendencyDescription_Rising() {
            Pressure current = new Pressure(1015.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getPressureTendencyDescription(previous)).isEqualTo("Rising");
        }
        
        @Test
        void testGetPressureTendencyDescription_Steady() {
            Pressure current = new Pressure(1013.5, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getPressureTendencyDescription(previous)).isEqualTo("Steady");
        }
        
        @Test
        void testGetPressureTendencyDescription_Falling() {
            Pressure current = new Pressure(1011.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getPressureTendencyDescription(previous)).isEqualTo("Falling");
        }
        
        @Test
        void testGetPressureTendencyDescription_RapidlyFalling() {
            Pressure current = new Pressure(1008.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getPressureTendencyDescription(previous)).isEqualTo("Rapidly falling");
        }
        
        @Test
        void testGetWeatherCondition_NoPrevious_Stormy() {
            Pressure current = new Pressure(970.0, PressureUnit.HECTOPASCALS);
            assertThat(current.getWeatherCondition(null)).isEqualTo("Stormy conditions likely");
        }
        
        @Test
        void testGetWeatherCondition_NoPrevious_Unsettled() {
            Pressure current = new Pressure(990.0, PressureUnit.HECTOPASCALS);
            assertThat(current.getWeatherCondition(null)).isEqualTo("Unsettled weather likely");
        }
        
        @Test
        void testGetWeatherCondition_NoPrevious_Fair() {
            Pressure current = new Pressure(1035.0, PressureUnit.HECTOPASCALS);
            assertThat(current.getWeatherCondition(null)).isEqualTo("Fair weather likely");
        }
        
        @Test
        void testGetWeatherCondition_NoPrevious_GenerallyFair() {
            Pressure current = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
            assertThat(current.getWeatherCondition(null)).isEqualTo("Generally fair conditions");
        }
        
        @Test
        void testGetWeatherCondition_LowAndFalling() {
            Pressure current = new Pressure(995.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1000.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getWeatherCondition(previous))
                .isEqualTo("Deteriorating weather, storm approaching");
        }
        
        @Test
        void testGetWeatherCondition_LowAndRising() {
            Pressure current = new Pressure(995.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(990.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getWeatherCondition(previous))
                .isEqualTo("Improving weather, storm clearing");
        }
        
        @Test
        void testGetWeatherCondition_HighAndRising() {
            Pressure current = new Pressure(1025.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1023.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getWeatherCondition(previous))
                .isEqualTo("Fair weather, becoming more settled");
        }
        
        @Test
        void testGetWeatherCondition_HighAndFalling() {
            Pressure current = new Pressure(1025.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1027.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getWeatherCondition(previous))
                .isEqualTo("Fair weather, may deteriorate");
        }
        
        @Test
        void testGetWeatherCondition_RapidlyFalling() {
            Pressure current = new Pressure(1010.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1014.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getWeatherCondition(previous))
                .isEqualTo("Weather deteriorating");
        }
        
        @Test
        void testGetWeatherCondition_RapidlyRising() {
            Pressure current = new Pressure(1017.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getWeatherCondition(previous))
                .isEqualTo("Weather improving");
        }
        
        @Test
        void testGetWeatherCondition_Stable() {
            Pressure current = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1013.5, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getWeatherCondition(previous))
                .isEqualTo("Weather conditions stable");
        }
    }
    
    // ==================== Formatting Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")
    @DisplayName("Formatting Methods")
    class FormattingTests {
        
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
        void testGetFormattedValue_Hectopascals_Rounding() {
            Pressure pressure = new Pressure(1013.7, PressureUnit.HECTOPASCALS);
            assertThat(pressure.getFormattedValue()).isEqualTo("1014 hPa");
        }
        
        @Test
        void testToMetarAltimeter() {
            Pressure pressure = new Pressure(30.15, PressureUnit.INCHES_HG);
            assertThat(pressure.toMetarAltimeter()).isEqualTo("A3015");
        }
        
        @Test
        void testToMetarAltimeter_FromHectopascals() {
            Pressure pressure = new Pressure(1013.25, PressureUnit.HECTOPASCALS);
            assertThat(pressure.toMetarAltimeter()).isEqualTo("A2992");
        }
        
        @Test
        void testToMetarAltimeter_Rounding() {
            Pressure pressure = new Pressure(30.156, PressureUnit.INCHES_HG);
            assertThat(pressure.toMetarAltimeter()).isEqualTo("A3016");
        }
        
        @Test
        void testToMetarQNH() {
            Pressure pressure = new Pressure(1013.25, PressureUnit.HECTOPASCALS);
            assertThat(pressure.toMetarQNH()).isEqualTo("Q1013");
        }
        
        @Test
        void testToMetarQNH_FromInchesHg() {
            Pressure pressure = new Pressure(29.92, PressureUnit.INCHES_HG);
            assertThat(pressure.toMetarQNH()).isEqualTo("Q1013");
        }
        
        @Test
        void testToMetarQNH_Rounding() {
            Pressure pressure = new Pressure(1013.7, PressureUnit.HECTOPASCALS);
            assertThat(pressure.toMetarQNH()).isEqualTo("Q1014");
        }
        
        @Test
        void testGetSummary_InchesHg() {
            Pressure pressure = new Pressure(30.15, PressureUnit.INCHES_HG);
            String summary = pressure.getSummary();
            
            assertThat(summary).contains("30.15 inHg");
            assertThat(summary).contains("30.15 inHg");
            assertThat(summary).contains("hPa");
        }
        
        @Test
        void testGetSummary_Hectopascals() {
            Pressure pressure = new Pressure(1013.25, PressureUnit.HECTOPASCALS);
            String summary = pressure.getSummary();
            
            assertThat(summary).contains("1013 hPa");
            assertThat(summary).contains("inHg");
            assertThat(summary).contains("hPa");
        }
    }
    
    // ==================== Factory Methods Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")
    @DisplayName("Factory Methods")
    class FactoryMethodsTests {
        
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
        void testFactoryMethod_Standard() {
            Pressure pressure = Pressure.standard();
            
            assertThat(pressure.value()).isEqualTo(1013.25);
            assertThat(pressure.unit()).isEqualTo(PressureUnit.HECTOPASCALS);
        }
        
        @Test
        void testFromMetarAltimeter_Valid() {
            Pressure pressure = Pressure.fromMetarAltimeter("A3015");
            
            assertThat(pressure.value()).isEqualTo(30.15);
            assertThat(pressure.unit()).isEqualTo(PressureUnit.INCHES_HG);
        }
        
        @Test
        void testFromMetarAltimeter_StandardPressure() {
            Pressure pressure = Pressure.fromMetarAltimeter("A2992");
            
            assertThat(pressure.value()).isEqualTo(29.92);
            assertThat(pressure.unit()).isEqualTo(PressureUnit.INCHES_HG);
        }
        
        @Test
        void testFromMetarAltimeter_Invalid_Null() {
            assertThatThrownBy(() -> Pressure.fromMetarAltimeter(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid METAR altimeter format");
        }
        
        @Test
        void testFromMetarAltimeter_Invalid_WrongPrefix() {
            assertThatThrownBy(() -> Pressure.fromMetarAltimeter("Q3015"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid METAR altimeter format");
        }
        
        @Test
        void testFromMetarAltimeter_Invalid_WrongLength() {
            assertThatThrownBy(() -> Pressure.fromMetarAltimeter("A301"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid METAR altimeter format");
        }
        
        @Test
        void testFromMetarAltimeter_Invalid_NonNumeric() {
            assertThatThrownBy(() -> Pressure.fromMetarAltimeter("A30AB"))
                .isInstanceOf(IllegalArgumentException.class);
        }
        
        @Test
        void testFromMetarQNH_Valid() {
            Pressure pressure = Pressure.fromMetarQNH("Q1013");
            
            assertThat(pressure.value()).isEqualTo(1013.0);
            assertThat(pressure.unit()).isEqualTo(PressureUnit.HECTOPASCALS);
        }
        
        @Test
        void testFromMetarQNH_HighPressure() {
            Pressure pressure = Pressure.fromMetarQNH("Q1035");
            
            assertThat(pressure.value()).isEqualTo(1035.0);
            assertThat(pressure.unit()).isEqualTo(PressureUnit.HECTOPASCALS);
        }
        
        @Test
        void testFromMetarQNH_Invalid_Null() {
            assertThatThrownBy(() -> Pressure.fromMetarQNH(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid METAR QNH format");
        }
        
        @Test
        void testFromMetarQNH_Invalid_WrongPrefix() {
            assertThatThrownBy(() -> Pressure.fromMetarQNH("A1013"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid METAR QNH format");
        }
        
        @Test
        void testFromMetarQNH_Invalid_WrongLength() {
            assertThatThrownBy(() -> Pressure.fromMetarQNH("Q101"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid METAR QNH format");
        }
        
        @Test
        void testFromMetarQNH_Invalid_NonNumeric() {
            assertThatThrownBy(() -> Pressure.fromMetarQNH("Q10AB"))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
    
    // ==================== Real-World Scenarios Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")
    @DisplayName("Real-World Scenarios")
    class RealWorldScenariosTests {
        
        @Test
        void testScenario_HurricaneConditions() {
            Pressure pressure = new Pressure(940.0, PressureUnit.HECTOPASCALS);
            
            assertThat(pressure.isExtremelyLow()).isTrue();
            assertThat(pressure.isLowPressure()).isTrue();
            assertThat(pressure.isBelowStandard()).isTrue();
            assertThat(pressure.getWeatherCondition(null)).isEqualTo("Stormy conditions likely");
        }
        
        @Test
        void testScenario_ApproachingStorm() {
            Pressure current = new Pressure(995.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1005.0, PressureUnit.HECTOPASCALS);
            
            assertThat(current.isLowPressure()).isTrue();
            assertThat(current.isRapidPressureChange(previous)).isTrue();
            assertThat(current.getPressureTendencyDescription(previous)).isEqualTo("Rapidly falling");
            assertThat(current.getWeatherCondition(previous))
                .isEqualTo("Deteriorating weather, storm approaching");
        }
        
        @Test
        void testScenario_FairWeather() {
            Pressure pressure = new Pressure(1035.0, PressureUnit.HECTOPASCALS);
            
            assertThat(pressure.isHighPressure()).isTrue();
            assertThat(pressure.isAboveStandard()).isTrue();
            assertThat(pressure.getWeatherCondition(null)).isEqualTo("Fair weather likely");
        }
        
        @Test
        void testScenario_StableConditions() {
            Pressure current = new Pressure(1013.0, PressureUnit.HECTOPASCALS);
            Pressure previous = new Pressure(1013.5, PressureUnit.HECTOPASCALS);
            
            assertThat(current.getPressureTendencyDescription(previous)).isEqualTo("Steady");
            assertThat(current.isRapidPressureChange(previous)).isFalse();
            assertThat(current.getWeatherCondition(previous)).isEqualTo("Weather conditions stable");
        }
        
        @Test
        void testScenario_HighAltitudeAirport() {
            // Denver International Airport at 5,434 feet
            Pressure pressure = new Pressure(28.0, PressureUnit.INCHES_HG);
            
            double altimeter = pressure.getAltimeterSetting(5434.0);
            assertThat(altimeter).isGreaterThan(33.0);
            
            double pressureAlt = pressure.getPressureAltitudeFeet();
            // 28.0 inHg is significantly below standard, so pressure altitude will be positive
            assertThat(pressureAlt).isGreaterThan(1800.0);
            assertThat(pressureAlt).isLessThan(2000.0);
        }
        
        @Test
        void testScenario_HotDayDensityAltitude() {
            // Hot summer day affecting aircraft performance
            Pressure pressure = new Pressure(29.92, PressureUnit.INCHES_HG);
            double densityAlt = pressure.getDensityAltitude(35.0);
            
            assertThat(densityAlt).isGreaterThan(2000.0);
        }
    }
    
    // ==================== Constants Validation Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")
    @DisplayName("Constants Validation")
    class ConstantsValidationTests {
        
        @Test
        void testStandardPressureConstants() {
            assertThat(Pressure.STANDARD_PRESSURE_INHG).isEqualTo(29.92);
            assertThat(Pressure.STANDARD_PRESSURE_HPA).isEqualTo(1013.25);
        }
        
        @Test
        void testPressureRangeConstants() {
            assertThat(Pressure.MIN_PRESSURE_INHG).isEqualTo(25.0);
            assertThat(Pressure.MAX_PRESSURE_INHG).isEqualTo(35.0);
            assertThat(Pressure.MIN_PRESSURE_HPA).isEqualTo(850.0);
            assertThat(Pressure.MAX_PRESSURE_HPA).isEqualTo(1085.0);
        }
        
        @Test
        void testConversionFactorConstants() {
            assertThat(Pressure.INHG_TO_HPA).isCloseTo(33.8639, within(0.0001));
            assertThat(Pressure.HPA_TO_INHG).isCloseTo(0.02953, within(0.00001));
            assertThat(Pressure.HPA_TO_MB).isEqualTo(1.0);
        }
        
        @Test
        void testThresholdConstants() {
            assertThat(Pressure.LOW_PRESSURE_THRESHOLD_HPA).isEqualTo(1000.0);
            assertThat(Pressure.HIGH_PRESSURE_THRESHOLD_HPA).isEqualTo(1020.0);
            assertThat(Pressure.RAPID_PRESSURE_CHANGE_HPA).isEqualTo(6.0);
        }
    }
    
    // ==================== Edge Cases Tests ====================
    
    @Test
    void testEquality() {
        Pressure p1 = new Pressure(30.15, PressureUnit.INCHES_HG);
        Pressure p2 = new Pressure(30.15, PressureUnit.INCHES_HG);
        Pressure p3 = new Pressure(30.16, PressureUnit.INCHES_HG);
        
        assertThat(p1).isEqualTo(p2);
        assertThat(p1).isNotEqualTo(p3);
    }
    
    @Test
    void testToString() {
        Pressure pressure = new Pressure(30.15, PressureUnit.INCHES_HG);
        String str = pressure.toString();
        
        assertThat(str).contains("30.15");
        assertThat(str).contains("INCHES_HG");
    }
}
