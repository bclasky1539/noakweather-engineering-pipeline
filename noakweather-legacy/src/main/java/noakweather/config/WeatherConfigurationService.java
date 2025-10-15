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

import java.util.Locale;

/**
 * Weather Configuration Service
 *
 * 
 * @author bclasky1539
 *
 */
public interface WeatherConfigurationService {
    // Weather domain methods
    String getWeatherCondition(String condition);
    String getWeatherDescription(String condition);
    String getIntensityDescription(String intensity);
    
    // Cloud domain methods
    String getCloudType(String type);
    String getCloudDescription(String type);
    
    // Wind domain methods
    String getWindDirection(String direction);
    String getWindUnit(String unit);
    String getWindDescription(String condition);
    
    // Exception/logging methods
    String getExceptionMessage(String type);
    String getLogMessage(String level);
    
    // Raw access for edge cases
    String getRawString(String key);
    String getRawString(String key, Object... arguments);
    
    // Locale support
    void setLocale(Locale locale);
}
