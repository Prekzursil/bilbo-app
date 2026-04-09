package dev.spark.app.ui.screen

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.spark.app.ui.components.FPBalanceWidget
import dev.spark.app.ui.theme.SparkTheme
import dev.spark.domain.DopamineBudget
import dev.spark.domain.FPEconomy
import kotlinx.datetime.LocalDate

// ── Palette helpers ────────────────────────────────────────────────────────────
private val FpGreen  = Color(0xFF4CAF50)
private val FpRed    = Color(0xFFE53935)
private val FpNeutral = Color(0xFF9E9E9E)
private val FpAmber  = Color(0xFFFFC107)

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
    val weeklyBalances: List<Int> = List(7) { 0 },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── 1. FP Balance widget ─────────────────────────────────────
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    FPBalanceWidget(
                        currentBalance = uiState.currentBalance,
                        fpEarned = uiState.fpEarned,
                        size = 160.dp,
                    )
                }

                // ── 2. Today's Activity ──────────────────────────────────────
                SectionCard(title = "Today's Activity") {
                    TodayActivitySection(
                        earned = uiState.fpEarned,
                        spent = uiState.fpSpent,
                        bonus = uiState.fpBonus,
                    )
                }

                // ── 3. This Week ─────────────────────────────────────────────
                SectionCard(title = "This Week") {
                    WeeklyChart(
                        balances = uiState.weeklyBalances,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    )
                }

                // ── 4. Time Breakdown ────────────────────────────────────────
                SectionCard(title = "Time Breakdown") {
                    TimeBreakdownSection(
                        nutritiveMinutes = uiState.nutritiveMinutes,
                        neutralMinutes = uiState.neutralMinutes,
                        emptyCalorieMinutes = uiState.emptyCalorieMinutes,
                    )
                }

                // ── 5. Streak ────────────────────────────────────────────────
                SectionCard(title = "Streak") {
                    StreakSection(streakDays = uiState.streakDays)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ── Today's Activity ──────────────────────────────────────────────────────────

@Composable
private fun TodayActivitySection(earned: Int, spent: Int, bonus: Int) {
    val total = (earned + spent).coerceAtLeast(1)
    val earnedFrac by animateFloatAsState(
        targetValue = earned.toFloat() / total,
        animationSpec = tween(700),
        label = "EarnedFrac",
    )
    val spentFrac by animateFloatAsState(
        targetValue = spent.toFloat() / total,
        animationSpec = tween(700),
        label = "SpentFrac",
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Horizontal split bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(earnedFrac)
                    .height(12.dp)
                    .background(FpGreen),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(spentFrac)
                    .height(12.dp)
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
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val todayColor = FpGreen
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val data = balances.takeLast(7).let { list ->
        if (list.size < 7) List(7 - list.size) { 0 } + list else list
    }
    val maxVal = data.max().coerceAtLeast(1).toFloat()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padding = 16.dp.toPx()
        val chartH = h - padding * 2
        val stepX = if (data.size > 1) (w - padding * 2) / (data.size - 1) else w

        val points = data.mapIndexed { i, v ->
            val x = padding + i * stepX
            val y = padding + chartH * (1f - v.toFloat() / maxVal)
            Offset(x, y)
        }

        // Lines
        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // Dots
        points.forEachIndexed { i, pt ->
            val isToday = i == points.lastIndex
            drawCircle(
                color = if (isToday) todayColor else dotColor,
                radius = if (isToday) 7.dp.toPx() else 4.dp.toPx(),
                center = pt,
            )
            if (isToday) {
                drawCircle(
                    color = todayColor.copy(alpha = 0.25f),
                    radius = 12.dp.toPx(),
                    center = pt,
                )
            }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TimeBar(label = "Nutritive",     minutes = nutritiveMinutes,     color = FpGreen)
        TimeBar(label = "Neutral",       minutes = neutralMinutes,       color = FpNeutral)
        TimeBar(label = "Empty Calories",minutes = emptyCalorieMinutes,  color = FpRed)
    }
}

@Composable
private fun TimeBar(label: String, minutes: Int, color: Color) {
    val totalMin = 480f  // 8 hours max for visual scale
    val fraction by animateFloatAsState(
        targetValue = (minutes.toFloat() / totalMin).coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "TimeBar_$label",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.width(110.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.15f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .background(color),
            )
        }
        Text(
            text = "${minutes}m",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp),
        )
    }
}

// ── Streak ────────────────────────────────────────────────────────────────────

@Composable
private fun StreakSection(streakDays: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            tint = FpAmber,
            modifier = Modifier.size(32.dp),
        )
        Column {
            Text(
                text = "$streakDays day streak",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            if (streakDays >= 7) {
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 12.dp),
            )
            content()
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun BudgetDashboardPreview() {
    SparkTheme {
        BudgetDashboardScreen(
            uiState = BudgetDashboardUiState(
                currentBalance = 38,
                fpEarned = 25,
                fpSpent = 10,
                fpBonus = 5,
                nutritiveMinutes = 40,
                neutralMinutes = 60,
                emptyCalorieMinutes = 30,
                streakDays = 9,
                weeklyBalances = listOf(20, 35, 28, 42, 15, 50, 38),
            ),
            onRefresh = {},
        )
    }
}
