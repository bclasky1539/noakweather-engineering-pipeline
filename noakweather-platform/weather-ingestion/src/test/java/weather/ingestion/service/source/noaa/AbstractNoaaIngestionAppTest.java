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

import weather.ingestion.service.S3UploadService;
import weather.model.NoaaWeatherData;
import weather.exception.ErrorType;
import weather.exception.WeatherServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AbstractNoaaIngestionApp.
 * <p>
 * Tests the abstract base class using a concrete test implementation.
 *
 * @author bclasky1539
 *
 */
@ExtendWith(MockitoExtension.class)
class AbstractNoaaIngestionAppTest {

    @Mock
    private AbstractNoaaIngestionOrchestrator mockOrchestrator;

    private TestNoaaIngestionApp app;
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        app = new TestNoaaIngestionApp();

        // Capture System.out and System.err
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // Restore original streams
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    // ===== Test Implementation of Abstract Class =====

    /**
     * Concrete implementation for testing purposes.
     */
    private class TestNoaaIngestionApp extends AbstractNoaaIngestionApp {

        private final AbstractNoaaIngestionOrchestrator orchestratorToReturn = mockOrchestrator;

        @Override
        protected AbstractNoaaIngestionOrchestrator createOrchestrator(
                NoaaAviationWeatherClient noaaClient,
                S3UploadService s3Service,
                int maxConcurrentFetches) {
            return orchestratorToReturn;
        }

        @Override
        protected String getDataType() {
            return "TEST";
        }

        @Override
        protected String getAdditionalUsageNotes() {
            return "Test-specific usage notes";
        }
    }

    // ===== Usage/Help Tests =====

    @Test
    void testPrintUsage_NoArguments() {
        // Act
        app.run(new String[]{});

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("TEST Ingestion Application"));
        assertTrue(output.contains("Usage:"));
        assertTrue(output.contains("Examples:"));
        assertTrue(output.contains("Environment Variables:"));
        assertTrue(output.contains("Test-specific usage notes"));
        assertTrue(output.contains("--interactive"));
        assertTrue(output.contains("--schedule"));
    }

    @Test
    void testPrintUsage_ContainsCorrectClassName() {
        // Act
        app.run(new String[]{});

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("TestNoaaIngestionApp"));
    }

    // ===== Batch Mode Tests =====

    @Test
    void testBatchMode_Success() {
        // Arrange
        String[] args = {"KJFK", "KLGA"};

        AbstractNoaaIngestionOrchestrator.IngestionResult mockResult =
                createMockIngestionResult(2, 0);

        when(mockOrchestrator.isHealthy()).thenReturn(true);
        when(mockOrchestrator.ingestStationsSequential(anyList())).thenReturn(mockResult);

        // Act
        app.run(args);

        // Assert
        verify(mockOrchestrator).ingestStationsSequential(Arrays.asList("KJFK", "KLGA"));
        verify(mockOrchestrator).shutdown();

        String output = outputStream.toString();
        assertTrue(output.contains("Ingesting TEST data for 2 stations"));
        assertTrue(output.contains("Stations: [KJFK, KLGA]"));
        assertTrue(output.contains("Ingestion Results"));
        assertTrue(output.contains("Successful: 2"));
    }

    @Test
    void testBatchMode_WithFailures() {
        // Arrange
        String[] args = {"KJFK", "INVALID"};

        AbstractNoaaIngestionOrchestrator.IngestionResult mockResult =
                createMockIngestionResult(1, 1);

        when(mockOrchestrator.isHealthy()).thenReturn(true);
        when(mockOrchestrator.ingestStationsSequential(anyList())).thenReturn(mockResult);

        // Act
        app.run(args);

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Successful: 1"));
        assertTrue(output.contains("Failed: 1"));
        assertTrue(output.contains("Failed stations:"));
    }

    @Test
    void testBatchMode_HealthCheckFails() {
        // Arrange
        String[] args = {"KJFK"};
        when(mockOrchestrator.isHealthy()).thenReturn(false);

        // Act
        app.run(args);

        // Assert
        verify(mockOrchestrator, never()).ingestStationsSequential(anyList());

        String errorOutput = errorStream.toString();
        assertTrue(errorOutput.contains("Cannot access S3 bucket"));
    }

    // ===== Scheduled Mode Tests =====

    @Test
    void testScheduledMode_MissingArguments() {
        // Arrange
        String[] args = {"--schedule"};
        when(mockOrchestrator.isHealthy()).thenReturn(true);

        // Act
        app.run(args);

        // Assert
        String errorOutput = errorStream.toString();
        assertTrue(errorOutput.contains("requires interval (minutes) and station IDs"));

        verify(mockOrchestrator, never()).schedulePeriodicIngestion(anyList(), anyInt());
    }

    @Test
    void testScheduledMode_InvalidInterval() {
        // Arrange
        String[] args = {"--schedule", "invalid", "KJFK"};
        when(mockOrchestrator.isHealthy()).thenReturn(true);

        // Act
        app.run(args);

        // Assert
        String errorOutput = errorStream.toString();
        assertTrue(errorOutput.contains("Invalid interval"));

        verify(mockOrchestrator, never()).schedulePeriodicIngestion(anyList(), anyInt());
    }

    // Note: Testing actual scheduled mode execution is complex due to Thread.sleep(Long.MAX_VALUE)
    // In practice, this would require more sophisticated testing with interrupts

    // ===== Interactive Mode Tests =====

    @Test
    void testInteractiveMode_QuitCommand() {
        // Arrange
        String input = "quit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        String[] args = {"--interactive"};
        when(mockOrchestrator.isHealthy()).thenReturn(true);

        // Act
        app.run(args);

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Interactive TEST Ingestion Mode"));
        assertTrue(output.contains("Exiting..."));
    }

    /**
     * Parameterized test for simple interactive mode commands.
     * Tests commands that only require input/output verification without complex mocking.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "Exit command,               'exit\n',              'Exiting...'",
            "Health command,             'health\nquit\n',      'System health: HEALTHY'",
            "Ingest without stations,    'ingest\nquit\n',      'Please specify station ID(s)'",
            "Unknown command,            'unknown\nquit\n',     'Unknown command: unknown'",
            "Empty input,                '\n\nquit\n',          'Interactive TEST Ingestion Mode'"
    })
    void testInteractiveMode_SimpleCommands(String testName, String input, String expectedOutput) {
        // Arrange
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        String[] args = {"--interactive"};
        when(mockOrchestrator.isHealthy()).thenReturn(true);

        // Act
        app.run(args);

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains(expectedOutput),
                "Expected output to contain: " + expectedOutput);
    }

    @Test
    void testInteractiveMode_HealthCommand_Unhealthy() {
        // Arrange
        String input = "health\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        String[] args = {"--interactive"};
        when(mockOrchestrator.isHealthy()).thenReturn(true).thenReturn(false);

        // Act
        app.run(args);

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("System health: UNHEALTHY"));
    }

    @Test
    void testInteractiveMode_MetricsCommand() {
        // Arrange
        String input = "metrics\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        Map<String, Object> mockMetrics = new HashMap<>();
        mockMetrics.put("fetch_attempts", 10);
        mockMetrics.put("fetch_successes", 8);

        String[] args = {"--interactive"};
        when(mockOrchestrator.isHealthy()).thenReturn(true);
        when(mockOrchestrator.getMetrics()).thenReturn(mockMetrics);

        // Act
        app.run(args);

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Ingestion Metrics:"));
        assertTrue(output.contains("fetch_attempts"));
        assertTrue(output.contains("10"));
    }

    @Test
    void testInteractiveMode_IngestCommand_Success() {
        // Arrange
        String input = "ingest KJFK\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        AbstractNoaaIngestionOrchestrator.IngestionResult mockResult =
                createMockIngestionResult(1, 0);

        String[] args = {"--interactive"};
        when(mockOrchestrator.isHealthy()).thenReturn(true);
        when(mockOrchestrator.ingestStationsSequential(anyList())).thenReturn(mockResult);

        // Act
        app.run(args);

        // Assert
        verify(mockOrchestrator).ingestStationsSequential(List.of("KJFK"));

        String output = outputStream.toString();
        assertTrue(output.contains("Results: 1 succeeded"));
    }

    @Test
    void testInteractiveMode_IngestCommand_WithFailures() {
        // Arrange
        String input = "ingest KJFK INVALID\nquit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        AbstractNoaaIngestionOrchestrator.IngestionResult mockResult =
                createMockIngestionResult(1, 1);

        String[] args = {"--interactive"};
        when(mockOrchestrator.isHealthy()).thenReturn(true);
        when(mockOrchestrator.ingestStationsSequential(anyList())).thenReturn(mockResult);

        // Act
        app.run(args);

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("1 succeeded, 1 failed"));
        assertTrue(output.contains("Failures:"));
    }

    // ===== Error Handling Tests =====

    // Note: Test for exception handling removed because it calls System.exit(1)
    // which terminates the test JVM. Exception handling is verified by other tests.

    // ===== Helper Methods =====

    private AbstractNoaaIngestionOrchestrator.IngestionResult createMockIngestionResult(
            int successCount, int failureCount) {

        AbstractNoaaIngestionOrchestrator.IngestionResult result =
                new AbstractNoaaIngestionOrchestrator.IngestionResult();

        // Add successful stations
        for (int i = 0; i < successCount; i++) {
            String stationId = "STATION_" + i;
            NoaaWeatherData weatherData = new NoaaWeatherData(stationId, Instant.now(), "TEST");
            weatherData.setRawData("Test data for " + stationId);
            weatherData.addMetadata("s3_key", "key_" + i);
            result.addSuccess(stationId, weatherData);
        }

        // Add failed stations
        for (int i = 0; i < failureCount; i++) {
            WeatherServiceException exception =
                    new WeatherServiceException(ErrorType.NETWORK_ERROR, "Error for FAIL_" + i);
            result.addFailure("FAIL_" + i, exception);
        }

        result.setDuration(Duration.ofMillis(100));

        return result;
    }
}
