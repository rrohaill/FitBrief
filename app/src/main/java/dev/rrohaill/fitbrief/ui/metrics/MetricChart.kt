package dev.rrohaill.fitbrief.ui.metrics

import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.data.TimelineEvent
import dev.rrohaill.fitbrief.ui.MetricType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.ceil

data class MetricChartModel(
    val metric: MetricType,
    val range: RangeOption?,
    val title: String,
    val values: List<Float>,
    val barDates: List<LocalDate>,
    val heartPeriodValues: List<Float>,
    val yMin: Float,
    val yMax: Float,
    val yUnit: String,
    val yAxisLabels: List<String>,
    val xLabels: List<String>,
    val footnote: String?
) {
    val isHeartRateToday: Boolean get() = metric == MetricType.HeartRate && range == RangeOption.Today
}

val MetricType.timelineKey: String
    get() = when (this) {
        MetricType.Steps -> "steps"
        MetricType.HeartRate -> "heartRate"
        MetricType.Sleep -> "sleep"
        MetricType.ActiveCalories -> "activeCalories"
        MetricType.Distance -> "distance"
        MetricType.Exercise -> "exercise"
        MetricType.TotalCalories -> "totalCalories"
    }

fun buildMetricChartModel(
    metric: MetricType,
    snapshot: HealthSnapshot?,
    timeline: List<TimelineEvent>,
    heartRateSamples: List<Double>,
    zoneId: ZoneId = ZoneId.systemDefault(),
    now: LocalTime = LocalTime.now(zoneId),
    locale: Locale = Locale.getDefault()
): MetricChartModel {
    val range = snapshot?.range?.option
    val total = when (metric) {
        MetricType.Steps -> snapshot?.steps?.toFloat()
        MetricType.HeartRate -> snapshot?.averageHeartRateBpm?.toFloat()
        MetricType.Sleep -> snapshot?.sleepMinutes?.toFloat()
        MetricType.ActiveCalories -> snapshot?.activeCaloriesKcal?.toFloat()
        MetricType.Distance -> snapshot?.distanceMeters?.toFloat()
        MetricType.Exercise -> snapshot?.exerciseMinutes?.toFloat()
        MetricType.TotalCalories -> snapshot?.totalCaloriesKcal?.toFloat()
    } ?: 0f
    val key = metric.timelineKey
    val sortedTimeline = timeline.sortedBy(TimelineEvent::timestamp)
    val heartSamples = if (heartRateSamples.isNotEmpty()) {
        heartRateSamples.map(Double::toFloat)
    } else {
        sortedTimeline.flatMap { it.samples }.map(Double::toFloat)
    }
    val metricEvents = sortedTimeline.filter { it.values.containsKey(key) }
    val measuredValues = if (metric == MetricType.HeartRate && heartSamples.isNotEmpty()) {
        heartSamples
    } else {
        metricEvents.mapNotNull { it.values[key]?.toFloat() }
    }
    val values = measuredValues.ifEmpty { listOf(total) }
    val barDates = metricEvents.map { it.timestamp.atZone(zoneId).toLocalDate() }

    val heartPeriodValues = if (metric == MetricType.HeartRate) {
        when (range) {
            RangeOption.SevenDays -> DayOfWeek.entries.map { day ->
                timeline
                    .filter { it.timestamp.atZone(zoneId).dayOfWeek == day }
                    .mapNotNull { it.values[key]?.toFloat() }
                    .average().toFloat()
            }
            RangeOption.Month -> timeline
                .groupBy {
                    it.timestamp.atZone(zoneId).toLocalDate()
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                }
                .values
                .map { week -> week.mapNotNull { it.values[key]?.toFloat() }.average().toFloat() }
            else -> values
        }.filter { it.isFinite() && it > 0f }
    } else {
        values
    }

    val isHeartRateToday = metric == MetricType.HeartRate && range == RangeOption.Today
    val rawMax = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val yMin = if (isHeartRateToday) 30f else 0f
    val yMax = when (metric) {
        MetricType.HeartRate -> {
            val margin = if (range == RangeOption.Today) 10f else 2f
            (ceil((rawMax + margin) / 10f) * 10f).coerceAtLeast(rawMax + margin)
        }
        MetricType.Exercise -> rawMax.coerceAtLeast(60f)
        MetricType.Sleep -> rawMax.coerceAtLeast(540f)
        else -> rawMax
    }
    val yUnit = when (metric) {
        MetricType.Steps -> "steps"
        MetricType.HeartRate -> "bpm"
        MetricType.Sleep -> "hours"
        MetricType.ActiveCalories, MetricType.TotalCalories -> "kcal"
        MetricType.Distance -> "m"
        MetricType.Exercise -> "min"
    }

    val axisFormatter = DecimalFormat("#,##0.#", DecimalFormatSymbols(locale))
    val yAxisLabels = if (isHeartRateToday) {
        listOf(1f, 0.75f, 0.5f, 0.25f, 0f).map { fraction -> axisFormatter.format(yMin + (yMax - yMin) * fraction) }
    } else {
        fun label(axisValue: Float) = if (metric == MetricType.Sleep) {
            "${axisFormatter.format(axisValue / 60f)} h"
        } else {
            "${axisFormatter.format(axisValue)} $yUnit"
        }
        listOf(label(yMax), label((yMax + yMin) / 2f), label(yMin))
    }

    val xLabels = when {
        metric == MetricType.Exercise -> listOf("0 min", "30 min", "60 min")
        isHeartRateToday -> listOf("12am", "4am", "8am", "12pm", now.format(DateTimeFormatter.ofPattern("h a", locale)))
        else -> {
            val dates = metricEvents.map { it.timestamp }.sorted()
            val pattern = when (range) {
                RangeOption.SevenDays -> "EEE"
                RangeOption.Month -> "MMM d"
                RangeOption.Today, null -> "h a"
            }
            val formatter = DateTimeFormatter.ofPattern(pattern, locale)
            if (dates.isEmpty()) {
                listOf("Start", "Now")
            } else {
                listOf(dates.first(), dates[dates.lastIndex / 2], dates.last())
                    .distinct()
                    .map { it.atZone(zoneId).format(formatter) }
            }
        }
    }

    val title = when (metric) {
        MetricType.Steps -> "Activity bars"
        MetricType.HeartRate -> "Heart-rate trend"
        MetricType.Sleep -> "Sleep trend"
        MetricType.ActiveCalories, MetricType.TotalCalories -> "Calories burned"
        MetricType.Distance -> "Distance trend"
        MetricType.Exercise -> "Workout minutes"
    }
    val footnote = when (metric) {
        MetricType.Sleep -> "Shaded band: 7–9 hours"
        MetricType.Exercise -> "Reference marker: 30 minutes"
        else -> null
    }

    return MetricChartModel(
        metric = metric,
        range = range,
        title = title,
        values = values,
        barDates = barDates,
        heartPeriodValues = heartPeriodValues,
        yMin = yMin,
        yMax = yMax,
        yUnit = yUnit,
        yAxisLabels = yAxisLabels,
        xLabels = xLabels,
        footnote = footnote
    )
}
