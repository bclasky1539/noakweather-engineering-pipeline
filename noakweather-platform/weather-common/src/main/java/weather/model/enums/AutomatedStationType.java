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
package weather.model.enums;

/**
 * Type of automated weather station as indicated in METAR remarks section.
 *
 * The automated station type (AO1 or AO2) is coded in all METAR/SPECI
 * reports from automated stations to indicate the sensor capabilities.
 *
 * According to Federal Meteorological Handbook No. 1:
 * - AO1 - Automated station WITHOUT a precipitation discriminator
 * - AO2 - Automated station WITH a precipitation discriminator
 *
 * A precipitation discriminator is a sensor that can distinguish between
 * liquid and frozen precipitation (rain vs. snow). Stations with AO2 capability
 * provide more detailed precipitation type information.
 *
 * @author bclasky1539
 *
 */
public enum AutomatedStationType {

    /**
     * Automated Observing System WITHOUT precipitation discriminator.
     * Cannot distinguish between rain and snow automatically.
     */
    AO1("AO1", "Automated station without precipitation discriminator"),

    /**
     * Automated Observing System WITH precipitation discriminator.
     * Can distinguish between liquid and frozen precipitation.
     */
    AO2("AO2", "Automated station with precipitation discriminator");

    private final String code;
    private final String description;

    /**
     * Constructs an AutomatedStationType.
     *
     * @param code the METAR code (AO1 or AO2)
     * @param description human-readable description
     */
    AutomatedStationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Gets the METAR code for this automated station type.
     *
     * @return the code (AO1 or AO2)
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the human-readable description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this station has a precipitation discriminator.
     *
     * @return true if AO2 (has discriminator), false if AO1
     */
    public boolean hasPrecipitationDiscriminator() {
        return this == AO2;
    }

    /**
     * Parses an automated station type from a digit (1 or 2).
     *
     * @param typeDigit the digit from the METAR (1 or 2)
     * @return the corresponding AutomatedStationType
     * @throws IllegalArgumentException if digit is not 1 or 2
     */
    public static AutomatedStationType fromDigit(int typeDigit) {
        return switch (typeDigit) {
            case 1 -> AO1;
            case 2 -> AO2;
            default -> throw new IllegalArgumentException(
                    "Invalid automated station type: " + typeDigit + ". Must be 1 or 2.");
        };
    }

    /**
     * Parses an automated station type from a digit string (1 or 2).
     *
     * @param typeDigit the digit string from the METAR ("1" or "2")
     * @return the corresponding AutomatedStationType
     * @throws IllegalArgumentException if string is not "1" or "2"
     * @throws NumberFormatException if string is not a valid number
     */
    public static AutomatedStationType fromDigit(String typeDigit) {
        if (typeDigit == null || typeDigit.isBlank()) {
            throw new IllegalArgumentException("Automated station type digit cannot be null or blank");
        }
        return fromDigit(Integer.parseInt(typeDigit.trim()));
    }

    /**
     * Parses an automated station type from the full code (AO1 or AO2).
     * Handles OCR errors where O might be read as 0.
     *
     * @param code the METAR code (AO1, AO2, A01, or A02)
     * @return the corresponding AutomatedStationType
     * @throws IllegalArgumentException if code is invalid
     */
    public static AutomatedStationType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Automated station type code cannot be null or blank");
        }

        String normalized = code.trim().toUpperCase();

        // Handle both AO1/AO2 and OCR errors A01/A02
        return switch (normalized) {
            case "AO1", "A01" -> AO1;
            case "AO2", "A02" -> AO2;
            default -> throw new IllegalArgumentException(
                    "Invalid automated station type code: " + code + ". Must be AO1, AO2, A01, or A02.");
        };
    }

    @Override
    public String toString() {
        return code;
    }
}
