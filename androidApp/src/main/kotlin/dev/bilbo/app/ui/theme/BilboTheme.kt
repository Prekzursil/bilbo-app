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

package dev.bilbo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── MARK: Color primitives ─────────────────────────────────────────────

object BilboColor {

    // Brand
    val Teal40          = Color(0xFF00897B)   // Primary (light)
    val Teal80          = Color(0xFF4DB6AC)   // Primary container (light) / Primary (dark)
    val Teal20          = Color(0xFF004D40)   // On primary container
    val Teal10          = Color(0xFF00251A)

    val Amber40         = Color(0xFFFFB300)   // Secondary (light)
    val Amber80         = Color(0xFFFFD54F)   // Secondary container (light) / Secondary (dark)
    val Amber20         = Color(0xFFE65100)
    val Amber10         = Color(0xFF4E2000)

    val Purple40        = Color(0xFF5C33A3)   // Tertiary
    val Purple80        = Color(0xFFB39DDB)
    val Purple20        = Color(0xFF1A0F3C)

    // Neutral
    val Neutral10       = Color(0xFF191C1C)
    val Neutral20       = Color(0xFF2D3131)
    val Neutral90       = Color(0xFFE0E3E3)
    val Neutral95       = Color(0xFFEFF1F1)
    val Neutral99       = Color(0xFFFAFAFA)

    val NeutralVariant30 = Color(0xFF3F4848)
    val NeutralVariant50 = Color(0xFF6F7979)
    val NeutralVariant80 = Color(0xFFBEC8C8)
    val NeutralVariant90 = Color(0xFFDAE4E4)

    // Semantic
    val Error40         = Color(0xFFE57373)   // Soft red
    val Error80         = Color(0xFFEF9A9A)
    val Error10         = Color(0xFFB71C1C)
    val OnError         = Color.White

    // Surface
    val SurfaceLight    = Color.White
    val SurfaceDark     = Color(0xFF191C1C)
    val BackgroundLight = Color(0xFFFAFAFA)
    val BackgroundDark  = Color(0xFF101414)
}

// ── MARK: Light Color Scheme ───────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary               = BilboColor.Teal40,
    onPrimary             = Color.White,
    primaryContainer      = BilboColor.Teal80,
    onPrimaryContainer    = BilboColor.Teal10,

    secondary             = BilboColor.Amber40,
    onSecondary           = Color.White,
    secondaryContainer    = BilboColor.Amber80,
    onSecondaryContainer  = BilboColor.Amber10,

    tertiary              = BilboColor.Purple40,
    onTertiary            = Color.White,
    tertiaryContainer     = BilboColor.Purple80,
    onTertiaryContainer   = BilboColor.Purple20,

    error                 = BilboColor.Error40,
    onError               = BilboColor.OnError,
    errorContainer        = BilboColor.Error80,
    onErrorContainer      = BilboColor.Error10,

    background            = BilboColor.BackgroundLight,
    onBackground          = BilboColor.Neutral10,

    surface               = BilboColor.SurfaceLight,
    onSurface             = BilboColor.Neutral10,
    surfaceVariant        = BilboColor.NeutralVariant90,
    onSurfaceVariant      = BilboColor.NeutralVariant30,

    outline               = BilboColor.NeutralVariant50,
    outlineVariant        = BilboColor.NeutralVariant80,

    inverseSurface        = BilboColor.Neutral20,
    inverseOnSurface      = BilboColor.Neutral95,
    inversePrimary        = BilboColor.Teal80
)

// ── MARK: Dark Color Scheme ────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary               = BilboColor.Teal80,
    onPrimary             = BilboColor.Teal20,
    primaryContainer      = BilboColor.Teal40,
    onPrimaryContainer    = BilboColor.Teal80,

    secondary             = BilboColor.Amber80,
    onSecondary           = BilboColor.Amber20,
    secondaryContainer    = BilboColor.Amber40,
    onSecondaryContainer  = BilboColor.Amber80,

    tertiary              = BilboColor.Purple80,
    onTertiary            = BilboColor.Purple20,
    tertiaryContainer     = BilboColor.Purple40,
    onTertiaryContainer   = BilboColor.Purple80,

    error                 = BilboColor.Error80,
    onError               = BilboColor.Error10,
    errorContainer        = BilboColor.Error40,
    onErrorContainer      = BilboColor.Error80,

    background            = BilboColor.BackgroundDark,
    onBackground          = BilboColor.Neutral90,

    surface               = BilboColor.SurfaceDark,
    onSurface             = BilboColor.Neutral90,
    surfaceVariant        = BilboColor.NeutralVariant30,
    onSurfaceVariant      = BilboColor.NeutralVariant80,

    outline               = BilboColor.NeutralVariant50,
    outlineVariant        = BilboColor.NeutralVariant30,

    inverseSurface        = BilboColor.Neutral90,
    inverseOnSurface      = BilboColor.Neutral10,
    inversePrimary        = BilboColor.Teal40
)

// ── MARK: Shapes ──────────────────────────────────────────────────────

val BilboShapes = Shapes(
    extraSmall  = RoundedCornerShape(6.dp),
    small       = RoundedCornerShape(10.dp),
    medium      = RoundedCornerShape(16.dp),
    large       = RoundedCornerShape(24.dp),
    extraLarge  = RoundedCornerShape(32.dp)
)

// ── MARK: Theme composable ─────────────────────────────────────────────

@Composable
fun BilboTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes      = BilboShapes,
        typography  = BilboTypography,
        content     = content
    )
}
