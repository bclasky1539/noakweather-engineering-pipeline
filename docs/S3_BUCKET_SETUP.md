# S3 Bucket Setup for NoakWeather Platform

> **Current state (November 2026).** Data is now written under `bronze/` (`bronze/raw-data/`,
> `bronze/speed-layer/`, `bronze/batch-layer/`), with `silver/` and `gold/` alongside. The
> lifecycle rules below still target the original root prefixes, so they no longer affect
> current data: nothing under `bronze/` is expired or archived. Reviewing the rules is tracked
> in the Platform Operations project; this guide will be updated with that change.

## Create the S3 Bucket

### Option 1: Using AWS CLI (Recommended)

```bash
# Create the bucket in us-east-1
aws s3api create-bucket \
    --bucket noakweather-data \
    --region us-east-1

# Verify bucket was created
aws s3 ls | grep noakweather-data
```

**Note**: If you want to use a different region (e.g., us-west-2), you need to specify a location constraint:

```bash
# For regions OTHER than us-east-1
aws s3api create-bucket \
    --bucket noakweather-data \
    --region us-west-2 \
    --create-bucket-configuration LocationConstraint=us-west-2
```

### Option 2: Using AWS Console

1. Go to AWS S3 Console: https://console.aws.amazon.com/s3/
2. Click "Create bucket"
3. Bucket name: `noakweather-data`
4. AWS Region: `us-east-1` (or your preferred region)
5. Leave other settings as default
6. Click "Create bucket"

---

## Configure Bucket Settings (Optional but Recommended)

### Enable Versioning (for data recovery)

```bash
aws s3api put-bucket-versioning \
    --bucket noakweather-data \
    --versioning-configuration Status=Enabled
```

### Add Lifecycle Policy (for cost optimization)

Create a lifecycle policy to automatically delete old data:

```bash
# Create lifecycle-policy.json
cat > /tmp/lifecycle-policy.json << 'EOF'
{
    "Rules": [
        {
            "ID": "DeleteOldSpeedLayerData",
            "Status": "Enabled",
            "Filter": {
                "Prefix": "speed-layer/"
            },
            "Expiration": {
                "Days": 30
            },
            "NoncurrentVersionExpiration": {
                "NoncurrentDays": 7
            }
        },
        {
            "ID": "ArchiveOldRawData",
            "Status": "Enabled",
            "Filter": {
                "Prefix": "raw-data/"
            },
            "Transitions": [
                {
                    "Days": 90,
                    "StorageClass": "GLACIER"
                }
            ]
        }
    ]
}
EOF

# Apply the lifecycle policy
aws s3api put-bucket-lifecycle-configuration \
    --bucket noakweather-data \
    --lifecycle-configuration file:///tmp/lifecycle-policy.json

# Check that the lifecycle policy was applied
aws s3api get-bucket-lifecycle-configuration --bucket noakweather-data
```

**What this does** (for the original root layout):
- `speed-layer/` JSON: deleted after 30 days
- `raw-data/` text: moved to Glacier after 90 days (Glacier files must be restored before they
  can be read)

These prefixes do not match the current `bronze/` layout; see the note at the top.

### Add Bucket Tags (for cost tracking)

```bash
aws s3api put-bucket-tagging \
    --bucket noakweather-data \
    --tagging 'TagSet=[{Key=Project,Value=NoakWeather},{Key=Environment,Value=Production},{Key=CostCenter,Value=DataEngineering}]'
```

---

## Create Folder Structure (Optional)

S3 does not have real folders, but we can create the prefix structure for clarity:

```bash
# Create marker files to establish folder structure
aws s3api put-object --bucket noakweather-data --key bronze/raw-data/
aws s3api put-object --bucket noakweather-data --key bronze/speed-layer/
aws s3api put-object --bucket noakweather-data --key bronze/batch-layer/
aws s3api put-object --bucket noakweather-data --key silver/
aws s3api put-object --bucket noakweather-data --key gold/
```

---

## Verify Bucket Setup

### Check bucket exists
```bash
aws s3 ls | grep noakweather-data
```

### Check bucket region
```bash
aws s3api get-bucket-location --bucket noakweather-data
```

### Test upload permissions
```bash
# Create a test file
echo "Test file for NoakWeather" > /tmp/test.txt

# Upload it
aws s3 cp /tmp/test.txt s3://noakweather-data/test/test.txt

# List to verify
aws s3 ls s3://noakweather-data/test/

# Download to verify
aws s3 cp s3://noakweather-data/test/test.txt /tmp/test-download.txt
cat /tmp/test-download.txt

# Clean up test
aws s3 rm s3://noakweather-data/test/test.txt
```

---

## Expected Final Bucket Structure

After the ingestion system and the Glue job run, the bucket looks like this:

```
s3://noakweather-data/
├── bronze/
│   ├── raw-data/                    # Raw report text, as fetched
│   │   └── noaa/{metar,taf}/YYYY/MM/DD/<STATION>_<YYYYMMDD>_<HHMM>.txt
│   ├── speed-layer/                 # Parsed JSON (Bronze records)
│   │   └── noaa/{metar,taf}/YYYY/MM/DD/<STATION>_<YYYYMMDD>_<HHMM>.json
│   └── batch-layer/                 # Reserved for batch processing results
├── silver/
│   └── observations/                # Parquet written by the Glue job
│       └── data_source=noaa/year=YYYY/month=MM/day=DD/part-*.snappy.parquet
├── gold/                            # Reserved for the Gold layer (planned)
└── raw-data/                        # Legacy: raw text from the original root layout
```

Dates are UTC, and `<HHMM>` is the time of the ingestion run (UTC).

---

## Cost Considerations

**Estimated Monthly Costs** (assuming 100 stations, fetched every 5 minutes):

- Storage: ~$0.50/month (for speed layer + raw data)
- Requests: ~$0.10/month (PUT requests for uploads)
- Data Transfer: Minimal (within AWS)

**Total**: < $1.00/month

**Cost Optimization**:
- Lifecycle policy moves old raw data to Glacier ($0.004/GB vs $0.023/GB)
- Deletes old speed-layer data
- (Both apply only once the rules target the current prefixes; see the note at the top.)

---

## Security Best Practices (Optional)

### Block public access (recommended)
```bash
# Create the configuration file
cat > /tmp/public-access-block.json << 'EOF'
{
    "BlockPublicAcls": true,
    "IgnorePublicAcls": true,
    "BlockPublicPolicy": true,
    "RestrictPublicBuckets": true
}
EOF

# Apply it
aws s3api put-public-access-block \
    --bucket noakweather-data \
    --public-access-block-configuration file:///tmp/public-access-block.json

# Verify It Worked
aws s3api get-public-access-block --bucket noakweather-data
```

### Enable encryption at rest
```bash
aws s3api put-bucket-encryption \
    --bucket noakweather-data \
    --server-side-encryption-configuration '{
        "Rules": [{
            "ApplyServerSideEncryptionByDefault": {
                "SSEAlgorithm": "AES256"
            }
        }]
    }'

# Verify It Worked
aws s3api get-bucket-encryption --bucket noakweather-data
```

### Set environment variables
```bash
# Check which shell you're using
echo $SHELL

# If it says /bin/bash
echo 'export S3_BUCKET=noakweather-data' >> ~/.bashrc
echo 'export AWS_REGION=us-east-1' >> ~/.bashrc
source ~/.bashrc

# If it says /bin/zsh (macOS default)
echo 'export S3_BUCKET=noakweather-data' >> ~/.zshrc
echo 'export AWS_REGION=us-east-1' >> ~/.zshrc
source ~/.zshrc

# Verify
echo $S3_BUCKET
echo $AWS_REGION
```

The Java ingestion apps read `S3_BUCKET` and `AWS_REGION`. The UAT scripts in `scripts/uat/`
take the bucket from `-b <bucket>` or `NOAKWEATHER_S3_BUCKET` instead.

---

## Troubleshooting

### Error: "AccessDenied"
Check your IAM permissions include:
- `s3:CreateBucket`
- `s3:PutObject`
- `s3:GetObject`
- `s3:ListBucket`

### Error: "InvalidBucketName"
Bucket names must:
- Be 3-63 characters
- Use only lowercase letters, numbers, hyphens
- Start and end with letter or number
- Not use underscores or periods

---
