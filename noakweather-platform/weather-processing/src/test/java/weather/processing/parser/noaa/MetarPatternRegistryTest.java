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
import weather.utils.IndexedLinkedHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MetarPatternRegistry.
 * 
 * @author bclasky1539
 *
 */
class MetarPatternRegistryTest {
    
    private final MetarPatternRegistry registry = new MetarPatternRegistry();
    
    @Test
    void testGetMainHandlersReturnsIndexedLinkedHashMap() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getMainHandlers();
        
        assertThat(handlers)
                .isNotNull()
                .isInstanceOf(IndexedLinkedHashMap.class);
    }
    
    @Test
    void testGetMainHandlersHasExpectedPatterns() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getMainHandlers();
        
        // Should contain key patterns
        assertThat(handlers)
                .containsKey(RegExprConst.STATION_DAY_TIME_VALTMPER_PATTERN)
                .containsKey(RegExprConst.WIND_PATTERN)
                .containsKey(RegExprConst.VISIBILITY_PATTERN)
                .containsKey(RegExprConst.TEMP_DEWPOINT_PATTERN)
                .containsKey(RegExprConst.ALTIMETER_PATTERN);
    }
    
    @Test
    void testGetMainHandlersMaintainsOrder() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getMainHandlers();
        
        // Use IndexedLinkedHashMap's index access
        int stationIndex = handlers.getIndexOf(RegExprConst.STATION_DAY_TIME_VALTMPER_PATTERN);
        int windIndex = handlers.getIndexOf(RegExprConst.WIND_PATTERN);
        int tempIndex = handlers.getIndexOf(RegExprConst.TEMP_DEWPOINT_PATTERN);
        
        // Station should come before wind
        assertThat(stationIndex).isLessThan(windIndex);
        
        // Wind should come before temperature
        assertThat(windIndex).isLessThan(tempIndex);
    }
    
    @Test
    void testGetMainHandlersIndexAccess() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getMainHandlers();

        // Test index-based access (unique feature of IndexedLinkedHashMap)
        Pattern firstPattern = handlers.getKeyAtIndex(0);
        assertThat(firstPattern).isNotNull();

        MetarPatternHandler firstHandler = handlers.getValueAtIndex(0);
        assertThat(firstHandler).isNotNull();

        // First pattern should be reportType (METAR|SPECI)
        assertThat(firstPattern.pattern()).isEqualTo("^(METAR|SPECI)\\s+");
        assertThat(firstHandler.handlerName()).isEqualTo("reportType");
        assertThat(firstHandler.canRepeat()).isFalse();

        // Second pattern should be month/day/year (what used to be first)
        Pattern secondPattern = handlers.getKeyAtIndex(1);
        assertThat(secondPattern).isEqualTo(RegExprConst.MONTH_DAY_YEAR_PATTERN);
        MetarPatternHandler secondHandler = handlers.getValueAtIndex(1);
        assertThat(secondHandler.handlerName()).isEqualTo("monthDayYear");
    }
    
    @Test
    void testGetMainHandlersRepeatingPatterns() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getMainHandlers();
        
        // Sky condition should be repeating
        MetarPatternHandler skyHandler = handlers.get(RegExprConst.SKY_CONDITION_PATTERN);
        assertThat(skyHandler.canRepeat()).isTrue();
        
        // Wind should NOT be repeating
        MetarPatternHandler windHandler = handlers.get(RegExprConst.WIND_PATTERN);
        assertThat(windHandler.canRepeat()).isFalse();
    }
    
    @Test
    void testGetRemarksHandlersReturnsIndexedLinkedHashMap() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();
        
        assertThat(handlers)
                .isNotNull()
                .isInstanceOf(IndexedLinkedHashMap.class);
    }
    
    @Test
    void testGetRemarksHandlersHasExpectedPatterns() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();
        
        // Should contain key remarks patterns
        assertThat(handlers)
                .containsKey(RegExprConst.AUTO_PATTERN)
                .containsKey(RegExprConst.SEALVL_PRESS_PATTERN)
                .containsKey(RegExprConst.PEAK_WIND_PATTERN)
                .containsKey(RegExprConst.TEMP_1HR_PATTERN)
                .containsKey(RegExprConst.HAIL_SIZE_PATTERN);
    }
    
    @Test
    void testGetRemarksHandlersIndexAccess() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();
        
        // Test that we can access by index
        assertThat(handlers).hasSizeGreaterThan(0);
        
        Pattern firstPattern = handlers.getKeyAtIndex(0);
        assertThat(firstPattern)
                .isNotNull()
                // First remarks pattern should be AUTO (AO1/AO2)
                .isEqualTo(RegExprConst.AUTO_PATTERN);
    }
    
    @Test
    void testGetGroupHandlersReturnsIndexedLinkedHashMap() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getGroupHandlers();
        
        assertThat(handlers)
                .isNotNull()
                .isInstanceOf(IndexedLinkedHashMap.class);
    }
    
    @Test
    void testGetGroupHandlersHasExpectedPatterns() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getGroupHandlers();
        
        // Should contain TAF group patterns
        assertThat(handlers)
                .containsKey(RegExprConst.TAF_STR_PATTERN)
                .containsKey(RegExprConst.GROUP_BECMG_TEMPO_PROB_PATTERN)
                .containsKey(RegExprConst.GROUP_FM_PATTERN);
    }
    
    @Test
    void testAllHandlersHaveValidHandlerNames() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> mainHandlers = registry.getMainHandlers();
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> remarksHandlers = registry.getRemarksHandlers();
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> groupHandlers = registry.getGroupHandlers();
        
        // All handler names should be non-null and non-empty
        mainHandlers.values().forEach(handler -> {
            assertThat(handler.handlerName()).isNotNull();
            assertThat(handler.handlerName()).isNotEmpty();
        });
        
        remarksHandlers.values().forEach(handler -> {
            assertThat(handler.handlerName()).isNotNull();
            assertThat(handler.handlerName()).isNotEmpty();
        });
        
        groupHandlers.values().forEach(handler -> {
            assertThat(handler.handlerName()).isNotNull();
            assertThat(handler.handlerName()).isNotEmpty();
        });
    }
    
    @Test
    void testIndexedLinkedHashMapFeatures() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getMainHandlers();
        
        // Test IndexedLinkedHashMap specific features
        int size = handlers.size();
        assertThat(size).isGreaterThan(0);
        
        // Test that we can iterate by index
        for (int i = 0; i < size; i++) {
            Pattern pattern = handlers.getKeyAtIndex(i);
            MetarPatternHandler handler = handlers.getValueAtIndex(i);
            
            assertThat(pattern).isNotNull();
            assertThat(handler).isNotNull();
            
            // Verify consistency: getting by key should match getting by index
            assertThat(handlers).containsEntry(pattern, handler);
        }
    }

    @Test
    void testGetRemarksHandlersHasHailSizePattern() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();

        // Should contain hail size pattern
        assertThat(handlers)
                .containsKey(RegExprConst.HAIL_SIZE_PATTERN);

        // Verify handler details
        MetarPatternHandler hailSizeHandler = handlers.get(RegExprConst.HAIL_SIZE_PATTERN);
        assertThat(hailSizeHandler).isNotNull();
        assertThat(hailSizeHandler.handlerName()).isEqualTo("hailSize");
        assertThat(hailSizeHandler.canRepeat()).isFalse();
    }

    @Test
    void testGetRemarksHandlersHasBeginEndWeatherPattern() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();

        // Should contain BEGIN_END_WEATHER_PATTERN
        assertThat(handlers)
                .containsKey(RegExprConst.BEGIN_END_WEATHER_PATTERN);

        // Verify handler details
        MetarPatternHandler weatherEventHandler = handlers.get(RegExprConst.BEGIN_END_WEATHER_PATTERN);
        assertThat(weatherEventHandler).isNotNull();
        assertThat(weatherEventHandler.handlerName()).isEqualTo("weatherBeginEnd");
        assertThat(weatherEventHandler.canRepeat()).isTrue();  // Should be repeating for chained events
    }

    @Test
    void testGetRemarksHandlersWeatherEventAfterHailSize() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();

        // Get indices of both patterns
        int hailSizeIndex = handlers.getIndexOf(RegExprConst.HAIL_SIZE_PATTERN);
        int weatherEventIndex = handlers.getIndexOf(RegExprConst.BEGIN_END_WEATHER_PATTERN);

        // Weather events should come after hail size
        assertThat(weatherEventIndex)
                .as("Weather events should be registered after hail size")
                .isGreaterThan(hailSizeIndex);

        // Both should come before unparsed catch-all
        int unparsedIndex = handlers.getIndexOf(RegExprConst.UNPARSED_PATTERN);
        assertThat(hailSizeIndex)
                .as("Hail size should come before unparsed remarks")
                .isLessThan(unparsedIndex);
        assertThat(weatherEventIndex)
                .as("Weather events should come before unparsed remarks")
                .isLessThan(unparsedIndex);
    }

    @Test
    void testGetRemarksHandlersHasVariableCeilingPattern() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();

        // Should contain variable ceiling pattern
        assertThat(handlers)
                .containsKey(RegExprConst.VARIABLE_CEILING_PATTERN);

        // Verify handler details
        MetarPatternHandler variableCeilingHandler = handlers.get(RegExprConst.VARIABLE_CEILING_PATTERN);
        assertThat(variableCeilingHandler).isNotNull();
        assertThat(variableCeilingHandler.handlerName()).isEqualTo("variableCeiling");
        assertThat(variableCeilingHandler.canRepeat()).isFalse();
    }

    @Test
    void testGetRemarksHandlersVariableCeilingAfterVariableVisibility() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();

        // Get indices of both patterns
        int variableVisibilityIndex = handlers.getIndexOf(RegExprConst.VPV_SV_VSL_PATTERN);
        int variableCeilingIndex = handlers.getIndexOf(RegExprConst.VARIABLE_CEILING_PATTERN);

        // Variable ceiling should come after variable visibility
        assertThat(variableCeilingIndex)
                .as("Variable ceiling should be registered after variable visibility")
                .isGreaterThan(variableVisibilityIndex);

        // Both should come before unparsed catch-all
        int unparsedIndex = handlers.getIndexOf(RegExprConst.UNPARSED_PATTERN);
        assertThat(variableVisibilityIndex)
                .as("Variable visibility should come before unparsed remarks")
                .isLessThan(unparsedIndex);
        assertThat(variableCeilingIndex)
                .as("Variable ceiling should come before unparsed remarks")
                .isLessThan(unparsedIndex);
    }

    @Test
    void testGetRemarksHandlersHasCeilingSecondSitePattern() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();

        // Should contain ceiling second site pattern
        assertThat(handlers)
                .containsKey(RegExprConst.CEILING_SECOND_SITE_PATTERN);

        // Verify handler details
        MetarPatternHandler ceilingSecondSiteHandler = handlers.get(RegExprConst.CEILING_SECOND_SITE_PATTERN);
        assertThat(ceilingSecondSiteHandler).isNotNull();
        assertThat(ceilingSecondSiteHandler.handlerName()).isEqualTo("ceilingSecondSite");
        assertThat(ceilingSecondSiteHandler.canRepeat()).isFalse();
    }

    @Test
    void testGetRemarksHandlersCeilingSecondSiteAfterVariableCeiling() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();

        // Get indices of both patterns
        int variableCeilingIndex = handlers.getIndexOf(RegExprConst.VARIABLE_CEILING_PATTERN);
        int ceilingSecondSiteIndex = handlers.getIndexOf(RegExprConst.CEILING_SECOND_SITE_PATTERN);

        // Ceiling second site should come after variable ceiling
        assertThat(ceilingSecondSiteIndex)
                .as("Ceiling second site should be registered after variable ceiling")
                .isGreaterThan(variableCeilingIndex);

        // Both should come before unparsed catch-all
        int unparsedIndex = handlers.getIndexOf(RegExprConst.UNPARSED_PATTERN);
        assertThat(variableCeilingIndex)
                .as("Variable ceiling should come before unparsed remarks")
                .isLessThan(unparsedIndex);
        assertThat(ceilingSecondSiteIndex)
                .as("Ceiling second site should come before unparsed remarks")
                .isLessThan(unparsedIndex);
    }

    @Test
    void testGetRemarksHandlersHasObscurationPattern() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();

        // Should contain obscuration pattern
        assertThat(handlers)
                .containsKey(RegExprConst.OBSCURATION_PATTERN);

        // Verify handler details
        MetarPatternHandler obscurationHandler = handlers.get(RegExprConst.OBSCURATION_PATTERN);
        assertThat(obscurationHandler).isNotNull();
        assertThat(obscurationHandler.handlerName()).isEqualTo("obscurationLayers");
        assertThat(obscurationHandler.canRepeat()).isTrue();  // Should be repeating for multiple layers
    }

    @Test
    void testGetRemarksHandlersObscurationAfterCeilingSecondSite() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();

        // Get indices of both patterns
        int ceilingSecondSiteIndex = handlers.getIndexOf(RegExprConst.CEILING_SECOND_SITE_PATTERN);
        int obscurationIndex = handlers.getIndexOf(RegExprConst.OBSCURATION_PATTERN);

        // Obscuration should come after ceiling second site
        assertThat(obscurationIndex)
                .as("Obscuration should be registered after ceiling second site")
                .isGreaterThan(ceilingSecondSiteIndex);

        // Both should come before unparsed catch-all
        int unparsedIndex = handlers.getIndexOf(RegExprConst.UNPARSED_PATTERN);
        assertThat(ceilingSecondSiteIndex)
                .as("Ceiling second site should come before unparsed remarks")
                .isLessThan(unparsedIndex);
        assertThat(obscurationIndex)
                .as("Obscuration should come before unparsed remarks")
                .isLessThan(unparsedIndex);
    }

    @Test
    void testGetRemarksHandlersObscurationBeforeLightning() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();

        // Get indices of both patterns
        int obscurationIndex = handlers.getIndexOf(RegExprConst.OBSCURATION_PATTERN);
        int lightningIndex = handlers.getIndexOf(RegExprConst.LIGHTNING_PATTERN);

        // Obscuration should come before lightning
        assertThat(obscurationIndex)
                .as("Obscuration should be registered before lightning")
                .isLessThan(lightningIndex);
    }

    // ========== ADD TO MetarPatternRegistryTest.java ==========

// ADD THESE 3 TESTS to the test class:

    @Test
    @DisplayName("Should have CLOUD_OKTA_PATTERN in remarks handlers")
    void testGetRemarksHandlersHasCloudOktaPattern() {
        Map<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();

        // Verify CLOUD_OKTA_PATTERN is registered
        assertThat(handlers).containsKey(RegExprConst.CLOUD_OKTA_PATTERN);

        MetarPatternHandler handler = handlers.get(RegExprConst.CLOUD_OKTA_PATTERN);
        assertThat(handler).isNotNull();
        assertThat(handler.handlerName()).isEqualTo("cloudTypes");
        assertThat(handler.canRepeat()).isTrue();
    }

    @Test
    @DisplayName("Should have CLOUD_OKTA_PATTERN after OBSCURATION_PATTERN")
    void testGetRemarksHandlersCloudTypeAfterObscuration() {
        Map<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();

        // Get the insertion order of patterns
        List<Pattern> patterns = new ArrayList<>(handlers.keySet());

        int obscurationIndex = patterns.indexOf(RegExprConst.OBSCURATION_PATTERN);
        int cloudTypeIndex = patterns.indexOf(RegExprConst.CLOUD_OKTA_PATTERN);

        assertThat(obscurationIndex).isNotEqualTo(-1);
        assertThat(cloudTypeIndex)
                .isNotEqualTo(-1)
                .isGreaterThan(obscurationIndex);
    }

    @Test
    @DisplayName("Should have CLOUD_OKTA_PATTERN before LIGHTNING_PATTERN")
    void testGetRemarksHandlersCloudTypeBeforeLightning() {
        Map<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();

        // Get the insertion order of patterns
        List<Pattern> patterns = new ArrayList<>(handlers.keySet());

        int cloudTypeIndex = patterns.indexOf(RegExprConst.CLOUD_OKTA_PATTERN);
        int lightningIndex = patterns.indexOf(RegExprConst.LIGHTNING_PATTERN);

        assertThat(cloudTypeIndex).isNotEqualTo(-1);

        // If lightning pattern exists, cloud types should come before it
        if (lightningIndex != -1) {
            assertThat(cloudTypeIndex).isLessThan(lightningIndex);
        }
    }
}
