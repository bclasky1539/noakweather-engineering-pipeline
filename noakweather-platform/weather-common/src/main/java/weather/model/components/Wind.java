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

import weather.utils.ValidationPatterns;

/**
 * Immutable value object representing wind conditions.
 * 
 * Wind information includes direction (in degrees), speed, optional gusts,
 * and variability range. This replaces the legacy HashMap-based wind storage.
 * 
 * Examples:
 *   "28016KT" → Wind(280, 16, null, null, null, "KT")
 *   "18016G28KT" → Wind(180, 16, 28, null, null, "KT")
 *   "VRB03KT" → Wind(null, 3, null, null, null, "KT") with variable direction
 *   "28016KT 240V320" → Wind(280, 16, null, 240, 320, "KT")
 * 
 * @param directionDegrees Wind direction in degrees (0-360), null if variable or calm
 * @param speedValue Wind speed value
 * @param gustValue Gust speed value, null if no gusts
 * @param variabilityFrom Lower bound of direction variability, null if not variable
 * @param variabilityTo Upper bound of direction variability, null if not variable
 * @param unit Wind speed unit (KT, MPS, KMH)
 * 
 * @author bclasky1539
 * 
 */
public record Wind(
    Integer directionDegrees,
    Integer speedValue,
    Integer gustValue,
    Integer variabilityFrom,
    Integer variabilityTo,
    String unit
) {
    
    /** Minimum valid wind direction in degrees */
    private static final int MIN_DIRECTION_DEGREES = 0;
    
    /** Maximum valid wind direction in degrees */
    private static final int MAX_DIRECTION_DEGREES = 360;
    
    /** Minimum valid wind speed */
    private static final int MIN_SPEED = 0;
    
    /** Strong wind threshold in knots (22+ KT, Beaufort 6) */
    private static final int STRONG_WIND_THRESHOLD_KT = 22;
    
    /** Gale force threshold in knots (34+ KT, Beaufort 8) */
    private static final int GALE_THRESHOLD_KT = 34;
    
    /** Beaufort scale upper thresholds in knots for scales 1-11 (scale 12 is 64+) */
    private static final int[] BEAUFORT_THRESHOLDS = {3, 6, 10, 16, 21, 27, 33, 40, 47, 55, 63};
    
    /**
     * Compact constructor with validation.
     */
    public Wind {
        validateDirection(directionDegrees);
        validateSpeed(speedValue);
        validateGust(gustValue);
        validateVariability(variabilityFrom, variabilityTo);
        validateUnit(unit);
        validateGustVsSpeed(speedValue, gustValue);
    }
    
    /**
     * Generate a range validation error message.
     * 
     * @param fieldName the name of the field being validated
     * @param min minimum valid value
     * @param max maximum valid value
     * @return formatted error message base
     */
    private static String rangeErrorMessage(String fieldName, int min, int max) {
        return fieldName + " must be between " + min + " and " + max;
    }
    
    /**
     * Validate wind direction is in valid range.
     */
    private static void validateDirection(Integer direction) {
        if (direction != null && (direction < MIN_DIRECTION_DEGREES || direction > MAX_DIRECTION_DEGREES)) {
            throw new IllegalArgumentException(
                rangeErrorMessage("Wind direction", MIN_DIRECTION_DEGREES, MAX_DIRECTION_DEGREES) +
                " degrees, got: " + direction
            );
        }
    }
    
    /**
     * Validate wind speed is non-negative.
     */
    private static void validateSpeed(Integer speed) {
        if (speed != null && speed < MIN_SPEED) {
            throw new IllegalArgumentException("Wind speed cannot be negative: " + speed);
        }
    }
    
    /**
     * Validate gust speed is non-negative.
     */
    private static void validateGust(Integer gust) {
        if (gust != null && gust < MIN_SPEED) {
            throw new IllegalArgumentException("Gust speed cannot be negative: " + gust);
        }
    }
    
    /**
     * Validate that gust speed is greater than sustained wind speed.
     */
    private static void validateGustVsSpeed(Integer speed, Integer gust) {
        if (gust != null && speed != null && gust <= speed) {
            throw new IllegalArgumentException(
                "Gust speed (" + gust + ") must be greater than sustained wind speed (" + speed + ")"
            );
        }
    }
    
    /**
     * Validate variability range.
     */
    private static void validateVariability(Integer from, Integer to) {
        if (from != null || to != null) {
            validateVariabilityPresence(from, to);
            validateVariabilityRange(from, to);
        }
    }
    
    /**
     * Ensure both variability bounds are present together.
     */
    private static void validateVariabilityPresence(Integer from, Integer to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                "Both variabilityFrom and variabilityTo must be provided together"
            );
        }
    }
    
    /**
     * Validate variability bounds are in valid range.
     */
    private static void validateVariabilityRange(Integer from, Integer to) {
        if (from < MIN_DIRECTION_DEGREES || from > MAX_DIRECTION_DEGREES) {
            throw new IllegalArgumentException(
                rangeErrorMessage("Variability from", MIN_DIRECTION_DEGREES, MAX_DIRECTION_DEGREES) +
                ": " + from
            );
        }
        if (to < MIN_DIRECTION_DEGREES || to > MAX_DIRECTION_DEGREES) {
            throw new IllegalArgumentException(
                rangeErrorMessage("Variability to", MIN_DIRECTION_DEGREES, MAX_DIRECTION_DEGREES) +
                ": " + to
            );
        }
    }
    
    /**
     * Validate wind speed unit.
     */
    private static void validateUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("Wind speed unit cannot be null or blank");
        }
        
        String trimmed = unit.trim();
        if (!ValidationPatterns.WIND_UNIT.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                "Invalid wind speed unit: " + unit + " (valid units: KT, MPS, KMH)"
            );
        }
    }
    
    // ==================== Query Methods ====================

    /**
     * Check if wind has a variability range (e.g., 180V240).
     * This indicates wind direction is varying between two compass points.
     *
     * @return true if wind varies between two directions
     */
    public boolean isVariable() {
        return variabilityFrom != null && variabilityTo != null;
    }

    /**
     * Check if wind direction is unpredictable/variable (VRB).
     * Variable direction means the wind is coming from multiple directions
     * with no predictable pattern.
     *
     * @return true if direction is variable (VRB - null direction with speed > 0)
     */
    public boolean hasVariableDirection() {
        return directionDegrees == null && speedValue != null && speedValue > 0;
    }

    /**
     * Check if wind has gusts.
     * 
     * @return true if gusts are present
     */
    public boolean hasGusts() {
        return gustValue != null;
    }
    
    /**
     * Check if wind is calm (no direction reported, typically 0 speed).
     * 
     * @return true if calm conditions
     */
    public boolean isCalm() {
        return directionDegrees == null && (speedValue == null || speedValue == 0);
    }
    
    /**
     * Check if wind speed exceeds strong wind threshold (25+ knots).
     * 
     * @return true if wind is strong
     */
    public boolean isStrongWind() {
        Integer speedKt = getSpeedKnots();
        return speedKt != null && speedKt >= STRONG_WIND_THRESHOLD_KT;
    }
    
    /**
     * Check if wind speed exceeds gale force threshold (34+ knots, Beaufort 8).
     * 
     * @return true if wind is gale force or higher
     */
    public boolean isGale() {
        Integer speedKt = getSpeedKnots();
        return speedKt != null && speedKt >= GALE_THRESHOLD_KT;
    }
    
    // ==================== Conversion Methods ====================
    
    /**
     * Get wind direction as a cardinal direction (N, NE, E, etc.).
     * 
     * @return cardinal direction string, "VRB" if variable, "CALM" if calm
     */
    public String getCardinalDirection() {
        if (isCalm()) {
            return "CALM";
        }
        if (directionDegrees == null) {
            return "VRB";
        }
        
        // Convert degrees to cardinal direction (16 points)
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                               "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int index = (int) Math.round(directionDegrees / 22.5) % 16;
        return directions[index];
    }
    
    /**
     * Get wind speed in knots.
     * 
     * @return speed in knots, or null if speed is null
     */
    public Integer getSpeedKnots() {
        if (speedValue == null) {
            return null;
        }
        
        return switch (unit.toUpperCase()) {
            case "KT" -> speedValue;
            case "MPS" -> (int) Math.round(speedValue * 1.94384);  // m/s to knots
            case "KMH" -> (int) Math.round(speedValue * 0.539957); // km/h to knots
            default -> speedValue; // Should not happen due to validation
        };
    }
    
    /**
     * Get wind speed in meters per second.
     * 
     * @return speed in m/s, or null if speed is null
     */
    public Integer getSpeedMps() {
        if (speedValue == null) {
            return null;
        }
        
        return switch (unit.toUpperCase()) {
            case "KT" -> (int) Math.round(speedValue * 0.514444);  // knots to m/s
            case "MPS" -> speedValue;
            case "KMH" -> (int) Math.round(speedValue * 0.277778); // km/h to m/s
            default -> speedValue; // Should not happen due to validation
        };
    }
    
    /**
     * Get wind speed in kilometers per hour.
     * 
     * @return speed in km/h, or null if speed is null
     */
    public Double getSpeedKmh() {
        if (speedValue == null) {
            return null;
        }
        
        return switch (unit.toUpperCase()) {
            case "KT" -> speedValue * 1.852;      // knots to km/h
            case "MPS" -> speedValue * 3.6;       // m/s to km/h
            case "KMH" -> speedValue.doubleValue();
            default -> speedValue.doubleValue(); // Should not happen due to validation
        };
    }
    
    /**
     * Get Beaufort scale value (0-12) based on wind speed.
     * 
     * Beaufort scale classifies wind speeds:
     *   0 - Calm (0-1 kt)
     *   1 - Light air (1-3 kt)
     *   2 - Light breeze (4-6 kt)
     *   3 - Gentle breeze (7-10 kt)
     *   4 - Moderate breeze (11-16 kt)
     *   5 - Fresh breeze (17-21 kt)
     *   6 - Strong breeze (22-27 kt)
     *   7 - Near gale (28-33 kt)
     *   8 - Gale (34-40 kt)
     *   9 - Strong gale (41-47 kt)
     *   10 - Storm (48-55 kt)
     *   11 - Violent storm (56-63 kt)
     *   12 - Hurricane (64+ kt)
     * 
     * @return Beaufort scale value (0-12)
     */
    public int getBeaufortScale() {
        Integer speedKt = getSpeedKnots();
        if (speedKt == null || speedKt < 1) {
            return 0;
        }
        
        for (int i = 0; i < BEAUFORT_THRESHOLDS.length; i++) {
            if (speedKt <= BEAUFORT_THRESHOLDS[i]) {
                return i + 1;
            }
        }
        
        return 12; // Hurricane force (64+ kt)
    }
    
    /**
     * Get a human-readable summary of the wind conditions.
     * 
     * Examples:
     *   "280° at 16 KT"
     *   "VRB at 3 KT"
     *   "CALM"
     *   "180° at 16 KT gusting 28 KT"
     *   "280° at 16 KT (variable 240°-320°)"
     * 
     * @return formatted string describing the wind conditions
     */
    public String getSummary() {
        if (isCalm()) {
            return "CALM";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Direction
        if (directionDegrees != null) {
            sb.append(directionDegrees).append("°");
        } else {
            sb.append("VRB");
        }
        
        // Speed
        sb.append(" at ").append(speedValue).append(" ").append(unit);
        
        // Gusts
        if (hasGusts()) {
            sb.append(" gusting ").append(gustValue).append(" ").append(unit);
        }
        
        // Variability
        if (isVariable()) {
            sb.append(" (variable ").append(variabilityFrom).append("°-").append(variabilityTo).append("°)");
        }
        
        return sb.toString();
    }
    
    // ==================== Factory Methods ====================
    
    /**
     * Factory method for calm wind conditions.
     * 
     * @return Wind instance representing calm conditions
     */
    public static Wind calm() {
        return new Wind(null, 0, null, null, null, "KT");
    }
    
    /**
     * Factory method for variable wind.
     * 
     * @param speed Wind speed
     * @param unit Speed unit
     * @return Wind instance with variable direction
     */
    public static Wind variable(int speed, String unit) {
        return new Wind(null, speed, null, null, null, unit);
    }
    
    /**
     * Factory method for steady wind without gusts.
     * 
     * @param direction Wind direction in degrees
     * @param speed Wind speed
     * @param unit Speed unit
     * @return Wind instance
     */
    public static Wind of(int direction, int speed, String unit) {
        return new Wind(direction, speed, null, null, null, unit);
    }
    
    /**
     * Factory method for wind with gusts.
     * 
     * @param direction Wind direction in degrees
     * @param speed Wind speed
     * @param gust Gust speed
     * @param unit Speed unit
     * @return Wind instance with gusts
     */
    public static Wind ofWithGusts(int direction, int speed, int gust, String unit) {
        return new Wind(direction, speed, gust, null, null, unit);
    }
}
