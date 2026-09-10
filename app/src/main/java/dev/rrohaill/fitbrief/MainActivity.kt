package dev.rrohaill.fitbrief

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.health.connect.client.PermissionController
import dev.rrohaill.fitbrief.data.healthConnectProviderPackage
import dev.rrohaill.fitbrief.ui.FitBriefApp
import dev.rrohaill.fitbrief.ui.FitBriefViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FitBriefViewModel by viewModels()

    private val healthPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted -> viewModel.onHealthPermissionsResult(granted) }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.scheduleNotifications() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitBriefApp(
                viewModel = viewModel,
                onRequestHealthPermissions = {
                    healthPermissionLauncher.launch(viewModel.requiredPermissions)
                },
                onOpenHealthConnect = ::openHealthConnect,
                onScheduleNotifications = ::scheduleNotifications,
                onShareSummary = ::shareSummary
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshOnAppOpen()
    }

    private fun scheduleNotifications() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.scheduleNotifications()
        }
    }

    private fun shareSummary(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, "Share FitBrief summary"))
    }

    private fun openHealthConnect() {
        val settingsIntent = Intent("android.health.connect.action.HEALTH_CONNECT_SETTINGS")
        if (settingsIntent.resolveActivity(packageManager) != null) {
            startActivity(settingsIntent)
            return
        }
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$healthConnectProviderPackage".toUri()))
        }.onFailure {
            startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$healthConnectProviderPackage".toUri()))
        }
    }
}
