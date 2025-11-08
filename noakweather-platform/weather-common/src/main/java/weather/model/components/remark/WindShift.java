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
 * Immutable value object representing wind shift from remarks section.
 * 
 * Wind shift reports when wind direction changed by 45 degrees or more in less
 * than 15 minutes, with sustained winds of 10 knots or more.
 * 
 * Examples:
 * - "WSHFT 1530" → WindShift(15, 30, false)
 * - "WSHFT 1530 FROPA" → WindShift(15, 30, true)
 * 
 * @param hour Hour of wind shift (UTC)
 * @param minute Minute of wind shift
 * @param frontalPassage True if wind shift was due to frontal passage (FROPA)
 * 
 * @author bclasky1539
 * 
 */
public record WindShift(
    Integer hour,
    Integer minute,
    boolean frontalPassage
) {
    
    /**
     * Compact constructor with validation.
     */
    public WindShift {
        if (hour != null && (hour < 0 || hour > 23)) {
            throw new IllegalArgumentException("Hour must be between 0 and 23: " + hour);
        }
        
        if (minute != null && (minute < 0 || minute > 59)) {
            throw new IllegalArgumentException("Minute must be between 0 and 59: " + minute);
        }
    }
}
