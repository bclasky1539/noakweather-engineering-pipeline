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

import weather.exception.InvalidObservationException;

import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds {@link GroupHandler}s for simple, pattern-based groups (#153): a regex
 * and an action that stores the matched values, so such groups stay declarative.
 * <p>
 * The action takes the target first, so a parser's existing handler method can
 * be passed as a method reference:
 * {@code PatternGroupHandler.single("wind", WIND_PATTERN, NoaaMetarParser::handleWind)}.
 * <p>
 * Matching and text handling follow the pattern registry exactly, so moving a
 * registry entry here does not change parsed output:
 * <p>
 * - the pattern is applied with {@code find()}; an empty match is ignored;
 * - the match is removed with {@code replaceFirst("")}, the rest is trimmed, and a
 * trailing space is added when text remains (the registry's legacy rule, which
 * some patterns rely on to see the end of a group);
 * - a repeating handler applies its pattern again until it stops matching.
 * <p>
 * An action rejects a group by throwing {@link InvalidObservationException}, or
 * {@link NumberFormatException} for a malformed number; that becomes
 * {@link GroupOutcome.Rejected}. Any other runtime exception is a bug (#133) and
 * becomes {@link GroupOutcome.Failed}: the engine logs it at ERROR and moves on,
 * and the handler stays enabled for later groups.
 *
 * @author bclasky1539
 */
final class PatternGroupHandler {

    private PatternGroupHandler() {
    }

    /**
     * A group that appears at most once in a row (wind, visibility, ...).
     *
     * @param name    group name used in logs
     * @param pattern the group's pattern
     * @param action  stores the matched values in the target
     * @param <B>     the target type
     * @return the handler
     */
    static <B> GroupHandler<B> single(String name, Pattern pattern, BiConsumer<B, Matcher> action) {
        return create(name, pattern, action, false);
    }

    /**
     * A group that can repeat in a row (sky layers, present weather, RVR, ...).
     *
     * @param name    group name used in logs
     * @param pattern the group's pattern
     * @param action  stores the matched values in the target, once per match
     * @param <B>     the target type
     * @return the handler
     */
    static <B> GroupHandler<B> repeating(String name, Pattern pattern, BiConsumer<B, Matcher> action) {
        return create(name, pattern, action, true);
    }

    /**
     * A group that is recognized and consumed but not stored, so it does not stop
     * the section as unrecognized text would (e.g. a METAR-only group in a TAF, or
     * a registry entry that never had a handler).
     *
     * @param name    group name used in logs
     * @param pattern the group's pattern
     * @param <B>     the target type
     * @return the handler
     */
    static <B> GroupHandler<B> consumeOnly(String name, Pattern pattern) {
        return create(name, pattern, (target, matcher) -> {
            // Recognized but not stored
        }, false);
    }

    private static <B> GroupHandler<B> create(String name, Pattern pattern,
                                              BiConsumer<B, Matcher> action, boolean repeat) {
        if (pattern == null) {
            throw new IllegalArgumentException("Group pattern cannot be null");
        }
        if (action == null) {
            throw new IllegalArgumentException("Group action cannot be null");
        }
        return new GroupHandler<>(name, (text, target) -> apply(pattern, action, repeat, text, target));
    }

    private static <B> GroupOutcome apply(Pattern pattern, BiConsumer<B, Matcher> action,
                                          boolean repeat, String text, B target) {
        String current = text;
        boolean parsedAny = false;
        Matcher matcher = pattern.matcher(current);

        while (matcher.find() && !matcher.group(0).isEmpty()) {
            String matchedText = matcher.group(0).trim();
            try {
                action.accept(target, matcher);
            } catch (InvalidObservationException | NumberFormatException e) {
                return new GroupOutcome.Rejected(matchedText, current, removeMatch(matcher), e);
            } catch (RuntimeException e) {
                return new GroupOutcome.Failed(matchedText, current, removeMatch(matcher), e);
            }

            current = removeMatch(matcher);
            parsedAny = true;

            if (!repeat) {
                break;
            }
            matcher = pattern.matcher(current);
        }

        return parsedAny ? new GroupOutcome.Parsed(current) : GroupOutcome.noMatch();
    }

    /**
     * Remove the match and keep the registry's trailing-space rule.
     */
    private static String removeMatch(Matcher matcher) {
        String remaining = matcher.replaceFirst("").trim();
        return remaining.isEmpty() ? remaining : remaining + " ";
    }
}
