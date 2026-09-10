package dev.rrohaill.fitbrief.ui

/** Plain-text rendering of the current summary and timeline for the system share sheet. */
fun buildSummaryShareText(state: FitBriefUiState): String = buildString {
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
