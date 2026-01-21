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
package weather.storage.repository.snowflake;

import weather.model.WeatherData;
import weather.model.WeatherDataSource;
import weather.storage.repository.RepositoryStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SnowflakeRepository stub implementation.
 * <p>
 * UPDATED v1.12.0-SNAPSHOT: Changed from LocalDateTime to Instant for consistency
 * with WeatherData domain model.
 *
 * @author bclasky1539
 *
 */
class SnowflakeRepositoryTest {

    private SnowflakeRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SnowflakeRepository();
    }

    @Test
    void testRepositoryIsInitialized() {
        assertNotNull(repository);
    }

    @Test
    void testIsHealthyReturnsFalse() {
        assertFalse(repository.isHealthy());
    }

    @Test
    void testGetStatsReturnsZeroStats() {
        RepositoryStats stats = repository.getStats();

        assertNotNull(stats);
        assertEquals(0L, stats.totalRecordCount());
    }

    @Test
    void testAllQueryMethodsReturnEmpty() {
        assertTrue(repository.findByStationAndTimeRange(
                "KJFK", Instant.now(), Instant.now()
        ).isEmpty());

        assertFalse(repository.findByStationAndTime(
                "KJFK", Instant.now()
        ).isPresent());

        assertFalse(repository.findLatestByStation("KJFK").isPresent());

        assertTrue(repository.findBySourceAndTimeRange(
                WeatherDataSource.NOAA, Instant.now(), Instant.now()
        ).isEmpty());

        assertTrue(repository.findByStationsAndTime(
                Collections.singletonList("KJFK"), Instant.now()
        ).isEmpty());
    }

    @Test
    void testSaveThrowsException() {
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> repository.save(null)
        );

        assertTrue(exception.getMessage().contains("not yet implemented"));
    }

    @Test
    void testSaveBatchThrowsException() {
        List<WeatherData> emptyList = Collections.emptyList();

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> repository.saveBatch(emptyList)
        );

        // assertThrows guarantees non-null, optionally verify message:
        assertTrue(exception.getMessage().contains("not yet implemented"));
    }

    @Test
    void testDeleteOlderThanThrowsException() {
        Instant cutoffDate = Instant.now();

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> repository.deleteOlderThan(cutoffDate)
        );

        // Optionally verify message contains expected text
        assertTrue(exception.getMessage().contains("not yet implemented"));
    }
}
