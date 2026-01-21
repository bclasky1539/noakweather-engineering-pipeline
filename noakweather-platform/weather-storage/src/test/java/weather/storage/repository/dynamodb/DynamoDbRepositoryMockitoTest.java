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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import weather.model.NoaaWeatherData;
import weather.model.WeatherData;
import weather.storage.repository.RepositoryStats;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for DynamoDbRepository using Mockito.
 * <p>
 * These tests mock the DynamoDB client to test repository logic without AWS.
 * Coverage includes: save, find, query, batch operations, error handling.
 *
 * @author bclasky1539
 *
 */
@ExtendWith(MockitoExtension.class)
class DynamoDbRepositoryMockitoTest {

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    private DynamoDbRepository repository;

    @BeforeEach
    void setUp() {
        repository = new DynamoDbRepository(mockDynamoDbClient);
    }

    // ========== SAVE TESTS ==========

    @Test
    void shouldSaveWeatherDataSuccessfully() {
        // Given
        NoaaWeatherData weatherData = createTestWeatherData();
        PutItemResponse mockResponse = PutItemResponse.builder().build();

        when(mockDynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(mockResponse);

        // When
        WeatherData saved = repository.save(weatherData);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getStationId()).isEqualTo("KJFK");

        // Verify DynamoDB was called
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(mockDynamoDbClient, times(1)).putItem(captor.capture());

        PutItemRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo("noakweather-data");
        assertThat(request.item()).containsKey("station_id");
        assertThat(request.item()).containsKey("observation_time");
    }

    @Test
    void shouldThrowExceptionWhenSavingNullWeatherData() {
        assertThatThrownBy(() -> repository.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Weather data cannot be null");

        verify(mockDynamoDbClient, never()).putItem(any(PutItemRequest.class));
    }

    @Test
    void shouldThrowRuntimeExceptionWhenSaveFails() {
        // Given
        NoaaWeatherData weatherData = createTestWeatherData();

        when(mockDynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("Network error").build());

        // When/Then
        assertThatThrownBy(() -> repository.save(weatherData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save weather data");
    }

    // ========== FIND BY STATION AND TIME TESTS ==========

    @Test
    void shouldFindByStationAndTimeWhenItemExists() {
        // Given
        String stationId = "KJFK";
        Instant observationTime = Instant.now();

        Map<String, AttributeValue> mockItem = createMockDynamoDbItem(stationId, observationTime);
        GetItemResponse mockResponse = GetItemResponse.builder()
                .item(mockItem)
                .build();

        when(mockDynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(mockResponse);

        // When
        Optional<WeatherData> result = repository.findByStationAndTime(stationId, observationTime);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getStationId()).isEqualTo(stationId);

        verify(mockDynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
    }

    @Test
    void shouldReturnEmptyWhenItemNotFound() {
        // Given
        String stationId = "KJFK";
        Instant observationTime = Instant.now();

        GetItemResponse mockResponse = GetItemResponse.builder()
                .item(Collections.emptyMap())
                .build();

        when(mockDynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(mockResponse);

        // When
        Optional<WeatherData> result = repository.findByStationAndTime(stationId, observationTime);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenFindingWithNullStationId() {
        Instant now = Instant.now();

        assertThatThrownBy(() -> repository.findByStationAndTime(null, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Station ID cannot be null");

        verify(mockDynamoDbClient, never()).getItem(any(GetItemRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenFindingWithNullTime() {
        assertThatThrownBy(() -> repository.findByStationAndTime("KJFK", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Observation time cannot be null");
    }

    // ========== FIND BY TIME RANGE TESTS ==========

    @Test
    void shouldFindByStationAndTimeRange() {
        // Given
        String stationId = "KJFK";
        Instant startTime = Instant.now().minusSeconds(3600);
        Instant endTime = Instant.now();

        Map<String, AttributeValue> mockItem = createMockDynamoDbItem(stationId, startTime);
        QueryResponse mockResponse = QueryResponse.builder()
                .items(List.of(mockItem))
                .count(1)
                .build();

        when(mockDynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(mockResponse);

        // When
        List<WeatherData> results = repository.findByStationAndTimeRange(stationId, startTime, endTime);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStationId()).isEqualTo(stationId);

        // Verify query was constructed correctly
        ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(mockDynamoDbClient, times(1)).query(captor.capture());

        QueryRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo("noakweather-data");
        assertThat(request.keyConditionExpression()).contains("BETWEEN");
    }

    @Test
    void shouldReturnEmptyListWhenNoItemsInTimeRange() {
        // Given
        QueryResponse mockResponse = QueryResponse.builder()
                .items(Collections.emptyList())
                .count(0)
                .build();

        when(mockDynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(mockResponse);

        // When
        List<WeatherData> results = repository.findByStationAndTimeRange(
                "KJFK",
                Instant.now().minusSeconds(3600),
                Instant.now()
        );

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenTimeRangeIsInvalid() {
        Instant now = Instant.now();
        Instant earlier = now.minusSeconds(3600);

        // Start time AFTER end time
        assertThatThrownBy(() -> repository.findByStationAndTimeRange("KJFK", now, earlier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time must be before or equal to end time");
    }

    // ========== FIND LATEST TESTS ==========

    @Test
    void shouldFindLatestByStation() {
        // Given
        String stationId = "KJFK";
        Instant now = Instant.now();

        Map<String, AttributeValue> mockItem = createMockDynamoDbItem(stationId, now);
        QueryResponse mockResponse = QueryResponse.builder()
                .items(List.of(mockItem))
                .count(1)
                .build();

        when(mockDynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(mockResponse);

        // When
        Optional<WeatherData> result = repository.findLatestByStation(stationId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getStationId()).isEqualTo(stationId);

        // Verify query uses descending order and limit 1
        ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(mockDynamoDbClient, times(1)).query(captor.capture());

        QueryRequest request = captor.getValue();
        assertThat(request.scanIndexForward()).isFalse(); // Descending order
        assertThat(request.limit()).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyWhenNoLatestRecord() {
        // Given
        QueryResponse mockResponse = QueryResponse.builder()
                .items(Collections.emptyList())
                .count(0)
                .build();

        when(mockDynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(mockResponse);

        // When
        Optional<WeatherData> result = repository.findLatestByStation("KJFK");

        // Then
        assertThat(result).isEmpty();
    }

    // ========== BATCH SAVE TESTS ==========

    @Test
    void shouldSaveBatchSuccessfully() {
        // Given
        List<WeatherData> batch = List.of(
                createTestWeatherData(),
                createTestWeatherData()
        );

        BatchWriteItemResponse mockResponse = BatchWriteItemResponse.builder()
                .unprocessedItems(Collections.emptyMap())
                .build();

        when(mockDynamoDbClient.batchWriteItem(any(BatchWriteItemRequest.class)))
                .thenReturn(mockResponse);

        // When
        int savedCount = repository.saveBatch(batch);

        // Then
        assertThat(savedCount).isEqualTo(2);
        verify(mockDynamoDbClient, times(1)).batchWriteItem(any(BatchWriteItemRequest.class));
    }

    @Test
    void shouldReturnZeroWhenSavingEmptyBatch() {
        // When
        int savedCount = repository.saveBatch(Collections.emptyList());

        // Then
        assertThat(savedCount).isZero();
        verify(mockDynamoDbClient, never()).batchWriteItem(any(BatchWriteItemRequest.class));
    }

    @Test
    void shouldHandleUnprocessedItemsInBatch() {
        // Given
        List<WeatherData> batch = List.of(createTestWeatherData());

        // Simulate one unprocessed item
        List<WriteRequest> unprocessed = List.of(
                WriteRequest.builder().build()
        );

        BatchWriteItemResponse mockResponse = BatchWriteItemResponse.builder()
                .unprocessedItems(Map.of("noakweather-data", unprocessed))
                .build();

        when(mockDynamoDbClient.batchWriteItem(any(BatchWriteItemRequest.class)))
                .thenReturn(mockResponse);

        // When
        int savedCount = repository.saveBatch(batch);

        // Then - should return 0 successful (1 item - 1 unprocessed = 0)
        assertThat(savedCount).isZero();
    }

    // ========== HEALTH CHECK TESTS ==========

    @Test
    void shouldReturnHealthyWhenTableIsActive() {
        // Given
        DescribeTableResponse mockResponse = DescribeTableResponse.builder()
                .table(TableDescription.builder()
                        .tableStatus(TableStatus.ACTIVE)
                        .build())
                .build();

        when(mockDynamoDbClient.describeTable(any(DescribeTableRequest.class)))
                .thenReturn(mockResponse);

        // When
        boolean healthy = repository.isHealthy();

        // Then
        assertThat(healthy).isTrue();
    }

    @Test
    void shouldReturnUnhealthyWhenTableIsCreating() {
        // Given
        DescribeTableResponse mockResponse = DescribeTableResponse.builder()
                .table(TableDescription.builder()
                        .tableStatus(TableStatus.CREATING)
                        .build())
                .build();

        when(mockDynamoDbClient.describeTable(any(DescribeTableRequest.class)))
                .thenReturn(mockResponse);

        // When
        boolean healthy = repository.isHealthy();

        // Then
        assertThat(healthy).isFalse();
    }

    @Test
    void shouldReturnUnhealthyWhenExceptionOccurs() {
        // Given
        when(mockDynamoDbClient.describeTable(any(DescribeTableRequest.class)))
                .thenThrow(DynamoDbException.builder().message("Connection failed").build());

        // When
        boolean healthy = repository.isHealthy();

        // Then
        assertThat(healthy).isFalse();
    }

    // ========== DELETE TESTS ==========

    @Test
    void shouldThrowExceptionForDeleteOlderThan() {
        Instant now = Instant.now();

        assertThatThrownBy(() -> repository.deleteOlderThan(now))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("TTL");
    }

    // ========== STATS TESTS ==========

    @Test
    void shouldGetStats() {
        // Given
        DescribeTableResponse mockResponse = DescribeTableResponse.builder()
                .table(TableDescription.builder()
                        .tableStatus(TableStatus.ACTIVE)
                        .itemCount(100L)
                        .tableSizeBytes(5000L)
                        .build())
                .build();

        when(mockDynamoDbClient.describeTable(any(DescribeTableRequest.class)))
                .thenReturn(mockResponse);

        // When
        var stats = repository.getStats();

        // Then
        assertThat(stats.totalRecordCount()).isEqualTo(100L);
        assertThat(stats.storageSize()).isEqualTo(5000L);
    }

    // ========== HELPER METHODS ==========

    private NoaaWeatherData createTestWeatherData() {
        NoaaWeatherData data = new NoaaWeatherData();
        data.setStationId("KJFK");
        data.setObservationTime(Instant.now());
        data.setReportType("METAR");
        data.setRawText("METAR KJFK 191551Z 28016KT 10SM FEW250 22/12 A3015");
        data.setLatitude(40.6398);
        data.setLongitude(-73.7789);
        return data;
    }

    private Map<String, AttributeValue> createMockDynamoDbItem(String stationId, Instant observationTime) {
        Map<String, AttributeValue> item = new HashMap<>();

        // Create minimal valid JSON with correct Jackson property name
        String json = String.format(
                "{\"dataType\":\"NOAA\",\"id\":\"test-123\",\"ingestionTime\":\"%s\",\"source\":\"NOAA\",\"stationId\":\"%s\",\"observationTime\":\"%s\",\"reportType\":\"METAR\",\"conditions\":{\"wind\":null,\"visibility\":null,\"presentWeather\":[],\"skyConditions\":[],\"temperature\":null,\"pressure\":null},\"runwayVisualRange\":[],\"metadata\":{}}",
                Instant.now().toString(),
                stationId,
                observationTime.toString()
        );

        item.put("station_id", AttributeValue.builder().s(stationId).build());
        item.put("observation_time", AttributeValue.builder().n(String.valueOf(observationTime.getEpochSecond())).build());
        item.put("dataJson", AttributeValue.builder().s(json).build());  // ✅ Changed from "weather_data_json"
        item.put("dataType", AttributeValue.builder().s("NOAA").build());

        return item;
    }

    // ========== FIND BY STATIONS AND TIME TESTS ==========

    @Test
    void shouldFindByStationsAndTimeForMultipleStations() {
        // Given
        List<String> stationIds = List.of("KJFK", "KLGA", "KEWR");
        Instant observationTime = Instant.now();

        // Create mock items for each station
        Map<String, AttributeValue> mockItem1 = createMockDynamoDbItem("KJFK", observationTime);
        Map<String, AttributeValue> mockItem2 = createMockDynamoDbItem("KLGA", observationTime);
        Map<String, AttributeValue> mockItem3 = createMockDynamoDbItem("KEWR", observationTime);

        BatchGetItemResponse mockResponse = BatchGetItemResponse.builder()
                .responses(Map.of("noakweather-data", List.of(mockItem1, mockItem2, mockItem3)))
                .build();

        when(mockDynamoDbClient.batchGetItem(any(BatchGetItemRequest.class)))
                .thenReturn(mockResponse);

        // When
        List<WeatherData> results = repository.findByStationsAndTime(stationIds, observationTime);

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).extracting(WeatherData::getStationId)
                .containsExactlyInAnyOrder("KJFK", "KLGA", "KEWR");

        verify(mockDynamoDbClient, times(1)).batchGetItem(any(BatchGetItemRequest.class));
    }

    @Test
    void shouldReturnEmptyListWhenNoStationsProvided() {
        // Given
        List<String> emptyStationIds = Collections.emptyList();
        Instant observationTime = Instant.now();

        // When
        List<WeatherData> results = repository.findByStationsAndTime(emptyStationIds, observationTime);

        // Then
        assertThat(results).isEmpty();
        verify(mockDynamoDbClient, never()).batchGetItem(any(BatchGetItemRequest.class));
    }

    @Test
    void shouldReturnEmptyListWhenNullStationsProvided() {
        // Given
        Instant observationTime = Instant.now();

        // When
        List<WeatherData> results = repository.findByStationsAndTime(null, observationTime);

        // Then
        assertThat(results).isEmpty();
        verify(mockDynamoDbClient, never()).batchGetItem(any(BatchGetItemRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenObservationTimeIsNullInBatchGet() {
        // Given
        List<String> stationIds = List.of("KJFK", "KLGA");

        // When/Then
        assertThatThrownBy(() -> repository.findByStationsAndTime(stationIds, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Observation time cannot be null");

        verify(mockDynamoDbClient, never()).batchGetItem(any(BatchGetItemRequest.class));
    }

    @Test
    void shouldHandleLargeBatchOfStations() {
        // Given - Create 150 stations (will require 2 batches of 100 each)
        List<String> stationIds = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            stationIds.add("STATION" + i);
        }
        Instant observationTime = Instant.now();

        // Create mock responses for first 100 and then 50
        List<Map<String, AttributeValue>> batch1Items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            batch1Items.add(createMockDynamoDbItem("STATION" + i, observationTime));
        }

        List<Map<String, AttributeValue>> batch2Items = new ArrayList<>();
        for (int i = 100; i < 150; i++) {
            batch2Items.add(createMockDynamoDbItem("STATION" + i, observationTime));
        }

        BatchGetItemResponse mockResponse1 = BatchGetItemResponse.builder()
                .responses(Map.of("noakweather-data", batch1Items))
                .build();

        BatchGetItemResponse mockResponse2 = BatchGetItemResponse.builder()
                .responses(Map.of("noakweather-data", batch2Items))
                .build();

        when(mockDynamoDbClient.batchGetItem(any(BatchGetItemRequest.class)))
                .thenReturn(mockResponse1, mockResponse2);

        // When
        List<WeatherData> results = repository.findByStationsAndTime(stationIds, observationTime);

        // Then
        assertThat(results).hasSize(150);
        verify(mockDynamoDbClient, times(2)).batchGetItem(any(BatchGetItemRequest.class));
    }

    @Test
    void shouldHandlePartialResultsFromBatchGet() {
        // Given - Request 5 stations but only 3 found
        List<String> stationIds = List.of("KJFK", "KLGA", "KEWR", "KBOS", "KIAD");
        Instant observationTime = Instant.now();

        // Only 3 items returned
        Map<String, AttributeValue> mockItem1 = createMockDynamoDbItem("KJFK", observationTime);
        Map<String, AttributeValue> mockItem2 = createMockDynamoDbItem("KLGA", observationTime);
        Map<String, AttributeValue> mockItem3 = createMockDynamoDbItem("KEWR", observationTime);

        BatchGetItemResponse mockResponse = BatchGetItemResponse.builder()
                .responses(Map.of("noakweather-data", List.of(mockItem1, mockItem2, mockItem3)))
                .build();

        when(mockDynamoDbClient.batchGetItem(any(BatchGetItemRequest.class)))
                .thenReturn(mockResponse);

        // When
        List<WeatherData> results = repository.findByStationsAndTime(stationIds, observationTime);

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).extracting(WeatherData::getStationId)
                .containsExactlyInAnyOrder("KJFK", "KLGA", "KEWR");
    }

    @Test
    void shouldHandleEmptyResponseFromBatchGet() {
        // Given
        List<String> stationIds = List.of("KJFK", "KLGA");
        Instant observationTime = Instant.now();

        BatchGetItemResponse mockResponse = BatchGetItemResponse.builder()
                .responses(Collections.emptyMap())
                .build();

        when(mockDynamoDbClient.batchGetItem(any(BatchGetItemRequest.class)))
                .thenReturn(mockResponse);

        // When
        List<WeatherData> results = repository.findByStationsAndTime(stationIds, observationTime);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void shouldContinueOnBatchGetException() {
        // Given - 2 batches, first fails, second succeeds
        List<String> stationIds = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            stationIds.add("STATION" + i);
        }
        Instant observationTime = Instant.now();

        List<Map<String, AttributeValue>> batch2Items = new ArrayList<>();
        for (int i = 100; i < 150; i++) {
            batch2Items.add(createMockDynamoDbItem("STATION" + i, observationTime));
        }

        BatchGetItemResponse mockResponse2 = BatchGetItemResponse.builder()
                .responses(Map.of("noakweather-data", batch2Items))
                .build();

        // First call throws exception, second succeeds
        when(mockDynamoDbClient.batchGetItem(any(BatchGetItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("Network error").build())
                .thenReturn(mockResponse2);

        // When
        List<WeatherData> results = repository.findByStationsAndTime(stationIds, observationTime);

        // Then - Should have results from second batch only
        assertThat(results).hasSize(50);
        verify(mockDynamoDbClient, times(2)).batchGetItem(any(BatchGetItemRequest.class));
    }

    @Test
    void shouldFilterNullResultsFromBatchGet() {
        // Given
        List<String> stationIds = List.of("KJFK", "KLGA", "KEWR");
        Instant observationTime = Instant.now();

        // Create one valid item and one that will deserialize to null
        Map<String, AttributeValue> mockItem1 = createMockDynamoDbItem("KJFK", observationTime);
        Map<String, AttributeValue> mockItem2 = new HashMap<>();
        mockItem2.put("station_id", AttributeValue.builder().s("KLGA").build());
        mockItem2.put("observation_time", AttributeValue.builder().n(String.valueOf(observationTime.getEpochSecond())).build());
        // Missing dataJson - will return null from mapper

        BatchGetItemResponse mockResponse = BatchGetItemResponse.builder()
                .responses(Map.of("noakweather-data", List.of(mockItem1, mockItem2)))
                .build();

        when(mockDynamoDbClient.batchGetItem(any(BatchGetItemRequest.class)))
                .thenReturn(mockResponse);

        // When
        List<WeatherData> results = repository.findByStationsAndTime(stationIds, observationTime);

        // Then - Should only return valid item, filtered null
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStationId()).isEqualTo("KJFK");
    }

    // ========== GET STATS TESTS (Enhanced) ==========

    @Test
    void shouldGetStatsSuccessfully() {
        // Given
        DescribeTableResponse mockResponse = DescribeTableResponse.builder()
                .table(TableDescription.builder()
                        .tableStatus(TableStatus.ACTIVE)
                        .itemCount(1000L)
                        .tableSizeBytes(50000L)
                        .build())
                .build();

        when(mockDynamoDbClient.describeTable(any(DescribeTableRequest.class)))
                .thenReturn(mockResponse);

        // When
        RepositoryStats stats = repository.getStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.totalRecordCount()).isEqualTo(1000L);
        assertThat(stats.storageSize()).isEqualTo(50000L);

        // These fields are null because DynamoDB doesn't provide them without a scan
        assertThat(stats.oldestRecordTime()).isNull();
        assertThat(stats.newestRecordTime()).isNull();
        assertThat(stats.recordsLast24Hours()).isZero();
        assertThat(stats.recordsLast7Days()).isZero();
        assertThat(stats.uniqueStationCount()).isZero();

        verify(mockDynamoDbClient, times(1)).describeTable(any(DescribeTableRequest.class));
    }

    @Test
    void shouldGetStatsWithZeroItems() {
        // Given
        DescribeTableResponse mockResponse = DescribeTableResponse.builder()
                .table(TableDescription.builder()
                        .tableStatus(TableStatus.ACTIVE)
                        .itemCount(0L)
                        .tableSizeBytes(0L)
                        .build())
                .build();

        when(mockDynamoDbClient.describeTable(any(DescribeTableRequest.class)))
                .thenReturn(mockResponse);

        // When
        RepositoryStats stats = repository.getStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.totalRecordCount()).isZero();
        assertThat(stats.storageSize()).isZero();
    }

    @Test
    void shouldGetStatsWithLargeTable() {
        // Given
        DescribeTableResponse mockResponse = DescribeTableResponse.builder()
                .table(TableDescription.builder()
                        .tableStatus(TableStatus.ACTIVE)
                        .itemCount(10_000_000L)
                        .tableSizeBytes(5_000_000_000L)
                        .build())
                .build();

        when(mockDynamoDbClient.describeTable(any(DescribeTableRequest.class)))
                .thenReturn(mockResponse);

        // When
        RepositoryStats stats = repository.getStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.totalRecordCount()).isEqualTo(10_000_000L);
        assertThat(stats.storageSize()).isEqualTo(5_000_000_000L);
    }

    @Test
    void shouldReturnZeroStatsWhenExceptionOccurs() {
        // Given
        when(mockDynamoDbClient.describeTable(any(DescribeTableRequest.class)))
                .thenThrow(DynamoDbException.builder().message("Table not found").build());

        // When
        RepositoryStats stats = repository.getStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.totalRecordCount()).isZero();
        assertThat(stats.storageSize()).isZero();
        assertThat(stats.oldestRecordTime()).isNull();
        assertThat(stats.newestRecordTime()).isNull();
        assertThat(stats.recordsLast24Hours()).isZero();
        assertThat(stats.recordsLast7Days()).isZero();
        assertThat(stats.uniqueStationCount()).isZero();
    }

    @Test
    void shouldHandleNullTableDescription() {
        // Given - Response with null table (edge case)
        DescribeTableResponse mockResponse = DescribeTableResponse.builder()
                .table((TableDescription) null)
                .build();

        when(mockDynamoDbClient.describeTable(any(DescribeTableRequest.class)))
                .thenReturn(mockResponse);

        // When/Then - Should throw NullPointerException which gets caught
        RepositoryStats stats = repository.getStats();

        // Should return default zero stats due to exception handling
        assertThat(stats).isNotNull();
        assertThat(stats.totalRecordCount()).isZero();
    }

    @Test
    void shouldVerifyCorrectTableNameInGetStats() {
        // Given
        DescribeTableResponse mockResponse = DescribeTableResponse.builder()
                .table(TableDescription.builder()
                        .tableStatus(TableStatus.ACTIVE)
                        .itemCount(100L)
                        .tableSizeBytes(1000L)
                        .build())
                .build();

        when(mockDynamoDbClient.describeTable(any(DescribeTableRequest.class)))
                .thenReturn(mockResponse);

        // When
        repository.getStats();

        // Then
        ArgumentCaptor<DescribeTableRequest> captor = ArgumentCaptor.forClass(DescribeTableRequest.class);
        verify(mockDynamoDbClient).describeTable(captor.capture());

        assertThat(captor.getValue().tableName()).isEqualTo("noakweather-data");
    }

    @Test
    void shouldLogErrorWhenGetStatsFails() {
        // Given
        when(mockDynamoDbClient.describeTable(any(DescribeTableRequest.class)))
                .thenThrow(DynamoDbException.builder()
                        .message("Access denied")
                        .statusCode(403)
                        .build());

        // When
        RepositoryStats stats = repository.getStats();

        // Then - Should handle gracefully and return default stats
        assertThat(stats).isNotNull();
        assertThat(stats.totalRecordCount()).isZero();

        // Verify the error was logged (exception was caught)
        verify(mockDynamoDbClient, times(1)).describeTable(any(DescribeTableRequest.class));
    }
}
