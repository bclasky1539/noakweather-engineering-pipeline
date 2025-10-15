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
 * Represents wind information common to both METAR and TAF reports.
 * This encapsulates wind direction, speed, gusts, and variable directions.
 * 
 * @author bclasky1539
 *
 */
public class WindInformation {
    
    private Integer windDirectionDegrees;
    private Integer windSpeedKnots;
    private Integer windGustKnots;
    private String windVariableDirection; // e.g., "240V300" for variable wind
    
    public WindInformation() {}
    
    public WindInformation(Integer directionDegrees, Integer speedKnots, Integer gustKnots) {
        this.windDirectionDegrees = directionDegrees;
        this.windSpeedKnots = speedKnots;
        this.windGustKnots = gustKnots;
    }
    
    // Getters and setters
    public Integer getWindDirectionDegrees() {
        return windDirectionDegrees;
    }
    
    public void setWindDirectionDegrees(Integer windDirectionDegrees) {
        this.windDirectionDegrees = windDirectionDegrees;
    }
    
    public Integer getWindSpeedKnots() {
        return windSpeedKnots;
    }
    
    public void setWindSpeedKnots(Integer windSpeedKnots) {
        this.windSpeedKnots = windSpeedKnots;
    }
    
    public Integer getWindGustKnots() {
        return windGustKnots;
    }
    
    public void setWindGustKnots(Integer windGustKnots) {
        this.windGustKnots = windGustKnots;
    }
    
    public String getWindVariableDirection() {
        return windVariableDirection;
    }
    
    public void setWindVariableDirection(String windVariableDirection) {
        this.windVariableDirection = windVariableDirection;
    }
    
    /**
     * Checks if wind conditions are calm (typically < 3 knots)
     */
    public boolean isCalm() {
        return windSpeedKnots == null || windSpeedKnots < 3;
    }
    
    /**
     * Checks if there are wind gusts
     */
    public boolean hasGusts() {
        return windGustKnots != null && windGustKnots > 0;
    }
    
    /**
     * Gets the wind direction as a cardinal/intercardinal direction (N, NE, E, etc.)
     */
    public String getWindDirectionCardinal() {
        if (windDirectionDegrees == null) {
            return null;
        }
        
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                              "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        
        int index = (int) Math.round(windDirectionDegrees / 22.5) % 16;
        return directions[index];
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WindInformation)) return false;
        WindInformation that = (WindInformation) o;
        return Objects.equals(windDirectionDegrees, that.windDirectionDegrees) &&
               Objects.equals(windSpeedKnots, that.windSpeedKnots) &&
               Objects.equals(windGustKnots, that.windGustKnots) &&
               Objects.equals(windVariableDirection, that.windVariableDirection);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(windDirectionDegrees, windSpeedKnots, windGustKnots, windVariableDirection);
    }
    
    @Override
    public String toString() {
        if (isCalm()) {
            return "Wind: Calm";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Wind: ");
        if (windDirectionDegrees != null) {
            sb.append(windDirectionDegrees).append("° (").append(getWindDirectionCardinal()).append(") ");
        }
        if (windSpeedKnots != null) {
            sb.append(windSpeedKnots).append(" knots");
        }
        if (hasGusts()) {
            sb.append(" gusts ").append(windGustKnots).append(" knots");
        }
        if (windVariableDirection != null) {
            sb.append(" variable ").append(windVariableDirection);
        }
        return sb.toString();
    }
}
