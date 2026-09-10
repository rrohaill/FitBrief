package dev.rrohaill.fitbrief.ui.metrics

import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.ui.MetricType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class MetricDetailHeaderTest {
    private val today = LocalDate.of(2026, 9, 10)

    private fun header(
        metric: MetricType = MetricType.Steps,
        range: RangeOption = RangeOption.Today,
        offset: Int = 0,
        drilldown: LocalDate? = null,
        samples: List<Double> = emptyList()
    ) = buildMetricDetailHeader(
        metric = metric,
        snapshot = snapshot(range),
        timeline = emptyList(),
        heartRateSamples = samples,
        selectedRange = range,
        dayOffset = offset,
        drilldownDate = drilldown,
        today = today,
        locale = Locale.US
    )

    @Test
    fun `period label for today is the current date`() {
        assertEquals("September 10, 2026", header().periodLabel)
    }

    @Test
    fun `period label moves back one day per offset for today range`() {
        assertEquals("September 8, 2026", header(offset = 2).periodLabel)
    }

    @Test
    fun `period label for a week and month range names the period end`() {
        assertEquals("Week ending Sep 3", header(range = RangeOption.SevenDays, offset = 1).periodLabel)
        assertEquals("Month ending Aug 11", header(range = RangeOption.ThirtyDays, offset = 1).periodLabel)
    }

    @Test
    fun `drilldown date overrides the period label`() {
        assertEquals(
            "Monday, September 7, 2026",
            header(range = RangeOption.SevenDays, drilldown = LocalDate.of(2026, 9, 7)).periodLabel
        )
    }

    @Test
    fun `heart rate headline uses sample extremes and zone`() {
        val h = header(metric = MetricType.HeartRate, samples = listOf(58.0, 121.0, 97.0))
        assertEquals("58–121 bpm", h.value)
        assertEquals(58, h.heartLow)
        assertEquals(121, h.heartHigh)
        assertEquals("Vigorous", h.highestHeartRateZone)
    }

    @Test
    fun `heart rate without samples falls back to the average`() {
        val h = header(metric = MetricType.HeartRate)
        assertEquals("72–72 bpm", h.value)
        assertEquals("Moderate", h.highestHeartRateZone)
    }

    @Test
    fun `headline values are formatted per metric`() {
        assertEquals("8432", header(MetricType.Steps).value)
        assertEquals("7h 23m", header(MetricType.Sleep).value)
        assertEquals("384 kcal", header(MetricType.ActiveCalories).value)
        assertEquals("6.2 km", header(MetricType.Distance).value)
        assertEquals("42 min", header(MetricType.Exercise).value)
        assertEquals("1820 kcal", header(MetricType.TotalCalories).value)
    }

    @Test
    fun `heart rate zones follow the bpm thresholds`() {
        assertEquals("Light", heartRateZone(70))
        assertEquals("Moderate", heartRateZone(71))
        assertEquals("Vigorous", heartRateZone(110))
        assertEquals("Peak", heartRateZone(135))
    }
}
