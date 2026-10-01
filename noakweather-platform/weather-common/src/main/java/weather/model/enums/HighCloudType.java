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
 * Predominant high cloud type (C_H), per WMO Cloud Atlas coding
 * instructions section 2.8.3.3, as used in the METAR/synoptic
 * "8/C_L C_M C_H" remark group.
 * <p>
 * Declaration order matches the WMO digit order (NONE=0 through
 * CIRROCUMULUS_PREDOMINANT=9); {@link #fromCode(String)} relies on this via
 * ordinal lookup, so constants must not be reordered.
 *
 * @author bclasky1539
 */
public enum HighCloudType {
    NONE("No high cloud"),
    CIRRUS_FIBRATUS_OR_UNCINUS("Cirrus fibratus or uncinus"),
    CIRRUS_SPISSATUS_OR_CASTELLANUS_OR_FLOCCUS(
            "Cirrus spissatus (non-CB) or castellanus/floccus, predominant"),
    CIRRUS_SPISSATUS_FROM_CUMULONIMBUS("Cirrus spissatus originating from Cumulonimbus"),
    CIRRUS_INVADING("Cirrus uncinus or fibratus, progressively invading"),
    CIRROSTRATUS_INVADING_BELOW_45_DEGREES("Cirrostratus invading, below 45° above horizon"),
    CIRROSTRATUS_INVADING_ABOVE_45_DEGREES("Cirrostratus invading, above 45° above horizon"),
    CIRROSTRATUS_COVERING_WHOLE_SKY("Cirrostratus covering the whole sky"),
    CIRROSTRATUS_NOT_INVADING_NOT_COVERING("Cirrostratus, not invading, not covering whole sky"),
    CIRROCUMULUS_PREDOMINANT("Cirrocumulus alone or predominant"),
    OBSCURED("Obscured by overcast layer below");

    private final String summary;

    HighCloudType(String summary) {
        this.summary = summary;
    }

    /**
     * Parses the transmitted digit (or "/" for obscured) into a HighCloudType.
     *
     * @param code a single digit "0"-"9", or "/" for obscured-above-overcast
     * @return the corresponding HighCloudType
     * @throws IllegalArgumentException if code is not a recognized value
     */
    public static HighCloudType fromCode(String code) {
        return CloudTypeCodeParser.fromCode(code, HighCloudType.class, OBSCURED, "high cloud type");
    }

    /**
     * @return a human-readable description of this cloud type
     */
    public String getSummary() {
        return summary;
    }
}
