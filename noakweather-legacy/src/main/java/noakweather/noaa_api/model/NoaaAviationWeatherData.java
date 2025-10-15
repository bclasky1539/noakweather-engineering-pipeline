/*
 * noakweather(TM) is a Java library for parsing weather data
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
package noakweather.noaa_api.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base class for NOAA Aviation Weather data.
 * Represents common fields shared across different types of aviation weather reports
 * (METAR, TAF, etc.) as returned by NOAA's Aviation Weather Center API.
 * 
 * This acts as the foundation for all NOAA-specific weather data models,
 * similar to a base table structure in a database that contains common columns.
 * 
 * @author bclasky1539
 * 
 */
public abstract class NoaaAviationWeatherData {
    
    /**
     * The raw text of the weather report as received from NOAA
     * (e.g., "METAR KJFK 251651Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 SLP210")
     */
    private String rawText;
    
    /**
     * ICAO station identifier (e.g., "KJFK", "KLGA")
     */
    private String stationId;
    
    /**
     * Observation time in UTC
     */
    private LocalDateTime observationTime;
    
    /**
     * Latitude of the reporting station in decimal degrees
     */
    private Double latitude;
    
    /**
     * Longitude of the reporting station in decimal degrees  
     */
    private Double longitude;
    
    /**
     * Elevation of the reporting station in feet above mean sea level
     */
    private Integer elevationFeet;
    
    /**
     * Quality control flags or data source indicators from NOAA
     */
    private String qualityControlFlags;
    
    protected NoaaAviationWeatherData() {
        // Protected constructor for subclasses
    }
    
    protected NoaaAviationWeatherData(String rawText, String stationId, LocalDateTime observationTime) {
        this.rawText = rawText;
        this.stationId = stationId;
        this.observationTime = observationTime;
    }
    
    // Getters and setters
    public String getRawText() {
        return rawText;
    }
    
    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
    
    public String getStationId() {
        return stationId;
    }
    
    public void setStationId(String stationId) {
        this.stationId = stationId;
    }
    
    public LocalDateTime getObservationTime() {
        return observationTime;
    }
    
    public void setObservationTime(LocalDateTime observationTime) {
        this.observationTime = observationTime;
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
    
    /**
     * Determines if this weather data is considered current/valid.
     * Subclasses should implement their own logic based on report type.
     * 
     * @return true if the data is considered current
     */
    public abstract boolean isCurrent();
    
    /**
     * Returns the type of weather report (METAR, TAF, etc.)
     * 
     * @return the report type identifier
     */
    public abstract String getReportType();
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NoaaAviationWeatherData)) {
            return false;
        }
        NoaaAviationWeatherData that = (NoaaAviationWeatherData) o;
        return Objects.equals(stationId, that.getStationId()) &&
               Objects.equals(observationTime, that.getObservationTime()) &&
               Objects.equals(rawText, that.getRawText());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(stationId, observationTime, rawText);
    }
    
    @Override
    public String toString() {
        return String.format("%s{stationId='%s', observationTime=%s, rawText='%s'}", 
                           getClass().getSimpleName(), stationId, observationTime, rawText);
    }
}
