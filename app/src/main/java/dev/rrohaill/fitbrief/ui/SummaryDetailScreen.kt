package dev.rrohaill.fitbrief.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.rrohaill.fitbrief.ui.components.Badge
import dev.rrohaill.fitbrief.ui.components.TimelineEventCard

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
