/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025=2026 bclasky1539
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import weather.model.components.*;
import weather.model.components.remark.PeakWind;
import weather.model.components.remark.WindShift;
import weather.model.enums.SkyCoverage;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for NoaaMetarData class.
 *
 * Tests METAR-specific functionality only.
 * Base class functionality (including conditions) is tested in NoaaWeatherDataTest.
 *
 * @author bclasky1539
 *
 */
class NoaaMetarDataTest {

    // ========== CONSTRUCTOR TESTS ==========

    @Test
    @DisplayName("Should create NoaaMetarData with default constructor")
    void testDefaultConstructor() {
        NoaaMetarData data = new NoaaMetarData();

        assertThat(data.getReportType()).isEqualTo("METAR");
        assertThat(data.getSkyConditions()).isNotNull().isEmpty();
        assertThat(data.getRunwayVisualRange()).isNotNull().isEmpty();
        assertThat(data.isNoSignificantChange()).isFalse();
        assertThat(data.getConditions()).isNotNull();
    }

    @Test
    @DisplayName("Should create NoaaMetarData with parameterized constructor")
    void testParameterizedConstructor() {
        Instant now = Instant.now();
        NoaaMetarData data = new NoaaMetarData("KJFK", now);

        assertThat(data.getStationId()).isEqualTo("KJFK");
        assertThat(data.getObservationTime()).isEqualTo(now);
        assertThat(data.getReportType()).isEqualTo("METAR");
        assertThat(data.getConditions()).isNotNull();
    }

    // ========== CONDITIONS TESTS (Inherited) ==========
    // Note: Detailed conditions tests are in NoaaWeatherDataTest
    // These tests just verify the inheritance works correctly

    @Test
    @DisplayName("Should set and get weather conditions (inherited)")
    void testSetAndGetConditions() {
        NoaaMetarData data = new NoaaMetarData();

        WeatherConditions conditions = WeatherConditions.builder()
                .wind(Wind.of(280, 16, "KT"))
                .visibility(Visibility.statuteMiles(10.0))
                .temperature(Temperature.ofCurrent(22.0, 12.0))
                .pressure(Pressure.inchesHg(30.15))
                .build();

        data.setConditions(conditions);

        assertThat(data.getConditions()).isEqualTo(conditions);
    }

    @Test
    @DisplayName("Should get wind from conditions (inherited)")
    void testGetWind() {
        NoaaMetarData data = new NoaaMetarData();
        Wind wind = Wind.of(280, 16, "KT");

        WeatherConditions conditions = WeatherConditions.builder()
                .wind(wind)
                .build();
        data.setConditions(conditions);

        assertThat(data.getWind()).isEqualTo(wind);
    }

    @Test
    @DisplayName("Should get visibility from conditions (inherited)")
    void testGetVisibility() {
        NoaaMetarData data = new NoaaMetarData();
        Visibility visibility = Visibility.statuteMiles(10.0);

        WeatherConditions conditions = WeatherConditions.builder()
                .visibility(visibility)
                .build();
        data.setConditions(conditions);

        assertThat(data.getVisibility()).isEqualTo(visibility);
    }

    @Test
    @DisplayName("Should get sky conditions from conditions (inherited)")
    void testGetSkyConditions() {
        NoaaMetarData data = new NoaaMetarData();
        SkyCondition skyCondition = SkyCondition.of(SkyCoverage.BROKEN, 5000);

        WeatherConditions conditions = WeatherConditions.builder()
                .skyConditions(List.of(skyCondition))
                .build();
        data.setConditions(conditions);

        assertThat(data.getSkyConditions()).hasSize(1);
        assertThat(data.getSkyConditions().get(0)).isEqualTo(skyCondition);
    }

    @Test
    @DisplayName("Should get temperature from conditions (inherited)")
    void testGetTemperature() {
        NoaaMetarData data = new NoaaMetarData();
        Temperature temperature = Temperature.ofCurrent(22.0, 12.0);

        WeatherConditions conditions = WeatherConditions.builder()
                .temperature(temperature)
                .build();
        data.setConditions(conditions);

        assertThat(data.getTemperature()).isEqualTo(temperature);
    }

    @Test
    @DisplayName("Should get pressure from conditions (inherited)")
    void testGetPressure() {
        NoaaMetarData data = new NoaaMetarData();
        Pressure pressure = Pressure.inchesHg(30.15);

        WeatherConditions conditions = WeatherConditions.builder()
                .pressure(pressure)
                .build();
        data.setConditions(conditions);

        assertThat(data.getPressure()).isEqualTo(pressure);
    }

    // ========== REMARKS SECTION TESTS (METAR-Specific) ==========

    @Test
    @DisplayName("Should set and get peak wind")
    void testSetAndGetPeakWind() {
        NoaaMetarData data = new NoaaMetarData();
        PeakWind peakWind = new PeakWind(280, 32, 15, 30);

        data.setPeakWind(peakWind);

        assertThat(data.getPeakWind()).isEqualTo(peakWind);
    }

    @Test
    @DisplayName("Should set and get wind shift")
    void testSetAndGetWindShift() {
        NoaaMetarData data = new NoaaMetarData();
        WindShift windShift = new WindShift(15, 30, true);

        data.setWindShift(windShift);

        assertThat(data.getWindShift()).isEqualTo(windShift);
    }

    @Test
    @DisplayName("Should set and get automated station")
    void testSetAndGetAutomatedStation() {
        NoaaMetarData data = new NoaaMetarData();

        data.setAutomatedStation("AO2");

        assertThat(data.getAutomatedStation()).isEqualTo("AO2");
    }

    @Test
    @DisplayName("Should set and get sea level pressure")
    void testSetAndGetSeaLevelPressure() {
        NoaaMetarData data = new NoaaMetarData();

        data.setSeaLevelPressure(1013.2);

        assertThat(data.getSeaLevelPressure()).isEqualTo(1013.2);
    }

    @Test
    @DisplayName("Should set and get hourly precipitation")
    void testSetAndGetHourlyPrecipitation() {
        NoaaMetarData data = new NoaaMetarData();

        data.setHourlyPrecipitation(0.25);

        assertThat(data.getHourlyPrecipitation()).isEqualTo(0.25);
    }

    @Test
    @DisplayName("Should set and get temperature extremes")
    void testSetAndGetTemperatureExtremes() {
        NoaaMetarData data = new NoaaMetarData();

        data.setSixHourMaxTemp(30.0);
        data.setSixHourMinTemp(15.0);
        data.setTwentyFourHourMaxTemp(35.0);
        data.setTwentyFourHourMinTemp(10.0);

        assertThat(data.getSixHourMaxTemp()).isEqualTo(30.0);
        assertThat(data.getSixHourMinTemp()).isEqualTo(15.0);
        assertThat(data.getTwentyFourHourMaxTemp()).isEqualTo(35.0);
        assertThat(data.getTwentyFourHourMinTemp()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("Should set and get three hour pressure tendency")
    void testSetAndGetThreeHourPressureTendency() {
        NoaaMetarData data = new NoaaMetarData();

        data.setThreeHourPressureTendency(2.5);

        assertThat(data.getThreeHourPressureTendency()).isEqualTo(2.5);
    }

    @Test
    @DisplayName("Should set and get no significant change")
    void testSetAndGetNoSignificantChange() {
        NoaaMetarData data = new NoaaMetarData();

        data.setNoSignificantChange(true);

        assertThat(data.isNoSignificantChange()).isTrue();
    }

    // ========== UTILITY METHOD TESTS (METAR-Specific) ==========

    @Test
    @DisplayName("Should check if has AO2")
    void testHasAO2() {
        NoaaMetarData data = new NoaaMetarData();

        data.setAutomatedStation("AO2");
        assertThat(data.hasAO2()).isTrue();

        data.setAutomatedStation("AO1");
        assertThat(data.hasAO2()).isFalse();
    }

    @Test
    @DisplayName("Should check if has AO1")
    void testHasAO1() {
        NoaaMetarData data = new NoaaMetarData();

        data.setAutomatedStation("AO1");
        assertThat(data.hasAO1()).isTrue();

        data.setAutomatedStation("AO2");
        assertThat(data.hasAO1()).isFalse();
    }

    @Test
    @DisplayName("Should check if is automated")
    void testIsAutomated() {
        NoaaMetarData data = new NoaaMetarData();

        data.setAutomatedStation("AO1");
        assertThat(data.isAutomated()).isTrue();

        data.setAutomatedStation("AO2");
        assertThat(data.isAutomated()).isTrue();

        data.setAutomatedStation(null);
        assertThat(data.isAutomated()).isFalse();

        data.setAutomatedStation("MANUAL");
        assertThat(data.isAutomated()).isFalse();
    }

    @Test
    @DisplayName("Should check if has precipitation data")
    void testHasPrecipitationData() {
        NoaaMetarData data = new NoaaMetarData();

        assertThat(data.hasPrecipitationData()).isFalse();

        data.setHourlyPrecipitation(0.25);
        assertThat(data.hasPrecipitationData()).isTrue();
    }

    @Test
    @DisplayName("Should check if has temperature extremes")
    void testHasTemperatureExtremes() {
        NoaaMetarData data = new NoaaMetarData();

        assertThat(data.hasTemperatureExtremes()).isFalse();

        data.setSixHourMaxTemp(30.0);
        assertThat(data.hasTemperatureExtremes()).isTrue();

        data = new NoaaMetarData();
        data.setTwentyFourHourMinTemp(10.0);
        assertThat(data.hasTemperatureExtremes()).isTrue();
    }

    @Test
    @DisplayName("Should get minimum RVR feet")
    void testGetMinimumRvrFeet() {
        NoaaMetarData data = new NoaaMetarData();
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        data.addRunwayVisualRange(RunwayVisualRange.of("04R", 1800));
        data.addRunwayVisualRange(RunwayVisualRange.of("22L", 3000));

        assertThat(data.getMinimumRvrFeet()).isEqualTo(1800);
    }

    @Test
    @DisplayName("Should get RVR for specific runway")
    void testGetRvrForRunway_Found() {
        NoaaMetarData data = new NoaaMetarData();
        RunwayVisualRange rvr04L = RunwayVisualRange.of("04L", 2200);
        RunwayVisualRange rvr04R = RunwayVisualRange.of("04R", 1800);

        data.addRunwayVisualRange(rvr04L);
        data.addRunwayVisualRange(rvr04R);

        assertThat(data.getRvrForRunway("04L")).isEqualTo(rvr04L);
        assertThat(data.getRvrForRunway("04R")).isEqualTo(rvr04R);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL, null runway ID",
            "'', empty runway ID",
            "22R, not found runway ID"
    }, nullValues = "NULL")
    @DisplayName("Should return null for invalid runway IDs")
    void testGetRvrForRunway_InvalidRunwayIds(String runwayId) {
        NoaaMetarData data = new NoaaMetarData();
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));

        assertThat(data.getRvrForRunway(runwayId)).isNull();
    }

    // ========== OVERRIDE METHOD TESTS ==========

    @Test
    @DisplayName("Should return METAR as data type")
    void testGetDataType() {
        NoaaMetarData data = new NoaaMetarData();

        assertThat(data.getDataType()).isEqualTo("METAR");
    }

    @Test
    @DisplayName("Should generate summary with all conditions")
    void testGetSummary_Complete() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());

        WeatherConditions conditions = WeatherConditions.builder()
                .wind(Wind.ofWithGusts(280, 16, 25, "KT"))
                .visibility(Visibility.statuteMiles(10.0))
                .temperature(Temperature.ofCurrent(22.0, 12.0))
                .pressure(Pressure.inchesHg(30.15))
                .build();
        data.setConditions(conditions);

        String summary = data.getSummary();

        assertThat(summary).contains("METAR", "KJFK", "Wind:", "Vis:", "Temp:", "Dew:", "Press:");
    }

    @Test
    @DisplayName("Should generate minimal summary")
    void testGetSummary_Minimal() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());

        String summary = data.getSummary();

        assertThat(summary).contains("METAR", "KJFK");
    }

    @Test
    @DisplayName("Should generate toString with all fields")
    void testToString() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());

        WeatherConditions conditions = WeatherConditions.builder()
                .wind(Wind.of(280, 16, "KT"))
                .visibility(Visibility.statuteMiles(10.0))
                .temperature(Temperature.ofCurrent(22.0, 12.0))
                .pressure(Pressure.inchesHg(30.15))
                .skyConditions(List.of(SkyCondition.of(SkyCoverage.FEW, 25000)))
                .build();
        data.setConditions(conditions);

        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));

        String toString = data.toString();

        assertThat(toString).contains("NoaaMetarData", "KJFK", "wind=", "skyCond=1", "rvr=1");
    }

    // ========== EQUALS AND HASHCODE TESTS ==========

    @Test
    @DisplayName("Should be equal when all fields match")
    void testEquals_EqualObjects() {
        Instant now = Instant.now();

        WeatherConditions conditions = WeatherConditions.builder()
                .wind(Wind.of(280, 16, "KT"))
                .visibility(Visibility.statuteMiles(10.0))
                .temperature(Temperature.ofCurrent(22.0, 12.0))
                .pressure(Pressure.inchesHg(30.15))
                .skyConditions(List.of(SkyCondition.of(SkyCoverage.FEW, 25000)))
                .build();

        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.setRawText("METAR KJFK 191651Z");
        data1.setConditions(conditions);
        data1.setAutomatedStation("AO2");
        data1.setSeaLevelPressure(1013.2);

        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setRawText("METAR KJFK 191651Z");
        data2.setConditions(conditions);
        data2.setAutomatedStation("AO2");
        data2.setSeaLevelPressure(1013.2);

        assertThat(data1).isEqualTo(data2);
        assertThat(data2).isEqualTo(data1);
    }

    @Test
    @DisplayName("Should not be equal when station ID differs")
    void testEquals_DifferentStationId() {
        Instant now = Instant.now();

        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        NoaaMetarData data2 = new NoaaMetarData("KLGA", now);

        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    @DisplayName("Should not be equal when conditions differ")
    void testEquals_DifferentConditions() {
        Instant now = Instant.now();

        WeatherConditions conditions1 = WeatherConditions.builder()
                .wind(Wind.of(280, 16, "KT"))
                .build();

        WeatherConditions conditions2 = WeatherConditions.builder()
                .wind(Wind.of(290, 20, "KT"))
                .build();

        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.setConditions(conditions1);

        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setConditions(conditions2);

        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    @DisplayName("Should not be equal when METAR-specific fields differ")
    void testEquals_DifferentMetarFields() {
        Instant now = Instant.now();

        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.setAutomatedStation("AO2");

        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setAutomatedStation("AO1");

        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    @DisplayName("Should not be equal when compared with null")
    void testEquals_WithNull() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());

        assertThat(data).isNotEqualTo(null);
    }

    @Test
    @DisplayName("Should have consistent hashCode")
    void testHashCode_Consistency() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());

        WeatherConditions conditions = WeatherConditions.builder()
                .wind(Wind.of(280, 16, "KT"))
                .build();
        data.setConditions(conditions);
        data.setAutomatedStation("AO2");

        int hash1 = data.hashCode();
        int hash2 = data.hashCode();

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Should have same hashCode for equal objects")
    void testHashCode_EqualObjects() {
        Instant now = Instant.now();

        WeatherConditions conditions = WeatherConditions.builder()
                .wind(Wind.of(280, 16, "KT"))
                .build();

        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.setConditions(conditions);
        data1.setAutomatedStation("AO2");

        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setConditions(conditions);
        data2.setAutomatedStation("AO2");

        assertThat(data1).hasSameHashCodeAs(data2);
    }
}
