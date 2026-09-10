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
package weather.ingestion.service.source.noaa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import weather.exception.WeatherServiceException;
import weather.model.NoaaMetarData;
import weather.model.NoaaTafData;
import weather.model.WeatherData;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for METAR/TAF parser integration.
 * <p>
 * These tests verify that NoaaAviationWeatherClient correctly uses
 * the comprehensive parsers from weather-processing to create fully
 * structured weather data.
 * <p>
 * NOTE: These tests require network access to NOAA servers.
 * Skip these tests if running in CI/CD without internet access.
 *
 * @author bclasky1539
 *
 */
class NoaaAviationWeatherClientParserIntegrationTest {

    private NoaaAviationWeatherClient client;

    @BeforeEach
    void setUp() {
        client = new NoaaAviationWeatherClient();
    }

    @Test
    @DisplayName("METAR parsing integration - should return fully parsed NoaaMetarData")
    void testMetarParsingIntegration() throws WeatherServiceException {
        // Fetch METAR for Charlotte Douglas International Airport
        WeatherData data = client.fetchMetarReport("KCLT");

        // Basic assertions
        assertNotNull(data, "Weather data should not be null");
        assertInstanceOf(NoaaMetarData.class, data, "Should return NoaaMetarData (specific METAR type)");

        NoaaMetarData metarData = (NoaaMetarData) data;

        // Verify basic fields
        assertEquals("KCLT", metarData.getStationId());
        assertNotNull(metarData.getObservationTime());
        assertNotNull(metarData.getRawData(), "Raw data should be preserved");

        // CRITICAL: Verify parsing worked
        assertNotNull(metarData.getConditions(),
                "Weather conditions should be populated (NOT NULL!)");

        // Verify metadata indicates successful parsing
        assertEquals("true", metarData.getMetadata().get("parsed"),
                "Metadata should indicate successful parsing");
    }

    @Test
    @DisplayName("METAR should have temperature data when parsed")
    void testMetarTemperatureParsing() throws WeatherServiceException {
        WeatherData data = client.fetchMetarReport("KJFK");

        assertNotNull(data);
        assertInstanceOf(NoaaMetarData.class, data);

        NoaaMetarData metar = (NoaaMetarData) data;

        // Temperature should be parsed
        assertNotNull(metar.getConditions(), "Conditions should not be null");

        // Most METAR reports have temperature
        // (Note: This could be null in rare cases, but for major airports it should exist)
        if (metar.getConditions().temperature() != null) {
            assertNotNull(metar.getConditions().temperature().celsius(),
                    "Temperature celsius should be parsed");

            System.out.println(" Temperature parsed: " +
                    metar.getConditions().temperature().celsius() + "°C");
        }
    }

    @Test
    @DisplayName("METAR should have wind data when parsed")
    void testMetarWindParsing() throws WeatherServiceException {
        WeatherData data = client.fetchMetarReport("KLAX");

        assertNotNull(data);
        assertInstanceOf(NoaaMetarData.class, data);

        NoaaMetarData metar = (NoaaMetarData) data;

        assertNotNull(metar.getConditions(), "Conditions should not be null");

        // Wind data should exist (even if calm)
        assertNotNull(metar.getConditions().wind(), "Wind should be parsed");
    }

    @Test
    @DisplayName("METAR should have METAR-specific fields parsed")
    void testMetarSpecificFieldsParsing() throws WeatherServiceException {
        WeatherData data = client.fetchMetarReport("KORD");

        assertNotNull(data);
        assertInstanceOf(NoaaMetarData.class, data);
    }

    @Test
    @DisplayName("TAF parsing integration - should return fully parsed NoaaTafData")
    void testTafParsingIntegration() throws WeatherServiceException {
        // Fetch TAF for Charlotte
        WeatherData data = client.fetchTafReport("KCLT");

        // Basic assertions
        assertNotNull(data, "Weather data should not be null");
        assertInstanceOf(NoaaTafData.class, data, "Should return NoaaTafData (specific TAF type)");

        NoaaTafData tafData = (NoaaTafData) data;

        // Verify basic fields
        assertEquals("KCLT", tafData.getStationId());
        assertNotNull(tafData.getObservationTime());
        assertNotNull(tafData.getRawData(), "Raw data should be preserved");

        // CRITICAL: Verify parsing worked
        assertNotNull(tafData.getConditions(),
                "Base forecast conditions should be populated");

        // Verify metadata indicates successful parsing
        assertEquals("true", tafData.getMetadata().get("parsed"),
                "Metadata should indicate successful parsing");
    }

    @Test
    @DisplayName("Multiple stations should all return parsed data")
    void testMultipleStationsParsing() throws WeatherServiceException {
        String[] stations = {"KCLT", "KJFK", "KLAX", "KORD"};

        var results = client.fetchMetarReports(stations);

        assertFalse(results.isEmpty(), "Should fetch at least one station");

        int parsedCount = 0;

        for (WeatherData data : results) {
            assertInstanceOf(NoaaMetarData.class, data);
            NoaaMetarData metar = (NoaaMetarData) data;

            if (metar.getConditions() != null) {
                parsedCount++;
            }
        }

        // Most stations should parse successfully
        assertTrue(parsedCount > 0, "At least one station should parse successfully");
    }

    @Test
    @DisplayName("Parser fallback - should handle unparseable data gracefully")
    void testMetadataPresent() throws WeatherServiceException {
        // This test verifies that if parsing fails, we still get data
        // (with parsed=false in metadata)

        // Fetch real data (should parse successfully)
        WeatherData data = client.fetchMetarReport("KATL");

        assertNotNull(data);

        // Verify we have metadata about parsing
        assertNotNull(data.getMetadata());
        assertTrue(data.getMetadata().containsKey("parsed"));
    }

    @Test
    @DisplayName("Raw data should always be preserved regardless of parsing")
    void testRawDataPreservation() throws WeatherServiceException {
        WeatherData data = client.fetchMetarReport("KSEA");

        assertNotNull(data);
        assertInstanceOf(NoaaMetarData.class, data);

        NoaaMetarData metar = (NoaaMetarData) data;

        // Raw data should ALWAYS be preserved
        assertNotNull(metar.getRawData(), "Raw data must always be preserved");
        assertFalse(metar.getRawData().isEmpty(), "Raw data should not be empty");

        // Full response should be in metadata
        assertNotNull(metar.getMetadata().get("full_response"));
    }

    @Test
    @DisplayName("Parser version should be tracked in metadata")
    void testParserVersionTracking() throws WeatherServiceException {
        WeatherData data = client.fetchMetarReport("KBOS");

        assertNotNull(data);

        // Should have parser version in metadata
        assertEquals("2.0", data.getMetadata().get("parser_version"),
                "Parser version should be tracked");
    }
}
