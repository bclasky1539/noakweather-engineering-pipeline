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

/**
 * Test tool to generate large log output for testing log rollover.
 * This will generate approximately 15MB of log data to trigger size-based rollover.
 *
 * @author bclasky1539
 *
 */
public class TestLogRollover {
    private static final Logger logger = LoggerFactory.getLogger(TestLogRollover.class);

    public static void main(String[] args) {
        String separator = "=".repeat(80);

        logger.info(separator);
        logger.info("Testing Log Rollover - Generating ~15MB of log data");
        logger.info(separator);

        // Generate about 15MB of log data (each line is ~200 bytes with timestamp/metadata)
        // 15MB / 200 bytes = ~75,000 lines
        int linesToGenerate = 80000;
        int progressInterval = 10000;

        for (int i = 1; i <= linesToGenerate; i++) {
            // Generate a log line with some data (to make it substantial)
            logger.info("Log line {} - Testing rollover with some padding text to increase size: " +
                    "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor", i);

            if (i % progressInterval == 0) {
                logger.info("Progress: {}/{} lines generated", i, linesToGenerate);
            }
        }

        logger.info(separator);
        logger.info("Log generation complete! Generated {} lines", linesToGenerate);
        logger.info("Check the logs directory for rolled over files in archive/");
        logger.info(separator);
    }
}
