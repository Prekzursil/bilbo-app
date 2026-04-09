package dev.bilbo.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.bilbo.app.overlay.GatekeeperController
import dev.bilbo.app.overlay.OverlayManager
import dev.bilbo.data.IntentRepository
import dev.bilbo.tracking.BypassManager
import dev.bilbo.tracking.AppMonitor
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
