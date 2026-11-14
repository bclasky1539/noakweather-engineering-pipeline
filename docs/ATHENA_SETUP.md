# AWS Athena Data Lakehouse Setup - Medallion Architecture

## Overview

This document describes the complete data lakehouse implementation for NoakWeather Engineering Pipeline using AWS Athena and the Medallion Architecture pattern (Bronze → Silver → Gold layers).

**Architecture Pattern:** Medallion Architecture (Data Lakehouse)  
**Query Engine:** AWS Athena  
**Storage Format:** JSON (Bronze), Parquet (Silver), Apache Iceberg (Gold)  
**Transformation Engine:** AWS Glue + PySpark  
**Use Cases:** Historical analysis, ML feature engineering, BI reporting


### Current Status (November 2026)

| Component | Status |
|-----------|--------|
| Glue database `noakweather` | Deployed |
| Bronze table `bronze_metar_noaa` | Deployed (`sql/bronze/create_bronze_metar_noaa.sql`) |
| Silver table `silver_observations` | Deployed (`sql/silver/create_silver_observations.sql`) |
| Glue job `bronze-to-silver-metar` | Deployed; started manually per date, not scheduled |
| Scheduled orchestration (Step Functions / EventBridge) | Planned |
| Gold layer (Part 4 onward) | Planned: the Gold sections below are a design, not deployed |

Bronze is populated by the Java ingestion pipeline, which currently runs from the UAT scripts in
`scripts/uat/`. The Silver layer is reviewed against the Bronze model changes from METAR UAT
once Bronze UAT is complete.

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

The data bucket follows the Medallion Architecture. Items marked *(planned)* do not exist yet.

```bash
# View the current top-level structure
aws s3 ls s3://noakweather-data/
```

```
s3://noakweather-data/
├── bronze/                                  # Raw ingested data
│   ├── raw-data/                            # Report text as fetched (.txt)
│   │   └── noaa/{metar,taf}/YYYY/MM/DD/
│   ├── speed-layer/                         # Parsed records (.json)
│   │   └── noaa/{metar,taf}/YYYY/MM/DD/
│   └── batch-layer/                         # Reserved for batch processing results
├── silver/                                  # Standardized data (Parquet)
│   ├── observations/
│   │   └── data_source=noaa/year=YYYY/month=MM/day=DD/
│   └── forecasts/                           # (planned)
├── gold/                                    # Analytics-ready (Iceberg) (planned)
└── raw-data/                                # Legacy: raw text from the original root layout
```

Other sources (for example OpenWeatherMap) are planned and would follow the same
`<source>/<type>/YYYY/MM/DD/` layout under `bronze/`. Athena query results go to a separate
bucket, `noakweather-athena-results` (Step 2).

**Layout note:** Data was originally written to `speed-layer/` and `raw-data/` at the bucket root.
Ingestion now writes under `bronze/`, and the tables and the Glue job read from there. The root
`raw-data/` folder still holds older raw text; see [S3_BUCKET_SETUP.md](S3_BUCKET_SETUP.md).

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

AWS Athena workgroups help organize queries and control costs. Create a workgroup with Athena engine version 3 for Apache Iceberg support.

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

Athena uses the AWS Glue Data Catalog to store table metadata. Create a `noakweather` database to organize the Bronze, Silver, and Gold layer tables.

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
    - You do not need to run `USE noakweather;` in the Console - just select it from the dropdown

**Expected Result:**
- Database appears in the left sidebar dropdown
- "Tables and views" section shows "(0)" tables (add tables in the next step)
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

**Solution:** Your IAM user does not have `glue:CreateDatabase` permission. Use **Option A (Athena Query Editor)** or **Option C (Glue Console)** instead.

**Database already exists:**
```
AlreadyExistsException: Database noakweather already exists
```

**Solution:** This is fine! The database was already created. You can verify it exists by running `SHOW DATABASES;` in Athena.

**Cannot find database in Athena:**

**Solution:**
1. Refresh the page
2. Make sure you are using the correct workgroup (`noakweather-workgroup`)
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

The table definition is kept in [`sql/bronze/create_bronze_metar_noaa.sql`](../sql/bronze/create_bronze_metar_noaa.sql).
That file is the source of truth; this document no longer copies it, so the two cannot drift
apart.

The table is an external table over the JSON files written by the ingestion pipeline. It does
not list every Bronze field: fields missing from the DDL (for example `parseIssues` and
`remarks.directionalWeather`) are simply not queryable through Athena and do not cause errors.
Adding `parseIssues` to the table is part of the Silver layer review that follows Bronze UAT.

#### Using AWS Console (Recommended)

1. **Navigate to Athena Console:**
   - Go to https://console.aws.amazon.com/athena/
   - Make sure noakweather-workgroup is selected
   - Database:noakweather

2. **Execution of SQL:**
   - Copy the entire SQL from `sql/bronze/create_bronze_metar_noaa.sql` and paste it into the query editor
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

The table definition is kept in [`sql/silver/create_silver_observations.sql`](../sql/silver/create_silver_observations.sql),
which is the source of truth for the Silver schema.

### AWS Glue Job: Bronze → Silver Transformation

The job script is [`glue-jobs/bronze_to_silver_metar.py`](../glue-jobs/bronze_to_silver_metar.py).
See [`glue-jobs/README.md`](../glue-jobs/README.md) for what it does, how to deploy it, and how to
test it locally. In short, it:

- reads one day of Bronze JSON with an explicit schema (only the fields it uses);
- flattens and standardizes units (Celsius, knots, hPa, meters);
- derives flight category, fog and quality fields, and the `validation_flags` column;
- writes Snappy Parquet to `s3://noakweather-data/silver/observations/`, partitioned by
  `data_source`, `year`, `month` and `day`.

The job is deployed as `bronze-to-silver-metar` but is **not scheduled**: start it manually for
each date (below).

### Create AWS Glue Job
```bash
# Upload Glue job script to S3
aws s3 cp glue-jobs/bronze_to_silver_metar.py \
    s3://noakweather-glue-scripts/bronze_to_silver_metar.py

# Create Glue job
aws glue create-job \
    --name bronze-to-silver-metar \
    --role GlueServiceRole-NoakWeather \
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
    SUM(CASE WHEN validation_flags = '' THEN 1 ELSE 0 END) as passed_validation,
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

> **Planned, not deployed.** The Gold tables and jobs in this part are a design for later work.
> None of them exist in the `noakweather` database yet.

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
    SUM(CASE WHEN validation_flags = '' THEN 1 ELSE 0 END) as passed_validation,
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

### Done
1. ✅ Glue database `noakweather`, and the Bronze and Silver tables (`sql/`)
2. ✅ Bronze queries verified
3. ✅ Bronze → Silver Glue job deployed (`bronze-to-silver-metar`) and verified end to end

### Next (after METAR Bronze UAT)
1. Review the Silver layer against the Bronze model changes made during UAT (for example
   `parseIssues`, rejected groups in `unparsedMainBody`, and the validation ranges used in the
   quality score)
2. Add `parseIssues` to the Bronze table
3. Schedule ingestion and the Glue job (Step Functions and/or EventBridge)

### Later
1. Create Gold tables (Iceberg) and Silver → Gold Glue jobs
2. Add data quality checks (Great Expectations)
3. Build ML feature store and BI dashboards (QuickSight/Tableau)
4. Optimize query performance
5. Add additional data sources (OpenWeatherMap, etc.)
6. Implement SCD Type 2 for stations
7. Add real-time streaming (Kinesis → Bronze)

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

**Last Updated:** 2026-11-13  
**Version:** 1.32.0-SNAPSHOT  
**Architecture:** Medallion (Bronze → Silver → Gold)  
**Maintained By:** NoakWeather Engineering Team
