# Weather Format References

## Overview
This document provides authoritative references for METAR and TAF weather report formats used in the NoakWeather Java library.

## Official Standards

### METAR (Meteorological Terminal Air Report)

#### Primary Sources
- **[ICAO Annex 3 - Meteorological Service for International Air Navigation](https://www.icao.int/publications/pages/publication.aspx?docnum=10004)**
  - The definitive international standard for aviation weather reporting
  - Updated regularly by International Civil Aviation Organization
  - **Status**: Official international standard (paid document)

- **[FAA Order 7900.5D - Surface Weather Observing](https://www.faa.gov/air_traffic/publications/)**
  - US implementation of METAR standards
  - Federal Aviation Administration official guidance
  - **Status**: Free, official US standard

- **[NOAA/MADIS METAR Information](https://madis.ncep.noaa.gov/madis_metar.shtml)**
  - NOAA's Meteorological Assimilation Data Ingest System
  - Comprehensive METAR data and standards documentation
  - **Status**: Free, official NOAA resource

#### Secondary References
- **[AOPA Weather Decoding Guide](https://www.aopa.org/training-and-safety/online-learning/weather-courses)**
  - Aircraft Owners and Pilots Association training materials
  - Pilot-friendly explanations and examples
  - **Status**: Educational resource

- **[Aviation Weather Center - METAR/TAF Resources](https://aviationweather.gov/data/metar/)**
  - Live METAR data and decoding tools
  - Real-time examples for testing
  - **Status**: Operational weather service

### TAF (Terminal Aerodrome Forecast)

#### Primary Sources
- **[ICAO Annex 3 - Meteorological Service for International Air Navigation](https://www.icao.int/publications/pages/publication.aspx?docnum=10004)**
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

## Format Specifications

### METAR Format Structure
```
METAR KCLT 281752Z 09014KT 10SM FEW250 23/18 A3000 RMK AO2 SLP157 T02330183
│     │    │       │       │    │      │     │     │   │   │     │
│     │    │       │       │    │      │     │     │   │   │     └─ Temperature/Dew Point (precise)
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
TAF KCLT 281735Z 2818/2918 08012KT P6SM FEW250 
TEMPO 2818/2820 BKN015 
FM282000 06008KT P6SM SCT040 BKN250
│   │    │       │         │       │    │
│   │    │       │         │       │    └─ Cloud Information
│   │    │       │         │       └─ Visibility
│   │    │       │         └─ Wind Information
│   │    │       └─ Valid Period
│   │    └─ Issue Date/Time
│   └─ Station Identifier
└─ Report Type
```

## Common Elements Reference

### Wind Information
- **Format**: `dddssKT` or `dddssGggKT`
- **ddd**: Wind direction (3 digits, magnetic)
- **ss**: Wind speed (knots)
- **gg**: Gust speed (when present)
- **Special cases**: `VRB` (variable), `00000KT` (calm)

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
- **Temperature**: Celsius, `M` prefix for below zero
- **Altimeter**: Inches of mercury (`A2992` = 29.92")
- **QNH**: Hectopascals in international format

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

- **[NOAA Aviation Weather](https://www.aviationweather.gov/metar)**
  - Current observations
  - Forecast data
  - Graphical weather products

### Sample Data Collections
- **[METAR Examples Collection](https://aviationweather.gov/data/example/)**
  - Various weather conditions
  - Edge cases and special formats
  - Good for unit testing

## Validation Tools

### Online Decoders
- **[Aviation Weather Center Decoder](https://aviationweather.gov/data/metar/)**
- **[NOAA METAR Decoder](https://forecast.weather.gov/product.php?site=NWS&product=MTR)**
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
5. **Missing Data**: Handle `/////` and `M` indicators

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
**Last Verified:** January 25, 2026,  
**Author:** NoakWeather Engineering Team 

**Next review due**: March 25, 2026,

## Related Documentation
- [Code Standards](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/docs/CODE_STANDARDS.md) - Development guidelines
- [Domain Model](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/README.md#domain-model) - Architecture overview
- [API Documentation](https://github.com/bclasky1539/noakweather-engineering-pipeline/blob/main/README.md#api-usage) - Usage examples

---

**Note**: This document focuses on METAR/TAF formats. For other weather data formats (OpenWeatherMap API, etc.), see provider-specific documentation in the respective API packages.

