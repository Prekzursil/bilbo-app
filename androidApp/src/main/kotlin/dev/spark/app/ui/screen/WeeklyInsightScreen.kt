package dev.spark.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.spark.domain.HeuristicInsight
import dev.spark.domain.InsightType
import dev.spark.domain.WeeklyInsight
import kotlinx.datetime.LocalDate

// ── Sample / preview data helpers ────────────────────────────────────────────

private val PreviewInsight = WeeklyInsight(
    weekStart = LocalDate(2026, 4, 7),
    tier2Insights = listOf(
        dev.spark.domain.HeuristicInsight(
            type = InsightType.ACHIEVEMENT,
            message = "You stayed under your daily average for 5 days in a row this week.",
            confidence = 0.92f,
        ),
        dev.spark.domain.HeuristicInsight(
            type = InsightType.CORRELATION,
            message = "When you feel Stressed, there's a strong correlation with using scrolling apps more.",
            confidence = 0.78f,
        ),
        dev.spark.domain.HeuristicInsight(
            type = InsightType.TREND,
            message = "Mondays tend to be your highest-usage day. Consider scheduling a focus block.",
            confidence = 0.65f,
        ),
    ),
    tier3Narrative = null,
    totalScreenTimeMinutes = 312,
    nutritiveMinutes = 87,
    emptyCalorieMinutes = 148,
    fpEarned = 105,
    fpSpent = 60,
    intentAccuracyPercent = 0.78f,
    streakDays = 5,
)

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * Full Compose screen for the weekly insight report.
 *
 * @param insight          The [WeeklyInsight] to display. Pass null to show a skeleton loader.
 * @param dailyMinutes     7-element list (Mon→Sun) of screen time in minutes — used for the chart.
 * @param isCloudInsight   True if the narrative came from the cloud AI service.
 * @param isRefreshing     True while a pull-to-refresh operation is in flight.
 * @param canRefresh       False if the weekly rate limit has been hit (cloud calls capped to 1/week).
 * @param onRefresh        Called when the user pulls to refresh.
 * @param onBack           Navigation callback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyInsightScreen(
    insight: WeeklyInsight?,
    dailyMinutes: List<Int> = listOf(45, 62, 38, 71, 28, 35, 33),
    isCloudInsight: Boolean = false,
    isRefreshing: Boolean = false,
    canRefresh: Boolean = true,
    onRefresh: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val pullState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Weekly Insight",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = pullState,
            isRefreshing = isRefreshing,
            onRefresh = {
                if (canRefresh) onRefresh()
            },
        ) {
            if (insight == null) {
                WeeklyInsightSkeleton()
            } else {
                WeeklyInsightContent(
                    insight = insight,
                    dailyMinutes = dailyMinutes,
                    isCloudInsight = isCloudInsight,
                    canRefresh = canRefresh,
                )
            }
        }
    }
}

// ── Main content ──────────────────────────────────────────────────────────────

@Composable
private fun WeeklyInsightContent(
    insight: WeeklyInsight,
    dailyMinutes: List<Int>,
    isCloudInsight: Boolean,
    canRefresh: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero narrative card
        item {
            NarrativeHeroCard(
                narrative = insight.tier3Narrative ?: buildFallbackNarrative(insight),
                isCloudInsight = isCloudInsight,
            )
        }

        // Stats cards row
        item {
            StatsRow(insight = insight)
        }

        // 7-day trend chart
        item {
            TrendChartCard(dailyMinutes = dailyMinutes)
        }

        // Correlation insight cards (top 3, sorted by confidence)
        val topInsights = insight.tier2Insights
            .sortedByDescending { it.confidence }
            .take(3)

        if (topInsights.isNotEmpty()) {
            item {
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(topInsights) { heuristic ->
                InsightCard(insight = heuristic)
            }
        }

        // Streak card
        item {
            StreakCard(streakDays = insight.streakDays)
        }

        // Rate limit info if can't refresh
        if (!canRefresh) {
            item {
                RateLimitBanner()
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ── Hero narrative card ───────────────────────────────────────────────────────

@Composable
private fun NarrativeHeroCard(
    narrative: String,
    isCloudInsight: Boolean,
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCloudInsight)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            if (isCloudInsight) {
                // AI badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(text = "✨", fontSize = 12.sp)
                            Text(
                                text = "AI Insight",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = narrative,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = FontStyle.Italic,
                        lineHeight = 26.sp,
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = narrative,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Stats row ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(insight: WeeklyInsight) {
    val hours = insight.totalScreenTimeMinutes / 60
    val mins = insight.totalScreenTimeMinutes % 60
    val timeLabel = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

    val nutritivePct = if (insight.totalScreenTimeMinutes > 0)
        (insight.nutritiveMinutes * 100 / insight.totalScreenTimeMinutes) else 0
    val emptyPct = if (insight.totalScreenTimeMinutes > 0)
        (insight.emptyCalorieMinutes * 100 / insight.totalScreenTimeMinutes) else 0

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StatCard(
                label = "Screen Time",
                value = timeLabel,
                icon = Icons.Outlined.Timer,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        item {
            StatCard(
                label = "Nutritive",
                value = "$nutritivePct%",
                icon = Icons.Outlined.LocalFlorist,
                tint = Color(0xFF4CAF50),
            )
        }
        item {
            StatCard(
                label = "Empty Cal.",
                value = "$emptyPct%",
                icon = Icons.Outlined.PhoneAndroid,
                tint = Color(0xFFF44336),
            )
        }
        item {
            StatCard(
                label = "FP Balance",
                value = "+${insight.fpEarned - insight.fpSpent}",
                icon = Icons.Outlined.Star,
                tint = Color(0xFFFF9800),
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
) {
    Card(
        modifier = Modifier.width(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── 7-day trend chart ─────────────────────────────────────────────────────────

@Composable
private fun TrendChartCard(dailyMinutes: List<Int>) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val values = dailyMinutes.take(7).let { list ->
        // Pad to 7 if needed
        list + List(maxOf(0, 7 - list.size)) { 0 }
    }
    val maxVal = values.max().coerceAtLeast(1).toFloat()
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "7-Day Screen Time",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))

            // Canvas line chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            ) {
                val width = size.width
                val height = size.height
                val stepX = width / (values.size - 1).coerceAtLeast(1).toFloat()
                val padding = 8.dp.toPx()

                val points = values.mapIndexed { i, v ->
                    Offset(
                        x = i * stepX,
                        y = height - padding - (v / maxVal) * (height - padding * 2),
                    )
                }

                // Gradient fill under the line
                val gradientPath = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points.first().x, height)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, height)
                        close()
                    }
                }
                drawPath(
                    path = gradientPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primary.copy(alpha = 0.25f), Color.Transparent),
                        startY = 0f,
                        endY = height,
                    ),
                )

                // Line
                val linePath = Path().apply {
                    points.forEachIndexed { i, pt ->
                        if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = primary,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )

                // Dots
                points.forEach { pt ->
                    drawCircle(color = primary, radius = 5.dp.toPx(), center = pt)
                    drawCircle(
                        color = onSurface.copy(alpha = 0.05f),
                        radius = 5.dp.toPx(),
                        center = pt,
                    )
                }
            }

            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                days.forEach { d ->
                    Text(
                        text = d,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ── Insight card ─────────────────────────────────────────────────────────────

@Composable
private fun InsightCard(insight: HeuristicInsight) {
    val (icon, tint) = when (insight.type) {
        InsightType.CORRELATION -> Pair(Icons.Outlined.TrendingUp, Color(0xFF2196F3))
        InsightType.TREND       -> Pair(Icons.Outlined.ShowChart, Color(0xFFFF9800))
        InsightType.ANOMALY     -> Pair(Icons.Outlined.Warning, Color(0xFFF44336))
        InsightType.ACHIEVEMENT -> Pair(Icons.Outlined.EmojiEvents, Color(0xFF4CAF50))
    }

    val confidencePct = (insight.confidence * 100).toInt()
    val confidenceLabel = when {
        insight.confidence >= 0.8f -> "High"
        insight.confidence >= 0.6f -> "Medium"
        else -> "Low"
    }
    val confidenceColor = when {
        insight.confidence >= 0.8f -> Color(0xFF4CAF50)
        insight.confidence >= 0.6f -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = confidenceColor.copy(alpha = 0.12f),
                    modifier = Modifier.wrapContentSize(),
                ) {
                    Text(
                        text = "$confidenceLabel confidence",
                        style = MaterialTheme.typography.labelSmall,
                        color = confidenceColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

// ── Streak card ───────────────────────────────────────────────────────────────

@Composable
private fun StreakCard(streakDays: Int) {
    // Show last 4 weeks (28 days) as dots, with the current streak highlighted
    val totalDots = 28
    val activeDots = streakDays.coerceIn(0, totalDots)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "🔥", fontSize = 20.sp)
                Text(
                    text = "$streakDays-day streak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            // 4-week dot grid (7 columns × 4 rows)
            val primary = MaterialTheme.colorScheme.primary
            val outline = MaterialTheme.colorScheme.outline

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (0 until 4).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0 until 7).forEach { day ->
                            val dotIndex = week * 7 + day
                            val isActive = dotIndex >= (totalDots - activeDots)
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) primary else outline.copy(alpha = 0.3f),
                                    ),
                            )
                        }
                    }
                }
            }

            Text(
                text = "4-week history",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Rate limit banner ─────────────────────────────────────────────────────────

@Composable
private fun RateLimitBanner() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "AI insights refresh once per week. Check back next Sunday.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

// ── Skeleton loader ───────────────────────────────────────────────────────────

@Composable
private fun WeeklyInsightSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (it == 0) 140.dp else 80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun buildFallbackNarrative(insight: WeeklyInsight): String {
    val hours = insight.totalScreenTimeMinutes / 60
    val mins = insight.totalScreenTimeMinutes % 60
    val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    return "This week you spent $timeStr on your phone. " +
        if (insight.streakDays >= 3) "You kept a ${insight.streakDays}-day streak — great work!"
        else "Keep building your streak — every day counts."
}
