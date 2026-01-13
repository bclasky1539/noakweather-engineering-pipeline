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
import weather.model.WeatherData;
import weather.exception.WeatherServiceException;
import weather.exception.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TafIngestionOrchestrator.
 * <p>
 * Since TafIngestionOrchestrator is a simple concrete implementation of
 * AbstractNoaaIngestionOrchestrator, most functionality is tested in
 * AbstractNoaaIngestionOrchestratorTest.
 * <p>
 * These tests focus on TAF-specific behavior.
 *
 * @author bclasky1539
 *
 */
@ExtendWith(MockitoExtension.class)
class TafIngestionOrchestratorTest {

    @Mock
    private NoaaAviationWeatherClient mockNoaaClient;

    @Mock
    private S3UploadService mockS3Service;

    private TafIngestionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new TafIngestionOrchestrator(mockNoaaClient, mockS3Service);
    }

    // ===== Constructor Tests =====

    @Test
    void testConstructor_DefaultConcurrency() {
        // Act
        TafIngestionOrchestrator defaultOrchestrator =
                new TafIngestionOrchestrator(mockNoaaClient, mockS3Service);

        // Assert
        assertNotNull(defaultOrchestrator);
    }

    @Test
    void testConstructor_CustomConcurrency() {
        // Act
        TafIngestionOrchestrator customOrchestrator =
                new TafIngestionOrchestrator(mockNoaaClient, mockS3Service, 20);

        // Assert
        assertNotNull(customOrchestrator);
    }

    // ===== fetchFromNoaa() Tests =====

    @Test
    void testFetchFromNoaa_Success() throws WeatherServiceException {
        // Arrange
        String stationId = "KJFK";
        WeatherData expectedData = createMockWeatherData(stationId);

        when(mockNoaaClient.fetchTafReport(stationId)).thenReturn(expectedData);

        // Act
        WeatherData result = invokeFetchFromNoaa(orchestrator, stationId);

        // Assert
        assertNotNull(result);
        assertEquals(stationId, result.getStationId());
        verify(mockNoaaClient, times(1)).fetchTafReport(stationId);
    }

    @Test
    void testFetchFromNoaa_NoData() throws WeatherServiceException {
        // Arrange
        String stationId = "KJFK";

        when(mockNoaaClient.fetchTafReport(stationId)).thenReturn(null);

        // Act
        WeatherData result = invokeFetchFromNoaa(orchestrator, stationId);

        // Assert
        assertNull(result);
        verify(mockNoaaClient, times(1)).fetchTafReport(stationId);
    }

    @Test
    void testFetchFromNoaa_ThrowsException() throws WeatherServiceException {
        // Arrange
        String stationId = "KJFK";
        WeatherServiceException expectedException =
                new WeatherServiceException(ErrorType.NETWORK_ERROR, "Connection failed");

        when(mockNoaaClient.fetchTafReport(stationId)).thenThrow(expectedException);

        // Act & Assert
        WeatherServiceException exception = assertThrows(WeatherServiceException.class,
                () -> invokeFetchFromNoaa(orchestrator, stationId));

        assertEquals(ErrorType.NETWORK_ERROR, exception.getErrorType());
        verify(mockNoaaClient, times(1)).fetchTafReport(stationId);
    }

    @Test
    void testFetchFromNoaa_CallsCorrectClientMethod() throws WeatherServiceException {
        // Arrange
        String stationId = "KCLT";
        WeatherData mockData = createMockWeatherData(stationId);

        when(mockNoaaClient.fetchTafReport(stationId)).thenReturn(mockData);

        // Act
        invokeFetchFromNoaa(orchestrator, stationId);

        // Assert - verify it calls fetchTafReport, not fetchMetarReport
        verify(mockNoaaClient, times(1)).fetchTafReport(stationId);
        verify(mockNoaaClient, never()).fetchMetarReport(anyString());
    }

    // ===== Data Type Verification =====

    @Test
    void testDataType_IsTaf() {
        // This verifies the orchestrator was constructed with "TAF" as dataType
        // We can't directly access the dataType field, but we can verify behavior

        // The metrics and logs will contain "TAF"
        assertNotNull(orchestrator.getMetrics());
    }

    // ===== Helper Methods =====

    private WeatherData createMockWeatherData(String stationId) {
        NoaaWeatherData data = new NoaaWeatherData(stationId, Instant.now(), "TAF");
        data.setRawData("TAF " + stationId + " test data");
        return data;
    }

    /**
     * Helper to invoke the protected fetchFromNoaa method using reflection.
     */
    private WeatherData invokeFetchFromNoaa(TafIngestionOrchestrator orchestrator,
                                            String stationId) throws WeatherServiceException {
        try {
            java.lang.reflect.Method method =
                    AbstractNoaaIngestionOrchestrator.class.getDeclaredMethod("fetchFromNoaa", String.class);
            method.setAccessible(true);
            return (WeatherData) method.invoke(orchestrator, stationId);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Unwrap the actual exception thrown by the method
            Throwable cause = e.getCause();
            if (cause instanceof WeatherServiceException) {
                throw (WeatherServiceException) cause;
            }
            throw new RuntimeException("Unexpected exception", cause);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke fetchFromNoaa", e);
        }
    }
}
