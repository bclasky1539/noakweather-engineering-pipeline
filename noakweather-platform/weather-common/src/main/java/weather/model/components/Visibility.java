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

/**
 * Immutable value object representing visibility conditions.
 * 
 * Visibility can be reported in statute miles (SM), meters, or special conditions
 * like CAVOK (Ceiling And Visibility OK). This replaces the legacy HashMap-based
 * visibility storage.
 * 
 * Examples:
 * - "10SM" → Visibility(10.0, "SM", null)
 * - "9999" → Visibility(9999.0, "M", null)
 * - "1 1/2SM" → Visibility(1.5, "SM", null)
 * - "CAVOK" → Visibility(null, null, "CAVOK")
 * 
 * @param distanceValue Visibility distance value, null for special conditions
 * @param unit Distance unit (SM=statute miles, M=meters, KM=kilometers)
 * @param specialCondition Special visibility condition (CAVOK, NDV=no directional variation)
 * 
 * @author bclasky1539
 * 
 */
public record Visibility(
    Double distanceValue,
    String unit,
    String specialCondition
) {
    
    /**
     * Compact constructor with validation.
     */
    public Visibility {
        // Validate distance is non-negative if present
        if (distanceValue != null && distanceValue < 0) {
            throw new IllegalArgumentException(
                "Visibility distance cannot be negative: " + distanceValue
            );
        }
        
        // Either distance+unit OR specialCondition should be present
        if (distanceValue == null && specialCondition == null) {
            throw new IllegalArgumentException(
                "Visibility must have either distance or special condition"
            );
        }
        
        // If distance is present, unit must be present
        if (distanceValue != null && (unit == null || unit.isBlank())) {
            throw new IllegalArgumentException(
                "Visibility unit must be specified when distance is provided"
            );
        }
    }
    
    /**
     * Check if visibility is a special condition (CAVOK, NDV, etc.).
     * 
     * @return true if special condition is present
     */
    public boolean isSpecialCondition() {
        return specialCondition != null;
    }
    
    /**
     * Check if visibility is CAVOK (Ceiling And Visibility OK).
     * CAVOK means: visibility ≥10km, no clouds below 5000ft, no CB, no significant weather.
     * 
     * @return true if CAVOK
     */
    public boolean isCavok() {
        return "CAVOK".equals(specialCondition);
    }
    
    /**
     * Convert visibility to meters for comparison.
     * 
     * @return visibility in meters, or null for special conditions
     */
    public Double toMeters() {
        if (distanceValue == null) {
            return null;
        }
        
        return switch (unit) {
            case "M" -> distanceValue;
            case "KM" -> distanceValue * 1000.0;
            case "SM" -> distanceValue * 1609.34; // statute miles to meters
            default -> distanceValue;
        };
    }
    
    /**
     * Convert visibility to statute miles for comparison.
     * 
     * @return visibility in statute miles, or null for special conditions
     */
    public Double toStatuteMiles() {
        if (distanceValue == null) {
            return null;
        }
        
        return switch (unit) {
            case "SM" -> distanceValue;
            case "M" -> distanceValue / 1609.34;
            case "KM" -> distanceValue * 0.621371;
            default -> distanceValue;
        };
    }
    
    /**
     * Factory method for CAVOK visibility.
     * 
     * @return Visibility instance representing CAVOK
     */
    public static Visibility cavok() {
        return new Visibility(null, null, "CAVOK");
    }
    
    /**
     * Factory method for visibility in statute miles.
     * 
     * @param miles Distance in statute miles
     * @return Visibility instance
     */
    public static Visibility statuteMiles(double miles) {
        return new Visibility(miles, "SM", null);
    }
    
    /**
     * Factory method for visibility in meters.
     * 
     * @param meters Distance in meters
     * @return Visibility instance
     */
    public static Visibility meters(double meters) {
        return new Visibility(meters, "M", null);
    }
}
