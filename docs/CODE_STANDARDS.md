# NoakWeather Java - Code Standards Documentation

## Overview
This document establishes coding standards for the NoakWeather Java library, which parses METAR and TAF weather reports. These standards ensure consistency, maintainability, and clarity throughout the codebase.

## Project Structure & Package Organization

### Package Hierarchy
Based on the established architecture, follow this structure:

```
src/main/java/noakweather/
├── service/                              # Main coordination layer
│   ├── WeatherService.java               # Main service interface
│   ├── WeatherServiceImpl.java           # Service implementation
│   └── exception/                        # Service layer exceptions
│       └── WeatherServiceException.java
├── noaa_api/                             # Complete NOAA implementation
│   ├── service/                          # NOAA-specific service
│   ├── client/                           # Main NOAA API client & HTTP client
│   ├── model/                            # Base classes (NoaaAviationWeatherData)
│   │   ├── NoaaMetarData.java            # Extends base - METAR specific
│   │   └── NoaaTafData.java              # Extends base - TAF specific
│   └── exception/                        # NOAA-specific exceptions
├── openweather_api/                      # Complete OpenWeatherMap implementation
│   ├── service/                          # OpenWeatherMap service
│   ├── client/                           # OpenWeatherMap API client & HTTP client
│   ├── model/                            # Current & forecast data models
│   └── exception/                        # OpenWeatherMap exceptions
├── config/                               # Configuration classes
│   ├── WeatherConfigurationService.java  # Configuration management
│   └── WeatherConfigurationFactory.java  # Configuration factory
└── NoakWeatherMain.java                  # Main application entry point
```

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
