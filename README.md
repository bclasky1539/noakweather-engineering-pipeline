# NoakWeather Engineering Pipeline

A multi-source weather data engineering platform built on Lambda Architecture principles, designed to collect, process, store, and analyze aviation weather data from multiple sources including NOAA, AWS, and potentially other providers.

## Build Status

[![Java CI with Maven](https://github.com/bclasky1539/noakweather-engineering-pipeline/actions/workflows/maven.yml/badge.svg)](https://github.com/bclasky1539/noakweather-engineering-pipeline/actions/workflows/maven.yml)
[![Sonar verify](https://github.com/bclasky1539/noakweather-engineering-pipeline/actions/workflows/sonarcloud.yml/badge.svg)](https://github.com/bclasky1539/noakweather-engineering-pipeline/actions/workflows/sonarcloud.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=bclasky1539_noakweather-engineering-pipeline&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=bclasky1539_noakweather-engineering-pipeline)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=bclasky1539_noakweather-engineering-pipeline&metric=coverage&branch=main)](https://sonarcloud.io/summary/new_code?id=bclasky1539_noakweather-engineering-pipeline)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=bclasky1539_noakweather-engineering-pipeline&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=bclasky1539_noakweather-engineering-pipeline)
[![License](https://img.shields.io/github/license/bclasky1539/noakweather-engineering-pipeline)](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/LICENSE)

## Project Structure

This project consists of two major components:

### noakweather-platform (New Multi-Module Architecture)
Source-agnostic weather data platform with Lambda Architecture implementation:

- **weather-common**: Shared models and interfaces (source-agnostic)
- **weather-ingestion**: Universal data collection and S3 upload (Speed Layer)
- **weather-processing**: Stream and batch processing (Batch Layer)
- **weather-storage**: Multi-backend storage (Snowflake, DynamoDB, S3) with Phase 4 GSI implementation
- **weather-analytics**: Universal analytics and reporting (Serving Layer)
- **weather-infrastructure**: AWS CDK infrastructure as code

### noakweather-legacy
Original NOAA-specific METAR/TAF decoder (maintained for reference and gradual migration)

## Architecture

The platform implements **Lambda Architecture** to handle both real-time and batch processing:

- **Speed Layer**: Real-time ingestion of weather data from multiple sources → S3 → DynamoDB with time-bucket GSI
- **Batch Layer**: Historical data processing and reprocessing
- **Serving Layer**: Unified query interface combining real-time and batch views

### Technology Stack

- **Java 17+**: Modern Java features and performance
- **Maven**: Multi-module build management
- **AWS Services**: S3, Lambda, DynamoDB, CloudWatch
- **Snowflake**: Data warehouse for analytics
- **JUnit 5**: Comprehensive testing framework
- **JaCoCo**: Code coverage analysis
- **SonarQube**: Code quality and security scanning
- **Log4j2/Logback**: Enterprise logging with centralized configuration
- **GitHub Actions**: CI/CD pipeline
- **LocalStack**: Local DynamoDB testing with Testcontainers

## What is METAR?

METAR (Meteorological Aerodrome Report) is a current weather report format used in aviation. Typical METAR reports contain information such as location, report issue time, wind, visibility, clouds, weather phenomena, temperature, dewpoint, and atmospheric pressure.

**Example METAR:**
```
2021/12/28 01:52 KCLT 280152Z 22006KT 10SM BKN240 17/13 A2989 RMK AO2 SLP116 T01720133
```

## What is TAF?

TAF (Terminal Aerodrome Forecast) is a weather forecast report format used in aviation. TAF reports provide trends and changes in visibility, wind, clouds, and weather over periods of time.

**Example TAF:**
```
2021/12/28 02:52 TAF AMD KCLT 280150Z 2802/2906 21006KT P6SM SCT040 BKN150 FM281100 22005KT P6SM SCT008
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- AWS CLI configured (for deployment)
- Snowflake account (for data warehouse features)

### Building the Project

```bash
# Clone the repository
git clone https://github.com/bclasky1539/noakweather-engineering-pipeline.git
cd noakweather-engineering-pipeline

# Build and test legacy module
cd noakweather-legacy
./wethb.sh    # Build
./wetht.sh    # Test with coverage
mvn clean install

# Build and test platform modules
cd ../noakweather-platform
./wethb.sh    # Build
./wetht.sh    # Test with coverage
mvn clean install

# Build entire project from root
cd ..
mvn clean install
```

### Running Tests

```bash
# Run tests with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Code Quality

```bash
# Run SonarQube analysis
mvn clean verify sonar:sonar \
  -Dsonar.organization=bclasky1539 \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.login=$SONAR_TOKEN
```

## Documentation

### Setup Guides

- **[AWS IAM User Setup for DynamoDB](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/AWS_IAM_DYNAMODB_SETUP.md)** - Complete guide for creating AWS IAM users with DynamoDB permissions
    - IAM user creation and permission setup
    - Access key generation and secure storage
    - AWS credentials file configuration
    - Security best practices and troubleshooting

- **[Logging Configuration Setup](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/LOGGING_SETUP.md)** - Centralized logging configuration for multi-module projects
    - Log4j2 master configuration
    - Maven resources plugin setup
    - Environment variable configuration
    - Log rotation and retention policies

### Deployment Guides

- **[Phase 4 GSI Deployment Guide](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/PHASE_4_GSI_DEPLOYMENT_GUIDE.md)** - Zero-downtime DynamoDB GSI deployment
    - Pre-deployment checklist
    - Step-by-step deployment instructions
    - Rollback procedures
    - Performance benchmarks (50x improvement)

### Technical Documentation

- **[Code Standards](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/CODE_STANDARDS.md)** - Comprehensive coding standards and best practices
  - Package organization and architecture principles
  - Naming conventions and code structure
  - Error handling patterns and testing standards
  - Git workflow and quality metrics
  - Continuous integration requirements

- **[Weather Format References](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/WEATHER_FORMAT_REFERENCES.md)** - METAR/TAF format specifications
  - Official ICAO and FAA standards
  - Complete format structure diagrams
  - Weather element reference guide
  - Live data feeds and validation tools
  - Parsing considerations and implementation notes

- **Architecture Decisions** - Lambda Architecture design patterns
  - Speed Layer: Real-time data ingestion
  - Batch Layer: Historical data processing
  - Serving Layer: Query interface design

### API Documentation

- **DynamoDB Repository API** (weather-storage module)
  - CRUD operations for weather data
  - Time-bucket GSI query methods
  - Batch operations and statistics
  - Integration with AWS SDK v2

- **Parser API** (weather-processing module)
  - Universal parser interface
  - NOAA METAR/TAF parsers
  - Parse result handling
  - Error handling patterns

## Phase 4 Features (Latest)

### DynamoDB Time-Bucket GSI Implementation

**Performance Improvements:**
- 50x faster time-range queries using `time-bucket-index` GSI
- Hourly time buckets for optimal query performance
- Backward-compatible table scan fallback
- Zero-downtime deployment support

**Technical Details:**
```
GSI Schema:
- Index Name: time-bucket-index
- Partition Key: time_bucket (String, "YYYY-MM-DD-HH")
- Sort Key: observation_time (Number, epoch seconds)
- Projection: ALL
- Billing: On-demand

Query Performance:
- Table Scan: O(n) - ~200ms for 10,000 items
- GSI Query: O(m) - ~4ms for same result (50x faster!)
```

**Deployment Strategy:**
1. Deploy code with GSI support + fallback → Works immediately using table scan
2. Add GSI to production table → ~5 minutes to create
3. Queries automatically switch to GSI → 50x performance improvement
4. Zero downtime throughout entire process

See [Phase 4 Deployment Guide](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/PHASE_4_GSI_DEPLOYMENT_GUIDE.md) for details.

## Development Workflow

This project follows a phased migration approach:

1. **Phase 1** (Complete): Multi-module structure with platform foundation
2. **Phase 2** (Complete): NOAA models and parsers
3. **Phase 3** (Complete): Universal ingestion layer with S3 upload
4. **Phase 4** (Complete): DynamoDB storage with time-bucket GSI and comprehensive testing
5. **Phase 5** (Next): Analytics and serving layer
6. **Phase 6** (Planned): Additional data sources
7. **Phase 7** (Planned): Legacy deprecation

## Running Legacy METAR/TAF Decoder

The legacy decoder retrieves METAR and TAF data from NOAA or local files.

**Parameters:**
- **Type**: `m` (METAR) or `t` (TAF)
- **Source**: 4-letter ICAO code (e.g., `KCLT`) or `file:filename.txt`
- **Print**: `Y` or `N`
- **Logging**: `I` (Info), `W` (Warnings), `D` (Debug)

**Example:**
```bash
cd noakweather-legacy
./weth.sh m KCLT Y I
```
## Running DynamoDB Integration Tools

### Add GSI to AWS Production Table

```bash
cd noakweather-platform/weather-storage

# Add time-bucket-index GSI to production table
mvn exec:java -Dexec.mainClass="weather.storage.tools.AddGSIsToAwsTable"

# Expected output:
# ✓ Table status verified: ACTIVE
# ✓ No existing GSI found (safe to add)
# ✓ Creating time-bucket-index GSI...
# ✓ Waiting for GSI to become ACTIVE...
# ✓ GSI deployment successful!
# ✓ Query performance improved 50x
```

See [AWS IAM User Setup Guide](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/AWS_IAM_DYNAMODB_SETUP.md) for AWS credentials setup.

## Project Statistics

**Current Status (v1.13.0-SNAPSHOT):**
- **Total Tests**: 221 (weather-storage) + additional tests in other modules
- **Code Coverage**: 90%+ overall (DynamoDB repository ~90%, parsers 85%+)
- **Build Time**: ~21 seconds for weather-storage module
- **Lines of Code**: ~15,000+ lines across platform modules
- **Zero Failures**: All tests passing

**Test Infrastructure:**
- LocalStack for DynamoDB testing
- Testcontainers for container management
- JUnit 5 with AssertJ assertions
- Comprehensive integration and unit tests

## Contributing

1. Create a feature branch from `main`
2. Make your changes following the code standards
3. Ensure all tests pass and coverage meets requirements
4. Submit a pull request

## License

Apache License 2.0 - See [LICENSE](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/LICENSE) for details

## Project Status

**Active Development** - Phase 4 Complete, Phase 5 In Progress

### Recent Milestones

**Phase 4 Complete (January 2026)**
- DynamoDB time-bucket GSI implementation
- 50x performance improvement on time-range queries
- Zero-downtime deployment support
- Comprehensive integration test suite (221 tests)
- Production-ready with complete documentation

### Next Milestones

**Phase 5 - Analytics & Serving Layer**
- Query interface combining real-time + batch views
- Analytics dashboard
- API endpoints for weather data access
- Real-time + batch view reconciliation

## Support & Contact

**Maintainer**: Brian Clasky (quark95cos@noayok.com)

**Resources:**
- [GitHub Repository](https://github.com/bclasky1539/noakweather-engineering-pipeline)
- [Issue Tracker](https://github.com/bclasky1539/noakweather-engineering-pipeline/issues)
- [Documentation](https://github.com/bclasky1539/noakweather-engineering-pipeline/tree/main/docs)

