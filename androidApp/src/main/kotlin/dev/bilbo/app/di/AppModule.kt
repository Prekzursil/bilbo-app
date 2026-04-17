package dev.bilbo.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.bilbo.app.BuildConfig
import dev.bilbo.data.AndroidDatabaseDriver
import dev.bilbo.data.DatabaseDriverFactory
import dev.bilbo.shared.data.remote.BilboApiService
import dev.bilbo.shared.data.remote.createBilboSupabaseClient
import dev.bilbo.shared.data.repository.InsightRepository
import dev.bilbo.data.BilboDatabase
import dev.bilbo.preferences.AndroidBilboPreferences
import dev.bilbo.preferences.BilboPreferences
import dev.bilbo.shared.domain.usecase.GetDailyInsightsUseCase
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient =
        createBilboSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        )

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

    @Provides
    @Singleton
    fun provideBilboPreferences(@ApplicationContext context: Context): BilboPreferences =
        AndroidBilboPreferences(context)
}
