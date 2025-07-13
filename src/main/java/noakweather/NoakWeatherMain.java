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

import noakweather.config.WeatherConfigurationService;
import noakweather.config.WeatherConfigurationFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Main entry point for the NoakWeather application.
 * Provides weather data processing functionality with comprehensive logging.
 * 
 * @author bclasky1539
 *
 */
public class NoakWeatherMain {
    
    private static final Logger LOGGER = LogManager.getLogger(NoakWeatherMain.class);
    
    // String constants
    private static final String ERROR_FORMAT_WITH_MESSAGE = "{} {}";
    private static final String STATION_FORMAT = "{} {}";
    private static final String INVALID_STATION_FORMAT_MSG = "Invalid station code format: {}";
    private static final String PROCESSING_REQUEST_MSG = "Processing {} request for station: {}";
    private static final String REQUEST_PROCESSED_MSG = "{} request processed for station: {}";

    // Weather configuration service for accessing weather-related constants
    private static final WeatherConfigurationService WEATHER_CONFIG;
    
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
        
        // Initialize weather configuration
        WEATHER_CONFIG = WeatherConfigurationFactory.getInstance();
        
        // Log weather configuration initialization
        LOGGER.debug("Weather configuration system initialized");
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
                case "metar":
                case "-m":
                    handleMetarCommand(args);
                    break;
                case "taf":
                case "-t":
                    handleTafCommand(args);
                    break;
                default:
                    LOGGER.warn("Unknown command: {}", command);
                    displayHelp();
                    return;
            }
            
        } catch (Exception e) {
            LOGGER.error("Application error occurred", e);
            String errorMsg = WEATHER_CONFIG.getExceptionMessage("NULL_POINTER_EXCEPTION");
            LOGGER.error(ERROR_FORMAT_WITH_MESSAGE, errorMsg, e.getMessage());
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
                "  help, -h, --help          Show this help message%n" +
                "  version, -v, --version    Show version information%n" +
                "  weather [station]         Get weather data for station%n" +
                "  metar, -m [station]       Get METAR data for station%n" +
                "  taf, -t [station]         Get TAF data for station%n%n" +
                "Examples:%n" +
                "  java -jar noakweather.jar help%n" +
                "  java -jar noakweather.jar version%n" +
                "  java -jar noakweather.jar weather KJFK%n" +
                "  java -jar noakweather.jar metar KJFK%n" +
                "  java -jar noakweather.jar taf KJFK%n%n" +
                "For more information, visit: https://github.com/bclasky1539/noakweather-java%n",
                APP_NAME, APP_VERSION);
        
        LOGGER.debug("Help information displayed: {}", helpText);
    }
    
    /**
     * Displays version information to the user.
     */
    public static void displayVersion() {
        String versionInfo = String.format("%s version %s",
                APP_NAME, APP_VERSION);
        LOGGER.info("Version information displayed: {}", versionInfo);
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
        LOGGER.info(PROCESSING_REQUEST_MSG, "weather", station);
        
        // Validate station code format (basic validation)
        if (!isValidStationCode(station)) {
            LOGGER.warn(INVALID_STATION_FORMAT_MSG, station);
            LOGGER.error(INVALID_STATION_FORMAT_MSG, station);
            LOGGER.error("Station codes should be 3-4 characters (e.g., KJFK, LAX)");
            return;
        }
        
        LOGGER.info("{}", WEATHER_CONFIG.getRawString("MISC_WEATHER_DATA"));
        logStationInfo(station);
        LOGGER.info("Status: Ready to fetch weather data");
        LOGGER.info("Note: Weather fetching functionality will be implemented in future versions");
        
        LOGGER.info(REQUEST_PROCESSED_MSG, "Weather", station);
    }
    
    /**
     * Handles METAR-specific commands.
     * 
     * @param args Command line arguments including the metar command
     */
    public static void handleMetarCommand(String[] args) {
        if (args.length < 2) {
            String logMsg = WEATHER_CONFIG.getLogMessage("MSG_MET_PARM");
            LOGGER.warn("METAR command requires station parameter");
            LOGGER.error("METAR command requires a station code");
            LOGGER.error("{}", logMsg);
            return;
        }
        
        String station = args[1].toUpperCase();
        LOGGER.info("Processing METAR request for station: {}", station);
        
        if (!isValidStationCode(station)) {
            LOGGER.warn(INVALID_STATION_FORMAT_MSG, station);
            LOGGER.error(INVALID_STATION_FORMAT_MSG, station);
            return;
        }
        
        try {
            String metarUrl = WEATHER_CONFIG.getRawString("MISC_METAR_URL") + 
                             station + 
                             WEATHER_CONFIG.getRawString("MISC_METAR_EXT");
            
            LOGGER.info("METAR URL: {}", metarUrl);
            logStationInfo(station);
            LOGGER.info("Note: METAR fetching functionality will be implemented in future versions");
            
            LOGGER.info("METAR request processed for station: {}", station);
            
        } catch (Exception e) {
            String errorMsg = WEATHER_CONFIG.getExceptionMessage("WEATHER_GET_METAR");
            LOGGER.error(ERROR_FORMAT_WITH_MESSAGE, errorMsg, e.getMessage());
        }
    }

    /**
     * Handles TAF-specific commands.
     * 
     * @param args Command line arguments including the taf command
     */
    public static void handleTafCommand(String[] args) {
        if (args.length < 2) {
            String logMsg = WEATHER_CONFIG.getLogMessage("MSG_TAF_PARM");
            LOGGER.warn("TAF command requires station parameter");
            LOGGER.error("TAF command requires a station code");
            LOGGER.error("{}", logMsg);
            return;
        }
        
        String station = args[1].toUpperCase();
        LOGGER.info("Processing TAF request for station: {}", station);
        
        if (!isValidStationCode(station)) {
            LOGGER.warn(INVALID_STATION_FORMAT_MSG, station);
            LOGGER.error(INVALID_STATION_FORMAT_MSG, station);
            return;
        }
        
        try {
            String tafUrl = WEATHER_CONFIG.getRawString("MISC_TAF_URL") + 
                           station + 
                           WEATHER_CONFIG.getRawString("MISC_TAF_EXT");
            
            LOGGER.info("TAF URL: {}", tafUrl);
            logStationInfo(station);
            LOGGER.info("Note: TAF fetching functionality will be implemented in future versions");
            
            LOGGER.info("TAF request processed for station: {}", station);
            
        } catch (Exception e) {
            String errorMsg = WEATHER_CONFIG.getExceptionMessage("WEATHER_GET_TAF");
            LOGGER.error(ERROR_FORMAT_WITH_MESSAGE, errorMsg, e.getMessage());
        }
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
    
    /**
     * Gets the weather configuration service (for testing purposes).
     * 
     * @return The weather configuration service
     */
    public static WeatherConfigurationService getWeatherConfig() {
        return WEATHER_CONFIG;
    }
    
    /**
     * Logs station information using the configured station label.
     * 
     * @param station The station code to log
     */
    private static void logStationInfo(String station) {
        LOGGER.info(STATION_FORMAT, WEATHER_CONFIG.getRawString("MISC_STATION"), station);
    }
}
