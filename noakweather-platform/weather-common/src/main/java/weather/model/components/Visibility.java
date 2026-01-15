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
package weather.model.components;

import weather.utils.ValidationPatterns;

/**
 * Immutable value object representing visibility conditions.
 * <p>
 * Visibility can be reported in statute miles (SM), meters, or special conditions
 * like CAVOK (Ceiling And Visibility OK). This replaces the legacy HashMap-based
 * visibility storage.
 * <p>
 * Examples:
 * - "10SM" → Visibility(10.0, "SM", false, false, null)
 * - "9999" → Visibility(9999.0, "M", false, false, null)
 * - "M1/4SM" → Visibility(0.25, "SM", true, false, null)
 * - "P6SM" → Visibility(6.0, "SM", false, true, null)
 * - "CAVOK" → Visibility(null, null, false, false, "CAVOK")
 * 
 * @param distanceValue Visibility distance value, null for special conditions
 * @param unit Distance unit (SM=statute miles, M=meters, KM=kilometers)
 * @param lessThan True if visibility is less than stated value (e.g., M1/4SM)
 * @param greaterThan True if visibility is greater than stated value (e.g., P6SM)
 * @param specialCondition Special visibility condition (CAVOK, NDV=no directional variation)
 * 
 * @author bclasky1539
 * 
 */
public record Visibility(
    Double distanceValue,
    String unit,
    boolean lessThan,
    boolean greaterThan,
    String specialCondition
) {
    
    // ==================== Constants ====================
    
    /** Meters per statute mile conversion factor */
    public static final double METERS_PER_STATUTE_MILE = 1609.344;
    
    /** Meters per kilometer */
    public static final double METERS_PER_KILOMETER = 1000.0;
    
    /** Kilometers per statute mile conversion factor */
    public static final double KM_PER_STATUTE_MILE = 1.609344;
    
    /** Statute miles per kilometer conversion factor */
    public static final double SM_PER_KILOMETER = 0.621371;
    
    /** VFR minimum visibility in statute miles */
    public static final double VFR_MINIMUM_SM = 3.0;
    
    /** 3.0 */
    public static final double IFR_MAXIMUM_SM = 3.0;
    
    /** Low visibility threshold in statute miles */
    public static final double LOW_VISIBILITY_SM = 1.0;
    
    /** Unlimited visibility threshold in meters (10km or greater) */
    public static final double UNLIMITED_VISIBILITY_METERS = 10000.0;
    
    /** Unlimited visibility threshold in statute miles */
    public static final double UNLIMITED_VISIBILITY_SM = 6.0;
    
    /**
     * Compact constructor with validation.
 */
    public Visibility {
        validateDistanceValue(distanceValue);
        validateEitherDistanceOrSpecialCondition(distanceValue, specialCondition);
        validateUnitWhenDistancePresent(distanceValue, unit);
        validateSpecialCondition(specialCondition);
        validateModifiers(lessThan, greaterThan);
    }
    
    // ==================== Validation Helper Methods ====================
    
    /**
     * Validate that distance value is non-negative if present.
     */
    private static void validateDistanceValue(Double distanceValue) {
        if (distanceValue != null && distanceValue < 0) {
            throw new IllegalArgumentException(
                "Visibility distance cannot be negative: " + distanceValue
            );
        }
    }
    
    /**
     * Validate that either distance+unit OR specialCondition is present.
     */
    private static void validateEitherDistanceOrSpecialCondition(
            Double distanceValue, 
            String specialCondition) {
        if (distanceValue == null && specialCondition == null) {
            throw new IllegalArgumentException(
                "Visibility must have either distance or special condition"
            );
        }
    }
    
    /**
     * Validate unit when distance is present.
     */
    private static void validateUnitWhenDistancePresent(Double distanceValue, String unit) {
        if (distanceValue == null) {
            return;
        }
        
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException(
                "Visibility unit must be specified when distance is provided"
            );
        }
        
        if (!ValidationPatterns.VISIBILITY_UNIT.matcher(unit).matches()) {
            throw new IllegalArgumentException(
                "Invalid visibility unit: " + unit + ". Must be SM, M, or KM"
            );
        }
    }
    
    /**
     * Validate special condition if present.
     */
    private static void validateSpecialCondition(String specialCondition) {
        if (specialCondition == null || specialCondition.isBlank()) {
            return;
        }
        
        if (!ValidationPatterns.VISIBILITY_SPECIAL_CONDITION.matcher(specialCondition).matches()) {
            throw new IllegalArgumentException(
                "Invalid visibility special condition: " + specialCondition
            );
        }
    }
    
    /**
     * Validate that modifiers are not contradictory.
     */
    private static void validateModifiers(boolean lessThan, boolean greaterThan) {
        if (lessThan && greaterThan) {
            throw new IllegalArgumentException(
                "Visibility cannot be both less than and greater than"
            );
        }
    }
    
    // ==================== Query Methods ====================
    
    /**
     * Check if visibility is a special condition (CAVOK, NDV, etc.).
     * 
     * @return true if special condition is present
     */
    public boolean isSpecialCondition() {
        return specialCondition != null && !specialCondition.isBlank();
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
     * Check if visibility is unlimited (10km or greater / 6SM or greater).
     * 
     * @return true if visibility is considered unlimited
     */
    public boolean isUnlimited() {
        if (isCavok()) {
            return true;
        }
        
        // Use statute miles for SM unit, meters for others
        if ("SM".equals(unit) && distanceValue != null) {
            return distanceValue >= UNLIMITED_VISIBILITY_SM;
        }
        
        Double meters = toMeters();
        return meters != null && meters >= UNLIMITED_VISIBILITY_METERS;
    }
    
    /**
     * Check if visibility meets VFR (Visual Flight Rules) minimums.
     * VFR requires visibility of 3 statute miles or greater.
     * 
     * @return true if visibility meets VFR minimums
     */
    public boolean isVFR() {
        if (isCavok()) {
            return true;
        }
        
        Double sm = toStatuteMiles();
        if (sm == null) {
            return false;
        }
        
        // If it's "greater than" a value below VFR minimum, we can't be certain
        if (greaterThan) {
            return sm >= VFR_MINIMUM_SM;
        }
        
        // If it's "less than" any value, it's not VFR
        if (lessThan) {
            return false;
        }
        
        return sm >= VFR_MINIMUM_SM;
    }
    
    /**
     * Check if visibility is IFR (Instrument Flight Rules) conditions.
     * IFR is visibility less than 3 statute miles.
     * 
     * @return true if visibility is IFR conditions
     */
    public boolean isIFR() {
        if (isCavok()) {
            return false;
        }
        
        Double sm = toStatuteMiles();
        if (sm == null) {
            return false;
        }
        
        // If it's "less than" any value <= IFR max, it's IFR
        if (lessThan) {
            return true;
        }
        
        return sm < IFR_MAXIMUM_SM;
    }
    
    /**
     * Check if visibility is considered low (less than 1 statute mile).
     * 
     * @return true if visibility is low
     */
    public boolean isLowVisibility() {
        if (isCavok()) {
            return false;
        }
        
        Double sm = toStatuteMiles();
        if (sm == null) {
            return false;
        }
        
        // If it's "less than" any value, it's definitely low
        if (lessThan) {
            return true;
        }
        
        return sm < LOW_VISIBILITY_SM;
    }
    
    // ==================== Conversion Methods ====================
    
    /**
     * Convert visibility to meters for comparison.
     * 
     * @return visibility in meters, or null for special conditions
     * @throws IllegalStateException if unit is not recognized (should never happen due to constructor validation)
     */
    public Double toMeters() {
        if (distanceValue == null) {
            return null;
        }
        
        return switch (unit) {
            case "M" -> distanceValue;
            case "KM" -> distanceValue * METERS_PER_KILOMETER;
            case "SM" -> distanceValue * METERS_PER_STATUTE_MILE;
            // Defensive: should never reach here due to constructor validation
            default -> throw new IllegalStateException("Unknown unit: " + unit);
        };
    }
    
    /**
     * Convert visibility to statute miles for comparison.
     * 
     * @return visibility in statute miles, or null for special conditions
     * @throws IllegalStateException if unit is not recognized (should never happen due to constructor validation)
     */
    public Double toStatuteMiles() {
        if (distanceValue == null) {
            return null;
        }
        
        return switch (unit) {
            case "SM" -> distanceValue;
            case "M" -> distanceValue / METERS_PER_STATUTE_MILE;
            case "KM" -> distanceValue * SM_PER_KILOMETER;
            // Defensive: should never reach here due to constructor validation
            default -> throw new IllegalStateException("Unknown unit: " + unit);
        };
    }
    
    /**
     * Convert visibility to kilometers for comparison.
     * 
     * @return visibility in kilometers, or null for special conditions
     * @throws IllegalStateException if unit is not recognized (should never happen due to constructor validation)
     */
    public Double toKilometers() {
        if (distanceValue == null) {
            return null;
        }
        
        return switch (unit) {
            case "KM" -> distanceValue;
            case "M" -> distanceValue / METERS_PER_KILOMETER;
            case "SM" -> distanceValue * KM_PER_STATUTE_MILE;
            // Defensive: should never reach here due to constructor validation
            default -> throw new IllegalStateException("Unknown unit: " + unit);
        };
    }
    
    /**
     * Get human-readable summary of visibility conditions.
     * 
     * @return formatted visibility summary
     */
    public String getSummary() {
        if (isCavok()) {
            return "CAVOK (>10km, clear skies)";
        }
        
        if (specialCondition != null && !specialCondition.isBlank()) {
            return specialCondition;
        }
        
        if (distanceValue == null) {
            return "Unknown";
        }
        
        StringBuilder summary = new StringBuilder();
        
        if (lessThan) {
            summary.append("Less than ");
        } else if (greaterThan) {
            summary.append("Greater than ");
        }
        
        summary.append(String.format("%.2f", distanceValue));
        
        String unitText = switch (unit) {
            case "SM" -> " statute miles";
            case "M" -> " meters";
            case "KM" -> " kilometers";
            default -> " " + unit;
        };
        
        summary.append(unitText);
        
        // Add flight rules indicator
        if (isVFR()) {
            summary.append(" (VFR)");
        } else if (isIFR()) {
            summary.append(" (IFR)");
        }
        
        return summary.toString();
    }
    
    // ==================== Factory Methods ====================
    
    /**
     * Factory method for CAVOK visibility.
     * 
     * @return Visibility instance representing CAVOK
     */
    public static Visibility cavok() {
        return new Visibility(null, null, false, false, "CAVOK");
    }
    
    /**
     * Factory method for visibility in statute miles.
     * 
     * @param miles Distance in statute miles
     * @return Visibility instance
     */
    public static Visibility statuteMiles(double miles) {
        return new Visibility(miles, "SM", false, false, null);
    }
    
    /**
     * Factory method for visibility in meters.
     * 
     * @param meters Distance in meters
     * @return Visibility instance
     */
    public static Visibility meters(double meters) {
        return new Visibility(meters, "M", false, false, null);
    }
    
    /**
     * Factory method for visibility in kilometers.
     * 
     * @param kilometers Distance in kilometers
     * @return Visibility instance
     */
    public static Visibility kilometers(double kilometers) {
        return new Visibility(kilometers, "KM", false, false, null);
    }
    
    /**
     * Factory method for visibility with less than modifier.
     * 
     * @param distance Distance value
     * @param unit Unit (SM, M, KM)
     * @return Visibility instance with lessThan flag
     */
    public static Visibility lessThan(double distance, String unit) {
        return new Visibility(distance, unit, true, false, null);
    }
    
    /**
     * Factory method for visibility with greater than modifier.
     * 
     * @param distance Distance value
     * @param unit Unit (SM, M, KM)
     * @return Visibility instance with greaterThan flag
     */
    public static Visibility greaterThan(double distance, String unit) {
        return new Visibility(distance, unit, false, true, null);
    }
    
    /**
     * Factory method for standard visibility with all parameters.
     * 
     * @param distance Distance value
     * @param unit Unit (SM, M, KM)
     * @param lessThan Less than modifier
     * @param greaterThan Greater than modifier
     * @return Visibility instance
     */
    public static Visibility of(double distance, String unit, boolean lessThan, boolean greaterThan) {
        return new Visibility(distance, unit, lessThan, greaterThan, null);
    }
}
