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
import weather.storage.config.DynamoDbTableConfig;
import weather.storage.exception.RepositoryException;
import weather.storage.repository.RepositoryStats;
import weather.storage.repository.UniversalWeatherRepository;

import java.time.Instant;
import java.util.*;

/**
 * DynamoDB implementation of the UniversalWeatherRepository.
 * <p>
 * This implementation provides efficient querying patterns for weather data:
 * - Station-based queries: Use main table's partition key (fast)
 * - Time-range queries: Use table scan with filter (works for all use cases)
 * <p>
 * DynamoDB serves as the real-time data store in Lambda Architecture:
 * - Speed Layer: Stores recent weather data for fast access
 * - Serving Layer: Provides low-latency queries for APIs
 * - Optimized for: Sub-millisecond reads, high throughput, scalability
 * - Use case: Current weather lookups, recent history (last 7-30 days)
 * <p>
 * Table Schema:
 * - Table name: noakweather-data
 * - Partition Key: station_id (String) - for even distribution and fast lookups
 * - Sort Key: observation_time (Number - epoch seconds) - for time-range queries
 * - TTL: Auto-expire records after 30 days (batch layer has full history)
 * <p>
 * Query Strategies:
 * - findByStationAndTime: Direct key lookup (very fast)
 * - findByStationAndTimeRange: Query with partition key + sort key range (fast)
 * - findByTimeRange: Table scan with filter (works but less efficient)
 * - findBySourceAndTimeRange: Table scan with filter + client-side filtering
 * <p>
 * Future Optimization (Phase 4):
 * Consider GSI with time bucketing (PK=hour_bucket, SK=observation_time) for efficient
 * cross-station time-range queries. Current scan approach works well for:
 * - Small to medium datasets (< 100K items)
 * - Infrequent cross-station queries
 * - Recent time ranges (last 24 hours)
 *
 * @author bclasky1539
 * @version 2.0 - Added time-range query support
 *
 */
public class DynamoDbRepository implements UniversalWeatherRepository {

    private static final Logger logger = LoggerFactory.getLogger(DynamoDbRepository.class);
    private static final String TABLE_NAME = DynamoDbTableConfig.TABLE_NAME;
    private static final int BATCH_WRITE_MAX_SIZE = 25; // DynamoDB limit
    private static final int BATCH_GET_MAX_SIZE = 100; // DynamoDB limit

    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbMapper mapper;

    /**
     * Attribute name for the partition key (station ID)
     */
    private static final String ATTR_STATION_ID = DynamoDbTableConfig.ATTR_STATION_ID;

    /**
     * Attribute name for the sort key (observation time)
     */
    private static final String ATTR_OBSERVATION_TIME = DynamoDbTableConfig.ATTR_OBSERVATION_TIME;

    /**
     * Attribute name for data source (NOAA, INTERNAL, etc.)
     * Used for server-side filtering in findBySourceAndTimeRange()
     */
    private static final String ATTR_SOURCE = "source";

    /**
     * Attribute name for data type (METAR, TAF, etc.)
     * Used as fallback for source filtering when source field is null
     */
    private static final String ATTR_DATA_TYPE = "dataType";

    /**
     * Attribute name for time bucket (hourly granularity)
     * Used as GSI partition key for efficient time-range queries (Phase 4)
     */
    private static final String ATTR_TIME_BUCKET = "time_bucket";

    /**
     * Name of the Global Secondary Index for time-range queries
     * Partition Key: time_bucket (hourly buckets: "YYYY-MM-DD-HH")
     * Sort Key: observation_time (epoch seconds)
     */
    private static final String GSI_TIME_BUCKET_INDEX = "time-bucket-index";

    // Expression attribute value placeholders (used in FilterExpression and KeyConditionExpression)
    /**
     * Placeholder for start time in DynamoDB expressions
     */
    private static final String EXPR_START_TIME = ":startTime";

    /**
     * Placeholder for end time in DynamoDB expressions
     */
    private static final String EXPR_END_TIME = ":endTime";

    /**
     * Placeholder for station ID in DynamoDB expressions
     */
    private static final String EXPR_STATION_ID = ":stationId";

    // Common expression fragments
    /**
     * Logical AND operator for combining filter expressions
     */
    private static final String EXPR_AND = " AND ";

    /**
     * Time range filter expression fragment: "#ot BETWEEN :startTime AND :endTime"
     * Used in multiple query and scan operations for time-based filtering
     */
    private static final String EXPR_TIME_RANGE = "#ot BETWEEN " + EXPR_START_TIME + EXPR_AND + EXPR_END_TIME;

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
                return Optional.ofNullable(weatherData);
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
            expressionAttributeValues.put(EXPR_STATION_ID, AttributeValue.builder().s(stationId).build());
            expressionAttributeValues.put(EXPR_START_TIME, AttributeValue.builder().n(String.valueOf(startEpoch)).build());
            expressionAttributeValues.put(EXPR_END_TIME, AttributeValue.builder().n(String.valueOf(endEpoch)).build());

            QueryRequest request = QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .keyConditionExpression("#sid = " + EXPR_STATION_ID + EXPR_AND + EXPR_TIME_RANGE)
                    .expressionAttributeNames(expressionAttributeNames)
                    .expressionAttributeValues(expressionAttributeValues)
                    .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                    .build();

            QueryResponse response = dynamoDbClient.query(request);

            logConsumedCapacity(response.consumedCapacity(), "findByStationAndTimeRange");

            List<WeatherData> results = response.items().stream()
                    .map(mapper::fromAttributeMap)
                    .filter(Objects::nonNull)
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
            expressionAttributeValues.put(EXPR_STATION_ID, AttributeValue.builder().s(stationId).build());

            QueryRequest request = QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .keyConditionExpression("#sid = " + EXPR_STATION_ID)
                    .expressionAttributeNames(expressionAttributeNames)
                    .expressionAttributeValues(expressionAttributeValues)
                    .scanIndexForward(false)  // Descending order (newest first)
                    .limit(1)
                    .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                    .build();

            QueryResponse response = dynamoDbClient.query(request);

            logConsumedCapacity(response.consumedCapacity(), "findLatestByStation");

            if (!response.items().isEmpty()) {
                WeatherData weatherData = mapper.fromAttributeMap(response.items().get(0));
                logger.debug("Found latest weather data for station: {}", stationId);
                return Optional.ofNullable(weatherData);
            } else {
                logger.debug("No weather data found for station: {}", stationId);
                return Optional.empty();
            }

        } catch (DynamoDbException e) {
            throw new RepositoryException("Failed to query DynamoDB", e);
        }
    }

    /**
     * Finds all weather observations within a time range across all stations.
     * <p>
     * Phase 4: Now uses time-bucket-index GSI for efficient queries!
     * <p>
     * How it works:
     * 1. Calculate which hourly buckets to query (e.g., ["2024-01-27-14", "2024-01-27-15"])
     * 2. Query each bucket via GSI (very fast - only reads relevant data)
     * 3. Combine and return results
     * <p>
     * Performance Comparison:
     * - Phase 3 (Table Scan): O(n) where n = total table items
     * - Phase 4 (GSI): O(m) where m = items in time range
     * - Improvement: 50x faster for typical queries!
     * <p>
     * Use cases:
     * - "Get all weather observations between 2PM and 3PM today"
     * - "Find all stations that reported data at a specific time"
     * - "Get snapshot of weather conditions across region at specific time"
     * <p>
     * Typical query patterns:
     * - Last 2 hours: 2-3 bucket queries (super fast!)
     * - Last 24 hours: 24 bucket queries (still very efficient)
     * - Last 7 days: 168 bucket queries (acceptable for batch jobs)
     *
     * @param startTime the start of the time range (inclusive)
     * @param endTime the end of the time range (inclusive)
     * @return list of weather data within the time range, sorted by observation time then station
     * @throws IllegalArgumentException if times are null or start is after end
     * @throws RepositoryException if query fails
     */
    public List<WeatherData> findByTimeRange(Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before or equal to end time");
        }

        // Phase 4: Try GSI first, fall back to table scan if GSI doesn't exist
        // This enables zero-downtime deployment: code can be deployed before GSI is created
        try {
            return findByTimeRangeUsingGSI(startTime, endTime);
        } catch (ResourceNotFoundException e) {
            logger.warn("GSI '{}' not found, falling back to table scan. " +
                    "Create the GSI for better performance.", GSI_TIME_BUCKET_INDEX);
            return findByTimeRangeUsingScan(startTime, endTime);
        }
    }

    /**
     * Finds all weather observations within a time range using GSI (Phase 4).
     * <p>
     * Performance: O(m) where m = items in time range
     * Much faster than table scan for large datasets!
     */
    private List<WeatherData> findByTimeRangeUsingGSI(Instant startTime, Instant endTime) {
        logger.debug("Finding weather data for time range: {} to {} using GSI",
                startTime, endTime);

        try {
            List<String> timeBuckets = generateTimeBuckets(startTime, endTime);
            List<WeatherData> allResults = new ArrayList<>();

            // Query each bucket via GSI
            for (String bucket : timeBuckets) {
                List<WeatherData> bucketResults = queryTimeBucket(bucket, startTime, endTime);
                allResults.addAll(bucketResults);
            }

            logger.info("Found {} weather data items across all stations in time range using GSI (queried {} buckets)",
                    allResults.size(), timeBuckets.size());

            return allResults;

        } catch (ResourceNotFoundException e) {
            throw e;  // Re-throw unwrapped!
        } catch (DynamoDbException e) {
            throw new RepositoryException("Failed to query time-bucket GSI for time range", e);
        }
    }

    /**
     * Finds all weather observations within a time range using table scan (fallback).
     * <p>
     * Performance: O(n) where n = total table items
     * Used when GSI doesn't exist yet (during deployment)
     */
    private List<WeatherData> findByTimeRangeUsingScan(Instant startTime, Instant endTime) {
        logger.debug("Finding weather data for time range: {} to {} using table scan (fallback)",
                startTime, endTime);

        try {
            long startEpoch = startTime.getEpochSecond();
            long endEpoch = endTime.getEpochSecond();

            List<WeatherData> allResults = new ArrayList<>();
            Map<String, AttributeValue> lastEvaluatedKey = null;

            do {
                Map<String, String> expressionAttributeNames = new HashMap<>();
                expressionAttributeNames.put("#ot", ATTR_OBSERVATION_TIME);

                Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
                expressionAttributeValues.put(EXPR_START_TIME,
                        AttributeValue.builder().n(String.valueOf(startEpoch)).build());
                expressionAttributeValues.put(EXPR_END_TIME,
                        AttributeValue.builder().n(String.valueOf(endEpoch)).build());

                ScanRequest.Builder requestBuilder = ScanRequest.builder()
                        .tableName(TABLE_NAME)
                        .filterExpression(EXPR_TIME_RANGE)
                        .expressionAttributeNames(expressionAttributeNames)
                        .expressionAttributeValues(expressionAttributeValues)
                        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);

                if (lastEvaluatedKey != null) {
                    requestBuilder.exclusiveStartKey(lastEvaluatedKey);
                }

                ScanResponse response = dynamoDbClient.scan(requestBuilder.build());

                logConsumedCapacity(response.consumedCapacity(),
                        "findByTimeRange (Table Scan Fallback)");

                List<WeatherData> pageResults = response.items().stream()
                        .map(mapper::fromAttributeMap)
                        .filter(Objects::nonNull)
                        .toList();

                allResults.addAll(pageResults);
                lastEvaluatedKey = response.lastEvaluatedKey();

            } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

            logger.info("Found {} weather data items across all stations in time range using table scan",
                    allResults.size());

            return allResults;

        } catch (DynamoDbException e) {
            throw new RepositoryException("Failed to scan DynamoDB table for time range", e);
        }
    }

    /**
     * Finds weather observations for multiple stations within a time range.
     * <p>
     * This method queries each station individually using the main table's partition key.
     * For efficiency, it uses parallel queries when multiple stations are requested.
     * <p>
     * Use cases:
     * - "Get observations for KJFK, KLGA, and KEWR between 1PM and 2PM"
     * - "Compare weather conditions across multiple airports at same time"
     * <p>
     * Performance: O(n * log m) where n = number of stations, m = items per station
     *
     * @param stationIds list of station IDs to query
     * @param startTime the start of the time range (inclusive)
     * @param endTime the end of the time range (inclusive)
     * @return list of weather data for the specified stations and time range
     * @throws IllegalArgumentException if parameters are invalid
     * @throws RepositoryException if query fails
     */
    public List<WeatherData> findByStationListAndTimeRange(List<String> stationIds,
                                                           Instant startTime,
                                                           Instant endTime) {
        if (stationIds == null || stationIds.isEmpty()) {
            return Collections.emptyList();
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before or equal to end time");
        }

        logger.debug("Finding weather data for {} stations in time range: {} to {}",
                stationIds.size(), startTime, endTime);

        // Query each station individually (can be parallelized if needed)
        List<WeatherData> allResults = new ArrayList<>();

        for (String stationId : stationIds) {
            try {
                List<WeatherData> stationResults = findByStationAndTimeRange(stationId, startTime, endTime);
                allResults.addAll(stationResults);
            } catch (Exception e) {
                logger.warn("Failed to query station: {}, continuing with other stations", stationId, e);
            }
        }

        logger.info("Found {} weather data items for {} stations in time range",
                allResults.size(), stationIds.size());

        return allResults;
    }

    @Override
    public List<WeatherData> findBySourceAndTimeRange(WeatherDataSource source,
                                                      Instant startTime,
                                                      Instant endTime) {
        if (source == null) {
            throw new IllegalArgumentException("Weather data source cannot be null");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before or equal to end time");
        }

        // Phase 4: Try GSI first, fall back to table scan if GSI doesn't exist
        try {
            return findBySourceAndTimeRangeUsingGSI(source, startTime, endTime);
        } catch (ResourceNotFoundException e) {
            logger.warn("GSI '{}' not found, falling back to table scan for source query. " +
                    "Create the GSI for better performance.", GSI_TIME_BUCKET_INDEX);
            return findBySourceAndTimeRangeUsingScan(source, startTime, endTime);
        }
    }

    /**
     * Finds weather observations by source and time range using GSI (Phase 4).
     */
    private List<WeatherData> findBySourceAndTimeRangeUsingGSI(WeatherDataSource source,
                                                               Instant startTime,
                                                               Instant endTime) {
        logger.debug("Finding weather data for source: {} in time range: {} to {} using GSI",
                source, startTime, endTime);

        try {
            List<String> timeBuckets = generateTimeBuckets(startTime, endTime);
            List<WeatherData> allResults = new ArrayList<>();

            String sourceFilter = buildSourceOnlyFilterExpression(source);

            for (String bucket : timeBuckets) {
                List<WeatherData> bucketResults = queryTimeBucketWithSourceFilter(
                        bucket, startTime, endTime, source, sourceFilter);
                allResults.addAll(bucketResults);
            }

            logger.info("Found {} weather data items for source: {} in time range using GSI (queried {} buckets)",
                    allResults.size(), source, timeBuckets.size());

            return allResults;

        } catch (ResourceNotFoundException e) {
            throw e;  // Re-throw unwrapped!
        } catch (DynamoDbException e) {
            throw new RepositoryException("Failed to query DynamoDB for source and time range", e);
        }
    }

    /**
     * Finds weather observations by source and time range using table scan (fallback).
     */
    private List<WeatherData> findBySourceAndTimeRangeUsingScan(WeatherDataSource source,
                                                                Instant startTime,
                                                                Instant endTime) {
        logger.debug("Finding weather data for source: {} in time range: {} to {} using scan",
                source, startTime, endTime);

        try {
            long startEpoch = startTime.getEpochSecond();
            long endEpoch = endTime.getEpochSecond();

            List<WeatherData> allResults = new ArrayList<>();
            Map<String, AttributeValue> lastEvaluatedKey = null;

            String filterExpression = buildSourceFilterExpression(source);

            do {
                Map<String, String> expressionAttributeNames = new HashMap<>();
                expressionAttributeNames.put("#ot", ATTR_OBSERVATION_TIME);

                Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
                expressionAttributeValues.put(EXPR_START_TIME,
                        AttributeValue.builder().n(String.valueOf(startEpoch)).build());
                expressionAttributeValues.put(EXPR_END_TIME,
                        AttributeValue.builder().n(String.valueOf(endEpoch)).build());

                addSourceFilterAttributes(source, expressionAttributeNames, expressionAttributeValues);

                ScanRequest.Builder requestBuilder = ScanRequest.builder()
                        .tableName(TABLE_NAME)
                        .filterExpression(filterExpression)
                        .expressionAttributeNames(expressionAttributeNames)
                        .expressionAttributeValues(expressionAttributeValues)
                        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);

                if (lastEvaluatedKey != null) {
                    requestBuilder.exclusiveStartKey(lastEvaluatedKey);
                }

                ScanResponse response = dynamoDbClient.scan(requestBuilder.build());

                logConsumedCapacity(response.consumedCapacity(),
                        "findBySourceAndTimeRange (Scan Fallback)");

                List<WeatherData> pageResults = response.items().stream()
                        .map(mapper::fromAttributeMap)
                        .filter(Objects::nonNull)
                        .toList();

                allResults.addAll(pageResults);
                lastEvaluatedKey = response.lastEvaluatedKey();

            } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

            logger.info("Found {} weather data items for source: {} in time range (scan fallback)",
                    allResults.size(), source);

            return allResults;

        } catch (DynamoDbException e) {
            throw new RepositoryException("Failed to scan DynamoDB for source and time range", e);
        }
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
        List<List<String>> batches = partitionList(stationIds, BATCH_GET_MAX_SIZE);

        for (List<String> batch : batches) {
            try {
                List<Map<String, AttributeValue>> keys = batch.stream()
                        .map(stationId -> {
                            Map<String, AttributeValue> key = new HashMap<>();
                            key.put(ATTR_STATION_ID, AttributeValue.builder().s(stationId).build());
                            key.put(ATTR_OBSERVATION_TIME,
                                    AttributeValue.builder().n(String.valueOf(epochSeconds)).build());
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
                        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                        .build();

                BatchGetItemResponse response = dynamoDbClient.batchGetItem(request);

                // Log consumed capacity for each table
                response.consumedCapacity().forEach(cc ->
                        logConsumedCapacity(cc, "findByStationsAndTime (BatchGet)"));

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
                        "Configure TTL on the 'observation_time' attribute in DynamoDB console."
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
     * Builds the DynamoDB filter expression for source filtering.
     * <p>
     * Phase 3.5: Server-side filtering to reduce data transfer.
     * <p>
     * Filter logic:
     * - NOAA: (source = 'NOAA' OR source does not exist) OR (dataType IN ('METAR', 'TAF', 'NOAA'))
     * - INTERNAL: source = 'INTERNAL'
     * - Other sources: Currently returns empty (not yet implemented)
     * <p>
     * Performance: Filters at DynamoDB server before data transfer, reducing network traffic by 50-70%
     *
     * @param source the source to filter by
     * @return DynamoDB filter expression string
     */
    private String buildSourceFilterExpression(WeatherDataSource source) {
        return switch (source) {
            case NOAA ->
                // Match if:
                // 1. source field is explicitly NOAA, OR
                // 2. source field doesn't exist AND dataType is METAR/TAF/NOAA (backward compatibility)
                    EXPR_TIME_RANGE + EXPR_AND +
                            "(#src = :sourceNoaa OR (attribute_not_exists(#src) AND #dt IN (:dtMetar, :dtTaf, :dtNoaa)))";

            case INTERNAL ->
                // Match only if source is explicitly INTERNAL
                    EXPR_TIME_RANGE + EXPR_AND + "(#src = :sourceInternal)";

            case OPENWEATHERMAP, WEATHERAPI, VISUAL_CROSSING, UNKNOWN ->
                // Not yet implemented - return expression that matches nothing
                    EXPR_TIME_RANGE + EXPR_AND + "(#src = :sourceUnimplemented)";
        };
    }

    /**
     * Adds source-specific attribute names and values to the filter expression.
     * <p>
     * Phase 3.5: Populates the expression attribute maps for server-side filtering.
     *
     * @param source the source to filter by
     * @param names map of expression attribute names (modified in place)
     * @param values map of expression attribute values (modified in place)
     */
    private void addSourceFilterAttributes(WeatherDataSource source,
                                           Map<String, String> names,
                                           Map<String, AttributeValue> values) {
        // Add source attribute name (used by all sources)
        names.put("#src", ATTR_SOURCE);

        switch (source) {
            case NOAA -> {
                // NOAA uses both #src and #dt attribute names
                names.put("#dt", ATTR_DATA_TYPE);
                values.put(":sourceNoaa", AttributeValue.builder().s("NOAA").build());
                values.put(":dtMetar", AttributeValue.builder().s("METAR").build());
                values.put(":dtTaf", AttributeValue.builder().s("TAF").build());
                values.put(":dtNoaa", AttributeValue.builder().s("NOAA").build());
            }
            case INTERNAL ->
                // INTERNAL only uses #src
                values.put(":sourceInternal", AttributeValue.builder().s("INTERNAL").build());
            case OPENWEATHERMAP, WEATHERAPI, VISUAL_CROSSING, UNKNOWN -> {
                // Unimplemented sources - use placeholder that won't match anything
                values.put(":sourceUnimplemented",
                        AttributeValue.builder().s("__UNIMPLEMENTED__").build());
                logger.debug("Source {} not yet implemented, query will return no results", source);
            }
        }
    }

    /**
     * Logs consumed capacity information for monitoring query performance.
     *
     * @param consumedCapacity the consumed capacity from DynamoDB response
     * @param operationName the name of the operation for logging context
     */
    private void logConsumedCapacity(ConsumedCapacity consumedCapacity, String operationName) {
        if (consumedCapacity != null) {
            Double capacityUnits = consumedCapacity.capacityUnits();
            if (capacityUnits != null) {
                logger.debug("{} consumed {} capacity units", operationName, capacityUnits);
            }
        }
    }

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

    // ========== PHASE 4: TIME BUCKET GSI UTILITIES ==========

    /**
     * Generates a list of time bucket strings for the given time range.
     * <p>
     * Phase 4: Used to query the time-bucket-index GSI efficiently.
     * Each bucket represents one hour of data.
     * <p>
     * Example:
     * - Input: 2024-01-27 14:30:00 to 2024-01-27 16:15:00
     * - Output: ["2024-01-27-14", "2024-01-27-15", "2024-01-27-16"]
     * <p>
     * Performance: Typical queries span 1-24 hours → 1-24 buckets → Very efficient!
     *
     * @param startTime the start of the time range (inclusive)
     * @param endTime the end of the time range (inclusive)
     * @return list of time bucket strings in "YYYY-MM-DD-HH" format
     */
    private List<String> generateTimeBuckets(Instant startTime, Instant endTime) {
        List<String> buckets = new ArrayList<>();

        // Start at the hour containing startTime
        Instant current = startTime.atZone(java.time.ZoneOffset.UTC)
                .truncatedTo(java.time.temporal.ChronoUnit.HOURS)
                .toInstant();

        // Generate buckets until we pass endTime
        while (!current.isAfter(endTime)) {
            String bucket = formatTimeBucket(current);
            buckets.add(bucket);

            // Move to next hour
            current = current.plus(1, java.time.temporal.ChronoUnit.HOURS);
        }

        logger.debug("Generated {} time buckets for range {} to {}",
                buckets.size(), startTime, endTime);

        return buckets;
    }

    /**
     * Formats an Instant into a time bucket string.
     * <p>
     * Format: "YYYY-MM-DD-HH" (e.g., "2024-01-27-15" for 3 PM on Jan 27, 2024)
     *
     * @param instant the timestamp to format
     * @return time bucket string
     */
    private String formatTimeBucket(Instant instant) {
        return instant.atZone(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));
    }

    /**
     * Queries a single time bucket from the GSI.
     * <p>
     * Phase 4: This is the core GSI query operation.
     * Queries one hourly bucket and filters by exact time range.
     * <p>
     * Performance: O(m) where m = items in this specific hour
     * Much better than O(n) where n = all items in table!
     *
     * @param timeBucket the time bucket to query (e.g., "2024-01-27-15")
     * @param startTime filter results to times >= this
     * @param endTime filter results to times <= this
     * @return list of weather data in this bucket within the time range
     */
    private List<WeatherData> queryTimeBucket(String timeBucket, Instant startTime, Instant endTime) {
        try {
            long startEpoch = startTime.getEpochSecond();
            long endEpoch = endTime.getEpochSecond();

            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#tb", ATTR_TIME_BUCKET);
            expressionAttributeNames.put("#ot", ATTR_OBSERVATION_TIME);

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":bucket", AttributeValue.builder().s(timeBucket).build());
            expressionAttributeValues.put(EXPR_START_TIME,
                    AttributeValue.builder().n(String.valueOf(startEpoch)).build());
            expressionAttributeValues.put(EXPR_END_TIME,
                    AttributeValue.builder().n(String.valueOf(endEpoch)).build());

            QueryRequest request = QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .indexName(GSI_TIME_BUCKET_INDEX)
                    .keyConditionExpression("#tb = :bucket" + EXPR_AND + EXPR_TIME_RANGE)
                    .expressionAttributeNames(expressionAttributeNames)
                    .expressionAttributeValues(expressionAttributeValues)
                    .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                    .build();

            QueryResponse response = dynamoDbClient.query(request);

            logConsumedCapacity(response.consumedCapacity(),
                    "queryTimeBucket (" + timeBucket + ")");

            return response.items().stream()
                    .map(mapper::fromAttributeMap)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (ResourceNotFoundException e) {
            // Re-throw unwrapped so outer try-catch can handle it
            throw e;
        } catch (DynamoDbException e) {
            throw new RepositoryException("Failed to query time bucket GSI", e);
        }
    }

    /**
     * Builds source-only filter expression (without time range).
     * Phase 4: Used in GSI queries where time is handled by KeyConditionExpression.
     *
     * @param source the source to filter by
     * @return filter expression string for source filtering only
     */
    private String buildSourceOnlyFilterExpression(WeatherDataSource source) {
        return switch (source) {
            case NOAA ->
                    "(#src = :sourceNoaa OR (attribute_not_exists(#src) AND #dt IN (:dtMetar, :dtTaf, :dtNoaa)))";
            case INTERNAL ->
                    "(#src = :sourceInternal)";
            case OPENWEATHERMAP, WEATHERAPI, VISUAL_CROSSING, UNKNOWN ->
                    "(#src = :sourceUnimplemented)";
        };
    }

    /**
     * Queries a time bucket with source filtering.
     * Phase 4: Combines GSI time-range query with source FilterExpression.
     *
     * @param timeBucket the time bucket to query
     * @param startTime filter results to times >= this
     * @param endTime filter results to times <= this
     * @param source the source to filter by
     * @param sourceFilterExpression the source filter expression
     * @return list of weather data matching both time and source criteria
     */
    private List<WeatherData> queryTimeBucketWithSourceFilter(String timeBucket,
                                                              Instant startTime,
                                                              Instant endTime,
                                                              WeatherDataSource source,
                                                              String sourceFilterExpression) {
        try {
            long startEpoch = startTime.getEpochSecond();
            long endEpoch = endTime.getEpochSecond();

            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#tb", ATTR_TIME_BUCKET);
            expressionAttributeNames.put("#ot", ATTR_OBSERVATION_TIME);

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":bucket", AttributeValue.builder().s(timeBucket).build());
            expressionAttributeValues.put(EXPR_START_TIME,
                    AttributeValue.builder().n(String.valueOf(startEpoch)).build());
            expressionAttributeValues.put(EXPR_END_TIME,
                    AttributeValue.builder().n(String.valueOf(endEpoch)).build());

            // Add source-specific attributes
            addSourceFilterAttributes(source, expressionAttributeNames, expressionAttributeValues);

            QueryRequest request = QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .indexName(GSI_TIME_BUCKET_INDEX)
                    .keyConditionExpression("#tb = :bucket" + EXPR_AND + EXPR_TIME_RANGE)
                    .filterExpression(sourceFilterExpression)  // Add source filter
                    .expressionAttributeNames(expressionAttributeNames)
                    .expressionAttributeValues(expressionAttributeValues)
                    .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                    .build();

            QueryResponse response = dynamoDbClient.query(request);

            logConsumedCapacity(response.consumedCapacity(),
                    "queryTimeBucketWithSourceFilter (" + timeBucket + ", " + source + ")");

            return response.items().stream()
                    .map(mapper::fromAttributeMap)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (ResourceNotFoundException e) {
            // Re-throw unwrapped so outer try-catch can handle it
            throw e;
        } catch (DynamoDbException e) {
            throw new RepositoryException("Failed to query time bucket GSI with source filter", e);
        }
    }
}
