package dev.rrohaill.fitbrief.ui

import android.app.TimePickerDialog
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import dev.rrohaill.fitbrief.data.HealthConnectAvailability
import dev.rrohaill.fitbrief.data.HealthRange
import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.PermissionStatus
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.data.RefreshInterval
import dev.rrohaill.fitbrief.data.ThemeMode
import dev.rrohaill.fitbrief.data.TimelineEvent
import dev.rrohaill.fitbrief.summary.SummarizerBackend
import dev.rrohaill.fitbrief.ui.theme.FitBriefTheme
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun OnboardingScreen(
    state: FitBriefUiState,
    onRequestHealthPermissions: () -> Unit,
    onOpenHealthConnect: () -> Unit
) {
    var contentVisible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(650),
        label = "onboarding-content-alpha"
    )
    val contentOffset by animateDpAsState(
        targetValue = if (contentVisible) 0.dp else 28.dp,
        animationSpec = tween(650),
        label = "onboarding-content-offset"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0.92f,
        animationSpec = tween(650),
        label = "onboarding-content-scale"
    )
    LaunchedEffect(Unit) {
        delay(1_000.milliseconds)
        contentVisible = true
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.fillMaxSize()) {
            AiPulseBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 44.dp)
                    .offset { IntOffset(0, contentOffset.roundToPx()) }
                    .scale(contentScale)
                    .alpha(contentAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(18.dp))
                LogoMark()
                Spacer(Modifier.height(30.dp))
                Text(
                    "Your health,\nsummarized.",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.displaySmall.lineHeight
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "FitBrief securely connects to Google Health Connect and creates short, actionable AI summaries of your daily vitals.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(26.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Badge(
                            "⊗",
                            MaterialTheme.colorScheme.primary.copy(alpha = .08f),
                            MaterialTheme.colorScheme.primary
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(end = 16.dp)
                        ) {
                            Text("On-device Privacy First", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Your physical activity, sleep, and heart metrics stay on your phone. Processing is 100% private and on-device.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = state.permissionStatus.availability == HealthConnectAvailability.Available,
                    onClick = onRequestHealthPermissions
                ) {
                    Text("Connect Health Connect", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenHealthConnect
                ) {
                    Text(
                        if (state.permissionStatus.availability == HealthConnectAvailability.Available)
                            "Maybe later"
                        else "Install Health Connect",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun AiPulseBackground() {
    val transition = rememberInfiniteTransition(label = "ai-pulse-bg")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3_600),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val primary = MaterialTheme.colorScheme.primary
    val ambient = MaterialTheme.colorScheme.onSurface

    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.56f, size.height * 0.30f)
        val radius = 62f + pulse * 82f
        drawCircle(primary.copy(alpha = 0.045f + (1f - pulse) * 0.07f), radius)
        drawCircle(
            primary.copy(alpha = 0.22f * (1f - pulse)),
            radius = radius + 22f,
            style = Stroke(width = 1.2f)
        )
        drawCircle(
            ambient.copy(alpha = 0.045f),
            radius = 118f,
            style = Stroke(
                width = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(
                        3f,
                        7f
                    )
                )
            )
        )
        drawCircle(
            primary.copy(alpha = 0.18f),
            radius = 4f,
            center = Offset(size.width * (0.14f + pulse * 0.58f), size.height * 0.84f)
        )
        drawCircle(
            primary.copy(alpha = 0.12f),
            radius = 3f,
            center = Offset(size.width * 0.73f, size.height * 0.78f)
        )
        drawCircle(
            Color(0xFFE5B96B).copy(alpha = 0.3f),
            radius = 4f,
            center = Offset(size.width * 0.72f, size.height * 0.76f)
        )
        drawLine(
            ambient.copy(alpha = 0.04f),
            Offset(0f, size.height * 0.48f),
            Offset(size.width, size.height * 0.48f),
            strokeWidth = 1f
        )
        drawLine(
            primary.copy(alpha = 0.035f),
            Offset(size.width * 0.25f, size.height * 0.18f),
            Offset(size.width * 0.16f, size.height * 0.54f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
internal fun DashboardScreen(
    state: FitBriefUiState,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectRange: (RangeOption) -> Unit,
    onScheduleNotifications: () -> Unit,
    onOpenMetricDetail: (MetricType) -> Unit = {}
) {
    val snapshot = state.snapshot
    val dashboardScrollState = rememberScrollState()
    var pullDistance by remember { mutableFloatStateOf(0f) }
    val pullRefreshConnection = remember(state.isLoading, onRefresh) {
        object : NestedScrollConnection {
            fun triggerRefreshIfReady() {
                if (pullDistance >= 120f && !state.isLoading) onRefresh()
                pullDistance = 0f
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (available.y > 0f && dashboardScrollState.value == 0 && !state.isLoading) {
                    pullDistance += available.y
                    Offset(0f, available.y)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                triggerRefreshIfReady()
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                triggerRefreshIfReady()
                return Velocity.Zero
            }
        }
    }
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    val date = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, MMMM d", LocalLocale.current.platformLocale))
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier
            .fillMaxSize()
            .padding(padding)) {
            if (state.isLoading) {
                AiPulseBackground()
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .nestedScroll(pullRefreshConnection)
                    .verticalScroll(dashboardScrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pullDistance > 0f && !state.isLoading) {
                    LinearProgressIndicator(
                        progress = { (pullDistance / 120f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            greeting,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(date, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        }
                    }
                    Badge(
                        "⚙",
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.onSurface,
                        onClick = onOpenSettings
                    )
                }

                RangeTabs(state.selectedRange, onSelectRange)
                MetricGrid(snapshot, onMetricClick = onOpenMetricDetail)
                InsightCard(state)
                Text(
                    "AI activity timeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (state.timeline.isEmpty()) {
                    Text(
                        "No timestamped activity is available for this range.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.timeline.forEach { event ->
                        TimelineEventCard(event)
                    }
                }
                Spacer(Modifier.height(60.dp))
                if (state.message != null) {
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(
                    "FitBrief is descriptive, not medical advice. All processing stays on-device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(104.dp))
            }
        }
    }
}

@Composable
internal fun SettingsScreen(
    state: FitBriefUiState,
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
                    "Synced, ${state.permissionStatus.grantedCount} permissions active",
                    onClick = onOpenHealthConnect
                )
                SettingsRow(
                    "Data Refresh Interval",
                    state.refreshInterval.label,
                    onClick = { refreshDialog = true })
            }
            SettingsSection("NOTIFICATIONS") {
                SettingsRow(
                    "Daily Summary Alert",
                    "Promptly at ${formatTime(state.dailySummaryTimeMinutes)}",
                    trailing = {
                        SettingSwitch(
                            state.dailySummaryEnabled,
                            onClick = {
                                if (state.dailySummaryEnabled) {
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
                    "${dayName(state.weeklyReportDayOfWeek)} at ${formatTime(state.weeklyReportTimeMinutes)}",
                    trailing = {
                        SettingSwitch(
                            state.weeklyReportEnabled,
                            onClick = {
                                if (state.weeklyReportEnabled) {
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
                SettingsRow("Theme Mode", state.themeMode.label, onClick = { themeDialog = true })
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
            selected = dayName(state.weeklyReportDayOfWeek),
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
            if (target == "daily") state.dailySummaryTimeMinutes else state.weeklyReportTimeMinutes
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
        state = state,
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
    state: FitBriefUiState,
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
            state.themeMode.label,
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
            state.refreshInterval.label,
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

@Composable
internal fun SummaryDetailScreen(
    state: FitBriefUiState,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
    onOpenMetricDetail: (MetricType) -> Unit = {}
) {
    val snapshot = state.snapshot
    val context = LocalContext.current
    val shareText = buildString {
        appendLine("FitBrief ${state.selectedRange.label} summary")
        appendLine()
        appendLine(state.summary.ifBlank { "AI summary is not available yet." })
        if (state.timeline.isNotEmpty()) {
            appendLine()
            appendLine("Activity timeline")
            state.timeline.forEach { event ->
                appendLine("${event.title}: ${event.detail}")
            }
        }
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "‹",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .clickable(onClick = onClose)
                            .padding(end = 12.dp)
                    )
                    Text(
                        "Your summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Badge(
                        "↻",
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.onSurface,
                        40.dp,
                        onClick = onRefresh
                    )
                    Badge(
                        "↗",
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.onSurface,
                        40.dp,
                        onClick = {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    },
                                    "Share FitBrief summary"
                                )
                            )
                        }
                    )
                }
            }
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "Generating your ${state.selectedRange.label.lowercase()} summary…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                when {
                    state.isLoading -> "Analyzing your selected range with on-device AI…"
                    state.summary.isNotBlank() -> state.summary
                    state.snapshot != null -> "The AI summary could not be generated. Tap refresh to try again."
                    else -> "No AI summary is available for this range yet."
                },
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )
            Text(
                "This AI-generated reflection uses the health data available for the selected range. It is useful context, not a diagnosis.",
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )
            Text(
                "Activity timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (state.timeline.isEmpty()) {
                Text(
                    "No timestamped activity is available for this range or the related permission is not granted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.timeline.forEach { event -> TimelineEventCard(event) }
            }
            Text(
                "ⓘ  FitBrief summaries are motivational and descriptive only, not medical advice.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun TimelineEventCard(event: TimelineEvent) {
    val time = remember(event.timestamp) {
        event.timestamp.atZone(java.time.ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("h:mm a"))
    }
    val endTime = remember(event.endTimestamp) {
        event.endTimestamp?.atZone(java.time.ZoneId.systemDefault())
            ?.format(DateTimeFormatter.ofPattern("h:mm a"))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(event.icon, style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(event.title, fontWeight = FontWeight.Bold)
                    Text(
                        event.periodLabel
                            ?: if (endTime != null && endTime != time) "$time - $endTime" else time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(event.detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TrendCard(
    title: String,
    label: String,
    value: Long,
    target: Long,
    valueLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    modifier = Modifier.width(92.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = {
                        if (target > 0) (value.toFloat() / target).coerceIn(
                            0f,
                            1f
                        ) else 0f
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.background
                )
                Text(
                    valueLabel,
                    modifier = Modifier.width(72.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun InsightCard(state: FitBriefUiState) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (expanded) 279.dp else 156.dp)
            .animateContentSize()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD4EFEB))
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                    Text(
                        "✣  AI INSIGHT",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        color = Color(0xFF167565),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    if (state.isLoading) "Generating…" else "Generated just now",
                    color = Color(0xFF4C8980),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                when {
                    state.isLoading -> "Analyzing your selected range with on-device AI…"
                    state.summary.isNotBlank() -> state.summary
                    state.snapshot != null -> "The AI summary could not be generated. Pull down to try again."
                    else -> "Connect your health data to get a short, friendly view of your activity and recovery."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF174A43),
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (expanded && state.summary.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFB5DED8))
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "A suggestion is included only when the selected data shows an opportunity to improve.",
                    color = Color(0xFF315E58),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Tap to expand",
                    color = Color(0xFF315E58),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun RangeTabs(selected: RangeOption, onSelect: (RangeOption) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            RangeOption.Today to "Today",
            RangeOption.SevenDays to "Week",
            RangeOption.ThirtyDays to "Month"
        ).forEach { (option, label) ->
            val active = option == selected
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                onClick = { onSelect(option) }
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

private enum class MetricGraph {
    StepsProgress,
    HeartRateBars,
    None
}

enum class MetricType(val label: String) {
    Steps("Steps"),
    HeartRate("Heart Rate"),
    Sleep("Sleep"),
    ActiveCalories("Active Calories"),
    Distance("Distance"),
    Exercise("Exercise"),
    TotalCalories("Total Calories")
}

private data class Metric(
    val type: MetricType,
    val title: String,
    val value: String,
    val unit: String,
    val icon: String,
    val tint: Color,
    val footer: String,
    val graph: MetricGraph = MetricGraph.None,
    val progress: Float? = null
)

@Composable
internal fun MetricDetailScreen(
    state: FitBriefUiState,
    metric: MetricType,
    onClose: () -> Unit,
    onNavigateDay: (Int) -> Unit,
    onOpenMetricDate: (LocalDate) -> Unit
) {
    val snapshot = state.snapshot
    val timeline = state.timeline
    val graphLoading = snapshot == null
    var selectedBarDate by remember(metric, state.metricDrilldownDate) { mutableStateOf<LocalDate?>(null) }
    val periodDate = LocalDate.now().minusDays(
        when (state.selectedRange) {
            RangeOption.Today -> state.metricDayOffset.toLong()
            RangeOption.SevenDays -> state.metricDayOffset * 7L
            RangeOption.ThirtyDays -> state.metricDayOffset * 30L
        }
    )
    val periodLabel = if (state.metricDrilldownDate != null) {
        state.metricDrilldownDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
    } else when (state.selectedRange) {
        RangeOption.Today -> periodDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
        RangeOption.SevenDays -> "Week ending ${periodDate.format(DateTimeFormatter.ofPattern("MMM d"))}"
        RangeOption.ThirtyDays -> "Month ending ${periodDate.format(DateTimeFormatter.ofPattern("MMM d"))}"
    }
    val heartSamples = if (state.metricHeartRateSamples.isNotEmpty()) {
        state.metricHeartRateSamples
    } else {
        state.timeline.flatMap { it.samples }
    }
    val heartLow = heartSamples.minOrNull()?.toInt() ?: snapshot?.averageHeartRateBpm?.toInt() ?: 0
    val heartHigh = heartSamples.maxOrNull()?.toInt() ?: snapshot?.averageHeartRateBpm?.toInt() ?: 0
    val value = when (metric) {
        MetricType.Steps -> snapshot?.steps?.toString() ?: "0"
        MetricType.HeartRate -> "$heartLow–$heartHigh bpm"
        MetricType.Sleep -> snapshot?.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "0h"
        MetricType.ActiveCalories -> snapshot?.activeCaloriesKcal?.let { "${it.toInt()} kcal" } ?: "0 kcal"
        MetricType.Distance -> snapshot?.distanceKilometers?.let { "${DecimalFormat("#,##0.#").format(it)} km" } ?: "0 km"
        MetricType.Exercise -> "${snapshot?.exerciseMinutes ?: 0} min"
        MetricType.TotalCalories -> snapshot?.totalCaloriesKcal?.let { "${it.toInt()} kcal" } ?: "0 kcal"
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("‹", style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.clickable(onClick = onClose).padding(end = 12.dp))
                Column {
                    Text(metric.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        periodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Badge(
                    "‹",
                    MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.colorScheme.onSurface,
                    40.dp,
                    onClick = { onNavigateDay(1) }
                )
                Spacer(Modifier.width(8.dp))
                Badge(
                    "›",
                    MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (state.metricDayOffset == 0) 0.35f else 1f
                    ),
                    40.dp,
                    onClick = { if (state.metricDayOffset > 0) onNavigateDay(-1) }
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (metric == MetricType.HeartRate) "Heart-rate range" else "Selected range total",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    if (metric == MetricType.HeartRate) {
                        Text(
                            "Average ${snapshot?.averageHeartRateBpm ?: "—"} bpm  •  Low $heartLow  •  High $heartHigh",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Highest zone: ${heartRateZone(heartHigh)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("Detailed view from Health Connect data for this range.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!graphLoading) {
                MetricDetailGraph(metric, snapshot, timeline, state.metricHeartRateSamples) { date ->
                    selectedBarDate = date
                }
                selectedBarDate?.let { date ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenMetricDate(date) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "View day ›",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (metric == MetricType.HeartRate) HeartRateZoneLegend()
                MetricInsightCard(state)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Loading ${metric.label.lowercase()} data…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                }
            }
            Text("This visualization is descriptive context, not medical advice.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun heartRateZone(bpm: Int): String = when {
            bpm >= 135 -> "Peak"
            bpm >= 110 -> "Vigorous"
            bpm >= 71 -> "Moderate"
            else -> "Light"
        }

@Composable
private fun MetricInsightCard(state: FitBriefUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AI insight", fontWeight = FontWeight.Bold)
            if (state.metricInsightLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Analyzing this metric…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text(
                    state.metricInsight ?: "No AI insight is available for this metric yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HeartRateZoneLegend() {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    "Light" to Color(0xFF32C7F0),
                    "Moderate" to Color(0xFF4FD477),
                    "Vigorous" to Color(0xFFFFB52E),
                    "Peak" to Color(0xFFE9656D)
                ).forEach { (label, color) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
                        Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun MetricDetailGraph(
    metric: MetricType,
    snapshot: HealthSnapshot?,
    timeline: List<TimelineEvent>,
    metricHeartRateSamples: List<Double>,
    onBarSelected: (LocalDate) -> Unit
) {
    val color = MaterialTheme.colorScheme.primary
    val total = when (metric) {
        MetricType.Steps -> snapshot?.steps?.toFloat() ?: 0f
        MetricType.HeartRate -> snapshot?.averageHeartRateBpm?.toFloat() ?: 0f
        MetricType.Sleep -> snapshot?.sleepMinutes?.toFloat() ?: 0f
        MetricType.ActiveCalories -> snapshot?.activeCaloriesKcal?.toFloat() ?: 0f
        MetricType.Distance -> snapshot?.distanceMeters?.toFloat() ?: 0f
        MetricType.Exercise -> snapshot?.exerciseMinutes?.toFloat() ?: 0f
        MetricType.TotalCalories -> snapshot?.totalCaloriesKcal?.toFloat() ?: 0f
    }
    val metricKey = when (metric) {
        MetricType.Steps -> "steps"
        MetricType.HeartRate -> "heartRate"
        MetricType.Sleep -> "sleep"
        MetricType.ActiveCalories -> "activeCalories"
        MetricType.Distance -> "distance"
        MetricType.Exercise -> "exercise"
        MetricType.TotalCalories -> "totalCalories"
    }
    val heartSamples = if (metricHeartRateSamples.isNotEmpty()) {
        metricHeartRateSamples.map(Double::toFloat)
    } else timeline
        .sortedBy(TimelineEvent::timestamp)
        .flatMap { it.samples }
        .map(Double::toFloat)
    val metricEvents = timeline
        .sortedBy(TimelineEvent::timestamp)
        .filter { it.values.containsKey(metricKey) }
    val measuredValues = if (metric == MetricType.HeartRate && heartSamples.isNotEmpty()) {
        heartSamples
    } else {
        metricEvents.mapNotNull { it.values[metricKey]?.toFloat() }
    }
    val values = measuredValues.ifEmpty { listOf(total) }
    val barDates = metricEvents
        .map { it.timestamp.atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
    val heartPeriodValues = if (metric == MetricType.HeartRate) {
        when (snapshot?.range?.option) {
            RangeOption.SevenDays -> (0 until 7).map { index ->
                timeline.filter {
                    it.timestamp.atZone(java.time.ZoneId.systemDefault()).dayOfWeek.value == index + 1
                }.mapNotNull { it.values["heartRate"]?.toFloat() }.average().toFloat()
            }
            RangeOption.ThirtyDays -> timeline.groupBy {
                it.timestamp.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            }.values.map { group -> group.mapNotNull { it.values["heartRate"]?.toFloat() }.average().toFloat() }
            else -> values
        }.filter { it.isFinite() && it > 0f }
    } else {
        values
    }
    val axisFormatter = DecimalFormat("#,##0.#")
    val rawMax = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val yMin = if (metric == MetricType.HeartRate && snapshot?.range?.option == RangeOption.Today) {
        30f
    } else {
        0f
    }
    val yMax = if (metric == MetricType.HeartRate) {
        val margin = if (snapshot?.range?.option == RangeOption.Today) 10f else 2f
        (kotlin.math.ceil((rawMax + margin) / 10f) * 10f).coerceAtLeast(rawMax + margin)
    } else if (metric == MetricType.Exercise) {
        rawMax.coerceAtLeast(60f)
    } else if (metric == MetricType.Sleep) {
        rawMax.coerceAtLeast(540f)
    } else {
        rawMax
    }
    val yUnit = when (metric) {
        MetricType.Steps -> "steps"
        MetricType.HeartRate -> "bpm"
        MetricType.Sleep -> "hours"
        MetricType.ActiveCalories, MetricType.TotalCalories -> "kcal"
        MetricType.Distance -> "m"
        MetricType.Exercise -> "min"
    }
    val xLabels = if (metric == MetricType.Exercise) {
        listOf("0 min", "30 min", "60 min")
    } else {
        val dates = metricEvents.map { it.timestamp }.sorted()
        val formatter = when (snapshot?.range?.option) {
            RangeOption.Today -> DateTimeFormatter.ofPattern("h a")
            RangeOption.SevenDays -> DateTimeFormatter.ofPattern("EEE")
            RangeOption.ThirtyDays -> DateTimeFormatter.ofPattern("MMM d")
            null -> DateTimeFormatter.ofPattern("h a")
        }
        if (metric == MetricType.HeartRate && snapshot?.range?.option == RangeOption.Today) {
            listOf(
                "12am",
                "4am",
                "8am",
                "12pm",
                LocalTime.now().format(DateTimeFormatter.ofPattern("h a"))
            )
        } else if (dates.isEmpty()) listOf("Start", "Now")
        else listOf(dates.first(), dates[dates.lastIndex / 2], dates.last())
            .distinct()
            .map { it.atZone(java.time.ZoneId.systemDefault()).format(formatter) }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                when (metric) {
                    MetricType.Steps -> "Activity bars"
                    MetricType.HeartRate -> "Heart-rate trend"
                    MetricType.Sleep -> "Sleep trend"
                    MetricType.ActiveCalories, MetricType.TotalCalories -> "Calories burned"
                    MetricType.Distance -> "Distance trend"
                    MetricType.Exercise -> "Workout minutes"
                },
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(48.dp).height(180.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    if (metric == MetricType.HeartRate && snapshot?.range?.option == RangeOption.Today) {
                        listOf(1f, 0.75f, 0.5f, 0.25f, 0f).forEach { fraction ->
                            Text(
                                axisFormatter.format(yMin + (yMax - yMin) * fraction),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    } else {
                        val formatAxisValue: (Float) -> String = { axisValue ->
                            if (metric == MetricType.Sleep) {
                                "${axisFormatter.format(axisValue / 60f)} h"
                            } else {
                                "${axisFormatter.format(axisValue)} $yUnit"
                            }
                        }
                        Text(formatAxisValue(yMax), style = MaterialTheme.typography.labelSmall)
                        Text(
                            formatAxisValue((yMax + yMin) / 2f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(formatAxisValue(yMin), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Box(Modifier.weight(1f).height(180.dp)) {
                    when (metric) {
                        MetricType.Steps -> BarMetricGraph(values, color) { index ->
                            barDates.getOrNull(index)?.let(onBarSelected)
                        }
                        MetricType.Distance -> DistanceMetricGraph(values) { index ->
                            barDates.getOrNull(index)?.let(onBarSelected)
                        }
                        MetricType.HeartRate -> if (snapshot?.range?.option == RangeOption.Today) {
                            HeartRateMetricGraph(values, yMin, yMax)
                        } else {
                            HeartRatePeriodGraph(heartPeriodValues, snapshot?.range?.option, Color(0xFF20C7F2))
                        }
                        MetricType.Sleep -> SleepDurationGraph(values, yMax) { index ->
                            barDates.getOrNull(index)?.let(onBarSelected)
                        }
                        MetricType.ActiveCalories, MetricType.TotalCalories -> CaloriesMetricGraph(
                            values,
                            if (metric == MetricType.ActiveCalories) Color(0xFFEFA92E) else Color(0xFFFF7A59)
                        ) { index ->
                            barDates.getOrNull(index)?.let(onBarSelected)
                        }
                        MetricType.Exercise -> ExerciseDurationGraph(values, yMax, color) { index ->
                            barDates.getOrNull(index)?.let(onBarSelected)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.padding(start = 48.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                xLabels.forEach { label ->
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("X-axis: selected range  •  Y-axis: $yUnit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (metric == MetricType.Sleep) {
                Text(
                    "Shaded band: 7–9 hours",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (metric == MetricType.Exercise) {
                Text(
                    "Reference marker: 30 minutes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BarMetricGraph(values: List<Float>, color: Color, onBarClick: (Int) -> Unit) {
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(values) {
                detectTapGestures { offset ->
                    val slot = size.width / values.size.coerceAtLeast(1)
                    onBarClick((offset.x / slot).toInt().coerceIn(0, values.lastIndex))
                }
            }
    ) {
        val max = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val gap = size.width / (values.size * 2f)
        values.forEachIndexed { index, value ->
            val height = size.height * value / max
            drawRoundRect(color.copy(alpha = 0.55f + index * 0.04f),
                Offset(gap + index * gap * 2, size.height - height),
                androidx.compose.ui.geometry.Size(gap, height),
                androidx.compose.ui.geometry.CornerRadius(10f, 10f))
        }
    }
}

@Composable
private fun DistanceMetricGraph(values: List<Float>, onBarClick: (Int) -> Unit) {
    TrendMetricGraph(values, Color(0xFF20B8A6), onBarClick)
}

@Composable
private fun SleepDurationGraph(values: List<Float>, chartMax: Float, onBarClick: (Int) -> Unit) {
    val barColor = Color(0xFF8B7CFF)
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(values) {
                detectTapGestures { offset ->
                    val slot = size.width / values.size.coerceAtLeast(1)
                    onBarClick((offset.x / slot).toInt().coerceIn(0, values.lastIndex))
                }
            }
    ) {
        val bottom = size.height * 0.92f
        val top = size.height * 0.08f
        fun yFor(minutes: Float): Float =
            bottom - (bottom - top) * (minutes / chartMax).coerceIn(0f, 1f)

        drawRoundRect(
            color = Color(0xFF8B7CFF).copy(alpha = 0.12f),
            topLeft = Offset(0f, yFor(540f)),
            size = androidx.compose.ui.geometry.Size(size.width, yFor(420f) - yFor(540f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
        )
        for (index in 0..3) {
            val y = top + (bottom - top) * index / 3f
            drawLine(
                color = barColor.copy(alpha = 0.12f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        val slot = size.width / values.size.coerceAtLeast(1)
        val barWidth = (slot * 0.58f).coerceAtLeast(12f)
        values.forEachIndexed { index, value ->
            val barHeight = bottom - yFor(value)
            val x = slot * index + (slot - barWidth) / 2f
            drawRoundRect(
                color = barColor.copy(alpha = 0.72f + (index % 3) * 0.08f),
                topLeft = Offset(x, yFor(value)),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
            )
            drawCircle(
                color = barColor,
                radius = 4f,
                center = Offset(x + barWidth / 2f, yFor(value))
            )
        }
        drawLine(
            color = barColor.copy(alpha = 0.35f),
            start = Offset(0f, bottom),
            end = Offset(size.width, bottom),
            strokeWidth = 2f
        )
    }
}

@Composable
private fun TrendMetricGraph(values: List<Float>, lineColor: Color, onBarClick: (Int) -> Unit) {
    val fillTop = lineColor.copy(alpha = 0.34f)
    val fillBottom = lineColor.copy(alpha = 0.02f)
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(values) {
                detectTapGestures { offset ->
                    val slot = size.width / values.size.coerceAtLeast(1)
                    onBarClick((offset.x / slot).toInt().coerceIn(0, values.lastIndex))
                }
            }
    ) {
        val chartMax = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val bottom = size.height * 0.92f
        val top = size.height * 0.12f
        val step = size.width / (values.size - 1).coerceAtLeast(1)
        val points = values.mapIndexed { index, value ->
            Offset(
                x = if (values.size == 1) size.width / 2f else index * step,
                y = bottom - (bottom - top) * (value / chartMax).coerceIn(0f, 1f)
            )
        }

        for (index in 0..3) {
            val y = top + (bottom - top) * index / 3f
            drawLine(
                color = lineColor.copy(alpha = 0.12f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        if (points.isNotEmpty()) {
            val area = androidx.compose.ui.graphics.Path().apply {
                moveTo(points.first().x, bottom)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, bottom)
                close()
            }
            drawPath(
                area,
                brush = Brush.verticalGradient(
                    colors = listOf(fillTop, fillBottom),
                    startY = top,
                    endY = bottom
                )
            )
            points.zipWithNext().forEach { (start, end) ->
                drawLine(lineColor, start, end, strokeWidth = 5f, cap = StrokeCap.Round)
            }
            points.forEach { point ->
                drawCircle(Color.White, radius = 7f, center = point)
                drawCircle(lineColor, radius = 4f, center = point)
            }
        }
        drawLine(
            color = lineColor.copy(alpha = 0.3f),
            start = Offset(0f, bottom),
            end = Offset(size.width, bottom),
            strokeWidth = 2f
        )
    }
}

@Composable
private fun CaloriesMetricGraph(values: List<Float>, color: Color, onBarClick: (Int) -> Unit) {
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(values) {
                detectTapGestures { offset ->
                    val slot = size.width / values.size.coerceAtLeast(1)
                    onBarClick((offset.x / slot).toInt().coerceIn(0, values.lastIndex))
                }
            }
    ) {
        val top = size.height * 0.08f
        val bottom = size.height * 0.92f
        val maxValue = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val slot = size.width / values.size.coerceAtLeast(1)
        val barWidth = (slot * 0.58f).coerceAtLeast(12f)
        values.forEachIndexed { index, value ->
            val height = (bottom - top) * (value / maxValue).coerceIn(0f, 1f)
            val x = slot * index + (slot - barWidth) / 2f
            val y = bottom - height
            drawRoundRect(
                color = color.copy(alpha = 0.16f),
                topLeft = Offset(x - 4f, y - 4f),
                size = androidx.compose.ui.geometry.Size(barWidth + 8f, height + 4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = color.copy(alpha = 0.78f + (index % 3) * 0.07f),
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 3.5f,
                center = Offset(x + barWidth / 2f, y)
            )
        }
        for (index in 0..3) {
            val y = top + (bottom - top) * index / 3f
            drawLine(
                color = color.copy(alpha = 0.12f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }
        drawLine(
            color = color.copy(alpha = 0.35f),
            start = Offset(0f, bottom),
            end = Offset(size.width, bottom),
            strokeWidth = 2f
        )
    }
}

@Composable
private fun HeartRateMetricGraph(values: List<Float>, chartMin: Float, chartMax: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val pointCount = values.size.coerceAtLeast(1)
        val normalizedValues = List(pointCount) { index ->
            val source = values[index.coerceAtMost(values.lastIndex)]
            ((source - chartMin) / (chartMax - chartMin)).coerceIn(0f, 1f)
        }

        val points = normalizedValues.mapIndexed { index, normalized ->
            Offset(
                x = if (pointCount == 1) size.width / 2f
                else size.width * index / (pointCount - 1),
                y = size.height * (0.82f - normalized * 0.56f)
            )
        }
            val zones = listOf(
                chartMax to Color(0xFFE9656D),
                chartMin + (chartMax - chartMin) * 0.78f to Color(0xFFFFB52E),
                chartMin + (chartMax - chartMin) * 0.58f to Color(0xFF4FD477),
                chartMin + (chartMax - chartMin) * 0.32f to Color(0xFF32C7F0)
            )
            zones.forEach { (bpm, zoneColor) ->
                val y = size.height * (0.82f - ((bpm - chartMin) / (chartMax - chartMin)).coerceIn(0f, 1f) * 0.56f)
                var x = 0f
                while (x < size.width) {
                    drawCircle(zoneColor.copy(alpha = 0.9f), 1.5f, Offset(x, y))
                    x += 9f
                }
            }
        points.forEachIndexed { index, point ->
            val barHeight = size.height * (0.08f + normalizedValues[index] * 0.35f)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.14f),
                    topLeft = Offset(point.x - 2f, point.y - barHeight),
                    size = androidx.compose.ui.geometry.Size(4f, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                )
        }
        points.zipWithNext().forEach { (start, end) ->
            drawLine(Color(0xFF20C7F2), start, end, strokeWidth = 4f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun HeartRatePeriodGraph(values: List<Float>, range: RangeOption?, color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val chartValues = values.takeLast(if (range == RangeOption.SevenDays) 7 else 5)
        val minValue = chartValues.minOrNull()?.coerceAtLeast(1f) ?: 1f
        val maxValue = chartValues.maxOrNull()?.coerceAtLeast(minValue + 1f) ?: 1f
        val step = size.width / (chartValues.size - 1).coerceAtLeast(1)
        val points = chartValues.mapIndexed { index, value ->
            Offset(index * step, size.height * (0.84f - ((value - minValue) / (maxValue - minValue)) * 0.58f))
        }
        points.zipWithNext().forEach { (start, end) ->
            drawLine(color, start, end, strokeWidth = 4f, cap = StrokeCap.Round)
        }
        points.forEach { drawCircle(color, 6f, it) }
    }
}

@Composable
private fun ExerciseDurationGraph(
    values: List<Float>,
    chartMax: Float,
    color: Color,
    onBarClick: (Int) -> Unit
) {
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(values) {
                detectTapGestures { offset ->
                    val slot = size.width / values.size.coerceAtLeast(1)
                    onBarClick((offset.x / slot).toInt().coerceIn(0, values.lastIndex))
                }
            }
    ) {
        val top = size.height * 0.08f
        val bottom = size.height * 0.92f
        fun yFor(minutes: Float): Float =
            bottom - (bottom - top) * (minutes / chartMax).coerceIn(0f, 1f)

        for (index in 0..3) {
            val y = top + (bottom - top) * index / 3f
            drawLine(
                color = color.copy(alpha = 0.12f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }
        val targetY = yFor(30f)
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = color.copy(alpha = 0.65f),
                start = Offset(x, targetY),
                end = Offset((x + 8f).coerceAtMost(size.width), targetY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            x += 14f
        }

        val slot = size.width / values.size.coerceAtLeast(1)
        val barWidth = (slot * 0.56f).coerceAtLeast(12f)
        values.forEachIndexed { index, value ->
            val barHeight = bottom - yFor(value)
            val barX = slot * index + (slot - barWidth) / 2f
            drawRoundRect(
                color = color.copy(alpha = 0.72f + (index % 3) * 0.08f),
                topLeft = Offset(barX, yFor(value)),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = Offset(barX + barWidth / 2f, yFor(value))
            )
        }
    }
}

    @Composable
    private fun MetricGrid(
    snapshot: HealthSnapshot?,
    showGraphs: Boolean = true,
    onMetricClick: (MetricType) -> Unit = {}
) {
    val decimal = DecimalFormat("#,##0.#")
    val rangeDays = when (snapshot?.range?.option) {
        RangeOption.SevenDays -> 7
        RangeOption.ThirtyDays -> 30
        else -> 1
    }
    val stepGoal = rangeDays * 10_000L
    val stepProgress = snapshot?.steps?.toFloat()?.div(stepGoal)?.coerceIn(0f, 1f)
    val stepGoalPercent =
        snapshot?.steps?.let { (it.toFloat() / stepGoal * 100f).roundToInt() } ?: 0
    val metrics = buildList {
        snapshot?.let { data ->
            if (data.steps > 0) {
                add(
                    Metric(
                        MetricType.Steps,
                        "Steps",
                        decimal.format(data.steps),
                        "steps",
                        "♧",
                        Color(0xFF167565),
                        "$stepGoalPercent% of ${decimal.format(stepGoal)} goal",
                        MetricGraph.StepsProgress,
                        stepProgress
                    )
                )
            }
            data.averageHeartRateBpm?.let {
                add(
                    Metric(
                        MetricType.HeartRate,
                        "Heart Rate",
                        it.toString(),
                        "bpm avg",
                        "♡",
                        Color(0xFFE9656D),
                        "Selected range average",
                        MetricGraph.HeartRateBars
                    )
                )
            }
            if (data.sleepMinutes > 0) {
                add(
                    Metric(
                        MetricType.Sleep,
                        "Sleep",
                        "${data.sleepMinutes / 60}h ${data.sleepMinutes % 60}m",
                        "sleep duration",
                        "☾",
                        Color(0xFF6576E8),
                        "Selected range"
                    )
                )
            }
            if (data.activeCaloriesKcal > 0) {
                add(
                    Metric(
                        MetricType.ActiveCalories,
                        "Active Calories",
                        decimal.format(data.activeCaloriesKcal),
                        "kcal",
                        "♨",
                        Color(0xFFEFA92E),
                        "Selected range"
                    )
                )
            }
            if (data.distanceMeters > 0) {
                add(
                    Metric(
                        MetricType.Distance,
                        "Distance",
                        "${decimal.format(data.distanceKilometers)} km",
                        "distance",
                        "↗",
                        Color(0xFF4E9BE8),
                        "Selected range"
                    )
                )
            }
            if (data.exerciseMinutes > 0) {
                add(
                    Metric(
                        MetricType.Exercise,
                        "Exercise",
                        "${data.exerciseMinutes} min",
                        "exercise time",
                        "✦",
                        Color(0xFFB276E8),
                        "Selected range"
                    )
                )
            }
            if (data.totalCaloriesKcal > 0) {
                add(
                    Metric(
                        MetricType.TotalCalories,
                        "Total Calories",
                        decimal.format(data.totalCaloriesKcal),
                        "kcal",
                        "♨",
                        Color(0xFFE58B45),
                        "Selected range"
                    )
                )
            }
        }
    }
    val displayMetrics = if (showGraphs) {
        metrics
    } else {
        metrics.map { it.copy(graph = MetricGraph.None, progress = null) }
    }
    if (displayMetrics.isEmpty()) {
        Text(
            "No health data is available for this range.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            displayMetrics.chunked(2).forEach { rowMetrics ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowMetrics.forEach { metric ->
                        MetricCard(metric, Modifier.weight(1f), onMetricClick)
                    }
                    if (rowMetrics.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    metric: Metric,
    modifier: Modifier = Modifier,
    onClick: (MetricType) -> Unit = {}
) {
    Card(
        modifier = modifier.clickable { onClick(metric.type) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .height(124.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    metric.title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Badge(metric.icon, metric.tint.copy(alpha = .12f), metric.tint, 28.dp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                metric.value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                metric.unit,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            when (metric.graph) {
                MetricGraph.StepsProgress -> LinearProgressIndicator(
                    progress = { metric.progress ?: 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = metric.tint,
                    trackColor = MaterialTheme.colorScheme.background
                )

                MetricGraph.HeartRateBars -> HeartRateBars(metric.tint)
                MetricGraph.None -> Spacer(Modifier.height(5.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                metric.footer,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HeartRateBars(color: Color) {
    val bars = listOf(0.42f, 0.62f, 0.5f, 0.7f, 0.55f, 0.9f, 0.58f, 0.76f, 0.68f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { height ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((18f * height).dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun LogoMark() {
    Badge("✣", Color(0xFFD4EFEB), Color(0xFF167565), 48.dp)
}

@Composable
private fun Badge(
    text: String,
    background: Color,
    foreground: Color,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    onClick: (() -> Unit)? = null
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = foreground, fontWeight = FontWeight.Bold)
    }
}

private fun previewSnapshot() = HealthSnapshot(
    range = HealthRange(
        option = RangeOption.Today,
        start = Instant.parse("2026-09-08T00:00:00Z"),
        end = Instant.parse("2026-09-08T12:00:00Z")
    ),
    steps = 8_432,
    distanceMeters = 6_240.0,
    activeCaloriesKcal = 384.0,
    totalCaloriesKcal = 1_820.0,
    exerciseMinutes = 42,
    averageHeartRateBpm = 72,
    sleepMinutes = 443
)

@Composable
private fun PreviewSurface(darkTheme: Boolean, content: @Composable () -> Unit) {
    FitBriefTheme(darkTheme = darkTheme) {
        Surface { content() }
    }
}

@Preview(name = "Onboarding Light", showBackground = true)
@Composable
private fun OnboardingLightPreview() {
    PreviewSurface(false) {
        OnboardingScreen(
            FitBriefUiState(
                permissionStatus = PermissionStatus(
                    HealthConnectAvailability.Available,
                    false,
                    0,
                    7
                )
            ), {}, {})
    }
}

@Preview(name = "Onboarding Dark", showBackground = true)
@Composable
private fun OnboardingDarkPreview() {
    PreviewSurface(true) {
        OnboardingScreen(
            FitBriefUiState(
                permissionStatus = PermissionStatus(
                    HealthConnectAvailability.Available,
                    false,
                    0,
                    7
                )
            ), {}, {})
    }
}

@Preview(name = "Dashboard Light", showBackground = true)
@Composable
private fun DashboardLightPreview() {
    PreviewSurface(false) {
        DashboardScreen(
            FitBriefUiState(
                snapshot = previewSnapshot(),
                summary = "Your movement and recovery are building a steady rhythm."
            ), {}, {}, {}, {}, {})
    }
}

@Preview(name = "Dashboard Dark", showBackground = true)
@Composable
private fun DashboardDarkPreview() {
    PreviewSurface(true) {
        DashboardScreen(
            FitBriefUiState(
                snapshot = previewSnapshot(),
                summary = "Your movement and recovery are building a steady rhythm."
            ), {}, {}, {}, {}, {})
    }
}

@Preview(name = "Summary Light", showBackground = true)
@Composable
private fun SummaryLightPreview() {
    PreviewSurface(false) {
        SummaryDetailScreen(
            FitBriefUiState(
                snapshot = previewSnapshot(),
                summary = "You recorded a balanced day of movement and recovery."
            ), {}, {})
    }
}

@Preview(name = "Summary Dark", showBackground = true)
@Composable
private fun SummaryDarkPreview() {
    PreviewSurface(true) {
        SummaryDetailScreen(
            FitBriefUiState(
                snapshot = previewSnapshot(),
                summary = "You recorded a balanced day of movement and recovery."
            ), {}, {})
    }
}

@Preview(name = "Settings Light", showBackground = true)
@Composable
private fun SettingsLightPreview() {
    PreviewSurface(false) {
        SettingsScreen(
            state = FitBriefUiState(showSettings = true),
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
            state = FitBriefUiState(showSettings = true),
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
