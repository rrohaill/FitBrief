package dev.rrohaill.fitbrief.summary

import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.TimelineEvent
import dev.rrohaill.fitbrief.ui.MetricType

interface SummaryService {
    suspend fun summarize(
        preferredBackend: SummarizerBackend,
        snapshot: HealthSnapshot,
        onProgress: (BackendProgress) -> Unit,
        onBackendFallback: (String) -> Unit
    ): FitBriefSummary

    suspend fun summarizeTimeline(
        backend: SummarizerBackend,
        snapshot: HealthSnapshot,
        events: List<TimelineEvent>
    ): List<TimelineEvent>

    suspend fun summarizeMetric(
        preferredBackend: SummarizerBackend,
        snapshot: HealthSnapshot,
        metric: MetricType,
        value: String
    ): String
}
