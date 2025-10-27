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
package weather.ingestion.service.source.noaa;

import weather.model.ProcessingLayer;
import weather.model.WeatherData;
import weather.model.WeatherDataSource;
import weather.exception.WeatherServiceException;
import weather.exception.ErrorType;
import weather.ingestion.config.NoaaConfiguration;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NoaaAviationWeatherClient using WireMock.
 * These tests don't require actual NOAA API access.
 */
class NoaaAviationWeatherClientTest {
    
    private WireMockServer wireMockServer;
    private NoaaAviationWeatherClient client;
    
    @BeforeEach
    void setUp() {
        // Start WireMock server on random port
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        
        // Configure WireMock
        WireMock.configureFor("localhost", wireMockServer.port());
        
        // Create configuration pointing to WireMock server
        Properties testProps = new Properties();
        testProps.setProperty("noaa.metar.base.url", "http://localhost:" + wireMockServer.port() + "/metar");
        testProps.setProperty("noaa.taf.base.url", "http://localhost:" + wireMockServer.port() + "/taf");
        testProps.setProperty("noaa.timeout.seconds", "30");
        NoaaConfiguration testConfig = new NoaaConfiguration(testProps);
        
        // Create client with test configuration
        client = new NoaaAviationWeatherClient(testConfig);
    }
    
    @AfterEach
    void tearDown() {
        wireMockServer.stop();
        client.close();
    }
    
    @Test
    void testStationCodeValidation() {
        // Valid codes
        assertTrue(client.isValidStationCode("KJFK"));
        assertTrue(client.isValidStationCode("KLGA"));
        assertTrue(client.isValidStationCode("EGLL"));
        assertTrue(client.isValidStationCode("LFPG"));
        assertTrue(client.isValidStationCode("kjfk")); // lowercase should work
        assertTrue(client.isValidStationCode("  KJFK  ")); // with whitespace
        
        // Invalid codes
        assertFalse(client.isValidStationCode(null));
        assertFalse(client.isValidStationCode(""));
        assertFalse(client.isValidStationCode("  "));
        assertFalse(client.isValidStationCode("K1FK")); // contains number
        assertFalse(client.isValidStationCode("KJ")); // too short
        assertFalse(client.isValidStationCode("KJFK1")); // too long
        assertFalse(client.isValidStationCode("KJ-FK")); // contains special char
    }
    
    @Test
    void testInvalidStationCode_ThrowsException() {
        WeatherServiceException exception = assertThrows(WeatherServiceException.class, () -> {
            client.fetchMetarReports("INVALID123");
        });
        
        assertEquals(ErrorType.INVALID_STATION_CODE, exception.getErrorType());
        assertTrue(exception.getMessage().contains("3-4 alphabetic characters"));
    }
    
    @Test
    void testEmptyStationIds_ThrowsException() {
        WeatherServiceException exception = assertThrows(WeatherServiceException.class, () -> {
            client.fetchMetarReports();
        });
        
        assertEquals(ErrorType.INVALID_STATION_CODE, exception.getErrorType());
    }
    
    @Test
    void testNullStationIds_ThrowsException() {
        WeatherServiceException exception = assertThrows(WeatherServiceException.class, () -> {
            client.fetchMetarReports((String[]) null);
        });
        
        assertEquals(ErrorType.INVALID_STATION_CODE, exception.getErrorType());
    }
    
    @Test
    void testFetchMetarReports_Success() throws Exception {
        // Mock NOAA API response
        String mockResponse = """
                [
                    {
                        "rawOb": "METAR KJFK 251651Z 28016KT 10SM FEW250 22/12 A3015",
                        "icaoId": "KJFK",
                        "reportTime": "2025-10-25T16:51:00Z"
                    }
                ]
                """;
        
        stubFor(get(urlPathEqualTo("/metar"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));
        
        // Execute
        List<WeatherData> results = client.fetchMetarReports("KJFK");
        
        // Verify request was made
        verify(getRequestedFor(urlPathEqualTo("/metar"))
                .withQueryParam("ids", equalTo("KJFK"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("User-Agent", equalTo("NoakWeather-Platform/2.0")));
        
        // Verify response
        assertNotNull(results);
        assertEquals(1, results.size());
        
        WeatherData data = results.get(0);
        assertEquals(WeatherDataSource.NOAA, data.getSource());
        assertEquals("KJFK", data.getStationId());
        assertEquals("METAR", data.getDataType());
        assertEquals(ProcessingLayer.SPEED_LAYER, data.getProcessingLayer());
        assertNotNull(data.getIngestionTime());
    }
    
    @Test
    void testFetchMetarReports_MultipleStations() throws Exception {
        // Mock response with data for 3 stations
        String mockResponse = """
                [
                    {"icaoId": "KJFK"},
                    {"icaoId": "KLGA"},
                    {"icaoId": "KEWR"}
                ]
                """;
        
        stubFor(get(urlPathEqualTo("/metar"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));
        
        List<WeatherData> results = client.fetchMetarReports("KJFK", "KLGA", "KEWR");
        
        // Verify query contains all stations
        verify(getRequestedFor(urlPathEqualTo("/metar"))
                .withQueryParam("ids", matching(".*KJFK.*"))
                .withQueryParam("ids", matching(".*KLGA.*"))
                .withQueryParam("ids", matching(".*KEWR.*")));
        
        assertNotNull(results);
        assertEquals(3, results.size());
    }
    
    @Test
    void testFetchTafReports_Success() throws Exception {
        String mockResponse = """
                [
                    {
                        "rawTAF": "TAF KJFK 251720Z 2518/2624 28016KT P6SM FEW250",
                        "icaoId": "KJFK",
                        "issueTime": "2025-10-25T17:20:00Z"
                    }
                ]
                """;
        
        stubFor(get(urlPathEqualTo("/taf"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));
        
        List<WeatherData> results = client.fetchTafReports("KJFK");
        
        verify(getRequestedFor(urlPathEqualTo("/taf")));
        
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("TAF", results.get(0).getDataType());
    }
    
    @Test
    void testFetchLatestMetar_Success() throws Exception {
        String mockResponseBody = "[{\"icaoId\":\"KJFK\"}]";
        
        stubFor(get(urlPathEqualTo("/metar"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponseBody)));
        
        WeatherData result = client.fetchLatestMetar("KJFK");
        
        assertNotNull(result);
        assertEquals("KJFK", result.getStationId());
    }
    
    @Test
    void testFetchLatestMetar_NoData() throws Exception {
        String mockResponseBody = "[]";
        
        stubFor(get(urlPathEqualTo("/metar"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponseBody)));
        
        WeatherData result = client.fetchLatestMetar("KJFK");
        
        assertNull(result, "Should return null when no data available");
    }
    
    @Test
    void testFetchMetarByBoundingBox() throws Exception {
        String mockResponseBody = "[]";
        
        stubFor(get(urlPathEqualTo("/metar"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponseBody)));
        
        List<WeatherData> results = client.fetchMetarByBoundingBox(
                40.0, -75.0, 41.0, -73.0);
        
        verify(getRequestedFor(urlPathEqualTo("/metar"))
                .withQueryParam("bbox", matching(".*-75.*40.*")));
        
        assertNotNull(results);
    }
    
    @Test
    void testApiError_500() {
        stubFor(get(urlPathEqualTo("/metar"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));
        
        WeatherServiceException exception = assertThrows(WeatherServiceException.class, () -> {
            client.fetchMetarReports("KJFK");
        });
        
        assertEquals(ErrorType.NETWORK_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Failed to fetch METAR data"));
    }
    
    @Test
    void testApiError_404() {
        stubFor(get(urlPathEqualTo("/metar"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Not Found")));
        
        WeatherServiceException exception = assertThrows(WeatherServiceException.class, () -> {
            client.fetchMetarReports("KJFK");
        });
        
        assertEquals(ErrorType.NETWORK_ERROR, exception.getErrorType());
    }
    
    @Test
    void testUserAgentHeader() throws Exception {
        stubFor(get(urlPathEqualTo("/metar"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("[]")));
        
        client.fetchMetarReports("KJFK");
        
        verify(getRequestedFor(urlPathEqualTo("/metar"))
                .withHeader("User-Agent", equalTo("NoakWeather-Platform/2.0")));
    }
    
    @Test
    void testConnectionTimeout() {
        // Simulate timeout with delay exceeding configured timeout
        stubFor(get(urlPathEqualTo("/metar"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("[]")
                        .withFixedDelay(35000))); // 35 seconds - exceeds 30s timeout
        
        WeatherServiceException exception = assertThrows(WeatherServiceException.class, () -> {
            client.fetchMetarReports("KJFK");
        });
        
        // Could be either TIMEOUT or NETWORK_ERROR depending on how it fails
        assertTrue(
            exception.getErrorType() == ErrorType.TIMEOUT ||
            exception.getErrorType() == ErrorType.NETWORK_ERROR
        );
    }
}
