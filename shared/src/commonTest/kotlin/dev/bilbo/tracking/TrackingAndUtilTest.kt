// TrackingAndUtilTest.kt
// Bilbo — Comprehensive Unit Tests
//
// Covers: BypassManager, AppInfo, AppMonitor, BilboError, DefaultErrorHandler,
//         OfflineException, NetworkException, withRetry, safeCall, toUserMessage,
//         Result, DefaultEnforcementMode, SharingLevelPref, NotificationPreferences,
//         SeedDataLoader

package dev.bilbo.tracking

import dev.bilbo.data.AppProfileRepository
import dev.bilbo.data.ResourceReader
import dev.bilbo.data.SeedDataLoader
import dev.bilbo.data.SeedPreferenceStore
import dev.bilbo.data.SuggestionRepository
import dev.bilbo.domain.AnalogSuggestion
import dev.bilbo.domain.AppCategory
import dev.bilbo.domain.AppProfile
import dev.bilbo.domain.EnforcementMode
import dev.bilbo.domain.SuggestionCategory
import dev.bilbo.domain.TimeOfDay
import dev.bilbo.preferences.DefaultEnforcementMode
import dev.bilbo.preferences.NotificationPreferences
import dev.bilbo.preferences.SharingLevelPref
import dev.bilbo.shared.util.Result
import dev.bilbo.shared.util.getOrNull
import dev.bilbo.shared.util.getOrThrow
import dev.bilbo.shared.util.map
import dev.bilbo.util.BilboError
import dev.bilbo.util.DefaultErrorHandler
import dev.bilbo.util.ErrorListener
import dev.bilbo.util.NetworkException
import dev.bilbo.util.OfflineException
import dev.bilbo.util.safeCall
import dev.bilbo.util.toUserMessage
import dev.bilbo.util.withRetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlin.test.*

// =============================================================================
// MARK: - BypassManager Tests (production class at dev.bilbo.tracking.BypassManager)
// =============================================================================

class BypassManagerProductionTest {

    private lateinit var manager: dev.bilbo.tracking.BypassManager

    @BeforeTest
    fun setUp() {
        manager = dev.bilbo.tracking.BypassManager()
    }

    // ── Default bypass list ──────────────────────────────────────────────

    @Test
    fun defaultBypassPackagesIsNotEmpty() {
        assertTrue(dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES.isNotEmpty())
    }

    @Test
    fun defaultBypassListContainsAndroidDialer() {
        assertTrue("com.android.dialer" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    @Test
    fun defaultBypassListContainsGoogleDialer() {
        assertTrue("com.google.android.dialer" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    @Test
    fun defaultBypassListContainsSamsungDialer() {
        assertTrue("com.samsung.android.dialer" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    @Test
    fun defaultBypassListContainsMmsApp() {
        assertTrue("com.android.mms" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    @Test
    fun defaultBypassListContainsGoogleMaps() {
        assertTrue("com.google.android.apps.maps" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    @Test
    fun defaultBypassListContainsWaze() {
        assertTrue("com.waze" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    @Test
    fun defaultBypassListContainsAndroidCamera() {
        assertTrue("com.android.camera" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    @Test
    fun defaultBypassListContainsAndroidSettings() {
        assertTrue("com.android.settings" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    @Test
    fun defaultBypassListContainsBilboApp() {
        assertTrue("dev.bilbo.app" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    @Test
    fun defaultBypassListContainsBilboDebug() {
        assertTrue("dev.bilbo.app.debug" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    @Test
    fun defaultBypassListContainsBilboGithub() {
        assertTrue("dev.bilbo.app.github" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    @Test
    fun defaultBypassListContainsSystemUi() {
        assertTrue("com.android.systemui" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    @Test
    fun defaultBypassListContainsEmergency() {
        assertTrue("com.android.emergency" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    @Test
    fun defaultBypassListContainsLauncher3() {
        assertTrue("com.android.launcher3" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    @Test
    fun defaultBypassListContainsDeskclock() {
        assertTrue("com.android.deskclock" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    @Test
    fun defaultBypassListContainsCalculator() {
        assertTrue("com.android.calculator2" in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES)
    }

    // ── shouldBypass ─────────────────────────────────────────────────────

    @Test
    fun shouldBypassReturnsTrueForDefaultPackage() {
        assertTrue(manager.shouldBypass("com.android.dialer"))
    }

    @Test
    fun shouldBypassReturnsTrueForAnotherDefault() {
        assertTrue(manager.shouldBypass("com.android.settings"))
    }

    @Test
    fun shouldBypassReturnsFalseForUnknownPackage() {
        assertFalse(manager.shouldBypass("com.instagram.android"))
    }

    @Test
    fun shouldBypassReturnsFalseForEmptyString() {
        assertFalse(manager.shouldBypass(""))
    }

    @Test
    fun shouldBypassReturnsFalseForRandomPackage() {
        assertFalse(manager.shouldBypass("com.example.random.app"))
    }

    @Test
    fun shouldBypassReturnsTrueAfterAdd() {
        manager.addBypass("com.spotify.music")
        assertTrue(manager.shouldBypass("com.spotify.music"))
    }

    @Test
    fun shouldBypassReturnsFalseAfterRemoveDefault() {
        manager.removeBypass("com.android.dialer")
        assertFalse(manager.shouldBypass("com.android.dialer"))
    }

    // ── addBypass ────────────────────────────────────────────────────────

    @Test
    fun addBypassMakesPackageBypassed() {
        manager.addBypass("com.newapp.test")
        assertTrue(manager.shouldBypass("com.newapp.test"))
    }

    @Test
    fun addBypassAppearsInGetBypassList() {
        manager.addBypass("com.newapp.test")
        assertTrue("com.newapp.test" in manager.getBypassList())
    }

    @Test
    fun addBypassIdempotent() {
        val sizeBefore = manager.getBypassList().size
        manager.addBypass("com.newapp.test")
        val sizeAfterFirst = manager.getBypassList().size
        manager.addBypass("com.newapp.test")
        val sizeAfterSecond = manager.getBypassList().size
        assertEquals(sizeBefore + 1, sizeAfterFirst)
        // Set semantics: adding again does not change size
        assertEquals(sizeAfterFirst, sizeAfterSecond)
    }

    @Test
    fun addBypassDoesNotDuplicateExisting() {
        manager.addBypass("com.android.dialer")
        // Already in default set, count should be 1
        val count = manager.getBypassList().count { it == "com.android.dialer" }
        assertEquals(1, count)
    }

    @Test
    fun addMultiplePackages() {
        manager.addBypass("com.app.one")
        manager.addBypass("com.app.two")
        manager.addBypass("com.app.three")
        assertTrue(manager.shouldBypass("com.app.one"))
        assertTrue(manager.shouldBypass("com.app.two"))
        assertTrue(manager.shouldBypass("com.app.three"))
    }

    // ── removeBypass ─────────────────────────────────────────────────────

    @Test
    fun removeBypassRemovesDefaultPackage() {
        manager.removeBypass("com.android.dialer")
        assertFalse(manager.shouldBypass("com.android.dialer"))
    }

    @Test
    fun removeBypassRemovesUserAddedPackage() {
        manager.addBypass("com.spotify.music")
        manager.removeBypass("com.spotify.music")
        assertFalse(manager.shouldBypass("com.spotify.music"))
    }

    @Test
    fun removeBypassOfNonExistentPackageDoesNothing() {
        val sizeBefore = manager.getBypassList().size
        manager.removeBypass("com.not.present")
        assertEquals(sizeBefore, manager.getBypassList().size)
    }

    @Test
    fun removeBypassDecreasesListSize() {
        val sizeBefore = manager.getBypassList().size
        manager.removeBypass("com.android.settings")
        assertEquals(sizeBefore - 1, manager.getBypassList().size)
    }

    @Test
    fun removeBypassThenAddRestoresPackage() {
        manager.removeBypass("com.android.dialer")
        assertFalse(manager.shouldBypass("com.android.dialer"))
        manager.addBypass("com.android.dialer")
        assertTrue(manager.shouldBypass("com.android.dialer"))
    }

    // ── setUserBypass ────────────────────────────────────────────────────

    @Test
    fun setUserBypassResetsToDefaultsPlusUserPackages() {
        manager.removeBypass("com.android.dialer") // modify state
        manager.addBypass("com.custom.app")

        manager.setUserBypass(setOf("com.user.app1", "com.user.app2"))

        // defaults are restored
        assertTrue(manager.shouldBypass("com.android.dialer"))
        // user packages are present
        assertTrue(manager.shouldBypass("com.user.app1"))
        assertTrue(manager.shouldBypass("com.user.app2"))
        // previously added custom app is gone
        assertFalse(manager.shouldBypass("com.custom.app"))
    }

    @Test
    fun setUserBypassWithEmptySetRestoresDefaults() {
        manager.addBypass("com.custom.app")
        manager.setUserBypass(emptySet())

        assertTrue(manager.shouldBypass("com.android.dialer"))
        assertFalse(manager.shouldBypass("com.custom.app"))
        assertEquals(dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES.size, manager.getBypassList().size)
    }

    @Test
    fun setUserBypassWithOverlappingDefaults() {
        // User set contains a default package -- should not duplicate
        manager.setUserBypass(setOf("com.android.dialer", "com.new.app"))
        val count = manager.getBypassList().count { it == "com.android.dialer" }
        assertEquals(1, count)
        assertTrue(manager.shouldBypass("com.new.app"))
    }

    @Test
    fun setUserBypassReplacesAllPreviousUserPackages() {
        manager.setUserBypass(setOf("com.first.app"))
        assertTrue(manager.shouldBypass("com.first.app"))

        manager.setUserBypass(setOf("com.second.app"))
        assertFalse(manager.shouldBypass("com.first.app"))
        assertTrue(manager.shouldBypass("com.second.app"))
    }

    // ── getBypassList ────────────────────────────────────────────────────

    @Test
    fun getBypassListReturnsAllDefaults() {
        val list = manager.getBypassList()
        for (pkg in dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES) {
            assertTrue(pkg in list, "Expected $pkg in bypass list")
        }
    }

    @Test
    fun getBypassListIncludesUserAdded() {
        manager.addBypass("com.user.added")
        assertTrue("com.user.added" in manager.getBypassList())
    }

    @Test
    fun getBypassListReturnsSnapshot() {
        val snapshot = manager.getBypassList()
        manager.addBypass("com.later.added")
        // Snapshot should not contain the later addition
        assertFalse("com.later.added" in snapshot)
    }

    @Test
    fun getBypassListIsNotMutableExternally() {
        val list = manager.getBypassList()
        // The returned Set should be a snapshot; modifying it should not affect internal state.
        // Since it's a Set<String>, we can't directly mutate it but we verify immutability
        // by checking that adding externally doesn't leak through
        val sizeBefore = list.size
        manager.addBypass("com.extra.app")
        assertEquals(sizeBefore, list.size)
    }

    // ── Fresh instance isolation ─────────────────────────────────────────

    @Test
    fun freshInstanceHasOnlyDefaults() {
        val fresh = dev.bilbo.tracking.BypassManager()
        assertEquals(dev.bilbo.tracking.BypassManager.DEFAULT_BYPASS_PACKAGES, fresh.getBypassList())
    }

    @Test
    fun separateInstancesAreIndependent() {
        val mgr1 = dev.bilbo.tracking.BypassManager()
        val mgr2 = dev.bilbo.tracking.BypassManager()
        mgr1.addBypass("com.only.in.mgr1")
        assertFalse(mgr2.shouldBypass("com.only.in.mgr1"))
    }
}

// =============================================================================
// MARK: - AppInfo Tests
// =============================================================================

class AppInfoTest {

    @Test
    fun appInfoStoresPackageName() {
        val info = AppInfo(
            packageName = "com.example.app",
            appLabel = "Example",
            category = null
        )
        assertEquals("com.example.app", info.packageName)
    }

    @Test
    fun appInfoStoresAppLabel() {
        val info = AppInfo(
            packageName = "com.example.app",
            appLabel = "My App",
            category = null
        )
        assertEquals("My App", info.appLabel)
    }

    @Test
    fun appInfoCategoryCanBeNull() {
        val info = AppInfo(
            packageName = "com.example.app",
            appLabel = "Example",
            category = null
        )
        assertNull(info.category)
    }

    @Test
    fun appInfoCategoryCanBeNutritive() {
        val info = AppInfo(
            packageName = "com.example.app",
            appLabel = "Example",
            category = AppCategory.NUTRITIVE
        )
        assertEquals(AppCategory.NUTRITIVE, info.category)
    }

    @Test
    fun appInfoCategoryCanBeEmptyCalories() {
        val info = AppInfo(
            packageName = "com.example.app",
            appLabel = "Example",
            category = AppCategory.EMPTY_CALORIES
        )
        assertEquals(AppCategory.EMPTY_CALORIES, info.category)
    }

    @Test
    fun appInfoCategoryCanBeNeutral() {
        val info = AppInfo(
            packageName = "com.example.app",
            appLabel = "Example",
            category = AppCategory.NEUTRAL
        )
        assertEquals(AppCategory.NEUTRAL, info.category)
    }

    @Test
    fun appInfoEquality() {
        val a = AppInfo("com.example.app", "Example", null)
        val b = AppInfo("com.example.app", "Example", null)
        assertEquals(a, b)
    }

    @Test
    fun appInfoInequalityByPackageName() {
        val a = AppInfo("com.a", "Same", null)
        val b = AppInfo("com.b", "Same", null)
        assertNotEquals(a, b)
    }

    @Test
    fun appInfoInequalityByLabel() {
        val a = AppInfo("com.example", "Label A", null)
        val b = AppInfo("com.example", "Label B", null)
        assertNotEquals(a, b)
    }

    @Test
    fun appInfoInequalityByCategory() {
        val a = AppInfo("com.example", "App", AppCategory.NUTRITIVE)
        val b = AppInfo("com.example", "App", AppCategory.EMPTY_CALORIES)
        assertNotEquals(a, b)
    }

    @Test
    fun appInfoCopyModifiesField() {
        val original = AppInfo("com.example", "Example", null)
        val copied = original.copy(appLabel = "Updated")
        assertEquals("Updated", copied.appLabel)
        assertEquals("com.example", copied.packageName)
    }

    @Test
    fun appInfoHashCodeConsistentWithEquals() {
        val a = AppInfo("com.example", "App", AppCategory.NEUTRAL)
        val b = AppInfo("com.example", "App", AppCategory.NEUTRAL)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun appInfoToStringContainsPackageName() {
        val info = AppInfo("com.example.app", "Example", null)
        assertTrue(info.toString().contains("com.example.app"))
    }
}

// =============================================================================
// MARK: - AppMonitor Interface Tests (via mock)
// =============================================================================

private class FakeAppMonitor : AppMonitor {
    var currentApp: AppInfo? = null
    var monitoring = false
    var registeredCallback: ((AppInfo) -> Unit)? = null
    var startCallCount = 0
    var stopCallCount = 0

    override fun getCurrentForegroundApp(): AppInfo? = currentApp

    override fun startMonitoring() {
        monitoring = true
        startCallCount++
    }

    override fun stopMonitoring() {
        monitoring = false
        stopCallCount++
    }

    override fun onAppChanged(callback: (AppInfo) -> Unit) {
        registeredCallback = callback
    }

    fun simulateAppChange(appInfo: AppInfo) {
        registeredCallback?.invoke(appInfo)
    }
}

class AppMonitorTest {

    private lateinit var monitor: FakeAppMonitor

    @BeforeTest
    fun setUp() {
        monitor = FakeAppMonitor()
    }

    @Test
    fun getCurrentForegroundAppReturnsNullWhenNoApp() {
        assertNull(monitor.getCurrentForegroundApp())
    }

    @Test
    fun getCurrentForegroundAppReturnsCurrentApp() {
        val app = AppInfo("com.example.app", "Example", null)
        monitor.currentApp = app
        assertEquals(app, monitor.getCurrentForegroundApp())
    }

    @Test
    fun startMonitoringSetsFlag() {
        monitor.startMonitoring()
        assertTrue(monitor.monitoring)
    }

    @Test
    fun stopMonitoringClearsFlag() {
        monitor.startMonitoring()
        monitor.stopMonitoring()
        assertFalse(monitor.monitoring)
    }

    @Test
    fun startMonitoringCalledMultipleTimesIncrements() {
        monitor.startMonitoring()
        monitor.startMonitoring()
        assertEquals(2, monitor.startCallCount)
    }

    @Test
    fun stopMonitoringCalledMultipleTimesIncrements() {
        monitor.stopMonitoring()
        monitor.stopMonitoring()
        assertEquals(2, monitor.stopCallCount)
    }

    @Test
    fun onAppChangedRegistersCallback() {
        var received: AppInfo? = null
        monitor.onAppChanged { received = it }
        val app = AppInfo("com.test.app", "Test", AppCategory.NEUTRAL)
        monitor.simulateAppChange(app)
        assertEquals(app, received)
    }

    @Test
    fun onAppChangedReplacesCallback() {
        var first: AppInfo? = null
        var second: AppInfo? = null
        monitor.onAppChanged { first = it }
        monitor.onAppChanged { second = it }
        val app = AppInfo("com.test.app", "Test", null)
        monitor.simulateAppChange(app)
        assertNull(first) // old callback replaced
        assertEquals(app, second)
    }

    @Test
    fun callbackNotInvokedWhenNoneRegistered() {
        // no callback registered -- simulateAppChange should be a no-op
        monitor.simulateAppChange(AppInfo("com.test", "Test", null))
        // No exception means success
    }
}

// =============================================================================
// MARK: - BilboError Tests
// =============================================================================

class BilboErrorTest {

    @Test
    fun offlineDefaultMessage() {
        val error = BilboError.Offline()
        assertEquals("You're offline. Check your connection.", error.message)
    }

    @Test
    fun offlineCustomMessage() {
        val error = BilboError.Offline("Custom offline msg")
        assertEquals("Custom offline msg", error.message)
    }

    @Test
    fun serverErrorStoresStatusCode() {
        val error = BilboError.ServerError(503)
        assertEquals(503, error.statusCode)
    }

    @Test
    fun serverErrorDefaultMessage() {
        val error = BilboError.ServerError(500)
        assertEquals("Server error (500). Try again shortly.", error.message)
    }

    @Test
    fun serverErrorCustomMessage() {
        val error = BilboError.ServerError(502, "Bad Gateway")
        assertEquals("Bad Gateway", error.message)
        assertEquals(502, error.statusCode)
    }

    @Test
    fun clientErrorStoresStatusCodeAndMessage() {
        val error = BilboError.ClientError(400, "Bad Request")
        assertEquals(400, error.statusCode)
        assertEquals("Bad Request", error.message)
    }

    @Test
    fun unauthorizedDefaultMessage() {
        val error = BilboError.Unauthorized()
        assertEquals("Session expired. Please sign in again.", error.message)
    }

    @Test
    fun unauthorizedCustomMessage() {
        val error = BilboError.Unauthorized("Access denied")
        assertEquals("Access denied", error.message)
    }

    @Test
    fun notFoundDefaultMessage() {
        val error = BilboError.NotFound()
        assertEquals("The requested data was not found.", error.message)
    }

    @Test
    fun notFoundCustomMessage() {
        val error = BilboError.NotFound("User not found")
        assertEquals("User not found", error.message)
    }

    @Test
    fun dataErrorDefaultMessage() {
        val error = BilboError.DataError()
        assertEquals("A data error occurred. Try reopening Bilbo.", error.message)
    }

    @Test
    fun dataErrorWithCause() {
        val cause = RuntimeException("parse failure")
        val error = BilboError.DataError(cause = cause)
        assertEquals(cause, error.cause)
    }

    @Test
    fun dataErrorCustomMessage() {
        val error = BilboError.DataError(message = "DB corrupt")
        assertEquals("DB corrupt", error.message)
    }

    @Test
    fun unknownDefaultMessage() {
        val error = BilboError.Unknown()
        assertEquals("Something went wrong. Please try again.", error.message)
    }

    @Test
    fun unknownWithCause() {
        val cause = IllegalStateException("unexpected")
        val error = BilboError.Unknown(cause = cause)
        assertEquals(cause, error.cause)
    }

    @Test
    fun unknownCustomMessage() {
        val error = BilboError.Unknown(message = "Oops")
        assertEquals("Oops", error.message)
    }

    @Test
    fun bilboErrorIsException() {
        val error: Exception = BilboError.Offline()
        assertTrue(error is Exception)
    }

    @Test
    fun bilboErrorSubtypesAreDistinct() {
        val offline = BilboError.Offline()
        val server = BilboError.ServerError(500)
        val client = BilboError.ClientError(400, "Bad")
        val unauthorized = BilboError.Unauthorized()
        val notFound = BilboError.NotFound()
        val dataError = BilboError.DataError()
        val unknown = BilboError.Unknown()

        assertTrue(offline::class != server::class)
        assertTrue(server::class != client::class)
        assertTrue(client::class != unauthorized::class)
        assertTrue(unauthorized::class != notFound::class)
        assertTrue(notFound::class != dataError::class)
        assertTrue(dataError::class != unknown::class)
    }

    @Test
    fun offlineEquality() {
        assertEquals(BilboError.Offline(), BilboError.Offline())
    }

    @Test
    fun serverErrorEqualitySameCode() {
        assertEquals(BilboError.ServerError(500), BilboError.ServerError(500))
    }

    @Test
    fun serverErrorInequalityDifferentCode() {
        assertNotEquals(BilboError.ServerError(500), BilboError.ServerError(502))
    }
}

// =============================================================================
// MARK: - OfflineException and NetworkException Tests
// =============================================================================

class ExceptionClassesTest {

    @Test
    fun offlineExceptionDefaultMessage() {
        val ex = OfflineException()
        assertEquals("No internet connection", ex.message)
    }

    @Test
    fun offlineExceptionCustomMessage() {
        val ex = OfflineException("WiFi disconnected")
        assertEquals("WiFi disconnected", ex.message)
    }

    @Test
    fun offlineExceptionIsException() {
        assertTrue(OfflineException() is Exception)
    }

    @Test
    fun networkExceptionStoresStatusCode() {
        val ex = NetworkException(404, "Not Found")
        assertEquals(404, ex.statusCode)
    }

    @Test
    fun networkExceptionStoresMessage() {
        val ex = NetworkException(500, "Internal Server Error")
        assertEquals("Internal Server Error", ex.message)
    }

    @Test
    fun networkExceptionIsException() {
        assertTrue(NetworkException(400, "Bad Request") is Exception)
    }
}

// =============================================================================
// MARK: - DefaultErrorHandler Tests
// =============================================================================

class DefaultErrorHandlerTest {

    private lateinit var handler: DefaultErrorHandler

    @BeforeTest
    fun setUp() {
        handler = DefaultErrorHandler()
    }

    // ── map() ────────────────────────────────────────────────────────────

    @Test
    fun mapBilboErrorReturnsItself() {
        val original = BilboError.Offline()
        val mapped = handler.map(original)
        assertSame(original, mapped)
    }

    @Test
    fun mapBilboServerErrorReturnsItself() {
        val original = BilboError.ServerError(500)
        val mapped = handler.map(original)
        assertSame(original, mapped)
    }

    @Test
    fun mapBilboClientErrorReturnsItself() {
        val original = BilboError.ClientError(400, "Bad Request")
        val mapped = handler.map(original)
        assertSame(original, mapped)
    }

    @Test
    fun mapBilboUnauthorizedReturnsItself() {
        val original = BilboError.Unauthorized()
        val mapped = handler.map(original)
        assertSame(original, mapped)
    }

    @Test
    fun mapBilboNotFoundReturnsItself() {
        val original = BilboError.NotFound()
        val mapped = handler.map(original)
        assertSame(original, mapped)
    }

    @Test
    fun mapBilboDataErrorReturnsItself() {
        val original = BilboError.DataError()
        val mapped = handler.map(original)
        assertSame(original, mapped)
    }

    @Test
    fun mapBilboUnknownReturnsItself() {
        val original = BilboError.Unknown()
        val mapped = handler.map(original)
        assertSame(original, mapped)
    }

    @Test
    fun mapOfflineExceptionReturnsOffline() {
        val mapped = handler.map(OfflineException())
        assertTrue(mapped is BilboError.Offline)
    }

    @Test
    fun mapNetworkException401ReturnsUnauthorized() {
        val mapped = handler.map(NetworkException(401, "Unauthorized"))
        assertTrue(mapped is BilboError.Unauthorized)
    }

    @Test
    fun mapNetworkException403ReturnsUnauthorized() {
        val mapped = handler.map(NetworkException(403, "Forbidden"))
        assertTrue(mapped is BilboError.Unauthorized)
    }

    @Test
    fun mapNetworkException404ReturnsNotFound() {
        val mapped = handler.map(NetworkException(404, "Not Found"))
        assertTrue(mapped is BilboError.NotFound)
    }

    @Test
    fun mapNetworkException400ReturnsClientError() {
        val mapped = handler.map(NetworkException(400, "Bad Request"))
        assertTrue(mapped is BilboError.ClientError)
        assertEquals(400, (mapped as BilboError.ClientError).statusCode)
    }

    @Test
    fun mapNetworkException422ReturnsClientError() {
        val mapped = handler.map(NetworkException(422, "Unprocessable Entity"))
        assertTrue(mapped is BilboError.ClientError)
        assertEquals(422, (mapped as BilboError.ClientError).statusCode)
        assertEquals("Unprocessable Entity", mapped.message)
    }

    @Test
    fun mapNetworkException429ReturnsClientError() {
        val mapped = handler.map(NetworkException(429, "Too Many Requests"))
        assertTrue(mapped is BilboError.ClientError)
        assertEquals(429, (mapped as BilboError.ClientError).statusCode)
    }

    @Test
    fun mapNetworkException499ReturnsClientError() {
        val mapped = handler.map(NetworkException(499, "Client Timeout"))
        assertTrue(mapped is BilboError.ClientError)
        assertEquals(499, (mapped as BilboError.ClientError).statusCode)
    }

    @Test
    fun mapNetworkException500ReturnsServerError() {
        val mapped = handler.map(NetworkException(500, "Internal Server Error"))
        assertTrue(mapped is BilboError.ServerError)
        assertEquals(500, (mapped as BilboError.ServerError).statusCode)
    }

    @Test
    fun mapNetworkException502ReturnsServerError() {
        val mapped = handler.map(NetworkException(502, "Bad Gateway"))
        assertTrue(mapped is BilboError.ServerError)
        assertEquals(502, (mapped as BilboError.ServerError).statusCode)
    }

    @Test
    fun mapNetworkException503ReturnsServerError() {
        val mapped = handler.map(NetworkException(503, "Service Unavailable"))
        assertTrue(mapped is BilboError.ServerError)
        assertEquals(503, (mapped as BilboError.ServerError).statusCode)
    }

    @Test
    fun mapNetworkException599ReturnsServerError() {
        val mapped = handler.map(NetworkException(599, "Network Connect Timeout"))
        assertTrue(mapped is BilboError.ServerError)
    }

    @Test
    fun mapNetworkExceptionOtherStatusCodeReturnsUnknown() {
        val mapped = handler.map(NetworkException(600, "Unknown Status"))
        assertTrue(mapped is BilboError.Unknown)
    }

    @Test
    fun mapNetworkException300ReturnsUnknown() {
        val mapped = handler.map(NetworkException(300, "Redirect"))
        assertTrue(mapped is BilboError.Unknown)
    }

    @Test
    fun mapNetworkException200ReturnsUnknown() {
        // 200 is not an error code, should map to Unknown
        val mapped = handler.map(NetworkException(200, "OK"))
        assertTrue(mapped is BilboError.Unknown)
    }

    @Test
    fun mapSerializationExceptionReturnsDataError() {
        val ex = SerializationException("Bad JSON")
        val mapped = handler.map(ex)
        assertTrue(mapped is BilboError.DataError)
        assertEquals(ex, mapped.cause)
    }

    @Test
    fun mapSerializationExceptionMessageContainsUpdatingHint() {
        val mapped = handler.map(SerializationException("parse error"))
        assertTrue(mapped is BilboError.DataError)
        assertTrue(mapped.message!!.contains("updating"))
    }

    @Test
    fun mapUnknownExceptionReturnsUnknown() {
        val ex = RuntimeException("something random")
        val mapped = handler.map(ex)
        assertTrue(mapped is BilboError.Unknown)
        assertEquals(ex, mapped.cause)
    }

    @Test
    fun mapIllegalArgumentExceptionReturnsUnknown() {
        val mapped = handler.map(IllegalArgumentException("bad arg"))
        assertTrue(mapped is BilboError.Unknown)
    }

    @Test
    fun mapIllegalStateExceptionReturnsUnknown() {
        val mapped = handler.map(IllegalStateException("bad state"))
        assertTrue(mapped is BilboError.Unknown)
    }

    @Test
    fun mapNullPointerExceptionReturnsUnknown() {
        val mapped = handler.map(NullPointerException("null"))
        assertTrue(mapped is BilboError.Unknown)
    }

    @Test
    fun mapNetworkExceptionWithNullMessage4xxUsesDefault() {
        // statusCode in 400-499 range but null message => default fallback message
        val ex = NetworkException(418, "I'm a teapot")
        val mapped = handler.map(ex)
        assertTrue(mapped is BilboError.ClientError)
        assertEquals("I'm a teapot", (mapped as BilboError.ClientError).message)
    }

    // ── handle() ─────────────────────────────────────────────────────────

    @Test
    fun handleMapsAndReturnsError() {
        val result = handler.handle(OfflineException())
        assertTrue(result is BilboError.Offline)
    }

    @Test
    fun handleNotifiesListeners() {
        var received: BilboError? = null
        handler.addListener(ErrorListener { received = it })
        handler.handle(OfflineException())
        assertTrue(received is BilboError.Offline)
    }

    @Test
    fun handleNotifiesMultipleListeners() {
        var count = 0
        handler.addListener(ErrorListener { count++ })
        handler.addListener(ErrorListener { count++ })
        handler.handle(RuntimeException("test"))
        assertEquals(2, count)
    }

    @Test
    fun handleWithNoListenersDoesNotThrow() {
        // Should not throw even with no listeners
        val result = handler.handle(RuntimeException("test"))
        assertTrue(result is BilboError.Unknown)
    }

    // ── addListener / removeListener ─────────────────────────────────────

    @Test
    fun addListenerRegistersCallback() {
        var called = false
        handler.addListener(ErrorListener { called = true })
        handler.handle(RuntimeException())
        assertTrue(called)
    }

    @Test
    fun removeListenerStopsNotifications() {
        var callCount = 0
        val listener = ErrorListener { callCount++ }
        handler.addListener(listener)
        handler.handle(RuntimeException())
        assertEquals(1, callCount)

        handler.removeListener(listener)
        handler.handle(RuntimeException())
        assertEquals(1, callCount) // not called again
    }

    @Test
    fun addSameListenerTwiceDoesNotDuplicate() {
        var count = 0
        val listener = ErrorListener { count++ }
        handler.addListener(listener)
        handler.addListener(listener)
        handler.handle(RuntimeException())
        assertEquals(1, count)
    }

    @Test
    fun removeNonExistentListenerDoesNotThrow() {
        val listener = ErrorListener { }
        handler.removeListener(listener) // should not throw
    }

    @Test
    fun listenerReceivesCorrectErrorType() {
        var receivedError: BilboError? = null
        handler.addListener(ErrorListener { receivedError = it })
        handler.handle(NetworkException(404, "Not Found"))
        assertTrue(receivedError is BilboError.NotFound)
    }
}

// =============================================================================
// MARK: - withRetry Tests
// =============================================================================

class WithRetryTest {

    @Test
    fun withRetryReturnsValueOnFirstSuccess() = runTest {
        val result = withRetry { "success" }
        assertEquals("success", result)
    }

    @Test
    fun withRetryRetriesOnOfflineException() = runTest {
        var attempts = 0
        val result = withRetry(
            maxAttempts = 3,
            initialDelay = 1L,
            factor = 1.0
        ) {
            attempts++
            if (attempts < 2) throw OfflineException()
            "recovered"
        }
        assertEquals("recovered", result)
        assertEquals(2, attempts)
    }

    @Test
    fun withRetryRetriesOnServerError() = runTest {
        var attempts = 0
        val result = withRetry(
            maxAttempts = 3,
            initialDelay = 1L,
            factor = 1.0
        ) {
            attempts++
            if (attempts < 2) throw NetworkException(500, "Server Error")
            "recovered"
        }
        assertEquals("recovered", result)
        assertEquals(2, attempts)
    }

    @Test
    fun withRetryThrowsOnClientError() = runTest {
        assertFailsWith<BilboError.ClientError> {
            withRetry(
                maxAttempts = 3,
                initialDelay = 1L,
                factor = 1.0
            ) {
                throw NetworkException(400, "Bad Request")
            }
        }
    }

    @Test
    fun withRetryThrowsOnUnauthorized() = runTest {
        assertFailsWith<BilboError.Unauthorized> {
            withRetry(
                maxAttempts = 3,
                initialDelay = 1L,
                factor = 1.0
            ) {
                throw NetworkException(401, "Unauthorized")
            }
        }
    }

    @Test
    fun withRetryThrowsOnNotFound() = runTest {
        assertFailsWith<BilboError.NotFound> {
            withRetry(
                maxAttempts = 3,
                initialDelay = 1L,
                factor = 1.0
            ) {
                throw NetworkException(404, "Not Found")
            }
        }
    }

    @Test
    fun withRetryThrowsOnDataError() = runTest {
        assertFailsWith<BilboError.DataError> {
            withRetry(
                maxAttempts = 3,
                initialDelay = 1L,
                factor = 1.0
            ) {
                throw SerializationException("bad json")
            }
        }
    }

    @Test
    fun withRetryThrowsOnUnknownError() = runTest {
        assertFailsWith<BilboError.Unknown> {
            withRetry(
                maxAttempts = 3,
                initialDelay = 1L,
                factor = 1.0
            ) {
                throw IllegalStateException("nope")
            }
        }
    }

    @Test
    fun withRetryExhaustsAllAttemptsOnPersistentOffline() = runTest {
        var attempts = 0
        assertFailsWith<BilboError.Offline> {
            withRetry(
                maxAttempts = 3,
                initialDelay = 1L,
                factor = 1.0
            ) {
                attempts++
                throw OfflineException()
            }
        }
        // Should have attempted maxAttempts - 1 times in the repeat block,
        // then thrown on the last retry check
        assertTrue(attempts >= 2)
    }

    @Test
    fun withRetryExhaustsAllAttemptsOnPersistentServerError() = runTest {
        var attempts = 0
        assertFailsWith<BilboError.ServerError> {
            withRetry(
                maxAttempts = 3,
                initialDelay = 1L,
                factor = 1.0
            ) {
                attempts++
                throw NetworkException(503, "Service Unavailable")
            }
        }
        assertTrue(attempts >= 2)
    }

    @Test
    fun withRetryDefaultMaxAttemptsIs3() = runTest {
        var attempts = 0
        assertFailsWith<BilboError.Offline> {
            withRetry(initialDelay = 1L, factor = 1.0) {
                attempts++
                throw OfflineException()
            }
        }
        assertTrue(attempts >= 2)
    }

    @Test
    fun withRetrySingleAttemptThrowsImmediately() = runTest {
        var attempts = 0
        assertFailsWith<OfflineException> {
            withRetry(
                maxAttempts = 1,
                initialDelay = 1L,
                factor = 1.0
            ) {
                attempts++
                throw OfflineException()
            }
        }
        assertEquals(1, attempts)
    }

    @Test
    fun withRetrySucceedsOnSecondAttempt() = runTest {
        var attempts = 0
        val result = withRetry(
            maxAttempts = 3,
            initialDelay = 1L,
            factor = 1.0
        ) {
            attempts++
            if (attempts == 1) throw OfflineException()
            42
        }
        assertEquals(42, result)
        assertEquals(2, attempts)
    }

    @Test
    fun withRetryAcceptsCustomErrorHandler() = runTest {
        val customHandler = DefaultErrorHandler()
        val result = withRetry(
            maxAttempts = 2,
            initialDelay = 1L,
            factor = 1.0,
            errorHandler = customHandler
        ) {
            "ok"
        }
        assertEquals("ok", result)
    }
}

// =============================================================================
// MARK: - safeCall Tests
// =============================================================================

class SafeCallTest {

    @Test
    fun safeCallReturnsSuccessOnSuccess() = runTest {
        val result = safeCall { "hello" }
        assertTrue(result.isSuccess)
        assertEquals("hello", result.getOrNull())
    }

    @Test
    fun safeCallReturnsFailureOnException() = runTest {
        val result = safeCall { throw OfflineException() }
        assertTrue(result.isFailure)
    }

    @Test
    fun safeCallMapsExceptionToBilboError() = runTest {
        val result = safeCall { throw OfflineException() }
        val error = result.exceptionOrNull()
        assertTrue(error is BilboError.Offline)
    }

    @Test
    fun safeCallMapsNetworkExceptionToServerError() = runTest {
        val result = safeCall { throw NetworkException(500, "Server Error") }
        val error = result.exceptionOrNull()
        assertTrue(error is BilboError.ServerError)
    }

    @Test
    fun safeCallMapsRuntimeExceptionToUnknown() = runTest {
        val result = safeCall { throw RuntimeException("oops") }
        val error = result.exceptionOrNull()
        assertTrue(error is BilboError.Unknown)
    }

    @Test
    fun safeCallMapsSerializationExceptionToDataError() = runTest {
        val result = safeCall { throw SerializationException("bad data") }
        val error = result.exceptionOrNull()
        assertTrue(error is BilboError.DataError)
    }

    @Test
    fun safeCallUsesCustomErrorHandler() = runTest {
        val customHandler = DefaultErrorHandler()
        val result = safeCall(customHandler) { throw OfflineException() }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BilboError.Offline)
    }

    @Test
    fun safeCallReturnsCorrectSuccessValue() = runTest {
        val result = safeCall { 42 }
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun safeCallSuccessWithNullValue() = runTest {
        val result = safeCall<String?> { null }
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun safeCallSuccessWithListValue() = runTest {
        val result = safeCall { listOf(1, 2, 3) }
        assertEquals(listOf(1, 2, 3), result.getOrNull())
    }
}

// =============================================================================
// MARK: - toUserMessage Tests
// =============================================================================

class ToUserMessageTest {

    @Test
    fun bilboOfflineToUserMessage() {
        val msg = BilboError.Offline().toUserMessage()
        assertEquals("You're offline. Check your connection.", msg)
    }

    @Test
    fun bilboServerErrorToUserMessage() {
        val msg = BilboError.ServerError(500).toUserMessage()
        assertTrue(msg.contains("Server error"))
    }

    @Test
    fun bilboClientErrorToUserMessage() {
        val msg = BilboError.ClientError(400, "Bad Request").toUserMessage()
        assertEquals("Bad Request", msg)
    }

    @Test
    fun bilboUnauthorizedToUserMessage() {
        val msg = BilboError.Unauthorized().toUserMessage()
        assertTrue(msg.contains("sign in"))
    }

    @Test
    fun bilboNotFoundToUserMessage() {
        val msg = BilboError.NotFound().toUserMessage()
        assertTrue(msg.contains("not found"))
    }

    @Test
    fun bilboDataErrorToUserMessage() {
        val msg = BilboError.DataError().toUserMessage()
        assertTrue(msg.contains("data error"))
    }

    @Test
    fun bilboUnknownToUserMessage() {
        val msg = BilboError.Unknown().toUserMessage()
        assertTrue(msg.contains("Something went wrong"))
    }

    @Test
    fun runtimeExceptionToUserMessage() {
        val msg = RuntimeException("oops").toUserMessage()
        // Maps through DefaultErrorHandler, becomes Unknown
        assertTrue(msg.contains("Something went wrong") || msg.contains("error"))
    }

    @Test
    fun offlineExceptionToUserMessage() {
        val msg = OfflineException().toUserMessage()
        assertTrue(msg.contains("offline"))
    }

    @Test
    fun networkException404ToUserMessage() {
        val msg = NetworkException(404, "Not Found").toUserMessage()
        assertTrue(msg.contains("not found"))
    }

    @Test
    fun networkException500ToUserMessage() {
        val msg = NetworkException(500, "ISE").toUserMessage()
        assertTrue(msg.contains("Server error") || msg.contains("server"))
    }

    @Test
    fun serializationExceptionToUserMessage() {
        val msg = SerializationException("bad").toUserMessage()
        assertTrue(msg.contains("data") || msg.contains("updating"))
    }
}

// =============================================================================
// MARK: - Result (dev.bilbo.shared.util.Result) Tests
// =============================================================================

class SharedResultTest {

    // ── Loading ──────────────────────────────────────────────────────────

    @Test
    fun loadingGetOrNullReturnsNull() {
        val result: Result<String> = Result.Loading
        assertNull(result.getOrNull())
    }

    @Test
    fun loadingGetOrThrowThrows() {
        val result: Result<String> = Result.Loading
        assertFailsWith<IllegalStateException> { result.getOrThrow() }
    }

    @Test
    fun loadingMapReturnsLoading() {
        val result: Result<Int> = Result.Loading
        val mapped = result.map { it * 2 }
        assertTrue(mapped is Result.Loading)
    }

    @Test
    fun loadingIsASingleton() {
        assertSame(Result.Loading, Result.Loading)
    }

    // ── Success ──────────────────────────────────────────────────────────

    @Test
    fun successGetOrNullReturnsData() {
        val result = Result.Success("hello")
        assertEquals("hello", result.getOrNull())
    }

    @Test
    fun successGetOrThrowReturnsData() {
        val result = Result.Success(42)
        assertEquals(42, result.getOrThrow())
    }

    @Test
    fun successMapTransformsData() {
        val result = Result.Success(5)
        val mapped = result.map { it * 3 }
        assertEquals(Result.Success(15), mapped)
    }

    @Test
    fun successWithNullData() {
        val result = Result.Success<String?>(null)
        assertNull(result.getOrNull())
    }

    @Test
    fun successEquality() {
        assertEquals(Result.Success(42), Result.Success(42))
    }

    @Test
    fun successInequality() {
        assertNotEquals(Result.Success(42), Result.Success(43))
    }

    @Test
    fun successDataAccessedViaProperty() {
        val result = Result.Success("data")
        assertEquals("data", result.data)
    }

    @Test
    fun successMapToStringType() {
        val result = Result.Success(42)
        val mapped = result.map { it.toString() }
        assertEquals(Result.Success("42"), mapped)
    }

    @Test
    fun successMapToListType() {
        val result = Result.Success(3)
        val mapped = result.map { List(it) { i -> i } }
        assertEquals(Result.Success(listOf(0, 1, 2)), mapped)
    }

    // ── Error ────────────────────────────────────────────────────────────

    @Test
    fun errorGetOrNullReturnsNull() {
        val result: Result<String> = Result.Error(RuntimeException("boom"))
        assertNull(result.getOrNull())
    }

    @Test
    fun errorGetOrThrowThrowsException() {
        val ex = RuntimeException("test error")
        val result: Result<String> = Result.Error(ex)
        val thrown = assertFailsWith<RuntimeException> { result.getOrThrow() }
        assertSame(ex, thrown)
    }

    @Test
    fun errorMapPreservesError() {
        val ex = RuntimeException("err")
        val result: Result<Int> = Result.Error(ex)
        val mapped = result.map { it * 2 }
        assertTrue(mapped is Result.Error)
        assertEquals(ex, (mapped as Result.Error).exception)
    }

    @Test
    fun errorExceptionAccessedViaProperty() {
        val ex = RuntimeException("fail")
        val result = Result.Error(ex)
        assertSame(ex, result.exception)
    }

    @Test
    fun errorEquality() {
        val ex = RuntimeException("err")
        assertEquals(Result.Error(ex), Result.Error(ex))
    }

    @Test
    fun errorMapDoesNotCallTransform() {
        val result: Result<Int> = Result.Error(RuntimeException())
        var called = false
        result.map {
            called = true
            it
        }
        assertFalse(called)
    }

    @Test
    fun loadingMapDoesNotCallTransform() {
        val result: Result<Int> = Result.Loading
        var called = false
        result.map {
            called = true
            it
        }
        assertFalse(called)
    }

    // ── Type checks ─────────────────────────────────────────────────────

    @Test
    fun successIsNotLoading() {
        val result: Result<Int> = Result.Success(1)
        assertFalse(result is Result.Loading)
    }

    @Test
    fun successIsNotError() {
        val result: Result<Int> = Result.Success(1)
        assertFalse(result is Result.Error)
    }

    @Test
    fun errorIsNotSuccess() {
        val result: Result<Int> = Result.Error(RuntimeException())
        assertFalse(result is Result.Success)
    }

    @Test
    fun loadingIsNotSuccess() {
        val result: Result<Int> = Result.Loading
        assertFalse(result is Result.Success)
    }
}

// =============================================================================
// MARK: - DefaultEnforcementMode Tests
// =============================================================================

class DefaultEnforcementModeTest {

    @Test
    fun softLockExists() {
        assertEquals("SOFT_LOCK", DefaultEnforcementMode.SOFT_LOCK.name)
    }

    @Test
    fun hardLockExists() {
        assertEquals("HARD_LOCK", DefaultEnforcementMode.HARD_LOCK.name)
    }

    @Test
    fun trackOnlyExists() {
        assertEquals("TRACK_ONLY", DefaultEnforcementMode.TRACK_ONLY.name)
    }

    @Test
    fun enumHasThreeValues() {
        assertEquals(3, DefaultEnforcementMode.entries.size)
    }

    @Test
    fun valuesContainAllEntries() {
        val values = DefaultEnforcementMode.entries
        assertTrue(DefaultEnforcementMode.SOFT_LOCK in values)
        assertTrue(DefaultEnforcementMode.HARD_LOCK in values)
        assertTrue(DefaultEnforcementMode.TRACK_ONLY in values)
    }

    @Test
    fun ordinalSoftLock() {
        assertEquals(0, DefaultEnforcementMode.SOFT_LOCK.ordinal)
    }

    @Test
    fun ordinalHardLock() {
        assertEquals(1, DefaultEnforcementMode.HARD_LOCK.ordinal)
    }

    @Test
    fun ordinalTrackOnly() {
        assertEquals(2, DefaultEnforcementMode.TRACK_ONLY.ordinal)
    }

    @Test
    fun valueOfSoftLock() {
        assertEquals(DefaultEnforcementMode.SOFT_LOCK, DefaultEnforcementMode.valueOf("SOFT_LOCK"))
    }

    @Test
    fun valueOfHardLock() {
        assertEquals(DefaultEnforcementMode.HARD_LOCK, DefaultEnforcementMode.valueOf("HARD_LOCK"))
    }

    @Test
    fun valueOfTrackOnly() {
        assertEquals(DefaultEnforcementMode.TRACK_ONLY, DefaultEnforcementMode.valueOf("TRACK_ONLY"))
    }

    @Test
    fun valueOfInvalidThrows() {
        assertFailsWith<IllegalArgumentException> {
            DefaultEnforcementMode.valueOf("INVALID")
        }
    }
}

// =============================================================================
// MARK: - SharingLevelPref Tests
// =============================================================================

class SharingLevelPrefTest {

    @Test
    fun privateExists() {
        assertEquals("PRIVATE", SharingLevelPref.PRIVATE.name)
    }

    @Test
    fun friendsExists() {
        assertEquals("FRIENDS", SharingLevelPref.FRIENDS.name)
    }

    @Test
    fun circleExists() {
        assertEquals("CIRCLE", SharingLevelPref.CIRCLE.name)
    }

    @Test
    fun publicExists() {
        assertEquals("PUBLIC", SharingLevelPref.PUBLIC.name)
    }

    @Test
    fun enumHasFourValues() {
        assertEquals(4, SharingLevelPref.entries.size)
    }

    @Test
    fun valuesContainAllEntries() {
        val values = SharingLevelPref.entries
        assertTrue(SharingLevelPref.PRIVATE in values)
        assertTrue(SharingLevelPref.FRIENDS in values)
        assertTrue(SharingLevelPref.CIRCLE in values)
        assertTrue(SharingLevelPref.PUBLIC in values)
    }

    @Test
    fun ordinalPrivate() {
        assertEquals(0, SharingLevelPref.PRIVATE.ordinal)
    }

    @Test
    fun ordinalFriends() {
        assertEquals(1, SharingLevelPref.FRIENDS.ordinal)
    }

    @Test
    fun ordinalCircle() {
        assertEquals(2, SharingLevelPref.CIRCLE.ordinal)
    }

    @Test
    fun ordinalPublic() {
        assertEquals(3, SharingLevelPref.PUBLIC.ordinal)
    }

    @Test
    fun valueOfPrivate() {
        assertEquals(SharingLevelPref.PRIVATE, SharingLevelPref.valueOf("PRIVATE"))
    }

    @Test
    fun valueOfPublic() {
        assertEquals(SharingLevelPref.PUBLIC, SharingLevelPref.valueOf("PUBLIC"))
    }

    @Test
    fun valueOfInvalidThrows() {
        assertFailsWith<IllegalArgumentException> {
            SharingLevelPref.valueOf("INVALID")
        }
    }
}

// =============================================================================
// MARK: - NotificationPreferences Tests
// =============================================================================

class NotificationPreferencesTest {

    @Test
    fun defaultNudgeEnabled() {
        val prefs = NotificationPreferences()
        assertTrue(prefs.nudgeEnabled)
    }

    @Test
    fun defaultWeeklyInsightEnabled() {
        val prefs = NotificationPreferences()
        assertTrue(prefs.weeklyInsightEnabled)
    }

    @Test
    fun defaultChallengeUpdateEnabled() {
        val prefs = NotificationPreferences()
        assertTrue(prefs.challengeUpdateEnabled)
    }

    @Test
    fun defaultQuietHoursEnabled() {
        val prefs = NotificationPreferences()
        assertTrue(prefs.quietHoursEnabled)
    }

    @Test
    fun defaultQuietStartHour() {
        val prefs = NotificationPreferences()
        assertEquals(22, prefs.quietStartHour)
    }

    @Test
    fun defaultQuietStartMinute() {
        val prefs = NotificationPreferences()
        assertEquals(0, prefs.quietStartMinute)
    }

    @Test
    fun defaultQuietEndHour() {
        val prefs = NotificationPreferences()
        assertEquals(8, prefs.quietEndHour)
    }

    @Test
    fun defaultQuietEndMinute() {
        val prefs = NotificationPreferences()
        assertEquals(0, prefs.quietEndMinute)
    }

    @Test
    fun customNudgeDisabled() {
        val prefs = NotificationPreferences(nudgeEnabled = false)
        assertFalse(prefs.nudgeEnabled)
    }

    @Test
    fun customWeeklyInsightDisabled() {
        val prefs = NotificationPreferences(weeklyInsightEnabled = false)
        assertFalse(prefs.weeklyInsightEnabled)
    }

    @Test
    fun customChallengeUpdateDisabled() {
        val prefs = NotificationPreferences(challengeUpdateEnabled = false)
        assertFalse(prefs.challengeUpdateEnabled)
    }

    @Test
    fun customQuietHoursDisabled() {
        val prefs = NotificationPreferences(quietHoursEnabled = false)
        assertFalse(prefs.quietHoursEnabled)
    }

    @Test
    fun customQuietStartHour() {
        val prefs = NotificationPreferences(quietStartHour = 23)
        assertEquals(23, prefs.quietStartHour)
    }

    @Test
    fun customQuietEndHour() {
        val prefs = NotificationPreferences(quietEndHour = 7)
        assertEquals(7, prefs.quietEndHour)
    }

    @Test
    fun customQuietStartMinute() {
        val prefs = NotificationPreferences(quietStartMinute = 30)
        assertEquals(30, prefs.quietStartMinute)
    }

    @Test
    fun customQuietEndMinute() {
        val prefs = NotificationPreferences(quietEndMinute = 45)
        assertEquals(45, prefs.quietEndMinute)
    }

    @Test
    fun equality() {
        val a = NotificationPreferences()
        val b = NotificationPreferences()
        assertEquals(a, b)
    }

    @Test
    fun inequalityNudge() {
        val a = NotificationPreferences(nudgeEnabled = true)
        val b = NotificationPreferences(nudgeEnabled = false)
        assertNotEquals(a, b)
    }

    @Test
    fun copyModifiesField() {
        val original = NotificationPreferences()
        val modified = original.copy(quietStartHour = 21)
        assertEquals(21, modified.quietStartHour)
        assertEquals(original.nudgeEnabled, modified.nudgeEnabled)
    }

    @Test
    fun hashCodeConsistentWithEquals() {
        val a = NotificationPreferences(quietStartHour = 20)
        val b = NotificationPreferences(quietStartHour = 20)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun allFieldsCustomized() {
        val prefs = NotificationPreferences(
            nudgeEnabled = false,
            weeklyInsightEnabled = false,
            challengeUpdateEnabled = false,
            quietHoursEnabled = false,
            quietStartHour = 0,
            quietStartMinute = 15,
            quietEndHour = 6,
            quietEndMinute = 30
        )
        assertFalse(prefs.nudgeEnabled)
        assertFalse(prefs.weeklyInsightEnabled)
        assertFalse(prefs.challengeUpdateEnabled)
        assertFalse(prefs.quietHoursEnabled)
        assertEquals(0, prefs.quietStartHour)
        assertEquals(15, prefs.quietStartMinute)
        assertEquals(6, prefs.quietEndHour)
        assertEquals(30, prefs.quietEndMinute)
    }
}

// =============================================================================
// MARK: - SeedDataLoader Tests
// =============================================================================

// ── Fake / Mock implementations ─────────────────────────────────────────

private class FakeResourceReader(
    private val files: Map<String, String> = emptyMap()
) : ResourceReader {
    var readCallCount = 0
    val readFileNames = mutableListOf<String>()

    override suspend fun readText(fileName: String): String {
        readCallCount++
        readFileNames.add(fileName)
        return files[fileName]
            ?: throw IllegalArgumentException("Resource not found: $fileName")
    }
}

private class FakeSeedPreferenceStore(
    private var seeded: Boolean = false
) : SeedPreferenceStore {
    var markSeededCallCount = 0
    var clearSeededCallCount = 0

    override fun isSeeded(): Boolean = seeded

    override fun markSeeded() {
        markSeededCallCount++
        seeded = true
    }

    override fun clearSeeded() {
        clearSeededCallCount++
        seeded = false
    }
}

private class FakeAppProfileRepository : AppProfileRepository {
    val stored = mutableMapOf<String, AppProfile>()
    var insertCallCount = 0

    override fun observeAll(): Flow<List<AppProfile>> = flowOf(stored.values.toList())
    override suspend fun getAll(): List<AppProfile> = stored.values.toList()
    override suspend fun getByPackageName(packageName: String): AppProfile? = stored[packageName]
    override fun observeByPackageName(packageName: String): Flow<AppProfile?> = flowOf(stored[packageName])
    override suspend fun getByCategory(category: AppCategory): List<AppProfile> =
        stored.values.filter { it.category == category }
    override suspend fun getByEnforcementMode(enforcementMode: EnforcementMode): List<AppProfile> =
        stored.values.filter { it.enforcementMode == enforcementMode }
    override suspend fun getBypassed(): List<AppProfile> = stored.values.filter { it.isBypassed }
    override suspend fun getCustomClassified(): List<AppProfile> = stored.values.filter { it.isCustomClassification }
    override suspend fun insert(profile: AppProfile) {
        insertCallCount++
        stored[profile.packageName] = profile
    }
    override suspend fun update(profile: AppProfile) { stored[profile.packageName] = profile }
    override suspend fun upsert(profile: AppProfile) { stored[profile.packageName] = profile }
    override suspend fun updateCategory(packageName: String, category: AppCategory) {
        stored[packageName]?.let { stored[packageName] = it.copy(category = category) }
    }
    override suspend fun updateBypass(packageName: String, isBypassed: Boolean) {
        stored[packageName]?.let { stored[packageName] = it.copy(isBypassed = isBypassed) }
    }
    override suspend fun deleteByPackageName(packageName: String) { stored.remove(packageName) }
}

private class FakeSuggestionRepository : SuggestionRepository {
    val stored = mutableListOf<AnalogSuggestion>()
    var insertCallCount = 0
    private var nextId = 1L

    override fun observeAll(): Flow<List<AnalogSuggestion>> = flowOf(stored.toList())
    override suspend fun getAll(): List<AnalogSuggestion> = stored.toList()
    override suspend fun getById(id: Long): AnalogSuggestion? = stored.find { it.id == id }
    override suspend fun getByCategory(category: SuggestionCategory): List<AnalogSuggestion> =
        stored.filter { it.category == category }
    override suspend fun getByTimeOfDay(timeOfDay: TimeOfDay): List<AnalogSuggestion> =
        stored.filter { it.timeOfDay == timeOfDay || it.timeOfDay == null }
    override suspend fun getByCategoryAndTimeOfDay(
        category: SuggestionCategory,
        timeOfDay: TimeOfDay
    ): List<AnalogSuggestion> = stored.filter { it.category == category && (it.timeOfDay == timeOfDay || it.timeOfDay == null) }
    override suspend fun getCustom(): List<AnalogSuggestion> = stored.filter { it.isCustom }
    override suspend fun getTopAccepted(limit: Long): List<AnalogSuggestion> =
        stored.sortedByDescending { it.timesAccepted }.take(limit.toInt())
    override suspend fun insert(suggestion: AnalogSuggestion): Long {
        insertCallCount++
        val id = nextId++
        stored.add(suggestion.copy(id = id))
        return id
    }
    override suspend fun update(suggestion: AnalogSuggestion) {
        val idx = stored.indexOfFirst { it.id == suggestion.id }
        if (idx >= 0) stored[idx] = suggestion
    }
    override suspend fun recordShown(id: Long) {
        val idx = stored.indexOfFirst { it.id == id }
        if (idx >= 0) stored[idx] = stored[idx].copy(timesShown = stored[idx].timesShown + 1)
    }
    override suspend fun recordAccepted(id: Long) {
        val idx = stored.indexOfFirst { it.id == id }
        if (idx >= 0) stored[idx] = stored[idx].copy(timesAccepted = stored[idx].timesAccepted + 1)
    }
    override suspend fun deleteById(id: Long) { stored.removeAll { it.id == id } }
}

// ── Test JSON fixtures ──────────────────────────────────────────────────

private val CLASSIFICATIONS_JSON = """
[
    {
        "packageName": "com.instagram.android",
        "appLabel": "Instagram",
        "category": "EMPTY_CALORIES",
        "defaultEnforcementMode": "HARD_LOCK"
    },
    {
        "packageName": "com.duolingo",
        "appLabel": "Duolingo",
        "category": "NUTRITIVE",
        "defaultEnforcementMode": "NUDGE"
    }
]
""".trimIndent()

private val SUGGESTIONS_JSON = """
[
    {
        "id": 0,
        "text": "Go for a 20-minute walk",
        "category": "EXERCISE",
        "tags": ["outdoor", "easy"],
        "timeOfDay": "MORNING"
    },
    {
        "id": 0,
        "text": "Read a chapter of a book",
        "category": "READING",
        "tags": ["indoor", "relaxing"],
        "timeOfDay": "EVENING"
    },
    {
        "id": 0,
        "text": "Play guitar for 15 minutes",
        "category": "MUSIC",
        "tags": ["creative"],
        "timeOfDay": null
    }
]
""".trimIndent()

private val EMPTY_JSON = "[]"

class SeedDataLoaderTest {

    private lateinit var appProfileRepo: FakeAppProfileRepository
    private lateinit var suggestionRepo: FakeSuggestionRepository
    private lateinit var resourceReader: FakeResourceReader
    private lateinit var prefStore: FakeSeedPreferenceStore
    private lateinit var loader: SeedDataLoader

    private fun createLoader(
        classJson: String = CLASSIFICATIONS_JSON,
        suggestJson: String = SUGGESTIONS_JSON,
        alreadySeeded: Boolean = false
    ) {
        appProfileRepo = FakeAppProfileRepository()
        suggestionRepo = FakeSuggestionRepository()
        resourceReader = FakeResourceReader(
            mapOf(
                "default_app_classifications.json" to classJson,
                "default_analog_suggestions.json" to suggestJson
            )
        )
        prefStore = FakeSeedPreferenceStore(seeded = alreadySeeded)
        loader = SeedDataLoader(appProfileRepo, suggestionRepo, resourceReader, prefStore)
    }

    // ── load() ───────────────────────────────────────────────────────────

    @Test
    fun loadReturnsTrueOnFirstRun() = runTest {
        createLoader()
        val result = loader.load()
        assertTrue(result)
    }

    @Test
    fun loadReturnsFalseIfAlreadySeeded() = runTest {
        createLoader(alreadySeeded = true)
        val result = loader.load()
        assertFalse(result)
    }

    @Test
    fun loadMarksAsSeeded() = runTest {
        createLoader()
        loader.load()
        assertTrue(prefStore.isSeeded())
        assertEquals(1, prefStore.markSeededCallCount)
    }

    @Test
    fun loadDoesNotMarkSeededIfAlreadySeeded() = runTest {
        createLoader(alreadySeeded = true)
        loader.load()
        assertEquals(0, prefStore.markSeededCallCount)
    }

    @Test
    fun loadInsertsAppProfiles() = runTest {
        createLoader()
        loader.load()
        assertEquals(2, appProfileRepo.stored.size)
        assertNotNull(appProfileRepo.stored["com.instagram.android"])
        assertNotNull(appProfileRepo.stored["com.duolingo"])
    }

    @Test
    fun loadInsertsCorrectAppCategory() = runTest {
        createLoader()
        loader.load()
        val instagram = appProfileRepo.stored["com.instagram.android"]!!
        assertEquals(AppCategory.EMPTY_CALORIES, instagram.category)
    }

    @Test
    fun loadInsertsCorrectEnforcementMode() = runTest {
        createLoader()
        loader.load()
        val instagram = appProfileRepo.stored["com.instagram.android"]!!
        assertEquals(EnforcementMode.HARD_LOCK, instagram.enforcementMode)
    }

    @Test
    fun loadInsertsCorrectNutritiveCategory() = runTest {
        createLoader()
        loader.load()
        val duolingo = appProfileRepo.stored["com.duolingo"]!!
        assertEquals(AppCategory.NUTRITIVE, duolingo.category)
    }

    @Test
    fun loadInsertsNudgeEnforcementMode() = runTest {
        createLoader()
        loader.load()
        val duolingo = appProfileRepo.stored["com.duolingo"]!!
        assertEquals(EnforcementMode.NUDGE, duolingo.enforcementMode)
    }

    @Test
    fun loadInsertsAppProfileWithCorrectLabel() = runTest {
        createLoader()
        loader.load()
        assertEquals("Instagram", appProfileRepo.stored["com.instagram.android"]!!.appLabel)
        assertEquals("Duolingo", appProfileRepo.stored["com.duolingo"]!!.appLabel)
    }

    @Test
    fun loadInsertsAppProfileNotBypassed() = runTest {
        createLoader()
        loader.load()
        assertFalse(appProfileRepo.stored["com.instagram.android"]!!.isBypassed)
    }

    @Test
    fun loadInsertsAppProfileNotCustomClassified() = runTest {
        createLoader()
        loader.load()
        assertFalse(appProfileRepo.stored["com.instagram.android"]!!.isCustomClassification)
    }

    @Test
    fun loadInsertsSuggestions() = runTest {
        createLoader()
        loader.load()
        assertEquals(3, suggestionRepo.stored.size)
    }

    @Test
    fun loadInsertsSuggestionWithCorrectCategory() = runTest {
        createLoader()
        loader.load()
        val exercise = suggestionRepo.stored.find { it.text == "Go for a 20-minute walk" }
        assertNotNull(exercise)
        assertEquals(SuggestionCategory.EXERCISE, exercise.category)
    }

    @Test
    fun loadInsertsSuggestionWithCorrectTimeOfDay() = runTest {
        createLoader()
        loader.load()
        val walk = suggestionRepo.stored.find { it.text == "Go for a 20-minute walk" }
        assertNotNull(walk)
        assertEquals(TimeOfDay.MORNING, walk.timeOfDay)
    }

    @Test
    fun loadInsertsSuggestionWithNullTimeOfDay() = runTest {
        createLoader()
        loader.load()
        val guitar = suggestionRepo.stored.find { it.text == "Play guitar for 15 minutes" }
        assertNotNull(guitar)
        assertNull(guitar.timeOfDay)
    }

    @Test
    fun loadInsertsSuggestionWithTags() = runTest {
        createLoader()
        loader.load()
        val walk = suggestionRepo.stored.find { it.text == "Go for a 20-minute walk" }
        assertNotNull(walk)
        assertEquals(listOf("outdoor", "easy"), walk.tags)
    }

    @Test
    fun loadInsertsSuggestionNotCustom() = runTest {
        createLoader()
        loader.load()
        suggestionRepo.stored.forEach { assertFalse(it.isCustom) }
    }

    @Test
    fun loadReadsCorrectResourceFiles() = runTest {
        createLoader()
        loader.load()
        assertEquals(2, resourceReader.readCallCount)
        assertTrue("default_app_classifications.json" in resourceReader.readFileNames)
        assertTrue("default_analog_suggestions.json" in resourceReader.readFileNames)
    }

    @Test
    fun loadDoesNotReadResourcesIfAlreadySeeded() = runTest {
        createLoader(alreadySeeded = true)
        loader.load()
        assertEquals(0, resourceReader.readCallCount)
    }

    @Test
    fun loadSkipsDuplicateAppProfiles() = runTest {
        createLoader()
        // Pre-populate a profile
        appProfileRepo.stored["com.instagram.android"] = AppProfile(
            packageName = "com.instagram.android",
            appLabel = "Instagram Custom",
            category = AppCategory.NEUTRAL,
            enforcementMode = EnforcementMode.NUDGE
        )
        loader.load()
        // The existing profile should NOT be overwritten
        assertEquals("Instagram Custom", appProfileRepo.stored["com.instagram.android"]!!.appLabel)
        assertEquals(AppCategory.NEUTRAL, appProfileRepo.stored["com.instagram.android"]!!.category)
    }

    @Test
    fun loadSkipsSuggestionsIfNonCustomExist() = runTest {
        createLoader()
        // Pre-populate a non-custom suggestion
        suggestionRepo.stored.add(
            AnalogSuggestion(
                id = 99,
                text = "Existing suggestion",
                category = SuggestionCategory.EXERCISE,
                tags = emptyList(),
                isCustom = false
            )
        )
        loader.load()
        // Should not have added new suggestions
        assertEquals(1, suggestionRepo.stored.size)
    }

    @Test
    fun loadInsertsSuggestionsIfOnlyCustomExist() = runTest {
        createLoader()
        // Pre-populate only a custom suggestion
        suggestionRepo.stored.add(
            AnalogSuggestion(
                id = 99,
                text = "My custom suggestion",
                category = SuggestionCategory.CREATIVE,
                tags = emptyList(),
                isCustom = true
            )
        )
        loader.load()
        // Custom suggestions don't count; new ones should be inserted
        assertEquals(4, suggestionRepo.stored.size) // 1 custom + 3 seeded
    }

    @Test
    fun loadWithEmptyClassificationsJson() = runTest {
        createLoader(classJson = EMPTY_JSON)
        loader.load()
        assertEquals(0, appProfileRepo.stored.size)
        // Suggestions should still be loaded
        assertEquals(3, suggestionRepo.stored.size)
    }

    @Test
    fun loadWithEmptySuggestionsJson() = runTest {
        createLoader(suggestJson = EMPTY_JSON)
        loader.load()
        // App profiles should still be loaded
        assertEquals(2, appProfileRepo.stored.size)
        assertEquals(0, suggestionRepo.stored.size)
    }

    @Test
    fun loadIsIdempotent() = runTest {
        createLoader()
        val first = loader.load()
        val second = loader.load()
        assertTrue(first)
        assertFalse(second)
    }

    // ── forceReload() ────────────────────────────────────────────────────

    @Test
    fun forceReloadClearsSeededFlagAndReseeds() = runTest {
        createLoader()
        loader.load()
        assertTrue(prefStore.isSeeded())

        loader.forceReload()
        assertTrue(prefStore.isSeeded()) // re-seeded
        assertEquals(1, prefStore.clearSeededCallCount)
    }

    @Test
    fun forceReloadCallsClearSeeded() = runTest {
        createLoader()
        loader.forceReload()
        assertEquals(1, prefStore.clearSeededCallCount)
    }

    @Test
    fun forceReloadReloadsData() = runTest {
        createLoader()
        loader.load()
        val initialSuggestionCount = suggestionRepo.stored.size

        // Clear stored suggestions so forceReload re-inserts them
        suggestionRepo.stored.clear()
        loader.forceReload()
        // forceReload clears seeded flag and calls load again, re-inserting suggestions
        assertEquals(initialSuggestionCount, suggestionRepo.stored.size)
    }

    @Test
    fun forceReloadOnNeverSeededInstance() = runTest {
        createLoader()
        loader.forceReload()
        assertTrue(prefStore.isSeeded())
        assertEquals(1, prefStore.clearSeededCallCount)
        assertEquals(2, appProfileRepo.stored.size)
    }

    // ── Category string mapping edge cases ───────────────────────────────

    @Test
    fun neutralCategoryMapsCorrectly() = runTest {
        val json = """
        [{"packageName":"com.test","appLabel":"Test","category":"NEUTRAL","defaultEnforcementMode":"NUDGE"}]
        """.trimIndent()
        createLoader(classJson = json)
        loader.load()
        assertEquals(AppCategory.NEUTRAL, appProfileRepo.stored["com.test"]!!.category)
    }

    @Test
    fun unknownCategoryDefaultsToNeutral() = runTest {
        val json = """
        [{"packageName":"com.test","appLabel":"Test","category":"UNKNOWN_CATEGORY","defaultEnforcementMode":"NUDGE"}]
        """.trimIndent()
        createLoader(classJson = json)
        loader.load()
        assertEquals(AppCategory.NEUTRAL, appProfileRepo.stored["com.test"]!!.category)
    }

    @Test
    fun unknownEnforcementModeDefaultsToNudge() = runTest {
        val json = """
        [{"packageName":"com.test","appLabel":"Test","category":"NEUTRAL","defaultEnforcementMode":"UNKNOWN_MODE"}]
        """.trimIndent()
        createLoader(classJson = json)
        loader.load()
        assertEquals(EnforcementMode.NUDGE, appProfileRepo.stored["com.test"]!!.enforcementMode)
    }

    @Test
    fun hardLockEnforcementModeMapCorrectly() = runTest {
        val json = """
        [{"packageName":"com.test","appLabel":"Test","category":"NEUTRAL","defaultEnforcementMode":"HARD_LOCK"}]
        """.trimIndent()
        createLoader(classJson = json)
        loader.load()
        assertEquals(EnforcementMode.HARD_LOCK, appProfileRepo.stored["com.test"]!!.enforcementMode)
    }

    // ── Suggestion category mapping edge cases ──────────────────────────

    @Test
    fun allSuggestionCategoriesMap() = runTest {
        val categories = listOf(
            "EXERCISE", "CREATIVE", "SOCIAL", "MINDFULNESS",
            "LEARNING", "NATURE", "COOKING", "MUSIC",
            "GAMING_PHYSICAL", "READING"
        )
        val expectedEnums = listOf(
            SuggestionCategory.EXERCISE, SuggestionCategory.CREATIVE,
            SuggestionCategory.SOCIAL, SuggestionCategory.MINDFULNESS,
            SuggestionCategory.LEARNING, SuggestionCategory.NATURE,
            SuggestionCategory.COOKING, SuggestionCategory.MUSIC,
            SuggestionCategory.GAMING_PHYSICAL, SuggestionCategory.READING
        )
        for ((i, cat) in categories.withIndex()) {
            val json = """
            [{"id":0,"text":"Test $cat","category":"$cat","tags":[],"timeOfDay":null}]
            """.trimIndent()
            createLoader(classJson = EMPTY_JSON, suggestJson = json)
            loader.load()
            assertEquals(expectedEnums[i], suggestionRepo.stored.first().category,
                "Expected ${expectedEnums[i]} for category string $cat")
        }
    }

    @Test
    fun unknownSuggestionCategoryDefaultsToReading() = runTest {
        val json = """
        [{"id":0,"text":"Unknown cat","category":"NONEXISTENT","tags":[],"timeOfDay":null}]
        """.trimIndent()
        createLoader(classJson = EMPTY_JSON, suggestJson = json)
        loader.load()
        assertEquals(SuggestionCategory.READING, suggestionRepo.stored.first().category)
    }

    // ── TimeOfDay mapping ────────────────────────────────────────────────

    @Test
    fun morningTimeOfDayMaps() = runTest {
        val json = """
        [{"id":0,"text":"Morning","category":"EXERCISE","tags":[],"timeOfDay":"MORNING"}]
        """.trimIndent()
        createLoader(classJson = EMPTY_JSON, suggestJson = json)
        loader.load()
        assertEquals(TimeOfDay.MORNING, suggestionRepo.stored.first().timeOfDay)
    }

    @Test
    fun afternoonTimeOfDayMaps() = runTest {
        val json = """
        [{"id":0,"text":"Afternoon","category":"EXERCISE","tags":[],"timeOfDay":"AFTERNOON"}]
        """.trimIndent()
        createLoader(classJson = EMPTY_JSON, suggestJson = json)
        loader.load()
        assertEquals(TimeOfDay.AFTERNOON, suggestionRepo.stored.first().timeOfDay)
    }

    @Test
    fun eveningTimeOfDayMaps() = runTest {
        val json = """
        [{"id":0,"text":"Evening","category":"EXERCISE","tags":[],"timeOfDay":"EVENING"}]
        """.trimIndent()
        createLoader(classJson = EMPTY_JSON, suggestJson = json)
        loader.load()
        assertEquals(TimeOfDay.EVENING, suggestionRepo.stored.first().timeOfDay)
    }

    @Test
    fun nightTimeOfDayMaps() = runTest {
        val json = """
        [{"id":0,"text":"Night","category":"EXERCISE","tags":[],"timeOfDay":"NIGHT"}]
        """.trimIndent()
        createLoader(classJson = EMPTY_JSON, suggestJson = json)
        loader.load()
        assertEquals(TimeOfDay.NIGHT, suggestionRepo.stored.first().timeOfDay)
    }

    @Test
    fun unknownTimeOfDayDefaultsToMorning() = runTest {
        val json = """
        [{"id":0,"text":"Unknown tod","category":"EXERCISE","tags":[],"timeOfDay":"MIDNIGHT"}]
        """.trimIndent()
        createLoader(classJson = EMPTY_JSON, suggestJson = json)
        loader.load()
        assertEquals(TimeOfDay.MORNING, suggestionRepo.stored.first().timeOfDay)
    }

    @Test
    fun nullTimeOfDayRemainsNull() = runTest {
        val json = """
        [{"id":0,"text":"No tod","category":"EXERCISE","tags":[],"timeOfDay":null}]
        """.trimIndent()
        createLoader(classJson = EMPTY_JSON, suggestJson = json)
        loader.load()
        assertNull(suggestionRepo.stored.first().timeOfDay)
    }
}
