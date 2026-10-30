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
 * <p>
 * Uses IndexedLinkedHashMap to maintain pattern order and provide index-based access.
 * Pattern order is critical for sequential parsing - patterns are checked in sequence.
 * <p>
 * Two types of registries:
 * - Main handlers: Core METAR body (before RMK)
 * - Group handlers: TAF groups (BECMG, TEMPO, FM, PROB)
 * <p>
 * The METAR main body and remarks no longer use this registry: they run on the
 * shared group parsing engine ({@link SectionParser}, #153). It remains for the
 * TAF parser until #153 phase 4.
 *
 * @author bclasky1539
 *
 */
public class NoaaAviationWeatherPatternRegistry {

    /**
     * Get handlers for main METAR body (before RMK).
     * Patterns are ordered by typical METAR sequence.
     * <p>
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

        // Runway state group (Rdd/ERCReeBB, Rdd/CLRDBB) - can repeat, one per runway
        // MUST be before RUNWAY_PATTERN: both start with Rdd/, and the first matching handler wins
        handlers.put(RegExprConst.RUNWAY_STATE_PATTERN,
                NoaaAviationWeatherPatternHandler.repeating("runwayState"));

        // Runway Visual Range (can have multiple runways)
        handlers.put(RegExprConst.RUNWAY_PATTERN,
                NoaaAviationWeatherPatternHandler.repeating("runway"));

        // Present weather (can have multiple phenomena: -RA BR)
        handlers.put(RegExprConst.PRESENT_WEATHER_PATTERN,
                NoaaAviationWeatherPatternHandler.repeating("presentWeather"));

        // Recent weather (RE groups) - up to three, after pressure and before NOSIG
        handlers.put(RegExprConst.RECENT_WEATHER_PATTERN,
                NoaaAviationWeatherPatternHandler.repeating("recentWeather"));

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

        return handlers;
    }

    /**
     * Get handlers for TAF group patterns (BECMG, TEMPO, FM, PROB).
     * <p>
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
