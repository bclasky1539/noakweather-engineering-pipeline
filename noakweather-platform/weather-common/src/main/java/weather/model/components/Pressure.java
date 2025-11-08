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
 * Pressure can be reported in inches of mercury (InHg) or hectopascals (hPa).
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
    
    /**
     * Compact constructor with validation.
     */
    public Pressure {
        if (value == null) {
            throw new IllegalArgumentException("Pressure value cannot be null");
        }
        
        if (unit == null) {
            throw new IllegalArgumentException("Pressure unit cannot be null");
        }
        
        validatePressureRange(value, unit);
    }
    
    /**
     * Validate pressure is within reasonable bounds for the given unit.
     */
    private static void validatePressureRange(Double value, PressureUnit unit) {
        if (unit == PressureUnit.INCHES_HG && (value < 25.0 || value > 35.0)) {
            throw new IllegalArgumentException(
                "Pressure out of reasonable range (25-35 inHg): " + value
            );
        }
        
        if (unit == PressureUnit.HECTOPASCALS && (value < 850.0 || value > 1085.0)) {
            throw new IllegalArgumentException(
                "Pressure out of reasonable range (850-1085 hPa): " + value
            );
        }
    }
    
    /**
     * Convert pressure to inches of mercury.
     * 
     * @return pressure in inHg
     */
    public double toInchesHg() {
        return switch (unit) {
            case INCHES_HG -> value;
            case HECTOPASCALS -> value * 0.02953; // hPa to inHg conversion
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
            case INCHES_HG -> value * 33.8639; // inHg to hPa conversion
        };
    }
    
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
}
