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
package weather.model.components.remark.windremarks;

import weather.exception.InvalidObservationException;
import weather.model.enums.CompassDirection;

/**
 * Immutable value object representing peak wind from remarks section.
 * <p>
 * Peak wind reports the maximum wind gust direction and speed that occurred
 * during a specific time period.
 * <p>
 * Example: "PK WND 28032/1530" → PeakWind(280, 32, 15, 30)
 *
 * @param directionDegrees Peak wind direction in degrees (0-360)
 * @param speedKnots       Peak wind speed in knots
 * @param hour             Hour of occurrence (UTC)
 * @param minute           Minute of occurrence
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
            throw new InvalidObservationException("PeakWind.directionDegrees", String.valueOf(directionDegrees),
                    "Peak wind direction must be between 0 and 360 degrees");
        }

        if (speedKnots != null && speedKnots < 0) {
            throw new InvalidObservationException("PeakWind.speedKnots", String.valueOf(speedKnots),
                    "Peak wind speed cannot be negative");
        }

        if (hour != null && (hour < 0 || hour > 23)) {
            throw new InvalidObservationException("PeakWind.hour", String.valueOf(hour),
                    "Hour must be between 0 and 23");
        }

        if (minute != null && (minute < 0 || minute > 59)) {
            throw new InvalidObservationException("PeakWind.minute", String.valueOf(minute),
                    "Minute must be between 0 and 59");
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
     * Get wind direction as the nearest of the 16 compass points.
     *
     * @return compass point code (N, NNE, NE, etc.), or "UNKNOWN" if direction is null
     * @see CompassDirection#fromDegrees(int)
     */
    public String getCardinalDirection() {
        if (directionDegrees == null) {
            return "UNKNOWN";
        }
        // directionDegrees was validated to 0-360 in the constructor
        return CompassDirection.fromDegrees(directionDegrees).name();
    }
}
