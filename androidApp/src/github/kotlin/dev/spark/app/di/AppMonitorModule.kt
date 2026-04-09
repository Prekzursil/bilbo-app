package dev.spark.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.spark.app.tracking.AccessibilityAppMonitor
import dev.spark.tracking.AppMonitor
import javax.inject.Singleton

/**
 * GitHub flavor — binds [AccessibilityAppMonitor] as the [AppMonitor] singleton.
 *
 * Driven by [dev.spark.app.service.SparkAccessibilityService] events.
 * No `PACKAGE_USAGE_STATS` permission required; the user enables the service
 * under Settings → Accessibility → Spark.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppMonitorModule {

    @Provides
    @Singleton
    fun provideAppMonitor(
        @ApplicationContext context: Context,
    ): AppMonitor = AccessibilityAppMonitor(context)
}
