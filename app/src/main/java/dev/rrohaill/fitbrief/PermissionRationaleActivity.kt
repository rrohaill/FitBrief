package dev.rrohaill.fitbrief

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rrohaill.fitbrief.ui.theme.FitBriefTheme

class PermissionRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitBriefTheme {
                Surface {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Why FitBrief needs Health Connect", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("FitBrief requests read-only access to steps, distance, calories, exercise, heart-rate, and sleep records.")
                        Text("The app reads aggregated totals and averages for the range you choose, then generates a private on-device wellness summary.")
                    }
                }
            }
        }
    }
}
