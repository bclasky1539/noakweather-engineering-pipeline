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
package weather.processing.parser.common;

import org.junit.jupiter.api.Test;
import weather.exception.InvalidObservationException;
import weather.model.components.ParseIssueRecord;
import weather.model.enums.ParseIssueKind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ParseIssue}.
 */
class ParseIssueTest {

    private static final String SECTION = "METAR main body";

    @Test
    void testRejected_FromInvalidObservationException_KeepsComponentValueAndReason() {
        InvalidObservationException rejection = new InvalidObservationException(
                "SkyCondition.heightFeet", "///", "Vertical visibility must have height specified");

        ParseIssue issue = ParseIssue.rejected(SECTION, "skyCondition", "VV///", rejection);

        assertThat(issue).isEqualTo(new ParseIssue(ParseIssueKind.REJECTED, SECTION, "skyCondition", "VV///",
                "SkyCondition.heightFeet", "///", "Vertical visibility must have height specified"));
        assertThat(issue.isParserBug()).isFalse();
    }

    @Test
    void testRejected_FromNumberFormatException_KeepsMessageAsReason() {
        ParseIssue issue = ParseIssue.rejected(SECTION, "visibility", "M12X4",
                new NumberFormatException("For input string: \"12X4\""));

        assertThat(issue.kind()).isEqualTo(ParseIssueKind.REJECTED);
        assertThat(issue.component()).isNull();
        assertThat(issue.rejectedValue()).isNull();
        assertThat(issue.reason()).isEqualTo("For input string: \"12X4\"");
    }

    @Test
    void testFailed_KeepsMessageAndIsParserBug() {
        ParseIssue issue = ParseIssue.failed(SECTION, "wind", "28016KT", new IllegalStateException("boom"));

        assertThat(issue.kind()).isEqualTo(ParseIssueKind.FAILED);
        assertThat(issue.text()).isEqualTo("28016KT");
        assertThat(issue.reason()).isEqualTo("boom");
        assertThat(issue.isParserBug()).isTrue();
    }

    @Test
    void testFailed_WithoutMessage_UsesExceptionType() {
        ParseIssue issue = ParseIssue.failed(SECTION, "wind", "28016KT", new NullPointerException());

        assertThat(issue.reason()).isEqualTo("NullPointerException");
    }

    @Test
    void testHandlerDisabled_HasNoTextAndIsParserBug() {
        ParseIssue issue = ParseIssue.handlerDisabled("METAR remarks", "peakWind");

        assertThat(issue.kind()).isEqualTo(ParseIssueKind.HANDLER_DISABLED);
        assertThat(issue.handler()).isEqualTo("peakWind");
        assertThat(issue.text()).isNull();
        assertThat(issue.reason()).isEqualTo("Handler disabled for the rest of the section");
        assertThat(issue.isParserBug()).isTrue();
    }

    @Test
    void testConstructor_RejectsNullKind() {
        assertThatThrownBy(() -> new ParseIssue(null, SECTION, "wind", "28016KT", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Parse issue kind cannot be null");
    }

    @Test
    void testToRecord_CopiesAllValues() {
        ParseIssue issue = ParseIssue.rejected("METAR remarks", "hail size", "GR 1/0",
                new InvalidObservationException("Visibility.distanceValue", "1/0",
                        "Fraction denominator cannot be zero"));

        assertThat(issue.toRecord()).isEqualTo(new ParseIssueRecord(ParseIssueKind.REJECTED,
                "METAR remarks", "hail size", "GR 1/0", "Visibility.distanceValue", "1/0",
                "Fraction denominator cannot be zero"));
    }
}
