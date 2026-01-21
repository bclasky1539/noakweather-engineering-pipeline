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
import weather.model.components.remark.*;

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
 * - METAR-specific fields for remarks and supplemental data remain here
 * - All condition getters inherited from parent class
 *
 * @author bclasky1539
 *
 */
@JsonTypeName("METAR")
public class NoaaMetarData extends NoaaWeatherData {

    // ========== REMARKS SECTION COMPONENTS ==========
    // These are METAR-specific and not part of the general conditions

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

    // ========== REMARKS GETTERS/SETTERS ==========

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

    // ========== METAR-SPECIFIC UTILITY METHODS ==========

    /**
     * Check if this METAR is an automated station with precipitation discriminator (AO2).
     *
     * @return true if AO2
     */
    public boolean hasAO2() {
        return "AO2".equals(automatedStation);
    }

    /**
     * Check if this METAR is an automated station without precipitation discriminator (AO1).
     *
     * @return true if AO1
     */
    public boolean hasAO1() {
        return "AO1".equals(automatedStation);
    }

    /**
     * Check if this METAR is from an automated station (AO1 or AO2).
     *
     * @return true if automated
     */
    public boolean isAutomated() {
        return automatedStation != null &&
                (automatedStation.equals("AO1") || automatedStation.equals("AO2"));
    }

    /**
     * Check if any precipitation data is available in remarks.
     *
     * @return true if precipitation data exists
     */
    public boolean hasPrecipitationData() {
        return hourlyPrecipitation != null;
    }

    /**
     * Check if temperature extremes data is available in remarks.
     *
     * @return true if 6-hour or 24-hour temperature data exists
     */
    public boolean hasTemperatureExtremes() {
        return sixHourMaxTemp != null || sixHourMinTemp != null ||
                twentyFourHourMaxTemp != null || twentyFourHourMinTemp != null;
    }

    // ========== OVERRIDES ==========

    @Override
    public String getDataType() { return OBSERVATION_TYPE; }

    @Override
    public String getSummary() {
        StringBuilder sb = new StringBuilder("METAR ");
        sb.append(getStationId()).append(" ");

        if (getWind() != null) {
            sb.append("Wind: ").append(getWind().getCardinalDirection()).append(" ");
            if (getWind().hasGusts()) {
                sb.append("G").append(getWind().gustValue()).append(getWind().unit()).append(" ");
            }
        }

        if (getVisibility() != null) {
            if (getVisibility().isCavok()) {
                sb.append("CAVOK ");
            } else {
                sb.append("Vis: ").append(getVisibility().distanceValue())
                        .append(getVisibility().unit()).append(" ");
            }
        }

        if (getTemperature() != null) {
            sb.append("Temp: ").append(getTemperature().celsius()).append("°C ");
            if (getTemperature().dewpointCelsius() != null) {
                sb.append("Dew: ").append(getTemperature().dewpointCelsius()).append("°C ");
            }
        }

        if (getPressure() != null) {
            sb.append("Press: ").append(getPressure().value())
                    .append(getPressure().unit()).append(" ");
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
        return noSignificantChange == that.noSignificantChange &&
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
                super.hashCode(),
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
