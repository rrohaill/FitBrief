package dev.rrohaill.fitbrief.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
internal fun AiPulseBackground() {
    val transition = rememberInfiniteTransition(label = "ai-pulse-bg")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3_600),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val primary = MaterialTheme.colorScheme.primary
    val ambient = MaterialTheme.colorScheme.onSurface

    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.56f, size.height * 0.30f)
        val radius = 62f + pulse * 82f
        drawCircle(primary.copy(alpha = 0.045f + (1f - pulse) * 0.07f), radius)
        drawCircle(
            primary.copy(alpha = 0.22f * (1f - pulse)),
            radius = radius + 22f,
            style = Stroke(width = 1.2f)
        )
        drawCircle(
            ambient.copy(alpha = 0.045f),
            radius = 118f,
            style = Stroke(
                width = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(
                        3f,
                        7f
                    )
                )
            )
        )
        drawCircle(
            primary.copy(alpha = 0.18f),
            radius = 4f,
            center = Offset(size.width * (0.14f + pulse * 0.58f), size.height * 0.84f)
        )
        drawCircle(
            primary.copy(alpha = 0.12f),
            radius = 3f,
            center = Offset(size.width * 0.73f, size.height * 0.78f)
        )
        drawCircle(
            Color(0xFFE5B96B).copy(alpha = 0.3f),
            radius = 4f,
            center = Offset(size.width * 0.72f, size.height * 0.76f)
        )
        drawLine(
            ambient.copy(alpha = 0.04f),
            Offset(0f, size.height * 0.48f),
            Offset(size.width, size.height * 0.48f),
            strokeWidth = 1f
        )
        drawLine(
            primary.copy(alpha = 0.035f),
            Offset(size.width * 0.25f, size.height * 0.18f),
            Offset(size.width * 0.16f, size.height * 0.54f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}
