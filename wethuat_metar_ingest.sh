#!/bin/bash
# ============================================================================
# UAT METAR Ingestion Sweep
#
# Purpose: Ingest METAR data for a broad, geographically and format-diverse
#          set of worldwide stations, then validate the resulting Bronze
#          layer output (raw-data + speed-layer) for each station via
#          glue-jobs/tools/analyze_bronze_station.py, to surface any
#          Bronze-layer parsing or ingestion issues before proceeding to
#          Gold layer development.
#
# Run from the repository root, on main (no application code changes are
# made here - this only ingests and validates live data). Requires a
# packaged weather-ingestion jar; run ./wethp.sh first if target/*.jar
# does not exist. Requires python3 with the dependencies in
# glue-jobs/requirements.txt installed (boto3), and AWS credentials
# already configured (same chain used by the AWS CLI). The existing
# glue_env conda environment already satisfies this.
#
# Result semantics per station:
#   Ingestion: OK / FAILED - did MetarIngestionApp report success?
#   Validation: PASS / WARN / FAIL - did analyze_bronze_station.py find
#     issues in the resulting S3 content? WARN = noteworthy but not a bug
#     (e.g. unparsed main body tokens - this is exactly the UAT signal
#     we're looking for). FAIL = a real structural/content problem.
#
# A validation failure for one station does not stop the sweep; every
# station is attempted and every result is recorded in the summary.
#
# Usage: ./uat_metar_ingest.sh
# ============================================================================

set -uo pipefail

INGESTION_DIR="noakweather-platform/weather-ingestion"
JAR_PATH=$(ls "${INGESTION_DIR}"/target/weather-ingestion-*-SNAPSHOT.jar 2>/dev/null | grep -v "/original-" | head -n 1)

if [ -z "$JAR_PATH" ]; then
  echo "ERROR: No weather-ingestion jar found in ${INGESTION_DIR}/target/"
  echo "Run ./wethp.sh first to build it."
  exit 1
fi

VALIDATOR_SCRIPT="glue-jobs/tools/analyze_bronze_station.py"
if [ ! -f "$VALIDATOR_SCRIPT" ]; then
  echo "ERROR: $VALIDATOR_SCRIPT not found."
  echo "Run this script from the repository root."
  exit 1
fi

echo "Using jar: $JAR_PATH"

echo "Checking python3/boto3 availability..."
PYTHON3_PATH=$(command -v python3)
if [ -z "$PYTHON3_PATH" ]; then
  echo "ERROR: python3 not found on PATH."
  exit 1
fi
echo "  python3: $PYTHON3_PATH"

if ! python3 -c "import boto3" 2>/dev/null; then
  echo "ERROR: python3 at $PYTHON3_PATH cannot import boto3."
  echo "If you have a conda environment with boto3 installed (e.g. glue_env),"
  echo "make sure it is active in THIS shell session before running this script:"
  echo "  conda activate glue_env"
  echo "  which python3   # confirm it points into the glue_env path"
  echo "  ./wethuat_metar_ingest.sh"
  exit 1
fi
echo "  boto3: OK"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_DIR="logs/uat"
if [ ! -d "$LOG_DIR" ]; then
  echo "Creating log directory: $LOG_DIR"
  mkdir -p "$LOG_DIR"
else
  echo "Log directory already exists: $LOG_DIR"
fi
LOG_FILE="${LOG_DIR}/uat_ingest_${TIMESTAMP}.log"

# Ingestion date for validation lookups: must match the UTC date the Java
# app used when partitioning S3 keys (ingestion time, not observation time).
TODAY=$(date -u +%Y-%m-%d)

# ----------------------------------------------------------------------------
# Station list: ~90 stations chosen for diversity across:
# - Units (statute miles/inHg vs meters/hPa)
# - Hemisphere and climate (tropical, polar, desert, monsoon)
# - Reporting conventions (CAVOK-heavy vs rarely used, RVR-heavy airports)
# - Elevation extremes (sea level to high altitude)
# - Automated vs staffed stations
# ----------------------------------------------------------------------------
STATIONS=(
  # --- North America (SM / inHg) ---
  KATL KJFK KORD KDFW KDEN KSFO KSEA KMIA KBOS KLAX KPHX KMCO KIAH KAFW
  CYYZ CYVR CYUL
  MMMX

  # --- Caribbean / Central America ---
  MKJP TJSJ TNCM

  # --- UK / Ireland (fog / RVR heavy) ---
  EGLL EGKK EGCC EIDW

  # --- Western / Central Europe (meters / hPa, CAVOK common) ---
  LFPG EDDF EDDM EHAM LEMD LIRF ESSA ENGM EKCH LSZH LOWW EPWA

  # --- Eastern Europe / Russia ---
  UUEE

  # --- Middle East (heat, sandstorms) ---
  OMDB OTHH OERK LLBG

  # --- Africa ---
  FAOR HECA DNMM GMMN HKJK

  # --- Asia ---
  RJTT RJAA RKSI ZBAA ZSPD VHHH WSSS VTBS VIDP VABB RPLL WMKK RCTP

  # --- Oceania ---
  YSSY YMML NZAA NZWN YBBN YPPH

  # --- South America ---
  SBGR SBGL SAEZ SCEL SPJC SKBO

  # --- Polar / extreme cold ---
  PAFA PANC BIRK ENSB SCCI

  # --- High altitude ---
  SLLP

  # --- Pacific islands ---
  PHNL
)

TOTAL=${#STATIONS[@]}
INGEST_OK=0
INGEST_FAIL=0
INGEST_FAILED_STATIONS=()

VALIDATE_PASS=0
VALIDATE_WARN=0
VALIDATE_FAIL=0
VALIDATE_WARN_STATIONS=()
VALIDATE_FAIL_STATIONS=()

echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "UAT METAR Ingestion + Validation Sweep - $TOTAL stations"
echo "Ingestion date (UTC): $TODAY"
echo "Log file: $LOG_FILE"
echo "+++++++++++++++++++++++++++++++++++++++++++++"

for STATION in "${STATIONS[@]}"; do
  echo "--- $STATION ---" | tee -a "$LOG_FILE"

  INGEST_OUTPUT=$(java -cp "$JAR_PATH" weather.ingestion.service.source.noaa.MetarIngestionApp "$STATION" 2>&1)
  INGEST_EXIT=$?
  echo "$INGEST_OUTPUT" >> "$LOG_FILE"

  if [ $INGEST_EXIT -eq 0 ] && echo "$INGEST_OUTPUT" | grep -q "Successfully ingested"; then
    echo "  Ingestion: OK"
    INGEST_OK=$((INGEST_OK + 1))
  else
    echo "  Ingestion: FAILED (exit code $INGEST_EXIT)"
    INGEST_FAIL=$((INGEST_FAIL + 1))
    INGEST_FAILED_STATIONS+=("$STATION")
    sleep 0.5
    continue
  fi

  VALIDATE_OUTPUT=$(python3 "$VALIDATOR_SCRIPT" "$STATION" "$TODAY" 2>&1)
  VALIDATE_EXIT=$?
  echo "$VALIDATE_OUTPUT" >> "$LOG_FILE"

  case $VALIDATE_EXIT in
    0)
      echo "  Validation: PASS"
      VALIDATE_PASS=$((VALIDATE_PASS + 1))
      ;;
    2)
      echo "  Validation: WARN (see log for details)"
      VALIDATE_WARN=$((VALIDATE_WARN + 1))
      VALIDATE_WARN_STATIONS+=("$STATION")
      ;;
    *)
      echo "  Validation: FAILED (see log for details)"
      VALIDATE_FAIL=$((VALIDATE_FAIL + 1))
      VALIDATE_FAIL_STATIONS+=("$STATION")
      ;;
  esac

  sleep 0.5
done

{
  echo ""
  echo "+++++++++++++++++++++++++++++++++++++++++++++"
  echo "Ingestion:  $INGEST_OK/$TOTAL succeeded, $INGEST_FAIL failed"
  if [ $INGEST_FAIL -gt 0 ]; then
    echo "  Failed stations: ${INGEST_FAILED_STATIONS[*]}"
  fi
  echo ""
  echo "Validation (of the $INGEST_OK successfully ingested stations):"
  echo "  PASS: $VALIDATE_PASS"
  echo "  WARN: $VALIDATE_WARN"
  if [ $VALIDATE_WARN -gt 0 ]; then
    echo "    Stations: ${VALIDATE_WARN_STATIONS[*]}"
  fi
  echo "  FAIL: $VALIDATE_FAIL"
  if [ $VALIDATE_FAIL -gt 0 ]; then
    echo "    Stations: ${VALIDATE_FAIL_STATIONS[*]}"
  fi
  echo "+++++++++++++++++++++++++++++++++++++++++++++"
  echo "Full log: $LOG_FILE"
} | tee -a "$LOG_FILE"
