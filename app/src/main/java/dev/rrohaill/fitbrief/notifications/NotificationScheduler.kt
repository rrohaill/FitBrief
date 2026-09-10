package dev.rrohaill.fitbrief.notifications

import android.content.Context

interface NotificationScheduler {
    fun schedule(intervalMinutes: Long = DEFAULT_INTERVAL_MINUTES)

    companion object {
        const val DEFAULT_INTERVAL_MINUTES: Long = 24 * 60L
    }
}

class WorkManagerNotificationScheduler(private val context: Context) : NotificationScheduler {
    override fun schedule(intervalMinutes: Long) = FitBriefWorkScheduler.schedule(context, intervalMinutes)
}
