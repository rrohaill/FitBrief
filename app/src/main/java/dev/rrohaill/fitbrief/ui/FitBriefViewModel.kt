package dev.rrohaill.fitbrief.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import dev.rrohaill.fitbrief.AppContainer
import dev.rrohaill.fitbrief.data.DailyHealthMetrics
import dev.rrohaill.fitbrief.data.FitBriefPreferencesStore
import dev.rrohaill.fitbrief.data.HealthRange
import dev.rrohaill.fitbrief.data.HealthRepository
import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.data.RefreshInterval
import dev.rrohaill.fitbrief.data.ThemeMode
import dev.rrohaill.fitbrief.data.TimelineEvent
import dev.rrohaill.fitbrief.data.toHealthRange
import dev.rrohaill.fitbrief.data.toHealthRangeForDate
import dev.rrohaill.fitbrief.data.toHealthRangeForOffset
import dev.rrohaill.fitbrief.notifications.NotificationScheduler
import dev.rrohaill.fitbrief.summary.SummarizerBackend
import dev.rrohaill.fitbrief.summary.SummaryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class FitBriefViewModel(
    private val repository: HealthRepository,
    private val preferences: FitBriefPreferencesStore,
    private val summaryService: SummaryService,
    private val scheduler: NotificationScheduler
) : ViewModel() {
    private data class RangeData(
        val snapshot: HealthSnapshot,
        val timeline: List<TimelineEvent>,
        val summary: String,
        val activeBackend: SummarizerBackend
    )

    private val rangeCache = mutableMapOf<RangeOption, RangeData>()

    private val _uiState = MutableStateFlow(
        FitBriefUiState(
            selectedRange = preferences.selectedRange(),
            settings = SettingsUiState(
                themeMode = preferences.themeMode(),
                refreshInterval = preferences.refreshInterval(),
                dailySummaryTimeMinutes = preferences.dailySummaryTimeMinutes(),
                weeklyReportTimeMinutes = preferences.weeklyReportTimeMinutes(),
                weeklyReportDayOfWeek = preferences.weeklyReportDayOfWeek(),
                dailySummaryEnabled = preferences.dailySummaryEnabled(),
                weeklyReportEnabled = preferences.weeklyReportEnabled()
            )
        )
    )
    val uiState: StateFlow<FitBriefUiState> = _uiState
    val requiredPermissions: Set<String> = repository.permissions

    init {
        refreshPermissionStatus()
    }

    private inline fun updateSettings(transform: (SettingsUiState) -> SettingsUiState) =
        _uiState.update { it.copy(settings = transform(it.settings)) }

    private inline fun updateMetricDetail(transform: (MetricDetailUiState) -> MetricDetailUiState) =
        _uiState.update { it.copy(metricDetail = transform(it.metricDetail)) }

    fun refreshPermissionStatus() {
        viewModelScope.launch {
            runCatching { repository.permissionStatus() }
                .onSuccess { status -> _uiState.update { it.copy(permissionStatus = status, message = null) } }
                .onFailure { error -> _uiState.update { it.copy(message = error.message) } }
        }
    }

    fun onHealthPermissionsResult(grantedPermissions: Set<String>) {
        _uiState.update {
            it.copy(
                permissionStatus = it.permissionStatus.copy(
                    granted = grantedPermissions.isNotEmpty(),
                    grantedCount = requiredPermissions.count(grantedPermissions::contains),
                    requiredCount = requiredPermissions.size
                )
            )
        }
        refreshOnAppOpen()
    }

    fun selectRange(option: RangeOption) {
        preferences.setSelectedRange(option)
        val cached = rangeCache[option]
        _uiState.update {
            it.copy(
                selectedRange = option,
                snapshot = cached?.snapshot,
                timeline = cached?.timeline ?: emptyList(),
                summary = cached?.summary ?: "",
                activeBackend = cached?.activeBackend,
                backendProgress = null,
                message = null
            )
        }
        if (cached == null) refresh()
    }

    private fun restoreSelectedRange() {
        val cached = rangeCache[_uiState.value.selectedRange]
        if (cached == null) {
            refresh()
            return
        }
        _uiState.update {
            it.copy(snapshot = cached.snapshot, timeline = cached.timeline, summary = cached.summary, activeBackend = cached.activeBackend)
        }
    }

    fun selectBackend(backend: SummarizerBackend) {
        _uiState.update { it.copy(selectedBackend = backend, backendProgress = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            val current = _uiState.value
            val status = runCatching { repository.permissionStatus() }.getOrElse { error ->
                _uiState.update { it.copy(message = error.message, isLoading = false) }
                return@launch
            }

            _uiState.update { it.copy(permissionStatus = status) }
            if (!status.granted) {
                _uiState.update { it.copy(message = "Grant Health Connect permissions to generate a summary.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, message = null, backendProgress = null) }
            runCatching {
                val range = current.selectedRange.toHealthRange()
                val snapshot = repository.readSnapshot(range)
                val timeline = repository.readTimeline(range)
                _uiState.update { it.copy(snapshot = snapshot, timeline = timeline, message = null) }
                val summary = summaryService.summarize(
                    preferredBackend = current.selectedBackend,
                    snapshot = snapshot,
                    onProgress = { progress -> _uiState.update { it.copy(backendProgress = progress) } },
                    onBackendFallback = { note -> _uiState.update { it.copy(message = note) } }
                )
                val enrichedTimeline = runCatching {
                    summaryService.summarizeTimeline(summary.backend, snapshot, timeline)
                }.getOrDefault(timeline)
                Triple(snapshot, enrichedTimeline, summary)
            }.onSuccess { (snapshot, timeline, summary) ->
                rangeCache[current.selectedRange] = RangeData(snapshot, timeline, summary.text, summary.backend)
                _uiState.update {
                    it.copy(
                        snapshot = snapshot,
                        timeline = timeline,
                        summary = summary.text,
                        activeBackend = summary.backend,
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = error.message ?: "Unable to refresh summary.") }
            }
        }
    }

    fun refreshOnAppOpen() {
        if (!_uiState.value.isLoading) {
            refresh()
        }
    }

    fun openMetricDetail(metric: MetricType) {
        updateMetricDetail { MetricDetailUiState(metric = metric, insightLoading = true) }
        val range = _uiState.value.selectedRange.toHealthRangeForOffset(0)
        viewModelScope.launch {
            runCatching { readMetricDetailData(range, metric) }
                .onSuccess { (samples, daily) ->
                    updateMetricDetail { it.copy(heartRateSamples = samples, dailyMetrics = daily) }
                }
            generateMetricInsight()
        }
    }

    fun closeMetricDetail() {
        val current = _uiState.value
        if (current.metricDetail.drilldownDate == null) {
            updateMetricDetail { it.copy(metric = null) }
            if (current.metricDetail.dayOffset > 0) restoreSelectedRange()
            return
        }
        loadMetricPeriod(
            range = current.selectedRange.toHealthRangeForOffset(current.metricDetail.dayOffset),
            errorMessage = "Unable to return to this range."
        ) { it.copy(drilldownDate = null) }
    }

    fun openMetricDate(date: LocalDate) {
        loadMetricPeriod(
            range = RangeOption.Today.toHealthRangeForDate(date),
            errorMessage = "Unable to load that day."
        ) { it.copy(drilldownDate = date) }
    }

    fun navigateMetricDay(delta: Int) {
        val current = _uiState.value
        val drilldownDate = current.metricDetail.drilldownDate
        if (drilldownDate != null) {
            val nextDate = drilldownDate.minusDays(delta.toLong())
            if (nextDate.isAfter(LocalDate.now())) return
            loadMetricPeriod(
                range = RangeOption.Today.toHealthRangeForDate(nextDate),
                errorMessage = "Unable to load this day."
            ) { it.copy(drilldownDate = nextDate) }
            return
        }
        val nextOffset = (current.metricDetail.dayOffset + delta).coerceAtLeast(0)
        if (nextOffset == current.metricDetail.dayOffset) return
        loadMetricPeriod(
            range = current.selectedRange.toHealthRangeForOffset(nextOffset),
            errorMessage = "Unable to load this day."
        ) { it.copy(dayOffset = nextOffset) }
    }

    private fun loadMetricPeriod(
        range: HealthRange,
        errorMessage: String,
        prepare: (MetricDetailUiState) -> MetricDetailUiState
    ) {
        _uiState.update {
            it.copy(
                snapshot = null,
                timeline = emptyList(),
                isLoading = true,
                metricDetail = prepare(it.metricDetail).copy(insight = null, insightLoading = true)
            )
        }
        viewModelScope.launch {
            runCatching {
                val detailData = readMetricDetailData(range, _uiState.value.metricDetail.metric)
                Triple(repository.readSnapshot(range), repository.readTimeline(range), detailData)
            }.onSuccess { (snapshot, timeline, detailData) ->
                _uiState.update {
                    it.copy(
                        snapshot = snapshot,
                        timeline = timeline,
                        isLoading = false,
                        metricDetail = it.metricDetail.copy(heartRateSamples = detailData.first, dailyMetrics = detailData.second)
                    )
                }
                generateMetricInsight()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = error.message ?: errorMessage) }
            }
        }
    }

    private suspend fun readMetricDetailData(
        range: HealthRange,
        metric: MetricType?
    ): Pair<List<Double>, List<DailyHealthMetrics>> {
        val samples = if (metric == MetricType.HeartRate) repository.readHeartRateSamples(range) else emptyList()
        val daily = if (range.option == RangeOption.Today) emptyList() else repository.readDailyMetrics(range)
        return samples to daily
    }

    private fun generateMetricInsight() {
        viewModelScope.launch {
            val current = _uiState.value
            val snapshot = current.snapshot ?: return@launch
            val metric = current.metricDetail.metric ?: return@launch
            runCatching {
                summaryService.summarizeMetric(
                    preferredBackend = current.activeBackend ?: current.selectedBackend,
                    snapshot = snapshot,
                    metric = metric,
                    value = metricValue(snapshot, metric, current)
                )
            }.onSuccess { insight ->
                updateMetricDetail { it.copy(insight = insight, insightLoading = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        message = error.message ?: "Unable to generate metric insight.",
                        metricDetail = it.metricDetail.copy(insightLoading = false)
                    )
                }
            }
        }
    }

    private fun metricValue(snapshot: HealthSnapshot, metric: MetricType, state: FitBriefUiState): String = when (metric) {
        MetricType.Steps -> "${snapshot.steps} steps"
        MetricType.HeartRate -> {
            val samples = state.metricDetail.heartRateSamples.ifEmpty { state.timeline.flatMap { it.samples } }
            val average = "${snapshot.averageHeartRateBpm ?: "no average"} bpm average"
            if (samples.isEmpty()) average
            else "$average, lowest ${samples.min().toInt()} bpm, highest ${samples.max().toInt()} bpm"
        }
        MetricType.Sleep -> "${snapshot.sleepMinutes} minutes of sleep"
        MetricType.ActiveCalories -> "${snapshot.activeCaloriesKcal.toInt()} kcal burned through activity"
        MetricType.Distance -> "${snapshot.distanceKilometers} km"
        MetricType.Exercise -> "${snapshot.exerciseMinutes} minutes"
        MetricType.TotalCalories -> "${snapshot.totalCaloriesKcal.toInt()} kcal burned in total, including resting metabolism"
    }

    fun scheduleNotifications() {
        scheduler.schedule()
        _uiState.update { it.copy(notificationsScheduled = true, message = "Daily summary notification scheduled.") }
    }

    fun toggleDailySummary() {
        val enabled = !_uiState.value.settings.dailySummaryEnabled
        preferences.setDailySummaryEnabled(enabled)
        updateSettings { it.copy(dailySummaryEnabled = enabled) }
        if (enabled) scheduleNotifications()
    }

    fun toggleWeeklyReport() {
        val enabled = !_uiState.value.settings.weeklyReportEnabled
        preferences.setWeeklyReportEnabled(enabled)
        updateSettings { it.copy(weeklyReportEnabled = enabled) }
    }

    fun setDailySummaryTime(minutes: Int) {
        preferences.setDailySummaryTimeMinutes(minutes)
        updateSettings { it.copy(dailySummaryTimeMinutes = minutes) }
    }

    fun setWeeklyReportTime(minutes: Int) {
        preferences.setWeeklyReportTimeMinutes(minutes)
        updateSettings { it.copy(weeklyReportTimeMinutes = minutes) }
    }

    fun setWeeklyReportDay(dayOfWeek: Int) {
        preferences.setWeeklyReportDayOfWeek(dayOfWeek)
        updateSettings { it.copy(weeklyReportDayOfWeek = dayOfWeek) }
    }

    fun setThemeMode(mode: ThemeMode) {
        preferences.setThemeMode(mode)
        updateSettings { it.copy(themeMode = mode) }
    }

    fun setRefreshInterval(interval: RefreshInterval) {
        preferences.setRefreshInterval(interval)
        updateSettings { it.copy(refreshInterval = interval) }
        scheduler.schedule(interval.minutes.toLong())
    }

    fun showSettingsNotice(message: String) {
        updateSettings { it.copy(notice = message) }
    }

    fun dismissSettingsNotice() {
        updateSettings { it.copy(notice = null) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                FitBriefViewModel(
                    repository = container.healthRepository,
                    preferences = container.preferences,
                    summaryService = container.summaryService,
                    scheduler = container.notificationScheduler
                )
            }
        }
    }
}
