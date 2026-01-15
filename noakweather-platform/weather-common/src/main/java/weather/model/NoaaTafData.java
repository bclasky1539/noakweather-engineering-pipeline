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

import weather.model.components.ForecastPeriod;
import weather.model.components.ValidityPeriod;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TAF-specific weather data model.
 * <p>
 * Extends NoaaWeatherData with TAF (Terminal Aerodrome Forecast) fields.
 * Inherits WeatherConditions from parent for the base forecast.
 * TAF is an aviation weather forecast valid for a specific time period (typically 24-30 hours).
 * <p>
 * TAF Structure:
 * - Header: Station, issue time, validity period
 * - Base forecast: Initial conditions for the period (stored in parent's conditions field)
 * - Change groups: FM, TEMPO, BECMG, PROB - variations from base forecast
 * - Optional: Max/min temperature forecasts
 * - Optional: Remarks
 * <p>
 * Example TAF:
 * TAF AMD KCLT 151953Z 1520/1624 VRB02KT P6SM FEW250
 *      FM152100 21005KT P6SM SCT250
 *      TEMPO 3003/3011 P6SM -SHSN BKN040 BKN160
 * <p>
 * Architecture:
 * - Uses ValidityPeriod for overall forecast validity
 * - Base forecast conditions inherited from NoaaWeatherData.conditions
 * - Uses List<ForecastPeriod> for change groups (FM, TEMPO, BECMG, PROB)
 * - Each ForecastPeriod contains its own WeatherConditions
 * - Inherits NOAA-specific fields (reportType, rawText, remarks, etc.)
 *
 * @author bclasky1539
 *
 */
public class NoaaTafData extends NoaaWeatherData {

    /**
     * When this TAF was issued (UTC).
     * This is the time the forecast was generated.
     */
    private Instant issueTime;

    /**
     * Overall validity period for this TAF.
     * Defines the start and end times for the entire forecast.
     * Example: "1520/1624" means valid from 15th at 2000Z to 16th at 2400Z
     */
    private ValidityPeriod validityPeriod;

    /**
     * All forecast periods including BASE and change groups.
     * Ordered chronologically. First element is typically the BASE forecast.
     * <p>
     * Types of periods:
     * - BASE: Initial forecast conditions
     * - FM: Permanent change from exact time
     * - TEMPO: Temporary fluctuations
     * - BECMG: Gradual change over period
     * - PROB: Probabilistic conditions
     */
    private List<ForecastPeriod> forecastPeriods;

    /**
     * Maximum temperature forecast (Celsius).
     * From TAF temperature group: TX15/1518Z
     */
    private Integer maxTemperature;

    /**
     * Time when maximum temperature is forecast to occur.
     */
    private Instant maxTemperatureTime;

    /**
     * Minimum temperature forecast (Celsius).
     * From TAF temperature group: TN05/1510Z
     */
    private Integer minTemperature;

    /**
     * Time when minimum temperature is forecast to occur.
     */
    private Instant minTemperatureTime;

    // ========== CONSTRUCTORS ==========

    public NoaaTafData() {
        super();
        setReportType("TAF");
        this.forecastPeriods = new ArrayList<>();
    }

    public NoaaTafData(String stationId, Instant issueTime) {
        super(stationId, issueTime, "TAF");
        this.issueTime = issueTime;
        this.forecastPeriods = new ArrayList<>();
    }

    public NoaaTafData(String stationId, Instant issueTime, ValidityPeriod validityPeriod) {
        super(stationId, issueTime, "TAF");
        this.issueTime = issueTime;
        this.validityPeriod = validityPeriod;
        this.forecastPeriods = new ArrayList<>();
    }

    // ========== GETTERS AND SETTERS ==========

    public Instant getIssueTime() {
        return issueTime;
    }

    public void setIssueTime(Instant issueTime) {
        this.issueTime = issueTime;
    }

    public ValidityPeriod getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(ValidityPeriod validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    /**
     * Get all forecast periods as an immutable copy.
     *
     * @return immutable list of forecast periods
     */
    public List<ForecastPeriod> getForecastPeriods() {
        return List.copyOf(forecastPeriods);
    }

    public void setForecastPeriods(List<ForecastPeriod> forecastPeriods) {
        this.forecastPeriods = forecastPeriods != null ? new ArrayList<>(forecastPeriods) : new ArrayList<>();
    }

    /**
     * Add a forecast period to the list.
     *
     * @param period the forecast period to add
     */
    public void addForecastPeriod(ForecastPeriod period) {
        if (period != null) {
            this.forecastPeriods.add(period);
        }
    }

    public Integer getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(Integer maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public Instant getMaxTemperatureTime() {
        return maxTemperatureTime;
    }

    public void setMaxTemperatureTime(Instant maxTemperatureTime) {
        this.maxTemperatureTime = maxTemperatureTime;
    }

    public Integer getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(Integer minTemperature) {
        this.minTemperature = minTemperature;
    }

    public Instant getMinTemperatureTime() {
        return minTemperatureTime;
    }

    public void setMinTemperatureTime(Instant minTemperatureTime) {
        this.minTemperatureTime = minTemperatureTime;
    }

    /**
     * Convenience method to set both max temperature and its occurrence time.
     *
     * @param temperature max temperature in Celsius
     * @param time when max temperature occurs
     */
    public void setMaxTemperatureForecast(Integer temperature, Instant time) {
        this.maxTemperature = temperature;
        this.maxTemperatureTime = time;
    }

    /**
     * Convenience method to set both min temperature and its occurrence time.
     *
     * @param temperature min temperature in Celsius
     * @param time when min temperature occurs
     */
    public void setMinTemperatureForecast(Integer temperature, Instant time) {
        this.minTemperature = temperature;
        this.minTemperatureTime = time;
    }

    // ========== QUERY METHODS ==========

    /**
     * Get the base forecast period (the initial conditions).
     *
     * @return the BASE forecast period, or null if not found
     */
    public ForecastPeriod getBaseForecast() {
        return forecastPeriods.stream()
                .filter(ForecastPeriod::isBaseForecast)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all FM (From) change periods.
     *
     * @return list of FM periods
     */
    public List<ForecastPeriod> getFromPeriods() {
        return forecastPeriods.stream()
                .filter(p -> p.changeIndicator() == weather.model.enums.ChangeIndicator.FM)
                .toList();
    }

    /**
     * Get all TEMPO (Temporary) change periods.
     *
     * @return list of TEMPO periods
     */
    public List<ForecastPeriod> getTempoPeriods() {
        return forecastPeriods.stream()
                .filter(p -> p.changeIndicator() == weather.model.enums.ChangeIndicator.TEMPO)
                .toList();
    }

    /**
     * Get all BECMG (Becoming) change periods.
     *
     * @return list of BECMG periods
     */
    public List<ForecastPeriod> getBecomingPeriods() {
        return forecastPeriods.stream()
                .filter(p -> p.changeIndicator() == weather.model.enums.ChangeIndicator.BECMG)
                .toList();
    }

    /**
     * Get all PROB (Probability) change periods.
     *
     * @return list of PROB periods
     */
    public List<ForecastPeriod> getProbabilityPeriods() {
        return forecastPeriods.stream()
                .filter(p -> p.changeIndicator() == weather.model.enums.ChangeIndicator.PROB)
                .toList();
    }

    /**
     * Get the forecast period that is currently active.
     *
     * @return the active forecast period, or null if none active
     */
    public ForecastPeriod getCurrentForecastPeriod() {
        return forecastPeriods.stream()
                .filter(ForecastPeriod::isCurrentlyActive)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get forecast periods active at a specific time.
     * Multiple periods can be active simultaneously (e.g., BASE + TEMPO).
     *
     * @param time the time to check
     * @return list of active periods at the given time
     */
    public List<ForecastPeriod> getForecastPeriodsAt(Instant time) {
        if (time == null) {
            return List.of();
        }

        return forecastPeriods.stream()
                .filter(p -> p.contains(time))
                .toList();
    }

    /**
     * Check if this TAF is currently valid.
     *
     * @return true if current time is within validity period
     */
    public boolean isCurrentlyValid() {
        return validityPeriod != null && validityPeriod.isCurrentlyValid();
    }

    /**
     * Check if this TAF has expired.
     *
     * @return true if validity period has ended
     */
    public boolean hasExpired() {
        return validityPeriod != null && validityPeriod.hasExpired();
    }

    /**
     * Check if significant weather is forecast in any period.
     *
     * @return true if any period forecasts precipitation, storms, or low visibility
     */
    public boolean hasSignificantWeatherForecast() {
        return forecastPeriods.stream()
                .anyMatch(ForecastPeriod::hasSignificantWeather);
    }

    /**
     * Check if this is an amended TAF.
     *
     * @return true if report modifier is AMD
     */
    public boolean isAmended() {
        return "AMD".equals(getReportModifier());
    }

    /**
     * Check if this is a corrected TAF.
     *
     * @return true if report modifier is COR
     */
    public boolean isCorrected() {
        return "COR".equals(getReportModifier());
    }

    /**
     * Get the number of forecast periods (including BASE).
     *
     * @return count of forecast periods
     */
    public int getForecastPeriodCount() {
        return forecastPeriods.size();
    }

    // ========== OVERRIDE METHODS ==========

    @Override
    public boolean isCurrent() {
        return isCurrentlyValid();
    }

    @Override
    public String getDataType() {
        return "TAF";
    }

    @Override
    public String getSummary() {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("TAF");
        if (isAmended()) {
            sb.append(" AMD");
        } else if (isCorrected()) {
            sb.append(" COR");
        }
        sb.append(" ").append(getStationId());

        // Validity
        if (validityPeriod != null) {
            sb.append(" ").append(validityPeriod.toTafFormat());
        }

        // Period count
        sb.append(" (").append(forecastPeriods.size()).append(" periods)");

        // Temperature forecast if present
        if (maxTemperature != null || minTemperature != null) {
            sb.append(" [");
            if (maxTemperature != null) {
                sb.append("TX").append(maxTemperature);
            }
            if (minTemperature != null) {
                if (maxTemperature != null) {
                    sb.append(" ");
                }
                sb.append("TN").append(minTemperature);
            }
            sb.append("]");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format(
                "NoaaTafData{station=%s, issueTime=%s, validity=%s, periods=%d, maxTemp=%s, minTemp=%s}",
                getStationId(),
                issueTime,
                validityPeriod != null ? validityPeriod.toTafFormat() : "null",
                forecastPeriods.size(),
                maxTemperature,
                minTemperature
        );
    }

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NoaaTafData that)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        // Compare NoaaTafData-specific fields
        return Objects.equals(issueTime, that.issueTime) &&
                Objects.equals(validityPeriod, that.validityPeriod) &&
                Objects.equals(forecastPeriods, that.forecastPeriods) &&
                Objects.equals(maxTemperature, that.maxTemperature) &&
                Objects.equals(maxTemperatureTime, that.maxTemperatureTime) &&
                Objects.equals(minTemperature, that.minTemperature) &&
                Objects.equals(minTemperatureTime, that.minTemperatureTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                issueTime,
                validityPeriod,
                forecastPeriods,
                maxTemperature,
                maxTemperatureTime,
                minTemperature,
                minTemperatureTime
        );
    }
}
