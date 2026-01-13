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

import java.net.http.HttpClient;
import java.util.List;
import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NoaaAviationWeatherClient using WireMock.
 * <p>
 * Updated for TG FTP endpoints - tests raw text file fetching.
 *
 * @author bclasky1539
 *
 */
class NoaaAviationWeatherClientTest {

    private WireMockServer wireMockServer;
    private NoaaAviationWeatherClient client;
    private NoaaConfiguration testConfig;

    @BeforeEach
    void setUp() {
        // Start WireMock server on random port
        wireMockServer = new WireMockServer();
        wireMockServer.start();

        // Configure WireMock
        WireMock.configureFor("localhost", wireMockServer.port());

        // Create configuration pointing to WireMock server
        Properties testProps = new Properties();
        testProps.setProperty("noaa.metar.base.url",
                "http://localhost:" + wireMockServer.port() + "/metar/stations");
        testProps.setProperty("noaa.taf.base.url",
                "http://localhost:" + wireMockServer.port() + "/taf/stations");
        testProps.setProperty("noaa.timeout.seconds", "5");
        testProps.setProperty("noaa.retry.attempts", "2");
        testProps.setProperty("noaa.retry.delay.ms", "100");
        testConfig = new NoaaConfiguration(testProps);

        // Create client with test configuration
        client = new NoaaAviationWeatherClient(testConfig);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
        if (client != null) {
            client.close();
        }
    }

    // ===== Station Code Validation Tests =====

    @Test
    void testValidStationCodes() {
        assertTrue(client.isValidStationCode("KJFK"));
        assertTrue(client.isValidStationCode("KLGA"));
        assertTrue(client.isValidStationCode("EGLL")); // 4 letters
        assertTrue(client.isValidStationCode("LFP")); // 3 letters
        assertTrue(client.isValidStationCode("kjfk")); // lowercase
        assertTrue(client.isValidStationCode("  KJFK  ")); // with whitespace
    }

    @Test
    void testInvalidStationCodes() {
        assertFalse(client.isValidStationCode(null));
        assertFalse(client.isValidStationCode(""));
        assertFalse(client.isValidStationCode("  "));
        assertFalse(client.isValidStationCode("K1FK")); // contains number
        assertFalse(client.isValidStationCode("KJ")); // too short
        assertFalse(client.isValidStationCode("KJFK1")); // too long (5 chars)
        assertFalse(client.isValidStationCode("KJ-FK")); // special character
        assertFalse(client.isValidStationCode("12345")); // all numbers
    }

    @Test
    void testInvalidStationCode_ThrowsException() {
        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> client.fetchMetarReport("INVALID123"));

        assertEquals(ErrorType.INVALID_STATION_CODE, exception.getErrorType());
        assertTrue(exception.getMessage().contains("3-4 alphabetic characters"));
    }

    // ===== METAR Fetching Tests =====

    @Test
    void testFetchMetarReport_Success() throws WeatherServiceException {
        // Mock NOAA TG FTP response (raw text format)
        String mockResponse = """
                2025/01/11 14:56
                KCLT 111456Z 27008KT 10SM FEW250 06/M07 A3034 RMK AO2 SLP278 T00561072
                """;

        stubFor(get(urlEqualTo("/metar/stations/KCLT.TXT"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody(mockResponse)));

        // Execute
        WeatherData result = client.fetchMetarReport("KCLT");

        // Verify request
        verify(getRequestedFor(urlEqualTo("/metar/stations/KCLT.TXT")));

        // Verify response
        assertNotNull(result);
        assertEquals(WeatherDataSource.NOAA, result.getSource());
        assertEquals("KCLT", result.getStationId());
        assertEquals("METAR", result.getDataType());
        assertNotNull(result.getRawData());
        assertTrue(result.getRawData().contains("KCLT"));
        assertTrue(result.getRawData().contains("27008KT"));
    }

    @Test
    void testFetchMetarReport_NoData() throws WeatherServiceException {
        // Empty response
        stubFor(get(urlEqualTo("/metar/stations/KJFK.TXT"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        WeatherData result = client.fetchMetarReport("KJFK");

        assertNull(result, "Should return null when no data available");
    }

    @Test
    void testFetchMetarReport_404NotFound() throws WeatherServiceException {
        // Station file doesn't exist - should return null, not throw exception
        stubFor(get(urlEqualTo("/metar/stations/XXXX.TXT"))
                .willReturn(aResponse()
                        .withStatus(404)));

        // Act
        WeatherData result = client.fetchMetarReport("XXXX");

        // Assert
        assertNull(result, "Should return null when station file not found");
    }

    @Test
    void testFetchMetarReport_LowercaseConversion() throws WeatherServiceException {
        String mockResponse = "2025/01/11 14:56\nKJFK 111456Z 28016KT";

        stubFor(get(urlEqualTo("/metar/stations/KJFK.TXT"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // Pass lowercase, should convert to uppercase
        WeatherData result = client.fetchMetarReport("kjfk");

        verify(getRequestedFor(urlEqualTo("/metar/stations/KJFK.TXT")));
        assertNotNull(result);
    }

    @Test
    void testFetchMetarReports_MultipleStations() throws WeatherServiceException {
        // Mock responses for multiple stations
        stubFor(get(urlEqualTo("/metar/stations/KJFK.TXT"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("2025/01/11 14:56\nKJFK 111456Z")));

        stubFor(get(urlEqualTo("/metar/stations/KLGA.TXT"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("2025/01/11 14:56\nKLGA 111456Z")));

        stubFor(get(urlEqualTo("/metar/stations/KEWR.TXT"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("2025/01/11 14:56\nKEWR 111456Z")));

        List<WeatherData> results = client.fetchMetarReports("KJFK", "KLGA", "KEWR");

        // Each station requested separately
        verify(getRequestedFor(urlEqualTo("/metar/stations/KJFK.TXT")));
        verify(getRequestedFor(urlEqualTo("/metar/stations/KLGA.TXT")));
        verify(getRequestedFor(urlEqualTo("/metar/stations/KEWR.TXT")));

        assertNotNull(results);
        assertEquals(3, results.size());
    }

    @Test
    void testFetchMetarReports_PartialFailure() throws WeatherServiceException {
        // First station succeeds
        stubFor(get(urlEqualTo("/metar/stations/KJFK.TXT"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("2025/01/11 14:56\nKJFK 111456Z")));

        // Second station fails
        stubFor(get(urlEqualTo("/metar/stations/XXXX.TXT"))
                .willReturn(aResponse()
                        .withStatus(404)));

        // Should return data for successful station only
        List<WeatherData> results = client.fetchMetarReports("KJFK", "XXXX");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("KJFK", results.get(0).getStationId());
    }

    @Test
    void testFetchMetarReports_NullStationIds() {
        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> client.fetchMetarReports());

        assertEquals(ErrorType.INVALID_STATION_CODE, exception.getErrorType());
        assertTrue(exception.getMessage().contains("At least one station"));
    }

    @Test
    void testFetchMetarReports_EmptyArray() {
        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> client.fetchMetarReports());

        assertEquals(ErrorType.INVALID_STATION_CODE, exception.getErrorType());
    }

    @Test
    void testFetchMetarReports_InvalidStationInList() {
        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> client.fetchMetarReports("KJFK", "INVALID123", "KLGA"));

        assertEquals(ErrorType.INVALID_STATION_CODE, exception.getErrorType());
    }

    // ===== TAF Fetching Tests =====

    @Test
    void testFetchTafReport_Success() throws WeatherServiceException {
        String mockResponse = """
                2025/01/11 11:25
                TAF KBUF 111125Z 1112/1212 31012G20KT P6SM BKN030
                     FM111900 30015G25KT P6SM BKN020
                """;

        stubFor(get(urlEqualTo("/taf/stations/KBUF.TXT"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        WeatherData result = client.fetchTafReport("KBUF");

        verify(getRequestedFor(urlEqualTo("/taf/stations/KBUF.TXT")));

        assertNotNull(result);
        assertEquals(WeatherDataSource.NOAA, result.getSource());
        assertEquals("KBUF", result.getStationId());
        assertEquals("TAF", result.getDataType());
        assertTrue(result.getRawData().contains("TAF KBUF"));
    }

    @Test
    void testFetchTafReport_NoData() throws WeatherServiceException {
        stubFor(get(urlEqualTo("/taf/stations/KJFK.TXT"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        WeatherData result = client.fetchTafReport("KJFK");

        assertNull(result, "Should return null when no TAF data available");
    }

    @Test
    void testFetchTafReport_InvalidStation() {
        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> client.fetchTafReport("123"));

        assertEquals(ErrorType.INVALID_STATION_CODE, exception.getErrorType());
    }

    @Test
    void testFetchTafReports_MultipleStations() throws WeatherServiceException {
        stubFor(get(urlMatching("/taf/stations/.*.TXT"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("2025/01/11 11:25\nTAF TEST")));

        List<WeatherData> results = client.fetchTafReports("KJFK", "KLGA");

        assertNotNull(results);
        assertEquals(2, results.size());
    }

    // ===== Error Handling Tests =====

    @Test
    void testNetworkError_500() {
        stubFor(get(urlEqualTo("/metar/stations/KJFK.TXT"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> client.fetchMetarReport("KJFK"));

        assertEquals(ErrorType.NETWORK_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Failed to fetch METAR"));
    }

    @Test
    void testTimeout() {
        // Delay exceeds configured timeout (5 seconds)
        stubFor(get(urlEqualTo("/metar/stations/KJFK.TXT"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("data")
                        .withFixedDelay(6000)));

        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> client.fetchMetarReport("KJFK"));

        assertTrue(
                exception.getErrorType() == ErrorType.TIMEOUT ||
                        exception.getErrorType() == ErrorType.NETWORK_ERROR,
                "Should be TIMEOUT or NETWORK_ERROR"
        );
    }

    // ===== Retry Logic Tests =====

    @Test
    void testRetryOnFailure_EventualSuccess() throws WeatherServiceException {
        // First attempt fails, second succeeds
        stubFor(get(urlEqualTo("/metar/stations/KJFK.TXT"))
                .inScenario("Retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("First Attempt Failed"));

        stubFor(get(urlEqualTo("/metar/stations/KJFK.TXT"))
                .inScenario("Retry")
                .whenScenarioStateIs("First Attempt Failed")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("2025/01/11 14:56\nKJFK 111456Z")));

        WeatherData result = client.fetchMetarReport("KJFK");

        assertNotNull(result, "Should succeed after retry");
        verify(2, getRequestedFor(urlEqualTo("/metar/stations/KJFK.TXT")));
    }

    // ===== Custom HttpClient Tests =====

    @Test
    void testCustomHttpClient() {
        HttpClient customClient = HttpClient.newBuilder().build();
        NoaaAviationWeatherClient clientWithCustomHttp =
                new NoaaAviationWeatherClient(customClient, testConfig);

        assertNotNull(clientWithCustomHttp);
        clientWithCustomHttp.close();
    }

    // ===== Integration Tests =====

    @Test
    void testComplexMetarFormat() throws WeatherServiceException {
        String complexMetar = """
                2025/01/11 14:56
                METAR KJFK 251651Z 28016G28KT 240V310 3/4SM R04R/2200V6000FT
                +TSRA BR FEW015CB SCT025 BKN035 OVC250 22/21 A2990 RMK
                AO2 PK WND 27045/1655 WSHFT 1643 PRESRR RAB25 TSB32 SLP095
                FRQ LTGICCG OHD TS OHD MOV E CB DSNT N AND W P0035 T02220206
                """;

        stubFor(get(urlEqualTo("/metar/stations/KJFK.TXT"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(complexMetar)));

        WeatherData result = client.fetchMetarReport("KJFK");

        assertNotNull(result);
        assertTrue(result.getRawData().contains("+TSRA"));
        assertTrue(result.getRawData().contains("SLP095"));
    }

    @Test
    void testMetarWithMultipleLines() throws WeatherServiceException {
        String multiLineMetar = """
                2025/01/11 14:56
                METAR KJFK 251651Z 28016KT 10SM FEW250 22/12 A3015
                     RMK AO2 SLP210 T02220117
                """;

        stubFor(get(urlEqualTo("/metar/stations/KJFK.TXT"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(multiLineMetar)));

        WeatherData result = client.fetchMetarReport("KJFK");

        assertNotNull(result);
        assertTrue(result.getRawData().contains("RMK"));
    }
}
