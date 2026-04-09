// SparkTheme.kt
// Spark — Material 3 App Theme
//
// Calming palette:
//   Primary: Teal #00897B
//   Secondary: Warm Amber #FFB300
//   Background: Off-white #FAFAFA
//   Surface: White
//   Error: Soft Red #E57373
// Rounded corner shapes throughout.
// Light and dark variants.

package dev.spark.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── MARK: Color primitives ─────────────────────────────────────────────

object SparkColor {

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
    primary               = SparkColor.Teal40,
    onPrimary             = Color.White,
    primaryContainer      = SparkColor.Teal80,
    onPrimaryContainer    = SparkColor.Teal10,

    secondary             = SparkColor.Amber40,
    onSecondary           = Color.White,
    secondaryContainer    = SparkColor.Amber80,
    onSecondaryContainer  = SparkColor.Amber10,

    tertiary              = SparkColor.Purple40,
    onTertiary            = Color.White,
    tertiaryContainer     = SparkColor.Purple80,
    onTertiaryContainer   = SparkColor.Purple20,

    error                 = SparkColor.Error40,
    onError               = SparkColor.OnError,
    errorContainer        = SparkColor.Error80,
    onErrorContainer      = SparkColor.Error10,

    background            = SparkColor.BackgroundLight,
    onBackground          = SparkColor.Neutral10,

    surface               = SparkColor.SurfaceLight,
    onSurface             = SparkColor.Neutral10,
    surfaceVariant        = SparkColor.NeutralVariant90,
    onSurfaceVariant      = SparkColor.NeutralVariant30,

    outline               = SparkColor.NeutralVariant50,
    outlineVariant        = SparkColor.NeutralVariant80,

    inverseSurface        = SparkColor.Neutral20,
    inverseOnSurface      = SparkColor.Neutral95,
    inversePrimary        = SparkColor.Teal80
)

// ── MARK: Dark Color Scheme ────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary               = SparkColor.Teal80,
    onPrimary             = SparkColor.Teal20,
    primaryContainer      = SparkColor.Teal40,
    onPrimaryContainer    = SparkColor.Teal80,

    secondary             = SparkColor.Amber80,
    onSecondary           = SparkColor.Amber20,
    secondaryContainer    = SparkColor.Amber40,
    onSecondaryContainer  = SparkColor.Amber80,

    tertiary              = SparkColor.Purple80,
    onTertiary            = SparkColor.Purple20,
    tertiaryContainer     = SparkColor.Purple40,
    onTertiaryContainer   = SparkColor.Purple80,

    error                 = SparkColor.Error80,
    onError               = SparkColor.Error10,
    errorContainer        = SparkColor.Error40,
    onErrorContainer      = SparkColor.Error80,

    background            = SparkColor.BackgroundDark,
    onBackground          = SparkColor.Neutral90,

    surface               = SparkColor.SurfaceDark,
    onSurface             = SparkColor.Neutral90,
    surfaceVariant        = SparkColor.NeutralVariant30,
    onSurfaceVariant      = SparkColor.NeutralVariant80,

    outline               = SparkColor.NeutralVariant50,
    outlineVariant        = SparkColor.NeutralVariant30,

    inverseSurface        = SparkColor.Neutral90,
    inverseOnSurface      = SparkColor.Neutral10,
    inversePrimary        = SparkColor.Teal40
)

// ── MARK: Shapes ──────────────────────────────────────────────────────

val SparkShapes = Shapes(
    extraSmall  = RoundedCornerShape(6.dp),
    small       = RoundedCornerShape(10.dp),
    medium      = RoundedCornerShape(16.dp),
    large       = RoundedCornerShape(24.dp),
    extraLarge  = RoundedCornerShape(32.dp)
)

// ── MARK: Typography ──────────────────────────────────────────────────

// Use system default (Roboto) as the fallback; replace with a custom font by
// adding the font file to res/font/ and referencing it here.
private val SparkFontFamily = FontFamily.Default

val SparkTypography = Typography(
    displayLarge  = TextStyle(fontFamily = SparkFontFamily, fontWeight = FontWeight.Black,    fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = SparkFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall  = TextStyle(fontFamily = SparkFontFamily, fontWeight = FontWeight.Bold,      fontSize = 36.sp, lineHeight = 44.sp),

    headlineLarge  = TextStyle(fontFamily = SparkFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = SparkFontFamily, fontWeight = FontWeight.Bold,      fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontFamily = SparkFontFamily, fontWeight = FontWeight.Bold,      fontSize = 24.sp, lineHeight = 32.sp),

    titleLarge  = TextStyle(fontFamily = SparkFontFamily, fontWeight = FontWeight.Bold,      fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = SparkFontFamily, fontWeight = FontWeight.SemiBold,  fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall  = TextStyle(fontFamily = SparkFontFamily, fontWeight = FontWeight.SemiBold,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),

    bodyLarge   = TextStyle(fontFamily = SparkFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium  = TextStyle(fontFamily = SparkFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall   = TextStyle(fontFamily = SparkFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),

    labelLarge  = TextStyle(fontFamily = SparkFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = SparkFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontFamily = SparkFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
)

// ── MARK: Theme composable ─────────────────────────────────────────────

@Composable
fun SparkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes      = SparkShapes,
        typography  = SparkTypography,
        content     = content
    )
}
