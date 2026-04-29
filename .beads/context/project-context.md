# Bilbo v2.0.0 Project Context

> Maintained by the ralph-loop. Persists project-level facts across iterations.

## Repository

- **Slug:** Prekzursil/bilbo-app
- **Default branch:** main
- **Current version on main:** 1.0.1 (tag `v1.0.1`, commit `ce57ea5`)
- **Target version:** 2.0.0 (tag `v2.0.0`, to be created in WU-C7)

## Cross-repo dependencies

- **Prekzursil/quality-zero-platform** — provides reusable workflows. WU-B1 lives there. The ralph-loop needs `WRITE` or `ADMIN` permission on that repo to author the bilbo-app stack profile and `inventory/repos.yml` entry. Verify with `gh repo view Prekzursil/quality-zero-platform --json viewerPermission -q .viewerPermission`.

## Required GitHub Actions secrets (must be configured before tag-time)

- `SONAR_TOKEN`
- `CODACY_API_TOKEN`
- `DEEPSCAN_API_TOKEN`
- `DEEPSOURCE_DSN`
- `SENTRY_AUTH_TOKEN`
- `SENTRY_ORG` (variable, not secret)
- `SENTRY_PROJECT` (variable, not secret)
- `CODECOV_TOKEN`
- `ANTHROPIC_API_KEY` (Edge Function env)
- `SUPABASE_ACCESS_TOKEN` (Supabase CLI deploy)
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_PROJECT_REF`
- `BILBO_RELEASE_KEYSTORE_BASE64`
- `BILBO_RELEASE_KEYSTORE_PASSWORD`
- `BILBO_RELEASE_KEY_ALIAS`
- `BILBO_RELEASE_KEY_PASSWORD`
- `IOS_TEAM_ID`
- `IOS_DISTRIBUTION_CERT_BASE64`
- `IOS_PROVISIONING_PROFILE_BASE64`
- `MATCH_PASSWORD`
- `MATCH_GIT_URL` (private cert repo)
- `FCM_SERVER_KEY` (Edge Function env)
- `APNS_KEY_ID`, `APNS_TEAM_ID`, `APNS_PRIVATE_KEY_P8` (Edge Function env)

If any of these is missing when the loop reaches a WU that needs it, the loop logs `BLOCKED: missing secret <NAME>` to `execution-state.md` and ends the iteration. The user supplies the secret and re-launches.

## Stack profile (in quality-zero-platform)

The new stack `kotlin-multiplatform` extends `quality-zero-phase1-common` with:
- Coverage runner: `macos-14` (needed for Xcode test phase).
- Setup: Java 17, Ruby 3.3, Node 22, Deno v2.x, Xcode 16.1.
- Coverage command: gradle `:shared:koverXmlReport` + `:androidApp:koverXmlReport` + `xcodebuild test` + `xccov_to_lcov.mjs` + `deno test --coverage` + `deno coverage --lcov`.
- Inputs: 4 reports (shared XML, android XML, ios lcov, deno lcov).

## Build invariants

- `minSdk = 26` (Android), `IPHONEOS_DEPLOYMENT_TARGET = 16.0` (iOS).
- Gradle 9.4.1, Kotlin 2.3.20.
- Kover exclusion list capped at ≤5 truly platform-bound classes (target after WU-B6).
- No `continue-on-error: true` anywhere in `.github/workflows/` (target after WU-B3).

## Known follow-ups (out of scope for v2.0.0)

- Wear OS / watchOS — deferred to v2.1.
- Web admin dashboard — deferred to v2.1.
- F-Droid Inclusion Request — manual paperwork, deferred.
- Google Play / App Store submission — manual after v2.0.0 ships.
