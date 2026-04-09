// FirstIntentScreen.kt
// Bilbo — Onboarding Screen 5: First Intent Demo
//
// Guided walkthrough of the Intent Gatekeeper with coaching text.
// Simulates the gatekeeper experience step by step.

package dev.bilbo.app.ui.screen.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// MARK: - Demo steps

private enum class DemoStep(val coaching: String, val buttonLabel: String) {
    INTRO(
        coaching = "When you open a tracked app, Bilbo pauses and asks: why are you here?",
        buttonLabel = "Try it"
    ),
    GATEKEEPER_SHOWN(
        coaching = "This is the Intent Gatekeeper. It appears before you enter any tracked app.",
        buttonLabel = "I see it"
    ),
    INTENT_ENTERED(
        coaching = "You stated your intent. Bilbo notes this and tracks if you followed through.",
        buttonLabel = "Got it"
    ),
    TIMER_SHOWN(
        coaching = "A gentle timer reminds you of your stated session length.",
        buttonLabel = "Makes sense"
    ),
    COMPLETE(
        coaching = "You're all set! Bilbo will now help you stay intentional every day.",
        buttonLabel = "Complete Setup"
    )
}

@Composable
fun FirstIntentScreen(
    onCompleteSetup: () -> Unit,
    onBack: () -> Unit
) {
    var currentStep by remember { mutableStateOf(DemoStep.INTRO) }
    val steps = DemoStep.entries
    val stepIndex = steps.indexOf(currentStep)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            steps.forEachIndexed { i, _ ->
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(
                            if (i <= stepIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Let's try it out!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Demo card
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn() togetherWith
                    slideOutHorizontally(targetOffsetX = { -it / 2 }) + fadeOut()
            },
            label = "demo_step"
        ) { step ->
            DemoStepCard(step = step)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Coaching text
        AnimatedContent(
            targetState = currentStep.coaching,
            label = "coaching"
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (currentStep == DemoStep.COMPLETE) {
                    onCompleteSetup()
                } else {
                    currentStep = steps[stepIndex + 1]
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = currentStep.buttonLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (stepIndex == 0) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onBack) {
                Text("← Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Tap an app icon…",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GatekeeperCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2240))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Why are you opening Instagram?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = "Check messages",
                onValueChange = {},
                placeholder = { Text("Type your intent…") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFB300),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
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
        initialValue = 1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse_alpha"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "09:42",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
            )
            Text("remaining of 10 min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CompleteCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚡", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "+10 Focus Points",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text("Earned for completing your intent!", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FirstIntentScreenPreview() {
    FirstIntentScreen(onCompleteSetup = {}, onBack = {})
}
