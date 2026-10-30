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
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PatternGroupHandler} (#153, #154). The adapter must reproduce
 * the pattern registry's text handling so registry entries can move onto the
 * engine without changing parsed output.
 */
class PatternGroupHandlerTest {

    private static final Pattern GROUP = Pattern.compile("^a(?<n>\\d)\\s*");

    private static GroupOutcome apply(GroupHandler<List<String>> handler, String text, List<String> target) {
        return handler.step().apply(text, target);
    }

    private static void addDigit(List<String> target, java.util.regex.Matcher matcher) {
        target.add(matcher.group("n"));
    }

    @Test
    void testSingle_ParsesOneGroupAndKeepsTrailingSpace() {
        List<String> target = new ArrayList<>();
        GroupOutcome outcome = apply(PatternGroupHandler.single("a", GROUP, PatternGroupHandlerTest::addDigit),
                "a1 a2 b", target);

        assertThat(target).containsExactly("1");
        // Registry rule: trimmed rest plus one trailing space
        assertThat(outcome).isEqualTo(new GroupOutcome.Parsed("a2 b "));
    }

    @Test
    void testSingle_LastGroupLeavesEmptyText() {
        List<String> target = new ArrayList<>();
        GroupOutcome outcome = apply(PatternGroupHandler.single("a", GROUP, PatternGroupHandlerTest::addDigit),
                "a1", target);

        assertThat(outcome).isEqualTo(new GroupOutcome.Parsed(""));
    }

    @Test
    void testRepeating_ParsesEveryGroupInARow() {
        GroupHandler<List<String>> handler =
                PatternGroupHandler.repeating("digit group", GROUP, PatternGroupHandlerTest::addDigit);
        List<String> target = new ArrayList<>();

        GroupOutcome outcome = apply(handler, "a1 a2 a3 b", target);

        assertThat(handler.name()).isEqualTo("digit group");
        assertThat(target).containsExactly("1", "2", "3");
        assertThat(outcome).isEqualTo(new GroupOutcome.Parsed("b "));
    }

    @Test
    void testNoMatch_LeavesTargetUntouched() {
        List<String> target = new ArrayList<>();
        GroupOutcome outcome = apply(PatternGroupHandler.repeating("a", GROUP, PatternGroupHandlerTest::addDigit),
                "b a1", target);

        assertThat(target).isEmpty();
        assertThat(outcome).isSameAs(GroupOutcome.noMatch());
    }

    @Test
    void testEmptyMatch_IsNoMatch() {
        GroupHandler<List<String>> optional = PatternGroupHandler.single("optional",
                Pattern.compile("^(a1)?"), (target, matcher) -> target.add("called"));
        List<String> target = new ArrayList<>();

        assertThat(apply(optional, "b", target)).isSameAs(GroupOutcome.noMatch());
        assertThat(target).isEmpty();
    }

    @Test
    void testInvalidObservation_IsRejectedWithBothTextForms() {
        GroupHandler<List<String>> handler = PatternGroupHandler.single("a", GROUP, (target, matcher) -> {
            throw new InvalidObservationException("Test.n", matcher.group("n"), "Invalid digit");
        });

        GroupOutcome outcome = apply(handler, "a9 b", new ArrayList<>());

        assertThat(outcome).isInstanceOfSatisfying(GroupOutcome.Rejected.class, rejected -> {
            assertThat(rejected.matchedText()).isEqualTo("a9");
            assertThat(rejected.atGroup()).isEqualTo("a9 b");
            assertThat(rejected.afterGroup()).isEqualTo("b ");
            assertThat(rejected.reason()).isInstanceOf(InvalidObservationException.class);
        });
    }

    @Test
    void testNumberFormatException_IsRejected() {
        GroupHandler<List<String>> handler = PatternGroupHandler.single("a", GROUP, (target, matcher) -> {
            throw new NumberFormatException("bad number");
        });

        assertThat(apply(handler, "a9", new ArrayList<>()))
                .isInstanceOfSatisfying(GroupOutcome.Rejected.class,
                        rejected -> assertThat(rejected.reason()).isInstanceOf(NumberFormatException.class));
    }

    @Test
    void testRepeating_RejectionAfterParsedGroupsKeepsTheirProgress() {
        GroupHandler<List<String>> handler = PatternGroupHandler.repeating("a", GROUP, (target, matcher) -> {
            if ("9".equals(matcher.group("n"))) {
                throw new InvalidObservationException("Test.n", "9", "Invalid digit");
            }
            target.add(matcher.group("n"));
        });
        List<String> target = new ArrayList<>();

        GroupOutcome outcome = apply(handler, "a1 a2 a9 a3", target);

        assertThat(target).containsExactly("1", "2");
        assertThat(outcome).isInstanceOfSatisfying(GroupOutcome.Rejected.class, rejected -> {
            assertThat(rejected.atGroup()).isEqualTo("a9 a3 ");
            assertThat(rejected.afterGroup()).isEqualTo("a3 ");
        });
    }

    @Test
    void testOtherException_IsFailedNotRejected() {
        // A plain IllegalArgumentException is a programming error (#133), not bad data
        IllegalArgumentException bug = new IllegalArgumentException("bug");
        GroupHandler<List<String>> handler = PatternGroupHandler.single("a", GROUP, (target, matcher) -> {
            throw bug;
        });

        GroupOutcome outcome = apply(handler, "a1 b", new ArrayList<>());

        assertThat(outcome).isInstanceOfSatisfying(GroupOutcome.Failed.class, failed -> {
            assertThat(failed.matchedText()).isEqualTo("a1");
            assertThat(failed.atGroup()).isEqualTo("a1 b");
            assertThat(failed.afterGroup()).isEqualTo("b ");
            assertThat(failed.cause()).isSameAs(bug);
        });
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
        // null pattern / action on purpose: each call must throw
    void testCreate_RejectsNullPatternOrAction() {
        assertThatThrownBy(() -> PatternGroupHandler.<List<String>>single("a", null, (t, m) -> {
        }))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group pattern cannot be null");
        assertThatThrownBy(() -> PatternGroupHandler.<List<String>>repeating("a", GROUP, null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group action cannot be null");
    }

    @Test
    void testConsumeOnly_ConsumesWithoutStoring() {
        GroupHandler<List<String>> handler = PatternGroupHandler.consumeOnly("ignored", GROUP);
        List<String> target = new ArrayList<>();

        assertThat(apply(handler, "a1 b", target)).isEqualTo(new GroupOutcome.Parsed("b "));
        assertThat(apply(handler, "b", target)).isSameAs(GroupOutcome.noMatch());
        assertThat(handler.name()).isEqualTo("ignored");
        assertThat(target).isEmpty();
    }
}
