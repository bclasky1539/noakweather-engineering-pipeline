/*
 * Copyright 2025 bdeveloper.
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

/**
 * Enumeration of all supported weather data sources.
 * 
 * Each source has different characteristics:
 * - Data formats (XML, JSON)
 * - Update frequencies (real-time, hourly)
 * - Coverage areas (aviation-focused, global, regional)
 * - Data types (observations, forecasts, warnings)
 * 
 * @author bclasky1539
 *
 */
public enum WeatherDataSource {
    
    /**
     * NOAA Aviation Weather Center
     * - Primary source for aviation weather (METAR, TAF, PIREP)
     * - XML-based API
     * - US-focused with some international coverage
     * - High reliability, official government source
     */
    NOAA("NOAA Aviation Weather Center", "https://aviationweather.gov", true),
    
    /**
     * OpenWeatherMap
     * - Global current weather and forecasts
     * - JSON-based API
     * - Worldwide coverage
     * - Commercial API with free tier
     */
    OPENWEATHERMAP("OpenWeatherMap", "https://openweathermap.org", false),
    
    /**
     * WeatherAPI.com
     * - Global weather data provider
     * - JSON-based API
     * - Real-time and forecast data
     */
    WEATHERAPI("WeatherAPI", "https://weatherapi.com", false),
    
    /**
     * Visual Crossing
     * - Historical and forecast weather data
     * - JSON-based API
     * - Good for historical analysis
     */
    VISUAL_CROSSING("Visual Crossing", "https://visualcrossing.com", false),
    
    /**
     * Internal/synthetic data for testing
     */
    INTERNAL("Internal System", "internal", true),
    
    /**
     * Unknown or unspecified source
     */
    UNKNOWN("Unknown Source", "unknown", false);
    
    private final String displayName;
    private final String baseUrl;
    private final boolean isGovernmentSource;
    
    WeatherDataSource(String displayName, String baseUrl, boolean isGovernmentSource) {
        this.displayName = displayName;
        this.baseUrl = baseUrl;
        this.isGovernmentSource = isGovernmentSource;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public boolean isGovernmentSource() {
        return isGovernmentSource;
    }
    
    /**
     * Parse a source string into enum value (Java 17+ enhanced)
     * @param source
     * @return 
     */
    public static WeatherDataSource fromString(String source) {
        if (source == null || source.isBlank()) {
            return UNKNOWN;
        }
        
        try {
            return valueOf(source.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
