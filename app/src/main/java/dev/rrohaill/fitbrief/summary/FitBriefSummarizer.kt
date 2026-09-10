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
        val period = snapshot.range.label.lowercase()
        val nextPeriod = when (snapshot.range.option) {
            dev.rrohaill.fitbrief.data.RangeOption.Today -> "tomorrow"
            dev.rrohaill.fitbrief.data.RangeOption.SevenDays -> "next week"
            dev.rrohaill.fitbrief.data.RangeOption.ThirtyDays -> "next month"
        }
        val prompt = """
    You are FitBrief, a warm wellness companion. Write a short health recap for the selected $period, speaking directly to the user as "you".

    Use ONLY the data below. Do not invent, estimate, or mention any metric that is missing or zero. Never state medical facts, diagnose, or give medical advice.

    DATA:
    ${templateFitBrief(snapshot)}

    Write your response as plain text (no headings, no bullet points, no markdown) in this order:
    1. One or two sentences recapping this $period using the available movement, heart-rate, sleep, distance, calorie, and exercise figures. Reference the actual numbers.
    2. One short encouraging observation about something that went well.
    3. Add one short, gentle, actionable suggestion for $nextPeriod only when the data shows a clear opportunity to improve (for example, low movement or insufficient sleep). Prefix it with "Suggestion:". If no improvement is needed or the data is insufficient, omit the suggestion entirely.

    Keep the whole summary under 70 words. Be friendly and concrete, not generic. If very little data is available, keep it to a single encouraging sentence.
""".trimIndent()
        val response = model.generateContent(prompt)
        val text = response.candidates.firstOrNull()?.text?.takeIf { it.isNotBlank() }
            ?: throw BackendUnavailableException("Gemini Nano returned no summary.")
        return FitBriefSummary(text, backend)
    }

    override suspend fun summarizeTimeline(snapshot: HealthSnapshot, event: TimelineEvent): String {
        val start = event.timestamp.toString()
        val end = event.endTimestamp?.toString() ?: start
        val prompt = """
            You are FitBrief, turning one grouped health activity into a useful, non-medical insight.
            Write one or two friendly sentences in plain text. Use only the event data below.
            Explain what happened and call out the most useful pattern or change. If this activity
            shows a clear opportunity to improve, add one gentle actionable suggestion prefixed
            with "Suggestion:". Otherwise, omit any suggestion. Do not diagnose, speculate, or give
            medical advice. Do not mention missing data or this prompt.

            SELECTED RANGE: ${snapshot.range.label}
            ACTIVITY: ${event.title}
            TIME WINDOW: $start to $end
            ACTIVITY DATA: ${event.detail}
        """.trimIndent()
        val response = model.generateContent(prompt)
        return response.candidates.firstOrNull()?.text?.takeIf { it.isNotBlank() }
            ?: event.detail
    }

    override suspend fun summarizeMetric(snapshot: HealthSnapshot, metric: MetricType, value: String): String {
        val prompt = """
            You are FitBrief. Write one or two friendly, plain-language sentences about one health metric.
            Use only the supplied selected-range value. Be descriptive and motivational, never diagnostic.
            If the value shows a clear opportunity to improve, add one gentle actionable suggestion
            prefixed with "Suggestion:". Otherwise, omit any suggestion. Do not invent comparisons,
            causes, or missing values.

            RANGE: ${snapshot.range.label}
            METRIC: ${metric.label}
            VALUE: $value
        """.trimIndent()
        return model.generateContent(prompt).candidates.firstOrNull()?.text?.takeIf { it.isNotBlank() }
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
