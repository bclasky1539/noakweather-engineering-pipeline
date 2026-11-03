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

import weather.processing.parser.common.ParseIssue;

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
 * @param failures        every group a handler failed on, in the order failed (#139)
 * @param trippedHandlers handlers disabled by the circuit breaker during this run
 * @author bclasky1539
 */
record SectionResult(String remaining,
                     List<String> unparsedTokens,
                     List<Rejection> rejections,
                     List<Failure> failures,
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

    /**
     * One group a handler failed on with an unexpected exception (a parser bug).
     *
     * @param handlerName the handler that failed
     * @param matchedText the group, as it appears in the report
     * @param cause       the unexpected exception
     */
    record Failure(String handlerName, String matchedText, RuntimeException cause) {
    }

    SectionResult {
        unparsedTokens = List.copyOf(unparsedTokens);
        rejections = List.copyOf(rejections);
        failures = List.copyOf(failures);
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

    /**
     * This run's problems as parse issues: rejections, then failures, each in
     * the order they happened, then disabled handlers by name (the set has no order).
     *
     * @param section the section this result came from, e.g. "METAR main body"
     * @return the issues; empty when the run had none
     */
    List<ParseIssue> toParseIssues(String section) {
        List<ParseIssue> issues = new ArrayList<>();
        rejections.forEach(rejection -> issues.add(ParseIssue.rejected(
                section, rejection.handlerName(), rejection.matchedText(), rejection.reason())));
        failures.forEach(failure -> issues.add(ParseIssue.failed(
                section, failure.handlerName(), failure.matchedText(), failure.cause())));
        trippedHandlers.stream().sorted()
                .forEach(handler -> issues.add(ParseIssue.handlerDisabled(section, handler)));
        return issues;
    }
}
