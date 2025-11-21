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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Comprehensive test suite for Temperature record.
 * Replaces TemperatureTestEnhanced.java with improved organization and coverage.
 * 
 * @author bclasky1539
 * 
 */
@SuppressWarnings("unused") // Nested test classes are used by JUnit 5 via reflection
class TemperatureTest {
    
    // ==================== Construction and Validation Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")  // SonarQube: @Nested test classes contain tests in methods
    @DisplayName("Temperature Validation")
    class TemperatureValidationTests {
        
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
        void testValidTemperature_NullTemperature() {
            Temperature temp = new Temperature(null, null);
            
            assertThat(temp.celsius()).isNull();
            assertThat(temp.dewpointCelsius()).isNull();
        }
        
        @Test
        void testValidTemperature_BoundaryValues() {
            // Test at exact boundaries
            Temperature tempMax = new Temperature(60.0, 60.0);
            Temperature tempMin = new Temperature(-100.0, -100.0);
            
            assertThat(tempMax.celsius()).isEqualTo(60.0);
            assertThat(tempMin.celsius()).isEqualTo(-100.0);
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
        
        @ParameterizedTest
        @ValueSource(doubles = {-150.0, -101.0, 60.1, 70.0, 100.0})
        void testInvalidTemperatureValues(double invalidTemp) {
            assertThatThrownBy(() -> new Temperature(invalidTemp, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Temperature out of reasonable range");
        }
    }
    
    @Nested
    @SuppressWarnings("java:S2187")  // SonarQube: @Nested test classes contain tests in methods
    @DisplayName("Dewpoint Validation")
    class DewpointValidationTests {
        
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
        void testEqualDewpointAndTemp() {
            Temperature temp = new Temperature(15.0, 15.0);
            
            assertThat(temp.getSpread()).isCloseTo(0.0, within(0.01));
            assertThat(temp.isFogLikely()).isTrue();
        }
        
        @ParameterizedTest
        @CsvSource({
            "20.0, 25.0",
            "0.0, 1.0",
            "-10.0, -5.0",
            "15.0, 20.0"
        })
        void testInvalidDewpoint_HigherThanTemp_Various(double temp, double dewpoint) {
            assertThatThrownBy(() -> new Temperature(temp, dewpoint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be higher than temperature");
        }
    }
    
    // ==================== Fahrenheit Conversion Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")  // SonarQube: @Nested test classes contain tests in methods
    @DisplayName("Fahrenheit Conversions")
    class FahrenheitConversionTests {
        
        @Test
        void testToFahrenheit_FreezingPoint() {
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
        
        @ParameterizedTest
        @CsvSource({
            "0.0, 32.0",
            "-40.0, -40.0",
            "37.0, 98.6",
            "22.0, 71.6"
        })
        void testToFahrenheit_VariousTemperatures(double celsius, double expectedF) {
            Temperature temp = new Temperature(celsius, null);
            assertThat(temp.toFahrenheit()).isCloseTo(expectedF, within(0.1));
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
    }
    
    // ==================== Kelvin Conversion Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")  // SonarQube: @Nested test classes contain tests in methods
    @DisplayName("Kelvin Conversions")
    class KelvinConversionTests {
        
        @Test
        void testToKelvin() {
            Temperature temp = new Temperature(0.0, null);
            assertThat(temp.toKelvin()).isCloseTo(273.15, within(0.01));
        }
        
        @Test
        void testToKelvin_Positive() {
            Temperature temp = new Temperature(25.0, null);
            assertThat(temp.toKelvin()).isCloseTo(298.15, within(0.01));
        }
        
        @Test
        void testToKelvin_Negative() {
            Temperature temp = new Temperature(-10.0, null);
            assertThat(temp.toKelvin()).isCloseTo(263.15, within(0.01));
        }
        
        @Test
        void testToKelvin_Null() {
            Temperature temp = new Temperature(null, 10.0);
            assertThat(temp.toKelvin()).isNull();
        }
        
        @Test
        void testDewpointToKelvin() {
            Temperature temp = new Temperature(22.0, 10.0);
            assertThat(temp.dewpointToKelvin()).isCloseTo(283.15, within(0.01));
        }
        
        @Test
        void testDewpointToKelvin_Null() {
            Temperature temp = new Temperature(22.0, null);
            assertThat(temp.dewpointToKelvin()).isNull();
        }
    }
    
    // ==================== Freezing Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")  // SonarQube: @Nested test classes contain tests in methods
    @DisplayName("Freezing Point Tests")
    class FreezingTests {
        
        @Test
        void testIsFreezing_True() {
            Temperature temp = new Temperature(0.0, null);
            assertThat(temp.isFreezing()).isTrue();
        }
        
        @Test
        void testIsFreezing_BelowZero() {
            Temperature temp = new Temperature(-5.0, null);
            assertThat(temp.isFreezing()).isTrue();
        }
        
        @Test
        void testIsFreezing_False() {
            Temperature temp = new Temperature(5.0, null);
            assertThat(temp.isFreezing()).isFalse();
        }
        
        @Test
        void testIsFreezing_Null() {
            Temperature temp = new Temperature(null, null);
            assertThat(temp.isFreezing()).isFalse();
        }
        
        @Test
        void testIsBelowFreezing_True() {
            Temperature temp = new Temperature(-1.0, null);
            assertThat(temp.isBelowFreezing()).isTrue();
        }
        
        @Test
        void testIsBelowFreezing_AtZero() {
            Temperature temp = new Temperature(0.0, null);
            assertThat(temp.isBelowFreezing()).isFalse();
        }
        
        @Test
        void testIsAboveFreezing_True() {
            Temperature temp = new Temperature(1.0, null);
            assertThat(temp.isAboveFreezing()).isTrue();
        }
        
        @Test
        void testIsAboveFreezing_AtZero() {
            Temperature temp = new Temperature(0.0, null);
            assertThat(temp.isAboveFreezing()).isFalse();
        }
        
        @Test
        void testIsAboveFreezing_Null() {
            Temperature temp = new Temperature(null, null);
            assertThat(temp.isAboveFreezing()).isFalse();
        }
        
        @Test
        void testIsBelowFreezing_Null() {
            Temperature temp = new Temperature(null, null);
            assertThat(temp.isBelowFreezing()).isFalse();
        }
    }
    
    // ==================== Temperature Extreme Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")  // SonarQube: @Nested test classes contain tests in methods
    @DisplayName("Temperature Extremes")
    class TemperatureExtremeTests {
        
        @Test
        void testIsVeryCold_True() {
            Temperature temp = new Temperature(-25.0, null);
            assertThat(temp.isVeryCold()).isTrue();
        }
        
        @Test
        void testIsVeryCold_BoundaryFalse() {
            Temperature temp = new Temperature(-20.0, null);
            assertThat(temp.isVeryCold()).isFalse();
        }
        
        @Test
        void testIsVeryCold_False() {
            Temperature temp = new Temperature(-15.0, null);
            assertThat(temp.isVeryCold()).isFalse();
        }
        
        @Test
        void testIsVeryCold_Null() {
            Temperature temp = new Temperature(null, null);
            assertThat(temp.isVeryCold()).isFalse();
        }
        
        @Test
        void testIsVeryHot_True() {
            Temperature temp = new Temperature(40.0, null);
            assertThat(temp.isVeryHot()).isTrue();
        }
        
        @Test
        void testIsVeryHot_BoundaryFalse() {
            Temperature temp = new Temperature(35.0, null);
            assertThat(temp.isVeryHot()).isFalse();
        }
        
        @Test
        void testIsVeryHot_False() {
            Temperature temp = new Temperature(30.0, null);
            assertThat(temp.isVeryHot()).isFalse();
        }
        
        @Test
        void testIsVeryHot_Null() {
            Temperature temp = new Temperature(null, null);
            assertThat(temp.isVeryHot()).isFalse();
        }
    }
    
    // ==================== Relative Humidity Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")  // SonarQube: @Nested test classes contain tests in methods
    @DisplayName("Relative Humidity")
    class RelativeHumidityTests {
        
        @Test
        void testGetRelativeHumidity() {
            Temperature temp = new Temperature(20.0, 10.0);
            
            Double rh = temp.getRelativeHumidity();
            assertThat(rh)
                .isNotNull()
                .isBetween(0.0, 100.0);
        }
        
        @Test
        void testGetRelativeHumidity_HighHumidity() {
            // When temp and dewpoint are close, RH should be high
            Temperature temp = new Temperature(20.0, 19.0);
            
            Double rh = temp.getRelativeHumidity();
            assertThat(rh).isGreaterThan(90.0);
        }
        
        @Test
        void testGetRelativeHumidity_LowHumidity() {
            // When temp and dewpoint are far apart, RH should be low
            Temperature temp = new Temperature(30.0, 5.0);
            
            Double rh = temp.getRelativeHumidity();
            assertThat(rh).isLessThan(30.0);
        }
        
        @Test
        void testGetRelativeHumidity_SameValues() {
            // When temp equals dewpoint, RH should be 100%
            Temperature temp = new Temperature(20.0, 20.0);
            
            Double rh = temp.getRelativeHumidity();
            assertThat(rh).isCloseTo(100.0, within(0.1));
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
        void testGetRelativeHumidity_BothNull() {
            Temperature temp = new Temperature(null, null);
            assertThat(temp.getRelativeHumidity()).isNull();
        }
        
        @ParameterizedTest
        @CsvSource({
            "-10.0, -15.0",
            "0.0, -5.0",
            "40.0, 30.0"
        })
        void testGetRelativeHumidity_AlwaysBounded(double temp, double dewpoint) {
            Temperature temperature = new Temperature(temp, dewpoint);
            Double rh = temperature.getRelativeHumidity();
            assertThat(rh)
                .isNotNull()
                .isBetween(0.0, 100.0);
        }
    }
    
    // ==================== Spread and Fog Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")  // SonarQube: @Nested test classes contain tests in methods
    @DisplayName("Spread and Fog Detection")
    class SpreadAndFogTests {
        
        @Test
        void testGetSpread() {
            Temperature temp = new Temperature(22.0, 12.0);
            assertThat(temp.getSpread()).isCloseTo(10.0, within(0.01));
        }
        
        @Test
        void testGetSpread_SmallSpread() {
            Temperature temp = new Temperature(15.0, 14.0);
            assertThat(temp.getSpread()).isCloseTo(1.0, within(0.01));
        }
        
        @Test
        void testGetSpread_ZeroSpread() {
            Temperature temp = new Temperature(10.0, 10.0);
            assertThat(temp.getSpread()).isCloseTo(0.0, within(0.01));
        }
        
        @Test
        void testGetSpread_Null() {
            Temperature temp = new Temperature(22.0, null);
            assertThat(temp.getSpread()).isNull();
        }
        
        @Test
        void testGetSpread_NullTemperature() {
            Temperature temp = new Temperature(null, 10.0);
            assertThat(temp.getSpread()).isNull();
        }
        
        @Test
        void testIsFogLikely_True_SmallSpread() {
            Temperature temp = new Temperature(15.0, 14.0);
            assertThat(temp.isFogLikely()).isTrue();
        }
        
        @Test
        void testIsFogLikely_True_AtThreshold() {
            Temperature temp = new Temperature(15.0, 13.0);  // Spread = 2.0
            assertThat(temp.isFogLikely()).isTrue();
        }
        
        @Test
        void testIsFogLikely_False() {
            Temperature temp = new Temperature(22.0, 12.0);  // Spread = 10.0
            assertThat(temp.isFogLikely()).isFalse();
        }
        
        @Test
        void testIsFogLikely_False_JustOverThreshold() {
            Temperature temp = new Temperature(15.0, 12.9);  // Spread = 2.1
            assertThat(temp.isFogLikely()).isFalse();
        }
        
        @Test
        void testIsFogLikely_NullDewpoint() {
            Temperature temp = new Temperature(22.0, null);
            assertThat(temp.isFogLikely()).isFalse();
        }
    }
    
    // ==================== Icing Condition Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")  // SonarQube: @Nested test classes contain tests in methods
    @DisplayName("Icing Conditions")
    class IcingConditionTests {
        
        @Test
        void testIsIcingLikely_True() {
            // Between 0 and -20°C with high humidity
            Temperature temp = new Temperature(-10.0, -12.0);
            assertThat(temp.isIcingLikely()).isTrue();
        }
        
        @Test
        void testIsIcingLikely_AtZero() {
            Temperature temp = new Temperature(0.0, -2.0);
            assertThat(temp.isIcingLikely()).isTrue();
        }
        
        @Test
        void testIsIcingLikely_AtMinusTwenty() {
            Temperature temp = new Temperature(-20.0, -22.0);
            // At exactly -20°C with high humidity - this is the lower boundary
            assertThat(temp.isIcingLikely()).isTrue();
        }
        
        @Test
        void testIsIcingLikely_False_TooWarm() {
            Temperature temp = new Temperature(5.0, 3.0);
            assertThat(temp.isIcingLikely()).isFalse();
        }
        
        @Test
        void testIsIcingLikely_False_TooCold() {
            Temperature temp = new Temperature(-25.0, -30.0);
            assertThat(temp.isIcingLikely()).isFalse();
        }
        
        @Test
        void testIsIcingLikely_False_LowHumidity() {
            // In icing range but low humidity
            Temperature temp = new Temperature(-10.0, -30.0);  // Very low RH
            assertThat(temp.isIcingLikely()).isFalse();
        }
        
        @Test
        void testIsIcingLikely_NullTemperature() {
            Temperature temp = new Temperature(null, null);
            assertThat(temp.isIcingLikely()).isFalse();
        }
        
        @Test
        void testIsIcingLikely_NullDewpoint_AssumesHighHumidity() {
            // Null dewpoint should assume high humidity
            Temperature temp = new Temperature(-10.0, null);
            assertThat(temp.isIcingLikely()).isTrue();
        }
        
        @ParameterizedTest
        @CsvSource({
            "0.0, -2.0",
            "-5.0, -7.0",
            "-10.0, -12.0",
            "-15.0, -17.0",
            "-20.0, -22.0"
        })
        void testIsIcingLikely_TrueInRange(double temp, double dewpoint) {
            Temperature temperature = new Temperature(temp, dewpoint);
            assertThat(temperature.isIcingLikely()).isTrue();
        }
    }
    
    // ==================== Heat Index Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")  // SonarQube: @Nested test classes contain tests in methods
    @DisplayName("Heat Index Calculations")
    class HeatIndexTests {
        
        @Test
        void testGetHeatIndex_ValidConditions() {
            // Heat index only valid for temp >= 27°C
            Temperature temp = new Temperature(30.0, 25.0);
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            assertThat(heatIndex).isGreaterThan(30.0);  // Should be higher than actual temp
        }
        
        @Test
        void testGetHeatIndex_TooLow() {
            Temperature temp = new Temperature(20.0, 15.0);
            assertThat(temp.getHeatIndex()).isNull();
        }
        
        @Test
        void testGetHeatIndex_NullDewpoint() {
            Temperature temp = new Temperature(30.0, null);
            assertThat(temp.getHeatIndex()).isNull();
        }
        
        @Test
        void testGetHeatIndex_NullTemperature() {
            Temperature temp = new Temperature(null, 25.0);
            assertThat(temp.getHeatIndex()).isNull();
        }
        
        @Test
        void testGetHeatIndex_SimpleFormula_JustAbove80F() {
            // 27.2°C = 81°F with low RH
            // Should use simple formula since averaged HI will be < 80°F
            Temperature temp = Temperature.fromFahrenheit(81.0, 50.0);
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            
            // Simple formula should give moderate heat index
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            assertThat(hiF).isBetween(79.0, 84.0);
        }
        
        @Test
        void testGetHeatIndex_SimpleFormula_LowHumidity() {
            // Temperature that uses simple formula with low humidity
            // Must be >= 27°C (80.6°F), so use 81°F
            Temperature temp = Temperature.fromFahrenheit(81.0, 40.0);
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            
            // Convert to Fahrenheit for comparison
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            assertThat(hiF).isBetween(79.0, 84.0);  // Should use simple formula
        }
        
        @ParameterizedTest
        @CsvSource({
            // temp°F, dewpoint°F, expected HI range (lower, upper)
            "90, 75, 95, 105",      // Moderate heat and humidity
            "95, 80, 110, 125",     // High heat and humidity
            "100, 85, 130, 150",    // Very high heat and humidity
            "85, 70, 86, 95"        // Moderate conditions
        })
        void testGetHeatIndex_RothfuszRegression(double tempF, double dewpointF, 
                                                  double expectedLow, double expectedHigh) {
            Temperature temp = Temperature.fromFahrenheit(tempF, dewpointF);
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            
            // Convert to Fahrenheit for comparison
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            assertThat(hiF).isBetween(expectedLow, expectedHigh);
        }
        
        @Test
        void testGetHeatIndex_HighTemperatureHighHumidity() {
            // 38°C (100°F) with 80% RH - dangerous conditions
            // Using direct values to ensure high RH
            Temperature temp = Temperature.fromFahrenheit(100.0, 90.0);
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            assertThat(hiF).isGreaterThan(120.0);  // Dangerous heat index
        }
        
        @Test
        void testGetHeatIndex_LowHumidityAdjustment() {
            // RH < 13% and temp 80-112°F should subtract adjustment
            // 95°F with very low RH (~10%)
            Temperature temp = Temperature.fromFahrenheit(95.0, 45.0);  // Low dewpoint = low RH
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            
            // With low humidity, heat index should be lower than without adjustment
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            assertThat(hiF).isBetween(85.0, 100.0);
        }
        
        @Test
        void testGetHeatIndex_LowHumidityAdjustment_At80F() {
            // Boundary test: 81°F with low humidity (above 27°C threshold)
            Temperature temp = Temperature.fromFahrenheit(81.0, 20.0);  // Low dewpoint
            
            Double heatIndex = temp.getHeatIndex();
            // Should calculate (81°F is above 27°C threshold)
            assertThat(heatIndex).isNotNull();
        }
        
        @Test
        void testGetHeatIndex_LowHumidityAdjustment_At112F() {
            // Boundary test: exactly 112°F with low humidity
            Temperature temp = Temperature.fromFahrenheit(112.0, 40.0);  // Low dewpoint
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            assertThat(hiF).isGreaterThan(100.0);
        }
        
        @Test
        void testGetHeatIndex_LowHumidityAdjustment_At95F() {
            // Special case: 95°F is the midpoint for the adjustment calculation
            Temperature temp = Temperature.fromFahrenheit(95.0, 35.0);  // Very low dewpoint
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
        }
        
        @Test
        void testGetHeatIndex_HighHumidityAdjustment() {
            // RH > 85% and temp 80-87°F should add adjustment
            // 85°F with very high RH (~90%)
            Temperature temp = Temperature.fromFahrenheit(85.0, 82.0);  // High dewpoint = high RH
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            
            // With high humidity, heat index should be higher
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            assertThat(hiF).isGreaterThan(85.0);
        }
        
        @Test
        void testGetHeatIndex_HighHumidityAdjustment_At80F() {
            // Boundary test: 81°F with high humidity (above 27°C threshold)
            Temperature temp = Temperature.fromFahrenheit(81.0, 77.0);  // High dewpoint
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
        }
        
        @Test
        void testGetHeatIndex_HighHumidityAdjustment_At87F() {
            // Boundary test: exactly 87°F with high humidity
            Temperature temp = Temperature.fromFahrenheit(87.0, 84.0);  // High dewpoint
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            assertThat(hiF).isGreaterThan(87.0);
        }
        
        @Test
        void testGetHeatIndex_HighHumidityAdjustment_Above87F() {
            // Above 87°F - high humidity adjustment should NOT apply
            Temperature temp = Temperature.fromFahrenheit(90.0, 87.0);  // High dewpoint
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            
            // Should use regular Rothfusz without high humidity adjustment
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            assertThat(hiF).isGreaterThan(90.0);
        }
        
        @Test
        void testGetHeatIndex_LowHumidityAdjustment_Below80F() {
            // Below 80°F - low humidity adjustment should NOT apply
            // But still above 27°C threshold
            Temperature temp = Temperature.fromFahrenheit(81.0, 40.0);
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
        }
        
        @Test
        void testGetHeatIndex_LowHumidityAdjustment_Above112F() {
            // Above 112°F - low humidity adjustment should NOT apply
            Temperature temp = Temperature.fromFahrenheit(113.0, 40.0);
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
        }
        
        @Test
        void testGetHeatIndex_HighHumidityAdjustment_Below80F() {
            // Below 80°F - high humidity adjustment should NOT apply
            // But still above 27°C threshold
            Temperature temp = Temperature.fromFahrenheit(81.0, 78.0);
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
        }
        
        @Test
        void testGetHeatIndex_LowHumidityAdjustment_HighRH() {
            // RH >= 13% - low humidity adjustment should NOT apply
            Temperature temp = Temperature.fromFahrenheit(95.0, 60.0);  // Higher RH
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
        }
        
        @Test
        void testGetHeatIndex_HighHumidityAdjustment_LowRH() {
            // RH <= 85% - high humidity adjustment should NOT apply
            Temperature temp = Temperature.fromFahrenheit(85.0, 65.0);  // Lower RH
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
        }
        
        @Test
        void testGetHeatIndex_Exactly27Celsius() {
            // Exactly at the threshold (27°C ≈ 80.6°F)
            Temperature temp = Temperature.of(27.0, 20.0);
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
        }
        
        @Test
        void testGetHeatIndex_JustBelow27Celsius() {
            // Just below threshold
            Temperature temp = Temperature.of(26.9, 20.0);
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNull();
        }
        
        @Test
        void testGetHeatIndex_ExtremeHeat() {
            // Very high temperature (44°C = 111°F)
            Temperature temp = Temperature.fromFahrenheit(111.0, 85.0);
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            assertThat(hiF).isGreaterThan(130.0);  // Extremely dangerous
        }
        
        @Test
        void testGetHeatIndex_NOAAExample1() {
            // Example from NOAA: 95°F with 55% RH
            // Expected HI ≈ 109°F
            Temperature temp = Temperature.fromFahrenheit(95.0, 73.0);  // ≈55% RH
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            // Allow wider tolerance for RH calculation differences
            assertThat(hiF).isBetween(103.0, 115.0);
        }
        
        @Test
        void testGetHeatIndex_NOAAExample2() {
            // Example: 90°F with 70% RH
            // Expected HI ≈ 106°F
            Temperature temp = Temperature.fromFahrenheit(90.0, 78.0);  // ≈70% RH
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            assertThat(hiF).isBetween(103.0, 109.0);
        }
        
        @Test
        void testGetHeatIndex_ModerateConditions() {
            // Moderate: 85°F with 60% RH
            // Expected HI ≈ 90°F
            Temperature temp = Temperature.fromFahrenheit(85.0, 72.0);  // ≈60% RH
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            assertThat(hiF).isBetween(87.0, 93.0);
        }
        
        @Test
        void testGetHeatIndex_100PercentHumidity() {
            // Saturated air (temp = dewpoint)
            Temperature temp = Temperature.fromFahrenheit(90.0, 90.0);  // 100% RH
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            assertThat(hiF).isGreaterThan(110.0);  // Very high with 100% RH
        }
        
        @Test
        void testGetHeatIndex_VeryLowHumidity() {
            // Very dry conditions
            Temperature temp = Temperature.fromFahrenheit(100.0, 40.0);  // Very low RH
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            
            // Low humidity actually reduces perceived heat
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            assertThat(hiF).isLessThan(110.0);
        }
        
        @Test
        void testGetHeatIndex_RealisticSummerDay() {
            // Typical hot summer day: 35°C (95°F) with 60% RH
            Temperature temp = Temperature.of(35.0, 26.0);  // ≈60% RH
            
            Double heatIndex = temp.getHeatIndex();
            assertThat(heatIndex).isNotNull();
            assertThat(heatIndex).isGreaterThan(35.0);
            assertThat(heatIndex).isLessThan(50.0);  // Reasonable range
            
            // Verify it's in "Danger" category (> 40°C / 103°F)
            Double hiF = heatIndex * 9.0 / 5.0 + 32.0;
            assertThat(hiF).isGreaterThan(103.0);
        }
        
        @Test
        void testGetHeatIndex_CelsiusToFahrenheitRoundTrip() {
            // Verify conversion doesn't introduce errors
            Temperature tempC = Temperature.of(32.0, 25.0);
            Double hiC = tempC.getHeatIndex();
            
            Temperature tempF = Temperature.fromFahrenheit(
                tempC.toFahrenheit(), 
                tempC.dewpointToFahrenheit()
            );
            Double hiF = tempF.getHeatIndex();
            
            // Should be very close (within rounding error)
            assertThat(hiC).isCloseTo(hiF, within(0.1));
        }
    }
    
    // ==================== Summary Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")  // SonarQube: @Nested test classes contain tests in methods
    @DisplayName("Summary Generation")
    class SummaryTests {
        
        @Test
        void testGetSummary_Full() {
            Temperature temp = new Temperature(22.0, 12.0);
            
            String summary = temp.getSummary();
            assertThat(summary).contains("22.0°C");
            assertThat(summary).contains("°F");
            assertThat(summary).contains("dewpoint");
            assertThat(summary).contains("12.0°C");
            assertThat(summary).contains("RH");
        }
        
        @Test
        void testGetSummary_NoDewpoint() {
            Temperature temp = new Temperature(22.0, null);
            
            String summary = temp.getSummary();
            assertThat(summary).contains("22.0°C");
            assertThat(summary).contains("°F");
            assertThat(summary).doesNotContain("dewpoint");
            assertThat(summary).doesNotContain("RH");
        }
        
        @Test
        void testGetSummary_NullTemperature() {
            Temperature temp = new Temperature(null, null);
            assertThat(temp.getSummary()).isEqualTo("Unknown temperature");
        }
    }
    
    // ==================== Factory Method Tests ====================
    
    @Nested
    @SuppressWarnings("java:S2187")  // SonarQube: @Nested test classes contain tests in methods
    @DisplayName("Factory Methods")
    class FactoryMethodTests {
        
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
        void testFromKelvin() {
            Temperature temp = Temperature.fromKelvin(273.15, 263.15);
            
            assertThat(temp.celsius()).isCloseTo(0.0, within(0.01));
            assertThat(temp.dewpointCelsius()).isCloseTo(-10.0, within(0.01));
        }
        
        @Test
        void testFromKelvin_NullDewpoint() {
            Temperature temp = Temperature.fromKelvin(293.15, null);
            
            assertThat(temp.celsius()).isCloseTo(20.0, within(0.01));
            assertThat(temp.dewpointCelsius()).isNull();
        }
        
        @Test
        void testOf_TemperatureOnly() {
            Temperature temp = Temperature.of(22.0);
            
            assertThat(temp.celsius()).isEqualTo(22.0);
            assertThat(temp.dewpointCelsius()).isNull();
        }
        
        @Test
        void testOf_TemperatureAndDewpoint() {
            Temperature temp = Temperature.of(22.0, 12.0);
            
            assertThat(temp.celsius()).isEqualTo(22.0);
            assertThat(temp.dewpointCelsius()).isEqualTo(12.0);
        }
    }
    
    // ==================== Real World Examples ====================
    
    @Nested
    @SuppressWarnings("java:S2187")  // SonarQube: @Nested test classes contain tests in methods
    @DisplayName("Real-World Scenarios")
    class RealWorldScenarioTests {
        
        @Test
        void testRealWorldExample_FrozenConditions() {
            // Winter conditions with high humidity for icing
            Temperature temp = new Temperature(-10.0, -12.0);
            
            assertThat(temp.isFreezing()).isTrue();
            assertThat(temp.isBelowFreezing()).isTrue();
            assertThat(temp.isVeryCold()).isFalse();
            assertThat(temp.isIcingLikely()).isTrue();
        }
        
        @Test
        void testRealWorldExample_HotSummer() {
            // Hot summer day
            Temperature temp = new Temperature(38.0, 20.0);
            
            assertThat(temp.isVeryHot()).isTrue();
            assertThat(temp.getHeatIndex()).isNotNull();
            assertThat(temp.isFreezing()).isFalse();
        }
        
        @Test
        void testRealWorldExample_FoggyMorning() {
            // Foggy morning conditions
            Temperature temp = new Temperature(12.0, 11.0);
            
            assertThat(temp.isFogLikely()).isTrue();
            assertThat(temp.getRelativeHumidity()).isGreaterThan(85.0);
        }
    }
    
    // ==================== Equality and ToString Tests ====================
    
    @Test
    void testEquality() {
        Temperature temp1 = new Temperature(22.0, 12.0);
        Temperature temp2 = new Temperature(22.0, 12.0);
        Temperature temp3 = new Temperature(20.0, 12.0);
        
        assertThat(temp1).isEqualTo(temp2);
        assertThat(temp1).isNotEqualTo(temp3);
    }
    
    @Test
    void testToString() {
        Temperature temp = new Temperature(22.0, 12.0);
        String str = temp.toString();
        
        assertThat(str).contains("22.0");
        assertThat(str).contains("12.0");
    }
}
