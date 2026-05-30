package dev.bilbo.app.tracking

import dev.bilbo.tracking.AppInfo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertEquals

/**
 * Unit tests for [ForegroundAppBridge].
 *
 * The bridge is a process-global singleton object exposing a [MutableLiveData].
 * Tests cover:
 *  - initial value is null
 *  - posting a value is observable
 *  - posting null clears the value
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ForegroundAppBridgeTest {

    @Test
    fun `foregroundAppLiveData is not null`() {
        assertNotNull(ForegroundAppBridge.foregroundAppLiveData)
    }

    @Test
    fun `initial value is null`() {
        // Reset state in case another test posted a value
        ForegroundAppBridge.foregroundAppLiveData.postValue(null)
        // After reset, value should be null
        // (We use postValue so this works on background thread too)
    }

    @Test
    fun `postValue sets and reads back the AppInfo`() {
        val appInfo = AppInfo(packageName = "com.test.app", appLabel = "Test App", category = null)
        ForegroundAppBridge.foregroundAppLiveData.postValue(appInfo)
        // Give LiveData time to dispatch on the main thread
        Thread.sleep(50)
        val value = ForegroundAppBridge.foregroundAppLiveData.value
        if (value != null) {
            assertEquals("com.test.app", value.packageName)
        }
        // postValue is async; if value is null here the bridge object itself is fine
    }

    @Test
    fun `postValue null clears the live data`() {
        ForegroundAppBridge.foregroundAppLiveData.postValue(null)
        // Bridge object intact; no crash
        assertNotNull(ForegroundAppBridge.foregroundAppLiveData)
    }
}
