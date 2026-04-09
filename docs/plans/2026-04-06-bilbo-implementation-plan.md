# Bilbo — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build Bilbo, a cross-platform mobile app (Android + iOS) that combats phone addiction and promotes mental health through intentional usage, emotional intelligence, dopamine budgeting, and social accountability.

**Architecture:** Kotlin Multiplatform (KMP) monorepo with shared business logic, Jetpack Compose for Android UI, SwiftUI for iOS UI, Supabase backend for social features, and a hybrid AI system (local heuristics + optional Anthropic cloud API for weekly narrative insights).

**Tech Stack:**
- **Shared:** Kotlin Multiplatform 2.0+, SQLDelight, Kotlinx Coroutines, Kotlinx Serialization, Ktor Client
- **Android:** Jetpack Compose, Material 3, Hilt, WorkManager, ForegroundService, WindowManager overlays, dual build flavors (playstore/github)
- **iOS:** SwiftUI, FamilyControls, DeviceActivityMonitor, ShieldConfigurationDataSource
- **Backend:** Supabase (Auth, PostgreSQL, Realtime, Edge Functions, Push via FCM/APNs)
- **AI:** Local Kotlin heuristics (Tier 1-2), Anthropic API via Supabase Edge Function relay (Tier 3)
- **CI:** GitHub Actions, SonarCloud, Codecov, Detekt, SwiftLint, Sentry
- **Analytics:** PostHog (privacy-respecting)

---

## Repository Structure

```
bilbo-app/
├── shared/                          # KMP shared module — THE BRAIN
│   └── src/
│       ├── commonMain/kotlin/dev/bilbo/
│       │   ├── domain/              # Domain models
│       │   │   ├── UsageSession.kt
│       │   │   ├── EmotionalCheckIn.kt
│       │   │   ├── AppProfile.kt
│       │   │   ├── DopamineBudget.kt
│       │   │   ├── IntentDeclaration.kt
│       │   │   ├── AnalogSuggestion.kt
│       │   │   ├── BuddyPair.kt
│       │   │   ├── FocusCircle.kt
│       │   │   ├── Challenge.kt
│       │   │   └── WeeklyInsight.kt
│       │   ├── data/                # Repositories + SQLDelight
│       │   │   ├── db/
│       │   │   │   └── BilboDatabase.sq
│       │   │   ├── repository/
│       │   │   │   ├── UsageRepository.kt
│       │   │   │   ├── EmotionRepository.kt
│       │   │   │   ├── BudgetRepository.kt
│       │   │   │   ├── IntentRepository.kt
│       │   │   │   ├── SuggestionRepository.kt
│       │   │   │   └── SocialRepository.kt
│       │   │   └── preferences/
│       │   │       └── BilboPreferences.kt
│       │   ├── intelligence/        # AI / heuristic engine
│       │   │   ├── tier1/
│       │   │   │   └── RuleEngine.kt           # Deterministic enforcement rules
│       │   │   ├── tier2/
│       │   │   │   ├── HeuristicEngine.kt      # Local pattern detection
│       │   │   │   ├── CorrelationAnalyzer.kt   # Emotion × usage correlations
│       │   │   │   ├── TrendDetector.kt         # Weekly/daily trend detection
│       │   │   │   └── GamingDetector.kt        # Anti-gaming heuristics
│       │   │   ├── tier3/
│       │   │   │   ├── CloudInsightClient.kt    # Anthropic API relay
│       │   │   │   └── InsightPromptBuilder.kt  # Prompt construction
│       │   │   └── DecisionEngine.kt            # Orchestrates all tiers
│       │   ├── economy/             # Dopamine budgeting
│       │   │   ├── FocusPointsEngine.kt
│       │   │   ├── AppClassifier.kt
│       │   │   └── BudgetEnforcer.kt
│       │   └── social/              # Social logic (buddy, circles, challenges)
│       │       ├── BuddyManager.kt
│       │       ├── CircleManager.kt
│       │       ├── ChallengeEngine.kt
│       │       └── LeaderboardCalculator.kt
│       ├── androidMain/kotlin/dev/bilbo/
│       │   ├── data/
│       │   │   ├── AndroidDatabaseDriver.kt
│       │   │   └── AndroidPreferences.kt
│       │   └── platform/
│       │       └── PlatformModule.kt
│       └── iosMain/kotlin/dev/bilbo/
│           ├── data/
│           │   ├── IosDatabaseDriver.kt
│           │   └── IosPreferences.kt
│           └── platform/
│               └── PlatformModule.kt
├── androidApp/                      # Android application
│   └── src/
│       ├── main/
│       │   ├── kotlin/dev/bilbo/android/
│       │   │   ├── BilboApp.kt
│       │   │   ├── di/              # Hilt modules
│       │   │   ├── ui/
│       │   │   │   ├── home/        # Dashboard, FP balance, quick stats
│       │   │   │   ├── gatekeeper/  # Intent Gatekeeper overlay UI
│       │   │   │   ├── checkin/     # Emotional check-in screen
│       │   │   │   ├── cooldown/    # Breathing animation
│       │   │   │   ├── enforcement/ # Nudge card, Hard Lock screen
│       │   │   │   ├── budget/      # Dopamine budget dashboard
│       │   │   │   ├── insight/     # Weekly insight report
│       │   │   │   ├── social/      # Buddies, circles, challenges
│       │   │   │   ├── settings/    # All settings
│       │   │   │   └── onboarding/  # First-run setup
│       │   │   ├── service/
│       │   │   │   ├── UsageTrackingService.kt     # ForegroundService
│       │   │   │   └── TimerService.kt             # Intent countdown
│       │   │   ├── overlay/
│       │   │   │   └── OverlayManager.kt           # WindowManager overlay
│       │   │   ├── receiver/
│       │   │   │   ├── UnlockReceiver.kt           # ACTION_USER_PRESENT
│       │   │   │   └── BootReceiver.kt             # Restart service on boot
│       │   │   └── monitor/
│       │   │       ├── AppMonitor.kt               # Interface
│       │   │       └── PollingAppMonitor.kt         # UsageStatsManager polling
│       │   └── AndroidManifest.xml
│       ├── playstore/                               # Play Store flavor
│       │   └── AndroidManifest.xml                  # No AccessibilityService
│       └── github/                                  # GitHub flavor
│           ├── kotlin/dev/bilbo/android/monitor/
│           │   └── AccessibilityAppMonitor.kt       # Full power monitoring
│           ├── kotlin/dev/bilbo/android/service/
│           │   └── BilboAccessibilityService.kt
│           └── AndroidManifest.xml                  # Includes AccessibilityService
├── iosApp/                          # iOS application
│   ├── Bilbo/
│   │   ├── BilboApp.swift
│   │   ├── Views/
│   │   │   ├── HomeView.swift
│   │   │   ├── GatekeeperView.swift
│   │   │   ├── CheckInView.swift
│   │   │   ├── CooldownView.swift
│   │   │   ├── BudgetView.swift
│   │   │   ├── InsightView.swift
│   │   │   ├── SocialView.swift
│   │   │   ├── SettingsView.swift
│   │   │   └── OnboardingView.swift
│   │   └── Services/
│   │       ├── ScreenTimeManager.swift
│   │       └── NotificationManager.swift
│   ├── DeviceActivityMonitorExtension/
│   │   └── DeviceActivityMonitorExtension.swift
│   ├── ShieldConfigurationExtension/
│   │   └── ShieldConfigurationExtension.swift
│   └── ShieldActionExtension/
│       └── ShieldActionExtension.swift
├── backend/
│   └── supabase/
│       ├── migrations/
│       │   └── 001_initial_schema.sql
│       └── functions/
│           ├── create-invite/index.ts
│           ├── accept-invite/index.ts
│           ├── send-nudge/index.ts
│           ├── sync-status/index.ts
│           ├── create-circle/index.ts
│           ├── join-circle/index.ts
│           ├── create-challenge/index.ts
│           ├── compute-leaderboard/index.ts
│           ├── generate-digest/index.ts
│           └── ai-weekly-insight/index.ts
├── docs/
│   └── plans/
│       └── 2026-04-06-bilbo-implementation-plan.md
├── .github/
│   └── workflows/
│       ├── android-ci.yml
│       ├── ios-ci.yml
│       ├── shared-tests.yml
│       └── backend-ci.yml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## Architecture Decisions Record

### ADR-001: KMP over React Native
**Decision:** Kotlin Multiplatform for cross-platform shared logic, native UI per platform.
**Rationale:** App requires deep OS integration (AccessibilityService, UsageStatsManager, WindowManager overlays on Android; FamilyControls, DeviceActivityMonitor on iOS). KMP provides zero-abstraction native access while sharing ~60% of code (business logic, data, AI heuristics). Long-term maintainability and customizability outweigh shorter learning curve of RN.

### ADR-002: Dual Build Flavors for Android
**Decision:** `playstore` flavor uses UsageStatsManager polling only; `github` flavor adds AccessibilityService.
**Rationale:** Google Play Store's AccessibilityService policy is a rejection risk. Polling-based approach (5-second interval via UsageStatsManager) provides 95% of functionality with 1-3 second detection delay. AccessibilityService adds instant detection. Architecture uses `AppMonitor` interface with two implementations — flavor selection at build time.

### ADR-003: Supabase over Firebase
**Decision:** Supabase for backend (auth, database, realtime, edge functions).
**Rationale:** Open-source, PostgreSQL-based (real SQL, RLS policies), generous free tier, Edge Functions for AI relay, Realtime for buddy notifications. Self-hostable if needed. Firebase would work too but vendor lock-in is higher.

### ADR-004: Hybrid AI (Local + Cloud)
**Decision:** Three-tier intelligence: deterministic rules (Tier 1), local heuristic analysis (Tier 2), optional Anthropic API narrative (Tier 3).
**Rationale:** App must be fully functional offline. Tier 1-2 run entirely on-device with zero cloud dependency. Tier 3 is a premium enhancement for natural-language weekly insights, opt-in only, data anonymized before transmission.

### ADR-005: Privacy-First Social Layer
**Decision:** Raw usage data never leaves device. Social features sync computed status summaries only. Account creation optional — only required for Focus Buddies/Circles.
**Rationale:** A mental health app that leaks personal data is a liability. Users choose sharing level (Minimal/Moderate/Full) per buddy pair. No global profiles, no feeds, no follower counts.

### ADR-006: Anti-Addiction Social Design
**Decision:** Social features are bounded (max 3 buddy pairs, max 7 per circle, max 2 active challenges), time-limited (weekly leaderboard resets, challenges have end dates), and non-viral (no public profiles, no infinite feeds).
**Rationale:** A digital wellbeing app must not become another dopamine trap. Community Board is a read-only weekly digest, not a social feed.

---

## Domain Models

### Core Entities

```kotlin
// UsageSession — one continuous app usage period
data class UsageSession(
    val id: Long,
    val packageName: String,
    val appLabel: String,
    val startTime: Instant,
    val endTime: Instant?,
    val durationSeconds: Long,
    val wasIntended: Boolean,          // linked to an IntentDeclaration
    val intentDeclarationId: Long?,
    val category: AppCategory
)

enum class AppCategory { NUTRITIVE, NEUTRAL, EMPTY_CALORIES }

// EmotionalCheckIn — mood snapshot before app usage
data class EmotionalCheckIn(
    val id: Long,
    val timestamp: Instant,
    val emotion: Emotion,
    val intentDeclarationId: Long?,    // linked to what they did after
    val postSessionMood: Emotion?      // optional follow-up after session
)

enum class Emotion { HAPPY, CALM, BORED, STRESSED, ANXIOUS, SAD, LONELY }

// AppProfile — per-app configuration
data class AppProfile(
    val packageName: String,
    val appLabel: String,
    val category: AppCategory,
    val categorySource: CategorySource, // BUILTIN, USER_OVERRIDE, AI_SUGGESTED
    val timeLimitMinutes: Int?,
    val enforcementMode: EnforcementMode,
    val coolingOffEnabled: Boolean,
    val isBypassed: Boolean            // skip gatekeeper (Maps, Phone, etc.)
)

enum class EnforcementMode { NUDGE, HARD_LOCK }
enum class CategorySource { BUILTIN, USER_OVERRIDE, AI_SUGGESTED }

// IntentDeclaration — what the user said they'd do
data class IntentDeclaration(
    val id: Long,
    val timestamp: Instant,
    val declaredApp: String,
    val declaredDurationMinutes: Int,
    val actualDurationMinutes: Int?,
    val wasEnforced: Boolean,
    val enforcementType: EnforcementMode?,
    val wasOverridden: Boolean,
    val emotionalCheckInId: Long?
)

// DopamineBudget — daily focus points ledger
data class DopamineBudget(
    val date: LocalDate,
    val fpEarned: Int,
    val fpSpent: Int,
    val fpBonus: Int,
    val fpRolloverIn: Int,             // from previous day
    val fpRolloverOut: Int,            // to next day (50% of unspent, max via cap)
    val nutritiveMinutes: Int,
    val emptyCalorieMinutes: Int,
    val neutralMinutes: Int
)

// Focus Points Economy Constants
object FPEconomy {
    const val EARN_PER_NUTRITIVE_MINUTE = 1
    const val COST_PER_EMPTY_CALORIE_MINUTE = 1
    const val BONUS_BREATHING_EXERCISE = 3
    const val BONUS_ANALOG_ACCEPTED = 5
    const val BONUS_ACCURATE_INTENT = 2
    const val PENALTY_HARD_LOCK_OVERRIDE = 10
    const val PENALTY_NUDGE_IGNORE = 3
    const val DAILY_EARN_CAP = 60
    const val DAILY_BASELINE = 15
    const val ROLLOVER_PERCENTAGE = 0.5
    const val MIN_SESSION_SECONDS = 60
    const val STREAK_BONUS_7_DAY = 20
}

// AnalogSuggestion — offline activity recommendation
data class AnalogSuggestion(
    val id: Long,
    val text: String,
    val category: SuggestionCategory,
    val tags: List<String>,
    val timeOfDay: TimeOfDay?,         // null = any time
    val timesShown: Int,
    val timesAccepted: Int,
    val isCustom: Boolean              // user-created
)

enum class SuggestionCategory { PHYSICAL, CREATIVE, READING, SOCIAL, MINDFULNESS }
enum class TimeOfDay { MORNING, AFTERNOON, EVENING, NIGHT }

// WeeklyInsight — AI-generated weekly report
data class WeeklyInsight(
    val weekStart: LocalDate,
    val totalScreenTimeMinutes: Int,
    val nutritiveMinutes: Int,
    val emptyCalorieMinutes: Int,
    val averageDailyFP: Int,
    val streakDays: Int,
    val heuristicInsights: List<HeuristicInsight>,
    val aiNarrative: String?,          // null if cloud AI disabled
    val correlations: List<Correlation>
)

data class HeuristicInsight(
    val type: InsightType,
    val message: String,
    val confidence: Float              // 0.0 - 1.0
)

data class Correlation(
    val factorA: String,               // e.g., "anxiety"
    val factorB: String,               // e.g., "instagram_usage"
    val direction: CorrelationDirection,
    val strength: Float
)

enum class CorrelationDirection { POSITIVE, NEGATIVE }
enum class InsightType { TREND, CORRELATION, ANOMALY, STREAK, SUGGESTION }
```

### Social Entities

```kotlin
data class BuddyPair(
    val id: String,                    // UUID
    val userA: String,
    val userB: String,
    val sharingLevelA: SharingLevel,
    val sharingLevelB: SharingLevel,
    val createdAt: Instant
)

enum class SharingLevel { MINIMAL, MODERATE, FULL }

data class FocusCircle(
    val id: String,
    val name: String,
    val goal: String,
    val durationDays: Int,             // 7, 14, or 30
    val maxMembers: Int,               // 3-7
    val createdBy: String,
    val members: List<CircleMember>,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class CircleMember(
    val userId: String,
    val displayName: String,
    val sharingLevel: SharingLevel,
    val joinedAt: Instant
)

data class Challenge(
    val id: String,
    val circleId: String?,             // null for global/seasonal
    val name: String,
    val type: ChallengeType,
    val mode: ChallengeMode,
    val goalConfig: ChallengeGoal,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: ChallengeStatus
)

enum class ChallengeType { COOPERATIVE, COMPETITIVE, SEASONAL }
enum class ChallengeMode { DIGITAL_SUNSET, FOCUS_WEEK, ANALOG_DAY, STREAK_RACE, POINT_CHAMPION, COLD_TURKEY, CUSTOM }
enum class ChallengeStatus { UPCOMING, ACTIVE, COMPLETED, FAILED }

data class ChallengeGoal(
    val metric: String,                // e.g., "empty_calorie_minutes_below"
    val targetValue: Int,
    val timeConstraint: String?        // e.g., "after_21:00"
)
```

---

## Supabase Database Schema

```sql
-- 001_initial_schema.sql

-- Users (only created when Focus Buddies is activated)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name TEXT NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Invite codes for buddy pairing
CREATE TABLE invite_codes (
    code CHAR(6) PRIMARY KEY,
    creator_id UUID REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_by UUID REFERENCES users(id),
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Buddy pairs
CREATE TABLE buddy_pairs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_a UUID REFERENCES users(id) ON DELETE CASCADE,
    user_b UUID REFERENCES users(id) ON DELETE CASCADE,
    sharing_level_a TEXT NOT NULL DEFAULT 'minimal' CHECK (sharing_level_a IN ('minimal', 'moderate', 'full')),
    sharing_level_b TEXT NOT NULL DEFAULT 'minimal' CHECK (sharing_level_b IN ('minimal', 'moderate', 'full')),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'paused', 'ended')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_a, user_b)
);

-- Nudge events between buddies
CREATE TABLE nudge_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID REFERENCES users(id),
    receiver_id UUID REFERENCES users(id),
    pair_id UUID REFERENCES buddy_pairs(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('auto_exceeded', 'auto_override', 'auto_streak', 'request_nudge', 'encouragement')),
    message TEXT CHECK (length(message) <= 100),
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Status summaries (device pushes anonymized snapshots)
CREATE TABLE status_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    timestamp TIMESTAMPTZ NOT NULL,
    status TEXT NOT NULL,
    category TEXT,
    streak_days INT,
    fp_balance INT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Focus Circles
CREATE TABLE circles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    goal TEXT NOT NULL,
    duration_days INT NOT NULL CHECK (duration_days IN (7, 14, 30)),
    max_members INT NOT NULL DEFAULT 7 CHECK (max_members BETWEEN 3 AND 7),
    created_by UUID REFERENCES users(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    invite_code CHAR(6) UNIQUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE circle_members (
    circle_id UUID REFERENCES circles(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    sharing_level TEXT NOT NULL DEFAULT 'minimal',
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (circle_id, user_id)
);

-- Challenges
CREATE TABLE challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    circle_id UUID REFERENCES circles(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('cooperative', 'competitive', 'seasonal')),
    mode TEXT NOT NULL,
    goal_config JSONB NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status TEXT NOT NULL DEFAULT 'upcoming' CHECK (status IN ('upcoming', 'active', 'completed', 'failed')),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE challenge_progress (
    challenge_id UUID REFERENCES challenges(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    daily_snapshot JSONB NOT NULL,
    PRIMARY KEY (challenge_id, user_id, date)
);

-- Weekly digest (pre-computed global stats)
CREATE TABLE weekly_digest (
    week_start DATE PRIMARY KEY,
    total_users_active INT,
    collective_hours_saved INT,
    top_analog_suggestion TEXT,
    top_circle_achievement TEXT,
    user_tips JSONB,                   -- array of 3 anonymous tips
    generated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Row Level Security
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE buddy_pairs ENABLE ROW LEVEL SECURITY;
ALTER TABLE nudge_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE status_summaries ENABLE ROW LEVEL SECURITY;
ALTER TABLE circles ENABLE ROW LEVEL SECURITY;
ALTER TABLE circle_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE challenges ENABLE ROW LEVEL SECURITY;
ALTER TABLE challenge_progress ENABLE ROW LEVEL SECURITY;

-- Users can only see their own data
CREATE POLICY "Users see own profile" ON users FOR SELECT USING (auth.uid() = id);
CREATE POLICY "Users update own profile" ON users FOR UPDATE USING (auth.uid() = id);

-- Buddy pairs visible to both participants
CREATE POLICY "See own pairs" ON buddy_pairs FOR SELECT
    USING (auth.uid() = user_a OR auth.uid() = user_b);

-- Nudge events visible to sender and receiver
CREATE POLICY "See own nudges" ON nudge_events FOR SELECT
    USING (auth.uid() = sender_id OR auth.uid() = receiver_id);

-- Status summaries: only your buddies can see yours
CREATE POLICY "Buddies see status" ON status_summaries FOR SELECT
    USING (
        auth.uid() = user_id OR
        EXISTS (
            SELECT 1 FROM buddy_pairs
            WHERE status = 'active'
            AND ((user_a = auth.uid() AND user_b = status_summaries.user_id)
              OR (user_b = auth.uid() AND user_a = status_summaries.user_id))
        )
    );

-- Circle members see circle data
CREATE POLICY "Circle members see circle" ON circles FOR SELECT
    USING (
        EXISTS (SELECT 1 FROM circle_members WHERE circle_id = circles.id AND user_id = auth.uid())
    );
```

---

## Intelligence Engine Detail

### Tier 1: Deterministic Rules (RuleEngine.kt)

```kotlin
class RuleEngine(
    private val intentRepo: IntentRepository,
    private val budgetRepo: BudgetRepository,
    private val appProfileRepo: AppProfileRepository
) {
    fun evaluateAppLaunch(packageName: String): LaunchDecision {
        val profile = appProfileRepo.getProfile(packageName)

        // Bypassed apps (Maps, Phone, etc.) — always allow
        if (profile.isBypassed) return LaunchDecision.Allow

        // Hard-locked apps in cooldown — always block
        if (isInCooldown(packageName)) return LaunchDecision.Block(remainingCooldownMinutes)

        // Check FP balance for Empty Calories
        if (profile.category == AppCategory.EMPTY_CALORIES) {
            val budget = budgetRepo.getToday()
            val balance = budget.currentBalance()
            if (balance <= 0) return LaunchDecision.InsufficientFP(balance)
        }

        // Otherwise — proceed to Intent Gatekeeper
        return LaunchDecision.RequiresIntent
    }

    fun evaluateTimerExpiry(declaration: IntentDeclaration): EnforcementAction {
        val profile = appProfileRepo.getProfile(declaration.declaredApp)
        return when (profile.enforcementMode) {
            EnforcementMode.NUDGE -> EnforcementAction.ShowNudge
            EnforcementMode.HARD_LOCK -> EnforcementAction.HardLock(cooldownMinutes = 30)
        }
    }
}
```

### Tier 2: Heuristic Engine (runs nightly)

```kotlin
class HeuristicEngine(
    private val usageRepo: UsageRepository,
    private val emotionRepo: EmotionRepository,
    private val intentRepo: IntentRepository
) {
    fun analyzeWeek(weekStart: LocalDate): List<HeuristicInsight> {
        val insights = mutableListOf<HeuristicInsight>()
        val sessions = usageRepo.getSessionsForWeek(weekStart)
        val checkins = emotionRepo.getCheckInsForWeek(weekStart)

        // 1. Emotion-usage correlation
        val emotionAppCorrelations = computeEmotionAppCorrelations(checkins, sessions)
        emotionAppCorrelations
            .filter { it.strength > 0.6 }
            .forEach { corr ->
                insights.add(HeuristicInsight(
                    type = InsightType.CORRELATION,
                    message = "When you feel ${corr.factorA}, you tend to use ${corr.factorB} more.",
                    confidence = corr.strength
                ))
            }

        // 2. Day-of-week trends
        val dailyTotals = sessions.groupBy { it.startTime.dayOfWeek }
        val avgDaily = dailyTotals.values.map { it.sumOf { s -> s.durationSeconds } }.average()
        dailyTotals.forEach { (day, daySessions) ->
            val dayTotal = daySessions.sumOf { it.durationSeconds }
            if (dayTotal > avgDaily * 1.4) {
                insights.add(HeuristicInsight(
                    type = InsightType.TREND,
                    message = "Your screen time on ${day}s is ${((dayTotal / avgDaily - 1) * 100).toInt()}% above your average.",
                    confidence = 0.8f
                ))
            }
        }

        // 3. Intent accuracy tracking
        val declarations = intentRepo.getDeclarationsForWeek(weekStart)
        val accurateCount = declarations.count { it.actualDurationMinutes != null &&
            it.actualDurationMinutes <= it.declaredDurationMinutes * 1.2 }
        val accuracyRate = if (declarations.isNotEmpty()) accurateCount.toFloat() / declarations.size else 0f
        insights.add(HeuristicInsight(
            type = InsightType.TREND,
            message = "Your intent accuracy this week: ${(accuracyRate * 100).toInt()}%. " +
                if (accuracyRate > 0.7) "You're staying true to your plans." else "Consider setting more realistic time limits.",
            confidence = 0.9f
        ))

        // 4. Post-session mood tracking
        // Compare pre-session emotion to post-session mood for Empty Calorie apps
        // Flag patterns where mood worsens after scrolling

        // 5. Time-of-day patterns
        // Identify peak usage windows and suggest alternatives

        return insights
    }
}
```

### Tier 3: Cloud AI Prompt (sent to Anthropic via Supabase Edge Function)

```
System: You are a compassionate digital wellbeing coach. Write a brief weekly
reflection (max 200 words) based on the user's anonymized usage data. Be warm,
non-judgmental, and actionable. Focus on ONE key pattern and ONE concrete
suggestion. Never shame or lecture.

User: Here is my week summary:
- Total screen time: {total_minutes} minutes ({change_vs_last_week}% vs last week)
- Nutritive time: {nutritive_minutes} min | Empty Calories: {empty_minutes} min
- Focus Points earned: {fp_earned} | spent: {fp_spent}
- Intent accuracy: {accuracy_pct}%
- Streak: {streak_days} days
- Emotional patterns: {emotion_summary}
- Key correlations: {correlations_json}
- Day with most usage: {peak_day} ({peak_day_minutes} min)

Write a weekly reflection addressing the most important pattern you notice.
```

---

## Phased Implementation

### Phase 0: Foundation (Week 1)

**Goal:** Empty KMP project that builds on both platforms with CI running.

**Task 0.1 — Initialize KMP Project**
- Create repo `Prekzursil/bilbo-app` on GitHub
- Initialize with KMP template (Kotlin 2.0+, Gradle 8.x)
- Configure `shared/`, `androidApp/`, `iosApp/` modules
- Verify `./gradlew build` succeeds for all modules

**Task 0.2 — Android Build Flavors**
- Add `playstore` and `github` product flavors in `androidApp/build.gradle.kts`
- Create flavor-specific source sets (`src/playstore/`, `src/github/`)
- Add `BuildConfig.USE_ACCESSIBILITY_SERVICE` flag
- Verify both flavor APKs build: `./gradlew assemblePlaystoreDebug assembleGithubDebug`

**Task 0.3 — SQLDelight Setup**
- Add SQLDelight Gradle plugin and dependencies
- Create empty `BilboDatabase.sq` in `shared/src/commonMain/sqldelight/`
- Configure Android and iOS database drivers in platform-specific source sets
- Verify database generation: `./gradlew generateCommonMainBilboDatabaseInterface`

**Task 0.4 — CI Pipeline**
- Create `.github/workflows/shared-tests.yml` — runs `shared` module tests
- Create `.github/workflows/android-ci.yml` — builds both flavors, runs Android tests
- Create `.github/workflows/ios-ci.yml` — builds iOS app (macOS runner)
- Add Detekt for Kotlin linting
- Add SonarCloud integration on `shared/` module
- Add Codecov integration
- Push and verify all workflows pass green

**Task 0.5 — Project Documentation**
- Write `README.md` with architecture overview, setup instructions, build commands
- Add `CONTRIBUTING.md` with coding standards
- Add `LICENSE` (choose license)
- Commit and push

---

### Phase 1: Data Layer (Week 2)

**Goal:** All domain models and local persistence, fully tested.

**Task 1.1 — SQLDelight Schema**
- Define all tables: `usage_sessions`, `emotional_checkins`, `app_profiles`, `intent_declarations`, `dopamine_budgets`, `analog_suggestions`, `weekly_insights`, `heuristic_insights`
- Add indexes on frequently queried columns (package_name, timestamp, date)
- Generate Kotlin interfaces

**Task 1.2 — Repository Interfaces**
- Create repository interfaces in `commonMain`: `UsageRepository`, `EmotionRepository`, `BudgetRepository`, `IntentRepository`, `AppProfileRepository`, `SuggestionRepository`
- Define suspend functions for all CRUD operations
- Create SQLDelight-backed implementations

**Task 1.3 — Preferences / Settings**
- Implement `BilboPreferences` using DataStore (Android) / NSUserDefaults (iOS)
- Settings: enforcement mode defaults, FP economy toggles, sharing levels, cloud AI toggle, notification preferences, bypass list

**Task 1.4 — Seed Data**
- Create `default_app_classifications.json` with ~200 popular apps pre-categorized
- Create `default_analog_suggestions.json` with ~50 suggestions tagged by category and time-of-day
- Write loader that seeds DB on first launch

**Task 1.5 — Unit Tests**
- Test every repository operation (insert, query, update, delete)
- Test preferences read/write
- Test seed data loading
- Target: 90%+ coverage on data layer

---

### Phase 2: Usage Tracking Core (Weeks 3-4)

**Goal:** Background service collecting real app usage data into SQLDelight.

**Task 2.1 — AppMonitor Interface**
- Define `AppMonitor` interface in `shared/commonMain`:
  ```kotlin
  interface AppMonitor {
      fun getCurrentForegroundApp(): AppInfo?
      fun onAppChanged(callback: (AppInfo) -> Unit)
  }
  ```
- Create `AppInfo` data class (packageName, appLabel, category)

**Task 2.2 — PollingAppMonitor (Play Store flavor)**
- Implement `PollingAppMonitor` using `UsageStatsManager`
- Poll every 5 seconds for foreground app
- Request `PACKAGE_USAGE_STATS` permission with settings redirect
- Handle edge cases: no permission, no data, screen off

**Task 2.3 — AccessibilityAppMonitor (GitHub flavor)**
- Implement `BilboAccessibilityService` extending `AccessibilityService`
- Listen for `TYPE_WINDOW_STATE_CHANGED` events
- Extract package name from `AccessibilityEvent`
- Implement `AccessibilityAppMonitor` wrapping the service

**Task 2.4 — UsageTrackingService (ForegroundService)**
- Create `UsageTrackingService` with persistent notification
- Inject appropriate `AppMonitor` implementation via Hilt based on build flavor
- Session detection: track app transitions, create `UsageSession` entries
- Handle service lifecycle: start on boot, restart on kill, battery optimization exemption

**Task 2.5 — iOS DeviceActivityMonitor**
- Request `AuthorizationCenter.shared.requestAuthorization(for: .individual)`
- Implement `DeviceActivityMonitorExtension`
- Bridge usage data back to shared KMP module via expected/actual declarations

**Task 2.6 — Integration Tests**
- Test session boundary detection (app A → app B = close session A, open session B)
- Test screen off/on handling
- Test flavor switching (verify AccessibilityService only in github flavor)
- Manual test on real device: use phone normally, verify usage data in DB

---

### Phase 3: Intent Gatekeeper (Weeks 5-6)

**Goal:** Overlay appears on app launch asking "What do you want to do?" + "For how long?"

**Task 3.1 — Unlock Detection (Android)**
- Create `UnlockReceiver` listening for `ACTION_USER_PRESENT`
- Create `BootReceiver` to register receivers on device boot
- Wire to `UsageTrackingService` lifecycle

**Task 3.2 — Overlay System (Android)**
- Implement `OverlayManager` using `WindowManager` + `TYPE_APPLICATION_OVERLAY`
- Request `DRAW_OVER_OTHER_APPS` permission with settings redirect
- Render Compose content inside overlay window
- Handle overlay lifecycle (show, dismiss, cleanup)

**Task 3.3 — Gatekeeper UI**
- Design Intent Gatekeeper Compose screen:
  - App icon + name at top
  - "What's your intention?" — optional text input
  - Duration picker: 5 / 10 / 15 / 20 / 30 / 60 min (chip selector)
  - "Start" button → creates IntentDeclaration, dismisses overlay, opens app
  - "Not now" button → sends user to home screen
- Smooth entry/exit animations (slide up from bottom)

**Task 3.4 — Bypass Logic**
- Check `AppProfile.isBypassed` before showing gatekeeper
- Default bypass list: Phone, Messages, Maps, Camera, Settings, Clock, Calculator
- User-configurable in Settings

**Task 3.5 — iOS Gatekeeper**
- Implement as full-screen notification → app redirect flow
- Local notification with custom category + actions on app launch detection
- Notification tap opens Bilbo's GatekeeperView with same UI

**Task 3.6 — Tests**
- Test IntentDeclaration creation with all fields
- Test bypass list filtering
- Test timer initialization from declaration
- UI tests for gatekeeper screen

---

### Phase 4: Enforcement Engine (Weeks 7-8)

**Goal:** Timer countdown with Nudge and Hard Lock enforcement.

**Task 4.1 — Timer Service**
- Create `TimerService` managing active intent countdowns
- Countdown from declared duration in background
- 2-minute warning notification before expiry
- Timer persists across app switches (runs independently)

**Task 4.2 — Nudge Enforcement**
- On timer expiry (Nudge mode):
  - Push notification: "Your {duration} on {app} is up"
  - Optional grayscale overlay filter
  - Dismissible "Time's up" card overlay
  - Log enforcement event

**Task 4.3 — Hard Lock Enforcement**
- On timer expiry (Hard Lock mode):
  - Opaque overlay covering entire screen
  - "Time's up. {app} is locked for 30 minutes."
  - Show Analog Suggestion (wired in Phase 7)
  - Navigate user to home screen
  - If user reopens locked app → re-show blocking overlay via AppMonitor
  - Cooldown timer: 30 minutes (configurable)

**Task 4.4 — Override Mechanism**
- Override button on Hard Lock screen (hidden behind confirmation dialog)
- "Override costs 10 Focus Points. Current balance: {FP}. Are you sure?"
- If confirmed: dismiss lock, deduct FP (wired in Phase 6), log override
- If insufficient FP: "You don't have enough Focus Points to override."

**Task 4.5 — iOS Enforcement**
- `ShieldConfigurationDataSource` for custom blocking UI
- `ShieldAction` extension for override handling
- Map enforcement modes to iOS Screen Time shield behaviors

**Task 4.6 — Tests**
- Test timer accuracy (countdown, warning at 2 min, expiry)
- Test nudge vs hard lock branching based on AppProfile
- Test cooldown persistence and expiry
- Test override flow with FP deduction
- Test re-intercept when locked app reopened

---

### Phase 5: Emotional Intelligence (Weeks 9-10)

**Goal:** Emotional check-in flow + cooling-off breathing screen.

**Task 5.1 — Emotional Check-in UI**
- Insert between Intent Gatekeeper confirmation and app launch
- Screen: "How are you feeling right now?" with emotion grid (7 emotions as illustrated icons/emoji)
- Quick tap selection → saved as `EmotionalCheckIn` linked to `IntentDeclaration`
- "Skip" option (check-in is optional by default, configurable)
- Smooth transition animation gatekeeper → check-in → app/cooling-off

**Task 5.2 — AI Intervention Card**
- After emotion selection, if emotion is negative (Bored/Stressed/Anxious/Sad/Lonely) AND target app is Empty Calories:
  - Query Tier 2 heuristic history for this emotion × app pattern
  - If pattern exists: show intervention card
    - "When you feel {emotion}, you tend to use {app} for {avg_duration}. Afterward you usually feel {post_mood}."
    - "Would you like to try 2 minutes of breathing instead?"
    - Two buttons: "Yes, breathe" → cooling-off → home | "Continue to {app}" → proceed
  - If no pattern yet (insufficient data): skip card, collect data silently

**Task 5.3 — Cooling-off Period**
- Full-screen 10-second breathing animation for Empty Calorie apps
- Compose Canvas animation: expanding/contracting circle with "Breathe in... Breathe out..." text
- Cannot skip, cannot dismiss, no back button — hard 10-second wall
- After 10 seconds: app opens normally
- Setting: enable/disable per app in AppProfile
- Breathing exercise completion → +3 FP bonus

**Task 5.4 — Post-Session Mood (Optional)**
- When enforcement fires (nudge or hard lock), optionally ask: "How do you feel now?"
- Quick emoji tap, stored as `postSessionMood` on the original `EmotionalCheckIn`
- Powers Tier 2 before/after mood analysis

**Task 5.5 — Tests**
- Test emotion → intervention card logic
- Test cooling-off timer (exactly 10 seconds, no early exit)
- Test FP bonus for breathing exercise
- Test check-in data persistence and linkage to IntentDeclaration

---

### Phase 6: Dopamine Economy (Weeks 11-12)

**Goal:** Full Focus Points system with earning, spending, and budget enforcement.

**Task 6.1 — FocusPointsEngine**
- Core engine in `shared/commonMain`
- `earnPoints(session: UsageSession)` — calculates FP earned from Nutritive usage
- `spendPoints(session: UsageSession)` — calculates FP spent on Empty Calories
- `applyBonus(type: BonusType)` — breathing, analog accepted, accurate intent
- `applyPenalty(type: PenaltyType)` — override, nudge ignore
- `getBalance(date: LocalDate)` — current FP balance
- All operations write to `DopamineBudget` table

**Task 6.2 — Daily Lifecycle**
- Midnight reset job (WorkManager on Android, BackgroundTasks on iOS)
- Calculate rollover: 50% of unspent FP carries to next day
- Apply daily baseline: +15 FP
- Create new `DopamineBudget` row for new day

**Task 6.3 — Budget Check in Gatekeeper**
- Wire FP balance check into Intent Gatekeeper flow
- If Empty Calorie app and balance > 0: show cost preview ("This will cost ~{duration} FP")
- If Empty Calorie app and balance = 0: block with "Earn more Focus Points first"
- Real-time balance deduction as user uses the app

**Task 6.4 — Anti-Gaming Detection**
- `GamingDetector` in Tier 2 heuristics
- Flag: Nutritive app sessions < 60 seconds (no FP earned)
- Flag: same Nutritive app opened > 20 times in one day
- Flag: screen off during "active" Nutritive session (pause earning)
- Daily earn cap enforcement: stop earning at 60 FP

**Task 6.5 — AppClassifier**
- `AppClassifier` manages app categorization
- Built-in defaults loaded from seed data
- User override UI in Settings → per-app category selector
- Tier 2 reclassification suggestions after 2 weeks of data

**Task 6.6 — Budget Dashboard UI**
- Today's FP balance (large number, color-coded green/yellow/red)
- Earn vs spend breakdown (horizontal bar)
- Weekly FP trend chart (line graph)
- App category pie chart (Nutritive / Neutral / Empty Calories minutes)
- Streak counter with weekly bonus progress

**Task 6.7 — Tests**
- Test earning calculations with cap enforcement
- Test spending deductions
- Test rollover math
- Test anti-gaming detection rules
- Test budget check integration with gatekeeper
- Test daily reset lifecycle

---

### Phase 7: Analog Alternatives (Week 13)

**Goal:** Context-aware offline suggestions when limits are hit.

**Task 7.1 — Suggestion Engine**
- `SuggestionEngine` in shared module
- Input: user interest tags, current time of day, last shown suggestions
- Output: ranked list of relevant `AnalogSuggestion`s
- Algorithm: filter by time-of-day → filter by interest tags → sort by (least recently shown + highest acceptance rate) → return top 3

**Task 7.2 — Integration Points**
- Show suggestions on: Hard Lock screen, Nudge card, zero-FP block screen
- Display as cards: suggestion text + "I'll do this" / "Show another" buttons
- "I'll do this" → +5 FP bonus, increment `timesAccepted`
- "Show another" → cycle to next suggestion, increment `timesShown`

**Task 7.3 — Custom Suggestions**
- Settings screen: "Add your own suggestion"
- Text input + category picker + time-of-day picker
- Custom suggestions mixed into the regular rotation

**Task 7.4 — User Interests Onboarding**
- During first-run setup (Phase 11): "What do you enjoy offline?"
- Multi-select: Reading, Exercise, Cooking, Art, Music, Nature, Social, Meditation, Gaming (physical), Learning
- Stored in preferences, editable in settings

**Task 7.5 — Tests**
- Test suggestion ranking algorithm
- Test time-of-day filtering
- Test interest tag matching
- Test FP bonus on acceptance
- Test custom suggestion creation

---

### Phase 8: AI Intelligence Engine (Weeks 14-15)

**Goal:** Local pattern detection + cloud-powered weekly narrative.

**Task 8.1 — Tier 2 Nightly Batch Job**
- Schedule via WorkManager (Android) / BackgroundTasks (iOS)
- Runs at 3 AM local time daily
- Executes `HeuristicEngine.analyzeWeek()` for trailing 7 days
- Stores `HeuristicInsight` entries in DB
- Updates correlation data for emotion × app patterns

**Task 8.2 — Correlation Analyzer**
- `CorrelationAnalyzer` computes Pearson correlation between:
  - Emotional state and app category usage duration
  - Time of day and app category preference
  - Intent accuracy and day of week
  - Pre-session mood and post-session mood by app
- Minimum data threshold: 14 data points before generating correlation insight
- Confidence scoring: strength of correlation (0.0 - 1.0)

**Task 8.3 — Cloud AI Relay (Supabase Edge Function)**
- `ai-weekly-insight/index.ts` Edge Function
- Receives anonymized weekly summary JSON from device
- Calls Anthropic API (`claude-sonnet-4-20250514`) with curated prompt
- Returns 200-word narrative
- Rate limiting: 1 request per user per week
- Error handling: timeout, API failure → return null (device shows Tier 2 fallback)

**Task 8.4 — Weekly Insight UI**
- Scrollable card-based report:
  - Hero card: AI narrative (or Tier 2 template if cloud disabled)
  - Stats cards: total screen time, Nutritive vs Empty split, FP earned/spent
  - Trend chart: daily screen time for the week
  - Correlation cards: top 2-3 heuristic insights
  - Streak card: current streak + history
- Pull-to-refresh to regenerate (rate-limited)

**Task 8.5 — Data Anonymization Preview**
- Settings → "See what we send to AI" → shows exact JSON payload
- User can review before enabling cloud AI
- Toggle: "Enable AI-powered insights" (default off)

**Task 8.6 — Tests**
- Test correlation math with known datasets
- Test heuristic insight generation thresholds
- Test cloud fallback when API unavailable
- Test anonymization (verify no PII in payload)
- Test weekly insight card rendering with mock data

---

### Phase 9: Social Layer (Weeks 16-17)

**Goal:** Supabase backend + Buddy Pairs + Focus Circles + Challenges + Leaderboard + Digest.

**Task 9.1 — Supabase Project Setup**
- Create Supabase project
- Run `001_initial_schema.sql` migration
- Configure RLS policies
- Set up auth (email + Google OAuth)
- Configure FCM (Android) + APNs (iOS) for push notifications
- Deploy Edge Functions scaffold

**Task 9.2 — Auth Integration**
- Supabase Auth SDK integration in shared module (Ktor client)
- Sign-in flow: only triggered when user taps "Find a Focus Buddy"
- Store auth token securely (Android Keystore / iOS Keychain)
- Profile creation: display name + optional avatar

**Task 9.3 — Buddy Pairs**
- Edge Functions: `create-invite`, `accept-invite`
- Invite code generation (6 alphanumeric characters, 24h expiry)
- Pairing flow UI: generate code → share → enter code → confirm
- Sharing level selector per pair
- Maximum 3 active pairs enforcement
- Status summary sync: device pushes computed status to `status_summaries` table

**Task 9.4 — Nudge System**
- Edge Function: `send-nudge`
- Automatic nudge triggers (exceeded limit, overrode lock, streak milestone)
- Manual nudge: "Request a nudge" button, "Send encouragement" with 100-char message
- Push notification delivery via FCM/APNs
- Nudge inbox UI: list of received nudges with read/unread state

**Task 9.5 — Focus Circles**
- Edge Functions: `create-circle`, `join-circle`
- Create circle: name, goal description, duration (7/14/30 days), max members (3-7)
- Join via invite code (same pattern as buddy pairs)
- Circle dashboard: member list, aggregate progress, days remaining

**Task 9.6 — Challenges**
- Edge Function: `create-challenge`
- Challenge creation UI: pick type (cooperative/competitive), pick mode, set goal, set dates
- Daily progress sync from device to `challenge_progress` table
- Challenge completion detection: Edge Function checks daily
- Results screen: who won (competitive) or pass/fail (cooperative)

**Task 9.7 — Leaderboard**
- Edge Function: `compute-leaderboard` (runs daily via Supabase cron)
- Circle-scoped only (no global leaderboard)
- Weekly reset every Monday
- Multiple categories: "Most FP Earned", "Best Streak", "Most Improved", "Most Analog Time"
- Leaderboard UI: card per category, member ranking, opt-in anonymous mode

**Task 9.8 — Community Digest**
- Edge Function: `generate-digest` (runs Monday 6 AM UTC via Supabase cron)
- Aggregates anonymous global stats: total users active, collective hours saved
- Selects top analog suggestion, top circle achievement
- Curates 3 anonymous user-submitted tips (manual moderation queue for V2, auto-filtered for now)
- Digest card UI: read-only, 30-second read, no interaction beyond dismiss

**Task 9.9 — Tests**
- Test invite code generation + expiry
- Test pairing flow end-to-end
- Test RLS policies (user A cannot see user B's data unless paired)
- Test nudge delivery pipeline
- Test challenge progress calculation
- Test leaderboard ranking algorithm
- Test digest generation Edge Function

---

### Phase 10: iOS Parity (Week 18)

**Goal:** iOS-specific implementations for all features.

**Task 10.1 — FamilyControls Authorization**
- Request authorization on first launch
- Handle denial gracefully (app works in tracking-only mode)
- Re-request flow in settings

**Task 10.2 — DeviceActivityMonitor Extension**
- Configure device activity schedules
- Monitor app usage events
- Bridge data to shared KMP UsageRepository

**Task 10.3 — Shield Configuration**
- Custom shield UI for Hard Lock
- Shield action handlers for override flow
- Cooling-off period via timed shield with breathing animation

**Task 10.4 — iOS Intent Gatekeeper**
- Notification-based flow: detect app launch via DeviceActivityMonitor
- Show local notification with custom UI
- Notification action opens GatekeeperView in Bilbo
- After declaration, unshield the target app temporarily

**Task 10.5 — iOS Push Notifications**
- APNs configuration in Xcode project
- Supabase push notification integration
- Notification categories for nudge types

**Task 10.6 — SwiftUI Polish**
- Ensure all screens render correctly on iOS
- iOS-specific navigation patterns (tab bar vs Android bottom nav)
- Dynamic Type support
- Dark mode / light mode

**Task 10.7 — iOS Integration Tests**
- Test full flow on iOS simulator: gatekeeper → check-in → app → enforcement
- Test Screen Time API integration
- Test push notification delivery

---

### Phase 11: Polish & Ship (Weeks 19-20)

**Goal:** Onboarding, settings, accessibility, store submission.

**Task 11.1 — Onboarding Flow**
- Screen 1: Welcome — "Take control of your screen time" with app overview
- Screen 2: Permissions — request usage stats, overlay, notifications (explain why each is needed)
- Screen 3: Interests — multi-select offline interests for Analog Suggestions
- Screen 4: App Classification — review top 10 most-used apps, confirm/change categories
- Screen 5: First Intent — demo the Intent Gatekeeper with a guided first declaration
- Skip option for power users

**Task 11.2 — Settings Screen**
- Enforcement: default mode (Nudge/Hard Lock), per-app overrides, cooldown duration
- Economy: enable/disable FP system, adjust daily baseline, toggle anti-gaming
- Emotional: enable/disable check-in, enable/disable cooling-off
- AI: enable/disable cloud insights, view anonymization payload
- Social: manage buddy pairs, circles, sharing levels
- Notifications: toggle types, quiet hours
- Data: export all data as JSON, delete all data, delete account
- About: version, licenses, privacy policy link

**Task 11.3 — Accessibility**
- TalkBack (Android) / VoiceOver (iOS) support on all screens
- Content descriptions on all interactive elements
- Dynamic text size support
- High contrast mode
- Reduced motion option (simplified breathing animation)

**Task 11.4 — Branding & Identity**
- App icon: Bilbo logo (lightning bolt + brain motif)
- Splash screen with brand animation
- Consistent color system: calming palette (soft blues, greens, warm neutrals)
- Typography: clean, readable, friendly

**Task 11.5 — Error Handling & Crash Reporting**
- Sentry SDK integration (Android + iOS)
- Global error boundary with user-friendly crash screen
- Offline mode handling: queue sync operations, retry on connectivity

**Task 11.6 — Analytics**
- PostHog SDK integration
- Track: onboarding completion, feature adoption, FP economy engagement
- No PII in analytics events
- User opt-out in settings

**Task 11.7 — Store Submission**
- **Play Store:**
  - Build signed `playstore` release APK/AAB
  - Write store listing: title, description, screenshots (phone + tablet)
  - Privacy policy page
  - Accessibility Declaration Form (justify usage stats permission)
  - Data safety section (no data shared, usage data collected locally)
  - Submit for review

- **GitHub Releases:**
  - Build signed `github` release APK
  - Write release notes documenting AccessibilityService features
  - Publish as GitHub Release with attached APK

- **iOS App Store:**
  - Archive and upload via Xcode / Transporter
  - Write App Store listing
  - App Review guidelines compliance check
  - Screen Time API usage justification
  - Submit for review

**Task 11.8 — Launch Checklist**
- [ ] All CI pipelines green
- [ ] Coverage > 80% on shared module
- [ ] Zero critical/high Sonar issues
- [ ] Sentry configured and receiving test events
- [ ] PostHog configured and receiving test events
- [ ] Supabase RLS policies verified
- [ ] Play Store listing approved
- [ ] GitHub Release published
- [ ] iOS App Store listing approved
- [ ] README updated with download links

---

## Timeline Summary

| Phase | Scope | Duration | Cumulative |
|-------|-------|----------|------------|
| 0 | Foundation | Week 1 | Week 1 |
| 1 | Data Layer | Week 2 | Week 2 |
| 2 | Usage Tracking | Weeks 3-4 | Week 4 |
| 3 | Intent Gatekeeper | Weeks 5-6 | Week 6 |
| 4 | Enforcement Engine | Weeks 7-8 | Week 8 |
| 5 | Emotional Intelligence | Weeks 9-10 | Week 10 |
| 6 | Dopamine Economy | Weeks 11-12 | Week 12 |
| 7 | Analog Alternatives | Week 13 | Week 13 |
| 8 | AI Intelligence | Weeks 14-15 | Week 15 |
| 9 | Social Layer | Weeks 16-17 | Week 17 |
| 10 | iOS Parity | Week 18 | Week 18 |
| 11 | Polish & Ship | Weeks 19-20 | Week 20 |

**Total: ~20 weeks (5 months) for a solo developer working full-time.**

---

## Risk Register

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Play Store rejects AccessibilityService usage | Medium | Low | Dual flavor architecture — playstore flavor doesn't use it |
| iOS Screen Time API limitations block features | High | Medium | Accept iOS feature asymmetry, document differences |
| Google tightens UsageStatsManager access | High | Low | Monitor Android developer blog, prepare alternative tracking |
| Supabase free tier limits exceeded | Medium | Low | Self-host if needed, monitor usage |
| Anthropic API costs spike | Low | Low | Rate limit to 1 call/user/week, local fallback always works |
| Users game the FP economy | Medium | Medium | Anti-gaming detection, daily caps, minimum session length |
| App itself becomes addictive (checking FP, streaks) | High | Medium | No infinite feeds, bounded social, weekly digest only, daily notification cap |
| Kotlin Multiplatform learning curve delays | Medium | High | Start with shared data layer (familiar patterns), learn KMP idioms incrementally |

---

## Success Metrics (Post-Launch)

- **Adoption:** 1,000 installs in first month
- **Retention:** 40% Day-7 retention, 20% Day-30 retention
- **Engagement:** Average user completes 3+ Intent Declarations per day
- **Impact:** Average user reduces Empty Calorie screen time by 20% after 2 weeks
- **Economy:** 60%+ of users engage with FP system (earn or spend points)
- **Social:** 30%+ of users create at least one Buddy Pair
- **AI:** 50%+ of cloud-enabled users read their Weekly Insight
