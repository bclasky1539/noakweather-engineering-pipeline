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

import noakweather.service.WeatherService;
import noakweather.service.WeatherServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import noakweather.config.WeatherConfigurationService;
import noakweather.config.WeatherConfigurationFactory;
import noakweather.service.WeatherServiceException;


/**
 * Main entry point for the NoakWeather application.
 * Provides weather data processing functionality with comprehensive logging.
 * 
 * This class serves as the application coordinator, handling command-line argument parsing,
 * application configuration, and delegating weather operations to the service layer.
 * 
 * @author bclasky1539
 *
 */
public class NoakWeatherMain {
    
    private static final Logger LOGGER = LogManager.getLogger(NoakWeatherMain.class);
    
    // String constants for logging and messaging
    private static final String PROCESSING_REQUEST_MSG = "Processing {} request for station: {}";
    private static final String REQUEST_PROCESSED_MSG = "{} request processed for station: {}";
    private static final String EXCEPTION_DETAILS_MSG= "Exception details: {}";
    private static final String LOG_LEVEL_INFO = "Set logging to INFO level";
    private static final String LOG_LEVEL_DEBUG = "Set logging to DEBUG level";
    private static final String LOG_LEVEL_WARN = "Set logging to WARN level";
    private static final String LOG_LEVEL_ERROR = "Set logging to ERROR level";

    // Weather request type constants
    private static final String WEATHER_TYPE_METAR = "METAR";
    private static final String WEATHER_TYPE_TAF = "TAF";

    // Configuration services
    private static final WeatherConfigurationService WEATHER_CONFIG;
    private static final WeatherService WEATHER_SERVICE;
    
    // Application metadata - loaded from properties
    private static final String APP_NAME;
    private static final String APP_VERSION;
    private static final String APP_DESCRIPTION;
    
    // Static initialization block - sets up the application's "foundation"
    static {
        // Load application properties (like reading the hotel's operational manual)
        Properties props = loadApplicationProperties();
        
        APP_NAME = props.getProperty("app.name", "NoakWeather");
        APP_VERSION = props.getProperty("app.version", "0.0.1");
        APP_DESCRIPTION = props.getProperty("app.description", "Weather data processing application");
        
        // Initialize configuration and services (setting up the hotel's infrastructure)
        WEATHER_CONFIG = WeatherConfigurationFactory.getInstance();
        WEATHER_SERVICE = new WeatherServiceImpl(WEATHER_CONFIG);
        
        LOGGER.debug("Application initialized: {} v{}", APP_NAME, APP_VERSION);
    }

    /**
     * Private constructor to prevent instantiation - this is a utility class.
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
            // Handle empty arguments
            if (args.length == 0) {
                displayHelp();
                return;
            }
            
            // Parse command line arguments into a structured format
            CommandOptions options = parseCommandLineArguments(args);
            
            // Configure logging based on user preferences
            configureLogging(options.getLogLevel());
            
            // Log parsed options for debugging
            logParsedOptions(options);
            
            // Execute the requested command
            executeCommand(options);
            
        } catch (Exception e) {
            handleApplicationError(e);
            return;
        }
        
        LOGGER.info("Application completed successfully");
    }

    /**
     * Loads application properties from the classpath.
     * 
     * @return Properties object with application configuration
     */
    private static Properties loadApplicationProperties() {
        Properties props = new Properties();
        
        try (InputStream input = NoakWeatherMain.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                props.load(input);
                LOGGER.debug("Application properties loaded successfully");
            } else {
                LOGGER.warn("application.properties not found, using fallback values");
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load application.properties, using fallback values", e);
        }
        
        return props;
    }

    /**
     * Parses command line arguments into a structured CommandOptions object.
     * 
     * This method acts like a translator, converting the user's command-line input
     * into a format the application can easily work with.
     * 
     * @param args Raw command line arguments
     * @return Parsed CommandOptions object
     */
    private static CommandOptions parseCommandLineArguments(String[] args) {
        CommandOptions options = new CommandOptions();
        
        int i = 0;
        while (i < args.length) {
            switch (args[i]) {
                case "-p":
                    if (i + 1 < args.length) {
                        options.setPrint("y".equalsIgnoreCase(args[i + 1]));
                        i += 2;
                    } else {
                        i++;
                    }
                    break;
                case "-l":
                    if (i + 1 < args.length) {
                        options.setLogLevel(args[i + 1]);
                        i += 2;
                    } else {
                        i++;
                    }
                    break;
                default:
                    // Collect positional arguments (commands and station codes)
                    options.addPositionalArg(args[i]);
                    i++;
                    break;
            }
        }
        
        return options;
    }

    /**
     * Logs the parsed command options for debugging purposes.
     * 
     * @param options The parsed command options
     */
    private static void logParsedOptions(CommandOptions options) {
        LOGGER.debug("Parsed options:");
        LOGGER.debug("  Command: {}", options.getCommand());
        LOGGER.debug("  Station: {}", options.getStationCode());
        LOGGER.debug("  Print: {}", options.shouldPrint());
        LOGGER.debug("  Log Level: {}", options.getLogLevel());
        LOGGER.debug("  All args: {}", options.getPositionalArgs());
    }

    /**
     * Executes the appropriate command based on parsed options.
     * 
     * This is like the hotel's concierge - it determines what service the guest needs
     * and directs them accordingly.
     * 
     * @param options Parsed command options
     */
    private static void executeCommand(CommandOptions options) {
        String command = options.getCommand();
        
        if (command == null) {
            LOGGER.warn("No command specified");
            displayHelp();
            return;
        }
        
        switch (command.toLowerCase()) {
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
            case "metar":
            case "-m":
                handleWeatherRequest(options, WEATHER_TYPE_METAR);
                break;
            case "taf":
            case "-t":
                handleWeatherRequest(options, WEATHER_TYPE_TAF);
                break;
            default:
                LOGGER.warn("Unknown command: {}", command);
                displayHelp();
                break;
        }
    }

    /**
     * Handles weather data requests by delegating to the service layer.
     * 
     * This method acts as a coordinator - it validates the request, delegates to the
     * appropriate service, and handles the response. It's like a maitre d' who takes
     * your order, sends it to the kitchen, and brings back your meal.
     * 
     * @param options Command options containing station code and preferences
     * @param requestType Type of weather request ("METAR" or "TAF")
     */
    private static void handleWeatherRequest(CommandOptions options, String requestType) {
        String stationCode = options.getStationCode();
        
        // Validate that a station code was provided
        if (stationCode == null || stationCode.trim().isEmpty()) {
            handleMissingStationCode(requestType);
            return;
        }
        
        String station = stationCode.toUpperCase().trim();
        LOGGER.info(PROCESSING_REQUEST_MSG, requestType, station);
        
        try {
            // Delegate to the service layer based on request type
            String weatherData;
            if (null == requestType) {
                throw new IllegalArgumentException("Unknown request type: " + requestType);
            } else switch (requestType) {
                case WEATHER_TYPE_METAR:
                    weatherData = WEATHER_SERVICE.getMetarData(station);
                    break;
                case WEATHER_TYPE_TAF:
                    weatherData = WEATHER_SERVICE.getTafData(station);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown request type: " + requestType);
            }
            
            // Handle output based on user preferences
            if (options.shouldPrint()) {
                displayWeatherData(requestType, station, weatherData);
            }
            
            LOGGER.info(REQUEST_PROCESSED_MSG, requestType, station);
        } catch (WeatherServiceException e) {
            handleWeatherServiceError(requestType, station, e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid request type: {}", requestType);
            LOGGER.error(EXCEPTION_DETAILS_MSG, e.getMessage(), e);
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected runtime error processing {} request for station {}", requestType, station);
            LOGGER.error(EXCEPTION_DETAILS_MSG, e.getMessage(), e);
        }
    }

    /**
     * Handles the case where a station code is missing from a weather request.
     * 
     * @param requestType Type of weather request
     */
    private static void handleMissingStationCode(String requestType) {
        String configKey = WEATHER_TYPE_METAR.equals(requestType) ? "MSG_MET_PARM" : "MSG_TAF_PARM";
        String logMsg = WEATHER_CONFIG.getLogMessage(configKey);
        
        LOGGER.warn("{} command requires station parameter", requestType);
        LOGGER.error("{} command requires a station code", requestType);
        LOGGER.error("{}", logMsg);
    }

    /**
     * Displays weather data to the user.
     * 
     * @param requestType Type of weather request
     * @param station Station code
     * @param weatherData The weather data to display
     */
    private static void displayWeatherData(String requestType, String station, String weatherData) {
        LOGGER.info("{} Data for {}:", requestType, station);
        LOGGER.info("{}", weatherData);
    }

    /**
     * Handles errors that occur during weather service operations.
     * 
     * @param requestType Type of weather request
     * @param station Station code
     * @param e The exception that occurred
     */
    private static void handleWeatherServiceError(String requestType, String station, Exception e) {
        String configKey = WEATHER_TYPE_METAR.equals(requestType) ? "WEATHER_GET_METAR" : "WEATHER_GET_TAF";
        String errorMsg = WEATHER_CONFIG.getExceptionMessage(configKey);
        
        LOGGER.error("Error processing {} request for station {}: {}", requestType, station, errorMsg);
        LOGGER.error(EXCEPTION_DETAILS_MSG, e.getMessage(), e);
    }

    /**
     * Handles general application errors.
     * 
     * @param e The exception that occurred
     */
    private static void handleApplicationError(Exception e) {
        LOGGER.error("Application error occurred", e);
        String errorMsg = WEATHER_CONFIG.getExceptionMessage("NULL_POINTER_EXCEPTION");
        LOGGER.error("Error: {} - {}", errorMsg, e.getMessage());
    }

    /**
     * Displays help information to the user.
     */
    public static void displayHelp() {
        String helpText = buildHelpText();
        LOGGER.info("Help information displayed: {}", helpText);
    }
    
    /**
     * Displays version information to the user.
     */
    public static void displayVersion() {
        String versionInfo = String.format("%s version %s", APP_NAME, APP_VERSION);
        LOGGER.info("Version information displayed: {}", versionInfo);
    }

    /**
     * Builds the help text for the application.
     * 
     * @return Formatted help text
     */
    private static String buildHelpText() {
        return String.format("%s v%s - Weather Data Processing Application%n%n" +
                "Usage: java -jar noakweather.jar [COMMAND] [STATION] [OPTIONS]%n%n" +
                "Commands:%n" +
                "  help, -h, --help          Show this help message%n" +
                "  version, -v, --version    Show version information%n" +
                "  metar, -m [station]       Get METAR data for station%n" +
                "  taf, -t [station]         Get TAF data for station%n%n" +
                "Options:%n" +
                "  -p [y|n]                  Print weather data to output (default: n)%n" +
                "  -l [level]                Set log level%n" +
                "                            Levels: i|info, d|debug, w|warn, e|error%n%n" +
                "Examples:%n" +
                "  java -jar noakweather.jar help%n" +
                "  java -jar noakweather.jar version%n" +
                "  java -jar noakweather.jar metar KJFK%n" +
                "  java -jar noakweather.jar -m KJFK%n" +
                "  java -jar noakweather.jar taf KJFK%n" +
                "  java -jar noakweather.jar -t KJFK%n" +
                "  java -jar noakweather.jar metar KJFK -p y%n" +
                "  java -jar noakweather.jar -m KCLT -p y -l debug%n" +
                "  java -jar noakweather.jar taf KJFK -p n -l i%n%n" +
                "Station Codes:%n" +
                "  Use standard ICAO airport codes (3-4 letters)%n" +
                "  Examples: KJFK, KCLT, KORD, KLAX%n%n" +
                "For more information, visit: https://github.com/bclasky1539/noakweather-java%n",
                APP_NAME, APP_VERSION);
    }

    /**
     * Configures the logging level based on user input.
     * 
     * @param logLevel The desired log level
     */
    private static void configureLogging(String logLevel) {
        if (logLevel == null || logLevel.trim().isEmpty()) {
            Configurator.setRootLevel(Level.INFO);
            LOGGER.info(LOG_LEVEL_INFO);
            return;
        }

        switch (logLevel.toLowerCase()) {
            case "i":
            case "info":
                Configurator.setRootLevel(Level.INFO);
                LOGGER.info(LOG_LEVEL_INFO);
                break;
            case "d":
            case "debug":
                Configurator.setRootLevel(Level.DEBUG);
                LOGGER.info(LOG_LEVEL_DEBUG);
                break;
            case "w":
            case "warn":
                Configurator.setRootLevel(Level.WARN);
                LOGGER.info(LOG_LEVEL_WARN);
                break;
            case "e":
            case "error":
                Configurator.setRootLevel(Level.ERROR);
                LOGGER.info(LOG_LEVEL_ERROR);
                break;
            default:
                LOGGER.warn("Unknown log level: {}. Using INFO level as default.", logLevel);
                Configurator.setRootLevel(Level.INFO);
                LOGGER.info(LOG_LEVEL_INFO);
                break;
        }
    }

    // Getter methods for testing and external access
    public static String getAppName() {
        return APP_NAME;
    }
    
    public static String getAppVersion() {
        return APP_VERSION;
    }
    
    public static String getAppDescription() {
        return APP_DESCRIPTION;
    }
    
    public static WeatherConfigurationService getWeatherConfig() {
        return WEATHER_CONFIG;
    }

    /**
     * Helper class to hold parsed command line options.
     * 
     * This acts like a structured form that collects all the user's preferences
     * in one organized place.
     */
    private static class CommandOptions {
        private boolean print = false;
        private String logLevel = null;
        private final List<String> positionalArgs = new ArrayList<>();
        
        public void setPrint(boolean print) { 
            this.print = print; 
        }
        
        public void setLogLevel(String logLevel) { 
            this.logLevel = logLevel; 
        }
        
        public void addPositionalArg(String arg) { 
            this.positionalArgs.add(arg); 
        }
        
        public boolean shouldPrint() { 
            return print; 
        }
        
        public String getLogLevel() { 
            return logLevel; 
        }
        
        public String getCommand() { 
            return positionalArgs.isEmpty() ? null : positionalArgs.get(0); 
        }
        
        public String getStationCode() { 
            return positionalArgs.size() > 1 ? positionalArgs.get(1) : null; 
        }
        
        public List<String> getPositionalArgs() { 
            return Collections.unmodifiableList(positionalArgs); 
        }
    }
}
