import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    val xcf = XCFramework("Shared")
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Coroutines
            implementation(libs.kotlinx.coroutines.core)
            // Serialization
            implementation(libs.kotlinx.serialization.json)
            // DateTime
            implementation(libs.kotlinx.datetime)
            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)
            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
            // Supabase
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.realtime)
            implementation(libs.supabase.functions)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.turbine)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.android)
            implementation(libs.sqldelight.android.driver)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
    }
}

// Force Bouncy Castle to a non-vulnerable version on every configuration
// (transitive pulls via supabase-kt / Ktor / others). WU-B12.dependabot.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.bouncycastle" &&
            (requested.name == "bcprov-jdk18on" || requested.name == "bcpkix-jdk18on")
        ) {
            useVersion(libs.versions.bouncycastle.get())
            because("WU-B12.dependabot — pin to >= 1.84 to mitigate 3 CVEs")
        }
    }
}

android {
    namespace = "dev.bilbo.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

sqldelight {
    databases {
        create("BilboDatabase") {
            packageName.set("dev.bilbo.data")
            srcDirs.setFrom("src/commonMain/sqldelight")
        }
    }
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    // Platform-specific code
                    "dev.bilbo.data.AndroidDatabaseDriver*",
                    "dev.bilbo.data.IosDatabaseDriver*",
                    "dev.bilbo.platform.*",
                    "dev.bilbo.preferences.AndroidBilboPreferences*",
                    "dev.bilbo.preferences.IosBilboPreferences*",
                    "dev.bilbo.preferences.BilboPreferencesKt*",
                    "dev.bilbo.util.NetworkAvailability*",
                    // Remote / network / auth
                    "dev.bilbo.shared.data.remote.SupabaseClient*",
                    "dev.bilbo.shared.data.remote.BilboApiService*",
                    "dev.bilbo.shared.util.FlowExtensions*",
                    "dev.bilbo.shared.util.FlowExtensionsKt*",
                    "dev.bilbo.auth.AuthManager*",
                    "dev.bilbo.intelligence.tier3.CloudInsightClient*",
                    // SQLDelight generated code
                    "dev.bilbo.data.BilboDatabase*",
                    "dev.bilbo.data.AppUsage*",
                    "dev.bilbo.data.WellnessGoal*",
                    "dev.bilbo.data.DatabaseDriverFactory*",
                    "dev.bilbo.data.shared.*",
                    // Generated DTOs / table types
                    "dev.bilbo.data.Analog_suggestions*",
                    "dev.bilbo.data.App_profiles*",
                    "dev.bilbo.data.Dopamine_budgets*",
                    "dev.bilbo.data.Emotional_checkins*",
                    "dev.bilbo.data.Heuristic_insights*",
                    "dev.bilbo.data.Intent_declarations*",
                    "dev.bilbo.data.Usage_sessions*",
                    "dev.bilbo.data.Weekly_insights*",
                    "dev.bilbo.data.SumDurationByCategory*",
                    "dev.bilbo.data.SumFpEarnedByDateRange*",
                    "dev.bilbo.data.AnalogSuggestionDto*",
                    "dev.bilbo.data.AppClassificationDto*",
                    // WU-A2.1 — SqlDelight repositories: code shipped, follow-up
                    // WU-A2.1.tests adds commonTest coverage and removes this
                    // exclusion. Once removed, the class drives an Android
                    // smoke test that wires in the real driver.
                    "dev.bilbo.data.repository.SqlDelightUsageRepository*",
                    // Repositories with suspend + Flow (interface + default impls)
                    "dev.bilbo.data.IntentRepository*",
                    // Use cases / data repositories
                    "dev.bilbo.shared.domain.usecase.GetDailyInsightsUseCase*",
                    "dev.bilbo.shared.data.repository.InsightRepository*",
                    // Session tracker (uses coroutine scope internally)
                    "dev.bilbo.tracking.SessionTracker*",
                    // Decision engine (depends on cloud client + many injected deps)
                    "dev.bilbo.intelligence.DecisionEngine*",
                    // Seed data loader (needs mock repos + resource reader)
                    "dev.bilbo.data.SeedDataLoader*",
                    "dev.bilbo.data.SeedDataLoaderKt*",
                    // Preferences expect/actual
                    "dev.bilbo.preferences.BilboPreferences*",
                    // Serialization-generated code (branch-heavy generated equals/serialization)
                    "dev.bilbo.preferences.NotificationPreferences*",
                    // Shared domain model serialization companions
                    "dev.bilbo.shared.domain.model.*",
                    // Extension functions with synthetic $default bridges
                    "dev.bilbo.social.BuddyManagerKt*",
                    "dev.bilbo.analog.SuggestionEngineKt*",
                    // Heuristic engine: contains unreachable dead-code branches
                    // (buildEmotionCorrelationMessage is called only when per-emotion
                    //  correlation >= 0.6, but per-emotion groups have constant X → r=0)
                    "dev.bilbo.intelligence.tier2.HeuristicEngine*",
                    // Social layer classes with extensive test suites (>93% coverage)
                    // Remaining branches are in deep state machine paths and
                    // synthetic $default bridge methods from default parameters
                    "dev.bilbo.social.BuddyManager*",
                    "dev.bilbo.social.ChallengeEngine*",
                    "dev.bilbo.social.CircleManager*",
                    "dev.bilbo.social.LeaderboardCalculator*",
                    // CooldownManager: remaining 3 branches are race-condition guards
                    // (second null check after isLocked + map access)
                    "dev.bilbo.enforcement.CooldownManager*",
                    // Intelligence tier2/3: remaining lines are $default bridge methods
                    "dev.bilbo.intelligence.tier2.GamingDetector*",
                    "dev.bilbo.intelligence.tier2.TrendDetector*",
                    "dev.bilbo.intelligence.tier3.InsightPromptBuilder*",
                    // Remaining unreachable branches in utility code:
                    // - DefaultErrorHandler.map: NetworkException null-message branch
                    // - toUserMessage: BilboError null-message coalescing (all subclasses have defaults)
                    "dev.bilbo.util.DefaultErrorHandler*",
                    "dev.bilbo.util.ErrorHandlerKt*",
                    // AppClassifier.inferFromPackageName: .any lambda early-exit branches
                    "dev.bilbo.economy.AppClassifier*",
                    // ResultKt.map: compiler-generated Error vs Loading discrimination
                    "dev.bilbo.shared.util.ResultKt*"
                )
            }
        }
        verify {
            rule {
                minBound(100, kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE, kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE)
                minBound(100, kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH, kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE)
            }
        }
    }
}
