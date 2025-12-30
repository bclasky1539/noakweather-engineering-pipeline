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

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for WeatherEvent record.
 *
 * Tests validation, time conversion, formatting, and real-world examples.
 *
 * @author bclasky1539
 */
@DisplayName("WeatherEvent Tests")
class WeatherEventTest {

    // ==================== Valid Construction Tests ====================

    @Test
    @DisplayName("Should create weather event with begin time only (minutes)")
    void shouldCreateWithBeginMinutesOnly() {
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        assertThat(event.weatherCode()).isEqualTo("RA");
        assertThat(event.intensity()).isNull();
        assertThat(event.beginHour()).isNull();
        assertThat(event.beginMinute()).isEqualTo(5);
        assertThat(event.endHour()).isNull();
        assertThat(event.endMinute()).isNull();
    }

    @Test
    @DisplayName("Should create weather event with full begin timestamp")
    void shouldCreateWithFullBeginTimestamp() {
        WeatherEvent event = new WeatherEvent("FZRA", null, 11, 59, null, null);

        assertThat(event.weatherCode()).isEqualTo("FZRA");
        assertThat(event.beginHour()).isEqualTo(11);
        assertThat(event.beginMinute()).isEqualTo(59);
        assertThat(event.getBeginTime()).isEqualTo(LocalTime.of(11, 59));
    }

    @Test
    @DisplayName("Should create weather event with both begin and end times")
    void shouldCreateWithBothTimes() {
        WeatherEvent event = new WeatherEvent("RA", null, null, 15, null, 30);

        assertThat(event.beginMinute()).isEqualTo(15);
        assertThat(event.endMinute()).isEqualTo(30);
        assertThat(event.hasBeginTime()).isTrue();
        assertThat(event.hasEndTime()).isTrue();
    }

    @Test
    @DisplayName("Should create weather event with light intensity")
    void shouldCreateWithLightIntensity() {
        WeatherEvent event = new WeatherEvent("RA", "-", null, 5, null, null);

        assertThat(event.intensity()).isEqualTo("-");
        assertThat(event.getFullWeatherCode()).isEqualTo("-RA");
    }

    @Test
    @DisplayName("Should create weather event with heavy intensity")
    void shouldCreateWithHeavyIntensity() {
        WeatherEvent event = new WeatherEvent("TSRA", "+", null, 20, null, 45);

        assertThat(event.intensity()).isEqualTo("+");
        assertThat(event.getFullWeatherCode()).isEqualTo("+TSRA");
    }

    // ==================== Validation Tests ====================

    @Test
    @DisplayName("Should throw exception when weather code is null")
    void shouldThrowWhenWeatherCodeIsNull() {
        assertThatThrownBy(() -> new WeatherEvent(null, null, null, 5, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Weather code cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when weather code is blank")
    void shouldThrowWhenWeatherCodeIsBlank() {
        assertThatThrownBy(() -> new WeatherEvent("", null, null, 5, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Weather code cannot be blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "M", "x", "++"})
    @DisplayName("Should throw exception for invalid intensity")
    void shouldThrowForInvalidIntensity(String invalidIntensity) {
        assertThatThrownBy(() -> new WeatherEvent("RA", invalidIntensity, null, 5, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Intensity must be");
    }

    @Test
    @DisplayName("Should throw exception when no begin or end time provided")
    void shouldThrowWhenNoTimeProvided() {
        assertThatThrownBy(() -> new WeatherEvent("RA", null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must have at least a begin or end time");
    }

    @ParameterizedTest
    @CsvSource({"-1", "24", "25"})
    @DisplayName("Should throw exception for invalid begin hour")
    void shouldThrowForInvalidBeginHour(int invalidHour) {
        assertThatThrownBy(() -> new WeatherEvent("RA", null, invalidHour, 30, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Begin hour must be between 0 and 23");
    }

    @ParameterizedTest
    @CsvSource({"-1", "60", "99"})
    @DisplayName("Should throw exception for invalid begin minute")
    void shouldThrowForInvalidBeginMinute(int invalidMinute) {
        assertThatThrownBy(() -> new WeatherEvent("RA", null, null, invalidMinute, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Begin minute must be between 0 and 59");
    }

    // ==================== Time Conversion Tests ====================

    @Test
    @DisplayName("Should convert begin time to LocalTime when hour and minute present")
    void shouldConvertBeginTimeToLocalTime() {
        WeatherEvent event = new WeatherEvent("RA", null, 11, 59, null, null);

        assertThat(event.getBeginTime())
                .isNotNull()
                .isEqualTo(LocalTime.of(11, 59));
    }

    @Test
    @DisplayName("Should return null begin time when only minute present")
    void shouldReturnNullBeginTimeWhenOnlyMinute() {
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        assertThat(event.getBeginTime()).isNull();
        assertThat(event.hasBeginTime()).isTrue();
        assertThat(event.hasBeginHour()).isFalse();
    }

    @Test
    @DisplayName("Should convert end time to LocalTime when hour and minute present")
    void shouldConvertEndTimeToLocalTime() {
        WeatherEvent event = new WeatherEvent("RA", null, null, null, 12, 40);

        assertThat(event.getEndTime())
                .isNotNull()
                .isEqualTo(LocalTime.of(12, 40));
    }

    // ==================== Formatting Tests ====================

    @Test
    @DisplayName("Should format begin time with hours and minutes")
    void shouldFormatBeginTimeWithHoursAndMinutes() {
        WeatherEvent event = new WeatherEvent("RA", null, 11, 59, null, null);
        assertThat(event.getFormattedBeginTime()).isEqualTo("11:59");
    }

    @Test
    @DisplayName("Should format begin time with minutes only")
    void shouldFormatBeginTimeWithMinutesOnly() {
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);
        assertThat(event.getFormattedBeginTime()).isEqualTo(":05");
    }

    @Test
    @DisplayName("Should format end time as N/A when absent")
    void shouldFormatEndTimeAsNAWhenAbsent() {
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);
        assertThat(event.getFormattedEndTime()).isEqualTo("N/A");
    }

    @Test
    @DisplayName("Should get summary with begin and end times")
    void shouldGetSummaryWithBothTimes() {
        WeatherEvent event = new WeatherEvent("RA", null, null, 15, null, 30);
        String summary = event.getSummary();

        assertThat(summary)
                .contains("RA")
                .contains("began :15")
                .contains("ended :30");
    }

    // ==================== Real-world METAR Examples ====================

    @Test
    @DisplayName("Should handle RAB05 (rain began at :05)")
    void shouldHandleRAB05() {
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        assertThat(event.weatherCode()).isEqualTo("RA");
        assertThat(event.hasBeginTime()).isTrue();
        assertThat(event.hasEndTime()).isFalse();
        assertThat(event.getFormattedBeginTime()).isEqualTo(":05");
    }

    @Test
    @DisplayName("Should handle FZRAB1159E1240 (freezing rain 11:59-12:40)")
    void shouldHandleFZRAB1159E1240() {
        WeatherEvent event = new WeatherEvent("FZRA", null, 11, 59, 12, 40);

        assertThat(event.weatherCode()).isEqualTo("FZRA");
        assertThat(event.getBeginTime()).isEqualTo(LocalTime.of(11, 59));
        assertThat(event.getEndTime()).isEqualTo(LocalTime.of(12, 40));
    }

    @Test
    @DisplayName("Should handle TSB0159E0240 (thunderstorm 01:59-02:40)")
    void shouldHandleTSB0159E0240() {
        WeatherEvent event = new WeatherEvent("TS", null, 1, 59, 2, 40);

        assertThat(event.weatherCode()).isEqualTo("TS");
        assertThat(event.getBeginTime()).isEqualTo(LocalTime.of(1, 59));
        assertThat(event.getEndTime()).isEqualTo(LocalTime.of(2, 40));
    }

    @Test
    @DisplayName("Should handle -RAB05 (light rain began at :05)")
    void shouldHandleLightRAB05() {
        WeatherEvent event = new WeatherEvent("RA", "-", null, 5, null, null);

        assertThat(event.weatherCode()).isEqualTo("RA");
        assertThat(event.intensity()).isEqualTo("-");
        assertThat(event.getFullWeatherCode()).isEqualTo("-RA");
    }

    @Test
    @DisplayName("Should handle +TSRAB20E45 (heavy thunderstorm with rain)")
    void shouldHandleHeavyTSRAB20E45() {
        WeatherEvent event = new WeatherEvent("TSRA", "+", null, 20, null, 45);

        assertThat(event.weatherCode()).isEqualTo("TSRA");
        assertThat(event.getFullWeatherCode()).isEqualTo("+TSRA");
        assertThat(event.getFormattedBeginTime()).isEqualTo(":20");
        assertThat(event.getFormattedEndTime()).isEqualTo(":45");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle midnight hour (00)")
    void shouldHandleMidnightHour() {
        WeatherEvent event = new WeatherEvent("RA", null, 0, 5, null, null);

        assertThat(event.beginHour()).isZero();
        assertThat(event.getBeginTime()).isEqualTo(LocalTime.of(0, 5));
    }

    @Test
    @DisplayName("Should handle 23:59 (last minute of day)")
    void shouldHandle2359() {
        WeatherEvent event = new WeatherEvent("RA", null, 23, 59, null, null);

        assertThat(event.beginHour()).isEqualTo(23);
        assertThat(event.getBeginTime()).isEqualTo(LocalTime.of(23, 59));
    }

    // ========== toString() Coverage Tests ==========

    @Test
    @DisplayName("toString with only begin time (no end)")
    void testToStringWithOnlyBeginTime() {
        // Tests the branch: hasBeginTime() = true, hasEndTime() = false
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        String result = event.toString();

        assertThat(result)
                .contains("WeatherEvent")
                .contains("code='RA'")
                .contains("began=:05")
                .doesNotContain("ended=");
    }

    @Test
    @DisplayName("toString with only end time (no begin)")
    void testToStringWithOnlyEndTime() {
        // Tests the branch: hasBeginTime() = false, hasEndTime() = true
        WeatherEvent event = new WeatherEvent("SN", null, null, null, null, 30);

        String result = event.toString();

        assertThat(result)
                .contains("WeatherEvent")
                .contains("code='SN'")
                .contains("ended=:30")
                .doesNotContain("began=");
    }

    @Test
    @DisplayName("toString with both begin and end times")
    void testToStringWithBothTimes() {
        // Tests the branch: hasBeginTime() = true, hasEndTime() = true
        WeatherEvent event = new WeatherEvent("FZRA", null, 11, 59, 12, 40);

        String result = event.toString();

        assertThat(result)
                .contains("WeatherEvent")
                .contains("code='FZRA'")
                .contains("began=11:59")
                .contains("ended=12:40");
    }

    @Test
    @DisplayName("toString with intensity included")
    void testToStringWithIntensity() {
        WeatherEvent event = new WeatherEvent("RA", "+", null, 20, null, 45);

        String result = event.toString();

        assertThat(result)
                .contains("code='+RA'")
                .contains("began=:20")
                .contains("ended=:45");
    }

    // ========== getSummary() Coverage Tests ==========

    @Test
    @DisplayName("getSummary with only begin time")
    void testGetSummaryWithOnlyBeginTime() {
        // Tests the branch: hasBeginTime() = true, hasEndTime() = false
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        String summary = event.getSummary();

        assertThat(summary)
                .isEqualTo("RA began :05")
                .doesNotContain("ended");
    }

    @Test
    @DisplayName("getSummary with only end time")
    void testGetSummaryWithOnlyEndTime() {
        // Tests the branch: hasBeginTime() = false, hasEndTime() = true
        WeatherEvent event = new WeatherEvent("SN", null, null, null, null, 30);

        String summary = event.getSummary();

        assertThat(summary)
                .isEqualTo("SN ended :30")
                .doesNotContain("began");
    }

    @Test
    @DisplayName("getSummary with both begin and end times")
    void testGetSummaryWithBothTimes() {
        // Tests the branch: hasBeginTime() = true, hasEndTime() = true
        WeatherEvent event = new WeatherEvent("FZRA", null, 11, 59, 12, 40);

        String summary = event.getSummary();

        assertThat(summary)
                .contains("FZRA")
                .contains("began 11:59")
                .contains("ended 12:40");
    }

    @Test
    @DisplayName("getSummary with light intensity and only begin time")
    void testGetSummaryWithLightIntensityBeginOnly() {
        WeatherEvent event = new WeatherEvent("DZ", "-", null, 15, null, null);

        String summary = event.getSummary();

        assertThat(summary)
                .startsWith("-DZ")
                .contains("began :15")
                .doesNotContain("ended");
    }

    @Test
    @DisplayName("getSummary with heavy intensity and only end time")
    void testGetSummaryWithHeavyIntensityEndOnly() {
        WeatherEvent event = new WeatherEvent("TSRA", "+", null, null, null, 45);

        String summary = event.getSummary();

        assertThat(summary)
                .startsWith("+TSRA")
                .contains("ended :45")
                .doesNotContain("began");
    }

    @Test
    @DisplayName("getSummary with intensity and both times")
    void testGetSummaryWithIntensityAndBothTimes() {
        WeatherEvent event = new WeatherEvent("SN", "+", null, 10, null, 25);

        String summary = event.getSummary();

        assertThat(summary)
                .startsWith("+SN")
                .contains("began :10")
                .contains("ended :25");
    }

    // ========== hasEndHour() Coverage Tests ==========

    @Test
    @DisplayName("hasEndHour should return true when end hour is present")
    void testHasEndHourReturnsTrueWhenPresent() {
        // End time with full timestamp (hour + minute)
        WeatherEvent event = new WeatherEvent("RA", null, null, null, 12, 40);

        assertThat(event.hasEndHour()).isTrue();
        assertThat(event.hasEndTime()).isTrue();
    }

    @Test
    @DisplayName("hasEndHour should return false when end hour is absent")
    void testHasEndHourReturnsFalseWhenAbsent() {
        // End time with only minutes (no hour)
        WeatherEvent event = new WeatherEvent("RA", null, null, null, null, 30);

        assertThat(event.hasEndHour()).isFalse();
        assertThat(event.hasEndTime()).isTrue(); // Still has end time (minute only)
    }

    @Test
    @DisplayName("hasEndHour should return false when no end time at all")
    void testHasEndHourReturnsFalseWhenNoEndTime() {
        // Only begin time, no end time
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        assertThat(event.hasEndHour()).isFalse();
        assertThat(event.hasEndTime()).isFalse();
    }

    @Test
    @DisplayName("hasEndHour should work with full end timestamp")
    void testHasEndHourWithFullTimestamp() {
        // Full end timestamp: 23:59
        WeatherEvent event = new WeatherEvent("FZRA", null, 11, 59, 23, 59);

        assertThat(event.hasEndHour()).isTrue();
        assertThat(event.endHour()).isEqualTo(23);
        assertThat(event.endMinute()).isEqualTo(59);
        assertThat(event.getEndTime()).isEqualTo(LocalTime.of(23, 59));
    }

    // ========== hasBeginHour() Coverage Tests ==========

    @Test
    @DisplayName("hasBeginHour should return true when begin hour is present")
    void testHasBeginHourReturnsTrueWhenPresent() {
        // Begin time with full timestamp (hour + minute)
        WeatherEvent event = new WeatherEvent("RA", null, 11, 59, null, null);

        assertThat(event.hasBeginHour()).isTrue();
        assertThat(event.hasBeginTime()).isTrue();
    }

    @Test
    @DisplayName("hasBeginHour should return false when begin hour is absent")
    void testHasBeginHourReturnsFalseWhenAbsent() {
        // Begin time with only minutes (no hour)
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        assertThat(event.hasBeginHour()).isFalse();
        assertThat(event.hasBeginTime()).isTrue(); // Still has begin time (minute only)
    }

    @Test
    @DisplayName("hasBeginHour should return false when no begin time at all")
    void testHasBeginHourReturnsFalseWhenNoBeginTime() {
        // Only end time, no begin time
        WeatherEvent event = new WeatherEvent("RA", null, null, null, null, 30);

        assertThat(event.hasBeginHour()).isFalse();
        assertThat(event.hasBeginTime()).isFalse();
    }

    @Test
    @DisplayName("hasBeginHour should work with full begin timestamp")
    void testHasBeginHourWithFullTimestamp() {
        // Full begin timestamp: 11:59
        WeatherEvent event = new WeatherEvent("FZRA", null, 11, 59, 12, 40);

        assertThat(event.hasBeginHour()).isTrue();
        assertThat(event.beginHour()).isEqualTo(11);
        assertThat(event.beginMinute()).isEqualTo(59);
        assertThat(event.getBeginTime()).isEqualTo(LocalTime.of(11, 59));
    }

    /**
     * ADD THESE TESTS TO WeatherEventTest.java
     * =========================================
     *
     * These tests will bring getEndTime() coverage from 87%/50% to 100%/100%
     * Using AssertJ assertions consistently.
     */

// ========== getEndTime() Coverage Tests ==========

    @Test
    @DisplayName("getEndTime should return LocalTime when both hour and minute present")
    void testGetEndTimeWithFullTimestamp() {
        // Branch: endMinute != null AND endHour != null → return LocalTime
        WeatherEvent event = new WeatherEvent("RA", null, null, null, 12, 40);

        LocalTime endTime = event.getEndTime();

        assertThat(endTime)
                .isEqualTo(LocalTime.of(12, 40));
        assertThat(endTime.getHour()).isEqualTo(12);
        assertThat(endTime.getMinute()).isEqualTo(40);
    }

    @Test
    @DisplayName("getEndTime should return null when endMinute is null")
    void testGetEndTimeReturnsNullWhenMinuteIsNull() {
        // Branch: endMinute == null → return null
        // This is actually impossible to create with the constructor validation,
        // but we test the logic path
        // We'll use a scenario where only begin time exists
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        LocalTime endTime = event.getEndTime();

        assertThat(endTime).isNull();
        assertThat(event.hasEndTime()).isFalse();
    }

    @Test
    @DisplayName("getEndTime should return null when endHour is null")
    void testGetEndTimeReturnsNullWhenHourIsNull() {
        // Branch: endMinute != null BUT endHour == null → return null
        // Minute-only format (no hour)
        WeatherEvent event = new WeatherEvent("RA", null, null, null, null, 30);

        LocalTime endTime = event.getEndTime();

        assertThat(endTime).isNull();
        assertThat(event.hasEndTime()).isTrue();  // Has end time (minute)
        assertThat(event.hasEndHour()).isFalse(); // But no hour
    }

    @Test
    @DisplayName("getEndTime should handle midnight correctly")
    void testGetEndTimeWithMidnight() {
        // Edge case: 00:00 (midnight)
        WeatherEvent event = new WeatherEvent("RA", null, null, null, 0, 0);

        LocalTime endTime = event.getEndTime();

        assertThat(endTime)
                .isNotNull()
                .isEqualTo(LocalTime.MIDNIGHT)
                .isEqualTo(LocalTime.of(0, 0));
    }

    @Test
    @DisplayName("getEndTime should handle end of day correctly")
    void testGetEndTimeWithEndOfDay() {
        // Edge case: 23:59
        WeatherEvent event = new WeatherEvent("RA", null, null, null, 23, 59);

        LocalTime endTime = event.getEndTime();

        assertThat(endTime)
                .isNotNull()
                .isEqualTo(LocalTime.of(23, 59));
    }

// ========== getBeginTime() Coverage Tests ==========
// Add similar tests for getBeginTime() to ensure symmetry

    @Test
    @DisplayName("getBeginTime should return LocalTime when both hour and minute present")
    void testGetBeginTimeWithFullTimestamp() {
        // Branch: beginMinute != null AND beginHour != null → return LocalTime
        WeatherEvent event = new WeatherEvent("RA", null, 11, 59, null, null);

        LocalTime beginTime = event.getBeginTime();

        assertThat(beginTime)
                .isNotNull()
                .isEqualTo(LocalTime.of(11, 59));
        assertThat(beginTime.getHour()).isEqualTo(11);
        assertThat(beginTime.getMinute()).isEqualTo(59);
    }

    @Test
    @DisplayName("getBeginTime should return null when beginMinute is null")
    void testGetBeginTimeReturnsNullWhenMinuteIsNull() {
        // Branch: beginMinute == null → return null
        // Only end time exists
        WeatherEvent event = new WeatherEvent("RA", null, null, null, null, 30);

        LocalTime beginTime = event.getBeginTime();

        assertThat(beginTime).isNull();
        assertThat(event.hasBeginTime()).isFalse();
    }

    @Test
    @DisplayName("getBeginTime should return null when beginHour is null")
    void testGetBeginTimeReturnsNullWhenHourIsNull() {
        // Branch: beginMinute != null BUT beginHour == null → return null
        // Minute-only format (no hour)
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        LocalTime beginTime = event.getBeginTime();

        assertThat(beginTime).isNull();
        assertThat(event.hasBeginTime()).isTrue();  // Has begin time (minute)
        assertThat(event.hasBeginHour()).isFalse(); // But no hour
    }

    @Test
    @DisplayName("getBeginTime should handle midnight correctly")
    void testGetBeginTimeWithMidnight() {
        // Edge case: 00:00 (midnight)
        WeatherEvent event = new WeatherEvent("RA", null, 0, 0, null, null);

        LocalTime beginTime = event.getBeginTime();

        assertThat(beginTime)
                .isNotNull()
                .isEqualTo(LocalTime.MIDNIGHT)
                .isEqualTo(LocalTime.of(0, 0));
    }

    @Test
    @DisplayName("getBeginTime should handle end of day correctly")
    void testGetBeginTimeWithEndOfDay() {
        // Edge case: 23:59
        WeatherEvent event = new WeatherEvent("RA", null, 23, 59, null, null);

        LocalTime beginTime = event.getBeginTime();

        assertThat(beginTime)
                .isNotNull()
                .isEqualTo(LocalTime.of(23, 59));
    }

    // ========== Both times together ==========

    @Test
    @DisplayName("getBeginTime and getEndTime should both work with full timestamps")
    void testBothTimesWithFullTimestamps() {
        WeatherEvent event = new WeatherEvent("FZRA", null, 11, 59, 12, 40);

        LocalTime beginTime = event.getBeginTime();
        LocalTime endTime = event.getEndTime();

        // Chain assertions on beginTime
        assertThat(beginTime)
                .isNotNull()
                .isEqualTo(LocalTime.of(11, 59));

        // Chain assertions on endTime
        assertThat(endTime)
                .isNotNull()
                .isEqualTo(LocalTime.of(12, 40))
                .isAfter(beginTime);
    }

    @Test
    @DisplayName("getBeginTime and getEndTime should handle minute-only formats")
    void testBothTimesWithMinuteOnlyFormats() {
        WeatherEvent event = new WeatherEvent("RA", null, null, 15, null, 30);

        LocalTime beginTime = event.getBeginTime();
        LocalTime endTime = event.getEndTime();

        assertThat(beginTime).isNull(); // No hour, so no LocalTime
        assertThat(endTime).isNull();   // No hour, so no LocalTime

        // But the formatted strings should work
        assertThat(event.getFormattedBeginTime()).isEqualTo(":15");
        assertThat(event.getFormattedEndTime()).isEqualTo(":30");
    }

    // ========== getFormattedBeginTime() Coverage Tests ==========

    @Test
    @DisplayName("getFormattedBeginTime should return N/A when beginMinute is null")
    void testGetFormattedBeginTimeReturnsNAWhenMinuteIsNull() {
        // Branch: beginMinute == null → return "N/A"
        // Only end time exists, no begin time at all
        WeatherEvent event = new WeatherEvent("RA", null, null, null, null, 30);

        assertThat(event.getFormattedBeginTime())
                .isEqualTo("N/A");

        assertThat(event.hasBeginTime()).isFalse();
    }

    @Test
    @DisplayName("getFormattedBeginTime should format single digit minute with leading zero")
    void testGetFormattedBeginTimeLeadingZero() {
        // Verify formatting with single-digit minute
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        assertThat(event.getFormattedBeginTime())
                .isEqualTo(":05")
                .hasSize(3);
    }

    @Test
    @DisplayName("getFormattedBeginTime should format midnight correctly")
    void testGetFormattedBeginTimeWithMidnight() {
        // Edge case: 00:00
        WeatherEvent event = new WeatherEvent("RA", null, 0, 0, null, null);

        assertThat(event.getFormattedBeginTime())
                .isEqualTo("00:00");
    }

    @Test
    @DisplayName("getFormattedBeginTime should format end of day correctly")
    void testGetFormattedBeginTimeWithEndOfDay() {
        // Edge case: 23:59
        WeatherEvent event = new WeatherEvent("RA", null, 23, 59, null, null);

        assertThat(event.getFormattedBeginTime())
                .isEqualTo("23:59");
    }

    // ========== getFormattedEndTime() Coverage Tests ==========

    @Test
    @DisplayName("getFormattedEndTime should return N/A when endMinute is null")
    void testGetFormattedEndTimeReturnsNAWhenMinuteIsNull() {
        // Branch: endMinute == null → return "N/A"
        // Only begin time exists, no end time at all
        WeatherEvent event = new WeatherEvent("RA", null, null, 5, null, null);

        assertThat(event.getFormattedEndTime())
                .isEqualTo("N/A");

        assertThat(event.hasEndTime()).isFalse();
    }

    @Test
    @DisplayName("getFormattedEndTime should return HH:MM when both hour and minute present")
    void testGetFormattedEndTimeWithFullTimestamp() {
        // Branch: endMinute != null AND endHour != null → return "HH:MM"
        WeatherEvent event = new WeatherEvent("RA", null, null, null, 12, 40);

        assertThat(event.getFormattedEndTime())
                .isEqualTo("12:40");
    }

    @Test
    @DisplayName("getFormattedEndTime should return :MM when only minute present")
    void testGetFormattedEndTimeWithMinuteOnly() {
        // Branch: endMinute != null BUT endHour == null → return ":MM"
        WeatherEvent event = new WeatherEvent("RA", null, null, null, null, 30);

        assertThat(event.getFormattedEndTime())
                .isEqualTo(":30");
    }

    @Test
    @DisplayName("getFormattedEndTime should format single digit minute with leading zero")
    void testGetFormattedEndTimeLeadingZero() {
        // Verify formatting with single-digit minute
        WeatherEvent event = new WeatherEvent("RA", null, null, null, null, 5);

        assertThat(event.getFormattedEndTime())
                .isEqualTo(":05")
                .hasSize(3);
    }

    @Test
    @DisplayName("getFormattedEndTime should format midnight correctly")
    void testGetFormattedEndTimeWithMidnight() {
        // Edge case: 00:00
        WeatherEvent event = new WeatherEvent("RA", null, null, null, 0, 0);

        assertThat(event.getFormattedEndTime())
                .isEqualTo("00:00");
    }

    @Test
    @DisplayName("getFormattedEndTime should format end of day correctly")
    void testGetFormattedEndTimeWithEndOfDay() {
        // Edge case: 23:59
        WeatherEvent event = new WeatherEvent("RA", null, null, null, 23, 59);

        assertThat(event.getFormattedEndTime())
                .isEqualTo("23:59");
    }

    // ========== Integration tests for both formatted times ==========

    @Test
    @DisplayName("Both formatted times should handle minute-only format")
    void testBothFormattedTimesWithMinuteOnly() {
        WeatherEvent event = new WeatherEvent("RA", null, null, 15, null, 30);

        assertThat(event.getFormattedBeginTime())
                .isEqualTo(":15");

        assertThat(event.getFormattedEndTime())
                .isEqualTo(":30");
    }

    @Test
    @DisplayName("Both formatted times should handle full timestamp format")
    void testBothFormattedTimesWithFullTimestamp() {
        WeatherEvent event = new WeatherEvent("FZRA", null, 11, 59, 12, 40);

        assertThat(event.getFormattedBeginTime())
                .isEqualTo("11:59");

        assertThat(event.getFormattedEndTime())
                .isEqualTo("12:40");
    }

    @Test
    @DisplayName("Formatted times should show N/A appropriately")
    void testFormattedTimesShowNA() {
        // Begin only
        WeatherEvent event1 = new WeatherEvent("RA", null, null, 5, null, null);
        assertThat(event1.getFormattedBeginTime()).isEqualTo(":05");
        assertThat(event1.getFormattedEndTime()).isEqualTo("N/A");

        // End only
        WeatherEvent event2 = new WeatherEvent("SN", null, null, null, null, 30);
        assertThat(event2.getFormattedBeginTime()).isEqualTo("N/A");
        assertThat(event2.getFormattedEndTime()).isEqualTo(":30");
    }
}
