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
package weather.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link InvalidObservationException} (#133).
 *
 * @author bclasky1539
 */
class InvalidObservationExceptionTest {

    @Test
    @DisplayName("Message is reason followed by the rejected value")
    void testMessageWithRejectedValue() {
        InvalidObservationException e =
                new InvalidObservationException("PeakWind.hour", "25", "Hour must be between 0 and 23");

        assertThat(e).hasMessage("Hour must be between 0 and 23: 25");
    }

    @Test
    @DisplayName("Message is the reason alone when there is no single rejected value")
    void testMessageWithoutRejectedValue() {
        InvalidObservationException e =
                new InvalidObservationException("Temperature.dewpoint", null,
                        "Dewpoint cannot be higher than temperature");

        assertThat(e).hasMessage("Dewpoint cannot be higher than temperature");
        assertThat(e.getRejectedValue()).isNull();
    }

    @Test
    @DisplayName("Message matches the pre-#133 IllegalArgumentException format, so existing message assertions still pass")
    void testMessageMatchesLegacyFormat() {
        int hour = 25;
        String legacyMessage = new IllegalArgumentException("Hour must be between 0 and 23: " + hour).getMessage();

        InvalidObservationException e = new InvalidObservationException(
                "WindShift.hour", String.valueOf(hour), "Hour must be between 0 and 23");

        assertThat(e.getMessage()).isEqualTo(legacyMessage);
    }

    @Test
    @DisplayName("Getters return the structured fields")
    void testGetters() {
        InvalidObservationException e =
                new InvalidObservationException("Wind.directionDegrees", "999", "Wind direction must be 0-360 degrees");

        assertThat(e.getComponent()).isEqualTo("Wind.directionDegrees");
        assertThat(e.getRejectedValue()).isEqualTo("999");
        assertThat(e.getReason()).isEqualTo("Wind direction must be 0-360 degrees");
    }

    @Test
    @DisplayName("Cause is kept when provided")
    void testCauseIsKept() {
        NumberFormatException cause = new NumberFormatException("For input string: \"00X5\"");

        InvalidObservationException e = new InvalidObservationException(
                "PrecipitationAmount.encodedValue", "00X5", "Invalid encoded value", cause);

        assertThat(e).hasCause(cause);
        assertThat(e).hasMessage("Invalid encoded value: 00X5");
    }

    @Test
    @DisplayName("Cause is null when not provided")
    void testCauseIsNullByDefault() {
        InvalidObservationException e =
                new InvalidObservationException("CloudType.oktas", "9", "Oktas must be between 1 and 8");

        assertThat(e.getCause()).isNull();
    }

    @Test
    @DisplayName("Is an IllegalArgumentException, so existing catch blocks still catch it during the migration")
    void testIsIllegalArgumentException() {
        assertThatThrownBy(() -> {
            throw new InvalidObservationException("PeakWind.minute", "75", "Minute must be between 0 and 59");
        })
                .isInstanceOf(IllegalArgumentException.class)
                .isExactlyInstanceOf(InvalidObservationException.class);
    }

    @Test
    @DisplayName("Can be caught separately from a plain IllegalArgumentException")
    void testDistinguishableFromPlainIllegalArgumentException() {
        IllegalArgumentException programmingError = new IllegalArgumentException("Wind cannot be null");
        IllegalArgumentException domainRejection =
                new InvalidObservationException("Wind.speed", "-5", "Wind speed cannot be negative");

        assertThat(programmingError).isNotInstanceOf(InvalidObservationException.class);
        assertThat(domainRejection).isInstanceOf(InvalidObservationException.class);
    }

    @Test
    @DisplayName("toString shows component, rejected value and reason")
    void testToString() {
        InvalidObservationException e =
                new InvalidObservationException("PeakWind.hour", "25", "Hour must be between 0 and 23");

        assertThat(e).hasToString(
                "InvalidObservationException{component=PeakWind.hour, rejectedValue=25, "
                        + "reason='Hour must be between 0 and 23'}");
    }
}
