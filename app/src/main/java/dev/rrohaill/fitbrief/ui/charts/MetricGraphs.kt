package dev.rrohaill.fitbrief.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import dev.rrohaill.fitbrief.data.RangeOption

@Composable
internal fun BarMetricGraph(values: List<Float>, color: Color, onBarClick: (Int) -> Unit) {
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(values) {
                detectTapGestures { offset ->
                    val slot = size.width / values.size.coerceAtLeast(1)
                    onBarClick((offset.x / slot).toInt().coerceIn(0, values.lastIndex))
                }
            }
    ) {
        val max = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val gap = size.width / (values.size * 2f)
        values.forEachIndexed { index, value ->
            val height = size.height * value / max
            drawRoundRect(color.copy(alpha = 0.55f + index * 0.04f),
                Offset(gap + index * gap * 2, size.height - height),
                androidx.compose.ui.geometry.Size(gap, height),
                androidx.compose.ui.geometry.CornerRadius(10f, 10f))
        }
    }
}

@Composable
internal fun DistanceMetricGraph(values: List<Float>, onBarClick: (Int) -> Unit) {
    TrendMetricGraph(values, Color(0xFF20B8A6), onBarClick)
}

@Composable
internal fun SleepDurationGraph(values: List<Float>, chartMax: Float, onBarClick: (Int) -> Unit) {
    val barColor = Color(0xFF8B7CFF)
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(values) {
                detectTapGestures { offset ->
                    val slot = size.width / values.size.coerceAtLeast(1)
                    onBarClick((offset.x / slot).toInt().coerceIn(0, values.lastIndex))
                }
            }
    ) {
        val bottom = size.height * 0.92f
        val top = size.height * 0.08f
        fun yFor(minutes: Float): Float =
            bottom - (bottom - top) * (minutes / chartMax).coerceIn(0f, 1f)

        drawRoundRect(
            color = Color(0xFF8B7CFF).copy(alpha = 0.12f),
            topLeft = Offset(0f, yFor(540f)),
            size = androidx.compose.ui.geometry.Size(size.width, yFor(420f) - yFor(540f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
        )
        for (index in 0..3) {
            val y = top + (bottom - top) * index / 3f
            drawLine(
                color = barColor.copy(alpha = 0.12f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        val slot = size.width / values.size.coerceAtLeast(1)
        val barWidth = (slot * 0.58f).coerceAtLeast(12f)
        values.forEachIndexed { index, value ->
            val barHeight = bottom - yFor(value)
            val x = slot * index + (slot - barWidth) / 2f
            drawRoundRect(
                color = barColor.copy(alpha = 0.72f + (index % 3) * 0.08f),
                topLeft = Offset(x, yFor(value)),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
            )
            drawCircle(
                color = barColor,
                radius = 4f,
                center = Offset(x + barWidth / 2f, yFor(value))
            )
        }
        drawLine(
            color = barColor.copy(alpha = 0.35f),
            start = Offset(0f, bottom),
            end = Offset(size.width, bottom),
            strokeWidth = 2f
        )
    }
}

@Composable
private fun TrendMetricGraph(values: List<Float>, lineColor: Color, onBarClick: (Int) -> Unit) {
    val fillTop = lineColor.copy(alpha = 0.34f)
    val fillBottom = lineColor.copy(alpha = 0.02f)
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(values) {
                detectTapGestures { offset ->
                    val slot = size.width / values.size.coerceAtLeast(1)
                    onBarClick((offset.x / slot).toInt().coerceIn(0, values.lastIndex))
                }
            }
    ) {
        val chartMax = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val bottom = size.height * 0.92f
        val top = size.height * 0.12f
        val step = size.width / (values.size - 1).coerceAtLeast(1)
        val points = values.mapIndexed { index, value ->
            Offset(
                x = if (values.size == 1) size.width / 2f else index * step,
                y = bottom - (bottom - top) * (value / chartMax).coerceIn(0f, 1f)
            )
        }

        for (index in 0..3) {
            val y = top + (bottom - top) * index / 3f
            drawLine(
                color = lineColor.copy(alpha = 0.12f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        if (points.isNotEmpty()) {
            val area = androidx.compose.ui.graphics.Path().apply {
                moveTo(points.first().x, bottom)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, bottom)
                close()
            }
            drawPath(
                area,
                brush = Brush.verticalGradient(
                    colors = listOf(fillTop, fillBottom),
                    startY = top,
                    endY = bottom
                )
            )
            points.zipWithNext().forEach { (start, end) ->
                drawLine(lineColor, start, end, strokeWidth = 5f, cap = StrokeCap.Round)
            }
            points.forEach { point ->
                drawCircle(Color.White, radius = 7f, center = point)
                drawCircle(lineColor, radius = 4f, center = point)
            }
        }
        drawLine(
            color = lineColor.copy(alpha = 0.3f),
            start = Offset(0f, bottom),
            end = Offset(size.width, bottom),
            strokeWidth = 2f
        )
    }
}

@Composable
internal fun CaloriesMetricGraph(values: List<Float>, color: Color, onBarClick: (Int) -> Unit) {
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(values) {
                detectTapGestures { offset ->
                    val slot = size.width / values.size.coerceAtLeast(1)
                    onBarClick((offset.x / slot).toInt().coerceIn(0, values.lastIndex))
                }
            }
    ) {
        val top = size.height * 0.08f
        val bottom = size.height * 0.92f
        val maxValue = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val slot = size.width / values.size.coerceAtLeast(1)
        val barWidth = (slot * 0.58f).coerceAtLeast(12f)
        values.forEachIndexed { index, value ->
            val height = (bottom - top) * (value / maxValue).coerceIn(0f, 1f)
            val x = slot * index + (slot - barWidth) / 2f
            val y = bottom - height
            drawRoundRect(
                color = color.copy(alpha = 0.16f),
                topLeft = Offset(x - 4f, y - 4f),
                size = androidx.compose.ui.geometry.Size(barWidth + 8f, height + 4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = color.copy(alpha = 0.78f + (index % 3) * 0.07f),
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 3.5f,
                center = Offset(x + barWidth / 2f, y)
            )
        }
        for (index in 0..3) {
            val y = top + (bottom - top) * index / 3f
            drawLine(
                color = color.copy(alpha = 0.12f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }
        drawLine(
            color = color.copy(alpha = 0.35f),
            start = Offset(0f, bottom),
            end = Offset(size.width, bottom),
            strokeWidth = 2f
        )
    }
}

@Composable
internal fun HeartRateMetricGraph(values: List<Float>, chartMin: Float, chartMax: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val pointCount = values.size.coerceAtLeast(1)
        val normalizedValues = List(pointCount) { index ->
            val source = values[index.coerceAtMost(values.lastIndex)]
            ((source - chartMin) / (chartMax - chartMin)).coerceIn(0f, 1f)
        }

        val points = normalizedValues.mapIndexed { index, normalized ->
            Offset(
                x = if (pointCount == 1) size.width / 2f
                else size.width * index / (pointCount - 1),
                y = size.height * (0.82f - normalized * 0.56f)
            )
        }
            val zones = listOf(
                chartMax to Color(0xFFE9656D),
                chartMin + (chartMax - chartMin) * 0.78f to Color(0xFFFFB52E),
                chartMin + (chartMax - chartMin) * 0.58f to Color(0xFF4FD477),
                chartMin + (chartMax - chartMin) * 0.32f to Color(0xFF32C7F0)
            )
            zones.forEach { (bpm, zoneColor) ->
                val y = size.height * (0.82f - ((bpm - chartMin) / (chartMax - chartMin)).coerceIn(0f, 1f) * 0.56f)
                var x = 0f
                while (x < size.width) {
                    drawCircle(zoneColor.copy(alpha = 0.9f), 1.5f, Offset(x, y))
                    x += 9f
                }
            }
        points.forEachIndexed { index, point ->
            val barHeight = size.height * (0.08f + normalizedValues[index] * 0.35f)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.14f),
                    topLeft = Offset(point.x - 2f, point.y - barHeight),
                    size = androidx.compose.ui.geometry.Size(4f, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                )
        }
        points.zipWithNext().forEach { (start, end) ->
            drawLine(Color(0xFF20C7F2), start, end, strokeWidth = 4f, cap = StrokeCap.Round)
        }
    }
}

@Composable
internal fun HeartRatePeriodGraph(values: List<Float>, range: RangeOption?, color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val chartValues = values.takeLast(if (range == RangeOption.SevenDays) 7 else 5)
        val minValue = chartValues.minOrNull()?.coerceAtLeast(1f) ?: 1f
        val maxValue = chartValues.maxOrNull()?.coerceAtLeast(minValue + 1f) ?: 1f
        val step = size.width / (chartValues.size - 1).coerceAtLeast(1)
        val points = chartValues.mapIndexed { index, value ->
            Offset(index * step, size.height * (0.84f - ((value - minValue) / (maxValue - minValue)) * 0.58f))
        }
        points.zipWithNext().forEach { (start, end) ->
            drawLine(color, start, end, strokeWidth = 4f, cap = StrokeCap.Round)
        }
        points.forEach { drawCircle(color, 6f, it) }
    }
}

@Composable
internal fun ExerciseDurationGraph(
    values: List<Float>,
    chartMax: Float,
    color: Color,
    onBarClick: (Int) -> Unit
) {
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(values) {
                detectTapGestures { offset ->
                    val slot = size.width / values.size.coerceAtLeast(1)
                    onBarClick((offset.x / slot).toInt().coerceIn(0, values.lastIndex))
                }
            }
    ) {
        val top = size.height * 0.08f
        val bottom = size.height * 0.92f
        fun yFor(minutes: Float): Float =
            bottom - (bottom - top) * (minutes / chartMax).coerceIn(0f, 1f)

        for (index in 0..3) {
            val y = top + (bottom - top) * index / 3f
            drawLine(
                color = color.copy(alpha = 0.12f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }
        val targetY = yFor(30f)
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = color.copy(alpha = 0.65f),
                start = Offset(x, targetY),
                end = Offset((x + 8f).coerceAtMost(size.width), targetY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            x += 14f
        }

        val slot = size.width / values.size.coerceAtLeast(1)
        val barWidth = (slot * 0.56f).coerceAtLeast(12f)
        values.forEachIndexed { index, value ->
            val barHeight = bottom - yFor(value)
            val barX = slot * index + (slot - barWidth) / 2f
            drawRoundRect(
                color = color.copy(alpha = 0.72f + (index % 3) * 0.08f),
                topLeft = Offset(barX, yFor(value)),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = Offset(barX + barWidth / 2f, yFor(value))
            )
        }
    }
}
