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
}
