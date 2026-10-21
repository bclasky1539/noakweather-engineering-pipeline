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

import weather.exception.InvalidObservationException;

/**
 * Type of runway deposit (E_R), the first character of a runway state group
 * ({@code Rdd/ERCReeBB}).
 * <p>
 * Each constant carries its transmitted code explicitly, so declaration order
 * is not significant.
 *
 * @author bclasky1539
 */
public enum RunwayDeposit {
    CLEAR_AND_DRY("0", "Clear and dry"),
    DAMP("1", "Damp"),
    WET_OR_WATER_PATCHES("2", "Wet or water patches"),
    RIME_OR_FROST("3", "Rime or frost covered (depth normally less than 1 mm)"),
    DRY_SNOW("4", "Dry snow"),
    WET_SNOW("5", "Wet snow"),
    SLUSH("6", "Slush"),
    ICE("7", "Ice"),
    COMPACTED_OR_ROLLED_SNOW("8", "Compacted or rolled snow"),
    FROZEN_RUTS_OR_RIDGES("9", "Frozen ruts or ridges"),
    NOT_REPORTED("/", "Type of deposit not reported (e.g. runway clearance in progress)");

    private final String code;
    private final String summary;

    RunwayDeposit(String code, String summary) {
        this.code = code;
        this.summary = summary;
    }

    /**
     * Parses the transmitted deposit code into a RunwayDeposit.
     *
     * @param code a single digit "0"-"9", or "/" for not reported
     * @return the corresponding RunwayDeposit
     * @throws IllegalArgumentException    if code is null (programming error)
     * @throws InvalidObservationException if code is not a recognized runway deposit code
     */
    public static RunwayDeposit fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Runway deposit code cannot be null");
        }
        for (RunwayDeposit deposit : values()) {
            if (deposit.code.equals(code)) {
                return deposit;
            }
        }
        throw new InvalidObservationException("RunwayDeposit.code", code, "Invalid runway deposit code");
    }

    /**
     * @return the transmitted code for this deposit type
     */
    public String getCode() {
        return code;
    }

    /**
     * @return a human-readable description of this deposit type
     */
    public String getSummary() {
        return summary;
    }
}
