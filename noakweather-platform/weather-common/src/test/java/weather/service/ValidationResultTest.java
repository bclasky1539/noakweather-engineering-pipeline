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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for ValidationResult and ValidationResultBuilder
 * 
 * @author bclasky1539
 *
 */
class ValidationResultTest {
    
    @Test
    @DisplayName("Should create successful validation result")
    void testSuccessResult() {
        ValidationResult result = ValidationResult.success();
        
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        assertFalse(result.hasWarnings());
        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
    }
    
    @Test
    @DisplayName("Should create validation result with warnings")
    void testWithWarnings() {
        List<String> warnings = Arrays.asList("Warning 1", "Warning 2");
        ValidationResult result = ValidationResult.withWarnings(warnings);
        
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        assertTrue(result.hasWarnings());
        assertEquals(2, result.getWarnings().size());
    }
    
    @Test
    @DisplayName("Should create failed validation result")
    void testFailureResult() {
        List<String> errors = Arrays.asList("Error 1", "Error 2");
        ValidationResult result = ValidationResult.failure(errors);
        
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertFalse(result.hasWarnings());
        assertEquals(2, result.getErrors().size());
    }
    
    @Test
    @DisplayName("Should create failed validation result with warnings")
    void testFailureWithWarnings() {
        List<String> errors = Arrays.asList("Error 1");
        List<String> warnings = Arrays.asList("Warning 1");
        ValidationResult result = ValidationResult.failure(errors, warnings);
        
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.hasWarnings());
        assertEquals(1, result.getErrors().size());
        assertEquals(1, result.getWarnings().size());
    }
    
    @Test
    @DisplayName("Should build validation result with builder")
    void testValidationResultBuilder() {
        ValidationResultBuilder builder = new ValidationResultBuilder();
        
        ValidationResult result = builder
            .addError("Missing station ID")
            .addWarning("Old observation time")
            .build();
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(1, result.getWarnings().size());
    }
    
    @Test
    @DisplayName("Should build successful result with only warnings")
    void testBuilderWithOnlyWarnings() {
        ValidationResultBuilder builder = new ValidationResultBuilder();
        
        ValidationResult result = builder
            .addWarning("Data is old")
            .build();
        
        assertTrue(result.isValid());
        assertTrue(result.hasWarnings());
        assertFalse(result.hasErrors());
    }
    
    @Test
    @DisplayName("Should build successful result with no issues")
    void testBuilderNoIssues() {
        ValidationResultBuilder builder = new ValidationResultBuilder();
        ValidationResult result = builder.build();
        
        assertTrue(result.isValid());
        assertFalse(result.hasWarnings());
        assertFalse(result.hasErrors());
    }
    
    @Test
    @DisplayName("Should add multiple errors at once")
    void testBuilderAddMultipleErrors() {
        List<String> errors = Arrays.asList("Error 1", "Error 2", "Error 3");
        
        ValidationResultBuilder builder = new ValidationResultBuilder();
        ValidationResult result = builder.addErrors(errors).build();
        
        assertFalse(result.isValid());
        assertEquals(3, result.getErrors().size());
    }
    
    @Test
    @DisplayName("Should add multiple warnings at once")
    void testBuilderAddMultipleWarnings() {
        List<String> warnings = Arrays.asList("Warning 1", "Warning 2");
        
        ValidationResultBuilder builder = new ValidationResultBuilder();
        ValidationResult result = builder.addWarnings(warnings).build();
        
        assertTrue(result.isValid());
        assertEquals(2, result.getWarnings().size());
    }
    
    @Test
    @DisplayName("Should have meaningful toString")
    void testToString() {
        ValidationResult success = ValidationResult.success();
        assertTrue(success.toString().contains("valid=true"));
        
        ValidationResult failure = ValidationResult.failure(
            Arrays.asList("Error")
        );
        assertTrue(failure.toString().contains("valid=false"));
        assertTrue(failure.toString().contains("errors=1"));
    }
    
    @Test
    @DisplayName("Validation result errors should be immutable")
    void testImmutableErrors() {
        List<String> errors = Arrays.asList("Error 1");
        ValidationResult result = ValidationResult.failure(errors);
        
        List<String> errorList = result.getErrors();
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class, 
            () -> errorList.add("Error 2")
        );
        assertNotNull(exception);
    }
    
    @Test
    @DisplayName("Validation result warnings should be immutable")
    void testImmutableWarnings() {
        List<String> warnings = Arrays.asList("Warning 1");
        ValidationResult result = ValidationResult.withWarnings(warnings);
        
        List<String> warningList = result.getWarnings();
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class, 
            () -> warningList.add("Warning 2")
        );
        assertNotNull(exception);
    }
}
