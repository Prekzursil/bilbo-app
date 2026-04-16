# Changelog

All notable changes to Bilbo are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and Bilbo adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
