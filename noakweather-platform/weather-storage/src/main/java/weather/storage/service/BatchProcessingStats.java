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
package weather.storage.service;

import java.time.Instant;

/**
 * Statistics about batch processing performance over time.
 * <p>
 * Tracks aggregate metrics for monitoring and optimization of the batch layer.
 * Similar to database performance counters or ETL job statistics.
 * <p>
 * UPDATED v1.12.0-SNAPSHOT:
 * - Changed from LocalDateTime to Instant for consistency with WeatherData domain model
 * - Converted from class to record for immutability and conciseness
 * - Preserved computed methods for success rate and throughput calculations
 *
 * @param totalBatchesProcessed total number of batches processed
 * @param totalRecordsProcessed total number of records successfully processed
 * @param totalRecordsFailed total number of records that failed processing
 * @param lastBatchTime timestamp of the last batch processed (UTC), or null if no batches
 * @param averageProcessingTimeMs average processing time per batch in milliseconds
 *
 * @author bclasky1539
 *
 */
public record BatchProcessingStats(
        int totalBatchesProcessed,
        long totalRecordsProcessed,
        long totalRecordsFailed,
        Instant lastBatchTime,
        long averageProcessingTimeMs
) {
    /**
     * Calculates the overall success rate across all processed records.
     *
     * @return success rate as a decimal (0.0 to 1.0), or 0.0 if no records processed
     */
    public double getOverallSuccessRate() {
        long total = totalRecordsProcessed + totalRecordsFailed;
        return total > 0 ? (double) totalRecordsProcessed / total : 0.0;
    }

    /**
     * Calculates the average throughput in records per second.
     *
     * @return throughput in records/second, or 0.0 if insufficient data
     */
    public double getAverageThroughput() {
        if (averageProcessingTimeMs > 0 && totalBatchesProcessed > 0) {
            return (double) totalRecordsProcessed / totalBatchesProcessed /
                    (averageProcessingTimeMs / 1000.0);
        }
        return 0.0;
    }

    /**
     * Custom toString with formatted output for monitoring dashboards.
     *
     * @return formatted string representation with computed metrics
     */
    @Override
    public String toString() {
        return String.format(
                "BatchProcessingStats{batches=%d, processed=%d, failed=%d, " +
                        "successRate=%.2f%%, avgTimeMs=%d, throughput=%.2f rec/sec, lastBatch=%s}",
                totalBatchesProcessed, totalRecordsProcessed, totalRecordsFailed,
                getOverallSuccessRate() * 100, averageProcessingTimeMs,
                getAverageThroughput(), lastBatchTime
        );
    }
}
