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
 * One kind of report group (wind, sky condition, peak wind, FM group, ...): a name
 * used in logs and the step that tries the group at the front of a section's text.
 * <p>
 * Part of the shared group parsing engine (#153): every section of every parser
 * runs its handlers through {@link SectionParser}.
 *
 * @param name human-readable group name, unique within a section, e.g. "sky condition"
 * @param step the parsing step
 * @param <B>  the target the step stores parsed values in
 * @author bclasky1539
 */
record GroupHandler<B>(String name, Step<B> step) {

    /**
     * A group parsing step. It tries its group at the front of {@code text}, stores
     * what it parsed in {@code target}, and says what happened.
     *
     * @param <B> the target type
     */
    @FunctionalInterface
    interface Step<B> {
        GroupOutcome apply(String text, B target);
    }

    /**
     * A step in the older text-returning form: it returns the text with its group
     * removed, or the text unchanged when the group is absent or was rejected (the
     * step logs its own rejections).
     *
     * @param <B> the target type
     */
    @FunctionalInterface
    interface TextStep<B> {
        String apply(String text, B target);
    }

    /**
     * Wrap a text-returning step (the METAR remark handlers' form) as a handler:
     * unchanged text is {@link GroupOutcome#noMatch()}, anything else
     * {@link GroupOutcome.Parsed}. Lets existing steps run on the engine unchanged
     * (#153 phase 3).
     *
     * @param name group name used in logs
     * @param step the text-returning step
     * @param <B>  the target type
     * @return the handler
     * @throws IllegalArgumentException if step is null (or name is null or blank)
     */
    static <B> GroupHandler<B> textStep(String name, TextStep<B> step) {
        if (step == null) {
            throw new IllegalArgumentException("Group handler step cannot be null");
        }
        return new GroupHandler<>(name, (text, target) -> {
            String remaining = step.apply(text, target);
            if (remaining == null) {
                throw new IllegalStateException("Group handler step returned null");
            }
            return remaining.equals(text) ? GroupOutcome.noMatch() : new GroupOutcome.Parsed(remaining);
        });
    }

    GroupHandler {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group handler name cannot be null or blank");
        }
        if (step == null) {
            throw new IllegalArgumentException("Group handler step cannot be null");
        }
    }
}
