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
package weather.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for creating ValidationResult instances.
 * 
 * Provides a fluent API for accumulating errors and warnings,
 * then building the final ValidationResult.
 * 
 * Example usage:
 * <pre>
 * ValidationResult result = new ValidationResultBuilder()
 *     .addError("Station ID is required")
 *     .addWarning("Observation time is old")
 *     .build();
 * </pre>
 * 
 * @author bclasky1539
 *
 */
public final class ValidationResultBuilder {
    
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    
    /**
     * Add a single error message.
     * 
     * @param error the error message
     * @return this builder for chaining
     */
    public ValidationResultBuilder addError(String error) {
        errors.add(error);
        return this;
    }
    
    /**
     * Add a single warning message.
     * 
     * @param warning the warning message
     * @return this builder for chaining
     */
    public ValidationResultBuilder addWarning(String warning) {
        warnings.add(warning);
        return this;
    }
    
    /**
     * Add multiple error messages at once.
     * 
     * @param errors list of error messages
     * @return this builder for chaining
     */
    public ValidationResultBuilder addErrors(List<String> errors) {
        this.errors.addAll(errors);
        return this;
    }
    
    /**
     * Add multiple warning messages at once.
     * 
     * @param warnings list of warning messages
     * @return this builder for chaining
     */
    public ValidationResultBuilder addWarnings(List<String> warnings) {
        this.warnings.addAll(warnings);
        return this;
    }
    
    /**
     * Build the final ValidationResult.
     * 
     * Logic:
     * - If there are errors: result is invalid
     * - If no errors but warnings: result is valid with warnings
     * - If no errors and no warnings: result is success
     * 
     * @return the constructed ValidationResult
     */
    public ValidationResult build() {
        if (errors.isEmpty()) {
            return warnings.isEmpty() 
                ? ValidationResult.success()
                : ValidationResult.withWarnings(warnings);
        }
        return ValidationResult.failure(errors, warnings);
    }
}
