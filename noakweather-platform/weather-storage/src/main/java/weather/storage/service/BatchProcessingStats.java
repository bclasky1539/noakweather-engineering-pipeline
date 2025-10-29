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

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Statistics about batch processing performance over time.
 * 
 * Tracks aggregate metrics for monitoring and optimization of the batch layer.
 * Similar to database performance counters or ETL job statistics.
 * 
 * @author bclasky1539
 *
 */
public class BatchProcessingStats {
    
    private final int totalBatchesProcessed;
    private final long totalRecordsProcessed;
    private final long totalRecordsFailed;
    private final LocalDateTime lastBatchTime;
    private final long averageProcessingTimeMs;
    
    public BatchProcessingStats(int totalBatchesProcessed,
                               long totalRecordsProcessed,
                               long totalRecordsFailed,
                               LocalDateTime lastBatchTime,
                               long averageProcessingTimeMs) {
        this.totalBatchesProcessed = totalBatchesProcessed;
        this.totalRecordsProcessed = totalRecordsProcessed;
        this.totalRecordsFailed = totalRecordsFailed;
        this.lastBatchTime = lastBatchTime;
        this.averageProcessingTimeMs = averageProcessingTimeMs;
    }
    
    public int getTotalBatchesProcessed() {
        return totalBatchesProcessed;
    }
    
    public long getTotalRecordsProcessed() {
        return totalRecordsProcessed;
    }
    
    public long getTotalRecordsFailed() {
        return totalRecordsFailed;
    }
    
    public LocalDateTime getLastBatchTime() {
        return lastBatchTime;
    }
    
    public long getAverageProcessingTimeMs() {
        return averageProcessingTimeMs;
    }
    
    public double getOverallSuccessRate() {
        long total = totalRecordsProcessed + totalRecordsFailed;
        return total > 0 ? (double) totalRecordsProcessed / total : 0.0;
    }
    
    public double getAverageThroughput() {
        if (averageProcessingTimeMs > 0 && totalBatchesProcessed > 0) {
            return (double) totalRecordsProcessed / totalBatchesProcessed / 
                   (averageProcessingTimeMs / 1000.0);
        }
        return 0.0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BatchProcessingStats)) {
            return false;
        }
        BatchProcessingStats that = (BatchProcessingStats) o;
        return getTotalBatchesProcessed() == that.getTotalBatchesProcessed() &&
               getTotalRecordsProcessed() == that.getTotalRecordsProcessed() &&
               getTotalRecordsFailed() == that.getTotalRecordsFailed() &&
               getAverageProcessingTimeMs() == that.getAverageProcessingTimeMs() &&
               Objects.equals(getLastBatchTime(), that.getLastBatchTime());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getTotalBatchesProcessed(), getTotalRecordsProcessed(), 
                           getTotalRecordsFailed(), getLastBatchTime(), 
                           getAverageProcessingTimeMs());
    }
    
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
