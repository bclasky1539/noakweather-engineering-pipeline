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
        this.noSignificantChange = false;
    }
    
    public NoaaMetarData(String stationId, Instant observationTime) {
        super(stationId, observationTime, "METAR");
        this.noSignificantChange = false;
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
        if (getSkyConditions() == null || getSkyConditions().isEmpty()) {
            return null;
        }
        
        return getSkyConditions().stream()
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
        return getVisibility() != null && !getSkyConditions().isEmpty();
    }
    
    /**
     * Get minimum RVR value across all runways (useful for operational decisions).
     * 
     * @return minimum RVR in feet, or null if no RVR data
     */
    public Integer getMinimumRvrFeet() {
        if (getRunwayVisualRange() == null || getRunwayVisualRange().isEmpty()) {
            return null;
        }
        
        return getRunwayVisualRange().stream()
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
        if (getRunwayVisualRange() == null || runwayId == null) {
            return null;
        }
        
        return getRunwayVisualRange().stream()
            .filter(rvr -> rvr.runway().equalsIgnoreCase(runwayId))
            .findFirst()
            .orElse(null);
    }
    
    @Override
    public String getSummary() {
        StringBuilder sb = new StringBuilder("METAR ");
        sb.append(getStationId()).append(" ");
        
        if (getWind() != null) {
            sb.append("Wind: ").append(getWind().getCardinalDirection()).append(" ");
        }
        if (getVisibility() != null) {
            sb.append("Vis: ").append(getVisibility().distanceValue()).append(getVisibility().unit()).append(" ");
        }
        if (getTemperature() != null) {
            sb.append("Temp: ").append(getTemperature().celsius()).append("°C ");
        }
        
        return sb.toString().trim();
    }
    
    @Override
    public String toString() {
        return String.format(
            "NoaaMetarData{station=%s, time=%s, wind=%s, vis=%s, temp=%s, pressure=%s, skyCond=%d, rvr=%d}",
            getStationId(), getObservationTime(), getWind(), getVisibility(), getTemperature(), getPressure(),
            getSkyConditions() != null ? getSkyConditions().size() : 0,
            getRunwayVisualRange() != null ? getRunwayVisualRange().size() : 0
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
               Objects.equals(getWind(), that.getWind()) &&
               Objects.equals(getVisibility(), that.getVisibility()) &&
               Objects.equals(getTemperature(), that.getTemperature()) &&
               Objects.equals(getPressure(), that.getPressure()) &&
               Objects.equals(getSkyConditions(), that.getSkyConditions()) &&
               Objects.equals(getPresentWeather(), that.getPresentWeather()) &&
               Objects.equals(getRunwayVisualRange(), that.getRunwayVisualRange()) &&
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
            getWind(),
            getVisibility(),
            getTemperature(),
            getPressure(),
            getSkyConditions(),
            getPresentWeather(),
            getRunwayVisualRange(),
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
