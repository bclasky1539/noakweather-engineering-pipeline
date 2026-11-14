# NoakWeather Engineering Pipeline - Code Standards

## Overview

This document sets the code-level standards for the NoakWeather Engineering Pipeline: how the
code is organized, how classes and tests are written, and which tools and libraries are used.

The **workflow** (issues, branches, commits, versioning, pull requests, UAT) and the **parser
rules** (rejections, parse issues, logging) are in
[CONTRIBUTING.md](../CONTRIBUTING.md). This document does not repeat them.

## Project Structure

### Repository Layout

```
noakweather-engineering-pipeline/
├── docs/                    # Project documentation
├── glue-jobs/               # AWS Glue PySpark ETL (Bronze → Silver) and tools/
├── noakweather-legacy/      # Legacy application, frozen at v0.0.5 (never modified)
├── noakweather-platform/    # Multi-module Maven platform (see below)
├── scripts/                 # Build, test, packaging and version scripts
│   └── uat/                 # UAT and discovery scripts
└── sql/                     # Athena DDL: bronze/, silver/
```

### Platform Packages

Package-level only: individual classes change too often to list here.

```
noakweather-platform/
├── src/main/resources/log4j2.xml           # Master logging configuration (copied into each module)
├── weather-common/                         # weather.*
│   ├── exception/                          # Shared exceptions (see Error Handling)
│   ├── model/                              # WeatherData hierarchy: NoaaWeatherData, NoaaMetarData, NoaaTafData
│   │   ├── components/                     # Main-body components: Wind, Visibility, Temperature, Pressure, ...
│   │   │   └── remark/                     # Remark components, with grouped sub-packages:
│   │   │       ├── ceilingremarks/
│   │   │       ├── maintenanceremarks/
│   │   │       ├── pressureremarks/
│   │   │       ├── visibilityremarks/
│   │   │       └── windremarks/
│   │   └── enums/                          # Enumerations (SkyCoverage, PressureUnit, ParseIssueKind, ...)
│   ├── service/                            # Shared service, parser and validation interfaces
│   └── utils/                              # Shared utilities
├── weather-ingestion/                      # weather.ingestion.*
│   ├── config/                             # Source configuration (e.g. NOAA endpoints)
│   └── service/                            # S3 upload, speed-layer processing
│       └── source/noaa/                    # NOAA METAR/TAF ingestion apps, orchestrators and client
├── weather-processing/                     # weather.processing.*
│   ├── config/                             # Parser configuration
│   ├── parser/common/                      # ParseResult, ParserException, parser interface
│   ├── parser/noaa/                        # NOAA METAR/TAF parsers, group parsing engine, RegExprConst
│   └── service/                            # Universal parser service
├── weather-storage/                        # weather.storage.*
│   ├── config/                             # DynamoDB configuration and table/GSI definitions
│   ├── exception/                          # Storage exceptions
│   ├── repository/                         # Repository abstraction
│   │   ├── dynamodb/                       # DynamoDB repository and mapper
│   │   └── snowflake/                      # Snowflake repository (planned)
│   ├── service/                            # Batch-layer processing
│   ├── examples/                           # Demo code (excluded from coverage)
│   └── tools/                              # Operational tools, e.g. GSI deployment (excluded from coverage)
├── weather-analytics/                      # Placeholder module
└── weather-infrastructure/                 # Placeholder module (AWS CDK planned)
```

### Module Responsibilities

**weather-common**
- Domain model shared by every module: the `WeatherData` hierarchy and its components
- Shared exceptions and enumerations
- Shared service, parser and validation interfaces

**weather-ingestion** (Speed Layer)
- Fetches METAR and TAF reports from NOAA
- Uploads raw text and parsed JSON to the Bronze layer in S3

**weather-processing** (Batch Layer)
- METAR and TAF parsing, built on one group parsing engine
- Universal parser service

**weather-storage** (Serving Layer)
- Repository abstraction, with DynamoDB (time-bucket GSI) implemented and Snowflake planned
- Batch-layer processing

**weather-analytics**, **weather-infrastructure**
- Reserved for the analytics/serving layer and AWS CDK infrastructure. Both are in the reactor
  but have no sources yet.

### Cross-Module Dependencies

Dependencies flow downward:

```
weather-analytics ──────┐
weather-storage ────────┼──> weather-processing ──┐
weather-ingestion ──────┘                         ├──> weather-common
weather-infrastructure ───────────────────────────┘
```

- Every module may depend on `weather-common`; `weather-common` depends on no other module.
- No circular dependencies.
- Cross-module communication goes through the types and interfaces in `weather-common`.

## Design Principles

- **Domain model first.** Model the weather element (a record with validated fields) before
  writing the pattern or handler that fills it.
- **One parsing mechanism.** METAR main body, remarks and TAF groups are all parsed by the same
  group parsing engine; see [CONTRIBUTING.md](../CONTRIBUTING.md#parsers-weatherprocessingparsernoaa).
  New report sources should reuse it rather than add a parser loop of their own.
- **Immutable once parsed.** Parsed data is immutable: records for components, defensive copies
  for collections.
- **Bad data is never stored as good data.** A value that fails validation is rejected and kept
  visible, not replaced or dropped.
- **Lightweight, explicit dependencies.** No Spring Boot; no dependency injection framework.

## Naming Conventions

### Packages
1. Lowercase only: `weather.storage.repository`
2. Singular nouns: `weather.model`, not `weather.models`
3. Organized by layer, then by feature (`model.components.remark.windremarks`)
4. Keep depth reasonable: 3–5 levels
5. Meaningful names; avoid abbreviations unless standard (METAR, TAF, RVR)

### Classes
- **Records** for value types, named for the weather element: `Wind`, `Temperature`,
  `DirectionSegment`, `PeakWind`.
- **Grouping records** end with the group they collect: `WindRemarks`, `CeilingRemarks`.
- **Data classes** follow the hierarchy: `WeatherData` → `NoaaWeatherData` →
  `NoaaMetarData` / `NoaaTafData`.
- **Services** end with `Service` (`S3UploadService`), **repositories** with `Repository`,
  **configuration** with `Config` or `Configuration`.
- **Exceptions** end with `Exception`.

### Methods and Fields
- Accessors follow the record or field name (`directionSegments()`, `getTemperature()`).
- Use standard meteorological terms consistently, and spell them out in code except for
  well-known abbreviations.
- Units belong in the name when a value has one: `heightFeet`, `speedKnots`, `celsius`.
- Constants are `UPPER_SNAKE_CASE`. Regex patterns end with `_PATTERN`.

## Code Structure

### Class Member Order
1. Static constants
2. Instance fields (or record components)
3. Constructors (for a record: the compact constructor, then convenience constructors)
4. Public methods
5. Package-private methods
6. Private methods
7. Static factory and helper methods
8. Nested types

### Records and Validation
- Validate in the compact constructor. Invalid **report data** throws
  `InvalidObservationException`; a **programming error** (null required argument, inconsistent
  combination) throws `IllegalArgumentException`. See
  [CONTRIBUTING.md](../CONTRIBUTING.md#parsers-weatherprocessingparsernoaa) for how to fill in
  the component, value and reason.
- Copy collections defensively (`List.copyOf`) and normalize an empty collection to the
  record's chosen "absent" value (for example `null`) in one place.
- Convenience getters that only delegate to a nested object are annotated `@JsonIgnore`, so
  Bronze JSON does not contain duplicate keys.

### Methods
- Keep methods short and focused, and use guard clauses to reduce nesting.
- **Sonar cognitive complexity (S3776) is the enforced limit.** When a handler grows, extract
  helpers rather than suppress the rule.
- Prefer few parameters; if a method needs many, introduce a record for them.
- A suppression (for example `java:S5843` on a complex regex) is allowed only with a comment
  saying why the complexity is needed.

### Regular Expressions
- See [CONTRIBUTING.md](../CONTRIBUTING.md#general): end groups with a lookahead such as
  `(?=\s|$)`, use possessive quantifiers, and keep shape in the pattern and value checks in the
  handler.
- Document each pattern in `RegExprConst` with examples, and reference the issue that shaped
  it (`#118`, `#179`).

## Error Handling

### Exceptions in Use

| Exception | Module | Purpose |
|-----------|--------|---------|
| `InvalidObservationException` | weather-common | Report data that is invalid or cannot be interpreted. Expected: becomes a REJECTED parse issue |
| `WeatherParseException` | weather-common | Parsing errors |
| `WeatherServiceException` | weather-common | Service-layer errors |
| `ErrorType` | weather-common | Enumeration of error types |
| `ParserException` | weather-processing | Parser errors |
| `RepositoryException` | weather-storage | Repository-layer errors |
| `WeatherDataMappingException` | weather-storage | Errors mapping weather data to and from storage |

### Rules
- **Rejection is not failure.** Bad report data is expected: it is rejected, recorded in
  `parseIssues`, and logged at DEBUG. Any other exception during parsing is a parser bug and is
  logged at ERROR. Never log bad report data at WARN.
- **Keep the cause.** When converting text fails, wrap the original exception
  (`new InvalidObservationException(component, value, reason, e)`).
- **Messages carry context.** Name the field and include the rejected text, so the parse issue
  or log line is enough to find the problem.

## Testing Standards

### Organization
- One test class per class: `ClassNameTest`, in the same package.
- Test methods are named `testMethodOrBehavior_Case` (for example
  `testTempDewpoint_WrongDigitCount_Rejected`) with a `@DisplayName` stating the expected
  behavior in plain words, and the issue number when the test guards a fix (`(#180)`).
- Use the Arrange / Act / Assert structure.

### Style
- **AssertJ** for assertions (`assertThat(...)`, `singleElement()`, `extracting(...)`,
  `satisfies(...)`).
- **Parameterized tests** (`@CsvSource`, `@ValueSource`) for families of similar inputs. Add a
  row rather than a new method when a case fits an existing table.
- **Real report text** for parser tests, including the live report that exposed a problem.
- Test both directions of a fix: the case it rejects and the valid neighbors it must still
  accept.

### Test Types
- **Unit tests** for records and helpers, including their validation.
- **Parser tests** that parse full reports and assert on the result, its
  `unparsedMainBody`/`freeText`, and its `parseIssues`.
- **Golden-output tests** (`NoaaMetarParserGoldenOutputTest`, `NoaaTafParserGoldenOutputTest`):
  a permanent regression guard over a fixed set of real reports. See
  [CONTRIBUTING.md](../CONTRIBUTING.md) for re-recording.
- **Logging tests** with `LogCapture` for code that only runs when DEBUG is enabled.
- **Integration tests**: WireMock for HTTP sources, Testcontainers with LocalStack for DynamoDB.
  These need Docker and run in `scripts/wetht.sh`.

### Test Doubles
Prefer real objects, WireMock and Testcontainers. Mockito is available in `weather-ingestion`
and `weather-storage` for collaborators that cannot be run for real; parser and model tests do
not use mocks.

## Documentation Standards

### Javadoc
- All public classes and methods have Javadoc; package-private methods where useful; private
  methods for non-obvious logic only.
- Explain **why**, not only what: a rule that came from a real report or an issue says so, with
  the issue number (`(#180)`).
- Document examples of the report text a pattern, handler or record accepts and rejects.
- `@throws InvalidObservationException` on methods that reject report data.

```java
/**
 * Parse a temperature or dewpoint value from METAR format.
 *
 * @param sign      the sign indicator ("M", "-", or null)
 * @param digits    the temperature digits (e.g., "05", "22")
 * @param component the field being parsed, for a rejection
 * @return temperature in Celsius, or null if missing/unknown
 * @throws InvalidObservationException if the value does not have exactly two digits (#180)
 */
```

### Project Documentation
- Update `README.md` and the relevant document in `docs/` when a change affects how the project
  is built, run or understood.
- Link to source files (`sql/`, `glue-jobs/`) instead of copying them into documents.

## Quality Metrics

- **SonarCloud quality gate**, enforced in CI: at least 80% coverage on new code, no new issues,
  A ratings. The project aims for about 98% overall coverage and 100% of changed lines, with no
  duplication.
- **JaCoCo** produces coverage per module; `diff-cover` checks changed lines locally.
- **Excluded from coverage:** demo and operational code (`weather.storage.examples`,
  `weather.storage.tools`) and test-only models.
- **SonarLint** in the IDE, connected to the SonarCloud project, for feedback before CI.

## Dependencies and Libraries

### In Use
- **Logging:** SLF4J API backed by Log4j2 (`log4j-slf4j2-impl`), configured by the shared
  `log4j2.xml`.
- **Testing:** JUnit 5, AssertJ, WireMock, Testcontainers (with LocalStack); Mockito in
  `weather-ingestion` and `weather-storage`.
- **AWS:** AWS SDK for Java v2 (S3, DynamoDB).
- **Build:** Maven, with versions managed in the parent `pom.xml`.

### Guidelines
- Add a dependency only when it is truly needed, and manage its version in the parent POM.
- Check a new dependency for known vulnerabilities (for example on MVNRepository) before
  adding it. A library with open vulnerabilities is not added (JavaTuples was rejected for this
  reason; Java records replace it).
- No Spring Boot.
- Ensure licenses are compatible with Apache 2.0.

## Continuous Integration

- **GitHub Actions:** `maven.yml` builds and tests on Java 17, 21 and 25; `sonarcloud.yml` runs
  the SonarCloud analysis and enforces the quality gate.
- A pull request merges only when CI passes on every Java version and the quality gate passes
  with no new issues.``
- Before pushing, run `scripts/wethb.sh` and `scripts/wetht.sh` and check new-code coverage;
  see [CONTRIBUTING.md](../CONTRIBUTING.md).

## Future Data Sources

The project plans more data sources (for example Weather.gov) beyond NOAA METAR/TAF. A new
source:
- adds its ingestion under `weather.ingestion.service.source.<source>`;
- maps into the shared `WeatherData` model in `weather-common`;
- parses report groups with the shared group parsing engine rather than its own loop.
