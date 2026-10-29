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
package weather.processing.parser.noaa;

/**
 * One kind of report group (wind, sky condition, peak wind, FM group, ...): a name
 * used in logs and the step that tries the group at the front of a section's text.
 * <p>
 * Part of the shared group parsing engine (#153): every section of every parser
 * runs its handlers through {@link SectionParser}.
 *
 * @param name human-readable group name, unique within a section, e.g. "sky condition"
 * @param step the parsing step
 * @param <B>  the target the step stores parsed values in
 * @author bclasky1539
 */
record GroupHandler<B>(String name, Step<B> step) {

    /**
     * A group parsing step. It tries its group at the front of {@code text}, stores
     * what it parsed in {@code target}, and says what happened.
     *
     * @param <B> the target type
     */
    @FunctionalInterface
    interface Step<B> {
        GroupOutcome apply(String text, B target);
    }

    GroupHandler {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group handler name cannot be null or blank");
        }
        if (step == null) {
            throw new IllegalArgumentException("Group handler step cannot be null");
        }
    }
}
