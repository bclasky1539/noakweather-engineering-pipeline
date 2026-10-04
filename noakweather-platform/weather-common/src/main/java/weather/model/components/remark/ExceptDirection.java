/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025 bclasky1539
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
package weather.model.components.remark;

import java.util.List;

/**
 * "Except [direction]" remark ({@code XCPT}/{@code XCP}/{@code EXCP}/
 * {@code EXC <direction>}), indicating a previously reported condition
 * does not apply in the given direction(s).
 * <p>
 * Not to be confused with {@code EXPCD}/{@code EXPCTD}/{@code EXPTD}/
 * {@code EXP} ("expected"), which has a different meaning and is not
 * represented by this type.
 * <p>
 * Which of the four "except" abbreviations appeared in the raw text is
 * not preserved — all four are equivalent in meaning.
 */
public record ExceptDirection(List<DirectionSegment> directionSegments) {

    public static ExceptDirection of(List<DirectionSegment> directionSegments) {
        return new ExceptDirection(directionSegments);
    }

    /**
     * @return true if at least one direction segment is present
     */
    public boolean hasDirections() {
        return directionSegments != null && !directionSegments.isEmpty();
    }

    /**
     * @return a human-readable summary, e.g. "Except N" or
     * "Except N-NE AND SW"
     */
    public String getSummary() {
        if (!hasDirections()) {
            return "Except (no direction)";
        }
        String joined = directionSegments.stream()
                .map(DirectionSegment::getSummary)
                .reduce((a, b) -> a + " AND " + b)
                .orElse("");
        return "Except " + joined;
    }
}
