package weather.model.components.remark;

import java.util.List;
import java.util.Set;

/**
 * A single directional segment within a thunderstorm/cloud location
 * reading — either a single compass point (e.g. "N") or a multi-point
 * arc/range (e.g. "E-S-SW"). Multiple segments can be chained together
 * with AND to describe a location reported at more than one place
 * (e.g. "N-E AND SE-S").
 * <p>
 * Points are validated against the 8 standard compass abbreviations
 * (N, NE, E, SE, S, SW, W, NW). A finer-grained point (e.g. ESE) is
 * not currently recognized and would surface via freeText for review
 * rather than being silently accepted or guessed at.
 */
public record DirectionSegment(List<String> points) {

    private static final Set<String> VALID_POINTS =
            Set.of("N", "NE", "E", "SE", "S", "SW", "W", "NW");

    public DirectionSegment {
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("DirectionSegment requires at least one point");
        }

        List<String> normalized = points.stream()
                .map(p -> p == null ? null : p.trim().toUpperCase())
                .toList();

        for (String point : normalized) {
            if (point == null || point.isEmpty()) {
                throw new IllegalArgumentException("DirectionSegment points cannot be null or blank");
            }
            if (!VALID_POINTS.contains(point)) {
                throw new IllegalArgumentException("Invalid compass point: " + point);
            }
        }

        points = List.copyOf(normalized);
    }

    /**
     * @return true if this segment spans more than one point (an arc/range)
     */
    public boolean isRange() {
        return points.size() > 1;
    }

    /**
     * @return the raw notation, e.g. "N" or "E-S-SW"
     */
    public String getSummary() {
        return String.join("-", points);
    }
}
