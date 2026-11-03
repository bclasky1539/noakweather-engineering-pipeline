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
package weather.ingestion.service.source.noaa;

import weather.model.WeatherData;
import weather.processing.parser.common.ParseIssue;

import java.util.List;

/**
 * One fetched and parsed report, with the issues found while parsing it (#139).
 * <p>
 * The client hands this to the ingestion orchestrators so the issues reach the
 * per-batch summary and the metrics; they are not stored on the record itself
 * (that is #166).
 * <p>
 * Public because it is the return type of the orchestrators' {@code fetchFromNoaa} hook.
 *
 * @param data   the report, parsed or a fallback with the raw text
 * @param issues the parse issues; empty for a cleanly parsed report
 * @author bclasky1539
 */
public record ParsedReport(WeatherData data, List<ParseIssue> issues) {

    public ParsedReport {
        if (data == null || issues == null) {
            throw new IllegalArgumentException("Parsed report data and issues cannot be null");
        }
        issues = List.copyOf(issues);
    }
}
