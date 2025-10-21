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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable geographic location representation using Java 17 record.
 * 
 * Design principles:
 * - Immutable: Thread-safe, cacheable
 * - Value object: Equality based on coordinates
 * - Validation: Ensures valid lat/long ranges
 * 
 * @author bclasky1539
 *
 */
public record GeoLocation(
    double latitude,
    double longitude,
    Integer elevationMeters
) {
    
    /**
     * Compact constructor with validation
     */
    @JsonCreator
    public GeoLocation {
        validateLatitude(latitude);
        validateLongitude(longitude);
    }
    
    /**
     * Constructor without elevation
     */
    public GeoLocation(double latitude, double longitude) {
        this(latitude, longitude, null);
    }
    
    /**
     * Create from feet elevation (common in aviation)
     */
    public static GeoLocation fromFeet(double latitude, double longitude, Integer elevationFeet) {
        Integer elevationMeters = elevationFeet != null 
            ? (int) Math.round(elevationFeet * 0.3048) 
            : null;
        return new GeoLocation(latitude, longitude, elevationMeters);
    }
    
    private static void validateLatitude(double lat) {
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException(
                "Latitude must be between -90 and 90, got: %.4f".formatted(lat));
        }
    }
    
    private static void validateLongitude(double lon) {
        if (lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException(
                "Longitude must be between -180 and 180, got: %.4f".formatted(lon));
        }
    }
    
    /**
     * Get elevation in feet (aviation standard)
     */
    public Integer elevationFeet() {
        return elevationMeters != null 
            ? (int) Math.round(elevationMeters / 0.3048) 
            : null;
    }
    
    /**
     * Calculate distance to another location using Haversine formula
     * @return distance in kilometers
     */
    public double distanceTo(GeoLocation other) {
        final double EARTH_RADIUS_KM = 6371.0;
        
        double lat1Rad = Math.toRadians(this.latitude());
        double lat2Rad = Math.toRadians(other.latitude());
        double deltaLat = Math.toRadians(other.latitude() - this.latitude());
        double deltaLon = Math.toRadians(other.longitude() - this.longitude());
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
    
    @Override
    public String toString() {
        return elevationMeters() != null
            ? "GeoLocation{lat=%.4f, lon=%.4f, elev=%dm}".formatted(latitude(), longitude(), elevationMeters())
            : "GeoLocation{lat=%.4f, lon=%.4f}".formatted(latitude(), longitude());
    }
}
