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
package weather.model.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Comprehensive test suite for ValidityPeriod record.
 * Tests TAF validity period handling.
 *
 * @author bclasky1539
 *
 */
class ValidityPeriodTest {

   // ==================== Construction and Validation Tests ====================

    @Nested
    @DisplayName("Construction and Validation")
    class ConstructionTests {

        @Test
        void testValidConstruction() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = Instant.parse("2024-03-16T20:00:00Z");

            ValidityPeriod period = new ValidityPeriod(start, end);

            assertThat(period.validFrom()).isEqualTo(start);
            assertThat(period.validTo()).isEqualTo(end);
        }

        @ParameterizedTest
        @CsvSource({
                "1, Minimum duration",
                "24, Standard 24-hour TAF",
                "30, Common 30-hour TAF",
                "48, Maximum duration"
        })
        void testValidConstruction_VariousDurations(long hours) {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = start.plus(hours, ChronoUnit.HOURS);

            ValidityPeriod period = new ValidityPeriod(start, end);

            assertThat(period.getDurationHours()).isEqualTo(hours);
        }
    }

    // ==================== Duration Tests ====================

    @Nested
    @DisplayName("Duration Calculations")
    class DurationTests {

        @Test
        void testGetDuration() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = start.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            Duration duration = period.getDuration();

            assertThat(duration).isEqualTo(Duration.ofHours(24));
        }

        @Test
        void testGetDurationHours_24() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = start.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            assertThat(period.getDurationHours()).isEqualTo(24);
        }

        @Test
        void testGetDurationHours_30() {
            Instant start = Instant.parse("2024-03-30T18:00:00Z");
            Instant end = start.plus(30, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            assertThat(period.getDurationHours()).isEqualTo(30);
        }

        @Test
        void testIsStandard24Hour_True() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = start.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            assertThat(period.isStandard24Hour()).isTrue();
        }

        @Test
        void testIsStandard24Hour_False() {
            Instant start = Instant.parse("2024-03-30T18:00:00Z");
            Instant end = start.plus(30, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            assertThat(period.isStandard24Hour()).isFalse();
        }

        @Test
        void testIs30Hour_True() {
            Instant start = Instant.parse("2024-03-30T18:00:00Z");
            Instant end = start.plus(30, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            assertThat(period.is30Hour()).isTrue();
        }

        @Test
        void testIs30Hour_False() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = start.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            assertThat(period.is30Hour()).isFalse();
        }
    }

    // ==================== Contains Tests ====================

    @Nested
    @DisplayName("Contains Time Checks")
    class ContainsTests {

        @Test
        void testContains_TimeInMiddle() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = start.plus(24, ChronoUnit.HOURS);
            Instant middle = start.plus(12, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            assertThat(period.contains(middle)).isTrue();
        }

        @Test
        void testContains_StartTime() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = start.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            // Inclusive of start
            assertThat(period.contains(start)).isTrue();
        }

        @Test
        void testContains_EndTime() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = start.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            // Exclusive of end
            assertThat(period.contains(end)).isFalse();
        }

        @Test
        void testContains_BeforeStart() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = start.plus(24, ChronoUnit.HOURS);
            Instant before = start.minus(1, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            assertThat(period.contains(before)).isFalse();
        }

        @Test
        void testContains_AfterEnd() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = start.plus(24, ChronoUnit.HOURS);
            Instant after = end.plus(1, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            assertThat(period.contains(after)).isFalse();
        }

        @Test
        void testContains_Null() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = start.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            assertThat(period.contains(null)).isFalse();
        }
    }

    // ==================== Time Status Tests ====================

    @Nested
    @DisplayName("Time Status Checks")
    class TimeStatusTests {

        // Note: These tests are time-dependent and may need to be mocked
        // For now, testing the logic with explicit time comparisons

        @Test
        void testIsCurrentlyValid_Logic() {
            Instant now = Instant.now();
            Instant start = now.minus(1, ChronoUnit.HOURS);
            Instant end = now.plus(1, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            // Should be currently valid
            assertThat(period.isCurrentlyValid()).isTrue();
        }

        @Test
        void testHasExpired_Logic() {
            Instant now = Instant.now();
            Instant start = now.minus(25, ChronoUnit.HOURS);
            Instant end = now.minus(1, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            // Should be expired
            assertThat(period.hasExpired()).isTrue();
        }

        @Test
        void testIsFuture_Logic() {
            Instant now = Instant.now();
            Instant start = now.plus(1, ChronoUnit.HOURS);
            Instant end = now.plus(25, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            // Should be in future
            assertThat(period.isFuture()).isTrue();
        }
    }

    // ==================== Overlap Tests ====================

    @Nested
    @DisplayName("Overlap Checks")
    class OverlapTests {

        @Test
        void testOverlaps_CompleteOverlap() {
            Instant start1 = Instant.parse("2024-03-15T20:00:00Z");
            Instant end1 = start1.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period1 = new ValidityPeriod(start1, end1);

            Instant start2 = start1.plus(6, ChronoUnit.HOURS);
            Instant end2 = end1.minus(6, ChronoUnit.HOURS);
            ValidityPeriod period2 = new ValidityPeriod(start2, end2);

            assertThat(period1.overlaps(period2)).isTrue();
            assertThat(period2.overlaps(period1)).isTrue();
        }

        @Test
        void testOverlaps_PartialOverlap() {
            Instant start1 = Instant.parse("2024-03-15T20:00:00Z");
            Instant end1 = start1.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period1 = new ValidityPeriod(start1, end1);

            Instant start2 = start1.plus(12, ChronoUnit.HOURS);
            Instant end2 = end1.plus(12, ChronoUnit.HOURS);
            ValidityPeriod period2 = new ValidityPeriod(start2, end2);

            assertThat(period1.overlaps(period2)).isTrue();
            assertThat(period2.overlaps(period1)).isTrue();
        }

        @Test
        void testOverlaps_NoOverlap() {
            Instant start1 = Instant.parse("2024-03-15T20:00:00Z");
            Instant end1 = start1.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period1 = new ValidityPeriod(start1, end1);

            Instant start2 = end1.plus(1, ChronoUnit.HOURS);
            Instant end2 = start2.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period2 = new ValidityPeriod(start2, end2);

            assertThat(period1.overlaps(period2)).isFalse();
            assertThat(period2.overlaps(period1)).isFalse();
        }

        @Test
        void testOverlaps_AdjacentPeriods() {
            Instant start1 = Instant.parse("2024-03-15T20:00:00Z");
            Instant end1 = start1.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period1 = new ValidityPeriod(start1, end1);

            // Starts exactly when period1 ends
            Instant end2 = end1.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period2 = new ValidityPeriod(end1, end2);

            // Implementation treats adjacent periods (sharing an endpoint) as overlapping
            // This is because the comparison uses <= and >= (inclusive)
            // For aviation: TAF 1520/1620 followed by 1620/1720 share the endpoint 1620
            assertThat(period1.overlaps(period2)).isTrue();
            assertThat(period2.overlaps(period1)).isTrue();
        }

        @Test
        void testOverlaps_Null() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = start.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            assertThat(period.overlaps(null)).isFalse();
        }
    }

    // ==================== Expiration Time Tests ====================

    @Nested
    @DisplayName("Expiration Time Calculations")
    class ExpirationTimeTests {

        @Test
        void testGetTimeUntilExpiration_NotExpired() {
            Instant now = Instant.now();
            Instant start = now.minus(1, ChronoUnit.HOURS);
            Instant end = now.plus(5, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            Duration timeUntil = period.getTimeUntilExpiration();

            assertThat(timeUntil).isNotNull();
            assertThat(timeUntil.toHours()).isCloseTo(5, within(1L));
        }

        @Test
        void testGetTimeUntilExpiration_Expired() {
            Instant now = Instant.now();
            Instant start = now.minus(25, ChronoUnit.HOURS);
            Instant end = now.minus(1, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            Duration timeUntil = period.getTimeUntilExpiration();

            assertThat(timeUntil).isNull();
        }

        @Test
        void testGetTimeSinceExpiration_Expired() {
            Instant now = Instant.now();
            Instant start = now.minus(25, ChronoUnit.HOURS);
            Instant end = now.minus(3, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            Duration timeSince = period.getTimeSinceExpiration();

            assertThat(timeSince).isNotNull();
            assertThat(timeSince.toHours()).isCloseTo(3, within(1L));
        }

        @Test
        void testGetTimeSinceExpiration_NotExpired() {
            Instant now = Instant.now();
            Instant start = now.minus(1, ChronoUnit.HOURS);
            Instant end = now.plus(5, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            Duration timeSince = period.getTimeSinceExpiration();

            assertThat(timeSince).isNull();
        }
    }

    // ==================== Format Tests ====================

    @Nested
    @DisplayName("Formatting Tests")
    class FormatTests {

        @Test
        void testToTafFormat() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = Instant.parse("2024-03-16T20:00:00Z");
            ValidityPeriod period = new ValidityPeriod(start, end);

            String formatted = period.toTafFormat();

            assertThat(formatted).isEqualTo("1520/1620");
        }

        @Test
        void testToTafFormat_CrossMonth() {
            Instant start = Instant.parse("2024-03-30T18:00:00Z");
            Instant end = Instant.parse("2024-03-31T18:00:00Z");
            ValidityPeriod period = new ValidityPeriod(start, end);

            String formatted = period.toTafFormat();

            assertThat(formatted).isEqualTo("3018/3118");
        }

        @Test
        void testToFullFormat() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = Instant.parse("2024-03-16T20:00:00Z");
            ValidityPeriod period = new ValidityPeriod(start, end);

            String formatted = period.toFullFormat();

            assertThat(formatted)
                    .contains("2024-03-15 20:00Z")
                    .contains("2024-03-16 20:00Z")
                    .contains(" to ");
        }

        @Test
        void testGetSummary() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = start.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            String summary = period.getSummary();

            assertThat(summary)
                    .contains("1520/1620")
                    .contains("24 hours");
        }

        @Test
        void testToString() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = start.plus(24, ChronoUnit.HOURS);
            ValidityPeriod period = new ValidityPeriod(start, end);

            String str = period.toString();

            assertThat(str)
                    .contains("ValidityPeriod")
                    .contains("1520/1620")
                    .contains("24 hours");
        }
    }

    // ==================== Factory Method Tests ====================

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        void testOf() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = start.plus(24, ChronoUnit.HOURS);

            ValidityPeriod period = ValidityPeriod.of(start, end);

            assertThat(period.validFrom()).isEqualTo(start);
            assertThat(period.validTo()).isEqualTo(end);
        }

        @Test
        void testFromDuration() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Duration duration = Duration.ofHours(24);

            ValidityPeriod period = ValidityPeriod.fromDuration(start, duration);

            assertThat(period.validFrom()).isEqualTo(start);
            assertThat(period.getDurationHours()).isEqualTo(24);
        }

        @Test
        @SuppressWarnings("ConstantConditions") // Intentionally testing null validation
        void testFromDuration_NullStart() {
            Duration duration = Duration.ofHours(24);

            assertThatThrownBy(() -> ValidityPeriod.fromDuration(null, duration))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start time cannot be null");
        }

        @Test
        @SuppressWarnings("ConstantConditions") // Intentionally testing null validation
        void testFromDuration_NullDuration() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");

            assertThatThrownBy(() -> ValidityPeriod.fromDuration(start, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Duration cannot be null");
        }

        @Test
        void testStandard24Hour() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");

            ValidityPeriod period = ValidityPeriod.standard24Hour(start);

            assertThat(period.validFrom()).isEqualTo(start);
            assertThat(period.getDurationHours()).isEqualTo(24);
            assertThat(period.isStandard24Hour()).isTrue();
        }

        @Test
        void testThirtyHour() {
            Instant start = Instant.parse("2024-03-30T18:00:00Z");

            ValidityPeriod period = ValidityPeriod.thirtyHour(start);

            assertThat(period.validFrom()).isEqualTo(start);
            assertThat(period.getDurationHours()).isEqualTo(30);
            assertThat(period.is30Hour()).isTrue();
        }
    }

    // ==================== Equality and HashCode Tests ====================

    @Test
    void testEquality() {
        Instant start = Instant.parse("2024-03-15T20:00:00Z");
        Instant end = start.plus(24, ChronoUnit.HOURS);

        ValidityPeriod period1 = new ValidityPeriod(start, end);
        ValidityPeriod period2 = new ValidityPeriod(start, end);
        ValidityPeriod period3 = new ValidityPeriod(start, end.plus(1, ChronoUnit.HOURS));

        assertThat(period1)
                .isEqualTo(period2)
                .isNotEqualTo(period3);
    }

    @Test
    void testHashCode() {
        Instant start = Instant.parse("2024-03-15T20:00:00Z");
        Instant end = start.plus(24, ChronoUnit.HOURS);

        ValidityPeriod period1 = new ValidityPeriod(start, end);
        ValidityPeriod period2 = new ValidityPeriod(start, end);

        assertThat(period1.hashCode()).hasSameHashCodeAs(period2);
    }
}
