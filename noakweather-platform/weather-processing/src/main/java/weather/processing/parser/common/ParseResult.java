/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025 bclasky1539
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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Result wrapper for parsing operations.
 * 
 * This is a "Result" or "Either" type - it can hold either:
 * - Success: parsed data
 * - Failure: error message and optional exception
 * 
 * Benefits:
 * - No need for null checks
 * - Explicit error handling
 * - Type-safe error propagation
 * - No expensive exception throwing for expected failures
 * 
 * @param <T> The type of data when successful
 * 
 * @author bclasky1539
 *
 */
public class ParseResult<T> {
    
    private final T data;
    private final boolean success;
    private final String errorMessage;
    private final Exception exception;
    
    private ParseResult(T data, boolean success, String errorMessage, Exception exception) {
        this.data = data;
        this.success = success;
        this.errorMessage = errorMessage;
        this.exception = exception;
    }
    
    /**
     * Create a successful result with data.
     * 
     * @param data The parsed data
     * @param <T> The type of the data
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
     * @param <T> The type that would have been returned on success
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
     * @param exception The exception that caused the failure
     * @param <T> The type that would have been returned on success
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
     * Transform the data if successful.
     * 
     * @param mapper Function to transform the data
     * @param <U> The new type
     * @return A new ParseResult with transformed data, or the same failure
     */
    public <U> ParseResult<U> map(Function<T, U> mapper) {
        if (success && data != null) {
            try {
                U mappedData = mapper.apply(data);
                return ParseResult.success(mappedData);
            } catch (Exception e) {
                return ParseResult.failure("Mapping failed: " + e.getMessage(), e);
            }
        }
        return ParseResult.failure(errorMessage, exception);
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
        if (success) {
            return "ParseResult{success=true, data=" + data + "}";
        } else {
            return "ParseResult{success=false, error='" + errorMessage + "'" +
                   (exception != null ? ", exception=" + exception.getClass().getSimpleName() : "") +
                   "}";
        }
    }
}
