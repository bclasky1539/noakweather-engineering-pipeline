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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared support for the golden-output regression tests (#137, #153 phase 4).
 * <p>
 * A golden test parses every report in a corpus file, renders each result as stable,
 * line-per-field text, and compares it with a recorded expected file. Run with
 * {@code -Dgolden.record=true} to re-record the expected file instead of comparing.
 * <p>
 * Rendering is deterministic: records are shown by component, sets and maps are sorted,
 * so the output never depends on hash iteration order.
 */
final class GoldenOutput {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoldenOutput.class);

    static final String RECORD_PROPERTY = "golden.record";
    private static final String CASE_HEADER = "=== ";
    // Surefire runs with the module directory as the working directory
    private static final Path RESOURCE_ROOT = Path.of("src/test/resources");

    private GoldenOutput() {
    }

    // ==================== Comparison ====================

    /**
     * Render every corpus report and compare with the expected file, or record it.
     *
     * @param corpusResource   classpath resource with one report per line ({@code #} comments allowed)
     * @param expectedResource classpath resource holding the recorded renderings
     * @param renderer         renders one raw report
     */
    static void verify(String corpusResource, String expectedResource, UnaryOperator<String> renderer)
            throws IOException {
        Map<String, String> actual = new LinkedHashMap<>();
        for (String raw : readCorpus(corpusResource)) {
            actual.put(raw, renderer.apply(raw));
        }

        if (Boolean.getBoolean(RECORD_PROPERTY)) {
            writeExpected(expectedResource, actual);
            return;
        }

        Map<String, String> expected = readExpected(expectedResource);

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
     * Append one {@code name: value} line.
     */
    static void field(StringBuilder sb, String name, Object value) {
        sb.append("  ").append(name).append(": ").append(format(value)).append('\n');
    }

    /**
     * One line per record component, so a diff points at the field that changed.
     * Non-records are rendered as a single line.
     */
    static void expand(StringBuilder sb, String name, Object value) {
        if (value == null || !value.getClass().isRecord()) {
            field(sb, name, value);
            return;
        }
        for (RecordComponent component : value.getClass().getRecordComponents()) {
            field(sb, name + "." + component.getName(), componentValue(value, component));
        }
    }

    /**
     * Deterministic single-line rendering of any value.
     */
    static String format(Object value) {
        if (value == null) {
            return "null";
        }
        if (value.getClass().isRecord()) {
            return Arrays.stream(value.getClass().getRecordComponents())
                    .map(component -> component.getName() + "=" + format(componentValue(value, component)))
                    .collect(Collectors.joining(", ", value.getClass().getSimpleName() + "[", "]"));
        }
        if (value instanceof Set<?> set) {
            return set.stream().map(GoldenOutput::format).sorted()
                    .collect(Collectors.joining(", ", "{", "}"));
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(GoldenOutput::format)
                    .collect(Collectors.joining(", ", "[", "]"));
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(e -> format(e.getKey()) + "=" + format(e.getValue())).sorted()
                    .collect(Collectors.joining(", ", "{", "}"));
        }
        return String.valueOf(value);
    }

    /**
     * Read one record component through its accessor.
     */
    static Object componentValue(Object target, RecordComponent component) {
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

    private static List<String> readCorpus(String corpusResource) throws IOException {
        return readResourceLines(corpusResource).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();
    }

    private static Map<String, String> readExpected(String expectedResource) throws IOException {
        Map<String, String> expected = new LinkedHashMap<>();
        String raw = null;
        StringBuilder rendering = new StringBuilder();

        for (String line : readResourceLines(expectedResource)) {
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

    private static void writeExpected(String expectedResource, Map<String, String> actual) throws IOException {
        StringBuilder sb = new StringBuilder();
        actual.forEach((raw, rendering) ->
                sb.append(CASE_HEADER).append(raw).append('\n').append(rendering).append('\n'));

        // Resource names start with "/", which Path.resolve would treat as absolute
        Path target = RESOURCE_ROOT.resolve(expectedResource.substring(1));
        Files.createDirectories(target.getParent());
        Files.writeString(target, sb.toString(), StandardCharsets.UTF_8);
        LOGGER.info("Recorded {} reports to {}", actual.size(), target.toAbsolutePath());
    }

    private static List<String> readResourceLines(String resource) throws IOException {
        InputStream in = GoldenOutput.class.getResourceAsStream(resource);
        assertThat(in)
                .as("Missing test resource %s; record it with -D%s=true", resource, RECORD_PROPERTY)
                .isNotNull();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        }
    }
}
