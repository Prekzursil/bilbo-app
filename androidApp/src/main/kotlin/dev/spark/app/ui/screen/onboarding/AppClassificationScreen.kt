// AppClassificationScreen.kt
// Bilbo — Onboarding Screen 4: App Classification
//
// Presents the user's top 10 installed apps and lets them classify
// each as Nutritive, Neutral, or Empty Calories.
// Pre-filled from seed data defaults.

package dev.spark.app.ui.screen.onboarding

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

// MARK: - App classification types

enum class AppClassification(val label: String) {
    NUTRITIVE("Nutritive"),
    NEUTRAL("Neutral"),
    EMPTY_CALORIES("Empty Calories")
}

// MARK: - Seed defaults — pre-fill empty-calorie apps

private val emptyCalorieDefaults = setOf(
    "com.instagram.android",
    "com.zhiliaoapp.musically",   // TikTok
    "com.google.android.youtube",
    "com.snapchat.android",
    "com.twitter.android",
    "com.reddit.frontpage",
    "com.facebook.katana",
    "com.pinterest",
    "com.vkontakte.android",
    "tv.twitch.android.app"
)

private val nutritiveDefaults = setOf(
    "com.duolingo",
    "com.audible.application",
    "com.goodreads",
    "com.anki.flashcards",
    "com.pocket",
    "org.kiwix.kiwixmobile"
)

// MARK: - App entry model

data class ClassifiableApp(
    val packageName: String,
    val displayName: String,
    var classification: AppClassification
)

// MARK: - Screen

@Composable
fun AppClassificationScreen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val apps = remember { loadTopApps(context) }
    val classificationMap = remember {
        mutableStateMapOf<String, AppClassification>().also { map ->
            apps.forEach { app -> map[app.packageName] = app.classification }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 32.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Classify Your Apps",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            text = "How should we categorize your most-used apps?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Legend
        ClassificationLegend()

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppClassificationRow(
                    app = app,
                    selectedClassification = classificationMap[app.packageName] ?: app.classification,
                    onClassificationChange = { newClass ->
                        classificationMap[app.packageName] = newClass
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "Looks Good!",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun ClassificationLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            AppClassification.NUTRITIVE to MaterialTheme.colorScheme.primary,
            AppClassification.NEUTRAL to MaterialTheme.colorScheme.secondary,
            AppClassification.EMPTY_CALORIES to MaterialTheme.colorScheme.error
        ).forEach { (cls, color) ->
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text(cls.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = color.copy(alpha = 0.12f),
                    labelColor = color
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppClassificationRow(
    app: ClassifiableApp,
    selectedClassification: AppClassification,
    onClassificationChange: (AppClassification) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = app.displayName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppClassification.entries.forEach { cls ->
                    val selected = selectedClassification == cls
                    val containerColor = when (cls) {
                        AppClassification.NUTRITIVE     -> MaterialTheme.colorScheme.primary
                        AppClassification.NEUTRAL       -> MaterialTheme.colorScheme.secondary
                        AppClassification.EMPTY_CALORIES -> MaterialTheme.colorScheme.error
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { onClassificationChange(cls) },
                        label = {
                            Text(
                                cls.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = containerColor,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }
}

// MARK: - Load top apps

private fun loadTopApps(context: Context): List<ClassifiableApp> {
    val pm = context.packageManager
    val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
        .take(10)

    return installed.map { info ->
        val name = try { pm.getApplicationLabel(info).toString() } catch (e: Exception) { info.packageName }
        val defaultClass = when (info.packageName) {
            in emptyCalorieDefaults -> AppClassification.EMPTY_CALORIES
            in nutritiveDefaults    -> AppClassification.NUTRITIVE
            else                    -> AppClassification.NEUTRAL
        }
        ClassifiableApp(
            packageName    = info.packageName,
            displayName    = name,
            classification = defaultClass
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppClassificationScreenPreview() {
    AppClassificationScreen(onNext = {}, onBack = {})
}
