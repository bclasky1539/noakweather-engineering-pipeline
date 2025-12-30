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
 * Represents an automated maintenance indicator from METAR remarks.
 *
 * These indicators show equipment or sensor failures at automated weather stations.
 * Examples include:
 * - RVRNO: RVR (Runway Visual Range) missing
 * - PWINO: Precipitation identifier information not available
 * - PNO: Precipitation amount not available
 * - FZRANO: Freezing rain information not available
 * - TSNO: Thunderstorm information not available
 * - VISNO [LOC]: Visibility at location not available (e.g., VISNO RWY06)
 * - CHINO [LOC]: Cloud height indicator at location not available (e.g., CHINO RWY06)
 * - $: Station requires maintenance check
 *
 * @param type the maintenance indicator type (RVRNO, PWINO, etc., or $)
 * @param location optional location specifier (e.g., RWY06) - only for VISNO and CHINO
 *
 * @author bclasky1539
 *
 */
public record AutomatedMaintenanceIndicator(
        String type,
        String location
) {

    private static final String RVRNO_MAINT_CODE = "RVRNO";
    private static final String PWINO_MAINT_CODE = "PWINO";
    private static final String PNO_MAINT_CODE = "PNO";
    private static final String FZRANO_MAINT_CODE = "FZRANO";
    private static final String TSNO_MAINT_CODE = "TSNO";
    private static final String VISNO_MAINT_CODE = "VISNO";
    private static final String CHINO_MAINT_CODE = "CHINO";
    private static final String STATION_MAINT_CODE = "$";

    // Valid maintenance indicator types
    private static final String[] VALID_TYPES = {
            RVRNO_MAINT_CODE, PWINO_MAINT_CODE, PNO_MAINT_CODE, FZRANO_MAINT_CODE,
            TSNO_MAINT_CODE, VISNO_MAINT_CODE, CHINO_MAINT_CODE, STATION_MAINT_CODE
    };

    /**
     * Compact constructor with validation.
     */
    public AutomatedMaintenanceIndicator {
        type = validateAndNormalizeType(type);
        location = normalizeLocation(location);
    }

    /**
     * Validate and normalize maintenance indicator type.
     *
     * @param type the type to validate
     * @return normalized type (uppercase, trimmed)
     * @throws IllegalArgumentException if type is invalid
     */
    private static String validateAndNormalizeType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Maintenance indicator type cannot be null or blank");
        }

        String normalized = type.trim().toUpperCase();

        for (String valid : VALID_TYPES) {
            if (valid.equals(normalized)) {
                return normalized;
            }
        }

        throw new IllegalArgumentException("Invalid maintenance indicator type: " + type);
    }

    /**
     * Normalize location specifier.
     *
     * @param location the location to normalize
     * @return normalized location (uppercase, trimmed), or null if blank
     */
    private static String normalizeLocation(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        return location.trim().toUpperCase();
    }

    // ==================== Query Methods ====================

    /**
     * Check if this indicator has a location specified.
     *
     * @return true if location is not null
     */
    public boolean hasLocation() {
        return location != null;
    }

    /**
     * Check if this is a RVR (Runway Visual Range) not available indicator.
     *
     * @return true if type is RVRNO
     */
    public boolean isRVRNotAvailable() {
        return RVRNO_MAINT_CODE.equals(type);
    }

    /**
     * Check if this is a precipitation identifier not available indicator.
     *
     * @return true if type is PWINO
     */
    public boolean isPresentWeatherNotAvailable() {
        return PWINO_MAINT_CODE.equals(type);
    }

    /**
     * Check if this is a precipitation amount not available indicator.
     *
     * @return true if type is PNO
     */
    public boolean isPrecipitationNotAvailable() {
        return "PNO".equals(type);
    }

    /**
     * Check if this is a freezing rain not available indicator.
     *
     * @return true if type is FZRANO
     */
    public boolean isFreezingRainNotAvailable() {
        return FZRANO_MAINT_CODE.equals(type);
    }

    /**
     * Check if this is a thunderstorm information not available indicator.
     *
     * @return true if type is TSNO
     */
    public boolean isThunderstormNotAvailable() {
        return "TSNO".equals(type);
    }

    /**
     * Check if this is a visibility not available indicator.
     *
     * @return true if type is VISNO
     */
    public boolean isVisibilityNotAvailable() {
        return VISNO_MAINT_CODE.equals(type);
    }

    /**
     * Check if this is a cloud height indicator not available.
     *
     * @return true if type is CHINO
     */
    public boolean isCloudHeightNotAvailable() {
        return CHINO_MAINT_CODE.equals(type);
    }

    /**
     * Check if this is the general maintenance required indicator.
     *
     * @return true if type is $
     */
    public boolean isMaintenanceCheck() {
        return "$".equals(type);
    }

    // ==================== Conversion Methods ====================

    /**
     * Get a human-readable description of this maintenance indicator.
     *
     * @return formatted description
     */
    public String getDescription() {
        String desc = switch (type) {
            case RVRNO_MAINT_CODE -> "Runway Visual Range not available";
            case PWINO_MAINT_CODE -> "Precipitation identifier information not available";
            case PNO_MAINT_CODE -> "Precipitation amount not available";
            case FZRANO_MAINT_CODE -> "Freezing rain information not available";
            case TSNO_MAINT_CODE -> "Thunderstorm information not available";
            case VISNO_MAINT_CODE -> "Visibility not available";
            case CHINO_MAINT_CODE -> "Cloud height indicator not available";
            case STATION_MAINT_CODE -> "Station requires maintenance";
            default -> type;
        };

        if (location != null) {
            desc += " at " + location;
        }

        return desc;
    }

    /**
     * Get a compact string representation.
     *
     * @return compact format (e.g., "VISNO RWY06", "TSNO", "$")
     */
    @Override
    public String toString() {
        if (location != null) {
            return type + " " + location;
        }
        return type;
    }

    // ==================== Factory Methods ====================

    /**
     * Factory method for maintenance indicator without location.
     *
     * @param type the indicator type
     * @return new AutomatedMaintenanceIndicator
     */
    public static AutomatedMaintenanceIndicator of(String type) {
        return new AutomatedMaintenanceIndicator(type, null);
    }

    /**
     * Factory method for maintenance indicator with location.
     *
     * @param type the indicator type
     * @param location the location specifier
     * @return new AutomatedMaintenanceIndicator
     */
    public static AutomatedMaintenanceIndicator of(String type, String location) {
        return new AutomatedMaintenanceIndicator(type, location);
    }

    /**
     * Factory method for maintenance check indicator ($).
     *
     * @return new AutomatedMaintenanceIndicator for maintenance check
     */
    public static AutomatedMaintenanceIndicator maintenanceCheck() {
        return new AutomatedMaintenanceIndicator("$", null);
    }
}
