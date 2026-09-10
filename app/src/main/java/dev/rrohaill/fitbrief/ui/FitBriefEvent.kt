package dev.rrohaill.fitbrief.ui

import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.data.RefreshInterval
import dev.rrohaill.fitbrief.data.ThemeMode
import dev.rrohaill.fitbrief.summary.SummarizerBackend
import java.time.LocalDate

sealed interface FitBriefEvent {
    data object RequestHealthPermissions : FitBriefEvent
    data object OpenHealthConnect : FitBriefEvent
    data object Refresh : FitBriefEvent
    data object ShareSummary : FitBriefEvent
    data object ToggleDailySummary : FitBriefEvent
    data object ToggleWeeklyReport : FitBriefEvent
    data class SetDailySummaryTime(val minutes: Int) : FitBriefEvent
    data class SetWeeklyReportTime(val minutes: Int) : FitBriefEvent
    data class SetWeeklyReportDay(val day: Int) : FitBriefEvent
    data class ShowSettingsNotice(val action: String) : FitBriefEvent
    data object DismissSettingsNotice : FitBriefEvent
    data class ChangeTheme(val mode: ThemeMode) : FitBriefEvent
    data class ChangeRefreshInterval(val interval: RefreshInterval) : FitBriefEvent
    data class SelectRange(val range: RangeOption) : FitBriefEvent
    data object ScheduleNotifications : FitBriefEvent
    data class OpenMetric(val metric: MetricType) : FitBriefEvent
    data object CloseMetric : FitBriefEvent
    data class NavigateMetricDay(val delta: Int) : FitBriefEvent
    data class OpenMetricDate(val date: LocalDate) : FitBriefEvent
    data class SelectBackend(val backend: SummarizerBackend) : FitBriefEvent
}
