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
package weather.model;

import weather.model.components.Wind;
import weather.model.components.Visibility;
import weather.model.components.PresentWeather;
import weather.model.components.SkyCondition;
import weather.model.components.Temperature;
import weather.model.components.Pressure;

import java.util.List;

import static weather.model.enums.SkyCoverage.*;

/**
 * Universal weather conditions snapshot.
 *
 * Represents a complete set of meteorological observations at a point in time,
 * regardless of data source (NOAA, OpenWeatherMap, etc.) or report type (METAR, TAF).
 *
 * Design Philosophy:
 * - Source-agnostic: Same structure works for all weather data providers
 * - Reusable: Used in both observations (METAR) and forecasts (TAF forecast periods)
 * - Immutable: Once created, conditions don't change
 * - Optional fields: Not all reports contain all elements
 * - Pure universal: No source-specific conveniences (e.g., CAVOK handled in NOAA classes)
 *
 * Think of this as the "common vocabulary" for describing weather conditions.
 * It's like a standardized form that different weather services fill out.
 *
 * Usage Examples:
 * - METAR: Single WeatherConditions representing current conditions
 * - TAF: Multiple WeatherConditions, one per forecast period
 * - OpenWeatherMap: Single WeatherConditions for current weather
 * - 5-day forecast: List of WeatherConditions for each forecast interval
 *
 * Analogy: If WeatherData is the "envelope" (who, what, when, where),
 * then WeatherConditions is the "letter" (the actual weather content).
 *
 * @param wind Wind conditions (direction, speed, gusts, variability)
 * @param visibility Prevailing visibility
 * @param presentWeather List of weather phenomena (rain, snow, fog, etc.)
 * @param skyConditions List of cloud layers (SKC, FEW, SCT, BKN, OVC)
 * @param temperature Temperature information (current or forecast max/min)
 * @param pressure Atmospheric pressure information
 *
 * @author bclasky1539
 *
 */
public record WeatherConditions(
        Wind wind,
        Visibility visibility,
        List<PresentWeather> presentWeather,
        List<SkyCondition> skyConditions,
        Temperature temperature,
        Pressure pressure
) {

    /**
     * Compact constructor with defensive copying and null safety.
     * Ensures lists are immutable and never null.
     */
    public WeatherConditions {
        // Defensive copy of lists to ensure immutability
        presentWeather = presentWeather != null ? List.copyOf(presentWeather) : List.of();
        skyConditions = skyConditions != null ? List.copyOf(skyConditions) : List.of();
    }

    // ==================== Query Methods ====================

    /**
     * Check if any weather conditions are present (not empty).
     *
     * @return true if at least one condition field is populated
     */
    public boolean hasAnyConditions() {
        return wind != null
                || visibility != null
                || !presentWeather.isEmpty()
                || !skyConditions.isEmpty()
                || temperature != null
                || pressure != null;
    }

    /**
     * Check if this represents clear/calm conditions (no significant weather).
     *
     * @return true if conditions are generally clear and calm
     */
    public boolean isClearAndCalm() {
        boolean noWind = wind == null || wind.isCalm();
        boolean goodVisibility = visibility != null && visibility.isVFR();
        boolean noWeather = presentWeather.isEmpty();
        boolean clearSkies = skyConditions.isEmpty() || skyConditions.stream()
                .map(SkyCondition::coverage)
                .allMatch(coverage -> coverage == null ||
                        coverage == SKC ||
                        coverage == CLR ||
                        coverage == FEW);
        return noWind && goodVisibility && noWeather && clearSkies;
    }

    /**
     * Check if conditions include any ceiling (BKN or OVC layers).
     *
     * @return true if there's a ceiling layer
     */
    public boolean hasCeiling() {
        return skyConditions.stream().anyMatch(SkyCondition::isCeiling);
    }

    /**
     * Get the ceiling height in feet (lowest BKN or OVC layer).
     *
     * @return ceiling height in feet, or null if no ceiling
     */
    public Integer getCeilingFeet() {
        return skyConditions.stream()
                .filter(SkyCondition::isCeiling)
                .map(SkyCondition::heightFeet)
                .filter(java.util.Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(null);
    }

    /**
     * Check if precipitation is present.
     *
     * @return true if any present weather includes precipitation
     */
    public boolean hasPrecipitation() {
        return presentWeather.stream()
                .anyMatch(PresentWeather::hasPrecipitation);
    }

    /**
     * Check if convective weather (thunderstorms) is present.
     *
     * @return true if thunderstorms are reported
     */
    public boolean hasThunderstorms() {
        return presentWeather.stream()
                .anyMatch(PresentWeather::isThunderstorm);
    }

    /**
     * Check if freezing conditions exist (based on temperature or weather).
     *
     * @return true if freezing or below, or if freezing weather is present
     */
    public boolean hasFreezingConditions() {
        // Check temperature
        if (temperature != null && temperature.isFreezing()) {
            return true;
        }

        // Check for freezing weather phenomena (FZ prefix)
        return presentWeather.stream()
                .anyMatch(PresentWeather::isFreezing);
    }

    /**
     * Check if instrument meteorological conditions (IMC) are likely.
     * IMC means conditions below VFR minimums (typically ceiling < 1000ft or vis < 3SM).
     *
     * @return true if conditions appear to be IMC
     */
    public boolean isLikelyIMC() {
        // Check visibility
        if (visibility != null && visibility.distanceValue() != null) {
            // Less than 3 statute miles
            if ("SM".equals(visibility.unit()) && visibility.distanceValue() < 3.0) {
                return true;
            }
            // Less than 5 kilometers
            if ("KM".equals(visibility.unit()) && visibility.distanceValue() < 5.0) {
                return true;
            }
        }

        // Check ceiling - less than 1000 feet
        Integer ceiling = getCeilingFeet();
        return ceiling != null && ceiling < 1000;
    }

    /**
     * Check if visual meteorological conditions (VMC) are likely.
     * VMC means conditions at or above VFR minimums.
     *
     * @return true if conditions appear to be VMC
     */
    public boolean isLikelyVMC() {
        return !isLikelyIMC();
    }

    // ==================== Summary Methods ====================

    /**
     * Get a brief human-readable summary of conditions.
     *
     * @return formatted summary string
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();

        if (wind != null) {
            summary.append("Wind: ").append(wind.getSummary()).append("; ");
        }

        if (visibility != null) {
            summary.append("Vis: ").append(visibility.getSummary()).append("; ");
        }

        if (!presentWeather.isEmpty()) {
            summary.append("Weather: ");
            presentWeather.forEach(pw -> summary.append(pw.getDescription()).append(" "));
            summary.append("; ");
        }

        if (!skyConditions.isEmpty()) {
            summary.append("Sky: ");
            skyConditions.forEach(sky -> summary.append(sky.getSummary()).append(" "));
            summary.append("; ");
        }

        if (temperature != null) {
            summary.append("Temp: ").append(temperature.getSummary()).append("; ");
        }

        if (pressure != null) {
            summary.append("Press: ").append(pressure.getSummary()).append("; ");
        }

        // Remove trailing semicolon and space
        if (summary.length() > 2) {
            summary.setLength(summary.length() - 2);
        }

        return !summary.isEmpty() ? summary.toString() : "No conditions reported";
    }

    // ==================== Factory Methods ====================

    /**
     * Create empty conditions (all fields null/empty).
     * Useful as a starting point or placeholder.
     *
     * @return empty WeatherConditions
     */
    public static WeatherConditions empty() {
        return new WeatherConditions(null, null, List.of(), List.of(), null, null);
    }

    /**
     * Create conditions with only wind and visibility (common minimal set).
     *
     * @param wind Wind conditions
     * @param visibility Visibility
     * @return WeatherConditions with wind and visibility
     */
    public static WeatherConditions ofBasic(Wind wind, Visibility visibility) {
        return new WeatherConditions(wind, visibility, List.of(), List.of(), null, null);
    }

    /**
     * Create conditions from individual components (full set).
     *
     * @param wind Wind conditions
     * @param visibility Visibility
     * @param presentWeather List of weather phenomena
     * @param skyConditions List of cloud layers
     * @param temperature Temperature information
     * @param pressure Pressure information
     * @return WeatherConditions with all specified components
     */
    public static WeatherConditions of(
            Wind wind,
            Visibility visibility,
            List<PresentWeather> presentWeather,
            List<SkyCondition> skyConditions,
            Temperature temperature,
            Pressure pressure
    ) {
        return new WeatherConditions(
                wind,
                visibility,
                presentWeather,
                skyConditions,
                temperature,
                pressure
        );
    }

    /**
     * Builder for constructing WeatherConditions incrementally.
     * Useful when parsing reports where conditions are discovered piece by piece.
     *
     * @return new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for incremental construction of WeatherConditions.
     * Follows the builder pattern for flexibility in parsing scenarios.
     */
    public static class Builder {
        private Wind wind;
        private Visibility visibility;
        private List<PresentWeather> presentWeather = List.of();
        private List<SkyCondition> skyConditions = List.of();
        private Temperature temperature;
        private Pressure pressure;

        public Builder wind(Wind wind) {
            this.wind = wind;
            return this;
        }

        public Builder visibility(Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder presentWeather(List<PresentWeather> presentWeather) {
            this.presentWeather = presentWeather;
            return this;
        }

        public Builder skyConditions(List<SkyCondition> skyConditions) {
            this.skyConditions = skyConditions;
            return this;
        }

        public Builder temperature(Temperature temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder pressure(Pressure pressure) {
            this.pressure = pressure;
            return this;
        }

        /**
         * Build the WeatherConditions instance.
         *
         * @return new immutable WeatherConditions
         */
        public WeatherConditions build() {
            return new WeatherConditions(
                    wind,
                    visibility,
                    presentWeather,
                    skyConditions,
                    temperature,
                    pressure
            );
        }
    }
}
