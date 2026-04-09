package dev.spark.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Brand colours ─────────────────────────────────────────────────────────────
val SparkOrange = Color(0xFFFF7200)
val SparkOrangeDim = Color(0xFFCC5B00)
val SparkDeepBlue = Color(0xFF0D1B2A)
val SparkMidBlue = Color(0xFF1B2E40)
val SparkSurface = Color(0xFF121827)
val SparkOnSurface = Color(0xFFE8EAF0)
val SparkOutline = Color(0xFF3A4555)

private val LightColorScheme = lightColorScheme(
    primary = SparkOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF3D1100),
    secondary = SparkDeepBlue,
    onSecondary = Color.White,
    background = Color(0xFFFAF9F6),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    outline = Color(0xFF73788A),
)

private val DarkColorScheme = darkColorScheme(
    primary = SparkOrange,
    onPrimary = Color.White,
    primaryContainer = SparkOrangeDim,
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFF7FB3D3),
    onSecondary = SparkDeepBlue,
    background = SparkDeepBlue,
    onBackground = SparkOnSurface,
    surface = SparkSurface,
    onSurface = SparkOnSurface,
    surfaceVariant = SparkMidBlue,
    onSurfaceVariant = Color(0xFFB8BECA),
    outline = SparkOutline,
)

@Composable
fun SparkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SparkTypography,
        content = content,
    )
}
