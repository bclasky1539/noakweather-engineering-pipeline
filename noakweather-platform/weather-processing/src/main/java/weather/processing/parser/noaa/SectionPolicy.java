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

import java.util.function.Predicate;

/**
 * How {@link SectionParser} runs one section of a report (#153). The settings
 * capture how sections differ (METAR main body and TAF periods versus METAR remarks),
 * so each section chooses its behavior explicitly.
 *
 * @param scheduling   the order handlers are tried in
 * @param rejectedText what happens to a group a handler rejected
 * @param unrecognized what happens when no handler can parse the front of the text
 * @param boundary     true when the text starts with the next section (e.g. a TAF
 *                     change group); the section stops there
 * @author bclasky1539
 */
record SectionPolicy(Scheduling scheduling,
                     RejectedText rejectedText,
                     UnrecognizedToken unrecognized,
                     Predicate<String> boundary) {

    /**
     * Order in which handlers are tried.
     */
    enum Scheduling {
        /**
         * Try handlers in order; after any progress, start again from the first
         * handler. Used by the METAR main body and TAF forecast periods.
         */
        FIRST_MATCH_RESTART,
        /**
         * Run every handler once per pass, in order, repeating passes until one
         * makes no progress. Used by METAR remarks.
         */
        PASSES
    }

    /**
     * What happens to a group a handler rejected.
     */
    enum RejectedText {
        /**
         * Drop the rejected group and continue after it (main body today).
         */
        CONSUME,
        /**
         * Leave the rejected group in place so it surfaces as unparsed text
         * (remarks, where it reaches freeText).
         */
        KEEP
    }

    /**
     * What happens when no handler can parse the front of the text.
     */
    enum UnrecognizedToken {
        /**
         * Stop the section; the rest of the text is returned as remaining.
         */
        STOP,
        /**
         * Move the first whitespace-delimited token to the unparsed list and carry
         * on with what follows (remarks today).
         */
        SKIP_TO_UNPARSED
    }

    private static final Predicate<String> NO_BOUNDARY = text -> false;

    SectionPolicy {
        if (scheduling == null || rejectedText == null || unrecognized == null || boundary == null) {
            throw new IllegalArgumentException("Section policy settings cannot be null");
        }
    }

    /**
     * Policy for a section with no boundary.
     *
     * @param scheduling   the order handlers are tried in
     * @param rejectedText what happens to rejected groups
     * @param unrecognized what happens to text no handler can parse
     * @return the policy
     */
    static SectionPolicy of(Scheduling scheduling, RejectedText rejectedText, UnrecognizedToken unrecognized) {
        return new SectionPolicy(scheduling, rejectedText, unrecognized, NO_BOUNDARY);
    }

    /**
     * @param newBoundary true when the text starts with the next section
     * @return a copy of this policy that stops at the given boundary
     */
    SectionPolicy withBoundary(Predicate<String> newBoundary) {
        return new SectionPolicy(scheduling, rejectedText, unrecognized, newBoundary);
    }
}
