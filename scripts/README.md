# Deployment scripts

Reproducible, credentials-free steps to set up and publish 2exp11 on Google Play.
Sensitive values (GCP project id, service-account email, Play developer id, WIF
provider, keystore passwords) are **never** hard-coded here — scripts read them from
`.store-passwd` at the repo root (git-ignored) or from the environment.

**Reusable for another Android game:** every script is parameterised (package name,
project, paths via env/`.store-passwd`), so it ports to a new app by changing those
values — see [Clone this setup for a new game](#clone-this-setup-for-a-new-game) for
the minimal ordered procedure. The gotchas below (owner token,
`changesNotSentForReview`, draft-app production rule, KVM access for the emulator)
are the time-savers — read them first.

> **Tokens — owner vs CI/WIF (one source of truth: `lib/play-api.sh`).** A `:commit`
> (listing / data safety / release) needs an **owner** `androidpublisher` token — the
> service account cannot commit its first submission. Scripts take it from
> `ACCESS_TOKEN` if set, else the local owner ADC token. **Do not** set
> `changesNotSentForReview` (this app auto-sends for review; the param returns 400).
> CI carries no token of its own: Workload Identity Federation mints a short-lived
> one inside `release.yml` and passes it as `ACCESS_TOKEN`.

Run from the repo root. Order for a fresh setup:

| # | Script | What it does | Manual? |
|---|--------|--------------|---------|
| 1 | `create-publisher-sa.sh` | GCP project + `play-publisher` service account + enable Android Publisher API (`gcloud`). **Keyless by default** — no JSON key (`--with-key` for the legacy flow) | no |
| 2 | `setup-wif.sh` | Workload Identity Federation pool/provider/binding so CI authenticates **keyless** (`gcloud`); prints `WIF_PROVIDER` / `WIF_SERVICE_ACCOUNT` | no |
| 3 | `gen-upload-keystore.sh` | Generate upload keystore + random passwords → `.store-passwd` + `local.properties` (`keytool`) | no |
| 4 | `gen-store-assets.sh` | Launcher icon (all mipmap buckets) + store graphics (`ImageMagick`) | no |
| 5 | `enable-github-pages.sh` | Host `docs/privacy.md` on GitHub Pages (`gh`) | no |
| 6 | `grant-publisher-sa.sh [--account-wide]` | Grant the SA Play Console access — **per-app by default** (least privilege); `--account-wide` for all apps. Requires the app to exist in the Console | needs an owner token (see below) |
| 7a | `gen-screenshots.sh` | Capture phone screenshots on a headless emulator (`adb`) | no |
| 7b | `upload-listing.sh` | Upload listing texts + all graphics via the API and commit (owner token) | needs owner token |
| 8 | `publish-internal.sh` | Upload AAB + release on a testing track (default internal) via API | needs owner/CI token |
| 9 | `set-data-safety.sh <csv>` | Submit Data safety via API. Body is the Console questionnaire CSV (export template first) | needs owner token |
| 10 | `create-leaderboards.sh` | Create the 3 leaderboards via the Games Configuration API (idempotent) | needs owner ADC |
| 11 | `set-github-secrets.sh` | Push the GitHub Actions secrets the release workflow needs (`gh`) | no |

`release.sh [build\|publish\|listing]` is a convenience wrapper over the canonical
paths: `build` runs the Gradle bundle, `publish`/`listing` delegate to
`publish-internal.sh` / `upload-listing.sh`. There is **no Gradle Play Publisher** —
it was removed in #15 (its `listing`/`publish` tasks failed and it is unmaintained).

> Prefer per-app (default) for **6**. `--account-wide` permissions apply to *every*
> app on the developer account.

## Leaderboards & CI (Play Games Services + tag → Play)

- `create-leaderboards.sh` creates the three boards (best score, best tile, time-to-2048)
  through `gamesconfiguration.googleapis.com`. The API needs an **owner** on the games
  project (the publisher SA has no Games Services access), so authenticate ADC first
  (`gcloud auth application-default login`); the call also needs `GCP_PROJECT` as the
  quota project. The board ids it prints go in `.store-passwd` (git-ignored).
- `set-github-secrets.sh` pushes the secrets the release workflow needs
  (`UPLOAD_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`/`_KEY_ALIAS`/`_KEY_PASSWORD`,
  `WIF_PROVIDER`, `WIF_SERVICE_ACCOUNT`, `PLAY_GAMES_APP_ID`,
  `LEADERBOARD_SPEED`/`_EFFICIENCY`/`_TIME`), reading values from `.store-passwd` +
  the keystore. **Keyless** — there is no service-account JSON secret. Never prints a value.
- Pushing a `vX.Y.Z` tag then runs `.github/workflows/release.yml` → signed AAB →
  internal track (see `docs/agent-references/deployment.md`).

## Store listing (versioned, automatable)

Listing text and graphics live under `app/src/main/play/`:
`listings/<lang>/{title,short-description,full-description}.txt` and
`listings/fr-FR/graphics/{icon,feature-graphic,phone-screenshots}/`. Edit those
files, then `scripts/upload-listing.sh` (or `scripts/release.sh listing`) pushes them
to the Store. **Add phone screenshots** (≥2) under `graphics/phone-screenshots/`
before submission — none were captured during setup (no device). The `play/` tree was
Gradle Play Publisher's layout; the REST `upload-listing.sh` reads the same files, so
it stays after GPP's removal.

## Irreducibly manual (web console — no API)

- Create the Play Console account and the app entry (`2exp11`, fr-FR, Game, free).
- **First AAB upload** can go through a testing track via `publish-internal.sh`, but
  **content rating (IARC)**, **Ads**, **App access**, **Target audience** — App
  content declarations with no public API.
- **Play Games Services sign-in setup** (leaderboard *creation* is scripted; the rest
  is not): the **OAuth consent screen**, the **Android credential** bound to the
  package + signing SHA-1, and **publishing the Games Services project**. There is no
  usable API — the IAP OAuth Admin API is deprecated (shut down 2026-03-19) and never
  worked for personal Google accounts. Consent-screen scopes: `games`, `games_lite`,
  `drive.appdata`. Use the **Play App-signing SHA-1** (Test and release → App integrity)
  for the credential, since Play re-signs installed builds.

(Data safety *does* have an API — see `set-data-safety.sh`.)

## The owner token (step 6, and 9 if the SA lacks app-content rights)

Play Console permissions are not GCP IAM, so `gcloud` cannot set them and the
service account cannot grant itself its first access. Step 6 must be authenticated
as a Play Console **owner/admin** with the `androidpublisher` scope. gcloud's
built-in OAuth client does not whitelist that scope for `print-access-token`, and
only `application-default login` accepts `--scopes`, so re-login the ADC once with
the scope included:

```bash
gcloud auth application-default login \
  --scopes=openid,https://www.googleapis.com/auth/cloud-platform,https://www.googleapis.com/auth/androidpublisher
```

Then any API script (`grant-publisher-sa.sh`, `upload-listing.sh`,
`set-data-safety.sh`, …) picks the token up via `lib/play-api.sh` — that file is the
single place the owner-vs-CI token rule is documented. You also need `DEVELOPER_ID`
(the number in the Play Console URL `.../developers/<DEVELOPER_ID>/...`) recorded in
`.store-passwd`.

## Clone this setup for a new game

The whole publishing setup ports to another Android game by changing a handful of
values and running the scripts in order. Nothing here is `2exp11`-specific by
accident — everything reads from `.store-passwd` or the environment.

**(a) Values to set** (in `.store-passwd` at the repo root, git-ignored, plus a few
source files):

- `.store-passwd`: `GCP_PROJECT`, `SERVICE_ACCOUNT_EMAIL`, `DEVELOPER_ID`,
  `PACKAGE_NAME`, `GITHUB_REPO`, the keystore passwords (filled by
  `gen-upload-keystore.sh`), then `WIF_PROVIDER` (filled by `setup-wif.sh`) and the
  leaderboard ids (filled by `create-leaderboards.sh`).
- Source: `applicationId` / `namespace` in `app/build.gradle.kts`, the `app_name`
  string in `res/values/strings.xml`, and the privacy-policy URL in the listing.

**(b) Scripted steps**, in order (each is re-runnable and credential-free):

1. `create-publisher-sa.sh` — GCP project + SA + Android Publisher API (keyless).
2. `setup-wif.sh` — WIF pool/provider/binding; record the printed `WIF_PROVIDER`.
3. `gen-upload-keystore.sh` — upload keystore + passwords.
4. `gen-store-assets.sh` — launcher icon + store graphics (replace with real art).
5. `enable-github-pages.sh` — host the privacy policy.
6. *(manual: create the app in the Play Console — see (c))*
7. `grant-publisher-sa.sh` — per-app Play Console access (needs the app to exist + an owner token).
8. `upload-listing.sh` — push the store listing.
9. `create-leaderboards.sh` — only if the game uses Play Games leaderboards.
10. `set-github-secrets.sh` — push the release-workflow secrets.
11. Tag `vX.Y.Z` → `.github/workflows/release.yml` builds + releases to internal, keyless.

**(c) Irreducibly manual Console steps** (no public API):

- Create the Play Console account and the app entry.
- Content rating (IARC), Ads, App access, Target audience, app category.
- Play Games Services OAuth consent screen + Android credential + publish (if the
  game uses leaderboards).
- Data safety has an API (`set-data-safety.sh`) but the questionnaire CSV is exported
  from the Console first.
