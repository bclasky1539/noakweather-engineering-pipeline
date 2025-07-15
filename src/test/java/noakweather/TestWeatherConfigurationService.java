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
package noakweather;

import noakweather.config.WeatherConfigurationService;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;


/**
 * Test implementation of WeatherConfigurationService for unit testing.
 * 
 * This class provides a controllable configuration service that allows tests
 * to set up specific configuration values without depending on external files
 * or system configuration.
 * 
 * Think of this as a "test double" or "mock configuration" that lets you
 * simulate different configuration scenarios in your tests.
 * 
 * @author bclasky1539
 * 
 */
public class TestWeatherConfigurationService implements WeatherConfigurationService {
    
    private final Map<String, String> rawConfigs = new HashMap<>();
    private final Map<String, String> exceptionMessages = new HashMap<>();
    private final Map<String, String> logMessages = new HashMap<>();
    private final Map<String, String> weatherConditions = new HashMap<>();
    private final Map<String, String> weatherDescriptions = new HashMap<>();
    private final Map<String, String> intensityDescriptions = new HashMap<>();
    private final Map<String, String> cloudTypes = new HashMap<>();
    private final Map<String, String> cloudDescriptions = new HashMap<>();
    private final Map<String, String> windDirections = new HashMap<>();
    private final Map<String, String> windUnits = new HashMap<>();
    private final Map<String, String> windDescriptions = new HashMap<>();
    private Locale currentLocale = Locale.getDefault();
    
    /**
     * Creates a new test configuration service with empty configuration.
     */
    public TestWeatherConfigurationService() {
        // Initialize with some sensible defaults
        rawConfigs.put("MISC_WEATHER_DATA", "Weather Data:");
        rawConfigs.put("MISC_STATION", "Station:");
    }
    
    /**
     * Adds a raw configuration value for testing.
     * 
     * @param key The configuration key
     * @param value The configuration value
     * @return This instance for method chaining
     */
    public TestWeatherConfigurationService withRawConfig(String key, String value) {
        rawConfigs.put(key, value);
        return this;
    }
    
    /**
     * Adds an exception message for testing.
     * 
     * @param key The exception message key
     * @param message The exception message
     * @return This instance for method chaining
     */
    public TestWeatherConfigurationService withExceptionMessage(String key, String message) {
        exceptionMessages.put(key, message);
        return this;
    }
    
    /**
     * Adds a log message for testing.
     * 
     * @param key The log message key
     * @param message The log message
     * @return This instance for method chaining
     */
    public TestWeatherConfigurationService withLogMessage(String key, String message) {
        logMessages.put(key, message);
        return this;
    }
    
    // Weather domain methods
    @Override
    public String getWeatherCondition(String condition) {
        return weatherConditions.getOrDefault(condition, "Test weather condition: " + condition);
    }
    
    @Override
    public String getWeatherDescription(String condition) {
        return weatherDescriptions.getOrDefault(condition, "Test weather description: " + condition);
    }
    
    @Override
    public String getIntensityDescription(String intensity) {
        return intensityDescriptions.getOrDefault(intensity, "Test intensity: " + intensity);
    }
    
    // Cloud domain methods
    @Override
    public String getCloudType(String type) {
        return cloudTypes.getOrDefault(type, "Test cloud type: " + type);
    }
    
    @Override
    public String getCloudDescription(String type) {
        return cloudDescriptions.getOrDefault(type, "Test cloud description: " + type);
    }
    
    // Wind domain methods
    @Override
    public String getWindDirection(String direction) {
        return windDirections.getOrDefault(direction, "Test wind direction: " + direction);
    }
    
    @Override
    public String getWindUnit(String unit) {
        return windUnits.getOrDefault(unit, "Test wind unit: " + unit);
    }
    
    @Override
    public String getWindDescription(String condition) {
        return windDescriptions.getOrDefault(condition, "Test wind description: " + condition);
    }
    
    // Exception/logging methods
    @Override
    public String getExceptionMessage(String type) {
        String message = exceptionMessages.get(type);
        if (message == null) {
            return "Test exception message for: " + type;
        }
        return message;
    }
    
    @Override
    public String getLogMessage(String level) {
        String message = logMessages.get(level);
        if (message == null) {
            return "Test log message for: " + level;
        }
        return message;
    }
    
    // Raw access methods
    @Override
    public String getRawString(String key) {
        String value = rawConfigs.get(key);
        if (value == null) {
            throw new RuntimeException("Configuration key not found: " + key);
        }
        return value;
    }
    
    @Override
    public String getRawString(String key, Object... arguments) {
        String template = getRawString(key);
        if (arguments.length > 0) {
            return String.format(template, arguments);
        }
        return template;
    }
    
    // Locale support
    @Override
    public void setLocale(Locale locale) {
        this.currentLocale = locale;
    }
    
    /**
     * Gets the current locale for testing purposes.
     * 
     * @return The current locale
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }
    
    /**
     * Clears all configuration for a fresh start.
     */
    public void clear() {
        rawConfigs.clear();
        exceptionMessages.clear();
        logMessages.clear();
        weatherConditions.clear();
        weatherDescriptions.clear();
        intensityDescriptions.clear();
        cloudTypes.clear();
        cloudDescriptions.clear();
        windDirections.clear();
        windUnits.clear();
        windDescriptions.clear();
    }
    
    /**
     * Gets all raw configuration keys for debugging.
     * 
     * @return Set of all raw configuration keys
     */
    public java.util.Set<String> getRawConfigKeys() {
        return rawConfigs.keySet();
    }
    
    /**
     * Gets all exception message keys for debugging.
     * 
     * @return Set of all exception message keys
     */
    public java.util.Set<String> getExceptionMessageKeys() {
        return exceptionMessages.keySet();
    }
    
    /**
     * Gets all log message keys for debugging.
     * 
     * @return Set of all log message keys
     */
    public java.util.Set<String> getLogMessageKeys() {
        return logMessages.keySet();
    }
}
