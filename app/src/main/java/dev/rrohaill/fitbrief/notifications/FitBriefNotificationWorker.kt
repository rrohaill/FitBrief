package dev.rrohaill.fitbrief.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.rrohaill.fitbrief.MainActivity
import dev.rrohaill.fitbrief.R
import java.util.concurrent.TimeUnit

private const val FITBRIEF_WORK_NAME = "daily-fitbrief-summary"
private const val FITBRIEF_CHANNEL_ID = "fitbrief_summary"
private const val FITBRIEF_NOTIFICATION_ID = 1001

object FitBriefWorkScheduler {
    fun schedule(context: Context, intervalMinutes: Long = 24 * 60L) {
        val safeInterval = intervalMinutes.coerceAtLeast(15L)
        val request = PeriodicWorkRequestBuilder<FitBriefNotificationWorker>(safeInterval, TimeUnit.MINUTES)
            .setInitialDelay(safeInterval, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            FITBRIEF_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

class FitBriefNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        createNotificationChannel()
        val launchIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, FITBRIEF_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.notification_title))
            .setContentText(applicationContext.getString(R.string.notification_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(applicationContext.getString(R.string.notification_text)))
            .setContentIntent(launchIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(FITBRIEF_NOTIFICATION_ID, notification)
        return Result.success()
    }

    private fun createNotificationChannel() {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            FITBRIEF_CHANNEL_ID,
            applicationContext.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }
}
