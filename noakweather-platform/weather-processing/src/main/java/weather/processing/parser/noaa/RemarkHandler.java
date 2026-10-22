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

import weather.model.components.remark.NoaaMetarRemarks;

/**
 * One METAR remark handler: a name used in logs and the step that consumes its
 * group from the front of the remaining remarks text.
 * <p>
 * A step returns the text with its group removed when it parsed the group, and
 * the text unchanged when the group is absent or was rejected as invalid data.
 *
 * @param name human-readable handler name, unique within a {@link RemarkHandlerChain}
 * @param step the parsing step
 * @author bclasky1539
 */
record RemarkHandler(String name, Step step) {

    /**
     * A remark parsing step. Implementations populate {@code builder} and return
     * the remaining text.
     */
    @FunctionalInterface
    interface Step {
        String apply(String remaining, NoaaMetarRemarks.Builder builder);
    }

    RemarkHandler {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Remark handler name cannot be null or blank");
        }
        if (step == null) {
            throw new IllegalArgumentException("Remark handler step cannot be null");
        }
    }
}
