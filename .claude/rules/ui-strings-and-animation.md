---
paths:
  - "app/src/main/java/com/gghez/game2048/ui/**"
  - "app/src/main/res/values/**"
---

# UI: strings & animation

- No hard-coded user-facing text in composables. Every visible string goes in
  `res/values/strings.xml` (English default) and is mirrored in each
  `res/values-<lang>/strings.xml`, with correct accents/scripts. The in-app
  language override is applied in `MainActivity.attachBaseContext`.
- Composables stay stateless: read from `GameUiState`, send events to the
  ViewModel. Do not put game logic in the UI.
- Keep tiles keyed by their stable id and positioned absolutely so movement
  animates. Do not switch the board to a flow layout that would break sliding.
- Respect the "fast animations" setting: scale every animation duration by it.
- Theme follows the in-app setting, not the system theme.
