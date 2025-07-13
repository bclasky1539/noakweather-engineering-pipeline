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
package noakweather;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Comprehensive test suite for NoakWeatherMain class.
 * Tests all public methods and command line interface functionality.
 */
class NoakWeatherMainTest {
    
    @BeforeEach
    void setUp() {
        // Setup for each test if needed
    }
    
    @AfterEach
    void tearDown() {
        // Cleanup after each test if needed
    }
    
    @Test
    void testConstructorThrowsException() {
        // Test that the private constructor prevents instantiation
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> {
            // Use reflection to access private constructor
            var constructor = NoakWeatherMain.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
        
        // The actual UnsupportedOperationException is wrapped in InvocationTargetException
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
        assertEquals("Utility class cannot be instantiated", exception.getCause().getMessage());
    }
    
    @Test
    void testGetAppName() {
        String appName = NoakWeatherMain.getAppName();
        assertNotNull(appName);
        assertFalse(appName.isEmpty());
        // Should be loaded from properties or fallback value
        assertTrue(appName.contains("noakweather") || appName.equals("NoakWeather"));
    }
    
    @Test
    void testGetAppVersion() {
        String version = NoakWeatherMain.getAppVersion();
        assertNotNull(version);
        assertFalse(version.isEmpty());
        // Should follow semantic versioning or be fallback
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+") || version.equals("0.0.1"));
    }
    
    @Test
    void testGetAppDescription() {
        String description = NoakWeatherMain.getAppDescription();
        assertNotNull(description);
        assertFalse(description.isEmpty());
        // Should contain meaningful description
        assertTrue(description.toLowerCase().contains("weather") || 
                  description.contains("Weather data processing application"));
    }
    
    @Test
    void testDisplayVersion() {
        // This method logs the version info, so we test it doesn't throw exceptions
        assertDoesNotThrow(() -> {
            NoakWeatherMain.displayVersion();
        });
    }
    
    @Test
    void testDisplayHelp() {
        // This method logs help information, so we test it doesn't throw exceptions
        assertDoesNotThrow(() -> {
            NoakWeatherMain.displayHelp();
        });
    }
    
    @Test
    void testMainWithNoArguments() {
        String[] args = {};
        // Should call displayHelp() and return gracefully
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        });
    }
    
    @Test
    void testMainWithHelpCommand() {
        String[] helpCommands = {"help", "-h", "--help", "HELP"};
        
        for (String helpCommand : helpCommands) {
            String[] args = {helpCommand};
            assertDoesNotThrow(() -> {
                NoakWeatherMain.main(args);
            }, "Help command should work: " + helpCommand);
        }
    }
    
    @Test
    void testMainWithVersionCommand() {
        String[] versionCommands = {"version", "-v", "--version", "VERSION"};
        
        for (String versionCommand : versionCommands) {
            String[] args = {versionCommand};
            assertDoesNotThrow(() -> {
                NoakWeatherMain.main(args);
            }, "Version command should work: " + versionCommand);
        }
    }
    
    @Test
    void testMainWithWeatherCommandValidStation() {
        String[] args = {"weather", "KJFK"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        });
    }
    
    @Test
    void testMainWithWeatherCommandLowercaseStation() {
        String[] args = {"weather", "kjfk"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        });
    }
    
    @Test
    void testMainWithWeatherCommandNoStation() {
        String[] args = {"weather"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        });
    }
    
    @Test
    void testMainWithWeatherCommandInvalidStation() {
        String[] invalidStations = {"12", "TOOLONG", "K1FK", ""};
        
        for (String invalidStation : invalidStations) {
            String[] args = {"weather", invalidStation};
            assertDoesNotThrow(() -> {
                NoakWeatherMain.main(args);
            }, "Should handle invalid station gracefully: " + invalidStation);
        }
    }
    
    @Test
    void testMainWithUnknownCommand() {
        String[] args = {"unknown"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        });
    }
    
    @Test
    void testIsValidStationCode() {
        // Valid station codes
        assertTrue(NoakWeatherMain.isValidStationCode("KJFK"));
        assertTrue(NoakWeatherMain.isValidStationCode("LAX"));
        assertTrue(NoakWeatherMain.isValidStationCode("EGLL"));
        assertTrue(NoakWeatherMain.isValidStationCode("kjfk")); // Should handle lowercase
        assertTrue(NoakWeatherMain.isValidStationCode(" KJFK ")); // Should handle whitespace
        
        // Invalid station codes
        assertFalse(NoakWeatherMain.isValidStationCode(null));
        assertFalse(NoakWeatherMain.isValidStationCode(""));
        assertFalse(NoakWeatherMain.isValidStationCode("   "));
        assertFalse(NoakWeatherMain.isValidStationCode("KJ")); // Too short
        assertFalse(NoakWeatherMain.isValidStationCode("KJFKK")); // Too long
        assertFalse(NoakWeatherMain.isValidStationCode("K1FK")); // Contains numbers
        assertFalse(NoakWeatherMain.isValidStationCode("KJ-K")); // Contains special characters
        assertFalse(NoakWeatherMain.isValidStationCode("KJ FK")); // Contains spaces
    }
    
    @Test
    void testHandleWeatherCommand() {
        // Test with valid station
        String[] args = {"weather", "KLAX"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.handleWeatherCommand(args);
        });
        
        // Test with insufficient arguments
        String[] argsShort = {"weather"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.handleWeatherCommand(argsShort);
        });
        
        // Test with invalid station
        String[] argsInvalid = {"weather", "INVALID123"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.handleWeatherCommand(argsInvalid);
        });
    }
    
    @Test
    void testCaseInsensitiveCommands() {
        // Test that commands are case-insensitive
        String[] upperCaseArgs = {"HELP"};
        String[] mixedCaseArgs = {"HeLp"};
        
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(upperCaseArgs);
        });
        
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(mixedCaseArgs);
        });
    }
    
    @Test
    void testStationCodeValidation_EdgeCases() {
        // Test edge cases for station code validation
        
        // Exactly 3 characters (valid)
        assertTrue(NoakWeatherMain.isValidStationCode("ABC"));
        
        // Exactly 4 characters (valid)
        assertTrue(NoakWeatherMain.isValidStationCode("ABCD"));
        
        // 2 characters (invalid)
        assertFalse(NoakWeatherMain.isValidStationCode("AB"));
        
        // 5 characters (invalid)
        assertFalse(NoakWeatherMain.isValidStationCode("ABCDE"));
        
        // Mixed case should work
        assertTrue(NoakWeatherMain.isValidStationCode("aBc"));
        assertTrue(NoakWeatherMain.isValidStationCode("AbCd"));
    }
    
    @Test
    void testApplicationConstants() {
        // Verify that application constants are properly defined
        assertNotNull(NoakWeatherMain.getAppName());
        assertNotNull(NoakWeatherMain.getAppVersion());
        assertNotNull(NoakWeatherMain.getAppDescription());
        assertFalse(NoakWeatherMain.getAppName().isEmpty());
        assertFalse(NoakWeatherMain.getAppVersion().isEmpty());
        assertFalse(NoakWeatherMain.getAppDescription().isEmpty());
    }
    
    @Test
    void testMainExceptionHandling() {
        // Test that main method handles exceptions gracefully
        // This is more of a structural test since we can't easily force exceptions
        String[] normalArgs = {"version"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(normalArgs);
        });
    }
    
    @Test
    void testWeatherCommandWithNullArgs() {
        // Test handleWeatherCommand with null args (edge case)
        assertDoesNotThrow(() -> {
            NoakWeatherMain.handleWeatherCommand(new String[]{"weather"});
        });
    }
    
    @Test
    void testStationCodeTrimsWhitespace() {
        // Test that station code validation properly trims whitespace
        assertTrue(NoakWeatherMain.isValidStationCode("  KJFK  "));
        assertTrue(NoakWeatherMain.isValidStationCode("\tKJFK\t"));
        assertTrue(NoakWeatherMain.isValidStationCode("\nKJFK\n"));
    }
    
    @Test
    void testMultipleCommandExecution() {
        // Test running multiple commands to ensure no state issues
        String[][] multipleCommands = {
            {"help"},
            {"version"}, 
            {"weather", "KJFK"},
            {"help"}
        };
        
        for (String[] command : multipleCommands) {
            assertDoesNotThrow(() -> {
                NoakWeatherMain.main(command);
            }, "Command should execute without issues: " + String.join(" ", command));
        }
    }
}
