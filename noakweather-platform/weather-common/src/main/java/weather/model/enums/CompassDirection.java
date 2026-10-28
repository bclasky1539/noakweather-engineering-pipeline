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

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The 16 points of the compass rose, in clockwise order from north.
 * <p>
 * The single source of truth for compass direction codes (#141). Two kinds of
 * caller use it:
 * <p>
 * Degree conversion ({@code Wind}, {@code PeakWind}) uses all 16 points via
 * {@link #fromDegrees(int)}.
 * <p>
 * Validators ({@code VariableVisibility}, {@code DirectionSegment},
 * {@code CloudType}) accept only the 8 principal points (N, NE, E, SE, S, SW,
 * W, NW) via {@link #isPrincipal(String)}. A finer point such as ESE is
 * rejected on purpose so it surfaces in freeText for review rather than being
 * accepted or guessed at.
 * <p>
 * Codes are matched exactly; callers normalize (trim, upper-case) before lookup.
 * <p>
 * Declaration order is significant: {@link #fromDegrees(int)} indexes
 * {@code values()} in 22.5-degree steps.
 *
 * @author bclasky1539
 */
public enum CompassDirection {

    N("North", true),
    NNE("North-northeast", false),
    NE("Northeast", true),
    ENE("East-northeast", false),
    E("East", true),
    ESE("East-southeast", false),
    SE("Southeast", true),
    SSE("South-southeast", false),
    S("South", true),
    SSW("South-southwest", false),
    SW("Southwest", true),
    WSW("West-southwest", false),
    W("West", true),
    WNW("West-northwest", false),
    NW("Northwest", true),
    NNW("North-northwest", false);

    private static final int MIN_DEGREES = 0;
    private static final int MAX_DEGREES = 360;
    private static final double DEGREES_PER_POINT = 22.5;

    private static final CompassDirection[] POINTS = values();

    private static final Map<String, CompassDirection> BY_CODE = Arrays.stream(POINTS)
            .collect(Collectors.toUnmodifiableMap(Enum::name, Function.identity()));

    private static final String PRINCIPAL_CODES_DISPLAY = Arrays.stream(POINTS)
            .filter(CompassDirection::isPrincipal)
            .map(Enum::name)
            .collect(Collectors.joining(", "));

    private final String description;
    private final boolean principal;

    CompassDirection(String description, boolean principal) {
        this.description = description;
        this.principal = principal;
    }

    /**
     * @return human-readable name, e.g. "Northeast" or "East-southeast"
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return true for the 8 principal points (N, NE, E, SE, S, SW, W, NW),
     * false for the 8 in-between points (NNE, ENE, ...)
     */
    public boolean isPrincipal() {
        return principal;
    }

    /**
     * Convert a direction in degrees to the nearest of the 16 compass points.
     * <p>
     * Each point covers 22.5 degrees centred on its bearing (e.g. NNE is 12-33).
     * 360 is north.
     *
     * @param degrees direction in degrees, 0-360
     * @return the nearest compass point
     * @throws IllegalArgumentException if degrees is outside 0-360; callers
     *                                  validate the range first, so this is a bug
     */
    public static CompassDirection fromDegrees(int degrees) {
        if (degrees < MIN_DEGREES || degrees > MAX_DEGREES) {
            throw new IllegalArgumentException(
                    "Degrees must be between " + MIN_DEGREES + " and " + MAX_DEGREES + ": " + degrees);
        }
        int index = (int) Math.round(degrees / DEGREES_PER_POINT) % POINTS.length;
        return POINTS[index];
    }

    /**
     * Look up any of the 16 points by its code.
     *
     * @param code the code, already trimmed and upper-cased (e.g. "NE", "ESE")
     * @return the point, or empty for null or unknown codes
     */
    public static Optional<CompassDirection> find(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(BY_CODE.get(code));
    }

    /**
     * Check whether a code is one of the 8 principal points.
     *
     * @param code the code, already trimmed and upper-cased (e.g. "NE")
     * @return true for N, NE, E, SE, S, SW, W, NW; false for null, blank,
     * in-between points (e.g. "NNE") and unknown codes
     */
    public static boolean isPrincipal(String code) {
        return find(code).map(CompassDirection::isPrincipal).orElse(false);
    }

    /**
     * @return the 8 principal codes in clockwise order, for rejection messages:
     * "N, NE, E, SE, S, SW, W, NW"
     */
    public static String principalCodesDisplay() {
        return PRINCIPAL_CODES_DISPLAY;
    }
}
