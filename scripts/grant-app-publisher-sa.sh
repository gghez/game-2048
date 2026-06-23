#!/usr/bin/env bash
# Grant the publishing service account access to a SINGLE app (least privilege),
# instead of account-wide permissions. The app must already exist in the Play
# Console (its package cannot be created via the API). Re-runnable. No secrets.
#
# This is the preferred alternative to invite-publisher-sa.sh, whose account-level
# permissions apply to EVERY app on the developer account.
#
# Needs an owner token with the androidpublisher scope (see scripts/README.md).
# Reads from .store-passwd (git-ignored) or the environment:
#   DEVELOPER_ID, SERVICE_ACCOUNT_EMAIL, GCP_PROJECT
#   PACKAGE_NAME  - app package (default: com.gghez.game2048)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
[ -f "$ROOT/.store-passwd" ] && source "$ROOT/.store-passwd"
: "${DEVELOPER_ID:?set DEVELOPER_ID}"
: "${SERVICE_ACCOUNT_EMAIL:?set SERVICE_ACCOUNT_EMAIL}"
PACKAGE_NAME="${PACKAGE_NAME:-com.gghez.game2048}"
TOKEN="${ACCESS_TOKEN:-$(gcloud auth application-default print-access-token)}"
API="https://androidpublisher.googleapis.com/androidpublisher/v3"
auth=(-H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")
[ -n "${GCP_PROJECT:-}" ] && auth+=(-H "x-goog-user-project: ${GCP_PROJECT}")
EMAIL_ENC="${SERVICE_ACCOUNT_EMAIL/@/%40}"

# 1. The user must exist with NO account-level permissions (app-only access).
#    Create it if absent; ignore "already exists".
if ! curl -fsS "${auth[@]}" "$API/developers/${DEVELOPER_ID}/users?pageSize=-1" \
     | jq -e --arg e "$SERVICE_ACCOUNT_EMAIL" '.users[]?|select(.email==$e)' >/dev/null 2>&1; then
  echo "Creating user (no account-level permissions)..."
  curl -fsS -X POST "${auth[@]}" "$API/developers/${DEVELOPER_ID}/users" \
    -d "{\"email\":\"${SERVICE_ACCOUNT_EMAIL}\",\"developerAccountPermissions\":[]}" >/dev/null
fi

# 2. Grant app-level publishing permissions for this package only.
echo "Granting app-level access to ${PACKAGE_NAME}..."
curl -fsS -X POST "${auth[@]}" \
  "$API/developers/${DEVELOPER_ID}/users/${EMAIL_ENC}/grants" \
  -d "{\"packageName\":\"${PACKAGE_NAME}\",\"appLevelPermissions\":[\"CAN_MANAGE_PUBLIC_APKS\",\"CAN_MANAGE_TRACK_APKS\",\"CAN_MANAGE_PUBLIC_LISTING\"]}"
echo
echo "Done. The SA can publish ${PACKAGE_NAME} only — no access to other apps."
