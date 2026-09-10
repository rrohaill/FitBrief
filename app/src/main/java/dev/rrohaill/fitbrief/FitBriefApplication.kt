package dev.rrohaill.fitbrief

import android.app.Application
import dev.rrohaill.fitbrief.notifications.FitBriefWorkScheduler
import dev.rrohaill.fitbrief.data.FitBriefPreferences

class FitBriefApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FitBriefWorkScheduler.schedule(this, FitBriefPreferences(this).refreshInterval().minutes.toLong())
    }
}
