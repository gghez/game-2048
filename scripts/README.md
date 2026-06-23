# Deployment scripts

Reproducible, credentials-free steps to set up and publish 2exp11 on Google Play.
Sensitive values (GCP project id, service-account email, Play developer id,
keystore passwords) are **never** hard-coded here — scripts read them from
`.store-passwd` at the repo root (git-ignored) or from the environment.

**Reusable for another Android game:** these scripts are parameterised (package
name, project, paths via env/`.store-passwd`), so they port to a new app by changing
those values. The gotchas below (owner token, `changesNotSentForReview`, draft-app
production rule, KVM access for the emulator) are the time-savers — read them first.

> **Committing edits:** use an **owner** `androidpublisher` token (the SA's `:commit`
> returns 403) and **do not** set `changesNotSentForReview` (this app auto-sends for
> review; the param returns 400). `upload-listing.sh` does this correctly. Gradle
> Play Publisher sets the param by default, so `release.sh listing` currently fails —
> prefer `upload-listing.sh` for the store listing.

Run from the repo root. Order for a fresh setup:

| # | Script | What it does | Manual? |
|---|--------|--------------|---------|
| 1 | `create-publisher-sa.sh` | GCP project + `play-publisher` service account + JSON key + enable Android Publisher API (`gcloud`) | no |
| 2 | `gen-upload-keystore.sh` | Generate upload keystore + random passwords → `.store-passwd` + `local.properties` (`keytool`) | no |
| 3 | `gen-store-assets.sh` | Launcher icon (all mipmap buckets) + store graphics (`ImageMagick`) | no |
| 4 | `enable-github-pages.sh` | Host `docs/privacy.md` on GitHub Pages (`gh`) | no |
| 5a | `grant-app-publisher-sa.sh` | **Preferred:** grant the SA access to ONE app only (least privilege). Requires the app to exist in the Console | needs an owner token (see below) |
| 5b | `invite-publisher-sa.sh` | Grant the SA **account-wide** publishing access (all apps). Use only if per-app is impractical | needs an owner token (see below) |
| 6a | `gen-screenshots.sh` | Capture phone screenshots on a headless emulator (`adb`) | no |
| 6b | `upload-listing.sh` | Upload listing texts + all graphics via the API and commit (owner token) | needs owner token |
| 7a | `publish-internal.sh` | **Works:** upload AAB + release on a testing track (default internal) via API, owner token | needs owner token |
| 7b | `release.sh [build\|listing\|publish\|promote]` | Build signed AAB; `listing`/`publish` use GPP and currently fail (see gotcha) — prefer `upload-listing.sh` / `publish-internal.sh` | no |
| 8 | `set-data-safety.sh <csv>` | Submit Data safety via API. Body is the Console questionnaire CSV (export template first) | needs owner token |

> Prefer **5a** (per-app). Account-level permissions from **5b** apply to *every*
> app on the developer account.

## Store listing (versioned, automatable)

Listing text and graphics live under `app/src/main/play/` (Gradle Play Publisher
"fastlane" layout): `listings/fr-FR/{title,short-description,full-description}.txt`
and `listings/fr-FR/graphics/{icon,feature-graphic,phone-screenshots}/`. Edit those
files, then `scripts/release.sh listing` pushes them to the Store. **Add phone
screenshots** (≥2) under `graphics/phone-screenshots/` before submission — none were
captured during setup (no device).

## Irreducibly manual (web console — no API)

- Create the Play Console account and the app entry (`2exp11`, fr-FR, Game, free).
- **Content rating (IARC)**, **Ads**, **App access**, **Target audience** — App
  content declarations with no public API.

(Data safety *does* have an API — see `set-data-safety.sh`, payload format to confirm.)

## The owner token (steps 5, and 7 if the SA lacks app-content rights)

Play Console permissions are not GCP IAM, so `gcloud` cannot set them and the
service account cannot grant itself its first access. Step 5 must be authenticated
as a Play Console **owner/admin** with the `androidpublisher` scope. (Steps 3/4 are
local; `release.sh` uses the SA key. `set-data-safety.sh` may also need the owner
token if the SA isn't granted app-content management.) gcloud's
built-in OAuth client does not whitelist that scope for `print-access-token`, and
only `application-default login` accepts `--scopes`, so re-login the ADC once with
the scope included:

```bash
gcloud auth application-default login \
  --scopes=openid,https://www.googleapis.com/auth/cloud-platform,https://www.googleapis.com/auth/androidpublisher
```

Then `scripts/invite-publisher-sa.sh` (which calls
`gcloud auth application-default print-access-token`) works. You also need
`DEVELOPER_ID` (the number in the Play Console URL
`.../developers/<DEVELOPER_ID>/...`) recorded in `.store-passwd`.
