package dev.bilbo.app.ui.overlay

import androidx.compose.ui.graphics.Color
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Pure-JVM unit tests for [OverlayPalette] constants — touches every property
 * initializer so Kover counts each Color() construction.
 */
class OverlayPaletteTest {
    @Test
    fun `every overlay colour is a valid non-default Color`() {
        val all =
            listOf(
                OverlayPalette.Primary,
                OverlayPalette.FpGreen,
                OverlayPalette.FpYellow,
                OverlayPalette.FpRed,
                OverlayPalette.OnSurface,
                OverlayPalette.Surface,
                OverlayPalette.Subtle,
                OverlayPalette.Background,
            )
        for (c in all) {
            assertNotEquals(Color.Unspecified, c)
        }
    }

    @Test
    fun `preview background constant matches ARGB literal`() {
        assertEquals(0xFF1A2C3D, OverlayPalette.PREVIEW_BACKGROUND)
    }

    @Test
    fun `fp colours are pairwise distinct`() {
        assertNotEquals(OverlayPalette.FpGreen, OverlayPalette.FpYellow)
        assertNotEquals(OverlayPalette.FpYellow, OverlayPalette.FpRed)
        assertNotEquals(OverlayPalette.FpGreen, OverlayPalette.FpRed)
    }

    @Test
    fun `primary and background are not the same colour`() {
        assertNotEquals(OverlayPalette.Primary, OverlayPalette.Background)
    }
}
