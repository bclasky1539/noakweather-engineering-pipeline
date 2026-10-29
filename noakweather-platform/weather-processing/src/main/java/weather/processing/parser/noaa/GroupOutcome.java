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
 * What a {@link GroupHandler} step did with the text in front of it (#153).
 * <p>
 * Steps report a rejection instead of handling it: the section's
 * {@link SectionPolicy} decides whether rejected text is consumed or kept, and
 * {@link SectionParser} logs and records every rejection in one place.
 *
 * @author bclasky1539
 */
sealed interface GroupOutcome {

    /**
     * The step parsed its group (or several, for a repeating group).
     *
     * @param remaining the text after the parsed group(s)
     */
    record Parsed(String remaining) implements GroupOutcome {
        public Parsed {
            if (remaining == null) {
                throw new IllegalArgumentException("Parsed remaining text cannot be null");
            }
        }
    }

    /**
     * The step matched its group but rejected it as invalid data. A repeating
     * step may have parsed earlier groups first; {@code atGroup} starts at the
     * rejected group, so those are consumed either way.
     *
     * @param matchedText the rejected group, as it appears in the report
     * @param atGroup     the text starting at the rejected group (used when the
     *                    section keeps rejected text)
     * @param afterGroup  the text after the rejected group (used when the section
     *                    consumes it)
     * @param reason      why the group was rejected
     */
    record Rejected(String matchedText, String atGroup, String afterGroup,
                    IllegalArgumentException reason) implements GroupOutcome {
        public Rejected {
            if (matchedText == null || atGroup == null || afterGroup == null || reason == null) {
                throw new IllegalArgumentException("Rejected outcome fields cannot be null");
            }
        }
    }

    /**
     * The group is not at the front of the text.
     */
    record NoMatch() implements GroupOutcome {
    }

    /**
     * The shared no-match outcome.
     */
    NoMatch NO_MATCH = new NoMatch();

    /**
     * @return the shared no-match outcome
     */
    static GroupOutcome noMatch() {
        return NO_MATCH;
    }
}
