# NoakWeather Engineering Pipeline

A multi-source weather data engineering platform built on Lambda Architecture principles, designed to collect, process, store, and analyze aviation weather data from multiple sources including NOAA, AWS, and potentially other providers.

## Build Status

[![Java CI with Maven](https://github.com/bclasky1539/noakweather-java/actions/workflows/maven.yml/badge.svg)](https://github.com/bclasky1539/noakweather-java/actions/workflows/maven.yml)
[![Sonar verify](https://github.com/bclasky1539/noakweather-java/actions/workflows/sonarcloud.yml/badge.svg)](https://github.com/bclasky1539/noakweather-java/actions/workflows/sonarcloud.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=bclasky1539_noakweather-java2&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=bclasky1539_noakweather-java2)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=bclasky1539_noakweather-java2&metric=coverage&branch=main)](https://sonarcloud.io/summary/new_code?id=bclasky1539_noakweather-java2)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=bclasky1539_noakweather-java2&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=bclasky1539_noakweather-java2)
[![License](https://img.shields.io/github/license/bclasky1539/noakweather-java)](https://github.com/bclasky1539/noakweather-java/blob/main/LICENSE)

## Project Structure

This project consists of two major components:

### noakweather-platform (New Multi-Module Architecture)
Source-agnostic weather data platform with Lambda Architecture implementation:

- **weather-common**: Shared models and interfaces (source-agnostic)
- **weather-ingestion**: Universal data collection and S3 upload (Speed Layer)
- **weather-processing**: Stream and batch processing (Batch Layer)
- **weather-storage**: Multi-backend storage (Snowflake, DynamoDB, S3)
- **weather-analytics**: Universal analytics and reporting (Serving Layer)
- **weather-infrastructure**: AWS CDK infrastructure as code

### noakweather-legacy
Original NOAA-specific METAR/TAF decoder (maintained for reference and gradual migration)

## Architecture

The platform implements **Lambda Architecture** to handle both real-time and batch processing:

- **Speed Layer**: Real-time ingestion of weather data from multiple sources → S3
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
- **Log4j2/Logback**: Enterprise logging
- **GitHub Actions**: CI/CD pipeline

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

## Development Workflow

This project follows a phased migration approach:

1. **Phase 1** (Current): Multi-module structure with empty platform modules
2. **Phase 2**: Migrate NOAA models to source-agnostic models in weather-common
3. **Phase 3**: Build universal ingestion layer
4. **Phase 4**: Implement storage and processing layers
5. **Phase 5**: Add analytics and serving layer
6. **Phase 6**: Deprecate legacy module

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

## Contributing

1. Create a feature branch from `main`
2. Make your changes following the code standards
3. Ensure all tests pass and coverage meets requirements
4. Submit a pull request

## License

Apache License 2.0 - See [LICENSE](LICENSE) for details

## Project Status

**Active Development** - Currently in Phase 1 of platform migration

---

**Maintainer**: Brian Clasky (quark95cos@noayok.com)
