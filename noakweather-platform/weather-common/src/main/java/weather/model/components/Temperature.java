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
package weather.model.components;

/**
 * Immutable value object representing temperature and dewpoint.
 * 
 * Temperatures are stored in Celsius. This replaces the legacy HashMap-based
 * temperature storage.
 * 
 * Examples:
 * - "22/12" → Temperature(22.0, 12.0)
 * - "M05/M12" → Temperature(-5.0, -12.0)
 * - "15/" → Temperature(15.0, null)
 * 
 * @param celsius Temperature in degrees Celsius
 * @param dewpointCelsius Dewpoint temperature in degrees Celsius, null if not reported
 * 
 * @author bclasky1539
 * 
 */
public record Temperature(
    Double celsius,
    Double dewpointCelsius
) {
    
    /**
     * Compact constructor with validation.
     */
    public Temperature {
        validateTemperature(celsius);
        validateDewpoint(dewpointCelsius);
        validateDewpointNotHigherThanTemp(celsius, dewpointCelsius);
    }
    
    private static void validateTemperature(Double temp) {
        if (temp != null && (temp < -100.0 || temp > 60.0)) {
            throw new IllegalArgumentException(
                "Temperature out of reasonable range (-100 to 60°C): " + temp
            );
        }
    }
    
    private static void validateDewpoint(Double dewpoint) {
        if (dewpoint != null && (dewpoint < -100.0 || dewpoint > 60.0)) {
            throw new IllegalArgumentException(
                "Dewpoint out of reasonable range (-100 to 60°C): " + dewpoint
            );
        }
    }
    
    private static void validateDewpointNotHigherThanTemp(Double temp, Double dewpoint) {
        if (temp != null && dewpoint != null && dewpoint > temp) {
            throw new IllegalArgumentException(
                "Dewpoint (" + dewpoint + "°C) cannot be higher than temperature (" + temp + "°C)"
            );
        }
    }
    
    /**
     * Convert temperature to Fahrenheit.
     * 
     * @return temperature in Fahrenheit
     */
    public Double toFahrenheit() {
        if (celsius == null) {
            return null;
        }
        return celsius * 9.0 / 5.0 + 32.0;
    }
    
    /**
     * Convert dewpoint to Fahrenheit.
     * 
     * @return dewpoint in Fahrenheit
     */
    public Double dewpointToFahrenheit() {
        if (dewpointCelsius == null) {
            return null;
        }
        return dewpointCelsius * 9.0 / 5.0 + 32.0;
    }
    
    /**
     * Calculate relative humidity from temperature and dewpoint.
     * 
     * Uses the August-Roche-Magnus approximation (also known as the Bolton 1980
     * formula), which is the World Meteorological Organization (WMO) recommended
     * method for calculating saturation vapor pressure over liquid water.
     * 
     * Formula: RH = 100 * (e_dewpoint / e_temperature)
     * Where: e(T) = 6.112 * exp((17.67 * T) / (T + 243.5))
     * 
     * This formula is accurate to within ±0.06% for temperatures between
     * -40°C and +50°C, covering the typical range of meteorological observations.
     * 
     * @return relative humidity as percentage (0-100), or null if dewpoint not
     * available
     */
    @SuppressWarnings("java:S6885")  // Math.clamp not available in Java 17
    public Double getRelativeHumidity() {
        if (celsius == null || dewpointCelsius == null) {
            return null;
        }
        
        // August-Roche-Magnus approximation (WMO recommended)
        // Constants: a = 17.67, b = 243.5
        
        // Calculate saturation vapor pressure at temperature
        double eT = 6.112 * Math.exp((17.67 * celsius) / (celsius + 243.5));
        
        // Calculate saturation vapor pressure at dewpoint
        double eTd = 6.112 * Math.exp((17.67 * dewpointCelsius) / (dewpointCelsius + 243.5));
        
        // Calculate relative humidity as percentage
        double rh = 100.0 * (eTd / eT);
        
        // Clamp to reasonable range (0-100)
        // Note: Using Math.max/Math.min for Java 17 compatibility (Math.clamp requires Java 21)
        return Math.max(0.0, Math.min(100.0, rh));
    }
    
    /**
     * Calculate temperature-dewpoint spread (difference).
     * Used for fog/low cloud prediction - smaller spread means higher probability.
     * 
     * @return temperature-dewpoint spread in °C, or null if dewpoint not available
     */
    public Double getSpread() {
        if (celsius == null || dewpointCelsius == null) {
            return null;
        }
        return celsius - dewpointCelsius;
    }
    
    /**
     * Check if conditions are favorable for fog (spread ≤ 2°C).
     * 
     * @return true if fog is likely based on temperature-dewpoint spread
     */
    public boolean isFogLikely() {
        Double spread = getSpread();
        return spread != null && spread <= 2.0;
    }
    
    /**
     * Factory method from Fahrenheit values.
     * 
     * @param fahrenheit Temperature in Fahrenheit
     * @param dewpointFahrenheit Dewpoint in Fahrenheit
     * @return Temperature instance with values converted to Celsius
     */
    public static Temperature fromFahrenheit(double fahrenheit, Double dewpointFahrenheit) {
        double celsius = (fahrenheit - 32.0) * 5.0 / 9.0;
        Double dewpointCelsius = dewpointFahrenheit != null 
            ? (dewpointFahrenheit - 32.0) * 5.0 / 9.0 
            : null;
        return new Temperature(celsius, dewpointCelsius);
    }
}
