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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Result of running one section through {@link SectionParser} (#153).
 *
 * @param remaining       text left when the section stopped: at a boundary, or at
 *                        the first unrecognized token under
 *                        {@link SectionPolicy.UnrecognizedToken#STOP}; empty otherwise
 * @param unparsedTokens  tokens skipped under
 *                        {@link SectionPolicy.UnrecognizedToken#SKIP_TO_UNPARSED} and
 *                        groups moved aside under
 *                        {@link SectionPolicy.RejectedText#MOVE_TO_UNPARSED}, in report order
 * @param rejections      every rejected group, in the order rejected
 * @param trippedHandlers handlers disabled by the circuit breaker during this run
 * @author bclasky1539
 */
record SectionResult(String remaining,
                     List<String> unparsedTokens,
                     List<Rejection> rejections,
                     Set<String> trippedHandlers) {

    /**
     * One rejected group.
     *
     * @param handlerName the handler that rejected it
     * @param matchedText the rejected group, as it appears in the report
     * @param reason      why it was rejected
     */
    record Rejection(String handlerName, String matchedText, IllegalArgumentException reason) {
    }

    SectionResult {
        unparsedTokens = List.copyOf(unparsedTokens);
        rejections = List.copyOf(rejections);
        trippedHandlers = Set.copyOf(trippedHandlers);
    }

    /**
     * @return the skipped tokens joined with single spaces, or null when none were
     * skipped (matches how freeText is stored)
     */
    String unparsedText() {
        return unparsedTokens.isEmpty() ? null : String.join(" ", unparsedTokens);
    }

    /**
     * Everything the section did not parse, in report order: the unparsed tokens,
     * then the remaining text. Unparsed tokens always come before the point where
     * the section stopped, so the order matches the report.
     *
     * @return the text joined with single spaces, or null when everything was parsed
     * (matches how unparsedMainBody is stored)
     */
    String unparsedAndRemainingText() {
        List<String> parts = new ArrayList<>(unparsedTokens);
        if (!remaining.isBlank()) {
            parts.add(remaining.trim());
        }
        return parts.isEmpty() ? null : String.join(" ", parts);
    }
}
