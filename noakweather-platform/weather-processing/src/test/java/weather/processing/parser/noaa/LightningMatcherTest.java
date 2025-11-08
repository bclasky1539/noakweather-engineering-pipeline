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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for LightningMatcher wrapper class.
 * Enhanced to achieve 95%+ code coverage.
 * 
 * @author bclasky1539
 *
 */
class LightningMatcherTest {
    
    @Test
    void testSimpleLightning() {
        LightningMatcher matcher = new LightningMatcher("LTG DSNT ");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("loc")).isEqualTo("DSNT");
        assertThat(matcher.group("freq")).isNull();
        assertThat(matcher.hasAnyTypes()).isFalse();
    }
    
    @Test
    void testLightningWithFrequency() {
        LightningMatcher matcher = new LightningMatcher("OCNL LTG VC ");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("freq")).isEqualTo("OCNL");
        assertThat(matcher.group("loc")).isEqualTo("VC");
    }
    
    @Test
    void testLightningWithSingleType() {
        LightningMatcher matcher = new LightningMatcher("LTGIC OHD ");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("typeic")).isEqualTo("IC");
        assertThat(matcher.group("typecc")).isNull();
        assertThat(matcher.group("typecg")).isNull();
        assertThat(matcher.hasType("IC")).isTrue();
        assertThat(matcher.hasType("CC")).isFalse();
    }
    
    @Test
    void testLightningWithMultipleTypes() {
        LightningMatcher matcher = new LightningMatcher("FRQ LTGICCCCG VC N-NE ");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("freq")).isEqualTo("FRQ");
        assertThat(matcher.group("typeic")).isEqualTo("IC");
        assertThat(matcher.group("typecc")).isEqualTo("CC");
        assertThat(matcher.group("typecg")).isEqualTo("CG");
        assertThat(matcher.group("typeca")).isNull();
        assertThat(matcher.group("typecw")).isNull();
        assertThat(matcher.group("loc")).isEqualTo("VC");
        assertThat(matcher.group("dir")).isEqualTo("N");
        assertThat(matcher.group("dir2")).isEqualTo("NE");
        assertThat(matcher.getTypesString()).isEqualTo("ICCCCG");
    }
    
    @Test
    void testLightningWithAllTypes() {
        LightningMatcher matcher = new LightningMatcher("CONS LTGICCCCGCACW OHD ");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.hasType("IC")).isTrue();
        assertThat(matcher.hasType("CC")).isTrue();
        assertThat(matcher.hasType("CG")).isTrue();
        assertThat(matcher.hasType("CA")).isTrue();
        assertThat(matcher.hasType("CW")).isTrue();
        assertThat(matcher.group("typeic")).isEqualTo("IC");
        assertThat(matcher.group("typecc")).isEqualTo("CC");
        assertThat(matcher.group("typecg")).isEqualTo("CG");
        assertThat(matcher.group("typeca")).isEqualTo("CA");
        assertThat(matcher.group("typecw")).isEqualTo("CW");
    }
    
    @ParameterizedTest
    @CsvSource({
        "OCNL, OCNL",
        "FRQ, FRQ",
        "CONS, CONS"
    })
    void testVariousFrequencies(String input, String expectedFreq) {
        LightningMatcher matcher = new LightningMatcher(input + " LTG DSNT ");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("freq")).isEqualTo(expectedFreq);
    }
    
    @ParameterizedTest
    @CsvSource({
        "OHD, OHD",
        "VC, VC",
        "DSNT, DSNT"
    })
    void testVariousLocations(String input, String expectedLoc) {
        LightningMatcher matcher = new LightningMatcher("LTG " + input + " ");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("loc")).isEqualTo(expectedLoc);
    }
    
    @Test
    void testLightningWithDirectionOnly() {
        LightningMatcher matcher = new LightningMatcher("LTG DSNT NE ");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("dir")).isEqualTo("NE");
        assertThat(matcher.group("dir2")).isNull();
    }
    
    @Test
    void testLightningWithDirectionRange() {
        LightningMatcher matcher = new LightningMatcher("LTGCG VC SE-S ");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("dir")).isEqualTo("SE");
        assertThat(matcher.group("dir2")).isEqualTo("S");
    }
    
    @Test
    void testNoMatch() {
        LightningMatcher matcher = new LightningMatcher("NOTLIGHTNING ");
        
        assertThat(matcher.find()).isFalse();
    }
    
    @Test
    void testReplaceFirst() {
        LightningMatcher matcher = new LightningMatcher("OCNL LTG VC ");
        
        assertThat(matcher.find()).isTrue();
        String replaced = matcher.replaceFirst("");
        assertThat(replaced).isEmpty();
    }
    
    // ========== NEW TESTS FOR IMPROVED COVERAGE ==========
    
    @Test
    void testHasAnyTypes_WhenNoTypes() {
        LightningMatcher matcher = new LightningMatcher("LTG DSNT ");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.hasAnyTypes()).isFalse();
        assertThat(matcher.getTypesString()).isNull();
    }
    
    @Test
    void testHasAnyTypes_WhenHasTypes() {
        LightningMatcher matcher = new LightningMatcher("LTGIC OHD ");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.hasAnyTypes()).isTrue();
        assertThat(matcher.getTypesString()).isEqualTo("IC");
    }
    
    @Test
    void testHasAnyTypes_BeforeFind() {
        LightningMatcher matcher = new LightningMatcher("LTGIC OHD ");
        
        // Before calling find(), types should be null
        assertThat(matcher.hasAnyTypes()).isFalse();
    }
    
    @Test
    void testHasType_WithNullTypes() {
        LightningMatcher matcher = new LightningMatcher("LTG DSNT ");
        
        assertThat(matcher.find()).isTrue();
        // types is null, so hasType should return false
        assertThat(matcher.hasType("IC")).isFalse();
        assertThat(matcher.hasType("CC")).isFalse();
    }
    
    @Test
    void testHasType_PartialMatch() {
        LightningMatcher matcher = new LightningMatcher("LTGCG OHD ");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.hasType("CG")).isTrue();
        assertThat(matcher.hasType("IC")).isFalse();
        assertThat(matcher.hasType("CA")).isFalse();
    }
    
    @Test
    void testGroup_NoArgument() {
        LightningMatcher matcher = new LightningMatcher("OCNL LTGIC VC ");
        
        assertThat(matcher.find()).isTrue();
        // group() with no argument returns entire match
        String entireMatch = matcher.group();
        assertThat(entireMatch).isNotNull();
        assertThat(entireMatch).contains("OCNL");
        assertThat(entireMatch).contains("LTG");
        assertThat(entireMatch).contains("VC");
    }
    
    @Test
    void testGroup_WithIndex() {
        LightningMatcher matcher = new LightningMatcher("OCNL LTGIC VC ");
        
        assertThat(matcher.find()).isTrue();
        // group(0) returns entire match (same as group())
        String group0 = matcher.group(0);
        assertThat(group0).isNotNull();
        assertThat(group0).isEqualTo(matcher.group());
    }
    
    @Test
    void testGetTypesString_WhenNull() {
        LightningMatcher matcher = new LightningMatcher("LTG DSNT ");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.getTypesString()).isNull();
    }
    
    @Test
    void testGetTypesString_WhenPresent() {
        LightningMatcher matcher = new LightningMatcher("LTGICCCCGCA OHD ");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.getTypesString()).isEqualTo("ICCCCGCA");
    }
    
    @Test
    void testMultipleFindCalls() {
        LightningMatcher matcher = new LightningMatcher("LTG DSNT ");
        
        // First find should succeed
        assertThat(matcher.find()).isTrue();
        
        // Second find should fail (no more matches)
        assertThat(matcher.find()).isFalse();
    }
    
    @Test
    void testUnknownGroupName() {
        LightningMatcher matcher = new LightningMatcher("LTG DSNT ");
        
        assertThat(matcher.find()).isTrue();
        
        // Unknown group names throw IllegalArgumentException (Java Matcher behavior)
        assertThatThrownBy(() -> matcher.group("unknowngroup"))
            .isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    void testReplaceFirst_WithReplacement() {
        LightningMatcher matcher = new LightningMatcher("OCNL LTG VC  rest of text");
        
        assertThat(matcher.find()).isTrue();
        // Add space in replacement since pattern consumes trailing space
        String replaced = matcher.replaceFirst("REPLACED ");
        assertThat(replaced).isEqualTo("REPLACED rest of text");
    }
    
    @Test
    void testEdgeCase_EmptyInput() {
        LightningMatcher matcher = new LightningMatcher("");
        
        assertThat(matcher.find()).isFalse();
        assertThat(matcher.hasAnyTypes()).isFalse();
    }
    
    @Test
    void testEdgeCase_JustWhitespace() {
        LightningMatcher matcher = new LightningMatcher("   ");
        
        assertThat(matcher.find()).isFalse();
    }
}
