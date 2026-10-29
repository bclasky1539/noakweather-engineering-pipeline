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

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import weather.exception.InvalidObservationException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static weather.processing.parser.noaa.SectionPolicy.RejectedText.CONSUME;
import static weather.processing.parser.noaa.SectionPolicy.RejectedText.KEEP;
import static weather.processing.parser.noaa.SectionPolicy.Scheduling.FIRST_MATCH_RESTART;
import static weather.processing.parser.noaa.SectionPolicy.Scheduling.PASSES;
import static weather.processing.parser.noaa.SectionPolicy.UnrecognizedToken.SKIP_TO_UNPARSED;
import static weather.processing.parser.noaa.SectionPolicy.UnrecognizedToken.STOP;

/**
 * Tests for {@link SectionParser}, the shared group parsing engine (#153, #154).
 * <p>
 * Handlers record "name:group" into a list target, so each test can see which
 * handler parsed what, in which order.
 */
class SectionParserTest {

    private static final String SECTION = "test section";

    private static GroupHandler<List<String>> recording(String name, String regex) {
        return PatternGroupHandler.single(name, Pattern.compile(regex),
                (target, matcher) -> target.add(name + ":" + matcher.group(0).trim()));
    }

    private static GroupHandler<List<String>> rejecting(String name) {
        return PatternGroupHandler.single(name, Pattern.compile("^x9\\s*"), (target, matcher) -> {
            throw new InvalidObservationException("Test.field", matcher.group(0).trim(), "Invalid test group");
        });
    }

    private static SectionResult run(List<GroupHandler<List<String>>> handlers, SectionPolicy policy,
                                     String text, List<String> target) {
        return new SectionParser<>(SECTION, handlers, policy).parse(text, target);
    }

    // ==================== Scheduling ====================

    @Test
    void testFirstMatchRestart_RestartsFromFirstHandlerAfterProgress() {
        // After B takes "b", A (first in the list) gets "a" before C
        List<String> target = new ArrayList<>();
        SectionResult result = run(List.of(recording("A", "^a\\s*"), recording("B", "^b\\s*"), recording("C", "^a\\s*")),
                SectionPolicy.of(FIRST_MATCH_RESTART, CONSUME, STOP), "b a", target);

        assertThat(target).containsExactly("B:b", "A:a");
        assertThat(result.remaining()).isEmpty();
    }

    @Test
    void testPasses_RunsEveryHandlerOncePerPass() {
        // After B takes "b", the pass continues with C, which gets "a" before A
        List<String> target = new ArrayList<>();
        SectionResult result = run(List.of(recording("A", "^a\\s*"), recording("B", "^b\\s*"), recording("C", "^a\\s*")),
                SectionPolicy.of(PASSES, CONSUME, STOP), "b a", target);

        assertThat(target).containsExactly("B:b", "C:a");
        assertThat(result.remaining()).isEmpty();
    }

    @Test
    void testPasses_LaterPassPicksUpGroupExposedByAnotherHandler() {
        // An only matches once B has consumed the token in front of it
        List<String> target = new ArrayList<>();
        run(List.of(recording("A", "^a\\s*"), recording("B", "^b\\s*")),
                SectionPolicy.of(PASSES, KEEP, STOP), "b a", target);

        assertThat(target).containsExactly("B:b", "A:a");
    }

    // ==================== Rejected text ====================

    @Test
    void testRejected_Consume_DropsGroupAndContinues() {
        List<String> target = new ArrayList<>();
        SectionResult result = run(List.of(rejecting("R"), recording("B", "^b\\s*")),
                SectionPolicy.of(FIRST_MATCH_RESTART, CONSUME, STOP), "x9 b", target);

        assertThat(target).containsExactly("B:b");
        assertThat(result.remaining()).isEmpty();
        assertThat(result.rejections()).singleElement().satisfies(rejection -> {
            assertThat(rejection.handlerName()).isEqualTo("R");
            assertThat(rejection.matchedText()).isEqualTo("x9");
            assertThat(rejection.reason()).isInstanceOf(InvalidObservationException.class);
        });
    }

    @Test
    void testRejected_KeepAndStop_LeavesGroupInRemaining() {
        List<String> target = new ArrayList<>();
        SectionResult result = run(List.of(rejecting("R"), recording("B", "^b\\s*")),
                SectionPolicy.of(FIRST_MATCH_RESTART, KEEP, STOP), "x9 b", target);

        assertThat(target).isEmpty();
        assertThat(result.remaining()).isEqualTo("x9 b");
        assertThat(result.rejections()).hasSize(1);
    }

    @Test
    void testRejected_KeepAndSkip_MovesGroupToUnparsedAndContinues() {
        List<String> target = new ArrayList<>();
        SectionResult result = run(List.of(rejecting("R"), recording("B", "^b\\s*")),
                SectionPolicy.of(PASSES, KEEP, SKIP_TO_UNPARSED), "x9 b", target);

        assertThat(target).containsExactly("B:b");
        assertThat(result.unparsedTokens()).containsExactly("x9");
        assertThat(result.rejections()).hasSize(1);
    }

    @Test
    void testRejected_Keep_RecordedOnceWhileGroupStaysAtFront() {
        // Q is not anchored, so it makes progress behind the kept x9 and forces a
        // second pass in which R rejects x9 again
        List<String> target = new ArrayList<>();
        SectionResult result = run(List.of(rejecting("R"), recording("Q", "q\\s*")),
                SectionPolicy.of(PASSES, KEEP, STOP), "x9 q", target);

        assertThat(target).containsExactly("Q:q");
        assertThat(result.rejections()).hasSize(1);
        assertThat(result.remaining()).isEqualTo("x9 ");
    }

    @Test
    void testRejected_Keep_SameTextAgainAfterSkipIsRecordedAgain() {
        List<String> target = new ArrayList<>();
        SectionResult result = run(List.of(rejecting("R"), recording("B", "^b\\s*")),
                SectionPolicy.of(PASSES, KEEP, SKIP_TO_UNPARSED), "x9 b x9", target);

        assertThat(result.unparsedTokens()).containsExactly("x9", "x9");
        assertThat(result.rejections()).hasSize(2);
    }

    @Test
    void testRejected_Consume_EachOccurrenceRecorded() {
        List<String> target = new ArrayList<>();
        SectionResult result = run(List.of(rejecting("R"), recording("B", "^b\\s*")),
                SectionPolicy.of(FIRST_MATCH_RESTART, CONSUME, STOP), "x9 b x9", target);

        assertThat(target).containsExactly("B:b");
        assertThat(result.rejections()).hasSize(2);
    }

    @Test
    void testRejected_LoggedAtDebugOnRejectedCategory() {
        try (LogCapture rejected = LogCapture.of(RejectedGroupLog.CATEGORY, Level.DEBUG)) {
            run(List.of(rejecting("test group")),
                    SectionPolicy.of(FIRST_MATCH_RESTART, CONSUME, STOP), "x9", new ArrayList<>());

            assertThat(rejected.events()).singleElement().satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
                assertThat(event.getMessage().getFormattedMessage())
                        .isEqualTo("Rejected test group 'x9' [Test.field]: Invalid test group: x9");
            });
        }
    }

    // ==================== Unrecognized tokens ====================

    @Test
    void testUnrecognized_Stop_ReturnsRestAsRemaining() {
        List<String> target = new ArrayList<>();
        SectionResult result = run(List.of(recording("B", "^b\\s*")),
                SectionPolicy.of(FIRST_MATCH_RESTART, CONSUME, STOP), "zz b", target);

        assertThat(target).isEmpty();
        assertThat(result.remaining()).isEqualTo("zz b");
        assertThat(result.unparsedTokens()).isEmpty();
        assertThat(result.unparsedText()).isNull();
    }

    @Test
    void testUnrecognized_Skip_CollectsTokensAndParsesWhatFollows() {
        List<String> target = new ArrayList<>();
        SectionResult result = run(List.of(recording("B", "^b\\s*")),
                SectionPolicy.of(PASSES, KEEP, SKIP_TO_UNPARSED), "zz b yy", target);

        assertThat(target).containsExactly("B:b");
        assertThat(result.remaining()).isEmpty();
        assertThat(result.unparsedTokens()).containsExactly("zz", "yy");
        assertThat(result.unparsedText()).isEqualTo("zz yy");
    }

    @ParameterizedTest
    @EnumSource(SectionPolicy.Scheduling.class)
    void testEmptyAndBlankText_ParseNothing(SectionPolicy.Scheduling scheduling) {
        List<String> target = new ArrayList<>();
        SectionParser<List<String>> parser = new SectionParser<>(SECTION, List.of(recording("A", "^a\\s*")),
                SectionPolicy.of(scheduling, KEEP, SKIP_TO_UNPARSED));

        assertThat(parser.parse("", target).remaining()).isEmpty();
        assertThat(parser.parse("   ", target).remaining()).isEqualTo("   ");
        assertThat(target).isEmpty();
    }

    // ==================== Boundary ====================

    @ParameterizedTest
    @EnumSource(SectionPolicy.Scheduling.class)
    void testBoundary_StopsBeforeNextSection(SectionPolicy.Scheduling scheduling) {
        List<String> target = new ArrayList<>();
        SectionPolicy policy = SectionPolicy.of(scheduling, CONSUME, SKIP_TO_UNPARSED)
                .withBoundary(text -> text.startsWith("FM"));

        SectionResult result = run(List.of(recording("A", "^a\\s*"), recording("B", "^b\\s*")),
                policy, "a FM12 b", target);

        assertThat(target).containsExactly("A:a");
        assertThat(result.remaining()).isEqualTo("FM12 b ");
        assertThat(result.unparsedTokens()).isEmpty();
    }

    // ==================== Circuit breaker ====================

    @Test
    void testBreaker_TripsFailingHandlerAndKeepsOthersRunning() {
        GroupHandler<List<String>> failing = new GroupHandler<>("failing", (text, target) -> {
            throw new IllegalStateException("boom");
        });
        List<String> target = new ArrayList<>();

        try (LogCapture warnings = LogCapture.of(SectionParser.class)) {
            SectionResult result = run(List.of(failing, recording("B", "^b\\s*")),
                    SectionPolicy.of(FIRST_MATCH_RESTART, CONSUME, STOP), "b b", target);

            assertThat(target).containsExactly("B:b", "B:b");
            assertThat(result.trippedHandlers()).containsExactly("failing");
            assertThat(warnings.events()).singleElement().satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getMessage().getFormattedMessage())
                        .isEqualTo("Group handler 'failing' in test section failed on 'b b' "
                                + "and is disabled for the rest of this section");
                assertThat(event.getThrown()).isInstanceOf(IllegalStateException.class).hasMessage("boom");
            });
        }
    }

    @Test
    void testBreaker_NullOutcomeTripsHandler() {
        GroupHandler<List<String>> nullOutcome = new GroupHandler<>("null outcome", (text, target) -> null);

        try (LogCapture warnings = LogCapture.of(SectionParser.class)) {
            SectionResult result = run(List.of(nullOutcome, recording("B", "^b\\s*")),
                    SectionPolicy.of(PASSES, KEEP, STOP), "b", new ArrayList<>());

            assertThat(result.trippedHandlers()).containsExactly("null outcome");
            assertThat(warnings.events()).singleElement()
                    .satisfies(event -> assertThat(event.getThrown()).hasMessage("Group handler returned null"));
        }
    }

    @Test
    void testBreaker_ResetsOnEveryParseCall() {
        int[] calls = {0};
        GroupHandler<List<String>> failsOnce = new GroupHandler<>("fails once", (text, target) -> {
            if (calls[0]++ == 0) {
                throw new IllegalStateException("first call fails");
            }
            return GroupOutcome.noMatch();
        });
        SectionParser<List<String>> parser = new SectionParser<>(SECTION, List.of(failsOnce),
                SectionPolicy.of(FIRST_MATCH_RESTART, CONSUME, STOP));

        try (LogCapture warnings = LogCapture.of(SectionParser.class)) {
            assertThat(parser.parse("a", new ArrayList<>()).trippedHandlers()).containsExactly("fails once");
            assertThat(parser.parse("a", new ArrayList<>()).trippedHandlers()).isEmpty();
            assertThat(warnings.events()).hasSize(1);
        }
    }

    // ==================== Construction ====================

    @Test
    @SuppressWarnings("DataFlowIssue")
        // invalid arguments on purpose: each call must throw
    void testConstructor_RejectsInvalidArguments() {
        List<GroupHandler<List<String>>> handlers = List.of(recording("A", "^a"));
        SectionPolicy policy = SectionPolicy.of(PASSES, KEEP, STOP);
        List<GroupHandler<List<String>>> withNull = new ArrayList<>();
        withNull.add(null);
        List<GroupHandler<List<String>>> duplicates = List.of(recording("A", "^a"), recording("A", "^b"));

        assertThatThrownBy(() -> new SectionParser<>(null, handlers, policy))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Section name cannot be null or blank");
        assertThatThrownBy(() -> new SectionParser<>(" ", handlers, policy))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Section name cannot be null or blank");
        assertThatThrownBy(() -> new SectionParser<List<String>>(SECTION, null, policy))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Section handlers cannot be null");
        assertThatThrownBy(() -> new SectionParser<>(SECTION, handlers, null))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Section policy cannot be null");
        assertThatThrownBy(() -> new SectionParser<>(SECTION, withNull, policy))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Section handlers cannot contain null");
        assertThatThrownBy(() -> new SectionParser<>(SECTION, duplicates, policy))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Duplicate group handler name: A");
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
        // null text on purpose: parse must throw
    void testParse_RejectsNullText() {
        SectionParser<List<String>> parser = new SectionParser<>(SECTION, List.of(),
                SectionPolicy.of(PASSES, KEEP, STOP));
        List<String> target = new ArrayList<>();

        assertThatThrownBy(() -> parser.parse(null, target))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Section text cannot be null");
    }
}
