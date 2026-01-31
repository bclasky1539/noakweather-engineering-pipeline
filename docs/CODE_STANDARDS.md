# NoakWeather Java - Code Standards Documentation

## Overview
This document establishes coding standards for the NoakWeather Java library, which parses METAR and TAF weather reports. These standards ensure consistency, maintainability, and clarity throughout the codebase.

## Project Structure & Package Organization

### Package Hierarchy
The NoakWeather Engineering Pipeline follows a multi-module Lambda Architecture. Each module has a specific responsibility in the data processing pipeline.

```
noakweather-engineering-pipeline/
├── docs/                                        # Project-wide documentation
│   ├── CODE_STANDARDS.md                        # Development guidelines (this file)
│   ├── WEATHER_FORMAT_REFERENCES.md             # METAR/TAF specifications
│   ├── AWS_IAM_DYNAMODB_SETUP.md                # AWS IAM setup guide
│   ├── LOGGING_SETUP.md                         # Centralized logging configuration
│   └── PHASE_4_GSI_DEPLOYMENT_GUIDE.md          # DynamoDB GSI deployment
├── noakweather-legacy/                          # Legacy single-module implementation
│   └── [legacy codebase]                        # Original noakweather-java code
└── noakweather-platform/                        # Multi-module Lambda Architecture
    ├── src/main/resources/
    │   └── log4j2.xml                           # Master logging configuration
    │
    ├── logs/                                    # Centralized platform-level logs
    │   ├── noakweather.log                      # All application logs
    │   ├── noakweather-error.log                # Error-level logs
    │   ├── dynamodb.log                         # DynamoDB operations
    │   └── archive/                             # Rolled/compressed logs
    │
    ├── weather-common/                          # Shared models, interfaces, utilities
    │   └── src/main/java/weather/
    │       ├── exception/                       # Common exception hierarchy
    │       │   ├── ErrorType.java               # Enumeration of error types
    │       │   ├── WeatherParseException.java   # Parsing errors
    │       │   └── WeatherServiceException.java # Service layer errors
    │       ├── model/                           # Domain model - shared data structures
    │       │   ├── components/                  # Weather data components
    │       │   │   │   ├── ForecastPeriod.java
    │       │   │   │   ├── PresentWeather.java
    │       │   │   │   ├── Pressure.java
    │       │   │   │   ├── RunwayVisualRange.java
    │       │   │   │   ├── SkyCondition.java
    │       │   │   │   ├── Temperature.java
    │       │   │   │   ├── ValidityPeriod.java
    │       │   │   │   ├── Visibility.java
    │       │   │   │   └── Wind.java
    │       │   │   └── remark/                  # Remark-specific components
    │       │   ├── enums/                         # Enumerations
    │       │   │   ├── AutomatedStationType.java  # Type of automated weather station
    │       │   │   ├── ChangeIndicator.java       # Types of forecast change indicators
    │       │   │   ├── PressureUnit.java          # Enumeration of atmospheric pressure units
    │       │   │   └── SkyCoverage.java           # Sky coverage enumeration
    │       │   ├── GeoLocation.java             # Geographic locations
    │       │   ├── ProcessingLayer.java         # Lambda layer types
    │       │   ├── WeatherDataSource.java       # Data source identifiers
    │       │   ├── NoaaMetarData.java           # NOAA METAR data model
    │       │   ├── NoaaTafData.java             # NOAA TAF data model
    │       │   ├── NoaaWeatherData.java         # Base NOAA weather data
    │       │   ├── TestWeatherData.java         # Test data model
    │       │   ├── WeatherConditions.java       # Weather conditions model
    │       │   └── WeatherData.java             # Universal weather data interface
    │       ├── service/                         # Common service interfaces
    │       │   ├── ValidationResult.java        # Validation result model
    │       │   ├── ValidationResultBuilder.java # Builder for validation results
    │       │   ├── WeatherParser.java           # Universal parser interface
    │       │   ├── WeatherService.java          # Main service interface
    │       │   └── WeatherValidator.java        # Validation interface
    │       └── utils/                           # Common utilities
    │           ├── AviationWeatherDecoder.java  # METAR/TAF decoder utilities
    │           ├── IndexedLinkedHashMap.java    # Custom collection
    │           └── ValidationPatterns.java      # Validation regex patterns
    │
    ├── weather-ingestion/                       # Speed Layer - Real-time data collection
    │   └── src/main/java/weather/ingestion/
    │       ├── config/                          # Ingestion configuration
    │       │   └── NoaaConfiguration.java       # NOAA API configuration
    │       └── service/                         # Ingestion services
    │           └── source/                      # Source-specific implementations
    │               └── noaa/                    # NOAA data source
    │                   ├── AbstractNoaaIngestionApp.java
    │                   ├── AbstractNoaaIngestionOrchestrator.java
    │                   ├── MetarIngestionApp.java
    │                   ├── MetarIngestionOrchestrator.java
    │                   ├── NoaaAviationWeatherClient.java
    │                   ├── TafIngestionApp.java
    │                   ├── TafIngestionOrchestrator.java
    │                   ├── openweathermap/      # OpenWeatherMap integration
    │                   ├── weathergov/          # Weather.gov integration
    │                   ├── S3UploadService.java
    │                   └── SpeedLayerProcessor.java
    │
    ├── weather-processing/                      # Batch Layer - Data parsing/transformation
    │   └── src/main/java/weather/processing/
    │       ├── config/                          # Parser configuration
    │       │   └── ParserConfiguration.java     # Parser settings
    │       ├── parser/                          # Parser implementations
    │       │   ├── common/                      # Common parser interfaces
    │       │   │   ├── ParseResult.java         # Parser result wrapper
    │       │   │   ├── ParserException.java     # Parser exceptions
    │       │   │   └── WeatherParser.java       # Base parser interface
    │       │   └── noaa/                        # NOAA-specific parsers
    │       │       ├── LightningMatcher.java    # Lightning data parsing
    │       │       ├── NoaaAviationWeatherParser.java      # Base NOAA parser
    │       │       ├── NoaaAviationWeatherPatternLibrary.java  # Regex patterns
    │       │       ├── NoaaMetarParser.java     # METAR parser
    │       │       ├── NoaaTafParser.java       # TAF parser
    │       │       └── RegExprConst.java        # Regular expression constants
    │       ├── service/                         # Processing services
    │       │   └── UniversalWeatherParserService.java  # Universal parser service
    │       └── resources/
    │           └── parser.properties            # Parser configuration
    │
    ├── weather-storage/                         # Serving Layer - Multi-backend storage
    │   └── src/main/java/weather/storage/
    │       ├── config/                          # Storage configuration
    │       │   ├── DynamoDbConfig.java          # DynamoDB AWS SDK configuration
    │       │   └── DynamoDbTableConfig.java     # Table and GSI definitions
    │       ├── examples/                        # Example/demo code (excluded from coverage)
    │       │   └── QuickDynamoDbTest.java       # Quick DynamoDB test
    │       ├── exception/                       # Storage exceptions
    │       │   ├── RepositoryException.java     # Repository layer exceptions
    │       │   └── WeatherDataMappingException.java  # Mapping errors
    │       ├── repository/                      # Repository pattern implementations
    │       │   ├── dynamodb/                    # DynamoDB repository
    │       │   │   ├── DynamoDbMapper.java      # Bidirectional object mapping
    │       │   │   └── DynamoDbRepository.java  # DynamoDB CRUD + GSI queries
    │       │   └── snowflake/                   # Snowflake repository (future)
    │       │   │   └── SnowflakeRepository.java # Snowflake implementation
    │       │   ├── AbstractStubRepository.java
    │       │   ├── RepositoryStats.java
    │       │   └── UniversalWeatherRepository.java
    │       ├── service/                         # Storage services
    │       │   ├── source/                      # Source-specific processing
    │       │   ├── BatchLayerProcessor.java
    │       │   ├── BatchProcessingResult.java
    │       │   └── BatchProcessingStats.java
    │       └── tools/                           # Operational tools (excluded from coverage)
    │           ├── AddGSIsToAwsTable.java       # Production GSI deployment tool
    │           └── TestLogRollover.java         # Log rollover testing
    │
    ├── weather-analytics/                       # Analytics and reporting services
    │   └── src/main/java/com/weather/analytics/
    │       └── service/                         # Analytics services
    │           └── [analytics implementations]
    │
    └── weather-infrastructure/                  # Infrastructure as Code (AWS CDK)
        └── src/main/java/com/weather/infra/
            └── [CDK stack definitions]
```

### Module Responsibilities

**weather-common**
- Shared domain models and interfaces
- Common exception hierarchy
- Validation utilities and patterns
- Universal parser and service interfaces

**weather-ingestion** (Speed Layer)
- Real-time data collection from multiple sources
- NOAA Aviation Weather API client
- OpenWeatherMap integration
- Data source orchestration

**weather-processing** (Batch Layer)
- METAR/TAF parsing
- Data transformation and validation
- Format-specific parsers (NOAA, Weather.gov, etc.)
- Universal parser service

**weather-storage** (Serving Layer)
- Multi-backend storage abstraction
- DynamoDB repository with GSI support
- Snowflake integration (planned)
- Batch processing services

**weather-analytics**
- Analytics and reporting
- Data aggregation
- Statistical analysis

**weather-infrastructure**
- AWS CDK infrastructure definitions
- Cloud resource management
- Deployment automation

### Architecture Principles
- **Provider Pattern**: Each weather API (NOAA, OpenWeatherMap) is self-contained
- **Inheritance Hierarchy**: METAR/TAF extend NoaaAviationWeatherData base class
- **Clean Separation**: No cross-dependencies between weather providers
- **Layered Structure**: service → client → model → exception within each provider

### Class Organization Principles
- **Domain-first approach**: Start with domain models before implementation
- **Single Responsibility**: Each class has one clear purpose
- **Layered architecture**: Domain → Parser → Service → Util
- **Immutable domain objects**: Weather data should be immutable once parsed

## Naming Conventions

### Package Naming Conventions

Follow these conventions for package names:

1. **Use lowercase only**: `weather.storage.repository`
2. **Use singular nouns**: `weather.model` not `weather.models`
3. **Organize by feature/layer**: Group related functionality
4. **Keep depth reasonable**: 3-5 levels maximum
5. **Use meaningful names**: Avoid abbreviations unless standard

### Cross-Module Dependencies

Modules should follow this dependency hierarchy (dependencies flow downward):

```
weather-analytics ──────┐
weather-storage ────────┼──> weather-processing ──┐
weather-ingestion ──────┘                         ├──> weather-common
weather-infrastructure ───────────────────────────┘
```

**Rules:**
- All modules depend on `weather-common`
- Higher layers can depend on lower layers
- No circular dependencies allowed
- Use interfaces from `weather-common` for cross-module communication

### Classes
- **Base Classes**: Abstract base with common aviation methods
  - `NoaaAviationWeatherData` (abstract base class)
- **Weather Data Models**: Extend base class appropriately
  - `NoaaMetarData extends NoaaAviationWeatherData`
  - `NoaaTafData extends NoaaAviationWeatherData`
  - `OpenWeatherCurrentData`, `OpenWeatherForecastData`
- **Service Classes**: End with "Service"
  - `WeatherService`, `NoaaWeatherService`, `OpenWeatherMapService`
- **Client Classes**: End with "Client"
  - `NoaaApiClient`, `NoaaHttpClient`, `OpenWeatherApiClient`
- **Configuration Classes**: Descriptive names
  - `WeatherConfigurationService`, `WeatherConfigurationFactory`
- **Exceptions**: End with "Exception"
  - `WeatherServiceException`, `NoaaApiException`, `OpenWeatherApiException`

### Methods
- **Service methods**: Business-oriented naming
  - `getWeatherData()`, `getCurrentConditions()`, `getForecast()`
- **API client methods**: API-specific operations
  - `fetchMetarData()`, `fetchTafData()`, `callNoaaEndpoint()`
- **Model methods**: Data access and common aviation operations
  - `getWindDirection()`, `getWindSpeed()`, `getVisibility()`
  - `getTemperature()`, `getDewPoint()`, `getCloudLayers()`
- **Configuration methods**: Setup and factory patterns
  - `createConfiguration()`, `buildWeatherService()`

### Variables
- **Weather data fields**: Based on aviation weather elements
  - `stationCode`, `rawData`, `retrievedAt` (base class fields)
  - `windDirection`, `windSpeed`, `windGust`, `visibility`
  - `temperature`, `dewPoint`, `altimeter`, `weatherConditions`, `cloudLayers`
- **Service fields**: Clear service-oriented naming
  - `weatherService`, `noaaApiClient`, `configurationFactory`
- **API fields**: Provider-specific naming
  - `noaaEndpointUrl`, `openWeatherApiKey`, `httpTimeout`
- **Constants**: ALL_CAPS with underscores
  - `DEFAULT_TIMEOUT`, `NOAA_BASE_URL`, `METAR_ENDPOINT`

### Weather Domain Terminology
- Use standard meteorological terms consistently
- Avoid abbreviations in code (except for well-known ones like METAR, TAF)
- Document any aviation-specific terminology

## Code Structure Standards

### Class Structure Order
1. Static constants
2. Instance fields
3. Constructors
4. Public methods
5. Package-private methods
6. Private methods
7. Static methods
8. Inner classes

### Method Design
- **Maximum method length**: 20 lines
- **Parameter limit**: 5 parameters max
- **Return early**: Use guard clauses to reduce nesting
- **Immutable parameters**: Prefer final parameters

### Inheritance and Abstraction Standards
```java
// Base class example following the established pattern
public abstract class NoaaAviationWeatherData {
    // Common fields for all aviation weather data
    protected final String stationCode;
    protected final String rawData;
    protected final LocalDateTime retrievedAt;
    
    // Common aviation methods (as shown in inheritance diagram)
    public abstract String getWindDirection();
    public abstract String getWindSpeed();
    public abstract String getWindGust();
    public abstract String getVisibility();
    public abstract String getTemperature();
    public abstract String getDewPoint();
    public abstract String getAltimeter();
    public abstract List<String> getWeatherConditions();
    public abstract List<String> getCloudLayers();
    
    // Abstract method for report type identification
    public abstract String getReportType();
}

// Concrete implementation example
public final class NoaaMetarData extends NoaaAviationWeatherData {
    // METAR-specific fields and methods
    private final String reportTime;
    private final String flightCategory;
    private final boolean isAutomatedReport;
    private final boolean isCorrectedReport;
    
    // METAR-specific methods
    public String getReportTime() { return reportTime; }
    public String getFlightCategory() { return flightCategory; }
    // ... implement all abstract methods from base class
}
```

## Error Handling Standards

### Exception Hierarchy
Based on the provider pattern architecture:
```
WeatherServiceException (base)
├── NoaaApiException
│   ├── InvalidMetarException
│   ├── InvalidTafException
│   └── NoaaEndpointException
├── OpenWeatherApiException
│   ├── InvalidApiKeyException
│   └── OpenWeatherEndpointException
└── ConfigurationException
    ├── InvalidConfigurationException
    └── MissingConfigurationException
```

### Provider-Specific Error Handling
- **Service Layer**: Handle cross-provider errors and coordination
- **API Layer**: Handle provider-specific API errors (timeouts, invalid responses)
- **Model Layer**: Handle data validation and parsing errors
- **Configuration Layer**: Handle setup and initialization errors

### Error Handling Patterns
- **Fail fast**: Validate inputs immediately
- **Descriptive messages**: Include context about what failed
- **Preserve original data**: Include raw input in error messages
- **Log at appropriate levels**: ERROR for failures, WARN for unparsed data

### Example Error Handling
```java
public WindInformation parseWind(String windGroup) {
    if (windGroup == null || windGroup.trim().isEmpty()) {
        throw new InvalidMetarException("Wind group cannot be null or empty");
    }
    
    try {
        return parseWindInternal(windGroup);
    } catch (NumberFormatException e) {
        throw new InvalidMetarException(
            "Invalid wind format: " + windGroup, e);
    }
}
```

## Testing Standards

### Test Class Organization
- **Test class naming**: `ClassNameTest`
- **Test method naming**: `should_DoSomething_When_Condition()`
- **Test structure**: Arrange, Act, Assert pattern

### Test Categories
- **Unit tests**: Test individual classes in isolation
- **Integration tests**: Test parser with real METAR/TAF data
- **Edge case tests**: Test boundary conditions and error cases

### Test Data Management
- **Use test data files**: Store sample METAR/TAF reports in resources
- **Parameterized tests**: For testing multiple similar inputs
- **Mock external dependencies**: Keep tests focused and fast

### Example Test Structure
```java
class MetarParserTest {
    
    @Test
    void should_ParseWindCorrectly_When_ValidWindGroup() {
        // Arrange
        String windGroup = "25012KT";
        
        // Act
        WindInformation result = parser.parseWind(windGroup);
        
        // Assert
        assertThat(result.getDirection()).isEqualTo(250);
        assertThat(result.getSpeed()).isEqualTo(12);
        assertThat(result.getGustSpeed()).isEmpty();
    }
}
```

## Documentation Standards

### JavaDoc Requirements
- **All public classes and methods**: Must have JavaDoc
- **Package-private methods**: JavaDoc recommended
- **Private methods**: JavaDoc for complex logic only

### JavaDoc Format
```java
/**
 * Parses METAR wind information from a wind group string.
 * 
 * @param windGroup the wind group string (e.g., "25012KT", "VRB03G15KT")
 * @return parsed wind information including direction, speed, and gust
 * @throws InvalidMetarException if the wind group format is invalid
 */
public WindInformation parseWind(String windGroup) {
    // Implementation
}
```

### README Updates
- Update main README.md when adding new features
- Include code examples for new parsing capabilities
- Document any new command-line options or parameters

## Git Workflow Standards

### Branch Naming
- **Feature branches**: `feature/wind-parsing-enhancement`
- **Bug fixes**: `fix/visibility-parsing-bug`
- **Documentation**: `docs/update-readme`

### Commit Messages
```
type: short description (50 chars max)

Longer explanation if needed (72 chars per line)

- Bullet points for details
- Reference issues: Fixes #123
```

### Commit Types
- `feat`: New features
- `fix`: Bug fixes
- `docs`: Documentation updates
- `test`: Test additions/updates
- `refactor`: Code refactoring
- `style`: Code style changes

### Pull Request Standards
- **Title**: Clear, descriptive summary
- **Description**: Explain what and why
- **Testing**: Describe testing performed
- **Checklist**: Use PR template checklist

### Future Extensions Architecture
Following the established provider pattern for extensibility:

**New Weather Provider Template:**
```
future_provider_api/               # e.g., weather_gov_api/, accuweather_api/
├── service/                       # Provider-specific service
├── client/                        # API client & HTTP client
├── model/                         # Provider-specific data models
└── exception/                     # Provider-specific exceptions
```

**Integration Points:**
- **Main WeatherService**: Coordinate between multiple providers
- **Configuration**: Add new provider configurations
- **Consistent Interface**: All providers follow same service pattern
- **Independent Development**: Providers can be developed and tested separately

This architecture supports the planned extensions (Weather.gov API, AccuWeather API, National Weather Service) while maintaining clean separation and consistent patterns.

## Quality Metrics

### Code Coverage
- **Minimum coverage**: 80% line coverage
- **Critical paths**: 95% coverage for parsing logic
- **Exclude from coverage**: DTOs, simple getters/setters

### Static Analysis
- **Use SpotBugs/PMD**: Configure in Maven build
- **Checkstyle**: Enforce coding standards automatically
- **SonarQube**: Monitor code quality metrics

## Dependencies and Libraries

### Allowed Dependencies
- **Testing**: JUnit 5, AssertJ
- **Utilities**: Apache Commons Lang, Google Guava (if needed)
- **Logging**: SLF4J with Logback
- **Build tools**: Maven, standard plugins

### Dependency Guidelines
- **Minimize dependencies**: Only add what is truly needed
- **Version consistency**: Use dependency management
- **Security**: Regular dependency updates
- **License compatibility**: Ensure compatible licenses

## Continuous Integration

### Build Requirements
- **All tests pass**: Zero test failures
- **Code coverage**: Meet minimum thresholds
- **Static analysis**: No high-priority violations
- **Documentation**: All public APIs documented

### Pre-commit Checks
- **Format code**: Consistent formatting
- **Run tests**: Quick smoke tests
- **Check dependencies**: No vulnerable dependencies
