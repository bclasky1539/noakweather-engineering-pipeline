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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import weather.model.components.Pressure;
import weather.model.components.Temperature;
import weather.model.components.Visibility;
import weather.model.components.Wind;

import java.time.Instant;
import java.util.Objects;

/**
 * METAR-specific weather data model.
 * <p>
 * Extends NoaaWeatherData with METAR observation fields.
 * Inherits WeatherConditions from parent for current observations.
 * <p>
 * METAR (Meteorological Aerodrome Report) is an aviation weather observation
 * that describes current conditions at an airport.
 * <p>
 * Architecture:
 * - Weather conditions (wind, visibility, weather, sky, temp, pressure) inherited from NoaaWeatherData
 * - METAR-specific supplemental data (NOSIG) remains here
 * - All condition getters inherited from parent class
 * - Remark values (sea level pressure, peak wind, temperature extremes and so on)
 *   live only in {@code getRemarks()}; this class keeps no top-level copies (#217)
 *
 * @author bclasky1539
 *
 */
@JsonTypeName("METAR")
public class NoaaMetarData extends NoaaWeatherData {

    // ========== METAR-SPECIFIC FIELDS ==========

    /**
     * No significant change indicator (NOSIG)
     */
    private boolean noSignificantChange;

    /**
     * Metar value
     */
    private static final String OBSERVATION_TYPE = "METAR";

    // ========== CONSTRUCTORS ==========

    public NoaaMetarData() {
        super();
        setReportType(OBSERVATION_TYPE);
        this.noSignificantChange = false;
    }

    public NoaaMetarData(String stationId, Instant observationTime) {
        super(stationId, observationTime, OBSERVATION_TYPE);
        this.noSignificantChange = false;
    }

    // ========== CONDITIONS ACCESS ==========
    // Note: getConditions() and setConditions() are inherited from NoaaWeatherData
    // Note: All convenience getters (getWind(), getVisibility(), etc.) are inherited

    // ========== METAR-SPECIFIC GETTERS/SETTERS ==========

    public boolean isNoSignificantChange() {
        return noSignificantChange;
    }

    public void setNoSignificantChange(boolean noSignificantChange) {
        this.noSignificantChange = noSignificantChange;
    }

    // ========== OVERRIDES ==========

    @Override
    @JsonIgnore
    public String getDataType() {
        return OBSERVATION_TYPE;
    }

    @Override
    public String getSummary() {
        StringBuilder sb = new StringBuilder("METAR ");
        sb.append(getStationId()).append(" ");

        Wind wind = getWind();
        if (wind != null) {
            sb.append("Wind: ").append(wind.getCardinalDirection()).append(" ");
            if (wind.hasGusts()) {
                sb.append("G").append(wind.gustValue()).append(wind.unit()).append(" ");
            }
        }

        Visibility visibility = getVisibility();
        if (visibility != null) {
            if (visibility.isCavok()) {
                sb.append("CAVOK ");
            } else {
                sb.append("Vis: ").append(visibility.distanceValue())
                        .append(visibility.unit()).append(" ");
            }
        }

        Temperature temperature = getTemperature();
        if (temperature != null) {
            sb.append("Temp: ").append(temperature.celsius()).append("°C ");
            if (temperature.dewpointCelsius() != null) {
                sb.append("Dew: ").append(temperature.dewpointCelsius()).append("°C ");
            }
        }

        Pressure pressure = getPressure();
        if (pressure != null) {
            sb.append("Press: ").append(pressure.value())
                    .append(pressure.unit()).append(" ");
        }

        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return String.format(
                "NoaaMetarData{station=%s, time=%s, wind=%s, vis=%s, temp=%s, pressure=%s, skyCond=%d, rvr=%d}",
                getStationId(),
                getObservationTime(),
                getWind(),
                getVisibility(),
                getTemperature(),
                getPressure(),
                getSkyConditions().size(),
                getRunwayVisualRange().size()
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
        if (!super.equals(o)) {
            return false;
        }

        // Compare NoaaMetarData-specific fields only
        // Parent fields (including conditions) are compared in parent's equals()
        return noSignificantChange == that.noSignificantChange;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), noSignificantChange);
    }
}
