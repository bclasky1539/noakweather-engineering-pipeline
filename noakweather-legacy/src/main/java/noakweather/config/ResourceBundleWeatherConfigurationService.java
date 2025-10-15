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
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Resource Bundle Weather Configuration Service
 * Implements from Weather Configuration Service
 * 
 * @author bclasky1539
 *
 */
public class ResourceBundleWeatherConfigurationService implements WeatherConfigurationService {
    private static final String BUNDLE_NAME = "configs";
    private ResourceBundle resourceBundle;
    
    public ResourceBundleWeatherConfigurationService() {
        this(Locale.getDefault());
    }
    
    public ResourceBundleWeatherConfigurationService(Locale locale) {
        setLocale(locale);
    }
    
    @Override
    public void setLocale(Locale locale) {
        Locale.setDefault(locale);
        ResourceBundle.clearCache();
        resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
    }
    
    // Weather domain methods
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
    
    // Cloud domain methods
    @Override
    public String getCloudType(String type) {
        return getString("CLOUD_" + type);
    }
    
    @Override
    public String getCloudDescription(String type) {
        return getString("CLOUD_DECODED_" + type);
    }
    
    // Wind domain methods
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
    
    // Exception/logging methods
    @Override
    public String getExceptionMessage(String type) {
        return getString("EXCEP_" + type);
    }
    
    @Override
    public String getLogMessage(String level) {
        return getString("LOG_DECODED_" + level);
    }
    
    // Raw access methods
    @Override
    public String getRawString(String key) {
        return getString(key);
    }
    
    @Override
    public String getRawString(String key, Object... arguments) {
        return MessageFormat.format(getString(key), arguments);
    }
    
    private String getString(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            // Log the missing key and return a default value
            return "Missing config: " + key;
        }
    }
}
