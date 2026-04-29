# Bilbo v2.0.0 — "Quality-Zero Final" Design

**Date:** 2026-04-29  
**Author:** Prekzursil (assisted)  
**Status:** Approved for planning  
**Supersedes:** `docs/plans/2026-04-06-bilbo-implementation-plan.md` (functional scope) and the v1.0.x posture documented in `CHANGELOG.md`.

---

## 1. Goal

Take Bilbo from v1.0.1 — a functional skeleton with in-memory repository stubs, an unconfigured Supabase backend, and unsigned debug release artifacts — to **v2.0.0 Final**: a fully functional, signed, persistence-backed Kotlin Multiplatform digital-wellness app whose every PR and `main` commit is gated by the same strict-zero, 100%-coverage quality regime that governs `event-link` and `quality-zero-platform` itself.

This is a single release tagged `v2.0.0`. There are no v1.1 / v1.2 way-points; we collapse the v1.x roadmap into one big-bang release because every gap below is a precondition of the same outcome (a working app that survives strict-zero CI).

## 2. Success Criteria (hard blockers — all must be true to ship)

1. **Functional parity with the README's "What is" claims:**
   - All 6 repositories on Android persist via SQLDelight `AndroidSqliteDriver`. Data survives process restart and device reboot.
   - All 6 repositories on iOS persist via SQLDelight `NativeSqliteDriver`. Same guarantee.
   - `SUPABASE_URL` and `SUPABASE_ANON_KEY` resolve to a real Supabase project for `release` build types; `local.properties` / `.env.local` for `debug`. CI injects via repository secrets.
   - All 13 edge functions in `supabase/functions/` are reachable from the mobile clients with retry, error mapping, and telemetry.
   - Realtime subscriptions for buddies / circles / challenge progress are live.
   - Push notifications work end-to-end on Android (FCM) and iOS (APNs — replace stub at `supabase/functions/push-notification/index.ts:92` with real implementation via `node-apn` or web-push for Deno).
   - Tier-3 weekly narrative wired to `ai-weekly-insight` edge function and surfaced in `WeeklyInsightScreen` / iOS analog.
   - iOS auth check via `AuthManager.hasActiveSession()` exported through Kotlin/Native ObjC header.
   - Every "coming soon" placeholder in Settings, Insights, Analog Suggestions, and Data Export is replaced with the real screen.
   - iOS `DeviceActivityMonitorExtension`, `ShieldConfigurationExtension`, and `ShieldActionExtension` are real Xcode targets in `iosApp/project.yml` (currently excluded), wired to shared `BypassManager` / `CooldownManager`.

2. **Quality-Zero v2 parity with `event-link`:**
   - `Prekzursil/bilbo-app` is registered in `Prekzursil/quality-zero-platform/inventory/repos.yml` with `mode.phase: absolute`, `issue_policy.mode: zero`.
   - A new stack profile `quality-zero-platform/profiles/stacks/kotlin-multiplatform.yml` exists, inheriting `quality-zero-phase1-common.yml` and supplying language-specific coverage and verify commands.
   - A repo profile `quality-zero-platform/profiles/repos/bilbo-app.yml` enables all 12 scanners with `severity: block`.
   - `bilbo-app/.github/workflows/` contains:
     - `quality-zero-gate.yml` calling `reusable-quality-zero-gate.yml@<pinned-sha>`
     - `quality-zero-platform.yml` calling `reusable-scanner-matrix.yml@<pinned-sha>`
     - `quality-zero-backlog.yml` calling `reusable-backlog-sweep.yml@<pinned-sha>`
     - `quality-zero-remediation.yml` calling `reusable-remediation-loop.yml@<pinned-sha>`
     - `codeql.yml` calling `reusable-codeql.yml@<pinned-sha>` for languages `kotlin`, `swift`, `javascript-typescript`
     - Existing `shared-tests.yml`, `android-ci.yml`, `ios-ci.yml`, `backend-ci.yml` — **with every `continue-on-error: true` removed** and Kover / lint / detekt / SwiftLint / deno-test treated as blocking failures.
   - `bilbo-app/.coverage-thresholds.json` declares `lines/branches/functions/statements: 100`, `blockPRCreation: true`, `blockTaskCompletion: true`.
   - `bilbo-app/.semgrep.yml`, `bilbo-app/.codacy.yaml`, `bilbo-app/.deepsource.toml` exist with strict-zero rule sets covering Kotlin, Swift, and TypeScript/Deno.
   - `bilbo-app/sonar-project.properties` references all four coverage report paths: shared Kover XML, androidApp Kover XML, iosApp `xccov` XML, and Deno `cov.lcov`.
   - All 12 required contexts are listed in `quality-zero-platform/profiles/repos/bilbo-app.yml`'s `required_contexts.always` and applied via branch protection.

3. **Real 100% coverage (not Kover-excluded):**
   - The Kover exclusion list in `shared/build.gradle.kts:108-189` shrinks from ~70 classes to **≤5** truly platform-bound classes (the actual `expect`/`actual` Android/iOS driver factories and Keychain/EncryptedPrefs adapters that have no JVM-runnable surface).
   - Every other class — including `HeuristicEngine`, `BuddyManager`, `ChallengeEngine`, `CooldownManager`, `DecisionEngine`, `SeedDataLoader`, `IntentRepository`, `CloudInsightClient`, `AppClassifier`, `DefaultErrorHandler`, etc. — is covered to 100% line + 100% branch via real tests in `commonTest`, exercising the in-memory SQLDelight `JdbcSqliteDriver(IN_MEMORY)` and `MockEngine` Ktor tests.
   - `androidApp` has a Kover module with 100% coverage on every non-Compose-preview class: `BilboApplication`, `MainActivity`, every ViewModel, every Service, every Worker, every DI module verifier.
   - `iosApp` has an `iosAppTests` target in `project.yml` running under `xcodebuild test` with `xccov` reporting; covers every `ContentViewModel` / navigation / FamilyControls integration via stub APIs.
   - `supabase/functions/` has `*_test.ts` next to every `index.ts` running under `deno test --coverage=cov && deno coverage cov --lcov > cov.lcov` to 100% line.
   - The aggregating `assert_coverage_100.py` from `quality-zero-platform/scripts/quality/` runs on all four reports inside the `Coverage 100 Gate` job and exits 0.

4. **Signed release artifacts:**
   - Android `release` build type uses a real keystore decoded from CI secrets (`BILBO_RELEASE_KEYSTORE_BASE64`, `BILBO_RELEASE_KEYSTORE_PASSWORD`, `BILBO_RELEASE_KEY_ALIAS`, `BILBO_RELEASE_KEY_PASSWORD`). The `signingConfig = signingConfigs.getByName("debug")` line in `androidApp/build.gradle.kts:76` is replaced with a `release` config that errors if secrets are absent in CI mode.
   - iOS `release` config is signed via `fastlane match` (or App Store Connect API key + `xcodebuild -allowProvisioningUpdates`) using `IOS_TEAM_ID`, `IOS_DISTRIBUTION_CERT_BASE64`, `IOS_PROVISIONING_PROFILE_BASE64`, `MATCH_PASSWORD` secrets.
   - If the iOS signing secrets are absent at tag time, the workflow fails the release **explicitly** ("iOS signing secrets not configured — cannot ship v2.0.0 final"); it does NOT silently emit an unsigned-but-suffixed artifact.
   - The `v2.0.0` GitHub Release ends up with attached:
     - `bilbo-playstore-release.apk` (signed, R8-shrunk)
     - `bilbo-github-release.apk` (signed, R8-shrunk)
     - `Bilbo-iOS.ipa` (signed, ad-hoc or app-store distribution)
     - `Bilbo-iOS.app.dSYM.zip` (for Sentry symbolication)
     - `bilbo-app-2.0.0-source.tar.gz`
     - `SHA256SUMS.txt` listing all of the above

5. **Documentation final:**
   - README's "Current Status" section is rewritten to describe shipping reality, not "in active early development". Adds quality-zero badges. Adds installation instructions for the signed APK/IPA.
   - `CHANGELOG.md` has a complete `[2.0.0] — 2026-04-29` section grouping every change.
   - `docs/RELEASE_NOTES_v2.0.0.md` is a public-facing release announcement.
   - `docs/QUALITY_GATES.md` mirrors `event-link/docs/quality/QUALITY_ZERO_GATES.md` and lists every gate, threshold, and bypass policy.
   - `docs/CHANGELOG.md` and `README.md` no longer reference any v1.1 follow-up that has been completed (i.e. all "in-memory" / "unconfigured Supabase" / "unsigned" disclaimers are removed).

## 3. Architecture & Approach

### 3.1 The three converging tracks

The work is decomposable into three tracks, all of which must be done for v2.0.0. The ralph-loop drives them in dependency order, but multiple work units within each track can be done in parallel by sub-agents.

**Track A — Functional completion**

| Unit | Scope | Files |
|------|-------|-------|
| A1 | SQLDelight driver wiring (Android + iOS), seed data load on first run | `shared/src/androidMain/kotlin/dev/bilbo/data/AndroidDatabaseDriverFactory.kt`, `shared/src/iosMain/.../IosDatabaseDriverFactory.kt`, `androidApp/src/main/kotlin/dev/bilbo/app/di/RepositoryModule.kt` (replace in-memory impls), iOS `BilboKoinModule.swift` |
| A2 | Real Supabase client init: `BuildConfig.SUPABASE_URL`/`ANON_KEY` from CI secrets; iOS `.xcconfig` parity; SDK init in `BilboApplication.onCreate` and `BilboApp.init` | `androidApp/build.gradle.kts`, `iosApp/Configs/*.xcconfig`, `shared/.../SupabaseClient.kt` |
| A3 | Edge-function client wrappers in shared, with retry / error mapping / telemetry | `shared/src/commonMain/kotlin/dev/bilbo/data/remote/BilboApiService.kt` and 13 callers in repository impls |
| A4 | iOS `AuthManager.hasActiveSession()` ObjC export + `ContentViewModel.checkAuthState()` real impl | `shared/.../AuthManager.kt` (`@ObjCName` annotations), `iosApp/.../ContentViewModel.swift` |
| A5 | Real Settings sub-screens: enforcement, sharing-level, Tier-3 opt-in, data-anonymization preview, per-app overrides | `androidApp/.../ui/screen/settings/*.kt`, `iosApp/.../Settings/*.swift` |
| A6 | iOS extensions added as targets in `project.yml` and wired | `iosApp/project.yml`, `iosApp/iosApp/DeviceActivityMonitorExtension.swift` (currently excluded) |
| A7 | Realtime subscriptions for `BuddyManager` / `CircleManager` / `ChallengeEngine` | `shared/.../social/*.kt` |
| A8 | FCM (Android) + APNs (iOS) push registration; replace `push-notification/index.ts:92` APNs stub with real `apple/notifications` | `androidApp/.../service/BilboFirebaseMessagingService.kt`, `iosApp/.../AppDelegate.swift`, `supabase/functions/push-notification/index.ts` |
| A9 | Tier-3 narrative wiring + UI surface | `shared/intelligence/tier3/CloudInsightClient.kt`, `androidApp/.../ui/screen/WeeklyInsightScreen.kt`, iOS analog |
| A10 | Replace every "coming soon" placeholder with real impl (Insights, Analog suggestions sheet on iOS, Settings data export) | per agent-listed paths |

**Track B — Quality-Zero gates**

| Unit | Scope | Files (in `bilbo-app` unless noted) |
|------|-------|--------------------------------------|
| B1 | Onboard `bilbo-app` into `quality-zero-platform/inventory/repos.yml`, author `profiles/stacks/kotlin-multiplatform.yml` and `profiles/repos/bilbo-app.yml` | files in **`Prekzursil/quality-zero-platform`** (separate PR there) |
| B2 | Add 5 reusable-caller workflow files (`quality-zero-gate.yml`, `quality-zero-platform.yml`, `quality-zero-backlog.yml`, `quality-zero-remediation.yml`, replace `codeql.yml`) | `.github/workflows/` |
| B3 | Remove every `continue-on-error: true` in existing `android-ci.yml`, `ios-ci.yml`, `backend-ci.yml`. Add `koverVerify` as a real failing step on android too. Add `swiftlint --strict` (no `continue-on-error`). | `.github/workflows/*.yml` |
| B4 | Add config files: `.coverage-thresholds.json`, `.semgrep.yml`, `.codacy.yaml`, `.deepsource.toml`, `.gitleaks.toml`, `.tool-versions` | repo root |
| B5 | Update `sonar-project.properties` to include iOS sources, Deno function sources, all 4 coverage report paths | `sonar-project.properties` |
| B6 | Reduce `shared/build.gradle.kts` Kover exclusion list from ~70 to ≤5 truly platform-bound classes; backfill commonTest tests until 100% real coverage | `shared/build.gradle.kts`, `shared/src/commonTest/.../*Test.kt` |
| B7 | Add Kover to `androidApp/build.gradle.kts`; write missing tests (services, workers, ViewModels, DI verifiers) to 100% | `androidApp/build.gradle.kts`, `androidApp/src/test/.../*Test.kt` |
| B8 | Add iOS test target in `project.yml` with XCTest suite; cover ViewModels, navigation, FamilyControls integration via stubs | `iosApp/project.yml`, `iosApp/iosAppTests/.../*.swift` |
| B9 | Add Deno `*_test.ts` next to every `index.ts` in `supabase/functions/`; configure `deno.json` with coverage settings | `supabase/functions/*/index_test.ts`, `supabase/deno.json` |
| B10 | Local `scripts/verify` script that mirrors `event-link/scripts/verify` and runs all 12 lanes locally. `scripts/quality/` adapted Python wrappers | `scripts/verify`, `scripts/quality/*.py` |

**Track C — Release polish**

| Unit | Scope |
|------|-------|
| C1 | Real Android signing config + CI secrets; reject build if signing absent in release mode |
| C2 | iOS signing via fastlane match or ASC API key + provisioning profile decode |
| C3 | Bump `versionName=2.0.0`, `MARKETING_VERSION=2.0.0`, `versionCode=2`; update KMP `libs.versions.toml` if any drift |
| C4 | Rewrite README.md "Current Status" + add quality-zero badges + add signed-install instructions |
| C5 | Author `CHANGELOG.md [2.0.0]` section, `docs/RELEASE_NOTES_v2.0.0.md`, `docs/QUALITY_GATES.md` |
| C6 | `attach-release-artifacts` workflow updated to upload signed APKs + IPA + dSYM + source tarball + SHA256SUMS |

### 3.2 Why a single big-bang v2.0.0 instead of iterative v1.1, v1.2 …

The user's directive is explicit ("update the release into a final version"). More importantly, the gates are interlocked:

- We cannot enforce 100% coverage on `androidApp` while the repositories are stubs (because tests would prove correctness of stubs).
- We cannot enforce SonarCloud Sonar Zero while CI tolerates `continue-on-error: true`.
- We cannot ship a signed `.ipa` while `iosApp/project.yml` excludes the FamilyControls extensions.

So the migration is atomic: every gate flips from "soft" to "blocking" in the same release that makes the code actually meet the threshold. Phase progression (`shadow → ratchet → absolute`) is orthogonal — we ship `absolute` from day one because the code must already pass `absolute` for v2.0.0 to be tagged.

### 3.3 Why a ralph-loop instead of a single agent

A normal Task agent is bounded by its context window. The work above spans 70+ files across 4 languages, 4 build systems, two repos (bilbo-app and quality-zero-platform), and a long-running iteration loop:

> implement → run all 12 lanes locally → fix every finding → commit → push → wait for CI → if any lane red, fix → loop until green → tag → publish

A ralph-loop with **dynamic self-pacing** is ideal: each iteration picks up the next unblocked work unit from `.beads/plans/active-plan.md`, mutates code, runs `bash scripts/verify`, commits if green, and reschedules itself until the stop condition is met. Context resets between iterations are recovered via `.beads/context/execution-state.md`.

## 4. Component Boundaries

The work modifies the following independently-owned units:

- **`shared`** — KMP module: domain, repositories, intelligence, economy, social, tracking. Owner of business logic.
- **`androidApp`** — Android UI + services. Imports `:shared`. Owns Hilt DI, foreground service, overlay, navigation.
- **`iosApp`** — iOS UI + extensions. Consumes `Shared.xcframework`. Owns SwiftUI views, FamilyControls extensions, app delegate.
- **`supabase`** — Backend: edge functions + migrations. Owned independently of mobile.
- **`.github/workflows/`** — CI policy. Owned by infra; consumes the other units' outputs.
- **`Prekzursil/quality-zero-platform/profiles/`** — Cross-repo quality policy. Modified via a separate PR in the platform repo.

Each unit has a clear input contract (interfaces / API endpoints / build outputs) and a clear test boundary (commonTest / Android unit + instrumented / XCTest / `deno test` / CI assertion scripts).

## 5. Data Flow

No new data flow is introduced. The architecture diagrams in `README.md` are already correct. What changes is that data **actually flows** end-to-end:

```
Android UsageTrackingService
  → SessionTracker
  → UsageRepository (NOW: SQLDelight-backed, was: in-memory)
  → BilboDatabase (Android driver)
  → SyncWorker (periodic)
  → BilboApiService.syncStatus()  (NEW: actually calls Supabase)
  → supabase/functions/sync-status/index.ts
  → PostgreSQL status_summaries table
  → Realtime subscription back to other clients
```

The iOS path is symmetric, replacing AndroidSqliteDriver with NativeSqliteDriver and FCM with APNs.

## 6. Error Handling

All boundaries already have error types defined in `shared/util/BilboError.kt` (per the Kover exclusion comment "all subclasses have defaults"). The work:

1. Routes every Supabase call through `DefaultErrorHandler` and emits `Result<T>` to repositories.
2. Surfaces `BilboError.NetworkUnavailable` / `Unauthorized` / `RateLimited` to UI as snackbars with retry CTA.
3. Logs to Sentry with PII redaction (no usernames, no app-package names of installed apps in error metadata — only error class + repository name + retry count).
4. Edge functions return RFC-7807 problem-details JSON; mobile maps to typed errors.
5. APNs/FCM send failures are surfaced via `push-notification` edge function logs (Sentry-instrumented).

## 7. Testing Strategy

Per quality-zero-platform's `whole-project-100` policy:

- **commonTest:** Hand-written fakes (no MockK on shared because it's not multiplatform). `JdbcSqliteDriver(IN_MEMORY)` for all SQLDelight tests. Ktor `MockEngine` for all HTTP tests. `runTest` + Turbine for all coroutine/Flow tests.
- **androidApp/src/test:** JUnit 4 + MockK + Turbine + `runTest`. Tests every ViewModel state transition, Service binding/unbinding, Worker `doWork()`, BootReceiver intent handling, Hilt module wiring (via `@HiltAndroidTest` + `HiltAndroidRule`).
- **androidApp/src/androidTest:** Compose UI tests for every screen, instrumented SQLDelight smoke test (real driver), accessibility audit via Espresso accessibility checks.
- **iosAppTests:** XCTest suite mocking the `Shared` framework's repositories via SwiftUI ViewModel fakes; `xccov` extracted post-test.
- **supabase/functions:** `deno test --coverage` per function, exercising happy path, auth failure, validation failure, and rate-limit branches. PostgreSQL test container via `pg_tap` for migrations.
- **E2E gate:** `scripts/e2e/smoke.sh` boots local Supabase stack, installs the playstore-flavor APK in an emulator (if available in CI runner; skipped on fork PRs), and runs a 60-second usage-session simulation. Optional gate; not blocking for tag.

## 8. Out of Scope (explicit non-goals)

- Wear OS / watchOS apps (Phase 5 in README; deferred to v2.1).
- Web admin dashboard (deferred to v2.1).
- F-Droid Inclusion Request paperwork (deferred; we ship the GitHub-flavor APK to GitHub Releases only).
- App Store / Google Play store submission (we attach signed binaries to GitHub Releases; submission to stores is a manual step the user performs after this release).
- Custom on-device LLM (Tier-3 stays cloud-only via `ai-weekly-insight` edge function).

## 9. Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Achieving real 100% coverage on KMP `expect`/`actual` boundaries is fragile | Excluded list capped at ≤5 classes — verified by file path in `shared/build.gradle.kts`; rest must be covered |
| iOS signing secrets may not be available | Fail the release explicitly with a clear error message; do not emit unsigned-but-labelled artifacts (closes a foot-gun where unsigned shipped under v2.0.0 banner) |
| quality-zero-platform reusable workflows may not yet support Kotlin | Track B1 authors the new `kotlin-multiplatform.yml` stack profile inside that repo; this is a co-evolution and is in-scope |
| Codacy / DeepScan / DeepSource may not have Kotlin analyzers at the same fidelity as Python/JS | Codacy detekt + ktlint plugins exist; DeepScan is JS-only (skipped on Kotlin path; still required on TS edge functions); DeepSource has Kotlin beta — enable it |
| The ralph-loop could thrash on a flaky test | Each iteration's `scripts/verify` is deterministic; flaky tests are quarantined to `*FlakyTest.kt` and re-run with `--rerun-failed` 3× before declared red |
| Big-bang release vs. iterative may surprise users | Mitigated by `docs/RELEASE_NOTES_v2.0.0.md` explaining the change and by keeping v1.0.1's API contract surface |

## 10. Stop condition (the loop knows when to halt)

The ralph-loop iterates until **all** of these are true:

1. `gh run list --workflow=quality-zero-gate.yml --branch=main --limit=1 --json conclusion -q '.[0].conclusion'` → `success`
2. `gh release view v2.0.0 --json assets -q '.assets | length'` → `>= 6`
3. `python scripts/quality/assert_coverage_100.py --reports shared/build/reports/kover/report.xml androidApp/build/reports/kover/report.xml iosApp/build/coverage/ios.xml supabase/functions/coverage/cov.lcov` exits 0
4. `git log --oneline -1 origin/main` is the v2.0.0 release commit, with tag `v2.0.0` resolved

If any are false, the loop reschedules itself with a brief delay and continues. If the loop has run 50 iterations without progress (no new commits since last check), it stops and reports.

## 11. Approval

This document is the binding contract. Implementation deviates from it only if a deviation is committed to a follow-up commit on this file with a clear `## Deviation YYYY-MM-DD` header.
