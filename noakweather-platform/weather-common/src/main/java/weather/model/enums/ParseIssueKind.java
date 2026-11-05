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
 * Kind of problem found while parsing a report (#139, #166).
 * <p>
 * Shared by the parser's {@code ParseIssue} and the stored record of each issue on the
 * weather data, so both use one type.
 *
 * @author bclasky1539
 */
public enum ParseIssueKind {
    /**
     * A group the parser recognized but rejected as invalid data.
     */
    REJECTED,
    /**
     * A group a handler failed on with an unexpected exception (a parser bug).
     */
    FAILED,
    /**
     * A handler disabled for the rest of a section by its circuit breaker (a parser bug).
     */
    HANDLER_DISABLED;

    /**
     * @return true for parser bugs ({@link #FAILED}, {@link #HANDLER_DISABLED}); false for
     * bad data in the report ({@link #REJECTED})
     */
    public boolean isParserBug() {
        return this != REJECTED;
    }
}
