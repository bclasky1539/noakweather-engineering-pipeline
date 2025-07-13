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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main entry point for the NoakWeather application.
 * Provides weather data processing functionality with comprehensive logging.
 * 
 * @author NoakWeather Team
 * @version 0.0.1
 * @since 2025-01-01
 */
public class NoakWeatherMain {
    
    private static final Logger LOGGER = LogManager.getLogger(NoakWeatherMain.class);
    
    // Application configuration constants - loaded from properties
    private static final String APP_NAME;
    private static final String APP_VERSION;
    private static final String APP_DESCRIPTION;
    
    // Static block to load application properties from pom.xml
    static {
        Properties props = new Properties();
        String name = "NoakWeather";  // fallback
        String version = "0.0.1";     // fallback
        String description = "Weather data processing application"; // fallback
        
        try (InputStream input = NoakWeatherMain.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                props.load(input);
                name = props.getProperty("app.name", name);
                version = props.getProperty("app.version", version);
                description = props.getProperty("app.description", description);
                LOGGER.debug("Application properties loaded successfully");
            } else {
                LOGGER.warn("application.properties not found, using fallback values");
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load application.properties, using fallback values", e);
        }
        
        APP_NAME = name;
        APP_VERSION = version;
        APP_DESCRIPTION = description;
    }

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private NoakWeatherMain() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Main entry point for the application.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        LOGGER.info("Starting {} version {}", APP_NAME, APP_VERSION);
        
        try {
            // Process command line arguments
            if (args.length == 0) {
                displayHelp();
                return;
            }
            
            // Parse and execute commands
            String command = args[0].toLowerCase();
            
            switch (command) {
                case "help":
                case "-h":
                case "--help":
                    displayHelp();
                    break;
                case "version":
                case "-v":
                case "--version":
                    displayVersion();
                    break;
                case "weather":
                    handleWeatherCommand(args);
                    break;
                default:
                    LOGGER.warn("Unknown command: {}", command);
                    displayHelp();
                    return;
            }
            
        } catch (Exception e) {
            LOGGER.error("Application error occurred", e);
            return;
        }
        
        LOGGER.info("Application completed successfully");
    }
    
    /**
     * Displays help information to the user.
     */
    public static void displayHelp() {
        String helpText = String.format("%s v%s - Weather Data Processing Application%n%n" +
                "Usage: java -jar noakweather.jar [COMMAND] [OPTIONS]%n%n" +
                "Commands:%n" +
                "  help, -h, --help      Show this help message%n" +
                "  version, -v, --version Show version information%n" +
                "  weather [station]     Get weather data for station%n%n" +
                "Examples:%n" +
                "  java -jar noakweather.jar help%n" +
                "  java -jar noakweather.jar version%n" +
                "  java -jar noakweather.jar weather KJFK%n%n" +
                "For more information, visit: https://github.com/bclasky1539/noakweather-java%n",
                APP_NAME, APP_VERSION);
        
        LOGGER.debug("Help information displayed: {}", helpText);
    }
    
    /**
     * Displays version information to the user.
     */
    public static void displayVersion() {
        String versionInfo = String.format("%s version %s", APP_NAME, APP_VERSION);
        LOGGER.debug("Version information displayed: {}", versionInfo);
    }
    
    /**
     * Handles weather-related commands.
     * 
     * @param args Command line arguments including the weather command
     */
    public static void handleWeatherCommand(String[] args) {
        if (args.length < 2) {
            LOGGER.warn("Weather command requires station parameter");
            LOGGER.error("Error: Weather command requires a station code");
            LOGGER.error("Usage: weather [station]");
            LOGGER.error("Example: weather KJFK");
            return;
        }
        
        String station = args[1].toUpperCase();
        LOGGER.info("Processing weather request for station: {}", station);
        
        // Validate station code format (basic validation)
        if (!isValidStationCode(station)) {
            LOGGER.warn("Invalid station code format: {}", station);
            LOGGER.error("Error: Invalid station code format: " + station);
            LOGGER.error("Station codes should be 3-4 characters (e.g., KJFK, LAX)");
            return;
        }
        
        LOGGER.info("Weather data for station: " + station);
        LOGGER.info("Status: Ready to fetch weather data");
        LOGGER.info("Note: Weather fetching functionality will be implemented in future versions");
        
        LOGGER.info("Weather request processed for station: {}", station);
    }
    
    /**
     * Validates if a station code has the correct format.
     * 
     * @param stationCode The station code to validate
     * @return true if the station code format is valid, false otherwise
     */
    public static boolean isValidStationCode(String stationCode) {
        if (stationCode == null || stationCode.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = stationCode.trim().toUpperCase();
        
        // Station codes are typically 3-4 characters, all letters
        return trimmed.matches("[A-Z]{3,4}");
    }
    
    /**
     * Gets the application name.
     * 
     * @return The application name
     */
    public static String getAppName() {
        return APP_NAME;
    }
    
    /**
     * Gets the application version.
     * 
     * @return The application version
     */
    public static String getAppVersion() {
        return APP_VERSION;
    }
    
    /**
     * Gets the application description.
     * 
     * @return The application description
     */
    public static String getAppDescription() {
        return APP_DESCRIPTION;
    }
}
