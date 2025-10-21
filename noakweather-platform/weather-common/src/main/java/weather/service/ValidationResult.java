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

import java.util.Collections;
import java.util.List;

/**
 * Result of a weather data validation operation.
 * 
 * Immutable value object that contains:
 * - Valid/invalid status
 * - List of errors (if any)
 * - List of warnings (if any)
 * 
 * Use ValidationResultBuilder to construct instances.
 * 
 * @author bclasky1539
 *
 */
public final class ValidationResult {
    
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    
    private ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = Collections.unmodifiableList(errors);
        this.warnings = Collections.unmodifiableList(warnings);
    }
    
    /**
     * Create a successful validation result.
     * 
     * @return validation result with no errors or warnings
     */
    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
    }
    
    /**
     * Create a validation result with warnings (still valid).
     * 
     * @param warnings list of warning messages
     * @return validation result that is valid but has warnings
     */
    public static ValidationResult withWarnings(List<String> warnings) {
        return new ValidationResult(true, Collections.emptyList(), warnings);
    }
    
    /**
     * Create a failed validation result with errors.
     * 
     * @param errors list of error messages
     * @return validation result that failed
     */
    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors, Collections.emptyList());
    }
    
    /**
     * Create a failed validation result with errors and warnings.
     * 
     * @param errors list of error messages
     * @param warnings list of warning messages
     * @return validation result that failed
     */
    public static ValidationResult failure(List<String> errors, List<String> warnings) {
        return new ValidationResult(false, errors, warnings);
    }
    
    /**
     * Check if validation passed.
     * 
     * @return true if valid (even with warnings)
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Get list of errors.
     * 
     * @return immutable list of error messages (empty if valid)
     */
    public List<String> getErrors() {
        return errors;
    }
    
    /**
     * Get list of warnings.
     * 
     * @return immutable list of warning messages (may be empty)
     */
    public List<String> getWarnings() {
        return warnings;
    }
    
    /**
     * Check if there are any warnings.
     * 
     * @return true if warnings exist
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    /**
     * Check if there are any errors.
     * 
     * @return true if errors exist
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    @Override
    public String toString() {
        if (valid && !hasWarnings()) {
            return "ValidationResult{valid=true}";
        }
        return "ValidationResult{valid=%s, errors=%d, warnings=%d}".formatted(
            valid, errors.size(), warnings.size());
    }
}
