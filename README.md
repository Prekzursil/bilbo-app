# Bilbo — Digital Wellness & Mindful Screen Time

> Take control of your screen time. Reclaim your focus. Protect your mental health.

[![CI — Shared Tests](https://github.com/Prekzursil/bilbo-app/actions/workflows/shared-tests.yml/badge.svg)](https://github.com/Prekzursil/bilbo-app/actions/workflows/shared-tests.yml)
[![CI — Android](https://github.com/Prekzursil/bilbo-app/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Prekzursil/bilbo-app/actions/workflows/android-ci.yml)
[![CI — iOS](https://github.com/Prekzursil/bilbo-app/actions/workflows/ios-ci.yml/badge.svg)](https://github.com/Prekzursil/bilbo-app/actions/workflows/ios-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple.svg)](https://kotlinlang.org)
[![codecov](https://codecov.io/gh/Prekzursil/bilbo-app/branch/main/graph/badge.svg)](https://codecov.io/gh/Prekzursil/bilbo-app)

---

## What is Bilbo?

Bilbo is a Kotlin Multiplatform (KMP) digital wellness app for Android and iOS that helps you build mindful screen-time habits. Before you open a distracting app, Bilbo asks you *why* — then enforces your intention, tracks your emotional state, and rewards you for staying on track. It combines a Focus Points economy, social accountability, and a three-tier AI intelligence system to make conscious screen-time the default, not the exception.

---

## Architecture Overview

```mermaid
graph TB
    subgraph "Mobile Clients"
        A["Android App<br/>Jetpack Compose<br/>Hilt · WorkManager · ForegroundService"]
        I["iOS App<br/>SwiftUI<br/>FamilyControls · DeviceActivityMonitor"]
    end

    subgraph "Shared KMP Module"
        direction TB
        DM["Domain Models<br/>UsageSession · IntentDeclaration<br/>EmotionalCheckIn · DopamineBudget<br/>AppProfile · AnalogSuggestion"]
        REPO["Repositories<br/>UsageRepository · IntentRepository<br/>EmotionRepository · BudgetRepository<br/>AppProfileRepository · SuggestionRepository"]
        IE["Intelligence Engine<br/>RuleEngine (Tier 1)<br/>HeuristicEngine (Tier 2)<br/>CloudInsightClient (Tier 3)"]
        EE["Economy Engine<br/>FocusPointsEngine · AppClassifier<br/>BudgetEnforcer"]
        SL["Social Logic<br/>BuddyManager · CircleManager<br/>ChallengeEngine · LeaderboardCalculator"]
        TRACK["Tracking<br/>AppMonitor · SessionTracker<br/>BypassManager"]
        ENFC["Enforcement<br/>CooldownManager · DecisionEngine"]
    end

    subgraph "Backend — Supabase"
        Auth["Supabase Auth<br/>JWT · RLS"]
        DB[("PostgreSQL<br/>Row Level Security<br/>10 tables")]
        EF["Edge Functions (Deno/TypeScript)<br/>ai-weekly-insight · generate-digest<br/>send-nudge · sync-status<br/>create-invite · accept-invite<br/>create-circle · join-circle<br/>create-challenge · compute-leaderboard<br/>push-notification"]
        RT["Supabase Realtime<br/>Buddy status · Circle feed<br/>Challenge progress"]
    end

    subgraph "External Services"
        AI["Anthropic Claude<br/>Tier 3 Weekly Narrative"]
        FCM["FCM / APNs<br/>Push Notifications"]
        PH["PostHog<br/>Privacy-respecting Analytics"]
        ST["Sentry<br/>Crash Reporting"]
        SC["SonarCloud · Codecov<br/>Code Quality"]
    end

    A --> DM
    I --> DM
    DM --> REPO
    REPO --> IE
    REPO --> EE
    REPO --> SL
    REPO --> TRACK
    REPO --> ENFC
    A --> Auth
    I --> Auth
    Auth --> DB
    REPO -->|"Ktor HTTP / supabase-kt"| EF
    EF --> DB
    EF --> AI
    EF --> FCM
    RT -->|"Realtime subscriptions"| A
    RT -->|"Realtime subscriptions"| I
    A --> PH
    I --> PH
    A --> ST
    I --> ST
```

---

## Features

### Intent Gatekeeper
Before any tracked app opens, Bilbo intercepts with an overlay asking: *"What do you want to do, and for how long?"* Users declare their intent and duration. This single friction point breaks the habit loop of mindless app-switching and is at the core of how Bilbo changes behavior.

### Emotional Check-ins
At the start and end of each session, Bilbo captures your emotional state (Happy, Calm, Bored, Stressed, Anxious, Sad, Lonely). Over time, the AI correlates usage patterns with mood to surface insights like "You tend to feel more anxious after Instagram sessions that run over 20 minutes."

### Focus Points Economy
Bilbo uses a Focus Points (FP) currency system to gamify wellness:
- **Earn FP** by using nutritive apps, completing breathing exercises, accepting analog suggestions, and achieving accurate intent declarations.
- **Spend FP** by using empty-calorie apps beyond your declared intent.
- **Lose FP** for ignoring nudges or overriding hard locks.
- A daily baseline, streak bonuses, and rollover (50%) keep the system fair and motivating.

### Enforcement Engine (Nudge + Hard Lock)
Each app is classified as Nutritive, Neutral, or Empty Calories and assigned an enforcement mode:
- **Nudge** — a dismissible reminder that costs FP to bypass, creating conscious friction.
- **Hard Lock** — a full overlay that cannot be dismissed without a significant FP penalty, used for the most problematic apps.
- A **CooldownManager** prevents immediate re-entry after a session ends.

### Analog Alternatives
When Bilbo enforces a limit, it surfaces a curated suggestion from 10 categories (Exercise, Creative, Social, Mindfulness, Learning, Nature, Cooking, Music, Physical Gaming, Reading). Accepting a suggestion earns bonus FP and is tracked for intent-accuracy scoring.

### 3-Tier AI Intelligence

| Tier | Engine | Frequency | Description |
|------|--------|-----------|-------------|
| 1 | RuleEngine | Real-time | Deterministic rules: session length thresholds, daily limits, budget enforcement |
| 2 | HeuristicEngine | Nightly batch | Correlation analysis, trend detection, gaming/streak detection |
| 3 | CloudInsightClient | Weekly | Anthropic Claude generates a 200-word personalized narrative using anonymized usage data |

### Focus Buddies & Circles
- **Buddies** — one-to-one accountability pairings with configurable sharing levels (Scores Only → Full transparency). Buddies can send encouragements and maintain mutual streaks.
- **Circles** — named groups (public or invite-only) for broader social accountability. Members share wellness progress at their chosen sharing level.

### Challenges & Leaderboards
Time-boxed wellness challenges between buddies or circle members. Challenge types include Most FP Earned, Longest Streak, Most Nutritive Minutes, Best Intent Accuracy, and more. Challenges run in either Competitive or Cooperative mode, with real-time leaderboards powered by Supabase Realtime.

---

## System Architecture

```mermaid
graph TB
    subgraph "Android App — Jetpack Compose"
        ACT["MainActivity<br/>NavHost"]
        SVC["BilboForegroundService<br/>AppMonitor polling"]
        ACC["BilboAccessibilityService<br/>(github flavor)"]
        OVER["WindowManager Overlay<br/>Intent Gatekeeper UI"]
        WRK["WorkManager<br/>Sync Worker · Nightly batch"]
        HILT["Hilt DI Graph"]
    end

    subgraph "iOS App — SwiftUI"
        CONT["ContentView<br/>Auth routing"]
        DAM["DeviceActivityMonitor<br/>Extension"]
        SHD["ShieldConfiguration<br/>Extension"]
        FAMC["FamilyControls<br/>Framework"]
    end

    subgraph "KMP Shared — commonMain"
        direction LR
        DOM["domain/"]
        DAT["data/"]
        INT["intelligence/"]
        ECON["economy/"]
        SOC["social/"]
        TRK["tracking/"]
        ENF["enforcement/"]
        AUTH["auth/"]
        PREF["preferences/"]
    end

    subgraph "Supabase Backend"
        direction TB
        PG["PostgreSQL + RLS"]
        FN["Edge Functions"]
        RLT["Realtime"]
        STG["Storage (avatars)"]
    end

    ACT --> HILT
    SVC --> TRK
    ACC --> TRK
    OVER --> ENF
    WRK --> DAT
    CONT --> DOM
    DAM --> TRK
    SHD --> ENF
    FAMC --> DAM
    DOM --- DAT
    DAT --- INT
    DAT --- ECON
    DAT --- SOC
    DAT --- TRK
    DAT --- ENF
    DAT -->|"Ktor + supabase-kt"| PG
    DAT -->|"Edge Function calls"| FN
    SOC -->|"Realtime subscriptions"| RLT
    FN --> PG
    FN --> RLT
```

---

## Data Flow

### Intent Gatekeeper Sequence

```mermaid
sequenceDiagram
    actor U as User
    participant AM as AppMonitor
    participant GK as Gatekeeper<br/>(DecisionEngine)
    participant EC as EmotionalCheckIn
    participant FP as FocusPointsEngine
    participant BE as BudgetEnforcer
    participant TE as Timer/Enforcement
    participant DB as Local SQLDelight

    U->>AM: Opens tracked app
    AM->>GK: App detected (package, category, profile)
    GK->>GK: Check BypassManager — is app on bypass list?
    alt App is bypassed
        GK-->>U: App opens immediately
    else App requires gatekeeper
        GK->>U: Show Intent Gatekeeper overlay
        U->>GK: Declares intent + duration (e.g. 10 min)
        GK->>EC: Request pre-session emotional check-in
        EC->>U: Show emotion selector (7 options)
        U->>EC: Selects emotion (e.g. BORED)
        EC->>DB: Save EmotionalCheckIn (pre-session)
        EC->>FP: Query current FP balance
        FP->>BE: Validate budget for declared session
        BE-->>GK: Budget OK / Budget low warning
        GK->>DB: Save IntentDeclaration
        GK->>TE: Start session timer (10 min)
        TE-->>U: App overlay dismissed — app accessible
        Note over TE,U: Session in progress...
        TE->>U: 2-minute warning notification
        TE->>GK: Session timer expired
        GK->>GK: Evaluate enforcement mode (NUDGE or HARD_LOCK)
        alt Enforcement = NUDGE
            GK->>U: Show dismissible nudge overlay
            U->>GK: Dismiss (costs 3 FP) or end session
        else Enforcement = HARD_LOCK
            GK->>U: Show hard lock overlay (cannot dismiss)
            U->>GK: End session (or override: costs 10 FP)
        end
        GK->>EC: Request post-session emotional check-in
        U->>EC: Selects mood (e.g. ANXIOUS)
        EC->>DB: Save EmotionalCheckIn (post-session, linked)
        GK->>FP: Calculate and award/deduct FP
        FP->>DB: Update DopamineBudget
        GK->>DB: Update IntentDeclaration (actual duration, enforcement)
        Note over GK,DB: Session complete — data synced to Supabase on next WorkManager run
    end
```

---

## Domain Model

```mermaid
classDiagram
    class UsageSession {
        +Long id
        +String packageName
        +String appLabel
        +AppCategory category
        +Instant startTime
        +Instant? endTime
        +Long durationSeconds
        +Boolean wasTracked
    }

    class IntentDeclaration {
        +Long id
        +Instant timestamp
        +String declaredApp
        +Int declaredDurationMinutes
        +Int? actualDurationMinutes
        +Boolean wasEnforced
        +EnforcementMode? enforcementType
        +Boolean wasOverridden
        +Long? emotionalCheckInId
    }

    class EmotionalCheckIn {
        +Long id
        +Instant timestamp
        +Emotion preSessionEmotion
        +Emotion? postSessionMood
        +Long? linkedIntentId
    }

    class AppProfile {
        +String packageName
        +String appLabel
        +AppCategory category
        +EnforcementMode enforcementMode
        +Boolean coolingOffEnabled
        +Boolean isBypassed
        +Boolean isCustomClassification
    }

    class DopamineBudget {
        +LocalDate date
        +Int fpEarned
        +Int fpSpent
        +Int fpBonus
        +Int fpRolloverIn
        +Int fpRolloverOut
        +Int nutritiveMinutes
        +Int emptyCalorieMinutes
        +Int neutralMinutes
        +currentBalance() Int
    }

    class AnalogSuggestion {
        +Long id
        +String text
        +SuggestionCategory category
        +List~String~ tags
        +TimeOfDay? timeOfDay
        +Int timesShown
        +Int timesAccepted
        +Boolean isCustom
    }

    class WeeklyInsight {
        +Long id
        +LocalDate weekStart
        +List~HeuristicInsight~ tier2Insights
        +String? tier3Narrative
        +Int totalScreenTimeMinutes
        +Int nutritiveMinutes
        +Int emptyCalorieMinutes
        +Int fpEarned
        +Int fpSpent
        +Float intentAccuracyPercent
        +Int streakDays
    }

    class HeuristicInsight {
        +InsightType type
        +String message
        +Float confidence
    }

    class BuddyPair {
        +Long id
        +String localUserId
        +String buddyUserId
        +String buddyDisplayName
        +SharingLevel sharingLevel
        +Instant createdAt
        +Boolean isActive
        +Int streakDays
    }

    class Circle {
        +Long id
        +String remoteId
        +String name
        +String ownerId
        +SharingLevel sharingLevel
        +Boolean isPublic
        +String? inviteCode
        +Int memberCount
        +List~CircleMember~ members
    }

    class CircleMember {
        +String userId
        +String displayName
        +CircleRole role
        +Instant joinedAt
        +SharingLevel sharingLevel
        +Int currentStreakDays
        +Int weeklyFpEarned
    }

    class Challenge {
        +Long id
        +String remoteId
        +String title
        +ChallengeType type
        +ChallengeMode mode
        +ChallengeStatus status
        +ChallengeGoal goal
        +LocalDate startDate
        +LocalDate endDate
        +List~String~ participantIds
        +Float localUserProgress
    }

    class ChallengeGoal {
        +Int targetValue
        +String unit
        +Boolean perParticipant
    }

    class AppCategory {
        <<enumeration>>
        NUTRITIVE
        NEUTRAL
        EMPTY_CALORIES
    }

    class EnforcementMode {
        <<enumeration>>
        NUDGE
        HARD_LOCK
    }

    class Emotion {
        <<enumeration>>
        HAPPY
        CALM
        BORED
        STRESSED
        ANXIOUS
        SAD
        LONELY
    }

    class SharingLevel {
        <<enumeration>>
        PRIVATE
        SCORES_ONLY
        CATEGORY_BREAKDOWN
        APP_TOTALS
        FULL
    }

    class ChallengeType {
        <<enumeration>>
        MOST_FP_EARNED
        LONGEST_STREAK
        MOST_NUTRITIVE_MINUTES
        LOWEST_EMPTY_CALORIE_MINUTES
        MOST_ANALOG_ACCEPTED
        BEST_INTENT_ACCURACY
        BREATHING_EXERCISES_COUNT
    }

    UsageSession --> AppProfile : "classified by"
    IntentDeclaration --> EmotionalCheckIn : "linked to"
    IntentDeclaration --> AppProfile : "declares use of"
    DopamineBudget --> UsageSession : "aggregates"
    WeeklyInsight --> HeuristicInsight : "contains"
    BuddyPair --> SharingLevel : "uses"
    Circle --> CircleMember : "has many"
    CircleMember --> SharingLevel : "uses"
    Challenge --> ChallengeGoal : "defines"
    AppProfile --> AppCategory : "classified as"
    AppProfile --> EnforcementMode : "enforced by"
    EmotionalCheckIn --> Emotion : "records"
```

---

## AI Intelligence Tiers

```mermaid
graph LR
    subgraph "Tier 1 — Deterministic (Real-time)"
        RE["RuleEngine<br/>━━━━━━━━━━━━<br/>Session length threshold<br/>Daily budget enforcement<br/>Hard lock trigger<br/>Bypass check<br/>Cooldown gating"]
    end

    subgraph "Tier 2 — Heuristic (Nightly Batch)"
        HE["HeuristicEngine<br/>Orchestrator"]
        CA["CorrelationAnalyzer<br/>emotion × usage<br/>time-of-day patterns"]
        TD["TrendDetector<br/>Week-over-week<br/>category shifts<br/>streak analysis"]
        GD["GamingDetector<br/>Intent accuracy<br/>bypass abuse<br/>override frequency"]
    end

    subgraph "Tier 3 — Cloud AI (Weekly)"
        PB["InsightPromptBuilder<br/>Anonymizes & serializes<br/>weekly usage to JSON"]
        CI["CloudInsightClient<br/>Supabase Edge Function<br/>ai-weekly-insight"]
        CL["Anthropic Claude<br/>200-word narrative<br/>personalized insight"]
    end

    subgraph "Output"
        RT["Real-time decisions<br/>enforcement · nudges · FP"]
        HI["HeuristicInsights<br/>stored in WeeklyInsight"]
        WN["Weekly Narrative<br/>tier3Narrative string<br/>displayed in digest"]
    end

    RE -->|"enforce / allow / nudge"| RT
    RE -->|"signals anomaly"| HE
    HE --> CA
    HE --> TD
    HE --> GD
    CA -->|"CORRELATION insight"| HI
    TD -->|"TREND insight"| HI
    GD -->|"ANOMALY insight"| HI
    PB -->|"anonymized JSON payload"| CI
    CI -->|"Anthropic API call"| CL
    CL -->|"narrative text"| WN
```

---

## Database Schema

```mermaid
erDiagram
    users {
        uuid id PK
        text email
        timestamptz created_at
    }

    profiles {
        uuid id PK
        text username
        text display_name
        text avatar_url
        text timezone
        timestamptz onboarded_at
        timestamptz created_at
        timestamptz updated_at
    }

    invite_codes {
        uuid id PK
        uuid creator_id FK
        text code
        boolean is_used
        uuid used_by FK
        timestamptz expires_at
        timestamptz created_at
    }

    buddy_pairs {
        uuid id PK
        uuid user_a_id FK
        uuid user_b_id FK
        text sharing_level
        boolean is_active
        integer streak_days
        timestamptz last_synced_at
        timestamptz created_at
    }

    nudge_events {
        uuid id PK
        uuid sender_id FK
        uuid recipient_id FK
        text message
        boolean was_read
        timestamptz sent_at
    }

    status_summaries {
        uuid id PK
        uuid user_id FK
        date summary_date
        integer fp_earned
        integer fp_spent
        integer fp_balance
        integer nutritive_minutes
        integer empty_calorie_minutes
        integer neutral_minutes
        integer streak_days
        float intent_accuracy
        timestamptz created_at
    }

    circles {
        uuid id PK
        uuid owner_id FK
        text name
        text description
        text avatar_url
        text sharing_level
        boolean is_public
        text invite_code
        timestamptz created_at
    }

    circle_members {
        uuid id PK
        uuid circle_id FK
        uuid user_id FK
        text role
        text sharing_level
        boolean is_active
        timestamptz joined_at
    }

    challenges {
        uuid id PK
        uuid creator_id FK
        text title
        text description
        text type
        text mode
        text status
        jsonb goal
        date start_date
        date end_date
        timestamptz created_at
        timestamptz completed_at
        uuid winner_id FK
    }

    challenge_progress {
        uuid id PK
        uuid challenge_id FK
        uuid user_id FK
        float current_value
        integer rank
        timestamptz last_updated_at
    }

    weekly_digest {
        uuid id PK
        uuid user_id FK
        date week_start
        integer total_screen_time_minutes
        integer nutritive_minutes
        integer empty_calorie_minutes
        integer fp_earned
        integer fp_spent
        float intent_accuracy_percent
        integer streak_days
        jsonb tier2_insights
        text tier3_narrative
        timestamptz generated_at
    }

    users ||--|| profiles : "extends"
    users ||--o{ invite_codes : "creates"
    users ||--o{ invite_codes : "redeems"
    users ||--o{ buddy_pairs : "user_a"
    users ||--o{ buddy_pairs : "user_b"
    users ||--o{ nudge_events : "sends"
    users ||--o{ nudge_events : "receives"
    users ||--o{ status_summaries : "has"
    users ||--o{ circles : "owns"
    users ||--o{ circle_members : "joins"
    circles ||--o{ circle_members : "has"
    users ||--o{ challenges : "creates"
    challenges ||--o{ challenge_progress : "tracks"
    users ||--o{ challenge_progress : "has"
    users ||--o{ weekly_digest : "has"
```

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Shared KMP** | Kotlin 2.1.0, SQLDelight 2.0.2, Ktor 3.0.3, kotlinx.coroutines 1.9.0, kotlinx.serialization 1.7.3 |
| **Android** | Jetpack Compose, Material 3, Hilt, WorkManager, ForegroundService, WindowManager overlays |
| **iOS** | SwiftUI, FamilyControls, DeviceActivityMonitor, ShieldConfigurationDataSource |
| **Backend** | Supabase (Auth, PostgreSQL, Realtime, Edge Functions, Storage) |
| **Edge Functions** | Deno / TypeScript |
| **AI** | Local Kotlin heuristics (Tier 1–2), Anthropic Claude via Supabase Edge Function relay (Tier 3) |
| **Push** | Firebase Cloud Messaging (Android), APNs (iOS) |
| **Analytics** | PostHog (privacy-respecting, self-hostable) |
| **Observability** | Sentry (Android + iOS), Timber |
| **CI** | GitHub Actions, SonarCloud, Codecov, Detekt, SwiftLint, CodeQL |

---

## Project Structure

```
bilbo-app/
├── androidApp/                      # Android Jetpack Compose application
│   ├── src/main/
│   │   ├── kotlin/dev/spark/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── BilboApplication.kt
│   │   │   ├── di/                  # Hilt DI modules
│   │   │   ├── service/             # ForegroundService, AccessibilityService, BootReceiver
│   │   │   ├── ui/                  # Compose screens, navigation, theme
│   │   │   └── worker/              # WorkManager sync worker
│   │   └── res/                     # Layouts, drawables, XML configs
│   └── build.gradle.kts             # Android module build — playstore + github flavors
├── shared/                          # KMP shared module
│   └── src/
│       ├── commonMain/kotlin/dev/spark/
│       │   ├── domain/              # Core models: UsageSession, IntentDeclaration, etc.
│       │   │   └── social/          # BuddyPair, Circle, CircleMember, Challenge
│       │   ├── data/                # Repositories, DatabaseDriverFactory, SeedDataLoader
│       │   ├── intelligence/
│       │   │   ├── tier1/           # RuleEngine (real-time deterministic)
│       │   │   ├── tier2/           # HeuristicEngine, CorrelationAnalyzer, TrendDetector, GamingDetector
│       │   │   └── tier3/           # CloudInsightClient, InsightPromptBuilder
│       │   ├── economy/             # FocusPointsEngine, AppClassifier, BudgetEnforcer
│       │   ├── social/              # BuddyManager, CircleManager, ChallengeEngine, LeaderboardCalculator
│       │   ├── tracking/            # AppMonitor, SessionTracker, BypassManager
│       │   ├── enforcement/         # CooldownManager, DecisionEngine
│       │   ├── analog/              # SuggestionEngine
│       │   ├── auth/                # AuthManager
│       │   └── preferences/         # BilboPreferences
│       │   ├── commonMain/sqldelight/ # SQLDelight .sq schema files
│       │   └── commonMain/resources/  # Seed data JSON (app classifications, analog suggestions)
│       ├── androidMain/             # Android SQLDelight driver + platform-specific
│       ├── iosMain/                 # iOS SQLDelight native driver
│       └── commonTest/              # Shared unit tests (≥80% coverage target)
├── iosApp/                          # iOS SwiftUI application
│   └── iosApp/
│       ├── BilboApp.swift            # @main entry point + AppDelegate
│       ├── ContentView.swift         # Root view with auth routing
│       ├── DeviceActivityMonitorExtension.swift
│       └── ShieldConfigurationExtension.swift
├── supabase/
│   ├── functions/
│   │   ├── ai-weekly-insight/        # Anthropic Claude weekly narrative (Tier 3)
│   │   ├── ai-relay/                 # Generic Anthropic API proxy
│   │   ├── generate-digest/          # Weekly digest assembly
│   │   ├── send-nudge/               # Buddy nudge delivery
│   │   ├── sync-status/              # Status summary sync from mobile
│   │   ├── create-invite/            # Buddy invite code generation
│   │   ├── accept-invite/            # Buddy invite acceptance
│   │   ├── create-circle/            # Circle creation
│   │   ├── join-circle/              # Circle membership join
│   │   ├── create-challenge/         # Challenge creation
│   │   ├── compute-leaderboard/      # Challenge leaderboard computation
│   │   └── push-notification/        # FCM + APNs notification dispatcher
│   └── migrations/                   # PostgreSQL schema migrations
├── .github/
│   ├── workflows/
│   │   ├── shared-tests.yml          # KMP shared module tests
│   │   ├── android-ci.yml            # Android lint, tests, APK builds
│   │   ├── ios-ci.yml                # iOS XCFramework build + SwiftLint
│   │   ├── backend-ci.yml            # Deno lint + Edge Function deploy
│   │   └── codeql.yml                # CodeQL security analysis
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.yml
│   │   ├── feature_request.yml
│   │   └── config.yml
│   ├── CODEOWNERS
│   ├── dependabot.yml
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── SECURITY.md
├── docs/                             # Extended documentation and architecture plans
├── gradle/
│   ├── libs.versions.toml            # Version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties # Gradle 8.11.1
├── build.gradle.kts                  # Root build — plugin declarations
├── settings.gradle.kts               # Project settings + module includes
├── gradle.properties                 # JVM args, Android, KMP flags
├── detekt.yml                        # Kotlin static analysis configuration
├── sonar-project.properties          # SonarCloud configuration
├── CONTRIBUTING.md
├── LICENSE
└── README.md
```

---

## Build Flavors

| Flavor | Application ID | Screen Time Method | Distribution |
|--------|---------------|---------------------|--------------|
| `playstore` | `dev.spark.app` | `UsageStatsManager` (no special permission required) | Google Play Store |
| `github` | `dev.spark.app.github` | `AccessibilityService` (full app-switch detection) | GitHub Releases / F-Droid |

The `github` flavor provides more precise enforcement because `AccessibilityService` fires on every window change, whereas `UsageStatsManager` is polled on a timer. Google Play policies restrict `AccessibilityService` to assistive-technology use cases, hence the separate flavor.

---

## Getting Started

### Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 17+ | [Adoptium](https://adoptium.net) |
| Android Studio | Ladybug 2024.2.1+ | [developer.android.com](https://developer.android.com/studio) |
| Kotlin Multiplatform Plugin | 2.1.0 | Android Studio → Plugins |
| Xcode | 16+ | macOS only — required for iOS |
| Supabase CLI | Latest | `brew install supabase/tap/supabase` |
| Deno | 1.40+ | For local Edge Function development |

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/Prekzursil/bilbo-app.git
   cd bilbo-app
   ```

2. **Configure local secrets** — create `local.properties` (not committed to git):
   ```properties
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your-anon-key
   SENTRY_DSN=https://xxx@sentry.io/xxx
   POSTHOG_API_KEY=phc_xxx
   ```

3. **Start local Supabase stack** (requires Docker):
   ```bash
   supabase start
   supabase db reset   # applies all migrations from /supabase/migrations
   ```

4. **Build and run Android (github flavor — AccessibilityService)**
   ```bash
   ./gradlew :androidApp:installGithubDebug
   ```

5. **Build Android (playstore flavor — UsageStatsManager)**
   ```bash
   ./gradlew :androidApp:installPlaystoreDebug
   ```

6. **Build shared KMP module**
   ```bash
   ./gradlew :shared:build
   ```

7. **Build iOS XCFramework**
   ```bash
   ./gradlew :shared:assembleSharedDebugXCFramework
   ```
   Then open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator or device.

8. **Run Edge Functions locally with hot reload**
   ```bash
   supabase functions serve --env-file supabase/.env.local
   ```

### Run All Tests

```bash
# Shared KMP unit tests
./gradlew :shared:allTests

# Android lint + Detekt
./gradlew :androidApp:lint detekt

# Codecov report
./gradlew koverXmlReport
```

---

## CI/CD

All CI runs on GitHub Actions:

| Workflow | Trigger | What it does |
|----------|---------|-------------|
| `shared-tests.yml` | Push / PR to `main` | Runs `allTests` for the shared KMP module; uploads coverage to Codecov |
| `android-ci.yml` | Push / PR to `main` | Detekt, lint, unit tests, builds both flavor APKs, uploads to Codecov |
| `ios-ci.yml` | Push / PR to `main` | Builds XCFramework on macOS, runs SwiftLint, builds iOS app (simulator) |
| `backend-ci.yml` | Push / PR to `main` | Deno lint + type-check for Edge Functions; deploys on `main` push |
| `codeql.yml` | Push / PR to `main` + weekly | CodeQL security analysis for Kotlin and TypeScript |

---

## Required Secrets

Configure these in your repository's **Settings → Secrets and variables → Actions**:

| Secret | Purpose |
|--------|---------|
| `SUPABASE_URL` | Supabase project URL |
| `SUPABASE_ANON_KEY` | Supabase public anon key |
| `SUPABASE_PROJECT_REF` | Supabase project reference ID (for CLI deploy) |
| `SUPABASE_ACCESS_TOKEN` | Supabase management API token |
| `SENTRY_AUTH_TOKEN` | Sentry release upload token |
| `SONAR_TOKEN` | SonarCloud analysis token |
| `CODECOV_TOKEN` | Codecov upload token |
| `ANTHROPIC_API_KEY` | Anthropic Claude API key (Edge Function environment variable) |
| `FCM_SERVER_KEY` | Firebase Cloud Messaging server key (Edge Function environment variable) |

---

## Security

Bilbo handles sensitive personal data including screen-time sessions, emotional states, and social relationships. We take security seriously.

**To report a vulnerability**, please review our [Security Policy](.github/SECURITY.md) and email **prekzursil1993@gmail.com** rather than opening a public issue. We acknowledge all reports within 72 hours and follow a 90-day responsible disclosure timeline.

---

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a pull request. Key points:

- Fork the repo and create a branch following the naming convention (`feature/`, `fix/`, `docs/`, etc.)
- Follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages
- Ensure `./gradlew detekt` and `./gradlew :shared:allTests` pass before opening a PR
- Fill in the PR template completely, including screenshots for UI changes

---

## License

Copyright © 2026 Prekzursil. Released under the [MIT License](LICENSE).

---

## Roadmap

| Phase | Focus | Status |
|-------|-------|--------|
| **Phase 1 — Foundation** | Intent Gatekeeper, Emotional Check-ins, Focus Points Economy, Basic Enforcement | ✅ In progress |
| **Phase 2 — Intelligence** | Tier 1 Rule Engine, Tier 2 Heuristic Engine, Analog Suggestions, Weekly Insights | 🔄 Planned |
| **Phase 3 — Social** | Focus Buddies, Circles, Challenges, Leaderboards, Realtime | 🔄 Planned |
| **Phase 4 — AI** | Tier 3 Anthropic Claude integration, Digest generation, Prompt Builder | 🔄 Planned |
| **Phase 5 — Polish** | Accessibility, Widgets, Wear OS/watchOS, Advanced Analytics | 🔄 Future |
