package dev.rrohaill.fitbrief.data

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

private const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

enum class HealthConnectAvailability {
    Available,
    NotInstalled,
    UpdateRequired,
    Unsupported
}

enum class RangeOption(val label: String) {
    Today("Today"),
    SevenDays("7 days"),
    Month("Month")
}

data class HealthRange(
    val option: RangeOption,
    val start: Instant,
    val end: Instant
) {
    val label: String = option.label

    fun dayCount(zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val first = start.atZone(zoneId).toLocalDate()
        val last = end.minusSeconds(1).atZone(zoneId).toLocalDate()
        return (ChronoUnit.DAYS.between(first, last) + 1).toInt().coerceAtLeast(1)
    }
}

data class HealthSnapshot(
    val range: HealthRange,
    val steps: Long,
    val distanceMeters: Double,
    val activeCaloriesKcal: Double,
    val totalCaloriesKcal: Double,
    val exerciseMinutes: Long,
    val averageHeartRateBpm: Long?,
    val sleepMinutes: Long
) {
    val distanceKilometers: Double get() = distanceMeters / 1_000.0
}

data class TimelineEvent(
    val timestamp: Instant,
    val title: String,
    val detail: String,
    val icon: String,
    val endTimestamp: Instant? = null,
    val periodLabel: String? = null,
    val values: Map<String, Double> = emptyMap(),
    val samples: List<Double> = emptyList()
)

data class DailyHealthMetrics(
    val date: LocalDate,
    val steps: Long = 0,
    val distanceMeters: Double = 0.0,
    val activeCaloriesKcal: Double = 0.0,
    val totalCaloriesKcal: Double = 0.0,
    val exerciseMinutes: Long = 0,
    val sleepMinutes: Long = 0,
    val averageHeartRateBpm: Double? = null
)

data class PermissionStatus(
    val availability: HealthConnectAvailability,
    val granted: Boolean,
    val grantedCount: Int,
    val requiredCount: Int
)

fun RangeOption.toHealthRange(
    clockNow: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): HealthRange {
    val today = LocalDate.now(zoneId)
    val startDate = when (this) {
        RangeOption.Today -> today
        RangeOption.SevenDays -> today.minusDays(6)
        RangeOption.Month -> today.withDayOfMonth(1)
    }

    return HealthRange(
        option = this,
        start = startDate.atStartOfDay(zoneId).toInstant(),
        end = clockNow
    )
}

fun RangeOption.toHealthRangeForDate(
    date: LocalDate,
    clockNow: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): HealthRange {
    val start = date.atStartOfDay(zoneId).toInstant()
    val nextDay = date.plusDays(1).atStartOfDay(zoneId).toInstant()
    return HealthRange(
        option = this,
        start = start,
        end = if (date == LocalDate.now(zoneId)) clockNow else nextDay
    )
}

fun RangeOption.toHealthRangeForOffset(
    offset: Int,
    clockNow: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): HealthRange {
    val today = LocalDate.now(zoneId)
    val (startDate, endDate) = when (this) {
        RangeOption.Today -> today.minusDays(offset.toLong()).let { it to it }
        RangeOption.SevenDays -> today.minusDays(offset * 7L).let { it.minusDays(6) to it }
        RangeOption.Month -> today.minusMonths(offset.toLong()).let {
            it.withDayOfMonth(1) to it.with(TemporalAdjusters.lastDayOfMonth())
        }
    }
    val end = if (offset == 0) clockNow else endDate.plusDays(1).atStartOfDay(zoneId).toInstant()
    return HealthRange(this, startDate.atStartOfDay(zoneId).toInstant(), end)
}

fun Duration.toWholeMinutes(): Long = toMinutes().coerceAtLeast(0)

val healthConnectProviderPackage: String = HEALTH_CONNECT_PROVIDER_PACKAGE
