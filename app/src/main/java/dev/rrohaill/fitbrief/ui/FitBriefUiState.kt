package dev.rrohaill.fitbrief.ui

import dev.rrohaill.fitbrief.data.DailyHeartRate
import dev.rrohaill.fitbrief.data.HealthConnectAvailability
import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.PermissionStatus
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.data.RefreshInterval
import dev.rrohaill.fitbrief.data.ThemeMode
import dev.rrohaill.fitbrief.data.TimelineEvent
import dev.rrohaill.fitbrief.summary.BackendProgress
import dev.rrohaill.fitbrief.summary.SummarizerBackend
import java.time.LocalDate

data class SettingsUiState(
    val dailySummaryEnabled: Boolean = true,
    val weeklyReportEnabled: Boolean = false,
    val dailySummaryTimeMinutes: Int = 8 * 60,
    val weeklyReportTimeMinutes: Int = 8 * 60,
    val weeklyReportDayOfWeek: Int = 1,
    val themeMode: ThemeMode = ThemeMode.System,
    val refreshInterval: RefreshInterval = RefreshInterval.FourHours,
    val notice: String? = null
)

data class MetricDetailUiState(
    val metric: MetricType? = null,
    val dayOffset: Int = 0,
    val drilldownDate: LocalDate? = null,
    val heartRateSamples: List<Double> = emptyList(),
    val dailyHeartRate: List<DailyHeartRate> = emptyList(),
    val insight: String? = null,
    val insightLoading: Boolean = false
)

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
    val summary: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val notificationsScheduled: Boolean = false,
    val settings: SettingsUiState = SettingsUiState(),
    val metricDetail: MetricDetailUiState = MetricDetailUiState()
)
