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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import weather.model.components.*;
import weather.model.components.remark.NoaaMetarRemarks;
import weather.model.enums.AutomatedStationType;
import weather.model.enums.SkyCoverage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NoaaWeatherData base class.
 * Tests NOAA-specific fields and WeatherConditions integration.
 *
 * @author bclasky1539
 */
class NoaaWeatherDataTest {

    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
    }

    // ========== CONSTRUCTOR TESTS ==========

    @Test
    @DisplayName("Should create NoaaWeatherData with default constructor")
    void testDefaultConstructor() {
        NoaaWeatherData data = new NoaaWeatherData();

        assertNotNull(data);
        assertNotNull(data.getId());
        assertNotNull(data.getIngestionTime());
        assertNotNull(data.getConditions());
        assertNotNull(data.getSkyConditions());
        assertNotNull(data.getRunwayVisualRange());
        assertTrue(data.getSkyConditions().isEmpty());
        assertTrue(data.getRunwayVisualRange().isEmpty());
    }

    @Test
    @DisplayName("Should create NoaaWeatherData with parameterized constructor")
    void testParameterizedConstructor() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertNotNull(data);
        assertEquals("KJFK", data.getStationId());
        assertEquals(now, data.getObservationTime());
        assertEquals("METAR", data.getReportType());
        assertEquals(WeatherDataSource.NOAA, data.getSource());
        assertNotNull(data.getConditions());
        assertNotNull(data.getSkyConditions());
        assertNotNull(data.getRunwayVisualRange());
    }

    // ========== CONDITIONS TESTS ==========

    @Test
    @DisplayName("Should set and get weather conditions")
    void testSetAndGetConditions() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

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
    @DisplayName("Should handle null conditions by setting empty")
    void testSetConditions_Null() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        data.setConditions(null);

        assertThat(data.getConditions()).isNotNull();
        assertThat(data.getConditions()).isEqualTo(WeatherConditions.empty());
    }

    // ========== CONVENIENCE GETTER TESTS ==========

    @Test
    @DisplayName("Should get wind from conditions")
    void testGetWind() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        Wind wind = Wind.of(280, 16, "KT");

        WeatherConditions conditions = WeatherConditions.builder()
                .wind(wind)
                .build();
        data.setConditions(conditions);

        assertThat(data.getWind()).isEqualTo(wind);
    }

    @Test
    @DisplayName("Should return null when no wind in conditions")
    void testGetWind_Null() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertThat(data.getWind()).isNull();
    }

    @Test
    @DisplayName("Should get visibility from conditions")
    void testGetVisibility() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        Visibility visibility = Visibility.statuteMiles(10.0);

        WeatherConditions conditions = WeatherConditions.builder()
                .visibility(visibility)
                .build();
        data.setConditions(conditions);

        assertThat(data.getVisibility()).isEqualTo(visibility);
    }

    @Test
    @DisplayName("Should return null when no visibility in conditions")
    void testGetVisibility_Null() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertThat(data.getVisibility()).isNull();
    }

    @Test
    @DisplayName("Should get present weather from conditions")
    void testGetPresentWeather() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        PresentWeather weather = PresentWeather.of("-RA");

        WeatherConditions conditions = WeatherConditions.builder()
                .presentWeather(List.of(weather))
                .build();
        data.setConditions(conditions);

        assertThat(data.getPresentWeather()).hasSize(1);
        assertThat(data.getPresentWeather()).contains(weather);
    }

    @Test
    @DisplayName("Should return empty list when no present weather in conditions")
    void testGetPresentWeather_Empty() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertThat(data.getPresentWeather()).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when conditions is null for present weather")
    void testGetPresentWeather_NullConditions() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        data.setConditions(null);

        assertThat(data.getPresentWeather()).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when present weather is null in conditions")
    void testGetPresentWeather_NullWeatherInConditions() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        WeatherConditions conditions = WeatherConditions.builder()
                .wind(Wind.of(280, 16, "KT"))
                // No present weather set
                .build();
        data.setConditions(conditions);

        assertThat(data.getPresentWeather()).isEmpty();
    }

    @Test
    @DisplayName("Should get temperature from conditions")
    void testGetTemperature() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        Temperature temperature = Temperature.ofCurrent(22.0, 12.0);

        WeatherConditions conditions = WeatherConditions.builder()
                .temperature(temperature)
                .build();
        data.setConditions(conditions);

        assertThat(data.getTemperature()).isEqualTo(temperature);
    }

    @Test
    @DisplayName("Should return null when no temperature in conditions")
    void testGetTemperature_Null() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertThat(data.getTemperature()).isNull();
    }

    @Test
    @DisplayName("Should get pressure from conditions")
    void testGetPressure() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        Pressure pressure = Pressure.inchesHg(30.15);

        WeatherConditions conditions = WeatherConditions.builder()
                .pressure(pressure)
                .build();
        data.setConditions(conditions);

        assertThat(data.getPressure()).isEqualTo(pressure);
    }

    @Test
    @DisplayName("Should return null when no pressure in conditions")
    void testGetPressure_Null() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertThat(data.getPressure()).isNull();
    }

    // ========== SKY CONDITION TESTS (via WeatherConditions) ==========

    @Test
    @DisplayName("Should get sky conditions from weather conditions")
    void testGetSkyConditions() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        SkyCondition skyCondition = SkyCondition.of(SkyCoverage.BROKEN, 5000);

        WeatherConditions conditions = WeatherConditions.builder()
                .skyConditions(List.of(skyCondition))
                .build();
        data.setConditions(conditions);

        List<SkyCondition> skyConditions = data.getSkyConditions();
        assertThat(skyConditions).hasSize(1);
        assertThat(skyConditions.get(0)).isEqualTo(skyCondition);
    }

    @Test
    @DisplayName("Should get multiple sky conditions")
    void testGetMultipleSkyConditions() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        SkyCondition few = SkyCondition.of(SkyCoverage.FEW, 1500);
        SkyCondition sct = SkyCondition.of(SkyCoverage.SCATTERED, 4000);
        SkyCondition bkn = SkyCondition.of(SkyCoverage.BROKEN, 8000);

        WeatherConditions conditions = WeatherConditions.builder()
                .skyConditions(List.of(few, sct, bkn))
                .build();
        data.setConditions(conditions);

        List<SkyCondition> skyConditions = data.getSkyConditions();
        assertThat(skyConditions).hasSize(3);
        assertThat(skyConditions.get(0)).isEqualTo(few);
        assertThat(skyConditions.get(1)).isEqualTo(sct);
        assertThat(skyConditions.get(2)).isEqualTo(bkn);
    }

    @Test
    @DisplayName("Should return empty list when no sky conditions in weather conditions")
    void testGetSkyConditions_Empty() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        WeatherConditions conditions = WeatherConditions.builder()
                .wind(Wind.of(280, 16, "KT"))
                .build();
        data.setConditions(conditions);

        List<SkyCondition> skyConditions = data.getSkyConditions();
        assertThat(skyConditions).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when conditions is null for sky conditions")
    void testGetSkyConditions_NullConditions() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        data.setConditions(null);

        assertThat(data.getSkyConditions()).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when sky conditions is null in conditions")
    void testGetSkyConditions_NullSkyInConditions() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        WeatherConditions conditions = WeatherConditions.builder()
                .wind(Wind.of(280, 16, "KT"))
                // No sky conditions set
                .build();
        data.setConditions(conditions);

        assertThat(data.getSkyConditions()).isEmpty();
    }

    @Test
    @DisplayName("Should get sky conditions as immutable copy")
    void testGetSkyConditionsImmutable() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        SkyCondition skyCondition = SkyCondition.of(SkyCoverage.BROKEN, 5000);

        WeatherConditions conditions = WeatherConditions.builder()
                .skyConditions(List.of(skyCondition))
                .build();
        data.setConditions(conditions);

        List<SkyCondition> skyConditions = data.getSkyConditions();

        // Verify it's an immutable copy
        SkyCondition additionalCondition = SkyCondition.of(SkyCoverage.OVERCAST, 2000);
        assertThrows(UnsupportedOperationException.class, () ->
                skyConditions.add(additionalCondition)
        );
    }

    @Test
    @DisplayName("Should handle sky conditions with cloud types")
    void testSkyConditionWithCloudType() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        SkyCondition cb = SkyCondition.of(SkyCoverage.BROKEN, 3000, "CB");

        WeatherConditions conditions = WeatherConditions.builder()
                .skyConditions(List.of(cb))
                .build();
        data.setConditions(conditions);

        List<SkyCondition> skyConditions = data.getSkyConditions();
        assertThat(skyConditions).hasSize(1);
        assertThat(skyConditions.get(0).cloudType()).isEqualTo("CB");
        assertThat(skyConditions.get(0).isCumulonimbus()).isTrue();
    }

    @Test
    @DisplayName("Should handle clear sky conditions")
    void testClearSkyCondition() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        SkyCondition clear = SkyCondition.clear();

        WeatherConditions conditions = WeatherConditions.builder()
                .skyConditions(List.of(clear))
                .build();
        data.setConditions(conditions);

        List<SkyCondition> skyConditions = data.getSkyConditions();
        assertThat(skyConditions).hasSize(1);
        assertThat(skyConditions.get(0).isClear()).isTrue();
        assertThat(skyConditions.get(0).heightFeet()).isNull();
    }

    @Test
    @DisplayName("Should handle vertical visibility")
    void testVerticalVisibility() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        SkyCondition vv = SkyCondition.verticalVisibility(800);

        WeatherConditions conditions = WeatherConditions.builder()
                .skyConditions(List.of(vv))
                .build();
        data.setConditions(conditions);

        List<SkyCondition> skyConditions = data.getSkyConditions();
        assertThat(skyConditions).hasSize(1);
        assertThat(skyConditions.get(0).coverage()).isEqualTo(SkyCoverage.VERTICAL_VISIBILITY);
        assertThat(skyConditions.get(0).heightFeet()).isEqualTo(800);
        assertThat(skyConditions.get(0).isCeiling()).isTrue();
    }

    // ========== GETTER/SETTER TESTS ==========

    @Test
    @DisplayName("Should get and set report type")
    void testGetSetReportType() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertEquals("METAR", data.getReportType());

        data.setReportType("TAF");
        assertEquals("TAF", data.getReportType());
    }

    @Test
    @DisplayName("Should get and set raw text")
    void testGetSetRawText() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertNull(data.getRawText());

        String rawText = "METAR KJFK 121251Z 31008KT 10SM FEW250 M04/M17 A3034";
        data.setRawText(rawText);

        assertEquals(rawText, data.getRawText());
    }

    @Test
    @DisplayName("Should get and set report modifier")
    void testGetSetReportModifier() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertNull(data.getReportModifier());

        data.setReportModifier("AUTO");
        assertEquals("AUTO", data.getReportModifier());

        data.setReportModifier("COR");
        assertEquals("COR", data.getReportModifier());
    }

    @Test
    @DisplayName("Should get and set latitude")
    void testGetSetLatitude() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertNull(data.getLatitude());

        data.setLatitude(40.6413);
        assertEquals(40.6413, data.getLatitude());
    }

    @Test
    @DisplayName("Should get and set longitude")
    void testGetSetLongitude() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertNull(data.getLongitude());

        data.setLongitude(-73.7781);
        assertEquals(-73.7781, data.getLongitude());
    }

    @Test
    @DisplayName("Should get and set elevation feet")
    void testGetSetElevationFeet() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertNull(data.getElevationFeet());

        data.setElevationFeet(13);
        assertEquals(13, data.getElevationFeet());
    }

    @Test
    @DisplayName("Should get and set quality control flags")
    void testGetSetQualityControlFlags() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertNull(data.getQualityControlFlags());

        data.setQualityControlFlags("CORRECTED");
        assertEquals("CORRECTED", data.getQualityControlFlags());
    }

    // ========== RUNWAY VISUAL RANGE TESTS ==========

    @Test
    @DisplayName("Should add runway visual range")
    void testAddRunwayVisualRange() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        RunwayVisualRange rvr = RunwayVisualRange.of("04L", 2200);

        data.addRunwayVisualRange(rvr);

        List<RunwayVisualRange> rvrList = data.getRunwayVisualRange();
        assertThat(rvrList).hasSize(1);
        assertThat(rvrList.get(0)).isEqualTo(rvr);
    }

    @Test
    @DisplayName("Should add multiple runway visual ranges")
    void testAddMultipleRunwayVisualRanges() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        RunwayVisualRange rvr1 = RunwayVisualRange.of("04L", 2200);
        RunwayVisualRange rvr2 = RunwayVisualRange.variable("04R", 1800, 2400);

        data.addRunwayVisualRange(rvr1);
        data.addRunwayVisualRange(rvr2);

        List<RunwayVisualRange> rvrList = data.getRunwayVisualRange();
        assertThat(rvrList).hasSize(2);
    }

    @Test
    @DisplayName("Should not add null runway visual range")
    void testAddNullRunwayVisualRange() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        data.addRunwayVisualRange(null);

        assertThat(data.getRunwayVisualRange()).isEmpty();
    }

    @Test
    @DisplayName("Should get runway visual range as immutable copy")
    void testGetRunwayVisualRangeImmutable() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        RunwayVisualRange rvr = RunwayVisualRange.of("04L", 2200);
        data.addRunwayVisualRange(rvr);

        List<RunwayVisualRange> rvrList = data.getRunwayVisualRange();

        // Verify it's immutable
        RunwayVisualRange additionalRvr = RunwayVisualRange.of("22L", 3000);
        assertThrows(UnsupportedOperationException.class, () ->
                rvrList.add(additionalRvr)
        );
    }

    @Test
    @DisplayName("Should set runway visual range list")
    void testSetRunwayVisualRange() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        List<RunwayVisualRange> rvrList = List.of(
                RunwayVisualRange.of("04L", 2200),
                RunwayVisualRange.variable("04R", 1800, 2400)
        );

        data.setRunwayVisualRange(rvrList);

        assertThat(data.getRunwayVisualRange()).hasSize(2);
    }

    @Test
    @DisplayName("Should set runway visual range to null")
    void testSetRunwayVisualRangeNull() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));

        data.setRunwayVisualRange(null);

        assertThat(data.getRunwayVisualRange()).isEmpty();
    }

    // ========== UTILITY METHODS TESTS ==========

    @Test
    @DisplayName("Should get ceiling feet from conditions")
    void testGetCeilingFeet() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        WeatherConditions conditions = WeatherConditions.builder()
                .skyConditions(List.of(
                        SkyCondition.of(SkyCoverage.FEW, 10000),
                        SkyCondition.of(SkyCoverage.BROKEN, 5000),
                        SkyCondition.of(SkyCoverage.OVERCAST, 8000)
                ))
                .build();
        data.setConditions(conditions);

        assertThat(data.getCeilingFeet()).isEqualTo(5000);
    }

    @Test
    @DisplayName("Should return null ceiling when conditions is null")
    void testGetCeilingFeet_NullConditions() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        data.setConditions(null);

        assertThat(data.getCeilingFeet()).isNull();
    }

    @Test
    @DisplayName("Should check if has flight category data")
    void testHasFlightCategoryData_True() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        WeatherConditions conditions = WeatherConditions.builder()
                .visibility(Visibility.statuteMiles(10.0))
                .skyConditions(List.of(SkyCondition.of(SkyCoverage.FEW, 25000)))
                .build();
        data.setConditions(conditions);

        assertThat(data.hasFlightCategoryData()).isTrue();
    }

    @Test
    @DisplayName("Should return false when no visibility")
    void testHasFlightCategoryData_NoVisibility() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        WeatherConditions conditions = WeatherConditions.builder()
                .skyConditions(List.of(SkyCondition.of(SkyCoverage.FEW, 25000)))
                .build();
        data.setConditions(conditions);

        assertThat(data.hasFlightCategoryData()).isFalse();
    }

    @Test
    @DisplayName("Should return false when no sky conditions")
    void testHasFlightCategoryData_NoSkyConditions() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        WeatherConditions conditions = WeatherConditions.builder()
                .visibility(Visibility.statuteMiles(10.0))
                .build();
        data.setConditions(conditions);

        assertThat(data.hasFlightCategoryData()).isFalse();
    }

    @Test
    @DisplayName("Should get minimum RVR feet")
    void testGetMinimumRvrFeet() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        data.addRunwayVisualRange(RunwayVisualRange.of("04R", 1800));
        data.addRunwayVisualRange(RunwayVisualRange.of("22L", 3000));

        assertThat(data.getMinimumRvrFeet()).isEqualTo(1800);
    }

    @Test
    @DisplayName("Should get RVR for specific runway")
    void testGetRvrForRunway() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        RunwayVisualRange rvr04L = RunwayVisualRange.of("04L", 2200);

        data.addRunwayVisualRange(rvr04L);
        data.addRunwayVisualRange(RunwayVisualRange.of("04R", 1800));

        assertThat(data.getRvrForRunway("04L")).isEqualTo(rvr04L);
    }

    // ========== REMARKS TESTS ==========

    @Test
    @DisplayName("Should get and set remarks")
    void testGetSetRemarks() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .build();

        data.setRemarks(remarks);

        assertEquals(remarks, data.getRemarks());
    }

    @Test
    @DisplayName("Should return null for remarks by default")
    void testGetRemarks_DefaultNull() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertNull(data.getRemarks());
    }

    // ========== ISCURRENT TESTS ==========

    @Test
    @DisplayName("Should return true when observation time is recent")
    void testIsCurrent_Recent() {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        NoaaWeatherData data = new NoaaWeatherData("KJFK", oneHourAgo, "METAR");

        assertTrue(data.isCurrent());
    }

    @Test
    @DisplayName("Should return false when observation time is old")
    void testIsCurrent_Old() {
        Instant threeHoursAgo = Instant.now().minus(3, ChronoUnit.HOURS);
        NoaaWeatherData data = new NoaaWeatherData("KJFK", threeHoursAgo, "METAR");

        assertFalse(data.isCurrent());
    }

    @Test
    @DisplayName("Should return false when observation time is null")
    void testIsCurrent_Null() {
        NoaaWeatherData data = new NoaaWeatherData();
        data.setObservationTime(null);

        assertFalse(data.isCurrent());
    }

    // ========== GETDATATYPE TESTS ==========

    @Test
    @DisplayName("Should return report type when not null")
    void testGetDataType_WithReportType() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertEquals("METAR", data.getDataType());
    }

    @Test
    @DisplayName("Should return NOAA when report type is null")
    void testGetDataType_NullReportType() {
        NoaaWeatherData data = new NoaaWeatherData();
        data.setReportType(null);

        assertEquals("NOAA", data.getDataType());
    }

    // ========== GETSUMMARY TESTS ==========

    @Test
    @DisplayName("Should generate summary with report type")
    void testGetSummary_WithReportType() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        String summary = data.getSummary();

        assertNotNull(summary);
        assertTrue(summary.contains("METAR"));
        assertTrue(summary.contains("KJFK"));
    }

    @Test
    @DisplayName("Should generate summary with conditions")
    void testGetSummary_WithConditions() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        WeatherConditions conditions = WeatherConditions.builder()
                .wind(Wind.of(280, 16, "KT"))
                .visibility(Visibility.statuteMiles(10.0))
                .temperature(Temperature.ofCurrent(22.0, 12.0))
                .build();
        data.setConditions(conditions);

        String summary = data.getSummary();

        assertNotNull(summary);
        assertTrue(summary.contains("METAR"));
        assertTrue(summary.contains("KJFK"));
        assertTrue(summary.contains("Wind:"));
        assertTrue(summary.contains("Vis:"));
        assertTrue(summary.contains("Temp:"));
    }

    // ========== EQUALS TESTS ==========

    @Test
    @DisplayName("Should return false when comparing with different class")
    void testEquals_DifferentClass() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        assertThat(data)
                .isNotEqualTo("Not a NoaaWeatherData")
                .isNotEqualTo(null)
                .isNotEqualTo(new Object());
    }

    // ========== HASHCODE TESTS ==========

    @Test
    @DisplayName("Should have consistent hashCode")
    void testHashCode_Consistency() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        int hash1 = data.hashCode();
        int hash2 = data.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should generate toString with all fields")
    void testToString_Complete() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

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

        assertThat(toString)
                .contains("NoaaWeatherData")
                .contains("station=KJFK")
                .contains("type=METAR")
                .contains("wind=Wind")
                .contains("vis=Visibility")
                .contains("temp=Temperature")
                .contains("pressure=Pressure")
                .contains("skyCond=1")
                .contains("rvr=1");
    }

    @Test
    @DisplayName("Should generate toString with minimal fields")
    void testToString_Minimal() {
        NoaaWeatherData data = new NoaaWeatherData("KLGA", now, "TAF");

        String toString = data.toString();

        assertThat(toString)
                .contains("NoaaWeatherData")
                .contains("station=KLGA")
                .contains("type=TAF")
                .contains("wind=null")
                .contains("vis=null")
                .contains("temp=null")
                .contains("pressure=null")
                .contains("skyCond=0")
                .contains("rvr=0");
    }

    @Test
    @DisplayName("Should generate toString with partial conditions")
    void testToString_PartialConditions() {
        NoaaWeatherData data = new NoaaWeatherData("KORD", now, "METAR");

        // Only set wind and visibility, no temperature or pressure
        WeatherConditions conditions = WeatherConditions.builder()
                .wind(Wind.of(350, 8, "KT"))
                .visibility(Visibility.meters(9999))
                .build();
        data.setConditions(conditions);

        String toString = data.toString();

        assertThat(toString)
                .contains("station=KORD")
                .contains("wind=Wind")
                .contains("vis=Visibility")
                .contains("temp=null")
                .contains("pressure=null");
    }

    @Test
    @DisplayName("Should change hashCode when conditions are set")
    void testHashCode_WithConditions() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        int hashBefore = data.hashCode();

        WeatherConditions conditions = WeatherConditions.builder()
                .wind(Wind.of(280, 16, "KT"))
                .build();
        data.setConditions(conditions);

        int hashAfter = data.hashCode();

        assertNotEquals(hashBefore, hashAfter,
                "HashCode should change when conditions are set");
    }

    // ========== CAVOK VISIBILITY TESTS ==========

    @Test
    @DisplayName("Should create CAVOK visibility")
    void testCreateCavokVisibility() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        // Access protected method through test subclass
        Visibility cavok = data.createCavokVisibility();

        assertThat(cavok).isNotNull();
        assertThat(cavok.isCavok()).isTrue();
    }

    @Test
    @DisplayName("Should create visibility with correct CAVOK properties")
    void testCavokVisibilityProperties() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        Visibility cavok = data.createCavokVisibility();

        assertThat(cavok.isCavok()).isTrue();
        // CAVOK implies visibility >= 10km (approximately 6+ statute miles)
        assertThat(cavok.specialCondition()).isEqualTo("CAVOK");
    }

    @Test
    @DisplayName("Should use CAVOK visibility in weather conditions")
    void testSetCavokInConditions() {
        NoaaWeatherData data = new NoaaWeatherData("KJFK", now, "METAR");

        Visibility cavok = data.createCavokVisibility();

        WeatherConditions conditions = WeatherConditions.builder()
                .visibility(cavok)
                .build();

        data.setConditions(conditions);

        assertThat(data.getVisibility()).isNotNull();
        assertThat(data.getVisibility().isCavok()).isTrue();
    }
}
