---
paths:
  - "app/src/main/java/com/gghez/game2048/domain/**"
---

# Domain purity

- No `android.*` / `androidx.*` imports in this package — it must stay pure Kotlin.
- Every behaviour change ships with a unit test (TDD); tests live in
  `app/src/test/.../domain`.
- Preserve tile identity across moves: a sliding tile keeps its id, a merge reuses
  the surviving tile's id. The UI animation depends on this.
- Keep randomness injected (the spawner interface) so tests stay deterministic.
- Do not add I/O, persistence, or framework calls here.
