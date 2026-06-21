# Architecture

This document explains how the app is built and, more importantly, *why* the main
decisions were made. For the product vision (free, no ads, no tracking) see
`CLAUDE.md`. For build/run instructions see `README.md`.

## Layers

```
ui/      Jetpack Compose screens + GameViewModel (MVVM)
data/    DataStore repositories (settings, score, saved game) + leaderboard
domain/  pure game engine — no Android, unit-tested
```

Dependencies point inward: `ui` depends on `data` and `domain`; `data` depends on
`domain`; `domain` depends on nothing Android. This keeps the rules portable and
testable and prevents UI concerns from leaking into game logic.

## Domain engine

The engine is plain Kotlin. The board is a 16-cell, row-major list of nullable
tiles. A move slides and merges each line toward the destination edge.

Key decision — **stable tile identity**. Each tile has an id. When a tile only
slides, it keeps its id. When two tiles merge, the result reuses the *surviving*
tile's id (with the doubled value) and the absorbed tile disappears. This matters
because the UI animates by id: a composable tied to an id survives across moves, so
its position can animate from the old cell to the new one. Without stable ids the
board would be redrawn from scratch every move (the earlier implementation did
this, which caused tiles to "pop" constantly and inconsistently).

Key decision — **determinism by injection**. Tile spawning goes through a
`TileSpawner` interface. Production uses a random implementation; tests use a
seeded one, so move/spawn/win/loss behaviour is fully reproducible.

`move` is pure and spawn-free; spawning and loss detection are a separate step.
This lets each rule be unit-tested in isolation.

## State & persistence

`GameViewModel` holds the authoritative `GameState` and a timer/move counter, and
exposes one immutable `GameUiState` via `StateFlow`. The UI is a pure function of
that snapshot.

Persistence uses DataStore (Preferences): best score, the four settings, and the
in-progress game (serialized as a compact delimited string to avoid a JSON
dependency for a single small object). The game is saved on every change so it
resumes after the process is killed.

## Rendering & animation

The board renders a static background grid of empty cells, then draws live tiles on
top in **absolute position**, each `key(id)`-ed and offset to `(col, row) * cell`.
When the engine moves a tile, its target offset changes and Compose animates it —
that is the slide. Appear and merge add a scale "pop". The "fast animations"
setting scales every duration.

Digit size is proportional to the tile (system font, no bundled typeface) so
numbers fill the tile like the design and stay visually consistent.

## Leaderboards

Leaderboards are behind a `LeaderboardRepository` interface with two
implementations: a real Google Play Games (v2) one and a Noop fallback. A provider
returns the real one only when game ids are configured (injected from
`local.properties` as string resources); otherwise it returns Noop.

This is a deliberate guard: Play Games v2 auto-initializes at launch and requires a
valid numeric `APP_ID`, so the related manifest meta-data is left commented out by
default. The result is that the app always builds and runs without a Play Console
account, while the real integration is one documented opt-in away. The v2 clients
need an Activity, which the host binds to the repository via an `attach` hook
(held weakly to avoid leaks).

Two boards mirror the design: *Vitesse d'exécution* (best score) and *Optimisation
des déplacements* (best tile reached).

## Dependency injection

A single `AppContainer`, created in the `Application`, builds the repositories and
hands them to the ViewModel through a factory. No DI framework — the dependency
graph is tiny and explicit on purpose.

## Theming

Two Material 3 schemes (light/dark) over a taupe/cream palette extracted from the
design, plus a per-value tile color ramp. The active theme follows the in-app
setting rather than the system, to match the explicit selector in the mockups.
