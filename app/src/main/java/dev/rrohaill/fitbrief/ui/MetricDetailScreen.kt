package dev.rrohaill.fitbrief.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.TimelineEvent
import dev.rrohaill.fitbrief.ui.charts.BarMetricGraph
import dev.rrohaill.fitbrief.ui.charts.CaloriesMetricGraph
import dev.rrohaill.fitbrief.ui.charts.DistanceMetricGraph
import dev.rrohaill.fitbrief.ui.charts.ExerciseDurationGraph
import dev.rrohaill.fitbrief.ui.charts.HeartRateMetricGraph
import dev.rrohaill.fitbrief.ui.charts.HeartRatePeriodGraph
import dev.rrohaill.fitbrief.ui.charts.SleepDurationGraph
import dev.rrohaill.fitbrief.ui.components.Badge
import dev.rrohaill.fitbrief.ui.metrics.buildMetricChartModel
import dev.rrohaill.fitbrief.ui.metrics.buildMetricDetailHeader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun MetricDetailScreen(
    state: FitBriefUiState,
    metric: MetricType,
    onClose: () -> Unit,
    onNavigateDay: (Int) -> Unit,
    onOpenMetricDate: (LocalDate) -> Unit
) {
    val snapshot = state.snapshot
    val timeline = state.timeline
    val graphLoading = snapshot == null
    var selectedBarDate by remember(metric, state.metricDrilldownDate) { mutableStateOf<LocalDate?>(null) }
    val header = remember(metric, snapshot, timeline, state.metricHeartRateSamples, state.selectedRange, state.metricDayOffset, state.metricDrilldownDate) {
        buildMetricDetailHeader(
            metric = metric,
            snapshot = snapshot,
            timeline = timeline,
            heartRateSamples = state.metricHeartRateSamples,
            selectedRange = state.selectedRange,
            dayOffset = state.metricDayOffset,
            drilldownDate = state.metricDrilldownDate
        )
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("‹", style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.clickable(onClick = onClose).padding(end = 12.dp))
                Column {
                    Text(metric.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        header.periodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Badge(
                    "‹",
                    MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.colorScheme.onSurface,
                    40.dp,
                    onClick = { onNavigateDay(1) }
                )
                Spacer(Modifier.width(8.dp))
                Badge(
                    "›",
                    MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (state.metricDayOffset == 0) 0.35f else 1f
                    ),
                    40.dp,
                    onClick = { if (state.metricDayOffset > 0) onNavigateDay(-1) }
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (metric == MetricType.HeartRate) "Heart-rate range" else "Selected range total",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(header.value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    if (metric == MetricType.HeartRate) {
                        Text(
                            "Average ${snapshot?.averageHeartRateBpm ?: "—"} bpm  •  Low ${header.heartLow}  •  High ${header.heartHigh}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Highest zone: ${header.highestHeartRateZone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("Detailed view from Health Connect data for this range.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!graphLoading) {
                MetricDetailGraph(metric, snapshot, timeline, state.metricHeartRateSamples) { date ->
                    selectedBarDate = date
                }
                selectedBarDate?.let { date ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenMetricDate(date) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "View day ›",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (metric == MetricType.HeartRate) HeartRateZoneLegend()
                MetricInsightCard(state)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Loading ${metric.label.lowercase()} data…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                }
            }
            Text("This visualization is descriptive context, not medical advice.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricInsightCard(state: FitBriefUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AI insight", fontWeight = FontWeight.Bold)
            if (state.metricInsightLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Analyzing this metric…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text(
                    state.metricInsight ?: "No AI insight is available for this metric yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HeartRateZoneLegend() {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    "Light" to Color(0xFF32C7F0),
                    "Moderate" to Color(0xFF4FD477),
                    "Vigorous" to Color(0xFFFFB52E),
                    "Peak" to Color(0xFFE9656D)
                ).forEach { (label, color) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
                        Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun MetricDetailGraph(
    metric: MetricType,
    snapshot: HealthSnapshot?,
    timeline: List<TimelineEvent>,
    metricHeartRateSamples: List<Double>,
    onBarSelected: (LocalDate) -> Unit
) {
    val color = MaterialTheme.colorScheme.primary
    val chart = remember(metric, snapshot, timeline, metricHeartRateSamples) {
        buildMetricChartModel(metric, snapshot, timeline, metricHeartRateSamples)
    }
    val values = chart.values
    val barDates = chart.barDates
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(chart.title, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(48.dp).height(180.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    chart.yAxisLabels.forEach { label ->
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Box(Modifier.weight(1f).height(180.dp)) {
                    when (metric) {
                        MetricType.Steps -> BarMetricGraph(values, color) { index ->
                            barDates.getOrNull(index)?.let(onBarSelected)
                        }
                        MetricType.Distance -> DistanceMetricGraph(values) { index ->
                            barDates.getOrNull(index)?.let(onBarSelected)
                        }
                        MetricType.HeartRate -> if (chart.isHeartRateToday) {
                            HeartRateMetricGraph(values, chart.yMin, chart.yMax)
                        } else {
                            HeartRatePeriodGraph(chart.heartPeriodValues, chart.range, Color(0xFF20C7F2))
                        }
                        MetricType.Sleep -> SleepDurationGraph(values, chart.yMax) { index ->
                            barDates.getOrNull(index)?.let(onBarSelected)
                        }
                        MetricType.ActiveCalories, MetricType.TotalCalories -> CaloriesMetricGraph(
                            values,
                            if (metric == MetricType.ActiveCalories) Color(0xFFEFA92E) else Color(0xFFFF7A59)
                        ) { index ->
                            barDates.getOrNull(index)?.let(onBarSelected)
                        }
                        MetricType.Exercise -> ExerciseDurationGraph(values, chart.yMax, color) { index ->
                            barDates.getOrNull(index)?.let(onBarSelected)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.padding(start = 48.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                chart.xLabels.forEach { label ->
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("X-axis: selected range  •  Y-axis: ${chart.yUnit}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            chart.footnote?.let { note ->
                Text(
                    note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
