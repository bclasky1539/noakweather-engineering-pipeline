# NoakWeather Engineering Pipeline

A multi-source weather data engineering platform built on Lambda Architecture principles, designed to collect, process, store, and analyze aviation weather data from multiple sources including NOAA, AWS, and potentially other providers.

## Build Status

[![Java CI with Maven](https://github.com/bclasky1539/noakweather-engineering-pipeline/actions/workflows/maven.yml/badge.svg)](https://github.com/bclasky1539/noakweather-engineering-pipeline/actions/workflows/maven.yml)
[![Sonar verify](https://github.com/bclasky1539/noakweather-engineering-pipeline/actions/workflows/sonarcloud.yml/badge.svg)](https://github.com/bclasky1539/noakweather-engineering-pipeline/actions/workflows/sonarcloud.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=bclasky1539_noakweather-engineering-pipeline&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=bclasky1539_noakweather-engineering-pipeline)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=bclasky1539_noakweather-engineering-pipeline&metric=coverage&branch=main)](https://sonarcloud.io/summary/new_code?id=bclasky1539_noakweather-engineering-pipeline)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=bclasky1539_noakweather-engineering-pipeline&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=bclasky1539_noakweather-engineering-pipeline)
[![License](https://img.shields.io/github/license/bclasky1539/noakweather-engineering-pipeline)](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/LICENSE)

For the current version, full change history, and detailed release notes, see [CHANGELOG.md](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/CHANGELOG.md). For current test coverage and code quality metrics, see the badges above (live from SonarCloud).

## Project Structure

This project consists of two major components:

### noakweather-platform (Multi-Module Architecture)
Source-agnostic weather data platform with Lambda Architecture implementation:

- **weather-common**: Shared models and interfaces (source-agnostic)
- **weather-ingestion**: Universal data collection and S3 upload (Speed Layer)
- **weather-processing**: Stream and batch processing (Batch Layer)
- **weather-storage**: Multi-backend storage (Snowflake, DynamoDB, S3) with Phase 4 GSI implementation
- **weather-analytics**: Universal analytics and reporting (Serving Layer)
- **weather-infrastructure**: AWS CDK infrastructure as code

### glue-jobs (Medallion Architecture - Data Lakehouse)
AWS Glue PySpark ETL scripts implementing a Bronze → Silver → Gold medallion architecture for batch analytics, queryable via Amazon Athena:

- **Bronze layer**: Raw JSON weather data (as ingested)
- **Silver layer**: Standardized, validated, unit-converted Parquet data with computed quality scores and flight categories
- **Gold layer** *(planned)*: Aggregated, query-optimized tables for the analytics/serving layer

See [Athena Data Lakehouse Setup](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/ATHENA_SETUP.md) and [Glue Deployment Guide](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/GLUE-DEPLOYMENT-GUIDE.md) for full details.

### noakweather-legacy
Original NOAA-specific METAR/TAF decoder (maintained for reference and gradual migration)

## Architecture

The platform implements two complementary architectural patterns for different access needs:

**Lambda Architecture** handles real-time, low-latency access:
- **Speed Layer**: Real-time ingestion of weather data from multiple sources → S3 → DynamoDB with time-bucket GSI
- **Batch Layer**: Historical data processing and reprocessing
- **Serving Layer**: Unified query interface combining real-time and batch views

**Medallion Architecture** handles batch analytics and historical querying:
- **Bronze**: Raw ingested JSON in S3 (`bronze/speed-layer/...`)
- **Silver**: AWS Glue-transformed, standardized Parquet data, queryable via Amazon Athena with partition projection
- **Gold** *(planned)*: Aggregated, analytics-ready tables

### Technology Stack

- **Java 17+**: Modern Java features and performance
- **Maven**: Multi-module build management
- **AWS Services**: S3, Lambda, DynamoDB, Glue, Athena, CloudWatch
- **Apache Spark (PySpark)**: AWS Glue ETL transformations (Bronze → Silver)
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
- Docker (required for `wetht.sh` - weather-storage's DynamoDB integration tests use Testcontainers)
- AWS CLI configured (for deployment and Glue/Athena work)
- Snowflake account (for data warehouse features)
- Python 3.10 with PySpark (only if developing/testing Glue ETL scripts locally; AWS Glue itself handles this at runtime)

### Development Scripts

All scripts are run from the repository root:

| Script                     | Purpose                                                                                                                                                              |
|----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `./wethb.sh`               | Compile-only build check across the full reactor (fast sanity check)                                                                                                 |
| `./wetht.sh`               | Full test suite with JaCoCo coverage (requires Docker running)                                                                                                       |
| `./wethp.sh`               | Package into jars (`mvn clean package -DskipTests`) - only needed when jars are actually required, e.g. running an app locally                                       |
| `./wethv.sh <new-version>` | Bump the project version across `noakweather-platform` and its submodules. `noakweather-legacy` has its own independent version and is never touched by this script. |

### Building the Project

```bash
# Clone the repository
git clone https://github.com/bclasky1539/noakweather-engineering-pipeline.git
cd noakweather-engineering-pipeline

# Quick compile check
./wethb.sh

# Run the full test suite
./wetht.sh

# Only when you need jars (e.g. to run an app locally)
./wethp.sh
```

### Running Tests

```bash
# Run tests with coverage
./wetht.sh

# View coverage report for a specific module, e.g. weather-common
open noakweather-platform/weather-common/target/site/jacoco/index.html
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

- **[S3 Bucket Setup](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/S3_BUCKET_SETUP.md)** - Comprehensive guide for configuring S3 buckets for dual-storage weather data
  - AWS CLI and Console bucket creation
  - Lifecycle policies for cost optimization (30-day retention, Glacier archival)
  - Bucket structure and date partitioning examples
  - Security best practices (encryption, public access blocking)
  - Environment variable configuration and troubleshooting

- **[Single Station Integration Test](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/SINGLE_STATION_TEST_GUIDE.md)** - Step-by-step guide for testing dual-storage NOAA data ingestion
  - Pre-flight checklist (AWS credentials, S3 access, Maven build)
  - Test execution for KCLT (Charlotte Douglas International)
  - Validation commands for raw text and JSON files
  - Success criteria and verification steps
  - Troubleshooting common issues

- **[Logging Configuration Setup](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/LOGGING_SETUP.md)** - Centralized logging configuration for multi-module projects
  - Log4j2 master configuration
  - Maven resources plugin setup
  - Environment variable configuration
  - Log rotation and retention policies

### Deployment Guides

- **[AWS Athena Data Lakehouse Setup](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/ATHENA_SETUP.md)** - Medallion architecture (Bronze/Silver/Gold) setup on S3 and Athena
  - S3 bucket structure, Athena workgroup, and Glue database setup
  - Bronze and Silver layer DDL with partition projection
  - The Bronze → Silver Glue ETL transformation script
  - Query examples, orchestration, cost optimization, and troubleshooting

- **[AWS Glue Deployment Guide](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/GLUE-DEPLOYMENT-GUIDE.md)** - Deploying and running the Bronze → Silver Glue job
  - Prerequisites, IAM role, and Glue job creation
  - Running and verifying job output
  - Monitoring and troubleshooting common Glue errors

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

- **Architecture Decisions** - Lambda and Medallion Architecture design patterns
  - Speed Layer: Real-time data ingestion
  - Batch Layer: Historical data processing
  - Serving Layer: Query interface design
  - Bronze/Silver/Gold: Data lakehouse layering for batch analytics

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

## Recent Milestones

**Bronze-to-Silver Glue ETL Pipeline Complete (August 2026)**
- AWS Glue PySpark ETL job transforming Bronze METAR JSON to standardized Silver Parquet
- Explicit read schema, eliminating Spark JSON schema-inference failures on empty arrays (present weather, sky conditions, runway visual range)
- Correct unit conversion for flight category / fog / marginal VFR / low IFR calculations
- Athena Silver observations table with Hive-style partition projection (`data_source`, `year`, `month`, `day`)
- Verified end-to-end: Bronze JSON → Silver Parquet → Athena queryable
- SonarCloud maintainability and reliability cleanup (implicit time zones, duplicated validation logic, non-linear regex backtracking, modern lambda/`List.of()` idioms)

**Phase 4 Complete (January 2026)**
- DynamoDB time-bucket GSI implementation
- 50x performance improvement on time-range queries
- Zero-downtime deployment support
- Comprehensive integration test suite
- Production-ready with complete documentation

## Development Workflow

This project follows a phased migration approach:

1. **Phase 1** (Complete): Multi-module structure with platform foundation
2. **Phase 2** (Complete): NOAA models and parsers
3. **Phase 3** (Complete): Universal ingestion layer with S3 upload
4. **Phase 4** (Complete): DynamoDB storage with time-bucket GSI and comprehensive testing
5. **Phase 5** (In Progress): Analytics and serving layer
  - Bronze-to-Silver Glue ETL and Athena querying (Complete)
  - Gold layer aggregation tables (Planned)
  - Analytics dashboard and API endpoints (Planned)
  - Real-time + batch view reconciliation (Planned)
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

## Running the Bronze-to-Silver Glue ETL Job

```bash
# Upload the transformation script (only needed after script changes)
aws s3 cp glue-jobs/bronze_to_silver_metar.py s3://noakweather-glue-scripts/

# Run the job for a specific date
aws glue start-job-run \
  --job-name bronze-to-silver-metar \
  --arguments '{"--source_date":"2026-08-27"}'

# Check job status
aws glue get-job-run --job-name bronze-to-silver-metar --run-id <JobRunId>
```

See the [Glue Deployment Guide](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/GLUE-DEPLOYMENT-GUIDE.md) for full setup and troubleshooting.

## Contributing

1. Create a feature branch from `main`
2. Make your changes following the code standards
3. Ensure all tests pass and coverage meets requirements
4. Submit a pull request

## License

Apache License 2.0 - See [LICENSE](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/LICENSE) for details

## Project Status

**Active Development** - Phase 4 Complete, Phase 5 In Progress (Bronze-to-Silver ETL complete, Gold layer next)

See [CHANGELOG.md](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/CHANGELOG.md) for the complete, detailed version history.

## Support & Contact

**Maintainer**: Brian Clasky (quark95cos@noayok.com)

**Resources:**
- [GitHub Repository](https://github.com/bclasky1539/noakweather-engineering-pipeline)
- [Issue Tracker](https://github.com/bclasky1539/noakweather-engineering-pipeline/issues)
- [Documentation](https://github.com/bclasky1539/noakweather-engineering-pipeline/tree/main/docs)

