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
import weather.model.NoaaTafData;
import weather.model.NoaaWeatherData;
import weather.model.components.ForecastPeriod;
import weather.processing.parser.common.ParseResult;

import java.io.IOException;
import java.util.List;

import static weather.processing.parser.noaa.GoldenOutput.expand;
import static weather.processing.parser.noaa.GoldenOutput.field;

/**
 * Golden-output regression test for {@link NoaaTafParser} (#153 phase 4).
 * <p>
 * Parses every report in {@code golden/taf-golden-corpus.txt} and compares a
 * deterministic text rendering of the result with {@code golden/taf-golden-expected.txt}.
 * The expected file was recorded before TAF moved onto the group parsing engine,
 * so the move must pass without re-recording.
 * <p>
 * Every corpus report starts with a {@code YYYY/MM/DD HH:MM} date header, so issue,
 * validity, change-group and temperature-forecast times resolve the same way on any
 * day and can be rendered in full.
 * <p>
 * To re-record after an intended output change, run from the repository root:
 * <pre>
 * mvn test -pl noakweather-platform/weather-processing -am -Dtest=NoaaTafParserGoldenOutputTest \
 *     -Dsurefire.failIfNoSpecifiedTests=false -Dgolden.record=true
 * </pre>
 * then review the diff of the expected file before committing it.
 */
class NoaaTafParserGoldenOutputTest {

    private static final String CORPUS_RESOURCE = "/golden/taf-golden-corpus.txt";
    private static final String EXPECTED_RESOURCE = "/golden/taf-golden-expected.txt";

    @Test
    void parsedOutputMatchesGoldenFile() throws IOException {
        GoldenOutput.verify(CORPUS_RESOURCE, EXPECTED_RESOURCE, NoaaTafParserGoldenOutputTest::render);
    }

    /**
     * Render one parse result as stable, line-per-field text, one block per forecast period.
     */
    static String render(String raw) {
        ParseResult<NoaaWeatherData> result = new NoaaTafParser().parse(raw);
        if (result.isFailure()) {
            return "  FAILURE: " + result.getErrorMessage() + "\n";
        }

        NoaaTafData data = (NoaaTafData) result.getData().orElseThrow();
        StringBuilder sb = new StringBuilder();

        field(sb, "stationId", data.getStationId());
        field(sb, "reportType", data.getReportType());
        field(sb, "reportModifier", data.getReportModifier());
        field(sb, "issueTime", data.getIssueTime());
        field(sb, "validityPeriod", data.getValidityPeriod());
        field(sb, "maxTemperature", data.getMaxTemperature());
        field(sb, "maxTemperatureTime", data.getMaxTemperatureTime());
        field(sb, "minTemperature", data.getMinTemperature());
        field(sb, "minTemperatureTime", data.getMinTemperatureTime());
        // Not routed for TAF today; listed so a handler that starts storing them shows up
        field(sb, "recentWeather", data.getRecentWeather());
        field(sb, "recentWeatherNotObservable", data.isRecentWeatherNotObservable());

        List<ForecastPeriod> periods = data.getForecastPeriods();
        field(sb, "forecastPeriods", periods.size());
        for (int i = 0; i < periods.size(); i++) {
            ForecastPeriod period = periods.get(i);
            String prefix = "period[" + i + "]";
            field(sb, prefix + ".changeIndicator", period.changeIndicator());
            field(sb, prefix + ".changeTime", period.changeTime());
            field(sb, prefix + ".periodStart", period.periodStart());
            field(sb, prefix + ".periodEnd", period.periodEnd());
            field(sb, prefix + ".probability", period.probability());
            expand(sb, prefix + ".conditions", period.conditions());
        }
        return sb.toString();
    }
}
