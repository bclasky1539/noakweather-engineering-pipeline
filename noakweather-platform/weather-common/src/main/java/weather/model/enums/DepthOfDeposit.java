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

/**
 * Depth of runway deposit (e_R e_R), the third and fourth characters of a
 * runway state group ({@code Rdd/ERCReeBB}).
 * <p>
 * The quoted depth is the mean of a number of readings or, if operationally
 * significant, the greatest depth measured.
 * <ul>
 *   <li>00 = less than 1 mm</li>
 *   <li>01-90 = depth in millimetres ({@link #MILLIMETRES})</li>
 *   <li>91 = not used (rejected)</li>
 *   <li>92-98 = 10 cm to 40 cm or more, in 5 cm steps</li>
 *   <li>99 = runway not operational, depth not reported</li>
 *   <li>// = depth not operationally significant or not measurable</li>
 * </ul>
 * Codes 01-90 share one constant; the actual value is carried separately in
 * millimetres via {@link #toMillimetres(String)}.
 *
 * @author bclasky1539
 */
public enum DepthOfDeposit {
    LESS_THAN_1_MM("00", 0, "Depth less than 1 mm"),
    MILLIMETRES("01-90", null, "Depth in millimetres"),
    CM_10("92", 100, "Depth 10 cm"),
    CM_15("93", 150, "Depth 15 cm"),
    CM_20("94", 200, "Depth 20 cm"),
    CM_25("95", 250, "Depth 25 cm"),
    CM_30("96", 300, "Depth 30 cm"),
    CM_35("97", 350, "Depth 35 cm"),
    CM_40_OR_MORE("98", 400, "Depth 40 cm or more"),
    RUNWAY_NOT_OPERATIONAL("99",
            null, "Runway not operational due to snow, slush, ice, drifts or clearance; depth not reported"),
    NOT_SIGNIFICANT("//", null, "Depth not operationally significant or not measurable");

    private static final int MAX_MILLIMETRE_CODE = 90;
    private static final String UNUSED_CODE = "91";

    private final String code;
    private final Integer fixedMillimetres;
    private final String summary;

    DepthOfDeposit(String code, Integer fixedMillimetres, String summary) {
        this.code = code;
        this.fixedMillimetres = fixedMillimetres;
        this.summary = summary;
    }

    /**
     * Parses the transmitted two-character depth code into a DepthOfDeposit.
     *
     * @param code "00"-"90", "92"-"99", or "//"
     * @return the corresponding DepthOfDeposit
     * @throws IllegalArgumentException if code is "91" (not used) or not a recognized value
     */
    public static DepthOfDeposit fromCode(String code) {
        if (NOT_SIGNIFICANT.code.equals(code)) {
            return NOT_SIGNIFICANT;
        }
        if (code == null || !code.matches("\\d{2}") || UNUSED_CODE.equals(code)) {
            throw new IllegalArgumentException("Invalid runway deposit depth code: " + code);
        }
        int value = Integer.parseInt(code);
        if (value >= 1 && value <= MAX_MILLIMETRE_CODE) {
            return MILLIMETRES;
        }
        for (DepthOfDeposit depth : values()) {
            if (depth.code.equals(code)) {
                return depth;
            }
        }
        throw new IllegalArgumentException("Invalid runway deposit depth code: " + code);
    }

    /**
     * Converts a transmitted depth code of this constant into millimetres.
     *
     * @param code the transmitted code this constant was decoded from
     * @return depth in millimetres, or null if this constant carries no depth
     * ({@link #RUNWAY_NOT_OPERATIONAL}, {@link #NOT_SIGNIFICANT})
     */
    public Integer toMillimetres(String code) {
        if (this == MILLIMETRES) {
            return Integer.parseInt(code);
        }
        return fixedMillimetres;
    }

    /**
     * @return true if this constant carries a depth value in millimetres
     */
    public boolean hasDepthValue() {
        return this == MILLIMETRES || fixedMillimetres != null;
    }

    /**
     * @return true if the depth is a lower bound (code 98: 40 cm or more)
     */
    public boolean isLowerBound() {
        return this == CM_40_OR_MORE;
    }

    /**
     * @return the fixed depth in millimetres for this constant, or null if the
     * depth varies ({@link #MILLIMETRES}) or is not given
     */
    public Integer getFixedMillimetres() {
        return fixedMillimetres;
    }

    /**
     * @return the transmitted code for this depth ("01-90" for {@link #MILLIMETRES})
     */
    public String getCode() {
        return code;
    }

    /**
     * @return a human-readable description of this depth
     */
    public String getSummary() {
        return summary;
    }
}
