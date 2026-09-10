package dev.rrohaill.fitbrief.data

import java.time.Duration
import java.time.Instant

data class ActivityMeasurement(
    val start: Instant,
    val end: Instant,
    val value: Double
)

const val ACTIVITY_GAP_MINUTES = 10L
const val NOTABLE_WINDOW_MINUTES = 10L
const val NOTABLE_WINDOW_STEPS = 500.0

fun TimelineEvent.isTrivial(): Boolean {
    if ("exercise" in values || "sleep" in values) return false
    val end = endTimestamp ?: return true
    val minutes = Duration.between(timestamp, end).toMinutes()
    val steps = values["steps"] ?: 0.0
    return minutes < NOTABLE_WINDOW_MINUTES || ("steps" in values && steps < NOTABLE_WINDOW_STEPS)
}

fun List<TimelineEvent>.notable(): List<TimelineEvent> = filterNot { it.isTrivial() }

fun groupIntoWindows(
    measurements: List<ActivityMeasurement>,
    gapMinutes: Long = ACTIVITY_GAP_MINUTES
): List<List<ActivityMeasurement>> {
    val windows = mutableListOf<MutableList<ActivityMeasurement>>()
    var windowEnd: Instant? = null
    measurements.sortedBy(ActivityMeasurement::start).forEach { measurement ->
        val lastEnd = windowEnd
        if (lastEnd == null || Duration.between(lastEnd, measurement.start).toMinutes() > gapMinutes) {
            windows += mutableListOf(measurement)
            windowEnd = measurement.end
        } else {
            windows.last() += measurement
            windowEnd = maxOf(lastEnd, measurement.end)
        }
    }
    return windows
}

fun buildActivityWindows(
    steps: List<ActivityMeasurement>,
    distance: List<ActivityMeasurement>,
    activeCalories: List<ActivityMeasurement>,
    totalCalories: List<ActivityMeasurement>
): List<TimelineEvent> {
    val seeds = steps + distance
    if (seeds.isEmpty()) return emptyList()
    return groupIntoWindows(seeds).map { window ->
        val start = window.first().start
        val end = window.maxOf(ActivityMeasurement::end)
        fun List<ActivityMeasurement>.sumWithin() =
            filter { it.start <= end && it.end >= start }.sumOf(ActivityMeasurement::value)

        val stepCount = steps.sumWithin().toLong()
        val meters = distance.sumWithin()
        val activeKcal = activeCalories.sumWithin()
        val totalKcal = totalCalories.sumWithin()
        val minutes = Duration.between(start, end).toWholeMinutes().coerceAtLeast(1)

        val values = buildMap {
            if (stepCount > 0) put("steps", stepCount.toDouble())
            if (meters > 0) put("distance", meters)
            if (activeKcal > 0) put("activeCalories", activeKcal)
            if (totalKcal > 0) put("totalCalories", totalKcal)
        }
        val movement = listOfNotNull(
            stepCount.takeIf { it > 0 }?.let { "took $it steps" },
            meters.takeIf { it > 0 }?.let { "covered ${it.toInt()} meters" }
        ).joinToString(" and ")
        val energy = activeKcal.takeIf { it > 0 }?.let { ", burning ${it.toInt()} active kcal" } ?: ""
        TimelineEvent(
            timestamp = start,
            title = "Walking activity",
            detail = "You $movement over about $minutes minutes$energy.",
            icon = "♧",
            endTimestamp = end,
            values = values
        )
    }
}
