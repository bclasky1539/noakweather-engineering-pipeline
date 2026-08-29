# AWS Athena Data Lakehouse Setup - Medallion Architecture

## Overview

This document describes the complete data lakehouse implementation for NoakWeather Engineering Pipeline using AWS Athena and the Medallion Architecture pattern (Bronze → Silver → Gold layers).

**Architecture Pattern:** Medallion Architecture (Data Lakehouse)  
**Query Engine:** AWS Athena  
**Storage Format:** JSON (Bronze), Parquet (Silver), Apache Iceberg (Gold)  
**Transformation Engine:** AWS Glue + PySpark  
**Use Cases:** Historical analysis, ML feature engineering, BI reporting

---

## Architecture Overview

### Medallion Architecture Layers
```
┌─────────────────────────────────────────────────────────────────┐
│                        DATA SOURCES                             │
│        NOAA  |  OpenWeatherMap  |  WeatherAPI  |  Others        │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ↓ [Java Ingestion Pipeline]
┌─────────────────────────────────────────────────────────────────┐
│                     BRONZE LAYER (Raw Data)                     │
│  Purpose: Immutable audit trail, exact copy from source         │
│  Format:  JSON files                                            │
│  Storage: s3://noakweather-data/bronze/                         │
│  Query:   Athena external tables                                │
│  Owner:   Platform Engineering Team (Java)                      │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ↓ [AWS Glue PySpark Jobs]
┌─────────────────────────────────────────────────────────────────┐
│                 SILVER LAYER (Standardized Data)                │
│  Purpose: Validated, unified schema, deduplicated               │
│  Format:  Parquet (columnar)                                    │
│  Storage: s3://noakweather-data/silver/                         │
│  Query:   Athena managed tables                                 │
│  Owner:   Data Engineering Team (Python)                        │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ↓ [AWS Glue PySpark Jobs]
┌─────────────────────────────────────────────────────────────────┐
│               GOLD LAYER (Analytics-Ready Lakehouse)            │
│  Purpose: Pre-aggregated, feature-engineered, BI-ready          │
│  Format:  Apache Iceberg                                        │
│  Storage: s3://noakweather-data/gold/                           │
│  Query:   Athena Iceberg tables                                 │
│  Owner:   Data Science Team (Python)                            │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│                     CONSUMPTION LAYER                           │
│  Jupyter Notebooks  |  BI Tools  |  ML Models  |  APIs          │
└─────────────────────────────────────────────────────────────────┘
```

### Technology Stack

| Layer      | Storage Format | Query Interface        | Transformation   | Owner                    |
|------------|----------------|------------------------|------------------|--------------------------|
| **Bronze** | JSON           | Athena External Tables | None (raw)       | Java Platform Team       |
| **Silver** | Parquet        | Athena Managed Tables  | AWS Glue PySpark | Python Data Eng Team     |
| **Gold**   | Iceberg        | Athena Iceberg Tables  | AWS Glue PySpark | Python Data Science Team |

---

## Prerequisites

- AWS Account with appropriate permissions
- S3 bucket: `noakweather-data` (existing)
- AWS CLI configured
- IAM permissions for: Athena, S3, Glue, Glue Data Catalog
- Python 3.9+ (for local development)
- Java 17+ (for ingestion pipeline - already configured)

---

## Part 1: Infrastructure Setup

### Step 1: Create S3 Bucket Structure

Reorganize your existing S3 bucket to follow Medallion Architecture:
```bash
# View current structure
aws s3 ls s3://noakweather-data/ --recursive | head -20

# Recommended structure (migrate gradually)
s3://noakweather-data/
├── bronze/                          # Raw ingested data (JSON)
│   ├──batch-layer
│   │   ├── noaa/
│   │   │   ├── metar/YYYY/MM/DD/
│   │   │   └── taf/YYYY/MM/DD/
│   │   ├── openweathermap/YYYY/MM/DD/
│   └── └── weatherapi/YYYY/MM/DD/
│   ├──raw-data
│   │   ├── noaa/
│   │   │   ├── metar/YYYY/MM/DD/
│   │   │   └── taf/YYYY/MM/DD/
│   │   ├── openweathermap/YYYY/MM/DD/
│   └── └── weatherapi/YYYY/MM/DD/
│   ├──speed-layer
│   │   ├── noaa/
│   │   │   ├── metar/YYYY/MM/DD/
│   │   │   └── taf/YYYY/MM/DD/
│   │   ├── openweathermap/YYYY/MM/DD/
│   └── └── weatherapi/YYYY/MM/DD/
├── silver/                          # Standardized data (Parquet)
│   ├── observations/
│   │   └── source=noaa/year=YYYY/month=MM/day=DD/
│   └── forecasts/
│       └── source=noaa/year=YYYY/month=MM/day=DD/
├── gold/                            # Analytics-ready (Iceberg)
│   ├── current_conditions/
│   ├── observations_historical/
│   ├── daily_stats/
│   ├── ml_features/
│   └── stations/
└── athena-results/                  # Query results
```

**Migration Note:** Your current `speed-layer` directory maps to `bronze`. You can:
- **Option A:** Copy existing data to new structure
- **Option B:** Create symlinks (S3 doesn't support true symlinks, but Athena can point to multiple locations)
- **Option C:** Update Java ingestion code to write to `bronze/` going forward

### Step 2: Create Athena Query Results Bucket
```bash
# Create lifecycle policy first
cat > athena-lifecycle-policy.json << 'EOF'
{
  "Rules": [
    {
      "ID": "DeleteOldQueryResults",
      "Status": "Enabled",
      "Prefix": "",
      "Expiration": {
        "Days": 30
      },
      "NoncurrentVersionExpiration": {
        "NoncurrentDays": 7
      }
    }
  ]
}
EOF

# Create bucket
aws s3 mb s3://noakweather-athena-results --region us-east-1

# Apply lifecycle policy
aws s3api put-bucket-lifecycle-configuration \
  --bucket noakweather-athena-results \
  --lifecycle-configuration file://athena-lifecycle-policy.json

# Check policy
aws s3api get-bucket-lifecycle-configuration \
  --bucket noakweather-athena-results
```

### Step 3: Create Athena Workgroup

AWS Athena workgroups help organize queries and control costs. We'll create a workgroup with Athena engine version 3 for Apache Iceberg support.

#### Option A: Using AWS Console (Recommended)

**Use this method if you encounter IAM permission issues with the CLI.**

1. **Navigate to Athena Console:**
    - Go to https://console.aws.amazon.com/athena/
    - Click **"Workgroups"** in the left sidebar
    - Click **"Create workgroup"**

2. **Workgroup details:**
    - **Workgroup name:** `noakweather-workgroup`
    - **Description:** `NoakWeather Data Lakehouse workgroup with Iceberg support`

3. **Analytics engine:**
    - Select **"Athena SQL"** (for SQL queries on S3 data)
    - **Upgrade query engine:** Keep **"Automatic"** selected
    - This ensures you get Athena engine version 3 automatically

4. **Authentication:**
    - Select **"AWS Identity and Access Management (IAM)"**
    - Click **"Next"**

5. **Query result configuration:**
    - **Management of query results:** Select **"Customer managed"**
    - **Location of query result:** `s3://noakweather-athena-results/`
    - **Expected bucket owner:** Leave blank
    - **Assign bucket owner full control:** Leave unchecked
    - **Encrypt query results:** Leave unchecked (bucket uses default AWS encryption)
    - **Override client-side settings:** ☑️ **CHECK THIS BOX** (enforces workgroup settings)
    - Click **"Next"**

6. **Review and create:**
    - Review your settings
    - Click **"Create workgroup"**

7. **Verify creation:**
    - You should see a green banner: "Workgroup created successfully"
    - Verify settings:
        - Query engine version: Athena engine version 3
        - Override client side settings: Turned on
        - Location of query result: s3://noakweather-athena-results/

#### Option B: Using AWS CLI

**Note:** This requires `athena:CreateWorkGroup` IAM permission. If you get an AccessDenied error, use Option A (Console) instead.
```bash
aws athena create-work-group \
  --name noakweather-workgroup \
  --description "NoakWeather Data Lakehouse workgroup with Iceberg support" \
  --configuration '{
    "ResultConfiguration": {
      "OutputLocation": "s3://noakweather-athena-results/"
    },
    "EnforceWorkGroupConfiguration": true,
    "EngineVersion": {
      "SelectedEngineVersion": "Athena engine version 3"
    }
  }'
```

**Verify the workgroup:**
```bash
# This command also requires athena:GetWorkGroup permission
aws athena get-work-group --work-group noakweather-workgroup
```

**Expected output:**
```json
{
    "WorkGroup": {
        "Name": "noakweather-workgroup",
        "State": "ENABLED",
        "Configuration": {
            "ResultConfiguration": {
                "OutputLocation": "s3://noakweather-athena-results/"
            },
            "EnforceWorkGroupConfiguration": true,
            "EngineVersion": {
                "SelectedEngineVersion": "Athena engine version 3",
                "EffectiveEngineVersion": "Athena engine version 3"
            }
        },
        "Description": "NoakWeather Data Lakehouse workgroup with Iceberg support",
        "CreationTime": "2026-02-09T..."
    }
}
```

#### Troubleshooting

**AccessDenied Error:**
```
An error occurred (AccessDeniedException) when calling the CreateWorkGroup operation
```

**Solution:** Your IAM user doesn't have `athena:CreateWorkGroup` permission. Use **Option A (Console)** instead, which typically has broader permissions.

**To request CLI permissions,** ask your AWS administrator to add this IAM policy. This includes Glue Data Catalog permissions so it is ready for Glue deployment.
```json
{
   "Version": "2012-10-17",
   "Statement": [
      {
         "Effect": "Allow",
         "Action": [
            "athena:CreateWorkGroup",
            "athena:GetWorkGroup",
            "athena:UpdateWorkGroup",
            "athena:ListTableMetadata"
         ],
         "Resource": "*"
      },
      {
         "Effect": "Allow",
         "Action": [
            "glue:GetDatabase",
            "glue:GetDatabases",
            "glue:GetTable",
            "glue:GetTables"
         ],
         "Resource": "*"
      }
   ]
}
```

**Important:** Use Athena engine version 3 for Apache Iceberg support.

### Step 4: Create Glue Database

Athena uses AWS Glue Data Catalog to store table metadata. We will create a database to organize our Bronze, Silver, and Gold layer tables.

#### Option A: Using Athena Query Editor (Recommended)

**This is the easiest method and works for users with limited CLI permissions.**

1. **Navigate to Athena Query Editor:**
    - Go to https://console.aws.amazon.com/athena/
    - Click **"Query editor"** in the left sidebar
    - **Important:** Verify that **"noakweather-workgroup"** is selected in the workgroup dropdown (top right)

2. **Create the database:**

   Copy and paste this SQL into the query editor:
```sql
   CREATE DATABASE IF NOT EXISTS noakweather
   COMMENT 'NoakWeather Engineering Pipeline - Medallion Architecture'
   LOCATION 's3://noakweather-data/';
```

3. **Run the query:**
    - Click the **"Run"** button (or press Ctrl+Enter / Cmd+Enter)
    - You should see "Completed" status

4. **Verify the database was created:**

   Run this query:
```sql
   SHOW DATABASES;
```

You should see both `default` and `noakweather` in the results.

5. **Select the database:**
    - In the left sidebar under **"Database"**, select **"noakweather"** from the dropdown
    - You don't need to run `USE noakweather;` in the Console - just select it from the dropdown

**Expected Result:**
- Database appears in the left sidebar dropdown
- "Tables and views" section shows "(0)" tables (we'll add tables in the next step)
- Query results show the database was created

#### Option B: Using AWS CLI

**Note:** This requires Glue permissions. If you get an AccessDenied error, use Option A (Console) instead.
```bash
aws glue create-database \
  --database-input '{
    "Name": "noakweather",
    "Description": "NoakWeather Engineering Pipeline - Medallion Architecture",
    "LocationUri": "s3://noakweather-data/"
  }'
```

**Verify the database:**
```bash
aws glue get-database --name noakweather
```

**Expected output:**
```json
{
    "Database": {
        "Name": "noakweather",
        "Description": "NoakWeather Engineering Pipeline - Medallion Architecture",
        "LocationUri": "s3://noakweather-data/",
        "CreateTime": "2026-02-09T...",
        "CatalogId": "123456789012"
    }
}
```

#### Option C: Using Glue Console

**Alternative method using the AWS Glue Console:**

1. Go to https://console.aws.amazon.com/glue/
2. Click **"Databases"** in the left sidebar (under Data Catalog)
3. Click **"Add database"**
4. **Database details:**
    - **Name:** `noakweather`
    - **Description:** `NoakWeather Engineering Pipeline - Medallion Architecture`
    - **Location (optional):** `s3://noakweather-data/`
5. Click **"Create database"**

#### Troubleshooting

**AccessDenied Error (CLI):**
```
An error occurred (AccessDeniedException) when calling the CreateDatabase operation
```

**Solution:** Your IAM user doesn't have `glue:CreateDatabase` permission. Use **Option A (Athena Query Editor)** or **Option C (Glue Console)** instead.

**Database already exists:**
```
AlreadyExistsException: Database noakweather already exists
```

**Solution:** This is fine! The database was already created. You can verify it exists by running `SHOW DATABASES;` in Athena.

**Cannot find database in Athena:**

**Solution:**
1. Refresh the page
2. Make sure you're using the correct workgroup (`noakweather-workgroup`)
3. Click the refresh icon next to "Data" in the left sidebar

---

## Part 2: Bronze Layer Setup (Raw Data)

### Bronze Layer Characteristics

- **Purpose:** Immutable audit trail, exact source copy
- **Format:** JSON (as received from APIs)
- **Partitioning:** year/month/day/source
- **Retention:** Indefinite (archival storage)
- **Query Use:** Debugging, reprocessing, compliance

### DDL: Bronze METAR Observations

Save as `sql/bronze/create_bronze_metar_noaa.sql`:
```sql
-- Bronze Layer: Raw NOAA METAR data
-- Purpose: Immutable source of truth, exact JSON from ingestion pipeline
-- Location: s3://noakweather-data/bronze/speed-layer/noaa/metar/
-- Medallion Architecture: Bronze (Raw) → Silver (Standardized) → Gold (Analytics)

CREATE TABLE IF NOT EXISTS noakweather.bronze_metar_noaa (
    -- Core identification (as ingested)
    id STRING COMMENT 'Unique observation ID (UUID)',
    dataType STRING COMMENT 'Data type identifier',
    source STRING COMMENT 'Data source',
    processingLayer STRING COMMENT 'Processing layer identifier',
    stationId STRING COMMENT 'ICAO station identifier',
    observationTime BIGINT COMMENT 'Unix timestamp (seconds)',
    ingestionTime DOUBLE COMMENT 'Pipeline ingestion timestamp',

    -- Raw data preservation
    rawData STRING COMMENT 'Original METAR text from source',
    rawText STRING COMMENT 'Parsed METAR text',
    unparsedMainBody STRING COMMENT 'Tokens not recognized by parser',

    -- Complete JSON structure (preserves all fields)
    metadata STRUCT
        fetch_timestamp:STRING,
        validated:STRING,
        processor_version:STRING,
        parser_version:STRING,
        full_response:STRING,
        storage_format:STRING,
        parsed:STRING,
        processor:STRING,
        validation_timestamp:STRING
    >,

    reportType STRING,
    reportModifier STRING,

    -- Weather conditions (as parsed)
    conditions STRUCT
        wind:STRUCT
            directionDegrees:INT,
            speedValue:INT,
            gustValue:INT,
            variabilityFrom:INT,
            variabilityTo:INT,
            unit:STRING,
            speedKnots:INT,
            cardinalDirection:STRING,
            summary:STRING,
            calm:BOOLEAN,
            strongWind:BOOLEAN,
            speedMps:INT,
            speedKmh:DOUBLE,
            beaufortScale:INT,
            gale:BOOLEAN,
            variable:BOOLEAN
        >,
        visibility:STRUCT
            distanceValue:DOUBLE,
            unit:STRING,
            lessThan:BOOLEAN,
            greaterThan:BOOLEAN,
            specialCondition:BOOLEAN,
            summary:STRING,
            cavok:BOOLEAN,
            lowVisibility:BOOLEAN,
            vfr:BOOLEAN,
            ifr:BOOLEAN,
            unlimited:BOOLEAN
        >,
        temperature:STRUCT
            celsius:DOUBLE,
            dewpointCelsius:DOUBLE,
            relativeHumidity:DOUBLE,
            heatIndex:DOUBLE,
            spread:DOUBLE,
            freezing:BOOLEAN,
            summary:STRING,
            maxCelsius:DOUBLE,
            minCelsius:DOUBLE,
            belowFreezing:BOOLEAN,
            aboveFreezing:BOOLEAN,
            fogLikely:BOOLEAN,
            icingLikely:BOOLEAN,
            veryCold:BOOLEAN,
            veryHot:BOOLEAN,
            currentObservation:BOOLEAN,
            forecast:BOOLEAN
        >,
        pressure:STRUCT
            value:DOUBLE,
            unit:STRING,
            pressureAltitudeFeet:DOUBLE,
            formattedValue:STRING,
            deviationFromStandard:DOUBLE,
            summary:STRING,
            belowStandard:BOOLEAN,
            aboveStandard:BOOLEAN,
            lowPressure:BOOLEAN,
            highPressure:BOOLEAN,
            extremelyLow:BOOLEAN,
            extremelyHigh:BOOLEAN
        >,
        skyConditions:ARRAY<STRUCT
            coverage:STRING,
            heightFeet:INT,
            heightMeters:INT,
            cloudType:STRING,
            ceiling:BOOLEAN,
            cumulonimbus:BOOLEAN,
            toweringCumulus:BOOLEAN,
            convective:BOOLEAN,
            summary:STRING,
            clear:BOOLEAN
        >>,
        presentWeather:ARRAY<STRUCT
            intensity:STRING,
            descriptor:STRING,
            precipitation:STRING,
            obscuration:STRING,
            other:STRING,
            rawCode:STRING,
            description:STRING,
            thunderstorm:BOOLEAN,
            freezing:BOOLEAN,
            light:BOOLEAN,
            heavy:BOOLEAN,
            vicinity:BOOLEAN,
            showers:BOOLEAN,
            noSignificantWeather:BOOLEAN,
            intensityDescription:STRING
        >>,
        ceilingFeet:INT,
        likelyVMC:BOOLEAN,
        likelyIMC:BOOLEAN,
        clearAndCalm:BOOLEAN
    >,

    -- Runway visual range
    runwayVisualRange ARRAY<STRUCT
        runway:STRING,
        visualRangeFeet:INT,
        variableLow:INT,
        variableHigh:INT,
        trend:STRING
    >>,

    -- Remarks
    remarks STRUCT
        automatedStationType:STRING,
        seaLevelPressure:STRUCT
            value:DOUBLE,
            unit:STRING,
            summary:STRING
        >,
        preciseTemperature:STRUCT
            celsius:DOUBLE,
            dewpointCelsius:DOUBLE
        >,
        peakWind:STRUCT
            directionDegrees:INT,
            speedKnots:INT,
            hour:INT,
            minute:INT
        >,
        windShift:STRUCT
            hour:INT,
            minute:INT,
            frontalPassage:BOOLEAN
        >,
        maintenanceRequired:BOOLEAN,
        freeText:STRING
    >,

    -- Flattened fields (duplicated for convenience)
    latitude DOUBLE,
    longitude DOUBLE,
    elevationFeet INT,
    peakWind STRUCT
        directionDegrees:INT,
        speedKnots:INT,
        hour:INT,
        minute:INT
    >,
    seaLevelPressure DOUBLE,
    automatedStation STRING,
    noSignificantChange BOOLEAN,
    automated BOOLEAN,
    summary STRING,
    current BOOLEAN,
    minimumRvrFeet INT,
    
    -- Convenience accessors (duplicated from conditions for easier queries)
    temperature STRUCT
        celsius:DOUBLE,
        dewpointCelsius:DOUBLE,
        relativeHumidity:DOUBLE,
        summary:STRING
    >,
    visibility STRUCT
        distanceValue:DOUBLE,
        unit:STRING,
        vfr:BOOLEAN,
        summary:STRING
    >,
    pressure STRUCT
        value:DOUBLE,
        unit:STRING,
        summary:STRING
    >,
    wind STRUCT
        directionDegrees:INT,
        speedKnots:INT,
        cardinalDirection:STRING,
        summary:STRING
    >,
    skyConditions ARRAY<STRUCT
        coverage:STRING,
        heightFeet:INT,
        cloudType:STRING,
        ceiling:BOOLEAN
    >>,
    presentWeather ARRAY<STRUCT
        rawCode:STRING,
        description:STRING
    >>,
    ceilingFeet INT
)
COMMENT 'Bronze: Raw NOAA METAR observations (immutable source of truth)'
PARTITIONED BY (
    year STRING,
    month STRING,
    day STRING
)
ROW FORMAT SERDE 'org.openx.data.jsonserde.JsonSerDe'
WITH SERDEPROPERTIES (
    'ignore.malformed.json' = 'true',
    'case.insensitive' = 'true'
)
STORED AS INPUTFORMAT 'org.apache.hadoop.mapred.TextInputFormat'
OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
LOCATION 's3://noakweather-data/bronze/speed-layer/noaa/metar/'
TBLPROPERTIES (
    'projection.enabled' = 'true',
    'projection.year.type' = 'integer',
    'projection.year.range' = '2020,2030',
    'projection.year.digits' = '4',
    'projection.month.type' = 'integer',
    'projection.month.range' = '01,12',
    'projection.month.digits' = '2',
    'projection.day.type' = 'integer',
    'projection.day.range' = '01,31',
    'projection.day.digits' = '2',
    'storage.location.template' = 's3://noakweather-data/bronze/speed-layer/noaa/metar/${year}/${month}/${day}',
    'classification' = 'json'
);
```

#### Using AWS Console (Recommended)

1. **Navigate to Athena Console:**
   - Go to https://console.aws.amazon.com/athena/
   - Make sure noakweather-workgroup is selected
   - Database:noakweather

2. **Execution of SQL:**
   - Copy the entire SQL from the file and paste it into the query editor
   - Click "Run"

3. **Expected result:**
   - "Query successful" message
   - Table bronze_metar_noaa appears in left sidebar under "Tables (1)"

### Verify Bronze Layer
```sql
-- Test query: Count records
SELECT COUNT(*) as total_records
FROM noakweather.bronze_metar_noaa
WHERE year = '2026' AND month = '02' AND day = '07';

-- Sample recent data. The year, month and day may vary.
SELECT
stationId,
    FROM_UNIXTIME(observationTime) as observation_time,
    conditions.temperature.celsius as temp_c,
    conditions.wind.speedKnots as wind_knots,
    conditions.pressure.value as pressure_hpa,
    conditions.visibility.distanceValue as visibility,
    metadata.processor_version,
    metadata.parser_version,
    runwayVisualRange,
    summary,
    remarks.automatedStationType,
    remarks.freeText,
    remarks.seaLevelPressure.summary
FROM noakweather.bronze_metar_noaa
WHERE year = '2026' AND month = '02' AND day = '07'
ORDER BY stationId
LIMIT 5;

-- Sample recent data including fields within the runwayVisualRange array/structure.
-- The year, month and day may vary.
SELECT
   m.stationId,
   FROM_UNIXTIME(m.observationTime) as observation_time,
   m.conditions.temperature.celsius as temp_c,
   m.conditions.wind.speedKnots as wind_knots,
   m.conditions.pressure.value as pressure_hpa,
   m.conditions.visibility.distanceValue as visibility,
   m.metadata.processor_version,
   m.metadata.parser_version,
   runway.runway,
   runway.visualRangeFeet,
   runway.trend,
   runway.variablelow,
   runway.variablehigh,
   m.summary,
   m.remarks.automatedStationType,
   m.remarks.freeText,
   m.remarks.seaLevelPressure.summary
FROM noakweather.bronze_metar_noaa m
    LEFT JOIN UNNEST(m.runwayVisualRange) AS t(runway) ON TRUE
WHERE m.year = '2026' AND m.month = '02' AND m.day = '07'
ORDER BY m.stationId
   LIMIT 5;

SELECT
   stationId,
   FROM_UNIXTIME(observationTime) as observation_time,
   conditions.temperature.celsius as temp_c,
   conditions.wind.speedKnots as wind_knots,
   conditions.pressure.value as pressure_hpa,
   conditions.visibility.distanceValue as visibility,
   metadata.processor_version,
   metadata.parser_version,
   CAST(runwayVisualRange AS JSON) as runwayVisualRange_json,
   summary,
   remarks.automatedStationType,
   remarks.freeText,
   remarks.seaLevelPressure.summary
FROM noakweather.bronze_metar_noaa
WHERE year = '2026' AND month = '02' AND day = '07'
ORDER BY stationId
   LIMIT 15;
```

---

## Part 3: Silver Layer Setup (Standardized Data)

### Silver Layer Characteristics

- **Purpose:** Unified schema across all sources, validated
- **Format:** Parquet (columnar, compressed)
- **Partitioning:** source/year/month/day
- **Retention:** 7+ years
- **Query Use:** Analytics, reporting, ML feature engineering

### DDL: Silver Observations (Unified)

Save as `sql/silver/create_silver_observations.sql`:
```sql
-- Silver Layer: Standardized Weather Observations
-- Purpose: Clean, validated, standardized data for analytics
-- Source: bronze_metar_noaa (and future: bronze_taf_noaa, bronze_openweather, etc.)
-- Format: Parquet with Snappy compression
-- Units: Celsius, knots, hPa, meters (standardized)

CREATE EXTERNAL TABLE IF NOT EXISTS noakweather.silver_observations (
    -- Core identification
    observation_id STRING COMMENT 'Unique observation ID',
    observation_type STRING COMMENT 'METAR, TAF, SYNOP, etc.',
    station_id STRING COMMENT 'ICAO or WMO station identifier',
    observation_time TIMESTAMP COMMENT 'Observation timestamp (UTC)',
    ingestion_time TIMESTAMP COMMENT 'When ingested into Bronze layer',
    processing_time TIMESTAMP COMMENT 'When transformed to Silver layer',

    -- Location
    latitude DOUBLE COMMENT 'Station latitude (decimal degrees)',
    longitude DOUBLE COMMENT 'Station longitude (decimal degrees)',
    elevation_meters INT COMMENT 'Station elevation (meters)',
    
    -- Temperature (all in Celsius)
    temperature_celsius DOUBLE COMMENT 'Air temperature',
    dewpoint_celsius DOUBLE COMMENT 'Dewpoint temperature',
    temperature_spread_celsius DOUBLE COMMENT 'Temperature-dewpoint spread',
    relative_humidity_percent DOUBLE COMMENT 'Relative humidity (0-100)',
    heat_index_celsius DOUBLE COMMENT 'Heat index',
    
    -- Wind (all in knots and degrees)
    wind_direction_degrees INT COMMENT 'Wind direction (0-360)',
    wind_speed_knots INT COMMENT 'Wind speed',
    wind_gust_knots INT COMMENT 'Wind gust speed',
    wind_cardinal_direction STRING COMMENT 'Cardinal direction (N, NE, E, etc.)',
    is_wind_calm BOOLEAN COMMENT 'Calm winds',
    is_wind_variable BOOLEAN COMMENT 'Variable wind direction',
    
    -- Visibility (all in meters)
    visibility_meters DOUBLE COMMENT 'Prevailing visibility',
    is_visibility_less_than BOOLEAN COMMENT 'Visibility is less than reported value',
    is_cavok BOOLEAN COMMENT 'Ceiling and visibility OK',
    
    -- Pressure (all in hPa)
    pressure_hpa DOUBLE COMMENT 'Altimeter setting',
    pressure_altitude_feet DOUBLE COMMENT 'Pressure altitude',
    sea_level_pressure_hpa DOUBLE COMMENT 'Sea level pressure (from remarks)',
    
    -- Sky conditions
    ceiling_feet INT COMMENT 'Ceiling height',
    ceiling_meters INT COMMENT 'Ceiling height (meters)',
    lowest_cloud_base_feet INT COMMENT 'Lowest cloud layer base',
    sky_coverage STRING COMMENT 'Overall sky coverage (CLR, FEW, SCT, BKN, OVC)',
    has_cumulonimbus BOOLEAN COMMENT 'Cumulonimbus present',
    has_towering_cumulus BOOLEAN COMMENT 'Towering cumulus present',
    
    -- Weather phenomena
    present_weather STRING COMMENT 'Current weather (codes)',
    weather_intensity STRING COMMENT 'Intensity (light, moderate, heavy)',
    has_thunderstorm BOOLEAN COMMENT 'Thunderstorm in vicinity or at station',
    has_precipitation BOOLEAN COMMENT 'Any precipitation occurring',
    has_fog BOOLEAN COMMENT 'Fog or mist present',
    
    -- Flight categories
    flight_category STRING COMMENT 'VFR, MVFR, IFR, LIFR',
    is_vfr BOOLEAN COMMENT 'Visual flight rules conditions',
    is_ifr BOOLEAN COMMENT 'Instrument flight rules conditions',
    is_marginal_vfr BOOLEAN COMMENT 'Marginal VFR conditions',
    is_low_ifr BOOLEAN COMMENT 'Low IFR conditions',
    
    -- Runway Visual Range (flattened from array)
    rvr_runway_id STRING COMMENT 'RVR runway identifier',
    rvr_visual_range_feet INT COMMENT 'RVR visibility',
    rvr_variable_low_feet INT COMMENT 'RVR variable low',
    rvr_variable_high_feet INT COMMENT 'RVR variable high',
    
    -- Raw data preservation
    raw_text STRING COMMENT 'Original observation text',
    unparsed_tokens STRING COMMENT 'Tokens not recognized by parser',
    
    -- Metadata
    processor_version STRING COMMENT 'Parser/processor version',
    is_automated BOOLEAN COMMENT 'Automated station',
    automated_station_type STRING COMMENT 'Type of automation',
    
    -- Data quality
    quality_score DOUBLE COMMENT 'Overall quality score (0-100)',
    completeness_score DOUBLE COMMENT 'Data completeness (0-100)',
    has_temperature BOOLEAN COMMENT 'Temperature field present',
    has_wind BOOLEAN COMMENT 'Wind field present',
    has_pressure BOOLEAN COMMENT 'Pressure field present',
    has_visibility BOOLEAN COMMENT 'Visibility field present',
    validation_flags STRING COMMENT 'Validation issues (comma-separated)',
    
    -- Summary
    observation_summary STRING COMMENT 'Human-readable summary'
)
COMMENT 'Silver: Standardized weather observations (all sources, validated, Parquet)'
PARTITIONED BY (
    data_source STRING COMMENT 'Source system (NOAA, OpenWeatherMap, etc.)',
    year STRING,
    month STRING,
    day STRING
)
STORED AS PARQUET
LOCATION 's3://noakweather-data/silver/observations/'
TBLPROPERTIES (
    'parquet.compression'='SNAPPY',
    'projection.enabled'='true',
    'projection.data_source.type'='enum',
    'projection.data_source.values'='noaa,openweathermap,weatherapi',
    'projection.year.type'='integer',
    'projection.year.range'='2020,2030',
    'projection.year.digits'='4',
    'projection.month.type'='integer',
    'projection.month.range'='01,12',
    'projection.month.digits'='2',
    'projection.day.type'='integer',
    'projection.day.range'='01,31',
    'projection.day.digits'='2',
    'storage.location.template'='s3://noakweather-data/silver/observations/data_source=${data_source}/year=${year}/month=${month}/day=${day}'
);
```

### AWS Glue Job: Bronze → Silver Transformation

Create file: `glue-jobs/bronze_to_silver_metar.py`
```python
"""
AWS Glue ETL Job: Bronze to Silver - METAR Observations
Purpose: Transform raw NOAA METAR data into standardized, validated Parquet format
Author: NoakWeather Engineering Team
Last Updated: 2026-08-27

Transformations:
- Flatten nested JSON structures
- Standardize units (all temps in Celsius, winds in knots, pressure in hPa)
- Calculate flight categories (VFR/MVFR/IFR/LIFR)
- Compute data quality scores
- Convert to Parquet with Snappy compression

Notes:
- Uses an explicit schema on read instead of relying on Spark's JSON schema
  inference. Present weather, sky conditions, and runway visual range are
  frequently empty arrays in real-world METARs (clear skies, no significant
  weather, no RVR reported). Spark cannot infer an element type from an
  empty array and silently falls back to array<string>, which then fails
  later when the transformation tries to access struct fields on those
  elements. An explicit schema avoids this entirely, regardless of whether
  the arrays are empty or populated in any given file.
- Flight category thresholds (VFR/MVFR/IFR/LIFR) must be evaluated against
  visibility already converted to meters. The raw conditions.visibility.
  distanceValue field is in whatever unit the source reported (commonly
  statute miles "SM" for NOAA), so it cannot be compared directly against
  meter-based thresholds. visibility_meters_expr performs the same M/SM
  conversion used for the visibility_meters output column; it is defined
  as its own expression (not referenced by the visibility_meters alias)
  because Spark evaluates every expression in a single select() against
  the original input columns, not against sibling expressions being
  computed in that same select() call.
"""

import sys
from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from awsglue.context import GlueContext
from awsglue.job import Job
from pyspark.sql.functions import (
    col, lit, when, from_unixtime,
    current_timestamp, size, concat_ws, expr
)
from pyspark.sql.types import (
    StructType, StructField, StringType, DoubleType,
    IntegerType, BooleanType, ArrayType
)

# Initialize Glue context
args = getResolvedOptions(sys.argv, ['JOB_NAME', 'source_date'])
sc = SparkContext()
glueContext = GlueContext(sc)
spark = glueContext.spark_session

# Set Spark to be case-sensitive to avoid column name conflicts
spark.conf.set("spark.sql.caseSensitive", "true")

job = Job(glueContext)
job.init(args['JOB_NAME'], args)

# Parameters
SOURCE_DATE = args['source_date']  # Format: YYYY-MM-DD
year, month, day = SOURCE_DATE.split('-')

print(f"Processing Bronze METAR data for date: {SOURCE_DATE}")

# ============================================================================
# EXPLICIT SCHEMA
# Only fields actually used by the transformation below are included.
# Any fields present in the JSON but not listed here are simply ignored
# by Spark - this is not an exhaustive schema of the full Bronze record.
# ============================================================================

wind_schema = StructType([
    StructField("directionDegrees", IntegerType(), True),
    StructField("speedKnots", IntegerType(), True),
    StructField("gustValue", IntegerType(), True),
    StructField("cardinalDirection", StringType(), True),
    StructField("calm", BooleanType(), True),
    StructField("variable", BooleanType(), True),
])

visibility_schema = StructType([
    StructField("distanceValue", DoubleType(), True),
    StructField("unit", StringType(), True),
    StructField("lessThan", BooleanType(), True),
    StructField("cavok", BooleanType(), True),
    StructField("vfr", BooleanType(), True),
    StructField("ifr", BooleanType(), True),
])

present_weather_element_schema = StructType([
    StructField("rawCode", StringType(), True),
    StructField("intensityDescription", StringType(), True),
    StructField("thunderstorm", BooleanType(), True),
])

sky_condition_element_schema = StructType([
    StructField("heightFeet", IntegerType(), True),
    StructField("coverage", StringType(), True),
    StructField("cumulonimbus", BooleanType(), True),
    StructField("toweringCumulus", BooleanType(), True),
])

temperature_schema = StructType([
    StructField("celsius", DoubleType(), True),
    StructField("dewpointCelsius", DoubleType(), True),
    StructField("spread", DoubleType(), True),
    StructField("relativeHumidity", DoubleType(), True),
    StructField("heatIndex", DoubleType(), True),
])

pressure_schema = StructType([
    StructField("value", DoubleType(), True),
    StructField("unit", StringType(), True),
    StructField("pressureAltitudeFeet", DoubleType(), True),
])

conditions_schema = StructType([
    StructField("wind", wind_schema, True),
    StructField("visibility", visibility_schema, True),
    StructField("presentWeather", ArrayType(present_weather_element_schema), True),
    StructField("skyConditions", ArrayType(sky_condition_element_schema), True),
    StructField("temperature", temperature_schema, True),
    StructField("pressure", pressure_schema, True),
    StructField("ceilingFeet", IntegerType(), True),
])

rvr_element_schema = StructType([
    StructField("runway", StringType(), True),
    StructField("visualRangeFeet", IntegerType(), True),
    StructField("variableLow", IntegerType(), True),
    StructField("variableHigh", IntegerType(), True),
])

metadata_schema = StructType([
    StructField("processor_version", StringType(), True),
])

remarks_schema = StructType([
    StructField("seaLevelPressure", StructType([
        StructField("value", DoubleType(), True),
    ]), True),
    StructField("automatedStationType", StringType(), True),
])

bronze_schema = StructType([
    StructField("dataType", StringType(), True),
    StructField("id", StringType(), True),
    StructField("stationId", StringType(), True),
    StructField("observationTime", DoubleType(), True),
    StructField("ingestionTime", DoubleType(), True),
    StructField("latitude", DoubleType(), True),
    StructField("longitude", DoubleType(), True),
    StructField("elevationFeet", IntegerType(), True),
    StructField("rawText", StringType(), True),
    StructField("summary", StringType(), True),
    StructField("unparsedMainBody", StringType(), True),
    StructField("automated", BooleanType(), True),
    StructField("metadata", metadata_schema, True),
    StructField("remarks", remarks_schema, True),
    StructField("conditions", conditions_schema, True),
    StructField("runwayVisualRange", ArrayType(rvr_element_schema), True),
])

# ============================================================================
# READ FROM BRONZE LAYER
# ============================================================================

bronze_df = spark.read \
    .schema(bronze_schema) \
    .json(f"s3://noakweather-data/bronze/speed-layer/noaa/metar/{year}/{month}/{day}/")

# Rename dataType to observation_type_raw immediately
bronze_df = bronze_df.withColumnRenamed("dataType", "observation_type_raw")

print(f"Loaded {bronze_df.count()} records from Bronze layer")

# ============================================================================
# TRANSFORMATION: Flatten and standardize
# ============================================================================

# Define column references (these are already Column objects - do NOT wrap
# them in col() again anywhere below, just use the variable directly)
present_weather_col = col("conditions.presentWeather")
sky_conditions_col = col("conditions.skyConditions")
runway_visual_range_col = col("runwayVisualRange")
ceiling_feet_col = col("conditions.ceilingFeet")
pressure_value_col = col("conditions.pressure.value")
visibility_distanceValue_col = col("conditions.visibility.distanceValue")
visibility_unit_col = col("conditions.visibility.unit")

# Visibility converted to meters, for use in flight category / fog / marginal
# VFR / low IFR threshold comparisons below. Must match the conversion logic
# used for the visibility_meters output column (see select() below) - see
# module docstring for why this can't just reference visibility_meters by name.
visibility_meters_expr = (
    when(visibility_unit_col == "M", visibility_distanceValue_col)
    .when(visibility_unit_col == "SM", visibility_distanceValue_col * lit(1609.34))
    .otherwise(visibility_distanceValue_col)
)

# noinspection PyTypeChecker
silver_df = bronze_df.select(
    # Core identification
    col("id").alias("observation_id"),
    col("observation_type_raw").alias("observation_type"),
    col("stationId").alias("station_id"),

    # Timestamps
    from_unixtime(col("observationTime")).cast("timestamp").alias("observation_time"),
    from_unixtime(col("ingestionTime")).cast("timestamp").alias("ingestion_time"),
    current_timestamp().alias("processing_time"),

    # Location
    col("latitude"),
    col("longitude"),
    (col("elevationFeet") * lit(0.3048)).cast(IntegerType()).alias("elevation_meters"),

    # Temperature (already in Celsius from Bronze)
    col("conditions.temperature.celsius").cast(DoubleType()).alias("temperature_celsius"),
    col("conditions.temperature.dewpointCelsius").cast(DoubleType()).alias("dewpoint_celsius"),
    col("conditions.temperature.spread").cast(DoubleType()).alias("temperature_spread_celsius"),
    col("conditions.temperature.relativeHumidity").cast(DoubleType()).alias("relative_humidity_percent"),
    col("conditions.temperature.heatIndex").cast(DoubleType()).alias("heat_index_celsius"),

    # Wind (already in knots from Bronze)
    col("conditions.wind.directionDegrees").cast(IntegerType()).alias("wind_direction_degrees"),
    col("conditions.wind.speedKnots").cast(IntegerType()).alias("wind_speed_knots"),
    col("conditions.wind.gustValue").cast(IntegerType()).alias("wind_gust_knots"),
    col("conditions.wind.cardinalDirection").alias("wind_cardinal_direction"),
    col("conditions.wind.calm").cast(BooleanType()).alias("is_wind_calm"),
    col("conditions.wind.variable").cast(BooleanType()).alias("is_wind_variable"),

    # Visibility (convert to meters if needed)
    when(visibility_unit_col == "M",
         visibility_distanceValue_col)
    .when(visibility_unit_col == "SM",
          visibility_distanceValue_col * lit(1609.34))  # statute miles to meters
    .otherwise(visibility_distanceValue_col)
    .cast(DoubleType()).alias("visibility_meters"),

    col("conditions.visibility.lessThan").cast(BooleanType()).alias("is_visibility_less_than"),
    col("conditions.visibility.cavok").cast(BooleanType()).alias("is_cavok"),

    # Pressure (convert to hPa if needed)
    when(col("conditions.pressure.unit") == "HECTOPASCALS",
         pressure_value_col)
    .when(col("conditions.pressure.unit") == "INCHES_HG",
          pressure_value_col * lit(33.8639))  # inHg to hPa
    .otherwise(pressure_value_col)
    .cast(DoubleType()).alias("pressure_hpa"),

    col("conditions.pressure.pressureAltitudeFeet").cast(DoubleType()).alias("pressure_altitude_feet"),
    col("remarks.seaLevelPressure.value").cast(DoubleType()).alias("sea_level_pressure_hpa"),
    col("summary").alias("observation_summary"),

    # Sky conditions
    ceiling_feet_col.cast(IntegerType()).alias("ceiling_feet"),
    (ceiling_feet_col * lit(0.3048)).cast(IntegerType()).alias("ceiling_meters"),

    # Get the lowest cloud base from skyConditions array
    when(size(sky_conditions_col) > lit(0),
         sky_conditions_col[0].heightFeet)
    .otherwise(None).cast(IntegerType()).alias("lowest_cloud_base_feet"),

    # Determine overall sky coverage
    when(size(sky_conditions_col) == lit(0), lit("CLR"))
    .otherwise(sky_conditions_col[0].coverage)
    .alias("sky_coverage"),

    # Check for significant cloud types
    when(size(sky_conditions_col) > lit(0),
         sky_conditions_col[0].cumulonimbus)
    .otherwise(lit(False)).cast(BooleanType()).alias("has_cumulonimbus"),

    when(size(sky_conditions_col) > lit(0),
         sky_conditions_col[0].toweringCumulus)
    .otherwise(lit(False)).cast(BooleanType()).alias("has_towering_cumulus"),

    # Weather phenomena
    when(size(present_weather_col) > lit(0),
         concat_ws(",", col("conditions.presentWeather.rawCode")))
    .otherwise(None).alias("present_weather"),

    when(size(present_weather_col) > lit(0),
         present_weather_col[0].intensityDescription)
    .otherwise(None).alias("weather_intensity"),

    when(size(present_weather_col) > lit(0),
         present_weather_col[0].thunderstorm)
    .otherwise(lit(False)).cast(BooleanType()).alias("has_thunderstorm"),

    (size(present_weather_col) > lit(0)).cast(BooleanType()).alias("has_precipitation"),

    # Check for fog based on visibility (meters)
    (visibility_meters_expr < lit(1000)).cast(BooleanType()).alias("has_fog"),

    # Flight category calculation (thresholds are in meters)
    when((visibility_meters_expr < lit(1609)) |
         (ceiling_feet_col < lit(500)), lit("LIFR"))
    .when((visibility_meters_expr < lit(4828)) |
          (ceiling_feet_col < lit(1000)), lit("IFR"))
    .when((visibility_meters_expr < lit(8045)) |
          (ceiling_feet_col < lit(3000)), lit("MVFR"))
    .otherwise(lit("VFR")).alias("flight_category"),

    # Flight category booleans
    col("conditions.visibility.vfr").cast(BooleanType()).alias("is_vfr"),
    col("conditions.visibility.ifr").cast(BooleanType()).alias("is_ifr"),

    # Marginal VFR and Low IFR (derived, thresholds in meters)
    when((visibility_meters_expr >= lit(4828)) &
         (visibility_meters_expr < lit(8045)), lit(True))
    .otherwise(lit(False)).cast(BooleanType()).alias("is_marginal_vfr"),

    when((visibility_meters_expr < lit(1609)) |
         (ceiling_feet_col < lit(500)), lit(True))
    .otherwise(lit(False)).cast(BooleanType()).alias("is_low_ifr"),

    # RVR (take first element if array exists)
    when(size(runway_visual_range_col) > lit(0),
         runway_visual_range_col[0].runway)
    .otherwise(None).alias("rvr_runway_id"),

    when(size(runway_visual_range_col) > lit(0),
         runway_visual_range_col[0].visualRangeFeet)
    .otherwise(None).cast(IntegerType()).alias("rvr_visual_range_feet"),

    when(size(runway_visual_range_col) > lit(0),
         runway_visual_range_col[0].variableLow)
    .otherwise(None).cast(IntegerType()).alias("rvr_variable_low_feet"),

    when(size(runway_visual_range_col) > lit(0),
         runway_visual_range_col[0].variableHigh)
    .otherwise(None).cast(IntegerType()).alias("rvr_variable_high_feet"),

    # Raw data preservation
    col("rawText").alias("raw_text"),
    col("unparsedMainBody").alias("unparsed_tokens"),

    # Metadata
    col("metadata.processor_version").alias("processor_version"),
    col("automated").cast(BooleanType()).alias("is_automated"),
    col("remarks.automatedStationType").alias("automated_station_type"),

    # Partitions
    lit(year).alias("year"),
    lit(month).alias("month"),
    lit(day).alias("day")
)

# ============================================================================
# DATA QUALITY SCORING
# ============================================================================

# Calculate completeness score (0-100)
silver_df = silver_df.withColumn(
    "has_temperature",
    expr("temperature_celsius IS NOT NULL").cast(BooleanType())
).withColumn(
    "has_wind",
    expr("wind_speed_knots IS NOT NULL").cast(BooleanType())
).withColumn(
    "has_pressure",
    expr("pressure_hpa IS NOT NULL").cast(BooleanType())
).withColumn(
    "has_visibility",
    expr("visibility_meters IS NOT NULL").cast(BooleanType())
).withColumn(
    "completeness_score",
    (
            when(col("has_temperature"), lit(25)).otherwise(lit(0)) +
            when(col("has_wind"), lit(25)).otherwise(lit(0)) +
            when(col("has_pressure"), lit(25)).otherwise(lit(0)) +
            when(col("has_visibility"), lit(25)).otherwise(lit(0))
    ).cast(DoubleType())
)

# Calculate quality score (0-100)
silver_df = silver_df.withColumn(
    "quality_score",
    (
        # Start with completeness
        col("completeness_score") * lit(0.6) +
        # Bonus for valid ranges
        when((col("temperature_celsius") >= lit(-90)) &
             (col("temperature_celsius") <= lit(60)), lit(10)).otherwise(lit(0)) +
        when((col("wind_speed_knots") >= lit(0)) &
             (col("wind_speed_knots") <= lit(200)), lit(10)).otherwise(lit(0)) +
        when((col("pressure_hpa") >= lit(870)) &
             (col("pressure_hpa") <= lit(1085)), lit(10)).otherwise(lit(0)) +
        # Penalty for unparsed tokens
        when(expr("unparsed_tokens IS NOT NULL"), lit(-10)).otherwise(lit(0))
    ).cast(DoubleType())
)

# Validation flags
silver_df = silver_df.withColumn(
    "validation_flags",
    concat_ws(",",
              when((col("temperature_celsius") < lit(-90)) |
                   (col("temperature_celsius") > lit(60)), lit("TEMP_OUT_OF_RANGE")).otherwise(None),
              when((col("wind_speed_knots") < lit(0)) |
                   (col("wind_speed_knots") > lit(200)), lit("WIND_OUT_OF_RANGE")).otherwise(None),
              when((col("pressure_hpa") < lit(870)) |
                   (col("pressure_hpa") > lit(1085)), lit("PRESSURE_OUT_OF_RANGE")).otherwise(None),
              when(expr("unparsed_tokens IS NOT NULL"), lit("HAS_UNPARSED_TOKENS")).otherwise(None)
              )
)

# Data source
silver_df = silver_df.withColumn(
    "data_source",
    lit("noaa")
)

# ============================================================================
# WRITE TO SILVER LAYER
# ============================================================================

output_path = "s3://noakweather-data/silver/observations/"

silver_df.write \
    .mode("overwrite") \
    .partitionBy("data_source", "year", "month", "day") \
    .parquet(output_path, compression="snappy")

print(f"Successfully wrote {silver_df.count()} records to Silver layer: {output_path}")

job.commit()
```

### Create AWS Glue Job
```bash
# Upload Glue job script to S3
aws s3 cp glue-jobs/bronze_to_silver_metar.py \
    s3://noakweather-glue-scripts/bronze_to_silver_metar.py

# Create Glue job
aws glue create-job \
    --name bronze-to-silver-metar \
    --role AWSGlueServiceRole-NoakWeather \
    --command "Name=glueetl,ScriptLocation=s3://noakweather-glue-scripts/bronze_to_silver_metar.py,PythonVersion=3" \
    --default-arguments '{
        "--job-language":"python",
        "--TempDir":"s3://noakweather-glue-scripts/temp/",
        "--enable-metrics":"true",
        "--enable-continuous-cloudwatch-log":"true",
        "--enable-spark-ui":"true",
        "--spark-event-logs-path":"s3://noakweather-data/glue-spark-logs/"
    }' \
    --max-retries 1 \
    --timeout 60 \
    --glue-version "4.0" \
    --number-of-workers 2 \
    --worker-type G.1X

# Run job for specific date
aws glue start-job-run \
    --job-name bronze-to-silver-metar \
    --arguments '{"--source_date":"2026-02-08"}'
```

### Verify Silver Layer
```sql
-- Check Silver data
SELECT COUNT(*) as total_records
FROM noakweather.silver_observations
WHERE data_source = 'noaa'
  AND year = '2026'
  AND month = '02'
  AND day = '08';

-- Quality metrics
SELECT 
    AVG(quality_score) as avg_quality,
    SUM(CASE WHEN validation_passed THEN 1 ELSE 0 END) as passed_validation,
    COUNT(*) as total
FROM noakweather.silver_observations
WHERE year = '2026' AND month = '02' AND day = '08';

-- Sample data
SELECT 
    station_id,
    observation_time,
    temperature_celsius,
    wind_speed_knots,
    flight_category,
    quality_score
FROM noakweather.silver_observations
WHERE year = '2026' AND month = '02' AND day = '08'
LIMIT 10;
```

---

## Part 4: Gold Layer Setup (Data Lakehouse)

### Gold Layer Characteristics

- **Purpose:** Analytics-ready, pre-aggregated, feature-engineered
- **Format:** Apache Iceberg (ACID transactions, time travel)
- **Partitioning:** Optimized for query patterns
- **Retention:** Indefinite (compressed)
- **Query Use:** BI dashboards, ML models, real-time serving

### Enable Apache Iceberg in Athena

Athena Engine Version 3 includes native Iceberg support (already enabled in workgroup).

### DDL: Gold Current Conditions (Serving Layer)

Save as `sql/gold/create_gold_current_conditions.sql`:
```sql
-- Gold Layer: Current weather conditions (serving layer)
-- Purpose: Latest observation per station for real-time applications
-- Format: Apache Iceberg with ACID guarantees
-- Last Updated: 2026-02-08

CREATE TABLE noakweather.gold_current_conditions (
    -- Station identification
    station_id STRING,
    station_name STRING,
    latitude DOUBLE,
    longitude DOUBLE,
    elevation_feet INT,
    timezone STRING,
    
    -- Current observation
    observation_time TIMESTAMP,
    data_source STRING,
    minutes_old DOUBLE,  -- Computed: current_time - observation_time
    
    -- Temperature
    temperature_celsius DOUBLE,
    temperature_fahrenheit DOUBLE,
    dewpoint_celsius DOUBLE,
    relative_humidity DOUBLE,
    heat_index_celsius DOUBLE,
    feels_like_celsius DOUBLE,
    
    -- Pressure
    pressure_hpa DOUBLE,
    sea_level_pressure_hpa DOUBLE,
    pressure_trend_3hr STRING,  -- 'RISING', 'FALLING', 'STEADY'
    
    -- Wind
    wind_direction_degrees INT,
    wind_cardinal STRING,  -- 'N', 'NE', 'E', etc.
    wind_speed_knots DOUBLE,
    wind_speed_mph DOUBLE,
    wind_gust_knots DOUBLE,
    
    -- Visibility & Sky
    visibility_meters DOUBLE,
    visibility_miles DOUBLE,
    ceiling_feet INT,
    sky_condition STRING,  -- 'CLEAR', 'FEW', 'SCATTERED', 'BROKEN', 'OVERCAST'
    
    -- Weather
    present_weather STRING,  -- Human-readable summary
    weather_codes ARRAY<STRING>,
    
    -- Flight conditions
    flight_category STRING,  -- VFR, MVFR, IFR, LIFR
    density_altitude_feet INT,
    
    -- Computed meteorology
    cloud_base_agl_feet INT,
    freezing_level_feet INT,
    
    -- Data quality
    data_quality_score DOUBLE,
    
    -- Metadata
    last_updated TIMESTAMP
)
USING iceberg
PARTITIONED BY (bucket(100, station_id))
LOCATION 's3://noakweather-data/gold/current_conditions/'
TBLPROPERTIES (
    'format-version' = '2',
    'write.parquet.compression-codec' = 'snappy'
);
```

### DDL: Gold Historical Observations

Save as `sql/gold/create_gold_observations_historical.sql`:
```sql
-- Gold Layer: Historical observations (time-series optimized)
-- Purpose: Long-term time-series data for trend analysis
-- Format: Apache Iceberg with hourly partitioning
-- Last Updated: 2026-02-08

CREATE TABLE noakweather.gold_observations_historical (
    -- Time & Location
    observation_hour TIMESTAMP,  -- Truncated to hour
    station_id STRING,
    
    -- Hourly aggregates - Temperature
    temp_min DOUBLE,
    temp_max DOUBLE,
    temp_mean DOUBLE,
    temp_median DOUBLE,
    temp_std DOUBLE,
    
    -- Hourly aggregates - Pressure
    pressure_min DOUBLE,
    pressure_max DOUBLE,
    pressure_mean DOUBLE,
    
    -- Hourly aggregates - Wind
    wind_speed_mean DOUBLE,
    wind_speed_max DOUBLE,
    wind_gust_max DOUBLE,
    wind_direction_mode INT,  -- Most common direction
    
    -- Hourly aggregates - Visibility
    visibility_min DOUBLE,
    visibility_mean DOUBLE,
    
    -- Categorical modes
    flight_category_mode STRING,
    sky_condition_mode STRING,
    
    -- Counts & Metrics
    observation_count INT,
    imc_count INT,
    vmc_count INT,
    ceiling_below_1000_count INT,
    
    -- Weather occurrences (boolean flags)
    had_precipitation BOOLEAN,
    had_thunderstorm BOOLEAN,
    had_fog BOOLEAN,
    had_snow BOOLEAN,
    
    -- Data quality
    data_quality_mean DOUBLE,
    data_completeness_pct DOUBLE,
    
    -- Metadata
    min_observation_time TIMESTAMP,
    max_observation_time TIMESTAMP,
    processing_time TIMESTAMP
)
USING iceberg
PARTITIONED BY (months(observation_hour))
LOCATION 's3://noakweather-data/gold/observations_historical/'
TBLPROPERTIES (
    'format-version' = '2',
    'write.parquet.compression-codec' = 'snappy'
);
```

### DDL: Gold Daily Statistics

Save as `sql/gold/create_gold_daily_stats.sql`:
```sql
-- Gold Layer: Daily statistics per station
-- Purpose: BI reporting, climate analysis
-- Format: Apache Iceberg with daily partitioning
-- Last Updated: 2026-02-08

CREATE TABLE noakweather.gold_station_daily_stats (
    -- Identification
    station_id STRING,
    date DATE,
    
    -- Temperature
    temp_high DOUBLE,
    temp_low DOUBLE,
    temp_mean DOUBLE,
    temp_range DOUBLE,  -- high - low
    
    -- Departure from normals (when available)
    temp_departure_from_normal DOUBLE,
    temp_high_record BOOLEAN,  -- New record high
    temp_low_record BOOLEAN,   -- New record low
    
    -- Precipitation (when available)
    total_precip_inches DOUBLE,
    hours_with_precip INT,
    max_precip_rate_in_per_hr DOUBLE,
    
    -- Pressure
    pressure_high DOUBLE,
    pressure_low DOUBLE,
    pressure_mean DOUBLE,
    
    -- Wind
    max_wind_gust DOUBLE,
    max_wind_sustained DOUBLE,
    prevailing_wind_dir INT,
    
    -- Sky & Visibility
    hours_clear INT,
    hours_imc INT,
    hours_vmc INT,
    hours_ceiling_below_1000 INT,
    min_visibility_miles DOUBLE,
    
    -- Weather phenomena
    had_thunderstorm BOOLEAN,
    had_fog BOOLEAN,
    had_snow BOOLEAN,
    had_freezing_rain BOOLEAN,
    
    -- Data availability
    observations_received INT,
    observations_expected INT,  -- Typically 24 for hourly
    data_completeness_pct DOUBLE,
    
    -- Metadata
    processing_time TIMESTAMP
)
USING iceberg
PARTITIONED BY (years(date), months(date))
LOCATION 's3://noakweather-data/gold/daily_stats/'
TBLPROPERTIES (
    'format-version' = '2',
    'write.parquet.compression-codec' = 'snappy'
);
```

### DDL: Gold ML Features

Save as `sql/gold/create_gold_ml_features.sql`:
```sql
-- Gold Layer: ML Feature Store
-- Purpose: Pre-computed features for machine learning models
-- Format: Apache Iceberg optimized for ML workflows
-- Last Updated: 2026-02-08

CREATE TABLE noakweather.gold_ml_features (
    -- Identification
    station_id STRING,
    feature_timestamp TIMESTAMP,
    
    -- Current weather features
    temp_current DOUBLE,
    pressure_current DOUBLE,
    wind_speed_current DOUBLE,
    visibility_current DOUBLE,
    
    -- Lag features (lookback)
    temp_1hr_ago DOUBLE,
    temp_3hr_ago DOUBLE,
    temp_6hr_ago DOUBLE,
    temp_12hr_ago DOUBLE,
    temp_24hr_ago DOUBLE,
    
    pressure_1hr_ago DOUBLE,
    pressure_3hr_ago DOUBLE,
    pressure_24hr_ago DOUBLE,
    
    -- Trend features
    temp_trend_1hr DOUBLE,   -- (current - 1hr_ago) / 1hr
    temp_trend_3hr DOUBLE,
    temp_trend_24hr DOUBLE,
    
    pressure_trend_1hr DOUBLE,
    pressure_trend_3hr DOUBLE,
    pressure_trend_24hr DOUBLE,
    
    -- Rolling window aggregates
    temp_rolling_mean_6hr DOUBLE,
    temp_rolling_std_6hr DOUBLE,
    temp_rolling_mean_24hr DOUBLE,
    
    wind_speed_rolling_max_3hr DOUBLE,
    
    -- Temporal features
    hour_of_day INT,
    day_of_week INT,
    month INT,
    is_weekend BOOLEAN,
    is_night BOOLEAN,  -- Based on solar calculations
    season STRING,     -- 'SPRING', 'SUMMER', 'FALL', 'WINTER'
    
    -- Categorical encodings
    flight_category_encoded INT,  -- VFR=0, MVFR=1, IFR=2, LIFR=3
    sky_condition_encoded INT,
    
    -- Interaction features
    temp_pressure_interaction DOUBLE,  -- temp * pressure
    wind_temp_interaction DOUBLE,      -- wind * temp
    
    -- Target variables (for supervised learning)
    temp_1hr_future DOUBLE,   -- Temperature 1hr in future (for forecasting)
    temp_3hr_future DOUBLE,
    temp_6hr_future DOUBLE,
    
    flight_category_1hr_future STRING,
    
    -- Feature version (for experiment tracking)
    feature_version STRING,
    
    -- Metadata
    created_at TIMESTAMP
)
USING iceberg
PARTITIONED BY (days(feature_timestamp))
LOCATION 's3://noakweather-data/gold/ml_features/'
TBLPROPERTIES (
    'format-version' = '2',
    'write.parquet.compression-codec' = 'snappy'
);
```

### DDL: Gold Stations (Dimension Table)

Save as `sql/gold/create_gold_stations.sql`:
```sql
-- Gold Layer: Station metadata (SCD Type 2)
-- Purpose: Dimension table for station information
-- Format: Apache Iceberg with SCD Type 2 support
-- Last Updated: 2026-02-08

CREATE TABLE noakweather.gold_stations (
    -- Primary key
    station_key STRING,  -- Surrogate key (UUID)
    
    -- Station identification
    station_id STRING,  -- ICAO code (business key)
    station_name STRING,
    iata_code STRING,
    wmo_code STRING,
    
    -- Location
    latitude DOUBLE,
    longitude DOUBLE,
    elevation_feet INT,
    timezone STRING,
    country STRING,
    state_province STRING,
    city STRING,
    
    -- Classification
    station_type STRING,  -- 'AIRPORT', 'ASOS', 'AWOS', 'METAR_ONLY'
    is_major_airport BOOLEAN,
    is_military BOOLEAN,
    
    -- Operational status
    is_active BOOLEAN,
    first_observation_date DATE,
    last_observation_date DATE,
    data_sources ARRAY<STRING>,  -- ['NOAA', 'OpenWeatherMap']
    observation_frequency_minutes INT,
    
    -- Climate classification
    climate_zone STRING,  -- Köppen classification
    elevation_category STRING,  -- 'SEA_LEVEL', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH'
    
    -- SCD Type 2 fields
    effective_from TIMESTAMP,
    effective_to TIMESTAMP,
    is_current BOOLEAN,
    version INT,
    
    -- Metadata
    created_at TIMESTAMP,
    updated_at TIMESTAMP
)
USING iceberg
LOCATION 's3://noakweather-data/gold/stations/'
TBLPROPERTIES (
    'format-version' = '2',
    'write.parquet.compression-codec' = 'snappy'
);
```

### AWS Glue Job: Silver → Gold Transformation

Create file: `glue-jobs/silver_to_gold_current_conditions.py`
```python
"""
AWS Glue Job: Silver to Gold Current Conditions
Purpose: Create latest observation per station for serving layer
Author: NoakWeather Data Science Team
Last Updated: 2026-02-08
"""

import sys
from awsglue.transforms import *
from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from awsglue.context import GlueContext
from awsglue.job import Job
from pyspark.sql import functions as F
from pyspark.sql.window import Window

# Initialize
args = getResolvedOptions(sys.argv, ['JOB_NAME'])
sc = SparkContext()
glueContext = GlueContext(sc)
spark = glueContext.spark_session
job = Job(glueContext)
job.init(args['JOB_NAME'], args)

# Read from Silver
silver_df = spark.read.parquet("s3://noakweather-data/silver/observations/")

# Get latest observation per station
window_spec = Window.partitionBy("station_id").orderBy(F.col("observation_time").desc())

latest_obs = (silver_df
    .withColumn("row_num", F.row_number().over(window_spec))
    .filter(F.col("row_num") == 1)
    .drop("row_num")
)

# Transform to Gold current conditions schema
gold_current = (latest_obs
    .select(
        # Station ID
        F.col("station_id"),
        F.lit(None).cast("string").alias("station_name"),  # To be enriched from dimension
        F.lit(None).cast("double").alias("latitude"),
        F.lit(None).cast("double").alias("longitude"),
        F.lit(None).cast("int").alias("elevation_feet"),
        F.lit(None).cast("string").alias("timezone"),
        
        # Observation
        F.col("observation_time"),
        F.col("data_source"),
        ((F.unix_timestamp(F.current_timestamp()) - 
          F.unix_timestamp(F.col("observation_time"))) / 60.0).alias("minutes_old"),
        
        # Temperature
        F.col("temperature_celsius"),
        (F.col("temperature_celsius") * 9/5 + 32).alias("temperature_fahrenheit"),
        F.col("dewpoint_celsius"),
        F.col("relative_humidity"),
        F.col("heat_index_celsius"),
        F.col("temperature_celsius").alias("feels_like_celsius"),  # Simplified
        
        # Pressure
        F.col("pressure_hpa"),
        F.col("sea_level_pressure_hpa"),
        F.lit("STEADY").alias("pressure_trend_3hr"),  # To be computed from history
        
        # Wind
        F.col("wind_direction_degrees"),
        # Cardinal direction from degrees
        F.when(F.col("wind_direction_degrees").isNull(), None)
         .when(F.col("wind_direction_degrees") < 22.5, "N")
         .when(F.col("wind_direction_degrees") < 67.5, "NE")
         .when(F.col("wind_direction_degrees") < 112.5, "E")
         .when(F.col("wind_direction_degrees") < 157.5, "SE")
         .when(F.col("wind_direction_degrees") < 202.5, "S")
         .when(F.col("wind_direction_degrees") < 247.5, "SW")
         .when(F.col("wind_direction_degrees") < 292.5, "W")
         .when(F.col("wind_direction_degrees") < 337.5, "NW")
         .otherwise("N").alias("wind_cardinal"),
        
        F.col("wind_speed_knots"),
        (F.col("wind_speed_knots") * 1.15078).alias("wind_speed_mph"),
        F.col("wind_gust_knots"),
        
        # Visibility
        F.col("visibility_meters"),
        (F.col("visibility_meters") / 1609.34).alias("visibility_miles"),
        F.col("ceiling_feet"),
        F.lit("UNKNOWN").alias("sky_condition"),  # To be derived from sky conditions array
        
        # Weather
        F.col("weather_summary").alias("present_weather"),
        F.col("present_weather_codes").alias("weather_codes"),
        
        # Flight conditions
        F.col("flight_category"),
        F.lit(None).cast("int").alias("density_altitude_feet"),  # To be computed
        
        # Computed
        F.col("ceiling_feet").alias("cloud_base_agl_feet"),
        F.lit(None).cast("int").alias("freezing_level_feet"),
        
        # Quality
        F.col("quality_score").alias("data_quality_score"),
        
        # Metadata
        F.current_timestamp().alias("last_updated")
    )
)

# Write to Gold using Iceberg MERGE (upsert)
# Note: This requires Iceberg table to already exist
gold_current.createOrReplaceTempView("updates")

spark.sql("""
MERGE INTO noakweather.gold_current_conditions AS target
USING updates AS source
ON target.station_id = source.station_id
WHEN MATCHED THEN UPDATE SET *
WHEN NOT MATCHED THEN INSERT *
""")

print(f"Updated {gold_current.count()} stations in gold_current_conditions")

job.commit()
```

---

## Part 5: Query Examples

### Bronze Layer Queries
```sql
-- Debugging: Find unparsed tokens
SELECT 
    stationId,
    unparsedMainBody,
    COUNT(*) as occurrences
FROM noakweather.bronze_metar_noaa
WHERE unparsedMainBody IS NOT NULL
  AND year = '2026' AND month = '02'
GROUP BY stationId, unparsedMainBody
ORDER BY occurrences DESC
LIMIT 20;

-- Quality check: Validate Bronze ingestion
SELECT 
    year, month, day,
    COUNT(*) as total_records,
    COUNT(DISTINCT stationId) as unique_stations,
    MIN(FROM_UNIXTIME(observationTime)) as earliest_obs,
    MAX(FROM_UNIXTIME(observationTime)) as latest_obs
FROM noakweather.bronze_metar_noaa
WHERE year = '2026' AND month = '02'
GROUP BY year, month, day
ORDER BY year DESC, month DESC, day DESC;
```

### Silver Layer Queries
```sql
-- Data quality dashboard
SELECT 
    data_source,
    year, month, day,
    COUNT(*) as total_observations,
    AVG(quality_score) as avg_quality,
    SUM(CASE WHEN validation_passed THEN 1 ELSE 0 END) as passed_validation,
    ROUND(AVG(data_completeness_pct), 2) as avg_completeness
FROM noakweather.silver_observations
WHERE year = '2026' AND month = '02'
GROUP BY data_source, year, month, day
ORDER BY year DESC, month DESC, day DESC;

-- Temperature analysis
SELECT 
    station_id,
    DATE_TRUNC('hour', observation_time) as hour,
    AVG(temperature_celsius) as avg_temp,
    MIN(temperature_celsius) as min_temp,
    MAX(temperature_celsius) as max_temp,
    STDDEV(temperature_celsius) as temp_std
FROM noakweather.silver_observations
WHERE year = '2026' AND month = '02' AND day = '08'
  AND station_id IN ('KJFK', 'KLAX', 'KORD')
GROUP BY station_id, DATE_TRUNC('hour', observation_time)
ORDER BY station_id, hour;

-- Flight category trends
SELECT 
    flight_category,
    COUNT(*) as observation_count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage
FROM noakweather.silver_observations
WHERE year = '2026' AND month = '02'
  AND data_source = 'noaa'
GROUP BY flight_category
ORDER BY observation_count DESC;
```

### Gold Layer Queries
```sql
-- Current conditions for major airports
SELECT 
    station_id,
    temperature_fahrenheit,
    wind_cardinal,
    wind_speed_mph,
    visibility_miles,
    flight_category,
    present_weather,
    minutes_old
FROM noakweather.gold_current_conditions
WHERE station_id IN ('KJFK', 'KLAX', 'KORD', 'KATL', 'KDFW')
ORDER BY station_id;

-- Historical temperature trends
SELECT 
    station_id,
    DATE(observation_hour) as date,
    AVG(temp_mean) as daily_avg_temp,
    MIN(temp_min) as daily_low,
    MAX(temp_max) as daily_high
FROM noakweather.gold_observations_historical
WHERE observation_hour >= TIMESTAMP '2026-02-01'
  AND observation_hour < TIMESTAMP '2026-03-01'
  AND station_id = 'KJFK'
GROUP BY station_id, DATE(observation_hour)
ORDER BY date;

-- Daily statistics for BI reporting
SELECT 
    date,
    station_id,
    temp_high,
    temp_low,
    max_wind_gust,
    hours_imc,
    hours_vmc,
    data_completeness_pct
FROM noakweather.gold_station_daily_stats
WHERE date >= DATE '2026-02-01'
  AND station_id IN ('KJFK', 'KLAX')
ORDER BY date DESC, station_id;

-- ML features for model training
SELECT 
    station_id,
    temp_current,
    temp_1hr_ago,
    temp_trend_1hr,
    pressure_current,
    pressure_trend_3hr,
    wind_speed_current,
    hour_of_day,
    day_of_week,
    is_weekend,
    temp_1hr_future  -- Target variable
FROM noakweather.gold_ml_features
WHERE feature_timestamp >= TIMESTAMP '2026-01-01'
  AND feature_version = 'v1.0'
  AND temp_1hr_future IS NOT NULL  -- Only records with known targets
LIMIT 10000;
```

---

## Part 6: Orchestration & Automation

### AWS Step Functions Workflow

Create a state machine to orchestrate Bronze → Silver → Gold:
```json
{
  "Comment": "NoakWeather ETL Pipeline - Medallion Architecture",
  "StartAt": "BronzeToSilver",
  "States": {
    "BronzeToSilver": {
      "Type": "Task",
      "Resource": "arn:aws:states:::glue:startJobRun.sync",
      "Parameters": {
        "JobName": "bronze-to-silver-metar",
        "Arguments": {
          "--source_date.$": "$.date"
        }
      },
      "Next": "SilverToGoldCurrentConditions"
    },
    "SilverToGoldCurrentConditions": {
      "Type": "Task",
      "Resource": "arn:aws:states:::glue:startJobRun.sync",
      "Parameters": {
        "JobName": "noakweather-silver-to-gold-current"
      },
      "Next": "SilverToGoldHistorical"
    },
    "SilverToGoldHistorical": {
      "Type": "Task",
      "Resource": "arn:aws:states:::glue:startJobRun.sync",
      "Parameters": {
        "JobName": "noakweather-silver-to-gold-historical"
      },
      "Next": "Success"
    },
    "Success": {
      "Type": "Succeed"
    }
  }
}
```

### EventBridge Schedule (Daily Processing)
```bash
# Create EventBridge rule to trigger daily at 2 AM UTC
aws events put-rule \
  --name noakweather-daily-etl \
  --schedule-expression "cron(0 2 * * ? *)" \
  --state ENABLED

# Add Step Functions as target
aws events put-targets \
  --rule noakweather-daily-etl \
  --targets "Id=1,Arn=arn:aws:states:us-east-1:ACCOUNT_ID:stateMachine:noakweather-etl-pipeline,Input={\"date\":\"$( date -u -d yesterday +%Y-%m-%d)\"}"
```

---

## Part 7: Cost Optimization

### Partition Pruning (Critical!)

**❌ BAD - Full table scan:**
```sql
SELECT * FROM noakweather.silver_observations
WHERE station_id = 'KJFK';
```
**Cost:** Scans entire table (~$5/TB)

**✅ GOOD - Partition pruning:**
```sql
SELECT * FROM noakweather.silver_observations
WHERE year = '2026' AND month = '02' AND day = '08'
  AND station_id = 'KJFK';
```
**Cost:** Scans only 1 day (~$0.01)

### Columnar Storage Benefits

**❌ BAD - SELECT *:**
```sql
SELECT * FROM noakweather.silver_observations
WHERE year = '2026' AND month = '02';
```
**Scans:** All columns in Parquet files

**✅ GOOD - Specific columns:**
```sql
SELECT station_id, observation_time, temperature_celsius
FROM noakweather.silver_observations
WHERE year = '2026' AND month = '02';
```
**Scans:** Only 3 columns (80% cost reduction!)

### Compression

- Bronze (JSON): ~1 MB per 1000 records
- Silver (Parquet + Snappy): ~200 KB per 1000 records (80% savings)
- Gold (Iceberg + Snappy): ~150 KB per 1000 records (85% savings)

### Query Cost Estimates

| Query Type                        | Data Scanned | Cost (@ $5/TB) |
|-----------------------------------|--------------|----------------|
| Bronze full scan (1 month)        | 500 MB       | $0.0025        |
| Silver with partitions (1 day)    | 10 MB        | $0.00005       |
| Gold aggregated (1 month)         | 50 MB        | $0.00025       |
| Current conditions (all stations) | 5 MB         | $0.000025      |

**Monthly estimate:** ~$0.10 - $1.00 for typical analysis workloads

---

## Part 8: Monitoring & Troubleshooting

### CloudWatch Metrics

Athena automatically publishes:
- `DataScannedInBytes` - Amount scanned per query
- `EngineExecutionTime` - Query execution time
- `TotalExecutionTime` - Including queue time

### Glue Job Monitoring
```bash
# View Glue job runs
aws glue get-job-runs --job-name bronze-to-silver-metar

# View job metrics in CloudWatch
aws cloudwatch get-metric-statistics \
  --namespace Glue \
  --metric-name glue.driver.aggregate.recordsRead \
  --dimensions Name=JobName,Value=bronze-to-silver-metar \
  --start-time 2026-02-08T00:00:00Z \
  --end-time 2026-02-08T23:59:59Z \
  --period 3600 \
  --statistics Sum
```

### Common Issues

**Issue: "HIVE_PARTITION_SCHEMA_MISMATCH"**
- **Cause:** Partition column types don't match
- **Solution:** Ensure partition columns are STRING type

**Issue: "Table not found"**
- **Cause:** Database/table doesn't exist or wrong workgroup
- **Solution:** Verify with `SHOW TABLES IN noakweather`

**Issue: Slow queries**
- **Cause:** Missing partition filters or SELECT *
- **Solution:** Always include partition filters, select specific columns

**Issue: Glue job fails with OOM**
- **Cause:** Insufficient worker memory
- **Solution:** Increase worker type (G.1X → G.2X) or worker count

---

## Part 9: Data Quality & Validation

### Great Expectations Integration
```python
# Example: Data quality checks in Glue job
from great_expectations.dataset import SparkDFDataset

# After transforming to silver_df
ge_df = SparkDFDataset(silver_df)

# Validation rules
ge_df.expect_column_values_to_be_between("temperature_celsius", -80, 60)
ge_df.expect_column_values_to_be_between("wind_speed_knots", 0, 200)
ge_df.expect_column_values_to_not_be_null("station_id")
ge_df.expect_column_values_to_not_be_null("observation_time")

# Get validation results
validation_results = ge_df.validate()

if not validation_results["success"]:
    raise Exception(f"Data quality check failed: {validation_results}")
```

---

## Part 10: Next Steps

### Immediate (Week 1-2)
1. ✅ Run Bronze table DDL
2. ✅ Verify Bronze queries work
3. ✅ Create Silver table DDL
4. ✅ Deploy first Glue job (Bronze → Silver)
5. ✅ Test Silver transformations

### Short-term (Week 3-4)
1. Create Gold tables (Iceberg)
2. Deploy Silver → Gold Glue jobs
3. Set up Step Functions orchestration
4. Enable EventBridge scheduling

### Medium-term (Month 2)
1. Add data quality checks (Great Expectations)
2. Build ML feature store
3. Create BI dashboards (QuickSight/Tableau)
4. Optimize query performance

### Long-term (Month 3+)
1. Add additional data sources (OpenWeatherMap, etc.)
2. Implement SCD Type 2 for stations
3. Build automated retraining pipelines
4. Add real-time streaming (Kinesis → Bronze)

---

## Appendix A: IAM Permissions

**Glue Service Role:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": [
        "arn:aws:s3:::noakweather-data/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::noakweather-data"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "glue:GetDatabase",
        "glue:GetTable",
        "glue:GetPartitions",
        "glue:CreateTable",
        "glue:UpdateTable",
        "glue:BatchCreatePartition"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:/aws-glue/*"
    }
  ]
}
```

---

## Appendix B: S3 Lifecycle Policies
```json
{
  "Rules": [
    {
      "Id": "TransitionBronzeToIA",
      "Status": "Enabled",
      "Prefix": "bronze/",
      "Transitions": [
        {
          "Days": 90,
          "StorageClass": "STANDARD_IA"
        },
        {
          "Days": 365,
          "StorageClass": "GLACIER_IR"
        }
      ]
    },
    {
      "Id": "DeleteOldGlueTemp",
      "Status": "Enabled",
      "Prefix": "glue-temp/",
      "Expiration": {
        "Days": 7
      }
    }
  ]
}
```

---

## Additional Resources

- [AWS Athena Documentation](https://docs.aws.amazon.com/athena/)
- [Apache Iceberg Documentation](https://iceberg.apache.org/)
- [AWS Glue Documentation](https://docs.aws.amazon.com/glue/)
- [Medallion Architecture Overview](https://www.databricks.com/glossary/medallion-architecture)
- [NoakWeather Project Repository](https://github.com/your-org/noakweather)

---

**Last Updated:** 2026-02-08  
**Version:** 1.16.0-SNAPSHOT  
**Architecture:** Medallion (Bronze → Silver → Gold)  
**Maintained By:** NoakWeather Engineering Team
