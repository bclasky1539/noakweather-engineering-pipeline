/*
 * noakweather(TM) is a Java library for parsing weather data
 * Copyright (C) 2025 bclasky1539
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package noakweather.service;

/**
 * Custom exception for weather service operations.
 * 
 * This exception encapsulates all errors that can occur during weather data
 * operations, providing a clean abstraction layer between the service
 * implementation details and the calling code.
 * 
 * Think of this as a "standardized error report" - regardless of whether
 * the underlying issue is a network problem, invalid data, or service
 * unavailability, this exception provides a consistent way to handle errors.
 * 
 * @author bclasky1539
 */
public class WeatherServiceException extends Exception {
    private static final long serialVersionUID = 1L;
    
    /**
     * The type of error that occurred.
     */
    @SuppressWarnings("java:S1104")
    public enum ErrorType {
        INVALID_STATION_CODE("Invalid station code format"),
        STATION_NOT_FOUND("Station not found"),
        NETWORK_ERROR("Network communication error"),
        SERVICE_UNAVAILABLE("Weather service is unavailable"),
        DATA_PARSING_ERROR("Error parsing weather data"),
        CONFIGURATION_ERROR("Service configuration error"),
        UNKNOWN_ERROR("Unknown error occurred");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final ErrorType errorType;
    private final String stationCode;
    
    /**
     * Creates a new WeatherServiceException with the specified error type and message.
     * 
     * @param errorType The type of error that occurred
     * @param message Detailed error message
     */
    public WeatherServiceException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.stationCode = null;
    }
    
    /**
     * Creates a new WeatherServiceException with error type, message, and station code.
     * 
     * @param errorType The type of error that occurred
     * @param message Detailed error message
     * @param stationCode The station code related to the error
     */
    public WeatherServiceException(ErrorType errorType, String message, String stationCode) {
        super(message);
        this.errorType = errorType;
        this.stationCode = stationCode;
    }
    
    /**
     * Creates a new WeatherServiceException with error type, message, and underlying cause.
     * 
     * @param errorType The type of error that occurred
     * @param message Detailed error message
     * @param cause The underlying exception that caused this error
     */
    public WeatherServiceException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.stationCode = null;
    }
    
    /**
     * Creates a new WeatherServiceException with all details.
     * 
     * @param errorType The type of error that occurred
     * @param message Detailed error message
     * @param stationCode The station code related to the error
     * @param cause The underlying exception that caused this error
     */
    public WeatherServiceException(ErrorType errorType, String message, String stationCode, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.stationCode = stationCode;
    }
    
    /**
     * Gets the type of error that occurred.
     * 
     * @return The error type
     */
    public ErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * Gets the station code associated with this error, if any.
     * 
     * @return The station code, or null if not applicable
     */
    public String getStationCode() {
        return stationCode;
    }
    
    /**
     * Gets a user-friendly error message.
     * 
     * @return A formatted error message suitable for display to users
     */
    public String getUserFriendlyMessage() {
        StringBuilder message = new StringBuilder(errorType.getDescription());
        
        if (stationCode != null) {
            message.append(" for station ").append(stationCode);
        }
        
        if (getMessage() != null && !getMessage().isEmpty()) {
            message.append(": ").append(getMessage());
        }
        
        return message.toString();
    }
    
    @Override
    public String toString() {
        return String.format("WeatherServiceException{errorType=%s, stationCode='%s', message='%s'}", 
                           errorType, stationCode, getMessage());
    }
}
