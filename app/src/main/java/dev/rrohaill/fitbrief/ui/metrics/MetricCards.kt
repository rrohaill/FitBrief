package dev.rrohaill.fitbrief.ui.metrics

import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.ui.MetricType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

enum class MetricCardGraph {
    StepsProgress,
    HeartRateBars,
    None
}

data class MetricCardModel(
    val type: MetricType,
    val title: String,
    val value: String,
    val unit: String,
    val icon: String,
    val tintArgb: Long,
    val footer: String,
    val graph: MetricCardGraph = MetricCardGraph.None,
    val progress: Float? = null
)

private const val DAILY_STEP_GOAL = 10_000L

fun RangeOption?.dayCount(): Int = when (this) {
    RangeOption.SevenDays -> 7
    RangeOption.ThirtyDays -> 30
    RangeOption.Today, null -> 1
}

fun buildMetricCards(
    snapshot: HealthSnapshot?,
    showGraphs: Boolean = true,
    locale: Locale = Locale.getDefault()
): List<MetricCardModel> {
    val data = snapshot ?: return emptyList()
    val decimal = DecimalFormat("#,##0.#", DecimalFormatSymbols(locale))
    val stepGoal = data.range.option.dayCount() * DAILY_STEP_GOAL
    val stepProgress = (data.steps.toFloat() / stepGoal).coerceIn(0f, 1f)
    val stepGoalPercent = (data.steps.toFloat() / stepGoal * 100f).roundToInt()

    val cards = buildList {
        if (data.steps > 0) {
            add(
                MetricCardModel(
                    type = MetricType.Steps,
                    title = "Steps",
                    value = decimal.format(data.steps),
                    unit = "steps",
                    icon = "♧",
                    tintArgb = 0xFF167565,
                    footer = "$stepGoalPercent% of ${decimal.format(stepGoal)} goal",
                    graph = MetricCardGraph.StepsProgress,
                    progress = stepProgress
                )
            )
        }
        data.averageHeartRateBpm?.let { bpm ->
            add(
                MetricCardModel(
                    type = MetricType.HeartRate,
                    title = "Heart Rate",
                    value = bpm.toString(),
                    unit = "bpm avg",
                    icon = "♡",
                    tintArgb = 0xFFE9656D,
                    footer = "Selected range average",
                    graph = MetricCardGraph.HeartRateBars
                )
            )
        }
        if (data.sleepMinutes > 0) {
            add(
                MetricCardModel(
                    type = MetricType.Sleep,
                    title = "Sleep",
                    value = "${data.sleepMinutes / 60}h ${data.sleepMinutes % 60}m",
                    unit = "sleep duration",
                    icon = "☾",
                    tintArgb = 0xFF6576E8,
                    footer = "Selected range"
                )
            )
        }
        if (data.activeCaloriesKcal > 0) {
            add(
                MetricCardModel(
                    type = MetricType.ActiveCalories,
                    title = "Active Calories",
                    value = decimal.format(data.activeCaloriesKcal),
                    unit = "kcal",
                    icon = "♨",
                    tintArgb = 0xFFEFA92E,
                    footer = "Selected range"
                )
            )
        }
        if (data.distanceMeters > 0) {
            add(
                MetricCardModel(
                    type = MetricType.Distance,
                    title = "Distance",
                    value = "${decimal.format(data.distanceKilometers)} km",
                    unit = "distance",
                    icon = "↗",
                    tintArgb = 0xFF4E9BE8,
                    footer = "Selected range"
                )
            )
        }
        if (data.exerciseMinutes > 0) {
            add(
                MetricCardModel(
                    type = MetricType.Exercise,
                    title = "Exercise",
                    value = "${data.exerciseMinutes} min",
                    unit = "exercise time",
                    icon = "✦",
                    tintArgb = 0xFFB276E8,
                    footer = "Selected range"
                )
            )
        }
        if (data.totalCaloriesKcal > 0) {
            add(
                MetricCardModel(
                    type = MetricType.TotalCalories,
                    title = "Total Calories",
                    value = decimal.format(data.totalCaloriesKcal),
                    unit = "kcal",
                    icon = "♨",
                    tintArgb = 0xFFE58B45,
                    footer = "Selected range"
                )
            )
        }
    }
    return if (showGraphs) cards else cards.map { it.copy(graph = MetricCardGraph.None, progress = null) }
}
