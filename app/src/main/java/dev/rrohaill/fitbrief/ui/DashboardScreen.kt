package dev.rrohaill.fitbrief.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.data.notable
import dev.rrohaill.fitbrief.ui.components.AiPulseBackground
import dev.rrohaill.fitbrief.ui.components.Badge
import dev.rrohaill.fitbrief.ui.components.MetricGrid
import dev.rrohaill.fitbrief.ui.components.TimelineEventCard
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
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
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                AiPulseBackground()
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(dashboardScrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                val notableTimeline = remember(state.timeline) { state.timeline.notable() }
                if (notableTimeline.isEmpty()) {
                    Text(
                        "No notable activity is available for this range.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    notableTimeline.forEach { event ->
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
            RangeOption.Week to "Week",
            RangeOption.Month to "Month"
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
