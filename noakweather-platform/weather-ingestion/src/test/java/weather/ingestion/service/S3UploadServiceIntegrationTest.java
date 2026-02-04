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

import weather.model.ProcessingLayer;
import weather.model.WeatherData;
import weather.model.NoaaWeatherData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for S3UploadService.
 * <p>
 * UPDATED: Now includes tests for dual storage functionality.
 * <p>
 * These tests require:
 * - AWS credentials configured (via ~/.aws/credentials or environment variables)
 * - S3 bucket specified via environment variable: TEST_WEATHER_BUCKET
 * - Appropriate IAM permissions for the test bucket
 * <p>
 * To run these tests:
 * export TEST_WEATHER_BUCKET=your-test-bucket-name
 * export AWS_REGION=us-east-1
 * mvn test -Dtest=S3UploadServiceIntegrationTest
 * <p>
 * Note: These tests will create and delete objects in S3. Ensure the test bucket
 * is not used for production data.
 *
 * @author bclasky1539
 *
 */
@EnabledIfEnvironmentVariable(named = "TEST_WEATHER_BUCKET", matches = ".+")
class S3UploadServiceIntegrationTest {

    private S3UploadService s3Service;
    private String testBucketName;
    private String testRegion;
    private List<String> uploadedKeys; // Track keys for cleanup

    @BeforeEach
    void setUp() {
        testBucketName = System.getenv("TEST_WEATHER_BUCKET");
        testRegion = System.getenv().getOrDefault("AWS_REGION", "us-east-1");

        s3Service = new S3UploadService(testBucketName, testRegion);
        uploadedKeys = new ArrayList<>();

        System.out.println("Running S3 integration tests with bucket: " + testBucketName);
    }

    @AfterEach
    void tearDown() {
        // Clean up uploaded test objects
        if (s3Service != null) {
            try (S3Client s3Client = software.amazon.awssdk.services.s3.S3Client.builder()
                    .region(software.amazon.awssdk.regions.Region.of(testRegion))
                    .build()) {

                for (String key : uploadedKeys) {
                    try {
                        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                                .bucket(testBucketName)
                                .key(key)
                                .build();
                        s3Client.deleteObject(deleteRequest);
                        System.out.println("Cleaned up test object: " + key);
                    } catch (S3Exception e) {
                        System.err.println("Failed to clean up " + key + ": " + e.getMessage());
                    }
                }
            }

            s3Service.close();
        }
    }

    @Test
    void testBucketAccessible() {
        assertTrue(s3Service.isBucketAccessible(),
                "Test bucket should be accessible: " + testBucketName);
    }

    // ===== NEW: Dual Storage Integration Tests =====

    @Test
    void testUploadWeatherDataDual() throws IOException {
        // Create test weather data
        NoaaWeatherData testData = createTestWeatherData("KJFK");

        // Upload to S3 using dual storage
        S3UploadService.DualStorageResult result = s3Service.uploadWeatherDataDual(testData);

        // Track for cleanup
        uploadedKeys.add(result.rawTextKey());
        uploadedKeys.add(result.jsonKey());

        // Verify result object
        assertNotNull(result);
        assertNotNull(result.rawTextKey());
        assertNotNull(result.jsonKey());

        // Verify raw text key structure
        assertTrue(result.rawTextKey().contains("raw-data"),
                "Raw key should contain raw-data prefix");
        assertTrue(result.rawTextKey().contains("kjfk"),
                "Raw key should contain station ID");
        assertTrue(result.rawTextKey().endsWith(".txt"),
                "Raw key should have .txt extension");

        // Verify JSON key structure
        assertTrue(result.jsonKey().contains("speed-layer"),
                "JSON key should contain speed-layer prefix");
        assertTrue(result.jsonKey().contains("kjfk"),
                "JSON key should contain station ID");
        assertTrue(result.jsonKey().endsWith(".json"),
                "JSON key should have .json extension");

        // Verify both objects exist in S3
        assertTrue(objectExistsInS3(result.rawTextKey()),
                "Raw text file should exist in S3");
        assertTrue(objectExistsInS3(result.jsonKey()),
                "JSON file should exist in S3");
    }

    @Test
    void testUploadWeatherDataDual_BothFilesHaveSameTimestamp() throws IOException {
        // Create test weather data
        NoaaWeatherData testData = createTestWeatherData("KCLT");

        // Upload to S3
        S3UploadService.DualStorageResult result = s3Service.uploadWeatherDataDual(testData);

        // Track for cleanup
        uploadedKeys.add(result.rawTextKey());
        uploadedKeys.add(result.jsonKey());

        // Extract timestamps from both keys
        String rawTimestamp = extractTimestamp(result.rawTextKey());
        String jsonTimestamp = extractTimestamp(result.jsonKey());

        // Verify same timestamp
        assertEquals(rawTimestamp, jsonTimestamp,
                "Both files should have the same timestamp");
    }

    @Test
    void testUploadWeatherDataDual_BothFilesHaveSameDatePartitioning() throws IOException {
        // Create test weather data
        NoaaWeatherData testData = createTestWeatherData("KLAX");

        // Upload to S3
        S3UploadService.DualStorageResult result = s3Service.uploadWeatherDataDual(testData);

        // Track for cleanup
        uploadedKeys.add(result.rawTextKey());
        uploadedKeys.add(result.jsonKey());

        // Extract date paths from both keys
        String rawDatePath = extractDatePath(result.rawTextKey());
        String jsonDatePath = extractDatePath(result.jsonKey());

        // Verify same date partitioning
        assertEquals(rawDatePath, jsonDatePath,
                "Both files should have the same date partitioning (YYYY/MM/DD)");
    }

    @Test
    void testUploadWeatherDataDual_MultipleStations() throws IOException {
        // Create test data for multiple stations
        List<String> stations = List.of("KJFK", "KLGA", "KEWR");
        List<S3UploadService.DualStorageResult> results = new ArrayList<>();

        for (String station : stations) {
            NoaaWeatherData testData = createTestWeatherData(station);
            S3UploadService.DualStorageResult result = s3Service.uploadWeatherDataDual(testData);
            results.add(result);

            // Track for cleanup
            uploadedKeys.add(result.rawTextKey());
            uploadedKeys.add(result.jsonKey());
        }

        // Verify all uploads succeeded
        assertEquals(3, results.size());

        // Verify all files exist
        for (S3UploadService.DualStorageResult result : results) {
            assertTrue(objectExistsInS3(result.rawTextKey()),
                    "Raw file should exist: " + result.rawTextKey());
            assertTrue(objectExistsInS3(result.jsonKey()),
                    "JSON file should exist: " + result.jsonKey());
        }
    }

    // ===== Existing Tests (Kept for backward compatibility) =====

    @Test
    void testUploadSingleWeatherData() throws IOException {
        // Create test weather data
        NoaaWeatherData testData = createTestWeatherData("KJFK");

        // Upload to S3
        String s3Key = s3Service.uploadWeatherData(testData);
        uploadedKeys.add(s3Key);

        // Verify
        assertNotNull(s3Key, "S3 key should not be null");
        assertTrue(s3Key.contains("speed-layer"), "Key should contain speed-layer prefix");
        assertTrue(s3Key.toLowerCase().contains("kjfk"), "Key should contain station ID");
        assertTrue(s3Key.endsWith(".json"), "Key should have .json extension");

        // Verify object exists in S3
        assertTrue(objectExistsInS3(s3Key), "Object should exist in S3");
    }

    @Test
    void testUploadMultipleWeatherDataBatch() throws IOException {
        // Create multiple test weather data records
        List<WeatherData> testDataList = new ArrayList<>();
        testDataList.add(createTestWeatherData("KJFK"));
        testDataList.add(createTestWeatherData("KLGA"));
        testDataList.add(createTestWeatherData("KEWR"));

        // Upload batch
        List<String> s3Keys = s3Service.uploadWeatherDataBatch(testDataList);
        uploadedKeys.addAll(s3Keys);

        // Verify
        assertEquals(3, s3Keys.size(), "Should upload 3 objects");

        for (String key : s3Keys) {
            assertTrue(objectExistsInS3(key), "Object should exist in S3: " + key);
        }
    }

    @Test
    void testUploadRawData() throws IOException {
        String rawData = "METAR KJFK 251651Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 SLP210";

        String s3Key = s3Service.uploadRawData("noaa", rawData, "KJFK");
        uploadedKeys.add(s3Key);

        assertNotNull(s3Key);
        assertTrue(s3Key.contains("raw-data"), "Key should contain raw-data prefix");
        assertTrue(objectExistsInS3(s3Key), "Raw data object should exist in S3");
    }

    @Test
    void testS3KeyGeneration() throws IOException {
        NoaaWeatherData testData = createTestWeatherData("TEST");

        String s3Key = s3Service.uploadWeatherData(testData);
        uploadedKeys.add(s3Key);

        // Verify key structure
        assertTrue(s3Key.contains("speed-layer/"), "Should start with speed-layer/");
        assertTrue(s3Key.contains("/metar/"), "Should contain report type");
        assertTrue(s3Key.contains("TEST_"), "Should contain station ID");
        assertTrue(s3Key.endsWith(".json"), "Should end with .json");

        // Verify date partitioning exists (format: /YYYY/MM/DD/)
        assertTrue(s3Key.matches(".*/(\\d{4})/(\\d{2})/(\\d{2})/.*"),
                "Should contain date partitioning");
    }

    @Test
    void testUploadWithMetadata() throws IOException {
        NoaaWeatherData testData = createTestWeatherData("KJFK");
        testData.addMetadata("test-key", "test-value");
        testData.addMetadata("priority", "high");

        String s3Key = s3Service.uploadWeatherData(testData);
        uploadedKeys.add(s3Key);

        // Verify upload succeeded
        assertTrue(objectExistsInS3(s3Key));
    }

    @Test
    void testUploadEmptyBatch() throws IOException {
        List<WeatherData> emptyList = new ArrayList<>();

        List<String> s3Keys = s3Service.uploadWeatherDataBatch(emptyList);

        assertTrue(s3Keys.isEmpty(), "Should return empty list for empty input");
    }

    // ===== Helper Methods =====

    /**
     * Helper method to create test weather data using the parameterized constructor.
     */
    private NoaaWeatherData createTestWeatherData(String stationId) {
        NoaaWeatherData data = new NoaaWeatherData(
                stationId,
                Instant.now(),
                "METAR"
        );

        data.setRawData("Test raw data for " + stationId);
        data.setProcessingLayer(ProcessingLayer.SPEED_LAYER);
        data.addMetadata("format", "JSON");

        return data;
    }

    /**
     * Helper method to check if an object exists in S3.
     */
    private boolean objectExistsInS3(String key) {
        try (S3Client s3Client = software.amazon.awssdk.services.s3.S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.of(testRegion))
                .build()) {

            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(testBucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headRequest);
            return true;

        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    /**
     * Extracts timestamp from S3 key.
     * Expected format: .../STATION_YYYYMMDD_HHMM.ext
     */
    private String extractTimestamp(String s3Key) {
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
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/(\\d{4})/(\\d{2})/(\\d{2})/");
        java.util.regex.Matcher matcher = pattern.matcher(s3Key);
        if (matcher.find()) {
            return matcher.group(1) + "/" + matcher.group(2) + "/" + matcher.group(3);
        }
        return "";
    }
}
