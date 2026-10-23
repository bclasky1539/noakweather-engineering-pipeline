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
package weather.processing.parser.noaa;

import weather.exception.InvalidObservationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audit log for report groups the NOAA parsers reject as invalid data, main body
 * and remarks alike (#136). Every rejection is logged at DEBUG on one category,
 * so enabling DEBUG for {@link #CATEGORY} alone shows each rejected group with
 * its component and value.
 */
final class RejectedGroupLog {

    /**
     * Log category for rejected report groups.
     */
    static final String CATEGORY = "weather.processing.parser.noaa.Rejected";

    private static final Logger LOGGER = LoggerFactory.getLogger(CATEGORY);

    private RejectedGroupLog() {
    }

    /**
     * Log a rejected report group at DEBUG.
     *
     * @param groupType human-readable group name, e.g. "sky condition"
     * @param text      the rejected text from the report
     * @param e         the rejection; its component is included when it is an
     *                  {@link InvalidObservationException}
     */
    static void log(String groupType, String text, IllegalArgumentException e) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }
        if (e instanceof InvalidObservationException ioe) {
            LOGGER.debug("Rejected {} '{}' [{}]: {}", groupType, text, ioe.getComponent(), e.getMessage());
        } else {
            LOGGER.debug("Rejected {} '{}': {}", groupType, text, e.getMessage());
        }
    }
}
