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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import weather.model.NoaaMetarData;
import weather.model.WeatherData;
import weather.model.WeatherDataSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for DynamoDB GSI query methods (Phase 3).
 * <p>
 * Tests cover:
 * 1. findByTimeRange() - Uses table scan with time filter
 * 2. findByStationListAndTimeRange() - Uses main table queries
 * 3. findBySourceAndTimeRange() - Uses table scan + client-side filtering
 * <p>
 * These tests run against LocalStack DynamoDB.
 * Each test gets a fresh table.
 *
 * @author bclasky1539
 *
 */
@DisplayName("DynamoDB Repository Integration Tests - Phase 3 Sample")
class DynamoDbRepositoryIntegrationSampleTest extends BaseDynamoDbIntegrationTest {

    // ========== findByTimeRange() Tests (TimeIndex GSI) ==========

    @Test
    @DisplayName("findByTimeRange: Should find all observations in time range across multiple stations")
    void shouldFindByTimeRange() {
        // Given - Create observations at different times for different stations
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        Instant twoHoursAgo = now.minus(2, ChronoUnit.HOURS);
        Instant threeHoursAgo = now.minus(3, ChronoUnit.HOURS);

        // Save data for multiple stations at various times
        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(threeHoursAgo)
                .build());

        repository.save(MetarTestDataBuilder.create("KLGA")
                .withObservationTime(twoHoursAgo)
                .build());

        repository.save(MetarTestDataBuilder.create("KEWR")
                .withObservationTime(oneHourAgo)
                .build());

        repository.save(MetarTestDataBuilder.create("KTEB")
                .withObservationTime(now)
                .build());

        // When - Query for observations in the last 2.5 hours
        List<WeatherData> results = repository.findByTimeRange(
                twoHoursAgo.minus(5, ChronoUnit.MINUTES),
                now.plus(1, ChronoUnit.MINUTES)
        );

        // Then - Should get 3 observations (KLGA, KEWR, KTEB) but not KJFK
        assertThat(results).hasSize(3);

        List<String> stationIds = results.stream()
                .map(WeatherData::getStationId)
                .toList();

        assertThat(stationIds)
                .containsExactlyInAnyOrder("KLGA", "KEWR", "KTEB")
                .doesNotContain("KJFK");
    }

    @Test
    @DisplayName("findByTimeRange: Should return empty list when no observations in range")
    void shouldReturnEmptyListWhenNoObservationsInTimeRange() {
        // Given - Create observations outside the query range
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant oldTime = now.minus(10, ChronoUnit.HOURS);

        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(oldTime)
                .build());

        // When - Query for recent observations
        List<WeatherData> results = repository.findByTimeRange(
                now.minus(1, ChronoUnit.HOURS),
                now
        );

        // Then - Should get empty list
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findByTimeRange: Should handle exact time boundary matches")
    void shouldHandleExactTimeBoundaries() {
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

    @Test
    @DisplayName("findByTimeRange: Should handle mixed data types (METAR + TAF)")
    void shouldHandleMixedDataTypes() {
        // Given - Mix of METAR and TAF observations
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(now)
                .build());

        repository.save(TafTestDataBuilder.create("KJFK")
                .withObservationTime(now.minus(10, ChronoUnit.SECONDS))
                .build());

        repository.save(MetarTestDataBuilder.create("KLGA")
                .withObservationTime(now.minus(20, ChronoUnit.SECONDS))
                .build());

        // When - Query for all observations
        List<WeatherData> results = repository.findByTimeRange(
                now.minus(1, ChronoUnit.MINUTES),
                now.plus(1, ChronoUnit.MINUTES)
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
    @DisplayName("findByTimeRange: Should handle large result sets efficiently")
    void shouldHandleLargeResultSets() {
        // Given - Create 50 observations across different stations
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        for (int i = 0; i < 50; i++) {
            repository.save(MetarTestDataBuilder.create("KTEST" + i)
                    .withObservationTime(now.minus(i, ChronoUnit.SECONDS))
                    .build());
        }

        // When - Query for all observations
        List<WeatherData> results = repository.findByTimeRange(
                now.minus(1, ChronoUnit.MINUTES),
                now
        );

        // Then - Should get all 50 observations
        assertThat(results).hasSize(50);
    }

    @Test
    @DisplayName("findByTimeRange: Should reject invalid time range (start after end)")
    void shouldRejectInvalidTimeRange() {
        // Given - Create Instant objects OUTSIDE lambda (evaluated only once)
        Instant now = Instant.now();
        Instant past = now.minus(1, ChronoUnit.HOURS);

        // When/Then - Should throw exception
        assertThatThrownBy(() -> repository.findByTimeRange(now, past))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time must be before or equal to end time");
    }

    @Test
    @DisplayName("findByTimeRange: Should reject null parameters")
    void shouldRejectNullTimeRange() {
        // Given - Pre-compute values for single method invocation in lambda
        Instant now = Instant.now();

        // When/Then - Test null start time
        assertThatThrownBy(() -> repository.findByTimeRange(null, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");

        // When/Then - Test null end time
        assertThatThrownBy(() -> repository.findByTimeRange(now, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    // ========== findByStationListAndTimeRange() Tests ==========

    @Test
    @DisplayName("findByStationListAndTimeRange: Should find observations for multiple stations")
    void shouldFindByStationListAndTimeRange() {
        // Given - Create observations for multiple stations
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant past = now.minus(1, ChronoUnit.HOURS);

        // Target stations
        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(now)
                .build());
        repository.save(MetarTestDataBuilder.create("KLGA")
                .withObservationTime(now.minus(30, ChronoUnit.MINUTES))
                .build());

        // Non-target station
        repository.save(MetarTestDataBuilder.create("KEWR")
                .withObservationTime(now)
                .build());

        // When - Query for specific stations
        List<WeatherData> results = repository.findByStationListAndTimeRange(
                List.of("KJFK", "KLGA"),
                past,
                now.plus(1, ChronoUnit.MINUTES)
        );

        // Then - Should get only KJFK and KLGA
        assertThat(results).hasSize(2);

        List<String> stationIds = results.stream()
                .map(WeatherData::getStationId)
                .toList();

        assertThat(stationIds)
                .containsExactlyInAnyOrder("KJFK", "KLGA")
                .doesNotContain("KEWR");
    }

    @Test
    @DisplayName("findByStationListAndTimeRange: Should handle single station")
    void shouldHandleSingleStation() {
        // Given
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(now)
                .build());

        // When - Query for single station
        List<WeatherData> results = repository.findByStationListAndTimeRange(
                List.of("KJFK"),
                now.minus(1, ChronoUnit.MINUTES),
                now.plus(1, ChronoUnit.MINUTES)
        );

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStationId()).isEqualTo("KJFK");
    }

    @Test
    @DisplayName("findByStationListAndTimeRange: Should return empty for empty station list")
    void shouldReturnEmptyForEmptyStationList() {
        // When
        List<WeatherData> results = repository.findByStationListAndTimeRange(
                List.of(),
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now()
        );

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findByStationListAndTimeRange: Should continue on failure for one station")
    void shouldContinueOnPartialFailure() {
        // Given - Valid station with data
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(now)
                .build());

        // When - Query includes both existing and non-existing stations
        List<WeatherData> results = repository.findByStationListAndTimeRange(
                List.of("KJFK", "KNONEXISTENT"),
                now.minus(1, ChronoUnit.MINUTES),
                now.plus(1, ChronoUnit.MINUTES)
        );

        // Then - Should get data for KJFK only (KNONEXISTENT has no data)
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStationId()).isEqualTo("KJFK");
    }

    @Test
    @DisplayName("findByStationListAndTimeRange: Should filter by time range correctly")
    void shouldFilterByTimeRangeForMultipleStations() {
        // Given
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant twoHoursAgo = now.minus(2, ChronoUnit.HOURS);

        // In range
        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(now.minus(30, ChronoUnit.MINUTES))
                .build());

        // Out of range (too old)
        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(twoHoursAgo)
                .build());

        // When - Query for last hour only
        List<WeatherData> results = repository.findByStationListAndTimeRange(
                List.of("KJFK"),
                now.minus(1, ChronoUnit.HOURS),
                now
        );

        // Then - Should get only the recent observation
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getObservationTime())
                .isAfter(now.minus(1, ChronoUnit.HOURS));
    }

    // ========== findBySourceAndTimeRange() Tests ==========

    @Test
    @DisplayName("findBySourceAndTimeRange: Should filter by NOAA source correctly")
    void shouldFindBySourceAndTimeRange() {
        // Given - Mix of data from different sources
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // NOAA METAR
        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(now)
                .withSource(WeatherDataSource.NOAA)
                .build());

        // NOAA TAF
        repository.save(TafTestDataBuilder.create("KLGA")
                .withObservationTime(now.minus(10, ChronoUnit.SECONDS))
                .build());

        // Internal test data (different source)
        NoaaMetarData internalData = MetarTestDataBuilder.create("KEWR")
                .withObservationTime(now.minus(20, ChronoUnit.SECONDS))
                .withSource(WeatherDataSource.INTERNAL)
                .build();
        repository.save(internalData);

        // When - Query for NOAA source only
        List<WeatherData> results = repository.findBySourceAndTimeRange(
                WeatherDataSource.NOAA,
                now.minus(1, ChronoUnit.MINUTES),
                now.plus(1, ChronoUnit.MINUTES)
        );

        // Then - Should get only NOAA data (KJFK METAR + KLGA TAF)
        // KEWR with source=INTERNAL should NOT match
        assertThat(results).hasSize(2);

        results.forEach(data -> {
            String dataType = data.getDataType();
            assertThat(dataType).isIn("METAR", "TAF", "NOAA");
        });
    }

    @Test
    @DisplayName("findBySourceAndTimeRange: Should handle INTERNAL source")
    void shouldFindInternalSourceData() {
        // Given
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        NoaaMetarData internalData = MetarTestDataBuilder.create("KJFK")
                .withObservationTime(now)
                .withSource(WeatherDataSource.INTERNAL)
                .build();
        repository.save(internalData);

        repository.save(MetarTestDataBuilder.create("KLGA")
                .withObservationTime(now)
                .withSource(WeatherDataSource.NOAA)
                .build());

        // When
        List<WeatherData> results = repository.findBySourceAndTimeRange(
                WeatherDataSource.INTERNAL,
                now.minus(1, ChronoUnit.MINUTES),
                now.plus(1, ChronoUnit.MINUTES)
        );

        // Then - Should get only internal data (checks source field, not dataType)
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSource()).isEqualTo(WeatherDataSource.INTERNAL);
        assertThat(results.get(0).getDataType()).isEqualTo("METAR"); // Still METAR because it's NoaaMetarData
    }

    @Test
    @DisplayName("findBySourceAndTimeRange: Should return empty for unimplemented sources")
    void shouldReturnEmptyForUnimplementedSources() {
        // Given
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(now)
                .build());

        // When - Query for unimplemented source
        List<WeatherData> results = repository.findBySourceAndTimeRange(
                WeatherDataSource.OPENWEATHERMAP,
                now.minus(1, ChronoUnit.MINUTES),
                now.plus(1, ChronoUnit.MINUTES)
        );

        // Then - Should get empty list (no mapping defined yet)
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findBySourceAndTimeRange: Should respect time range boundaries")
    void shouldRespectTimeRangeBoundariesForSource() {
        // Given
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant twoHoursAgo = now.minus(2, ChronoUnit.HOURS);

        // In range
        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(now)
                .build());

        // Out of range
        repository.save(MetarTestDataBuilder.create("KLGA")
                .withObservationTime(twoHoursAgo)
                .build());

        // When - Query for last hour only
        List<WeatherData> results = repository.findBySourceAndTimeRange(
                WeatherDataSource.NOAA,
                now.minus(1, ChronoUnit.HOURS),
                now.plus(1, ChronoUnit.MINUTES)
        );

        // Then - Should get only recent observation
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStationId()).isEqualTo("KJFK");
    }

    @Test
    @DisplayName("findBySourceAndTimeRange: Should reject null source")
    void shouldRejectNullSource() {
        // Given - Create Instant objects OUTSIDE lambda (evaluated only once)
        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);

        // When/Then - Should throw exception for null source
        assertThatThrownBy(() -> repository.findBySourceAndTimeRange(
                null,
                oneHourAgo,
                now
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source cannot be null");
    }

    // ========== Performance and Edge Case Tests ==========

    @Test
    @DisplayName("Should handle queries with no results gracefully")
    void shouldHandleNoResults() {
        // Given - Empty table
        Instant now = Instant.now();

        // When - Various queries
        List<WeatherData> timeResults = repository.findByTimeRange(
                now.minus(1, ChronoUnit.HOURS),
                now
        );

        List<WeatherData> stationResults = repository.findByStationListAndTimeRange(
                List.of("KJFK", "KLGA"),
                now.minus(1, ChronoUnit.HOURS),
                now
        );

        List<WeatherData> sourceResults = repository.findBySourceAndTimeRange(
                WeatherDataSource.NOAA,
                now.minus(1, ChronoUnit.HOURS),
                now
        );

        // Then - All should return empty lists
        assertThat(timeResults).isEmpty();
        assertThat(stationResults).isEmpty();
        assertThat(sourceResults).isEmpty();
    }

    @Test
    @DisplayName("Should handle queries spanning multiple days")
    void shouldHandleMultiDayQueries() {
        // Given - Data over 3 days
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(now)
                .build());
        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(now.minus(1, ChronoUnit.DAYS))
                .build());
        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(now.minus(2, ChronoUnit.DAYS))
                .build());

        // When - Query for 3-day range
        List<WeatherData> results = repository.findByTimeRange(
                now.minus(3, ChronoUnit.DAYS),
                now
        );

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("Should demonstrate GSI query returns different results than main table query")
    void shouldDemonstrateGSIVsMainTableQueries() {
        // Given - Data for multiple stations at different times
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        repository.save(MetarTestDataBuilder.create("KJFK")
                .withObservationTime(now)
                .build());
        repository.save(MetarTestDataBuilder.create("KLGA")
                .withObservationTime(now)
                .build());
        repository.save(MetarTestDataBuilder.create("KEWR")
                .withObservationTime(now.minus(1, ChronoUnit.HOURS))
                .build());

        // When - Use GSI query (cross-station time query)
        List<WeatherData> gsiResults = repository.findByTimeRange(
                now.minus(5, ChronoUnit.MINUTES),
                now.plus(1, ChronoUnit.MINUTES)
        );

        // When - Use main table query (single station time range)
        List<WeatherData> mainTableResults = repository.findByStationAndTimeRange(
                "KJFK",
                now.minus(5, ChronoUnit.MINUTES),
                now.plus(1, ChronoUnit.MINUTES)
        );

        // Then - GSI query gets multiple stations, main table gets one station
        assertThat(gsiResults).hasSize(2);  // KJFK + KLGA
        assertThat(mainTableResults).hasSize(1);  // KJFK only

        assertThat(gsiResults.stream().map(WeatherData::getStationId).toList())
                .containsExactlyInAnyOrder("KJFK", "KLGA");
        assertThat(mainTableResults.get(0).getStationId()).isEqualTo("KJFK");
    }
}
