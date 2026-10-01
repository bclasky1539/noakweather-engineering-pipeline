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
package weather.model.components.remark;

import weather.model.components.Pressure;
import weather.model.components.Temperature;
import weather.model.components.remark.ceilingremarks.CeilingRemarks;
import weather.model.components.remark.ceilingremarks.CeilingSecondSite;
import weather.model.components.remark.ceilingremarks.VariableCeiling;
import weather.model.components.remark.maintenanceremarks.AutomatedMaintenanceIndicator;
import weather.model.components.remark.maintenanceremarks.MaintenanceRemarks;
import weather.model.components.remark.pressureremarks.PressureRapidChange;
import weather.model.components.remark.pressureremarks.PressureRemarks;
import weather.model.components.remark.pressureremarks.PressureTendency;
import weather.model.components.remark.visibilityremarks.VariableVisibility;
import weather.model.components.remark.visibilityremarks.VisibilityRemarks;
import weather.model.components.remark.windremarks.PeakWind;
import weather.model.components.remark.windremarks.WindAtLocation;
import weather.model.components.remark.windremarks.WindRemarks;
import weather.model.components.remark.windremarks.WindShift;
import weather.model.components.Visibility;
import weather.model.enums.AutomatedStationType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Contains remarks (supplemental information) from a METAR or SPECI report.
 * <p>
 * Remarks appear after the "RMK" delimiter in METAR/SPECI reports and provide
 * additional meteorological information beyond the main observation body. All fields
 * are optional as remarks content varies by station and conditions.
 * <p>
 * This is an immutable record that uses the Builder pattern for construction
 * since all fields are optional.
 * <p>
 * Several related fields are grouped into sub-records (WindRemarks,
 * VisibilityRemarks, CeilingRemarks, PressureRemarks, MaintenanceRemarks)
 * to keep this class's distinct class-dependency count within
 * SonarCloud's Monster Class (java:S6539) threshold. The public API is
 * unchanged: every originally flat field is still accessible via the
 * same-named accessor and builder setter, which delegate to the
 * appropriate sub-record internally.
 * <p>
 * * @param automatedStationType AO1 or AO2 indicator
 * * @param seaLevelPressure Sea level pressure in hPa (SLP)
 * * @param hourlyTemperature Hourly temperature and dewpoint (T-group)
 * * @param wind Wind-related remarks (peak wind, wind shift, winds at location)
 * * @param directionalWeather Directional Weather
 * * @param visibility Visibility-related remarks (tower, surface, variable)
 * * @param ceiling Ceiling-related remarks (variable ceiling, second site)
 * * @param obscurationLayers Obscuration Layer information
 * * @param cloudTypes Cloud Type information
 * * @param hourlyPrecipitation Hourly precipitation amount (P)
 * * @param ppGroupValue Raw value from "PP" precipitation-amount group; unit/scale
 * *                      and time period unconfirmed, captured as-is pending
 * *                      further research
 * * @param precipitation6Hour 6-hour precipitation amount
 * * @param precipitation24Hour 24-hour precipitation amount
 * * @param hailSize Hail size in inches
 * * @param weatherEvents List of weather events (beginning/ending times)
 * * @param thunderstormLocations List of thunderstorm/cloud locations
 * * @param pressure Pressure-anomaly remarks (3-hour tendency, rapid change)
 * * @param icing Icing information
 * * @param secondaryAltimeter Secondary altimeter reading
 * * @param sixHourMaxTemperature 6-hour maximum temperature
 * * @param sixHourMinTemperature 6-hour minimum temperature
 * * @param twentyFourHourMaxTemperature 24-hour maximum temperature
 * * @param twentyFourHourMinTemperature 24-hour minimum temperature
 * * @param densityAltitudeFeet Density Altitude
 * * @param maintenance Automated-station maintenance remarks
 * * @param observationProgramStatus Canadian MANOBS observation program status (LAST STFD OBS/NEXT)
 * * @param lightningRemarks List of lightning remarks (LTG)
 * * @param predominantCloudTypes Predominant low/middle/high cloud type (8/C_L C_M C_H group)
 * * @param freeText Unparsed remarks text
 *
 * @author bclasky1539
 *
 */
public record NoaaMetarRemarks(
        AutomatedStationType automatedStationType,
        Pressure seaLevelPressure,
        Temperature preciseTemperature,
        Temperature preciseDewpoint,
        WindRemarks wind,
        DirectionalWeather directionalWeather,
        VisibilityRemarks visibility,
        CeilingRemarks ceiling,
        List<ObscurationLayer> obscurationLayers,
        List<CloudType> cloudTypes,
        PrecipitationAmount hourlyPrecipitation,
        Integer ppGroupValue,
        PrecipitationAmount sixHourPrecipitation,
        PrecipitationAmount twentyFourHourPrecipitation,
        HailSize hailSize,
        List<WeatherEvent> weatherEvents,
        List<ThunderstormLocation> thunderstormLocations,
        PressureRemarks pressure,
        Icing icing,
        Pressure secondaryAltimeter,
        Temperature sixHourMaxTemperature,
        Temperature sixHourMinTemperature,
        Temperature twentyFourHourMaxTemperature,
        Temperature twentyFourHourMinTemperature,
        Integer densityAltitudeFeet,
        MaintenanceRemarks maintenance,
        ObservationProgramStatus observationProgramStatus,
        List<LightningRemark> lightningRemarks,
        PredominantCloudTypes predominantCloudTypes,
        String freeText
) {

    /**
     * Format string for temperature display in toString().
     * Displays temperature to 1 decimal place with Celsius unit.
     */
    private static final String TEMPERATURE_FORMAT = "%.1f°C";

    public NoaaMetarRemarks {
        wind = wind == null ? WindRemarks.empty() : wind;
        visibility = visibility == null ? VisibilityRemarks.empty() : visibility;
        ceiling = ceiling == null ? CeilingRemarks.empty() : ceiling;
        pressure = pressure == null ? PressureRemarks.empty() : pressure;
        maintenance = maintenance == null ? MaintenanceRemarks.empty() : maintenance;
        obscurationLayers = obscurationLayers == null ? List.of() : List.copyOf(obscurationLayers);
        cloudTypes = cloudTypes == null ? List.of() : List.copyOf(cloudTypes);
        weatherEvents = weatherEvents == null ? List.of() : List.copyOf(weatherEvents);
        thunderstormLocations = thunderstormLocations == null ? List.of() : List.copyOf(thunderstormLocations);
        lightningRemarks = lightningRemarks == null ? List.of() : List.copyOf(lightningRemarks);
    }

    /**
     * Creates a builder for constructing NoaaMetarRemarks instances.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an empty NoaaMetarRemarks instance with all fields null/empty.
     *
     * @return an empty remarks instance
     */
    public static NoaaMetarRemarks empty() {
        return new NoaaMetarRemarks(null, null, null, null,
                WindRemarks.empty(), null, VisibilityRemarks.empty(), CeilingRemarks.empty(),
                List.of(), List.of(), null, null, null, null, null,
                List.of(), List.of(), PressureRemarks.empty(), null, null,
                null, null, null, null, null,
                MaintenanceRemarks.empty(), null, List.of(), null, null);
    }

    // ==================== Delegating accessors for grouped fields ====================
    // These preserve the original flat public API; the actual storage now
    // lives in the sub-records above.

    /**
     * @return peak wind data from the PK WND group, or null if not reported
     */
    public PeakWind peakWind() {
        return wind.peakWind();
    }

    /**
     * @return wind shift data from the WSHFT group, or null if not reported
     */
    public WindShift windShift() {
        return wind.windShift();
    }

    /**
     * @return winds reported at a specific altitude/runway location; empty if none
     */
    public List<WindAtLocation> windsAtLocation() {
        return wind.windsAtLocation();
    }

    /**
     * @return tower visibility from the TWR VIS group, or null if not reported
     */
    public Visibility towerVisibility() {
        return visibility.towerVisibility();
    }

    /**
     * @return surface visibility from the SFC VIS group, or null if not reported
     */
    public Visibility surfaceVisibility() {
        return visibility.surfaceVisibility();
    }

    /**
     * @return variable visibility from the VIS group, or null if not reported
     */
    public VariableVisibility variableVisibility() {
        return visibility.variableVisibility();
    }

    /**
     * @return variable ceiling observation, or null if not reported
     */
    public VariableCeiling variableCeiling() {
        return ceiling.variableCeiling();
    }

    /**
     * @return ceiling height at a second observation site, or null if not reported
     */
    public CeilingSecondSite ceilingSecondSite() {
        return ceiling.ceilingSecondSite();
    }

    /**
     * @return 3-hour pressure tendency, or null if not reported
     */
    public PressureTendency pressureTendency() {
        return pressure.pressureTendency();
    }

    /**
     * @return rapid pressure change indicator, or null if not reported
     */
    public PressureRapidChange pressureRapidChange() {
        return pressure.pressureRapidChange();
    }

    /**
     * @return list of automated maintenance indicators; empty if none
     */
    public List<AutomatedMaintenanceIndicator> automatedMaintenanceIndicators() {
        return maintenance.automatedMaintenanceIndicators();
    }

    /**
     * Returns whether maintenance is required for the automated weather station.
     * Returns false if not explicitly set to true.
     *
     * @return true if maintenance is required, false otherwise
     */
    public Boolean maintenanceRequired() {
        return maintenance.maintenanceRequired();
    }

    // ==================== isEmpty() ====================

    /**
     * Checks if this remarks object has any content.
     *
     * @return true if all fields are null/empty, false otherwise
     */
    public boolean isEmpty() {
        return isCoreFieldsEmpty()
                && isVisibilityAndCeilingFieldsEmpty()
                && isPrecipitationAndWeatherFieldsEmpty()
                && isPressureAndTemperatureFieldsEmpty()
                && isMaintenanceAndStatusFieldsEmpty();
    }

    private boolean isCoreFieldsEmpty() {
        return automatedStationType == null
                && seaLevelPressure == null
                && preciseTemperature == null
                && preciseDewpoint == null
                && wind.isEmpty()
                && directionalWeather == null;
    }

    private boolean isVisibilityAndCeilingFieldsEmpty() {
        return visibility.isEmpty()
                && ceiling.isEmpty()
                && obscurationLayers.isEmpty()
                && cloudTypes.isEmpty();
    }

    private boolean isPrecipitationAndWeatherFieldsEmpty() {
        return hourlyPrecipitation == null
                && ppGroupValue == null
                && sixHourPrecipitation == null
                && twentyFourHourPrecipitation == null
                && hailSize == null
                && weatherEvents.isEmpty()
                && thunderstormLocations.isEmpty()
                && lightningRemarks.isEmpty();
    }

    private boolean isPressureAndTemperatureFieldsEmpty() {
        return pressure.isEmpty()
                && icing == null
                && secondaryAltimeter == null
                && sixHourMaxTemperature == null
                && sixHourMinTemperature == null
                && twentyFourHourMaxTemperature == null
                && twentyFourHourMinTemperature == null;
    }

    private boolean isMaintenanceAndStatusFieldsEmpty() {
        return densityAltitudeFeet == null
                && maintenance.isEmpty()
                && observationProgramStatus == null
                && predominantCloudTypes == null
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
        WindShift windShift = wind.windShift();
        return windShift != null && windShift.frontalPassage();
    }

    /**
     * Builder for creating NoaaMetarRemarks instances.
     * All fields are optional and default to null. Setters for fields that
     * now live in sub-records (wind, visibility, ceiling, pressure,
     * maintenance) are unchanged from before this class's internal
     * reorganization; the sub-records are assembled internally at build().
     */
    public static class Builder {
        private AutomatedStationType automatedStationType;
        private Pressure seaLevelPressure;
        private Temperature preciseTemperature;
        private Temperature preciseDewpoint;
        private DirectionalWeather directionalWeather;
        private List<ObscurationLayer> obscurationLayers = new ArrayList<>();
        private List<CloudType> cloudTypes = new ArrayList<>();
        private PrecipitationAmount hourlyPrecipitation;
        private Integer ppGroupValue;
        private PrecipitationAmount sixHourPrecipitation;
        private PrecipitationAmount twentyFourHourPrecipitation;
        private HailSize hailSize;
        private List<WeatherEvent> weatherEvents = new ArrayList<>();
        private List<ThunderstormLocation> thunderstormLocations = new ArrayList<>();
        private Icing icing;
        private Pressure secondaryAltimeter;
        private Temperature sixHourMaxTemperature;
        private Temperature sixHourMinTemperature;
        private Temperature twentyFourHourMaxTemperature;
        private Temperature twentyFourHourMinTemperature;
        private Integer densityAltitudeFeet;
        private ObservationProgramStatus observationProgramStatus;
        private List<LightningRemark> lightningRemarks = new ArrayList<>();
        private WindRemarks wind = WindRemarks.empty();
        private VisibilityRemarks visibility = VisibilityRemarks.empty();
        private CeilingRemarks ceiling = CeilingRemarks.empty();
        private PressureRemarks pressure = PressureRemarks.empty();
        private MaintenanceRemarks maintenance = MaintenanceRemarks.empty();
        private PredominantCloudTypes predominantCloudTypes;
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
         * Sets the present-weather phenomenon reported in remarks with optional
         * compass direction(s), typically restating a vicinity or nearby-occurring
         * phenomenon (e.g. VCSH E SE).
         *
         * @param directionalWeather the directional weather remark
         * @return this builder
         */
        public Builder directionalWeather(DirectionalWeather directionalWeather) {
            this.directionalWeather = directionalWeather;
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
         * Sets the raw value from the "PP" precipitation-amount group.
         * Format, unit/scale, and time period are not fully confirmed at this
         * time — the value is captured as-is (raw 3-digit integer) pending
         * further research. Distinct from the standard single-P hourly
         * precipitation group (see PrecipitationAmount).
         *
         * @param ppGroupValue the raw PP group value
         * @return this builder
         */
        public Builder ppGroupValue(Integer ppGroupValue) {
            this.ppGroupValue = ppGroupValue;
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
         * Sets the Density Altitude.
         *
         * @param densityAltitudeFeet the density altitude
         * @return this builder
         */
        public Builder densityAltitudeFeet(Integer densityAltitudeFeet) {
            this.densityAltitudeFeet = densityAltitudeFeet;
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
         * Adds a single lightning remark.
         *
         * @param lightningRemark the lightning remark to add
         * @return this builder
         */
        public Builder addLightningRemark(LightningRemark lightningRemark) {
            if (lightningRemark != null) {
                this.lightningRemarks.add(lightningRemark);
            }
            return this;
        }

        /**
         * Sets the lightning remarks list.
         *
         * @param lightningRemarks the lightning remarks
         * @return this builder
         */
        public Builder lightningRemarks(List<LightningRemark> lightningRemarks) {
            this.lightningRemarks = lightningRemarks != null ? new ArrayList<>(lightningRemarks) : new ArrayList<>();
            return this;
        }

        /**
         * Adds multiple lightning remarks.
         *
         * @param remarks the lightning remarks to add
         * @return this builder
         */
        public Builder addLightningRemarks(List<LightningRemark> remarks) {
            if (remarks != null) {
                this.lightningRemarks.addAll(remarks);
            }
            return this;
        }

        /**
         * Set icing (ICG) remark.
         *
         * @param icing the icing remark
         * @return this builder
         */
        public Builder icing(Icing icing) {
            this.icing = icing;
            return this;
        }

        /**
         * Set Canadian MANOBS observation program status (LAST STFD OBS/NEXT remark).
         *
         * @param observationProgramStatus the observation program status
         * @return this builder
         */
        public Builder observationProgramStatus(ObservationProgramStatus observationProgramStatus) {
            this.observationProgramStatus = observationProgramStatus;
            return this;
        }

        /**
         * Sets the predominant low/middle/high cloud type from the "8/C_L C_M C_H"
         * remark group, per WMO Cloud Atlas coding instructions.
         *
         * @param predominantCloudTypes the predominant cloud types
         * @return this builder
         */
        public Builder predominantCloudTypes(PredominantCloudTypes predominantCloudTypes) {
            this.predominantCloudTypes = predominantCloudTypes;
            return this;
        }

        /**
         * Sets the secondary altimeter setting repeated inside the remarks section.
         * Common in the Philippines/Taiwan-region METARs as a redundant confirmation
         * of the main body's altimeter reading, in US-style inches of mercury.
         *
         * @param secondaryAltimeter the secondary altimeter pressure
         * @return this builder
         */
        public Builder secondaryAltimeter(Pressure secondaryAltimeter) {
            this.secondaryAltimeter = secondaryAltimeter;
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
         * Sets the wind-related remarks (peak wind, wind shift, winds at location)
         * as a single unit, replacing anything set previously.
         *
         * @param wind the wind remarks; null resets to an empty WindRemarks
         * @return this builder
         */
        public Builder wind(WindRemarks wind) {
            this.wind = wind == null ? WindRemarks.empty() : wind;
            return this;
        }

        /**
         * Updates the wind-related remarks in place by applying a function to the
         * current value. Intended for setting one field at a time, e.g.
         * {@code builder.updateWind(w -> w.withPeakWind(peakWind))}.
         *
         * @param updater function that receives the current WindRemarks and returns the replacement;
         *                must not return null
         * @return this builder
         */
        public Builder updateWind(UnaryOperator<WindRemarks> updater) {
            this.wind = updater.apply(this.wind);
            return this;
        }

        /**
         * Sets the visibility-related remarks (tower, surface, variable visibility)
         * as a single unit, replacing anything set previously.
         *
         * @param visibility the visibility remarks; null resets to an empty VisibilityRemarks
         * @return this builder
         */
        public Builder visibility(VisibilityRemarks visibility) {
            this.visibility = visibility == null ? VisibilityRemarks.empty() : visibility;
            return this;
        }

        /**
         * Updates the visibility-related remarks in place by applying a function to
         * the current value, e.g.
         * {@code builder.updateVisibility(v -> v.withTowerVisibility(towerVis))}.
         *
         * @param updater function that receives the current VisibilityRemarks and returns the
         *                replacement; must not return null
         * @return this builder
         */
        public Builder updateVisibility(UnaryOperator<VisibilityRemarks> updater) {
            this.visibility = updater.apply(this.visibility);
            return this;
        }

        /**
         * Sets the ceiling-related remarks (variable ceiling, second-site ceiling)
         * as a single unit, replacing anything set previously.
         *
         * @param ceiling the ceiling remarks; null resets to an empty CeilingRemarks
         * @return this builder
         */
        public Builder ceiling(CeilingRemarks ceiling) {
            this.ceiling = ceiling == null ? CeilingRemarks.empty() : ceiling;
            return this;
        }

        /**
         * Updates the ceiling-related remarks in place by applying a function to the
         * current value, e.g.
         * {@code builder.updateCeiling(c -> c.withVariableCeiling(variableCeiling))}.
         *
         * @param updater function that receives the current CeilingRemarks and returns the
         *                replacement; must not return null
         * @return this builder
         */
        public Builder updateCeiling(UnaryOperator<CeilingRemarks> updater) {
            this.ceiling = updater.apply(this.ceiling);
            return this;
        }

        /**
         * Sets the pressure-anomaly remarks (3-hour tendency, rapid change) as a
         * single unit, replacing anything set previously.
         *
         * @param pressure the pressure remarks; null resets to an empty PressureRemarks
         * @return this builder
         */
        public Builder pressure(PressureRemarks pressure) {
            this.pressure = pressure == null ? PressureRemarks.empty() : pressure;
            return this;
        }

        /**
         * Updates the pressure-anomaly remarks in place by applying a function to the
         * current value, e.g.
         * {@code builder.updatePressure(p -> p.withPressureTendency(tendency))}.
         *
         * @param updater function that receives the current PressureRemarks and returns the
         *                replacement; must not return null
         * @return this builder
         */
        public Builder updatePressure(UnaryOperator<PressureRemarks> updater) {
            this.pressure = updater.apply(this.pressure);
            return this;
        }

        /**
         * Sets the automated-station maintenance remarks (indicators and the
         * maintenance-required flag) as a single unit, replacing anything set previously.
         *
         * @param maintenance the maintenance remarks; null resets to an empty MaintenanceRemarks
         * @return this builder
         */
        public Builder maintenance(MaintenanceRemarks maintenance) {
            this.maintenance = maintenance == null ? MaintenanceRemarks.empty() : maintenance;
            return this;
        }

        /**
         * Updates the maintenance remarks in place by applying a function to the
         * current value, e.g.
         * {@code builder.updateMaintenance(m -> m.withMaintenanceRequired(true))}.
         *
         * @param updater function that receives the current MaintenanceRemarks and returns the
         *                replacement; must not return null
         * @return this builder
         */
        public Builder updateMaintenance(UnaryOperator<MaintenanceRemarks> updater) {
            this.maintenance = updater.apply(this.maintenance);
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
         * Builds the NoaaMetarRemarks instance, assembling the five
         * sub-records (wind, visibility, ceiling, pressure, maintenance)
         * from the flat fields set on this builder.
         *
         * @return a new NoaaMetarRemarks instance
         */
        public NoaaMetarRemarks build() {
            return new NoaaMetarRemarks(
                    automatedStationType, seaLevelPressure, preciseTemperature, preciseDewpoint,
                    wind, directionalWeather, visibility, ceiling,
                    List.copyOf(obscurationLayers), List.copyOf(cloudTypes),
                    hourlyPrecipitation, ppGroupValue, sixHourPrecipitation, twentyFourHourPrecipitation,
                    hailSize, List.copyOf(weatherEvents), List.copyOf(thunderstormLocations),
                    pressure, icing, secondaryAltimeter,
                    sixHourMaxTemperature, sixHourMinTemperature,
                    twentyFourHourMaxTemperature, twentyFourHourMinTemperature,
                    densityAltitudeFeet, maintenance, observationProgramStatus,
                    List.copyOf(lightningRemarks), predominantCloudTypes, freeText);
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
        addIfPresent(parts, wind.peakWind(), "peakWind", Object::toString);
        addIfPresent(parts, wind.windShift(), "windShift", Object::toString);
        if (!wind.windsAtLocation().isEmpty()) {
            parts.add("windsAtLocation=" + wind.windsAtLocation().stream()
                    .map(WindAtLocation::getSummary)
                    .collect(Collectors.joining("; ")));
        }
        addIfPresent(parts, directionalWeather, "directionalWeather", DirectionalWeather::getSummary);
        addIfPresent(parts, visibility.variableVisibility(), "variableVisibility", Object::toString);
        addIfPresent(parts, ceiling.variableCeiling(), "variableCeiling", VariableCeiling::getSummary);
        addIfPresent(parts, ceiling.ceilingSecondSite(), "ceilingSecondSite", CeilingSecondSite::getSummary);
        if (!obscurationLayers.isEmpty()) {
            parts.add("obscurationLayers=" + obscurationLayers.stream()
                    .map(ObscurationLayer::getSummary)
                    .collect(Collectors.joining("; ")));
        }
        if (!cloudTypes.isEmpty()) {
            parts.add("cloudTypes=" + cloudTypes.stream()
                    .map(CloudType::getSummary)
                    .collect(Collectors.joining("; ")));
        }
        addIfPresent(parts, visibility.towerVisibility(), "towerVisibility", Visibility::getSummary);
        addIfPresent(parts, visibility.surfaceVisibility(), "surfaceVisibility", Visibility::getSummary);
        addIfPresent(parts, hourlyPrecipitation, "hourlyPrecip", PrecipitationAmount::getDescription);
        addIfPresent(parts, ppGroupValue, "ppGroupValue", Object::toString);
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
        addIfPresent(parts, pressure.pressureTendency(), "pressureTendency", PressureTendency::getSummary);
        addIfPresent(parts, pressure.pressureRapidChange(), "pressureRapidChange", PressureRapidChange::getSummary);
        addIfPresent(parts, icing, "icing", Icing::getSummary);
        addIfPresent(parts, secondaryAltimeter, "secondaryAltimeter", Pressure::getFormattedValue);
        addIfPresent(parts, sixHourMaxTemperature, "sixHourMaxTemp",
                t -> String.format(TEMPERATURE_FORMAT, t.celsius()));
        addIfPresent(parts, sixHourMinTemperature, "sixHourMinTemp",
                t -> String.format(TEMPERATURE_FORMAT, t.celsius()));
        addIfPresent(parts, twentyFourHourMaxTemperature, "twentyFourHourMaxTemp",
                t -> String.format(TEMPERATURE_FORMAT, t.celsius()));
        addIfPresent(parts, twentyFourHourMinTemperature, "twentyFourHourMinTemp",
                t -> String.format(TEMPERATURE_FORMAT, t.celsius()));
        addIfPresent(parts, densityAltitudeFeet, "densityAltitudeFeet", Object::toString);
        if (!maintenance.automatedMaintenanceIndicators().isEmpty()) {
            parts.add("automatedMaintenance=" + maintenance.automatedMaintenanceIndicators().stream()
                    .map(AutomatedMaintenanceIndicator::toString)
                    .collect(Collectors.joining("; ")));
        }
        addMaintenanceRequiredIfSet(parts);
        addIfPresent(parts, observationProgramStatus, "observationProgramStatus", ObservationProgramStatus::getSummary);
        if (!lightningRemarks.isEmpty()) {
            parts.add("lightningRemarks=" + lightningRemarks.stream()
                    .map(LightningRemark::getSummary)
                    .collect(Collectors.joining("; ")));
        }
        addIfPresent(parts, predominantCloudTypes, "predominantCloudTypes", PredominantCloudTypes::getSummary);
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
     * Helper method to add the maintenanceRequired flag to the toString parts
     * only if it was explicitly set, preserving the original behavior from
     * before the field moved into MaintenanceRemarks.
     */
    private void addMaintenanceRequiredIfSet(List<String> parts) {
        if (maintenance.hasMaintenanceRequired()) {
            parts.add("maintenanceRequired=" + maintenance.maintenanceRequired());
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
