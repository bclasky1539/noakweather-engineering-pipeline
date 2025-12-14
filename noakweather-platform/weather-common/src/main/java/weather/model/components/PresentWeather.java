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
package weather.model.components;

/**
 * Represents present weather phenomena in aviation weather reports.
 *
 * Present weather describes current atmospheric conditions using standardized codes.
 * Multiple phenomena can be reported simultaneously (e.g., light rain and mist).
 *
 * Format: [Intensity][Descriptor][Precipitation][Obscuration][Other]
 * Examples:
 * - "-RA" = Light rain
 * - "+TSRA" = Heavy thunderstorm with rain
 * - "VCFG" = Fog in vicinity
 * - "BR" = Mist
 * - "NSW" = No significant weather
 *
 * @param intensity Intensity indicator: "-" (light), "+" (heavy), "VC" (vicinity), or null (moderate)
 * @param descriptor Weather descriptor: TS, FZ, SH, etc.
 * @param precipitation Precipitation type: RA, SN, DZ, etc.
 * @param obscuration Obscuration type: FG, BR, HZ, etc.
 * @param other Other phenomena: SQ, FC, DS, NSW, etc.
 * @param rawCode Original weather code as it appeared in the report
 *
 * @author bclasky1539
 *
 */
public record PresentWeather(
        String intensity,
        String descriptor,
        String precipitation,
        String obscuration,
        String other,
        String rawCode
) {

    /**
     * Compact constructor with validation.
     */
    public PresentWeather {
        validateRawCode(rawCode);
        // Allow nulls for optional components, but normalize to uppercase if present
        intensity = normalize(intensity);
        descriptor = normalize(descriptor);
        precipitation = normalize(precipitation);
        obscuration = normalize(obscuration);
        other = normalize(other);
    }

    /**
     * Parse a weather code string into its components.
     *
     * Format: [Intensity][Descriptor][Precipitation][Obscuration][Other]
     * Examples:
     * - "-RA" = Light rain
     * - "+TSRA" = Heavy thunderstorm with rain
     * - "VCFG" = Fog in vicinity
     * - "BR" = Mist
     * - "NSW" = No significant weather
     *
     * @param rawCode the weather code to parse
     * @return parsed PresentWeather object
     * @throws IllegalArgumentException if rawCode is invalid
     */
    public static PresentWeather parse(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("Weather code cannot be null or blank");
        }

        String code = rawCode.trim().toUpperCase();
        ParseContext ctx = new ParseContext(code);

        // Extract intensity
        extractIntensity(ctx);

        // Special case: NSW
        if (ctx.remaining().equals("NSW")) {
            return new PresentWeather(ctx.intensity, null, null, null, "NSW", code);
        }

        // Extract components in order
        ctx.descriptor = extractComponent(ctx, PresentWeather::isDescriptor);
        ctx.precipitation = extractComponent(ctx, PresentWeather::isPrecipitation);
        ctx.obscuration = extractComponent(ctx, PresentWeather::isObscuration);
        ctx.other = extractComponent(ctx, PresentWeather::isOther);

        // If unparsed remainder exists, treat as "other"
        if (ctx.pos < code.length()) {
            ctx.other = code.substring(ctx.pos);
        }

        return new PresentWeather(
                ctx.intensity,
                ctx.descriptor,
                ctx.precipitation,
                ctx.obscuration,
                ctx.other,
                code
        );
    }

    /**
     * Extract intensity from the beginning of the code.
     */
    private static void extractIntensity(ParseContext ctx) {
        if (ctx.code.startsWith("-") || ctx.code.startsWith("+")) {
            ctx.intensity = ctx.code.substring(0, 1);
            ctx.pos = 1;
        } else if (ctx.code.startsWith("VC")) {
            ctx.intensity = "VC";
            ctx.pos = 2;
        }
    }

    /**
     * Extract a 2-letter component using the provided validator.
     */
    private static String extractComponent(ParseContext ctx, java.util.function.Predicate<String> validator) {
        if (ctx.pos + 2 <= ctx.code.length()) {
            String potential = ctx.code.substring(ctx.pos, ctx.pos + 2);
            if (validator.test(potential)) {
                ctx.pos += 2;
                return potential;
            }
        }
        return null;
    }

    /**
     * Helper class to track parsing state.
     */
    private static class ParseContext {
        final String code;
        int pos = 0;
        String intensity = null;
        String descriptor = null;
        String precipitation = null;
        String obscuration = null;
        String other = null;

        ParseContext(String code) {
            this.code = code;
        }

        String remaining() {
            return code.substring(pos);
        }
    }

    /**
     * Check if a code is a valid descriptor.
     */
    private static boolean isDescriptor(String code) {
        return switch (code) {
            case "MI", "PR", "BC", "DR", "BL", "SH", "TS", "FZ" -> true;
            default -> false;
        };
    }

    /**
     * Check if a code is a valid precipitation type.
     */
    private static boolean isPrecipitation(String code) {
        return switch (code) {
            case "DZ", "RA", "SN", "SG", "IC", "PL", "GR", "GS", "UP" -> true;
            default -> false;
        };
    }

    /**
     * Check if a code is a valid obscuration type.
     */
    private static boolean isObscuration(String code) {
        return switch (code) {
            case "BR", "FG", "FU", "VA", "DU", "SA", "HZ", "PY" -> true;
            default -> false;
        };
    }

    /**
     * Check if a code is a valid other phenomenon.
     */
    private static boolean isOther(String code) {
        return switch (code) {
            case "PO", "SQ", "FC", "SS", "DS", "NSW" -> true;
            default -> false;
        };
    }

    /**
     * Validate that at least the raw code is provided.
     */
    private static void validateRawCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("Raw weather code cannot be null or blank");
        }
    }

    /**
     * Normalize string to uppercase and trim, or return null if blank.
     */
    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    /**
     * Check if this represents no significant weather.
     *
     * @return true if the code is "NSW" (No Significant Weather)
     */
    public boolean isNoSignificantWeather() {
        return "NSW".equals(other);
    }

    /**
     * Check if intensity is light.
     *
     * @return true if intensity is "-"
     */
    public boolean isLight() {
        return "-".equals(intensity);
    }

    /**
     * Check if intensity is heavy.
     *
     * @return true if intensity is "+"
     */
    public boolean isHeavy() {
        return "+".equals(intensity);
    }

    /**
     * Check if phenomena is in the vicinity (not at the station).
     *
     * @return true if intensity is "VC"
     */
    public boolean isVicinity() {
        return "VC".equals(intensity);
    }

    /**
     * Check if this includes thunderstorm.
     *
     * @return true if descriptor is "TS"
     */
    public boolean isThunderstorm() {
        return "TS".equals(descriptor);
    }

    /**
     * Check if this includes freezing conditions.
     *
     * @return true if descriptor is "FZ"
     */
    public boolean isFreezing() {
        return "FZ".equals(descriptor);
    }

    /**
     * Check if this includes showers.
     *
     * @return true if descriptor is "SH"
     */
    public boolean isShowers() {
        return "SH".equals(descriptor);
    }

    /**
     * Check if this includes any precipitation.
     *
     * @return true if precipitation type is present
     */
    public boolean hasPrecipitation() {
        return precipitation != null && !precipitation.isBlank();
    }

    /**
     * Check if this includes reduced visibility obscuration.
     *
     * @return true if obscuration type is present
     */
    public boolean hasObscuration() {
        return obscuration != null && !obscuration.isBlank();
    }

    /**
     * Get the intensity description in human-readable format.
     *
     * @return "Light", "Heavy", "Vicinity", "Moderate", or ""
     */
    public String getIntensityDescription() {
        if (intensity == null) {
            return "Moderate";
        }
        return switch (intensity) {
            case "-" -> "Light";
            case "+" -> "Heavy";
            case "VC" -> "Vicinity";
            default -> "";
        };
    }

    /**
     * Get a human-readable description of the weather phenomena.
     *
     * @return formatted description
     */
    public String getDescription() {
        if (isNoSignificantWeather()) {
            return "No Significant Weather";
        }

        StringBuilder sb = new StringBuilder();

        // Add intensity - always include it (even "Moderate")
        sb.append(getIntensityDescription()).append(" ");

        // Add descriptor
        if (descriptor != null) {
            sb.append(getDescriptorDescription()).append(" ");
        }

        // Add precipitation
        if (precipitation != null) {
            sb.append(getPrecipitationDescription()).append(" ");
        }

        // Add obscuration
        if (obscuration != null) {
            sb.append(getObscurationDescription()).append(" ");
        }

        // Add other
        if (other != null) {
            sb.append(getOtherDescription()).append(" ");
        }

        return sb.toString().trim();
    }

    /**
     * Get descriptor description.
     */
    private String getDescriptorDescription() {
        if (descriptor == null) return "";
        return switch (descriptor) {
            case "MI" -> "Shallow";
            case "PR" -> "Partial";
            case "BC" -> "Patches";
            case "DR" -> "Drifting";
            case "BL" -> "Blowing";
            case "SH" -> "Showers";
            case "TS" -> "Thunderstorm";
            case "FZ" -> "Freezing";
            default -> descriptor;
        };
    }

    /**
     * Get precipitation description.
     */
    private String getPrecipitationDescription() {
        if (precipitation == null) return "";
        return switch (precipitation) {
            case "DZ" -> "Drizzle";
            case "RA" -> "Rain";
            case "SN" -> "Snow";
            case "SG" -> "Snow Grains";
            case "IC" -> "Ice Crystals";
            case "PL" -> "Ice Pellets";
            case "GR" -> "Hail";
            case "GS" -> "Small Hail/Snow Pellets";
            case "UP" -> "Unknown Precipitation";
            default -> precipitation;
        };
    }

    /**
     * Get obscuration description.
     */
    private String getObscurationDescription() {
        if (obscuration == null) return "";
        return switch (obscuration) {
            case "BR" -> "Mist";
            case "FG" -> "Fog";
            case "FU" -> "Smoke";
            case "VA" -> "Volcanic Ash";
            case "DU" -> "Dust";
            case "SA" -> "Sand";
            case "HZ" -> "Haze";
            case "PY" -> "Spray";
            default -> obscuration;
        };
    }

    /**
     * Get other phenomena description.
     */
    private String getOtherDescription() {
        if (other == null) return "";
        return switch (other) {
            case "PO" -> "Dust/Sand Whirls";
            case "SQ" -> "Squall";
            case "FC" -> "Funnel Cloud/Tornado/Waterspout";
            case "SS" -> "Sandstorm";
            case "DS" -> "Duststorm";
            case "NSW" -> "No Significant Weather";
            default -> other;
        };
    }

    /**
     * Factory method for simple weather code.
     *
     * @param rawCode the weather code (e.g., "RA", "-SN", "+TSRA")
     * @return new PresentWeather instance
     */
    public static PresentWeather of(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("Raw code cannot be null or blank");
        }
        return new PresentWeather(null, null, null, null, null, rawCode.trim().toUpperCase());
    }
}
