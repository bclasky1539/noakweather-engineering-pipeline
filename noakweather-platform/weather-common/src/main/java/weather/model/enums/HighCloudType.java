package weather.model.enums;

/**
 * Predominant high cloud type (C_H), per WMO Cloud Atlas coding
 * instructions section 2.8.3.3, as used in the METAR/synoptic
 * "8/C_L C_M C_H" remark group.
 *
 * @author bclasky1539
 */
public enum HighCloudType {
    NONE,
    CIRRUS_FIBRATUS_OR_UNCINUS,
    CIRRUS_SPISSATUS_OR_CASTELLANUS_OR_FLOCCUS,
    CIRRUS_SPISSATUS_FROM_CUMULONIMBUS,
    CIRRUS_INVADING,
    CIRROSTRATUS_INVADING_BELOW_45_DEGREES,
    CIRROSTRATUS_INVADING_ABOVE_45_DEGREES,
    CIRROSTRATUS_COVERING_WHOLE_SKY,
    CIRROSTRATUS_NOT_INVADING_NOT_COVERING,
    CIRROCUMULUS_PREDOMINANT,
    OBSCURED;

    /**
     * Parses the transmitted digit (or "/" for obscured) into a HighCloudType.
     * <p>
     * Relies on enum declaration order matching the WMO digit order
     * (NONE=0 through CIRROCUMULUS_PREDOMINANT=9); OBSCURED is handled
     * separately since "/" has no corresponding digit.
     *
     * @param code a single digit "0"-"9", or "/" for obscured-above-overcast
     * @return the corresponding HighCloudType
     * @throws IllegalArgumentException if code is not a recognized value
     */
    public static HighCloudType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("High cloud type code cannot be null");
        }
        if ("/".equals(code)) {
            return OBSCURED;
        }
        if (code.length() == 1 && Character.isDigit(code.charAt(0))) {
            return values()[code.charAt(0) - '0'];
        }
        throw new IllegalArgumentException("Invalid high cloud type code: " + code);
    }

    /**
     * @return a human-readable description of this cloud type
     */
    public String getSummary() {
        return switch (this) {
            case NONE -> "No high cloud";
            case CIRRUS_FIBRATUS_OR_UNCINUS -> "Cirrus fibratus or uncinus";
            case CIRRUS_SPISSATUS_OR_CASTELLANUS_OR_FLOCCUS ->
                    "Cirrus spissatus (non-CB) or castellanus/floccus, predominant";
            case CIRRUS_SPISSATUS_FROM_CUMULONIMBUS -> "Cirrus spissatus originating from Cumulonimbus";
            case CIRRUS_INVADING -> "Cirrus uncinus or fibratus, progressively invading";
            case CIRROSTRATUS_INVADING_BELOW_45_DEGREES -> "Cirrostratus invading, below 45° above horizon";
            case CIRROSTRATUS_INVADING_ABOVE_45_DEGREES -> "Cirrostratus invading, above 45° above horizon";
            case CIRROSTRATUS_COVERING_WHOLE_SKY -> "Cirrostratus covering the whole sky";
            case CIRROSTRATUS_NOT_INVADING_NOT_COVERING -> "Cirrostratus, not invading, not covering whole sky";
            case CIRROCUMULUS_PREDOMINANT -> "Cirrocumulus alone or predominant";
            case OBSCURED -> "Obscured by overcast layer below";
        };
    }
}
