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
 * Friction coefficient or braking action (B_R B_R), the last two characters of
 * a runway state group ({@code Rdd/ERCReeBB}).
 * <p>
 * The mean value is transmitted or, if operationally significant, the lowest value.
 * <ul>
 *   <li>01-90 = measured friction coefficient 0.01-0.90 ({@link #MEASURED})</li>
 *   <li>91-95 = reported braking action, poor to good</li>
 *   <li>99 = figures unreliable (e.g. equipment that does not measure satisfactorily in slush or snow)</li>
 *   <li>// = braking action not reported (e.g. runway not operational, aerodrome closed)</li>
 * </ul>
 * Codes 01-90 share one constant; the actual coefficient is carried separately
 * via {@link #toCoefficient(String)}.
 *
 * @author bclasky1539
 */
public enum FrictionCoefficient {
    MEASURED("01-90", "Measured friction coefficient"),
    BRAKING_POOR("91", "Braking action poor"),
    BRAKING_MEDIUM_POOR("92", "Braking action medium to poor"),
    BRAKING_MEDIUM("93", "Braking action medium"),
    BRAKING_MEDIUM_GOOD("94", "Braking action medium to good"),
    BRAKING_GOOD("95", "Braking action good"),
    UNRELIABLE("99", "Figures unreliable or not measurable"),
    NOT_REPORTED("//", "Braking action not reported");

    private static final int MAX_MEASURED_CODE = 90;

    /*
     * Equivalence between a measured coefficient (x 100) and braking action,
     * per the standard runway state decoding table: upper bound of each band.
     */
    private static final int POOR_MAX = 25;
    private static final int MEDIUM_POOR_MAX = 29;
    private static final int MEDIUM_MAX = 35;
    private static final int MEDIUM_GOOD_MAX = 39;

    private final String code;
    private final String summary;

    FrictionCoefficient(String code, String summary) {
        this.code = code;
        this.summary = summary;
    }

    /**
     * Parses the transmitted two-character friction code into a FrictionCoefficient.
     *
     * @param code "01"-"95", "99", or "//"
     * @return the corresponding FrictionCoefficient
     * @throws IllegalArgumentException    if code is null (programming error)
     * @throws InvalidObservationException if code is not a recognized value
     */
    public static FrictionCoefficient fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Runway friction code cannot be null");
        }
        if (NOT_REPORTED.code.equals(code)) {
            return NOT_REPORTED;
        }
        if (!code.matches("\\d{2}")) {
            throw new InvalidObservationException("FrictionCoefficient.code", code, "Invalid runway friction code");
        }
        int value = Integer.parseInt(code);
        if (value >= 1 && value <= MAX_MEASURED_CODE) {
            return MEASURED;
        }
        for (FrictionCoefficient friction : values()) {
            if (friction.code.equals(code)) {
                return friction;
            }
        }
        throw new InvalidObservationException("FrictionCoefficient.code", code, "Invalid runway friction code");
    }

    /**
     * Converts a transmitted friction code of this constant into a coefficient.
     *
     * @param code the transmitted code this constant was decoded from
     * @return coefficient 0.01-0.90 for {@link #MEASURED}, otherwise null
     */
    public Double toCoefficient(String code) {
        return this == MEASURED ? Integer.parseInt(code) / 100.0 : null;
    }

    /**
     * Maps a measured friction coefficient to its equivalent braking action.
     *
     * @param coefficient measured coefficient, 0.01-0.90
     * @return the equivalent braking action constant (BRAKING_POOR to BRAKING_GOOD)
     */
    public static FrictionCoefficient equivalentBrakingAction(double coefficient) {
        int hundredths = (int) Math.round(coefficient * 100);
        if (hundredths <= POOR_MAX) {
            return BRAKING_POOR;
        }
        if (hundredths <= MEDIUM_POOR_MAX) {
            return BRAKING_MEDIUM_POOR;
        }
        if (hundredths <= MEDIUM_MAX) {
            return BRAKING_MEDIUM;
        }
        if (hundredths <= MEDIUM_GOOD_MAX) {
            return BRAKING_MEDIUM_GOOD;
        }
        return BRAKING_GOOD;
    }

    /**
     * @return true if this constant is a reported braking action (codes 91-95)
     */
    public boolean isBrakingAction() {
        return this == BRAKING_POOR || this == BRAKING_MEDIUM_POOR || this == BRAKING_MEDIUM
                || this == BRAKING_MEDIUM_GOOD || this == BRAKING_GOOD;
    }

    /**
     * @return the transmitted code ("01-90" for {@link #MEASURED})
     */
    public String getCode() {
        return code;
    }

    /**
     * @return a human-readable description
     */
    public String getSummary() {
        return summary;
    }
}
