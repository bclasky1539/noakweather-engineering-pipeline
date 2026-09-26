#!/usr/bin/env bash
# ============================================================================
# UAT discovery tool for checking of tokens existing in data
#
# uat_discover_stations.sh
#
# One-off discovery tool (NOT part of the pipeline or the UAT sweep itself).
# Downloads the current worldwide METAR bulk snapshot from aviationweather.gov
# and greps it for token patterns tied to issues currently Pending UAT.
# Output is a list of candidate station IDs + their raw METAR line, for
# manual review before adding any of them to the UAT station list in
# wethuat_metar_ingest.sh.
#
# Update the PATTERNS array below each UAT round to match whatever issues
# are currently open and need fresh live confirmation.
#
# Usage: ./wethuat_discover_stations.sh
# ============================================================================

BULK_URL="https://aviationweather.gov/data/cache/metars.cache.csv.gz"
WORKDIR="./uat-discovery/METAR/"
BULK_FILE="${WORKDIR}metars.cache-$(date +%Y%m%d-%H%M%S).csv"

mkdir -p "${WORKDIR}"

echo "Fetching bulk METAR snapshot..."
if ! curl -sS "${BULK_URL}" -o "${BULK_FILE}.gz"; then
    echo "ERROR: failed to download bulk snapshot" >&2
    exit 1
fi
gunzip "${BULK_FILE}.gz"

echo "Snapshot saved: ${BULK_FILE}"
echo ""

# "PRESFR-PRESRR (#74)|PRESFR|PRESRR"
# "Chained-begin-end-weather (#73)|\b(TS|RA|SN|DZ|SG|IC|PL|GR|GS|UP|BR|FG|FU|VA|DU|SA|HZ|PY|FZ|MI|PR|BC|DR|BL|SH)[A-Z]{0,2}[BE][0-9]{2,4}([BE][0-9]{2,4})+\b"
declare -a PATTERNS=(
    "ICG-with-qualifier (#75)|ICG [A-Z]{4} [A-Z]{2}"
    "ICG-without-qualifier (#75)|ICG"
    "CI0-zero-okta (#72)|CI0"
    "TCU-CB-EMBDD (#72)|(TCU|CB) EMBDD"
    "VCSH (pending UAT) (#60)|VCSH"
    "Directional-arc-3pt (#69)|[NSEW]{1,2}(-[NSEW]{1,2}){2,}"
    "AND-chain-direction (#69)|[NSEW]{1,2} AND [NSEW]{1,2}"
)

for entry in "${PATTERNS[@]}"; do
    label="${entry%%|*}"
    pattern="${entry#*|}"

    echo "=== ${label} ==="
    echo "Pattern: ${pattern}"

    matches="$(grep -E "${pattern}" "${BULK_FILE}" | awk -F'","' '{print $1}' | sed 's/^"//' 2>/dev/null)"

    if [ -z "${matches}" ]; then
        echo "(no matches in this snapshot)"
    else
        echo "${matches}" | sort -u | head -20
    fi

    echo ""
done

echo "Full snapshot retained at: ${BULK_FILE}"
echo "(Review matches above, pick stations to add to wethuat_metar_ingest.sh.)"
