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
import org.apache.logging.log4j.core.LogEvent;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import weather.model.components.remark.NoaaMetarRemarks;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link RemarkHandlerChain}: multi-pass progress and the per-handler
 * circuit breaker (#135).
 * <p>
 * Breaker trips are logged at WARN with a stack trace. The chain's logger is
 * captured for each test so those expected traces are asserted, not printed.
 */
class RemarkHandlerChainTest {

    private final NoaaMetarRemarks.Builder builder = NoaaMetarRemarks.builder();
    private LogCapture logs;

    @BeforeEach
    void captureLogs() {
        logs = LogCapture.of(RemarkHandlerChain.class);
    }

    @AfterEach
    void releaseLogs() {
        logs.close();
    }

    /** A handler that consumes {@code token} when the text starts with it. */
    private static RemarkHandler consuming(String name, String token) {
        return new RemarkHandler(name, (remaining, b) ->
                remaining.startsWith(token) ? remaining.substring(token.length()).trim() : remaining);
    }

    /** A handler that counts its calls and always throws {@code failure}. */
    private static RemarkHandler failing(String name, RuntimeException failure, AtomicInteger calls) {
        return new RemarkHandler(name, (remaining, b) -> {
            calls.incrementAndGet();
            throw failure;
        });
    }

    @Test
    void testRunsPassesUntilNoProgress() {
        // "b" can only match after "a" has consumed AAA, so it needs a second pass
        RemarkHandlerChain chain = new RemarkHandlerChain(List.of(
                consuming("b", "BBB"),
                consuming("a", "AAA")));

        assertThat(chain.runPasses("AAA BBB CCC", builder)).isEqualTo("CCC");
        assertThat(logs.events()).isEmpty();
    }

    @Test
    void testFailingHandlerTripsAndOtherHandlersContinue() {
        AtomicInteger calls = new AtomicInteger();
        NullPointerException failure = new NullPointerException("boom");
        RemarkHandlerChain chain = new RemarkHandlerChain(List.of(
                failing("bad", failure, calls),
                consuming("a", "AAA")));

        assertThat(chain.runPasses("AAA CCC", builder)).isEqualTo("CCC");
        assertThat(chain.isTripped("bad")).isTrue();
        assertThat(chain.isTripped("a")).isFalse();

        assertThat(logs.events()).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getMessage().getFormattedMessage()).isEqualTo(
                    "Remark handler 'bad' failed on 'AAA CCC' and is disabled for the rest of this report");
            assertThat(event.getThrown()).isSameAs(failure);
        });
    }

    @Test
    void testFailingHandlerLeavesTextUnchanged() {
        AtomicInteger calls = new AtomicInteger();
        RemarkHandlerChain chain = new RemarkHandlerChain(List.of(
                failing("bad", new IllegalStateException("boom"), calls)));

        assertThat(chain.runPasses("AAA CCC", builder)).isEqualTo("AAA CCC");
    }

    @Test
    void testTrippedHandlerIsNotCalledAgainInSameReport() {
        // Three passes run (AAA consumed, then BBB, then no progress); "bad" runs only in the first
        AtomicInteger calls = new AtomicInteger();
        RemarkHandlerChain chain = new RemarkHandlerChain(List.of(
                failing("bad", new IndexOutOfBoundsException("boom"), calls),
                consuming("b", "BBB"),
                consuming("a", "AAA")));

        chain.runPasses("AAA BBB CCC", builder);

        assertThat(calls).hasValue(1);
        assertThat(logs.events()).hasSize(1);
    }

    @Test
    void testPlainIllegalArgumentExceptionTripsBreaker() {
        // A plain IAE is a programming error, not rejected data, so it trips the breaker
        AtomicInteger calls = new AtomicInteger();
        RemarkHandlerChain chain = new RemarkHandlerChain(List.of(
                failing("bad", new IllegalArgumentException("bug"), calls)));

        chain.runPasses("AAA", builder);

        assertThat(chain.isTripped("bad")).isTrue();
    }

    @Test
    void testNewChainStartsWithBreakersClosed() {
        AtomicInteger calls = new AtomicInteger();
        List<RemarkHandler> handlers = List.of(failing("bad", new IllegalStateException("boom"), calls));

        RemarkHandlerChain firstReport = new RemarkHandlerChain(handlers);
        firstReport.runPasses("AAA", builder);
        RemarkHandlerChain secondReport = new RemarkHandlerChain(handlers);

        assertThat(secondReport.isTripped("bad")).isFalse();
        secondReport.runPasses("AAA", builder);
        assertThat(calls).hasValue(2);
    }

    @Test
    void testNullResultTripsBreaker() {
        RemarkHandlerChain chain = new RemarkHandlerChain(List.of(
                new RemarkHandler("null", (remaining, b) -> null),
                consuming("a", "AAA")));

        assertThat(chain.runPasses("AAA CCC", builder)).isEqualTo("CCC");
        assertThat(chain.isTripped("null")).isTrue();
        assertThat(logs.events()).extracting(LogEvent::getThrown)
                .singleElement(as(InstanceOfAssertFactories.THROWABLE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Remark handler returned null");
    }

    @Test
    void testEmptyHandlerListReturnsTextUnchanged() {
        RemarkHandlerChain chain = new RemarkHandlerChain(List.of());

        assertThat(chain.runPasses("AAA", builder)).isEqualTo("AAA");
    }

    @Test
    void testNullHandlerListRejected() {
        assertThatThrownBy(() -> new RemarkHandlerChain(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Remark handlers cannot be null");
    }

    @Test
    void testDuplicateHandlerNamesRejected() {
        List<RemarkHandler> handlers = List.of(consuming("a", "AAA"), consuming("a", "BBB"));

        assertThatThrownBy(() -> new RemarkHandlerChain(handlers))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate remark handler name: a");
    }

    @Test
    void testRemarkHandlerRejectsBlankName() {
        assertThatThrownBy(() -> new RemarkHandler(" ", (remaining, b) -> remaining))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Remark handler name cannot be null or blank");
    }

    @Test
    void testRemarkHandlerRejectsNullStep() {
        assertThatThrownBy(() -> new RemarkHandler("a", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Remark handler step cannot be null");
    }
}
