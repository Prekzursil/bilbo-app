package dev.bilbo.app.ui

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure-JVM unit tests for [BilboRoute] constants. Touches every route string
 * to guarantee the constants are evaluated and to lock the values against
 * accidental rename regressions.
 */
class BilboRouteTest {
    @Test
    fun `top-level tab routes`() {
        assertEquals("dashboard", BilboRoute.DASHBOARD)
        assertEquals("focus", BilboRoute.FOCUS)
        assertEquals("insights", BilboRoute.INSIGHTS)
        assertEquals("social", BilboRoute.SOCIAL)
        assertEquals("settings", BilboRoute.SETTINGS)
    }

    @Test
    fun `dashboard and insights sub-routes`() {
        assertEquals("budget", BilboRoute.BUDGET)
        assertEquals("insights/weekly/{weekStart}", BilboRoute.WEEKLY_INSIGHT)
        assertEquals("analog/suggestions", BilboRoute.ANALOG_SUGGESTIONS)
        assertEquals("interests/setup", BilboRoute.INTERESTS_ONBOARDING)
    }

    @Test
    fun `social sub-routes`() {
        assertEquals("social/hub", BilboRoute.SOCIAL_HUB)
        assertEquals("social/buddies", BilboRoute.BUDDY_PAIRS)
        assertEquals("social/circles", BilboRoute.CIRCLES)
        assertEquals("social/challenges", BilboRoute.CHALLENGES)
        assertEquals("social/leaderboard", BilboRoute.LEADERBOARD)
        assertEquals("social/digest", BilboRoute.DIGEST)
    }

    @Test
    fun `settings sub-routes`() {
        assertEquals("settings/enforcement", BilboRoute.SETTINGS_ENFORCEMENT)
        assertEquals("settings/economy", BilboRoute.SETTINGS_ECONOMY)
        assertEquals("settings/emotional", BilboRoute.SETTINGS_EMOTIONAL)
        assertEquals("settings/ai", BilboRoute.SETTINGS_AI)
        assertEquals("settings/social", BilboRoute.SETTINGS_SOCIAL)
        assertEquals("settings/notifications", BilboRoute.SETTINGS_NOTIFICATIONS)
        assertEquals("settings/data", BilboRoute.SETTINGS_DATA)
        assertEquals("settings/data/anonymization", BilboRoute.DATA_ANONYMIZATION)
    }

    @Test
    fun `onboarding route`() {
        assertEquals("onboarding", BilboRoute.ONBOARDING)
    }

    @Test
    fun `every route is non-blank`() {
        val all =
            listOf(
                BilboRoute.DASHBOARD,
                BilboRoute.FOCUS,
                BilboRoute.INSIGHTS,
                BilboRoute.SOCIAL,
                BilboRoute.SETTINGS,
                BilboRoute.BUDGET,
                BilboRoute.WEEKLY_INSIGHT,
                BilboRoute.ANALOG_SUGGESTIONS,
                BilboRoute.INTERESTS_ONBOARDING,
                BilboRoute.SOCIAL_HUB,
                BilboRoute.BUDDY_PAIRS,
                BilboRoute.CIRCLES,
                BilboRoute.CHALLENGES,
                BilboRoute.LEADERBOARD,
                BilboRoute.DIGEST,
                BilboRoute.SETTINGS_ENFORCEMENT,
                BilboRoute.SETTINGS_ECONOMY,
                BilboRoute.SETTINGS_EMOTIONAL,
                BilboRoute.SETTINGS_AI,
                BilboRoute.SETTINGS_SOCIAL,
                BilboRoute.SETTINGS_NOTIFICATIONS,
                BilboRoute.SETTINGS_DATA,
                BilboRoute.DATA_ANONYMIZATION,
                BilboRoute.ONBOARDING,
            )
        for (r in all) assertTrue(r.isNotBlank())
    }
}
