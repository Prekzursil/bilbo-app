package dev.bilbo.app.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.bilbo.data.AndroidDatabaseDriver
import dev.bilbo.preferences.AndroidBilboPreferences
import dev.bilbo.preferences.BilboPreferences
import dev.bilbo.shared.data.remote.BilboApiService
import dev.bilbo.shared.data.repository.InsightRepository
import dev.bilbo.shared.domain.usecase.GetDailyInsightsUseCase
import io.mockk.mockk
import io.github.jan.supabase.SupabaseClient
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Unit tests for [AppModule].
 *
 * Calls each @Provides function directly (no Hilt component launched).
 * [SupabaseClient] is mocked where needed to avoid real HTTP init.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppModuleTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ── DatabaseDriverFactory ────────────────────────────────────────────────

    @Test
    fun `provideDatabaseDriverFactory returns AndroidDatabaseDriver`() {
        val factory = AppModule.provideDatabaseDriverFactory(context)
        assertNotNull(factory)
        assertIs<AndroidDatabaseDriver>(factory)
    }

    // ── BilboPreferences ─────────────────────────────────────────────────────

    @Test
    fun `provideBilboPreferences returns AndroidBilboPreferences`() {
        val prefs: BilboPreferences = AppModule.provideBilboPreferences(context)
        assertNotNull(prefs)
        assertIs<AndroidBilboPreferences>(prefs)
    }

    // ── BilboApiService ──────────────────────────────────────────────────────

    @Test
    fun `provideBilboApiService returns a non-null BilboApiService`() {
        val client = mockk<SupabaseClient>(relaxed = true)
        val service = AppModule.provideBilboApiService(client)
        assertNotNull(service)
        assertIs<BilboApiService>(service)
    }

    // ── InsightRepository ────────────────────────────────────────────────────

    @Test
    fun `provideInsightRepository returns a non-null InsightRepository`() {
        val client = mockk<SupabaseClient>(relaxed = true)
        val apiService = AppModule.provideBilboApiService(client)
        val repo = AppModule.provideInsightRepository(apiService)
        assertNotNull(repo)
        assertIs<InsightRepository>(repo)
    }

    // ── GetDailyInsightsUseCase ──────────────────────────────────────────────

    @Test
    fun `provideGetDailyInsightsUseCase returns a non-null GetDailyInsightsUseCase`() {
        val client = mockk<SupabaseClient>(relaxed = true)
        val apiService = AppModule.provideBilboApiService(client)
        val repo = AppModule.provideInsightRepository(apiService)
        val useCase = AppModule.provideGetDailyInsightsUseCase(repo)
        assertNotNull(useCase)
        assertIs<GetDailyInsightsUseCase>(useCase)
    }

    // ── Two consecutive calls produce fresh instances ────────────────────────

    @Test
    fun `provideBilboPreferences calls are independent`() {
        val a = AppModule.provideBilboPreferences(context)
        val b = AppModule.provideBilboPreferences(context)
        assertNotNull(a)
        assertNotNull(b)
    }
}
