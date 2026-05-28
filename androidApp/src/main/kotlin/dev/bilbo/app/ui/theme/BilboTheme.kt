package dev.bilbo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// BilboTheme.kt
// Bilbo — Material 3 App Theme
//
// Calming palette:
//   Primary: Teal #00897B
//   Secondary: Warm Amber #FFB300
//   Background: Off-white #FAFAFA
//   Surface: White
//   Error: Soft Red #E57373
// Rounded corner shapes throughout.
// Light and dark variants.

// ── MARK: Color primitives ─────────────────────────────────────────────

object BilboColor {
    // ARGB literals as named compile-time constants (detekt ignores const
    // declarations) so the Color primitives below read as named values.
    private const val ARGB_TEAL40 = 0xFF00897B
    private const val ARGB_TEAL80 = 0xFF4DB6AC
    private const val ARGB_TEAL20 = 0xFF004D40
    private const val ARGB_TEAL10 = 0xFF00251A
    private const val ARGB_AMBER40 = 0xFFFFB300
    private const val ARGB_AMBER80 = 0xFFFFD54F
    private const val ARGB_AMBER20 = 0xFFE65100
    private const val ARGB_AMBER10 = 0xFF4E2000
    private const val ARGB_PURPLE40 = 0xFF5C33A3
    private const val ARGB_PURPLE80 = 0xFFB39DDB
    private const val ARGB_PURPLE20 = 0xFF1A0F3C
    private const val ARGB_NEUTRAL10 = 0xFF191C1C
    private const val ARGB_NEUTRAL20 = 0xFF2D3131
    private const val ARGB_NEUTRAL90 = 0xFFE0E3E3
    private const val ARGB_NEUTRAL95 = 0xFFEFF1F1
    private const val ARGB_NEUTRAL99 = 0xFFFAFAFA
    private const val ARGB_NEUTRAL_VARIANT30 = 0xFF3F4848
    private const val ARGB_NEUTRAL_VARIANT50 = 0xFF6F7979
    private const val ARGB_NEUTRAL_VARIANT80 = 0xFFBEC8C8
    private const val ARGB_NEUTRAL_VARIANT90 = 0xFFDAE4E4
    private const val ARGB_ERROR40 = 0xFFE57373
    private const val ARGB_ERROR80 = 0xFFEF9A9A
    private const val ARGB_ERROR10 = 0xFFB71C1C
    private const val ARGB_SURFACE_DARK = 0xFF191C1C
    private const val ARGB_BACKGROUND_LIGHT = 0xFFFAFAFA
    private const val ARGB_BACKGROUND_DARK = 0xFF101414

    // Brand
    val Teal40 = Color(ARGB_TEAL40) // Primary (light)
    val Teal80 = Color(ARGB_TEAL80) // Primary container (light) / Primary (dark)
    val Teal20 = Color(ARGB_TEAL20) // On primary container
    val Teal10 = Color(ARGB_TEAL10)

    val Amber40 = Color(ARGB_AMBER40) // Secondary (light)
    val Amber80 = Color(ARGB_AMBER80) // Secondary container (light) / Secondary (dark)
    val Amber20 = Color(ARGB_AMBER20)
    val Amber10 = Color(ARGB_AMBER10)

    val Purple40 = Color(ARGB_PURPLE40) // Tertiary
    val Purple80 = Color(ARGB_PURPLE80)
    val Purple20 = Color(ARGB_PURPLE20)

    // Neutral
    val Neutral10 = Color(ARGB_NEUTRAL10)
    val Neutral20 = Color(ARGB_NEUTRAL20)
    val Neutral90 = Color(ARGB_NEUTRAL90)
    val Neutral95 = Color(ARGB_NEUTRAL95)
    val Neutral99 = Color(ARGB_NEUTRAL99)

    val NeutralVariant30 = Color(ARGB_NEUTRAL_VARIANT30)
    val NeutralVariant50 = Color(ARGB_NEUTRAL_VARIANT50)
    val NeutralVariant80 = Color(ARGB_NEUTRAL_VARIANT80)
    val NeutralVariant90 = Color(ARGB_NEUTRAL_VARIANT90)

    // Semantic
    val Error40 = Color(ARGB_ERROR40) // Soft red
    val Error80 = Color(ARGB_ERROR80)
    val Error10 = Color(ARGB_ERROR10)
    val OnError = Color.White

    // Surface
    val SurfaceLight = Color.White
    val SurfaceDark = Color(ARGB_SURFACE_DARK)
    val BackgroundLight = Color(ARGB_BACKGROUND_LIGHT)
    val BackgroundDark = Color(ARGB_BACKGROUND_DARK)
}

// ── MARK: Light Color Scheme ───────────────────────────────────────────

private val LightColorScheme =
    lightColorScheme(
        primary = BilboColor.Teal40,
        onPrimary = Color.White,
        primaryContainer = BilboColor.Teal80,
        onPrimaryContainer = BilboColor.Teal10,
        secondary = BilboColor.Amber40,
        onSecondary = Color.White,
        secondaryContainer = BilboColor.Amber80,
        onSecondaryContainer = BilboColor.Amber10,
        tertiary = BilboColor.Purple40,
        onTertiary = Color.White,
        tertiaryContainer = BilboColor.Purple80,
        onTertiaryContainer = BilboColor.Purple20,
        error = BilboColor.Error40,
        onError = BilboColor.OnError,
        errorContainer = BilboColor.Error80,
        onErrorContainer = BilboColor.Error10,
        background = BilboColor.BackgroundLight,
        onBackground = BilboColor.Neutral10,
        surface = BilboColor.SurfaceLight,
        onSurface = BilboColor.Neutral10,
        surfaceVariant = BilboColor.NeutralVariant90,
        onSurfaceVariant = BilboColor.NeutralVariant30,
        outline = BilboColor.NeutralVariant50,
        outlineVariant = BilboColor.NeutralVariant80,
        inverseSurface = BilboColor.Neutral20,
        inverseOnSurface = BilboColor.Neutral95,
        inversePrimary = BilboColor.Teal80,
    )

// ── MARK: Dark Color Scheme ────────────────────────────────────────────

private val DarkColorScheme =
    darkColorScheme(
        primary = BilboColor.Teal80,
        onPrimary = BilboColor.Teal20,
        primaryContainer = BilboColor.Teal40,
        onPrimaryContainer = BilboColor.Teal80,
        secondary = BilboColor.Amber80,
        onSecondary = BilboColor.Amber20,
        secondaryContainer = BilboColor.Amber40,
        onSecondaryContainer = BilboColor.Amber80,
        tertiary = BilboColor.Purple80,
        onTertiary = BilboColor.Purple20,
        tertiaryContainer = BilboColor.Purple40,
        onTertiaryContainer = BilboColor.Purple80,
        error = BilboColor.Error80,
        onError = BilboColor.Error10,
        errorContainer = BilboColor.Error40,
        onErrorContainer = BilboColor.Error80,
        background = BilboColor.BackgroundDark,
        onBackground = BilboColor.Neutral90,
        surface = BilboColor.SurfaceDark,
        onSurface = BilboColor.Neutral90,
        surfaceVariant = BilboColor.NeutralVariant30,
        onSurfaceVariant = BilboColor.NeutralVariant80,
        outline = BilboColor.NeutralVariant50,
        outlineVariant = BilboColor.NeutralVariant30,
        inverseSurface = BilboColor.Neutral90,
        inverseOnSurface = BilboColor.Neutral10,
        inversePrimary = BilboColor.Teal40,
    )

// ── MARK: Shapes ──────────────────────────────────────────────────────

private const val CORNER_EXTRA_SMALL_DP = 6
private const val CORNER_SMALL_DP = 10
private const val CORNER_MEDIUM_DP = 16
private const val CORNER_LARGE_DP = 24
private const val CORNER_EXTRA_LARGE_DP = 32

val BilboShapes =
    Shapes(
        extraSmall = RoundedCornerShape(CORNER_EXTRA_SMALL_DP.dp),
        small = RoundedCornerShape(CORNER_SMALL_DP.dp),
        medium = RoundedCornerShape(CORNER_MEDIUM_DP.dp),
        large = RoundedCornerShape(CORNER_LARGE_DP.dp),
        extraLarge = RoundedCornerShape(CORNER_EXTRA_LARGE_DP.dp),
    )

// ── MARK: Theme composable ─────────────────────────────────────────────

@Composable
fun BilboTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = BilboShapes,
        typography = BilboTypography,
        content = content,
    )
}
