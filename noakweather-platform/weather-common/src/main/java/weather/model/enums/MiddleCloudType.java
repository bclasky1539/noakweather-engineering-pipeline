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
 * Predominant middle cloud type (C_M), per WMO Cloud Atlas coding
 * instructions section 2.8.3.2, as used in the METAR/synoptic
 * "8/C_L C_M C_H" remark group.
 * <p>
 * WMO documents three distinct observational conditions that all transmit
 * as digit 7 (Ac + As/Ns coexisting; Ac translucidus/opacus at 2+ levels;
 * Ac single-level predominantly opacus). Decoding is one-way: the digit
 * maps to one representative label, not back to which specific condition
 * an observer selected.
 * <p>
 * Declaration order matches the WMO digit order (NONE=0 through
 * ALTOCUMULUS_CHAOTIC_SKY=9); {@link #fromCode(String)} relies on this via
 * ordinal lookup, so constants must not be reordered.
 *
 * @author bclasky1539
 */
public enum MiddleCloudType {
    NONE("No middle cloud"),
    ALTOSTRATUS_TRANSLUCIDUS("Altostratus, mostly semi-transparent"),
    ALTOSTRATUS_DENSE_OR_NIMBOSTRATUS("Altostratus (dense) or Nimbostratus"),
    ALTOCUMULUS_TRANSLUCIDUS_SINGLE_LEVEL("Altocumulus translucidus, single level"),
    ALTOCUMULUS_PATCHES_CHANGING("Altocumulus patches, continuously changing"),
    ALTOCUMULUS_INVADING("Altocumulus progressively invading the sky"),
    ALTOCUMULUS_CUMULOGENITUS("Altocumulus cumulogenitus or cumulonimbogenitus"),
    ALTOCUMULUS_MULTI_LEVEL_OR_WITH_ALTOSTRATUS_OR_OPACUS(
            "Altocumulus at multiple levels, with Altostratus/Nimbostratus, or opacus"),
    ALTOCUMULUS_CASTELLANUS_OR_FLOCCUS("Altocumulus castellanus or floccus"),
    ALTOCUMULUS_CHAOTIC_SKY("Altocumulus, chaotic sky"),
    OBSCURED("Obscured by overcast layer below");

    private final String summary;

    MiddleCloudType(String summary) {
        this.summary = summary;
    }

    /**
     * Parses the transmitted digit (or "/" for obscured) into a MiddleCloudType.
     *
     * @param code a single digit "0"-"9", or "/" for obscured-above-overcast
     * @return the corresponding MiddleCloudType
     * @throws IllegalArgumentException if code is not a recognized value
     */
    public static MiddleCloudType fromCode(String code) {
        return CloudTypeCodeParser.fromCode(code, MiddleCloudType.class, OBSCURED, "middle cloud type");
    }

    /**
     * @return a human-readable description of this cloud type
     */
    public String getSummary() {
        return summary;
    }
}
