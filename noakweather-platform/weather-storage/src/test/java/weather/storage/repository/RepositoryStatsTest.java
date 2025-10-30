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

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RepositoryStats model class.
 * Focuses on equals, hashCode, and edge cases.
 * 
 * @author bclasky1539
 *
 */
class RepositoryStatsTest {
    
    @Test
    void testEqualsSameReference() {
        RepositoryStats stats = new RepositoryStats(
            100L, LocalDateTime.now(), LocalDateTime.now(), 
            10L, 50L, 5, 1024L
        );
        
        assertEquals(stats, stats, "Same reference should be equal");
    }
    
    @Test
    void testEqualsNull() {
        RepositoryStats stats = new RepositoryStats(
            100L, LocalDateTime.now(), LocalDateTime.now(), 
            10L, 50L, 5, 1024L
        );
        
        assertNotEquals(null, stats, "Stats should not equal null");
    }
    
    @Test
    @SuppressWarnings("java:S5838") // Intentional: tests equals() implementation
    void testEqualsDifferentType() {
        RepositoryStats stats = new RepositoryStats(
            100L, LocalDateTime.now(), LocalDateTime.now(), 
            10L, 50L, 5, 1024L
        );
        
        assertNotEquals(stats, "not a RepositoryStats", 
                       "Stats should not equal different type");
        
    }
    
    @Test
    void testEqualsIdenticalValues() {
        LocalDateTime oldest = LocalDateTime.now().minusDays(7);
        LocalDateTime newest = LocalDateTime.now();
        
        RepositoryStats stats1 = new RepositoryStats(
            100L, oldest, newest, 10L, 50L, 5, 1024L
        );
        RepositoryStats stats2 = new RepositoryStats(
            100L, oldest, newest, 10L, 50L, 5, 1024L
        );
        
        assertEquals(stats1, stats2, "Stats with identical values should be equal");
        assertEquals(stats1.hashCode(), stats2.hashCode(), 
                    "Equal stats should have same hash code");
    }
    
    @Test
    void testEqualsDifferentTotalRecordCount() {
        LocalDateTime now = LocalDateTime.now();
        RepositoryStats stats1 = new RepositoryStats(
            100L, now, now, 10L, 50L, 5, 1024L
        );
        RepositoryStats stats2 = new RepositoryStats(
            200L, now, now, 10L, 50L, 5, 1024L
        );
        
        assertNotEquals(stats1, stats2,
                       "Stats with different record counts should not be equal");
    }
    
    @Test
    void testEqualsDifferentRecordsLast24Hours() {
        LocalDateTime now = LocalDateTime.now();
        RepositoryStats stats1 = new RepositoryStats(
            100L, now, now, 10L, 50L, 5, 1024L
        );
        RepositoryStats stats2 = new RepositoryStats(
            100L, now, now, 20L, 50L, 5, 1024L
        );
        
        assertNotEquals(stats1, stats2,
                       "Stats with different 24h records should not be equal");
    }
    
    @Test
    void testEqualsDifferentRecordsLast7Days() {
        LocalDateTime now = LocalDateTime.now();
        RepositoryStats stats1 = new RepositoryStats(
            100L, now, now, 10L, 50L, 5, 1024L
        );
        RepositoryStats stats2 = new RepositoryStats(
            100L, now, now, 10L, 100L, 5, 1024L
        );
        
        assertNotEquals(stats1, stats2,
                       "Stats with different 7d records should not be equal");
    }
    
    @Test
    void testEqualsDifferentUniqueStationCount() {
        LocalDateTime now = LocalDateTime.now();
        RepositoryStats stats1 = new RepositoryStats(
            100L, now, now, 10L, 50L, 5, 1024L
        );
        RepositoryStats stats2 = new RepositoryStats(
            100L, now, now, 10L, 50L, 10, 1024L
        );
        
        assertNotEquals(stats1, stats2,
                       "Stats with different station counts should not be equal");
    }
    
    @Test
    void testEqualsDifferentStorageSize() {
        LocalDateTime now = LocalDateTime.now();
        RepositoryStats stats1 = new RepositoryStats(
            100L, now, now, 10L, 50L, 5, 1024L
        );
        RepositoryStats stats2 = new RepositoryStats(
            100L, now, now, 10L, 50L, 5, 2048L
        );
        
        assertNotEquals(stats1, stats2,
                       "Stats with different storage sizes should not be equal");
    }
    
    @Test
    void testEqualsDifferentOldestRecordTime() {
        LocalDateTime oldest1 = LocalDateTime.now().minusDays(7);
        LocalDateTime oldest2 = LocalDateTime.now().minusDays(14);
        LocalDateTime newest = LocalDateTime.now();
        
        RepositoryStats stats1 = new RepositoryStats(
            100L, oldest1, newest, 10L, 50L, 5, 1024L
        );
        RepositoryStats stats2 = new RepositoryStats(
            100L, oldest2, newest, 10L, 50L, 5, 1024L
        );
        
        assertNotEquals(stats1, stats2,
                       "Stats with different oldest times should not be equal");
    }
    
    @Test
    void testEqualsDifferentNewestRecordTime() {
        LocalDateTime oldest = LocalDateTime.now().minusDays(7);
        LocalDateTime newest1 = LocalDateTime.now();
        LocalDateTime newest2 = LocalDateTime.now().minusHours(1);
        
        RepositoryStats stats1 = new RepositoryStats(
            100L, oldest, newest1, 10L, 50L, 5, 1024L
        );
        RepositoryStats stats2 = new RepositoryStats(
            100L, oldest, newest2, 10L, 50L, 5, 1024L
        );
        
        assertNotEquals(stats1, stats2,
                       "Stats with different newest times should not be equal");
    }
    
    @Test
    void testEqualsWithNullOldestRecordTime() {
        LocalDateTime newest = LocalDateTime.now();
        
        RepositoryStats stats1 = new RepositoryStats(
            100L, null, newest, 10L, 50L, 5, 1024L
        );
        RepositoryStats stats2 = new RepositoryStats(
            100L, null, newest, 10L, 50L, 5, 1024L
        );
        
        assertEquals(stats1, stats2,
                    "Stats with both null oldest times should be equal");
    }
    
    @Test
    void testEqualsOneNullOldestRecordTime() {
        LocalDateTime oldest = LocalDateTime.now().minusDays(7);
        LocalDateTime newest = LocalDateTime.now();
        
        RepositoryStats stats1 = new RepositoryStats(
            100L, oldest, newest, 10L, 50L, 5, 1024L
        );
        RepositoryStats stats2 = new RepositoryStats(
            100L, null, newest, 10L, 50L, 5, 1024L
        );
        
        assertNotEquals(stats1, stats2,
                       "Stats with one null oldest time should not be equal");
    }
    
    @Test
    void testEqualsWithNullNewestRecordTime() {
        LocalDateTime oldest = LocalDateTime.now().minusDays(7);
        
        RepositoryStats stats1 = new RepositoryStats(
            100L, oldest, null, 10L, 50L, 5, 1024L
        );
        RepositoryStats stats2 = new RepositoryStats(
            100L, oldest, null, 10L, 50L, 5, 1024L
        );
        
        assertEquals(stats1, stats2,
                    "Stats with both null newest times should be equal");
    }
    
    @Test
    void testEqualsOneNullNewestRecordTime() {
        LocalDateTime oldest = LocalDateTime.now().minusDays(7);
        LocalDateTime newest = LocalDateTime.now();
        
        RepositoryStats stats1 = new RepositoryStats(
            100L, oldest, newest, 10L, 50L, 5, 1024L
        );
        RepositoryStats stats2 = new RepositoryStats(
            100L, oldest, null, 10L, 50L, 5, 1024L
        );
        
        assertNotEquals(stats1, stats2,
                       "Stats with one null newest time should not be equal");
    }
    
    @Test
    void testToStringContainsExpectedContent() {
        RepositoryStats stats = new RepositoryStats(
            100L, LocalDateTime.now(), LocalDateTime.now(), 
            10L, 50L, 5, 1024L
        );
        
        String result = stats.toString();
        
        assertNotNull(result, "toString should not return null");
        assertTrue(result.contains("RepositoryStats"), 
                  "toString should contain class name");
        assertTrue(result.contains("100"), 
                  "toString should contain record count");
        assertTrue(result.contains("5"), 
                  "toString should contain station count");
    }
}
