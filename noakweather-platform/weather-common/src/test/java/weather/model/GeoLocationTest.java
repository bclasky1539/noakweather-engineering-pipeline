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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GeoLocation record
 * 
 * @author bclasky1539
 *
 */
class GeoLocationTest {
    
    @Test
    @DisplayName("Should create GeoLocation with valid coordinates")
    void testValidCoordinates() {
        GeoLocation location = new GeoLocation(40.7128, -74.0060, 10);
        
        assertEquals(40.7128, location.latitude());
        assertEquals(-74.0060, location.longitude());
        assertEquals(10, location.elevationMeters());
    }
    
    @Test
    @DisplayName("Should create GeoLocation without elevation")
    void testWithoutElevation() {
        GeoLocation location = new GeoLocation(40.7128, -74.0060);
        
        assertEquals(40.7128, location.latitude());
        assertEquals(-74.0060, location.longitude());
        assertNull(location.elevationMeters());
    }
    
    @Test
    @DisplayName("Should throw exception for invalid latitude")
    void testInvalidLatitude() {
        assertThrows(IllegalArgumentException.class, 
            () -> new GeoLocation(91.0, -74.0060));
        
        assertThrows(IllegalArgumentException.class, 
            () -> new GeoLocation(-91.0, -74.0060));
    }
    
    @Test
    @DisplayName("Should throw exception for invalid longitude")
    void testInvalidLongitude() {
        assertThrows(IllegalArgumentException.class, 
            () -> new GeoLocation(40.7128, 181.0));
        
        assertThrows(IllegalArgumentException.class, 
            () -> new GeoLocation(40.7128, -181.0));
    }
    
    @Test
    @DisplayName("Should accept boundary values for coordinates")
    void testBoundaryValues() {
        assertDoesNotThrow(() -> new GeoLocation(90.0, 180.0));
        assertDoesNotThrow(() -> new GeoLocation(-90.0, -180.0));
        assertDoesNotThrow(() -> new GeoLocation(0.0, 0.0));
    }
    
    @Test
    @DisplayName("Should create from feet elevation")
    void testFromFeet() {
        GeoLocation location = GeoLocation.fromFeet(40.7128, -74.0060, 33);
        
        assertEquals(40.7128, location.latitude());
        assertEquals(-74.0060, location.longitude());
        assertEquals(10, location.elevationMeters()); // 33 feet ≈ 10 meters
    }
    
    @Test
    @DisplayName("Should convert elevation to feet")
    void testElevationFeet() {
        GeoLocation location = new GeoLocation(40.7128, -74.0060, 10);
        
        assertEquals(33, location.elevationFeet()); // 10 meters ≈ 33 feet
    }
    
    @Test
    @DisplayName("Should calculate distance between two locations")
    void testDistanceTo() {
        // New York City
        GeoLocation nyc = new GeoLocation(40.7128, -74.0060);
        
        // Los Angeles
        GeoLocation la = new GeoLocation(34.0522, -118.2437);
        
        double distance = nyc.distanceTo(la);
        
        // NYC to LA is approximately 3944 km
        assertTrue(distance > 3900 && distance < 4000, 
            "Distance should be approximately 3944 km, got: " + distance);
    }
    
    @Test
    @DisplayName("Should calculate zero distance for same location")
    void testDistanceToSameLocation() {
        GeoLocation location1 = new GeoLocation(40.7128, -74.0060);
        GeoLocation location2 = new GeoLocation(40.7128, -74.0060);
        
        assertEquals(0.0, location1.distanceTo(location2), 0.001);
    }
    
    @Test
    @DisplayName("Should implement equals correctly")
    void testEquals() {
        GeoLocation location1 = new GeoLocation(40.7128, -74.0060, 10);
        GeoLocation location2 = new GeoLocation(40.7128, -74.0060, 10);
        GeoLocation location3 = new GeoLocation(40.7128, -74.0060, 20);
        
        assertEquals(location1, location2);
        assertNotEquals(location1, location3);
    }
    
    @Test
    @DisplayName("Should implement hashCode correctly")
    void testHashCode() {
        GeoLocation location1 = new GeoLocation(40.7128, -74.0060, 10);
        GeoLocation location2 = new GeoLocation(40.7128, -74.0060, 10);
        
        assertEquals(location1.hashCode(), location2.hashCode());
    }
    
    @Test
    @DisplayName("Should have meaningful toString")
    void testToString() {
        GeoLocation location = new GeoLocation(40.7128, -74.0060, 10);
        String str = location.toString();
        
        assertTrue(str.contains("40.7128"));
        assertTrue(str.contains("-74.0060"));
        assertTrue(str.contains("10"));
    }
    
    @Test
    @DisplayName("Should handle null elevation in toString")
    void testToStringWithoutElevation() {
        GeoLocation location = new GeoLocation(40.7128, -74.0060);
        String str = location.toString();
        
        assertTrue(str.contains("40.7128"));
        assertTrue(str.contains("-74.0060"));
        assertFalse(str.contains("elev"));
    }
    
    @Test
    @DisplayName("Should create from feet with null elevation")
    void testFromFeetWithNullElevation() {
        GeoLocation location = GeoLocation.fromFeet(40.7128, -74.0060, null);
        
        assertEquals(40.7128, location.latitude());
        assertEquals(-74.0060, location.longitude());
        assertNull(location.elevationMeters(), "Elevation should be null when input is null");
    }
    
    @Test
    @DisplayName("Should return null from elevationFeet when elevation is null")
    void testElevationFeetWithNullElevation() {
        GeoLocation location = new GeoLocation(40.7128, -74.0060, null);
        
        assertNull(location.elevationFeet(), "Should return null when elevation is null");
    }
    
    @Test
    @DisplayName("Should handle zero elevation in fromFeet")
    void testFromFeetWithZeroElevation() {
        GeoLocation location = GeoLocation.fromFeet(40.7128, -74.0060, 0);
        
        assertEquals(0, location.elevationMeters());
    }
    
    @Test
    @DisplayName("Should handle zero elevation in elevationFeet")
    void testElevationFeetWithZeroElevation() {
        GeoLocation location = new GeoLocation(40.7128, -74.0060, 0);
        
        assertEquals(0, location.elevationFeet());
    }
    
    @Test
    @DisplayName("Should handle negative elevation")
    void testNegativeElevation() {
        // Death Valley is below sea level: -282 feet ≈ -86 meters
        GeoLocation location = GeoLocation.fromFeet(36.5323, -116.9325, -282);
        
        assertTrue(location.elevationMeters() < 0);
        assertTrue(location.elevationFeet() < 0);
    }
}
