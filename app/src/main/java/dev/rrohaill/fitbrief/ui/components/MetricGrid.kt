package dev.rrohaill.fitbrief.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.rrohaill.fitbrief.data.HealthSnapshot
import dev.rrohaill.fitbrief.ui.MetricType
import dev.rrohaill.fitbrief.ui.metrics.MetricCardGraph
import dev.rrohaill.fitbrief.ui.metrics.MetricCardModel
import dev.rrohaill.fitbrief.ui.metrics.buildMetricCards

@Composable
internal fun MetricGrid(
    snapshot: HealthSnapshot?,
    showGraphs: Boolean = true,
    onMetricClick: (MetricType) -> Unit = {}
) {
    val displayMetrics = remember(snapshot, showGraphs) { buildMetricCards(snapshot, showGraphs) }
    if (displayMetrics.isEmpty()) {
        Text(
            "No health data is available for this range.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            displayMetrics.chunked(2).forEach { rowMetrics ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowMetrics.forEach { metric ->
                        MetricCard(metric, Modifier.weight(1f), onMetricClick)
                    }
                    if (rowMetrics.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    metric: MetricCardModel,
    modifier: Modifier = Modifier,
    onClick: (MetricType) -> Unit = {}
) {
    Card(
        modifier = modifier.clickable { onClick(metric.type) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .height(124.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    metric.title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                val tint = Color(metric.tintArgb)
                Badge(metric.icon, tint.copy(alpha = .12f), tint, 28.dp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                metric.value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                metric.unit,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            when (metric.graph) {
                MetricCardGraph.StepsProgress -> LinearProgressIndicator(
                    progress = { metric.progress ?: 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = Color(metric.tintArgb),
                    trackColor = MaterialTheme.colorScheme.background
                )

                MetricCardGraph.HeartRateBars -> HeartRateBars(Color(metric.tintArgb))
                MetricCardGraph.None -> Spacer(Modifier.height(5.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                metric.footer,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HeartRateBars(color: Color) {
    val bars = listOf(0.42f, 0.62f, 0.5f, 0.7f, 0.55f, 0.9f, 0.58f, 0.76f, 0.68f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { height ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((18f * height).dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(color)
            )
        }
    }
}
