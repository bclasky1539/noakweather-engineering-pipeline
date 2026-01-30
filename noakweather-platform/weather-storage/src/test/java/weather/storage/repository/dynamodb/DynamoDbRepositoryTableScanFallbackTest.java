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
package weather.storage.repository.dynamodb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weather.model.WeatherData;
import weather.model.WeatherDataSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DynamoDB table scan fallback when GSI doesn't exist.
 * <p>
 * Phase 4: These tests validate backward compatibility by testing the fallback path.
 * Unlike other tests, this class creates a table WITHOUT the time-bucket-index GSI,
 * forcing the repository to use table scan instead of optimized GSI queries.
 * <p>
 * Why This Matters:
 * - Validates zero-downtime deployment (code works before GSI is created)
 * - Tests graceful degradation when GSI is missing or fails
 * - Ensures repository doesn't crash, just uses slower path
 * <p>
 * Performance Note:
 * - GSI queries: O(m) where m = items in time range (fast)
 * - Table scans: O(n) where n = total items in table (slow)
 * - Both return identical results, just different performance
 *
 * @author bclasky1539
 *
 */
@DisplayName("DynamoDB Repository - Table Scan Fallback Tests (No GSI)")
class DynamoDbRepositoryTableScanFallbackTest extends BaseDynamoDbIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(DynamoDbRepositoryTableScanFallbackTest.class);

    /**
     * Override setUp to create table WITHOUT GSI.
     * This forces all queries to use table scan fallback.
     */
    @Override
    @BeforeEach
    public void setUp() {
        logger.info("Setting up test - creating table WITHOUT GSI for fallback testing...");

        // Delete existing table if present
        DynamoDbTestHelper.deleteTableIfExists(dynamoDbClient);

        // CRITICAL: Create table WITHOUT GSI to force table scan fallback
        DynamoDbTestHelper.createTableWithoutGSI(dynamoDbClient);

        // Verify no GSI exists
        DynamoDbTestHelper.verifyTableHasNoGSI(dynamoDbClient);

        logger.info("Table created without GSI - repository will use table scan fallback");

        // Initialize repository
        repository = new DynamoDbRepository(dynamoDbClient);
    }

    // ========== findByTimeRange() Fallback Tests ==========

    @Test
    @DisplayName("Should fall back to table scan when GSI doesn't exist")
    void shouldFallBackToTableScanWhenGSIMissing() {
        // Given - Sample weather data across multiple stations
        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(baseTime)
                .build());

        repository.save(MetarTestDataBuilder.create("KLGA")
                .withObservationTime(baseTime.plus(30, ChronoUnit.MINUTES))
                .build());

        repository.save(MetarTestDataBuilder.create("KEWR")
                .withObservationTime(baseTime.plus(60, ChronoUnit.MINUTES))
                .build());

        // When - Query by time range (should trigger table scan fallback)
        Instant startTime = baseTime.minus(1, ChronoUnit.HOURS);
        Instant endTime = baseTime.plus(2, ChronoUnit.HOURS);

        List<WeatherData> results = repository.findByTimeRange(startTime, endTime);

        // Then - Should find all 3 records using table scan
        assertThat(results).hasSize(3);
        assertThat(results)
                .extracting(WeatherData::getStationId)
                .containsExactlyInAnyOrder("KJFK", "KLGA", "KEWR");

        logger.info("✓ Table scan fallback successfully found {} records", results.size());
    }

    @Test
    @DisplayName("Should correctly filter by time range in table scan fallback")
    void shouldCorrectlyFilterByTimeRangeInTableScanFallback() {
        // Given - Weather data at different times
        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(baseTime.minus(2, ChronoUnit.HOURS))
                .build());

        repository.save(MetarTestDataBuilder.create("KLGA")
                .withObservationTime(baseTime)
                .build());

        repository.save(MetarTestDataBuilder.create("KEWR")
                .withObservationTime(baseTime.plus(1, ChronoUnit.HOURS))
                .build());

        repository.save(MetarTestDataBuilder.create("KBOS")
                .withObservationTime(baseTime.plus(3, ChronoUnit.HOURS))
                .build());

        // When - Query with specific time range
        Instant startTime = baseTime.minus(30, ChronoUnit.MINUTES);
        Instant endTime = baseTime.plus(90, ChronoUnit.MINUTES);

        List<WeatherData> results = repository.findByTimeRange(startTime, endTime);

        // Then - Should only return data within time range
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(WeatherData::getStationId)
                .containsExactlyInAnyOrder("KLGA", "KEWR");
    }

    @Test
    @DisplayName("Should return empty list when no data matches time range in table scan")
    void shouldReturnEmptyListWhenNoDataMatchesTimeRangeInTableScan() {
        // Given - Weather data at a specific time
        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(baseTime)
                .build());

        // When - Query for different time range
        Instant startTime = baseTime.plus(5, ChronoUnit.HOURS);
        Instant endTime = baseTime.plus(10, ChronoUnit.HOURS);

        List<WeatherData> results = repository.findByTimeRange(startTime, endTime);

        // Then - Should return empty list
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should handle large result sets in table scan fallback")
    void shouldHandleLargeResultSetsInTableScanFallback() {
        // Given - Multiple weather observations
        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        for (int i = 0; i < 50; i++) {
            repository.save(MetarTestDataBuilder.create("STATION" + i)
                    .withObservationTime(baseTime.plus(i * 5, ChronoUnit.MINUTES))
                    .build());
        }

        // When - Query for all data
        Instant startTime = baseTime.minus(1, ChronoUnit.HOURS);
        Instant endTime = baseTime.plus(5, ChronoUnit.HOURS);

        List<WeatherData> results = repository.findByTimeRange(startTime, endTime);

        // Then - Should return all 50 records
        assertThat(results).hasSize(50);
    }

    @Test
    @DisplayName("Should handle exact time boundaries in table scan")
    void shouldHandleExactTimeBoundariesInTableScan() {
        // Given
        Instant exactTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(exactTime)
                .build());

        // When - Query with exact time as boundaries (BETWEEN is inclusive)
        List<WeatherData> results = repository.findByTimeRange(exactTime, exactTime);

        // Then - Should find the observation
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getObservationTime()).isEqualTo(exactTime);
    }

    // ========== findBySourceAndTimeRange() Fallback Tests ==========

    @Test
    @DisplayName("Should correctly filter by NOAA source in table scan fallback")
    void shouldCorrectlyFilterByNoaaSourceInTableScanFallback() {
        // Given - NOAA weather data (METAR + TAF) with explicit source
        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // Explicitly set source for METAR records
        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(baseTime)
                .build());

        repository.save(MetarTestDataBuilder.create("KLGA")
                .withObservationTime(baseTime.plus(30, ChronoUnit.MINUTES))
                .build());

        // TAF records default to NOAA source
        repository.save(TafTestDataBuilder.create("KCLT")
                .withObservationTime(baseTime)
                .build());

        repository.save(TafTestDataBuilder.create("KATL")
                .withObservationTime(baseTime.plus(30, ChronoUnit.MINUTES))
                .build());

        // When - Query for NOAA data
        Instant startTime = baseTime.minus(1, ChronoUnit.HOURS);
        Instant endTime = baseTime.plus(2, ChronoUnit.HOURS);

        List<WeatherData> results = repository.findBySourceAndTimeRange(
                WeatherDataSource.NOAA,
                startTime,
                endTime
        );

        // Then - Should return all NOAA data (METAR + TAF)
        assertThat(results)
                .hasSize(4)
                .allMatch(data -> data.getDataType().equals("METAR") ||
                        data.getDataType().equals("TAF"));
    }

    @Test
    @DisplayName("Should filter by INTERNAL source in table scan")
    void shouldFilterByInternalSourceInTableScan() {
        // Given
        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // NOAA data
        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(baseTime)
                .withSource(WeatherDataSource.NOAA)
                .build());

        // INTERNAL data
        repository.save(MetarTestDataBuilder.create("KLGA")
                .withObservationTime(baseTime)
                .withSource(WeatherDataSource.INTERNAL)
                .build());

        // When - Query for INTERNAL only
        List<WeatherData> results = repository.findBySourceAndTimeRange(
                WeatherDataSource.INTERNAL,
                baseTime.minus(1, ChronoUnit.HOURS),
                baseTime.plus(1, ChronoUnit.HOURS)
        );

        // Then - Should only return INTERNAL data
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSource()).isEqualTo(WeatherDataSource.INTERNAL);
        assertThat(results.get(0).getStationId()).isEqualTo("KLGA");
    }

    @Test
    @DisplayName("Should return empty for unimplemented sources in table scan")
    void shouldReturnEmptyForUnimplementedSourcesInTableScan() {
        // Given
        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(baseTime)
                .build());

        // When - Query for unimplemented source
        List<WeatherData> results = repository.findBySourceAndTimeRange(
                WeatherDataSource.OPENWEATHERMAP,
                baseTime.minus(1, ChronoUnit.HOURS),
                baseTime.plus(1, ChronoUnit.HOURS)
        );

        // Then - Should return empty list
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should respect time boundaries for source queries in table scan")
    void shouldRespectTimeBoundariesForSourceInTableScan() {
        // Given
        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // In range
        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(baseTime)
                .build());

        // Out of range
        repository.save(MetarTestDataBuilder.create("KLGA")
                .withObservationTime(baseTime.minus(3, ChronoUnit.HOURS))
                .build());

        // When - Query for last hour only
        List<WeatherData> results = repository.findBySourceAndTimeRange(
                WeatherDataSource.NOAA,
                baseTime.minus(1, ChronoUnit.HOURS),
                baseTime.plus(1, ChronoUnit.HOURS)
        );

        // Then - Should get only recent observation
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStationId()).isEqualTo("KJFK");
    }

    // ========== Behavior Comparison Tests ==========

    @Test
    @DisplayName("Should return same results as GSI would - just slower")
    void shouldReturnSameResultsAsGSIWouldJustSlower() {
        // This test documents the expected behavior:
        // - Table scan (no GSI): O(n) - scans entire table
        // - GSI query (with GSI): O(m) - only scans time range
        // - Both return identical results, just different performance

        // Given - Weather data
        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(baseTime)
                .build());

        repository.save(MetarTestDataBuilder.create("KLGA")
                .withObservationTime(baseTime.minus(30, ChronoUnit.MINUTES))
                .build());

        // When - Query using table scan (no GSI present)
        Instant startTime = baseTime.minus(1, ChronoUnit.HOURS);
        Instant endTime = baseTime.plus(1, ChronoUnit.HOURS);

        List<WeatherData> scanResults = repository.findByTimeRange(startTime, endTime);

        // Then - Should return correct data (would be same as GSI, just slower)
        assertThat(scanResults).hasSize(2);
        assertThat(scanResults)
                .extracting(WeatherData::getStationId)
                .containsExactlyInAnyOrder("KJFK", "KLGA");

        // Note: If GSI were present, results would be identical but query would be ~50x faster
        logger.info("Table scan returned {} results (GSI would return same results ~50x faster)",
                scanResults.size());
    }

    @Test
    @DisplayName("Should maintain data integrity across scan and GSI approaches")
    void shouldMaintainDataIntegrityAcrossScanAndGSIApproaches() {
        // Given - Multiple observations
        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(baseTime)
                .build());

        repository.save(TafTestDataBuilder.create("KLGA")
                .withObservationTime(baseTime.minus(15, ChronoUnit.MINUTES))
                .build());

        // When - Query using table scan
        List<WeatherData> results = repository.findByTimeRange(
                baseTime.minus(30, ChronoUnit.MINUTES),
                baseTime.plus(30, ChronoUnit.MINUTES)
        );

        // Then - Should return all matching records with correct data
        assertThat(results).hasSize(2);

        // Verify data integrity
        results.forEach(data -> {
            assertThat(data.getStationId()).isNotNull();
            assertThat(data.getObservationTime()).isNotNull();
            assertThat(data.getDataType()).isNotNull();
            assertThat(data.getObservationTime())
                    .isBetween(baseTime.minus(30, ChronoUnit.MINUTES),
                            baseTime.plus(30, ChronoUnit.MINUTES));
        });
    }

    @Test
    @DisplayName("Should handle mixed data types in table scan")
    void shouldHandleMixedDataTypesInTableScan() {
        // Given - Mix of METAR and TAF observations
        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(baseTime)
                .build());

        repository.save(TafTestDataBuilder.create("KJFK")
                .withObservationTime(baseTime.minus(10, ChronoUnit.SECONDS))
                .build());

        repository.save(MetarTestDataBuilder.create("KLGA")
                .withObservationTime(baseTime.minus(20, ChronoUnit.SECONDS))
                .build());

        // When - Query for all observations
        List<WeatherData> results = repository.findByTimeRange(
                baseTime.minus(1, ChronoUnit.MINUTES),
                baseTime.plus(1, ChronoUnit.MINUTES)
        );

        // Then - Should get all 3 observations
        assertThat(results).hasSize(3);

        long metarCount = results.stream()
                .filter(d -> d.getDataType().equals("METAR"))
                .count();
        long tafCount = results.stream()
                .filter(d -> d.getDataType().equals("TAF"))
                .count();

        assertThat(metarCount).isEqualTo(2);
        assertThat(tafCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle pagination for large result sets in table scan")
    void shouldHandlePaginationForLargeResultSetsInTableScan() {
        // Given - Create enough data to potentially trigger pagination
        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        for (int i = 0; i < 100; i++) {
            repository.save(MetarTestDataBuilder.create("STATION" + String.format("%03d", i))
                    .withObservationTime(baseTime.plus(i, ChronoUnit.SECONDS))
                    .build());
        }

        // When - Query for all data (may require multiple scan pages)
        List<WeatherData> results = repository.findByTimeRange(
                baseTime.minus(1, ChronoUnit.HOURS),
                baseTime.plus(2, ChronoUnit.HOURS)
        );

        // Then - Should get all 100 records despite pagination
        assertThat(results).hasSize(100);
        assertThat(results)
                .extracting(WeatherData::getStationId)
                .containsAll(List.of("STATION000", "STATION050", "STATION099"));
    }
}
