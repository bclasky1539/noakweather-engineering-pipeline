/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025-2026 bclasky1539
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
import org.junit.jupiter.api.BeforeEach;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NoaaConfiguration.
 * <p>
 * Updated to test the correct TG FTP endpoints and new configuration structure.
 *
 * @author bclasky1539
 *
 */
class NoaaConfigurationTest {

    private NoaaConfiguration defaultConfig;

    @BeforeEach
    void setUp() {
        defaultConfig = new NoaaConfiguration();
    }

    @Test
    void testDefaultConfiguration() {
        assertNotNull(defaultConfig.getMetarBaseUrl());
        assertNotNull(defaultConfig.getTafBaseUrl());
        assertTrue(defaultConfig.getTimeoutSeconds() > 0);
        assertTrue(defaultConfig.getRetryAttempts() > 0);
        assertTrue(defaultConfig.getRetryDelayMs() > 0);
    }

    @Test
    void testDefaultMetarBaseUrl() {
        String url = defaultConfig.getMetarBaseUrl();
        assertTrue(url.contains("tgftp.nws.noaa.gov"),
                "Should use TG FTP endpoint");
        assertTrue(url.contains("observations/metar/stations"),
                "Should point to METAR stations directory");
    }

    @Test
    void testDefaultTafBaseUrl() {
        String url = defaultConfig.getTafBaseUrl();
        assertTrue(url.contains("tgftp.nws.noaa.gov"),
                "Should use TG FTP endpoint");
        assertTrue(url.contains("forecasts/taf/stations"),
                "Should point to TAF stations directory");
    }

    @Test
    void testDefaultTimeoutSeconds() {
        assertEquals(30, defaultConfig.getTimeoutSeconds());
    }

    @Test
    void testDefaultRetryAttempts() {
        assertEquals(3, defaultConfig.getRetryAttempts());
    }

    @Test
    void testDefaultRetryDelayMs() {
        assertEquals(1000, defaultConfig.getRetryDelayMs());
    }

    @Test
    void testCustomPropertiesConfiguration() {
        Properties props = new Properties();
        props.setProperty("noaa.metar.base.url", "https://custom.noaa.gov/metar");
        props.setProperty("noaa.taf.base.url", "https://custom.noaa.gov/taf");
        props.setProperty("noaa.timeout.seconds", "45");
        props.setProperty("noaa.retry.attempts", "5");
        props.setProperty("noaa.retry.delay.ms", "2000");

        NoaaConfiguration config = new NoaaConfiguration(props);

        assertEquals("https://custom.noaa.gov/metar", config.getMetarBaseUrl());
        assertEquals("https://custom.noaa.gov/taf", config.getTafBaseUrl());
        assertEquals(45, config.getTimeoutSeconds());
        assertEquals(5, config.getRetryAttempts());
        assertEquals(2000, config.getRetryDelayMs());
    }

    @Test
    void testBuildMetarUrl() {
        String url = defaultConfig.buildMetarUrl("KJFK");

        assertTrue(url.contains("KJFK"), "Should contain station ID");
        assertTrue(url.endsWith(".TXT"), "Should end with .TXT extension");
        assertTrue(url.contains("tgftp.nws.noaa.gov"), "Should use TG FTP");
        assertEquals("https://tgftp.nws.noaa.gov/data/observations/metar/stations/KJFK.TXT", url);
    }

    @Test
    void testBuildMetarUrlLowercaseStation() {
        String url = defaultConfig.buildMetarUrl("kjfk");

        // Should convert to uppercase
        assertTrue(url.contains("KJFK"), "Should convert to uppercase");
        assertFalse(url.contains("kjfk"), "Should not contain lowercase");
    }

    @Test
    void testBuildTafUrl() {
        String url = defaultConfig.buildTafUrl("KCLT");

        assertTrue(url.contains("KCLT"), "Should contain station ID");
        assertTrue(url.endsWith(".TXT"), "Should end with .TXT extension");
        assertTrue(url.contains("tgftp.nws.noaa.gov"), "Should use TG FTP");
        assertEquals("https://tgftp.nws.noaa.gov/data/forecasts/taf/stations/KCLT.TXT", url);
    }

    @Test
    void testBuildTafUrlLowercaseStation() {
        String url = defaultConfig.buildTafUrl("kclt");

        // Should convert to uppercase
        assertTrue(url.contains("KCLT"), "Should convert to uppercase");
        assertFalse(url.contains("kclt"), "Should not contain lowercase");
    }

    @Test
    void testValidateConfigurationWithCorrectEndpoints() {
        Properties props = new Properties();
        props.setProperty("noaa.metar.base.url",
                "https://tgftp.nws.noaa.gov/data/observations/metar/stations");
        props.setProperty("noaa.taf.base.url",
                "https://tgftp.nws.noaa.gov/data/forecasts/taf/stations");

        NoaaConfiguration config = new NoaaConfiguration(props);

        assertTrue(config.validateConfiguration(),
                "Should validate correctly with TG FTP endpoints");
    }

    @Test
    void testValidateConfigurationWithIncorrectMetarEndpoint() {
        Properties props = new Properties();
        props.setProperty("noaa.metar.base.url",
                "https://aviationweather.gov/api/data/metar");
        props.setProperty("noaa.taf.base.url",
                "https://tgftp.nws.noaa.gov/data/forecasts/taf/stations");

        NoaaConfiguration config = new NoaaConfiguration(props);

        assertFalse(config.validateConfiguration(),
                "Should fail validation with incorrect aviationweather.gov API endpoint");
    }

    @Test
    void testValidateConfigurationWithIncorrectTafEndpoint() {
        Properties props = new Properties();
        props.setProperty("noaa.metar.base.url",
                "https://tgftp.nws.noaa.gov/data/observations/metar/stations");
        props.setProperty("noaa.taf.base.url",
                "https://aviationweather.gov/api/data/taf");

        NoaaConfiguration config = new NoaaConfiguration(props);

        assertFalse(config.validateConfiguration(),
                "Should fail validation with incorrect aviationweather.gov API endpoint");
    }

    @Test
    void testInvalidTimeoutUsesDefault() {
        Properties props = new Properties();
        props.setProperty("noaa.timeout.seconds", "invalid");

        NoaaConfiguration config = new NoaaConfiguration(props);

        assertEquals(30, config.getTimeoutSeconds(),
                "Should fall back to default timeout on parse error");
    }

    @Test
    void testInvalidRetryAttemptsUsesDefault() {
        Properties props = new Properties();
        props.setProperty("noaa.retry.attempts", "not_a_number");

        NoaaConfiguration config = new NoaaConfiguration(props);

        assertEquals(3, config.getRetryAttempts(),
                "Should fall back to default retry attempts on parse error");
    }

    @Test
    void testInvalidRetryDelayUsesDefault() {
        Properties props = new Properties();
        props.setProperty("noaa.retry.delay.ms", "abc");

        NoaaConfiguration config = new NoaaConfiguration(props);

        assertEquals(1000, config.getRetryDelayMs(),
                "Should fall back to default retry delay on parse error");
    }

    @Test
    void testPartialPropertiesOverride() {
        Properties props = new Properties();
        props.setProperty("noaa.metar.base.url", "https://custom.metar.url");
        // Don't set TAF URL - should use default

        NoaaConfiguration config = new NoaaConfiguration(props);

        assertEquals("https://custom.metar.url", config.getMetarBaseUrl());
        assertTrue(config.getTafBaseUrl().contains("tgftp.nws.noaa.gov"),
                "TAF URL should use default");
    }

    @Test
    void testGetConfigurationSummary() {
        String summary = defaultConfig.getConfigurationSummary();

        assertNotNull(summary);
        assertTrue(summary.contains("NoaaConfiguration"));
        assertTrue(summary.contains("metarBase="));
        assertTrue(summary.contains("tafBase="));
        assertTrue(summary.contains("timeout="));
        assertTrue(summary.contains("retries="));
    }

    @Test
    void testConfigurationSummaryWithCustomValues() {
        Properties props = new Properties();
        props.setProperty("noaa.timeout.seconds", "60");
        props.setProperty("noaa.retry.attempts", "10");

        NoaaConfiguration config = new NoaaConfiguration(props);
        String summary = config.getConfigurationSummary();

        assertTrue(summary.contains("timeout=60s"));
        assertTrue(summary.contains("retries=10"));
    }

    @Test
    void testEmptyStationIdHandling() {
        String metarUrl = defaultConfig.buildMetarUrl("");
        String tafUrl = defaultConfig.buildTafUrl("");

        // Should still build URL even with empty station
        assertTrue(metarUrl.endsWith("/.TXT"));
        assertTrue(tafUrl.endsWith("/.TXT"));
    }

    @Test
    void testMetarUrlFormat() {
        String url = defaultConfig.buildMetarUrl("TEST");

        // Verify exact format
        String expected = "https://tgftp.nws.noaa.gov/data/observations/metar/stations/TEST.TXT";
        assertEquals(expected, url);
    }

    @Test
    void testTafUrlFormat() {
        String url = defaultConfig.buildTafUrl("TEST");

        // Verify exact format
        String expected = "https://tgftp.nws.noaa.gov/data/forecasts/taf/stations/TEST.TXT";
        assertEquals(expected, url);
    }

    @Test
    void testMultipleStationsNotSupported() {
        // The new TG FTP endpoint doesn't support multiple stations in one URL
        // Each station has its own file
        String url1 = defaultConfig.buildMetarUrl("KJFK");
        String url2 = defaultConfig.buildMetarUrl("KLGA");

        assertNotEquals(url1, url2, "Each station should have its own URL");
        assertTrue(url1.contains("KJFK"));
        assertTrue(url2.contains("KLGA"));
    }

    @Test
    void testZeroTimeoutAllowed() {
        Properties props = new Properties();
        props.setProperty("noaa.timeout.seconds", "0");

        NoaaConfiguration config = new NoaaConfiguration(props);

        assertEquals(0, config.getTimeoutSeconds(),
                "Zero timeout should be allowed (infinite wait)");
    }

    @Test
    void testNegativeRetryAttempts() {
        Properties props = new Properties();
        props.setProperty("noaa.retry.attempts", "-1");

        NoaaConfiguration config = new NoaaConfiguration(props);

        // Method parses it successfully, returns negative value
        // Could add validation in the actual class if needed
        assertEquals(-1, config.getRetryAttempts());
    }
}
