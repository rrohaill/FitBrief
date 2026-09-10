package dev.rrohaill.fitbrief.data

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
    ThirtyDays("30 days")
}

data class HealthRange(
    val option: RangeOption,
    val start: Instant,
    val end: Instant
) {
    val label: String = option.label
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
        RangeOption.ThirtyDays -> today.minusDays(29)
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
    val endDate = when (this) {
        RangeOption.Today -> today.minusDays(offset.toLong())
        RangeOption.SevenDays -> today.minusDays(offset * 7L)
        RangeOption.ThirtyDays -> today.minusDays(offset * 30L)
    }
    val startDate = when (this) {
        RangeOption.Today -> endDate
        RangeOption.SevenDays -> endDate.minusDays(6)
        RangeOption.ThirtyDays -> endDate.minusDays(29)
    }
    val end = if (offset == 0) clockNow else endDate.plusDays(1).atStartOfDay(zoneId).toInstant()
    return HealthRange(this, startDate.atStartOfDay(zoneId).toInstant(), end)
}

fun Duration.toWholeMinutes(): Long = toMinutes().coerceAtLeast(0)

val healthConnectProviderPackage: String = HEALTH_CONNECT_PROVIDER_PACKAGE
