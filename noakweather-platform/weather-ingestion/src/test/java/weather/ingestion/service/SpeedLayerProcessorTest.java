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
package weather.ingestion.service;

import weather.ingestion.service.source.noaa.NoaaAviationWeatherClient;
import weather.model.NoaaWeatherData;
import weather.model.ProcessingLayer;
import weather.model.WeatherData;
import weather.exception.WeatherServiceException;
import weather.exception.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SpeedLayerProcessor using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class SpeedLayerProcessorTest {
    
    @Mock
    private NoaaAviationWeatherClient noaaClient;
    
    @Mock
    private S3UploadService s3Service;
    
    private SpeedLayerProcessor processor;
    
    @BeforeEach
    void setUp() {
        processor = new SpeedLayerProcessor(noaaClient, s3Service);
    }
    
    @Test
    void testProcessStationSuccess() throws IOException, WeatherServiceException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KJFK");
        String s3Key = "speed-layer/KJFK/2025/10/26/test.json";
        
        when(noaaClient.fetchLatestMetar("KJFK")).thenReturn(weatherData);
        when(s3Service.uploadWeatherData(any(WeatherData.class))).thenReturn(s3Key);
        
        // Act
        WeatherData result = processor.processStation("KJFK");
        
        // Assert
        assertNotNull(result);
        assertEquals("KJFK", result.getStationId());
        assertEquals(ProcessingLayer.SPEED_LAYER, result.getProcessingLayer());
        assertTrue(result.getMetadata().containsKey("storage_location"));
        assertTrue(result.getMetadata().containsKey("validated"));
        
        verify(noaaClient, times(1)).fetchLatestMetar("KJFK");
        verify(s3Service, times(1)).uploadWeatherData(any(WeatherData.class));
    }
    
    @Test
    void testProcessStationNoDataAvailable() throws WeatherServiceException, IOException {
        // Arrange
        when(noaaClient.fetchLatestMetar("KJFK")).thenReturn(null);
    
        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            processor.processStation("KJFK");
        });
    
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("No weather data available"));
        verify(s3Service, never()).uploadWeatherData(any(WeatherData.class));
    }
    
    @Test
    void testProcessStationWeatherServiceException() throws WeatherServiceException, IOException {
        // Arrange
        when(noaaClient.fetchLatestMetar("KJFK"))
                .thenThrow(new WeatherServiceException(
                        ErrorType.NETWORK_ERROR,
                        "Connection failed",
                        "KJFK"
                ));
        
        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            processor.processStation("KJFK");
        });
        
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Failed to process station"));
        verify(s3Service, never()).uploadWeatherData(any(WeatherData.class));
    }
    
    @Test
    void testProcessStationS3UploadFailure() throws IOException, WeatherServiceException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KJFK");
        
        when(noaaClient.fetchLatestMetar("KJFK")).thenReturn(weatherData);
        when(s3Service.uploadWeatherData(any(WeatherData.class)))
                .thenThrow(new IOException("S3 upload failed"));
        
        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            processor.processStation("KJFK");
        });
        
        assertNotNull(exception);
    }
    
    @Test
    void testProcessStationsBatch() throws WeatherServiceException, IOException {
        // Arrange
        List<String> stationIds = Arrays.asList("KJFK", "KLGA", "KEWR");
        
        when(noaaClient.fetchLatestMetar(anyString()))
                .thenReturn(createTestWeatherData("KJFK"))
                .thenReturn(createTestWeatherData("KLGA"))
                .thenReturn(createTestWeatherData("KEWR"));
        
        when(s3Service.uploadWeatherData(any(WeatherData.class)))
                .thenReturn("s3://test-key-1")
                .thenReturn("s3://test-key-2")
                .thenReturn("s3://test-key-3");
        
        // Act
        List<WeatherData> results = processor.processStationsBatch(stationIds);
        
        // Assert
        assertEquals(3, results.size());
        verify(noaaClient, times(3)).fetchLatestMetar(anyString());
        verify(s3Service, times(3)).uploadWeatherData(any(WeatherData.class));
    }
    
    @Test
    void testProcessStationsBatchPartialFailure() throws WeatherServiceException, IOException {
        // Arrange
        List<String> stationIds = Arrays.asList("KJFK", "KLGA");
        
        when(noaaClient.fetchLatestMetar("KJFK"))
                .thenReturn(createTestWeatherData("KJFK"));
        when(noaaClient.fetchLatestMetar("KLGA"))
                .thenReturn(null);  // No data for KLGA
        
        // Act
        List<WeatherData> results = processor.processStationsBatch(stationIds);
        
        // Assert - only successful one should be in results
        assertEquals(1, results.size());
        assertEquals("KJFK", results.get(0).getStationId());
    }
    
    @Test
    void testProcessRegion() throws IOException, WeatherServiceException {
        // Arrange
        List<WeatherData> weatherDataList = Arrays.asList(
                createTestWeatherData("KJFK"),
                createTestWeatherData("KLGA")
        );
        
        when(noaaClient.fetchMetarByBoundingBox(40.0, -75.0, 41.0, -73.0))
                .thenReturn(weatherDataList);
        
        when(s3Service.uploadWeatherDataBatch(anyList()))
                .thenReturn(Arrays.asList("s3://key1", "s3://key2"));
        
        // Act
        List<WeatherData> results = processor.processRegion(40.0, -75.0, 41.0, -73.0);
        
        // Assert
        assertEquals(2, results.size());
        for (WeatherData data : results) {
            assertEquals(ProcessingLayer.SPEED_LAYER, data.getProcessingLayer());
            assertTrue(data.getMetadata().containsKey("validated"));
        }
        
        verify(noaaClient, times(1)).fetchMetarByBoundingBox(40.0, -75.0, 41.0, -73.0);
        verify(s3Service, times(1)).uploadWeatherDataBatch(anyList());
    }
    
    @Test
    void testProcessRegionNoDataFound() throws IOException, WeatherServiceException {
        // Arrange
        when(noaaClient.fetchMetarByBoundingBox(40.0, -75.0, 41.0, -73.0))
                .thenReturn(Arrays.asList());
        
        // Act
        List<WeatherData> results = processor.processRegion(40.0, -75.0, 41.0, -73.0);
        
        // Assert
        assertTrue(results.isEmpty());
        verify(s3Service, never()).uploadWeatherDataBatch(anyList());
    }
    
    @Test
    void testProcessRegionWeatherServiceException() throws WeatherServiceException, IOException {
        // Arrange
        when(noaaClient.fetchMetarByBoundingBox(40.0, -75.0, 41.0, -73.0))
                .thenThrow(new WeatherServiceException(
                        ErrorType.NETWORK_ERROR,
                        "Failed to fetch",
                        "bbox"
                ));
        
        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            processor.processRegion(40.0, -75.0, 41.0, -73.0);
        });
        
        assertNotNull(exception);
    }
    
    @Test
    void testGetStatistics() {
        // Act
        var stats = processor.getStatistics();
        
        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("max_concurrent_requests"));
        assertTrue(stats.containsKey("executor_active_threads"));
        assertTrue(stats.containsKey("executor_queue_size"));
    }
    
    @Test
    void testShutdown() {
        // Act & Assert - verify shutdown completes without throwing
        assertDoesNotThrow(() -> processor.shutdown());
    }
    
    @Test
    void testValidationAddsMetadata() throws IOException, WeatherServiceException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KJFK");
        
        when(noaaClient.fetchLatestMetar("KJFK")).thenReturn(weatherData);
        when(s3Service.uploadWeatherData(any(WeatherData.class))).thenReturn("s3://key");
        
        // Act
        WeatherData result = processor.processStation("KJFK");
        
        // Assert
        assertTrue(result.getMetadata().containsKey("validated"));
        assertTrue(result.getMetadata().containsKey("validation_timestamp"));
        assertTrue(result.getMetadata().containsKey("processor"));
        assertEquals("true", result.getMetadata().get("validated"));
        assertEquals("SpeedLayerProcessor", result.getMetadata().get("processor"));
    }
    
    @Test
    void testConstructorWithCustomConcurrency() {
        // Act
        SpeedLayerProcessor customProcessor = new SpeedLayerProcessor(
                noaaClient, s3Service, 10
        );
        
        // Assert
        assertNotNull(customProcessor);
        var stats = customProcessor.getStatistics();
        assertEquals(10, stats.get("max_concurrent_requests"));
        
        customProcessor.shutdown();
    }
    
    @Test
    void testProcessStationMissingStationId() throws IOException, WeatherServiceException {
        // Arrange
        WeatherData weatherData = createTestWeatherData(null); // null station ID
        when(noaaClient.fetchLatestMetar("TEST")).thenReturn(weatherData);
        
        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            processor.processStation("TEST");
        });
        
        assertTrue(exception.getMessage().contains("stationId"));
    }
    
    @Test
    void testProcessStationMissingSource() throws IOException, WeatherServiceException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KJFK");
        weatherData.setSource(null);
        when(noaaClient.fetchLatestMetar("KJFK")).thenReturn(weatherData);
        
        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            processor.processStation("KJFK");
        });
        
        assertTrue(exception.getMessage().contains("dataSource"));
    }

    @Test
    void testRunContinuousIngestionShortDuration() {
        // Arrange
        List<String> stationIds = Arrays.asList("KJFK");
        
        // Act - run for 0 minutes (exits immediately, no mocks needed)
        assertDoesNotThrow(() -> {
            processor.runContinuousIngestion(stationIds, 2, 0);
        });
    }

     @Test
    void testProcessStationsBatchWithEmptyList() {
        // Act
        List<WeatherData> results = processor.processStationsBatch(Arrays.asList());
        
        // Assert
        assertTrue(results.isEmpty());
    }

    private WeatherData createTestWeatherData(String stationId) {
        NoaaWeatherData data = new NoaaWeatherData(stationId, Instant.now(), "METAR");
        data.setRawData("{\"test\": \"data\"}");
        return data;
    }
}
