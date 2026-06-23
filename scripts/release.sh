#!/usr/bin/env bash
# Build the signed AAB and (optionally) upload it to Google Play via Gradle Play
# Publisher. Requires local.properties wired for signing and play-service-account.json
# at the repo root (both git-ignored). No secrets in this file.
#
# Usage:
#   scripts/release.sh build        # build the signed AAB only (default)
#   scripts/release.sh listing      # push store listing text + graphics (no release)
#   scripts/release.sh publish      # build + upload AAB to the internal track
#   scripts/release.sh promote      # promote internal -> production (no rebuild)
#
# Store listing/graphics live (versioned) under app/src/main/play/ — edit those
# files then run `listing` to update the Play Store entry. Requires the SA to have
# CAN_MANAGE_PUBLIC_LISTING on the app (see grant-app-publisher-sa.sh).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
case "${1:-build}" in
  build)   ./gradlew bundleRelease ;;
  listing) ./gradlew publishListing ;;
  publish) ./gradlew bundleRelease publishReleaseBundle ;;
  promote) ./gradlew promoteArtifact --from-track internal --promote-track production ;;
  *) echo "usage: $0 [build|listing|publish|promote]" >&2; exit 2 ;;
esac
