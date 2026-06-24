# Automating Google Play publishing for an open-source Android game

**Research question.** What is the cleanest, most reproducible way to fully
automate publishing an Android game (Kotlin + Jetpack Compose, Gradle Kotlin DSL)
to the Google Play Store, for an open-source project with no monetization and no
tracking? Reasoned comparison of automation approaches plus a concrete
recommendation.

*Synthesized from a deep-research run (103 of 104 sub-agents succeeded: search,
fetch, and adversarial verification). Verify-phase verdicts are folded in below;
5 claims were refuted and are flagged inline. Dates are current as of mid-2026.*

---

## 1. TL;DR / recommendation

- **Recommended stack: GitHub Actions, tag-driven, with a thin set of
  single-purpose steps** — sign the AAB with `r0adkll/sign-android-release`,
  upload with `r0adkll/upload-google-play@v1` (latest `v1.1.5`, Apr 2026, still
  actively maintained) [1][2][8]. This matches the repo's existing
  tag-driven Play release flow and keeps the toolchain minimal.
- **Authenticate to the Play Developer API with Workload Identity Federation
  (OIDC), not a long-lived JSON key.** GitHub mints a short-lived OIDC token that
  `google-github-actions/auth` exchanges for a ~1-hour GCP token — no service
  account key to store or rotate [3][4][5]. The r0adkll upload action's
  `serviceAccountJson` input supports a workload-identity provider file [2].
- **The app-signing keystore has no OIDC equivalent** — it must be supplied as CI
  secret material (base64-encoded keystore + passwords in GitHub Secrets, decoded
  at runtime) [1][8]. Keep this scope as small as possible.
- **Store metadata, screenshots and changelogs as-code.** Both Fastlane `supply`
  and Gradle Play Publisher (GPP) read listing text/images/release-notes from
  files committed in the repo [6][7]. Either works; pick `supply`'s
  `metadata/` layout or GPP's `src/.../play/` layout.
- **The Data safety form IS API-automatable** via the official
  `androidpublisher.v3 applications.dataSafety` endpoint, which ingests Google's
  Data safety CSV [10]. The repo already does this
  (`set-data-safety.sh`). A community Fastlane plugin
  (`fastlane-plugin-google_data_safety`) also wraps it [11]. *(This corrects a
  widely repeated but refuted claim that the form is "not automatable" — see §4.)*
- **Unavoidable manual step (once per app):** the very first AAB must be uploaded
  by hand in the Play Console before any API/CI upload works — app registration
  is not exposed by the Play Developer API [2][6][7].

**Why not GPP as the primary tool?** GPP is excellent functionally but its own
README declares **maintenance mode** ("Issues are ignored, but pull requests are
not") [7]. For a long-lived OSS publishing stack, that maintenance signal
argues against making it the load-bearing dependency, even though 4.0.0 shipped
recently (Jan 2026). Use it only if you want one-Gradle-task publishing and accept
that tradeoff.

---

## 2. Comparison table

| Criterion | GitHub Actions (r0adkll) | Fastlane (`supply`) | Gradle Play Publisher (GPP) | Gradle-native (API by hand) |
|---|---|---|---|---|
| **Maturity / maintenance 2025-2026** | Actively maintained; upload `v1.1.5` Apr 2026, sign action current [1][2] | Mature, widely used; `upload_to_play_store` well documented [6] | Functionally rich but **maintenance mode** per own README; 4.0.0 Jan 2026 [7] (refuted: "actively maintained") | N/A — DIY against `androidpublisher` v3 |
| **AAB build + signing** | Sign step (`sign-android-release`) signs `.aab`/`.apk` from keystore secrets [1][8] | Build via Gradle; supply uploads | GPP builds + uploads in one Gradle task [7] | Gradle build + manual signing config |
| **Track upload (internal/closed/open/prod)** | `track` input; all 4 tracks [2] | All 4 tracks; staged rollout via `--rollout` [6] | All tracks incl. custom; `track.set("internal")` [7] | Direct edits/commit API calls |
| **Metadata as-code** | Release notes via `whatsNewDirectory`; listing text/screenshots not handled by this action [2] | Yes — `metadata/` + locale dirs, `changelogs/<versionCode>` [6] | Yes — `publishListing`, `src/.../play/` files [7] | Possible but you build it |
| **Data safety automation** | Not in the action; do it separately via API/CSV [10] | Not in core; community plugin `google_data_safety` [11] | Not in GPP; (refuted "out of scope" framing) — use API directly [10] | Yes — `applications.dataSafety` CSV [10] |
| **Secrets model** | OIDC for Play API + keystore as base64 secret [2][3] | JSON key **or** WIF; `.p12` deprecated [6] | JSON key, or `ANDROID_PUBLISHER_CREDENTIALS` env, or ADC / SA impersonation [7] | Whatever you wire (OIDC recommended) |
| **OSS fit** | High — secrets in GitHub, OIDC keyless, minimal deps | High — but Ruby toolchain to maintain in CI | Medium — couples publishing to a maintenance-mode plugin | Low — most code to own |

---

## 3. Per-approach detail

### GitHub Actions (`r0adkll/sign-android-release` + `r0adkll/upload-google-play`)
- **Signing:** `sign-android-release` decodes `signingKeyBase64`, signs both
  `.apk` and `.aab`, and outputs the signed file path so it can chain straight
  into the upload step. Inputs (`alias`, `keyStorePassword`, `keyPassword`) come
  from GitHub Secrets [1][8].
- **Upload:** `upload-google-play@v1` takes `packageName`, `releaseFile`,
  `track`, and release notes from a `whatsNewDirectory` (`whatsnew-<LOCALE>`
  files). It supports two SA auth modes: `serviceAccountJsonPlainText` (secret)
  and `serviceAccountJson` (a file path that can be a **workload-identity
  provider** file) — i.e. OIDC-compatible [2].
- **Maintenance:** upload action `v1.1.5` (Apr 21, 2026), 26 releases [2]; the
  sign action is current in 2026 [1].
- **Weakness:** the upload action does **not** push store-listing text/screenshots
  or the Data safety form; those are separate steps [2].

### Fastlane (`supply` / `upload_to_play_store`)
- One command uploads an AAB to `production/beta/alpha/internal`, with staged
  rollout via `--rollout` [6].
- **Metadata-as-code** is a core strength: changelogs/descriptions/images live in
  a local `metadata/` tree with locale subdirs; changelog files are named by
  version code with a `default.txt` fallback; uploaded images *replace* existing
  store images [6].
- **Auth:** `json_key_file` (`.p12` deprecated in favor of JSON) **and** Workload
  Identity Federation are supported; `validate_play_store_json_key` tests the
  connection [6].
- **Data safety:** not in core supply, but the community plugin
  `fastlane-plugin-google_data_safety` (action `upload_google_data_safety`)
  uploads the form from a CSV via the SA JSON [11]. (Until that plugin, there was
  an open, unanswered feature request for it [9].)
- **Weakness:** adds a Ruby/Bundler toolchain to maintain in CI; requires the
  mandatory first manual upload before it can connect [6].

### Gradle Play Publisher (GPP, Triple-T/gradle-play-publisher)
- **Most integrated:** a single Gradle task builds, uploads, and promotes
  AABs/APKs across tracks and publishes listing metadata; `./gradlew
  publishListing` handles title/description/screenshots; release notes live at
  `src/[sourceSet]/play/release-notes/[lang]/[track].txt` [7].
- **Auth:** SA JSON via `serviceAccountCredentials` or the
  `ANDROID_PUBLISHER_CREDENTIALS` env var; 3.12.0 (Nov 2024) added Application
  Default Credentials + Service Account impersonation [7].
- **Version pacing:** kept up with the Play API (Subscriptions in 3.10.0, version
  promotion in 3.9.0) [7].
- **Two verified cautions (refuted claims corrected):**
  1. *"Actively maintained, 4.0.0 on Jan 25 2025"* — **refuted.** 4.0.0 shipped
     **Jan 25, 2026** (GitHub API `published_at`), and the README itself says the
     project is in **maintenance mode**: "Issues are ignored, but pull requests
     are not." 23 open issues, 0 open PRs [7]. So it is *recent* but not
     *actively maintained* in the usual sense.
  2. GPP 4.0.0 requires Android Gradle Plugin 9 (a Jan 2026 release) and drops
     older AGP/Gradle; teams on older toolchains stay on the 3.x line [7].
- **Data safety:** GPP does not fill the form, and a 2022 issue (#1064) was closed
  with a pointer to Google's docs [12]. The inference "therefore the
  form is not automatable" was **refuted** — the API automates it (§4).

### Gradle-native (direct `androidpublisher` v3)
- Maximum control, no third-party publishing dependency, but you own all the code
  (edits → upload → commit, listing, dataSafety). Only worth it if you must avoid
  every external plugin. The same pitfalls (first manual upload, keystore secret)
  still apply.

---

## 4. Pitfalls / gotchas

- **Mandatory manual first release.** Across *all* tools, the first APK/AAB must
  be uploaded by hand in the Play Console; app registration is not exposed by the
  Play Developer API. Fastlane and GPP both document this explicitly
  [2][6][7].
- **App signing by Google Play vs. CI keystore.** OIDC removes the *API* key but
  **not** the signing keystore — there is no OIDC alternative for the keystore, so
  it must be base64-encoded secret material in CI [1][8].
- **Data safety form — IS automatable (corrects a refuted claim).** The official
  endpoint `POST androidpublisher/v3/applications/{packageName}/dataSafety`
  accepts a `safetyLabels` field = the Data safety **CSV** (same export/import
  format as the Console); docs last updated 2025-05-21 [10]. The repeated claim
  that GPP/"the API" does not automate it was **refuted with high confidence** —
  GPP-the-plugin doesn't, but the underlying API does. The repo already wires this
  (`set-data-safety.sh`). The Fastlane plugin generates the CSV interactively but
  cannot generate it non-interactively [10][11].
- **API quotas.** Default **3000 queries/minute per quota bucket**; all
  publishing/track operations share the single "Publishing, Monetization, and
  Reply to Reviews APIs" bucket; buckets are independent and quota can be raised
  on request [13]. A single game's release cadence will never approach this.
- **OIDC vs JSON key.** Static SA JSON keys are long-lived, never expire by
  default, and grant full impersonation to any holder — a real risk
  [3][5]. WIF/OIDC is Google's recommended replacement: GitHub mints a
  ~5-min OIDC token, exchanged for a ~1-hour GCP token; nothing long-lived to
  store or rotate [3][4][5]. **OSS caveat:** because GitHub uses one issuer
  URL across all tenants, you **must** set an attribute condition (e.g. restrict
  to your org/repo) on the workload identity provider, or any GitHub repo could
  request tokens [4]. The workflow also needs `id-token: write` permission [3].

---

## 5. Recommended reproducible stack (template for future games)

A tag-driven GitHub Actions workflow (triggered on a `v*` tag), single OS image:

1. **Versioning** — derive `versionCode` in CI from a UTC build number
   (`date +%s` offset by a base epoch) or persist an auto-incremented repo
   variable; `versionName` from the release tag / `release/x.y.z` branch [8].
2. **Auth (keyless)** — `google-github-actions/auth` with Workload Identity
   Federation: a pool + provider with an **attribute condition** restricting to
   this repo/org, bound to a per-app service account via
   `roles/iam.workloadIdentityUser`; grant that SA only the Play publishing role
   (least privilege) [3][4][5]. Workflow declares `id-token: write`.
3. **Build + sign** — Gradle assembles the release AAB; `r0adkll/sign-android-release`
   signs it from `SIGNING_KEY` (base64 keystore), `ALIAS`, `KEY_STORE_PASSWORD`,
   `KEY_PASSWORD` GitHub Secrets [1][8].
4. **Upload** — `r0adkll/upload-google-play@v1` (pin `v1.1.5`) with
   `packageName`, the signed `releaseFile`, `track: internal` (promote later),
   `whatsNewDirectory` for localized release notes, and `serviceAccountJson`
   pointing at the WIF credential file [2].
5. **Store metadata as-code** — keep listing text/screenshots/changelogs in the
   repo. Either a Fastlane `supply` `metadata/` tree, or GPP's `src/.../play/`
   layout if you adopt GPP for listings only [6][7].
6. **Data safety as-code** — commit the Data safety **CSV** and push it via
   `applications.dataSafety` (the repo's `set-data-safety.sh` pattern) or the
   `google_data_safety` Fastlane plugin [10][11].
7. **One-time bootstrap (manual):** upload the first AAB and complete initial
   Console setup by hand; everything after is automated [2][6][7].

**Exact coordinates / versions where supported by the data:**
`r0adkll/upload-google-play@v1` (`v1.1.5`, Apr 2026) [2];
`r0adkll/sign-android-release` (current 2026) [1];
`com.github.triplet.gradle:play-publisher` (GPP, latest 4.0.0 Jan 25 2026 — AGP 9;
3.13.0 Dec 2025 / 3.12.x for older toolchains) [7];
`fastlane-plugin-google_data_safety` (owenbean400) [11];
`google-github-actions/auth` for WIF [4]; Play API `androidpublisher` v3
`applications.dataSafety` [10].

**OSS fit summary.** GitHub Actions + OIDC keeps the only stored secret as the
signing keystore (unavoidable), avoids any tracking/monetization dependency
(consistent with the project's no-ads/no-tracking charter), and reuses cleanly
across future games by templating the workflow and swapping `packageName` +
keystore + WIF binding.

---

## 6. Sources

All URLs below were present in the research transcripts; none are invented.

1. `r0adkll/sign-android-release` (marketplace / repo) — https://github.com/marketplace/actions/sign-android-release ; https://github.com/r0adkll/sign-android-release
2. `r0adkll/upload-google-play` — https://github.com/r0adkll/upload-google-play ; issue on auth modes: https://github.com/r0adkll/upload-google-play/issues/70
3. Google Cloud blog, "Enabling keyless authentication from GitHub Actions" — https://cloud.google.com/blog/products/identity-security/enabling-keyless-authentication-from-github-actions
4. `google-github-actions/auth` (WIF action) — https://github.com/google-github-actions/auth ; GitHub docs, configuring OIDC in GCP — https://docs.github.com/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-google-cloud-platform
5. GCP IAM, best practices for Workload Identity Federation / service-account impersonation — https://docs.cloud.google.com/iam/docs/best-practices-for-using-workload-identity-federation ; https://docs.cloud.google.com/iam/docs/service-account-impersonation
6. Fastlane `supply` / `upload_to_play_store` docs — https://docs.fastlane.tools/actions/supply/ ; https://docs.fastlane.tools/actions/upload_to_play_store/
7. Gradle Play Publisher (Triple-T) — https://github.com/Triple-T/gradle-play-publisher ; releases — https://github.com/Triple-T/gradle-play-publisher/releases ; release `4.0.0` (API) — https://api.github.com/repos/Triple-T/gradle-play-publisher/releases/tags/4.0.0 ; Maven — https://mvnrepository.com/artifact/com.github.triplet.gradle/play-publisher
8. CI walkthroughs (keystore-as-secret, GA + GPP) — https://www.valueof.io/blog/deploying-to-google-play-using-github-actions ; https://medium.com/nerd-for-tech/ci-cd-for-android-using-github-actions-and-gradle-play-publisher-448bd8e42774
9. Fastlane Data safety feature request (no built-in support) — https://github.com/fastlane/fastlane/issues/20521 ; https://github.com/fastlane/fastlane/discussions/19851
10. Play Developer API — `applications.dataSafety` (Data safety automatable via CSV) — https://developers.google.com/android-publisher/api-ref/rest/v3/applications/dataSafety ; API overview — https://developers.google.com/android-publisher
11. Fastlane plugin `fastlane-plugin-google_data_safety` — https://github.com/owenbean400/fastlane-plugin-google_data_safety
12. GPP Data safety issue #1064 (closed with doc pointer) — https://github.com/Triple-T/gradle-play-publisher/issues/1064 ; Google "Data safety" help — https://support.google.com/googleplay/android-developer/answer/10787469
13. Play Developer API quotas (3000 QPM per bucket) — https://developers.google.com/android-publisher/quotas

---

### Verification notes (refuted / weak claims)

- **Refuted (high confidence):** GPP 4.0.0 was dated "Jan 25 2025" → actually
  **2026** (GitHub API timestamp) [7].
- **Refuted (high confidence):** GPP "actively maintained" → README says
  **maintenance mode**, issues ignored [7].
- **Refuted (high confidence, ×2):** "Data safety form is out of scope / not
  automatable by the API" → the official `applications.dataSafety` endpoint
  automates it via CSV [10]. GPP-the-plugin not supporting it does not imply the
  API can't.
- **Weakly supported:** GPP "can reuse the most recently uploaded
  metadata/release notes when local files are absent" — documented ambiguously per
  the source; treat as uncertain [7].
- **Sources that disagreed:** community tutorials describe per-method quota
  buckets, but the authoritative Play quotas page states only broad buckets
  ("All Publishing APIs") at 3000 QPM, not per-method limits — prefer the official
  page [13].
