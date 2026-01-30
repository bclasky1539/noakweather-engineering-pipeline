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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import weather.storage.config.DynamoDbTableConfig;

import java.net.URI;

/**
 * Base class for DynamoDB integration tests using Testcontainers.
 * <p>
 * This class provides:
 * - LocalStack container with DynamoDB
 * - Pre-configured DynamoDB client
 * - Table creation and cleanup
 * - Test data builders
 * - Helper methods for common operations
 * <p>
 * Philosophy:
 * - Tests run against real DynamoDB (via LocalStack)
 * - Each test gets a clean table state
 * - Container is shared across all tests in the class (for speed)
 * - Table is recreated between tests (for isolation)
 * <p>
 * Usage:
 * Extend this class in your integration test:
 *
 * <pre>{@code
 * class MyDynamoDbIntegrationTest extends BaseDynamoDbIntegrationTest {
 *
 *     @Test
 *     void testSomething() {
 *         // Use dynamoDbClient and repository
 *         WeatherData data = TestDataBuilder.createMetar("KJFK");
 *         repository.save(data);
 *         // ... assertions
 *     }
 * }
 * }</pre>
 *
 * @author bclasky1539
 *
 */
@Testcontainers
public abstract class BaseDynamoDbIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseDynamoDbIntegrationTest.class);

    // Table configuration
    protected static final String TABLE_NAME = DynamoDbTableConfig.TABLE_NAME;
    protected static final String PARTITION_KEY = "station_id";
    protected static final String SORT_KEY = "observation_time";

    // LocalStack container (shared across all tests in the class)
    @Container
    @SuppressWarnings("resource")
    protected static final GenericContainer<?> localstack = new GenericContainer<>(
            DockerImageName.parse("localstack/localstack:3.0"))
            .withExposedPorts(4566)  // LocalStack edge port
            .withEnv("SERVICES", "dynamodb")
            .withEnv("DEBUG", "1")  // Optional: for debugging
            .withReuse(true)
            .waitingFor(Wait.forLogMessage(".*Ready.*", 1));

    // DynamoDB client (shared, but table is recreated per test)
    protected static DynamoDbClient dynamoDbClient;

    // Repository (recreated per test with fresh table)
    protected DynamoDbRepository repository;

    /**
     * Initialize LocalStack container and DynamoDB client once for all tests.
     * Container startup is expensive (~10-15 seconds), so we do it once.
     */
    @BeforeAll
    @SuppressWarnings("HttpUrlsUsage")
    static void initializeContainer() {
        logger.info("Initializing LocalStack container with DynamoDB...");

        // Create DynamoDB client pointing to LocalStack
        dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(URI.create(
                        "http://" + localstack.getHost() + ":" + localstack.getMappedPort(4566)))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")
                ))
                .build();

        logger.info("DynamoDB client initialized successfully");
        logger.info("LocalStack endpoint: http://{}:{}",
                localstack.getHost(), localstack.getMappedPort(4566));
    }

    /**
     * Setup for each test:
     * 1. Create fresh DynamoDB table
     * 2. Initialize repository
     * <p>
     * This ensures test isolation - each test starts with empty table.
     */
    @BeforeEach
    void setUp() {
        logger.info("Setting up test - creating fresh DynamoDB table...");

        // Delete existing table if present
        DynamoDbTestHelper.deleteTableIfExists(dynamoDbClient);

        logger.info("Created fresh DynamoDB table...");

        // Create new table with GSIs
        DynamoDbTestHelper.createTableWithGSI(dynamoDbClient);

        logger.info("Table is active...");
        logger.info("Test setup complete - table created and repository initialized");

        // Initialize repository
        repository = new DynamoDbRepository(dynamoDbClient);
    }

    /**
     * Cleanup after each test:
     * Delete the table to ensure clean state for next test.
     */
    @AfterEach
    void tearDown() {
        logger.info("Tearing down test - deleting DynamoDB table...");
        DynamoDbTestHelper.deleteTableIfExists(dynamoDbClient);
    }

    /**
     * Shutdown DynamoDB client after all tests complete.
     */
    @AfterAll
    static void shutdownClient() {
        logger.info("Shutting down DynamoDB client...");
        if (dynamoDbClient != null) {
            dynamoDbClient.close();
        }
        logger.info("DynamoDB client closed");
    }

    /**
     * Helper method to verify table exists and is accessible.
     * Useful for debugging test failures.
     *
     * @return true if table exists and is active
     */
    protected boolean isTableReady() {
        try {
            DescribeTableResponse response = dynamoDbClient.describeTable(
                    DescribeTableRequest.builder()
                            .tableName(TABLE_NAME)
                            .build()
            );
            return response.table().tableStatus() == TableStatus.ACTIVE;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Helper method to get current item count in table.
     * Useful for assertions and debugging.
     * <p>
     * Note: This scans the entire table, so use sparingly.
     *
     * @return number of items in the table
     */
    protected long getTableItemCount() {
        try {
            ScanResponse response = dynamoDbClient.scan(
                    ScanRequest.builder()
                            .tableName(TABLE_NAME)
                            .select(Select.COUNT)
                            .build()
            );
            return response.count();
        } catch (Exception e) {
            logger.error("Error getting table item count", e);
            return -1;
        }
    }

    /**
     * Helper method to clear all items from table.
     * Useful when you want to reset table state mid-test.
     */
    protected void clearTable() {
        logger.info("Clearing all items from table...");

        try {
            // Scan to get all items
            ScanResponse scanResponse = dynamoDbClient.scan(
                    ScanRequest.builder()
                            .tableName(TABLE_NAME)
                            .build()
            );

            // Delete each item
            for (var item : scanResponse.items()) {
                dynamoDbClient.deleteItem(
                        DeleteItemRequest.builder()
                                .tableName(TABLE_NAME)
                                .key(item)
                                .build()
                );
            }

            logger.info("Cleared {} items from table", scanResponse.count());

        } catch (Exception e) {
            logger.error("Error clearing table", e);
        }
    }
}
