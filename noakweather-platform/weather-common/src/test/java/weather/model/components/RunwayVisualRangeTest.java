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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import org.junit.jupiter.api.DisplayName;

/**
 * Tests for RunwayVisualRange record.
 * 
 * @author bclasky1539
 * 
 */
class RunwayVisualRangeTest {
    
    // ==================== Valid Construction Tests ====================
    
    @Test
    void testSimpleRVR() {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 2200, null, null, null, null);
        
        assertThat(rvr.runway()).isEqualTo("04L");
        assertThat(rvr.visualRangeFeet()).isEqualTo(2200);
        assertThat(rvr.variableLow()).isNull();
        assertThat(rvr.variableHigh()).isNull();
        assertThat(rvr.prefix()).isNull();
        assertThat(rvr.trend()).isNull();
    }
    
    @Test
    void testVariableRVR() {
        RunwayVisualRange rvr = new RunwayVisualRange("22R", null, 1800, 2400, null, null);
        
        assertThat(rvr.runway()).isEqualTo("22R");
        assertThat(rvr.visualRangeFeet()).isNull();
        assertThat(rvr.variableLow()).isEqualTo(1800);
        assertThat(rvr.variableHigh()).isEqualTo(2400);
        assertThat(rvr.isVariable()).isTrue();
    }
    
    @Test
    void testRVRWithPrefix() {
        RunwayVisualRange rvr = new RunwayVisualRange("18", 6000, null, null, "P", null);
        
        assertThat(rvr.prefix()).isEqualTo("P");
        assertThat(rvr.isGreaterThan()).isTrue();
        assertThat(rvr.isLessThan()).isFalse();
    }
    
    @Test
    void testRVRWithTrend() {
        RunwayVisualRange rvr = new RunwayVisualRange("09C", 2200, null, null, null, "D");
        
        assertThat(rvr.trend()).isEqualTo("D");
        assertThat(rvr.getTrendDescription()).isEqualTo("Decreasing");
    }
    
    @Test
    void testCompleteRVR() {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", null, 1200, 1800, "M", "U");
        
        assertThat(rvr.runway()).isEqualTo("04L");
        assertThat(rvr.isVariable()).isTrue();
        assertThat(rvr.isLessThan()).isTrue();
        assertThat(rvr.getTrendDescription()).isEqualTo("Increasing");
    }
    
    // ==================== Runway Validation Tests ====================
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void testInvalidRunway_NullOrBlank(String runway) {
        assertThatThrownBy(() -> new RunwayVisualRange(runway, 2200, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Runway identifier cannot be null or blank");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"00", "37", "99", "4L", "041", "04X", "AA", "L04"})
    void testInvalidRunway_BadFormat(String runway) {
        assertThatThrownBy(() -> new RunwayVisualRange(runway, 2200, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid runway identifier format");
    }
    
    @ParameterizedTest
    @CsvSource({
        "01", "09", "18", "27", "36",
        "01L", "09C", "18R", "27L", "36C",
        "04l", "22r", "18c"  // Test case insensitivity in validation
    })
    void testValidRunway_Formats(String runway) {
        RunwayVisualRange rvr = new RunwayVisualRange(runway, 2200, null, null, null, null);
        assertThat(rvr.runway()).isNotNull();
    }
    
    // ==================== Visual Range Validation Tests ====================
    
    @Test
    void testInvalidVisualRange_NegativeValue() {
        assertThatThrownBy(() -> new RunwayVisualRange("04L", -100, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Visual range cannot be negative");
    }
    
    @Test
    void testInvalidVisualRange_NoValues() {
        assertThatThrownBy(() -> new RunwayVisualRange("04L", null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Must have either visualRangeFeet or both variableLow and variableHigh");
    }
    
    @Test
    void testInvalidVisualRange_OnlyVariableLow() {
        assertThatThrownBy(() -> new RunwayVisualRange("04L", null, 1800, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Must have either visualRangeFeet or both variableLow and variableHigh");
    }
    
    @Test
    void testInvalidVisualRange_OnlyVariableLow_WithVisualRange() {
        // This tests the branch in isVariable() where variableLow != null but variableHigh == null
        // Even though it throws an exception, isVariable() gets called during validation
        assertThatThrownBy(() -> new RunwayVisualRange("04L", 2000, 1800, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Both variableLow and variableHigh must be provided together");
    }
    
    @Test
    void testInvalidVisualRange_OnlyVariableHigh() {
        assertThatThrownBy(() -> new RunwayVisualRange("04L", null, null, 2400, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Must have either visualRangeFeet or both variableLow and variableHigh");
    }
    
    @Test
    void testInvalidVisualRange_OnlyVariableHigh_WithVisualRange() {
        // This tests the branch in isVariable() where variableLow == null but variableHigh != null
        // Even though it throws an exception, this helps with branch coverage
        assertThatThrownBy(() -> new RunwayVisualRange("04L", 2000, null, 2400, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Both variableLow and variableHigh must be provided together");
    }
    
    @Test
    void testInvalidVisualRange_NegativeVariableLow() {
        assertThatThrownBy(() -> new RunwayVisualRange("04L", null, -100, 2400, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Variable low cannot be negative");
    }
    
    @Test
    void testInvalidVisualRange_NegativeVariableHigh() {
        assertThatThrownBy(() -> new RunwayVisualRange("04L", null, 1800, -100, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Variable high cannot be negative");
    }
    
    @Test
    void testInvalidVisualRange_LowGreaterThanHigh() {
        assertThatThrownBy(() -> new RunwayVisualRange("04L", null, 2400, 1800, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Variable low (2400) must be less than variable high (1800)");
    }
    
    @Test
    void testInvalidVisualRange_LowEqualToHigh() {
        assertThatThrownBy(() -> new RunwayVisualRange("04L", null, 2000, 2000, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Variable low (2000) must be less than variable high (2000)");
    }
    
    // ==================== Prefix Validation Tests ====================
    
    @ParameterizedTest
    @ValueSource(strings = {"P", "p", "M", "m"})
    void testValidPrefix(String prefix) {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 6000, null, null, prefix, null);
        assertThat(rvr.prefix()).isNotNull();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"X", "A", "PP", "MM", "1"})
    void testInvalidPrefix(String prefix) {
        assertThatThrownBy(() -> new RunwayVisualRange("04L", 6000, null, null, prefix, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid prefix");
    }
    
    // ==================== Trend Validation Tests ====================
    
    @ParameterizedTest
    @ValueSource(strings = {"D", "d", "N", "n", "U", "u"})
    void testValidTrend(String trend) {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 2200, null, null, null, trend);
        assertThat(rvr.trend()).isNotNull();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"X", "A", "DD", "NN", "1"})
    void testInvalidTrend(String trend) {
        assertThatThrownBy(() -> new RunwayVisualRange("04L", 2200, null, null, null, trend))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid trend");
    }
    
    // ==================== Behavior Tests ====================
    
    @Test
    void testIsVariable_True() {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", null, 1800, 2400, null, null);
        assertThat(rvr.isVariable()).isTrue();
    }
    
    @Test
    void testIsVariable_False() {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 2200, null, null, null, null);
        assertThat(rvr.isVariable()).isFalse();
    }
    
    @Test
    void testIsGreaterThan_True() {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 6000, null, null, "P", null);
        assertThat(rvr.isGreaterThan()).isTrue();
        assertThat(rvr.isLessThan()).isFalse();
    }
    
    @Test
    void testIsLessThan_True() {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 600, null, null, "M", null);
        assertThat(rvr.isLessThan()).isTrue();
        assertThat(rvr.isGreaterThan()).isFalse();
    }
    
    @Test
    void testPrefix_CaseInsensitive() {
        RunwayVisualRange rvr1 = new RunwayVisualRange("04L", 6000, null, null, "P", null);
        RunwayVisualRange rvr2 = new RunwayVisualRange("04L", 6000, null, null, "p", null);
        
        assertThat(rvr1.isGreaterThan()).isTrue();
        assertThat(rvr2.isGreaterThan()).isTrue();
    }
    
    // ==================== Trend Description Tests ====================
    
    @ParameterizedTest
    @CsvSource({
        "D, Decreasing",
        "d, Decreasing",
        "N, No Change",
        "n, No Change",
        "U, Increasing",
        "u, Increasing",
        " D , Decreasing",
        "  N  , No Change",
        "\tU\t, Increasing"
    })
    @DisplayName("getTrendDescription handles valid trends and trims whitespace")
    void testGetTrendDescription_ValidTrendsAndWhitespace(String trend, String expected) {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 2200, null, null, null, trend);
        assertThat(rvr.getTrendDescription()).isEqualTo(expected);
    }
    
    @Test
    void testGetTrendDescription_NullTrend() {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 2200, null, null, null, null);
        assertThat(rvr.getTrendDescription()).isEqualTo("Unknown");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    void testGetTrendDescription_BlankTrend(String trend) {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 2200, null, null, null, trend);
        assertThat(rvr.getTrendDescription()).isEqualTo("Unknown");
    }
    
    // ==================== Conversion Tests ====================
    
    @Test
    void testGetVisualRangeStatuteMiles_ExactMile() {
        // 5280 feet = exactly 1 statute mile
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 5280, null, null, null, null);
        assertThat(rvr.getVisualRangeStatuteMiles()).isCloseTo(1.0, within(0.001));
    }
    
    @Test
    void testGetVisualRangeStatuteMiles_HalfMile() {
        // 2640 feet = 0.5 statute miles
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 2640, null, null, null, null);
        assertThat(rvr.getVisualRangeStatuteMiles()).isCloseTo(0.5, within(0.001));
    }
    
    @Test
    void testGetVisualRangeStatuteMiles_QuarterMile() {
        // 1320 feet = 0.25 statute miles
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 1320, null, null, null, null);
        assertThat(rvr.getVisualRangeStatuteMiles()).isCloseTo(0.25, within(0.001));
    }
    
    @ParameterizedTest
    @CsvSource({
        "600, 0.1136",      // Common minimum RVR
        "1200, 0.2273",     // Typical low visibility
        "2400, 0.4545",     // CAT II minimum
        "6000, 1.1364",     // Common maximum reporting
        "10560, 2.0"        // 2 statute miles
    })
    void testGetVisualRangeStatuteMiles_RealWorldValues(int feet, double expectedMiles) {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", feet, null, null, null, null);
        assertThat(rvr.getVisualRangeStatuteMiles()).isCloseTo(expectedMiles, within(0.01));
    }
    
    @Test
    void testGetVisualRangeStatuteMiles_Variable() {
        // Average of 2400 and 3600 = 3000 feet = ~0.568 miles
        RunwayVisualRange rvr = new RunwayVisualRange("04L", null, 2400, 3600, null, null);
        assertThat(rvr.getVisualRangeStatuteMiles()).isCloseTo(0.568, within(0.01));
    }
    
    @Test
    void testGetVisualRangeStatuteMiles_VariableRange_LowValues() {
        // Average of 600 and 1200 = 900 feet
        RunwayVisualRange rvr = new RunwayVisualRange("04L", null, 600, 1200, null, null);
        double expected = 900.0 / 5280.0;
        assertThat(rvr.getVisualRangeStatuteMiles()).isCloseTo(expected, within(0.001));
    }
    
    @Test
    void testGetVisualRangeStatuteMiles_VariableRange_HighValues() {
        // Average of 5000 and 6000 = 5500 feet
        RunwayVisualRange rvr = new RunwayVisualRange("04L", null, 5000, 6000, null, null);
        double expected = 5500.0 / 5280.0;
        assertThat(rvr.getVisualRangeStatuteMiles()).isCloseTo(expected, within(0.001));
    }
    
    @Test
    void testGetVisualRangeStatuteMiles_ZeroValue() {
        // Edge case: zero visibility (shouldn't happen in reality but validate the math)
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 0, null, null, null, null);
        assertThat(rvr.getVisualRangeStatuteMiles()).isCloseTo(0.0, within(0.001));
    }
    
    @Test
    void testGetVisualRangeMeters_1000Feet() {
        // 1000 feet = 304.8 meters (exact conversion)
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 1000, null, null, null, null);
        assertThat(rvr.getVisualRangeMeters()).isCloseTo(304.8, within(0.01));
    }
    
    @Test
    void testGetVisualRangeMeters_100Feet() {
        // 100 feet = 30.48 meters
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 100, null, null, null, null);
        assertThat(rvr.getVisualRangeMeters()).isCloseTo(30.48, within(0.01));
    }
    
    @ParameterizedTest
    @CsvSource({
        "600, 182.88",      // 600 feet minimum RVR
        "1200, 365.76",     // Common low RVR
        "1800, 548.64",     // CAT I minimum
        "2400, 731.52",     // CAT II
        "3000, 914.4",      // Common mid-range
        "6000, 1828.8"      // Common maximum
    })
    void testGetVisualRangeMeters_RealWorldValues(int feet, double expectedMeters) {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", feet, null, null, null, null);
        assertThat(rvr.getVisualRangeMeters()).isCloseTo(expectedMeters, within(0.1));
    }
    
    @Test
    void testGetVisualRangeMeters_Variable() {
        // Average of 1500 and 2500 = 2000 feet = 609.6 meters
        RunwayVisualRange rvr = new RunwayVisualRange("04L", null, 1500, 2500, null, null);
        assertThat(rvr.getVisualRangeMeters()).isCloseTo(609.6, within(0.1));
    }
    
    @Test
    void testGetVisualRangeMeters_VariableRange_ExtremeValues() {
        // Average of 600 and 6000 = 3300 feet = 1005.84 meters
        RunwayVisualRange rvr = new RunwayVisualRange("04L", null, 600, 6000, null, null);
        double avgFeet = (600 + 6000) / 2.0;
        double expectedMeters = avgFeet * 0.3048;
        assertThat(rvr.getVisualRangeMeters()).isCloseTo(expectedMeters, within(0.1));
    }
    
    @Test
    void testGetVisualRangeMeters_ConversionFactorAccuracy() {
        // Verify conversion factor: 1 foot = 0.3048 meters exactly
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 5000, null, null, null, null);
        double expectedMeters = 5000 * 0.3048;
        assertThat(rvr.getVisualRangeMeters()).isCloseTo(expectedMeters, within(0.01));
        assertThat(expectedMeters).isCloseTo(1524.0, within(0.01));
    }
    
    @Test
    void testGetVisualRangeMeters_ZeroValue() {
        // Edge case: zero visibility
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 0, null, null, null, null);
        assertThat(rvr.getVisualRangeMeters()).isCloseTo(0.0, within(0.001));
    }
    
    @Test
    void testConversionConsistency_FeetToMilesAndMeters() {
        // Verify 1 statute mile = 5280 feet = 1609.344 meters
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 5280, null, null, null, null);
        
        assertThat(rvr.getVisualRangeStatuteMiles()).isCloseTo(1.0, within(0.001));
        assertThat(rvr.getVisualRangeMeters()).isCloseTo(1609.344, within(0.1));
    }
    
    @Test
    void testConversionConsistency_VariableRange() {
        // Verify conversions are consistent for variable ranges
        RunwayVisualRange rvr = new RunwayVisualRange("04L", null, 2000, 4000, null, null);
        
        // Average = 3000 feet
        double expectedMiles = 3000.0 / 5280.0;
        double expectedMeters = 3000.0 * 0.3048;
        
        assertThat(rvr.getVisualRangeStatuteMiles()).isCloseTo(expectedMiles, within(0.001));
        assertThat(rvr.getVisualRangeMeters()).isCloseTo(expectedMeters, within(0.1));
    }
    
    @ParameterizedTest
    @CsvSource({
        "1200, 0.2273, 365.76",   // CAT I approach minimum
        "1800, 0.3409, 548.64",   // CAT II touchdown zone
        "2400, 0.4545, 731.52",   // CAT III
        "6000, 1.1364, 1828.8"    // Maximum typical RVR
    })
    void testConversionConsistency_MultipleValues(int feet, double expectedMiles, double expectedMeters) {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", feet, null, null, null, null);
        
        assertThat(rvr.getVisualRangeStatuteMiles()).isCloseTo(expectedMiles, within(0.01));
        assertThat(rvr.getVisualRangeMeters()).isCloseTo(expectedMeters, within(0.1));
    }
    
    @Test
    void testConversion_VariableRangeTakesPrecedence() {
        // If both visualRangeFeet and variable range are provided, variable takes precedence
        // (This shouldn't happen in real METAR but code should handle it gracefully)
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 3000, 2000, 4000, null, null);
        
        // Should use variable range average (3000) not the visualRangeFeet
        double expectedMiles = 3000.0 / 5280.0;
        double expectedMeters = 3000.0 * 0.3048;
        
        assertThat(rvr.getVisualRangeStatuteMiles()).isCloseTo(expectedMiles, within(0.001));
        assertThat(rvr.getVisualRangeMeters()).isCloseTo(expectedMeters, within(0.1));
    }
    
    // NOTE: The following branches are intentionally unreachable due to validation:
    // 1. getVisualRangeStatuteMiles/Meters returning 0.0 when visualRangeFeet is null and not variable
    //    - Validation ensures: if visualRangeFeet is null, then BOTH variableLow and variableHigh must be non-null
    // 2. isVariable() returning false when exactly one of variableLow/variableHigh is null
    //    - Validation ensures: if either is non-null, both must be non-null
    // This defensive validation makes the code more robust and these unreachable branches are acceptable.
    
    
    // ==================== Summary Tests ====================
    
    @Test
    void testGetSummary_Simple() {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 2200, null, null, null, null);
        assertThat(rvr.getSummary()).isEqualTo("Runway 04L: 2200 feet");
    }
    
    @Test
    void testGetSummary_Variable() {
        RunwayVisualRange rvr = new RunwayVisualRange("22R", null, 1800, 2400, null, null);
        assertThat(rvr.getSummary()).isEqualTo("Runway 22R: 1800 to 2400 feet");
    }
    
    @Test
    void testGetSummary_WithGreaterThanPrefix() {
        RunwayVisualRange rvr = new RunwayVisualRange("18", 6000, null, null, "P", null);
        assertThat(rvr.getSummary()).isEqualTo("Runway 18: Greater than 6000 feet");
    }
    
    @Test
    void testGetSummary_WithLessThanPrefix() {
        RunwayVisualRange rvr = new RunwayVisualRange("09C", 600, null, null, "M", null);
        assertThat(rvr.getSummary()).isEqualTo("Runway 09C: Less than 600 feet");
    }
    
    @Test
    void testGetSummary_WithTrend() {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 2200, null, null, null, "D");
        assertThat(rvr.getSummary()).isEqualTo("Runway 04L: 2200 feet, Decreasing");
    }
    
    @Test
    void testGetSummary_Complete() {
        RunwayVisualRange rvr = new RunwayVisualRange("04L", null, 1200, 1800, "M", "U");
        assertThat(rvr.getSummary()).isEqualTo("Runway 04L: Less than 1200 to 1800 feet, Increasing");
    }
    
    // ==================== Factory Method Tests ====================
    
    @Test
    void testFactoryMethod_Of() {
        RunwayVisualRange rvr = RunwayVisualRange.of("04L", 2200);
        
        assertThat(rvr.runway()).isEqualTo("04L");
        assertThat(rvr.visualRangeFeet()).isEqualTo(2200);
        assertThat(rvr.variableLow()).isNull();
        assertThat(rvr.variableHigh()).isNull();
        assertThat(rvr.prefix()).isNull();
        assertThat(rvr.trend()).isNull();
        assertThat(rvr.isVariable()).isFalse();
    }
    
    @Test
    void testFactoryMethod_Variable() {
        RunwayVisualRange rvr = RunwayVisualRange.variable("22R", 1800, 2400);
        
        assertThat(rvr.runway()).isEqualTo("22R");
        assertThat(rvr.visualRangeFeet()).isNull();
        assertThat(rvr.variableLow()).isEqualTo(1800);
        assertThat(rvr.variableHigh()).isEqualTo(2400);
        assertThat(rvr.prefix()).isNull();
        assertThat(rvr.trend()).isNull();
        assertThat(rvr.isVariable()).isTrue();
    }
    
    // ==================== Real World Examples ====================
    
    @Test
    void testRealWorldExample_R04L_2200FT() {
        // R04L/2200FT
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 2200, null, null, null, null);
        
        assertThat(rvr.runway()).isEqualTo("04L");
        assertThat(rvr.visualRangeFeet()).isEqualTo(2200);
        assertThat(rvr.isVariable()).isFalse();
        assertThat(rvr.getSummary()).contains("2200 feet");
    }
    
    @Test
    void testRealWorldExample_R04L_1800V2400FT() {
        // R04L/1800V2400FT
        RunwayVisualRange rvr = new RunwayVisualRange("04L", null, 1800, 2400, null, null);
        
        assertThat(rvr.runway()).isEqualTo("04L");
        assertThat(rvr.isVariable()).isTrue();
        assertThat(rvr.variableLow()).isEqualTo(1800);
        assertThat(rvr.variableHigh()).isEqualTo(2400);
    }
    
    @Test
    void testRealWorldExample_R22R_P6000FT() {
        // R22R/P6000FT
        RunwayVisualRange rvr = new RunwayVisualRange("22R", 6000, null, null, "P", null);
        
        assertThat(rvr.runway()).isEqualTo("22R");
        assertThat(rvr.visualRangeFeet()).isEqualTo(6000);
        assertThat(rvr.isGreaterThan()).isTrue();
        assertThat(rvr.getSummary()).contains("Greater than");
    }
    
    @Test
    void testRealWorldExample_R04L_M0600FT() {
        // R04L/M0600FT
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 600, null, null, "M", null);
        
        assertThat(rvr.runway()).isEqualTo("04L");
        assertThat(rvr.visualRangeFeet()).isEqualTo(600);
        assertThat(rvr.isLessThan()).isTrue();
        assertThat(rvr.getSummary()).contains("Less than");
    }
    
    @Test
    void testRealWorldExample_R04L_2200FT_D() {
        // R04L/2200FT/D
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 2200, null, null, null, "D");
        
        assertThat(rvr.runway()).isEqualTo("04L");
        assertThat(rvr.visualRangeFeet()).isEqualTo(2200);
        assertThat(rvr.getTrendDescription()).isEqualTo("Decreasing");
    }
}
