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
import weather.model.NoaaWeatherData;
import weather.processing.parser.common.ParseResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

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
        NoaaWeatherData data = result.getData().get();
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
            assertEquals(station, result.getData().get().getStationId());
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
        assertEquals(expectedStationId, result.getData().get().getStationId());
    }
    
    @Test
    @DisplayName("Should trim raw data when storing")
    void testRawDataIsTrimmed() {
        String metar = "  METAR KJFK 251651Z 28016KT  ";
        
        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        
        assertTrue(result.isSuccess());
        assertEquals(metar.trim(), result.getData().get().getRawData());
    }
    
    @Test
    @DisplayName("Should parse real-world METAR example - JFK")
    void testParseRealWorldJFK() {
        String metar = "METAR KJFK 121851Z 24008KT 10SM FEW250 23/14 A3012 RMK AO2 SLP201 T02330139";
        
        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        
        assertTrue(result.isSuccess());
        NoaaWeatherData data = result.getData().get();
        assertEquals("KJFK", data.getStationId());
        assertEquals("METAR", data.getReportType());
    }
    
    @Test
    @DisplayName("Should parse real-world METAR example - LAX")
    void testParseRealWorldLAX() {
        String metar = "METAR KLAX 121853Z 26008KT 10SM FEW015 SCT250 21/16 A2990 RMK AO2 SLP127";
        
        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        
        assertTrue(result.isSuccess());
        NoaaWeatherData data = result.getData().get();
        assertEquals("KLAX", data.getStationId());
    }
 
    @Test
    @DisplayName("Should parse METAR with remarks section")
    void testParseWithRemarks() {
        String metar = "METAR KATL 251651Z 28016KT 10SM FEW250 22/12 A3015 RMK AO2 SLP210 T02220117";
        
        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getData().get().getRawData().contains("RMK"));
    }
    
    @Test
    @DisplayName("Should set observation time when parsing")
    void testObservationTimeIsSet() {
        String metar = "METAR KJFK 251651Z 28016KT 10SM";
        
        ParseResult<NoaaWeatherData> result = parser.parse(metar);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData().get().getObservationTime());
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
}
