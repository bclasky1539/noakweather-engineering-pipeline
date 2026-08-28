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
**Output:** `s3://noakweather-data/silver/observations/noaa/{year}/{month}/{day}/`

## Deployment

Deploy to AWS Glue using AWS Console or CLI:
```bash
aws glue create-job \
  --name bronze-to-silver-metar \
  --role GlueServiceRole \
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
# (Use the script I provided earlier)
