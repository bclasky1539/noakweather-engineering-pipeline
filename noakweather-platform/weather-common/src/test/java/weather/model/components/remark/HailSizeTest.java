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
 * Unit tests for HailSize record.
 *
 * @author bclasky1539
 *
 */
class HailSizeTest {

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Should create hail size with valid inches")
    void testValidConstructor() {
        HailSize hailSize = new HailSize(1.75);

        assertEquals(1.75, hailSize.inches(), 0.001);
    }

    @Test
    @DisplayName("Should throw exception for zero size")
    void testZeroSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new HailSize(0.0),
                "Should reject zero size");
    }

    @Test
    @DisplayName("Should throw exception for negative size")
    void testNegativeSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new HailSize(-0.5),
                "Should reject negative size");
    }

    @Test
    @DisplayName("Should throw exception for unreasonably large size")
    void testUnreasonablyLargeSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new HailSize(10.1),
                "Should reject size > 10 inches");
    }

    @Test
    @DisplayName("Should accept maximum reasonable size")
    void testMaximumReasonableSize() {
        HailSize hailSize = new HailSize(10.0);
        assertEquals(10.0, hailSize.inches(), 0.001);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.25, 0.5, 0.75, 1.0, 1.75, 2.0, 2.5, 3.5, 5.0})
    @DisplayName("Should accept various valid sizes")
    void testVariousValidSizes(double size) {
        HailSize hailSize = new HailSize(size);
        assertEquals(size, hailSize.inches(), 0.001);
    }


    // ==================== Conversion Tests ====================

    @Test
    @DisplayName("Should convert inches to centimeters")
    void testToCentimeters() {
        HailSize hailSize = new HailSize(1.0);

        assertEquals(2.54, hailSize.toCentimeters(), 0.01);
    }

    @Test
    @DisplayName("Should convert inches to millimeters")
    void testToMillimeters() {
        HailSize hailSize = new HailSize(1.0);

        assertEquals(25.4, hailSize.toMillimeters(), 0.1);
    }

    @ParameterizedTest
    @CsvSource({
            "0.5, 1.27, 12.7",
            "0.75, 1.905, 19.05",
            "1.0, 2.54, 25.4",
            "1.75, 4.445, 44.45",
            "2.0, 5.08, 50.8"
    })
    @DisplayName("Should correctly convert various sizes")
    void testVariousConversions(double inches, double expectedCm, double expectedMm) {
        HailSize hailSize = new HailSize(inches);

        assertEquals(expectedCm, hailSize.toCentimeters(), 0.01);
        assertEquals(expectedMm, hailSize.toMillimeters(), 0.1);
    }


    // ==================== Size Category Tests ====================

    @ParameterizedTest
    @CsvSource({
            "0.20, 'Pea-sized'",
            "0.30, 'Marble-sized'",
            "0.60, 'Penny-sized'",
            "0.80, 'Nickel-sized'",
            "1.00, 'Quarter-sized'",
            "1.60, 'Golf ball-sized'",
            "2.00, 'Tennis ball-sized'",
            "2.60, 'Baseball-sized'",
            "3.00, 'Softball-sized'",
            "4.50, 'Grapefruit-sized or larger'"
    })
    @DisplayName("Should correctly categorize hail sizes")
    void testSizeCategories(double inches, String expectedCategory) {
        HailSize hailSize = new HailSize(inches);
        assertEquals(expectedCategory, hailSize.getSizeCategory());
    }

    @Test
    @DisplayName("Should categorize pea-sized hail")
    void testPeaSized() {
        HailSize hailSize = new HailSize(0.20);
        assertEquals("Pea-sized", hailSize.getSizeCategory());
    }

    @Test
    @DisplayName("Should categorize marble-sized hail")
    void testMarbleSized() {
        HailSize hailSize = new HailSize(0.40);
        assertEquals("Marble-sized", hailSize.getSizeCategory());
    }

    @Test
    @DisplayName("Should categorize quarter-sized hail")
    void testQuarterSized() {
        HailSize hailSize = new HailSize(1.00);
        assertEquals("Quarter-sized", hailSize.getSizeCategory());
    }

    @Test
    @DisplayName("Should categorize golf ball-sized hail")
    void testGolfBallSized() {
        HailSize hailSize = new HailSize(1.60);  // ← Use 1.60 for golf ball
        assertEquals("Golf ball-sized", hailSize.getSizeCategory());
    }

    @Test
    @DisplayName("Should categorize softball-sized hail")
    void testSoftballSized() {
        HailSize hailSize = new HailSize(3.50);
        assertEquals("Softball-sized", hailSize.getSizeCategory());
    }


    // ==================== Severity Tests ====================

    @Test
    @DisplayName("Should identify severe hail (1.0 inch)")
    void testIsSevereAtThreshold() {
        HailSize hailSize = new HailSize(1.0);
        assertTrue(hailSize.isSevere(), "1.0 inch should be severe");
    }

    @Test
    @DisplayName("Should identify severe hail (above 1.0 inch)")
    void testIsSevereAboveThreshold() {
        HailSize hailSize = new HailSize(1.5);
        assertTrue(hailSize.isSevere());
    }

    @Test
    @DisplayName("Should identify non-severe hail (below 1.0 inch)")
    void testNotSevereBelowThreshold() {
        HailSize hailSize = new HailSize(0.75);
        assertFalse(hailSize.isSevere(), "0.75 inch should not be severe");
    }

    @Test
    @DisplayName("Should identify significantly severe hail (2.0 inches)")
    void testIsSignificantlySevereAtThreshold() {
        HailSize hailSize = new HailSize(2.0);
        assertTrue(hailSize.isSignificantlySevere(), "2.0 inches should be significantly severe");
        assertTrue(hailSize.isSevere(), "Significantly severe should also be severe");
    }

    @Test
    @DisplayName("Should identify significantly severe hail (above 2.0 inches)")
    void testIsSignificantlySevereAboveThreshold() {
        HailSize hailSize = new HailSize(2.5);
        assertTrue(hailSize.isSignificantlySevere());
        assertTrue(hailSize.isSevere());
    }

    @Test
    @DisplayName("Should identify non-significantly-severe hail (below 2.0 inches)")
    void testNotSignificantlySevereBelowThreshold() {
        HailSize hailSize = new HailSize(1.5);
        assertFalse(hailSize.isSignificantlySevere());
        assertTrue(hailSize.isSevere(), "Should still be severe");
    }

    @ParameterizedTest
    @CsvSource({
            "0.5, false, false",
            "0.99, false, false",
            "1.0, true, false",
            "1.5, true, false",
            "1.99, true, false",
            "2.0, true, true",
            "3.0, true, true"
    })
    @DisplayName("Should correctly identify severity levels")
    void testSeverityLevels(double inches, boolean severe, boolean significantlySevere) {
        HailSize hailSize = new HailSize(inches);

        assertEquals(severe, hailSize.isSevere(),
                String.format("%.2f inches severe check", inches));
        assertEquals(significantlySevere, hailSize.isSignificantlySevere(),
                String.format("%.2f inches significantly severe check", inches));
    }


    // ==================== Description Tests ====================

    @Test
    @DisplayName("Should generate description with size and category")
    void testGetDescription() {
        HailSize hailSize = new HailSize(1.75);
        String description = hailSize.getDescription();

        assertTrue(description.contains("1.75"));
        assertTrue(description.contains("inches"));
        assertTrue(description.contains("Tennis ball-sized"));  // ← FIXED
    }

    @ParameterizedTest
    @CsvSource({
            "0.5, '0.50 inches (Penny-sized)'",
            "1.0, '1.00 inches (Quarter-sized)'",
            "1.75, '1.75 inches (Tennis ball-sized)'",  // ← FIXED
            "2.5, '2.50 inches (Baseball-sized)'"
    })
    @DisplayName("Should format descriptions correctly")
    void testDescriptionFormats(double inches, String expected) {
        HailSize hailSize = new HailSize(inches);
        assertEquals(expected, hailSize.getDescription());
    }

    @Test
    @DisplayName("Should generate summary without severity for small hail")
    void testGetSummaryNonSevere() {
        HailSize hailSize = new HailSize(0.75);
        String summary = hailSize.getSummary();

        assertTrue(summary.contains("0.75"));
        assertFalse(summary.contains("severe"));
    }

    @Test
    @DisplayName("Should generate summary with severe indicator")
    void testGetSummarySevere() {
        HailSize hailSize = new HailSize(1.25);
        String summary = hailSize.getSummary();

        assertTrue(summary.contains("1.25"));
        assertTrue(summary.contains("(severe)"));
        assertFalse(summary.contains("significantly"));
    }

    @Test
    @DisplayName("Should generate summary with significantly severe indicator")
    void testGetSummarySignificantlySevere() {
        HailSize hailSize = new HailSize(2.5);
        String summary = hailSize.getSummary();

        assertTrue(summary.contains("2.50"));
        assertTrue(summary.contains("(significantly severe)"));
    }


    // ==================== Factory Method Tests ====================

    @Test
    @DisplayName("Should create from inches factory method")
    void testInchesFactory() {
        HailSize hailSize = HailSize.inches(1.75);

        assertEquals(1.75, hailSize.inches(), 0.001);
    }

    @Test
    @DisplayName("Should create from centimeters factory method")
    void testCentimetersFactory() {
        HailSize hailSize = HailSize.centimeters(2.54);

        assertEquals(1.0, hailSize.inches(), 0.01);
    }

    @Test
    @DisplayName("Should create from millimeters factory method")
    void testMillimetersFactory() {
        HailSize hailSize = HailSize.millimeters(25.4);

        assertEquals(1.0, hailSize.inches(), 0.01);
    }

    @Test
    @DisplayName("Should throw exception for invalid inches in factory")
    void testInchesFactoryInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> HailSize.inches(-1.0));
    }

    @Test
    @DisplayName("Should throw exception for invalid centimeters in factory")
    void testCentimetersFactoryInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> HailSize.centimeters(-2.54));
    }

    @Test
    @DisplayName("Should throw exception for invalid millimeters in factory")
    void testMillimetersFactoryInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> HailSize.millimeters(-25.4));
    }


    // ==================== Equality and HashCode Tests ====================

    @Test
    @DisplayName("Should be equal when sizes match")
    void testEquality() {
        HailSize hailSize1 = new HailSize(1.75);
        HailSize hailSize2 = new HailSize(1.75);

        assertEquals(hailSize1, hailSize2);
        assertEquals(hailSize1.hashCode(), hailSize2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when sizes differ")
    void testInequality() {
        HailSize hailSize1 = new HailSize(1.75);
        HailSize hailSize2 = new HailSize(2.00);

        assertNotEquals(hailSize1, hailSize2);
    }

    @Test
    @DisplayName("Should not be equal when sizes differ slightly")
    void testInequalitySlightDifference() {
        HailSize hailSize1 = new HailSize(1.75);
        HailSize hailSize2 = new HailSize(1.76);

        assertNotEquals(hailSize1, hailSize2);
    }


    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle very small hail")
    void testVerySmallHail() {
        HailSize hailSize = new HailSize(0.01);

        assertEquals(0.01, hailSize.inches(), 0.001);
        assertEquals("Pea-sized", hailSize.getSizeCategory());
        assertFalse(hailSize.isSevere());
    }

    @Test
    @DisplayName("Should handle boundary at pea/marble")
    void testPeaMarbleBoundary() {
        HailSize pea = new HailSize(0.24);
        HailSize marble = new HailSize(0.25);

        assertEquals("Pea-sized", pea.getSizeCategory());
        assertEquals("Marble-sized", marble.getSizeCategory());
    }

    @Test
    @DisplayName("Should handle boundary at severe threshold")
    void testSevereThresholdBoundary() {
        HailSize justBelow = new HailSize(0.99);
        HailSize atThreshold = new HailSize(1.00);

        assertFalse(justBelow.isSevere());
        assertTrue(atThreshold.isSevere());
    }

    @Test
    @DisplayName("Should handle boundary at significantly severe threshold")
    void testSignificantlySevereThresholdBoundary() {
        HailSize justBelow = new HailSize(1.99);
        HailSize atThreshold = new HailSize(2.00);

        assertFalse(justBelow.isSignificantlySevere());
        assertTrue(atThreshold.isSignificantlySevere());
    }

    @Test
    @DisplayName("Should handle maximum size")
    void testMaximumSize() {
        HailSize hailSize = new HailSize(10.0);

        assertEquals(10.0, hailSize.inches(), 0.001);
        assertEquals("Grapefruit-sized or larger", hailSize.getSizeCategory());
        assertTrue(hailSize.isSignificantlySevere());
    }

    @Test
    @DisplayName("Should handle grapefruit-sized exactly at boundary")
    void testGrapefruitBoundary() {
        HailSize justBelow = new HailSize(3.99);
        HailSize atBoundary = new HailSize(4.0);

        assertEquals("Softball-sized", justBelow.getSizeCategory());
        assertEquals("Grapefruit-sized or larger", atBoundary.getSizeCategory());
    }
}
