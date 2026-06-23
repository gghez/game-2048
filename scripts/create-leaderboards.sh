#!/usr/bin/env bash
# Create the three 2exp11 leaderboards on Play Games Services via the Games
# Configuration API (gamesconfiguration.googleapis.com). Idempotent: a board whose
# name already exists is skipped. No secrets here.
#
# Scope of automation: the API supports leaderboard creation, so this is scripted.
# The REST of Play Games Services setup has NO web-free path and stays manual in the
# Play Console: OAuth consent screen + an Android credential bound to the signing
# SHA-1 + "Review and publish". (The IAP OAuth Admin API that once created consent
# screens is deprecated — shut down 2026-03-19 — and never worked for personal
# Google accounts anyway.)
#
# Auth: the Games Configuration API requires an OWNER on the games project; the
# publisher service account is NOT granted Games Services access, so use the owner's
# Application Default Credentials:
#   gcloud auth application-default login
# The API also needs a quota project header (GCP_PROJECT) because ADC bills the call.
#
# Env (from .store-passwd or the environment):
#   PLAY_GAMES_APP_ID  numeric Play Games application id (= the games project id /
#                      the value behind the games.APP_ID manifest meta-data)
#   GCP_PROJECT        the publishing GCP project, used as the API quota project
#   ACCESS_TOKEN       optional; defaults to the owner ADC token
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
[ -f "$ROOT/.store-passwd" ] && source "$ROOT/.store-passwd"
APPID="${PLAY_GAMES_APP_ID:?set PLAY_GAMES_APP_ID (numeric games app id)}"
QP="${GCP_PROJECT:?set GCP_PROJECT (used as API quota project)}"
TOKEN="${ACCESS_TOKEN:-$(gcloud auth application-default print-access-token)}"
BASE="https://www.googleapis.com/games/v1configuration/applications/$APPID/leaderboards"
# Read headers (auth + quota) and write headers (also Content-Type).
RH=(-H "Authorization: Bearer $TOKEN" -H "x-goog-user-project: $QP")
WH=("${RH[@]}" -H "Content-Type: application/json")

# Enable the API (idempotent / no-op if already on).
gcloud services enable gamesconfiguration.googleapis.com --project="$QP" >/dev/null

# Note: fr-FR is not an enabled project language, so the French label is stored
# under the project default locale (en-US); a French-only game shows it to everyone.
create_board() { # name  scoreOrder  numberFormatType  sortRank
  local name="$1" order="$2" fmt="$3" rank="$4"
  if curl -fsS "${RH[@]}" "$BASE" \
       | jq -e --arg n "$name" '.items[]?.draft.name.translations[]? | select(.value==$n)' \
       >/dev/null 2>&1; then
    echo "exists:  $name"; return
  fi
  local sf
  if [ "$fmt" = "TIME_DURATION" ]; then sf='{numberFormatType:$fmt}'
  else sf='{numberFormatType:$fmt,numDecimalPlaces:0}'; fi
  local body id
  body=$(jq -n --arg n "$name" --arg o "$order" --arg fmt "$fmt" --argjson r "$rank" \
    "{scoreOrder:\$o, draft:{sortRank:\$r, name:{translations:[{locale:\"en-US\",value:\$n}]}, scoreFormat:$sf}}")
  id=$(curl -fsS "${WH[@]}" -X POST "$BASE" -d "$body" | jq -r '.id')
  echo "created: $name -> $id"
}

# Mapping to LeaderboardKind in the app: SPEED=best score, EFFICIENCY=best tile,
# TIME_TO_2048=fastest time to 2048 (submitted in ms; Time type, smaller is better).
create_board "Meilleur score"  LARGER_IS_BETTER  NUMERIC       1
create_board "Meilleure tuile" LARGER_IS_BETTER  NUMERIC       2
create_board "Temps pour 2048" SMALLER_IS_BETTER TIME_DURATION 3

echo
echo "Board ids (set these as GitHub secrets / local.properties via set-github-secrets.sh):"
curl -fsS "${RH[@]}" "$BASE" \
  | jq -r '.items[] | "  \(.draft.name.translations[0].value)\t\(.id)\t\(.scoreOrder)\t\(.draft.scoreFormat.numberFormatType)"'
