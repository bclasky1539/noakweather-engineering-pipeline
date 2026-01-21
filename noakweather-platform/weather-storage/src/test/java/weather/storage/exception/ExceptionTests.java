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
package weather.storage.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for custom exception classes.
 * <p>
 * These tests verify that exceptions are properly constructed and
 * maintain the expected message and cause chain.
 *
 * @author bclasky1539
 *
 */
class ExceptionTests {

    // ========== RepositoryException Tests ==========

    @Test
    void shouldCreateRepositoryExceptionWithMessage() {
        // Given
        String message = "Repository operation failed";

        // When
        RepositoryException exception = new RepositoryException(message);

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateRepositoryExceptionWithMessageAndCause() {
        // Given
        String message = "Repository operation failed";
        Throwable cause = new IllegalStateException("Database connection lost");

        // When
        RepositoryException exception = new RepositoryException(message, cause);

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause().getMessage()).isEqualTo("Database connection lost");
    }

    @Test
    void shouldMaintainCauseChainForRepositoryException() {
        // Given
        Throwable rootCause = new RuntimeException("Root cause");
        Throwable intermediateCause = new IllegalStateException("Intermediate", rootCause);

        // When
        RepositoryException exception = new RepositoryException("Failed", intermediateCause);

        // Then
        assertThat(exception.getCause()).isEqualTo(intermediateCause);
        assertThat(exception.getCause().getCause()).isEqualTo(rootCause);
    }

    // ========== WeatherDataMappingException Tests ==========

    @Test
    void shouldCreateWeatherDataMappingExceptionWithMessage() {
        // Given
        String message = "Failed to serialize weather data";

        // When
        WeatherDataMappingException exception = new WeatherDataMappingException(message);

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateWeatherDataMappingExceptionWithMessageAndCause() {
        // Given
        String message = "Failed to deserialize weather data";
        Throwable cause = new com.fasterxml.jackson.core.JsonProcessingException("Invalid JSON") {};

        // When
        WeatherDataMappingException exception = new WeatherDataMappingException(message, cause);

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldMaintainCauseChainForWeatherDataMappingException() {
        // Given
        Throwable rootCause = new RuntimeException("JSON parser error");
        Throwable intermediateCause = new IllegalArgumentException("Invalid format", rootCause);

        // When
        WeatherDataMappingException exception = new WeatherDataMappingException(
                "Mapping failed", intermediateCause);

        // Then
        assertThat(exception.getCause()).isEqualTo(intermediateCause);
        assertThat(exception.getCause().getCause()).isEqualTo(rootCause);
    }

    // ========== Exception Type Verification ==========

    @Test
    void repositoryExceptionShouldBeRuntimeException() {
        RepositoryException exception = new RepositoryException("test");
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void weatherDataMappingExceptionShouldBeRuntimeException() {
        WeatherDataMappingException exception = new WeatherDataMappingException("test");
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
