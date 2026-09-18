package weather.model.components.remark;

/**
 * Icing remark (ICG). Reports whether icing has been observed in clouds
 * (IC) and/or in precipitation (IP), with an optional free-form qualifier
 * describing additional context (e.g. "PAST HR" — in the past hour).
 * <p>
 * The qualifier is stored as-is rather than parsed into a structured
 * shape, since only one real-world format has been confirmed to date;
 * revisit if additional formats are observed.
 */
public record Icing(
        boolean inClouds,
        boolean inPrecipitation,
        String qualifier
) {

    /**
     * @return a human-readable summary, e.g. "Icing in clouds (PAST HR)"
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder("Icing");

        if (inClouds) {
            sb.append(" in clouds");
        }
        if (inPrecipitation) {
            sb.append(inClouds ? " and precipitation" : " in precipitation");
        }
        if (qualifier != null && !qualifier.isBlank()) {
            sb.append(" (").append(qualifier).append(")");
        }

        return sb.toString();
    }

    /**
     * Factory method matching the {@code of(...)} convention used elsewhere
     * in the remarks model (e.g. {@code PressureTendency.of}, {@code CloudType.of}).
     *
     * @param inClouds        true if icing observed in clouds
     * @param inPrecipitation true if icing observed in precipitation
     * @param qualifier        free-form qualifier text, or null if none
     * @return the constructed Icing
     */
    public static Icing of(boolean inClouds, boolean inPrecipitation, String qualifier) {
        return new Icing(inClouds, inPrecipitation, qualifier);
    }

    /**
     * @return true if a qualifier is present
     */
    public boolean hasQualifier() {
        return qualifier != null && !qualifier.isBlank();
    }
}