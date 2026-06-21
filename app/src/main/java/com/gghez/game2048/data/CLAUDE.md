# data — persistence & services

Stance for this package:

- DataStore (Preferences) holds settings, best score, and the in-progress game so
  a session resumes after the app is killed.
- The saved game is serialized as a compact delimited string on purpose, to avoid
  pulling in a JSON dependency for one small object.
- Leaderboards sit behind a single interface with two implementations: a real
  Google Play Games one and a Noop fallback. The provider picks the real one only
  when game ids are configured, so the app always builds and runs without a Play
  Console account.
- Play Games v2 needs an Activity; the host binds it via the repository's attach
  hook, held weakly to avoid leaks. No Android-context state leaks into the domain.
