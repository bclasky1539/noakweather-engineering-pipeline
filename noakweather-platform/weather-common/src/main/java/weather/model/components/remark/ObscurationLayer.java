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
 * Represents an obscuration layer in the atmosphere.
 * Format: [Coverage] [Phenomenon] [Height] where height is in hundreds of feet.
 *
 * Examples:
 * - FEW FG 000 → Few fog at ground level (0 feet)
 * - SCT FU 010 → Scattered smoke at 1000 feet
 * - BKN BR 005 → Broken mist at 500 feet
 *
 * @param coverage sky coverage (FEW, SCT, BKN, OVC)
 * @param phenomenon obscuration type (FG, BR, FU, HZ, etc.)
 * @param heightFeet height of obscuration base in feet
 *
 * @author bclasky1539
 *
 */
public record ObscurationLayer(
        String coverage,
        String phenomenon,
        int heightFeet
) {

    /**
     * Compact constructor with validation.
     */
    public ObscurationLayer {
        if (coverage == null || coverage.isBlank()) {
            throw new IllegalArgumentException("Coverage cannot be null or blank");
        }
        if (phenomenon == null || phenomenon.isBlank()) {
            throw new IllegalArgumentException("Phenomenon cannot be null or blank");
        }
        if (heightFeet < 0) {
            throw new IllegalArgumentException("Height cannot be negative");
        }
    }

    /**
     * Create an ObscurationLayer from encoded values.
     *
     * @param coverage sky coverage code (FEW, SCT, BKN, OVC)
     * @param phenomenon obscuration phenomenon code (FG, BR, FU, HZ, etc.)
     * @param hundreds height in hundreds of feet (e.g., 000 = 0 ft, 010 = 1000 ft)
     * @return ObscurationLayer instance
     * @throws IllegalArgumentException if validation fails
     */
    public static ObscurationLayer fromHundreds(String coverage, String phenomenon, int hundreds) {
        int heightFeet = hundreds * 100;
        return new ObscurationLayer(coverage, phenomenon, heightFeet);
    }

    /**
     * Check if obscuration is at ground level (0 feet).
     *
     * @return true if height is 0 feet
     */
    public boolean isGroundLevel() {
        return heightFeet == 0;
    }

    /**
     * Check if obscuration is low (below 1000 feet).
     *
     * @return true if height is below 1000 feet
     */
    public boolean isLowLevel() {
        return heightFeet < 1000;
    }

    /**
     * Get coverage description.
     *
     * @return human-readable coverage description
     */
    public String getCoverageDescription() {
        return switch (coverage) {
            case "FEW" -> "Few";
            case "SCT" -> "Scattered";
            case "BKN" -> "Broken";
            case "OVC" -> "Overcast";
            default -> coverage;
        };
    }

    /**
     * Get phenomenon description.
     *
     * @return human-readable phenomenon description
     */
    public String getPhenomenonDescription() {
        return switch (phenomenon) {
            case "FG" -> "Fog";
            case "BR" -> "Mist";
            case "FU" -> "Smoke";
            case "HZ" -> "Haze";
            case "DU" -> "Dust";
            case "SA" -> "Sand";
            case "VA" -> "Volcanic Ash";
            case "PY" -> "Spray";
            default -> phenomenon;
        };
    }

    /**
     * Get a human-readable summary.
     *
     * @return formatted summary string
     */
    public String getSummary() {
        String coverageDesc = getCoverageDescription();
        String phenomenonDesc = getPhenomenonDescription();

        if (isGroundLevel()) {
            return String.format("%s %s at ground level", coverageDesc, phenomenonDesc);
        }
        return String.format("%s %s at %d ft", coverageDesc, phenomenonDesc, heightFeet);
    }

    @Override
    public String toString() {
        return String.format("ObscurationLayer{%s %s %d ft}", coverage, phenomenon, heightFeet);
    }
}
