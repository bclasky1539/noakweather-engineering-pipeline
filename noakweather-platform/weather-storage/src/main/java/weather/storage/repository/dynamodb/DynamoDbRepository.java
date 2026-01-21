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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import weather.model.WeatherData;
import weather.model.WeatherDataSource;
import weather.storage.exception.RepositoryException;
import weather.storage.repository.RepositoryStats;
import weather.storage.repository.UniversalWeatherRepository;

import java.time.Instant;
import java.util.*;

/**
 * DynamoDB implementation of the UniversalWeatherRepository.
 * <p>
 * This replaces the stub implementation with actual DynamoDB integration.
 * <p>
 * DynamoDB serves as the real-time data store in Lambda Architecture:
 * - Speed Layer: Stores recent weather data for fast access
 * - Serving Layer: Provides low-latency queries for APIs
 * - Optimized for: Sub-millisecond reads, high throughput, scalability
 * - Use case: Current weather lookups, recent history (last 7-30 days)
 * <p>
 * Table Schema:
 * - Table name: weather-data
 * - Partition Key: station_id (String) - for even distribution and fast lookups
 * - Sort Key: observation_time (Number - epoch seconds) - for time-range queries
 * - TTL: Auto-expire records after 30 days (batch layer has full history)
 * <p>
 * Design considerations:
 * - Works with WeatherData base class (polymorphic)
 * - Optimized for NoaaWeatherData (most common type)
 * - Batch writes use DynamoDB's BatchWriteItem (max 25 items per batch)
 * - Uses conditional writes for idempotency where appropriate
 * - Integrates with existing logging (SLF4J)
 *
 * @author bclasky1539
 */
public class DynamoDbRepository implements UniversalWeatherRepository {

    private static final Logger logger = LoggerFactory.getLogger(DynamoDbRepository.class);
    private static final String TABLE_NAME = "noakweather-data";
    private static final int BATCH_WRITE_MAX_SIZE = 25; // DynamoDB limit

    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbMapper mapper;

    /**
     * Attribute name for the partition key (station ID)
     */
    private static final String ATTR_STATION_ID = "station_id";

    /**
     * Attribute name for the sort key (observation time)
     */
    private static final String ATTR_OBSERVATION_TIME = "observation_time";

    /**
     * Creates a new DynamoDB repository with the provided client.
     *
     * @param dynamoDbClient the DynamoDB client to use
     */
    public DynamoDbRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = Objects.requireNonNull(dynamoDbClient, "DynamoDB client cannot be null");
        this.mapper = new DynamoDbMapper();
        logger.info("DynamoDB repository initialized for table: {}", TABLE_NAME);
    }

    @Override
    public WeatherData save(WeatherData weatherData) {
        if (weatherData == null) {
            throw new IllegalArgumentException("Weather data cannot be null");
        }

        logger.debug("Saving weather data for station: {} at time: {}",
                weatherData.getStationId(),
                weatherData.getObservationTime());

        try {
            Map<String, AttributeValue> item = mapper.toAttributeMap(weatherData);

            PutItemRequest request = PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(item)
                    .build();

            dynamoDbClient.putItem(request);

            logger.info("Successfully saved weather data for station: {}",
                    weatherData.getStationId());

            return weatherData;

        } catch (DynamoDbException e) {
            throw new RepositoryException("Failed to save weather data to DynamoDB", e);
        }
    }

    @Override
    public int saveBatch(List<WeatherData> weatherDataList) {
        if (weatherDataList == null || weatherDataList.isEmpty()) {
            logger.debug("Empty weather data list provided, nothing to save");
            return 0;
        }

        logger.info("Saving batch of {} weather data items", weatherDataList.size());

        int successCount = 0;

        // Split into batches of 25 (DynamoDB limit)
        List<List<WeatherData>> batches = partitionList(weatherDataList, BATCH_WRITE_MAX_SIZE);

        for (List<WeatherData> batch : batches) {
            try {
                List<WriteRequest> writeRequests = batch.stream()
                        .map(mapper::toAttributeMap)
                        .map(item -> WriteRequest.builder()
                                .putRequest(builder -> builder.item(item))
                                .build())
                        .toList();

                Map<String, List<WriteRequest>> requestItems = new HashMap<>();
                requestItems.put(TABLE_NAME, writeRequests);

                BatchWriteItemRequest request = BatchWriteItemRequest.builder()
                        .requestItems(requestItems)
                        .build();

                BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(request);

                // Count successful writes
                int batchSize = batch.size();
                int unprocessedCount = response.unprocessedItems().getOrDefault(TABLE_NAME, List.of()).size();
                successCount += (batchSize - unprocessedCount);

                // Handle unprocessed items
                if (!response.unprocessedItems().isEmpty()) {
                    logger.warn("Batch write had {} unprocessed items", unprocessedCount);
                }

            } catch (DynamoDbException e) {
                logger.error("Failed to save batch of weather data", e);
                // Continue with next batch rather than failing completely
            }
        }

        logger.info("Successfully saved {}/{} weather data items",
                successCount, weatherDataList.size());

        return successCount;
    }

    @Override
    public Optional<WeatherData> findByStationAndTime(String stationId, Instant observationTime) {
        if (stationId == null || stationId.isEmpty()) {
            throw new IllegalArgumentException("Station ID cannot be null or empty");
        }
        if (observationTime == null) {
            throw new IllegalArgumentException("Observation time cannot be null");
        }

        logger.debug("Finding weather data for station: {} at time: {}", stationId, observationTime);

        try {
            long epochSeconds = observationTime.getEpochSecond();

            Map<String, AttributeValue> key = new HashMap<>();
            key.put(ATTR_STATION_ID, AttributeValue.builder().s(stationId).build());
            key.put(ATTR_OBSERVATION_TIME, AttributeValue.builder().n(String.valueOf(epochSeconds)).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (response.hasItem()) {
                logger.debug("Found weather data for station: {}", stationId);
                WeatherData weatherData = mapper.fromAttributeMap(response.item());
                return Optional.ofNullable(weatherData);  // ✅ Changed to ofNullable
            } else {
                logger.debug("No weather data found for station: {} at time: {}",
                        stationId, observationTime);
                return Optional.empty();
            }

        } catch (DynamoDbException e) {
            throw new RepositoryException("Failed to query DynamoDB", e);
        }
    }

    @Override
    public List<WeatherData> findByStationAndTimeRange(String stationId,
                                                       Instant startTime,
                                                       Instant endTime) {
        if (stationId == null || stationId.isEmpty()) {
            throw new IllegalArgumentException("Station ID cannot be null or empty");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before or equal to end time");
        }

        logger.debug("Finding weather data for station: {} between {} and {}",
                stationId, startTime, endTime);

        try {
            long startEpoch = startTime.getEpochSecond();
            long endEpoch = endTime.getEpochSecond();

            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#sid", ATTR_STATION_ID);
            expressionAttributeNames.put("#ot", ATTR_OBSERVATION_TIME);

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":stationId", AttributeValue.builder().s(stationId).build());
            expressionAttributeValues.put(":startTime", AttributeValue.builder().n(String.valueOf(startEpoch)).build());
            expressionAttributeValues.put(":endTime", AttributeValue.builder().n(String.valueOf(endEpoch)).build());

            QueryRequest request = QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .keyConditionExpression("#sid = :stationId AND #ot BETWEEN :startTime AND :endTime")
                    .expressionAttributeNames(expressionAttributeNames)
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();

            QueryResponse response = dynamoDbClient.query(request);

            List<WeatherData> results = response.items().stream()
                    .map(mapper::fromAttributeMap)
                    .toList();

            logger.info("Found {} weather data items for station: {} in time range",
                    results.size(), stationId);

            return results;

        } catch (DynamoDbException e) {
            throw new RepositoryException("Failed to query DynamoDB", e);
        }
    }

    @Override
    public Optional<WeatherData> findLatestByStation(String stationId) {
        if (stationId == null || stationId.isEmpty()) {
            throw new IllegalArgumentException("Station ID cannot be null or empty");
        }

        logger.debug("Finding latest weather data for station: {}", stationId);

        try {
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#sid", ATTR_STATION_ID);

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":stationId", AttributeValue.builder().s(stationId).build());

            QueryRequest request = QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .keyConditionExpression("#sid = :stationId")
                    .expressionAttributeNames(expressionAttributeNames)
                    .expressionAttributeValues(expressionAttributeValues)
                    .scanIndexForward(false)  // Descending order (newest first)
                    .limit(1)
                    .build();

            QueryResponse response = dynamoDbClient.query(request);

            if (!response.items().isEmpty()) {
                WeatherData weatherData = mapper.fromAttributeMap(response.items().get(0));
                logger.debug("Found latest weather data for station: {}", stationId);
                return Optional.ofNullable(weatherData);  // ✅ Changed to ofNullable
            } else {
                logger.debug("No weather data found for station: {}", stationId);
                return Optional.empty();
            }

        } catch (DynamoDbException e) {
            throw new RepositoryException("Failed to query DynamoDB", e);
        }
    }

    @Override
    public List<WeatherData> findBySourceAndTimeRange(WeatherDataSource source,
                                                      Instant startTime,
                                                      Instant endTime) {
        // This would require a GSI (Global Secondary Index) on source + observation_time
        // For now, return empty list as this requires additional DynamoDB setup
        logger.warn("findBySourceAndTimeRange not yet implemented - requires GSI on source field");
        return Collections.emptyList();
    }

    @Override
    public List<WeatherData> findByStationsAndTime(List<String> stationIds,
                                                   Instant observationTime) {
        if (stationIds == null || stationIds.isEmpty()) {
            return Collections.emptyList();
        }
        if (observationTime == null) {
            throw new IllegalArgumentException("Observation time cannot be null");
        }

        logger.debug("Finding weather data for {} stations at time: {}",
                stationIds.size(), observationTime);

        // Use BatchGetItem for efficient multi-station retrieval
        List<WeatherData> results = new ArrayList<>();
        long epochSeconds = observationTime.getEpochSecond();

        // DynamoDB allows max 100 items per BatchGetItem
        List<List<String>> batches = partitionList(stationIds, 100);

        for (List<String> batch : batches) {
            try {
                List<Map<String, AttributeValue>> keys = batch.stream()
                        .map(stationId -> {
                            Map<String, AttributeValue> key = new HashMap<>();
                            key.put(ATTR_STATION_ID, AttributeValue.builder().s(stationId).build());
                            key.put(ATTR_OBSERVATION_TIME, AttributeValue.builder().n(String.valueOf(epochSeconds)).build());
                            return key;
                        })
                        .toList();

                KeysAndAttributes keysAndAttributes = KeysAndAttributes.builder()
                        .keys(keys)
                        .build();

                Map<String, KeysAndAttributes> requestItems = new HashMap<>();
                requestItems.put(TABLE_NAME, keysAndAttributes);

                BatchGetItemRequest request = BatchGetItemRequest.builder()
                        .requestItems(requestItems)
                        .build();

                BatchGetItemResponse response = dynamoDbClient.batchGetItem(request);

                List<WeatherData> batchResults = response.responses()
                        .getOrDefault(TABLE_NAME, List.of())
                        .stream()
                        .map(mapper::fromAttributeMap)
                        .filter(Objects::nonNull)
                        .toList();

                results.addAll(batchResults);

            } catch (DynamoDbException e) {
                logger.error("Failed to batch get weather data", e);
            }
        }

        logger.info("Found {} weather data items for {} stations",
                results.size(), stationIds.size());

        return results;
    }

    @Override
    public int deleteOlderThan(Instant cutoffDate) {
        // DynamoDB should use TTL (Time To Live) for automatic expiration
        // Manual deletion requires expensive Scan + BatchWriteItem operations
        throw new UnsupportedOperationException(
                "DynamoDbRepository.deleteOlderThan() - use TTL for automatic expiration instead. " +
                        "Configure TTL on the 'timestamp' attribute in DynamoDB console."
        );
    }

    @Override
    public boolean isHealthy() {
        try {
            // Simple health check: try to describe the table
            DescribeTableRequest request = DescribeTableRequest.builder()
                    .tableName(TABLE_NAME)
                    .build();

            DescribeTableResponse response = dynamoDbClient.describeTable(request);

            boolean healthy = response.table().tableStatus() == TableStatus.ACTIVE;
            logger.debug("DynamoDB health check: {}", healthy ? "HEALTHY" : "UNHEALTHY");

            return healthy;

        } catch (DynamoDbException e) {
            logger.error("DynamoDB health check failed", e);
            return false;
        }
    }

    @Override
    public RepositoryStats getStats() {
        try {
            DescribeTableRequest request = DescribeTableRequest.builder()
                    .tableName(TABLE_NAME)
                    .build();

            DescribeTableResponse response = dynamoDbClient.describeTable(request);

            // Add null check for table description
            if (response.table() == null) {
                logger.warn("DynamoDB describe table returned null table description");
                return new RepositoryStats(0L, null, null,
                        0L, 0L, 0, 0L);
            }

            long itemCount = response.table().itemCount();
            long tableSizeBytes = response.table().tableSizeBytes();

            logger.debug("DynamoDB stats - Items: {}, Size: {} bytes", itemCount, tableSizeBytes);

            // Note: DynamoDB doesn't provide oldest/newest timestamps without a scan
            // These would require additional queries or maintaining summary data
            return new RepositoryStats(
                    itemCount,          // totalRecordCount
                    null,               // oldestRecordTime (would require scan)
                    null,               // newestRecordTime (would require scan)
                    0L,                 // recordsLast24Hours (would require scan)
                    0L,                 // recordsLast7Days (would require scan)
                    0,                  // uniqueStationCount (would require scan)
                    tableSizeBytes      // storageSize
            );

        } catch (DynamoDbException e) {
            logger.error("Failed to get DynamoDB stats", e);
            return new RepositoryStats(0L, null, null, 0L, 0L, 0, 0L);
        }
    }

    // ========== UTILITY METHODS ==========

    /**
     * Partitions a list into smaller sublists of specified size.
     *
     * @param list the list to partition
     * @param size the maximum size of each partition
     * @param <T> the type of elements in the list
     * @return list of partitioned sublists
     */
    private <T> List<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
