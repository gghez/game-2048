# ui — Compose presentation

Stance for this package:

- MVVM: `GameViewModel` owns state and exposes a single `StateFlow<GameUiState>`;
  composables are stateless and render that snapshot.
- The board is drawn with tiles in absolute position, keyed by their stable id, so
  a tile's offset animates when it changes cell — this is what makes tiles slide.
  A flow layout (Row/Column) could not animate movement this way.
- Appear and merge use a scale pop; the "fast animations" setting shortens every
  duration.
- Digit size is proportional to the tile (system font, no bundled typeface) so the
  numbers fill the tile like the design and stay consistent across sizes.
- Theme (light/dark) follows the in-app setting, not the system.
- All visible text comes from `res/values/strings.xml` (English default), mirrored
  per locale in `res/values-<lang>/`. Users can override the language in Settings
  (persisted; applied in `MainActivity.attachBaseContext`).
