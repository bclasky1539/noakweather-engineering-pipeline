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

import com.fasterxml.jackson.annotation.JsonTypeName;
import weather.model.components.*;
import weather.model.components.remark.NoaaMetarRemarks;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Platform-native NOAA weather data implementation.
 * <p>
 * This is the base class for all NOAA weather products (METAR, TAF, PIREP, etc.).
 * It extends the universal WeatherData base with NOAA-specific fields common across
 * all NOAA aviation weather reports.
 * <p>
 * Design Philosophy:
 * - NOAA-specific fields only (not in other weather sources)
 * - Shared across METAR, TAF, and other NOAA products
 * - Weather conditions stored in WeatherConditions object (single source of truth)
 * - Provides convenience getters that delegate to conditions
 * <p>
 * Architecture:
 * - WeatherConditions is the authoritative source for wind, visibility, weather, sky, temp, pressure
 * - All getters delegate to the conditions object
 * - Subclasses (NoaaMetarData, NoaaTafData) add product-specific fields
 * <p>
 * The legacy NoaaAviationWeatherData in noakweather-legacy remains unchanged.
 *
 * @author bclasky1539
 *
 */
@JsonTypeName("NOAA")
public non-sealed class NoaaWeatherData extends WeatherData {

    /**
     * Type of report (METAR, TAF, PIREP, etc.)
     */
    private String reportType;

    /**
     * Current weather conditions (wind, visibility, weather, sky, temperature, pressure).
     * This is the single source of truth for meteorological observations.
     * <p>
     * All NOAA aviation products (METAR, TAF, etc.) use this structure.
     */
    private WeatherConditions conditions;

    /**
     * Runway visual range information (aviation-specific)
     * Not present in general weather APIs, only in NOAA aviation products
     */
    private List<RunwayVisualRange> runwayVisualRange;

    /**
     * Raw text of the weather report as received from NOAA
     */
    private String rawText;

    /**
     * Report modifier (AUTO, COR, AMD, etc.)
     */
    private String reportModifier;

    /**
     * Station latitude in decimal degrees
     */
    private Double latitude;

    /**
     * Station longitude in decimal degrees
     */
    private Double longitude;

    /**
     * Station elevation in feet above mean sea level
     */
    private Integer elevationFeet;

    /**
     * Quality control flags from NOAA
     */
    private String qualityControlFlags;

    /**
     * Remarks section containing supplemental weather information.
     * Shared across NOAA weather products (METAR, SPECI, TAF).
     */
    private NoaaMetarRemarks remarks;

    // ========== CONSTRUCTORS ==========

    public NoaaWeatherData() {
        super();
        this.conditions = WeatherConditions.empty();
        this.runwayVisualRange = new ArrayList<>();
    }

    public NoaaWeatherData(String stationId, Instant observationTime, String reportType) {
        super(WeatherDataSource.NOAA, stationId, observationTime);
        this.reportType = reportType;
        this.conditions = WeatherConditions.empty();
        this.runwayVisualRange = new ArrayList<>();
    }

    // ========== CONDITIONS GETTER/SETTER ==========

    /**
     * Get the complete weather conditions.
     *
     * @return weather conditions
     */
    public WeatherConditions getConditions() {
        return conditions;
    }

    /**
     * Set the complete weather conditions.
     * <p>
     * This is the primary way to set conditions. Parsers should use
     * WeatherConditions.builder() to construct the conditions object,
     * then pass it to this method.
     * <p>
     * Example:
     * <pre>
     * WeatherConditions conditions = WeatherConditions.builder()
     *     .wind(parsedWind)
     *     .visibility(parsedVisibility)
     *     .presentWeather(parsedWeather)
     *     .skyConditions(parsedSkyConditions)
     *     .temperature(parsedTemp)
     *     .pressure(parsedPressure)
     *     .build();
     * weatherData.setConditions(conditions);
     * </pre>
     *
     * @param conditions the weather conditions
     */
    public void setConditions(WeatherConditions conditions) {
        this.conditions = conditions != null ? conditions : WeatherConditions.empty();
    }

    // ========== CONVENIENCE GETTERS (Delegate to Conditions) ==========
    // These provide easy access to individual condition components

    /**
     * Get wind conditions.
     * Convenience method that delegates to conditions.
     *
     * @return wind, or null if no conditions
     */
    public Wind getWind() {
        return conditions != null ? conditions.wind() : null;
    }

    /**
     * Get visibility.
     * Convenience method that delegates to conditions.
     *
     * @return visibility, or null if no conditions
     */
    public Visibility getVisibility() {
        return conditions != null ? conditions.visibility() : null;
    }

    /**
     * Get present weather phenomena.
     * Convenience method that delegates to conditions.
     *
     * @return list of present weather, empty list if no conditions
     */
    public List<PresentWeather> getPresentWeather() {
        return conditions != null && conditions.presentWeather() != null
                ? conditions.presentWeather()
                : List.of();
    }

    /**
     * Get sky conditions (cloud layers).
     * Convenience method that delegates to conditions.
     *
     * @return immutable list of sky conditions, empty list if no conditions
     */
    public List<SkyCondition> getSkyConditions() {
        return conditions != null && conditions.skyConditions() != null
                ? List.copyOf(conditions.skyConditions())
                : List.of();
    }

    /**
     * Get temperature information.
     * Convenience method that delegates to conditions.
     *
     * @return temperature, or null if no conditions
     */
    public Temperature getTemperature() {
        return conditions != null ? conditions.temperature() : null;
    }

    /**
     * Get pressure information.
     * Convenience method that delegates to conditions.
     *
     * @return pressure, or null if no conditions
     */
    public Pressure getPressure() {
        return conditions != null ? conditions.pressure() : null;
    }

    // ========== OTHER GETTERS AND SETTERS ==========

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    /**
     * Get runway visual range as an immutable copy.
     *
     * @return immutable copy of runway visual range list
     */
    public List<RunwayVisualRange> getRunwayVisualRange() {
        return List.copyOf(runwayVisualRange);
    }

    public void setRunwayVisualRange(List<RunwayVisualRange> runwayVisualRange) {
        this.runwayVisualRange = runwayVisualRange != null ? runwayVisualRange : new ArrayList<>();
    }

    public void addRunwayVisualRange(RunwayVisualRange rvr) {
        if (rvr != null) {
            this.runwayVisualRange.add(rvr);
        }
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getReportModifier() {
        return reportModifier;
    }

    public void setReportModifier(String reportModifier) {
        this.reportModifier = reportModifier;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getElevationFeet() {
        return elevationFeet;
    }

    public void setElevationFeet(Integer elevationFeet) {
        this.elevationFeet = elevationFeet;
    }

    public String getQualityControlFlags() {
        return qualityControlFlags;
    }

    public void setQualityControlFlags(String qualityControlFlags) {
        this.qualityControlFlags = qualityControlFlags;
    }

    public NoaaMetarRemarks getRemarks() {
        return remarks;
    }

    public void setRemarks(NoaaMetarRemarks remarks) {
        this.remarks = remarks;
    }

    // ========== NOAA-SPECIFIC CONVENIENCE METHODS ==========

    /**
     * Get the ceiling (lowest BKN or OVC layer).
     *
     * @return ceiling in feet, or null if no ceiling
     */
    public Integer getCeilingFeet() {
        if (conditions == null) {
            return null;
        }
        return conditions.getCeilingFeet();
    }

    /**
     * Check if flight category data is available.
     *
     * @return true if can determine flight category
     */
    public boolean hasFlightCategoryData() {
        if (conditions == null) {
            return false;
        }
        return conditions.visibility() != null &&
                conditions.skyConditions() != null &&
                !conditions.skyConditions().isEmpty();
    }

    /**
     * Get minimum RVR value across all runways (useful for operational decisions).
     *
     * @return minimum RVR in feet, or null if no RVR data
     */
    public Integer getMinimumRvrFeet() {
        if (runwayVisualRange == null || runwayVisualRange.isEmpty()) {
            return null;
        }

        return runwayVisualRange.stream()
                .filter(r -> !r.isLessThan()) // Exclude "less than" values for conservative estimate
                .map(r -> r.isVariable() ? r.variableLow() : r.visualRangeFeet())
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(null);
    }

    /**
     * Get RVR for a specific runway.
     *
     * @param runwayId runway identifier (e.g., "04L", "22R")
     * @return RVR for the runway, or null if not found
     */
    public RunwayVisualRange getRvrForRunway(String runwayId) {
        if (runwayVisualRange == null || runwayId == null) {
            return null;
        }

        return runwayVisualRange.stream()
                .filter(r -> r.runway().equalsIgnoreCase(runwayId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Set visibility to CAVOK (Ceiling And Visibility OK).
     * CAVOK is an ICAO/NOAA aviation-specific term indicating:
     * - Visibility 10km or more
     * - No clouds below 5000ft
     * - No cumulonimbus
     * - No significant weather
     * <p>
     * This is a convenience method for NOAA aviation weather products.
     * It creates a CAVOK visibility condition.
     * <p>
     * Note: This method should be used by subclasses when parsing CAVOK from reports.
     * The actual WeatherConditions with CAVOK visibility should be set in the subclass.
     *
     * @return Visibility object representing CAVOK
     */
    protected Visibility createCavokVisibility() {
        return Visibility.cavok();
    }

    // ========== ABSTRACT METHOD IMPLEMENTATIONS ==========

    @Override
    public boolean isCurrent() {
        if (getObservationTime() == null) {
            return false;
        }

        // NOAA data is current if less than 2 hours old
        Duration age = Duration.between(getObservationTime(), Instant.now());
        return age.toHours() < 2;
    }

    @Override
    public String getDataType() {
        return reportType != null ? reportType : "NOAA";
    }

    @Override
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(reportType != null ? reportType : "NOAA Report");
        sb.append(" from ").append(getStationId());
        sb.append(" at ").append(getObservationTime());

        if (conditions != null) {
            if (conditions.wind() != null) {
                sb.append(" | Wind: ").append(conditions.wind().getCardinalDirection());
            }
            if (conditions.visibility() != null) {
                sb.append(" | Vis: ");
                if (conditions.visibility().isCavok()) {
                    sb.append("CAVOK");
                } else {
                    sb.append(conditions.visibility().distanceValue())
                            .append(conditions.visibility().unit());
                }
            }
            if (conditions.temperature() != null) {
                sb.append(" | Temp: ").append(conditions.temperature().celsius()).append("°C");
            }
        }

        return sb.toString();
    }

    // ========== OBJECT METHODS ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NoaaWeatherData that)) {
            return false;
        }
        if (!Objects.equals(getStationId(), that.getStationId())) {
            return false;
        }
        if (!Objects.equals(getObservationTime(), that.getObservationTime())) {
            return false;
        }

        // Compare NOAA-specific fields
        return Objects.equals(reportType, that.reportType) &&
                Objects.equals(conditions, that.conditions) &&
                Objects.equals(runwayVisualRange, that.runwayVisualRange) &&
                Objects.equals(rawText, that.rawText) &&
                Objects.equals(reportModifier, that.reportModifier) &&
                Objects.equals(remarks, that.remarks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getStationId(),
                getObservationTime(),
                reportType,
                conditions,
                runwayVisualRange,
                rawText,
                reportModifier,
                remarks
        );
    }

    @Override
    public String toString() {
        return String.format(
                "NoaaWeatherData{station=%s, time=%s, type=%s, wind=%s, vis=%s, temp=%s, pressure=%s, skyCond=%d, rvr=%d}",
                getStationId(),
                getObservationTime(),
                reportType,
                getWind(),
                getVisibility(),
                getTemperature(),
                getPressure(),
                getSkyConditions().size(),
                runwayVisualRange.size()
        );
    }
}
