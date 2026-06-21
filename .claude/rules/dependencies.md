---
paths:
  - "app/build.gradle.kts"
  - "gradle/libs.versions.toml"
---

# Dependencies

- Never add an advertising, analytics, tracking, attribution, or crash-reporting
  SDK. This is the project's core promise.
- Never add a paid or freemium dependency.
- Prefer the standard library and Jetpack. Justify any new dependency in the commit
  message and keep the footprint minimal.
- Keep Google Play Games optional — it must never become required to build or run.
