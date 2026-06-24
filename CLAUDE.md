# game-2048 — Vision & Architectural Stance

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
- Coroutines + `StateFlow` for state; DataStore (Preferences) for persistence.
- Google Play Games Services v2 for leaderboards (optional).
- Gradle (Kotlin DSL). Min SDK 24, Target/Compile SDK 35, Java 17.

## Architectural stance (the "why")

- **Pure domain core.** Game rules live in `domain/`, free of any Android import,
  so they are fast to unit-test and impossible to couple to the UI.
- **Stable tile identity.** A tile keeps its id while it slides; a merge reuses the
  surviving tile's id. This identity is what lets the UI animate movement instead
  of redrawing — the rendering choice depends on this domain guarantee.
- **Deterministic by injection.** Randomness (tile spawning) is injected, so the
  engine is reproducible and testable with a seed.
- **Leaderboard behind an interface.** A Noop fallback keeps the app fully playable
  with no Google Play Console account; the real implementation is opt-in.
- **Manual, minimal DI.** A small container created in the Application — no DI
  framework — to keep the dependency surface tiny.
- **Settings-driven theme.** Light/dark follows the in-app setting, not the system,
  to match the explicit selector in the design.

## Conventions

- Code, docs, and comments in English. User-facing strings are localized: the
  default `res/values/strings.xml` is English; translations live in `res/values-<lang>/`
  (fr, es, de, it, ja, ko, zh, iw — Hebrew uses the legacy `iw` folder). Keep every
  locale in sync and accents/scripts correct. Shipped languages are declared in
  `res/xml/locales_config.xml`.
- No paid or tracking dependencies, ever.
- Domain logic stays Android-free and unit-tested.
- When you change anything deployment-related (signing, publishing, store assets,
  hosting, cloud resources), update `docs/agent-references/deployment.md` in the
  same change. Never commit secrets to it.

## Where to look

- Build & run: `README.md`.
- Detailed architecture and design decisions: `docs/ARCHITECTURE.md`.
- Module-specific stances: nested `CLAUDE.md` in `domain/`, `data/`, `ui/`.
- Path-scoped rules Claude must follow: `.claude/rules/`.
- Deployment state & how the app ships: `docs/agent-references/deployment.md`
  (step-by-step CLI procedures stay in GitHub issues labelled `deployment`).
- Why the publishing stack is shaped this way (approach comparison, 2026
  best-practice rationale): `docs/agent-references/play-publishing-research.md`.
