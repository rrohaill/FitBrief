package dev.rrohaill.fitbrief.ui.metrics

import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.data.TimelineEvent
import dev.rrohaill.fitbrief.ui.MetricType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MetricDetailHeader(
    val periodLabel: String,
    val value: String,
    val heartLow: Int,
    val heartHigh: Int
) {
    val highestHeartRateZone: String get() = heartRateZone(heartHigh)
}

fun heartRateZone(bpm: Int): String = when {
    bpm >= 135 -> "Peak"
    bpm >= 110 -> "Vigorous"
    bpm >= 71 -> "Moderate"
    else -> "Light"
}

fun periodEndDate(range: RangeOption, dayOffset: Int, today: LocalDate): LocalDate = when (range) {
    RangeOption.Today -> today.minusDays(dayOffset.toLong())
    RangeOption.SevenDays -> today.minusDays(dayOffset * 7L)
    RangeOption.Month -> today.minusMonths(dayOffset.toLong())
}

fun buildMetricDetailHeader(
    metric: MetricType,
    snapshot: HealthSnapshot?,
    timeline: List<TimelineEvent>,
    heartRateSamples: List<Double>,
    selectedRange: RangeOption,
    dayOffset: Int,
    drilldownDate: LocalDate?,
    today: LocalDate = LocalDate.now(),
    locale: Locale = Locale.getDefault()
): MetricDetailHeader {
    val periodDate = periodEndDate(selectedRange, dayOffset, today)
    val periodLabel = if (drilldownDate != null) {
        drilldownDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale))
    } else when (selectedRange) {
        RangeOption.Today -> periodDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", locale))
        RangeOption.SevenDays -> "Week ending ${periodDate.format(DateTimeFormatter.ofPattern("MMM d", locale))}"
        RangeOption.Month -> periodDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    }

    val samples = heartRateSamples.ifEmpty { timeline.flatMap { it.samples } }
    val average = snapshot?.averageHeartRateBpm?.toInt() ?: 0
    val heartLow = samples.minOrNull()?.toInt() ?: average
    val heartHigh = samples.maxOrNull()?.toInt() ?: average

    val decimal = DecimalFormat("#,##0.#", DecimalFormatSymbols(locale))
    val value = when (metric) {
        MetricType.Steps -> snapshot?.steps?.toString() ?: "0"
        MetricType.HeartRate -> "$heartLow–$heartHigh bpm"
        MetricType.Sleep -> snapshot?.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "0h"
        MetricType.ActiveCalories -> snapshot?.activeCaloriesKcal?.let { "${it.toInt()} kcal" } ?: "0 kcal"
        MetricType.Distance -> snapshot?.distanceKilometers?.let { "${decimal.format(it)} km" } ?: "0 km"
        MetricType.Exercise -> "${snapshot?.exerciseMinutes ?: 0} min"
        MetricType.TotalCalories -> snapshot?.totalCaloriesKcal?.let { "${it.toInt()} kcal" } ?: "0 kcal"
    }
    return MetricDetailHeader(periodLabel, value, heartLow, heartHigh)
}
