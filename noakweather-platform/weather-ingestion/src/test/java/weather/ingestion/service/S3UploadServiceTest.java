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
import weather.model.WeatherData;
import weather.model.WeatherDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for S3UploadService using Mockito.
 * UPDATED: Comprehensive coverage for dual storage functionality.
 *
 * @author bclasky1539
 *
 */
@ExtendWith(MockitoExtension.class)
class S3UploadServiceTest {

    @Mock
    private S3Client s3Client;

    @Captor
    private ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor;

    @Captor
    private ArgumentCaptor<RequestBody> requestBodyCaptor;

    private S3UploadService uploadService;

    private static final String TEST_BUCKET = "test-weather-bucket";

    @BeforeEach
    void setUp() {
        uploadService = new S3UploadService(s3Client, TEST_BUCKET);
    }

    // ===== NEW: Comprehensive Dual Storage Tests =====

    @Test
    void testUploadWeatherDataDual() throws IOException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KJFK");

        PutObjectResponse response = PutObjectResponse.builder()
                .eTag("test-etag-123")
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        // Act
        S3UploadService.DualStorageResult result = uploadService.uploadWeatherDataDual(weatherData);

        // Assert
        assertNotNull(result);
        assertNotNull(result.rawTextKey());  // Using record accessor (no "get")
        assertNotNull(result.jsonKey());     // Using record accessor (no "get")

        // Verify both files uploaded (2 putObject calls)
        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        // Verify raw text key structure
        assertTrue(result.rawTextKey().contains("raw-data"));
        assertTrue(result.rawTextKey().contains("KJFK"));
        assertTrue(result.rawTextKey().endsWith(".txt"));

        // Verify JSON key structure
        assertTrue(result.jsonKey().contains("speed-layer"));
        assertTrue(result.jsonKey().contains("KJFK"));
        assertTrue(result.jsonKey().endsWith(".json"));
    }

    @Test
    void testUploadWeatherDataDual_VerifyContentTypes() throws IOException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KLAX");

        PutObjectResponse response = PutObjectResponse.builder()
                .eTag("test-etag")
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        // Act
        uploadService.uploadWeatherDataDual(weatherData);

        // Assert
        verify(s3Client, times(2)).putObject(putObjectRequestCaptor.capture(), any(RequestBody.class));

        List<PutObjectRequest> requests = putObjectRequestCaptor.getAllValues();
        assertEquals(2, requests.size());

        // First request should be raw text
        PutObjectRequest rawRequest = requests.get(0);
        assertEquals("text/plain", rawRequest.contentType());
        assertTrue(rawRequest.key().contains("raw-data"));
        assertTrue(rawRequest.key().endsWith(".txt"));

        // Second request should be JSON
        PutObjectRequest jsonRequest = requests.get(1);
        assertEquals("application/json", jsonRequest.contentType());
        assertTrue(jsonRequest.key().contains("speed-layer"));
        assertTrue(jsonRequest.key().endsWith(".json"));
    }

    @Test
    void testUploadWeatherDataDual_VerifyMetadata() throws IOException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KCLT");

        PutObjectResponse response = PutObjectResponse.builder()
                .eTag("test-etag")
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        // Act
        uploadService.uploadWeatherDataDual(weatherData);

        // Assert
        verify(s3Client, times(2)).putObject(putObjectRequestCaptor.capture(), any(RequestBody.class));

        List<PutObjectRequest> requests = putObjectRequestCaptor.getAllValues();

        // Both requests should have metadata
        for (PutObjectRequest request : requests) {
            assertNotNull(request.metadata());
            assertEquals("NOAA", request.metadata().get("source"));
            assertEquals("KCLT", request.metadata().get("station-id"));
        }

        // Raw request has specific metadata
        assertEquals("METAR", requests.get(0).metadata().get("data-type"));

        // JSON request has specific metadata
        assertEquals("METAR", requests.get(1).metadata().get("report-type"));
    }

    @Test
    void testUploadWeatherDataDual_NullWeatherData() {
        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadWeatherDataDual(null));

        assertTrue(exception.getMessage().contains("null"));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadWeatherDataDual_NullRawData() {
        // Arrange - Create WeatherData with null raw data
        NoaaWeatherData weatherData = new NoaaWeatherData("KJFK", Instant.now(), "METAR");
        weatherData.setRawData(null); // This should trigger validation

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadWeatherDataDual(weatherData));

        assertTrue(exception.getMessage().contains("Raw data cannot be null or empty"));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadWeatherDataDual_EmptyRawData() {
        // Arrange - Create WeatherData with empty raw data
        NoaaWeatherData weatherData = new NoaaWeatherData("KJFK", Instant.now(), "METAR");
        weatherData.setRawData(""); // This should trigger validation

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadWeatherDataDual(weatherData));

        assertTrue(exception.getMessage().contains("Raw data cannot be null or empty"));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadWeatherDataDual_NullStationId() {
        // Arrange - Create WeatherData with null station ID
        NoaaWeatherData weatherData = new NoaaWeatherData(null, Instant.now(), "METAR");
        weatherData.setRawData("METAR KJFK 251651Z 28016KT");

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadWeatherDataDual(weatherData));

        assertTrue(exception.getMessage().contains("Station ID cannot be null or empty"));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadWeatherDataDual_EmptyStationId() {
        // Arrange - Create WeatherData with empty station ID
        NoaaWeatherData weatherData = new NoaaWeatherData("", Instant.now(), "METAR");
        weatherData.setRawData("METAR KJFK 251651Z 28016KT");

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadWeatherDataDual(weatherData));

        assertTrue(exception.getMessage().contains("Station ID cannot be null or empty"));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadWeatherDataDual_EmptyDataType() {
        // Arrange - Create WeatherData with empty data type
        NoaaWeatherData weatherData = new NoaaWeatherData("KJFK", Instant.now(), "");
        weatherData.setRawData("METAR KJFK 251651Z 28016KT");

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadWeatherDataDual(weatherData));

        assertTrue(exception.getMessage().contains("Data type cannot be null or empty"));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    // NOTE: testUploadWeatherDataDual_NullIngestionTime removed because:
    // - WeatherData.ingestionTime is final and always initialized in constructor
    // - getIngestionTime() can never return null
    // - The validation check in uploadRawDataWithPartitioning() is unreachable code

    @Test
    void testUploadWeatherDataDual_RawUploadFailsS3Exception() {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KJFK");

        S3Exception s3Exception = (S3Exception) S3Exception.builder()
                .message("Access Denied")
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("AccessDenied")
                        .errorMessage("Access Denied")
                        .build())
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(s3Exception);

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadWeatherDataDual(weatherData));

        assertTrue(exception.getMessage().contains("Failed to upload raw data"));

        // Only one putObject call (the failed raw upload)
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadWeatherDataDual_RawUploadFailsRuntimeException() {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KJFK");

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("Network error"));

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadWeatherDataDual(weatherData));

        assertTrue(exception.getMessage().contains("Failed to upload raw data to S3"));

        // Only one putObject call
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadWeatherDataDual_JsonUploadFailsAfterRawSucceeds() {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KJFK");

        PutObjectResponse successResponse = PutObjectResponse.builder()
                .eTag("test-etag")
                .build();

        // Use RuntimeException instead of S3Exception to avoid awsErrorDetails() mocking issues
        // Production code handles both exception types the same way
        RuntimeException uploadFailure = new RuntimeException("S3 quota exceeded");

        // First call (raw text) succeeds, second call (JSON) fails
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(successResponse)  // Raw upload succeeds
                .thenThrow(uploadFailure);    // JSON upload fails

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadWeatherDataDual(weatherData));

        // Verify error message indicates failure
        assertTrue(exception.getMessage().contains("Failed to upload") ||
                        exception.getMessage().contains("S3 upload failed"),
                "Expected error about upload failure, got: " + exception.getMessage());

        // Both putObject calls should have been made (raw succeeded, JSON failed)
        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testDualStorageResult_ValidCreation() {
        // Act
        S3UploadService.DualStorageResult result =
                new S3UploadService.DualStorageResult("raw-key", "json-key");

        // Assert
        assertEquals("raw-key", result.rawTextKey());
        assertEquals("json-key", result.jsonKey());
    }

    @Test
    void testDualStorageResult_NullRawKeyThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new S3UploadService.DualStorageResult(null, "json-key"));

        assertTrue(exception.getMessage().contains("Raw text key"));
    }

    @Test
    void testDualStorageResult_EmptyRawKeyThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new S3UploadService.DualStorageResult("", "json-key"));

        assertTrue(exception.getMessage().contains("Raw text key"));
    }

    @Test
    void testDualStorageResult_NullJsonKeyThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new S3UploadService.DualStorageResult("raw-key", null));

        assertTrue(exception.getMessage().contains("JSON key"));
    }

    @Test
    void testDualStorageResult_EmptyJsonKeyThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new S3UploadService.DualStorageResult("raw-key", ""));

        assertTrue(exception.getMessage().contains("JSON key"));
    }

    @Test
    void testDualStorageResult_ToString() {
        // Arrange
        S3UploadService.DualStorageResult result =
                new S3UploadService.DualStorageResult("raw-key", "json-key");

        // Act
        String toString = result.toString();

        // Assert - Record toString includes field names and values
        assertNotNull(toString);
        assertTrue(toString.contains("raw-key"));
        assertTrue(toString.contains("json-key"));
    }

    @Test
    void testDualStorageResult_Equals() {
        // Arrange
        S3UploadService.DualStorageResult result1 =
                new S3UploadService.DualStorageResult("raw-key", "json-key");
        S3UploadService.DualStorageResult result2 =
                new S3UploadService.DualStorageResult("raw-key", "json-key");
        S3UploadService.DualStorageResult result3 =
                new S3UploadService.DualStorageResult("different-raw", "different-json");

        // Assert
        assertEquals(result1, result2);
        assertNotEquals(result1, result3);
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void testUploadWeatherDataDual_BothFilesHaveSameTimestamp() throws IOException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KCLT");

        PutObjectResponse response = PutObjectResponse.builder()
                .eTag("test-etag")
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        // Act
        S3UploadService.DualStorageResult result = uploadService.uploadWeatherDataDual(weatherData);

        // Assert - extract timestamp from both keys
        String rawTimestamp = extractTimestamp(result.rawTextKey());
        String jsonTimestamp = extractTimestamp(result.jsonKey());

        assertEquals(rawTimestamp, jsonTimestamp,
                "Both files should have the same timestamp");
    }

    @Test
    void testUploadWeatherDataDual_BothFilesHaveSameDatePartitioning() throws IOException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KLAX");

        PutObjectResponse response = PutObjectResponse.builder()
                .eTag("test-etag")
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        // Act
        S3UploadService.DualStorageResult result = uploadService.uploadWeatherDataDual(weatherData);

        // Assert - both should have same date partitioning (YYYY/MM/DD)
        String rawDatePath = extractDatePath(result.rawTextKey());
        String jsonDatePath = extractDatePath(result.jsonKey());

        assertEquals(rawDatePath, jsonDatePath,
                "Both files should have the same date partitioning");
    }

    // ===== Existing Tests (Kept for backward compatibility) =====

    @Test
    void testUploadWeatherData() throws IOException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KJFK");

        PutObjectResponse response = PutObjectResponse.builder()
                .eTag("test-etag-123")
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        // Act
        String s3Key = uploadService.uploadWeatherData(weatherData);

        // Assert
        assertNotNull(s3Key);
        assertTrue(s3Key.contains("KJFK"));
        assertTrue(s3Key.contains("speed-layer"));
        assertTrue(s3Key.endsWith(".json"));

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadWeatherDataBatch() throws IOException {
        // Arrange
        List<WeatherData> weatherDataList = Arrays.asList(
                createTestWeatherData("KJFK"),
                createTestWeatherData("KLGA"),
                createTestWeatherData("KEWR")
        );

        PutObjectResponse response = PutObjectResponse.builder()
                .eTag("test-etag")
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        // Act
        List<String> s3Keys = uploadService.uploadWeatherDataBatch(weatherDataList);

        // Assert
        assertEquals(3, s3Keys.size());
        assertTrue(s3Keys.get(0).contains("KJFK"));
        assertTrue(s3Keys.get(1).contains("KLGA"));
        assertTrue(s3Keys.get(2).contains("KEWR"));

        verify(s3Client, times(3)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadWeatherDataNullData() {
        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadWeatherData(null));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("null"));

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadWeatherDataMissingStationId() {
        // Arrange
        WeatherData weatherData = new NoaaWeatherData(null, Instant.now(), "METAR");

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadWeatherData(weatherData));

        assertNotNull(exception);
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadWeatherDataS3Exception() {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KJFK");

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 connection failed"));

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadWeatherData(weatherData));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Failed to upload") ||
                exception.getCause().getMessage().contains("S3 connection failed"));
    }

    @Test
    void testUploadWeatherDataBatchEmptyList() throws IOException {
        // Act
        List<String> s3Keys = uploadService.uploadWeatherDataBatch(Collections.emptyList());

        // Assert
        assertTrue(s3Keys.isEmpty());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadWeatherDataBatchPartialFailure() throws IOException {
        // Arrange
        List<WeatherData> weatherDataList = Arrays.asList(
                createTestWeatherData("KJFK"),
                createTestWeatherData("KLGA")
        );

        PutObjectResponse response = PutObjectResponse.builder().eTag("test").build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response)
                .thenThrow(new RuntimeException("S3 error"));

        // Act
        List<String> s3Keys = uploadService.uploadWeatherDataBatch(weatherDataList);

        // Assert - batch continues, only 1 succeeds
        assertEquals(1, s3Keys.size());
        assertTrue(s3Keys.get(0).contains("KJFK"));
    }

    @Test
    void testGenerateS3Key() throws IOException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KJFK");

        PutObjectResponse response = PutObjectResponse.builder().eTag("test").build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        // Act
        String s3Key = uploadService.uploadWeatherData(weatherData);

        // Assert - verify key format
        assertNotNull(s3Key);
        assertTrue(s3Key.contains("KJFK"));
    }

    @Test
    void testClose() {
        // Act
        uploadService.close();

        // Assert
        verify(s3Client, times(1)).close();
    }

    @Test
    void testUploadWithMetadata() throws IOException {
        // Arrange
        WeatherData weatherData = createTestWeatherData("KJFK");
        weatherData.addMetadata("test-key", "test-value");

        PutObjectResponse response = PutObjectResponse.builder().eTag("test").build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        // Act
        String s3Key = uploadService.uploadWeatherData(weatherData);

        // Assert
        assertNotNull(s3Key);
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadDifferentStations() throws IOException {
        // Arrange
        PutObjectResponse response = PutObjectResponse.builder().eTag("test").build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        // Act
        String s3Key1 = uploadService.uploadWeatherData(createTestWeatherData("KJFK"));
        String s3Key2 = uploadService.uploadWeatherData(createTestWeatherData("KLGA"));
        String s3Key3 = uploadService.uploadWeatherData(createTestWeatherData("KEWR"));

        // Assert
        assertTrue(s3Key1.contains("KJFK"));
        assertTrue(s3Key2.contains("KLGA"));
        assertTrue(s3Key3.contains("KEWR"));

        assertNotEquals(s3Key1, s3Key2);
        assertNotEquals(s3Key2, s3Key3);

        verify(s3Client, times(3)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadRawData() throws IOException {
        // Arrange
        String source = "noaa";
        String rawData = "METAR KJFK 251651Z 28016KT 10SM FEW250 22/12 A3015";
        String stationId = "KJFK";

        PutObjectResponse response = PutObjectResponse.builder()
                .eTag("test-etag")
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(response);

        // Act
        String s3Key = uploadService.uploadRawData(source, rawData, stationId);

        // Assert
        assertNotNull(s3Key);
        assertTrue(s3Key.contains(stationId));
        assertTrue(s3Key.contains("raw"));

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadRawDataNullSource() {
        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadRawData(null, "some data", "KJFK"));

        assertTrue(exception.getMessage().contains("Source"));
    }

    @Test
    void testUploadRawDataNullRawData() {
        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadRawData("noaa", null, "KJFK"));

        assertTrue(exception.getMessage().contains("Raw data"));
    }

    @Test
    void testUploadRawDataNullStationId() {
        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadRawData("noaa", "some data", null));

        assertTrue(exception.getMessage().contains("Station ID"));
    }

    @Test
    void testUploadRawDataS3Exception() {
        // Arrange
        String source = "noaa";
        String rawData = "test data";
        String stationId = "KJFK";

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 connection failed"));

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadRawData(source, rawData, stationId));

        assertTrue(exception.getMessage().contains("Failed to upload"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testIsBucketAccessible() {
        // Arrange
        when(s3Client.headBucket(any(Consumer.class)))
                .thenReturn(software.amazon.awssdk.services.s3.model.HeadBucketResponse.builder().build());

        // Act
        boolean accessible = uploadService.isBucketAccessible();

        // Assert
        assertTrue(accessible);
        verify(s3Client, times(1)).headBucket(any(Consumer.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testIsBucketNotAccessible() {
        // Arrange
        when(s3Client.headBucket(any(Consumer.class)))
                .thenThrow(software.amazon.awssdk.services.s3.model.NoSuchBucketException.builder()
                        .message("Bucket not found").build());

        // Act
        boolean accessible = uploadService.isBucketAccessible();

        // Assert
        assertFalse(accessible);
        verify(s3Client, times(1)).headBucket(any(Consumer.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testIsBucketAccessibleS3Exception() {
        // Arrange
        when(s3Client.headBucket(any(Consumer.class)))
                .thenThrow(new RuntimeException("S3 connection error"));

        // Act
        boolean accessible = uploadService.isBucketAccessible();

        // Assert
        assertFalse(accessible);
    }

    @Test
    void testUploadRawDataEmptySource() {
        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadRawData("", "some data", "KJFK"));

        assertTrue(exception.getMessage().contains("Source"));
    }

    @Test
    void testUploadRawDataEmptyRawData() {
        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadRawData("noaa", "", "KJFK"));

        assertTrue(exception.getMessage().contains("Raw data"));
    }

    @Test
    void testUploadRawDataEmptyStationId() {
        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadRawData("noaa", "some data", ""));

        assertTrue(exception.getMessage().contains("Station ID"));
    }

    @Test
    void testUploadWeatherDataBatchAllFailures() {
        // Arrange
        List<WeatherData> weatherDataList = Arrays.asList(
                createTestWeatherData("KJFK"),
                createTestWeatherData("KLGA")
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 error"));

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> uploadService.uploadWeatherDataBatch(weatherDataList));

        assertTrue(exception.getMessage().contains("All uploads in batch failed"));
    }

    // ===== Helper Methods =====

    private WeatherData createTestWeatherData(String stationId) {
        try {
            NoaaWeatherData data = new NoaaWeatherData(stationId, Instant.now(), "METAR");
            data.setRawData("METAR " + stationId + " 251651Z 28016KT 10SM FEW250 22/12 A3015");
            data.setSource(WeatherDataSource.NOAA);
            return data;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test data", e);
        }
    }

    /**
     * Extracts timestamp from S3 key.
     * Expected format: .../STATION_YYYYMMDD_HHMM.ext
     */
    private String extractTimestamp(String s3Key) {
        // Extract YYYYMMDD_HHMM from the key
        int lastSlash = s3Key.lastIndexOf('/');
        int lastDot = s3Key.lastIndexOf('.');
        String filename = s3Key.substring(lastSlash + 1, lastDot);
        int underscorePos = filename.indexOf('_');
        return filename.substring(underscorePos + 1); // Return YYYYMMDD_HHMM
    }

    /**
     * Extracts date path from S3 key.
     * Expected format: .../YYYY/MM/DD/...
     */
    private String extractDatePath(String s3Key) {
        // Match pattern like /2025/02/02/
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/(\\d{4})/(\\d{2})/(\\d{2})/");
        java.util.regex.Matcher matcher = pattern.matcher(s3Key);
        if (matcher.find()) {
            return matcher.group(1) + "/" + matcher.group(2) + "/" + matcher.group(3);
        }
        return "";
    }
}
