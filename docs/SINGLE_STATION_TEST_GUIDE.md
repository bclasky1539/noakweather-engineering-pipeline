# Single Station Integration Test - KCLT (Charlotte Douglas International)

## Overview

This test will verify the complete dual storage pipeline with **real NOAA METAR data** from Charlotte Douglas International Airport (KCLT).
This is an example case. Certain information could be different for other test cases.

---

## Pre-Flight Checklist

### Environment Setup

**1. Set Environment Variables:**

```bash
export AWS_REGION=us-east-1
export S3_BUCKET=noakweather-data
```

**2. Verify AWS Credentials:**

```bash
# Check AWS authentication
aws sts get-caller-identity

# Expected output:
# {
#     "UserId": "...",
#     "Account": "...",
#     "Arn": "..."
# }
```

**3. Verify S3 Bucket Access:**

```bash
# Check bucket exists and is accessible
aws s3 ls s3://noakweather-data/

# Expected: Bucket listing (may be empty)
```

**4. Build the Application:**

```bash
cd ~/Development/Projects/Java/noakweather-engineering-pipeline/weather-ingestion

# Build without running tests (already passed)
mvn clean package -DskipTests

# Expected output: BUILD SUCCESS
```

---

## Test Execution

### Step 1: Run Single Station Ingestion

```bash
cd ~/Development/Projects/Java/noakweather-engineering-pipeline/weather-ingestion

# Run the METAR ingestion for KCLT
java -cp target/weather-ingestion-1.13.0-SNAPSHOT.jar \
    weather.ingestion.service.source.noaa.MetarIngestionApp KCLT
```

### Expected Console Output

```
=== METAR Ingestion Application Started ===

Configuration:
  S3 Bucket: noakweather-data
  AWS Region: us-east-1
  
System Health Check:
  ✓ NOAA API accessible
  ✓ S3 bucket accessible
  
Starting METAR ingestion for station: KCLT
Fetching METAR report for station: KCLT from https://tgftp.nws.noaa.gov/data/observations/metar/stations/KCLT.TXT

Successfully ingested METAR for KCLT in XXms
Processed weather data for station KCLT in XXms (Raw: raw-data/noaa/metar/2026/02/03/KCLT_20260203_XXXX.txt, JSON: speed-layer/noaa/metar/2026/02/03/KCLT_20260203_XXXX.json)

=== METAR Ingestion Application Completed ===
```

**Key things to look for:**
- "System Health Check" passes
- "Successfully ingested METAR for KCLT"
- Shows both raw and JSON file paths
- Date partitioning: `2026/02/03/`
- Timestamp in filename matches

---

## Step 2: Verify Files in S3

### Check Raw Text File

```bash
# List raw text files for today
aws s3 ls s3://noakweather-data/raw-data/noaa/metar/2026/02/03/ --recursive

# Expected output (example):
# 2026-02-02 19:56:22         70 raw-data/noaa/metar/2026/02/03/KCLT_20260203_0056.txt
```

**Download and inspect raw file:**

```bash
# Download the file (adjust timestamp as needed)
aws s3 cp s3://noakweather-data/raw-data/noaa/metar/2026/02/03/KCLT_20260203_0056.txt - | cat

# Expected content (example):
# KCLT 030052Z 00000KT 10SM FEW060 00/M08 A3023 RMK AO2 SLP262 T00001083%
```

**What to verify:**
- File contains raw METAR text
- Timestamp in filename matches observation time
- Station ID is KCLT
- Data format is plain text

### Check JSON File

```bash
# List JSON files for today
aws s3 ls s3://noakweather-data/speed-layer/noaa/metar/2026/02/03/ --recursive

# Expected output (example):
# 2026-02-02 19:56:22       1275 speed-layer/noaa/metar/2026/02/03/KCLT_20260203_0056.json
```

**Download and inspect JSON file:**

```bash
# Download the file (adjust timestamp as needed)
aws s3 cp s3://noakweather-data/speed-layer/noaa/metar/2026/02/03/KCLT_20260203_0056.json - | jq .

# Expected JSON structure:
{
  "dataType": "METAR",
  "id": "52b7ba3b-40bb-4c6b-a0f1-50eed2ccbd80",
  "ingestionTime": 1770080181.692608000,
  "source": "NOAA",
  "processingLayer": "SPEED_LAYER",
  "stationId": "KCLT",
  "observationTime": 1770080181.691918000,
  "location": null,
  "rawData": "KCLT 030052Z 00000KT 10SM FEW060 00/M08 A3023 RMK AO2 SLP262 T00001083",
  "qualityFlags": null,
  "metadata": {
    "fetch_timestamp": "2026-02-03T00:56:21.693224Z",
    "validated": "true",
    "processor_version": "2.1",
    "format": "TEXT",
    "full_response": "2026/02/03 00:52\nKCLT 030052Z 00000KT 10SM FEW060 00/M08 A3023 RMK AO2 SLP262 T00001083\n",
    "storage_format": "dual",
    "processor": "SpeedLayerProcessor",
    "validation_timestamp": "2026-02-02T19:56:21.693370"
  },
  "reportType": "METAR",
  "conditions": {
    "wind": null,
    "visibility": null,
    "presentWeather": [],
    "skyConditions": [],
    "temperature": null,
    "pressure": null,
    "likelyIMC": false,
    "clearAndCalm": false,
    "likelyVMC": true,
    "ceilingFeet": null
  },
  "runwayVisualRange": [],
  "rawText": null,
  "reportModifier": null,
  "latitude": null,
  "longitude": null,
  "elevationFeet": null,
  "qualityControlFlags": null,
  "remarks": null,
  "ceilingFeet": null,
  "visibility": null,
  "temperature": null,
  "pressure": null,
  "skyConditions": [],
  "presentWeather": [],
  "minimumRvrFeet": null,
  "current": true,
  "summary": "METAR from KCLT at 2026-02-03T00:56:21.691918Z",
  "wind": null
}
```

**What to verify:**
- Valid JSON format
- `stationId` = "KCLT"
- `rawText` matches raw file content
- `reportType` = "METAR"
- `source` = "NOAA"
- **Metadata contains BOTH S3 keys:**
  - `s3_raw_key` points to raw text file
  - `s3_json_key` points to JSON file
  - `s3_key` (legacy) points to JSON file
- `storage_format` = "dual"
- `processor_version` = "2.1"

---

## Step 3: Verify Dual Storage Consistency

### Check File Timestamps Match

```bash
# Get metadata for both files
aws s3api head-object --bucket noakweather-data \
    --key raw-data/noaa/metar/2026/02/03/KCLT_20260203_0056.txt

{
    "AcceptRanges": "bytes",
    "LastModified": "2026-02-03T00:56:22+00:00",
    "ContentLength": 70,
    "ETag": "\"aa52b69b5746acd4731d5d64ff522d45\"",
    "VersionId": ".THWYWC0Tq_YENfRY1W6F.d.LtO.6hSJ",
    "ContentType": "text/plain",
    "ServerSideEncryption": "AES256",
    "Metadata": {
        "station-id": "KCLT",
        "source": "NOAA",
        "ingestion-time": "2026-02-03T00:56:21.692608Z",
        "data-type": "METAR"
    }
}

aws s3api head-object --bucket noakweather-data \
    --key speed-layer/noaa/metar/2026/02/03/KCLT_20260203_0056.json

{
    "AcceptRanges": "bytes",
    "Expiration": "expiry-date=\"Fri, 06 Mar 2026 00:00:00 GMT\", rule-id=\"DeleteOldSpeedLayerData\"",
    "LastModified": "2026-02-03T00:56:22+00:00",
    "ContentLength": 1275,
    "ETag": "\"eac5c6eb6c4f3cbe220a69ae7f725419\"",
    "VersionId": "c7cWOcBZKCW24bbhZ51nOeOtY3keNzN8",
    "ContentType": "application/json",
    "ServerSideEncryption": "AES256",
    "Metadata": {
        "station-id": "KCLT",
        "report-type": "METAR",
        "source": "NOAA",
        "ingestion-time": "2026-02-03T00:56:21.692608Z"
    }
}
```

**What to verify:**
- Both files have same (or very close) `LastModified` timestamp
- Both files uploaded within seconds of each other

### Verify Content Consistency

```bash
# Extract raw text from both sources
RAW_FROM_TXT=$(aws s3 cp s3://noakweather-data/raw-data/noaa/metar/2026/02/03/KCLT_20260203_0056.txt - | tail -n 1)
RAW_FROM_JSON=$(aws s3 cp s3://noakweather-data/speed-layer/noaa/metar/2026/02/03/KCLT_20260203_0056.json - | jq -r '.rawData')

# Compare
echo "Raw file: $RAW_FROM_TXT"
echo "JSON rawText: $RAW_FROM_JSON"

# They should match!
Raw file: KCLT 030052Z 00000KT 10SM FEW060 00/M08 A3023 RMK AO2 SLP262 T00001083
JSON rawText: KCLT 030052Z 00000KT 10SM FEW060 00/M08 A3023 RMK AO2 SLP262 T00001083
```

**What to verify:**
- Raw METAR text is identical in both files

---

## Step 4: Verify Date Partitioning

```bash
# List all partitions
aws s3 ls  s3://noakweather-data/raw-data/noaa/metar/ --recursive
aws s3 ls s3://noakweather-data/speed-layer/noaa/metar/ --recursive

# Expected structure:
# 2026-02-02 19:56:22         70 raw-data/noaa/metar/2026/02/03/KCLT_20260203_0056.txt
# 2026-02-02 19:56:22       1275 speed-layer/noaa/metar/2026/02/03/KCLT_20260203_0056.json
```

**What to verify:**
- Files are partitioned by date: `YYYY/MM/DD/`
- Same partitioning in both `raw-data` and `speed-layer` paths

---

## Success Criteria

### All Must Pass:

1. **Application Run:**
   - [ ] Application starts without errors
   - [ ] Health check passes
   - [ ] METAR data fetched successfully
   - [ ] Both file paths logged with same timestamp

2. **Raw Text File:**
   - [ ] File exists in S3
   - [ ] Contains actual METAR observation
   - [ ] Filename timestamp matches observation time
   - [ ] Date partitioned correctly

3. **JSON File:**
   - [ ] File exists in S3
   - [ ] Valid JSON structure
   - [ ] Contains all required fields
   - [ ] `rawText` matches raw file content
   - [ ] Metadata contains both S3 keys
   - [ ] `storage_format` = "dual"
   - [ ] `processor_version` = "2.1"

4. **Dual Storage Consistency:**
   - [ ] Both files created within seconds
   - [ ] Same timestamp in filenames
   - [ ] Same date partitioning
   - [ ] Raw text content matches

---

## Troubleshooting

### Issue: "S3 bucket not accessible"

**Cause:** AWS credentials not configured or insufficient permissions

**Solution:**
```bash
# Re-configure AWS credentials
aws configure

# Or use environment variables
export AWS_ACCESS_KEY_ID=your_key
export AWS_SECRET_ACCESS_KEY=your_secret
```

### Issue: "NOAA request failed"

**Cause:** NOAA API may be temporarily unavailable or network issues

**Solution:**
- Wait a few minutes and retry
- Check network connectivity
- Verify NOAA URL: https://tgftp.nws.noaa.gov

### Issue: "No METAR data available for station"

**Cause:** KCLT may not have recent data (unlikely, major airport)

**Solution:**
- Try another major airport: `KJFK`, `KLAX`, `KORD`
- Check NOAA directly: https://tgftp.nws.noaa.gov/data/observations/metar/stations/KCLT.TXT

### Issue: Only one file appears in S3

**Cause:** Dual storage implementation error

**Solution:**
- Check logs for upload errors
- Verify both `uploadWeatherDataDual()` calls succeeded
- Check S3 permissions (need PutObject on both paths)

---

## Validation Commands Summary

```bash
# 1. Run ingestion
java -cp target/weather-ingestion-1.13.0-SNAPSHOT.jar \
    weather.ingestion.service.source.noaa.MetarIngestionApp KCLT

# 2. Check raw file exists
aws s3 ls s3://noakweather-data/raw-data/noaa/metar/2026/02/03/ --recursive

# 3. Check JSON file exists
aws s3 ls s3://noakweather-data/speed-layer/noaa/metar/2026/02/03/ --recursive

# 4. Download and inspect raw file
aws s3 cp s3://noakweather-data/raw-data/noaa/metar/2026/02/03/KCLT_20260203_XXXX.txt -

# 5. Download and inspect JSON file (with pretty printing)
aws s3 cp s3://noakweather-data/speed-layer/noaa/metar/2026/02/03/KCLT_20260203_XXXX.json - | jq .

# 6. Verify metadata
aws s3 cp s3://noakweather-data/speed-layer/noaa/metar/2026/02/03/KCLT_20260203_XXXX.json - | jq '.metadata'

# 7. Count files created
aws s3 ls s3://noakweather-data/ --recursive | grep KCLT | wc -l
# Expected: At least 2 (raw + JSON)
```

---

## Next Steps After Success

Once the single station test passes:

1. **Test Multiple Stations:**
   ```bash
   java -cp target/weather-ingestion-1.13.0-SNAPSHOT.jar \
       weather.ingestion.service.source.noaa.MetarIngestionApp KCLT KJFK KLAX
   ```

2. **Test Scheduled Ingestion:**
   ```bash
   # Run every 10 minutes
   java -cp target/weather-ingestion-1.13.0-SNAPSHOT.jar \
       weather.ingestion.service.source.noaa.MetarIngestionApp \
       --schedule 10 KCLT KJFK
   ```

3. **Verify TAF Dual Storage:**
   ```bash
   java -cp target/weather-ingestion-1.13.0-SNAPSHOT.jar \
       weather.ingestion.service.source.noaa.TafIngestionApp KCLT
   ```

4. **Production Deployment:**
   - Deploy to production environment
   - Monitor S3 costs (now storing 2x files)
   - Set up alerts for ingestion failures
   - Configure CloudWatch logs

---

