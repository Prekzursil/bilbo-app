package dev.bilbo.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.bilbo.app.tracking.PollingAppMonitor
import dev.bilbo.tracking.AppMonitor
import javax.inject.Singleton

/**
 * Play Store flavor — binds [PollingAppMonitor] as the [AppMonitor] singleton.
 *
 * Uses `UsageStatsManager` to poll for the foreground app every 5 seconds.
 * Requires the user to grant `PACKAGE_USAGE_STATS` via system Settings.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppMonitorModule {

    @Provides
    @Singleton
    fun provideAppMonitor(
        @ApplicationContext context: Context,
    ): AppMonitor = PollingAppMonitor(context)
}
