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
package weather.utils;

import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive tests for AviationWeatherDecoder.
 * 
 * Tests all ICAO standard aviation weather codes including sky coverage,
 * weather phenomena, intensity indicators, descriptors, and cloud types.
 * 
 * @author bclasky1539
 * 
 */
class AviationWeatherDecoderTest {
    
    // ==================== Sky Coverage Tests ====================
    
    @ParameterizedTest
    @CsvSource({
        "SKC, Sky Clear",
        "CLR, Clear",
        "NSC, No Significant Clouds",
        "NCD, No Clouds Detected",
        "FEW, Few",
        "SCT, Scattered",
        "BKN, Broken",
        "OVC, Overcast",
        "VV, Vertical Visibility",
        "///, Sky Obscured"
    })
    void testDecodeSkyCoverage_StandardCodes(String code, String expected) {
        assertThat(AviationWeatherDecoder.decodeSkyCoverage(code)).isEqualTo(expected);
    }
    
    @Test
    void testDecodeSkyCoverage_CaseInsensitive() {
        assertThat(AviationWeatherDecoder.decodeSkyCoverage("bkn")).isEqualTo("Broken");
        assertThat(AviationWeatherDecoder.decodeSkyCoverage("BKN")).isEqualTo("Broken");
        assertThat(AviationWeatherDecoder.decodeSkyCoverage("Bkn")).isEqualTo("Broken");
        assertThat(AviationWeatherDecoder.decodeSkyCoverage("bKn")).isEqualTo("Broken");
    }
    
    @Test
    void testDecodeSkyCoverage_WithWhitespace() {
        assertThat(AviationWeatherDecoder.decodeSkyCoverage(" BKN ")).isEqualTo("Broken");
        assertThat(AviationWeatherDecoder.decodeSkyCoverage("  OVC  ")).isEqualTo("Overcast");
    }
    
    @Test
    void testDecodeSkyCoverage_UnknownCode() {
        assertThat(AviationWeatherDecoder.decodeSkyCoverage("XXX")).isEqualTo("XXX");
        assertThat(AviationWeatherDecoder.decodeSkyCoverage("ABC")).isEqualTo("ABC");
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testDecodeSkyCoverage_NullOrBlank(String code) {
        assertThat(AviationWeatherDecoder.decodeSkyCoverage(code)).isEmpty();
    }
    
    // ==================== Weather Phenomena Tests ====================
    
    @ParameterizedTest
    @CsvSource({
        // Precipitation
        "DZ, Drizzle",
        "RA, Rain",
        "SN, Snow",
        "SG, Snow Grains",
        "IC, Ice Crystals",
        "PL, Ice Pellets",
        "GR, Hail",
        "GS, Small Hail",
        "UP, Unknown Precipitation",
        // Obscuration
        "BR, Mist",
        "FG, Fog",
        "FU, Smoke",
        "VA, Volcanic Ash",
        "DU, Widespread Dust",
        "SA, Sand",
        "HZ, Haze",
        "PY, Spray",
        // Other
        "PO, Dust/Sand Whirls",
        "SQ, Squalls",
        "FC, Funnel Cloud",
        "SS, Sandstorm",
        "DS, Duststorm"
    })
    void testDecodeWeatherPhenomenon_StandardCodes(String code, String expected) {
        assertThat(AviationWeatherDecoder.decodeWeatherPhenomenon(code)).isEqualTo(expected);
    }
    
    @Test
    void testDecodeWeatherPhenomenon_CaseInsensitive() {
        assertThat(AviationWeatherDecoder.decodeWeatherPhenomenon("ra")).isEqualTo("Rain");
        assertThat(AviationWeatherDecoder.decodeWeatherPhenomenon("RA")).isEqualTo("Rain");
        assertThat(AviationWeatherDecoder.decodeWeatherPhenomenon("Ra")).isEqualTo("Rain");
    }
    
    @Test
    void testDecodeWeatherPhenomenon_UnknownCode() {
        assertThat(AviationWeatherDecoder.decodeWeatherPhenomenon("ZZ")).isEqualTo("ZZ");
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void testDecodeWeatherPhenomenon_NullOrBlank(String code) {
        assertThat(AviationWeatherDecoder.decodeWeatherPhenomenon(code)).isEmpty();
    }
    
    // ==================== Intensity Tests ====================
    
    @ParameterizedTest
    @CsvSource({
        "-, Light",
        "+, Heavy",
        "VC, In the Vicinity"
    })
    void testDecodeIntensity_ValidCodes(String code, String expected) {
        assertThat(AviationWeatherDecoder.decodeIntensity(code)).isEqualTo(expected);
    }
    
    @Test
    void testDecodeIntensity_UnknownCode() {
        assertThat(AviationWeatherDecoder.decodeIntensity("X")).isEmpty();
        assertThat(AviationWeatherDecoder.decodeIntensity("=")).isEmpty();
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void testDecodeIntensity_NullOrBlank(String code) {
        assertThat(AviationWeatherDecoder.decodeIntensity(code)).isEmpty();
    }
    
    // ==================== Descriptor Tests ====================
    
    @ParameterizedTest
    @CsvSource({
        "MI, Shallow",
        "PR, Partial",
        "BC, Patches",
        "DR, Low Drifting",
        "BL, Blowing",
        "SH, Shower(s)",
        "TS, Thunderstorm",
        "FZ, Freezing"
    })
    void testDecodeDescriptor_ValidCodes(String code, String expected) {
        assertThat(AviationWeatherDecoder.decodeDescriptor(code)).isEqualTo(expected);
    }
    
    @Test
    void testDecodeDescriptor_CaseInsensitive() {
        assertThat(AviationWeatherDecoder.decodeDescriptor("sh")).isEqualTo("Shower(s)");
        assertThat(AviationWeatherDecoder.decodeDescriptor("SH")).isEqualTo("Shower(s)");
        assertThat(AviationWeatherDecoder.decodeDescriptor("Sh")).isEqualTo("Shower(s)");
    }
    
    @Test
    void testDecodeDescriptor_WithWhitespace() {
        assertThat(AviationWeatherDecoder.decodeDescriptor(" SH ")).isEqualTo("Shower(s)");
        assertThat(AviationWeatherDecoder.decodeDescriptor("  TS  ")).isEqualTo("Thunderstorm");
    }
    
    @Test
    void testDecodeDescriptor_UnknownCode() {
        assertThat(AviationWeatherDecoder.decodeDescriptor("XY")).isEqualTo("XY");
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void testDecodeDescriptor_NullOrBlank(String code) {
        assertThat(AviationWeatherDecoder.decodeDescriptor(code)).isEmpty();
    }
    
    // ==================== Cloud Type Tests ====================
    
    @ParameterizedTest
    @CsvSource({
        "CB, Cumulonimbus",
        "TCU, Towering Cumulus",
        "CU, Cumulus",
        "SC, Stratocumulus",
        "ST, Stratus",
        "NS, Nimbostratus",
        "AS, Altostratus",
        "AC, Altocumulus",
        "CI, Cirrus",
        "CC, Cirrocumulus",
        "CS, Cirrostratus"
    })
    void testDecodeCloudType_StandardCodes(String code, String expected) {
        assertThat(AviationWeatherDecoder.decodeCloudType(code)).isEqualTo(expected);
    }
    
    @Test
    void testDecodeCloudType_CaseInsensitive() {
        assertThat(AviationWeatherDecoder.decodeCloudType("cb")).isEqualTo("Cumulonimbus");
        assertThat(AviationWeatherDecoder.decodeCloudType("CB")).isEqualTo("Cumulonimbus");
        assertThat(AviationWeatherDecoder.decodeCloudType("Cb")).isEqualTo("Cumulonimbus");
    }
    
    @Test
    void testDecodeCloudType_UnknownCode() {
        assertThat(AviationWeatherDecoder.decodeCloudType("XX")).isEqualTo("XX");
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void testDecodeCloudType_NullOrBlank(String code) {
        assertThat(AviationWeatherDecoder.decodeCloudType(code)).isEmpty();
    }
    
    // ==================== Complete Weather String Tests ====================
    
    @ParameterizedTest
    @CsvSource({
        // Simple phenomena
        "RA, Rain",
        "SN, Snow",
        "FG, Fog",
        "BR, Mist",
        
        // With intensity
        "-RA, Light Rain",
        "+RA, Heavy Rain",
        "-SN, Light Snow",
        "+SN, Heavy Snow",
        
        // With descriptor
        "SHRA, Shower(s) Rain",
        "TSRA, Thunderstorm Rain",
        "FZRA, Freezing Rain",
        "FZDZ, Freezing Drizzle",
        "BLSN, Blowing Snow",
        "DRSN, Low Drifting Snow",
        
        // With intensity and descriptor
        "-SHRA, Light Shower(s) Rain",
        "+SHRA, Heavy Shower(s) Rain",
        "-TSRA, Light Thunderstorm Rain",
        "+TSRA, Heavy Thunderstorm Rain",
        "-FZRA, Light Freezing Rain",
        "+FZRA, Heavy Freezing Rain",
        
        // Multiple phenomena
        "RASN, Rain Snow",
        "SNRA, Snow Rain",
        "-RASN, Light Rain Snow",
        "+RASN, Heavy Rain Snow",
        
        // With descriptor and multiple phenomena
        "TSRASN, Thunderstorm Rain Snow",
        "+TSRASN, Heavy Thunderstorm Rain Snow",
        "SHRASN, Shower(s) Rain Snow",
        
        // Complex combinations
        "+TSRAGR, Heavy Thunderstorm Rain Hail",
        "-FZDZFG, Light Freezing Drizzle Fog",
        "SHSN, Shower(s) Snow",
        "TSGS, Thunderstorm Small Hail",
        
        // Vicinity
        "VCFG, Fog In the Vicinity",
        "VCTS, Thunderstorm In the Vicinity",
        "VCSH, Shower(s) In the Vicinity",
        
        // Obscuration
        "MIFG, Shallow Fog",
        "BCFG, Patches Fog",
        "PRFG, Partial Fog"
    })
    void testDecodeWeather_StandardPatterns(String code, String expected) {
        assertThat(AviationWeatherDecoder.decodeWeather(code)).isEqualTo(expected);
    }
    
    @Test
    void testDecodeWeather_CaseInsensitive() {
        assertThat(AviationWeatherDecoder.decodeWeather("ra")).isEqualTo("Rain");
        assertThat(AviationWeatherDecoder.decodeWeather("RA")).isEqualTo("Rain");
        assertThat(AviationWeatherDecoder.decodeWeather("Ra")).isEqualTo("Rain");
        assertThat(AviationWeatherDecoder.decodeWeather("+tsra")).isEqualTo("Heavy Thunderstorm Rain");
    }
    
    @Test
    void testDecodeWeather_WithWhitespace() {
        assertThat(AviationWeatherDecoder.decodeWeather(" RA ")).isEqualTo("Rain");
        assertThat(AviationWeatherDecoder.decodeWeather("  +TSRA  ")).isEqualTo("Heavy Thunderstorm Rain");
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testDecodeWeather_NullOrBlank(String code) {
        assertThat(AviationWeatherDecoder.decodeWeather(code)).isEmpty();
    }
    
    @Test
    void testDecodeWeather_PartiallyUnknownCode() {
        // Known intensity, unknown phenomenon
        assertThat(AviationWeatherDecoder.decodeWeather("+XX")).isEqualTo("Heavy XX");
        
        // Known descriptor, unknown phenomenon
        assertThat(AviationWeatherDecoder.decodeWeather("SHXX")).isEqualTo("Shower(s) XX");
        
        // Mix of known and unknown
        assertThat(AviationWeatherDecoder.decodeWeather("RAXX")).isEqualTo("Rain XX");
    }
    
    @Test
    void testDecodeWeather_RealWorldExamples() {
        // From actual METARs
        assertThat(AviationWeatherDecoder.decodeWeather("-SHRA")).isEqualTo("Light Shower(s) Rain");
        assertThat(AviationWeatherDecoder.decodeWeather("+TSRA")).isEqualTo("Heavy Thunderstorm Rain");
        assertThat(AviationWeatherDecoder.decodeWeather("VCSH")).isEqualTo("Shower(s) In the Vicinity");
        assertThat(AviationWeatherDecoder.decodeWeather("FZFG")).isEqualTo("Freezing Fog");
        assertThat(AviationWeatherDecoder.decodeWeather("BLSN")).isEqualTo("Blowing Snow");
        assertThat(AviationWeatherDecoder.decodeWeather("-DZ")).isEqualTo("Light Drizzle");
        assertThat(AviationWeatherDecoder.decodeWeather("BR")).isEqualTo("Mist");
    }
    
    // ==================== Utility Class Tests ====================
    
    @Test
    void testConstructor_ThrowsAssertionError() throws NoSuchMethodException {
        var constructor = AviationWeatherDecoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        assertThatThrownBy(constructor::newInstance)
            .isInstanceOf(InvocationTargetException.class)
            .hasCauseInstanceOf(AssertionError.class)
            .hasRootCauseMessage("Utility class should not be instantiated");
    }
}
