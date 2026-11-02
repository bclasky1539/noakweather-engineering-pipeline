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
package weather.processing.parser.common;

import weather.exception.InvalidObservationException;

/**
 * One problem found while parsing a report (#139).
 * <p>
 * Issues travel with the {@link ParseResult}, like an inspection report attached to an
 * item leaving the line, so the pipeline can count and alert on them instead of relying
 * on logs alone.
 * <p>
 * {@link Kind#REJECTED} is bad data in the report: expected, and a metric.
 * {@link Kind#FAILED} and {@link Kind#HANDLER_DISABLED} are parser bugs: unexpected, and
 * worth an alert.
 *
 * @param kind          what kind of problem this is
 * @param section       the report section, e.g. "METAR main body", "METAR remarks"
 * @param handler       the group handler involved, e.g. "skyCondition"
 * @param text          the group as reported, e.g. "VV///"; null for a disabled handler
 * @param component     the rejected field as {@code ClassName.field}, when known
 * @param rejectedValue the rejected value as text, when known
 * @param reason        why the group was rejected, or the failure's message
 * @author bclasky1539
 */
public record ParseIssue(Kind kind,
                         String section,
                         String handler,
                         String text,
                         String component,
                         String rejectedValue,
                         String reason) {

    /**
     * Kind of parse issue.
     */
    public enum Kind {
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
        HANDLER_DISABLED
    }

    public ParseIssue {
        if (kind == null) {
            throw new IllegalArgumentException("Parse issue kind cannot be null");
        }
    }

    /**
     * A rejected group. Component, value and reason come from the exception when it is an
     * {@link InvalidObservationException}; otherwise only its message is kept as the reason.
     *
     * @param section   the report section
     * @param handler   the handler that rejected the group
     * @param text      the group as reported
     * @param rejection why it was rejected
     * @return the issue
     */
    public static ParseIssue rejected(String section, String handler, String text,
                                      IllegalArgumentException rejection) {
        if (rejection instanceof InvalidObservationException ioe) {
            return new ParseIssue(Kind.REJECTED, section, handler, text,
                    ioe.getComponent(), ioe.getRejectedValue(), ioe.getReason());
        }
        return new ParseIssue(Kind.REJECTED, section, handler, text, null, null, messageOf(rejection));
    }

    /**
     * A group a handler failed on.
     *
     * @param section the report section
     * @param handler the handler that failed
     * @param text    the group as reported
     * @param cause   the unexpected exception
     * @return the issue
     */
    public static ParseIssue failed(String section, String handler, String text, RuntimeException cause) {
        return new ParseIssue(Kind.FAILED, section, handler, text, null, null, messageOf(cause));
    }

    /**
     * A handler disabled by its circuit breaker.
     *
     * @param section the report section
     * @param handler the disabled handler
     * @return the issue
     */
    public static ParseIssue handlerDisabled(String section, String handler) {
        return new ParseIssue(Kind.HANDLER_DISABLED, section, handler, null, null, null,
                "Handler disabled for the rest of the section");
    }

    /**
     * @return true for parser bugs ({@link Kind#FAILED}, {@link Kind#HANDLER_DISABLED})
     */
    public boolean isParserBug() {
        return kind != Kind.REJECTED;
    }

    private static String messageOf(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
