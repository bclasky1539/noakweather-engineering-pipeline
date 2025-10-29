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
package weather.storage.repository;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Statistics and metrics about a weather data repository.
 * 
 * This class provides insight into the repository's contents and health,
 * useful for monitoring and operational dashboards.
 * 
 * @author bclasky1539
 *
 */
public class RepositoryStats {
    
    private final long totalRecordCount;
    private final LocalDateTime oldestRecordTime;
    private final LocalDateTime newestRecordTime;
    private final long recordsLast24Hours;
    private final long recordsLast7Days;
    private final int uniqueStationCount;
    private final long storageSize; // in bytes, if applicable
    
    public RepositoryStats(long totalRecordCount,
                          LocalDateTime oldestRecordTime,
                          LocalDateTime newestRecordTime,
                          long recordsLast24Hours,
                          long recordsLast7Days,
                          int uniqueStationCount,
                          long storageSize) {
        this.totalRecordCount = totalRecordCount;
        this.oldestRecordTime = oldestRecordTime;
        this.newestRecordTime = newestRecordTime;
        this.recordsLast24Hours = recordsLast24Hours;
        this.recordsLast7Days = recordsLast7Days;
        this.uniqueStationCount = uniqueStationCount;
        this.storageSize = storageSize;
    }
    
    // Getters
    public long getTotalRecordCount() {
        return totalRecordCount;
    }
    
    public LocalDateTime getOldestRecordTime() {
        return oldestRecordTime;
    }
    
    public LocalDateTime getNewestRecordTime() {
        return newestRecordTime;
    }
    
    public long getRecordsLast24Hours() {
        return recordsLast24Hours;
    }
    
    public long getRecordsLast7Days() {
        return recordsLast7Days;
    }
    
    public int getUniqueStationCount() {
        return uniqueStationCount;
    }
    
    public long getStorageSize() {
        return storageSize;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RepositoryStats)) {
            return false;
        }
        RepositoryStats that = (RepositoryStats) o;
        return getTotalRecordCount() == that.getTotalRecordCount() &&
               getRecordsLast24Hours() == that.getRecordsLast24Hours() &&
               getRecordsLast7Days() == that.getRecordsLast7Days() &&
               getUniqueStationCount() == that.getUniqueStationCount() &&
               getStorageSize() == that.getStorageSize() &&
               Objects.equals(getOldestRecordTime(), that.getOldestRecordTime()) &&
               Objects.equals(getNewestRecordTime(), that.getNewestRecordTime());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getTotalRecordCount(), getOldestRecordTime(), getNewestRecordTime(),
                           getRecordsLast24Hours(), getRecordsLast7Days(), 
                           getUniqueStationCount(), getStorageSize());
    }
    
    @Override
    public String toString() {
        return String.format(
            "RepositoryStats{totalRecords=%d, stations=%d, oldest=%s, newest=%s, last24h=%d, last7d=%d, size=%d bytes}",
            totalRecordCount, uniqueStationCount, oldestRecordTime, newestRecordTime,
            recordsLast24Hours, recordsLast7Days, storageSize
        );
    }
}
