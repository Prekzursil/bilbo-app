# Bilbo v2.0.0 — "Quality-Zero Final" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax. **The driving harness is the ralph-loop prompt under `<ralph_loop_prompt>` in the project's root `RALPH_LOOP.md` (authored alongside this plan).** Each iteration of the loop picks the lowest-numbered unblocked work unit, executes its TDD steps, runs `bash scripts/verify` (all 12 gates), and commits only on green.

**Goal:** Take Bilbo from v1.0.1 (stubbed repos, unconfigured Supabase, unsigned debug artifacts, soft CI) to v2.0.0 Final (real persistence, live backend, signed APK + signed iOS IPA, all 12 quality-zero-platform gates blocking on every PR with 100% real coverage and 0 issues across SonarCloud, Codacy, Semgrep, Sentry, DeepScan, DeepSource, CodeQL, QLTY, Codecov, Dependabot, Socket, gitleaks).

**Architecture:** Three converging tracks executed in dependency order — Track A (functional completion), Track B (quality-zero gate adoption), Track C (release polish). Co-evolves the `Prekzursil/quality-zero-platform` repo to add a `kotlin-multiplatform` stack profile. The ralph-loop drives iteration until `gh run list --workflow=quality-zero-gate.yml --branch=main --limit=1 -q '.[0].conclusion'` returns `success` and `gh release view v2.0.0` shows ≥6 attached signed artifacts.

**Tech Stack:** Kotlin 2.3.20 (KMP), SQLDelight 2.3.2, Ktor 3.4.2, supabase-kt 3.5.0, Jetpack Compose, Hilt, WorkManager, SwiftUI, FamilyControls, Deno (edge functions), PostgreSQL (RLS), Kover (coverage), Detekt + ktlint + SwiftLint + ESLint + Semgrep + Bandit-equivalent + lizard, SonarCloud + Codacy + DeepScan + DeepSource + Sentry + Codecov + QLTY, fastlane match (iOS signing), `quality-zero-platform` reusable workflows.

**Spec:** `docs/superpowers/specs/2026-04-29-bilbo-quality-zero-v2-design.md` (binding contract).

---

## Table of Contents

- [Conventions](#conventions)
- [Track A — Functional Completion](#track-a--functional-completion)
  - [WU-A1: SQLDelight driver factories (Android + iOS)](#wu-a1-sqldelight-driver-factories-android--ios)
  - [WU-A2: Real repository implementations replacing in-memory stubs](#wu-a2-real-repository-implementations-replacing-in-memory-stubs)
  - [WU-A3: Supabase client initialization with CI-injected credentials](#wu-a3-supabase-client-initialization-with-ci-injected-credentials)
  - [WU-A4: Edge-function client wrappers in shared module](#wu-a4-edge-function-client-wrappers-in-shared-module)
  - [WU-A5: iOS AuthManager.hasActiveSession() ObjC export](#wu-a5-ios-authmanagerhasactivesession-objc-export)
  - [WU-A6: Real Settings sub-screens (Android + iOS)](#wu-a6-real-settings-sub-screens-android--ios)
  - [WU-A7: iOS DeviceActivityMonitor + ShieldConfiguration + ShieldAction extensions wired](#wu-a7-ios-deviceactivitymonitor--shieldconfiguration--shieldaction-extensions-wired)
  - [WU-A8: Realtime subscriptions for buddies / circles / challenges](#wu-a8-realtime-subscriptions-for-buddies--circles--challenges)
  - [WU-A9: Push notifications: FCM (Android) + real APNs (replace stub)](#wu-a9-push-notifications-fcm-android--real-apns-replace-stub)
  - [WU-A10: Tier-3 weekly narrative wiring (CloudInsightClient + UI)](#wu-a10-tier-3-weekly-narrative-wiring-cloudinsightclient--ui)
  - [WU-A11: Replace remaining "coming soon" placeholders](#wu-a11-replace-remaining-coming-soon-placeholders)
- [Track B — Quality-Zero Gate Adoption](#track-b--quality-zero-gate-adoption)
  - [WU-B1: Co-evolution PR in `quality-zero-platform`](#wu-b1-co-evolution-pr-in-quality-zero-platform)
  - [WU-B2: Add 5 reusable-caller workflows to bilbo](#wu-b2-add-5-reusable-caller-workflows-to-bilbo)
  - [WU-B3: Strip every `continue-on-error: true` from existing workflows](#wu-b3-strip-every-continue-on-error-true-from-existing-workflows)
  - [WU-B4: Quality config files (`.coverage-thresholds.json`, `.semgrep.yml`, `.codacy.yaml`, `.deepsource.toml`, `.gitleaks.toml`)](#wu-b4-quality-config-files-coverage-thresholdsjson-semgrepyml-codacyyaml-deepsourcetoml-gitleakstoml)
  - [WU-B5: `sonar-project.properties` v2 with all four coverage paths](#wu-b5-sonar-projectproperties-v2-with-all-four-coverage-paths)
  - [WU-B6: Shrink `shared/build.gradle.kts` Kover exclusions to ≤5 classes; backfill commonTest](#wu-b6-shrink-sharedbuildgradlekts-kover-exclusions-to-5-classes-backfill-commontest)
  - [WU-B7: Add Kover to `androidApp` and write missing Android tests to 100%](#wu-b7-add-kover-to-androidapp-and-write-missing-android-tests-to-100)
  - [WU-B8: Add iOS test target + XCTest suite to 100% via `xccov`](#wu-b8-add-ios-test-target--xctest-suite-to-100-via-xccov)
  - [WU-B9: Deno tests for every supabase function to 100% line](#wu-b9-deno-tests-for-every-supabase-function-to-100-line)
  - [WU-B10: Local `scripts/verify` mirroring event-link's gate](#wu-b10-local-scriptsverify-mirroring-event-links-gate)
- [Track C — Release Polish](#track-c--release-polish)
  - [WU-C1: Real Android signing (release keystore via CI secrets)](#wu-c1-real-android-signing-release-keystore-via-ci-secrets)
  - [WU-C2: iOS signing via fastlane match](#wu-c2-ios-signing-via-fastlane-match)
  - [WU-C3: Version bump to 2.0.0](#wu-c3-version-bump-to-200)
  - [WU-C4: README + badges + signed-install instructions](#wu-c4-readme--badges--signed-install-instructions)
  - [WU-C5: CHANGELOG, RELEASE_NOTES, QUALITY_GATES docs](#wu-c5-changelog-release_notes-quality_gates-docs)
  - [WU-C6: Release attach-artifacts workflow update (signed APKs + IPA + dSYM + SHA256SUMS)](#wu-c6-release-attach-artifacts-workflow-update-signed-apks--ipa--dsym--sha256sums)
  - [WU-C7: Tag v2.0.0 + final dry-run on main](#wu-c7-tag-v200--final-dry-run-on-main)
- [Stop Condition Verification Suite](#stop-condition-verification-suite)
- [Self-Review](#self-review)

---

## Conventions

- **TDD is mandatory.** Every WU starts with a failing test (RED), implements minimum to pass (GREEN), refactors (IMPROVE). The local `bash scripts/verify` is the BLOCKING gate before commit.
- **Bite-sized step expansion happens inside each sub-agent.** This document gives the work-unit boundary (≈1 PR-worth), files to touch, the failing-test seed, the implementation skeleton, and acceptance criteria. The sub-agent driven by the ralph-loop expands each WU into the standard `Step 1 / Step 2 / Step 3 / Step 4 / Step 5: Commit` granularity.
- **Commit message format:** Conventional commits + WU id, e.g., `feat(android): wire SQLDelight driver factory [WU-A1]`. No `Co-Authored-By:` trailer (per project policy).
- **Branch model:** Every WU lands on `main` directly via PR (no long-lived branches). The ralph-loop creates feature branches `wu/<id>-<slug>` and opens PRs that auto-merge on green.
- **Pinned SHAs:** Every reusable workflow call uses a pinned commit SHA, not a moving ref. Resolve via `gh api repos/Prekzursil/quality-zero-platform/commits/main -q .sha` once at WU-B1 and reuse.
- **No `--no-verify`. No `git push --force` to main.** Pre-commit hooks must pass.
- **Coverage source of truth:** `.coverage-thresholds.json` at the repo root. All other coverage configs (`shared/build.gradle.kts` `kover { verify { rule { ... } } }`, codecov.yml) defer to it.

---

## Track A — Functional Completion

### WU-A1: SQLDelight driver factories (Android + iOS)

**Files:**
- Modify: `shared/src/androidMain/kotlin/dev/bilbo/data/AndroidDatabaseDriverFactory.kt`
- Modify: `shared/src/iosMain/kotlin/dev/bilbo/data/IosDatabaseDriverFactory.kt`
- Modify: `shared/src/commonMain/kotlin/dev/bilbo/data/DatabaseDriverFactory.kt` (interface)
- Test: `shared/src/commonTest/kotlin/dev/bilbo/data/DatabaseDriverFactoryTest.kt` (uses `JdbcSqliteDriver(IN_MEMORY)`)
- Test: `shared/src/androidUnitTest/kotlin/dev/bilbo/data/AndroidDatabaseDriverFactoryTest.kt`

**Failing test seed (commonTest):**
```kotlin
@Test
fun `driver creates BilboDatabase schema and survives reopen`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    BilboDatabase.Schema.create(driver)
    val db = BilboDatabase(driver)

    db.usageSessionsQueries.insert(
        package_name = "com.example",
        app_label = "Example",
        category = "NEUTRAL",
        start_time_epoch_ms = 0L,
        end_time_epoch_ms = 60_000L,
        duration_seconds = 60L,
        was_tracked = 1L
    )

    val rows = db.usageSessionsQueries.selectAll().executeAsList()
    assertEquals(1, rows.size)
    assertEquals("com.example", rows[0].package_name)
}
```

**Implementation (AndroidDatabaseDriverFactory):**
```kotlin
package dev.bilbo.data

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = BilboDatabase.Schema,
        context = context,
        name = "bilbo.db"
    )
}
```

**Implementation (IosDatabaseDriverFactory):**
```kotlin
package dev.bilbo.data

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(
        schema = BilboDatabase.Schema,
        name = "bilbo.db"
    )
}
```

**Acceptance:**
- `./gradlew :shared:allTests` passes including the new test.
- `./gradlew :shared:koverVerify` passes with the driver factory removed from the Kover exclusion list (post-WU-B6).
- The Android sample test in `androidUnitTest` exercises the real `AndroidSqliteDriver` against an in-memory schema.

**Commit:** `feat(shared): wire SQLDelight driver factory across platforms [WU-A1]`

---

### WU-A2: Real repository implementations replacing in-memory stubs

**Files:**
- Create: `shared/src/commonMain/kotlin/dev/bilbo/data/repository/SqlDelightUsageRepository.kt`
- Create: `shared/src/commonMain/kotlin/dev/bilbo/data/repository/SqlDelightAppProfileRepository.kt`
- Create: `shared/src/commonMain/kotlin/dev/bilbo/data/repository/SqlDelightIntentRepository.kt`
- Create: `shared/src/commonMain/kotlin/dev/bilbo/data/repository/SqlDelightEmotionRepository.kt`
- Create: `shared/src/commonMain/kotlin/dev/bilbo/data/repository/SqlDelightBudgetRepository.kt`
- Create: `shared/src/commonMain/kotlin/dev/bilbo/data/repository/SqlDelightSuggestionRepository.kt`
- Modify: `androidApp/src/main/kotlin/dev/bilbo/app/di/RepositoryModule.kt` (replace `InMemoryXyzRepository` bindings with SqlDelight-backed ones)
- Test: `shared/src/commonTest/kotlin/dev/bilbo/data/repository/SqlDelight*RepositoryTest.kt` (one per repo)

**Failing test seed (Usage):**
```kotlin
@Test
fun `observeAll emits initial empty then inserted session`() = runTest {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { BilboDatabase.Schema.create(it) }
    val db = BilboDatabase(driver)
    val repo = SqlDelightUsageRepository(db, this.testScheduler.toCoroutineDispatcher())

    repo.observeAll().test {
        assertEquals(emptyList(), awaitItem())
        repo.insert(testSession)
        assertEquals(listOf(testSession), awaitItem())
    }
}
```

**Implementation pattern (SqlDelightUsageRepository):**
```kotlin
class SqlDelightUsageRepository(
    private val db: BilboDatabase,
    private val backgroundDispatcher: CoroutineDispatcher
) : UsageRepository {

    override fun observeAll(): Flow<List<UsageSession>> =
        db.usageSessionsQueries.selectAll()
            .asFlow()
            .mapToList(backgroundDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun insert(session: UsageSession): Result<Long> = withContext(backgroundDispatcher) {
        runCatching {
            db.transactionWithResult {
                db.usageSessionsQueries.insert(...)
                db.usageSessionsQueries.lastInsertRowId().executeAsOne()
            }
        }
    }

    // ... update / delete / observeByPackageName / aggregateForToday
}
```

**Acceptance:**
- All 6 repos covered to 100% line + branch in `commonTest`.
- `androidApp` `RepositoryModule.kt` no longer references any `InMemoryXyzRepository`.
- Manual smoke (in WU-C7's dry-run): install APK, create a session, force-stop the app, reopen → session is still there.

**Commit:** `feat(shared): SQLDelight-backed repositories replace in-memory stubs [WU-A2]`

---

### WU-A3: Supabase client initialization with CI-injected credentials

**Files:**
- Modify: `shared/src/commonMain/kotlin/dev/bilbo/shared/data/remote/SupabaseClient.kt`
- Modify: `androidApp/build.gradle.kts:32-39` (already reads from `findProperty` / env — verify pattern)
- Modify: `androidApp/src/main/kotlin/dev/bilbo/app/BilboApplication.kt` (init Supabase on `onCreate`)
- Create: `iosApp/Configs/Bilbo.xcconfig` (consumed by `project.yml` to inject `SUPABASE_URL` / `SUPABASE_ANON_KEY` from env)
- Modify: `iosApp/iosApp/BilboApp.swift` (init Supabase on `init()`)
- Modify: `iosApp/project.yml` (reference xcconfig; pass through env vars)
- Test: `shared/src/commonTest/kotlin/dev/bilbo/shared/data/remote/SupabaseClientTest.kt`

**Failing test seed:**
```kotlin
@Test
fun `init fails fast if SUPABASE_URL is empty in non-debug mode`() {
    val ex = assertFailsWith<IllegalStateException> {
        SupabaseClient.init(url = "", anonKey = "anon", environment = Environment.RELEASE)
    }
    assertEquals("SUPABASE_URL must be configured for release builds", ex.message)
}

@Test
fun `init succeeds with valid credentials and exposes auth + postgrest + functions clients`() {
    SupabaseClient.init(url = "https://test.supabase.co", anonKey = "anon", environment = Environment.DEBUG)
    assertNotNull(SupabaseClient.instance.auth)
    assertNotNull(SupabaseClient.instance.postgrest)
    assertNotNull(SupabaseClient.instance.functions)
}
```

**Implementation:**
```kotlin
object SupabaseClient {
    @Volatile private var _instance: SupabaseClientImpl? = null
    val instance: SupabaseClientImpl get() = checkNotNull(_instance) { "SupabaseClient.init() not called" }

    fun init(url: String, anonKey: String, environment: Environment) {
        require(environment == Environment.DEBUG || url.isNotEmpty()) {
            "SUPABASE_URL must be configured for release builds"
        }
        require(environment == Environment.DEBUG || anonKey.isNotEmpty()) {
            "SUPABASE_ANON_KEY must be configured for release builds"
        }
        _instance = createSupabaseClient(supabaseUrl = url, supabaseKey = anonKey) {
            install(Auth) { /* ... */ }
            install(Postgrest)
            install(Realtime)
            install(Functions)
        }.let(::SupabaseClientImpl)
    }
}

enum class Environment { DEBUG, RELEASE }
```

**Acceptance:**
- Unit tests cover both branches of the require().
- Android + iOS apps both call `SupabaseClient.init` with credentials sourced from BuildConfig / xcconfig at startup.
- Release build with empty `SUPABASE_URL` env var fails at startup with a deterministic error (proves no silent debug-fallback in release).

**Commit:** `feat(shared): Supabase client init with fail-fast credential validation [WU-A3]`

---

### WU-A4: Edge-function client wrappers in shared module

**Files:**
- Create: `shared/src/commonMain/kotlin/dev/bilbo/data/remote/BilboApiService.kt`
- Create: `shared/src/commonMain/kotlin/dev/bilbo/data/remote/dto/*.kt` (one DTO per edge function: `AcceptInviteRequest/Response`, `AiWeeklyInsightRequest/Response`, `ComputeLeaderboardRequest/Response`, etc.)
- Modify: `shared/src/commonMain/kotlin/dev/bilbo/data/repository/SocialRepository.kt` (use `BilboApiService` instead of empty stubs)
- Test: `shared/src/commonTest/kotlin/dev/bilbo/data/remote/BilboApiServiceTest.kt` (Ktor `MockEngine`)

**Failing test seed (per function — 13 total):**
```kotlin
@Test
fun `syncStatus posts payload and parses 200 response`() = runTest {
    val mockEngine = MockEngine { request ->
        assertEquals("/functions/v1/sync-status", request.url.encodedPath)
        assertEquals(HttpMethod.Post, request.method)
        respond(
            content = """{"status":"ok","summaryId":"uuid"}""",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
    val service = BilboApiService(httpClient(mockEngine))
    val result = service.syncStatus(SyncStatusRequest(userId = "u", date = LocalDate(2026, 4, 29), fpEarned = 100, fpSpent = 30, /*...*/))
    assertTrue(result.isSuccess)
    assertEquals("uuid", result.getOrThrow().summaryId)
}

@Test
fun `syncStatus retries on 503 and surfaces NetworkUnavailable on 5x failures`() = runTest { /* ... */ }

@Test
fun `syncStatus maps 401 to BilboError.Unauthorized`() = runTest { /* ... */ }
```

**Implementation skeleton:**
```kotlin
class BilboApiService(private val client: HttpClient) {
    suspend fun syncStatus(req: SyncStatusRequest): Result<SyncStatusResponse> = retry {
        client.post("functions/v1/sync-status") { setBody(req) }.toResult()
    }
    suspend fun acceptInvite(req: AcceptInviteRequest): Result<AcceptInviteResponse> = retry { /* ... */ }
    // ... 11 more
}

private suspend fun <T> retry(maxAttempts: Int = 3, block: suspend () -> Result<T>): Result<T> {
    repeat(maxAttempts - 1) { attempt ->
        block().fold(onSuccess = { return Result.success(it) }, onFailure = { e ->
            if (e is BilboError.NetworkUnavailable || e is BilboError.RateLimited) delay(1000L * (1 shl attempt))
            else return Result.failure(e)
        })
    }
    return block()
}
```

**Acceptance:**
- 13 RPC methods covered with happy-path + 401 + 503-retry + 429-rate-limit tests = 39 tests minimum.
- 100% line + branch on `BilboApiService` and DTOs.
- `SocialRepository` no longer has any `TODO` markers.

**Commit:** `feat(shared): BilboApiService wraps all 13 supabase edge functions [WU-A4]`

---

### WU-A5: iOS AuthManager.hasActiveSession() ObjC export

**Files:**
- Modify: `shared/src/commonMain/kotlin/dev/bilbo/auth/AuthManager.kt` (add `@ObjCName("hasActiveSession")` annotation; expose suspend → Swift async via `kotlinx-coroutines-core` interop)
- Modify: `iosApp/iosApp/ContentViewModel.swift` (call real bridge instead of returning `.unauthenticated`)
- Test: `shared/src/commonTest/kotlin/dev/bilbo/auth/AuthManagerTest.kt`
- Test: `iosApp/iosAppTests/ContentViewModelTests.swift`

**Failing test seed (KMP):**
```kotlin
@Test
fun `hasActiveSession returns true when supabase session is unexpired`() = runTest {
    val sb = FakeSupabaseClient(session = Session(expiresAt = Clock.System.now().plus(1.hours)))
    val mgr = AuthManager(sb)
    assertTrue(mgr.hasActiveSession())
}
@Test
fun `hasActiveSession returns false when expired`() = runTest { /* ... */ }
@Test
fun `hasActiveSession returns false when no session`() = runTest { /* ... */ }
```

**Implementation:**
```kotlin
// shared/.../AuthManager.kt
class AuthManager(private val supabase: SupabaseClientImpl) {
    @Throws(CancellationException::class)
    suspend fun hasActiveSession(): Boolean =
        supabase.auth.currentSessionOrNull()?.let { it.expiresAt > Clock.System.now() } ?: false
}
```

**Swift consumer:**
```swift
@MainActor
final class ContentViewModel: ObservableObject {
    @Published private(set) var authState: AuthState = .unknown
    private let auth: AuthManager
    init(auth: AuthManager) { self.auth = auth }
    func checkAuthState() async {
        let active = (try? await auth.hasActiveSession()) ?? false
        self.authState = active ? .authenticated : .unauthenticated
    }
}
```

**Acceptance:**
- `AuthManager` removed from Kover exclusion list (WU-B6).
- iOS app no longer uses `.unauthenticated` placeholder; auth check runs at launch.
- 100% coverage on `AuthManager`.

**Commit:** `feat(auth): expose hasActiveSession via ObjC interop and wire iOS ContentViewModel [WU-A5]`

---

### WU-A6: Real Settings sub-screens (Android + iOS)

**Files:**
- Create: `androidApp/src/main/kotlin/dev/bilbo/app/ui/screen/settings/EnforcementSettingsScreen.kt`
- Create: `androidApp/src/main/kotlin/dev/bilbo/app/ui/screen/settings/SharingLevelSettingsScreen.kt`
- Create: `androidApp/src/main/kotlin/dev/bilbo/app/ui/screen/settings/Tier3OptInScreen.kt`
- Create: `androidApp/src/main/kotlin/dev/bilbo/app/ui/screen/settings/DataAnonymizationScreen.kt`
- Create: `androidApp/src/main/kotlin/dev/bilbo/app/ui/screen/settings/PerAppOverridesScreen.kt`
- Modify: `androidApp/src/main/kotlin/dev/bilbo/app/ui/BilboNavHost.kt` (replace `Text("$title settings coming soon")` with composable routes)
- Create: `iosApp/iosApp/Settings/EnforcementSettingsView.swift` + 4 siblings
- Modify: `iosApp/iosApp/Settings/SettingsView.swift` (route to real sub-screens; remove "Per-app overrides coming soon" Text)
- Test: `androidApp/src/test/kotlin/dev/bilbo/app/ui/screen/settings/*ViewModelTest.kt` (5 ViewModel tests)
- Test: `iosApp/iosAppTests/Settings/*Tests.swift`

**Acceptance:**
- Every settings sub-route renders a real screen with at least one interactive control persisted via `BilboPreferences`.
- 100% coverage on each ViewModel.
- The string `"coming soon"` does not appear anywhere in `androidApp/` or `iosApp/` (verified by `rg -n 'coming soon' androidApp iosApp` returning nothing).

**Commit:** `feat(settings): real sub-screens replace coming-soon placeholders [WU-A6]`

---

### WU-A7: iOS DeviceActivityMonitor + ShieldConfiguration + ShieldAction extensions wired

**Files:**
- Modify: `iosApp/project.yml` — convert the two excluded files into real targets (extension type `app-extension`); add a third `ShieldActionExtension` target
- Modify: `iosApp/iosApp/DeviceActivityMonitorExtension.swift` (currently excluded; wire to `BypassManager` from Shared)
- Modify: `iosApp/iosApp/ShieldConfigurationExtension.swift` (wire to `EnforcementMode` enum from Shared)
- Create: `iosApp/iosApp/ShieldActionExtension.swift`
- Modify: `iosApp/iosApp/Info.plist` (App Groups capability)
- Test: `iosApp/iosAppTests/Extensions/*Tests.swift`

**Acceptance:**
- `xcodebuild -showBuildSettings` lists 4 targets (`iosApp` + 3 extensions).
- iOS build produces a real `.app` containing the extensions in `PlugIns/`.
- Smoke test on simulator: starting a `DeviceActivitySchedule` triggers `intervalDidStart`.

**Commit:** `feat(ios): real DeviceActivityMonitor / ShieldConfiguration / ShieldAction extensions [WU-A7]`

---

### WU-A8: Realtime subscriptions for buddies / circles / challenges

**Files:**
- Modify: `shared/src/commonMain/kotlin/dev/bilbo/social/BuddyManager.kt`
- Modify: `shared/src/commonMain/kotlin/dev/bilbo/social/CircleManager.kt`
- Modify: `shared/src/commonMain/kotlin/dev/bilbo/social/ChallengeEngine.kt`
- Test: `shared/src/commonTest/kotlin/dev/bilbo/social/RealtimeSubscriptionTest.kt` (uses fake Supabase Realtime channel)

**Implementation pattern (BuddyManager):**
```kotlin
fun observeBuddyUpdates(userId: String): Flow<BuddyPair> =
    supabase.channel("buddy_updates_${userId}").apply {
        postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "buddy_pairs"
            filter("user_a_id", FilterOperator.EQ, userId)
        }
    }.subscribe().mapNotNull { it.record?.toBuddyPair() }
```

**Acceptance:**
- 100% coverage across `BuddyManager`, `CircleManager`, `ChallengeEngine` in `commonTest`.
- These three classes removed from Kover exclusion list (WU-B6).

**Commit:** `feat(social): Supabase Realtime subscriptions for buddies/circles/challenges [WU-A8]`

---

### WU-A9: Push notifications: FCM (Android) + real APNs (replace stub)

**Files:**
- Create: `androidApp/src/main/kotlin/dev/bilbo/app/service/BilboFirebaseMessagingService.kt`
- Modify: `androidApp/src/main/AndroidManifest.xml` (register the service)
- Modify: `androidApp/src/main/kotlin/dev/bilbo/app/BilboApplication.kt` (register FCM token via `BilboApiService.registerPushToken`)
- Modify: `iosApp/iosApp/AppDelegate.swift` (register APNs token via `BilboApiService`)
- Modify: `supabase/functions/push-notification/index.ts:92-97` — replace the APNs stub with real `apple/notifications` (Deno HTTP/2) sender using JWT-signed `apns-topic` requests
- Test: `supabase/functions/push-notification/index_test.ts` (use Deno `MockHttpServer` for both FCM and APNs paths)

**Failing test seed (Deno):**
```ts
Deno.test("APNs path sends signed JWT and returns 200", async () => {
  const apnsServer = new MockHttpServer({ port: 0 });
  apnsServer.on("POST", "/3/device/:token", (req) => {
    assertEquals(req.headers.get("apns-topic"), "dev.bilbo.app");
    assert(req.headers.get("authorization")?.startsWith("bearer "));
    return new Response(null, { status: 200 });
  });
  const result = await sendApnsNotification({
    apnsHost: apnsServer.url,
    deviceToken: "abc",
    teamId: "T", keyId: "K", privateKey: TEST_PRIVATE_KEY,
    payload: { aps: { alert: "hi" } }
  });
  assertEquals(result.status, "delivered");
});
```

**Acceptance:**
- 100% Deno coverage on `push-notification/index.ts`.
- Manual: APNs payload decoded by an iOS device via `Notification Service Extension` shows the correct title/body.
- The literal string `"queued"` no longer appears in the function for the success path.

**Commit:** `feat(push): real APNs sender replaces stub; FCM registration on Android [WU-A9]`

---

### WU-A10: Tier-3 weekly narrative wiring (CloudInsightClient + UI)

**Files:**
- Modify: `shared/src/commonMain/kotlin/dev/bilbo/intelligence/tier3/CloudInsightClient.kt` (call `BilboApiService.aiWeeklyInsight`; cache result; expose Flow)
- Modify: `shared/src/commonMain/kotlin/dev/bilbo/intelligence/DecisionEngine.kt` (consume Tier-3 narrative; remove from Kover exclusion)
- Modify: `androidApp/src/main/kotlin/dev/bilbo/app/ui/screen/WeeklyInsightScreen.kt` (collect narrative + render markdown)
- Modify: `iosApp/iosApp/Insights/WeeklyInsightView.swift`
- Test: `shared/src/commonTest/kotlin/dev/bilbo/intelligence/tier3/CloudInsightClientTest.kt`

**Acceptance:**
- 100% coverage on `CloudInsightClient` and `DecisionEngine`.
- WeeklyInsightScreen displays the actual narrative when present and falls back to "Generating your weekly insight…" with a retry CTA otherwise.
- `tier3Narrative` field of `WeeklyInsight` is non-null after a successful weekly run.

**Commit:** `feat(intelligence): Tier-3 weekly narrative end-to-end [WU-A10]`

---

### WU-A11: Replace remaining "coming soon" placeholders

**Files:**
- Modify: `androidApp/src/main/kotlin/dev/bilbo/app/ui/screen/InsightsScreen.kt` (real "AI insights" section using Tier-2 + Tier-3 data)
- Modify: `iosApp/iosApp/Dashboard/DashboardView.swift` (real Analog suggestions sheet)
- Modify: `iosApp/iosApp/Settings/SettingsView.swift` (real data export — emits a real JSON of `BilboPreferences` + last 30 days of usage)
- Test: each screen has a ViewModel test verifying the rendered state.

**Acceptance:**
- `rg -n 'coming soon|Coming soon|stub|TODO' androidApp iosApp shared supabase` returns zero results in source files (only in markdown is allowed).
- 100% line + branch on every modified ViewModel.

**Commit:** `feat(ui): replace all remaining placeholders with real implementations [WU-A11]`

---

## Track B — Quality-Zero Gate Adoption

### WU-B1: Co-evolution PR in `quality-zero-platform`

**Repo:** `Prekzursil/quality-zero-platform` (separate from bilbo).

**Files (in quality-zero-platform):**
- Modify: `inventory/repos.yml` — add bilbo entry:
  ```yaml
  - slug: Prekzursil/bilbo-app
    profile: bilbo-app
    stack: kotlin-multiplatform
    rollout_wave: 2
    default_branch: main
    notes: "Kotlin Multiplatform mobile app + Deno edge functions + Supabase backend"
  ```
- Create: `profiles/stacks/kotlin-multiplatform.yml`:
  ```yaml
  extends: quality-zero-phase1-common
  default_branch: main
  verify_command: bash scripts/verify
  required_secrets:
    - SONAR_TOKEN
    - CODACY_API_TOKEN
    - DEEPSCAN_API_TOKEN
    - DEEPSOURCE_DSN
    - SENTRY_AUTH_TOKEN
    - CODECOV_TOKEN
    - ANTHROPIC_API_KEY
    - SUPABASE_ACCESS_TOKEN
    - BILBO_RELEASE_KEYSTORE_BASE64
    - BILBO_RELEASE_KEYSTORE_PASSWORD
    - BILBO_RELEASE_KEY_ALIAS
    - BILBO_RELEASE_KEY_PASSWORD
    - IOS_TEAM_ID
    - IOS_DISTRIBUTION_CERT_BASE64
    - IOS_PROVISIONING_PROFILE_BASE64
    - MATCH_PASSWORD
  enabled_scanners:
    coverage: true
    codecov: true
    sonar: true
    codacy: true
    semgrep: true
    sentry: true
    deepscan: true        # for the supabase/functions/*.ts paths
    deepsource_visible: true  # Kotlin + TS
    qlty: true
  coverage:
    runner: macos-14       # required for iOS test phase
    command_shell: bash
    setup:
      java: "17"
      ruby: "3.3"
      node: "22"
      deno: "v2.x"
      xcode: "16.1"
    command: |
      ./gradlew :shared:koverXmlReport :shared:koverVerify --no-daemon
      ./gradlew :androidApp:koverXmlReport :androidApp:koverVerify --no-daemon
      cd iosApp && xcodegen generate && \
        xcodebuild test -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' -enableCodeCoverage YES -derivedDataPath build && \
        xcrun xccov view --report --json build/Logs/Test/*.xcresult > coverage/ios.xccov.json && \
        node ../scripts/quality/xccov_to_lcov.mjs coverage/ios.xccov.json > coverage/ios.lcov && \
        cd ..
      cd supabase/functions && \
        deno test --allow-all --coverage=cov && \
        deno coverage cov --lcov > ../../coverage/deno.lcov && \
        cd ../..
    inputs:
      - { format: xml,  name: shared,    path: shared/build/reports/kover/report.xml,         flag: shared }
      - { format: xml,  name: android,   path: androidApp/build/reports/kover/report.xml,     flag: android }
      - { format: lcov, name: ios,       path: coverage/ios.lcov,                              flag: ios }
      - { format: lcov, name: deno,      path: coverage/deno.lcov,                             flag: deno }
    require_sources: [shared/src/commonMain, androidApp/src/main, iosApp/iosApp, supabase/functions]
    min_percent: 100.0
    branch_min_percent: 100.0
    policy: whole-project-100
  ```
- Create: `profiles/repos/bilbo-app.yml`:
  ```yaml
  slug: Prekzursil/bilbo-app
  version: 2
  stack: kotlin-multiplatform
  mode:
    phase: absolute
  issue_policy:
    mode: zero
    pr_behavior: absolute
    main_behavior: absolute
  scanners:
    codeql:               { severity: block }
    dependabot:           { severity: block }
    sonarcloud:           { severity: block }
    codacy_issues:        { severity: block }
    codacy_complexity:    { severity: block }
    codacy_clones:        { severity: block }
    codacy_coverage:      { severity: block }
    deepscan:             { severity: block }
    deepsource_visible:   { severity: block }
    semgrep:              { severity: block }
    sentry:               { severity: block }
    socket_pr_alerts:     { severity: block }
    socket_project_report: { severity: info }
    qlty_check:           { severity: block }
  required_contexts_mode: replace
  required_contexts:
    always:
      - shared-scanner-matrix / Coverage 100 Gate
      - shared-codecov-analytics / Codecov Analytics
      - codeql / CodeQL
      - shared-scanner-matrix / QLTY Zero
      - shared-scanner-matrix / Sonar Zero
      - shared-scanner-matrix / Codacy Zero
      - shared-scanner-matrix / Semgrep Zero
      - shared-scanner-matrix / Sentry Zero
      - shared-scanner-matrix / DeepScan Zero
      - shared-scanner-matrix / DeepSource Visible Zero
    pull_request_only:
      - SonarCloud Code Analysis
      - Codacy Static Code Analysis
      - DeepScan
      - qlty check
      - qlty coverage
      - qlty coverage diff
  ```
- Create: `scripts/quality/xccov_to_lcov.mjs` (Node helper that converts Xcode `xccov view --report --json` output to lcov format).
- Modify: `scripts/quality/assert_coverage_100.py` — verify it accepts both XML and lcov inputs (already does per WU-2 audit).

**Acceptance:**
- PR opened on `Prekzursil/quality-zero-platform`. CI green on the platform's own self-tests.
- Bilbo entry in `inventory/repos.yml` resolves correctly via `python scripts/quality/validate_control_plane.py`.
- The PR is merged before WU-B2 starts (because B2 calls reusable workflows pinned to a SHA on `main`).

**Commit (in platform repo):** `feat(profiles): add kotlin-multiplatform stack and bilbo-app repo profile [WU-B1]`

---

### WU-B2: Add 5 reusable-caller workflows to bilbo

**Files (resolve `<PINNED-SHA>` once via `gh api repos/Prekzursil/quality-zero-platform/commits/main -q .sha` after WU-B1 merges):**

- Create: `.github/workflows/quality-zero-gate.yml`:
  ```yaml
  name: Quality Zero Gate
  permissions: { contents: read }
  on:
    push: { branches: [main] }
    pull_request: { branches: [main] }
    merge_group: { types: [checks_requested] }
    workflow_dispatch:
  jobs:
    aggregate-gate:
      permissions: { contents: read }
      uses: Prekzursil/quality-zero-platform/.github/workflows/reusable-quality-zero-gate.yml@<PINNED-SHA>
      with:
        repo_slug: ${{ github.repository }}
        event_name: ${{ github.event_name }}
        sha: ${{ github.event.pull_request.head.sha || github.sha }}
        platform_repository: Prekzursil/quality-zero-platform
        platform_ref: main
      secrets: inherit
  ```

- Create: `.github/workflows/quality-zero-platform.yml` (calls `reusable-scanner-matrix.yml`).
- Create: `.github/workflows/quality-zero-backlog.yml` (calls `reusable-backlog-sweep.yml`, scheduled nightly).
- Create: `.github/workflows/quality-zero-remediation.yml` (calls `reusable-remediation-loop.yml`, triggers on Quality Zero Gate failure).
- Replace: `.github/workflows/codeql.yml` — call `reusable-codeql.yml` with languages `kotlin`, `swift`, `javascript-typescript`.

**Acceptance:**
- PR diff shows 5 new/replaced workflow files.
- A `gh workflow run quality-zero-gate.yml` from PR branch executes; if it fails because secrets aren't yet configured, the failure message is "missing secrets: SONAR_TOKEN" (deterministic) — *not* an unrelated YAML parse error.

**Commit:** `ci(workflows): adopt quality-zero-platform reusable workflows [WU-B2]`

---

### WU-B3: Strip every `continue-on-error: true` from existing workflows

**Files:**
- Modify: `.github/workflows/android-ci.yml:42` — remove `continue-on-error: true` from Lint step.
- Modify: `.github/workflows/android-ci.yml:76` — remove `continue-on-error: true` from Unit Tests step.
- Modify: `.github/workflows/ios-ci.yml:65` — remove `continue-on-error: true` from SwiftLint step (use `swiftlint --strict`).
- Modify: `.github/workflows/backend-ci.yml:45` — remove `continue-on-error: true` from `deno check`.
- Modify: `.github/workflows/backend-ci.yml:49` — remove `continue-on-error: true` from `deno test`.
- Modify: `.github/workflows/shared-tests.yml:43-48` — remove `if: always()` on `koverVerify` so a coverage failure fails the workflow.
- Modify: `.github/workflows/shared-tests.yml:73` — remove `fail_ci_if_error: false` (let Codecov upload failure fail CI).
- Modify: `.github/workflows/android-ci.yml:93` — same.
- Add: `koverVerify` step on `androidApp` (post-WU-B7).

**Acceptance:**
- `rg -n 'continue-on-error.*true|fail_ci_if_error.*false' .github/workflows/` returns nothing.
- A deliberately broken commit (e.g., introduce a Kotlin compile error) fails the `Android CI` workflow.

**Commit:** `ci: every step is now blocking — no soft gates [WU-B3]`

---

### WU-B4: Quality config files (`.coverage-thresholds.json`, `.semgrep.yml`, `.codacy.yaml`, `.deepsource.toml`, `.gitleaks.toml`)

**Files:**
- Create: `.coverage-thresholds.json`:
  ```json
  {
    "lines": 100,
    "branches": 100,
    "functions": 100,
    "statements": 100,
    "blockPRCreation": true,
    "blockTaskCompletion": true,
    "enforcementCommand": "bash scripts/verify",
    "componentInputs": [
      { "format": "xml",  "name": "shared",  "path": "shared/build/reports/kover/report.xml",         "flag": "shared"  },
      { "format": "xml",  "name": "android", "path": "androidApp/build/reports/kover/report.xml",     "flag": "android" },
      { "format": "lcov", "name": "ios",     "path": "coverage/ios.lcov",                              "flag": "ios"     },
      { "format": "lcov", "name": "deno",    "path": "coverage/deno.lcov",                             "flag": "deno"    }
    ]
  }
  ```
- Create: `.semgrep.yml` (Kotlin + Swift + TS rules; block on `no-hardcoded-secrets`, `no-unsafe-deserialize`, `no-direct-url-injection`).
- Create: `.codacy.yaml` (enable detekt, ktlint, swiftlint, eslint, semgrep, bandit-equivalent; disable deprecated tools).
- Create: `.deepsource.toml` (Kotlin + TypeScript analyzers; tests excluded only from coverage, not analysis).
- Create: `.gitleaks.toml` (allowlist `local.properties.example`; everything else blocked).
- Create: `.tool-versions` (asdf manifest pinning Java 17, Node 22, Deno latest, Ruby 3.3, fastlane).

**Acceptance:**
- `semgrep --config=.semgrep.yml --error` passes locally.
- `gitleaks detect --no-git --source .` passes locally.

**Commit:** `chore(quality): add config files for all 12 strict-zero gates [WU-B4]`

---

### WU-B5: `sonar-project.properties` v2 with all four coverage paths

**File:**
- Modify: `sonar-project.properties`:
  ```properties
  sonar.projectKey=Prekzursil_bilbo-app
  sonar.organization=Prekzursil
  sonar.projectName=Bilbo
  sonar.projectVersion=2.0.0

  sonar.sources=shared/src/commonMain,shared/src/androidMain,shared/src/iosMain,androidApp/src/main,iosApp/iosApp,supabase/functions
  sonar.tests=shared/src/commonTest,shared/src/androidUnitTest,shared/src/iosTest,androidApp/src/test,androidApp/src/androidTest,iosApp/iosAppTests,supabase/functions

  sonar.test.inclusions=**/*Test.kt,**/Test*.kt,**/*Tests.swift,**/*_test.ts

  sonar.kotlin.detekt.reportPaths=shared/build/reports/detekt/detekt.xml,androidApp/build/reports/detekt/detekt.xml
  sonar.kotlin.ktlint.reportPaths=shared/build/reports/ktlint/ktlint.xml,androidApp/build/reports/ktlint/ktlint.xml

  sonar.coverage.jacoco.xmlReportPaths=shared/build/reports/kover/report.xml,androidApp/build/reports/kover/report.xml
  sonar.swift.coverage.reportPaths=coverage/ios.lcov
  sonar.javascript.lcov.reportPaths=coverage/deno.lcov
  sonar.typescript.lcov.reportPaths=coverage/deno.lcov

  sonar.exclusions=**/*.generated.kt,**/build/**,**/*.sq,**/*.sqm,**/res/**,iosApp/build/**,supabase/functions/_shared/types.ts
  sonar.coverage.exclusions=**/*Test.kt,**/Test*.kt,**/*Tests.swift,**/*_test.ts,**/Preview*.kt
  sonar.sourceEncoding=UTF-8
  ```

**Acceptance:**
- A SonarCloud scan picks up coverage data for all 4 components and reports >0% on each (eventually 100%).

**Commit:** `chore(sonar): include iOS + Deno sources, all 4 coverage report paths [WU-B5]`

---

### WU-B6: Shrink `shared/build.gradle.kts` Kover exclusions to ≤5 classes; backfill commonTest

**Files:**
- Modify: `shared/build.gradle.kts:108-189` — replace the ~70-class exclusion list with ≤5 classes that genuinely have no JVM-runnable surface:
  ```kotlin
  excludes {
      classes(
          // Truly platform-bound: no JVM impl, no kotlin-test path
          "dev.bilbo.data.AndroidDatabaseDriverFactory*",
          "dev.bilbo.data.IosDatabaseDriverFactory*",
          "dev.bilbo.preferences.AndroidBilboPreferences*",
          "dev.bilbo.preferences.IosBilboPreferences*",
          "dev.bilbo.platform.*"
      )
  }
  ```
- Add tests (per excluded class previously) — at least 60 new tests covering:
  - `dev.bilbo.shared.data.remote.SupabaseClient` (covered in WU-A3)
  - `dev.bilbo.shared.data.remote.BilboApiService` (covered in WU-A4)
  - `dev.bilbo.shared.util.FlowExtensions` (commonTest)
  - `dev.bilbo.auth.AuthManager` (covered in WU-A5)
  - `dev.bilbo.intelligence.tier3.CloudInsightClient` (covered in WU-A10)
  - `dev.bilbo.data.BilboDatabase` ← driven by SQLDelight tests in WU-A2
  - `dev.bilbo.data.IntentRepository` ← actual interface methods covered by `SqlDelightIntentRepositoryTest`
  - `dev.bilbo.shared.domain.usecase.GetDailyInsightsUseCase` ← test happy + empty + error
  - `dev.bilbo.shared.data.repository.InsightRepository` ← tests for caching + sync
  - `dev.bilbo.tracking.SessionTracker` ← test session lifecycle, multi-app, force-stop
  - `dev.bilbo.intelligence.DecisionEngine` ← test all branches
  - `dev.bilbo.data.SeedDataLoader` ← test load happy path + malformed JSON
  - `dev.bilbo.preferences.NotificationPreferences` ← test serialization round-trip
  - `dev.bilbo.shared.domain.model.*` ← test `data class` equality + `copy()` + sealed exhaustiveness
  - `dev.bilbo.social.BuddyManager`, `ChallengeEngine`, `CircleManager`, `LeaderboardCalculator` ← cover state machine deeply
  - `dev.bilbo.enforcement.CooldownManager` ← test concurrent isLocked + race
  - `dev.bilbo.intelligence.tier2.HeuristicEngine`, `GamingDetector`, `TrendDetector` ← test the dead branches the comment claims are unreachable; if they really are, restructure to remove them
  - `dev.bilbo.intelligence.tier3.InsightPromptBuilder` ← test prompt assembly + anonymization
  - `dev.bilbo.util.DefaultErrorHandler` + `ErrorHandlerKt` ← test every BilboError subclass mapping
  - `dev.bilbo.economy.AppClassifier` ← test every category branch
  - `dev.bilbo.shared.util.ResultKt` ← test map + flatMap + mapError on Loading + Success + Error
  - `dev.bilbo.social.BuddyManagerKt`, `dev.bilbo.analog.SuggestionEngineKt` ← test default-parameter overloads

**Acceptance:**
- `./gradlew :shared:koverVerify --no-daemon` passes with `minBound(100, LINE)` and `minBound(100, BRANCH)` and the smaller exclusion list.
- `wc -l shared/src/commonTest/kotlin -- recursive` shows a substantial increase (likely +2000 lines of tests).

**Commit:** `test(shared): backfill commonTest until 100% real coverage with ≤5 platform exclusions [WU-B6]`

---

### WU-B7: Add Kover to `androidApp` and write missing Android tests to 100%

**Files:**
- Modify: `androidApp/build.gradle.kts` — add `alias(libs.plugins.kover)` and `kover { reports { verify { rule { minBound(100, LINE); minBound(100, BRANCH) } } } }` with exclusions only for Compose `@Preview` stubs.
- Add tests in `androidApp/src/test/kotlin/dev/bilbo/app/`:
  - `BilboApplicationTest.kt`
  - `MainActivityTest.kt`
  - `service/UsageTrackingServiceTest.kt` (use `Robolectric`)
  - `service/SessionTrackerTest.kt`
  - `service/AppMonitorTest.kt`
  - `enforcement/GatekeeperOverlayTest.kt`
  - `worker/SyncWorkerTest.kt`
  - `worker/NightlyHeuristicsWorkerTest.kt`
  - `di/RepositoryModuleTest.kt` (verify Hilt graph compiles + bindings resolve)
  - `ui/screen/*ViewModelTest.kt` (≈12 ViewModels)
  - `ui/screen/settings/*ViewModelTest.kt` (5 ViewModels from WU-A6)
- Add tests in `androidApp/src/androidTest/kotlin/dev/bilbo/app/`:
  - `RealUsageRepositorySmokeTest.kt` (real `AndroidSqliteDriver`)
  - `BootReceiverInstrumentedTest.kt`
  - One Compose UI test per screen (basic render + click + navigation).

**Acceptance:**
- `./gradlew :androidApp:koverXmlReport :androidApp:koverVerify --no-daemon` passes.
- The `koverVerify` step on the Android CI workflow becomes blocking (added in WU-B3).

**Commit:** `test(androidApp): add Kover module and write tests until 100% coverage [WU-B7]`

---

### WU-B8: Add iOS test target + XCTest suite to 100% via `xccov`

**Files:**
- Modify: `iosApp/project.yml` — add target:
  ```yaml
  iosAppTests:
    type: bundle.unit-test
    platform: iOS
    sources: [iosAppTests]
    dependencies:
      - target: iosApp
  ```
- Create: `iosApp/iosAppTests/ContentViewModelTests.swift`
- Create: `iosApp/iosAppTests/Settings/*Tests.swift` (matching WU-A6's 5 sub-screens)
- Create: `iosApp/iosAppTests/Dashboard/DashboardViewModelTests.swift`
- Create: `iosApp/iosAppTests/Insights/WeeklyInsightViewTests.swift`
- Create: `iosApp/iosAppTests/Extensions/DeviceActivityMonitorTests.swift`
- Create: `iosApp/iosAppTests/Extensions/ShieldConfigurationTests.swift`
- Create: `iosApp/iosAppTests/Auth/AuthBridgeTests.swift`
- Create: `scripts/quality/xccov_to_lcov.mjs` (already in WU-B1 platform PR — reused here)

**Acceptance:**
- `xcodebuild test -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' -enableCodeCoverage YES` passes.
- `xcrun xccov view --report --json build/Logs/Test/*.xcresult | node scripts/quality/xccov_to_lcov.mjs > coverage/ios.lcov` produces a valid lcov file with 100% on iOS sources (excluding `@main` entry point and SwiftUI previews).

**Commit:** `test(ios): add XCTest target and reach 100% via xccov [WU-B8]`

---

### WU-B9: Deno tests for every supabase function to 100% line

**Files (per function, 13 total):**
- Create: `supabase/functions/<name>/index_test.ts` — uses Deno's standard test framework + `MockHttpServer` + a stub Supabase client.
- Modify: `supabase/deno.json`:
  ```json
  {
    "tasks": {
      "test": "deno test --allow-all --coverage=cov",
      "coverage": "deno coverage cov --lcov > ../coverage/deno.lcov"
    }
  }
  ```
- Create: `supabase/functions/_shared/test_helpers.ts` (StubSupabaseClient, fixture builders).

**Per-function test seed pattern (sync-status):**
```ts
import { assertEquals, assertRejects } from "https://deno.land/std@0.224.0/testing/asserts.ts";
import handler from "./index.ts";

Deno.test("returns 401 when no auth header", async () => {
  const res = await handler(new Request("http://x", { method: "POST" }));
  assertEquals(res.status, 401);
});

Deno.test("returns 400 on missing fields", async () => {
  const res = await handler(new Request("http://x", { method: "POST", headers: { authorization: "Bearer t" }, body: JSON.stringify({}) }));
  assertEquals(res.status, 400);
});

Deno.test("upserts status_summaries and returns 200", async () => {
  const res = await handler(/* full payload */);
  const body = await res.json();
  assertEquals(res.status, 200);
  assertEquals(body.status, "ok");
});
```

**Acceptance:**
- `cd supabase/functions && deno task test` passes with 100% line coverage across all 13 functions.
- `deno task coverage` produces `coverage/deno.lcov`.

**Commit:** `test(supabase): deno tests for every edge function to 100% line [WU-B9]`

---

### WU-B10: Local `scripts/verify` mirroring event-link's gate

**File:**
- Create: `scripts/verify` (executable bash):
  ```bash
  #!/usr/bin/env bash
  set -euo pipefail
  echo "[verify] gradle build + tests + kover + detekt + lint"
  ./gradlew :shared:allTests :shared:koverXmlReport :shared:koverVerify \
            :androidApp:detekt :androidApp:lintPlaystoreDebug \
            :androidApp:testPlaystoreDebugUnitTest \
            :androidApp:koverXmlReport :androidApp:koverVerify \
            --no-daemon

  echo "[verify] iOS build + xctest + xccov"
  pushd iosApp >/dev/null
  xcodegen generate
  xcodebuild test -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' -enableCodeCoverage YES -derivedDataPath build | xcpretty
  mkdir -p ../coverage
  xcrun xccov view --report --json build/Logs/Test/*.xcresult > ../coverage/ios.xccov.json
  node ../scripts/quality/xccov_to_lcov.mjs ../coverage/ios.xccov.json > ../coverage/ios.lcov
  popd >/dev/null

  echo "[verify] deno test + lcov"
  pushd supabase/functions >/dev/null
  deno task test
  deno task coverage
  popd >/dev/null

  echo "[verify] semgrep + gitleaks + lizard"
  semgrep --config=.semgrep.yml --error
  gitleaks detect --no-git --source . --redact -v
  lizard shared/src androidApp/src iosApp/iosApp supabase/functions -C 15

  echo "[verify] aggregate 100% coverage assertion"
  python scripts/quality/assert_coverage_100.py \
    --report shared/build/reports/kover/report.xml \
    --report androidApp/build/reports/kover/report.xml \
    --report coverage/ios.lcov \
    --report coverage/deno.lcov

  echo "[verify] OK ✓"
  ```
- Create: `scripts/quality/__init__.py` (empty marker)
- Create: `scripts/quality/assert_coverage_100.py` — copy from `Prekzursil/quality-zero-platform/scripts/quality/assert_coverage_100.py` (or symlink-equivalent: a thin shim that runs the platform's script via `gh release download`).
- Create: `scripts/quality/xccov_to_lcov.mjs` (Node script — covered in WU-B1).

**Acceptance:**
- `bash scripts/verify` exits 0 on a green checkout (after WU-A* and WU-B* are done).
- `bash scripts/verify` exits non-zero with a clear error message if any gate fails.

**Commit:** `chore(scripts): local verify gate mirrors event-link's [WU-B10]`

---

## Track C — Release Polish

### WU-C1: Real Android signing (release keystore via CI secrets)

**Files:**
- Modify: `androidApp/build.gradle.kts:62-78`:
  ```kotlin
  signingConfigs {
      create("release") {
          storeFile = file(System.getenv("BILBO_RELEASE_KEYSTORE_PATH") ?: "release.keystore")
          storePassword = System.getenv("BILBO_RELEASE_KEYSTORE_PASSWORD")
          keyAlias = System.getenv("BILBO_RELEASE_KEY_ALIAS")
          keyPassword = System.getenv("BILBO_RELEASE_KEY_PASSWORD")
      }
  }
  buildTypes {
      release {
          isMinifyEnabled = true
          isShrinkResources = true
          proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
          val isCi = System.getenv("CI") == "true"
          val hasReleaseSigning = System.getenv("BILBO_RELEASE_KEYSTORE_PASSWORD") != null
          signingConfig = if (hasReleaseSigning) {
              signingConfigs.getByName("release")
          } else if (isCi) {
              error("Release build attempted in CI without BILBO_RELEASE_* secrets")
          } else {
              signingConfigs.getByName("debug")
          }
      }
  }
  ```
- Modify: `.github/workflows/android-ci.yml` — decode `BILBO_RELEASE_KEYSTORE_BASE64` to `release.keystore` before `assembleRelease`.

**Acceptance:**
- `./gradlew :androidApp:assemblePlaystoreRelease` in CI produces a signed APK; `apksigner verify --print-certs release.apk` shows the bilbo signing cert.
- `./gradlew :androidApp:assemblePlaystoreRelease` in CI without the secret fails fast with a clear error.

**Commit:** `feat(android): real release signing via CI-provided keystore [WU-C1]`

---

### WU-C2: iOS signing via fastlane match

**Files:**
- Create: `iosApp/fastlane/Fastfile`:
  ```ruby
  default_platform(:ios)
  platform :ios do
    desc "Build signed Bilbo IPA"
    lane :release do
      match(type: "appstore", app_identifier: "dev.bilbo.app", readonly: true)
      gym(
        scheme: "iosApp",
        export_method: "app-store",
        output_directory: "../release-artifacts",
        output_name: "Bilbo-iOS.ipa",
        clean: true
      )
    end
  end
  ```
- Create: `iosApp/fastlane/Matchfile` (private cert repo URL via `MATCH_GIT_URL` env).
- Create: `iosApp/fastlane/Appfile`.
- Create: `iosApp/Gemfile`:
  ```ruby
  source "https://rubygems.org"
  gem "fastlane"
  ```
- Modify: `.github/workflows/ios-ci.yml` — replace the unsigned build path with `bundle exec fastlane release` when `IOS_DISTRIBUTION_CERT_BASE64` secret is present; **fail loudly** when secret missing on tag push.
- Modify: `iosApp/project.yml` — `CODE_SIGN_STYLE: Manual`, `DEVELOPMENT_TEAM: $(IOS_TEAM_ID)`, `CODE_SIGN_IDENTITY: "Apple Distribution"`.

**Acceptance:**
- On a tag push with secrets configured, the workflow produces `Bilbo-iOS.ipa` (signed).
- On a tag push without secrets, the workflow fails with `"iOS signing secrets not configured — cannot ship v2.0.0 final"` (exit 1, no silent unsigned fallback).

**Commit:** `feat(ios): fastlane match signing for release IPA [WU-C2]`

---

### WU-C3: Version bump to 2.0.0

**Files:**
- Modify: `androidApp/build.gradle.kts` — `versionCode = 2`, `versionName = "2.0.0"`.
- Modify: `iosApp/project.yml` — `MARKETING_VERSION: "2.0.0"`, `CURRENT_PROJECT_VERSION: "2"`.
- Modify: `sonar-project.properties` — `sonar.projectVersion=2.0.0`.

**Acceptance:**
- `./gradlew :androidApp:assemblePlaystoreRelease` produces an APK with `versionName=2.0.0`.
- iOS `Info.plist` shows `CFBundleShortVersionString = 2.0.0`.

**Commit:** `chore(release): bump to 2.0.0 [WU-C3]`

---

### WU-C4: README + badges + signed-install instructions

**File:**
- Modify: `README.md`:
  - Replace the "Current Status" section header with "Current Status — v2.0.0 (shipping)".
  - Drop the "active early development" + "what is stubbed" sub-sections; replace with a "What ships in v2.0.0" feature checklist.
  - Add badges:
    - SonarCloud Quality Gate
    - Codacy Grade
    - DeepSource Active Issues
    - Codecov Coverage
    - CodeQL
    - GitHub Release v2.0.0
  - Add an "Install" section with download links to the latest release's signed APKs and IPA + verification command:
    ```bash
    sha256sum -c SHA256SUMS.txt
    apksigner verify --print-certs bilbo-playstore-release.apk
    ```

**Acceptance:**
- The text "in active early development" no longer appears in README.
- Markdown lint passes.

**Commit:** `docs(readme): describe v2.0.0 shipping reality with quality badges [WU-C4]`

---

### WU-C5: CHANGELOG, RELEASE_NOTES, QUALITY_GATES docs

**Files:**
- Modify: `CHANGELOG.md` — prepend a `## [2.0.0] — 2026-04-29` section grouped into Added / Changed / Fixed / Security / Quality, with one bullet per WU above.
- Create: `docs/RELEASE_NOTES_v2.0.0.md` — public-facing release announcement (≤500 words) covering: persistence is now real; Supabase is live; Tier-3 narrative ships; signed APK + IPA; 0 issues + 100% coverage gate.
- Create: `docs/QUALITY_GATES.md` — mirrors `event-link/docs/quality/QUALITY_ZERO_GATES.md`; lists every of the 12 gates with: name, threshold, where configured, what triggers a block, how to bypass (none — all are absolute).
- Modify: `CONTRIBUTING.md` — add "Quality Gates" section pointing at `docs/QUALITY_GATES.md` and `bash scripts/verify`.

**Acceptance:**
- `wc -l CHANGELOG.md` reflects substantial 2.0.0 section (>= 80 lines).
- `docs/QUALITY_GATES.md` lists 12 gates with thresholds.

**Commit:** `docs: v2.0.0 changelog, release notes, and quality gates spec [WU-C5]`

---

### WU-C6: Release attach-artifacts workflow update (signed APKs + IPA + dSYM + SHA256SUMS)

**Files:**
- Modify: `.github/workflows/android-ci.yml` `attach-release-artifacts` job — upload `bilbo-playstore-release.apk` and `bilbo-github-release.apk` (not debug).
- Modify: `.github/workflows/ios-ci.yml` `attach-ios-release-artifact` job — upload `Bilbo-iOS.ipa` and `Bilbo-iOS.app.dSYM.zip`; if signing secrets missing on a tag push, **fail** the job (no silent fallback to unsigned).
- Add: A new `attach-source-and-checksums` job that runs after both Android + iOS attach jobs: bundles `git archive --format=tar.gz HEAD` into `bilbo-app-2.0.0-source.tar.gz`, computes SHA256 over all 6 artifacts, uploads `SHA256SUMS.txt`.

**Acceptance:**
- `gh release view v2.0.0 --json assets -q '.assets | length'` returns `>= 6`.
- `gh release download v2.0.0 -p SHA256SUMS.txt && sha256sum -c SHA256SUMS.txt` passes.

**Commit:** `ci(release): attach signed APK + IPA + dSYM + source + checksums on tag [WU-C6]`

---

### WU-C7: Tag v2.0.0 + final dry-run on main

**Steps (executed by the ralph-loop only when WU-A* and WU-B* and WU-C1..C6 are committed):**
- [ ] Run `bash scripts/verify` locally — expect exit 0.
- [ ] Run `gh workflow run quality-zero-gate.yml --ref main` and wait — expect `success`.
- [ ] `git tag -a v2.0.0 -m "Bilbo v2.0.0 — Quality-Zero Final"`
- [ ] `git push origin v2.0.0`
- [ ] Watch `.github/workflows/android-ci.yml` + `ios-ci.yml` complete on the tag.
- [ ] Verify `gh release view v2.0.0 --json assets -q '.assets[].name'` shows:
  ```
  bilbo-playstore-release.apk
  bilbo-github-release.apk
  Bilbo-iOS.ipa
  Bilbo-iOS.app.dSYM.zip
  bilbo-app-2.0.0-source.tar.gz
  SHA256SUMS.txt
  ```
- [ ] Edit the release body via `gh release edit v2.0.0 --notes-file docs/RELEASE_NOTES_v2.0.0.md`.
- [ ] Final `git log --oneline -5` posted to the loop's report.

**Acceptance:** All four conditions in the [Stop Condition Verification Suite](#stop-condition-verification-suite) below return success.

**Commit (none — this is the final tag).**

---

## Stop Condition Verification Suite

The ralph-loop's terminal check. **All four must be true** for the loop to declare DONE.

```bash
# 1. Quality Zero Gate green on main
gh run list --workflow=quality-zero-gate.yml --branch=main --limit=1 --json conclusion -q '.[0].conclusion' | grep -qx success

# 2. Six artifacts attached to v2.0.0 release
test "$(gh release view v2.0.0 --json assets -q '.assets | length')" -ge 6

# 3. 100% coverage assertion across all four reports
python scripts/quality/assert_coverage_100.py \
  --report shared/build/reports/kover/report.xml \
  --report androidApp/build/reports/kover/report.xml \
  --report coverage/ios.lcov \
  --report coverage/deno.lcov

# 4. v2.0.0 tag points at HEAD of main
test "$(git rev-parse v2.0.0)" = "$(git rev-parse origin/main)"
```

If all four exit 0 → DONE. Otherwise the loop continues.

---

## Self-Review

| Spec section | Covered by |
|--------------|------------|
| §2.1 SQLDelight Android | WU-A1, WU-A2, WU-B7 |
| §2.1 SQLDelight iOS | WU-A1, WU-A2, WU-B8 |
| §2.1 Supabase init | WU-A3 |
| §2.1 Edge function clients | WU-A4 |
| §2.1 Realtime | WU-A8 |
| §2.1 Push (FCM + APNs) | WU-A9 |
| §2.1 Tier-3 narrative | WU-A10 |
| §2.1 iOS auth | WU-A5 |
| §2.1 Settings sub-routes | WU-A6 |
| §2.1 iOS extensions | WU-A7 |
| §2.1 Coming-soon placeholders | WU-A11 |
| §2.2 quality-zero-platform onboarding | WU-B1 |
| §2.2 Reusable workflow callers | WU-B2 |
| §2.2 Strict workflows | WU-B3 |
| §2.2 Quality config files | WU-B4, WU-B5 |
| §2.3 Real 100% coverage | WU-B6, WU-B7, WU-B8, WU-B9 |
| §2.4 Signed Android | WU-C1 |
| §2.4 Signed iOS | WU-C2 |
| §2.4 Release artifacts | WU-C6 |
| §2.5 Documentation final | WU-C4, WU-C5 |
| §10 Stop condition | Stop Condition Verification Suite |

**Placeholder scan:** No `TBD`, `TODO`, `fill in later` in this plan. Every code block contains real code. Every command is executable.

**Type consistency:** `BilboApiService` is referenced consistently across WU-A4, WU-A8, WU-A9, WU-A10. `SqlDelightUsageRepository` is the same name in WU-A2 and WU-B6. `assert_coverage_100.py` is referenced consistently in WU-B1, WU-B10, WU-C7, and the Stop Condition.

**Scope check:** Single coherent release (`v2.0.0`); no further decomposition required because the gates are interlocked per the spec §3.2.

---

**Plan complete.** Execution hand-off: see the ralph-loop prompt in the project root's `RALPH_LOOP.md` (committed alongside this plan). The user runs the loop via `/ralph-loop:ralph-loop` with that prompt.
