package dev.spark.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.spark.app.overlay.GatekeeperController
import dev.spark.app.overlay.OverlayManager
import dev.spark.data.IntentRepository
import dev.spark.tracking.BypassManager
import dev.spark.tracking.AppMonitor
import javax.inject.Singleton

/**
 * Provides gatekeeper-related singletons: [OverlayManager], [BypassManager],
 * and [GatekeeperController].
 *
 * [GatekeeperController] is NOT automatically wired to the [AppMonitor] here —
 * [UsageTrackingService] calls [GatekeeperController.attach] after startup so
 * the attachment lifetime matches the service lifetime.
 */
@Module
@InstallIn(SingletonComponent::class)
object GatekeeperModule {

    @Provides
    @Singleton
    fun provideOverlayManager(
        @ApplicationContext context: Context,
    ): OverlayManager = OverlayManager(context)

    @Provides
    @Singleton
    fun provideBypassManager(): BypassManager = BypassManager()

    @Provides
    @Singleton
    fun provideGatekeeperController(
        @ApplicationContext context: Context,
        overlayManager: OverlayManager,
        bypassManager: BypassManager,
        intentRepository: IntentRepository,
    ): GatekeeperController = GatekeeperController(
        context = context,
        overlayManager = overlayManager,
        bypassManager = bypassManager,
        intentRepository = intentRepository,
    )
}
