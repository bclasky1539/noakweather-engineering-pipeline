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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for DynamoDbTableConfig.
 * <p>
 * Tests the Phase 4 table and time-bucket GSI configuration builders to ensure
 * they produce correct DynamoDB table creation and update requests.
 *
 * @author bclasky1539
 * @version 2.0 - Phase 4: Time Bucket GSI Tests
 *
 */
@DisplayName("DynamoDB Table Configuration Tests - Phase 4")
class DynamoDbTableConfigTest {

    @Test
    @DisplayName("Should not allow instantiation of utility class")
    void shouldNotAllowInstantiation() {
        // Then - Verify constructor throws exception
        assertThatThrownBy(() -> {
            var constructor = DynamoDbTableConfig.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        })
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .cause()
                .hasMessage("Utility class - do not instantiate");
    }

    @Test
    @DisplayName("Should create table request with Phase 4 time-bucket GSI")
    void shouldCreateTableRequestWithGSI() {
        // When
        CreateTableRequest request = DynamoDbTableConfig.createTableRequestWithGSI();

        // Then - Verify table name
        assertThat(request.tableName()).isEqualTo(DynamoDbTableConfig.TABLE_NAME);

        // Then - Verify billing mode
        assertThat(request.billingMode()).isEqualTo(BillingMode.PAY_PER_REQUEST);

        // Then - Verify primary key schema
        assertThat(request.keySchema()).hasSize(2);
        assertThat(request.keySchema().get(0).attributeName()).isEqualTo(DynamoDbTableConfig.ATTR_STATION_ID);
        assertThat(request.keySchema().get(0).keyType()).isEqualTo(KeyType.HASH);
        assertThat(request.keySchema().get(1).attributeName()).isEqualTo(DynamoDbTableConfig.ATTR_OBSERVATION_TIME);
        assertThat(request.keySchema().get(1).keyType()).isEqualTo(KeyType.RANGE);

        // Then - Verify attribute definitions include time_bucket
        assertThat(request.attributeDefinitions()).hasSize(3);
        assertThat(request.attributeDefinitions())
                .extracting(AttributeDefinition::attributeName)
                .containsExactlyInAnyOrder(
                        DynamoDbTableConfig.ATTR_STATION_ID,
                        DynamoDbTableConfig.ATTR_OBSERVATION_TIME,
                        DynamoDbTableConfig.ATTR_TIME_BUCKET
                );

        // Then - Verify attribute types
        assertThat(request.attributeDefinitions())
                .filteredOn(attr -> attr.attributeName().equals(DynamoDbTableConfig.ATTR_STATION_ID))
                .extracting(AttributeDefinition::attributeType)
                .containsExactly(ScalarAttributeType.S);

        assertThat(request.attributeDefinitions())
                .filteredOn(attr -> attr.attributeName().equals(DynamoDbTableConfig.ATTR_OBSERVATION_TIME))
                .extracting(AttributeDefinition::attributeType)
                .containsExactly(ScalarAttributeType.N);

        assertThat(request.attributeDefinitions())
                .filteredOn(attr -> attr.attributeName().equals(DynamoDbTableConfig.ATTR_TIME_BUCKET))
                .extracting(AttributeDefinition::attributeType)
                .containsExactly(ScalarAttributeType.S);

        // Then - Verify GSI exists
        assertThat(request.hasGlobalSecondaryIndexes()).isTrue();
        assertThat(request.globalSecondaryIndexes()).hasSize(1);

        // Then - Verify GSI name
        assertThat(request.globalSecondaryIndexes().get(0).indexName())
                .isEqualTo(DynamoDbTableConfig.GSI_TIME_BUCKET_INDEX);
    }

    @Test
    @DisplayName("Should create table request without GSI")
    void shouldCreateTableRequestWithoutGSI() {
        // When
        CreateTableRequest request = DynamoDbTableConfig.createTableRequestWithoutGSI();

        // Then - Verify table name
        assertThat(request.tableName()).isEqualTo(DynamoDbTableConfig.TABLE_NAME);

        // Then - Verify billing mode
        assertThat(request.billingMode()).isEqualTo(BillingMode.PAY_PER_REQUEST);

        // Then - Verify no GSIs
        assertThat(request.hasGlobalSecondaryIndexes()).isFalse();

        // Then - Verify attribute definitions do NOT include time_bucket
        assertThat(request.attributeDefinitions()).hasSize(2);
        assertThat(request.attributeDefinitions())
                .extracting(AttributeDefinition::attributeName)
                .containsExactlyInAnyOrder(
                        DynamoDbTableConfig.ATTR_STATION_ID,
                        DynamoDbTableConfig.ATTR_OBSERVATION_TIME
                );

        // Then - Verify key schema still correct
        assertThat(request.keySchema()).hasSize(2);
        assertThat(request.keySchema().get(0).attributeName()).isEqualTo(DynamoDbTableConfig.ATTR_STATION_ID);
        assertThat(request.keySchema().get(1).attributeName()).isEqualTo(DynamoDbTableConfig.ATTR_OBSERVATION_TIME);
    }

    @Test
    @DisplayName("Should create time-bucket GSI with correct Phase 4 configuration")
    void shouldCreateTimeBucketGSIWithCorrectConfiguration() {
        // When
        CreateTableRequest request = DynamoDbTableConfig.createTableRequestWithGSI();

        // Then - Find time-bucket-index
        GlobalSecondaryIndex timeBucketIndex = request.globalSecondaryIndexes().get(0);
        assertThat(timeBucketIndex.indexName()).isEqualTo(DynamoDbTableConfig.GSI_TIME_BUCKET_INDEX);

        // Then - Verify key schema (time_bucket as PK, observation_time as SK)
        assertThat(timeBucketIndex.keySchema()).hasSize(2);
        assertThat(timeBucketIndex.keySchema().get(0).attributeName())
                .isEqualTo(DynamoDbTableConfig.ATTR_TIME_BUCKET);
        assertThat(timeBucketIndex.keySchema().get(0).keyType())
                .isEqualTo(KeyType.HASH);
        assertThat(timeBucketIndex.keySchema().get(1).attributeName())
                .isEqualTo(DynamoDbTableConfig.ATTR_OBSERVATION_TIME);
        assertThat(timeBucketIndex.keySchema().get(1).keyType())
                .isEqualTo(KeyType.RANGE);

        // Then - Verify projection (should be ALL)
        assertThat(timeBucketIndex.projection().projectionType()).isEqualTo(ProjectionType.ALL);
    }

    @Test
    @DisplayName("Should create UpdateTableRequest for adding time-bucket-index")
    void shouldCreateUpdateRequestForTimeBucketIndex() {
        // When
        UpdateTableRequest request = DynamoDbTableConfig.addTimeBucketIndexRequest();

        // Then - Verify table name
        assertThat(request.tableName()).isEqualTo(DynamoDbTableConfig.TABLE_NAME);

        // Then - Verify attribute definitions include time_bucket
        assertThat(request.attributeDefinitions()).hasSize(3);
        assertThat(request.attributeDefinitions())
                .extracting(AttributeDefinition::attributeName)
                .containsExactlyInAnyOrder(
                        DynamoDbTableConfig.ATTR_STATION_ID,
                        DynamoDbTableConfig.ATTR_OBSERVATION_TIME,
                        DynamoDbTableConfig.ATTR_TIME_BUCKET
                );

        // Then - Verify GSI update exists
        assertThat(request.hasGlobalSecondaryIndexUpdates()).isTrue();
        assertThat(request.globalSecondaryIndexUpdates()).hasSize(1);

        // Then - Verify it's a CREATE action
        GlobalSecondaryIndexUpdate update = request.globalSecondaryIndexUpdates().get(0);
        assertThat(update.create()).isNotNull();
        assertThat(update.create().indexName()).isEqualTo(DynamoDbTableConfig.GSI_TIME_BUCKET_INDEX);

        // Then - Verify key schema (time_bucket as PK, observation_time as SK)
        assertThat(update.create().keySchema()).hasSize(2);
        assertThat(update.create().keySchema().get(0).attributeName())
                .isEqualTo(DynamoDbTableConfig.ATTR_TIME_BUCKET);
        assertThat(update.create().keySchema().get(0).keyType())
                .isEqualTo(KeyType.HASH);
        assertThat(update.create().keySchema().get(1).attributeName())
                .isEqualTo(DynamoDbTableConfig.ATTR_OBSERVATION_TIME);
        assertThat(update.create().keySchema().get(1).keyType())
                .isEqualTo(KeyType.RANGE);

        // Then - Verify projection
        assertThat(update.create().projection().projectionType()).isEqualTo(ProjectionType.ALL);

        // Then - Verify no provisioned throughput (on-demand mode)
        assertThat(update.create().provisionedThroughput()).isNull();
    }

    @Test
    @DisplayName("Should create UpdateTableRequest with provisioned capacity")
    void shouldCreateUpdateRequestWithProvisionedCapacity() {
        // Given
        long expectedReadCapacity = 10L;
        long expectedWriteCapacity = 5L;

        // When
        UpdateTableRequest request = DynamoDbTableConfig.addTimeBucketIndexRequestWithProvisionedCapacity(
                expectedReadCapacity,
                expectedWriteCapacity
        );

        // Then - Verify table name
        assertThat(request.tableName()).isEqualTo(DynamoDbTableConfig.TABLE_NAME);

        // Then - Verify GSI update exists
        assertThat(request.hasGlobalSecondaryIndexUpdates()).isTrue();
        GlobalSecondaryIndexUpdate update = request.globalSecondaryIndexUpdates().get(0);
        assertThat(update.create()).isNotNull();

        // Then - Verify provisioned throughput is set
        assertThat(update.create().provisionedThroughput()).isNotNull();
        assertThat(update.create().provisionedThroughput().readCapacityUnits())
                .isEqualTo(expectedReadCapacity);
        assertThat(update.create().provisionedThroughput().writeCapacityUnits())
                .isEqualTo(expectedWriteCapacity);

        // Then - Verify index name and key schema still correct
        assertThat(update.create().indexName()).isEqualTo(DynamoDbTableConfig.GSI_TIME_BUCKET_INDEX);
        assertThat(update.create().keySchema()).hasSize(2);
    }

    @Test
    @DisplayName("Should use correct table name constant")
    void shouldUseCorrectTableName() {
        // Then
        assertThat(DynamoDbTableConfig.TABLE_NAME).isEqualTo("noakweather-data");
    }

    @Test
    @DisplayName("Should use correct attribute name constants")
    void shouldUseCorrectAttributeNames() {
        // Then
        assertThat(DynamoDbTableConfig.ATTR_STATION_ID).isEqualTo("station_id");
        assertThat(DynamoDbTableConfig.ATTR_OBSERVATION_TIME).isEqualTo("observation_time");
        assertThat(DynamoDbTableConfig.ATTR_TIME_BUCKET).isEqualTo("time_bucket");
        assertThat(DynamoDbTableConfig.ATTR_DATA).isEqualTo("data");
    }

    @Test
    @DisplayName("Should use correct Phase 4 GSI name constant")
    void shouldUseCorrectGSIName() {
        // Then
        assertThat(DynamoDbTableConfig.GSI_TIME_BUCKET_INDEX).isEqualTo("time-bucket-index");
    }

    @Test
    @DisplayName("Should verify GSI key schema matches repository expectations")
    void shouldVerifyGSIKeySchemaMatchesRepositoryExpectations() {
        // When
        CreateTableRequest request = DynamoDbTableConfig.createTableRequestWithGSI();
        GlobalSecondaryIndex gsi = request.globalSecondaryIndexes().get(0);

        // Then - This is the exact schema that DynamoDbRepository expects
        // PK: time_bucket (for bucketing by hour: "YYYY-MM-DD-HH")
        // SK: observation_time (for ordering within bucket)
        assertThat(gsi.keySchema().get(0).attributeName()).isEqualTo("time_bucket");
        assertThat(gsi.keySchema().get(0).keyType()).isEqualTo(KeyType.HASH);
        assertThat(gsi.keySchema().get(1).attributeName()).isEqualTo("observation_time");
        assertThat(gsi.keySchema().get(1).keyType()).isEqualTo(KeyType.RANGE);

        // Then - Index name matches what repository queries
        assertThat(gsi.indexName()).isEqualTo("time-bucket-index");
    }

    @Test
    @DisplayName("Should have consistent attribute definitions between with and without GSI")
    void shouldHaveConsistentAttributeDefinitionsBetweenMethods() {
        // When
        CreateTableRequest withGSI = DynamoDbTableConfig.createTableRequestWithGSI();
        CreateTableRequest withoutGSI = DynamoDbTableConfig.createTableRequestWithoutGSI();

        // Then - Both should have station_id and observation_time
        assertThat(withGSI.attributeDefinitions())
                .filteredOn(attr -> attr.attributeName().equals("station_id"))
                .hasSize(1);
        assertThat(withoutGSI.attributeDefinitions())
                .filteredOn(attr -> attr.attributeName().equals("station_id"))
                .hasSize(1);

        assertThat(withGSI.attributeDefinitions())
                .filteredOn(attr -> attr.attributeName().equals("observation_time"))
                .hasSize(1);
        assertThat(withoutGSI.attributeDefinitions())
                .filteredOn(attr -> attr.attributeName().equals("observation_time"))
                .hasSize(1);

        // Then - Only withGSI should have time_bucket
        assertThat(withGSI.attributeDefinitions())
                .filteredOn(attr -> attr.attributeName().equals("time_bucket"))
                .hasSize(1);
        assertThat(withoutGSI.attributeDefinitions())
                .filteredOn(attr -> attr.attributeName().equals("time_bucket"))
                .isEmpty();
    }

    @Test
    @DisplayName("Should create update request with correct attribute type for time_bucket")
    void shouldCreateUpdateRequestWithCorrectAttributeTypeForTimeBucket() {
        // When
        UpdateTableRequest request = DynamoDbTableConfig.addTimeBucketIndexRequest();

        // Then - time_bucket should be String (for "YYYY-MM-DD-HH" format)
        assertThat(request.attributeDefinitions())
                .filteredOn(attr -> attr.attributeName().equals("time_bucket"))
                .extracting(AttributeDefinition::attributeType)
                .containsExactly(ScalarAttributeType.S);
    }
}
