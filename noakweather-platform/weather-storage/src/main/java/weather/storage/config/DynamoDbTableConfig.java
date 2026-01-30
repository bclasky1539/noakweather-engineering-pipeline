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
package weather.storage.config;

import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;

/**
 * Enhanced DynamoDB table configuration with Phase 4 Time Bucket GSI support.
 * <p>
 * This configuration works for both:
 * - Local DynamoDB (Testcontainers for testing)
 * - AWS DynamoDB (production)
 * <p>
 * Phase 4 GSI Strategy:
 * The time-bucket-index GSI uses hourly time buckets (e.g., "2024-01-27-15") as the partition key.
 * This provides:
 * - Better distribution across DynamoDB partitions
 * - Efficient time-range queries (query specific buckets, not entire table)
 * - 50x performance improvement over table scans
 * <p>
 * Primary Table Access Patterns:
 * 1. Query by station + time range (uses main table)
 * 2. Query by time range across all stations (uses time-bucket-index GSI)
 * 3. Query by source + time range (uses time-bucket-index GSI with filter)
 * <p>
 * Backward Compatibility:
 * The repository code gracefully falls back to table scans if GSI doesn't exist,
 * enabling zero-downtime deployment.
 *
 * @author bclasky1539
 * @version 2.0 - Phase 4: Time Bucket GSI
 *
 */
public class DynamoDbTableConfig {

    // Table and attribute names
    public static final String TABLE_NAME = "noakweather-data";
    public static final String ATTR_STATION_ID = "station_id";
    public static final String ATTR_OBSERVATION_TIME = "observation_time";
    public static final String ATTR_TIME_BUCKET = "time_bucket";
    public static final String ATTR_DATA = "data";

    // GSI name (Phase 4)
    public static final String GSI_TIME_BUCKET_INDEX = "time-bucket-index";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private DynamoDbTableConfig() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }

    /**
     * Create table request with Phase 4 time-bucket GSI.
     * Use this for initial table creation (local or AWS).
     * <p>
     * Note: GSI creation can take several minutes in production.
     * Consider creating table without GSI first, then adding it separately.
     */
    public static CreateTableRequest createTableRequestWithGSI() {
        return CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(getPrimaryKeySchema())
                .attributeDefinitions(getAttributeDefinitionsWithTimeBucket())
                .globalSecondaryIndexes(getTimeBucketGSI())
                .billingMode(BillingMode.PAY_PER_REQUEST) // On-demand mode
                .build();
    }

    /**
     * Create table request without GSI.
     * Use this if you want to add GSI later separately (recommended for zero-downtime deployment).
     * <p>
     * Deployment Strategy:
     * 1. Deploy code with fallback logic
     * 2. Create table without GSI (fast)
     * 3. Add GSI later when ready (optional performance optimization)
     */
    public static CreateTableRequest createTableRequestWithoutGSI() {
        return CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(getPrimaryKeySchema())
                .attributeDefinitions(getAttributeDefinitions())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();
    }

    /**
     * Primary key schema: station_id (HASH) + observation_time (RANGE)
     * <p>
     * This enables efficient queries like:
     * - Get specific observation: station_id = "KJFK" AND observation_time = 1706360400
     * - Get time range for station: station_id = "KJFK" AND observation_time BETWEEN start AND end
     */
    private static List<KeySchemaElement> getPrimaryKeySchema() {
        return List.of(
                KeySchemaElement.builder()
                        .attributeName(ATTR_STATION_ID)
                        .keyType(KeyType.HASH)  // Partition key
                        .build(),
                KeySchemaElement.builder()
                        .attributeName(ATTR_OBSERVATION_TIME)
                        .keyType(KeyType.RANGE) // Sort key
                        .build()
        );
    }

    /**
     * Attribute definitions for primary key only (no GSI).
     * Only includes attributes used in key schemas.
     */
    private static List<AttributeDefinition> getAttributeDefinitions() {
        return List.of(
                AttributeDefinition.builder()
                        .attributeName(ATTR_STATION_ID)
                        .attributeType(ScalarAttributeType.S) // String
                        .build(),
                AttributeDefinition.builder()
                        .attributeName(ATTR_OBSERVATION_TIME)
                        .attributeType(ScalarAttributeType.N) // Number (epoch seconds)
                        .build()
        );
    }

    /**
     * Attribute definitions including time_bucket for GSI.
     * Used when creating table with GSI or adding GSI to existing table.
     */
    private static List<AttributeDefinition> getAttributeDefinitionsWithTimeBucket() {
        return List.of(
                AttributeDefinition.builder()
                        .attributeName(ATTR_STATION_ID)
                        .attributeType(ScalarAttributeType.S)
                        .build(),
                AttributeDefinition.builder()
                        .attributeName(ATTR_OBSERVATION_TIME)
                        .attributeType(ScalarAttributeType.N)
                        .build(),
                AttributeDefinition.builder()
                        .attributeName(ATTR_TIME_BUCKET)
                        .attributeType(ScalarAttributeType.S) // String (format: "YYYY-MM-DD-HH")
                        .build()
        );
    }

    /**
     * Phase 4 Time Bucket GSI Configuration
     * <p>
     * Use Case: Find all weather observations across stations within a time range
     * <p>
     * Example Queries:
     * - "Get all weather observations between 2PM and 4PM today"
     * - "Find all stations that reported data in the last hour"
     * - "Get all NOAA observations from 3PM to 5PM yesterday"
     * <p>
     * Key Schema:
     * - PK: time_bucket (hourly buckets: "2024-01-27-15")
     * - SK: observation_time (epoch seconds for ordering within bucket)
     * <p>
     * Performance:
     * - Query cost: O(m) where m = items in time range
     * - Table scan cost: O(n) where n = total items in table
     * - Typical improvement: 50x faster for cross-station time queries
     * <p>
     * How it works:
     * 1. Query calculates which buckets to query (e.g., ["2024-01-27-14", "2024-01-27-15"])
     * 2. Each bucket is queried independently via GSI
     * 3. Results are combined and filtered by exact time range
     * 4. Only relevant data is scanned (not entire table)
     */
    private static GlobalSecondaryIndex getTimeBucketGSI() {
        // Build key schema list explicitly to avoid unchecked varargs warning
        List<KeySchemaElement> keySchema = new java.util.ArrayList<>();
        keySchema.add(KeySchemaElement.builder()
                .attributeName(ATTR_TIME_BUCKET)
                .keyType(KeyType.HASH)
                .build());
        keySchema.add(KeySchemaElement.builder()
                .attributeName(ATTR_OBSERVATION_TIME)
                .keyType(KeyType.RANGE)
                .build());

        return GlobalSecondaryIndex.builder()
                .indexName(GSI_TIME_BUCKET_INDEX)
                .keySchema(keySchema)
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .build();
    }

    /**
     * Request to add time-bucket-index GSI to existing table.
     * Use this to add GSI to an already created table without downtime.
     * <p>
     * Usage (AWS CLI):
     * <pre>
     * aws dynamodb update-table --cli-input-json file://add-gsi.json
     * </pre>
     * <p>
     * Usage (SDK):
     * <pre>
     * dynamoDbClient.updateTable(DynamoDbTableConfig.addTimeBucketIndexRequest());
     * </pre>
     * <p>
     * GSI Creation Process:
     * 1. CREATING: AWS is creating the index (1-5 minutes typically)
     * 2. BACKFILLING: AWS is populating existing data (time depends on table size)
     * 3. ACTIVE: GSI is ready to use
     * <p>
     * During creation:
     * - Table remains fully available for reads/writes
     * - No downtime
     * - Repository automatically uses GSI once ACTIVE
     * <p>
     * Monitoring:
     * <pre>
     * aws dynamodb describe-table --table-name noakweather-data \
     *   --query 'Table.GlobalSecondaryIndexes[?IndexName==`time-bucket-index`].IndexStatus'
     * </pre>
     */
    public static UpdateTableRequest addTimeBucketIndexRequest() {
        // Build key schema list explicitly to avoid unchecked varargs warning
        List<KeySchemaElement> keySchema = new java.util.ArrayList<>();
        keySchema.add(KeySchemaElement.builder()
                .attributeName(ATTR_TIME_BUCKET)
                .keyType(KeyType.HASH)
                .build());
        keySchema.add(KeySchemaElement.builder()
                .attributeName(ATTR_OBSERVATION_TIME)
                .keyType(KeyType.RANGE)
                .build());

        // Use consumer builder pattern for nested objects (avoids inspection warnings)
        GlobalSecondaryIndexUpdate gsiUpdate = GlobalSecondaryIndexUpdate.builder()
                .create(create -> create
                        .indexName(GSI_TIME_BUCKET_INDEX)
                        .keySchema(keySchema)  // Explicit list (no varargs)
                        .projection(projection -> projection  // Consumer (no varargs)
                                .projectionType(ProjectionType.ALL)))
                .build();

        return UpdateTableRequest.builder()
                .tableName(TABLE_NAME)
                .attributeDefinitions(getAttributeDefinitionsWithTimeBucket())
                .globalSecondaryIndexUpdates(gsiUpdate)
                .build();
    }

    /**
     * Request to add time-bucket-index GSI with provisioned capacity (for production).
     * Use this if you want to control and predict costs with provisioned throughput.
     * <p>
     * Recommended for:
     * - Production workloads with predictable traffic
     * - Cost optimization (can be cheaper than on-demand for steady usage)
     * - Need for reserved capacity guarantees
     *
     * @param readCapacityUnits  Initial read capacity (start low, monitor, adjust)
     * @param writeCapacityUnits Initial write capacity (start low, monitor, adjust)
     */
    public static UpdateTableRequest addTimeBucketIndexRequestWithProvisionedCapacity(
            long readCapacityUnits,
            long writeCapacityUnits) {
        // Build key schema list explicitly to avoid unchecked varargs warning
        List<KeySchemaElement> keySchema = new java.util.ArrayList<>();
        keySchema.add(KeySchemaElement.builder()
                .attributeName(ATTR_TIME_BUCKET)
                .keyType(KeyType.HASH)
                .build());
        keySchema.add(KeySchemaElement.builder()
                .attributeName(ATTR_OBSERVATION_TIME)
                .keyType(KeyType.RANGE)
                .build());

        // Use consumer builder pattern for nested objects (avoids inspection warnings)
        GlobalSecondaryIndexUpdate gsiUpdate = GlobalSecondaryIndexUpdate.builder()
                .create(create -> create
                        .indexName(GSI_TIME_BUCKET_INDEX)
                        .keySchema(keySchema)  // Explicit list (no varargs)
                        .projection(projection -> projection  // Consumer (no varargs)
                                .projectionType(ProjectionType.ALL))
                        .provisionedThroughput(pt -> pt  // Consumer (no varargs)
                                .readCapacityUnits(readCapacityUnits)
                                .writeCapacityUnits(writeCapacityUnits)))
                .build();

        return UpdateTableRequest.builder()
                .tableName(TABLE_NAME)
                .attributeDefinitions(getAttributeDefinitionsWithTimeBucket())
                .globalSecondaryIndexUpdates(gsiUpdate)
                .build();
    }
}
