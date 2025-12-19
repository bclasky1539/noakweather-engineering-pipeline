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
package weather.model.components.remark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import weather.model.components.Pressure;
import weather.model.components.Temperature;
import weather.model.components.Visibility;
import weather.model.enums.AutomatedStationType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NoaaMetarRemarks.
 *
 * @author bclasky1539
 *
 */
class NoaaMetarRemarksTest {

    @Test
    void testEmptyRemarks() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.empty();

        assertNull(remarks.automatedStationType());
        assertNull(remarks.seaLevelPressure());
        assertNull(remarks.preciseTemperature());
        assertNull(remarks.preciseDewpoint());
        assertNull(remarks.freeText());
        assertTrue(remarks.isEmpty());
    }

    @Test
    void testBuilder() {
        AutomatedStationType stationType = AutomatedStationType.AO2;
        Pressure slp = Pressure.hectopascals(1013.2);
        Temperature temp = Temperature.of(22.2);
        Temperature dewpoint = Temperature.of(11.7);
        String freeText = "Some unparsed text";

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(stationType)
                .seaLevelPressure(slp)
                .preciseTemperature(temp)
                .preciseDewpoint(dewpoint)
                .freeText(freeText)
                .build();

        assertEquals(stationType, remarks.automatedStationType());
        assertEquals(slp, remarks.seaLevelPressure());
        assertEquals(temp, remarks.preciseTemperature());
        assertEquals(dewpoint, remarks.preciseDewpoint());
        assertEquals(freeText, remarks.freeText());
        assertFalse(remarks.isEmpty());
    }

    @Test
    void testBuilderPartial() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO1)
                .build();

        assertEquals(AutomatedStationType.AO1, remarks.automatedStationType());
        assertNull(remarks.seaLevelPressure());
        assertNull(remarks.preciseTemperature());
        assertNull(remarks.preciseDewpoint());
        assertNull(remarks.freeText());
        assertFalse(remarks.isEmpty());
    }

    @Test
    void testHasPrecipitationDiscriminatorAO2() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .build();

        assertTrue(remarks.hasPrecipitationDiscriminator());
    }

    @Test
    void testHasPrecipitationDiscriminatorAO1() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO1)
                .build();

        assertFalse(remarks.hasPrecipitationDiscriminator());
    }

    @Test
    void testHasPrecipitationDiscriminatorNull() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .build();

        assertFalse(remarks.hasPrecipitationDiscriminator());
    }

    @Test
    void testIsEmptyWithAllFields() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .seaLevelPressure(Pressure.hectopascals(1013.2))
                .preciseTemperature(Temperature.of(22.2))
                .preciseDewpoint(Temperature.of(11.7))
                .freeText("Some text")
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    void testIsEmptyWithBlankFreeText() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .freeText("   ")
                .build();

        assertTrue(remarks.isEmpty());
    }

    @Test
    void testRecordConstructor() {
        AutomatedStationType stationType = AutomatedStationType.AO2;
        Pressure slp = Pressure.hectopascals(1013.2);
        Temperature temp = Temperature.of(22.2);
        Temperature dewpoint = Temperature.of(11.7);
        String freeText = "Text";

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                stationType, slp, temp, dewpoint, null, null, null, null,
                null, null, null, null,
                null,freeText
        );

        assertEquals(stationType, remarks.automatedStationType());
        assertEquals(slp, remarks.seaLevelPressure());
        assertEquals(temp, remarks.preciseTemperature());
        assertEquals(dewpoint, remarks.preciseDewpoint());
        assertEquals(freeText, remarks.freeText());
    }

    @Test
    void testToStringEmpty() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.empty();
        String str = remarks.toString();

        assertTrue(str.contains("empty"));
    }

    @Test
    void testToStringWithStationType() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("AO2"));
    }

    @Test
    void testEquality() {
        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .seaLevelPressure(Pressure.hectopascals(1013.2))
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .seaLevelPressure(Pressure.hectopascals(1013.2))
                .build();

        assertEquals(remarks1, remarks2);
        assertEquals(remarks1.hashCode(), remarks2.hashCode());
    }

    @Test
    void testInequality() {
        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO1)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .build();

        assertNotEquals(remarks1, remarks2);
    }

    // Add these tests to NoaaMetarRemarksTest.java

// ========== Tests for hasFrontalPassage() ==========

    @Test
    void testHasFrontalPassageTrue() {
        WindShift windShiftWithFropa = new WindShift(15, 30, true);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .windShift(windShiftWithFropa)
                .build();

        assertTrue(remarks.hasFrontalPassage());
    }

    @Test
    void testHasFrontalPassageFalse() {
        WindShift windShiftNoFropa = new WindShift(15, 30, false);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .windShift(windShiftNoFropa)
                .build();

        assertFalse(remarks.hasFrontalPassage());
    }

    @Test
    void testHasFrontalPassageNullWindShift() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .build();

        assertFalse(remarks.hasFrontalPassage());
    }

// ========== Tests for isEmpty() edge cases ==========

    @Test
    void testIsEmptyWithOnlySeaLevelPressure() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .seaLevelPressure(Pressure.hectopascals(1013.2))
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    void testIsEmptyWithOnlyPreciseTemperature() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .preciseTemperature(Temperature.of(22.2))
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    void testIsEmptyWithOnlyPreciseDewpoint() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .preciseDewpoint(Temperature.of(11.7))
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    void testIsEmptyWithOnlyPeakWind() {
        PeakWind peakWind = new PeakWind(280, 32, 15, 30);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .peakWind(peakWind)
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    void testIsEmptyWithOnlyWindShift() {
        WindShift windShift = new WindShift(15, 30, false);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .windShift(windShift)
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    void testIsEmptyWithNonBlankFreeText() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .freeText("Some text")
                .build();

        assertFalse(remarks.isEmpty());
    }

// ========== Tests for record constructor with all field combinations ==========

    @Test
    void testRecordConstructorWithPeakWind() {
        PeakWind peakWind = new PeakWind(280, 32, 15, 30);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, peakWind,
                null, null, null, null, null,
                null, null, null, null
        );

        assertEquals(peakWind, remarks.peakWind());
        assertNull(remarks.windShift());
    }

    @Test
    void testRecordConstructorWithWindShift() {
        WindShift windShift = new WindShift(15, 30, true);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                windShift, null, null, null, null,
                null, null, null, null
        );

        assertNull(remarks.peakWind());
        assertEquals(windShift, remarks.windShift());
    }

    // ========== Tests for toString() with PeakWind and WindShift ==========

    @Test
    void testToStringWithPeakWind() {
        PeakWind peakWind = new PeakWind(280, 32, 15, 30);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .peakWind(peakWind)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("peakWind"));
    }

    @Test
    void testToStringWithWindShift() {
        WindShift windShift = new WindShift(15, 30, true);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .windShift(windShift)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("windShift"));
    }

    // ========== Tests for isEmpty() with VariableVisibility ==========

    @Test
    @DisplayName("Should not be empty when only variableVisibility is present")
    void testIsEmptyWithOnlyVariableVisibility() {
        Visibility min = Visibility.statuteMiles(0.5);
        Visibility max = Visibility.statuteMiles(2.0);
        VariableVisibility varVis = new VariableVisibility(min, max, null, null);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .variableVisibility(varVis)
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should be empty when variableVisibility is null")
    void testIsEmptyWithNullVariableVisibility() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .variableVisibility(null)
                .build();

        assertTrue(remarks.isEmpty());
    }

    // ========== Tests for record constructor with VariableVisibility ==========

    @Test
    @DisplayName("Should create remarks with variableVisibility via record constructor")
    void testRecordConstructorWithVariableVisibility() {
        Visibility min = Visibility.statuteMiles(1.0);
        Visibility max = Visibility.statuteMiles(3.0);
        VariableVisibility varVis = new VariableVisibility(min, max, "NE", null);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, varVis, null, null, null,
                null, null, null, null
        );

        assertEquals(varVis, remarks.variableVisibility());
        assertNull(remarks.peakWind());
        assertNull(remarks.windShift());
    }

    // ========== Tests for toString() with VariableVisibility ==========

    @Test
    @DisplayName("Should include variableVisibility in toString()")
    void testToStringWithVariableVisibility() {
        Visibility min = Visibility.statuteMiles(1.0);
        Visibility max = Visibility.statuteMiles(3.0);
        VariableVisibility varVis = new VariableVisibility(min, max, "SW", null);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .variableVisibility(varVis)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("variableVisibility"));
    }

    // ========== Tests for builder with VariableVisibility ==========

    @Test
    @DisplayName("Should build remarks with variableVisibility via builder")
    void testBuilderWithVariableVisibility() {
        Visibility min = Visibility.statuteMiles(1.0);
        Visibility max = Visibility.statuteMiles(4.0);
        VariableVisibility varVis = VariableVisibility.withDirection(min, max, "NE");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .variableVisibility(varVis)
                .build();

        assertEquals(varVis, remarks.variableVisibility());
        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should build remarks with multiple fields including variableVisibility")
    void testBuilderWithMultipleFieldsIncludingVariableVisibility() {
        AutomatedStationType stationType = AutomatedStationType.AO2;
        Pressure slp = Pressure.hectopascals(1013.2);
        VariableVisibility varVis = VariableVisibility.of(
                Visibility.statuteMiles(0.25),
                Visibility.statuteMiles(1.0)
        );

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(stationType)
                .seaLevelPressure(slp)
                .variableVisibility(varVis)
                .build();

        assertEquals(stationType, remarks.automatedStationType());
        assertEquals(slp, remarks.seaLevelPressure());
        assertEquals(varVis, remarks.variableVisibility());
        assertNull(remarks.preciseTemperature());
        assertNull(remarks.freeText());
        assertFalse(remarks.isEmpty());
    }

    // ========== Tests for equality with VariableVisibility ==========

    @Test
    @DisplayName("Should be equal when variableVisibility is the same")
    void testEqualityWithVariableVisibility() {
        VariableVisibility varVis = VariableVisibility.of(
                Visibility.statuteMiles(1.0),
                Visibility.statuteMiles(3.0)
        );

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .variableVisibility(varVis)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .variableVisibility(varVis)
                .build();

        assertEquals(remarks1, remarks2);
        assertEquals(remarks1.hashCode(), remarks2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when variableVisibility differs")
    void testInequalityWithDifferentVariableVisibility() {
        VariableVisibility varVis1 = VariableVisibility.of(
                Visibility.statuteMiles(1.0),
                Visibility.statuteMiles(2.0)
        );

        VariableVisibility varVis2 = VariableVisibility.of(
                Visibility.statuteMiles(2.0),
                Visibility.statuteMiles(4.0)
        );

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .variableVisibility(varVis1)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .variableVisibility(varVis2)
                .build();

        assertNotEquals(remarks1, remarks2);
    }

    // ========== Edge case tests ==========

    @Test
    @DisplayName("Should handle variableVisibility with direction in toString()")
    void testToStringWithVariableVisibilityWithDirection() {
        VariableVisibility varVis = VariableVisibility.withDirection(
                Visibility.statuteMiles(1.0),
                Visibility.statuteMiles(3.0),
                "SE"
        );

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .variableVisibility(varVis)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("variableVisibility"));
        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should handle variableVisibility with location in toString()")
    void testToStringWithVariableVisibilityWithLocation() {
        VariableVisibility varVis = VariableVisibility.withLocation(
                Visibility.statuteMiles(0.25),
                Visibility.statuteMiles(1.0),
                "RWY"
        );

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .variableVisibility(varVis)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("variableVisibility"));
        assertFalse(remarks.isEmpty());
    }

    // ========== Tests for isEmpty() with Tower/Surface Visibility ==========

    @Test
    @DisplayName("Should not be empty when only towerVisibility is present")
    void testIsEmptyWithOnlyTowerVisibility() {
        Visibility towerVis = Visibility.statuteMiles(1.5);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .towerVisibility(towerVis)
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should not be empty when only surfaceVisibility is present")
    void testIsEmptyWithOnlySurfaceVisibility() {
        Visibility surfaceVis = Visibility.statuteMiles(0.25);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .surfaceVisibility(surfaceVis)
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should be empty when tower and surface visibility are null")
    void testIsEmptyWithNullTowerAndSurfaceVisibility() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .towerVisibility(null)
                .surfaceVisibility(null)
                .build();

        assertTrue(remarks.isEmpty());
    }


// ========== Tests for record constructor with Tower/Surface Visibility ==========

    @Test
    @DisplayName("Should create remarks with towerVisibility via record constructor")
    void testRecordConstructorWithTowerVisibility() {
        Visibility towerVis = Visibility.statuteMiles(1.5);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, towerVis, null, null,
                null, null, null, null
        );

        assertEquals(towerVis, remarks.towerVisibility());
        assertNull(remarks.surfaceVisibility());
    }

    @Test
    @DisplayName("Should create remarks with surfaceVisibility via record constructor")
    void testRecordConstructorWithSurfaceVisibility() {
        Visibility surfaceVis = Visibility.statuteMiles(0.25);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, surfaceVis, null,
                null, null, null, null
        );

        assertNull(remarks.towerVisibility());
        assertEquals(surfaceVis, remarks.surfaceVisibility());
    }

    @Test
    @DisplayName("Should create remarks with both tower and surface visibility")
    void testRecordConstructorWithBothTowerAndSurfaceVisibility() {
        Visibility towerVis = Visibility.statuteMiles(2.0);
        Visibility surfaceVis = Visibility.statuteMiles(1.0);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, towerVis, surfaceVis, null,
                null, null, null,null
        );

        assertEquals(towerVis, remarks.towerVisibility());
        assertEquals(surfaceVis, remarks.surfaceVisibility());
    }


// ========== Tests for builder with Tower/Surface Visibility ==========

    @Test
    @DisplayName("Should build remarks with towerVisibility via builder")
    void testBuilderWithTowerVisibility() {
        Visibility towerVis = Visibility.statuteMiles(1.5);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .towerVisibility(towerVis)
                .build();

        assertEquals(towerVis, remarks.towerVisibility());
        assertNull(remarks.surfaceVisibility());
        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should build remarks with surfaceVisibility via builder")
    void testBuilderWithSurfaceVisibility() {
        Visibility surfaceVis = Visibility.statuteMiles(0.5);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .surfaceVisibility(surfaceVis)
                .build();

        assertNull(remarks.towerVisibility());
        assertEquals(surfaceVis, remarks.surfaceVisibility());
        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should build remarks with multiple fields including tower visibility")
    void testBuilderWithMultipleFieldsIncludingTowerVisibility() {
        AutomatedStationType stationType = AutomatedStationType.AO2;
        Pressure slp = Pressure.hectopascals(1013.2);
        Visibility towerVis = Visibility.statuteMiles(2.0);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(stationType)
                .seaLevelPressure(slp)
                .towerVisibility(towerVis)
                .build();

        assertEquals(stationType, remarks.automatedStationType());
        assertEquals(slp, remarks.seaLevelPressure());
        assertEquals(towerVis, remarks.towerVisibility());
        assertNull(remarks.surfaceVisibility());
        assertFalse(remarks.isEmpty());
    }

    // ========== Tests for toString() with Tower/Surface Visibility ==========

    @Test
    @DisplayName("Should include towerVisibility in toString()")
    void testToStringWithTowerVisibility() {
        Visibility towerVis = Visibility.statuteMiles(1.5);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .towerVisibility(towerVis)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("towerVisibility"));
    }

    @Test
    @DisplayName("Should include surfaceVisibility in toString()")
    void testToStringWithSurfaceVisibility() {
        Visibility surfaceVis = Visibility.statuteMiles(0.25);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .surfaceVisibility(surfaceVis)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("surfaceVisibility"));
    }

    @Test
    @DisplayName("Should include both tower and surface visibility in toString()")
    void testToStringWithBothTowerAndSurfaceVisibility() {
        Visibility towerVis = Visibility.statuteMiles(2.0);
        Visibility surfaceVis = Visibility.statuteMiles(1.0);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .towerVisibility(towerVis)
                .surfaceVisibility(surfaceVis)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("towerVisibility"));
        assertTrue(str.contains("surfaceVisibility"));
    }


    // ========== Tests for equality with Tower/Surface Visibility ==========

    @Test
    @DisplayName("Should be equal when towerVisibility is the same")
    void testEqualityWithTowerVisibility() {
        Visibility towerVis = Visibility.statuteMiles(1.5);

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .towerVisibility(towerVis)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .towerVisibility(towerVis)
                .build();

        assertEquals(remarks1, remarks2);
        assertEquals(remarks1.hashCode(), remarks2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when towerVisibility differs")
    void testInequalityWithDifferentTowerVisibility() {
        Visibility towerVis1 = Visibility.statuteMiles(1.5);
        Visibility towerVis2 = Visibility.statuteMiles(2.0);

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .towerVisibility(towerVis1)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .towerVisibility(towerVis2)
                .build();

        assertNotEquals(remarks1, remarks2);
    }

    // ========== Tests for isEmpty() with Precipitation ==========

    @Test
    @DisplayName("Should not be empty when only hourlyPrecipitation is present")
    void testIsEmptyWithOnlyHourlyPrecipitation() {
        PrecipitationAmount hourly = PrecipitationAmount.fromEncoded("0015", 1);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .hourlyPrecipitation(hourly)
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should not be empty when only sixHourPrecipitation is present")
    void testIsEmptyWithOnlySixHourPrecipitation() {
        PrecipitationAmount sixHour = PrecipitationAmount.fromEncoded("0025", 6);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .sixHourPrecipitation(sixHour)
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should not be empty when only twentyFourHourPrecipitation is present")
    void testIsEmptyWithOnlyTwentyFourHourPrecipitation() {
        PrecipitationAmount twentyFourHour = PrecipitationAmount.fromEncoded("0125", 24);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .twentyFourHourPrecipitation(twentyFourHour)
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should be empty when all precipitation fields are null")
    void testIsEmptyWithNullPrecipitation() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .hourlyPrecipitation(null)
                .sixHourPrecipitation(null)
                .twentyFourHourPrecipitation(null)
                .build();

        assertTrue(remarks.isEmpty());
    }


    // ========== Tests for record constructor with Precipitation ==========

    @Test
    @DisplayName("Should create remarks with hourlyPrecipitation via record constructor")
    void testRecordConstructorWithHourlyPrecipitation() {
        PrecipitationAmount hourly = PrecipitationAmount.fromEncoded("0015", 1);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, null, hourly, null,
                null, null, null
        );

        assertEquals(hourly, remarks.hourlyPrecipitation());
        assertNull(remarks.sixHourPrecipitation());
        assertNull(remarks.twentyFourHourPrecipitation());
    }

    @Test
    @DisplayName("Should create remarks with sixHourPrecipitation via record constructor")
    void testRecordConstructorWithSixHourPrecipitation() {
        PrecipitationAmount sixHour = PrecipitationAmount.fromEncoded("0025", 6);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, null, null, sixHour,
                null, null, null
        );

        assertNull(remarks.hourlyPrecipitation());
        assertEquals(sixHour, remarks.sixHourPrecipitation());
        assertNull(remarks.twentyFourHourPrecipitation());
    }

    @Test
    @DisplayName("Should create remarks with twentyFourHourPrecipitation via record constructor")
    void testRecordConstructorWithTwentyFourHourPrecipitation() {
        PrecipitationAmount twentyFourHour = PrecipitationAmount.fromEncoded("0125", 24);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, null, null,
                null, twentyFourHour, null, null
        );

        assertNull(remarks.hourlyPrecipitation());
        assertNull(remarks.sixHourPrecipitation());
        assertEquals(twentyFourHour, remarks.twentyFourHourPrecipitation());
    }

    @Test
    @DisplayName("Should create remarks with all precipitation types")
    void testRecordConstructorWithAllPrecipitationTypes() {
        PrecipitationAmount hourly = PrecipitationAmount.fromEncoded("0015", 1);
        PrecipitationAmount sixHour = PrecipitationAmount.fromEncoded("0025", 6);
        PrecipitationAmount twentyFourHour = PrecipitationAmount.fromEncoded("0125", 24);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, null,
                hourly, sixHour, twentyFourHour, null, null
        );

        assertEquals(hourly, remarks.hourlyPrecipitation());
        assertEquals(sixHour, remarks.sixHourPrecipitation());
        assertEquals(twentyFourHour, remarks.twentyFourHourPrecipitation());
    }


    // ========== Tests for builder with Precipitation ==========

    @Test
    @DisplayName("Should build remarks with hourlyPrecipitation via builder")
    void testBuilderWithHourlyPrecipitation() {
        PrecipitationAmount hourly = PrecipitationAmount.fromEncoded("0015", 1);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .hourlyPrecipitation(hourly)
                .build();

        assertEquals(hourly, remarks.hourlyPrecipitation());
        assertNull(remarks.sixHourPrecipitation());
        assertNull(remarks.twentyFourHourPrecipitation());
        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should build remarks with sixHourPrecipitation via builder")
    void testBuilderWithSixHourPrecipitation() {
        PrecipitationAmount sixHour = PrecipitationAmount.fromEncoded("0025", 6);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .sixHourPrecipitation(sixHour)
                .build();

        assertNull(remarks.hourlyPrecipitation());
        assertEquals(sixHour, remarks.sixHourPrecipitation());
        assertNull(remarks.twentyFourHourPrecipitation());
        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should build remarks with twentyFourHourPrecipitation via builder")
    void testBuilderWithTwentyFourHourPrecipitation() {
        PrecipitationAmount twentyFourHour = PrecipitationAmount.fromEncoded("0125", 24);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .twentyFourHourPrecipitation(twentyFourHour)
                .build();

        assertNull(remarks.hourlyPrecipitation());
        assertNull(remarks.sixHourPrecipitation());
        assertEquals(twentyFourHour, remarks.twentyFourHourPrecipitation());
        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should build remarks with multiple fields including precipitation")
    void testBuilderWithMultipleFieldsIncludingPrecipitation() {
        AutomatedStationType stationType = AutomatedStationType.AO2;
        Pressure slp = Pressure.hectopascals(1013.2);
        PrecipitationAmount hourly = PrecipitationAmount.fromEncoded("0015", 1);
        PrecipitationAmount sixHour = PrecipitationAmount.fromEncoded("0025", 6);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(stationType)
                .seaLevelPressure(slp)
                .hourlyPrecipitation(hourly)
                .sixHourPrecipitation(sixHour)
                .build();

        assertEquals(stationType, remarks.automatedStationType());
        assertEquals(slp, remarks.seaLevelPressure());
        assertEquals(hourly, remarks.hourlyPrecipitation());
        assertEquals(sixHour, remarks.sixHourPrecipitation());
        assertNull(remarks.twentyFourHourPrecipitation());
        assertFalse(remarks.isEmpty());
    }


    // ========== Tests for toString() with Precipitation ==========

    @Test
    @DisplayName("Should include hourlyPrecipitation in toString()")
    void testToStringWithHourlyPrecipitation() {
        PrecipitationAmount hourly = PrecipitationAmount.fromEncoded("0015", 1);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .hourlyPrecipitation(hourly)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("hourlyPrecip"));
    }

    @Test
    @DisplayName("Should include sixHourPrecipitation in toString()")
    void testToStringWithSixHourPrecipitation() {
        PrecipitationAmount sixHour = PrecipitationAmount.fromEncoded("0025", 6);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .sixHourPrecipitation(sixHour)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("sixHourPrecip"));
    }

    @Test
    @DisplayName("Should include twentyFourHourPrecipitation in toString()")
    void testToStringWithTwentyFourHourPrecipitation() {
        PrecipitationAmount twentyFourHour = PrecipitationAmount.fromEncoded("0125", 24);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .twentyFourHourPrecipitation(twentyFourHour)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("twentyFourHourPrecip"));
    }

    @Test
    @DisplayName("Should include all precipitation types in toString()")
    void testToStringWithAllPrecipitationTypes() {
        PrecipitationAmount hourly = PrecipitationAmount.fromEncoded("0015", 1);
        PrecipitationAmount sixHour = PrecipitationAmount.fromEncoded("0025", 6);
        PrecipitationAmount twentyFourHour = PrecipitationAmount.fromEncoded("0125", 24);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .hourlyPrecipitation(hourly)
                .sixHourPrecipitation(sixHour)
                .twentyFourHourPrecipitation(twentyFourHour)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("hourlyPrecip"));
        assertTrue(str.contains("sixHourPrecip"));
        assertTrue(str.contains("twentyFourHourPrecip"));
    }


    // ========== Tests for equality with Precipitation ==========

    @Test
    @DisplayName("Should be equal when hourlyPrecipitation is the same")
    void testEqualityWithHourlyPrecipitation() {
        PrecipitationAmount hourly = PrecipitationAmount.fromEncoded("0015", 1);

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .hourlyPrecipitation(hourly)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .hourlyPrecipitation(hourly)
                .build();

        assertEquals(remarks1, remarks2);
        assertEquals(remarks1.hashCode(), remarks2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when hourlyPrecipitation differs")
    void testInequalityWithDifferentHourlyPrecipitation() {
        PrecipitationAmount hourly1 = PrecipitationAmount.fromEncoded("0015", 1);
        PrecipitationAmount hourly2 = PrecipitationAmount.fromEncoded("0025", 1);

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .hourlyPrecipitation(hourly1)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .hourlyPrecipitation(hourly2)
                .build();

        assertNotEquals(remarks1, remarks2);
    }


    // ========== Tests with Trace Precipitation ==========

    @Test
    @DisplayName("Should handle trace hourly precipitation")
    void testTraceHourlyPrecipitation() {
        PrecipitationAmount trace = PrecipitationAmount.trace(1);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .hourlyPrecipitation(trace)
                .build();

        assertEquals(trace, remarks.hourlyPrecipitation());
        assertTrue(remarks.hourlyPrecipitation().isTrace());
        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should handle trace 6-hour precipitation")
    void testTraceSixHourPrecipitation() {
        PrecipitationAmount trace = PrecipitationAmount.trace(6);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .sixHourPrecipitation(trace)
                .build();

        assertEquals(trace, remarks.sixHourPrecipitation());
        assertTrue(remarks.sixHourPrecipitation().isTrace());
    }

    // ========== Tests for isEmpty() with Hail Size ==========

    @Test
    @DisplayName("Should not be empty when only hailSize is present")
    void testIsEmptyWithOnlyHailSize() {
        HailSize hailSize = HailSize.inches(1.75);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .hailSize(hailSize)
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should be empty when hailSize is null")
    void testIsEmptyWithNullHailSize() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .hailSize(null)
                .build();

        assertTrue(remarks.isEmpty());
    }

    // ========== Tests for record constructor with Hail Size ==========

    @Test
    @DisplayName("Should create remarks with hailSize via record constructor")
    void testRecordConstructorWithHailSize() {
        HailSize hailSize = HailSize.inches(1.75);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, hailSize, null
        );

        assertEquals(hailSize, remarks.hailSize());
    }

    @Test
    @DisplayName("Should create remarks with null hailSize via record constructor")
    void testRecordConstructorWithNullHailSize() {
        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null
        );

        assertNull(remarks.hailSize());
        assertTrue(remarks.isEmpty());
    }

    // ========== Tests for builder with Hail Size ==========

    @Test
    @DisplayName("Should build remarks with hailSize via builder")
    void testBuilderWithHailSize() {
        HailSize hailSize = HailSize.inches(1.75);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .hailSize(hailSize)
                .build();

        assertEquals(hailSize, remarks.hailSize());
        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should build remarks with multiple fields including hailSize")
    void testBuilderWithMultipleFieldsIncludingHailSize() {
        AutomatedStationType stationType = AutomatedStationType.AO2;
        Pressure slp = Pressure.hectopascals(1013.2);
        HailSize hailSize = HailSize.inches(2.0);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(stationType)
                .seaLevelPressure(slp)
                .hailSize(hailSize)
                .build();

        assertEquals(stationType, remarks.automatedStationType());
        assertEquals(slp, remarks.seaLevelPressure());
        assertEquals(hailSize, remarks.hailSize());
        assertFalse(remarks.isEmpty());
    }

    // ========== Tests for toString() with Hail Size ==========

    @Test
    @DisplayName("Should include hailSize in toString()")
    void testToStringWithHailSize() {
        HailSize hailSize = HailSize.inches(1.75);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .hailSize(hailSize)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("hailSize"));
    }

    @Test
    @DisplayName("Should show hail size details in toString()")
    void testToStringHailSizeDetails() {
        HailSize hailSize = HailSize.inches(2.5);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .hailSize(hailSize)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("hailSize"));
        // Should contain summary which includes size
        assertTrue(str.contains("2.50") || str.contains("2.5"));
    }

    // ========== Tests for equality with Hail Size ==========

    @Test
    @DisplayName("Should be equal when hailSize is the same")
    void testEqualityWithHailSize() {
        HailSize hailSize = HailSize.inches(1.75);

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .hailSize(hailSize)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .hailSize(hailSize)
                .build();

        assertEquals(remarks1, remarks2);
        assertEquals(remarks1.hashCode(), remarks2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when hailSize differs")
    void testInequalityWithDifferentHailSize() {
        HailSize hailSize1 = HailSize.inches(1.75);
        HailSize hailSize2 = HailSize.inches(2.00);

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .hailSize(hailSize1)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .hailSize(hailSize2)
                .build();

        assertNotEquals(remarks1, remarks2);
    }


// ========== Tests with Different Hail Sizes ==========

    @Test
    @DisplayName("Should handle severe hail size")
    void testSevereHailSize() {
        HailSize hailSize = HailSize.inches(1.0);  // Severe threshold

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .hailSize(hailSize)
                .build();

        assertEquals(hailSize, remarks.hailSize());
        assertTrue(remarks.hailSize().isSevere());
    }

    @Test
    @DisplayName("Should handle significantly severe hail size")
    void testSignificantlySevereHailSize() {
        HailSize hailSize = HailSize.inches(2.5);  // Significantly severe

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .hailSize(hailSize)
                .build();

        assertEquals(hailSize, remarks.hailSize());
        assertTrue(remarks.hailSize().isSignificantlySevere());
    }

    @Test
    @DisplayName("Should handle small hail size")
    void testSmallHailSize() {
        HailSize hailSize = HailSize.inches(0.5);  // Small, non-severe

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .hailSize(hailSize)
                .build();

        assertEquals(hailSize, remarks.hailSize());
        assertFalse(remarks.hailSize().isSevere());
    }

    @Test
    @DisplayName("Should include all fields in toString()")
    void testToStringWithAllFields() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .seaLevelPressure(Pressure.hectopascals(1013.2))
                .preciseTemperature(Temperature.of(22.2))
                .preciseDewpoint(Temperature.of(11.7))
                .peakWind(new PeakWind(280, 32, 15, 30))
                .windShift(new WindShift(15, 30, true))
                .variableVisibility(VariableVisibility.of(
                        Visibility.statuteMiles(0.5),
                        Visibility.statuteMiles(2.0)
                ))
                .towerVisibility(Visibility.statuteMiles(1.5))
                .surfaceVisibility(Visibility.statuteMiles(0.75))
                .hourlyPrecipitation(PrecipitationAmount.fromEncoded("0015", 1))
                .sixHourPrecipitation(PrecipitationAmount.fromEncoded("0025", 6))
                .twentyFourHourPrecipitation(PrecipitationAmount.fromEncoded("0125", 24))
                .hailSize(HailSize.inches(1.75))
                .freeText("Additional info")
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("AO2"));
        assertTrue(str.contains("seaLevelPressure"));
        assertTrue(str.contains("preciseTemp"));
        assertTrue(str.contains("preciseDewpoint"));
        assertTrue(str.contains("peakWind"));
        assertTrue(str.contains("windShift"));
        assertTrue(str.contains("variableVisibility"));
        assertTrue(str.contains("towerVisibility"));
        assertTrue(str.contains("surfaceVisibility"));
        assertTrue(str.contains("hourlyPrecip"));
        assertTrue(str.contains("sixHourPrecip"));
        assertTrue(str.contains("twentyFourHourPrecip"));
        assertTrue(str.contains("hailSize"));
        assertTrue(str.contains("freeText"));
    }

    @Test
    @DisplayName("Should create remarks with all fields")
    void testRecordConstructorWithAllFields() {
        AutomatedStationType stationType = AutomatedStationType.AO2;
        Pressure slp = Pressure.hectopascals(1013.2);
        Temperature temp = Temperature.of(22.2);
        Temperature dewpoint = Temperature.of(11.7);
        PeakWind peakWind = new PeakWind(280, 32, 15, 30);
        WindShift windShift = new WindShift(15, 30, true);
        Visibility min = Visibility.statuteMiles(0.5);
        Visibility max = Visibility.statuteMiles(2.0);
        VariableVisibility varVis = new VariableVisibility(min, max, null, null);
        Visibility towerVis = Visibility.statuteMiles(1.5);
        Visibility surfaceVis = Visibility.statuteMiles(0.75);
        PrecipitationAmount hourly = PrecipitationAmount.fromEncoded("0015", 1);
        PrecipitationAmount sixHour = PrecipitationAmount.fromEncoded("0025", 6);
        PrecipitationAmount twentyFourHour = PrecipitationAmount.fromEncoded("0125", 24);
        HailSize hailSize = HailSize.inches(1.75);
        String freeText = "Additional remarks";

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                stationType, slp, temp, dewpoint, peakWind, windShift, varVis,
                towerVis, surfaceVis, hourly, sixHour, twentyFourHour, hailSize, freeText
        );

        assertEquals(stationType, remarks.automatedStationType());
        assertEquals(slp, remarks.seaLevelPressure());
        assertEquals(temp, remarks.preciseTemperature());
        assertEquals(dewpoint, remarks.preciseDewpoint());
        assertEquals(peakWind, remarks.peakWind());
        assertEquals(windShift, remarks.windShift());
        assertEquals(varVis, remarks.variableVisibility());
        assertEquals(towerVis, remarks.towerVisibility());
        assertEquals(surfaceVis, remarks.surfaceVisibility());
        assertEquals(hourly, remarks.hourlyPrecipitation());
        assertEquals(sixHour, remarks.sixHourPrecipitation());
        assertEquals(twentyFourHour, remarks.twentyFourHourPrecipitation());
        assertEquals(hailSize, remarks.hailSize());
        assertEquals(freeText, remarks.freeText());
    }
}
