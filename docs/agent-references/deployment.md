# Deployment reference — how 2exp11 ships to Google Play

> Living record of the actual deployment setup. **Keep it current** whenever you
> change anything deployment-related (see the maintenance rule in the root
> `CLAUDE.md`). **Never put secrets here** — no keystore passwords, no service
> account key contents, no absolute machine paths.
>
> Step-by-step CLI procedures live in the GitHub issues labelled `deployment`
> (#1–#5). This file records what was actually done and what remains.

## App identity

- **Display name:** `2exp11` (reads as 2¹¹ = 2048). "2048" was already taken on the
  Store; the rename avoids a trademark/duplicate rejection. Defined once in
  `app_name` (`res/values/strings.xml`); the in-app header reads that resource.
- **Technical package id** (`applicationId` / `namespace`): `com.gghez.game2048`.
  Intentionally **unchanged** — it is invisible to users, already globally unique,
  and immutable once published. Do not rename it.
- Version: `versionCode` / `versionName` in `app/build.gradle.kts`.

## Wired in the repo (done)

- **Release signing** (issue #1): `signingConfigs.release` in `app/build.gradle.kts`,
  credentials read from `local.properties` (`RELEASE_STORE_FILE`,
  `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`). When those
  are absent the release build is simply unsigned, so CI/local builds still work.
  `*.jks` is git-ignored. **The upload keystore has been generated** (RSA 2048,
  alias `game2048`, validity ~27 years) and kept outside the repo under the owner's
  home dir; its credentials live in `.store-passwd` at the repo root (git-ignored)
  and are mirrored into `local.properties`. A signed AAB builds with
  `./gradlew bundleRelease` — verified (`jarsigner -verify` → `jar verified`,
  cert `CN=gghez`). Upload-key SHA-1 (for Play Games OAuth if needed) is recorded in
  the Play Console after enrollment; recompute with
  `keytool -list -v -keystore <jks> -alias game2048`.
- **Gradle Play Publisher 3.11.0** (issue #2): plugin declared in root + app
  `build.gradle.kts`; `play {}` block targets the `internal` track and defaults to
  App Bundles. It reads `play-service-account.json` at the repo root (git-ignored).
- **Launcher icon + store graphics** (issue #5): a `2¹¹` placeholder generated with
  ImageMagick. Density buckets in `app/src/main/res/mipmap-*/` (`ic_launcher.png` +
  `ic_launcher_round.png`); source art and store assets in `store-assets/`
  (`icon-source.png`, `play-icon-512.png`, `play-feature-1024x500.png`). Manifest
  references `@mipmap/ic_launcher` + `@mipmap/ic_launcher_round`. Replace with real
  art when available.
- **Privacy policy** (issue #4): `docs/privacy.md`, hosted on GitHub Pages.

## Cloud / external resources (created)

- **GitHub Pages:** enabled from `/docs` on `main`. Privacy policy is live at
  <https://gghez.github.io/game-2048/privacy>.
- **GCP project:** a dedicated project for Play publishing (id kept out of version
  control — see `.store-passwd`, git-ignored).
- **Service account:** a `play-publisher` account in that project, with the Android
  Publisher API enabled. Its email and the project id are recorded in `.store-passwd`
  (git-ignored); its JSON key was generated locally and must never be committed.
  **Play Console access: per-app only.** An initial account-wide grant was revoked
  (too broad — it covered every app). The SA now has a single **app-level grant**
  on `com.gghez.game2048` (manage production APKs, testing tracks, store presence)
  and **no account-level permissions**, so it cannot touch any other app. Applied
  via `scripts/grant-app-publisher-sa.sh` (state `ACCESS_GRANTED`).
- **Google Play Console:** account created and active (owned by the project owner's
  Google account).

## Remaining manual / web steps

These cannot be scripted from here (interactive secrets or web-only consoles):

1. **Back up the upload keystore** — the `.jks` and `.store-passwd` exist locally
   only. Copy them to a safe place; losing the upload key means requesting a reset
   from Google (possible thanks to Play App Signing).
2. **Questionnaires:** content rating (IARC) and Data safety — declare *no data
   collected, no tracking*.
3. **Paste the privacy policy URL** into the store listing.
4. **Screenshots:** capture from a device (`adb exec-out screencap -p > shot.png`).
   No device was connected during setup, so none were generated.
5. **Real icon (optional):** replace the placeholder with proper adaptive-icon art.
6. **Leaderboards (optional, issue #3):** create the Play Games Services config and
   two leaderboards on the web, put their ids + the numeric App ID in
   `local.properties`, then uncomment the `games.APP_ID` meta-data in
   `AndroidManifest.xml`. The app falls back to `NoopLeaderboard` until then.

## Reproducible scripts

The whole automatable procedure is captured, credentials-free, in `scripts/`
(see `scripts/README.md` for the ordered table and prerequisites):

- `create-publisher-sa.sh` — GCP project + service account + key + API (`gcloud`)
- `gen-upload-keystore.sh` — upload keystore + passwords → `.store-passwd` (`keytool`)
- `gen-store-assets.sh` — launcher icon + store graphics (`ImageMagick`)
- `enable-github-pages.sh` — host the privacy policy (`gh`)
- `grant-app-publisher-sa.sh` / `invite-publisher-sa.sh` — grant the SA per-app /
  account-wide Play Console access (Android Publisher API)
- `release.sh [build|listing|publish|promote]` — build / push store listing /
  upload AAB / promote (`gradlew` + Gradle Play Publisher)
- `set-data-safety.sh` — submit Data safety (API; payload format to confirm)

Store listing text + graphics are versioned under `app/src/main/play/`
(`listings/fr-FR/…`); edit and push with `release.sh listing`. Scripts read
sensitive values from `.store-passwd` (git-ignored) — never hard-coded.

**Still web-only (no API):** content rating (IARC), Ads, App access, Target
audience, app category — these App-content declarations must be filled in the
Console.

## Committing edits via the API — two gotchas

The store listing (texts) **and all graphics** (icon, feature, phone + 7"/10" tablet
screenshots) are pushed via the Publisher API with `scripts/upload-listing.sh` —
this works even for a never-published app. Earlier "first publish must be manual"
notes were wrong; the real blockers were:

- **Do not set `changesNotSentForReview`** on `:commit`. This app auto-sends changes
  for review; setting the param returns `400 "must not be set"`. Gradle Play
  Publisher sets it by default → that is why `release.sh listing` failed.
- **Use an owner `androidpublisher` token.** The service account could set listing
  content but its `:commit` returned `403`; committing the submission needs an owner
  token (the ADC login in scripts/README.md).

Committing **sends the changes for review** (no draft mode here). The AAB release
upload was not attempted via API yet — do it in the Console, or try
`release.sh publish` with an owner token.

## Release commands (CLI)

```bash
scripts/release.sh build      # build the signed AAB
./gradlew bootstrapListing    # first time: pull listing metadata into app/src/main/play/
scripts/release.sh publish    # build + upload the AAB to the internal track
scripts/release.sh promote    # promote internal -> production (no rebuild)
```
