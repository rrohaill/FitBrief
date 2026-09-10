package dev.rrohaill.fitbrief.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun Badge(
    text: String,
    background: Color,
    foreground: Color,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    onClick: (() -> Unit)? = null
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = foreground, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun LogoMark() {
    Badge("✣", Color(0xFFD4EFEB), Color(0xFF167565), 48.dp)
}
