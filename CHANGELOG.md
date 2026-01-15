# Changelog

All notable changes to the NoakWeather Engineering Pipeline project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Version 1.11.0-SNAPSHOT - January 15, 2026

#### Weather Common Package Structure Refactoring & Code Quality Improvements

**Changed:**
- **Package Structure Reorganization** (weather-common)
    - Moved `WeatherParseException` from `weather.service` to `weather.exception` package
    - Improved package organization and discoverability
    - Consistent exception placement with `ErrorType` and `WeatherServiceException`

- **Code Quality Improvements** (weather-common)
    - **Visibility.java**:
        - Replaced `default` branches in conversion methods with `IllegalStateException` for unknown units
        - Collapsed redundant `if` block in `isIFR()` method
        - Improved conversion methods: `toMeters()`, `toStatuteMiles()`, `toKilometers()`
    - **NoaaTafData.java**:
        - Removed redundant field initialization for `forecastPeriods` (handled by constructors)
        - Added `@Serial` annotation to `serialVersionUID`
    - **IndexedLinkedHashMap.java**:
        - Added `@Serial` annotations to serialization-related members
        - Replaced iteration loop with `addAll()` in `readObject()` method (more efficient)
        - Enhanced JavaDoc with comprehensive `@param` descriptions for class and methods
    - **Exception Handling**:
        - Updated imports across affected classes after package reorganization

- **Test Improvements** (weather-common)
    - **VisibilityTest.java**:
        - Added tests for unknown unit handling (coverage for defensive code)
        - Improved test clarity and conciseness
    - **NoaaTafDataTest.java**:
        - Added `testSetIssueTime()` to improve coverage (0% → 100%)
        - Added null handling and overwrite scenarios

**Technical Improvements:**
- **Java 17+ Best Practices**:
    - Improved serialization documentation and tool support
- **Code Efficiency**:
    - Bulk collection operations over iteration loops
    - Eliminated redundant object creation
- **Documentation**:
    - Comprehensive JavaDoc additions for `IndexedLinkedHashMap`
    - Better exception documentation with `@throws` tags

**Fixed:**
- Package structure inconsistency (exception in wrong package)

**Testing:**
- All tests passing in weather-common module
- Maintained high test coverage (97%+ instruction coverage)
- No breaking changes to public APIs

**Notes:**
- Refactoring focused on code quality and maintainability
- All changes are internal improvements with no API changes
- Prepares codebase for upcoming weather-storage integration

### Version 1.10.0-SNAPSHOT - January 13, 2026

#### NOAA Aviation Weather Ingestion System - Complete Implementation

**Added:**
- **NOAA Aviation Weather Ingestion System**
    - Abstract base classes (`AbstractNoaaIngestionOrchestrator`, `AbstractNoaaIngestionApp`) to eliminate code duplication
    - `NoaaConfiguration` for TG FTP endpoint management and URL building
    - `NoaaAviationWeatherClient` for fetching METAR and TAF data from NOAA TG FTP service
    - `MetarIngestionApp` and `TafIngestionApp` command-line applications
    - Three execution modes: batch (one-time), scheduled (periodic), and interactive (command-line interface)
    - Scheduled periodic ingestion with configurable intervals using `ScheduledExecutorService`
    - Comprehensive test suite with 139 tests (100% pass rate)
    - Station code validation (3-4 alphabetic characters)
    - Retry logic with exponential backoff for NOAA API calls
    - Batch processing support for multiple stations

**Changed:**
- **Refactored `SpeedLayerProcessor`** to be source-agnostic
    - Now works with any `WeatherData` source, not just NOAA
    - Generic processing pipeline for future data sources
- Improved async testing patterns using Mockito's `timeout()` instead of `Thread.sleep()`

**Fixed:**
- Non-deterministic parallel processing test in `SpeedLayerProcessorTest`
- 404 handling in NOAA client (returns null instead of throwing exception)

**Technical Improvements:**
- **Design Patterns**: Template Method, Strategy, Builder, Dependency Injection

**Testing:**
- Added 9 comprehensive test suites for NOAA components
- Total of 139 passing tests (0 failures, 0 errors)
- Coverage improvements:
    - `schedulePeriodicIngestion`: 0% → ~90%
    - Overall orchestrator: 78% instruction coverage

**Documentation:**
- Added comprehensive Javadoc for all public APIs
- Inline comments explaining complex logic
- Test documentation with clear arrange/act/assert structure

### Version 1.9.0-SNAPSHOT - January 10, 2026

#### TAF Parser Implementation & Architecture Refactoring

**Added:**
- **TAF Parser Components** (Complete Terminal Aerodrome Forecast parsing)
    - `NoaaTafParser` - Full TAF report parser with change group support
    - Header parsing: station ID, issue time, validity period, modifiers (AMD/COR)
    - Base forecast: Initial conditions for validity period
    - Change groups: FM (From), TEMPO (Temporary), BECMG (Becoming), PROB (Probability)
    - Temperature forecasts: TX/TN with time specifications
    - Multi-pass parsing for order-independent processing
    - Pattern Infrastructure

- **Wind Enhancements** (weather-common)
    - Distinguishes between VRB (unpredictable) and variability range (180V240)
    - Comprehensive test suite for variable direction detection

**Enhanced:**
- **Architecture Refactoring** (weather-common)
    - **NoaaWeatherData hierarchy refactored**:
        - Moved `WeatherConditions` from subclasses → `NoaaWeatherData` (single source of truth)
        - Fixed architectural flaw: `getSkyConditions()` now delegates to `conditions.skyConditions()`
        - Eliminated duplicate storage of sky conditions and present weather
        - `NoaaMetarData` and `NoaaTafData` now extend consistent parent
    - **equals/hashCode improvements**:
        - Now use business fields only (exclude auto-generated IDs and timestamps)
        - Proper `super.equals()` calls in subclasses
        - Consistent with domain-driven design principles
    - **Stream API modernization**:
        - Updated to Java 16+ `.toList()` (replaces `.collect(Collectors.toList())`)
        - Cleaner, more concise code
    - **Temperature component**:
        - Fixed `dewpointCelsius()` accessor (was incorrectly named `dewPointCelsius()`)
    - **Wind factory methods**:
        - Improved naming consistency across factory methods
        - Added proper support for calm and variable wind

- **Pattern Registry Refactoring** (weather-processing)
    - **Renamed classes** for clarity:
        - `MetarPatternRegistry` → `NoaaAviationWeatherPatternRegistry`
        - `MetarPatternHandler` → `NoaaAviationWeatherPatternHandler`
    - Rationale: Both METAR and TAF are aviation weather reports sharing common patterns
    - Registered weather element patterns (wind, visibility, sky conditions)
    - TAF change group patterns handled separately (not in registry)

- **NoaaAviationWeatherParser Base Class** (weather-processing)
    - **handleWind()** refactored for reduced cognitive complexity:
        - Extracted parsing methods: `parseWindDirection()`, `parseWindSpeed()`, `parseWindGust()`, `parseWindUnit()`
        - Extracted creation method: `createWind()` with clear decision logic
        - Extracted logging method: `logWindData()`
        - Improved from ~15 complexity → ~3-4 per method
    - **Calm wind detection**: `00000KT` correctly creates `Wind.calm()`
    - **Variable wind support**: `VRB05KT` correctly creates `Wind.variable(speed, unit)`
    - **Null safety**: Uses `"VRB".equals(directionStr)` pattern (prevents NPE)

- **NoaaTafParser** (weather-processing)
    - **Validity period parsing**:
        - Fixed month/year wrap-around logic (removed `&& hour == 24` restriction)
        - Now correctly handles periods crossing month boundaries (Dec 31 → Jan 1)
        - Supports 24:00 notation as next day 00:00
    - **Forecast period parsing**:
        - Multi-pass loop structure for change groups
        - Proper handling of PROB periods (fixed validity period consumption issue)
        - Each change group creates separate `ForecastPeriod` object
    - **Temperature forecast parsing**:
        - Fixed pattern to use `\\s*` instead of `\\s+` (handles end-of-string)
        - Correctly parses both TX and TN in sequence

- **RegExprConst Pattern Updates** (weather-processing)
    - Split monolithic pattern into focused patterns:
        - `STATION_DAY_TIME_VALTMPER_PATTERN` split into `STATION_AND_ISSUE_TIME_PATTERN` + `VALIDITY_PERIOD_PATTERN`
        - Improved single responsibility and reusability

**Testing:**
- **weather-common**: 1,792 tests (maintained from v1.7.0)
    - NoaaWeatherData refactored tests: 100% coverage
    - NoaaMetarData refactored tests: 100% coverage
    - NoaaTafData refactored tests: 100% coverage
    - Wind variable direction tests: 6 new tests (100% coverage)
    - CAVOK visibility tests: 3 new tests (100% coverage)
    - Overall coverage: **97% instruction, 91% branch**

- **weather-processing**: 1,097 tests (+484 new TAF tests)
    - NoaaTafParser: 70+ comprehensive tests
        - Header parsing (station, issue time, validity, modifiers)
        - Base forecast parsing (wind, visibility, sky conditions)
        - Change group parsing (FM, TEMPO, BECMG, PROB)
        - Temperature forecast parsing (TX/TN)
        - Month/year wrap-around scenarios
        - Real-world TAF examples
    - NoaaMetarParser: All existing tests passing + 3 fixes
    - Pattern registry tests: Updated for renamed classes
    - Overall coverage: **87% instruction, 77% branch**
    - Missing coverage is defensive code (null checks, exception handlers)

**TAF Components Now Supported:**
1. **Header**:
    - Report type (TAF)
    - Modifiers (AMD, COR)
    - Station ID
    - Issue time (with date/time prefix support)
    - Validity period (with month/year wrap-around)

2. **Base Forecast**:
    - Wind (including calm and variable)
    - Visibility
    - Present weather
    - Sky conditions
    - All inherited from NoaaAviationWeatherParser

3. **Change Groups**:
    - FM (From) - Permanent change at exact time
    - TEMPO - Temporary fluctuations
    - BECMG - Gradual change
    - PROB30/PROB40 - Probabilistic conditions

4. **Temperature Forecasts**:
    - TX (Maximum temperature)
    - TN (Minimum temperature)
    - With date/time specifications

**Technical Details:**
- Parametric polymorphism: `NoaaAviationWeatherParser<T extends NoaaWeatherData>`
- Generics ensure type safety between parser and data model
- Multi-pass parsing ensures order-independent processing
- Comprehensive validation with meaningful error messages
- Real-world TAF examples validated in tests
- Defensive coding with extensive null checks

**Architecture Benefits:**
- **DRY Principle**: Weather parsing logic shared between METAR and TAF
- **Single Source of Truth**: WeatherConditions stored once in parent class
- **Type Safety**: Generics prevent type errors at compile time
- **Extensibility**: Easy to add new NOAA report types (SPECI, PIREP, etc.)
- **Maintainability**: Clear separation of concerns, consistent patterns

**Migration Notes:**
- NoaaWeatherData hierarchy changed (WeatherConditions moved to parent)
- Pattern registry classes renamed (Metar → NoaaAviationWeather)
- Wind class has new `hasVariableDirection()` method
- Temperature accessor renamed (`dewPointCelsius()` → `dewpointCelsius()`)
- Tests updated to use `hasVariableDirection()` instead of checking nulls

**Notes:**
- TAF parser implementation complete and production-ready
- Architecture refactoring improves code quality and maintainability
- Both METAR and TAF parsers achieve >85% test coverage
- Ready for integration with weather-ingestion pipeline

### Version 1.8.0-SNAPSHOT - December 29, 2025

#### Enhanced METAR Remarks Parser - Phase 2 Complete

**Added:**
- **METAR Remarks Parser Components** (9 additional remark types implemented)
    - Weather Events Begin/End Times (RAB, SNE, FZRAB, etc. with timestamps)
    - Thunderstorm/Cloud Locations (TS, CB, TCU with direction and movement)
    - Pressure Tendency (3-hour tendency with WMO codes)
    - 6-Hour Max/Min Temperature (1sTTT, 2sTTT formats)
    - 24-Hour Max/Min Temperature (4sTTTsTTT format)
    - Variable Ceiling (CIG minVmax)
    - Ceiling at Second Site (CIG height with location)
    - Obscuration Layers (FEW FG, SCT FU, etc.)
    - Cloud Type in Oktas (SC1, AC2, CI with intensity/movement)
    - Automated Maintenance Indicators (RVRNO, PWINO, PNO, FZRANO, TSNO, VISNO, CHINO, $)

- **METAR Main Body Component**
    - NOSIG (No Significant Change) indicator parsing

- **Value Object Components** (weather-common)
    - `WeatherEvent` record - Weather phenomenon begin/end times
        - Weather code with optional intensity
        - Begin time (hour/minute, optional hour)
        - End time (hour/minute, optional hour)
        - Query methods (`hasBeginTime()`, `hasEndTime()`, `getDuration()`)
        - Human-readable summaries and descriptions
        - Support for chained events (RAB15E30SNB30)

    - `ThunderstormLocation` record - Thunderstorm and cloud locations
        - Cloud type (TS, CB, TCU, ACC, CBMAM, VIRGA)
        - Location qualifier (OHD, VC, DSNT, DSIPTD, TOP, TR)
        - Direction (N, NE, E, SE, S, SW, W, NW)
        - Direction range (N-NE, SW-W, etc.)
        - Movement direction
        - Query methods (`isOverhead()`, `isDistant()`, `isMoving()`)
        - Comprehensive descriptions

    - `PressureTendency` record - 3-hour pressure change data
        - WMO Code 0200 tendency codes (0-8)
        - Pressure change in hPa (tenths precision)
        - Tendency descriptions (Increasing, Decreasing, Steady, etc.)
        - Query methods (`isIncreasing()`, `isDecreasing()`, `isSteady()`)
        - Factory method `fromMetar()` with validation

    - `VariableCeiling` record - Ceiling height variations
        - Minimum and maximum heights in feet
        - Spread calculation
        - Factory method `fromHundreds()` for METAR format
        - Validation (min ≤ max, positive values)

    - `CeilingSecondSite` record - Ceiling at alternate location
        - Height in feet
        - Optional location (RWY designator)
        - Factory method `fromHundreds()` for METAR format
        - Query methods (`hasLocation()`)

    - `ObscurationLayer` record - Atmospheric obscuration
        - Coverage (FEW, SCT, BKN, OVC)
        - Phenomenon (FG, FU, HZ, DU, SA, VA, PY)
        - Height in feet
        - Factory method `fromHundreds()` for METAR format
        - Validation and descriptions

    - `CloudType` record - Cloud type observations in oktas
        - Cloud type code (CI, CC, CS, AC, AS, NS, SC, ST, CU, CB)
        - Optional oktas (0-8)
        - Optional intensity (FEW, BKN, LYR, etc.)
        - Optional location (OHD, DSNT, VC)
        - Optional movement direction
        - Query methods (`hasOktas()`, `hasIntensity()`, `isMoving()`)
        - Comprehensive validation

    - `AutomatedMaintenanceIndicator` record - Station maintenance status
        - 7 indicator types (RVRNO, PWINO, PNO, FZRANO, TSNO, VISNO, CHINO)
        - Maintenance check indicator ($)
        - Optional location qualifier (runway or direction)
        - Static factories for each type
        - Query methods for each indicator type
        - Location handling (runway designators, cardinal directions)

**Enhanced:**
- **NoaaMetarRemarks Model** (weather-common)
    - Added 14 new fields to record:
        - `List<WeatherEvent> weatherEvents`
        - `List<ThunderstormLocation> thunderstormLocations`
        - `PressureTendency pressureTendency`
        - `Temperature sixHourMaxTemperature`
        - `Temperature sixHourMinTemperature`
        - `Temperature twentyFourHourMaxTemperature`
        - `Temperature twentyFourHourMinTemperature`
        - `VariableCeiling variableCeiling`
        - `CeilingSecondSite ceilingSecondSite`
        - `List<ObscurationLayer> obscurationLayers`
        - `List<CloudType> cloudTypes`
        - `Boolean maintenanceRequired`
        - `List<AutomatedMaintenanceIndicator> automatedMaintenanceIndicators`
        - `String freeText` (unparsed remarks)
    - Total parameters: 28 (up from 14)
    - Builder pattern with collection adders (`addWeatherEvent()`, etc.)
    - Custom `maintenanceRequired()` getter (returns `false` for `null`)
    - Enhanced `isEmpty()` checks all 28 fields
    - Maintained 99% test coverage (2,526 tests)

- **NoaaMetarData Model** (weather-common)
    - Added `noSignificantChange` boolean field for NOSIG indicator
    - Included in `equals()` and `hashCode()`
    - Getter: `isNoSignificantChange()`

- **NoaaMetarParser** (weather-processing)
    - Added 9 sequential handler methods:
        - `handleWeatherEventsSequential()` - Parses weather begin/end times
        - `handleThunderstormLocationSequential()` - Parses cloud locations
        - `handlePressureTendencySequential()` - Parses 3-hour pressure tendency
        - `handle6HourMaxMinTemperatureSequential()` - Parses 1/2 groups
        - `handle24HourMaxMinTemperatureSequential()` - Parses 4 group
        - `handleVariableCeilingSequential()` - Parses CIG minVmax
        - `handleCeilingSecondSiteSequential()` - Parses CIG with location
        - `handleObscurationSequential()` - Parses obscuration layers
        - `handleCloudTypeSequential()` - Parses cloud types in oktas
        - `handleAutomatedMaintenanceSequential()` - Parses maintenance indicators
    - Added main body handler:
        - `handleNoSigChange()` - Parses NOSIG indicator
    - Helper methods:
        - `parseWeatherEventFromExistingPattern()` - Weather event extraction
        - `parseThunderstormLocationFromMatcher()` - Cloud location extraction
        - `parsePressureTendencyFromMatcher()` - Pressure tendency extraction
        - `parse6HourTemperatureFromMatcher()` - 6-hour temp extraction
        - `parse24HourMaxTemperatureFromMatcher()` - 24-hour max temp
        - `parse24HourMinTemperatureFromMatcher()` - 24-hour min temp
        - `extractCloudTypeFromMatcher()` - Cloud type extraction
        - `processAutomatedMaintenance()` - Maintenance indicator processing
        - `parseTimeDigits()` - Time parsing (2 or 4 digits)
        - `buildWeatherCodeFromExistingGroups()` - Weather code construction
    - Updated `handleRemarks()` multi-pass loop with all new handlers
    - Updated `handlePattern()` switch with `"noSigChange"` case

- **RegExprConst** (weather-processing)
    - Updated `AUTOMATED_MAINTENANCE_PATTERN`:
        - Now matches all 7 types plus $ indicator
        - Supports runway designators (RWY06, RY11)
        - Supports cardinal directions (N, SE, NW)
        - Optional location handling
        - Word boundary handling for $ indicator

**Testing:**
- **weather-common**: 2,526 tests (+734 new tests from Phase 1)
    - WeatherEvent: 75 tests (100% instruction coverage)
    - ThunderstormLocation: 15 tests (100% coverage)
    - PressureTendency: 89 tests (100% coverage)
    - VariableCeiling: 14 tests (100% coverage)
    - CeilingSecondSite: 15 tests (100% coverage)
    - ObscurationLayer: 18 tests (100% coverage)
    - CloudType: 136 tests (100% coverage)
    - AutomatedMaintenanceIndicator: 34 tests (99% instruction, 96% branch)
    - NoaaMetarRemarks: Enhanced tests for all new fields (99% coverage)
    - All value objects achieve 99-100% coverage

- **weather-processing**: 929 tests (+316 new tests from Phase 1)
    - Weather Events parsing: 9 tests with chained events
    - Thunderstorm/Cloud locations: 8 tests with movement
    - Pressure Tendency: 5 tests (all WMO codes)
    - 6-Hour Max/Min Temperature: 6 tests
    - 24-Hour Max/Min Temperature: 4 tests
    - Variable Ceiling: 3 tests
    - Ceiling Second Site: 4 tests
    - Obscuration Layers: 7 tests (repeating pattern)
    - Cloud Types: 12 tests (oktas, intensity, movement)
    - Automated Maintenance: 14 tests (all types, locations, $)
    - NOSIG: 4 tests (main body parsing)
    - Integration tests covering complex real-world METARs
    - Parser coverage: 82% instruction, 73% branch

**METAR Components Now Supported:**

*Main Body:*
1. Station ID & observation time
2. Report type & modifiers
3. Wind (direction, speed, gusts, variability)
4. Visibility (statute miles, meters, special conditions)
5. Runway Visual Range with CLRD flag
6. Present Weather (intensity, descriptor, precipitation, obscuration)
7. Sky Conditions (FEW/SCT/BKN/OVC/VV with heights and cloud types)
8. Temperature/Dewpoint
9. Altimeter (multiple formats: A/Q/QNH/INS)
10. **NOSIG (No Significant Change)** (NEW)

*Remarks Section (21 types):*
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
13. **Weather Events Begin/End (RAB, SNE, etc.)** (NEW)
14. **Thunderstorm/Cloud Locations (TS, CB, TCU)** (NEW)
15. **3-Hour Pressure Tendency (5TCCC)** (NEW)
16. **6-Hour Max/Min Temperature (1/2sTTT)** (NEW)
17. **24-Hour Max/Min Temperature (4sTTTsTTT)** (NEW)
18. **Variable Ceiling (CIG minVmax)** (NEW)
19. **Ceiling Second Site (CIG height [LOC])** (NEW)
20. **Obscuration Layers (FEW FG, etc.)** (NEW)
21. **Cloud Types in Oktas (SC1, CI, etc.)** (NEW)
22. **Automated Maintenance Indicators (RVRNO, $, etc.)** (NEW)
23. **Free Text (unparsed remarks)** (NEW)

**Technical Details:**
- Multi-pass parsing ensures order-independent remark processing
- Comprehensive validation in all value objects
- Reused existing patterns from `RegExprConst` where possible
- Complex regex patterns for weather events and cloud locations
- Real-world METAR examples validated in tests
- Defensive coding with extensive null checks and exception handling

**Notes:**
- **Phase 2 of remarks parsing complete** (21 total remark types)
- **Main body parsing complete** with NOSIG support
- Strong architectural foundation
- All implementations follow consistent patterns established in Phase 1

### Version 1.7.0-SNAPSHOT - December 18, 2025

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
