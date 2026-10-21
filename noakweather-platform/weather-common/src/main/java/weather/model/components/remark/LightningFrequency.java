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
package weather.model.components.remark;

import weather.exception.InvalidObservationException;

/**
 * Lightning frequency (OCNL, FRQ, CONS/CONTUS remark qualifier).
 * CONS (US/FAA convention) and CONTUS (Canadian MANOBS convention) both
 * mean "continuous" and are normalized to the same value.
 */
public enum LightningFrequency {
    OCCASIONAL,
    FREQUENT,
    CONTINUOUS;

    /**
     * Parses the frequency code from the LTG remark.
     *
     * @param code "OCNL", "FRQ", "CONS", or "CONTUS"
     * @return the corresponding LightningFrequency
     * @throws IllegalArgumentException    if code is null
     * @throws InvalidObservationException if code is not recognized
     */
    public static LightningFrequency fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Lightning frequency code cannot be null");
        }

        return switch (code) {
            case "OCNL" -> OCCASIONAL;
            case "FRQ" -> FREQUENT;
            case "CONS", "CONTUS" -> CONTINUOUS;
            default -> throw new InvalidObservationException(
                    "LightningFrequency.code", code,
                    "Invalid lightning frequency code (valid codes: OCNL, FRQ, CONS, CONTUS)");
        };
    }

    /**
     * @return a human-readable description, e.g. "Occasional"
     */
    public String getDescription() {
        return switch (this) {
            case OCCASIONAL -> "Occasional";
            case FREQUENT -> "Frequent";
            case CONTINUOUS -> "Continuous";
        };
    }
}
