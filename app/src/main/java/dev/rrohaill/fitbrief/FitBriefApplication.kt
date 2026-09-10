package dev.rrohaill.fitbrief

import android.app.Application

class FitBriefApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationScheduler.schedule(container.preferences.refreshInterval().minutes.toLong())
    }
}
