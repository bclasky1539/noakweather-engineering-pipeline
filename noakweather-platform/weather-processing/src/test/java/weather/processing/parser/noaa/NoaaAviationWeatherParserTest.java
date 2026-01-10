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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import weather.model.NoaaWeatherData;
import weather.model.WeatherConditions;
import weather.model.components.*;
import weather.model.enums.SkyCoverage;
import weather.processing.parser.common.ParseResult;

import java.time.Instant;
import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.*;
import static weather.processing.parser.noaa.RegExprConst.*;

/**
 * Tests for NoaaAviationWeatherParser base class functionality.
 *
 * Since NoaaAviationWeatherParser is abstract, we test it through a concrete
 * test implementation that exposes the protected methods for testing.
 *
 * Tests cover:
 * - Wind parsing
 * - Visibility parsing (all formats)
 * - Present weather parsing
 * - Sky condition parsing
 * - Utility methods (fractions, coverage, height, cloud type)
 * - State initialization
 *
 * @author bclasky1539
 *
 */
class NoaaAviationWeatherParserTest {

    private TestAviationWeatherParser parser;

    @BeforeEach
    void setUp() {
        parser = new TestAviationWeatherParser();
    }

    // ==================== INITIALIZATION TESTS ====================

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("initializeSharedState should create new builder and empty lists")
        void testInitializeSharedState() {
            parser.initializeSharedState();

            assertThat(parser.conditionsBuilder).isNotNull();
            assertThat(parser.presentWeatherList).isNotNull().isEmpty();
            assertThat(parser.skyConditionsList).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("initializeSharedState should reset existing state")
        void testInitializeSharedState_Reset() {
            // Initialize and add some data
            parser.initializeSharedState();
            parser.presentWeatherList.add(PresentWeather.parse("-RA"));
            parser.skyConditionsList.add(new SkyCondition(SkyCoverage.FEW, 10000, null));

            // Reset
            parser.initializeSharedState();

            // Verify state is clean
            assertThat(parser.presentWeatherList).isEmpty();
            assertThat(parser.skyConditionsList).isEmpty();
        }
    }

    // ==================== WIND PARSING TESTS ====================

    @Nested
    @DisplayName("Wind Parsing Tests")
    class WindParsingTests {

        @BeforeEach
        void setUp() {
            parser.initializeSharedState();
            parser.weatherData = new TestWeatherData("TEST", Instant.now());
        }

        @Test
        @DisplayName("handleWind should parse basic wind")
        void testHandleWind_Basic() {
            String input = "19005KT ";
            Matcher matcher = WIND_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handleWind(matcher);
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.wind()).isNotNull();
            Wind wind = conditions.wind();
            assertThat(wind.directionDegrees()).isEqualTo(190);
            assertThat(wind.speedValue()).isEqualTo(5);
            assertThat(wind.gustValue()).isNull();
        }

        @Test
        @DisplayName("handleWind should parse wind with gusts")
        void testHandleWind_WithGusts() {
            String input = "18016G28KT ";
            Matcher matcher = WIND_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handleWind(matcher);
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.wind()).isNotNull();
            Wind wind = conditions.wind();
            assertThat(wind.directionDegrees()).isEqualTo(180);
            assertThat(wind.speedValue()).isEqualTo(16);
            assertThat(wind.gustValue()).isEqualTo(28);
        }

        @Test
        @DisplayName("handleWind should parse variable wind")
        void testHandleWind_Variable() {
            String input = "VRB03KT ";
            Matcher matcher = WIND_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handleWind(matcher);
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.wind()).isNotNull();
            Wind wind = conditions.wind();
            assertThat(wind.directionDegrees()).isNull(); // Variable = no direction
            assertThat(wind.speedValue()).isEqualTo(3);
        }

        @Test
        @DisplayName("handleWind should not set wind when weatherData is null")
        void testHandleWind_NullWeatherData() {
            parser.weatherData = null;
            String input = "19005KT ";
            Matcher matcher = WIND_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handleWind(matcher);
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.wind()).isNull();
        }
    }

    // ==================== VISIBILITY PARSING TESTS ====================

    @Nested
    @DisplayName("Visibility Parsing Tests")
    class VisibilityParsingTests {

        @BeforeEach
        void setUp() {
            parser.initializeSharedState();
            parser.weatherData = new TestWeatherData("TEST", Instant.now());
        }

        @Test
        @DisplayName("handleVisibility should parse CAVOK")
        void testHandleVisibility_CAVOK() {
            String input = "CAVOK ";
            Matcher matcher = VISIBILITY_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handleVisibility(matcher);
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.visibility()).isNotNull();
            Visibility vis = conditions.visibility();
            assertThat(vis.isCavok()).isTrue();
        }

        @Test
        @DisplayName("handleVisibility should parse NDV and not set visibility")
        void testHandleVisibility_NDV() {
            String input = "NDV ";
            Matcher matcher = VISIBILITY_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handleVisibility(matcher);
            WeatherConditions conditions = parser.buildConditions();

            // NDV means no visibility is set
            assertThat(conditions.visibility()).isNull();
        }

        @ParameterizedTest
        @CsvSource({
                "'10SM ', 10.0, 'whole number'",
                "'1/2SM ', 0.5, 'simple fraction'",
                "'1 1/2SM ', 1.5, 'mixed fraction'",
                "'3/4SM ', 0.75, 'three-quarter fraction'",
                "'2 3/4SM ', 2.75, 'mixed fraction with 3/4'"
        })
        @DisplayName("handleVisibility should parse statute miles in various formats")
        void testHandleVisibility_StatuteMiles(String input, double expectedMiles, String scenario) {
            Matcher matcher = VISIBILITY_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handleVisibility(matcher);
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.visibility())
                    .as("Visibility should be present for: %s", scenario)
                    .isNotNull();
            Visibility vis = conditions.visibility();
            assertThat(vis.toStatuteMiles())
                    .as("Statute miles should match for: %s", scenario)
                    .isEqualTo(expectedMiles);
        }

        @Test
        @DisplayName("handleVisibility should parse meters")
        void testHandleVisibility_Meters() {
            String input = "9999 ";
            Matcher matcher = VISIBILITY_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handleVisibility(matcher);
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.visibility()).isNotNull();
            Visibility vis = conditions.visibility();
            assertThat(vis.toMeters()).isEqualTo(9999.0);
        }

        @Test
        @DisplayName("handleVisibility should parse with M prefix (less than)")
        void testHandleVisibility_LessThan() {
            String input = "M1/4SM ";
            Matcher matcher = VISIBILITY_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handleVisibility(matcher);
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.visibility()).isNotNull();
            Visibility vis = conditions.visibility();
            assertThat(vis.lessThan()).isTrue();
            assertThat(vis.toStatuteMiles()).isEqualTo(0.25);
        }

        @Test
        @DisplayName("handleVisibility should parse with P prefix (greater than)")
        void testHandleVisibility_GreaterThan() {
            String input = "P6SM ";
            Matcher matcher = VISIBILITY_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handleVisibility(matcher);
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.visibility()).isNotNull();
            Visibility vis = conditions.visibility();
            assertThat(vis.greaterThan()).isTrue();
            assertThat(vis.toStatuteMiles()).isEqualTo(6.0);
        }
    }

    // ==================== FRACTIONAL DISTANCE TESTS ====================

    @Nested
    @DisplayName("Fractional Distance Parsing Tests")
    class FractionalDistanceTests {

        @ParameterizedTest
        @CsvSource({
                "10, 10.0",
                "1/2, 0.5",
                "1/4, 0.25",
                "3/4, 0.75",
                "1 1/2, 1.5",
                "2 1/4, 2.25",
                "5 3/4, 5.75"
        })
        @DisplayName("parseFractionalDistance should parse various formats")
        void testParseFractionalDistance(String input, double expected) {
            double result = parser.parseFractionalDistance(input);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("parseFraction should parse simple fraction")
        void testParseFraction() {
            double result = parser.parseFraction("3/4");
            assertThat(result).isEqualTo(0.75);
        }
    }

    // ==================== PRESENT WEATHER TESTS ====================

    @Nested
    @DisplayName("Present Weather Parsing Tests")
    class PresentWeatherTests {

        @BeforeEach
        void setUp() {
            parser.initializeSharedState();
            parser.weatherData = new TestWeatherData("TEST", Instant.now());
        }

        @Test
        @DisplayName("handlePresentWeather should parse light rain")
        void testHandlePresentWeather_LightRain() {
            String input = "-RA ";
            Matcher matcher = PRESENT_WEATHER_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handlePresentWeather(matcher);
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.presentWeather()).hasSize(1);
            PresentWeather weather = conditions.presentWeather().get(0);
            assertThat(weather.intensity()).isEqualTo("-");
            assertThat(weather.precipitation()).isEqualTo("RA");
        }

        @Test
        @DisplayName("handlePresentWeather should parse thunderstorm")
        void testHandlePresentWeather_Thunderstorm() {
            String input = "+TSRA ";
            Matcher matcher = PRESENT_WEATHER_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handlePresentWeather(matcher);
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.presentWeather()).hasSize(1);
            PresentWeather weather = conditions.presentWeather().get(0);
            assertThat(weather.intensity()).isEqualTo("+");
            assertThat(weather.descriptor()).isEqualTo("TS");
            assertThat(weather.precipitation()).isEqualTo("RA");
        }

        @Test
        @DisplayName("handlePresentWeather should parse mist")
        void testHandlePresentWeather_Mist() {
            String input = "BR ";
            Matcher matcher = PRESENT_WEATHER_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handlePresentWeather(matcher);
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.presentWeather()).hasSize(1);
            PresentWeather weather = conditions.presentWeather().get(0);
            assertThat(weather.obscuration()).isEqualTo("BR");
        }

        @Test
        @DisplayName("handlePresentWeather should handle multiple phenomena")
        void testHandlePresentWeather_Multiple() {
            parser.initializeSharedState();
            parser.weatherData = new TestWeatherData("TEST", Instant.now());

            // Add rain
            String input1 = "-RA ";
            Matcher matcher1 = PRESENT_WEATHER_PATTERN.matcher(input1);
            assertThat(matcher1.find()).isTrue();
            parser.handlePresentWeather(matcher1);

            // Add mist
            String input2 = "BR ";
            Matcher matcher2 = PRESENT_WEATHER_PATTERN.matcher(input2);
            assertThat(matcher2.find()).isTrue();
            parser.handlePresentWeather(matcher2);

            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.presentWeather()).hasSize(2);
        }
    }

    // ==================== SKY CONDITION TESTS ====================

    @Nested
    @DisplayName("Sky Condition Parsing Tests")
    class SkyConditionTests {

        @BeforeEach
        void setUp() {
            parser.initializeSharedState();
            parser.weatherData = new TestWeatherData("TEST", Instant.now());
        }

        @Test
        @DisplayName("handleSkyCondition should parse FEW clouds")
        void testHandleSkyCondition_Few() {
            String input = "FEW250 ";
            Matcher matcher = SKY_CONDITION_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handleSkyCondition(matcher);
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.skyConditions()).hasSize(1);
            SkyCondition sky = conditions.skyConditions().get(0);
            assertThat(sky.coverage()).isEqualTo(SkyCoverage.FEW);
            assertThat(sky.heightFeet()).isEqualTo(25000);
            assertThat(sky.cloudType()).isNull();
        }

        @Test
        @DisplayName("handleSkyCondition should parse with cloud type")
        void testHandleSkyCondition_WithCloudType() {
            String input = "BKN050CB ";
            Matcher matcher = SKY_CONDITION_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handleSkyCondition(matcher);
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.skyConditions()).hasSize(1);
            SkyCondition sky = conditions.skyConditions().get(0);
            assertThat(sky.coverage()).isEqualTo(SkyCoverage.BROKEN);
            assertThat(sky.heightFeet()).isEqualTo(5000);
            assertThat(sky.cloudType()).isEqualTo("CB");
        }

        @Test
        @DisplayName("handleSkyCondition should parse SKC (sky clear)")
        void testHandleSkyCondition_SkyClear() {
            String input = "SKC ";
            Matcher matcher = SKY_CONDITION_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handleSkyCondition(matcher);
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.skyConditions()).hasSize(1);
            SkyCondition sky = conditions.skyConditions().get(0);
            assertThat(sky.coverage()).isEqualTo(SkyCoverage.SKC);
            assertThat(sky.heightFeet()).isNull();
        }

        @Test
        @DisplayName("handleSkyCondition should handle multiple layers")
        void testHandleSkyCondition_MultipleLayers() {
            parser.initializeSharedState();
            parser.weatherData = new TestWeatherData("TEST", Instant.now());

            // Add FEW
            String input1 = "FEW100 ";
            Matcher matcher1 = SKY_CONDITION_PATTERN.matcher(input1);
            assertThat(matcher1.find()).isTrue();
            parser.handleSkyCondition(matcher1);

            // Add SCT
            String input2 = "SCT250 ";
            Matcher matcher2 = SKY_CONDITION_PATTERN.matcher(input2);
            assertThat(matcher2.find()).isTrue();
            parser.handleSkyCondition(matcher2);

            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.skyConditions()).hasSize(2);
        }

        @Test
        @DisplayName("handleSkyCondition should skip unknown coverage (///)")
        void testHandleSkyCondition_UnknownCoverage() {
            String input = "/// ";
            Matcher matcher = SKY_CONDITION_PATTERN.matcher(input);
            assertThat(matcher.find()).isTrue();

            parser.handleSkyCondition(matcher);
            WeatherConditions conditions = parser.buildConditions();

            // Unknown coverage should be skipped
            assertThat(conditions.skyConditions()).isEmpty();
        }
    }

    // ==================== COVERAGE PARSING TESTS ====================

    @Nested
    @DisplayName("Coverage Parsing Tests")
    class CoverageParsingTests {

        @ParameterizedTest
        @CsvSource({
                "SKC, SKC",
                "CLR, CLR",
                "NSC, NSC",
                "NCD, NSC",  // NCD treated as NSC
                "FEW, FEW",
                "SCT, SCATTERED",
                "BKN, BROKEN",
                "OVC, OVERCAST",
                "VV, VERTICAL_VISIBILITY",
                "0VC, OVERCAST",  // OCR error: 0→O
                "SCK, SKC"        // OCR error: K→C
        })
        @DisplayName("parseCoverage should handle various formats")
        void testParseCoverage(String input, String expectedName) {
            SkyCoverage result = parser.parseCoverage(input);
            assertThat(result.name()).isEqualTo(expectedName);
        }

        @Test
        @DisplayName("parseCoverage should throw exception for null")
        void testParseCoverage_Null() {
            assertThatThrownBy(() -> parser.parseCoverage(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }

        @Test
        @DisplayName("parseCoverage should throw exception for blank")
        void testParseCoverage_Blank() {
            assertThatThrownBy(() -> parser.parseCoverage(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or blank");
        }

        @Test
        @DisplayName("parseCoverage should throw exception for unknown coverage")
        void testParseCoverage_Unknown() {
            assertThatThrownBy(() -> parser.parseCoverage("XXX"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown sky coverage");
        }
    }

    // ==================== HEIGHT PARSING TESTS ====================

    @Nested
    @DisplayName("Height Parsing Tests")
    class HeightParsingTests {

        @Test
        @DisplayName("parseHeight should return null for clear sky")
        void testParseHeight_ClearSky() {
            Integer height = parser.parseHeight("100", SkyCoverage.SKC);
            assertThat(height).isNull();
        }

        @Test
        @DisplayName("parseHeight should parse normal height")
        void testParseHeight_Normal() {
            Integer height = parser.parseHeight("050", SkyCoverage.FEW);
            assertThat(height).isEqualTo(5000);
        }

        @Test
        @DisplayName("parseHeight should handle OCR error (O→0)")
        void testParseHeight_OcrError() {
            Integer height = parser.parseHeight("O5O", SkyCoverage.FEW);
            assertThat(height).isEqualTo(5000);
        }

        @Test
        @DisplayName("parseHeight should throw exception for VV without height")
        void testParseHeight_VerticalVisibilityRequiresHeight() {
            assertThatThrownBy(() -> parser.parseHeight(null, SkyCoverage.VERTICAL_VISIBILITY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must have height");
        }

        @Test
        @DisplayName("parseHeight should return null for /// (unknown)")
        void testParseHeight_Unknown() {
            Integer height = parser.parseHeight("///", SkyCoverage.FEW);
            assertThat(height).isNull();
        }

        @Test
        @DisplayName("parseHeight should throw exception for invalid format")
        void testParseHeight_Invalid() {
            assertThatThrownBy(() -> parser.parseHeight("ABC", SkyCoverage.FEW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid cloud height");
        }
    }

    // ==================== CLOUD TYPE PARSING TESTS ====================

    @Nested
    @DisplayName("Cloud Type Parsing Tests")
    class CloudTypeParsingTests {

        @Test
        @DisplayName("parseCloudType should parse CB")
        void testParseCloudType_CB() {
            String type = parser.parseCloudType("CB");
            assertThat(type).isEqualTo("CB");
        }

        @Test
        @DisplayName("parseCloudType should parse TCU")
        void testParseCloudType_TCU() {
            String type = parser.parseCloudType("TCU");
            assertThat(type).isEqualTo("TCU");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"///", "   "})
        @DisplayName("parseCloudType should return null for null, blank, or unknown values")
        void testParseCloudType_ReturnsNull(String input) {
            String type = parser.parseCloudType(input);
            assertThat(type).isNull();
        }
    }

    // ==================== BUILD CONDITIONS TESTS ====================

    @Nested
    @DisplayName("Build Conditions Tests")
    class BuildConditionsTests {

        @BeforeEach
        void setUp() {
            parser.initializeSharedState();
        }

        @Test
        @DisplayName("buildConditions should include present weather list")
        void testBuildConditions_WithPresentWeather() {
            parser.presentWeatherList.add(PresentWeather.parse("-RA"));
            parser.presentWeatherList.add(PresentWeather.parse("BR"));

            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.presentWeather()).hasSize(2);
        }

        @Test
        @DisplayName("buildConditions should include sky conditions list")
        void testBuildConditions_WithSkyConditions() {
            parser.skyConditionsList.add(new SkyCondition(SkyCoverage.FEW, 10000, null));
            parser.skyConditionsList.add(new SkyCondition(SkyCoverage.SCATTERED, 25000, null));

            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.skyConditions()).hasSize(2);
        }

        @Test
        @DisplayName("buildConditions should handle empty lists")
        void testBuildConditions_EmptyLists() {
            WeatherConditions conditions = parser.buildConditions();

            assertThat(conditions.presentWeather()).isEmpty();
            assertThat(conditions.skyConditions()).isEmpty();
        }
    }

    // ==================== TEST IMPLEMENTATION ====================

    /**
     * Concrete test implementation of NoaaAviationWeatherParser
     * that exposes protected methods for testing.
     */
    private static class TestAviationWeatherParser extends NoaaAviationWeatherParser<TestWeatherData> {

        @Override
        public ParseResult<NoaaWeatherData> parse(String rawData) {
            // Not needed for unit tests
            return ParseResult.success(weatherData);
        }

        @Override
        public boolean canParse(String rawData) {
            return true;
        }

        @Override
        public String getSourceType() {
            return "TEST";
        }

        // Expose protected methods for testing
        @Override
        public void initializeSharedState() {
            super.initializeSharedState();
        }

        @Override
        public void handleWind(Matcher matcher) {
            super.handleWind(matcher);
        }

        @Override
        public void handleVisibility(Matcher matcher) {
            super.handleVisibility(matcher);
        }

        @Override
        public void handlePresentWeather(Matcher matcher) {
            super.handlePresentWeather(matcher);
        }

        @Override
        public void handleSkyCondition(Matcher matcher) {
            super.handleSkyCondition(matcher);
        }

        @Override
        public double parseFractionalDistance(String distStr) {
            return super.parseFractionalDistance(distStr);
        }

        @Override
        public double parseFraction(String fraction) {
            return super.parseFraction(fraction);
        }

        @Override
        public SkyCoverage parseCoverage(String coverageStr) {
            return super.parseCoverage(coverageStr);
        }

        @Override
        public Integer parseHeight(String heightStr, SkyCoverage coverage) {
            return super.parseHeight(heightStr, coverage);
        }

        @Override
        public String parseCloudType(String cloudTypeStr) {
            return super.parseCloudType(cloudTypeStr);
        }

        @Override
        public WeatherConditions buildConditions() {
            return super.buildConditions();
        }
    }

    /**
     * Simple test weather data implementation.
     */
    private static class TestWeatherData extends NoaaWeatherData {
        public TestWeatherData(String stationId, Instant observationTime) {
            super(stationId, observationTime, "TEST");
        }

        @Override
        public boolean isCurrent() {
            return true;
        }

        @Override
        public String getDataType() {
            return "TEST";
        }

        @Override
        public String getSummary() {
            return "Test Weather Data";
        }
    }
}
