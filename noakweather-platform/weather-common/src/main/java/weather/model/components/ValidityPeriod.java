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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Immutable value object representing a TAF validity period.
 *
 * TAF forecasts are valid for a specific time period, typically 24 or 30 hours.
 * The validity period is expressed in UTC and defines when the forecast applies.
 *
 * Format in TAF: DDHH/DDHH
 * - First DDHH: Start day and hour (UTC)
 * - Second DDHH: End day and hour (UTC)
 *
 * Examples:
 * - "1520/1624" = Valid from 15th day at 2000Z to 16th day at 2400Z (24 hours)
 * - "3018/3124" = Valid from 30th day at 1800Z to 31st day at 2400Z (30 hours)
 * - "0100/0206" = Valid from 1st day at 0000Z to 2nd day at 0600Z (30 hours)
 *
 * Design Philosophy:
 * - Immutable: Once created, the period cannot change
 * - Timezone-aware: All times are UTC (Instant)
 * - Validation: Ensures valid time ranges
 *
 * @param validFrom Start of the validity period (UTC)
 * @param validTo End of the validity period (UTC)
 *
 * @author bclasky1539
 *
 */
public record ValidityPeriod(
        Instant validFrom,
        Instant validTo
) {

    /**
     * Maximum reasonable TAF validity period in hours.
     * Standard TAFs are 24-30 hours, but allowing up to 48 for extended forecasts.
     */
    private static final int MAX_VALIDITY_HOURS = 48;

    /**
     * Minimum reasonable TAF validity period in hours.
     * TAFs should be at least a few hours to be meaningful.
     */
    private static final int MIN_VALIDITY_HOURS = 1;

    /**
     * Compact constructor with validation.
     */
    public ValidityPeriod {
        validateNotNull(validFrom, validTo);
        validateFromBeforeTo(validFrom, validTo);
        validateReasonableDuration(validFrom, validTo);
    }

    // ==================== Validation Helper Methods ====================

    /**
     * Validate that both times are not null.
     */
    private static void validateNotNull(Instant validFrom, Instant validTo) {
        if (validFrom == null) {
            throw new IllegalArgumentException("Validity start time cannot be null");
        }
        if (validTo == null) {
            throw new IllegalArgumentException("Validity end time cannot be null");
        }
    }

    /**
     * Validate that start time is before end time.
     */
    private static void validateFromBeforeTo(Instant validFrom, Instant validTo) {
        if (!validFrom.isBefore(validTo)) {
            throw new IllegalArgumentException(
                    "Validity start time (" + validFrom + ") must be before end time (" + validTo + ")"
            );
        }
    }

    /**
     * Validate that the duration is within reasonable bounds.
     */
    private static void validateReasonableDuration(Instant validFrom, Instant validTo) {
        long hours = Duration.between(validFrom, validTo).toHours();

        if (hours < MIN_VALIDITY_HOURS) {
            throw new IllegalArgumentException(
                    "Validity period too short (" + hours + " hours). Minimum is " + MIN_VALIDITY_HOURS + " hour(s)"
            );
        }

        if (hours > MAX_VALIDITY_HOURS) {
            throw new IllegalArgumentException(
                    "Validity period too long (" + hours + " hours). Maximum is " + MAX_VALIDITY_HOURS + " hours"
            );
        }
    }

    // ==================== Query Methods ====================

    /**
     * Get the duration of the validity period.
     *
     * @return duration between start and end times
     */
    public Duration getDuration() {
        return Duration.between(validFrom, validTo);
    }

    /**
     * Get the duration in hours.
     *
     * @return duration in hours
     */
    public long getDurationHours() {
        return getDuration().toHours();
    }

    /**
     * Check if a given time falls within this validity period.
     *
     * @param time the time to check
     * @return true if the time is within the validity period (inclusive of start, exclusive of end)
     */
    public boolean contains(Instant time) {
        if (time == null) {
            return false;
        }
        return !time.isBefore(validFrom) && time.isBefore(validTo);
    }

    /**
     * Check if this validity period is currently active.
     *
     * @return true if current time is within the validity period
     */
    public boolean isCurrentlyValid() {
        return contains(Instant.now());
    }

    /**
     * Check if this validity period has expired.
     *
     * @return true if current time is after the end of the validity period
     */
    public boolean hasExpired() {
        return Instant.now().isAfter(validTo);
    }

    /**
     * Check if this validity period has not yet started.
     *
     * @return true if current time is before the start of the validity period
     */
    public boolean isFuture() {
        return Instant.now().isBefore(validFrom);
    }

    /**
     * Check if this is a standard 24-hour TAF.
     *
     * @return true if duration is 24 hours
     */
    public boolean isStandard24Hour() {
        return getDurationHours() == 24;
    }

    /**
     * Check if this is a 30-hour TAF (common for some airports).
     *
     * @return true if duration is 30 hours
     */
    public boolean is30Hour() {
        return getDurationHours() == 30;
    }

    /**
     * Check if another validity period overlaps with this one.
     *
     * @param other the other validity period
     * @return true if the periods overlap
     */
    public boolean overlaps(ValidityPeriod other) {
        if (other == null) {
            return false;
        }

        // Periods overlap if one starts before the other ends and vice versa
        return !this.validTo.isBefore(other.validFrom) && !other.validTo.isBefore(this.validFrom);
    }

    /**
     * Get the time remaining until this validity period expires.
     *
     * @return duration until expiration, or null if already expired
     */
    public Duration getTimeUntilExpiration() {
        Instant now = Instant.now();
        if (now.isAfter(validTo)) {
            return null; // Already expired
        }
        return Duration.between(now, validTo);
    }

    /**
     * Get the time since this validity period expired.
     *
     * @return duration since expiration, or null if not yet expired
     */
    public Duration getTimeSinceExpiration() {
        Instant now = Instant.now();
        if (now.isBefore(validTo)) {
            return null; // Not yet expired
        }
        return Duration.between(validTo, now);
    }

    // ==================== Conversion Methods ====================

    /**
     * Format the validity period in TAF format (DDHH/DDHH).
     *
     * @return formatted string (e.g., "1520/1624")
     */
    public String toTafFormat() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddHH")
                .withZone(ZoneOffset.UTC);

        String fromStr = formatter.format(validFrom);
        String toStr = formatter.format(validTo);

        return fromStr + "/" + toStr;
    }

    /**
     * Format the validity period with full date-time information.
     *
     * @return formatted string with full timestamps
     */
    public String toFullFormat() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm'Z'")
                .withZone(ZoneOffset.UTC);

        return formatter.format(validFrom) + " to " + formatter.format(validTo);
    }

    /**
     * Get a human-readable summary of the validity period.
     *
     * @return formatted summary
     */
    public String getSummary() {
        long hours = getDurationHours();
        String status;

        if (isCurrentlyValid()) {
            status = "Currently active";
        } else if (hasExpired()) {
            status = "Expired";
        } else {
            status = "Future";
        }

        return String.format("Valid %s (%d hours) - %s", toTafFormat(), hours, status);
    }

    // ==================== Factory Methods ====================

    /**
     * Create a validity period from two Instant values.
     *
     * @param validFrom start time
     * @param validTo end time
     * @return new ValidityPeriod
     */
    public static ValidityPeriod of(Instant validFrom, Instant validTo) {
        return new ValidityPeriod(validFrom, validTo);
    }

    /**
     * Create a validity period starting from a given time with a specified duration.
     *
     * @param validFrom start time
     * @param duration duration of the validity period
     * @return new ValidityPeriod
     */
    public static ValidityPeriod fromDuration(Instant validFrom, Duration duration) {
        if (validFrom == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        if (duration == null) {
            throw new IllegalArgumentException("Duration cannot be null");
        }

        Instant validTo = validFrom.plus(duration);
        return new ValidityPeriod(validFrom, validTo);
    }

    /**
     * Create a standard 24-hour validity period starting at the given time.
     *
     * @param validFrom start time
     * @return new ValidityPeriod with 24-hour duration
     */
    public static ValidityPeriod standard24Hour(Instant validFrom) {
        return fromDuration(validFrom, Duration.ofHours(24));
    }

    /**
     * Create a 30-hour validity period starting at the given time.
     *
     * @param validFrom start time
     * @return new ValidityPeriod with 30-hour duration
     */
    public static ValidityPeriod thirtyHour(Instant validFrom) {
        return fromDuration(validFrom, Duration.ofHours(30));
    }

    @Override
    public String toString() {
        return String.format("ValidityPeriod[%s, %d hours]", toTafFormat(), getDurationHours());
    }
}
