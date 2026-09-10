package dev.rrohaill.fitbrief.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class HealthConnectRepository(private val context: Context) {
    val permissions: Set<String> = setOf(
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

    private fun adaptiveActivityGapMinutes(starts: List<Instant>, ends: List<Instant>): Long {
        if (starts.size < 2) return 1L
        val gaps = starts.zipWithNext()
            .mapIndexed { index, (current, next) ->
                Duration.between(maxOf(current, ends[index]), next).toMinutes().coerceAtLeast(0)
            }
            .filter { it > 0 }
            .sorted()
        if (gaps.isEmpty()) return 1L
        return (gaps[gaps.size / 2] * 2).coerceAtLeast(1L)
    }

    suspend fun permissionStatus(): PermissionStatus {
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

    suspend fun readSnapshot(range: HealthRange): HealthSnapshot {
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

    suspend fun readTimeline(range: HealthRange): List<TimelineEvent> {
        val granted = client().permissionController.getGrantedPermissions()
        val events = mutableListOf<TimelineEvent>()
        data class ActivityMeasurement(
            val start: Instant,
            val end: Instant,
            val value: Double
        )
        fun appendGroupedMeasurements(
            measurements: List<ActivityMeasurement>,
            title: String,
            icon: String,
            valueKey: String,
            detail: (Double, Long) -> String
        ) {
            var window: MutableList<ActivityMeasurement> = mutableListOf()
            val activityGap = adaptiveActivityGapMinutes(
                measurements.map { it.start },
                measurements.map { it.end }
            )
            fun flush() {
                if (window.isEmpty()) return
                val start = window.first().start
                val end = window.maxOf(ActivityMeasurement::end)
                val total = window.sumOf(ActivityMeasurement::value)
                val minutes = Duration.between(start, end).toWholeMinutes().coerceAtLeast(1)
                events += TimelineEvent(
                    start,
                    title,
                    detail(total, minutes),
                    icon,
                    end,
                    values = mapOf(valueKey to total)
                )
                window = mutableListOf()
            }
            measurements.sortedBy(ActivityMeasurement::start).forEach { measurement ->
                val gap = window.lastOrNull()?.let {
                    Duration.between(it.end, measurement.start).toMinutes()
                } ?: 0
                if (window.isNotEmpty() && gap > activityGap) flush()
                window += measurement
            }
            flush()
        }
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
        if (permissionsFor(StepsRecord::class) in granted) {
            val records = readAll<StepsRecord>(range).filter { it.count > 0 }
            var start: Instant? = null
            var end: Instant? = null
            var count = 0L
            val activityGap = adaptiveActivityGapMinutes(
                records.map { it.startTime },
                records.map { it.endTime }
            )
            fun flush() {
                val windowStart = start ?: return
                val windowEnd = end ?: windowStart
                val minutes = Duration.between(windowStart, windowEnd).toWholeMinutes().coerceAtLeast(1)
                events += TimelineEvent(
                    windowStart,
                    "Walking activity",
                    "You logged $count steps over about $minutes minutes. This looks like a sustained movement window rather than isolated readings.",
                    "♧",
                    windowEnd,
                    values = mapOf("steps" to count.toDouble())
                )
                start = null
                end = null
                count = 0
            }
            records.forEach { record ->
                val gap = end?.let { Duration.between(it, record.startTime).toMinutes() } ?: 0
                if (start != null && gap > activityGap) flush()
                if (start == null) start = record.startTime
                end = maxOf(end ?: record.endTime, record.endTime)
                count += record.count
            }
            flush()
        }
        if (permissionsFor(DistanceRecord::class) in granted) {
            val measurements = readAll<DistanceRecord>(range)
                .filter { it.distance.inMeters > 0.0 }
                .map { ActivityMeasurement(it.startTime, it.endTime, it.distance.inMeters) }
            appendGroupedMeasurements(
                measurements,
                "Walking distance",
                "⌁",
                "distance"
            ) { meters, minutes ->
                "You covered ${meters.toInt()} meters over about $minutes minutes in this activity window."
            }
        }
        if (permissionsFor(ActiveCaloriesBurnedRecord::class) in granted) {
            val measurements = readAll<ActiveCaloriesBurnedRecord>(range)
                .filter { it.energy.inKilocalories > 0.0 }
                .map { ActivityMeasurement(it.startTime, it.endTime, it.energy.inKilocalories) }
            appendGroupedMeasurements(
                measurements,
                "Active energy",
                "♨",
                "activeCalories"
            ) { calories, minutes ->
                "You burned ${calories.toInt()} active calories over about $minutes minutes in this activity window."
            }
        }
        if (permissionsFor(TotalCaloriesBurnedRecord::class) in granted) {
            val measurements = readAll<TotalCaloriesBurnedRecord>(range)
                .filter { it.energy.inKilocalories > 0.0 }
                .map { ActivityMeasurement(it.startTime, it.endTime, it.energy.inKilocalories) }
            appendGroupedMeasurements(
                measurements,
                "Total energy",
                "◈",
                "totalCalories"
            ) { calories, minutes ->
                "You recorded ${calories.toInt()} total calories over about $minutes minutes in this activity window."
            }
        }
        if (permissionsFor(HeartRateRecord::class) in granted) {
            val samples = readAll<HeartRateRecord>(range).flatMap { it.samples }.sortedBy { it.time }
            var window = mutableListOf<HeartRateRecord.Sample>()
            val activityGap = adaptiveActivityGapMinutes(
                samples.map { it.time },
                samples.map { it.time }
            )
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
            RangeOption.ThirtyDays -> collapseTimelineByPeriod(
                sortedEvents,
                periodOf = {
                    it.atZone(ZoneId.systemDefault()).toLocalDate()
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                },
                labelOf = { date -> "Week of ${date.format(DateTimeFormatter.ofPattern("MMM d"))}" }
            )
        }
    }

    suspend fun readHeartRateSamples(range: HealthRange): List<Double> {
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
                val counts = periodEvents.groupingBy { it.title }.eachCount()
                    .entries.joinToString(", ") { "${it.key} (${it.value})" }
                val highlights = periodEvents.map { it.detail }.distinct().take(3)
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
                    detail = "$counts. $highlights",
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
