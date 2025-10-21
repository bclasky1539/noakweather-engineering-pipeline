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

import weather.model.WeatherDataSource;

/**
 * Exception thrown when weather data parsing fails.
 * 
 * This exception preserves the raw data and source information
 * to enable debugging and retry logic.
 * 
 * @author bclasky1539
 *
 */
public class WeatherParseException extends Exception {
    
    private final String rawData;
    private final WeatherDataSource source;
    
    /**
     * Create a parse exception with message.
     * 
     * @param message description of what went wrong
     * @param rawData the raw data that failed to parse
     * @param source the weather data source
     */
    public WeatherParseException(String message, String rawData, WeatherDataSource source) {
        super(message);
        this.rawData = rawData;
        this.source = source;
    }
    
    /**
     * Create a parse exception with message and cause.
     * 
     * @param message description of what went wrong
     * @param cause the underlying exception
     * @param rawData the raw data that failed to parse
     * @param source the weather data source
     */
    public WeatherParseException(String message, Throwable cause, String rawData, WeatherDataSource source) {
        super(message, cause);
        this.rawData = rawData;
        this.source = source;
    }
    
    /**
     * Get the raw data that failed to parse.
     * 
     * @return raw data string
     */
    public String getRawData() {
        return rawData;
    }
    
    /**
     * Get the source of the data.
     * 
     * @return weather data source
     */
    public WeatherDataSource getSource() {
        return source;
    }
    
    @Override
    public String toString() {
        return "WeatherParseException{source=%s, message='%s', rawDataLength=%d}".formatted(
            source,
            getMessage(),
            rawData != null ? rawData.length() : 0
        );
    }
}
