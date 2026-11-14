# Single Station Ingestion Test

## Overview

This guide runs the METAR ingestion for **one station** and checks, by hand, everything it
writes to the Bronze layer in S3: the raw report text and the parsed JSON. KCLT (Charlotte
Douglas International) is used as the example; any active station works.

Use it to check a change end to end on live data, or to look closely at one report. For many
stations at once (a UAT round), use `scripts/uat/wethuat_metar_ingest.sh`, which runs the same
ingestion and the automated check in Step 3 for each station; see
[CONTRIBUTING.md](../CONTRIBUTING.md#uat).

Run all commands from the repository root.

---

## Step 1: Prepare

**AWS credentials.** The ingestion writes to S3, so the AWS CLI profile you use needs S3 access:

```bash
aws sts get-caller-identity
```

**Environment variables.** The ingestion apps read these, with the defaults shown:

| Variable     | Default            | Purpose                 |
|--------------|--------------------|-------------------------|
| `S3_BUCKET`  | `noakweather-data` | Bucket to write Bronze to |
| `AWS_REGION` | `us-east-1`        | AWS region              |

Set them explicitly so the commands below use the same values:

```bash
export S3_BUCKET=noakweather-data
export AWS_REGION=us-east-1
aws s3 ls "s3://$S3_BUCKET/bronze/"
```

**Build the jars.** The ingestion runs from the packaged jar:

```bash
./scripts/wethp.sh
ls noakweather-platform/weather-ingestion/target/weather-ingestion-*-SNAPSHOT.jar
```

---

## Step 2: Run the Ingestion

```bash
JAR=$(ls noakweather-platform/weather-ingestion/target/weather-ingestion-*-SNAPSHOT.jar)
java -cp "$JAR" weather.ingestion.service.source.noaa.MetarIngestionApp KCLT
```

The app fetches the station's latest report from
`https://tgftp.nws.noaa.gov/data/observations/metar/stations/KCLT.TXT`, parses it, and uploads
two files. In the output, look for:

- the configuration (bucket and region) you set in Step 1;
- a successful health check (NOAA reachable, bucket accessible);
- `Successfully ingested METAR for KCLT`;
- the S3 keys of the raw-text file and the JSON file.

Other ways to run the app:

| Command | Does |
|---------|------|
| `MetarIngestionApp KCLT KJFK KLAX` | Several stations in one run |
| `MetarIngestionApp --schedule 10 KCLT KJFK` | Repeats every 10 minutes until stopped |
| `MetarIngestionApp --interactive` | Prompts for stations |
| `TafIngestionApp KCLT` | The same for TAF reports |

Running the app with no arguments prints its usage.

---

## Step 3: Automated Check

`glue-jobs/tools/analyze_bronze_station.py` checks the station's files for one date: both
files present, valid JSON, no duplicate JSON keys, required fields present, and any unparsed
content. Use the UTC date of the run:

```bash
python3 glue-jobs/tools/analyze_bronze_station.py KCLT "$(date -u +%Y-%m-%d)" --bucket "$S3_BUCKET"
```

Exit codes: `0` = PASS, `2` = WARN (noteworthy but not a bug, e.g. unrecognized remarks
content), `1` = FAIL (structural problem). Run it in the `glue_env` conda environment (it needs
`boto3`).

---

## Step 4: Inspect the Files

Files are partitioned by **UTC** date, so a run in the evening (US time) may land in the next
day's folder. File names are `<STATION>_<YYYYMMDD>_<HHMM>.<ext>`, with the time of the
ingestion run (UTC), not the observation time in the report.

```bash
DAY=$(date -u +%Y/%m/%d)
aws s3 ls "s3://$S3_BUCKET/bronze/raw-data/noaa/metar/$DAY/" | grep KCLT
aws s3 ls "s3://$S3_BUCKET/bronze/speed-layer/noaa/metar/$DAY/" | grep KCLT
```

Set the base name of the files you want to look at (from the listing):

```bash
NAME=KCLT_20261113_1530    # example: use the name from your listing
```

### Raw text

```bash
aws s3 cp "s3://$S3_BUCKET/bronze/raw-data/noaa/metar/$DAY/$NAME.txt" -
```

It holds NOAA's response as fetched: a date/timeline, then the report.

### Parsed JSON

The full record is long. These are the fields that tell you whether the report parsed well:

```bash
aws s3 cp "s3://$S3_BUCKET/bronze/speed-layer/noaa/metar/$DAY/$NAME.json" - | jq '{
  stationId, reportType, observationTime, rawText,
  unparsedMainBody,
  remarksFreeText: .remarks.freeText,
  parseIssues,
  metadata: (.metadata | {parser_version, processor_version, storage_format, validated})
}'
```

**What to check:**

- `stationId` is `KCLT` and `rawText` is the report from the raw-text file.
- `unparsedMainBody` is null. If it is not, each group in it should have a matching entry in
  `parseIssues` (a rejected group), or be text that no handler recognizes.
- `remarks.freeText` holds only remark text the parser does not model.
- `parseIssues` is empty, or every entry is explainable: `kind`, `handler`, the `text`, and
  for a rejection the `component`, `rejectedValue` and `reason`. See
  [README.md](../README.md#parse-quality-what-was-not-parsed).
- The decoded values match the report. Compare the sections that apply, for example:
  ```bash
  aws s3 cp "s3://$S3_BUCKET/bronze/speed-layer/noaa/metar/$DAY/$NAME.json" - \
    | jq '{wind: .conditions.wind, visibility: .conditions.visibility,
           sky: .conditions.skyConditions, temperature: .conditions.temperature,
           pressure: .conditions.pressure}'
  ```

To see every top-level field, use `jq 'keys'`.

### Raw text and JSON agree

```bash
RAW_FROM_TXT=$(aws s3 cp "s3://$S3_BUCKET/bronze/raw-data/noaa/metar/$DAY/$NAME.txt" - | tail -n 1)
RAW_FROM_JSON=$(aws s3 cp "s3://$S3_BUCKET/bronze/speed-layer/noaa/metar/$DAY/$NAME.json" - | jq -r '.rawText')
[ "$RAW_FROM_TXT" = "$RAW_FROM_JSON" ] && echo "MATCH" || printf 'DIFFER\n%s\n%s\n' "$RAW_FROM_TXT" "$RAW_FROM_JSON"
```

---

## Success Criteria

1. **Run:** the app starts, the health check passes, and both S3 keys are printed.
2. **Automated check:** `analyze_bronze_station.py` exits `0` (or `2` with an explainable
   warning).
3. **Files:** the raw-text and JSON files exist under the same UTC date folder, with the same
   base name.
4. **Content:** the report text is the same in both files, decoded values match the report,
   and anything in `unparsedMainBody`, `remarks.freeText` or `parseIssues` is explainable.

---

## Troubleshooting

### "S3 bucket not accessible"

The AWS profile in use lacks S3 access, or `S3_BUCKET` names the wrong bucket. Check
`aws sts get-caller-identity`, `echo $S3_BUCKET`, and `aws s3 ls "s3://$S3_BUCKET/"`.

### "NOAA request failed"

NOAA may be temporarily unavailable. Wait a few minutes and retry, and check the station file
directly: <https://tgftp.nws.noaa.gov/data/observations/metar/stations/KCLT.TXT>.

### "No 'METAR' data available for station"

The station has no recent report. Try another active station (e.g. `KJFK`, `KATL`, `KORD`).

### No jar found

Run `./scripts/wethp.sh` first; `./scripts/wetht.sh` and `./scripts/wethb.sh` clean the
`target` folders and do not leave jars behind.

### Only one file in S3

Check the application log (`$NOAKWEATHER_LOG_DIR/noakweather.log`, see
[LOGGING_SETUP.md](LOGGING_SETUP.md)) for upload errors.

---

## Data Retention Note

The bucket's lifecycle rules were written for an older layout (`speed-layer/` and `raw-data/`
at the bucket root). Data under `bronze/` is currently neither expired nor archived. Files under
the legacy root `raw-data/` move to Glacier after 90 days and must be restored before they can
be read. A review of these rules is tracked in the Platform Operations project.
