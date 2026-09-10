package dev.rrohaill.fitbrief.summary

import android.content.Context
import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.TimelineEvent
import dev.rrohaill.fitbrief.ui.MetricType

class SummarizerFactory(private val context: Context) : SummaryService {
    override suspend fun summarize(
        preferredBackend: SummarizerBackend,
        snapshot: HealthSnapshot,
        onProgress: (BackendProgress) -> Unit,
        onBackendFallback: (String) -> Unit
    ): FitBriefSummary {
        // Backend selection deliberately probes unstable GenAI runtimes with reflection so the app still compiles without preview artifacts.
        val candidates = buildList {
            add(preferredBackend)
            add(SummarizerBackend.MlKitPrompt)
            add(SummarizerBackend.LiteRtLm)
            add(SummarizerBackend.Template)
        }.distinct()

        val failures = mutableListOf<String>()
        for (backend in candidates) {
            val summarizer = create(backend)
            try {
                summarizer.prepare(onProgress)
                if (backend != preferredBackend) {
                    onBackendFallback("Using ${backend.label}; ${preferredBackend.label} was unavailable.")
                }

                return summarizer.summarize(snapshot)
            } catch (unavailable: BackendUnavailableException) {
                failures += unavailable.message ?: "${backend.label} unavailable"
            }
        }

        error("No summarizer backend available: ${failures.joinToString()}")
    }

    override suspend fun summarizeTimeline(
        backend: SummarizerBackend,
        snapshot: HealthSnapshot,
        events: List<TimelineEvent>
    ): List<TimelineEvent> {
        if (events.isEmpty()) return events
        val summarizer = create(backend)
        return events.map { event ->
            if (isTrivialWindow(event)) return@map event
            val insight = runCatching {
                summarizer.summarizeTimeline(snapshot, event)
            }.getOrNull()
            event.copy(detail = insight?.takeIf { it.isNotBlank() } ?: event.detail)
        }
    }

    override suspend fun summarizeMetric(
        preferredBackend: SummarizerBackend,
        snapshot: HealthSnapshot,
        metric: MetricType,
        value: String
    ): String {
        val candidates = listOf(
            preferredBackend,
            SummarizerBackend.MlKitPrompt,
            SummarizerBackend.LiteRtLm,
            SummarizerBackend.Template
        ).distinct()
        for (backend in candidates) {
            try {
                val summarizer = create(backend)
                summarizer.prepare {}
                return summarizer.summarizeMetric(snapshot, metric, value)
            } catch (_: BackendUnavailableException) {
                continue
            }
        }
        return "$value recorded for ${metric.label.lowercase()} in this ${snapshot.range.label.lowercase()}."
    }

    private fun create(backend: SummarizerBackend): FitBriefSummarizer {
        return when (backend) {
            SummarizerBackend.MlKitPrompt -> MlKitPromptSummarizer()
            SummarizerBackend.LiteRtLm -> LiteRtLmSummarizer(context)
            SummarizerBackend.Template -> TemplateSummarizer()
        }
    }
}
