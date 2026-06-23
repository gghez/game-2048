#!/usr/bin/env bash
# Grant the publishing service account ACCOUNT-WIDE publishing permissions on the
# Play Console developer account via the Android Publisher API (Users.create).
# Re-runnable. No secrets in this file.
#
# WARNING: account-level (*_GLOBAL) permissions apply to EVERY app on the developer
# account, not just one. For least privilege, prefer grant-app-publisher-sa.sh
# (per-app access) once the target app exists in the Console.
#
# WHY THIS NEEDS A SPECIAL TOKEN
# Play Console permissions are NOT GCP IAM — `gcloud` cannot set them, and the
# service account cannot grant itself its own first access (chicken-and-egg). The
# call must be authenticated as a Play Console *owner/admin* (a human Google
# account) with the scope https://www.googleapis.com/auth/androidpublisher.
# Obtain such a token via an interactive ADC re-login that includes the scope
# (only `application-default login` accepts --scopes):
#   gcloud auth application-default login \
#     --scopes=openid,https://www.googleapis.com/auth/cloud-platform,https://www.googleapis.com/auth/androidpublisher
# After that, `gcloud auth application-default print-access-token` yields an
# androidpublisher token. Override with ACCESS_TOKEN=... if you mint it elsewhere.
#
# Sensitive values are read from .store-passwd (git-ignored) or the environment:
#   DEVELOPER_ID           - Play Console developer id (the number in the console
#                            URL .../developers/<DEVELOPER_ID>/...)
#   SERVICE_ACCOUNT_EMAIL  - the play-publisher service account email
#   GCP_PROJECT            - project where Android Publisher API is enabled (quota)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
[ -f "$ROOT/.store-passwd" ] && source "$ROOT/.store-passwd"
: "${DEVELOPER_ID:?set DEVELOPER_ID (number in the Play Console URL)}"
: "${SERVICE_ACCOUNT_EMAIL:?set SERVICE_ACCOUNT_EMAIL}"

TOKEN="${ACCESS_TOKEN:-$(gcloud auth application-default print-access-token)}"

# Account-level publishing permissions (least privilege for CI publishing):
#   CAN_SEE_ALL_APPS                 - see the app(s)
#   CAN_MANAGE_PUBLIC_APKS_GLOBAL    - release to production / Play App Signing
#   CAN_MANAGE_TRACK_APKS_GLOBAL     - manage testing tracks (internal track)
#   CAN_MANAGE_PUBLIC_LISTING_GLOBAL - manage store presence (listing metadata)
read -r -d '' BODY <<JSON || true
{
  "email": "${SERVICE_ACCOUNT_EMAIL}",
  "developerAccountPermissions": [
    "CAN_SEE_ALL_APPS",
    "CAN_MANAGE_PUBLIC_APKS_GLOBAL",
    "CAN_MANAGE_TRACK_APKS_GLOBAL",
    "CAN_MANAGE_PUBLIC_LISTING_GLOBAL"
  ]
}
JSON

echo "Granting $SERVICE_ACCOUNT_EMAIL access to developer $DEVELOPER_ID ..."
curl -fsS -X POST \
  "https://androidpublisher.googleapis.com/androidpublisher/v3/developers/${DEVELOPER_ID}/users" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  ${GCP_PROJECT:+-H "x-goog-user-project: ${GCP_PROJECT}"} \
  -d "${BODY}"
echo
echo "Done. The service account can now publish via Gradle Play Publisher."
