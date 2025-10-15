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

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WindInformation Tests")
class WindInformationTest {

    private WindInformation windInformation;

    @BeforeEach
    void setUp() {
        windInformation = new WindInformation();
    }

    @Test
    @DisplayName("Default constructor creates empty wind information")
    void testDefaultConstructor() {
        assertNull(windInformation.getWindDirectionDegrees());
        assertNull(windInformation.getWindSpeedKnots());
        assertNull(windInformation.getWindGustKnots());
        assertNull(windInformation.getWindVariableDirection());
    }

    @Test
    @DisplayName("Parameterized constructor sets wind values")
    void testParameterizedConstructor() {
        WindInformation wind = new WindInformation(270, 15, 22);
        
        assertEquals(270, wind.getWindDirectionDegrees());
        assertEquals(15, wind.getWindSpeedKnots());
        assertEquals(22, wind.getWindGustKnots());
    }

    @Test
    @DisplayName("Wind direction setter and getter work correctly")
    void testWindDirectionSetterGetter() {
        windInformation.setWindDirectionDegrees(180);
        assertEquals(180, windInformation.getWindDirectionDegrees());
        
        windInformation.setWindDirectionDegrees(null);
        assertNull(windInformation.getWindDirectionDegrees());
    }

    @Test
    @DisplayName("Wind speed setter and getter work correctly")
    void testWindSpeedSetterGetter() {
        windInformation.setWindSpeedKnots(12);
        assertEquals(12, windInformation.getWindSpeedKnots());
        
        windInformation.setWindSpeedKnots(null);
        assertNull(windInformation.getWindSpeedKnots());
    }

    @Test
    @DisplayName("Wind gust setter and getter work correctly")
    void testWindGustSetterGetter() {
        windInformation.setWindGustKnots(25);
        assertEquals(25, windInformation.getWindGustKnots());
        
        windInformation.setWindGustKnots(null);
        assertNull(windInformation.getWindGustKnots());
    }

    @Test
    @DisplayName("Variable direction setter and getter work correctly")
    void testVariableDirectionSetterGetter() {
        windInformation.setWindVariableDirection("240V300");
        assertEquals("240V300", windInformation.getWindVariableDirection());
        
        windInformation.setWindVariableDirection(null);
        assertNull(windInformation.getWindVariableDirection());
    }

    @Test
    @DisplayName("isCalm returns true for null wind speed")
    void testIsCalmWithNullSpeed() {
        assertTrue(windInformation.isCalm());
    }

    @Test
    @DisplayName("isCalm returns true for wind speed less than 3 knots")
    void testIsCalmWithLowSpeed() {
        windInformation.setWindSpeedKnots(2);
        assertTrue(windInformation.isCalm());
    }

    @Test
    @DisplayName("isCalm returns false for wind speed 3 knots or higher")
    void testIsNotCalmWithHighSpeed() {
        windInformation.setWindSpeedKnots(3);
        assertFalse(windInformation.isCalm());
        
        windInformation.setWindSpeedKnots(15);
        assertFalse(windInformation.isCalm());
    }

    @Test
    @DisplayName("hasGusts returns false for null wind gusts")
    void testHasGustsWithNull() {
        assertFalse(windInformation.hasGusts());
    }

    @Test
    @DisplayName("hasGusts returns false for zero wind gusts")
    void testHasGustsWithZero() {
        windInformation.setWindGustKnots(0);
        assertFalse(windInformation.hasGusts());
    }

    @Test
    @DisplayName("hasGusts returns true for positive wind gusts")
    void testHasGustsWithPositiveValue() {
        windInformation.setWindGustKnots(20);
        assertTrue(windInformation.hasGusts());
    }

    @Test
    @DisplayName("getWindDirectionCardinal returns null for null direction")
    void testWindDirectionCardinalWithNull() {
        assertNull(windInformation.getWindDirectionCardinal());
    }

    @Test
    @DisplayName("getWindDirectionCardinal returns correct cardinal directions")
    void testWindDirectionCardinal() {
        // Test all cardinal and intercardinal directions
        windInformation.setWindDirectionDegrees(0);
        assertEquals("N", windInformation.getWindDirectionCardinal());
        
        windInformation.setWindDirectionDegrees(45);
        assertEquals("NE", windInformation.getWindDirectionCardinal());
        
        windInformation.setWindDirectionDegrees(90);
        assertEquals("E", windInformation.getWindDirectionCardinal());
        
        windInformation.setWindDirectionDegrees(135);
        assertEquals("SE", windInformation.getWindDirectionCardinal());
        
        windInformation.setWindDirectionDegrees(180);
        assertEquals("S", windInformation.getWindDirectionCardinal());
        
        windInformation.setWindDirectionDegrees(225);
        assertEquals("SW", windInformation.getWindDirectionCardinal());
        
        windInformation.setWindDirectionDegrees(270);
        assertEquals("W", windInformation.getWindDirectionCardinal());
        
        windInformation.setWindDirectionDegrees(315);
        assertEquals("NW", windInformation.getWindDirectionCardinal());
        
        windInformation.setWindDirectionDegrees(360);
        assertEquals("N", windInformation.getWindDirectionCardinal());
    }

    @Test
    @DisplayName("getWindDirectionCardinal handles edge cases")
    void testWindDirectionCardinalEdgeCases() {
        // Test values that should round to nearest cardinal direction
        windInformation.setWindDirectionDegrees(11);
        assertEquals("N", windInformation.getWindDirectionCardinal());
        
        windInformation.setWindDirectionDegrees(34);
        assertEquals("NE", windInformation.getWindDirectionCardinal());
    }

    @Test
    @DisplayName("toString returns calm message for calm winds")
    void testToStringCalm() {
        String result = windInformation.toString();
        assertEquals("Wind: Calm", result);
    }

    @Test
    @DisplayName("toString returns formatted wind information")
    void testToStringWithWindData() {
        windInformation.setWindDirectionDegrees(270);
        windInformation.setWindSpeedKnots(15);
        
        String result = windInformation.toString();
        assertEquals("Wind: 270° (W) 15 knots", result);
    }

    @Test
    @DisplayName("toString includes gusts when present")
    void testToStringWithGusts() {
        windInformation.setWindDirectionDegrees(180);
        windInformation.setWindSpeedKnots(12);
        windInformation.setWindGustKnots(20);
        
        String result = windInformation.toString();
        assertEquals("Wind: 180° (S) 12 knots gusts 20 knots", result);
    }

    @Test
    @DisplayName("toString includes variable direction when present")
    void testToStringWithVariableDirection() {
        windInformation.setWindDirectionDegrees(270);
        windInformation.setWindSpeedKnots(8);
        windInformation.setWindVariableDirection("240V300");
        
        String result = windInformation.toString();
        assertEquals("Wind: 270° (W) 8 knots variable 240V300", result);
    }

    @Test
    @DisplayName("equals returns true for same object")
    void testEqualsSameObject() {
        assertEquals(windInformation, windInformation);
    }

    @Test
    @DisplayName("equals returns false for null")
    void testEqualsNull() {
        assertNotEquals(windInformation, null);
    }

    @Test
    @DisplayName("equals returns false for different class")
    void testEqualsDifferentClass() {
        assertNotEquals(windInformation, "not a wind object");
    }

    @Test
    @DisplayName("equals returns true for equal wind information")
    void testEqualsEqual() {
        WindInformation wind1 = new WindInformation(270, 15, 22);
        wind1.setWindVariableDirection("240V300");
        
        WindInformation wind2 = new WindInformation(270, 15, 22);
        wind2.setWindVariableDirection("240V300");
        
        assertEquals(wind1, wind2);
        assertEquals(wind1.hashCode(), wind2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different wind information")
    void testEqualsNotEqual() {
        WindInformation wind1 = new WindInformation(270, 15, 22);
        WindInformation wind2 = new WindInformation(180, 15, 22);
        
        assertNotEquals(wind1, wind2);
    }

    @Test
    @DisplayName("hashCode is consistent")
    void testHashCodeConsistency() {
        windInformation.setWindDirectionDegrees(270);
        windInformation.setWindSpeedKnots(15);
        
        int hashCode1 = windInformation.hashCode();
        int hashCode2 = windInformation.hashCode();
        
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("hashCode is equal for equal objects")
    void testHashCodeEquality() {
        WindInformation wind1 = new WindInformation(270, 15, null);
        WindInformation wind2 = new WindInformation(270, 15, null);
        
        assertEquals(wind1.hashCode(), wind2.hashCode());
    }
}
