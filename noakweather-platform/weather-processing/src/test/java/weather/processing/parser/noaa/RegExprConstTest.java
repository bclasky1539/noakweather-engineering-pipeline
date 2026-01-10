/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025-2026 bclasky1539
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    // ========== HAIL SIZE PATTERN TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'GR 1/4 ', 1/4, 'Quarter inch'",
            "'GR 1/2 ', 1/2, 'Half inch'",
            "'GR 3/4 ', 3/4, 'Three-quarter inch'",
            "'GR 1 ', 1, 'One inch (severe threshold)'",
            "'GR 1 1/4 ', '1 1/4', 'One and quarter'",
            "'GR 1 1/2 ', '1 1/2', 'One and half'",
            "'GR 1 3/4 ', '1 3/4', 'One and three-quarter'",
            "'GR 2 ', 2, 'Two inches (significantly severe)'",
            "'GR 2 1/2 ', '2 1/2', 'Two and half'",
            "'GR 3 ', 3, 'Three inches'",
            "'GR 4 ', 4, 'Four inches'",
            "'GR 2 3/4 ', '2 3/4', 'Baseball sized'",
            "'GR 1 3/4', '1 3/4', 'Without trailing space'"
    })
    @DisplayName("HAIL_SIZE_PATTERN should match various hail sizes")
    void testHailSizePattern_VariousSizes(String input, String expectedSize, String scenario) {
        Matcher matcher = RegExprConst.HAIL_SIZE_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();

        assertThat(matcher.group("size"))
                .as("Size should match: %s", scenario)
                .isEqualTo(expectedSize);
    }

    @Test
    void testHailSizePattern_DoesNotMatchWithoutGR() {
        // Should NOT match without GR prefix
        String input = "1 3/4 ";
        Matcher matcher = RegExprConst.HAIL_SIZE_PATTERN.matcher(input);

        assertThat(matcher.find()).isFalse();
    }

    @Test
    void testHailSizePattern_DoesNotMatchInvalidFormat() {
        // Should NOT match invalid formats
        String input = "GR A ";
        Matcher matcher = RegExprConst.HAIL_SIZE_PATTERN.matcher(input);

        assertThat(matcher.find()).isFalse();
    }

    // ========== WEATHER BEGIN/END PATTERN TESTS ==========

    @Test
    void testBeginEndWeatherPattern_SimpleBegin() {
        // RAB05 - Rain began at :05 (minute only)
        String input = "RAB05 ";
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("prec")).isEqualTo("RA");
        assertThat(matcher.group("begin")).isEqualTo("B");
        assertThat(matcher.group("begint")).isEqualTo("05");
        assertThat(matcher.group("end")).isNull();
    }

    @Test
    void testBeginEndWeatherPattern_SimpleEnd() {
        // RAE30 - Rain ended at :30 (minute only)
        String input = "RAE30 ";
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("prec")).isEqualTo("RA");
        assertThat(matcher.group("begin")).isNull();
        assertThat(matcher.group("end")).isEqualTo("E");
        assertThat(matcher.group("endt")).isEqualTo("30");
    }

    @Test
    void testBeginEndWeatherPattern_BeginAndEnd() {
        // RAB15E30 - Rain began :15, ended :30 (both minute only)
        String input = "RAB15E30 ";
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("prec")).isEqualTo("RA");
        assertThat(matcher.group("begin")).isEqualTo("B");
        assertThat(matcher.group("begint")).isEqualTo("15");
        assertThat(matcher.group("end")).isEqualTo("E");
        assertThat(matcher.group("endt")).isEqualTo("30");
    }

    @Test
    void testBeginEndWeatherPattern_FullTimestamp() {
        // FZRAB1159E1240 - Freezing rain began 11:59, ended 12:40 (4-digit format)
        String input = "FZRAB1159E1240 ";
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("desc")).isEqualTo("FZ");
        assertThat(matcher.group("prec")).isEqualTo("RA");
        assertThat(matcher.group("begin")).isEqualTo("B");
        assertThat(matcher.group("begint")).isEqualTo("1159");
        assertThat(matcher.group("end")).isEqualTo("E");
        assertThat(matcher.group("endt")).isEqualTo("1240");
    }

    @Test
    void testBeginEndWeatherPattern_WithLightIntensity() {
        // -RAB05 - Light rain began :05
        String input = "-RAB05 ";
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("int")).isEqualTo("-");
        assertThat(matcher.group("prec")).isEqualTo("RA");
        assertThat(matcher.group("begint")).isEqualTo("05");
    }

    @Test
    void testBeginEndWeatherPattern_WithHeavyIntensity() {
        // +TSRAB20E45 - Heavy thunderstorm with rain began :20, ended :45
        String input = "+TSRAB20E45 ";
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("int")).isEqualTo("+");
        assertThat(matcher.group("desc")).isEqualTo("TS");
        assertThat(matcher.group("prec")).isEqualTo("RA");
        assertThat(matcher.group("begint")).isEqualTo("20");
        assertThat(matcher.group("endt")).isEqualTo("45");
    }

    @Test
    void testBeginEndWeatherPattern_Thunderstorm() {
        // TSB0159E0240 - Thunderstorm began 01:59, ended 02:40
        String input = "TSB0159E0240 ";
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("desc")).isEqualTo("TS");
        assertThat(matcher.group("begint")).isEqualTo("0159");
        assertThat(matcher.group("endt")).isEqualTo("0240");
    }

    @Test
    void testBeginEndWeatherPattern_Snow() {
        // SNB30 - Snow began :30
        String input = "SNB30 ";
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("prec")).isEqualTo("SN");
        assertThat(matcher.group("begint")).isEqualTo("30");
        assertThat(matcher.group("end")).isNull();
    }

    @Test
    void testBeginEndWeatherPattern_FreezingRain() {
        // FZRAE42 - Freezing rain ended :42
        String input = "FZRAE42 ";
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("desc")).isEqualTo("FZ");
        assertThat(matcher.group("prec")).isEqualTo("RA");
        assertThat(matcher.group("endt")).isEqualTo("42");
    }

    @Test
    void testBeginEndWeatherPattern_Obscuration() {
        // BRB10E25 - Mist began :10, ended :25
        String input = "BRB10E25 ";
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("obsc")).isEqualTo("BR");
        assertThat(matcher.group("begint")).isEqualTo("10");
        assertThat(matcher.group("endt")).isEqualTo("25");
    }

    @Test
    void testBeginEndWeatherPattern_Fog() {
        // FGB0520E0630 - Fog began 05:20, ended 06:30
        String input = "FGB0520E0630 ";
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("obsc")).isEqualTo("FG");
        assertThat(matcher.group("begint")).isEqualTo("0520");
        assertThat(matcher.group("endt")).isEqualTo("0630");
    }

    @Test
    void testBeginEndWeatherPattern_MixedTimeFormats() {
        // RAB1159E30 - Rain began 11:59 (4-digit), ended :30 (2-digit)
        // Note: This is technically valid but unusual in practice
        String input = "RAB1159E30 ";
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("prec")).isEqualTo("RA");
        assertThat(matcher.group("begint")).isEqualTo("1159");
        assertThat(matcher.group("endt")).isEqualTo("30");
    }

    @Test
    void testBeginEndWeatherPattern_Drizzle() {
        // DZB05E20 - Drizzle began :05, ended :20
        String input = "DZB05E20 ";
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("prec")).isEqualTo("DZ");
        assertThat(matcher.group("begint")).isEqualTo("05");
        assertThat(matcher.group("endt")).isEqualTo("20");
    }

    @Test
    void testBeginEndWeatherPattern_WithIntensity2() {
        // -SNRAB15E30 - Light snow and rain began :15, ended :30
        // Note: int2 captures intensity at the end if present
        String input = "-SNRAB15E30 ";
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("int")).isEqualTo("-");
        assertThat(matcher.group("prec")).contains("SN");
        assertThat(matcher.group("begint")).isEqualTo("15");
        assertThat(matcher.group("endt")).isEqualTo("30");
    }

    @Test
    void testBeginEndWeatherPattern_EdgeCaseMidnight() {
        // RAB0000E0030 - Rain began 00:00 (midnight), ended 00:30
        String input = "RAB0000E0030 ";
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("prec")).isEqualTo("RA");
        assertThat(matcher.group("begint")).isEqualTo("0000");
        assertThat(matcher.group("endt")).isEqualTo("0030");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "RAB05 ",           // Rain began :05
            "SNE30 ",           // Snow ended :30
            "FZRAB1159 ",       // Freezing rain began 11:59
            "TSE0240 ",         // Thunderstorm ended 02:40
            "BRB10E25 ",        // Mist began :10, ended :25
            "-DZB05 ",          // Light drizzle began :05
            "+TSRAB20E45 ",     // Heavy thunderstorm with rain began :20, ended :45
            "PLB1545 ",         // Ice pellets began 15:45 (edge case: 4-digit)
            "SNB2359 ",         // Snow began 23:59 (edge case: end of day)
            "RAB05"             // Word boundary test (no trailing space)
    })
    void testBeginEndWeatherPattern_VariousFormats(String input) {
        Matcher matcher = RegExprConst.BEGIN_END_WEATHER_PATTERN.matcher(input);
        assertThat(matcher.find())
                .as("Pattern should match: %s", input.trim())
                .isTrue();
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

    // ========== TAF PATTERN TESTS ==========

    @Test
    @DisplayName("VALIDITY_PATTERN should match TAF validity period")
    void testValidityPattern() {
        String input = "1520/1624 ";
        Matcher matcher = RegExprConst.VALIDITY_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("from")).isEqualTo("1520");
        assertThat(matcher.group("to")).isEqualTo("1624");
    }

    @Test
    @DisplayName("FM_PATTERN should match FM change group")
    void testFmPattern() {
        String input = "FM152100 ";
        Matcher matcher = RegExprConst.FM_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("time")).isEqualTo("152100");
    }

    @Test
    @DisplayName("TEMPO_PATTERN should match TEMPO change group with validity")
    void testTempoPattern() {
        String input = "TEMPO 3003/3011 ";
        Matcher matcher = RegExprConst.TEMPO_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("from")).isEqualTo("3003");
        assertThat(matcher.group("to")).isEqualTo("3011");
    }

    @Test
    @DisplayName("BECMG_PATTERN should match BECMG change group with validity")
    void testBecmgPattern() {
        String input = "BECMG 3003/3011 ";
        Matcher matcher = RegExprConst.BECMG_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("from")).isEqualTo("3003");
        assertThat(matcher.group("to")).isEqualTo("3011");
    }

    @Test
    @DisplayName("PROB_PATTERN should match PROB change group with validity")
    void testProbPattern() {
        String input = "PROB30 3003/3011 ";
        Matcher matcher = RegExprConst.PROB_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("prob")).isEqualTo("30");
        assertThat(matcher.group("from")).isEqualTo("3003");
        assertThat(matcher.group("to")).isEqualTo("3011");
    }

    @Test
    @DisplayName("PROB_PATTERN should match PROB with TEMPO")
    void testProbPatternWithTempo() {
        String input = "PROB40 TEMPO 3003/3011 ";
        Matcher matcher = RegExprConst.PROB_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("prob")).isEqualTo("40");
        assertThat(matcher.group("from")).isEqualTo("3003");
        assertThat(matcher.group("to")).isEqualTo("3011");
    }

    @ParameterizedTest
    @CsvSource({
            "'PROB30 3003/3011 ', 30, 'PROB30'",
            "'PROB40 3003/3011 ', 40, 'PROB40'",
            "'PROB30 TEMPO 3003/3011 ', 30, 'PROB30 TEMPO'",
            "'PROB40 TEMPO 3003/3011 ', 40, 'PROB40 TEMPO'"
    })
    @DisplayName("PROB_PATTERN should match various probability formats")
    void testProbPatternVariations(String input, String expectedProb, String scenario) {
        Matcher matcher = RegExprConst.PROB_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("prob"))
                .as("Probability should match: %s", scenario)
                .isEqualTo(expectedProb);
    }

    @Test
    @DisplayName("TEMP_FORECAST_PATTERN should match TX temperature forecast")
    void testTempForecastPattern_Max() {
        String input = "TX15/1518Z ";
        Matcher matcher = RegExprConst.TEMP_FORECAST_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("X");
        assertThat(matcher.group("sign")).isNull();
        assertThat(matcher.group("temp")).isEqualTo("15");
        assertThat(matcher.group("day")).isEqualTo("15");
        assertThat(matcher.group("hour")).isEqualTo("18");
    }

    @Test
    @DisplayName("TEMP_FORECAST_PATTERN should match TN temperature forecast")
    void testTempForecastPattern_Min() {
        String input = "TN05/1510Z ";
        Matcher matcher = RegExprConst.TEMP_FORECAST_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("N");
        assertThat(matcher.group("sign")).isNull();
        assertThat(matcher.group("temp")).isEqualTo("05");
        assertThat(matcher.group("day")).isEqualTo("15");
        assertThat(matcher.group("hour")).isEqualTo("10");
    }

    @Test
    @DisplayName("TEMP_FORECAST_PATTERN should match negative temperature")
    void testTempForecastPattern_Negative() {
        String input = "TNM05/1510Z ";
        Matcher matcher = RegExprConst.TEMP_FORECAST_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("N");
        assertThat(matcher.group("sign")).isEqualTo("M");
        assertThat(matcher.group("temp")).isEqualTo("05");
        assertThat(matcher.group("day")).isEqualTo("15");
        assertThat(matcher.group("hour")).isEqualTo("10");
    }

    @ParameterizedTest
    @CsvSource({
            "'TX15/1518Z ', X, '', 15, 'Max temp positive'",
            "'TN05/1510Z ', N, '', 05, 'Min temp positive'",
            "'TNM05/1510Z ', N, M, 05, 'Min temp negative'",
            "'TXM12/1612Z ', X, M, 12, 'Max temp negative'",
            "'TX00/1500Z ', X, '', 00, 'Zero temp'",
            "'TN99/3023Z ', N, '', 99, 'High temp value'"
    })
    @DisplayName("TEMP_FORECAST_PATTERN should match various temperature formats")
    void testTempForecastPatternVariations(String input, String expectedType,
                                           String expectedSign, String expectedTemp, String scenario) {
        Matcher matcher = RegExprConst.TEMP_FORECAST_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("type"))
                .as("Type should match: %s", scenario)
                .isEqualTo(expectedType);

        if (expectedSign.isEmpty()) {
            assertThat(matcher.group("sign"))
                    .as("Sign should be null: %s", scenario)
                    .isNull();
        } else {
            assertThat(matcher.group("sign"))
                    .as("Sign should match: %s", scenario)
                    .isEqualTo(expectedSign);
        }

        assertThat(matcher.group("temp"))
                .as("Temperature should match: %s", scenario)
                .isEqualTo(expectedTemp);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "FM152100 ",
            "FM010000 ",
            "FM302359 ",
            "TEMPO 0102/0106 ",
            "BECMG 1520/1524 ",
            "PROB30 3003/3011 ",
            "TX15/1518Z ",
            "TNM05/1510Z ",
            "1520/1624 "
    })
    @DisplayName("TAF patterns should match valid inputs")
    void testTafPatternsVariousInputs(String input) {
        // This test ensures all TAF patterns compile and can match basic inputs
        boolean matched = false;

        if (input.startsWith("FM")) {
            matched = RegExprConst.FM_PATTERN.matcher(input).find();
        } else if (input.startsWith("TEMPO")) {
            matched = RegExprConst.TEMPO_PATTERN.matcher(input).find();
        } else if (input.startsWith("BECMG")) {
            matched = RegExprConst.BECMG_PATTERN.matcher(input).find();
        } else if (input.startsWith("PROB")) {
            matched = RegExprConst.PROB_PATTERN.matcher(input).find();
        } else if (input.startsWith("TX") || input.startsWith("TN")) {
            matched = RegExprConst.TEMP_FORECAST_PATTERN.matcher(input).find();
        } else if (input.matches("^\\d{4}/\\d{4}.*")) {
            matched = RegExprConst.VALIDITY_PATTERN.matcher(input).find();
        }

        assertThat(matched)
                .as("TAF pattern should match: %s", input.trim())
                .isTrue();
    }
}
