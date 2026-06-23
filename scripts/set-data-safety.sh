#!/usr/bin/env bash
# Submit the Data safety declaration via the Android Publisher API
# (applications.dataSafety). For 2exp11 this declares: no data collected, no data
# shared, no tracking. No secrets in this file.
#
# VERIFIED format: POST body is {"safetyLabels": "<CSV>"} where <CSV> is the Play
# Console Data safety questionnaire CSV — one row per response, with Google-specific
# Question IDs and a "Response value" column (TRUE/FALSE). There is no GET and no
# template via the API: download the sample/export CSV from the Console
# (App content > Data safety > Export to CSV), fill it, and pass its path here.
# Ref: https://developers.google.com/android-publisher/api-ref/rest/v3/applications/dataSafety
# This is the ONLY "App content" declaration with an API; content rating (IARC),
# ads, app access and target audience remain web-only.
#
# Usage: scripts/set-data-safety.sh <filled-data-safety.csv>
#
# Reads from .store-passwd (git-ignored) or the environment:
#   GCP_PROJECT
#   PACKAGE_NAME  - app package (default: com.gghez.game2048)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
[ -f "$ROOT/.store-passwd" ] && source "$ROOT/.store-passwd"
PACKAGE_NAME="${PACKAGE_NAME:-com.gghez.game2048}"
CSV="${1:?usage: $0 <filled-data-safety.csv> (export the template from the Console first)}"
TOKEN="${ACCESS_TOKEN:-$(gcloud auth application-default print-access-token)}"

[ -f "$CSV" ] || { echo "CSV not found: $CSV" >&2; exit 1; }
BODY="$(jq -Rs '{safetyLabels: .}' < "$CSV")"

curl -fsS -X POST \
  "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE_NAME}/dataSafety" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  ${GCP_PROJECT:+-H "x-goog-user-project: ${GCP_PROJECT}"} \
  -d "$BODY"
echo
echo "Submitted Data safety from $CSV."
