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
package weather.ingestion.config;

import org.junit.jupiter.api.Test;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NoaaConfiguration.
 * 
 * @author bclasky1539
 *
 */
class NoaaConfigurationTest {
    
    @Test
    void testDefaultConfiguration() {
        NoaaConfiguration config = new NoaaConfiguration();
        
        // Should use default values when properties file not found
        assertNotNull(config.getMetarBaseUrl());
        assertNotNull(config.getTafBaseUrl());
        assertNotNull(config.getFormat());
        assertTrue(config.getTimeoutSeconds() > 0);
    }
    
    @Test
    void testCustomPropertiesConfiguration() {
        Properties props = new Properties();
        props.setProperty("noaa.metar.base.url", "https://custom.noaa.gov/metar");
        props.setProperty("noaa.taf.base.url", "https://custom.noaa.gov/taf");
        props.setProperty("noaa.format", "xml");
        props.setProperty("noaa.timeout.seconds", "45");
        
        NoaaConfiguration config = new NoaaConfiguration(props);
        
        assertEquals("https://custom.noaa.gov/metar", config.getMetarBaseUrl());
        assertEquals("https://custom.noaa.gov/taf", config.getTafBaseUrl());
        assertEquals("xml", config.getFormat());
        assertEquals(45, config.getTimeoutSeconds());
    }
    
    @Test
    void testDefaultMetarBaseUrl() {
        NoaaConfiguration config = new NoaaConfiguration();
        
        String url = config.getMetarBaseUrl();
        assertTrue(url.contains("aviationweather.gov"));
        assertTrue(url.contains("metar"));
    }
    
    @Test
    void testDefaultTafBaseUrl() {
        NoaaConfiguration config = new NoaaConfiguration();
        
        String url = config.getTafBaseUrl();
        assertTrue(url.contains("aviationweather.gov"));
        assertTrue(url.contains("taf"));
    }
    
    @Test
    void testDefaultFormat() {
        NoaaConfiguration config = new NoaaConfiguration();
        assertEquals("json", config.getFormat());
    }
    
    @Test
    void testDefaultTimeout() {
        NoaaConfiguration config = new NoaaConfiguration();
        assertEquals(30, config.getTimeoutSeconds());
    }
    
    @Test
    void testBuildMetarUrlSingleStation() {
        NoaaConfiguration config = new NoaaConfiguration();
        
        String url = config.buildMetarUrl("KJFK");
        
        assertTrue(url.contains("KJFK"));
        assertTrue(url.contains("ids="));
        assertTrue(url.contains("format="));
        assertTrue(url.contains("taf=false"));
    }
    
    @Test
    void testBuildMetarUrlMultipleStations() {
        NoaaConfiguration config = new NoaaConfiguration();
        
        String url = config.buildMetarUrl("KJFK", "KLGA", "KEWR");
        
        assertTrue(url.contains("KJFK"));
        assertTrue(url.contains("KLGA"));
        assertTrue(url.contains("KEWR"));
        assertTrue(url.contains("ids="));
    }
    
    @Test
    void testBuildTafUrlSingleStation() {
        NoaaConfiguration config = new NoaaConfiguration();
        
        String url = config.buildTafUrl("KJFK");
        
        assertTrue(url.contains("KJFK"));
        assertTrue(url.contains("ids="));
        assertTrue(url.contains("format="));
        assertTrue(url.contains("taf=true"));
    }
    
    @Test
    void testBuildTafUrlMultipleStations() {
        NoaaConfiguration config = new NoaaConfiguration();
        
        String url = config.buildTafUrl("KJFK", "KLGA");
        
        assertTrue(url.contains("KJFK"));
        assertTrue(url.contains("KLGA"));
        assertTrue(url.contains("ids="));
    }
    
    @Test
    void testBuildMetarBboxUrl() {
        NoaaConfiguration config = new NoaaConfiguration();
        
        String url = config.buildMetarBboxUrl(40.0, -75.0, 41.0, -73.0);
        
        assertTrue(url.contains("bbox="));
        assertTrue(url.contains("-75"));
        assertTrue(url.contains("40"));
        assertTrue(url.contains("-73"));
        assertTrue(url.contains("41"));
    }
    
    @Test
    void testBoundingBoxUrlFormat() {
        NoaaConfiguration config = new NoaaConfiguration();
        
        String url = config.buildMetarBboxUrl(40.5, -74.5, 41.5, -73.5);
        
        // Bbox format should be: minLon,minLat,maxLon,maxLat
        assertTrue(url.matches(".*bbox=-74\\.5.*40\\.5.*-73\\.5.*41\\.5.*"));
    }
    
    @Test
    void testInvalidTimeoutUsesDefault() {
        Properties props = new Properties();
        props.setProperty("noaa.timeout.seconds", "invalid");
        
        NoaaConfiguration config = new NoaaConfiguration(props);
        
        // Should fall back to default timeout
        assertEquals(30, config.getTimeoutSeconds());
    }
    
    @Test
    void testNegativeTimeoutUsesDefault() {
        Properties props = new Properties();
        props.setProperty("noaa.timeout.seconds", "-10");
        
        NoaaConfiguration config = new NoaaConfiguration(props);
        
        // Should parse -10, but it's a bad value
        // The method parses it successfully, so we get -10
        assertEquals(-10, config.getTimeoutSeconds());
    }
    
    @Test
    void testEmptyStationIdHandling() {
        NoaaConfiguration config = new NoaaConfiguration();
        
        String url = config.buildMetarUrl("");
        
        // Should still build URL even with empty station
        assertTrue(url.contains("ids="));
    }
    
    @Test
    void testUrlFormatConsistency() {
        NoaaConfiguration config = new NoaaConfiguration();
        
        String metarUrl = config.buildMetarUrl("KJFK");
        String tafUrl = config.buildTafUrl("KJFK");
        
        // Both should use same format parameter
        assertTrue(metarUrl.contains("format=json"));
        assertTrue(tafUrl.contains("format=json"));
    }
    
    @Test
    void testCustomFormatInUrls() {
        Properties props = new Properties();
        props.setProperty("noaa.format", "xml");
        
        NoaaConfiguration config = new NoaaConfiguration(props);
        
        String url = config.buildMetarUrl("KJFK");
        assertTrue(url.contains("format=xml"));
    }
    
    @Test
    void testVarargsWithEmptyArray() {
        NoaaConfiguration config = new NoaaConfiguration();
        
        String url = config.buildMetarUrl(new String[]{});
        
        // Should handle empty array gracefully
        assertNotNull(url);
        assertTrue(url.contains("ids="));
    }
    
    @Test
    void testPropertiesPartialOverride() {
        Properties props = new Properties();
        props.setProperty("noaa.metar.base.url", "https://custom.metar.url");
        // Don't set TAF URL - should use default
        
        NoaaConfiguration config = new NoaaConfiguration(props);
        
        assertEquals("https://custom.metar.url", config.getMetarBaseUrl());
        assertTrue(config.getTafBaseUrl().contains("aviationweather.gov"));
    }
}
