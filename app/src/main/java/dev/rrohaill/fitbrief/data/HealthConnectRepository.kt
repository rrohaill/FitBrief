package dev.rrohaill.fitbrief.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.records.Record
import java.time.Duration
import java.time.DayOfWeek
import java.time.Instant
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class HealthConnectRepository(private val context: Context) : HealthRepository {
    override val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND",
        "android.permission.health.READ_HEALTH_DATA_HISTORY"
    )

    fun availability(): HealthConnectAvailability {
        return when (HealthConnectClient.getSdkStatus(context, healthConnectProviderPackage)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UpdateRequired
            HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectAvailability.NotInstalled
            else -> HealthConnectAvailability.Unsupported
        }

    }

    override suspend fun permissionStatus(): PermissionStatus {
        val availability = availability()
        if (availability != HealthConnectAvailability.Available) {
            return PermissionStatus(availability, granted = false, grantedCount = 0, requiredCount = permissions.size)
        }

        val granted = client().permissionController.getGrantedPermissions()
        val grantedCount = permissions.count(granted::contains)
        return PermissionStatus(
            availability = availability,
            // A partial grant is useful: aggregate only the metrics the user allowed.
            granted = granted.isNotEmpty(),
            grantedCount = grantedCount,
            requiredCount = permissions.size
        )
    }

    override suspend fun readSnapshot(range: HealthRange): HealthSnapshot {
        val status = ensureReady()
        val granted = client().permissionController.getGrantedPermissions()
        val metrics = buildSet {
            if (permissionsFor(StepsRecord::class).let(granted::contains)) add(StepsRecord.COUNT_TOTAL)
            if (permissionsFor(DistanceRecord::class).let(granted::contains)) add(DistanceRecord.DISTANCE_TOTAL)
            if (permissionsFor(ActiveCaloriesBurnedRecord::class).let(granted::contains)) {
                add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            }

            if (permissionsFor(TotalCaloriesBurnedRecord::class).let(granted::contains)) {
                add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
            }
            if (permissionsFor(ExerciseSessionRecord::class).let(granted::contains)) {
                add(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL)
            }
            if (permissionsFor(HeartRateRecord::class).let(granted::contains)) add(HeartRateRecord.BPM_AVG)
            if (permissionsFor(SleepSessionRecord::class).let(granted::contains)) {
                add(SleepSessionRecord.SLEEP_DURATION_TOTAL)
            }
        }

        val aggregate = client().aggregate(
            AggregateRequest(
                metrics = metrics,
                // Aggregate Health Connect reads minimize data exposure: only totals/averages for the selected range are requested.
                timeRangeFilter = TimeRangeFilter.between(range.start, range.end)
            )
        )
        val sleepRange = HealthRange(
            option = range.option,
            start = range.start.minusSeconds(24 * 60 * 60),
            end = range.end
        )
        val sleepAggregate = if (permissionsFor(SleepSessionRecord::class) in granted) {
            client().aggregate(
                AggregateRequest(
                    metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(sleepRange.start, sleepRange.end)
                )
            )[SleepSessionRecord.SLEEP_DURATION_TOTAL]
        } else {
            null
        }

        return HealthSnapshot(
            range = range,
            steps = aggregate[StepsRecord.COUNT_TOTAL] ?: 0L,
            distanceMeters = aggregate[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0,
            activeCaloriesKcal = aggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0,
            totalCaloriesKcal = aggregate[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0,
            exerciseMinutes = aggregate[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toWholeMinutes() ?: 0,
            averageHeartRateBpm = aggregate[HeartRateRecord.BPM_AVG],
            sleepMinutes = (sleepAggregate ?: aggregate[SleepSessionRecord.SLEEP_DURATION_TOTAL])
                ?.toWholeMinutes() ?: 0
        )
    }

    override suspend fun readTimeline(range: HealthRange): List<TimelineEvent> {
        val granted = client().permissionController.getGrantedPermissions()
        val events = mutableListOf<TimelineEvent>()
        val sleepRange = HealthRange(
            option = range.option,
            start = range.start.minusSeconds(24 * 60 * 60),
            end = range.end
        )

        if (permissionsFor(ExerciseSessionRecord::class) in granted) {
            readAll<ExerciseSessionRecord>(range).forEach { record ->
                val minutes = Duration.between(record.startTime, record.endTime).toWholeMinutes()
                events += TimelineEvent(
                    record.startTime,
                    record.title ?: "Exercise session",
                    "You exercised for $minutes minutes. This was your main structured activity in this window.",
                    "✦",
                    record.endTime,
                    values = mapOf("exercise" to minutes.toDouble())
                )
            }

        }
        suspend fun <T : Record> activity(
            recordClass: kotlin.reflect.KClass<T>,
            records: suspend () -> List<T>,
            measure: (T) -> ActivityMeasurement
        ): List<ActivityMeasurement> =
            if (permissionsFor(recordClass) in granted) records().map(measure).filter { it.value > 0.0 } else emptyList()

        events += buildActivityWindows(
            steps = activity(StepsRecord::class, { readAll<StepsRecord>(range) }) {
                ActivityMeasurement(it.startTime, it.endTime, it.count.toDouble())
            },
            distance = activity(DistanceRecord::class, { readAll<DistanceRecord>(range) }) {
                ActivityMeasurement(it.startTime, it.endTime, it.distance.inMeters)
            },
            activeCalories = activity(ActiveCaloriesBurnedRecord::class, { readAll<ActiveCaloriesBurnedRecord>(range) }) {
                ActivityMeasurement(it.startTime, it.endTime, it.energy.inKilocalories)
            },
            totalCalories = activity(TotalCaloriesBurnedRecord::class, { readAll<TotalCaloriesBurnedRecord>(range) }) {
                ActivityMeasurement(it.startTime, it.endTime, it.energy.inKilocalories)
            }
        )
        if (permissionsFor(HeartRateRecord::class) in granted) {
            val samples = readAll<HeartRateRecord>(range).flatMap { it.samples }.sortedBy { it.time }
            var window = mutableListOf<HeartRateRecord.Sample>()
            val activityGap = ACTIVITY_GAP_MINUTES
            fun flushHeartRate() {
                if (window.isEmpty()) return
                val first = window.first()
                val last = window.last()
                val values = window.map { it.beatsPerMinute }
                val average = values.average().toLong()
                val minimum = values.minOrNull() ?: average
                val maximum = values.maxOrNull() ?: average
                val detail = if (maximum - minimum >= 15) {
                    "Your heart rate changed from $minimum to $maximum bpm, averaging $average bpm during this window."
                } else {
                    "Your heart rate stayed fairly steady around $average bpm during this window."
                }
                events += TimelineEvent(
                    first.time,
                    "Heart-rate window",
                    detail,
                    "♡",
                    last.time,
                    values = mapOf("heartRate" to average.toDouble()),
                    samples = values.map { it.toDouble() }
                )
                window = mutableListOf()
            }
            samples.forEach { sample ->
                val gap = window.lastOrNull()?.let { Duration.between(it.time, sample.time).toMinutes() } ?: 0
                if (window.isNotEmpty() && gap > activityGap) flushHeartRate()
                window += sample
            }
            flushHeartRate()
        }
        if (permissionsFor(SleepSessionRecord::class) in granted) {
            readAll<SleepSessionRecord>(sleepRange).forEach { record ->
                val minutes = Duration.between(record.startTime, record.endTime).toWholeMinutes()
                events += TimelineEvent(
                    record.startTime,
                    "Sleep session",
                    "You recorded $minutes minutes of sleep. This is the recovery window captured for this period.",
                    "◒",
                    record.endTime,
                    values = mapOf("sleep" to minutes.toDouble())
                )
            }
        }

        val sortedEvents = events.sortedByDescending(TimelineEvent::timestamp)
        return when (range.option) {
            RangeOption.Today -> sortedEvents.take(100)
            RangeOption.SevenDays -> collapseTimelineByPeriod(
                sortedEvents,
                periodOf = { it.atZone(ZoneId.systemDefault()).toLocalDate() },
                labelOf = { date -> date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")) }
            )
            RangeOption.Month -> collapseTimelineByPeriod(
                sortedEvents,
                periodOf = {
                    it.atZone(ZoneId.systemDefault()).toLocalDate()
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                },
                labelOf = { date -> "Week of ${date.format(DateTimeFormatter.ofPattern("MMM d"))}" }
            )
        }
    }

    override suspend fun readHeartRateSamples(range: HealthRange): List<Double> {
        val granted = client().permissionController.getGrantedPermissions()
        if (permissionsFor(HeartRateRecord::class) !in granted) return emptyList()
        val samples = readAll<HeartRateRecord>(range)
            .flatMap { record -> record.samples.map { it.beatsPerMinute.toDouble() } }
        if (samples.size <= 48) return samples
        val bucketSize = samples.size.toDouble() / 48.0
        return (0 until 48).map { bucket ->
            val start = (bucket * bucketSize).toInt()
            val end = ((bucket + 1) * bucketSize).toInt().coerceAtMost(samples.size)
            samples.subList(start, end.coerceAtLeast(start + 1)).average()
        }
    }

    override suspend fun readDailyHeartRate(range: HealthRange): List<DailyHeartRate> {
        val granted = client().permissionController.getGrantedPermissions()
        if (permissionsFor(HeartRateRecord::class) !in granted) return emptyList()
        val zone = ZoneId.systemDefault()
        val groups = client().aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(HeartRateRecord.BPM_AVG),
                timeRangeFilter = TimeRangeFilter.between(
                    range.start.atZone(zone).toLocalDateTime(),
                    range.end.atZone(zone).toLocalDateTime()
                ),
                timeRangeSlicer = Period.ofDays(1)
            )
        )
        return groups
            .mapNotNull { group ->
                group.result[HeartRateRecord.BPM_AVG]?.let { DailyHeartRate(group.startTime.toLocalDate(), it.toDouble()) }
            }
            .sortedBy(DailyHeartRate::date)
    }

    private fun collapseTimelineByPeriod(
        events: List<TimelineEvent>,
        periodOf: (Instant) -> java.time.LocalDate,
        labelOf: (java.time.LocalDate) -> String
    ): List<TimelineEvent> {
        val zone = ZoneId.systemDefault()
        return events.groupBy { periodOf(it.timestamp) }
            .toList()
            .sortedByDescending { it.first }
            .map { (period, periodEvents) ->
                val notableEvents = periodEvents.notable()
                val counts = notableEvents.groupingBy { it.title }.eachCount()
                    .entries.joinToString(", ") { "${it.key} (${it.value})" }
                val highlights = notableEvents.map { it.detail }.distinct().take(3)
                    .joinToString(" ")
                val metricValues = periodEvents.flatMap { it.values.entries }
                    .groupBy { it.key }
                    .mapValues { (key, entries) ->
                        if (key == "heartRate") entries.map { it.value }.average()
                        else entries.sumOf { it.value }
                    }
                TimelineEvent(
                    timestamp = period.atStartOfDay(zone).toInstant(),
                    title = labelOf(period),
                    detail = if (notableEvents.isEmpty()) "Only brief movement was recorded." else "$counts. $highlights",
                    icon = "◷",
                    periodLabel = labelOf(period),
                    values = metricValues
                )
            }
            .take(100)
    }

    private suspend fun ensureReady(): PermissionStatus {
        val status = permissionStatus()
        check(status.availability == HealthConnectAvailability.Available) {
            "Health Connect is not available: ${status.availability}"
        }
        check(status.grantedCount > 0) {
            "Grant at least one Health Connect read permission before reading health data."
        }
        return status
    }

    private fun client(): HealthConnectClient = HealthConnectClient.getOrCreate(context)

    private suspend inline fun <reified T : Record> readAll(range: HealthRange): List<T> {
        val records = mutableListOf<T>()
        var pageToken: String? = null
        do {
            val response = client().readRecords(
                ReadRecordsRequest<T>(
                    timeRangeFilter = TimeRangeFilter.between(range.start, range.end),
                    ascendingOrder = true,
                    pageToken = pageToken
                )
            )
            records += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return records
    }

    private fun <T : androidx.health.connect.client.records.Record> permissionsFor(
        recordClass: kotlin.reflect.KClass<T>
    ): String = HealthPermission.getReadPermission(recordClass)
}
