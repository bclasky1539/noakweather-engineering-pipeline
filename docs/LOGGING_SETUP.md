# Logging Configuration Setup Guide

## Overview

This guide explains the centralized logging configuration across all modules in the NoakWeather Engineering Pipeline. The setup ensures:

- **Single source of truth** for logging configuration
- **Consistent logging** across all modules from any execution location
- **Automatic propagation** of changes
- **Environment-aware** configuration that works from parent or child directories

## Architecture

```
noakweather-engineering-pipeline/
└── noakweather-platform/
    ├── src/main/resources/
    │   └── log4j2.xml                    ← Master configuration (single source of truth)
    ├── logs/                              ← Centralized log output directory
    │   ├── noakweather.log               ← All application logs
    │   ├── noakweather-error.log         ← Error-level logs only
    │   ├── dynamodb.log                  ← DynamoDB-specific operations
    │   └── archive/                      ← Rolled/compressed old logs
    ├── weather-common/
    │   └── pom.xml                       ← Resources plugin config
    ├── weather-storage/
    │   └── pom.xml                       ← Resources plugin config
    ├── weather-ingestion/
    │   └── pom.xml                       ← Resources plugin config
    ├── weather-processing/
    │   └── pom.xml                       ← Resources plugin config
    ├── weather-analytics/
    │   └── pom.xml                       ← Resources plugin config
    └── weather-infrastructure/
        └── pom.xml                       ← Resources plugin config
```

## Prerequisites

- Unix-like operating system (macOS, Linux) or Git Bash on Windows
- Maven 3.8+
- Java 17+
- Shell access to set environment variables

## Initial Setup

### Step 1: Set Environment Variable (Required)

Add the log directory environment variable to your shell configuration:

```bash
# For macOS/Linux with zsh (default on modern macOS)
echo 'export NOAKWEATHER_LOG_DIR="$HOME/Development/Projects/Java/noakweather-engineering-pipeline/noakweather-platform/logs"' >> ~/.zshrc
source ~/.zshrc

# For macOS/Linux with bash
echo 'export NOAKWEATHER_LOG_DIR="$HOME/Development/Projects/Java/noakweather-engineering-pipeline/noakweather-platform/logs"' >> ~/.bashrc
source ~/.bashrc

# Verify it's set
echo $NOAKWEATHER_LOG_DIR
```

**Why this is needed:** Maven's `maven.multiModuleProjectDirectory` property is only available when running from the reactor (parent) directory. When running `mvn exec:java` from a child module like `weather-storage`, the property is not set, causing logs to be created in the wrong location. The environment variable ensures consistent log location regardless of execution context.

### Step 2: Create Log Directory

```bash
mkdir -p $NOAKWEATHER_LOG_DIR
mkdir -p $NOAKWEATHER_LOG_DIR/archive
```

### Step 3: Verify Master Configuration Exists

The master `log4j2.xml` should exist at:

```bash
# Location
noakweather-platform/src/main/resources/log4j2.xml
```

Verify it contains the environment variable configuration:

```bash
cat noakweather-engineering-pipeline/noakweather-platform/src/main/resources/log4j2.xml | grep NOAKWEATHER_LOG_DIR
```

**Expected output:** Should show the property line with `${env:NOAKWEATHER_LOG_DIR:-...}`

### Step 4: Configure Maven Resources Plugin in Child Modules

Each child module needs the Maven resources plugin configuration to include the parent's log4j2.xml. Add this to the `<build>` section of each child module's `pom.xml`:

**Modules to update:**
- `weather-common/pom.xml`
- `weather-storage/pom.xml`
- `weather-ingestion/pom.xml`
- `weather-processing/pom.xml`
- `weather-analytics/pom.xml`
- `weather-infrastructure/pom.xml`

**Configuration to add:**

```xml
<build>
    <resources>
        <!-- Include parent log4j2.xml -->
        <resource>
            <directory>../src/main/resources</directory>
            <includes>
                <include>log4j2.xml</include>
            </includes>
        </resource>
        <!-- Include own resources if any -->
        <resource>
            <directory>src/main/resources</directory>
        </resource>
    </resources>
    
    <!-- Existing build plugins go here -->
</build>
```

**Important:** This configuration tells Maven to copy the parent's `log4j2.xml` into each module's `target/classes` during the build phase, ensuring the logging configuration is available at runtime.

### Step 5: Build and Verify

```bash
cd noakweather-engineering-pipeline/noakweather-platform

# Clean build
mvn clean install -DskipTests

# Verify log4j2.xml was copied to target/classes
ls -la weather-storage/target/classes/log4j2.xml
ls -la weather-common/target/classes/log4j2.xml
```

**Expected output:** You should see the log4j2.xml file in each module's target/classes directory.

### Step 6: Test the Configuration

Test from both parent and child directories to ensure it works everywhere:

```bash
# Test 1: Run from child module (weather-storage)
cd weather-storage
mvn exec:java -Dexec.mainClass="weather.storage.tools.AddGSIsToAwsTable"

# Test 2: Verify logs are in the correct location
ls -la $NOAKWEATHER_LOG_DIR/

# Test 3: View logs
tail -f $NOAKWEATHER_LOG_DIR/noakweather.log
```

**Expected:** Logs should appear in `noakweather-platform/logs/`, NOT in `weather-storage/logs/`.

## Master Configuration File

The configuration is [`noakweather-platform/src/main/resources/log4j2.xml`](../noakweather-platform/src/main/resources/log4j2.xml).
That file is the source of truth; this guide summarizes it rather than copying it.

The log directory is `${env:NOAKWEATHER_LOG_DIR}`, falling back as described in
[Configuration Priority Chain](#configuration-priority-chain).

### Appenders

| Appender       | Writes to               | Rolls over                       | Old files                                                         |
|----------------|-------------------------|----------------------------------|-------------------------------------------------------------------|
| `Console`      | standard output         | n/a                              | n/a                                                               |
| `RollingFile`  | `noakweather.log`       | on startup, daily, and at 10 MB  | archived as `.gz`; deleted after 30 days                          |
| `ErrorFile`    | `noakweather-error.log` | on startup, daily, and at 10 MB  | archived as `.gz`; at most 30 per rollover period, not deleted by age |
| `DynamoDbFile` | `dynamodb.log`          | on startup, daily, and at 5 MB   | archived as `.gz`; at most 7 per rollover period, not deleted by age  |

`ErrorFile` accepts only ERROR and FATAL events.

### Loggers

| Logger                               | Level | Appenders                         |
|--------------------------------------|-------|-----------------------------------|
| Root                                 | INFO  | Console, RollingFile, ErrorFile   |
| `weather` (all platform code)        | INFO  | Console, RollingFile, ErrorFile   |
| `weather.storage.repository.dynamodb`| DEBUG | Console, DynamoDbFile, ErrorFile  |
| `weather.storage.tools`              | INFO  | Console, RollingFile, ErrorFile   |
| `software.amazon.awssdk`             | WARN  | Console, ErrorFile                |
| `org.apache.http`                    | WARN  | Console, ErrorFile                |
| `org.testcontainers`                 | WARN  | Console                           |
| `com.github.dockerjava`              | WARN  | Console                           |

All named loggers use `additivity="false"`, so an event is written only by the appenders listed
for its logger.

## How It Works

### Configuration Priority Chain

The log directory is determined by this priority order:

1. **`$NOAKWEATHER_LOG_DIR`** (environment variable) - **Highest priority**
   - Set in `~/.zshrc` or `~/.bashrc`
   - Works from any directory
   - Recommended approach

2. **`maven.multiModuleProjectDirectory`** (Maven property) - **Medium priority**
   - Only available when running from parent directory
   - Automatically set by Maven when invoked from reactor

3. **`user.dir/../logs`** (fallback) - **Lowest priority**
   - Computed relative path when running from child
   - Used if neither of the above are available

### Why This Approach?

**Problem:** Maven's `maven.multiModuleProjectDirectory` is only set when Maven runs from the multi-module parent (reactor). When you run:

```bash
cd weather-storage
mvn exec:java -Dexec.mainClass="..."
```

Maven doesn't set `maven.multiModuleProjectDirectory`, so logs would incorrectly go to `weather-storage/logs/` instead of `noakweather-platform/logs/`.

**Solution:** The environment variable `NOAKWEATHER_LOG_DIR` takes priority and ensures logs always go to the correct location regardless of where Maven is invoked.

## Log File Descriptions

| File                    | Purpose                                         | Archives                                                    |
|-------------------------|-------------------------------------------------|-------------------------------------------------------------|
| `noakweather.log`       | Application logs (INFO and above by default)    | Deleted after 30 days                                       |
| `noakweather-error.log` | ERROR and FATAL only                            | Up to 30 per rollover period; not deleted by age            |
| `dynamodb.log`          | DynamoDB repository operations (DEBUG and above)| Up to 7 per rollover period; not deleted by age             |
| `archive/*.log.gz`      | Compressed rolled logs                          | See above; clean up error and DynamoDB archives manually    |

`DefaultRolloverStrategy max` limits how many numbered archives are kept within one rollover
period; it is not a number of days. Only `noakweather.log` has an age-based `Delete` rule.

## Logging in Code

The rules for writing log statements are in [CONTRIBUTING.md](../CONTRIBUTING.md#logging):
SLF4J placeholders, when to guard a debug call, and how rejected report data is logged. In
short, a group rejected as bad data is logged at DEBUG on the shared category
`weather.processing.parser.noaa.Rejected`, never at WARN.

With the default configuration, everything under `weather` logs at INFO, so DEBUG output
(including rejections) is **not** written.

### Turning on DEBUG for an investigation

Log levels are read from `log4j2.xml` when the application starts, and the file is copied into
each module at build time. To see DEBUG output for part of the code:

1. Add a logger for that package or category to
   `noakweather-platform/src/main/resources/log4j2.xml`, inside `<Loggers>`. For example, for
   the METAR/TAF parsers and for rejected groups:
   ```xml
   <Logger name="weather.processing.parser.noaa" level="debug" additivity="false">
       <AppenderRef ref="Console"/>
       <AppenderRef ref="RollingFile"/>
       <AppenderRef ref="ErrorFile"/>
   </Logger>
   ```
   (`weather.processing.parser.noaa` also covers the `...noaa.Rejected` category; use that
   name alone to see only rejections.)
2. Rebuild (`scripts/wethb.sh`, or `scripts/wethp.sh` if you run the packaged jars), then run.
3. Remove the logger again afterward; do not commit it.

Switching a stage to DEBUG at run time, without a rebuild, is planned in issue #174.

### Logging in Tests

There is no separate test configuration: tests use the same `log4j2.xml`. To test code that
only runs when DEBUG is enabled (inside a debug guard), use `LogCapture`, which enables DEBUG
for one logger during the test and lets the test assert on the messages.

## Adding a New Module

When creating a new module:

```bash
# 1. Create the module directory structure
mkdir -p new-module/src/main/java
mkdir -p new-module/src/test/java

# 2. Create pom.xml with resources plugin config
cat > new-module/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <!-- ... basic project info ... -->
    
    <build>
        <resources>
            <!-- Include parent log4j2.xml -->
            <resource>
                <directory>../src/main/resources</directory>
                <includes>
                    <include>log4j2.xml</include>
                </includes>
            </resource>
            <!-- Include own resources if any -->
            <resource>
                <directory>src/main/resources</directory>
            </resource>
        </resources>
    </build>
</project>
EOF

# 3. Build to verify
mvn clean compile

# 4. Test logging
mvn exec:java -Dexec.mainClass="your.main.Class"
```

## Troubleshooting

### Issue 1: Logs Still Going to Child Module Directory

**Symptom:** Running from `weather-storage`, logs appear in `weather-storage/logs/` instead of `noakweather-platform/logs/`

**Diagnosis:**
```bash
# Check if environment variable is set
echo $NOAKWEATHER_LOG_DIR

# Check which shell you're using
echo $SHELL
```

**Solution:**
```bash
# If env var is not set, add it to the correct config file
# For zsh (macOS default)
echo 'export NOAKWEATHER_LOG_DIR="$HOME/Development/Projects/Java/noakweather-engineering-pipeline/noakweather-platform/logs"' >> ~/.zshrc
source ~/.zshrc

# For bash
echo 'export NOAKWEATHER_LOG_DIR="$HOME/Development/Projects/Java/noakweather-engineering-pipeline/noakweather-platform/logs"' >> ~/.bashrc
source ~/.bashrc

# Verify
echo $NOAKWEATHER_LOG_DIR

# Clean and rebuild
cd noakweather-engineering-pipeline/noakweather-platform
mvn clean install -DskipTests

# Test again
cd weather-storage
mvn exec:java -Dexec.mainClass="weather.storage.tools.AddGSIsToAwsTable"
ls -la $NOAKWEATHER_LOG_DIR/
```

### Issue 2: log4j2.xml Not Found

**Symptom:** `ERROR Could not find configuration file`

**Diagnosis:**
```bash
# Check if log4j2.xml exists in master location
ls -la noakweather-platform/src/main/resources/log4j2.xml

# Check if it was copied to target/classes
ls -la weather-storage/target/classes/log4j2.xml
```

**Solution:**
```bash
# 1. Verify master file exists
cat noakweather-platform/src/main/resources/log4j2.xml | head -5

# 2. Check resources plugin config in child module
cat weather-storage/pom.xml | grep -A 10 "<resources>"

# 3. Clean rebuild
cd noakweather-platform
mvn clean install -DskipTests

# 4. Verify file was copied
ls -la weather-storage/target/classes/log4j2.xml
```

### Issue 3: Changes to log4j2.xml Not Taking Effect

**Symptom:** Updated master log4j2.xml but changes don't appear in logs

**Solution:**
```bash
# 1. Clean all target directories
cd noakweather-engineering-pipeline/noakweather-platform
mvn clean

# 2. Verify master file has your changes
cat src/main/resources/log4j2.xml

# 3. Rebuild
mvn compile

# 4. Check target/classes has updated file
diff src/main/resources/log4j2.xml weather-storage/target/classes/log4j2.xml

# 5. Run application
cd weather-storage
mvn exec:java -Dexec.mainClass="weather.storage.tools.AddGSIsToAwsTable"
```

### Issue 4: Maven Resources Plugin Not Working

**Symptom:** Build succeeds but shows "Copying 0 resource from ../src/main/resources"

**Diagnosis:**
```bash
# Check Maven output during build
mvn clean compile | grep "Copying.*resource"
```

**Solution:**
```bash
# Verify resources plugin configuration
cat weather-storage/pom.xml | grep -A 15 "<resources>"

# Should show:
# <resource>
#     <directory>../src/main/resources</directory>
#     <includes>
#         <include>log4j2.xml</include>
#     </includes>
# </resource>

# If missing, add the configuration and rebuild
mvn clean install -DskipTests
```

### Issue 5: Permission Denied on Log Files

**Symptom:** `ERROR Unable to create file logs/noakweather.log`

**Solution:**
```bash
# Check directory permissions
ls -la noakweather-engineering-pipeline/noakweather-platform/ | grep logs

# Create directory if missing
mkdir -p $NOAKWEATHER_LOG_DIR

# Fix permissions if needed
chmod 755 $NOAKWEATHER_LOG_DIR
```

## Testing Different Scenarios

### Test 1: Running from Parent Directory

```bash
cd noakweather-engineering-pipeline/noakweather-platform

# Run a storage tool
mvn -pl weather-storage exec:java -Dexec.mainClass="weather.storage.tools.AddGSIsToAwsTable"

# Verify logs location
ls -la logs/noakweather.log
```

### Test 2: Running from Child Directory

```bash
cd noakweather-engineering-pipeline/noakweather-platform/weather-storage

# Run directly
mvn exec:java -Dexec.mainClass="weather.storage.tools.AddGSIsToAwsTable"

# Verify logs are still in parent
ls -la ../logs/noakweather.log
ls -la $NOAKWEATHER_LOG_DIR/noakweather.log
```

### Test 3: Running Tests

```bash
cd noakweather-engineering-pipeline/noakweather-platform

# Run all tests
mvn test

# Check test logs
tail -f logs/noakweather.log
```

## Benefits of This Approach

### 1. **Location Independence**
- Logs always go to the same place regardless of where Maven is invoked: from the
  repository root, from `noakweather-platform`, or from a child module, as long as
  `NOAKWEATHER_LOG_DIR` is set

### 2. **Single Source of Truth**
- One master `log4j2.xml` file
- Changes automatically propagate via Maven resources plugin
- No configuration drift between modules

### 3. **Developer Friendly**
- New developers set one environment variable
- Configuration automatically copied during build
- No manual file management

### 4. **CI/CD Ready**
- Set `NOAKWEATHER_LOG_DIR` in CI environment
- Consistent logging in all environments
- Easy to collect logs for analysis

### 5. **Production Ready**
- Automatic log rotation (daily + size-based)
- Compression of old logs (`.gz`)
- Automatic cleanup of main-log archives after 30 days
- Separate error log for alerts

## Monitoring and Maintenance

### View Live Logs

```bash
# Tail main log
tail -f $NOAKWEATHER_LOG_DIR/noakweather.log

# Tail error log
tail -f $NOAKWEATHER_LOG_DIR/noakweather-error.log

# Tail DynamoDB operations
tail -f $NOAKWEATHER_LOG_DIR/dynamodb.log

# Follow last 100 lines
tail -n 100 -f $NOAKWEATHER_LOG_DIR/noakweather.log
```

### Search Logs

```bash
# Search for errors
grep ERROR $NOAKWEATHER_LOG_DIR/noakweather.log

# Search with context
grep -C 5 "Exception" $NOAKWEATHER_LOG_DIR/noakweather.log

# Search archived logs
zgrep "ERROR" $NOAKWEATHER_LOG_DIR/archive/*.gz
```

### Clean Old Logs Manually

```bash
# List old logs
ls -lh $NOAKWEATHER_LOG_DIR/archive/

# Remove logs older than 30 days (automatic, but manual if needed)
find $NOAKWEATHER_LOG_DIR/archive/ -name "*.gz" -mtime +30 -delete
```

## Quick Reference

### Common Commands

```bash
# Set environment variable (one-time setup)
export NOAKWEATHER_LOG_DIR="$HOME/Development/Projects/Java/noakweather-engineering-pipeline/noakweather-platform/logs"

# Create log directory
mkdir -p $NOAKWEATHER_LOG_DIR

# Build all modules
cd noakweather-engineering-pipeline/noakweather-platform
mvn clean install -DskipTests

# Run from child module
cd weather-storage
mvn exec:java -Dexec.mainClass="weather.storage.tools.AddGSIsToAwsTable"

# View logs
tail -f $NOAKWEATHER_LOG_DIR/noakweather.log

# Verify log location
ls -la $NOAKWEATHER_LOG_DIR/
```

### File Locations

| File          | Location                                             |
|---------------|------------------------------------------------------|
| Master config | `noakweather-platform/src/main/resources/log4j2.xml` |
| Main log      | `noakweather-platform/logs/noakweather.log`          |
| Error log     | `noakweather-platform/logs/noakweather-error.log`    |
| DynamoDB log  | `noakweather-platform/logs/dynamodb.log`             |
| Archives      | `noakweather-platform/logs/archive/*.log.gz`         |

## References

- [Log4j2 Configuration Documentation](https://logging.apache.org/log4j/2.x/manual/configuration.html)
- [Maven Resources Plugin](https://maven.apache.org/plugins/maven-resources-plugin/)
- [Log4j2 Lookups (Environment Variables)](https://logging.apache.org/log4j/2.x/manual/lookups.html)

---

## Maintenance

**Document Version:** 2.1  
**Last Updated:** November 13, 2026,  
**Author:** NoakWeather Engineering Team  
**Project:** NoakWeather Engineering Pipeline
