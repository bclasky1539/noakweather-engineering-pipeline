/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025 bclasky1539
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

/**
 * Immutable value object representing a cloud type observation in METAR remarks.
 *
 * Cloud types describe cloud formations using standard abbreviations with optional
 * okta coverage (eighths of sky covered), intensity, and location/movement.
 *
 * Format: [Intensity] CloudType [Oktas] [Location/Movement]
 *
 * Examples:
 *   "SC1" → CloudType("SC", 1, null, null, null)
 *   "SC TR" → CloudType("SC", null, null, "TR", null)
 *   "MDT CU OHD" → CloudType("CU", null, "MDT", "OHD", null)
 *   "AC8" → CloudType("AC", 8, null, null, null)
 *   "CI MOVG NE" → CloudType("CI", null, null, null, "NE")
 *
 * Cloud Type Codes:
 *   CU - Cumulus
 *   TCU - Towering Cumulus
 *   CF - Cumuliform
 *   ST - Stratus
 *   SC - Stratocumulus
 *   SF - Stratiform
 *   NS - Nimbostratus
 *   AS - Altostratus
 *   AC - Altocumulus
 *   CS - Cirrostratus
 *   CC - Cirrocumulus
 *   CI - Cirrus
 *
 * Oktas: 1-8 (eighths of sky covered), null if not specified
 *
 * Intensity:
 *   MDT - Moderate
 *
 * Location:
 *   OHD - Overhead
 *   OHD-ALQDS - Overhead all quadrants
 *   ALQDS - All quadrants
 *   TR - Trace
 *
 * Movement Direction:
 *   N, S, E, W, NE, NW, SE, SW
 *
 * @param cloudType Cloud type code (CU, CI, AC, etc.) - required
 * @param oktas Coverage in eighths (1-8), null if not specified
 * @param intensity Intensity modifier (MDT), null if not specified
 * @param location Location indicator (OHD, TR, etc.), null if not specified
 * @param movementDirection Movement direction (N, NE, etc.), null if not specified
 *
 * @author bclasky1539
 *
 */
public record CloudType(
        String cloudType,
        Integer oktas,
        String intensity,
        String location,
        String movementDirection
) {

    private static final String OVERHEAD_ALL_QUADRANTS_CODE = "OHD-ALQDS";

    // Valid cloud type codes
    private static final String[] VALID_CLOUD_TYPES = {
            "CU", "TCU", "CF", "ST", "SC", "SF", "NS", "AS", "AC", "CS", "CC", "CI"
    };

    // Valid intensity modifiers
    private static final String[] VALID_INTENSITIES = {
            "MDT"
    };

    // Valid location indicators
    private static final String[] VALID_LOCATIONS = {
            "OHD", OVERHEAD_ALL_QUADRANTS_CODE, "ALQDS", "TR"
    };

    // Valid movement directions
    private static final String[] VALID_DIRECTIONS = {
            "N", "S", "E", "W", "NE", "NW", "SE", "SW"
    };

    /**
     * Compact constructor with validation.
     */
    public CloudType {
        cloudType = validateAndNormalizeCloudType(cloudType);
        validateOktas(oktas);
        intensity = validateAndNormalizeIntensity(intensity);
        location = validateAndNormalizeLocation(location);
        movementDirection = validateAndNormalizeMovementDirection(movementDirection);
    }

    /**
     * Validate and normalize cloud type code.
     *
     * @param cloudType the cloud type to validate
     * @return normalized cloud type (uppercase, trimmed)
     * @throws IllegalArgumentException if cloud type is invalid
     */
    private static String validateAndNormalizeCloudType(String cloudType) {
        if (cloudType == null || cloudType.isBlank()) {
            throw new IllegalArgumentException("Cloud type cannot be null or blank");
        }

        String normalized = cloudType.trim().toUpperCase();

        for (String valid : VALID_CLOUD_TYPES) {
            if (valid.equals(normalized)) {
                return normalized;
            }
        }

        throw new IllegalArgumentException("Invalid cloud type: " + cloudType);
    }

    /**
     * Validate oktas value.
     *
     * @param oktas the oktas value to validate
     * @throws IllegalArgumentException if oktas is out of range
     */
    private static void validateOktas(Integer oktas) {
        if (oktas != null && (oktas < 1 || oktas > 8)) {
            throw new IllegalArgumentException("Oktas must be between 1 and 8: " + oktas);
        }
    }

    /**
     * Validate and normalize intensity modifier.
     *
     * @param intensity the intensity to validate
     * @return normalized intensity (uppercase, trimmed), or null if blank
     * @throws IllegalArgumentException if intensity is invalid
     */
    private static String validateAndNormalizeIntensity(String intensity) {
        if (intensity == null || intensity.isBlank()) {
            return null;
        }

        String normalized = intensity.trim().toUpperCase();

        for (String valid : VALID_INTENSITIES) {
            if (valid.equals(normalized)) {
                return normalized;
            }
        }

        throw new IllegalArgumentException("Invalid intensity: " + intensity);
    }

    /**
     * Validate and normalize location indicator.
     *
     * @param location the location to validate
     * @return normalized location (uppercase, trimmed), or null if blank
     * @throws IllegalArgumentException if location is invalid
     */
    private static String validateAndNormalizeLocation(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }

        String normalized = location.trim().toUpperCase();

        for (String valid : VALID_LOCATIONS) {
            if (valid.equals(normalized)) {
                return normalized;
            }
        }

        throw new IllegalArgumentException("Invalid location: " + location);
    }

    /**
     * Validate and normalize movement direction.
     *
     * @param movementDirection the movement direction to validate
     * @return normalized direction (uppercase, trimmed), or null if blank
     * @throws IllegalArgumentException if direction is invalid
     */
    private static String validateAndNormalizeMovementDirection(String movementDirection) {
        if (movementDirection == null || movementDirection.isBlank()) {
            return null;
        }

        String normalized = movementDirection.trim().toUpperCase();

        for (String valid : VALID_DIRECTIONS) {
            if (valid.equals(normalized)) {
                return normalized;
            }
        }

        throw new IllegalArgumentException("Invalid movement direction: " + movementDirection);
    }

    // ==================== Query Methods ====================

    /**
     * Check if this cloud type has okta coverage specified.
     *
     * @return true if oktas is not null
     */
    public boolean hasOktaCoverage() {
        return oktas != null;
    }

    /**
     * Check if this cloud type has intensity modifier.
     *
     * @return true if intensity is not null
     */
    public boolean hasIntensity() {
        return intensity != null;
    }

    /**
     * Check if this cloud type has location specified.
     *
     * @return true if location is not null
     */
    public boolean hasLocation() {
        return location != null;
    }

    /**
     * Check if this cloud type has movement direction.
     *
     * @return true if movementDirection is not null
     */
    public boolean hasMovement() {
        return movementDirection != null;
    }

    /**
     * Check if this is a trace amount.
     *
     * @return true if location is "TR"
     */
    public boolean isTrace() {
        return "TR".equals(location);
    }

    /**
     * Check if this cloud type is overhead.
     *
     * @return true if location is OHD or OHD-ALQDS
     */
    public boolean isOverhead() {
        return "OHD".equals(location) || OVERHEAD_ALL_QUADRANTS_CODE.equals(location);
    }

    /**
     * Check if this cloud type covers all quadrants.
     *
     * @return true if location is ALQDS or OHD-ALQDS
     */
    public boolean isAllQuadrants() {
        return "ALQDS".equals(location) || OVERHEAD_ALL_QUADRANTS_CODE.equals(location);
    }

    // ==================== Conversion Methods ====================

    /**
     * Get cloud type description.
     *
     * @return human-readable cloud type name
     */
    public String getCloudTypeDescription() {
        return switch (cloudType) {
            case "CU" -> "Cumulus";
            case "TCU" -> "Towering Cumulus";
            case "CF" -> "Cumuliform";
            case "ST" -> "Stratus";
            case "SC" -> "Stratocumulus";
            case "SF" -> "Stratiform";
            case "NS" -> "Nimbostratus";
            case "AS" -> "Altostratus";
            case "AC" -> "Altocumulus";
            case "CS" -> "Cirrostratus";
            case "CC" -> "Cirrocumulus";
            case "CI" -> "Cirrus";
            default -> cloudType;
        };
    }

    /**
     * Get okta coverage as fraction of sky.
     *
     * @return fraction 0.0-1.0, or null if not specified
     */
    public Double getOktasFraction() {
        if (oktas == null) {
            return null;
        }
        return oktas / 8.0;
    }

    /**
     * Get a human-readable summary of the cloud type.
     *
     * Examples:
     *   "Stratocumulus (1/8)"
     *   "Stratocumulus (trace)"
     *   "Cumulus (moderate, overhead)"
     *   "Altocumulus (8/8)"
     *   "Cirrus (moving NE)"
     *
     * @return formatted string describing the cloud type
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();

        // Add intensity if present
        if (intensity != null) {
            sb.append(intensity.toLowerCase()).append(" ");
        }

        // Add cloud type description
        sb.append(getCloudTypeDescription());

        // Build detail string
        StringBuilder details = new StringBuilder();

        // Add oktas
        if (oktas != null) {
            details.append(oktas).append("/8");
        }

        // Add location
        if (location != null) {
            if (!details.isEmpty()) {
                details.append(", ");
            }
            details.append(location.toLowerCase());
        }

        // Add movement
        if (movementDirection != null) {
            if (!details.isEmpty()) {
                details.append(", ");
            }
            details.append("moving ").append(movementDirection);
        }

        // Add details if any
        if (!details.isEmpty()) {
            sb.append(" (").append(details).append(")");
        }

        return sb.toString();
    }

    // ==================== Factory Methods ====================

    /**
     * Factory method for simple cloud type without modifiers.
     *
     * @param cloudType cloud type code
     * @return new CloudType
     */
    public static CloudType of(String cloudType) {
        return new CloudType(cloudType, null, null, null, null);
    }

    /**
     * Factory method for cloud type with oktas.
     *
     * @param cloudType cloud type code
     * @param oktas coverage in eighths
     * @return new CloudType
     */
    public static CloudType of(String cloudType, int oktas) {
        return new CloudType(cloudType, oktas, null, null, null);
    }

    /**
     * Factory method for cloud type with location.
     *
     * @param cloudType cloud type code
     * @param location location indicator
     * @return new CloudType
     */
    public static CloudType withLocation(String cloudType, String location) {
        return new CloudType(cloudType, null, null, location, null);
    }
}
