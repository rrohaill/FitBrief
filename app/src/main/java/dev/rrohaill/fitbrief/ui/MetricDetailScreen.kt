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
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.data.TimelineEvent
import dev.rrohaill.fitbrief.ui.charts.BarMetricGraph
import dev.rrohaill.fitbrief.ui.charts.CaloriesMetricGraph
import dev.rrohaill.fitbrief.ui.charts.DistanceMetricGraph
import dev.rrohaill.fitbrief.ui.charts.ExerciseDurationGraph
import dev.rrohaill.fitbrief.ui.charts.HeartRateMetricGraph
import dev.rrohaill.fitbrief.ui.charts.HeartRatePeriodGraph
import dev.rrohaill.fitbrief.ui.charts.SleepDurationGraph
import dev.rrohaill.fitbrief.ui.components.Badge
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.LocalTime
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
    val periodDate = LocalDate.now().minusDays(
        when (state.selectedRange) {
            RangeOption.Today -> state.metricDayOffset.toLong()
            RangeOption.SevenDays -> state.metricDayOffset * 7L
            RangeOption.ThirtyDays -> state.metricDayOffset * 30L
        }
    )
    val periodLabel = if (state.metricDrilldownDate != null) {
        state.metricDrilldownDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
    } else when (state.selectedRange) {
        RangeOption.Today -> periodDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
        RangeOption.SevenDays -> "Week ending ${periodDate.format(DateTimeFormatter.ofPattern("MMM d"))}"
        RangeOption.ThirtyDays -> "Month ending ${periodDate.format(DateTimeFormatter.ofPattern("MMM d"))}"
    }
    val heartSamples = if (state.metricHeartRateSamples.isNotEmpty()) {
        state.metricHeartRateSamples
    } else {
        state.timeline.flatMap { it.samples }
    }
    val heartLow = heartSamples.minOrNull()?.toInt() ?: snapshot?.averageHeartRateBpm?.toInt() ?: 0
    val heartHigh = heartSamples.maxOrNull()?.toInt() ?: snapshot?.averageHeartRateBpm?.toInt() ?: 0
    val value = when (metric) {
        MetricType.Steps -> snapshot?.steps?.toString() ?: "0"
        MetricType.HeartRate -> "$heartLow–$heartHigh bpm"
        MetricType.Sleep -> snapshot?.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "0h"
        MetricType.ActiveCalories -> snapshot?.activeCaloriesKcal?.let { "${it.toInt()} kcal" } ?: "0 kcal"
        MetricType.Distance -> snapshot?.distanceKilometers?.let { "${DecimalFormat("#,##0.#").format(it)} km" } ?: "0 km"
        MetricType.Exercise -> "${snapshot?.exerciseMinutes ?: 0} min"
        MetricType.TotalCalories -> snapshot?.totalCaloriesKcal?.let { "${it.toInt()} kcal" } ?: "0 kcal"
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
                        periodLabel,
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
                    Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    if (metric == MetricType.HeartRate) {
                        Text(
                            "Average ${snapshot?.averageHeartRateBpm ?: "—"} bpm  •  Low $heartLow  •  High $heartHigh",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Highest zone: ${heartRateZone(heartHigh)}",
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

private fun heartRateZone(bpm: Int): String = when {
            bpm >= 135 -> "Peak"
            bpm >= 110 -> "Vigorous"
            bpm >= 71 -> "Moderate"
            else -> "Light"
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
    val total = when (metric) {
        MetricType.Steps -> snapshot?.steps?.toFloat() ?: 0f
        MetricType.HeartRate -> snapshot?.averageHeartRateBpm?.toFloat() ?: 0f
        MetricType.Sleep -> snapshot?.sleepMinutes?.toFloat() ?: 0f
        MetricType.ActiveCalories -> snapshot?.activeCaloriesKcal?.toFloat() ?: 0f
        MetricType.Distance -> snapshot?.distanceMeters?.toFloat() ?: 0f
        MetricType.Exercise -> snapshot?.exerciseMinutes?.toFloat() ?: 0f
        MetricType.TotalCalories -> snapshot?.totalCaloriesKcal?.toFloat() ?: 0f
    }
    val metricKey = when (metric) {
        MetricType.Steps -> "steps"
        MetricType.HeartRate -> "heartRate"
        MetricType.Sleep -> "sleep"
        MetricType.ActiveCalories -> "activeCalories"
        MetricType.Distance -> "distance"
        MetricType.Exercise -> "exercise"
        MetricType.TotalCalories -> "totalCalories"
    }
    val heartSamples = if (metricHeartRateSamples.isNotEmpty()) {
        metricHeartRateSamples.map(Double::toFloat)
    } else timeline
        .sortedBy(TimelineEvent::timestamp)
        .flatMap { it.samples }
        .map(Double::toFloat)
    val metricEvents = timeline
        .sortedBy(TimelineEvent::timestamp)
        .filter { it.values.containsKey(metricKey) }
    val measuredValues = if (metric == MetricType.HeartRate && heartSamples.isNotEmpty()) {
        heartSamples
    } else {
        metricEvents.mapNotNull { it.values[metricKey]?.toFloat() }
    }
    val values = measuredValues.ifEmpty { listOf(total) }
    val barDates = metricEvents
        .map { it.timestamp.atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
    val heartPeriodValues = if (metric == MetricType.HeartRate) {
        when (snapshot?.range?.option) {
            RangeOption.SevenDays -> (0 until 7).map { index ->
                timeline.filter {
                    it.timestamp.atZone(java.time.ZoneId.systemDefault()).dayOfWeek.value == index + 1
                }.mapNotNull { it.values["heartRate"]?.toFloat() }.average().toFloat()
            }
            RangeOption.ThirtyDays -> timeline.groupBy {
                it.timestamp.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            }.values.map { group -> group.mapNotNull { it.values["heartRate"]?.toFloat() }.average().toFloat() }
            else -> values
        }.filter { it.isFinite() && it > 0f }
    } else {
        values
    }
    val axisFormatter = DecimalFormat("#,##0.#")
    val rawMax = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val yMin = if (metric == MetricType.HeartRate && snapshot?.range?.option == RangeOption.Today) {
        30f
    } else {
        0f
    }
    val yMax = if (metric == MetricType.HeartRate) {
        val margin = if (snapshot?.range?.option == RangeOption.Today) 10f else 2f
        (kotlin.math.ceil((rawMax + margin) / 10f) * 10f).coerceAtLeast(rawMax + margin)
    } else if (metric == MetricType.Exercise) {
        rawMax.coerceAtLeast(60f)
    } else if (metric == MetricType.Sleep) {
        rawMax.coerceAtLeast(540f)
    } else {
        rawMax
    }
    val yUnit = when (metric) {
        MetricType.Steps -> "steps"
        MetricType.HeartRate -> "bpm"
        MetricType.Sleep -> "hours"
        MetricType.ActiveCalories, MetricType.TotalCalories -> "kcal"
        MetricType.Distance -> "m"
        MetricType.Exercise -> "min"
    }
    val xLabels = if (metric == MetricType.Exercise) {
        listOf("0 min", "30 min", "60 min")
    } else {
        val dates = metricEvents.map { it.timestamp }.sorted()
        val formatter = when (snapshot?.range?.option) {
            RangeOption.Today -> DateTimeFormatter.ofPattern("h a")
            RangeOption.SevenDays -> DateTimeFormatter.ofPattern("EEE")
            RangeOption.ThirtyDays -> DateTimeFormatter.ofPattern("MMM d")
            null -> DateTimeFormatter.ofPattern("h a")
        }
        if (metric == MetricType.HeartRate && snapshot?.range?.option == RangeOption.Today) {
            listOf(
                "12am",
                "4am",
                "8am",
                "12pm",
                LocalTime.now().format(DateTimeFormatter.ofPattern("h a"))
            )
        } else if (dates.isEmpty()) listOf("Start", "Now")
        else listOf(dates.first(), dates[dates.lastIndex / 2], dates.last())
            .distinct()
            .map { it.atZone(java.time.ZoneId.systemDefault()).format(formatter) }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                when (metric) {
                    MetricType.Steps -> "Activity bars"
                    MetricType.HeartRate -> "Heart-rate trend"
                    MetricType.Sleep -> "Sleep trend"
                    MetricType.ActiveCalories, MetricType.TotalCalories -> "Calories burned"
                    MetricType.Distance -> "Distance trend"
                    MetricType.Exercise -> "Workout minutes"
                },
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(48.dp).height(180.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    if (metric == MetricType.HeartRate && snapshot?.range?.option == RangeOption.Today) {
                        listOf(1f, 0.75f, 0.5f, 0.25f, 0f).forEach { fraction ->
                            Text(
                                axisFormatter.format(yMin + (yMax - yMin) * fraction),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    } else {
                        val formatAxisValue: (Float) -> String = { axisValue ->
                            if (metric == MetricType.Sleep) {
                                "${axisFormatter.format(axisValue / 60f)} h"
                            } else {
                                "${axisFormatter.format(axisValue)} $yUnit"
                            }
                        }
                        Text(formatAxisValue(yMax), style = MaterialTheme.typography.labelSmall)
                        Text(
                            formatAxisValue((yMax + yMin) / 2f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(formatAxisValue(yMin), style = MaterialTheme.typography.labelSmall)
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
                        MetricType.HeartRate -> if (snapshot?.range?.option == RangeOption.Today) {
                            HeartRateMetricGraph(values, yMin, yMax)
                        } else {
                            HeartRatePeriodGraph(heartPeriodValues, snapshot?.range?.option, Color(0xFF20C7F2))
                        }
                        MetricType.Sleep -> SleepDurationGraph(values, yMax) { index ->
                            barDates.getOrNull(index)?.let(onBarSelected)
                        }
                        MetricType.ActiveCalories, MetricType.TotalCalories -> CaloriesMetricGraph(
                            values,
                            if (metric == MetricType.ActiveCalories) Color(0xFFEFA92E) else Color(0xFFFF7A59)
                        ) { index ->
                            barDates.getOrNull(index)?.let(onBarSelected)
                        }
                        MetricType.Exercise -> ExerciseDurationGraph(values, yMax, color) { index ->
                            barDates.getOrNull(index)?.let(onBarSelected)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.padding(start = 48.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                xLabels.forEach { label ->
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("X-axis: selected range  •  Y-axis: $yUnit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (metric == MetricType.Sleep) {
                Text(
                    "Shaded band: 7–9 hours",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (metric == MetricType.Exercise) {
                Text(
                    "Reference marker: 30 minutes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
