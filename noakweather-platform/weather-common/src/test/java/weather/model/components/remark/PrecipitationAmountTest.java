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
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PrecipitationAmount record.
 *
 * @author bclasky1539
 *
 */
class PrecipitationAmountTest {

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Should create precipitation amount with valid parameters")
    void testValidConstructor() {
        PrecipitationAmount precip = new PrecipitationAmount(0.15, 1, false);

        assertEquals(0.15, precip.inches());
        assertEquals(1, precip.periodHours());
        assertFalse(precip.isTrace());
    }

    @Test
    @DisplayName("Should create trace precipitation with null inches")
    void testTraceWithNullInches() {
        PrecipitationAmount precip = new PrecipitationAmount(null, 1, true);

        assertNull(precip.inches());
        assertTrue(precip.isTrace());
    }

    @Test
    @DisplayName("Should throw exception for invalid period hours")
    void testInvalidPeriodHours() {
        assertThrows(IllegalArgumentException.class,
                () -> new PrecipitationAmount(0.15, 5, false),
                "Should reject period of 5 hours");

        assertThrows(IllegalArgumentException.class,
                () -> new PrecipitationAmount(0.15, 0, false),
                "Should reject period of 0 hours");

        assertThrows(IllegalArgumentException.class,
                () -> new PrecipitationAmount(0.15, -1, false),
                "Should reject negative period");
    }

    @Test
    @DisplayName("Should throw exception for negative precipitation amount")
    void testNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new PrecipitationAmount(-0.5, 1, false),
                "Should reject negative precipitation");
    }

    @Test
    @DisplayName("Should throw exception for trace with measurable amount")
    void testTraceWithMeasurableAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new PrecipitationAmount(0.15, 1, true),
                "Trace should not have measurable amount >= 0.01");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 6, 24})
    @DisplayName("Should accept all valid period hours")
    void testValidPeriodHours(int periodHours) {
        PrecipitationAmount precip = new PrecipitationAmount(0.15, periodHours, false);
        assertEquals(periodHours, precip.periodHours());
    }


    // ==================== fromEncoded() Factory Method Tests ====================

    @ParameterizedTest
    @CsvSource({
            "0015, 1, 0.15, 'Quarter inch'",
            "0009, 6, 0.09, 'Less than tenth'",
            "0125, 24, 1.25, 'Over an inch'",
            "0000, 1, 0.0, 'Zero precipitation'",
            "9999, 1, 99.99, 'Maximum value'"
    })
    @DisplayName("Should parse encoded precipitation values")
    void testFromEncoded(String encoded, int periodHours, double expectedInches, String scenario) {
        PrecipitationAmount precip = PrecipitationAmount.fromEncoded(encoded, periodHours);

        assertEquals(expectedInches, precip.inches(), 0.001, scenario);
        assertEquals(periodHours, precip.periodHours(), scenario);
        assertFalse(precip.isTrace(), scenario);
    }

    @ParameterizedTest
    @ValueSource(strings = {"////", "///", "/////", "/"})
    @DisplayName("Should parse trace precipitation (slashes)")
    void testFromEncodedTrace(String encoded) {
        PrecipitationAmount precip = PrecipitationAmount.fromEncoded(encoded, 1);

        assertNull(precip.inches());
        assertTrue(precip.isTrace());
        assertEquals(1, precip.periodHours());
    }

    @Test
    @DisplayName("Should throw exception for null encoded value")
    void testFromEncodedNull() {
        assertThrows(NullPointerException.class,
                () -> PrecipitationAmount.fromEncoded(null, 1));
    }

    @Test
    @DisplayName("Should throw exception for blank encoded value")
    void testFromEncodedBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> PrecipitationAmount.fromEncoded("", 1));

        assertThrows(IllegalArgumentException.class,
                () -> PrecipitationAmount.fromEncoded("   ", 1));
    }

    @Test
    @DisplayName("Should throw exception for non-numeric encoded value")
    void testFromEncodedNonNumeric() {
        assertThrows(IllegalArgumentException.class,
                () -> PrecipitationAmount.fromEncoded("ABCD", 1));
    }

    @Test
    @DisplayName("Should throw exception for invalid period in fromEncoded")
    void testFromEncodedInvalidPeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> PrecipitationAmount.fromEncoded("0015", 5));
    }


    // ==================== trace() Factory Method Tests ====================

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 6, 24})
    @DisplayName("Should create trace precipitation for all periods")
    void testTraceFactory(int periodHours) {
        PrecipitationAmount precip = PrecipitationAmount.trace(periodHours);

        assertNull(precip.inches());
        assertTrue(precip.isTrace());
        assertEquals(periodHours, precip.periodHours());
    }


    // ==================== inches() Factory Method Tests ====================

    @Test
    @DisplayName("Should create precipitation from inches value")
    void testInchesFactory() {
        PrecipitationAmount precip = PrecipitationAmount.inches(0.25, 6);

        assertEquals(0.25, precip.inches());
        assertEquals(6, precip.periodHours());
        assertFalse(precip.isTrace());
    }

    @Test
    @DisplayName("Should throw exception for negative inches in factory")
    void testInchesFactoryNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> PrecipitationAmount.inches(-0.5, 1));
    }


    // ==================== Conversion Tests ====================

    @Test
    @DisplayName("Should convert inches to millimeters")
    void testToMillimeters() {
        PrecipitationAmount precip = new PrecipitationAmount(1.0, 1, false);

        Double mm = precip.toMillimeters();
        assertNotNull(mm);
        assertEquals(25.4, mm, 0.01, "1 inch should equal 25.4 mm");
    }

    @Test
    @DisplayName("Should return null millimeters for trace")
    void testToMillimetersTrace() {
        PrecipitationAmount precip = PrecipitationAmount.trace(1);
        assertNull(precip.toMillimeters());
    }

    @ParameterizedTest
    @CsvSource({
            "0.15, 3.81",
            "0.25, 6.35",
            "0.50, 12.70",
            "1.25, 31.75"
    })
    @DisplayName("Should correctly convert various amounts to millimeters")
    void testVariousConversions(double inches, double expectedMm) {
        PrecipitationAmount precip = new PrecipitationAmount(inches, 1, false);

        Double mm = precip.toMillimeters();
        assertNotNull(mm);
        assertEquals(expectedMm, mm, 0.01);
    }


    // ==================== Query Method Tests ====================

    @Test
    @DisplayName("Should identify measurable precipitation")
    void testIsMeasurable() {
        PrecipitationAmount measurable = new PrecipitationAmount(0.15, 1, false);
        assertTrue(measurable.isMeasurable());
    }

    @Test
    @DisplayName("Should identify trace as not measurable")
    void testIsMeasurableTrace() {
        PrecipitationAmount trace = PrecipitationAmount.trace(1);
        assertFalse(trace.isMeasurable());
    }

    @Test
    @DisplayName("Should identify zero as not measurable")
    void testIsMeasurableZero() {
        PrecipitationAmount zero = new PrecipitationAmount(0.0, 1, false);
        assertFalse(zero.isMeasurable());
    }

    @Test
    @DisplayName("Should identify very small amount as not measurable")
    void testIsMeasurableVerySmall() {
        PrecipitationAmount verySmall = new PrecipitationAmount(0.005, 1, false);
        assertFalse(verySmall.isMeasurable());
    }

    @Test
    @DisplayName("Should identify hourly precipitation")
    void testIsHourly() {
        PrecipitationAmount hourly = new PrecipitationAmount(0.15, 1, false);
        assertTrue(hourly.isHourly());
        assertFalse(hourly.isSixHour());
        assertFalse(hourly.isTwentyFourHour());
    }

    @Test
    @DisplayName("Should identify 6-hour precipitation")
    void testIsSixHour() {
        PrecipitationAmount sixHour = new PrecipitationAmount(0.25, 6, false);
        assertFalse(sixHour.isHourly());
        assertTrue(sixHour.isSixHour());
        assertFalse(sixHour.isTwentyFourHour());
    }

    @Test
    @DisplayName("Should identify 24-hour precipitation")
    void testIsTwentyFourHour() {
        PrecipitationAmount twentyFourHour = new PrecipitationAmount(1.25, 24, false);
        assertFalse(twentyFourHour.isHourly());
        assertFalse(twentyFourHour.isSixHour());
        assertTrue(twentyFourHour.isTwentyFourHour());
    }


    // ==================== getDescription() Tests ====================

    @Test
    @DisplayName("Should generate description for measurable precipitation")
    void testGetDescriptionMeasurable() {
        PrecipitationAmount precip = new PrecipitationAmount(0.15, 1, false);
        String description = precip.getDescription();

        assertTrue(description.contains("0.15"));
        assertTrue(description.contains("inches"));
        assertTrue(description.contains("1 hour"));
    }

    @Test
    @DisplayName("Should generate description for trace precipitation")
    void testGetDescriptionTrace() {
        PrecipitationAmount precip = PrecipitationAmount.trace(6);
        String description = precip.getDescription();

        assertTrue(description.contains("Trace"));
        assertTrue(description.contains("6 hour"));
    }

    @Test
    @DisplayName("Should generate description for missing data")
    void testGetDescriptionMissing() {
        PrecipitationAmount precip = new PrecipitationAmount(null, 24, false);
        String description = precip.getDescription();

        assertTrue(description.contains("missing"));
        assertTrue(description.contains("24 hour"));
    }

    @ParameterizedTest
    @CsvSource({
            "0.15, 1, '0.15 inches (1 hour)'",
            "0.25, 6, '0.25 inches (6 hour)'",
            "1.25, 24, '1.25 inches (24 hour)'"
    })
    @DisplayName("Should format descriptions correctly for various periods")
    void testDescriptionFormats(double inches, int periodHours, String expected) {
        PrecipitationAmount precip = new PrecipitationAmount(inches, periodHours, false);
        assertEquals(expected, precip.getDescription());
    }


    // ==================== Equality and HashCode Tests ====================

    @Test
    @DisplayName("Should be equal when all fields match")
    void testEquality() {
        PrecipitationAmount precip1 = new PrecipitationAmount(0.15, 1, false);
        PrecipitationAmount precip2 = new PrecipitationAmount(0.15, 1, false);

        assertEquals(precip1, precip2);
        assertEquals(precip1.hashCode(), precip2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when inches differ")
    void testInequalityDifferentInches() {
        PrecipitationAmount precip1 = new PrecipitationAmount(0.15, 1, false);
        PrecipitationAmount precip2 = new PrecipitationAmount(0.25, 1, false);

        assertNotEquals(precip1, precip2);
    }

    @Test
    @DisplayName("Should not be equal when period differs")
    void testInequalityDifferentPeriod() {
        PrecipitationAmount precip1 = new PrecipitationAmount(0.15, 1, false);
        PrecipitationAmount precip2 = new PrecipitationAmount(0.15, 6, false);

        assertNotEquals(precip1, precip2);
    }

    @Test
    @DisplayName("Should not be equal when trace differs")
    void testInequalityDifferentTrace() {
        PrecipitationAmount precip1 = PrecipitationAmount.trace(1);
        PrecipitationAmount precip2 = new PrecipitationAmount(0.0, 1, false);

        assertNotEquals(precip1, precip2);
    }


    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle maximum reasonable precipitation")
    void testMaximumPrecipitation() {
        PrecipitationAmount precip = new PrecipitationAmount(99.99, 24, false);

        assertEquals(99.99, precip.inches());
        assertTrue(precip.isMeasurable());

        Double mm = precip.toMillimeters();
        assertNotNull(mm);
        assertEquals(2539.746, mm, 0.01);
    }

    @Test
    @DisplayName("Should handle zero precipitation")
    void testZeroPrecipitation() {
        PrecipitationAmount precip = new PrecipitationAmount(0.0, 1, false);

        assertEquals(0.0, precip.inches());
        assertFalse(precip.isMeasurable());

        Double mm = precip.toMillimeters();
        assertNotNull(mm);
        assertEquals(0.0, mm);
    }

    @Test
    @DisplayName("Should handle very small measurable precipitation")
    void testVerySmallMeasurable() {
        PrecipitationAmount precip = new PrecipitationAmount(0.01, 1, false);

        assertEquals(0.01, precip.inches());
        assertTrue(precip.isMeasurable(), "0.01 inches should be measurable");
    }

    @Test
    @DisplayName("Should handle 3-hour period")
    void testThreeHourPeriod() {
        PrecipitationAmount precip = new PrecipitationAmount(0.50, 3, false);

        assertEquals(3, precip.periodHours());
        assertFalse(precip.isHourly());
        assertFalse(precip.isSixHour());
        assertFalse(precip.isTwentyFourHour());
    }
}
