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
 * Extent of runway contamination (C_R), the second character of a runway
 * state group ({@code Rdd/ERCReeBB}).
 * <p>
 * Only the digits 1, 2, 5 and 9 are defined, plus "/" for not reported, so each
 * constant carries its transmitted code explicitly rather than relying on
 * ordinal position.
 *
 * @author bclasky1539
 */
public enum RunwayContaminationExtent {
    UP_TO_10_PERCENT("1", "10% or less of runway covered"),
    PERCENT_11_TO_25("2", "11% to 25% of runway covered"),
    PERCENT_26_TO_50("5", "26% to 50% of runway covered"),
    PERCENT_51_TO_100("9", "51% to 100% of runway covered"),
    NOT_REPORTED("/", "Extent not reported (e.g. runway clearance in progress)");

    private final String code;
    private final String summary;

    RunwayContaminationExtent(String code, String summary) {
        this.code = code;
        this.summary = summary;
    }

    /**
     * Parses the transmitted extent code into a RunwayContaminationExtent.
     *
     * @param code one of "1", "2", "5", "9", or "/" for not reported
     * @return the corresponding RunwayContaminationExtent
     * @throws IllegalArgumentException if code is not a recognized value
     */
    public static RunwayContaminationExtent fromCode(String code) {
        for (RunwayContaminationExtent extent : values()) {
            if (extent.code.equals(code)) {
                return extent;
            }
        }
        throw new IllegalArgumentException("Invalid runway contamination extent code: " + code);
    }

    /**
     * @return the transmitted code for this extent
     */
    public String getCode() {
        return code;
    }

    /**
     * @return a human-readable description of this extent
     */
    public String getSummary() {
        return summary;
    }
}
