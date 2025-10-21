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
package weather.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProcessingLayer enum
 * 
 * @author bclasky1539
 *
 */
class ProcessingLayerTest {
    
    @Test
    @DisplayName("Should have correct display names")
    void testDisplayNames() {
        assertEquals("Speed Layer", ProcessingLayer.SPEED_LAYER.getDisplayName());
        assertEquals("Batch Layer", ProcessingLayer.BATCH_LAYER.getDisplayName());
        assertEquals("Serving Layer", ProcessingLayer.SERVING_LAYER.getDisplayName());
        assertEquals("Raw Layer", ProcessingLayer.RAW.getDisplayName());
    }
    
    @Test
    @DisplayName("Should have correct descriptions")
    void testDescriptions() {
        assertEquals("Real-time processing", ProcessingLayer.SPEED_LAYER.getDescription());
        assertEquals("Historical processing", ProcessingLayer.BATCH_LAYER.getDescription());
        assertEquals("Unified queries", ProcessingLayer.SERVING_LAYER.getDescription());
        assertEquals("Unprocessed data", ProcessingLayer.RAW.getDescription());
    }
    
    @Test
    @DisplayName("Should have correct retention hours")
    void testRetentionHours() {
        assertEquals(24, ProcessingLayer.SPEED_LAYER.getRetentionHours());
        assertEquals(Integer.MAX_VALUE, ProcessingLayer.BATCH_LAYER.getRetentionHours());
        assertEquals(Integer.MAX_VALUE, ProcessingLayer.SERVING_LAYER.getRetentionHours());
        assertEquals(0, ProcessingLayer.RAW.getRetentionHours());
    }
    
    @Test
    @DisplayName("Should correctly identify real-time layers")
    void testIsRealTime() {
        assertTrue(ProcessingLayer.SPEED_LAYER.isRealTime());
        
        assertFalse(ProcessingLayer.BATCH_LAYER.isRealTime());
        assertFalse(ProcessingLayer.SERVING_LAYER.isRealTime());
        assertFalse(ProcessingLayer.RAW.isRealTime());
    }
    
    @Test
    @DisplayName("Should correctly identify historical layers")
    void testIsHistorical() {
        assertTrue(ProcessingLayer.BATCH_LAYER.isHistorical());
        assertTrue(ProcessingLayer.SERVING_LAYER.isHistorical());
        
        assertFalse(ProcessingLayer.SPEED_LAYER.isHistorical());
        assertFalse(ProcessingLayer.RAW.isHistorical());
    }
    
    @Test
    @DisplayName("Should have all expected enum values")
    void testAllEnumValues() {
        ProcessingLayer[] layers = ProcessingLayer.values();
        
        assertEquals(4, layers.length, "Should have exactly 4 processing layers");
        
        // Verify all layers exist
        assertNotNull(ProcessingLayer.valueOf("SPEED_LAYER"));
        assertNotNull(ProcessingLayer.valueOf("BATCH_LAYER"));
        assertNotNull(ProcessingLayer.valueOf("SERVING_LAYER"));
        assertNotNull(ProcessingLayer.valueOf("RAW"));
    }
    
    @Test
    @DisplayName("Should follow Lambda Architecture principles")
    void testLambdaArchitecture() {
        // Speed layer: fast, recent data
        assertTrue(ProcessingLayer.SPEED_LAYER.isRealTime());
        assertFalse(ProcessingLayer.SPEED_LAYER.isHistorical());
        assertTrue(ProcessingLayer.SPEED_LAYER.getRetentionHours() < 100);
        
        // Batch layer: slow, all historical data
        assertFalse(ProcessingLayer.BATCH_LAYER.isRealTime());
        assertTrue(ProcessingLayer.BATCH_LAYER.isHistorical());
        
        // Serving layer: merged view
        assertFalse(ProcessingLayer.SERVING_LAYER.isRealTime());
        assertTrue(ProcessingLayer.SERVING_LAYER.isHistorical());
    }
}
