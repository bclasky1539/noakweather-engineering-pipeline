# AWS Glue Deployment Guide - Bronze to Silver ETL

**Purpose:** Deploy the Bronze → Silver METAR transformation job to AWS Glue  
**Author:** NoakWeather Engineering Team  
**Last Updated:** 2026-02-13  
**Glue Version:** 4.0 (Python 3.10, PySpark 3.3.0)

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Deployment Steps](#deployment-steps)
3. [Running the Job](#running-the-job)
4. [Verification](#verification)
5. [Monitoring](#monitoring)
6. [Troubleshooting](#troubleshooting)
7. [Scheduling (Optional)](#scheduling-optional)

---

## Prerequisites

### AWS Resources Required

- AWS Account with Glue access
- S3 buckets created:
  - `s3://noakweather-data/` (data bucket)
  - `s3://noakweather-glue-scripts/` (scripts bucket)
- Bronze layer data exists in S3
- Athena tables created (`bronze_metar_noaa`, `silver_observations`)
- AWS CLI configured with credentials

### Verify Prerequisites

```bash
# Check AWS credentials
aws sts get-caller-identity

# Verify Bronze data exists
aws s3 ls s3://noakweather-data/bronze/speed-layer/noaa/metar/2026/02/10/

# Verify Athena tables
aws athena list-table-metadata \
  --catalog-name AwsDataCatalog \
  --database-name noakweather \
  --query 'TableMetadataList[*].Name'
```

---

## Deployment Steps

### Step 1: Create S3 Bucket for Glue Scripts

```bash
# Create bucket for Glue ETL scripts
aws s3 mb s3://noakweather-glue-scripts

# Enable versioning (recommended)
aws s3api put-bucket-versioning \
  --bucket noakweather-glue-scripts \
  --versioning-configuration Status=Enabled

# Verify creation
aws s3 ls | grep glue-scripts
```

**Expected output:**
```
YYYY-MM-DD HH:MI:SS noakweather-glue-scripts

Example
2026-03-03 12:27:25 noakweather-glue-scripts
```

---

### Step 2: Upload Script to S3

```bash
# Navigate to project root
cd ~/Development/Projects/Java/noakweather-engineering-pipeline

# Upload the Bronze → Silver transformation script
aws s3 cp glue-jobs/bronze_to_silver_metar.py s3://noakweather-glue-scripts/

# Verify upload
aws s3 ls s3://noakweather-glue-scripts/
```

**Expected output:**
```
YYYY-MM-DD HH:MI:SS      XXXXX bronze_to_silver_metar.py

Example
2026-03-03 12:35:46      11812 bronze_to_silver_metar.py
```

---

### Step 3: Create IAM Role for Glue

**Option A: Using AWS Console (Recommended)**

1. Go to **IAM Console**: https://console.aws.amazon.com/iam/

2. **Navigate to Roles:**
   - Click **"Roles"** in left sidebar
   - Click **"Create role"** button

3. **Select trusted entity:**
   - **Trusted entity type:** AWS service
   - **Use case:** Glue
   - Click **"Next"**

4. **Attach permissions policies:**
   - Search and select: `AWSGlueServiceRole` (managed policy)
   - Search and select: `AmazonS3FullAccess` (or custom S3 policy below)
   - Click **"Next"**

5. **Name and create:**
   - **Role name:** `GlueServiceRole-NoakWeather`
   - **Description:** `IAM role for NoakWeather Glue ETL jobs`
   - Click **"Create role"**

**Option B: Using AWS CLI**

```bash
# Create trust policy
cat > glue-trust-policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "glue.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Create role
aws iam create-role \
  --role-name GlueServiceRole-NoakWeather \
  --assume-role-policy-document file://glue-trust-policy.json

# Attach AWS managed policy
aws iam attach-role-policy \
  --role-name GlueServiceRole-NoakWeather \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSGlueServiceRole

# Attach S3 access policy
aws iam attach-role-policy \
  --role-name GlueServiceRole-NoakWeather \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess
```

**Custom S3 Policy (More Restrictive - Recommended for Production):**

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
        "arn:aws:s3:::noakweather-data/*",
        "arn:aws:s3:::noakweather-glue-scripts/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::noakweather-data",
        "arn:aws:s3:::noakweather-glue-scripts"
      ]
    }
  ]
}
```

---

### Step 4: Add Glue Management to user

**Option A: Using AWS Console (Recommended)**

1. Go to **IAM Console**: https://console.aws.amazon.com/iam/

2. **Navigate to Roles:**
   - Click **"Users"** in left sidebar
   - Click on "noakweather-platform-dev"

3. **Add Inline Policy:**
   - Click "Permissions" tab
   - Click "Add permissions" dropdown → "Create inline policy"

4. **Switch to JSON:**
   - Click "JSON" tab
   - Paste this policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "glue:CreateJob",
        "glue:GetJob",
        "glue:UpdateJob",
        "glue:DeleteJob",
        "glue:StartJobRun",
        "glue:GetJobRun",
        "glue:GetJobRuns",
        "glue:BatchStopJobRun",
        "glue:ListJobs"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "iam:PassRole"
      ],
      "Resource": "arn:aws:iam::975050008849:role/GlueServiceRole-NoakWeather"
    }
  ]
}
```
5. **Name and Create:**
   - Click "Next"
   - **Policy name:** GlueJobManagement
   - Click "Create policy"

6. **Verify at console:**
```bash
# Check if policy is now attached
aws iam list-user-policies --user-name noakweather-platform-dev
```

**Option B: Using AWS CLI**

```bash
# Create the Glue user policy
cat > glue-user-policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "glue:CreateJob",
        "glue:GetJob",
        "glue:UpdateJob",
        "glue:DeleteJob",
        "glue:StartJobRun",
        "glue:GetJobRun",
        "glue:GetJobRuns",
        "glue:BatchStopJobRun",
        "glue:ListJobs"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "iam:PassRole"
      ],
      "Resource": "arn:aws:iam::975050008849:role/GlueServiceRole-NoakWeather"
    }
  ]
}
EOF

# Attach policy to your user
aws iam put-user-policy \
  --user-name noakweather-platform-dev \
  --policy-name GlueJobManagement \
  --policy-document file://glue-user-policy.json

# Verify it was added
aws iam list-user-policies --user-name noakweather-platform-dev
```

---

### Step 5: Create Glue Job

**Using AWS Console:**

1. **Go to AWS Glue Console**: https://console.aws.amazon.com/glue/

2. **Navigate to ETL Jobs:**
   - Click **"ETL jobs"** in left sidebar
   - Click **"Script editor"** tab
   - Click **"Create job"** button

3. **Configure Job Details:**

   **Basic Information:**
   - **Name:** `bronze-to-silver-metar`
   - **Description:** `Transform Bronze METAR JSON to Silver standardized Parquet`
   - **IAM Role:** Select `GlueServiceRole-NoakWeather`
   - **Type:** Spark
   - **Glue version:** 4.0 (Important: Matches Python 3.10 + PySpark 3.3)
   - **Language:** Python 3

   **Script:**
   - **Script filename:** `bronze_to_silver_metar.py`
   - **Script path:** `s3://noakweather-glue-scripts/bronze_to_silver_metar.py`
   
   OR paste script content directly in the editor

   **Job Details → Advanced properties:**
   - **Worker type:** G.1X (Standard)
   - **Requested number of workers:** 2 (for small datasets) or 5-10 (for larger)
   - **Job timeout:** 10 minutes
   - **Maximum retries:** 0 (for testing) or 1 (for production)

   **Job parameters:**
   Click "Add new parameter":
   - **Key:** `--source_date`
   - **Value:** `2026-02-10` (change to your test date)

4. **Save:**
   - Click **"Save"** button (top right)

**Using AWS CLI:**

```bash
# Create job
aws glue create-job \
  --name bronze-to-silver-metar \
  --role GlueServiceRole-NoakWeather \
  --command '{
    "Name": "glueetl",
    "ScriptLocation": "s3://noakweather-glue-scripts/bronze_to_silver_metar.py",
    "PythonVersion": "3"
  }' \
  --default-arguments '{
    "--source_date": "2026-02-10",
    "--TempDir": "s3://noakweather-glue-scripts/temp/",
    "--enable-metrics": "",
    "--enable-continuous-cloudwatch-log": "true",
    "--job-language": "python"
  }' \
  --glue-version "4.0" \
  --worker-type "G.1X" \
  --number-of-workers 2 \
  --timeout 10 \
  --max-retries 0 \
  --description "Transform Bronze METAR JSON to Silver standardized Parquet"

# Verify creation
aws glue get-job --job-name bronze-to-silver-metar --query 'Job.Name'
```

**Expected output:**
```
"bronze-to-silver-metar"
```

---

## Running the Job

### Option 1: Run via Console

1. **In AWS Glue Console:**
   - Navigate to **ETL jobs** → **bronze-to-silver-metar**
   - Click **"Run"** button (top right)

2. **Monitor run status:**
   - Status will change: Waiting → Starting → Running → Succeeded
   - Estimated time: 2-5 minutes for small datasets

### Option 2: Run via CLI

```bash
# Start job run (For source_date the format is YYYY-MM-DD)
aws glue start-job-run \
  --job-name bronze-to-silver-metar \
  --arguments '{"--source_date":"2026-02-08"}'

# Note the JobRunId from output, e.g., jr_abc123

# Check status
aws glue get-job-run \
  --job-name bronze-to-silver-metar \
  --run-id jr_abc123 \
  --query 'JobRun.JobRunState'
```

**Run with Different Date:**

```bash
# Process different date
aws glue start-job-run \
  --job-name bronze-to-silver-metar \
  --arguments '{"--source_date":"2026-02-11"}'
```

---

## Verification

### Step 1: Check S3 for Output Files

```bash
# List Silver layer output
aws s3 ls s3://noakweather-data/silver/observations/noaa/2026/02/10/ --recursive

# Expected output:
# 2026-02-13 XX:XX:XX  XXXXX data_source=noaa/year=2026/month=02/day=10/part-00000-xxx.snappy.parquet
```

### Step 2: Query Silver Data in Athena

```sql
-- Count records in Silver layer
SELECT COUNT(*) as record_count
FROM noakweather.silver_observations
WHERE data_source = 'noaa'
  AND year = '2026'
  AND month = '02'
  AND day = '10';

-- View sample records
SELECT 
    station_id,
    observation_time,
    temperature_celsius,
    wind_speed_knots,
    pressure_hpa,
    quality_score,
    completeness_score
FROM noakweather.silver_observations
WHERE data_source = 'noaa'
  AND year = '2026'
  AND month = '02'
  AND day = '10'
ORDER BY quality_score DESC
LIMIT 10;

-- Check data quality distribution
SELECT 
    CASE 
        WHEN quality_score >= 90 THEN 'Excellent (90-100)'
        WHEN quality_score >= 70 THEN 'Good (70-89)'
        WHEN quality_score >= 50 THEN 'Fair (50-69)'
        ELSE 'Poor (<50)'
    END as quality_tier,
    COUNT(*) as count,
    AVG(completeness_score) as avg_completeness
FROM noakweather.silver_observations
WHERE data_source = 'noaa'
  AND year = '2026'
  AND month = '02'
  AND day = '10'
GROUP BY 1
ORDER BY 1;
```

### Step 3: Compare Bronze vs Silver Record Counts

```sql
-- Bronze count
SELECT COUNT(*) as bronze_count
FROM noakweather.bronze_metar_noaa
WHERE year = '2026' AND month = '02' AND day = '10';

-- Silver count
SELECT COUNT(*) as silver_count
FROM noakweather.silver_observations
WHERE data_source = 'noaa'
  AND year = '2026' AND month = '02' AND day = '10';

-- Should match
```

---

## Monitoring

### CloudWatch Logs

**View via Console:**

1. Go to Glue job run details
2. Scroll to **"Logs"** section
3. Click **"Output logs"** or **"Error logs"**
4. Opens CloudWatch Logs

**View via CLI:**

```bash
# Get log stream name
aws glue get-job-run \
  --job-name bronze-to-silver-metar \
  --run-id jr_abc123 \
  --query 'JobRun.LogGroupName'

# View logs
aws logs tail /aws-glue/jobs/output \
  --follow \
  --filter-pattern "bronze-to-silver-metar"
```

### Expected Log Output

```
Processing Bronze METAR data for date: 2026-02-10
Loaded 12 records from Bronze layer
Successfully wrote 12 records to Silver layer: s3://noakweather-data/silver/observations/noaa/2026/02/10/
```

### CloudWatch Metrics

Monitor in CloudWatch Console:
- **Namespace:** AWS/Glue
- **Metrics:**
  - `glue.driver.ExecutorAllocationManager.executors.numberAllExecutors`
  - `glue.ALL.s3.filesystem.read_bytes`
  - `glue.ALL.s3.filesystem.write_bytes`

---

## Troubleshooting

### Common Issues

#### 1. Job Fails with "Access Denied" Error

**Symptom:**
```
AccessDeniedException: Access Denied
```

**Solution:**
- Verify IAM role has S3 permissions
- Check bucket policies don't deny Glue access
- Ensure Glue service role trust relationship is correct

```bash
# Verify role policies
aws iam list-attached-role-policies --role-name GlueServiceRole-NoakWeather

# Check trust relationship
aws iam get-role --role-name GlueServiceRole-NoakWeather --query 'Role.AssumeRolePolicyDocument'
```

---

#### 2. Job Fails with "No records found"

**Symptom:**
```
Loaded 0 records from Bronze layer
```

**Solution:**
- Verify Bronze data exists for the specified date
- Check S3 path format matches script expectations

```bash
# Check Bronze data
aws s3 ls s3://noakweather-data/bronze/speed-layer/noaa/metar/2026/02/10/

# Verify date parameter
# Make sure --source_date matches actual data date
```

---

#### 3. Job Times Out

**Symptom:**
```
Job timed out after 10 minutes
```

**Solution:**
- Increase timeout in job configuration
- Increase number of workers for large datasets
- Check for infinite loops or inefficient transformations

```bash
# Update job timeout
aws glue update-job \
  --job-name bronze-to-silver-metar \
  --job-update '{"Timeout": 30}'
```

---

#### 4. Python Import Errors

**Symptom:**
```
ModuleNotFoundError: No module named 'xyz'
```

**Solution:**
- AWS Glue 4.0 has specific library versions
- Avoid using libraries not available in Glue runtime
- Check: https://docs.aws.amazon.com/glue/latest/dg/aws-glue-programming-python-libraries.html

---

#### 5. Schema Mismatch Errors

**Symptom:**
```
AnalysisException: cannot resolve 'conditions.temperature.celsius'
```

**Solution:**
- Verify Bronze JSON structure matches script expectations
- Check field names are case-sensitive
- Inspect sample Bronze file structure

```bash
# Download and inspect Bronze file
aws s3 cp s3://noakweather-data/bronze/speed-layer/noaa/metar/2026/02/10/BPHO_20260210_0356.json ./
cat BPHO_20260210_0356.json | jq .
```

---

## Scheduling (Optional)

### Set Up Daily Processing

**Using AWS Glue Triggers:**

1. **In Glue Console:**
   - Navigate to **"Triggers"**
   - Click **"Add trigger"**
   - **Name:** `daily-bronze-to-silver-metar`
   - **Trigger type:** Schedule
   - **Frequency:** Daily
   - **Start time:** 02:00 UTC (after ingestion completes)
   - **Jobs to trigger:** bronze-to-silver-metar
   - **Arguments:** `--source_date` = `#{format(addDays(currentDate, -1), 'yyyy-MM-dd')}` (process yesterday)

**Using EventBridge (Recommended):**

```bash
# Create EventBridge rule
aws events put-rule \
  --name daily-metar-etl \
  --schedule-expression "cron(0 2 * * ? *)" \
  --description "Trigger Bronze to Silver METAR ETL daily at 2 AM UTC"

# Add Glue job as target
aws events put-targets \
  --rule daily-metar-etl \
  --targets '[{
    "Id": "1",
    "Arn": "arn:aws:glue:us-east-1:ACCOUNT_ID:job/bronze-to-silver-metar",
    "RoleArn": "arn:aws:iam::ACCOUNT_ID:role/service-role/EventBridgeGlueRole",
    "Input": "{\"--source_date\": \"$(date -u -d yesterday +%Y-%m-%d)\"}"
  }]'
```

---

## Cost Optimization

### Estimated Costs (us-east-1 region)

**For small dataset (10-100 records/day):**
- Worker type: G.1X ($0.44/DPU-hour)
- Number of workers: 2
- Typical runtime: 2-3 minutes
- **Cost per run:** ~$0.04
- **Monthly cost (30 runs):** ~$1.20

**For larger dataset (1000+ records/day):**
- Worker type: G.1X
- Number of workers: 5
- Typical runtime: 5-10 minutes
- **Cost per run:** ~$0.20
- **Monthly cost (30 runs):** ~$6.00

### Cost Reduction Tips

1. **Minimize worker count** for small datasets (2-3 workers)
2. **Process in batches** (daily vs hourly)
3. **Use partition pruning** (only read necessary partitions)
4. **Monitor and adjust timeout** (don't pay for idle time)
5. **Archive old processed data** to S3 Glacier

---

## Next Steps

After successful deployment:

1. ✅ **Set up monitoring alerts** (CloudWatch Alarms)
2. ✅ **Create Gold layer tables** (analytics-ready data)
3. ✅ **Build Silver → Gold transformation**
4. ✅ **Set up orchestration** (AWS Step Functions)
5. ✅ **Configure data quality checks** (AWS Glue Data Quality)
6. ✅ **Implement incremental processing** (process only new data)

---

## References

- [AWS Glue Documentation](https://docs.aws.amazon.com/glue/)
- [PySpark SQL Functions](https://spark.apache.org/docs/latest/api/python/reference/pyspark.sql/functions.html)
- [Parquet Format Specification](https://parquet.apache.org/docs/)
- [NoakWeather Architecture Docs](./athena-setup.md)

---

**Last Updated:** 2026-02-13  
**Maintainer:** NoakWeather Data Engineering Team
