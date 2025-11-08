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
import weather.model.WeatherData;
import weather.model.TestWeatherData;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WeatherValidator interface, specifically the default methods.
 * 
 * @author bclasky1539
 */
class WeatherValidatorTest {
    
    /**
     * Test implementation that returns a successful validation
     */
    private static class SuccessValidator implements WeatherValidator<WeatherData> {
        @Override
        public ValidationResult validate(WeatherData data) {
            return ValidationResult.success();
        }
    }
    
    /**
     * Test implementation that returns validation with warnings
     */
    private static class WarningValidator implements WeatherValidator<WeatherData> {
        @Override
        public ValidationResult validate(WeatherData data) {
            return ValidationResult.withWarnings(Arrays.asList("Test warning"));
        }
    }
    
    /**
     * Test implementation that returns validation failure
     */
    private static class FailureValidator implements WeatherValidator<WeatherData> {
        @Override
        public ValidationResult validate(WeatherData data) {
            return ValidationResult.failure(Arrays.asList("Test error"));
        }
    }
    
    /**
     * Test implementation that returns failure with warnings
     */
    private static class FailureWithWarningsValidator implements WeatherValidator<WeatherData> {
        @Override
        public ValidationResult validate(WeatherData data) {
            return ValidationResult.failure(
                Arrays.asList("Test error"),
                Arrays.asList("Test warning")
            );
        }
    }
    
    @Test
    @DisplayName("Should return true when validation succeeds")
    void testIsValid_Success() {
        WeatherValidator<WeatherData> validator = new SuccessValidator();
        TestWeatherData data = new TestWeatherData();
        
        boolean isValid = validator.isValid(data);
        
        assertTrue(isValid, "isValid should return true for successful validation");
    }
    
    @Test
    @DisplayName("Should return true when validation has only warnings")
    void testIsValid_WithWarnings() {
        WeatherValidator<WeatherData> validator = new WarningValidator();
        TestWeatherData data = new TestWeatherData();
        
        boolean isValid = validator.isValid(data);
        
        assertTrue(isValid, "isValid should return true when only warnings exist");
    }
    
    @Test
    @DisplayName("Should return false when validation fails")
    void testIsValid_Failure() {
        WeatherValidator<WeatherData> validator = new FailureValidator();
        TestWeatherData data = new TestWeatherData();
        
        boolean isValid = validator.isValid(data);
        
        assertFalse(isValid, "isValid should return false when validation fails");
    }
    
    @Test
    @DisplayName("Should return false when validation fails with warnings")
    void testIsValid_FailureWithWarnings() {
        WeatherValidator<WeatherData> validator = new FailureWithWarningsValidator();
        TestWeatherData data = new TestWeatherData();
        
        boolean isValid = validator.isValid(data);
        
        assertFalse(isValid, "isValid should return false when validation fails even with warnings");
    }
    
    @Test
    @DisplayName("Should call validate method when isValid is called")
    void testIsValidCallsValidate() {
        // Track if validate was called
        final boolean[] validateCalled = {false};
        
        WeatherValidator<WeatherData> validator = new WeatherValidator<WeatherData>() {
            @Override
            public ValidationResult validate(WeatherData data) {
                validateCalled[0] = true;
                return ValidationResult.success();
            }
        };
        
        TestWeatherData data = new TestWeatherData();
        validator.isValid(data);
        
        assertTrue(validateCalled[0], "isValid should call validate() method");
    }
    
    @Test
    @DisplayName("Should work with different WeatherData subclasses")
    void testWithDifferentWeatherDataTypes() {
        WeatherValidator<TestWeatherData> validator = new WeatherValidator<TestWeatherData>() {
            @Override
            public ValidationResult validate(TestWeatherData data) {
                // Simple validation: check if observation time is set
                if (data.getObservationTime() == null) {
                    return ValidationResult.failure(Arrays.asList("Observation time required"));
                }
                return ValidationResult.success();
            }
        };
        
        // Test with observation time
        TestWeatherData dataWithTime = new TestWeatherData();
        dataWithTime.setObservationTime(Instant.now());
        assertTrue(validator.isValid(dataWithTime));
        
        // Test without observation time
        TestWeatherData dataWithoutTime = new TestWeatherData();
        dataWithoutTime.setObservationTime(null);
        assertFalse(validator.isValid(dataWithoutTime));
    }
}
