#!/usr/bin/env bash
# Push every GitHub Actions secret that .github/workflows/release.yml consumes.
# Values come from .store-passwd + the upload keystore + play-service-account.json.
# No secret value is ever printed. No secret is stored in the repo.
#
# Prereqs: gh authenticated with repo scope; the upload keystore present; the
# leaderboard ids recorded in .store-passwd (see create-leaderboards.sh).
#
# Env (from .store-passwd or the environment):
#   KEYSTORE_FILE, STORE_PASSWORD, KEYSTORE_ALIAS, KEY_PASSWORD   (signing)
#   PLAY_GAMES_APP_ID, LEADERBOARD_SPEED, LEADERBOARD_EFFICIENCY, LEADERBOARD_TIME
#   REPO   target repo (default gghez/game-2048)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
[ -f "$ROOT/.store-passwd" ] && source "$ROOT/.store-passwd"
REPO="${REPO:-gghez/game-2048}"
SA="$ROOT/play-service-account.json"
: "${KEYSTORE_FILE:?}" "${STORE_PASSWORD:?}" "${KEYSTORE_ALIAS:?}" "${KEY_PASSWORD:?}"
: "${PLAY_GAMES_APP_ID:?}" "${LEADERBOARD_SPEED:?}" "${LEADERBOARD_EFFICIENCY:?}" "${LEADERBOARD_TIME:?}"
[ -f "$KEYSTORE_FILE" ] || { echo "keystore not found: $KEYSTORE_FILE"; exit 1; }
[ -f "$SA" ] || { echo "missing $SA"; exit 1; }

set_secret() { # name  value
  printf '%s' "$2" | gh secret set "$1" -R "$REPO" >/dev/null && echo "set $1"
}

base64 -w0 "$KEYSTORE_FILE" | gh secret set UPLOAD_KEYSTORE_BASE64 -R "$REPO" >/dev/null && echo "set UPLOAD_KEYSTORE_BASE64"
set_secret RELEASE_STORE_PASSWORD "$STORE_PASSWORD"
set_secret RELEASE_KEY_ALIAS      "$KEYSTORE_ALIAS"
set_secret RELEASE_KEY_PASSWORD   "$KEY_PASSWORD"
gh secret set PLAY_SERVICE_ACCOUNT_JSON -R "$REPO" < "$SA" >/dev/null && echo "set PLAY_SERVICE_ACCOUNT_JSON"
set_secret PLAY_GAMES_APP_ID      "$PLAY_GAMES_APP_ID"
set_secret LEADERBOARD_SPEED      "$LEADERBOARD_SPEED"
set_secret LEADERBOARD_EFFICIENCY "$LEADERBOARD_EFFICIENCY"
set_secret LEADERBOARD_TIME       "$LEADERBOARD_TIME"

echo
echo "Secrets on $REPO:"; gh secret list -R "$REPO" | awk '{print "  "$1}'
