package dev.rrohaill.fitbrief.ui.metrics

import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.ui.MetricType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MetricCardsTest {
    @Test
    fun `no snapshot yields no cards`() {
        assertTrue(buildMetricCards(null).isEmpty())
    }

    @Test
    fun `full snapshot yields every metric in dashboard order`() {
        val cards = buildMetricCards(snapshot(), locale = Locale.US)
        assertEquals(
            listOf(
                MetricType.Steps, MetricType.HeartRate, MetricType.Sleep, MetricType.ActiveCalories,
                MetricType.Distance, MetricType.Exercise, MetricType.TotalCalories
            ),
            cards.map { it.type }
        )
    }

    @Test
    fun `metrics without data are omitted`() {
        val cards = buildMetricCards(
            snapshot(steps = 0, sleepMinutes = 0, averageHeartRateBpm = null, exerciseMinutes = 0),
            locale = Locale.US
        )
        assertEquals(
            listOf(MetricType.ActiveCalories, MetricType.Distance, MetricType.TotalCalories),
            cards.map { it.type }
        )
    }

    @Test
    fun `steps card reports progress against a daily goal for today`() {
        val steps = buildMetricCards(snapshot(steps = 8_432), locale = Locale.US).first()
        assertEquals("8,432", steps.value)
        assertEquals("84% of 10,000 goal", steps.footer)
        assertEquals(MetricCardGraph.StepsProgress, steps.graph)
        assertEquals(0.8432f, steps.progress!!, 0.0001f)
    }

    @Test
    fun `steps goal scales with the range length`() {
        val week = buildMetricCards(snapshot(RangeOption.Week, steps = 35_000), locale = Locale.US).first()
        assertEquals("50% of 70,000 goal", week.footer)
        val month = buildMetricCards(snapshot(RangeOption.Month, steps = 60_000), locale = Locale.US).first()
        assertEquals("75% of 80,000 goal", month.footer)
        assertEquals(0.75f, month.progress!!, 0.0001f)
    }

    @Test
    fun `values are formatted for display`() {
        val cards = buildMetricCards(snapshot(), locale = Locale.US).associateBy { it.type }
        assertEquals("7h 23m", cards.getValue(MetricType.Sleep).value)
        assertEquals("6.2 km", cards.getValue(MetricType.Distance).value)
        assertEquals("1,820", cards.getValue(MetricType.TotalCalories).value)
        assertEquals("42 min", cards.getValue(MetricType.Exercise).value)
        assertEquals("72", cards.getValue(MetricType.HeartRate).value)
        assertEquals(MetricCardGraph.HeartRateBars, cards.getValue(MetricType.HeartRate).graph)
    }

    @Test
    fun `hiding graphs strips graph and progress from every card`() {
        val cards = buildMetricCards(snapshot(), showGraphs = false, locale = Locale.US)
        assertTrue(cards.all { it.graph == MetricCardGraph.None })
        cards.forEach { assertNull(it.progress) }
    }
}
