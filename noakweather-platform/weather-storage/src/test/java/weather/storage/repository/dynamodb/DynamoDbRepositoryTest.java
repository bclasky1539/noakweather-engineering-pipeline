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
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DynamoDbRepository.
 * <p>
 * These are basic tests using only JUnit Jupiter (no Mockito).
 * Tests focus on validation logic and parameter checking.
 * SonarQube-compliant: Only one method invocation per assertThrows.
 *
 * @author bclasky1539
 *
 */
class DynamoDbRepositoryTest {

    private DynamoDbRepository repository;
    private DynamoDbClient mockClient;

    @BeforeEach
    void setUp() {
        mockClient = new MockDynamoDbClient();
        repository = new DynamoDbRepository(mockClient);
    }

    @Test
    void shouldThrowExceptionWhenSavingNullWeatherData() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }

    @Test
    void shouldThrowExceptionWhenFindingWithNullStationId() {
        Instant now = Instant.now();
        assertThrows(IllegalArgumentException.class,
                () -> repository.findByStationAndTime(null, now));
    }

    @Test
    void shouldThrowExceptionWhenFindingWithEmptyStationId() {
        Instant now = Instant.now();
        assertThrows(IllegalArgumentException.class,
                () -> repository.findByStationAndTime("", now));
    }

    @Test
    void shouldThrowExceptionWhenFindingWithNullTime() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.findByStationAndTime("KJFK", null));
    }

    @Test
    void shouldThrowExceptionForDeleteOlderThan() {
        Instant now = Instant.now();
        assertThrows(UnsupportedOperationException.class,
                () -> repository.deleteOlderThan(now));
    }

    @Test
    void shouldThrowExceptionWhenTimeRangeIsInvalid() {
        // Given
        Instant now = Instant.now();
        Instant earlier = now.minusSeconds(3600);

        // When/Then - Start time is AFTER end time (invalid)
        assertThrows(IllegalArgumentException.class,
                () -> repository.findByStationAndTimeRange("KJFK", now, earlier));
    }

    @Test
    void shouldThrowExceptionWhenFindingRangeWithNullStationId() {
        Instant now = Instant.now();
        assertThrows(IllegalArgumentException.class,
                () -> repository.findByStationAndTimeRange(null, now, now));
    }

    @Test
    void shouldThrowExceptionWhenFindingRangeWithNullTimes() {
        Instant now = Instant.now();
        assertThrows(IllegalArgumentException.class,
                () -> repository.findByStationAndTimeRange("KJFK", null, now));
    }

    @Test
    void shouldThrowExceptionWhenFindingLatestWithNullStationId() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.findLatestByStation(null));
    }

    @Test
    void shouldThrowExceptionWhenFindingLatestWithEmptyStationId() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.findLatestByStation(""));
    }

    @Test
    void shouldThrowExceptionWhenFindingStationsWithNullTime() {
        // Pre-create the list OUTSIDE the lambda to satisfy SonarQube
        List<String> stationIds = List.of("KJFK");
        assertThrows(IllegalArgumentException.class,
                () -> repository.findByStationsAndTime(stationIds, null));
    }

    @Test
    void shouldCreateRepositoryWithValidClient() {
        // When
        DynamoDbRepository repo = new DynamoDbRepository(mockClient);

        // Then
        assertNotNull(repo);
    }

    @Test
    void shouldThrowExceptionWhenCreatingWithNullClient() {
        assertThrows(NullPointerException.class, () -> new DynamoDbRepository(null));
    }

    // ========== MOCK CLIENT (Minimal Implementation) ==========

    /**
     * Minimal mock DynamoDB client for validation tests.
     * Only implements required interface methods - all throw UnsupportedOperationException.
     */
    private static class MockDynamoDbClient implements DynamoDbClient {

        @Override
        public String serviceName() {
            return "dynamodb";
        }

        @Override
        public void close() {
            // No-op
        }

        @Override
        public PutItemResponse putItem(PutItemRequest request) {
            throw new UnsupportedOperationException("Mock client - not implemented");
        }

        @Override
        public GetItemResponse getItem(GetItemRequest request) {
            throw new UnsupportedOperationException("Mock client - not implemented");
        }

        @Override
        public QueryResponse query(QueryRequest request) {
            throw new UnsupportedOperationException("Mock client - not implemented");
        }

        @Override
        public DeleteItemResponse deleteItem(DeleteItemRequest request) {
            throw new UnsupportedOperationException("Mock client - not implemented");
        }

        @Override
        public BatchWriteItemResponse batchWriteItem(BatchWriteItemRequest request) {
            throw new UnsupportedOperationException("Mock client - not implemented");
        }

        @Override
        public BatchGetItemResponse batchGetItem(BatchGetItemRequest request) {
            throw new UnsupportedOperationException("Mock client - not implemented");
        }

        @Override
        public DescribeTableResponse describeTable(DescribeTableRequest request) {
            throw new UnsupportedOperationException("Mock client - not implemented");
        }
    }
}
