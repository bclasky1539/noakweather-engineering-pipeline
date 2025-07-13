# noakweather-java
noakweather-java project

This Java library provides a METAR and TAF decoder.

## Build Status

[![Java CI with Maven](https://github.com/bclasky1539/noakweather-java/actions/workflows/maven.yml/badge.svg)](https://github.com/bclasky1539/noakweather-java/actions/workflows/maven.yml)
[![Sonar verify](https://github.com/bclasky1539/noakweather-java/actions/workflows/sonarcloud.yml/badge.svg)](https://github.com/bclasky1539/noakweather-java/actions/workflows/sonarcloud.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=bclasky1539_noakweather-java2&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=bclasky1539_noakweather-java2)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=bclasky1539_noakweather-java2&metric=coverage&branch=main)](https://sonarcloud.io/summary/new_code?id=bclasky1539_noakweather-java2)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=bclasky1539_noakweather-java2&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=bclasky1539_noakweather-java2)
[![License](https://img.shields.io/github/license/bclasky1539/noakweather-java)](https://github.com/bclasky1539/noakweather-java/blob/main/LICENSE)

## Features

- Java 11+ compatibility
- Comprehensive test coverage with JUnit 5
- Code quality analysis with SonarQube
- Enterprise-grade logging with Log4j2
- Maven-based build system
- CI/CD pipeline with GitHub Actions

## What is METAR?
METAR (Meteorological Terminal Air Report) is current weather report format used in aviation. Typical METAR report contains information such as location,
report issue time, wind, visibility, clouds, weather phenomena, temperature, dewpoint and atmospheric pressure.

METAR in raw form is human-readable though it might look cryptic for an untrained person.

Examples of a METAR report is as follows:

2021/12/28 01:52 KCLT 280152Z 22006KT 10SM BKN240 17/13 A2989 RMK AO2 SLP116 T01720133

2021/12/28 01:53 KSEG 280153Z AUTO VRB03KT 7SM OVC014 01/00 A2983 RMK AO2 RAB35E50UPB50E53 SLP104 P0002 T00110000


## What is TAF?
TAF (Terminal Aerodrome Forecast) is a weather forecast report format used in aviation. A TAF report is quite similar to METAR and reports
trends and changes in visibility, wind, clouds, weather, etc over periods of time.

TAF in raw form is also human-readable but requires training to decode.

Examples of a TAF report is as follows:

2021/12/28 02:52 TAF AMD KCLT 280150Z 2802/2906 21006KT P6SM SCT040 BKN150 FM281100 22005KT P6SM SCT008 BKN015 FM281500 22007KT P6SM BKN020
FM281700 21012G18KT P6SM BKN040 FM282300 21010G17KT P6SM SCT050 BKN200

2021/12/28 00:00 TAF TAF KDOV 280000Z 2800/2906 08006KT 9999 OVC030 QNH2979INS TEMPO 2800/2804 8000 -SHRA TEMPO 2806/2810 VRB06KT BECMG 2809/2810
30009KT 9999 BKN020 OVC030 QNH2980INS BECMG 2815/2816 31006KT 9999 BKN120 QNH2989INS BECMG 2819/2820 27006KT 9999 BKN100 QNH2987INS BECMG 2823/2824
09006KT 8000 -RA OVC080 QNH2985INS BECMG 2903/2904 12006KT 8000 -RA OVC050 QNH2983INS

## Run project
The decoder can retrieve the METAR and TAF data from either the NOAA website or from a file specified at the command line.

The decoder requires 4 parameters\
**Type of data:** m - metar or t - taf\
**Station 4-letter ICAO code or filename where the METAR or TAF data is:** Specify the full path of the file.\
&nbsp;&nbsp;&nbsp;&nbsp;For 4-letter ICAO code an example can be KCLT or kclt\
&nbsp;&nbsp;&nbsp;&nbsp;For a filename an example can be weather_data_metar.txt or weather_data_taf.txt. An example of each are included.\
&nbsp;&nbsp;&nbsp;&nbsp;Make sure when using a filename that the phrase **file:** precedes the filename. For example **file:weather_data_metar.txt**\
**Print results:** N or Y\
**Logging of run:** I - Info, W - Warnings (includes info), D - Debug (includes info and warnings)

A shell script is provided named weth.sh. To run normally run logging as I for info. If there is any error or there is unparsed data found run logging
as D for debug to see why the error or unparsed data is occurring.

## Getting Started

```bash
# Clone the repository
git clone https://github.com/bclasky1539/noakweather-java.git

# Navigate to project directory
cd noakweather-java

# Build the project
mvn clean compile

# Run tests
mvn test

# Generate coverage report
mvn test jacoco:report

