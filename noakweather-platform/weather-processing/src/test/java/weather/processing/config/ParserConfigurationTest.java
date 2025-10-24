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
package weather.processing.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ParserConfiguration.
 * Tests configuration loading, defaults, and property access.
 * 
 * @author bclasky1539
 *
 */
class ParserConfigurationTest {
    
    @Test
    @DisplayName("Should load with default configuration")
    void testDefaultConfiguration() {
        ParserConfiguration config = new ParserConfiguration();
        
        assertNotNull(config);
        assertFalse(config.useNewParser()); // Default is false
        assertTrue(config.isStrictMode()); // Default is true
        assertFalse(config.isCacheEnabled()); // Default is false
    }
    
    @Test
    @DisplayName("Should use legacy parser by default")
    void testUseLegacyParserByDefault() {
        ParserConfiguration config = new ParserConfiguration();
        
        assertFalse(config.useNewParser());
    }
    
    @Test
    @DisplayName("Should enable strict mode by default")
    void testStrictModeByDefault() {
        ParserConfiguration config = new ParserConfiguration();
        
        assertTrue(config.isStrictMode());
    }
    
    @Test
    @DisplayName("Should disable caching by default")
    void testCacheDisabledByDefault() {
        ParserConfiguration config = new ParserConfiguration();
        
        assertFalse(config.isCacheEnabled());
    }
    
    @Test
    @DisplayName("Should have default timeout of 30 seconds")
    void testDefaultTimeout() {
        ParserConfiguration config = new ParserConfiguration();
        
        assertEquals(30, config.getTimeoutSeconds());
    }
    
    @Test
    @DisplayName("Should have default log level INFO")
    void testDefaultLogLevel() {
        ParserConfiguration config = new ParserConfiguration();
        
        assertEquals("INFO", config.getLogLevel());
    }
    
    @Test
    @DisplayName("Should disable debug mode by default")
    void testDebugDisabledByDefault() {
        ParserConfiguration config = new ParserConfiguration();
        
        assertFalse(config.isDebugEnabled());
    }
    
    @Test
    @DisplayName("Should have default thread pool size of 10")
    void testDefaultThreadPoolSize() {
        ParserConfiguration config = new ParserConfiguration();
        
        assertEquals(10, config.getThreadPoolSize());
    }
    
    @Test
    @DisplayName("Should have default max batch size of 100")
    void testDefaultMaxBatchSize() {
        ParserConfiguration config = new ParserConfiguration();
        
        assertEquals(100, config.getMaxBatchSize());
    }
    
    @Test
    @DisplayName("Should create configuration with custom properties")
    void testCustomProperties() {
        Properties props = new Properties();
        props.setProperty("use.new.parser", "true");
        props.setProperty("parser.strict.mode", "false");
        
        ParserConfiguration config = new ParserConfiguration(props);
        
        assertTrue(config.useNewParser());
        assertFalse(config.isStrictMode());
    }
    
    @Test
    @DisplayName("Should override defaults with custom properties")
    void testCustomPropertiesOverrideDefaults() {
        Properties props = new Properties();
        props.setProperty("parser.timeout.seconds", "60");
        props.setProperty("parser.thread.pool.size", "20");
        
        ParserConfiguration config = new ParserConfiguration(props);
        
        assertEquals(60, config.getTimeoutSeconds());
        assertEquals(20, config.getThreadPoolSize());
    }
    
    @Test
    @DisplayName("Should keep defaults for properties not in custom set")
    void testPartialCustomProperties() {
        Properties props = new Properties();
        props.setProperty("use.new.parser", "true");
        // Don't set other properties
        
        ParserConfiguration config = new ParserConfiguration(props);
        
        assertTrue(config.useNewParser()); // Custom
        assertTrue(config.isStrictMode()); // Default
        assertFalse(config.isCacheEnabled()); // Default
    }
    
    @Test
    @DisplayName("Should get property by key")
    void testGetProperty() {
        Properties props = new Properties();
        props.setProperty("custom.key", "custom.value");
        
        ParserConfiguration config = new ParserConfiguration(props);
        
        assertEquals("custom.value", config.getProperty("custom.key"));
    }
    
    @Test
    @DisplayName("Should return null for nonexistent property")
    void testGetNonexistentProperty() {
        ParserConfiguration config = new ParserConfiguration();
        
        assertNull(config.getProperty("nonexistent.key"));
    }
    
    @Test
    @DisplayName("Should get property with default value")
    void testGetPropertyWithDefault() {
        ParserConfiguration config = new ParserConfiguration();
        
        String value = config.getProperty("nonexistent.key", "default.value");
        
        assertEquals("default.value", value);
    }
    
    @Test
    @DisplayName("Should not use default when property exists")
    void testGetPropertyIgnoresDefaultWhenExists() {
        Properties props = new Properties();
        props.setProperty("existing.key", "actual.value");
        
        ParserConfiguration config = new ParserConfiguration(props);
        String value = config.getProperty("existing.key", "default.value");
        
        assertEquals("actual.value", value);
    }
    
    @Test
    @DisplayName("Should set property in memory")
    void testSetProperty() {
        ParserConfiguration config = new ParserConfiguration();
        
        config.setProperty("test.key", "test.value");
        
        assertEquals("test.value", config.getProperty("test.key"));
    }
    
    @Test
    @DisplayName("Should override existing property when setting")
    void testSetPropertyOverride() {
        Properties props = new Properties();
        props.setProperty("key", "original");
        
        ParserConfiguration config = new ParserConfiguration(props);
        config.setProperty("key", "updated");
        
        assertEquals("updated", config.getProperty("key"));
    }
    
    @Test
    @DisplayName("Should get all properties as copy")
    void testGetAllProperties() {
        Properties props = new Properties();
        props.setProperty("key1", "value1");
        props.setProperty("key2", "value2");
        
        ParserConfiguration config = new ParserConfiguration(props);
        Properties allProps = config.getAllProperties();
        
        assertNotNull(allProps);
        assertEquals("value1", allProps.getProperty("key1"));
        assertEquals("value2", allProps.getProperty("key2"));
        assertTrue(allProps.size() >= 2); // At least our 2 + defaults
    }
    
    @Test
    @DisplayName("Should return copy of properties not reference")
    void testGetAllPropertiesReturnsCopy() {
        ParserConfiguration config = new ParserConfiguration();
        Properties props1 = config.getAllProperties();
        Properties props2 = config.getAllProperties();
        
        assertNotSame(props1, props2);
    }
    
    @Test
    @DisplayName("Should not affect config when modifying returned properties")
    void testGetAllPropertiesIsolation() {
        ParserConfiguration config = new ParserConfiguration();
        Properties props = config.getAllProperties();
        
        props.setProperty("use.new.parser", "true");
        
        // Original config should not be affected
        assertFalse(config.useNewParser());
    }
    
    @Test
    @DisplayName("Should have meaningful toString")
    void testToString() {
        ParserConfiguration config = new ParserConfiguration();
        String str = config.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("ParserConfiguration"));
        assertTrue(str.contains("useNewParser"));
        assertTrue(str.contains("strictMode"));
        assertTrue(str.contains("cacheEnabled"));
        assertTrue(str.contains("timeoutSeconds"));
    }
    
    @Test
    @DisplayName("Should reflect current values in toString")
    void testToStringReflectsValues() {
        Properties props = new Properties();
        props.setProperty("use.new.parser", "true");
        props.setProperty("parser.strict.mode", "false");
        
        ParserConfiguration config = new ParserConfiguration(props);
        String str = config.toString();
        
        assertTrue(str.contains("useNewParser=true"));
        assertTrue(str.contains("strictMode=false"));
    }
    
    @Test
    @DisplayName("Should parse boolean properties correctly")
    void testBooleanPropertyParsing() {
        Properties props = new Properties();
        props.setProperty("use.new.parser", "true");
        props.setProperty("parser.strict.mode", "false");
        props.setProperty("parser.cache.enabled", "TRUE"); // Different case
        props.setProperty("parser.debug.enabled", "False"); // Different case
        
        ParserConfiguration config = new ParserConfiguration(props);
        
        assertTrue(config.useNewParser());
        assertFalse(config.isStrictMode());
        assertTrue(config.isCacheEnabled());
        assertFalse(config.isDebugEnabled());
    }
    
    @Test
    @DisplayName("Should parse integer properties correctly")
    void testIntegerPropertyParsing() {
        Properties props = new Properties();
        props.setProperty("parser.timeout.seconds", "45");
        props.setProperty("parser.thread.pool.size", "15");
        props.setProperty("parser.max.batch.size", "200");
        
        ParserConfiguration config = new ParserConfiguration(props);
        
        assertEquals(45, config.getTimeoutSeconds());
        assertEquals(15, config.getThreadPoolSize());
        assertEquals(200, config.getMaxBatchSize());
    }
    
    @Test
    @DisplayName("Should handle missing parser.properties file gracefully")
    void testMissingPropertiesFile() {
        // The default constructor should work even if parser.properties is missing
        // It should fall back to defaults
        ParserConfiguration config = new ParserConfiguration();
        
        assertNotNull(config);
        assertFalse(config.useNewParser());
        assertTrue(config.isStrictMode());
    }
    
    @Test
    @DisplayName("Should support all log levels")
    void testLogLevels() {
        String[] logLevels = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR"};
        
        for (String level : logLevels) {
            Properties props = new Properties();
            props.setProperty("parser.log.level", level);
            
            ParserConfiguration config = new ParserConfiguration(props);
            assertEquals(level, config.getLogLevel());
        }
    }
    
    @Test
    @DisplayName("Should allow zero timeout")
    void testZeroTimeout() {
        Properties props = new Properties();
        props.setProperty("parser.timeout.seconds", "0");
        
        ParserConfiguration config = new ParserConfiguration(props);
        
        assertEquals(0, config.getTimeoutSeconds());
    }
    
    @Test
    @DisplayName("Should allow large batch sizes")
    void testLargeBatchSize() {
        Properties props = new Properties();
        props.setProperty("parser.max.batch.size", "10000");
        
        ParserConfiguration config = new ParserConfiguration(props);
        
        assertEquals(10000, config.getMaxBatchSize());
    }
}
