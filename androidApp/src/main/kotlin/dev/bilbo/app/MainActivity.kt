package dev.bilbo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import dev.bilbo.app.ui.BilboNavHost
import dev.bilbo.app.ui.theme.BilboTheme
import dev.bilbo.preferences.BilboPreferences
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferences: BilboPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialOnboardingCompleted = preferences.onboardingCompleted
        setContent {
            BilboTheme {
                // Track onboarding completion in-session so marking it true from the
                // onboarding flow's finish callback can re-route the navigator.
                var onboardingCompleted by remember { mutableStateOf(initialOnboardingCompleted) }
                BilboNavHost(
                    onboardingCompleted = onboardingCompleted,
                    onOnboardingFinished = {
                        preferences.onboardingCompleted = true
                        onboardingCompleted = true
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    BilboTheme {
        BilboNavHost(onboardingCompleted = false, onOnboardingFinished = {})
    }
}
