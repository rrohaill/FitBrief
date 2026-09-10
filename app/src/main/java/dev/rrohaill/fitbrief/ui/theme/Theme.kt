package dev.rrohaill.fitbrief.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF167565),
    onPrimary = Color.White,
    secondary = Color(0xFF4D635F),
    tertiary = Color(0xFF38656A),
    background = Color(0xFFF8FAF9),
    surface = Color(0xFFF8FAF9),
    surfaceContainer = Color(0xFFEEF3F1)
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF8BD8C7),
    secondary = Color(0xFFB4CCBA),
    tertiary = Color(0xFFA0CFD4),
    background = Color(0xFF101510),
    surface = Color(0xFF101510),
    surfaceContainer = Color(0xFF1C241D)
)

@Composable
fun FitBriefTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
