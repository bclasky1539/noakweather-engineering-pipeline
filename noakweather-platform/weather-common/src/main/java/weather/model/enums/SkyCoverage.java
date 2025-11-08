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
package weather.model.enums;

/**
 * Sky coverage enumeration.
 * Represents the amount of sky covered by clouds in oktas (eighths).
 * 
 * @author bclasky1539
 * 
 */
public enum SkyCoverage {
    /** Sky Clear - 0 oktas (0/8 coverage) */
    SKC("SKC", 0),
    
    /** Clear - 0 oktas */
    CLR("CLR", 0),
    
    /** No Significant Cloud - 0 oktas */
    NSC("NSC", 0),
    
    /** Few - 1-2 oktas (1/8 to 2/8 coverage) */
    FEW("FEW", 1),
    
    /** Scattered - 3-4 oktas (3/8 to 4/8 coverage) */
    SCATTERED("SCT", 3),
    
    /** Broken - 5-7 oktas (5/8 to 7/8 coverage) */
    BROKEN("BKN", 5),
    
    /** Overcast - 8 oktas (full coverage) */
    OVERCAST("OVC", 8),
    
    /** Vertical Visibility (obscured sky) */
    VERTICAL_VISIBILITY("VV", 8);
    
    private final String code;
    private final int oktas;
    
    SkyCoverage(String code, int oktas) {
        this.code = code;
        this.oktas = oktas;
    }
    
    public String getCode() {
        return code;
    }
    
    public int getOktas() {
        return oktas;
    }
    
    /**
     * Parse sky coverage from METAR code.
     * 
     * @param code METAR code (e.g., "FEW", "SCT", "BKN", "OVC")
     * @return SkyCoverage enum value
     * @throws IllegalArgumentException if code is not recognized
     */
    public static SkyCoverage fromCode(String code) {
        for (SkyCoverage coverage : values()) {
            if (coverage.getCode().equals(code)) {
                return coverage;
            }
        }
        throw new IllegalArgumentException("Unknown sky coverage code: " + code);
    }
}
