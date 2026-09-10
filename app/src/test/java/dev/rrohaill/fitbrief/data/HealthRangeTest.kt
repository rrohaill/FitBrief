package dev.rrohaill.fitbrief.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthRangeTest {
    private val zone = ZoneId.of("UTC")
    private val now = Instant.parse("2026-09-10T13:05:00Z")
    private fun day(d: String) = LocalDate.parse(d).atStartOfDay(zone).toInstant()

    @Test
    fun `month range starts on the first of the current month and ends now`() {
        val range = RangeOption.Month.toHealthRange(now, zone)
        assertEquals(day("2026-09-01"), range.start)
        assertEquals(now, range.end)
        assertEquals(10, range.dayCount(zone))
    }

    @Test
    fun `month offset covers the whole previous calendar month`() {
        val range = RangeOption.Month.toHealthRangeForOffset(1, now, zone)
        assertEquals(day("2026-08-01"), range.start)
        assertEquals(day("2026-09-01"), range.end)
        assertEquals(31, range.dayCount(zone))
        val february = RangeOption.Month.toHealthRangeForOffset(7, now, zone)
        assertEquals(day("2026-02-01"), february.start)
        assertEquals(day("2026-03-01"), february.end)
        assertEquals(28, february.dayCount(zone))
    }

    @Test
    fun `week and day offsets are unchanged`() {
        val week = RangeOption.SevenDays.toHealthRangeForOffset(1, now, zone)
        assertEquals(day("2026-08-28"), week.start)
        assertEquals(day("2026-09-04"), week.end)
        assertEquals(7, week.dayCount(zone))
        val yesterday = RangeOption.Today.toHealthRangeForOffset(1, now, zone)
        assertEquals(day("2026-09-09"), yesterday.start)
        assertEquals(day("2026-09-10"), yesterday.end)
        assertEquals(1, yesterday.dayCount(zone))
    }

    @Test
    fun `today counts as one day even before midnight passes`() {
        assertEquals(1, RangeOption.Today.toHealthRange(now, zone).dayCount(zone))
    }
}
