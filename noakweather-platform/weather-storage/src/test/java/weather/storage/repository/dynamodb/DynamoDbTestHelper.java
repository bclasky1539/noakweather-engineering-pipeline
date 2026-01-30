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

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import weather.storage.config.DynamoDbTableConfig;

/**
 * Helper class for setting up DynamoDB tables in tests.
 * Works with both Testcontainers (local) and AWS DynamoDB.
 * <p>
 * Phase 4 Update: Now works with single time-bucket-index GSI instead of multiple GSIs.
 *
 * @author bclasky1539
 * @version 2.0 - Phase 4: Time Bucket GSI Support
 *
 */
public class DynamoDbTestHelper {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private DynamoDbTestHelper() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }

    /**
     * Create table with Phase 4 time-bucket GSI for testing.
     * Waits for table and GSI to become ACTIVE.
     *
     * @param client DynamoDB client
     * @return true if table was created, false if it already existed
     */
    public static boolean createTableWithGSI(DynamoDbClient client) {
        try {
            // Try to create the table with Phase 4 GSI
            CreateTableRequest request = DynamoDbTableConfig.createTableRequestWithGSI();
            client.createTable(request);

            // Wait for table to become ACTIVE
            waitForTableActive(client, DynamoDbTableConfig.TABLE_NAME);

            // Wait for GSI to become ACTIVE (important for tests)
            waitForGSIActive(client, DynamoDbTableConfig.TABLE_NAME);

            return true;

        } catch (ResourceInUseException e) {
            // Table already exists, that's fine
            return false;
        }
    }

    /**
     * Create table WITHOUT GSI for testing backward-compatible fallback logic.
     * This is useful for testing Phase 4's graceful degradation to table scans.
     *
     * @param client DynamoDB client
     * @return true if table was created, false if it already existed
     */
    public static boolean createTableWithoutGSI(DynamoDbClient client) {
        try {
            // Create table without GSI (for testing fallback logic)
            CreateTableRequest request = DynamoDbTableConfig.createTableRequestWithoutGSI();
            client.createTable(request);

            // Wait for table to become ACTIVE
            waitForTableActive(client, DynamoDbTableConfig.TABLE_NAME);

            return true;

        } catch (ResourceInUseException e) {
            // Table already exists, that's fine
            return false;
        }
    }

    /**
     * Delete table if it exists.
     * Useful for test cleanup.
     *
     * @param client DynamoDB client
     */
    public static void deleteTableIfExists(DynamoDbClient client) {
        try {
            client.deleteTable(DeleteTableRequest.builder()
                    .tableName(DynamoDbTableConfig.TABLE_NAME)
                    .build());

            // Wait for deletion to complete
            waitForTableDeleted(client, DynamoDbTableConfig.TABLE_NAME);

        } catch (ResourceNotFoundException e) {
            // Table doesn't exist, that's fine
        }
    }

    /**
     * Wait for table to become ACTIVE using AWS SDK Waiter.
     */
    private static void waitForTableActive(DynamoDbClient client, String tableName) {
        try {
            client.waiter().waitUntilTableExists(
                    DescribeTableRequest.builder()
                            .tableName(tableName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to wait for table to become ACTIVE: " + tableName, e);
        }
    }

    /**
     * Wait for Phase 4 time-bucket GSI to become ACTIVE.
     * Important for tests - queries will fail if GSI isn't ready.
     * <p>
     * Note: AWS SDK doesn't provide a built-in waiter for GSI status,
     * so we implement custom polling logic using LockSupport for efficient waiting.
     */
    private static void waitForGSIActive(DynamoDbClient client, String tableName) {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = 30_000; // 30 seconds max
        long pollIntervalNanos = 1_000_000_000L; // Poll every second (1 billion nanos)

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            DescribeTableResponse response = client.describeTable(
                    DescribeTableRequest.builder()
                            .tableName(tableName)
                            .build()
            );

            if (!response.table().hasGlobalSecondaryIndexes()) {
                // No GSIs defined, nothing to wait for
                return;
            }

            // Check if time-bucket-index GSI is ACTIVE
            boolean gsiActive = response.table().globalSecondaryIndexes()
                    .stream()
                    .filter(gsi -> gsi.indexName().equals(DynamoDbTableConfig.GSI_TIME_BUCKET_INDEX))
                    .anyMatch(gsi -> gsi.indexStatus() == IndexStatus.ACTIVE);

            if (gsiActive) {
                return;
            }

            // Park thread for poll interval (more efficient than Thread.sleep)
            java.util.concurrent.locks.LockSupport.parkNanos(pollIntervalNanos);
        }

        throw new RuntimeException("GSI did not become ACTIVE within timeout");
    }

    /**
     * Wait for table to be deleted using AWS SDK Waiter.
     */
    private static void waitForTableDeleted(DynamoDbClient client, String tableName) {
        try {
            client.waiter().waitUntilTableNotExists(
                    DescribeTableRequest.builder()
                            .tableName(tableName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to wait for table deletion: " + tableName, e);
        }
    }

    /**
     * Verify that Phase 4 time-bucket-index GSI exists and is ACTIVE.
     * Useful for test assertions.
     *
     * @param client DynamoDB client
     * @throws AssertionError if GSI doesn't exist or isn't ACTIVE
     */
    public static void verifyTimeBucketGSIExists(DynamoDbClient client) {
        DescribeTableResponse response = client.describeTable(
                DescribeTableRequest.builder()
                        .tableName(DynamoDbTableConfig.TABLE_NAME)
                        .build()
        );

        if (!response.table().hasGlobalSecondaryIndexes()) {
            throw new AssertionError("Table has no GSIs");
        }

        // Find the time-bucket-index GSI
        GlobalSecondaryIndexDescription timeBucketGSI = response.table().globalSecondaryIndexes()
                .stream()
                .filter(gsi -> gsi.indexName().equals(DynamoDbTableConfig.GSI_TIME_BUCKET_INDEX))
                .findFirst()
                .orElseThrow(() -> new AssertionError("time-bucket-index GSI not found"));

        // Verify it's ACTIVE
        if (timeBucketGSI.indexStatus() != IndexStatus.ACTIVE) {
            throw new AssertionError("time-bucket-index GSI is not ACTIVE: " + timeBucketGSI.indexStatus());
        }
    }

    /**
     * Verify that table exists but has NO GSI.
     * Useful for testing Phase 4 backward-compatible fallback logic.
     *
     * @param client DynamoDB client
     * @throws AssertionError if table doesn't exist or has GSIs
     */
    public static void verifyTableHasNoGSI(DynamoDbClient client) {
        DescribeTableResponse response = client.describeTable(
                DescribeTableRequest.builder()
                        .tableName(DynamoDbTableConfig.TABLE_NAME)
                        .build()
        );

        if (response.table().hasGlobalSecondaryIndexes()) {
            throw new AssertionError("Table should not have GSIs but has: " +
                    response.table().globalSecondaryIndexes().size());
        }
    }

    /**
     * Check if time-bucket-index GSI exists (without throwing exception).
     * Useful for conditional test logic.
     *
     * @param client DynamoDB client
     * @return true if GSI exists and is ACTIVE, false otherwise
     */
    public static boolean hasTimeBucketGSI(DynamoDbClient client) {
        try {
            DescribeTableResponse response = client.describeTable(
                    DescribeTableRequest.builder()
                            .tableName(DynamoDbTableConfig.TABLE_NAME)
                            .build()
            );

            if (!response.table().hasGlobalSecondaryIndexes()) {
                return false;
            }

            return response.table().globalSecondaryIndexes()
                    .stream()
                    .anyMatch(gsi -> gsi.indexName().equals(DynamoDbTableConfig.GSI_TIME_BUCKET_INDEX) &&
                            gsi.indexStatus() == IndexStatus.ACTIVE);

        } catch (ResourceNotFoundException e) {
            return false;
        }
    }
}
