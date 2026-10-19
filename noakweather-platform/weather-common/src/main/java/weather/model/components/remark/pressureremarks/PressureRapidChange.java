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
package weather.model.components.remark.pressureremarks;

import weather.exception.InvalidObservationException;

/**
 * Pressure rising or falling rapidly (PRESRR or PRESFR remark).
 * Binary indicator with no associated magnitude — distinct from
 * {@link PressureTendency}, which carries a coded rate and change amount.
 */
public enum PressureRapidChange {
    RISING,
    FALLING;

    /**
     * Parses the single-letter code from the PRES remark.
     *
     * @param code "R" (rising) or "F" (falling)
     * @return the corresponding PressureRapidChange
     * @throws IllegalArgumentException    if code is null
     * @throws InvalidObservationException if code is not "R" or "F"
     */
    public static PressureRapidChange fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Pressure rapid change code cannot be null");
        }

        return switch (code) {
            case "R" -> RISING;
            case "F" -> FALLING;
            default -> throw new InvalidObservationException(
                    "PressureRapidChange.code", code, "Invalid pressure rapid change code (valid codes: R, F)");
        };
    }

    /**
     * Alias for {@link #fromCode(String)}, matching the {@code of(...)}
     * factory-method convention used elsewhere (e.g. {@code PressureTendency.of},
     * {@code Pressure.ofInchesHg}).
     *
     * @param code "R" (rising) or "F" (falling)
     * @return the corresponding PressureRapidChange
     */
    public static PressureRapidChange of(String code) {
        return fromCode(code);
    }

    /**
     * @return a human-readable summary, e.g. "Rising rapidly"
     */
    public String getSummary() {
        return switch (this) {
            case RISING -> "Rising rapidly";
            case FALLING -> "Falling rapidly";
        };
    }
}
