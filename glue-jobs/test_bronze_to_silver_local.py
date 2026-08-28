"""
Local test version - NO AWS Glue dependencies
"""

from pyspark.sql import SparkSession
from pyspark.sql.functions import (
    col, lit, when, from_unixtime,
    current_timestamp, size, concat_ws, expr
)
from pyspark.sql.types import DoubleType, IntegerType, BooleanType

# Create Spark session
spark = SparkSession.builder \
    .appName("BronzeToSilver-LocalTest") \
    .config("spark.driver.memory", "4g") \
    .getOrCreate()

# Test parameters
SOURCE_DATE = "2026-02-10"
year, month, day = SOURCE_DATE.split('-')

print(f"Processing Bronze METAR data for date: {SOURCE_DATE}")

# Read from S3 (requires AWS credentials)
try:
    bronze_df = spark.read.json(
        f"s3://noakweather-data/bronze/speed-layer/noaa/metar/{year}/{month}/{day}/*.json"
    )
    print(f"Loaded {bronze_df.count()} records from Bronze layer")
except Exception as e:
    print(f"Error reading from S3: {e}")
    print("\nMake sure AWS credentials are configured:")
    print("  aws configure")
    print("  OR set AWS_PROFILE environment variable")
    spark.stop()
    exit(1)

# Show schema
print("\n=== Bronze Schema ===")
bronze_df.printSchema()

# Show sample data
print("\n=== Sample Bronze Data ===")
bronze_df.select("stationId", "rawText", "observationTime").show(3, truncate=False)

# ============================================================================
# TRANSFORMATION
# ============================================================================

# Define column references
present_weather_col = col("conditions.presentWeather")
sky_conditions_col = col("conditions.skyConditions")
runway_visual_range_col = col("runwayVisualRange")
ceiling_feet_col = col("conditions.ceilingFeet")
pressure_value_col = col("conditions.pressure.value")
visibility_distanceValue_col = col("conditions.visibility.distanceValue")

# Copy your entire silver_df = bronze_df.select(...) section here
# Copy from line ~70 to ~250 of your bronze_to_silver_metar.py

# For now, let's just do a simple transformation to test connectivity:
silver_df = bronze_df.select(
    col("id").alias("observation_id"),
    lit("METAR").alias("observation_type"),
    col("stationId").alias("station_id"),
    from_unixtime(col("observationTime")).cast("timestamp").alias("observation_time"),
    col("conditions.temperature.celsius").cast(DoubleType()).alias("temperature_celsius"),
    col("conditions.wind.speedKnots").cast(IntegerType()).alias("wind_speed_knots"),
    lit(year).alias("year"),
    lit(month).alias("month"),
    lit(day).alias("day"),
    lit("noaa").alias("data_source")
)

print("\n=== Sample Silver Data ===")
silver_df.show(10, truncate=False)

print(f"\nTransformation successful! Processed {silver_df.count()} records")

spark.stop()
