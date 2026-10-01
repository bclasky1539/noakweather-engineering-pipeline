/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025-2026 bclasky1539
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
package weather.model.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AutomatedStationType.
 *
 * @author bclasky1539
 *
 */
class AutomatedStationTypeTest {

    @Test
    void testEnumValues() {
        AutomatedStationType[] types = AutomatedStationType.values();
        assertEquals(4, types.length, "Should have exactly 4 types");
        assertEquals(AutomatedStationType.AO1, types[0]);
        assertEquals(AutomatedStationType.AO2, types[1]);
        assertEquals(AutomatedStationType.AO1A, types[2]);
        assertEquals(AutomatedStationType.AO2A, types[3]);
    }

    @Test
    void testGetCode() {
        assertEquals("AO1", AutomatedStationType.AO1.getCode());
        assertEquals("AO2", AutomatedStationType.AO2.getCode());
        assertEquals("AO1A", AutomatedStationType.AO1A.getCode());
        assertEquals("AO2A", AutomatedStationType.AO2A.getCode());
    }

    @Test
    void testGetDescription() {
        assertTrue(AutomatedStationType.AO1.getDescription().contains("without"));
        assertTrue(AutomatedStationType.AO2.getDescription().contains("with"));
        assertTrue(AutomatedStationType.AO1A.getDescription().contains("without"));
        assertTrue(AutomatedStationType.AO1A.getDescription().contains("augmented"));
        assertTrue(AutomatedStationType.AO2A.getDescription().contains("with"));
        assertTrue(AutomatedStationType.AO2A.getDescription().contains("augmented"));
    }

    @Test
    void testHasPrecipitationDiscriminator() {
        assertFalse(AutomatedStationType.AO1.hasPrecipitationDiscriminator(),
                "AO1 should NOT have precipitation discriminator");
        assertTrue(AutomatedStationType.AO2.hasPrecipitationDiscriminator(),
                "AO2 should have precipitation discriminator");
        assertFalse(AutomatedStationType.AO1A.hasPrecipitationDiscriminator(),
                "AO1A should NOT have precipitation discriminator");
        assertTrue(AutomatedStationType.AO2A.hasPrecipitationDiscriminator(),
                "AO2A should have precipitation discriminator");
    }

    @Test
    void testHasManualAugmentation() {
        assertFalse(AutomatedStationType.AO1.hasManualAugmentation(),
                "AO1 should NOT be manually augmented");
        assertFalse(AutomatedStationType.AO2.hasManualAugmentation(),
                "AO2 should NOT be manually augmented");
        assertTrue(AutomatedStationType.AO1A.hasManualAugmentation(),
                "AO1A should be manually augmented");
        assertTrue(AutomatedStationType.AO2A.hasManualAugmentation(),
                "AO2A should be manually augmented");
    }

    @Test
    void testToString() {
        assertEquals("AO1", AutomatedStationType.AO1.toString());
        assertEquals("AO2", AutomatedStationType.AO2.toString());
        assertEquals("AO1A", AutomatedStationType.AO1A.toString());
        assertEquals("AO2A", AutomatedStationType.AO2A.toString());
    }

    @Test
    void testFromDigitInt() {
        assertEquals(AutomatedStationType.AO1, AutomatedStationType.fromDigit(1));
        assertEquals(AutomatedStationType.AO2, AutomatedStationType.fromDigit(2));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 4, 5, -1, 10, 99})
    void testFromDigitIntInvalid(int digit) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AutomatedStationType.fromDigit(digit)
        );
        assertTrue(exception.getMessage().contains("Invalid automated station type"));
        assertTrue(exception.getMessage().contains(String.valueOf(digit)));
    }

    @ParameterizedTest
    @CsvSource({
            "1, AO1",
            "2, AO2",
            " 1 , AO1",
            " 2 , AO2"
    })
    void testFromDigitString(String input, AutomatedStationType expected) {
        assertEquals(expected, AutomatedStationType.fromDigit(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "3", "9", "-1", "10"})
    void testFromDigitStringInvalid(String digit) {
        assertThrows(IllegalArgumentException.class,
                () -> AutomatedStationType.fromDigit(digit));
    }

    @Test
    void testFromDigitStringNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AutomatedStationType.fromDigit((String) null)
        );
        assertTrue(exception.getMessage().contains("cannot be null"));
    }

    @Test
    void testFromDigitStringBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AutomatedStationType.fromDigit("   ")
        );
        assertTrue(exception.getMessage().contains("cannot be null or blank"));
    }

    @Test
    void testFromDigitStringNotANumber() {
        assertThrows(NumberFormatException.class,
                () -> AutomatedStationType.fromDigit("ABC"));
    }

    @ParameterizedTest
    @CsvSource({
            "1, false, AO1",
            "2, false, AO2",
            "1, true, AO1A",
            "2, true, AO2A"
    })
    void testFromDigitAndAugmentation(int digit, boolean augmented, AutomatedStationType expected) {
        assertEquals(expected, AutomatedStationType.fromDigitAndAugmentation(digit, augmented));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 4, 5, -1, 10, 99})
    void testFromDigitAndAugmentationInvalidDigit(int digit) {
        assertThrows(IllegalArgumentException.class,
                () -> AutomatedStationType.fromDigitAndAugmentation(digit, false));
        assertThrows(IllegalArgumentException.class,
                () -> AutomatedStationType.fromDigitAndAugmentation(digit, true));
    }

    @ParameterizedTest
    @CsvSource({
            "AO1, AO1",
            "AO2, AO2",
            "AO1A, AO1A",
            "AO2A, AO2A",
            "A01, AO1",
            "A02, AO2",
            "A01A, AO1A",
            "A02A, AO2A",
            "ao1, AO1",
            "ao2, AO2",
            "ao1a, AO1A",
            "ao2a, AO2A",
            "a01, AO1",
            "a02, AO2",
            "a01a, AO1A",
            "a02a, AO2A",
            " AO1 , AO1",
            " AO2 , AO2",
            " AO1A , AO1A",
            " AO2A , AO2A"
    })
    void testFromCode(String input, AutomatedStationType expected) {
        assertEquals(expected, AutomatedStationType.fromCode(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AO3", "AO0", "A03", "A00", "XO1", "BO2", "AO", "123", "AO1B", "AO2X", "AO1AA"})
    void testFromCodeInvalid(String code) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AutomatedStationType.fromCode(code)
        );
        assertTrue(exception.getMessage().contains("Invalid automated station type code"));
        assertTrue(exception.getMessage().contains(code));
    }

    @Test
    void testFromCodeNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AutomatedStationType.fromCode(null)
        );
        assertTrue(exception.getMessage().contains("cannot be null"));
    }

    @Test
    void testFromCodeBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AutomatedStationType.fromCode("   ")
        );
        assertTrue(exception.getMessage().contains("cannot be null or blank"));
    }

    @Test
    void testRoundTripFromDigitToCode() {
        AutomatedStationType ao1 = AutomatedStationType.fromDigit(1);
        assertEquals("AO1", ao1.getCode());

        AutomatedStationType ao2 = AutomatedStationType.fromDigit(2);
        assertEquals("AO2", ao2.getCode());
    }

    @Test
    void testRoundTripFromCodeToDiscriminator() {
        AutomatedStationType ao1 = AutomatedStationType.fromCode("AO1");
        assertFalse(ao1.hasPrecipitationDiscriminator());

        AutomatedStationType ao2 = AutomatedStationType.fromCode("AO2");
        assertTrue(ao2.hasPrecipitationDiscriminator());

        AutomatedStationType ao1a = AutomatedStationType.fromCode("AO1A");
        assertFalse(ao1a.hasPrecipitationDiscriminator());
        assertTrue(ao1a.hasManualAugmentation());

        AutomatedStationType ao2a = AutomatedStationType.fromCode("AO2A");
        assertTrue(ao2a.hasPrecipitationDiscriminator());
        assertTrue(ao2a.hasManualAugmentation());
    }

    @Test
    void testOcrErrorHandling() {
        AutomatedStationType fromO = AutomatedStationType.fromCode("AO1");
        AutomatedStationType from0 = AutomatedStationType.fromCode("A01");
        assertSame(fromO, from0, "AO1 and A01 should resolve to same enum value");

        AutomatedStationType fromO2 = AutomatedStationType.fromCode("AO2");
        AutomatedStationType from02 = AutomatedStationType.fromCode("A02");
        assertSame(fromO2, from02, "AO2 and A02 should resolve to same enum value");

        AutomatedStationType fromO1A = AutomatedStationType.fromCode("AO1A");
        AutomatedStationType from01A = AutomatedStationType.fromCode("A01A");
        assertSame(fromO1A, from01A, "AO1A and A01A should resolve to same enum value");

        AutomatedStationType fromO2A = AutomatedStationType.fromCode("AO2A");
        AutomatedStationType from02A = AutomatedStationType.fromCode("A02A");
        assertSame(fromO2A, from02A, "AO2A and A02A should resolve to same enum value");
    }
}
