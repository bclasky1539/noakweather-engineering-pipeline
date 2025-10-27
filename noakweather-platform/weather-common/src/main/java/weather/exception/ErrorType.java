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
package weather.exception;

/**
 * Error types for categorizing weather service exceptions.
 * 
 * @author bclasky1539
 *
 */
public enum ErrorType {
    INVALID_STATION_CODE("Invalid station code format"),
    STATION_NOT_FOUND("Station not found"),
    SERVICE_UNAVAILABLE("Weather service unavailable"),
    NETWORK_ERROR("Network communication error"),
    INVALID_RESPONSE("Invalid response from weather service"),
    TIMEOUT("Request timeout"),
    CONFIGURATION_ERROR("Configuration error");
    
    private final String description;
    
    ErrorType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
