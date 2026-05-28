package dev.bilbo.app.ui.overlay

import androidx.compose.ui.graphics.Color

/**
 * Shared colour palette for the full-screen gatekeeper/enforcement overlays.
 *
 * Centralising these values keeps the dark overlay surfaces consistent and avoids
 * duplicating the same ARGB literals across every overlay composable.
 */
internal object OverlayPalette {
    private const val ARGB_PRIMARY = 0xFF48B8A0
    private const val ARGB_FP_GREEN = 0xFF4CAF50
    private const val ARGB_FP_YELLOW = 0xFFFFC107
    private const val ARGB_FP_RED = 0xFFE53935
    private const val ARGB_ON_SURFACE = 0xFFE0EEF5
    private const val ARGB_SURFACE = 0xFF243344
    private const val ARGB_SUBTLE = 0xFF8AAFC4
    private const val ARGB_BACKGROUND = 0xFF1A2C3D

    const val PREVIEW_BACKGROUND = ARGB_BACKGROUND

    val Primary = Color(ARGB_PRIMARY)
    val FpGreen = Color(ARGB_FP_GREEN)
    val FpYellow = Color(ARGB_FP_YELLOW)
    val FpRed = Color(ARGB_FP_RED)
    val OnSurface = Color(ARGB_ON_SURFACE)
    val Surface = Color(ARGB_SURFACE)
    val Subtle = Color(ARGB_SUBTLE)
    val Background = Color(ARGB_BACKGROUND)
}
