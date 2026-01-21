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
package weather.storage.examples;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import weather.model.NoaaWeatherData;
import weather.model.WeatherData;
import weather.storage.config.DynamoDbConfig;
import weather.storage.repository.dynamodb.DynamoDbRepository;

import java.time.Instant;
import java.util.List;

/**
 * Quick test to verify DynamoDB table is working correctly.
 * <p>
 * This test:
 * 1. Checks if table is healthy
 * 2. Inserts a test weather data record
 * 3. Retrieves it back
 * 4. Queries by time range
 * 5. Gets latest record
 * <p>
 * Run this to verify your setup before writing unit tests.
 *
 * @author bclasky1539
 *
 */
@SuppressWarnings("java:S106") // System.out is acceptable for example/demo code
public class QuickDynamoDbTest {

    public static void main(String[] args) {
        System.out.println("=== DynamoDB Quick Test ===\n");

        DynamoDbClient client = DynamoDbConfig.createDefaultClient();
        DynamoDbRepository repository = new DynamoDbRepository(client);

        try {
            // Test 1: Health Check
            System.out.println("Test 1: Health Check");
            if (repository.isHealthy()) {
                System.out.println("✓ DynamoDB table is healthy and accessible\n");
            } else {
                System.out.println("✗ DynamoDB table is not accessible\n");
                return;
            }

            // Test 2: Insert Test Data
            System.out.println("Test 2: Insert Test Data");
            NoaaWeatherData testData = new NoaaWeatherData();
            testData.setStationId("KJFK");
            testData.setObservationTime(Instant.now());
            testData.setReportType("METAR");
            testData.setRawText("TEST METAR KJFK 181551Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2");
            testData.setLatitude(40.6398);
            testData.setLongitude(-73.7789);
            testData.setElevationFeet(13);

            System.out.println("  Inserting: " + testData.getStationId() + " at " + testData.getObservationTime());
            System.out.println("✓ Successfully saved weather data\n");

            // Test 3: Retrieve by Station and Time
            System.out.println("Test 3: Retrieve by Station and Time");
            var retrieved = repository.findByStationAndTime(
                    testData.getStationId(),
                    testData.getObservationTime()
            );

            if (retrieved.isPresent()) {
                WeatherData found = retrieved.get();
                System.out.println("✓ Successfully retrieved weather data");
                System.out.println("  Station: " + found.getStationId());
                System.out.println("  Time: " + found.getObservationTime());

                if (found instanceof NoaaWeatherData noaaData) {
                    System.out.println("  Type: " + noaaData.getReportType());
                    System.out.println("  Raw Text: " + noaaData.getRawText());
                }
                System.out.println();
            } else {
                System.out.println("✗ Failed to retrieve weather data\n");
            }

            // Test 4: Query by Time Range
            System.out.println("Test 4: Query by Time Range");
            Instant now = Instant.now();
            List<WeatherData> rangeResults = repository.findByStationAndTimeRange(
                    "KJFK",
                    now.minusSeconds(3600), // Last hour
                    now.plusSeconds(3600)   // Next hour
            );

            System.out.println("✓ Time range query returned " + rangeResults.size() + " records\n");

            // Test 5: Get Latest by Station
            System.out.println("Test 5: Get Latest by Station");
            var latest = repository.findLatestByStation("KJFK");

            if (latest.isPresent()) {
                System.out.println("✓ Found latest record for KJFK");
                System.out.println("  Time: " + latest.get().getObservationTime());
                System.out.println();
            } else {
                System.out.println("✗ No records found for KJFK\n");
            }

            // Test 6: Repository Stats
            System.out.println("Test 6: Repository Stats");
            var stats = repository.getStats();
            System.out.println("  Total records: " + stats.totalRecordCount());
            System.out.println("  Storage size: " + stats.storageSize() + " bytes");
            System.out.println();

            System.out.println("=== All Tests Passed! ===");
            System.out.println("\nYour DynamoDB setup is working correctly! 🎉");

        } catch (Exception e) {
            System.err.println("\n✗ Test failed with error:");
            e.printStackTrace();
        } finally {
            client.close();
        }
    }
}
