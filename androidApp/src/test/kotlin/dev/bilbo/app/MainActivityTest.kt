package dev.bilbo.app

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.bilbo.app.di.AppModule
import dev.bilbo.data.AndroidDatabaseDriver
import dev.bilbo.data.BilboDatabase
import dev.bilbo.data.DatabaseDriverFactory
import dev.bilbo.preferences.AndroidBilboPreferences
import dev.bilbo.preferences.BilboPreferences
import dev.bilbo.shared.data.remote.BilboApiService
import dev.bilbo.shared.data.remote.createBilboSupabaseClient
import dev.bilbo.shared.data.repository.InsightRepository
import dev.bilbo.shared.domain.usecase.GetDailyInsightsUseCase
import io.github.jan.supabase.SupabaseClient
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Singleton

/**
 * Robolectric + Hilt tests for [MainActivity].
 *
 * [MainActivity] is `@AndroidEntryPoint` and injects [BilboPreferences].
 * We uninstall [AppModule] and replace it with [FakeAppModule] that
 * provides a mock [BilboPreferences] (onboarding not complete) plus real
 * stubs for all other [AppModule] bindings.
 *
 * Covers:
 *  - Activity creates without crash when onboardingCompleted = false
 *  - WelcomeScreen (start destination) is displayed when onboarding is not done
 */
@HiltAndroidTest
@UninstallModules(AppModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = dagger.hilt.android.testing.HiltTestApplication::class)
class MainActivityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun `MainActivity creates without crash when onboarding is not completed`() {
        // The activity is already launched by createAndroidComposeRule.
        composeRule.waitForIdle()
    }

    @Test
    fun `WelcomeScreen headline is displayed when onboarding is not complete`() {
        composeRule.waitForIdle()
        // BilboNavHost routes to OnboardingNavHost → WelcomeScreen when onboarding is not done.
        composeRule.onNodeWithText("Bilbo helps you build intentional digital habits.")
            .assertExists("WelcomeScreen headline should be visible")
    }

    // ── Test replacement for AppModule ────────────────────────────────────────

    @Module
    @InstallIn(SingletonComponent::class)
    object FakeAppModule {
        @Provides
        @Singleton
        fun provideSupabaseClient(): SupabaseClient =
            createBilboSupabaseClient(supabaseUrl = "", supabaseKey = "")

        @Provides
        @Singleton
        fun provideDatabaseDriverFactory(
            @ApplicationContext context: Context,
        ): DatabaseDriverFactory = AndroidDatabaseDriver(context)

        @Provides
        @Singleton
        fun provideBilboDatabase(driverFactory: DatabaseDriverFactory): BilboDatabase =
            BilboDatabase(driverFactory.createDriver())

        @Provides
        @Singleton
        fun provideBilboApiService(client: SupabaseClient): BilboApiService =
            BilboApiService(client)

        @Provides
        @Singleton
        fun provideInsightRepository(apiService: BilboApiService): InsightRepository =
            InsightRepository(apiService)

        @Provides
        fun provideGetDailyInsightsUseCase(repository: InsightRepository): GetDailyInsightsUseCase =
            GetDailyInsightsUseCase(repository)

        /**
         * Returns a mock [BilboPreferences] with onboardingCompleted = false
         * so that [MainActivity] routes to WelcomeScreen (no ViewModel needed).
         */
        @Provides
        @Singleton
        fun provideBilboPreferences(
            @ApplicationContext context: Context,
        ): BilboPreferences = mockk<BilboPreferences>(relaxed = true).also {
            every { it.onboardingCompleted } returns false
        }
    }
}
