package dev.bilbo.app.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bilbo.domain.HeuristicInsight
import dev.bilbo.domain.InsightType
import dev.bilbo.domain.WeeklyInsight

// ── Dimensions & layout constants ─────────────────────────────────────────────

private const val MINUTES_PER_HOUR = 60
private const val PERCENT_SCALE = 100
private const val TOP_INSIGHTS_COUNT = 3
private const val MIN_STREAK_FOR_MENTION = 3
private const val DAYS_IN_WEEK = 7
private const val STREAK_HISTORY_WEEKS = 4
private const val STREAK_HISTORY_DOTS = 28
private const val SKELETON_CARD_COUNT = 4

private const val CONF_HIGH = 0.8f
private const val CONF_MEDIUM = 0.6f
private const val ALPHA_BADGE = 0.15f
private const val ALPHA_ICON_BG = 0.12f
private const val ALPHA_GRADIENT = 0.25f
private const val ALPHA_DOT_OUTLINE = 0.3f
private const val ALPHA_DOT_HALO = 0.05f

private const val RADIUS_HERO_DP = 20
private const val RADIUS_LARGE_DP = 16
private const val RADIUS_CARD_DP = 14
private const val RADIUS_MEDIUM_DP = 12
private const val RADIUS_SMALL_DP = 6
private const val ELEVATION_HERO_DP = 4
private const val ELEVATION_CARD_DP = 2

private const val SPACE_SMALL_DP = 4
private const val SPACE_SMALL_PLUS_DP = 6
private const val SPACE_MEDIUM_DP = 8
private const val SPACE_MEDIUM_PLUS_DP = 10
private const val SPACE_LARGE_DP = 12
private const val SPACE_XL_DP = 14
private const val SPACE_XXL_DP = 16
private const val SPACE_HERO_DP = 20
private const val SPACE_SECTION_DP = 24

private const val ICON_SIZE_SMALL_DP = 18
private const val ICON_SIZE_DP = 20
private const val ICON_SIZE_LARGE_DP = 24
private const val AVATAR_SIZE_DP = 40
private const val STAT_CARD_WIDTH_DP = 110
private const val CHART_HEIGHT_DP = 120
private const val DOT_SIZE_DP = 12
private const val SKELETON_HERO_HEIGHT_DP = 140
private const val SKELETON_ROW_HEIGHT_DP = 80

private const val FONT_BADGE_SP = 12
private const val FONT_STREAK_EMOJI_SP = 20
private const val LINE_HEIGHT_SP = 26

private const val STROKE_LINE_DP = 3
private const val DOT_RADIUS_DP = 5

private const val ARGB_GREEN = 0xFF4CAF50
private const val ARGB_RED = 0xFFF44336
private const val ARGB_ORANGE = 0xFFFF9800
private const val ARGB_BLUE = 0xFF2196F3

// Placeholder daily-minutes series (Mon→Sun) shown until real data is wired in.
private const val SAMPLE_MON = 45
private const val SAMPLE_TUE = 62
private const val SAMPLE_WED = 38
private const val SAMPLE_THU = 71
private const val SAMPLE_FRI = 28
private const val SAMPLE_SAT = 35
private const val SAMPLE_SUN = 33
private val SAMPLE_DAILY_MINUTES =
    listOf(SAMPLE_MON, SAMPLE_TUE, SAMPLE_WED, SAMPLE_THU, SAMPLE_FRI, SAMPLE_SAT, SAMPLE_SUN)

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
    dailyMinutes: List<Int> = SAMPLE_DAILY_MINUTES,
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
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            modifier =
                Modifier
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
        modifier = Modifier.fillMaxSize().testTag("weekly_insight_list"),
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
        val topInsights =
            insight.tier2Insights
                .sortedByDescending { it.confidence }
                .take(TOP_INSIGHTS_COUNT)

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

        item { Spacer(Modifier.height(SPACE_SECTION_DP.dp)) }
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
        shape = RoundedCornerShape(RADIUS_HERO_DP.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isCloudInsight) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION_HERO_DP.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(SPACE_HERO_DP.dp),
        ) {
            if (isCloudInsight) {
                CloudNarrativeContent(narrative)
            } else {
                HeuristicNarrativeContent(narrative)
            }
        }
    }
}

@Composable
private fun CloudNarrativeContent(narrative: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SPACE_SMALL_PLUS_DP.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(RADIUS_MEDIUM_DP.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = ALPHA_BADGE),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = SPACE_MEDIUM_DP.dp, vertical = SPACE_SMALL_DP.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SPACE_SMALL_DP.dp),
            ) {
                Text(text = "✨", fontSize = FONT_BADGE_SP.sp)
                Text(
                    text = "AI Insight",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
    Spacer(Modifier.height(SPACE_LARGE_DP.dp))
    Text(
        text = narrative,
        style =
            MaterialTheme.typography.bodyLarge.copy(
                fontStyle = FontStyle.Italic,
                lineHeight = LINE_HEIGHT_SP.sp,
            ),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

@Composable
private fun HeuristicNarrativeContent(narrative: String) {
    Icon(
        imageVector = Icons.Outlined.Lightbulb,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(ICON_SIZE_LARGE_DP.dp),
    )
    Spacer(Modifier.height(SPACE_MEDIUM_PLUS_DP.dp))
    Text(
        text = narrative,
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = LINE_HEIGHT_SP.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ── Stats row ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(insight: WeeklyInsight) {
    val hours = insight.totalScreenTimeMinutes / MINUTES_PER_HOUR
    val mins = insight.totalScreenTimeMinutes % MINUTES_PER_HOUR
    val timeLabel = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

    val nutritivePct =
        if (insight.totalScreenTimeMinutes > 0) {
            (insight.nutritiveMinutes * PERCENT_SCALE / insight.totalScreenTimeMinutes)
        } else {
            0
        }
    val emptyPct =
        if (insight.totalScreenTimeMinutes > 0) {
            (insight.emptyCalorieMinutes * PERCENT_SCALE / insight.totalScreenTimeMinutes)
        } else {
            0
        }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(SPACE_LARGE_DP.dp),
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
                tint = Color(ARGB_GREEN),
            )
        }
        item {
            StatCard(
                label = "Empty Cal.",
                value = "$emptyPct%",
                icon = Icons.Outlined.PhoneAndroid,
                tint = Color(ARGB_RED),
            )
        }
        item {
            StatCard(
                label = "FP Balance",
                value = "+${insight.fpEarned - insight.fpSpent}",
                icon = Icons.Outlined.Star,
                tint = Color(ARGB_ORANGE),
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
        modifier = Modifier.width(STAT_CARD_WIDTH_DP.dp),
        shape = RoundedCornerShape(RADIUS_LARGE_DP.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier = Modifier.padding(SPACE_XL_DP.dp),
            verticalArrangement = Arrangement.spacedBy(SPACE_SMALL_PLUS_DP.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(ICON_SIZE_DP.dp),
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
    val values =
        dailyMinutes.take(DAYS_IN_WEEK).let { list ->
            // Pad to 7 if needed
            list + List(maxOf(0, DAYS_IN_WEEK - list.size)) { 0 }
        }
    val maxVal = values.max().coerceAtLeast(1).toFloat()
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RADIUS_LARGE_DP.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(modifier = Modifier.padding(SPACE_XXL_DP.dp)) {
            Text(
                text = "7-Day Screen Time",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(SPACE_LARGE_DP.dp))

            TrendLineChart(values = values, maxVal = maxVal, primary = primary, onSurface = onSurface)

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

@Composable
private fun TrendLineChart(
    values: List<Int>,
    maxVal: Float,
    primary: Color,
    onSurface: Color,
) {
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT_DP.dp),
    ) {
        val height = size.height
        val stepX = size.width / (values.size - 1).coerceAtLeast(1).toFloat()
        val padding = SPACE_MEDIUM_DP.dp.toPx()

        val points =
            values.mapIndexed { i, v ->
                Offset(
                    x = i * stepX,
                    y = height - padding - (v / maxVal) * (height - padding * 2),
                )
            }

        // Gradient fill under the line
        val gradientPath =
            Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points.first().x, height)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, height)
                    close()
                }
            }
        drawPath(
            path = gradientPath,
            brush =
                Brush.verticalGradient(
                    colors = listOf(primary.copy(alpha = ALPHA_GRADIENT), Color.Transparent),
                    startY = 0f,
                    endY = height,
                ),
        )

        // Line
        val linePath =
            Path().apply {
                points.forEachIndexed { i, pt ->
                    if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
            }
        drawPath(
            path = linePath,
            color = primary,
            style = Stroke(width = STROKE_LINE_DP.dp.toPx(), cap = StrokeCap.Round),
        )

        // Dots
        points.forEach { pt ->
            drawCircle(color = primary, radius = DOT_RADIUS_DP.dp.toPx(), center = pt)
            drawCircle(
                color = onSurface.copy(alpha = ALPHA_DOT_HALO),
                radius = DOT_RADIUS_DP.dp.toPx(),
                center = pt,
            )
        }
    }
}

// ── Insight card ─────────────────────────────────────────────────────────────

private fun iconAndTintFor(type: InsightType): Pair<ImageVector, Color> =
    when (type) {
        InsightType.CORRELATION -> Pair(Icons.Outlined.TrendingUp, Color(ARGB_BLUE))
        InsightType.TREND -> Pair(Icons.Outlined.ShowChart, Color(ARGB_ORANGE))
        InsightType.ANOMALY -> Pair(Icons.Outlined.Warning, Color(ARGB_RED))
        InsightType.ACHIEVEMENT -> Pair(Icons.Outlined.EmojiEvents, Color(ARGB_GREEN))
    }

private fun confidenceLabelFor(confidence: Float): String =
    when {
        confidence >= CONF_HIGH -> "High"
        confidence >= CONF_MEDIUM -> "Medium"
        else -> "Low"
    }

@Composable
private fun InsightIconAvatar(
    icon: ImageVector,
    tint: Color,
) {
    Surface(
        shape = CircleShape,
        color = tint.copy(alpha = ALPHA_ICON_BG),
        modifier = Modifier.size(AVATAR_SIZE_DP.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(ICON_SIZE_DP.dp),
            )
        }
    }
}

@Composable
private fun InsightCard(insight: HeuristicInsight) {
    val (icon, tint) = iconAndTintFor(insight.type)
    val confidenceLabel = confidenceLabelFor(insight.confidence)
    val confidenceColor =
        when {
            insight.confidence >= CONF_HIGH -> Color(ARGB_GREEN)
            insight.confidence >= CONF_MEDIUM -> Color(ARGB_ORANGE)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RADIUS_CARD_DP.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION_CARD_DP.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(SPACE_XL_DP.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(SPACE_LARGE_DP.dp),
        ) {
            InsightIconAvatar(icon = icon, tint = tint)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SPACE_SMALL_PLUS_DP.dp)) {
                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = RoundedCornerShape(RADIUS_SMALL_DP.dp),
                    color = confidenceColor.copy(alpha = ALPHA_ICON_BG),
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
    val totalDots = STREAK_HISTORY_DOTS
    val activeDots = streakDays.coerceIn(0, totalDots)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RADIUS_LARGE_DP.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier = Modifier.padding(SPACE_XXL_DP.dp),
            verticalArrangement = Arrangement.spacedBy(SPACE_LARGE_DP.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SPACE_MEDIUM_DP.dp),
            ) {
                Text(text = "🔥", fontSize = FONT_STREAK_EMOJI_SP.sp)
                Text(
                    text = "$streakDays-day streak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            // 4-week dot grid (7 columns × 4 rows)
            val primary = MaterialTheme.colorScheme.primary
            val outline = MaterialTheme.colorScheme.outline

            Column(verticalArrangement = Arrangement.spacedBy(SPACE_SMALL_PLUS_DP.dp)) {
                repeat(STREAK_HISTORY_WEEKS) { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(SPACE_SMALL_PLUS_DP.dp)) {
                        repeat(DAYS_IN_WEEK) { day ->
                            val dotIndex = week * DAYS_IN_WEEK + day
                            val isActive = dotIndex >= (totalDots - activeDots)
                            Box(
                                modifier =
                                    Modifier
                                        .size(DOT_SIZE_DP.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isActive) primary else outline.copy(alpha = ALPHA_DOT_OUTLINE),
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
        shape = RoundedCornerShape(RADIUS_MEDIUM_DP.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(SPACE_LARGE_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SPACE_MEDIUM_PLUS_DP.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(ICON_SIZE_SMALL_DP.dp),
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
        modifier =
            Modifier
                .fillMaxSize()
                .padding(SPACE_XXL_DP.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(SKELETON_CARD_COUNT) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(if (it == 0) SKELETON_HERO_HEIGHT_DP.dp else SKELETON_ROW_HEIGHT_DP.dp)
                        .clip(RoundedCornerShape(RADIUS_LARGE_DP.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun buildFallbackNarrative(insight: WeeklyInsight): String {
    val hours = insight.totalScreenTimeMinutes / MINUTES_PER_HOUR
    val mins = insight.totalScreenTimeMinutes % MINUTES_PER_HOUR
    val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    return "This week you spent $timeStr on your phone. " +
        if (insight.streakDays >= MIN_STREAK_FOR_MENTION) {
            "You kept a ${insight.streakDays}-day streak — great work!"
        } else {
            "Keep building your streak — every day counts."
        }
}
