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

import weather.model.WeatherDataSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Result of a batch processing operation.
 * 
 * Contains metrics and status information about a completed (or failed) batch job.
 * Useful for monitoring, alerting, and performance optimization.
 * 
 * @author bclasky1539
 *
 */
public class BatchProcessingResult {
    
    private final WeatherDataSource source;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final LocalDateTime processingStartTime;
    private final LocalDateTime processingEndTime;
    private final boolean success;
    private final long recordsProcessed;
    private final long recordsFailed;
    private final long recordsSkipped;
    private final List<String> errors;
    private final String statusMessage;
    
    private BatchProcessingResult(Builder builder) {
        this.source = builder.getSource();
        this.startTime = builder.getStartTime();
        this.endTime = builder.getEndTime();
        this.processingStartTime = builder.getProcessingStartTime();
        this.processingEndTime = builder.getProcessingEndTime();
        this.success = builder.isSuccess();
        this.recordsProcessed = builder.getRecordsProcessed();
        this.recordsFailed = builder.getRecordsFailed();
        this.recordsSkipped = builder.getRecordsSkipped();
        this.errors = new ArrayList<>(builder.getErrors());
        this.statusMessage = builder.getStatusMessage();
    }
    
    public WeatherDataSource getSource() {
        return source;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public LocalDateTime getProcessingStartTime() {
        return processingStartTime;
    }
    
    public LocalDateTime getProcessingEndTime() {
        return processingEndTime;
    }
    
    public Duration getProcessingDuration() {
        if (processingStartTime != null && processingEndTime != null) {
            return Duration.between(processingStartTime, processingEndTime);
        }
        return Duration.ZERO;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public long getRecordsProcessed() {
        return recordsProcessed;
    }
    
    public long getRecordsFailed() {
        return recordsFailed;
    }
    
    public long getRecordsSkipped() {
        return recordsSkipped;
    }
    
    public long getTotalRecords() {
        return recordsProcessed + recordsFailed + recordsSkipped;
    }
    
    public double getSuccessRate() {
        long total = getTotalRecords();
        return total > 0 ? (double) recordsProcessed / total : 0.0;
    }
    
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BatchProcessingResult)) {
            return false;
        }
        BatchProcessingResult that = (BatchProcessingResult) o;
        return isSuccess() == that.isSuccess() &&
               getRecordsProcessed() == that.getRecordsProcessed() &&
               getRecordsFailed() == that.getRecordsFailed() &&
               getRecordsSkipped() == that.getRecordsSkipped() &&
               getSource() == that.getSource() &&
               Objects.equals(getStartTime(), that.getStartTime()) &&
               Objects.equals(getEndTime(), that.getEndTime()) &&
               Objects.equals(getProcessingStartTime(), that.getProcessingStartTime()) &&
               Objects.equals(getProcessingEndTime(), that.getProcessingEndTime());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getSource(), getStartTime(), getEndTime(), 
                           getProcessingStartTime(), getProcessingEndTime(), 
                           isSuccess(), getRecordsProcessed(), 
                           getRecordsFailed(), getRecordsSkipped());
    }
    
    @Override
    public String toString() {
        return String.format(
            "BatchProcessingResult{source=%s, window=[%s to %s], duration=%s, " +
            "processed=%d, failed=%d, skipped=%d, successRate=%.2f%%, status=%s}",
            source, startTime, endTime, getProcessingDuration(),
            recordsProcessed, recordsFailed, recordsSkipped, 
            getSuccessRate() * 100, success ? "SUCCESS" : "FAILED"
        );
    }
    
    /**
     * Builder for creating BatchProcessingResult instances.
     */
    public static class Builder {
        private WeatherDataSource source;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime processingStartTime;
        private LocalDateTime processingEndTime;
        private boolean success;
        private long recordsProcessed;
        private long recordsFailed;
        private long recordsSkipped;
        private List<String> errors = new ArrayList<>();
        private String statusMessage;
        
        public Builder source(WeatherDataSource source) {
            this.source = source;
            return this;
        }
        
        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder processingStartTime(LocalDateTime processingStartTime) {
            this.processingStartTime = processingStartTime;
            return this;
        }
        
        public Builder processingEndTime(LocalDateTime processingEndTime) {
            this.processingEndTime = processingEndTime;
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder recordsProcessed(long recordsProcessed) {
            this.recordsProcessed = recordsProcessed;
            return this;
        }
        
        public Builder recordsFailed(long recordsFailed) {
            this.recordsFailed = recordsFailed;
            return this;
        }
        
        public Builder recordsSkipped(long recordsSkipped) {
            this.recordsSkipped = recordsSkipped;
            return this;
        }
        
        public Builder errors(List<String> errors) {
            this.errors = new ArrayList<>(errors);
            return this;
        }
        
        public Builder addError(String error) {
            this.errors.add(error);
            return this;
        }
        
        public Builder statusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
            return this;
        }
        
        public BatchProcessingResult build() {
            return new BatchProcessingResult(this);
        }
        
        // Package-private getters for BatchProcessingResult constructor
        WeatherDataSource getSource() {
            return source;
        }
        
        LocalDateTime getStartTime() {
            return startTime;
        }
        
        LocalDateTime getEndTime() {
            return endTime;
        }
        
        LocalDateTime getProcessingStartTime() {
            return processingStartTime;
        }
        
        LocalDateTime getProcessingEndTime() {
            return processingEndTime;
        }
        
        boolean isSuccess() {
            return success;
        }
        
        long getRecordsProcessed() {
            return recordsProcessed;
        }
        
        long getRecordsFailed() {
            return recordsFailed;
        }
        
        long getRecordsSkipped() {
            return recordsSkipped;
        }
        
        List<String> getErrors() {
            return errors;
        }
        
        String getStatusMessage() {
            return statusMessage;
        }
    }
    
    /**
     * Creates a builder for a successful batch result.
     * 
     * @return a new Builder with success set to true
     */
    public static Builder success() {
        return new Builder().success(true);
    }
    
    /**
     * Creates a builder for a failed batch result.
     * 
     * @return a new Builder with success set to false
     */
    public static Builder failure() {
        return new Builder().success(false);
    }
}
