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
 * <p>
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
        assertThat(RegExprConst.PRES_RF_RAPDLY_PATTERN).isNotNull();
        assertThat(RegExprConst.ICING_PATTERN).isNotNull();
        assertThat(RegExprConst.CLOUD_OKTA_PATTERN).isNotNull();
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

    // ========== PRESSURE RAPID CHANGE PATTERN TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'PRESRR ', R, 'Rising rapidly'",
            "'PRESFR ', F, 'Falling rapidly'"
    })
    @DisplayName("PRES_RF_RAPDLY_PATTERN should match rising/falling codes")
    void testPressureRapidChangePattern(String input, String expectedCode, String scenario) {
        Matcher matcher = RegExprConst.PRES_RF_RAPDLY_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("presrisfal"))
                .as("Code should match: %s", scenario)
                .isEqualTo(expectedCode);
    }

    @Test
    @DisplayName("PRES_RF_RAPDLY_PATTERN should match at end of string (no trailing space)")
    void testPressureRapidChangePattern_AtEndOfString() {
        String input = "PRESFR";
        Matcher matcher = RegExprConst.PRES_RF_RAPDLY_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("presrisfal")).isEqualTo("F");
    }

// ========== ICING PATTERN TESTS ==========

    @Test
    @DisplayName("ICING_PATTERN should match bare ICG with qualifier")
    void testIcingPattern_Bare() {
        String input = "ICG PAST HR ";
        Matcher matcher = RegExprConst.ICING_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("ICG");
        assertThat(matcher.group("typeic")).isNull();
        assertThat(matcher.group("typeip")).isNull();
        assertThat(matcher.group("extra")).isEqualTo("PAST HR");
    }

    @Test
    @DisplayName("ICING_PATTERN should match ICG with IC (in clouds)")
    void testIcingPattern_InClouds() {
        String input = "ICGIC PAST HR ";
        Matcher matcher = RegExprConst.ICING_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("typeic")).isEqualTo("IC");
        assertThat(matcher.group("typeip")).isNull();
        assertThat(matcher.group("extra")).isEqualTo("PAST HR");
    }

    @Test
    @DisplayName("ICING_PATTERN should match ICG with IP (in precipitation)")
    void testIcingPattern_InPrecipitation() {
        String input = "ICGIP PAST HR ";
        Matcher matcher = RegExprConst.ICING_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("typeic")).isNull();
        assertThat(matcher.group("typeip")).isEqualTo("IP");
        assertThat(matcher.group("extra")).isEqualTo("PAST HR");
    }

    @Test
    @DisplayName("ICING_PATTERN should match at end of string (no trailing space)")
    void testIcingPattern_AtEndOfString() {
        String input = "ICG PAST HR";
        Matcher matcher = RegExprConst.ICING_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("extra")).isEqualTo("PAST HR");
    }

    @Test
    @DisplayName("ICING_PATTERN should not match bare ICG without qualifier")
    void testIcingPattern_DoesNotMatchWithoutQualifier() {
        String input = "ICG ";
        Matcher matcher = RegExprConst.ICING_PATTERN.matcher(input);

        assertThat(matcher.find()).isFalse();
    }

    // ========== CLOUD OKTA PATTERN TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'CI1 ', CI, 1, 'Cirrus 1 okta'",
            "'CI8 ', CI, 8, 'Cirrus 8 oktas'",
            "'AC8SC1 ', AC, 8, 'Chained - first code'",
            "'SC5 ', SC, 5, 'Stratocumulus 5 oktas'"
    })
    @DisplayName("CLOUD_OKTA_PATTERN should match cloud type with oktas 1-8")
    void testCloudOktaPattern_ValidOktas(String input, String expectedCloud, String expectedOkta, String scenario) {
        Matcher matcher = RegExprConst.CLOUD_OKTA_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("cloud"))
                .as("Cloud type should match: %s", scenario)
                .isEqualTo(expectedCloud);
        assertThat(matcher.group("okta"))
                .as("Okta should match: %s", scenario)
                .isEqualTo(expectedOkta);
    }

    @Test
    @DisplayName("CLOUD_OKTA_PATTERN should match zero-okta cloud type (CI0 - CYHZ)")
    void testCloudOktaPattern_ZeroOktas() {
        String input = "CI0 ";
        Matcher matcher = RegExprConst.CLOUD_OKTA_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("cloud")).isEqualTo("CI");
        assertThat(matcher.group("okta")).isEqualTo("0");
    }

    @Test
    @DisplayName("CLOUD_OKTA_PATTERN should match zero-okta cloud type at end of chained group")
    void testCloudOktaPattern_ZeroOktasChained() {
        // CU1AS2CI0 - the zero-okta code appearing after two valid ones
        String input = "CI0";
        Matcher matcher = RegExprConst.CLOUD_OKTA_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("cloud")).isEqualTo("CI");
        assertThat(matcher.group("okta")).isEqualTo("0");
    }

    @Test
    @DisplayName("CLOUD_OKTA_PATTERN should not match okta 9 or higher")
    void testCloudOktaPattern_DoesNotMatchOktaNine() {
        String input = "CI9 ";
        Matcher matcher = RegExprConst.CLOUD_OKTA_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        // Okta group should not capture "9" - pattern falls back to bare
        // cloud type match with no okta
        assertThat(matcher.group("okta")).isNull();
    }

    @Test
    @DisplayName("CLOUD_OKTA_PATTERN should match cloud type with EMBDD location qualifier (TCU EMBDD - CYVR)")
    void testCloudOktaPattern_Embdd() {
        String input = "TCU EMBDD";
        Matcher matcher = RegExprConst.CLOUD_OKTA_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("cloud")).isEqualTo("TCU");
        assertThat(matcher.group("okta")).isNull();
        assertThat(matcher.group("direction")).isEqualTo("EMBDD");
    }

    @Test
    @DisplayName("CLOUD_OKTA_PATTERN should match CB with EMBDD location qualifier")
    void testCloudOktaPattern_EmbddWithCb() {
        // CB is not in the cloud-type alternation itself but the pattern's
        // "cloud" group includes TCU/CU/etc - confirming EMBDD works with
        // any qualifying code in the alternation, using CU as a stand-in
        String input = "CU EMBDD";
        Matcher matcher = RegExprConst.CLOUD_OKTA_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("cloud")).isEqualTo("CU");
        assertThat(matcher.group("direction")).isEqualTo("EMBDD");
    }

    @ParameterizedTest
    @ValueSource(strings = {"OHD-ALQDS", "ALQDS", "OHD", "TR", "EMBDD"})
    @DisplayName("CLOUD_OKTA_PATTERN should match all recognized location qualifiers")
    void testCloudOktaPattern_AllLocationQualifiers(String qualifier) {
        String input = "SC " + qualifier;
        Matcher matcher = RegExprConst.CLOUD_OKTA_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("direction")).isEqualTo(qualifier);
    }

    @Test
    @DisplayName("CLOUD_OKTA_PATTERN should match with MOVG movement (unaffected by EMBDD addition)")
    void testCloudOktaPattern_Movement() {
        String input = "CI MOVG NE";
        Matcher matcher = RegExprConst.CLOUD_OKTA_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("cloud")).isEqualTo("CI");
        assertThat(matcher.group("verb")).isEqualTo("MOVG");
        assertThat(matcher.group("dirm")).isEqualTo("NE");
        assertThat(matcher.group("direction")).isNull();
    }

    @Test
    @DisplayName("CLOUD_OKTA_PATTERN should match with MDT intensity")
    void testCloudOktaPattern_Intensity() {
        String input = "MDT CU OHD";
        Matcher matcher = RegExprConst.CLOUD_OKTA_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("intensity")).isEqualTo("MDT ");
        assertThat(matcher.group("cloud")).isEqualTo("CU");
        assertThat(matcher.group("direction")).isEqualTo("OHD");
    }

    @Test
    @DisplayName("CLOUD_OKTA_PATTERN should match bare cloud type with no qualifier (structural match only)")
    void testCloudOktaPattern_BareCloudTypeStillMatchesStructurally() {
        // The regex itself will still match a bare cloud type with nothing
        // following - rejecting it as invalid is extractCloudTypeFromMatcher's
        // responsibility, not the pattern's. This test documents that the
        // pattern alone does not enforce "must have a qualifier".
        String input = "TCU ";
        Matcher matcher = RegExprConst.CLOUD_OKTA_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("cloud")).isEqualTo("TCU");
        assertThat(matcher.group("okta")).isNull();
        assertThat(matcher.group("direction")).isNull();
        assertThat(matcher.group("verb")).isNull();
    }

    // ========== WIND AT LOCATION PATTERN TESTS ==========

    @Test
    @DisplayName("WIND_AT_LOCATION_PATTERN should match wind at altitude")
    void testWindAtLocationPattern_Altitude() {
        String input = "WIND 1400FT 23010KT ";
        Matcher matcher = RegExprConst.WIND_AT_LOCATION_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("height")).isEqualTo("1400");
        assertThat(matcher.group("runway")).isNull();
        assertThat(matcher.group("dir")).isEqualTo("230");
        assertThat(matcher.group("speed")).isEqualTo("10");
        assertThat(matcher.group("gust")).isNull();
        assertThat(matcher.group("unit")).isEqualTo("KT");
    }

    @Test
    @DisplayName("WIND_AT_LOCATION_PATTERN should match wind at runway")
    void testWindAtLocationPattern_Runway() {
        String input = "WIND RWY 26 00000KT ";
        Matcher matcher = RegExprConst.WIND_AT_LOCATION_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("height")).isNull();
        assertThat(matcher.group("runway")).isEqualTo("26");
        assertThat(matcher.group("dir")).isEqualTo("000");
        assertThat(matcher.group("speed")).isEqualTo("00");
        assertThat(matcher.group("unit")).isEqualTo("KT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"32", "32L", "32R", "32C", "4", "4L"})
    @DisplayName("WIND_AT_LOCATION_PATTERN should match runway with and without L/R/C suffix, single or double digit")
    void testWindAtLocationPattern_RunwayDesignators(String runway) {
        String input = "WIND RWY " + runway + " 00000KT ";
        Matcher matcher = RegExprConst.WIND_AT_LOCATION_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("runway")).isEqualTo(runway);
    }

    @Test
    @DisplayName("WIND_AT_LOCATION_PATTERN should match VRB direction at runway")
    void testWindAtLocationPattern_VrbDirectionRunway() {
        String input = "WIND RWY 32 VRB01KT ";
        Matcher matcher = RegExprConst.WIND_AT_LOCATION_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("runway")).isEqualTo("32");
        assertThat(matcher.group("dir")).isEqualTo("VRB");
        assertThat(matcher.group("speed")).isEqualTo("01");
    }

    @Test
    @DisplayName("WIND_AT_LOCATION_PATTERN should match VRB direction at altitude")
    void testWindAtLocationPattern_VrbDirectionAltitude() {
        String input = "WIND 1119FT VRB03KT ";
        Matcher matcher = RegExprConst.WIND_AT_LOCATION_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("height")).isEqualTo("1119");
        assertThat(matcher.group("dir")).isEqualTo("VRB");
        assertThat(matcher.group("speed")).isEqualTo("03");
    }

    @Test
    @DisplayName("WIND_AT_LOCATION_PATTERN should match with gust")
    void testWindAtLocationPattern_WithGust() {
        String input = "WIND 1400FT 23010G20KT ";
        Matcher matcher = RegExprConst.WIND_AT_LOCATION_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("speed")).isEqualTo("10");
        assertThat(matcher.group("gust")).isEqualTo("20");
        assertThat(matcher.group("unit")).isEqualTo("KT");
    }

    @Test
    @DisplayName("WIND_AT_LOCATION_PATTERN should match without gust")
    void testWindAtLocationPattern_WithoutGust() {
        String input = "WIND 1400FT 23010KT ";
        Matcher matcher = RegExprConst.WIND_AT_LOCATION_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("gust")).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "KT, 'Knots (single K T)'",
            "KTS, 'Knots (plural)'",
            "MPS, 'Meters per second'",
            "KMH, 'Kilometers per hour'"
    })
    @DisplayName("WIND_AT_LOCATION_PATTERN should match all valid unit formats")
    void testWindAtLocationPattern_AllUnits(String unit, String scenario) {
        String input = "WIND 1400FT 23010" + unit + " ";
        Matcher matcher = RegExprConst.WIND_AT_LOCATION_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("unit"))
                .as("Unit should match: %s", scenario)
                .isEqualTo(unit);
    }

    @Test
    @DisplayName("WIND_AT_LOCATION_PATTERN should match three-digit speed and gust")
    void testWindAtLocationPattern_ThreeDigitSpeedAndGust() {
        String input = "WIND 1400FT 230105G120KT ";
        Matcher matcher = RegExprConst.WIND_AT_LOCATION_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("speed")).isEqualTo("105");
        assertThat(matcher.group("gust")).isEqualTo("120");
    }

    @Test
    @DisplayName("WIND_AT_LOCATION_PATTERN should match at end of string (no trailing space)")
    void testWindAtLocationPattern_AtEndOfString() {
        String input = "WIND 1400FT 23010KT";
        Matcher matcher = RegExprConst.WIND_AT_LOCATION_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("unit")).isEqualTo("KT");
    }

    @ParameterizedTest
    @CsvSource({
            "'1400FT 23010KT ', 'Missing WIND prefix'",
            "'WIND RWY 320 00000KT ', 'Runway designator with three digits'",
            "'WIND 1400FT 23010XYZ ', 'Invalid unit'"
    })
    @DisplayName("WIND_AT_LOCATION_PATTERN should not match invalid formats")
    void testWindAtLocationPattern_DoesNotMatchInvalidFormats(String input, String scenario) {
        Matcher matcher = RegExprConst.WIND_AT_LOCATION_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Should not match: %s", scenario)
                .isFalse();
    }

    // ========== THUNDERSTORM CLOUD LOCATION PATTERN TESTS ==========

    @Test
    @DisplayName("TS_CLD_LOC_PATTERN should not match bare type immediately followed by EMBDD (word-bounded)")
    void testThunderstormCloudLocationPattern_DoesNotMatchEmbdd() {
        String input = "TCU EMBDD";
        Matcher matcher = RegExprConst.TS_CLD_LOC_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("TCU EMBDD should be excluded here so CLOUD_OKTA_PATTERN can match it instead")
                .isFalse();
    }

    @Test
    @DisplayName("TS_CLD_LOC_PATTERN should still match a bare type followed by a different word starting with EMBDD")
    void testThunderstormCloudLocationPattern_MatchesWhenNotExactlyEmbdd() {
        // Confirms the word-bounded lookahead doesn't over-exclude - a token
        // that merely starts with "EMBDD" (not the whole word) should not be
        // blocked, since it isn't the literal EMBDD qualifier
        String input = "TCU EMBDDXXX";
        Matcher matcher = RegExprConst.TS_CLD_LOC_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Should match as a bare TCU, since EMBDDXXX is not the literal EMBDD token")
                .isTrue();
        assertThat(matcher.group("type")).isEqualTo("TCU");
    }

    @Test
    @DisplayName("TS_CLD_LOC_PATTERN should still match TCU with a normal location qualifier")
    void testThunderstormCloudLocationPattern_StillMatchesNormalQualifiers() {
        // Confirms the EMBDD exclusion doesn't regress any existing qualifier
        String input = "TCU DSNT S";
        Matcher matcher = RegExprConst.TS_CLD_LOC_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("TCU");
        assertThat(matcher.group("loc")).isEqualTo("DSNT");
        assertThat(matcher.group("dirchain")).isEqualTo("S");
    }

    // ========== CB / TCU PATTERN AMBIGUITY REGRESSION TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'CB OHD ', OHD, 'Overhead - existing qualifier'",
            "'CB VC ', VC, 'In vicinity - existing qualifier'",
            "'CB DSNT ', DSNT, 'Distant - existing qualifier'",
            "'CB TR ', TR, 'Trace - existing qualifier'"
    })
    @DisplayName("TS_CLD_LOC_PATTERN should still match CB with real location qualifiers (CLOUD_OKTA_PATTERN now also includes CB)")
    void testThunderstormCloudLocationPattern_CbStillMatchesRealQualifiers(String input, String expectedLoc, String scenario) {
        Matcher matcher = RegExprConst.TS_CLD_LOC_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Should still match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("type")).isEqualTo("CB");
        assertThat(matcher.group("loc")).isEqualTo(expectedLoc);
    }

    @Test
    @DisplayName("TS_CLD_LOC_PATTERN should still match CB with direction and movement")
    void testThunderstormCloudLocationPattern_CbStillMatchesDirectionAndMovement() {
        String input = "CB DSNT W-NW MOV E";
        Matcher matcher = RegExprConst.TS_CLD_LOC_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("CB");
        assertThat(matcher.group("loc")).isEqualTo("DSNT");
        assertThat(matcher.group("dirchain")).isEqualTo("W-NW");
        assertThat(matcher.group("dirm")).isEqualTo("E");
    }

    @Test
    @DisplayName("TS_CLD_LOC_PATTERN should not match CB immediately followed by EMBDD")
    void testThunderstormCloudLocationPattern_CbDoesNotMatchEmbdd() {
        String input = "CB EMBDD";
        Matcher matcher = RegExprConst.TS_CLD_LOC_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("CB EMBDD should be excluded here so CLOUD_OKTA_PATTERN can match it instead")
                .isFalse();
    }

    @Test
    @DisplayName("CLOUD_OKTA_PATTERN should match CB with EMBDD location qualifier")
    void testCloudOktaPattern_CbEmbdd() {
        String input = "CB EMBDD";
        Matcher matcher = RegExprConst.CLOUD_OKTA_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("cloud")).isEqualTo("CB");
        assertThat(matcher.group("direction")).isEqualTo("EMBDD");
    }

    @Test
    @DisplayName("CLOUD_OKTA_PATTERN should match CB with oktas, distinct from TS_CLD_LOC_PATTERN's location-based CB")
    void testCloudOktaPattern_CbWithOktas() {
        String input = "CB4 ";
        Matcher matcher = RegExprConst.CLOUD_OKTA_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("cloud")).isEqualTo("CB");
        assertThat(matcher.group("okta")).isEqualTo("4");
    }

    // ========== PRESSURE Q PATTERN TESTS ==========

    @ParameterizedTest
    @ValueSource(strings = {"QFE", "QNH", "QNE"})
    @DisplayName("PRESS_Q_PATTERN should match all valid pressure type codes alone")
    void testPressQPattern_TypeAlone(String type) {
        String input = type + " ";
        Matcher matcher = RegExprConst.PRESS_Q_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("pressq")).isEqualTo(type);
        assertThat(matcher.group("pressmm")).isNull();
        assertThat(matcher.group("pressmb")).isNull();
    }

    @Test
    @DisplayName("PRESS_Q_PATTERN should match QNH with four-digit value")
    void testPressQPattern_QnhFourDigit() {
        String input = "QNH1013 ";
        Matcher matcher = RegExprConst.PRESS_Q_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("pressq")).isEqualTo("QNH");
        assertThat(matcher.group("pressmm")).isEqualTo("1013");
        assertThat(matcher.group("pressmb")).isNull();
    }

    @Test
    @DisplayName("PRESS_Q_PATTERN should match QFE with three-digit value")
    void testPressQPattern_QfeThreeDigit() {
        String input = "QFE760 ";
        Matcher matcher = RegExprConst.PRESS_Q_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("pressq")).isEqualTo("QFE");
        assertThat(matcher.group("pressmm")).isEqualTo("760");
        assertThat(matcher.group("pressmb")).isNull();
    }

    @Test
    @DisplayName("PRESS_Q_PATTERN should match value with mb/hPa secondary value")
    void testPressQPattern_WithSecondaryValue() {
        String input = "QNH1013/760 ";
        Matcher matcher = RegExprConst.PRESS_Q_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("pressq")).isEqualTo("QNH");
        assertThat(matcher.group("pressmm")).isEqualTo("1013");
        assertThat(matcher.group("pressmb")).isEqualTo("760");
    }

    @Test
    @DisplayName("PRESS_Q_PATTERN should match three-digit primary with three-digit secondary")
    void testPressQPattern_ThreeDigitBoth() {
        String input = "QFE760/999 ";
        Matcher matcher = RegExprConst.PRESS_Q_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("pressmm")).isEqualTo("760");
        assertThat(matcher.group("pressmb")).isEqualTo("999");
    }

    @Test
    @DisplayName("PRESS_Q_PATTERN should match QNE alone")
    void testPressQPattern_Qne() {
        String input = "QNE ";
        Matcher matcher = RegExprConst.PRESS_Q_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("pressq")).isEqualTo("QNE");
        assertThat(matcher.group("pressmm")).isNull();
    }

    @Test
    @DisplayName("PRESS_Q_PATTERN should not match without trailing whitespace")
    void testPressQPattern_DoesNotMatchWithoutTrailingSpace() {
        String input = "QNH1013";
        Matcher matcher = RegExprConst.PRESS_Q_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern requires trailing \\s+, unlike most other RegExprConst patterns which accept end-of-string too")
                .isFalse();
    }

    @Test
    @DisplayName("PRESS_Q_PATTERN should not match invalid pressure type code")
    void testPressQPattern_DoesNotMatchInvalidType() {
        String input = "QXX1013 ";
        Matcher matcher = RegExprConst.PRESS_Q_PATTERN.matcher(input);

        assertThat(matcher.find()).isFalse();
    }

    @Test
    @DisplayName("PRESS_Q_PATTERN should not match when a secondary value appears without a primary value")
    void testPressQPattern_DoesNotMatchSecondaryWithoutPrimary() {
        // "/760" cannot satisfy the pattern: pressmb is nested inside pressmm's
        // optional wrapper, so pressmm must match first. Here pressmm fails
        // (next char is "/", not a digit), the optional group is skipped
        // entirely, and the pattern then requires \s+ immediately after "QNH" -
        // but the next character is "/", not whitespace, so the whole match
        // fails outright (no partial "QNH"-only match occurs).
        String input = "QNH/760 ";
        Matcher matcher = RegExprConst.PRESS_Q_PATTERN.matcher(input);

        assertThat(matcher.find()).isFalse();
    }

    // ========== DENSITY ALTITUDE PATTERN TESTS ==========

    @Test
    @DisplayName("DENSITY_ALTITUDE_PATTERN should match a typical density altitude - CYYZ real-world")
    void testDensityAltitudePattern_Typical() {
        String input = "DENSITY ALT 1500FT ";
        Matcher matcher = RegExprConst.DENSITY_ALTITUDE_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("DENSITY ALT");
        assertThat(matcher.group("denalt")).isEqualTo("1500");
        assertThat(matcher.group("units")).isEqualTo("FT");
    }

    @ParameterizedTest
    @CsvSource({
            "'DENSITY ALT 0FT ', 0, 'Single digit / zero'",
            "'DENSITY ALT 800FT ', 800, 'Three digits'",
            "'DENSITY ALT 12000FT ', 12000, 'Five digits (upper bound)'"
    })
    @DisplayName("DENSITY_ALTITUDE_PATTERN should match value lengths from 1 to 5 digits")
    void testDensityAltitudePattern_ValueLengths(String input, String expectedValue, String scenario) {
        Matcher matcher = RegExprConst.DENSITY_ALTITUDE_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("denalt"))
                .as("Value should match: %s", scenario)
                .isEqualTo(expectedValue);
    }

    @Test
    @DisplayName("DENSITY_ALTITUDE_PATTERN should match at end of string (no trailing space)")
    void testDensityAltitudePattern_AtEndOfString() {
        String input = "DENSITY ALT 700FT";
        Matcher matcher = RegExprConst.DENSITY_ALTITUDE_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("denalt")).isEqualTo("700");
    }

    @Test
    @DisplayName("DENSITY_ALTITUDE_PATTERN should not match six-digit value")
    void testDensityAltitudePattern_DoesNotMatchSixDigits() {
        // \d{1,5} should only consume the first 5 digits, then require FT
        // immediately after; a 6th digit in that position breaks the match
        String input = "DENSITY ALT 123456FT ";
        Matcher matcher = RegExprConst.DENSITY_ALTITUDE_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Six digits should not match as a whole - FT must immediately follow the captured 1-5 digits")
                .isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "'DENSITY ALT 1500 ', 'Missing FT unit'",
            "'DENSITY ALT FT ', 'Missing value'",
            "'DENSITY ALT 1500ft ', 'Lowercase unit'",
            "'DENSITY ALT 123456FT ', 'Six-digit value (only 1-5 digits allowed before FT)'"
    })
    @DisplayName("DENSITY_ALTITUDE_PATTERN should not match invalid formats")
    void testDensityAltitudePattern_DoesNotMatchInvalidFormats(String input, String scenario) {
        Matcher matcher = RegExprConst.DENSITY_ALTITUDE_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Should not match: %s", scenario)
                .isFalse();
    }

    // ========== VARIABLE CEILING PATTERN TESTS ==========

    @Test
    @DisplayName("VARIABLE_CEILING_PATTERN should match a low ceiling range")
    void testVariableCeilingPattern_LowRange() {
        String input = "CIG 005V010 ";
        Matcher matcher = RegExprConst.VARIABLE_CEILING_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("min")).isEqualTo("005");
        assertThat(matcher.group("max")).isEqualTo("010");
    }

    @ParameterizedTest
    @CsvSource({
            "'CIG 000V002 ', 000, 002, 'Ground level fog range'",
            "'CIG 020V035 ', 020, 035, 'Normal ceiling range'",
            "'CIG 050V100 ', 050, 100, 'High ceiling range'"
    })
    @DisplayName("VARIABLE_CEILING_PATTERN should match various min/max ceiling ranges")
    void testVariableCeilingPattern_VariousRanges(String input, String expectedMin, String expectedMax, String scenario) {
        Matcher matcher = RegExprConst.VARIABLE_CEILING_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("min"))
                .as("Min should match: %s", scenario)
                .isEqualTo(expectedMin);
        assertThat(matcher.group("max"))
                .as("Max should match: %s", scenario)
                .isEqualTo(expectedMax);
    }

    @Test
    @DisplayName("VARIABLE_CEILING_PATTERN should match with no trailing whitespace consumed (\\s* allows zero)")
    void testVariableCeilingPattern_AtEndOfString() {
        String input = "CIG 005V010";
        Matcher matcher = RegExprConst.VARIABLE_CEILING_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("max")).isEqualTo("010");
    }

    @ParameterizedTest
    @CsvSource({
            "'CIG 05V10 ', 'Only 2 digits per side'",
            "'CIG 005-010 ', 'Hyphen instead of V separator'",
            "'005V010 ', 'Missing CIG prefix'",
            "'CIGV005V010 ', 'No space after CIG'"
    })
    @DisplayName("VARIABLE_CEILING_PATTERN should not match invalid formats")
    void testVariableCeilingPattern_DoesNotMatchInvalidFormats(String input, String scenario) {
        Matcher matcher = RegExprConst.VARIABLE_CEILING_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Should not match: %s", scenario)
                .isFalse();
    }

// ========== CEILING SECOND SITE PATTERN TESTS ==========

    @Test
    @DisplayName("CEILING_SECOND_SITE_PATTERN should match ceiling with RY-style runway location")
    void testCeilingSecondSitePattern_RyLocation() {
        String input = "CIG 002 RY11 ";
        Matcher matcher = RegExprConst.CEILING_SECOND_SITE_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("height")).isEqualTo("002");
        assertThat(matcher.group("loc")).isEqualTo("RY11");
    }

    @Test
    @DisplayName("CEILING_SECOND_SITE_PATTERN should match ceiling with RWY-style runway location")
    void testCeilingSecondSitePattern_RwyLocation() {
        String input = "CIG 005 RWY06 ";
        Matcher matcher = RegExprConst.CEILING_SECOND_SITE_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("height")).isEqualTo("005");
        assertThat(matcher.group("loc")).isEqualTo("RWY06");
    }

    @ParameterizedTest
    @CsvSource({
            "'CIG 002 RY11 ', RY11, 'RY with two-digit number'",
            "'CIG 003 RY04L ', RY04L, 'RY with L suffix'",
            "'CIG 003 RY22R ', RY22R, 'RY with R suffix'",
            "'CIG 003 RY22C ', RY22C, 'RY with C suffix'",
            "'CIG 005 RWY06 ', RWY06, 'RWY with two-digit number'",
            "'CIG 005 RWY6 ', RWY6, 'RWY with one-digit number'",
            "'CIG 020 TWR ', TWR, 'Tower location'",
            "'CIG 003 APCH ', APCH, 'Approach location'"
    })
    @DisplayName("CEILING_SECOND_SITE_PATTERN should match all recognized location formats")
    void testCeilingSecondSitePattern_AllLocationFormats(String input, String expectedLoc, String scenario) {
        Matcher matcher = RegExprConst.CEILING_SECOND_SITE_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("loc"))
                .as("Location should match: %s", scenario)
                .isEqualTo(expectedLoc);
    }

    @ParameterizedTest
    @CsvSource({
            "'CIG 010 ', 010, 'No location, trailing space'",
            "'CIG 015', 015, 'No location, end of string'",
            "'CIG 005V010 ', 005, 'Partial match against variable-ceiling input (ambiguity check) - V010 left unconsumed'"
    })
    @DisplayName("CEILING_SECOND_SITE_PATTERN should match height alone when no location follows")
    void testCeilingSecondSitePattern_HeightWithoutLocation(String input, String expectedHeight, String scenario) {
        // The third case documents the overlap with VARIABLE_CEILING_PATTERN:
        // since loc requires a leading \s+ and "V010" has no leading whitespace,
        // the optional loc group is skipped, and \s* at the end matches zero
        // characters - so this pattern matches "CIG 005" alone, leaving "V010 "
        // unconsumed. Handler-level disambiguation (trying VARIABLE_CEILING_PATTERN
        // first, or checking for "V" at the match boundary) is therefore
        // load-bearing, not incidental.
        Matcher matcher = RegExprConst.CEILING_SECOND_SITE_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("height"))
                .as("Height should match: %s", scenario)
                .isEqualTo(expectedHeight);
        assertThat(matcher.group("loc"))
                .as("Location should be null: %s", scenario)
                .isNull();
    }

    @Test
    @DisplayName("CEILING_SECOND_SITE_PATTERN should match ground-level height (000)")
    void testCeilingSecondSitePattern_GroundLevel() {
        String input = "CIG 000 RY11 ";
        Matcher matcher = RegExprConst.CEILING_SECOND_SITE_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("height")).isEqualTo("000");
    }

    @ParameterizedTest
    @CsvSource({
            "'CIG 05 RY11 ', 'Only 2 digits'",
            "'005 RY11 ', 'Missing CIG prefix'",
            "'CIG ABC RY11 ', 'Non-numeric height'"
    })
    @DisplayName("CEILING_SECOND_SITE_PATTERN should not match invalid formats")
    void testCeilingSecondSitePattern_DoesNotMatchInvalidFormats(String input, String scenario) {
        Matcher matcher = RegExprConst.CEILING_SECOND_SITE_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Should not match: %s", scenario)
                .isFalse();
    }

    // ========== OBSCURATION PATTERN TESTS ==========

    @Test
    @DisplayName("OBSCURATION_PATTERN should match ground-level fog")
    void testObscurationPattern_GroundLevelFog() {
        String input = "FEW FG 000 ";
        Matcher matcher = RegExprConst.OBSCURATION_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("coverage")).isEqualTo("FEW");
        assertThat(matcher.group("phenomenon")).isEqualTo("FG");
        assertThat(matcher.group("height")).isEqualTo("000");
    }

    @ParameterizedTest
    @ValueSource(strings = {"FEW", "SCT", "BKN", "OVC"})
    @DisplayName("OBSCURATION_PATTERN should match all valid coverage codes")
    void testObscurationPattern_AllCoverageCodes(String coverage) {
        String input = coverage + " BR 005 ";
        Matcher matcher = RegExprConst.OBSCURATION_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("coverage")).isEqualTo(coverage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FG", "BR", "FU", "HZ", "DU", "SA", "VA", "PY"})
    @DisplayName("OBSCURATION_PATTERN should match all valid phenomenon codes")
    void testObscurationPattern_AllPhenomenonCodes(String phenomenon) {
        String input = "SCT " + phenomenon + " 010 ";
        Matcher matcher = RegExprConst.OBSCURATION_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("phenomenon")).isEqualTo(phenomenon);
    }

    @ParameterizedTest
    @CsvSource({
            "'FEW FG 000 ', 000, 'Ground level'",
            "'SCT FU 010 ', 010, 'Low altitude'",
            "'BKN HZ 025 ', 025, 'Higher altitude'"
    })
    @DisplayName("OBSCURATION_PATTERN should match various heights")
    void testObscurationPattern_VariousHeights(String input, String expectedHeight, String scenario) {
        Matcher matcher = RegExprConst.OBSCURATION_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("height"))
                .as("Height should match: %s", scenario)
                .isEqualTo(expectedHeight);
    }

    @Test
    @DisplayName("OBSCURATION_PATTERN should match at end of string (no trailing space)")
    void testObscurationPattern_AtEndOfString() {
        String input = "FEW FG 000";
        Matcher matcher = RegExprConst.OBSCURATION_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("height")).isEqualTo("000");
    }

    @ParameterizedTest
    @CsvSource({
            "'XXX FG 000 ', 'Invalid coverage code'",
            "'FEW XX 000 ', 'Invalid phenomenon code'",
            "'FEW FG 00 ', 'Only two-digit height'",
            "'FEW FG ', 'Missing height'",
            "'FEWFG000 ', 'No spaces at all'",
            "'FG 000 ', 'Missing coverage code'"
    })
    @DisplayName("OBSCURATION_PATTERN should not match invalid formats")
    void testObscurationPattern_DoesNotMatchInvalidFormats(String input, String scenario) {
        Matcher matcher = RegExprConst.OBSCURATION_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Should not match: %s", scenario)
                .isFalse();
    }

    // ========== AUTOMATED MAINTENANCE PATTERN TESTS ==========

    @ParameterizedTest
    @ValueSource(strings = {"RVRNO", "PWINO", "PNO", "FZRANO", "TSNO"})
    @DisplayName("AUTOMATED_MAINTENANCE_PATTERN should match indicators without a location")
    void testAutomatedMaintenancePattern_NoLocationIndicators(String indicator) {
        String input = indicator + " ";
        Matcher matcher = RegExprConst.AUTOMATED_MAINTENANCE_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("typeam")).isEqualTo(indicator);
        assertThat(matcher.group("loc")).isNull();
        assertThat(matcher.group("typemc")).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "'VISNO RWY06 ', VISNO, RWY06, 'Visibility not available at runway'",
            "'VISNO RY11 ', VISNO, RY11, 'Visibility not available, RY-style runway'",
            "'CHINO N ', CHINO, N, 'Cloud height indicator not available, North'",
            "'CHINO SE ', CHINO, SE, 'Cloud height indicator not available, Southeast'",
            "'CHINO RWY22L ', CHINO, RWY22L, 'Cloud height indicator not available, runway with L suffix'"
    })
    @DisplayName("AUTOMATED_MAINTENANCE_PATTERN should match indicators with a location")
    void testAutomatedMaintenancePattern_WithLocation(String input, String expectedType, String expectedLoc, String scenario) {
        Matcher matcher = RegExprConst.AUTOMATED_MAINTENANCE_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("typeam"))
                .as("Type should match: %s", scenario)
                .isEqualTo(expectedType);
        assertThat(matcher.group("loc"))
                .as("Location should match: %s", scenario)
                .isEqualTo(expectedLoc);
    }

    @Test
    @DisplayName("AUTOMATED_MAINTENANCE_PATTERN should match the maintenance check indicator ($)")
    void testAutomatedMaintenancePattern_MaintenanceCheck() {
        String input = "$";
        Matcher matcher = RegExprConst.AUTOMATED_MAINTENANCE_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("typemc")).isEqualTo("$");
        assertThat(matcher.group("typeam")).isNull();
    }

    @Test
    @DisplayName("AUTOMATED_MAINTENANCE_PATTERN should match $ with trailing space")
    void testAutomatedMaintenancePattern_MaintenanceCheckWithTrailingSpace() {
        String input = "$ ";
        Matcher matcher = RegExprConst.AUTOMATED_MAINTENANCE_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("typemc")).isEqualTo("$");
    }

    @Test
    @DisplayName("AUTOMATED_MAINTENANCE_PATTERN should match at end of string with no location")
    void testAutomatedMaintenancePattern_AtEndOfString() {
        String input = "TSNO";
        Matcher matcher = RegExprConst.AUTOMATED_MAINTENANCE_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("typeam")).isEqualTo("TSNO");
        assertThat(matcher.group("loc")).isNull();
    }

    @Test
    @DisplayName("AUTOMATED_MAINTENANCE_PATTERN should match RY-style location without W or Y variant confusion")
    void testAutomatedMaintenancePattern_RwVariant() {
        // loc's prefix is R(W)?(Y)? - both W and Y independently optional, so
        // "RW06" (W present, Y absent) is technically permitted by the pattern
        // even though real-world remarks use RY or RWY, not RW
        String input = "VISNO RW06 ";
        Matcher matcher = RegExprConst.AUTOMATED_MAINTENANCE_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("loc")).isEqualTo("RW06");
    }

    @Test
    @DisplayName("AUTOMATED_MAINTENANCE_PATTERN should not match an invalid indicator")
    void testAutomatedMaintenancePattern_DoesNotMatchInvalidIndicator() {
        String input = "XXNO ";
        Matcher matcher = RegExprConst.AUTOMATED_MAINTENANCE_PATTERN.matcher(input);

        assertThat(matcher.find()).isFalse();
    }

    @Test
    @DisplayName("AUTOMATED_MAINTENANCE_PATTERN should not match a bare direction with no preceding indicator")
    void testAutomatedMaintenancePattern_DoesNotMatchBareDirection() {
        String input = "N ";
        Matcher matcher = RegExprConst.AUTOMATED_MAINTENANCE_PATTERN.matcher(input);

        assertThat(matcher.find()).isFalse();
    }

    // ========== PP GROUP PATTERN TESTS ==========

    @Test
    @DisplayName("PP_GROUP_PATTERN should match a typical PP group - SPJC real-world")
    void testPpGroupPattern_Typical() {
        String input = "PP000 ";
        Matcher matcher = RegExprConst.PP_GROUP_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("value")).isEqualTo("000");
    }

    @ParameterizedTest
    @ValueSource(strings = {"000", "015", "102", "999"})
    @DisplayName("PP_GROUP_PATTERN should match various three-digit values")
    void testPpGroupPattern_VariousValues(String value) {
        String input = "PP" + value + " ";
        Matcher matcher = RegExprConst.PP_GROUP_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("value")).isEqualTo(value);
    }

    @Test
    @DisplayName("PP_GROUP_PATTERN should match at end of string (no trailing space)")
    void testPpGroupPattern_AtEndOfString() {
        String input = "PP000";
        Matcher matcher = RegExprConst.PP_GROUP_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("value")).isEqualTo("000");
    }

    @ParameterizedTest
    @CsvSource({
            "'PP00 ', 'Only two digits'",
            "'PP0000 ', 'Four digits (breaks word boundary)'",
            "'P000 ', 'Missing second P'",
            "'PPABC ', 'Non-numeric value'"
    })
    @DisplayName("PP_GROUP_PATTERN should not match invalid formats")
    void testPpGroupPattern_DoesNotMatchInvalidFormats(String input, String scenario) {
        Matcher matcher = RegExprConst.PP_GROUP_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Should not match: %s", scenario)
                .isFalse();
    }

    // ========== PRECIP 3HR/24HR PATTERN TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'60025 ', 6, 0025, 'Six-hour precipitation'",
            "'70125 ', 7, 0125, 'Twenty-four-hour precipitation'"
    })
    @DisplayName("PRECIP_3HR_24HR_PATTERN should match both type codes with a numeric value")
    void testPrecip3Hr24HrPattern_TypeCodes(String input, String expectedType, String expectedPrecip, String scenario) {
        Matcher matcher = RegExprConst.PRECIP_3HR_24HR_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("type"))
                .as("Type should match: %s", scenario)
                .isEqualTo(expectedType);
        assertThat(matcher.group("precip"))
                .as("Precip value should match: %s", scenario)
                .isEqualTo(expectedPrecip);
    }

    @ParameterizedTest
    @CsvSource({
            "'60 ', 0, 'One digit'",
            "'600 ', 00, 'Two digits'",
            "'6000 ', 000, 'Three digits'",
            "'60009 ', 0009, 'Four digits'",
            "'600091 ', 00091, 'Five digits'"
    })
    @DisplayName("PRECIP_3HR_24HR_PATTERN should match precip values from 1 to 5 digits")
    void testPrecip3Hr24HrPattern_DigitLengths(String input, String expectedPrecip, String scenario) {
        Matcher matcher = RegExprConst.PRECIP_3HR_24HR_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("precip"))
                .as("Precip value should match: %s", scenario)
                .isEqualTo(expectedPrecip);
    }

    @Test
    @DisplayName("PRECIP_3HR_24HR_PATTERN should match trace precipitation with four slashes")
    void testPrecip3Hr24HrPattern_TraceFourSlashes() {
        String input = "6//// ";
        Matcher matcher = RegExprConst.PRECIP_3HR_24HR_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("6");
        assertThat(matcher.group("precip")).isEqualTo("////");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "//", "///", "////", "/////"})
    @DisplayName("PRECIP_3HR_24HR_PATTERN should match trace indicator from 1 to 5 slashes")
    void testPrecip3Hr24HrPattern_TraceSlashLengths(String slashes) {
        String input = "7" + slashes + " ";
        Matcher matcher = RegExprConst.PRECIP_3HR_24HR_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("precip")).isEqualTo(slashes);
    }

    @Test
    @DisplayName("PRECIP_3HR_24HR_PATTERN should match with no trailing whitespace consumed (\\s* allows zero)")
    void testPrecip3Hr24HrPattern_AtEndOfString() {
        String input = "60025";
        Matcher matcher = RegExprConst.PRECIP_3HR_24HR_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("precip")).isEqualTo("0025");
    }

    @Test
    @DisplayName("PRECIP_3HR_24HR_PATTERN should partially match mixed slashes and letters, consuming only the leading slash (ambiguity check)")
    void testPrecip3Hr24HrPattern_PartiallyMatchesMixedSlashesAndLetters() {
        // precip's alternation (\d{1,5}|/{1,5}) only requires ONE leading
        // digit-or-slash character to succeed - it doesn't require the whole
        // token to be homogeneous. For "6/A/B ", precip greedily matches just
        // "/" (a single slash; the following "A" breaks the slash run), and
        // \s* then matches zero-width immediately after - so the pattern DOES
        // match, consuming only "6/" and leaving "A/B " unconsumed. This
        // isn't a rejection case; it's a partial-match ambiguity worth knowing
        // about at the handler level.
        String input = "6/A/B ";
        Matcher matcher = RegExprConst.PRECIP_3HR_24HR_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("6");
        assertThat(matcher.group("precip")).isEqualTo("/");
    }

    @ParameterizedTest
    @CsvSource({
            "'80025 ', 'Invalid type code (8)'",
            "'6ABCD ', 'Non-numeric, non-slash value'",
            "'6 ', 'Missing value entirely'"
    })
    @DisplayName("PRECIP_3HR_24HR_PATTERN should not match invalid formats")
    void testPrecip3Hr24HrPattern_DoesNotMatchInvalidFormats(String input, String scenario) {
        Matcher matcher = RegExprConst.PRECIP_3HR_24HR_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Should not match: %s", scenario)
                .isFalse();
    }

    // ========== TEMP 24HR PATTERN TESTS ==========

    @Test
    @DisplayName("TEMP_24HR_PATTERN should match positive max, negative min")
    void testTemp24HrPattern_PositiveMaxNegativeMin() {
        String input = "400461006 ";
        Matcher matcher = RegExprConst.TEMP_24HR_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("4");
        assertThat(matcher.group("maxsign")).isEqualTo("0");
        assertThat(matcher.group("maxtemp")).isEqualTo("046");
        assertThat(matcher.group("minsign")).isEqualTo("1");
        assertThat(matcher.group("mintemp")).isEqualTo("006");
    }

    @ParameterizedTest
    @CsvSource({
            "'400120010 ', 0, 012, 0, 010, 'Both positive'",
            "'411231089 ', 1, 123, 1, 089, 'Both negative'",
            "'410050005 ', 1, 005, 0, 005, 'Negative max, positive min'",
            "'400000000 ', 0, 000, 0, 000, 'Both zero, both positive sign'",
            "'403501250 ', 0, 350, 1, 250, 'Large positive max, large negative min'"
    })
    @DisplayName("TEMP_24HR_PATTERN should match various sign/value combinations")
    void testTemp24HrPattern_SignCombinations(String input, String expectedMaxSign, String expectedMaxTemp,
                                              String expectedMinSign, String expectedMinTemp, String scenario) {
        Matcher matcher = RegExprConst.TEMP_24HR_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("maxsign"))
                .as("Max sign should match: %s", scenario)
                .isEqualTo(expectedMaxSign);
        assertThat(matcher.group("maxtemp"))
                .as("Max temp should match: %s", scenario)
                .isEqualTo(expectedMaxTemp);
        assertThat(matcher.group("minsign"))
                .as("Min sign should match: %s", scenario)
                .isEqualTo(expectedMinSign);
        assertThat(matcher.group("mintemp"))
                .as("Min temp should match: %s", scenario)
                .isEqualTo(expectedMinTemp);
    }

    @Test
    @DisplayName("TEMP_24HR_PATTERN should match with no trailing whitespace consumed (\\s* allows zero)")
    void testTemp24HrPattern_AtEndOfString() {
        String input = "400461006";
        Matcher matcher = RegExprConst.TEMP_24HR_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("mintemp")).isEqualTo("006");
    }

    @ParameterizedTest
    @CsvSource({
            "'500461006 ', 'Invalid type code (5)'",
            "'420461006 ', 'Invalid max sign (2)'",
            "'400462006 ', 'Invalid min sign (2)'",
            "'40046100 ', 'Incomplete - missing final digit'",
            "'4ABCDEFGH ', 'Non-numeric characters throughout'"
    })
    @DisplayName("TEMP_24HR_PATTERN should not match invalid formats")
    void testTemp24HrPattern_DoesNotMatchInvalidFormats(String input, String scenario) {
        Matcher matcher = RegExprConst.TEMP_24HR_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Should not match: %s", scenario)
                .isFalse();
    }

    // ========== DIRECTIONAL ARC AND AND-CHAIN PATTERN TESTS ==========

    @Test
    @DisplayName("TS_CLD_LOC_PATTERN should match a three-point directional arc - KDFW real-world")
    void testThunderstormCloudLocationPattern_ThreePointArc() {
        String input = "CB E-S-SW";
        Matcher matcher = RegExprConst.TS_CLD_LOC_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("CB");
        assertThat(matcher.group("dirchain")).isEqualTo("E-S-SW");
    }

    @Test
    @DisplayName("TS_CLD_LOC_PATTERN should match AND-chained single directions - KMIA real-world")
    void testThunderstormCloudLocationPattern_AndChainedSingleDirections() {
        String input = "TCU N AND SW";
        Matcher matcher = RegExprConst.TS_CLD_LOC_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("TCU");
        assertThat(matcher.group("dirchain")).isEqualTo("N AND SW");
    }

    @Test
    @DisplayName("TS_CLD_LOC_PATTERN should match AND-chained ranges - KPHX real-world")
    void testThunderstormCloudLocationPattern_AndChainedRanges() {
        String input = "CB DSNT N-E AND SE-S";
        Matcher matcher = RegExprConst.TS_CLD_LOC_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("type")).isEqualTo("CB");
        assertThat(matcher.group("loc")).isEqualTo("DSNT");
        assertThat(matcher.group("dirchain")).isEqualTo("N-E AND SE-S");
    }

    @Test
    @DisplayName("TS_CLD_LOC_PATTERN should match a two-point directional range (regression check)")
    void testThunderstormCloudLocationPattern_TwoPointRange() {
        String input = "TCU DSNT N-NE";
        Matcher matcher = RegExprConst.TS_CLD_LOC_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("dirchain")).isEqualTo("N-NE");
    }

    @Test
    @DisplayName("TS_CLD_LOC_PATTERN should match dirchain with movement following an AND-chain")
    void testThunderstormCloudLocationPattern_AndChainWithMovement() {
        String input = "CB N AND SW MOV E";
        Matcher matcher = RegExprConst.TS_CLD_LOC_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("dirchain")).isEqualTo("N AND SW");
        assertThat(matcher.group("dirm")).isEqualTo("E");
    }

    // ========== LAST OBSERVATION / NEXT OBSERVATION PATTERN TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'LAST STFD OBS/NEXT 261200Z ', 26, 12, 00, 'CYZG real-world'",
            "'LAST STFD OBS / NEXT 271200 UTC ', 27, 12, 00, 'CYKG real-world, spaced slash and UTC'",
            "'LAST STFD OBS/NEXT 101300Z ', 10, 13, 00, 'MANOBS example'"
    })
    @DisplayName("LAST_OBS_PATTERN should match staffed status across format variants")
    void testLastObsPattern_StaffedVariants(String input, String expectedDay, String expectedHour,
                                            String expectedMinute, String scenario) {
        Matcher matcher = RegExprConst.LAST_OBS_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should match: %s", scenario)
                .isTrue();
        assertThat(matcher.group("stfd"))
                .as("Should be staffed: %s", scenario)
                .isNotNull();
        assertThat(matcher.group("day"))
                .as("Day should match: %s", scenario)
                .isEqualTo(expectedDay);
        assertThat(matcher.group("hour"))
                .as("Hour should match: %s", scenario)
                .isEqualTo(expectedHour);
        assertThat(matcher.group("minute"))
                .as("Minute should match: %s", scenario)
                .isEqualTo(expectedMinute);
    }

    @Test
    @DisplayName("LAST_OBS_PATTERN should match non-staffed status with fused UTC suffix - MANOBS example")
    void testLastObsPattern_NotStaffedFusedUtc() {
        String input = "LAST OBS/NEXT 101300UTC ";
        Matcher matcher = RegExprConst.LAST_OBS_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("stfd")).isNull();
        assertThat(matcher.group("day")).isEqualTo("10");
        assertThat(matcher.group("hour")).isEqualTo("13");
        assertThat(matcher.group("minute")).isEqualTo("00");
    }

    @Test
    @DisplayName("LAST_OBS_PATTERN should match at end of string (no trailing space)")
    void testLastObsPattern_AtEndOfString() {
        String input = "LAST STFD OBS/NEXT 261200Z";
        Matcher matcher = RegExprConst.LAST_OBS_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("minute")).isEqualTo("00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Z", "UTC"})
    @DisplayName("LAST_OBS_PATTERN should match both fused time-zone suffixes")
    void testLastObsPattern_BothFusedSuffixes(String suffix) {
        String input = "LAST OBS/NEXT 101300" + suffix + " ";
        Matcher matcher = RegExprConst.LAST_OBS_PATTERN.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("day")).isEqualTo("10");
    }

    @ParameterizedTest
    @CsvSource({
            "'LAST 261200Z ', 'Missing OBS/NEXT'",
            "'LAST STFD OBS/NEXT 261200 ', 'Missing time-zone suffix'",
            "'LAST STFD OBS/NEXT 26120Z ', 'Only five digits'"
    })
    @DisplayName("LAST_OBS_PATTERN should not match invalid formats")
    void testLastObsPattern_DoesNotMatchInvalidFormats(String input, String scenario) {
        Matcher matcher = RegExprConst.LAST_OBS_PATTERN.matcher(input);

        assertThat(matcher.find())
                .as("Should not match: %s", scenario)
                .isFalse();
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
