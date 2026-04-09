package dev.spark.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import dev.spark.app.ui.BilboNavHost
import dev.spark.app.ui.theme.BilboTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BilboTheme {
                BilboNavHost(onboardingCompleted = false)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    BilboTheme {
        BilboNavHost(onboardingCompleted = false)
    }
}
