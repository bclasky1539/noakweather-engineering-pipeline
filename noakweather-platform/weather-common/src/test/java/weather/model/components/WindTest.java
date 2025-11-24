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
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Comprehensive tests for Wind record.
 * 
 * Tests cover:
 * - Validation (direction, speed, gusts, variability, units)
 * - Query methods (isVariable, hasGusts, isCalm, isStrongWind, isGale)
 * - Conversion methods (unit conversions, cardinal directions, Beaufort scale)
 * - getSummary() formatting
 * - Factory methods
 * - Real-world METAR examples
 * 
 * @author bclasky1539
 * 
 */
class WindTest {
    
    // ==================== Basic Construction Tests ====================
    
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
    
    // ==================== Validation Tests - Direction ====================
    
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
    
    @ParameterizedTest
    @ValueSource(ints = {0, 90, 180, 270, 360})
    void testValidDirection_BoundaryValues(int direction) {
        Wind wind = new Wind(direction, 10, null, null, null, "KT");
        assertThat(wind.directionDegrees()).isEqualTo(direction);
    }
    
    // ==================== Validation Tests - Speed ====================
    
    @Test
    void testInvalidSpeed_Negative() {
        assertThatThrownBy(() -> new Wind(280, -5, null, null, null, "KT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Wind speed cannot be negative");
    }
    
    @Test
    void testValidSpeed_Zero() {
        Wind wind = new Wind(null, 0, null, null, null, "KT");
        assertThat(wind.speedValue()).isZero();
    }
    
    // ==================== Validation Tests - Gusts ====================
    
    @Test
    void testInvalidGust_Negative() {
        assertThatThrownBy(() -> new Wind(280, 10, -5, null, null, "KT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Gust speed cannot be negative");
    }
    
    @Test
    void testInvalidGust_LessThanSpeed() {
        assertThatThrownBy(() -> new Wind(280, 20, 15, null, null, "KT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Gust speed (15) must be greater than sustained wind speed (20)");
    }
    
    @Test
    void testInvalidGust_EqualToSpeed() {
        assertThatThrownBy(() -> new Wind(280, 20, 20, null, null, "KT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Gust speed (20) must be greater than sustained wind speed (20)");
    }
    
    @Test
    void testValidGust_GreaterThanSpeed() {
        Wind wind = new Wind(280, 20, 30, null, null, "KT");
        assertThat(wind.gustValue()).isEqualTo(30);
    }
    
    // ==================== Validation Tests - Variability ====================
    
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
    
    @ParameterizedTest
    @ValueSource(ints = {0, 90, 180, 270, 360})
    void testValidVariability_BoundaryValues(int degrees) {
        Wind wind = new Wind(280, 10, null, degrees, 320, "KT");
        assertThat(wind.variabilityFrom()).isEqualTo(degrees);
        
        Wind wind2 = new Wind(280, 10, null, 240, degrees, "KT");
        assertThat(wind2.variabilityTo()).isEqualTo(degrees);
    }
    
    // ==================== Validation Tests - Unit ====================
    
    @Test
    void testInvalidUnit_Null() {
        assertThatThrownBy(() -> new Wind(280, 10, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Wind speed unit cannot be null or blank");
    }
    
    @Test
    void testInvalidUnit_Blank() {
        assertThatThrownBy(() -> new Wind(280, 10, null, null, null, "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Wind speed unit cannot be null or blank");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"kt", "mps", "kmh", "MPH", "KTS", "KNOTS", "ABC", "XYZ"})
    void testInvalidUnit_InvalidValues(String invalidUnit) {
        assertThatThrownBy(() -> new Wind(280, 10, null, null, null, invalidUnit))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid wind speed unit");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"KT", "MPS", "KMH"})
    void testValidUnit(String validUnit) {
        Wind wind = new Wind(280, 10, null, null, null, validUnit);
        assertThat(wind.unit()).isEqualTo(validUnit);
    }
    
    // ==================== Query Method Tests ====================
    
    @Test
    void testIsVariable_True() {
        Wind wind = new Wind(280, 16, null, 240, 320, "KT");
        assertThat(wind.isVariable()).isTrue();
    }
    
    @Test
    void testIsVariable_False_BothNull() {
        Wind wind = new Wind(280, 16, null, null, null, "KT");
        assertThat(wind.isVariable()).isFalse();
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
    void testIsCalm_False_HasDirection() {
        Wind wind = new Wind(280, 0, null, null, null, "KT");
        assertThat(wind.isCalm()).isFalse();
    }
    
    @Test
    void testIsCalm_False_HasSpeed() {
        Wind wind = new Wind(null, 5, null, null, null, "KT");
        assertThat(wind.isCalm()).isFalse();
    }
    
    @Test
    void testIsStrongWind_True() {
        Wind wind = new Wind(280, 25, null, null, null, "KT");
        assertThat(wind.isStrongWind()).isTrue();
    }
    
    @Test
    void testIsStrongWind_True_JustAtThreshold() {
        Wind wind = new Wind(280, 22, null, null, null, "KT");
        assertThat(wind.isStrongWind()).isTrue();
    }
    
    @Test
    void testIsStrongWind_False() {
        Wind wind = new Wind(280, 20, null, null, null, "KT");
        assertThat(wind.isStrongWind()).isFalse();
    }
    
    @Test
    void testIsStrongWind_NullSpeed() {
        Wind wind = new Wind(280, null, null, null, null, "KT");
        assertThat(wind.isStrongWind()).isFalse();
    }
    
    @Test
    void testIsStrongWind_WithMPS_True() {
        // 13 m/s ≈ 25.3 knots (strong wind)
        Wind wind = new Wind(280, 13, null, null, null, "MPS");
        assertThat(wind.isStrongWind()).isTrue();
    }
    
    @Test
    void testIsGale_True() {
        Wind wind = new Wind(280, 34, null, null, null, "KT");
        assertThat(wind.isGale()).isTrue();
    }
    
    @Test
    void testIsGale_False() {
        Wind wind = new Wind(280, 33, null, null, null, "KT");
        assertThat(wind.isGale()).isFalse();
    }
    
    @Test
    void testIsGale_NullSpeed() {
        Wind wind = new Wind(280, null, null, null, null, "KT");
        assertThat(wind.isGale()).isFalse();
    }
    
    @Test
    void testIsGale_WithMPS_True() {
        // 18 m/s ≈ 35 knots (gale force)
        Wind wind = new Wind(280, 18, null, null, null, "MPS");
        assertThat(wind.isGale()).isTrue();
    }
    
    // ==================== Cardinal Direction Tests ====================
    
    @Test
    void testGetCardinalDirection_North() {
        Wind wind = new Wind(0, 10, null, null, null, "KT");
        assertThat(wind.getCardinalDirection()).isEqualTo("N");
    }
    
    @Test
    void testGetCardinalDirection_NorthAlternate() {
        Wind wind = new Wind(360, 10, null, null, null, "KT");
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
    void testGetCardinalDirection_NE() {
        Wind wind = new Wind(45, 10, null, null, null, "KT");
        assertThat(wind.getCardinalDirection()).isEqualTo("NE");
    }
    
    @Test
    void testGetCardinalDirection_SE() {
        Wind wind = new Wind(135, 10, null, null, null, "KT");
        assertThat(wind.getCardinalDirection()).isEqualTo("SE");
    }
    
    @Test
    void testGetCardinalDirection_SW() {
        Wind wind = new Wind(225, 10, null, null, null, "KT");
        assertThat(wind.getCardinalDirection()).isEqualTo("SW");
    }
    
    @Test
    void testGetCardinalDirection_NW() {
        Wind wind = new Wind(315, 10, null, null, null, "KT");
        assertThat(wind.getCardinalDirection()).isEqualTo("NW");
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
    
    // ==================== Unit Conversion Tests ====================
    
    @Test
    void testGetSpeedKnots_FromKT() {
        Wind wind = new Wind(280, 16, null, null, null, "KT");
        assertThat(wind.getSpeedKnots()).isEqualTo(16);
    }
    
    @Test
    void testGetSpeedKnots_FromMPS() {
        // 10 m/s ≈ 19.4 knots
        Wind wind = new Wind(280, 10, null, null, null, "MPS");
        assertThat(wind.getSpeedKnots()).isCloseTo(19, within(1));
    }
    
    @Test
    void testGetSpeedKnots_FromKMH() {
        // 30 km/h ≈ 16.2 knots
        Wind wind = new Wind(280, 30, null, null, null, "KMH");
        assertThat(wind.getSpeedKnots()).isCloseTo(16, within(1));
    }
    
    @Test
    void testGetSpeedKnots_NullSpeed() {
        Wind wind = new Wind(280, null, null, null, null, "KT");
        assertThat(wind.getSpeedKnots()).isNull();
    }
    
    @Test
    void testGetSpeedMps_FromKT() {
        // 20 knots ≈ 10.3 m/s
        Wind wind = new Wind(280, 20, null, null, null, "KT");
        assertThat(wind.getSpeedMps()).isCloseTo(10, within(1));
    }
    
    @Test
    void testGetSpeedMps_FromMPS() {
        Wind wind = new Wind(280, 10, null, null, null, "MPS");
        assertThat(wind.getSpeedMps()).isEqualTo(10);
    }
    
    @Test
    void testGetSpeedMps_FromKMH() {
        // 36 km/h = 10 m/s
        Wind wind = new Wind(280, 36, null, null, null, "KMH");
        assertThat(wind.getSpeedMps()).isCloseTo(10, within(1));
    }
    
    @Test
    void testGetSpeedMps_NullSpeed() {
        Wind wind = new Wind(280, null, null, null, null, "MPS");
        assertThat(wind.getSpeedMps()).isNull();
    }
    
    @Test
    void testGetSpeedKmh_FromKT() {
        // 10 knots ≈ 18.52 km/h
        Wind wind = new Wind(280, 10, null, null, null, "KT");
        assertThat(wind.getSpeedKmh()).isCloseTo(18.52, within(0.1));
    }
    
    @Test
    void testGetSpeedKmh_FromMPS() {
        // 10 m/s = 36 km/h
        Wind wind = new Wind(280, 10, null, null, null, "MPS");
        assertThat(wind.getSpeedKmh()).isCloseTo(36.0, within(0.1));
    }
    
    @Test
    void testGetSpeedKmh_FromKMH() {
        Wind wind = new Wind(280, 30, null, null, null, "KMH");
        assertThat(wind.getSpeedKmh()).isCloseTo(30.0, within(0.1));
    }
    
    @Test
    void testGetSpeedKmh_NullSpeed() {
        Wind wind = new Wind(280, null, null, null, null, "KMH");
        assertThat(wind.getSpeedKmh()).isNull();
    }
    
    @Test
    void testGetSpeedKnots_CaseInsensitiveUnit() {
        // Test that unit matching works correctly (uppercase required)
        Wind windKT = new Wind(280, 20, null, null, null, "KT");
        Wind windMPS = new Wind(280, 20, null, null, null, "MPS");
        Wind windKMH = new Wind(280, 20, null, null, null, "KMH");
        
        assertThat(windKT.getSpeedKnots()).isEqualTo(20);
        assertThat(windMPS.getSpeedKnots()).isNotNull();
        assertThat(windKMH.getSpeedKnots()).isNotNull();
    }
    
    @Test
    void testGetSpeedMps_AllUnits() {
        // Test conversions for all three units
        Wind windKT = new Wind(280, 20, null, null, null, "KT");
        Wind windMPS = new Wind(280, 10, null, null, null, "MPS");
        Wind windKMH = new Wind(280, 36, null, null, null, "KMH");
        
        assertThat(windKT.getSpeedMps()).isCloseTo(10, within(1));
        assertThat(windMPS.getSpeedMps()).isEqualTo(10);
        assertThat(windKMH.getSpeedMps()).isCloseTo(10, within(1));
    }
    
    @Test
    void testGetSpeedKmh_AllUnits() {
        // Test conversions for all three units
        Wind windKT = new Wind(280, 10, null, null, null, "KT");
        Wind windMPS = new Wind(280, 10, null, null, null, "MPS");
        Wind windKMH = new Wind(280, 30, null, null, null, "KMH");
        
        assertThat(windKT.getSpeedKmh()).isCloseTo(18.52, within(0.1));
        assertThat(windMPS.getSpeedKmh()).isCloseTo(36.0, within(0.1));
        assertThat(windKMH.getSpeedKmh()).isCloseTo(30.0, within(0.1));
    }
    
    // ==================== Beaufort Scale Tests ====================
    
    @ParameterizedTest
    @CsvSource({
        "0, 0",   // Calm
        "1, 1",   // Light air
        "3, 1",
        "4, 2",   // Light breeze
        "6, 2",
        "7, 3",   // Gentle breeze
        "10, 3",
        "11, 4",  // Moderate breeze
        "16, 4",
        "17, 5",  // Fresh breeze
        "21, 5",
        "22, 6",  // Strong breeze
        "27, 6",
        "28, 7",  // Near gale
        "33, 7",
        "34, 8",  // Gale
        "40, 8",
        "41, 9",  // Strong gale
        "47, 9",
        "48, 10", // Storm
        "55, 10",
        "56, 11", // Violent storm
        "63, 11",
        "64, 12", // Hurricane
        "100, 12"
    })
    void testGetBeaufortScale(int speedKnots, int expectedBeaufort) {
        Wind wind = new Wind(280, speedKnots, null, null, null, "KT");
        assertThat(wind.getBeaufortScale()).isEqualTo(expectedBeaufort);
    }
    
    @Test
    void testGetBeaufortScale_NullSpeed() {
        Wind wind = new Wind(280, null, null, null, null, "KT");
        assertThat(wind.getBeaufortScale()).isZero();
    }
    
    @Test
    void testGetBeaufortScale_WithMPS() {
        // 10 m/s ≈ 19.4 knots = Beaufort 5
        Wind wind = new Wind(280, 10, null, null, null, "MPS");
        assertThat(wind.getBeaufortScale()).isEqualTo(5);
    }
    
    // ==================== getSummary() Tests ====================
    
    @Test
    void testGetSummary_SimpleWind() {
        Wind wind = new Wind(280, 16, null, null, null, "KT");
        assertThat(wind.getSummary()).isEqualTo("280° at 16 KT");
    }
    
    @Test
    void testGetSummary_WithGusts() {
        Wind wind = new Wind(180, 16, 28, null, null, "KT");
        assertThat(wind.getSummary()).isEqualTo("180° at 16 KT gusting 28 KT");
    }
    
    @Test
    void testGetSummary_Variable() {
        Wind wind = new Wind(null, 3, null, null, null, "KT");
        assertThat(wind.getSummary()).isEqualTo("VRB at 3 KT");
    }
    
    @Test
    void testGetSummary_Calm() {
        Wind wind = Wind.calm();
        assertThat(wind.getSummary()).isEqualTo("CALM");
    }
    
    @Test
    void testGetSummary_WithVariability() {
        Wind wind = new Wind(280, 16, null, 240, 320, "KT");
        assertThat(wind.getSummary()).isEqualTo("280° at 16 KT (variable 240°-320°)");
    }
    
    @Test
    void testGetSummary_WithGustsAndVariability() {
        Wind wind = new Wind(280, 16, 28, 240, 320, "KT");
        assertThat(wind.getSummary()).isEqualTo("280° at 16 KT gusting 28 KT (variable 240°-320°)");
    }
    
    @Test
    void testGetSummary_MPS() {
        Wind wind = new Wind(280, 10, null, null, null, "MPS");
        assertThat(wind.getSummary()).isEqualTo("280° at 10 MPS");
    }
    
    @Test
    void testGetSummary_KMH() {
        Wind wind = new Wind(280, 30, null, null, null, "KMH");
        assertThat(wind.getSummary()).isEqualTo("280° at 30 KMH");
    }
    
    // ==================== Factory Method Tests ====================
    
    @Test
    void testFactoryMethod_Calm() {
        Wind wind = Wind.calm();
        
        assertThat(wind.isCalm()).isTrue();
        assertThat(wind.directionDegrees()).isNull();
        assertThat(wind.speedValue()).isZero();
        assertThat(wind.unit()).isEqualTo("KT");
    }
    
    @Test
    void testFactoryMethod_Variable() {
        Wind wind = Wind.variable(5, "KT");
        
        assertThat(wind.directionDegrees()).isNull();
        assertThat(wind.speedValue()).isEqualTo(5);
        assertThat(wind.unit()).isEqualTo("KT");
        assertThat(wind.getCardinalDirection()).isEqualTo("VRB");
    }
    
    @Test
    void testFactoryMethod_Of() {
        Wind wind = Wind.of(280, 16, "KT");
        
        assertThat(wind.directionDegrees()).isEqualTo(280);
        assertThat(wind.speedValue()).isEqualTo(16);
        assertThat(wind.gustValue()).isNull();
        assertThat(wind.unit()).isEqualTo("KT");
    }
    
    @Test
    void testFactoryMethod_OfWithGusts() {
        Wind wind = Wind.ofWithGusts(280, 16, 28, "KT");
        
        assertThat(wind.directionDegrees()).isEqualTo(280);
        assertThat(wind.speedValue()).isEqualTo(16);
        assertThat(wind.gustValue()).isEqualTo(28);
        assertThat(wind.unit()).isEqualTo("KT");
        assertThat(wind.hasGusts()).isTrue();
    }
    
    // ==================== Real-World METAR Examples ====================
    
    @Test
    void testRealWorldExample_28016KT() {
        // Standard wind: 280 degrees at 16 knots
        Wind wind = Wind.of(280, 16, "KT");
        
        assertThat(wind.directionDegrees()).isEqualTo(280);
        assertThat(wind.speedValue()).isEqualTo(16);
        assertThat(wind.getSummary()).isEqualTo("280° at 16 KT");
        assertThat(wind.getCardinalDirection()).isEqualTo("W");
    }
    
    @Test
    void testRealWorldExample_18016G28KT() {
        // Wind with gusts: 180 degrees at 16 knots gusting 28 knots
        Wind wind = Wind.ofWithGusts(180, 16, 28, "KT");
        
        assertThat(wind.hasGusts()).isTrue();
        assertThat(wind.getSummary()).isEqualTo("180° at 16 KT gusting 28 KT");
        assertThat(wind.getCardinalDirection()).isEqualTo("S");
    }
    
    @Test
    void testRealWorldExample_VRB03KT() {
        // Variable light wind
        Wind wind = Wind.variable(3, "KT");
        
        assertThat(wind.directionDegrees()).isNull();
        assertThat(wind.getSummary()).isEqualTo("VRB at 3 KT");
        assertThat(wind.getCardinalDirection()).isEqualTo("VRB");
    }
    
    @Test
    void testRealWorldExample_00000KT() {
        // Calm conditions
        Wind wind = Wind.calm();
        
        assertThat(wind.isCalm()).isTrue();
        assertThat(wind.getSummary()).isEqualTo("CALM");
    }
    
    @Test
    void testRealWorldExample_28016KT_240V320() {
        // Wind with directional variability
        Wind wind = new Wind(280, 16, null, 240, 320, "KT");
        
        assertThat(wind.isVariable()).isTrue();
        assertThat(wind.getSummary()).isEqualTo("280° at 16 KT (variable 240°-320°)");
    }
    
    // ==================== Equality and ToString Tests ====================
    
    @Test
    void testEquality_SameValues() {
        Wind wind1 = Wind.of(280, 16, "KT");
        Wind wind2 = Wind.of(280, 16, "KT");
        
        assertThat(wind1).isEqualTo(wind2);
        assertThat(wind1.hashCode()).hasSameHashCodeAs(wind2.hashCode());
    }
    
    @Test
    void testEquality_DifferentDirection() {
        Wind wind1 = Wind.of(280, 16, "KT");
        Wind wind2 = Wind.of(270, 16, "KT");
        
        assertThat(wind1).isNotEqualTo(wind2);
    }
    
    @Test
    void testToString_ContainsFields() {
        Wind wind = Wind.ofWithGusts(280, 16, 28, "KT");
        String str = wind.toString();
        
        assertThat(str).contains("280")
                .contains("16")
                .contains("28")
                .contains("KT");
    }
}
