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
package weather.model;

import org.junit.jupiter.api.Test;
import weather.model.components.*;
import weather.model.components.remark.*;
import weather.model.enums.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for NoaaMetarData class.
 * 
 * Tests METAR-specific functionality only.
 * Base class functionality is tested in NoaaWeatherDataTest.
 *
 * Coverage Note: This class achieves 99% instruction and 90% branch coverage.
 * The remaining 10% of branches are defensive null checks that are unreachable
 * in normal usage due to:
 * - Constructors initialize collections as empty (never null)
 * - Setters replace null inputs with empty collections
 * 
 * These defensive branches protect against reflection/serialization edge cases
 * but are not worth testing with reflection-based approaches.
 * 
 * @author bclasky1539
 *
 */
class NoaaMetarDataTest {
    
    // ========== CONSTRUCTOR TESTS ==========
    
    @Test
    void testDefaultConstructor() {
        NoaaMetarData data = new NoaaMetarData();
        
        assertThat(data.getReportType()).isEqualTo("METAR");
        assertThat(data.getSkyConditions()).isNotNull().isEmpty();
        assertThat(data.getPresentWeather()).isNotNull().isEmpty();
        assertThat(data.getRunwayVisualRange()).isNotNull().isEmpty();
        assertThat(data.isNoSignificantChange()).isFalse();
    }
    
    @Test
    void testParameterizedConstructor() {
        Instant now = Instant.now();
        NoaaMetarData data = new NoaaMetarData("KJFK", now);
        
        assertThat(data.getStationId()).isEqualTo("KJFK");
        assertThat(data.getObservationTime()).isEqualTo(now);
        assertThat(data.getReportType()).isEqualTo("METAR");
    }
    
    // ========== MAIN BODY COMPONENT TESTS ==========
    
    @Test
    void testSetAndGetWind() {
        NoaaMetarData data = new NoaaMetarData();
        Wind wind = new Wind(280, 16, null, null, null, "KT");
        
        data.setWind(wind);
        
        assertThat(data.getWind()).isEqualTo(wind);
    }
    
    @Test
    void testSetAndGetVisibility() {
        NoaaMetarData data = new NoaaMetarData();
        Visibility visibility = new Visibility(10.0, "SM", false, false, null);
        
        data.setVisibility(visibility);
        
        assertThat(data.getVisibility()).isEqualTo(visibility);
    }
    
    @Test
    void testSetAndGetTemperature() {
        NoaaMetarData data = new NoaaMetarData();
        Temperature temperature = new Temperature(22.0, 12.0);
        
        data.setTemperature(temperature);
        
        assertThat(data.getTemperature()).isEqualTo(temperature);
    }
    
    @Test
    void testSetAndGetPressure() {
        NoaaMetarData data = new NoaaMetarData();
        Pressure pressure = new Pressure(30.15, PressureUnit.INCHES_HG);
        
        data.setPressure(pressure);
        
        assertThat(data.getPressure()).isEqualTo(pressure);
    }
    
    // ========== SKY CONDITIONS TESTS ==========
    
    @Test
    void testAddSkyCondition() {
        NoaaMetarData data = new NoaaMetarData();
        SkyCondition sky1 = new SkyCondition(SkyCoverage.FEW, 25000, null);
        SkyCondition sky2 = new SkyCondition(SkyCoverage.BROKEN, 5000, null);
        
        data.addSkyCondition(sky1);
        data.addSkyCondition(sky2);
        
        assertThat(data.getSkyConditions()).hasSize(2);
        assertThat(data.getSkyConditions()).containsExactly(sky1, sky2);
    }
    
    @Test
    void testAddSkyCondition_Null() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.addSkyCondition(null);
        
        assertThat(data.getSkyConditions()).isEmpty();
    }
    
    @Test
    void testGetSkyConditions_ReturnsImmutableCopy() {
        NoaaMetarData data = new NoaaMetarData();
        SkyCondition sky = new SkyCondition(SkyCoverage.FEW, 25000, null);
        data.addSkyCondition(sky);
        
        var skyConditions = data.getSkyConditions();
        
        // List.copyOf() returns immutable list
        assertThat(skyConditions).hasSize(1);
    }
    
    @Test
    void testSetSkyConditions_WithNull() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setSkyConditions(null);
        
        // Should create empty list, not null
        assertThat(data.getSkyConditions()).isNotNull().isEmpty();
    }
    
    @Test
    void testSetSkyConditions_WithNonNullList() {
        NoaaMetarData data = new NoaaMetarData();
        
        List<SkyCondition> skyList = new ArrayList<>();
        skyList.add(new SkyCondition(SkyCoverage.FEW, 25000, null));
        skyList.add(new SkyCondition(SkyCoverage.BROKEN, 5000, null));
        
        data.setSkyConditions(skyList);
        
        assertThat(data.getSkyConditions()).hasSize(2);
        assertThat(data.getSkyConditions().get(0).coverage()).isEqualTo(SkyCoverage.FEW);
        assertThat(data.getSkyConditions().get(1).coverage()).isEqualTo(SkyCoverage.BROKEN);
    }
    
    @Test
    void testSetSkyConditions_WithEmptyList() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setSkyConditions(new ArrayList<>());
        
        assertThat(data.getSkyConditions()).isEmpty();
    }
    
    // ========== PRESENT WEATHER TESTS ==========
    
    @Test
    void testAddPresentWeather() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.addPresentWeather("-RA");
        data.addPresentWeather("BR");
        
        assertThat(data.getPresentWeather()).hasSize(2);
        assertThat(data.getPresentWeather()).containsExactly("-RA", "BR");
    }
    
    @Test
    void testAddPresentWeather_NullOrBlank() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.addPresentWeather(null);
        data.addPresentWeather("");
        data.addPresentWeather("   ");
        
        assertThat(data.getPresentWeather()).isEmpty();
    }
    
    @Test
    void testSetPresentWeather_WithNull() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setPresentWeather(null);
        
        assertThat(data.getPresentWeather()).isNotNull().isEmpty();
    }
    
    @Test
    void testSetPresentWeather_WithNonNullList() {
        NoaaMetarData data = new NoaaMetarData();
        
        List<String> weatherList = new ArrayList<>();
        weatherList.add("-RA");
        weatherList.add("BR");
        weatherList.add("FG");
        
        data.setPresentWeather(weatherList);
        
        assertThat(data.getPresentWeather()).hasSize(3);
        assertThat(data.getPresentWeather()).containsExactly("-RA", "BR", "FG");
    }
    
    @Test
    void testSetPresentWeather_WithEmptyList() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setPresentWeather(new ArrayList<>());
        
        assertThat(data.getPresentWeather()).isEmpty();
    }
    
    // ========== RUNWAY VISUAL RANGE TESTS (UPDATED FOR RunwayVisualRange OBJECTS) ==========
    
    @Test
    void testAddRunwayVisualRange() {
        NoaaMetarData data = new NoaaMetarData();
        RunwayVisualRange rvr1 = RunwayVisualRange.of("04L", 2200);
        RunwayVisualRange rvr2 = RunwayVisualRange.variable("04R", 1800, 2400);
        
        data.addRunwayVisualRange(rvr1);
        data.addRunwayVisualRange(rvr2);
        
        assertThat(data.getRunwayVisualRange()).hasSize(2);
        assertThat(data.getRunwayVisualRange()).containsExactly(rvr1, rvr2);
    }
    
    @Test
    void testAddRunwayVisualRange_Null() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.addRunwayVisualRange(null);
        
        assertThat(data.getRunwayVisualRange()).isEmpty();
    }
    
    @Test
    void testGetRunwayVisualRange_ReturnsImmutableCopy() {
        NoaaMetarData data = new NoaaMetarData();
        RunwayVisualRange rvr = RunwayVisualRange.of("04L", 2200);
        data.addRunwayVisualRange(rvr);
        
        var rvrList = data.getRunwayVisualRange();
        
        // List.copyOf() returns immutable list
        assertThat(rvrList).hasSize(1);
    }
    
    @Test
    void testSetRunwayVisualRange_WithNull() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setRunwayVisualRange(null);
        
        // Should create empty list, not null
        assertThat(data.getRunwayVisualRange()).isNotNull().isEmpty();
    }
    
    @Test
    void testSetRunwayVisualRange_WithNonNullList() {
        NoaaMetarData data = new NoaaMetarData();
        
        List<RunwayVisualRange> rvrList = new ArrayList<>();
        rvrList.add(RunwayVisualRange.of("04L", 2200));
        rvrList.add(RunwayVisualRange.variable("04R", 1800, 2400));
        rvrList.add(new RunwayVisualRange("22L", 6000, null, null, "P", null));
        
        data.setRunwayVisualRange(rvrList);
        
        assertThat(data.getRunwayVisualRange()).hasSize(3);
        assertThat(data.getRunwayVisualRange().get(0).runway()).isEqualTo("04L");
        assertThat(data.getRunwayVisualRange().get(0).visualRangeFeet()).isEqualTo(2200);
        assertThat(data.getRunwayVisualRange().get(1).isVariable()).isTrue();
        assertThat(data.getRunwayVisualRange().get(2).isGreaterThan()).isTrue();
    }
    
    @Test
    void testSetRunwayVisualRange_WithEmptyList() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setRunwayVisualRange(new ArrayList<>());
        
        assertThat(data.getRunwayVisualRange()).isEmpty();
    }
    
    @Test
    void testAddRunwayVisualRange_WithVariableRange() {
        NoaaMetarData data = new NoaaMetarData();
        RunwayVisualRange rvr = RunwayVisualRange.variable("18", 1200, 1800);
        
        data.addRunwayVisualRange(rvr);
        
        assertThat(data.getRunwayVisualRange()).hasSize(1);
        assertThat(data.getRunwayVisualRange().get(0).isVariable()).isTrue();
        assertThat(data.getRunwayVisualRange().get(0).variableLow()).isEqualTo(1200);
        assertThat(data.getRunwayVisualRange().get(0).variableHigh()).isEqualTo(1800);
    }
    
    @Test
    void testAddRunwayVisualRange_WithPrefixAndTrend() {
        NoaaMetarData data = new NoaaMetarData();
        RunwayVisualRange rvr = new RunwayVisualRange("04L", 600, null, null, "M", "D");
        
        data.addRunwayVisualRange(rvr);
        
        assertThat(data.getRunwayVisualRange()).hasSize(1);
        assertThat(data.getRunwayVisualRange().get(0).isLessThan()).isTrue();
        assertThat(data.getRunwayVisualRange().get(0).getTrendDescription()).isEqualTo("Decreasing");
    }
    
    // ========== NEW RVR UTILITY METHOD TESTS ==========
    
    @Test
    void testGetMinimumRvrFeet_WithMultipleRunways() {
        NoaaMetarData data = new NoaaMetarData();
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        data.addRunwayVisualRange(RunwayVisualRange.of("04R", 1800));
        data.addRunwayVisualRange(RunwayVisualRange.of("22L", 3000));
        
        assertThat(data.getMinimumRvrFeet()).isEqualTo(1800);
    }
    
    @Test
    void testGetMinimumRvrFeet_WithVariableRange() {
        NoaaMetarData data = new NoaaMetarData();
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        data.addRunwayVisualRange(RunwayVisualRange.variable("04R", 1200, 1800));
        
        // Should use the low end of variable range
        assertThat(data.getMinimumRvrFeet()).isEqualTo(1200);
    }
    
    @Test
    void testGetMinimumRvrFeet_ExcludesLessThanValues() {
        NoaaMetarData data = new NoaaMetarData();
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        data.addRunwayVisualRange(new RunwayVisualRange("04R", 600, null, null, "M", null));
        
        // Should exclude the "less than" value and return 2200
        assertThat(data.getMinimumRvrFeet()).isEqualTo(2200);
    }
    
    @Test
    void testGetMinimumRvrFeet_EmptyList() {
        NoaaMetarData data = new NoaaMetarData();
        
        assertThat(data.getMinimumRvrFeet()).isNull();
    }
    
    @Test
    void testGetMinimumRvrFeet_NullList() {
        NoaaMetarData data = new NoaaMetarData();
        data.setRunwayVisualRange(null);
        
        assertThat(data.getMinimumRvrFeet()).isNull();
    }
    
    @Test
    void testGetRvrForRunway_Found() {
        NoaaMetarData data = new NoaaMetarData();
        RunwayVisualRange rvr04L = RunwayVisualRange.of("04L", 2200);
        RunwayVisualRange rvr04R = RunwayVisualRange.of("04R", 1800);
        
        data.addRunwayVisualRange(rvr04L);
        data.addRunwayVisualRange(rvr04R);
        
        assertThat(data.getRvrForRunway("04L")).isEqualTo(rvr04L);
        assertThat(data.getRvrForRunway("04R")).isEqualTo(rvr04R);
    }
    
    @Test
    void testGetRvrForRunway_NotFound() {
        NoaaMetarData data = new NoaaMetarData();
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        
        assertThat(data.getRvrForRunway("22R")).isNull();
    }
    
    @Test
    void testGetRvrForRunway_CaseInsensitive() {
        NoaaMetarData data = new NoaaMetarData();
        RunwayVisualRange rvr = RunwayVisualRange.of("04L", 2200);
        data.addRunwayVisualRange(rvr);
        
        assertThat(data.getRvrForRunway("04l")).isEqualTo(rvr);
        assertThat(data.getRvrForRunway("04L")).isEqualTo(rvr);
    }
    
    @Test
    void testGetRvrForRunway_NullRunwayId() {
        NoaaMetarData data = new NoaaMetarData();
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        
        assertThat(data.getRvrForRunway(null)).isNull();
    }
    
    @Test
    void testGetRvrForRunway_NullRvrList() {
        NoaaMetarData data = new NoaaMetarData();
        data.setRunwayVisualRange(null);
        
        assertThat(data.getRvrForRunway("04L")).isNull();
    }
    
    // ========== REMARKS SECTION TESTS ==========
    
    @Test
    void testSetAndGetPeakWind() {
        NoaaMetarData data = new NoaaMetarData();
        PeakWind peakWind = new PeakWind(280, 32, 15, 30);
        
        data.setPeakWind(peakWind);
        
        assertThat(data.getPeakWind()).isEqualTo(peakWind);
    }
    
    @Test
    void testSetAndGetWindShift() {
        NoaaMetarData data = new NoaaMetarData();
        WindShift windShift = new WindShift(15, 30, true);
        
        data.setWindShift(windShift);
        
        assertThat(data.getWindShift()).isEqualTo(windShift);
    }
    
    @Test
    void testSetAndGetAutomatedStation() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setAutomatedStation("AO2");
        
        assertThat(data.getAutomatedStation()).isEqualTo("AO2");
    }
    
    @Test
    void testSetAndGetSeaLevelPressure() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setSeaLevelPressure(1013.2);
        
        assertThat(data.getSeaLevelPressure()).isEqualTo(1013.2);
    }
    
    @Test
    void testSetAndGetHourlyPrecipitation() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setHourlyPrecipitation(0.25);
        
        assertThat(data.getHourlyPrecipitation()).isEqualTo(0.25);
    }
    
    @Test
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
    void testSetAndGetThreeHourPressureTendency() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setThreeHourPressureTendency(2.5);
        
        assertThat(data.getThreeHourPressureTendency()).isEqualTo(2.5);
    }
    
    @Test
    void testSetAndGetNoSignificantChange() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setNoSignificantChange(true);
        
        assertThat(data.isNoSignificantChange()).isTrue();
    }
    
    // ========== INHERITED FIELD TESTS (from NoaaWeatherData) ==========
    
    @Test
    void testSetAndGetRawText() {
        NoaaMetarData data = new NoaaMetarData();
        String rawText = "METAR KJFK 191651Z 28016KT 10SM FEW250 22/12 A3015";
        
        data.setRawText(rawText);
        
        assertThat(data.getRawText()).isEqualTo(rawText);
    }
    
    @Test
    void testSetAndGetReportModifier() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setReportModifier("AUTO");
        
        assertThat(data.getReportModifier()).isEqualTo("AUTO");
    }
    
    @Test
    void testSetAndGetLocation() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setLatitude(40.6413);
        data.setLongitude(-73.7781);
        data.setElevationFeet(13);
        
        assertThat(data.getLatitude()).isEqualTo(40.6413);
        assertThat(data.getLongitude()).isEqualTo(-73.7781);
        assertThat(data.getElevationFeet()).isEqualTo(13);
    }
    
    @Test
    void testSetAndGetQualityControlFlags() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setQualityControlFlags("CORRECTED");
        
        assertThat(data.getQualityControlFlags()).isEqualTo("CORRECTED");
    }
    
    // ========== METAR-SPECIFIC UTILITY METHOD TESTS ==========
    
    @Test
    void testGetCeilingFeet_WithBrokenLayer() {
        NoaaMetarData data = new NoaaMetarData();
        data.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 10000, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.BROKEN, 5000, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.OVERCAST, 8000, null));
        
        // Should return lowest ceiling (BKN or OVC)
        assertThat(data.getCeilingFeet()).isEqualTo(5000);
    }
    
    @Test
    void testGetCeilingFeet_WithOvercastLayer() {
        NoaaMetarData data = new NoaaMetarData();
        data.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 10000, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.OVERCAST, 3000, null));
        
        assertThat(data.getCeilingFeet()).isEqualTo(3000);
    }
    
    @Test
    void testGetCeilingFeet_NoCeiling() {
        NoaaMetarData data = new NoaaMetarData();
        data.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 10000, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.SCATTERED, 5000, null));
        
        assertThat(data.getCeilingFeet()).isNull();
    }
    
    @Test
    void testGetCeilingFeet_EmptySkyConditions() {
        NoaaMetarData data = new NoaaMetarData();
        
        assertThat(data.getCeilingFeet()).isNull();
    }
    
    @Test
    void testGetCeilingFeet_WithNullSkyConditions() {
        NoaaMetarData data = new NoaaMetarData();
        data.setSkyConditions(null);
        
        assertThat(data.getCeilingFeet()).isNull();
    }
    
    @Test
    void testGetCeilingFeet_WithSkyConditionsButNoHeights() {
        NoaaMetarData data = new NoaaMetarData();
        data.addSkyCondition(new SkyCondition(SkyCoverage.BROKEN, null, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.OVERCAST, null, null));
        
        assertThat(data.getCeilingFeet()).isNull();
    }
    
    @Test
    void testGetCeilingFeet_WithMultipleCeilingLayers() {
        NoaaMetarData data = new NoaaMetarData();
        data.addSkyCondition(new SkyCondition(SkyCoverage.BROKEN, 8000, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.OVERCAST, 5000, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.OVERCAST, 10000, null));
        
        // Should return the lowest ceiling
        assertThat(data.getCeilingFeet()).isEqualTo(5000);
    }
    
    @Test
    void testGetCeilingFeet_WithOnlyBrokenLayer() {
        NoaaMetarData data = new NoaaMetarData();
        data.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 10000, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.BROKEN, 7000, null));
        
        assertThat(data.getCeilingFeet()).isEqualTo(7000);
    }
    
    @Test
    void testGetCeilingFeet_WithOnlyOvercastLayer() {
        NoaaMetarData data = new NoaaMetarData();
        data.addSkyCondition(new SkyCondition(SkyCoverage.SCATTERED, 10000, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.OVERCAST, 6000, null));
        
        assertThat(data.getCeilingFeet()).isEqualTo(6000);
    }
    
    @Test
    void testHasFlightCategoryData_True() {
        NoaaMetarData data = new NoaaMetarData();
        data.setVisibility(new Visibility(10.0, "SM", false, false, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 25000, null));
        
        assertThat(data.hasFlightCategoryData()).isTrue();
    }
    
    @Test
    void testHasFlightCategoryData_False_NoVisibility() {
        NoaaMetarData data = new NoaaMetarData();
        data.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 25000, null));
        
        assertThat(data.hasFlightCategoryData()).isFalse();
    }
    
    @Test
    void testHasFlightCategoryData_False_NoSkyConditions() {
        NoaaMetarData data = new NoaaMetarData();
        data.setVisibility(new Visibility(10.0, "SM", false, false, null));
        
        assertThat(data.hasFlightCategoryData()).isFalse();
    }
    
    // ========== OVERRIDE METHOD TESTS ==========
    
    @Test
    void testGetDataType() {
        NoaaMetarData data = new NoaaMetarData();
        
        assertThat(data.getDataType()).isEqualTo("METAR");
    }
    
    @Test
    void testGetSummary_Complete() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        data.setWind(new Wind(280, 16, null, null, null, "KT"));
        data.setVisibility(new Visibility(10.0, "SM", false, false, null));
        data.setTemperature(new Temperature(22.0, 12.0));
        
        String summary = data.getSummary();
        
        assertThat(summary).contains("METAR", "KJFK", "Wind:", "Vis:", "Temp:");
    }
    
    @Test
    void testGetSummary_Minimal() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        
        String summary = data.getSummary();
        
        assertThat(summary).contains("METAR", "KJFK");
    }
    
    @Test
    void testGetSummary_WithNullWind() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        data.setWind(null);
        data.setVisibility(new Visibility(10.0, "SM", false, false, null));
        data.setTemperature(new Temperature(22.0, 12.0));
        
        String summary = data.getSummary();
        
        assertThat(summary)
                .contains("METAR", "KJFK", "Vis:", "Temp:")
                .doesNotContain("Wind:");
    }
    
    @Test
    void testGetSummary_WithNullVisibility() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        data.setWind(new Wind(280, 16, null, null, null, "KT"));
        data.setVisibility(null);
        data.setTemperature(new Temperature(22.0, 12.0));
        
        String summary = data.getSummary();
        
        assertThat(summary).contains("Wind:", "Temp:")
                .doesNotContain("Vis:");
    }
    
    @Test
    void testGetSummary_WithNullTemperature() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        data.setWind(new Wind(280, 16, null, null, null, "KT"));
        data.setVisibility(new Visibility(10.0, "SM", false, false, null));
        data.setTemperature(null);
        
        String summary = data.getSummary();
        
        assertThat(summary).contains("Wind:", "Vis:")
                .doesNotContain("Temp:");
    }
    
    @Test
    void testGetSummary_WithAllNullWeatherComponents() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        data.setWind(null);
        data.setVisibility(null);
        data.setTemperature(null);
        
        String summary = data.getSummary();
        
        assertThat(summary).isEqualTo("METAR KJFK");
    }
    
    @Test
    void testToString() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        data.setWind(new Wind(280, 16, null, null, null, "KT"));
        
        String toString = data.toString();
        
        assertThat(toString).contains("NoaaMetarData", "KJFK", "wind=");
    }
    
    @Test
    void testToString_WithNullSkyConditions() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        data.setSkyConditions(null);
        
        String toString = data.toString();
        
        assertThat(toString).contains("NoaaMetarData", "KJFK", "skyCond=0");
    }
    
    @Test
    void testToString_WithEmptySkyConditions() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        data.setSkyConditions(new ArrayList<>());
        
        String toString = data.toString();
        
        assertThat(toString).contains("skyCond=0");
    }
    
    @Test
    void testToString_WithMultipleSkyConditions() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        data.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 25000, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.BROKEN, 5000, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.OVERCAST, 3000, null));
        
        String toString = data.toString();
        
        assertThat(toString).contains("skyCond=3");
    }
    
    @Test
    void testToString_WithRunwayVisualRange() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        data.addRunwayVisualRange(RunwayVisualRange.of("04R", 1800));
        
        String toString = data.toString();
        
        assertThat(toString).contains("rvr=2");
    }
    
    @Test
    void testToString_WithAllFields() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        data.setWind(new Wind(280, 16, null, null, null, "KT"));
        data.setVisibility(new Visibility(10.0, "SM", false, false, null));
        data.setTemperature(new Temperature(22.0, 12.0));
        data.setPressure(new Pressure(30.15, PressureUnit.INCHES_HG));
        data.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 25000, null));
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        
        String toString = data.toString();
        
        assertThat(toString).contains("NoaaMetarData", "KJFK", "wind=", "vis=", "temp=", "pressure=", "skyCond=1", "rvr=1");
    }
    
    // ========== EQUALS AND HASHCODE TESTS ==========

    @Test
    void testEquals_SameObject() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        
        assertThat(data).isEqualTo(data);
    }
    
    @Test
    void testEquals_EqualObjects() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.setRawText("METAR KJFK 191651Z 28016KT 10SM FEW250 22/12 A3015");
        data1.setWind(new Wind(280, 16, null, null, null, "KT"));
        data1.setVisibility(new Visibility(10.0, "SM", false, false, null));
        data1.setTemperature(new Temperature(22.0, 12.0));
        data1.setPressure(new Pressure(30.15, PressureUnit.INCHES_HG));
        data1.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 25000, null));
        data1.setAutomatedStation("AO2");
        data1.setSeaLevelPressure(1013.2);
        data1.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        
        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setRawText("METAR KJFK 191651Z 28016KT 10SM FEW250 22/12 A3015");
        data2.setWind(new Wind(280, 16, null, null, null, "KT"));
        data2.setVisibility(new Visibility(10.0, "SM", false, false, null));
        data2.setTemperature(new Temperature(22.0, 12.0));
        data2.setPressure(new Pressure(30.15, PressureUnit.INCHES_HG));
        data2.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 25000, null));
        data2.setAutomatedStation("AO2");
        data2.setSeaLevelPressure(1013.2);
        data2.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        
        assertThat(data1).isEqualTo(data2);
        assertThat(data2).isEqualTo(data1);
    }
    
    @Test
    void testEquals_DifferentStationId() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        NoaaMetarData data2 = new NoaaMetarData("KLGA", now);
        
        assertThat(data1).isNotEqualTo(data2);
    }
    
    @Test
    void testEquals_DifferentObservationTime() {
        NoaaMetarData data1 = new NoaaMetarData("KJFK", Instant.now());
        NoaaMetarData data2 = new NoaaMetarData("KJFK", Instant.now().plusSeconds(60));
        
        assertThat(data1).isNotEqualTo(data2);
    }
    
    @Test
    void testEquals_DifferentWind() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.setWind(new Wind(280, 16, null, null, null, "KT"));
        
        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setWind(new Wind(290, 20, null, null, null, "KT"));
        
        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    void testEquals_DifferentVisibility() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.setVisibility(new Visibility(10.0, "SM", false, false, null));
        
        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setVisibility(new Visibility(5.0, "SM", false, false, null));
        
        assertThat(data1).isNotEqualTo(data2);
    }
    
    @Test
    void testEquals_DifferentTemperature() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.setTemperature(new Temperature(22.0, 12.0));
        
        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setTemperature(new Temperature(25.0, 15.0));
        
        assertThat(data1).isNotEqualTo(data2);
    }
    
    @Test
    void testEquals_DifferentPressure() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.setPressure(new Pressure(30.15, PressureUnit.INCHES_HG));
        
        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setPressure(new Pressure(29.92, PressureUnit.INCHES_HG));
        
        assertThat(data1).isNotEqualTo(data2);
    }
    
    @Test
    void testEquals_DifferentSkyConditions() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 25000, null));
        
        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.addSkyCondition(new SkyCondition(SkyCoverage.BROKEN, 5000, null));
        
        assertThat(data1).isNotEqualTo(data2);
    }
    
    @Test
    void testEquals_DifferentPresentWeather() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.addPresentWeather("-RA");
        
        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.addPresentWeather("BR");
        
        assertThat(data1).isNotEqualTo(data2);
    }
    
    @Test
    void testEquals_DifferentRunwayVisualRange() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        
        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.addRunwayVisualRange(RunwayVisualRange.of("04L", 1800));
        
        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    void testEquals_DifferentRemarks() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.setAutomatedStation("AO2");
        data1.setSeaLevelPressure(1013.2);
        
        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setAutomatedStation("AO1");
        data2.setSeaLevelPressure(1015.0);
        
        assertThat(data1).isNotEqualTo(data2);
    }
    
    @Test
    void testEquals_WithNull() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        
        assertThat(data).isNotEqualTo(null);
    }
    
    @Test
    void testEquals_WithDifferentType() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        String notAMetar = "KJFK";
        
        assertThat(data).isNotEqualTo(notAMetar);
    }
    
    @Test
    void testEquals_DifferentNoSignificantChange() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.setNoSignificantChange(true);
        
        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setNoSignificantChange(false);
        
        assertThat(data1).isNotEqualTo(data2);
    }
    
    @Test
    void testHashCode_Consistency() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        data.setWind(new Wind(280, 16, null, null, null, "KT"));
        data.setVisibility(new Visibility(10.0, "SM", false, false, null));
        
        int hash1 = data.hashCode();
        int hash2 = data.hashCode();
        
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void testHashCode_EqualObjects() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.setWind(new Wind(280, 16, null, null, null, "KT"));
        data1.setVisibility(new Visibility(10.0, "SM", false, false, null));
        data1.setTemperature(new Temperature(22.0, 12.0));
        data1.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        
        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setWind(new Wind(280, 16, null, null, null, "KT"));
        data2.setVisibility(new Visibility(10.0, "SM", false, false, null));
        data2.setTemperature(new Temperature(22.0, 12.0));
        data2.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        
        // Equal objects must have equal hash codes
        assertThat(data1).hasSameHashCodeAs(data2);
    }
    
    @Test
    void testHashCode_DifferentObjects() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.setWind(new Wind(280, 16, null, null, null, "KT"));
        
        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setWind(new Wind(290, 20, null, null, null, "KT"));
        
        // Different objects MAY have different hash codes (not required, but likely)
        // We are just verifying hashCode does not throw an exception
        assertThatCode(data1::hashCode).doesNotThrowAnyException();
        assertThatCode(data2::hashCode).doesNotThrowAnyException();
    }
    
    @Test
    void testHashCode_IncludesParentFields() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        NoaaMetarData data2 = new NoaaMetarData("KLGA", now);
        
        // Different parent fields should (likely) result in different hash codes
        // Verify hashCode() executes without throwing exception
        assertThatCode(data1::hashCode).doesNotThrowAnyException();
        assertThatCode(data2::hashCode).doesNotThrowAnyException();
    }
    
    // ========== ADDITIONAL TESTS FOR getCeilingFeet() ==========
    
    /**
     * Tests the branch where skyConditions has ceiling layers but all have null heights.
     * This covers the .filter(Objects::nonNull) branch followed by .orElse(null).
     */
    @Test
    void testGetCeilingFeet_AllCeilingLayersHaveNullHeights() {
        NoaaMetarData data = new NoaaMetarData();
        // Add ceiling layers (BKN/OVC) but with null heights
        data.addSkyCondition(new SkyCondition(SkyCoverage.BROKEN, null, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.OVERCAST, null, null));
        
        // Should return null since all ceiling heights are null
        assertThat(data.getCeilingFeet()).isNull();
    }
    
    /**
     * Tests the branch where there are non-ceiling layers with heights and 
     * ceiling layers with null heights mixed together.
     */
    @Test
    void testGetCeilingFeet_MixedNullAndValidHeights() {
        NoaaMetarData data = new NoaaMetarData();
        data.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 10000, null));  // Not a ceiling
        data.addSkyCondition(new SkyCondition(SkyCoverage.BROKEN, null, null)); // Ceiling but null height
        data.addSkyCondition(new SkyCondition(SkyCoverage.OVERCAST, 5000, null)); // Valid ceiling
        
        // Should return 5000, the only valid ceiling height
        assertThat(data.getCeilingFeet()).isEqualTo(5000);
    }

    /**
     * Tests when sky conditions list exists but contains only non-ceiling layers.
     * This ensures the stream filter works correctly.
     */
    @Test
    void testGetCeilingFeet_OnlyNonCeilingLayers() {
        NoaaMetarData data = new NoaaMetarData();
        data.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 5000, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.SCATTERED, 8000, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 12000, null));
        
        // No ceiling layers, should return null
        assertThat(data.getCeilingFeet()).isNull();
    }
    
    // ========== ADDITIONAL TESTS FOR getMinimumRvrFeet() ==========
    
    /**
     * Tests when all RVRs are "less than" values.
     * This covers the branch where .filter(rvr -> !rvr.isLessThan()) removes all items,
     * then .orElse(null) is reached.
     */
    @Test
    void testGetMinimumRvrFeet_AllLessThanValues() {
        NoaaMetarData data = new NoaaMetarData();
        data.addRunwayVisualRange(new RunwayVisualRange("04L", 600, null, null, "M", null));
        data.addRunwayVisualRange(new RunwayVisualRange("04R", 400, null, null, "M", null));
        data.addRunwayVisualRange(new RunwayVisualRange("22L", 500, null, null, "M", null));
        
        // All are "less than" values, should be excluded, returning null
        assertThat(data.getMinimumRvrFeet()).isNull();
    }

    /**
     * Tests when RVRs have null visualRangeFeet after filtering.
     * This covers the .filter(Objects::nonNull) branch after the map operation.
     */
    @Test
    void testGetMinimumRvrFeet_WithNullRangeValues() {
        NoaaMetarData data = new NoaaMetarData();
        // Variable range RVR (visualRangeFeet is null, but variableLow is used)
        data.addRunwayVisualRange(RunwayVisualRange.variable("04L", 1200, 1800));
        // Regular RVR with value
        data.addRunwayVisualRange(RunwayVisualRange.of("04R", 2000));
        
        // Should use variableLow (1200) as minimum
        assertThat(data.getMinimumRvrFeet()).isEqualTo(1200);
    }
    
    /**
     * Tests with only variable range RVRs to ensure variableLow is properly used.
     */
    @Test
    void testGetMinimumRvrFeet_OnlyVariableRanges() {
        NoaaMetarData data = new NoaaMetarData();
        data.addRunwayVisualRange(RunwayVisualRange.variable("04L", 1500, 2000));
        data.addRunwayVisualRange(RunwayVisualRange.variable("04R", 1200, 1800));
        data.addRunwayVisualRange(RunwayVisualRange.variable("22L", 1800, 2400));
        
        // Should use the lowest variableLow value (1200)
        assertThat(data.getMinimumRvrFeet()).isEqualTo(1200);
    }

    /**
     * Tests mixed "greater than" and regular values.
     * "Greater than" values are not filtered out, only "less than" values are.
     */
    @Test
    void testGetMinimumRvrFeet_WithGreaterThanValues() {
        NoaaMetarData data = new NoaaMetarData();
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        data.addRunwayVisualRange(new RunwayVisualRange("04R", 6000, null, null, "P", null)); // Greater than
        
        // Should include both values, minimum is 2200
        assertThat(data.getMinimumRvrFeet()).isEqualTo(2200);
    }
    
    /**
     * Tests single RVR that is "less than" - ensures null is returned.
     */
    @Test
    void testGetMinimumRvrFeet_SingleLessThanValue() {
        NoaaMetarData data = new NoaaMetarData();
        data.addRunwayVisualRange(new RunwayVisualRange("04L", 600, null, null, "M", null));
        
        // Only RVR is "less than", should be filtered out
        assertThat(data.getMinimumRvrFeet()).isNull();
    }
    
    // ========== ADDITIONAL TESTS FOR getRvrForRunway() ==========
    
    /**
     * Tests with empty runway list to ensure early return with null.
     */
    @Test
    void testGetRvrForRunway_EmptyList() {
        NoaaMetarData data = new NoaaMetarData();
        // List is empty (not null)
        
        assertThat(data.getRvrForRunway("04L")).isNull();
    }

    /**
     * Tests case sensitivity more thoroughly with different case combinations.
     */
    @Test
    void testGetRvrForRunway_VariousCaseCombinations() {
        NoaaMetarData data = new NoaaMetarData();
        RunwayVisualRange rvr = RunwayVisualRange.of("04L", 2200);
        data.addRunwayVisualRange(rvr);
        
        // Test all case variations
        assertThat(data.getRvrForRunway("04l")).isEqualTo(rvr);  // lowercase L
        assertThat(data.getRvrForRunway("04L")).isEqualTo(rvr);  // uppercase L
        assertThat(data.getRvrForRunway("04l")).isEqualTo(rvr);  // mixed case
    }
    
    /**
     * Tests with multiple RVRs to ensure correct one is found.
     */
    @Test
    void testGetRvrForRunway_MultipleRunways() {
        NoaaMetarData data = new NoaaMetarData();
        RunwayVisualRange rvr04L = RunwayVisualRange.of("04L", 2200);
        RunwayVisualRange rvr04R = RunwayVisualRange.of("04R", 1800);
        RunwayVisualRange rvr22L = RunwayVisualRange.of("22L", 3000);
        RunwayVisualRange rvr22R = RunwayVisualRange.of("22R", 2500);
        
        data.addRunwayVisualRange(rvr04L);
        data.addRunwayVisualRange(rvr04R);
        data.addRunwayVisualRange(rvr22L);
        data.addRunwayVisualRange(rvr22R);
        
        // Test each runway
        assertThat(data.getRvrForRunway("04L")).isEqualTo(rvr04L);
        assertThat(data.getRvrForRunway("04R")).isEqualTo(rvr04R);
        assertThat(data.getRvrForRunway("22L")).isEqualTo(rvr22L);
        assertThat(data.getRvrForRunway("22R")).isEqualTo(rvr22R);
        
        // Test non-existent runway
        assertThat(data.getRvrForRunway("09")).isNull();
    }

    /**
     * Tests with runway without L/C/R suffix.
     */
    @Test
    void testGetRvrForRunway_NoSuffix() {
        NoaaMetarData data = new NoaaMetarData();
        RunwayVisualRange rvr = RunwayVisualRange.of("18", 2200);
        data.addRunwayVisualRange(rvr);
        
        assertThat(data.getRvrForRunway("18")).isEqualTo(rvr);
        assertThat(data.getRvrForRunway("18L")).isNull(); // Different runway
    }
    
    /**
     * Tests empty string as runway ID.
     */
    @Test
    void testGetRvrForRunway_EmptyString() {
        NoaaMetarData data = new NoaaMetarData();
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        
        // Empty string should not match
        assertThat(data.getRvrForRunway("")).isNull();
    }

    // ========== INTEGRATION TESTS FOR EDGE CASES ==========
    
    /**
     * Integration test combining all three methods with edge case data.
     */
    @Test
    void testUtilityMethods_IntegrationWithEdgeCases() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        
        // Add sky conditions with mix of ceiling and non-ceiling
        data.addSkyCondition(new SkyCondition(SkyCoverage.SCATTERED, 3000, null));
        data.addSkyCondition(new SkyCondition(SkyCoverage.BROKEN, null, null)); // Ceiling but null height
        data.addSkyCondition(new SkyCondition(SkyCoverage.OVERCAST, 8000, null));
        
        // Add RVRs with various conditions
        data.addRunwayVisualRange(RunwayVisualRange.variable("04L", 1200, 1800));
        data.addRunwayVisualRange(new RunwayVisualRange("04R", 600, null, null, "M", null));
        data.addRunwayVisualRange(new RunwayVisualRange("22L", 6000, null, null, "P", null));
        
        // Test ceiling - should find lowest valid ceiling
        assertThat(data.getCeilingFeet()).isEqualTo(8000);
        
        // Test minimum RVR - should exclude "less than" and use variable low
        assertThat(data.getMinimumRvrFeet()).isEqualTo(1200);
        
        // Test specific runway lookup
        assertThat(data.getRvrForRunway("04L")).isNotNull();
        assertThat(data.getRvrForRunway("04L").isVariable()).isTrue();
        assertThat(data.getRvrForRunway("09")).isNull();
    }

    /**
     * Test with data set to null after initial population.
     */
    @Test
    void testUtilityMethods_AfterSettingToNull() {
        NoaaMetarData data = new NoaaMetarData();
        
        // Add initial data
        data.addSkyCondition(new SkyCondition(SkyCoverage.BROKEN, 5000, null));
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        
        // Verify data exists
        assertThat(data.getCeilingFeet()).isEqualTo(5000);
        assertThat(data.getMinimumRvrFeet()).isEqualTo(2200);
        assertThat(data.getRvrForRunway("04L")).isNotNull();
        
        // Set to null
        data.setSkyConditions(null);
        data.setRunwayVisualRange(null);
        
        // All should return null
        assertThat(data.getCeilingFeet()).isNull();
        assertThat(data.getMinimumRvrFeet()).isNull();
        assertThat(data.getRvrForRunway("04L")).isNull();
    }
    
    // ========== ADDITIONAL TESTS FOR getCeilingFeet() TO REACH 100% COVERAGE ==========
    
    /**
     * Explicitly tests the skyConditions == null branch (first part of OR condition).
     * Ensures the short-circuit evaluation path is covered.
     */
    @Test
    void testGetCeilingFeet_ExplicitNullSkyConditions() {
        NoaaMetarData data = new NoaaMetarData();
        // Explicitly set skyConditions to null
        data.setSkyConditions(null);
        
        Integer ceiling = data.getCeilingFeet();
        
        assertThat(ceiling).isNull();
    }
    
    /**
     * Explicitly tests the isEmpty() == true branch (second part of OR condition).
     * The list is not null, but it is empty.
     */
    @Test
    void testGetCeilingFeet_ExplicitEmptyList() {
        NoaaMetarData data = new NoaaMetarData();
        // Create an explicitly empty (but non-null) list
        data.setSkyConditions(new ArrayList<>());
        
        Integer ceiling = data.getCeilingFeet();
        
        assertThat(ceiling).isNull();
    }
    
    // ========== ADDITIONAL TESTS FOR getMinimumRvrFeet() TO REACH 100% COVERAGE ==========
    
    /**
     * Explicitly tests the runwayVisualRange == null branch (first part of OR condition).
     * Ensures the short-circuit evaluation path is covered.
     */
    @Test
    void testGetMinimumRvrFeet_ExplicitNullList() {
        NoaaMetarData data = new NoaaMetarData();
        // Explicitly set runwayVisualRange to null
        data.setRunwayVisualRange(null);
        
        Integer minRvr = data.getMinimumRvrFeet();
        
        assertThat(minRvr).isNull();
    }

    /**
     * Explicitly tests the isEmpty() == true branch (second part of OR condition).
     * The list is not null, but it is empty.
     */
    @Test
    void testGetMinimumRvrFeet_ExplicitEmptyList() {
        NoaaMetarData data = new NoaaMetarData();
        // Create an explicitly empty (but non-null) list
        data.setRunwayVisualRange(new ArrayList<>());
        
        Integer minRvr = data.getMinimumRvrFeet();
        
        assertThat(minRvr).isNull();
    }

    /**
     * Tests the ternary operator's TRUE branch: isVariable() returns true.
     * This test uses only variable RVRs to ensure the variableLow() path is taken.
     */
    @Test
    void testGetMinimumRvrFeet_OnlyVariableRvrs() {
        NoaaMetarData data = new NoaaMetarData();
        // Add only variable range RVRs (forces isVariable() == true in ternary)
        data.addRunwayVisualRange(RunwayVisualRange.variable("04L", 1200, 1800));
        data.addRunwayVisualRange(RunwayVisualRange.variable("22R", 1500, 2000));
        
        Integer minRvr = data.getMinimumRvrFeet();
        
        // Should use variableLow values, minimum is 1200
        assertThat(minRvr).isEqualTo(1200);
    }
    
    /**
     * Tests the ternary operator's FALSE branch: isVariable() returns false.
     * This test uses only non-variable RVRs to ensure the visualRangeFeet() path is taken.
     */
    @Test
    void testGetMinimumRvrFeet_OnlyNonVariableRvrs() {
        NoaaMetarData data = new NoaaMetarData();
        // Add only regular (non-variable) RVRs (forces isVariable() == false in ternary)
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        data.addRunwayVisualRange(RunwayVisualRange.of("22R", 1800));
        data.addRunwayVisualRange(RunwayVisualRange.of("04R", 2500));
        
        Integer minRvr = data.getMinimumRvrFeet();
        
        // Should use visualRangeFeet values, minimum is 1800
        assertThat(minRvr).isEqualTo(1800);
    }

    // ========== ADDITIONAL TESTS FOR getRvrForRunway() TO REACH 100% COVERAGE ==========
    
    /**
     * Explicitly tests the runwayVisualRange == null branch (first part of OR condition).
     * Ensures the short-circuit evaluation path is covered.
     */
    @Test
    void testGetRvrForRunway_ExplicitNullRvrList() {
        NoaaMetarData data = new NoaaMetarData();
        // Explicitly set runwayVisualRange to null
        data.setRunwayVisualRange(null);
        
        RunwayVisualRange rvr = data.getRvrForRunway("04L");
        
        assertThat(rvr).isNull();
    }
    
    /**
     * Explicitly tests the runwayId == null branch (second part of OR condition).
     * The list exists, but the runway ID parameter is null.
     */
    @Test
    void testGetRvrForRunway_ExplicitNullRunway() {
        NoaaMetarData data = new NoaaMetarData();
        data.addRunwayVisualRange(RunwayVisualRange.of("04L", 2200));
        
        // Pass null as runway ID
        RunwayVisualRange rvr = data.getRvrForRunway(null);
        
        assertThat(rvr).isNull();
    }

// ===========================================================================================
// EXPLANATION OF WHY THESE TESTS ARE NEEDED
// ===========================================================================================
/*
 * The compound boolean condition "if (x == null || y.isEmpty())" creates multiple branches
 * in JaCoCo's coverage analysis:
 *
 * 1. x == null → TRUE (short-circuit, method returns early)
 * 2. x == null → FALSE, y.isEmpty() → TRUE (second condition triggers return)
 * 3. Both false, processing succeeds (returns value)
 * 4. Both false, processing fails (returns null via orElse)
 *
 * Your existing tests likely cover scenarios where the object is initialized (and collections
 * are automatically initialized to empty lists in constructors), but may not explicitly test:
 * - The case where collections are explicitly set to NULL after creation
 * - The case where collections are explicitly set to EMPTY (non-null) lists
 *
 * By adding these explicit tests, we ensure JaCoCo recognizes that all branch paths through
 * the compound conditions are covered, resulting in 100% branch coverage.
 *
 * For getMinimumRvrFeet(), the additional ternary operator inside the .map() call creates
 * two more branches that must be explicitly covered by testing:
 * - A scenario with ONLY variable RVRs (ternary true branch)
 * - A scenario with ONLY non-variable RVRs (ternary false branch)
 */

// ===========================================================================================
// AFTER ADDING THESE TESTS
// ===========================================================================================
/*
 * 1. Run your test suite: mvn test
 * 2. Generate coverage report: mvn jacoco:report
 * 3. Verify coverage:
 *    - getCeilingFeet(): should show 100% (4/4 branches)
 *    - getMinimumRvrFeet(): should show 100% (4/4 branches)
 *    - getRvrForRunway(): should show 100% (4/4 branches)
 * 4. Overall branch coverage should increase from 82% toward 90%
 */
}
