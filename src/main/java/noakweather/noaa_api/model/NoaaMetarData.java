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
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Represents METAR (Meteorological Terminal Aviation Routine Weather Report) data
 * as returned by NOAA's Aviation Weather Center API.
 * 
 * METAR reports are current weather observations, typically updated hourly.
 * Think of this as a snapshot of current conditions at a specific airport.
 * 
 * @author bclasky1539
 * 
 */
public class NoaaMetarData extends NoaaAviationWeatherData {
    
    // Temperature and pressure
    private Double temperatureCelsius;
    private Double dewpointCelsius;
    private Double altimeterInHg;
    
    // Composition objects to eliminate duplication
    private WindInformation windInformation;
    private WeatherConditions weatherConditions;
    
    // Flight category (VFR, MVFR, IFR, LIFR) - calculated by NOAA
    private String flightCategory;
    
    // Precipitation
    private Double precipitationLastHourInches;
    private Double precipitationLast3HoursInches;
    private Double precipitationLast6HoursInches;
    
    // Special fields
    private String metarType; // "METAR" or "SPECI" (special report)
    private Boolean isAutoReport; // True if automated station
    
    public NoaaMetarData() {
        super();
        this.windInformation = new WindInformation();
        this.weatherConditions = new WeatherConditions();
    }
    
    public NoaaMetarData(String rawText, String stationId, LocalDateTime observationTime) {
        super(rawText, stationId, observationTime);
        this.windInformation = new WindInformation();
        this.weatherConditions = new WeatherConditions();
        
        // Parse the METAR type from raw text if available
        if (rawText != null) {
            this.metarType = rawText.startsWith("SPECI") ? "SPECI" : "METAR";
            this.isAutoReport = rawText.contains("AUTO");
        }
    }
    
    @Override
    public boolean isCurrent() {
        if (getObservationTime() == null) {
            return false;
        }
        // METAR reports are considered current if less than 3 hours old
        LocalDateTime now = LocalDateTime.now();
        long hoursBetween = ChronoUnit.HOURS.between(getObservationTime(), now);
        return hoursBetween <= 3;
    }
    
    @Override
    public String getReportType() {
        return metarType != null ? metarType : "METAR";
    }
    
    // Temperature and pressure getters/setters
    public Double getTemperatureCelsius() {
        return temperatureCelsius;
    }
    
    public void setTemperatureCelsius(Double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }
    
    public Double getDewpointCelsius() {
        return dewpointCelsius;
    }
    
    public void setDewpointCelsius(Double dewpointCelsius) {
        this.dewpointCelsius = dewpointCelsius;
    }
    
    public Double getAltimeterInHg() {
        return altimeterInHg;
    }
    
    public void setAltimeterInHg(Double altimeterInHg) {
        this.altimeterInHg = altimeterInHg;
    }
    
    // Wind information - delegating to WindInformation object
    public WindInformation getWindInformation() {
        return windInformation;
    }
    
    public void setWindInformation(WindInformation windInformation) {
        this.windInformation = windInformation != null ? windInformation : new WindInformation();
    }
    
    // Convenience methods for backward compatibility
    public Integer getWindDirectionDegrees() {
        return windInformation != null ? windInformation.getWindDirectionDegrees() : null;
    }
    
    public void setWindDirectionDegrees(Integer windDirectionDegrees) {
        if (windInformation == null) {
            windInformation = new WindInformation();
        }
        windInformation.setWindDirectionDegrees(windDirectionDegrees);
    }
    
    public Integer getWindSpeedKnots() {
        return windInformation != null ? windInformation.getWindSpeedKnots() : null;
    }
    
    public void setWindSpeedKnots(Integer windSpeedKnots) {
        if (windInformation == null) {
            windInformation = new WindInformation();
        }
        windInformation.setWindSpeedKnots(windSpeedKnots);
    }
    
    public Integer getWindGustKnots() {
        return windInformation != null ? windInformation.getWindGustKnots() : null;
    }
    
    public void setWindGustKnots(Integer windGustKnots) {
        if (windInformation == null) {
            windInformation = new WindInformation();
        }
        windInformation.setWindGustKnots(windGustKnots);
    }
    
    public String getWindVariableDirection() {
        return windInformation != null ? windInformation.getWindVariableDirection() : null;
    }
    
    public void setWindVariableDirection(String windVariableDirection) {
        if (windInformation == null) {
            windInformation = new WindInformation();
        }
        windInformation.setWindVariableDirection(windVariableDirection);
    }
    
    // Weather conditions - delegating to WeatherConditions object
    public WeatherConditions getWeatherConditions() {
        return weatherConditions;
    }
    
    public void setWeatherConditions(WeatherConditions weatherConditions) {
        this.weatherConditions = weatherConditions != null ? weatherConditions : new WeatherConditions();
    }
    
    // Convenience methods for backward compatibility
    public Double getVisibilityStatuteMiles() {
        return weatherConditions != null ? weatherConditions.getVisibilityStatuteMiles() : null;
    }
    
    public void setVisibilityStatuteMiles(Double visibilityStatuteMiles) {
        if (weatherConditions == null) {
            weatherConditions = new WeatherConditions();
        }
        weatherConditions.setVisibilityStatuteMiles(visibilityStatuteMiles);
    }
    
    public String getWeatherString() {
        return weatherConditions != null ? weatherConditions.getWeatherString() : null;
    }
    
    public void setWeatherString(String weatherString) {
        if (weatherConditions == null) {
            weatherConditions = new WeatherConditions();
        }
        weatherConditions.setWeatherString(weatherString);
    }
    
    public String getSkyCondition() {
        return weatherConditions != null ? weatherConditions.getSkyCondition() : null;
    }
    
    public void setSkyCondition(String skyCondition) {
        if (weatherConditions == null) {
            weatherConditions = new WeatherConditions();
        }
        weatherConditions.setSkyCondition(skyCondition);
    }
    
    // Flight category getter/setter
    public String getFlightCategory() {
        return flightCategory;
    }
    
    public void setFlightCategory(String flightCategory) {
        this.flightCategory = flightCategory;
    }
    
    // Precipitation getters/setters
    public Double getPrecipitationLastHourInches() {
        return precipitationLastHourInches;
    }
    
    public void setPrecipitationLastHourInches(Double precipitationLastHourInches) {
        this.precipitationLastHourInches = precipitationLastHourInches;
    }
    
    public Double getPrecipitationLast3HoursInches() {
        return precipitationLast3HoursInches;
    }
    
    public void setPrecipitationLast3HoursInches(Double precipitationLast3HoursInches) {
        this.precipitationLast3HoursInches = precipitationLast3HoursInches;
    }
    
    public Double getPrecipitationLast6HoursInches() {
        return precipitationLast6HoursInches;
    }
    
    public void setPrecipitationLast6HoursInches(Double precipitationLast6HoursInches) {
        this.precipitationLast6HoursInches = precipitationLast6HoursInches;
    }
    
    // Special fields getters/setters
    public String getMetarType() {
        return metarType;
    }
    
    public void setMetarType(String metarType) {
        this.metarType = metarType;
    }
    
    public Boolean getIsAutoReport() {
        return isAutoReport;
    }
    
    public void setIsAutoReport(Boolean isAutoReport) {
        this.isAutoReport = isAutoReport;
    }
    
    /**
     * Convenience method to get temperature in Fahrenheit
     * @return temperature in Fahrenheit or null if not available
     */
    public Double getTemperatureFahrenheit() {
        if (temperatureCelsius == null) {
            return null;
        }
        return (temperatureCelsius * 9.0 / 5.0) + 32.0;
    }
    
    /**
     * Convenience method to get dewpoint in Fahrenheit
     * @return dewpoint in Fahrenheit or null if not available
     */
    public Double getDewpointFahrenheit() {
        if (dewpointCelsius == null) {
            return null;
        }
        return (dewpointCelsius * 9.0 / 5.0) + 32.0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NoaaMetarData)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        NoaaMetarData that = (NoaaMetarData) o;
        return Objects.equals(temperatureCelsius, that.getTemperatureCelsius()) &&
               Objects.equals(windInformation, that.getWindInformation()) &&
               Objects.equals(weatherConditions, that.getWeatherConditions()) &&
               Objects.equals(metarType, that.getMetarType());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), temperatureCelsius, windInformation, weatherConditions, metarType);
    }
}
