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

import weather.model.components.Visibility;

/**
 * Immutable value object representing variable visibility from remarks section.
 *
 * Variable visibility reports when visibility is fluctuating between two values.
 * This is coded in remarks as "VIS minVmax" where visibility varies between
 * the minimum and maximum values.
 *
 * Examples:
 * - "VIS 1/2V2" → Visibility varying from 1/2 to 2 statute miles
 * - "VIS NE 2V4" → Northeast visibility varying from 2 to 4 statute miles
 * - "VIS RWY 1/4V1" → Runway visibility varying from 1/4 to 1 statute mile
 *
 * @param minimumVisibility Minimum visibility value
 * @param maximumVisibility Maximum visibility value
 * @param direction Optional direction sector (N, NE, E, SE, S, SW, W, NW)
 * @param location Optional location qualifier (RWY for runway, or other sector info)
 *
 * @author bclasky1539
 *
 */
public record VariableVisibility(
        Visibility minimumVisibility,
        Visibility maximumVisibility,
        String direction,
        String location
) {

    /**
     * Compact constructor with validation.
     */
    public VariableVisibility {
        validateMinimumVisibility(minimumVisibility);
        validateMaximumVisibility(maximumVisibility);
        validateMinLessThanOrEqualMax(minimumVisibility, maximumVisibility);
        validateDirection(direction);
    }

    // ==================== Validation Helper Methods ====================

    /**
     * Validate that minimum visibility is present.
     */
    private static void validateMinimumVisibility(Visibility minimumVisibility) {
        if (minimumVisibility == null) {
            throw new IllegalArgumentException("Minimum visibility is required");
        }
    }

    /**
     * Validate that maximum visibility is present.
     */
    private static void validateMaximumVisibility(Visibility maximumVisibility) {
        if (maximumVisibility == null) {
            throw new IllegalArgumentException("Maximum visibility is required");
        }
    }

    /**
     * Validate that minimum visibility is not greater than maximum.
     */
    private static void validateMinLessThanOrEqualMax(
            Visibility minimumVisibility,
            Visibility maximumVisibility) {

        // Compare using statute miles for consistency
        Double minSM = minimumVisibility.toStatuteMiles();
        Double maxSM = maximumVisibility.toStatuteMiles();

        if (minSM != null && maxSM != null && minSM > maxSM) {
            throw new IllegalArgumentException(
                    String.format("Minimum visibility (%.2f SM) cannot be greater than maximum (%.2f SM)",
                            minSM, maxSM)
            );
        }
    }

    /**
     * Validate direction if present.
     */
    private static void validateDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return;
        }

        // Valid directions: N, NE, E, SE, S, SW, W, NW
        String[] validDirections = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        boolean valid = false;
        for (String validDir : validDirections) {
            if (validDir.equals(direction)) {
                valid = true;
                break;
            }
        }

        if (!valid) {
            throw new IllegalArgumentException(
                    "Invalid direction: " + direction + ". Must be N, NE, E, SE, S, SW, W, or NW"
            );
        }
    }

    // ==================== Query Methods ====================

    /**
     * Check if variable visibility has a directional component.
     *
     * @return true if direction is specified
     */
    public boolean hasDirection() {
        return direction != null && !direction.isBlank();
    }

    /**
     * Check if variable visibility is for a specific location (e.g., runway).
     *
     * @return true if location is specified
     */
    public boolean hasLocation() {
        return location != null && !location.isBlank();
    }

    /**
     * Get the visibility range as a formatted string.
     *
     * @return formatted range (e.g., "1/2 to 2 SM")
     */
    public String getRange() {
        return String.format("%s to %s",
                formatVisibility(minimumVisibility),
                formatVisibility(maximumVisibility));
    }

    /**
     * Get the complete variable visibility description.
     *
     * @return formatted description with direction and location if present
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();

        if (hasDirection()) {
            desc.append(direction).append(" ");
        }

        desc.append("visibility varying from ");
        desc.append(formatVisibility(minimumVisibility));
        desc.append(" to ");
        desc.append(formatVisibility(maximumVisibility));

        if (hasLocation()) {
            desc.append(" (").append(location).append(")");
        }

        return desc.toString();
    }

    /**
     * Calculate the visibility spread (difference between max and min).
     *
     * @return spread in statute miles, or null if it cannot be calculated
     */
    public Double getSpread() {
        Double minSM = minimumVisibility.toStatuteMiles();
        Double maxSM = maximumVisibility.toStatuteMiles();

        if (minSM == null || maxSM == null) {
            return null;
        }

        return maxSM - minSM;
    }

    /**
     * Check if visibility variability indicates significant weather.
     * A spread > 1 SM suggests rapidly changing conditions.
     *
     * @return true if spread is significant (> 1 SM)
     */
    public boolean hasSignificantVariability() {
        Double spread = getSpread();
        return spread != null && spread > 1.0;
    }

    // ==================== Helper Methods ====================

    /**
     * Format a visibility value for display.
     */
    private String formatVisibility(Visibility vis) {
        if (vis.isSpecialCondition()) {
            return vis.specialCondition();
        }

        if (vis.distanceValue() == null) {
            return "unknown";
        }

        StringBuilder formatted = new StringBuilder();

        if (vis.lessThan()) {
            formatted.append("less than ");
        } else if (vis.greaterThan()) {
            formatted.append("greater than ");
        }

        // Format the value
        double value = vis.distanceValue();
        if (value == Math.floor(value)) {
            formatted.append(String.format("%.0f", value));
        } else {
            formatted.append(formatFraction(value));
        }

        // Add unit
        formatted.append(" ");
        formatted.append(vis.unit());

        return formatted.toString();
    }

    /**
     * Format a decimal value as a fraction if appropriate.
     * Examples: 0.25 → "1/4", 0.5 → "1/2", 1.25 → "1 1/4"
     */
    private String formatFraction(double value) {
        // Check for common fractions
        double fractionalPart = value - Math.floor(value);
        int wholePart = (int) Math.floor(value);

        if (Math.abs(fractionalPart - 0.25) < 0.01) {
            return wholePart > 0 ? wholePart + " 1/4" : "1/4";
        } else if (Math.abs(fractionalPart - 0.5) < 0.01) {
            return wholePart > 0 ? wholePart + " 1/2" : "1/2";
        } else if (Math.abs(fractionalPart - 0.75) < 0.01) {
            return wholePart > 0 ? wholePart + " 3/4" : "3/4";
        } else if (Math.abs(fractionalPart - 0.33) < 0.01) {
            return wholePart > 0 ? wholePart + " 1/3" : "1/3";
        } else if (Math.abs(fractionalPart - 0.67) < 0.01) {
            return wholePart > 0 ? wholePart + " 2/3" : "2/3";
        }

        // Default to decimal format
        return String.format("%.2f", value);
    }

    // ==================== Factory Methods ====================

    /**
     * Create variable visibility without direction or location.
     *
     * @param min Minimum visibility
     * @param max Maximum visibility
     * @return VariableVisibility instance
     */
    public static VariableVisibility of(Visibility min, Visibility max) {
        return new VariableVisibility(min, max, null, null);
    }

    /**
     * Create variable visibility with direction.
     *
     * @param min Minimum visibility
     * @param max Maximum visibility
     * @param direction Direction sector (N, NE, E, etc.)
     * @return VariableVisibility instance
     */
    public static VariableVisibility withDirection(Visibility min, Visibility max, String direction) {
        return new VariableVisibility(min, max, direction, null);
    }

    /**
     * Create variable visibility with location.
     *
     * @param min Minimum visibility
     * @param max Maximum visibility
     * @param location Location qualifier (e.g., "RWY")
     * @return VariableVisibility instance
     */
    public static VariableVisibility withLocation(Visibility min, Visibility max, String location) {
        return new VariableVisibility(min, max, null, location);
    }
}
