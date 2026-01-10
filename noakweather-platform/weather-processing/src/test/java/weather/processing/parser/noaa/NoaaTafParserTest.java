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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import weather.model.NoaaTafData;
import weather.model.NoaaWeatherData;
import weather.model.components.*;
import weather.model.enums.ChangeIndicator;
import weather.model.enums.SkyCoverage;
import weather.processing.parser.common.ParseResult;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for NoaaTafParser.
 *
 * Tests TAF-specific parsing functionality including:
 * - TAF header parsing (type, modifier, station, issue time, validity)
 * - Base forecast parsing
 * - Change group parsing (FM, TEMPO, BECMG, PROB)
 * - Temperature forecasts (TX/TN)
 * - Weather conditions inherited from base class
 * - Real-world TAF examples
 *
 * @author bclasky1539
 *
 */
class NoaaTafParserTest {

    private NoaaTafParser parser;

    @BeforeEach
    void setUp() {
        parser = new NoaaTafParser();
    }

    // ==================== SOURCE TYPE & CAN PARSE ====================

    @Nested
    @DisplayName("Source Type and Can Parse Tests")
    class SourceTypeAndCanParseTests {

        @Test
        @DisplayName("Should return correct source type")
        void testGetSourceType() {
            assertThat(parser.getSourceType()).isEqualTo("NOAA_TAF");
        }

        @ParameterizedTest
        @CsvSource({
                "'TAF KJFK 151800Z 1518/1624 18010KT P6SM FEW250', 'standard TAF'",
                "'2025/12/15 20:57 TAF AMD KCLT 151953Z 1520/1624 VRB02KT P6SM FEW250', 'TAF with date prefix'",
                "'TAF AMD KJFK 151800Z 1518/1624 18010KT P6SM FEW250', 'TAF with AMD modifier'",
                "'TAF COR KJFK 151800Z 1518/1624 18010KT P6SM FEW250', 'TAF with COR modifier'"
        })
        @DisplayName("Should correctly identify valid TAF formats")
        void testCanParse_ValidTAFs(String taf, String description) {
            assertThat(parser.canParse(taf))
                    .as("Should parse TAF: %s", description)
                    .isTrue();
        }

        @Test
        @DisplayName("Should not parse null input")
        void testCanParse_Null() {
            assertThat(parser.canParse(null)).isFalse();
        }

        @Test
        @DisplayName("Should not parse empty string")
        void testCanParse_Empty() {
            assertThat(parser.canParse("")).isFalse();
            assertThat(parser.canParse("   ")).isFalse();
        }

        @Test
        @DisplayName("Should not parse METAR")
        void testCanParse_METAR() {
            String metar = "METAR KJFK 151856Z 18010KT 10SM FEW250 22/14 A3012";
            assertThat(parser.canParse(metar)).isFalse();
        }

        @Test
        @DisplayName("Should not parse invalid format")
        void testCanParse_Invalid() {
            assertThat(parser.canParse("INVALID DATA")).isFalse();
            assertThat(parser.canParse("SPECI KJFK 151856Z")).isFalse();
        }
    }

    // ==================== BASIC PARSING ====================

    @Nested
    @DisplayName("Basic TAF Header Parsing Tests")
    class BasicHeaderParsingTests {

        @Test
        @DisplayName("Should parse minimal TAF")
        void testParseMinimalTAF() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();
            assertThat(data.getStationId()).isEqualTo("KJFK");
            assertThat(data.getReportType()).isEqualTo("TAF");
            assertThat(data.getValidityPeriod()).isNotNull();
        }

        @Test
        @DisplayName("Should parse TAF with AMD modifier")
        void testParseWithAMD() {
            String taf = "TAF AMD KJFK 151800Z 1518/1624 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();
            assertThat(data.getReportModifier()).isEqualTo("AMD");
        }

        @Test
        @DisplayName("Should parse TAF with COR modifier")
        void testParseWithCOR() {
            String taf = "TAF COR KJFK 151800Z 1518/1624 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();
            assertThat(data.getReportModifier()).isEqualTo("COR");
        }

        @Test
        @DisplayName("Should parse TAF with external issue date/time")
        void testParseWithExternalIssueDateTime() {
            String taf = "2025/12/15 20:57 TAF KCLT 151953Z 1520/1624 VRB02KT P6SM FEW250";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();
            assertThat(data.getIssueTime()).isNotNull();

            LocalDateTime issueDateTime = LocalDateTime.ofInstant(data.getIssueTime(), ZoneOffset.UTC);
            assertThat(issueDateTime.getYear()).isEqualTo(2025);
            assertThat(issueDateTime.getMonthValue()).isEqualTo(12);
            assertThat(issueDateTime.getDayOfMonth()).isEqualTo(15);
            assertThat(issueDateTime.getHour()).isEqualTo(19);
            assertThat(issueDateTime.getMinute()).isEqualTo(53);
        }

        @Test
        @DisplayName("Should parse station ID")
        void testParseStationId() {
            String taf = "TAF KLGA 151800Z 1518/1624 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();
            assertThat(data.getStationId()).isEqualTo("KLGA");
        }

        @ParameterizedTest
        @CsvSource({
                ", 'null or empty', 'null input'",
                "'', 'null or empty', 'empty string'",
                "'INVALID DATA', 'not a valid TAF', 'invalid data'"
        })
        @DisplayName("Should handle invalid TAF inputs")
        void testParseInvalidInputs(String input, String expectedErrorFragment, String description) {
            ParseResult<NoaaWeatherData> result = parser.parse(input);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains(expectedErrorFragment);
        }

        @Test
        @DisplayName("Should set raw text")
        void testRawTextSet() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();
            assertThat(data.getRawText()).isEqualTo(taf);
        }
    }

    // ==================== ISSUE TIME PARSING ====================

    @Nested
    @DisplayName("Issue Time Parsing Tests")
    class IssueTimeParsingTests {

        @Test
        @DisplayName("Should parse issue time with minutes")
        void testParseIssueTime() {
            String taf = "TAF KJFK 151856Z 1518/1624 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            LocalDateTime issueDateTime = LocalDateTime.ofInstant(data.getIssueTime(), ZoneOffset.UTC);
            assertThat(issueDateTime.getDayOfMonth()).isEqualTo(15);
            assertThat(issueDateTime.getHour()).isEqualTo(18);
            assertThat(issueDateTime.getMinute()).isEqualTo(56);
        }

        @Test
        @DisplayName("Should parse issue time at midnight")
        void testParseIssueTimeAtMidnight() {
            String taf = "TAF KJFK 150000Z 1500/1606 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            LocalDateTime issueDateTime = LocalDateTime.ofInstant(data.getIssueTime(), ZoneOffset.UTC);
            assertThat(issueDateTime.getHour()).isZero();
            assertThat(issueDateTime.getMinute()).isZero();
        }

        @Test
        @DisplayName("Should handle month wrap-around in issue time")
        void testParseIssueTimeMonthWrapAround() {
            // If today is Jan 2nd and TAF issued on 31st, it's from previous month
            String taf = "2026/01/02 12:00 TAF KJFK 311800Z 3118/0124 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            LocalDateTime issueDateTime = LocalDateTime.ofInstant(data.getIssueTime(), ZoneOffset.UTC);
            assertThat(issueDateTime.getMonthValue()).isEqualTo(12);  // December
            assertThat(issueDateTime.getDayOfMonth()).isEqualTo(31);
        }
    }

    // ==================== VALIDITY PERIOD PARSING ====================

    @Nested
    @DisplayName("Validity Period Parsing Tests")
    class ValidityPeriodParsingTests {

        @Test
        @DisplayName("Should parse standard 24-hour validity period")
        void testParseValidityPeriod24Hour() {
            String taf = "TAF KJFK 151800Z 1518/1618 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ValidityPeriod validity = data.getValidityPeriod();
            assertThat(validity).isNotNull();

            LocalDateTime validFrom = LocalDateTime.ofInstant(validity.validFrom(), ZoneOffset.UTC);
            LocalDateTime validTo = LocalDateTime.ofInstant(validity.validTo(), ZoneOffset.UTC);

            assertThat(validFrom.getDayOfMonth()).isEqualTo(15);
            assertThat(validFrom.getHour()).isEqualTo(18);
            assertThat(validTo.getDayOfMonth()).isEqualTo(16);
            assertThat(validTo.getHour()).isEqualTo(18);
        }

        @Test
        @DisplayName("Should parse validity period ending at 24Z")
        void testParseValidityPeriodEndingAt24Z() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ValidityPeriod validity = data.getValidityPeriod();
            LocalDateTime validTo = LocalDateTime.ofInstant(validity.validTo(), ZoneOffset.UTC);

            // 1624 means 16th at 2400Z = 17th at 0000Z
            assertThat(validTo.getDayOfMonth()).isEqualTo(17);
            assertThat(validTo.getHour()).isZero();
        }

        @Test
        @DisplayName("Should parse 30-hour validity period")
        void testParseValidityPeriod30Hour() {
            String taf = "TAF KJFK 151200Z 1512/1618 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ValidityPeriod validity = data.getValidityPeriod();
            assertThat(validity.getDurationHours()).isEqualTo(30);
        }

        @Test
        @DisplayName("Should parse validity period crossing month boundary")
        void testParseValidityPeriodMonthWrapAround() {
            String taf = "2025/12/31 18:00 TAF KJFK 311800Z 3118/0118 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ValidityPeriod validity = data.getValidityPeriod();
            LocalDateTime validFrom = LocalDateTime.ofInstant(validity.validFrom(), ZoneOffset.UTC);
            LocalDateTime validTo = LocalDateTime.ofInstant(validity.validTo(), ZoneOffset.UTC);

            assertThat(validFrom.getMonthValue()).isEqualTo(12);
            assertThat(validFrom.getDayOfMonth()).isEqualTo(31);
            assertThat(validTo.getMonthValue()).isEqualTo(1);
            assertThat(validTo.getDayOfMonth()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should fail if validity period is missing")
        void testMissingValidityPeriod() {
            String taf = "TAF KJFK 151800Z 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("validity period");
        }
    }

    // ==================== BASE FORECAST PARSING ====================

    @Nested
    @DisplayName("Base Forecast Parsing Tests")
    class BaseForecastParsingTests {

        @Test
        @DisplayName("Should parse base forecast with wind")
        void testParseBaseForecastWithWind() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            List<ForecastPeriod> periods = data.getForecastPeriods();
            assertThat(periods).hasSize(1);

            ForecastPeriod base = periods.get(0);
            assertThat(base.changeIndicator()).isEqualTo(ChangeIndicator.BASE);
            assertThat(base.conditions().wind()).isNotNull();
            assertThat(base.conditions().wind().directionDegrees()).isEqualTo(180);
            assertThat(base.conditions().wind().getSpeedKnots()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should parse base forecast with visibility")
        void testParseBaseForecastWithVisibility() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT 3SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().visibility()).isNotNull();
            assertThat(base.conditions().visibility().toStatuteMiles()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("Should parse base forecast with present weather")
        void testParseBaseForecastWithPresentWeather() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT 5SM -RA BR SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().presentWeather()).hasSize(2);
        }

        @Test
        @DisplayName("Should parse base forecast with sky conditions")
        void testParseBaseForecastWithSkyConditions() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM FEW050 SCT100 BKN200";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().skyConditions()).hasSize(3);
            assertThat(base.conditions().skyConditions().get(0).coverage()).isEqualTo(SkyCoverage.FEW);
            assertThat(base.conditions().skyConditions().get(0).heightFeet()).isEqualTo(5000);
        }

        @Test
        @DisplayName("Should parse base forecast with CAVOK")
        void testParseBaseForecastWithCAVOK() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT CAVOK";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().visibility()).isNotNull();
            assertThat(base.conditions().visibility().isCavok()).isTrue();
        }

        @Test
        @DisplayName("Should parse base forecast with variable wind")
        void testParseBaseForecastWithVariableWind() {
            String taf = "TAF KJFK 151800Z 1518/1624 VRB05KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().wind()).isNotNull();
            assertThat(base.conditions().wind().hasVariableDirection()).isTrue();
        }

        @Test
        @DisplayName("Should parse base forecast with calm wind")
        void testParseBaseForecastWithCalmWind() {
            String taf = "TAF KJFK 151800Z 1518/1624 00000KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().wind()).isNotNull();
            assertThat(base.conditions().wind().isCalm()).isTrue();
        }
    }

    // ==================== FM (FROM) CHANGE GROUP PARSING ====================

    @Nested
    @DisplayName("FM Change Group Parsing Tests")
    class FMChangeGroupTests {

        @Test
        @DisplayName("Should parse single FM group")
        void testParseSingleFM() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "FM152100 21015KT P6SM FEW100";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            List<ForecastPeriod> periods = data.getForecastPeriods();
            assertThat(periods).hasSize(2);

            ForecastPeriod fm = periods.get(1);
            assertThat(fm.changeIndicator()).isEqualTo(ChangeIndicator.FM);
            assertThat(fm.changeTime()).isNotNull();

            LocalDateTime changeTime = LocalDateTime.ofInstant(fm.changeTime(), ZoneOffset.UTC);
            assertThat(changeTime.getDayOfMonth()).isEqualTo(15);
            assertThat(changeTime.getHour()).isEqualTo(21);
            assertThat(changeTime.getMinute()).isZero();
        }

        @Test
        @DisplayName("Should parse multiple FM groups")
        void testParseMultipleFM() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "FM152100 21015KT P6SM FEW100 " +
                    "FM160300 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            List<ForecastPeriod> periods = data.getForecastPeriods();
            assertThat(periods).hasSize(3);
            assertThat(periods.get(0).changeIndicator()).isEqualTo(ChangeIndicator.BASE);
            assertThat(periods.get(1).changeIndicator()).isEqualTo(ChangeIndicator.FM);
            assertThat(periods.get(2).changeIndicator()).isEqualTo(ChangeIndicator.FM);
        }

        @Test
        @DisplayName("Should parse FM with complete weather conditions")
        void testParseFMWithCompleteConditions() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "FM160000 27020G30KT 3SM -SHRA BKN015CB";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod fm = data.getForecastPeriods().get(1);
            assertThat(fm.conditions().wind().directionDegrees()).isEqualTo(270);
            assertThat(fm.conditions().wind().getSpeedKnots()).isEqualTo(20);
            assertThat(fm.conditions().wind().gustValue()).isEqualTo(30);
            assertThat(fm.conditions().visibility().toStatuteMiles()).isEqualTo(3.0);
            assertThat(fm.conditions().presentWeather()).hasSize(1);
            assertThat(fm.conditions().skyConditions()).hasSize(1);
        }

        @Test
        @DisplayName("Should parse FM at minute precision")
        void testParseFMWithMinutes() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "FM152145 21015KT P6SM FEW100";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod fm = data.getForecastPeriods().get(1);
            LocalDateTime changeTime = LocalDateTime.ofInstant(fm.changeTime(), ZoneOffset.UTC);
            assertThat(changeTime.getMinute()).isEqualTo(45);
        }
    }

    // ==================== TEMPO CHANGE GROUP PARSING ====================

    @Nested
    @DisplayName("TEMPO Change Group Parsing Tests")
    class TEMPOChangeGroupTests {

        @Test
        @DisplayName("Should parse single TEMPO group")
        void testParseSingleTEMPO() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "TEMPO 1520/1523 5SM -RA BKN020";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            List<ForecastPeriod> periods = data.getForecastPeriods();
            assertThat(periods).hasSize(2);

            ForecastPeriod tempo = periods.get(1);
            assertThat(tempo.changeIndicator()).isEqualTo(ChangeIndicator.TEMPO);
            assertThat(tempo.periodStart()).isNotNull();
            assertThat(tempo.periodEnd()).isNotNull();
        }

        @Test
        @DisplayName("Should parse TEMPO with time period")
        void testParseTEMPOTimePeriod() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "TEMPO 1520/1523 5SM -RA BKN020";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod tempo = data.getForecastPeriods().get(1);

            LocalDateTime start = LocalDateTime.ofInstant(tempo.periodStart(), ZoneOffset.UTC);
            LocalDateTime end = LocalDateTime.ofInstant(tempo.periodEnd(), ZoneOffset.UTC);

            assertThat(start.getDayOfMonth()).isEqualTo(15);
            assertThat(start.getHour()).isEqualTo(20);
            assertThat(end.getDayOfMonth()).isEqualTo(15);
            assertThat(end.getHour()).isEqualTo(23);
        }

        @Test
        @DisplayName("Should parse multiple TEMPO groups")
        void testParseMultipleTEMPO() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "TEMPO 1520/1523 5SM -RA BKN020 " +
                    "TEMPO 1600/1606 3SM -SN OVC015";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            List<ForecastPeriod> periods = data.getForecastPeriods();
            assertThat(periods).hasSize(3);
            assertThat(periods.get(1).changeIndicator()).isEqualTo(ChangeIndicator.TEMPO);
            assertThat(periods.get(2).changeIndicator()).isEqualTo(ChangeIndicator.TEMPO);
        }

        @Test
        @DisplayName("Should parse TEMPO with complete conditions")
        void testParseTEMPOWithCompleteConditions() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "TEMPO 1520/1523 27025G35KT 1SM +TSRA OVC010CB";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod tempo = data.getForecastPeriods().get(1);
            assertThat(tempo.conditions().wind().getSpeedKnots()).isEqualTo(25);
            assertThat(tempo.conditions().wind().gustValue()).isEqualTo(35);
            assertThat(tempo.conditions().visibility().toStatuteMiles()).isEqualTo(1.0);
            assertThat(tempo.conditions().presentWeather()).isNotEmpty();
            assertThat(tempo.conditions().skyConditions()).isNotEmpty();
        }
    }

    // ==================== BECMG CHANGE GROUP PARSING ====================

    @Nested
    @DisplayName("BECMG Change Group Parsing Tests")
    class BECMGChangeGroupTests {

        @Test
        @DisplayName("Should parse single BECMG group")
        void testParseSingleBECMG() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "BECMG 1520/1522 21015KT";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            List<ForecastPeriod> periods = data.getForecastPeriods();
            assertThat(periods).hasSize(2);

            ForecastPeriod becmg = periods.get(1);
            assertThat(becmg.changeIndicator()).isEqualTo(ChangeIndicator.BECMG);
            assertThat(becmg.periodStart()).isNotNull();
            assertThat(becmg.periodEnd()).isNotNull();
        }

        @Test
        @DisplayName("Should parse BECMG with time period")
        void testParseBECMGTimePeriod() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "BECMG 1521/1523 21015KT";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod becmg = data.getForecastPeriods().get(1);

            LocalDateTime start = LocalDateTime.ofInstant(becmg.periodStart(), ZoneOffset.UTC);
            LocalDateTime end = LocalDateTime.ofInstant(becmg.periodEnd(), ZoneOffset.UTC);

            assertThat(start.getHour()).isEqualTo(21);
            assertThat(end.getHour()).isEqualTo(23);
        }

        @Test
        @DisplayName("Should parse BECMG with visibility change")
        void testParseBECMGWithVisibility() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "BECMG 1520/1522 3SM BR";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod becmg = data.getForecastPeriods().get(1);
            assertThat(becmg.conditions().visibility().toStatuteMiles()).isEqualTo(3.0);
            assertThat(becmg.conditions().presentWeather()).hasSize(1);
        }
    }

    // ==================== PROB CHANGE GROUP PARSING ====================

    @Nested
    @DisplayName("PROB Change Group Parsing Tests")
    class PROBChangeGroupTests {

        @Test
        @DisplayName("Should parse PROB30 group")
        void testParsePROB30() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "PROB30 1520/1523 3SM -SHRA BKN020";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            List<ForecastPeriod> periods = data.getForecastPeriods();
            assertThat(periods).hasSize(2);

            ForecastPeriod prob = periods.get(1);
            assertThat(prob.changeIndicator()).isEqualTo(ChangeIndicator.PROB);
            assertThat(prob.probability()).isEqualTo(30);
        }

        @Test
        @DisplayName("Should parse PROB40 group")
        void testParsePROB40() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "PROB40 1520/1523 2SM +TSRA BKN015CB";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod prob = data.getForecastPeriods().get(1);
            assertThat(prob.probability()).isEqualTo(40);
        }

        @Test
        @DisplayName("Should parse PROB with time period")
        void testParsePROBTimePeriod() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "PROB30 1520/1602 1SM +SN OVC010";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod prob = data.getForecastPeriods().get(1);

            LocalDateTime start = LocalDateTime.ofInstant(prob.periodStart(), ZoneOffset.UTC);
            LocalDateTime end = LocalDateTime.ofInstant(prob.periodEnd(), ZoneOffset.UTC);

            assertThat(start.getDayOfMonth()).isEqualTo(15);
            assertThat(start.getHour()).isEqualTo(20);
            assertThat(end.getDayOfMonth()).isEqualTo(16);
            assertThat(end.getHour()).isEqualTo(2);
        }
    }

    // ==================== MIXED CHANGE GROUPS ====================

    @Nested
    @DisplayName("Mixed Change Group Parsing Tests")
    class MixedChangeGroupTests {

        @Test
        @DisplayName("Should parse TAF with FM and TEMPO")
        void testParseFMAndTEMPO() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "FM152100 21015KT P6SM FEW100 " +
                    "TEMPO 1523/1602 5SM -RA BKN020";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            List<ForecastPeriod> periods = data.getForecastPeriods();
            assertThat(periods).hasSize(3);
            assertThat(periods.get(0).changeIndicator()).isEqualTo(ChangeIndicator.BASE);
            assertThat(periods.get(1).changeIndicator()).isEqualTo(ChangeIndicator.FM);
            assertThat(periods.get(2).changeIndicator()).isEqualTo(ChangeIndicator.TEMPO);
        }

        @Test
        @DisplayName("Should parse TAF with all change group types")
        void testParseAllChangeGroupTypes() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "FM152100 21015KT P6SM FEW100 " +
                    "TEMPO 1523/1602 5SM -RA BKN020 " +
                    "BECMG 1603/1605 27020KT " +
                    "PROB30 1608/1612 2SM +TSRA OVC015CB";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            List<ForecastPeriod> periods = data.getForecastPeriods();
            assertThat(periods).hasSize(5);
            assertThat(periods.get(0).changeIndicator()).isEqualTo(ChangeIndicator.BASE);
            assertThat(periods.get(1).changeIndicator()).isEqualTo(ChangeIndicator.FM);
            assertThat(periods.get(2).changeIndicator()).isEqualTo(ChangeIndicator.TEMPO);
            assertThat(periods.get(3).changeIndicator()).isEqualTo(ChangeIndicator.BECMG);
            assertThat(periods.get(4).changeIndicator()).isEqualTo(ChangeIndicator.PROB);
        }

        @Test
        @DisplayName("Should parse TAF with multiple FM groups and TEMPO")
        void testParseMultipleFMWithTEMPO() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "FM152100 21015KT P6SM FEW100 " +
                    "FM160000 27020KT P6SM SCT080 " +
                    "TEMPO 1603/1606 5SM -SHRA BKN030 " +
                    "FM160900 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            assertThat(data.getForecastPeriods()).hasSize(5);
        }
    }

    // ==================== TEMPERATURE FORECAST PARSING ====================

    @Nested
    @DisplayName("Temperature Forecast Parsing Tests")
    class TemperatureForecastTests {

        @Test
        @DisplayName("Should parse maximum temperature forecast")
        void testParseMaxTemperature() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "TX28/1521Z";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            assertThat(data.getMaxTemperature()).isEqualTo(28);
            assertThat(data.getMaxTemperatureTime()).isNotNull();
        }

        @Test
        @DisplayName("Should parse minimum temperature forecast")
        void testParseMinTemperature() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "TN12/1612Z";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            assertThat(data.getMinTemperature()).isEqualTo(12);
            assertThat(data.getMinTemperatureTime()).isNotNull();
        }

        @Test
        @DisplayName("Should parse both max and min temperature forecasts")
        void testParseMaxAndMinTemperatures() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "TX28/1521Z TN12/1612Z";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            assertThat(data.getMaxTemperature()).isEqualTo(28);
            assertThat(data.getMinTemperature()).isEqualTo(12);
        }

        @Test
        @DisplayName("Should parse negative temperature forecast")
        void testParseNegativeTemperature() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "TNM05/1612Z";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            assertThat(data.getMinTemperature()).isEqualTo(-5);
        }

        @Test
        @DisplayName("Should parse temperature forecast with time")
        void testParseTemperatureForecastTime() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "TX28/1521Z";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            LocalDateTime forecastTime = LocalDateTime.ofInstant(
                    data.getMaxTemperatureTime(), ZoneOffset.UTC);
            assertThat(forecastTime.getDayOfMonth()).isEqualTo(15);
            assertThat(forecastTime.getHour()).isEqualTo(21);
        }
    }

    // ==================== REAL-WORLD TAF EXAMPLES ====================

    @Nested
    @DisplayName("Real-World TAF Examples")
    class RealWorldExamples {

        @Test
        @DisplayName("Should parse real TAF from KJFK")
        void testRealTAF_KJFK() {
            String taf = "TAF KJFK 151730Z 1518/1624 18012KT P6SM FEW250 " +
                    "FM152100 21015KT P6SM FEW250 " +
                    "FM160300 24012KT P6SM SCT250";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();
            assertThat(data.getStationId()).isEqualTo("KJFK");
            assertThat(data.getForecastPeriods()).hasSize(3);
        }

        @Test
        @DisplayName("Should parse real TAF from KCLT with AMD")
        void testRealTAF_KCLT_AMD() {
            String taf = "2025/12/15 20:57 " +
                    "TAF AMD KCLT 151953Z 1520/1624 VRB02KT P6SM FEW250 " +
                    "FM152100 21005KT P6SM SCT250 " +
                    "FM160400 20007KT P6SM BKN250";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();
            assertThat(data.getStationId()).isEqualTo("KCLT");
            assertThat(data.getReportModifier()).isEqualTo("AMD");
            assertThat(data.getForecastPeriods()).hasSize(3);
        }

        @Test
        @DisplayName("Should parse complex TAF with TEMPO and weather")
        void testComplexTAF_WithTEMPO() {
            String taf = "TAF KLGA 151800Z 1518/1624 27015G25KT P6SM SCT050 BKN100 " +
                    "FM152200 28018G28KT P6SM BKN040 " +
                    "TEMPO 1522/1602 5SM -SHRA BKN030 " +
                    "FM160200 30012KT P6SM BKN050";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();
            assertThat(data.getForecastPeriods()).hasSize(4);

            // Verify TEMPO period has precipitation
            ForecastPeriod tempo = data.getForecastPeriods().get(2);
            assertThat(tempo.changeIndicator()).isEqualTo(ChangeIndicator.TEMPO);
            assertThat(tempo.conditions().presentWeather()).isNotEmpty();
        }

        @Test
        @DisplayName("Should parse TAF with PROB and thunderstorms")
        void testTAF_WithPROBAndThunderstorms() {
            String taf = "TAF KATL 151730Z 1518/1624 20012KT P6SM SCT040 BKN100 " +
                    "FM152100 22015G25KT P6SM SCT030 BKN080 " +
                    "PROB30 1521/1524 3SM TSRA BKN025CB " +
                    "FM160000 25012KT P6SM FEW040 SCT250";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();
            assertThat(data.getForecastPeriods()).hasSize(4);

            // Verify PROB period
            ForecastPeriod prob = data.getForecastPeriods().get(2);
            assertThat(prob.changeIndicator()).isEqualTo(ChangeIndicator.PROB);
            assertThat(prob.probability()).isEqualTo(30);
            assertThat(prob.conditions().presentWeather()).isNotEmpty();
        }

        @Test
        @DisplayName("Should parse TAF with temperature forecasts")
        void testTAF_WithTemperatureForecasts() {
            String taf = "TAF KORD 151730Z 1518/1624 27012KT P6SM SCT100 " +
                    "TX24/1521Z TN08/1612Z " +
                    "FM152100 28015KT P6SM FEW100";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();
            assertThat(data.getMaxTemperature()).isEqualTo(24);
            assertThat(data.getMinTemperature()).isEqualTo(8);
        }

        @Test
        @DisplayName("Should parse TAF with BECMG and visibility reduction")
        void testTAF_WithBECMGAndVisibility() {
            String taf = "TAF KSFO 151730Z 1518/1624 29012KT P6SM FEW015 FEW200 " +
                    "FM152300 30015KT P6SM FEW020 " +
                    "BECMG 1603/1605 4SM BR " +
                    "FM160900 VRB03KT 1SM BR OVC005";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();
            assertThat(data.getForecastPeriods()).hasSize(4);

            // Verify BECMG has reduced visibility
            ForecastPeriod becmg = data.getForecastPeriods().get(2);
            assertThat(becmg.changeIndicator()).isEqualTo(ChangeIndicator.BECMG);
            assertThat(becmg.conditions().visibility().toStatuteMiles()).isEqualTo(4.0);
        }

        @Test
        @DisplayName("Should parse international TAF with metric visibility")
        void testInternationalTAF_MetricVisibility() {
            String taf = "TAF EGLL 151700Z 1518/1624 27015KT 9999 SCT030 " +
                    "TEMPO 1518/1522 5000 -RA BKN015 " +
                    "FM160000 30012KT 9999 FEW020";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();
            assertThat(data.getStationId()).isEqualTo("EGLL");

            // Base forecast has 10km+ visibility
            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().visibility().toMeters()).isEqualTo(9999);

            // TEMPO has 5km visibility
            ForecastPeriod tempo = data.getForecastPeriods().get(1);
            assertThat(tempo.conditions().visibility().toMeters()).isEqualTo(5000);
        }

        @Test
        @DisplayName("Should parse TAF with multiple weather phenomena")
        void testTAF_MultipleWeatherPhenomena() {
            String taf = "TAF KDEN 151730Z 1518/1624 35015G25KT P6SM FEW100 " +
                    "FM160000 02020G30KT 3SM -SN BLSN BKN030 OVC050 " +
                    "TEMPO 1600/1606 1SM +SN VV010";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            // FM period has multiple weather phenomena
            ForecastPeriod fm = data.getForecastPeriods().get(1);
            assertThat(fm.conditions().presentWeather()).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    // ==================== WIND PARSING (INHERITED) ====================

    @Nested
    @DisplayName("Wind Parsing Tests (Inherited from Base)")
    class WindParsingTests {

        @Test
        @DisplayName("Should parse wind with gusts")
        void testParseWindWithGusts() {
            String taf = "TAF KJFK 151800Z 1518/1624 27020G35KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().wind().directionDegrees()).isEqualTo(270);
            assertThat(base.conditions().wind().getSpeedKnots()).isEqualTo(20);
            assertThat(base.conditions().wind().gustValue()).isEqualTo(35);
        }

        @Test
        @DisplayName("Should parse wind in MPS")
        void testParseWindMPS() {
            String taf = "TAF KJFK 151800Z 1518/1624 27010MPS P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().wind().getSpeedKnots()).isGreaterThan(10);
        }
    }

    // ==================== VISIBILITY PARSING (INHERITED) ====================

    @Nested
    @DisplayName("Visibility Parsing Tests (Inherited from Base)")
    class VisibilityParsingTests {

        @Test
        @DisplayName("Should parse fractional visibility")
        void testParseFractionalVisibility() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT 1/2SM FG OVC002";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().visibility().toStatuteMiles()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should parse mixed number visibility")
        void testParseMixedNumberVisibility() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT 1 1/2SM BR BKN010";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().visibility().toStatuteMiles()).isEqualTo(1.5);
        }

        @Test
        @DisplayName("Should parse metric visibility 9999")
        void testParseMetricVisibility9999() {
            String taf = "TAF EGLL 151700Z 1518/1624 27015KT 9999 FEW020";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().visibility().toMeters()).isEqualTo(9999);
        }
    }

    // ==================== PRESENT WEATHER PARSING (INHERITED) ====================

    @Nested
    @DisplayName("Present Weather Parsing Tests (Inherited from Base)")
    class PresentWeatherParsingTests {

        @Test
        @DisplayName("Should parse rain with intensity")
        void testParseRainWithIntensity() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT 3SM -RA BKN020";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().presentWeather()).hasSize(1);
        }

        @Test
        @DisplayName("Should parse thunderstorm with rain")
        void testParseThunderstorm() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT 2SM +TSRA OVC015CB";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().presentWeather()).hasSize(1);
        }

        @Test
        @DisplayName("Should parse multiple weather phenomena")
        void testParseMultipleWeather() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT 1SM -SN BLSN BKN020";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().presentWeather()).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    // ==================== SKY CONDITION PARSING (INHERITED) ====================

    @Nested
    @DisplayName("Sky Condition Parsing Tests (Inherited from Base)")
    class SkyConditionParsingTests {

        @Test
        @DisplayName("Should parse multiple cloud layers")
        void testParseMultipleLayers() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM FEW050 SCT100 BKN200";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().skyConditions()).hasSize(3);
        }

        @Test
        @DisplayName("Should parse clouds with cumulonimbus")
        void testParseCumulonimbus() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT 2SM +TSRA OVC015CB";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().skyConditions()).hasSize(1);
            assertThat(base.conditions().skyConditions().get(0).cloudType()).isEqualTo("CB");
        }

        @Test
        @DisplayName("Should parse vertical visibility")
        void testParseVerticalVisibility() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT 1/4SM +SN VV005";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().skyConditions()).hasSize(1);
            assertThat(base.conditions().skyConditions().get(0).coverage()).isEqualTo(SkyCoverage.VERTICAL_VISIBILITY);
        }

        @Test
        @DisplayName("Should parse SKC")
        void testParseSKC() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();

            ForecastPeriod base = data.getForecastPeriods().get(0);
            assertThat(base.conditions().skyConditions()).hasSize(1);
            assertThat(base.conditions().skyConditions().get(0).coverage()).isEqualTo(SkyCoverage.SKC);
        }
    }

    // ==================== ERROR HANDLING ====================

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle missing validity period gracefully")
        void testMissingValidityPeriod() {
            String taf = "TAF KJFK 151800Z 18010KT P6SM SKC";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).containsIgnoringCase("validity");
        }

        @Test
        @DisplayName("Should handle malformed change group gracefully")
        void testMalformedChangeGroup() {
            String taf = "TAF KJFK 151800Z 1518/1624 18010KT P6SM SKC " +
                    "FMXXX 21015KT P6SM FEW100";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            // Should still parse base forecast successfully
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isPresent();
            NoaaTafData data = (NoaaTafData) result.getData().get();
            assertThat(data.getForecastPeriods()).hasSizeGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should handle unexpected exception gracefully")
        void testUnexpectedException() {
            // This should trigger error handling without crashing
            String taf = "TAF";

            ParseResult<NoaaWeatherData> result = parser.parse(taf);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isNotEmpty();
        }
    }
}
