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
package weather.model.components;

import com.fasterxml.jackson.annotation.JsonIgnore;
import weather.exception.InvalidObservationException;
import weather.model.enums.DepthOfDeposit;
import weather.model.enums.FrictionCoefficient;
import weather.model.enums.RunwayContaminationExtent;
import weather.model.enums.RunwayDeposit;
import weather.utils.ValidationPatterns;

/**
 * Immutable value object representing a runway state group (RSG) from a METAR or SPECI.
 * <p>
 * Where RVR describes how far a pilot can see down the runway, runway state
 * describes the surface itself: what is on it, how much of it is covered, how
 * deep, and how well the brakes grip. Both groups start with {@code Rdd/}, but
 * they are different reports and must never be confused.
 * <p>
 * The RSG was omitted from METAR with the introduction of the new SNOWTAM format
 * in 2021, but some states (e.g. Russia) still include it, and historical data
 * contains it.
 * <p>
 * Format: {@code Rdd/ERCReeBB}
 * <ul>
 *   <li>E_R type of deposit, see {@link RunwayDeposit}</li>
 *   <li>C_R extent of contamination, see {@link RunwayContaminationExtent}</li>
 *   <li>e_R e_R depth of deposit, see {@link DepthOfDeposit}</li>
 *   <li>B_R B_R friction coefficient or braking action, see {@link FrictionCoefficient}</li>
 * </ul>
 * Special forms:
 * <ul>
 *   <li>{@code Rdd/CLRDBB}: contamination has ceased to exist (cleared), followed by friction</li>
 *   <li>{@code Rdd//////}: report not updated</li>
 * </ul>
 * Format Examples:
 * <pre>
 * R23R/1///60  → Runway 23R, damp, extent not reported, depth not significant, friction 0.60
 * R23L/2/0150  → Runway 23L, wet, extent not reported, depth 1 mm, friction 0.50
 * R05R/090060  → Runway 05R, clear and dry, 51-100%, depth less than 1 mm, friction 0.60
 * R27/451291   → Runway 27, dry snow, 26-50%, depth 12 mm, braking action poor
 * R09/899299   → Runway 09, compacted snow, 51-100%, depth 10 cm, figures unreliable
 * R24/CLRD93   → Runway 24, cleared, braking action medium
 * R24//////    → Runway 24, report not updated
 * </pre>
 * In a normal report, deposit, extent, depth and friction are always set: "not
 * reported" and "not significant" are explicit constants, not nulls. They are
 * null only for a cleared report (deposit, extent, depth) or a not-updated
 * report (all four).
 *
 * @param runway           Runway identifier (e.g., "23R", "05", "34L")
 * @param deposit          Type of deposit; null if cleared or not updated
 * @param extent           Extent of contamination; null if cleared or not updated
 * @param depth            Depth of deposit code; null if cleared or not updated
 * @param depthMm          Depth in millimetres when the depth code carries one, otherwise null
 * @param friction         Friction coefficient or braking action code; null only if not updated
 * @param frictionValue    Measured coefficient 0.01-0.90 when friction is MEASURED, otherwise null
 * @param cleared          True for a CLRD report (contamination ceased to exist)
 * @param reportNotUpdated True for a "//////" report (not updated)
 * @author bclasky1539
 */
public record RunwayState(
        String runway,
        RunwayDeposit deposit,
        RunwayContaminationExtent extent,
        DepthOfDeposit depth,
        Integer depthMm,
        FrictionCoefficient friction,
        Double frictionValue,
        boolean cleared,
        boolean reportNotUpdated
) {

    private static final String NOT_UPDATED_SINGLE = "/";
    private static final String NOT_UPDATED_DOUBLE = "//";
    private static final int MAX_MILLIMETRE_DEPTH = 90;
    private static final double MAX_FRICTION_VALUE = 0.90;

    /**
     * Compact constructor with validation.
     */
    public RunwayState {
        validateRunway(runway);
        if (reportNotUpdated) {
            validateNotUpdated(deposit, extent, depth, depthMm, friction, frictionValue, cleared);
        } else if (cleared) {
            validateCleared(deposit, extent, depth, depthMm);
            validateFriction(friction, frictionValue);
        } else {
            validateReported(deposit, extent, depth, friction);
            validateDepth(depth, depthMm);
            validateFriction(friction, frictionValue);
        }
    }

    // ========== FACTORY METHODS ==========

    /**
     * Build a RunwayState from the transmitted code fields of an {@code Rdd/ERCReeBB} group.
     * All six characters "/" ({@code Rdd//////}) produce a not-updated report.
     *
     * @param runway       runway identifier
     * @param depositCode  one character: "0"-"9" or "/"
     * @param extentCode   one character: "1", "2", "5", "9" or "/"
     * @param depthCode    two characters: "00"-"90", "92"-"99" or "//"
     * @param frictionCode two characters: "01"-"95", "99" or "//"
     * @return decoded RunwayState
     * @throws IllegalArgumentException if any code is not a recognized value
     */
    public static RunwayState fromCodes(String runway, String depositCode, String extentCode,
                                        String depthCode, String frictionCode) {
        if (isNotUpdated(depositCode, extentCode, depthCode, frictionCode)) {
            return notUpdated(runway);
        }
        DepthOfDeposit depth = DepthOfDeposit.fromCode(depthCode);
        FrictionCoefficient friction = FrictionCoefficient.fromCode(frictionCode);
        return new RunwayState(runway,
                RunwayDeposit.fromCode(depositCode),
                RunwayContaminationExtent.fromCode(extentCode),
                depth, depth.toMillimetres(depthCode),
                friction, friction.toCoefficient(frictionCode),
                false, false);
    }

    /**
     * Build a cleared RunwayState from a {@code Rdd/CLRDBB} group.
     *
     * @param runway       runway identifier
     * @param frictionCode two characters: "01"-"95", "99" or "//"
     * @return cleared RunwayState with the decoded friction
     * @throws IllegalArgumentException if the friction code is not a recognized value
     */
    public static RunwayState clearedFromCode(String runway, String frictionCode) {
        FrictionCoefficient friction = FrictionCoefficient.fromCode(frictionCode);
        return new RunwayState(runway, null, null, null, null,
                friction, friction.toCoefficient(frictionCode), true, false);
    }

    /**
     * Build a not-updated RunwayState from a {@code Rdd//////} group.
     *
     * @param runway runway identifier
     * @return RunwayState flagged as report not updated
     */
    public static RunwayState notUpdated(String runway) {
        return new RunwayState(runway, null, null, null, null, null, null, false, true);
    }

    private static boolean isNotUpdated(String depositCode, String extentCode,
                                        String depthCode, String frictionCode) {
        return NOT_UPDATED_SINGLE.equals(depositCode) && NOT_UPDATED_SINGLE.equals(extentCode)
                && NOT_UPDATED_DOUBLE.equals(depthCode) && NOT_UPDATED_DOUBLE.equals(frictionCode);
    }

    // ========== VALIDATION ==========

    private static void validateRunway(String runway) {
        if (runway == null || runway.isBlank()) {
            throw new IllegalArgumentException("Runway identifier cannot be null or blank");
        }
        String trimmed = runway.trim().toUpperCase();
        if (!ValidationPatterns.RUNWAY_IDENTIFIER.matcher(trimmed).matches()) {
            throw new InvalidObservationException(
                    "RunwayState.runway",
                    runway,
                    "Invalid runway identifier format (expected format: 01-36 optionally followed by L, C, or R)"
            );
        }
    }

    private static void validateNotUpdated(RunwayDeposit deposit, RunwayContaminationExtent extent,
                                           DepthOfDeposit depth, Integer depthMm,
                                           FrictionCoefficient friction, Double frictionValue,
                                           boolean cleared) {
        if (cleared) {
            throw new IllegalArgumentException("Runway state cannot be both cleared and not updated");
        }
        if (deposit != null || extent != null || depth != null || depthMm != null
                || friction != null || frictionValue != null) {
            throw new IllegalArgumentException("Not-updated runway state cannot have any values");
        }
    }

    private static void validateCleared(RunwayDeposit deposit, RunwayContaminationExtent extent,
                                        DepthOfDeposit depth, Integer depthMm) {
        if (deposit != null || extent != null || depth != null || depthMm != null) {
            throw new IllegalArgumentException(
                    "Cleared runway state cannot have deposit, extent or depth values");
        }
    }

    private static void validateReported(RunwayDeposit deposit, RunwayContaminationExtent extent,
                                         DepthOfDeposit depth, FrictionCoefficient friction) {
        if (deposit == null || extent == null || depth == null || friction == null) {
            throw new IllegalArgumentException(
                    "Runway state requires deposit, extent, depth and friction (use NOT_REPORTED / NOT_SIGNIFICANT)");
        }
    }

    private static void validateDepth(DepthOfDeposit depth, Integer depthMm) {
        if (!depth.hasDepthValue()) {
            if (depthMm != null) {
                throw new IllegalArgumentException("Depth " + depth + " cannot have a millimetre value");
            }
            return;
        }
        if (depthMm == null) {
            throw new IllegalArgumentException("Depth " + depth + " requires a millimetre value");
        }
        if (depth == DepthOfDeposit.MILLIMETRES) {
            if (depthMm < 1 || depthMm > MAX_MILLIMETRE_DEPTH) {
                throw new IllegalArgumentException("Depth in millimetres must be 1-90: " + depthMm);
            }
        } else if (!depth.getFixedMillimetres().equals(depthMm)) {
            throw new IllegalArgumentException(
                    "Depth " + depth + " must be " + depth.getFixedMillimetres() + " mm: " + depthMm);
        }
    }

    private static void validateFriction(FrictionCoefficient friction, Double frictionValue) {
        if (friction == null) {
            throw new IllegalArgumentException("Friction is required (use NOT_REPORTED)");
        }
        if (friction != FrictionCoefficient.MEASURED) {
            if (frictionValue != null) {
                throw new IllegalArgumentException("Friction " + friction + " cannot have a coefficient value");
            }
            return;
        }
        if (frictionValue == null || frictionValue <= 0.0 || frictionValue > MAX_FRICTION_VALUE) {
            throw new IllegalArgumentException(
                    "Measured friction coefficient must be 0.01-0.90: " + frictionValue);
        }
    }

    // ========== CONVENIENCE METHODS ==========

    /**
     * Get the braking action for this report: the reported braking action, or the
     * equivalent of a measured coefficient (e.g. 0.60 is good, 0.28 is medium to poor).
     *
     * @return a BRAKING_* constant, or null if friction is unreliable, not reported, or not updated
     */
    @JsonIgnore
    public FrictionCoefficient getEquivalentBrakingAction() {
        if (friction == null) {
            return null;
        }
        if (friction.isBrakingAction()) {
            return friction;
        }
        if (friction == FrictionCoefficient.MEASURED) {
            return FrictionCoefficient.equivalentBrakingAction(frictionValue);
        }
        return null;
    }

    /**
     * Get a human-readable summary of the runway state.
     *
     * @return formatted string describing the runway state
     */
    @JsonIgnore
    public String getSummary() {
        String prefix = "Runway " + runway + ": ";
        if (reportNotUpdated) {
            return prefix + "Report not updated";
        }
        if (cleared) {
            return prefix + "Contamination cleared; " + frictionDescription();
        }
        return prefix + String.join("; ",
                deposit.getSummary(), extent.getSummary(), depthDescription(), frictionDescription());
    }

    private String depthDescription() {
        return depth == DepthOfDeposit.MILLIMETRES ? "Depth " + depthMm + " mm" : depth.getSummary();
    }

    private String frictionDescription() {
        return friction == FrictionCoefficient.MEASURED
                ? String.format("Friction coefficient %.2f", frictionValue)
                : friction.getSummary();
    }
}
