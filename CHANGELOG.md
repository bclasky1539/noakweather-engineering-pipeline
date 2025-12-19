# Changelog

All notable changes to the NoakWeather Engineering Pipeline project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Version 1.7.0-SNAPSHOT - December 18, 2024

#### Enhanced METAR Remarks Parser - Phase 1 Complete

**Added:**
- **METAR Remarks Parser Components** (4 major remark types implemented)
    - Variable Visibility (`VIS min V max` with optional direction/location)
    - Tower/Surface Visibility (`TWR VIS` / `SFC VIS`)
    - Precipitation Amount (Hourly `P`, 6-hour `6`, 24-hour `7`)
    - Hail Size (`GR` followed by size in inches)

- **Value Object Components** (weather-common)
    - `VariableVisibility` record - Variable visibility range representation
        - Minimum and maximum visibility values
        - Optional direction (N, NE, E, SE, S, SW, W, NW)
        - Optional location qualifier (RWY, TWR, VC)
        - Query methods (`hasDirection()`, `hasLocation()`, `getSpread()`)
        - Human-readable descriptions and summaries
        - Comprehensive validation (min ≤ max, valid ranges)

    - `PrecipitationAmount` record - Precipitation accumulation data
        - Support for 1-hour, 3-hour, 6-hour, and 24-hour periods
        - Inches measurement with metric conversion
        - Trace precipitation handling (`////` format)
        - Severity classification (measurable vs trace)
        - Period identification methods (`isHourly()`, `isSixHour()`, etc.)
        - Human-readable descriptions

    - `HailSize` record - Hail stone size measurements
        - Size in inches with cm/mm conversions
        - NWS-standard size categories (pea, marble, quarter, golf ball, etc.)
        - Severe weather thresholds (≥1.0" severe, ≥2.0" significantly severe)
        - Validation (positive values, reasonable upper limit of 10")
        - Category-based descriptions and summaries

**Enhanced:**
- **NoaaMetarRemarks Model** (weather-common)
    - Added 7 new fields to record:
        - `VariableVisibility variableVisibility`
        - `Visibility towerVisibility`
        - `Visibility surfaceVisibility`
        - `PrecipitationAmount hourlyPrecipitation`
        - `PrecipitationAmount sixHourPrecipitation`
        - `PrecipitationAmount twentyFourHourPrecipitation`
        - `HailSize hailSize`
    - Total parameters: 14 (up from 7)
    - Builder pattern updated with new field methods
    - `isEmpty()` checks all 14 fields
    - `toString()` includes all new fields with summaries
    - Maintained 100% test coverage

- **NoaaMetarParser** (weather-processing)
    - Added 6 sequential handler methods:
        - `handleVariableVisibilitySequential()` - Parses VIS min V max patterns
        - `handleTowerSurfaceVisibilitySequential()` - Parses TWR/SFC VIS
        - `handleHourlyPrecipitationSequential()` - Parses P group
        - `handleMultiHourPrecipitationSequential()` - Parses 6/7 groups
        - `handleHailSizeSequential()` - Parses GR group
        - Reuses `parseVisibilityDistance()` helper (fractions, mixed, whole numbers)
    - Updated `handleRemarks()` multi-pass loop with new handlers
    - Updated `handlePattern()` switch with new cases

- **MetarPatternRegistry** (weather-processing)
    - Registered 4 new remark patterns:
        - `VPV_SV_VSL_PATTERN` → "variableVis"
        - `TWR_SFC_VIS_PATTERN` → "towerSurfaceVis"
        - `PRECIP_1HR_PATTERN` → "precip1Hour"
        - `PRECIP_3HR_24HR_PATTERN` → "precip3Hr24Hr"
        - `HAIL_SIZE_PATTERN` → "hailSize"

- **RegExprConst** (weather-processing)
    - Added new regex pattern:
        - `HAIL_SIZE_PATTERN` - Matches GR followed by size (fractions, mixed, whole)

**Testing:**
- **weather-common**: 1,792 tests (+167 new tests)
    - VariableVisibility: 45 tests (99% instruction, 90% branch coverage)
    - PrecipitationAmount: 52 tests (100% coverage)
    - HailSize: 72 tests (100% coverage)
    - NoaaMetarRemarks: +36 tests for new fields (100% coverage maintained)
    - All value objects achieve 99-100% coverage

- **weather-processing**: 613 tests (+95 new tests)
    - Variable Visibility parsing: 16 tests (36 executions)
    - Tower/Surface Visibility parsing: 9 tests (24 executions)
    - Precipitation Amount parsing: 11 tests (35 executions)
    - Hail Size parsing: 14 tests (26 executions)
    - MetarPatternRegistry: +2 tests for new patterns
    - Parser methods: 64-75% coverage (production quality)
        - Missing coverage is defensive code (null checks, exception handlers)

**METAR Remarks Now Supported:**
1. Automated Station Type (AO1/AO2)
2. Sea Level Pressure (SLP)
3. Hourly Temperature/Dewpoint (T-group)
4. Peak Wind (PK WND)
5. Wind Shift (WSHFT/FROPA)
6. Variable Visibility (VIS minVmax)
7. Tower Visibility (TWR VIS)
8. Surface Visibility (SFC VIS)
9. Hourly Precipitation (Prrrr)
10. 6-Hour Precipitation (6RRRR)
11. 24-Hour Precipitation (7RRRR)
12. Hail Size (GR size)

**Technical Details:**
- Comprehensive validation in all components
- Reused existing helpers where possible (parseVisibilityDistance)
- Multi-pass parsing ensures order-independent remark processing
- Real-world METAR examples validated in tests

**Notes:**
- Phase 1 of remarks parsing complete (7 remark types)
- Strong foundation established for future remark types
- All implementations follow consistent architectural patterns

### Version 1.6.0-SNAPSHOT - December 13, 2025

#### Enhanced METAR Parser Implementation - Main Body Components Complete

**Added:**
- **METAR Parser Components** (5 major handlers implemented)
    - `handlePresentWeather()` - Complete present weather parsing
    - `handleSkyCondition()` - Cloud layer parsing
    - `handleTempDewpoint()` - Temperature and dewpoint parsing
    - `handleAltimeter()` - Pressure/altimeter parsing
    - `RunwayVisualRange` - Enhanced parsing support

- **Value Object Components** (weather-common)
    - `PresentWeather` record - Immutable weather phenomenon representation
        - Parse from METAR code (e.g., "-SHRA", "BCFG")
        - Comprehensive query methods (isPrecipitation, isObscuration, etc.)
        - Human-readable descriptions
        - Primary phenomenon identification

    - `SkyCondition` record - Immutable cloud layer representation
        - Coverage types with descriptions
        - Height in feet above ground level
        - Optional cloud type (CB/TCU)
        - Ceiling identification
        - Query methods (isCeiling, isCumulonimbus, etc.)

**Enhanced:**
- **Architecture Refactoring** (improved inheritance hierarchy)
    - Moved `Temperature` from `NoaaMetarData` → `WeatherData` (universal)
    - Moved `Pressure` from `NoaaMetarData` → `WeatherData` (universal)
    - Moved `SkyCondition` list from `NoaaMetarData` → `NoaaWeatherData` (NOAA-specific)
    - Fixed duplicate `presentWeather` field bug in `NoaaMetarData`
    - Consistent pattern: universal components in `WeatherData`, NOAA-specific in `NoaaWeatherData`

- **Pressure Component** (weather-common)
    - Fixed null pointer dereference in `getHeatIndex()` method
    - Improved type safety (Double vs double for conversions)
    - Added explicit null checks for safety

- **Temperature Component** (weather-common)
    - Heat index calculation with NOAA's official algorithm
    - Relative humidity using August-Roche-Magnus approximation
    - Icing condition detection
    - Fog likelihood assessment
    - Comprehensive aviation and meteorological query methods

**Testing:**
- **weather-common**: 1,625 tests (+21 tests for Pressure, +16 for Temperature)
    - 100% instruction coverage for new components
    - All architectural changes validated

- **weather-processing**: 186 tests (+23 new METAR component tests)
    - PresentWeather: 20 parameterized scenarios + edge cases
    - SkyCondition: 15 parameterized scenarios + OCR errors
    - Temperature: 14 parameterized scenarios + conversions + query methods
    - Altimeter: 13 parameterized scenarios + edge cases
    - Overall parser coverage: 84% instruction / 73% branch
    - All missing coverage is defensive code (null checks, exception handlers)

**METAR Components Now Supported:**
1. Station ID & observation time (existing)
2. Report type & modifiers (existing)
3. Wind (direction, speed, gusts, variability) (existing)
4. Visibility (statute miles, meters, special conditions) (existing)
5. Runway Visual Range with CLRD flag (enhanced)
6. **Present Weather** - intensity, descriptor, precipitation, obscuration (NEW)
7. **Sky Conditions** - FEW/SCT/BKN/OVC/VV with heights and cloud types (NEW)
8. **Temperature/Dewpoint** - with negative sign handling (NEW)
9. **Altimeter** - multiple formats (A/Q/QNH/INS) (NEW)

**Technical Details:**
- Real-world METAR parsing validated with production examples
- All handlers follow consistent architecture pattern

**Notes:**
- Main METAR body parsing complete and production-ready
- Remarks section parsing deferred to next iteration
- Build time: ~3 seconds for weather-common, ~2.5 seconds for weather-processing
- Zero test failures across all modules

### Version 1.5.0-SNAPSHOT - November 21, 2025

#### Domain Model Refinement & Test Coverage Excellence

**Enhanced:**
- **NoaaMetarData Domain Model**
  - Migrated from `LocalDateTime` to `Instant` for observation timestamps
  - Improved immutability with `List.copyOf()` for collection getters
  - Enhanced utility methods: `getCeilingFeet()`, `getMinimumRvrFeet()`, `getRvrForRunway()`
  - Added flight category support with `hasFlightCategoryData()`
  - Comprehensive `equals()` and `hashCode()` implementations
  - Business-focused equality (excludes auto-generated fields)

- **RunwayVisualRange Value Object**
  - Converted to immutable record with factory methods
  - Static factories: `of()`, `variable()`, `lessThan()`, `greaterThan()`
  - Support for variable range (low/high), prefix indicators (M/P), trend information
  - Helper methods: `isVariable()`, `isLessThan()`, `isGreaterThan()`
  - Comprehensive trend descriptions (Upward, Downward, No Change)
  - Full test coverage with edge case handling

- **NoaaWeatherData Base Class**
  - Abstract base with sealed class hierarchy
  - Added `getSummary()` and `getDataType()` abstract methods
  - Improved field organization and documentation
  - Quality control flags support
  - Report modifier tracking (AUTO, COR, etc.)

**Testing:**
- **Comprehensive Test Suite** (99% instruction coverage, 82% branch coverage)
  - NoaaMetarDataTest: 95 tests covering all functionality
  
**Build & Quality:**
- All 95+ tests passing (0 failures, 0 errors, 0 skipped)
- Build time: ~8 seconds for weather-common module
- Maven build: clean install successful
- Ready for Day 7: Enhanced METAR Parser Implementation

### Version 1.4.0-SNAPSHOT Additional - November 8, 2025

#### Domain Model Foundation

**Added:**
**New Domain Model Components**
- **Value Objects (Records)**
  - `Wind` - Wind data with direction, speed, gusts, variability
  - `Visibility` - Visibility measurements with prevailing and runway visual range support
  - `Temperature` - Temperature and dewpoint data
  - `Pressure` - Pressure readings with multiple unit support (in/Hg, hPa, mb)
  - `SkyCondition` - Sky coverage layers with ceiling heights
  - `PeakWind` - Peak wind data from remarks section
  - `WindShift` - Wind shift timing and frontal passage indicators
  - `GeoLocation` - Geographic coordinates with elevation

**Core Data Classes**
- **`WeatherData`** (sealed abstract base)
  - Universal base for all weather data sources
  - Lambda architecture layer tracking
  - Immutable ID and ingestion timestamps
  - Source-agnostic design (NOAA, OpenWeatherMap, etc.)
  
- **`NoaaWeatherData`** (extends WeatherData)
  - NOAA Aviation Weather Center specific implementation
  - Report type support (METAR, TAF, PIREP)
  - Station metadata (coordinates, elevation)
  - Quality control flags

- **`NoaaMetarData`** (extends NoaaWeatherData)
  - Complete METAR observation support
  - Main body: wind, visibility, weather, sky conditions, temperature, pressure
  - Remarks: peak wind, wind shift, automated station, sea level pressure
  - Precipitation and temperature extremes
  - Flight category data support

- **`Enums`**
  - SkyCoverage - CLR, FEW, SCT, BKN, OVC, VV with descriptions
  - PressureUnit - INCHES_HG, HECTOPASCAL, MILLIBAR with conversions
  - WeatherDataSource - NOAA, OPENWEATHERMAP, WEATHERAPI, INTERNAL
  - ProcessingLayer - SPEED_LAYER, BATCH_LAYER, SERVING_LAYER

**Notes**
- Domain model is complete and tested, ready for parser integration
- Parser implementation deferred to next iteration

### Version 1.4.0-SNAPSHOT - Date: October 29, 2025

#### weather-storage Module - Weather Storage & Lambda Architecture Foundation

**Added:**
**Weather Storage**
- **Created weather-storage module** implementing Lambda Architecture patterns
- **Repository Pattern**: 
  - `UniversalWeatherRepository` - Unified interface for multiple storage backends
  - `DynamoDbRepository` - Stub for real-time Speed Layer storage
  - `SnowflakeRepository` - Stub for historical Batch Layer storage
  - `RepositoryStats` - Storage metrics and monitoring
- **Batch Layer Processing**:
  - `BatchLayerProcessor` - Batch processing orchestration
  - `BatchProcessingResult` - Processing results with builder pattern
  - `BatchProcessingStats` - Aggregated batch statistics
- **Test Coverage**: 69 comprehensive unit tests achieving 98% instruction, 97% branch coverage

**Weather Common Improvements**
- **TestWeatherData** - Test utility class for WeatherData testing
- Added to sealed class permits for testing purposes
- **Enhanced GeoLocation tests**: 100% instruction and branch coverage
- **Enhanced WeatherData tests**: 100% instruction and branch coverage using reflection
- Added WeatherDataTest.java with 10 comprehensive tests

**Weather Ingestion Improvements**
- **Enhanced S3UploadService tests**: 
  - Added validation tests for null/empty parameters
  - Added batch upload failure scenarios
  - Improved to 82% instruction, 91% branch coverage (+16% branch)
- **Enhanced SpeedLayerProcessor tests**:
  - Added custom concurrency constructor tests
  - Added validation failure tests (missing stationId, missing source)
  - Added continuous ingestion tests
  - Improved to 85% instruction, 86% branch coverage (+14% branch)

**Build & Quality**
- **JaCoCo Configuration**: Added TestWeatherData exclusion to parent POM
- **Overall Coverage**: weather-storage (98%/97%), weather-common (99%/97%), weather-ingestion (83%/89%)
- All 69+ new tests passing across modules
- Build time: ~40 seconds for full project

**Changed**
- Updated sealed class `WeatherData` to permit `TestWeatherData` for testing
- Improved test quality across modules with better edge case coverage
- Enhanced error handling and validation in S3UploadService

**Technical Debt Addressed**
- Improved branch coverage from 73% to 89% in weather-ingestion

### Version 1.3.0-SNAPSHOT - Date: October 26, 2025

#### weather-ingestion Module - Comprehensive Unit Test Coverage

**Added:**
- **NoaaConfiguration Test Suite** (21 tests, 94% coverage)
  - Configuration loading and defaults testing
  - URL building for single/multiple stations
  - Bounding box URL generation
  - Custom properties override validation
  - Edge case handling (invalid timeout, empty arrays, partial overrides)

- **S3UploadService Test Suite** (11 tests) using Mockito
  - Weather data upload validation
  - Batch upload functionality
  - Null data and missing station ID handling
  - S3 exception handling and wrapping
  - S3 key format generation
  - Metadata attachment validation

- **SpeedLayerProcessor Test Suite** (14 tests) using Mockito
  - Single station processing with validation
  - Batch station processing
  - Regional (bounding box) processing
  - Error handling (no data, network errors, S3 failures)
  - Metadata enrichment verification
  - Statistics and shutdown functionality

**weather-common Module - Exception Test Coverage**
- **ErrorType Test Suite** (11 tests, 100% coverage)
  - Enum value validation
  - Severity level checks
  - HTTP status code mapping
  - String representation
  - Invalid valueOf handling

- **WeatherServiceException Test Suite** (10 tests, 100% coverage)
  - Constructor variations
  - Error type association
  - Cause chain handling
  - Context preservation
  - getMessage formatting

**Testing:**
- Total Tests: 169 comprehensive tests (+63 from Day 3)
- weather-common Coverage: 100% (up from ~85%)
- weather-ingestion Coverage: 75% (up from 35%)
- Build Status: All tests passing (0 failures, 0 errors, 0 skipped)
- Test Framework: JUnit 5 with Mockito for mocking
- Integration Tests: 7 skipped (require AWS credentials)

**Technical Details:**
- Mockito 5.14.2 added for unit testing
- All Sonar quality rules satisfied (no unused imports, proper exception handling)
- Test methods properly declare checked exceptions

### Version 1.2.0-SNAPSHOT - Date: October 23, 2025

#### weather-processing Module - Weather Processing Module

**Added:**
- **Parser Infrastructure** - Multi-source weather data parsing framework
  - `WeatherParser<T>` interface - Universal parser contract for any weather source
  - `ParseResult<T>` - Type-safe result wrapper with explicit success/failure handling
  - `ParserException` - Custom exception with parsing context
  - `NoaaMetarParser` - METAR report parser adapted from legacy code
  - `UniversalWeatherParserService` - Parser routing and auto-detection service
  - `ParserConfiguration` - Feature flags and configuration management
  - `parser.properties` - Configuration file with feature flag defaults

- **Comprehensive Test Suite** (124 tests, 98% coverage)
  - `ParseResultTest` (24 tests) - Result wrapper functionality
  - `ParserExceptionTest` (10 tests) - Exception handling
  - `NoaaMetarParserTest` (23 tests) - METAR parsing with real examples
  - `UniversalWeatherParserServiceTest` (22 tests) - Service routing
  - `ParserConfigurationTest` (29 tests) - Configuration loading
  - `WeatherParserTest` (16 tests) - Interface contract

**Testing:**
- Total Tests: 124 comprehensive tests
- Overall Coverage: 98%
- Build Status**: All tests passing (0 failures, 0 errors, 0 skipped)
- ParseResultTest (24 tests)
- ParserExceptionTest (10 tests)
- NoaaMetarParserTest (23 tests)
- UniversalWeatherParserServiceTest (22 tests)
- ParserConfigurationTest (29 tests)
- WeatherParserTest (16 tests)

### Version 1.1.0-SNAPSHOT - Date: October 21, 2025

#### weather-common Module - Universal Weather Data Models

**Added:**
- **Universal Weather Data Model** (Java 17 sealed classes):
  - `WeatherData`: Abstract base class for all weather data sources
  - `NoaaWeatherData`: NOAA-specific implementation
  - `WeatherDataSource`: Enum for supported data sources (NOAA, OpenWeatherMap, WeatherAPI, etc.)
  - `ProcessingLayer`: Enum for Lambda Architecture layers (Speed, Batch, Serving, Raw)
  - `GeoLocation`: Immutable record for geographic coordinates with distance calculations
  
- **Service Interfaces** (Strategy and Validator patterns):
  - `WeatherParser<T>`: Universal interface for parsing weather data from any source
  - `WeatherParseException`: Exception for parse failures with raw data preservation
  - `WeatherValidator<T>`: Interface for data quality validation
  - `ValidationResult`: Immutable result object for validation outcomes
  - `ValidationResultBuilder`: Fluent builder for constructing validation results
  - `WeatherService<T>`: Facade interface for unified weather data access

**Testing:**
- Model tests: 48 tests covering all domain objects
- Service tests: 33 tests covering interfaces and supporting classes
- All tests use JUnit 5 with descriptive display names
- Mock implementations for interface testing

### Version 1.0.0-SNAPSHOT - Date: October 15, 2025

#### Major Architecture Rewrite - Multi-Module Platform Structure

**Breaking Changes:**
- Restructured entire project into multi-module Maven architecture
- Moved original codebase to `noakweather-legacy` module
- Created new `noakweather-platform` with Lambda Architecture design
- Upgraded from Java 11 to Java 17

**Added:**
- **noakweather-platform** parent module with 6 sub-modules:
  - `weather-common`: Source-agnostic shared models and interfaces
  - `weather-ingestion`: Universal data collection (Speed Layer)
  - `weather-processing`: Stream and batch processing (Batch Layer)
  - `weather-storage`: Multi-backend storage layer (Snowflake, DynamoDB, S3)
  - `weather-analytics`: Analytics and reporting (Serving Layer)
  - `weather-infrastructure`: AWS CDK infrastructure as code
- Centralized dependency management in parent POM
- Multi-module build scripts (`wethb.sh`, `wetht.sh`)
- Support for Java 17 features and performance improvements
- Lambda Architecture foundation for real-time and batch processing

**Changed:**
- Project name: `noakweather-java` → `noakweather-engineering-pipeline`
- Build system: Single module → Multi-module Maven reactor
- Architecture: NOAA-specific → Source-agnostic platform
- Java version: 11 → 17
- Packaging strategy: Now uses maven-shade-plugin for uber JARs

**Technical Details:**
- Maven Reactor build with proper module dependencies
- Dependency versions centralized in platform parent POM:
  - AWS SDK: 2.28.11
  - Jackson: 2.18.2
  - Snowflake JDBC: 3.23.1
  - JUnit 5: 5.11.4
  - Log4j2: 2.25.2
- JaCoCo code coverage configured for all modules
- SonarQube integration updated for multi-module analysis

**Migration Notes:**
- Legacy NOAA decoder remains in `noakweather-legacy`
- Platform modules are currently empty scaffolding (Day 1)
- Gradual migration path planned from legacy to platform
- Both modules can be built and tested independently

---

## [Legacy Versions]

## Version 0.0.5 - Date: October 1, 2025
- Added classes for noaa_api/model
- Renamed project locally and remotely

## Version 0.0.4 - Date: July 17, 2025
- Created comprehensive code standards document
- Created weather format references document

## Version 0.0.3 - Date: July 15, 2025
- Changes to the project to implement a more domain based architecture
- Added classes for services
- Fixed weth.sh shell script to allow all necessary arguments to be passed correctly
- SonarCloud maintainability issues corrected in Test classes

## Version 0.0.2 - Date: July 13, 2025
- Implementation of configuration items

## Version 0.0.1 - Date: July 12, 2025
- Initial release for NOAA weather data (METAR and TAF)

---

## Future Roadmap

### Phase 2: Source-Agnostic Models (Planned)
- Migrate NOAA models from legacy to weather-common
- Define universal weather data interfaces
- Create adapter pattern for multiple data sources

### Phase 3: Universal Ingestion (Planned)
- Implement multi-source data collectors
- Build S3 upload pipeline
- Add AWS integration

### Phase 4: Storage & Processing (Planned)
- Snowflake data warehouse integration
- DynamoDB for real-time data
- Batch processing pipelines

### Phase 5: Analytics & Serving Layer (Planned)
- Query interface for combined views
- Analytics dashboard
- API endpoints

### Phase 6: Legacy Deprecation (Planned)
- Complete feature parity with legacy
- Migration guide for users
- Archive legacy module
