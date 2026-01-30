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
package weather.model.components;

import weather.model.enums.PressureUnit;

/**
 * Immutable value object representing atmospheric pressure.
 * 
 * Pressure can be reported in inches of mercury (inHg) or hectopascals (hPa).
 * This replaces the legacy HashMap-based pressure storage.
 * 
 * Examples:
 * - "A3015" → Pressure(30.15, PressureUnit.INCHES_HG)
 * - "Q1013" → Pressure(1013.0, PressureUnit.HECTOPASCALS)
 * 
 * @param value Pressure value
 * @param unit Pressure unit
 * 
 * @author bclasky1539
 * 
 */
public record Pressure(
    Double value,
    PressureUnit unit
) {
    
    // ==================== Constants ====================
    
    /** Standard sea level pressure in inHg */
    public static final double STANDARD_PRESSURE_INHG = 29.92;
    
    /** Standard sea level pressure in hPa */
    public static final double STANDARD_PRESSURE_HPA = 1013.25;
    
    /** Minimum reasonable pressure in inHg (strongest recorded hurricane) */
    public static final double MIN_PRESSURE_INHG = 25.0;
    
    /** Maximum reasonable pressure in inHg (strongest high pressure system) */
    public static final double MAX_PRESSURE_INHG = 35.0;
    
    /** Minimum reasonable pressure in hPa */
    public static final double MIN_PRESSURE_HPA = 850.0;
    
    /** Maximum reasonable pressure in hPa */
    public static final double MAX_PRESSURE_HPA = 1085.0;
    
    /** Conversion factor: inHg to hPa */
    public static final double INHG_TO_HPA = 33.8639;
    
    /** Conversion factor: hPa to inHg */
    public static final double HPA_TO_INHG = 0.02953;
    
    /** Conversion factor: hPa to mb (millibars - same as hPa) */
    public static final double HPA_TO_MB = 1.0;
    
    /** Low pressure threshold in hPa (indicates potential storm) */
    public static final double LOW_PRESSURE_THRESHOLD_HPA = 1000.0;
    
    /** High pressure threshold in hPa (indicates fair weather) */
    public static final double HIGH_PRESSURE_THRESHOLD_HPA = 1020.0;
    
    /** Rapid pressure change threshold in hPa/3hrs (meteorological significance) */
    public static final double RAPID_PRESSURE_CHANGE_HPA = 6.0;
    
    // ==================== Compact Constructor ====================
    
    /**
     * Compact constructor with validation.
     */
    public Pressure {
        validateNotNull(value, unit);
        validatePressureRange(value, unit);
    }
    
    // ==================== Validation Helper Methods ====================
    
    /**
     * Validate that value and unit are not null.
     */
    private static void validateNotNull(Double value, PressureUnit unit) {
        if (value == null) {
            throw new IllegalArgumentException("Pressure value cannot be null");
        }
        
        if (unit == null) {
            throw new IllegalArgumentException("Pressure unit cannot be null");
        }
    }
    
    /**
     * Validate pressure is within reasonable bounds for the given unit.
     */
    private static void validatePressureRange(Double value, PressureUnit unit) {
        if (unit == PressureUnit.INCHES_HG && (value < MIN_PRESSURE_INHG || value > MAX_PRESSURE_INHG)) {
            throw new IllegalArgumentException(
                String.format("Pressure out of reasonable range (%.1f-%.1f inHg): %.2f", 
                             MIN_PRESSURE_INHG, MAX_PRESSURE_INHG, value)
            );
        }
        
        if (unit == PressureUnit.HECTOPASCALS && (value < MIN_PRESSURE_HPA || value > MAX_PRESSURE_HPA)) {
            throw new IllegalArgumentException(
                String.format("Pressure out of reasonable range (%.0f-%.0f hPa): %.1f", 
                             MIN_PRESSURE_HPA, MAX_PRESSURE_HPA, value)
            );
        }
    }
    
    // ==================== Conversion Methods ====================
    
    /**
     * Convert pressure to inches of mercury.
     * 
     * @return pressure in inHg
     */
    public double toInchesHg() {
        return switch (unit) {
            case INCHES_HG -> value;
            case HECTOPASCALS -> value * HPA_TO_INHG;
        };
    }
    
    /**
     * Convert pressure to hectopascals.
     * 
     * @return pressure in hPa
     */
    public double toHectopascals() {
        return switch (unit) {
            case HECTOPASCALS -> value;
            case INCHES_HG -> value * INHG_TO_HPA;
        };
    }
    
    /**
     * Convert pressure to millibars (same as hectopascals).
     * Included for compatibility with older aviation reports.
     * 
     * @return pressure in mb (equivalent to hPa)
     */
    public double toMillibars() {
        return toHectopascals() * HPA_TO_MB;
    }
    
    // ==================== Query Methods ====================
    
    /**
     * Calculate deviation from standard sea level pressure.
     * Positive values indicate higher than standard pressure.
     * 
     * @return deviation in hPa
     */
    public double getDeviationFromStandard() {
        return toHectopascals() - STANDARD_PRESSURE_HPA;
    }
    
    /**
     * Check if pressure is below normal (< 1013.25 hPa).
     * 
     * @return true if below standard pressure
     */
    public boolean isBelowStandard() {
        return toHectopascals() < STANDARD_PRESSURE_HPA;
    }
    
    /**
     * Check if pressure is above normal (> 1013.25 hPa).
     * 
     * @return true if above standard pressure
     */
    public boolean isAboveStandard() {
        return toHectopascals() > STANDARD_PRESSURE_HPA;
    }
    
    /**
     * Check if pressure indicates low pressure system (< 1000 hPa).
     * Low pressure typically associated with storms and precipitation.
     * 
     * @return true if low pressure
     */
    public boolean isLowPressure() {
        return toHectopascals() < LOW_PRESSURE_THRESHOLD_HPA;
    }
    
    /**
     * Check if pressure indicates high pressure system (> 1020 hPa).
     * High pressure typically associated with fair weather.
     * 
     * @return true if high pressure
     */
    public boolean isHighPressure() {
        return toHectopascals() > HIGH_PRESSURE_THRESHOLD_HPA;
    }
    
    /**
     * Check if pressure is extremely low (< 950 hPa).
     * Typically indicates severe storm or hurricane conditions.
     * 
     * @return true if extremely low pressure
     */
    public boolean isExtremelyLow() {
        return toHectopascals() < 950.0;
    }
    
    /**
     * Check if pressure is extremely high (> 1040 hPa).
     * Indicates strong high pressure system.
     * 
     * @return true if extremely high pressure
     */
    public boolean isExtremelyHigh() {
        return toHectopascals() > 1040.0;
    }
    
    // ==================== Aviation-Specific Methods ====================
    
    /**
     * Calculate altimeter setting correction from sea level pressure.
     * Used in aviation to adjust altimeter readings.
     * 
     * @param elevationFeet Field elevation in feet above MSL
     * @return altimeter setting in inHg
     */
    public double getAltimeterSetting(double elevationFeet) {
        // Use standard atmosphere lapse rate
        // Altimeter setting = station pressure + (elevation / 1000) * 1 inHg (approximate)
        double stationPressureInHg = toInchesHg();
        double correction = (elevationFeet / 1000.0);
        return stationPressureInHg + correction;
    }
    
    /**
     * Calculate pressure altitude in feet.
     * Pressure altitude is the altitude in the standard atmosphere where this pressure occurs.
     * 
     * Formula: PA = 145366.45 * (1 - (P/1013.25)^0.190284)
     * 
     * @return pressure altitude in feet
     */
    public double getPressureAltitudeFeet() {
        double pressureHpa = toHectopascals();
        double ratio = pressureHpa / STANDARD_PRESSURE_HPA;
        return 145366.45 * (1 - Math.pow(ratio, 0.190284));
    }
    
    /**
     * Calculate density altitude given temperature.
     * Density altitude affects aircraft performance.
     * 
     * Formula: DA = PA + [120 * (OAT - ISA)]
     * Where ISA = 15°C - (2°C * PA/1000 ft)
     * 
     * @param temperatureCelsius Outside air temperature in Celsius
     * @return density altitude in feet
     */
    public double getDensityAltitude(double temperatureCelsius) {
        double pressureAltitude = getPressureAltitudeFeet();
        double isaTemperature = 15.0 - (2.0 * pressureAltitude / 1000.0);
        double temperatureDeviation = temperatureCelsius - isaTemperature;
        return pressureAltitude + (120.0 * temperatureDeviation);
    }
    
    // ==================== Meteorological Analysis Methods ====================
    
    /**
     * Calculate pressure tendency (change over time).
     * Positive values indicate rising pressure, negative indicates falling.
     * 
     * @param previousPressure Previous pressure reading
     * @return pressure change in hPa (current - previous)
     */
    public double getPressureTendency(Pressure previousPressure) {
        if (previousPressure == null) {
            throw new IllegalArgumentException("Previous pressure cannot be null");
        }
        return toHectopascals() - previousPressure.toHectopascals();
    }
    
    /**
     * Check if pressure change indicates rapid change (>= 6 hPa in 3 hours).
     * Rapid pressure changes are meteorologically significant.
     * 
     * @param previousPressure Previous pressure reading (3 hours ago)
     * @return true if rapid pressure change detected
     */
    public boolean isRapidPressureChange(Pressure previousPressure) {
        if (previousPressure == null) {
            return false;
        }
        double change = Math.abs(getPressureTendency(previousPressure));
        return change >= RAPID_PRESSURE_CHANGE_HPA;
    }
    
    /**
     * Get pressure tendency description.
     * 
     * @param previousPressure Previous pressure reading
     * @return description of pressure tendency
     */
    public String getPressureTendencyDescription(Pressure previousPressure) {
        double tendency = getPressureTendency(previousPressure);
        
        if (tendency > 3.0) {
            return "Rapidly rising";
        } else if (tendency > 1.0) {
            return "Rising";
        } else if (tendency > -1.0) {
            return "Steady";
        } else if (tendency > -3.0) {
            return "Falling";
        } else {
            return "Rapidly falling";
        }
    }
    
    /**
     * Get weather condition based on pressure and tendency.
     * 
     * @param previousPressure Previous pressure reading (can be null)
     * @return likely weather condition description
     */
    public String getWeatherCondition(Pressure previousPressure) {
        if (previousPressure == null) {
            return getBaseWeatherCondition();
        }
        
        return getWeatherConditionWithTendency(previousPressure);
    }
    
    private String getBaseWeatherCondition() {
        double pressureHpa = toHectopascals();

        // Base assessment only on current pressure
        if (pressureHpa < 980.0) {
            return "Stormy conditions likely";
        } else if (pressureHpa < 1000.0) {
            return "Unsettled weather likely";
        } else if (pressureHpa > 1030.0) {
            return "Fair weather likely";
        } else {
            return "Generally fair conditions";
        }
    }
    
    private String getWeatherConditionWithTendency(Pressure previousPressure) {
        double pressureHpa = toHectopascals();
        double tendency = getPressureTendency(previousPressure);
        
        // Analyze combination of pressure and tendency
        if (pressureHpa < 1000.0) {
            return getLowPressureCondition(tendency);
        }
        if (pressureHpa > 1020.0) {
            return getHighPressureCondition(tendency);
        }
        return getTendencyOnlyCondition(tendency);
    }
    
    private String getLowPressureCondition(double tendency) {
        if (tendency < -2.0) {
            return "Deteriorating weather, storm approaching";
        }
        if (tendency > 2.0) {
            return "Improving weather, storm clearing";
        }
        return getTendencyOnlyCondition(tendency);
    }
    
    private String getHighPressureCondition(double tendency) {
        if (tendency > 1.0) {
            return "Fair weather, becoming more settled";
        }
        if (tendency < -1.0) {
            return "Fair weather, may deteriorate";
        }
        return getTendencyOnlyCondition(tendency);
    }
    
    private String getTendencyOnlyCondition(double tendency) {
        if (tendency < -3.0) {
            return "Weather deteriorating";
        }
        if (tendency > 3.0) {
            return "Weather improving";
        }
        return "Weather conditions stable";
    }
    
    // ==================== Formatting Methods ====================
    
    /**
     * Get formatted pressure string with unit.
     * 
     * @return formatted pressure (e.g., "30.15 inHg", "1013 hPa")
     */
    public String getFormattedValue() {
        return switch (unit) {
            case INCHES_HG -> String.format("%.2f %s", value, unit.getSymbol());
            case HECTOPASCALS -> String.format("%.0f %s", value, unit.getSymbol());
        };
    }
    
    /**
     * Get METAR-style altimeter setting format (Axxxx).
     * 
     * @return METAR altimeter string (e.g., "A3015")
     */
    public String toMetarAltimeter() {
        double inHg = toInchesHg();
        int value = (int) Math.round(inHg * 100);
        return String.format("A%04d", value);
    }
    
    /**
     * Get METAR-style QNH format (Qxxxx).
     * 
     * @return METAR QNH string (e.g., "Q1013")
     */
    public String toMetarQNH() {
        int hpa = (int) Math.round(toHectopascals());
        return String.format("Q%04d", hpa);
    }
    
    /**
     * Get detailed pressure summary.
     * 
     * @return formatted pressure summary with conversions
     */
    public String getSummary() {
        return String.format("%s (%.2f inHg, %.0f hPa)", 
                           getFormattedValue(), 
                           toInchesHg(), 
                           toHectopascals());
    }
    
    // ==================== Factory Methods ====================
    
    /**
     * Factory method for inches of mercury.
     * 
     * @param inHg Pressure in inches of mercury
     * @return Pressure instance
     */
    public static Pressure inchesHg(double inHg) {
        return new Pressure(inHg, PressureUnit.INCHES_HG);
    }
    
    /**
     * Factory method for hectopascals.
     * 
     * @param hPa Pressure in hectopascals
     * @return Pressure instance
     */
    public static Pressure hectopascals(double hPa) {
        return new Pressure(hPa, PressureUnit.HECTOPASCALS);
    }
    
    /**
     * Factory method for standard sea level pressure.
     * 
     * @return Standard pressure (1013.25 hPa / 29.92 inHg)
     */
    public static Pressure standard() {
        return new Pressure(STANDARD_PRESSURE_HPA, PressureUnit.HECTOPASCALS);
    }
    
    /**
     * Factory method from METAR altimeter setting (Axxxx).
     * 
     * @param altimeter METAR altimeter string (e.g., "A3015")
     * @return Pressure instance
     */
    public static Pressure fromMetarAltimeter(String altimeter) {
        if (altimeter == null || !altimeter.matches("A\\d{4}")) {
            throw new IllegalArgumentException("Invalid METAR altimeter format: " + altimeter);
        }
        double inHg = Integer.parseInt(altimeter.substring(1)) / 100.0;
        return new Pressure(inHg, PressureUnit.INCHES_HG);
    }
    
    /**
     * Factory method from METAR QNH (Qxxxx).
     * 
     * @param qnh METAR QNH string (e.g., "Q1013")
     * @return Pressure instance
     */
    public static Pressure fromMetarQNH(String qnh) {
        if (qnh == null || !qnh.matches("Q\\d{4}")) {
            throw new IllegalArgumentException("Invalid METAR QNH format: " + qnh);
        }
        double hPa = Integer.parseInt(qnh.substring(1));
        return new Pressure(hPa, PressureUnit.HECTOPASCALS);
    }

    /**
     * Factory method for creating pressure in inches of mercury.
     *
     * @param value Pressure value in inHg
     * @return Pressure instance
     */
    public static Pressure ofInchesHg(double value) {
        return new Pressure(value, PressureUnit.INCHES_HG);
    }
}
