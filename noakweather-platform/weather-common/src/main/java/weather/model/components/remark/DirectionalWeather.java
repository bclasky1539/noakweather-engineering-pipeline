package weather.model.components.remark;

import weather.model.components.PresentWeather;

import java.util.List;

/**
 * Immutable value object representing a present-weather phenomenon reported
 * in remarks, optionally with associated compass direction(s), typically
 * restating a vicinity or nearby-occurring phenomenon.
 * <p>
 * Examples:
 * - "VCSH E SE" → DirectionalWeather(PresentWeather("VCSH"), ["E", "SE"])
 * - "VCSH" → DirectionalWeather(PresentWeather("VCSH"), null)
 *
 * @param presentWeather The present-weather phenomenon (reuses existing PresentWeather parsing)
 * @param directions One or more compass directions associated with the phenomenon, or null if none given
 *
 * @author bclasky1539
 *
 */
public record DirectionalWeather(
        PresentWeather presentWeather,
        List<String> directions
) {

    /**
     * Compact constructor with validation and defensive copying.
     */
    public DirectionalWeather {
        if (presentWeather == null) {
            throw new IllegalArgumentException("Present weather cannot be null");
        }
        directions = directions != null && !directions.isEmpty() ? List.copyOf(directions) : null;
    }

    /**
     * Check if direction information is present.
     *
     * @return true if one or more directions are recorded
     */
    public boolean hasDirections() {
        return directions != null;
    }

    /**
     * Get a human-readable summary.
     * Examples: "Showers in vicinity: E, SE", "Showers in vicinity"
     *
     * @return formatted summary string
     */
    public String getSummary() {
        String base = presentWeather.getDescription();
        return hasDirections() ? base + ": " + String.join(", ", directions) : base;
    }
}
