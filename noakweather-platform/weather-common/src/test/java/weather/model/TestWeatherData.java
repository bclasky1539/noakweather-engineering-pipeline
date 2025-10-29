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
package weather.model;

import java.time.Instant;

/**
 * Minimal concrete implementation of WeatherData for testing purposes only.
 * Does NOT override equals() or hashCode(), so we can test the parent class behavior.
 * 
 * @author bclasky1539
 *
 */
final class TestWeatherData extends WeatherData {
    
    public TestWeatherData() {
        super();
    }
    
    public TestWeatherData(WeatherDataSource source, String stationId, Instant observationTime) {
        super(source, stationId, observationTime);
    }
    
    @Override
    public boolean isCurrent() {
        return true; // Simple implementation for testing
    }
    
    @Override
    public String getDataType() {
        return "TEST";
    }
    
    @Override
    public String getSummary() {
        return "Test Weather Data: " + getStationId();
    }
}
