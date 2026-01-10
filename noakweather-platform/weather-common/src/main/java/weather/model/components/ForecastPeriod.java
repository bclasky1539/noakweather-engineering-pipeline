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

import weather.model.WeatherConditions;
import weather.model.enums.ChangeIndicator;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Immutable value object representing a single forecast period within a TAF.
 *
 * TAF forecasts consist of a base forecast plus zero or more change groups.
 * Each change group describes how conditions will differ from the base forecast
 * during a specific time period or starting at a specific time.
 *
 * Design Philosophy:
 * - Immutable: Once created, the forecast period cannot change
 * - Reuses WeatherConditions: Same structure as METAR observations
 * - Time-aware: Different change indicators use time differently
 * - Flexible: Handles BASE, FM, TEMPO, BECMG, and PROB forecasts
 *
 * Examples from TAF:
 *
 * BASE period:
 *   "TAF KJFK 151953Z 1520/1624 VRB02KT P6SM FEW250"
 *   → ForecastPeriod(BASE, null, 1520Z, 1624Z, null, conditions)
 *
 * FM (From) period:
 *   "FM152100 21005KT P6SM SCT250"
 *   → ForecastPeriod(FM, 152100Z, null, null, null, conditions)
 *
 * TEMPO period:
 *   "TEMPO 3003/3011 P6SM -SHSN BKN040"
 *   → ForecastPeriod(TEMPO, null, 3003Z, 3011Z, null, conditions)
 *
 * BECMG period:
 *   "BECMG 3108/3110 FEW038CB"
 *   → ForecastPeriod(BECMG, null, 3108Z, 3110Z, null, conditions)
 *
 * PROB period:
 *   "PROB30 3005/3011 3SM -SHSN OVC020"
 *   → ForecastPeriod(PROB, null, 3005Z, 3011Z, 30, conditions)
 *
 * @param changeIndicator Type of forecast change (BASE, FM, TEMPO, BECMG, PROB)
 * @param changeTime Exact time of change (FM only, format: FMDDHHmm)
 * @param periodStart Start of the forecast period (TEMPO, BECMG, PROB, BASE)
 * @param periodEnd End of the forecast period (TEMPO, BECMG, PROB, BASE)
 * @param probability Probability percentage (PROB only: 30 or 40)
 * @param conditions The forecast weather conditions for this period
 *
 * @author bclasky1539
 *
 */
public record ForecastPeriod(
        ChangeIndicator changeIndicator,
        Instant changeTime,
        Instant periodStart,
        Instant periodEnd,
        Integer probability,
        WeatherConditions conditions
) {

    /**
     * Valid probability values for PROB forecasts.
     */
    private static final int[] VALID_PROBABILITIES = {30, 40};

    /**
     * Maximum reasonable forecast period duration in hours.
     */
    private static final int MAX_PERIOD_HOURS = 12;

    /**
     * Compact constructor with validation.
     */
    public ForecastPeriod {
        validateChangeIndicator(changeIndicator);
        validateTimeFields(changeIndicator, changeTime, periodStart, periodEnd);
        validateProbability(changeIndicator, probability);
        validateConditions(conditions);
    }

    // ==================== Validation Helper Methods ====================

    /**
     * Validate that change indicator is not null.
     */
    private static void validateChangeIndicator(ChangeIndicator changeIndicator) {
        if (changeIndicator == null) {
            throw new IllegalArgumentException("Change indicator cannot be null");
        }
    }

    /**
     * Validate time fields based on change indicator type.
     */
    private static void validateTimeFields(
            ChangeIndicator changeIndicator,
            Instant changeTime,
            Instant periodStart,
            Instant periodEnd
    ) {
        switch (changeIndicator) {
            case FM -> validateFmTimeFields(changeTime, periodStart, periodEnd);
            case TEMPO, BECMG, PROB -> validatePeriodTimeFields(changeIndicator, changeTime, periodStart, periodEnd);
            case BASE -> validateBaseTimeFields(periodStart, periodEnd);
        }
    }

    /**
     * Validate time fields for FM (From) forecasts.
     */
    private static void validateFmTimeFields(Instant changeTime, Instant periodStart, Instant periodEnd) {
        // FM requires exact change time, no period
        if (changeTime == null) {
            throw new IllegalArgumentException("FM forecast must have changeTime");
        }
        if (periodStart != null || periodEnd != null) {
            throw new IllegalArgumentException("FM forecast should not have period start/end");
        }
    }

    /**
     * Validate time fields for period-based forecasts (TEMPO, BECMG, PROB).
     */
    private static void validatePeriodTimeFields(
            ChangeIndicator changeIndicator,
            Instant changeTime,
            Instant periodStart,
            Instant periodEnd
    ) {
        // These require period start/end, no exact change time
        if (periodStart == null || periodEnd == null) {
            throw new IllegalArgumentException(
                    changeIndicator + " forecast must have period start and end times"
            );
        }
        if (changeTime != null) {
            throw new IllegalArgumentException(
                    changeIndicator + " forecast should not have exact change time"
            );
        }
        if (!periodStart.isBefore(periodEnd)) {
            throw new IllegalArgumentException(
                    "Period start (" + periodStart + ") must be before end (" + periodEnd + ")"
            );
        }
        // Check reasonable duration
        long hours = Duration.between(periodStart, periodEnd).toHours();
        if (hours > MAX_PERIOD_HOURS) {
            throw new IllegalArgumentException(
                    "Forecast period too long (" + hours + " hours). Maximum is " + MAX_PERIOD_HOURS
            );
        }
    }

    /**
     * Validate time fields for BASE forecasts.
     */
    private static void validateBaseTimeFields(Instant periodStart, Instant periodEnd) {
        // BASE can have period (overall TAF validity) or not (if using TAF validity)
        if (periodStart != null && periodEnd != null && !periodStart.isBefore(periodEnd)) {
            throw new IllegalArgumentException(
                    "Period start (" + periodStart + ") must be before end (" + periodEnd + ")"
            );
        }
    }

    /**
     * Validate probability field.
     */
    private static void validateProbability(ChangeIndicator changeIndicator, Integer probability) {
        if (changeIndicator == ChangeIndicator.PROB) {
            if (probability == null) {
                throw new IllegalArgumentException("PROB forecast must have probability value");
            }
            boolean valid = false;
            for (int validProb : VALID_PROBABILITIES) {
                if (probability == validProb) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                throw new IllegalArgumentException(
                        "Invalid probability: " + probability + ". Must be 30 or 40"
                );
            }
        } else {
            if (probability != null) {
                throw new IllegalArgumentException(
                        "Only PROB forecasts should have probability. Found " + probability +
                                " for " + changeIndicator
                );
            }
        }
    }

    /**
     * Validate that conditions are present.
     */
    private static void validateConditions(WeatherConditions conditions) {
        if (conditions == null) {
            throw new IllegalArgumentException("Weather conditions cannot be null");
        }
    }

    // ==================== Query Methods ====================

    /**
     * Check if this is the base forecast period.
     *
     * @return true if this is a BASE forecast
     */
    public boolean isBaseForecast() {
        return changeIndicator == ChangeIndicator.BASE;
    }

    /**
     * Check if this is a permanent change (BASE or FM).
     *
     * @return true if permanent
     */
    public boolean isPermanentChange() {
        return changeIndicator.isPermanent();
    }

    /**
     * Check if this is a temporary change (TEMPO).
     *
     * @return true if temporary
     */
    public boolean isTemporaryChange() {
        return changeIndicator.isTemporary();
    }

    /**
     * Check if this is a gradual change (BECMG).
     *
     * @return true if gradual
     */
    public boolean isGradualChange() {
        return changeIndicator.isGradual();
    }

    /**
     * Check if this is a probabilistic forecast (PROB).
     *
     * @return true if probabilistic
     */
    public boolean isProbabilistic() {
        return changeIndicator.isProbabilistic();
    }

    /**
     * Check if this forecast period has a defined time range.
     * FM uses exact time, others use period start/end.
     *
     * @return true if has time range
     */
    public boolean hasTimeRange() {
        return (periodStart != null && periodEnd != null) || changeTime != null;
    }

    /**
     * Get the effective start time of this forecast period.
     * For FM, this is the change time. For others, it's the period start.
     *
     * @return start time, or null if not defined
     */
    public Instant getEffectiveStartTime() {
        if (changeTime != null) {
            return changeTime;
        }
        return periodStart;
    }

    /**
     * Get the effective end time of this forecast period.
     * For FM, there's no defined end (continues until next change).
     * For others, it's the period end.
     *
     * @return end time, or null if not defined (FM forecasts)
     */
    public Instant getEffectiveEndTime() {
        return periodEnd;
    }

    /**
     * Get the duration of this forecast period.
     * Only applicable for periods with start and end times (not FM).
     *
     * @return duration, or null if not a time period
     */
    public Duration getPeriodDuration() {
        if (periodStart == null || periodEnd == null) {
            return null;
        }
        return Duration.between(periodStart, periodEnd);
    }

    /**
     * Get the duration in hours.
     *
     * @return duration in hours, or null if not a time period
     */
    public Long getPeriodDurationHours() {
        Duration duration = getPeriodDuration();
        return duration != null ? duration.toHours() : null;
    }

    /**
     * Check if a given time falls within this forecast period.
     *
     * @param time the time to check
     * @return true if the time is within the period
     */
    public boolean contains(Instant time) {
        if (time == null) {
            return false;
        }

        // FM forecasts start at change time and continue indefinitely
        if (changeIndicator == ChangeIndicator.FM) {
            return changeTime != null && !time.isBefore(changeTime);
        }

        // Period-based forecasts
        if (periodStart != null && periodEnd != null) {
            return !time.isBefore(periodStart) && time.isBefore(periodEnd);
        }

        return false;
    }

    /**
     * Check if this forecast period is currently active.
     *
     * @return true if current time falls within this period
     */
    public boolean isCurrentlyActive() {
        return contains(Instant.now());
    }

    /**
     * Check if significant weather is forecast.
     *
     * @return true if conditions include precipitation, thunderstorms, or low visibility
     */
    public boolean hasSignificantWeather() {
        if (conditions == null) {
            return false;
        }

        return conditions.hasPrecipitation()
                || conditions.hasThunderstorms()
                || conditions.isLikelyIMC();
    }

    // ==================== Conversion Methods ====================

    /**
     * Format this forecast period in TAF notation.
     *
     * Examples:
     * - BASE: "BASE 1520/1624"
     * - FM: "FM152100"
     * - TEMPO: "TEMPO 3003/3011"
     * - BECMG: "BECMG 3108/3110"
     * - PROB: "PROB30 3005/3011"
     *
     * @return TAF formatted string
     */
    public String toTafFormat() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("ddHHmm")
                .withZone(ZoneOffset.UTC);
        DateTimeFormatter periodFormatter = DateTimeFormatter.ofPattern("ddHH")
                .withZone(ZoneOffset.UTC);

        StringBuilder sb = new StringBuilder();

        switch (changeIndicator) {
            case BASE:
                sb.append("BASE");
                if (periodStart != null && periodEnd != null) {
                    sb.append(" ")
                            .append(periodFormatter.format(periodStart))
                            .append("/")
                            .append(periodFormatter.format(periodEnd));
                }
                break;

            case FM:
                sb.append("FM").append(timeFormatter.format(changeTime));
                break;

            case TEMPO:
                sb.append("TEMPO ")
                        .append(periodFormatter.format(periodStart))
                        .append("/")
                        .append(periodFormatter.format(periodEnd));
                break;

            case BECMG:
                sb.append("BECMG ")
                        .append(periodFormatter.format(periodStart))
                        .append("/")
                        .append(periodFormatter.format(periodEnd));
                break;

            case PROB:
                sb.append("PROB").append(probability).append(" ")
                        .append(periodFormatter.format(periodStart))
                        .append("/")
                        .append(periodFormatter.format(periodEnd));
                break;
        }

        return sb.toString();
    }

    /**
     * Get a human-readable summary of this forecast period.
     *
     * @return formatted summary including time and conditions
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();

        sb.append(toTafFormat());

        if (conditions != null && conditions.hasAnyConditions()) {
            sb.append(": ").append(conditions.getSummary());
        }

        return sb.toString();
    }

    // ==================== Factory Methods ====================

    /**
     * Create a BASE forecast period.
     *
     * @param periodStart start of base forecast period
     * @param periodEnd end of base forecast period
     * @param conditions forecast conditions
     * @return new ForecastPeriod
     */
    public static ForecastPeriod base(Instant periodStart, Instant periodEnd, WeatherConditions conditions) {
        return new ForecastPeriod(
                ChangeIndicator.BASE,
                null,
                periodStart,
                periodEnd,
                null,
                conditions
        );
    }

    /**
     * Create an FM (From) forecast period.
     *
     * @param changeTime exact time when change occurs
     * @param conditions forecast conditions
     * @return new ForecastPeriod
     */
    public static ForecastPeriod from(Instant changeTime, WeatherConditions conditions) {
        return new ForecastPeriod(
                ChangeIndicator.FM,
                changeTime,
                null,
                null,
                null,
                conditions
        );
    }

    /**
     * Create a TEMPO (Temporary) forecast period.
     *
     * @param periodStart start of temporary period
     * @param periodEnd end of temporary period
     * @param conditions forecast conditions
     * @return new ForecastPeriod
     */
    public static ForecastPeriod tempo(Instant periodStart, Instant periodEnd, WeatherConditions conditions) {
        return new ForecastPeriod(
                ChangeIndicator.TEMPO,
                null,
                periodStart,
                periodEnd,
                null,
                conditions
        );
    }

    /**
     * Create a BECMG (Becoming) forecast period.
     *
     * @param periodStart start of gradual change period
     * @param periodEnd end of gradual change period
     * @param conditions forecast conditions
     * @return new ForecastPeriod
     */
    public static ForecastPeriod becoming(Instant periodStart, Instant periodEnd, WeatherConditions conditions) {
        return new ForecastPeriod(
                ChangeIndicator.BECMG,
                null,
                periodStart,
                periodEnd,
                null,
                conditions
        );
    }

    /**
     * Create a PROB (Probability) forecast period.
     *
     * @param periodStart start of probability period
     * @param periodEnd end of probability period
     * @param probability probability percentage (30 or 40)
     * @param conditions forecast conditions
     * @return new ForecastPeriod
     */
    public static ForecastPeriod probability(
            Instant periodStart,
            Instant periodEnd,
            int probability,
            WeatherConditions conditions
    ) {
        return new ForecastPeriod(
                ChangeIndicator.PROB,
                null,
                periodStart,
                periodEnd,
                probability,
                conditions
        );
    }

    /**
     * Create a forecast period with all parameters specified.
     * Use this when you need full control over all fields.
     *
     * @param changeIndicator type of change
     * @param changeTime exact change time (FM only)
     * @param periodStart period start time
     * @param periodEnd period end time
     * @param probability probability percentage (PROB only)
     * @param conditions weather conditions
     * @return new ForecastPeriod
     */
    public static ForecastPeriod of(
            ChangeIndicator changeIndicator,
            Instant changeTime,
            Instant periodStart,
            Instant periodEnd,
            Integer probability,
            WeatherConditions conditions
    ) {
        return new ForecastPeriod(
                changeIndicator,
                changeTime,
                periodStart,
                periodEnd,
                probability,
                conditions
        );
    }

    @Override
    public String toString() {
        return String.format("ForecastPeriod[%s]", toTafFormat());
    }
}
