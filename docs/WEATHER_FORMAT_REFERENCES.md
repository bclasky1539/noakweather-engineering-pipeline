# Weather Format References

## Overview
This document lists the authoritative references for the METAR and TAF formats that the
NoakWeather Engineering Pipeline ingests and parses, with a short guide to the elements. For what
the parser does with a group it cannot use, see
[Parse Quality](../README.md#parse-quality-what-was-not-parsed) in the README.

## Official Standards

### METAR (Meteorological Aerodrome Report; "Aviation Routine Weather Report" in FAA usage)

#### Primary Sources
- **[ICAO Annex 3 - Meteorological Service for International Air Navigation](https://www.icao.int/sites/default/files/postalhistory/annex_3_meteorological_service_for_international_air_navigation.htm)**
  - The definitive international standard for aviation weather reporting
  - Updated regularly by International Civil Aviation Organization
  - **Status**: Official international standard (paid document)

- **[FAA Order JO 7900.5 - Surface Weather Observing](https://www.faa.gov/regulations_policies/orders_notices/index.cfm/go/document.current/documentNumber/7900.5)**
  - US implementation of METAR standards (the link always opens the current revision; 7900.5E
    with Change 1 as of November 2026)
  - Federal Aviation Administration official guidance
  - **Status**: Free, official US standard

- **WMO-No. 306, Manual on Codes, Volume I.1** (World Meteorological Organization)
  - The code forms themselves: FM 15 METAR, FM 16 SPECI, FM 51 TAF, and the code tables used in
    remarks (e.g. cloud types)
  - Available from the WMO library
  - **Status**: Free, official international code specification

- **Federal Meteorological Handbook No. 1 (FMH-1), Surface Weather Observations and Reports**
  - The US METAR/SPECI coding rules, including the remarks section (`RMK`)
  - Published by the Interagency Council for Advancing Meteorological Services (ICAMS)
  - **Status**: Free, official US standard

- **[NOAA/MADIS METAR Information](https://madis.ncep.noaa.gov/madis_metar.shtml)**
  - NOAA's Meteorological Assimilation Data Ingest System
  - Comprehensive METAR data and standards documentation
  - **Status**: Free, official NOAA resource

#### Secondary References
- **[AOPA Online Learning](https://www.aopa.org/training-and-safety/online-learning#OnlineCourses)**
  - Aircraft Owners and Pilots Association online courses, including weather courses
  - Pilot-friendly explanations and examples
  - **Status**: Educational resource (some courses may require an AOPA account)

- **[FAA Aviation Weather Handbook (FAA-H-8083-28A)](https://www.faa.gov/sites/faa.gov/files/FAA-H-8083-28A_FAA_Web.pdf)**
  - FAA handbook covering aviation weather theory and products, including how to read METAR and TAF
  - Detailed decoding examples, including remarks
  - **Status**: Free, official FAA resource (PDF)

- **[Aviation Weather Center - METAR/TAF Resources](https://aviationweather.gov/data/metar/)**
  - Live METAR data and decoding tools
  - Real-time examples for testing
  - **Status**: Operational weather service

### TAF (Terminal Aerodrome Forecast)

#### Primary Sources
- **[ICAO Annex 3 - Meteorological Service for International Air Navigation](https://www.icao.int/sites/default/files/postalhistory/annex_3_meteorological_service_for_international_air_navigation.htm)**
  - International standard for TAF format
  - Covers forecast periods, change indicators, and probability groups

- **[FAA AIM Chapter 7 - Meteorology](https://www.faa.gov/air_traffic/publications/atpubs/aim_html/chap7_section_1.html)**
  - Complete METAR/TAF decoding keys and explanations
  - Official FAA guidance with detailed examples
  - **Status**: Official FAA resource, regularly updated

- **[NOAA/NWS TAF Information](https://www.weather.gov/jetstream/taf)**
  - National Weather Service TAF explanation and examples
  - **Status**: Free, educational resource

#### Secondary References
- **[Aviation Weather Center - TAF Resources](https://aviationweather.gov/data/taf/)**
  - Live TAF data and examples
  - Real-time forecast data for testing

- **FAA Aviation Weather Handbook (FAA-H-8083-28A)**: see METAR secondary references above; covers TAF decoding as well

## Format Specifications

### METAR Format Structure
```
METAR KCLT 281752Z 09014KT 10SM FEW250 23/18 A3000 RMK AO2 SLP157 T02330183
│     │    │       │       │    │      │     │     │   │   │      │
│     │    │       │       │    │      │     │     │   │   │      └─ Temperature/Dew Point (precise)
│     │    │       │       │    │      │     │     │   │   └─ Sea Level Pressure
│     │    │       │       │    │      │     │     │   └─ Automated Station Type
│     │    │       │       │    │      │     │     └─ Remarks Section
│     │    │       │       │    │      │     └─ Altimeter Setting
│     │    │       │       │    │      └─ Temperature/Dew Point
│     │    │       │       │    └─ Cloud Information
│     │    │       │       └─ Visibility
│     │    │       └─ Wind Information
│     │    └─ Date/Time Group
│     └─ Station Identifier (ICAO)
└─ Report Type
```

### TAF Format Structure
```
┌─ Report Type
│   ┌─ Station Identifier
│   │    ┌─ Issue Date/Time
│   │    │       ┌─ Valid Period
│   │    │       │         ┌─ Wind Information
│   │    │       │         │       ┌─ Visibility
│   │    │       │         │       │    ┌─ Cloud Information
│   │    │       │         │       │    │
TAF KCLT 281735Z 2818/2918 08012KT P6SM FEW250
TEMPO 2818/2820 BKN015                  ◄─ Change group: temporary conditions on the 28th between 1800Z and 2000Z
FM282000 06008KT P6SM SCT040 BKN250     ◄─ Change group: new conditions from 28th at 2000Z
```

## Common Elements Reference

### Wind Information
- **Format**: `dddssKT` or `dddssGggKT`
- **ddd**: Wind direction (3 digits, degrees **true**; magnetic directions are used only in
  ATIS and tower broadcasts, not in METAR/TAF)
- **ss**: Wind speed (knots)
- **gg**: Gust speed (when present)
- **Special cases**: `VRB` (variable), `00000KT` (calm)
- **Variable range**: `dddVddd` after the wind group (e.g. `180V240`)
- **Units**: `KT` (knots) in most reports; `MPS` (metres per second) in some countries

### Visibility
- **US Format**: Statute miles (`10SM`, `1/2SM`, `1 1/4SM`)
- **International**: Meters (`9999`, `1200`, `0800`)
- **Special**: `CAVOK` (Ceiling and Visibility OK)

### Cloud Coverage
- **SKC/CLR**: Sky clear
- **FEW**: Few (1-2 oktas)
- **SCT**: Scattered (3-4 oktas)
- **BKN**: Broken (5-7 oktas)
- **OVC**: Overcast (8 oktas)
- **VV**: Vertical visibility (obscured)

### Weather Phenomena
- **Intensity**: `-` (light), no prefix (moderate), `+` (heavy)
- **Descriptor**: `SH` (showers), `TS` (thunderstorm), `FZ` (freezing)
- **Precipitation**: `RA` (rain), `SN` (snow), `GR` (hail)
- **Obscuration**: `FG` (fog), `BR` (mist), `HZ` (haze)

### Temperature and Pressure
- **Temperature/dew point**: `TT/DD` in whole degrees Celsius, always two digits each, with an
  `M` prefix for below zero (`04/M01`); a missing dew point is coded `//` (`04///`)
- **Altimeter**: Inches of mercury (`A2992` = 29.92")
- **QNH**: Hectopascals in international format (`Q1013`)

### Groups Beyond the Basic Structure

Live reports regularly contain groups that the basic structure above does not show. Groups seen
during UAT include:

- **Runway visual range** with trend: `R24/P1500N`, `R08L/0800V1000FT/N`
- **Runway state** (surface condition): `R23R/1///60`, `R34L/CLRD70`
- **Recent weather**: `REDZRA`, `RESHRA`, `RE//`
- **Trend**: `NOSIG`, and change groups (`TEMPO`, `BECMG`) after the main body
- **Remarks** (US and international): peak wind, wind shift, tower/surface and variable
  visibility, thunderstorm and cloud locations with directions and ranges (`TS SE MOV E`,
  `VCSH S-SW`), lightning, begin/end times of weather, hourly and 3/6/24-hour groups, sea-level
  pressure, predominant cloud types (`8/903`), observation-program remarks

Some national forms are reviewed and deliberately left unparsed (for example QFE in mmHg and
sea state groups); they stay in `remarks.freeText`.

The live snapshot used to find stations for UAT is
`https://aviationweather.gov/data/cache/metars.cache.csv.gz`. The ingestion fetches each station's
latest report from `https://tgftp.nws.noaa.gov/data/observations/metar/stations/<STATION>.TXT`.

## TAF-Specific Elements

### Change Indicators
- **FM**: From (definite time change)
- **BECMG**: Becoming (gradual change)
- **TEMPO**: Temporary (temporary fluctuations)
- **INTER**: Intermittent (frequent fluctuations)

### Probability Groups
- **PROB30**: 30% probability
- **PROB40**: 40% probability
- Used with TEMPO and change periods

### Time Formats
- **Valid Period**: `ddHH/ddHH` (day/hour from, day/hour to)
- **Change Times**: `ddHHmm` (day/hour/minute)

## Testing Data Sources

### Live Data Feeds
- **[Aviation Weather Center](https://aviationweather.gov/)**
  - Real-time METAR/TAF data
  - Multiple airport selection
  - Historical data available

- **[NOAA Aviation Weather](https://aviationweather.gov/gfa/#obs)**
  - Current observations
  - Forecast data
  - Graphical weather products

## Validation Tools

### Online Decoders
- **[Aviation Weather Center Decoder](https://aviationweather.gov/data/metar/)**
- **[SkyVector METAR Decoder](https://skyvector.com/)**

### API Endpoints for Validation
- **NOAA Aviation Weather API**: `https://aviationweather.gov/api/`
- **FAA System Operations Center**: Various endpoints for real-time data

## Implementation Notes

### Parsing Considerations
1. **Variable Formats**: METAR/TAF can have optional fields
2. **International Differences**: US vs. ICAO format variations
3. **Error Handling**: Invalid or incomplete reports are common
4. **Time Zones**: All times in UTC (Zulu time)
5. **Missing Data**: Slashes (`/////`, `//`) mark missing values; `M` is a minus sign in
   temperatures and a "less than" prefix in visibility and RVR
6. **Truncated Reports**: A report can be cut off at the source, leaving an incomplete last group
   (e.g. `04/0`); such a group is rejected, not completed

### Common Parsing Challenges
- **Wind Calm**: `00000KT` vs missing wind group
- **Variable Visibility**: Multiple visibility groups in remarks
- **Cloud Layers**: Handling `CLR` vs `SKC` vs missing clouds
- **Amendments**: `COR` (corrected) and `AUTO` indicators
- **Remarks Section**: Highly variable format, optional parsing

## Updates and Maintenance

### Document Maintenance
- **Review Schedule**: Quarterly review of external links
- **Update Triggers**: When ICAO/FAA standards change
- **Version Control**: Track changes to referenced documents

### Link Verification
**Last Verified:** November 13, 2026,  
**Author:** NoakWeather Engineering Team

**Next review due**: February 13, 2027,

## Related Documentation
- [Code Standards](CODE_STANDARDS.md) - Code organization and standards
- [Contributing](../CONTRIBUTING.md#parsers-weatherprocessingparsernoaa) - Parser conventions
- [Parse Quality](../README.md#parse-quality-what-was-not-parsed) - What happens to text the parser cannot use

---

**Note**: This document covers METAR/TAF only. Other data sources are planned; their formats
will be documented when they are added.

