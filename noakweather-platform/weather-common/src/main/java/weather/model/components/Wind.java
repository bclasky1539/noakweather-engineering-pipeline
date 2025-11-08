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
package weather.model.components;

/**
 * Immutable value object representing wind conditions.
 * 
 * Wind information includes direction (in degrees), speed, optional gusts,
 * and variability range. This replaces the legacy HashMap-based wind storage.
 * 
 * Examples:
 * - "28016KT" → Wind(280, 16, null, null, null, "KT")
 * - "18016G28KT" → Wind(180, 16, 28, null, null, "KT")
 * - "VRB03KT" → Wind(null, 3, null, null, null, "KT") with variable direction
 * - "28016KT 240V320" → Wind(280, 16, null, 240, 320, "KT")
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
    
    /**
     * Compact constructor with validation.
     */
    public Wind {
        validateDirection(directionDegrees);
        validateSpeed(speedValue);
        validateGust(gustValue);
        validateVariability(variabilityFrom, variabilityTo);
    }
    
    /**
     * Validate wind direction is in valid range.
     */
    private static void validateDirection(Integer direction) {
        if (direction != null && (direction < 0 || direction > 360)) {
            throw new IllegalArgumentException(
                "Wind direction must be between 0 and 360 degrees, got: " + direction
            );
        }
    }
    
    /**
     * Validate wind speed is non-negative.
     */
    private static void validateSpeed(Integer speed) {
        if (speed != null && speed < 0) {
            throw new IllegalArgumentException("Wind speed cannot be negative: " + speed);
        }
    }
    
    /**
     * Validate gust speed is non-negative.
     */
    private static void validateGust(Integer gust) {
        if (gust != null && gust < 0) {
            throw new IllegalArgumentException("Gust speed cannot be negative: " + gust);
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
        if (from < 0 || from > 360) {
            throw new IllegalArgumentException(
                "Variability from must be between 0 and 360: " + from
            );
        }
        if (to < 0 || to > 360) {
            throw new IllegalArgumentException(
                "Variability to must be between 0 and 360: " + to
            );
        }
    }
    
    /**
     * Check if wind direction is variable.
     * 
     * @return true if direction varies (has variability range)
     */
    public boolean isVariable() {
        return variabilityFrom != null && variabilityTo != null;
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
        
        // Convert degrees to cardinal direction
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                               "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int index = (int) Math.round(directionDegrees / 22.5) % 16;
        return directions[index];
    }
    
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
}
