package dev.rrohaill.fitbrief.data

import android.content.Context
import androidx.core.content.edit

enum class ThemeMode(val label: String) {
    System("System default"),
    Light("Light"),
    Dark("Dark")
}

enum class RefreshInterval(val minutes: Int, val label: String) {
    ThirtyMinutes(30, "Every 30 minutes"),
    OneHour(60, "Every hour"),
    TwoHours(120, "Every 2 hours"),
    FourHours(240, "Every 4 hours"),
    EightHours(480, "Every 8 hours"),
    Daily(1440, "Once a day")
}

class FitBriefPreferences(context: Context) : FitBriefPreferencesStore {
    private val preferences = context.getSharedPreferences("fitbrief_preferences", Context.MODE_PRIVATE)

    override fun themeMode(): ThemeMode =
        preferences.getString("theme_mode", ThemeMode.System.name)
            ?.let { value -> ThemeMode.entries.firstOrNull { it.name == value } }
            ?: ThemeMode.System

    override fun setThemeMode(mode: ThemeMode) {
        preferences.edit { putString("theme_mode", mode.name) }
    }

    override fun refreshInterval(): RefreshInterval {
        val storedMinutes = preferences.getInt("refresh_interval_minutes", 240)
        return RefreshInterval.entries.firstOrNull { it.minutes == storedMinutes }
            ?: RefreshInterval.FourHours
    }

    override fun setRefreshInterval(interval: RefreshInterval) {
        preferences.edit { putInt("refresh_interval_minutes", interval.minutes) }
    }

    override fun dailySummaryTimeMinutes(): Int =
        preferences.getInt("daily_summary_time_minutes", 8 * 60)

    override fun setDailySummaryTimeMinutes(minutes: Int) {
        preferences.edit { putInt("daily_summary_time_minutes", minutes) }
    }

    override fun weeklyReportTimeMinutes(): Int =
        preferences.getInt("weekly_report_time_minutes", 8 * 60)

    override fun setWeeklyReportTimeMinutes(minutes: Int) {
        preferences.edit { putInt("weekly_report_time_minutes", minutes) }
    }

    override fun weeklyReportDayOfWeek(): Int =
        preferences.getInt("weekly_report_day_of_week", 1)

    override fun setWeeklyReportDayOfWeek(dayOfWeek: Int) {
        preferences.edit { putInt("weekly_report_day_of_week", dayOfWeek.coerceIn(1, 7)) }
    }

    override fun dailySummaryEnabled(): Boolean =
        preferences.getBoolean("daily_summary_enabled", true)

    override fun setDailySummaryEnabled(enabled: Boolean) {
        preferences.edit { putBoolean("daily_summary_enabled", enabled) }
    }

    override fun weeklyReportEnabled(): Boolean =
        preferences.getBoolean("weekly_report_enabled", false)

    override fun setWeeklyReportEnabled(enabled: Boolean) {
        preferences.edit { putBoolean("weekly_report_enabled", enabled) }
    }
}
