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
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Comprehensive tests for enhanced Visibility record.
 * 
 * @author bclasky1539
 * 
 */
class VisibilityTest {
    
    // ==================== Construction and Validation Tests ====================
    
    @Test
    void testValidVisibility_StatuteMiles() {
        Visibility visibility = new Visibility(10.0, "SM", false, false, null);
        
        assertThat(visibility.distanceValue()).isEqualTo(10.0);
        assertThat(visibility.unit()).isEqualTo("SM");
        assertThat(visibility.lessThan()).isFalse();
        assertThat(visibility.greaterThan()).isFalse();
        assertThat(visibility.specialCondition()).isNull();
        assertThat(visibility.isSpecialCondition()).isFalse();
    }
    
    @Test
    void testValidVisibility_Meters() {
        Visibility visibility = new Visibility(9999.0, "M", false, false, null);
        
        assertThat(visibility.distanceValue()).isEqualTo(9999.0);
        assertThat(visibility.unit()).isEqualTo("M");
    }
    
    @Test
    void testValidVisibility_Kilometers() {
        Visibility visibility = new Visibility(10.0, "KM", false, false, null);
        
        assertThat(visibility.distanceValue()).isEqualTo(10.0);
        assertThat(visibility.unit()).isEqualTo("KM");
    }
    
    @Test
    void testValidVisibility_Cavok() {
        Visibility visibility = new Visibility(null, null, false, false, "CAVOK");
        
        assertThat(visibility.distanceValue()).isNull();
        assertThat(visibility.unit()).isNull();
        assertThat(visibility.specialCondition()).isEqualTo("CAVOK");
        assertThat(visibility.isSpecialCondition()).isTrue();
        assertThat(visibility.isCavok()).isTrue();
    }
    
    @Test
    void testValidVisibility_LessThan() {
        Visibility visibility = new Visibility(0.25, "SM", true, false, null);
        
        assertThat(visibility.lessThan()).isTrue();
        assertThat(visibility.greaterThan()).isFalse();
    }
    
    @Test
    void testValidVisibility_GreaterThan() {
        Visibility visibility = new Visibility(6.0, "SM", false, true, null);
        
        assertThat(visibility.lessThan()).isFalse();
        assertThat(visibility.greaterThan()).isTrue();
    }
    
    @Test
    void testInvalidVisibility_NegativeDistance() {
        assertThatThrownBy(() -> new Visibility(-1.0, "SM", false, false, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Visibility distance cannot be negative");
    }
    
    @Test
    void testInvalidVisibility_NoDistanceOrSpecialCondition() {
        assertThatThrownBy(() -> new Visibility(null, null, false, false, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Visibility must have either distance or special condition");
    }
    
    @Test
    void testInvalidVisibility_DistanceWithoutUnit() {
        assertThatThrownBy(() -> new Visibility(10.0, null, false, false, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Visibility unit must be specified when distance is provided");
    }
    
    @Test
    void testInvalidVisibility_DistanceWithBlankUnit() {
        assertThatThrownBy(() -> new Visibility(10.0, "  ", false, false, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Visibility unit must be specified when distance is provided");
    }
    
    @Test
    void testInvalidVisibility_InvalidUnit() {
        assertThatThrownBy(() -> new Visibility(10.0, "FEET", false, false, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid visibility unit");
    }
    
    @Test
    void testInvalidVisibility_InvalidSpecialCondition() {
        assertThatThrownBy(() -> new Visibility(null, null, false, false, "INVALID"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid visibility special condition");
    }
    
    @Test
    void testInvalidVisibility_BothLessThanAndGreaterThan() {
        assertThatThrownBy(() -> new Visibility(5.0, "SM", true, true, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Visibility cannot be both less than and greater than");
    }
    
    // ==================== Conversion Tests ====================
    
    @ParameterizedTest
    @CsvSource({
        "1.0, SM, 1609.34",
        "5.0, SM, 8046.7",
        "10.0, SM, 16093.4"
    })
    void testToMeters_FromStatuteMiles(double sm, String unit, double expectedMeters) {
        Visibility visibility = new Visibility(sm, unit, false, false, null);
        
        assertThat(visibility.toMeters()).isCloseTo(expectedMeters, within(0.1));
    }
    
    @Test
    void testToMeters_FromMeters() {
        Visibility visibility = new Visibility(5000.0, "M", false, false, null);
        
        assertThat(visibility.toMeters()).isEqualTo(5000.0);
    }
    
    @ParameterizedTest
    @CsvSource({
        "1.0, KM, 1000.0",
        "5.0, KM, 5000.0",
        "10.0, KM, 10000.0"
    })
    void testToMeters_FromKilometers(double km, String unit, double expectedMeters) {
        Visibility visibility = new Visibility(km, unit, false, false, null);
        
        assertThat(visibility.toMeters()).isEqualTo(expectedMeters);
    }
    
    @Test
    void testToStatuteMiles_FromStatuteMiles() {
        Visibility visibility = new Visibility(10.0, "SM", false, false, null);
        
        assertThat(visibility.toStatuteMiles()).isEqualTo(10.0);
    }
    
    @ParameterizedTest
    @CsvSource({
        "1609.34, M, 1.0",
        "8046.7, M, 5.0",
        "16093.4, M, 10.0"
    })
    void testToStatuteMiles_FromMeters(double meters, String unit, double expectedSM) {
        Visibility visibility = new Visibility(meters, unit, false, false, null);
        
        assertThat(visibility.toStatuteMiles()).isCloseTo(expectedSM, within(0.01));
    }
    
    @ParameterizedTest
    @CsvSource({
        "1.60934, KM, 1.0",
        "8.0467, KM, 5.0",
        "16.0934, KM, 10.0"
    })
    void testToStatuteMiles_FromKilometers(double km, String unit, double expectedSM) {
        Visibility visibility = new Visibility(km, unit, false, false, null);
        
        assertThat(visibility.toStatuteMiles()).isCloseTo(expectedSM, within(0.01));
    }
    
    @Test
    void testToKilometers_FromKilometers() {
        Visibility visibility = new Visibility(10.0, "KM", false, false, null);
        
        assertThat(visibility.toKilometers()).isEqualTo(10.0);
    }
    
    @ParameterizedTest
    @CsvSource({
        "1000.0, M, 1.0",
        "5000.0, M, 5.0",
        "10000.0, M, 10.0"
    })
    void testToKilometers_FromMeters(double meters, String unit, double expectedKM) {
        Visibility visibility = new Visibility(meters, unit, false, false, null);
        
        assertThat(visibility.toKilometers()).isEqualTo(expectedKM);
    }
    
    @ParameterizedTest
    @CsvSource({
        "1.0, SM, 1.60934",
        "5.0, SM, 8.0467",
        "10.0, SM, 16.0934"
    })
    void testToKilometers_FromStatuteMiles(double sm, String unit, double expectedKM) {
        Visibility visibility = new Visibility(sm, unit, false, false, null);
        
        assertThat(visibility.toKilometers()).isCloseTo(expectedKM, within(0.0001));
    }
    
    // ==================== Aviation Flight Rules Tests ====================
    
    @Test
    void testIsVFR_CavokCondition() {
        Visibility visibility = Visibility.cavok();
        
        assertThat(visibility.isVFR()).isTrue();
        assertThat(visibility.isIFR()).isFalse();
    }
    
    @ParameterizedTest
    @CsvSource({
        "10.0, SM, true",
        "5.0, SM, true",
        "3.0, SM, true",
        "2.99, SM, false",
        "1.0, SM, false",
        "0.5, SM, false"
    })
    void testIsVFR_VariousVisibilities(double distance, String unit, boolean expectedVFR) {
        Visibility visibility = new Visibility(distance, unit, false, false, null);
        
        assertThat(visibility.isVFR()).isEqualTo(expectedVFR);
    }
    
    @ParameterizedTest
    @CsvSource({
        "2.99, SM, true",
        "1.0, SM, true",
        "0.5, SM, true",
        "3.0, SM, false",
        "5.0, SM, false",
        "10.0, SM, false"
    })
    void testIsIFR_VariousVisibilities(double distance, String unit, boolean expectedIFR) {
        Visibility visibility = new Visibility(distance, unit, false, false, null);
        
        assertThat(visibility.isIFR()).isEqualTo(expectedIFR);
    }
    
    @Test
    void testIsIFR_LessThanModifier() {
        Visibility visibility = Visibility.lessThan(1.0, "SM");
        
        assertThat(visibility.isIFR()).isTrue();
        assertThat(visibility.isVFR()).isFalse();
    }
    
    @Test
    void testIsVFR_GreaterThanModifier() {
        Visibility visibility = Visibility.greaterThan(6.0, "SM");
        
        assertThat(visibility.isVFR()).isTrue();
        assertThat(visibility.isIFR()).isFalse();
    }
    
    // ==================== Special Condition Tests ====================
    
    @Test
    void testIsUnlimited_CavokCondition() {
        Visibility visibility = Visibility.cavok();
        
        assertThat(visibility.isUnlimited()).isTrue();
    }
    
    @ParameterizedTest
    @CsvSource({
        "10000.0, M, true",
        "15000.0, M, true",
        "9999.0, M, false",
        "5000.0, M, false"
    })
    void testIsUnlimited_MetersThreshold(double meters, String unit, boolean expectedUnlimited) {
        Visibility visibility = new Visibility(meters, unit, false, false, null);
        
        assertThat(visibility.isUnlimited()).isEqualTo(expectedUnlimited);
    }
    
    @ParameterizedTest
    @CsvSource({
        "10.0, SM, true",
        "6.0, SM, true",
        "5.99, SM, false",
        "3.0, SM, false"
    })
    void testIsUnlimited_StatuteMilesThreshold(double sm, String unit, boolean expectedUnlimited) {
        Visibility visibility = new Visibility(sm, unit, false, false, null);
        
        assertThat(visibility.isUnlimited()).isEqualTo(expectedUnlimited);
    }
    
    @ParameterizedTest
    @CsvSource({
        "0.99, SM, true",
        "0.5, SM, true",
        "0.25, SM, true",
        "1.0, SM, false",
        "2.0, SM, false",
        "5.0, SM, false"
    })
    void testIsLowVisibility(double distance, String unit, boolean expectedLow) {
        Visibility visibility = new Visibility(distance, unit, false, false, null);
        
        assertThat(visibility.isLowVisibility()).isEqualTo(expectedLow);
    }
    
    @Test
    void testIsLowVisibility_LessThanModifier() {
        Visibility visibility = Visibility.lessThan(5.0, "SM");
        
        assertThat(visibility.isLowVisibility()).isTrue();
    }
    
    @Test
    void testIsLowVisibility_CavokCondition() {
        Visibility visibility = Visibility.cavok();
        
        assertThat(visibility.isLowVisibility()).isFalse();
    }
    
    // ==================== Factory Method Tests ====================
    
    @Test
    void testFactoryMethod_Cavok() {
        Visibility visibility = Visibility.cavok();
        
        assertThat(visibility.isCavok()).isTrue();
        assertThat(visibility.isSpecialCondition()).isTrue();
        assertThat(visibility.specialCondition()).isEqualTo("CAVOK");
        assertThat(visibility.lessThan()).isFalse();
        assertThat(visibility.greaterThan()).isFalse();
    }
    
    @Test
    void testFactoryMethod_StatuteMiles() {
        Visibility visibility = Visibility.statuteMiles(5.0);
        
        assertThat(visibility.distanceValue()).isEqualTo(5.0);
        assertThat(visibility.unit()).isEqualTo("SM");
        assertThat(visibility.specialCondition()).isNull();
        assertThat(visibility.lessThan()).isFalse();
        assertThat(visibility.greaterThan()).isFalse();
    }
    
    @Test
    void testFactoryMethod_Meters() {
        Visibility visibility = Visibility.meters(9999.0);
        
        assertThat(visibility.distanceValue()).isEqualTo(9999.0);
        assertThat(visibility.unit()).isEqualTo("M");
        assertThat(visibility.specialCondition()).isNull();
    }
    
    @Test
    void testFactoryMethod_Kilometers() {
        Visibility visibility = Visibility.kilometers(10.0);
        
        assertThat(visibility.distanceValue()).isEqualTo(10.0);
        assertThat(visibility.unit()).isEqualTo("KM");
        assertThat(visibility.specialCondition()).isNull();
    }
    
    @Test
    void testFactoryMethod_LessThan() {
        Visibility visibility = Visibility.lessThan(0.25, "SM");
        
        assertThat(visibility.distanceValue()).isEqualTo(0.25);
        assertThat(visibility.unit()).isEqualTo("SM");
        assertThat(visibility.lessThan()).isTrue();
        assertThat(visibility.greaterThan()).isFalse();
        assertThat(visibility.specialCondition()).isNull();
    }
    
    @Test
    void testFactoryMethod_GreaterThan() {
        Visibility visibility = Visibility.greaterThan(6.0, "SM");
        
        assertThat(visibility.distanceValue()).isEqualTo(6.0);
        assertThat(visibility.unit()).isEqualTo("SM");
        assertThat(visibility.lessThan()).isFalse();
        assertThat(visibility.greaterThan()).isTrue();
        assertThat(visibility.specialCondition()).isNull();
    }
    
    @Test
    void testFactoryMethod_Of() {
        Visibility visibility = Visibility.of(3.0, "SM", false, false);
        
        assertThat(visibility.distanceValue()).isEqualTo(3.0);
        assertThat(visibility.unit()).isEqualTo("SM");
        assertThat(visibility.lessThan()).isFalse();
        assertThat(visibility.greaterThan()).isFalse();
    }
    
    // ==================== Summary Tests ====================
    
    @Test
    void testGetSummary_Cavok() {
        Visibility visibility = Visibility.cavok();
        
        assertThat(visibility.getSummary()).isEqualTo("CAVOK (>10km, clear skies)");
    }
    
    @Test
    void testGetSummary_StandardVisibility_VFR() {
        Visibility visibility = Visibility.statuteMiles(10.0);
        
        String summary = visibility.getSummary();
        assertThat(summary).contains("10.00")
                .contains("statute miles")
                .contains("(VFR)");
    }
    
    @Test
    void testGetSummary_StandardVisibility_IFR() {
        Visibility visibility = Visibility.statuteMiles(1.0);
        
        String summary = visibility.getSummary();
        assertThat(summary).contains("1.00")
                .contains("statute miles")
                .contains("(IFR)");
    }
    
    @Test
    void testGetSummary_LessThan() {
        Visibility visibility = Visibility.lessThan(0.25, "SM");
        
        String summary = visibility.getSummary();
        assertThat(summary).startsWith("Less than")
                .contains("0.25")
                .contains("statute miles")
                .contains("(IFR)");
    }
    
    @Test
    void testGetSummary_GreaterThan() {
        Visibility visibility = Visibility.greaterThan(6.0, "SM");
        
        String summary = visibility.getSummary();
        assertThat(summary).startsWith("Greater than")
                .contains("6.00")
                .contains("statute miles")
                .contains("(VFR)");
    }
    
    @Test
    void testGetSummary_Meters() {
        Visibility visibility = Visibility.meters(5000.0);
        
        String summary = visibility.getSummary();
        assertThat(summary).contains("5000.00")
                .contains("meters");
    }
    
    @Test
    void testGetSummary_Kilometers() {
        Visibility visibility = Visibility.kilometers(10.0);
        
        String summary = visibility.getSummary();
        assertThat(summary).contains("10.00")
                .contains("kilometers");
    }
    
    // ==================== Real World Examples ====================
    
    @Test
    void testRealWorldExample_10SM() {
        // 10SM - Standard VFR visibility
        Visibility visibility = Visibility.statuteMiles(10.0);
        
        assertThat(visibility.isVFR()).isTrue();
        assertThat(visibility.isIFR()).isFalse();
        assertThat(visibility.isUnlimited()).isTrue();
        assertThat(visibility.toMeters()).isCloseTo(16093.4, within(0.1));
    }
    
    @Test
    void testRealWorldExample_9999_Meters() {
        // 9999 - ICAO format for 10km or greater
        Visibility visibility = Visibility.meters(9999.0);
        
        assertThat(visibility.isVFR()).isTrue();
        assertThat(visibility.toStatuteMiles()).isCloseTo(6.21, within(0.01));
    }
    
    @Test
    void testRealWorldExample_M1_4SM() {
        // M1/4SM - Less than 1/4 statute mile
        Visibility visibility = Visibility.lessThan(0.25, "SM");
        
        assertThat(visibility.isIFR()).isTrue();
        assertThat(visibility.isVFR()).isFalse();
        assertThat(visibility.isLowVisibility()).isTrue();
    }
    
    @Test
    void testRealWorldExample_P6SM() {
        // P6SM - Greater than 6 statute miles
        Visibility visibility = Visibility.greaterThan(6.0, "SM");
        
        assertThat(visibility.isVFR()).isTrue();
        assertThat(visibility.isUnlimited()).isTrue();
    }
    
    @Test
    void testRealWorldExample_FractionalVisibility() {
        // 1 1/2SM - Fractional visibility
        Visibility visibility = Visibility.statuteMiles(1.5);
        
        assertThat(visibility.isIFR()).isTrue();
        assertThat(visibility.toMeters()).isCloseTo(2414.01, within(0.01));
    }
    
    @Test
    void testRealWorldExample_0000_Meters() {
        // 0000 - Zero visibility (fog)
        Visibility visibility = Visibility.meters(0.0);
        
        assertThat(visibility.isIFR()).isTrue();
        assertThat(visibility.isLowVisibility()).isTrue();
        assertThat(visibility.toStatuteMiles()).isEqualTo(0.0);
    }
    
    // ==================== Equality and ToString Tests ====================
    
    @Test
    void testEquality() {
        Visibility vis1 = Visibility.statuteMiles(10.0);
        Visibility vis2 = Visibility.statuteMiles(10.0);
        Visibility vis3 = Visibility.statuteMiles(5.0);
        
        assertThat(vis1).isEqualTo(vis2)
                .isNotEqualTo(vis3);
    }
    
    @Test
    void testEquality_WithModifiers() {
        Visibility vis1 = Visibility.lessThan(1.0, "SM");
        Visibility vis2 = Visibility.lessThan(1.0, "SM");
        Visibility vis3 = Visibility.greaterThan(1.0, "SM");
        
        assertThat(vis1).isEqualTo(vis2)
                .isNotEqualTo(vis3);
    }
    
    @Test
    void testToString() {
        Visibility visibility = Visibility.statuteMiles(10.0);
        String str = visibility.toString();
        
        assertThat(str).contains("10.0")
                .contains("SM");
    }
    
        // ==================== getSummary() - Missing Branches ====================
    
    @Test
    void testGetSummary_NDVSpecialCondition() {
        // Cover non-CAVOK special condition branch
        Visibility visibility = new Visibility(null, null, false, false, "NDV");
        
        assertThat(visibility.getSummary()).isEqualTo("NDV");
    }
    
    @Test
    void testGetSummary_MetersUnit_VFR() {
        // Cover "M" unit in switch statement with VFR
        Visibility visibility = Visibility.meters(5000.0);
        
        String summary = visibility.getSummary();
        assertThat(summary).contains("5000.00")
                .contains("meters")
                .contains("(VFR)");
    }
    
    @Test
    void testGetSummary_MetersUnit_IFR() {
        // Cover "M" unit in switch statement with IFR
        Visibility visibility = Visibility.meters(1000.0);
        
        String summary = visibility.getSummary();
        assertThat(summary).contains("1000.00")
                .contains("meters")
                .contains("(IFR)");
    }
    
    @Test
    void testGetSummary_KilometersUnit_VFR() {
        // Cover "KM" unit in switch statement with VFR
        Visibility visibility = Visibility.kilometers(10.0);
        
        String summary = visibility.getSummary();
        assertThat(summary).contains("10.00")
                .contains("kilometers")
                .contains("(VFR)");
    }
    
    @Test
    void testGetSummary_KilometersUnit_IFR() {
        // Cover "KM" unit in switch statement with IFR
        Visibility visibility = Visibility.kilometers(2.0);
        
        String summary = visibility.getSummary();
        assertThat(summary).contains("2.00")
                .contains("kilometers")
                .contains("(IFR)");
    }
    
    @Test
    void testGetSummary_NeitherVFRNorIFR() {
        // Tests the case where neither isVFR() nor isIFR() is true
        // This shouldn't happen with valid visibility data since:
        // - visibility >= 3 SM → VFR
        // - visibility < 3 SM → IFR
        // One of them is always true for numeric visibility
        
        // However, for special conditions like NDV (not CAVOK):
        Visibility visibility = new Visibility(null, null, false, false, "NDV");
        
        String summary = visibility.getSummary();
        
        // NDV should return just "NDV" before checking VFR/IFR
        assertThat(summary).isEqualTo("NDV");
        // This tests the early return, so VFR/IFR check is never reached
    }
    
    // ==================== isLowVisibility() - Missing Branches ====================
    
    @Test
    void testIsLowVisibility_ExactlyOneStatuteMile() {
        // Exactly at threshold - boundary case
        Visibility visibility = Visibility.statuteMiles(1.0);
        
        // 1.0 is NOT < 1.0, so should be false
        assertThat(visibility.isLowVisibility()).isFalse();
    }
    
    @Test
    void testIsLowVisibility_JustAboveThreshold() {
        Visibility visibility = Visibility.statuteMiles(1.01);
        
        assertThat(visibility.isLowVisibility()).isFalse();
    }
    
    @Test
    void testIsLowVisibility_MetersBelow() {
        // 800 meters < 1 SM (1609 meters)
        Visibility visibility = Visibility.meters(800.0);
        
        assertThat(visibility.isLowVisibility()).isTrue();
    }
    
    @Test
    void testIsLowVisibility_MetersAbove() {
        // 2000 meters > 1 SM (1609 meters)
        Visibility visibility = Visibility.meters(2000.0);
        
        assertThat(visibility.isLowVisibility()).isFalse();
    }
    
    @Test
    void testIsLowVisibility_KilometersBelow() {
        // 0.8 km < 1 SM (1.609 km)
        Visibility visibility = Visibility.kilometers(0.8);
        
        assertThat(visibility.isLowVisibility()).isTrue();
    }
    
    @Test
    void testIsLowVisibility_KilometersAbove() {
        // 5 km > 1 SM (1.609 km)
        Visibility visibility = Visibility.kilometers(5.0);
        
        assertThat(visibility.isLowVisibility()).isFalse();
    }
    
    @Test
    void testIsLowVisibility_GreaterThanModifier_BelowThreshold() {
        // Greater than 0.5 SM - the value 0.5 < 1.0
        Visibility visibility = Visibility.greaterThan(0.5, "SM");
        
        assertThat(visibility.isLowVisibility()).isTrue();
    }
    
    @Test
    void testIsLowVisibility_GreaterThanModifier_AboveThreshold() {
        // Greater than 2.0 SM - the value 2.0 > 1.0
        Visibility visibility = Visibility.greaterThan(2.0, "SM");
        
        assertThat(visibility.isLowVisibility()).isFalse();
    }
    
    @Test
    void testIsLowVisibility_ZeroVisibility() {
        Visibility visibility = Visibility.meters(0.0);
        
        assertThat(visibility.isLowVisibility()).isTrue();
    }
    
    // ==================== isSpecialCondition() - Edge Cases ====================
    
    @Test
    void testIsSpecialCondition_NullValue() {
        // Null special condition should return false
        Visibility visibility = Visibility.statuteMiles(10.0);
        
        assertThat(visibility.isSpecialCondition()).isFalse();
    }
    
    @Test
    void testIsSpecialCondition_ValidNDV() {
        // Non-null, non-blank special condition should return true
        Visibility visibility = new Visibility(null, null, false, false, "NDV");
        
        assertThat(visibility.isSpecialCondition()).isTrue();
    }
    
    // ==================== isIFR() - Missing Branches ====================
    
    @Test
    void testIsIFR_NullStatuteMiles() {
        // toStatuteMiles() returns null for special conditions
        Visibility visibility = new Visibility(null, null, false, false, "NDV");
        
        assertThat(visibility.isIFR()).isFalse();
    }
    
    @Test
    void testIsIFR_GreaterThanModifier_BelowThreshold() {
        // Greater than 2.0 SM, where 2.0 < 3.0
        Visibility visibility = Visibility.greaterThan(2.0, "SM");
        
        assertThat(visibility.isIFR()).isTrue();
    }
    
    @Test
    void testIsIFR_GreaterThanModifier_AtOrAboveThreshold() {
        // Greater than 3.0 SM, where 3.0 >= 3.0
        Visibility visibility = Visibility.greaterThan(3.0, "SM");
        
        assertThat(visibility.isIFR()).isFalse();
    }
    
    // ==================== isVFR() - Missing Branches ====================
    
    @Test
    void testIsVFR_NullStatuteMiles() {
        // toStatuteMiles() returns null for NDV
        Visibility visibility = new Visibility(null, null, false, false, "NDV");
        
        assertThat(visibility.isVFR()).isFalse();
    }
    
    @Test
    void testIsVFR_GreaterThanModifier_BelowMinimum() {
        // Greater than 2.0 SM, where 2.0 < 3.0
        Visibility visibility = Visibility.greaterThan(2.0, "SM");
        
        assertThat(visibility.isVFR()).isFalse();
    }
    
    @Test
    void testIsVFR_GreaterThanModifier_AtOrAboveMinimum() {
        // Greater than 3.0 SM, where 3.0 >= 3.0
        Visibility visibility = Visibility.greaterThan(3.0, "SM");
        
        assertThat(visibility.isVFR()).isTrue();
    }
    
    // ==================== toMeters() - Missing Branches ====================
    
    @Test
    void testToMeters_NullDistanceValue() {
        // Special condition with null distance
        Visibility visibility = Visibility.cavok();
        
        assertThat(visibility.toMeters()).isNull();
    }
    
    @Test
    void testToMeters_MetersUnit() {
        Visibility visibility = Visibility.meters(5000.0);
        
        assertThat(visibility.toMeters()).isEqualTo(5000.0);
    }
    
    @Test
    void testToMeters_KilometersUnit() {
        Visibility visibility = Visibility.kilometers(5.0);
        
        assertThat(visibility.toMeters()).isEqualTo(5000.0);
    }
    
    @Test
    void testToMeters_StatuteMilesUnit() {
        Visibility visibility = Visibility.statuteMiles(1.0);
        
        assertThat(visibility.toMeters()).isCloseTo(1609.34, org.assertj.core.data.Offset.offset(0.01));
    }
    
    // ==================== toStatuteMiles() - Missing Branches ====================
    
    @Test
    void testToStatuteMiles_NullDistanceValue() {
        // Special condition with null distance
        Visibility visibility = Visibility.cavok();
        
        assertThat(visibility.toStatuteMiles()).isNull();
    }
    
    @Test
    void testToStatuteMiles_StatuteMilesUnit() {
        Visibility visibility = Visibility.statuteMiles(10.0);
        
        assertThat(visibility.toStatuteMiles()).isEqualTo(10.0);
    }
    
    @Test
    void testToStatuteMiles_MetersUnit() {
        Visibility visibility = Visibility.meters(1609.34);
        
        assertThat(visibility.toStatuteMiles()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));
    }
    
    @Test
    void testToStatuteMiles_KilometersUnit() {
        Visibility visibility = Visibility.kilometers(1.60934);
        
        assertThat(visibility.toStatuteMiles()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));
    }
    
    // ==================== toKilometers() - Missing Branches ====================
    
    @Test
    void testToKilometers_NullDistanceValue() {
        // Special condition with null distance
        Visibility visibility = Visibility.cavok();
        
        assertThat(visibility.toKilometers()).isNull();
    }
    
    @Test
    void testToKilometers_KilometersUnit() {
        Visibility visibility = Visibility.kilometers(10.0);
        
        assertThat(visibility.toKilometers()).isEqualTo(10.0);
    }
    
    @Test
    void testToKilometers_MetersUnit() {
        Visibility visibility = Visibility.meters(5000.0);
        
        assertThat(visibility.toKilometers()).isEqualTo(5.0);
    }
    
    @Test
    void testToKilometers_StatuteMilesUnit() {
        Visibility visibility = Visibility.statuteMiles(1.0);
        
        assertThat(visibility.toKilometers()).isCloseTo(1.60934, org.assertj.core.data.Offset.offset(0.0001));
    }
    
    // ==================== Parameterized Tests for Complete Coverage ====================
    
    @ParameterizedTest
    @CsvSource({
        "0.0, SM, true",
        "0.5, SM, true",
        "0.99, SM, true",
        "1.0, SM, false",
        "1.01, SM, false",
        "2.0, SM, false"
    })
    void testIsLowVisibility_BoundaryConditions(double distance, String unit, boolean expected) {
        Visibility visibility = new Visibility(distance, unit, false, false, null);
        
        assertThat(visibility.isLowVisibility()).isEqualTo(expected);
    }
    
    @ParameterizedTest
    @CsvSource({
        "2.99, SM, true",
        "3.0, SM, false",
        "3.01, SM, false",
        "10.0, SM, false"
    })
    void testIsIFR_BoundaryConditions(double distance, String unit, boolean expected) {
        Visibility visibility = new Visibility(distance, unit, false, false, null);
        
        assertThat(visibility.isIFR()).isEqualTo(expected);
    }
    
    @ParameterizedTest
    @CsvSource({
        "2.99, SM, false",
        "3.0, SM, true",
        "3.01, SM, true",
        "10.0, SM, true"
    })
    void testIsVFR_BoundaryConditions(double distance, String unit, boolean expected) {
        Visibility visibility = new Visibility(distance, unit, false, false, null);
        
        assertThat(visibility.isVFR()).isEqualTo(expected);
    }
    
    @ParameterizedTest
    @CsvSource({
        "1.0, SM, 1609.34",
        "5.0, SM, 8046.7",
        "10.0, SM, 16093.4"
    })
    void testToMeters_Conversions(double distance, String unit, double expectedMeters) {
        Visibility visibility = new Visibility(distance, unit, false, false, null);
        
        assertThat(visibility.toMeters()).isCloseTo(expectedMeters, org.assertj.core.data.Offset.offset(0.1));
    }
    
    @ParameterizedTest
    @CsvSource({
        "1609.34, M, 1.0",
        "8046.7, M, 5.0",
        "16093.4, M, 10.0"
    })
    void testToStatuteMiles_Conversions(double distance, String unit, double expectedSM) {
        Visibility visibility = new Visibility(distance, unit, false, false, null);
        
        assertThat(visibility.toStatuteMiles()).isCloseTo(expectedSM, org.assertj.core.data.Offset.offset(0.01));
    }
    
    @ParameterizedTest
    @CsvSource({
        "1000, M, 1.0",
        "5000, M, 5.0",
        "10000, M, 10.0"
    })
    void testToKilometers_Conversions(double distance, String unit, double expectedKM) {
        Visibility visibility = new Visibility(distance, unit, false, false, null);
        
        assertThat(visibility.toKilometers()).isEqualTo(expectedKM);
    }
    
        @Test
    void testGetSummary_BothModifiersFalse() {
        // Ensure we test the path where both lessThan and greaterThan are false
        // This should already be covered by existing tests, but let's be explicit
        Visibility visibility = Visibility.statuteMiles(5.0);
        
        String summary = visibility.getSummary();
        
        // Should not have "Less than" or "Greater than" prefix
        assertThat(summary).doesNotContain("Less than")
                .doesNotContain("Greater than")
                .contains("5.00")
                .contains("statute miles");
    }
    
    @Test
    void testGetSummary_MetersUnit_NoModifiers() {
        // Explicitly test M unit path in switch with no modifiers
        Visibility visibility = new Visibility(3000.0, "M", false, false, null);
        
        String summary = visibility.getSummary();
        
        assertThat(summary).contains("3000.00")
                .contains("meters")
        // 3000 meters ≈ 1.86 SM, which is < 3 SM, so IFR
                .contains("(IFR)");
    }
    
    @Test
    void testGetSummary_KilometersUnit_NoModifiers() {
        // Explicitly test KM unit path in switch with no modifiers
        Visibility visibility = new Visibility(8.0, "KM", false, false, null);
        
        String summary = visibility.getSummary();
        
        assertThat(summary).contains("8.00")
                .contains("kilometers")
        // 8 km ≈ 4.97 SM, which is >= 3 SM, so VFR
                .contains("(VFR)");
    }
}
