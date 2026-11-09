#!/usr/bin/env bash
# ============================================================================
# UAT review helper
#
# wethuat_review_stations.sh
#
# Downloads the Bronze speed-layer JSON written by wethuat_metar_ingest.sh for
# the given stations and prints one line per report with the fields used to
# check Pending UAT items: raw METAR, unparsedMainBody, remarks.freeText and a
# short form of parseIssues. The full JSON files are kept for item-specific
# fields (runwayStates, recentWeather, ...).
#
# The bucket is never hardcoded: pass it with -b or set NOAKWEATHER_S3_BUCKET.
# Output goes to logs/uat/review/<YYYYMMDD>/ (not committed).
#
# Requires the AWS CLI (same credentials as the sweep) and jq.
#
# Usage:
#   ./scripts/uat/wethuat_review_stations.sh [-b bucket] [-d YYYY-MM-DD] STATION [STATION...]
# Example:
#   NOAKWEATHER_S3_BUCKET=my-bucket ./scripts/uat/wethuat_review_stations.sh CYYQ UHPP EGBB
# ============================================================================

set -uo pipefail

REPO_ROOT="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)" || exit 1
cd "$REPO_ROOT" || exit 1

BUCKET="${NOAKWEATHER_S3_BUCKET:-}"
DAY="$(date -u +%Y-%m-%d)"   # ingestion date (UTC), as used in the S3 keys

usage() {
  echo "Usage: $0 [-b bucket] [-d YYYY-MM-DD] STATION [STATION...]" >&2
  echo "  bucket defaults to \$NOAKWEATHER_S3_BUCKET; date defaults to today (UTC)" >&2
  exit 1
}

while getopts "b:d:h" opt; do
  case "$opt" in
    b) BUCKET="$OPTARG" ;;
    d) DAY="$OPTARG" ;;
    *) usage ;;
  esac
done
shift $((OPTIND - 1))

if [ -z "$BUCKET" ]; then
  echo "ERROR: no bucket given: pass -b or set NOAKWEATHER_S3_BUCKET" >&2
  usage
fi
if [ $# -eq 0 ]; then
  echo "ERROR: no stations given" >&2
  usage
fi
if [[ ! "$DAY" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "ERROR: date must be YYYY-MM-DD" >&2
  exit 1
fi
for tool in aws jq; do
  command -v "$tool" >/dev/null || { echo "ERROR: $tool not found on PATH" >&2; exit 1; }
done

DAY_PATH="${DAY//-//}"                # 2026-11-08 -> 2026/11/08
OUT="logs/uat/review/${DAY//-/}"      # logs/uat/review/20261108
if [ ! -d "$OUT" ]; then
  echo "Creating review directory: $OUT"
  mkdir -p "$OUT"
else
  echo "Review directory already exists: $OUT"
fi

INCLUDES=()
for station in "$@"; do
  INCLUDES+=(--include "${station}_*")
done

S3_PREFIX="s3://${BUCKET}/bronze/speed-layer/noaa/metar/${DAY_PATH}/"
echo "Downloading speed-layer JSON for $# station(s) from ${S3_PREFIX}"
if ! aws s3 cp "$S3_PREFIX" "$OUT/" --recursive --exclude '*' "${INCLUDES[@]}" --only-show-errors; then
  echo "ERROR: download failed" >&2
  exit 1
fi

shopt -s nullglob
SUMMARY="$OUT/summary.jsonl"
: > "$SUMMARY"
for station in "$@"; do
  files=("$OUT/${station}"_*.json)
  if [ ${#files[@]} -eq 0 ]; then
    echo "WARN: no JSON for ${station} on ${DAY}" >&2
    continue
  fi
  for f in "${files[@]}"; do
    jq -c --arg file "$(basename "$f")" \
      '{station: .stationId, file: $file, raw: .rawData, unparsedMainBody,
        freeText: .remarks.freeText,
        parseIssues: [(.parseIssues // [])[] | {kind, handler, text, reason}]}' "$f"
  done
done | tee "$SUMMARY"

echo ""
echo "Full JSON and summary.jsonl kept in: $OUT"
