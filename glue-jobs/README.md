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

**Schedule:** Daily at 2 AM UTC (via Step Functions)

**Input:** `s3://noakweather-data/bronze/speed-layer/noaa/metar/{year}/{month}/{day}/`
<br>**Output:** `s3://noakweather-data/silver/observations/` (Hive-style partitioned by `data_source`, `year`, `month`, `day`)

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

**Typically invoked via** `./wethuat_metar_ingest.sh` at the repository root,
which runs it automatically after each station's ingestion during a UAT sweep.

## Deployment

Deploy to AWS Glue using AWS Console or CLI:
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

## Testing

Test locally using AWS Glue Docker container:
```bash
docker run -it -v $(pwd):/home/glue_user/workspace/ \
  amazon/aws-glue-libs:glue_libs_4.0.0_image_01 \
  spark-submit /home/glue_user/workspace/bronze_to_silver_metar.py
```

## Future Jobs
- `silver_to_gold_current_conditions.py` - Current weather conditions
- `silver_to_gold_hourly_aggregates.py` - Hourly roll-ups
- `silver_to_gold_ml_features.py` - ML feature store

# Save the Glue script

