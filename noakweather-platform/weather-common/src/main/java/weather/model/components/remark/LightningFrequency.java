package weather.model.components.remark;

/**
 * Lightning frequency (OCNL, FRQ, CONS/CONTUS remark qualifier).
 * CONS (US/FAA convention) and CONTUS (Canadian MANOBS convention) both
 * mean "continuous" and are normalized to the same value.
 */
public enum LightningFrequency {
    OCCASIONAL,
    FREQUENT,
    CONTINUOUS;

    /**
     * Parses the frequency code from the LTG remark.
     *
     * @param code "OCNL", "FRQ", "CONS", or "CONTUS"
     * @return the corresponding LightningFrequency
     * @throws IllegalArgumentException if code is not recognized
     */
    public static LightningFrequency fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Lightning frequency code cannot be null");
        }

        return switch (code) {
            case "OCNL" -> OCCASIONAL;
            case "FRQ" -> FREQUENT;
            case "CONS", "CONTUS" -> CONTINUOUS;
            default -> throw new IllegalArgumentException("Invalid lightning frequency code: " + code);
        };
    }

    /**
     * @return a human-readable description, e.g. "Occasional"
     */
    public String getDescription() {
        return switch (this) {
            case OCCASIONAL -> "Occasional";
            case FREQUENT -> "Frequent";
            case CONTINUOUS -> "Continuous";
        };
    }
}
