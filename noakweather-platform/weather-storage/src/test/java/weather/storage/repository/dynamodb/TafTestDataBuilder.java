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
package weather.storage.repository.dynamodb;

import weather.model.NoaaTafData;
import weather.model.WeatherDataSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Builder for creating test TAF data.
 * <p>
 * Simplified compared to METAR builder since TAF structure is more complex
 * with forecast periods. For integration tests, we often just need basic TAF records.
 * <p>
 * Usage:
 * <pre>
 * NoaaTafData taf = TafTestDataBuilder.create("KJFK").build();
 *
 * NoaaTafData taf = TafTestDataBuilder.create("KLGA")
 *     .withIssueTime(Instant.now())
 *     .withValidPeriod(24)
 *     .build();
 * </pre>
 *
 * @author bclasky1539
 *
 */
public class TafTestDataBuilder {

    private final String stationId;
    private Instant observationTime;
    private Instant issueTime;
    private String rawText;
    private int validHours;

    private TafTestDataBuilder(String stationId) {
        this.stationId = stationId;
        this.observationTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        this.issueTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        this.validHours = 24;  // Standard 24-hour TAF
    }

    /**
     * Create a builder with default values.
     *
     * @param stationId ICAO station identifier
     * @return builder instance
     */
    public static TafTestDataBuilder create(String stationId) {
        return new TafTestDataBuilder(stationId);
    }

    /**
     * Create a simple TAF with just station and times.
     *
     * @param stationId ICAO station identifier
     * @return TAF data
     */
    public static NoaaTafData createSimple(String stationId) {
        return new TafTestDataBuilder(stationId).build();
    }

    public TafTestDataBuilder withObservationTime(Instant time) {
        this.observationTime = time;
        return this;
    }

    public TafTestDataBuilder withIssueTime(Instant time) {
        this.issueTime = time;
        return this;
    }

    public TafTestDataBuilder withValidPeriod(int hours) {
        this.validHours = hours;
        return this;
    }

    public TafTestDataBuilder withRawText(String rawText) {
        this.rawText = rawText;
        return this;
    }

    /**
     * Build the NoaaTafData instance.
     *
     * @return TAF data
     */
    public NoaaTafData build() {
        NoaaTafData taf = new NoaaTafData(
                stationId,
                observationTime
        );

        taf.setIssueTime(issueTime);

        // Set raw text (auto-generate if not provided)
        taf.setRawText(Objects.requireNonNullElseGet(rawText, this::generateRawText));

        taf.setSource(WeatherDataSource.NOAA);

        return taf;
    }

    /**
     * Generate a simple TAF raw text.
     */
    private String generateRawText() {
        return String.format("TAF %s %dZ VRB05KT P6SM FEW250",
                stationId,
                validHours);
    }
}
