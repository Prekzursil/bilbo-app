package dev.spark.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.spark.app.BuildConfig
import dev.spark.data.AndroidDatabaseDriver
import dev.spark.data.DatabaseDriverFactory
import dev.spark.shared.data.remote.BilboApiService
import dev.spark.shared.data.remote.createBilboSupabaseClient
import dev.spark.shared.data.repository.InsightRepository
import dev.spark.data.BilboDatabase
import dev.spark.shared.domain.usecase.GetDailyInsightsUseCase
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
}
