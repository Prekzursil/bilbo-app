package dev.bilbo.app.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bilbo.app.ui.components.FPBalanceWidget
import dev.bilbo.domain.FPEconomy

// ── Palette helpers ────────────────────────────────────────────────────────────
private const val ARGB_GREEN = 0xFF4CAF50
private const val ARGB_RED = 0xFFE53935
private const val ARGB_NEUTRAL = 0xFF9E9E9E
private const val ARGB_AMBER = 0xFFFFC107

private val FpGreen = Color(ARGB_GREEN)
private val FpRed = Color(ARGB_RED)
private val FpNeutral = Color(ARGB_NEUTRAL)
private val FpAmber = Color(ARGB_AMBER)

private const val WEEK_DAYS = 7
private const val BAR_ANIM_MS = 700
private const val FP_WIDGET_SIZE_DP = 160
private const val WEEKLY_CHART_HEIGHT_DP = 120
private const val SCREEN_PAD_H_DP = 16
private const val SCREEN_PAD_V_DP = 8
private const val SECTION_GAP_DP = 16
private const val SMALL_GAP_DP = 8
private const val SPLIT_BAR_HEIGHT_DP = 12
private const val SPLIT_BAR_RADIUS_DP = 6
private const val ALPHA_LINE = 0.4f
private const val ALPHA_TODAY_GLOW = 0.25f
private const val ALPHA_BAR_TRACK = 0.15f
private const val CHART_PAD_DP = 16
private const val CHART_LINE_DP = 2
private const val DOT_RADIUS_DP = 4
private const val DOT_TODAY_RADIUS_DP = 7
private const val DOT_GLOW_RADIUS_DP = 12
private const val TIME_BAR_MAX_MIN = 480f
private const val TIME_LABEL_WIDTH_DP = 110
private const val TIME_VALUE_WIDTH_DP = 36
private const val TIME_BAR_HEIGHT_DP = 8
private const val TIME_BAR_RADIUS_DP = 4
private const val STREAK_ICON_DP = 32
private const val STREAK_BONUS_THRESHOLD = 7
private const val CARD_CORNER_DP = 16
private const val CARD_ELEVATION_DP = 2
private const val CARD_PAD_DP = 16
private const val TITLE_BOTTOM_PAD_DP = 12

// ── Data representation fed into the screen ────────────────────────────────────

/**
 * All UI state needed by [BudgetDashboardScreen].
 * In production this is produced by a ViewModel collecting from [BudgetRepository].
 */
data class BudgetDashboardUiState(
    val currentBalance: Int = 0,
    val fpEarned: Int = 0,
    val fpSpent: Int = 0,
    val fpBonus: Int = 0,
    val nutritiveMinutes: Int = 0,
    val neutralMinutes: Int = 0,
    val emptyCalorieMinutes: Int = 0,
    val streakDays: Int = 0,
    /** Daily balances for the last 7 days, index 0 = oldest, 6 = today. */
    val weeklyBalances: List<Int> = List(WEEK_DAYS) { 0 },
    val isRefreshing: Boolean = false,
)

// ── Screen ─────────────────────────────────────────────────────────────────────

/**
 * Full dopamine economy dashboard.
 *
 * @param uiState   All display data.
 * @param onRefresh Called when the user pull-to-refreshes.
 * @param onBack    Optional back navigation callback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDashboardScreen(
    uiState: BudgetDashboardUiState,
    onRefresh: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val pullState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Economy") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            state = pullState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            BudgetDashboardContent(uiState = uiState)
        }
    }
}

@Composable
private fun BudgetDashboardContent(uiState: BudgetDashboardUiState) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SCREEN_PAD_H_DP.dp, vertical = SCREEN_PAD_V_DP.dp),
        verticalArrangement = Arrangement.spacedBy(SECTION_GAP_DP.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FPBalanceWidget(
                currentBalance = uiState.currentBalance,
                fpEarned = uiState.fpEarned,
                size = FP_WIDGET_SIZE_DP.dp,
            )
        }

        SectionCard(title = "Today's Activity") {
            TodayActivitySection(
                earned = uiState.fpEarned,
                spent = uiState.fpSpent,
                bonus = uiState.fpBonus,
            )
        }

        SectionCard(title = "This Week") {
            WeeklyChart(
                balances = uiState.weeklyBalances,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(WEEKLY_CHART_HEIGHT_DP.dp),
            )
        }

        SectionCard(title = "Time Breakdown") {
            TimeBreakdownSection(
                nutritiveMinutes = uiState.nutritiveMinutes,
                neutralMinutes = uiState.neutralMinutes,
                emptyCalorieMinutes = uiState.emptyCalorieMinutes,
            )
        }

        SectionCard(title = "Streak") {
            StreakSection(streakDays = uiState.streakDays)
        }

        Spacer(modifier = Modifier.height(SECTION_GAP_DP.dp))
    }
}

// ── Today's Activity ──────────────────────────────────────────────────────────

@Composable
private fun TodayActivitySection(
    earned: Int,
    spent: Int,
    bonus: Int,
) {
    val total = (earned + spent).coerceAtLeast(1)
    val earnedFrac by animateFloatAsState(
        targetValue = earned.toFloat() / total,
        animationSpec = tween(BAR_ANIM_MS),
        label = "EarnedFrac",
    )
    val spentFrac by animateFloatAsState(
        targetValue = spent.toFloat() / total,
        animationSpec = tween(BAR_ANIM_MS),
        label = "SpentFrac",
    )

    Column(verticalArrangement = Arrangement.spacedBy(SMALL_GAP_DP.dp)) {
        // Horizontal split bar
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(SPLIT_BAR_HEIGHT_DP.dp)
                    .clip(RoundedCornerShape(SPLIT_BAR_RADIUS_DP.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(earnedFrac)
                        .height(SPLIT_BAR_HEIGHT_DP.dp)
                        .background(FpGreen),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(spentFrac)
                        .height(SPLIT_BAR_HEIGHT_DP.dp)
                        .background(FpRed),
            )
        }

        Text(
            text = "+$earned earned  |  -$spent spent  |  +$bonus bonus",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Weekly Chart ──────────────────────────────────────────────────────────────

@Composable
private fun WeeklyChart(
    balances: List<Int>,
    modifier: Modifier = Modifier,
) {
    val dotColor = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = ALPHA_LINE)
    val todayColor = FpGreen

    val data =
        balances.takeLast(WEEK_DAYS).let { list ->
            if (list.size < WEEK_DAYS) List(WEEK_DAYS - list.size) { 0 } + list else list
        }
    val maxVal = data.max().coerceAtLeast(1).toFloat()

    Canvas(modifier = modifier) {
        val padding = CHART_PAD_DP.dp.toPx()
        val chartH = size.height - padding * 2
        val stepX = if (data.size > 1) (size.width - padding * 2) / (data.size - 1) else size.width

        val points =
            data.mapIndexed { i, v ->
                val x = padding + i * stepX
                val y = padding + chartH * (1f - v.toFloat() / maxVal)
                Offset(x, y)
            }

        drawChartLines(points = points, lineColor = lineColor)
        drawChartDots(points = points, dotColor = dotColor, todayColor = todayColor)
    }
}

private fun DrawScope.drawChartLines(
    points: List<Offset>,
    lineColor: Color,
) {
    for (i in 0 until points.size - 1) {
        drawLine(
            color = lineColor,
            start = points[i],
            end = points[i + 1],
            strokeWidth = CHART_LINE_DP.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawChartDots(
    points: List<Offset>,
    dotColor: Color,
    todayColor: Color,
) {
    points.forEachIndexed { i, pt ->
        val isToday = i == points.lastIndex
        drawCircle(
            color = if (isToday) todayColor else dotColor,
            radius = if (isToday) DOT_TODAY_RADIUS_DP.dp.toPx() else DOT_RADIUS_DP.dp.toPx(),
            center = pt,
        )
        if (isToday) {
            drawCircle(
                color = todayColor.copy(alpha = ALPHA_TODAY_GLOW),
                radius = DOT_GLOW_RADIUS_DP.dp.toPx(),
                center = pt,
            )
        }
    }
}

// ── Time Breakdown ────────────────────────────────────────────────────────────

@Composable
private fun TimeBreakdownSection(
    nutritiveMinutes: Int,
    neutralMinutes: Int,
    emptyCalorieMinutes: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SMALL_GAP_DP.dp)) {
        TimeBar(label = "Nutritive", minutes = nutritiveMinutes, color = FpGreen)
        TimeBar(label = "Neutral", minutes = neutralMinutes, color = FpNeutral)
        TimeBar(label = "Empty Calories", minutes = emptyCalorieMinutes, color = FpRed)
    }
}

@Composable
private fun TimeBar(
    label: String,
    minutes: Int,
    color: Color,
) {
    val fraction by animateFloatAsState(
        targetValue = (minutes.toFloat() / TIME_BAR_MAX_MIN).coerceIn(0f, 1f),
        animationSpec = tween(BAR_ANIM_MS),
        label = "TimeBar_$label",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SMALL_GAP_DP.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.width(TIME_LABEL_WIDTH_DP.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(TIME_BAR_HEIGHT_DP.dp)
                    .clip(RoundedCornerShape(TIME_BAR_RADIUS_DP.dp))
                    .background(color.copy(alpha = ALPHA_BAR_TRACK)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(TIME_BAR_HEIGHT_DP.dp)
                        .background(color),
            )
        }
        Text(
            text = "${minutes}m",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(TIME_VALUE_WIDTH_DP.dp),
        )
    }
}

// ── Streak ────────────────────────────────────────────────────────────────────

@Composable
private fun StreakSection(streakDays: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SMALL_GAP_DP.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            tint = FpAmber,
            modifier = Modifier.size(STREAK_ICON_DP.dp),
        )
        Column {
            Text(
                text = "$streakDays day streak",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            if (streakDays >= STREAK_BONUS_THRESHOLD) {
                Text(
                    text = "7-day bonus: +${FPEconomy.STREAK_BONUS_7_DAY} FP",
                    style = MaterialTheme.typography.bodySmall,
                    color = FpAmber,
                )
            }
        }
    }
}

// ── Generic section card ──────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION_DP.dp),
    ) {
        Column(modifier = Modifier.padding(CARD_PAD_DP.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = TITLE_BOTTOM_PAD_DP.dp),
            )
            content()
        }
    }
}
