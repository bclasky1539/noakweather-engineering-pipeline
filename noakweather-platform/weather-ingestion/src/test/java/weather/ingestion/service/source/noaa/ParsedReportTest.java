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

import org.junit.jupiter.api.Test;
import weather.model.NoaaWeatherData;
import weather.model.WeatherData;
import weather.processing.parser.common.ParseIssue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ParsedReport}.
 */
class ParsedReportTest {

    private static final WeatherData DATA = new NoaaWeatherData("KJFK", Instant.now(), "METAR");
    private static final ParseIssue ISSUE = ParseIssue.handlerDisabled("METAR remarks", "peakWind");

    @Test
    void testCopiesTheIssueList() {
        List<ParseIssue> issues = new ArrayList<>(List.of(ISSUE));
        ParsedReport report = new ParsedReport(DATA, issues);

        issues.clear();

        assertEquals(List.of(ISSUE), report.issues());
        List<ParseIssue> stored = report.issues();
        assertThrows(UnsupportedOperationException.class, () -> stored.add(ISSUE));
    }

    @Test
    void testRejectsNullDataOrIssues() {
        List<ParseIssue> noIssues = List.of();

        assertThrows(IllegalArgumentException.class, () -> new ParsedReport(null, noIssues));
        assertThrows(IllegalArgumentException.class, () -> new ParsedReport(DATA, null));
    }
}
