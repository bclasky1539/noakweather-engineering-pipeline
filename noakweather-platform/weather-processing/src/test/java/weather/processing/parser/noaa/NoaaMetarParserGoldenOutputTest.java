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
import weather.model.NoaaMetarData;
import weather.model.NoaaWeatherData;
import weather.processing.parser.common.ParseResult;

import java.io.IOException;

import static weather.processing.parser.noaa.GoldenOutput.expand;
import static weather.processing.parser.noaa.GoldenOutput.field;

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

    private static final String CORPUS_RESOURCE = "/golden/metar-golden-corpus.txt";
    private static final String EXPECTED_RESOURCE = "/golden/metar-golden-expected.txt";

    @Test
    void parsedOutputMatchesGoldenFile() throws IOException {
        GoldenOutput.verify(CORPUS_RESOURCE, EXPECTED_RESOURCE, NoaaMetarParserGoldenOutputTest::render);
    }

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
}
