package dev.rrohaill.fitbrief.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import dev.rrohaill.fitbrief.data.HealthRange
import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.ui.theme.FitBriefTheme
import java.time.Instant

internal fun previewSnapshot() = HealthSnapshot(
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
internal fun PreviewSurface(darkTheme: Boolean, content: @Composable () -> Unit) {
    FitBriefTheme(darkTheme = darkTheme) {
        Surface { content() }
    }
}
