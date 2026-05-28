package dev.bilbo.app.ui.screen.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// FirstIntentScreen.kt
// Bilbo — Onboarding Screen 5: First Intent Demo
//
// Guided walkthrough of the Intent Gatekeeper with coaching text.
// Simulates the gatekeeper experience step by step.

// MARK: - Layout / palette constants

private const val ARGB_GATEKEEPER_BG = 0xFF0F2240
private const val ARGB_AMBER = 0xFFFFB300
private const val ALPHA_BORDER = 0.3f
private const val PULSE_MIN = 0.4f
private const val PULSE_MAX = 1f
private const val PULSE_PERIOD_MS = 900
private const val SLIDE_DIVISOR = 2
private const val SCREEN_PAD_H_DP = 24
private const val SCREEN_PAD_TOP_DP = 48
private const val SCREEN_PAD_BOTTOM_DP = 32
private const val PROGRESS_GAP_DP = 6
private const val PROGRESS_HEIGHT_DP = 4
private const val CARD_CORNER_DP = 24
private const val INTRO_CARD_HEIGHT_DP = 220
private const val INTRO_ICON_DP = 64
private const val CARD_PAD_DP = 24
private const val BUTTON_HEIGHT_DP = 56
private const val BUTTON_CORNER_DP = 16
private const val SPACE_TITLE_DP = 32
private const val SPACE_CARD_DP = 40
private const val SPACE_SM_DP = 8
private const val SPACE_MD_DP = 12
private const val SPACE_LG_DP = 16

// MARK: - Demo steps

private enum class DemoStep(
    val coaching: String,
    val buttonLabel: String,
) {
    INTRO(
        coaching = "When you open a tracked app, Bilbo pauses and asks: why are you here?",
        buttonLabel = "Try it",
    ),
    GATEKEEPER_SHOWN(
        coaching = "This is the Intent Gatekeeper. It appears before you enter any tracked app.",
        buttonLabel = "I see it",
    ),
    INTENT_ENTERED(
        coaching = "You stated your intent. Bilbo notes this and tracks if you followed through.",
        buttonLabel = "Got it",
    ),
    TIMER_SHOWN(
        coaching = "A gentle timer reminds you of your stated session length.",
        buttonLabel = "Makes sense",
    ),
    COMPLETE(
        coaching = "You're all set! Bilbo will now help you stay intentional every day.",
        buttonLabel = "Complete Setup",
    ),
}

@Composable
fun FirstIntentScreen(
    onCompleteSetup: () -> Unit,
    onBack: () -> Unit,
) {
    var currentStep by remember { mutableStateOf(DemoStep.INTRO) }
    val steps = DemoStep.entries
    val stepIndex = steps.indexOf(currentStep)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = SCREEN_PAD_H_DP.dp)
                .padding(top = SCREEN_PAD_TOP_DP.dp, bottom = SCREEN_PAD_BOTTOM_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DemoProgressBar(stepCount = steps.size, currentIndex = stepIndex)

        Spacer(modifier = Modifier.height(SPACE_TITLE_DP.dp))

        Text(
            text = "Let's try it out!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        )

        Spacer(modifier = Modifier.height(SPACE_CARD_DP.dp))

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                slideInHorizontally(initialOffsetX = { it / SLIDE_DIVISOR }) + fadeIn() togetherWith
                    slideOutHorizontally(targetOffsetX = { -it / SLIDE_DIVISOR }) + fadeOut()
            },
            label = "demo_step",
        ) { step ->
            DemoStepCard(step = step)
        }

        Spacer(modifier = Modifier.height(SPACE_TITLE_DP.dp))

        AnimatedContent(targetState = currentStep.coaching, label = "coaching") { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = SPACE_SM_DP.dp),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        DemoNavButtons(
            buttonLabel = currentStep.buttonLabel,
            showBack = stepIndex == 0,
            onNext = {
                if (currentStep == DemoStep.COMPLETE) onCompleteSetup() else currentStep = steps[stepIndex + 1]
            },
            onBack = onBack,
        )
    }
}

@Composable
private fun DemoNavButtons(
    buttonLabel: String,
    showBack: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Button(
        onClick = onNext,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(BUTTON_HEIGHT_DP.dp),
        shape = RoundedCornerShape(BUTTON_CORNER_DP.dp),
    ) {
        Text(
            text = buttonLabel,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
    }

    if (showBack) {
        Spacer(modifier = Modifier.height(SPACE_SM_DP.dp))
        TextButton(onClick = onBack) {
            Text("← Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DemoProgressBar(
    stepCount: Int,
    currentIndex: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PROGRESS_GAP_DP.dp),
    ) {
        repeat(stepCount) { i ->
            Box(
                modifier =
                    Modifier
                        .height(PROGRESS_HEIGHT_DP.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(
                            if (i <= currentIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
            )
        }
    }
}

@Composable
private fun DemoStepCard(step: DemoStep) {
    when (step) {
        DemoStep.INTRO -> IntroCard()
        DemoStep.GATEKEEPER_SHOWN -> GatekeeperCard()
        DemoStep.INTENT_ENTERED -> IntentEnteredCard()
        DemoStep.TIMER_SHOWN -> TimerCard()
        DemoStep.COMPLETE -> CompleteCard()
    }
}

@Composable
private fun IntroCard() {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(INTRO_CARD_HEIGHT_DP.dp),
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(INTRO_ICON_DP.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(SPACE_MD_DP.dp))
                Text(
                    "Tap an app icon…",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GatekeeperCard() {
    Card(
        modifier =
            Modifier
                .fillMaxWidth(),
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = Color(ARGB_GATEKEEPER_BG)),
    ) {
        Column(
            modifier = Modifier.padding(CARD_PAD_DP.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Why are you opening Instagram?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(SPACE_LG_DP.dp))
            OutlinedTextField(
                value = "Check messages",
                onValueChange = {},
                placeholder = { Text("Type your intent…") },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(ARGB_AMBER),
                        unfocusedBorderColor = Color.White.copy(alpha = ALPHA_BORDER),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("How long? 10 min", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun IntentEnteredCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Intent logged", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("\"Check messages\" · 10 min", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TimerCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = PULSE_MAX,
        targetValue = PULSE_MIN,
        animationSpec = infiniteRepeatable(tween(PULSE_PERIOD_MS), RepeatMode.Reverse),
        label = "pulse_alpha",
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "09:42",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
            )
            Text(
                "remaining of 10 min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompleteCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("⚡", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "+10 Focus Points",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Earned for completing your intent!",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
