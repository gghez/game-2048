---
name: run-emulator
description: Launch the 2048 debug build on a visible Android emulator (interactive play or manual screenshots), including the WSL2/WSLg KVM + software-GPU setup. Use when asked to run, start, see, or screenshot the app on an emulator.
allowed-tools: Bash
---

# Run on a visible emulator

`scripts/run-emulator.sh` boots the `game2048-shots` AVD **with a window**, installs the
debug APK, and launches the app. It is the interactive companion to the headless
`gen-screenshots.sh`.

## Run

```bash
bash scripts/run-emulator.sh
```

Then play in the window on your desktop, or capture a frame:

```bash
adb exec-out screencap -p > shot.png   # read shot.png to inspect the UI
```

## One-time host setup (WSL2 / Linux)

- **KVM** — x86_64 emulation requires it. If the emulator logs
  *"This user doesn't have permissions to use KVM"*, add yourself to the group once:
  ```bash
  sudo gpasswd -a "$(id -un)" kvm
  ```
  Permanent (survives reboots). The script applies it immediately without a re-login
  by launching through `sg kvm`.
- **Display** — WSLg already provides `DISPLAY=:0`; the emulator window shows up on the
  Windows desktop. No extra X server needed.
- **GPU** — WSLg exposes only software GL (`llvmpipe`), so the script forces
  `-gpu swiftshader_indirect`. CPU stays KVM-accelerated, so it remains responsive.

## Prereqs (installed once via `sdkmanager`)

`emulator`, `platform-tools`, `cmdline-tools`, and
`system-images;android-35;google_apis;x86_64`. The SDK path is read from
`local.properties` (`sdk.dir`) or `$ANDROID_SDK_ROOT`. The script auto-creates the AVD
if missing and builds the debug APK if absent.
