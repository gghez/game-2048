# domain — game rules (pure Kotlin)

Stance for this package:

- Zero Android imports. Everything here is plain Kotlin so it runs under JVM unit
  tests without an emulator.
- Tiles carry a stable id. Moves preserve that id when a tile slides; a merge
  reuses the surviving tile's id. The UI relies on this to animate movement.
- Randomness is injected via a spawner interface, so a seed makes the engine fully
  deterministic and testable.
- `move` is spawn-free and pure; spawning a new tile and detecting loss happen in a
  separate step. This keeps each rule independently testable.
- This package is the source of truth for game behaviour; the UI must not
  re-implement any rule.
