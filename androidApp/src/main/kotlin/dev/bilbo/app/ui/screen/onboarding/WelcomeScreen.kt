package dev.bilbo.app.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// WelcomeScreen.kt
// Bilbo — Onboarding Screen 1: Welcome

private const val ARGB_BG_NAVY = 0xFF0F2240
private const val ARGB_BG_PURPLE = 0xFF1A0F3C
private const val ARGB_AMBER = 0xFFFFB300
private const val ARGB_PURPLE = 0xFF5C33A3

private const val ALPHA_SUBTITLE = 0.7f
private const val ALPHA_DESC = 0.65f
private const val ALPHA_LOGO_BG = 0.8f
private const val ALPHA_FEATURE_BG = 0.5f

private const val SCREEN_PAD_H_DP = 32
private const val SCREEN_PAD_TOP_DP = 72
private const val SCREEN_PAD_BOTTOM_DP = 40
private const val LOGO_SIZE_DP = 96
private const val LOGO_ICON_DP = 56
private const val FEATURE_ICON_BOX_DP = 48
private const val FEATURE_ICON_DP = 26
private const val FEATURE_CORNER_DP = 14
private const val CTA_HEIGHT_DP = 56
private const val CTA_CORNER_DP = 16
private const val TITLE_FONT_SP = 36
private const val TITLE_LINE_HEIGHT_SP = 44
private const val SPACE_LOGO_DP = 32
private const val SPACE_TITLE_DP = 8
private const val SPACE_FEATURES_DP = 48
private const val SPACE_FEATURE_DP = 20
private const val SPACE_ICON_DP = 16

private data class FeatureHighlight(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

private val WELCOME_FEATURES =
    listOf(
        FeatureHighlight(Icons.Filled.FlashOn, "Focus Points", "Earn rewards for intentional screen time"),
        FeatureHighlight(Icons.Filled.Psychology, "Intent Gatekeeper", "Pause before opening distracting apps"),
        FeatureHighlight(Icons.Filled.Groups, "Social Accountability", "Grow alongside friends and circles"),
    )

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(ARGB_BG_NAVY), Color(ARGB_BG_PURPLE)),
                    ),
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = SCREEN_PAD_H_DP.dp)
                    .padding(top = SCREEN_PAD_TOP_DP.dp, bottom = SCREEN_PAD_BOTTOM_DP.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BilboLogoMark()
            Spacer(modifier = Modifier.height(SPACE_LOGO_DP.dp))
            WelcomeHeadline()
            Spacer(modifier = Modifier.height(SPACE_FEATURES_DP.dp))
            WELCOME_FEATURES.forEach { feature ->
                FeatureRow(feature = feature)
                Spacer(modifier = Modifier.height(SPACE_FEATURE_DP.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            GetStartedButton(onGetStarted = onGetStarted)
        }
    }
}

@Composable
private fun WelcomeHeadline() {
    Text(
        text = "Take control of\nyour screen time",
        style =
            MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = TITLE_FONT_SP.sp,
                lineHeight = TITLE_LINE_HEIGHT_SP.sp,
            ),
        color = Color.White,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(SPACE_TITLE_DP.dp))
    Text(
        text = "Bilbo helps you build intentional digital habits.",
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = ALPHA_SUBTITLE),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun GetStartedButton(onGetStarted: () -> Unit) {
    Button(
        onClick = onGetStarted,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(CTA_HEIGHT_DP.dp),
        shape = RoundedCornerShape(CTA_CORNER_DP.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(ARGB_AMBER)),
    ) {
        Text(
            text = "Get Started",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(ARGB_BG_NAVY),
        )
    }
}

@Composable
private fun BilboLogoMark() {
    Box(
        modifier =
            Modifier
                .size(LOGO_SIZE_DP.dp)
                .clip(CircleShape)
                .background(Color(ARGB_PURPLE).copy(alpha = ALPHA_LOGO_BG)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.FlashOn,
            contentDescription = "Bilbo logo",
            tint = Color(ARGB_AMBER),
            modifier = Modifier.size(LOGO_ICON_DP.dp),
        )
    }
}

@Composable
private fun FeatureRow(feature: FeatureHighlight) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(FEATURE_ICON_BOX_DP.dp)
                    .clip(RoundedCornerShape(FEATURE_CORNER_DP.dp))
                    .background(Color(ARGB_PURPLE).copy(alpha = ALPHA_FEATURE_BG)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = Color(ARGB_AMBER),
                modifier = Modifier.size(FEATURE_ICON_DP.dp),
            )
        }

        Spacer(modifier = Modifier.width(SPACE_ICON_DP.dp))

        Column {
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = ALPHA_DESC),
            )
        }
    }
}
