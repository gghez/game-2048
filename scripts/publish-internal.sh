#!/usr/bin/env bash
# Upload the signed AAB and create a release on a testing track (default: internal)
# via the Publisher API, then commit. The app stays private to the track's testers —
# nothing public. No secrets here.
#
# Why not Gradle Play Publisher: GPP commits with changesNotSentForReview, which
# this app rejects; and the service account's :commit returns 403. So we use the
# REST API directly with an OWNER androidpublisher token and commit without that
# param (see scripts/README.md for the ADC login).
#
# Env (from .store-passwd or environment):
#   GCP_PROJECT, PACKAGE_NAME (default com.gghez.game2048)
#   TRACK     - internal | alpha | beta | production (default internal)
#   AAB_PATH  - path to the signed .aab (default: release bundle output)
# Note: production on a never-published ("draft") app rejects 'completed' releases
# ("Only releases with status draft may be created on draft app") — do the first
# production publish via the Console; testing tracks accept 'completed'.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
[ -f "$ROOT/.store-passwd" ] && source "$ROOT/.store-passwd"
PKG="${PACKAGE_NAME:-com.gghez.game2048}"
TRACK="${TRACK:-internal}"
AAB="${AAB_PATH:-$ROOT/app/build/outputs/bundle/release/app-release.aab}"
TOKEN="${ACCESS_TOKEN:-$(gcloud auth application-default print-access-token)}"
BASE="https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PKG"
UP="https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/$PKG"
H=(-H "Authorization: Bearer $TOKEN")
[ -n "${GCP_PROJECT:-}" ] && H+=(-H "x-goog-user-project: $GCP_PROJECT")

[ -f "$AAB" ] || (cd "$ROOT" && ./gradlew bundleRelease)

EID=$(curl -fsS "${H[@]}" -X POST "$BASE/edits" | jq -r .id)
echo "edit $EID"
VC=$(curl -fsS "${H[@]}" -H "Content-Type: application/octet-stream" --data-binary @"$AAB" \
  -X POST "$UP/edits/$EID/bundles?uploadType=media" | jq -r '.versionCode')
echo "uploaded AAB versionCode=$VC"
curl -fsS "${H[@]}" -H "Content-Type: application/json" -X PUT "$BASE/edits/$EID/tracks/$TRACK" \
  -d "{\"track\":\"$TRACK\",\"releases\":[{\"status\":\"completed\",\"versionCodes\":[\"$VC\"]}]}" >/dev/null
echo "assigned vc=$VC to '$TRACK' (completed)"
curl -fsS "${H[@]}" -X POST "$BASE/edits/$EID:commit" | jq -r '"committed edit " + .id'
echo "Done. Version $VC released on '$TRACK'. Add testers in the Console if not set."
