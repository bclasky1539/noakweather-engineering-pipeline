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
package weather.processing.parser.noaa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for RegExprConst pattern compilation and basic matching behavior.
 * Validates that all regex patterns compile successfully and match expected inputs.
 * 
 * Note: All patterns require trailing whitespace (\s+), so test inputs include spaces.
 * 
 * @author bclasky1539
 *
 */
class RegExprConstTest {
    
    // ========== CONSTRUCTOR TEST ==========
    
    @Test
    void testConstructorThrowsException() {
        // Private constructor should throw UnsupportedOperationException
        assertThatThrownBy(() -> {
            var constructor = RegExprConst.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        })
        .hasCauseInstanceOf(UnsupportedOperationException.class)
        .hasStackTraceContaining("Utility class - do not instantiate");
    }
    
    // ========== PATTERN COMPILATION TEST ==========
    
    @Test
    void testAllPatternsCompile() {
        // If any pattern failed to compile, class loading would fail
        assertThat(RegExprConst.STATION_DAY_TIME_VALTMPER_PATTERN).isNotNull();
        assertThat(RegExprConst.WIND_PATTERN).isNotNull();
        assertThat(RegExprConst.VISIBILITY_PATTERN).isNotNull();
        assertThat(RegExprConst.TEMP_DEWPOINT_PATTERN).isNotNull();
        assertThat(RegExprConst.ALTIMETER_PATTERN).isNotNull();
        assertThat(RegExprConst.LIGHTNING_PATTERN).isNotNull();
    }
    
    // ========== MAIN BODY PATTERNS ==========
    
    @Test
    void testStationDayTimePattern() {
        String input = "KJFK 121851Z ";
        Matcher matcher = RegExprConst.STATION_DAY_TIME_VALTMPER_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("station")).isEqualTo("KJFK");
        assertThat(matcher.group("zday")).isEqualTo("12");
        assertThat(matcher.group("zhour")).isEqualTo("18");
        assertThat(matcher.group("zmin")).isEqualTo("51");
    }
    
    @Test
    void testWindPattern() {
        String input = "28016KT ";
        Matcher matcher = RegExprConst.WIND_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("dir")).isEqualTo("280");
        assertThat(matcher.group("speed")).isEqualTo("16");
        assertThat(matcher.group("units")).isEqualTo("KT");
    }
    
    @Test
    void testWindPattern_WithGusts() {
        String input = "18016G28KT ";
        Matcher matcher = RegExprConst.WIND_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("dir")).isEqualTo("180");
        assertThat(matcher.group("speed")).isEqualTo("16");
        assertThat(matcher.group("gust")).isEqualTo("28");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"10SM ", "1/2SM ", "9999 ", "CAVOK "})
    void testVisibilityPattern(String input) {
        Matcher matcher = RegExprConst.VISIBILITY_PATTERN.matcher(input);
        assertThat(matcher.find()).isTrue();
    }
    
    @Test
    void testTempDewpointPattern() {
        String input = "22/12 ";
        Matcher matcher = RegExprConst.TEMP_DEWPOINT_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("temp")).isEqualTo("22");
        assertThat(matcher.group("dewpt")).isEqualTo("12");
    }
    
    @Test
    void testTempDewpointPattern_Negative() {
        String input = "M05/M12 ";
        Matcher matcher = RegExprConst.TEMP_DEWPOINT_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("signt")).isEqualTo("M");
        assertThat(matcher.group("temp")).isEqualTo("05");
        assertThat(matcher.group("signd")).isEqualTo("M");
        assertThat(matcher.group("dewpt")).isEqualTo("12");
    }
    
    @Test
    void testAltimeterPattern_InchesHg() {
        String input = "A3015 ";
        Matcher matcher = RegExprConst.ALTIMETER_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("unit")).isEqualTo("A");
        assertThat(matcher.group("press")).isEqualTo("3015");
    }
    
    @Test
    void testAltimeterPattern_Hectopascals() {
        String input = "Q1013 ";
        Matcher matcher = RegExprConst.ALTIMETER_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("unit")).isEqualTo("Q");
        assertThat(matcher.group("press")).isEqualTo("1013");
    }
    
    @Test
    void testSkyConditionPattern() {
        String input = "FEW250 ";
        Matcher matcher = RegExprConst.SKY_CONDITION_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("cover")).isEqualTo("FEW");
        assertThat(matcher.group("height")).isEqualTo("250");
    }
    
    @Test
    void testSkyConditionPattern_WithCloudType() {
        String input = "BKN050CB ";
        Matcher matcher = RegExprConst.SKY_CONDITION_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("cover")).isEqualTo("BKN");
        assertThat(matcher.group("height")).isEqualTo("050");
        assertThat(matcher.group("cloud")).isEqualTo("CB");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"-RA ", "+TSRA ", "VCFG ", "BR ", "SHRA "})
    void testPresentWeatherPattern(String input) {
        Matcher matcher = RegExprConst.PRESENT_WEATHER_PATTERN.matcher(input);
        assertThat(matcher.find()).isTrue();
    }
    
    @Test
    void testRunwayPattern() {
        // Runway pattern is complex - let's use a simpler format
        String input = "R06/1200FT ";
        Matcher matcher = RegExprConst.RUNWAY_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("name")).isEqualTo("06");
        assertThat(matcher.group("lvalue")).isEqualTo("1200");
    }

    @Test
    void testRunwayPattern_BasicRvr() {
        String input = "R06/1200FT ";
        Matcher matcher = RegExprConst.RUNWAY_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("name")).isEqualTo("06");
        assertThat(matcher.group("lvalue")).isEqualTo("1200");
        assertThat(matcher.group("unit")).isEqualTo("FT");
    }

    @Test
    void testRunwayPattern_RvrWithTrend() {
        String input = "R22R/0400N ";
        Matcher matcher = RegExprConst.RUNWAY_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("name")).isEqualTo("22R"); // Includes suffix
        assertThat(matcher.group("inden")).isEqualTo("R");
        assertThat(matcher.group("lvalue")).isEqualTo("0400");
        assertThat(matcher.group("unit")).isEqualTo("N");
    }

    @Test
    void testRunwayPattern_RvrVariable() {
        String input = "R23L/0900V6000FT ";
        Matcher matcher = RegExprConst.RUNWAY_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("name")).isEqualTo("23L"); // Includes suffix
        assertThat(matcher.group("inden")).isEqualTo("L");
        assertThat(matcher.group("lvalue")).isEqualTo("0900");
        assertThat(matcher.group("high")).isEqualTo("6000");
        assertThat(matcher.group("unit")).isEqualTo("FT");
    }

    @Test
    void testRunwayPattern_RvrWithPrefix() {
        String input = "R24/P2000N ";
        Matcher matcher = RegExprConst.RUNWAY_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("name")).isEqualTo("24");
        assertThat(matcher.group("low")).startsWith("P");
        assertThat(matcher.group("lvalue")).isEqualTo("2000");
        assertThat(matcher.group("unit")).isEqualTo("N");
    }

    @Test
    void testRunwayPattern_ClrdStandalone() {
        // CLRD without any suffix (the case we just fixed!)
        String input = "R22R/CLRD ";
        Matcher matcher = RegExprConst.RUNWAY_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("name")).isEqualTo("22R"); // Includes suffix
        assertThat(matcher.group("inden")).isEqualTo("R");
        assertThat(matcher.group("lvalue")).isEqualTo("CLRD");
        assertThat(matcher.group("unit")).isNull(); // No unit for standalone CLRD
    }

    @Test
    void testRunwayPattern_ClrdWithSuffix() {
        // CLRD with numeric suffix
        String input = "R34L/CLRD70 ";
        Matcher matcher = RegExprConst.RUNWAY_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("name")).isEqualTo("34L"); // Includes suffix
        assertThat(matcher.group("inden")).isEqualTo("L");
        assertThat(matcher.group("lvalue")).isEqualTo("CLRD");
        assertThat(matcher.group("unit")).isEqualTo("70");
    }

    // ========== REMARKS PATTERNS ==========
    
    @Test
    void testAutoPattern() {
        String input = "AO2 ";
        Matcher matcher = RegExprConst.AUTO_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("2");
    }
    
    @Test
    void testSeaLevelPressurePattern() {
        String input = "SLP210 ";
        Matcher matcher = RegExprConst.SEALVL_PRESS_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("SLP");
        assertThat(matcher.group("press")).isEqualTo("210");
    }
    
    @Test
    void testPeakWindPattern() {
        String input = "PK WND 28032/1530 ";
        Matcher matcher = RegExprConst.PEAK_WIND_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("dir")).isEqualTo("280");
        assertThat(matcher.group("speed")).isEqualTo("32");
        assertThat(matcher.group("hour")).isEqualTo("15");
        assertThat(matcher.group("min")).isEqualTo("30");
    }
    
    @Test
    void testWindShiftPattern() {
        String input = "WSHFT 1530 ";
        Matcher matcher = RegExprConst.WIND_SHIFT_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("hour")).isEqualTo("15");
        assertThat(matcher.group("min")).isEqualTo("30");
    }
    
    @Test
    void testTemp1HourPattern() {
        // Temperature pattern: T + sign + 3 digits + (sign + 3 digits for dewpoint)
        // Format: T[0|1]TTT[0|1]DDD where T=temp, D=dewpoint
        String input = "T00031139 ";  // Temp 3.0°C, Dewpoint -13.9°C
        Matcher matcher = RegExprConst.TEMP_1HR_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("T");
        assertThat(matcher.group("tsign")).isEqualTo("0");
        assertThat(matcher.group("temp")).isEqualTo("003");
        assertThat(matcher.group("dsign")).isEqualTo("1");
        assertThat(matcher.group("dewpt")).isEqualTo("139");
    }
    
    @Test
    void testPrecip1HourPattern() {
        String input = "P0015 ";
        Matcher matcher = RegExprConst.PRECIP_1HR_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("P");
        assertThat(matcher.group("precip")).isEqualTo("0015");
    }
    
    @Test
    void testTemp6HourMaxMinPattern() {
        String input = "10142 ";
        Matcher matcher = RegExprConst.TEMP_6HR_MAX_MIN_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("1");
        assertThat(matcher.group("sign")).isEqualTo("0");
        assertThat(matcher.group("temp")).isEqualTo("142");
    }
    
    @Test
    void testPressure3HourPattern() {
        String input = "52032 ";
        Matcher matcher = RegExprConst.PRESS_3HR_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("5");
        assertThat(matcher.group("tend")).isEqualTo("2");
        assertThat(matcher.group("press")).isEqualTo("032");
    }
    
    // ========== REPORT MODIFIER TEST ==========
    
    @ParameterizedTest
    @ValueSource(strings = {"AUTO ", "COR ", "AMD "})
    void testReportModifierPattern(String input) {
        Matcher matcher = RegExprConst.REPORT_MODIFIER_PATTERN.matcher(input);
        assertThat(matcher.find()).isTrue();
    }
    
    // ========== NO SIGNIFICANT CHANGE TEST ==========
    
    @Test
    void testNoSigChangePattern() {
        String input = "NOSIG ";
        Matcher matcher = RegExprConst.NO_SIG_CHANGE_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("nosigchng")).isEqualTo("NOSIG");
    }
    
    // ========== UNPARSED PATTERN TEST ==========
    
    @Test
    void testUnparsedPattern() {
        String input = "UNKNOWN_TOKEN ";
        Matcher matcher = RegExprConst.UNPARSED_PATTERN.matcher(input);
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("unparsed")).isEqualTo("UNKNOWN_TOKEN");
    }
}
