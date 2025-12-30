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
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AutomatedMaintenanceIndicator record.
 *
 * @author bclasky1539
 *
 */
class AutomatedMaintenanceIndicatorTest {

    @Test
    @DisplayName("Should create indicator with type only")
    void testCreateWithTypeOnly() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("RVRNO");

        assertEquals("RVRNO", indicator.type());
        assertNull(indicator.location());
        assertFalse(indicator.hasLocation());
    }

    @Test
    @DisplayName("Should create indicator with type and location")
    void testCreateWithTypeAndLocation() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("VISNO", "RWY06");

        assertEquals("VISNO", indicator.type());
        assertEquals("RWY06", indicator.location());
        assertTrue(indicator.hasLocation());
    }

    @Test
    @DisplayName("Should create maintenance check indicator")
    void testCreateMaintenanceCheck() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.maintenanceCheck();

        assertEquals("$", indicator.type());
        assertNull(indicator.location());
        assertTrue(indicator.isMaintenanceCheck());
    }

    @Test
    @DisplayName("Should normalize type to uppercase")
    void testNormalizeType() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("tsno");

        assertEquals("TSNO", indicator.type());
        assertTrue(indicator.isThunderstormNotAvailable());
    }

    @Test
    @DisplayName("Should normalize location to uppercase")
    void testNormalizeLocation() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("CHINO", "rwy24");

        assertEquals("RWY24", indicator.location());
    }

    @Test
    @DisplayName("Should throw exception for null type")
    void testNullType() {
        assertThrows(IllegalArgumentException.class, () ->
                AutomatedMaintenanceIndicator.of(null));
    }

    @Test
    @DisplayName("Should throw exception for blank type")
    void testBlankType() {
        assertThrows(IllegalArgumentException.class, () ->
                AutomatedMaintenanceIndicator.of("   "));
    }

    @Test
    @DisplayName("Should throw exception for invalid type")
    void testInvalidType() {
        assertThrows(IllegalArgumentException.class, () ->
                AutomatedMaintenanceIndicator.of("INVALID"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RVRNO", "PWINO", "PNO", "FZRANO", "TSNO", "VISNO", "CHINO", "$"})
    @DisplayName("Should accept all valid types")
    void testAllValidTypes(String type) {
        assertDoesNotThrow(() -> AutomatedMaintenanceIndicator.of(type));
    }

    @Test
    @DisplayName("Should correctly identify RVRNO")
    void testIsRVRNotAvailable() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("RVRNO");

        assertTrue(indicator.isRVRNotAvailable());
        assertFalse(indicator.isPresentWeatherNotAvailable());
        assertFalse(indicator.isMaintenanceCheck());
    }

    @Test
    @DisplayName("Should correctly identify PWINO")
    void testIsPresentWeatherNotAvailable() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("PWINO");

        assertTrue(indicator.isPresentWeatherNotAvailable());
        assertFalse(indicator.isRVRNotAvailable());
    }

    @Test
    @DisplayName("Should correctly identify PNO")
    void testIsPrecipitationNotAvailable() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("PNO");

        assertTrue(indicator.isPrecipitationNotAvailable());
        assertFalse(indicator.isRVRNotAvailable());
    }

    @Test
    @DisplayName("Should correctly identify FZRANO")
    void testIsFreezingRainNotAvailable() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("FZRANO");

        assertTrue(indicator.isFreezingRainNotAvailable());
        assertFalse(indicator.isRVRNotAvailable());
    }

    @Test
    @DisplayName("Should correctly identify TSNO")
    void testIsThunderstormNotAvailable() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("TSNO");

        assertTrue(indicator.isThunderstormNotAvailable());
        assertFalse(indicator.isRVRNotAvailable());
    }

    @Test
    @DisplayName("Should correctly identify VISNO")
    void testIsVisibilityNotAvailable() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("VISNO");

        assertTrue(indicator.isVisibilityNotAvailable());
        assertFalse(indicator.isRVRNotAvailable());
    }

    @Test
    @DisplayName("Should correctly identify CHINO")
    void testIsCloudHeightNotAvailable() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("CHINO");

        assertTrue(indicator.isCloudHeightNotAvailable());
        assertFalse(indicator.isRVRNotAvailable());
    }

    @Test
    @DisplayName("Should correctly identify maintenance check ($)")
    void testIsMaintenanceCheckIndicator() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("$");

        assertTrue(indicator.isMaintenanceCheck());
        assertFalse(indicator.isRVRNotAvailable());
    }

    @Test
    @DisplayName("Should generate correct description without location")
    void testGetDescriptionWithoutLocation() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("RVRNO");

        assertEquals("Runway Visual Range not available", indicator.getDescription());
    }

    @Test
    @DisplayName("Should generate correct description with location")
    void testGetDescriptionWithLocation() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("VISNO", "RWY06");

        assertEquals("Visibility not available at RWY06", indicator.getDescription());
    }

    @Test
    @DisplayName("Should generate correct description for maintenance check")
    void testGetDescriptionForMaintenanceCheck() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.maintenanceCheck();

        assertEquals("Station requires maintenance", indicator.getDescription());
    }

    @Test
    @DisplayName("Should generate correct toString without location")
    void testToStringWithoutLocation() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("TSNO");

        assertEquals("TSNO", indicator.toString());
    }

    @Test
    @DisplayName("Should generate correct toString with location")
    void testToStringWithLocation() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("CHINO", "RWY24");

        assertEquals("CHINO RWY24", indicator.toString());
    }

    @Test
    @DisplayName("Should handle null location as no location")
    void testNullLocation() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("PWINO", null);

        assertNull(indicator.location());
        assertFalse(indicator.hasLocation());
    }

    @Test
    @DisplayName("Should handle blank location as no location")
    void testBlankLocation() {
        AutomatedMaintenanceIndicator indicator = AutomatedMaintenanceIndicator.of("PNO", "   ");

        assertNull(indicator.location());
        assertFalse(indicator.hasLocation());
    }

    @Test
    @DisplayName("Should be equal when type and location match")
    void testEquality() {
        AutomatedMaintenanceIndicator indicator1 = AutomatedMaintenanceIndicator.of("VISNO", "RWY06");
        AutomatedMaintenanceIndicator indicator2 = AutomatedMaintenanceIndicator.of("VISNO", "RWY06");

        assertEquals(indicator1, indicator2);
        assertEquals(indicator1.hashCode(), indicator2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when types differ")
    void testInequalityDifferentTypes() {
        AutomatedMaintenanceIndicator indicator1 = AutomatedMaintenanceIndicator.of("VISNO", "RWY06");
        AutomatedMaintenanceIndicator indicator2 = AutomatedMaintenanceIndicator.of("CHINO", "RWY06");

        assertNotEquals(indicator1, indicator2);
    }

    @Test
    @DisplayName("Should not be equal when locations differ")
    void testInequalityDifferentLocations() {
        AutomatedMaintenanceIndicator indicator1 = AutomatedMaintenanceIndicator.of("VISNO", "RWY06");
        AutomatedMaintenanceIndicator indicator2 = AutomatedMaintenanceIndicator.of("VISNO", "RWY24");

        assertNotEquals(indicator1, indicator2);
    }
}
