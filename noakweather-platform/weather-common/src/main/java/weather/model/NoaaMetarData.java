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
package weather.model;

import weather.model.components.*;
import weather.model.components.remark.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * METAR-specific weather data model.
 * 
 * Extends NoaaWeatherData with parsed METAR observation fields using
 * modern immutable domain model components (Wind, Visibility, Temperature, etc.).
 * 
 * METAR (Meteorological Aerodrome Report) is an aviation weather observation
 * that describes current conditions at an airport.
 * 
 * @author bclasky1539
 * 
 */
public class NoaaMetarData extends NoaaWeatherData {
    
    // ========== MAIN BODY WEATHER COMPONENTS ==========
    
    /**
     * Wind information (direction, speed, gusts, variability)
     */
    private Wind wind;
    
    /**
     * Visibility information (distance, unit, special conditions)
     */
    private Visibility visibility;
    
    /**
     * Temperature and dewpoint information
     */
    private Temperature temperature;
    
    /**
     * Atmospheric pressure information
     */
    private Pressure pressure;
    
    /**
     * Sky condition layers (cloud coverage, height, type)
     */
    private List<SkyCondition> skyConditions;
    
    /**
     * Present weather phenomena (rain, snow, fog, etc.)
     */
    private List<String> presentWeather;
    
    /**
     * Runway visual range information
     */
    private List<RunwayVisualRange> runwayVisualRange;
    
    // ========== REMARKS SECTION COMPONENTS ==========
    
    /**
     * Peak wind information from remarks
     */
    private PeakWind peakWind;
    
    /**
     * Wind shift information from remarks
     */
    private WindShift windShift;
    
    /**
     * Automated station indicator (AO1, AO2)
     */
    private String automatedStation;
    
    /**
     * Sea level pressure from remarks (in hPa)
     */
    private Double seaLevelPressure;
    
    /**
     * Hourly precipitation amount (in inches)
     */
    private Double hourlyPrecipitation;
    
    /**
     * 6-hour maximum temperature (Celsius)
     */
    private Double sixHourMaxTemp;
    
    /**
     * 6-hour minimum temperature (Celsius)
     */
    private Double sixHourMinTemp;
    
    /**
     * 24-hour maximum temperature (Celsius)
     */
    private Double twentyFourHourMaxTemp;
    
    /**
     * 24-hour minimum temperature (Celsius)
     */
    private Double twentyFourHourMinTemp;
    
    /**
     * 3-hour pressure tendency (in hPa)
     */
    private Double threeHourPressureTendency;
    
    /**
     * No significant change indicator (NOSIG)
     */
    private boolean noSignificantChange;
    
    // ========== CONSTRUCTORS ==========
    
    public NoaaMetarData() {
        super();
        setReportType("METAR");
        this.skyConditions = new ArrayList<>();
        this.presentWeather = new ArrayList<>();
        this.runwayVisualRange = new ArrayList<>();
        this.noSignificantChange = false;
    }
    
    public NoaaMetarData(String stationId, Instant observationTime) {
        super(stationId, observationTime, "METAR");
        this.skyConditions = new ArrayList<>();
        this.presentWeather = new ArrayList<>();
        this.runwayVisualRange = new ArrayList<>();
        this.noSignificantChange = false;
    }
    
    public Wind getWind() {
        return wind;
    }
    
    public void setWind(Wind wind) {
        this.wind = wind;
    }
    
    public Visibility getVisibility() {
        return visibility;
    }
    
    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }
    
    public Temperature getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Temperature temperature) {
        this.temperature = temperature;
    }
    
    public Pressure getPressure() {
        return pressure;
    }
    
    public void setPressure(Pressure pressure) {
        this.pressure = pressure;
    }
    
    /**
     * Get sky conditions as an immutable copy.
     * 
     * @return immutable copy of sky conditions list
     */
    public List<SkyCondition> getSkyConditions() {
        return List.copyOf(skyConditions);
    }
    
    public void setSkyConditions(List<SkyCondition> skyConditions) {
        this.skyConditions = skyConditions != null ? skyConditions : new ArrayList<>();
    }
    
    public void addSkyCondition(SkyCondition skyCondition) {
        if (skyCondition != null) {
            this.skyConditions.add(skyCondition);
        }
    }
    
    /**
     * Get present weather as an immutable copy.
     * 
     * @return immutable copy of present weather list
     */
    public List<String> getPresentWeather() {
        return List.copyOf(presentWeather);
    }
    
    public void setPresentWeather(List<String> presentWeather) {
        this.presentWeather = presentWeather != null ? presentWeather : new ArrayList<>();
    }
    
    public void addPresentWeather(String weather) {
        if (weather != null && !weather.isBlank()) {
            this.presentWeather.add(weather);
        }
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
    
    public PeakWind getPeakWind() {
        return peakWind;
    }
    
    public void setPeakWind(PeakWind peakWind) {
        this.peakWind = peakWind;
    }
    
    public WindShift getWindShift() {
        return windShift;
    }
    
    public void setWindShift(WindShift windShift) {
        this.windShift = windShift;
    }
    
    public String getAutomatedStation() {
        return automatedStation;
    }
    
    public void setAutomatedStation(String automatedStation) {
        this.automatedStation = automatedStation;
    }
    
    public Double getSeaLevelPressure() {
        return seaLevelPressure;
    }
    
    public void setSeaLevelPressure(Double seaLevelPressure) {
        this.seaLevelPressure = seaLevelPressure;
    }
    
    public Double getHourlyPrecipitation() {
        return hourlyPrecipitation;
    }
    
    public void setHourlyPrecipitation(Double hourlyPrecipitation) {
        this.hourlyPrecipitation = hourlyPrecipitation;
    }
    
    public Double getSixHourMaxTemp() {
        return sixHourMaxTemp;
    }
    
    public void setSixHourMaxTemp(Double sixHourMaxTemp) {
        this.sixHourMaxTemp = sixHourMaxTemp;
    }
    
    public Double getSixHourMinTemp() {
        return sixHourMinTemp;
    }
    
    public void setSixHourMinTemp(Double sixHourMinTemp) {
        this.sixHourMinTemp = sixHourMinTemp;
    }
    
    public Double getTwentyFourHourMaxTemp() {
        return twentyFourHourMaxTemp;
    }
    
    public void setTwentyFourHourMaxTemp(Double twentyFourHourMaxTemp) {
        this.twentyFourHourMaxTemp = twentyFourHourMaxTemp;
    }
    
    public Double getTwentyFourHourMinTemp() {
        return twentyFourHourMinTemp;
    }
    
    public void setTwentyFourHourMinTemp(Double twentyFourHourMinTemp) {
        this.twentyFourHourMinTemp = twentyFourHourMinTemp;
    }
    
    public Double getThreeHourPressureTendency() {
        return threeHourPressureTendency;
    }
    
    public void setThreeHourPressureTendency(Double threeHourPressureTendency) {
        this.threeHourPressureTendency = threeHourPressureTendency;
    }
    
    public boolean isNoSignificantChange() {
        return noSignificantChange;
    }
    
    public void setNoSignificantChange(boolean noSignificantChange) {
        this.noSignificantChange = noSignificantChange;
    }
    
    // ========== UTILITY METHODS ==========
    
    /**
     * Get the ceiling (lowest BKN or OVC layer).
     * 
     * @return ceiling in feet, or null if no ceiling
     */
    public Integer getCeilingFeet() {
        if (skyConditions == null || skyConditions.isEmpty()) {
            return null;
        }
        
        return skyConditions.stream()
            .filter(SkyCondition::isCeiling)
            .map(SkyCondition::heightFeet)
            .filter(Objects::nonNull)
            .min(Integer::compareTo)
            .orElse(null);
    }
    
    /**
     * Check if flight category data is available.
     * 
     * @return true if can determine flight category
     */
    public boolean hasFlightCategoryData() {
        return visibility != null && !skyConditions.isEmpty();
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
            .filter(rvr -> !rvr.isLessThan()) // Exclude "less than" values for conservative estimate
            .map(rvr -> rvr.isVariable() ? rvr.variableLow() : rvr.visualRangeFeet())
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
            .filter(rvr -> rvr.runway().equalsIgnoreCase(runwayId))
            .findFirst()
            .orElse(null);
    }
    
    @Override
    public String getSummary() {
        StringBuilder sb = new StringBuilder("METAR ");
        sb.append(getStationId()).append(" ");
        
        if (wind != null) {
            sb.append("Wind: ").append(wind.getCardinalDirection()).append(" ");
        }
        if (visibility != null) {
            sb.append("Vis: ").append(visibility.distanceValue()).append(visibility.unit()).append(" ");
        }
        if (temperature != null) {
            sb.append("Temp: ").append(temperature.celsius()).append("°C ");
        }
        
        return sb.toString().trim();
    }
    
    @Override
    public String toString() {
        return String.format(
            "NoaaMetarData{station=%s, time=%s, wind=%s, vis=%s, temp=%s, pressure=%s, skyCond=%d, rvr=%d}", 
            getStationId(), getObservationTime(), wind, visibility, temperature, pressure,
            skyConditions != null ? skyConditions.size() : 0,
            runwayVisualRange != null ? runwayVisualRange.size() : 0  // ← ADD THIS LINE
        );
    }
    
    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NoaaMetarData that)) {
            return false;
        }
        
        // Compare parent fields manually (skip auto-generated id and ingestionTimestamp)
        if (!Objects.equals(getStationId(), that.getStationId())) {
            return false;
        }
        if (!Objects.equals(getObservationTime(), that.getObservationTime())) {
            return false;
        }
        if (!Objects.equals(getReportType(), that.getReportType())) {
            return false;
        }
        if (!Objects.equals(getRawText(), that.getRawText())) {
            return false;
        }
        
        // Compare NoaaMetarData-specific fields
        return noSignificantChange == that.noSignificantChange &&
               Objects.equals(wind, that.wind) &&
               Objects.equals(visibility, that.visibility) &&
               Objects.equals(temperature, that.temperature) &&
               Objects.equals(pressure, that.pressure) &&
               Objects.equals(skyConditions, that.skyConditions) &&
               Objects.equals(presentWeather, that.presentWeather) &&
               Objects.equals(runwayVisualRange, that.runwayVisualRange) &&
               Objects.equals(peakWind, that.peakWind) &&
               Objects.equals(windShift, that.windShift) &&
               Objects.equals(automatedStation, that.automatedStation) &&
               Objects.equals(seaLevelPressure, that.seaLevelPressure) &&
               Objects.equals(hourlyPrecipitation, that.hourlyPrecipitation) &&
               Objects.equals(sixHourMaxTemp, that.sixHourMaxTemp) &&
               Objects.equals(sixHourMinTemp, that.sixHourMinTemp) &&
               Objects.equals(twentyFourHourMaxTemp, that.twentyFourHourMaxTemp) &&
               Objects.equals(twentyFourHourMinTemp, that.twentyFourHourMinTemp) &&
               Objects.equals(threeHourPressureTendency, that.threeHourPressureTendency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            // Parent business fields (NOT id or ingestionTimestamp)
            getStationId(),
            getObservationTime(),
            getReportType(),
            getRawText(),
            // NoaaMetarData fields
            wind,
            visibility,
            temperature,
            pressure,
            skyConditions,
            presentWeather,
            runwayVisualRange,
            peakWind,
            windShift,
            automatedStation,
            seaLevelPressure,
            hourlyPrecipitation,
            sixHourMaxTemp,
            sixHourMinTemp,
            twentyFourHourMaxTemp,
            twentyFourHourMinTemp,
            threeHourPressureTendency,
            noSignificantChange
        );
    }
}
