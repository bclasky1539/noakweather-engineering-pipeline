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
package weather.processing.parser.noaa;

import weather.utils.IndexedLinkedHashMap;
import java.util.regex.Pattern;

/**
 * Registry of regex patterns and their handlers for METAR/TAF parsing.
 * 
 * Uses IndexedLinkedHashMap to maintain pattern order and provide index-based access.
 * Pattern order is critical for sequential parsing - patterns are checked in sequence.
 * 
 * Three types of registries:
 * - Main handlers: Core METAR body (before RMK)
 * - Remarks handlers: Remarks section (after RMK)
 * - Group handlers: TAF groups (BECMG, TEMPO, FM, PROB)
 * 
 * @author bclasky1539
 * 
 */
public class NoaaAviationWeatherPatternRegistry {
    
    /**
     * Get handlers for main METAR body (before RMK).
     * Patterns are ordered by typical METAR sequence.
     * 
     * METAR Format:
     * TYPE STATION DAY/TIME MODIFIER WIND VISIBILITY RVR WEATHER SKY TEMP PRESSURE NOSIG
     * 
     * @return IndexedLinkedHashMap maintaining insertion order with index access
     */
    public IndexedLinkedHashMap<Pattern, NoaaAviationWeatherPatternHandler> getMainHandlers() {
        IndexedLinkedHashMap<Pattern, NoaaAviationWeatherPatternHandler> handlers = new IndexedLinkedHashMap<>();
        
        // Aviation Weather type (usually required)
        handlers.put(Pattern.compile("^(METAR|SPECI)\\s+"), 
                NoaaAviationWeatherPatternHandler.single("reportType"));
        
        // Optional month/day/year at start (some formats)
        handlers.put(RegExprConst.MONTH_DAY_YEAR_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("monthDayYear"));
        
        // Station ID and observation time (required)
        handlers.put(RegExprConst.STATION_DAY_TIME_VALTMPER_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("station"));

        // Station ID and observation time (required)
        handlers.put(RegExprConst.STATION_AND_ISSUE_TIME_PATTERN,
                NoaaAviationWeatherPatternHandler.single("handleStationAndIssueTime"));

        // Report modifier (AUTO, COR, etc.) - optional
        handlers.put(RegExprConst.REPORT_MODIFIER_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("reportModifier"));
        
        // Wind information (usually required)
        handlers.put(RegExprConst.WIND_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("wind"));
        
        // Visibility (required)
        handlers.put(RegExprConst.VISIBILITY_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("visibility"));
        
        // Runway Visual Range (can have multiple runways)
        handlers.put(RegExprConst.RUNWAY_PATTERN, 
                NoaaAviationWeatherPatternHandler.repeating("runway"));
        
        // Present weather (can have multiple phenomena: -RA BR)
        handlers.put(RegExprConst.PRESENT_WEATHER_PATTERN, 
                NoaaAviationWeatherPatternHandler.repeating("presentWeather"));
        
        // Sky conditions (can have multiple cloud layers: FEW250 SCT100 BKN050)
        handlers.put(RegExprConst.SKY_CONDITION_PATTERN, 
                NoaaAviationWeatherPatternHandler.repeating("skyCondition"));
        
        // Temperature and dewpoint (required)
        handlers.put(RegExprConst.TEMP_DEWPOINT_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("tempDewpoint"));
        
        // Altimeter/pressure (required)
        handlers.put(RegExprConst.ALTIMETER_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("altimeter"));
        
        // No significant change indicator (optional)
        handlers.put(RegExprConst.NO_SIG_CHANGE_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("noSigChange"));
        
        // Catch unparsed tokens
        handlers.put(RegExprConst.UNPARSED_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("unparsed"));
        
        return handlers;
    }
    
    /**
     * Get handlers for METAR remarks section (after RMK).
     * 
     * Remarks contain additional information such as:
     * - Automated station type (AO1, AO2)
     * - Sea level pressure (SLP)
     * - Peak wind, wind shift
     * - Precise temperature/dewpoint
     * - Precipitation amounts
     * - Pressure tendency
     * 
     * @return IndexedLinkedHashMap maintaining insertion order with index access
     */
    public IndexedLinkedHashMap<Pattern, NoaaAviationWeatherPatternHandler> getRemarksHandlers() {
        IndexedLinkedHashMap<Pattern, NoaaAviationWeatherPatternHandler> handlers = new IndexedLinkedHashMap<>();
        
        // Automated station type (AO1 or AO2)
        handlers.put(RegExprConst.AUTO_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("autoType"));
        
        // Sea level pressure (SLP)
        handlers.put(RegExprConst.SEALVL_PRESS_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("seaLevelPressure"));
        
        // Peak wind
        handlers.put(RegExprConst.PEAK_WIND_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("peakWind"));
        
        // Wind shift
        handlers.put(RegExprConst.WIND_SHIFT_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("windShift"));
        
        // Tower or surface visibility (NOT HANDLED)

        // Variable visibility
        handlers.put(RegExprConst.VPV_SV_VSL_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("variableVis"));

        // Variable ceiling
        handlers.put(RegExprConst.VARIABLE_CEILING_PATTERN,
                NoaaAviationWeatherPatternHandler.single("variableCeiling"));

        // Ceiling second site (MUST be after variable ceiling)
        handlers.put(RegExprConst.CEILING_SECOND_SITE_PATTERN,
                NoaaAviationWeatherPatternHandler.single("ceilingSecondSite"));

        // Obscuration layers (can have multiple)
        handlers.put(RegExprConst.OBSCURATION_PATTERN,
                NoaaAviationWeatherPatternHandler.repeating("obscurationLayers"));

        // Thunderstorm/cloud location
        handlers.put(RegExprConst.TS_CLD_LOC_PATTERN,
                NoaaAviationWeatherPatternHandler.repeating("thunderstormCloudLocation"));

        // Cloud types (can have multiple)
        handlers.put(RegExprConst.CLOUD_OKTA_PATTERN,
                NoaaAviationWeatherPatternHandler.repeating("cloudTypes"));

        // Lightning (uses wrapper class for individual type captures)
        handlers.put(RegExprConst.LIGHTNING_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("lightning"));
        
        // Pressure rising/falling rapidly
        handlers.put(RegExprConst.PRES_RF_RAPDLY_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("pressureRapidly"));
        
        // Temperature precision (T02330139)
        handlers.put(RegExprConst.TEMP_1HR_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("temp1Hour"));
        
        // Hourly precipitation
        handlers.put(RegExprConst.PRECIP_1HR_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("precip1Hour"));
        
        // 6-hour temperature extremes (can have both max and min)
        handlers.put(RegExprConst.TEMP_6HR_MAX_MIN_PATTERN, 
                NoaaAviationWeatherPatternHandler.repeating("temp6HourMaxMin"));
        
        // 24-hour temperature
        handlers.put(RegExprConst.TEMP_24HR_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("temp24Hour"));
        
        // 3-hour pressure tendency
        handlers.put(RegExprConst.PRESS_3HR_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("pressure3Hour"));
        
        // 3/6-hour precipitation
        handlers.put(RegExprConst.PRECIP_3HR_24HR_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("precip3Hr24Hr"));
        
        // Pressure Q-codes
        handlers.put(RegExprConst.PRESS_Q_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("pressureQCode"));
        
        // Automated maintenance indicators
        handlers.put(RegExprConst.AUTOMATED_MAINTENANCE_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("automatedMaintenance"));

        // Hail size
        handlers.put(RegExprConst.HAIL_SIZE_PATTERN,
                NoaaAviationWeatherPatternHandler.single("hailSize"));

        // Weather begin/end times (RAB05, FZRAB1159E1240, etc.)
        // IMPORTANT: This is a repeating pattern because multiple weather events can be chained
        // Example: RAB15E30SNB30 contains two events (rain and snow)
        handlers.put(RegExprConst.BEGIN_END_WEATHER_PATTERN,
                NoaaAviationWeatherPatternHandler.repeating("weatherBeginEnd"));

        // Catch unparsed remarks
        handlers.put(RegExprConst.UNPARSED_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("unparsedRemark"));
        
        return handlers;
    }
    
    /**
     * Get handlers for TAF group patterns (BECMG, TEMPO, FM, PROB).
     * 
     * TAF (Terminal Aerodrome Forecast) groups indicate changes in conditions:
     * - BECMG: Becoming (gradual change)
     * - TEMPO: Temporary fluctuations
     * - FM: From (abrupt change at specific time)
     * - PROB: Probability forecast
     * 
     * @return IndexedLinkedHashMap maintaining insertion order with index access
     */
    public IndexedLinkedHashMap<Pattern, NoaaAviationWeatherPatternHandler> getGroupHandlers() {
        IndexedLinkedHashMap<Pattern, NoaaAviationWeatherPatternHandler> handlers = new IndexedLinkedHashMap<>();
        
        // TAF indicator (at start of TAF)
        handlers.put(RegExprConst.TAF_STR_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("tafIndicator"));
        
        // Valid time period
        handlers.put(RegExprConst.VALTMPER_PATTERN, 
                NoaaAviationWeatherPatternHandler.single("validTimePeriod"));
        
        // BECMG, TEMPO, PROB groups
        handlers.put(RegExprConst.GROUP_BECMG_TEMPO_PROB_PATTERN, 
                NoaaAviationWeatherPatternHandler.repeating("groupBecmgTempoProb"));

        // FM (From) group
        handlers.put(RegExprConst.GROUP_FM_PATTERN, 
                NoaaAviationWeatherPatternHandler.repeating("groupFm"));
        
        return handlers;
    }
}
