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
import weather.model.components.Pressure;

/**
 * Atmospheric pressure at aerodrome elevation (QFE), reported in the remarks
 * section by stations in the Russian region.
 * <p>
 * QFE is not reduced to sea level: an altimeter set to QFE reads zero on the
 * runway. It is therefore lower than QNH by roughly 1 hPa per 8 m of field
 * elevation, and can fall well below the sea-level range accepted by
 * {@link Pressure}. This record uses its own, wider range for that reason.
 * <p>
 * Formats:
 * <ul>
 *   <li>{@code QFE752} - 752 mmHg</li>
 *   <li>{@code QFE752/1003} - 752 mmHg, also reported as 1003 hPa</li>
 * </ul>
 *
 * @param millimetersHg pressure in millimeters of mercury (always reported)
 * @param hectopascals  pressure in hectopascals as reported, or null when the group carries only mmHg
 * @author bclasky1539
 */
public record AerodromePressure(
        Double millimetersHg,
        Double hectopascals
) {

    /**
     * Lowest accepted QFE in mmHg (about 533 hPa), allowing for aerodromes above 4,000 m
     */
    public static final double MIN_MMHG = 400.0;

    /**
     * Highest accepted QFE in mmHg (about 1093 hPa)
     */
    public static final double MAX_MMHG = 820.0;

    /**
     * Largest accepted difference between the reported hPa value and the converted mmHg value.
     * Rounding both values to whole units accounts for at most about 1.2 hPa.
     */
    public static final double MAX_UNIT_DISAGREEMENT_HPA = 2.0;

    private static final String FIELD = "AerodromePressure";

    /**
     * Compact constructor with validation.
     */
    public AerodromePressure {
        if (millimetersHg == null) {
            throw new IllegalArgumentException("QFE millimetersHg cannot be null");
        }
        if (millimetersHg < MIN_MMHG || millimetersHg > MAX_MMHG) {
            throw new InvalidObservationException(
                    FIELD + ".millimetersHg",
                    String.valueOf(millimetersHg),
                    String.format("QFE out of reasonable range (%.0f-%.0f mmHg)", MIN_MMHG, MAX_MMHG));
        }
        if (hectopascals != null) {
            double converted = millimetersHg * Pressure.MMHG_TO_HPA;
            if (Math.abs(converted - hectopascals) > MAX_UNIT_DISAGREEMENT_HPA) {
                throw new InvalidObservationException(
                        FIELD + ".hectopascals",
                        String.valueOf(hectopascals),
                        String.format("QFE hPa value disagrees with %.0f mmHg (expected about %.0f hPa)",
                                millimetersHg, converted));
            }
        }
    }

    // ==================== Factory Methods ====================

    /**
     * Create a QFE reported in mmHg only.
     *
     * @param millimetersHg pressure in mmHg
     * @return AerodromePressure instance
     */
    public static AerodromePressure of(double millimetersHg) {
        return new AerodromePressure(millimetersHg, null);
    }

    /**
     * Create a QFE reported in both mmHg and hPa.
     *
     * @param millimetersHg pressure in mmHg
     * @param hectopascals  pressure in hPa as reported
     * @return AerodromePressure instance
     */
    public static AerodromePressure of(double millimetersHg, double hectopascals) {
        return new AerodromePressure(millimetersHg, hectopascals);
    }

    /**
     * Create a QFE from the digit groups of a METAR remark.
     *
     * @param millimetersHgDigits the mmHg digits (for example "752")
     * @param hectopascalsDigits  the hPa digits (for example "1003"), or null when absent
     * @return AerodromePressure instance
     */
    public static AerodromePressure fromMetarGroup(String millimetersHgDigits, String hectopascalsDigits) {
        double mmHg = Integer.parseInt(millimetersHgDigits);
        Double hPa = hectopascalsDigits == null ? null : (double) Integer.parseInt(hectopascalsDigits);
        return new AerodromePressure(mmHg, hPa);
    }

    // ==================== Query Methods ====================

    /**
     * @return true if the group also carried the hPa value
     */
    public boolean hasReportedHectopascals() {
        return hectopascals != null;
    }

    /**
     * Pressure in hPa: the reported value when present, otherwise converted from mmHg.
     *
     * @return pressure in hPa
     */
    public double toHectopascals() {
        return hasReportedHectopascals() ? hectopascals : millimetersHg * Pressure.MMHG_TO_HPA;
    }

    // ==================== Formatting Methods ====================

    /**
     * @return the METAR remark form, for example "QFE752" or "QFE752/1003"
     */
    public String toMetarCode() {
        String code = String.format("QFE%03.0f", millimetersHg);
        return hasReportedHectopascals() ? code + String.format("/%.0f", hectopascals) : code;
    }

    /**
     * @return a readable summary, for example "QFE 752 mmHg (1003 hPa)"
     */
    public String getSummary() {
        return String.format("QFE %.0f mmHg (%.0f hPa)", millimetersHg, toHectopascals());
    }
}
