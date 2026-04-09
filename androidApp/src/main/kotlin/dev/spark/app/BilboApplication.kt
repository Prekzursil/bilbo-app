package dev.spark.app

import android.app.Application
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid
import timber.log.Timber

@HiltAndroidApp
class BilboApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initLogging()
        initSentry()
        initPostHog()
    }

    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun initSentry() {
        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.isDebug = BuildConfig.DEBUG
            options.tracesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.2
            options.environment = if (BuildConfig.DEBUG) "development" else "production"
        }
    }

    private fun initPostHog() {
        val config = PostHogAndroidConfig(
            apiKey = BuildConfig.POSTHOG_API_KEY,
            host = "https://app.posthog.com",
        ).apply {
            captureApplicationLifecycleEvents = true
            captureDeepLinks = true
            captureScreenViews = false // manual tracking for privacy
        }
        PostHogAndroid.setup(this, config)
    }
}
