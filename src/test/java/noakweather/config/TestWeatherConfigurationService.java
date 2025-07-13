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
package noakweather.config;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

/**
 * Test Weather Configuration Service
 * Implements Weather Configuration Service
 * 
 * @author bclasky1539
 *
 */
public class TestWeatherConfigurationService implements WeatherConfigurationService {
    private final Map<String, String> configurations = new HashMap<>();
    
    // Fluent methods for test setup
    public TestWeatherConfigurationService withWeatherCondition(String condition, String value) {
        configurations.put("WEATHER_" + condition, value);
        return this;
    }
    
    public TestWeatherConfigurationService withCloudType(String type, String value) {
        configurations.put("CLOUD_" + type, value);
        return this;
    }
    
    public TestWeatherConfigurationService withExceptionMessage(String type, String value) {
        configurations.put("EXCEP_" + type, value);
        return this;
    }
    
    public TestWeatherConfigurationService withRawConfig(String key, String value) {
        configurations.put(key, value);
        return this;
    }
    
    // Interface implementations
    @Override
    public String getWeatherCondition(String condition) {
        return getString("WEATHER_" + condition);
    }
    
    @Override
    public String getWeatherDescription(String condition) {
        return getString("WEATHER_DECODED_" + condition);
    }
    
    @Override
    public String getIntensityDescription(String intensity) {
        return getString("WEATHER_DECODED_" + intensity);
    }
    
    @Override
    public String getCloudType(String type) {
        return getString("CLOUD_" + type);
    }
    
    @Override
    public String getCloudDescription(String type) {
        return getString("CLOUD_DECODED_" + type);
    }
    
    @Override
    public String getWindDirection(String direction) {
        return getString("WIND_DIR_" + direction);
    }
    
    @Override
    public String getWindUnit(String unit) {
        return getString("WIND_" + unit);
    }
    
    @Override
    public String getWindDescription(String condition) {
        return getString("WIND_DECODED_" + condition);
    }
    
    @Override
    public String getExceptionMessage(String type) {
        return getString("EXCEP_" + type);
    }
    
    @Override
    public String getLogMessage(String level) {
        return getString("LOG_DECODED_" + level);
    }
    
    @Override
    public String getRawString(String key) {
        return getString(key);
    }
    
    @Override
    public String getRawString(String key, Object... arguments) {
        return MessageFormat.format(getString(key), arguments);
    }
    
    @Override
    public void setLocale(Locale locale) {
        // Test implementation - can be enhanced if needed
    }
    
    private String getString(String key) {
        String value = configurations.get(key);
        if (value == null) {
            throw new MissingResourceException("Key not found: " + key, "TestConfig", key);
        }
        return value;
    }
}
