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

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for DynamoDbConfig utility class.
 * <p>
 * Note: These tests verify client creation but do not make actual AWS calls.
 * Integration tests should verify actual DynamoDB connectivity.
 *
 * @author bclasky1539
 *
 */
class DynamoDbConfigTest {

    @Test
    void shouldNotBeInstantiable() throws NoSuchMethodException {
        // Given
        Constructor<DynamoDbConfig> constructor = DynamoDbConfig.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When/Then
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .hasRootCauseMessage("Utility class - do not instantiate");
    }

    @Test
    void shouldCreateDefaultClient() {
        // When
        DynamoDbClient client = DynamoDbConfig.createDefaultClient();

        // Then
        assertThat(client).isNotNull();

        // Verify client is properly configured (without making AWS calls)
        assertThat(client.serviceName()).isEqualTo("dynamodb");

        // Clean up
        client.close();
    }

    @Test
    void shouldCreateClientWithUsEast1Region() {
        // When
        DynamoDbClient client = DynamoDbConfig.createClient(Region.US_EAST_1);

        // Then
        assertThat(client).isNotNull();
        assertThat(client.serviceName()).isEqualTo("dynamodb");

        // Clean up
        client.close();
    }

    @Test
    void shouldCreateClientWithUsWest2Region() {
        // When
        DynamoDbClient client = DynamoDbConfig.createClient(Region.US_WEST_2);

        // Then
        assertThat(client).isNotNull();
        assertThat(client.serviceName()).isEqualTo("dynamodb");

        // Clean up
        client.close();
    }

    @Test
    void shouldCreateClientWithEuWest1Region() {
        // When
        DynamoDbClient client = DynamoDbConfig.createClient(Region.EU_WEST_1);

        // Then
        assertThat(client).isNotNull();
        assertThat(client.serviceName()).isEqualTo("dynamodb");

        // Clean up
        client.close();
    }

    @Test
    void shouldCreateClientWithApSoutheast1Region() {
        // When
        DynamoDbClient client = DynamoDbConfig.createClient(Region.AP_SOUTHEAST_1);

        // Then
        assertThat(client).isNotNull();
        assertThat(client.serviceName()).isEqualTo("dynamodb");

        // Clean up
        client.close();
    }

    @Test
    void shouldCreateMultipleClientsIndependently() {
        // When
        DynamoDbClient client1 = DynamoDbConfig.createDefaultClient();
        DynamoDbClient client2 = DynamoDbConfig.createClient(Region.US_WEST_2);

        // Then
        assertThat(client1).isNotNull();
        assertThat(client2).isNotNull();
        assertThat(client1).isNotSameAs(client2);

        // Clean up
        client1.close();
        client2.close();
    }

    @Test
    void defaultClientShouldBeReusable() {
        // When - Create multiple clients using default method
        DynamoDbClient client1 = DynamoDbConfig.createDefaultClient();
        DynamoDbClient client2 = DynamoDbConfig.createDefaultClient();

        // Then - Should create separate instances
        assertThat(client1).isNotNull();
        assertThat(client2).isNotNull();
        assertThat(client1).isNotSameAs(client2);

        // Clean up
        client1.close();
        client2.close();
    }
}
