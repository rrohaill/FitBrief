package dev.rrohaill.fitbrief.data

interface HealthRepository {
    val permissions: Set<String>
    suspend fun permissionStatus(): PermissionStatus
    suspend fun readSnapshot(range: HealthRange): HealthSnapshot
    suspend fun readTimeline(range: HealthRange): List<TimelineEvent>
    suspend fun readHeartRateSamples(range: HealthRange): List<Double>
    suspend fun readDailyHeartRate(range: HealthRange): List<DailyHeartRate>
}
