/*
 * noakweather(TM) is a Java library for parsing weather data
 * Copyright (C) 2025 bclasky1539
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package noakweather.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Weather Configuration Factory Test
 *
 * 
 * @author bclasky1539
 *
 */
public class WeatherConfigurationFactoryTest {
    
    @BeforeEach
    void setUp() {
        // Reset factory to clean state before each test
        WeatherConfigurationFactory.reset();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after each test
        WeatherConfigurationFactory.reset();
    }
    
    @Test
    void testGetInstanceReturnsNotNull() {
        WeatherConfigurationService instance = WeatherConfigurationFactory.getInstance();
        assertNotNull(instance);
    }
    
    @Test
    void testGetInstanceReturnsSameInstance() {
        // Test singleton behavior - should return same instance
        WeatherConfigurationService first = WeatherConfigurationFactory.getInstance();
        WeatherConfigurationService second = WeatherConfigurationFactory.getInstance();
        
        assertSame(first, second, "getInstance() should return the same instance");
    }
    
    @Test
    void testGetInstanceReturnsResourceBundleImplementation() {
        WeatherConfigurationService instance = WeatherConfigurationFactory.getInstance();
        assertTrue(instance instanceof ResourceBundleWeatherConfigurationService,
                "Default instance should be ResourceBundleWeatherConfigurationService");
    }
    
    @Test
    void testSetInstanceChangesReturnedInstance() {
        // Create a test configuration service
        TestWeatherConfigurationService testConfig = new TestWeatherConfigurationService();
        
        // Set it as the instance
        WeatherConfigurationFactory.setInstance(testConfig);
        
        // Verify getInstance() now returns our test instance
        WeatherConfigurationService instance = WeatherConfigurationFactory.getInstance();
        assertSame(testConfig, instance, "setInstance() should change the returned instance");
    }
    
    @Test
    void testResetClearsInstance() {
        // Get initial instance
        WeatherConfigurationService first = WeatherConfigurationFactory.getInstance();
        
        // Reset the factory
        WeatherConfigurationFactory.reset();
        
        // Get new instance - should be different from first
        WeatherConfigurationService second = WeatherConfigurationFactory.getInstance();
        
        assertNotSame(first, second, "reset() should clear the cached instance");
    }
    
    @Test
    void testResetFollowedByGetInstanceCreatesNewInstance() {
        // Set a test instance
        TestWeatherConfigurationService testConfig = new TestWeatherConfigurationService();
        WeatherConfigurationFactory.setInstance(testConfig);
        
        // Verify test instance is returned
        assertSame(testConfig, WeatherConfigurationFactory.getInstance());
        
        // Reset and get new instance
        WeatherConfigurationFactory.reset();
        WeatherConfigurationService newInstance = WeatherConfigurationFactory.getInstance();
        
        // Should be back to default implementation, not the test instance
        assertNotSame(testConfig, newInstance);
        assertTrue(newInstance instanceof ResourceBundleWeatherConfigurationService);
    }
    
    @Test
    void testPrivateConstructorThrowsException() {
        // Test that the private constructor prevents instantiation
        assertThrows(InvocationTargetException.class, () -> {
            Constructor<WeatherConfigurationFactory> constructor = 
                WeatherConfigurationFactory.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
    }
    
    @Test
    void testPrivateConstructorThrowsCorrectException() throws Exception {
        // Test that the private constructor throws UnsupportedOperationException
        Constructor<WeatherConfigurationFactory> constructor = 
            WeatherConfigurationFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> {
            constructor.newInstance();
        });
        
        // Verify the underlying exception is UnsupportedOperationException
        Throwable cause = exception.getCause();
        assertTrue(cause instanceof UnsupportedOperationException);
        assertEquals("This is a utility class and cannot be instantiated", cause.getMessage());
    }
    
    @Test
    void testLazyInitialization() {
        // Ensure factory starts clean
        WeatherConfigurationFactory.reset();
        
        // First call should create new instance (tests the if condition)
        WeatherConfigurationService first = WeatherConfigurationFactory.getInstance();
        assertNotNull(first);
        
        // Second call should return same instance (tests the cached path)
        WeatherConfigurationService second = WeatherConfigurationFactory.getInstance();
        assertSame(first, second);
    }
    
    @Test
    void testSetInstanceWithNull() {
        // Set instance to null
        WeatherConfigurationFactory.setInstance(null);
        
        // Next call to getInstance should create new instance
        WeatherConfigurationService instance = WeatherConfigurationFactory.getInstance();
        assertNotNull(instance);
        assertTrue(instance instanceof ResourceBundleWeatherConfigurationService);
    }
    
    @Test
    void testMultipleResetCalls() {
        // Multiple reset calls should be safe
        WeatherConfigurationFactory.reset();
        WeatherConfigurationFactory.reset();
        WeatherConfigurationFactory.reset();
        
        // Should still work normally
        WeatherConfigurationService instance = WeatherConfigurationFactory.getInstance();
        assertNotNull(instance);
    }
    
    @Test
    void testFactoryStateAfterSetAndReset() {
        // Test complete cycle: get -> set -> reset -> get
        
        // 1. Get default instance
        WeatherConfigurationService defaultInstance = WeatherConfigurationFactory.getInstance();
        assertTrue(defaultInstance instanceof ResourceBundleWeatherConfigurationService);
        
        // 2. Set custom instance
        TestWeatherConfigurationService customInstance = new TestWeatherConfigurationService();
        WeatherConfigurationFactory.setInstance(customInstance);
        assertSame(customInstance, WeatherConfigurationFactory.getInstance());
        
        // 3. Reset
        WeatherConfigurationFactory.reset();
        
        // 4. Get new default instance
        WeatherConfigurationService newDefaultInstance = WeatherConfigurationFactory.getInstance();
        assertTrue(newDefaultInstance instanceof ResourceBundleWeatherConfigurationService);
        assertNotSame(defaultInstance, newDefaultInstance); // Should be new instance
        assertNotSame(customInstance, newDefaultInstance); // Should not be custom instance
    }
}
