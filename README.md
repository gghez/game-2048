# 2048

A free, open-source **2048** game for Android.

> **No ads. No in-app purchases. No tracking. No hidden economic model. Forever.**
> This game is a gift. See [CLAUDE.md](CLAUDE.md) for the full, non-negotiable vision.

## Features

- Classic 4×4 2048 gameplay with smooth tile animations.
- Score and best score (persisted locally).
- Move timer (`MM:SS`) and move counter.
- Single-level **undo**.
- Sound (on by default) and vibration toggles, with a synthesized swipe/merge cue.
- Light / dark theme and a faster-animations toggle.
- Portrait orientation only.
- "New game" confirmation dialog.
- In-progress game is saved and resumed automatically.
- Optional **Google Play Games** leaderboards (two boards: best score and best tile).

All UI text is in French.

## Tech stack

- Kotlin + Jetpack Compose (Material 3), single-Activity, MVVM.
- Pure, framework-free game engine in `domain/`, covered by unit tests.
- Jetpack DataStore for persistence.
- Google Play Games Services v2 for leaderboards (optional).
- Gradle (Kotlin DSL). Min SDK 24, Target/Compile SDK 35, Java 17.

## Build & run

Prerequisites: JDK 17 and the Android SDK (platform 35, build-tools 35.0.0).
Point Gradle at your SDK in `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

Then:

```bash
# Run the domain unit tests
./gradlew testDebugUnitTest

# Build a debug APK
./gradlew assembleDebug

# Install on a connected device (USB debugging enabled)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Google Play Games leaderboards (optional)

Leaderboards are **disabled by default** so the project builds and runs without a
Google Play Console account (it falls back to a no-op leaderboard). To enable
them:

1. Create a game in the Google Play Console and create two leaderboards
   ("Vitesse d'exécution" for best score, "Optimisation des déplacements" for
   best tile).
2. Add the ids to `local.properties` (never committed):

   ```properties
   playGamesAppId=1234567890
   leaderboardSpeed=CgkI...speed
   leaderboardEfficiency=CgkI...efficiency
   ```

3. Uncomment the `com.google.android.gms.games.APP_ID` meta-data block in
   `app/src/main/AndroidManifest.xml`.

> The Play Games v2 SDK auto-initializes at launch and requires a valid numeric
> `APP_ID`; that is why the meta-data is commented out until you configure it.

## Project layout

```
app/src/main/java/com/gghez/game2048/
  domain/      pure game engine (board, slide/merge, spawn, win/loss) — unit tested
  data/        DataStore repositories (settings, score, saved game) + leaderboard
  ui/          Compose theme, components, settings sheet, game screen, view model
docs/superpowers/   design spec and implementation plan
```

## Contributing

Contributions are welcome, with one hard rule: **nothing that adds ads,
monetization, or tracking** will be accepted. Keep the domain layer Android-free
and unit-tested.

## License

[MIT](LICENSE) © 2026 gghez
