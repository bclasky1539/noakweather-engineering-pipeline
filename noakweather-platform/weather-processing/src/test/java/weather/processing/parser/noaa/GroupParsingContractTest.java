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

import org.junit.jupiter.api.Test;
import weather.exception.InvalidObservationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static weather.processing.parser.noaa.SectionPolicy.RejectedText.KEEP;
import static weather.processing.parser.noaa.SectionPolicy.Scheduling.PASSES;
import static weather.processing.parser.noaa.SectionPolicy.UnrecognizedToken.STOP;

/**
 * Validation tests for the group parsing engine's value types (#153, #154):
 * {@link GroupHandler}, {@link GroupOutcome}, {@link SectionPolicy} and
 * {@link SectionResult}.
 */
class GroupParsingContractTest {

    private static final GroupHandler.Step<Object> NO_MATCH = (text, target) -> GroupOutcome.noMatch();

    @Test
    void testGroupHandler_RejectsBlankNameAndNullStep() {
        assertThatThrownBy(() -> new GroupHandler<>(null, NO_MATCH))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group handler name cannot be null or blank");
        assertThatThrownBy(() -> new GroupHandler<>(" ", NO_MATCH))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group handler name cannot be null or blank");
        assertThatThrownBy(() -> new GroupHandler<>("name", null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group handler step cannot be null");
    }

    @Test
    void testTextStep_UnchangedTextIsNoMatchChangedTextIsParsed() {
        GroupHandler<List<String>> handler = GroupHandler.textStep("text step", (text, target) -> {
            if (text.startsWith("AAA")) {
                target.add("AAA");
                return text.substring(3).trim();
            }
            return text;
        });
        List<String> target = new ArrayList<>();

        assertThat(handler.step().apply("AAA BBB", target)).isEqualTo(new GroupOutcome.Parsed("BBB"));
        assertThat(handler.step().apply("BBB", target)).isSameAs(GroupOutcome.noMatch());
        assertThat(handler.name()).isEqualTo("text step");
        assertThat(target).containsExactly("AAA");
    }

    @Test
    void testTextStep_NullResultIsABug() {
        GroupHandler<List<String>> handler = GroupHandler.textStep("null result", (text, target) -> null);
        GroupHandler.Step<List<String>> step = handler.step();
        List<String> target = new ArrayList<>();

        assertThatThrownBy(() -> step.apply("AAA", target))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Group handler step returned null");
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
        // null step on purpose: the call must throw
    void testTextStep_RejectsNullStep() {
        assertThatThrownBy(() -> GroupHandler.<List<String>>textStep("a", null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group handler step cannot be null");
    }

    @Test
    void testParsed_RejectsNullRemaining() {
        assertThatThrownBy(() -> new GroupOutcome.Parsed(null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Parsed remaining text cannot be null");
    }

    @Test
    void testRejected_RejectsNullFields() {
        InvalidObservationException reason = new InvalidObservationException("C.f", "x", "bad");

        assertThatThrownBy(() -> new GroupOutcome.Rejected(null, "a", "b", reason))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rejected outcome fields cannot be null");
        assertThatThrownBy(() -> new GroupOutcome.Rejected("x", null, "b", reason))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GroupOutcome.Rejected("x", "a", null, reason))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GroupOutcome.Rejected("x", "a", "b", null))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
        // null fields on purpose: each call must throw
    void testFailed_RejectsNullFields() {
        IllegalStateException cause = new IllegalStateException("bug");

        assertThatThrownBy(() -> new GroupOutcome.Failed(null, "a", "b", cause))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed outcome fields cannot be null");
        assertThatThrownBy(() -> new GroupOutcome.Failed("x", null, "b", cause))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GroupOutcome.Failed("x", "a", null, cause))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GroupOutcome.Failed("x", "a", "b", null))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testNoMatch_IsShared() {
        assertThat(GroupOutcome.noMatch()).isSameAs(GroupOutcome.NO_MATCH);
    }

    @Test
    void testSectionPolicy_DefaultsToNoBoundary() {
        SectionPolicy policy = SectionPolicy.of(PASSES, KEEP, STOP);

        assertThat(policy.boundary().test("FM120000")).isFalse();
        assertThat(policy.withBoundary(text -> text.startsWith("FM")).boundary().test("FM120000")).isTrue();
    }

    @Test
    void testSectionPolicy_RejectsNullSettings() {
        assertThatThrownBy(() -> new SectionPolicy(null, KEEP, STOP, text -> false))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Section policy settings cannot be null");
        assertThatThrownBy(() -> new SectionPolicy(PASSES, null, STOP, text -> false))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SectionPolicy(PASSES, KEEP, null, text -> false))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SectionPolicy(PASSES, KEEP, STOP, null))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSectionResult_CopiesCollectionsAndJoinsUnparsedText() {
        SectionResult result = new SectionResult("", new java.util.ArrayList<>(List.of("A", "B")),
                List.of(), Set.of());
        List<String> unparsedTokens = result.unparsedTokens();

        assertThat(result.unparsedText()).isEqualTo("A B");
        assertThatThrownBy(() -> unparsedTokens.add("C"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(new SectionResult("", List.of(), List.of(), Set.of()).unparsedText()).isNull();
    }
}
