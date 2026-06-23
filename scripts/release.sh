#!/usr/bin/env bash
# Build the signed AAB and (optionally) upload it to Google Play via Gradle Play
# Publisher. Requires local.properties wired for signing and play-service-account.json
# at the repo root (both git-ignored). No secrets in this file.
#
# Usage:
#   scripts/release.sh build        # build the signed AAB only (default)
#   scripts/release.sh publish      # build + upload to the internal track
#   scripts/release.sh promote      # promote internal -> production (no rebuild)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
case "${1:-build}" in
  build)   ./gradlew bundleRelease ;;
  publish) ./gradlew bundleRelease publishReleaseBundle ;;
  promote) ./gradlew promoteArtifact --from-track internal --promote-track production ;;
  *) echo "usage: $0 [build|publish|promote]" >&2; exit 2 ;;
esac
