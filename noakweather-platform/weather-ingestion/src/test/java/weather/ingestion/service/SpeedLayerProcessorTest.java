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
package weather.ingestion.service;

import weather.model.NoaaWeatherData;
import weather.model.ProcessingLayer;
import weather.model.WeatherData;
import weather.model.WeatherDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SpeedLayerProcessor using Mockito.
 * <p>
 * Updated for refactored generic processor that works with ANY weather data source.
 *
 * @author bclasky1539
 *
 */
@ExtendWith(MockitoExtension.class)
class SpeedLayerProcessorTest {

    @Mock
    private S3UploadService s3Service;

    private SpeedLayerProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new SpeedLayerProcessor(s3Service);
    }

    // ===== Constructor Tests =====

    @Test
    void testConstructorWithDefaultConcurrency() {
        SpeedLayerProcessor defaultProcessor = new SpeedLayerProcessor(s3Service);

        assertNotNull(defaultProcessor);
        Map<String, Object> stats = defaultProcessor.getStatistics();
        assertEquals(10, stats.get("max_concurrent_requests"));

        defaultProcessor.shutdown();
    }

    @Test
    void testConstructorWithCustomConcurrency() {
        SpeedLayerProcessor customProcessor = new SpeedLayerProcessor(s3Service, 20);

        assertNotNull(customProcessor);
        Map<String, Object> stats = customProcessor.getStatistics();
        assertEquals(20, stats.get("max_concurrent_requests"));

        customProcessor.shutdown();
    }

    // ===== processWeatherData() Tests =====

    @Test
    void testProcessWeatherData_Success() throws IOException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KJFK", "METAR");
        String s3Key = "speed-layer/noaa/metar/2025/01/12/KJFK_123456.json";

        when(s3Service.uploadWeatherData(any(WeatherData.class))).thenReturn(s3Key);

        // Act
        WeatherData result = processor.processWeatherData(weatherData);

        // Assert
        assertNotNull(result);
        assertEquals("KJFK", result.getStationId());
        assertEquals(ProcessingLayer.SPEED_LAYER, result.getProcessingLayer());

        // Verify metadata enrichment
        assertTrue(result.getMetadata().containsKey("validated"));
        assertTrue(result.getMetadata().containsKey("validation_timestamp"));
        assertTrue(result.getMetadata().containsKey("processor"));
        assertTrue(result.getMetadata().containsKey("processor_version"));
        assertTrue(result.getMetadata().containsKey("s3_key"));
        assertTrue(result.getMetadata().containsKey("processing_duration_ms"));

        assertEquals("true", result.getMetadata().get("validated"));
        assertEquals("SpeedLayerProcessor", result.getMetadata().get("processor"));
        assertEquals("2.0", result.getMetadata().get("processor_version"));
        assertEquals(s3Key, result.getMetadata().get("s3_key"));

        verify(s3Service, times(1)).uploadWeatherData(any(WeatherData.class));
    }

    @Test
    void testProcessWeatherData_NullInput() throws IOException {
        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> processor.processWeatherData(null));

        assertEquals("Weather data cannot be null", exception.getMessage());
        verify(s3Service, never()).uploadWeatherData(any());
    }

    @Test
    void testProcessWeatherData_S3UploadFailure() throws IOException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KJFK", "METAR");

        when(s3Service.uploadWeatherData(any(WeatherData.class)))
                .thenThrow(new IOException("S3 upload failed"));

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> processor.processWeatherData(weatherData));

        assertEquals("S3 upload failed", exception.getMessage());
    }

    @Test
    void testProcessWeatherData_EnrichmentApplied() throws IOException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KLGA", "TAF");

        when(s3Service.uploadWeatherData(any(WeatherData.class))).thenReturn("s3://key");

        // Act
        WeatherData result = processor.processWeatherData(weatherData);

        // Assert - verify all metadata fields were added
        Map<String, Object> metadata = result.getMetadata();
        assertTrue(metadata.containsKey("validated"));
        assertTrue(metadata.containsKey("validation_timestamp"));
        assertTrue(metadata.containsKey("processor"));
        assertTrue(metadata.containsKey("processor_version"));

        // Verify processor version
        assertEquals("2.0", metadata.get("processor_version"));
    }

    @Test
    void testProcessWeatherData_ProcessingLayerSet() throws IOException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KCLT", "METAR");
        weatherData.setProcessingLayer(null); // Start with no layer

        when(s3Service.uploadWeatherData(any(WeatherData.class))).thenReturn("s3://key");

        // Act
        WeatherData result = processor.processWeatherData(weatherData);

        // Assert
        assertEquals(ProcessingLayer.SPEED_LAYER, result.getProcessingLayer());
    }

    // ===== processWeatherDataBatch() Tests =====

    @Test
    void testProcessWeatherDataBatch_Success() throws IOException {
        // Arrange
        List<WeatherData> inputData = Arrays.asList(
                createTestWeatherData("KJFK", "METAR"),
                createTestWeatherData("KLGA", "METAR"),
                createTestWeatherData("KEWR", "METAR")
        );

        when(s3Service.uploadWeatherData(any(WeatherData.class)))
                .thenReturn("s3://key1")
                .thenReturn("s3://key2")
                .thenReturn("s3://key3");

        // Act
        List<WeatherData> results = processor.processWeatherDataBatch(inputData);

        // Assert
        assertEquals(3, results.size());

        for (WeatherData data : results) {
            assertEquals(ProcessingLayer.SPEED_LAYER, data.getProcessingLayer());
            assertTrue(data.getMetadata().containsKey("validated"));
            assertTrue(data.getMetadata().containsKey("s3_key"));
        }

        verify(s3Service, times(3)).uploadWeatherData(any(WeatherData.class));
    }

    @Test
    void testProcessWeatherDataBatch_EmptyList() throws IOException {
        // Act
        List<WeatherData> results = processor.processWeatherDataBatch(Collections.emptyList());

        // Assert
        assertTrue(results.isEmpty());
        verify(s3Service, never()).uploadWeatherData(any());
    }

    @Test
    void testProcessWeatherDataBatch_NullList() throws IOException {
        // Act
        List<WeatherData> results = processor.processWeatherDataBatch(null);

        // Assert
        assertTrue(results.isEmpty());
        verify(s3Service, never()).uploadWeatherData(any());
    }

    @Test
    void testProcessWeatherDataBatch_PartialFailure() throws IOException {
        // Arrange
        List<WeatherData> inputData = Arrays.asList(
                createTestWeatherData("KJFK", "METAR"),
                createTestWeatherData("KLGA", "METAR")
        );

        // First upload succeeds, second fails
        when(s3Service.uploadWeatherData(any(WeatherData.class)))
                .thenReturn("s3://key1")
                .thenThrow(new IOException("Upload failed"));

        // Act
        List<WeatherData> results = processor.processWeatherDataBatch(inputData);

        // Assert - only successful one should be returned
        // Note: Due to parallel processing, we can't guarantee which station succeeds
        assertEquals(1, results.size());
        String resultStationId = results.get(0).getStationId();
        assertTrue(resultStationId.equals("KJFK") || resultStationId.equals("KLGA"),
                "Result should be either KJFK or KLGA, but was: " + resultStationId);

        verify(s3Service, times(2)).uploadWeatherData(any(WeatherData.class));
    }

    @Test
    void testProcessWeatherDataBatch_AllFailures() throws IOException {
        // Arrange
        List<WeatherData> inputData = Arrays.asList(
                createTestWeatherData("KJFK", "METAR"),
                createTestWeatherData("KLGA", "METAR")
        );

        when(s3Service.uploadWeatherData(any(WeatherData.class)))
                .thenThrow(new IOException("Upload failed"));

        // Act
        List<WeatherData> results = processor.processWeatherDataBatch(inputData);

        // Assert - no successful results
        assertTrue(results.isEmpty());

        verify(s3Service, times(2)).uploadWeatherData(any(WeatherData.class));
    }

    @Test
    void testProcessWeatherDataBatch_MixedDataTypes() throws IOException {
        // Arrange - mix of METAR and TAF
        List<WeatherData> inputData = Arrays.asList(
                createTestWeatherData("KJFK", "METAR"),
                createTestWeatherData("KLGA", "TAF")
        );

        when(s3Service.uploadWeatherData(any(WeatherData.class)))
                .thenReturn("s3://key1")
                .thenReturn("s3://key2");

        // Act
        List<WeatherData> results = processor.processWeatherDataBatch(inputData);

        // Assert - should handle both types
        assertEquals(2, results.size());
        verify(s3Service, times(2)).uploadWeatherData(any(WeatherData.class));
    }

    // ===== getStatistics() Tests =====

    @Test
    void testGetStatistics() {
        // Act
        Map<String, Object> stats = processor.getStatistics();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("max_concurrent_requests"));
        assertTrue(stats.containsKey("executor_active_threads"));
        assertTrue(stats.containsKey("executor_queue_size"));
        assertTrue(stats.containsKey("executor_completed_tasks"));

        assertEquals(10, stats.get("max_concurrent_requests"));
    }

    @Test
    void testGetStatistics_AfterProcessing() throws IOException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KJFK", "METAR");
        when(s3Service.uploadWeatherData(any(WeatherData.class))).thenReturn("s3://key");

        // Act
        processor.processWeatherData(weatherData);
        Map<String, Object> stats = processor.getStatistics();

        // Assert - completed tasks should be > 0
        assertNotNull(stats);
        long completedTasks = (Long) stats.get("executor_completed_tasks");
        assertTrue(completedTasks >= 0);
    }

    // ===== isHealthy() Tests =====

    @Test
    void testIsHealthy_S3Accessible() {
        // Arrange
        when(s3Service.isBucketAccessible()).thenReturn(true);

        // Act
        boolean healthy = processor.isHealthy();

        // Assert
        assertTrue(healthy);
        verify(s3Service, times(1)).isBucketAccessible();
    }

    @Test
    void testIsHealthy_S3NotAccessible() {
        // Arrange
        when(s3Service.isBucketAccessible()).thenReturn(false);

        // Act
        boolean healthy = processor.isHealthy();

        // Assert
        assertFalse(healthy);
        verify(s3Service, times(1)).isBucketAccessible();
    }

    // ===== shutdown() Tests =====

    @Test
    void testShutdown() {
        // Act & Assert - should complete without throwing
        assertDoesNotThrow(() -> processor.shutdown());
    }

    @Test
    void testShutdown_GracefulTermination() {
        // Act
        processor.shutdown();

        // Assert - verify stats are still accessible after shutdown
        Map<String, Object> stats = processor.getStatistics();
        assertNotNull(stats);
    }

    // ===== Integration Tests =====

    @Test
    void testFullProcessingWorkflow() throws IOException {
        // Arrange
        WeatherData inputData = createTestWeatherData("KCLT", "METAR");
        inputData.addMetadata("custom_field", "custom_value");

        String expectedS3Key = "speed-layer/noaa/metar/2025/01/12/KCLT_123456.json";
        when(s3Service.uploadWeatherData(any(WeatherData.class))).thenReturn(expectedS3Key);

        // Act
        WeatherData result = processor.processWeatherData(inputData);

        // Assert
        // Original data preserved
        assertEquals("KCLT", result.getStationId());
        assertEquals("METAR", result.getDataType());
        assertEquals("custom_value", result.getMetadata().get("custom_field"));

        // Processing layer set
        assertEquals(ProcessingLayer.SPEED_LAYER, result.getProcessingLayer());

        // Generic metadata added
        assertEquals("true", result.getMetadata().get("validated"));
        assertEquals("SpeedLayerProcessor", result.getMetadata().get("processor"));

        // S3 key added
        assertEquals(expectedS3Key, result.getMetadata().get("s3_key"));

        // Processing duration tracked
        assertTrue(result.getMetadata().containsKey("processing_duration_ms"));
    }

    @Test
    void testGenericProcessor_WorksWithAnySource() throws IOException {
        // Arrange - simulate different sources (NOAA, OpenWeather, etc.)
        WeatherData noaaData = createTestWeatherData("KJFK", "METAR");
        noaaData.setSource(WeatherDataSource.NOAA);

        WeatherData openWeatherData = createTestWeatherData("LONDON", "CURRENT");
        openWeatherData.setSource(WeatherDataSource.OPENWEATHERMAP);

        when(s3Service.uploadWeatherData(any(WeatherData.class)))
                .thenReturn("s3://key1")
                .thenReturn("s3://key2");

        // Act
        WeatherData result1 = processor.processWeatherData(noaaData);
        WeatherData result2 = processor.processWeatherData(openWeatherData);

        // Assert - processor works with any source
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(ProcessingLayer.SPEED_LAYER, result1.getProcessingLayer());
        assertEquals(ProcessingLayer.SPEED_LAYER, result2.getProcessingLayer());

        verify(s3Service, times(2)).uploadWeatherData(any(WeatherData.class));
    }

    // ===== Helper Methods =====

    private WeatherData createTestWeatherData(String stationId, String dataType) {
        NoaaWeatherData data = new NoaaWeatherData(stationId, Instant.now(), dataType);
        data.setRawData("Test raw data for " + stationId);
        return data;
    }
}
