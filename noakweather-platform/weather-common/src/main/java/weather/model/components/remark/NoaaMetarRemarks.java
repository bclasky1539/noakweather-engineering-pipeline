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
import java.util.stream.Collectors;

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
 * Fields are nullable as not all remarks are present in every METAR.
 *
 *  * @param automatedStationType AO1 or AO2 indicator
 *  * @param seaLevelPressure Sea level pressure in hPa (SLP)
 *  * @param hourlyTemperature Hourly temperature and dewpoint (T-group)
 *  * @param peakWind Peak wind information
 *  * @param windShift Wind shift information
 *  * @param variableVisibility Variable visibility data
 *  * @param CeilingSecondSite Ceiling height at second observation site
 *  * @param obscurationLayers Obscuration Layer information
 *  * @param cloudTypes Cloud Type information
 *  * @param towerVisibility Tower visibility (if different from surface)
 *  * @param surfaceVisibility Surface visibility (if different from prevailing)
 *  * @param hourlyPrecipitation Hourly precipitation amount (P)
 *  * @param precipitation3Hour 3-hour precipitation amount
 *  * @param precipitation6Hour 6-hour precipitation amount
 *  * @param precipitation24Hour 24-hour precipitation amount
 *  * @param snowDepth Snow depth on ground
 *  * @param hailSize Hail size in inches
 *  * @param weatherEvents List of weather events (beginning/ending times)
 *  * @param thunderstormLocations List of thunderstorm/cloud locations
 *  * @param pressureTendency 3-hour pressure tendency
 *  * @param sixHourMaxTemperature 6-hour maximum temperature
 *  * @param sixHourMinTemperature 6-hour minimum temperature
 *  * @param twentyFourHourMaxTemperature 24-hour maximum temperature
 *  * @param twentyFourHourMinTemperature 24-hour minimum temperature
 *  * @param automatedMaintenanceIndicators List of Automated Maintenance Indicators
 *  * @param maintenanceRequired Boolean if maintanence is required
 *  * @param freeText Unparsed remarks text
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
        VariableCeiling variableCeiling,
        CeilingSecondSite ceilingSecondSite,
        List<ObscurationLayer> obscurationLayers,
        List<CloudType> cloudTypes,
        Visibility towerVisibility,
        Visibility surfaceVisibility,
        PrecipitationAmount hourlyPrecipitation,
        PrecipitationAmount sixHourPrecipitation,
        PrecipitationAmount twentyFourHourPrecipitation,
        HailSize hailSize,
        List<WeatherEvent> weatherEvents,
        List<ThunderstormLocation> thunderstormLocations,
        PressureTendency pressureTendency,
        Temperature sixHourMaxTemperature,
        Temperature sixHourMinTemperature,
        Temperature twentyFourHourMaxTemperature,
        Temperature twentyFourHourMinTemperature,
        List<AutomatedMaintenanceIndicator> automatedMaintenanceIndicators,
        Boolean maintenanceRequired,
        String freeText
) {

    /**
     * Format string for temperature display in toString().
     * Displays temperature to 1 decimal place with Celsius unit.
     */
    private static final String TEMPERATURE_FORMAT = "%.1f°C";

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
                null, null, null,null, null, List.of(),
                List.of(), null,null, null,null,
                null, null, List.of(), List.of(),null,
                null, null,null,
                null, List.of(),null, null);
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
                && variableCeiling == null
                && ceilingSecondSite == null
                && (obscurationLayers == null || obscurationLayers.isEmpty())
                && (cloudTypes == null || cloudTypes.isEmpty())
                && towerVisibility == null
                && surfaceVisibility == null
                && hourlyPrecipitation == null
                && sixHourPrecipitation == null
                && twentyFourHourPrecipitation == null
                && hailSize == null
                && (weatherEvents == null || weatherEvents.isEmpty())
                && (thunderstormLocations == null || thunderstormLocations.isEmpty())
                && pressureTendency == null
                && sixHourMaxTemperature == null
                && sixHourMinTemperature == null
                && twentyFourHourMaxTemperature == null
                && twentyFourHourMinTemperature == null
                && (automatedMaintenanceIndicators == null || automatedMaintenanceIndicators.isEmpty())
                && maintenanceRequired == null
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
        private VariableCeiling variableCeiling;
        private CeilingSecondSite ceilingSecondSite;
        private List<ObscurationLayer> obscurationLayers = new ArrayList<>();
        private List<CloudType> cloudTypes = new ArrayList<>();
        private Visibility towerVisibility;
        private Visibility surfaceVisibility;
        private PrecipitationAmount hourlyPrecipitation;
        private PrecipitationAmount sixHourPrecipitation;
        private PrecipitationAmount twentyFourHourPrecipitation;
        private HailSize hailSize;
        private List<WeatherEvent> weatherEvents = new ArrayList<>();
        private List<ThunderstormLocation> thunderstormLocations = new ArrayList<>();
        private PressureTendency pressureTendency;
        private Temperature sixHourMaxTemperature;
        private Temperature sixHourMinTemperature;
        private Temperature twentyFourHourMaxTemperature;
        private Temperature twentyFourHourMinTemperature;
        private List<AutomatedMaintenanceIndicator> automatedMaintenanceIndicators = new ArrayList<>();
        private Boolean maintenanceRequired;
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
         * Set variable ceiling.
         *
         * @param variableCeiling variable ceiling observation
         * @return this builder
         */
        public Builder variableCeiling(VariableCeiling variableCeiling) {
            this.variableCeiling = variableCeiling;
            return this;
        }

        /**
         * Set ceiling at second observation site.
         *
         * @param ceilingSecondSite ceiling at second site
         * @return this builder
         */
        public Builder ceilingSecondSite(CeilingSecondSite ceilingSecondSite) {
            this.ceilingSecondSite = ceilingSecondSite;
            return this;
        }

        /**
         * Sets the obscuration layers list.
         *
         * @param obscurationLayers the obscuration layers
         * @return this builder
         */
        public Builder obscurationLayers(List<ObscurationLayer> obscurationLayers) {
            this.obscurationLayers = obscurationLayers != null ? new ArrayList<>(obscurationLayers) : new ArrayList<>();
            return this;
        }

        /**
         * Adds a single obscuration layer.
         *
         * @param layer the obscuration layer to add
         * @return this builder
         */
        public Builder addObscurationLayer(ObscurationLayer layer) {
            if (layer != null) {
                this.obscurationLayers.add(layer);
            }
            return this;
        }

        /**
         * Adds multiple obscuration layers.
         *
         * @param layers the obscuration layers to add
         * @return this builder
         */
        public Builder addObscurationLayers(List<ObscurationLayer> layers) {
            if (layers != null) {
                this.obscurationLayers.addAll(layers);
            }
            return this;
        }

        /**
         * Sets the cloud types list.
         *
         * @param cloudTypes the cloud types
         * @return this builder
         */
        public Builder cloudTypes(List<CloudType> cloudTypes) {
            this.cloudTypes = cloudTypes != null ? new ArrayList<>(cloudTypes) : new ArrayList<>();
            return this;
        }

        /**
         * Adds a single cloud type.
         *
         * @param cloudType the cloud type to add
         * @return this builder
         */
        public Builder addCloudType(CloudType cloudType) {
            if (cloudType != null) {
                this.cloudTypes.add(cloudType);
            }
            return this;
        }

        /**
         * Adds multiple cloud types.
         *
         * @param types the cloud types to add
         * @return this builder
         */
        public Builder addCloudTypes(List<CloudType> types) {
            if (types != null) {
                this.cloudTypes.addAll(types);
            }
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
         * Sets the weather events list.
         *
         * @param weatherEvents the weather events
         * @return this builder
         */
        public Builder weatherEvents(List<WeatherEvent> weatherEvents) {
            this.weatherEvents = weatherEvents != null ? new ArrayList<>(weatherEvents) : new ArrayList<>();
            return this;
        }

        /**
         * Adds a single weather event.
         *
         * @param weatherEvent the weather event to add
         * @return this builder
         */
        public Builder addWeatherEvent(WeatherEvent weatherEvent) {
            if (weatherEvent != null) {
                this.weatherEvents.add(weatherEvent);
            }
            return this;
        }

        /**
         * Adds multiple weather events.
         *
         * @param events the weather events to add
         * @return this builder
         */
        public Builder addWeatherEvents(List<WeatherEvent> events) {
            if (events != null) {
                this.weatherEvents.addAll(events);
            }
            return this;
        }

        /**
         * Sets the weather events list.
         *
         * @param location the thunderstorm location
         * @return this builder
         */
        public Builder addThunderstormLocation(ThunderstormLocation location) {
            this.thunderstormLocations.add(location);
            return this;
        }

        /**
         * Adds multiple weather events.
         *
         * @param locations the thunderstorm locations
         * @return this builder
         */
        public Builder thunderstormLocations(List<ThunderstormLocation> locations) {
            this.thunderstormLocations = locations != null ? new ArrayList<>(locations) : new ArrayList<>();
            return this;
        }

        /**
         * Set 3-hour pressure tendency.
         *
         * @param pressureTendency the pressure tendency
         * @return this builder
         */
        public Builder pressureTendency(PressureTendency pressureTendency) {
            this.pressureTendency = pressureTendency;
            return this;
        }

        /**
         * Set 6-hour maximum temperature.
         *
         * @param sixHourMaxTemperature maximum temperature in 6-hour period
         * @return this builder
         */
        public Builder sixHourMaxTemperature(Temperature sixHourMaxTemperature) {
            this.sixHourMaxTemperature = sixHourMaxTemperature;
            return this;
        }

        /**
         * Set 6-hour minimum temperature.
         *
         * @param sixHourMinTemperature minimum temperature in 6-hour period
         * @return this builder
         */
        public Builder sixHourMinTemperature(Temperature sixHourMinTemperature) {
            this.sixHourMinTemperature = sixHourMinTemperature;
            return this;
        }

        /**
         * Set 24-hour maximum temperature.
         *
         * @param twentyFourHourMaxTemperature maximum temperature in 24-hour period
         * @return this builder
         */
        public Builder twentyFourHourMaxTemperature(Temperature twentyFourHourMaxTemperature) {
            this.twentyFourHourMaxTemperature = twentyFourHourMaxTemperature;
            return this;
        }

        /**
         * Set 24-hour minimum temperature.
         *
         * @param twentyFourHourMinTemperature minimum temperature in 24-hour period
         * @return this builder
         */
        public Builder twentyFourHourMinTemperature(Temperature twentyFourHourMinTemperature) {
            this.twentyFourHourMinTemperature = twentyFourHourMinTemperature;
            return this;
        }

        /**
         * Sets the automated maintenance indicators list.
         *
         * @param automatedMaintenanceIndicators the automated maintenance indicators
         * @return this builder
         */
        public Builder automatedMaintenanceIndicators(List<AutomatedMaintenanceIndicator> automatedMaintenanceIndicators) {
            this.automatedMaintenanceIndicators = automatedMaintenanceIndicators != null ?
                    new ArrayList<>(automatedMaintenanceIndicators) : new ArrayList<>();
            return this;
        }

        /**
         * Adds a single automated maintenance indicator.
         *
         * @param indicator the maintenance indicator to add
         * @return this builder
         */
        public Builder addAutomatedMaintenanceIndicator(AutomatedMaintenanceIndicator indicator) {
            if (indicator != null) {
                this.automatedMaintenanceIndicators.add(indicator);
            }
            return this;
        }

        /**
         * Sets the maintenance required flag ($ indicator).
         *
         * @param maintenanceRequired true if maintenance is required
         * @return this builder
         */
        public Builder maintenanceRequired(Boolean maintenanceRequired) {
            this.maintenanceRequired = maintenanceRequired;
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
                    variableCeiling,
                    ceilingSecondSite,
                    List.copyOf(obscurationLayers),
                    List.copyOf(cloudTypes),
                    towerVisibility,
                    surfaceVisibility,
                    hourlyPrecipitation,
                    sixHourPrecipitation,
                    twentyFourHourPrecipitation,
                    hailSize,
                    List.copyOf(weatherEvents),
                    List.copyOf(thunderstormLocations),
                    pressureTendency,
                    sixHourMaxTemperature,
                    sixHourMinTemperature,
                    twentyFourHourMaxTemperature,
                    twentyFourHourMinTemperature,
                    List.copyOf(automatedMaintenanceIndicators),
                    maintenanceRequired,
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
        addIfPresent(parts, preciseTemperature, "preciseTemp",
                t -> String.format(TEMPERATURE_FORMAT, t.celsius()));
        addIfPresent(parts, preciseDewpoint, "preciseDewpoint",
                t -> String.format(TEMPERATURE_FORMAT, t.celsius()));
        addIfPresent(parts, peakWind, "peakWind", Object::toString);
        addIfPresent(parts, windShift, "windShift", Object::toString);
        addIfPresent(parts, variableVisibility, "variableVisibility", Object::toString);
        addIfPresent(parts, variableCeiling, "variableCeiling", VariableCeiling::getSummary);
        addIfPresent(parts, ceilingSecondSite, "ceilingSecondSite", CeilingSecondSite::getSummary);
        if (obscurationLayers != null && !obscurationLayers.isEmpty()) {
            parts.add("obscurationLayers=" + obscurationLayers.stream()
                    .map(ObscurationLayer::getSummary)
                    .collect(Collectors.joining("; ")));
        }
        if (cloudTypes != null && !cloudTypes.isEmpty()) {
            parts.add("cloudTypes=" + cloudTypes.stream()
                    .map(CloudType::getSummary)
                    .collect(Collectors.joining("; ")));
        }
        addIfPresent(parts, towerVisibility, "towerVisibility", Visibility::getSummary);
        addIfPresent(parts, surfaceVisibility, "surfaceVisibility", Visibility::getSummary);
        addIfPresent(parts, hourlyPrecipitation, "hourlyPrecip", PrecipitationAmount::getDescription);
        addIfPresent(parts, sixHourPrecipitation, "sixHourPrecip", PrecipitationAmount::getDescription);
        addIfPresent(parts, twentyFourHourPrecipitation, "twentyFourHourPrecip", PrecipitationAmount::getDescription);
        addIfPresent(parts, hailSize, "hailSize", HailSize::getSummary);
        if (!weatherEvents.isEmpty()) {
            parts.add("weatherEvents=" + weatherEvents.stream()
                    .map(WeatherEvent::getSummary)
                    .collect(Collectors.joining("; ")));
        }
        if (!thunderstormLocations.isEmpty()) {
            parts.add("thunderstormLocations=" + thunderstormLocations.stream()
                    .map(ThunderstormLocation::getSummary)
                    .collect(Collectors.joining("; ")));
        }
        addIfPresent(parts, pressureTendency, "pressureTendency", PressureTendency::getSummary);
        addIfPresent(parts, sixHourMaxTemperature, "sixHourMaxTemp",
                t -> String.format(TEMPERATURE_FORMAT, t.celsius()));
        addIfPresent(parts, sixHourMinTemperature, "sixHourMinTemp",
                t -> String.format(TEMPERATURE_FORMAT, t.celsius()));
        addIfPresent(parts, twentyFourHourMaxTemperature, "twentyFourHourMaxTemp",
                t -> String.format(TEMPERATURE_FORMAT, t.celsius()));
        addIfPresent(parts, twentyFourHourMinTemperature, "twentyFourHourMinTemp",
                t -> String.format(TEMPERATURE_FORMAT, t.celsius()));
        if (automatedMaintenanceIndicators != null && !automatedMaintenanceIndicators.isEmpty()) {
            parts.add("automatedMaintenance=" + automatedMaintenanceIndicators.stream()
                    .map(AutomatedMaintenanceIndicator::toString)
                    .collect(Collectors.joining("; ")));
        }
        addIfPresent(parts, maintenanceRequired, "maintenanceRequired", Object::toString);
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

    /**
     * Returns whether maintenance is required for the automated weather station.
     * Returns false if not explicitly set to true.
     *
     * @return true if maintenance is required, false otherwise
     */
    public Boolean maintenanceRequired() {
        return maintenanceRequired != null && maintenanceRequired;
    }
}
