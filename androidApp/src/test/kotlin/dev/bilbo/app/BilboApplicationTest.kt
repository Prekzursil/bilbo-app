package dev.bilbo.app

import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Robolectric tests for [BilboApplication].
 *
 * Robolectric instantiates the application class declared in AndroidManifest.xml
 * (.BilboApplication) via [ApplicationProvider.getApplicationContext], which
 * calls [BilboApplication.onCreate] on the test JVM.
 *
 * Covers:
 *  - Application is created as [BilboApplication] (Hilt annotation processed)
 *  - BuildConfig.DEBUG flag is accessible and is true in the debug variant
 *  - onCreate completes without throwing (Sentry + PostHog init with empty keys)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = BilboApplication::class)
class BilboApplicationTest {

    @Test
    fun `ApplicationProvider returns a BilboApplication instance`() {
        val app = ApplicationProvider.getApplicationContext<BilboApplication>()
        assertNotNull(app)
        assertIs<BilboApplication>(app)
    }

    @Test
    fun `application context is non-null`() {
        val app = ApplicationProvider.getApplicationContext<BilboApplication>()
        assertNotNull(app.applicationContext)
    }

    @Test
    fun `BuildConfig DEBUG is true in the debug variant`() {
        // Validates that the playstore/debug BuildConfig values are wired in tests
        // and that the logging branch (Timber.plant) would have been exercised by onCreate.
        assert(BuildConfig.DEBUG)
    }

    @Test
    fun `application package name matches dev bilbo app`() {
        val app = ApplicationProvider.getApplicationContext<BilboApplication>()
        // Package name includes the debug suffix in the debug variant
        assert(app.packageName.startsWith("dev.bilbo.app"))
    }
}
