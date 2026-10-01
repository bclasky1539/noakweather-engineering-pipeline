package weather.model.enums;

/**
 * Predominant middle cloud type (C_M), per WMO Cloud Atlas coding
 * instructions section 2.8.3.2, as used in the METAR/synoptic
 * "8/C_L C_M C_H" remark group.
 * <p>
 * WMO documents three distinct observational conditions that all transmit
 * as digit 7 (Ac + As/Ns coexisting; Ac translucidus/opacus at 2+ levels;
 * Ac single-level predominantly opacus). Decoding is one-way: the digit
 * maps to one representative label, not back to which specific condition
 * an observer selected.
 *
 * @author bclasky1539
 */
public enum MiddleCloudType {
    NONE,
    ALTOSTRATUS_TRANSLUCIDUS,
    ALTOSTRATUS_DENSE_OR_NIMBOSTRATUS,
    ALTOCUMULUS_TRANSLUCIDUS_SINGLE_LEVEL,
    ALTOCUMULUS_PATCHES_CHANGING,
    ALTOCUMULUS_INVADING,
    ALTOCUMULUS_CUMULOGENITUS,
    ALTOCUMULUS_MULTI_LEVEL_OR_WITH_ALTOSTRATUS_OR_OPACUS,
    ALTOCUMULUS_CASTELLANUS_OR_FLOCCUS,
    ALTOCUMULUS_CHAOTIC_SKY,
    OBSCURED;

    /**
     * Parses the transmitted digit (or "/" for obscured) into a MiddleCloudType.
     * <p>
     * Relies on enum declaration order matching the WMO digit order
     * (NONE=0 through ALTOCUMULUS_CHAOTIC_SKY=9); OBSCURED is handled
     * separately since "/" has no corresponding digit.
     *
     * @param code a single digit "0"-"9", or "/" for obscured-above-overcast
     * @return the corresponding MiddleCloudType
     * @throws IllegalArgumentException if code is not a recognized value
     */
    public static MiddleCloudType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Middle cloud type code cannot be null");
        }
        if ("/".equals(code)) {
            return OBSCURED;
        }
        if (code.length() == 1 && Character.isDigit(code.charAt(0))) {
            return values()[code.charAt(0) - '0'];
        }
        throw new IllegalArgumentException("Invalid middle cloud type code: " + code);
    }

    /**
     * @return a human-readable description of this cloud type
     */
    public String getSummary() {
        return switch (this) {
            case NONE -> "No middle cloud";
            case ALTOSTRATUS_TRANSLUCIDUS -> "Altostratus, mostly semi-transparent";
            case ALTOSTRATUS_DENSE_OR_NIMBOSTRATUS -> "Altostratus (dense) or Nimbostratus";
            case ALTOCUMULUS_TRANSLUCIDUS_SINGLE_LEVEL -> "Altocumulus translucidus, single level";
            case ALTOCUMULUS_PATCHES_CHANGING -> "Altocumulus patches, continuously changing";
            case ALTOCUMULUS_INVADING -> "Altocumulus progressively invading the sky";
            case ALTOCUMULUS_CUMULOGENITUS -> "Altocumulus cumulogenitus or cumulonimbogenitus";
            case ALTOCUMULUS_MULTI_LEVEL_OR_WITH_ALTOSTRATUS_OR_OPACUS ->
                    "Altocumulus at multiple levels, with Altostratus/Nimbostratus, or opacus";
            case ALTOCUMULUS_CASTELLANUS_OR_FLOCCUS -> "Altocumulus castellanus or floccus";
            case ALTOCUMULUS_CHAOTIC_SKY -> "Altocumulus, chaotic sky";
            case OBSCURED -> "Obscured by overcast layer below";
        };
    }
}
