/*
 * noakweather(TM) is a Java library for parsing weather data
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
package noakweather.noaa_api.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Simplified NoaaTafData Tests")
class NoaaTafDataTest {

    private NoaaTafData tafData;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now().minusHours(1);
        tafData = new NoaaTafData();
    }

    @Test
    @DisplayName("Default constructor initializes composition objects and collections")
    void testDefaultConstructor() {
        assertNotNull(tafData.getBaseWindInformation());
        assertNotNull(tafData.getBaseWeatherConditions());
        assertNotNull(tafData.getChangeGroups());
        assertTrue(tafData.getChangeGroups().isEmpty());
        assertEquals("TAF", tafData.getReportType());
    }

    @Test
    @DisplayName("Parameterized constructor sets basic fields and parses TAF type")
    void testParameterizedConstructor() {
        String rawText = "TAF KJFK 251720Z 2518/2624 28012KT P6SM SCT040 BKN100";
        NoaaTafData taf = new NoaaTafData(rawText, "KJFK", testTime);
        
        assertEquals(rawText, taf.getRawText());
        assertEquals("KJFK", taf.getStationId());
        assertEquals(testTime, taf.getObservationTime());
        assertEquals("TAF", taf.getTafType());
        assertEquals("TAF", taf.getReportType());
        assertNull(taf.getIsAmended());
        assertNull(taf.getIsCorrected());
        assertNotNull(taf.getBaseWindInformation());
        assertNotNull(taf.getBaseWeatherConditions());
    }

    @Test
    @DisplayName("Constructor handles TAF AMD reports")
    void testTafAmdConstructor() {
        String rawText = "TAF AMD KJFK 251720Z 2518/2624 28012KT P6SM SCT040 BKN100";
        NoaaTafData taf = new NoaaTafData(rawText, "KJFK", testTime);
        
        assertEquals("TAF AMD", taf.getTafType());
        assertEquals("TAF AMD", taf.getReportType());
        assertTrue(taf.getIsAmended());
        assertNull(taf.getIsCorrected());
    }

    @Test
    @DisplayName("Constructor handles TAF COR reports")
    void testTafCorConstructor() {
        String rawText = "TAF COR KJFK 251720Z 2518/2624 28012KT P6SM SCT040 BKN100";
        NoaaTafData taf = new NoaaTafData(rawText, "KJFK", testTime);
        
        assertEquals("TAF COR", taf.getTafType());
        assertEquals("TAF COR", taf.getReportType());
        assertNull(taf.getIsAmended());
        assertTrue(taf.getIsCorrected());
    }

    @Test
    @DisplayName("Validity period getters and setters work correctly")
    void testValidityPeriodFields() {
        LocalDateTime fromTime = LocalDateTime.now().plusHours(1);
        LocalDateTime toTime = LocalDateTime.now().plusHours(25);
        
        tafData.setValidFromTime(fromTime);
        tafData.setValidToTime(toTime);
        
        assertEquals(fromTime, tafData.getValidFromTime());
        assertEquals(toTime, tafData.getValidToTime());
    }

    @Test
    @DisplayName("Issue time and bulletin time fields work correctly")
    void testIssueTimeFields() {
        LocalDateTime issueTime = LocalDateTime.now();
        String bulletinTime = "251720Z";
        
        tafData.setIssueTime(issueTime);
        tafData.setBulletinTime(bulletinTime);
        
        assertEquals(issueTime, tafData.getIssueTime());
        assertEquals(bulletinTime, tafData.getBulletinTime());
    }

    @Test
    @DisplayName("TAF type and amendment fields work correctly")
    void testTafTypeFields() {
        tafData.setTafType("TAF AMD");
        tafData.setIsAmended(true);
        tafData.setIsCorrected(false);
        
        assertEquals("TAF AMD", tafData.getTafType());
        assertEquals("TAF AMD", tafData.getReportType());
        assertTrue(tafData.getIsAmended());
        assertFalse(tafData.getIsCorrected());
    }

    @Test
    @DisplayName("Base forecast text field works correctly")
    void testBaseForecastText() {
        String forecastText = "28012KT P6SM SCT040 BKN100";
        tafData.setBaseForecastText(forecastText);
        assertEquals(forecastText, tafData.getBaseForecastText());
    }

    @Test
    @DisplayName("Base wind information composition works correctly")
    void testBaseWindInformationComposition() {
        WindInformation wind = tafData.getBaseWindInformation();
        assertNotNull(wind);
        
        wind.setWindDirectionDegrees(270);
        wind.setWindSpeedKnots(15);
        wind.setWindGustKnots(22);
        
        assertEquals(270, tafData.getBaseWindInformation().getWindDirectionDegrees());
        assertEquals(15, tafData.getBaseWindInformation().getWindSpeedKnots());
        assertEquals(22, tafData.getBaseWindInformation().getWindGustKnots());
        
        assertFalse(tafData.getBaseWindInformation().isCalm());
        assertTrue(tafData.getBaseWindInformation().hasGusts());
        assertEquals("W", tafData.getBaseWindInformation().getWindDirectionCardinal());
    }

    @Test
    @DisplayName("Setting base wind information to null creates new instance")
    void testSetBaseWindInformationNull() {
        tafData.setBaseWindInformation(null);
        assertNotNull(tafData.getBaseWindInformation());
    }

    @Test
    @DisplayName("Setting custom base wind information object works")
    void testSetCustomBaseWindInformation() {
        WindInformation customWind = new WindInformation(180, 12, null);
        tafData.setBaseWindInformation(customWind);
        
        assertEquals(180, tafData.getBaseWindInformation().getWindDirectionDegrees());
        assertEquals(12, tafData.getBaseWindInformation().getWindSpeedKnots());
        assertNull(tafData.getBaseWindInformation().getWindGustKnots());
    }

    @Test
    @DisplayName("Base weather conditions composition works correctly")
    void testBaseWeatherConditionsComposition() {
        WeatherConditions weather = tafData.getBaseWeatherConditions();
        assertNotNull(weather);
        
        weather.setVisibilityStatuteMiles(5.0);
        weather.setWeatherString("RA BR");
        weather.setSkyCondition("OVC010");
        
        assertEquals(5.0, tafData.getBaseWeatherConditions().getVisibilityStatuteMiles());
        assertEquals("RA BR", tafData.getBaseWeatherConditions().getWeatherString());
        assertEquals("OVC010", tafData.getBaseWeatherConditions().getSkyCondition());
        
        assertTrue(tafData.getBaseWeatherConditions().hasGoodVisibility());
        assertTrue(tafData.getBaseWeatherConditions().hasActiveWeather());
    }

    @Test
    @DisplayName("Setting base weather conditions to null creates new instance")
    void testSetBaseWeatherConditionsNull() {
        tafData.setBaseWeatherConditions(null);
        assertNotNull(tafData.getBaseWeatherConditions());
    }

    @Test
    @DisplayName("Setting custom base weather conditions object works")
    void testSetCustomBaseWeatherConditions() {
        WeatherConditions customWeather = new WeatherConditions(2.5, "FG", "OVC005");
        tafData.setBaseWeatherConditions(customWeather);
        
        assertEquals(2.5, tafData.getBaseWeatherConditions().getVisibilityStatuteMiles());
        assertEquals("FG", tafData.getBaseWeatherConditions().getWeatherString());
        assertEquals("OVC005", tafData.getBaseWeatherConditions().getSkyCondition());
        assertFalse(tafData.getBaseWeatherConditions().hasGoodVisibility());
        assertTrue(tafData.getBaseWeatherConditions().hasActiveWeather());
    }

    @Test
    @DisplayName("Change groups list management works correctly")
    void testChangeGroupsManagement() {
        NoaaTafData.TafChangeGroup changeGroup1 = new NoaaTafData.TafChangeGroup("TEMPO", "TEMPO 2520/2524 3SM BR");
        NoaaTafData.TafChangeGroup changeGroup2 = new NoaaTafData.TafChangeGroup("BECMG", "BECMG 2602/2604 VRB06KT");
        
        tafData.addChangeGroup(changeGroup1);
        tafData.addChangeGroup(changeGroup2);
        
        assertEquals(2, tafData.getChangeGroups().size());
        assertTrue(tafData.getChangeGroups().contains(changeGroup1));
        assertTrue(tafData.getChangeGroups().contains(changeGroup2));
    }

    @Test
    @DisplayName("Setting change groups to null creates empty list")
    void testSetChangeGroupsNull() {
        tafData.setChangeGroups(null);
        assertNotNull(tafData.getChangeGroups());
        assertTrue(tafData.getChangeGroups().isEmpty());
    }

    @Test
    @DisplayName("addChangeGroup creates list when null")
    void testAddChangeGroupWithNullList() {
        tafData.setChangeGroups(null);
        NoaaTafData.TafChangeGroup changeGroup = new NoaaTafData.TafChangeGroup("FM", "FM252300 30008KT");
        
        tafData.addChangeGroup(changeGroup);
        assertNotNull(tafData.getChangeGroups());
        assertEquals(1, tafData.getChangeGroups().size());
    }

    @Test
    @DisplayName("getValidityPeriodHours calculates correctly")
    void testGetValidityPeriodHours() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.plusHours(24);
        
        tafData.setValidFromTime(from);
        tafData.setValidToTime(to);
        
        assertEquals(24, tafData.getValidityPeriodHours());
    }

    @Test
    @DisplayName("getValidityPeriodHours returns 0 for null times")
    void testGetValidityPeriodHoursWithNullTimes() {
        assertEquals(0, tafData.getValidityPeriodHours());
        
        tafData.setValidFromTime(LocalDateTime.now());
        assertEquals(0, tafData.getValidityPeriodHours());
        
        tafData.setValidFromTime(null);
        tafData.setValidToTime(LocalDateTime.now());
        assertEquals(0, tafData.getValidityPeriodHours());
    }

    @Test
    @DisplayName("isModified returns correct values")
    void testIsModified() {
        assertFalse(tafData.isModified());
        
        tafData.setIsAmended(true);
        assertTrue(tafData.isModified());
        
        tafData.setIsAmended(false);
        tafData.setIsCorrected(true);
        assertTrue(tafData.isModified());
        
        tafData.setIsAmended(true);
        tafData.setIsCorrected(true);
        assertTrue(tafData.isModified());
        
        tafData.setIsAmended(false);
        tafData.setIsCorrected(false);
        assertFalse(tafData.isModified());
    }

    @Test
    @DisplayName("isCurrent returns true when within validity period")
    void testIsCurrentWithinPeriod() {
        LocalDateTime now = LocalDateTime.now();
        tafData.setValidFromTime(now.minusHours(1));
        tafData.setValidToTime(now.plusHours(23));
        assertTrue(tafData.isCurrent());
    }

    @Test
    @DisplayName("isCurrent returns false when outside validity period")
    void testIsCurrentOutsidePeriod() {
        LocalDateTime now = LocalDateTime.now();
        tafData.setValidFromTime(now.plusHours(1));
        tafData.setValidToTime(now.plusHours(25));
        assertFalse(tafData.isCurrent());
        
        tafData.setValidFromTime(now.minusHours(25));
        tafData.setValidToTime(now.minusHours(1));
        assertFalse(tafData.isCurrent());
    }

    @Test
    @DisplayName("isCurrent returns false for null validToTime")
    void testIsCurrentNullValidToTime() {
        tafData.setValidFromTime(LocalDateTime.now().minusHours(1));
        assertFalse(tafData.isCurrent());
    }

    @Test
    @DisplayName("isCurrent handles null validFromTime correctly")
    void testIsCurrentNullValidFromTime() {
        LocalDateTime now = LocalDateTime.now();
        tafData.setValidToTime(now.plusHours(23));
        assertTrue(tafData.isCurrent());
    }

    @Test
    @DisplayName("equals works correctly with composition objects")
    void testEquals() {
        LocalDateTime time = LocalDateTime.now();
        LocalDateTime validFrom = time.plusHours(1);
        LocalDateTime validTo = time.plusHours(25);
        LocalDateTime issueTime = time;
        
        NoaaTafData taf1 = new NoaaTafData("TAF KJFK 251720Z", "KJFK", time);
        taf1.setValidFromTime(validFrom);
        taf1.setValidToTime(validTo);
        taf1.setIssueTime(issueTime);
        taf1.setTafType("TAF");
        taf1.getBaseWindInformation().setWindDirectionDegrees(270);
        taf1.getBaseWeatherConditions().setVisibilityStatuteMiles(10.0);
        
        NoaaTafData taf2 = new NoaaTafData("TAF KJFK 251720Z", "KJFK", time);
        taf2.setValidFromTime(validFrom);
        taf2.setValidToTime(validTo);
        taf2.setIssueTime(issueTime);
        taf2.setTafType("TAF");
        taf2.getBaseWindInformation().setWindDirectionDegrees(270);
        taf2.getBaseWeatherConditions().setVisibilityStatuteMiles(10.0);
        
        assertEquals(taf1, taf2);
        assertEquals(taf1.hashCode(), taf2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different composition objects")
    void testEqualsNotEqual() {
        LocalDateTime time = LocalDateTime.now();
        
        NoaaTafData taf1 = new NoaaTafData("TAF KJFK 251720Z", "KJFK", time);
        taf1.getBaseWindInformation().setWindDirectionDegrees(270);
        
        NoaaTafData taf2 = new NoaaTafData("TAF KJFK 251720Z", "KJFK", time);
        taf2.getBaseWindInformation().setWindDirectionDegrees(180);
        
        assertNotEquals(taf1, taf2);
    }

    @Test
    @DisplayName("hashCode is consistent with composition objects")
    void testHashCodeConsistency() {
        tafData.setTafType("TAF AMD");
        tafData.getBaseWindInformation().setWindDirectionDegrees(90);
        tafData.getBaseWeatherConditions().setVisibilityStatuteMiles(8.0);
        
        int hashCode1 = tafData.hashCode();
        int hashCode2 = tafData.hashCode();
        
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("TafChangeGroup default constructor initializes composition objects")
    void testTafChangeGroupDefaultConstructor() {
        NoaaTafData.TafChangeGroup changeGroup = new NoaaTafData.TafChangeGroup();
        
        assertNotNull(changeGroup.getWindInformation());
        assertNotNull(changeGroup.getWeatherConditions());
    }

    @Test
    @DisplayName("TafChangeGroup parameterized constructor sets fields")
    void testTafChangeGroupParameterizedConstructor() {
        NoaaTafData.TafChangeGroup changeGroup = new NoaaTafData.TafChangeGroup("TEMPO", "TEMPO 2520/2524 3SM BR");
        
        assertEquals("TEMPO", changeGroup.getChangeType());
        assertEquals("TEMPO 2520/2524 3SM BR", changeGroup.getChangeText());
        assertNotNull(changeGroup.getWindInformation());
        assertNotNull(changeGroup.getWeatherConditions());
    }

    @Test
    @DisplayName("TafChangeGroup basic fields work correctly")
    void testTafChangeGroupBasicFields() {
        NoaaTafData.TafChangeGroup changeGroup = new NoaaTafData.TafChangeGroup();
        LocalDateTime fromTime = LocalDateTime.now().plusHours(4);
        LocalDateTime toTime = LocalDateTime.now().plusHours(8);
        
        changeGroup.setChangeType("BECMG");
        changeGroup.setChangeTimeFrom(fromTime);
        changeGroup.setChangeTimeTo(toTime);
        changeGroup.setChangeText("BECMG 2604/2608 VRB06KT");
        
        assertEquals("BECMG", changeGroup.getChangeType());
        assertEquals(fromTime, changeGroup.getChangeTimeFrom());
        assertEquals(toTime, changeGroup.getChangeTimeTo());
        assertEquals("BECMG 2604/2608 VRB06KT", changeGroup.getChangeText());
    }

    @Test
    @DisplayName("TafChangeGroup wind composition works correctly")
    void testTafChangeGroupWindComposition() {
        NoaaTafData.TafChangeGroup changeGroup = new NoaaTafData.TafChangeGroup();
        
        WindInformation wind = changeGroup.getWindInformation();
        assertNotNull(wind);
        
        wind.setWindDirectionDegrees(220);
        wind.setWindSpeedKnots(18);
        wind.setWindGustKnots(25);
        
        assertEquals(220, changeGroup.getWindInformation().getWindDirectionDegrees());
        assertEquals(18, changeGroup.getWindInformation().getWindSpeedKnots());
        assertEquals(25, changeGroup.getWindInformation().getWindGustKnots());
        
        assertFalse(changeGroup.getWindInformation().isCalm());
        assertTrue(changeGroup.getWindInformation().hasGusts());
        assertEquals("SW", changeGroup.getWindInformation().getWindDirectionCardinal());
    }

    @Test
    @DisplayName("TafChangeGroup weather composition works correctly")
    void testTafChangeGroupWeatherComposition() {
        NoaaTafData.TafChangeGroup changeGroup = new NoaaTafData.TafChangeGroup();
        
        WeatherConditions weather = changeGroup.getWeatherConditions();
        assertNotNull(weather);
        
        weather.setVisibilityStatuteMiles(3.0);
        weather.setWeatherString("BR");
        weather.setSkyCondition("OVC008");
        
        assertEquals(3.0, changeGroup.getWeatherConditions().getVisibilityStatuteMiles());
        assertEquals("BR", changeGroup.getWeatherConditions().getWeatherString());
        assertEquals("OVC008", changeGroup.getWeatherConditions().getSkyCondition());
        
        assertTrue(changeGroup.getWeatherConditions().hasGoodVisibility());
        assertTrue(changeGroup.getWeatherConditions().hasActiveWeather());
    }

    @Test
    @DisplayName("TafChangeGroup setting composition objects to null creates new instances")
    void testTafChangeGroupSetCompositionObjectsNull() {
        NoaaTafData.TafChangeGroup changeGroup = new NoaaTafData.TafChangeGroup();
        
        changeGroup.setWindInformation(null);
        assertNotNull(changeGroup.getWindInformation());
        
        changeGroup.setWeatherConditions(null);
        assertNotNull(changeGroup.getWeatherConditions());
    }

    @Test
    @DisplayName("TafChangeGroup equals and hashCode work correctly")
    void testTafChangeGroupEqualsAndHashCode() {
        LocalDateTime from = LocalDateTime.now().plusHours(2);
        LocalDateTime to = LocalDateTime.now().plusHours(6);
        
        NoaaTafData.TafChangeGroup group1 = new NoaaTafData.TafChangeGroup("TEMPO", "TEMPO 2520/2524 3SM BR");
        group1.setChangeTimeFrom(from);
        group1.setChangeTimeTo(to);
        group1.getWindInformation().setWindDirectionDegrees(180);
        group1.getWeatherConditions().setVisibilityStatuteMiles(3.0);
        
        NoaaTafData.TafChangeGroup group2 = new NoaaTafData.TafChangeGroup("TEMPO", "TEMPO 2520/2524 3SM BR");
        group2.setChangeTimeFrom(from);
        group2.setChangeTimeTo(to);
        group2.getWindInformation().setWindDirectionDegrees(180);
        group2.getWeatherConditions().setVisibilityStatuteMiles(3.0);

        assertEquals(group1, group2);
        assertEquals(group1.hashCode(), group2.hashCode());
    }

    @Test
    @DisplayName("TafChangeGroup toString works correctly")
    void testTafChangeGroupToString() {
        LocalDateTime from = LocalDateTime.of(2025, 1, 25, 20, 0);
        LocalDateTime to = LocalDateTime.of(2025, 1, 26, 4, 0);
        
        NoaaTafData.TafChangeGroup changeGroup = new NoaaTafData.TafChangeGroup("TEMPO", "TEMPO 2520/2604 3SM BR");
        changeGroup.setChangeTimeFrom(from);
        changeGroup.setChangeTimeTo(to);
        
        String expected = "TafChangeGroup{type='TEMPO', from=2025-01-25T20:00, to=2025-01-26T04:00}";
        assertEquals(expected, changeGroup.toString());
    }

    @Test
    @DisplayName("Complete TAF data workflow with composition objects")
    void testCompleteTafWorkflow() {
        String rawText = "TAF AMD KJFK 251720Z 2518/2627 28012KT P6SM SCT040 BKN100 TEMPO 2520/2524 2SM BR OVC008";
        LocalDateTime issueTime = LocalDateTime.of(2025, 1, 25, 17, 20);
        LocalDateTime validFrom = LocalDateTime.of(2025, 1, 25, 18, 0);
        LocalDateTime validTo = LocalDateTime.of(2025, 1, 27, 0, 0);
        
        NoaaTafData taf = new NoaaTafData(rawText, "KJFK", issueTime);
        
        taf.setValidFromTime(validFrom);
        taf.setValidToTime(validTo);
        taf.setIssueTime(issueTime);
        taf.setBulletinTime("251720Z");
        
        taf.setBaseForecastText("28012KT P6SM SCT040 BKN100");
        
        WindInformation baseWind = taf.getBaseWindInformation();
        baseWind.setWindDirectionDegrees(280);
        baseWind.setWindSpeedKnots(12);
        
        WeatherConditions baseWeather = taf.getBaseWeatherConditions();
        baseWeather.setVisibilityStatuteMiles(10.0);
        baseWeather.setSkyCondition("SCT040 BKN100");
        
        NoaaTafData.TafChangeGroup tempoGroup = new NoaaTafData.TafChangeGroup("TEMPO", "TEMPO 2520/2524 2SM BR OVC008");
        tempoGroup.setChangeTimeFrom(LocalDateTime.of(2025, 1, 25, 20, 0));
        tempoGroup.setChangeTimeTo(LocalDateTime.of(2025, 1, 26, 0, 0));
        
        WeatherConditions tempoWeather = tempoGroup.getWeatherConditions();
        tempoWeather.setVisibilityStatuteMiles(2.0);
        tempoWeather.setWeatherString("BR");
        tempoWeather.setSkyCondition("OVC008");
        
        taf.addChangeGroup(tempoGroup);
        
        assertEquals("KJFK", taf.getStationId());
        assertEquals(rawText, taf.getRawText());
        assertEquals("TAF AMD", taf.getTafType());
        assertTrue(taf.isModified());
        assertEquals(30, taf.getValidityPeriodHours());
        
        WindInformation retrievedBaseWind = taf.getBaseWindInformation();
        assertEquals(280, retrievedBaseWind.getWindDirectionDegrees());
        assertEquals(12, retrievedBaseWind.getWindSpeedKnots());
        assertEquals("W", retrievedBaseWind.getWindDirectionCardinal());
        assertFalse(retrievedBaseWind.isCalm());
        
        WeatherConditions retrievedBaseWeather = taf.getBaseWeatherConditions();
        assertEquals(10.0, retrievedBaseWeather.getVisibilityStatuteMiles());
        assertEquals("SCT040 BKN100", retrievedBaseWeather.getSkyCondition());
        assertTrue(retrievedBaseWeather.hasGoodVisibility());
        
        assertEquals(1, taf.getChangeGroups().size());
        NoaaTafData.TafChangeGroup retrievedGroup = taf.getChangeGroups().get(0);
        assertEquals("TEMPO", retrievedGroup.getChangeType());
        
        WeatherConditions retrievedTempoWeather = retrievedGroup.getWeatherConditions();
        assertEquals(2.0, retrievedTempoWeather.getVisibilityStatuteMiles());
        assertEquals("BR", retrievedTempoWeather.getWeatherString());
        assertEquals("OVC008", retrievedTempoWeather.getSkyCondition());
        assertFalse(retrievedTempoWeather.hasGoodVisibility());
        assertTrue(retrievedTempoWeather.hasActiveWeather());
    }

    @Test
    @DisplayName("Composition objects maintain independence across TAF instances")
    void testCompositionObjectIndependence() {
        NoaaTafData taf1 = new NoaaTafData();
        NoaaTafData taf2 = new NoaaTafData();
        
        taf1.getBaseWindInformation().setWindDirectionDegrees(270);
        taf1.getBaseWindInformation().setWindSpeedKnots(15);
        
        assertNull(taf2.getBaseWindInformation().getWindDirectionDegrees());
        assertNull(taf2.getBaseWindInformation().getWindSpeedKnots());
        
        taf2.getBaseWeatherConditions().setVisibilityStatuteMiles(5.0);
        taf2.getBaseWeatherConditions().setWeatherString("RA");
        
        assertNull(taf1.getBaseWeatherConditions().getVisibilityStatuteMiles());
        assertNull(taf1.getBaseWeatherConditions().getWeatherString());
    }

    @Test
    @DisplayName("Composition objects maintain independence across change groups")
    void testChangeGroupCompositionObjectIndependence() {
        NoaaTafData.TafChangeGroup group1 = new NoaaTafData.TafChangeGroup();
        NoaaTafData.TafChangeGroup group2 = new NoaaTafData.TafChangeGroup();
        
        group1.getWindInformation().setWindDirectionDegrees(180);
        group1.getWindInformation().setWindSpeedKnots(20);
        
        assertNull(group2.getWindInformation().getWindDirectionDegrees());
        assertNull(group2.getWindInformation().getWindSpeedKnots());
        
        group2.getWeatherConditions().setVisibilityStatuteMiles(1.0);
        group2.getWeatherConditions().setWeatherString("FG");
        
        assertNull(group1.getWeatherConditions().getVisibilityStatuteMiles());
        assertNull(group1.getWeatherConditions().getWeatherString());
    }
}
