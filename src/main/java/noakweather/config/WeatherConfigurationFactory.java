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

/**
 * Weather Configuration Factory
 *
 * 
 * @author bclasky1539
 *
 */
public final class WeatherConfigurationFactory {
    private static WeatherConfigurationService instance;
    
    // Private constructor to prevent instantiation
    private WeatherConfigurationFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    public static WeatherConfigurationService getInstance() {
        if (instance == null) {
            instance = new ResourceBundleWeatherConfigurationService();
        }
        return instance;
    }

    // For testing - allows injection of mock configurations
    public static void setInstance(WeatherConfigurationService service) {
        instance = service;
    }
    
    // Reset to default (useful for test cleanup)
    public static void reset() {
        instance = null;
    }
}
