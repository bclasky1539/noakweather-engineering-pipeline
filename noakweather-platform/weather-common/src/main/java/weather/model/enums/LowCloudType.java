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
 * Predominant low cloud type (C_L), per WMO Cloud Atlas coding instructions
 * section 2.8.3.1, as used in the METAR/synoptic "8/C_L C_M C_H" remark group.
 * <p>
 * Digits are assigned by the observer's first-applicable-condition priority
 * order documented by WMO; that priority does not correspond to numeric
 * order. This enum only decodes the transmitted digit back to a label — the
 * original observational judgment is not reconstructed.
 * <p>
 * Declaration order matches the WMO digit order (NONE=0 through
 * CUMULONIMBUS_CAPILLATUS=9); {@link #fromCode(String)} relies on this via
 * ordinal lookup, so constants must not be reordered.
 *
 * @author bclasky1539
 */
public enum LowCloudType {
    NONE("No low cloud"),
    CUMULUS_HUMILIS_OR_FRACTUS_FAIR_WEATHER("Cumulus humilis or fractus (fair weather)"),
    CUMULUS_MEDIOCRIS_OR_CONGESTUS("Cumulus mediocris or congestus"),
    CUMULONIMBUS_CALVUS("Cumulonimbus, not yet fibrous or striated"),
    STRATOCUMULUS_CUMULOGENITUS("Stratocumulus formed by spreading of Cumulus"),
    STRATOCUMULUS_NON_CUMULOGENITUS("Stratocumulus, not from spreading Cumulus"),
    STRATUS_NEBULOSUS_OR_FRACTUS_FAIR_WEATHER("Stratus nebulosus or fractus (fair weather)"),
    STRATUS_FRACTUS_OR_CUMULUS_FRACTUS_BAD_WEATHER("Stratus or Cumulus fractus (bad weather)"),
    CUMULUS_AND_STRATOCUMULUS_DIFFERENT_LEVELS("Cumulus and Stratocumulus at different levels"),
    CUMULONIMBUS_CAPILLATUS("Cumulonimbus capillatus"),
    OBSCURED("Obscured by overcast layer below");

    private final String summary;

    LowCloudType(String summary) {
        this.summary = summary;
    }

    /**
     * Parses the transmitted digit (or "/" for obscured) into a LowCloudType.
     *
     * @param code a single digit "0"-"9", or "/" for obscured-above-overcast
     * @return the corresponding LowCloudType
     * @throws IllegalArgumentException if code is not a recognized value
     */
    public static LowCloudType fromCode(String code) {
        return CloudTypeCodeParser.fromCode(code, LowCloudType.class, OBSCURED, "low cloud type");
    }

    /**
     * @return a human-readable description of this cloud type
     */
    public String getSummary() {
        return summary;
    }
}
