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

import java.time.LocalTime;

/**
 * Immutable value object representing a weather phenomenon's begin/end time from METAR remarks.
 *
 * Weather phenomena can be reported with their start and/or end times in the remarks section.
 * This provides more granular information about when specific weather conditions occurred.
 *
 * Examples from METAR remarks:
 * - "RAB05" → WeatherEvent("RA", null, null, 5, null, null)
 * - "FZRAB1159E1240" → WeatherEvent("FZRA", null, 11, 59, 12, 40)
 * - "TSB0159E0240" → WeatherEvent("TS", null, 1, 59, 2, 40)
 * - "-RAB05" → WeatherEvent("RA", "-", null, 5, null, null)
 * - "+TSRAB20E45" → WeatherEvent("TSRA", "+", null, 20, null, 45)
 *
 * Format: [Intensity][Descriptor][Phenomenon]Bhhmm or Bmm and/or Ehhmm or Emm
 *
 * Where:
 * - Intensity: - (light), + (heavy), or absent (moderate)
 * - Descriptor: MI, PR, BC, DR, BL, SH, TS, FZ
 * - Phenomenon: DZ, RA, SN, SG, IC, PL, GR, GS, UP, BR, FG, FU, VA, DU, SA, HZ, PY
 * - B: Begin marker
 * - E: End marker
 * - hhmm or mm: Time in hours/minutes (UTC)
 *
 * @param weatherCode The complete weather code (e.g., "RA", "FZRA", "TS")
 * @param intensity Optional intensity indicator ('-', '+', or null for moderate)
 * @param beginHour Hour when phenomenon began (0-23), null if only minutes provided
 * @param beginMinute Minute when phenomenon began (0-59), null if no begin time
 * @param endHour Hour when phenomenon ended (0-23), null if only minutes provided
 * @param endMinute Minute when phenomenon ended (0-59), null if no end time
 *
 * @author bclasky1539
 *
 */
public record WeatherEvent(
        String weatherCode,
        String intensity,
        Integer beginHour,
        Integer beginMinute,
        Integer endHour,
        Integer endMinute
) {

    /**
     * Compact constructor with validation.
     * Validation logic is extracted to private methods to reduce cognitive complexity.
     */
    public WeatherEvent {
        validateWeatherCode(weatherCode);
        validateIntensity(intensity);
        validateBeginTime(beginHour, beginMinute);
        validateEndTime(endHour, endMinute);
        validateAtLeastOneTime(beginMinute, endMinute);
    }

    /**
     * Validate weather code is not null or blank.
     *
     * @param weatherCode the weather code to validate
     * @throws IllegalArgumentException if weather code is null or blank
     */
    private static void validateWeatherCode(String weatherCode) {
        if (weatherCode == null) {
            throw new IllegalArgumentException("Weather code cannot be null");
        }

        if (weatherCode.isBlank()) {
            throw new IllegalArgumentException("Weather code cannot be blank");
        }
    }

    /**
     * Validate intensity is either '-', '+', or null.
     *
     * @param intensity the intensity to validate
     * @throws IllegalArgumentException if intensity is invalid
     */
    private static void validateIntensity(String intensity) {
        if (intensity != null && !intensity.matches("[-+]")) {
            throw new IllegalArgumentException(
                    "Intensity must be '-' (light) or '+' (heavy): " + intensity
            );
        }
    }

    /**
     * Validate begin time components are within valid ranges.
     *
     * @param beginHour the begin hour to validate (0-23 or null)
     * @param beginMinute the begin minute to validate (0-59 or null)
     * @throws IllegalArgumentException if begin time components are out of range
     */
    private static void validateBeginTime(Integer beginHour, Integer beginMinute) {
        validateHour(beginHour, "Begin");
        validateMinute(beginMinute, "Begin");
    }

    /**
     * Validate end time components are within valid ranges.
     *
     * @param endHour the end hour to validate (0-23 or null)
     * @param endMinute the end minute to validate (0-59 or null)
     * @throws IllegalArgumentException if end time components are out of range
     */
    private static void validateEndTime(Integer endHour, Integer endMinute) {
        validateHour(endHour, "End");
        validateMinute(endMinute, "End");
    }

    /**
     * Validate hour is within valid range (0-23) or null.
     *
     * @param hour the hour to validate
     * @param timeType the type of time (Begin or End) for error messages
     * @throws IllegalArgumentException if hour is out of range
     */
    private static void validateHour(Integer hour, String timeType) {
        if (hour != null && (hour < 0 || hour > 23)) {
            throw new IllegalArgumentException(
                    timeType + " hour must be between 0 and 23: " + hour
            );
        }
    }

    /**
     * Validate minute is within valid range (0-59) or null.
     *
     * @param minute the minute to validate
     * @param timeType the type of time (Begin or End) for error messages
     * @throws IllegalArgumentException if minute is out of range
     */
    private static void validateMinute(Integer minute, String timeType) {
        if (minute != null && (minute < 0 || minute > 59)) {
            throw new IllegalArgumentException(
                    timeType + " minute must be between 0 and 59: " + minute
            );
        }
    }

    /**
     * Validate that at least one time (begin or end minute) is present.
     *
     * @param beginMinute the begin minute
     * @param endMinute the end minute
     * @throws IllegalArgumentException if both are null
     */
    private static void validateAtLeastOneTime(Integer beginMinute, Integer endMinute) {
        if (beginMinute == null && endMinute == null) {
            throw new IllegalArgumentException(
                    "Weather event must have at least a begin or end time"
            );
        }
    }

    // ==================== Time Conversion Methods ====================

    /**
     * Get the begin time as LocalTime if both hour and minute are present.
     *
     * @return LocalTime of begin event, or null if no begin time or hour not specified
     */
    public LocalTime getBeginTime() {
        if (beginMinute == null || beginHour == null) {
            return null;
        }
        return LocalTime.of(beginHour, beginMinute);
    }

    /**
     * Get the end time as LocalTime if both hour and minute are present.
     *
     * @return LocalTime of end event, or null if no end time or hour not specified
     */
    public LocalTime getEndTime() {
        if (endMinute == null || endHour == null) {
            return null;
        }
        return LocalTime.of(endHour, endMinute);
    }

    // ==================== Convenience Methods ====================

    /**
     * Check if this event has a begin time.
     *
     * @return true if begin time is present
     */
    public boolean hasBeginTime() {
        return beginMinute != null;
    }

    /**
     * Check if this event has an end time.
     *
     * @return true if end time is present
     */
    public boolean hasEndTime() {
        return endMinute != null;
    }

    /**
     * Check if the begin time includes hours.
     *
     * @return true if begin hour is specified
     */
    public boolean hasBeginHour() {
        return beginHour != null;
    }

    /**
     * Check if the end time includes hours.
     *
     * @return true if end hour is specified
     */
    public boolean hasEndHour() {
        return endHour != null;
    }

    // ==================== Formatting Methods ====================

    /**
     * Get a formatted string representation of the begin time.
     *
     * @return formatted begin time (e.g., "11:59", ":05", or "N/A")
     */
    public String getFormattedBeginTime() {
        if (beginMinute == null) {
            return "N/A";
        }
        if (beginHour != null) {
            return String.format("%02d:%02d", beginHour, beginMinute);
        }
        return String.format(":%02d", beginMinute);
    }

    /**
     * Get a formatted string representation of the end time.
     *
     * @return formatted end time (e.g., "12:40", ":30", or "N/A")
     */
    public String getFormattedEndTime() {
        if (endMinute == null) {
            return "N/A";
        }
        if (endHour != null) {
            return String.format("%02d:%02d", endHour, endMinute);
        }
        return String.format(":%02d", endMinute);
    }

    /**
     * Get the full weather descriptor including intensity if present.
     *
     * @return formatted weather description (e.g., "-RA", "TS", "+TSRA")
     */
    public String getFullWeatherCode() {
        return intensity != null ? intensity + weatherCode : weatherCode;
    }

    /**
     * Get a summary for display purposes.
     *
     * @return brief summary string
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFullWeatherCode());

        if (hasBeginTime()) {
            sb.append(" began ").append(getFormattedBeginTime());
        }

        if (hasEndTime()) {
            if (hasBeginTime()) {
                sb.append(",");
            }
            sb.append(" ended ").append(getFormattedEndTime());
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "WeatherEvent{" +
                "code='" + getFullWeatherCode() + '\'' +
                (hasBeginTime() ? ", began=" + getFormattedBeginTime() : "") +
                (hasEndTime() ? ", ended=" + getFormattedEndTime() : "") +
                '}';
    }
}
