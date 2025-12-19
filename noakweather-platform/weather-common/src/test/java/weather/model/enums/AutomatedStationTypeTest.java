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
        assertEquals(2, types.length, "Should have exactly 2 types");
        assertEquals(AutomatedStationType.AO1, types[0]);
        assertEquals(AutomatedStationType.AO2, types[1]);
    }

    @Test
    void testGetCode() {
        assertEquals("AO1", AutomatedStationType.AO1.getCode());
        assertEquals("AO2", AutomatedStationType.AO2.getCode());
    }

    @Test
    void testGetDescription() {
        assertTrue(AutomatedStationType.AO1.getDescription().contains("without"));
        assertTrue(AutomatedStationType.AO2.getDescription().contains("with"));
    }

    @Test
    void testHasPrecipitationDiscriminator() {
        assertFalse(AutomatedStationType.AO1.hasPrecipitationDiscriminator(),
                "AO1 should NOT have precipitation discriminator");
        assertTrue(AutomatedStationType.AO2.hasPrecipitationDiscriminator(),
                "AO2 should have precipitation discriminator");
    }

    @Test
    void testToString() {
        assertEquals("AO1", AutomatedStationType.AO1.toString());
        assertEquals("AO2", AutomatedStationType.AO2.toString());
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
            "AO1, AO1",
            "AO2, AO2",
            "A01, AO1",
            "A02, AO2",
            "ao1, AO1",
            "ao2, AO2",
            "a01, AO1",
            "a02, AO2",
            " AO1 , AO1",
            " AO2 , AO2"
    })
    void testFromCode(String input, AutomatedStationType expected) {
        assertEquals(expected, AutomatedStationType.fromCode(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AO3", "AO0", "A03", "A00", "XO1", "BO2", "AO", "123"})
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
    }

    @Test
    void testOcrErrorHandling() {
        AutomatedStationType fromO = AutomatedStationType.fromCode("AO1");
        AutomatedStationType from0 = AutomatedStationType.fromCode("A01");
        assertSame(fromO, from0, "AO1 and A01 should resolve to same enum value");

        AutomatedStationType fromO2 = AutomatedStationType.fromCode("AO2");
        AutomatedStationType from02 = AutomatedStationType.fromCode("A02");
        assertSame(fromO2, from02, "AO2 and A02 should resolve to same enum value");
    }
}
