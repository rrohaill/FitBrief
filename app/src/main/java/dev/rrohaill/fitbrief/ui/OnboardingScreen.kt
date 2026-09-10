package dev.rrohaill.fitbrief.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.rrohaill.fitbrief.data.HealthConnectAvailability
import dev.rrohaill.fitbrief.data.PermissionStatus
import dev.rrohaill.fitbrief.ui.components.AiPulseBackground
import dev.rrohaill.fitbrief.ui.components.Badge
import dev.rrohaill.fitbrief.ui.components.LogoMark
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
internal fun OnboardingScreen(
    state: FitBriefUiState,
    onRequestHealthPermissions: () -> Unit,
    onOpenHealthConnect: () -> Unit
) {
    var contentVisible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(650),
        label = "onboarding-content-alpha"
    )
    val contentOffset by animateDpAsState(
        targetValue = if (contentVisible) 0.dp else 28.dp,
        animationSpec = tween(650),
        label = "onboarding-content-offset"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0.92f,
        animationSpec = tween(650),
        label = "onboarding-content-scale"
    )
    LaunchedEffect(Unit) {
        delay(1_000.milliseconds)
        contentVisible = true
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.fillMaxSize()) {
            AiPulseBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 44.dp)
                    .offset { IntOffset(0, contentOffset.roundToPx()) }
                    .scale(contentScale)
                    .alpha(contentAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(18.dp))
                LogoMark()
                Spacer(Modifier.height(30.dp))
                Text(
                    "Your health,\nsummarized.",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.displaySmall.lineHeight
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "FitBrief securely connects to Google Health Connect and creates short, actionable AI summaries of your daily vitals.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(26.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Badge(
                            "⊗",
                            MaterialTheme.colorScheme.primary.copy(alpha = .08f),
                            MaterialTheme.colorScheme.primary
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(end = 16.dp)
                        ) {
                            Text("On-device Privacy First", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Your physical activity, sleep, and heart metrics stay on your phone. Processing is 100% private and on-device.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = state.permissionStatus.availability == HealthConnectAvailability.Available,
                    onClick = onRequestHealthPermissions
                ) {
                    Text("Connect Health Connect", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenHealthConnect
                ) {
                    Text(
                        if (state.permissionStatus.availability == HealthConnectAvailability.Available)
                            "Maybe later"
                        else "Install Health Connect",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Preview(name = "Onboarding Light", showBackground = true)
@Composable
private fun OnboardingLightPreview() {
    PreviewSurface(false) {
        OnboardingScreen(
            FitBriefUiState(
                permissionStatus = PermissionStatus(
                    HealthConnectAvailability.Available,
                    false,
                    0,
                    7
                )
            ), {}, {})
    }
}

@Preview(name = "Onboarding Dark", showBackground = true)
@Composable
private fun OnboardingDarkPreview() {
    PreviewSurface(true) {
        OnboardingScreen(
            FitBriefUiState(
                permissionStatus = PermissionStatus(
                    HealthConnectAvailability.Available,
                    false,
                    0,
                    7
                )
            ), {}, {})
    }
}
