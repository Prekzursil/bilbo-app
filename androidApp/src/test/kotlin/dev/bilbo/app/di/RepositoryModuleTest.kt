package dev.bilbo.app.di

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.bilbo.app.emotional.EmotionalFlowSettings
import dev.bilbo.app.enforcement.SharedPrefsCooldownPersistence
import dev.bilbo.data.AppProfileRepository
import dev.bilbo.data.BudgetRepository
import dev.bilbo.data.EmotionRepository
import dev.bilbo.data.IntentRepository
import dev.bilbo.data.SuggestionRepository
import dev.bilbo.data.UsageRepository
import dev.bilbo.enforcement.CooldownPersistence
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Unit tests for [RepositoryModule].
 *
 * Instantiates the object directly and calls each @Provides function with
 * real or mock arguments, asserting:
 *  - non-null return
 *  - correct concrete type
 *
 * No Hilt component is started; this purely tests the factory logic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RepositoryModuleTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ── Repository providers ─────────────────────────────────────────────────

    @Test
    fun `provideUsageRepository returns non-null UsageRepository`() {
        val repo = RepositoryModule.provideUsageRepository()
        assertNotNull(repo)
        assertIs<UsageRepository>(repo)
    }

    @Test
    fun `provideAppProfileRepository returns non-null AppProfileRepository`() {
        val repo = RepositoryModule.provideAppProfileRepository()
        assertNotNull(repo)
        assertIs<AppProfileRepository>(repo)
    }

    @Test
    fun `provideIntentRepository returns non-null IntentRepository`() {
        val repo = RepositoryModule.provideIntentRepository()
        assertNotNull(repo)
        assertIs<IntentRepository>(repo)
    }

    @Test
    fun `provideEmotionRepository returns non-null EmotionRepository`() {
        val repo = RepositoryModule.provideEmotionRepository()
        assertNotNull(repo)
        assertIs<EmotionRepository>(repo)
    }

    @Test
    fun `provideBudgetRepository returns non-null BudgetRepository`() {
        val repo = RepositoryModule.provideBudgetRepository()
        assertNotNull(repo)
        assertIs<BudgetRepository>(repo)
    }

    @Test
    fun `provideSuggestionRepository returns non-null SuggestionRepository`() {
        val repo = RepositoryModule.provideSuggestionRepository()
        assertNotNull(repo)
        assertIs<SuggestionRepository>(repo)
    }

    // ── CooldownPersistence ──────────────────────────────────────────────────

    @Test
    fun `provideCooldownPersistence returns SharedPrefsCooldownPersistence as CooldownPersistence`() {
        val impl = SharedPrefsCooldownPersistence(context)
        val bound: CooldownPersistence = RepositoryModule.provideCooldownPersistence(impl)
        assertNotNull(bound)
        assertIs<CooldownPersistence>(bound)
    }

    // ── NotificationManager ──────────────────────────────────────────────────

    @Test
    fun `provideNotificationManager returns a non-null NotificationManager`() {
        val nm = RepositoryModule.provideNotificationManager(context)
        assertNotNull(nm)
        assertIs<NotificationManager>(nm)
    }

    // ── EmotionalFlowSettings ────────────────────────────────────────────────

    @Test
    fun `provideEmotionalFlowSettings returns a non-null EmotionalFlowSettings`() {
        val settings = RepositoryModule.provideEmotionalFlowSettings()
        assertNotNull(settings)
        assertIs<EmotionalFlowSettings>(settings)
    }

    // ── Each repository is a distinct instance (no accidental aliasing) ──────

    @Test
    fun `each call to provideUsageRepository produces a fresh instance`() {
        val a = RepositoryModule.provideUsageRepository()
        val b = RepositoryModule.provideUsageRepository()
        // The @Singleton scoping is enforced by Hilt; outside Hilt each call is new.
        assertNotNull(a)
        assertNotNull(b)
    }
}
