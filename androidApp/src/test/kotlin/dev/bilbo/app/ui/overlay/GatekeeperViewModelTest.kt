package dev.bilbo.app.ui.overlay

import app.cash.turbine.test
import dev.bilbo.data.AppProfileRepository
import dev.bilbo.data.IntentRepository
import dev.bilbo.domain.AppCategory
import dev.bilbo.domain.AppProfile
import dev.bilbo.domain.EnforcementMode
import dev.bilbo.domain.IntentDeclaration
import dev.bilbo.tracking.AppInfo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Unit tests for [GatekeeperViewModel]. Pure JVM — no Robolectric required.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GatekeeperViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val intentRepo = mockk<IntentRepository>()
    private val profileRepo = mockk<AppProfileRepository>()
    private val appInfo = AppInfo(packageName = "com.example.app", appLabel = "Example", category = null)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadAppProfile sets bypass and empty-calories flags from profile`() =
        runTest(dispatcher) {
            val vm = GatekeeperViewModel(intentRepo, profileRepo)
            coEvery { profileRepo.getByPackageName(appInfo.packageName) } returns
                AppProfile(
                    packageName = appInfo.packageName,
                    appLabel = "Example",
                    category = AppCategory.EMPTY_CALORIES,
                    enforcementMode = EnforcementMode.NUDGE,
                    isBypassed = true,
                )
            vm.loadAppProfile(appInfo)
            advanceUntilIdle()
            val state = vm.uiState.value
            assertTrue(state.isBypassedApp)
            assertTrue(state.isEmptyCaloriesApp)
        }

    @Test
    fun `loadAppProfile leaves defaults when no profile exists`() =
        runTest(dispatcher) {
            val vm = GatekeeperViewModel(intentRepo, profileRepo)
            coEvery { profileRepo.getByPackageName(any()) } returns null
            vm.loadAppProfile(appInfo)
            advanceUntilIdle()
            val state = vm.uiState.value
            assertEquals(false, state.isBypassedApp)
            assertEquals(false, state.isEmptyCaloriesApp)
        }

    @Test
    fun `loadAppProfile swallows repository exception and keeps state`() =
        runTest(dispatcher) {
            val vm = GatekeeperViewModel(intentRepo, profileRepo)
            coEvery { profileRepo.getByPackageName(any()) } throws RuntimeException("boom")
            vm.loadAppProfile(appInfo)
            advanceUntilIdle()
            assertEquals(false, vm.uiState.value.isBypassedApp)
        }

    @Test
    fun `onIntentionChanged ignores text over the max length`() {
        val vm = GatekeeperViewModel(intentRepo, profileRepo)
        val longText = "x".repeat(101)
        vm.onIntentionChanged(longText)
        assertEquals("", vm.uiState.value.intention)
        vm.onIntentionChanged("ok")
        assertEquals("ok", vm.uiState.value.intention)
    }

    @Test
    fun `onDurationSelected updates duration`() {
        val vm = GatekeeperViewModel(intentRepo, profileRepo)
        vm.onDurationSelected(42)
        assertEquals(42, vm.uiState.value.selectedDurationMinutes)
    }

    @Test
    fun `onStart persists declaration and invokes timer callback`() =
        runTest(dispatcher) {
            val vm = GatekeeperViewModel(intentRepo, profileRepo)
            coEvery { intentRepo.insert(any()) } returns 99L
            var capturedId = -1L
            var capturedDur = -1
            vm.onStart(appInfo) { id, dur ->
                capturedId = id
                capturedDur = dur
            }
            advanceUntilIdle()
            assertEquals(99L, capturedId)
            assertEquals(15, capturedDur)
            assertEquals(99L, vm.uiState.value.activeDeclarationId)
            assertEquals(false, vm.uiState.value.isLoading)
        }

    @Test
    fun `onStart reports error when repository throws`() =
        runTest(dispatcher) {
            val vm = GatekeeperViewModel(intentRepo, profileRepo)
            coEvery { intentRepo.insert(any()) } throws RuntimeException("db down")
            vm.onStart(appInfo) { _, _ -> }
            advanceUntilIdle()
            val state = vm.uiState.value
            assertEquals(false, state.isLoading)
            assertEquals("Could not save intent. Please try again.", state.error)
        }

    @Test
    fun `onStart is a no-op when already loading`() =
        runTest(dispatcher) {
            val vm = GatekeeperViewModel(intentRepo, profileRepo)
            // simulate in-flight by triggering once with a slow insert (never completes
            // before we attempt the second call). Easiest: mark state externally via
            // first call, then trigger second call before dispatcher advances.
            coEvery { intentRepo.insert(any()) } returns 1L
            vm.onStart(appInfo) { _, _ -> }
            // second call before advanceUntilIdle — should bail out because isLoading == true
            var calls = 0
            vm.onStart(appInfo) { _, _ -> calls++ }
            advanceUntilIdle()
            assertEquals(0, calls)
        }

    @Test
    fun `checkActiveDeclaration returns the most recent unexpired declaration`() =
        runTest(dispatcher) {
            val vm = GatekeeperViewModel(intentRepo, profileRepo)
            val now = Clock.System.now()
            val expired =
                IntentDeclaration(
                    timestamp = kotlin.time.Instant.fromEpochSeconds(now.epochSeconds - 3600),
                    declaredApp = "com.example.app",
                    declaredDurationMinutes = 1,
                )
            val active =
                IntentDeclaration(
                    timestamp = now,
                    declaredApp = "com.example.app",
                    declaredDurationMinutes = 10,
                )
            coEvery { intentRepo.getByApp("com.example.app") } returns listOf(expired, active)
            var result: IntentDeclaration? = null
            vm.checkActiveDeclaration("com.example.app") { result = it }
            advanceUntilIdle()
            assertEquals(active.declaredDurationMinutes, result?.declaredDurationMinutes)
        }

    @Test
    fun `checkActiveDeclaration returns null when only expired declarations exist`() =
        runTest(dispatcher) {
            val vm = GatekeeperViewModel(intentRepo, profileRepo)
            val now = Clock.System.now()
            val expired =
                IntentDeclaration(
                    timestamp = kotlin.time.Instant.fromEpochSeconds(now.epochSeconds - 3600),
                    declaredApp = "com.example.app",
                    declaredDurationMinutes = 1,
                )
            coEvery { intentRepo.getByApp(any()) } returns listOf(expired)
            var called = false
            var result: IntentDeclaration? = expired
            vm.checkActiveDeclaration("com.example.app") {
                called = true
                result = it
            }
            advanceUntilIdle()
            assertTrue(called)
            assertNull(result)
        }

    @Test
    fun `checkActiveDeclaration returns null on repository error`() =
        runTest(dispatcher) {
            val vm = GatekeeperViewModel(intentRepo, profileRepo)
            coEvery { intentRepo.getByApp(any()) } throws RuntimeException("io")
            var seen: IntentDeclaration? = null
            var called = false
            vm.checkActiveDeclaration("x") {
                called = true
                seen = it
            }
            advanceUntilIdle()
            assertTrue(called)
            assertNull(seen)
        }

    @Test
    fun `clearError resets error to null`() =
        runTest(dispatcher) {
            val vm = GatekeeperViewModel(intentRepo, profileRepo)
            coEvery { intentRepo.insert(any()) } throws RuntimeException("x")
            vm.onStart(appInfo) { _, _ -> }
            advanceUntilIdle()
            assertEquals("Could not save intent. Please try again.", vm.uiState.value.error)
            vm.clearError()
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun `uiState turbine emits initial defaults`() =
        runTest(dispatcher) {
            val vm = GatekeeperViewModel(intentRepo, profileRepo)
            vm.uiState.test {
                val first = awaitItem()
                assertEquals(15, first.selectedDurationMinutes)
                assertEquals("", first.intention)
            }
        }
}
