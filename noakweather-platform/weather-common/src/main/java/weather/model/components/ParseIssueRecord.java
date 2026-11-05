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

import weather.model.enums.ParseIssueKind;

/**
 * One problem found while parsing a report, as stored on the weather data (#166).
 * <p>
 * The parser records issues while it reads a report; this is the form they take on the
 * Bronze record, so they can be queried per report in Athena (e.g. the stations that most
 * often send rejected groups). Only these seven values are stored: add no derived accessors
 * here, since a getter would be serialized as an extra JSON key.
 *
 * @param kind          what kind of problem this is
 * @param section       the report section, e.g. "METAR main body", "METAR remarks"
 * @param handler       the group handler involved, e.g. "skyCondition"
 * @param text          the group as reported, e.g. "VV///"; null for a disabled handler
 * @param component     the rejected field as {@code ClassName.field}, when known
 * @param rejectedValue the rejected value as text, when known
 * @param reason        why the group was rejected, or the failure's message
 * @author bclasky1539
 */
public record ParseIssueRecord(ParseIssueKind kind,
                               String section,
                               String handler,
                               String text,
                               String component,
                               String rejectedValue,
                               String reason) {

    public ParseIssueRecord {
        if (kind == null) {
            throw new IllegalArgumentException("Parse issue kind cannot be null");
        }
    }
}
