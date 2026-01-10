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
package weather.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import weather.model.components.*;
import weather.model.enums.SkyCoverage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for WeatherConditions record.
 * Tests universal weather conditions structure used across all report types.
 *
 * @author bclasky1539
 *
 */
class WeatherConditionsTest {

    // ==================== Construction Tests ====================

    @Nested
    @DisplayName("Construction and Immutability")
    class ConstructionTests {

        @Test
        void testConstruction_AllFields() {
            Wind wind = Wind.of(270, 10, "KT");
            Visibility visibility = Visibility.statuteMiles(10.0);
            Temperature temp = Temperature.ofCurrent(22.0, 12.0);
            Pressure pressure = Pressure.inchesHg(29.92);
            List<PresentWeather> weather = List.of(
                    PresentWeather.parse("RA")
            );
            List<SkyCondition> sky = List.of(
                    SkyCondition.of(SkyCoverage.BROKEN, 2500)
            );

            WeatherConditions conditions = new WeatherConditions(
                    wind, visibility, weather, sky, temp, pressure
            );

            assertThat(conditions.wind()).isEqualTo(wind);
            assertThat(conditions.visibility()).isEqualTo(visibility);
            assertThat(conditions.presentWeather()).isEqualTo(weather);
            assertThat(conditions.skyConditions()).isEqualTo(sky);
            assertThat(conditions.temperature()).isEqualTo(temp);
            assertThat(conditions.pressure()).isEqualTo(pressure);
        }

        @Test
        void testConstruction_NullFields() {
            WeatherConditions conditions = new WeatherConditions(
                    null, null, null, null, null, null
            );

            assertThat(conditions.wind()).isNull();
            assertThat(conditions.visibility()).isNull();
            assertThat(conditions.presentWeather()).isEmpty();  // null becomes empty list
            assertThat(conditions.skyConditions()).isEmpty();   // null becomes empty list
            assertThat(conditions.temperature()).isNull();
            assertThat(conditions.pressure()).isNull();
        }

        @Test
        void testConstruction_DefensiveCopyOfLists() {
            List<PresentWeather> weatherList = new java.util.ArrayList<>();
            weatherList.add(PresentWeather.parse("RA"));

            List<SkyCondition> skyList = new java.util.ArrayList<>();
            skyList.add(SkyCondition.of(SkyCoverage.SCATTERED, 5000));

            WeatherConditions conditions = new WeatherConditions(
                    null, null, weatherList, skyList, null, null
            );

            // Modify original lists
            weatherList.add(PresentWeather.parse("SN"));
            skyList.add(SkyCondition.of(SkyCoverage.OVERCAST, 2000));

            // Conditions should be unchanged (defensive copy)
            assertThat(conditions.presentWeather()).hasSize(1);
            assertThat(conditions.skyConditions()).hasSize(1);
        }

        @Test
        void testConstruction_ListsAreImmutable() {
            WeatherConditions conditions = new WeatherConditions(
                    null, null,
                    List.of(PresentWeather.parse("RA")),
                    List.of(SkyCondition.of(SkyCoverage.BROKEN, 2500)),
                    null, null
            );

            // Attempting to modify should throw UnsupportedOperationException
            assertThat(conditions.presentWeather()).isUnmodifiable();
            assertThat(conditions.skyConditions()).isUnmodifiable();
        }
    }

    // ==================== Factory Method Tests ====================

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        void testEmpty() {
            WeatherConditions conditions = WeatherConditions.empty();

            assertThat(conditions.wind()).isNull();
            assertThat(conditions.visibility()).isNull();
            assertThat(conditions.presentWeather()).isEmpty();
            assertThat(conditions.skyConditions()).isEmpty();
            assertThat(conditions.temperature()).isNull();
            assertThat(conditions.pressure()).isNull();
        }

        @Test
        void testOfBasic() {
            Wind wind = Wind.of(180, 15, "KT");
            Visibility visibility = Visibility.statuteMiles(5.0);

            WeatherConditions conditions = WeatherConditions.ofBasic(wind, visibility);

            assertThat(conditions.wind()).isEqualTo(wind);
            assertThat(conditions.visibility()).isEqualTo(visibility);
            assertThat(conditions.presentWeather()).isEmpty();
            assertThat(conditions.skyConditions()).isEmpty();
            assertThat(conditions.temperature()).isNull();
            assertThat(conditions.pressure()).isNull();
        }

        @Test
        void testOf_FullSet() {
            Wind wind = Wind.of(270, 10, "KT");
            Visibility visibility = Visibility.statuteMiles(10.0);
            Temperature temp = Temperature.ofCurrent(22.0, 12.0);
            Pressure pressure = Pressure.inchesHg(29.92);
            List<PresentWeather> weather = List.of(PresentWeather.parse("RA"));
            List<SkyCondition> sky = List.of(SkyCondition.of(SkyCoverage.BROKEN, 2500));

            WeatherConditions conditions = WeatherConditions.of(
                    wind, visibility, weather, sky, temp, pressure
            );

            assertThat(conditions.wind()).isEqualTo(wind);
            assertThat(conditions.visibility()).isEqualTo(visibility);
            assertThat(conditions.presentWeather()).isEqualTo(weather);
            assertThat(conditions.skyConditions()).isEqualTo(sky);
            assertThat(conditions.temperature()).isEqualTo(temp);
            assertThat(conditions.pressure()).isEqualTo(pressure);
        }
    }

    // ==================== Builder Tests ====================

    @Nested
    @DisplayName("Builder Pattern")
    class BuilderTests {

        @Test
        void testBuilder_AllFields() {
            Wind wind = Wind.of(270, 10, "KT");
            Visibility visibility = Visibility.statuteMiles(10.0);
            Temperature temp = Temperature.ofCurrent(22.0, 12.0);
            Pressure pressure = Pressure.inchesHg(29.92);
            List<PresentWeather> weather = List.of(PresentWeather.parse("RA"));
            List<SkyCondition> sky = List.of(SkyCondition.of(SkyCoverage.BROKEN, 2500));

            WeatherConditions conditions = WeatherConditions.builder()
                    .wind(wind)
                    .visibility(visibility)
                    .presentWeather(weather)
                    .skyConditions(sky)
                    .temperature(temp)
                    .pressure(pressure)
                    .build();

            assertThat(conditions.wind()).isEqualTo(wind);
            assertThat(conditions.visibility()).isEqualTo(visibility);
            assertThat(conditions.presentWeather()).isEqualTo(weather);
            assertThat(conditions.skyConditions()).isEqualTo(sky);
            assertThat(conditions.temperature()).isEqualTo(temp);
            assertThat(conditions.pressure()).isEqualTo(pressure);
        }

        @Test
        void testBuilder_PartialFields() {
            Wind wind = Wind.of(180, 15, "KT");
            Visibility visibility = Visibility.statuteMiles(5.0);

            WeatherConditions conditions = WeatherConditions.builder()
                    .wind(wind)
                    .visibility(visibility)
                    .build();

            assertThat(conditions.wind()).isEqualTo(wind);
            assertThat(conditions.visibility()).isEqualTo(visibility);
            assertThat(conditions.presentWeather()).isEmpty();
            assertThat(conditions.skyConditions()).isEmpty();
            assertThat(conditions.temperature()).isNull();
            assertThat(conditions.pressure()).isNull();
        }

        @Test
        void testBuilder_NoFields() {
            WeatherConditions conditions = WeatherConditions.builder().build();

            assertThat(conditions.wind()).isNull();
            assertThat(conditions.visibility()).isNull();
            assertThat(conditions.presentWeather()).isEmpty();
            assertThat(conditions.skyConditions()).isEmpty();
            assertThat(conditions.temperature()).isNull();
            assertThat(conditions.pressure()).isNull();
        }

        @Test
        void testBuilder_Chaining() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .wind(Wind.of(270, 10, "KT"))
                    .visibility(Visibility.statuteMiles(10.0))
                    .temperature(Temperature.ofCurrent(22.0, 12.0))
                    .build();

            assertThat(conditions.wind()).isNotNull();
            assertThat(conditions.visibility()).isNotNull();
            assertThat(conditions.temperature()).isNotNull();
        }
    }

    // ==================== Query Method Tests ====================

    @Nested
    @DisplayName("Query Methods")
    class QueryMethodTests {

        @Test
        void testHasAnyConditions_True_WithWind() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .wind(Wind.of(270, 10, "KT"))
                    .build();

            assertThat(conditions.hasAnyConditions()).isTrue();
        }

        @Test
        void testHasAnyConditions_True_WithVisibility() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .visibility(Visibility.statuteMiles(10.0))
                    .build();

            assertThat(conditions.hasAnyConditions()).isTrue();
        }

        @Test
        void testHasAnyConditions_True_WithWeather() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .presentWeather(List.of(PresentWeather.parse("RA")))
                    .build();

            assertThat(conditions.hasAnyConditions()).isTrue();
        }

        @Test
        void testHasAnyConditions_True_WithSky() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .skyConditions(List.of(SkyCondition.of(SkyCoverage.SCATTERED, 5000)))
                    .build();

            assertThat(conditions.hasAnyConditions()).isTrue();
        }

        @Test
        void testHasAnyConditions_True_WithTemperature() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .temperature(Temperature.ofCurrent(22.0, 12.0))
                    .build();

            assertThat(conditions.hasAnyConditions()).isTrue();
        }

        @Test
        void testHasAnyConditions_True_WithPressure() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .pressure(Pressure.inchesHg(29.92))
                    .build();

            assertThat(conditions.hasAnyConditions()).isTrue();
        }

        @Test
        void testHasAnyConditions_False_Empty() {
            WeatherConditions conditions = WeatherConditions.empty();

            assertThat(conditions.hasAnyConditions()).isFalse();
        }

        @Test
        void testHasAnyConditions_False_OnlyEmptyLists() {
            WeatherConditions conditions = new WeatherConditions(
                    null, null, List.of(), List.of(), null, null
            );

            assertThat(conditions.hasAnyConditions()).isFalse();
        }
    }

    @Nested
    @DisplayName("Clear and Calm Tests")
    class ClearAndCalmTests {

        @Test
        void testIsClearAndCalm_True() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .wind(Wind.calm())
                    .visibility(Visibility.statuteMiles(10.0))
                    .skyConditions(List.of(SkyCondition.of(SkyCoverage.FEW, 25000)))
                    .build();

            assertThat(conditions.isClearAndCalm()).isTrue();
        }

        @Test
        void testIsClearAndCalm_True_NoWind_NoSky() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .visibility(Visibility.statuteMiles(10.0))
                    .build();

            assertThat(conditions.isClearAndCalm()).isTrue();
        }

        @Test
        void testIsClearAndCalm_False_HighWind() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .wind(Wind.of(270, 20, "KT"))
                    .visibility(Visibility.statuteMiles(10.0))
                    .build();

            assertThat(conditions.isClearAndCalm()).isFalse();
        }

        @Test
        void testIsClearAndCalm_False_LowVisibility() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .wind(Wind.calm())
                    .visibility(Visibility.statuteMiles(2.0))
                    .build();

            assertThat(conditions.isClearAndCalm()).isFalse();
        }

        @Test
        void testIsClearAndCalm_False_PresentWeather() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .wind(Wind.calm())
                    .visibility(Visibility.statuteMiles(10.0))
                    .presentWeather(List.of(PresentWeather.parse("RA")))
                    .build();

            assertThat(conditions.isClearAndCalm()).isFalse();
        }

        @Test
        void testIsClearAndCalm_False_OvercastSky() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .wind(Wind.calm())
                    .visibility(Visibility.statuteMiles(10.0))
                    .skyConditions(List.of(SkyCondition.of(SkyCoverage.OVERCAST, 5000)))
                    .build();

            assertThat(conditions.isClearAndCalm()).isFalse();
        }
    }

    @Nested
    @DisplayName("Ceiling Tests")
    class CeilingTests {

        @Test
        void testHasCeiling_True_BKN() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .skyConditions(List.of(SkyCondition.of(SkyCoverage.BROKEN, 2500)))
                    .build();

            assertThat(conditions.hasCeiling()).isTrue();
        }

        @Test
        void testHasCeiling_True_OVC() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .skyConditions(List.of(SkyCondition.of(SkyCoverage.OVERCAST, 1500)))
                    .build();

            assertThat(conditions.hasCeiling()).isTrue();
        }

        @Test
        void testHasCeiling_False_SCT() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .skyConditions(List.of(SkyCondition.of(SkyCoverage.SCATTERED, 5000)))
                    .build();

            assertThat(conditions.hasCeiling()).isFalse();
        }

        @Test
        void testHasCeiling_False_Empty() {
            WeatherConditions conditions = WeatherConditions.empty();

            assertThat(conditions.hasCeiling()).isFalse();
        }

        @Test
        void testGetCeilingFeet_BKN() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .skyConditions(List.of(SkyCondition.of(SkyCoverage.BROKEN, 2500)))
                    .build();

            assertThat(conditions.getCeilingFeet()).isEqualTo(2500);
        }

        @Test
        void testGetCeilingFeet_LowestBKN() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .skyConditions(List.of(
                            SkyCondition.of(SkyCoverage.SCATTERED, 5000),
                            SkyCondition.of(SkyCoverage.BROKEN, 2500),
                            SkyCondition.of(SkyCoverage.OVERCAST, 3500)
                    ))
                    .build();

            assertThat(conditions.getCeilingFeet()).isEqualTo(2500);
        }

        @Test
        void testGetCeilingFeet_NoCeiling() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .skyConditions(List.of(SkyCondition.of(SkyCoverage.FEW, 5000)))
                    .build();

            assertThat(conditions.getCeilingFeet()).isNull();
        }

        @Test
        void testGetCeilingFeet_Empty() {
            WeatherConditions conditions = WeatherConditions.empty();

            assertThat(conditions.getCeilingFeet()).isNull();
        }
    }

    @Nested
    @DisplayName("Precipitation Tests")
    class PrecipitationTests {

        @Test
        void testHasPrecipitation_True_Rain() {
            PresentWeather rain = PresentWeather.parse("RA");
            WeatherConditions conditions = WeatherConditions.builder()
                    .presentWeather(List.of(rain))
                    .build();

            assertThat(conditions.hasPrecipitation()).isTrue();
        }

        @Test
        void testHasPrecipitation_False_Fog() {
            PresentWeather fog = PresentWeather.parse("FG");
            WeatherConditions conditions = WeatherConditions.builder()
                    .presentWeather(List.of(fog))
                    .build();

            // Depends on PresentWeather.hasPrecipitation() implementation
            // Fog typically doesn't count as precipitation
            assertThat(conditions.hasPrecipitation()).isFalse();
        }

        @Test
        void testHasPrecipitation_False_Empty() {
            WeatherConditions conditions = WeatherConditions.empty();

            assertThat(conditions.hasPrecipitation()).isFalse();
        }
    }

    @Nested
    @DisplayName("Thunderstorm Tests")
    class ThunderstormTests {

        @Test
        void testHasThunderstorms_True() {
            PresentWeather ts = PresentWeather.parse("TSRA");
            WeatherConditions conditions = WeatherConditions.builder()
                    .presentWeather(List.of(ts))
                    .build();

            assertThat(conditions.hasThunderstorms()).isTrue();
        }

        @Test
        void testHasThunderstorms_False_Rain() {
            PresentWeather rain = PresentWeather.parse("RA");
            WeatherConditions conditions = WeatherConditions.builder()
                    .presentWeather(List.of(rain))
                    .build();

            assertThat(conditions.hasThunderstorms()).isFalse();
        }

        @Test
        void testHasThunderstorms_False_Empty() {
            WeatherConditions conditions = WeatherConditions.empty();

            assertThat(conditions.hasThunderstorms()).isFalse();
        }
    }

    @Nested
    @DisplayName("Freezing Conditions Tests")
    class FreezingConditionsTests {

        @Test
        void testHasFreezingConditions_True_FreezingTemp() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .temperature(Temperature.ofCurrent(-5.0, -10.0))
                    .build();

            assertThat(conditions.hasFreezingConditions()).isTrue();
        }

        @Test
        void testHasFreezingConditions_True_FreezingWeather() {
            // Freezing rain (FZRA)
            PresentWeather fzra = PresentWeather.parse("FZRA");

            WeatherConditions conditions = WeatherConditions.builder()
                    .presentWeather(List.of(fzra))
                    .build();

            assertThat(conditions.hasFreezingConditions()).isTrue();
        }

        @Test
        void testHasFreezingConditions_False_AboveFreezing() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .temperature(Temperature.ofCurrent(15.0, 10.0))
                    .build();

            assertThat(conditions.hasFreezingConditions()).isFalse();
        }

        @Test
        void testHasFreezingConditions_False_Empty() {
            WeatherConditions conditions = WeatherConditions.empty();

            assertThat(conditions.hasFreezingConditions()).isFalse();
        }
    }

    @Nested
    @DisplayName("IMC/VMC Tests")
    class ImcVmcTests {

        @Test
        void testIsLikelyIMC_True_LowVisibility() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .visibility(Visibility.statuteMiles(2.0))
                    .build();

            assertThat(conditions.isLikelyIMC()).isTrue();
        }

        @Test
        void testIsLikelyIMC_True_LowCeiling() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .visibility(Visibility.statuteMiles(10.0))
                    .skyConditions(List.of(SkyCondition.of(SkyCoverage.BROKEN, 800)))
                    .build();

            assertThat(conditions.isLikelyIMC()).isTrue();
        }

        @Test
        void testIsLikelyIMC_False_GoodConditions() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .visibility(Visibility.statuteMiles(10.0))
                    .skyConditions(List.of(SkyCondition.of(SkyCoverage.FEW, 5000)))
                    .build();

            assertThat(conditions.isLikelyIMC()).isFalse();
        }

        @Test
        void testIsLikelyVMC_True_GoodConditions() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .visibility(Visibility.statuteMiles(10.0))
                    .skyConditions(List.of(SkyCondition.of(SkyCoverage.FEW, 5000)))
                    .build();

            assertThat(conditions.isLikelyVMC()).isTrue();
        }

        @Test
        void testIsLikelyVMC_False_LowVisibility() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .visibility(Visibility.statuteMiles(2.0))
                    .build();

            assertThat(conditions.isLikelyVMC()).isFalse();
        }
    }

    // ==================== Summary Tests ====================

    @Nested
    @DisplayName("Summary Generation")
    class SummaryTests {

        @Test
        void testGetSummary_FullConditions() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .wind(Wind.of(270, 10, "KT"))
                    .visibility(Visibility.statuteMiles(10.0))
                    .presentWeather(List.of(PresentWeather.parse("RA")))
                    .skyConditions(List.of(SkyCondition.of(SkyCoverage.BROKEN, 2500)))
                    .temperature(Temperature.ofCurrent(22.0, 12.0))
                    .pressure(Pressure.inchesHg(29.92))
                    .build();

            String summary = conditions.getSummary();

            assertThat(summary)
                    .contains("Wind:")
                    .contains("Vis:")
                    .contains("Weather:")
                    .contains("Sky:")
                    .contains("Temp:")
                    .contains("Press:");
        }

        @Test
        void testGetSummary_PartialConditions() {
            WeatherConditions conditions = WeatherConditions.builder()
                    .wind(Wind.of(180, 15, "KT"))
                    .visibility(Visibility.statuteMiles(5.0))
                    .build();

            String summary = conditions.getSummary();

            assertThat(summary)
                    .contains("Wind:")
                    .contains("Vis:")
                    .doesNotContain("Weather:")
                    .doesNotContain("Temp:");
        }

        @Test
        void testGetSummary_Empty() {
            WeatherConditions conditions = WeatherConditions.empty();

            String summary = conditions.getSummary();

            assertThat(summary).isEqualTo("No conditions reported");
        }
    }

    // ==================== Equality and HashCode Tests ====================

    @Test
    void testEquality() {
        Wind wind = Wind.of(270, 10, "KT");
        Visibility visibility = Visibility.statuteMiles(10.0);

        WeatherConditions conditions1 = WeatherConditions.ofBasic(wind, visibility);
        WeatherConditions conditions2 = WeatherConditions.ofBasic(wind, visibility);
        WeatherConditions conditions3 = WeatherConditions.ofBasic(Wind.of(180, 10, "KT"), visibility);

        assertThat(conditions1)
                .isEqualTo(conditions2)
                .isNotEqualTo(conditions3);
    }

    @Test
    void testHashCode() {
        Wind wind = Wind.of(270, 10, "KT");
        Visibility visibility = Visibility.statuteMiles(10.0);

        WeatherConditions conditions1 = WeatherConditions.ofBasic(wind, visibility);
        WeatherConditions conditions2 = WeatherConditions.ofBasic(wind, visibility);

        assertThat(conditions1.hashCode()).hasSameHashCodeAs(conditions2);
    }

    @Test
    void testToString() {
        WeatherConditions conditions = WeatherConditions.builder()
                .wind(Wind.of(270, 10, "KT"))
                .visibility(Visibility.statuteMiles(10.0))
                .build();

        String str = conditions.toString();

        assertThat(str)
                .isNotNull()
                .contains("WeatherConditions");
    }
}
