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

import java.time.Instant;

/**
 * Immutable value object representing temperature information.
 *
 * Supports both:
 * 1. Current observations (METAR): temperature and dewpoint
 * 2. Forecast extremes (TAF): maximum and minimum temperatures with occurrence times
 *
 * Design Philosophy:
 * - Single unified Temperature class for all temperature data
 * - Fields are optional - use only what's relevant for the report type
 * - METAR uses: celsius, dewpointCelsius
 * - TAF uses: maxCelsius, maxTime, minCelsius, minTime
 *
 * Temperatures are stored in Celsius.
 *
 * Examples:
 * - METAR "22/12" → Temperature(22.0, 12.0, null, null, null, null)
 * - METAR "M05/M12" → Temperature(-5.0, -12.0, null, null, null, null)
 * - TAF "TX15/1518Z TN05/1510Z" → Temperature(null, null, 15.0, instant1518Z, 5.0, instant1510Z)
 *
 * @param celsius Temperature in degrees Celsius (current observation)
 * @param dewpointCelsius Dewpoint temperature in degrees Celsius (current observation)
 * @param maxCelsius Maximum forecast temperature in degrees Celsius (TAF)
 * @param maxTime Time when maximum temperature is forecast to occur (TAF)
 * @param minCelsius Minimum forecast temperature in degrees Celsius (TAF)
 * @param minTime Time when minimum temperature is forecast to occur (TAF)
 *
 * @author bclasky1539
 *
 */
public record Temperature(
        Double celsius,
        Double dewpointCelsius,
        Double maxCelsius,
        Instant maxTime,
        Double minCelsius,
        Instant minTime
) {

    // ==================== Constants ====================

    /** Minimum reasonable temperature in Celsius (Antarctic record: -89.2°C) */
    public static final double MIN_TEMPERATURE_CELSIUS = -100.0;

    /** Maximum reasonable temperature in Celsius (Death Valley record: 56.7°C) */
    public static final double MAX_TEMPERATURE_CELSIUS = 60.0;

    /** Minimum reasonable dewpoint in Celsius */
    public static final double MIN_DEWPOINT_CELSIUS = -100.0;

    /** Maximum reasonable dewpoint in Celsius */
    public static final double MAX_DEWPOINT_CELSIUS = 60.0;

    /** Fog likely threshold - temperature-dewpoint spread in °C */
    public static final double FOG_THRESHOLD_CELSIUS = 2.0;

    /** Freezing point in Celsius */
    public static final double FREEZING_POINT_CELSIUS = 0.0;

    /** Freezing point in Fahrenheit */
    public static final double FREEZING_POINT_FAHRENHEIT = 32.0;

    /** Fahrenheit to Celsius conversion factor (5/9) */
    public static final double FAHRENHEIT_TO_CELSIUS_FACTOR = 5.0 / 9.0;

    /** Celsius to Fahrenheit conversion factor (9/5) */
    public static final double CELSIUS_TO_FAHRENHEIT_FACTOR = 9.0 / 5.0;

    // Constants for August-Roche-Magnus approximation (WMO recommended)
    /** Magnus formula constant a */
    private static final double MAGNUS_A = 17.67;

    /** Magnus formula constant b in °C */
    private static final double MAGNUS_B = 243.5;

    /** Base saturation vapor pressure in hPa */
    private static final double MAGNUS_E0 = 6.112;

    /**
     * Compact constructor with validation.
     * Validates all temperature fields and ensures logical consistency.
     */
    public Temperature {
        // Validate current observation temperatures
        validateTemperature(celsius, "Temperature");
        validateDewpoint(dewpointCelsius);
        validateDewpointNotHigherThanTemp(celsius, dewpointCelsius);

        // Validate forecast temperatures
        validateTemperature(maxCelsius, "Max temperature");
        validateTemperature(minCelsius, "Min temperature");
        validateMinNotHigherThanMax(maxCelsius, minCelsius);

        // Validate that max/min have corresponding times if present
        validateForecastTempHasTime(maxCelsius, maxTime, "max");
        validateForecastTempHasTime(minCelsius, minTime, "min");
    }

    // ==================== Validation Helper Methods ====================

    /**
     * Validate that temperature is within reasonable range.
     */
    private static void validateTemperature(Double temp, String fieldName) {
        if (temp != null && (temp < MIN_TEMPERATURE_CELSIUS || temp > MAX_TEMPERATURE_CELSIUS)) {
            throw new IllegalArgumentException(
                    fieldName + " out of reasonable range (" + MIN_TEMPERATURE_CELSIUS +
                            " to " + MAX_TEMPERATURE_CELSIUS + "°C): " + temp
            );
        }
    }

    /**
     * Validate that dewpoint is within reasonable range.
     */
    private static void validateDewpoint(Double dewpoint) {
        if (dewpoint != null && (dewpoint < MIN_DEWPOINT_CELSIUS || dewpoint > MAX_DEWPOINT_CELSIUS)) {
            throw new IllegalArgumentException(
                    "Dewpoint out of reasonable range (" + MIN_DEWPOINT_CELSIUS +
                            " to " + MAX_DEWPOINT_CELSIUS + "°C): " + dewpoint
            );
        }
    }

    /**
     * Validate that dewpoint is not higher than temperature.
     * This is a physical impossibility under normal atmospheric conditions.
     */
    private static void validateDewpointNotHigherThanTemp(Double temp, Double dewpoint) {
        if (temp != null && dewpoint != null && dewpoint > temp) {
            throw new IllegalArgumentException(
                    "Dewpoint (" + dewpoint + "°C) cannot be higher than temperature (" + temp + "°C)"
            );
        }
    }

    /**
     * Validate that minimum temperature is not higher than maximum.
     */
    private static void validateMinNotHigherThanMax(Double max, Double min) {
        if (max != null && min != null && min > max) {
            throw new IllegalArgumentException(
                    "Minimum temperature (" + min + "°C) cannot be higher than maximum (" + max + "°C)"
            );
        }
    }

    /**
     * Validate that forecast temperatures have corresponding occurrence times.
     */
    private static void validateForecastTempHasTime(Double temp, Instant time, String type) {
        if (temp != null && time == null) {
            throw new IllegalArgumentException(
                    "Forecast " + type + " temperature (" + temp + "°C) must have corresponding occurrence time"
            );
        }
        if (temp == null && time != null) {
            throw new IllegalArgumentException(
                    "Forecast " + type + " occurrence time specified without temperature value"
            );
        }
    }

    // ==================== Type Check Methods ====================

    /**
     * Check if this is a current observation (METAR-style).
     *
     * @return true if contains current temperature/dewpoint data
     */
    public boolean isCurrentObservation() {
        return celsius != null;
    }

    /**
     * Check if this is a forecast (TAF-style).
     *
     * @return true if contains max/min forecast data
     */
    public boolean isForecast() {
        return maxCelsius != null || minCelsius != null;
    }

    // ==================== Conversion Methods ====================

    /**
     * Convert temperature to Fahrenheit.
     *
     * @return temperature in Fahrenheit, or null if temperature is null
     */
    public Double toFahrenheit() {
        if (celsius == null) {
            return null;
        }
        return celsius * CELSIUS_TO_FAHRENHEIT_FACTOR + FREEZING_POINT_FAHRENHEIT;
    }

    /**
     * Convert dewpoint to Fahrenheit.
     *
     * @return dewpoint in Fahrenheit, or null if dewpoint is null
     */
    public Double dewpointToFahrenheit() {
        if (dewpointCelsius == null) {
            return null;
        }
        return dewpointCelsius * CELSIUS_TO_FAHRENHEIT_FACTOR + FREEZING_POINT_FAHRENHEIT;
    }

    /**
     * Convert max temperature to Fahrenheit.
     *
     * @return max temperature in Fahrenheit, or null if max temperature is null
     */
    public Double maxToFahrenheit() {
        if (maxCelsius == null) {
            return null;
        }
        return maxCelsius * CELSIUS_TO_FAHRENHEIT_FACTOR + FREEZING_POINT_FAHRENHEIT;
    }

    /**
     * Convert min temperature to Fahrenheit.
     *
     * @return min temperature in Fahrenheit, or null if min temperature is null
     */
    public Double minToFahrenheit() {
        if (minCelsius == null) {
            return null;
        }
        return minCelsius * CELSIUS_TO_FAHRENHEIT_FACTOR + FREEZING_POINT_FAHRENHEIT;
    }

    /**
     * Convert temperature to Kelvin.
     *
     * @return temperature in Kelvin, or null if temperature is null
     */
    public Double toKelvin() {
        if (celsius == null) {
            return null;
        }
        return celsius + 273.15;
    }

    /**
     * Convert dewpoint to Kelvin.
     *
     * @return dewpoint in Kelvin, or null if dewpoint is null
     */
    public Double dewpointToKelvin() {
        if (dewpointCelsius == null) {
            return null;
        }
        return dewpointCelsius + 273.15;
    }

    // ==================== Query Methods ====================

    /**
     * Check if temperature is at or below freezing.
     *
     * @return true if temperature is at or below 0°C
     */
    public boolean isFreezing() {
        return celsius != null && celsius <= FREEZING_POINT_CELSIUS;
    }

    /**
     * Check if temperature is below freezing.
     *
     * @return true if temperature is below 0°C
     */
    public boolean isBelowFreezing() {
        return celsius != null && celsius < FREEZING_POINT_CELSIUS;
    }

    /**
     * Check if temperature is at or above freezing.
     *
     * @return true if temperature is at or above 0°C
     */
    public boolean isAboveFreezing() {
        return celsius != null && celsius > FREEZING_POINT_CELSIUS;
    }

    /**
     * Check if conditions are very cold (temperature < -20°C).
     *
     * @return true if temperature is very cold
     */
    public boolean isVeryCold() {
        return celsius != null && celsius < -20.0;
    }

    /**
     * Check if conditions are very hot (temperature > 35°C).
     *
     * @return true if temperature is very hot
     */
    public boolean isVeryHot() {
        return celsius != null && celsius > 35.0;
    }

    /**
     * Check if forecast max temperature is freezing or below.
     *
     * @return true if max temperature is at or below 0°C
     */
    public boolean isMaxFreezing() {
        return maxCelsius != null && maxCelsius <= FREEZING_POINT_CELSIUS;
    }

    /**
     * Check if forecast min temperature is freezing or below.
     *
     * @return true if min temperature is at or below 0°C
     */
    public boolean isMinFreezing() {
        return minCelsius != null && minCelsius <= FREEZING_POINT_CELSIUS;
    }

    // ==================== Aviation Weather Methods ====================

    /**
     * Calculate relative humidity from temperature and dewpoint.
     * Only applicable for current observations (METAR).
     *
     * Uses the August-Roche-Magnus approximation (also known as the Bolton 1980
     * formula), which is the World Meteorological Organization (WMO) recommended
     * method for calculating saturation vapor pressure over liquid water.
     *
     * Formula: RH = 100 * (e_dewpoint / e_temperature)
     * Where: e(T) = 6.112 * exp((17.67 * T) / (T + 243.5))
     *
     * This formula is accurate to within ±0.06% for temperatures between
     * -40°C and +50°C, covering the typical range of meteorological observations.
     *
     * @return relative humidity as percentage (0-100), or null if dewpoint not available
     */
    @SuppressWarnings("java:S6885")  // Math.clamp not available in Java 17
    public Double getRelativeHumidity() {
        if (celsius == null || dewpointCelsius == null) {
            return null;
        }

        // Calculate saturation vapor pressure at temperature
        double eT = MAGNUS_E0 * Math.exp((MAGNUS_A * celsius) / (celsius + MAGNUS_B));

        // Calculate saturation vapor pressure at dewpoint
        double eTd = MAGNUS_E0 * Math.exp((MAGNUS_A * dewpointCelsius) / (dewpointCelsius + MAGNUS_B));

        // Calculate relative humidity as percentage
        double rh = 100.0 * (eTd / eT);

        // Clamp to reasonable range (0-100)
        // Note: Using Math.max/Math.min for Java 17 compatibility (Math.clamp requires Java 21)
        return Math.max(0.0, Math.min(100.0, rh));
    }

    /**
     * Calculate temperature-dewpoint spread (difference).
     * Used for fog/low cloud prediction - smaller spread means higher probability.
     * Only applicable for current observations (METAR).
     *
     * @return temperature-dewpoint spread in °C, or null if dewpoint not available
     */
    public Double getSpread() {
        if (celsius == null || dewpointCelsius == null) {
            return null;
        }
        return celsius - dewpointCelsius;
    }

    /**
     * Check if conditions are favorable for fog (spread ≤ 2°C).
     * Only applicable for current observations (METAR).
     *
     * @return true if fog is likely based on temperature-dewpoint spread
     */
    public boolean isFogLikely() {
        Double spread = getSpread();
        return spread != null && spread <= FOG_THRESHOLD_CELSIUS;
    }

    /**
     * Check if icing conditions are likely (temperature between 0°C and -20°C with high humidity).
     * Only applicable for current observations (METAR).
     *
     * @return true if icing conditions are likely
     */
    public boolean isIcingLikely() {
        if (celsius == null) {
            return false;
        }

        // Icing typically occurs between 0°C and -20°C
        boolean temperatureRange = celsius <= FREEZING_POINT_CELSIUS && celsius >= -20.0;

        // Check if humidity is high (if available)
        Double rh = getRelativeHumidity();
        boolean highHumidity = rh == null || rh >= 80.0;  // Assume high if not available

        return temperatureRange && highHumidity;
    }

    /**
     * Get heat index (feels-like temperature) using NOAA's official algorithm.
     * Only applicable for current observations (METAR).
     *
     * Implementation follows the National Weather Service Technical Attachment (SR 90-23)
     * by Lans P. Rothfusz, which uses a refined regression equation with adjustments
     * for various temperature and humidity conditions.
     *
     * The algorithm has three calculation paths:
     * 1. Simple formula for HI < 80°F (Steadman approximation)
     * 2. Full Rothfusz regression for HI >= 80°F
     * 3. Adjustments for extreme humidity conditions
     *
     * Valid range: Temperature between 80°F and 112°F with any humidity.
     * Below 80°F, the simple formula is used.
     *
     * Reference: https://www.wpc.ncep.noaa.gov/html/heatindex_equation.shtml
     *
     * @return heat index in Celsius, or null if conditions not met (temp < 27°C / 80°F)
     */
    public Double getHeatIndex() {
        // Heat index is only meaningful for warm temperatures
        if (celsius == null || celsius < 27.0) {  // 27°C ≈ 80.6°F
            return null;
        }

        Double rh = getRelativeHumidity();
        if (rh == null) {
            return null;
        }

        // Convert to Fahrenheit for calculation (NOAA formula uses °F)
        Double tf = toFahrenheit();

        // Add null check for extra safety
        if (tf == null) {
            return null;
        }

        // Step 1: Calculate simple heat index (Steadman)
        double simpleHI = 0.5 * (tf + 61.0 + ((tf - 68.0) * 1.2) + (rh * 0.094));

        // Step 2: Average simple HI with temperature
        double avgHI = (simpleHI + tf) / 2.0;

        // Step 3: If average is less than 80°F, use simple formula
        if (avgHI < 80.0) {
            // Convert back to Celsius
            return (simpleHI - FREEZING_POINT_FAHRENHEIT) * FAHRENHEIT_TO_CELSIUS_FACTOR;
        }

        // Step 4: Use full Rothfusz regression equation
        double hi = calculateRothfuszHeatIndex(tf, rh);

        // Step 5: Apply adjustments for extreme conditions
        hi = applyHeatIndexAdjustments(tf, rh, hi);

        // Convert back to Celsius
        return (hi - FREEZING_POINT_FAHRENHEIT) * FAHRENHEIT_TO_CELSIUS_FACTOR;
    }

    /**
     * Calculate heat index using the Rothfusz regression equation.
     *
     * HI = -42.379 + 2.04901523*T + 10.14333127*RH - 0.22475541*T*RH
     *      - 0.00683783*T*T - 0.05481717*RH*RH + 0.00122874*T*T*RH
     *      + 0.00085282*T*RH*RH - 0.00000199*T*T*RH*RH
     *
     * @param tf Temperature in Fahrenheit
     * @param rh Relative humidity in percent
     * @return Heat index in Fahrenheit
     */
    private static double calculateRothfuszHeatIndex(double tf, double rh) {
        // Rothfusz regression coefficients
        double c1 = -42.379;
        double c2 = 2.04901523;
        double c3 = 10.14333127;
        double c4 = -0.22475541;
        double c5 = -0.00683783;
        double c6 = -0.05481717;
        double c7 = 0.00122874;
        double c8 = 0.00085282;
        double c9 = -0.00000199;

        // Pre-calculate common terms
        double t2 = tf * tf;
        double rh2 = rh * rh;
        double tRh = tf * rh;

        // Full regression equation
        return c1
                + c2 * tf
                + c3 * rh
                + c4 * tRh
                + c5 * t2
                + c6 * rh2
                + c7 * t2 * rh
                + c8 * tf * rh2
                + c9 * t2 * rh2;
    }

    /**
     * Apply NOAA adjustments for extreme humidity conditions.
     *
     * Two adjustments are applied:
     * 1. Low humidity (RH < 13%) and temp 80-112°F: subtract adjustment
     * 2. High humidity (RH > 85%) and temp 80-87°F: add adjustment
     *
     * @param tf Temperature in Fahrenheit
     * @param rh Relative humidity in percent
     * @param hi Calculated heat index before adjustments
     * @return Adjusted heat index in Fahrenheit
     */
    private static double applyHeatIndexAdjustments(double tf, double rh, double hi) {
        double adjustedHI = hi;

        // Adjustment for low humidity (RH < 13% and temp 80-112°F)
        if (rh < 13.0 && tf >= 80.0 && tf <= 112.0) {
            double adjustment = ((13.0 - rh) / 4.0)
                    * Math.sqrt((17.0 - Math.abs(tf - 95.0)) / 17.0);
            adjustedHI -= adjustment;
        }

        // Adjustment for high humidity (RH > 85% and temp 80-87°F)
        if (rh > 85.0 && tf >= 80.0 && tf <= 87.0) {
            double adjustment = ((rh - 85.0) / 10.0) * ((87.0 - tf) / 5.0);
            adjustedHI += adjustment;
        }

        return adjustedHI;
    }

    /**
     * Get human-readable summary of temperature conditions.
     * Adapts output based on whether this is a current observation or forecast.
     *
     * @return formatted temperature summary
     */
    public String getSummary() {
        if (isCurrentObservation()) {
            return getCurrentObservationSummary();
        } else if (isForecast()) {
            return getForecastSummary();
        } else {
            return "No temperature data";
        }
    }

    /**
     * Get summary for current observation (METAR-style).
     */
    private String getCurrentObservationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("%.1f°C (%.1f°F)", celsius, toFahrenheit()));

        if (dewpointCelsius != null) {
            summary.append(String.format(", dewpoint %.1f°C (%.1f°F)",
                    dewpointCelsius, dewpointToFahrenheit()));

            Double rh = getRelativeHumidity();
            if (rh != null) {
                summary.append(String.format(", RH %.0f%%", rh));
            }
        }

        return summary.toString();
    }

    /**
     * Get summary for forecast (TAF-style).
     */
    private String getForecastSummary() {
        StringBuilder summary = new StringBuilder();

        if (maxCelsius != null) {
            summary.append(String.format("Max %.1f°C (%.1f°F) at %s",
                    maxCelsius, maxToFahrenheit(), maxTime));
        }

        if (minCelsius != null) {
            if (!summary.isEmpty()) {
                summary.append(", ");
            }
            summary.append(String.format("Min %.1f°C (%.1f°F) at %s",
                    minCelsius, minToFahrenheit(), minTime));
        }

        return summary.toString();
    }

    // ==================== Factory Methods ====================

    /**
     * Factory method for current observation (METAR).
     *
     * @param celsius Temperature in Celsius
     * @param dewpointCelsius Dewpoint in Celsius
     * @return Temperature instance for current observation
     */
    public static Temperature ofCurrent(Double celsius, Double dewpointCelsius) {
        return new Temperature(celsius, dewpointCelsius, null, null, null, null);
    }

    /**
     * Factory method for forecast max temperature (TAF).
     *
     * @param maxCelsius Maximum temperature in Celsius
     * @param maxTime Time when maximum occurs
     * @return Temperature instance for forecast max
     */
    public static Temperature ofMax(double maxCelsius, Instant maxTime) {
        return new Temperature(null, null, maxCelsius, maxTime, null, null);
    }

    /**
     * Factory method for forecast min temperature (TAF).
     *
     * @param minCelsius Minimum temperature in Celsius
     * @param minTime Time when minimum occurs
     * @return Temperature instance for forecast min
     */
    public static Temperature ofMin(double minCelsius, Instant minTime) {
        return new Temperature(null, null, null, null, minCelsius, minTime);
    }

    /**
     * Factory method for forecast max and min temperatures (TAF).
     *
     * @param maxCelsius Maximum temperature in Celsius
     * @param maxTime Time when maximum occurs
     * @param minCelsius Minimum temperature in Celsius
     * @param minTime Time when minimum occurs
     * @return Temperature instance for forecast max/min
     */
    public static Temperature ofForecast(double maxCelsius, Instant maxTime,
                                         double minCelsius, Instant minTime) {
        return new Temperature(null, null, maxCelsius, maxTime, minCelsius, minTime);
    }

    /**
     * Factory method from Fahrenheit values (current observation).
     *
     * @param fahrenheit Temperature in Fahrenheit
     * @param dewpointFahrenheit Dewpoint in Fahrenheit, null if not reported
     * @return Temperature instance with values converted to Celsius
     */
    public static Temperature fromFahrenheit(double fahrenheit, Double dewpointFahrenheit) {
        double celsius = (fahrenheit - FREEZING_POINT_FAHRENHEIT) * FAHRENHEIT_TO_CELSIUS_FACTOR;
        Double dewpointCelsius = dewpointFahrenheit != null
                ? (dewpointFahrenheit - FREEZING_POINT_FAHRENHEIT) * FAHRENHEIT_TO_CELSIUS_FACTOR
                : null;
        return new Temperature(celsius, dewpointCelsius, null, null, null, null);
    }

    /**
     * Factory method from Kelvin values (current observation).
     *
     * @param kelvin Temperature in Kelvin
     * @param dewpointKelvin Dewpoint in Kelvin, null if not reported
     * @return Temperature instance with values converted to Celsius
     */
    public static Temperature fromKelvin(double kelvin, Double dewpointKelvin) {
        double celsius = kelvin - 273.15;
        Double dewpointCelsius = dewpointKelvin != null ? dewpointKelvin - 273.15 : null;
        return new Temperature(celsius, dewpointCelsius, null, null, null, null);
    }

    /**
     * Factory method for temperature only (no dewpoint) - current observation.
     *
     * @param celsius Temperature in Celsius
     * @return Temperature instance with null dewpoint
     */
    public static Temperature of(double celsius) {
        return new Temperature(celsius, null, null, null, null, null);
    }

    /**
     * Factory method for temperature and dewpoint - current observation.
     *
     * @param celsius Temperature in Celsius
     * @param dewpointCelsius Dewpoint in Celsius
     * @return Temperature instance
     */
    public static Temperature of(double celsius, double dewpointCelsius) {
        return new Temperature(celsius, dewpointCelsius, null, null, null, null);
    }
}
