package dev.rrohaill.fitbrief.data

/** Persisted user settings. Implemented by [FitBriefPreferences] on SharedPreferences; faked in tests. */
interface FitBriefPreferencesStore {
    fun themeMode(): ThemeMode
    fun setThemeMode(mode: ThemeMode)
    fun refreshInterval(): RefreshInterval
    fun setRefreshInterval(interval: RefreshInterval)
    fun dailySummaryTimeMinutes(): Int
    fun setDailySummaryTimeMinutes(minutes: Int)
    fun weeklyReportTimeMinutes(): Int
    fun setWeeklyReportTimeMinutes(minutes: Int)
    fun weeklyReportDayOfWeek(): Int
    fun setWeeklyReportDayOfWeek(dayOfWeek: Int)
    fun dailySummaryEnabled(): Boolean
    fun setDailySummaryEnabled(enabled: Boolean)
    fun weeklyReportEnabled(): Boolean
    fun setWeeklyReportEnabled(enabled: Boolean)
}
