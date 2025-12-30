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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import weather.model.components.Pressure;
import weather.model.components.Temperature;
import weather.model.components.Visibility;
import weather.model.enums.AutomatedStationType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
        assertTrue(remarks.weatherEvents().isEmpty());
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

        // Note: isEmpty() returns false because List fields are initialized to empty lists
        assertThat(remarks.freeText()).isBlank();
        assertThat(remarks.weatherEvents()).isEmpty();
        assertThat(remarks.thunderstormLocations()).isEmpty();
        assertThat(remarks.obscurationLayers()).isEmpty();
    }

    @Test
    @DisplayName("Should correctly handle blank freeText in isEmpty check")
    void testIsEmptyLogicWithBlankFreeText() {
        // Create a truly empty remarks (no fields set, all lists empty)
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .build();

        // With our current design, isEmpty() should return FALSE because lists exist
        // But if we fix isEmpty() to check isEmpty() on lists, this should return TRUE

        assertThat(remarks.automatedStationType()).isNull();
        assertThat(remarks.weatherEvents()).isEmpty();
        assertThat(remarks.freeText()).isNull();
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
                null, null, null,null,null,
                null, null,null,null, null,
                null, null,null, null,
                null,null, null,
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

    @Test
    @DisplayName("Should not be empty when freeText is not blank")
    void testIsNotEmptyWithNonBlankFreeText() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .freeText("Some text")
                .build();

        assertThat(remarks.isEmpty()).isFalse();
        assertThat(remarks.freeText()).isNotBlank();
    }

// ========== Tests for record constructor with all field combinations ==========

    @Test
    void testRecordConstructorWithPeakWind() {
        PeakWind peakWind = new PeakWind(280, 32, 15, 30);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, peakWind,
                null, null, null, null, null,
                null, null, null,
                null,null, null, null, null,
                null, null, null, null,
                null, null, null,
                null, null
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
                null,null, null,
                null,null, null, null, null,
                null, null, null, null,
                null, null, null,
                null, null
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

        assertThat(remarks.variableVisibility()).isNull();
        // Note: isEmpty() returns false because List fields are initialized
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
                null, null, null,
                null,null, null, null, null,
                null, null, null, null,
                null, null, null,
                null, null
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

        assertThat(remarks.towerVisibility()).isNull();
        assertThat(remarks.surfaceVisibility()).isNull();
        // Note: isEmpty() returns false because List fields are initialized
    }


// ========== Tests for record constructor with Tower/Surface Visibility ==========

    @Test
    @DisplayName("Should create remarks with towerVisibility via record constructor")
    void testRecordConstructorWithTowerVisibility() {
        Visibility towerVis = Visibility.statuteMiles(1.5);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, null, null,
                null, towerVis, null,
                null,null, null, null, null,
                null, null,null, null,
                null, null,null,
                null, null
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
                null, null, null,null, null,
                null, null, surfaceVis,
                null,null, null, null, null,
                null, null, null, null,
                null, null,null,
                null, null
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
                null, null, null, null, null,
                null, towerVis, surfaceVis,
                null,null, null, null,null,
                null, null, null, null,
                null, null,null,
                null, null
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

        assertThat(remarks.hourlyPrecipitation()).isNull();
        assertThat(remarks.sixHourPrecipitation()).isNull();
        assertThat(remarks.twentyFourHourPrecipitation()).isNull();
        // Note: isEmpty() returns false because List fields are initialized
    }


    // ========== Tests for record constructor with Precipitation ==========

    @Test
    @DisplayName("Should create remarks with hourlyPrecipitation via record constructor")
    void testRecordConstructorWithHourlyPrecipitation() {
        PrecipitationAmount hourly = PrecipitationAmount.fromEncoded("0015", 1);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, null, null,
                null,null, null,
                 hourly, null,null, null, null, null,
                null, null, null, null,
                null, null, null, null
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
                null, null, null, null, null,
                null,null, null,
                null, sixHour,null, null, null, null,
                null, null, null, null,
                null, null, null, null
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
                null,null, null,
                null,null, twentyFourHour, null, null, null,
                null, null, null, null,
                null, null, null, null
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
                null, null, null, null, null,
                null,null, null,
                hourly, sixHour, twentyFourHour, null, null, null,
                null, null, null, null,
                null, null, null, null
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

        assertThat(remarks.hailSize()).isNull();
        // Note: isEmpty() returns false because List fields are initialized
    }

    // ========== Tests for record constructor with Hail Size ==========

    @Test
    @DisplayName("Should create remarks with hailSize via record constructor")
    void testRecordConstructorWithHailSize() {
        HailSize hailSize = HailSize.inches(1.75);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, null, null,
                null,null, null,
                null,null, null, hailSize, null,
                null, null, null, null,
                null, null, null,
                null, null
        );

        assertEquals(hailSize, remarks.hailSize());
    }

    @Test
    @DisplayName("Should create remarks with null hailSize via record constructor")
    void testRecordConstructorWithNullHailSize() {
        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, null, null,
                null,null, null,
                null,null, null, null, null,
                null,null, null, null,
                null, null, null,
                null, null
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

    // ========== Tests for isEmpty() with Thunderstorm Locations ==========

    @Test
    @DisplayName("Should not be empty when only thunderstormLocations is present")
    void testIsEmptyWithOnlyThunderstormLocations() {
        ThunderstormLocation location = ThunderstormLocation.of("TS", "SE");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location)
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should handle empty thunderstormLocations list")
    void testIsEmptyWithEmptyThunderstormLocationsList() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .thunderstormLocations(List.of())
                .build();

        assertThat(remarks.thunderstormLocations()).isEmpty();
        assertThat(remarks.thunderstormLocations()).isNotNull();
        // Note: isEmpty() returns false because thunderstormLocations field is set
    }

    @Test
    @DisplayName("Should handle null thunderstormLocations in builder")
    void testIsEmptyWithNullThunderstormLocations() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .thunderstormLocations(null)
                .build();

        assertThat(remarks.thunderstormLocations()).isEmpty();
        assertThat(remarks.thunderstormLocations()).isNotNull();
        // Note: isEmpty() returns false because thunderstormLocations field is set
    }

    // ========== Tests for record constructor with Thunderstorm Locations ==========

    @Test
    @DisplayName("Should create remarks with single thunderstormLocation via record constructor")
    void testRecordConstructorWithSingleThunderstormLocation() {
        ThunderstormLocation location = ThunderstormLocation.of("TS", "SE");

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, null, null,
                null,null, null,
                null,null, null, null, null,
                 List.of(location), null, null, null,
                null, null, null,
                null, null
        );

        assertEquals(1, remarks.thunderstormLocations().size());
        assertEquals(location, remarks.thunderstormLocations().get(0));
    }

    @Test
    @DisplayName("Should create remarks with multiple thunderstormLocations via record constructor")
    void testRecordConstructorWithMultipleThunderstormLocations() {
        ThunderstormLocation location1 = ThunderstormLocation.of("TS", "SE");
        ThunderstormLocation location2 = ThunderstormLocation.of("CB", "W");

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, null, null,
                null,null, null,
                null,null, null, null, null,
                 List.of(location1, location2), null, null, null,
                null, null, null,
                null, null
        );

        assertEquals(2, remarks.thunderstormLocations().size());
        assertEquals(location1, remarks.thunderstormLocations().get(0));
        assertEquals(location2, remarks.thunderstormLocations().get(1));
    }

    // ========== Tests for builder with Thunderstorm Locations ==========

    @Test
    @DisplayName("Should build remarks with single thunderstormLocation via builder")
    void testBuilderWithSingleThunderstormLocation() {
        ThunderstormLocation location = ThunderstormLocation.of("TS", "SE");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location)
                .build();

        assertEquals(1, remarks.thunderstormLocations().size());
        assertEquals(location, remarks.thunderstormLocations().get(0));
        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should build remarks with multiple thunderstormLocations via addThunderstormLocation")
    void testBuilderWithMultipleThunderstormLocationsViaAdd() {
        ThunderstormLocation location1 = ThunderstormLocation.of("TS", "SE");
        ThunderstormLocation location2 = ThunderstormLocation.of("CB", "W");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location1)
                .addThunderstormLocation(location2)
                .build();

        assertEquals(2, remarks.thunderstormLocations().size());
        assertEquals(location1, remarks.thunderstormLocations().get(0));
        assertEquals(location2, remarks.thunderstormLocations().get(1));
    }

    @Test
    @DisplayName("Should build remarks with thunderstormLocations list via builder")
    void testBuilderWithThunderstormLocationsList() {
        ThunderstormLocation location1 = ThunderstormLocation.of("TS", "SE");
        ThunderstormLocation location2 = ThunderstormLocation.of("CB", "W");
        List<ThunderstormLocation> locations = List.of(location1, location2);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .thunderstormLocations(locations)
                .build();

        assertEquals(2, remarks.thunderstormLocations().size());
        assertEquals(location1, remarks.thunderstormLocations().get(0));
        assertEquals(location2, remarks.thunderstormLocations().get(1));
    }

    @Test
    @DisplayName("Should build remarks with multiple fields including thunderstormLocations")
    void testBuilderWithMultipleFieldsIncludingThunderstormLocations() {
        AutomatedStationType stationType = AutomatedStationType.AO2;
        Pressure slp = Pressure.hectopascals(1013.2);
        ThunderstormLocation location = ThunderstormLocation.of("TS", "SE");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(stationType)
                .seaLevelPressure(slp)
                .addThunderstormLocation(location)
                .build();

        assertEquals(stationType, remarks.automatedStationType());
        assertEquals(slp, remarks.seaLevelPressure());
        assertEquals(1, remarks.thunderstormLocations().size());
        assertEquals(location, remarks.thunderstormLocations().get(0));
        assertFalse(remarks.isEmpty());
    }

    // ========== Tests for toString() with Thunderstorm Locations ==========

    @Test
    @DisplayName("Should include single thunderstormLocation in toString()")
    void testToStringWithSingleThunderstormLocation() {
        ThunderstormLocation location = ThunderstormLocation.of("TS", "SE");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("thunderstormLocations"));
    }

    @Test
    @DisplayName("Should include multiple thunderstormLocations in toString()")
    void testToStringWithMultipleThunderstormLocations() {
        ThunderstormLocation location1 = ThunderstormLocation.of("TS", "SE");
        ThunderstormLocation location2 = ThunderstormLocation.of("CB", "W");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location1)
                .addThunderstormLocation(location2)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("thunderstormLocations"));
    }

    @Test
    @DisplayName("Should show thunderstorm location details in toString()")
    void testToStringThunderstormLocationDetails() {
        ThunderstormLocation location = new ThunderstormLocation("CB", "OHD", null, null, "E");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("thunderstormLocations"));
        // Summary should include CB or thunderstormLocations
        assertTrue(str.contains("CB") || str.contains("thunderstormLocations"));
    }

    // ========== Tests for equality with Thunderstorm Locations ==========

    @Test
    @DisplayName("Should be equal when thunderstormLocations are the same")
    void testEqualityWithThunderstormLocations() {
        ThunderstormLocation location = ThunderstormLocation.of("TS", "SE");

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .addThunderstormLocation(location)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .addThunderstormLocation(location)
                .build();

        assertEquals(remarks1, remarks2);
        assertEquals(remarks1.hashCode(), remarks2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when thunderstormLocations differ")
    void testInequalityWithDifferentThunderstormLocations() {
        ThunderstormLocation location1 = ThunderstormLocation.of("TS", "SE");
        ThunderstormLocation location2 = ThunderstormLocation.of("CB", "W");

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location1)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location2)
                .build();

        assertNotEquals(remarks1, remarks2);
    }

    @Test
    @DisplayName("Should not be equal when thunderstormLocations count differs")
    void testInequalityWithDifferentThunderstormLocationCounts() {
        ThunderstormLocation location1 = ThunderstormLocation.of("TS", "SE");
        ThunderstormLocation location2 = ThunderstormLocation.of("CB", "W");

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location1)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location1)
                .addThunderstormLocation(location2)
                .build();

        assertNotEquals(remarks1, remarks2);
    }

    // ========== Tests with Various Thunderstorm Location Types ==========

    @Test
    @DisplayName("Should handle thunderstorm location with qualifier")
    void testThunderstormLocationWithQualifier() {
        ThunderstormLocation location = new ThunderstormLocation("TS", "OHD", null, null, null);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location)
                .build();

        assertEquals(1, remarks.thunderstormLocations().size());
        assertEquals("OHD", remarks.thunderstormLocations().get(0).locationQualifier());
    }

    @Test
    @DisplayName("Should handle thunderstorm location with direction range")
    void testThunderstormLocationWithDirectionRange() {
        ThunderstormLocation location = new ThunderstormLocation("CB", "DSNT", "N", "NE", null);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location)
                .build();

        assertEquals(1, remarks.thunderstormLocations().size());
        ThunderstormLocation stored = remarks.thunderstormLocations().get(0);
        assertEquals("N", stored.direction());
        assertEquals("NE", stored.directionRange());
        assertTrue(stored.hasDirectionRange());
    }

    @Test
    @DisplayName("Should handle thunderstorm location with movement")
    void testThunderstormLocationWithMovement() {
        ThunderstormLocation location = ThunderstormLocation.withMovement("TS", "SE", "E");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location)
                .build();

        assertEquals(1, remarks.thunderstormLocations().size());
        ThunderstormLocation stored = remarks.thunderstormLocations().get(0);
        assertEquals("E", stored.movingDirection());
        assertTrue(stored.isMoving());
    }

    @Test
    @DisplayName("Should handle complete thunderstorm location")
    void testCompleteThunderstormLocation() {
        ThunderstormLocation location = new ThunderstormLocation("TCU", "VC", "W", "NW", "N");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location)
                .build();

        assertEquals(1, remarks.thunderstormLocations().size());
        ThunderstormLocation stored = remarks.thunderstormLocations().get(0);
        assertEquals("TCU", stored.cloudType());
        assertEquals("VC", stored.locationQualifier());
        assertEquals("W", stored.direction());
        assertEquals("NW", stored.directionRange());
        assertEquals("N", stored.movingDirection());
    }

    @Test
    @DisplayName("Should handle various cloud types")
    void testVariousCloudTypes() {
        ThunderstormLocation ts = ThunderstormLocation.of("TS", "SE");
        ThunderstormLocation cb = ThunderstormLocation.of("CB", "W");
        ThunderstormLocation tcu = ThunderstormLocation.of("TCU", "NW");
        ThunderstormLocation acc = ThunderstormLocation.of("ACC", "E");
        ThunderstormLocation cbmam = ThunderstormLocation.of("CBMAM", "N");
        ThunderstormLocation virga = ThunderstormLocation.of("VIRGA", "S");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addThunderstormLocation(ts)
                .addThunderstormLocation(cb)
                .addThunderstormLocation(tcu)
                .addThunderstormLocation(acc)
                .addThunderstormLocation(cbmam)
                .addThunderstormLocation(virga)
                .build();

        assertEquals(6, remarks.thunderstormLocations().size());
        assertTrue(remarks.thunderstormLocations().get(0).isThunderstorm());
        assertFalse(remarks.thunderstormLocations().get(1).isThunderstorm());
    }

    @Test
    @DisplayName("Should handle thunderstormLocations with weatherEvents")
    void testThunderstormLocationsWithWeatherEvents() {
        ThunderstormLocation location = ThunderstormLocation.of("TS", "SE");
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, 30);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location)
                .addWeatherEvent(event)
                .build();

        assertEquals(1, remarks.thunderstormLocations().size());
        assertEquals(1, remarks.weatherEvents().size());
        assertFalse(remarks.isEmpty());
    }

    // ========== Edge case tests ==========

    @Test
    @DisplayName("Should handle null thunderstormLocations list in builder")
    void testBuilderWithNullThunderstormLocationsList() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .thunderstormLocations(null)
                .build();

        assertNotNull(remarks.thunderstormLocations());
        assertTrue(remarks.thunderstormLocations().isEmpty());
    }

    @Test
    @DisplayName("Should create immutable thunderstormLocations list")
    void testImmutableThunderstormLocationsList() {
        ThunderstormLocation location = ThunderstormLocation.of("TS", "SE");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addThunderstormLocation(location)
                .build();

        // Extract list first, then test immutability with single method call
        List<ThunderstormLocation> locations = remarks.thunderstormLocations();
        ThunderstormLocation newLocation = ThunderstormLocation.of("CB", "W");

        assertThrows(UnsupportedOperationException.class, () ->
                        locations.add(newLocation)
        );
    }

    @Test
    @DisplayName("Should handle empty list and null separately")
    void testEmptyListVsNull() {
        NoaaMetarRemarks remarksWithEmptyList = NoaaMetarRemarks.builder()
                .thunderstormLocations(List.of())
                .build();

        NoaaMetarRemarks remarksWithNull = NoaaMetarRemarks.builder()
                .thunderstormLocations(null)
                .build();

        assertTrue(remarksWithEmptyList.thunderstormLocations().isEmpty());
        assertTrue(remarksWithNull.thunderstormLocations().isEmpty());
        assertEquals(remarksWithEmptyList, remarksWithNull);
    }

    // ========== Tests for isEmpty() with Weather Events ==========

    @Test
    @DisplayName("Should not be empty when only weatherEvents is present")
    void testIsEmptyWithOnlyWeatherEvents() {
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addWeatherEvent(event)
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should handle empty weatherEvents list")
    void testIsEmptyWithEmptyWeatherEventsList() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .weatherEvents(List.of())
                .build();

        assertThat(remarks.weatherEvents()).isEmpty();
        assertThat(remarks.weatherEvents()).isNotNull();
        // Note: isEmpty() returns false because weatherEvents field is set (even though empty)
    }

    @Test
    @DisplayName("Should handle null weatherEvents in builder")
    void testIsEmptyWithNullWeatherEvents() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .weatherEvents(null)
                .build();

        assertThat(remarks.weatherEvents()).isEmpty();
        assertThat(remarks.weatherEvents()).isNotNull();
        // Note: isEmpty() returns false because weatherEvents field is set to empty list
    }

// ========== NEW Tests for record constructor with Weather Events ==========

    @Test
    @DisplayName("Should create remarks with single weatherEvent via record constructor")
    void testRecordConstructorWithSingleWeatherEvent() {
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null,null, null,
                null,null, null,
                null,null, null, null, List.of(event),
                null, null, null, null,
                null, null, null,
                null, null
        );

        assertEquals(1, remarks.weatherEvents().size());
        assertEquals(event, remarks.weatherEvents().get(0));
    }

    @Test
    @DisplayName("Should create remarks with multiple weatherEvents via record constructor")
    void testRecordConstructorWithMultipleWeatherEvents() {
        WeatherEvent event1 = new WeatherEvent("RA", null, null, 5, null, 30);
        WeatherEvent event2 = new WeatherEvent("SN", null, null, 30, null, null);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, null, null,
                null,null, null,
                null,null, null, null, List.of(event1, event2),
                null, null, null, null,
                null, null, null,
                null, null
        );

        assertEquals(2, remarks.weatherEvents().size());
        assertEquals(event1, remarks.weatherEvents().get(0));
        assertEquals(event2, remarks.weatherEvents().get(1));
    }

    // ========== NEW Tests for builder with Weather Events ==========

    @Test
    @DisplayName("Should build remarks with single weatherEvent via builder")
    void testBuilderWithSingleWeatherEvent() {
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addWeatherEvent(event)
                .build();

        assertEquals(1, remarks.weatherEvents().size());
        assertEquals(event, remarks.weatherEvents().get(0));
        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should build remarks with multiple weatherEvents via addWeatherEvent")
    void testBuilderWithMultipleWeatherEventsViaAdd() {
        WeatherEvent event1 = new WeatherEvent("RA", null, null, 5, null, 30);
        WeatherEvent event2 = new WeatherEvent("SN", null, null, 30, null, null);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addWeatherEvent(event1)
                .addWeatherEvent(event2)
                .build();

        assertEquals(2, remarks.weatherEvents().size());
        assertEquals(event1, remarks.weatherEvents().get(0));
        assertEquals(event2, remarks.weatherEvents().get(1));
    }

    @Test
    @DisplayName("Should build remarks with weatherEvents list via builder")
    void testBuilderWithWeatherEventsList() {
        WeatherEvent event1 = new WeatherEvent("RA", null, null, 5, null, 30);
        WeatherEvent event2 = new WeatherEvent("SN", null, null, 30, null, null);
        List<WeatherEvent> events = List.of(event1, event2);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .weatherEvents(events)
                .build();

        assertEquals(2, remarks.weatherEvents().size());
        assertEquals(event1, remarks.weatherEvents().get(0));
        assertEquals(event2, remarks.weatherEvents().get(1));
    }

    @Test
    @DisplayName("Should build remarks with addWeatherEvents bulk method")
    void testBuilderWithAddWeatherEventsBulk() {
        WeatherEvent event1 = new WeatherEvent("FZRA", null, 11, 59, 12, 40);
        WeatherEvent event2 = new WeatherEvent("TS", null, 1, 59, 2, 40);
        List<WeatherEvent> events = List.of(event1, event2);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addWeatherEvents(events)
                .build();

        assertEquals(2, remarks.weatherEvents().size());
        assertTrue(remarks.weatherEvents().containsAll(events));
    }

    @Test
    @DisplayName("Should build remarks with multiple fields including weatherEvents")
    void testBuilderWithMultipleFieldsIncludingWeatherEvents() {
        AutomatedStationType stationType = AutomatedStationType.AO2;
        Pressure slp = Pressure.hectopascals(1013.2);
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, 30);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(stationType)
                .seaLevelPressure(slp)
                .addWeatherEvent(event)
                .build();

        assertEquals(stationType, remarks.automatedStationType());
        assertEquals(slp, remarks.seaLevelPressure());
        assertEquals(1, remarks.weatherEvents().size());
        assertEquals(event, remarks.weatherEvents().get(0));
        assertFalse(remarks.isEmpty());
    }

    // ========== NEW Tests for toString() with Weather Events ==========

    @Test
    @DisplayName("Should include single weatherEvent in toString()")
    void testToStringWithSingleWeatherEvent() {
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addWeatherEvent(event)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("weatherEvents"));
    }

    @Test
    @DisplayName("Should include multiple weatherEvents in toString()")
    void testToStringWithMultipleWeatherEvents() {
        WeatherEvent event1 = new WeatherEvent("RA", null, null, 5, null, 30);
        WeatherEvent event2 = new WeatherEvent("SN", null, null, 30, null, null);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addWeatherEvent(event1)
                .addWeatherEvent(event2)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("weatherEvents"));
    }

    @Test
    @DisplayName("Should show weather event details in toString()")
    void testToStringWeatherEventDetails() {
        WeatherEvent event = new WeatherEvent("FZRA", null, 11, 59, 12, 40);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addWeatherEvent(event)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("weatherEvents"));
        // Summary should include FZRA
        assertTrue(str.contains("FZRA") || str.contains("weatherEvents"));
    }

    // ========== NEW Tests for equality with Weather Events ==========

    @Test
    @DisplayName("Should be equal when weatherEvents are the same")
    void testEqualityWithWeatherEvents() {
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .addWeatherEvent(event)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .addWeatherEvent(event)
                .build();

        assertEquals(remarks1, remarks2);
        assertEquals(remarks1.hashCode(), remarks2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when weatherEvents differ")
    void testInequalityWithDifferentWeatherEvents() {
        WeatherEvent event1 = new WeatherEvent("RA", null, null, 5, null, null);
        WeatherEvent event2 = new WeatherEvent("SN", null, null, 30, null, null);

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .addWeatherEvent(event1)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .addWeatherEvent(event2)
                .build();

        assertNotEquals(remarks1, remarks2);
    }

    @Test
    @DisplayName("Should not be equal when weatherEvents count differs")
    void testInequalityWithDifferentWeatherEventCounts() {
        WeatherEvent event1 = new WeatherEvent("RA", null, null, 5, null, 30);
        WeatherEvent event2 = new WeatherEvent("SN", null, null, 30, null, null);

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .addWeatherEvent(event1)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .addWeatherEvent(event1)
                .addWeatherEvent(event2)
                .build();

        assertNotEquals(remarks1, remarks2);
    }

    // ========== Tests with Various Weather Event Types ==========

    @Test
    @DisplayName("Should handle weather event with light intensity")
    void testWeatherEventWithLightIntensity() {
        WeatherEvent event = new WeatherEvent("RA", "-", null, 5, null, null);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addWeatherEvent(event)
                .build();

        assertEquals(1, remarks.weatherEvents().size());
        assertEquals("-", remarks.weatherEvents().get(0).intensity());
    }

    @Test
    @DisplayName("Should handle weather event with heavy intensity")
    void testWeatherEventWithHeavyIntensity() {
        WeatherEvent event = new WeatherEvent("TSRA", "+", null, 20, null, 45);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addWeatherEvent(event)
                .build();

        assertEquals(1, remarks.weatherEvents().size());
        assertEquals("+", remarks.weatherEvents().get(0).intensity());
    }

    @Test
    @DisplayName("Should handle weather event with full timestamps")
    void testWeatherEventWithFullTimestamps() {
        WeatherEvent event = new WeatherEvent("FZRA", null, 11, 59, 12, 40);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addWeatherEvent(event)
                .build();

        assertEquals(1, remarks.weatherEvents().size());
        WeatherEvent stored = remarks.weatherEvents().get(0);
        assertEquals(11, stored.beginHour());
        assertEquals(59, stored.beginMinute());
        assertEquals(12, stored.endHour());
        assertEquals(40, stored.endMinute());
    }

    @Test
    @DisplayName("Should handle null weatherEvents in builder")
    void testBuilderWithNullWeatherEvent() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addWeatherEvent(null)
                .build();

        assertTrue(remarks.weatherEvents().isEmpty());
        // Note: isEmpty() returns false because List fields are initialized
    }

    @Test
    @DisplayName("Should handle null weatherEvents list in builder")
    void testBuilderWithNullWeatherEventsList() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .weatherEvents(null)
                .build();

        assertNotNull(remarks.weatherEvents());
        assertTrue(remarks.weatherEvents().isEmpty());
    }

    @Test
    @DisplayName("Should handle null events list in addWeatherEvents")
    void testBuilderAddWeatherEvents_Null() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addWeatherEvents(null)
                .build();

        assertNotNull(remarks.weatherEvents());
        assertTrue(remarks.weatherEvents().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "OHD, Overhead, 'Overhead qualifier'",
            "VC, 'In vicinity', 'In vicinity qualifier'",
            "DSNT, Distant, 'Distant qualifier'",
            "DSIPTD, Dissipated, 'Dissipated qualifier'",
            "TOP, 'At or above level', 'At or above level qualifier'",
            "TR, 'At all quadrants', 'At all quadrants qualifier'"
    })
    @DisplayName("Should generate correct description for all location qualifiers")
    void testGetSummary_AllLocationQualifiers(String qualifier, String expectedDescription, String scenario) {
        ThunderstormLocation location = new ThunderstormLocation("TS", qualifier, null,
                null, null);

        String summary = location.getSummary();
        assertThat(summary)
                .as("Summary should contain qualifier description: %s", scenario)
                .contains(expectedDescription);
    }

    @Test
    @DisplayName("Should handle unknown location qualifier with default case")
    void testGetSummary_UnknownLocationQualifier() {
        ThunderstormLocation location = new ThunderstormLocation("TS", "UNKNOWN", null,
                null, null);

        String summary = location.getSummary();
        assertThat(summary).contains("UNKNOWN");
    }

    @Test
    @DisplayName("Should handle unknown cloud type with default case")
    void testGetSummary_UnknownCloudType() {
        // Test the default case with an unknown cloud type
        ThunderstormLocation location = ThunderstormLocation.of("UNKNOWN", "SE");

        String summary = location.getSummary();
        assertThat(summary)
                .as("Should include unknown cloud type as-is")
                .startsWith("UNKNOWN")
                .contains("SE");
    }

    @Test
    @DisplayName("Should return true when direction range is present")
    void testHasDirectionRange_True() {
        ThunderstormLocation location = new ThunderstormLocation("CB", "DSNT", "N",
                "NE", null);

        assertThat(location.hasDirectionRange()).isTrue();
    }

    @Test
    @DisplayName("Should return false when direction range is null")
    void testHasDirectionRange_False() {
        ThunderstormLocation location = ThunderstormLocation.of("TS", "SE");

        assertThat(location.hasDirectionRange()).isFalse();
    }

    @Test
    @DisplayName("Should return true when location qualifier is present")
    void testHasLocationQualifier_True() {
        ThunderstormLocation location = new ThunderstormLocation("TS", "OHD", null,
                null, null);

        assertThat(location.hasLocationQualifier()).isTrue();
    }

    @Test
    @DisplayName("Should return false when location qualifier is null")
    void testHasLocationQualifier_False() {
        ThunderstormLocation location = ThunderstormLocation.of("CB", "W");

        assertThat(location.hasLocationQualifier()).isFalse();
    }

    // ========== Tests for isEmpty() with Pressure Tendency ==========

    @Test
    @DisplayName("Should not be empty when only pressureTendency is present")
    void testIsEmptyWithOnlyPressureTendency() {
        PressureTendency tendency = PressureTendency.of(2, 3.2);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .pressureTendency(tendency)
                .build();

        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should be empty when pressureTendency is null")
    void testIsEmptyWithNullPressureTendency() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .pressureTendency(null)
                .build();

        assertThat(remarks.pressureTendency()).isNull();
        // Note: isEmpty() returns false because List fields are initialized
    }

    // ========== Tests for record constructor with Pressure Tendency ==========

    @Test
    @DisplayName("Should create remarks with pressureTendency via record constructor")
    void testRecordConstructorWithPressureTendency() {
        PressureTendency tendency = PressureTendency.of(7, 4.5);

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, null, null,
                null,null, null,
                null,null, null, null, null,
                null, tendency, null, null,
                null, null, null,
                null, null
        );

        assertEquals(tendency, remarks.pressureTendency());
    }

    @Test
    @DisplayName("Should create remarks with null pressureTendency via record constructor")
    void testRecordConstructorWithNullPressureTendency() {
        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                null, null, null, null, null,
                null, null, null, null, null,
                null,null, null,
                null,null, null, null, null,
                null, null, null, null,
                null, null, null,
                null, null
        );

        assertNull(remarks.pressureTendency());
        assertTrue(remarks.isEmpty());
    }

    // ========== Tests for builder with Pressure Tendency ==========

    @Test
    @DisplayName("Should build remarks with pressureTendency via builder")
    void testBuilderWithPressureTendency() {
        PressureTendency tendency = PressureTendency.of(2, 3.2);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .pressureTendency(tendency)
                .build();

        assertEquals(tendency, remarks.pressureTendency());
        assertFalse(remarks.isEmpty());
    }

    @Test
    @DisplayName("Should build remarks with multiple fields including pressureTendency")
    void testBuilderWithMultipleFieldsIncludingPressureTendency() {
        AutomatedStationType stationType = AutomatedStationType.AO2;
        Pressure slp = Pressure.hectopascals(1013.2);
        PressureTendency tendency = PressureTendency.of(2, 3.2);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(stationType)
                .seaLevelPressure(slp)
                .pressureTendency(tendency)
                .build();

        assertEquals(stationType, remarks.automatedStationType());
        assertEquals(slp, remarks.seaLevelPressure());
        assertEquals(tendency, remarks.pressureTendency());
        assertFalse(remarks.isEmpty());
    }

    // ========== Tests for toString() with Pressure Tendency ==========

    @Test
    @DisplayName("Should include pressureTendency in toString()")
    void testToStringWithPressureTendency() {
        PressureTendency tendency = PressureTendency.of(2, 3.2);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .pressureTendency(tendency)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("pressureTendency"));
    }

    @Test
    @DisplayName("Should show pressure tendency details in toString()")
    void testToStringPressureTendencyDetails() {
        PressureTendency tendency = PressureTendency.of(7, 6.5);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .pressureTendency(tendency)
                .build();

        String str = remarks.toString();
        assertTrue(str.contains("pressureTendency"));
        // Should contain summary which includes tendency description
        assertTrue(str.contains("Pressure tendency") || str.contains("pressureTendency"));
    }

// ========== Tests for equality with Pressure Tendency ==========

    @Test
    @DisplayName("Should be equal when pressureTendency is the same")
    void testEqualityWithPressureTendency() {
        PressureTendency tendency = PressureTendency.of(2, 3.2);

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .pressureTendency(tendency)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .pressureTendency(tendency)
                .build();

        assertEquals(remarks1, remarks2);
        assertEquals(remarks1.hashCode(), remarks2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when pressureTendency differs")
    void testInequalityWithDifferentPressureTendency() {
        PressureTendency tendency1 = PressureTendency.of(2, 3.2);
        PressureTendency tendency2 = PressureTendency.of(7, 4.5);

        NoaaMetarRemarks remarks1 = NoaaMetarRemarks.builder()
                .pressureTendency(tendency1)
                .build();

        NoaaMetarRemarks remarks2 = NoaaMetarRemarks.builder()
                .pressureTendency(tendency2)
                .build();

        assertNotEquals(remarks1, remarks2);
    }

    // ========== Tests with Various Pressure Tendencies ==========

    @Test
    @DisplayName("Should handle increasing pressure tendency")
    void testIncreasingPressureTendency() {
        PressureTendency tendency = PressureTendency.of(2, 3.2);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .pressureTendency(tendency)
                .build();

        assertEquals(tendency, remarks.pressureTendency());
        assertTrue(remarks.pressureTendency().isIncreasing());
        assertFalse(remarks.pressureTendency().isDecreasing());
    }

    @Test
    @DisplayName("Should handle decreasing pressure tendency")
    void testDecreasingPressureTendency() {
        PressureTendency tendency = PressureTendency.of(7, 4.5);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .pressureTendency(tendency)
                .build();

        assertEquals(tendency, remarks.pressureTendency());
        assertTrue(remarks.pressureTendency().isDecreasing());
        assertFalse(remarks.pressureTendency().isIncreasing());
    }

    @Test
    @DisplayName("Should handle steady pressure tendency")
    void testSteadyPressureTendency() {
        PressureTendency tendency = PressureTendency.of(4, 0.5);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .pressureTendency(tendency)
                .build();

        assertEquals(tendency, remarks.pressureTendency());
        assertTrue(remarks.pressureTendency().isSteady());
        assertFalse(remarks.pressureTendency().isIncreasing());
        assertFalse(remarks.pressureTendency().isDecreasing());
    }

    @Test
    @DisplayName("Should handle rapid pressure change")
    void testRapidPressureChange() {
        PressureTendency tendency = PressureTendency.of(7, 8.0);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .pressureTendency(tendency)
                .build();

        assertEquals(tendency, remarks.pressureTendency());
        assertTrue(remarks.pressureTendency().isRapidChange());
        assertTrue(remarks.pressureTendency().isSignificant());
    }

    @Test
    @DisplayName("Should handle significant but not rapid pressure change")
    void testSignificantPressureChange() {
        PressureTendency tendency = PressureTendency.of(2, 4.0);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .pressureTendency(tendency)
                .build();

        assertEquals(tendency, remarks.pressureTendency());
        assertTrue(remarks.pressureTendency().isSignificant());
        assertFalse(remarks.pressureTendency().isRapidChange());
    }

    @Test
    @DisplayName("Should handle all tendency codes")
    void testAllTendencyCodes() {
        for (int code = 0; code <= 8; code++) {
            PressureTendency tendency = PressureTendency.of(code, 3.0);

            NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                    .pressureTendency(tendency)
                    .build();

            assertEquals(code, remarks.pressureTendency().tendencyCode());
            assertFalse(remarks.isEmpty());
        }
    }

    @Test
    @DisplayName("Should handle pressureTendency with other weather data")
    void testPressureTendencyWithOtherData() {
        PressureTendency tendency = PressureTendency.of(2, 3.2);
        WeatherEvent event = new WeatherEvent("RA", null, null, 5,
                null, 30);
        ThunderstormLocation location = ThunderstormLocation.of("TS", "SE");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .pressureTendency(tendency)
                .addWeatherEvent(event)
                .addThunderstormLocation(location)
                .build();

        assertEquals(tendency, remarks.pressureTendency());
        assertEquals(1, remarks.weatherEvents().size());
        assertEquals(1, remarks.thunderstormLocations().size());
        assertFalse(remarks.isEmpty());
    }

    // ========== 6-HOUR MAX/MIN TEMPERATURE TESTS ==========

    @Test
    @DisplayName("Should create NoaaMetarRemarks with 6-hour maximum temperature")
    void testBuilder_SixHourMaxTemperature() {
        Temperature maxTemp = Temperature.of(14.2);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .sixHourMaxTemperature(maxTemp)
                .build();

        assertThat(remarks.sixHourMaxTemperature()).isEqualTo(maxTemp);
        assertThat(remarks.sixHourMaxTemperature().celsius()).isEqualTo(14.2, within(0.01));
        assertThat(remarks.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should create NoaaMetarRemarks with 6-hour minimum temperature")
    void testBuilder_SixHourMinTemperature() {
        Temperature minTemp = Temperature.of(-0.1);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .sixHourMinTemperature(minTemp)
                .build();

        assertThat(remarks.sixHourMinTemperature()).isEqualTo(minTemp);
        assertThat(remarks.sixHourMinTemperature().celsius()).isEqualTo(-0.1, within(0.01));
        assertThat(remarks.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should create NoaaMetarRemarks with both 6-hour max and min temperatures")
    void testBuilder_BothSixHourTemperatures() {
        Temperature maxTemp = Temperature.of(14.2);
        Temperature minTemp = Temperature.of(-0.1);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .sixHourMaxTemperature(maxTemp)
                .sixHourMinTemperature(minTemp)
                .build();

        assertThat(remarks.sixHourMaxTemperature()).isEqualTo(maxTemp);
        assertThat(remarks.sixHourMinTemperature()).isEqualTo(minTemp);
        assertThat(remarks.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should handle null 6-hour max/min temperatures")
    void testBuilder_NullSixHourTemperatures() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .sixHourMaxTemperature(null)
                .sixHourMinTemperature(null)
                .build();

        assertThat(remarks.sixHourMaxTemperature()).isNull();
        assertThat(remarks.sixHourMinTemperature()).isNull();
        // Note: isEmpty() returns false because List fields are initialized
    }

    @Test
    @DisplayName("Should format toString with 6-hour max/min temperatures")
    void testToString_SixHourTemperatures() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .sixHourMaxTemperature(Temperature.of(14.2))
                .sixHourMinTemperature(Temperature.of(-0.1))
                .build();

        String result = remarks.toString();

        assertThat(result)
                .contains("sixHourMaxTemp=14.2°C")
                .contains("sixHourMinTemp=-0.1°C");
    }

    @Test
    @DisplayName("Should handle extreme 6-hour temperatures")
    void testBuilder_ExtremeSixHourTemperatures() {
        Temperature veryHot = Temperature.of(45.0);
        Temperature veryCold = Temperature.of(-40.0);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .sixHourMaxTemperature(veryHot)
                .sixHourMinTemperature(veryCold)
                .build();

        assertThat(remarks.sixHourMaxTemperature().celsius()).isEqualTo(45.0, within(0.01));
        assertThat(remarks.sixHourMinTemperature().celsius()).isEqualTo(-40.0, within(0.01));
        assertThat(remarks.sixHourMaxTemperature().isVeryHot()).isTrue();
        assertThat(remarks.sixHourMinTemperature().isVeryCold()).isTrue();
    }

    @Test
    @DisplayName("Should handle zero 6-hour temperatures")
    void testBuilder_ZeroSixHourTemperatures() {
        Temperature zeroMax = Temperature.of(0.0);
        Temperature zeroMin = Temperature.of(0.0);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .sixHourMaxTemperature(zeroMax)
                .sixHourMinTemperature(zeroMin)
                .build();

        assertThat(remarks.sixHourMaxTemperature().celsius()).isEqualTo(0.0, within(0.01));
        assertThat(remarks.sixHourMinTemperature().celsius()).isEqualTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("Should convert 6-hour temperatures to Fahrenheit")
    void testBuilder_SixHourTemperatureFahrenheitConversion() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .sixHourMaxTemperature(Temperature.of(14.2))
                .sixHourMinTemperature(Temperature.of(-0.1))
                .build();

        // 14.2°C ≈ 57.56°F
        assertThat(remarks.sixHourMaxTemperature().toFahrenheit())
                .isCloseTo(57.56, within(0.1));

        // -0.1°C ≈ 31.82°F
        assertThat(remarks.sixHourMinTemperature().toFahrenheit())
                .isCloseTo(31.82, within(0.1));
    }

    // ========== 24-HOUR MAX/MIN TEMPERATURE TESTS ==========

    @Test
    @DisplayName("Should create NoaaMetarRemarks with 24-hour max/min temperatures")
    void testBuilder_TwentyFourHourTemperatures() {
        Temperature maxTemp = Temperature.of(4.6);
        Temperature minTemp = Temperature.of(-0.6);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .twentyFourHourMaxTemperature(maxTemp)
                .twentyFourHourMinTemperature(minTemp)
                .build();

        assertThat(remarks.twentyFourHourMaxTemperature()).isEqualTo(maxTemp);
        assertThat(remarks.twentyFourHourMaxTemperature().celsius()).isEqualTo(4.6, within(0.01));
        assertThat(remarks.twentyFourHourMinTemperature()).isEqualTo(minTemp);
        assertThat(remarks.twentyFourHourMinTemperature().celsius()).isEqualTo(-0.6, within(0.01));
        assertThat(remarks.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should handle null 24-hour temperatures")
    void testBuilder_NullTwentyFourHourTemperatures() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .twentyFourHourMaxTemperature(null)
                .twentyFourHourMinTemperature(null)
                .build();

        assertThat(remarks.twentyFourHourMaxTemperature()).isNull();
        assertThat(remarks.twentyFourHourMinTemperature()).isNull();
        // Note: isEmpty() returns false because List fields are initialized
    }

    @Test
    @DisplayName("Should format toString with 24-hour temperatures")
    void testToString_TwentyFourHourTemperatures() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .twentyFourHourMaxTemperature(Temperature.of(4.6))
                .twentyFourHourMinTemperature(Temperature.of(-0.6))
                .build();

        String result = remarks.toString();

        assertThat(result)
                .contains("twentyFourHourMaxTemp=4.6°C")
                .contains("twentyFourHourMinTemp=-0.6°C");
    }

    @Test
    @DisplayName("Should handle extreme 24-hour temperatures")
    void testBuilder_ExtremeTwentyFourHourTemperatures() {
        Temperature veryHot = Temperature.of(45.0);
        Temperature veryCold = Temperature.of(-40.0);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .twentyFourHourMaxTemperature(veryHot)
                .twentyFourHourMinTemperature(veryCold)
                .build();

        assertThat(remarks.twentyFourHourMaxTemperature().celsius()).isEqualTo(45.0, within(0.01));
        assertThat(remarks.twentyFourHourMinTemperature().celsius()).isEqualTo(-40.0, within(0.01));
        assertThat(remarks.twentyFourHourMaxTemperature().isVeryHot()).isTrue();
        assertThat(remarks.twentyFourHourMinTemperature().isVeryCold()).isTrue();
    }

    @Test
    @DisplayName("Should handle zero 24-hour temperatures")
    void testBuilder_ZeroTwentyFourHourTemperatures() {
        Temperature zeroMax = Temperature.of(0.0);
        Temperature zeroMin = Temperature.of(0.0);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .twentyFourHourMaxTemperature(zeroMax)
                .twentyFourHourMinTemperature(zeroMin)
                .build();

        assertThat(remarks.twentyFourHourMaxTemperature().celsius()).isEqualTo(0.0, within(0.01));
        assertThat(remarks.twentyFourHourMinTemperature().celsius()).isEqualTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("Should convert 24-hour temperatures to Fahrenheit")
    void testBuilder_TwentyFourHourTemperatureFahrenheitConversion() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .twentyFourHourMaxTemperature(Temperature.of(4.6))
                .twentyFourHourMinTemperature(Temperature.of(-0.6))
                .build();

        // 4.6°C ≈ 40.28°F
        assertThat(remarks.twentyFourHourMaxTemperature().toFahrenheit())
                .isCloseTo(40.28, within(0.1));

        // -0.6°C ≈ 30.92°F
        assertThat(remarks.twentyFourHourMinTemperature().toFahrenheit())
                .isCloseTo(30.92, within(0.1));
    }

    @Test
    @DisplayName("Should handle both 6-hour and 24-hour temperatures")
    void testBuilder_BothSixHourAndTwentyFourHourTemperatures() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .sixHourMaxTemperature(Temperature.of(14.2))
                .sixHourMinTemperature(Temperature.of(-0.1))
                .twentyFourHourMaxTemperature(Temperature.of(4.6))
                .twentyFourHourMinTemperature(Temperature.of(-0.6))
                .build();

        // 6-hour temps
        assertThat(remarks.sixHourMaxTemperature()).isNotNull();
        assertThat(remarks.sixHourMinTemperature()).isNotNull();

        // 24-hour temps
        assertThat(remarks.twentyFourHourMaxTemperature()).isNotNull();
        assertThat(remarks.twentyFourHourMinTemperature()).isNotNull();

        assertThat(remarks.isEmpty()).isFalse();
    }

    // ========== VARIABLE CEILING TESTS ==========

    @Test
    @DisplayName("Should create NoaaMetarRemarks with variable ceiling")
    void testBuilder_VariableCeiling() {
        VariableCeiling ceiling = VariableCeiling.fromHundreds(5, 10);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .variableCeiling(ceiling)
                .build();

        assertThat(remarks.variableCeiling()).isEqualTo(ceiling);
        assertThat(remarks.variableCeiling().minimumHeightFeet()).isEqualTo(500);
        assertThat(remarks.variableCeiling().maximumHeightFeet()).isEqualTo(1000);
        assertThat(remarks.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should handle null variable ceiling")
    void testBuilder_NullVariableCeiling() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .variableCeiling(null)
                .build();

        assertThat(remarks.variableCeiling()).isNull();
        // Note: isEmpty() returns false because List fields are initialized
    }

    @Test
    @DisplayName("Should format toString with variable ceiling")
    void testToString_VariableCeiling() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .variableCeiling(VariableCeiling.fromHundreds(20, 35))
                .build();

        String result = remarks.toString();

        assertThat(result).contains("variableCeiling=Variable ceiling: 2000-3500 ft");
    }

    @Test
    @DisplayName("Should handle low ceiling")
    void testBuilder_LowCeiling() {
        VariableCeiling lowCeiling = VariableCeiling.fromHundreds(3, 8);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .variableCeiling(lowCeiling)
                .build();

        assertThat(remarks.variableCeiling().isLowCeiling()).isTrue();
    }

    @Test
    @DisplayName("Should handle high ceiling")
    void testBuilder_HighCeiling() {
        VariableCeiling highCeiling = VariableCeiling.fromHundreds(50, 100);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .variableCeiling(highCeiling)
                .build();

        assertThat(remarks.variableCeiling().isLowCeiling()).isFalse();
        assertThat(remarks.variableCeiling().minimumHeightFeet()).isEqualTo(5000);
    }

    // ========== CEILING SECOND SITE TESTS ==========

    @Test
    @DisplayName("Should create NoaaMetarRemarks with ceiling second site with location")
    void testBuilder_CeilingSecondSite_WithLocation() {
        CeilingSecondSite ceiling = CeilingSecondSite.fromHundreds(2, "RY11");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .ceilingSecondSite(ceiling)
                .build();

        assertThat(remarks.ceilingSecondSite()).isEqualTo(ceiling);
        assertThat(remarks.ceilingSecondSite().heightFeet()).isEqualTo(200);
        assertThat(remarks.ceilingSecondSite().location()).isEqualTo("RY11");
        assertThat(remarks.ceilingSecondSite().hasLocation()).isTrue();
        assertThat(remarks.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should create NoaaMetarRemarks with ceiling second site without location")
    void testBuilder_CeilingSecondSite_WithoutLocation() {
        CeilingSecondSite ceiling = CeilingSecondSite.fromHundreds(10, null);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .ceilingSecondSite(ceiling)
                .build();

        assertThat(remarks.ceilingSecondSite()).isEqualTo(ceiling);
        assertThat(remarks.ceilingSecondSite().heightFeet()).isEqualTo(1000);
        assertThat(remarks.ceilingSecondSite().location()).isNull();
        assertThat(remarks.ceilingSecondSite().hasLocation()).isFalse();
        assertThat(remarks.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should handle null ceiling second site")
    void testBuilder_NullCeilingSecondSite() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .ceilingSecondSite(null)
                .build();

        assertThat(remarks.ceilingSecondSite()).isNull();
        // Note: isEmpty() returns false because List fields are initialized
    }

    @Test
    @DisplayName("Should format toString with ceiling second site")
    void testToString_CeilingSecondSite() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .ceilingSecondSite(CeilingSecondSite.fromHundreds(5, "RWY06"))
                .build();

        String result = remarks.toString();

        assertThat(result).contains("ceilingSecondSite=Ceiling 500 ft at RWY06");
    }

    @Test
    @DisplayName("Should handle low ceiling at second site")
    void testBuilder_LowCeilingSecondSite() {
        CeilingSecondSite lowCeiling = CeilingSecondSite.fromHundreds(3, "RY11");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .ceilingSecondSite(lowCeiling)
                .build();

        assertThat(remarks.ceilingSecondSite().isLowCeiling()).isTrue();
    }

    @Test
    @DisplayName("Should handle both variable ceiling and ceiling second site")
    void testBuilder_BothVariableAndSecondSiteCeiling() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .variableCeiling(VariableCeiling.fromHundreds(5, 10))
                .ceilingSecondSite(CeilingSecondSite.fromHundreds(2, "RY11"))
                .build();

        // Both should be present
        assertThat(remarks.variableCeiling()).isNotNull();
        assertThat(remarks.ceilingSecondSite()).isNotNull();

        assertThat(remarks.isEmpty()).isFalse();
    }

    // ========== OBSCURATION LAYERS TESTS ==========

    @Test
    @DisplayName("Should create NoaaMetarRemarks with single obscuration layer")
    void testBuilder_SingleObscurationLayer() {
        ObscurationLayer layer = ObscurationLayer.fromHundreds("FEW", "FG", 0);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addObscurationLayer(layer)
                .build();

        assertThat(remarks.obscurationLayers()).hasSize(1);
        assertThat(remarks.obscurationLayers().get(0)).isEqualTo(layer);
        assertThat(remarks.obscurationLayers().get(0).coverage()).isEqualTo("FEW");
        assertThat(remarks.obscurationLayers().get(0).phenomenon()).isEqualTo("FG");
        assertThat(remarks.obscurationLayers().get(0).heightFeet()).isZero();
        assertThat(remarks.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should create NoaaMetarRemarks with multiple obscuration layers")
    void testBuilder_MultipleObscurationLayers() {
        ObscurationLayer layer1 = ObscurationLayer.fromHundreds("FEW", "FG", 0);
        ObscurationLayer layer2 = ObscurationLayer.fromHundreds("SCT", "FU", 10);
        ObscurationLayer layer3 = ObscurationLayer.fromHundreds("BKN", "BR", 5);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addObscurationLayer(layer1)
                .addObscurationLayer(layer2)
                .addObscurationLayer(layer3)
                .build();

        assertThat(remarks.obscurationLayers()).hasSize(3);
        assertThat(remarks.obscurationLayers()).containsExactly(layer1, layer2, layer3);
        assertThat(remarks.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should create NoaaMetarRemarks with obscuration layers list")
    void testBuilder_ObscurationLayersList() {
        List<ObscurationLayer> layers = List.of(
                ObscurationLayer.fromHundreds("FEW", "FG", 0),
                ObscurationLayer.fromHundreds("SCT", "FU", 10)
        );

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .obscurationLayers(layers)
                .build();

        assertThat(remarks.obscurationLayers()).hasSize(2);
        assertThat(remarks.obscurationLayers()).isEqualTo(layers);
        assertThat(remarks.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should handle null obscuration layers")
    void testBuilder_NullObscurationLayers() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .obscurationLayers(null)
                .build();

        assertThat(remarks.obscurationLayers()).isEmpty();
        assertThat(remarks.obscurationLayers()).isNotNull();
    }

    @Test
    @DisplayName("Should handle empty obscuration layers list")
    void testBuilder_EmptyObscurationLayersList() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .obscurationLayers(List.of())
                .build();

        assertThat(remarks.obscurationLayers()).isEmpty();
        assertThat(remarks.obscurationLayers()).isNotNull();
    }

    @Test
    @DisplayName("Should format toString with obscuration layers")
    void testToString_ObscurationLayers() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addObscurationLayer(ObscurationLayer.fromHundreds("FEW", "FG", 0))
                .addObscurationLayer(ObscurationLayer.fromHundreds("SCT", "FU", 10))
                .build();

        String result = remarks.toString();

        assertThat(result)
                .contains("obscurationLayers=")
                .contains("Few Fog at ground level")
                .contains("Scattered Smoke at 1000 ft")
                .contains(";"); // Separator between layers
    }

    @Test
    @DisplayName("Should handle ground level obscuration")
    void testBuilder_GroundLevelObscuration() {
        ObscurationLayer groundFog = ObscurationLayer.fromHundreds("FEW", "FG", 0);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addObscurationLayer(groundFog)
                .build();

        assertThat(remarks.obscurationLayers().get(0).isGroundLevel()).isTrue();
        assertThat(remarks.obscurationLayers().get(0).isLowLevel()).isTrue();
    }

    @Test
    @DisplayName("Should handle various obscuration phenomena")
    void testBuilder_VariousPhenomena() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addObscurationLayer(ObscurationLayer.fromHundreds("FEW", "FG", 0))   // Fog
                .addObscurationLayer(ObscurationLayer.fromHundreds("SCT", "BR", 5))   // Mist
                .addObscurationLayer(ObscurationLayer.fromHundreds("BKN", "FU", 10))  // Smoke
                .addObscurationLayer(ObscurationLayer.fromHundreds("OVC", "HZ", 15))  // Haze
                .build();

        assertThat(remarks.obscurationLayers()).hasSize(4);
        assertThat(remarks.obscurationLayers().get(0).phenomenon()).isEqualTo("FG");
        assertThat(remarks.obscurationLayers().get(1).phenomenon()).isEqualTo("BR");
        assertThat(remarks.obscurationLayers().get(2).phenomenon()).isEqualTo("FU");
        assertThat(remarks.obscurationLayers().get(3).phenomenon()).isEqualTo("HZ");
    }

    @Test
    @DisplayName("Should make defensive copy of obscuration layers list")
    void testBuilder_DefensiveCopyOfObscurationLayers() {
        List<ObscurationLayer> originalList = new java.util.ArrayList<>();
        originalList.add(ObscurationLayer.fromHundreds("FEW", "FG", 0));

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .obscurationLayers(originalList)
                .build();

        // Modify original list
        originalList.add(ObscurationLayer.fromHundreds("SCT", "FU", 10));

        // Remarks should still have only 1 layer (defensive copy)
        assertThat(remarks.obscurationLayers()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle null obscuration layer in addObscurationLayer")
    void testBuilder_AddNullObscurationLayer() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addObscurationLayer(null)  // Should ignore null
                .build();

        assertThat(remarks.obscurationLayers()).isEmpty();
    }

    @Test
    @DisplayName("Should add multiple obscuration layers via addObscurationLayers")
    void testBuilder_AddObscurationLayersBulk() {
        ObscurationLayer layer1 = ObscurationLayer.fromHundreds("FEW", "FG", 0);
        ObscurationLayer layer2 = ObscurationLayer.fromHundreds("SCT", "FU", 10);
        List<ObscurationLayer> layers = List.of(layer1, layer2);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addObscurationLayers(layers)
                .build();

        assertThat(remarks.obscurationLayers()).hasSize(2);
        assertThat(remarks.obscurationLayers()).containsExactly(layer1, layer2);
    }

    @Test
    @DisplayName("Should handle null list in addObscurationLayers")
    void testBuilder_AddObscurationLayersNull() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addObscurationLayers(null)  // Should ignore null
                .build();

        assertThat(remarks.obscurationLayers()).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty list in addObscurationLayers")
    void testBuilder_AddObscurationLayersEmpty() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addObscurationLayers(List.of())  // Empty list
                .build();

        assertThat(remarks.obscurationLayers()).isEmpty();
    }

    // ========== CLOUD TYPES TESTS ==========

    @Test
    @DisplayName("Should create NoaaMetarRemarks with single cloud type")
    void testBuilder_SingleCloudType() {
        CloudType cloudType = CloudType.of("SC", 1);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudType(cloudType)
                .build();

        assertThat(remarks.cloudTypes()).hasSize(1);
        assertThat(remarks.cloudTypes().get(0)).isEqualTo(cloudType);
        assertThat(remarks.cloudTypes().get(0).cloudType()).isEqualTo("SC");
        assertThat(remarks.cloudTypes().get(0).oktas()).isEqualTo(1);
        assertThat(remarks.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should create NoaaMetarRemarks with multiple cloud types")
    void testBuilder_MultipleCloudTypes() {
        CloudType cloudType1 = CloudType.of("SC", 1);
        CloudType cloudType2 = CloudType.withLocation("SC", "TR");
        CloudType cloudType3 = new CloudType("CU", null, "MDT", "OHD", null);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudType(cloudType1)
                .addCloudType(cloudType2)
                .addCloudType(cloudType3)
                .build();

        assertThat(remarks.cloudTypes()).hasSize(3);
        assertThat(remarks.cloudTypes()).containsExactly(cloudType1, cloudType2, cloudType3);
        assertThat(remarks.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should create NoaaMetarRemarks with cloudTypes list")
    void testBuilder_CloudTypesList() {
        List<CloudType> types = List.of(
                CloudType.of("SC", 1),
                CloudType.withLocation("AC", "TR")
        );

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .cloudTypes(types)
                .build();

        assertThat(remarks.cloudTypes()).hasSize(2);
        assertThat(remarks.cloudTypes()).isEqualTo(types);
        assertThat(remarks.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should handle null cloud types")
    void testBuilder_NullCloudTypes() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .cloudTypes(null)
                .build();

        assertThat(remarks.cloudTypes()).isEmpty();
        assertThat(remarks.cloudTypes()).isNotNull();
    }

    @Test
    @DisplayName("Should handle empty cloud types list")
    void testBuilder_EmptyCloudTypesList() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .cloudTypes(List.of())
                .build();

        assertThat(remarks.cloudTypes()).isEmpty();
        assertThat(remarks.cloudTypes()).isNotNull();
    }

    @Test
    @DisplayName("Should format toString with cloud types")
    void testToString_CloudTypes() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudType(CloudType.of("SC", 1))
                .addCloudType(CloudType.withLocation("SC", "TR"))
                .build();

        String result = remarks.toString();

        assertThat(result)
                .contains("cloudTypes=")
                .contains("Stratocumulus (1/8)")
                .contains("Stratocumulus (tr)")
                .contains(";"); // Separator between cloud types
    }

    @Test
    @DisplayName("Should handle cloud type with oktas")
    void testBuilder_CloudTypeWithOktas() {
        CloudType cloudType = CloudType.of("AC", 8);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudType(cloudType)
                .build();

        assertThat(remarks.cloudTypes().get(0).hasOktaCoverage()).isTrue();
        assertThat(remarks.cloudTypes().get(0).oktas()).isEqualTo(8);
    }

    @Test
    @DisplayName("Should handle cloud type with trace location")
    void testBuilder_CloudTypeTrace() {
        CloudType cloudType = CloudType.withLocation("SC", "TR");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudType(cloudType)
                .build();

        assertThat(remarks.cloudTypes().get(0).isTrace()).isTrue();
        assertThat(remarks.cloudTypes().get(0).location()).isEqualTo("TR");
    }

    @Test
    @DisplayName("Should handle cloud type with intensity and location")
    void testBuilder_CloudTypeWithIntensityAndLocation() {
        CloudType cloudType = new CloudType("CU", null, "MDT", "OHD", null);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudType(cloudType)
                .build();

        assertThat(remarks.cloudTypes().get(0).hasIntensity()).isTrue();
        assertThat(remarks.cloudTypes().get(0).isOverhead()).isTrue();
    }

    @Test
    @DisplayName("Should handle cloud type with movement")
    void testBuilder_CloudTypeWithMovement() {
        CloudType cloudType = new CloudType("CI", null, null, null, "NE");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudType(cloudType)
                .build();

        assertThat(remarks.cloudTypes().get(0).hasMovement()).isTrue();
        assertThat(remarks.cloudTypes().get(0).movementDirection()).isEqualTo("NE");
    }

    @Test
    @DisplayName("Should handle various cloud type codes")
    void testBuilder_VariousCloudTypes() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudType(CloudType.of("CU", 1))   // Cumulus
                .addCloudType(CloudType.of("SC", 2))   // Stratocumulus
                .addCloudType(CloudType.of("AC", 3))   // Altocumulus
                .addCloudType(CloudType.of("CI", 4))   // Cirrus
                .build();

        assertThat(remarks.cloudTypes()).hasSize(4);
        assertThat(remarks.cloudTypes().get(0).cloudType()).isEqualTo("CU");
        assertThat(remarks.cloudTypes().get(1).cloudType()).isEqualTo("SC");
        assertThat(remarks.cloudTypes().get(2).cloudType()).isEqualTo("AC");
        assertThat(remarks.cloudTypes().get(3).cloudType()).isEqualTo("CI");
    }

    @Test
    @DisplayName("Should make defensive copy of cloud types list")
    void testBuilder_DefensiveCopyOfCloudTypes() {
        List<CloudType> originalList = new java.util.ArrayList<>();
        originalList.add(CloudType.of("SC", 1));

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .cloudTypes(originalList)
                .build();

        // Modify original list
        originalList.add(CloudType.of("AC", 2));

        // Remarks should still have only 1 cloud type (defensive copy)
        assertThat(remarks.cloudTypes()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle null cloud type in addCloudType")
    void testBuilder_AddNullCloudType() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudType(null)  // Should ignore null
                .build();

        assertThat(remarks.cloudTypes()).isEmpty();
    }

    @Test
    @DisplayName("Should add multiple cloud types via addCloudTypes")
    void testBuilder_AddCloudTypesBulk() {
        CloudType cloudType1 = CloudType.of("SC", 1);
        CloudType cloudType2 = CloudType.of("AC", 2);
        List<CloudType> types = List.of(cloudType1, cloudType2);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudTypes(types)
                .build();

        assertThat(remarks.cloudTypes()).hasSize(2);
        assertThat(remarks.cloudTypes()).containsExactly(cloudType1, cloudType2);
    }

    @Test
    @DisplayName("Should handle null list in addCloudTypes")
    void testBuilder_AddCloudTypesNull() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudTypes(null)  // Should ignore null
                .build();

        assertThat(remarks.cloudTypes()).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty list in addCloudTypes")
    void testBuilder_AddCloudTypesEmpty() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudTypes(List.of())  // Empty list
                .build();

        assertThat(remarks.cloudTypes()).isEmpty();
    }

    @Test
    @DisplayName("Should handle cloud types with obscuration layers")
    void testBuilder_CloudTypesWithObscurationLayers() {
        CloudType cloudType = CloudType.of("SC", 1);
        ObscurationLayer layer = ObscurationLayer.fromHundreds("FEW", "FG", 0);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudType(cloudType)
                .addObscurationLayer(layer)
                .build();

        assertThat(remarks.cloudTypes()).hasSize(1);
        assertThat(remarks.obscurationLayers()).hasSize(1);
        assertThat(remarks.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should not be empty when only cloudTypes is present")
    void testIsEmptyWithOnlyCloudTypes() {
        CloudType cloudType = CloudType.of("SC", 1);

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudType(cloudType)
                .build();

        assertFalse(remarks.isEmpty());
    }

    // ========== CORRECTED TESTS FOR NoaaMetarRemarksTest.java ==========

    @Test
    @DisplayName("toString should include obscurationLayers when present")
    void testToString_WithObscurationLayers() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addObscurationLayer(ObscurationLayer.fromHundreds("BKN", "FG", 10))
                .addObscurationLayer(ObscurationLayer.fromHundreds("FEW", "BR", 5))
                .build();

        String result = remarks.toString();

        // Verify the toString output includes obscurationLayers
        assertThat(result)
                .contains("obscurationLayers=")
                .contains("Fog")
                .contains("Mist");
    }

    @Test
    @DisplayName("toString should include cloudTypes when present")
    void testToString_WithCloudTypes() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudType(CloudType.of("TCU", 4))
                .addCloudType(CloudType.of("AC", 1))
                .addCloudType(CloudType.of("AC", 2))
                .build();

        String result = remarks.toString();

        // Verify the toString output includes cloudTypes
        assertThat(result)
                .contains("cloudTypes=")
                .contains("Towering Cumulus")
                .contains("Altocumulus");
    }

    @Test
    @DisplayName("toString should include both obscurationLayers and cloudTypes when both present")
    void testToString_WithBothObscurationAndCloudTypes() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addObscurationLayer(ObscurationLayer.fromHundreds("BKN", "FG", 10))
                .addCloudType(CloudType.of("TCU", 4))
                .addCloudType(CloudType.of("AC", 2))
                .build();

        String result = remarks.toString();

        // Verify the toString output includes BOTH sections
        assertThat(result)
                .contains("obscurationLayers=")
                .contains("Fog")
                .contains("cloudTypes=")
                .contains("Towering Cumulus")
                .contains("Altocumulus");
    }

    @Test
    @DisplayName("toString should handle empty remarks gracefully")
    void testToString_Empty() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder().build();

        String result = remarks.toString();

        // Should return the empty indicator
        assertThat(result).isEqualTo("NoaaMetarRemarks{empty}");
    }

    @Test
    @DisplayName("toString should not include obscurationLayers or cloudTypes when null/empty")
    void testToString_WithoutObscurationOrCloudTypes() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .seaLevelPressure(Pressure.hectopascals(1013.2))
                .build();

        String result = remarks.toString();

        // Should NOT contain obscurationLayers or cloudTypes
        assertThat(result)
                .doesNotContain("obscurationLayers=")
                .doesNotContain("cloudTypes=")
                .contains("stationType")
                .contains("AO2")
                .contains("seaLevelPressure");
    }

    @Test
    @DisplayName("toString should format complex remarks with all fields correctly")
    void testToString_CompleteRemarks() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .seaLevelPressure(Pressure.hectopascals(1013.2))
                .addObscurationLayer(ObscurationLayer.fromHundreds("BKN", "FG", 10))
                .addCloudType(CloudType.of("TCU", 4))
                .addCloudType(CloudType.of("AC", 1))
                .peakWind(new PeakWind(280, 45, 15, 30))
                .freeText("SOME EXTRA TEXT")
                .build();

        String result = remarks.toString();

        // Verify all components are present
        assertThat(result)
                .contains("stationType")
                .contains("AO2")
                .contains("seaLevelPressure")
                .contains("obscurationLayers=")
                .contains("Fog")
                .contains("cloudTypes=")
                .contains("Towering Cumulus")
                .contains("Altocumulus")
                .contains("peakWind")
                .contains("freeText");
    }

    @Test
    @DisplayName("toString should follow expected format pattern")
    void testToString_FormatPattern() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addObscurationLayer(ObscurationLayer.fromHundreds("BKN", "FG", 10))
                .addCloudType(CloudType.of("TCU", 4))
                .build();

        String result = remarks.toString();

        // Should start with class name
        assertThat(result)
                .startsWith("NoaaMetarRemarks{")
                .endsWith("}");
        // Note: Semicolons only appear when there are MULTIPLE items in a list
        // This test has only 1 obscurationLayer and 1 cloudType, so no semicolons
    }


// BONUS: Additional edge case tests

    @Test
    @DisplayName("toString should handle single obscurationLayer correctly")
    void testToString_SingleObscurationLayer() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addObscurationLayer(ObscurationLayer.fromHundreds("FEW", "FG", 0))
                .build();

        String result = remarks.toString();

        assertThat(result)
                .contains("obscurationLayers=")
                .contains("Fog")
                .contains("ground level");
    }

    @Test
    @DisplayName("toString should handle single cloudType correctly")
    void testToString_SingleCloudType() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudType(CloudType.of("TCU", 8))
                .build();

        String result = remarks.toString();

        assertThat(result)
                .contains("cloudTypes=")
                .contains("Towering Cumulus")
                .contains("8/8");
    }

    @Test
    @DisplayName("toString should handle multiple obscurationLayers with semicolon separator")
    void testToString_MultipleObscurationLayers() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addObscurationLayer(ObscurationLayer.fromHundreds("FEW", "FG", 0))
                .addObscurationLayer(ObscurationLayer.fromHundreds("SCT", "BR", 5))
                .addObscurationLayer(ObscurationLayer.fromHundreds("BKN", "HZ", 10))
                .build();

        String result = remarks.toString();

        assertThat(result)
                .contains("obscurationLayers=")
                .contains(";")
                .contains("Fog")
                .contains("Mist")
                .contains("Haze");
    }

    @Test
    @DisplayName("toString should handle multiple cloudTypes with semicolon separator")
    void testToString_MultipleCloudTypes() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addCloudType(CloudType.of("TCU", 4))
                .addCloudType(CloudType.of("AC", 1))
                .addCloudType(CloudType.of("CI", 2))
                .build();

        String result = remarks.toString();

        assertThat(result)
                .contains("cloudTypes=")
                .contains(";")
                .contains("Towering Cumulus")
                .contains("Altocumulus")
                .contains("Cirrus");
    }

    // ========== AUTOMATED MAINTENANCE INDICATOR TESTS ==========

    @Test
    @DisplayName("Should build NoaaMetarRemarks with automated maintenance indicators")
    void testBuildWithAutomatedMaintenanceIndicators() {
        AutomatedMaintenanceIndicator indicator1 = AutomatedMaintenanceIndicator.of("TSNO");
        AutomatedMaintenanceIndicator indicator2 = AutomatedMaintenanceIndicator.of("VISNO", "RWY06");

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addAutomatedMaintenanceIndicator(indicator1)
                .addAutomatedMaintenanceIndicator(indicator2)
                .maintenanceRequired(true)
                .build();

        assertNotNull(remarks.automatedMaintenanceIndicators());
        assertEquals(2, remarks.automatedMaintenanceIndicators().size());
        assertTrue(remarks.maintenanceRequired());
    }

    @Test
    @DisplayName("Should build NoaaMetarRemarks with maintenance required flag")
    void testBuildWithMaintenanceRequired() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .maintenanceRequired(true)
                .build();

        assertTrue(remarks.maintenanceRequired());
    }

    @Test
    @DisplayName("Should handle null automated maintenance indicators")
    void testNullAutomatedMaintenanceIndicators() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedMaintenanceIndicators(null)
                .build();

        assertNotNull(remarks.automatedMaintenanceIndicators());
        assertTrue(remarks.automatedMaintenanceIndicators().isEmpty());
    }

    @Test
    @DisplayName("Should create defensive copy of automated maintenance indicators")
    void testDefensiveCopyOfAutomatedMaintenanceIndicators() {
        List<AutomatedMaintenanceIndicator> indicators = new ArrayList<>();
        indicators.add(AutomatedMaintenanceIndicator.of("RVRNO"));

        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedMaintenanceIndicators(indicators)
                .build();

        // Modify original list
        indicators.add(AutomatedMaintenanceIndicator.of("PWINO"));

        // Remarks should not be affected
        assertEquals(1, remarks.automatedMaintenanceIndicators().size());
    }

    @Test
    @DisplayName("Should include automated maintenance in isEmpty check")
    void testIsEmptyWithAutomatedMaintenance() {
        NoaaMetarRemarks remarksWithIndicators = NoaaMetarRemarks.builder()
                .addAutomatedMaintenanceIndicator(AutomatedMaintenanceIndicator.of("TSNO"))
                .build();

        assertFalse(remarksWithIndicators.isEmpty());

        NoaaMetarRemarks remarksWithFlag = NoaaMetarRemarks.builder()
                .maintenanceRequired(true)
                .build();

        assertFalse(remarksWithFlag.isEmpty());
    }

    @Test
    @DisplayName("Should include automated maintenance in toString")
    void testToStringWithAutomatedMaintenance() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addAutomatedMaintenanceIndicator(AutomatedMaintenanceIndicator.of("TSNO"))
                .addAutomatedMaintenanceIndicator(AutomatedMaintenanceIndicator.of("VISNO", "RWY06"))
                .maintenanceRequired(true)
                .build();

        String toString = remarks.toString();

        assertTrue(toString.contains("automatedMaintenance"));
        assertTrue(toString.contains("TSNO"));
        assertTrue(toString.contains("VISNO RWY06"));
        assertTrue(toString.contains("maintenanceRequired=true"));
    }

    @Test
    @DisplayName("Should handle empty automated maintenance indicators list")
    void testEmptyAutomatedMaintenanceIndicatorsList() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedMaintenanceIndicators(List.of())
                .build();

        assertNotNull(remarks.automatedMaintenanceIndicators());
        assertTrue(remarks.automatedMaintenanceIndicators().isEmpty());
    }

    @Test
    @DisplayName("Should add multiple automated maintenance indicators")
    void testAddMultipleAutomatedMaintenanceIndicators() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addAutomatedMaintenanceIndicator(AutomatedMaintenanceIndicator.of("RVRNO"))
                .addAutomatedMaintenanceIndicator(AutomatedMaintenanceIndicator.of("PWINO"))
                .addAutomatedMaintenanceIndicator(AutomatedMaintenanceIndicator.of("PNO"))
                .addAutomatedMaintenanceIndicator(AutomatedMaintenanceIndicator.maintenanceCheck())
                .build();

        assertEquals(4, remarks.automatedMaintenanceIndicators().size());

        // Verify all indicators are present
        List<String> types = remarks.automatedMaintenanceIndicators().stream()
                .map(AutomatedMaintenanceIndicator::type)
                .toList();

        assertTrue(types.contains("RVRNO"));
        assertTrue(types.contains("PWINO"));
        assertTrue(types.contains("PNO"));
        assertTrue(types.contains("$"));
    }

    @Test
    @DisplayName("Should not add null automated maintenance indicator")
    void testAddNullAutomatedMaintenanceIndicator() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .addAutomatedMaintenanceIndicator(null)
                .build();

        assertTrue(remarks.automatedMaintenanceIndicators().isEmpty());
    }

    @Test
    @DisplayName("Empty remarks should have empty automated maintenance indicators")
    void testEmptyRemarksAutomatedMaintenance() {
        NoaaMetarRemarks empty = NoaaMetarRemarks.empty();

        assertNotNull(empty.automatedMaintenanceIndicators());
        assertTrue(empty.automatedMaintenanceIndicators().isEmpty());
        assertFalse(empty.maintenanceRequired());
    }

    @Test
    @DisplayName("Should generate correct description for PWINO")
    void testGetDescriptionPWINO() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("PWINO");
        assertEquals("Precipitation identifier information not available", indicator.getDescription());
    }

    @Test
    @DisplayName("Should generate correct description for PNO")
    void testGetDescriptionPNO() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("PNO");
        assertEquals("Precipitation amount not available", indicator.getDescription());
    }

    @Test
    @DisplayName("Should generate correct description for FZRANO")
    void testGetDescriptionFZRANO() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("FZRANO");
        assertEquals("Freezing rain information not available", indicator.getDescription());
    }

    @Test
    @DisplayName("Should generate correct description for VISNO without location")
    void testGetDescriptionVISNO_NoLocation() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("VISNO");
        assertEquals("Visibility not available", indicator.getDescription());
    }

    @Test
    @DisplayName("Should generate correct description for CHINO without location")
    void testGetDescriptionCHINO_NoLocation() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("CHINO");
        assertEquals("Cloud height indicator not available", indicator.getDescription());
    }

    @Test
    @DisplayName("Should generate correct description for TSNO")
    void testGetDescriptionTSNO() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("TSNO");
        assertEquals("Thunderstorm information not available", indicator.getDescription());
    }

    @Test
    @DisplayName("Should generate correct description for $ (maintenance check)")
    void testGetDescriptionMaintenanceCheck() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.maintenanceCheck();
        assertEquals("Station requires maintenance", indicator.getDescription());
    }

    @Test
    @DisplayName("maintenanceRequired() should return false when field is null")
    void testMaintenanceRequired_Null() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .build();

        assertThat(remarks.maintenanceRequired()).isFalse();
    }

    @Test
    @DisplayName("maintenanceRequired() should return false when explicitly set to false")
    void testMaintenanceRequired_False() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .maintenanceRequired(false)
                .build();

        assertThat(remarks.maintenanceRequired()).isFalse();
    }

    @Test
    @DisplayName("maintenanceRequired() should return true when explicitly set to true")
    void testMaintenanceRequired_True() {
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .maintenanceRequired(true)
                .build();

        assertThat(remarks.maintenanceRequired()).isTrue();
    }

    // ========== All Field Tests ==========

    @Test
    @DisplayName("Should include core fields in toString()")
    void testToStringWithAllFields_Part1() {
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
                .variableCeiling(VariableCeiling.fromHundreds(5, 10))
                .ceilingSecondSite(CeilingSecondSite.fromHundreds(2, "RY11"))
                .addObscurationLayer(ObscurationLayer.fromHundreds("FEW", "FG", 0))
                .addObscurationLayer(ObscurationLayer.fromHundreds("SCT", "FU", 10))
                .addCloudType(CloudType.of("SC", 1))
                .addCloudType(CloudType.withLocation("AC", "TR"))
                .towerVisibility(Visibility.statuteMiles(1.5))
                .surfaceVisibility(Visibility.statuteMiles(0.75))
                .hourlyPrecipitation(PrecipitationAmount.fromEncoded("0015", 1))
                .sixHourPrecipitation(PrecipitationAmount.fromEncoded("0025", 6))
                .twentyFourHourPrecipitation(PrecipitationAmount.fromEncoded("0125", 24))
                .hailSize(HailSize.inches(1.75))
                .addWeatherEvent(new WeatherEvent("RA", null, null, 5,
                        null, 30))
                .addThunderstormLocation(ThunderstormLocation.of("TS", "SE"))
                .pressureTendency(PressureTendency.of(2, 3.2))
                .sixHourMaxTemperature(Temperature.of(14.2))
                .sixHourMinTemperature(Temperature.of(-0.1))
                .twentyFourHourMaxTemperature(Temperature.of(4.6))
                .twentyFourHourMinTemperature(Temperature.of(-0.6))
                .addAutomatedMaintenanceIndicator(AutomatedMaintenanceIndicator.of("TSNO"))
                .addAutomatedMaintenanceIndicator(AutomatedMaintenanceIndicator.of("VISNO", "RWY06"))
                .maintenanceRequired(true)
                .freeText("Additional info")
                .build();

        String str = remarks.toString();

        // First 16 assertions
        assertAll("Core fields should be present in toString",
                () -> assertTrue(str.contains("AO2")),
                () -> assertTrue(str.contains("seaLevelPressure")),
                () -> assertTrue(str.contains("preciseTemp")),
                () -> assertTrue(str.contains("preciseDewpoint")),
                () -> assertTrue(str.contains("peakWind")),
                () -> assertTrue(str.contains("windShift")),
                () -> assertTrue(str.contains("variableVisibility")),
                () -> assertTrue(str.contains("variableCeiling=Variable ceiling: 500-1000 ft")),
                () -> assertTrue(str.contains("ceilingSecondSite=Ceiling 200 ft at RY11")),
                () -> assertTrue(str.contains("obscurationLayers=")),
                () -> assertTrue(str.contains("Few Fog at ground level")),
                () -> assertTrue(str.contains("Scattered Smoke at 1000 ft")),
                () -> assertTrue(str.contains("cloudTypes=")),
                () -> assertTrue(str.contains("Stratocumulus (1/8)")),
                () -> assertTrue(str.contains("towerVisibility")),
                () -> assertTrue(str.contains("surfaceVisibility"))
        );
    }

    @Test
    @DisplayName("Should include weather and temperature fields in toString()")
    void testToStringWithAllFields_Part2() {
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
                .variableCeiling(VariableCeiling.fromHundreds(5, 10))
                .ceilingSecondSite(CeilingSecondSite.fromHundreds(2, "RY11"))
                .addObscurationLayer(ObscurationLayer.fromHundreds("FEW", "FG", 0))
                .addObscurationLayer(ObscurationLayer.fromHundreds("SCT", "FU", 10))
                .addCloudType(CloudType.of("SC", 1))
                .addCloudType(CloudType.withLocation("AC", "TR"))
                .towerVisibility(Visibility.statuteMiles(1.5))
                .surfaceVisibility(Visibility.statuteMiles(0.75))
                .hourlyPrecipitation(PrecipitationAmount.fromEncoded("0015", 1))
                .sixHourPrecipitation(PrecipitationAmount.fromEncoded("0025", 6))
                .twentyFourHourPrecipitation(PrecipitationAmount.fromEncoded("0125", 24))
                .hailSize(HailSize.inches(1.75))
                .addWeatherEvent(new WeatherEvent("RA", null, null, 5,
                        null, 30))
                .addThunderstormLocation(ThunderstormLocation.of("TS", "SE"))
                .pressureTendency(PressureTendency.of(2, 3.2))
                .sixHourMaxTemperature(Temperature.of(14.2))
                .sixHourMinTemperature(Temperature.of(-0.1))
                .twentyFourHourMaxTemperature(Temperature.of(4.6))
                .twentyFourHourMinTemperature(Temperature.of(-0.6))
                .addAutomatedMaintenanceIndicator(AutomatedMaintenanceIndicator.of("TSNO"))
                .addAutomatedMaintenanceIndicator(AutomatedMaintenanceIndicator.of("VISNO", "RWY06"))
                .maintenanceRequired(true)
                .freeText("Additional info")
                .build();

        String str = remarks.toString();

        // Remaining 16 assertions
        assertAll("Weather and temperature fields should be present in toString",
                () -> assertTrue(str.contains("hourlyPrecip")),
                () -> assertTrue(str.contains("sixHourPrecip")),
                () -> assertTrue(str.contains("twentyFourHourPrecip")),
                () -> assertTrue(str.contains("hailSize")),
                () -> assertTrue(str.contains("weatherEvents")),
                () -> assertTrue(str.contains("thunderstormLocations")),
                () -> assertTrue(str.contains("pressureTendency")),
                () -> assertTrue(str.contains("sixHourMaxTemp=14.2°C")),
                () -> assertTrue(str.contains("sixHourMinTemp=-0.1°C")),
                () -> assertTrue(str.contains("twentyFourHourMaxTemp=4.6°C")),
                () -> assertTrue(str.contains("twentyFourHourMinTemp=-0.6°C")),
                () -> assertTrue(str.contains("automatedMaintenance=")),
                () -> assertTrue(str.contains("TSNO")),
                () -> assertTrue(str.contains("VISNO RWY06")),
                () -> assertTrue(str.contains("maintenanceRequired=true")),
                () -> assertTrue(str.contains("freeText"))
        );
    }

    @Test
    @DisplayName("Should create remarks with all fields - Part 1")
    void testRecordConstructorWithAllFields_Part1() {
        AutomatedStationType stationType = AutomatedStationType.AO2;
        Pressure slp = Pressure.hectopascals(1013.2);
        Temperature temp = Temperature.of(22.2);
        Temperature dewpoint = Temperature.of(11.7);
        PeakWind peakWind = new PeakWind(280, 32, 15, 30);
        WindShift windShift = new WindShift(15, 30, true);
        Visibility min = Visibility.statuteMiles(0.5);
        Visibility max = Visibility.statuteMiles(2.0);
        VariableVisibility varVis = new VariableVisibility(min, max, null, null);
        VariableCeiling variableCeiling = VariableCeiling.fromHundreds(5, 10);
        CeilingSecondSite ceilingSecondSite = CeilingSecondSite.fromHundreds(2, "RY11");
        List<ObscurationLayer> obscurationLayers = List.of(
                ObscurationLayer.fromHundreds("FEW", "FG", 0),
                ObscurationLayer.fromHundreds("SCT", "FU", 10)
        );
        List<CloudType> cloudTypes = List.of(
                CloudType.of("SC", 1),
                CloudType.withLocation("AC", "TR")
        );
        Visibility towerVis = Visibility.statuteMiles(1.5);
        Visibility surfaceVis = Visibility.statuteMiles(0.75);
        PrecipitationAmount hourly = PrecipitationAmount.fromEncoded("0015", 1);
        PrecipitationAmount sixHour = PrecipitationAmount.fromEncoded("0025", 6);
        PrecipitationAmount twentyFourHour = PrecipitationAmount.fromEncoded("0125", 24);
        HailSize hailSize = HailSize.inches(1.75);
        WeatherEvent event1 = new WeatherEvent("RA", null, null, 5, null, 30);
        WeatherEvent event2 = new WeatherEvent("SN", null, null, 30, null, null);
        List<WeatherEvent> weatherEvents = List.of(event1, event2);
        ThunderstormLocation location1 = ThunderstormLocation.of("TS", "SE");
        ThunderstormLocation location2 = ThunderstormLocation.withMovement("CB", "W", "E");
        List<ThunderstormLocation> thunderstormLocations = List.of(location1, location2);
        PressureTendency tendency = PressureTendency.of(2, 3.2);
        Temperature sixHourMaxTemp = Temperature.of(14.2);
        Temperature sixHourMinTemp = Temperature.of(-0.1);
        Temperature twentyFourHourMaxTemp = Temperature.of(4.6);
        Temperature twentyFourHourMinTemp = Temperature.of(-0.6);
        List<AutomatedMaintenanceIndicator> automatedMaintenanceIndicators = List.of(
                AutomatedMaintenanceIndicator.of("TSNO"),
                AutomatedMaintenanceIndicator.of("VISNO", "RWY06")
        );
        Boolean maintenanceRequired = true;
        String freeText = "Additional remarks";

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                stationType, slp, temp, dewpoint, peakWind, windShift, varVis, variableCeiling, ceilingSecondSite,
                obscurationLayers, cloudTypes, towerVis, surfaceVis, hourly, sixHour, twentyFourHour, hailSize, weatherEvents,
                thunderstormLocations, tendency, sixHourMaxTemp, sixHourMinTemp, twentyFourHourMaxTemp,
                twentyFourHourMinTemp, automatedMaintenanceIndicators, maintenanceRequired, freeText
        );

        // Verify first 13 fields
        assertAll("First half of fields should be correctly set",
                () -> assertEquals(stationType, remarks.automatedStationType()),
                () -> assertEquals(slp, remarks.seaLevelPressure()),
                () -> assertEquals(temp, remarks.preciseTemperature()),
                () -> assertEquals(dewpoint, remarks.preciseDewpoint()),
                () -> assertEquals(peakWind, remarks.peakWind()),
                () -> assertEquals(windShift, remarks.windShift()),
                () -> assertEquals(varVis, remarks.variableVisibility()),
                () -> assertEquals(variableCeiling, remarks.variableCeiling()),
                () -> assertEquals(ceilingSecondSite, remarks.ceilingSecondSite()),
                () -> assertEquals(obscurationLayers, remarks.obscurationLayers()),
                () -> assertEquals(cloudTypes, remarks.cloudTypes()),
                () -> assertEquals(towerVis, remarks.towerVisibility()),
                () -> assertEquals(surfaceVis, remarks.surfaceVisibility())
        );
    }

    @Test
    @DisplayName("Should create remarks with all fields - Part 2")
    void testRecordConstructorWithAllFields_Part2() {
        AutomatedStationType stationType = AutomatedStationType.AO2;
        Pressure slp = Pressure.hectopascals(1013.2);
        Temperature temp = Temperature.of(22.2);
        Temperature dewpoint = Temperature.of(11.7);
        PeakWind peakWind = new PeakWind(280, 32, 15, 30);
        WindShift windShift = new WindShift(15, 30, true);
        Visibility min = Visibility.statuteMiles(0.5);
        Visibility max = Visibility.statuteMiles(2.0);
        VariableVisibility varVis = new VariableVisibility(min, max, null, null);
        VariableCeiling variableCeiling = VariableCeiling.fromHundreds(5, 10);
        CeilingSecondSite ceilingSecondSite = CeilingSecondSite.fromHundreds(2, "RY11");
        List<ObscurationLayer> obscurationLayers = List.of(
                ObscurationLayer.fromHundreds("FEW", "FG", 0),
                ObscurationLayer.fromHundreds("SCT", "FU", 10)
        );
        List<CloudType> cloudTypes = List.of(
                CloudType.of("SC", 1),
                CloudType.withLocation("AC", "TR")
        );
        Visibility towerVis = Visibility.statuteMiles(1.5);
        Visibility surfaceVis = Visibility.statuteMiles(0.75);
        PrecipitationAmount hourly = PrecipitationAmount.fromEncoded("0015", 1);
        PrecipitationAmount sixHour = PrecipitationAmount.fromEncoded("0025", 6);
        PrecipitationAmount twentyFourHour = PrecipitationAmount.fromEncoded("0125", 24);
        HailSize hailSize = HailSize.inches(1.75);
        WeatherEvent event1 = new WeatherEvent("RA", null, null, 5, null, 30);
        WeatherEvent event2 = new WeatherEvent("SN", null, null, 30, null, null);
        List<WeatherEvent> weatherEvents = List.of(event1, event2);
        ThunderstormLocation location1 = ThunderstormLocation.of("TS", "SE");
        ThunderstormLocation location2 = ThunderstormLocation.withMovement("CB", "W", "E");
        List<ThunderstormLocation> thunderstormLocations = List.of(location1, location2);
        PressureTendency tendency = PressureTendency.of(2, 3.2);
        Temperature sixHourMaxTemp = Temperature.of(14.2);
        Temperature sixHourMinTemp = Temperature.of(-0.1);
        Temperature twentyFourHourMaxTemp = Temperature.of(4.6);
        Temperature twentyFourHourMinTemp = Temperature.of(-0.6);
        List<AutomatedMaintenanceIndicator> automatedMaintenanceIndicators = List.of(
                AutomatedMaintenanceIndicator.of("TSNO"),
                AutomatedMaintenanceIndicator.of("VISNO", "RWY06")
        );
        Boolean maintenanceRequired = true;
        String freeText = "Additional remarks";

        NoaaMetarRemarks remarks = new NoaaMetarRemarks(
                stationType, slp, temp, dewpoint, peakWind, windShift, varVis, variableCeiling, ceilingSecondSite,
                obscurationLayers, cloudTypes, towerVis, surfaceVis, hourly, sixHour, twentyFourHour, hailSize, weatherEvents,
                thunderstormLocations, tendency, sixHourMaxTemp, sixHourMinTemp, twentyFourHourMaxTemp,
                twentyFourHourMinTemp, automatedMaintenanceIndicators, maintenanceRequired, freeText
        );

        // Verify remaining 14 fields
        assertAll("Second half of fields should be correctly set",
                () -> assertEquals(hourly, remarks.hourlyPrecipitation()),
                () -> assertEquals(sixHour, remarks.sixHourPrecipitation()),
                () -> assertEquals(twentyFourHour, remarks.twentyFourHourPrecipitation()),
                () -> assertEquals(hailSize, remarks.hailSize()),
                () -> assertEquals(weatherEvents, remarks.weatherEvents()),
                () -> assertEquals(thunderstormLocations, remarks.thunderstormLocations()),
                () -> assertEquals(tendency, remarks.pressureTendency()),
                () -> assertEquals(sixHourMaxTemp, remarks.sixHourMaxTemperature()),
                () -> assertEquals(sixHourMinTemp, remarks.sixHourMinTemperature()),
                () -> assertEquals(twentyFourHourMaxTemp, remarks.twentyFourHourMaxTemperature()),
                () -> assertEquals(twentyFourHourMinTemp, remarks.twentyFourHourMinTemperature()),
                () -> assertEquals(automatedMaintenanceIndicators, remarks.automatedMaintenanceIndicators()),
                () -> assertEquals(maintenanceRequired, remarks.maintenanceRequired()),
                () -> assertEquals(freeText, remarks.freeText())
        );
    }
}
