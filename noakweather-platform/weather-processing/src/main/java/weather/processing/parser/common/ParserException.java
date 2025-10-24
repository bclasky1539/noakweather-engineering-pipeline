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
package weather.processing.parser.common;

/**
 * Custom exception for parsing errors.
 * 
 * Provides additional context specific to weather data parsing,
 * including the raw data that failed to parse and the parser type.
 * 
 * @author bclasky1539
 *
 */
public class ParserException extends Exception {
    
    private final String rawData;
    private final String parserType;
    
    /**
     * Create a ParserException with message and context.
     * 
     * @param message Error message
     * @param rawData The raw data that failed to parse
     * @param parserType The type of parser that failed
     */
    public ParserException(String message, String rawData, String parserType) {
        super(message);
        this.rawData = rawData;
        this.parserType = parserType;
    }
    
    /**
     * Create a ParserException with message, cause, and context.
     * 
     * @param message Error message
     * @param cause The underlying exception
     * @param rawData The raw data that failed to parse
     * @param parserType The type of parser that failed
     */
    public ParserException(String message, Throwable cause, String rawData, String parserType) {
        super(message, cause);
        this.rawData = rawData;
        this.parserType = parserType;
    }
    
    /**
     * Get the raw data that failed to parse.
     * 
     * @return The raw data (may be truncated if very long)
     */
    public String getRawData() {
        return rawData;
    }
    
    /**
     * Get the parser type that failed.
     * 
     * @return The parser type identifier
     */
    public String getParserType() {
        return parserType;
    }
    
    @Override
    public String toString() {
        return String.format("ParserException{parser='%s', message='%s', rawData='%s'}", 
                           parserType, 
                           getMessage(), 
                           rawData != null && rawData.length() > 50 
                               ? rawData.substring(0, 50) + "..." 
                               : rawData);
    }
}
