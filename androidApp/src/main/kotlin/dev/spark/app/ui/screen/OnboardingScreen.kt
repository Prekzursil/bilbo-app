package dev.spark.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.spark.app.ui.theme.BilboTheme

@Composable
fun OnboardingScreen(
    onNavigateToDashboard: () -> Unit,
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Bilbo",
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your digital wellness companion.\nBuild mindful habits, one screen at a time.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onNavigateToDashboard,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Get Started")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onNavigateToDashboard,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign In")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    BilboTheme {
        OnboardingScreen(onNavigateToDashboard = {})
    }
}
