# 2048 Android — Design Specification

Date: 2026-06-21
Status: Approved (pending spec review)

## Vision

A free, open-source 2048 game for Android. **No advertising, no in-app
purchases, no hidden economic model, no tracking, no analytics.** The game is a
gift: complete, polished, and free forever. This vision is non-negotiable and is
recorded in `CLAUDE.md` at the repo root.

## Goals

- Faithful 2048 gameplay matching the provided mockups (taupe/cream theme).
- Native, modern Android implementation.
- Real Google Play Games leaderboard integration, with a fallback that lets the
  project build and run without a Play Console account.
- 100% open source under the MIT license, published at `github.com/gghez/game-2048` (public).

## Technical Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3), single-Activity.
- **Async/state:** Coroutines + `StateFlow`.
- **Persistence:** Jetpack DataStore (Preferences).
- **Leaderboard:** Google Play Games Services SDK (`play-games-services-v2`).
- **Build:** Gradle (Kotlin DSL), AGP + Kotlin plugins.
- **Min SDK:** 24 (Android 7.0). **Target/Compile SDK:** 35.
- **Testing:** JUnit + kotlin.test for the pure domain engine (TDD); optional
  instrumented tests for the ViewModel.
- **No paid dependencies. No ad SDKs. No analytics SDKs.**

## Architecture

MVVM with a framework-free domain layer.

```
app/src/main/java/com/gghez/game2048/
├─ domain/                 # PURE Kotlin, no Android imports → unit-testable
│   ├─ Tile.kt             # value + id (for stable animations)
│   ├─ Direction.kt        # UP/DOWN/LEFT/RIGHT
│   ├─ Board.kt            # 4x4 immutable grid representation
│   ├─ GameStatus.kt       # Playing / Won / Lost
│   ├─ GameState.kt        # board + score + moves + status
│   ├─ MoveResult.kt       # new state + list of tile movements/merges (for animation)
│   ├─ RandomTileSpawner.kt# spawns 2 (90%) / 4 (10%); seedable for tests
│   └─ GameEngine.kt       # move(state, dir) -> MoveResult; new game; game-over check
├─ data/
│   ├─ settings/
│   │   ├─ SettingsRepository.kt   # DataStore: theme, fast anim, vibration, orientation
│   │   └─ GameSettings.kt         # data class
│   ├─ score/
│   │   └─ ScoreRepository.kt      # DataStore: best score (local) + saved in-progress game
│   └─ leaderboard/
│       ├─ LeaderboardRepository.kt      # interface
│       ├─ PlayGamesLeaderboard.kt       # real GPGS implementation
│       ├─ NoopLeaderboard.kt            # fallback when GPGS not configured
│       └─ LeaderboardProvider.kt        # picks impl based on configured game IDs
├─ ui/
│   ├─ game/
│   │   ├─ GameViewModel.kt        # holds GameState, timer, move count; exposes StateFlow<GameUiState>
│   │   ├─ GameUiState.kt
│   │   └─ GameScreen.kt           # header + score cards + grid + footer
│   ├─ settings/
│   │   └─ SettingsBottomSheet.kt
│   ├─ components/
│   │   ├─ TileView.kt             # animated tile
│   │   ├─ GameGrid.kt             # 4x4 layout + swipe gesture detection
│   │   ├─ ScoreCard.kt
│   │   └─ NewGameDialog.kt
│   └─ theme/
│       ├─ Color.kt               # taupe palette + per-tile colors (light & dark)
│       ├─ Theme.kt               # light/dark Material 3 themes
│       └─ Type.kt
└─ MainActivity.kt                 # hosts GameScreen; applies orientation setting
```

### Dependency injection

Lightweight manual DI (a small `AppContainer` created in `Application`). No
Hilt/Dagger — keeps the project minimal and dependency-light, consistent with
the "no bloat" vision.

## Domain Engine (TDD target)

The engine is pure and deterministic (random spawn is injected). Developed
test-first. Test cases:

- **Slide & merge** in each of the 4 directions:
  - `[2,2,_,_]` left → `[4,_,_,_]`, score +4.
  - `[2,2,2,2]` left → `[4,4,_,_]` (each tile merges at most once per move).
  - `[4,4,2,2]` left → `[8,4,_,_]`.
  - `[2,_,2,_]` left → `[4,_,_,_]`.
  - No-op move (nothing slides/merges) → reported as `moved = false`, no spawn.
- **Spawn:** after a successful move, exactly one new tile (2 or 4) appears in a
  random empty cell. With a seeded spawner the position/value is deterministic.
- **Win:** reaching 2048 sets status `Won`; play can continue past 2048.
- **Lose:** board full and no adjacent equal tiles → status `Lost`.
- **Move tracking:** `MoveResult` lists per-tile source→destination + merge flags
  so the UI can animate slides and merges.

## UI / Screens (faithful to mockups)

### GameScreen
- **Header:** circular 2048 logo (4-square icon), title "2048", and three icon
  buttons: leaderboard (medal/star), restart (circular arrow), settings (gear).
- **Score row:** "SCORE" card (current) and "MEILLEUR SCORE" card (best).
- **Grid:** 4×4 rounded board, empty cells in muted taupe; tiles with value
  colors; swipe gestures (up/down/left/right) drive moves; animated appear,
  slide, and merge (pop) transitions.
- **Footer:** elapsed time `MM:SS` (left) and `N COUPS` move counter (right).
- **Undo:** discreet undo control to revert the last move (single-level undo,
  not in mockups but requested).
- **Win/Lose overlays:** "Vous avez gagné !" (continue / new game) and game-over.

### SettingsBottomSheet
- Theme selector: **Thème clair** / **Thème sombre** (checkmark on active).
- Toggle **Accélérer les animations** ("Enchaînez les combos plus rapidement.").
- Toggle **Activer les vibrations** ("Vibration surprise pour un 2048.").
- Orientation selector: **Portrait** / **Auto** / **Landscape** (checkmark on active).

### NewGameDialog
- Modal "Nouvelle Partie", body "Êtes-vous bien certain de vouloir démarrer une
  nouvelle partie ? La partie en cours sera définitivement perdue.", buttons
  **DÉMARRER** (filled) / **ANNULER** (outlined).

### Leaderboard (Classements)
- Tapping the leaderboard icon opens the **native Google Play Games** leaderboard
  UI when GPGS is configured and the user is signed in.
- Two leaderboards mirror the mockup:
  - **Vitesse d'exécution** — best score (with time as tiebreak/context).
  - **Optimisation des déplacements** — best tile reached (with move count).
- When GPGS is not configured (no game IDs) or sign-in fails, the icon shows a
  small "classement indisponible" message; local best score still works.

## Theme & Colors (extracted from mockups)

Light theme:
- App background: `#FAF8EF`
- Board background: `#8C7B68`
- Empty cell: `#A89B89`
- Primary text / brown: `#5C5248`
- Accent (buttons like DÉMARRER): `#8A7A66`

Per-tile colors (observed: 2 cream, 8 orange, 32 coral) completed into a coherent ramp:
- 2: bg `#EEE8DC`, text `#5C5248`
- 4: bg `#EDE0C8`, text `#5C5248`
- 8: bg `#E8A96B`, text `#FFFFFF`
- 16: bg `#E8965C`, text `#FFFFFF`
- 32: bg `#E86B5C`, text `#FFFFFF`
- 64: bg `#E5503E`, text `#FFFFFF`
- 128–2048: graduated warm gold/amber tones, text `#FFFFFF`
- ≥4096: deep brown `#3D3530`, text `#FFFFFF`

Dark theme: dark taupe/charcoal background derived from the same hues; tile
colors kept recognizable with adjusted contrast. Driven by the theme setting
(not the system) so it matches the in-app selector.

## Persistence

DataStore (Preferences):
- `best_score: Int`
- `theme: light|dark`
- `fast_animations: Bool`
- `vibration: Bool`
- `orientation: portrait|auto|landscape`
- In-progress game (serialized board + score + moves + elapsed) so the game
  resumes after the app is killed.

## Game Rules

- Start: two tiles spawned (each 2 at 90% / 4 at 10%).
- A move slides all tiles in the chosen direction; equal adjacent tiles merge
  once per move; score increases by the merged value.
- After any move that changes the board, spawn one new tile.
- Win at 2048 (continue allowed). Lose when the board is full and no move is
  possible.
- Timer counts up from the first move (or game start); pauses when app
  backgrounded; resets on new game.
- Move counter increments on each board-changing move.

## Google Play Games configuration

- Requires (from the user's Play Console): an **App ID** and **two leaderboard
  IDs**. These go in `app/src/main/res/values/game-ids.xml` (or
  `local.properties`), git-ignored / templated.
- A committed template `game-ids.xml.template` documents the required keys.
- `LeaderboardProvider` returns `PlayGamesLeaderboard` when a real App ID is
  present, else `NoopLeaderboard`, so the app builds and runs without Play
  Console.

## Build & Run

- `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
- Install on a physical device over adb (already installed): `adb install -r <apk>`.
- The Android SDK + command-line tools must be installed first (see Tooling).

## Tooling to install (WSL2)

- Android command-line tools + SDK (platform-tools, platforms;android-35,
  build-tools;35.x) under `$HOME/Android/Sdk`, `ANDROID_HOME` exported.
- Gradle wrapper committed (no global Gradle needed).
- Java 17 already present.

## Open Source Deliverables

- `CLAUDE.md` — vision (free, no ads, no hidden economy, no tracking) + stack.
- `LICENSE` — MIT, holder "gghez".
- `README.md` — screenshots, build/run instructions, GPGS setup, contributing,
  explicit "no ads / no monetization" statement.
- `.gitignore` — Android/Gradle standard + `game-ids.xml` + `local.properties`.
- Public repo `gghez/game-2048` created via `gh repo create`.

## Out of Scope (v1)

- iOS / multiplatform.
- Online multiplayer.
- Cloud save sync (beyond GPGS leaderboard).
- Larger boards (5×5, 6×6) / variant modes.
- Multi-level undo (single-level only).
```
