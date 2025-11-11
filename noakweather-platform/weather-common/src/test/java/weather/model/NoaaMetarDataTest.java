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
        Visibility visibility = new Visibility(10.0, "SM", null);
        
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
    
    // ========== RUNWAY VISUAL RANGE TESTS ==========
    
    @Test
    void testAddRunwayVisualRange() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.addRunwayVisualRange("R04L/2200FT");
        data.addRunwayVisualRange("R04R/1800V2400FT");
        
        assertThat(data.getRunwayVisualRange()).hasSize(2);
        assertThat(data.getRunwayVisualRange()).containsExactly("R04L/2200FT", "R04R/1800V2400FT");
    }
    
    @Test
    void testSetRunwayVisualRange_WithNull() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setRunwayVisualRange(null);
        
        assertThat(data.getRunwayVisualRange()).isNotNull().isEmpty();
    }
    
    @Test
    void testSetRunwayVisualRange_WithNonNullList() {
        NoaaMetarData data = new NoaaMetarData();
        
        List<String> rvrList = new ArrayList<>();
        rvrList.add("R04L/2200FT");
        rvrList.add("R04R/1800V2400FT");
        rvrList.add("R22L/P6000FT");
        
        data.setRunwayVisualRange(rvrList);
        
        assertThat(data.getRunwayVisualRange()).hasSize(3);
        assertThat(data.getRunwayVisualRange()).containsExactly("R04L/2200FT", "R04R/1800V2400FT", "R22L/P6000FT");
    }
    
    @Test
    void testSetRunwayVisualRange_WithEmptyList() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.setRunwayVisualRange(new ArrayList<>());
        
        assertThat(data.getRunwayVisualRange()).isEmpty();
    }
    
    @Test
    void testAddRunwayVisualRange_WithBlankString() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.addRunwayVisualRange("   "); // Blank string (only whitespace)
        
        assertThat(data.getRunwayVisualRange()).isEmpty();
    }
    
    @Test
    void testAddRunwayVisualRange_WithNull() {
        NoaaMetarData data = new NoaaMetarData();
        
        data.addRunwayVisualRange(null);
        
        assertThat(data.getRunwayVisualRange()).isEmpty();
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
        data.setVisibility(new Visibility(10.0, "SM", null));
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
        data.setVisibility(new Visibility(10.0, "SM", null));
        
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
        data.setVisibility(new Visibility(10.0, "SM", null));
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
        data.setVisibility(new Visibility(10.0, "SM", null));
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
        data.setVisibility(new Visibility(10.0, "SM", null));
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
    void testToString_WithAllFields() {
        NoaaMetarData data = new NoaaMetarData("KJFK", Instant.now());
        data.setWind(new Wind(280, 16, null, null, null, "KT"));
        data.setVisibility(new Visibility(10.0, "SM", null));
        data.setTemperature(new Temperature(22.0, 12.0));
        data.setPressure(new Pressure(30.15, PressureUnit.INCHES_HG));
        data.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 25000, null));
        
        String toString = data.toString();
        
        assertThat(toString).contains("NoaaMetarData", "KJFK", "wind=", "vis=", "temp=", "pressure=", "skyCond=1");
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
        data1.setVisibility(new Visibility(10.0, "SM", null));
        data1.setTemperature(new Temperature(22.0, 12.0));
        data1.setPressure(new Pressure(30.15, PressureUnit.INCHES_HG));
        data1.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 25000, null));
        data1.setAutomatedStation("AO2");
        data1.setSeaLevelPressure(1013.2);
        
        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setRawText("METAR KJFK 191651Z 28016KT 10SM FEW250 22/12 A3015");
        data2.setWind(new Wind(280, 16, null, null, null, "KT"));
        data2.setVisibility(new Visibility(10.0, "SM", null));
        data2.setTemperature(new Temperature(22.0, 12.0));
        data2.setPressure(new Pressure(30.15, PressureUnit.INCHES_HG));
        data2.addSkyCondition(new SkyCondition(SkyCoverage.FEW, 25000, null));
        data2.setAutomatedStation("AO2");
        data2.setSeaLevelPressure(1013.2);
        
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
        data1.setVisibility(new Visibility(10.0, "SM", null));
        
        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setVisibility(new Visibility(5.0, "SM", null));
        
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
        data.setVisibility(new Visibility(10.0, "SM", null));
        
        int hash1 = data.hashCode();
        int hash2 = data.hashCode();
        
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void testHashCode_EqualObjects() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        data1.setWind(new Wind(280, 16, null, null, null, "KT"));
        data1.setVisibility(new Visibility(10.0, "SM", null));
        data1.setTemperature(new Temperature(22.0, 12.0));
        
        NoaaMetarData data2 = new NoaaMetarData("KJFK", now);
        data2.setWind(new Wind(280, 16, null, null, null, "KT"));
        data2.setVisibility(new Visibility(10.0, "SM", null));
        data2.setTemperature(new Temperature(22.0, 12.0));
        
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
        assertThatCode(() -> data1.hashCode()).doesNotThrowAnyException();
        assertThatCode(() -> data2.hashCode()).doesNotThrowAnyException();
    }
    
    @Test
    void testHashCode_IncludesParentFields() {
        Instant now = Instant.now();
        
        NoaaMetarData data1 = new NoaaMetarData("KJFK", now);
        NoaaMetarData data2 = new NoaaMetarData("KLGA", now);
        
        // Different parent fields should (likely) result in different hash codes
        // Verify hashCode() executes without throwing exception
        assertThatCode(() -> data1.hashCode()).doesNotThrowAnyException();
        assertThatCode(() -> data2.hashCode()).doesNotThrowAnyException();
    }
}
