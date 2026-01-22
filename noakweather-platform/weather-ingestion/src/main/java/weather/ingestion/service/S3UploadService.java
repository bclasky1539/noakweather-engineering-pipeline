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

import weather.model.WeatherData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.time.ZoneId;
import java.time.LocalDateTime;

/**
 * Service for uploading weather data to Amazon S3.
 * Part of the Speed Layer in Lambda Architecture - provides low-latency access to recent data.
 * <p>
 * S3 Structure:
 *   s3://bucket-name/speed-layer/
 *     ├── noaa/
 *     │   ├── metar/2025/10/25/KJFK_20251025_1430.json
 *     │   └── taf/2025/10/25/KLGA_20251025_1430.json
 *     └── openweather/
 *         └── current/2025/10/25/NewYork_20251025_1430.json
 * <p>
 * NEW FUNCTIONALITY - Not present in legacy system
 * 
 * @author bclasky1539
 *
 */
public class S3UploadService {
    
    private static final Logger logger = LoggerFactory.getLogger(S3UploadService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    
    private final S3Client s3Client;
    private final String bucketName;
    private final ObjectMapper objectMapper;
    
    /**
     * Creates an S3UploadService with specified bucket and region.
     * 
     * @param bucketName the S3 bucket name for weather data
     * @param region the AWS region (e.g., "us-east-1")
     */
    public S3UploadService(String bucketName, String region) {
        this.bucketName = bucketName;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
        
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        logger.info("S3UploadService initialized for bucket: {} in region: {}", 
                bucketName, region);
    }
    
    /**
     * Constructor for dependency injection (testing).
     * 
     * @param s3Client custom S3 client
     * @param bucketName the S3 bucket name
     */
    public S3UploadService(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Uploads a single weather data record to S3.
     * File is stored in a partitioned structure by source, type, and date.
     * 
     * @param weatherData the weather data to upload
     * @return S3 key (path) where the data was stored
     * @throws IOException if upload fails
     */
    public String uploadWeatherData(WeatherData weatherData) throws IOException {
        // Add null check FIRST, before calling generateS3Key
        if (weatherData == null) {
            throw new IOException("Weather data cannot be null");
        }
    
        String s3Key = generateS3Key(weatherData);
    
        try {
            // Serialize weather data to JSON
            byte[] jsonBytes = objectMapper.writeValueAsBytes(weatherData);
        
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType("application/json")
                    .metadata(java.util.Map.of(
                            "source", weatherData.getSource().toString(),
                            "station-id", weatherData.getStationId(),
                            "report-type", weatherData.getDataType(),
                            "ingestion-time", weatherData.getIngestionTime().toString()
                    ))
                    .build();
        
            PutObjectResponse response = s3Client.putObject(putRequest, 
                    RequestBody.fromBytes(jsonBytes));
        
            logger.info("Successfully uploaded weather data to S3: {} (ETag: {})", 
                    s3Key, response.eTag());
        
            return s3Key;
        
        } catch (S3Exception e) {
            throw new IOException("S3 upload failed: " + e.awsErrorDetails().errorMessage(), e);
        } catch (RuntimeException e) {
            throw new IOException("Failed to upload weather data to S3", e);
        }
    }
    
    /**
     * Uploads multiple weather data records in batch.
     * More efficient than uploading one at a time.
     * 
     * @param weatherDataList list of weather data to upload
     * @return list of S3 keys where data was stored
     * @throws IOException if any upload fails
     */
    public List<String> uploadWeatherDataBatch(List<WeatherData> weatherDataList) throws IOException {
        logger.info("Starting batch upload of {} weather records to S3", weatherDataList.size());
        
        List<String> s3Keys = new java.util.ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        for (WeatherData data : weatherDataList) {
            try {
                String key = uploadWeatherData(data);
                s3Keys.add(key);
                successCount++;
            } catch (IOException e) {
                logger.error("Failed to upload record for station {}: {}", 
                        data.getStationId(), e.getMessage());
                failCount++;
                // Continue with other uploads rather than failing entire batch
            } catch (RuntimeException e) {
                logger.error("Unexpected error uploading record for station {}: {}", 
                        data.getStationId(), e.getMessage());
                failCount++;
                // Continue with other uploads rather than failing entire batch
            }
        }
        
        logger.info("Batch upload complete: {} succeeded, {} failed", successCount, failCount);
        
        if (failCount > 0 && successCount == 0) {
            throw new IOException("All uploads in batch failed");
        }
        
        return s3Keys;
    }
    
    /**
     * Generates an S3 key (path) for a weather data record.
     * Format: speed-layer/{source}/{type}/{year}/{month}/{day}/{station}_{timestamp}.json
     * <p>
     * Example: speed-layer/noaa/metar/2025/10/25/KJFK_20251025_1430.json
     * 
     * @param weatherData the weather data
     * @return the S3 key
     */
    private String generateS3Key(WeatherData weatherData) {
        String source = weatherData.getSource().toString().toLowerCase();
        String reportType = weatherData.getDataType().toLowerCase();
        String stationId = weatherData.getStationId();
        
        // Convert Instant to LocalDateTime in UTC for partitioning
        LocalDateTime ingestionDateTime = LocalDateTime.ofInstant(
                weatherData.getIngestionTime(), 
                ZoneId.of("UTC")
        );

        // Extract date components for partitioning
        int year = ingestionDateTime.getYear();
        int month = ingestionDateTime.getMonthValue();
        int day = ingestionDateTime.getDayOfMonth();

        // Create timestamp for filename
        String timestamp = ingestionDateTime.format(TIMESTAMP_FORMAT);
        
        // Build hierarchical key
        return String.format("speed-layer/%s/%s/%d/%02d/%02d/%s_%s.json",
                source, reportType, year, month, day, stationId, timestamp);
    }
    
    /**
     * Uploads raw weather data (for archival purposes).
     * Stores the original response from the weather API without processing.
     * 
     * @param source the data source (e.g., "noaa", "openweather")
     * @param rawData the raw data string
     * @param stationId the station identifier
     * @return the S3 key where data was stored
     * @throws IOException if upload fails
     */
    public String uploadRawData(String source, String rawData, String stationId) throws IOException {
        // Add null checks FIRST
        if (source == null || source.isEmpty()) {
            throw new IOException("Source cannot be null or empty");
        }
        
        if (rawData == null || rawData.isEmpty()) {
            throw new IOException("Raw data cannot be null or empty");
        }
        
        if (stationId == null || stationId.isEmpty()) {
            throw new IOException("Station ID cannot be null or empty");
        }
    
        String timestamp = java.time.LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String s3Key = String.format("raw-data/%s/%s_%s.txt", 
                source.toLowerCase(), stationId, timestamp);
        
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType("text/plain")
                    .build();
            
            s3Client.putObject(putRequest, RequestBody.fromString(rawData));
            
            logger.info("Uploaded raw data to S3: {}", s3Key);
            return s3Key;
            
        } catch (S3Exception e) {
            throw new IOException("Failed to upload raw data: " + e.awsErrorDetails().errorMessage(), e);
        } catch (RuntimeException e) {
            throw new IOException("Failed to upload raw data to S3", e);
        }
    }
    
    /**
     * Checks if the S3 bucket exists and is accessible.
     * 
     * @return true if bucket is accessible
     */
    public boolean isBucketAccessible() {
        try {
            s3Client.headBucket(builder -> builder.bucket(bucketName));
            if (logger.isInfoEnabled()) {
                logger.info("S3 bucket {} is accessible", bucketName);
            }
            return true;
        } catch (S3Exception e) {
            logger.error("S3 bucket {} is not accessible: {}", bucketName, e.getMessage());
            return false;
        } catch (RuntimeException e) {
            // Catch runtime exceptions (network errors, connection failures, etc.)
            logger.error("Error checking S3 bucket {} accessibility: {}", bucketName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Closes the S3 client and releases resources.
     */
    public void close() {
        if (s3Client != null) {
            s3Client.close();
            logger.info("S3UploadService closed");
        }
    }
}
