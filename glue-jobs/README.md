# AWS Glue ETL Jobs

Batch ETL transformations for the NoakWeather Medallion Architecture.

## Ownership
**Data Engineering Team**

## Jobs

### bronze_to_silver_metar.py
Transforms raw NOAA METAR observations from Bronze layer (nested JSON) to Silver layer (flattened Parquet).

**Transformations:**
- Flatten nested JSON structures
- Standardize units (Celsius, knots, hPa, meters)
- Calculate flight categories (VFR/MVFR/IFR/LIFR)
- Compute data quality scores
- Convert to Parquet with Snappy compression

The job reads Bronze with an explicit schema that lists only the fields it uses; other Bronze
fields are ignored. A change to the Bronze model therefore only affects this job if it renames
or removes one of those fields.

**Schedule:** None yet. The job is deployed as `bronze-to-silver-metar` and started manually for
each date (see [Deployment](#deployment)). Scheduled orchestration (Step Functions or
EventBridge) is planned.

**Input:** `s3://noakweather-data/bronze/speed-layer/noaa/metar/{year}/{month}/{day}/`
<br>**Output:** `s3://noakweather-data/silver/observations/` (Hive-style partitioned by `data_source`, `year`, `month`, `day`)

The Athena tables for both layers are defined in `sql/bronze/create_bronze_metar_noaa.sql` and
`sql/silver/create_silver_observations.sql`; see [ATHENA_SETUP.md](../docs/ATHENA_SETUP.md).

## Tools

### tools/analyze_bronze_station.py
Standalone local CLI tool (not deployed to AWS Glue) for validating Bronze-layer
ingestion output during UAT testing. Checks a single station/date's raw-data
`.txt` and speed-layer `.json` files in S3 for structural correctness — missing
files, malformed JSON, duplicate JSON keys (a regression check for a historical
Jackson serialization bug), missing required fields, and unparsed content in
`unparsedMainBody`/`remarks.freeText`.

**Usage:**
```bash
python3 glue-jobs/tools/analyze_bronze_station.py STATION DATE [--bucket BUCKET]
# Example:
python3 glue-jobs/tools/analyze_bronze_station.py KATL 2026-09-01
```

**Exit codes:** `0` = PASS, `2` = WARN (noteworthy but not a bug, e.g. unrecognized
remarks content), `1` = FAIL (structural problem)

**Dependencies:** Uses the same `requirements.txt` as the Glue jobs above (boto3).
Run inside the `glue_env` conda environment, or any environment with `boto3`
installed.

**Typically invoked via** `./scripts/uat/wethuat_metar_ingest.sh`,
which runs it automatically after each station's ingestion during a UAT sweep.

## Deployment

The job script is stored in `s3://noakweather-glue-scripts/`. After changing
`bronze_to_silver_metar.py`, upload it again (the job definition does not need to change):
```bash
aws s3 cp glue-jobs/bronze_to_silver_metar.py s3://noakweather-glue-scripts/
```

Run the job for one date, and check its status:
```bash
aws glue start-job-run \
  --job-name bronze-to-silver-metar \
  --arguments '{"--source_date":"2026-08-27"}'

aws glue get-job-run --job-name bronze-to-silver-metar --run-id <JobRunId>
```

To create the job from scratch:
```bash
aws glue create-job \
  --name bronze-to-silver-metar \
  --role AWSGlueServiceRole-NoakWeather \
  --command "Name=glueetl,ScriptLocation=s3://noakweather-glue-scripts/bronze_to_silver_metar.py" \
  --default-arguments '{"--source_date":"2026-02-10"}' \
  --glue-version "4.0" \
  --worker-type "G.1X" \
  --number-of-workers 2
```

These commands need Glue permissions, which the pipeline's own IAM user does not have; run them
with an account or role that does. See the [Glue Deployment Guide](../docs/GLUE-DEPLOYMENT-GUIDE.md).

## Testing

`test_bronze_to_silver_local.py` tests the transformation locally. To run the job itself in the
AWS Glue Docker image, mount the `glue-jobs` directory (run this from the repository root):
```bash
docker run -it -v "$(pwd)/glue-jobs:/home/glue_user/workspace/" \
  amazon/aws-glue-libs:glue_libs_4.0.0_image_01 \
  spark-submit /home/glue_user/workspace/bronze_to_silver_metar.py
```

## Future Jobs
- `silver_to_gold_current_conditions.py` - Current weather conditions
- `silver_to_gold_hourly_aggregates.py` - Hourly roll-ups
- `silver_to_gold_ml_features.py` - ML feature store

