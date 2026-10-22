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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the METAR remark handlers over one report's remarks text, with a circuit
 * breaker per handler (#135).
 * <p>
 * Three levels of protection, like household wiring:
 * <p>
 * Fuse (per remark group): a handler catches
 *   {@code InvalidObservationException} itself and leaves the rejected group
 *   for freeText. That is not a failure and does not reach this class.
 * Circuit breaker (per handler, this class): any other exception escaping a
 *   handler is a bug. It is logged once, the handler is skipped for the rest
 *   of the report, and the other handlers keep running.
 * Main breaker: {@code NoaaMetarParser.parse()}, which fails the whole report.
 * <p>
 * Create one chain per report: breakers start closed and never reset within a chain.
 *
 * @author bclasky1539
 */
final class RemarkHandlerChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemarkHandlerChain.class);

    private final List<RemarkHandler> handlers;
    private final Set<String> tripped = new HashSet<>();

    /**
     * @param handlers handlers in the order they run on each pass; names must be unique
     * @throws IllegalArgumentException if the list is null or two handlers share a name
     */
    RemarkHandlerChain(List<RemarkHandler> handlers) {
        if (handlers == null) {
            throw new IllegalArgumentException("Remark handlers cannot be null");
        }
        Set<String> names = new HashSet<>();
        for (RemarkHandler handler : handlers) {
            if (!names.add(handler.name())) {
                throw new IllegalArgumentException("Duplicate remark handler name: " + handler.name());
            }
        }
        this.handlers = List.copyOf(handlers);
    }

    /**
     * Run every handler repeatedly against {@code remaining} until a full pass makes
     * no progress. A handler that only matches after another has consumed the token
     * in front of it gets its chance on a later pass.
     *
     * @param remaining the remarks text to process
     * @param builder   the remarks builder to populate
     * @return the text no handler could consume
     */
    String runPasses(String remaining, NoaaMetarRemarks.Builder builder) {
        String current = remaining;
        String previous;

        do {
            previous = current;
            for (RemarkHandler handler : handlers) {
                current = runHandler(handler, current, builder);
            }
        } while (!Objects.equals(current, previous));

        return current;
    }

    /**
     * @param handlerName the handler's name
     * @return true if the handler failed earlier in this report and is now skipped
     */
    boolean isTripped(String handlerName) {
        return tripped.contains(handlerName);
    }

    private String runHandler(RemarkHandler handler, String remaining, NoaaMetarRemarks.Builder builder) {
        if (tripped.contains(handler.name())) {
            return remaining;
        }

        try {
            String result = handler.step().apply(remaining, builder);
            if (result == null) {
                throw new IllegalStateException("Remark handler returned null");
            }
            return result;
        } catch (RuntimeException e) {
            tripped.add(handler.name());
            LOGGER.warn("Remark handler '{}' failed on '{}' and is disabled for the rest of this report",
                    handler.name(), remaining, e);
            return remaining;
        }
    }
}
