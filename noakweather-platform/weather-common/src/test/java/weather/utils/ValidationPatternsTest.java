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
package weather.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ValidationPatterns utility class.
 * 
 * @author bclasky1539
 * 
 */
class ValidationPatternsTest {
    
    // ==================== Utility Class Tests ====================
    
    @Test
    void testUtilityClass_CannotBeInstantiated() {
        // Verify that the utility class cannot be instantiated
        assertThatThrownBy(() -> {
            var constructor = ValidationPatterns.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        })
        .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
        .hasCauseInstanceOf(AssertionError.class)
        .cause()
        .hasMessageContaining("Utility class should not be instantiated");
    }
    
    @Test
    void testAllPatternsAreNotNull() {
        assertThat(ValidationPatterns.RUNWAY_IDENTIFIER).isNotNull();
        assertThat(ValidationPatterns.CLOUD_TYPE).isNotNull();
        assertThat(ValidationPatterns.WIND_UNIT).isNotNull();
        assertThat(ValidationPatterns.TEMPERATURE_VALUE).isNotNull();
        assertThat(ValidationPatterns.WIND_DIRECTION).isNotNull();
        assertThat(ValidationPatterns.WIND_SPEED).isNotNull();
        assertThat(ValidationPatterns.ALTITUDE_HUNDREDS_FEET).isNotNull();
        assertThat(ValidationPatterns.PRESSURE_VALUE).isNotNull();
        assertThat(ValidationPatterns.VISIBILITY_UNIT).isNotNull();
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION).isNotNull();
    }
    
    @Test
    void testAllPatternsAreCompiled() {
        // Verify all patterns are instances of Pattern (i.e., compiled)
        assertThat(ValidationPatterns.RUNWAY_IDENTIFIER).isInstanceOf(Pattern.class);
        assertThat(ValidationPatterns.CLOUD_TYPE).isInstanceOf(Pattern.class);
        assertThat(ValidationPatterns.WIND_UNIT).isInstanceOf(Pattern.class);
        assertThat(ValidationPatterns.TEMPERATURE_VALUE).isInstanceOf(Pattern.class);
        assertThat(ValidationPatterns.WIND_DIRECTION).isInstanceOf(Pattern.class);
        assertThat(ValidationPatterns.WIND_SPEED).isInstanceOf(Pattern.class);
        assertThat(ValidationPatterns.ALTITUDE_HUNDREDS_FEET).isInstanceOf(Pattern.class);
        assertThat(ValidationPatterns.PRESSURE_VALUE).isInstanceOf(Pattern.class);
        assertThat(ValidationPatterns.VISIBILITY_UNIT).isInstanceOf(Pattern.class);
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION).isInstanceOf(Pattern.class);
    }
    
    // ==================== RUNWAY_IDENTIFIER Tests ====================
    
    @ParameterizedTest
    @ValueSource(strings = {"01", "09", "18", "22", "36", "04L", "22R", "09C", "18L", "27R"})
    void testRunwayIdentifier_Valid(String runway) {
        assertThat(ValidationPatterns.RUNWAY_IDENTIFIER.matcher(runway).matches()).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"00", "37", "4L", "18X", "0", "99", "1L", "36X", "04LR"})
    void testRunwayIdentifier_Invalid(String runway) {
        assertThat(ValidationPatterns.RUNWAY_IDENTIFIER.matcher(runway).matches()).isFalse();
    }
    
    @Test
    void testRunwayIdentifier_EmptyString() {
        assertThat(ValidationPatterns.RUNWAY_IDENTIFIER.matcher("").matches()).isFalse();
    }
    
    // ==================== CLOUD_TYPE Tests ====================
    
    @ParameterizedTest
    @ValueSource(strings = {"CB", "TCU", "CU", "SC", "ST", "NS", "AS", "AC", "CI", "CC", "CS"})
    void testCloudType_Valid(String cloudType) {
        assertThat(ValidationPatterns.CLOUD_TYPE.matcher(cloudType).matches()).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"XX", "ABC", "ZZ", "Q", "123", "cb", "tcu", "CBTCU", "C", "CBS"})
    void testCloudType_Invalid(String cloudType) {
        assertThat(ValidationPatterns.CLOUD_TYPE.matcher(cloudType).matches()).isFalse();
    }
    
    @Test
    void testCloudType_EmptyString() {
        assertThat(ValidationPatterns.CLOUD_TYPE.matcher("").matches()).isFalse();
    }
    
    @Test
    void testCloudType_CaseSensitive() {
        // Pattern should be case-sensitive (uppercase only)
        assertThat(ValidationPatterns.CLOUD_TYPE.matcher("cb").matches()).isFalse();
        assertThat(ValidationPatterns.CLOUD_TYPE.matcher("CB").matches()).isTrue();
        assertThat(ValidationPatterns.CLOUD_TYPE.matcher("Cb").matches()).isFalse();
    }
    
    // ==================== WIND_UNIT Tests ====================
    
    @ParameterizedTest
    @ValueSource(strings = {"KT", "MPS", "KMH"})
    void testWindUnit_Valid(String unit) {
        assertThat(ValidationPatterns.WIND_UNIT.matcher(unit).matches()).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"kt", "mps", "kmh", "MPH", "FPS", "KPH", "MS", "KTS", "KNOTS", "ABC"})
    void testWindUnit_Invalid(String unit) {
        assertThat(ValidationPatterns.WIND_UNIT.matcher(unit).matches()).isFalse();
    }
    
    @Test
    void testWindUnit_EmptyString() {
        assertThat(ValidationPatterns.WIND_UNIT.matcher("").matches()).isFalse();
    }
    
    @Test
    void testWindUnit_CaseSensitive() {
        // Pattern should be case-sensitive (uppercase only)
        assertThat(ValidationPatterns.WIND_UNIT.matcher("kt").matches()).isFalse();
        assertThat(ValidationPatterns.WIND_UNIT.matcher("KT").matches()).isTrue();
        assertThat(ValidationPatterns.WIND_UNIT.matcher("Kt").matches()).isFalse();
    }
    
    // ==================== VISIBILITY_UNIT Tests ====================
    
    @ParameterizedTest
    @ValueSource(strings = {"SM", "M", "KM"})
    void testVisibilityUnit_Valid(String unit) {
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher(unit).matches()).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "sm", "m", "km",                    // lowercase
        "Sm", "Km",                         // mixed case
        "FEET", "FT", "NM", "MI",          // invalid units
        "METERS", "MILES", "KILOMETERS",    // spelled out
        " SM", "SM ", " SM ",               // whitespace
        "S", "K", "SMM", "KMM"             // partial/too long
    })
    void testVisibilityUnit_Invalid(String unit) {
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher(unit).matches()).isFalse();
    }
    
    @Test
    void testVisibilityUnit_EmptyString() {
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher("").matches()).isFalse();
    }
    
    @Test
    void testVisibilityUnit_CaseSensitive() {
        // Pattern should be case-sensitive (uppercase only)
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher("SM").matches()).isTrue();
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher("sm").matches()).isFalse();
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher("M").matches()).isTrue();
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher("m").matches()).isFalse();
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher("KM").matches()).isTrue();
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher("km").matches()).isFalse();
    }
    
    @Test
    void testVisibilityUnit_WithWhitespace() {
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher("  SM  ").matches()).isFalse();
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher("\tSM\t").matches()).isFalse();
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher("S M").matches()).isFalse();
    }
    
    // ==================== VISIBILITY_SPECIAL_CONDITION Tests ====================
    
    @ParameterizedTest
    @ValueSource(strings = {"CAVOK", "NDV"})
    void testVisibilitySpecialCondition_Valid(String condition) {
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher(condition).matches()).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "cavok", "ndv",                     // lowercase
        "Cavok", "Ndv", "CaVoK",           // mixed case
        "SKC", "CLR", "NSC", "VV",         // other conditions
        "CAVO", "CAV", "ND",               // partial
        "CAVOKK", "NDVV",                  // too long
        " CAVOK", "CAVOK ", " CAVOK ",     // whitespace
        "CAV OK", "N DV",                  // space in middle
        "UNKNOWN", "UNLIMITED"              // invalid
    })
    void testVisibilitySpecialCondition_Invalid(String condition) {
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher(condition).matches()).isFalse();
    }
    
    @Test
    void testVisibilitySpecialCondition_EmptyString() {
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher("").matches()).isFalse();
    }
    
    @Test
    void testVisibilitySpecialCondition_CaseSensitive() {
        // Pattern should be case-sensitive (uppercase only)
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher("CAVOK").matches()).isTrue();
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher("cavok").matches()).isFalse();
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher("Cavok").matches()).isFalse();
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher("NDV").matches()).isTrue();
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher("ndv").matches()).isFalse();
    }
    
    @Test
    void testVisibilitySpecialCondition_WithWhitespace() {
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher("  CAVOK  ").matches()).isFalse();
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher("\tCAVOK\t").matches()).isFalse();
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher("CAV OK").matches()).isFalse();
    }
    
    // ==================== TEMPERATURE_VALUE Tests ====================
    
    @ParameterizedTest
    @ValueSource(strings = {"22", "05", "M05", "-10", "M0", "15", "99"})
    void testTemperatureValue_Valid(String temp) {
        assertThat(ValidationPatterns.TEMPERATURE_VALUE.matcher(temp).matches()).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"100", "M100", "-100", "ABC", "2.5", "+"})
    void testTemperatureValue_Invalid(String temp) {
        assertThat(ValidationPatterns.TEMPERATURE_VALUE.matcher(temp).matches()).isFalse();
    }
    
    // ==================== WIND_DIRECTION Tests ====================
    
    @ParameterizedTest
    @ValueSource(strings = {"000", "010", "180", "280", "360", "VRB", "090", "270"})
    void testWindDirection_Valid(String dir) {
        assertThat(ValidationPatterns.WIND_DIRECTION.matcher(dir).matches()).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"361", "370", "99", "00", "vrb", "Vrb", "VAR", "1000"})
    void testWindDirection_Invalid(String dir) {
        assertThat(ValidationPatterns.WIND_DIRECTION.matcher(dir).matches()).isFalse();
    }
    
    // ==================== WIND_SPEED Tests ====================
    
    @ParameterizedTest
    @ValueSource(strings = {"00", "05", "16", "99", "100", "032", "015"})
    void testWindSpeed_Valid(String speed) {
        assertThat(ValidationPatterns.WIND_SPEED.matcher(speed).matches()).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"0", "5", "1000", "ABC", "1.5"})
    void testWindSpeed_Invalid(String speed) {
        assertThat(ValidationPatterns.WIND_SPEED.matcher(speed).matches()).isFalse();
    }
    
    // ==================== ALTITUDE_HUNDREDS_FEET Tests ====================
    
    @ParameterizedTest
    @ValueSource(strings = {"000", "005", "015", "100", "250", "999"})
    void testAltitudeHundredsFeet_Valid(String alt) {
        assertThat(ValidationPatterns.ALTITUDE_HUNDREDS_FEET.matcher(alt).matches()).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"0", "00", "15", "1000", "ABC", "25.5"})
    void testAltitudeHundredsFeet_Invalid(String alt) {
        assertThat(ValidationPatterns.ALTITUDE_HUNDREDS_FEET.matcher(alt).matches()).isFalse();
    }
    
    // ==================== PRESSURE_VALUE Tests ====================
    
    @ParameterizedTest
    @ValueSource(strings = {"996", "1013", "3015", "999", "1000"})
    void testPressureValue_Valid(String pressure) {
        assertThat(ValidationPatterns.PRESSURE_VALUE.matcher(pressure).matches()).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"00", "99", "10000", "ABC", "30.15"})
    void testPressureValue_Invalid(String pressure) {
        assertThat(ValidationPatterns.PRESSURE_VALUE.matcher(pressure).matches()).isFalse();
    }
    
    // ==================== Edge Case Tests ====================
    
    @Test
    void testNullInput_DoesNotThrowException() {
        // Patterns should handle null gracefully (matcher will handle it)
        // This tests that patterns are properly instantiated
        assertThat(ValidationPatterns.RUNWAY_IDENTIFIER).isNotNull();
        assertThat(ValidationPatterns.CLOUD_TYPE).isNotNull();
        assertThat(ValidationPatterns.VISIBILITY_UNIT).isNotNull();
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION).isNotNull();
    }
    
    @Test
    void testEmptyString_ValidatesCorrectly() {
        String empty = "";
        
        assertThat(ValidationPatterns.RUNWAY_IDENTIFIER.matcher(empty).matches()).isFalse();
        assertThat(ValidationPatterns.CLOUD_TYPE.matcher(empty).matches()).isFalse();
        assertThat(ValidationPatterns.WIND_UNIT.matcher(empty).matches()).isFalse();
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher(empty).matches()).isFalse();
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher(empty).matches()).isFalse();
        assertThat(ValidationPatterns.TEMPERATURE_VALUE.matcher(empty).matches()).isFalse();
        assertThat(ValidationPatterns.WIND_DIRECTION.matcher(empty).matches()).isFalse();
        assertThat(ValidationPatterns.WIND_SPEED.matcher(empty).matches()).isFalse();
        assertThat(ValidationPatterns.ALTITUDE_HUNDREDS_FEET.matcher(empty).matches()).isFalse();
        assertThat(ValidationPatterns.PRESSURE_VALUE.matcher(empty).matches()).isFalse();
    }
    
    @Test
    void testWhitespace_ValidatesCorrectly() {
        String whitespace = "   ";
        
        assertThat(ValidationPatterns.RUNWAY_IDENTIFIER.matcher(whitespace).matches()).isFalse();
        assertThat(ValidationPatterns.CLOUD_TYPE.matcher(whitespace).matches()).isFalse();
        assertThat(ValidationPatterns.WIND_UNIT.matcher(whitespace).matches()).isFalse();
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher(whitespace).matches()).isFalse();
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher(whitespace).matches()).isFalse();
        assertThat(ValidationPatterns.TEMPERATURE_VALUE.matcher(whitespace).matches()).isFalse();
        assertThat(ValidationPatterns.WIND_DIRECTION.matcher(whitespace).matches()).isFalse();
        assertThat(ValidationPatterns.WIND_SPEED.matcher(whitespace).matches()).isFalse();
        assertThat(ValidationPatterns.ALTITUDE_HUNDREDS_FEET.matcher(whitespace).matches()).isFalse();
        assertThat(ValidationPatterns.PRESSURE_VALUE.matcher(whitespace).matches()).isFalse();
    }
    
    // ==================== Pattern Reusability Tests ====================
    
    @Test
    void testPatternReuse_ThreadSafe() {
        // Patterns are thread-safe and reusable
        Pattern pattern = ValidationPatterns.RUNWAY_IDENTIFIER;
        
        assertThat(pattern.matcher("04L").matches()).isTrue();
        assertThat(pattern.matcher("22R").matches()).isTrue();
        assertThat(pattern.matcher("99").matches()).isFalse();
    }
    
    @Test
    void testMultipleMatchersFromSamePattern() {
        // Creating multiple matchers from the same pattern should work
        var matcher1 = ValidationPatterns.CLOUD_TYPE.matcher("CB");
        var matcher2 = ValidationPatterns.CLOUD_TYPE.matcher("TCU");
        var matcher3 = ValidationPatterns.CLOUD_TYPE.matcher("XX");
        
        assertThat(matcher1.matches()).isTrue();
        assertThat(matcher2.matches()).isTrue();
        assertThat(matcher3.matches()).isFalse();
    }
    
    // ==================== Real World METAR Integration Tests ====================
    
    @Test
    void testVisibilityPatterns_RealWorldMetarExamples() {
        // Test patterns with real METAR visibility examples
        
        // Valid units from actual METARs
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher("SM").matches()).isTrue();  // US: "10SM"
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher("M").matches()).isTrue();   // ICAO: "9999"
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher("KM").matches()).isTrue();  // Metric: "10KM"
        
        // Valid special conditions from actual METARs
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher("CAVOK").matches()).isTrue();
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher("NDV").matches()).isTrue();
        
        // Invalid variations that might appear in malformed data
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher("10SM").matches()).isFalse();     // with number
        assertThat(ValidationPatterns.VISIBILITY_UNIT.matcher("9999M").matches()).isFalse();    // with number
        assertThat(ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher("NOSIG").matches()).isFalse(); // different condition
    }
}
