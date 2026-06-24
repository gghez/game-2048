#!/usr/bin/env bash
# Launch the debug build on a *visible* emulator window for interactive play or
# manual screenshots. Companion to gen-screenshots.sh (which runs headless). No secrets.
#
# WSL2 / Linux notes:
#   - x86_64 emulation needs KVM. If you hit "permissions to use KVM", add yourself
#     to the kvm group once:
#         sudo gpasswd -a "$(id -un)" kvm
#     It is permanent (survives reboots). This script applies it without a re-login
#     by re-execing the emulator through `sg kvm`.
#   - Under WSLg the GPU is software-only (llvmpipe), so we render with
#     swiftshader_indirect. The window still appears on the Windows desktop via WSLg
#     (DISPLAY=:0). The CPU is KVM-accelerated, so the emulator stays responsive.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$(sed -n 's/^sdk\.dir=//p' "$ROOT/local.properties" 2>/dev/null)}}"
SDK="${SDK:-$HOME/Android/Sdk}"
AVD="${AVD_NAME:-game2048-shots}"
IMG="system-images;android-35;google_apis;x86_64"
PKG="com.gghez.game2048"
EMU="$SDK/emulator/emulator"
ADB="$SDK/platform-tools/adb"; command -v adb >/dev/null 2>&1 && ADB="$(command -v adb)"
AVDM="$SDK/cmdline-tools/latest/bin/avdmanager"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"

# Build the debug APK on demand.
[ -f "$APK" ] || (cd "$ROOT" && ./gradlew :app:assembleDebug)

# Create the AVD if absent (Nexus 5, 1080x1920).
"$AVDM" list avd 2>/dev/null | grep -q "Name: $AVD" \
  || echo "no" | "$AVDM" create avd -n "$AVD" -k "$IMG" -d "Nexus 5" --force

# Boot the emulator with a window, picking a launcher that has KVM access.
EMU_CMD="'$EMU' -avd '$AVD' -no-snapshot -no-boot-anim -gpu swiftshader_indirect"
if [ -r /dev/kvm ] && [ -w /dev/kvm ]; then
  nohup bash -c "$EMU_CMD" >/dev/null 2>&1 &
elif id -nG | tr ' ' '\n' | grep -qx kvm; then
  # Member of the kvm group but not yet effective in this shell — apply via sg.
  nohup sg kvm -c "$EMU_CMD" >/dev/null 2>&1 &
else
  echo "No KVM access. Run once:  sudo gpasswd -a \"$(id -un)\" kvm   then re-run this script." >&2
  exit 1
fi
disown 2>/dev/null || true

# Wait for full boot, then install + launch.
"$ADB" wait-for-device
until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
"$ADB" shell wm dismiss-keyguard >/dev/null 2>&1 || true
"$ADB" install -r "$APK"
"$ADB" shell am start -n "$PKG/.MainActivity"

echo "Emulator '$AVD' is up and $PKG launched. The window is on your desktop (WSLg)."
echo "Capture a frame with:  $ADB exec-out screencap -p > shot.png"
