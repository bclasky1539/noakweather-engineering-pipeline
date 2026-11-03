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

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import weather.processing.parser.common.ParseIssue;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ParseIssueSummary} (#139).
 */
class ParseIssueSummaryTest {

    private static final String SECTION = "METAR main body";

    private static ParseIssue rejected(String handler) {
        return new ParseIssue(ParseIssue.Kind.REJECTED, SECTION, handler, "x", null, null, "bad");
    }

    @Test
    void testEmptyBatch_IsInfoWithZeroCounts() {
        ParseIssueSummary summary = new ParseIssueSummary("METAR");

        assertEquals(Level.INFO, summary.level());
        assertEquals(
                "ParseIssueSummary type=METAR reports=0 reportsWithIssues=0 rejected=0 failed=0 handlerDisabled=0 topRejected=[]",
                summary.toLogLine());
    }

    @Test
    void testCountsReportsAndIssuesByKind() {
        ParseIssueSummary summary = new ParseIssueSummary("METAR");

        summary.add(List.of());
        summary.add(List.of(rejected("tempDewpoint"), rejected("skyCondition")));
        summary.add(List.of(rejected("tempDewpoint")));

        assertEquals(3, summary.reports());
        assertEquals(2, summary.reportsWithIssues());
        assertEquals(3, summary.count(ParseIssue.Kind.REJECTED));
        assertEquals(0, summary.count(ParseIssue.Kind.FAILED));
        assertFalse(summary.hasParserBugs());
        assertEquals(Level.INFO, summary.level());
        assertEquals(
                "ParseIssueSummary type=METAR reports=3 reportsWithIssues=2 rejected=3 failed=0 handlerDisabled=0 "
                        + "topRejected=[tempDewpoint=2, skyCondition=1]",
                summary.toLogLine());
    }

    @Test
    void testFailedGroup_IsParserBugLoggedAtWarn() {
        ParseIssueSummary summary = new ParseIssueSummary("TAF");

        summary.add(List.of(ParseIssue.failed("TAF forecast period", "wind", "28016KT",
                new IllegalStateException("boom"))));

        assertEquals(1, summary.count(ParseIssue.Kind.FAILED));
        assertTrue(summary.hasParserBugs());
        assertEquals(Level.WARN, summary.level());
        // Failures are counted, not listed with the rejecting handlers
        assertTrue(summary.toLogLine().endsWith("rejected=0 failed=1 handlerDisabled=0 topRejected=[]"),
                summary.toLogLine());
    }

    @Test
    void testDisabledHandler_IsParserBugLoggedAtWarn() {
        ParseIssueSummary summary = new ParseIssueSummary("METAR");

        summary.add(List.of(ParseIssue.handlerDisabled("METAR remarks", "peakWind")));

        assertEquals(1, summary.count(ParseIssue.Kind.HANDLER_DISABLED));
        assertEquals(Level.WARN, summary.level());
    }

    @Test
    void testTopRejected_MostFirstThenByName_LimitedToFive() {
        ParseIssueSummary summary = new ParseIssueSummary("METAR");
        List<ParseIssue> issues = new ArrayList<>();
        for (String handler : List.of("g", "f", "e", "d", "c", "b", "a", "a", "b")) {
            issues.add(rejected(handler));
        }

        summary.add(issues);

        // a and b have 2 each (name order), then the 1s in name order, cut off after five
        assertTrue(summary.toLogLine().endsWith("topRejected=[a=2, b=2, c=1, d=1, e=1]"),
                summary.toLogLine());
    }
}
