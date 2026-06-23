#!/usr/bin/env bash
# Create the dedicated GCP project, the "play-publisher" service account, its JSON
# key, and enable the Android Publisher API. Re-runnable (skips existing resources).
#
# No secrets in this file. The generated key (play-service-account.json) is
# git-ignored. Sensitive identifiers are read from .store-passwd (git-ignored) or
# the environment:
#   GCP_PROJECT   - GCP project id dedicated to Play publishing
#
# Usage:  GCP_PROJECT=<id> scripts/create-publisher-sa.sh
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
[ -f "$ROOT/.store-passwd" ] && source "$ROOT/.store-passwd"
: "${GCP_PROJECT:?set GCP_PROJECT (dedicated Play-publishing project id)}"

SA_NAME="play-publisher"
SA="${SA_NAME}@${GCP_PROJECT}.iam.gserviceaccount.com"

gcloud projects describe "$GCP_PROJECT" >/dev/null 2>&1 \
  || gcloud projects create "$GCP_PROJECT" --name="2exp11 Play Publishing"
gcloud iam service-accounts describe "$SA" --project "$GCP_PROJECT" >/dev/null 2>&1 \
  || gcloud iam service-accounts create "$SA_NAME" --display-name "Play Publisher" --project "$GCP_PROJECT"
gcloud services enable androidpublisher.googleapis.com --project "$GCP_PROJECT"
if [ ! -f "$ROOT/play-service-account.json" ]; then
  gcloud iam service-accounts keys create "$ROOT/play-service-account.json" \
    --iam-account "$SA" --project "$GCP_PROJECT"
fi
echo "Service account ready: $SA"
echo "Record GCP_PROJECT and SERVICE_ACCOUNT_EMAIL=$SA in .store-passwd."
echo "Then grant it Play Console access: scripts/invite-publisher-sa.sh"
