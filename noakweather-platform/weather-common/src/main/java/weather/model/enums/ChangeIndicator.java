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
package weather.model.enums;

/**
 * Types of forecast change indicators used in TAF (Terminal Aerodrome Forecast) reports.
 *
 * TAF forecasts contain a base forecast plus optional change groups that describe
 * variations from the base conditions. Each change group is prefixed with an indicator
 * that defines the nature and timing of the change.
 *
 * Examples from TAF reports:
 * - "FM152100 21005KT P6SM SCT250" - Permanent change FROM 15th at 2100Z
 * - "TEMPO 3003/3011 P6SM -SHSN BKN040" - Temporary fluctuations between times
 * - "BECMG 3108/3110 FEW038CB" - Gradual change over the period
 * - "PROB30 3005/3011 3SM -SHSN OVC020" - 30% probability of conditions
 *
 * @author bclasky1539
 *
 */
public enum ChangeIndicator {

    /**
     * BASE - The initial forecast conditions.
     * This is the default forecast that applies from the start of the validity period
     * until the first change group, or for the entire period if no changes.
     *
     * Example: "TAF KJFK 151953Z 1520/1624 VRB02KT P6SM FEW250"
     *          The "VRB02KT P6SM FEW250" is the BASE forecast.
     */
    BASE("BASE", "Base Forecast", "Initial conditions for the forecast period"),

    /**
     * FM - From (permanent change at exact time).
     * Indicates a permanent change to forecast conditions starting at a specific time.
     * All previous conditions are replaced by the new conditions.
     *
     * Format: FMDDHHmm (where DD=day, HH=hour, mm=minute in UTC)
     * Example: "FM152100 21005KT P6SM SCT250"
     *          From 15th day at 2100Z, winds become 210° at 5kt, visibility >6SM, scattered at 25,000ft
     */
    FM("FM", "From", "Permanent change starting at exact time"),

    /**
     * TEMPO - Temporary fluctuations.
     * Indicates temporary fluctuations expected to occur for periods of less than one hour
     * at a time, and in total for less than half of the forecast period.
     *
     * Format: TEMPO DDHH/DDHH (start/end times)
     * Example: "TEMPO 3003/3011 P6SM -SHSN BKN040 BKN160"
     *          Temporary snow showers expected between 30th 0300Z and 30th 1100Z
     */
    TEMPO("TEMPO", "Temporary", "Temporary fluctuations (< 1hr at a time, < half of period)"),

    /**
     * BECMG - Becoming (gradual change).
     * Indicates a permanent change expected to occur at an unspecified time within
     * the specified period. The change is gradual, taking place over the entire period.
     *
     * Format: BECMG DDHH/DDHH (start/end times)
     * Example: "BECMG 3102/3104 27015KT"
     *          Winds gradually becoming 270° at 15kt sometime between 31st 0200Z and 0400Z
     */
    BECMG("BECMG", "Becoming", "Gradual change over the specified period"),

    /**
     * PROB - Probability.
     * Indicates conditions that have a specific probability of occurrence.
     * Typically PROB30 (30% chance) or PROB40 (40% chance).
     *
     * Format: PROB[30|40] DDHH/DDHH or PROB[30|40] TEMPO DDHH/DDHH
     * Example: "PROB30 3005/3011 3SM -SHSN OVC020"
     *          30% probability of conditions between 30th 0500Z and 1100Z
     */
    PROB("PROB", "Probability", "Probabilistic forecast (PROB30 or PROB40)");

    /**
     * The code as it appears in TAF reports.
     */
    private final String code;

    /**
     * Short human-readable name.
     */
    private final String displayName;

    /**
     * Detailed description of what this indicator means.
     */
    private final String description;

    /**
     * Constructor for ChangeIndicator enum.
     *
     * @param code The code as it appears in reports
     * @param displayName Short human-readable name
     * @param description Detailed description
     */
    ChangeIndicator(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get the code as it appears in TAF reports.
     *
     * @return the code string
     */
    public String getCode() {
        return code;
    }

    /**
     * Get the display name for UI/logging.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the detailed description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if this is a permanent change (BASE or FM).
     * Permanent changes replace all previous conditions.
     *
     * @return true if this is a permanent change
     */
    public boolean isPermanent() {
        return this == BASE || this == FM;
    }

    /**
     * Check if this is a temporary change (TEMPO).
     * Temporary changes are expected to occur briefly and intermittently.
     *
     * @return true if this is a temporary change
     */
    public boolean isTemporary() {
        return this == TEMPO;
    }

    /**
     * Check if this is a gradual change (BECMG).
     * Gradual changes occur over a period rather than at a specific time.
     *
     * @return true if this is a gradual change
     */
    public boolean isGradual() {
        return this == BECMG;
    }

    /**
     * Check if this is a probabilistic forecast (PROB).
     * Probabilistic forecasts have an associated probability percentage.
     *
     * @return true if this is probabilistic
     */
    public boolean isProbabilistic() {
        return this == PROB;
    }

    /**
     * Check if this change indicator requires a time period (DDHH/DDHH).
     * FM uses exact time (FMDDHHmm), but TEMPO, BECMG, and PROB use periods.
     *
     * @return true if requires a time period
     */
    public boolean requiresTimePeriod() {
        return this == TEMPO || this == BECMG || this == PROB;
    }

    /**
     * Check if this change indicator uses exact time (FMDDHHmm).
     * Only FM uses exact time instead of a period.
     *
     * @return true if uses exact time
     */
    public boolean usesExactTime() {
        return this == FM;
    }

    /**
     * Parse a change indicator from a TAF code string.
     *
     * @param code the code string (e.g., "FM", "TEMPO", "BECMG", "PROB30", "PROB40")
     * @return the matching ChangeIndicator, or null if not recognized
     */
    public static ChangeIndicator fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        String normalized = code.trim().toUpperCase();

        // Handle PROB30 and PROB40 variants
        if (normalized.startsWith("PROB")) {
            return PROB;
        }

        // Match exact codes
        for (ChangeIndicator indicator : values()) {
            if (indicator.code.equals(normalized)) {
                return indicator;
            }
        }

        return null;
    }

    /**
     * Get a human-readable summary of this change indicator.
     *
     * @return formatted summary
     */
    public String getSummary() {
        return String.format("%s (%s): %s", code, displayName, description);
    }

    @Override
    public String toString() {
        return code;
    }
}
