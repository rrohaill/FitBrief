package dev.rrohaill.fitbrief.data

/** Read access to the user's health data. Implemented by [HealthConnectRepository]; faked in tests. */
interface HealthRepository {
    val permissions: Set<String>
    suspend fun permissionStatus(): PermissionStatus
    suspend fun readSnapshot(range: HealthRange): HealthSnapshot
    suspend fun readTimeline(range: HealthRange): List<TimelineEvent>
    suspend fun readHeartRateSamples(range: HealthRange): List<Double>
}
