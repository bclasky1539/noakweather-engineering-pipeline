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

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Standard cloud type abbreviations used in METAR/TAF sky conditions and remarks.
 * <p>
 * The single source of truth for the cloud type vocabulary (#140). Main-body sky
 * layers (e.g. "BKN030ACC") and remark cloud types (e.g. "SC1", "ACSL OHD") both
 * validate against this enum, so the two can no longer drift apart.
 * <p>
 * Codes are matched exactly; callers normalize (trim, upper-case) before lookup.
 *
 * @author bclasky1539
 */
public enum CloudTypeAbbreviation {

    CU("Cumulus"),
    TCU("Towering Cumulus"),
    CB("Cumulonimbus"),
    CF("Cumuliform"),
    ST("Stratus"),
    SC("Stratocumulus"),
    SF("Stratiform"),
    NS("Nimbostratus"),
    AS("Altostratus"),
    AC("Altocumulus"),
    CS("Cirrostratus"),
    CC("Cirrocumulus"),
    CI("Cirrus"),
    ACC("Altocumulus Castellanus"),
    ACSL("Altocumulus Standing Lenticular Cloud");

    private static final Map<String, CloudTypeAbbreviation> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(Enum::name, Function.identity()));

    private static final String VALID_CODES_DISPLAY = Arrays.stream(values())
            .map(Enum::name)
            .collect(Collectors.joining(", "));

    private final String description;

    CloudTypeAbbreviation(String description) {
        this.description = description;
    }

    /**
     * @return human-readable cloud type name, e.g. "Altocumulus Castellanus"
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check whether a code is a known cloud type abbreviation.
     *
     * @param code the abbreviation, already trimmed and upper-cased (e.g. "ACC")
     * @return true if the code is known; false for null, blank or unknown codes
     */
    public static boolean isValid(String code) {
        return code != null && BY_CODE.containsKey(code);
    }

    /**
     * Look up a cloud type by its abbreviation.
     *
     * @param code the abbreviation, already trimmed and upper-cased
     * @return the cloud type, or empty for null or unknown codes
     */
    public static Optional<CloudTypeAbbreviation> find(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(BY_CODE.get(code));
    }

    /**
     * @return all valid codes in declaration order, for rejection messages,
     * e.g. "CU, TCU, CB, ... ACC, ACSL"
     */
    public static String validCodesDisplay() {
        return VALID_CODES_DISPLAY;
    }
}
