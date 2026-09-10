package dev.rrohaill.fitbrief.summary

import dev.rrohaill.fitbrief.data.HealthRange
import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.data.TimelineEvent
import dev.rrohaill.fitbrief.ui.MetricType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class PromptsTest {
    private val zone = ZoneId.of("Europe/Stockholm")
    private val start = Instant.parse("2026-09-10T10:01:00Z")

    private fun snapshot(option: RangeOption = RangeOption.Today, steps: Long = 8_432, sleep: Long = 443, hr: Long? = 72) =
        HealthSnapshot(HealthRange(option, start, start), steps, 6_240.0, 384.0, 1_820.0, 42, hr, sleep)

    private fun window(minutes: Long, values: Map<String, Double>, samples: List<Double> = emptyList()) =
        TimelineEvent(start, "Walking activity", "detail", "♧", start.plusSeconds(minutes * 60), values = values, samples = samples)

    @Test
    fun `data lines list only present metrics with scaled targets`() {
        val lines = snapshotDataLines(snapshot(RangeOption.SevenDays, steps = 35_000, sleep = 7 * 420, hr = null), Locale.US)
        assertEquals(
            """
            |Range: 7 days (7 days)
            |Steps: 35,000 (target 70,000)
            |Distance: 6.2 km
            |Active calories burned: 384 kcal
            |Total calories burned (including resting): 1,820 kcal
            |Exercise: 42 min (target 210)
            |Sleep: 49 h 0 min, about 7 h 0 min per night (target 7 to 9 h per night)
            """.trimMargin(),
            lines
        )
        assertFalse(lines.contains("heart", ignoreCase = true))
    }

    @Test
    fun `summary prompt embeds structured data and no template verdicts`() {
        val prompt = summaryPrompt(snapshot(), Locale.US)
        assertTrue(prompt.contains("Steps: 8,432 (target 10,000)"))
        assertTrue(prompt.contains("suggestion for tomorrow"))
        assertFalse(prompt.contains("movement day"))
        assertFalse(prompt.contains("no heart-rate average"))
    }

    @Test
    fun `timeline prompt uses local times and raw values`() {
        val prompt = timelinePrompt(window(14, mapOf("steps" to 64.0, "distance" to 27.0, "activeCalories" to 5.0)), zone, Locale.US)
        assertTrue(prompt.contains("LOCAL TIME: 12:01 PM to 12:15 PM on Thursday, Sep 10"))
        assertTrue(prompt.contains("DURATION: 14 min"))
        assertTrue(prompt.contains("STEPS: 64"))
        assertTrue(prompt.contains("DISTANCE: 27 m"))
        assertTrue(prompt.contains("ACTIVE CALORIES BURNED: 5 kcal"))
        assertTrue(prompt.contains("at most 25 words"))
        assertFalse(prompt.contains("Suggestion:"))
        assertFalse(prompt.contains("detail"))
    }

    @Test
    fun `timeline prompt reports heart rate range from samples`() {
        val prompt = timelinePrompt(window(20, mapOf("heartRate" to 88.0), listOf(70.0, 101.0, 93.0)), zone, Locale.US)
        assertTrue(prompt.contains("AVERAGE HEART RATE: 88 bpm"))
        assertTrue(prompt.contains("HEART RATE RANGE: 70 to 101 bpm"))
    }

    @Test
    fun `metric prompt adds daily average and target for multi-day ranges`() {
        val prompt = metricPrompt(snapshot(RangeOption.SevenDays, steps = 35_000), MetricType.Steps, "35000 steps", Locale.US)
        assertTrue(prompt.contains("RANGE: 7 days (7 days)"))
        assertTrue(prompt.contains("DAILY AVERAGE: 5,000 steps"))
        assertTrue(prompt.contains("TARGET: 10,000 steps per day"))
    }

    @Test
    fun `metric prompt has no target for heart rate`() {
        val prompt = metricPrompt(snapshot(), MetricType.HeartRate, "72 bpm average", Locale.US)
        assertFalse(prompt.contains("TARGET:"))
        assertFalse(prompt.contains("AVERAGE:"))
        assertTrue(prompt.contains("lowest and highest readings"))
    }

    @Test
    fun `calorie prompts state that calories are burned`() {
        val prompt = metricPrompt(snapshot(RangeOption.SevenDays), MetricType.ActiveCalories, "384 kcal burned through activity", Locale.US)
        assertTrue(prompt.contains("DAILY AVERAGE: 54.9 kcal burned"))
        assertTrue(prompt.contains("not food eaten"))
        assertTrue(prompt.contains("never consumed"))
    }
}
