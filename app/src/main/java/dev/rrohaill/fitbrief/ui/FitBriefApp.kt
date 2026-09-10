package dev.rrohaill.fitbrief.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rrohaill.fitbrief.data.ThemeMode
import dev.rrohaill.fitbrief.ui.theme.FitBriefTheme

@Composable
fun FitBriefApp(
    viewModel: FitBriefViewModel,
    onRequestHealthPermissions: () -> Unit,
    onOpenHealthConnect: () -> Unit,
    onScheduleNotifications: () -> Unit
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val onEvent: (FitBriefEvent) -> Unit = { event ->
        when (event) {
            FitBriefEvent.RequestHealthPermissions -> onRequestHealthPermissions()
            FitBriefEvent.OpenHealthConnect -> onOpenHealthConnect()
            FitBriefEvent.Refresh -> viewModel.refresh()
            FitBriefEvent.ToggleDailySummary -> viewModel.toggleDailySummary()
            FitBriefEvent.ToggleWeeklyReport -> viewModel.toggleWeeklyReport()
            is FitBriefEvent.SetDailySummaryTime -> viewModel.setDailySummaryTime(event.minutes)
            is FitBriefEvent.SetWeeklyReportTime -> viewModel.setWeeklyReportTime(event.minutes)
            is FitBriefEvent.SetWeeklyReportDay -> viewModel.setWeeklyReportDay(event.day)
            is FitBriefEvent.ShowSettingsNotice -> viewModel.showSettingsNotice(event.action)
            FitBriefEvent.DismissSettingsNotice -> viewModel.dismissSettingsNotice()
            is FitBriefEvent.ChangeTheme -> viewModel.setThemeMode(event.mode)
            is FitBriefEvent.ChangeRefreshInterval -> viewModel.setRefreshInterval(event.interval)
            is FitBriefEvent.SelectRange -> viewModel.selectRange(event.range)
            FitBriefEvent.ScheduleNotifications -> onScheduleNotifications()
            is FitBriefEvent.OpenMetric -> viewModel.openMetricDetail(event.metric)
            FitBriefEvent.CloseMetric -> viewModel.closeMetricDetail()
            is FitBriefEvent.NavigateMetricDay -> viewModel.navigateMetricDay(event.delta)
            is FitBriefEvent.OpenMetricDate -> viewModel.openMetricDate(event.date)
            is FitBriefEvent.SelectBackend -> viewModel.selectBackend(event.backend)
        }
    }
    FitBriefTheme(
        darkTheme = when (state.settings.themeMode) {
            ThemeMode.System -> isSystemInDarkTheme()
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }
    ) {
        FitBriefNavigation(
            state = state,
            onEvent = onEvent
        )
    }
}
