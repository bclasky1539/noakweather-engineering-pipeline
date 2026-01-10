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
package weather.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import weather.model.components.*;
import weather.model.enums.SkyCoverage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

/**
 * Tests for NoaaTafData class.
 *
 * Tests TAF-specific functionality only.
 * Base class functionality (including inherited WeatherConditions) is tested in NoaaWeatherDataTest.
 *
 * @author bclasky1539
 *
 */
class NoaaTafDataTest {

    // ========== CONSTRUCTOR TESTS ==========

    @Test
    @DisplayName("Should create NoaaTafData with default constructor")
    void testDefaultConstructor() {
        NoaaTafData data = new NoaaTafData();

        assertThat(data.getReportType()).isEqualTo("TAF");
        assertThat(data.getForecastPeriods()).isNotNull().isEmpty();
        assertThat(data.getConditions()).isNotNull(); // Inherited from parent
    }

    @Test
    @DisplayName("Should create NoaaTafData with station and issue time")
    void testParameterizedConstructor() {
        Instant issueTime = Instant.now();
        NoaaTafData data = new NoaaTafData("KJFK", issueTime);

        assertThat(data.getStationId()).isEqualTo("KJFK");
        assertThat(data.getIssueTime()).isEqualTo(issueTime);
        assertThat(data.getReportType()).isEqualTo("TAF");
        assertThat(data.getForecastPeriods()).isNotNull().isEmpty();
        assertThat(data.getConditions()).isNotNull(); // Inherited from parent
    }

    @Test
    @DisplayName("Should create NoaaTafData with validity period")
    void testConstructorWithValidityPeriod() {
        Instant issueTime = Instant.now();
        Instant validStart = issueTime.plus(1, ChronoUnit.HOURS);
        Instant validEnd = validStart.plus(24, ChronoUnit.HOURS);
        ValidityPeriod validity = new ValidityPeriod(validStart, validEnd);

        NoaaTafData data = new NoaaTafData("KJFK", issueTime, validity);

        assertThat(data.getStationId()).isEqualTo("KJFK");
        assertThat(data.getIssueTime()).isEqualTo(issueTime);
        assertThat(data.getValidityPeriod()).isEqualTo(validity);
    }

    // ========== INHERITED CONDITIONS TESTS ==========
    // Note: Base forecast conditions are stored in parent's WeatherConditions
    // Detailed tests are in NoaaWeatherDataTest; these just verify inheritance works

    @Test
    @DisplayName("Should set and get base conditions (inherited from parent)")
    void testSetAndGetBaseConditions() {
        NoaaTafData data = new NoaaTafData();

        WeatherConditions baseConditions = WeatherConditions.builder()
                .wind(Wind.of(280, 16, "KT"))
                .visibility(Visibility.statuteMiles(10.0))
                .skyConditions(List.of(SkyCondition.of(SkyCoverage.FEW, 25000)))
                .build();

        data.setConditions(baseConditions);

        assertThat(data.getConditions()).isEqualTo(baseConditions);
        assertThat(data.getWind()).isEqualTo(Wind.of(280, 16, "KT"));
        assertThat(data.getVisibility()).isEqualTo(Visibility.statuteMiles(10.0));
        assertThat(data.getSkyConditions()).hasSize(1);
    }

    @Test
    @DisplayName("Should access wind from base conditions (inherited)")
    void testGetWindFromBaseConditions() {
        NoaaTafData data = new NoaaTafData();
        Wind wind = Wind.of(290, 12, "KT");

        data.setConditions(WeatherConditions.builder().wind(wind).build());

        assertThat(data.getWind()).isEqualTo(wind);
    }

    @Test
    @DisplayName("Should access visibility from base conditions (inherited)")
    void testGetVisibilityFromBaseConditions() {
        NoaaTafData data = new NoaaTafData();
        Visibility visibility = Visibility.statuteMiles(6.0);

        data.setConditions(WeatherConditions.builder().visibility(visibility).build());

        assertThat(data.getVisibility()).isEqualTo(visibility);
    }

    // ========== VALIDITY PERIOD TESTS ==========

    @Test
    @DisplayName("Should set and get validity period")
    void testSetAndGetValidityPeriod() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);
        ValidityPeriod validity = new ValidityPeriod(start, end);

        data.setValidityPeriod(validity);

        assertThat(data.getValidityPeriod()).isEqualTo(validity);
    }

    @Test
    @DisplayName("Should check if currently valid")
    void testIsCurrentlyValid_True() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = start.plus(24, ChronoUnit.HOURS);
        ValidityPeriod validity = new ValidityPeriod(start, end);
        data.setValidityPeriod(validity);

        assertThat(data.isCurrentlyValid()).isTrue();
    }

    @Test
    @DisplayName("Should check if currently valid when expired")
    void testIsCurrentlyValid_False() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now().minus(48, ChronoUnit.HOURS);
        Instant end = start.plus(24, ChronoUnit.HOURS);
        ValidityPeriod validity = new ValidityPeriod(start, end);
        data.setValidityPeriod(validity);

        assertThat(data.isCurrentlyValid()).isFalse();
    }

    @Test
    @DisplayName("Should return false when validity period is null")
    void testIsCurrentlyValid_NullValidity() {
        NoaaTafData data = new NoaaTafData();

        assertThat(data.isCurrentlyValid()).isFalse();
    }

    @Test
    @DisplayName("Should check if has expired")
    void testHasExpired_True() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now().minus(48, ChronoUnit.HOURS);
        Instant end = start.plus(24, ChronoUnit.HOURS);
        ValidityPeriod validity = new ValidityPeriod(start, end);
        data.setValidityPeriod(validity);

        assertThat(data.hasExpired()).isTrue();
    }

    @Test
    @DisplayName("Should check if has expired when still valid")
    void testHasExpired_False() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = start.plus(24, ChronoUnit.HOURS);
        ValidityPeriod validity = new ValidityPeriod(start, end);
        data.setValidityPeriod(validity);

        assertThat(data.hasExpired()).isFalse();
    }

    @Test
    @DisplayName("Should return false for hasExpired when validity period is null")
    void testHasExpired_NullValidity() {
        NoaaTafData data = new NoaaTafData();

        assertThat(data.hasExpired()).isFalse();
    }

    // ========== FORECAST PERIOD TESTS ==========

    @Test
    @DisplayName("Should add forecast period")
    void testAddForecastPeriod() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);
        WeatherConditions conditions = WeatherConditions.empty();

        ForecastPeriod period = ForecastPeriod.base(start, end, conditions);
        data.addForecastPeriod(period);

        assertThat(data.getForecastPeriods()).hasSize(1);
        assertThat(data.getForecastPeriods()).contains(period);
    }

    @Test
    @DisplayName("Should not add null forecast period")
    void testAddForecastPeriod_Null() {
        NoaaTafData data = new NoaaTafData();

        data.addForecastPeriod(null);

        assertThat(data.getForecastPeriods()).isEmpty();
    }

    @Test
    @DisplayName("Should get forecast periods as immutable copy")
    void testGetForecastPeriods_Immutable() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);
        ForecastPeriod period = ForecastPeriod.base(start, end, WeatherConditions.empty());
        data.addForecastPeriod(period);

        List<ForecastPeriod> periods = data.getForecastPeriods();

        assertThatThrownBy(() -> periods.add(ForecastPeriod.base(start, end, WeatherConditions.empty())))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should set forecast periods")
    void testSetForecastPeriods() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);

        List<ForecastPeriod> periods = new ArrayList<>();
        periods.add(ForecastPeriod.base(start, end, WeatherConditions.empty()));
        periods.add(ForecastPeriod.from(start.plus(6, ChronoUnit.HOURS), WeatherConditions.empty()));

        data.setForecastPeriods(periods);

        assertThat(data.getForecastPeriods()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle null forecast periods list")
    void testSetForecastPeriods_Null() {
        NoaaTafData data = new NoaaTafData();

        data.setForecastPeriods(null);

        assertThat(data.getForecastPeriods()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should get forecast period count")
    void testGetForecastPeriodCount() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);

        data.addForecastPeriod(ForecastPeriod.base(start, end, WeatherConditions.empty()));
        data.addForecastPeriod(ForecastPeriod.from(start.plus(6, ChronoUnit.HOURS), WeatherConditions.empty()));
        data.addForecastPeriod(ForecastPeriod.tempo(start, start.plus(4, ChronoUnit.HOURS), WeatherConditions.empty()));

        assertThat(data.getForecastPeriodCount()).isEqualTo(3);
    }

    // ========== QUERY METHOD TESTS ==========

    @Test
    @DisplayName("Should get base forecast")
    void testGetBaseForecast() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);

        ForecastPeriod basePeriod = ForecastPeriod.base(start, end, WeatherConditions.empty());
        data.addForecastPeriod(basePeriod);
        data.addForecastPeriod(ForecastPeriod.from(start.plus(6, ChronoUnit.HOURS), WeatherConditions.empty()));

        assertThat(data.getBaseForecast()).isEqualTo(basePeriod);
    }

    @Test
    @DisplayName("Should return null when no base forecast")
    void testGetBaseForecast_None() {
        NoaaTafData data = new NoaaTafData();

        assertThat(data.getBaseForecast()).isNull();
    }

    @Test
    @DisplayName("Should get FROM periods")
    void testGetFromPeriods() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);

        ForecastPeriod fm1 = ForecastPeriod.from(start.plus(6, ChronoUnit.HOURS), WeatherConditions.empty());
        ForecastPeriod fm2 = ForecastPeriod.from(start.plus(12, ChronoUnit.HOURS), WeatherConditions.empty());

        data.addForecastPeriod(ForecastPeriod.base(start, end, WeatherConditions.empty()));
        data.addForecastPeriod(fm1);
        data.addForecastPeriod(fm2);
        data.addForecastPeriod(ForecastPeriod.tempo(start, start.plus(4, ChronoUnit.HOURS), WeatherConditions.empty()));

        assertThat(data.getFromPeriods()).hasSize(2);
        assertThat(data.getFromPeriods()).containsExactly(fm1, fm2);
    }

    @Test
    @DisplayName("Should get TEMPO periods")
    void testGetTempoPeriods() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);

        ForecastPeriod tempo1 = ForecastPeriod.tempo(start, start.plus(4, ChronoUnit.HOURS), WeatherConditions.empty());
        ForecastPeriod tempo2 = ForecastPeriod.tempo(start.plus(8, ChronoUnit.HOURS), start.plus(12, ChronoUnit.HOURS), WeatherConditions.empty());

        data.addForecastPeriod(ForecastPeriod.base(start, end, WeatherConditions.empty()));
        data.addForecastPeriod(tempo1);
        data.addForecastPeriod(tempo2);

        assertThat(data.getTempoPeriods()).hasSize(2);
        assertThat(data.getTempoPeriods()).containsExactly(tempo1, tempo2);
    }

    @Test
    @DisplayName("Should get BECMG periods")
    void testGetBecomingPeriods() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);

        ForecastPeriod becmg = ForecastPeriod.becoming(start, start.plus(2, ChronoUnit.HOURS), WeatherConditions.empty());

        data.addForecastPeriod(ForecastPeriod.base(start, end, WeatherConditions.empty()));
        data.addForecastPeriod(becmg);

        assertThat(data.getBecomingPeriods()).hasSize(1);
        assertThat(data.getBecomingPeriods()).contains(becmg);
    }

    @Test
    @DisplayName("Should get PROB periods")
    void testGetProbabilityPeriods() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);

        ForecastPeriod prob = ForecastPeriod.probability(start, start.plus(6, ChronoUnit.HOURS), 30, WeatherConditions.empty());

        data.addForecastPeriod(ForecastPeriod.base(start, end, WeatherConditions.empty()));
        data.addForecastPeriod(prob);

        assertThat(data.getProbabilityPeriods()).hasSize(1);
        assertThat(data.getProbabilityPeriods()).contains(prob);
    }

    @Test
    @DisplayName("Should get current forecast period")
    void testGetCurrentForecastPeriod() {
        NoaaTafData data = new NoaaTafData();
        Instant now = Instant.now();
        Instant past = now.minus(2, ChronoUnit.HOURS);
        Instant future = now.plus(2, ChronoUnit.HOURS);

        ForecastPeriod currentPeriod = ForecastPeriod.base(past, future, WeatherConditions.empty());
        data.addForecastPeriod(currentPeriod);

        assertThat(data.getCurrentForecastPeriod()).isEqualTo(currentPeriod);
    }

    @Test
    @DisplayName("Should return null when no current forecast period")
    void testGetCurrentForecastPeriod_None() {
        NoaaTafData data = new NoaaTafData();
        Instant past = Instant.now().minus(48, ChronoUnit.HOURS);

        data.addForecastPeriod(ForecastPeriod.base(past, past.plus(24, ChronoUnit.HOURS), WeatherConditions.empty()));

        assertThat(data.getCurrentForecastPeriod()).isNull();
    }

    @Test
    @DisplayName("Should get forecast periods at specific time")
    void testGetForecastPeriodsAt() {
        NoaaTafData data = new NoaaTafData();
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(23, ChronoUnit.HOURS);

        ForecastPeriod base = ForecastPeriod.base(start, end, WeatherConditions.empty());
        ForecastPeriod tempo = ForecastPeriod.tempo(now.minus(30, ChronoUnit.MINUTES), now.plus(30, ChronoUnit.MINUTES), WeatherConditions.empty());

        data.addForecastPeriod(base);
        data.addForecastPeriod(tempo);

        List<ForecastPeriod> activePeriods = data.getForecastPeriodsAt(now);

        assertThat(activePeriods)
                .hasSize(2)
                .contains(base, tempo);
    }

    @Test
    @DisplayName("Should return empty list when time is null")
    void testGetForecastPeriodsAt_NullTime() {
        NoaaTafData data = new NoaaTafData();

        assertThat(data.getForecastPeriodsAt(null)).isEmpty();
    }

    @Test
    @DisplayName("Should check if has significant weather forecast")
    void testHasSignificantWeatherForecast_True() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now();

        WeatherConditions significantWeather = WeatherConditions.builder()
                .presentWeather(List.of(PresentWeather.parse("TSRA")))
                .build();

        data.addForecastPeriod(ForecastPeriod.tempo(start, start.plus(4, ChronoUnit.HOURS), significantWeather));

        assertThat(data.hasSignificantWeatherForecast()).isTrue();
    }

    @Test
    @DisplayName("Should check if has significant weather forecast when none")
    void testHasSignificantWeatherForecast_False() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now();

        data.addForecastPeriod(ForecastPeriod.base(start, start.plus(24, ChronoUnit.HOURS), WeatherConditions.empty()));

        assertThat(data.hasSignificantWeatherForecast()).isFalse();
    }

    // ========== TEMPERATURE FORECAST TESTS ==========

    @Test
    @DisplayName("Should set and get max temperature")
    void testSetAndGetMaxTemperature() {
        NoaaTafData data = new NoaaTafData();

        data.setMaxTemperature(25);

        assertThat(data.getMaxTemperature()).isEqualTo(25);
    }

    @Test
    @DisplayName("Should set and get max temperature time")
    void testSetAndGetMaxTemperatureTime() {
        NoaaTafData data = new NoaaTafData();
        Instant time = Instant.now();

        data.setMaxTemperatureTime(time);

        assertThat(data.getMaxTemperatureTime()).isEqualTo(time);
    }

    @Test
    @DisplayName("Should set max temperature forecast with convenience method")
    void testSetMaxTemperatureForecast() {
        NoaaTafData data = new NoaaTafData();
        Instant time = Instant.now();

        data.setMaxTemperatureForecast(30, time);

        assertThat(data.getMaxTemperature()).isEqualTo(30);
        assertThat(data.getMaxTemperatureTime()).isEqualTo(time);
    }

    @Test
    @DisplayName("Should set and get min temperature")
    void testSetAndGetMinTemperature() {
        NoaaTafData data = new NoaaTafData();

        data.setMinTemperature(5);

        assertThat(data.getMinTemperature()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should set and get min temperature time")
    void testSetAndGetMinTemperatureTime() {
        NoaaTafData data = new NoaaTafData();
        Instant time = Instant.now();

        data.setMinTemperatureTime(time);

        assertThat(data.getMinTemperatureTime()).isEqualTo(time);
    }

    @Test
    @DisplayName("Should set min temperature forecast with convenience method")
    void testSetMinTemperatureForecast() {
        NoaaTafData data = new NoaaTafData();
        Instant time = Instant.now();

        data.setMinTemperatureForecast(10, time);

        assertThat(data.getMinTemperature()).isEqualTo(10);
        assertThat(data.getMinTemperatureTime()).isEqualTo(time);
    }

    // ========== REPORT MODIFIER TESTS ==========

    @Test
    @DisplayName("Should check if is amended")
    void testIsAmended_True() {
        NoaaTafData data = new NoaaTafData();
        data.setReportModifier("AMD");

        assertThat(data.isAmended()).isTrue();
    }

    @Test
    @DisplayName("Should check if is amended when not")
    void testIsAmended_False() {
        NoaaTafData data = new NoaaTafData();
        data.setReportModifier("COR");

        assertThat(data.isAmended()).isFalse();
    }

    @Test
    @DisplayName("Should check if is corrected")
    void testIsCorrected_True() {
        NoaaTafData data = new NoaaTafData();
        data.setReportModifier("COR");

        assertThat(data.isCorrected()).isTrue();
    }

    @Test
    @DisplayName("Should check if is corrected when not")
    void testIsCorrected_False() {
        NoaaTafData data = new NoaaTafData();
        data.setReportModifier("AMD");

        assertThat(data.isCorrected()).isFalse();
    }

    // ========== OVERRIDE METHOD TESTS ==========

    @Test
    @DisplayName("Should return TAF as data type")
    void testGetDataType() {
        NoaaTafData data = new NoaaTafData();

        assertThat(data.getDataType()).isEqualTo("TAF");
    }

    @Test
    @DisplayName("Should return currently valid for isCurrent")
    void testIsCurrent() {
        NoaaTafData data = new NoaaTafData();
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = start.plus(24, ChronoUnit.HOURS);
        data.setValidityPeriod(new ValidityPeriod(start, end));

        assertThat(data.isCurrent()).isTrue();
    }

    @Test
    @DisplayName("Should generate summary with all details")
    void testGetSummary_Complete() {
        NoaaTafData data = new NoaaTafData("KJFK", Instant.now());
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);

        data.setValidityPeriod(new ValidityPeriod(start, end));
        data.addForecastPeriod(ForecastPeriod.base(start, end, WeatherConditions.empty()));
        data.setMaxTemperature(25);
        data.setMinTemperature(10);

        String summary = data.getSummary();

        assertThat(summary)
                .contains("TAF")
                .contains("KJFK")
                .contains("1 periods")
                .contains("TX25")
                .contains("TN10");
    }

    @Test
    @DisplayName("Should generate summary for amended TAF")
    void testGetSummary_Amended() {
        NoaaTafData data = new NoaaTafData("KJFK", Instant.now());
        data.setReportModifier("AMD");

        String summary = data.getSummary();

        assertThat(summary).contains("TAF AMD");
    }

    @Test
    @DisplayName("Should generate summary for corrected TAF")
    void testGetSummary_Corrected() {
        NoaaTafData data = new NoaaTafData("KJFK", Instant.now());
        data.setReportModifier("COR");

        String summary = data.getSummary();

        assertThat(summary).contains("TAF COR");
    }

    @Test
    @DisplayName("Should generate toString with all fields")
    void testToString() {
        NoaaTafData data = new NoaaTafData("KJFK", Instant.now());
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);

        data.setValidityPeriod(new ValidityPeriod(start, end));
        data.addForecastPeriod(ForecastPeriod.base(start, end, WeatherConditions.empty()));
        data.setMaxTemperature(25);
        data.setMinTemperature(10);

        String toString = data.toString();

        assertThat(toString)
                .contains("NoaaTafData")
                .contains("KJFK")
                .contains("periods=1")
                .contains("maxTemp=25")
                .contains("minTemp=10");
    }

    // ========== EQUALS AND HASHCODE TESTS ==========

    @Test
    @DisplayName("Should be equal when all fields match")
    void testEquals_EqualObjects() {
        Instant issueTime = Instant.now();
        Instant start = issueTime.plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(24, ChronoUnit.HOURS);
        ValidityPeriod validity = new ValidityPeriod(start, end);

        NoaaTafData data1 = new NoaaTafData("KJFK", issueTime, validity);
        data1.setRawText("TAF KJFK...");
        data1.addForecastPeriod(ForecastPeriod.base(start, end, WeatherConditions.empty()));
        data1.setMaxTemperature(25);

        NoaaTafData data2 = new NoaaTafData("KJFK", issueTime, validity);
        data2.setRawText("TAF KJFK...");
        data2.addForecastPeriod(ForecastPeriod.base(start, end, WeatherConditions.empty()));
        data2.setMaxTemperature(25);

        assertThat(data1).isEqualTo(data2);
        assertThat(data2).isEqualTo(data1);
    }

    @Test
    @DisplayName("Should not be equal when station ID differs")
    void testEquals_DifferentStationId() {
        Instant issueTime = Instant.now();

        NoaaTafData data1 = new NoaaTafData("KJFK", issueTime);
        NoaaTafData data2 = new NoaaTafData("KLGA", issueTime);

        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    @DisplayName("Should not be equal when issue time differs")
    void testEquals_DifferentIssueTime() {
        NoaaTafData data1 = new NoaaTafData("KJFK", Instant.now());
        NoaaTafData data2 = new NoaaTafData("KJFK", Instant.now().plusSeconds(60));

        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    @DisplayName("Should not be equal when validity period differs")
    void testEquals_DifferentValidityPeriod() {
        Instant issueTime = Instant.now();
        Instant start = issueTime.plus(1, ChronoUnit.HOURS);

        ValidityPeriod validity1 = new ValidityPeriod(start, start.plus(24, ChronoUnit.HOURS));
        ValidityPeriod validity2 = new ValidityPeriod(start, start.plus(30, ChronoUnit.HOURS));

        NoaaTafData data1 = new NoaaTafData("KJFK", issueTime, validity1);
        NoaaTafData data2 = new NoaaTafData("KJFK", issueTime, validity2);

        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    @DisplayName("Should not be equal when forecast periods differ")
    void testEquals_DifferentForecastPeriods() {
        Instant issueTime = Instant.now();
        Instant start = issueTime.plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(24, ChronoUnit.HOURS);

        NoaaTafData data1 = new NoaaTafData("KJFK", issueTime);
        data1.addForecastPeriod(ForecastPeriod.base(start, end, WeatherConditions.empty()));

        NoaaTafData data2 = new NoaaTafData("KJFK", issueTime);
        data2.addForecastPeriod(ForecastPeriod.from(start, WeatherConditions.empty()));

        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    @DisplayName("Should not be equal when base conditions differ")
    void testEquals_DifferentBaseConditions() {
        Instant issueTime = Instant.now();

        WeatherConditions conditions1 = WeatherConditions.builder()
                .wind(Wind.of(280, 16, "KT"))
                .build();

        WeatherConditions conditions2 = WeatherConditions.builder()
                .wind(Wind.of(290, 20, "KT"))
                .build();

        NoaaTafData data1 = new NoaaTafData("KJFK", issueTime);
        data1.setConditions(conditions1);

        NoaaTafData data2 = new NoaaTafData("KJFK", issueTime);
        data2.setConditions(conditions2);

        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    @DisplayName("Should not be equal when compared with null")
    void testEquals_WithNull() {
        NoaaTafData data = new NoaaTafData("KJFK", Instant.now());

        assertThat(data).isNotEqualTo(null);
    }

    @Test
    @DisplayName("Should have consistent hashCode")
    void testHashCode_Consistency() {
        NoaaTafData data = new NoaaTafData("KJFK", Instant.now());

        int hash1 = data.hashCode();
        int hash2 = data.hashCode();

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Should have same hashCode for equal objects")
    void testHashCode_EqualObjects() {
        Instant issueTime = Instant.now();
        Instant start = issueTime.plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(24, ChronoUnit.HOURS);

        NoaaTafData data1 = new NoaaTafData("KJFK", issueTime);
        data1.addForecastPeriod(ForecastPeriod.base(start, end, WeatherConditions.empty()));

        NoaaTafData data2 = new NoaaTafData("KJFK", issueTime);
        data2.addForecastPeriod(ForecastPeriod.base(start, end, WeatherConditions.empty()));

        assertThat(data1).hasSameHashCodeAs(data2);
    }
}
