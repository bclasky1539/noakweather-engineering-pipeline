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

import weather.model.enums.SkyCoverage;
import weather.utils.ValidationPatterns;

/**
 * Immutable value object representing a sky condition (cloud layer).
 * 
 * Sky conditions describe cloud coverage, height, and type. Multiple layers
 * can be present in a single observation. This replaces the legacy HashMap-based
 * sky condition storage.
 * 
 * Examples:
 * 
 *   "FEW250" → SkyCondition(SkyCoverage.FEW, 25000, null)
 *   "SCT100" → SkyCondition(SkyCoverage.SCATTERED, 10000, null)
 *   "BKN050CB" → SkyCondition(SkyCoverage.BROKEN, 5000, "CB")
 *   "OVC020" → SkyCondition(SkyCoverage.OVERCAST, 2000, null)
 * 
 * 
 * @param coverage Cloud coverage amount
 * @param heightFeet Cloud base height in feet AGL (Above Ground Level)
 * @param cloudType Cloud type (CB=Cumulonimbus, TCU=Towering Cumulus, etc.)
 * 
 * @author bclasky1539
 * 
 */
public record SkyCondition(
    SkyCoverage coverage,
    Integer heightFeet,
    String cloudType
) {
    
    /**
     * Minimum valid cloud height in feet.
     */
    private static final int MIN_CLOUD_HEIGHT_FEET = 0;
    
    /**
     * Maximum valid cloud height in feet (~30km, above all weather).
     */
    private static final int MAX_CLOUD_HEIGHT_FEET = 100000;
    
    /**
     * Compact constructor with validation.
     */
    public SkyCondition {
        if (coverage == null) {
            throw new IllegalArgumentException("Sky coverage cannot be null");
        }
        
        // Height validation (if present, should be reasonable)
        if (heightFeet != null && (heightFeet < MIN_CLOUD_HEIGHT_FEET || heightFeet > MAX_CLOUD_HEIGHT_FEET)) {
            throw new IllegalArgumentException(
                "Cloud height out of reasonable range (" + MIN_CLOUD_HEIGHT_FEET + 
                "-" + MAX_CLOUD_HEIGHT_FEET + " ft): " + heightFeet
            );
        }
        
        // For clear skies, height should be null
        if ((coverage == SkyCoverage.SKC || coverage == SkyCoverage.CLR || coverage == SkyCoverage.NSC) 
            && heightFeet != null) {
            throw new IllegalArgumentException(
                "Clear sky conditions should not have height: " + coverage
            );
        }
        
        // Vertical visibility should have height
        if (coverage == SkyCoverage.VERTICAL_VISIBILITY && heightFeet == null) {
            throw new IllegalArgumentException(
                "Vertical visibility must have height specified"
            );
        }
        
        // Validate cloud type if present
        validateCloudType(cloudType);
    }
    
    /**
     * Validate cloud type against known aviation cloud types.
     * 
     * @param cloudType the cloud type to validate
     * @throws IllegalArgumentException if cloud type is invalid
     */
    private static void validateCloudType(String cloudType) {
        if (cloudType != null && !cloudType.isBlank()) {
            String upper = cloudType.trim().toUpperCase();
            if (!ValidationPatterns.CLOUD_TYPE.matcher(upper).matches()) {
                throw new IllegalArgumentException(
                    "Invalid cloud type: " + cloudType + 
                    " (valid types: CB, TCU, CU, SC, ST, NS, AS, AC, CI, CC, CS)"
                );
            }
        }
    }
    
    // ==================== Query Methods ====================
    
    /**
     * Check if this is a clear sky condition.
     * 
     * @return true if sky is clear (SKC, CLR, NSC)
     */
    public boolean isClear() {
        return coverage == SkyCoverage.SKC 
            || coverage == SkyCoverage.CLR 
            || coverage == SkyCoverage.NSC;
    }
    
    /**
     * Check if this layer represents a ceiling.
     * A ceiling is the lowest BKN or OVC layer, or vertical visibility.
     * 
     * @return true if this is a ceiling layer
     */
    public boolean isCeiling() {
        return coverage == SkyCoverage.BROKEN 
            || coverage == SkyCoverage.OVERCAST 
            || coverage == SkyCoverage.VERTICAL_VISIBILITY;
    }
    
    /**
     * Check if clouds are cumulonimbus (CB).
     * 
     * @return true if cloud type is CB
     */
    public boolean isCumulonimbus() {
        return "CB".equals(cloudType);
    }
    
    /**
     * Check if clouds are towering cumulus (TCU).
     * 
     * @return true if cloud type is TCU
     */
    public boolean isToweringCumulus() {
        return "TCU".equals(cloudType);
    }
    
    /**
     * Check if this represents significant convective activity.
     * Convective clouds include cumulonimbus (CB) and towering cumulus (TCU).
     * 
     * @return true if CB or TCU present
     */
    public boolean isConvective() {
        return isCumulonimbus() || isToweringCumulus();
    }
    
    /**
     * Check if this layer is below a given altitude.
     * 
     * @param altitudeFeet altitude in feet
     * @return true if cloud base is below the given altitude
     */
    public boolean isBelowAltitude(int altitudeFeet) {
        if (heightFeet == null) {
            return false;
        }
        return heightFeet < altitudeFeet;
    }
    
    // ==================== Conversion Methods ====================
    
    /**
     * Get height in meters.
     * 
     * @return height in meters, or null if not available
     */
    public Integer getHeightMeters() {
        if (heightFeet == null) {
            return null;
        }
        return (int) Math.round(heightFeet * 0.3048);
    }
    
    /**
     * Get a human-readable summary of the sky condition.
     * 
     * Examples:
     * 
     *   "Clear"
     *   "Scattered at 10000 feet"
     *   "Broken at 5000 feet (CB)"
     *   "Overcast at 2000 feet"
     * 
     * 
     * @return formatted string describing the sky condition
     */
    public String getSummary() {
        String description = formatCoverageDescription(coverage);
        
        if (isClear()) {
            return description;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(description);
        
        if (heightFeet != null) {
            sb.append(" at ").append(heightFeet).append(" feet");
        }
        
        if (cloudType != null && !cloudType.isBlank()) {
            sb.append(" (").append(cloudType).append(")");
        }
        
        return sb.toString();
    }
    
    /**
     * Format sky coverage enum as human-readable description.
     * 
     * @param coverage the sky coverage enum
     * @return human-readable description
     */
    private static String formatCoverageDescription(SkyCoverage coverage) {
        return switch (coverage) {
            case SKC -> "Sky Clear";
            case CLR -> "Clear";
            case NSC -> "No Significant Clouds";
            case FEW -> "Few";
            case SCATTERED -> "Scattered";
            case BROKEN -> "Broken";
            case OVERCAST -> "Overcast";
            case VERTICAL_VISIBILITY -> "Vertical Visibility";
        };
    }
    
    // ==================== Factory Methods ====================
    
    /**
     * Factory method for a simple cloud layer without cloud type.
     * 
     * @param coverage sky coverage
     * @param heightFeet cloud base height in feet
     * @return new SkyCondition
     */
    public static SkyCondition of(SkyCoverage coverage, int heightFeet) {
        return new SkyCondition(coverage, heightFeet, null);
    }
    
    /**
     * Factory method for a cloud layer with type.
     * 
     * @param coverage sky coverage
     * @param heightFeet cloud base height in feet
     * @param cloudType cloud type (CB, TCU, etc.)
     * @return new SkyCondition
     */
    public static SkyCondition of(SkyCoverage coverage, int heightFeet, String cloudType) {
        return new SkyCondition(coverage, heightFeet, cloudType);
    }
    
    /**
     * Factory method for vertical visibility.
     * 
     * @param heightFeet vertical visibility in feet
     * @return new SkyCondition with vertical visibility
     */
    public static SkyCondition verticalVisibility(int heightFeet) {
        return new SkyCondition(SkyCoverage.VERTICAL_VISIBILITY, heightFeet, null);
    }
    
    /**
     * Factory method for clear skies (CLR).
     * 
     * @return SkyCondition representing clear skies
     */
    public static SkyCondition clear() {
        return new SkyCondition(SkyCoverage.CLR, null, null);
    }
    
    /**
     * Factory method for sky clear (SKC).
     * 
     * @return SkyCondition representing sky clear
     */
    public static SkyCondition skyClear() {
        return new SkyCondition(SkyCoverage.SKC, null, null);
    }
}
