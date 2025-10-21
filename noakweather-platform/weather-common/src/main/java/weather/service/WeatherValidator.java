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

import weather.model.WeatherData;

/**
 * Interface for validating weather data quality and completeness.
 * 
 * Design Pattern: Validator Pattern
 * 
 * Ensures weather data meets quality standards before being processed
 * or stored. Different sources may have different validation rules.
 * 
 * Analogy: Like a quality control inspector on an assembly line who
 * checks each product against a checklist before it moves to the next stage.
 * 
 * @param <T>
 *
 * @author bclasky1539
 *
 */
public interface WeatherValidator<T extends WeatherData> {
    
    /**
     * Validate a weather data object.
     * 
     * @param data the weather data to validate
     * @return validation result with any errors/warnings
     */
    ValidationResult validate(T data);
    
    /**
     * Check if the data meets minimum requirements for processing.
     * This is a quick check - use validate() for detailed results.
     * 
     * @param data the weather data to check
     * @return true if data is valid enough to process
     */
    default boolean isValid(T data) {
        return validate(data).isValid();
    }
}
