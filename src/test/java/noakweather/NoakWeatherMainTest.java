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

import noakweather.config.WeatherConfigurationFactory;
import noakweather.config.WeatherConfigurationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for NoakWeatherMain.
 * 
 * This test suite validates the main application entry point and its core functionality,
 * including command parsing, weather service integration, and error handling.
 * 
 * @author bclasky1539
 * 
 */
class NoakWeatherMainTest {
    
    private TestWeatherConfigurationService testConfig;
    
    @BeforeEach
    void setUp() {
        // Create a test configuration with the necessary keys
        testConfig = new TestWeatherConfigurationService()
            .withRawConfig("MISC_WEATHER_DATA", "Weather Data:")
            .withRawConfig("MISC_STATION", "Station:")
            .withRawConfig("MISC_METAR_URL", "https://tgftp.nws.noaa.gov/data/observations/metar/stations/")
            .withRawConfig("MISC_METAR_EXT", ".TXT")
            .withRawConfig("MISC_TAF_URL", "https://tgftp.nws.noaa.gov/data/forecasts/taf/stations/")
            .withRawConfig("MISC_TAF_EXT", ".TXT")
            .withExceptionMessage("NULL_POINTER_EXCEPTION", "Null Pointer exception:")
            .withExceptionMessage("WEATHER_GET_METAR", "Weather getMetar:")
            .withExceptionMessage("WEATHER_GET_TAF", "Weather getTaf:")
            .withLogMessage("MSG_MET_PARM", "Metar: java -jar noakweather.jar m XXXX y|n d|i|w where XXXX is the station")
            .withLogMessage("MSG_TAF_PARM", "TAF: java -jar noakweather.jar t XXXX y|n d|i|w where XXXX is the station");
        
        // Set the test configuration for the factory
        WeatherConfigurationFactory.setInstance(testConfig);
    }
    
    @AfterEach
    void tearDown() {
        // Reset configuration factory to default after each test
        WeatherConfigurationFactory.reset();
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
        // The app name should be a meaningful name (could be "noakweather-java" from pom.xml or "NoakWeather" as fallback)
        assertTrue(appName.toLowerCase().contains("noak") || appName.toLowerCase().contains("weather"), 
                   "App name should contain 'noak' or 'weather', but was: " + appName);
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
    void testGetWeatherConfig() {
        WeatherConfigurationService config = NoakWeatherMain.getWeatherConfig();
        assertNotNull(config);
        // Should be able to access configuration values
        assertDoesNotThrow(() -> config.getRawString("MISC_WEATHER_DATA"));
    }
    
    @Test
    void testDisplayVersion() {
        // This method logs the version info, so we test it doesn't throw exceptions
        assertDoesNotThrow(NoakWeatherMain::displayVersion);
    }
    
    @Test
    void testDisplayHelp() {
        // This method logs help information, so we test it doesn't throw exceptions
        assertDoesNotThrow(NoakWeatherMain::displayHelp);
    }
    
    @Test
    void testMainWithNoArguments() {
        String[] args = {};
        // Should call displayHelp() and return gracefully
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        });
    }
    
    // NEW TESTS for METAR functionality
    @ParameterizedTest
    @ValueSource(strings = {"metar", "-m"})
    void testMainWithMetarCommand(String metarCommand) {
        String[] args = {metarCommand, "KJFK"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        }, "METAR command should work: " + metarCommand);
    }
    
    // NEW TESTS for TAF functionality
    @ParameterizedTest
    @ValueSource(strings = {"taf", "-t"})
    void testMainWithTafCommand(String tafCommand) {
        String[] args = {tafCommand, "KJFK"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        }, "TAF command should work: " + tafCommand);
    }
    
    @Test
    void testCaseInsensitiveCommands() {
        // Test that commands are case-insensitive
        String[] upperCaseArgs = {"HELP"};
        String[] mixedCaseArgs = {"HeLp"};
        String[] metarMixedCase = {"MeTaR", "KJFK"};
        String[] tafMixedCase = {"TaF", "KJFK"};
        
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(upperCaseArgs);
        });
        
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(mixedCaseArgs);
        });
        
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(metarMixedCase);
        });
        
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(tafMixedCase);
        });
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
        String[] normalArgs = {"version"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(normalArgs);
        });
    }
    
    @Test
    void testMultipleCommandExecution() {
        // Test running multiple commands to ensure no state issues
        String[][] multipleCommands = {
            {"help"},
            {"version"}, 
            {"metar", "KJFK"},
            {"taf", "KJFK"},
            {"help"}
        };
        
        for (String[] command : multipleCommands) {
            assertDoesNotThrow(() -> {
                NoakWeatherMain.main(command);
            }, "Command should execute without issues: " + String.join(" ", command));
        }
    }
    
    // NEW TESTS for configuration integration
    @Test
    void testConfigurationIntegration() {
        // Test that configuration is properly loaded and accessible
        WeatherConfigurationService config = NoakWeatherMain.getWeatherConfig();
        assertNotNull(config);
        
        // Test accessing configuration values
        assertDoesNotThrow(() -> {
            String weatherData = config.getRawString("MISC_WEATHER_DATA");
            assertNotNull(weatherData);
        });
    }
    
    @Test
    void testShortFlagCommands() {
        // Test the new short flag commands
        String[] metarShort = {"-m", "KJFK"};
        String[] tafShort = {"-t", "KJFK"};
        
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(metarShort);
        });
        
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(tafShort);
        });
    }
    
    // Parameterized tests for better coverage and less repetition
    @ParameterizedTest
    @ValueSource(strings = {"metar", "taf"})
    void testMainWithCommandNoStation(String command) {
        String[] args = {command};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        }, "Command should handle missing station gracefully: " + command);
    }
    
    @ParameterizedTest
    @MethodSource("provideCommandsAndInvalidStations")
    void testMainWithCommandInvalidStation(String command, String invalidStation) {
        String[] args = {command, invalidStation};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        }, "Command should handle invalid station gracefully: " + command + " " + invalidStation);
    }
    
    // Method source for invalid station combinations
    private static Stream<Arguments> provideCommandsAndInvalidStations() {
        String[] commands = {"metar", "taf"};
        String[] invalidStations = {"12", "TOOLONG", "K1FK", "", "INVALID123"};
        
        return Stream.of(commands)
            .flatMap(command -> Stream.of(invalidStations)
                .map(station -> Arguments.of(command, station)));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"metar", "taf", "-m", "-t"})
    void testMainWithCommandValidStation(String command) {
        String[] args = {command, "KJFK"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        }, "Command should work with valid station: " + command + " KJFK");
    }

    @Test
    void testMainWithUnknownCommand() {
        String[] args = {"unknown"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        });
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"help", "-h", "--help", "HELP"})
    void testMainWithHelpCommands(String helpCommand) {
        String[] args = {helpCommand};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        }, "Help command should work: " + helpCommand);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"version", "-v", "--version", "VERSION"})
    void testMainWithVersionCommands(String versionCommand) {
        String[] args = {versionCommand};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        }, "Version command should work: " + versionCommand);
    }
    
    // Test command line argument parsing with options
    @Test
    void testMainWithPrintOption() {
        String[] args = {"metar", "KJFK", "-p", "y"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        });
        
        String[] argsNo = {"metar", "KJFK", "-p", "n"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(argsNo);
        });
    }
    
    @Test
    void testMainWithLogLevelOption() {
        String[] debugArgs = {"metar", "KJFK", "-l", "debug"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(debugArgs);
        });
        
        String[] infoArgs = {"taf", "KJFK", "-l", "info"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(infoArgs);
        });
        
        String[] warnArgs = {"metar", "KJFK", "-l", "warn"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(warnArgs);
        });
        
        String[] errorArgs = {"taf", "KJFK", "-l", "error"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(errorArgs);
        });
    }
    
    @Test
    void testMainWithCombinedOptions() {
        String[] args = {"metar", "KJFK", "-p", "y", "-l", "debug"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        });
    }
    
    @Test
    void testMainWithOptionsInDifferentOrder() {
        // Test that options can come before or after the command
        String[] args1 = {"-l", "debug", "metar", "KJFK", "-p", "y"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args1);
        });
        
        String[] args2 = {"metar", "-p", "y", "KJFK", "-l", "info"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args2);
        });
    }
    
    @Test
    void testMainWithInvalidLogLevel() {
        String[] args = {"metar", "KJFK", "-l", "invalid"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(args);
        });
    }
    
    @Test
    void testMainWithPartialOptions() {
        // Test with flag but no value
        String[] argsP = {"metar", "KJFK", "-p"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(argsP);
        });
        
        String[] argsL = {"metar", "KJFK", "-l"};
        assertDoesNotThrow(() -> {
            NoakWeatherMain.main(argsL);
        });
    }
}
