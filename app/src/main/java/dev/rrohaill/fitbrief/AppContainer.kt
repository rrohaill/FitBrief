package dev.rrohaill.fitbrief

import android.content.Context
import dev.rrohaill.fitbrief.data.FitBriefPreferences
import dev.rrohaill.fitbrief.data.FitBriefPreferencesStore
import dev.rrohaill.fitbrief.data.HealthConnectRepository
import dev.rrohaill.fitbrief.data.HealthRepository
import dev.rrohaill.fitbrief.notifications.NotificationScheduler
import dev.rrohaill.fitbrief.notifications.WorkManagerNotificationScheduler
import dev.rrohaill.fitbrief.summary.SummarizerFactory
import dev.rrohaill.fitbrief.summary.SummaryService

/** Hand-rolled dependency graph. One instance lives on [FitBriefApplication]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val healthRepository: HealthRepository by lazy { HealthConnectRepository(appContext) }
    val preferences: FitBriefPreferencesStore by lazy { FitBriefPreferences(appContext) }
    val summaryService: SummaryService by lazy { SummarizerFactory(appContext) }
    val notificationScheduler: NotificationScheduler by lazy { WorkManagerNotificationScheduler(appContext) }
}
