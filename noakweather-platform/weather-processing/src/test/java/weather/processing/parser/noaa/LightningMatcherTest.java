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

import org.junit.jupiter.api.DisplayName;
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
        assertThat(matcher.group("dirchain")).isEqualTo("N-NE");
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
        assertThat(matcher.group("dirchain")).isEqualTo("NE");
        assertThat(matcher.group("alqds")).isNull();
    }

    @Test
    void testLightningWithDirectionRange() {
        LightningMatcher matcher = new LightningMatcher("LTGCG VC SE-S ");

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("dirchain"))
                .as("A range is one segment; the parser splits it into [SE, S]")
                .isEqualTo("SE-S");
        assertThat(matcher.group("alqds")).isNull();
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
        assertThat(replaced).isEqualTo(" ");
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
    @DisplayName("hasType should return false for all types when types is null")
    void testHasType_WithNullTypes() {
        LightningMatcher matcher = new LightningMatcher("LTG DSNT ");

        assertThat(matcher.find()).isTrue();
        // types is null, so hasType should return false
        assertThat(matcher.hasType("IC")).isFalse();
        assertThat(matcher.hasType("CC")).isFalse();
        assertThat(matcher.hasType("CG")).isFalse();
        assertThat(matcher.hasType("CA")).isFalse();
        assertThat(matcher.hasType("CW")).isFalse();
    }

    @Test
    @DisplayName("All type groups should return null when types is null")
    void testAllTypeGroups_WhenTypesIsNull() {
        LightningMatcher matcher = new LightningMatcher("LTG DSNT ");

        assertThat(matcher.find()).isTrue();

        // All virtual type groups should return null
        assertThat(matcher.group("typeic")).isNull();
        assertThat(matcher.group("typecc")).isNull();
        assertThat(matcher.group("typecg")).isNull();
        assertThat(matcher.group("typeca")).isNull();
        assertThat(matcher.group("typecw")).isNull();
    }

    @Test
    @DisplayName("hasAnyTypes should return false when no match is found")
    void testHasAnyTypes_NoMatchFound() {
        LightningMatcher matcher = new LightningMatcher("NO LIGHTNING HERE");

        // find() returns false, types remains null
        assertThat(matcher.find()).isFalse();

        // hasAnyTypes should return false
        assertThat(matcher.hasAnyTypes()).isFalse();
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
        assertThat(entireMatch)
                .isNotNull()
                .contains("OCNL")
                .contains("LTG")
                .contains("VC");
    }

    @Test
    void testGroup_WithIndex() {
        LightningMatcher matcher = new LightningMatcher("OCNL LTGIC VC ");

        assertThat(matcher.find()).isTrue();
        // group(0) returns entire match (same as group())
        String group0 = matcher.group(0);
        assertThat(group0)
                .isNotNull()
                .isEqualTo(matcher.group());
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
        String replaced = matcher.replaceFirst("REPLACED");
        assertThat(replaced).isEqualTo("REPLACED  rest of text");
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

    @Test
    @DisplayName("Should expose the AND-chained direction through the wrapper (Issue #117)")
    void testLightningWithAndChain() {
        LightningMatcher matcher = new LightningMatcher("OCNL LTGIC DSNT SE AND S-SW CB DSNT SE AND S-SW");

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("freq")).isEqualTo("OCNL");
        assertThat(matcher.group("loc")).isEqualTo("DSNT");
        assertThat(matcher.group("dirchain")).isEqualTo("SE AND S-SW");
        assertThat(matcher.group("alqds")).isNull();
        assertThat(matcher.group(0).trim())
                .as("Match stops before the CB group")
                .isEqualTo("OCNL LTGIC DSNT SE AND S-SW");
    }

    @Test
    @DisplayName("Should keep virtual type groups working alongside an AND chain (Issue #117)")
    void testVirtualTypeGroupsWithAndChain() {
        LightningMatcher matcher = new LightningMatcher("FRQ LTGICCG DSNT N AND NE");

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("typeic")).isEqualTo("IC");
        assertThat(matcher.group("typecg")).isEqualTo("CG");
        assertThat(matcher.group("typecc")).isNull();
        assertThat(matcher.getTypesString()).isEqualTo("ICCG");
        assertThat(matcher.group("dirchain")).isEqualTo("N AND NE");
    }

    @Test
    @DisplayName("Should expose ALQDS through the alqds group, not the direction chain")
    void testLightningWithAllQuadrants() {
        LightningMatcher matcher = new LightningMatcher("LTG DSNT ALQDS");

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group("alqds")).isEqualTo("ALQDS");
        assertThat(matcher.group("dirchain")).isNull();
    }

    @Test
    @DisplayName("Should reject the removed dir group name")
    void testRemovedDirGroupThrows() {
        LightningMatcher matcher = new LightningMatcher("LTG DSNT N");

        assertThat(matcher.find()).isTrue();
        assertThatThrownBy(() -> matcher.group("dir"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "{0}: IC={1} CC={2} CG={3} CA={4} CW={5}")
    @CsvSource({
            "ICCG,   true,  false, true,  false, false",
            "ICCA,   true,  false, false, true,  false",
            "ICCW,   true,  false, false, false, true",
            "ICCCCG, true,  true,  true,  false, false",
            "CCCG,   false, true,  true,  false, false",
            "CGIC,   true,  false, true,  false, false",
            "CWCA,   false, false, false, true,  true"
    })
    @DisplayName("hasType should check only 2-letter-aligned codes, not substrings across code boundaries")
    void testHasType_AlignedToCodeBoundaries(String types, boolean ic, boolean cc,
                                             boolean cg, boolean ca, boolean cw) {
        LightningMatcher matcher = new LightningMatcher("LTG" + types + " DSNT N");

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.getTypesString()).isEqualTo(types);
        assertThat(matcher.hasType("IC")).as("IC in %s", types).isEqualTo(ic);
        assertThat(matcher.hasType("CC")).as("CC in %s", types).isEqualTo(cc);
        assertThat(matcher.hasType("CG")).as("CG in %s", types).isEqualTo(cg);
        assertThat(matcher.hasType("CA")).as("CA in %s", types).isEqualTo(ca);
        assertThat(matcher.hasType("CW")).as("CW in %s", types).isEqualTo(cw);
    }

    @Test
    @DisplayName("hasType should return false for null, wrong-length, or no types captured")
    void testHasType_EdgeCases() {
        LightningMatcher withTypes = new LightningMatcher("LTGICCG DSNT N");
        assertThat(withTypes.find()).isTrue();
        assertThat(withTypes.hasType(null)).isFalse();
        assertThat(withTypes.hasType("C")).isFalse();
        assertThat(withTypes.hasType("ICC")).isFalse();

        LightningMatcher noTypes = new LightningMatcher("LTG DSNT N");
        assertThat(noTypes.find()).isTrue();
        assertThat(noTypes.hasType("IC")).isFalse();
    }
}
