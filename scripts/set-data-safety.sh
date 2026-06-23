#!/usr/bin/env bash
# Submit the Data safety declaration via the Android Publisher API
# (applications.dataSafety). For 2exp11 this declares: no data collected, no data
# shared, no tracking. No secrets in this file.
#
# IMPORTANT — payload format is NOT yet verified here. The endpoint expects a
# `safetyLabels` string whose format mirrors the Play Console Data safety CSV.
# Confirm the exact schema against the current API reference before relying on it:
#   https://developers.google.com/android-publisher/api-ref/rest/v3/applications/dataSafety
# Put the verified payload in store-config/data-safety.json (git-tracked, no secrets).
# This is the ONLY "App content" declaration with an API; content rating (IARC),
# ads, app access and target audience remain web-only in the Play Console.
#
# Reads from .store-passwd (git-ignored) or the environment:
#   GCP_PROJECT
#   PACKAGE_NAME  - app package (default: com.gghez.game2048)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
[ -f "$ROOT/.store-passwd" ] && source "$ROOT/.store-passwd"
PACKAGE_NAME="${PACKAGE_NAME:-com.gghez.game2048}"
PAYLOAD="${1:-$ROOT/store-config/data-safety.json}"
TOKEN="${ACCESS_TOKEN:-$(gcloud auth application-default print-access-token)}"

[ -f "$PAYLOAD" ] || { echo "Payload not found: $PAYLOAD (see header — provide a verified payload)." >&2; exit 1; }

curl -fsS -X POST \
  "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE_NAME}/dataSafety" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  ${GCP_PROJECT:+-H "x-goog-user-project: ${GCP_PROJECT}"} \
  --data-binary "@$PAYLOAD"
echo
echo "Submitted Data safety from $PAYLOAD."
