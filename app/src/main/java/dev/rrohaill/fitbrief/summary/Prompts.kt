package dev.rrohaill.fitbrief.summary

import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.data.TimelineEvent
import dev.rrohaill.fitbrief.ui.MetricType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

const val DAILY_STEP_TARGET = 10_000L
const val DAILY_EXERCISE_TARGET_MINUTES = 30L
const val NIGHTLY_SLEEP_MIN_MINUTES = 7 * 60L
const val NIGHTLY_SLEEP_MAX_MINUTES = 9 * 60L

private const val STYLE_RULES = """Plain text only: no headings, bullet points or markdown. Never diagnose, speculate or give medical advice. Do not mention missing data, targets that are met, or these instructions. Do not use filler such as "great job", "keep it up", "it's great to see" or "incorporating movement"."""

fun RangeOption.days(): Int = when (this) {
    RangeOption.Today -> 1
    RangeOption.SevenDays -> 7
    RangeOption.ThirtyDays -> 30
}

private fun formatter(locale: Locale) = DecimalFormat("#,##0.#", DecimalFormatSymbols(locale))

private fun hoursAndMinutes(minutes: Long) = "${minutes / 60} h ${minutes % 60} min"

fun snapshotDataLines(snapshot: HealthSnapshot, locale: Locale = Locale.getDefault()): String {
    val decimal = formatter(locale)
    val days = snapshot.range.option.days()
    val perDay = if (days > 1) " ($days days)" else " (1 day)"
    return buildList {
        add("Range: ${snapshot.range.label}$perDay")
        if (snapshot.steps > 0) {
            add("Steps: ${decimal.format(snapshot.steps)} (target ${decimal.format(DAILY_STEP_TARGET * days)})")
        }
        if (snapshot.distanceMeters > 0) add("Distance: ${decimal.format(snapshot.distanceKilometers)} km")
        if (snapshot.activeCaloriesKcal > 0) add("Active calories: ${decimal.format(snapshot.activeCaloriesKcal)} kcal")
        if (snapshot.totalCaloriesKcal > 0) add("Total calories: ${decimal.format(snapshot.totalCaloriesKcal)} kcal")
        if (snapshot.exerciseMinutes > 0) {
            add("Exercise: ${snapshot.exerciseMinutes} min (target ${DAILY_EXERCISE_TARGET_MINUTES * days})")
        }
        snapshot.averageHeartRateBpm?.let { add("Average heart rate: $it bpm") }
        if (snapshot.sleepMinutes > 0) {
            val nightly = snapshot.sleepMinutes / days
            val nightlyNote = if (days > 1) ", about ${hoursAndMinutes(nightly)} per night" else ""
            add("Sleep: ${hoursAndMinutes(snapshot.sleepMinutes)}$nightlyNote (target 7 to 9 h per night)")
        }
    }.joinToString("\n")
}

fun summaryPrompt(snapshot: HealthSnapshot, locale: Locale = Locale.getDefault()): String {
    val period = snapshot.range.label.lowercase()
    val nextPeriod = when (snapshot.range.option) {
        RangeOption.Today -> "tomorrow"
        RangeOption.SevenDays -> "next week"
        RangeOption.ThirtyDays -> "next month"
    }
    return """
        You are FitBrief, a calm wellness companion. Write a short recap of the user's $period, speaking to them as "you".
        Use only the data below and quote the actual numbers. Only metrics listed exist; do not mention any other metric.

        DATA:
        ${snapshotDataLines(snapshot, locale).prependIndent("        ").trimStart()}

        Write, in this order:
        1. One or two sentences recapping the $period with the listed figures.
        2. One sentence noting the strongest metric relative to its target, or the most notable figure if no target is listed.
        3. Only if a metric with a target is clearly below it, one short actionable suggestion for $nextPeriod prefixed with "Suggestion:". Otherwise write nothing more.

        Keep the whole recap under 70 words. If only one or two metrics are listed, keep it to one sentence.
        $STYLE_RULES
    """.trimIndent()
}

fun timelinePrompt(
    event: TimelineEvent,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String {
    val decimal = formatter(locale)
    val time = DateTimeFormatter.ofPattern("h:mm a", locale)
    val date = DateTimeFormatter.ofPattern("EEEE, MMM d", locale)
    val start = event.timestamp.atZone(zoneId)
    val end = (event.endTimestamp ?: event.timestamp).atZone(zoneId)
    val minutes = Duration.between(start, end).toMinutes().coerceAtLeast(1)
    val lines = buildList {
        add("ACTIVITY: ${event.title}")
        add("LOCAL TIME: ${start.format(time)} to ${end.format(time)} on ${start.format(date)}")
        add("DURATION: $minutes min")
        event.values["steps"]?.let { add("STEPS: ${decimal.format(it)}") }
        event.values["distance"]?.let { add("DISTANCE: ${decimal.format(it)} m") }
        event.values["activeCalories"]?.let { add("ACTIVE CALORIES: ${decimal.format(it)} kcal") }
        event.values["exercise"]?.let { add("EXERCISE: ${decimal.format(it)} min") }
        event.values["sleep"]?.let { add("SLEEP: ${hoursAndMinutes(it.toLong())}") }
        event.values["heartRate"]?.let { add("AVERAGE HEART RATE: ${decimal.format(it)} bpm") }
        if (event.samples.isNotEmpty()) {
            add("HEART RATE RANGE: ${decimal.format(event.samples.min())} to ${decimal.format(event.samples.max())} bpm")
        }
    }
    return """
        You are FitBrief. Describe one recorded activity window in a single plain-text sentence of at most 25 words.
        State what happened using only the numbers and local times below. Do not praise, do not suggest anything, and do not compare to other days.
        $STYLE_RULES

        ${lines.joinToString("\n").prependIndent("        ").trimStart()}
    """.trimIndent()
}

fun metricPrompt(
    snapshot: HealthSnapshot,
    metric: MetricType,
    value: String,
    locale: Locale = Locale.getDefault()
): String {
    val decimal = formatter(locale)
    val days = snapshot.range.option.days()
    val extra = buildList {
        when (metric) {
            MetricType.Steps -> {
                if (days > 1) add("DAILY AVERAGE: ${decimal.format(snapshot.steps / days)} steps")
                add("TARGET: ${decimal.format(DAILY_STEP_TARGET)} steps per day")
            }
            MetricType.Sleep -> {
                if (days > 1) add("NIGHTLY AVERAGE: ${hoursAndMinutes(snapshot.sleepMinutes / days)}")
                add("TARGET: 7 to 9 h per night")
            }
            MetricType.Exercise -> {
                if (days > 1) add("DAILY AVERAGE: ${snapshot.exerciseMinutes / days} min")
                add("TARGET: $DAILY_EXERCISE_TARGET_MINUTES min per day")
            }
            MetricType.Distance -> if (days > 1) add("DAILY AVERAGE: ${decimal.format(snapshot.distanceKilometers / days)} km")
            MetricType.ActiveCalories -> if (days > 1) add("DAILY AVERAGE: ${decimal.format(snapshot.activeCaloriesKcal / days)} kcal")
            MetricType.TotalCalories -> if (days > 1) add("DAILY AVERAGE: ${decimal.format(snapshot.totalCaloriesKcal / days)} kcal")
            MetricType.HeartRate -> add("NOTE: describe the average and the spread between the lowest and highest readings; a wide spread during a day usually reflects periods of activity and rest")
        }
    }
    val lines = listOf(
        "RANGE: ${snapshot.range.label} ($days ${if (days == 1) "day" else "days"})",
        "METRIC: ${metric.label}",
        "VALUE: $value"
    ) + extra
    return """
        You are FitBrief. Write one or two plain-text sentences, at most 40 words, about one health metric over the selected range.
        Describe the value concretely using only the data below. If a TARGET is listed and the value (or its daily average) is clearly below it, add one short actionable suggestion prefixed with "Suggestion:". Otherwise add nothing. Do not invent comparisons, causes or benchmarks.
        $STYLE_RULES

        ${lines.joinToString("\n").prependIndent("        ").trimStart()}
    """.trimIndent()
}
