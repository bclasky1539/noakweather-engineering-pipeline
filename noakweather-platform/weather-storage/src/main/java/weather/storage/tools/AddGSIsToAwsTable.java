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
package weather.storage.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import weather.storage.config.DynamoDbConfig;
import weather.storage.config.DynamoDbTableConfig;

import java.util.concurrent.locks.LockSupport;

/**
 * Utility to add Phase 4 time-bucket-index GSI to existing DynamoDB table.
 * <p>
 * CONFIGURATION: Update the Region in main() method if your table is not in US_EAST_1
 * <p>
 * Usage:
 *   mvn compile exec:java -Dexec.mainClass="weather.storage.tools.AddGSIsToAwsTable"
 * <p>
 * This script:
 * 1. Checks if time-bucket-index GSI already exists
 * 2. Adds the GSI if missing (on-demand billing mode)
 * 3. Waits for GSI to become ACTIVE
 * 4. Handles errors gracefully
 * <p>
 * WARNING: GSI creation on AWS takes time and triggers backfilling!
 * - time-bucket-index creation: ~5-30 minutes (depending on data size)
 * - During creation, you'll be charged for writes
 * - Table remains available during GSI creation
 * - Repository will automatically use GSI once ACTIVE (no code changes needed)
 * <p>
 * Phase 4 Update:
 * - Now adds single time-bucket-index GSI (not multiple GSIs)
 * - Enables 50x faster time-range queries across stations
 * - Backward compatible: code works without GSI (falls back to table scan)
 *
 * @author bclasky1539
 * @version 2.0 - Phase 4: Time Bucket GSI
 *
 */
public class AddGSIsToAwsTable {

    private static final Logger logger = LoggerFactory.getLogger(AddGSIsToAwsTable.class);
    private static final int MAX_WAIT_MINUTES = 60;
    private static final long POLL_INTERVAL_NANOS = 10_000_000_000L; // 10 seconds in nanoseconds

    public static void main(String[] args) {
        String separator = "=".repeat(70);
        logger.info(separator);
        logger.info("Add Phase 4 Time Bucket GSI to DynamoDB Table: {}", DynamoDbTableConfig.TABLE_NAME);
        logger.info(separator);

        // Create DynamoDB client with US_EAST_1 region (change if your table is in different region)
        // Using try-with-resources for automatic resource management
        try (DynamoDbClient client = DynamoDbConfig.createClient(Region.US_EAST_1)) {
            // Step 1: Verify table exists
            logger.info("\n[1/4] Verifying table exists...");
            verifyTableExists(client);
            logger.info("✓ Table exists and is ACTIVE");

            // Step 2: Check current GSI status
            logger.info("\n[2/4] Checking current GSI status...");
            GSIStatus status = checkGSIStatus(client);
            status.print();

            // Step 3: Add time-bucket-index if missing
            if (!status.hasTimeBucketIndex) {
                logger.info("\n[3/4] Adding time-bucket-index GSI...");
                logger.info("  This GSI enables 50x faster time-range queries across all stations");
                addTimeBucketIndexGSI(client);
            } else {
                logger.info("\n[3/4] time-bucket-index already exists - skipping");
            }

            // Step 4: Final verification
            logger.info("\n[4/4] Final verification...");
            GSIStatus finalStatus = checkGSIStatus(client);
            finalStatus.print();

            if (finalStatus.hasTimeBucketIndex && finalStatus.timeBucketIndexActive) {
                logger.info("");
                logger.info(separator);
                logger.info("  SUCCESS: time-bucket-index GSI is created and ACTIVE!");
                logger.info("  Repository will now automatically use GSI for time-range queries.");
                logger.info(separator);
            } else {
                logger.warn("");
                logger.warn(separator);
                logger.warn("  WARNING: GSI is still creating. Check AWS console.");
                logger.warn("  Repository will use table scan fallback until GSI is ACTIVE.");
                logger.warn(separator);
            }

        } catch (ResourceNotFoundException e) {
            logger.error("");
            logger.error(separator);
            logger.error("  ERROR: Table not found - {}", e.getMessage());
            logger.error(separator);
            logger.error("Make sure table '{}' exists in your AWS account.", DynamoDbTableConfig.TABLE_NAME);
            System.exit(1);
        } catch (ResourceInUseException e) {
            logger.error("");
            logger.error(separator);
            logger.error("  ERROR: Table or GSI operation already in progress - {}", e.getMessage());
            logger.error(separator);
            logger.error("Wait for the current operation to complete and try again.");
            System.exit(1);
        } catch (LimitExceededException e) {
            logger.error("");
            logger.error(separator);
            logger.error("  ERROR: DynamoDB limits exceeded - {}", e.getMessage());
            logger.error(separator);
            logger.error("You may have too many concurrent operations. Wait and retry.");
            System.exit(1);
        } catch (DynamoDbException e) {
            logger.error("");
            logger.error(separator);
            logger.error("  ERROR: DynamoDB error - {}", e.getMessage());
            logger.error(separator);
            logger.error("DynamoDB operation failed", e);
            System.exit(1);
        }
        // Client automatically closed by try-with-resources
    }

    private static void verifyTableExists(DynamoDbClient client) {
        try {
            DescribeTableResponse response = client.describeTable(
                    DescribeTableRequest.builder()
                            .tableName(DynamoDbTableConfig.TABLE_NAME)
                            .build()
            );

            TableStatus status = response.table().tableStatus();
            if (status != TableStatus.ACTIVE) {
                throw new IllegalStateException("Table is not ACTIVE. Current status: " + status);
            }
        } catch (ResourceNotFoundException e) {
            throw new IllegalStateException("Table does not exist: " + DynamoDbTableConfig.TABLE_NAME);
        }
    }

    private static GSIStatus checkGSIStatus(DynamoDbClient client) {
        DescribeTableResponse response = client.describeTable(
                DescribeTableRequest.builder()
                        .tableName(DynamoDbTableConfig.TABLE_NAME)
                        .build()
        );

        GSIStatus status = new GSIStatus();

        if (response.table().hasGlobalSecondaryIndexes()) {
            for (GlobalSecondaryIndexDescription gsi : response.table().globalSecondaryIndexes()) {
                String indexName = gsi.indexName();
                IndexStatus indexStatus = gsi.indexStatus();

                if (indexName.equals(DynamoDbTableConfig.GSI_TIME_BUCKET_INDEX)) {
                    status.hasTimeBucketIndex = true;
                    status.timeBucketIndexActive = (indexStatus == IndexStatus.ACTIVE);
                    status.timeBucketIndexStatus = indexStatus.toString();
                }
            }
        }

        return status;
    }

    private static void addTimeBucketIndexGSI(DynamoDbClient client) {
        try {
            logger.info("  Sending request to create {}...", DynamoDbTableConfig.GSI_TIME_BUCKET_INDEX);

            // Use on-demand billing mode (no provisioned capacity needed)
            UpdateTableRequest request = DynamoDbTableConfig.addTimeBucketIndexRequest();
            client.updateTable(request);

            logger.info("     Request accepted. GSI creation started.");
            logger.info("     Waiting for {} to become ACTIVE...", DynamoDbTableConfig.GSI_TIME_BUCKET_INDEX);
            logger.info("     (This may take 5-30 minutes depending on data size)");

            waitForTimeBucketIndexActive(client);

        } catch (ResourceInUseException e) {
            logger.warn("     {} is already being created or updated", DynamoDbTableConfig.GSI_TIME_BUCKET_INDEX);
            waitForTimeBucketIndexActive(client);
        } catch (LimitExceededException e) {
            throw new IllegalStateException("Cannot create GSI: DynamoDB limits exceeded. " +
                    "You may have too many concurrent GSI operations. Wait and retry.", e);
        }
    }

    private static void waitForTimeBucketIndexActive(DynamoDbClient client) {
        long startTime = System.currentTimeMillis();
        long maxWaitMillis = MAX_WAIT_MINUTES * 60L * 1000L;

        while (System.currentTimeMillis() - startTime < maxWaitMillis) {
            // Wait before checking status (using LockSupport instead of Thread.sleep)
            LockSupport.parkNanos(POLL_INTERVAL_NANOS);

            GSIStatus status = checkGSIStatus(client);

            int elapsedMinutes = (int) ((System.currentTimeMillis() - startTime) / 60000);
            logger.info("     [{} min] Status: {}", elapsedMinutes, status.timeBucketIndexStatus);

            if (status.timeBucketIndexActive) {
                logger.info("  ✓ {} is now ACTIVE!", DynamoDbTableConfig.GSI_TIME_BUCKET_INDEX);
                return;
            }

            if (status.timeBucketIndexStatus != null && status.timeBucketIndexStatus.equals("CREATING")) {
                // Still creating, continue waiting
                continue;
            }

            if (status.timeBucketIndexStatus != null && !status.timeBucketIndexStatus.equals("ACTIVE")) {
                throw new IllegalStateException("GSI creation failed. Status: " + status.timeBucketIndexStatus);
            }
        }

        throw new IllegalStateException("Timeout: GSI did not become ACTIVE within " + MAX_WAIT_MINUTES + " minutes");
    }

    private static class GSIStatus {
        boolean hasTimeBucketIndex = false;
        boolean timeBucketIndexActive = false;
        String timeBucketIndexStatus = "NOT_FOUND";

        void print() {
            logger.info("  time-bucket-index GSI (Phase 4):");
            logger.info("    Exists: {}", hasTimeBucketIndex ? "✓" : "✗");
            logger.info("    Status: {}", timeBucketIndexStatus);
            logger.info("    Active: {}", timeBucketIndexActive ? "✓" : "✗");

            if (!hasTimeBucketIndex) {
                logger.info("    Note: Repository will use table scan fallback until GSI is created");
            } else if (timeBucketIndexActive) {
                logger.info("    Note: Repository will automatically use GSI for optimal performance");
            }
        }
    }
}
