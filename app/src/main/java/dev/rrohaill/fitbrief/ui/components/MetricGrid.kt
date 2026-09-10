package dev.rrohaill.fitbrief.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.ui.MetricType
import java.text.DecimalFormat
import kotlin.math.roundToInt

private enum class MetricGraph {
    StepsProgress,
    HeartRateBars,
    None
}

private data class Metric(
    val type: MetricType,
    val title: String,
    val value: String,
    val unit: String,
    val icon: String,
    val tint: Color,
    val footer: String,
    val graph: MetricGraph = MetricGraph.None,
    val progress: Float? = null
)

@Composable
internal fun MetricGrid(
    snapshot: HealthSnapshot?,
    showGraphs: Boolean = true,
    onMetricClick: (MetricType) -> Unit = {}
) {
    val decimal = DecimalFormat("#,##0.#")
    val rangeDays = when (snapshot?.range?.option) {
        RangeOption.SevenDays -> 7
        RangeOption.ThirtyDays -> 30
        else -> 1
    }
    val stepGoal = rangeDays * 10_000L
    val stepProgress = snapshot?.steps?.toFloat()?.div(stepGoal)?.coerceIn(0f, 1f)
    val stepGoalPercent =
        snapshot?.steps?.let { (it.toFloat() / stepGoal * 100f).roundToInt() } ?: 0
    val metrics = buildList {
        snapshot?.let { data ->
            if (data.steps > 0) {
                add(
                    Metric(
                        MetricType.Steps,
                        "Steps",
                        decimal.format(data.steps),
                        "steps",
                        "♧",
                        Color(0xFF167565),
                        "$stepGoalPercent% of ${decimal.format(stepGoal)} goal",
                        MetricGraph.StepsProgress,
                        stepProgress
                    )
                )
            }
            data.averageHeartRateBpm?.let {
                add(
                    Metric(
                        MetricType.HeartRate,
                        "Heart Rate",
                        it.toString(),
                        "bpm avg",
                        "♡",
                        Color(0xFFE9656D),
                        "Selected range average",
                        MetricGraph.HeartRateBars
                    )
                )
            }
            if (data.sleepMinutes > 0) {
                add(
                    Metric(
                        MetricType.Sleep,
                        "Sleep",
                        "${data.sleepMinutes / 60}h ${data.sleepMinutes % 60}m",
                        "sleep duration",
                        "☾",
                        Color(0xFF6576E8),
                        "Selected range"
                    )
                )
            }
            if (data.activeCaloriesKcal > 0) {
                add(
                    Metric(
                        MetricType.ActiveCalories,
                        "Active Calories",
                        decimal.format(data.activeCaloriesKcal),
                        "kcal",
                        "♨",
                        Color(0xFFEFA92E),
                        "Selected range"
                    )
                )
            }
            if (data.distanceMeters > 0) {
                add(
                    Metric(
                        MetricType.Distance,
                        "Distance",
                        "${decimal.format(data.distanceKilometers)} km",
                        "distance",
                        "↗",
                        Color(0xFF4E9BE8),
                        "Selected range"
                    )
                )
            }
            if (data.exerciseMinutes > 0) {
                add(
                    Metric(
                        MetricType.Exercise,
                        "Exercise",
                        "${data.exerciseMinutes} min",
                        "exercise time",
                        "✦",
                        Color(0xFFB276E8),
                        "Selected range"
                    )
                )
            }
            if (data.totalCaloriesKcal > 0) {
                add(
                    Metric(
                        MetricType.TotalCalories,
                        "Total Calories",
                        decimal.format(data.totalCaloriesKcal),
                        "kcal",
                        "♨",
                        Color(0xFFE58B45),
                        "Selected range"
                    )
                )
            }
        }
    }
    val displayMetrics = if (showGraphs) {
        metrics
    } else {
        metrics.map { it.copy(graph = MetricGraph.None, progress = null) }
    }
    if (displayMetrics.isEmpty()) {
        Text(
            "No health data is available for this range.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            displayMetrics.chunked(2).forEach { rowMetrics ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowMetrics.forEach { metric ->
                        MetricCard(metric, Modifier.weight(1f), onMetricClick)
                    }
                    if (rowMetrics.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    metric: Metric,
    modifier: Modifier = Modifier,
    onClick: (MetricType) -> Unit = {}
) {
    Card(
        modifier = modifier.clickable { onClick(metric.type) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .height(124.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    metric.title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Badge(metric.icon, metric.tint.copy(alpha = .12f), metric.tint, 28.dp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                metric.value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                metric.unit,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            when (metric.graph) {
                MetricGraph.StepsProgress -> LinearProgressIndicator(
                    progress = { metric.progress ?: 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = metric.tint,
                    trackColor = MaterialTheme.colorScheme.background
                )

                MetricGraph.HeartRateBars -> HeartRateBars(metric.tint)
                MetricGraph.None -> Spacer(Modifier.height(5.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                metric.footer,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HeartRateBars(color: Color) {
    val bars = listOf(0.42f, 0.62f, 0.5f, 0.7f, 0.55f, 0.9f, 0.58f, 0.76f, 0.68f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { height ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((18f * height).dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(color)
            )
        }
    }
}
