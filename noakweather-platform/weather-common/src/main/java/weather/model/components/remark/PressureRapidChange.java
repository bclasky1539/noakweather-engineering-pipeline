package weather.model.components.remark;

/**
 * Pressure rising or falling rapidly (PRESRR or PRESFR remark).
 * Binary indicator with no associated magnitude — distinct from
 * {@link PressureTendency}, which carries a coded rate and change amount.
 */
public enum PressureRapidChange {
    RISING,
    FALLING;

    /**
     * Parses the single-letter code from the PRES remark.
     *
     * @param code "R" (rising) or "F" (falling)
     * @return the corresponding PressureRapidChange
     * @throws IllegalArgumentException if code is not "R" or "F"
     */
    public static PressureRapidChange fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Pressure rapid change code cannot be null");
        }

        return switch (code) {
            case "R" -> RISING;
            case "F" -> FALLING;
            default -> throw new IllegalArgumentException("Invalid pressure rapid change code: " + code);
        };
    }

    /**
     * Alias for {@link #fromCode(String)}, matching the {@code of(...)}
     * factory-method convention used elsewhere (e.g. {@code PressureTendency.of},
     * {@code Pressure.ofInchesHg}).
     *
     * @param code "R" (rising) or "F" (falling)
     * @return the corresponding PressureRapidChange
     */
    public static PressureRapidChange of(String code) {
        return fromCode(code);
    }

    /**
     * @return a human-readable summary, e.g. "Rising rapidly"
     */
    public String getSummary() {
        return switch (this) {
            case RISING -> "Rising rapidly";
            case FALLING -> "Falling rapidly";
        };
    }
}
