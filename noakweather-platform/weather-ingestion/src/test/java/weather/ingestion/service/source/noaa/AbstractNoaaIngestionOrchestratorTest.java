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
import weather.ingestion.service.SpeedLayerProcessor;
import weather.model.NoaaWeatherData;
import weather.model.WeatherData;
import weather.exception.WeatherServiceException;
import weather.exception.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AbstractNoaaIngestionOrchestrator.
 * <p>
 * Tests the abstract base class using a concrete test implementation.
 *
 * @author bclasky1539
 *
 */
@ExtendWith(MockitoExtension.class)
class AbstractNoaaIngestionOrchestratorTest {

    @Mock
    private NoaaAviationWeatherClient mockNoaaClient;

    @Mock
    private S3UploadService mockS3Service;

    @Mock
    private SpeedLayerProcessor mockSpeedLayerProcessor;

    private TestNoaaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new TestNoaaOrchestrator(mockNoaaClient, mockS3Service, 5);
    }

    // ===== Test Implementation of Abstract Class =====

    /**
     * Concrete implementation for testing purposes.
     */
    private class TestNoaaOrchestrator extends AbstractNoaaIngestionOrchestrator {

        public TestNoaaOrchestrator(NoaaAviationWeatherClient noaaClient,
                                    S3UploadService s3Service,
                                    int maxConcurrentFetches) {
            super(noaaClient, s3Service, maxConcurrentFetches, "TEST");
        }

        @Override
        protected WeatherData fetchFromNoaa(String stationId) throws WeatherServiceException {
            return mockNoaaClient.fetchMetarReport(stationId);
        }
    }

    // ===== Single Station Ingestion Tests =====

    @Test
    void testIngestStation_Success() throws Exception {
        // Arrange
        String stationId = "KJFK";
        WeatherData mockData = createMockWeatherData(stationId);
        WeatherData processedData = createMockWeatherData(stationId);
        processedData.addMetadata("s3_key", "test-key");

        when(mockNoaaClient.fetchMetarReport(stationId)).thenReturn(mockData);
        when(mockSpeedLayerProcessor.processWeatherData(any(WeatherData.class)))
                .thenReturn(processedData);

        // Inject mock SpeedLayerProcessor using reflection or create accessor
        setSpeedLayerProcessor(orchestrator, mockSpeedLayerProcessor);

        // Act
        WeatherData result = orchestrator.ingestStation(stationId);

        // Assert
        assertNotNull(result);
        assertEquals(stationId, result.getStationId());
        assertTrue(result.getMetadata().containsKey("ingestion_duration_ms"));

        verify(mockNoaaClient, times(1)).fetchMetarReport(stationId);
        verify(mockSpeedLayerProcessor, times(1)).processWeatherData(any(WeatherData.class));
    }

    @Test
    void testIngestStation_NoData() throws Exception {
        // Arrange
        String stationId = "KJFK";
        when(mockNoaaClient.fetchMetarReport(stationId)).thenReturn(null);

        // Act & Assert
        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> orchestrator.ingestStation(stationId));

        assertEquals(ErrorType.NO_DATA, exception.getErrorType());
        assertTrue(exception.getMessage().contains("No TEST data available"));

        verify(mockNoaaClient, times(1)).fetchMetarReport(stationId);
        verify(mockSpeedLayerProcessor, never()).processWeatherData(any());
    }

    @Test
    void testIngestStation_FetchFailure() throws Exception {
        // Arrange
        String stationId = "KJFK";
        when(mockNoaaClient.fetchMetarReport(stationId))
                .thenThrow(new WeatherServiceException(ErrorType.NETWORK_ERROR, "Connection failed"));

        // Act & Assert
        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> orchestrator.ingestStation(stationId));

        assertEquals(ErrorType.NETWORK_ERROR, exception.getErrorType());

        verify(mockNoaaClient, times(1)).fetchMetarReport(stationId);
        verify(mockSpeedLayerProcessor, never()).processWeatherData(any());
    }

    @Test
    void testIngestStation_ValidationFailure_MissingStationId() throws Exception {
        // Arrange
        String stationId = "KJFK";
        WeatherData invalidData = createMockWeatherData(null); // null stationId

        when(mockNoaaClient.fetchMetarReport(stationId)).thenReturn(invalidData);

        // Act & Assert
        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> orchestrator.ingestStation(stationId));

        assertEquals(ErrorType.INVALID_DATA, exception.getErrorType());
        assertTrue(exception.getMessage().contains("stationId"));
    }

    @Test
    void testIngestStation_ValidationFailure_MissingRawData() throws Exception {
        // Arrange
        String stationId = "KJFK";
        WeatherData invalidData = createMockWeatherData(stationId);
        invalidData.setRawData(null); // null rawData

        when(mockNoaaClient.fetchMetarReport(stationId)).thenReturn(invalidData);

        // Act & Assert
        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> orchestrator.ingestStation(stationId));

        assertEquals(ErrorType.INVALID_DATA, exception.getErrorType());
        assertTrue(exception.getMessage().contains("rawData"));
    }

    @Test
    void testIngestStation_SpeedLayerProcessingFailure() throws Exception {
        // Arrange
        String stationId = "KJFK";
        WeatherData mockData = createMockWeatherData(stationId);

        when(mockNoaaClient.fetchMetarReport(stationId)).thenReturn(mockData);
        when(mockSpeedLayerProcessor.processWeatherData(any(WeatherData.class)))
                .thenThrow(new IOException("S3 upload failed"));

        setSpeedLayerProcessor(orchestrator, mockSpeedLayerProcessor);

        // Act & Assert
        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> orchestrator.ingestStation(stationId));

        assertEquals(ErrorType.STORAGE_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Failed to process through speed layer"));
    }

    // ===== Batch Ingestion Tests =====

    @Test
    void testIngestStationsBatch_Success() throws Exception {
        // Arrange
        List<String> stationIds = Arrays.asList("KJFK", "KLGA", "KEWR");

        // Mock to return different data based on station ID
        when(mockNoaaClient.fetchMetarReport(anyString())).thenAnswer(invocation -> {
            String stationId = invocation.getArgument(0);
            return createMockWeatherData(stationId);
        });

        when(mockSpeedLayerProcessor.processWeatherData(any(WeatherData.class))).thenAnswer(invocation -> {
            WeatherData input = invocation.getArgument(0);
            WeatherData processedData = createMockWeatherData(input.getStationId());
            processedData.addMetadata("s3_key", "key-" + input.getStationId());
            return processedData;
        });

        setSpeedLayerProcessor(orchestrator, mockSpeedLayerProcessor);

        // Act
        List<WeatherData> results = orchestrator.ingestStationsBatch(stationIds);

        // Assert
        assertEquals(3, results.size());
        verify(mockNoaaClient, times(3)).fetchMetarReport(anyString());
    }

    @Test
    void testIngestStationsBatch_PartialFailure() throws Exception {
        // Arrange
        List<String> stationIds = Arrays.asList("KJFK", "INVALID", "KLGA");

        // First and third succeed
        WeatherData mockData1 = createMockWeatherData("KJFK");
        WeatherData mockData3 = createMockWeatherData("KLGA");
        WeatherData processedData = createMockWeatherData("TEST");

        when(mockNoaaClient.fetchMetarReport("KJFK")).thenReturn(mockData1);
        when(mockNoaaClient.fetchMetarReport("INVALID"))
                .thenThrow(new WeatherServiceException(ErrorType.INVALID_STATION_CODE, "Invalid"));
        when(mockNoaaClient.fetchMetarReport("KLGA")).thenReturn(mockData3);
        when(mockSpeedLayerProcessor.processWeatherData(any(WeatherData.class)))
                .thenReturn(processedData);

        setSpeedLayerProcessor(orchestrator, mockSpeedLayerProcessor);

        // Act
        List<WeatherData> results = orchestrator.ingestStationsBatch(stationIds);

        // Assert
        assertEquals(2, results.size()); // Only successful ones
    }

    // ===== Sequential Ingestion Tests =====

    @Test
    void testIngestStationsSequential_Success() throws Exception {
        // Arrange
        List<String> stationIds = Arrays.asList("KJFK", "KLGA");

        when(mockNoaaClient.fetchMetarReport(anyString())).thenAnswer(invocation -> {
            String stationId = invocation.getArgument(0);
            return createMockWeatherData(stationId);
        });

        when(mockSpeedLayerProcessor.processWeatherData(any(WeatherData.class))).thenAnswer(invocation -> {
            WeatherData input = invocation.getArgument(0);
            return createMockWeatherData(input.getStationId());
        });

        setSpeedLayerProcessor(orchestrator, mockSpeedLayerProcessor);

        // Act
        AbstractNoaaIngestionOrchestrator.IngestionResult result =
                orchestrator.ingestStationsSequential(stationIds);

        // Assert
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(1.0, result.getSuccessRate());
        assertNotNull(result.getDuration());
        assertEquals(2, result.getSuccessfulStations().size());
        assertTrue(result.getFailures().isEmpty());
    }

    @Test
    void testIngestStationsSequential_WithFailures() throws Exception {
        // Arrange
        List<String> stationIds = Arrays.asList("KJFK", "INVALID", "KLGA");

        WeatherData mockData1 = createMockWeatherData("KJFK");
        WeatherData mockData3 = createMockWeatherData("KLGA");
        WeatherData processedData = createMockWeatherData("TEST");

        when(mockNoaaClient.fetchMetarReport("KJFK")).thenReturn(mockData1);
        when(mockNoaaClient.fetchMetarReport("INVALID"))
                .thenThrow(new WeatherServiceException(ErrorType.INVALID_STATION_CODE, "Invalid"));
        when(mockNoaaClient.fetchMetarReport("KLGA")).thenReturn(mockData3);
        when(mockSpeedLayerProcessor.processWeatherData(any(WeatherData.class)))
                .thenReturn(processedData);

        setSpeedLayerProcessor(orchestrator, mockSpeedLayerProcessor);

        // Act
        AbstractNoaaIngestionOrchestrator.IngestionResult result =
                orchestrator.ingestStationsSequential(stationIds);

        // Assert
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(0.666, result.getSuccessRate(), 0.01);
        assertEquals(1, result.getFailures().size());
        assertTrue(result.getFailures().containsKey("INVALID"));
    }

    // ===== Scheduled Periodic Ingestion Tests =====

    @Test
    void testSchedulePeriodicIngestion_StartsSuccessfully() {
        // Arrange
        List<String> stationIds = List.of("KJFK", "KLGA");
        int intervalMinutes = 10;

        // Do not set up mocks - we're just testing that the future is created
        // The task will not execute because we cancel it immediately

        // Act
        java.util.concurrent.ScheduledFuture<?> future =
                orchestrator.schedulePeriodicIngestion(stationIds, intervalMinutes);

        // Assert
        assertNotNull(future, "ScheduledFuture should not be null");
        assertFalse(future.isDone(), "Task should not be done immediately");
        assertFalse(future.isCancelled(), "Task should not be cancelled");

        // Cleanup - cancel before first execution to avoid unnecessary mock setup
        future.cancel(true);
    }

    @Test
    void testSchedulePeriodicIngestion_CallsIngestStationsBatch() throws Exception {
        // Arrange
        List<String> stationIds = List.of("KJFK");
        int intervalMinutes = 1;

        // Create a spy to verify method calls
        TestNoaaOrchestrator spyOrchestrator = spy(orchestrator);

        when(mockNoaaClient.fetchMetarReport(anyString())).thenAnswer(invocation -> {
            String stationId = invocation.getArgument(0);
            return createMockWeatherData(stationId);
        });

        when(mockSpeedLayerProcessor.processWeatherData(any(WeatherData.class))).thenAnswer(invocation -> {
            WeatherData input = invocation.getArgument(0);
            return createMockWeatherData(input.getStationId());
        });

        setSpeedLayerProcessor(spyOrchestrator, mockSpeedLayerProcessor);

        // Act
        java.util.concurrent.ScheduledFuture<?> future =
                spyOrchestrator.schedulePeriodicIngestion(stationIds, intervalMinutes);

        // The task executes immediately (initialDelay = 0), so we can verify synchronously
        // Wait for the future to complete its first run using timeout
        verify(spyOrchestrator, timeout(2000).atLeastOnce()).ingestStationsBatch(stationIds);

        // Cleanup
        future.cancel(true);
    }

    @Test
    void testSchedulePeriodicIngestion_HandlesExceptions() {
        // Arrange
        List<String> stationIds = List.of("KJFK");
        int intervalMinutes = 1;

        // Make ingestStationsBatch throw exception
        TestNoaaOrchestrator spyOrchestrator = spy(orchestrator);
        doThrow(new RuntimeException("Test error"))
                .when(spyOrchestrator).ingestStationsBatch(stationIds);

        setSpeedLayerProcessor(spyOrchestrator, mockSpeedLayerProcessor);

        // Act
        java.util.concurrent.ScheduledFuture<?> future =
                spyOrchestrator.schedulePeriodicIngestion(stationIds, intervalMinutes);

        // Assert - verify the method was called despite throwing exception
        // The exception should be caught and logged, not propagate
        verify(spyOrchestrator, timeout(2000).atLeastOnce()).ingestStationsBatch(stationIds);

        // Task should continue running (not done/cancelled due to exception)
        assertFalse(future.isDone(), "Task should continue running after exception");
        assertFalse(future.isCancelled(), "Task should not be cancelled due to exception");

        // Cleanup
        future.cancel(true);
    }

    @Test
    void testSchedulePeriodicIngestion_CanBeCancelled() {
        // Arrange
        List<String> stationIds = List.of("KJFK", "KLGA");
        int intervalMinutes = 10;

        // Do not set up mocks - we are testing cancellation, not execution

        // Act
        java.util.concurrent.ScheduledFuture<?> future =
                orchestrator.schedulePeriodicIngestion(stationIds, intervalMinutes);

        boolean cancelled = future.cancel(true);

        // Assert
        assertTrue(cancelled, "Future should be cancellable");
        assertTrue(future.isCancelled(), "Future should report as cancelled");
    }

    @Test
    void testSchedulePeriodicIngestion_MultipleStations() throws Exception {
        // Arrange
        List<String> stationIds = List.of("KJFK", "KLGA", "KEWR");
        int intervalMinutes = 1;

        TestNoaaOrchestrator spyOrchestrator = spy(orchestrator);

        when(mockNoaaClient.fetchMetarReport(anyString())).thenAnswer(invocation -> {
            String stationId = invocation.getArgument(0);
            return createMockWeatherData(stationId);
        });

        when(mockSpeedLayerProcessor.processWeatherData(any(WeatherData.class))).thenAnswer(invocation -> {
            WeatherData input = invocation.getArgument(0);
            return createMockWeatherData(input.getStationId());
        });

        setSpeedLayerProcessor(spyOrchestrator, mockSpeedLayerProcessor);

        // Act
        java.util.concurrent.ScheduledFuture<?> future =
                spyOrchestrator.schedulePeriodicIngestion(stationIds, intervalMinutes);

        // Assert - verify ingestStationsBatch was called with all stations
        verify(spyOrchestrator, timeout(2000).atLeastOnce()).ingestStationsBatch(stationIds);

        // Cleanup
        future.cancel(true);
    }

    // ===== IngestionResult Tests =====

    @Test
    void testIngestionResult_EmptyResult() {
        // Arrange
        AbstractNoaaIngestionOrchestrator.IngestionResult result =
                new AbstractNoaaIngestionOrchestrator.IngestionResult();
        result.setDuration(Duration.ofMillis(100));

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(0.0, result.getSuccessRate());
        assertTrue(result.getSuccessfulStations().isEmpty());
        assertTrue(result.getFailures().isEmpty());
    }

    @Test
    void testIngestionResult_ToString() {
        // Arrange
        AbstractNoaaIngestionOrchestrator.IngestionResult result =
                new AbstractNoaaIngestionOrchestrator.IngestionResult();
        result.addSuccess("KJFK", createMockWeatherData("KJFK"));
        result.addFailure("INVALID",
                new WeatherServiceException(ErrorType.INVALID_STATION_CODE, "Invalid"));
        result.setDuration(Duration.ofMillis(500));

        // Act
        String resultString = result.toString();

        // Assert
        assertTrue(resultString.contains("success=1"));
        assertTrue(resultString.contains("failed=1"));
        assertTrue(resultString.contains("rate=50.00%"));
        assertTrue(resultString.contains("duration=500ms"));
    }

    // ===== Metrics Tests =====

    @Test
    void testGetMetrics_InitialState() {
        // Act
        Map<String, Object> metrics = orchestrator.getMetrics();

        // Assert
        assertNotNull(metrics);
        assertEquals(0L, metrics.get("fetch_attempts"));
        assertEquals(0L, metrics.get("fetch_successes"));
        assertEquals(0L, metrics.get("fetch_failures"));
        assertEquals(0L, metrics.get("no_data_count"));
        assertEquals(0.0, metrics.get("fetch_success_rate"));
    }

    @Test
    void testGetMetrics_AfterSuccessfulIngestion() throws Exception {
        // Arrange
        WeatherData mockData = createMockWeatherData("KJFK");
        WeatherData processedData = createMockWeatherData("KJFK");

        when(mockNoaaClient.fetchMetarReport("KJFK")).thenReturn(mockData);
        when(mockSpeedLayerProcessor.processWeatherData(any(WeatherData.class)))
                .thenReturn(processedData);

        setSpeedLayerProcessor(orchestrator, mockSpeedLayerProcessor);

        // Act
        orchestrator.ingestStation("KJFK");
        Map<String, Object> metrics = orchestrator.getMetrics();

        // Assert
        assertEquals(1L, metrics.get("fetch_attempts"));
        assertEquals(1L, metrics.get("fetch_successes"));
        assertEquals(1L, metrics.get("upload_successes"));
        assertEquals(1.0, metrics.get("fetch_success_rate"));
    }

    // ===== Health Check Tests =====

    @Test
    void testIsHealthy_SpeedLayerHealthy() {
        // Arrange
        setSpeedLayerProcessor(orchestrator, mockSpeedLayerProcessor);
        when(mockSpeedLayerProcessor.isHealthy()).thenReturn(true);

        // Act & Assert
        assertTrue(orchestrator.isHealthy());
    }

    @Test
    void testIsHealthy_SpeedLayerUnhealthy() {
        // Arrange
        setSpeedLayerProcessor(orchestrator, mockSpeedLayerProcessor);
        when(mockSpeedLayerProcessor.isHealthy()).thenReturn(false);

        // Act & Assert
        assertFalse(orchestrator.isHealthy());
    }

    // ===== Validation Tests =====

    @ParameterizedTest(name = "Missing field validation")
    @CsvSource({
            "null,           'test raw data'",
            "'KJFK',         ''"
    })
    void testValidation_MissingFields(String stationId, String rawData)
            throws WeatherServiceException {
        // Arrange
        WeatherData invalidData;

        if ("null".equals(stationId)) {
            // Create data with null stationId using reflection
            invalidData = createMockWeatherData("TEMP");
            setStationIdToNull(invalidData);
        } else {
            invalidData = createMockWeatherData(stationId);
        }

        invalidData.setRawData(rawData);

        when(mockNoaaClient.fetchMetarReport(anyString())).thenReturn(invalidData);

        // Act & Assert
        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> orchestrator.ingestStation("TEST"));

        assertEquals(ErrorType.INVALID_DATA, exception.getErrorType());
    }


    // ===== Shutdown Tests =====

    @Test
    void testShutdown() {
        // Act & Assert - should complete without throwing
        assertDoesNotThrow(() -> orchestrator.shutdown());
    }

    // ===== Helper Methods =====

    private WeatherData createMockWeatherData(String stationId) {
        NoaaWeatherData data = new NoaaWeatherData(stationId, Instant.now(), "TEST");
        data.setRawData("Test raw data for " + stationId);
        return data;
    }

    /**
     * Helper to inject mock SpeedLayerProcessor using reflection.
     * In real scenario, consider using constructor injection or package-private setter.
     */
    private void setSpeedLayerProcessor(AbstractNoaaIngestionOrchestrator orchestrator,
                                        SpeedLayerProcessor processor) {
        try {
            java.lang.reflect.Field field =
                    AbstractNoaaIngestionOrchestrator.class.getDeclaredField("speedLayerProcessor");
            field.setAccessible(true);
            field.set(orchestrator, processor);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject SpeedLayerProcessor", e);
        }
    }

    /**
     * Helper to set stationId to null via reflection (for testing validation).
     */
    private void setStationIdToNull(WeatherData data) {
        try {
            java.lang.reflect.Field field = data.getClass().getSuperclass().getDeclaredField("stationId");
            field.setAccessible(true);
            field.set(data, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set stationId to null", e);
        }
    }
}
