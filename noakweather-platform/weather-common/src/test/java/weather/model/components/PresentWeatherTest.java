/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025 bclasky1539
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for PresentWeather record.
 *
 * @author bclasky1539
 *
 */
@DisplayName("PresentWeather")
class PresentWeatherTest {

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should reject null raw code")
        void testNullRawCode() {
            assertThatThrownBy(() ->
                    new PresentWeather(null, null, null, null, null, null)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Raw weather code cannot be null or blank");
        }

        @Test
        @DisplayName("Should reject blank raw code")
        void testBlankRawCode() {
            assertThatThrownBy(() ->
                    new PresentWeather(null, null, null, null, null, "   ")
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Raw weather code cannot be null or blank");
        }

        @Test
        @DisplayName("Should accept null optional components")
        void testNullOptionalComponents() {
            PresentWeather weather = new PresentWeather(null, null, null, null, null, "RA");

            assertThat(weather.intensity()).isNull();
            assertThat(weather.descriptor()).isNull();
            assertThat(weather.precipitation()).isNull();
            assertThat(weather.obscuration()).isNull();
            assertThat(weather.other()).isNull();
            assertThat(weather.rawCode()).isEqualTo("RA");
        }
    }

    @Nested
    @DisplayName("Normalization Tests")
    class NormalizationTests {

        @Test
        @DisplayName("Should normalize components to uppercase")
        void testUppercaseNormalization() {
            PresentWeather weather = new PresentWeather("-", "ts", "ra", "fg", null, "-tsra");

            assertThat(weather.intensity()).isEqualTo("-");
            assertThat(weather.descriptor()).isEqualTo("TS");
            assertThat(weather.precipitation()).isEqualTo("RA");
            assertThat(weather.obscuration()).isEqualTo("FG");
        }

        @Test
        @DisplayName("Should trim whitespace from components")
        void testTrimWhitespace() {
            PresentWeather weather = new PresentWeather(" - ", " TS ", " RA ", null, null, "-TSRA");

            assertThat(weather.intensity()).isEqualTo("-");
            assertThat(weather.descriptor()).isEqualTo("TS");
            assertThat(weather.precipitation()).isEqualTo("RA");
        }

        @Test
        @DisplayName("Should convert blank strings to null")
        void testBlankToNull() {
            PresentWeather weather = new PresentWeather("", "  ", null, null, null, "RA");

            assertThat(weather.intensity()).isNull();
            assertThat(weather.descriptor()).isNull();
        }
    }

    @Nested
    @DisplayName("Intensity Query Tests")
    class IntensityQueryTests {

        @Test
        @DisplayName("Should identify light intensity")
        void testIsLight() {
            PresentWeather weather = new PresentWeather("-", null, "RA", null, null, "-RA");

            assertThat(weather.isLight()).isTrue();
            assertThat(weather.isHeavy()).isFalse();
            assertThat(weather.isVicinity()).isFalse();
        }

        @Test
        @DisplayName("Should identify heavy intensity")
        void testIsHeavy() {
            PresentWeather weather = new PresentWeather("+", null, "RA", null, null, "+RA");

            assertThat(weather.isLight()).isFalse();
            assertThat(weather.isHeavy()).isTrue();
            assertThat(weather.isVicinity()).isFalse();
        }

        @Test
        @DisplayName("Should identify vicinity")
        void testIsVicinity() {
            PresentWeather weather = new PresentWeather("VC", null, null, "FG", null, "VCFG");

            assertThat(weather.isLight()).isFalse();
            assertThat(weather.isHeavy()).isFalse();
            assertThat(weather.isVicinity()).isTrue();
        }

        @Test
        @DisplayName("Should handle null intensity as moderate")
        void testNullIntensity() {
            PresentWeather weather = new PresentWeather(null, null, "RA", null, null, "RA");

            assertThat(weather.isLight()).isFalse();
            assertThat(weather.isHeavy()).isFalse();
            assertThat(weather.isVicinity()).isFalse();
        }
    }

    @Nested
    @DisplayName("Descriptor Query Tests")
    class DescriptorQueryTests {

        @Test
        @DisplayName("Should identify thunderstorm")
        void testIsThunderstorm() {
            PresentWeather weather = new PresentWeather("+", "TS", "RA", null, null, "+TSRA");

            assertThat(weather.isThunderstorm()).isTrue();
            assertThat(weather.isFreezing()).isFalse();
            assertThat(weather.isShowers()).isFalse();
        }

        @Test
        @DisplayName("Should identify freezing")
        void testIsFreezing() {
            PresentWeather weather = new PresentWeather(null, "FZ", "DZ", null, null, "FZDZ");

            assertThat(weather.isThunderstorm()).isFalse();
            assertThat(weather.isFreezing()).isTrue();
            assertThat(weather.isShowers()).isFalse();
        }

        @Test
        @DisplayName("Should identify showers")
        void testIsShowers() {
            PresentWeather weather = new PresentWeather(null, "SH", "RA", null, null, "SHRA");

            assertThat(weather.isThunderstorm()).isFalse();
            assertThat(weather.isFreezing()).isFalse();
            assertThat(weather.isShowers()).isTrue();
        }
    }

    @Nested
    @DisplayName("Precipitation and Obscuration Tests")
    class PrecipitationObscurationTests {

        @Test
        @DisplayName("Should identify precipitation")
        void testHasPrecipitation() {
            PresentWeather rain = new PresentWeather("-", null, "RA", null, null, "-RA");
            PresentWeather fog = new PresentWeather(null, null, null, "FG", null, "FG");

            assertThat(rain.hasPrecipitation()).isTrue();
            assertThat(fog.hasPrecipitation()).isFalse();
        }

        @Test
        @DisplayName("Should identify obscuration")
        void testHasObscuration() {
            PresentWeather rain = new PresentWeather("-", null, "RA", null, null, "-RA");
            PresentWeather fog = new PresentWeather(null, null, null, "FG", null, "FG");

            assertThat(rain.hasObscuration()).isFalse();
            assertThat(fog.hasObscuration()).isTrue();
        }
    }

    @Nested
    @DisplayName("Special Conditions Tests")
    class SpecialConditionsTests {

        @Test
        @DisplayName("Should identify no significant weather")
        void testNoSignificantWeather() {
            PresentWeather nsw = new PresentWeather(null, null, null, null, "NSW", "NSW");

            assertThat(nsw.isNoSignificantWeather()).isTrue();
        }

        @Test
        @DisplayName("Should not identify regular weather as NSW")
        void testNotNoSignificantWeather() {
            PresentWeather rain = new PresentWeather("-", null, "RA", null, null, "-RA");

            assertThat(rain.isNoSignificantWeather()).isFalse();
        }
    }

    @Nested
    @DisplayName("Description Tests")
    class DescriptionTests {

        @Test
        @DisplayName("Should provide description for light rain")
        void testLightRainDescription() {
            PresentWeather weather = new PresentWeather("-", null, "RA", null, null, "-RA");

            assertThat(weather.getDescription()).isEqualTo("Light Rain");
            assertThat(weather.getIntensityDescription()).isEqualTo("Light");
        }

        @Test
        @DisplayName("Should provide description for heavy thunderstorm with rain")
        void testHeavyThunderstormDescription() {
            PresentWeather weather = new PresentWeather("+", "TS", "RA", null, null, "+TSRA");

            assertThat(weather.getDescription()).isEqualTo("Heavy Thunderstorm Rain");
            assertThat(weather.getIntensityDescription()).isEqualTo("Heavy");
        }

        @Test
        @DisplayName("Should provide description for fog in vicinity")
        void testVicinityFogDescription() {
            PresentWeather weather = new PresentWeather("VC", null, null, "FG", null, "VCFG");

            assertThat(weather.getDescription()).isEqualTo("Vicinity Fog");
            assertThat(weather.getIntensityDescription()).isEqualTo("Vicinity");
        }

        @Test
        @DisplayName("Should provide description for mist")
        void testMistDescription() {
            PresentWeather weather = new PresentWeather(null, null, null, "BR", null, "BR");

            assertThat(weather.getDescription()).isEqualTo("Moderate Mist");
        }

        @Test
        @DisplayName("Should provide description for no significant weather")
        void testNoSignificantWeatherDescription() {
            PresentWeather weather = new PresentWeather(null, null, null, null, "NSW", "NSW");

            assertThat(weather.getDescription()).isEqualTo("No Significant Weather");
        }

        @Test
        @DisplayName("Should provide description for freezing drizzle")
        void testFreezingDrizzleDescription() {
            PresentWeather weather = new PresentWeather(null, "FZ", "DZ", null, null, "FZDZ");

            assertThat(weather.getDescription()).isEqualTo("Moderate Freezing Drizzle");
        }

        @Test
        @DisplayName("Should provide description for snow showers")
        void testSnowShowersDescription() {
            PresentWeather weather = new PresentWeather(null, "SH", "SN", null, null, "SHSN");

            assertThat(weather.getDescription()).isEqualTo("Moderate Showers Snow");
        }

        @Test
        @DisplayName("Should provide description for all precipitation types")
        void testAllPrecipitationTypes() {
            assertThat(new PresentWeather(null, null, "DZ", null, null, "DZ").getDescription())
                    .contains("Drizzle");
            assertThat(new PresentWeather(null, null, "SN", null, null, "SN").getDescription())
                    .contains("Snow");
            assertThat(new PresentWeather(null, null, "SG", null, null, "SG").getDescription())
                    .contains("Snow Grains");
            assertThat(new PresentWeather(null, null, "IC", null, null, "IC").getDescription())
                    .contains("Ice Crystals");
            assertThat(new PresentWeather(null, null, "PL", null, null, "PL").getDescription())
                    .contains("Ice Pellets");
            assertThat(new PresentWeather(null, null, "GR", null, null, "GR").getDescription())
                    .contains("Hail");
            assertThat(new PresentWeather(null, null, "GS", null, null, "GS").getDescription())
                    .contains("Small Hail");
            assertThat(new PresentWeather(null, null, "UP", null, null, "UP").getDescription())
                    .contains("Unknown Precipitation");
        }

        @Test
        @DisplayName("Should provide description for all obscuration types")
        void testAllObscurationTypes() {
            assertThat(new PresentWeather(null, null, null, "FG", null, "FG").getDescription())
                    .contains("Fog");
            assertThat(new PresentWeather(null, null, null, "FU", null, "FU").getDescription())
                    .contains("Smoke");
            assertThat(new PresentWeather(null, null, null, "VA", null, "VA").getDescription())
                    .contains("Volcanic Ash");
            assertThat(new PresentWeather(null, null, null, "DU", null, "DU").getDescription())
                    .contains("Dust");
            assertThat(new PresentWeather(null, null, null, "SA", null, "SA").getDescription())
                    .contains("Sand");
            assertThat(new PresentWeather(null, null, null, "HZ", null, "HZ").getDescription())
                    .contains("Haze");
            assertThat(new PresentWeather(null, null, null, "PY", null, "PY").getDescription())
                    .contains("Spray");
        }

        @Test
        @DisplayName("Should provide description for all descriptor types")
        void testAllDescriptorTypes() {
            assertThat(new PresentWeather(null, "MI", null, "FG", null, "MIFG").getDescription())
                    .contains("Shallow");
            assertThat(new PresentWeather(null, "PR", null, "FG", null, "PRFG").getDescription())
                    .contains("Partial");
            assertThat(new PresentWeather(null, "BC", null, "FG", null, "BCFG").getDescription())
                    .contains("Patches");
            assertThat(new PresentWeather(null, "DR", "SN", null, null, "DRSN").getDescription())
                    .contains("Drifting");
            assertThat(new PresentWeather(null, "BL", "SN", null, null, "BLSN").getDescription())
                    .contains("Blowing");
        }

        @Test
        @DisplayName("Should provide description for all other phenomena types")
        void testAllOtherPhenomenaTypes() {
            assertThat(new PresentWeather(null, null, null, null, "PO", "PO").getDescription())
                    .contains("Dust/Sand Whirls");
            assertThat(new PresentWeather(null, null, null, null, "FC", "FC").getDescription())
                    .contains("Funnel Cloud");
            assertThat(new PresentWeather(null, null, null, null, "SS", "SS").getDescription())
                    .contains("Sandstorm");
            assertThat(new PresentWeather(null, null, null, null, "DS", "DS").getDescription())
                    .contains("Duststorm");
        }

        @Test
        @DisplayName("Should handle unknown other phenomenon code")
        void testUnknownOtherPhenomenon() {
            // Test with an unknown/invalid code that falls to default case
            PresentWeather weather = new PresentWeather(null, null, null, null, "XX", "XX");

            String description = weather.getDescription();

            // Should return the raw code when unknown
            assertThat(description).contains("XX");
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create from raw code")
        void testOf() {
            PresentWeather weather = PresentWeather.of("-RA");

            assertThat(weather.rawCode()).isEqualTo("-RA");
            assertThat(weather.intensity()).isNull();
            assertThat(weather.descriptor()).isNull();
            assertThat(weather.precipitation()).isNull();
        }

        @Test
        @DisplayName("Should normalize raw code in factory method")
        void testOfNormalization() {
            PresentWeather weather = PresentWeather.of(" -ra ");

            assertThat(weather.rawCode()).isEqualTo("-RA");
        }

        @Test
        @DisplayName("Should reject null in factory method")
        void testOfNull() {
            assertThatThrownBy(() -> PresentWeather.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Raw code cannot be null or blank");
        }

        @Test
        @DisplayName("Should reject blank in factory method")
        void testOfBlank() {
            assertThatThrownBy(() -> PresentWeather.of("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Raw code cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("Real World Scenarios")
    class RealWorldScenarios {

        @Test
        @DisplayName("Should handle light rain and mist")
        void testLightRainAndMist() {
            PresentWeather rain = new PresentWeather("-", null, "RA", null, null, "-RA");
            PresentWeather mist = new PresentWeather(null, null, null, "BR", null, "BR");

            assertThat(rain.getDescription()).contains("Light", "Rain");
            assertThat(mist.getDescription()).contains("Mist");
        }

        @Test
        @DisplayName("Should handle thunderstorm with heavy rain")
        void testThunderstormHeavyRain() {
            PresentWeather weather = new PresentWeather("+", "TS", "RA", null, null, "+TSRA");

            assertThat(weather.isThunderstorm()).isTrue();
            assertThat(weather.isHeavy()).isTrue();
            assertThat(weather.hasPrecipitation()).isTrue();
        }

        @Test
        @DisplayName("Should handle freezing fog")
        void testFreezingFog() {
            PresentWeather weather = new PresentWeather(null, "FZ", null, "FG", null, "FZFG");

            assertThat(weather.isFreezing()).isTrue();
            assertThat(weather.hasObscuration()).isTrue();
        }

        @Test
        @DisplayName("Should handle squall")
        void testSquall() {
            PresentWeather weather = new PresentWeather(null, null, null, null, "SQ", "SQ");

            assertThat(weather.other()).isEqualTo("SQ");
            assertThat(weather.getDescription()).contains("Squall");
        }
    }

    @Nested
    @DisplayName("Parse Method Tests")
    class ParseMethodTests {

        @Test
        @DisplayName("Should parse simple rain")
        void testParseSimpleRain() {
            PresentWeather weather = PresentWeather.parse("RA");

            assertThat(weather.rawCode()).isEqualTo("RA");
            assertThat(weather.intensity()).isNull();
            assertThat(weather.descriptor()).isNull();
            assertThat(weather.precipitation()).isEqualTo("RA");
            assertThat(weather.obscuration()).isNull();
            assertThat(weather.other()).isNull();
        }

        @Test
        @DisplayName("Should parse light rain")
        void testParseLightRain() {
            PresentWeather weather = PresentWeather.parse("-RA");

            assertThat(weather.rawCode()).isEqualTo("-RA");
            assertThat(weather.intensity()).isEqualTo("-");
            assertThat(weather.precipitation()).isEqualTo("RA");
            assertThat(weather.isLight()).isTrue();
        }

        @Test
        @DisplayName("Should parse heavy thunderstorm with rain")
        void testParseHeavyThunderstorm() {
            PresentWeather weather = PresentWeather.parse("+TSRA");

            assertThat(weather.rawCode()).isEqualTo("+TSRA");
            assertThat(weather.intensity()).isEqualTo("+");
            assertThat(weather.descriptor()).isEqualTo("TS");
            assertThat(weather.precipitation()).isEqualTo("RA");
            assertThat(weather.isHeavy()).isTrue();
            assertThat(weather.isThunderstorm()).isTrue();
        }

        @Test
        @DisplayName("Should parse vicinity fog")
        void testParseVicinityFog() {
            PresentWeather weather = PresentWeather.parse("VCFG");

            assertThat(weather.rawCode()).isEqualTo("VCFG");
            assertThat(weather.intensity()).isEqualTo("VC");
            assertThat(weather.obscuration()).isEqualTo("FG");
            assertThat(weather.isVicinity()).isTrue();
        }

        @Test
        @DisplayName("Should parse mist")
        void testParseMist() {
            PresentWeather weather = PresentWeather.parse("BR");

            assertThat(weather.rawCode()).isEqualTo("BR");
            assertThat(weather.obscuration()).isEqualTo("BR");
        }

        @Test
        @DisplayName("Should parse no significant weather")
        void testParseNoSignificantWeather() {
            PresentWeather weather = PresentWeather.parse("NSW");

            assertThat(weather.rawCode()).isEqualTo("NSW");
            assertThat(weather.other()).isEqualTo("NSW");
            assertThat(weather.isNoSignificantWeather()).isTrue();
        }

        @Test
        @DisplayName("Should parse freezing drizzle")
        void testParseFreezingDrizzle() {
            PresentWeather weather = PresentWeather.parse("FZDZ");

            assertThat(weather.descriptor()).isEqualTo("FZ");
            assertThat(weather.precipitation()).isEqualTo("DZ");
            assertThat(weather.isFreezing()).isTrue();
        }

        @Test
        @DisplayName("Should parse shower with snow")
        void testParseShowerSnow() {
            PresentWeather weather = PresentWeather.parse("SHSN");

            assertThat(weather.descriptor()).isEqualTo("SH");
            assertThat(weather.precipitation()).isEqualTo("SN");
            assertThat(weather.isShowers()).isTrue();
        }

        @Test
        @DisplayName("Should parse blowing snow")
        void testParseBlowingSnow() {
            PresentWeather weather = PresentWeather.parse("BLSN");

            assertThat(weather.descriptor()).isEqualTo("BL");
            assertThat(weather.precipitation()).isEqualTo("SN");
        }

        @Test
        @DisplayName("Should parse fog")
        void testParseFog() {
            PresentWeather weather = PresentWeather.parse("FG");

            assertThat(weather.obscuration()).isEqualTo("FG");
            assertThat(weather.hasObscuration()).isTrue();
        }

        @Test
        @DisplayName("Should parse squall")
        void testParseSquall() {
            PresentWeather weather = PresentWeather.parse("SQ");

            assertThat(weather.other()).isEqualTo("SQ");
        }

        @Test
        @DisplayName("Should normalize lowercase to uppercase")
        void testParseLowercase() {
            PresentWeather weather = PresentWeather.parse("-ra");

            assertThat(weather.rawCode()).isEqualTo("-RA");
            assertThat(weather.intensity()).isEqualTo("-");
            assertThat(weather.precipitation()).isEqualTo("RA");
        }

        @Test
        @DisplayName("Should trim whitespace")
        void testParseTrimWhitespace() {
            PresentWeather weather = PresentWeather.parse("  +TSRA  ");

            assertThat(weather.rawCode()).isEqualTo("+TSRA");
        }

        @Test
        @DisplayName("Should reject null code")
        void testParseNull() {
            assertThatThrownBy(() -> PresentWeather.parse(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Weather code cannot be null or blank");
        }

        @Test
        @DisplayName("Should reject blank code")
        void testParseBlank() {
            assertThatThrownBy(() -> PresentWeather.parse("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Weather code cannot be null or blank");
        }

        @Test
        @DisplayName("Should parse all precipitation types")
        void testParseAllPrecipitationTypes() {
            assertThat(PresentWeather.parse("DZ").precipitation()).isEqualTo("DZ");
            assertThat(PresentWeather.parse("RA").precipitation()).isEqualTo("RA");
            assertThat(PresentWeather.parse("SN").precipitation()).isEqualTo("SN");
            assertThat(PresentWeather.parse("SG").precipitation()).isEqualTo("SG");
            assertThat(PresentWeather.parse("IC").precipitation()).isEqualTo("IC");
            assertThat(PresentWeather.parse("PL").precipitation()).isEqualTo("PL");
            assertThat(PresentWeather.parse("GR").precipitation()).isEqualTo("GR");
            assertThat(PresentWeather.parse("GS").precipitation()).isEqualTo("GS");
            assertThat(PresentWeather.parse("UP").precipitation()).isEqualTo("UP");
        }

        @Test
        @DisplayName("Should parse all obscuration types")
        void testParseAllObscurationTypes() {
            assertThat(PresentWeather.parse("BR").obscuration()).isEqualTo("BR");
            assertThat(PresentWeather.parse("FG").obscuration()).isEqualTo("FG");
            assertThat(PresentWeather.parse("FU").obscuration()).isEqualTo("FU");
            assertThat(PresentWeather.parse("VA").obscuration()).isEqualTo("VA");
            assertThat(PresentWeather.parse("DU").obscuration()).isEqualTo("DU");
            assertThat(PresentWeather.parse("SA").obscuration()).isEqualTo("SA");
            assertThat(PresentWeather.parse("HZ").obscuration()).isEqualTo("HZ");
            assertThat(PresentWeather.parse("PY").obscuration()).isEqualTo("PY");
        }

        @Test
        @DisplayName("Should parse all descriptor types")
        void testParseAllDescriptorTypes() {
            assertThat(PresentWeather.parse("MIFG").descriptor()).isEqualTo("MI");
            assertThat(PresentWeather.parse("PRFG").descriptor()).isEqualTo("PR");
            assertThat(PresentWeather.parse("BCFG").descriptor()).isEqualTo("BC");
            assertThat(PresentWeather.parse("DRSN").descriptor()).isEqualTo("DR");
            assertThat(PresentWeather.parse("BLSN").descriptor()).isEqualTo("BL");
            assertThat(PresentWeather.parse("SHSN").descriptor()).isEqualTo("SH");
            assertThat(PresentWeather.parse("TSRA").descriptor()).isEqualTo("TS");
            assertThat(PresentWeather.parse("FZDZ").descriptor()).isEqualTo("FZ");
        }

        @Test
        @DisplayName("Should parse all other phenomena types")
        void testParseAllOtherPhenomenaTypes() {
            assertThat(PresentWeather.parse("PO").other()).isEqualTo("PO");
            assertThat(PresentWeather.parse("SQ").other()).isEqualTo("SQ");
            assertThat(PresentWeather.parse("FC").other()).isEqualTo("FC");
            assertThat(PresentWeather.parse("SS").other()).isEqualTo("SS");
            assertThat(PresentWeather.parse("DS").other()).isEqualTo("DS");
        }

        @Test
        @DisplayName("Should parse complex weather with multiple components")
        void testParseComplexWeather() {
            // Heavy thunderstorm with rain and fog
            PresentWeather weather = PresentWeather.parse("+TSRAFG");

            assertThat(weather.intensity()).isEqualTo("+");
            assertThat(weather.descriptor()).isEqualTo("TS");
            assertThat(weather.precipitation()).isEqualTo("RA");
            assertThat(weather.obscuration()).isEqualTo("FG");
        }
    }
}
