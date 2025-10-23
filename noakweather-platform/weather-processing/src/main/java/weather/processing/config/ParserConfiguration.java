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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration for parser features and flags.
 * Enables gradual migration from legacy to new parser.
 * 
 * Design Pattern: Configuration/Settings Pattern
 * 
 * Benefits:
 * - Feature flags (toggle new vs legacy parser)
 * - A/B testing (run both parsers and compare)
 * - Gradual rollout (start with small percentage of traffic)
 * - Quick rollback (just change config, no code deployment)
 * 
 * @author bclasky1539
 *
 */
public class ParserConfiguration {
    
    private static final String CONFIG_FILE = "parser.properties";
    private static final String FALSE = "false";
    private static final String TRUE = "true";
    
    private final Properties properties;
    
    /**
     * Create configuration by loading from parser.properties file.
     * Falls back to defaults if file not found.
     */
    public ParserConfiguration() {
        this.properties = loadProperties();
    }
    
    /**
     * Create configuration with custom properties.
     * Useful for testing.
     * 
     * @param customProperties Custom property values
     */
    public ParserConfiguration(Properties customProperties) {
        this.properties = new Properties();
        loadDefaults(this.properties);
        this.properties.putAll(customProperties);
    }
    
    /**
     * Load properties from file, with fallback to defaults.
     */
    private Properties loadProperties() {
        Properties props = new Properties();
        
        // First, load defaults
        loadDefaults(props);
        
        // Try to load user configuration (overrides defaults)
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                Properties userProps = new Properties();
                userProps.load(input);
                props.putAll(userProps);
            }
        } catch (IOException e) {
            // Silently fall back to defaults if file not found
            // Logging will be added in a future iteration
        }
        
        return props;
    }
    
    /**
     * Load default property values.
     */
    private void loadDefaults(Properties props) {
        // Feature flags - START WITH LEGACY (false)
        props.setProperty("use.new.parser", FALSE);
        
        // Parser behavior
        props.setProperty("parser.strict.mode", TRUE);
        props.setProperty("parser.cache.enabled", FALSE);
        props.setProperty("parser.timeout.seconds", "30");
        
        // Logging
        props.setProperty("parser.log.level", "INFO");
        props.setProperty("parser.debug.enabled", FALSE);
        
        // Performance
        props.setProperty("parser.thread.pool.size", "10");
        props.setProperty("parser.max.batch.size", "100");
    }
    
    // === Feature Flags ===
    
    /**
     * Should we use the new platform parser instead of legacy?
     * 
     * Day 3: Should be FALSE (use legacy)
     * Day 8: Will be changed to TRUE (use platform)
     * 
     * @return true to use new parser, false to use legacy
     */
    public boolean useNewParser() {
        return Boolean.parseBoolean(properties.getProperty("use.new.parser", FALSE));
    }
    
    // === Parser Behavior ===
    
    /**
     * Is strict mode enabled?
     * Strict: fail on any ambiguity
     * Lenient: attempt to parse even malformed data
     * 
     * @return true for strict mode
     */
    public boolean isStrictMode() {
        return Boolean.parseBoolean(properties.getProperty("parser.strict.mode", TRUE));
    }
    
    /**
     * Is caching enabled for parsed results?
     * 
     * @return true if caching should be used
     */
    public boolean isCacheEnabled() {
        return Boolean.parseBoolean(properties.getProperty("parser.cache.enabled", FALSE));
    }
    
    /**
     * Get parser timeout in seconds.
     * 
     * @return Timeout duration in seconds
     */
    public int getTimeoutSeconds() {
        return Integer.parseInt(properties.getProperty("parser.timeout.seconds", "30"));
    }
    
    // === Logging ===
    
    /**
     * Get the logging level.
     * 
     * @return Log level (TRACE, DEBUG, INFO, WARN, ERROR)
     */
    public String getLogLevel() {
        return properties.getProperty("parser.log.level", "INFO");
    }
    
    /**
     * Is debug mode enabled?
     * 
     * @return true if debug mode is on
     */
    public boolean isDebugEnabled() {
        return Boolean.parseBoolean(properties.getProperty("parser.debug.enabled", FALSE));
    }
    
    // === Performance ===
    
    /**
     * Get thread pool size for parallel parsing.
     * 
     * @return Number of threads
     */
    public int getThreadPoolSize() {
        return Integer.parseInt(properties.getProperty("parser.thread.pool.size", "10"));
    }
    
    /**
     * Get maximum batch size for bulk parsing.
     * 
     * @return Maximum items per batch
     */
    public int getMaxBatchSize() {
        return Integer.parseInt(properties.getProperty("parser.max.batch.size", "100"));
    }
    
    // === Generic Access ===
    
    /**
     * Get any property value.
     * 
     * @param key Property key
     * @return Property value or null
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Get property with default.
     * 
     * @param key Property key
     * @param defaultValue Default if not found
     * @return Property value or default
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Set a property value (in-memory only).
     * 
     * @param key Property key
     * @param value Property value
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
    
    /**
     * Get all properties.
     * 
     * @return Copy of all properties
     */
    public Properties getAllProperties() {
        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }
    
    @Override
    public String toString() {
        return "ParserConfiguration{" +
               "useNewParser=" + useNewParser() +
               ", strictMode=" + isStrictMode() +
               ", cacheEnabled=" + isCacheEnabled() +
               ", timeoutSeconds=" + getTimeoutSeconds() +
               '}';
    }
}
