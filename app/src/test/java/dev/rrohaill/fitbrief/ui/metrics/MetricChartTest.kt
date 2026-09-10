package dev.rrohaill.fitbrief.ui.metrics

import dev.rrohaill.fitbrief.data.DailyHealthMetrics
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.ui.MetricType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

class MetricChartTest {
    private val monday = LocalDate.of(2026, 9, 7)

    private fun chart(
        metric: MetricType,
        range: RangeOption = RangeOption.Today,
        timeline: List<dev.rrohaill.fitbrief.data.TimelineEvent> = emptyList(),
        samples: List<Double> = emptyList()
    ) = buildMetricChartModel(
        metric = metric,
        snapshot = snapshot(range),
        timeline = timeline,
        heartRateSamples = samples,
        zoneId = zone,
        now = LocalTime.of(15, 30),
        locale = Locale.US
    )

    @Test
    fun `series and bar dates come from timeline events carrying the metric key`() {
        val timeline = listOf(
            event(monday.plusDays(2), values = mapOf("steps" to 3000.0)),
            event(monday, values = mapOf("steps" to 1000.0)),
            event(monday.plusDays(1), values = mapOf("sleep" to 400.0)),
            event(monday.plusDays(1), values = mapOf("steps" to 2000.0))
        )
        val c = chart(MetricType.Steps, RangeOption.SevenDays, timeline)
        assertEquals(listOf(1000f, 2000f, 3000f), c.values)
        assertEquals(listOf(monday, monday.plusDays(1), monday.plusDays(2)), c.barDates)
        assertEquals(listOf("Mon", "Tue", "Wed"), c.xLabels)
        assertEquals("steps", c.yUnit)
        assertEquals(3000f, c.yMax)
        assertEquals(listOf("3,000 steps", "1,500 steps", "0 steps"), c.yAxisLabels)
        assertEquals("Activity bars", c.title)
        assertNull(c.footnote)
    }

    @Test
    fun `without events the series falls back to the snapshot total`() {
        val c = chart(MetricType.Distance)
        assertEquals(listOf(6240f), c.values)
        assertTrue(c.barDates.isEmpty())
        assertEquals(listOf("Start", "Now"), c.xLabels)
    }

    @Test
    fun `sleep axis is in hours with a nine hour floor`() {
        val c = chart(MetricType.Sleep, timeline = listOf(event(monday, values = mapOf("sleep" to 420.0))))
        assertEquals(540f, c.yMax)
        assertEquals(listOf("9 h", "4.5 h", "0 h"), c.yAxisLabels)
        assertEquals("Shaded band: 7–9 hours", c.footnote)
    }

    @Test
    fun `exercise uses fixed x labels and a one hour floor`() {
        val c = chart(MetricType.Exercise)
        assertEquals(listOf("0 min", "30 min", "60 min"), c.xLabels)
        assertEquals(60f, c.yMax)
        assertEquals("Reference marker: 30 minutes", c.footnote)
    }

    @Test
    fun `heart rate today plots raw samples on a padded bpm axis`() {
        val c = chart(MetricType.HeartRate, samples = listOf(60.0, 95.0, 130.0))
        assertTrue(c.isHeartRateToday)
        assertEquals(listOf(60f, 95f, 130f), c.values)
        assertEquals(30f, c.yMin)
        assertEquals(140f, c.yMax)
        assertEquals(listOf("140", "112.5", "85", "57.5", "30"), c.yAxisLabels)
        assertEquals(listOf("12am", "4am", "8am", "12pm", "3 PM"), c.xLabels)
    }

    @Test
    fun `heart rate over seven days averages per weekday and drops empty days`() {
        val timeline = listOf(
            event(monday, values = mapOf("heartRate" to 70.0)),
            event(monday, values = mapOf("heartRate" to 80.0)),
            event(monday.plusDays(2), values = mapOf("heartRate" to 90.0))
        )
        val c = chart(MetricType.HeartRate, RangeOption.SevenDays, timeline)
        assertFalse(c.isHeartRateToday)
        assertEquals(listOf(75f, 90f), c.heartPeriodValues)
        assertEquals(60f, c.yMin)
        assertEquals(100f, c.yMax)
    }

    @Test
    fun `heart rate over thirty days averages per week`() {
        val timeline = listOf(
            event(monday, values = mapOf("heartRate" to 60.0)),
            event(monday.plusDays(3), values = mapOf("heartRate" to 80.0)),
            event(monday.plusDays(8), values = mapOf("heartRate" to 100.0))
        )
        val c = chart(MetricType.HeartRate, RangeOption.Month, timeline)
        assertEquals(listOf(70f, 100f), c.heartPeriodValues)
    }

    @Test
    fun `heart rate samples embedded in timeline events are used when no samples are given`() {
        val timeline = listOf(event(monday, samples = listOf(64.0, 71.0)))
        val c = chart(MetricType.HeartRate, timeline = timeline)
        assertEquals(listOf(64f, 71f), c.values)
    }

    @Test
    fun `heart rate over a month plots one point per day with dates for drilldown`() {
        val daily = (1..10).map { day -> DailyHealthMetrics(LocalDate.of(2026, 9, day), averageHeartRateBpm = 60.0 + day) }
        val c = buildMetricChartModel(
            metric = MetricType.HeartRate,
            snapshot = snapshot(RangeOption.Month),
            timeline = listOf(event(monday, values = mapOf("heartRate" to 95.0))),
            heartRateSamples = listOf(50.0, 120.0),
            dailyMetrics = daily,
            zoneId = zone,
            now = LocalTime.NOON,
            locale = Locale.US
        )
        assertEquals((1..10).map { 60f + it }, c.heartPeriodValues)
        assertEquals(daily.map { it.date }, c.barDates)
        assertEquals(listOf("Sep 1", "Sep 5", "Sep 10"), c.xLabels)
        assertEquals(50f, c.yMin)
        assertEquals(80f, c.yMax)
        assertEquals(listOf("80 bpm", "65 bpm", "50 bpm"), c.yAxisLabels)
    }

    @Test
    fun `month bar charts use one bar per day from daily totals`() {
        val daily = (1..10).map { day ->
            DailyHealthMetrics(LocalDate.of(2026, 9, day), steps = day * 1_000L, sleepMinutes = 400L + day, exerciseMinutes = day.toLong())
        }
        val weeklyTimeline = listOf(event(monday, values = mapOf("steps" to 99_999.0)))
        val steps = buildMetricChartModel(MetricType.Steps, snapshot(RangeOption.Month), weeklyTimeline, emptyList(), daily, zone, LocalTime.NOON, Locale.US)
        assertEquals((1..10).map { it * 1_000f }, steps.values)
        assertEquals(daily.map { it.date }, steps.barDates)
        assertEquals(listOf("Sep 1", "Sep 5", "Sep 10"), steps.xLabels)
        assertEquals(10_000f, steps.yMax)

        val sleep = buildMetricChartModel(MetricType.Sleep, snapshot(RangeOption.Month), weeklyTimeline, emptyList(), daily, zone, LocalTime.NOON, Locale.US)
        assertEquals(540f, sleep.yMax)
        assertEquals(10, sleep.values.size)

        val exercise = buildMetricChartModel(MetricType.Exercise, snapshot(RangeOption.Month), weeklyTimeline, emptyList(), daily, zone, LocalTime.NOON, Locale.US)
        assertEquals(listOf("Sep 1", "Sep 5", "Sep 10"), exercise.xLabels)
    }

    @Test
    fun `daily totals are ignored for today`() {
        val daily = listOf(DailyHealthMetrics(monday, steps = 5_000))
        val c = buildMetricChartModel(MetricType.Steps, snapshot(RangeOption.Today), emptyList(), emptyList(), daily, zone, LocalTime.NOON, Locale.US)
        assertEquals(listOf(8_432f), c.values)
    }
}
