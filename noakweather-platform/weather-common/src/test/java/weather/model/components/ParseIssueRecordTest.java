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
package weather.model.components;

import org.junit.jupiter.api.Test;
import weather.model.enums.ParseIssueKind;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ParseIssueRecord} (#166).
 */
class ParseIssueRecordTest {

    @Test
    void testCreate_KeepsAllValues() {
        ParseIssueRecord issue = new ParseIssueRecord(ParseIssueKind.REJECTED, "METAR remarks",
                "hail size", "GR 1/0", "Visibility.distanceValue", "1/0",
                "Fraction denominator cannot be zero");

        assertEquals(ParseIssueKind.REJECTED, issue.kind());
        assertEquals("METAR remarks", issue.section());
        assertEquals("hail size", issue.handler());
        assertEquals("GR 1/0", issue.text());
        assertEquals("Visibility.distanceValue", issue.component());
        assertEquals("1/0", issue.rejectedValue());
        assertEquals("Fraction denominator cannot be zero", issue.reason());
    }

    @Test
    void testCreate_NullKind_Throws() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new ParseIssueRecord(null, "METAR remarks", "hail size", "GR 1/0", null, null, "bad"));
        assertEquals("Parse issue kind cannot be null", e.getMessage());
    }
}
