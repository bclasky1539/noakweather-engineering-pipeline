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
import weather.processing.parser.common.ParseIssue;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Running count of parse issues across one batch of reports (#139).
 * <p>
 * Rejected groups are bad data in the reports: expected, so they are counted and the
 * handlers that reject most are listed. Failed groups and disabled handlers are parser
 * bugs, so a batch with any of them is logged at WARN.
 * <p>
 * The log line is one stable {@code key=value} line, so a CloudWatch Logs metric filter
 * can turn it into metrics and alarms later without a code change.
 * <p>
 * Thread-safe: a parallel batch adds to one summary from several threads.
 *
 * @author bclasky1539
 */
final class ParseIssueSummary {

    /**
     * How many rejecting handlers the log line lists.
     */
    static final int TOP_REJECTED_HANDLERS = 5;

    private final String reportType;
    private final Map<ParseIssue.Kind, Integer> countsByKind = new EnumMap<>(ParseIssue.Kind.class);
    private final Map<String, Integer> rejectedByHandler = new HashMap<>();
    private int reports;
    private int reportsWithIssues;

    /**
     * @param reportType the batch's report type, e.g. "METAR" or "TAF"
     */
    ParseIssueSummary(String reportType) {
        this.reportType = reportType;
    }

    /**
     * Count one parsed report and its issues.
     *
     * @param issues the report's parse issues; empty for a cleanly parsed report
     */
    synchronized void add(List<ParseIssue> issues) {
        reports++;
        if (!issues.isEmpty()) {
            reportsWithIssues++;
        }
        for (ParseIssue issue : issues) {
            countsByKind.merge(issue.kind(), 1, Integer::sum);
            if (issue.kind() == ParseIssue.Kind.REJECTED) {
                rejectedByHandler.merge(issue.handler(), 1, Integer::sum);
            }
        }
    }

    /**
     * @return the number of reports counted
     */
    synchronized int reports() {
        return reports;
    }

    /**
     * @return the number of reports with at least one issue
     */
    synchronized int reportsWithIssues() {
        return reportsWithIssues;
    }

    /**
     * @param kind the kind of issue
     * @return how many issues of that kind the batch had
     */
    synchronized int count(ParseIssue.Kind kind) {
        return countsByKind.getOrDefault(kind, 0);
    }

    /**
     * @return true if any report had a failed group or a disabled handler
     */
    synchronized boolean hasParserBugs() {
        return count(ParseIssue.Kind.FAILED) + count(ParseIssue.Kind.HANDLER_DISABLED) > 0;
    }

    /**
     * @return WARN when the batch had parser bugs, INFO otherwise
     */
    synchronized Level level() {
        return hasParserBugs() ? Level.WARN : Level.INFO;
    }

    /**
     * One stable line for the batch, e.g.
     * {@code ParseIssueSummary type=METAR reports=40 reportsWithIssues=3 rejected=4 failed=0
     * handlerDisabled=0 topRejected=[tempDewpoint=2, peak wind=1, skyCondition=1]}.
     *
     * @return the log line
     */
    synchronized String toLogLine() {
        return "ParseIssueSummary type=%s reports=%d reportsWithIssues=%d rejected=%d failed=%d handlerDisabled=%d topRejected=%s"
                .formatted(reportType, reports, reportsWithIssues,
                        count(ParseIssue.Kind.REJECTED), count(ParseIssue.Kind.FAILED),
                        count(ParseIssue.Kind.HANDLER_DISABLED), topRejected());
    }

    /**
     * The handlers that rejected most, most first; ties in name order.
     */
    private String topRejected() {
        return rejectedByHandler.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_REJECTED_HANDLERS)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
