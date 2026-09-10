package dev.rrohaill.fitbrief.ui

import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.data.TimelineEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SummaryShareTextTest {
    @Test
    fun `share text has a header, the summary and one line per timeline event`() {
        val state = FitBriefUiState(
            selectedRange = RangeOption.SevenDays,
            summary = "A steady week.",
            timeline = listOf(
                TimelineEvent(Instant.EPOCH, "Walk", "3,000 steps", "♧"),
                TimelineEvent(Instant.EPOCH, "Sleep", "7h 10m", "☾")
            )
        )
        assertEquals(
            """
            |FitBrief 7 days summary
            |
            |A steady week.
            |
            |Activity timeline
            |Walk: 3,000 steps
            |Sleep: 7h 10m
            |""".trimMargin(),
            buildSummaryShareText(state)
        )
    }

    @Test
    fun `missing summary and timeline fall back to a placeholder`() {
        assertEquals(
            "FitBrief Today summary\n\nAI summary is not available yet.\n",
            buildSummaryShareText(FitBriefUiState())
        )
    }
}
