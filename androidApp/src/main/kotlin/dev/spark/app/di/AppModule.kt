package dev.spark.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.spark.app.BuildConfig
import dev.spark.shared.DatabaseDriverFactory
import dev.spark.shared.data.remote.SparkApiService
import dev.spark.shared.data.remote.createSparkSupabaseClient
import dev.spark.shared.data.repository.InsightRepository
import dev.spark.shared.db.SparkDatabase
import dev.spark.shared.domain.usecase.GetDailyInsightsUseCase
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient =
        createSparkSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        )

    @Provides
    @Singleton
    fun provideDatabaseDriverFactory(
        @ApplicationContext context: Context,
    ): DatabaseDriverFactory = DatabaseDriverFactory(context)

    @Provides
    @Singleton
    fun provideSparkDatabase(driverFactory: DatabaseDriverFactory): SparkDatabase =
        SparkDatabase(driverFactory.createDriver())

    @Provides
    @Singleton
    fun provideSparkApiService(client: SupabaseClient): SparkApiService =
        SparkApiService(client)

    @Provides
    @Singleton
    fun provideInsightRepository(apiService: SparkApiService): InsightRepository =
        InsightRepository(apiService)

    @Provides
    fun provideGetDailyInsightsUseCase(repository: InsightRepository): GetDailyInsightsUseCase =
        GetDailyInsightsUseCase(repository)
}
