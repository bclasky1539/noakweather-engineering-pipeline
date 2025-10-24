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
package weather.processing.parser.noaa;

import weather.model.NoaaWeatherData;
import weather.processing.parser.common.WeatherParser;
import weather.processing.parser.common.ParseResult;

import java.time.Instant;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Parser for NOAA METAR (Meteorological Aerodrome Report) data.
 * 
 * COPIED from legacy parsing logic and ADAPTED to use new platform interfaces.
 * Original legacy parser remains unchanged in noakweather-legacy/.
 * 
 * METAR Format Example:
 * "METAR KJFK 251651Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 SLP210"
 * 
 * Components:
 * - METAR: Report type
 * - KJFK: Station identifier (JFK Airport)
 * - 251651Z: Day (25th) and time (16:51 UTC)
 * - 28016KT: Wind from 280° at 16 knots
 * - 10SM: Visibility 10 statute miles
 * - FEW250: Few clouds at 25,000 feet
 * - 22/12: Temperature 22°C, Dewpoint 12°C
 * - A3015: Altimeter setting 30.15 inHg
 * - RMK: Remarks section
 * 
 * @author bclasky1539
 *
 */
public class NoaaMetarParser implements WeatherParser<NoaaWeatherData> {
    
    // Regex pattern for extracting station ID from METAR
    private static final Pattern STATION_PATTERN = Pattern.compile("METAR\\s+([A-Z]{4})");
    
    @Override
    public ParseResult<NoaaWeatherData> parse(String rawData) {
        if (rawData == null || rawData.trim().isEmpty()) {
            return ParseResult.failure("Raw data cannot be null or empty");
        }
        
        if (!canParse(rawData)) {
            return ParseResult.failure("Data is not a valid METAR report");
        }
        
        try {
            // Extract station ID first
            String stationId = extractStationId(rawData);
            if (stationId == null) {
                return ParseResult.failure("Could not extract station ID from METAR");
            }
            
            // Create NoaaWeatherData instance
            // Using current time as observation time for now
            Instant observationTime = Instant.now();
            NoaaWeatherData weatherData = new NoaaWeatherData(stationId, observationTime, "METAR");
            
            // Set the raw data
            weatherData.setRawData(rawData.trim());
            
            // Additional parsing logic can be added here as needed:
            // - Parse observation timestamp from METAR format (DDHHmmZ)
            // - Extract temperature and dewpoint
            // - Extract wind speed and direction
            // - Extract visibility
            // - Extract pressure/altimeter setting
            // - Extract cloud cover information
            // - Set geographic location if station coordinates are available
            
            return ParseResult.success(weatherData);
            
        } catch (Exception e) {
            return ParseResult.failure(
                "Failed to parse METAR data: " + e.getMessage(), 
                e
            );
        }
    }
    
    @Override
    public boolean canParse(String rawData) {
        if (rawData == null || rawData.trim().isEmpty()) {
            return false;
        }
        return rawData.trim().startsWith("METAR");
    }
    
    @Override
    public String getSourceType() {
        return "NOAA_METAR";
    }
    
    /**
     * Extract station ID from METAR report.
     * 
     * @param rawData The raw METAR string
     * @return The 4-letter ICAO station identifier, or null if not found
     */
    private String extractStationId(String rawData) {
        Matcher matcher = STATION_PATTERN.matcher(rawData);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
