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
package weather.storage.service;

import weather.model.WeatherData;
import weather.model.WeatherDataSource;
import weather.storage.repository.UniversalWeatherRepository;
import weather.storage.repository.snowflake.SnowflakeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BatchLayerProcessor.
 * 
 * These tests verify the batch processing service contract.
 * During Week 1, tests verify stub behavior and basic functionality.
 * 
 * @author bclasky1539
 *
 */
class BatchLayerProcessorTest {
    
    private BatchLayerProcessor processor;
    private UniversalWeatherRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = new SnowflakeRepository();
        processor = new BatchLayerProcessor(repository);
    }
    
    @Test
    void testProcessorIsInitialized() {
        assertNotNull(processor, "Processor should be initialized");
    }
    
    @Test
    void testProcessBatchThrowsUnsupportedOperation() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> processor.processBatch(WeatherDataSource.NOAA, start, end),
            "Stub processBatch() should throw UnsupportedOperationException"
        );
        
        assertNotNull(exception, "Exception should not be null");
    }
    
    @Test
    void testProcessBatchAsyncReturnsCompletableFuture() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        
        CompletableFuture<BatchProcessingResult> future = 
            processor.processBatchAsync(WeatherDataSource.NOAA, start, end);
        
        assertNotNull(future, "Async processing should return a CompletableFuture");
        assertTrue(future.isCompletedExceptionally() || !future.isDone(),
                  "Future should be exceptional or not done for stub");
    }
    
    @Test
    void testRecomputeViewsThrowsUnsupportedOperation() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> processor.recomputeViews(start, end),
            "Stub recomputeViews() should throw UnsupportedOperationException"
        );
        
        assertNotNull(exception, "Exception should not be null");
    }
    
    @Test
    void testProcessWeatherDataListWithNullReturnsZero() {
        int result = processor.processWeatherDataList(null);
        assertEquals(0, result, "Processing null list should return 0");
    }
    
    @Test
    void testProcessWeatherDataListWithEmptyListReturnsZero() {
        int result = processor.processWeatherDataList(Collections.emptyList());
        assertEquals(0, result, "Processing empty list should return 0");
    }
    
    @Test
    void testIsHealthyReturnsFalseForStubRepository() {
        assertFalse(processor.isHealthy(), 
                   "Processor with stub repository should not be healthy");
    }
    
    @Test
    void testGetStatsReturnsValidStats() {
        BatchProcessingStats stats = processor.getStats();
        
        assertNotNull(stats, "Stats should not be null");
        assertEquals(0, stats.getTotalBatchesProcessed(), 
                    "Stub should have zero batches processed");
        assertEquals(0L, stats.getTotalRecordsProcessed(), 
                    "Stub should have zero records processed");
    }
    
    @Test
    void testBatchProcessingStatsCalculations() {
        BatchProcessingStats stats = new BatchProcessingStats(
            10,    // totalBatchesProcessed
            1000L, // totalRecordsProcessed
            50L,   // totalRecordsFailed
            LocalDateTime.now(),
            5000L  // averageProcessingTimeMs
        );
        
        assertEquals(10, stats.getTotalBatchesProcessed());
        assertEquals(1000L, stats.getTotalRecordsProcessed());
        assertEquals(50L, stats.getTotalRecordsFailed());
        
        double successRate = stats.getOverallSuccessRate();
        assertTrue(successRate > 0.9 && successRate < 1.0, 
                  "Success rate should be around 95%");
        
        double throughput = stats.getAverageThroughput();
        assertTrue(throughput > 0, "Throughput should be positive");
    }
    
    @Test
    void testBatchProcessingResultBuilderSuccess() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingResult result = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .startTime(now.minusHours(1))
            .endTime(now)
            .processingStartTime(now.minusMinutes(30))
            .processingEndTime(now)
            .recordsProcessed(100L)
            .recordsFailed(5L)
            .recordsSkipped(2L)
            .statusMessage("Test batch completed")
            .build();
        
        assertTrue(result.isSuccess(), "Result should be successful");
        assertEquals(WeatherDataSource.NOAA, result.getSource());
        assertEquals(100L, result.getRecordsProcessed());
        assertEquals(107L, result.getTotalRecords());
        assertTrue(result.getSuccessRate() > 0.9, "Success rate should be high");
    }
    
    @Test
    void testBatchProcessingResultBuilderFailure() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingResult result = BatchProcessingResult.failure()
            .source(WeatherDataSource.NOAA)
            .startTime(now.minusHours(1))
            .endTime(now)
            .addError("Connection timeout")
            .addError("Invalid data format")
            .statusMessage("Batch failed")
            .build();
        
        assertFalse(result.isSuccess(), "Result should be failed");
        assertEquals(2, result.getErrors().size(), "Should have 2 errors");
        assertTrue(result.getErrors().contains("Connection timeout"));
    }
    
    @Test
    void testBatchProcessingResultToString() {
        BatchProcessingResult result = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .recordsProcessed(100L)
            .build();
        
        String resultString = result.toString();
        assertNotNull(resultString, "toString should not return null");
        assertTrue(resultString.contains("BatchProcessingResult"), 
                  "toString should contain class name");
        assertTrue(resultString.contains("NOAA"), 
                  "toString should contain source");
    }
    
    @Test
    void testBatchProcessingResultEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingResult result1 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .startTime(now)
            .endTime(now)
            .processingStartTime(now)
            .processingEndTime(now)
            .recordsProcessed(100L)
            .build();
        
        BatchProcessingResult result2 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .startTime(now)
            .endTime(now)
            .processingStartTime(now)
            .processingEndTime(now)
            .recordsProcessed(100L)
            .build();
        
        assertEquals(result1, result2, "Equal results should be equal");
        assertEquals(result1.hashCode(), result2.hashCode(), 
                    "Equal results should have same hash code");
    }

    @Test
    void testBatchProcessingResultWithNullDuration() {
        BatchProcessingResult result = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .recordsProcessed(100L)
            .build();
        
        assertEquals(java.time.Duration.ZERO, result.getProcessingDuration(),
                    "Null times should result in zero duration");
    }
    
    @Test
    void testBatchProcessingResultSuccessRateWithZeroRecords() {
        BatchProcessingResult result = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .build();
        
        assertEquals(0.0, result.getSuccessRate(),
                    "Zero records should result in 0.0 success rate");
    }
    
    @Test
    void testBatchProcessingResultGetErrorsReturnsDefensiveCopy() {
        BatchProcessingResult result = BatchProcessingResult.failure()
            .addError("Error 1")
            .build();
        
        List<String> errors = result.getErrors();
        errors.add("Error 2");
        
        assertEquals(1, result.getErrors().size(),
                    "Modifying returned list should not affect internal state");
    }

    @Test
    void testBatchProcessingStatsEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingStats stats1 = new BatchProcessingStats(
            10, 1000L, 50L, now, 5000L
        );
        BatchProcessingStats stats2 = new BatchProcessingStats(
            10, 1000L, 50L, now, 5000L
        );
        
        assertEquals(stats1, stats2);
        assertEquals(stats1.hashCode(), stats2.hashCode());
    }
    
    @Test
    void testBatchProcessingStatsWithZeroValues() {
        BatchProcessingStats stats = new BatchProcessingStats(
            0, 0L, 0L, null, 0L
        );
        
        assertEquals(0.0, stats.getOverallSuccessRate());
        assertEquals(0.0, stats.getAverageThroughput());
    }
    
    @Test
    void testBatchProcessingStatsToString() {
        BatchProcessingStats stats = new BatchProcessingStats(
            10, 1000L, 50L, LocalDateTime.now(), 5000L
        );
        
        String result = stats.toString();
        assertNotNull(result);
        assertTrue(result.contains("BatchProcessingStats"));
    }
    
    @Test
    void testBatchProcessingResultWithNullProcessingStartTime() {
        BatchProcessingResult result = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .processingEndTime(LocalDateTime.now())
            .recordsProcessed(100L)
            .build();
        
        assertEquals(java.time.Duration.ZERO, result.getProcessingDuration(),
                    "Null start time should result in zero duration");
    }
    
    @Test
    void testBatchProcessingResultWithNullProcessingEndTime() {
        BatchProcessingResult result = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .processingStartTime(LocalDateTime.now())
            .recordsProcessed(100L)
            .build();
        
        assertEquals(java.time.Duration.ZERO, result.getProcessingDuration(),
                    "Null end time should result in zero duration");
    }
    
    @Test
    void testBatchProcessingResultWithBothNullProcessingTimes() {
        BatchProcessingResult result = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .recordsProcessed(100L)
            .build();
        
        assertEquals(java.time.Duration.ZERO, result.getProcessingDuration(),
                    "Both null times should result in zero duration");
    }

    @Test
    void testBatchProcessingResultEqualsSameReference() {
        BatchProcessingResult result = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .recordsProcessed(100L)
            .build();
        
        assertEquals(result, result, "Same reference should be equal");
    }
    
    @Test
    void testBatchProcessingResultEqualsNull() {
        BatchProcessingResult result = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .recordsProcessed(100L)
            .build();
        
        assertNotEquals(null, result, "Result should not equal null");
    }
    
    @Test
    @SuppressWarnings("java:S5838") // Intentional: tests equals() implementation
    void testBatchProcessingResultEqualsDifferentType() {
        BatchProcessingResult result = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .recordsProcessed(100L)
            .build();
        
        assertNotEquals(result, "not a BatchProcessingResult", 
                       "Result should not equal different type");
    }

    @Test
    void testBatchProcessingResultEqualsDifferentSuccess() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingResult result1 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .startTime(now)
            .endTime(now)
            .recordsProcessed(100L)
            .build();
        
        BatchProcessingResult result2 = BatchProcessingResult.failure()
            .source(WeatherDataSource.NOAA)
            .startTime(now)
            .endTime(now)
            .recordsProcessed(100L)
            .build();
        
        assertNotEquals(result1, result2, 
                       "Results with different success status should not be equal");
    }
    
    @Test
    void testBatchProcessingResultEqualsDifferentRecordsProcessed() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingResult result1 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .startTime(now)
            .recordsProcessed(100L)
            .build();
        
        BatchProcessingResult result2 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .startTime(now)
            .recordsProcessed(200L)
            .build();
        
        assertNotEquals(result1, result2, 
                       "Results with different processed records should not be equal");
    }

    @Test
    void testBatchProcessingResultEqualsDifferentRecordsFailed() {
        BatchProcessingResult result1 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .recordsFailed(10L)
            .build();
        
        BatchProcessingResult result2 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .recordsFailed(20L)
            .build();
        
        assertNotEquals(result1, result2, 
                       "Results with different failed records should not be equal");
    }
    
    @Test
    void testBatchProcessingResultEqualsDifferentRecordsSkipped() {
        BatchProcessingResult result1 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .recordsSkipped(5L)
            .build();
        
        BatchProcessingResult result2 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .recordsSkipped(10L)
            .build();
        
        assertNotEquals(result1, result2, 
                       "Results with different skipped records should not be equal");
    }

    @Test
    void testBatchProcessingResultEqualsDifferentSource() {
        BatchProcessingResult result1 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .recordsProcessed(100L)
            .build();
        
        BatchProcessingResult result2 = BatchProcessingResult.success()
            .source(null)
            .recordsProcessed(100L)
            .build();
        
        assertNotEquals(result1, result2, 
                       "Results with different sources should not be equal");
    }

    @Test
    void testBatchProcessingResultEqualsDifferentStartTime() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingResult result1 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .startTime(now)
            .build();
        
        BatchProcessingResult result2 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .startTime(now.plusHours(1))
            .build();
        
        assertNotEquals(result1, result2, 
                       "Results with different start times should not be equal");
    }

    @Test
    void testBatchProcessingResultEqualsDifferentEndTime() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingResult result1 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .endTime(now)
            .build();
        
        BatchProcessingResult result2 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .endTime(now.plusHours(1))
            .build();
        
        assertNotEquals(result1, result2, 
                       "Results with different end times should not be equal");
    }
    
    @Test
    void testBatchProcessingResultEqualsDifferentProcessingStartTime() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingResult result1 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .processingStartTime(now)
            .build();
        
        BatchProcessingResult result2 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .processingStartTime(now.plusMinutes(5))
            .build();
        
        assertNotEquals(result1, result2, 
                       "Results with different processing start times should not be equal");
    }

    @Test
    void testBatchProcessingResultEqualsDifferentProcessingEndTime() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingResult result1 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .processingEndTime(now)
            .build();
        
        BatchProcessingResult result2 = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .processingEndTime(now.plusMinutes(10))
            .build();
        
        assertNotEquals(result1, result2, 
                       "Results with different processing end times should not be equal");
    }
    
    @Test
    void testBatchProcessingStatsEqualsSameReference() {
        BatchProcessingStats stats = new BatchProcessingStats(
            10, 1000L, 50L, LocalDateTime.now(), 5000L
        );
        
        assertEquals(stats, stats, "Same reference should be equal");
    }
    
    @Test
    void testBatchProcessingStatsEqualsNull() {
        BatchProcessingStats stats = new BatchProcessingStats(
            10, 1000L, 50L, LocalDateTime.now(), 5000L
        );
        
        assertNotEquals(null, stats, "Stats should not equal null");
    }

    @Test
    @SuppressWarnings("java:S5838") // Intentional: tests equals() implementation
    void testBatchProcessingStatsEqualsDifferentType() {
        BatchProcessingStats stats = new BatchProcessingStats(
            10, 1000L, 50L, LocalDateTime.now(), 5000L
        );
        
        assertNotEquals(stats, "not stats", 
                       "Stats should not equal different type");
    }
    
    @Test
    void testBatchProcessingStatsEqualsDifferentValues() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingStats stats1 = new BatchProcessingStats(
            10, 1000L, 50L, now, 5000L
        );
        BatchProcessingStats stats2 = new BatchProcessingStats(
            20, 1000L, 50L, now, 5000L
        );
        
        assertNotEquals(stats1, stats2, 
                       "Stats with different batches should not be equal");
    }
    
    @Test
    void testBatchProcessingResultWithValidProcessingDuration() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMinutes(30);
        
        BatchProcessingResult result = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .processingStartTime(start)
            .processingEndTime(end)
            .recordsProcessed(100L)
            .build();
        
        java.time.Duration duration = result.getProcessingDuration();
        
        assertNotNull(duration, "Duration should not be null");
        assertEquals(30, duration.toMinutes(), 
                    "Duration should be 30 minutes");
    }
    
    @Test
    void testBatchProcessingResultWithStatusMessage() {
        String statusMessage = "Batch completed successfully with 100 records";
        
        BatchProcessingResult result = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .recordsProcessed(100L)
            .statusMessage(statusMessage)
            .build();
        
        assertEquals(statusMessage, result.getStatusMessage(),
                    "Status message should match");
    }

    @Test
    void testBatchProcessingResultWithNullStatusMessage() {
        BatchProcessingResult result = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .recordsProcessed(100L)
            .build();
        
        assertNull(result.getStatusMessage(),
                  "Status message should be null when not set");
    }
    
    @Test
    void testBatchProcessingResultWithEmptyStatusMessage() {
        BatchProcessingResult result = BatchProcessingResult.success()
            .source(WeatherDataSource.NOAA)
            .recordsProcessed(100L)
            .statusMessage("")
            .build();
        
        assertEquals("", result.getStatusMessage(),
                    "Empty status message should be preserved");
    }
    
    @Test
    void testBatchProcessingStatsWithZeroAverageProcessingTime() {
        BatchProcessingStats stats = new BatchProcessingStats(
            10,    // totalBatchesProcessed > 0
            1000L, // totalRecordsProcessed
            50L,   // totalRecordsFailed
            LocalDateTime.now(),
            0L     // averageProcessingTimeMs = 0
        );
        
        assertEquals(0.0, stats.getAverageThroughput(),
                    "Zero processing time should result in 0.0 throughput");
    }
    
    @Test
    void testBatchProcessingStatsWithZeroBatchesProcessed() {
        BatchProcessingStats stats = new BatchProcessingStats(
            0,     // totalBatchesProcessed = 0
            1000L, // totalRecordsProcessed
            50L,   // totalRecordsFailed
            LocalDateTime.now(),
            5000L  // averageProcessingTimeMs > 0
        );
        
        assertEquals(0.0, stats.getAverageThroughput(),
                    "Zero batches should result in 0.0 throughput");
    }
    
    @Test
    void testBatchProcessingStatsWithBothZero() {
        BatchProcessingStats stats = new BatchProcessingStats(
            0,     // totalBatchesProcessed = 0
            0L,    // totalRecordsProcessed
            0L,    // totalRecordsFailed
            null,  // lastBatchTime
            0L     // averageProcessingTimeMs = 0
        );
        
        assertEquals(0.0, stats.getAverageThroughput(),
                    "Both zero should result in 0.0 throughput");
    }

    @Test
    void testBatchProcessingStatsWithValidThroughput() {
        BatchProcessingStats stats = new BatchProcessingStats(
            10,    // 10 batches
            1000L, // 1000 records
            50L,   // 50 failed
            LocalDateTime.now(),
            5000L  // 5 seconds average per batch
        );
        
        double throughput = stats.getAverageThroughput();
        assertTrue(throughput > 0, "Valid stats should have positive throughput");
        // throughput = 1000 / 10 / 5 = 20 records per second
        assertEquals(20.0, throughput, 0.01, 
                    "Throughput calculation should be accurate");
    }
    
    @Test
    void testBatchProcessingStatsEqualsDifferentTotalBatchesProcessed() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingStats stats1 = new BatchProcessingStats(
            10, 1000L, 50L, now, 5000L
        );
        BatchProcessingStats stats2 = new BatchProcessingStats(
            20, 1000L, 50L, now, 5000L
        );
        
        assertNotEquals(stats1, stats2,
                       "Stats with different batch counts should not be equal");
    }
    
    @Test
    void testBatchProcessingStatsEqualsDifferentTotalRecordsProcessed() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingStats stats1 = new BatchProcessingStats(
            10, 1000L, 50L, now, 5000L
        );
        BatchProcessingStats stats2 = new BatchProcessingStats(
            10, 2000L, 50L, now, 5000L
        );
        
        assertNotEquals(stats1, stats2,
                       "Stats with different records processed should not be equal");
    }

    @Test
    void testBatchProcessingStatsEqualsDifferentTotalRecordsFailed() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingStats stats1 = new BatchProcessingStats(
            10, 1000L, 50L, now, 5000L
        );
        BatchProcessingStats stats2 = new BatchProcessingStats(
            10, 1000L, 100L, now, 5000L
        );
        
        assertNotEquals(stats1, stats2,
                       "Stats with different records failed should not be equal");
    }
    
    @Test
    void testBatchProcessingStatsEqualsDifferentAverageProcessingTime() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingStats stats1 = new BatchProcessingStats(
            10, 1000L, 50L, now, 5000L
        );
        BatchProcessingStats stats2 = new BatchProcessingStats(
            10, 1000L, 50L, now, 10000L
        );
        
        assertNotEquals(stats1, stats2,
                       "Stats with different processing times should not be equal");
    }
    
    @Test
    void testBatchProcessingStatsEqualsDifferentLastBatchTime() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingStats stats1 = new BatchProcessingStats(
            10, 1000L, 50L, now, 5000L
        );
        BatchProcessingStats stats2 = new BatchProcessingStats(
            10, 1000L, 50L, now.plusHours(1), 5000L
        );
        
        assertNotEquals(stats1, stats2,
                       "Stats with different last batch times should not be equal");
    }

    @Test
    void testBatchProcessingStatsEqualsWithNullLastBatchTime() {
        BatchProcessingStats stats1 = new BatchProcessingStats(
            10, 1000L, 50L, null, 5000L
        );
        BatchProcessingStats stats2 = new BatchProcessingStats(
            10, 1000L, 50L, null, 5000L
        );
        
        assertEquals(stats1, stats2,
                    "Stats with both null last batch times should be equal");
    }
    
    @Test
    void testBatchProcessingStatsEqualsOneNullLastBatchTime() {
        LocalDateTime now = LocalDateTime.now();
        BatchProcessingStats stats1 = new BatchProcessingStats(
            10, 1000L, 50L, now, 5000L
        );
        BatchProcessingStats stats2 = new BatchProcessingStats(
            10, 1000L, 50L, null, 5000L
        );
        
        assertNotEquals(stats1, stats2,
                       "Stats with one null last batch time should not be equal");
    }
    
    @Test
    void testProcessBatchAsyncExecutesAndThrowsException() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        
        CompletableFuture<BatchProcessingResult> future = 
            processor.processBatchAsync(WeatherDataSource.NOAA, start, end);
        
        assertNotNull(future, "Future should not be null");
        
        // Execute the future and verify it throws the expected exception
        ExecutionException exception = assertThrows(
            ExecutionException.class,
            () -> future.get(),
            "Future should complete exceptionally"
        );
        
        assertNotNull(exception.getCause(), "Should have a cause");
        assertTrue(exception.getCause() instanceof UnsupportedOperationException,
                  "Cause should be UnsupportedOperationException");
    }
    
    @Test
    void testProcessWeatherDataListWithNonEmptyList() {
        // Create a mock list with some size
        // Since we can't create actual WeatherData without dependencies,
        // we can use a list of nulls just to test the size logic
        List<WeatherData> dataList = java.util.Arrays.asList(null, null, null);
        
        int result = processor.processWeatherDataList(dataList);
        
        assertEquals(3, result, 
                    "Should return the size of the list");
    }
    
    @Test
    void testIsHealthyWithNullRepository() {
        // Create processor with null repository
        BatchLayerProcessor processorWithNullRepo = 
            new BatchLayerProcessor(null);
        
        assertFalse(processorWithNullRepo.isHealthy(),
                   "Processor with null repository should not be healthy");
    }
}
