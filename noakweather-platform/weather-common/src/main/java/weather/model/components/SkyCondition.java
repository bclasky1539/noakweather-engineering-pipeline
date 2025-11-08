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

/**
 * Immutable value object representing a sky condition (cloud layer).
 * 
 * Sky conditions describe cloud coverage, height, and type. Multiple layers
 * can be present in a single observation. This replaces the legacy HashMap-based
 * sky condition storage.
 * 
 * Examples:
 * - "FEW250" → SkyCondition(SkyCoverage.FEW, 25000, null)
 * - "SCT100" → SkyCondition(SkyCoverage.SCATTERED, 10000, null)
 * - "BKN050CB" → SkyCondition(SkyCoverage.BROKEN, 5000, "CB")
 * - "OVC020" → SkyCondition(SkyCoverage.OVERCAST, 2000, null)
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
     * Compact constructor with validation.
     */
    public SkyCondition {
        if (coverage == null) {
            throw new IllegalArgumentException("Sky coverage cannot be null");
        }
        
        // Height validation (if present, should be reasonable)
        if (heightFeet != null && (heightFeet < 0 || heightFeet > 100000)) {
            throw new IllegalArgumentException(
                "Cloud height out of reasonable range (0-100000 ft): " + heightFeet
            );
        }
        
        // For clear skies, height should be null
        if ((coverage == SkyCoverage.SKC || coverage == SkyCoverage.CLR || coverage == SkyCoverage.NSC) 
            && heightFeet != null) {
            throw new IllegalArgumentException(
                "Clear sky conditions should not have height: " + coverage
            );
        }
    }
    
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
     * Get height in meters.
     * 
     * @return height in meters, or null if not available
     */
    public Integer getHeightMeters() {
        if (heightFeet == null) {
            return null;
        }
        return (int) (heightFeet * 0.3048);
    }
    
    /**
     * Factory method for clear skies.
     * 
     * @return SkyCondition representing clear skies
     */
    public static SkyCondition clear() {
        return new SkyCondition(SkyCoverage.CLR, null, null);
    }
    
    /**
     * Factory method for sky clear.
     * 
     * @return SkyCondition representing sky clear
     */
    public static SkyCondition skyClear() {
        return new SkyCondition(SkyCoverage.SKC, null, null);
    }
}
