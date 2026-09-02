#!/usr/bin/env python3
"""
analyze_bronze_station.py

Location: glue-jobs/tools/analyze_bronze_station.py

UAT validation tool: verifies that a single station's Bronze-layer output
(both raw-data/*.txt and speed-layer/*.json) exists in S3 and passes a set
of structural sanity checks, for a given ingestion date.

This is a standalone local CLI tool - unlike the scripts in glue-jobs/,
it does not run inside AWS Glue's PySpark runtime and is never uploaded
to S3. It lives in glue-jobs/tools/ (rather than glue-jobs/ directly) to
keep that distinction clear, while still reusing glue-jobs/requirements.txt
for its one dependency (boto3), since the existing glue_env conda
environment already has it installed.

Usage:
    python3 glue-jobs/tools/analyze_bronze_station.py STATION DATE [--bucket BUCKET]

    STATION  ICAO station code, e.g. KATL
    DATE     Ingestion date (UTC) in YYYY-MM-DD format

Exit codes:
    0  PASS  - no issues found
    1  FAIL  - a structural/content problem was found (missing file,
               malformed JSON, duplicate keys, missing required fields,
               station ID mismatch, raw/JSON text mismatch)
    2  WARN  - ingestion succeeded and structure is valid, but something
               noteworthy was found that does not indicate a bug (e.g.
               unparsed tokens in the main body - this is expected/useful
               UAT signal, not a failure)

Requires: boto3 (see glue-jobs/requirements.txt), with AWS credentials
already configured (same credential chain used by the AWS CLI).
"""

import sys
import json
import argparse
from datetime import datetime

import importlib.util

if importlib.util.find_spec("boto3") is None:
    print("ERROR: boto3 is required. Install with: pip install boto3 --break-system-packages")
    sys.exit(1)

import boto3
from botocore.exceptions import ClientError

DEFAULT_BUCKET = "noakweather-data"
RAW_DATA_PREFIX_TEMPLATE = "bronze/raw-data/noaa/metar/{year}/{month}/{day}/"
SPEED_LAYER_PREFIX_TEMPLATE = "bronze/speed-layer/noaa/metar/{year}/{month}/{day}/"

REQUIRED_TOP_LEVEL_FIELDS = [
    "id", "dataType", "stationId", "observationTime", "ingestionTime",
    "rawText", "conditions",
]
REQUIRED_CONDITIONS_FIELDS = ["wind", "visibility", "temperature", "pressure"]


class ValidationReport:
    """Collects PASS/WARN/FAIL findings for a single station's validation run."""

    def __init__(self, station, date_str):
        self.station = station
        self.date_str = date_str
        self.failures = []
        self.warnings = []
        self.info = []

    def fail(self, message):
        self.failures.append(message)

    def warn(self, message):
        self.warnings.append(message)

    def note(self, message):
        self.info.append(message)

    def result(self):
        if self.failures:
            return "FAIL"
        if self.warnings:
            return "WARN"
        return "PASS"

    def exit_code(self):
        return {"PASS": 0, "WARN": 2, "FAIL": 1}[self.result()]

    def print_report(self):
        print(f"=== UAT Validation: {self.station} ({self.date_str}) ===")
        for line in self.info:
            print(f"  INFO: {line}")
        for line in self.warnings:
            print(f"  WARNING: {line}")
        for line in self.failures:
            print(f"  FAILURE: {line}")
        print(f"UAT_RESULT: {self.result()}")


def find_latest_object(s3, bucket, prefix, station):
    """Find the most recently modified object under prefix whose filename
    starts with '{station}_'. Returns (key, match_count); key is None if
    nothing matched."""
    paginator = s3.get_paginator("list_objects_v2")
    matches = []
    for page in paginator.paginate(Bucket=bucket, Prefix=prefix):
        for obj in page.get("Contents", []):
            filename = obj["Key"].rsplit("/", 1)[-1]
            if filename.startswith(f"{station}_"):
                matches.append(obj)
    if not matches:
        return None, 0
    matches.sort(key=lambda o: o["LastModified"], reverse=True)
    return matches[0]["Key"], len(matches)


def duplicate_key_pairs_hook(duplicates_log):
    """Returns a json.loads object_pairs_hook that records any duplicate
    keys found within a single JSON object, at any nesting level. This is
    a direct regression check for the Bronze layer duplicate-fields bug
    fixed earlier (Jackson serializing both nested conditions.* and
    flattened top-level convenience-getter properties). json.loads()
    would otherwise silently keep only the last value for a duplicate
    key, hiding the problem the same way it went undetected originally."""
    def hook(pairs):
        seen = set()
        for key, _ in pairs:
            if key in seen:
                duplicates_log.append(key)
            seen.add(key)
        return dict(pairs)
    return hook


def get_nested(data, *path):
    """Safely walk a nested dict, returning None if any level is missing."""
    current = data
    for key in path:
        if not isinstance(current, dict) or key not in current:
            return None
        current = current[key]
    return current


def validate_station(s3, bucket, station, date_str):
    report = ValidationReport(station, date_str)
    year, month, day = date_str.split("-")

    raw_prefix = RAW_DATA_PREFIX_TEMPLATE.format(year=year, month=month, day=day)
    json_prefix = SPEED_LAYER_PREFIX_TEMPLATE.format(year=year, month=month, day=day)

    raw_key, raw_count = find_latest_object(s3, bucket, raw_prefix, station)
    json_key, json_count = find_latest_object(s3, bucket, json_prefix, station)

    if raw_key is None:
        report.fail(f"No raw-data file found under s3://{bucket}/{raw_prefix}")
    elif raw_count > 1:
        report.note(f"{raw_count} raw-data files found for this date; validating most recent ({raw_key})")

    if json_key is None:
        report.fail(f"No speed-layer JSON file found under s3://{bucket}/{json_prefix}")
    elif json_count > 1:
        report.note(f"{json_count} speed-layer files found for this date; validating most recent ({json_key})")

    if raw_key is None or json_key is None:
        return report  # Can't do content checks without both files

    try:
        raw_obj = s3.get_object(Bucket=bucket, Key=raw_key)
        raw_text = raw_obj["Body"].read().decode("utf-8")
    except ClientError as e:
        report.fail(f"Failed to download raw-data file: {e}")
        return report

    try:
        json_obj = s3.get_object(Bucket=bucket, Key=json_key)
        json_text = json_obj["Body"].read().decode("utf-8")
    except ClientError as e:
        report.fail(f"Failed to download speed-layer JSON file: {e}")
        return report

    duplicates_found = []
    try:
        data = json.loads(json_text, object_pairs_hook=duplicate_key_pairs_hook(duplicates_found))
    except json.JSONDecodeError as e:
        report.fail(f"speed-layer JSON is not valid JSON: {e}")
        return report

    if duplicates_found:
        unique_dupes = sorted(set(duplicates_found))
        report.fail(f"Duplicate JSON keys detected (Jackson serialization regression?): {unique_dupes}")

    for field in REQUIRED_TOP_LEVEL_FIELDS:
        if field not in data:
            report.fail(f"Missing required top-level field: '{field}'")

    if data.get("dataType") != "METAR":
        report.fail(f"Expected dataType 'METAR', found '{data.get('dataType')}'")

    if data.get("stationId") != station:
        report.fail(f"Expected stationId '{station}', found '{data.get('stationId')}'")

    obs_time = data.get("observationTime")
    if not isinstance(obs_time, (int, float)):
        report.fail(f"observationTime is missing or not numeric: {obs_time!r}")

    raw_text_field = data.get("rawText")
    if not raw_text_field:
        report.fail("rawText field is missing or empty")
    elif station not in raw_text_field[:20]:
        report.warn(f"Station code '{station}' not found near start of rawText: {raw_text_field[:40]!r}")

    if raw_text_field and raw_text.strip() != raw_text_field.strip():
        report.fail(
            "raw-data file content does not match speed-layer JSON's rawText field "
            "(dual storage inconsistency)"
        )

    conditions = data.get("conditions")
    if isinstance(conditions, dict):
        for field in REQUIRED_CONDITIONS_FIELDS:
            if field not in conditions:
                report.fail(f"Missing required conditions field: 'conditions.{field}'")
    # (missing 'conditions' itself is already caught by REQUIRED_TOP_LEVEL_FIELDS)

    # Soft/warning-level checks - interesting UAT signal, not bugs
    unparsed = data.get("unparsedMainBody")
    if unparsed:
        report.warn(f"Unparsed main body tokens found: {unparsed!r}")

    remarks_free_text = get_nested(data, "remarks", "freeText")
    if remarks_free_text:
        report.warn(f"Unparsed remarks content found (remarks.freeText): {remarks_free_text!r}")

    temp_celsius = get_nested(data, "conditions", "temperature", "celsius")
    if temp_celsius is None:
        report.warn("No temperature reported (conditions.temperature.celsius is null)")

    pressure_value = get_nested(data, "conditions", "pressure", "value")
    if pressure_value is None:
        report.warn("No pressure reported (conditions.pressure.value is null)")

    return report


def main():
    parser = argparse.ArgumentParser(description="Validate a station's Bronze-layer UAT output")
    parser.add_argument("station", help="ICAO station code, e.g. KATL")
    parser.add_argument("date", help="Ingestion date (UTC), format YYYY-MM-DD")
    parser.add_argument("--bucket", default=DEFAULT_BUCKET, help=f"S3 bucket (default: {DEFAULT_BUCKET})")
    args = parser.parse_args()

    try:
        datetime.strptime(args.date, "%Y-%m-%d")
    except ValueError:
        print(f"ERROR: date must be in YYYY-MM-DD format, got '{args.date}'")
        sys.exit(1)

    s3 = boto3.client("s3")
    report = validate_station(s3, args.bucket, args.station.upper(), args.date)
    report.print_report()
    sys.exit(report.exit_code())


if __name__ == "__main__":
    main()
