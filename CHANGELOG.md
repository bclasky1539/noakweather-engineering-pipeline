# Changelog

All notable changes to the NoakWeather Engineering Pipeline project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Version 1.0.0-SNAPSHOT - Date: October 15, 2025

#### Major Architecture Rewrite - Day 1: Multi-Module Platform Structure

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
