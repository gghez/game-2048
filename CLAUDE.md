# game-2048 — Project Vision & Stack

## Vision (non-negotiable)

This is a **free gift to players**. Forever.

- **No advertising.** No ad SDKs, no banners, no interstitials, no rewarded video.
- **No in-app purchases.** No "remove ads", no cosmetics, no boosters.
- **No hidden economic model.** No paywalls, no premium tier, no donation nags.
- **No tracking.** No analytics SDKs, no telemetry, no third-party data collection.
- Open source under the MIT license.

Any change that introduces monetization, ads, or tracking contradicts the
purpose of this project and must be rejected.

## Stack

- Kotlin + Jetpack Compose (Material 3), single-Activity, MVVM.
- Pure, framework-free domain engine (`domain/`), unit-tested (TDD).
- DataStore (Preferences) for settings, best score, and saved game.
- Google Play Games Services v2 for leaderboards (optional; app runs without it).
- Gradle (Kotlin DSL). Min SDK 24, Target/Compile SDK 35, Java 17.

## Conventions

- Code/docs/comments in English; user-facing strings in French (`res/values/strings.xml`).
- No paid or tracking dependencies, ever.
- Domain logic stays Android-free and unit-tested.

## Build & test

- `./gradlew testDebugUnitTest` — run the domain unit tests.
- `./gradlew assembleDebug` — produce `app/build/outputs/apk/debug/app-debug.apk`.
- `adb install -r app/build/outputs/apk/debug/app-debug.apk` — install on a device.

## Google Play Games (optional)

Leaderboards are disabled by default so the app builds and runs without a Play
Console account. To enable them:

1. Add your ids to `local.properties` (never committed):
   ```
   playGamesAppId=1234567890
   leaderboardSpeed=CgkI...speed
   leaderboardEfficiency=CgkI...efficiency
   ```
2. Uncomment the `com.google.android.gms.games.APP_ID` meta-data in
   `app/src/main/AndroidManifest.xml`.

Two leaderboards mirror the design: **Vitesse d'exécution** (best score) and
**Optimisation des déplacements** (best tile reached).
