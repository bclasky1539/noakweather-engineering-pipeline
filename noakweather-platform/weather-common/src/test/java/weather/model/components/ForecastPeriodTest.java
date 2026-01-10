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
package weather.model.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import weather.model.WeatherConditions;
import weather.model.enums.ChangeIndicator;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test suite for ForecastPeriod record.
 * Tests TAF forecast period handling with various change indicators.
 *
 * @author bclasky1539
 *
 */
class ForecastPeriodTest {

    // Test fixtures
    private static final Instant BASE_TIME = Instant.parse("2024-03-15T20:00:00Z");
    private static final WeatherConditions SAMPLE_CONDITIONS = WeatherConditions.builder()
            .wind(Wind.of(270, 10, "KT"))
            .visibility(Visibility.statuteMiles(10.0))
            .build();

    // ==================== Construction Tests ====================

    @Nested
    @DisplayName("BASE Forecast Construction")
    class BaseConstructionTests {

        @Test
        void testValidBaseForecast_WithPeriod() {
            Instant start = BASE_TIME;
            Instant end = start.plus(24, ChronoUnit.HOURS);

            ForecastPeriod period = new ForecastPeriod(
                    ChangeIndicator.BASE, null, start, end, null, SAMPLE_CONDITIONS
            );

            assertThat(period.changeIndicator()).isEqualTo(ChangeIndicator.BASE);
            assertThat(period.changeTime()).isNull();
            assertThat(period.periodStart()).isEqualTo(start);
            assertThat(period.periodEnd()).isEqualTo(end);
            assertThat(period.probability()).isNull();
            assertThat(period.conditions()).isEqualTo(SAMPLE_CONDITIONS);
        }

        @Test
        void testValidBaseForecast_WithoutPeriod() {
            ForecastPeriod period = new ForecastPeriod(
                    ChangeIndicator.BASE, null, null, null, null, SAMPLE_CONDITIONS
            );

            assertThat(period.changeIndicator()).isEqualTo(ChangeIndicator.BASE);
            assertThat(period.periodStart()).isNull();
            assertThat(period.periodEnd()).isNull();
        }

        @Test
        void testInvalidBaseForecast_StartAfterEnd() {
            Instant start = BASE_TIME;
            Instant end = start.minus(1, ChronoUnit.HOURS);

            assertThatThrownBy(() -> new ForecastPeriod(
                    ChangeIndicator.BASE, null, start, end, null, SAMPLE_CONDITIONS
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be before end");
        }
    }

    @Nested
    @DisplayName("FM Forecast Construction")
    class FmConstructionTests {

        @Test
        void testValidFmForecast() {
            Instant changeTime = BASE_TIME.plus(3, ChronoUnit.HOURS);

            ForecastPeriod period = new ForecastPeriod(
                    ChangeIndicator.FM, changeTime, null, null, null, SAMPLE_CONDITIONS
            );

            assertThat(period.changeIndicator()).isEqualTo(ChangeIndicator.FM);
            assertThat(period.changeTime()).isEqualTo(changeTime);
            assertThat(period.periodStart()).isNull();
            assertThat(period.periodEnd()).isNull();
            assertThat(period.probability()).isNull();
        }

        @Test
        void testInvalidFmForecast_NoChangeTime() {
            assertThatThrownBy(() -> new ForecastPeriod(
                    ChangeIndicator.FM, null, null, null, null, SAMPLE_CONDITIONS
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("FM forecast must have changeTime");
        }

        @Test
        void testInvalidFmForecast_HasPeriodStart() {
            Instant periodStart = BASE_TIME.plus(1, ChronoUnit.HOURS);

            assertThatThrownBy(() -> new ForecastPeriod(
                    ChangeIndicator.FM, BASE_TIME, periodStart, null, null, SAMPLE_CONDITIONS
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("should not have period start/end");
        }

        @Test
        void testInvalidFmForecast_HasPeriodEnd() {
            Instant periodEnd = BASE_TIME.plus(1, ChronoUnit.HOURS);

            assertThatThrownBy(() -> new ForecastPeriod(
                    ChangeIndicator.FM, BASE_TIME, null, periodEnd, null, SAMPLE_CONDITIONS
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("should not have period start/end");
        }
    }

    @Nested
    @DisplayName("TEMPO Forecast Construction")
    class TempoConstructionTests {

        @Test
        void testValidTempoForecast() {
            Instant start = BASE_TIME;
            Instant end = start.plus(4, ChronoUnit.HOURS);

            ForecastPeriod period = new ForecastPeriod(
                    ChangeIndicator.TEMPO, null, start, end, null, SAMPLE_CONDITIONS
            );

            assertThat(period.changeIndicator()).isEqualTo(ChangeIndicator.TEMPO);
            assertThat(period.changeTime()).isNull();
            assertThat(period.periodStart()).isEqualTo(start);
            assertThat(period.periodEnd()).isEqualTo(end);
            assertThat(period.probability()).isNull();
        }

        @Test
        void testInvalidTempoForecast_NoPeriodStart() {
            Instant end = BASE_TIME.plus(4, ChronoUnit.HOURS);

            assertThatThrownBy(() -> new ForecastPeriod(
                    ChangeIndicator.TEMPO, null, null, end, null, SAMPLE_CONDITIONS
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must have period start and end times");
        }

        @Test
        void testInvalidTempoForecast_NoPeriodEnd() {

            assertThatThrownBy(() -> new ForecastPeriod(
                    ChangeIndicator.TEMPO, null, BASE_TIME, null, null, SAMPLE_CONDITIONS
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must have period start and end times");
        }

        @Test
        void testInvalidTempoForecast_HasChangeTime() {
            Instant start = BASE_TIME;
            Instant end = start.plus(4, ChronoUnit.HOURS);
            Instant changeTime = start.plus(2, ChronoUnit.HOURS);

            assertThatThrownBy(() -> new ForecastPeriod(
                    ChangeIndicator.TEMPO, changeTime, start, end, null, SAMPLE_CONDITIONS
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("should not have exact change time");
        }

        @Test
        void testInvalidTempoForecast_TooLong() {
            Instant start = BASE_TIME;
            Instant end = start.plus(13, ChronoUnit.HOURS); // > 12 hours

            assertThatThrownBy(() -> new ForecastPeriod(
                    ChangeIndicator.TEMPO, null, start, end, null, SAMPLE_CONDITIONS
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("too long");
        }
    }

    @Nested
    @DisplayName("BECMG Forecast Construction")
    class BecmgConstructionTests {

        @Test
        void testValidBecmgForecast() {
            Instant start = BASE_TIME;
            Instant end = start.plus(2, ChronoUnit.HOURS);

            ForecastPeriod period = new ForecastPeriod(
                    ChangeIndicator.BECMG, null, start, end, null, SAMPLE_CONDITIONS
            );

            assertThat(period.changeIndicator()).isEqualTo(ChangeIndicator.BECMG);
            assertThat(period.periodStart()).isEqualTo(start);
            assertThat(period.periodEnd()).isEqualTo(end);
        }

        @Test
        void testInvalidBecmgForecast_NoPeriod() {
            assertThatThrownBy(() -> new ForecastPeriod(
                    ChangeIndicator.BECMG, null, null, null, null, SAMPLE_CONDITIONS
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must have period start and end times");
        }
    }

    @Nested
    @DisplayName("PROB Forecast Construction")
    class ProbConstructionTests {

        @Test
        void testValidProbForecast_30() {
            Instant start = BASE_TIME;
            Instant end = start.plus(6, ChronoUnit.HOURS);

            ForecastPeriod period = new ForecastPeriod(
                    ChangeIndicator.PROB, null, start, end, 30, SAMPLE_CONDITIONS
            );

            assertThat(period.changeIndicator()).isEqualTo(ChangeIndicator.PROB);
            assertThat(period.probability()).isEqualTo(30);
        }

        @Test
        void testValidProbForecast_40() {
            Instant start = BASE_TIME;
            Instant end = start.plus(6, ChronoUnit.HOURS);

            ForecastPeriod period = new ForecastPeriod(
                    ChangeIndicator.PROB, null, start, end, 40, SAMPLE_CONDITIONS
            );

            assertThat(period.probability()).isEqualTo(40);
        }

        @Test
        void testInvalidProbForecast_NoProbability() {
            Instant start = BASE_TIME;
            Instant end = start.plus(6, ChronoUnit.HOURS);

            assertThatThrownBy(() -> new ForecastPeriod(
                    ChangeIndicator.PROB, null, start, end, null, SAMPLE_CONDITIONS
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PROB forecast must have probability value");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 10, 20, 50, 60, 100})
        void testInvalidProbForecast_InvalidProbability(int invalidProb) {
            Instant start = BASE_TIME;
            Instant end = start.plus(6, ChronoUnit.HOURS);

            assertThatThrownBy(() -> new ForecastPeriod(
                    ChangeIndicator.PROB, null, start, end, invalidProb, SAMPLE_CONDITIONS
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid probability");
        }
    }

    @Nested
    @DisplayName("General Validation")
    class GeneralValidationTests {

        @Test
        void testInvalidConstruction_NullChangeIndicator() {
            assertThatThrownBy(() -> new ForecastPeriod(
                    null, null, null, null, null, SAMPLE_CONDITIONS
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Change indicator cannot be null");
        }

        @Test
        void testInvalidConstruction_NullConditions() {
            assertThatThrownBy(() -> new ForecastPeriod(
                    ChangeIndicator.BASE, null, null, null, null, null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Weather conditions cannot be null");
        }

        @Test
        void testInvalidConstruction_ProbabilityOnNonProb() {
            Instant start = BASE_TIME;
            Instant end = start.plus(4, ChronoUnit.HOURS);

            assertThatThrownBy(() -> new ForecastPeriod(
                    ChangeIndicator.TEMPO, null, start, end, 30, SAMPLE_CONDITIONS
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only PROB forecasts should have probability");
        }
    }

    // ==================== Query Method Tests ====================

    @Nested
    @DisplayName("Type Check Methods")
    class TypeCheckTests {

        @Test
        void testIsBaseForecast() {
            ForecastPeriod base = ForecastPeriod.base(BASE_TIME, BASE_TIME.plus(24, ChronoUnit.HOURS), SAMPLE_CONDITIONS);
            ForecastPeriod fm = ForecastPeriod.from(BASE_TIME, SAMPLE_CONDITIONS);

            assertThat(base.isBaseForecast()).isTrue();
            assertThat(fm.isBaseForecast()).isFalse();
        }

        @Test
        void testIsPermanentChange() {
            ForecastPeriod base = ForecastPeriod.base(BASE_TIME, BASE_TIME.plus(24, ChronoUnit.HOURS), SAMPLE_CONDITIONS);
            ForecastPeriod fm = ForecastPeriod.from(BASE_TIME, SAMPLE_CONDITIONS);
            ForecastPeriod tempo = ForecastPeriod.tempo(BASE_TIME, BASE_TIME.plus(4, ChronoUnit.HOURS), SAMPLE_CONDITIONS);

            assertThat(base.isPermanentChange()).isTrue();
            assertThat(fm.isPermanentChange()).isTrue();
            assertThat(tempo.isPermanentChange()).isFalse();
        }

        @Test
        void testIsTemporaryChange() {
            ForecastPeriod tempo = ForecastPeriod.tempo(BASE_TIME, BASE_TIME.plus(4, ChronoUnit.HOURS), SAMPLE_CONDITIONS);
            ForecastPeriod fm = ForecastPeriod.from(BASE_TIME, SAMPLE_CONDITIONS);

            assertThat(tempo.isTemporaryChange()).isTrue();
            assertThat(fm.isTemporaryChange()).isFalse();
        }

        @Test
        void testIsGradualChange() {
            ForecastPeriod becmg = ForecastPeriod.becoming(BASE_TIME, BASE_TIME.plus(2, ChronoUnit.HOURS), SAMPLE_CONDITIONS);
            ForecastPeriod fm = ForecastPeriod.from(BASE_TIME, SAMPLE_CONDITIONS);

            assertThat(becmg.isGradualChange()).isTrue();
            assertThat(fm.isGradualChange()).isFalse();
        }

        @Test
        void testIsProbabilistic() {
            ForecastPeriod prob = ForecastPeriod.probability(BASE_TIME, BASE_TIME.plus(6, ChronoUnit.HOURS), 30, SAMPLE_CONDITIONS);
            ForecastPeriod fm = ForecastPeriod.from(BASE_TIME, SAMPLE_CONDITIONS);

            assertThat(prob.isProbabilistic()).isTrue();
            assertThat(fm.isProbabilistic()).isFalse();
        }
    }

    @Nested
    @DisplayName("Time Range Methods")
    class TimeRangeTests {

        @Test
        void testHasTimeRange_Base() {
            ForecastPeriod period = ForecastPeriod.base(BASE_TIME, BASE_TIME.plus(24, ChronoUnit.HOURS), SAMPLE_CONDITIONS);
            assertThat(period.hasTimeRange()).isTrue();
        }

        @Test
        void testHasTimeRange_FM() {
            ForecastPeriod period = ForecastPeriod.from(BASE_TIME, SAMPLE_CONDITIONS);
            assertThat(period.hasTimeRange()).isTrue();
        }

        @Test
        void testHasTimeRange_BaseWithoutPeriod() {
            ForecastPeriod period = new ForecastPeriod(
                    ChangeIndicator.BASE, null, null, null, null, SAMPLE_CONDITIONS
            );
            assertThat(period.hasTimeRange()).isFalse();
        }

        @Test
        void testGetEffectiveStartTime_FM() {
            Instant changeTime = BASE_TIME.plus(3, ChronoUnit.HOURS);
            ForecastPeriod period = ForecastPeriod.from(changeTime, SAMPLE_CONDITIONS);

            assertThat(period.getEffectiveStartTime()).isEqualTo(changeTime);
        }

        @Test
        void testGetEffectiveStartTime_TEMPO() {
            Instant start = BASE_TIME;
            ForecastPeriod period = ForecastPeriod.tempo(start, start.plus(4, ChronoUnit.HOURS), SAMPLE_CONDITIONS);

            assertThat(period.getEffectiveStartTime()).isEqualTo(start);
        }

        @Test
        void testGetEffectiveEndTime_TEMPO() {
            Instant start = BASE_TIME;
            Instant end = start.plus(4, ChronoUnit.HOURS);
            ForecastPeriod period = ForecastPeriod.tempo(start, end, SAMPLE_CONDITIONS);

            assertThat(period.getEffectiveEndTime()).isEqualTo(end);
        }

        @Test
        void testGetEffectiveEndTime_FM() {
            ForecastPeriod period = ForecastPeriod.from(BASE_TIME, SAMPLE_CONDITIONS);

            assertThat(period.getEffectiveEndTime()).isNull();
        }

        @Test
        void testGetPeriodDuration() {
            Instant start = BASE_TIME;
            Instant end = start.plus(4, ChronoUnit.HOURS);
            ForecastPeriod period = ForecastPeriod.tempo(start, end, SAMPLE_CONDITIONS);

            assertThat(period.getPeriodDuration()).isEqualTo(Duration.ofHours(4));
        }

        @Test
        void testGetPeriodDuration_FM() {
            ForecastPeriod period = ForecastPeriod.from(BASE_TIME, SAMPLE_CONDITIONS);

            assertThat(period.getPeriodDuration()).isNull();
        }

        @Test
        void testGetPeriodDurationHours() {
            Instant start = BASE_TIME;
            Instant end = start.plus(6, ChronoUnit.HOURS);
            ForecastPeriod period = ForecastPeriod.tempo(start, end, SAMPLE_CONDITIONS);

            assertThat(period.getPeriodDurationHours()).isEqualTo(6L);
        }

        @Test
        void testGetPeriodDurationHours_FM() {
            ForecastPeriod period = ForecastPeriod.from(BASE_TIME, SAMPLE_CONDITIONS);

            assertThat(period.getPeriodDurationHours()).isNull();
        }
    }

    @Nested
    @DisplayName("Contains Tests")
    class ContainsTests {

        @Test
        void testContains_FM_AfterChangeTime() {
            Instant changeTime = BASE_TIME.plus(3, ChronoUnit.HOURS);
            ForecastPeriod period = ForecastPeriod.from(changeTime, SAMPLE_CONDITIONS);

            Instant testTime = changeTime.plus(1, ChronoUnit.HOURS);
            assertThat(period.contains(testTime)).isTrue();
        }

        @Test
        void testContains_FM_AtChangeTime() {
            Instant changeTime = BASE_TIME.plus(3, ChronoUnit.HOURS);
            ForecastPeriod period = ForecastPeriod.from(changeTime, SAMPLE_CONDITIONS);

            assertThat(period.contains(changeTime)).isTrue();
        }

        @Test
        void testContains_FM_BeforeChangeTime() {
            Instant changeTime = BASE_TIME.plus(3, ChronoUnit.HOURS);
            ForecastPeriod period = ForecastPeriod.from(changeTime, SAMPLE_CONDITIONS);

            Instant testTime = changeTime.minus(1, ChronoUnit.HOURS);
            assertThat(period.contains(testTime)).isFalse();
        }

        @Test
        void testContains_TEMPO_InMiddle() {
            Instant start = BASE_TIME;
            Instant end = start.plus(4, ChronoUnit.HOURS);
            ForecastPeriod period = ForecastPeriod.tempo(start, end, SAMPLE_CONDITIONS);

            Instant middle = start.plus(2, ChronoUnit.HOURS);
            assertThat(period.contains(middle)).isTrue();
        }

        @Test
        void testContains_TEMPO_AtStart() {
            Instant start = BASE_TIME;
            Instant end = start.plus(4, ChronoUnit.HOURS);
            ForecastPeriod period = ForecastPeriod.tempo(start, end, SAMPLE_CONDITIONS);

            assertThat(period.contains(start)).isTrue();
        }

        @Test
        void testContains_TEMPO_AtEnd() {
            Instant start = BASE_TIME;
            Instant end = start.plus(4, ChronoUnit.HOURS);
            ForecastPeriod period = ForecastPeriod.tempo(start, end, SAMPLE_CONDITIONS);

            assertThat(period.contains(end)).isFalse(); // Exclusive of end
        }

        @Test
        void testContains_TEMPO_Before() {
            Instant start = BASE_TIME;
            Instant end = start.plus(4, ChronoUnit.HOURS);
            ForecastPeriod period = ForecastPeriod.tempo(start, end, SAMPLE_CONDITIONS);

            Instant before = start.minus(1, ChronoUnit.HOURS);
            assertThat(period.contains(before)).isFalse();
        }

        @Test
        void testContains_TEMPO_After() {
            Instant start = BASE_TIME;
            Instant end = start.plus(4, ChronoUnit.HOURS);
            ForecastPeriod period = ForecastPeriod.tempo(start, end, SAMPLE_CONDITIONS);

            Instant after = end.plus(1, ChronoUnit.HOURS);
            assertThat(period.contains(after)).isFalse();
        }

        @Test
        void testContains_Null() {
            ForecastPeriod period = ForecastPeriod.tempo(BASE_TIME, BASE_TIME.plus(4, ChronoUnit.HOURS), SAMPLE_CONDITIONS);

            assertThat(period.contains(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Significant Weather Tests")
    class SignificantWeatherTests {

        @Test
        void testHasSignificantWeather_WithPrecipitation() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .presentWeather(java.util.List.of(PresentWeather.parse("RA")))
                    .build();

            ForecastPeriod period = ForecastPeriod.tempo(BASE_TIME, BASE_TIME.plus(4, ChronoUnit.HOURS), conditions);

            assertThat(period.hasSignificantWeather()).isTrue();
        }

        @Test
        void testHasSignificantWeather_WithThunderstorms() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .presentWeather(java.util.List.of(PresentWeather.parse("TSRA")))
                    .build();

            ForecastPeriod period = ForecastPeriod.tempo(BASE_TIME, BASE_TIME.plus(4, ChronoUnit.HOURS), conditions);

            assertThat(period.hasSignificantWeather()).isTrue();
        }

        @Test
        void testHasSignificantWeather_WithLowVisibility() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .visibility(Visibility.statuteMiles(2.0))
                    .build();

            ForecastPeriod period = ForecastPeriod.tempo(BASE_TIME, BASE_TIME.plus(4, ChronoUnit.HOURS), conditions);

            assertThat(period.hasSignificantWeather()).isTrue();
        }

        @Test
        void testHasSignificantWeather_NoSignificantWeather() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .visibility(Visibility.statuteMiles(10.0))
                    .build();

            ForecastPeriod period = ForecastPeriod.tempo(BASE_TIME, BASE_TIME.plus(4, ChronoUnit.HOURS), conditions);

            assertThat(period.hasSignificantWeather()).isFalse();
        }
    }

    // ==================== Format Tests ====================

    @Nested
    @DisplayName("Formatting Tests")
    class FormatTests {

        @Test
        void testToTafFormat_BASE() {
            Instant start = Instant.parse("2024-03-15T20:00:00Z");
            Instant end = Instant.parse("2024-03-16T20:00:00Z");
            ForecastPeriod period = ForecastPeriod.base(start, end, SAMPLE_CONDITIONS);

            assertThat(period.toTafFormat()).isEqualTo("BASE 1520/1620");
        }

        @Test
        void testToTafFormat_BASE_NoPeriod() {
            ForecastPeriod period = new ForecastPeriod(
                    ChangeIndicator.BASE, null, null, null, null, SAMPLE_CONDITIONS
            );

            assertThat(period.toTafFormat()).isEqualTo("BASE");
        }

        @Test
        void testToTafFormat_FM() {
            Instant changeTime = Instant.parse("2024-03-15T21:00:00Z");
            ForecastPeriod period = ForecastPeriod.from(changeTime, SAMPLE_CONDITIONS);

            assertThat(period.toTafFormat()).isEqualTo("FM152100");
        }

        @Test
        void testToTafFormat_TEMPO() {
            Instant start = Instant.parse("2024-03-30T03:00:00Z");
            Instant end = Instant.parse("2024-03-30T11:00:00Z");
            ForecastPeriod period = ForecastPeriod.tempo(start, end, SAMPLE_CONDITIONS);

            assertThat(period.toTafFormat()).isEqualTo("TEMPO 3003/3011");
        }

        @Test
        void testToTafFormat_BECMG() {
            Instant start = Instant.parse("2024-03-31T02:00:00Z");
            Instant end = Instant.parse("2024-03-31T04:00:00Z");
            ForecastPeriod period = ForecastPeriod.becoming(start, end, SAMPLE_CONDITIONS);

            assertThat(period.toTafFormat()).isEqualTo("BECMG 3102/3104");
        }

        @Test
        void testToTafFormat_PROB30() {
            Instant start = Instant.parse("2024-03-30T05:00:00Z");
            Instant end = Instant.parse("2024-03-30T11:00:00Z");
            ForecastPeriod period = ForecastPeriod.probability(start, end, 30, SAMPLE_CONDITIONS);

            assertThat(period.toTafFormat()).isEqualTo("PROB30 3005/3011");
        }

        @Test
        void testGetSummary() {
            ForecastPeriod period = ForecastPeriod.tempo(BASE_TIME, BASE_TIME.plus(4, ChronoUnit.HOURS), SAMPLE_CONDITIONS);

            String summary = period.getSummary();

            assertThat(summary)
                    .contains("TEMPO")
                    .contains(":");
        }

        @Test
        void testToString() {
            ForecastPeriod period = ForecastPeriod.tempo(BASE_TIME, BASE_TIME.plus(4, ChronoUnit.HOURS), SAMPLE_CONDITIONS);

            String str = period.toString();

            assertThat(str)
                    .contains("ForecastPeriod")
                    .contains("TEMPO");
        }
    }

    // ==================== Factory Method Tests ====================

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        void testBase() {
            Instant start = BASE_TIME;
            Instant end = start.plus(24, ChronoUnit.HOURS);

            ForecastPeriod period = ForecastPeriod.base(start, end, SAMPLE_CONDITIONS);

            assertThat(period.changeIndicator()).isEqualTo(ChangeIndicator.BASE);
            assertThat(period.periodStart()).isEqualTo(start);
            assertThat(period.periodEnd()).isEqualTo(end);
            assertThat(period.conditions()).isEqualTo(SAMPLE_CONDITIONS);
        }

        @Test
        void testFrom() {
            Instant changeTime = BASE_TIME.plus(3, ChronoUnit.HOURS);

            ForecastPeriod period = ForecastPeriod.from(changeTime, SAMPLE_CONDITIONS);

            assertThat(period.changeIndicator()).isEqualTo(ChangeIndicator.FM);
            assertThat(period.changeTime()).isEqualTo(changeTime);
            assertThat(period.conditions()).isEqualTo(SAMPLE_CONDITIONS);
        }

        @Test
        void testTempo() {
            Instant start = BASE_TIME;
            Instant end = start.plus(4, ChronoUnit.HOURS);

            ForecastPeriod period = ForecastPeriod.tempo(start, end, SAMPLE_CONDITIONS);

            assertThat(period.changeIndicator()).isEqualTo(ChangeIndicator.TEMPO);
            assertThat(period.periodStart()).isEqualTo(start);
            assertThat(period.periodEnd()).isEqualTo(end);
        }

        @Test
        void testBecoming() {
            Instant start = BASE_TIME;
            Instant end = start.plus(2, ChronoUnit.HOURS);

            ForecastPeriod period = ForecastPeriod.becoming(start, end, SAMPLE_CONDITIONS);

            assertThat(period.changeIndicator()).isEqualTo(ChangeIndicator.BECMG);
            assertThat(period.periodStart()).isEqualTo(start);
            assertThat(period.periodEnd()).isEqualTo(end);
        }

        @Test
        void testProbability() {
            Instant start = BASE_TIME;
            Instant end = start.plus(6, ChronoUnit.HOURS);

            ForecastPeriod period = ForecastPeriod.probability(start, end, 30, SAMPLE_CONDITIONS);

            assertThat(period.changeIndicator()).isEqualTo(ChangeIndicator.PROB);
            assertThat(period.probability()).isEqualTo(30);
        }

        @Test
        void testOf() {
            Instant start = BASE_TIME;
            Instant end = start.plus(4, ChronoUnit.HOURS);

            ForecastPeriod period = ForecastPeriod.of(
                    ChangeIndicator.TEMPO, null, start, end, null, SAMPLE_CONDITIONS
            );

            assertThat(period.changeIndicator()).isEqualTo(ChangeIndicator.TEMPO);
            assertThat(period.periodStart()).isEqualTo(start);
            assertThat(period.periodEnd()).isEqualTo(end);
        }
    }

    // ==================== Equality and HashCode Tests ====================

    @Test
    void testEquality() {
        Instant start = BASE_TIME;
        Instant end = start.plus(4, ChronoUnit.HOURS);

        ForecastPeriod period1 = ForecastPeriod.tempo(start, end, SAMPLE_CONDITIONS);
        ForecastPeriod period2 = ForecastPeriod.tempo(start, end, SAMPLE_CONDITIONS);
        ForecastPeriod period3 = ForecastPeriod.tempo(start, end.plus(1, ChronoUnit.HOURS), SAMPLE_CONDITIONS);

        assertThat(period1)
                .isEqualTo(period2)
                .isNotEqualTo(period3);
    }

    @Test
    void testHashCode() {
        Instant start = BASE_TIME;
        Instant end = start.plus(4, ChronoUnit.HOURS);

        ForecastPeriod period1 = ForecastPeriod.tempo(start, end, SAMPLE_CONDITIONS);
        ForecastPeriod period2 = ForecastPeriod.tempo(start, end, SAMPLE_CONDITIONS);

        assertThat(period1).hasSameHashCodeAs(period2);
    }
}
