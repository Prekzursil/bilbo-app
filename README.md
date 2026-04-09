# Spark — Digital Wellness App

A Kotlin Multiplatform (KMP) digital wellness mobile app for Android and iOS that helps users build mindful screen-time habits.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Shared KMP | Kotlin 2.1.0, SQLDelight 2.0.2, Ktor 3.0.3, kotlinx.coroutines 1.9.0, kotlinx.serialization 1.7.3 |
| Android | Jetpack Compose, Material 3, Hilt, WorkManager, ForegroundService, WindowManager overlays |
| iOS | SwiftUI, FamilyControls, DeviceActivityMonitor, ShieldConfigurationDataSource |
| Backend | Supabase (Auth, PostgreSQL, Realtime, Edge Functions, FCM/APNs push) |
| AI | Local Kotlin heuristics (Tier 1-2), Anthropic Claude via Supabase Edge Function relay (Tier 3) |
| Analytics | PostHog (privacy-respecting, self-hostable) |
| Observability | Sentry (Android + iOS), Timber |
| CI | GitHub Actions, SonarCloud, Codecov, Detekt, SwiftLint |

## Project Structure

```
spark/
├── androidApp/                 # Android Jetpack Compose application
│   ├── src/main/
│   │   ├── kotlin/dev/spark/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── SparkApplication.kt
│   │   │   ├── di/             # Hilt DI modules
│   │   │   ├── service/        # ForegroundService, AccessibilityService, BootReceiver
│   │   │   ├── ui/             # Compose screens, navigation, theme
│   │   │   └── worker/         # WorkManager sync worker
│   │   └── res/                # Layouts, drawables, XML configs
│   └── build.gradle.kts        # Android module build — playstore + github flavors
├── shared/                     # KMP shared module
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/dev/spark/shared/
│   │   │   │   ├── data/       # Remote API, SQLDelight repositories
│   │   │   │   ├── domain/     # Models, use cases
│   │   │   │   └── util/       # Result type, Flow extensions
│   │   │   └── sqldelight/     # .sq schema files (AppUsage, WellnessGoal)
│   │   ├── androidMain/        # Android SQLDelight driver
│   │   ├── iosMain/            # iOS SQLDelight native driver
│   │   └── commonTest/         # Shared unit tests
│   └── build.gradle.kts        # KMP module build with SQLDelight config
├── iosApp/                     # iOS SwiftUI application
│   └── iosApp/
│       ├── SparkApp.swift       # @main entry point + AppDelegate
│       ├── ContentView.swift    # Root view with auth routing
│       ├── DeviceActivityMonitorExtension.swift
│       └── ShieldConfigurationExtension.swift
├── supabase/
│   ├── functions/
│   │   ├── ai-relay/           # Anthropic API proxy (Tier-3 AI insights)
│   │   └── push-notification/  # FCM + APNs notification dispatcher
│   └── migrations/             # PostgreSQL schema migrations
├── .github/workflows/
│   ├── shared-tests.yml        # KMP shared module tests
│   ├── android-ci.yml          # Android lint, tests, APK builds
│   ├── ios-ci.yml              # iOS XCFramework build + SwiftLint
│   └── backend-ci.yml          # Deno lint + Edge Function deploy
├── gradle/
│   ├── libs.versions.toml      # Version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties  # Gradle 8.11.1
├── build.gradle.kts            # Root build — plugin declarations
├── settings.gradle.kts         # Project settings + module includes
├── gradle.properties           # JVM args, Android, KMP flags
├── detekt.yml                  # Kotlin static analysis configuration
├── .swiftlint.yml              # Swift static analysis configuration
├── sonar-project.properties    # SonarCloud configuration
└── .gitignore
```

## Build Flavors (Android)

| Flavor | `applicationId` | `USE_ACCESSIBILITY_SERVICE` | Distribution |
|--------|-----------------|---------------------------|--------------|
| `playstore` | `dev.spark.app` | `false` (uses UsageStatsManager) | Google Play Store |
| `github` | `dev.spark.app.github` | `true` (uses AccessibilityService) | GitHub Releases / F-Droid |

## Getting Started

### Prerequisites

- JDK 17+
- Android Studio Ladybug (2024.2.1) or later
- Xcode 16+ (for iOS)
- Supabase account

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/spark-wellness/spark.git
   cd spark
   ```

2. **Configure secrets** — create `local.properties` (not committed):
   ```properties
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your-anon-key
   SENTRY_DSN=https://xxx@sentry.io/xxx
   POSTHOG_API_KEY=phc_xxx
   ```

3. **Run the database migrations**
   ```bash
   supabase db push
   ```

4. **Build and run Android (playstore flavor)**
   ```bash
   ./gradlew :androidApp:installPlaystoreDebug
   ```

5. **Build iOS framework**
   ```bash
   ./gradlew :shared:assembleSharedDebugXCFramework
   ```
   Then open `iosApp/iosApp.xcodeproj` in Xcode.

## CI/CD

All CI runs on GitHub Actions:

- **`shared-tests.yml`** — runs on every push/PR; executes `allTests` for the shared KMP module
- **`android-ci.yml`** — lint, Detekt, unit tests, builds both flavor APKs; uploads to Codecov
- **`ios-ci.yml`** — builds XCFramework on macOS, runs SwiftLint, builds iOS app (simulator)
- **`backend-ci.yml`** — Deno lint + type check for Edge Functions; deploys on `main` push

## Required GitHub Secrets

| Secret | Purpose |
|--------|---------|
| `SUPABASE_URL` | Supabase project URL |
| `SUPABASE_ANON_KEY` | Supabase public anon key |
| `SUPABASE_PROJECT_REF` | Supabase project reference (for CLI deploy) |
| `SUPABASE_ACCESS_TOKEN` | Supabase management API token |
| `SENTRY_AUTH_TOKEN` | Sentry release upload token |
| `SONAR_TOKEN` | SonarCloud analysis token |
| `CODECOV_TOKEN` | Codecov upload token |
| `ANTHROPIC_API_KEY` | Anthropic Claude API key (Edge Function env var) |
| `FCM_SERVER_KEY` | Firebase Cloud Messaging server key (Edge Function env var) |

## License

Copyright © 2025 Spark Wellness. All rights reserved.
