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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The shared group parsing engine (#153): runs one section of a report through its
 * ordered {@link GroupHandler}s under a {@link SectionPolicy}.
 * <p>
 * Like a production line, the engine is the same for every section and source;
 * a section only brings its own handlers (stations) and policy.
 * <p>
 * Protection follows the remark chain's household-wiring model (#135):
 * <p>
 * Fuse (per group): a step reports bad data as {@link GroupOutcome.Rejected}. The
 * engine logs it once via {@link RejectedGroupLog}, records it in the result, and
 * consumes or keeps the text as the policy says.
 * Circuit breaker (per handler): any exception escaping a step is a bug. It is
 * logged at WARN, the handler is skipped for the rest of this run, and the other
 * handlers keep running.
 * Main breaker: the parser's {@code parse()}, which fails the whole report.
 * <p>
 * Each {@link #parse} call is independent: breakers start closed on every call, so
 * one instance can be built once per parser and reused for every report.
 *
 * @param <B> the target handlers store parsed values in
 * @author bclasky1539
 */
final class SectionParser<B> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SectionParser.class);

    private final String sectionName;
    private final List<GroupHandler<B>> handlers;
    private final SectionPolicy policy;

    /**
     * @param sectionName name used in logs, e.g. "METAR remarks"
     * @param handlers    handlers in the order they are tried; names must be unique
     * @param policy      how the section is run
     * @throws IllegalArgumentException if any argument is null or blank, a handler is
     *                                  null, or two handlers share a name
     */
    SectionParser(String sectionName, List<GroupHandler<B>> handlers, SectionPolicy policy) {
        if (sectionName == null || sectionName.isBlank()) {
            throw new IllegalArgumentException("Section name cannot be null or blank");
        }
        if (handlers == null) {
            throw new IllegalArgumentException("Section handlers cannot be null");
        }
        if (policy == null) {
            throw new IllegalArgumentException("Section policy cannot be null");
        }
        Set<String> names = new HashSet<>();
        for (GroupHandler<B> handler : handlers) {
            if (handler == null) {
                throw new IllegalArgumentException("Section handlers cannot contain null");
            }
            if (!names.add(handler.name())) {
                throw new IllegalArgumentException("Duplicate group handler name: " + handler.name());
            }
        }
        this.sectionName = sectionName;
        this.handlers = List.copyOf(handlers);
        this.policy = policy;
    }

    /**
     * Run the section's handlers over {@code text}.
     *
     * @param text   the section text
     * @param target where handlers store parsed values
     * @return what was left, skipped and rejected
     * @throws IllegalArgumentException if text is null
     */
    SectionResult parse(String text, B target) {
        if (text == null) {
            throw new IllegalArgumentException("Section text cannot be null");
        }
        return new Run(target).execute(text);
    }

    /**
     * State for one {@link #parse} call.
     */
    private final class Run {

        private final B target;
        private final Set<String> tripped = new LinkedHashSet<>();
        private final Set<String> keptRejections = new HashSet<>();
        private final List<SectionResult.Rejection> rejections = new ArrayList<>();
        private final List<String> unparsed = new ArrayList<>();

        private Run(B target) {
            this.target = target;
        }

        private SectionResult execute(String text) {
            String current = text;
            while (true) {
                current = runUntilStuck(current);
                if (atEnd(current) || policy.unrecognized() == SectionPolicy.UnrecognizedToken.STOP) {
                    break;
                }
                current = skipToken(current);
            }
            return new SectionResult(current, unparsed, rejections, tripped);
        }

        private String runUntilStuck(String text) {
            return policy.scheduling() == SectionPolicy.Scheduling.FIRST_MATCH_RESTART
                    ? runFirstMatchRestart(text)
                    : runPasses(text);
        }

        /**
         * Try handlers in order; after any progress, start again from the first.
         */
        private String runFirstMatchRestart(String text) {
            String current = text;
            boolean progressed = true;
            while (progressed && !atEnd(current)) {
                progressed = false;
                for (GroupHandler<B> handler : handlers) {
                    String next = attempt(handler, current);
                    if (!next.equals(current)) {
                        current = next;
                        progressed = true;
                        break;
                    }
                }
            }
            return current;
        }

        /**
         * Run every handler once per pass until a pass makes no progress.
         */
        private String runPasses(String text) {
            String current = text;
            String previous;
            do {
                previous = current;
                for (GroupHandler<B> handler : handlers) {
                    if (atEnd(current)) {
                        break;
                    }
                    current = attempt(handler, current);
                }
            } while (!current.equals(previous));
            return current;
        }

        private boolean atEnd(String text) {
            return text.isBlank() || policy.boundary().test(text);
        }

        /**
         * Move the first whitespace-delimited token to the unparsed list.
         */
        private String skipToken(String text) {
            String trimmed = text.trim();
            int spaceIdx = trimmed.indexOf(' ');
            if (spaceIdx < 0) {
                unparsed.add(trimmed);
                return "";
            }
            unparsed.add(trimmed.substring(0, spaceIdx));
            return trimmed.substring(spaceIdx + 1).trim();
        }

        /**
         * Run one handler, behind its circuit breaker.
         *
         * @return the text after the handler ran; unchanged when it made no progress
         */
        private String attempt(GroupHandler<B> handler, String text) {
            if (tripped.contains(handler.name())) {
                return text;
            }

            GroupOutcome outcome;
            try {
                outcome = handler.step().apply(text, target);
                if (outcome == null) {
                    throw new IllegalStateException("Group handler returned null");
                }
            } catch (RuntimeException e) {
                tripped.add(handler.name());
                LOGGER.warn("Group handler '{}' in {} failed on '{}' and is disabled for the rest of this section",
                        handler.name(), sectionName, text, e);
                return text;
            }

            if (outcome instanceof GroupOutcome.Parsed parsed) {
                return parsed.remaining();
            }
            if (outcome instanceof GroupOutcome.Rejected rejected) {
                return reject(handler, rejected);
            }
            return text;
        }

        /**
         * Log and record a rejection, then consume or keep its text per policy.
         */
        private String reject(GroupHandler<B> handler, GroupOutcome.Rejected rejected) {
            if (policy.rejectedText() == SectionPolicy.RejectedText.CONSUME) {
                recordRejection(handler, rejected);
                return rejected.afterGroup();
            }

            // Under KEEP the group stays at the front and is tried again on later
            // passes until it is skipped; record it once per stay at the front
            String key = handler.name() + '\u0000' + rejected.matchedText() + '\u0000' + unparsed.size();
            if (keptRejections.add(key)) {
                recordRejection(handler, rejected);
            }
            return rejected.atGroup();
        }

        private void recordRejection(GroupHandler<B> handler, GroupOutcome.Rejected rejected) {
            RejectedGroupLog.log(handler.name(), rejected.matchedText(), rejected.reason());
            rejections.add(new SectionResult.Rejection(handler.name(), rejected.matchedText(), rejected.reason()));
        }
    }
}
