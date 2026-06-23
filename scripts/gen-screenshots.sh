#!/usr/bin/env bash
# Generate Play Store phone screenshots by running the debug build on a headless
# emulator and capturing a few game states. No secrets.
#
# Prereqs (install once via sdkmanager):
#   emulator, platform-tools, "system-images;android-35;google_apis;x86_64"
# Uses a Nexus 5 AVD (1080x1920, 16:9 — within Play's <=2:1 ratio rule).
# Output: app/src/main/play/listings/fr-FR/graphics/phone-screenshots/{1,2,3}.png
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
AVD="${AVD_NAME:-game2048-shots}"
IMG="system-images;android-35;google_apis;x86_64"
PKG="com.gghez.game2048"
EMU="$SDK/emulator/emulator"; ADB="$SDK/platform-tools/adb"
AVDM="$SDK/cmdline-tools/latest/bin/avdmanager"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
OUT="$ROOT/app/src/main/play/listings/fr-FR/graphics/phone-screenshots"
mkdir -p "$OUT"

[ -f "$APK" ] || (cd "$ROOT" && ./gradlew assembleDebug)

# Create the AVD if absent
"$AVDM" list avd 2>/dev/null | grep -q "Name: $AVD" \
  || echo "no" | "$AVDM" create avd -n "$AVD" -k "$IMG" -d "Nexus 5" --force

# Boot headless
"$EMU" -avd "$AVD" -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect &
trap '"$ADB" emu kill >/dev/null 2>&1 || true' EXIT
"$ADB" wait-for-device
until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
"$ADB" shell wm dismiss-keyguard >/dev/null 2>&1 || true

# Install + launch
"$ADB" install -r "$APK"
"$ADB" shell am start -n "$PKG/.MainActivity"
sleep 6

size=$("$ADB" shell wm size | grep -o '[0-9]\+x[0-9]\+' | head -1)
W=${size%x*}; H=${size#*x}
cx=$((W/2)); top=$((H*70/100)); bot=$((H*30/100)); midy=$((H/2))
shot() { "$ADB" exec-out screencap -p > "$OUT/$1.png"; echo "captured $1.png"; }

shot 1                                   # fresh board
for _ in 1 2 3 4 5 6 7 8; do
  "$ADB" shell input swipe $cx $top $cx $bot 80
  "$ADB" shell input swipe $bot $midy $top $midy 80
  sleep 0.4
done
shot 2                                   # mid game
for _ in 1 2 3 4 5 6; do
  "$ADB" shell input swipe $cx $bot $cx $top 80
  "$ADB" shell input swipe $top $midy $bot $midy 80
  sleep 0.4
done
shot 3                                   # later state
echo "Done. Screenshots in $OUT"
