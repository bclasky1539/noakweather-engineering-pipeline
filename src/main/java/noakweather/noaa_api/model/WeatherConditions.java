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

import java.util.Objects;

/**
 * Represents common weather condition information shared between METAR and TAF reports.
 * This includes visibility, weather phenomena, and sky conditions.
 * 
 * @author bclasky1539
 *
 */
public class WeatherConditions {
    
    private Double visibilityStatuteMiles;
    private String weatherString; // Raw weather phenomena (e.g., "RA BR", "-SN")
    private String skyCondition;  // Sky coverage information
    
    public WeatherConditions() {}
    
    public WeatherConditions(Double visibility, String weather, String sky) {
        this.visibilityStatuteMiles = visibility;
        this.weatherString = weather;
        this.skyCondition = sky;
    }
    
    // Getters and setters
    public Double getVisibilityStatuteMiles() {
        return visibilityStatuteMiles;
    }
    
    public void setVisibilityStatuteMiles(Double visibilityStatuteMiles) {
        this.visibilityStatuteMiles = visibilityStatuteMiles;
    }
    
    public String getWeatherString() {
        return weatherString;
    }
    
    public void setWeatherString(String weatherString) {
        this.weatherString = weatherString;
    }
    
    public String getSkyCondition() {
        return skyCondition;
    }
    
    public void setSkyCondition(String skyCondition) {
        this.skyCondition = skyCondition;
    }
    
    /**
     * Checks if visibility is considered good (>= 3 statute miles)
     */
    public boolean hasGoodVisibility() {
        return visibilityStatuteMiles != null && visibilityStatuteMiles >= 3.0;
    }
    
    /**
     * Checks if there are active weather phenomena
     */
    public boolean hasActiveWeather() {
        return weatherString != null && !weatherString.trim().isEmpty();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WeatherConditions)) return false;
        WeatherConditions that = (WeatherConditions) o;
        return Objects.equals(visibilityStatuteMiles, that.visibilityStatuteMiles) &&
               Objects.equals(weatherString, that.weatherString) &&
               Objects.equals(skyCondition, that.skyCondition);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(visibilityStatuteMiles, weatherString, skyCondition);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Weather Conditions: ");
        
        if (visibilityStatuteMiles != null) {
            sb.append("Visibility ").append(visibilityStatuteMiles).append(" miles, ");
        }
        
        if (hasActiveWeather()) {
            sb.append("Weather: ").append(weatherString).append(", ");
        }
        
        if (skyCondition != null) {
            sb.append("Sky: ").append(skyCondition);
        }
        
        return sb.toString();
    }
}
