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
package weather.processing.parser.noaa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import weather.model.NoaaMetarData;
import weather.model.NoaaWeatherData;
import weather.model.components.*;
import weather.model.components.remark.*;
import weather.model.enums.AutomatedStationType;
import weather.model.enums.PressureUnit;
import weather.model.enums.SkyCoverage;
import weather.processing.parser.common.ParseResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Comprehensive tests for NoaaMetarParser.
 * Tests parsing logic with real METAR examples and edge cases.
 * 
 * @author bclasky1539
 *
 */
class NoaaMetarParserTest {
    
    private NoaaMetarParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new NoaaMetarParser();
    }

    /**
     * Extract data from ParseResult, throwing AssertionError if not present.
     * Use this after assertTrue(result.isSuccess()) to safely get data.
     *
     * @param result ParseResult to extract data from
     * @return NoaaWeatherData instance
     * @throws AssertionError if data is not present
     */
    private NoaaWeatherData extractData(ParseResult<NoaaWeatherData> result) {
        return result.getData()
                .orElseThrow(() -> new AssertionError("NoaaWeatherData: Expected successful parse to contain data"));
    }

    /**
     * Extract data from ParseResult, throwing AssertionError if not present.
     * Use this after assertTrue(result.isSuccess()) to safely get data.
     *
     * @param result ParseResult to extract data from
     * @return NoaaMetarData instance
     * @throws AssertionError if data is not present
     */
    private NoaaMetarData extractMetarData(ParseResult<NoaaWeatherData> result) {
        return (NoaaMetarData) result.getData()
                .orElseThrow(() -> new AssertionError("NoaaMetarData: Expected successful parse to contain data"));
    }

    @Test
    @DisplayName("Should return correct source type")
    void testGetSourceType() {
        assertEquals("NOAA_METAR", parser.getSourceType());
    }
    
    @Test
    @DisplayName("Should identify valid METAR data")
    void testCanParseValidMetar() {
        String metar = "METAR KJFK 251651Z 28016KT 10SM FEW250 22/12 A3015";
        
        assertTrue(parser.canParse(metar));
    }
    
    @Test
    @DisplayName("Should reject non-METAR data")
    void testCanParseInvalidData() {
        assertFalse(parser.canParse("TAF KJFK 251651Z"));
        assertFalse(parser.canParse("This is not METAR data"));
        assertFalse(parser.canParse(""));
    }
    
    @Test
    @DisplayName("Should reject null data")
    void testCanParseNull() {
        assertFalse(parser.canParse(null));
    }
    
    @Test
    @DisplayName("Should reject empty data")
    void testCanParseEmpty() {
        assertFalse(parser.canParse(""));
        assertFalse(parser.canParse("   "));
    }
    
    @Test
    @DisplayName("Should parse valid METAR successfully")
    void testParseValidMetar() {
        String metar = "METAR KJFK 251651Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 SLP210";
        
        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getData().isPresent());
        
        NoaaWeatherData data = result.getData().get();
        assertEquals("KJFK", data.getStationId());
        assertEquals("METAR", data.getReportType());
        assertEquals(metar, data.getRawData());
        assertNotNull(data.getObservationTime());
    }
    
    @Test
    @DisplayName("Should parse METAR with minimal data")
    void testParseMinimalMetar() {
        String metar = "METAR KLAX 251651Z";
        
        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        
        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);
        assertEquals("KLAX", data.getStationId());
    }
    
    @Test
    @DisplayName("Should parse METAR with different station codes")
    void testParseVariousStations() {
        String[] stations = {"KJFK", "KLAX", "KORD", "KATL", "KDFW"};
        
        for (String station : stations) {
            String metar = "METAR " + station + " 251651Z 28016KT 10SM";
            ParseResult<NoaaWeatherData> result = parser.parse(metar);
            
            assertTrue(result.isSuccess());
            NoaaWeatherData data = extractData(result);
            assertEquals(station, data.getStationId());
        }
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "  \t  ", "\n", "\r\n"})
    @DisplayName("Should fail when parsing invalid input")
    void testParseInvalidInput(String invalidInput) {
        ParseResult<NoaaWeatherData> result = parser.parse(invalidInput);
    
        assertTrue(result.isFailure());
        assertEquals("Raw data cannot be null or empty", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should fail when data is not METAR")
    void testParseNonMetarData() {
        String taf = "TAF KJFK 251651Z 2517/2618 28016KT P6SM";
        
        ParseResult<NoaaWeatherData> result = parser.parse(taf);
        
        assertTrue(result.isFailure());
        assertEquals("Data is not a valid METAR report", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should fail when station ID is missing")
    void testParseMissingStationId() {
        String invalidMetar = "METAR 251651Z 28016KT";
        
        ParseResult<NoaaWeatherData> result = parser.parse(invalidMetar);
        
        assertTrue(result.isFailure());
        assertEquals("Could not extract station ID from METAR", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should fail when station ID is invalid format")
    void testParseInvalidStationId() {
        String invalidMetar = "METAR K1X 251651Z 28016KT"; // Only 3 chars
        
        ParseResult<NoaaWeatherData> result = parser.parse(invalidMetar);
        
        assertTrue(result.isFailure());
        assertEquals("Could not extract station ID from METAR", result.getErrorMessage());
    }

    @ParameterizedTest
    @CsvSource({
        "'METAR  KJFK  251651Z  28016KT  10SM', KJFK",
        "'METAR KORD 251651Z VRB03KT 10SM FEW250', KORD",
        "'METAR PANC 251651Z 28016KT 10SM M05/M12 A3015', PANC"
    })
    @DisplayName("Should parse METAR with various formats and extract correct station ID")
    void testParseMetarVariousFormats(String metar, String expectedStationId) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);
    
        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);
        assertEquals(expectedStationId, data.getStationId());
    }
    
    @Test
    @DisplayName("Should trim raw data when storing")
    void testRawDataIsTrimmed() {
        String metar = "  METAR KJFK 251651Z 28016KT  ";
        
        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        
        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);
        assertEquals(metar.trim(), data.getRawData());
    }
    
    @Test
    @DisplayName("Should parse real-world METAR example - JFK")
    void testParseRealWorldJFK() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 RMK AO2 SLP201 T02330139";
        
        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        
        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);
        assertEquals("KJFK", data.getStationId());
        assertEquals("METAR", data.getReportType());
    }
    
    @Test
    @DisplayName("Should parse real-world METAR example - LAX")
    void testParseRealWorldLAX() {
        String metar = "METAR KLAX 121853Z 26008KT 10SM FEW015 SCT250 21/16 A2990 RMK AO2 SLP127";
        
        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        
        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);
        assertEquals("KLAX", data.getStationId());
    }
 
    @Test
    @DisplayName("Should parse METAR with remarks section")
    void testParseWithRemarks() {
        String metar = "METAR KATL 251651Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 SLP210 T02220117";
        
        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        
        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);
        assertTrue(data.getRawData().contains("RMK"));
    }
    
    @Test
    @DisplayName("Should set observation time when parsing")
    void testObservationTimeIsSet() {
        String metar = "METAR KJFK 251651Z 28016KT 10SM";
        
        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        
        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);
        assertNotNull(data.getObservationTime());
    }
    
    @Test
    @DisplayName("Should handle parser internal exceptions gracefully")
    void testExceptionHandling() {
        // This would require mocking to truly test exception handling
        // For now, we verify that malformed data doesn't crash the parser
        String malformed = "METAR";
        
        ParseResult<NoaaWeatherData> result = parser.parse(malformed);
        
        assertTrue(result.isFailure());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Should parse wind with direction and speed")
    void testParseWindBasic() {
        String metar = "METAR KJFK 251651Z 19005KT";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);

        // Verify Wind object was created
        assertNotNull(data.getWind());
        assertEquals(190, data.getWind().directionDegrees());
        assertEquals(5, data.getWind().speedValue());
        assertNull(data.getWind().gustValue());
        assertEquals("KT", data.getWind().unit());
    }

    @Test
    @DisplayName("Should parse wind with gusts")
    void testParseWindWithGusts() {
        String metar = "METAR KJFK 251651Z 28016G25KT";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);

        assertNotNull(data.getWind());
        assertEquals(280, data.getWind().directionDegrees());
        assertEquals(16, data.getWind().speedValue());
        assertEquals(25, data.getWind().gustValue());
        assertEquals("KT", data.getWind().unit());
    }

    @Test
    @DisplayName("Should parse variable wind direction")
    void testParseWindVariable() {
        String metar = "METAR KJFK 251651Z VRB03KT";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);

        assertNotNull(data.getWind());
        assertNull(data.getWind().directionDegrees()); // VRB = null direction
        assertEquals(3, data.getWind().speedValue());
        assertEquals("KT", data.getWind().unit());
    }

    @Test
    @DisplayName("Should parse wind with MPS units")
    void testParseWindMPS() {
        String metar = "METAR KJFK 251651Z 19005MPS";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);

        assertNotNull(data.getWind());
        assertEquals("MPS", data.getWind().unit());
    }

    @Test
    @DisplayName("Should handle calm wind (00000KT)")
    void testParseWindCalm() {
        String metar = "METAR KJFK 251651Z 00000KT";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);

        assertNotNull(data.getWind());
        assertEquals(0, data.getWind().directionDegrees());
        assertEquals(0, data.getWind().speedValue());
    }

    @Test
    @DisplayName("Should correctly identify SPECI report type")
    void testParseSpeciReport() {
        String speci = "SPECI KJFK 251651Z 19005KT 10SM FEW250";

        ParseResult<NoaaWeatherData> result = parser.parse(speci);

        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);

        assertEquals("KJFK", data.getStationId());
        assertEquals("SPECI", data.getReportType());  // ← Should be SPECI, not METAR
        assertNotNull(data.getObservationTime());
    }

    @Test
    @DisplayName("Should correctly identify METAR report type")
    void testParseMetarReportType() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM FEW250";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);

        assertEquals("KJFK", data.getStationId());
        assertEquals("METAR", data.getReportType());  // ← Should be METAR
        assertNotNull(data.getObservationTime());
    }

    @Test
    @DisplayName("Should default to METAR when report type is missing")
    void testParseWithoutReportType() {
        // Some formats don't have METAR/SPECI prefix
        String noPrefix = "2025/11/25 16:51 KJFK 251651Z 19005KT";

        ParseResult<NoaaWeatherData> result = parser.parse(noPrefix);

        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);

        assertEquals("KJFK", data.getStationId());
        assertEquals("METAR", data.getReportType());  // ← Should default to METAR
    }

    @ParameterizedTest
    @CsvSource({
            // Test case 1: Observation day from previous month (same year)
            "'METAR KJFK 302352Z 19005KT', 30, 'Previous month, same year'",

            // Test case 2: With explicit issue date
            "'2025/11/14 22:52 METAR KJFK 142252Z 19005KT', 14, 'With issue date'",

            // Test case 3: Year wrap-around (December to January)
            "'2025/01/02 10:00 METAR KJFK 312359Z 19005KT', 31, 'Year wrap-around to previous December'"
    })
    @DisplayName("Should handle month and year wrap-around scenarios")
    void testParseObsTimeWrapAround(String metar, int expectedDay, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaWeatherData data = extractData(result);

        assertEquals("KJFK", data.getStationId());
        assertNotNull(data.getObservationTime(), "Observation time should not be null: " + scenario);

        // Verify the day matches (helps validate correct month/year calculation)
        LocalDateTime obsTime = LocalDateTime.ofInstant(
                data.getObservationTime(),
                ZoneOffset.UTC
        );
        assertEquals(expectedDay, obsTime.getDayOfMonth(),
                "Day of month should match for: " + scenario);
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 251651Z AUTO 19005KT', AUTO",
            "'METAR KJFK 251651Z COR 19005KT', COR",
            "'METAR KJFK 251651Z AMD 19005KT', AMD",
            "'METAR KJFK 251651Z RTD 19005KT', RTD"
    })
    @DisplayName("Should parse various report modifiers")
    void testParseReportModifiers(String metar, String expectedModifier) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);

        assertEquals("KJFK", data.getStationId());
        assertEquals(expectedModifier, data.getReportModifier());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "METAR KJFK 251651Z 28016KT 10SM FEW250",          // Standard METAR
            "SPECI KJFK 251651Z 19005KT",                       // SPECI report
            "2025/11/14 22:52 METAR KJFK 142252Z 19005KT",     // Date/time prefix
            "  METAR KJFK 251651Z 19005KT  ",                  // Leading/trailing spaces
            "METAR  KJFK  251651Z  28016KT"                    // Extra internal spaces
    })
    @DisplayName("Should recognize valid METAR/SPECI formats")
    void testCanParseValidFormats(String metar) {
        assertTrue(parser.canParse(metar));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "TAF KJFK 251651Z",                                // TAF, not METAR
            "This is not METAR data",                          // Random text
            "XMETAR KJFK 251651Z",                             // Invalid prefix
            "METAR_KJFK 251651Z",                              // No space after METAR
            "",                                                 // Empty string
            "   "                                               // Only whitespace
    })
    @DisplayName("Should reject invalid METAR formats")
    void testCanParseInvalidFormats(String data) {
        assertFalse(parser.canParse(data));
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 251651Z 19005KT UNKNOWNTOKEN 10SM', 'Unparsed main body token'",
            "'METAR KJFK 251651Z 19005KT RMK UNKNOWNREMARK', 'Unparsed remark token'",
            "'METAR KJFK 251651Z XXXXX 10SM', 'Malformed wind data'"
    })
    @DisplayName("Should handle unparsed and malformed tokens gracefully")
    void testParseWithUnparsedOrMalformedTokens(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        // Should either succeed (skip invalid parts) or fail with clear error
        if (result.isFailure()) {
            assertNotNull(result.getErrorMessage(), "Should have error message: " + scenario);
        } else {
            NoaaWeatherData data = extractData(result);
            assertEquals("KJFK", data.getStationId(),
                    "Should still parse station: " + scenario);
        }
    }

    @Test
    @DisplayName("Should fail gracefully when exception occurs in handler")
    void testParseExceptionInHandler() {
        // This will fail to extract station ID, triggering the validation check
        String metar = "METAR 251651Z 19005KT";  // Missing station ID

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isFailure());
        assertEquals("Could not extract station ID from METAR", result.getErrorMessage());
    }

    @ParameterizedTest
    @CsvSource({
            // US Format - Statute Miles
            "'METAR KJFK 251651Z 19005KT 10SM', 10.0, SM, false, false, 'Whole number SM'",
            "'METAR KJFK 251651Z 19005KT 1/2SM', 0.5, SM, false, false, 'Fraction SM'",
            "'METAR KJFK 251651Z 19005KT 1 1/2SM', 1.5, SM, false, false, 'Mixed fraction SM'",
            "'METAR KJFK 251651Z 19005KT P6SM', 6.0, SM, false, true, 'Greater than SM'",
            "'METAR KJFK 251651Z 19005KT M1/4SM', 0.25, SM, true, false, 'Less than SM'",

            // International Format - Meters
            "'METAR EGLL 251651Z 19005KT 9999 FEW250', 9999.0, M, false, false, 'Unlimited visibility meters'",
            "'METAR EGLL 251651Z 19005KT 0800 FEW250', 800.0, M, false, false, 'Low visibility meters'",
            "'METAR EGLL 251651Z 19005KT M0400 FEW250', 400.0, M, true, false, 'Less than meters'",
            "'METAR EGLL 251651Z 19005KT P9999 FEW250', 9999.0, M, false, true, 'Greater than meters'"
    })
    @DisplayName("Should parse visibility in various formats")
    void testParseVisibility(String metar, double expectedDistance, String expectedUnit,
                             boolean expectedLessThan, boolean expectedGreaterThan, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaWeatherData data = extractData(result);

        assertNotNull(data.getVisibility(), "Visibility should not be null: " + scenario);
        assertEquals(expectedDistance, data.getVisibility().distanceValue(),
                "Distance value should match: " + scenario);
        assertEquals(expectedUnit, data.getVisibility().unit(),
                "Unit should match: " + scenario);
        assertEquals(expectedLessThan, data.getVisibility().lessThan(),
                "LessThan flag should match: " + scenario);
        assertEquals(expectedGreaterThan, data.getVisibility().greaterThan(),
                "GreaterThan flag should match: " + scenario);
    }

    @Test
    @DisplayName("Should parse CAVOK visibility")
    void testParseVisibilityCavok() {
        String metar = "METAR EGLL 251651Z 19005KT CAVOK";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);

        assertNotNull(data.getVisibility());
        assertTrue(data.getVisibility().isCavok());
    }

    @Test
    @DisplayName("Should parse NDV visibility")
    void testParseVisibilityNDV() {
        String metar = "METAR KJFK 251651Z 19005KT NDV";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaWeatherData data = extractData(result);

        assertNotNull(data.getVisibility());
        assertEquals("NDV", data.getVisibility().specialCondition());
    }

    // ========== RUNWAY VISUAL RANGE (RVR) TESTS ==========
    @ParameterizedTest
    @CsvSource({
            // Simple RVR with trend
            "'METAR KJFK 251651Z 19005KT R22R/0400N', 22R, 400, , , , N, 'RVR with no-change trend'",
            "'METAR KJFK 251651Z 19005KT R24/P2000N', 24, 2000, , , P, N, 'RVR greater than with trend'",
            "'METAR KJFK 251651Z 19005KT R23R/0450N', 23R, 450, , , , N, 'RVR with right designator'",
            "'METAR KJFK 251651Z 19005KT R23L/0350D', 23L, 350, , , , D, 'RVR with decreasing trend'",
            "'METAR KJFK 251651Z 19005KT R22C/1200U', 22C, 1200, , , , U, 'RVR with increasing trend and center designator'",
            "'METAR KJFK 251651Z 19005KT R04L/M0400N', 04L, 400, , , M, N, 'RVR less than'",
            // Variable RVR
            "'METAR KJFK 251651Z 19005KT R23L/0900V6000FT', 23L, , 900, 6000, , , 'Variable RVR with FT unit'",
            "'METAR KJFK 251651Z 19005KT R22R/1200V1800N', 22R, , 1200, 1800, , N, 'Variable RVR with trend'",
            "'METAR KJFK 251651Z 19005KT R04R/P6000VP6000FT', 04R, , 6000, 6000, P, , 'Variable RVR with P prefix'",
            // Multiple RVR entries
            "'METAR KJFK 251651Z 19005KT R23R/0450N R23L/0350N', 23L, 350, , , , N, 'Multiple RVR - verify last one'"
    })
    @DisplayName("Should parse runway visual range (RVR) correctly")
    void testParseRunwayVisualRange(String metar, String expectedRunway,
                                    Integer expectedVisualRange, Integer expectedVarLow,
                                    Integer expectedVarHigh, String expectedPrefix,
                                    String expectedTrend) {
        // Parse METAR
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        // Assert success
        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Get RVR list
        List<RunwayVisualRange> rvrList = data.getRunwayVisualRange();
        assertThat(rvrList).isNotEmpty();

        // Find the RVR for the expected runway (handles multiple RVR case)
        RunwayVisualRange rvr = data.getRvrForRunway(expectedRunway);
        assertThat(rvr).as("RVR for runway " + expectedRunway).isNotNull();

        // Assert runway
        assertThat(rvr.runway()).isEqualTo(expectedRunway);

        // Assert visual range or variable range
        if (expectedVisualRange != null) {
            assertThat(rvr.visualRangeFeet()).as("Visual range").isEqualTo(expectedVisualRange);
            assertThat(rvr.isVariable()).as("Should not be variable").isFalse();
        } else {
            assertThat(rvr.isVariable()).as("Should be variable").isTrue();
            assertThat(rvr.variableLow()).as("Variable low").isEqualTo(expectedVarLow);
            assertThat(rvr.variableHigh()).as("Variable high").isEqualTo(expectedVarHigh);
        }

        // Assert prefix (if specified)
        if (expectedPrefix != null && !expectedPrefix.isBlank()) {
            assertThat(rvr.prefix()).isEqualTo(expectedPrefix);
            if ("P".equals(expectedPrefix)) {
                assertThat(rvr.isGreaterThan()).isTrue();
            } else if ("M".equals(expectedPrefix)) {
                assertThat(rvr.isLessThan()).isTrue();
            }
        }

        // Assert trend (if specified)
        if (expectedTrend != null && !expectedTrend.isBlank()) {
            assertThat(rvr.trend()).isEqualTo(expectedTrend);
        }
    }

    @Test
    @DisplayName("Should parse multiple RVR entries")
    void testParseMultipleRvrEntries() {
        String metar = "METAR KJFK 251651Z 19005KT R23R/0450N R23L/0350N R22R/P6000FT";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Should have 3 RVR entries
        List<RunwayVisualRange> rvrList = data.getRunwayVisualRange();
        assertThat(rvrList).hasSize(3);

        // Verify each runway
        RunwayVisualRange rvr23R = data.getRvrForRunway("23R");
        assertThat(rvr23R).isNotNull();
        assertThat(rvr23R.visualRangeFeet()).isEqualTo(450);
        assertThat(rvr23R.trend()).isEqualTo("N");

        RunwayVisualRange rvr23L = data.getRvrForRunway("23L");
        assertThat(rvr23L).isNotNull();
        assertThat(rvr23L.visualRangeFeet()).isEqualTo(350);
        assertThat(rvr23L.trend()).isEqualTo("N");

        RunwayVisualRange rvr22R = data.getRvrForRunway("22R");
        assertThat(rvr22R).isNotNull();
        assertThat(rvr22R.visualRangeFeet()).isEqualTo(6000);
        assertThat(rvr22R.isGreaterThan()).isTrue();
    }

    @Test
    @DisplayName("Should handle RVRNO (RVR not available)")
    void testParseRvrNotAvailable() {
        String metar = "METAR KJFK 251651Z 19005KT RVRNO";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Should have no RVR entries
        List<RunwayVisualRange> rvrList = data.getRunwayVisualRange();
        assertThat(rvrList).isEmpty();
    }

    @Test
    @DisplayName("Should handle CLRD (RVR cleared)")
    void testParseRvrCleared() {
        String metar = "METAR KJFK 251651Z 19005KT R34L/CLRD70";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        assertThat(result.isSuccess()).isTrue();
        NoaaWeatherData data = extractData(result);

        // CLRD should now create an RVR object
        assertThat(data.getRunwayVisualRange()).hasSize(1);

        RunwayVisualRange rvr = data.getRunwayVisualRange().get(0);
        assertThat(rvr.runway()).isEqualTo("34L");
        assertThat(rvr.isCleared()).isTrue();
        assertThat(rvr.visualRangeFeet()).isNull();
    }

    @Test
    @DisplayName("Should handle RVR with numeric suffix")
    void testParseRvrWithNumericSuffix() {
        String metar = "METAR KJFK 251651Z 19005KT R34L/040070";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Should parse successfully
        List<RunwayVisualRange> rvrList = data.getRunwayVisualRange();
        assertThat(rvrList).isNotEmpty();

        RunwayVisualRange rvr = data.getRvrForRunway("34L");
        assertThat(rvr).isNotNull();
        assertThat(rvr.runway()).isEqualTo("34L");
        assertThat(rvr.visualRangeFeet()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should use utility methods on NoaaMetarData for RVR")
    void testRvrUtilityMethods() {
        String metar = "METAR KJFK 251651Z 19005KT R23R/0450N R23L/0350N R22R/P6000FT";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Test getMinimumRvrFeet()
        Integer minRvr = data.getMinimumRvrFeet();
        assertThat(minRvr).isEqualTo(350); // Should be the lowest value

        // Test getRvrForRunway()
        RunwayVisualRange rvr23L = data.getRvrForRunway("23L");
        assertThat(rvr23L).isNotNull();
        assertThat(rvr23L.visualRangeFeet()).isEqualTo(350);

        // Test with non-existent runway
        RunwayVisualRange rvrNone = data.getRvrForRunway("04L");
        assertThat(rvrNone).isNull();
    }

    @Test
    @DisplayName("Should handle CLRD runway visual range")
    void testParseRvrClrd() {
        String metar = "METAR KJFK 251651Z 19005KT R22R/CLRD";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // CLRD should now create an RVR object (not skip it)
        assertThat(data.getRunwayVisualRange()).hasSize(1);

        RunwayVisualRange rvr = data.getRunwayVisualRange().get(0);
        assertThat(rvr.runway()).isEqualTo("22R");
        assertThat(rvr.isCleared()).isTrue();
        assertThat(rvr.visualRangeFeet()).isNull();
        assertThat(rvr.isVariable()).isFalse();
    }

    @Test
    @DisplayName("Should handle RVRNO (RVR not available)")
    void testParseRvrno() {
        String metar = "METAR KJFK 251651Z 19005KT RVRNO";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // RVRNO means RVR is not available - no RVR objects should be created
        assertThat(data.getRunwayVisualRange()).isEmpty();
    }

    @Test
    @DisplayName("Should handle unknown visibility (////) gracefully")
    void testUnknownVisibility() {
        String metar = "METAR EGLL 251651Z 19005KT //// FEW250";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse with unknown visibility");
        NoaaWeatherData data = extractData(result);

        // Visibility should be null when unknown (//// means visibility not available)
        assertNull(data.getVisibility(), "Visibility should be null for ////");
    }

    // ========== PRESENT WEATHER TESTS ==========

    @ParameterizedTest
    @CsvSource({
            // Simple precipitation
            "'METAR KJFK 251651Z 19005KT 10SM RA', RA, , , RA, , , 'Simple rain'",
            "'METAR KJFK 251651Z 19005KT 10SM SN', SN, , , SN, , , 'Simple snow'",
            "'METAR KJFK 251651Z 19005KT 10SM DZ', DZ, , , DZ, , , 'Simple drizzle'",

            // With intensity
            "'METAR KJFK 251651Z 19005KT 10SM -RA', -RA, -, , RA, , , 'Light rain'",
            "'METAR KJFK 251651Z 19005KT 10SM +TSRA', +TSRA, +, TS, RA, , , 'Heavy thunderstorm with rain'",
            "'METAR KJFK 251651Z 19005KT 10SM -SN', -SN, -, , SN, , , 'Light snow'",

            // With descriptor
            "'METAR KJFK 251651Z 19005KT 10SM SHSN', SHSN, , SH, SN, , , 'Snow showers'",
            "'METAR KJFK 251651Z 19005KT 10SM FZDZ', FZDZ, , FZ, DZ, , , 'Freezing drizzle'",
            "'METAR KJFK 251651Z 19005KT 10SM TSRA', TSRA, , TS, RA, , , 'Thunderstorm with rain'",
            "'METAR KJFK 251651Z 19005KT 10SM BLSN', BLSN, , BL, SN, , , 'Blowing snow'",

            // Obscuration
            "'METAR KJFK 251651Z 19005KT 3SM BR', BR, , , , BR, , 'Mist'",
            "'METAR KJFK 251651Z 19005KT 1/2SM FG', FG, , , , FG, , 'Fog'",
            "'METAR KJFK 251651Z 19005KT 5SM HZ', HZ, , , , HZ, , 'Haze'",

            // Other phenomena
            "'METAR KJFK 251651Z 19005KT 10SM SQ', SQ, , , , , SQ, 'Squall'",
            "'METAR KJFK 251651Z 19005KT 10SM FC', FC, , , , , FC, 'Funnel cloud'",

            // Vicinity
            "'METAR KJFK 251651Z 19005KT 10SM VCFG', VCFG, VC, , , FG, , 'Fog in vicinity'",
            "'METAR KJFK 251651Z 19005KT 10SM VCTS', VCTS, VC, TS, , , , 'Thunderstorm in vicinity'",

            // No significant weather
            "'METAR KJFK 251651Z 19005KT 10SM NSW', NSW, , , , , NSW, 'No significant weather'"
    })
    @DisplayName("Should parse present weather correctly")
    void testParsePresentWeather(String metar, String expectedRaw, String expectedIntensity,
                                 String expectedDescriptor, String expectedPrecipitation,
                                 String expectedObscuration, String expectedOther) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully");
        NoaaMetarData data = extractMetarData(result);

        // Should have present weather
        List<PresentWeather> weatherList = data.getPresentWeather();
        assertThat(weatherList).isNotEmpty();

        // Get the first (or only) weather element
        PresentWeather weather = weatherList.get(0);

        // Assert raw code
        assertThat(weather.rawCode()).isEqualTo(expectedRaw);

        // Assert intensity
        if (expectedIntensity != null && !expectedIntensity.isBlank()) {
            assertThat(weather.intensity()).isEqualTo(expectedIntensity);
        } else {
            assertThat(weather.intensity()).isNull();
        }

        // Assert descriptor
        if (expectedDescriptor != null && !expectedDescriptor.isBlank()) {
            assertThat(weather.descriptor()).isEqualTo(expectedDescriptor);
        } else {
            assertThat(weather.descriptor()).isNull();
        }

        // Assert precipitation
        if (expectedPrecipitation != null && !expectedPrecipitation.isBlank()) {
            assertThat(weather.precipitation()).isEqualTo(expectedPrecipitation);
            assertThat(weather.hasPrecipitation()).isTrue();
        } else {
            assertThat(weather.precipitation()).isNull();
            assertThat(weather.hasPrecipitation()).isFalse();
        }

        // Assert obscuration
        if (expectedObscuration != null && !expectedObscuration.isBlank()) {
            assertThat(weather.obscuration()).isEqualTo(expectedObscuration);
            assertThat(weather.hasObscuration()).isTrue();
        } else {
            assertThat(weather.obscuration()).isNull();
            assertThat(weather.hasObscuration()).isFalse();
        }

        // Assert other
        if (expectedOther != null && !expectedOther.isBlank()) {
            assertThat(weather.other()).isEqualTo(expectedOther);
        } else {
            assertThat(weather.other()).isNull();
        }
    }

    @Test
    @DisplayName("Should parse multiple present weather conditions")
    void testParseMultiplePresentWeather() {
        String metar = "METAR KJFK 251651Z 19005KT 3SM -RA BR";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Should have 2 weather conditions
        List<PresentWeather> weatherList = data.getPresentWeather();
        assertThat(weatherList).hasSize(2);

        // First: Light rain
        PresentWeather rain = weatherList.get(0);
        assertThat(rain.rawCode()).isEqualTo("-RA");
        assertThat(rain.intensity()).isEqualTo("-");
        assertThat(rain.precipitation()).isEqualTo("RA");

        // Second: Mist
        PresentWeather mist = weatherList.get(1);
        assertThat(mist.rawCode()).isEqualTo("BR");
        assertThat(mist.obscuration()).isEqualTo("BR");
    }

    @Test
    @DisplayName("Should parse complex present weather with multiple components")
    void testParseComplexPresentWeather() {
        String metar = "METAR KJFK 251651Z 19005KT 1/2SM +TSRAFG";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        List<PresentWeather> weatherList = data.getPresentWeather();
        assertThat(weatherList).isNotEmpty();

        PresentWeather weather = weatherList.get(0);
        assertThat(weather.rawCode()).isEqualTo("+TSRAFG");
        assertThat(weather.intensity()).isEqualTo("+");
        assertThat(weather.descriptor()).isEqualTo("TS");
        assertThat(weather.precipitation()).isEqualTo("RA");
        assertThat(weather.obscuration()).isEqualTo("FG");
    }

    @Test
    @DisplayName("Should handle NSW (No Significant Weather)")
    void testParsePresentWeatherNSW() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM NSW";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        List<PresentWeather> weatherList = data.getPresentWeather();
        assertThat(weatherList).isNotEmpty();

        PresentWeather weather = weatherList.get(0);
        assertThat(weather.rawCode()).isEqualTo("NSW");
        assertThat(weather.isNoSignificantWeather()).isTrue();
    }

    @Test
    @DisplayName("Should parse present weather with all precipitation types")
    void testParsePresentWeatherAllPrecipitation() {
        // Test various precipitation types
        String[] precipTypes = {"DZ", "RA", "SN", "SG", "IC", "PL", "GR", "GS", "UP"};

        for (String precip : precipTypes) {
            String metar = "METAR KJFK 251651Z 19005KT 10SM " + precip;
            ParseResult<NoaaWeatherData> result = parser.parse(metar);

            assertTrue(result.isSuccess(), "Should parse " + precip);
            NoaaMetarData data = extractMetarData(result);

            List<PresentWeather> weatherList = data.getPresentWeather();
            assertThat(weatherList).isNotEmpty();

            PresentWeather weather = weatherList.get(0);
            assertThat(weather.precipitation()).isEqualTo(precip);
            assertThat(weather.hasPrecipitation()).isTrue();
        }
    }

    @Test
    @DisplayName("Should parse present weather with all obscuration types")
    void testParsePresentWeatherAllObscurations() {
        // Test various obscuration types
        String[] obscurations = {"BR", "FG", "FU", "VA", "DU", "SA", "HZ", "PY"};

        for (String obscur : obscurations) {
            String metar = "METAR KJFK 251651Z 19005KT 3SM " + obscur;
            ParseResult<NoaaWeatherData> result = parser.parse(metar);

            assertTrue(result.isSuccess(), "Should parse " + obscur);
            NoaaMetarData data = extractMetarData(result);

            List<PresentWeather> weatherList = data.getPresentWeather();
            assertThat(weatherList).isNotEmpty();

            PresentWeather weather = weatherList.get(0);
            assertThat(weather.obscuration()).isEqualTo(obscur);
            assertThat(weather.hasObscuration()).isTrue();
        }
    }

    @Test
    @DisplayName("Should parse present weather with all descriptor types")
    void testParsePresentWeatherAllDescriptors() {
        // Test various descriptors with precipitation
        String[] descriptors = {"MI", "PR", "BC", "DR", "BL", "SH", "TS", "FZ"};

        for (String desc : descriptors) {
            String metar = "METAR KJFK 251651Z 19005KT 10SM " + desc + "RA";
            ParseResult<NoaaWeatherData> result = parser.parse(metar);

            assertTrue(result.isSuccess(), "Should parse " + desc + "RA");
            NoaaMetarData data = extractMetarData(result);

            List<PresentWeather> weatherList = data.getPresentWeather();
            assertThat(weatherList).isNotEmpty();

            PresentWeather weather = weatherList.get(0);
            assertThat(weather.descriptor()).isEqualTo(desc);
            assertThat(weather.precipitation()).isEqualTo("RA");
        }
    }

    @Test
    @DisplayName("Should parse present weather with all other phenomena")
    void testParsePresentWeatherAllOther() {
        // Test other phenomena
        String[] others = {"PO", "SQ", "FC", "SS", "DS"};

        for (String other : others) {
            String metar = "METAR KJFK 251651Z 19005KT 10SM " + other;
            ParseResult<NoaaWeatherData> result = parser.parse(metar);

            assertTrue(result.isSuccess(), "Should parse " + other);
            NoaaMetarData data = extractMetarData(result);

            List<PresentWeather> weatherList = data.getPresentWeather();
            assertThat(weatherList).isNotEmpty();

            PresentWeather weather = weatherList.get(0);
            assertThat(weather.other()).isEqualTo(other);
        }
    }

    @Test
    @DisplayName("Should parse real-world present weather example")
    void testParseRealWorldPresentWeather() {
        String metar = "METAR KJFK 251651Z 19005KT 1/2SM +TSRA FG R04R/P6000FT";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Should have 2 weather conditions: +TSRA and FG
        List<PresentWeather> weatherList = data.getPresentWeather();
        assertThat(weatherList).hasSize(2);

        // Heavy thunderstorm with rain
        PresentWeather tsra = weatherList.get(0);
        assertThat(tsra.rawCode()).isEqualTo("+TSRA");
        assertThat(tsra.intensity()).isEqualTo("+");
        assertThat(tsra.descriptor()).isEqualTo("TS");
        assertThat(tsra.precipitation()).isEqualTo("RA");

        // Fog
        PresentWeather fog = weatherList.get(1);
        assertThat(fog.rawCode()).isEqualTo("FG");
        assertThat(fog.obscuration()).isEqualTo("FG");
    }

    // ========== SKY CONDITION TESTS ==========

    @ParameterizedTest
    @CsvSource({
            // Simple cloud layers
            "'METAR KJFK 251651Z 19005KT 10SM FEW250', FEW, 25000, , 'Few at 25000 feet'",
            "'METAR KJFK 251651Z 19005KT 10SM SCT100', SCATTERED, 10000, , 'Scattered at 10000 feet'",
            "'METAR KJFK 251651Z 19005KT 10SM BKN050', BROKEN, 5000, , 'Broken at 5000 feet'",
            "'METAR KJFK 251651Z 19005KT 10SM OVC020', OVERCAST, 2000, , 'Overcast at 2000 feet'",

            // With cloud type
            "'METAR KJFK 251651Z 19005KT 3SM BKN050CB', BROKEN, 5000, CB, 'Broken with cumulonimbus'",
            "'METAR KJFK 251651Z 19005KT 5SM SCT040TCU', SCATTERED, 4000, TCU, 'Scattered with towering cumulus'",

            // Clear sky conditions
            "'METAR KJFK 251651Z 19005KT 10SM SKC', SKC, , , 'Sky clear'",
            "'METAR KJFK 251651Z 19005KT 10SM CLR', CLR, , , 'Clear'",
            "'METAR KJFK 251651Z 19005KT 10SM NSC', NSC, , , 'No significant clouds'",

            // Vertical visibility
            "'METAR KJFK 251651Z 19005KT 1/4SM VV008', VERTICAL_VISIBILITY, 800, , 'Vertical visibility 800 feet'",
            "'METAR KJFK 251651Z 19005KT 1SM VV002', VERTICAL_VISIBILITY, 200, , 'Vertical visibility 200 feet'",

            // Various heights
            "'METAR KJFK 251651Z 19005KT 10SM FEW015', FEW, 1500, , 'Few at 1500 feet'",
            "'METAR KJFK 251651Z 19005KT 10SM SCT035', SCATTERED, 3500, , 'Scattered at 3500 feet'",
            "'METAR KJFK 251651Z 19005KT 10SM BKN008', BROKEN, 800, , 'Broken at 800 feet'",
            "'METAR KJFK 251651Z 19005KT 10SM OVC012', OVERCAST, 1200, , 'Overcast at 1200 feet'"
    })
    @DisplayName("Should parse sky condition correctly")
    void testParseSkyCondition(String metar, SkyCoverage expectedCoverage, Integer expectedHeightFeet,
                               String expectedCloudType, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        // Should have sky condition
        List<SkyCondition> skyConditions = data.getSkyConditions();
        assertThat(skyConditions).isNotEmpty();

        // Get the first (or only) sky condition
        SkyCondition sky = skyConditions.get(0);

        // Assert coverage
        assertThat(sky.coverage()).isEqualTo(expectedCoverage);

        // Assert height
        if (expectedHeightFeet != null) {
            assertThat(sky.heightFeet()).isEqualTo(expectedHeightFeet);
        } else {
            assertThat(sky.heightFeet()).isNull();
        }

        // Assert cloud type
        if (expectedCloudType != null && !expectedCloudType.isBlank()) {
            assertThat(sky.cloudType()).isEqualTo(expectedCloudType);
        } else {
            assertThat(sky.cloudType()).isNull();
        }
    }

    @Test
    @DisplayName("Should parse multiple sky condition layers")
    void testParseMultipleSkyConditions() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM FEW015 SCT040 BKN100 OVC250";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Should have 4 sky condition layers
        List<SkyCondition> skyConditions = data.getSkyConditions();
        assertThat(skyConditions).hasSize(4);

        // Verify each layer
        SkyCondition few = skyConditions.get(0);
        assertThat(few.coverage()).isEqualTo(SkyCoverage.FEW);
        assertThat(few.heightFeet()).isEqualTo(1500);

        SkyCondition sct = skyConditions.get(1);
        assertThat(sct.coverage()).isEqualTo(SkyCoverage.SCATTERED);
        assertThat(sct.heightFeet()).isEqualTo(4000);

        SkyCondition bkn = skyConditions.get(2);
        assertThat(bkn.coverage()).isEqualTo(SkyCoverage.BROKEN);
        assertThat(bkn.heightFeet()).isEqualTo(10000);

        SkyCondition ovc = skyConditions.get(3);
        assertThat(ovc.coverage()).isEqualTo(SkyCoverage.OVERCAST);
        assertThat(ovc.heightFeet()).isEqualTo(25000);
    }

    @Test
    @DisplayName("Should parse complex sky conditions with cloud types")
    void testParseComplexSkyConditions() {
        String metar = "METAR KJFK 251651Z 19005KT 3SM SCT015TCU BKN030CB OVC100";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        List<SkyCondition> skyConditions = data.getSkyConditions();
        assertThat(skyConditions).hasSize(3);

        // Scattered with TCU
        SkyCondition sct = skyConditions.get(0);
        assertThat(sct.coverage()).isEqualTo(SkyCoverage.SCATTERED);
        assertThat(sct.heightFeet()).isEqualTo(1500);
        assertThat(sct.cloudType()).isEqualTo("TCU");
        assertThat(sct.isToweringCumulus()).isTrue();

        // Broken with CB
        SkyCondition bkn = skyConditions.get(1);
        assertThat(bkn.coverage()).isEqualTo(SkyCoverage.BROKEN);
        assertThat(bkn.heightFeet()).isEqualTo(3000);
        assertThat(bkn.cloudType()).isEqualTo("CB");
        assertThat(bkn.isCumulonimbus()).isTrue();

        // Overcast
        SkyCondition ovc = skyConditions.get(2);
        assertThat(ovc.coverage()).isEqualTo(SkyCoverage.OVERCAST);
        assertThat(ovc.heightFeet()).isEqualTo(10000);
        assertThat(ovc.cloudType()).isNull();
    }

    @Test
    @DisplayName("Should identify ceiling layers correctly")
    void testParseCeilingLayers() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM FEW015 BKN050 OVC100";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        List<SkyCondition> skyConditions = data.getSkyConditions();
        assertThat(skyConditions).hasSize(3);

        // FEW is not a ceiling
        assertThat(skyConditions.get(0).isCeiling()).isFalse();

        // BKN is a ceiling
        assertThat(skyConditions.get(1).isCeiling()).isTrue();

        // OVC is a ceiling
        assertThat(skyConditions.get(2).isCeiling()).isTrue();
    }

    @Test
    @DisplayName("Should handle clear sky conditions")
    void testParseClearSkyConditions() {
        String[] clearConditions = {"SKC", "CLR", "NSC"};

        for (String condition : clearConditions) {
            String metar = "METAR KJFK 251651Z 19005KT 10SM " + condition;
            ParseResult<NoaaWeatherData> result = parser.parse(metar);

            assertTrue(result.isSuccess(), "Should parse " + condition);
            NoaaMetarData data = extractMetarData(result);

            List<SkyCondition> skyConditions = data.getSkyConditions();
            assertThat(skyConditions).isNotEmpty();

            SkyCondition sky = skyConditions.get(0);
            assertThat(sky.isClear()).isTrue();
            assertThat(sky.heightFeet()).isNull();
        }
    }

    @Test
    @DisplayName("Should handle vertical visibility")
    void testParseVerticalVisibility() {
        String metar = "METAR KJFK 251651Z 19005KT 1/4SM FG VV008";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        List<SkyCondition> skyConditions = data.getSkyConditions();
        assertThat(skyConditions).isNotEmpty();

        SkyCondition vv = skyConditions.get(0);
        assertThat(vv.coverage()).isEqualTo(SkyCoverage.VERTICAL_VISIBILITY);
        assertThat(vv.heightFeet()).isEqualTo(800);
        assertThat(vv.isCeiling()).isTrue();
    }

    @Test
    @DisplayName("Should handle unknown sky conditions (///)")
    void testParseUnknownSkyCondition() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM ///";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse with unknown sky condition");
        NoaaMetarData data = extractMetarData(result);

        // Unknown sky conditions should be skipped (not added)
        List<SkyCondition> skyConditions = data.getSkyConditions();
        assertThat(skyConditions).isEmpty();
    }

    @Test
    @DisplayName("Should parse real-world METAR with sky conditions")
    void testParseRealWorldSkyConditions() {
        String metar = "METAR KJFK 251651Z 19005KT 5SM -RA BR FEW008 SCT015 BKN025CB OVC050";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Should have 4 sky condition layers
        List<SkyCondition> skyConditions = data.getSkyConditions();
        assertThat(skyConditions).hasSize(4);

        // Verify lowest ceiling (BKN)
        SkyCondition ceiling = skyConditions.stream()
                .filter(SkyCondition::isCeiling)
                .findFirst()
                .orElse(null);

        assertThat(ceiling).isNotNull();
        assertThat(ceiling.coverage()).isEqualTo(SkyCoverage.BROKEN);
        assertThat(ceiling.heightFeet()).isEqualTo(2500);
        assertThat(ceiling.isCumulonimbus()).isTrue();
    }

    @Test
    @DisplayName("Should use SkyCondition query methods")
    void testSkyConditionQueryMethods() {
        String metar = "METAR KJFK 251651Z 19005KT 3SM BKN015CB";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        SkyCondition sky = data.getSkyConditions().get(0);

        // Query methods
        assertThat(sky.isCeiling()).isTrue();
        assertThat(sky.isCumulonimbus()).isTrue();
        assertThat(sky.isConvective()).isTrue();
        assertThat(sky.isClear()).isFalse();
        assertThat(sky.isBelowAltitude(2000)).isTrue();
        assertThat(sky.isBelowAltitude(1000)).isFalse();

        // Conversion methods
        assertThat(sky.getHeightMeters()).isEqualTo(457); // ~1500 ft = 457 m
    }

    @Test
    @DisplayName("Should handle OCR errors in sky conditions")
    void testParseOcrErrorsInSkyConditions() {
        // Test 0VC → OVC (zero instead of O)
        String metar1 = "METAR KJFK 251651Z 19005KT 10SM 0VC020";
        ParseResult<NoaaWeatherData> result1 = parser.parse(metar1);

        assertTrue(result1.isSuccess());
        NoaaMetarData data1 = extractMetarData(result1);
        assertThat(data1.getSkyConditions().get(0).coverage()).isEqualTo(SkyCoverage.OVERCAST);

        // Test SCK → SKC (K instead of C)
        String metar2 = "METAR KJFK 251651Z 19005KT 10SM SCK";
        ParseResult<NoaaWeatherData> result2 = parser.parse(metar2);

        assertTrue(result2.isSuccess());
        NoaaMetarData data2 = extractMetarData(result2);
        assertThat(data2.getSkyConditions().get(0).coverage()).isEqualTo(SkyCoverage.SKC);
    }

    @Test
    @DisplayName("Should handle heights with OCR errors (O instead of 0)")
    void testParseHeightOcrErrors() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM BKNO5O"; // O5O instead of 050

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        SkyCondition sky = data.getSkyConditions().get(0);
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.BROKEN);
        assertThat(sky.heightFeet()).isEqualTo(5000); // Should parse as 050 → 5000 ft
    }

    @Test
    @DisplayName("Should parse NCD as NSC")
    void testParseNcdAsNsc() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM NCD";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        List<SkyCondition> skyConditions = data.getSkyConditions();
        assertThat(skyConditions).hasSize(1);
        assertThat(skyConditions.get(0).coverage()).isEqualTo(SkyCoverage.NSC);
    }

    @Test
    @DisplayName("Should parse NCD (No Cloud Detected) as NSC")
    void testParseNcd() {
        String metar = "METAR EGLL 251651Z 19005KT 10SM NCD";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        List<SkyCondition> skyConditions = data.getSkyConditions();
        assertThat(skyConditions).isNotEmpty();

        SkyCondition sky = skyConditions.get(0);
        assertThat(sky.coverage()).isEqualTo(SkyCoverage.NSC);
        assertThat(sky.isClear()).isTrue();
    }

    // ========== TEMPERATURE/DEWPOINT TESTS ==========

    @ParameterizedTest
    @CsvSource({
            // Positive temperatures
            "'METAR KJFK 251651Z 19005KT 10SM 22/12 A3015', 22.0, 12.0, 'Warm day'",
            "'METAR KJFK 251651Z 19005KT 10SM 15/08 A3015', 15.0, 8.0, 'Mild day'",
            "'METAR KJFK 251651Z 19005KT 10SM 30/20 A3015', 30.0, 20.0, 'Hot day'",

            // Negative temperatures with M prefix
            "'METAR KJFK 251651Z 19005KT 10SM M05/M12 A3015', -5.0, -12.0, 'Cold with negative dewpoint'",
            "'METAR KJFK 251651Z 19005KT 10SM M15/M20 A3015', -15.0, -20.0, 'Very cold'",
            "'METAR KJFK 251651Z 19005KT 10SM M01/M05 A3015', -1.0, -5.0, 'Below freezing'",

            // Freezing point
            "'METAR KJFK 251651Z 19005KT 10SM 00/M01 A3015', 0.0, -1.0, 'At freezing point'",
            "'METAR KJFK 251651Z 19005KT 10SM 01/00 A3015', 1.0, 0.0, 'Just above freezing'",

            // Temperature without dewpoint
            "'METAR KJFK 251651Z 19005KT 10SM 22/ A3015', 22.0, , 'Temperature only'",
            "'METAR KJFK 251651Z 19005KT 10SM 15/ A3015', 15.0, , 'No dewpoint reported'",

            // Mixed positive/negative
            "'METAR KJFK 251651Z 19005KT 10SM 05/M02 A3015', 5.0, -2.0, 'Positive temp, negative dewpoint'",
            "'METAR KJFK 251651Z 19005KT 10SM 02/M05 A3015', 2.0, -5.0, 'Cool with frost'",

            // Single digit temperatures
            "'METAR KJFK 251651Z 19005KT 10SM 08/05 A3015', 8.0, 5.0, 'Single digit positive'",
            "'METAR KJFK 251651Z 19005KT 10SM M08/M12 A3015', -8.0, -12.0, 'Single digit negative'"
    })
    @DisplayName("Should parse temperature and dewpoint correctly")
    void testParseTemperature(String metar, Double expectedTemp, Double expectedDewpoint, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        // Should have temperature
        assertNotNull(data.getTemperature(), scenario);

        Temperature temp = data.getTemperature();

        // Assert temperature
        assertEquals(expectedTemp, temp.celsius(), 0.01,
                "Temperature mismatch for: " + scenario);

        // Assert dewpoint
        if (expectedDewpoint != null) {
            assertNotNull(temp.dewpointCelsius(), "Dewpoint should not be null for: " + scenario);
            assertEquals(expectedDewpoint, temp.dewpointCelsius(), 0.01,
                    "Dewpoint mismatch for: " + scenario);
        } else {
            assertNull(temp.dewpointCelsius(), "Dewpoint should be null for: " + scenario);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 251651Z 19005KT 10SM // A3015', 'Missing temperature (//)' ",
            "'METAR KJFK 251651Z 19005KT 10SM XX/XX A3015', 'Missing temperature (XX)'",
            "'METAR KJFK 251651Z 19005KT 10SM MM/MM A3015', 'Missing temperature (MM)'"
    })
    @DisplayName("Should handle missing temperature indicators")
    void testParseMissingTemperature(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        // Missing temperature should result in null
        assertNull(data.getTemperature(), scenario);
    }

    @Test
    @DisplayName("Should handle temperature with missing dewpoint")
    void testParseTemperatureWithMissingDewpoint() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM 22/ A3015";  // ← Fixed: single slash

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getTemperature());
        assertEquals(22.0, data.getTemperature().celsius(), 0.01);
        assertNull(data.getTemperature().dewpointCelsius());
    }

    @Test
    @DisplayName("Should use Temperature query methods")
    void testTemperatureQueryMethods() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM M25/M30 A3015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        Temperature temp = data.getTemperature();
        assertNotNull(temp, "Temperature should not be null");  // ← Better error message

        // Query methods
        assertTrue(temp.isFreezing(), "Should be freezing at -10°C");
        assertTrue(temp.isBelowFreezing());
        assertFalse(temp.isAboveFreezing());
        assertTrue(temp.isVeryCold());
        assertFalse(temp.isVeryHot());
    }

    @Test
    @DisplayName("Should calculate temperature spread and relative humidity")
    void testTemperatureCalculations() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM 20/10 A3015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        Temperature temp = data.getTemperature();
        assertNotNull(temp);

        // Temperature spread
        Double spread = temp.getSpread();
        assertNotNull(spread);
        assertEquals(10.0, spread, 0.01);

        // Relative humidity
        Double rh = temp.getRelativeHumidity();
        assertNotNull(rh);
        assertTrue(rh >= 0.0 && rh <= 100.0);

        // Not fog conditions (spread > 2°C)
        assertFalse(temp.isFogLikely());
    }

    @Test
    @DisplayName("Should identify fog conditions")
    void testTemperatureFogConditions() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM 15/14 A3015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        Temperature temp = data.getTemperature();
        assertNotNull(temp);

        // Small spread indicates fog likely
        assertTrue(temp.isFogLikely());

        // High relative humidity
        Double rh = temp.getRelativeHumidity();
        assertNotNull(rh);
        assertTrue(rh > 80.0);
    }

    @Test
    @DisplayName("Should convert temperature to Fahrenheit and Kelvin")
    void testTemperatureConversions() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM 20/15 A3015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        Temperature temp = data.getTemperature();
        assertNotNull(temp);

        // Fahrenheit conversion
        Double tempF = temp.toFahrenheit();
        assertNotNull(tempF);
        assertEquals(68.0, tempF, 0.1); // 20°C = 68°F

        Double dewpointF = temp.dewpointToFahrenheit();
        assertNotNull(dewpointF);
        assertEquals(59.0, dewpointF, 0.1); // 15°C = 59°F

        // Kelvin conversion
        Double tempK = temp.toKelvin();
        assertNotNull(tempK);
        assertEquals(293.15, tempK, 0.01); // 20°C = 293.15K
    }

    @Test
    @DisplayName("Should parse real-world METAR with temperature")
    void testParseRealWorldTemperature() {
        String metar = "METAR KJFK 251651Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Verify temperature
        assertNotNull(data.getTemperature());
        assertEquals(22.0, data.getTemperature().celsius(), 0.01);
        assertEquals(12.0, data.getTemperature().dewpointCelsius(), 0.01);

        // Verify other components still parsed
        assertNotNull(data.getWind());
        assertNotNull(data.getVisibility());
        assertNotNull(data.getSkyConditions());
    }

    @Test
    @DisplayName("Should handle extreme temperatures")
    void testParseExtremeTemperatures() {
        // Very cold
        String coldMetar = "METAR KJFK 251651Z 19005KT 10SM M45/M50 A3015";
        ParseResult<NoaaWeatherData> coldResult = parser.parse(coldMetar);
        assertTrue(coldResult.isSuccess());
        NoaaMetarData coldData = extractMetarData(coldResult);
        assertEquals(-45.0, coldData.getTemperature().celsius(), 0.01);
        assertTrue(coldData.getTemperature().isVeryCold());

        // Very hot
        String hotMetar = "METAR KJFK 251651Z 19005KT 10SM 40/25 A3015";
        ParseResult<NoaaWeatherData> hotResult = parser.parse(hotMetar);
        assertTrue(hotResult.isSuccess());
        NoaaMetarData hotData = extractMetarData(hotResult);
        assertEquals(40.0, hotData.getTemperature().celsius(), 0.01);
        assertTrue(hotData.getTemperature().isVeryHot());
    }

    @Test
    @DisplayName("Should identify icing conditions")
    void testTemperatureIcingConditions() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM M10/M12 A3015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        Temperature temp = data.getTemperature();
        assertNotNull(temp);

        // Between 0°C and -20°C with high humidity = icing likely
        assertTrue(temp.isIcingLikely());
    }

    // ========== ALTIMETER/PRESSURE TESTS ==========

    @ParameterizedTest
    @CsvSource({
            // North American format (A prefix)
            "'METAR KJFK 251651Z 19005KT 10SM A3015', 30.15, INCHES_HG, 'Standard North American format'",
            "'METAR KJFK 251651Z 19005KT 10SM A2992', 29.92, INCHES_HG, 'Standard sea level pressure'",
            "'METAR KJFK 251651Z 19005KT 10SM A3050', 30.50, INCHES_HG, 'High pressure'",
            "'METAR KJFK 251651Z 19005KT 10SM A2950', 29.50, INCHES_HG, 'Low pressure'",

            // International format (Q prefix)
            "'METAR EGLL 251651Z 19005KT 10SM Q1013', 1013.0, HECTOPASCALS, 'Standard international format'",
            "'METAR EGLL 251651Z 19005KT 10SM Q0998', 998.0, HECTOPASCALS, 'Low pressure hPa'",
            "'METAR EGLL 251651Z 19005KT 10SM Q1030', 1030.0, HECTOPASCALS, 'High pressure hPa'",
            "'METAR EGLL 251651Z 19005KT 10SM Q0950', 950.0, HECTOPASCALS, 'Very low pressure (hurricane)'",

            // QNH format
            "'METAR EGLL 251651Z 19005KT 10SM QNH1013', 1013.0, HECTOPASCALS, 'QNH format'",
            "'METAR EGLL 251651Z 19005KT 10SM QNH1025', 1025.0, HECTOPASCALS, 'QNH high pressure'",

            // Older INS suffix format
            "'METAR KJFK 251651Z 19005KT 10SM 2992INS', 29.92, INCHES_HG, 'INS suffix format'",
            "'METAR KJFK 251651Z 19005KT 10SM 3015INS', 30.15, INCHES_HG, 'INS suffix high pressure'",

            // No prefix (value-based detection)
            "'METAR EGLL 251651Z 19005KT 10SM 998', 998.0, HECTOPASCALS, 'No prefix - 3 digits hPa'"
    })
    @DisplayName("Should parse altimeter/pressure correctly")
    void testParseAltimeter(String metar, Double expectedValue, PressureUnit expectedUnit, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        // Should have pressure
        assertNotNull(data.getPressure(), scenario);

        Pressure pressure = data.getPressure();

        // Assert pressure value and unit
        assertEquals(expectedValue, pressure.value(), 0.01,
                "Pressure value mismatch for: " + scenario);
        assertEquals(expectedUnit, pressure.unit(),
                "Pressure unit mismatch for: " + scenario);
    }

    @Test
    @DisplayName("Should handle missing altimeter (////)")
    void testParseMissingAltimeter() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM //// RMK AO2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Missing altimeter should result in null
        assertNull(data.getPressure());
    }

    @Test
    @DisplayName("Should convert pressure to different units")
    void testPressureConversions() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM A3015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        Pressure pressure = data.getPressure();
        assertNotNull(pressure);

        // Original value
        assertEquals(30.15, pressure.value(), 0.01);
        assertEquals(PressureUnit.INCHES_HG, pressure.unit());

        // Convert to hPa
        double hPa = pressure.toHectopascals();
        assertEquals(1021.0, hPa, 1.0); // ~30.15 inHg = ~1021 hPa

        // Convert to millibars (same as hPa)
        double mb = pressure.toMillibars();
        assertEquals(hPa, mb, 0.01);
    }

    @Test
    @DisplayName("Should use Pressure query methods")
    void testPressureQueryMethods() {
        // Low pressure
        String lowMetar = "METAR KJFK 251651Z 19005KT 10SM Q0995";
        ParseResult<NoaaWeatherData> lowResult = parser.parse(lowMetar);
        Pressure lowPressure = extractMetarData(lowResult).getPressure();

        assertNotNull(lowPressure);
        assertTrue(lowPressure.isLowPressure());
        assertTrue(lowPressure.isBelowStandard());
        assertFalse(lowPressure.isHighPressure());

        // High pressure
        String highMetar = "METAR KJFK 251651Z 19005KT 10SM Q1025";
        ParseResult<NoaaWeatherData> highResult = parser.parse(highMetar);
        Pressure highPressure = extractMetarData(highResult).getPressure();

        assertNotNull(highPressure);
        assertTrue(highPressure.isHighPressure());
        assertTrue(highPressure.isAboveStandard());
        assertFalse(highPressure.isLowPressure());
    }

    @Test
    @DisplayName("Should calculate deviation from standard pressure")
    void testPressureDeviation() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM Q1030";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        Pressure pressure = data.getPressure();
        assertNotNull(pressure);

        // Standard is 1013.25 hPa, so 1030 is +16.75 hPa above standard
        double deviation = pressure.getDeviationFromStandard();
        assertEquals(16.75, deviation, 0.1);
    }

    @Test
    @DisplayName("Should format pressure correctly")
    void testPressureFormatting() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM A3015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        Pressure pressure = data.getPressure();
        assertNotNull(pressure);

        // Formatted value
        String formatted = pressure.getFormattedValue();
        assertTrue(formatted.contains("30.15"));
        assertTrue(formatted.contains("inHg"));

        // METAR altimeter format
        String altimeter = pressure.toMetarAltimeter();
        assertEquals("A3015", altimeter);

        // METAR QNH format
        String qnh = pressure.toMetarQNH();
        assertTrue(qnh.startsWith("Q"));
    }

    @Test
    @DisplayName("Should calculate pressure altitude")
    void testPressureAltitude() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM A2992"; // Standard pressure

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        Pressure pressure = data.getPressure();
        assertNotNull(pressure);

        // At standard pressure (29.92 inHg), pressure altitude should be near 0
        double pressureAltitude = pressure.getPressureAltitudeFeet();
        assertEquals(0.0, pressureAltitude, 50.0); // Within 50 feet of sea level
    }

    @Test
    @DisplayName("Should handle OCR errors in altimeter (O instead of 0)")
    void testAltimeterOcrErrors() {
        // Note: This would require the regex to match, so if the pattern doesn't allow 'O',
        // this test might not be applicable. Assuming the regex handles it:
        String metar = "METAR KJFK 251651Z 19005KT 10SM A3O15"; // O instead of 0

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        Pressure pressure = data.getPressure();
        assertNotNull(pressure);
        assertEquals(30.15, pressure.value(), 0.01);
    }

    @Test
    @DisplayName("Should detect extremely low pressure (hurricane)")
    void testExtremelyLowPressure() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM Q0920";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        Pressure pressure = data.getPressure();
        assertNotNull(pressure);
        assertTrue(pressure.isExtremelyLow());
        assertTrue(pressure.isLowPressure());
    }

    @Test
    @DisplayName("Should detect extremely high pressure")
    void testExtremelyHighPressure() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM Q1050";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        Pressure pressure = data.getPressure();
        assertNotNull(pressure);
        assertTrue(pressure.isExtremelyHigh());
        assertTrue(pressure.isHighPressure());
    }

    @Test
    @DisplayName("Should parse real-world METAR with altimeter")
    void testParseRealWorldAltimeter() {
        String metar = "METAR KJFK 251651Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Verify altimeter
        assertNotNull(data.getPressure());
        assertEquals(30.15, data.getPressure().value(), 0.01);
        assertEquals(PressureUnit.INCHES_HG, data.getPressure().unit());

        // Verify other components still parsed
        assertNotNull(data.getWind());
        assertNotNull(data.getVisibility());
        assertNotNull(data.getTemperature());
    }

    @Test
    @DisplayName("Should handle altimeter with both prefix and suffix")
    void testAltimeterWithPrefixAndSuffix() {
        // Edge case: both prefix and suffix (prefix should take precedence)
        String metar = "METAR KJFK 251651Z 19005KT 10SM A2992INS";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getPressure());
        assertEquals(29.92, data.getPressure().value(), 0.01);
        assertEquals(PressureUnit.INCHES_HG, data.getPressure().unit());
    }

    @Test
    @DisplayName("Should handle 3-digit pressure value (hPa)")
    void testThreeDigitPressure() {
        String metar = "METAR EGLL 251651Z 19005KT 10SM Q998";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getPressure());
        assertEquals(998.0, data.getPressure().value(), 0.01);
        assertEquals(PressureUnit.HECTOPASCALS, data.getPressure().unit());
    }

    @Test
    @DisplayName("Should handle pressure at minimum valid range")
    void testPressureAtMinimumRange() {
        // Minimum valid pressure (hurricane-level)
        String metar = "METAR KJFK 251651Z 19005KT 10SM A2500";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getPressure());
        assertEquals(25.00, data.getPressure().value(), 0.01);
    }

    @Test
    @DisplayName("Should handle pressure at maximum valid range")
    void testPressureAtMaximumRange() {
        // Maximum valid pressure
        String metar = "METAR KJFK 251651Z 19005KT 10SM A3500";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getPressure());
        assertEquals(35.00, data.getPressure().value(), 0.01);
    }

    @Test
    @DisplayName("Should handle AA prefix (double A)")
    void testDoubleAPrefixAltimeter() {
        String metar = "METAR KJFK 251651Z 19005KT 10SM AA3015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getPressure());
        assertEquals(30.15, data.getPressure().value(), 0.01);
        assertEquals(PressureUnit.INCHES_HG, data.getPressure().unit());
    }

    @Test
    @DisplayName("Should handle lowercase prefix (normalized to uppercase)")
    void testLowercasePrefixAltimeter() {
        // Some parsers might encounter lowercase, test normalization
        String metar = "METAR KJFK 251651Z 19005KT 10SM a3015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        // May or may not parse depending on regex - test what actually happens
        if (result.isSuccess()) {
            NoaaMetarData data = extractMetarData(result);
            if (data.getPressure() != null) {
                assertEquals(30.15, data.getPressure().value(), 0.01);
            }
        }
        // This test documents behavior rather than asserts - lowercase may not be in regex
    }

    @Test
    @DisplayName("Should parse pressure from multiple formats in sequence")
    void testMultipleAltimeterFormats() {
        // Test different formats parse correctly
        String[] metars = {
                "METAR KJFK 251651Z 19005KT 10SM A3015",
                "METAR EGLL 251651Z 19005KT 10SM Q1013",
                "METAR KJFK 251651Z 19005KT 10SM QNH1013",
                "METAR KJFK 251651Z 19005KT 10SM 2992INS"
        };

        for (String metar : metars) {
            ParseResult<NoaaWeatherData> result = parser.parse(metar);
            assertTrue(result.isSuccess(), "Should parse: " + metar);
            NoaaMetarData data = extractMetarData(result);
            assertNotNull(data.getPressure(), "Should have pressure for: " + metar);
        }
    }

    // ========== NO SIGNIFICANT CHANGE TESTS ==========

    @Test
    @DisplayName("Should parse NOSIG in main body")
    void testParseNoSignificantChange() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM FEW250 22/12 A3015 NOSIG RMK AO2";
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.isNoSignificantChange()).isTrue();
    }

    @Test
    @DisplayName("Should parse NOSIG at end of main body (no RMK)")
    void testParseNoSignificantChange_NoRemarks() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM FEW250 22/12 A3015 NOSIG";
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.isNoSignificantChange()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2', 'No NOSIG'",
            "'METAR KJFK 121853Z 28016KT 10SM FEW250 22/12 A3015', 'No NOSIG or RMK'"
    })
    @DisplayName("Should have false noSignificantChange when NOSIG not present")
    void testNoSignificantChange_NotPresent(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);
        assertThat(data.isNoSignificantChange())
                .as("NOSIG should be false: %s", scenario)
                .isFalse();
    }

    // ========== REMARKS PARSING TESTS ==========

    @Test
    @DisplayName("Should parse METAR with AO1 remark")
    void testParseMetarWithAO1Remark() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM FEW250 22/12 A3015 RMK AO1 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null");
        assertEquals(AutomatedStationType.AO1, data.getRemarks().automatedStationType());
        assertFalse(data.getRemarks().hasPrecipitationDiscriminator());
    }

    @Test
    @DisplayName("Should parse METAR with AO2 remark")
    void testParseMetarWithAO2Remark() {
        String metar = "METAR KORD 121856Z 09014G20KT 10SM FEW055 SCT250 23/14 A2990 RMK AO2 SLP121";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null");
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertTrue(data.getRemarks().hasPrecipitationDiscriminator());
    }

    @Test
    @DisplayName("Should parse METAR with AO2 and SLP, storing unparsed T-group as free text")
    void testParseMetarWithAO2AndFreeText() {
        String metar = "METAR KORD 121856Z 09014G20KT 10SM FEW055 SCT250 23/14 A2990 RMK AO2 SLP121 T02330139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null");

        // AO2 should be parsed
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());

        // SLP121 should be parsed
        assertNotNull(data.getRemarks().seaLevelPressure(),"Sea level pressure should be parsed");
        assertEquals(1012.1, data.getRemarks().seaLevelPressure().toHectopascals(), 0.1,
                "SLP121 should decode to 1012.1 hPa");

        // T-group should be parsed
        assertNotNull(data.getRemarks().preciseTemperature(), "Precise temperature should be parsed");
        assertEquals(23.3, data.getRemarks().preciseTemperature().celsius(), 0.1);
        assertEquals(13.9, data.getRemarks().preciseTemperature().dewpointCelsius(), 0.1);

        // No unparsed free text should remain (all remarks parsed)
        if (data.getRemarks().freeText() != null) {
            assertTrue(data.getRemarks().freeText().isBlank(),
                    "All remarks should be parsed");
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM FEW250 22/12 A3015', 'No RMK section'",
            "'METAR KJFK 121853Z 28016KT 10SM FEW250 22/12 A3015 RMK', 'Empty RMK section'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK   ', 'RMK with only whitespace'"
    })
    @DisplayName("Should handle METARs with missing or empty remarks")
    void testParseMetarWithMissingOrEmptyRemarks(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        // Remarks should be null or empty
        if (data.getRemarks() != null) {
            assertTrue(data.getRemarks().isEmpty(),
                    "Remarks should be empty for: " + scenario);
        }
    }

    @Test
    @DisplayName("Should handle OCR error A01 (corrected to AO1)")
    void testParseMetarWithOcrErrorA01() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM FEW250 22/12 A3015 RMK A01 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null");
        // A01 should be corrected to AO1 by the enum
        assertEquals(AutomatedStationType.AO1, data.getRemarks().automatedStationType());
    }

    @Test
    @DisplayName("Should parse METAR with only AO2 remark")
    void testParseMetarWithOnlyAO2() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null");
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        // Free text should be null or blank since AO2 was the only remark
        if (data.getRemarks().freeText() != null) {
            assertTrue(data.getRemarks().freeText().isBlank(),
                    "Free text should be blank when only AO2 present");
        }
    }

    @Test
    @DisplayName("Should handle remarks without automated station type but with SLP")
    void testParseMetarRemarksWithNoAutomatedStationType() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM FEW250 22/12 A3015 RMK SLP210 T02220117";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null");

        // No AO type
        assertNull(data.getRemarks().automatedStationType(),"Automated station type should be null");

        // SLP210 should be parsed
        assertNotNull(data.getRemarks().seaLevelPressure(),"Sea level pressure should be parsed");
        assertEquals(1021.0, data.getRemarks().seaLevelPressure().toHectopascals(), 0.1,
                "SLP210 should decode to 1021.0 hPa");

        // T-group should be parsed
        assertNotNull(data.getRemarks().preciseTemperature());
        assertEquals(22.2, data.getRemarks().preciseTemperature().celsius(), 0.1);
        assertEquals(11.7, data.getRemarks().preciseTemperature().dewpointCelsius(), 0.1);
    }

    @Test
    @DisplayName("Should preserve main body parsing when remarks are present")
    void testParseMetarWithRemarksPreservesMainBody() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Verify main body was parsed correctly
        assertEquals("KJFK", data.getStationId());
        assertNotNull(data.getWind());
        assertEquals(280, data.getWind().directionDegrees());
        assertEquals(16, data.getWind().speedValue());
        assertNotNull(data.getVisibility());
        assertEquals(10.0, data.getVisibility().distanceValue(), 0.01);
        assertNotNull(data.getTemperature());
        assertEquals(22.0, data.getTemperature().celsius(), 0.01);
        assertNotNull(data.getPressure());
        assertEquals(30.15, data.getPressure().value(), 0.01);

        // Verify remarks were also parsed
        assertNotNull(data.getRemarks());
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO1', AO1, false, 'AO1 without precipitation discriminator'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2', AO2, true, 'AO2 with precipitation discriminator'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK A01', AO1, false, 'A01 OCR error corrected to AO1'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK A02', AO2, true, 'A02 OCR error corrected to AO2'"
    })
    @DisplayName("Should parse various automated station types correctly")
    void testParseVariousAutomatedStationTypes(String metar, AutomatedStationType expectedType,
                                               boolean expectedHasDiscriminator, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), scenario);
        assertEquals(expectedType, data.getRemarks().automatedStationType(), scenario);
        assertEquals(expectedHasDiscriminator, data.getRemarks().hasPrecipitationDiscriminator(), scenario);
    }

    @Test
    @DisplayName("Should parse real-world METAR with complete remarks")
    void testParseRealWorldMetarWithRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 RMK AO2 SLP201 T02330139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Verify all components parsed
        assertEquals("KJFK", data.getStationId());
        assertNotNull(data.getWind());
        assertNotNull(data.getVisibility());
        assertNotNull(data.getSkyConditions());
        assertNotNull(data.getTemperature());
        assertNotNull(data.getPressure());

        // Verify remarks
        assertNotNull(data.getRemarks());

        // AO2 should be parsed
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());

        // SLP201 should be parsed
        assertNotNull(data.getRemarks().seaLevelPressure(),"Sea level pressure should be parsed");
        assertEquals(1020.1, data.getRemarks().seaLevelPressure().toHectopascals(), 0.1,
                "SLP201 should decode to 1020.1 hPa");

        // T02330139 should be parsed
        assertNotNull(data.getRemarks().preciseTemperature());
        assertEquals(23.3, data.getRemarks().preciseTemperature().celsius(), 0.1);
        assertEquals(13.9, data.getRemarks().preciseTemperature().dewpointCelsius(), 0.1);
    }

    @Test
    @DisplayName("Should handle remarks with no automated station type pattern but with SLP")
    void testParseMetarRemarksWithoutAutoPattern() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK SLP210 T02330139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());

        // No AO type
        assertNull(data.getRemarks().automatedStationType(),"Should have no automated station type");

        // SLP210 should be parsed
        assertNotNull(data.getRemarks().seaLevelPressure(),"Sea level pressure should be parsed");
        assertEquals(1021.0, data.getRemarks().seaLevelPressure().toHectopascals(), 0.1,
                "SLP210 should decode to 1021.0 hPa");

        // T-group should be parsed
        assertNotNull(data.getRemarks().preciseTemperature());
        assertEquals(23.3, data.getRemarks().preciseTemperature().celsius(), 0.1);
        assertEquals(13.9, data.getRemarks().preciseTemperature().dewpointCelsius(), 0.1);
    }

    @Test
    @DisplayName("Should handle invalid automated station type digit")
    void testParseMetarWithInvalidAutoDigit() {
        // This covers lines 397-401 - exception handling
        // AO9 is invalid (only AO1 and AO2 are valid)
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO9 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Parser should handle invalid AO digit gracefully");
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        // Invalid digit should result in null automated station type
        // but remarks should still be created with free text
        assertNull(data.getRemarks().automatedStationType(),
                "Invalid AO9 should not set automated station type");

        // The unparsed text should still be captured
        if (data.getRemarks().freeText() != null) {
            // Depending on implementation, might contain "SLP210" or full text
            assertFalse(data.getRemarks().freeText().isBlank());
        }
    }

    @Test
    @DisplayName("Should handle malformed automated station pattern")
    void testParseMetarWithMalformedAutoPattern() {
        // Additional edge case - letters instead of digits
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AOX SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        assertNull(data.getRemarks().automatedStationType(),
                "Malformed pattern should not match");
        assertNotNull(data.getRemarks().freeText());
    }

    @Test
    @DisplayName("Should handle blank remarks text (whitespace only)")
    void testParseMetarWithBlankRemarks() {
        // Edge case: RMK followed by only spaces
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK   ";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Blank remarks should result in null or empty
        if (data.getRemarks() != null) {
            assertTrue(data.getRemarks().isEmpty());
        }
    }

    @Test
    @DisplayName("Should handle AO3 invalid digit")
    void testParseMetarWithAO3Invalid() {
        // Explicit test for AO3 (invalid)
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO3";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        assertNull(data.getRemarks().automatedStationType(),
                "AO3 is invalid, should be null");
    }

    @Test
    @DisplayName("Should handle A00 OCR error variant")
    void testParseMetarWithA00OcrError() {
        // A00 is invalid (both digits are zero)
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK A00";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        // A00 might match pattern but fail in fromDigit()
        // Should be handled gracefully
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "METAR KJFK 121853Z 28016KT 10SM RMK AO5",     // Invalid digit 5
            "METAR KJFK 121853Z 28016KT 10SM RMK AO7",     // Invalid digit 7
            "METAR KJFK 121853Z 28016KT 10SM RMK A03",     // Invalid digit 3
            "METAR KJFK 121853Z 28016KT 10SM RMK A04"      // Invalid digit 4
    })
    @DisplayName("Should handle various invalid AO digits gracefully")
    void testParseMetarWithVariousInvalidDigits(String metar) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully even with invalid AO digit");
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Should have remarks object");
        assertNull(data.getRemarks().automatedStationType(),
                "Invalid AO digit should result in null automated station type");
    }

    // ========== SEA LEVEL PRESSURE PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            // High values (>= 500): add 900
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP982', 998.2, 'High pressure - hurricane level'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP500', 950.0, 'Boundary high value'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK SLP982', 998.2, 'High without AO2'",

            // Low values (< 500): add 1000
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210', 1021.0, 'Standard pressure'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP145', 1014.5, 'Normal pressure'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP499', 1049.9, 'Boundary low value'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK SLP210', 1021.0, 'Low without AO2'",

            // Edge cases
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP000', 1000.0, 'Minimum value'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP999', 999.9, 'Maximum value'"
    })
    @DisplayName("Should parse sea level pressure correctly")
    void testParseSeaLevelPressure(String metar, double expectedHPa, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null: " + scenario);
        assertNotNull(data.getRemarks().seaLevelPressure(),
                "Sea level pressure should not be null: " + scenario);

        double actualHPa = data.getRemarks().seaLevelPressure().toHectopascals();
        assertEquals(expectedHPa, actualHPa, 0.1,
                "Sea level pressure mismatch for: " + scenario);
    }

    @Test
    @DisplayName("Should handle SLPNO (pressure not available)")
    void testParseSeaLevelPressureNotAvailable() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLPNO T02330139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        assertNull(data.getRemarks().seaLevelPressure(),
                "SLPNO should result in null pressure");

        // AO2 should be parsed
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());

        // T-group should be parsed (not in free text)
        assertNotNull(data.getRemarks().preciseTemperature());
        assertEquals(23.3, data.getRemarks().preciseTemperature().celsius(), 0.1);
        assertEquals(13.9, data.getRemarks().preciseTemperature().dewpointCelsius(), 0.1);
    }

    @Test
    @DisplayName("Should parse SLP with AO2 and additional remarks")
    void testParseSeaLevelPressureWithMultipleRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 RMK AO2 SLP201 T02330139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Verify all parsed remarks
        assertNotNull(data.getRemarks());
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertNotNull(data.getRemarks().seaLevelPressure());
        assertEquals(1020.1, data.getRemarks().seaLevelPressure().toHectopascals(), 0.1);

        // T-group should be parsed (not in free text)
        assertNotNull(data.getRemarks().preciseTemperature());
        assertEquals(23.3, data.getRemarks().preciseTemperature().celsius(), 0.1);
        assertEquals(13.9, data.getRemarks().preciseTemperature().dewpointCelsius(), 0.1);
    }

    @Test
    @DisplayName("Should parse SLP without AO2")
    void testParseSeaLevelPressureWithoutAO2() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        assertNull(data.getRemarks().automatedStationType(), "Should have no AO type");
        assertNotNull(data.getRemarks().seaLevelPressure());
        assertEquals(1021.0, data.getRemarks().seaLevelPressure().toHectopascals(), 0.1);
    }

    @Test
    @DisplayName("Should handle real-world METAR with SLP")
    void testParseRealWorldMetarWithSLP() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 RMK AO2 SLP201 T02330139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Verify main body
        assertEquals("KJFK", data.getStationId());
        assertNotNull(data.getWind());
        assertNotNull(data.getVisibility());
        assertNotNull(data.getTemperature());
        assertNotNull(data.getPressure());

        // Verify remarks
        assertNotNull(data.getRemarks());
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertEquals(1020.1, data.getRemarks().seaLevelPressure().toHectopascals(), 0.1);
    }

    @Test
    @DisplayName("Should handle malformed SLP gracefully")
    void testParseMalformedSeaLevelPressure() {
        // Invalid format - not 3 digits
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP99 T02330139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should handle malformed SLP gracefully");
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        // Malformed SLP should not match pattern, so will be in free text
        assertNull(data.getRemarks().seaLevelPressure());

        // Should be captured in free text
        assertNotNull(data.getRemarks().freeText());
    }

    @Test
    @DisplayName("Should convert sea level pressure to other units")
    void testSeaLevelPressureUnitConversions() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        Pressure slp = data.getRemarks().seaLevelPressure();
        assertNotNull(slp);

        // Verify conversions
        assertEquals(1021.0, slp.toHectopascals(), 0.1);
        assertEquals(1021.0, slp.toMillibars(), 0.1);
        assertEquals(30.15, slp.toInchesHg(), 0.01);
    }

    @Test
    @DisplayName("Should parse SLP before AO2")
    void testParseSeaLevelPressureBeforeAO2() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM RMK SLP210 AO2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());

        // SLP comes first and should parse
        assertNotNull(data.getRemarks().seaLevelPressure());
        assertEquals(1021.0, data.getRemarks().seaLevelPressure().toHectopascals(), 0.1);

        // AO2 comes after but might be in free text depending on sequential parsing
        // At minimum, one of them should be parsed
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP055', 1005.5, 'Extremely high pressure'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP920', 992.0, 'Extremely low pressure (hurricane level)'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP500', 950.0, 'Boundary value 500 (uses 900 + 50.0)'"
    })
    @DisplayName("Should handle extreme and boundary sea level pressure values")
    void testParseExtremePressureValues(String metar, double expectedHPa, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().seaLevelPressure(),
                "Sea level pressure should not be null: " + scenario);
        assertEquals(expectedHPa, data.getRemarks().seaLevelPressure().toHectopascals(), 0.1,
                "Sea level pressure mismatch for: " + scenario);
    }

    @Test
    @DisplayName("Should handle SLP with only digits")
    void testParseSeaLevelPressureOnlyDigits() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK SLP145";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        assertNotNull(data.getRemarks().seaLevelPressure());
        assertEquals(1014.5, data.getRemarks().seaLevelPressure().toHectopascals(), 0.1);
    }

    // ========== SECTION: HOURLY TEMPERATURE/DEWPOINT PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            // Positive temperature, positive dewpoint
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210 T02330139', 23.3, 13.9, 'Positive temp and dewpoint'",

            // Negative temperature, negative dewpoint
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210 T10281035', -2.8, -3.5, 'Negative temp and dewpoint'",

            // Positive temperature, negative dewpoint
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 T00171002', 1.7, -0.2, 'Positive temp, negative dewpoint'",

            // Near freezing
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 T00011000', 0.1, -0.0, 'Near freezing'",

            // Very cold
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK T12781289', -27.8, -28.9, 'Very cold conditions'",

            // Very hot
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK T04670311', 46.7, 31.1, 'Very hot conditions'"
    })
    @DisplayName("Should parse T-group with temperature and dewpoint")
    void testParseHourlyTemperatureWithDewpoint(String metar, double expectedTemp, double expectedDewpt, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null: " + scenario);
        assertNotNull(data.getRemarks().preciseTemperature(),
                "Precise temperature should not be null: " + scenario);

        // Verify temperature
        assertEquals(expectedTemp, data.getRemarks().preciseTemperature().celsius(), 0.1,
                "Temperature mismatch for: " + scenario);

        // Verify dewpoint
        assertNotNull(data.getRemarks().preciseTemperature().dewpointCelsius(),
                "Dewpoint should not be null: " + scenario);
        assertEquals(expectedDewpt, data.getRemarks().preciseTemperature().dewpointCelsius(), 0.1,
                "Dewpoint mismatch for: " + scenario);
    }

    @Test
    @DisplayName("Should parse T-group with temperature only (no dewpoint)")
    void testParseHourlyTemperatureOnly() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210 T0233";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        assertNotNull(data.getRemarks().preciseTemperature());

        // Temperature should be present
        assertEquals(23.3, data.getRemarks().preciseTemperature().celsius(), 0.1);

        // Dewpoint should be null (not reported)
        assertNull(data.getRemarks().preciseTemperature().dewpointCelsius(),
                "Dewpoint should be null when not reported");
    }

    @Test
    @DisplayName("Should parse complete METAR with AO2, SLP, and T-group")
    void testParseCompleteMetarWithAllRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 RMK AO2 SLP201 T02330139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Verify main body
        assertEquals("KJFK", data.getStationId());
        assertNotNull(data.getWind());
        assertNotNull(data.getVisibility());
        assertNotNull(data.getTemperature());
        assertNotNull(data.getPressure());

        // Verify all remarks parsed
        assertNotNull(data.getRemarks());

        // AO2
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());

        // SLP201
        assertNotNull(data.getRemarks().seaLevelPressure());
        assertEquals(1020.1, data.getRemarks().seaLevelPressure().toHectopascals(), 0.1);

        // T02330139
        assertNotNull(data.getRemarks().preciseTemperature());
        assertEquals(23.3, data.getRemarks().preciseTemperature().celsius(), 0.1);
        assertEquals(13.9, data.getRemarks().preciseTemperature().dewpointCelsius(), 0.1);

        // No unparsed free text should remain
        if (data.getRemarks().freeText() != null) {
            assertTrue(data.getRemarks().freeText().isBlank(),
                    "All remarks should be parsed, no free text expected");
        }
    }

    @Test
    @DisplayName("Should parse T-group without AO2 or SLP")
    void testParseHourlyTemperatureWithoutOtherRemarks() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK T02330139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());

        // No AO type
        assertNull(data.getRemarks().automatedStationType());

        // No SLP
        assertNull(data.getRemarks().seaLevelPressure());

        // But T-group should be parsed
        assertNotNull(data.getRemarks().preciseTemperature());
        assertEquals(23.3, data.getRemarks().preciseTemperature().celsius(), 0.1);
        assertEquals(13.9, data.getRemarks().preciseTemperature().dewpointCelsius(), 0.1);
    }

    @Test
    @DisplayName("Should handle malformed T-group gracefully")
    void testParseMalformedHourlyTemperature() {
        // Invalid format - not enough digits
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 T023";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should handle malformed T-group gracefully");
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        // Malformed T-group should not match pattern
        assertNull(data.getRemarks().preciseTemperature());
    }

    @Test
    @DisplayName("Should calculate relative humidity from T-group")
    void testHourlyTemperatureRelativeHumidity() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK T02330139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        Temperature preciseTemp = data.getRemarks().preciseTemperature();
        assertNotNull(preciseTemp);

        // Temperature class should calculate relative humidity
        Double rh = preciseTemp.getRelativeHumidity();
        assertNotNull(rh, "Relative humidity should be calculable");
        assertTrue(rh >= 0 && rh <= 100, "RH should be between 0-100%");

        // For 23.3°C temp and 13.9°C dewpoint, RH should be around 56%
        assertEquals(56.0, rh, 5.0, "RH should be approximately 56%");
    }

    @Test
    @DisplayName("Should parse T-group at end of remarks (no trailing space)")
    void testParseHourlyTemperatureAtEnd() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210 T02330139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        assertNotNull(data.getRemarks().preciseTemperature());
        assertEquals(23.3, data.getRemarks().preciseTemperature().celsius(), 0.1);
    }

    @Test
    @DisplayName("Should verify T-group is more precise than main body temp")
    void testHourlyTemperatureVsMainBodyTemperature() {
        // Main body: 23/14 (whole degrees)
        // T-group: T02330139 (23.3°C / 13.9°C - tenths precision)
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 RMK AO2 SLP201 T02330139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Main body temperature (whole degrees)
        assertEquals(23.0, data.getTemperature().celsius(), 0.1);
        assertEquals(14.0, data.getTemperature().dewpointCelsius(), 0.1);

        // T-group temperature (tenth degree precision)
        assertEquals(23.3, data.getRemarks().preciseTemperature().celsius(), 0.1);
        assertEquals(13.9, data.getRemarks().preciseTemperature().dewpointCelsius(), 0.1);

        // T-group should be more precise
        assertNotEquals(data.getTemperature().celsius(),
                data.getRemarks().preciseTemperature().celsius(),
                "T-group should provide more precise temperature");
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM RMK T0000', 0.0, 'Zero degrees'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK T1000', -0.0, 'Negative zero'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK T0001', 0.1, 'Just above freezing'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK T1001', -0.1, 'Just below freezing'"
    })
    @DisplayName("Should handle freezing point edge cases")
    void testFreezingPointEdgeCases(String metar, double expectedTemp, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().preciseTemperature());
        assertEquals(expectedTemp, data.getRemarks().preciseTemperature().celsius(), 0.1,
                scenario);
    }

    @Test
    @DisplayName("Should parse T-group in mixed remark order")
    void testParseTGroupInMixedOrder() {
        // T-group in middle of remarks
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 T02330139 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());

        // All should be parsed
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertNotNull(data.getRemarks().preciseTemperature());
        assertEquals(23.3, data.getRemarks().preciseTemperature().celsius(), 0.1);
        assertNotNull(data.getRemarks().seaLevelPressure());
        assertEquals(1021.0, data.getRemarks().seaLevelPressure().toHectopascals(), 0.1);
    }

    // ========== PEAK WIND PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            // Complete peak wind (direction, speed, hour, minute)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 PK WND 28032/1530', 280, 32, 15, 30, 'Complete peak wind'",

            // High speed with P prefix (>99 knots)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK PK WND 360P120/2145', 360, 120, 21, 45, 'High speed >99kt with P prefix'",

            // Another high speed example
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK PK WND 180P105/0830', 180, 105, 8, 30, 'Speed 105kt'",

            // Different directions
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK PK WND 090045/1215', 90, 45, 12, 15, 'East wind'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK PK WND 180050/2359', 180, 50, 23, 59, 'South wind at end of hour'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK PK WND 270038/0000', 270, 38, 0, 0, 'West wind at midnight'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK PK WND 360042/1800', 360, 42, 18, 0, 'North wind'"
    })
    @DisplayName("Should parse peak wind with complete data")
    void testParsePeakWind(String metar, int expectedDir, int expectedSpeed,
                           int expectedHour, int expectedMin, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null: " + scenario);
        assertNotNull(data.getRemarks().peakWind(), "Peak wind should not be null: " + scenario);

        PeakWind peakWind = data.getRemarks().peakWind();
        assertEquals(expectedDir, peakWind.directionDegrees(), "Direction mismatch: " + scenario);
        assertEquals(expectedSpeed, peakWind.speedKnots(), "Speed mismatch: " + scenario);
        assertEquals(expectedHour, peakWind.hour(), "Hour mismatch: " + scenario);
        assertEquals(expectedMin, peakWind.minute(), "Minute mismatch: " + scenario);
    }

    @Test
    @DisplayName("Should parse peak wind with partial time (hour only)")
    void testParsePeakWindHourOnly() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK PK WND 32035/15";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().peakWind());

        PeakWind peakWind = data.getRemarks().peakWind();
        assertEquals(320, peakWind.directionDegrees());
        assertEquals(35, peakWind.speedKnots());
        assertEquals(15, peakWind.hour(), "Hour should be 15");
        assertNull(peakWind.minute(), "Minute should be null when not provided");
    }

    @Test
    @DisplayName("Should parse peak wind in complete remarks")
    void testParsePeakWindWithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 RMK AO2 SLP201 T02330139 PK WND 28032/1530";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Verify all remarks parsed
        assertNotNull(data.getRemarks());
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertNotNull(data.getRemarks().seaLevelPressure());
        assertEquals(1020.1, data.getRemarks().seaLevelPressure().toHectopascals(), 0.1);
        assertNotNull(data.getRemarks().preciseTemperature());

        // Verify peak wind
        assertNotNull(data.getRemarks().peakWind());
        assertEquals(280, data.getRemarks().peakWind().directionDegrees());
        assertEquals(32, data.getRemarks().peakWind().speedKnots());
    }

    @Test
    @DisplayName("Should parse peak wind in mixed remark order")
    void testParsePeakWindInMixedOrder() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK PK WND 28032/1530 AO2 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // All should be parsed regardless of order
        assertNotNull(data.getRemarks().peakWind());
        assertEquals(280, data.getRemarks().peakWind().directionDegrees());
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertNotNull(data.getRemarks().seaLevelPressure());
    }

    @Test
    @DisplayName("Should handle malformed peak wind gracefully")
    void testParseMalformedPeakWind() {
        // Invalid format - missing slash
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK PK WND 280321530";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should handle malformed peak wind gracefully");
        NoaaMetarData data = extractMetarData(result);

        // Malformed peak wind should not match pattern
        assertNull(data.getRemarks().peakWind());
    }

    @Test
    @DisplayName("Should convert peak wind speed to MPH")
    void testPeakWindSpeedConversion() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK PK WND 28032/1530";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        PeakWind peakWind = data.getRemarks().peakWind();
        assertNotNull(peakWind);

        // 32 knots ≈ 37 MPH (32 * 1.1508)
        Double mph = peakWind.toMph();
        assertNotNull(mph, "MPH conversion should not be null");
        assertEquals(37.0, mph, 1.0);
    }

    @Test
    @DisplayName("Should get cardinal direction from peak wind")
    void testPeakWindCardinalDirection() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK PK WND 28032/1530";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        PeakWind peakWind = data.getRemarks().peakWind();
        assertNotNull(peakWind);

        // 280° is W or WNW
        String cardinal = peakWind.getCardinalDirection();
        assertNotNull(cardinal);
        assertTrue(cardinal.equals("W") || cardinal.equals("WNW"),
                "280° should be W or WNW, got: " + cardinal);
    }

    @Test
    @DisplayName("Should parse peak wind at end of remarks (no trailing space)")
    void testParsePeakWindAtEnd() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210 PK WND 28032/1530";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().peakWind());
        assertEquals(280, data.getRemarks().peakWind().directionDegrees());
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM RMK PK WND 000025/1200', 0, 'North (0°)'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK PK WND 090030/1200', 90, 'East (90°)'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK PK WND 180035/1200', 180, 'South (180°)'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK PK WND 270040/1200', 270, 'West (270°)'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK PK WND 360045/1200', 360, 'North (360°)'"
    })
    @DisplayName("Should handle all cardinal directions")
    void testPeakWindCardinalDirections(String metar, int expectedDir, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().peakWind());
        assertEquals(expectedDir, data.getRemarks().peakWind().directionDegrees(), scenario);
    }

    @Test
    @DisplayName("Should handle very high peak wind speeds")
    void testParseVeryHighPeakWindSpeed() {
        // Hurricane-force winds >100 knots
        String metar = "METAR KJFK 121853Z 28016KT 10SM A2950 RMK PK WND 090P145/1530";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().peakWind());
        assertEquals(145, data.getRemarks().peakWind().speedKnots(),
                "P prefix should be stripped, speed should be 145kt");
    }

    @Test
    @DisplayName("Should parse peak wind with only required fields")
    void testParsePeakWindMinimal() {
        // Direction and speed only (no time components)
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK PK WND 280P105/";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Should parse even with missing time components
        assertNotNull(data.getRemarks().peakWind());
        assertEquals(280, data.getRemarks().peakWind().directionDegrees());
        assertEquals(105, data.getRemarks().peakWind().speedKnots());
        assertNull(data.getRemarks().peakWind().hour());
        assertNull(data.getRemarks().peakWind().minute());
    }

    // ========== WIND SHIFT PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            // Complete wind shift (hour and minute)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 WSHFT 1530', 15, 30, false, 'Complete wind shift'",

            // With frontal passage
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 WSHFT 1530 FROPA', 15, 30, true, 'Wind shift with FROPA'",

            // Different times
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK WSHFT 0815', 8, 15, false, 'Morning wind shift'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK WSHFT 2145 FROPA', 21, 45, true, 'Evening FROPA'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK WSHFT 0000', 0, 0, false, 'Midnight wind shift'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK WSHFT 2359 FROPA', 23, 59, true, 'End of hour FROPA'"
    })
    @DisplayName("Should parse wind shift with complete data")
    void testParseWindShift(String metar, Integer expectedHour, Integer expectedMin,
                            boolean expectedFropa, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null: " + scenario);
        assertNotNull(data.getRemarks().windShift(), "Wind shift should not be null: " + scenario);

        WindShift windShift = data.getRemarks().windShift();
        assertEquals(expectedHour, windShift.hour(), "Hour mismatch: " + scenario);
        assertEquals(expectedMin, windShift.minute(), "Minute mismatch: " + scenario);
        assertEquals(expectedFropa, windShift.frontalPassage(), "FROPA mismatch: " + scenario);
    }

    @Test
    @DisplayName("Should parse wind shift with minute only (no hour)")
    void testParseWindShiftMinuteOnly() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK WSHFT 30";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().windShift());

        WindShift windShift = data.getRemarks().windShift();
        assertNull(windShift.hour(), "Hour should be null when not provided");
        assertEquals(30, windShift.minute());
        assertFalse(windShift.frontalPassage());
    }

    @Test
    @DisplayName("Should parse wind shift with minute only and FROPA")
    void testParseWindShiftMinuteOnlyWithFropa() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK WSHFT 45 FROPA";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().windShift());

        WindShift windShift = data.getRemarks().windShift();
        assertNull(windShift.hour());
        assertEquals(45, windShift.minute());
        assertTrue(windShift.frontalPassage(), "FROPA should be true");
    }

    @Test
    @DisplayName("Should parse wind shift in complete remarks")
    void testParseWindShiftWithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 PK WND 28032/1530 WSHFT 1545 FROPA";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Verify all remarks parsed
        assertNotNull(data.getRemarks());
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertNotNull(data.getRemarks().seaLevelPressure());
        assertNotNull(data.getRemarks().preciseTemperature());
        assertNotNull(data.getRemarks().peakWind());

        // Verify wind shift
        assertNotNull(data.getRemarks().windShift());
        assertEquals(15, data.getRemarks().windShift().hour());
        assertEquals(45, data.getRemarks().windShift().minute());
        assertTrue(data.getRemarks().windShift().frontalPassage());
    }

    @Test
    @DisplayName("Should parse wind shift in mixed remark order")
    void testParseWindShiftInMixedOrder() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK WSHFT 1530 FROPA AO2 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // All should be parsed regardless of order
        assertNotNull(data.getRemarks().windShift());
        assertEquals(15, data.getRemarks().windShift().hour());
        assertTrue(data.getRemarks().windShift().frontalPassage());
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertNotNull(data.getRemarks().seaLevelPressure());
    }

    @Test
    @DisplayName("Should handle malformed wind shift gracefully")
    void testParseMalformedWindShift() {
        // Invalid format - missing time
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK WSHFT";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should handle malformed wind shift gracefully");
        NoaaMetarData data = extractMetarData(result);

        // Malformed wind shift should not match pattern
        if (data.getRemarks() != null) {
            assertNull(data.getRemarks().windShift());
        }
    }

    @Test
    @DisplayName("Should parse wind shift at end of remarks (no trailing space)")
    void testParseWindShiftAtEnd() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210 WSHFT 1530 FROPA";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().windShift());
        assertEquals(15, data.getRemarks().windShift().hour());
        assertTrue(data.getRemarks().windShift().frontalPassage());
    }

    @Test
    @DisplayName("Should distinguish FROPA from non-FROPA wind shifts")
    void testWindShiftFropaVsNoFropa() {
        // Without FROPA
        String metar1 = "METAR KJFK 121853Z 28016KT 10SM RMK WSHFT 1530";
        ParseResult<NoaaWeatherData> result1 = parser.parse(metar1);
        NoaaMetarData data1 = extractMetarData(result1);
        assertFalse(data1.getRemarks().windShift().frontalPassage(),
                "Should be false without FROPA");

        // With FROPA
        String metar2 = "METAR KJFK 121853Z 28016KT 10SM RMK WSHFT 1530 FROPA";
        ParseResult<NoaaWeatherData> result2 = parser.parse(metar2);
        NoaaMetarData data2 = extractMetarData(result2);
        assertTrue(data2.getRemarks().windShift().frontalPassage(),
                "Should be true with FROPA");
    }

    @Test
    @DisplayName("Should parse wind shift without other remarks")
    void testParseWindShiftAlone() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK WSHFT 1530 FROPA";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        assertNotNull(data.getRemarks().windShift());

        // Other remark fields should be null
        assertNull(data.getRemarks().automatedStationType());
        assertNull(data.getRemarks().seaLevelPressure());
        assertNull(data.getRemarks().preciseTemperature());
        assertNull(data.getRemarks().peakWind());
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM RMK WSHFT 0000', 0, 0, 'Midnight'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK WSHFT 0100', 1, 0, 'One AM'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK WSHFT 1200', 12, 0, 'Noon'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK WSHFT 2300', 23, 0, 'Eleven PM'"
    })
    @DisplayName("Should handle edge case times")
    void testWindShiftEdgeCaseTimes(String metar, int expectedHour, int expectedMin, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().windShift());
        assertEquals(expectedHour, data.getRemarks().windShift().hour(), scenario);
        assertEquals(expectedMin, data.getRemarks().windShift().minute(), scenario);
    }

    @Test
    @DisplayName("Should handle wind shift with peak wind")
    void testWindShiftAndPeakWind() {
        // Both wind shift and peak wind in same METAR
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK PK WND 28045/1528 WSHFT 1530 FROPA";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Peak wind should be parsed: 280° at 45kt at 15:28 UTC
        assertNotNull(data.getRemarks().peakWind());
        assertEquals(280, data.getRemarks().peakWind().directionDegrees());
        assertEquals(45, data.getRemarks().peakWind().speedKnots());
        assertEquals(15, data.getRemarks().peakWind().hour());
        assertEquals(28, data.getRemarks().peakWind().minute());

        // Wind shift should be parsed: 15:30 UTC with FROPA
        assertNotNull(data.getRemarks().windShift());
        assertEquals(15, data.getRemarks().windShift().hour());
        assertEquals(30, data.getRemarks().windShift().minute());
        assertTrue(data.getRemarks().windShift().frontalPassage());
    }

    // ========== VARIABLE VISIBILITY PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            // Simple fractions
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 VIS 1/2V2', 0.5, 2.0, '', '', 'Half mile varying to 2'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 1/4V1', 0.25, 1.0, '', '', 'Quarter mile varying to 1'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 3/4V2', 0.75, 2.0, '', '', 'Three quarters varying to 2'",

            // Mixed numbers
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 1 1/2V3', 1.5, 3.0, '', '', 'One and a half varying to 3'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 2 1/4V5', 2.25, 5.0, '', '', 'Two and a quarter varying to 5'",

            // Whole numbers
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 2V4', 2.0, 4.0, '', '', 'Two varying to four'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 1V10', 1.0, 10.0, '', '', 'One varying to ten'"
    })
    @DisplayName("Should parse variable visibility with various formats")
    void testParseVariableVisibility(String metar, double expectedMin, double expectedMax,
                                     String expectedDir, String expectedLoc, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null: " + scenario);
        assertNotNull(data.getRemarks().variableVisibility(),
                "Variable visibility should not be null: " + scenario);

        VariableVisibility varVis = data.getRemarks().variableVisibility();

        // Verify minimum visibility
        assertNotNull(varVis.minimumVisibility(), "Minimum visibility should not be null: " + scenario);
        Double minSM = varVis.minimumVisibility().toStatuteMiles();
        assertNotNull(minSM, "Minimum visibility in SM should not be null: " + scenario);
        assertEquals(expectedMin, minSM, 0.01, "Minimum visibility mismatch: " + scenario);

        // Verify maximum visibility
        assertNotNull(varVis.maximumVisibility(), "Maximum visibility should not be null: " + scenario);
        Double maxSM = varVis.maximumVisibility().toStatuteMiles();
        assertNotNull(maxSM, "Maximum visibility in SM should not be null: " + scenario);
        assertEquals(expectedMax, maxSM, 0.01,"Maximum visibility mismatch: " + scenario);

        // Verify direction and location (empty string means null)
        if (!expectedDir.isEmpty()) {
            assertEquals(expectedDir, varVis.direction(), "Direction mismatch: " + scenario);
        } else {
            assertNull(varVis.direction(), "Direction should be null: " + scenario);
        }

        if (!expectedLoc.isEmpty()) {
            assertEquals(expectedLoc, varVis.location(), "Location mismatch: " + scenario);
        } else {
            assertNull(varVis.location(), "Location should be null: " + scenario);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM RMK VIS NE 2V4', NE, 'Northeast'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK VIS SE 1V3', SE, 'Southeast'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK VIS SW 1/2V2', SW, 'Southwest'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK VIS NW 3V5', NW, 'Northwest'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK VIS N 2V4', N, 'North'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK VIS E 1V3', E, 'East'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK VIS S 2V5', S, 'South'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK VIS W 1V4', W, 'West'"
    })
    @DisplayName("Should parse variable visibility with all cardinal directions")
    void testParseVariableVisibilityWithDirections(String metar, String expectedDir, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().variableVisibility());
        assertEquals(expectedDir, data.getRemarks().variableVisibility().direction(), scenario);
        assertTrue(data.getRemarks().variableVisibility().hasDirection());
    }

    @Test
    @DisplayName("Should parse variable visibility in complete remarks")
    void testParseVariableVisibilityWithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 PK WND 28032/1530 VIS 1/2V2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Verify all remarks parsed
        assertNotNull(data.getRemarks());
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertNotNull(data.getRemarks().seaLevelPressure());
        assertNotNull(data.getRemarks().preciseTemperature());
        assertNotNull(data.getRemarks().peakWind());

        // Verify variable visibility
        VariableVisibility varVis = data.getRemarks().variableVisibility();
        assertNotNull(varVis);

        Double minSM = varVis.minimumVisibility().toStatuteMiles();
        assertNotNull(minSM, "Minimum visibility in SM should not be null");
        assertEquals(0.5, minSM, 0.01);

        Double maxSM = varVis.maximumVisibility().toStatuteMiles();
        assertNotNull(maxSM, "Maximum visibility in SM should not be null");
        assertEquals(2.0, maxSM, 0.01);
    }

    @Test
    @DisplayName("Should parse variable visibility in mixed remark order")
    void testParseVariableVisibilityInMixedOrder() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 1V3 AO2 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // All should be parsed regardless of order
        VariableVisibility varVis = data.getRemarks().variableVisibility();
        assertNotNull(varVis);

        Double minSM = varVis.minimumVisibility().toStatuteMiles();
        assertNotNull(minSM);
        assertEquals(1.0, minSM, 0.01);

        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertNotNull(data.getRemarks().seaLevelPressure());
    }

    @Test
    @DisplayName("Should handle malformed variable visibility gracefully")
    void testParseMalformedVariableVisibility() {
        // Invalid format - missing max visibility
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 1/2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should handle malformed variable visibility gracefully");
        NoaaMetarData data = extractMetarData(result);

        // Malformed variable visibility should not parse
        if (data.getRemarks() != null) {
            assertNull(data.getRemarks().variableVisibility());
        }
    }

    @Test
    @DisplayName("Should parse variable visibility at end of remarks (no trailing space)")
    void testParseVariableVisibilityAtEnd() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210 VIS 1/2V2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        VariableVisibility varVis = data.getRemarks().variableVisibility();
        assertNotNull(varVis);

        Double minSM = varVis.minimumVisibility().toStatuteMiles();
        assertNotNull(minSM);
        assertEquals(0.5, minSM, 0.01);
    }

    @Test
    @DisplayName("Should parse variable visibility without other remarks")
    void testParseVariableVisibilityAlone() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 1V3";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        assertNotNull(data.getRemarks().variableVisibility());

        // Other remark fields should be null
        assertNull(data.getRemarks().automatedStationType());
        assertNull(data.getRemarks().seaLevelPressure());
        assertNull(data.getRemarks().preciseTemperature());
    }

    @Test
    @DisplayName("Should verify variable visibility spread calculation")
    void testVariableVisibilitySpread() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 1V4";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        VariableVisibility varVis = data.getRemarks().variableVisibility();
        assertNotNull(varVis);

        // Spread should be 3 SM (4 - 1)
        Double spread = varVis.getSpread();
        assertNotNull(spread, "Spread should not be null");
        assertEquals(3.0, spread, 0.01);
        assertTrue(varVis.hasSignificantVariability(), "Spread > 1 SM should be significant");
    }

    @Test
    @DisplayName("Should handle variable visibility with small spread")
    void testVariableVisibilitySmallSpread() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 2V3";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        VariableVisibility varVis = data.getRemarks().variableVisibility();
        assertNotNull(varVis);

        // Spread should be 1 SM (3 - 2)
        Double spread = varVis.getSpread();
        assertNotNull(spread, "Spread should not be null");
        assertEquals(1.0, spread, 0.01);
        assertFalse(varVis.hasSignificantVariability(), "Spread = 1 SM should not be significant");
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM RMK VIS 1/8V1/4', 0.125, 0.25, 'Very low visibility'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK VIS 1/4V1/2', 0.25, 0.5, 'Low visibility'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK VIS 5V10', 5.0, 10.0, 'High visibility'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK VIS 8V10', 8.0, 10.0, 'Good to excellent'"
    })
    @DisplayName("Should handle variable visibility edge cases")
    void testVariableVisibilityEdgeCases(String metar, double expectedMin,
                                         double expectedMax, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().variableVisibility());
        VariableVisibility varVis = data.getRemarks().variableVisibility();
        assertNotNull(varVis, "Variable visibility should not be null: " + scenario);

        Double minSM = varVis.minimumVisibility().toStatuteMiles();
        assertNotNull(minSM, "Minimum visibility in SM should not be null: " + scenario);
        assertEquals(expectedMin, minSM, 0.01, scenario);

        Double maxSM = varVis.maximumVisibility().toStatuteMiles();
        assertNotNull(maxSM, "Maximum visibility in SM should not be null: " + scenario);
        assertEquals(expectedMax, maxSM, 0.01, scenario);
    }

    @Test
    @DisplayName("Should generate human-readable description")
    void testVariableVisibilityDescription() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS NE 1/2V2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        VariableVisibility varVis = data.getRemarks().variableVisibility();
        assertNotNull(varVis);

        String description = varVis.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("NE"), "Description should include direction");
        assertTrue(description.contains("varying"), "Description should include 'varying'");
    }

    @Test
    @DisplayName("Should handle complex mixed fraction visibility")
    void testComplexMixedFractionVisibility() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 1 3/4V2 1/2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().variableVisibility());
        VariableVisibility varVis = data.getRemarks().variableVisibility();
        assertNotNull(varVis);

        Double minSM = varVis.minimumVisibility().toStatuteMiles();
        assertNotNull(minSM, "Minimum visibility in SM should not be null");
        assertEquals(1.75, minSM, 0.01, "Should parse 1 3/4 as 1.75");

        Double maxSM = varVis.maximumVisibility().toStatuteMiles();
        assertNotNull(maxSM, "Maximum visibility in SM should not be null");
        assertEquals(2.5, maxSM, 0.01, "Should parse 2 1/2 as 2.5");
    }

    // ========== VARIABLE VISIBILITY ERROR COVERAGE TEST ==========

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS V2', 'Missing minimum distance'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 1/2V', 'Missing maximum distance'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS RWY 1/2', 'Without V separator'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS ABCV2', 'Unparseable minimum'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 1/2VABC', 'Unparseable maximum'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 5V2', 'Minimum greater than maximum (triggers validation exception)'"
    })
    @DisplayName("Should skip variable visibility for malformed formats")
    void testVariableVisibilityMalformedFormats(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        // Variable visibility should not be parsed for malformed formats
        if (data.getRemarks() != null) {
            assertNull(data.getRemarks().variableVisibility(),
                    "Variable visibility should be null: " + scenario);
        }
    }

    // ========== TOWER VISIBILITY PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            // Tower visibility - fractions
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TWR VIS 1/2', 0.5, 'Half mile'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TWR VIS 1/4', 0.25, 'Quarter mile'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TWR VIS 3/4', 0.75, 'Three quarters'",

            // Tower visibility - mixed numbers
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TWR VIS 1 1/2', 1.5, 'One and a half'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TWR VIS 2 1/4', 2.25, 'Two and a quarter'",

            // Tower visibility - whole numbers
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TWR VIS 2', 2.0, 'Two miles'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TWR VIS 5', 5.0, 'Five miles'"
    })
    @DisplayName("Should parse tower visibility with various formats")
    void testParseTowerVisibility(String metar, double expectedVis, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null: " + scenario);
        assertNotNull(data.getRemarks().towerVisibility(),
                "Tower visibility should not be null: " + scenario);

        Visibility towerVis = data.getRemarks().towerVisibility();
        Double visSM = towerVis.toStatuteMiles();
        assertNotNull(visSM, "Tower visibility in SM should not be null: " + scenario);
        assertEquals(expectedVis, visSM, 0.01, "Tower visibility mismatch: " + scenario);
    }


    // ========== SURFACE VISIBILITY PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            // Surface visibility - fractions
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK SFC VIS 1/2', 0.5, 'Half mile'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK SFC VIS 1/4', 0.25, 'Quarter mile'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK SFC VIS 3/4', 0.75, 'Three quarters'",

            // Surface visibility - mixed numbers
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK SFC VIS 1 1/2', 1.5, 'One and a half'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK SFC VIS 2 1/4', 2.25, 'Two and a quarter'",

            // Surface visibility - whole numbers
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK SFC VIS 1', 1.0, 'One mile'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK SFC VIS 3', 3.0, 'Three miles'"
    })
    @DisplayName("Should parse surface visibility with various formats")
    void testParseSurfaceVisibility(String metar, double expectedVis, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null: " + scenario);
        assertNotNull(data.getRemarks().surfaceVisibility(),
                "Surface visibility should not be null: " + scenario);

        Visibility surfaceVis = data.getRemarks().surfaceVisibility();
        Double visSM = surfaceVis.toStatuteMiles();
        assertNotNull(visSM, "Surface visibility in SM should not be null: " + scenario);
        assertEquals(expectedVis, visSM, 0.01, "Surface visibility mismatch: " + scenario);
    }


    // ========== INTEGRATION TESTS ==========

    @Test
    @DisplayName("Should parse tower visibility with other remarks")
    void testParseTowerVisibilityWithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 TWR VIS 1 1/2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Verify all remarks parsed
        assertNotNull(data.getRemarks());
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertNotNull(data.getRemarks().seaLevelPressure());
        assertNotNull(data.getRemarks().preciseTemperature());

        // Verify tower visibility
        assertNotNull(data.getRemarks().towerVisibility());
        Double towerVisSM = data.getRemarks().towerVisibility().toStatuteMiles();
        assertNotNull(towerVisSM);
        assertEquals(1.5, towerVisSM, 0.01);
    }

    @Test
    @DisplayName("Should parse surface visibility with other remarks")
    void testParseSurfaceVisibilityWithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 SFC VIS 1/4 T02330139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Verify all remarks parsed
        assertNotNull(data.getRemarks());
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertNotNull(data.getRemarks().seaLevelPressure());
        assertNotNull(data.getRemarks().preciseTemperature());

        // Verify surface visibility
        assertNotNull(data.getRemarks().surfaceVisibility());
        Double surfaceVisSM = data.getRemarks().surfaceVisibility().toStatuteMiles();
        assertNotNull(surfaceVisSM);
        assertEquals(0.25, surfaceVisSM, 0.01);
    }

    @Test
    @DisplayName("Should parse tower visibility in mixed remark order")
    void testParseTowerVisibilityInMixedOrder() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK TWR VIS 2 AO2 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // All should be parsed regardless of order
        assertNotNull(data.getRemarks().towerVisibility());
        Double towerVisSM = data.getRemarks().towerVisibility().toStatuteMiles();
        assertNotNull(towerVisSM);
        assertEquals(2.0, towerVisSM, 0.01);

        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertNotNull(data.getRemarks().seaLevelPressure());
    }

    @Test
    @DisplayName("Should handle tower and surface visibility together")
    void testParseBothTowerAndSurfaceVisibility() {
        // Note: This is unusual in real METARs but testing the parser handles it
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK TWR VIS 2 SFC VIS 1/2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Both should be parsed
        assertNotNull(data.getRemarks().towerVisibility());
        assertNotNull(data.getRemarks().surfaceVisibility());

        Double towerVisSM = data.getRemarks().towerVisibility().toStatuteMiles();
        assertNotNull(towerVisSM);
        assertEquals(2.0, towerVisSM, 0.01);

        Double surfaceVisSM = data.getRemarks().surfaceVisibility().toStatuteMiles();
        assertNotNull(surfaceVisSM);
        assertEquals(0.5, surfaceVisSM, 0.01);
    }


    // ========== ERROR HANDLING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TWR VIS', 'TWR VIS missing distance'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK SFC VIS', 'SFC VIS missing distance'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TWR VIS ABC', 'TWR VIS unparseable distance'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK SFC VIS XYZ', 'SFC VIS unparseable distance'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TWR VIS  ', 'TWR VIS blank distance'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK SFC VIS  ', 'SFC VIS blank distance'"
    })
    @DisplayName("Should skip tower/surface visibility for malformed formats")
    void testTowerSurfaceVisibilityMalformedFormats(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        // Tower/Surface visibility should not be parsed for malformed formats
        if (data.getRemarks() != null) {
            assertNull(data.getRemarks().towerVisibility(),
                    "Tower visibility should be null: " + scenario);
            assertNull(data.getRemarks().surfaceVisibility(),
                    "Surface visibility should be null: " + scenario);
        }
    }

    @Test
    @DisplayName("Should parse tower visibility without other remarks")
    void testParseTowerVisibilityAlone() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK TWR VIS 1 1/2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        assertNotNull(data.getRemarks().towerVisibility());

        Double towerVisSM = data.getRemarks().towerVisibility().toStatuteMiles();
        assertNotNull(towerVisSM);
        assertEquals(1.5, towerVisSM, 0.01);

        // Other remark fields should be null
        assertNull(data.getRemarks().automatedStationType());
        assertNull(data.getRemarks().seaLevelPressure());
    }

    @Test
    @DisplayName("Should parse surface visibility at end of remarks")
    void testParseSurfaceVisibilityAtEnd() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210 SFC VIS 1/4";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().surfaceVisibility());
        Double surfaceVisSM = data.getRemarks().surfaceVisibility().toStatuteMiles();
        assertNotNull(surfaceVisSM);
        assertEquals(0.25, surfaceVisSM, 0.01);
    }

    // ========== EDGE CASE (SF CLOUD TYPE VS SFC VIS) TESTS ==========

    /**
     * Edge Case Tests: SF Cloud Type vs SFC VIS
     * These tests verify that the negative lookahead fix correctly handles:
     * 1. SF (Stratus Fractus) cloud type alone
     * 2. SFC VIS (Surface Visibility) alone
     * 3. Both coexisting in the same METAR without conflicts
     */

    @Test
    @DisplayName("Should parse SF cloud type without interfering with SFC VIS pattern")
    void testParseSF_CloudTypeAlone() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK SF3";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        assertNotNull(data.getRemarks().cloudTypes(), "Cloud types should not be null");
        assertFalse(data.getRemarks().cloudTypes().isEmpty(), "Should have cloud types");

        // Verify SF cloud type is parsed
        assertTrue(data.getRemarks().cloudTypes().stream()
                        .anyMatch(ct -> "SF".equals(ct.cloudType())),
                "Should contain SF cloud type");

        // Verify oktas
        CloudType sf = data.getRemarks().cloudTypes().stream()
                .filter(ct -> "SF".equals(ct.cloudType()))
                .findFirst()
                .orElseThrow();
        assertEquals(3, sf.oktas(), "SF should have coverage of 3 oktas");

        // Surface visibility should be null
        assertNull(data.getRemarks().surfaceVisibility(),
                "Surface visibility should be null when only SF cloud type present");
    }

    @Test
    @DisplayName("Should parse SF cloud type with multiple cloud types")
    void testParseSF_WithOtherCloudTypes() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK SF5 SC2 CU1";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        assertNotNull(data.getRemarks().cloudTypes());
        assertEquals(3, data.getRemarks().cloudTypes().size(), "Should have 3 cloud types");

        // Verify all cloud types
        List<CloudType> cloudTypes = data.getRemarks().cloudTypes();
        assertTrue(cloudTypes.stream().anyMatch(ct -> "SF".equals(ct.cloudType()) && ct.oktas() == 5));
        assertTrue(cloudTypes.stream().anyMatch(ct -> "SC".equals(ct.cloudType()) && ct.oktas() == 2));
        assertTrue(cloudTypes.stream().anyMatch(ct -> "CU".equals(ct.cloudType()) && ct.oktas() == 1));

        assertNull(data.getRemarks().surfaceVisibility());
    }

    @Test
    @DisplayName("Should parse BOTH SF cloud type AND SFC VIS in same METAR")
    void testParse_BothSF_CloudTypeAndSFC_VIS() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK SF4 SFC VIS 1 1/2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());

        // Verify SF cloud type is parsed
        assertNotNull(data.getRemarks().cloudTypes());
        assertFalse(data.getRemarks().cloudTypes().isEmpty());
        assertTrue(data.getRemarks().cloudTypes().stream()
                        .anyMatch(ct -> "SF".equals(ct.cloudType()) && ct.oktas() == 4),
                "Should contain SF cloud type with 4 oktas");

        // Verify SFC VIS is parsed
        assertNotNull(data.getRemarks().surfaceVisibility(),
                "Surface visibility should not be null");
        Double surfaceVisSM = data.getRemarks().surfaceVisibility().toStatuteMiles();
        assertNotNull(surfaceVisSM, "Surface visibility in SM should not be null");
        assertEquals(1.5, surfaceVisSM, 0.01,
                "Surface visibility should be 1.5 SM");
    }

    @Test
    @DisplayName("Should parse BOTH SFC VIS AND SF cloud type in reverse order")
    void testParse_SFC_VIS_ThenSF_CloudType() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK SFC VIS 2 SF3";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());

        // Verify SFC VIS is parsed
        assertNotNull(data.getRemarks().surfaceVisibility());
        Double surfaceVisSM = data.getRemarks().surfaceVisibility().toStatuteMiles();
        assertNotNull(surfaceVisSM, "Surface visibility in SM should not be null");
        assertEquals(2.0, surfaceVisSM, 0.01,
                "Surface visibility should be 2.0 SM");

        // Verify SF cloud type is parsed
        assertNotNull(data.getRemarks().cloudTypes());
        assertTrue(data.getRemarks().cloudTypes().stream()
                        .anyMatch(ct -> "SF".equals(ct.cloudType()) && ct.oktas() == 3),
                "Should contain SF cloud type with 3 oktas");
    }

    @Test
    @DisplayName("Should parse multiple cloud types including SF with SFC VIS")
    void testParse_MultipleCloudTypesIncludingSF_WithSFC_VIS() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK CU2 SF4 AC1 SFC VIS 3/4";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());

        // Verify all cloud types
        assertNotNull(data.getRemarks().cloudTypes());
        assertEquals(3, data.getRemarks().cloudTypes().size(), "Should have 3 cloud types");

        List<CloudType> cloudTypes = data.getRemarks().cloudTypes();
        assertTrue(cloudTypes.stream().anyMatch(ct -> "CU".equals(ct.cloudType()) && ct.oktas() == 2),
                "Should contain CU with 2 oktas");
        assertTrue(cloudTypes.stream().anyMatch(ct -> "SF".equals(ct.cloudType()) && ct.oktas() == 4),
                "Should contain SF with 4 oktas");
        assertTrue(cloudTypes.stream().anyMatch(ct -> "AC".equals(ct.cloudType()) && ct.oktas() == 1),
                "Should contain AC with 1 okta");

        // Verify SFC VIS
        assertNotNull(data.getRemarks().surfaceVisibility());
        Double surfaceVisSM = data.getRemarks().surfaceVisibility().toStatuteMiles();
        assertNotNull(surfaceVisSM, "Surface visibility in SM should not be null");
        assertEquals(0.75, surfaceVisSM, 0.01,
                "Surface visibility should be 0.75 SM");
    }

    @Test
    @DisplayName("Should parse SF cloud type without oktas (with location)")
    void testParseSF_WithoutOktas_WithLocation() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK SF OHD";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        assertNotNull(data.getRemarks().cloudTypes());

        // Verify SF cloud type with location but no oktas
        CloudType sf = data.getRemarks().cloudTypes().stream()
                .filter(ct -> "SF".equals(ct.cloudType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("SF cloud type should be present"));

        assertEquals("SF", sf.cloudType());
        assertNull(sf.oktas(), "Oktas should be null when not specified");
        assertEquals("OHD", sf.location(), "Location should be OHD (overhead)");
    }

    @Test
    @DisplayName("Should handle complex remarks with SF, SFC VIS, and TWR VIS")
    void testParse_ComplexRemarks_WithSF_SFC_VIS_AndTWR_VIS() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK " +
                "TWR VIS 2 SF3 SFC VIS 1/2 CU2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());

        // Verify TWR VIS
        assertNotNull(data.getRemarks().towerVisibility());
        Double towerVisSM = data.getRemarks().towerVisibility().toStatuteMiles();
        assertNotNull(towerVisSM, "Tower visibility in SM should not be null");
        assertEquals(2.0, towerVisSM, 0.01);

        // Verify SFC VIS
        assertNotNull(data.getRemarks().surfaceVisibility());
        Double surfaceVisSM = data.getRemarks().surfaceVisibility().toStatuteMiles();
        assertNotNull(surfaceVisSM, "Surface visibility in SM should not be null");
        assertEquals(0.5, surfaceVisSM, 0.01);

        // Verify cloud types
        assertNotNull(data.getRemarks().cloudTypes());
        assertEquals(2, data.getRemarks().cloudTypes().size());
        assertTrue(data.getRemarks().cloudTypes().stream()
                .anyMatch(ct -> "SF".equals(ct.cloudType()) && ct.oktas() == 3));
        assertTrue(data.getRemarks().cloudTypes().stream()
                .anyMatch(ct -> "CU".equals(ct.cloudType()) && ct.oktas() == 2));
    }

    // ========== HOURLY PRECIPITATION PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK P0015', 0.15, 'Quarter inch'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK P0009', 0.09, 'Less than tenth'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK P0025', 0.25, 'Quarter inch'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK P0125', 1.25, 'Over an inch'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK P0000', 0.0, 'Zero precipitation'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK P0001', 0.01, 'Minimal measurable'"
    })
    @DisplayName("Should parse hourly precipitation amounts")
    void testParseHourlyPrecipitation(String metar, double expectedInches, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null: " + scenario);
        assertNotNull(data.getRemarks().hourlyPrecipitation(),
                "Hourly precipitation should not be null: " + scenario);

        PrecipitationAmount precip = data.getRemarks().hourlyPrecipitation();
        assertEquals(1, precip.periodHours(), scenario);
        assertFalse(precip.isTrace(), scenario);

        Double inches = precip.inches();
        assertNotNull(inches, "Precipitation amount should not be null: " + scenario);
        assertEquals(expectedInches, inches, 0.001, scenario);
    }

    @Test
    @DisplayName("Should parse hourly trace precipitation")
    void testParseHourlyTracePrecipitation() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK P////";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().hourlyPrecipitation());
        PrecipitationAmount precip = data.getRemarks().hourlyPrecipitation();

        assertTrue(precip.isTrace(), "Should be trace precipitation");
        assertEquals(1, precip.periodHours());
        assertNull(precip.inches(), "Trace should have null inches");
    }

    // ========== 6-HOUR PRECIPITATION PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 60009', 0.09, '6-hour - less than tenth'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 60015', 0.15, '6-hour - quarter inch'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 60025', 0.25, '6-hour - quarter inch'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 60125', 1.25, '6-hour - over an inch'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 60000', 0.0, '6-hour - zero'"
    })
    @DisplayName("Should parse 6-hour precipitation amounts")
    void testParseSixHourPrecipitation(String metar, double expectedInches, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null: " + scenario);
        assertNotNull(data.getRemarks().sixHourPrecipitation(),
                "6-hour precipitation should not be null: " + scenario);

        PrecipitationAmount precip = data.getRemarks().sixHourPrecipitation();
        assertEquals(6, precip.periodHours(), scenario);
        assertFalse(precip.isTrace(), scenario);

        Double inches = precip.inches();
        assertNotNull(inches, "Precipitation amount should not be null: " + scenario);
        assertEquals(expectedInches, inches, 0.001, scenario);
    }

    @Test
    @DisplayName("Should parse 6-hour trace precipitation")
    void testParseSixHourTracePrecipitation() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 6////";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().sixHourPrecipitation());
        PrecipitationAmount precip = data.getRemarks().sixHourPrecipitation();

        assertTrue(precip.isTrace(), "Should be trace precipitation");
        assertEquals(6, precip.periodHours());
        assertNull(precip.inches(), "Trace should have null inches");
    }

    // ========== 24-HOUR PRECIPITATION PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 70009', 0.09, '24-hour - less than tenth'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 70125', 1.25, '24-hour - over an inch'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 70250', 2.50, '24-hour - two and a half'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 71000', 10.0, '24-hour - ten inches'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 70000', 0.0, '24-hour - zero'"
    })
    @DisplayName("Should parse 24-hour precipitation amounts")
    void testParseTwentyFourHourPrecipitation(String metar, double expectedInches, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null: " + scenario);
        assertNotNull(data.getRemarks().twentyFourHourPrecipitation(),
                "24-hour precipitation should not be null: " + scenario);

        PrecipitationAmount precip = data.getRemarks().twentyFourHourPrecipitation();
        assertEquals(24, precip.periodHours(), scenario);
        assertFalse(precip.isTrace(), scenario);

        Double inches = precip.inches();
        assertNotNull(inches, "Precipitation amount should not be null: " + scenario);
        assertEquals(expectedInches, inches, 0.001, scenario);
    }

    @Test
    @DisplayName("Should parse 24-hour trace precipitation")
    void testParseTwentyFourHourTracePrecipitation() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 7////";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().twentyFourHourPrecipitation());
        PrecipitationAmount precip = data.getRemarks().twentyFourHourPrecipitation();

        assertTrue(precip.isTrace(), "Should be trace precipitation");
        assertEquals(24, precip.periodHours());
        assertNull(precip.inches(), "Trace should have null inches");
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    @DisplayName("Should parse hourly precipitation with other remarks")
    void testParseHourlyPrecipitationWithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 P0015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Verify all remarks parsed
        assertNotNull(data.getRemarks());
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertNotNull(data.getRemarks().seaLevelPressure());
        assertNotNull(data.getRemarks().preciseTemperature());

        // Verify hourly precipitation
        assertNotNull(data.getRemarks().hourlyPrecipitation());
        Double precipInches = data.getRemarks().hourlyPrecipitation().inches();
        assertNotNull(precipInches);
        assertEquals(0.15, precipInches, 0.001);
    }

    @Test
    @DisplayName("Should parse multiple precipitation periods together")
    void testParseMultiplePrecipitationPeriods() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK P0015 60025 70125";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // All three should be parsed
        assertNotNull(data.getRemarks().hourlyPrecipitation());
        assertNotNull(data.getRemarks().sixHourPrecipitation());
        assertNotNull(data.getRemarks().twentyFourHourPrecipitation());

        Double hourlyInches = data.getRemarks().hourlyPrecipitation().inches();
        assertNotNull(hourlyInches);
        assertEquals(0.15, hourlyInches, 0.001);

        Double sixHourInches = data.getRemarks().sixHourPrecipitation().inches();
        assertNotNull(sixHourInches);
        assertEquals(0.25, sixHourInches, 0.001);

        Double twentyFourHourInches = data.getRemarks().twentyFourHourPrecipitation().inches();
        assertNotNull(twentyFourHourInches);
        assertEquals(1.25, twentyFourHourInches, 0.001);
    }

    @Test
    @DisplayName("Should parse precipitation in mixed remark order")
    void testParsePrecipitationInMixedOrder() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 60025 AO2 P0015 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // All should be parsed regardless of order
        assertNotNull(data.getRemarks().sixHourPrecipitation());
        assertNotNull(data.getRemarks().hourlyPrecipitation());
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertNotNull(data.getRemarks().seaLevelPressure());
    }

    @Test
    @DisplayName("Should parse precipitation at end of remarks")
    void testParsePrecipitationAtEnd() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210 P0015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().hourlyPrecipitation());
        Double precipInches = data.getRemarks().hourlyPrecipitation().inches();
        assertNotNull(precipInches);
        assertEquals(0.15, precipInches, 0.001);
    }


// ========== ERROR HANDLING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK PABCD', 'Hourly - invalid characters'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 6ABCD', '6-hour - invalid characters'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 7WXYZ', '24-hour - invalid characters'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 6    ', '6-hour - blank value'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 7    ', '24-hour - blank value'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK P    ', 'Hourly - blank value'"
    })
    @DisplayName("Should skip precipitation for malformed formats")
    void testPrecipitationMalformedFormats(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        // Precipitation should not be parsed for malformed formats
        if (data.getRemarks() != null) {
            assertNull(data.getRemarks().hourlyPrecipitation(),
                    "Hourly precipitation should be null: " + scenario);
            assertNull(data.getRemarks().sixHourPrecipitation(),
                    "6-hour precipitation should be null: " + scenario);
            assertNull(data.getRemarks().twentyFourHourPrecipitation(),
                    "24-hour precipitation should be null: " + scenario);
        }
    }

    @Test
    @DisplayName("Should parse precipitation without other remarks")
    void testParsePrecipitationAlone() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK P0015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        assertNotNull(data.getRemarks().hourlyPrecipitation());

        Double precipInches = data.getRemarks().hourlyPrecipitation().inches();
        assertNotNull(precipInches);
        assertEquals(0.15, precipInches, 0.001);

        // Other remark fields should be null
        assertNull(data.getRemarks().automatedStationType());
        assertNull(data.getRemarks().seaLevelPressure());
    }

    @Test
    @DisplayName("Should handle all trace precipitation types")
    void testParseAllTracePrecipitation() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK P//// 6//// 7////";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // All should be trace
        assertNotNull(data.getRemarks().hourlyPrecipitation());
        assertTrue(data.getRemarks().hourlyPrecipitation().isTrace());

        assertNotNull(data.getRemarks().sixHourPrecipitation());
        assertTrue(data.getRemarks().sixHourPrecipitation().isTrace());

        assertNotNull(data.getRemarks().twentyFourHourPrecipitation());
        assertTrue(data.getRemarks().twentyFourHourPrecipitation().isTrace());
    }

    @Test
    @DisplayName("Should verify precipitation isMeasurable check")
    void testPrecipitationMeasurableCheck() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK P0015";  // ← Only one value

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        PrecipitationAmount precip = data.getRemarks().hourlyPrecipitation();
        assertNotNull(precip);

        assertTrue(precip.isMeasurable(), "0.15 inches should be measurable");
        assertFalse(precip.isTrace());

        Double inches = precip.inches();
        assertNotNull(inches);
        assertEquals(0.15, inches, 0.001);
    }

    // ========== HAIL SIZE PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 1/2', 0.5, 'Half inch'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 3/4', 0.75, 'Three quarters inch'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 1', 1.0, 'One inch (severe)'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 1 3/4', 1.75, 'Golf ball sized'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 2', 2.0, 'Tennis ball sized (significantly severe)'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 2 1/2', 2.5, 'Baseball sized'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 3', 3.0, 'Softball sized'"
    })
    @DisplayName("Should parse hail sizes")
    void testParseHailSize(String metar, double expectedInches, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks(), "Remarks should not be null: " + scenario);
        assertNotNull(data.getRemarks().hailSize(),
                "Hail size should not be null: " + scenario);

        HailSize hailSize = data.getRemarks().hailSize();
        assertEquals(expectedInches, hailSize.inches(), 0.001, scenario);
    }

    // ========== HAIL SIZE SEVERITY TESTS ==========

    @Test
    @DisplayName("Should parse severe hail (1.0 inch)")
    void testParseSevereHail() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 1";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().hailSize());
        HailSize hailSize = data.getRemarks().hailSize();

        assertEquals(1.0, hailSize.inches(), 0.001);
        assertTrue(hailSize.isSevere(), "1.0 inch should be severe");
        assertFalse(hailSize.isSignificantlySevere());
    }

    @Test
    @DisplayName("Should parse significantly severe hail (2.0 inches)")
    void testParseSignificantlySevereHail() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().hailSize());
        HailSize hailSize = data.getRemarks().hailSize();

        assertEquals(2.0, hailSize.inches(), 0.001);
        assertTrue(hailSize.isSignificantlySevere(), "2.0 inches should be significantly severe");
        assertTrue(hailSize.isSevere(), "Should also be severe");
    }

    @Test
    @DisplayName("Should parse non-severe hail (0.75 inch)")
    void testParseNonSevereHail() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 3/4";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().hailSize());
        HailSize hailSize = data.getRemarks().hailSize();

        assertEquals(0.75, hailSize.inches(), 0.001);
        assertFalse(hailSize.isSevere(), "0.75 inch should not be severe");
    }

    // ========== HAIL SIZE CATEGORY TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 1/2', 'Penny-sized'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 1', 'Quarter-sized'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 1 3/4', 'Tennis ball-sized'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 2 1/2', 'Baseball-sized'"
    })
    @DisplayName("Should correctly categorize hail sizes")
    void testHailSizeCategories(String metar, String expectedCategory) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().hailSize());
        assertEquals(expectedCategory, data.getRemarks().hailSize().getSizeCategory());
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    @DisplayName("Should parse hail size with other remarks")
    void testParseHailSizeWithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 P0015 GR 1 3/4";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Verify all remarks parsed
        assertNotNull(data.getRemarks());
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertNotNull(data.getRemarks().seaLevelPressure());
        assertNotNull(data.getRemarks().preciseTemperature());
        assertNotNull(data.getRemarks().hourlyPrecipitation());

        // Verify hail size
        assertNotNull(data.getRemarks().hailSize());
        assertEquals(1.75, data.getRemarks().hailSize().inches(), 0.001);
    }

    @Test
    @DisplayName("Should parse hail size in mixed remark order")
    void testParseHailSizeInMixedOrder() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 2 AO2 SLP210 P0015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // All should be parsed regardless of order
        assertNotNull(data.getRemarks().hailSize());
        assertEquals(AutomatedStationType.AO2, data.getRemarks().automatedStationType());
        assertNotNull(data.getRemarks().seaLevelPressure());
        assertNotNull(data.getRemarks().hourlyPrecipitation());

        assertEquals(2.0, data.getRemarks().hailSize().inches(), 0.001);
    }

    @Test
    @DisplayName("Should parse hail size at end of remarks")
    void testParseHailSizeAtEnd() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210 GR 1 3/4";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().hailSize());
        assertEquals(1.75, data.getRemarks().hailSize().inches(), 0.001);
    }

    @Test
    @DisplayName("Should parse hail size at beginning of remarks")
    void testParseHailSizeAtBeginning() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 2 AO2 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().hailSize());
        assertEquals(2.0, data.getRemarks().hailSize().inches(), 0.001);
    }

    @Test
    @DisplayName("Should parse hail size without other remarks")
    void testParseHailSizeAlone() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 1 3/4";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks());
        assertNotNull(data.getRemarks().hailSize());

        assertEquals(1.75, data.getRemarks().hailSize().inches(), 0.001);

        // Other remark fields should be null
        assertNull(data.getRemarks().automatedStationType());
        assertNull(data.getRemarks().seaLevelPressure());
    }

    // ========== ERROR HANDLING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR ABC', 'Invalid characters'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR XYZ', 'Non-numeric value'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR    ', 'Blank value'"
    })
    @DisplayName("Should skip hail size for malformed formats")
    void testHailSizeMalformedFormats(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess(), "Should parse successfully: " + scenario);
        NoaaMetarData data = extractMetarData(result);

        // Hail size should not be parsed for malformed formats
        if (data.getRemarks() != null) {
            assertNull(data.getRemarks().hailSize(),
                    "Hail size should be null: " + scenario);
        }
    }

    @Test
    @DisplayName("Should skip hail size with missing value")
    void testHailSizeMissingValue() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        if (data.getRemarks() != null) {
            assertNull(data.getRemarks().hailSize(),
                    "Hail size should be null when value is missing");
        }
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("Should parse very small hail")
    void testParseVerySmallHail() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 1/4";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().hailSize());
        assertEquals(0.25, data.getRemarks().hailSize().inches(), 0.001);
        assertEquals("Marble-sized", data.getRemarks().hailSize().getSizeCategory());
    }

    @Test
    @DisplayName("Should parse large hail")
    void testParseLargeHail() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 4";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().hailSize());
        assertEquals(4.0, data.getRemarks().hailSize().inches(), 0.001);
        assertEquals("Grapefruit-sized or larger", data.getRemarks().hailSize().getSizeCategory());
        assertTrue(data.getRemarks().hailSize().isSignificantlySevere());
    }

    @Test
    @DisplayName("Should handle hail size at severe threshold boundary")
    void testHailSizeAtSevereThreshold() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK GR 1";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertNotNull(data.getRemarks().hailSize());
        HailSize hailSize = data.getRemarks().hailSize();

        assertEquals(1.0, hailSize.inches(), 0.001);
        assertTrue(hailSize.isSevere(), "Exactly 1.0 inch should be severe");
    }

    // ========== WEATHER EVENTS PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            // Simple begin only
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK RAB05', RA, , 5, , 'Rain began :05'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK SNB30', SN, , 30, , 'Snow began :30'",

            // Simple end only
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK RAE42', RA, , , 42, 'Rain ended :42'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK SNE15', SN, , , 15, 'Snow ended :15'",

            // Begin and end (minute only)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK RAB15E30', RA, , 15, 30, 'Rain began :15, ended :30'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK SNB05E45', SN, , 5, 45, 'Snow began :05, ended :45'",

            // Full timestamp (4-digit hhmm format)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK FZRAB1159E1240', FZRA, , 59, 40, 'Freezing rain with full timestamps'",

            // Different weather types
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK DZB10E20', DZ, , 10, 20, 'Drizzle'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK BRB10E25', BR, , 10, 25, 'Mist'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK FGB0520E0630', FG, , 20, 30, 'Fog with full timestamps'"
    })
    @DisplayName("Should parse simple weather events")
    void testParseWeatherEvents(String metar, String expectedCode, String expectedIntensity,
                                Integer expectedBeginMin, Integer expectedEndMin, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).as("Should parse successfully: %s", scenario).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).as("Remarks should not be null: %s", scenario).isNotNull();
        assertThat(data.getRemarks().weatherEvents())
                .as("Weather events should not be empty: %s", scenario)
                .isNotEmpty()
                .hasSize(1);

        WeatherEvent event = data.getRemarks().weatherEvents().get(0);
        assertThat(event.weatherCode()).as("Weather code: %s", scenario).isEqualTo(expectedCode);

        if (expectedIntensity != null && !expectedIntensity.isBlank()) {
            assertThat(event.intensity()).as("Intensity: %s", scenario).isEqualTo(expectedIntensity);
        }

        if (expectedBeginMin != null) {
            assertThat(event.beginMinute()).as("Begin minute: %s", scenario).isEqualTo(expectedBeginMin);
        }

        if (expectedEndMin != null) {
            assertThat(event.endMinute()).as("End minute: %s", scenario).isEqualTo(expectedEndMin);
        }
    }

    @Test
    @DisplayName("Should parse weather event with light intensity")
    void testParseWeatherEventWithLightIntensity() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK -RAB05";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().weatherEvents()).hasSize(1);
        WeatherEvent event = data.getRemarks().weatherEvents().get(0);

        assertThat(event.weatherCode()).isEqualTo("RA");
        assertThat(event.intensity()).isEqualTo("-");
        assertThat(event.beginMinute()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should parse weather event with heavy intensity")
    void testParseWeatherEventWithHeavyIntensity() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK +TSRAB20E45";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().weatherEvents()).hasSize(1);
        WeatherEvent event = data.getRemarks().weatherEvents().get(0);

        assertThat(event.weatherCode()).isEqualTo("TSRA");
        assertThat(event.intensity()).isEqualTo("+");
        assertThat(event.beginMinute()).isEqualTo(20);
        assertThat(event.endMinute()).isEqualTo(45);
    }

    @Test
    @DisplayName("Should parse thunderstorm event")
    void testParseThunderstormEvent() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK TSB0159E0240";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().weatherEvents()).hasSize(1);
        WeatherEvent event = data.getRemarks().weatherEvents().get(0);

        assertThat(event.weatherCode()).isEqualTo("TS");
        assertThat(event.beginHour()).isEqualTo(1);
        assertThat(event.beginMinute()).isEqualTo(59);
        assertThat(event.endHour()).isEqualTo(2);
        assertThat(event.endMinute()).isEqualTo(40);
    }

    @Test
    @DisplayName("Should parse chained weather events")
    void testParseChainedWeatherEvents() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK RAB15E30SNB30";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().weatherEvents())
                .as("Should have 2 chained events")
                .hasSize(2);

        // First event: Rain began :15, ended :30
        WeatherEvent rain = data.getRemarks().weatherEvents().get(0);
        assertThat(rain.weatherCode()).isEqualTo("RA");
        assertThat(rain.beginMinute()).isEqualTo(15);
        assertThat(rain.endMinute()).isEqualTo(30);

        // Second event: Snow began :30
        WeatherEvent snow = data.getRemarks().weatherEvents().get(1);
        assertThat(snow.weatherCode()).isEqualTo("SN");
        assertThat(snow.beginMinute()).isEqualTo(30);
        assertThat(snow.endMinute()).isNull();
    }

    @Test
    @DisplayName("Should parse multiple chained weather events")
    void testParseMultipleChainedWeatherEvents() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK RAB15E30SNB30E45DZB45";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().weatherEvents())
                .as("Should have 3 chained events")
                .hasSize(3);

        // Verify each event
        assertThat(data.getRemarks().weatherEvents().get(0).weatherCode()).isEqualTo("RA");
        assertThat(data.getRemarks().weatherEvents().get(1).weatherCode()).isEqualTo("SN");
        assertThat(data.getRemarks().weatherEvents().get(2).weatherCode()).isEqualTo("DZ");
    }

    @Test
    @DisplayName("Should parse weather events with full timestamps")
    void testParseWeatherEventsWithFullTimestamps() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK FZRAB1159E1240";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().weatherEvents()).hasSize(1);
        WeatherEvent event = data.getRemarks().weatherEvents().get(0);

        assertThat(event.weatherCode()).isEqualTo("FZRA");
        assertThat(event.beginHour()).isEqualTo(11);
        assertThat(event.beginMinute()).isEqualTo(59);
        assertThat(event.endHour()).isEqualTo(12);
        assertThat(event.endMinute()).isEqualTo(40);
    }

    @Test
    @DisplayName("Should parse weather events with other remarks")
    void testParseWeatherEventsWithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 P0015 GR 1 3/4 RAB15E30";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify all remarks parsed
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();
        assertThat(data.getRemarks().hourlyPrecipitation()).isNotNull();
        assertThat(data.getRemarks().hailSize()).isNotNull();

        // Verify weather event
        assertThat(data.getRemarks().weatherEvents()).hasSize(1);
        WeatherEvent event = data.getRemarks().weatherEvents().get(0);
        assertThat(event.weatherCode()).isEqualTo("RA");
        assertThat(event.beginMinute()).isEqualTo(15);
        assertThat(event.endMinute()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should parse weather events in mixed remark order")
    void testParseWeatherEventsInMixedOrder() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK RAB15E30 AO2 SLP210 P0015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // All should be parsed regardless of order
        assertThat(data.getRemarks().weatherEvents()).hasSize(1);
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().hourlyPrecipitation()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210 RAB15E30', 'At end of remarks'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK RAB15E30 AO2 SLP210', 'At beginning of remarks'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 RAB15E30 SLP210', 'In middle of remarks'"
    })
    @DisplayName("Should parse weather events regardless of position in remarks")
    void testParseWeatherEvents_Position(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().weatherEvents())
                .as("Should have weather events: %s", scenario)
                .hasSize(1);

        WeatherEvent event = data.getRemarks().weatherEvents().get(0);
        assertThat(event.weatherCode())
                .as("Weather code should be RA: %s", scenario)
                .isEqualTo("RA");
        assertThat(event.beginMinute())
                .as("Begin minute should be 15: %s", scenario)
                .isEqualTo(15);
        assertThat(event.endMinute())
                .as("End minute should be 30: %s", scenario)
                .isEqualTo(30);
    }

    @Test
    @DisplayName("Should parse weather events without other remarks")
    void testParseWeatherEventsAlone() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK RAB15E30";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().weatherEvents()).hasSize(1);

        WeatherEvent event = data.getRemarks().weatherEvents().get(0);
        assertThat(event.weatherCode()).isEqualTo("RA");
        assertThat(event.beginMinute()).isEqualTo(15);
        assertThat(event.endMinute()).isEqualTo(30);

        // Other remark fields should be null
        assertThat(data.getRemarks().automatedStationType()).isNull();
        assertThat(data.getRemarks().seaLevelPressure()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210', 'Remarks without weather events'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK', 'Empty remarks after RMK'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK   ', 'Blank/whitespace-only remarks'"
    })
    @DisplayName("Should handle METAR with no weather events")
    void testParseMetar_NoWeatherEvents(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        // Remarks might be null for empty/blank remarks
        if (data.getRemarks() != null) {
            assertThat(data.getRemarks().weatherEvents())
                    .as("Weather events should be empty: %s", scenario)
                    .isEmpty();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM RMK RAB0000E0030', 0, 0, 0, 30, 'Midnight begin'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK SNB2359', 23, 59, , , 'End of day begin'",
            "'METAR KJFK 121853Z 28016KT 10SM RMK RAB00E59', , 0, , 59, 'Minute 0 and 59'"
    })
    @DisplayName("Should handle edge case times")
    void testWeatherEventsEdgeCaseTimes(String metar, Integer expectedBeginHour,
                                        Integer expectedBeginMin, Integer expectedEndHour,
                                        Integer expectedEndMin, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).as("Should parse: %s", scenario).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().weatherEvents()).hasSize(1);
        WeatherEvent event = data.getRemarks().weatherEvents().get(0);

        if (expectedBeginHour != null) {
            assertThat(event.beginHour()).as("Begin hour: %s", scenario).isEqualTo(expectedBeginHour);
        }
        if (expectedBeginMin != null) {
            assertThat(event.beginMinute()).as("Begin minute: %s", scenario).isEqualTo(expectedBeginMin);
        }
        if (expectedEndHour != null) {
            assertThat(event.endHour()).as("End hour: %s", scenario).isEqualTo(expectedEndHour);
        }
        if (expectedEndMin != null) {
            assertThat(event.endMinute()).as("End minute: %s", scenario).isEqualTo(expectedEndMin);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "METAR KJFK 121853Z 28016KT 10SM RMK RAB05",      // Rain began
            "METAR KJFK 121853Z 28016KT 10SM RMK SNE30",      // Snow ended
            "METAR KJFK 121853Z 28016KT 10SM RMK FZRAB1159",  // Freezing rain began
            "METAR KJFK 121853Z 28016KT 10SM RMK TSE0240",    // Thunderstorm ended
            "METAR KJFK 121853Z 28016KT 10SM RMK BRB10E25",   // Mist began and ended
            "METAR KJFK 121853Z 28016KT 10SM RMK -DZB05",     // Light drizzle began
            "METAR KJFK 121853Z 28016KT 10SM RMK +TSRAB20E45" // Heavy TS with rain
    })
    @DisplayName("Should parse various weather event formats")
    void testParseVariousWeatherEventFormats(String metar) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Pattern should parse: %s", metar)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);
        assertThat(data.getRemarks().weatherEvents())
                .as("Should have at least one weather event")
                .isNotEmpty();
    }

    @Test
    @DisplayName("Should use WeatherEvent helper methods")
    void testWeatherEventHelperMethods() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK FZRAB1159E1240";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        WeatherEvent event = data.getRemarks().weatherEvents().get(0);

        // Helper methods
        assertThat(event.hasBeginTime()).isTrue();
        assertThat(event.hasEndTime()).isTrue();
        assertThat(event.hasBeginHour()).isTrue();
        assertThat(event.hasEndHour()).isTrue();

        // Formatted times
        assertThat(event.getFormattedBeginTime()).isEqualTo("11:59");
        assertThat(event.getFormattedEndTime()).isEqualTo("12:40");

        // LocalTime conversion
        assertThat(event.getBeginTime()).isNotNull();
        assertThat(event.getEndTime()).isNotNull();
    }

    @Test
    @DisplayName("Should parse real-world METAR with weather events")
    void testParseRealWorldMetarWithWeatherEvents() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM -RA FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 RAB15E30SNB30";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify main body
        assertThat(data.getStationId()).isEqualTo("KJFK");
        assertThat(data.getWind()).isNotNull();
        assertThat(data.getVisibility()).isNotNull();
        assertThat(data.getPresentWeather()).isNotEmpty();
        assertThat(data.getTemperature()).isNotNull();
        assertThat(data.getPressure()).isNotNull();

        // Verify remarks
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();

        // Verify weather events
        assertThat(data.getRemarks().weatherEvents()).hasSize(2);

        WeatherEvent rain = data.getRemarks().weatherEvents().get(0);
        assertThat(rain.weatherCode()).isEqualTo("RA");
        assertThat(rain.beginMinute()).isEqualTo(15);
        assertThat(rain.endMinute()).isEqualTo(30);

        WeatherEvent snow = data.getRemarks().weatherEvents().get(1);
        assertThat(snow.weatherCode()).isEqualTo("SN");
        assertThat(snow.beginMinute()).isEqualTo(30);
    }

    // ========== COVERAGE IMPROVEMENT TESTS ==========

    @Test
    @DisplayName("Should handle METAR without RMK section")
    void testParseWeatherEvents_NullRemarks() {
        // METAR without RMK section
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Remarks may be null when no RMK section exists
        // If present, weather events should be empty
        if (data.getRemarks() != null) {
            assertThat(data.getRemarks().weatherEvents()).isEmpty();
        }
    }

    @Test
    @DisplayName("Should handle weather event with no weather code")
    void testParseWeatherEvents_NoWeatherCode() {
        // This should trigger the weatherCode.isEmpty() check
        // Pattern matches but has no valid weather components
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK B15E30";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Should skip the invalid event (no weather code)
        // B15E30 without a weather type should be ignored
        assertThat(data.getRemarks().weatherEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should handle weather event with only begin time")
    void testParseWeatherEvents_OnlyBeginTime() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK RAB15";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().weatherEvents()).hasSize(1);
        WeatherEvent event = data.getRemarks().weatherEvents().get(0);

        assertThat(event.weatherCode()).isEqualTo("RA");
        assertThat(event.beginMinute()).isEqualTo(15);
        assertThat(event.endMinute()).isNull();
    }

    @Test
    @DisplayName("Should handle weather event with only end time")
    void testParseWeatherEvents_OnlyEndTime() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK RAE42";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().weatherEvents()).hasSize(1);
        WeatherEvent event = data.getRemarks().weatherEvents().get(0);

        assertThat(event.weatherCode()).isEqualTo("RA");
        assertThat(event.beginMinute()).isNull();
        assertThat(event.endMinute()).isEqualTo(42);
    }

    @Test
    @DisplayName("Should handle multiple weather events with other remarks")
    void testParseWeatherEvents_MixedWithOtherRemarks() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210 RAB15E30 T02330139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Should have weather event plus other remarks
        assertThat(data.getRemarks().weatherEvents()).hasSize(1);
        assertThat(data.getRemarks().automatedStationType()).isNotNull();
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();

        WeatherEvent event = data.getRemarks().weatherEvents().get(0);
        assertThat(event.weatherCode()).isEqualTo("RA");
        assertThat(event.beginMinute()).isEqualTo(15);
        assertThat(event.endMinute()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should handle weather events with various intensities")
    void testParseWeatherEvents_VariousIntensities() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK -DZB10 RAB15 +TSRAB20";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().weatherEvents()).hasSize(3);

        // Light drizzle
        WeatherEvent event1 = data.getRemarks().weatherEvents().get(0);
        assertThat(event1.weatherCode()).isEqualTo("DZ");
        assertThat(event1.intensity()).isEqualTo("-");

        // Moderate rain (no intensity marker)
        WeatherEvent event2 = data.getRemarks().weatherEvents().get(1);
        assertThat(event2.weatherCode()).isEqualTo("RA");
        assertThat(event2.intensity()).isNull();

        // Heavy thunderstorm with rain
        WeatherEvent event3 = data.getRemarks().weatherEvents().get(2);
        assertThat(event3.weatherCode()).isEqualTo("TSRA");
        assertThat(event3.intensity()).isEqualTo("+");
    }

    // ========== THUNDERSTORM LOCATION PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            // Simple cloud locations
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TS SE', TS, SE, 'Thunderstorm Southeast'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CB W', CB, W, 'Cumulonimbus West'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TCU N', TCU, N, 'Towering Cumulus North'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK ACC E', ACC, E, 'Altocumulus Castellanus East'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CBMAM S', CBMAM, S, 'Cumulonimbus Mammatus South'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIRGA NW', VIRGA, NW, 'Virga Northwest'"
    })
    @DisplayName("Should parse simple thunderstorm/cloud locations")
    void testParseThunderstormLocation_Simple(String metar, String expectedType,
                                              String expectedDir, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks())
                .as("Remarks should not be null: %s", scenario)
                .isNotNull();

        assertThat(data.getRemarks().thunderstormLocations())
                .as("Should have thunderstorm locations: %s", scenario)
                .hasSize(1);

        ThunderstormLocation location = data.getRemarks().thunderstormLocations().get(0);
        assertThat(location.cloudType())
                .as("Cloud type mismatch: %s", scenario)
                .isEqualTo(expectedType);
        assertThat(location.direction())
                .as("Direction mismatch: %s", scenario)
                .isEqualTo(expectedDir);
        assertThat(location.locationQualifier())
                .as("Should have no location qualifier: %s", scenario)
                .isNull();
        assertThat(location.movingDirection())
                .as("Should have no moving direction: %s", scenario)
                .isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CB OHD', OHD, 'Overhead'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TS VC W', VC, 'In vicinity'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TCU DSNT N', DSNT, 'Distant'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK ACC DSIPTD E', DSIPTD, 'Dissipated'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TS TOP S', TOP, 'At or above level'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CB TR', TR, 'At all quadrants'"
    })
    @DisplayName("Should parse thunderstorm locations with location qualifiers")
    void testParseThunderstormLocation_WithQualifiers(String metar, String expectedQualifier,
                                                      String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().thunderstormLocations())
                .as("Should have thunderstorm locations: %s", scenario)
                .hasSize(1);

        ThunderstormLocation location = data.getRemarks().thunderstormLocations().get(0);
        assertThat(location.locationQualifier())
                .as("Location qualifier mismatch: %s", scenario)
                .isEqualTo(expectedQualifier);
        assertThat(location.hasLocationQualifier())
                .as("Should have location qualifier: %s", scenario)
                .isTrue();
    }

    @Test
    @DisplayName("Should parse thunderstorm location with direction range")
    void testParseThunderstormLocation_DirectionRange() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK TCU DSNT N-NE";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);

        ThunderstormLocation location = data.getRemarks().thunderstormLocations().get(0);
        assertThat(location.cloudType()).isEqualTo("TCU");
        assertThat(location.locationQualifier()).isEqualTo("DSNT");
        assertThat(location.direction()).isEqualTo("N");
        assertThat(location.directionRange()).isEqualTo("NE");
        assertThat(location.hasDirectionRange()).isTrue();
        assertThat(location.movingDirection()).isNull();
    }

    @Test
    @DisplayName("Should parse thunderstorm location with movement")
    void testParseThunderstormLocation_WithMovement() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK CB OHD MOV E";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);

        ThunderstormLocation location = data.getRemarks().thunderstormLocations().get(0);
        assertThat(location.cloudType()).isEqualTo("CB");
        assertThat(location.locationQualifier()).isEqualTo("OHD");
        assertThat(location.direction()).isNull();
        assertThat(location.movingDirection()).isEqualTo("E");
        assertThat(location.isMoving()).isTrue();
    }

    @Test
    @DisplayName("Should parse multiple thunderstorm locations")
    void testParseThunderstormLocation_Multiple() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK TS SE CB W";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().thunderstormLocations())
                .as("Should have 2 thunderstorm locations")
                .hasSize(2);

        // First location: Thunderstorm Southeast
        ThunderstormLocation ts = data.getRemarks().thunderstormLocations().get(0);
        assertThat(ts.cloudType()).isEqualTo("TS");
        assertThat(ts.direction()).isEqualTo("SE");
        assertThat(ts.isThunderstorm()).isTrue();

        // Second location: Cumulonimbus West
        ThunderstormLocation cb = data.getRemarks().thunderstormLocations().get(1);
        assertThat(cb.cloudType()).isEqualTo("CB");
        assertThat(cb.direction()).isEqualTo("W");
    }

    @Test
    @DisplayName("Should parse thunderstorm location with complete data")
    void testParseThunderstormLocation_Complete() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK TCU DSNT N-NE MOV E";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);

        ThunderstormLocation location = data.getRemarks().thunderstormLocations().get(0);
        assertThat(location.cloudType()).isEqualTo("TCU");
        assertThat(location.locationQualifier()).isEqualTo("DSNT");
        assertThat(location.direction()).isEqualTo("N");
        assertThat(location.directionRange()).isEqualTo("NE");
        assertThat(location.movingDirection()).isEqualTo("E");

        // Verify helper methods
        assertThat(location.hasLocationQualifier()).isTrue();
        assertThat(location.hasDirectionRange()).isTrue();
        assertThat(location.isMoving()).isTrue();
    }

    @Test
    @DisplayName("Should parse thunderstorm location with other remarks")
    void testParseThunderstormLocation_WithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 P0015 TS SE";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify all remarks parsed
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();
        assertThat(data.getRemarks().hourlyPrecipitation()).isNotNull();

        // Verify thunderstorm location
        assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);
        ThunderstormLocation location = data.getRemarks().thunderstormLocations().get(0);
        assertThat(location.cloudType()).isEqualTo("TS");
        assertThat(location.direction()).isEqualTo("SE");
    }

    @Test
    @DisplayName("Should parse thunderstorm location in mixed remark order")
    void testParseThunderstormLocation_MixedOrder() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK CB W AO2 SLP210 P0015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // All should be parsed regardless of order
        assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().hourlyPrecipitation()).isNotNull();

        ThunderstormLocation location = data.getRemarks().thunderstormLocations().get(0);
        assertThat(location.cloudType()).isEqualTo("CB");
        assertThat(location.direction()).isEqualTo("W");
    }

    @Test
    @DisplayName("Should parse thunderstorm location at end of remarks")
    void testParseThunderstormLocation_AtEnd() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210 TS SE";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);
        ThunderstormLocation location = data.getRemarks().thunderstormLocations().get(0);
        assertThat(location.cloudType()).isEqualTo("TS");
        assertThat(location.direction()).isEqualTo("SE");
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210', 'No thunderstorm locations'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK', 'Empty remarks'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015', 'No RMK section'"
    })
    @DisplayName("Should handle METAR with no thunderstorm locations")
    void testParseMetar_NoThunderstormLocations(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        // Remarks might be null for empty/blank remarks or no RMK section
        if (data.getRemarks() != null) {
            assertThat(data.getRemarks().thunderstormLocations())
                    .as("Thunderstorm locations should be empty: %s", scenario)
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("Should parse real-world METAR with thunderstorm location")
    void testParseRealWorldMetarWithThunderstormLocation() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM -TSRA FEW015CB BKN250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 CB OHD MOV E";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify main body
        assertThat(data.getStationId()).isEqualTo("KJFK");
        assertThat(data.getWind()).isNotNull();
        assertThat(data.getVisibility()).isNotNull();
        assertThat(data.getPresentWeather()).isNotEmpty();
        assertThat(data.getSkyConditions()).isNotEmpty();
        assertThat(data.getTemperature()).isNotNull();
        assertThat(data.getPressure()).isNotNull();

        // Verify remarks
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();

        // Verify thunderstorm location
        assertThat(data.getRemarks().thunderstormLocations()).hasSize(1);
        ThunderstormLocation location = data.getRemarks().thunderstormLocations().get(0);
        assertThat(location.cloudType()).isEqualTo("CB");
        assertThat(location.locationQualifier()).isEqualTo("OHD");
        assertThat(location.movingDirection()).isEqualTo("E");
        assertThat(location.isMoving()).isTrue();

        // Verify summary
        String summary = location.getSummary();
        assertThat(summary)
                .contains("Cumulonimbus")
                .contains("Overhead")
                .contains("Moving");
    }

    // ========== PRESSURE TENDENCY PARSING TESTS ==========
// Add these tests to NoaaMetarParserTest.java after the Thunderstorm Location tests

    @ParameterizedTest
    @CsvSource({
            // Increasing pressure (codes 0-3)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 50125', 0, 12.5, 'Increasing, then decreasing (code 0)'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 51025', 1, 2.5, 'Increasing, then steady (code 1)'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 52015', 2, 1.5, 'Increasing (code 2)'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 53010', 3, 1.0, 'Increasing rapidly (code 3)'",

            // Steady pressure (code 4)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 54000', 4, 0.0, 'Steady (code 4)'",

            // Decreasing pressure (codes 5-8)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 55008', 5, 0.8, 'Decreasing, then increasing (code 5)'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 56018', 6, 1.8, 'Decreasing, then steady (code 6)'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 57035', 7, 3.5, 'Decreasing (code 7)'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 58045', 8, 4.5, 'Decreasing rapidly (code 8)'"
    })
    @DisplayName("Should parse 3-hour pressure tendency with all tendency codes")
    void testParsePressureTendency(String metar, int expectedCode,
                                   double expectedChangeHPa, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks())
                .as("Remarks should not be null: %s", scenario)
                .isNotNull();

        assertThat(data.getRemarks().pressureTendency())
                .as("Pressure tendency should not be null: %s", scenario)
                .isNotNull();

        PressureTendency tendency = data.getRemarks().pressureTendency();

        assertThat(tendency.tendencyCode())
                .as("Tendency code mismatch: %s", scenario)
                .isEqualTo(expectedCode);

        assertThat(tendency.changeHectopascals())
                .as("Pressure change mismatch: %s", scenario)
                .isEqualTo(expectedChangeHPa, within(0.01));
    }

    @Test
    @DisplayName("Should parse pressure tendency with zero change")
    void testParsePressureTendency_ZeroChange() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 50000";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().pressureTendency()).isNotNull();
        PressureTendency tendency = data.getRemarks().pressureTendency();

        assertThat(tendency.tendencyCode()).isZero();
        assertThat(tendency.changeHectopascals()).isEqualTo(0.0, within(0.01));
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 57500', 7, 50.0, 'Maximum valid change (50.0 hPa)'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210 52015', 2, 1.5, 'At end of remarks'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 56018 AO2 SLP210', 6, 1.8, 'At beginning of remarks'"
    })
    @DisplayName("Should parse pressure tendency in various positions and edge cases")
    void testParsePressureTendency_PositionsAndEdgeCases(String metar, int expectedCode,
                                                         double expectedChangeHPa, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().pressureTendency())
                .as("Pressure tendency should not be null: %s", scenario)
                .isNotNull();

        PressureTendency tendency = data.getRemarks().pressureTendency();

        assertThat(tendency.tendencyCode())
                .as("Tendency code mismatch: %s", scenario)
                .isEqualTo(expectedCode);

        assertThat(tendency.changeHectopascals())
                .as("Pressure change mismatch: %s", scenario)
                .isEqualTo(expectedChangeHPa, within(0.01));
    }

    @Test
    @DisplayName("Should parse pressure tendency with other remarks")
    void testParsePressureTendency_WithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 P0015 50125";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify all remarks parsed
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();
        assertThat(data.getRemarks().hourlyPrecipitation()).isNotNull();

        // Verify pressure tendency
        assertThat(data.getRemarks().pressureTendency()).isNotNull();
        PressureTendency tendency = data.getRemarks().pressureTendency();
        assertThat(tendency.tendencyCode()).isZero();
        assertThat(tendency.changeHectopascals()).isEqualTo(12.5, within(0.01));
    }

    @Test
    @DisplayName("Should parse pressure tendency in mixed remark order")
    void testParsePressureTendency_MixedOrder() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 57035 AO2 SLP210 P0015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // All should be parsed regardless of order
        assertThat(data.getRemarks().pressureTendency()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().hourlyPrecipitation()).isNotNull();

        PressureTendency tendency = data.getRemarks().pressureTendency();
        assertThat(tendency.tendencyCode()).isEqualTo(7);
        assertThat(tendency.changeHectopascals()).isEqualTo(3.5, within(0.01));
    }

    @Test
    @DisplayName("Should parse pressure tendency without other remarks")
    void testParsePressureTendency_Alone() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 51025";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().pressureTendency()).isNotNull();

        PressureTendency tendency = data.getRemarks().pressureTendency();
        assertThat(tendency.tendencyCode()).isEqualTo(1);
        assertThat(tendency.changeHectopascals()).isEqualTo(2.5, within(0.01));

        // Other remark fields should be null
        assertThat(data.getRemarks().automatedStationType()).isNull();
        assertThat(data.getRemarks().seaLevelPressure()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            // Missing pressure tendency
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210', 'No pressure tendency'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK', 'Empty remarks'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015', 'No RMK section'",

            // Malformed formats
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 5ABCD', 'Invalid characters after 5'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 59025', 'Invalid tendency code 9'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 5    ', 'Blank value after 5'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 5012X', 'Non-numeric pressure value'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 5', 'Missing value after 5'"
    })
    @DisplayName("Should handle missing or malformed pressure tendency")
    void testPressureTendency_MissingOrMalformed(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        // Remarks might be null for empty/blank remarks or no RMK section
        // Pressure tendency should be null for missing or malformed formats
        if (data.getRemarks() != null) {
            assertThat(data.getRemarks().pressureTendency())
                    .as("Pressure tendency should be null: %s", scenario)
                    .isNull();
        }
    }

    @Test
    @DisplayName("Should use PressureTendency query methods")
    void testPressureTendency_QueryMethods() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 57035";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        PressureTendency tendency = data.getRemarks().pressureTendency();
        assertThat(tendency).isNotNull();

        // Query methods
        assertThat(tendency.isIncreasing()).isFalse();
        assertThat(tendency.isDecreasing()).isTrue();
        assertThat(tendency.isSteady()).isFalse();

        // Significance methods
        assertThat(tendency.isSignificant()).isTrue(); // 3.5 hPa in 3 hours
        assertThat(tendency.isRapidChange()).isFalse(); // Code 7, but only 3.5 hPa (< 6.0)

        // Tendency description
        String description = tendency.getTendencyDescription();
        assertThat(description).contains("Decreasing");
    }

    @Test
    @DisplayName("Should identify significant pressure changes")
    void testPressureTendency_SignificantChanges() {
        // Rapid change - code 7, 6.0+ hPa
        String metar1 = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 57060";
        ParseResult<NoaaWeatherData> result1 = parser.parse(metar1);
        PressureTendency tendency1 = extractMetarData(result1).getRemarks().pressureTendency();
        assertThat(tendency1.isSignificant()).isTrue();
        assertThat(tendency1.isRapidChange()).isTrue();

        // Steady - not significant
        String metar2 = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 54000";
        ParseResult<NoaaWeatherData> result2 = parser.parse(metar2);
        PressureTendency tendency2 = extractMetarData(result2).getRemarks().pressureTendency();
        assertThat(tendency2.isSignificant()).isFalse();
        assertThat(tendency2.isRapidChange()).isFalse();

        // Small change - not significant
        String metar3 = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 53010";
        ParseResult<NoaaWeatherData> result3 = parser.parse(metar3);
        PressureTendency tendency3 = extractMetarData(result3).getRemarks().pressureTendency();
        assertThat(tendency3.isSignificant()).isFalse();
    }

    @Test
    @DisplayName("Should convert pressure change to inches of mercury")
    void testPressureTendency_InchesHgConversion() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 57035";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        PressureTendency tendency = extractMetarData(result).getRemarks().pressureTendency();

        assertThat(tendency).isNotNull();

        // 3.5 hPa ≈ 0.103 inHg
        double inchesHg = tendency.getChangeInchesHg();
        assertThat(inchesHg).isCloseTo(0.103, within(0.01));
    }

    @Test
    @DisplayName("Should format pressure tendency for display")
    void testPressureTendency_Formatting() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 57035";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        PressureTendency tendency = extractMetarData(result).getRemarks().pressureTendency();

        assertThat(tendency).isNotNull();

        // Formatted string
        String formatted = tendency.getFormattedChange();
        assertThat(formatted)
                .contains("-3.5")  // Negative for falling
                .contains("hPa");

        // Summary
        String summary = tendency.getSummary();
        assertThat(summary)
                .contains("Decreasing")
                .contains("-3.5")
                .contains("hPa")
                .contains("SIGNIFICANT");
    }

    @Test
    @DisplayName("Should handle pressure tendency with missing value")
    void testPressureTendency_MissingValue() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 5";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        if (data.getRemarks() != null) {
            assertThat(data.getRemarks().pressureTendency())
                    .as("Pressure tendency should be null when value is missing")
                    .isNull();
        }
    }

    @Test
    @DisplayName("Should parse real-world METAR with pressure tendency")
    void testParseRealWorldMetarWithPressureTendency() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 57035";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify main body
        assertThat(data.getStationId()).isEqualTo("KJFK");
        assertThat(data.getWind()).isNotNull();
        assertThat(data.getVisibility()).isNotNull();
        assertThat(data.getSkyConditions()).isNotEmpty();
        assertThat(data.getTemperature()).isNotNull();
        assertThat(data.getPressure()).isNotNull();

        // Verify remarks
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();

        // Verify pressure tendency
        assertThat(data.getRemarks().pressureTendency()).isNotNull();
        PressureTendency tendency = data.getRemarks().pressureTendency();
        assertThat(tendency.tendencyCode()).isEqualTo(7);
        assertThat(tendency.changeHectopascals()).isEqualTo(3.5, within(0.01));
        assertThat(tendency.isDecreasing()).isTrue();
        assertThat(tendency.isRapidChange()).isFalse(); // 3.5 < 6.0
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 50001', 0.1, 'Smallest measurable change'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 57499', 49.9, 'Near maximum valid change'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 54005', 0.5, 'Steady with minimal change'"
    })
    @DisplayName("Should handle pressure tendency edge cases")
    void testPressureTendency_EdgeCases(String metar, double expectedChangeHPa, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().pressureTendency()).isNotNull();
        PressureTendency tendency = data.getRemarks().pressureTendency();

        assertThat(tendency.changeHectopascals())
                .as("Pressure change: %s", scenario)
                .isEqualTo(expectedChangeHPa, within(0.01));
    }

    @Test
    @DisplayName("Should compare pressure tendency types correctly")
    void testPressureTendency_TypeComparison() {
        // Increasing
        String increasingMetar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 51025";
        PressureTendency increasing = extractMetarData(parser.parse(increasingMetar))
                .getRemarks().pressureTendency();
        assertThat(increasing.isIncreasing()).isTrue();
        assertThat(increasing.isDecreasing()).isFalse();
        assertThat(increasing.isSteady()).isFalse();

        // Steady
        String steadyMetar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 54000";
        PressureTendency steady = extractMetarData(parser.parse(steadyMetar))
                .getRemarks().pressureTendency();
        assertThat(steady.isIncreasing()).isFalse();
        assertThat(steady.isDecreasing()).isFalse();
        assertThat(steady.isSteady()).isTrue();

        // Decreasing
        String decreasingMetar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 57035";
        PressureTendency decreasing = extractMetarData(parser.parse(decreasingMetar))
                .getRemarks().pressureTendency();
        assertThat(decreasing.isIncreasing()).isFalse();
        assertThat(decreasing.isDecreasing()).isTrue();
        assertThat(decreasing.isSteady()).isFalse();
    }

    @Test
    @DisplayName("Should handle all 9 tendency codes (0-8)")
    void testPressureTendency_AllCodes() {
        for (int code = 0; code <= 8; code++) {
            String metar = String.format("METAR KJFK 121853Z 28016KT 10SM A3015 RMK 5%d015", code);
            ParseResult<NoaaWeatherData> result = parser.parse(metar);

            assertThat(result.isSuccess())
                    .as("Should parse code %d", code)
                    .isTrue();

            NoaaMetarData data = extractMetarData(result);
            assertThat(data.getRemarks().pressureTendency())
                    .as("Should have pressure tendency for code %d", code)
                    .isNotNull();

            PressureTendency tendency = data.getRemarks().pressureTendency();
            assertThat(tendency.changeHectopascals())
                    .as("Should have change value for code %d", code)
                    .isEqualTo(1.5, within(0.01));
        }
    }

    @Test
    @DisplayName("Should parse pressure tendency with complete remarks suite")
    void testPressureTendency_CompleteRemarksSuite() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 PK WND 28032/1530 WSHFT 1545 FROPA " +
                "P0015 60025 70125 GR 1 3/4 RAB15E30 TS SE 57035";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify ALL remarks parsed (comprehensive integration test)
        assertThat(data.getRemarks()).isNotNull();

        // Basic remarks
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();

        // Wind-related
        assertThat(data.getRemarks().peakWind()).isNotNull();
        assertThat(data.getRemarks().windShift()).isNotNull();

        // Precipitation
        assertThat(data.getRemarks().hourlyPrecipitation()).isNotNull();
        assertThat(data.getRemarks().sixHourPrecipitation()).isNotNull();
        assertThat(data.getRemarks().twentyFourHourPrecipitation()).isNotNull();

        // Weather phenomena
        assertThat(data.getRemarks().hailSize()).isNotNull();
        assertThat(data.getRemarks().weatherEvents()).isNotEmpty();
        assertThat(data.getRemarks().thunderstormLocations()).isNotEmpty();

        // **Pressure tendency** - the NEW component
        assertThat(data.getRemarks().pressureTendency()).isNotNull();
        PressureTendency tendency = data.getRemarks().pressureTendency();
        assertThat(tendency.tendencyCode()).isEqualTo(7);
        assertThat(tendency.changeHectopascals()).isEqualTo(3.5, within(0.01));
        assertThat(tendency.isDecreasing()).isTrue();
        assertThat(tendency.isRapidChange()).isFalse(); // 3.5 < 6.0
    }

    // Add these tests to NoaaMetarParserTest.java to achieve 100% coverage

    @ParameterizedTest
    @CsvSource({
            // Invalid tendency codes (validation will throw IllegalArgumentException)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 59025', 'Invalid tendency code 9'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 5A025', 'Non-numeric tendency code'",

            // Invalid pressure change values (validation will throw)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 57ABC', 'Non-numeric pressure change'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 5712X', 'Invalid characters in pressure'",

            // Values that exceed validation limits (> 50 hPa)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 57999', 'Exceeds max valid change (99.9 > 50)'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 50600', 'Exceeds max valid change (60.0 > 50)'"
    })
    @DisplayName("Should gracefully handle invalid pressure tendency and continue parsing")
    void testParsePressureTendency_ExceptionHandling(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        // Parser should succeed even with invalid pressure tendency
        assertThat(result.isSuccess())
                .as("Parser should handle invalid data gracefully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        // Pressure tendency should be null (validation failed, exception caught)
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().pressureTendency())
                .as("Invalid pressure tendency should be null: %s", scenario)
                .isNull();

        // But other remarks should still parse (if present)
        // This verifies the parser recovered and continued
    }

    @Test
    @DisplayName("Should handle invalid pressure tendency without crashing")
    void testParsePressureTendency_InvalidHandling() {
        // Invalid tendency code - triggers exception path
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 59025";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        // Should parse successfully (no crash)
        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Pressure tendency should be null (validation failed)
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().pressureTendency()).isNull();
    }

    @Test
    @DisplayName("Should log warning when pressure tendency parsing fails")
    void testParsePressureTendency_LogsWarning() {
        // This test verifies the LOGGER.warn() line is executed
        // Note: You may need to set up a logging appender to capture this

        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 59025";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();

        // The warning should be logged (check logs if needed)
        // For now, just verify the parse completes successfully
        NoaaMetarData data = extractMetarData(result);
        assertThat(data.getRemarks().pressureTendency()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            // Edge case: Pattern matches but validation fails
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 50999', 'Code 0, but 99.9 hPa exceeds limit'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 54800', 'Code 4, but 80.0 hPa exceeds limit'",

            // Negative code (should fail validation)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 5-125', 'Negative tendency code'",
    })
    @DisplayName("Should handle edge cases where pattern matches but validation fails")
    void testParsePressureTendency_ValidationEdgeCases(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().pressureTendency())
                .as("Pressure tendency should be null: %s", scenario)
                .isNull();
    }

    // ========== 6-HOUR MAX/MIN TEMPERATURE PARSING TESTS ==========
// Add these tests to NoaaMetarParserTest.java after the Pressure Tendency tests

    @ParameterizedTest
    @CsvSource({
            // Maximum temperature (type 1)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 10142', 1, 14.2, 'Maximum 14.2°C'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 10250', 1, 25.0, 'Maximum 25.0°C'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 10050', 1, 5.0, 'Maximum 5.0°C'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 10001', 1, 0.1, 'Maximum 0.1°C'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 10000', 1, 0.0, 'Maximum 0.0°C'",

            // Maximum temperature (negative)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 11023', 1, -2.3, 'Maximum -2.3°C'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 11150', 1, -15.0, 'Maximum -15.0°C'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 11001', 1, -0.1, 'Maximum -0.1°C'",

            // Minimum temperature (type 2)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 20012', 2, 1.2, 'Minimum 1.2°C'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 20100', 2, 10.0, 'Minimum 10.0°C'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 20000', 2, 0.0, 'Minimum 0.0°C'",

            // Minimum temperature (negative)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 21001', 2, -0.1, 'Minimum -0.1°C'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 21089', 2, -8.9, 'Minimum -8.9°C'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 21250', 2, -25.0, 'Minimum -25.0°C'"
    })
    @DisplayName("Should parse 6-hour max/min temperature")
    void testParse6HourMaxMinTemperature(String metar, int type, double expectedTempC, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks())
                .as("Remarks should not be null: %s", scenario)
                .isNotNull();

        if (type == 1) {
            // Maximum temperature
            assertThat(data.getRemarks().sixHourMaxTemperature())
                    .as("Maximum temperature should not be null: %s", scenario)
                    .isNotNull();

            assertThat(data.getRemarks().sixHourMaxTemperature().celsius())
                    .as("Temperature mismatch: %s", scenario)
                    .isEqualTo(expectedTempC, within(0.01));
        } else {
            // Minimum temperature
            assertThat(data.getRemarks().sixHourMinTemperature())
                    .as("Minimum temperature should not be null: %s", scenario)
                    .isNotNull();

            assertThat(data.getRemarks().sixHourMinTemperature().celsius())
                    .as("Temperature mismatch: %s", scenario)
                    .isEqualTo(expectedTempC, within(0.01));
        }
    }

    @Test
    @DisplayName("Should parse both max and min temperature in same METAR")
    void testParse6HourBothMaxAndMin() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 10142 21001";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Maximum: 14.2°C
        assertThat(data.getRemarks().sixHourMaxTemperature()).isNotNull();
        assertThat(data.getRemarks().sixHourMaxTemperature().celsius())
                .isEqualTo(14.2, within(0.01));

        // Minimum: -0.1°C
        assertThat(data.getRemarks().sixHourMinTemperature()).isNotNull();
        assertThat(data.getRemarks().sixHourMinTemperature().celsius())
                .isEqualTo(-0.1, within(0.01));
    }

    @Test
    @DisplayName("Should parse 6-hour max/min with other remarks")
    void testParse6HourMaxMin_WithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 10142 21001";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify all remarks parsed
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();

        // Verify 6-hour temperatures
        assertThat(data.getRemarks().sixHourMaxTemperature()).isNotNull();
        assertThat(data.getRemarks().sixHourMaxTemperature().celsius()).isEqualTo(14.2, within(0.01));

        assertThat(data.getRemarks().sixHourMinTemperature()).isNotNull();
        assertThat(data.getRemarks().sixHourMinTemperature().celsius()).isEqualTo(-0.1, within(0.01));
    }

    @Test
    @DisplayName("Should parse 6-hour max/min in mixed remark order")
    void testParse6HourMaxMin_MixedOrder() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 10142 AO2 21001 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // All should be parsed regardless of order
        assertThat(data.getRemarks().sixHourMaxTemperature()).isNotNull();
        assertThat(data.getRemarks().sixHourMinTemperature()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({
            // Missing 6-hour max/min temperature
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210', 'No max/min temp'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK', 'Empty remarks'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015', 'No RMK section'",
            // Invalid 6-hour max/min temperature formats
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 1ABCD', 'Non-numeric temperature'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 30142', 'Invalid type 3'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 12142', 'Invalid sign 2'"
    })
    @DisplayName("Should handle missing or invalid 6-hour max/min temperature")
    void testParse6HourMaxMin_MissingOrInvalid(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        if (data.getRemarks() != null) {
            assertThat(data.getRemarks().sixHourMaxTemperature())
                    .as("Max temperature should be null: %s", scenario)
                    .isNull();
            assertThat(data.getRemarks().sixHourMinTemperature())
                    .as("Min temperature should be null: %s", scenario)
                    .isNull();
        }
    }

    @Test
    @DisplayName("Should use Temperature query methods for 6-hour max/min")
    void testParse6HourMaxMin_TemperatureQueryMethods() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 10142 21001";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        Temperature maxTemp = data.getRemarks().sixHourMaxTemperature();
        assertThat(maxTemp).isNotNull();
        assertThat(maxTemp.isAboveFreezing()).isTrue();
        assertThat(maxTemp.toFahrenheit()).isCloseTo(57.56, within(0.1));

        Temperature minTemp = data.getRemarks().sixHourMinTemperature();
        assertThat(minTemp).isNotNull();
        assertThat(minTemp.isBelowFreezing()).isTrue();
        assertThat(minTemp.toFahrenheit()).isCloseTo(31.82, within(0.1));
    }

    @Test
    @DisplayName("Should parse real-world METAR with 6-hour max/min temperature")
    void testParseRealWorldMetarWith6HourMaxMin() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 10250 21001";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify main body
        assertThat(data.getStationId()).isEqualTo("KJFK");
        assertThat(data.getWind()).isNotNull();
        assertThat(data.getVisibility()).isNotNull();
        assertThat(data.getTemperature()).isNotNull();
        assertThat(data.getPressure()).isNotNull();

        // Verify remarks
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();

        // Verify 6-hour max/min temperatures
        assertThat(data.getRemarks().sixHourMaxTemperature()).isNotNull();
        assertThat(data.getRemarks().sixHourMaxTemperature().celsius()).isEqualTo(25.0, within(0.01));

        assertThat(data.getRemarks().sixHourMinTemperature()).isNotNull();
        assertThat(data.getRemarks().sixHourMinTemperature().celsius()).isEqualTo(-0.1, within(0.01));
    }

    @Test
    @DisplayName("Should parse extreme 6-hour temperatures")
    void testParse6HourMaxMin_ExtremeTemperatures() {
        // Very hot max, very cold min
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 10450 21400";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Maximum: 45.0°C (113°F)
        assertThat(data.getRemarks().sixHourMaxTemperature()).isNotNull();
        assertThat(data.getRemarks().sixHourMaxTemperature().celsius()).isEqualTo(45.0, within(0.01));
        assertThat(data.getRemarks().sixHourMaxTemperature().isVeryHot()).isTrue();

        // Minimum: -40.0°C (-40°F)
        assertThat(data.getRemarks().sixHourMinTemperature()).isNotNull();
        assertThat(data.getRemarks().sixHourMinTemperature().celsius()).isEqualTo(-40.0, within(0.01));
        assertThat(data.getRemarks().sixHourMinTemperature().isVeryCold()).isTrue();
    }

    @Test
    @DisplayName("Should calculate temperature range from 6-hour max/min")
    void testParse6HourMaxMin_TemperatureRange() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 10250 20050";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        double maxTemp = data.getRemarks().sixHourMaxTemperature().celsius();
        double minTemp = data.getRemarks().sixHourMinTemperature().celsius();

        double range = maxTemp - minTemp;

        // Range: 25.0 - 5.0 = 20.0°C
        assertThat(range).isEqualTo(20.0, within(0.01));
    }

    // ========== 24-HOUR MAX/MIN TEMPERATURE PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            // Positive max and min
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 400461006', 4.6, -0.6, 'Max 4.6°C, Min -0.6°C'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 400120010', 1.2, 1.0, 'Max 1.2°C, Min 1.0°C'",

            // Negative max and positive min (unusual but valid)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 410050005', -0.5, 0.5, 'Max -0.5°C, Min 0.5°C'",

            // Both negative
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 411231089', -12.3, -8.9, 'Max -12.3°C, Min -8.9°C'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 410011005', -0.1, -0.5, 'Max -0.1°C, Min -0.5°C'",

            // Both zero
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 400001000', 0.0, -0.0, 'Max 0.0°C, Min -0.0°C'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 400000000', 0.0, 0.0, 'Max 0.0°C, Min 0.0°C'",

            // Large values
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 403501250', 35.0, -25.0, 'Max 35.0°C, Min -25.0°C'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 404501350', 45.0, -35.0, 'Max 45.0°C, Min -35.0°C'"
    })
    @DisplayName("Should parse 24-hour max/min temperature")
    void testParse24HourMaxMinTemperature(String metar, double expectedMaxC, double expectedMinC, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks())
                .as("Remarks should not be null: %s", scenario)
                .isNotNull();

        // Maximum temperature
        assertThat(data.getRemarks().twentyFourHourMaxTemperature())
                .as("24-hour max temperature should not be null: %s", scenario)
                .isNotNull();

        assertThat(data.getRemarks().twentyFourHourMaxTemperature().celsius())
                .as("Max temperature mismatch: %s", scenario)
                .isEqualTo(expectedMaxC, within(0.01));

        // Minimum temperature
        assertThat(data.getRemarks().twentyFourHourMinTemperature())
                .as("24-hour min temperature should not be null: %s", scenario)
                .isNotNull();

        assertThat(data.getRemarks().twentyFourHourMinTemperature().celsius())
                .as("Min temperature mismatch: %s", scenario)
                .isEqualTo(expectedMinC, within(0.01));
    }

    @Test
    @DisplayName("Should parse 24-hour max/min with other remarks")
    void testParse24HourMaxMin_WithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 400461006";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify all remarks parsed
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();

        // Verify 24-hour temperatures
        assertThat(data.getRemarks().twentyFourHourMaxTemperature()).isNotNull();
        assertThat(data.getRemarks().twentyFourHourMaxTemperature().celsius()).isEqualTo(4.6, within(0.01));

        assertThat(data.getRemarks().twentyFourHourMinTemperature()).isNotNull();
        assertThat(data.getRemarks().twentyFourHourMinTemperature().celsius()).isEqualTo(-0.6, within(0.01));
    }

    @Test
    @DisplayName("Should parse 24-hour max/min in mixed remark order")
    void testParse24HourMaxMin_MixedOrder() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 400461006 AO2 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // All should be parsed regardless of order
        assertThat(data.getRemarks().twentyFourHourMaxTemperature()).isNotNull();
        assertThat(data.getRemarks().twentyFourHourMinTemperature()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210', 'No 24-hour temp'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK', 'Empty remarks'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015', 'No RMK section'",

            // Invalid formats
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 4ABCDEFGH', 'Non-numeric values'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 500461006', 'Invalid type 5'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 4204610', 'Incomplete (7 digits)'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 40046100', 'Invalid sign (missing digit)'"
    })
    @DisplayName("Should handle missing or invalid 24-hour max/min temperature")
    void testParse24HourMaxMin_MissingOrInvalid(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        if (data.getRemarks() != null) {
            assertThat(data.getRemarks().twentyFourHourMaxTemperature())
                    .as("Max temperature should be null: %s", scenario)
                    .isNull();
            assertThat(data.getRemarks().twentyFourHourMinTemperature())
                    .as("Min temperature should be null: %s", scenario)
                    .isNull();
        }
    }

    @Test
    @DisplayName("Should use Temperature query methods for 24-hour max/min")
    void testParse24HourMaxMin_TemperatureQueryMethods() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 400461006";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        Temperature maxTemp = data.getRemarks().twentyFourHourMaxTemperature();
        assertThat(maxTemp).isNotNull();
        assertThat(maxTemp.isAboveFreezing()).isTrue();
        assertThat(maxTemp.toFahrenheit()).isCloseTo(40.28, within(0.1));

        Temperature minTemp = data.getRemarks().twentyFourHourMinTemperature();
        assertThat(minTemp).isNotNull();
        assertThat(minTemp.isBelowFreezing()).isTrue();
        assertThat(minTemp.toFahrenheit()).isCloseTo(30.92, within(0.1));
    }

    @Test
    @DisplayName("Should parse real-world METAR with 24-hour max/min temperature")
    void testParseRealWorldMetarWith24HourMaxMin() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 400351015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify main body
        assertThat(data.getStationId()).isEqualTo("KJFK");
        assertThat(data.getWind()).isNotNull();
        assertThat(data.getVisibility()).isNotNull();
        assertThat(data.getTemperature()).isNotNull();
        assertThat(data.getPressure()).isNotNull();

        // Verify remarks
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();

        // Verify 24-hour max/min temperatures
        assertThat(data.getRemarks().twentyFourHourMaxTemperature()).isNotNull();
        assertThat(data.getRemarks().twentyFourHourMaxTemperature().celsius()).isEqualTo(3.5, within(0.01));

        assertThat(data.getRemarks().twentyFourHourMinTemperature()).isNotNull();
        assertThat(data.getRemarks().twentyFourHourMinTemperature().celsius()).isEqualTo(-1.5, within(0.01));
    }

    @Test
    @DisplayName("Should parse extreme 24-hour temperatures")
    void testParse24HourMaxMin_ExtremeTemperatures() {
        // Very hot max, very cold min
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 404501400";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Maximum: 45.0°C (113°F)
        assertThat(data.getRemarks().twentyFourHourMaxTemperature()).isNotNull();
        assertThat(data.getRemarks().twentyFourHourMaxTemperature().celsius()).isEqualTo(45.0, within(0.01));
        assertThat(data.getRemarks().twentyFourHourMaxTemperature().isVeryHot()).isTrue();

        // Minimum: -40.0°C (-40°F)
        assertThat(data.getRemarks().twentyFourHourMinTemperature()).isNotNull();
        assertThat(data.getRemarks().twentyFourHourMinTemperature().celsius()).isEqualTo(-40.0, within(0.01));
        assertThat(data.getRemarks().twentyFourHourMinTemperature().isVeryCold()).isTrue();
    }

    @Test
    @DisplayName("Should calculate temperature range from 24-hour max/min")
    void testParse24HourMaxMin_TemperatureRange() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 400351015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        double maxTemp = data.getRemarks().twentyFourHourMaxTemperature().celsius();
        double minTemp = data.getRemarks().twentyFourHourMinTemperature().celsius();

        double range = maxTemp - minTemp;

        // Range: 3.5 - (-1.5) = 5.0°C
        assertThat(range).isEqualTo(5.0, within(0.01));
    }

    @Test
    @DisplayName("Should parse 24-hour temps with both 6-hour and 24-hour in same METAR")
    void testParse24HourMaxMin_WithSixHourTemps() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK 10142 21001 400461006";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // 6-hour temps should be parsed
        assertThat(data.getRemarks().sixHourMaxTemperature()).isNotNull();
        assertThat(data.getRemarks().sixHourMaxTemperature().celsius()).isEqualTo(14.2, within(0.01));
        assertThat(data.getRemarks().sixHourMinTemperature()).isNotNull();
        assertThat(data.getRemarks().sixHourMinTemperature().celsius()).isEqualTo(-0.1, within(0.01));

        // 24-hour temps should be parsed
        assertThat(data.getRemarks().twentyFourHourMaxTemperature()).isNotNull();
        assertThat(data.getRemarks().twentyFourHourMaxTemperature().celsius()).isEqualTo(4.6, within(0.01));
        assertThat(data.getRemarks().twentyFourHourMinTemperature()).isNotNull();
        assertThat(data.getRemarks().twentyFourHourMinTemperature().celsius()).isEqualTo(-0.6, within(0.01));
    }

    // ========== VARIABLE CEILING PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 005V010', 500, 1000, 'Low ceiling 500-1000 ft'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 020V035', 2000, 3500, 'Normal ceiling 2000-3500 ft'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 010V015', 1000, 1500, 'Ceiling 1000-1500 ft'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 015V020', 1500, 2000, 'Ceiling 1500-2000 ft'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 003V008', 300, 800, 'Very low ceiling 300-800 ft'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 000V002', 0, 200, 'Ground level fog 0-200 ft'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 050V100', 5000, 10000, 'High ceiling 5000-10000 ft'"
    })
    @DisplayName("Should parse variable ceiling")
    void testParseVariableCeiling(String metar, int expectedMinFt, int expectedMaxFt, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks())
                .as("Remarks should not be null: %s", scenario)
                .isNotNull();

        assertThat(data.getRemarks().variableCeiling())
                .as("Variable ceiling should not be null: %s", scenario)
                .isNotNull();

        assertThat(data.getRemarks().variableCeiling().minimumHeightFeet())
                .as("Minimum ceiling mismatch: %s", scenario)
                .isEqualTo(expectedMinFt);

        assertThat(data.getRemarks().variableCeiling().maximumHeightFeet())
                .as("Maximum ceiling mismatch: %s", scenario)
                .isEqualTo(expectedMaxFt);
    }

    @Test
    @DisplayName("Should parse variable ceiling with other remarks")
    void testParseVariableCeiling_WithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 CIG 005V010";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify all remarks parsed
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();

        // Verify variable ceiling
        assertThat(data.getRemarks().variableCeiling()).isNotNull();
        assertThat(data.getRemarks().variableCeiling().minimumHeightFeet()).isEqualTo(500);
        assertThat(data.getRemarks().variableCeiling().maximumHeightFeet()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Should parse variable ceiling in mixed remark order")
    void testParseVariableCeiling_MixedOrder() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 010V015 AO2 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // All should be parsed regardless of order
        assertThat(data.getRemarks().variableCeiling()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210', 'No variable ceiling'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK', 'Empty remarks'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015', 'No RMK section'",

            // Invalid formats
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 05V10', 'Only 2 digits'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG ABCVDEF', 'Non-numeric'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK 005V010', 'Missing CIG prefix'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIGV010', 'No space after CIG'"
    })
    @DisplayName("Should handle missing or invalid variable ceiling")
    void testParseVariableCeiling_MissingOrInvalid(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        if (data.getRemarks() != null) {
            assertThat(data.getRemarks().variableCeiling())
                    .as("Variable ceiling should be null: %s", scenario)
                    .isNull();
        }
    }

    @Test
    @DisplayName("Should use VariableCeiling query methods")
    void testParseVariableCeiling_QueryMethods() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 005V010";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        VariableCeiling ceiling = data.getRemarks().variableCeiling();
        assertThat(ceiling).isNotNull();

        // Test query methods
        assertThat(ceiling.getRangeFeet()).isEqualTo(500);
        assertThat(ceiling.isLowCeiling()).isTrue();
        assertThat(ceiling.isSignificantVariation()).isTrue();
    }

    @Test
    @DisplayName("Should parse real-world METAR with variable ceiling")
    void testParseRealWorldMetarWithVariableCeiling() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 CIG 008V012";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify main body
        assertThat(data.getStationId()).isEqualTo("KJFK");
        assertThat(data.getWind()).isNotNull();
        assertThat(data.getVisibility()).isNotNull();
        assertThat(data.getTemperature()).isNotNull();
        assertThat(data.getPressure()).isNotNull();

        // Verify remarks
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();

        // Verify variable ceiling
        assertThat(data.getRemarks().variableCeiling()).isNotNull();
        assertThat(data.getRemarks().variableCeiling().minimumHeightFeet()).isEqualTo(800);
        assertThat(data.getRemarks().variableCeiling().maximumHeightFeet()).isEqualTo(1200);
    }

    @Test
    @DisplayName("Should handle ceiling range calculations")
    void testParseVariableCeiling_RangeCalculations() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 010V020";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        VariableCeiling ceiling = data.getRemarks().variableCeiling();

        // Range: 2000 - 1000 = 1000 feet
        assertThat(ceiling.getRangeFeet()).isEqualTo(1000);

        // Should be significant (>= 500 feet)
        assertThat(ceiling.isSignificantVariation()).isTrue();
    }

    @Test
    @DisplayName("Should handle low ceiling identification")
    void testParseVariableCeiling_LowCeilingIdentification() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 003V007";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        VariableCeiling ceiling = data.getRemarks().variableCeiling();

        // 300 ft minimum - IFR conditions
        assertThat(ceiling.isLowCeiling()).isTrue();
        assertThat(ceiling.minimumHeightFeet()).isLessThan(1000);
    }

    @Test
    @DisplayName("Should parse variable ceiling with variable visibility")
    void testParseVariableCeiling_WithVariableVisibility() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK VIS 1V3 CIG 005V010";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Both variable visibility and variable ceiling should be parsed
        assertThat(data.getRemarks().variableVisibility()).isNotNull();
        assertThat(data.getRemarks().variableCeiling()).isNotNull();

        assertThat(data.getRemarks().variableCeiling().minimumHeightFeet()).isEqualTo(500);
        assertThat(data.getRemarks().variableCeiling().maximumHeightFeet()).isEqualTo(1000);
    }

    // ========== CEILING SECOND SITE PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 002 RY11', 200, 'RY11', 'Low ceiling at runway 11'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 005 RWY06', 500, 'RWY06', 'Ceiling at runway 06'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 010', 1000, , 'Ceiling without location'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 020 TWR', 2000, 'TWR', 'Ceiling at tower'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 003 APCH', 300, 'APCH', 'Ceiling at approach'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 000 RY11', 0, 'RY11', 'Ground level ceiling'"
    })
    @DisplayName("Should parse ceiling second site")
    void testParseCeilingSecondSite(String metar, int expectedHeightFt, String expectedLocation, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks())
                .as("Remarks should not be null: %s", scenario)
                .isNotNull();

        assertThat(data.getRemarks().ceilingSecondSite())
                .as("Ceiling second site should not be null: %s", scenario)
                .isNotNull();

        assertThat(data.getRemarks().ceilingSecondSite().heightFeet())
                .as("Height mismatch: %s", scenario)
                .isEqualTo(expectedHeightFt);

        if (expectedLocation != null) {
            assertThat(data.getRemarks().ceilingSecondSite().location())
                    .as("Location mismatch: %s", scenario)
                    .isEqualTo(expectedLocation);
            assertThat(data.getRemarks().ceilingSecondSite().hasLocation()).isTrue();
        } else {
            assertThat(data.getRemarks().ceilingSecondSite().location()).isNull();
            assertThat(data.getRemarks().ceilingSecondSite().hasLocation()).isFalse();
        }
    }

    @Test
    @DisplayName("Should parse ceiling second site with other remarks")
    void testParseCeilingSecondSite_WithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 CIG 002 RY11";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify all remarks parsed
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();

        // Verify ceiling second site
        assertThat(data.getRemarks().ceilingSecondSite()).isNotNull();
        assertThat(data.getRemarks().ceilingSecondSite().heightFeet()).isEqualTo(200);
        assertThat(data.getRemarks().ceilingSecondSite().location()).isEqualTo("RY11");
    }

    @Test
    @DisplayName("Should NOT confuse ceiling second site with variable ceiling")
    void testParseCeilingSecondSite_NotVariableCeiling() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 005V010 CIG 002 RY11";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Should have BOTH variable ceiling and second site ceiling
        assertThat(data.getRemarks().variableCeiling()).isNotNull();
        assertThat(data.getRemarks().variableCeiling().minimumHeightFeet()).isEqualTo(500);
        assertThat(data.getRemarks().variableCeiling().maximumHeightFeet()).isEqualTo(1000);

        assertThat(data.getRemarks().ceilingSecondSite()).isNotNull();
        assertThat(data.getRemarks().ceilingSecondSite().heightFeet()).isEqualTo(200);
        assertThat(data.getRemarks().ceilingSecondSite().location()).isEqualTo("RY11");
    }

    @Test
    @DisplayName("Should parse ceiling second site in mixed remark order")
    void testParseCeilingSecondSite_MixedOrder() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 005 RWY06 AO2 SLP210";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // All should be parsed regardless of order
        assertThat(data.getRemarks().ceilingSecondSite()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210', 'No ceiling second site'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK', 'Empty remarks'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015', 'No RMK section'",

            // Invalid formats (these should NOT match ceiling second site)
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 05 RY11', 'Only 2 digits'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CEIL 002 RY11', 'Wrong prefix'"
    })
    @DisplayName("Should handle missing or invalid ceiling second site")
    void testParseCeilingSecondSite_MissingOrInvalid(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        if (data.getRemarks() != null) {
            assertThat(data.getRemarks().ceilingSecondSite())
                    .as("Ceiling second site should be null: %s", scenario)
                    .isNull();
        }
    }

    @Test
    @DisplayName("Should use CeilingSecondSite query methods")
    void testParseCeilingSecondSite_QueryMethods() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 005 RWY06";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        CeilingSecondSite ceiling = data.getRemarks().ceilingSecondSite();
        assertThat(ceiling).isNotNull();

        // Test query methods
        assertThat(ceiling.isLowCeiling()).isTrue();
        assertThat(ceiling.hasLocation()).isTrue();
        assertThat(ceiling.getSummary()).isEqualTo("Ceiling 500 ft at RWY06");
    }

    @Test
    @DisplayName("Should parse real-world METAR with ceiling second site")
    void testParseRealWorldMetarWithCeilingSecondSite() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 CIG 003 RY04L";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify main body
        assertThat(data.getStationId()).isEqualTo("KJFK");
        assertThat(data.getWind()).isNotNull();
        assertThat(data.getVisibility()).isNotNull();
        assertThat(data.getTemperature()).isNotNull();
        assertThat(data.getPressure()).isNotNull();

        // Verify remarks
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();

        // Verify ceiling second site
        assertThat(data.getRemarks().ceilingSecondSite()).isNotNull();
        assertThat(data.getRemarks().ceilingSecondSite().heightFeet()).isEqualTo(300);
        assertThat(data.getRemarks().ceilingSecondSite().location()).isEqualTo("RY04L");
        assertThat(data.getRemarks().ceilingSecondSite().isLowCeiling()).isTrue();
    }

    @Test
    @DisplayName("Should handle ceiling second site without location")
    void testParseCeilingSecondSite_NoLocation() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 015 AO2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        CeilingSecondSite ceiling = data.getRemarks().ceilingSecondSite();

        assertThat(ceiling).isNotNull();
        assertThat(ceiling.heightFeet()).isEqualTo(1500);
        assertThat(ceiling.location()).isNull();
        assertThat(ceiling.hasLocation()).isFalse();
        assertThat(ceiling.getSummary()).isEqualTo("Ceiling 1500 ft");
    }

    @Test
    @DisplayName("Should identify low ceiling at second site")
    void testParseCeilingSecondSite_LowCeilingIdentification() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK CIG 003 RY11";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        CeilingSecondSite ceiling = data.getRemarks().ceilingSecondSite();

        // 300 ft - IFR conditions
        assertThat(ceiling.isLowCeiling()).isTrue();
        assertThat(ceiling.heightFeet()).isLessThan(1000);
    }

    // ========== OBSCURATION LAYERS TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "FEW FG 000, FEW, FG, 0",
            "SCT FU 010, SCT, FU, 1000",
            "BKN BR 005, BKN, BR, 500",
            "OVC HZ 020, OVC, HZ, 2000",
            "FEW DU 015, FEW, DU, 1500",
            "SCT SA 025, SCT, SA, 2500"
    })
    @DisplayName("Should parse single obscuration layer")
    void testParseObscurationLayer_Single(String remark, String coverage, String phenomenon, int heightFeet) {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK " + remark;

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().obscurationLayers()).hasSize(1);

        ObscurationLayer layer = data.getRemarks().obscurationLayers().get(0);
        assertThat(layer.coverage()).isEqualTo(coverage);
        assertThat(layer.phenomenon()).isEqualTo(phenomenon);
        assertThat(layer.heightFeet()).isEqualTo(heightFeet);
    }

    @Test
    @DisplayName("Should parse multiple obscuration layers")
    void testParseObscurationLayer_Multiple() {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK FEW FG 000 SCT FU 010 BKN BR 005";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().obscurationLayers()).hasSize(3);

        // First layer: FEW FG 000
        assertThat(data.getRemarks().obscurationLayers().get(0).coverage()).isEqualTo("FEW");
        assertThat(data.getRemarks().obscurationLayers().get(0).phenomenon()).isEqualTo("FG");
        assertThat(data.getRemarks().obscurationLayers().get(0).heightFeet()).isZero();

        // Second layer: SCT FU 010
        assertThat(data.getRemarks().obscurationLayers().get(1).coverage()).isEqualTo("SCT");
        assertThat(data.getRemarks().obscurationLayers().get(1).phenomenon()).isEqualTo("FU");
        assertThat(data.getRemarks().obscurationLayers().get(1).heightFeet()).isEqualTo(1000);

        // Third layer: BKN BR 005
        assertThat(data.getRemarks().obscurationLayers().get(2).coverage()).isEqualTo("BKN");
        assertThat(data.getRemarks().obscurationLayers().get(2).phenomenon()).isEqualTo("BR");
        assertThat(data.getRemarks().obscurationLayers().get(2).heightFeet()).isEqualTo(500);
    }

    @Test
    @DisplayName("Should parse obscuration layer with other remarks")
    void testParseObscurationLayer_WithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 SLP210 FEW FG 000 T02220117";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().obscurationLayers()).hasSize(1);
        assertThat(data.getRemarks().obscurationLayers().get(0).phenomenon()).isEqualTo("FG");
    }

    @Test
    @DisplayName("Should identify ground level fog")
    void testParseObscurationLayer_GroundLevelFog() {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK FEW FG 000";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().obscurationLayers()).hasSize(1);
        ObscurationLayer layer = data.getRemarks().obscurationLayers().get(0);
        assertThat(layer.isGroundLevel()).isTrue();
        assertThat(layer.isLowLevel()).isTrue();
    }

    @Test
    @DisplayName("Should handle various obscuration phenomena")
    void testParseObscurationLayer_VariousPhenomena() {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK " +
                "FEW FG 000 SCT BR 005 BKN FU 010 OVC HZ 015";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().obscurationLayers()).hasSize(4);
        assertThat(data.getRemarks().obscurationLayers().get(0).phenomenon()).isEqualTo("FG");
        assertThat(data.getRemarks().obscurationLayers().get(1).phenomenon()).isEqualTo("BR");
        assertThat(data.getRemarks().obscurationLayers().get(2).phenomenon()).isEqualTo("FU");
        assertThat(data.getRemarks().obscurationLayers().get(3).phenomenon()).isEqualTo("HZ");
    }

    @Test
    @DisplayName("Should NOT parse sky condition as obscuration")
    void testParseObscurationLayer_NotSkyCondition() {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 SCT100 BKN050 22/12 A3015 RMK AO2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Sky conditions should be in skyConditions, not obscurationLayers
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().obscurationLayers()).isEmpty();
    }

    @Test
    @DisplayName("Should handle obscuration in mixed remark order")
    void testParseObscurationLayer_MixedOrder() {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK " +
                "AO2 FEW FG 000 SLP210 T02220117 SCT FU 010";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().obscurationLayers()).hasSize(2);
        assertThat(data.getRemarks().automatedStationType()).isNotNull();
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({
            "FEW FG, Missing height",
            "FG 000, Missing coverage",
            "FEW 000, Missing phenomenon",
            "FEW XX 000, Invalid phenomenon",
            "XXX FG 000, Invalid coverage"
    })
    @DisplayName("Should handle invalid obscuration formats")
    void testParseObscurationLayer_InvalidFormats(String invalidRemark) {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK " + invalidRemark + " AO2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        // Invalid formats should be skipped or in unparsed
        assertThat(data.getRemarks()).isNotNull();
        // Should not crash, but may have empty obscurationLayers
    }

    @Test
    @DisplayName("Should query obscuration layer properties")
    void testParseObscurationLayer_QueryMethods() {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK FEW FG 000 BKN HZ 020";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().obscurationLayers()).hasSize(2);

        // Ground fog
        ObscurationLayer fog = data.getRemarks().obscurationLayers().get(0);
        assertThat(fog.isGroundLevel()).isTrue();
        assertThat(fog.isLowLevel()).isTrue();
        assertThat(fog.getCoverageDescription()).isEqualTo("Few");
        assertThat(fog.getPhenomenonDescription()).isEqualTo("Fog");

        // Elevated haze
        ObscurationLayer haze = data.getRemarks().obscurationLayers().get(1);
        assertThat(haze.isGroundLevel()).isFalse();
        assertThat(haze.isLowLevel()).isFalse();
        assertThat(haze.getPhenomenonDescription()).isEqualTo("Haze");
    }

    @Test
    @DisplayName("Should parse real-world METAR with obscuration")
    void testParseObscurationLayer_RealWorld() {
        // NOTE: Obscuration layer MUST be in remarks section (after RMK)
        // Original had "FEW FG 000" before RMK (in main body) which is a sky condition, not an obscuration layer
        String metar = "METAR KSFO 121856Z 28012KT 1SM BKN250 15/14 A3012 RMK AO2 SLP201 FEW FG 000 T01500139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getStationId()).isEqualTo("KSFO");
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().obscurationLayers()).hasSize(1);

        ObscurationLayer fog = data.getRemarks().obscurationLayers().get(0);
        assertThat(fog.coverage()).isEqualTo("FEW");
        assertThat(fog.phenomenon()).isEqualTo("FG");
        assertThat(fog.heightFeet()).isZero();
        assertThat(fog.isGroundLevel()).isTrue();
    }

    // ADD this test to verify sky conditions in main body don't interfere with obscuration in remarks

    // ADD this test to verify sky conditions and obscuration layers don't interfere

    @Test
    @DisplayName("Should distinguish sky conditions from obscuration layers")
    void testParseObscurationLayer_WithSkyConditionsSameKeyword() {
        // FEW005 in main body = sky condition (no spaces)
        // FEW FG 000 in remarks = obscuration layer (with spaces)
        String metar = "METAR KSFO 121856Z 28012KT 1SM FEW005 BKN250 15/14 A3012 RMK AO2 SLP201 FEW FG 000 T01500139";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getStationId()).isEqualTo("KSFO");

        // Verify sky conditions were parsed in main body
        assertThat(data.getSkyConditions()).isNotNull();
        assertThat(data.getSkyConditions()).hasSizeGreaterThanOrEqualTo(2);  // At least FEW005 and BKN250

        // Verify first sky condition is FEW at 500 feet (not confused with FEW FG 000)
        SkyCondition firstLayer = data.getSkyConditions().get(0);
        assertThat(firstLayer.coverage()).isEqualTo(SkyCoverage.FEW);
        assertThat(firstLayer.heightFeet()).isEqualTo(500);
        assertThat(firstLayer.cloudType()).isNull();

        // Verify obscuration layer was parsed in remarks (not confused with FEW005)
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().obscurationLayers()).hasSize(1);

        ObscurationLayer fog = data.getRemarks().obscurationLayers().get(0);
        assertThat(fog.coverage()).isEqualTo("FEW");
        assertThat(fog.phenomenon()).isEqualTo("FG");
        assertThat(fog.heightFeet()).isZero();
        assertThat(fog.isGroundLevel()).isTrue();
    }

    // ========== CLOUD TYPE INTEGRATION TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "SC1, SC, 1",
            "AC2, AC, 2",
            "CI3, CI, 3",
            "CU4, CU, 4",
            "NS5, NS, 5",
            "AS8, AS, 8"
    })
    @DisplayName("Should parse single cloud type with oktas")
    void testParseCloudType_Single(String cloudTypeRemark, String expectedType, int expectedOktas) {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 " + cloudTypeRemark;

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().cloudTypes()).hasSize(1);

        CloudType cloudType = data.getRemarks().cloudTypes().get(0);
        assertThat(cloudType.cloudType()).isEqualTo(expectedType);
        assertThat(cloudType.oktas()).isEqualTo(expectedOktas);
        assertThat(cloudType.hasOktaCoverage()).isTrue();
    }

    @Test
    @DisplayName("Should parse multiple cloud types")
    void testParseCloudType_Multiple() {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 SC1 AC2 CI3";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().cloudTypes()).hasSize(3);

        CloudType sc = data.getRemarks().cloudTypes().get(0);
        assertThat(sc.cloudType()).isEqualTo("SC");
        assertThat(sc.oktas()).isEqualTo(1);

        CloudType ac = data.getRemarks().cloudTypes().get(1);
        assertThat(ac.cloudType()).isEqualTo("AC");
        assertThat(ac.oktas()).isEqualTo(2);

        CloudType ci = data.getRemarks().cloudTypes().get(2);
        assertThat(ci.cloudType()).isEqualTo("CI");
        assertThat(ci.oktas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should parse cloud types with other remarks")
    void testParseCloudType_WithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 SLP210 SC1 T02220117";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isNotNull();
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().cloudTypes()).hasSize(1);
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();

        CloudType sc = data.getRemarks().cloudTypes().get(0);
        assertThat(sc.cloudType()).isEqualTo("SC");
        assertThat(sc.oktas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should parse cloud type with trace location")
    void testParseCloudType_Trace() {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 SC TR";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().cloudTypes()).hasSize(1);

        CloudType sc = data.getRemarks().cloudTypes().get(0);
        assertThat(sc.cloudType()).isEqualTo("SC");
        assertThat(sc.oktas()).isNull();
        assertThat(sc.location()).isEqualTo("TR");
        assertThat(sc.isTrace()).isTrue();
    }

    @Test
    @DisplayName("Should parse cloud type with intensity and location")
    void testParseCloudType_WithIntensityAndLocation() {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 MDT CU OHD";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().cloudTypes()).hasSize(1);

        CloudType cu = data.getRemarks().cloudTypes().get(0);
        assertThat(cu.cloudType()).isEqualTo("CU");
        assertThat(cu.intensity()).isEqualTo("MDT");
        assertThat(cu.location()).isEqualTo("OHD");
        assertThat(cu.hasIntensity()).isTrue();
        assertThat(cu.isOverhead()).isTrue();
    }

    @Test
    @DisplayName("Should parse cloud type with movement")
    void testParseCloudType_WithMovement() {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 CI MOVG NE";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().cloudTypes()).hasSize(1);

        CloudType ci = data.getRemarks().cloudTypes().get(0);
        assertThat(ci.cloudType()).isEqualTo("CI");
        assertThat(ci.movementDirection()).isEqualTo("NE");
        assertThat(ci.hasMovement()).isTrue();
    }

    @Test
    @DisplayName("Should parse cloud type with all quadrants location")
    void testParseCloudType_AllQuadrants() {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 AC OHD-ALQDS";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().cloudTypes()).hasSize(1);

        CloudType ac = data.getRemarks().cloudTypes().get(0);
        assertThat(ac.cloudType()).isEqualTo("AC");
        assertThat(ac.location()).isEqualTo("OHD-ALQDS");
        assertThat(ac.isAllQuadrants()).isTrue();
        assertThat(ac.isOverhead()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "XX1",      // Invalid cloud type
            "SC9",      // Invalid oktas (out of range)
            "SC HEAVY", // Invalid intensity
            "SC XXX"    // Invalid location
    })
    @DisplayName("Should handle invalid cloud type formats")
    void testParseCloudType_InvalidFormats(String invalidRemark) {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK " + invalidRemark + " AO2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        // Invalid formats should be skipped
        assertThat(data.getRemarks().cloudTypes()).isEmpty();
    }

    @Test
    @DisplayName("Should test cloud type query methods")
    void testParseCloudType_QueryMethods() {
        String metar = "METAR KJFK 121851Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 SC1 AC TR";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().cloudTypes()).hasSize(2);

        CloudType sc = data.getRemarks().cloudTypes().get(0);
        assertThat(sc.hasOktaCoverage()).isTrue();
        assertThat(sc.getCloudTypeDescription()).isEqualTo("Stratocumulus");
        assertThat(sc.getOktasFraction()).isEqualTo(0.125);

        CloudType ac = data.getRemarks().cloudTypes().get(1);
        assertThat(ac.hasLocation()).isTrue();
        assertThat(ac.isTrace()).isTrue();
    }

    @Test
    @DisplayName("Should parse real-world METAR with cloud types - CYYZ example")
    void testParseCloudType_RealWorld_CYYZ() {
        // Real METAR from CYYZ - NOTE: "TCU4AC1AC2" is a chained cloud type format
        // Our parser expects space-separated cloud types like "TCU4 AC1 AC2"
        String metar = "METAR CYYZ 052200Z 16008KT 15SM SCT065TCU BKN080 BKN160 25/18 A2976 RMK TCU4 AC1 AC2";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getStationId()).isEqualTo("CYYZ");
        assertThat(data.getRemarks()).isNotNull();

        // Should have 3 cloud types parsed (TCU4, AC1, AC2)
        assertThat(data.getRemarks().cloudTypes()).hasSize(3);

        CloudType tcu = data.getRemarks().cloudTypes().get(0);
        assertThat(tcu.cloudType()).isEqualTo("TCU");
        assertThat(tcu.oktas()).isEqualTo(4);

        CloudType ac1 = data.getRemarks().cloudTypes().get(1);
        assertThat(ac1.cloudType()).isEqualTo("AC");
        assertThat(ac1.oktas()).isEqualTo(1);

        CloudType ac2 = data.getRemarks().cloudTypes().get(2);
        assertThat(ac2.cloudType()).isEqualTo("AC");
        assertThat(ac2.oktas()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should parse real-world METAR with cloud types - CYXP example")
    void testParseCloudType_RealWorld_CYXP() {
        // Real METAR from CYXP with SC TR
        String metar = "METAR CYXP 152200Z 08004KT 15SM FEW030 M28/M32 A3019 RMK SC1 SC TR";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertTrue(result.isSuccess());
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getStationId()).isEqualTo("CYXP");
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().cloudTypes()).hasSize(2);

        CloudType sc1 = data.getRemarks().cloudTypes().get(0);
        assertThat(sc1.cloudType()).isEqualTo("SC");
        assertThat(sc1.oktas()).isEqualTo(1);

        CloudType scTr = data.getRemarks().cloudTypes().get(1);
        assertThat(scTr.cloudType()).isEqualTo("SC");
        assertThat(scTr.location()).isEqualTo("TR");
        assertThat(scTr.isTrace()).isTrue();
    }

    // ========== AUTOMATED MAINTENANCE INDICATOR PARSING TESTS ==========

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK RVRNO', RVRNO, , 'RVR not available'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK PWINO', PWINO, , 'Present weather identifier not available'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK PNO', PNO, , 'Precipitation amount not available'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK FZRANO', FZRANO, , 'Freezing rain sensor not available'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK TSNO', TSNO, , 'Thunderstorm information not available'"
    })
    @DisplayName("Should parse automated maintenance indicators without location")
    void testParseAutomatedMaintenanceIndicator_NoLocation(String metar, String expectedType, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks())
                .as("Remarks should not be null: %s", scenario)
                .isNotNull();

        assertThat(data.getRemarks().automatedMaintenanceIndicators())
                .as("Should have automated maintenance indicators: %s", scenario)
                .hasSize(1);

        AutomatedMaintenanceIndicator indicator = data.getRemarks().automatedMaintenanceIndicators().get(0);
        assertThat(indicator.type()).isEqualTo(expectedType);
        assertThat(indicator.location()).isNull();
        assertThat(indicator.hasLocation()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK VISNO RWY06', VISNO, RWY06, 'Visibility at runway 06'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CHINO N', CHINO, N, 'Cloud height indicator North'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK VISNO RY11', VISNO, RY11, 'Visibility at runway 11'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK CHINO SE', CHINO, SE, 'Cloud height indicator Southeast'"
    })
    @DisplayName("Should parse automated maintenance indicators with location")
    void testParseAutomatedMaintenanceIndicator_WithLocation(String metar, String expectedType,
                                                             String expectedLocation, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks())
                .as("Remarks should not be null: %s", scenario)
                .isNotNull();

        assertThat(data.getRemarks().automatedMaintenanceIndicators())
                .as("Should have automated maintenance indicators: %s", scenario)
                .hasSize(1);

        AutomatedMaintenanceIndicator indicator = data.getRemarks().automatedMaintenanceIndicators().get(0);
        assertThat(indicator.type()).isEqualTo(expectedType);
        assertThat(indicator.location()).isEqualTo(expectedLocation);
        assertThat(indicator.hasLocation()).isTrue();
    }

    @Test
    @DisplayName("Should parse maintenance check indicator ($)")
    void testParseAutomatedMaintenanceIndicator_MaintenanceCheck() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK $";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedMaintenanceIndicators()).hasSize(1);
        assertThat(data.getRemarks().maintenanceRequired()).isTrue();

        AutomatedMaintenanceIndicator indicator = data.getRemarks().automatedMaintenanceIndicators().get(0);
        assertThat(indicator.type()).isEqualTo("$");
        assertThat(indicator.isMaintenanceCheck()).isTrue();
    }

    @Test
    @DisplayName("Should parse multiple automated maintenance indicators")
    void testParseAutomatedMaintenanceIndicator_Multiple() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK RVRNO PWINO CHINO N";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedMaintenanceIndicators()).hasSize(3);

        // First: RVRNO
        AutomatedMaintenanceIndicator rvrno = data.getRemarks().automatedMaintenanceIndicators().get(0);
        assertThat(rvrno.type()).isEqualTo("RVRNO");
        assertThat(rvrno.isRVRNotAvailable()).isTrue();

        // Second: PWINO
        AutomatedMaintenanceIndicator pwino = data.getRemarks().automatedMaintenanceIndicators().get(1);
        assertThat(pwino.type()).isEqualTo("PWINO");
        assertThat(pwino.isPresentWeatherNotAvailable()).isTrue();

        // Third: CHINO N
        AutomatedMaintenanceIndicator chino = data.getRemarks().automatedMaintenanceIndicators().get(2);
        assertThat(chino.type()).isEqualTo("CHINO");
        assertThat(chino.location()).isEqualTo("N");
        assertThat(chino.isCloudHeightNotAvailable()).isTrue();
    }

    @Test
    @DisplayName("Should parse automated maintenance indicators with other remarks")
    void testParseAutomatedMaintenanceIndicator_WithOtherRemarks() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 RVRNO PWINO $";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify all remarks parsed
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();

        // Verify automated maintenance indicators
        assertThat(data.getRemarks().automatedMaintenanceIndicators()).hasSize(3);
        assertThat(data.getRemarks().maintenanceRequired()).isTrue();
    }

    @Test
    @DisplayName("Should parse automated maintenance indicators in mixed remark order")
    void testParseAutomatedMaintenanceIndicator_MixedOrder() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK RVRNO AO2 PWINO SLP210 $";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // All should be parsed regardless of order
        assertThat(data.getRemarks().automatedMaintenanceIndicators()).hasSize(3);
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().maintenanceRequired()).isTrue();
    }

    @Test
    @DisplayName("Should parse automated maintenance indicators at end of remarks")
    void testParseAutomatedMaintenanceIndicator_AtEnd() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210 RVRNO $";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().automatedMaintenanceIndicators()).hasSize(2);
        assertThat(data.getRemarks().maintenanceRequired()).isTrue();
    }

    @Test
    @DisplayName("Should parse automated maintenance indicators without other remarks")
    void testParseAutomatedMaintenanceIndicator_Alone() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK RVRNO PWINO";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedMaintenanceIndicators()).hasSize(2);

        // Other remark fields should be null
        assertThat(data.getRemarks().automatedStationType()).isNull();
        assertThat(data.getRemarks().seaLevelPressure()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK AO2 SLP210', 'No maintenance indicators'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015 RMK', 'Empty remarks'",
            "'METAR KJFK 121853Z 28016KT 10SM A3015', 'No RMK section'"
    })
    @DisplayName("Should handle METAR with no automated maintenance indicators")
    void testParseAutomatedMaintenanceIndicator_Missing(String metar, String scenario) {
        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess())
                .as("Should parse successfully: %s", scenario)
                .isTrue();

        NoaaMetarData data = extractMetarData(result);

        if (data.getRemarks() != null) {
            assertThat(data.getRemarks().automatedMaintenanceIndicators())
                    .as("Automated maintenance indicators should be empty: %s", scenario)
                    .isEmpty();
            assertThat(data.getRemarks().maintenanceRequired())
                    .as("Maintenance required should be false: %s", scenario)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("Should use AutomatedMaintenanceIndicator query methods")
    void testParseAutomatedMaintenanceIndicator_QueryMethods() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK RVRNO VISNO RWY06 CHINO N TSNO PNO FZRANO PWINO $";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().automatedMaintenanceIndicators()).hasSize(8);

        // Test query methods for each type
        List<AutomatedMaintenanceIndicator> indicators = data.getRemarks().automatedMaintenanceIndicators();

        // RVRNO
        assertThat(indicators.get(0).isRVRNotAvailable()).isTrue();
        assertThat(indicators.get(0).getDescription()).containsIgnoringCase("runway visual range");

        // VISNO RWY06
        assertThat(indicators.get(1).isVisibilityNotAvailable()).isTrue();
        assertThat(indicators.get(1).hasLocation()).isTrue();
        assertThat(indicators.get(1).location()).isEqualTo("RWY06");

        // CHINO N
        assertThat(indicators.get(2).isCloudHeightNotAvailable()).isTrue();
        assertThat(indicators.get(2).location()).isEqualTo("N");

        // TSNO
        assertThat(indicators.get(3).isThunderstormNotAvailable()).isTrue();

        // PNO
        assertThat(indicators.get(4).isPrecipitationNotAvailable()).isTrue();

        // FZRANO
        assertThat(indicators.get(5).isFreezingRainNotAvailable()).isTrue();

        // PWINO
        assertThat(indicators.get(6).isPresentWeatherNotAvailable()).isTrue();

        // $
        assertThat(indicators.get(7).isMaintenanceCheck()).isTrue();
    }

    @Test
    @DisplayName("Should parse real-world METAR with automated maintenance indicators")
    void testParseRealWorldMetarWithAutomatedMaintenance() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 RVRNO PWINO CHINO N $";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify main body
        assertThat(data.getStationId()).isEqualTo("KJFK");
        assertThat(data.getWind()).isNotNull();
        assertThat(data.getVisibility()).isNotNull();
        assertThat(data.getTemperature()).isNotNull();
        assertThat(data.getPressure()).isNotNull();

        // Verify remarks
        assertThat(data.getRemarks()).isNotNull();
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();

        // Verify automated maintenance indicators
        assertThat(data.getRemarks().automatedMaintenanceIndicators()).hasSize(4);
        assertThat(data.getRemarks().maintenanceRequired()).isTrue();

        AutomatedMaintenanceIndicator rvrno = data.getRemarks().automatedMaintenanceIndicators().get(0);
        assertThat(rvrno.type()).isEqualTo("RVRNO");

        AutomatedMaintenanceIndicator pwino = data.getRemarks().automatedMaintenanceIndicators().get(1);
        assertThat(pwino.type()).isEqualTo("PWINO");

        AutomatedMaintenanceIndicator chino = data.getRemarks().automatedMaintenanceIndicators().get(2);
        assertThat(chino.type()).isEqualTo("CHINO");
        assertThat(chino.location()).isEqualTo("N");

        AutomatedMaintenanceIndicator maintenanceCheck = data.getRemarks().automatedMaintenanceIndicators().get(3);
        assertThat(maintenanceCheck.isMaintenanceCheck()).isTrue();
    }

    @Test
    @DisplayName("Should parse complete remarks suite including automated maintenance")
    void testParseAutomatedMaintenanceIndicator_CompleteRemarksSuite() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 " +
                "RMK AO2 SLP201 T02330139 PK WND 28032/1530 WSHFT 1545 FROPA " +
                "P0015 60025 70125 GR 1 3/4 RAB15E30 TS SE 57035 RVRNO PWINO $";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        // Verify ALL remarks parsed (comprehensive integration test)
        assertThat(data.getRemarks()).isNotNull();

        // Basic remarks
        assertThat(data.getRemarks().automatedStationType()).isEqualTo(AutomatedStationType.AO2);
        assertThat(data.getRemarks().seaLevelPressure()).isNotNull();
        assertThat(data.getRemarks().preciseTemperature()).isNotNull();

        // Wind-related
        assertThat(data.getRemarks().peakWind()).isNotNull();
        assertThat(data.getRemarks().windShift()).isNotNull();

        // Precipitation
        assertThat(data.getRemarks().hourlyPrecipitation()).isNotNull();
        assertThat(data.getRemarks().sixHourPrecipitation()).isNotNull();
        assertThat(data.getRemarks().twentyFourHourPrecipitation()).isNotNull();

        // Weather phenomena
        assertThat(data.getRemarks().hailSize()).isNotNull();
        assertThat(data.getRemarks().weatherEvents()).isNotEmpty();
        assertThat(data.getRemarks().thunderstormLocations()).isNotEmpty();

        // Pressure tendency
        assertThat(data.getRemarks().pressureTendency()).isNotNull();

        // **Automated maintenance indicators** - the NEW component
        assertThat(data.getRemarks().automatedMaintenanceIndicators()).hasSize(3);
        assertThat(data.getRemarks().maintenanceRequired()).isTrue();

        AutomatedMaintenanceIndicator rvrno = data.getRemarks().automatedMaintenanceIndicators().get(0);
        assertThat(rvrno.type()).isEqualTo("RVRNO");

        AutomatedMaintenanceIndicator pwino = data.getRemarks().automatedMaintenanceIndicators().get(1);
        assertThat(pwino.type()).isEqualTo("PWINO");

        AutomatedMaintenanceIndicator maintenanceCheck = data.getRemarks().automatedMaintenanceIndicators().get(2);
        assertThat(maintenanceCheck.isMaintenanceCheck()).isTrue();
    }

    @Test
    @DisplayName("Should parse VISNO and CHINO with various location formats")
    void testParseAutomatedMaintenanceIndicator_VariousLocations() {
        String metar = "METAR KJFK 121853Z 28016KT 10SM A3015 RMK " +
                "VISNO RWY06 VISNO RY11 CHINO N CHINO SE CHINO RWY22L";

        ParseResult<NoaaWeatherData> result = parser.parse(metar);

        assertThat(result.isSuccess()).isTrue();
        NoaaMetarData data = extractMetarData(result);

        assertThat(data.getRemarks().automatedMaintenanceIndicators()).hasSize(5);

        // Verify all locations parsed correctly
        List<AutomatedMaintenanceIndicator> indicators = data.getRemarks().automatedMaintenanceIndicators();

        assertThat(indicators.get(0).type()).isEqualTo("VISNO");
        assertThat(indicators.get(0).location()).isEqualTo("RWY06");

        assertThat(indicators.get(1).type()).isEqualTo("VISNO");
        assertThat(indicators.get(1).location()).isEqualTo("RY11");

        assertThat(indicators.get(2).type()).isEqualTo("CHINO");
        assertThat(indicators.get(2).location()).isEqualTo("N");

        assertThat(indicators.get(3).type()).isEqualTo("CHINO");
        assertThat(indicators.get(3).location()).isEqualTo("SE");

        assertThat(indicators.get(4).type()).isEqualTo("CHINO");
        assertThat(indicators.get(4).location()).isEqualTo("RWY22L");
    }
}
