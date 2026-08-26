"""
AWS Glue ETL Job: Bronze to Silver - METAR Observations
Purpose: Transform raw NOAA METAR data into standardized, validated Parquet format
Author: NoakWeather Engineering Team
Last Updated: 2026-02-10

Transformations:
- Flatten nested JSON structures
- Standardize units (all temps in Celsius, winds in knots, pressure in hPa)
- Calculate flight categories (VFR/MVFR/IFR/LIFR)
- Compute data quality scores
- Convert to Parquet with Snappy compressionclear
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
from pyspark.sql.types import DoubleType, IntegerType, BooleanType

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

# Read the directory itself, not with wildcard pattern
bronze_df = spark.read \
    .option("basePath", "s3://noakweather-data/bronze/speed-layer/noaa/metar/") \
    .option("mergeSchema", "false") \
    .json(f"s3://noakweather-data/bronze/speed-layer/noaa/metar/{year}/{month}/{day}/")

# Rename dataType to observation_type_raw immediately
bronze_df = bronze_df.withColumnRenamed("dataType", "observation_type_raw")

print(f"Loaded {bronze_df.count()} records from Bronze layer")

# ============================================================================
# TRANSFORMATION: Flatten and standardize
# ============================================================================

# Define column references
present_weather_col = col("conditions.presentWeather")
sky_conditions_col = col("conditions.skyConditions")
runway_visual_range_col = col("runwayVisualRange")
ceiling_feet_col = col("conditions.ceilingFeet")
pressure_value_col = col("conditions.pressure.value")
visibility_distanceValue_col = col("conditions.visibility.distanceValue")

# noinspection PyTypeChecker
silver_df = bronze_df.select(
    # Core identification
    col("id").alias("observation_id"),
    col("observation_type_raw").alias("observation_type"),  # Changed from lit("METAR")
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
    when(col("conditions.visibility.unit") == "M",
         col(visibility_distanceValue_col))
    .when(col("conditions.visibility.unit") == "SM",
          col(visibility_distanceValue_col) * lit(1609.34))  # statute miles to meters
    .otherwise(col(visibility_distanceValue_col))
    .cast(DoubleType()).alias("visibility_meters"),

    col("conditions.visibility.lessThan").cast(BooleanType()).alias("is_visibility_less_than"),
    col("conditions.visibility.cavok").cast(BooleanType()).alias("is_cavok"),

    # Pressure (convert to hPa if needed)
    when(col("conditions.pressure.unit") == "HECTOPASCALS",
         col(pressure_value_col))
    .when(col("conditions.pressure.unit") == "INCHES_HG",
          col(pressure_value_col) * lit(33.8639))  # inHg to hPa
    .otherwise(col(pressure_value_col))
    .cast(DoubleType()).alias("pressure_hpa"),

    col("conditions.pressure.pressureAltitudeFeet").cast(DoubleType()).alias("pressure_altitude_feet"),
    col("remarks.seaLevelPressure.value").cast(DoubleType()).alias("sea_level_pressure_hpa"),

    # Sky conditions
    col(ceiling_feet_col).cast(IntegerType()).alias("ceiling_feet"),
    (col(ceiling_feet_col) * lit(0.3048)).cast(IntegerType()).alias("ceiling_meters"),

    # Get the lowest cloud base from skyConditions array
    when(size(col(sky_conditions_col)) > lit(0),
         col(sky_conditions_col)[0].heightFeet)
    .otherwise(None).cast(IntegerType()).alias("lowest_cloud_base_feet"),

    # Determine overall sky coverage
    when(size(col(sky_conditions_col)) == lit(0), lit("CLR"))
    .otherwise(col(sky_conditions_col)[0].coverage)
    .alias("sky_coverage"),

    # Check for significant cloud types
    when(size(col(sky_conditions_col)) > lit(0),
         col(sky_conditions_col)[0].cumulonimbus)
    .otherwise(lit(False)).cast(BooleanType()).alias("has_cumulonimbus"),

    when(size(col(sky_conditions_col)) > lit(0),
         col(sky_conditions_col)[0].toweringCumulus)
    .otherwise(lit(False)).cast(BooleanType()).alias("has_towering_cumulus"),

    # Weather phenomena
    when(size(col(present_weather_col)) > lit(0),
         concat_ws(",", col("conditions.presentWeather.rawCode")))
    .otherwise(None).alias("present_weather"),

    when(size(col(present_weather_col)) > lit(0),
         col(present_weather_col)[0].intensityDescription)
    .otherwise(None).alias("weather_intensity"),

    when(size(col(present_weather_col)) > lit(0),
         col(present_weather_col)[0].thunderstorm)
    .otherwise(lit(False)).cast(BooleanType()).alias("has_thunderstorm"),

    (size(present_weather_col) > lit(0)).cast(BooleanType()).alias("has_precipitation"),

    # Check for fog based on visibility
    (col(visibility_distanceValue_col) < lit(1000)).cast(BooleanType()).alias("has_fog"),

    # Flight category calculation
    when((col(visibility_distanceValue_col) < 1609) |
         (col(ceiling_feet_col) < lit(500)), lit("LIFR"))
    .when((col(visibility_distanceValue_col) < 4828) |
          (col(ceiling_feet_col) < lit(1000)), lit("IFR"))
    .when((col(visibility_distanceValue_col) < 8045) |
          (col(ceiling_feet_col) < lit(3000)), lit("MVFR"))
    .otherwise(lit("VFR")).alias("flight_category"),

    # Flight category booleans
    col("conditions.visibility.vfr").cast(BooleanType()).alias("is_vfr"),
    col("conditions.visibility.ifr").cast(BooleanType()).alias("is_ifr"),

    # Marginal VFR and Low IFR (derived)
    when((col(visibility_distanceValue_col) >= lit(4828)) &
         (col(visibility_distanceValue_col) < lit(8045)), lit(True))
    .otherwise(lit(False)).cast(BooleanType()).alias("is_marginal_vfr"),

    when((col(visibility_distanceValue_col) < lit(1609)) |
         (col(ceiling_feet_col) < lit(500)), lit(True))
    .otherwise(lit(False)).cast(BooleanType()).alias("is_low_ifr"),

    # RVR (take first element if array exists)
    when(size(col(runway_visual_range_col)) > lit(0),
         col(runway_visual_range_col)[0].runwayId)
    .otherwise(None).alias("rvr_runway_id"),

    when(size(col(runway_visual_range_col)) > lit(0),
         col(runway_visual_range_col)[0].visualRangeFeet)
    .otherwise(None).cast(IntegerType()).alias("rvr_visual_range_feet"),

    when(size(col(runway_visual_range_col)) > lit(0),
         col(runway_visual_range_col)[0].variableLow)
    .otherwise(None).cast(IntegerType()).alias("rvr_variable_low_feet"),

    when(size(col(runway_visual_range_col)) > lit(0),
         col(runway_visual_range_col)[0].variableHigh)
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

# Summary
silver_df = silver_df.withColumn(
    "observation_summary",
    col("summary")
    ).withColumn(
    "data_source",
    lit("noaa")
)

# ============================================================================
# WRITE TO SILVER LAYER
# ============================================================================

output_path = f"s3://noakweather-data/silver/observations/noaa/{year}/{month}/{day}/"

silver_df.write \
    .mode("overwrite") \
    .partitionBy("data_source", "year", "month", "day") \
    .parquet(output_path, compression="snappy")

print(f"Successfully wrote {silver_df.count()} records to Silver layer: {output_path}")

job.commit()
