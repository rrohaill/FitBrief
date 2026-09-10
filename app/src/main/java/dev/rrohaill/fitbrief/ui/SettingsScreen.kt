package dev.rrohaill.fitbrief.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.rrohaill.fitbrief.data.RefreshInterval
import dev.rrohaill.fitbrief.data.ThemeMode
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun SettingsScreen(
    settings: SettingsUiState,
    grantedPermissionCount: Int,
    onBack: () -> Unit,
    onOpenHealthConnect: () -> Unit,
    onToggleDailySummary: () -> Unit,
    onToggleWeeklyReport: () -> Unit,
    onSetDailySummaryTime: (Int) -> Unit,
    onSetWeeklyReportTime: (Int) -> Unit,
    onSetWeeklyReportDay: (Int) -> Unit,
    onSettingsAction: (String) -> Unit,
    onChangeTheme: (ThemeMode) -> Unit,
    onChangeRefreshInterval: (RefreshInterval) -> Unit
) {
    var themeDialog by remember { mutableStateOf(false) }
    var refreshDialog by remember { mutableStateOf(false) }
    var timePickerTarget by remember { mutableStateOf<String?>(null) }
    var weeklyDayDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 22.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onBack)
            ) {
                Text(
                    "‹",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(end = 14.dp)
                )
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            SettingsSection("DATA SOURCE") {
                SettingsRow(
                    "Health Connect Permissions",
                    "Synced, $grantedPermissionCount permissions active",
                    onClick = onOpenHealthConnect
                )
                SettingsRow(
                    "Data Refresh Interval",
                    settings.refreshInterval.label,
                    onClick = { refreshDialog = true })
            }
            SettingsSection("NOTIFICATIONS") {
                SettingsRow(
                    "Daily Summary Alert",
                    "Promptly at ${formatTime(settings.dailySummaryTimeMinutes)}",
                    trailing = {
                        SettingSwitch(
                            settings.dailySummaryEnabled,
                            onClick = {
                                if (settings.dailySummaryEnabled) {
                                    onToggleDailySummary()
                                } else {
                                    timePickerTarget = "daily"
                                }
                            }
                        )
                    }
                )
                SettingsRow(
                    "Weekly Progress Report",
                    "${dayName(settings.weeklyReportDayOfWeek)} at ${formatTime(settings.weeklyReportTimeMinutes)}",
                    trailing = {
                        SettingSwitch(
                            settings.weeklyReportEnabled,
                            onClick = {
                                if (settings.weeklyReportEnabled) {
                                    onToggleWeeklyReport()
                                } else {
                                    weeklyDayDialog = true
                                }
                            }
                        )
                    }
                )
            }
            SettingsSection("ARTIFICIAL INTELLIGENCE") {
                SettingsRow(
                    "AI Model",
                    "On-device model selection",
                    onClick = { onSettingsAction("AI Model\nFitBrief automatically prefers Gemini Nano and falls back to the local LiteRT-LM model.") },
                    trailing = {
                        Text(
                            "⊗",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    })
                SettingsRow(
                    "On-Device Processing Only",
                    "Mandatory for privacy compliance",
                    trailing = { SettingSwitch(true, {}) })
            }
            SettingsSection("APPEARANCE") {
                SettingsRow("Theme Mode", settings.themeMode.label, onClick = { themeDialog = true })
            }
            SettingsSection("PRIVACY & LEGALS") {
                SettingsRow(
                    "Privacy Policy",
                    "All processing stays on-device",
                    onClick = { onSettingsAction("Privacy Policy\nHealth data is read through Health Connect and processed only on this device. It is never uploaded.") })
                SettingsRow(
                    "About FitBrief",
                    "FitBrief for Android v1.0.2",
                    onClick = { onSettingsAction("About FitBrief\nFitBrief creates descriptive, motivational health summaries from your on-device aggregates.") })
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "FitBrief for Android v1.0.2",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    "Secure Health Summaries Platform",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .6f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
    if (weeklyDayDialog) {
        val dayOptions = (1..7).map(::dayName)
        ChoiceDialog(
            title = "Weekly report day",
            options = dayOptions,
            selected = dayName(settings.weeklyReportDayOfWeek),
            onSelect = { selectedDay ->
                val day = dayOptions.indexOf(selectedDay) + 1
                onSetWeeklyReportDay(day)
                weeklyDayDialog = false
                timePickerTarget = "weekly"
            },
            onDismiss = { weeklyDayDialog = false }
        )
    }
    timePickerTarget?.let { target ->
        val minutes =
            if (target == "daily") settings.dailySummaryTimeMinutes else settings.weeklyReportTimeMinutes
        val dialog = remember(target, minutes) {
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val selectedMinutes = hour * 60 + minute
                    if (target == "daily") {
                        onSetDailySummaryTime(selectedMinutes)
                        onToggleDailySummary()
                    } else {
                        onSetWeeklyReportTime(selectedMinutes)
                        onToggleWeeklyReport()
                    }
                    timePickerTarget = null
                },
                minutes / 60,
                minutes % 60,
                false
            ).apply {
                setOnCancelListener { timePickerTarget = null }
            }
        }
        DisposableEffect(dialog) {
            dialog.show()
            onDispose {
                dialog.dismiss()
            }
        }
    }
    SettingsDialogs(
        settings = settings,
        themeDialog = themeDialog,
        refreshDialog = refreshDialog,
        onChangeTheme = onChangeTheme,
        onChangeRefreshInterval = onChangeRefreshInterval,
        closeTheme = { themeDialog = false },
        closeRefresh = { refreshDialog = false }
    )
}

private fun dayName(dayOfWeek: Int): String =
    listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        .get((dayOfWeek - 1).coerceIn(0, 6))

private fun formatTime(minutes: Int): String =
    LocalTime.of(minutes / 60 % 24, minutes % 60)
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

@Composable
private fun SettingsDialogs(
    settings: SettingsUiState,
    themeDialog: Boolean,
    refreshDialog: Boolean,
    onChangeTheme: (ThemeMode) -> Unit,
    onChangeRefreshInterval: (RefreshInterval) -> Unit,
    closeTheme: () -> Unit,
    closeRefresh: () -> Unit
) {
    if (themeDialog) {
        ChoiceDialog(
            "Theme Mode",
            ThemeMode.entries.map { it.label },
            settings.themeMode.label,
            { label ->
                ThemeMode.entries.firstOrNull { it.label == label }?.let(onChangeTheme)
                closeTheme()
            },
            closeTheme
        )
    }
    if (refreshDialog) {
        ChoiceDialog(
            "Data Refresh Interval",
            RefreshInterval.entries.map { it.label },
            settings.refreshInterval.label,
            { label ->
                RefreshInterval.entries.firstOrNull { it.label == label }
                    ?.let(onChangeRefreshInterval)
                closeRefresh()
            },
            closeRefresh
        )
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    OutlinedButton(
                        onClick = { onSelect(option) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (option == selected) "✓  $option" else option)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
        if (trailing != null) trailing() else Text(
            "›",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingSwitch(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (enabled) Color.White else MaterialTheme.colorScheme.surfaceContainer)
        )
    }
}

@Preview(name = "Settings Light", showBackground = true)
@Composable
private fun SettingsLightPreview() {
    PreviewSurface(false) {
        SettingsScreen(
            settings = SettingsUiState(),
            grantedPermissionCount = 7,
            onBack = {},
            onOpenHealthConnect = {},
            onToggleDailySummary = {},
            onToggleWeeklyReport = {},
            onSetDailySummaryTime = {},
            onSetWeeklyReportTime = {},
            onSetWeeklyReportDay = {},
            onSettingsAction = {},
            onChangeTheme = {},
            onChangeRefreshInterval = {}
        )
    }
}

@Preview(name = "Settings Dark", showBackground = true)
@Composable
private fun SettingsDarkPreview() {
    PreviewSurface(true) {
        SettingsScreen(
            settings = SettingsUiState(),
            grantedPermissionCount = 7,
            onBack = {},
            onOpenHealthConnect = {},
            onToggleDailySummary = {},
            onToggleWeeklyReport = {},
            onSetDailySummaryTime = {},
            onSetWeeklyReportTime = {},
            onSetWeeklyReportDay = {},
            onSettingsAction = {},
            onChangeTheme = {},
            onChangeRefreshInterval = {}
        )
    }
}
