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

import noakweather.noaa_api.model.NoaaTafData.TafChangeGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NoaaTafData class.
 * 
 * Tests TAF-specific functionality including forecast validity periods,
 * change groups, amended/corrected flags, and the isCurrent() business logic
 * specific to forecasts.
 * 
 * @author bclasky1539
 */
@DisplayName("NOAA TAF Data Tests")
class NoaaTafDataTest {
    
    private NoaaTafData tafData;
    private LocalDateTime testIssueTime;
    private LocalDateTime testValidFromTime;
    private LocalDateTime testValidToTime;
    
    @BeforeEach
    void setUp() {
        tafData = new NoaaTafData();
        testIssueTime = LocalDateTime.now().minusHours(1);
        testValidFromTime = LocalDateTime.now();
        testValidToTime = LocalDateTime.now().plusHours(24);
    }
    
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Default constructor creates empty TAF")
        void defaultConstructor() {
            NoaaTafData data = new NoaaTafData();
            
            assertNull(data.getRawText());
            assertNull(data.getStationId());
            assertNull(data.getObservationTime());
            assertNull(data.getValidFromTime());
            assertNull(data.getValidToTime());
            assertNull(data.getIssueTime());
            assertNull(data.getTafType());
            assertNull(data.getIsAmended());
            assertNull(data.getIsCorrected());
            assertNotNull(data.getChangeGroups());
            assertTrue(data.getChangeGroups().isEmpty());
        }
        
        @Test
        @DisplayName("Parameterized constructor parses TAF type from raw text")
        void parameterizedConstructorWithTaf() {
            String rawText = "TAF KJFK 151720Z 1518/1624 28016KT P6SM FEW250";
            
            NoaaTafData data = new NoaaTafData(rawText, "KJFK", testIssueTime);
            
            assertEquals(rawText, data.getRawText());
            assertEquals("KJFK", data.getStationId());
            assertEquals(testIssueTime, data.getObservationTime());
            assertEquals("TAF", data.getTafType());
            assertNull(data.getIsAmended());
            assertNull(data.getIsCorrected());
        }
        
        @Test
        @DisplayName("Parameterized constructor recognizes amended TAF")
        void parameterizedConstructorWithAmendedTaf() {
            String rawText = "TAF AMD KJFK 151800Z 1518/1624 29020KT P6SM FEW180";
            
            NoaaTafData data = new NoaaTafData(rawText, "KJFK", testIssueTime);
            
            assertEquals("TAF AMD", data.getTafType());
            assertTrue(data.getIsAmended());
            assertNull(data.getIsCorrected());
        }
        
        @Test
        @DisplayName("Parameterized constructor recognizes corrected TAF")
        void parameterizedConstructorWithCorrectedTaf() {
            String rawText = "TAF COR KJFK 151800Z 1518/1624 29020KT P6SM FEW180";
            
            NoaaTafData data = new NoaaTafData(rawText, "KJFK", testIssueTime);
            
            assertEquals("TAF COR", data.getTafType());
            assertNull(data.getIsAmended());
            assertTrue(data.getIsCorrected());
        }
    }
    
    @Nested
    @DisplayName("Validity Period Tests")
    class ValidityPeriodTests {
        
        @Test
        @DisplayName("Valid from time field works correctly")
        void validFromTimeField() {
            tafData.setValidFromTime(testValidFromTime);
            assertEquals(testValidFromTime, tafData.getValidFromTime());
            
            tafData.setValidFromTime(null);
            assertNull(tafData.getValidFromTime());
        }
        
        @Test
        @DisplayName("Valid to time field works correctly")
        void validToTimeField() {
            tafData.setValidToTime(testValidToTime);
            assertEquals(testValidToTime, tafData.getValidToTime());
        }
        
        @Test
        @DisplayName("Issue time field works correctly")
        void issueTimeField() {
            tafData.setIssueTime(testIssueTime);
            assertEquals(testIssueTime, tafData.getIssueTime());
        }
        
        @Test
        @DisplayName("Bulletin time field works correctly")
        void bulletinTimeField() {
            String bulletinTime = "151720Z";
            
            tafData.setBulletinTime(bulletinTime);
            assertEquals(bulletinTime, tafData.getBulletinTime());
        }
        
        @Test
        @DisplayName("Validity period hours calculation")
        void validityPeriodHours() {
            LocalDateTime from = LocalDateTime.of(2024, 1, 15, 18, 0);
            LocalDateTime to = LocalDateTime.of(2024, 1, 16, 18, 0);
            
            tafData.setValidFromTime(from);
            tafData.setValidToTime(to);
            
            assertEquals(24, tafData.getValidityPeriodHours());
        }
        
        @Test
        @DisplayName("Validity period with null times returns zero")
        void validityPeriodWithNullTimes() {
            assertEquals(0, tafData.getValidityPeriodHours());
            
            tafData.setValidFromTime(testValidFromTime);
            assertEquals(0, tafData.getValidityPeriodHours()); // Still null to time
            
            tafData.setValidFromTime(null);
            tafData.setValidToTime(testValidToTime);
            assertEquals(0, tafData.getValidityPeriodHours()); // Still null from time
        }
    }
    
    @Nested
    @DisplayName("TAF Type and Status Tests")
    class TafTypeAndStatusTests {
        
        @Test
        @DisplayName("TAF type field works correctly")
        void tafTypeField() {
            tafData.setTafType("TAF");
            assertEquals("TAF", tafData.getTafType());
            
            tafData.setTafType("TAF AMD");
            assertEquals("TAF AMD", tafData.getTafType());
            
            tafData.setTafType("TAF COR");
            assertEquals("TAF COR", tafData.getTafType());
        }
        
        @Test
        @DisplayName("Amended flag works correctly")
        void amendedFlag() {
            tafData.setIsAmended(true);
            assertTrue(tafData.getIsAmended());
            
            tafData.setIsAmended(false);
            assertFalse(tafData.getIsAmended());
            
            tafData.setIsAmended(null);
            assertNull(tafData.getIsAmended());
        }
        
        @Test
        @DisplayName("Corrected flag works correctly")
        void correctedFlag() {
            tafData.setIsCorrected(true);
            assertTrue(tafData.getIsCorrected());
            
            tafData.setIsCorrected(false);
            assertFalse(tafData.getIsCorrected());
        }
        
        @Test
        @DisplayName("isModified method works correctly")
        void isModifiedMethod() {
            // Default state - not modified
            assertFalse(tafData.isModified());
            
            // Amended TAF is modified
            tafData.setIsAmended(true);
            assertTrue(tafData.isModified());
            
            // Reset and test corrected
            tafData.setIsAmended(null);
            tafData.setIsCorrected(true);
            assertTrue(tafData.isModified());
            
            // Both amended and corrected
            tafData.setIsAmended(true);
            tafData.setIsCorrected(true);
            assertTrue(tafData.isModified());
            
            // Explicitly false flags
            tafData.setIsAmended(false);
            tafData.setIsCorrected(false);
            assertFalse(tafData.isModified());
        }
    }
    
    @Nested
    @DisplayName("Base Forecast Tests")
    class BaseForecastTests {
        
        @Test
        @DisplayName("Base forecast text field works correctly")
        void baseForecastTextField() {
            String forecastText = "28016KT P6SM FEW250";
            
            tafData.setBaseForecastText(forecastText);
            assertEquals(forecastText, tafData.getBaseForecastText());
        }
        
        @Test
        @DisplayName("Base wind fields work correctly")
        void baseWindFields() {
            tafData.setBaseWindDirectionDegrees(280);
            assertEquals(280, tafData.getBaseWindDirectionDegrees());
            
            tafData.setBaseWindSpeedKnots(16);
            assertEquals(16, tafData.getBaseWindSpeedKnots());
            
            tafData.setBaseWindGustKnots(25);
            assertEquals(25, tafData.getBaseWindGustKnots());
        }
        
        @Test
        @DisplayName("Base visibility field works correctly")
        void baseVisibilityField() {
            Double visibility = 6.0;
            
            tafData.setBaseVisibilityStatuteMiles(visibility);
            assertEquals(visibility, tafData.getBaseVisibilityStatuteMiles());
        }
        
        @Test
        @DisplayName("Base weather and sky condition fields work correctly")
        void baseWeatherAndSkyFields() {
            String weather = "SHRA";
            String skyCondition = "SCT030 BKN080";
            
            tafData.setBaseWeatherString(weather);
            assertEquals(weather, tafData.getBaseWeatherString());
            
            tafData.setBaseSkyCondition(skyCondition);
            assertEquals(skyCondition, tafData.getBaseSkyCondition());
        }
    }
    
    @Nested
    @DisplayName("Change Groups Tests")
    class ChangeGroupsTests {
        
        @Test
        @DisplayName("Change groups list initialization")
        void changeGroupsListInitialization() {
            NoaaTafData taf = new NoaaTafData();
            assertNotNull(taf.getChangeGroups());
            assertTrue(taf.getChangeGroups().isEmpty());
        }
        
        @Test
        @DisplayName("Adding change groups works correctly")
        void addingChangeGroups() {
            TafChangeGroup group1 = new TafChangeGroup("TEMPO", "TEMPO 1520/1522 29025G35KT 3SM -RA");
            TafChangeGroup group2 = new TafChangeGroup("BECMG", "BECMG 1600/1602 28015KT");
            
            tafData.addChangeGroup(group1);
            assertEquals(1, tafData.getChangeGroups().size());
            assertEquals(group1, tafData.getChangeGroups().get(0));
            
            tafData.addChangeGroup(group2);
            assertEquals(2, tafData.getChangeGroups().size());
            assertEquals(group2, tafData.getChangeGroups().get(1));
        }
        
        @Test
        @DisplayName("Setting change groups list works correctly")
        void settingChangeGroupsList() {
            List<TafChangeGroup> groups = new ArrayList<>();
            groups.add(new TafChangeGroup("TEMPO", "TEMPO 1520/1522 29025G35KT"));
            groups.add(new TafChangeGroup("FM", "FM151800 28012KT"));
            
            tafData.setChangeGroups(groups);
            assertEquals(2, tafData.getChangeGroups().size());
            assertEquals("TEMPO", tafData.getChangeGroups().get(0).getChangeType());
            assertEquals("FM", tafData.getChangeGroups().get(1).getChangeType());
        }
        
        @Test
        @DisplayName("Setting null change groups creates empty list")
        void settingNullChangeGroups() {
            tafData.setChangeGroups(null);
            assertNotNull(tafData.getChangeGroups());
            assertTrue(tafData.getChangeGroups().isEmpty());
        }
        
        @Test
        @DisplayName("Adding change group to null list initializes list")
        void addingToNullChangeGroupsList() {
            tafData.setChangeGroups(null);
            TafChangeGroup group = new TafChangeGroup("TEMPO", "TEMPO 1520/1522");
            
            tafData.addChangeGroup(group);
            assertEquals(1, tafData.getChangeGroups().size());
        }
    }
    
    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {
        
        @Test
        @DisplayName("isCurrent returns true for valid TAF")
        void isCurrentWithValidTaf() {
            LocalDateTime now = LocalDateTime.now();
            
            tafData.setValidFromTime(now.minusHours(1));
            tafData.setValidToTime(now.plusHours(23));
            
            assertTrue(tafData.isCurrent());
        }
        
        @Test
        @DisplayName("isCurrent returns false for expired TAF")
        void isCurrentWithExpiredTaf() {
            LocalDateTime now = LocalDateTime.now();
            
            tafData.setValidFromTime(now.minusHours(25));
            tafData.setValidToTime(now.minusHours(1));
            
            assertFalse(tafData.isCurrent());
        }
        
        @Test
        @DisplayName("isCurrent returns false for future TAF")
        void isCurrentWithFutureTaf() {
            LocalDateTime now = LocalDateTime.now();
            
            tafData.setValidFromTime(now.plusHours(1));
            tafData.setValidToTime(now.plusHours(25));
            
            assertFalse(tafData.isCurrent());
        }
        
        @Test
        @DisplayName("isCurrent returns false with null valid to time")
        void isCurrentWithNullValidToTime() {
            tafData.setValidFromTime(LocalDateTime.now().minusHours(1));
            tafData.setValidToTime(null);
            
            assertFalse(tafData.isCurrent());
        }
        
        @Test
        @DisplayName("isCurrent handles null valid from time correctly")
        void isCurrentWithNullValidFromTime() {
            LocalDateTime now = LocalDateTime.now();
            
            tafData.setValidFromTime(null);
            tafData.setValidToTime(now.plusHours(23));
            
            assertTrue(tafData.isCurrent()); // Should be current if before valid to time
        }
        
        @Test
        @DisplayName("getReportType returns correct type")
        void getReportTypeMethod() {
            tafData.setTafType("TAF");
            assertEquals("TAF", tafData.getReportType());
            
            tafData.setTafType("TAF AMD");
            assertEquals("TAF AMD", tafData.getReportType());
            
            tafData.setTafType(null);
            assertEquals("TAF", tafData.getReportType()); // Default fallback
        }
    }
    
    @Nested
    @DisplayName("TafChangeGroup Tests")
    class TafChangeGroupTests {
        
        @Test
        @DisplayName("TafChangeGroup default constructor")
        void tafChangeGroupDefaultConstructor() {
            TafChangeGroup group = new TafChangeGroup();
            
            assertNull(group.getChangeType());
            assertNull(group.getChangeText());
            assertNull(group.getChangeTimeFrom());
            assertNull(group.getChangeTimeTo());
        }
        
        @Test
        @DisplayName("TafChangeGroup parameterized constructor")
        void tafChangeGroupParameterizedConstructor() {
            String changeType = "TEMPO";
            String changeText = "TEMPO 1520/1522 29025G35KT 3SM -RA";
            
            TafChangeGroup group = new TafChangeGroup(changeType, changeText);
            
            assertEquals(changeType, group.getChangeType());
            assertEquals(changeText, group.getChangeText());
        }
        
        @Test
        @DisplayName("TafChangeGroup all fields work correctly")
        void tafChangeGroupAllFields() {
            TafChangeGroup group = new TafChangeGroup();
            LocalDateTime timeFrom = LocalDateTime.of(2024, 1, 15, 20, 0);
            LocalDateTime timeTo = LocalDateTime.of(2024, 1, 15, 22, 0);
            
            group.setChangeType("TEMPO");
            group.setChangeText("TEMPO 1520/1522 29025G35KT 3SM -RA");
            group.setChangeTimeFrom(timeFrom);
            group.setChangeTimeTo(timeTo);
            group.setWindDirectionDegrees(290);
            group.setWindSpeedKnots(25);
            group.setWindGustKnots(35);
            group.setVisibilityStatuteMiles(3.0);
            group.setWeatherString("-RA");
            group.setSkyCondition("BKN008 OVC015");
            
            assertEquals("TEMPO", group.getChangeType());
            assertEquals("TEMPO 1520/1522 29025G35KT 3SM -RA", group.getChangeText());
            assertEquals(timeFrom, group.getChangeTimeFrom());
            assertEquals(timeTo, group.getChangeTimeTo());
            assertEquals(290, group.getWindDirectionDegrees());
            assertEquals(25, group.getWindSpeedKnots());
            assertEquals(35, group.getWindGustKnots());
            assertEquals(3.0, group.getVisibilityStatuteMiles());
            assertEquals("-RA", group.getWeatherString());
            assertEquals("BKN008 OVC015", group.getSkyCondition());
        }
        
        @Test
        @DisplayName("TafChangeGroup equals and hashCode")
        void tafChangeGroupEqualsAndHashCode() {
            LocalDateTime timeFrom = LocalDateTime.of(2024, 1, 15, 20, 0);
            LocalDateTime timeTo = LocalDateTime.of(2024, 1, 15, 22, 0);
            
            TafChangeGroup group1 = new TafChangeGroup("TEMPO", "TEMPO 1520/1522");
            group1.setChangeTimeFrom(timeFrom);
            group1.setChangeTimeTo(timeTo);
            
            TafChangeGroup group2 = new TafChangeGroup("TEMPO", "TEMPO 1520/1522");
            group2.setChangeTimeFrom(timeFrom);
            group2.setChangeTimeTo(timeTo);
            
            assertEquals(group1, group2);
            assertEquals(group1.hashCode(), group2.hashCode());
            
            // Different change type
            group2.setChangeType("BECMG");
            assertNotEquals(group1, group2);
        }
        
        @Test
        @DisplayName("TafChangeGroup toString")
        void tafChangeGroupToString() {
            LocalDateTime timeFrom = LocalDateTime.of(2024, 1, 15, 20, 0);
            LocalDateTime timeTo = LocalDateTime.of(2024, 1, 15, 22, 0);
            
            TafChangeGroup group = new TafChangeGroup("TEMPO", "TEMPO 1520/1522");
            group.setChangeTimeFrom(timeFrom);
            group.setChangeTimeTo(timeTo);
            
            String result = group.toString();
            
            assertTrue(result.contains("TafChangeGroup"));
            assertTrue(result.contains("TEMPO"));
            assertTrue(result.contains(timeFrom.toString()));
            assertTrue(result.contains(timeTo.toString()));
        }
    }
    
    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {
        
        @Test
        @DisplayName("Objects with same TAF data are equal")
        void equalTafObjects() {
            NoaaTafData taf1 = new NoaaTafData("TAF KJFK 151720Z", "KJFK", testIssueTime);
            taf1.setValidFromTime(testValidFromTime);
            taf1.setValidToTime(testValidToTime);
            taf1.setIssueTime(testIssueTime);
            
            NoaaTafData taf2 = new NoaaTafData("TAF KJFK 151720Z", "KJFK", testIssueTime);
            taf2.setValidFromTime(testValidFromTime);
            taf2.setValidToTime(testValidToTime);
            taf2.setIssueTime(testIssueTime);
            
            assertEquals(taf1, taf2);
            assertEquals(taf1.hashCode(), taf2.hashCode());
        }
        
        @Test
        @DisplayName("Objects with different validity periods are not equal")
        void differentValidityPeriods() {
            NoaaTafData taf1 = new NoaaTafData("TAF KJFK 151720Z", "KJFK", testIssueTime);
            taf1.setValidFromTime(testValidFromTime);
            taf1.setValidToTime(testValidToTime);
            
            NoaaTafData taf2 = new NoaaTafData("TAF KJFK 151720Z", "KJFK", testIssueTime);
            taf2.setValidFromTime(testValidFromTime);
            taf2.setValidToTime(testValidToTime.plusHours(6)); // Different end time
            
            assertNotEquals(taf1, taf2);
        }
        
        @Test
        @DisplayName("Objects with different TAF types are not equal")
        void differentTafTypes() {
            NoaaTafData taf1 = new NoaaTafData("TAF KJFK 151720Z", "KJFK", testIssueTime);
            taf1.setTafType("TAF");
            
            NoaaTafData taf2 = new NoaaTafData("TAF AMD KJFK 151720Z", "KJFK", testIssueTime);
            taf2.setTafType("TAF AMD");
            
            assertNotEquals(taf1, taf2);
        }
        
        @Test
        @DisplayName("Equals calls super.equals() correctly")
        void equalsCallsSuper() {
            // Test that base class fields are also considered in equals
            NoaaTafData taf1 = new NoaaTafData("TAF KJFK 151720Z", "KJFK", testIssueTime);
            NoaaTafData taf2 = new NoaaTafData("TAF KLGA 151720Z", "KLGA", testIssueTime);
            
            assertNotEquals(taf1, taf2); // Different station IDs from base class
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Boundary Values")
    class EdgeCasesAndBoundaryValues {
        
        @Test
        @DisplayName("TAF with very long validity period")
        void tafWithLongValidityPeriod() {
            LocalDateTime from = LocalDateTime.of(2024, 1, 15, 0, 0);
            LocalDateTime to = LocalDateTime.of(2024, 1, 17, 0, 0); // 48 hours
            
            tafData.setValidFromTime(from);
            tafData.setValidToTime(to);
            
            assertEquals(48, tafData.getValidityPeriodHours());
        }
        
        @Test
        @DisplayName("TAF with short validity period")
        void tafWithShortValidityPeriod() {
            LocalDateTime from = LocalDateTime.of(2024, 1, 15, 18, 0);
            LocalDateTime to = LocalDateTime.of(2024, 1, 15, 20, 0); // 2 hours
            
            tafData.setValidFromTime(from);
            tafData.setValidToTime(to);
            
            assertEquals(2, tafData.getValidityPeriodHours());
        }
        
        @Test
        @DisplayName("TAF with many change groups")
        void tafWithManyChangeGroups() {
            for (int i = 0; i < 10; i++) {
                TafChangeGroup group = new TafChangeGroup("TEMPO", "TEMPO GROUP " + i);
                tafData.addChangeGroup(group);
            }
            
            assertEquals(10, tafData.getChangeGroups().size());
        }
        
        @Test
        @DisplayName("Complex TAF type strings")
        void complexTafTypeStrings() {
            String[] complexTypes = {
                "TAF AMD",
                "TAF COR",
                "TAF AMD COR",
                "TAF FCST"
            };
            
            for (String type : complexTypes) {
                tafData.setTafType(type);
                assertEquals(type, tafData.getTafType());
            }
        }
    }
    
    @Nested
    @DisplayName("Real World Data Tests")
    class RealWorldDataTests {
        
        @Test
        @DisplayName("Typical JFK TAF data")
        void typicalJfkTaf() {
            String rawText = "TAF KJFK 151720Z 1518/1624 28016KT P6SM FEW250 " +
                           "TEMPO 1520/1523 29025G35KT 3SM -RA BKN015 OVC030 " +
                           "FM152300 27012KT P6SM SCT030 BKN080";
            
            NoaaTafData taf = new NoaaTafData(rawText, "KJFK", testIssueTime);
            taf.setValidFromTime(LocalDateTime.of(2024, 1, 15, 18, 0));
            taf.setValidToTime(LocalDateTime.of(2024, 1, 16, 18, 0)); // 24-hour TAF
            taf.setIssueTime(LocalDateTime.of(2024, 1, 15, 17, 20));
            
            // Base forecast
            taf.setBaseWindDirectionDegrees(280);
            taf.setBaseWindSpeedKnots(16);
            taf.setBaseVisibilityStatuteMiles(6.0);
            taf.setBaseSkyCondition("FEW250");
            
            // Add change groups
            TafChangeGroup tempo = new TafChangeGroup("TEMPO", "TEMPO 1520/1523 29025G35KT 3SM -RA BKN015 OVC030");
            tempo.setWindDirectionDegrees(290);
            tempo.setWindSpeedKnots(25);
            tempo.setWindGustKnots(35);
            tempo.setVisibilityStatuteMiles(3.0);
            tempo.setWeatherString("-RA");
            tempo.setSkyCondition("BKN015 OVC030");
            taf.addChangeGroup(tempo);
            
            TafChangeGroup fm = new TafChangeGroup("FM", "FM152300 27012KT P6SM SCT030 BKN080");
            fm.setWindDirectionDegrees(270);
            fm.setWindSpeedKnots(12);
            fm.setVisibilityStatuteMiles(6.0);
            fm.setSkyCondition("SCT030 BKN080");
            taf.addChangeGroup(fm);
            
            assertEquals("KJFK", taf.getStationId());
            assertEquals("TAF", taf.getTafType());
            assertEquals(24, taf.getValidityPeriodHours()); // 18:00 to 18:00 next day = 24 hours
            assertEquals(2, taf.getChangeGroups().size());
            assertFalse(taf.isModified());
        }
        
        @Test
        @DisplayName("Amended TAF with probability groups")
        void amendedTafWithProbabilityGroups() {
            String rawText = "TAF AMD KJFK 151800Z 1518/1624 29020KT P6SM FEW180 " +
                           "PROB30 1520/1523 3SM -TSRA BKN020CB " +
                           "PROB40 1600/1606 1SM +TSRA BKN008 OVC020CB";
            
            NoaaTafData taf = new NoaaTafData(rawText, "KJFK", testIssueTime);
            taf.setIsAmended(true);
            
            // Add probability groups
            TafChangeGroup prob30 = new TafChangeGroup("PROB30", "PROB30 1520/1523 3SM -TSRA BKN020CB");
            prob30.setVisibilityStatuteMiles(3.0);
            prob30.setWeatherString("-TSRA");
            prob30.setSkyCondition("BKN020CB");
            taf.addChangeGroup(prob30);
            
            TafChangeGroup prob40 = new TafChangeGroup("PROB40", "PROB40 1600/1606 1SM +TSRA BKN008 OVC020CB");
            prob40.setVisibilityStatuteMiles(1.0);
            prob40.setWeatherString("+TSRA");
            prob40.setSkyCondition("BKN008 OVC020CB");
            taf.addChangeGroup(prob40);
            
            assertEquals("TAF AMD", taf.getTafType());
            assertTrue(taf.isModified());
            assertEquals(2, taf.getChangeGroups().size());
            assertEquals("PROB30", taf.getChangeGroups().get(0).getChangeType());
            assertEquals("PROB40", taf.getChangeGroups().get(1).getChangeType());
        }
        
        @Test
        @DisplayName("Corrected TAF with BECMG group")
        void correctedTafWithBecmgGroup() {
            String rawText = "TAF COR KEWR 151720Z 1518/1618 VRB06KT P6SM SCT250 " +
                           "BECMG 1521/1523 28015KT";
            
            NoaaTafData taf = new NoaaTafData(rawText, "KEWR", testIssueTime);
            taf.setIsCorrected(true);
            
            // Base forecast - variable wind
            taf.setBaseWindDirectionDegrees(null); // Variable wind
            taf.setBaseWindSpeedKnots(6);
            taf.setBaseVisibilityStatuteMiles(6.0);
            taf.setBaseSkyCondition("SCT250");
            
            // BECMG group
            TafChangeGroup becmg = new TafChangeGroup("BECMG", "BECMG 1521/1523 28015KT");
            becmg.setWindDirectionDegrees(280);
            becmg.setWindSpeedKnots(15);
            taf.addChangeGroup(becmg);
            
            assertEquals("TAF COR", taf.getTafType());
            assertTrue(taf.isModified());
            assertTrue(taf.getIsCorrected());
            assertNull(taf.getBaseWindDirectionDegrees()); // Variable wind
            assertEquals(1, taf.getChangeGroups().size());
        }
    }
    
    @Nested
    @DisplayName("Integration with Base Class")
    class IntegrationWithBaseClass {
        
        @Test
        @DisplayName("Base class fields work correctly in TAF context")
        void baseClassFieldsInTafContext() {
            tafData.setStationId("KJFK");
            tafData.setObservationTime(testIssueTime);
            tafData.setLatitude(40.6413);
            tafData.setLongitude(-73.7781);
            tafData.setElevationFeet(13);
            tafData.setQualityControlFlags("AUTO");
            
            // Set TAF-specific times for isCurrent test
            tafData.setValidFromTime(LocalDateTime.now().minusHours(1));
            tafData.setValidToTime(LocalDateTime.now().plusHours(23));
            
            // Verify base class fields
            assertEquals("KJFK", tafData.getStationId());
            assertEquals(testIssueTime, tafData.getObservationTime());
            assertEquals(40.6413, tafData.getLatitude());
            assertEquals(-73.7781, tafData.getLongitude());
            assertEquals(13, tafData.getElevationFeet());
            assertEquals("AUTO", tafData.getQualityControlFlags());
            
            // Verify TAF-specific behavior
            assertTrue(tafData.isCurrent());
            assertEquals("TAF", tafData.getReportType());
        }
        
        @Test
        @DisplayName("toString includes both base and TAF fields")
        void toStringIncludesBothTypes() {
            tafData.setStationId("KJFK");
            tafData.setObservationTime(testIssueTime);
            tafData.setRawText("TAF KJFK 151720Z 1518/1624");
            tafData.setValidFromTime(testValidFromTime);
            tafData.setValidToTime(testValidToTime);
            tafData.setTafType("TAF");
            
            String result = tafData.toString();
            
            assertTrue(result.contains("NoaaTafData"));
            assertTrue(result.contains("KJFK"));
            assertTrue(result.contains("TAF"));
        }
    }
}
