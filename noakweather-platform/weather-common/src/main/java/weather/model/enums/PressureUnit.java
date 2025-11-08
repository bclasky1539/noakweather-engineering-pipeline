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
 * Enumeration of atmospheric pressure units.
 * 
 * @author bclasky1539
 * 
 */
public enum PressureUnit {
    /** Inches of Mercury (US standard) */
    INCHES_HG("inHg"),
    
    /** Hectopascals / Millibars (ICAO standard) */
    HECTOPASCALS("hPa");
    
    private final String symbol;
    
    PressureUnit(String symbol) {
        this.symbol = symbol;
    }
    
    public String getSymbol() {
        return symbol;
    }
}
