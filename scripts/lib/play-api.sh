#!/usr/bin/env bash
# Shared helpers for the Android Publisher API scripts. SOURCE this file; do not run
# it. It exists so the "which token?" rule lives in ONE place instead of being
# repeated across every script.
#
# Token model — owner token vs CI/SA token (the single source of truth):
#   * ACCESS_TOKEN set    -> use it verbatim. CI injects a short-lived,
#       Workload-Identity-Federation-minted `androidpublisher` token this way
#       (see .github/workflows/release.yml); you can also paste a token you minted
#       elsewhere.
#   * ACCESS_TOKEN unset  -> fall back to the local OWNER token via Application
#       Default Credentials (`gcloud auth application-default print-access-token`).
#
# Why an owner token locally: Play Console permissions are NOT GCP IAM, and a
# `:commit` (listing / data safety / release submission) needs a Play Console
# OWNER/admin identity with the `androidpublisher` scope — the publisher service
# account cannot grant itself its first access. gcloud's built-in OAuth client does
# not whitelist that scope for `print-access-token`; only `application-default
# login` accepts `--scopes`, so log ADC in once with the scope:
#
#   gcloud auth application-default login \
#     --scopes=openid,https://www.googleapis.com/auth/cloud-platform,https://www.googleapis.com/auth/androidpublisher
#
# After sourcing:
#   play_load_env   -> source .store-passwd (git-ignored identifiers), if present
#   play_api_init   -> populate PLAY_TOKEN and the PLAY_AUTH curl header array

# Repo root, resolved from this lib's own location (scripts/lib/ -> ../../).
PLAY_REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Load git-ignored identifiers (GCP_PROJECT, PACKAGE_NAME, DEVELOPER_ID,
# SERVICE_ACCOUNT_EMAIL, ...) without hard-coding any of them.
play_load_env() {
  # shellcheck disable=SC1091
  [ -f "$PLAY_REPO_ROOT/.store-passwd" ] && source "$PLAY_REPO_ROOT/.store-passwd"
}

# Set PLAY_TOKEN (bearer) and PLAY_AUTH (curl -H args, incl. quota project header).
play_api_init() {
  PLAY_TOKEN="${ACCESS_TOKEN:-$(gcloud auth application-default print-access-token)}"
  PLAY_AUTH=(-H "Authorization: Bearer $PLAY_TOKEN")
  [ -n "${GCP_PROJECT:-}" ] && PLAY_AUTH+=(-H "x-goog-user-project: $GCP_PROJECT")
}
