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

/**
 * Immutable value object representing peak wind from remarks section.
 * 
 * Peak wind reports the maximum wind gust direction and speed that occurred
 * during a specific time period.
 * 
 * Example: "PK WND 28032/1530" → PeakWind(280, 32, 15, 30)
 * 
 * @param directionDegrees Peak wind direction in degrees (0-360)
 * @param speedKnots Peak wind speed in knots
 * @param hour Hour of occurrence (UTC)
 * @param minute Minute of occurrence
 * 
 * @author bclasky1539
 * 
 */
public record PeakWind(
    Integer directionDegrees,
    Integer speedKnots,
    Integer hour,
    Integer minute
) {
    
    /**
     * Compact constructor with validation.
     */
    public PeakWind {
        if (directionDegrees != null && (directionDegrees < 0 || directionDegrees > 360)) {
            throw new IllegalArgumentException(
                "Peak wind direction must be between 0 and 360 degrees: " + directionDegrees
            );
        }
        
        if (speedKnots != null && speedKnots < 0) {
            throw new IllegalArgumentException("Peak wind speed cannot be negative: " + speedKnots);
        }
        
        if (hour != null && (hour < 0 || hour > 23)) {
            throw new IllegalArgumentException("Hour must be between 0 and 23: " + hour);
        }
        
        if (minute != null && (minute < 0 || minute > 59)) {
            throw new IllegalArgumentException("Minute must be between 0 and 59: " + minute);
        }
    }
    
    /**
     * Convert peak wind speed to MPH.
     * 
     * @return speed in miles per hour
     */
    public Double toMph() {
        if (speedKnots == null) {
            return null;
        }
        return (double) Math.round(speedKnots * 1.1508);
    }
    
    /**
     * Get wind direction as cardinal direction.
     * 
     * @return cardinal direction (N, NE, E, etc.)
     */
    public String getCardinalDirection() {
        if (directionDegrees == null) {
            return "UNKNOWN";
        }
        
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                               "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int index = (int) Math.round(directionDegrees / 22.5) % 16;
        return directions[index];
    }
}
