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

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weather.model.NoaaMetarData;
import weather.model.NoaaWeatherData;
import weather.processing.parser.common.ParseResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden-output regression test for {@link NoaaMetarParser} (#137).
 * <p>
 * Parses every report in {@code golden/metar-golden-corpus.txt} and compares a
 * deterministic text rendering of the result with {@code golden/metar-golden-expected.txt}.
 * Any change to parsed output, main body or remarks, fails with the report and both renderings.
 * <p>
 * The expected file was recorded on main before #137 removed the remark registry pass,
 * so it doubles as the "output with and without" comparison that issue asks for.
 * <p>
 * To re-record after an intended output change, run from the repository root:
 * <pre>
 * mvn test -pl noakweather-platform/weather-processing -am -Dtest=NoaaMetarParserGoldenOutputTest \
 *     -Dsurefire.failIfNoSpecifiedTests=false -Dgolden.record=true
 * </pre>
 * then review the diff of the expected file before committing it.
 * <p>
 * Observation time is deliberately left out: reports without a date header resolve
 * their year and month from the current date, so it would change over time.
 */
class NoaaMetarParserGoldenOutputTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoaaMetarParserGoldenOutputTest.class);

    private static final String CORPUS_RESOURCE = "/golden/metar-golden-corpus.txt";
    private static final String EXPECTED_RESOURCE = "/golden/metar-golden-expected.txt";
    // Surefire runs with the module directory as the working directory
    private static final Path EXPECTED_SOURCE = Path.of("src/test/resources/golden/metar-golden-expected.txt");
    private static final String RECORD_PROPERTY = "golden.record";
    private static final String CASE_HEADER = "=== ";

    @Test
    void parsedOutputMatchesGoldenFile() throws IOException {
        Map<String, String> actual = new LinkedHashMap<>();
        for (String raw : readCorpus()) {
            actual.put(raw, render(raw));
        }

        if (Boolean.getBoolean(RECORD_PROPERTY)) {
            writeExpected(actual);
            return;
        }

        Map<String, String> expected = readExpected();

        assertThat(actual.keySet())
                .as("Corpus and expected file must list the same reports, in order; re-record if the corpus changed")
                .containsExactlyElementsOf(expected.keySet());

        SoftAssertions softly = new SoftAssertions();
        actual.forEach((raw, rendering) ->
                softly.assertThat(rendering).as(raw).isEqualTo(expected.get(raw)));
        softly.assertAll();
    }

    // ==================== Rendering ====================

    /**
     * Render one parse result as stable, line-per-field text.
     */
    static String render(String raw) {
        ParseResult<NoaaWeatherData> result = new NoaaMetarParser().parse(raw);
        if (result.isFailure()) {
            return "  FAILURE: " + result.getErrorMessage() + "\n";
        }

        NoaaMetarData data = (NoaaMetarData) result.getData().orElseThrow();
        StringBuilder sb = new StringBuilder();

        field(sb, "stationId", data.getStationId());
        field(sb, "reportType", data.getReportType());
        field(sb, "reportModifier", data.getReportModifier());
        field(sb, "unparsedMainBody", data.getUnparsedMainBody());
        expand(sb, "conditions", data.getConditions());
        field(sb, "runwayVisualRange", data.getRunwayVisualRange());
        field(sb, "runwayStates", data.getRunwayStates());
        field(sb, "recentWeather", data.getRecentWeather());
        field(sb, "recentWeatherNotObservable", data.isRecentWeatherNotObservable());
        field(sb, "noSignificantChange", data.isNoSignificantChange());

        // Top-level copies of remark values (copyRemarksToTopLevel)
        field(sb, "automatedStation", data.getAutomatedStation());
        field(sb, "seaLevelPressure", data.getSeaLevelPressure());
        field(sb, "hourlyPrecipitation", data.getHourlyPrecipitation());
        field(sb, "sixHourMaxTemp", data.getSixHourMaxTemp());
        field(sb, "sixHourMinTemp", data.getSixHourMinTemp());
        field(sb, "twentyFourHourMaxTemp", data.getTwentyFourHourMaxTemp());
        field(sb, "twentyFourHourMinTemp", data.getTwentyFourHourMinTemp());
        field(sb, "threeHourPressureTendency", data.getThreeHourPressureTendency());
        field(sb, "peakWind", data.getPeakWind());
        field(sb, "windShift", data.getWindShift());
        field(sb, "pressureRapidChange", data.getPressureRapidChange());
        field(sb, "icing", data.getIcing());

        expand(sb, "remarks", data.getRemarks());
        return sb.toString();
    }

    private static void field(StringBuilder sb, String name, Object value) {
        sb.append("  ").append(name).append(": ").append(format(value)).append('\n');
    }

    /**
     * One line per record component, so a diff points at the field that changed.
     */
    private static void expand(StringBuilder sb, String name, Object value) {
        if (value == null || !value.getClass().isRecord()) {
            field(sb, name, value);
            return;
        }
        for (RecordComponent component : value.getClass().getRecordComponents()) {
            field(sb, name + "." + component.getName(), componentValue(value, component));
        }
    }

    /**
     * Deterministic rendering: records by component, sets and maps sorted, so the
     * output does not depend on hash iteration order (e.g. Set.of).
     */
    private static String format(Object value) {
        if (value == null) {
            return "null";
        }
        if (value.getClass().isRecord()) {
            List<String> parts = new ArrayList<>();
            for (RecordComponent component : value.getClass().getRecordComponents()) {
                parts.add(component.getName() + "=" + format(componentValue(value, component)));
            }
            return value.getClass().getSimpleName() + "[" + String.join(", ", parts) + "]";
        }
        if (value instanceof Set<?> set) {
            return set.stream().map(NoaaMetarParserGoldenOutputTest::format).sorted()
                    .collect(Collectors.joining(", ", "{", "}"));
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(NoaaMetarParserGoldenOutputTest::format)
                    .collect(Collectors.joining(", ", "[", "]"));
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(e -> format(e.getKey()) + "=" + format(e.getValue())).sorted()
                    .collect(Collectors.joining(", ", "{", "}"));
        }
        return String.valueOf(value);
    }

    private static Object componentValue(Object target, RecordComponent component) {
        try {
            Method accessor = component.getAccessor();
            accessor.trySetAccessible();
            return accessor.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read " + target.getClass().getSimpleName()
                    + "." + component.getName(), e);
        }
    }

    // ==================== Files ====================

    private static List<String> readCorpus() throws IOException {
        return readResourceLines(CORPUS_RESOURCE).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();
    }

    private static Map<String, String> readExpected() throws IOException {
        Map<String, String> expected = new LinkedHashMap<>();
        String raw = null;
        StringBuilder rendering = new StringBuilder();

        for (String line : readResourceLines(EXPECTED_RESOURCE)) {
            if (line.startsWith(CASE_HEADER)) {
                if (raw != null) {
                    expected.put(raw, rendering.toString());
                }
                raw = line.substring(CASE_HEADER.length());
                rendering.setLength(0);
            } else if (raw != null && !line.isEmpty()) {
                rendering.append(line).append('\n');
            }
        }
        if (raw != null) {
            expected.put(raw, rendering.toString());
        }
        return expected;
    }

    private static void writeExpected(Map<String, String> actual) throws IOException {
        StringBuilder sb = new StringBuilder();
        actual.forEach((raw, rendering) ->
                sb.append(CASE_HEADER).append(raw).append('\n').append(rendering).append('\n'));

        Files.createDirectories(EXPECTED_SOURCE.getParent());
        Files.writeString(EXPECTED_SOURCE, sb.toString(), StandardCharsets.UTF_8);
        LOGGER.info("Recorded {} reports to {}", actual.size(), EXPECTED_SOURCE.toAbsolutePath());
    }

    private static List<String> readResourceLines(String resource) throws IOException {
        InputStream in = NoaaMetarParserGoldenOutputTest.class.getResourceAsStream(resource);
        assertThat(in)
                .as("Missing test resource %s; record it with -D%s=true", resource, RECORD_PROPERTY)
                .isNotNull();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        }
    }
}
