# Changelog

All notable changes to Bilbo are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and Bilbo adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] — 2026-04-17

First release that actually ships installable artifacts. Addresses the gaps
reported against v1.0.0 (empty release, navigation placeholders, legacy
service, onboarding always showing) and fills in the data path from the
foreground usage tracker to the Dashboard UI.

### Added
- **Release artifacts attached automatically on tag push.** Both
  `bilbo-playstore-debug.apk` and `bilbo-github-debug.apk` are built in CI
  and uploaded to the GitHub release, alongside an unsigned
  `Bilbo-iOS-unsigned.zip` simulator build. Tag strategy: pushing `vX.Y.Z`
  triggers Android CI + iOS CI, which then run the new
  `attach-release-artifacts` / `attach-ios-release-artifact` jobs.
- **`DashboardViewModel`** (Hilt-injected) observes `UsageRepository.observeAll()`,
  filters to today in the user's local timezone, aggregates per-package
  totals, and resolves category + label via `AppProfileRepository`.
  Includes derived `formattedTotal` and `goalDeltaCopy` helpers and a
  `refresh()` entry point for pull-to-refresh.
- **`DashboardScreen`** rewritten to consume the new ViewModel through
  `hiltViewModel()` + `collectAsStateWithLifecycle()`, with loading, empty,
  and error states.
- **Unit tests for `DashboardViewModel`** covering the today-only filter,
  per-package aggregation + descending sort, `AppProfile` category/label
  override, and both over- and under-goal copy variants.
- **Honest "Current Status" section** at the top of the README listing what
  actually works vs. what is stubbed (in-memory repos, unconfigured
  Supabase, lazy iOS auth) so the product vision below cannot be mistaken
  for shipping reality.

### Fixed
- **Onboarding never dismissed on Android.** `MainActivity` previously
  hard-coded `onboardingCompleted = false`; it now injects
  `BilboPreferences` via Hilt, reads the persisted flag on launch, and
  writes it through the onboarding completion callback so returning users
  land directly on the dashboard.
- **Placeholder navigation screens shadowed the real UI.** `BilboNavHost`
  now routes to the actual `InsightsScreen`, `SocialHubScreen`,
  `BudgetDashboardScreen`, `AnalogSuggestionsScreen`, `CircleScreen`,
  `ChallengeScreen`, `LeaderboardScreen`, `DigestScreen`,
  `WeeklyInsightScreen`, `DataAnonymizationScreen`, and the settings
  `SettingsScreen`. Only Focus (intentionally out-of-app), Interests
  reconfigure, and individual Settings sub-routes remain as documented
  placeholders.
- **Dashboard never refreshed after writes.** The in-memory
  `UsageRepository` and `AppProfileRepository` stubs used cold `flowOf(...)`
  emissions, so collectors saw only the initial snapshot. Both now use a
  hot `MutableStateFlow`, and `observeAll()` / `observeByPackageName()`
  re-emit on every insert/update/delete.
- **Misleading iOS `AuthManager` TODO comment.** `ContentViewModel` now
  documents that `.unauthenticated` at launch is the intended lazy-auth
  behaviour (per the shared `AuthManager.kt` contract) rather than a
  temporary stub.

### Changed
- **Removed legacy `UsageMonitorService.kt`** (unused stub). The real
  foreground service is `UsageTrackingService.kt`, which already persists
  `UsageSession`s to the repository via `SessionTracker`. The manifest was
  cleaned up to match.
- **Removed duplicate top-level `SettingsScreen.kt`** under
  `ui/screen/`; `ui/screen/settings/SettingsScreen.kt` is now the single
  source of truth and is aliased as `RealSettingsScreen` in the NavHost.
- **README tech-stack and prerequisites** corrected to match the actual
  versions on disk (Kotlin 2.3.20, SQLDelight 2.3.2, Ktor 3.4.2,
  kotlinx.coroutines 1.10.2, kotlinx.serialization 1.11.0, Gradle 9.4.1).

### Known follow-ups (v1.1)
- Swap the in-memory repository stubs for SQLDelight-backed implementations
  (schema already ships in `shared/src/commonMain/sqldelight/...`).
- Configure Supabase environment (`SUPABASE_URL`, `SUPABASE_ANON_KEY`)
  and deploy edge functions so buddies, circles, challenges, and the
  Tier-3 weekly AI narrative transact real data.
- Ship a signed iOS `.ipa` once an Apple Developer certificate is
  attached (current iOS artifact is an unsigned simulator build).
- Dependabot alert #29 (moderate severity) flagged on v1.0.0's PRs is
  tracked as a follow-up and does not block this release.

[1.0.1]: https://github.com/Prekzursil/bilbo-app/releases/tag/v1.0.1

## [1.0.0] — 2026-04-17

First tagged release of the Bilbo Kotlin Multiplatform app. All pre-1.0 work
prior to this tag is consolidated here; future releases will list deltas only.

### Added
- Kotlin Multiplatform shared module with 100% line coverage enforced via
  Kover, covering domain models, repositories, the three-tier intelligence
  engine, the Focus Points economy engine, social logic, tracking, and
  enforcement primitives.
- Android app built with Jetpack Compose, Hilt, WorkManager, and a
  `UsageMonitorService` foreground service (with `playstore` and `github`
  product flavors).
- iOS app built with SwiftUI, `FamilyControls`, and a
  `DeviceActivityMonitorExtension` for enforcement.
- Supabase edge functions and CI workflows for shared tests, Android, iOS,
  and CodeQL.
- Documentation: comprehensive `README.md` with architecture diagrams,
  `CONTRIBUTING.md`, and `CLAUDE.md` agent notes.

### Fixed
- **Android Lint `NewApi` blocker** in
  `PollingAppMonitor.hasUsageStatsPermission()`: the Play Store flavor now
  calls `AppOpsManager.unsafeCheckOpNoThrow` only on API 29+ and falls back
  to the deprecated `checkOpNoThrow` on API 26–28, matching the declared
  `minSdk = 26`.
- **iOS compile error from duplicate view types**: `ContentView.swift` no
  longer redeclares `MainTabView`, `DashboardView`, `FocusView`,
  `InsightsView`, and `SettingsView` — it now delegates to the canonical
  definitions in `MainTabView.swift`, `Dashboard/DashboardView.swift`, and
  `Settings/SettingsView.swift`.

### Changed
- `UsageMonitorService.pollUsageStats()` now queries `UsageStatsManager`
  for the last polling window and logs the number of records and total
  foreground time. Remote forwarding is documented as follow-up work.
- `SyncWorker.doWork()` now enumerates locally cached weekly insights via
  the shared `InsightRepository` so the worker exercises its dependency
  graph on every run instead of short-circuiting.
- `DashboardScreen` preview rows were extracted into a named
  `DEFAULT_PREVIEW_ROWS` constant to make the "real ViewModel" follow-up
  unambiguous.
- `ContentViewModel.checkAuthState()` now delegates to a documented
  async bridge that will call the shared KMP `AuthManager` once its ObjC
  header exports the `hasActiveSession()` helper.
- README Kotlin badge updated from `2.1.0` to `2.3.20` to match
  `gradle/libs.versions.toml`.

### Security
- CodeQL workflow continues to pass with no findings on the `main` branch.

[1.0.0]: https://github.com/Prekzursil/bilbo-app/releases/tag/v1.0.0
