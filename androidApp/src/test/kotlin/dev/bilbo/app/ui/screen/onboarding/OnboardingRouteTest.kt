package dev.bilbo.app.ui.screen.onboarding

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingRouteTest {
    @Test
    fun `every onboarding route has the expected slash path`() {
        assertEquals("onboarding/welcome", OnboardingRoute.WELCOME)
        assertEquals("onboarding/permissions", OnboardingRoute.PERMISSIONS)
        assertEquals("onboarding/app_classification", OnboardingRoute.APP_CLASSIFICATION)
        assertEquals("onboarding/first_intent", OnboardingRoute.FIRST_INTENT)
    }

    @Test
    fun `routes are unique and non-blank`() {
        val all =
            setOf(
                OnboardingRoute.WELCOME,
                OnboardingRoute.PERMISSIONS,
                OnboardingRoute.APP_CLASSIFICATION,
                OnboardingRoute.FIRST_INTENT,
            )
        assertEquals(4, all.size)
        for (r in all) assertTrue(r.isNotBlank())
    }
}
