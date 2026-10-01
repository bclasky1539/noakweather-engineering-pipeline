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

/**
 * Type of automated weather station as indicated in METAR remarks section.
 * <p>
 * The automated station type (AO1, AO2, AO1A, or AO2A) is coded in all
 * METAR/SPECI reports from automated stations to indicate the sensor
 * capabilities and whether the report was manually augmented.
 * <p>
 * According to Federal Meteorological Handbook No. 1:
 * - AO1 - Automated station WITHOUT a precipitation discriminator
 * - AO2 - Automated station WITH a precipitation discriminator
 * <p>
 * A precipitation discriminator is a sensor that can distinguish between
 * liquid and frozen precipitation (rain vs. snow). Stations with AO2 capability
 * provide more detailed precipitation type information.
 * <p>
 * Per US Air Force Pamphlet 11-238 ("Aircrew Quick Reference to METAR
 * and TAF Codes", 17 March 2011) and independently confirmed via
 * https://aviation.stackexchange.com/questions/97911/, a trailing "A"
 * suffix (AO1A, AO2A) indicates the automated observation was manually
 * augmented by a human observer. Augmentation is independent of the
 * precipitation-discriminator distinction above.
 *
 * @author bclasky1539
 *
 */
public enum AutomatedStationType {

    /**
     * Automated Observing System WITHOUT precipitation discriminator.
     * Cannot distinguish between rain and snow automatically.
     */
    AO1("AO1", "Automated station without precipitation discriminator", false),

    /**
     * Automated Observing System WITH precipitation discriminator.
     * Can distinguish between liquid and frozen precipitation.
     */
    AO2("AO2", "Automated station with precipitation discriminator", false),

    /**
     * Automated Observing System WITHOUT precipitation discriminator,
     * manually augmented by a human observer.
     */
    AO1A("AO1A", "Automated station without precipitation discriminator, manually augmented", true),

    /**
     * Automated Observing System WITH precipitation discriminator,
     * manually augmented by a human observer.
     */
    AO2A("AO2A", "Automated station with precipitation discriminator, manually augmented", true);

    private final String code;
    private final String description;
    private final boolean augmented;

    /**
     * Constructs an AutomatedStationType.
     *
     * @param code        the METAR code (AO1, AO2, AO1A, or AO2A)
     * @param description human-readable description
     * @param augmented   whether this type indicates manual augmentation
     */
    AutomatedStationType(String code, String description, boolean augmented) {
        this.code = code;
        this.description = description;
        this.augmented = augmented;
    }

    /**
     * Gets the METAR code for this automated station type.
     *
     * @return the code (AO1, AO2, AO1A, or AO2A)
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
     * @return true if AO2 or AO2A (has discriminator), false if AO1 or AO1A
     */
    public boolean hasPrecipitationDiscriminator() {
        return this == AO2 || this == AO2A;
    }

    /**
     * Checks if this report was manually augmented by a human observer.
     *
     * @return true if AO1A or AO2A, false if AO1 or AO2
     */
    public boolean hasManualAugmentation() {
        return augmented;
    }

    /**
     * Parses an automated station type from a digit (1 or 2), with no
     * augmentation information. Callers needing augmentation should
     * detect the trailing "A" separately and combine it with this
     * result (e.g. via {@link #fromDigitAndAugmentation(int, boolean)}).
     *
     * @param typeDigit the digit from the METAR (1 or 2)
     * @return the corresponding base AutomatedStationType (AO1 or AO2), never augmented
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
     * Parses an automated station type from a digit string (1 or 2), with
     * no augmentation information.
     *
     * @param typeDigit the digit string from the METAR ("1" or "2")
     * @return the corresponding base AutomatedStationType (AO1 or AO2), never augmented
     * @throws IllegalArgumentException if string is not "1" or "2"
     * @throws NumberFormatException    if string is not a valid number
     */
    public static AutomatedStationType fromDigit(String typeDigit) {
        if (typeDigit == null || typeDigit.isBlank()) {
            throw new IllegalArgumentException("Automated station type digit cannot be null or blank");
        }
        return fromDigit(Integer.parseInt(typeDigit.trim()));
    }

    /**
     * Resolves an automated station type from a digit (1 or 2) plus whether
     * the report was manually augmented, combining the two independent
     * dimensions into the correct one of the four constants.
     *
     * @param typeDigit the digit from the METAR (1 or 2)
     * @param augmented whether a trailing "A" augmentation suffix was present
     * @return the corresponding AutomatedStationType
     * @throws IllegalArgumentException if digit is not 1 or 2
     */
    public static AutomatedStationType fromDigitAndAugmentation(int typeDigit, boolean augmented) {
        AutomatedStationType base = fromDigit(typeDigit);
        if (!augmented) {
            return base;
        }
        return base == AO1 ? AO1A : AO2A;
    }

    /**
     * Parses an automated station type from the full code (AO1, AO2, AO1A,
     * or AO2A). Handles OCR errors where O might be read as 0.
     *
     * @param code the METAR code (AO1, AO2, AO1A, AO2A, A01, A02, A01A, or A02A)
     * @return the corresponding AutomatedStationType
     * @throws IllegalArgumentException if code is invalid
     */
    public static AutomatedStationType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Automated station type code cannot be null or blank");
        }

        String normalized = code.trim().toUpperCase();

        // Handle both AO1/AO2 and OCR errors A01/A02, plus their augmented "A" variants
        return switch (normalized) {
            case "AO1", "A01" -> AO1;
            case "AO2", "A02" -> AO2;
            case "AO1A", "A01A" -> AO1A;
            case "AO2A", "A02A" -> AO2A;
            default -> throw new IllegalArgumentException(
                    "Invalid automated station type code: " + code
                            + ". Must be AO1, AO2, AO1A, AO2A, A01, A02, A01A, or A02A.");
        };
    }

    @Override
    public String toString() {
        return code;
    }
}
