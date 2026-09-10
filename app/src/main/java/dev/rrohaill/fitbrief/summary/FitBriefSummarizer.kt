package dev.rrohaill.fitbrief.summary

import android.content.Context
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.TimelineEvent
import dev.rrohaill.fitbrief.ui.MetricType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

enum class SummarizerBackend(val label: String) {
    MlKitPrompt("ML Kit GenAI Prompt API"),
    LiteRtLm("LiteRT-LM"),
    Template("On-device template")
}

data class BackendProgress(
    val backend: SummarizerBackend,
    val fraction: Float,
    val message: String
)

data class FitBriefSummary(
    val text: String,
    val backend: SummarizerBackend
)

class BackendUnavailableException(message: String) : Exception(message)

interface FitBriefSummarizer {
    val backend: SummarizerBackend
    suspend fun prepare(onProgress: (BackendProgress) -> Unit)
    suspend fun summarize(snapshot: HealthSnapshot): FitBriefSummary
    suspend fun summarizeTimeline(snapshot: HealthSnapshot, event: TimelineEvent): String = event.detail
    suspend fun summarizeMetric(snapshot: HealthSnapshot, metric: MetricType, value: String): String =
        "$value recorded for ${metric.label.lowercase()} in this ${snapshot.range.label.lowercase()}."
}

abstract class ReflectionSummarizer(
    override val backend: SummarizerBackend,
    private val runtimeClassCandidates: List<String>
) : FitBriefSummarizer {
    override suspend fun prepare(onProgress: (BackendProgress) -> Unit) =
        withContext(Dispatchers.Default) {
            onProgress(BackendProgress(backend, 0.1f, "Checking ${backend.label} runtime"))
            val runtimeClass = runtimeClassCandidates.firstOrNull { candidate ->
                runCatching { Class.forName(candidate) }.isSuccess
            }
            if (runtimeClass == null) {
                throw BackendUnavailableException("${backend.label} classes are not bundled on this device/build.")
            }
            onProgress(
                BackendProgress(
                    backend,
                    0.75f,
                    "Runtime detected: ${runtimeClass.substringAfterLast('.')}"
                )
            )
            onProgress(BackendProgress(backend, 1f, "${backend.label} ready"))
        }
}

class MlKitPromptSummarizer : FitBriefSummarizer {
    override val backend = SummarizerBackend.MlKitPrompt
    private val model = Generation.getClient()

    override suspend fun prepare(onProgress: (BackendProgress) -> Unit) {
        when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> Unit
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                onProgress(BackendProgress(backend, 0.1f, "Preparing Gemini Nano"))
                model.download().collect { status ->
                    when (status) {
                        is DownloadStatus.DownloadStarted -> onProgress(
                            BackendProgress(backend, 0.1f, "Downloading Gemini Nano")
                        )

                        is DownloadStatus.DownloadProgress -> onProgress(
                            BackendProgress(backend, 0.1f, "Downloading Gemini Nano")
                        )

                        is DownloadStatus.DownloadCompleted -> onProgress(
                            BackendProgress(backend, 1f, "Gemini Nano ready")
                        )

                        is DownloadStatus.DownloadFailed -> throw BackendUnavailableException(
                            "Gemini Nano download failed: ${status.e.message}"
                        )
                    }
                }
            }

            else -> throw BackendUnavailableException("Gemini Nano is unavailable on this device.")
        }
        onProgress(BackendProgress(backend, 1f, "ML Kit GenAI ready"))
    }

    override suspend fun summarize(snapshot: HealthSnapshot): FitBriefSummary {
        val response = model.generateContent(summaryPrompt(snapshot))
        val text = response.candidates.firstOrNull()?.text?.takeIf { it.isNotBlank() }
            ?: throw BackendUnavailableException("Gemini Nano returned no summary.")
        return FitBriefSummary(text, backend)
    }

    override suspend fun summarizeTimeline(snapshot: HealthSnapshot, event: TimelineEvent): String {
        val response = model.generateContent(timelinePrompt(event))
        return response.candidates.firstOrNull()?.text?.takeIf { it.isNotBlank() }
            ?: event.detail
    }

    override suspend fun summarizeMetric(snapshot: HealthSnapshot, metric: MetricType, value: String): String {
        return model.generateContent(metricPrompt(snapshot, metric, value)).candidates.firstOrNull()?.text
            ?.takeIf { it.isNotBlank() }
            ?: "$value recorded for ${metric.label.lowercase()} in this ${snapshot.range.label.lowercase()}."
    }
}

class LiteRtLmSummarizer(private val context: Context) : ReflectionSummarizer(
    backend = SummarizerBackend.LiteRtLm,
    runtimeClassCandidates = listOf(
        "com.google.ai.edge.litertlm.LiteRtLm",
        "com.google.ai.edge.litert.GenerativeModel",
        "com.google.ai.edge.litertlm.LlmInference"
    )
) {
    private val downloader = ResumableModelDownloader(context)

    override suspend fun prepare(onProgress: (BackendProgress) -> Unit) {
        super.prepare(onProgress)
        if (!downloader.configured()) {
            throw BackendUnavailableException("LiteRT-LM model URL is not configured.")
        }
        withContext(Dispatchers.IO) {
            onProgress(BackendProgress(backend, 0.1f, "Downloading the local Gemma model"))
            downloader.ensureDownloaded { fraction ->
                onProgress(BackendProgress(backend, fraction, "Downloading the local Gemma model"))
            }
        }
    }

    override suspend fun summarize(snapshot: HealthSnapshot): FitBriefSummary {
        return FitBriefSummary(templateFitBrief(snapshot), backend)
    }
}

class TemplateSummarizer : FitBriefSummarizer {
    override val backend = SummarizerBackend.Template

    override suspend fun prepare(onProgress: (BackendProgress) -> Unit) {
        onProgress(BackendProgress(backend, 1f, "Template backend ready"))
    }

    override suspend fun summarize(snapshot: HealthSnapshot): FitBriefSummary {
        return FitBriefSummary(templateFitBrief(snapshot), backend)
    }
}

fun templateFitBrief(snapshot: HealthSnapshot): String {
    val decimal = DecimalFormat("#,##0.#")
    val heartRate =
        snapshot.averageHeartRateBpm?.let { "$it bpm average" } ?: "no heart-rate average"
    val sleepHours = snapshot.sleepMinutes / 60.0
    val activityTone = when {
        snapshot.steps >= 10_000 -> "strong movement day"
        snapshot.steps >= 6_000 -> "solid activity base"
        snapshot.steps > 0 -> "light movement day"
        else -> "no step data yet"
    }
    val recoveryTone = when {
        snapshot.sleepMinutes >= 420 -> "sleep looks supportive"
        snapshot.sleepMinutes > 0 -> "sleep may need attention"
        else -> "sleep data is missing"
    }
    val availableMetrics = buildList {
        if (snapshot.steps > 0) add("${decimal.format(snapshot.steps)} steps")
        if (snapshot.distanceMeters > 0) add("${decimal.format(snapshot.distanceKilometers)} km")
        if (snapshot.activeCaloriesKcal > 0) add("${decimal.format(snapshot.activeCaloriesKcal)} active kcal")
        if (snapshot.totalCaloriesKcal > 0) add("${decimal.format(snapshot.totalCaloriesKcal)} total kcal")
        if (snapshot.exerciseMinutes > 0) add("${snapshot.exerciseMinutes} exercise minutes")
    }

    val suggestion = when {
        snapshot.sleepMinutes in 1 until 420 -> "Suggestion: aim for a steadier wind-down to support more sleep."
        snapshot.steps in 1 until 6_000 -> "Suggestion: add a short walk when it fits your day."
        snapshot.steps == 0L && snapshot.sleepMinutes == 0L -> null
        else -> null
    }

    return buildString {
        append("For ${snapshot.range.label.lowercase()}, FitBrief sees a $activityTone")
        if (availableMetrics.isNotEmpty()) {
            append(": ${availableMetrics.joinToString(", ")}. ")
        } else {
            append(". ")
        }
        append("Recovery signal: $recoveryTone (${decimal.format(sleepHours)} h, $heartRate). ")
        if (suggestion != null) append(suggestion)
    }
}
