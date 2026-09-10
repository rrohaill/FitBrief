package dev.rrohaill.fitbrief.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import dev.rrohaill.fitbrief.AppContainer
import dev.rrohaill.fitbrief.data.FitBriefPreferencesStore
import dev.rrohaill.fitbrief.data.HealthConnectAvailability
import dev.rrohaill.fitbrief.data.HealthRange
import dev.rrohaill.fitbrief.data.HealthRepository
import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.PermissionStatus
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.data.RefreshInterval
import dev.rrohaill.fitbrief.data.ThemeMode
import dev.rrohaill.fitbrief.data.TimelineEvent
import dev.rrohaill.fitbrief.data.toHealthRange
import dev.rrohaill.fitbrief.data.toHealthRangeForDate
import dev.rrohaill.fitbrief.data.toHealthRangeForOffset
import dev.rrohaill.fitbrief.notifications.NotificationScheduler
import dev.rrohaill.fitbrief.summary.BackendProgress
import dev.rrohaill.fitbrief.summary.SummarizerBackend
import dev.rrohaill.fitbrief.summary.SummaryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FitBriefUiState(
    val permissionStatus: PermissionStatus = PermissionStatus(
        availability = HealthConnectAvailability.Unsupported,
        granted = false,
        grantedCount = 0,
        requiredCount = 0
    ),
    val selectedRange: RangeOption = RangeOption.Today,
    val selectedBackend: SummarizerBackend = SummarizerBackend.MlKitPrompt,
    val activeBackend: SummarizerBackend? = null,
    val backendProgress: BackendProgress? = null,
    val snapshot: HealthSnapshot? = null,
    val timeline: List<TimelineEvent> = emptyList(),
    val metricHeartRateSamples: List<Double> = emptyList(),
    val summary: String = "",
    val selectedMetric: MetricType? = null,
    val metricDayOffset: Int = 0,
    val metricDrilldownDate: LocalDate? = null,
    val metricInsight: String? = null,
    val metricInsightLoading: Boolean = false,
    val dailySummaryEnabled: Boolean = true,
    val weeklyReportEnabled: Boolean = false,
    val dailySummaryTimeMinutes: Int = 8 * 60,
    val weeklyReportTimeMinutes: Int = 8 * 60,
    val weeklyReportDayOfWeek: Int = 1,
    val themeMode: ThemeMode = ThemeMode.System,
    val refreshInterval: RefreshInterval = RefreshInterval.FourHours,
    val settingsNotice: String? = null,
    val message: String? = null,
    val isLoading: Boolean = false,
    val notificationsScheduled: Boolean = false
)

class FitBriefViewModel(
    private val repository: HealthRepository,
    private val preferences: FitBriefPreferencesStore,
    private val summaryService: SummaryService,
    private val scheduler: NotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FitBriefUiState(
            themeMode = preferences.themeMode(),
            refreshInterval = preferences.refreshInterval(),
            dailySummaryTimeMinutes = preferences.dailySummaryTimeMinutes(),
            weeklyReportTimeMinutes = preferences.weeklyReportTimeMinutes(),
            weeklyReportDayOfWeek = preferences.weeklyReportDayOfWeek(),
            dailySummaryEnabled = preferences.dailySummaryEnabled(),
            weeklyReportEnabled = preferences.weeklyReportEnabled()
        )
    )
    val uiState: StateFlow<FitBriefUiState> = _uiState
    val requiredPermissions: Set<String> = repository.permissions

    init {
        refreshPermissionStatus()
    }

    // region Permissions

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

    // endregion

    // region Dashboard summary

    fun selectRange(option: RangeOption) {
        _uiState.update {
            it.copy(
                selectedRange = option,
                snapshot = null,
                timeline = emptyList(),
                summary = "",
                activeBackend = null,
                backendProgress = null,
                message = null
            )
        }
        refresh()
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

    /** Plain-text version of the current summary for the system share sheet. */
    fun summaryShareText(): String = buildSummaryShareText(_uiState.value)

    // endregion

    // region Metric detail

    fun openMetricDetail(metric: MetricType) {
        _uiState.update {
            it.copy(
                selectedMetric = metric,
                metricDayOffset = 0,
                metricDrilldownDate = null,
                metricInsight = null,
                metricInsightLoading = true,
                metricHeartRateSamples = emptyList()
            )
        }
        if (metric == MetricType.HeartRate) {
            val range = _uiState.value.selectedRange.toHealthRangeForOffset(0)
            viewModelScope.launch {
                runCatching { repository.readHeartRateSamples(range) }
                    .onSuccess { samples -> _uiState.update { it.copy(metricHeartRateSamples = samples) } }
            }
        }
        generateMetricInsight()
    }

    fun closeMetricDetail() {
        val current = _uiState.value
        if (current.metricDrilldownDate == null) {
            _uiState.update { it.copy(selectedMetric = null) }
            return
        }
        // Leaving a drilled-down day returns to the period that was being browsed.
        loadMetricPeriod(
            range = current.selectedRange.toHealthRangeForOffset(current.metricDayOffset),
            errorMessage = "Unable to return to this range."
        ) { it.copy(metricDrilldownDate = null) }
    }

    fun openMetricDate(date: LocalDate) {
        loadMetricPeriod(
            range = RangeOption.Today.toHealthRangeForDate(date),
            errorMessage = "Unable to load that day."
        ) { it.copy(metricDrilldownDate = date) }
    }

    fun navigateMetricDay(delta: Int) {
        val current = _uiState.value
        val nextOffset = (current.metricDayOffset + delta).coerceAtLeast(0)
        if (nextOffset == current.metricDayOffset) return
        loadMetricPeriod(
            range = current.selectedRange.toHealthRangeForOffset(nextOffset),
            errorMessage = "Unable to load this day."
        ) { it.copy(metricDayOffset = nextOffset) }
    }

    /**
     * Replaces the snapshot and timeline with the data for [range], then regenerates the metric insight.
     * [prepare] applies the navigation change (offset or drill-down date) to the state before loading.
     */
    private fun loadMetricPeriod(
        range: HealthRange,
        errorMessage: String,
        prepare: (FitBriefUiState) -> FitBriefUiState
    ) {
        _uiState.update {
            prepare(it).copy(
                snapshot = null,
                timeline = emptyList(),
                isLoading = true,
                metricInsight = null,
                metricInsightLoading = true
            )
        }
        viewModelScope.launch {
            runCatching {
                val heartRateSamples = if (_uiState.value.selectedMetric == MetricType.HeartRate) {
                    repository.readHeartRateSamples(range)
                } else {
                    emptyList()
                }
                Triple(repository.readSnapshot(range), repository.readTimeline(range), heartRateSamples)
            }.onSuccess { (snapshot, timeline, heartRateSamples) ->
                _uiState.update {
                    it.copy(
                        snapshot = snapshot,
                        timeline = timeline,
                        metricHeartRateSamples = heartRateSamples,
                        isLoading = false
                    )
                }
                generateMetricInsight()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = error.message ?: errorMessage) }
            }
        }
    }

    private fun generateMetricInsight() {
        viewModelScope.launch {
            val current = _uiState.value
            val snapshot = current.snapshot ?: return@launch
            val metric = current.selectedMetric ?: return@launch
            runCatching {
                summaryService.summarizeMetric(
                    preferredBackend = current.activeBackend ?: current.selectedBackend,
                    snapshot = snapshot,
                    metric = metric,
                    value = metricValue(snapshot, metric)
                )
            }.onSuccess { insight ->
                _uiState.update { it.copy(metricInsight = insight, metricInsightLoading = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        metricInsightLoading = false,
                        message = error.message ?: "Unable to generate metric insight."
                    )
                }
            }
        }
    }

    private fun metricValue(snapshot: HealthSnapshot, metric: MetricType): String = when (metric) {
        MetricType.Steps -> "${snapshot.steps} steps"
        MetricType.HeartRate -> "${snapshot.averageHeartRateBpm ?: "no average"} bpm average"
        MetricType.Sleep -> "${snapshot.sleepMinutes} minutes of sleep"
        MetricType.ActiveCalories -> "${snapshot.activeCaloriesKcal} active kcal"
        MetricType.Distance -> "${snapshot.distanceKilometers} km"
        MetricType.Exercise -> "${snapshot.exerciseMinutes} minutes"
        MetricType.TotalCalories -> "${snapshot.totalCaloriesKcal} total kcal"
    }

    // endregion

    // region Settings & notifications

    fun scheduleNotifications() {
        scheduler.schedule()
        _uiState.update { it.copy(notificationsScheduled = true, message = "Daily summary notification scheduled.") }
    }

    fun toggleDailySummary() {
        val enabled = !_uiState.value.dailySummaryEnabled
        preferences.setDailySummaryEnabled(enabled)
        _uiState.update { it.copy(dailySummaryEnabled = enabled) }
        if (enabled) scheduleNotifications()
    }

    fun toggleWeeklyReport() {
        val enabled = !_uiState.value.weeklyReportEnabled
        preferences.setWeeklyReportEnabled(enabled)
        _uiState.update { it.copy(weeklyReportEnabled = enabled) }
    }

    fun setDailySummaryTime(minutes: Int) {
        preferences.setDailySummaryTimeMinutes(minutes)
        _uiState.update { it.copy(dailySummaryTimeMinutes = minutes) }
    }

    fun setWeeklyReportTime(minutes: Int) {
        preferences.setWeeklyReportTimeMinutes(minutes)
        _uiState.update { it.copy(weeklyReportTimeMinutes = minutes) }
    }

    fun setWeeklyReportDay(dayOfWeek: Int) {
        preferences.setWeeklyReportDayOfWeek(dayOfWeek)
        _uiState.update { it.copy(weeklyReportDayOfWeek = dayOfWeek) }
    }

    fun setThemeMode(mode: ThemeMode) {
        preferences.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setRefreshInterval(interval: RefreshInterval) {
        preferences.setRefreshInterval(interval)
        _uiState.update { it.copy(refreshInterval = interval) }
        scheduler.schedule(interval.minutes.toLong())
    }

    fun showSettingsNotice(message: String) {
        _uiState.update { it.copy(settingsNotice = message) }
    }

    fun dismissSettingsNotice() {
        _uiState.update { it.copy(settingsNotice = null) }
    }

    // endregion

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
