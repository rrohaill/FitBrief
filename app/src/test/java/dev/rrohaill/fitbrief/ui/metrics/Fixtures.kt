package dev.rrohaill.fitbrief.ui.metrics

import dev.rrohaill.fitbrief.data.HealthRange
import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.data.TimelineEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

internal val zone: ZoneId = ZoneId.of("UTC")

internal fun snapshot(
    option: RangeOption = RangeOption.Today,
    steps: Long = 8_432,
    distanceMeters: Double = 6_240.0,
    activeCaloriesKcal: Double = 384.0,
    totalCaloriesKcal: Double = 1_820.0,
    exerciseMinutes: Long = 42,
    averageHeartRateBpm: Long? = 72,
    sleepMinutes: Long = 443
) = HealthSnapshot(
    range = HealthRange(
        option,
        Instant.parse(
            when (option) {
                RangeOption.Today -> "2026-09-08T00:00:00Z"
                RangeOption.Week -> "2026-09-02T00:00:00Z"
                RangeOption.Month -> "2026-09-01T00:00:00Z"
            }
        ),
        Instant.parse("2026-09-08T12:00:00Z")
    ),
    steps = steps,
    distanceMeters = distanceMeters,
    activeCaloriesKcal = activeCaloriesKcal,
    totalCaloriesKcal = totalCaloriesKcal,
    exerciseMinutes = exerciseMinutes,
    averageHeartRateBpm = averageHeartRateBpm,
    sleepMinutes = sleepMinutes
)

internal fun event(
    date: LocalDate,
    time: LocalTime = LocalTime.NOON,
    values: Map<String, Double> = emptyMap(),
    samples: List<Double> = emptyList()
) = TimelineEvent(
    timestamp = date.atTime(time).atZone(zone).toInstant(),
    title = "event",
    detail = "",
    icon = "",
    values = values,
    samples = samples
)
