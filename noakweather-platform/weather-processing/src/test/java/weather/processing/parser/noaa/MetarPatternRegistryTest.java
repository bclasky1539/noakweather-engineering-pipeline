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
import weather.utils.IndexedLinkedHashMap;
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
        
        assertThat(handlers).isNotNull();
        assertThat(handlers).isInstanceOf(IndexedLinkedHashMap.class);
    }
    
    @Test
    void testGetMainHandlersHasExpectedPatterns() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getMainHandlers();
        
        // Should contain key patterns
        assertThat(handlers).containsKey(RegExprConst.STATION_DAY_TIME_VALTMPER_PATTERN);
        assertThat(handlers).containsKey(RegExprConst.WIND_PATTERN);
        assertThat(handlers).containsKey(RegExprConst.VISIBILITY_PATTERN);
        assertThat(handlers).containsKey(RegExprConst.TEMP_DEWPOINT_PATTERN);
        assertThat(handlers).containsKey(RegExprConst.ALTIMETER_PATTERN);
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
        
        // First pattern should be month/day/year (optional)
        assertThat(firstPattern).isEqualTo(RegExprConst.MONTH_DAY_YEAR_PATTERN);
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
        
        assertThat(handlers).isNotNull();
        assertThat(handlers).isInstanceOf(IndexedLinkedHashMap.class);
    }
    
    @Test
    void testGetRemarksHandlersHasExpectedPatterns() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();
        
        // Should contain key remarks patterns
        assertThat(handlers).containsKey(RegExprConst.AUTO_PATTERN);
        assertThat(handlers).containsKey(RegExprConst.SEALVL_PRESS_PATTERN);
        assertThat(handlers).containsKey(RegExprConst.PEAK_WIND_PATTERN);
        assertThat(handlers).containsKey(RegExprConst.TEMP_1HR_PATTERN);
    }
    
    @Test
    void testGetRemarksHandlersIndexAccess() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getRemarksHandlers();
        
        // Test that we can access by index
        assertThat(handlers.size()).isGreaterThan(0);
        
        Pattern firstPattern = handlers.getKeyAtIndex(0);
        assertThat(firstPattern).isNotNull();
        
        // First remarks pattern should be AUTO (AO1/AO2)
        assertThat(firstPattern).isEqualTo(RegExprConst.AUTO_PATTERN);
    }
    
    @Test
    void testGetGroupHandlersReturnsIndexedLinkedHashMap() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getGroupHandlers();
        
        assertThat(handlers).isNotNull();
        assertThat(handlers).isInstanceOf(IndexedLinkedHashMap.class);
    }
    
    @Test
    void testGetGroupHandlersHasExpectedPatterns() {
        IndexedLinkedHashMap<Pattern, MetarPatternHandler> handlers = registry.getGroupHandlers();
        
        // Should contain TAF group patterns
        assertThat(handlers).containsKey(RegExprConst.TAF_STR_PATTERN);
        assertThat(handlers).containsKey(RegExprConst.GROUP_BECMG_TEMPO_PROB_PATTERN);
        assertThat(handlers).containsKey(RegExprConst.GROUP_FM_PATTERN);
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
            assertThat(handlers.get(pattern)).isEqualTo(handler);
        }
    }
}
