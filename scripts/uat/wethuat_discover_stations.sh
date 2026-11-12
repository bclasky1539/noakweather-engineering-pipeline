#!/usr/bin/env bash
# ============================================================================
# UAT discovery tool for checking of tokens existing in data
#
# wethuat_discover_stations.sh
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
# Usage: ./scripts/uat/wethuat_discover_stations.sh
# ============================================================================

REPO_ROOT="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)" || exit 1
cd "$REPO_ROOT" || exit 1

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
# "ALQDS-thunderstorm-location (#99)|(?:TS|CB|TCU|ACC|CBMAM|VIRGA)\s+ALQDS"
# "ICG-with-qualifier (#75)|ICG [A-Z]{4} [A-Z]{2}"
# "ICG-without-qualifier (#75)|ICG"
# "CI0-zero-okta (#72)|CI0"
# "TCU-CB-EMBDD (#72)|(TCU|CB) EMBDD"
# "Alphanumeric-station-code (#100)|<pattern for digit-containing station IDs — likely not a remarks-text pattern at all, since this is a station-ID issue, not a remarks pattern>"
declare -a PATTERNS=(
    # #75 — bare ICG search, not anchored to a specific qualifier shape.
    # Review hits for the qualifier format that follows (PAST HR confirmed; others may surface new formats).
    "ICG-bare (#75 broadened)|\\bICG\\b"
    # #72a — CI0 standalone, not anchored to chained-cloud context.
    "CI0 (#72 broadened)|CI0( |\"|\$)"
    # #72b — EMBDD alone, not anchored to a preceding TCU/CB token.
    # Review what precedes each hit — may surface cloud types beyond TCU/CB worth adding to the pattern.
    "EMBDD-standalone (#72 broadened)|EMBDD"
    "VCSH (pending UAT) (#60)|VCSH"
    "Directional-arc-3pt (#69)|[NSEW]{1,2}(-[NSEW]{1,2}){2,}"
    "AND-chain-direction (#69)|[NSEW]{1,2} AND [NSEW]{1,2}"
    "Lightning-remark (#86)|LTG"
    "Lightning-ALQDS (#86)|LTG.*ALQDS"
    "Lightning-AT-AP (#86)|LTG.*AT AP"
    "Predominant-cloud-type (#87)|8/[0-9/]{3}"
    "Augmented-station-type (#88)|A[O0][12]A"
    "Observation-program-status (#89)|LAST (STFD )?OBS"
    "Alphanumeric-station-code (#100)|(^| )K([0-9][A-Z0-9]{2}|[A-Z][0-9][A-Z0-9]|[A-Z]{2}[0-9]) [0-9]{6}Z"
     # #99 — broadened to catch ALQDS anywhere, not just zero-distance after a cloud type.
     # Will also catch lightning-remark ALQDS (already confirmed via #86) — filter those out manually,
     # keep anything where ALQDS follows a bare cloud-type token (TS/CB/TCU/ACC/CBMAM/VIRGA) rather than LTG.
     "ALQDS-anywhere (#99 broadened)|ALQDS"
     # #105 — except-direction. Expect XCPT/XCP/EXCP/EXC followed by a compass point.
     "Except-direction (#105)| (XCPT|XCP|EXCP|EXC) (N|NE|E|SE|S|SW|W|NW)( |-|\"|\$)"
     # Out of scope but worth seeing if the 'expected' family shows up live.
     "Expected-family (not parsed, #105 scope note)| (EXPCD|EXPCTD|EXPTD|EXP) [NSEW]{1,2}( |\"|\$)"
     # #106 — space-separated continuation, 2+ points, bounded (the unbounded form gave false positives).
     "TS-continuation-space-separated (#106)|(OHD|VC|DSNT|DSIPTD|TOP|TR|ALQDS)( [NSEW]{1,2}){2,}( |\"|\$)"
     # #104 — ALQDS after a present-weather code.
     "ALQDS-after-weather (#104)| (HZ|BR|FG|FU|DU|VCSH|VCTS|VCFG|VCRA|SH|TS)[A-Z]* ALQDS( |\"|\$)"
     # #104 — AND <qualifier> after a thunderstorm location.
     "TS-AND-qualifier (#104)| (CB|TCU|TS|ACC|CBMAM|VIRGA) (OHD|VC|DSNT|DSIPTD|TOP|TR|ALQDS)( [NSEW]{1,2})* AND (OHD|VC|DSNT|DSIPTD|TOP|TR|ALQDS)( |\"|\$)"
     # #89 — VIA relay station on the observation-program remark.
     "Obs-program-VIA (#89)|OBS/NEXT [0-9]{6}( ?Z| UTC|UTC)? VIA [A-Z]{4}"
     # Two directional-weather remarks in one report. Watch for a second one overwriting the first
     # (directionalWeather is single-valued). Not tied to an issue yet.
     "Two-directional-weather (single-valued field)|RMK.*(VCSH|VCTS|VCFG|VCRA|HZ|FG|BR) [NSEW]{1,2}.*(VCSH|VCTS|VCFG|VCRA|HZ|FG|BR) [NSEW]{1,2}"
     "Recent-weather-main-body (RE groups)| (A|Q)[0-9]{4} RE[A-Z+-]+( |\"|\$)"
     "113-rvr-ft-trend-suffix|(^| )R[0-9]{2}[LRC]?/[MP]?[0-9]{4}(V[MP]?[0-9]{4})?FT/[UDN]( |\"|\$)"
     "121-runway-state-group|(^| )R[0-9]{2}[LRC]?/([0-9/][1259/]([0-8][0-9]|90|9[2-9]|//)|CLRD)(0[1-9]|[1-8][0-9]|90|9[1-5]|99|//)( |\"|\$)"
     "116-recent-weather|(^| )RE(//|[A-Z]{2,8})( |\"|\$)"
     "118-directional-weather-compass-follower|(^| )(VC[A-Z]{2}|HZ|FG|BR|FU|DU)( (N|NE|E|SE|S|SW|W|NW))+ ([NSEW][A-Z0-9]{2,}|NE|NW|SW)( |\"|\$)"
     "114-ts-location-space-separated-points|(^| )(TS|CB|TCU|ACC|CBMAM|VIRGA)( (OHD|VC|DSNT|DSIPTD|TOP|TR|ALQDS))? (N|NE|E|SE|S|SW|W|NW) (N|NE|E|SE|S|SW|W|NW)( |\"|\$)"
     "117-lightning-and-chain|(^| )LTG(IC|CC|CG|CA|CW)*( (OHD|VC|DSNT|AT AP))? [NSEW]{1,2}(-[NSEW]{1,2})* AND [NSEW]{1,2}(-[NSEW]{1,2})*( |\"|\$)"
     "115-vis-sector-or-second-location|(^| )VIS (([NSEW]{1,2}(-[NSEW]{1,2})*) [0-9]+(/[0-9]+)?( [0-9]+/[0-9]+)?|[0-9]+(/[0-9]+)?( [0-9]+/[0-9]+)? ([NSEW]{1,2}(-[NSEW]{1,2})*|RWY[0-9]{2}[LRC]?))( |\"|\$)"
     # #173 — main-body groups rejected since #138 (1.28.0) stay in unparsedMainBody and are
     # REJECTED parseIssues (#166); confirm both on live reports in UAT.
     "130-zero-denominator-visibility|(^|\"| )([0-9]+ )?[0-9]+/0SM( |\"|\$)"
     "138-vv-without-height|(^|\"| )VV///( |\"|\$)"
     # Pressure accepts 25.00-35.00 inHg and 850-1085 hPa; anything outside is rejected.
     # These also match a secondary altimeter in the remarks (#132).
     "138-altimeter-out-of-range|(^|\"| )A(0[0-9]{3}|1[0-9]{3}|2[0-4][0-9]{2}|350[1-9]|35[1-9][0-9]|3[6-9][0-9]{2}|[4-9][0-9]{3})( |\"|\$)"
     "138-qnh-out-of-range|(^|\"| )Q(0[0-7][0-9]{2}|08[0-4][0-9]|108[6-9]|109[0-9]|1[1-9][0-9]{2}|[2-9][0-9]{3})( |\"|\$)"
     # #132 — remark values that cannot be parsed are REJECTED parseIssues and stay in freeText.
     "132-remark-variable-visibility-zero-denominator|(^|\"| )VIS ([NSEW]{1,2} )?(([0-9]+ )?[0-9]+/0V|([0-9]+ )?[0-9]+(/[0-9]+)?V([0-9]+ )?[0-9]+/0( |\"|\$))"
     "132-remark-twr-sfc-visibility-zero-denominator|(^|\"| )(TWR|SFC) VIS ([0-9]+ )?[0-9]+/0( |\"|\$)"
     "132-remark-hail-size-invalid|(^|\"| )GR (0|0/[0-9]+|([0-9]+ )?[0-9]+/0)( |\"|\$)"
     # #140 — cloud types SkyCondition used to reject, on a main-body sky group or a remark okta.
     "140-cloud-type-vocabulary|(^|\"| )((FEW|SCT|BKN|OVC)[0-9]{3}(CF|SF|ACC|ACSL)|(CF|SF|ACC|ACSL)[0-9])( |\"|\$)"
     # #92 — bare cloud-type code (no okta) in the remarks.
     "92-bare-cloud-type|RMK.* (SC|AC|AS|CI|CU|ST|NS|CC|CS|SF|CF)( |\"|\$)"
     # #92 — bare cloud-type code (no okta) in the remarks.
     "92-bare-cloud-type|RMK.* (SC|AC|AS|CI|CU|ST|NS|CC|CS|SF|CF)( |\"|\$)"
     # #179 — directional weather with a hyphenated direction range (VCSH S-SW). The range must be
     # stored as one direction segment, with nothing left in freeText; also covers a location qualifier (SH DSNT S-SW).
     "179-directional-weather-direction-range|(^|\"| )(VC[A-Z]{2,4}|HZ|FG|BR|FU|DU|SH[A-Z]{0,4})( (OHD|VC|DSNT|DSIPTD|TOP|TR))?( [NSEW]{1,2}(-[NSEW]{1,2})*)* [NSEW]{1,2}(-[NSEW]{1,2})+( |\"|\$)"
     # #180 — temperature/dewpoint with a one- or three-digit value; rejected and kept in unparsedMainBody.
     "180-temp-dewpoint-truncated-dewpoint|(^|\"| )M?[0-9]{2}/M?([0-9]|[0-9]{3})( |\"|\$)"
     "180-temp-dewpoint-bad-temperature|(^|\"| )M?([0-9]|[0-9]{3})/M?[0-9]{2}( (A|Q)[0-9]{4}|\"|\$)"
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
