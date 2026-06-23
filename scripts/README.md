# Deployment scripts

Reproducible, credentials-free steps to set up and publish 2exp11 on Google Play.
Sensitive values (GCP project id, service-account email, Play developer id,
keystore passwords) are **never** hard-coded here — scripts read them from
`.store-passwd` at the repo root (git-ignored) or from the environment.

Run from the repo root. Order for a fresh setup:

| # | Script | What it does | Manual? |
|---|--------|--------------|---------|
| 1 | `create-publisher-sa.sh` | GCP project + `play-publisher` service account + JSON key + enable Android Publisher API (`gcloud`) | no |
| 2 | `gen-upload-keystore.sh` | Generate upload keystore + random passwords → `.store-passwd` + `local.properties` (`keytool`) | no |
| 3 | `gen-store-assets.sh` | Launcher icon (all mipmap buckets) + store graphics (`ImageMagick`) | no |
| 4 | `enable-github-pages.sh` | Host `docs/privacy.md` on GitHub Pages (`gh`) | no |
| 5 | `invite-publisher-sa.sh` | Grant the SA Play Console access via Android Publisher API | needs an owner token (see below) |
| 6 | `release.sh [build\|publish\|promote]` | Build signed AAB / upload / promote (`gradlew`) | no |

## Irreducibly manual (web console, not scriptable)

- Create the Play Console account and the app entry (`2exp11`, fr-FR, Game, free).
- Content rating (IARC) and Data safety questionnaires (declare no data, no tracking).
- Paste the privacy-policy URL into the listing; upload screenshots.

## The owner token for step 5

Play Console permissions are not GCP IAM, so `gcloud` cannot set them and the
service account cannot grant itself its first access. Step 5 must be authenticated
as a Play Console **owner/admin** with the `androidpublisher` scope. gcloud's
built-in OAuth client does not whitelist that scope for `print-access-token`, so
re-login once with the scope included:

```bash
gcloud auth login \
  --scopes=https://www.googleapis.com/auth/cloud-platform,https://www.googleapis.com/auth/androidpublisher
```

Then `scripts/invite-publisher-sa.sh` (which calls `gcloud auth print-access-token`)
works. You also need `DEVELOPER_ID` (the number in the Play Console URL
`.../developers/<DEVELOPER_ID>/...`) recorded in `.store-passwd`.
