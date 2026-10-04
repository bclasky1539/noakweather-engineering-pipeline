#!/usr/bin/env bash
#
# discover-historical-metars.sh (weths_discover-historical-metars.sh)
#
# Exploratory tool (standalone — NOT part of the wethuat_* UAT toolchain).
# Searches NOAA's tgftp.nws.noaa.gov per-station METAR text files for rare
# grammar forms that the bulk metars.cache.csv.gz "current" snapshot is
# unlikely to surface, since that snapshot only contains stations that
# reported within roughly the last hour.
#
# Each file under /data/observations/metar/stations/<ID>.TXT holds that
# station's MOST RECENTLY RECEIVED report, which may be years old if the
# station is inactive or reports infrequently. A match here is evidence
# the grammar form exists/existed somewhere, NOT that it's in current use.
#
# Matches print live as they're found (and are logged), so this can be
# safely Ctrl+C'd as soon as it's told you what you need.
#
# Usage:
#   ./weths_discover-historical-metars.sh <LETTER>
#
# Example:
#   ./weths_discover-historical-metars.sh C

set -uo pipefail

START_TIME=$(date +%s)

LETTER="${1:-}"
if [[ -z "$LETTER" || ! "$LETTER" =~ ^[A-Za-z]$ ]]; then
    echo "Usage: $0 <LETTER>   (single letter A-Z)" >&2
    exit 1
fi
LETTER="$(echo "$LETTER" | tr '[:lower:]' '[:upper:]')"

BASE_URL="https://tgftp.nws.noaa.gov/data/observations/metar/stations"
SLEEP_SECONDS=0.3
OUT_DIR="./historical-discovery/METAR"
LISTING_CACHE="${OUT_DIR}/stations-listing.html"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
MATCH_LOG="${OUT_DIR}/matches-${LETTER}-${TIMESTAMP}.log"
CHECKED_LOG="${OUT_DIR}/checked-${LETTER}-${TIMESTAMP}.log"

mkdir -p "$OUT_DIR"

echo "Fetching station directory listing..."
curl -s "${BASE_URL}/" -o "$LISTING_CACHE"
if [[ ! -s "$LISTING_CACHE" ]]; then
    echo "ERROR: failed to fetch directory listing" >&2
    exit 1
fi

STATIONS=()
while IFS= read -r STATION_ID; do
    STATIONS+=("$STATION_ID")
done < <(
    grep -oE 'href="[A-Z0-9]+\.TXT"' "$LISTING_CACHE" \
        | sed -E 's/href="([A-Z0-9]+)\.TXT"/\1/' \
        | grep -E "^${LETTER}" \
        | sort -u
)

TOTAL=${#STATIONS[@]}
echo "Found ${TOTAL} stations starting with '${LETTER}'."
if [[ "$TOTAL" -eq 0 ]]; then
    echo "Nothing to do."
    exit 0
fi

echo "Match log:   ${MATCH_LOG}"
echo "Checked log: ${CHECKED_LOG}  (raw record for every station fetched, regardless of match — ${TOTAL} expected)"
echo "Press Ctrl+C at any time once you've seen enough."
echo "---"

# "TEST-SLP-remark"
# "TEST-QNH-pressure"
PATTERN_LABELS=(
    "XCPT-except-anywhere"
    "Except-expected-forms"
    "ICG-bare"
    "CI0-standalone"
    "EMBDD-standalone"
    "TS-CLD-LOC-Continuation-SpaceSeparated"
    "TS-CLD-LOC-Continuation-LiteralSpace"
)
# '\bSLP[0-9]{3}\b'
# '\bQ[0-9]{4}\b'
# '(OHD|VC|DSNT|DSIPTD|TOP|TR|ALQDS) [NSEW]{1,2}( [NSEW]{1,2}){1,}'
PATTERN_REGEXES=(
    '\bX(C)?PT?\b'
    '\b(EXCP|EXC|EXPCD|EXPCTD|EXPTD|EXP)\b'
    '\bICG\b'
    '\bCI0\b'
    'EMBDD'
    '\b(OHD|VC|DSNT|DSIPTD|TOP|TR|ALQDS)\s+[NSEW]{1,2}(\s+[NSEW]{1,2}){1,}\b'
    '(OHD|VC|DSNT|DSIPTD|TOP|TR|ALQDS)( [NSEW]{1,2}){2,}( |$)'
)

COUNT=0
MATCH_COUNT=0

for STATION in "${STATIONS[@]}"; do
    COUNT=$((COUNT + 1))
    if (( COUNT % 100 == 0 )); then
        echo "  ...checked ${COUNT}/${TOTAL} (${STATION})"
    fi

    CONTENT=$(curl -s "${BASE_URL}/${STATION}.TXT")

    {
        echo "=== ${STATION} (${COUNT}/${TOTAL}) ==="
        if [[ -z "$CONTENT" ]]; then
            echo "(empty response)"
        else
            echo "$CONTENT"
        fi
        echo ""
    } >> "$CHECKED_LOG"

    if [[ -z "$CONTENT" ]]; then
        sleep "$SLEEP_SECONDS"
        continue
    fi

    for (( i=0; i<${#PATTERN_LABELS[@]}; i++ )); do
        LABEL="${PATTERN_LABELS[$i]}"
        PATTERN="${PATTERN_REGEXES[$i]}"
        if echo "$CONTENT" | grep -qE "$PATTERN"; then
            MATCH_COUNT=$((MATCH_COUNT + 1))
            {
                echo "=== MATCH: ${LABEL} — ${STATION} (checked ${COUNT}/${TOTAL}) ==="
                echo "$CONTENT"
                echo ""
            } | tee -a "$MATCH_LOG"
        fi
    done

    sleep "$SLEEP_SECONDS"
done

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))
ELAPSED_MIN=$((ELAPSED / 60))
ELAPSED_SEC=$((ELAPSED % 60))

echo "---"
echo "Done. Checked ${COUNT} stations, found ${MATCH_COUNT} match(es)."
echo "Elapsed time: ${ELAPSED_MIN}m ${ELAPSED_SEC}s"
echo "Match log:   ${MATCH_LOG}"
echo "Checked log: ${CHECKED_LOG}"
