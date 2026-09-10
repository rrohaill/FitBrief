package dev.rrohaill.fitbrief.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.rrohaill.fitbrief.data.HealthConnectAvailability
import dev.rrohaill.fitbrief.data.HealthConnectRepository
import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.TimelineEvent
import dev.rrohaill.fitbrief.data.FitBriefPreferences
import dev.rrohaill.fitbrief.data.RefreshInterval
import dev.rrohaill.fitbrief.data.PermissionStatus
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.data.ThemeMode
import dev.rrohaill.fitbrief.data.toHealthRange
import dev.rrohaill.fitbrief.data.toHealthRangeForDate
import dev.rrohaill.fitbrief.data.toHealthRangeForOffset
import dev.rrohaill.fitbrief.notifications.FitBriefWorkScheduler
import dev.rrohaill.fitbrief.summary.BackendProgress
import dev.rrohaill.fitbrief.summary.SummarizerBackend
import dev.rrohaill.fitbrief.summary.SummarizerFactory
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
    val showSummaryDetail: Boolean = false,
    val selectedMetric: MetricType? = null,
    val metricDayOffset: Int = 0,
    val metricDrilldownDate: LocalDate? = null,
    val metricInsight: String? = null,
    val metricInsightLoading: Boolean = false,
    val showSettings: Boolean = false,
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

class FitBriefViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HealthConnectRepository(application.applicationContext)
    private val preferences = FitBriefPreferences(application.applicationContext)
    private val summarizerFactory = SummarizerFactory(application.applicationContext)

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

    fun generateSummary() {
        _uiState.update {
            it.copy(
                showSummaryDetail = true,
                snapshot = null,
                timeline = emptyList(),
                summary = "",
                activeBackend = null,
                backendProgress = null,
                message = null
            )
        }
        refresh(openDetail = true)
    }

    fun refresh(openDetail: Boolean = false) {
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
                val snapshot = repository.readSnapshot(current.selectedRange.toHealthRange())
                val timeline = repository.readTimeline(current.selectedRange.toHealthRange())
                _uiState.update {
                    it.copy(
                        snapshot = snapshot,
                        timeline = timeline,
                        message = null
                    )
                }
                val summary = summarizerFactory.summarize(
                    preferredBackend = current.selectedBackend,
                    snapshot = snapshot,
                    onProgress = { progress -> _uiState.update { it.copy(backendProgress = progress) } },
                    onBackendFallback = { note -> _uiState.update { it.copy(message = note) } }
                )
                val enrichedTimeline = runCatching {
                    summarizerFactory.summarizeTimeline(
                        backend = summary.backend,
                        snapshot = snapshot,
                        events = timeline
                    )
                }.getOrDefault(timeline)
                Triple(snapshot, enrichedTimeline, summary)
            }.onSuccess { (snapshot, timeline, summary) ->
                _uiState.update {
                    it.copy(
                        snapshot = snapshot,
                        timeline = timeline,
                        summary = summary.text,
                        activeBackend = summary.backend,
                        showSummaryDetail = openDetail,
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

    fun scheduleNotifications() {
        FitBriefWorkScheduler.schedule(getApplication<Application>().applicationContext)
        _uiState.update { it.copy(notificationsScheduled = true, message = "Daily summary notification scheduled.") }
    }

    fun closeSummaryDetail() {
        _uiState.update { it.copy(showSummaryDetail = false) }
    }

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
        if (current.metricDrilldownDate != null) {
            _uiState.update {
                it.copy(
                    metricDrilldownDate = null,
                    snapshot = null,
                    timeline = emptyList(),
                    isLoading = true,
                    metricInsight = null,
                    metricInsightLoading = true
                )
            }
            viewModelScope.launch {
                runCatching {
                    val range = current.selectedRange.toHealthRangeForOffset(current.metricDayOffset)
                        Triple(
                            repository.readSnapshot(range),
                            repository.readTimeline(range),
                            repository.readHeartRateSamples(range)
                        )
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
                    _uiState.update { it.copy(isLoading = false, message = error.message ?: "Unable to return to this range.") }
                }
            }
            return
        }
        _uiState.update { it.copy(selectedMetric = null) }
    }

    fun openMetricDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                metricDrilldownDate = date,
                snapshot = null,
                timeline = emptyList(),
                isLoading = true,
                metricInsight = null,
                metricInsightLoading = true
            )
        }
        viewModelScope.launch {
            runCatching {
                val range = RangeOption.Today.toHealthRangeForDate(date)
            Triple(
                repository.readSnapshot(range),
                repository.readTimeline(range),
                repository.readHeartRateSamples(range)
            )
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
                _uiState.update { it.copy(isLoading = false, message = error.message ?: "Unable to load that day.") }
            }
        }
    }

    fun navigateMetricDay(delta: Int) {
        val current = _uiState.value
        val nextOffset = (current.metricDayOffset + delta).coerceAtLeast(0)
        if (nextOffset == current.metricDayOffset) return
        _uiState.update {
            it.copy(
                metricDayOffset = nextOffset,
                snapshot = null,
                timeline = emptyList(),
                isLoading = true,
                metricInsight = null,
                metricInsightLoading = true
            )
        }
        viewModelScope.launch {
            runCatching {
                val range = current.selectedRange.toHealthRangeForOffset(nextOffset)
                repository.readSnapshot(range) to repository.readTimeline(range)
            }.onSuccess { (snapshot, timeline) ->
                _uiState.update { it.copy(snapshot = snapshot, timeline = timeline, isLoading = false) }
                generateMetricInsight()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = error.message ?: "Unable to load this day.") }
            }
        }
    }

    private fun generateMetricInsight() {
                viewModelScope.launch {
                    val current = _uiState.value
                    val snapshot = current.snapshot ?: return@launch
                    val metric = current.selectedMetric ?: return@launch
                    val value = metricValue(snapshot, metric)
                    runCatching {
                        summarizerFactory.summarizeMetric(
                            preferredBackend = current.activeBackend ?: current.selectedBackend,
                            snapshot = snapshot,
                            metric = metric,
                            value = value
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

    fun openSettings() {
        _uiState.update { it.copy(showSettings = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }

    fun toggleDailySummary() {
        val enabled = !_uiState.value.dailySummaryEnabled
        preferences.setDailySummaryEnabled(enabled)
        _uiState.update { it.copy(dailySummaryEnabled = enabled) }
        if (_uiState.value.dailySummaryEnabled) scheduleNotifications()
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

    fun showSettingsNotice(message: String) {
        _uiState.update { it.copy(settingsNotice = message) }
    }

    fun setThemeMode(mode: ThemeMode) {
        preferences.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setRefreshInterval(interval: RefreshInterval) {
        preferences.setRefreshInterval(interval)
        _uiState.update { it.copy(refreshInterval = interval) }
        FitBriefWorkScheduler.schedule(getApplication(), interval.minutes.toLong())
    }

    fun dismissSettingsNotice() {
        _uiState.update { it.copy(settingsNotice = null) }
    }
}
