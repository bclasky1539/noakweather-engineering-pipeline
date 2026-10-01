package weather.model.enums;

/**
 * Predominant low cloud type (C_L), per WMO Cloud Atlas coding instructions
 * section 2.8.3.1, as used in the METAR/synoptic "8/C_L C_M C_H" remark group.
 * <p>
 * Digits are assigned by the observer's first-applicable-condition priority
 * order documented by WMO; that priority does not correspond to numeric
 * order. This enum only decodes the transmitted digit back to a label — the
 * original observational judgment is not reconstructed.
 *
 * @author bclasky1539
 */
public enum LowCloudType {
    NONE,
    CUMULUS_HUMILIS_OR_FRACTUS_FAIR_WEATHER,
    CUMULUS_MEDIOCRIS_OR_CONGESTUS,
    CUMULONIMBUS_CALVUS,
    STRATOCUMULUS_CUMULOGENITUS,
    STRATOCUMULUS_NON_CUMULOGENITUS,
    STRATUS_NEBULOSUS_OR_FRACTUS_FAIR_WEATHER,
    STRATUS_FRACTUS_OR_CUMULUS_FRACTUS_BAD_WEATHER,
    CUMULUS_AND_STRATOCUMULUS_DIFFERENT_LEVELS,
    CUMULONIMBUS_CAPILLATUS,
    OBSCURED;

    /**
     * Parses the transmitted digit (or "/" for obscured) into a LowCloudType.
     * <p>
     * Relies on enum declaration order matching the WMO digit order
     * (NONE=0 through CUMULONIMBUS_CAPILLATUS=9); OBSCURED is handled
     * separately since "/" has no corresponding digit.
     *
     * @param code a single digit "0"-"9", or "/" for obscured-above-overcast
     * @return the corresponding LowCloudType
     * @throws IllegalArgumentException if code is not a recognized value
     */
    public static LowCloudType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Low cloud type code cannot be null");
        }
        if ("/".equals(code)) {
            return OBSCURED;
        }
        if (code.length() == 1 && Character.isDigit(code.charAt(0))) {
            return values()[code.charAt(0) - '0'];
        }
        throw new IllegalArgumentException("Invalid low cloud type code: " + code);
    }

    /**
     * @return a human-readable description of this cloud type
     */
    public String getSummary() {
        return switch (this) {
            case NONE -> "No low cloud";
            case CUMULUS_HUMILIS_OR_FRACTUS_FAIR_WEATHER -> "Cumulus humilis or fractus (fair weather)";
            case CUMULUS_MEDIOCRIS_OR_CONGESTUS -> "Cumulus mediocris or congestus";
            case CUMULONIMBUS_CALVUS -> "Cumulonimbus, not yet fibrous or striated";
            case STRATOCUMULUS_CUMULOGENITUS -> "Stratocumulus formed by spreading of Cumulus";
            case STRATOCUMULUS_NON_CUMULOGENITUS -> "Stratocumulus, not from spreading Cumulus";
            case STRATUS_NEBULOSUS_OR_FRACTUS_FAIR_WEATHER -> "Stratus nebulosus or fractus (fair weather)";
            case STRATUS_FRACTUS_OR_CUMULUS_FRACTUS_BAD_WEATHER -> "Stratus or Cumulus fractus (bad weather)";
            case CUMULUS_AND_STRATOCUMULUS_DIFFERENT_LEVELS -> "Cumulus and Stratocumulus at different levels";
            case CUMULONIMBUS_CAPILLATUS -> "Cumulonimbus capitulates";
            case OBSCURED -> "Obscured by overcast layer below";
        };
    }
}
