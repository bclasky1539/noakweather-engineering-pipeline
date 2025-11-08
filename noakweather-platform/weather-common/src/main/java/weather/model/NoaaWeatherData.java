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

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Platform-native NOAA weather data implementation.
 * 
 * This is the NEW version that uses the universal WeatherData base.
 * The legacy NoaaAviationWeatherData in noakweather-legacy remains unchanged.
 * 
 * This class will be fully implemented in Day 3 (weather-processing module).
 * For now, it's a minimal placeholder to satisfy the sealed class requirement.
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
    
    public NoaaWeatherData() {
        super();
    }
    
    public NoaaWeatherData(String stationId, Instant observationTime, String reportType) {
        super(WeatherDataSource.NOAA, stationId, observationTime);
        this.reportType = reportType;
    }
    
    public String getReportType() {
        return reportType;
    }
    
    public void setReportType(String reportType) {
        this.reportType = reportType;
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
        return "%s from %s at %s".formatted(
            reportType != null ? reportType : "NOAA Report",
            getStationId(),
            getObservationTime()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NoaaWeatherData that)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return Objects.equals(reportType, that.getReportType());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), reportType);
    }
}
