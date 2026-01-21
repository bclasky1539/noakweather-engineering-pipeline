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

import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Configuration for AWS DynamoDB client.
 * Uses default credential provider chain (reads from ~/.aws/credentials).
 *
 * @author bclasky1539
 *
 */
public class DynamoDbConfig {

    private static final Region DEFAULT_REGION = Region.US_EAST_1;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private DynamoDbConfig() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }

    /**
     * Creates a DynamoDB client with default configuration.
     * Uses credentials from ~/.aws/credentials and us-east-1 region.
     *
     * @return configured DynamoDB client
     */
    public static DynamoDbClient createDefaultClient() {
        return DynamoDbClient.builder()
                .region(DEFAULT_REGION)
                .credentialsProvider(AwsCredentialsProviderChain.builder()
                        .addCredentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .addCredentialsProvider(ProfileCredentialsProvider.create())
                        .addCredentialsProvider(InstanceProfileCredentialsProvider.create())
                        .build())
                .build();
    }

    /**
     * Creates a DynamoDB client with a specific region.
     *
     * @param region the AWS region to use
     * @return configured DynamoDB client
     */
    public static DynamoDbClient createClient(Region region) {
        return DynamoDbClient.builder()
                .region(region)
                .credentialsProvider(AwsCredentialsProviderChain.builder()
                        .addCredentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .addCredentialsProvider(ProfileCredentialsProvider.create())
                        .addCredentialsProvider(InstanceProfileCredentialsProvider.create())
                        .build())
                .build();
    }
}
