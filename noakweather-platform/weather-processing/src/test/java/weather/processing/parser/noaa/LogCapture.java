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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test helper that captures one logger's events in memory instead of the console.
 * <p>
 * While open, the logger writes only to this capture (additivity off), at the
 * level given (WARN by default), so tests that trigger expected failures can
 * assert on the log without printing stack traces into the build output.
 * Close it to restore the normal configuration.
 */
final class LogCapture extends AbstractAppender implements AutoCloseable {

    private final List<LogEvent> events = new CopyOnWriteArrayList<>();
    private final String loggerName;
    private final Level level;
    private final LoggerContext context;

    private LogCapture(String loggerName, Level level) {
        super("LogCapture-" + loggerName, null, null, true, Property.EMPTY_ARRAY);
        this.loggerName = loggerName;
        this.level = level;
        this.context = (LoggerContext) LogManager.getContext(false);
    }

    /**
     * Start capturing the WARN and ERROR events of {@code type}'s logger.
     *
     * @param type the class whose logger to capture
     * @return an open capture; close it after the test
     */
    static LogCapture of(Class<?> type) {
        return of(type.getName(), Level.WARN);
    }

    /**
     * Start capturing a named logger's events at {@code level} and above.
     *
     * @param loggerName the logger (category) name to capture
     * @param level      the lowest level to capture
     * @return an open capture; close it after the test
     */
    static LogCapture of(String loggerName, Level level) {
        LogCapture capture = new LogCapture(loggerName, level);
        capture.attach();
        return capture;
    }

    private void attach() {
        start();
        Configuration config = context.getConfiguration();
        LoggerConfig loggerConfig = new LoggerConfig(loggerName, level, false);
        loggerConfig.addAppender(this, null, null);
        config.addLogger(loggerName, loggerConfig);
        context.updateLoggers();
    }

    @Override
    public void append(LogEvent event) {
        events.add(event.toImmutable());
    }

    /**
     * @return the captured events, oldest first
     */
    List<LogEvent> events() {
        return List.copyOf(events);
    }

    @Override
    public void close() {
        context.getConfiguration().removeLogger(loggerName);
        context.updateLoggers();
        stop();
    }
}
