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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents TAF (Terminal Aerodrome Forecast) data as returned by NOAA's Aviation Weather Center API.
 * 
 * TAF reports are weather forecasts for airports, typically covering 24-30 hours.
 * Think of this as a weather prediction document with multiple time periods and conditions.
 * Unlike METAR (current observations), TAF contains forecast periods with expected changes.
 * 
 * Uses composition with WindInformation and WeatherConditions to eliminate
 * code duplication and provide a cleaner domain model.
 * 
 * @author bclasky1539
 * 
 */
public class NoaaTafData extends NoaaAviationWeatherData {
    
    // Forecast validity period
    private LocalDateTime validFromTime;
    private LocalDateTime validToTime;
    
    // Issue information
    private LocalDateTime issueTime;
    private String bulletinTime; // The bulletin time from NOAA
    
    // Forecast type and amendment info
    private String tafType; // "TAF", "TAF AMD" (amended), "TAF COR" (corrected)
    private Boolean isAmended;
    private Boolean isCorrected;
    
    // Base forecast conditions (the primary forecast before any change groups)
    private String baseForecastText;
    private WindInformation baseWindInformation;
    private WeatherConditions baseWeatherConditions;
    
    // Change groups (TEMPO, BECMG, FM, PROB groups)
    private List<TafChangeGroup> changeGroups;
    
    public NoaaTafData() {
        super();
        this.baseWindInformation = new WindInformation();
        this.baseWeatherConditions = new WeatherConditions();
        this.changeGroups = new ArrayList<>();
    }
    
    public NoaaTafData(String rawText, String stationId, LocalDateTime observationTime) {
        super(rawText, stationId, observationTime);
        this.baseWindInformation = new WindInformation();
        this.baseWeatherConditions = new WeatherConditions();
        this.changeGroups = new ArrayList<>();
        
        // Parse TAF type from raw text if available
        if (rawText != null) {
            if (rawText.contains("TAF AMD")) {
                this.tafType = "TAF AMD";
                this.isAmended = true;
            } else if (rawText.contains("TAF COR")) {
                this.tafType = "TAF COR";
                this.isCorrected = true;
            } else {
                this.tafType = "TAF";
            }
        }
    }
    
    @Override
    public boolean isCurrent() {
        if (validToTime == null) {
            return false;
        }
        // TAF is current if we're within its valid time period
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(validToTime) && 
               (validFromTime == null || now.isAfter(validFromTime));
    }
    
    @Override
    public String getReportType() {
        return tafType != null ? tafType : "TAF";
    }
    
    // Validity period getters/setters
    public LocalDateTime getValidFromTime() {
        return validFromTime;
    }
    
    public void setValidFromTime(LocalDateTime validFromTime) {
        this.validFromTime = validFromTime;
    }
    
    public LocalDateTime getValidToTime() {
        return validToTime;
    }
    
    public void setValidToTime(LocalDateTime validToTime) {
        this.validToTime = validToTime;
    }
    
    public LocalDateTime getIssueTime() {
        return issueTime;
    }
    
    public void setIssueTime(LocalDateTime issueTime) {
        this.issueTime = issueTime;
    }
    
    public String getBulletinTime() {
        return bulletinTime;
    }
    
    public void setBulletinTime(String bulletinTime) {
        this.bulletinTime = bulletinTime;
    }
    
    // TAF type and status getters/setters
    public String getTafType() {
        return tafType;
    }
    
    public void setTafType(String tafType) {
        this.tafType = tafType;
    }
    
    public Boolean getIsAmended() {
        return isAmended;
    }
    
    public void setIsAmended(Boolean isAmended) {
        this.isAmended = isAmended;
    }
    
    public Boolean getIsCorrected() {
        return isCorrected;
    }
    
    public void setIsCorrected(Boolean isCorrected) {
        this.isCorrected = isCorrected;
    }
    
    // Base forecast getters/setters
    public String getBaseForecastText() {
        return baseForecastText;
    }
    
    public void setBaseForecastText(String baseForecastText) {
        this.baseForecastText = baseForecastText;
    }
    
    // Direct access to composition objects (preferred approach)
    public WindInformation getBaseWindInformation() {
        return baseWindInformation;
    }
    
    public void setBaseWindInformation(WindInformation baseWindInformation) {
        this.baseWindInformation = baseWindInformation != null ? baseWindInformation : new WindInformation();
    }
    
    public WeatherConditions getBaseWeatherConditions() {
        return baseWeatherConditions;
    }
    
    public void setBaseWeatherConditions(WeatherConditions baseWeatherConditions) {
        this.baseWeatherConditions = baseWeatherConditions != null ? baseWeatherConditions : new WeatherConditions();
    }
    
    // Change groups getters/setters
    public List<TafChangeGroup> getChangeGroups() {
        return changeGroups;
    }
    
    public void setChangeGroups(List<TafChangeGroup> changeGroups) {
        this.changeGroups = changeGroups != null ? changeGroups : new ArrayList<>();
    }
    
    public void addChangeGroup(TafChangeGroup changeGroup) {
        if (this.changeGroups == null) {
            this.changeGroups = new ArrayList<>();
        }
        this.changeGroups.add(changeGroup);
    }
    
    /**
     * Gets the forecast validity period in hours
     * @return validity period in hours
     */
    public long getValidityPeriodHours() {
        if (validFromTime == null || validToTime == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(validFromTime, validToTime);
    }
    
    /**
     * Checks if this TAF has been amended or corrected
     * @return true if the TAF has been modified
     */
    public boolean isModified() {
        return Boolean.TRUE.equals(isAmended) || Boolean.TRUE.equals(isCorrected);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NoaaTafData)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        NoaaTafData that = (NoaaTafData) o;
        return Objects.equals(validFromTime, that.getValidFromTime()) &&
               Objects.equals(validToTime, that.getValidToTime()) &&
               Objects.equals(issueTime, that.getIssueTime()) &&
               Objects.equals(tafType, that.getTafType()) &&
               Objects.equals(baseWindInformation, that.getBaseWindInformation()) &&
               Objects.equals(baseWeatherConditions, that.getBaseWeatherConditions());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), validFromTime, validToTime, issueTime, tafType, 
                          baseWindInformation, baseWeatherConditions);
    }
    
    /**
     * Represents a change group within a TAF forecast (TEMPO, BECMG, FM, PROB groups).
     * These indicate expected changes to conditions during specific time periods.
     */
    public static class TafChangeGroup {
        private String changeType; // "TEMPO", "BECMG", "FM", "PROB30", "PROB40", etc.
        private LocalDateTime changeTimeFrom;
        private LocalDateTime changeTimeTo;
        private String changeText; // Raw text of the change group
        
        // Using composition for change group conditions too
        private WindInformation windInformation;
        private WeatherConditions weatherConditions;
        
        // Constructors
        public TafChangeGroup() {
            this.windInformation = new WindInformation();
            this.weatherConditions = new WeatherConditions();
        }
        
        public TafChangeGroup(String changeType, String changeText) {
            this.changeType = changeType;
            this.changeText = changeText;
            this.windInformation = new WindInformation();
            this.weatherConditions = new WeatherConditions();
        }
        
        // Basic getters and setters
        public String getChangeType() {
            return changeType;
        }
        
        public void setChangeType(String changeType) {
            this.changeType = changeType;
        }
        
        public LocalDateTime getChangeTimeFrom() {
            return changeTimeFrom;
        }
        
        public void setChangeTimeFrom(LocalDateTime changeTimeFrom) {
            this.changeTimeFrom = changeTimeFrom;
        }
        
        public LocalDateTime getChangeTimeTo() {
            return changeTimeTo;
        }
        
        public void setChangeTimeTo(LocalDateTime changeTimeTo) {
            this.changeTimeTo = changeTimeTo;
        }
        
        public String getChangeText() {
            return changeText;
        }
        
        public void setChangeText(String changeText) {
            this.changeText = changeText;
        }
        
        // Direct access to composition objects (preferred approach)
        public WindInformation getWindInformation() {
            return windInformation;
        }
        
        public void setWindInformation(WindInformation windInformation) {
            this.windInformation = windInformation != null ? windInformation : new WindInformation();
        }
        
        public WeatherConditions getWeatherConditions() {
            return weatherConditions;
        }
        
        public void setWeatherConditions(WeatherConditions weatherConditions) {
            this.weatherConditions = weatherConditions != null ? weatherConditions : new WeatherConditions();
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TafChangeGroup)) {
                return false;
            }
            TafChangeGroup that = (TafChangeGroup) o;
            return Objects.equals(changeType, that.getChangeType()) &&
                   Objects.equals(changeTimeFrom, that.getChangeTimeFrom()) &&
                   Objects.equals(changeTimeTo, that.getChangeTimeTo()) &&
                   Objects.equals(changeText, that.getChangeText()) &&
                   Objects.equals(windInformation, that.getWindInformation()) &&
                   Objects.equals(weatherConditions, that.getWeatherConditions());
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(changeType, changeTimeFrom, changeTimeTo, changeText, 
                              windInformation, weatherConditions);
        }
        
        @Override
        public String toString() {
            return String.format("TafChangeGroup{type='%s', from=%s, to=%s}", 
                               changeType, changeTimeFrom, changeTimeTo);
        }
    }
}
