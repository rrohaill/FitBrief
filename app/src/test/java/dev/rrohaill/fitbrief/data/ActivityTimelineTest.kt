package dev.rrohaill.fitbrief.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ActivityTimelineTest {
    private val t0 = Instant.parse("2026-09-10T11:48:00Z")
    private fun at(minutes: Long) = t0.plusSeconds(minutes * 60)
    private fun m(startMin: Long, endMin: Long, value: Double) = ActivityMeasurement(at(startMin), at(endMin), value)

    @Test
    fun `bursts within the idle gap share a window and longer pauses split them`() {
        val steps = listOf(m(0, 1, 20.0), m(1, 2, 18.0), m(8, 9, 26.0), m(20, 21, 5.0), m(48, 49, 11.0))
        val windows = groupIntoWindows(steps)
        assertEquals(listOf(3, 1, 1), windows.map { it.size })
        assertEquals(at(20), windows[1].single().start)
    }

    @Test
    fun `steps and distance for the same period become one event with both values`() {
        val events = buildActivityWindows(
            steps = listOf(m(0, 2, 38.0), m(8, 9, 26.0)),
            distance = listOf(m(1, 2, 8.0), m(8, 9, 19.0)),
            activeCalories = listOf(m(0, 2, 3.0), m(8, 9, 2.0), m(60, 61, 50.0)),
            totalCalories = listOf(m(0, 15, 40.0), m(15, 30, 20.0))
        )
        assertEquals(1, events.size)
        val event = events.single()
        assertEquals(at(0), event.timestamp)
        assertEquals(at(9), event.endTimestamp)
        assertEquals(64.0, event.values["steps"])
        assertEquals(27.0, event.values["distance"])
        assertEquals(5.0, event.values["activeCalories"])
        assertEquals(40.0, event.values["totalCalories"])
        assertEquals("You took 64 steps and covered 27 meters over about 9 minutes, burning 5 active kcal.", event.detail)
    }

    @Test
    fun `separate activity periods produce separate events`() {
        val events = buildActivityWindows(
            steps = listOf(m(0, 2, 38.0), m(30, 31, 11.0)),
            distance = emptyList(),
            activeCalories = emptyList(),
            totalCalories = emptyList()
        )
        assertEquals(listOf(at(0), at(30)), events.map { it.timestamp })
        assertEquals("You took 38 steps over about 2 minutes.", events[0].detail)
        assertTrue(events.all { "distance" !in it.values })
    }

    @Test
    fun `continuous calorie records do not create windows on their own`() {
        val events = buildActivityWindows(
            steps = emptyList(),
            distance = emptyList(),
            activeCalories = listOf(m(0, 15, 3.0)),
            totalCalories = listOf(m(0, 15, 40.0), m(15, 30, 40.0))
        )
        assertTrue(events.isEmpty())
    }

    private fun window(minutes: Long, values: Map<String, Double>) =
        TimelineEvent(t0, "Walking activity", "", "♧", at(minutes), values = values)

    @Test
    fun `short or small walking windows are trivial and filtered from notable`() {
        val trivial = listOf(
            window(1, mapOf("steps" to 11.0)),
            window(25, mapOf("steps" to 120.0, "distance" to 80.0)),
            window(5, mapOf("heartRate" to 70.0))
        )
        val notable = listOf(
            window(25, mapOf("steps" to 1_800.0)),
            window(15, mapOf("heartRate" to 70.0)),
            window(3, mapOf("exercise" to 3.0)),
            window(3, mapOf("sleep" to 3.0))
        )
        assertTrue(trivial.all { it.isTrivial() })
        assertTrue(notable.none { it.isTrivial() })
        assertEquals(notable, (trivial + notable).notable())
    }
}
