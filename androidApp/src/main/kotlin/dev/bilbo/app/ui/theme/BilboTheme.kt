package dev.bilbo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// BilboTheme.kt
// Bilbo — Material 3 App Theme entry point.
//
// Colour primitives, schemes and shapes live in BilboColor.kt;
// typography lives in BilboType.kt.

// ── MARK: Theme composable ─────────────────────────────────────────────

@Composable
fun BilboTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) BilboDarkColorScheme else BilboLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = BilboShapes,
        typography = BilboTypography,
        content = content,
    )
}
