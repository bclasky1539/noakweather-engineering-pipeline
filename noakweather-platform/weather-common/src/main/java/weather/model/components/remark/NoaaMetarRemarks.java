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

import weather.model.components.Pressure;
import weather.model.components.Temperature;
import weather.model.components.Visibility;
import weather.model.enums.AutomatedStationType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Contains remarks (supplemental information) from a METAR or SPECI report.
 *
 * Remarks appear after the "RMK" delimiter in METAR/SPECI reports and provide
 * additional meteorological information beyond the main observation body. All fields
 * are optional as remarks content varies by station and conditions.
 *
 * This is an immutable record that uses the Builder pattern for construction
 * since all fields are optional.
 *
 * @author bclasky1539
 *
 */
public record NoaaMetarRemarks(
        AutomatedStationType automatedStationType,
        Pressure seaLevelPressure,
        Temperature preciseTemperature,
        Temperature preciseDewpoint,
        PeakWind peakWind,
        WindShift windShift,
        VariableVisibility variableVisibility,
        Visibility towerVisibility,
        Visibility surfaceVisibility,
        PrecipitationAmount hourlyPrecipitation,
        PrecipitationAmount sixHourPrecipitation,
        PrecipitationAmount twentyFourHourPrecipitation,
        HailSize hailSize,
        String freeText
) {

    /**
     * Creates a builder for constructing NoaaMetarRemarks instances.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an empty NoaaMetarRemarks instance with all fields null.
     *
     * @return an empty remarks instance
     */
    public static NoaaMetarRemarks empty() {
        return new NoaaMetarRemarks(null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null);
    }

    /**
     * Checks if this remarks object has any content.
     *
     * @return true if all fields are null, false otherwise
     */
    public boolean isEmpty() {
        return automatedStationType == null
                && seaLevelPressure == null
                && preciseTemperature == null
                && preciseDewpoint == null
                && peakWind == null
                && windShift == null
                && variableVisibility == null
                && towerVisibility == null
                && surfaceVisibility == null
                && hourlyPrecipitation == null
                && sixHourPrecipitation == null
                && twentyFourHourPrecipitation == null
                && hailSize == null
                && (freeText == null || freeText.isBlank());
    }

    /**
     * Checks if this station has a precipitation discriminator (AO2).
     *
     * @return true if station type is AO2, false otherwise
     */
    public boolean hasPrecipitationDiscriminator() {
        return automatedStationType != null
                && automatedStationType.hasPrecipitationDiscriminator();
    }

    /**
     * Checks if a frontal passage was reported.
     *
     * @return true if wind shift indicates frontal passage, false otherwise
     */
    public boolean hasFrontalPassage() {
        return windShift != null && windShift.frontalPassage();
    }

    /**
     * Builder for creating NoaaMetarRemarks instances.
     * All fields are optional and default to null.
     */
    public static class Builder {
        private AutomatedStationType automatedStationType;
        private Pressure seaLevelPressure;
        private Temperature preciseTemperature;
        private Temperature preciseDewpoint;
        private PeakWind peakWind;
        private WindShift windShift;
        private VariableVisibility variableVisibility;
        private Visibility towerVisibility;
        private Visibility surfaceVisibility;
        private PrecipitationAmount hourlyPrecipitation;
        private PrecipitationAmount sixHourPrecipitation;
        private PrecipitationAmount twentyFourHourPrecipitation;
        private HailSize hailSize;
        private String freeText;

        private Builder() {
            // Private constructor - use NoaaMetarRemarks.builder()
        }

        /**
         * Sets the automated station type.
         *
         * @param automatedStationType the station type (AO1 or AO2)
         * @return this builder
         */
        public Builder automatedStationType(AutomatedStationType automatedStationType) {
            this.automatedStationType = automatedStationType;
            return this;
        }

        /**
         * Sets the sea level pressure from the SLP group.
         *
         * @param seaLevelPressure the sea level pressure
         * @return this builder
         */
        public Builder seaLevelPressure(Pressure seaLevelPressure) {
            this.seaLevelPressure = seaLevelPressure;
            return this;
        }

        /**
         * Sets the precise temperature from the T group.
         *
         * @param preciseTemperature the precise temperature
         * @return this builder
         */
        public Builder preciseTemperature(Temperature preciseTemperature) {
            this.preciseTemperature = preciseTemperature;
            return this;
        }

        /**
         * Sets the precise dewpoint from the T group.
         *
         * @param preciseDewpoint the precise dewpoint
         * @return this builder
         */
        public Builder preciseDewpoint(Temperature preciseDewpoint) {
            this.preciseDewpoint = preciseDewpoint;
            return this;
        }

        /**
         * Sets the peak wind data.
         *
         * @param peakWind the peak wind data from PK WND group
         * @return this builder
         */
        public Builder peakWind(PeakWind peakWind) {
            this.peakWind = peakWind;
            return this;
        }

        /**
         * Sets the wind shift data.
         *
         * @param windShift the wind shift data from WSHFT group
         * @return this builder
         */
        public Builder windShift(WindShift windShift) {
            this.windShift = windShift;
            return this;
        }

        /**
         * Sets the variable visibility data.
         *
         * @param variableVisibility the variable visibility data from VIS group
         * @return this builder
         */
        public Builder variableVisibility(VariableVisibility variableVisibility) {
            this.variableVisibility = variableVisibility;
            return this;
        }

        /**
         * Sets the tower visibility data.
         *
         * @param towerVisibility the tower visibility from TWR VIS group
         * @return this builder
         */
        public Builder towerVisibility(Visibility towerVisibility) {
            this.towerVisibility = towerVisibility;
            return this;
        }

        /**
         * Sets the surface visibility data.
         *
         * @param surfaceVisibility the surface visibility from SFC VIS group
         * @return this builder
         */
        public Builder surfaceVisibility(Visibility surfaceVisibility) {
            this.surfaceVisibility = surfaceVisibility;
            return this;
        }

        /**
         * Sets the hourly precipitation amount.
         *
         * @param hourlyPrecipitation the hourly precipitation from P group
         * @return this builder
         */
        public Builder hourlyPrecipitation(PrecipitationAmount hourlyPrecipitation) {
            this.hourlyPrecipitation = hourlyPrecipitation;
            return this;
        }

        /**
         * Sets the 6-hour precipitation amount.
         *
         * @param sixHourPrecipitation the 6-hour precipitation from 6 group
         * @return this builder
         */
        public Builder sixHourPrecipitation(PrecipitationAmount sixHourPrecipitation) {
            this.sixHourPrecipitation = sixHourPrecipitation;
            return this;
        }

        /**
         * Sets the 24-hour precipitation amount.
         *
         * @param twentyFourHourPrecipitation the 24-hour precipitation from 7 group
         * @return this builder
         */
        public Builder twentyFourHourPrecipitation(PrecipitationAmount twentyFourHourPrecipitation) {
            this.twentyFourHourPrecipitation = twentyFourHourPrecipitation;
            return this;
        }

        /**
         * Sets the hail size.
         *
         * @param hailSize the hail size from GR group
         * @return this builder
         */
        public Builder hailSize(HailSize hailSize) {
            this.hailSize = hailSize;
            return this;
        }

        /**
         * Sets the free text (unparsed remarks).
         *
         * @param freeText the unparsed remark text
         * @return this builder
         */
        public Builder freeText(String freeText) {
            this.freeText = freeText;
            return this;
        }

        /**
         * Builds the NoaaMetarRemarks instance.
         *
         * @return a new NoaaMetarRemarks instance
         */
        public NoaaMetarRemarks build() {
            return new NoaaMetarRemarks(
                    automatedStationType,
                    seaLevelPressure,
                    preciseTemperature,
                    preciseDewpoint,
                    peakWind,
                    windShift,
                    variableVisibility,
                    towerVisibility,
                    surfaceVisibility,
                    hourlyPrecipitation,
                    sixHourPrecipitation,
                    twentyFourHourPrecipitation,
                    hailSize,
                    freeText
            );
        }
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "NoaaMetarRemarks{empty}";
        }

        List<String> parts = new ArrayList<>();

        addIfPresent(parts, automatedStationType, "stationType", Object::toString);
        addIfPresent(parts, seaLevelPressure, "seaLevelPressure", Pressure::getFormattedValue);
        addIfPresent(parts, preciseTemperature, "preciseTemp", t -> String.format("%.1f°C", t.celsius()));
        addIfPresent(parts, preciseDewpoint, "preciseDewpoint", t -> String.format("%.1f°C", t.celsius()));
        addIfPresent(parts, peakWind, "peakWind", Object::toString);
        addIfPresent(parts, windShift, "windShift", Object::toString);
        addIfPresent(parts, variableVisibility, "variableVisibility", Object::toString);
        addIfPresent(parts, towerVisibility, "towerVisibility", Visibility::getSummary);
        addIfPresent(parts, surfaceVisibility, "surfaceVisibility", Visibility::getSummary);
        addIfPresent(parts, hourlyPrecipitation, "hourlyPrecip", PrecipitationAmount::getDescription);
        addIfPresent(parts, sixHourPrecipitation, "sixHourPrecip", PrecipitationAmount::getDescription);
        addIfPresent(parts, twentyFourHourPrecipitation, "twentyFourHourPrecip", PrecipitationAmount::getDescription);
        addIfPresent(parts, hailSize, "hailSize", HailSize::getSummary);
        addFreeTextIfPresent(parts, freeText);

        return "NoaaMetarRemarks{" + String.join(", ", parts) + "}";
    }

    /**
     * Helper method to add a field to the toString parts if it's present.
     */
    private <T> void addIfPresent(List<String> parts, T value, String name, Function<T, String> formatter) {
        if (value != null) {
            parts.add(name + "=" + formatter.apply(value));
        }
    }

    /**
     * Helper method to add free text field if present and non-blank.
     */
    private void addFreeTextIfPresent(List<String> parts, String text) {
        if (text != null && !text.isBlank()) {
            parts.add("freeText='" + text + "'");
        }
    }
}
