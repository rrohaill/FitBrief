package dev.rrohaill.fitbrief.ui

import dev.rrohaill.fitbrief.data.DailyHeartRate
import dev.rrohaill.fitbrief.data.FitBriefPreferencesStore
import dev.rrohaill.fitbrief.data.HealthConnectAvailability
import dev.rrohaill.fitbrief.data.HealthRange
import dev.rrohaill.fitbrief.data.HealthRepository
import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.PermissionStatus
import dev.rrohaill.fitbrief.data.RefreshInterval
import dev.rrohaill.fitbrief.data.ThemeMode
import dev.rrohaill.fitbrief.data.TimelineEvent
import dev.rrohaill.fitbrief.notifications.NotificationScheduler
import dev.rrohaill.fitbrief.summary.BackendProgress
import dev.rrohaill.fitbrief.summary.FitBriefSummary
import dev.rrohaill.fitbrief.summary.SummarizerBackend
import dev.rrohaill.fitbrief.summary.SummaryService

class FakeHealthRepository : HealthRepository {
    override val permissions: Set<String> = setOf("steps", "heart")
    var granted = true
    var failWith: Throwable? = null
    val snapshotRanges = mutableListOf<HealthRange>()
    val heartRateRanges = mutableListOf<HealthRange>()
    var snapshotFor: (HealthRange) -> HealthSnapshot = { range ->
        HealthSnapshot(range, 5_000, 4_000.0, 300.0, 1_900.0, 30, 70, 420)
    }
    var timeline: List<TimelineEvent> = emptyList()
    var heartRateSamples: List<Double> = listOf(60.0, 90.0)

    override suspend fun permissionStatus() = PermissionStatus(
        HealthConnectAvailability.Available, granted, if (granted) permissions.size else 0, permissions.size
    )

    override suspend fun readSnapshot(range: HealthRange): HealthSnapshot {
        failWith?.let { throw it }
        snapshotRanges += range
        return snapshotFor(range)
    }

    override suspend fun readTimeline(range: HealthRange): List<TimelineEvent> = timeline

    override suspend fun readHeartRateSamples(range: HealthRange): List<Double> {
        heartRateRanges += range
        return heartRateSamples
    }

    var dailyHeartRate: List<DailyHeartRate> = emptyList()
    override suspend fun readDailyHeartRate(range: HealthRange): List<DailyHeartRate> = dailyHeartRate
}

class FakePreferences : FitBriefPreferencesStore {
    var theme = ThemeMode.System
    var interval = RefreshInterval.FourHours
    var dailyTime = 8 * 60
    var weeklyTime = 8 * 60
    var weeklyDay = 1
    var daily = true
    var weekly = false

    override fun themeMode() = theme
    override fun setThemeMode(mode: ThemeMode) { theme = mode }
    override fun refreshInterval() = interval
    override fun setRefreshInterval(interval: RefreshInterval) { this.interval = interval }
    override fun dailySummaryTimeMinutes() = dailyTime
    override fun setDailySummaryTimeMinutes(minutes: Int) { dailyTime = minutes }
    override fun weeklyReportTimeMinutes() = weeklyTime
    override fun setWeeklyReportTimeMinutes(minutes: Int) { weeklyTime = minutes }
    override fun weeklyReportDayOfWeek() = weeklyDay
    override fun setWeeklyReportDayOfWeek(dayOfWeek: Int) { weeklyDay = dayOfWeek }
    override fun dailySummaryEnabled() = daily
    override fun setDailySummaryEnabled(enabled: Boolean) { daily = enabled }
    override fun weeklyReportEnabled() = weekly
    override fun setWeeklyReportEnabled(enabled: Boolean) { weekly = enabled }
}

class FakeSummaryService : SummaryService {
    var backend = SummarizerBackend.Template
    val metricRequests = mutableListOf<Pair<MetricType, String>>()

    override suspend fun summarize(
        preferredBackend: SummarizerBackend,
        snapshot: HealthSnapshot,
        onProgress: (BackendProgress) -> Unit,
        onBackendFallback: (String) -> Unit
    ) = FitBriefSummary(text = "Summary of ${snapshot.steps} steps", backend = backend)

    override suspend fun summarizeTimeline(
        backend: SummarizerBackend,
        snapshot: HealthSnapshot,
        events: List<TimelineEvent>
    ) = events

    override suspend fun summarizeMetric(
        preferredBackend: SummarizerBackend,
        snapshot: HealthSnapshot,
        metric: MetricType,
        value: String
    ): String {
        metricRequests += metric to value
        return "Insight: $value"
    }
}

class FakeScheduler : NotificationScheduler {
    val intervals = mutableListOf<Long>()
    override fun schedule(intervalMinutes: Long) { intervals += intervalMinutes }
}
