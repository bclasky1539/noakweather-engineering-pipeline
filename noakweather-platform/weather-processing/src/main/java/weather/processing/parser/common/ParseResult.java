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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Result wrapper for parsing operations.
 * <p>
 * This is a "Result" or "Either" type - it can hold either:
 * - Success: parsed data
 * - Failure: error message and optional exception
 * <p>
 * Benefits:
 * - No need for null checks
 * - Explicit error handling
 * - Type-safe error propagation
 * - No expensive exception throwing for expected failures
 * <p>
 * Either kind of result can also carry {@link ParseIssue}s (#139): groups the parser
 * rejected or failed on, and handlers its circuit breaker disabled. A successful result
 * with issues is still a success; the issues say what was not parsed and why.
 *
 * @param <T> The type of data when successful
 * @author bclasky1539
 *
 */
public class ParseResult<T> {

    private final T data;
    private final boolean success;
    private final String errorMessage;
    private final Exception exception;
    private final List<ParseIssue> issues;

    private ParseResult(T data, boolean success, String errorMessage, Exception exception) {
        this(data, success, errorMessage, exception, List.of());
    }

    private ParseResult(T data, boolean success, String errorMessage, Exception exception,
                        List<ParseIssue> issues) {
        this.data = data;
        this.success = success;
        this.errorMessage = errorMessage;
        this.exception = exception;
        this.issues = issues;
    }

    /**
     * Create a successful result with data.
     *
     * @param data The parsed data
     * @param <T>  The type of the data
     * @return A successful ParseResult
     */
    public static <T> ParseResult<T> success(T data) {
        if (data == null) {
            throw new IllegalArgumentException("Success data cannot be null");
        }
        return new ParseResult<>(data, true, null, null);
    }

    /**
     * Create a failed result with error message.
     *
     * @param errorMessage Description of what went wrong
     * @param <T>          The type that would have been returned on success
     * @return A failed ParseResult
     */
    public static <T> ParseResult<T> failure(String errorMessage) {
        String message = errorMessage;
        if (message == null || message.trim().isEmpty()) {
            message = "Unknown error";
        }
        return new ParseResult<>(null, false, message, null);
    }

    /**
     * Create a failed result with error message and exception.
     *
     * @param errorMessage Description of what went wrong
     * @param exception    The exception that caused the failure
     * @param <T>          The type that would have been returned on success
     * @return A failed ParseResult
     */
    public static <T> ParseResult<T> failure(String errorMessage, Exception exception) {
        String message = errorMessage;
        if (message == null || message.trim().isEmpty()) {
            message = exception != null ? exception.getMessage() : "Unknown error";
        }
        return new ParseResult<>(null, false, message, exception);
    }

    /**
     * Check if the parsing was successful.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Check if the parsing failed.
     *
     * @return true if failed, false otherwise
     */
    public boolean isFailure() {
        return !success;
    }

    /**
     * Get the parsed data if successful.
     *
     * @return Optional containing data, or empty if failed
     */
    public Optional<T> getData() {
        return Optional.ofNullable(data);
    }

    /**
     * Get the error message if failed.
     *
     * @return The error message, or null if successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Get the exception if one was provided.
     *
     * @return Optional containing exception, or empty if none
     */
    public Optional<Exception> getException() {
        return Optional.ofNullable(exception);
    }

    /**
     * Get the issues found while parsing (#139).
     *
     * @return the issues, in the order found; empty when there were none (never null)
     */
    public List<ParseIssue> getIssues() {
        return issues;
    }

    /**
     * Check if any issues were found while parsing.
     *
     * @return true if there is at least one issue
     */
    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    /**
     * Copy of this result carrying the given issues, replacing any it had.
     *
     * @param newIssues the issues found while parsing
     * @return a new ParseResult with the same outcome and the given issues
     * @throws IllegalArgumentException if newIssues is null or contains null
     */
    public ParseResult<T> withIssues(List<ParseIssue> newIssues) {
        // stream check: List.of(...).contains(null) throws instead of returning false
        if (newIssues == null || newIssues.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Parse issues cannot be null or contain null");
        }
        return new ParseResult<>(data, success, errorMessage, exception, List.copyOf(newIssues));
    }

    /**
     * Execute an action if the result is successful.
     *
     * @param consumer The action to execute with the data
     * @return this ParseResult for chaining
     */
    public ParseResult<T> ifSuccess(Consumer<T> consumer) {
        if (success && data != null) {
            consumer.accept(data);
        }
        return this;
    }

    /**
     * Execute an action if the result is a failure.
     *
     * @param consumer The action to execute with the error message
     * @return this ParseResult for chaining
     */
    public ParseResult<T> ifFailure(Consumer<String> consumer) {
        if (!success) {
            consumer.accept(errorMessage);
        }
        return this;
    }

    /**
     * Transform the data if successful. The issues are carried over either way.
     *
     * @param mapper Function to transform the data
     * @param <U>    The new type
     * @return A new ParseResult with transformed data, or the same failure
     */
    public <U> ParseResult<U> map(Function<T, U> mapper) {
        if (success && data != null) {
            try {
                U mappedData = mapper.apply(data);
                return ParseResult.success(mappedData).withIssues(issues);
            } catch (Exception e) {
                return ParseResult.<U>failure("Mapping failed: " + e.getMessage(), e).withIssues(issues);
            }
        }
        return ParseResult.<U>failure(errorMessage, exception).withIssues(issues);
    }

    /**
     * Get the data or throw the exception if failed.
     *
     * @return The data
     * @throws ParserException if parsing failed
     */
    public T orElseThrow() throws ParserException {
        if (success) {
            return data;
        }
        throw new ParserException(errorMessage, null, "unknown");
    }

    /**
     * Get the data or return a default value if failed.
     *
     * @param defaultValue The value to return if failed
     * @return The data or default value
     */
    public T orElse(T defaultValue) {
        return success ? data : defaultValue;
    }

    @Override
    public String toString() {
        String issueCount = issues.isEmpty() ? "" : ", issues=" + issues.size();
        if (success) {
            return "ParseResult{success=true, data=" + data + issueCount + "}";
        } else {
            return "ParseResult{success=false, error='" + errorMessage + "'" +
                    (exception != null ? ", exception=" + exception.getClass().getSimpleName() : "") +
                    issueCount + "}";
        }
    }
}
