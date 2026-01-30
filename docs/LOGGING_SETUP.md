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
- Maven 3.6+
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
mkdir -p noakweather-engineering-pipeline/noakweather-platform/logs
mkdir -p noakweather-engineering-pipeline/noakweather-platform/logs/archive
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
ls -la noakweather-engineering-pipeline/noakweather-platform/logs/

# Test 3: View logs
tail -f noakweather-engineering-pipeline/noakweather-platform/logs/noakweather.log
```

**Expected:** Logs should appear in `noakweather-platform/logs/`, NOT in `weather-storage/logs/`.

## Master Configuration File

Location: `noakweather-platform/src/main/resources/log4j2.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
   <Properties>
      <!-- Define log directory location
           Priority: 1. NOAKWEATHER_LOG_DIR env var (recommended)
                    2. maven.multiModuleProjectDirectory (when running from parent)
                    3. user.dir/../logs (fallback when running from child) -->
      <Property name="log.dir">${env:NOAKWEATHER_LOG_DIR:-${sys:maven.multiModuleProjectDirectory:-${sys:user.dir}/..}/logs}</Property>
      <Property name="log.pattern">%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%t] %logger{36} - %msg%n</Property>
   </Properties>

   <Appenders>
      <!-- Console output for all modules -->
      <Console name="Console" target="SYSTEM_OUT">
         <PatternLayout pattern="%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n"/>
      </Console>

      <!-- Main application log file (all modules) -->
      <RollingFile name="RollingFile"
                   fileName="${log.dir}/noakweather.log"
                   filePattern="${log.dir}/archive/noakweather-%d{yyyy-MM-dd-HH-mm-ss}-%i.log.gz"
                   immediateFlush="true">
         <PatternLayout pattern="${log.pattern}"/>
         <Policies>
            <!-- Roll over on application startup -->
            <OnStartupTriggeringPolicy />
            <!-- Roll over daily at midnight -->
            <TimeBasedTriggeringPolicy interval="1" modulate="true"/>
            <!-- Roll over when file reaches 10MB -->
            <SizeBasedTriggeringPolicy size="10MB"/>
         </Policies>
         <DefaultRolloverStrategy max="30">
            <Delete basePath="${log.dir}/archive" maxDepth="1">
               <IfFileName glob="noakweather-*.log.gz"/>
               <IfLastModified age="30d"/>
            </Delete>
         </DefaultRolloverStrategy>
      </RollingFile>

      <!-- Error log file (only ERROR and FATAL) -->
      <RollingFile name="ErrorFile"
                   fileName="${log.dir}/noakweather-error.log"
                   filePattern="${log.dir}/archive/noakweather-error-%d{yyyy-MM-dd-HH-mm-ss}-%i.log.gz">
         <PatternLayout pattern="${log.pattern}"/>
         <Policies>
            <OnStartupTriggeringPolicy />
            <TimeBasedTriggeringPolicy interval="1" modulate="true"/>
            <SizeBasedTriggeringPolicy size="10MB"/>
         </Policies>
         <DefaultRolloverStrategy max="30"/>
         <ThresholdFilter level="ERROR" onMatch="ACCEPT" onMismatch="DENY"/>
      </RollingFile>

      <!-- DynamoDB operations log (optional - for debugging) -->
      <RollingFile name="DynamoDbFile"
                   fileName="${log.dir}/dynamodb.log"
                   filePattern="${log.dir}/archive/dynamodb-%d{yyyy-MM-dd-HH-mm-ss}-%i.log.gz">
         <PatternLayout pattern="${log.pattern}"/>
         <Policies>
            <OnStartupTriggeringPolicy />
            <TimeBasedTriggeringPolicy interval="1" modulate="true"/>
            <SizeBasedTriggeringPolicy size="5MB"/>
         </Policies>
         <DefaultRolloverStrategy max="7"/>
      </RollingFile>
   </Appenders>

   <Loggers>
      <!-- Root logger - all modules (console + file) -->
      <Root level="info">
         <AppenderRef ref="Console"/>
         <AppenderRef ref="RollingFile"/>
         <AppenderRef ref="ErrorFile"/>
      </Root>

      <!-- Weather platform modules -->
      <Logger name="weather" level="info" additivity="false">
         <AppenderRef ref="Console"/>
         <AppenderRef ref="RollingFile"/>
         <AppenderRef ref="ErrorFile"/>
      </Logger>

      <!-- DynamoDB repository operations (detailed logging) -->
      <Logger name="weather.storage.repository.dynamodb" level="debug" additivity="false">
         <AppenderRef ref="Console"/>
         <AppenderRef ref="DynamoDbFile"/>
         <AppenderRef ref="ErrorFile"/>
      </Logger>

      <!-- Tools (like AddGSIsToAwsTable) -->
      <Logger name="weather.storage.tools" level="info" additivity="false">
         <AppenderRef ref="Console"/>
         <AppenderRef ref="RollingFile"/>
         <AppenderRef ref="ErrorFile"/>
      </Logger>

      <!-- Quiet down AWS SDK (only warnings and errors) -->
      <Logger name="software.amazon.awssdk" level="warn" additivity="false">
         <AppenderRef ref="Console"/>
         <AppenderRef ref="ErrorFile"/>
      </Logger>

      <!-- Quiet down Apache HTTP client -->
      <Logger name="org.apache.http" level="warn" additivity="false">
         <AppenderRef ref="Console"/>
         <AppenderRef ref="ErrorFile"/>
      </Logger>

      <!-- Quiet down Testcontainers (only in tests, but config applies) -->
      <Logger name="org.testcontainers" level="warn" additivity="false">
         <AppenderRef ref="Console"/>
      </Logger>

      <!-- Quiet down Docker client used by Testcontainers -->
      <Logger name="com.github.dockerjava" level="warn" additivity="false">
         <AppenderRef ref="Console"/>
      </Logger>
   </Loggers>
</Configuration>
```

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

| File                    | Purpose                               | Retention                           |
|-------------------------|---------------------------------------|-------------------------------------|
| `noakweather.log`       | All application logs (INFO and above) | 30 days                             |
| `noakweather-error.log` | Error-level logs only                 | 30 days                             |
| `dynamodb.log`          | DynamoDB operations (DEBUG level)     | 7 days                              |
| `archive/*.log.gz`      | Compressed rolled logs                | Auto-deleted after retention period |

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
ls -la noakweather-engineering-pipeline/noakweather-platform/logs/
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
mkdir -p noakweather-engineering-pipeline/noakweather-platform/logs

# Fix permissions if needed
chmod 755 noakweather-engineering-pipeline/noakweather-platform/logs
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
ls -la noakweather-engineering-pipeline/noakweather-platform/logs/noakweather.log
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
- Logs always go to the same place regardless of where Maven is invoked
- Works from parent directory:
- Works from child directory:
- Works from anywhere with env var:

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
- Automatic cleanup (30-day retention)
- Separate error log for alerts

## Monitoring and Maintenance

### View Live Logs

```bash
# Tail main log
tail -f noakweather-engineering-pipeline/noakweather-platform/logs/noakweather.log

# Tail error log
tail -f noakweather-engineering-pipeline/noakweather-platform/logs/noakweather-error.log

# Tail DynamoDB operations
tail -f noakweather-engineering-pipeline/noakweather-platform/logs/dynamodb.log

# Follow last 100 lines
tail -n 100 -f noakweather-engineering-pipeline/noakweather-platform/logs/noakweather.log
```

### Search Logs

```bash
# Search for errors
grep ERROR noakweather-engineering-pipeline/noakweather-platform/logs/noakweather.log

# Search with context
grep -C 5 "Exception" noakweather-engineering-pipeline/noakweather-platform/logs/noakweather.log

# Search archived logs
zgrep "ERROR" noakweather-engineering-pipeline/noakweather-platform/logs/archive/*.gz
```

### Clean Old Logs Manually

```bash
# List old logs
ls -lh noakweather-engineering-pipeline/noakweather-platform/logs/archive/

# Remove logs older than 30 days (automatic, but manual if needed)
find noakweather-engineering-pipeline/noakweather-platform/logs/archive/ -name "*.gz" -mtime +30 -delete
```

## Quick Reference

### Common Commands

```bash
# Set environment variable (one-time setup)
export NOAKWEATHER_LOG_DIR="$HOME/Development/Projects/Java/noakweather-engineering-pipeline/noakweather-platform/logs"

# Create log directory
mkdir -p noakweather-engineering-pipeline/noakweather-platform/logs

# Build all modules
cd noakweather-engineering-pipeline/noakweather-platform
mvn clean install -DskipTests

# Run from child module
cd weather-storage
mvn exec:java -Dexec.mainClass="weather.storage.tools.AddGSIsToAwsTable"

# View logs
tail -f noakweather-engineering-pipeline/noakweather-platform/logs/noakweather.log

# Verify log location
ls -la noakweather-engineering-pipeline/noakweather-platform/logs/
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

**Document Version:** 2.0  
**Last Updated:** January 25, 2026,  
**Author:** NoakWeather Engineering Team  
**Project:** NoakWeather Engineering Pipeline
