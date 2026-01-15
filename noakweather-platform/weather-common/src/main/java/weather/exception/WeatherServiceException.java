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
package weather.exception;

/**
 * Exception for weather service operations.
 * Adapted from legacy noakweather.service.WeatherServiceException
 * 
 * @author bclasky1539
 *
 */
public class WeatherServiceException extends Exception {
    
    private final ErrorType errorType;
    private final String stationCode;
    
    /**
     * Creates a WeatherServiceException with error type and message
     * @param errorType error type
     * @param message description of what went wrong
     */
    public WeatherServiceException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.stationCode = null;
    }
    
    /**
     * Creates a WeatherServiceException with error type, message, and station code
     * @param errorType error type
     * @param message description of what went wrong
     * @param stationCode station code
     */
    public WeatherServiceException(ErrorType errorType, String message, String stationCode) {
        super(message + " [Station: " + stationCode + "]");
        this.errorType = errorType;
        this.stationCode = stationCode;
    }
    
    /**
     * Creates a WeatherServiceException with error type, message, station code, and cause
     * @param errorType error type
     * @param message description of what went wrong
     * @param stationCode station code
     * @param cause the underlying exception
     */
    public WeatherServiceException(ErrorType errorType, String message, String stationCode, Throwable cause) {
        super(message + " [Station: " + stationCode + "]", cause);
        this.errorType = errorType;
        this.stationCode = stationCode;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
    
    public String getStationCode() {
        return stationCode;
    }
    
    @Override
    public String toString() {
        return String.format("WeatherServiceException{type=%s, station=%s, message=%s}", 
                errorType, stationCode, getMessage());
    }
}
