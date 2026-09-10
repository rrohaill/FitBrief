package dev.rrohaill.fitbrief.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.time.LocalDate

@Composable
fun FitBriefNavigation(
    state: FitBriefUiState,
    onEvent: (FitBriefEvent) -> Unit
) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val startDestination = if (state.permissionStatus.granted) "dashboard" else "onboarding"

    LaunchedEffect(state.permissionStatus.granted, currentRoute) {
        if (state.permissionStatus.granted && currentRoute == "onboarding") {
            navController.navigate("dashboard") {
                popUpTo("onboarding") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                state = state,
                onRequestHealthPermissions = { onEvent(FitBriefEvent.RequestHealthPermissions) },
                onOpenHealthConnect = { onEvent(FitBriefEvent.OpenHealthConnect) }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                state = state,
                onRefresh = { onEvent(FitBriefEvent.Refresh) },
                onOpenSettings = {
                    onEvent(FitBriefEvent.OpenSettings)
                    navController.navigate("settings")
                },
                onSelectRange = { onEvent(FitBriefEvent.SelectRange(it)) },
                onScheduleNotifications = { onEvent(FitBriefEvent.ScheduleNotifications) },
                onOpenMetricDetail = { metric ->
                    onEvent(FitBriefEvent.OpenMetric(metric))
                    navController.navigate("metric/${metric.name}")
                }
            )
        }
        composable("summary") {
            SummaryDetailScreen(
                state = state,
                onRefresh = { onEvent(FitBriefEvent.Refresh) },
                onClose = {
                    onEvent(FitBriefEvent.CloseSummary)
                    navController.popBackStack()
                },
                onOpenMetricDetail = { metric ->
                    onEvent(FitBriefEvent.OpenMetric(metric))
                    navController.navigate("metric/${metric.name}")
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                state = state,
                onBack = {
                    onEvent(FitBriefEvent.CloseSettings)
                    navController.popBackStack()
                },
                onOpenHealthConnect = { onEvent(FitBriefEvent.OpenHealthConnect) },
                onToggleDailySummary = { onEvent(FitBriefEvent.ToggleDailySummary) },
                onToggleWeeklyReport = { onEvent(FitBriefEvent.ToggleWeeklyReport) },
                onSetDailySummaryTime = { onEvent(FitBriefEvent.SetDailySummaryTime(it)) },
                onSetWeeklyReportTime = { onEvent(FitBriefEvent.SetWeeklyReportTime(it)) },
                onSetWeeklyReportDay = { onEvent(FitBriefEvent.SetWeeklyReportDay(it)) },
                onSettingsAction = { onEvent(FitBriefEvent.ShowSettingsNotice(it)) },
                onChangeTheme = { onEvent(FitBriefEvent.ChangeTheme(it)) },
                onChangeRefreshInterval = { onEvent(FitBriefEvent.ChangeRefreshInterval(it)) }
            )
            state.settingsNotice?.let { notice ->
                AlertDialog(
                    onDismissRequest = { onEvent(FitBriefEvent.DismissSettingsNotice) },
                    title = { Text(notice.substringBefore("\n")) },
                    text = { Text(notice.substringAfter("\n", "")) },
                    confirmButton = {
                        Button(onClick = { onEvent(FitBriefEvent.DismissSettingsNotice) }) { Text("Done") }
                    }
                )
            }
        }
        composable(
            route = "metric/{metric}",
            arguments = listOf(navArgument("metric") { type = NavType.StringType })
        ) { entry ->
            val metric = entry.arguments?.getString("metric")
                ?.let { runCatching { MetricType.valueOf(it) }.getOrNull() }
            if (metric == null) {
                navController.popBackStack()
            } else {
                MetricDetailScreen(
                    state = state,
                    metric = metric,
                    onClose = {
                        onEvent(FitBriefEvent.CloseMetric)
                        if (state.metricDrilldownDate == null) navController.popBackStack()
                    },
                    onNavigateDay = { onEvent(FitBriefEvent.NavigateMetricDay(it)) },
                    onOpenMetricDate = { onEvent(FitBriefEvent.OpenMetricDate(it)) }
                )
            }
        }
    }
}
