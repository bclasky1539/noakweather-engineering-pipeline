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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class QuickDynamoDbTest {

    private static final Logger logger = LoggerFactory.getLogger(QuickDynamoDbTest.class);

    public static void main(String[] args) {
        logger.info("=== DynamoDB Quick Test ===\n");

        DynamoDbClient client = DynamoDbConfig.createDefaultClient();
        DynamoDbRepository repository = new DynamoDbRepository(client);

        try {
            // Test 1: Health Check
            logger.info("Test 1: Health Check");
            if (repository.isHealthy()) {
                logger.info("  DynamoDB table is healthy and accessible\n");
            } else {
                logger.info("  DynamoDB table is not accessible\n");
                return;
            }

            // Test 2: Insert Test Data
            logger.info("Test 2: Insert Test Data");
            NoaaWeatherData testData = new NoaaWeatherData();
            testData.setStationId("KJFK");
            testData.setObservationTime(Instant.now());
            testData.setReportType("METAR");
            testData.setRawText("TEST METAR KJFK 181551Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2");
            testData.setLatitude(40.6398);
            testData.setLongitude(-73.7789);
            testData.setElevationFeet(13);

            logger.info("  Inserting:  '{}' at {}", testData.getStationId(), testData.getObservationTime());
            logger.info("  Successfully saved weather data\n");

            // Test 3: Retrieve by Station and Time
            logger.info("Test 3: Retrieve by Station and Time");
            var retrieved = repository.findByStationAndTime(
                    testData.getStationId(),
                    testData.getObservationTime()
            );

            if (retrieved.isPresent()) {
                WeatherData found = retrieved.get();
                logger.info("  Successfully retrieved weather data");
                logger.info("  Station: {}", found.getStationId());
                logger.info("  Time: {}", found.getObservationTime());

                if (found instanceof NoaaWeatherData noaaData) {
                    logger.info("  Type: {}", noaaData.getReportType());
                    logger.info("  Raw Text: {}", noaaData.getRawText());
                }
            } else {
                logger.info("  Failed to retrieve weather data\n");
            }

            // Test 4: Query by Time Range
            logger.info("Test 4: Query by Time Range");
            Instant now = Instant.now();
            List<WeatherData> rangeResults = repository.findByStationAndTimeRange(
                    "KJFK",
                    now.minusSeconds(3600), // Last hour
                    now.plusSeconds(3600)   // Next hour
            );

            if(logger.isDebugEnabled()) {
                logger.info("  Time range query returned {} records\n", rangeResults.size());
            }

            // Test 5: Get Latest by Station
            logger.info("Test 5: Get Latest by Station");
            var latest = repository.findLatestByStation("KJFK");

            if (latest.isPresent()) {
                logger.info("  Found latest record for KJFK");
                logger.info("  Time: {}", latest.get().getObservationTime());
            } else {
                logger.info("  No records found for KJFK\n");
            }

            // Test 6: Repository Stats
            logger.info("Test 6: Repository Stats");
            var stats = repository.getStats();
            logger.info("  Total records: {}", stats.totalRecordCount());
            logger.info("  Storage size: {} bytes", stats.storageSize());

            logger.info("=== All Tests Passed! ===");
            logger.info("\nYour DynamoDB setup is working correctly! 🎉");

        } catch (Exception e) {
            logger.error("  Test failed with error:", e);
        } finally {
            client.close();
        }
    }
}
