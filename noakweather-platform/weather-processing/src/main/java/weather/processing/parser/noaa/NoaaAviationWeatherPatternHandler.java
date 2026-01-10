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
package weather.processing.parser.noaa;

/**
 * Defines how a regex pattern should be handled during METAR/TAF parsing.
 * 
 * This record associates a pattern with:
 * - The name of the handler method to invoke when the pattern matches
 * - Whether the pattern can match multiple times consecutively (repeating)
 * 
 * Examples:
 * - Wind pattern: non-repeating (appears once in sequence)
 * - Sky condition pattern: repeating (can have multiple cloud layers)
 * - Present weather pattern: repeating (can have multiple phenomena)
 * 
 * @param handlerName The name of the handler method to invoke
 * @param canRepeat Whether this pattern can match multiple times consecutively
 * 
 * 
 * @author bclasky1539
 *
 */
public record NoaaAviationWeatherPatternHandler(String handlerName, boolean canRepeat) {
    
    /**
     * Factory method for non-repeating patterns (most common case).
     * Use this for patterns that should only match once in sequence.
     * 
     * Examples: station ID, wind, visibility, temperature, pressure
     * 
     * @param handlerName The handler method name
     * @return NoaaAviationWeatherPatternHandler configured for single match
     */
    public static NoaaAviationWeatherPatternHandler single(String handlerName) {
        return new NoaaAviationWeatherPatternHandler(handlerName, false);
    }
    
    /**
     * Factory method for repeating patterns.
     * Use this for patterns that can match multiple times consecutively.
     * 
     * Examples: sky conditions (multiple cloud layers), present weather (multiple phenomena),
     * runway visual range (multiple runways)
     * 
     * @param handlerName The handler method name
     * @return NoaaAviationWeatherPatternHandler configured for repeating matches
     */
    public static NoaaAviationWeatherPatternHandler repeating(String handlerName) {
        return new NoaaAviationWeatherPatternHandler(handlerName, true);
    }
}
