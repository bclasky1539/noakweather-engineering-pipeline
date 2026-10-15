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
package weather.exception;

import java.io.Serial;

/**
 * Thrown when an observation value is invalid for the real world, or when report text
 * for a field cannot be interpreted.
 * <p>
 * This is a <em>domain rejection</em> of the data, not a programming error. Callers such
 * as the METAR parser treat it as expected: the rejected group is left for freeText and
 * logged at DEBUG. Programming errors (null or missing arguments, inconsistently
 * assembled objects) continue to throw a plain {@link IllegalArgumentException} so that
 * they surface as bugs (#133).
 * <p>
 * Extends {@link IllegalArgumentException} so that existing catch blocks keep working
 * during the migration.
 *
 * @author bclasky1539
 */
public class InvalidObservationException extends IllegalArgumentException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String component;
    private final String rejectedValue;
    private final String reason;

    /**
     * Create an invalid-observation exception.
     *
     * @param component     the rejected field, as {@code ClassName.field} (e.g. {@code "PeakWind.hour"})
     * @param rejectedValue the rejected value as text, or null when no single value applies
     * @param reason        why the value is invalid (e.g. {@code "Hour must be between 0 and 23"})
     */
    public InvalidObservationException(String component, String rejectedValue, String reason) {
        this(component, rejectedValue, reason, null);
    }

    /**
     * Create an invalid-observation exception that keeps the underlying cause
     * (e.g. a {@link NumberFormatException} from interpreting report text).
     *
     * @param component     the rejected field, as {@code ClassName.field}
     * @param rejectedValue the rejected value as text, or null when no single value applies
     * @param reason        why the value is invalid
     * @param cause         the underlying exception, or null
     */
    public InvalidObservationException(String component, String rejectedValue, String reason, Throwable cause) {
        super(buildMessage(rejectedValue, reason), cause);
        this.component = component;
        this.rejectedValue = rejectedValue;
        this.reason = reason;
    }

    private static String buildMessage(String rejectedValue, String reason) {
        return rejectedValue == null ? reason : reason + ": " + rejectedValue;
    }

    public String getComponent() {
        return component;
    }

    public String getRejectedValue() {
        return rejectedValue;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "InvalidObservationException{component=%s, rejectedValue=%s, reason='%s'}".formatted(
                component, rejectedValue, reason);
    }
}
