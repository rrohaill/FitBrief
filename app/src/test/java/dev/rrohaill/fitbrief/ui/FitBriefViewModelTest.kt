package dev.rrohaill.fitbrief.ui

import dev.rrohaill.fitbrief.data.RangeOption
import dev.rrohaill.fitbrief.data.RefreshInterval
import dev.rrohaill.fitbrief.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class FitBriefViewModelTest {
    private val repository = FakeHealthRepository()
    private val preferences = FakePreferences()
    private val summaries = FakeSummaryService()
    private val scheduler = FakeScheduler()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = FitBriefViewModel(repository, preferences, summaries, scheduler)

    @Test
    fun `initial state is seeded from preferences and permission status`() {
        preferences.theme = ThemeMode.Dark
        preferences.interval = RefreshInterval.OneHour
        preferences.weekly = true
        val state = viewModel().uiState.value
        assertEquals(ThemeMode.Dark, state.settings.themeMode)
        assertEquals(RefreshInterval.OneHour, state.settings.refreshInterval)
        assertTrue(state.settings.weeklyReportEnabled)
        assertTrue(state.permissionStatus.granted)
        assertEquals(2, state.permissionStatus.grantedCount)
    }

    @Test
    fun `refresh loads snapshot and summary for the selected range`() {
        val vm = viewModel()
        vm.selectRange(RangeOption.SevenDays)
        val state = vm.uiState.value
        assertEquals(RangeOption.SevenDays, state.selectedRange)
        assertEquals(RangeOption.SevenDays, repository.snapshotRanges.last().option)
        assertEquals(5_000L, state.snapshot?.steps)
        assertEquals("Summary of 5000 steps", state.summary)
        assertEquals(summaries.backend, state.activeBackend)
        assertFalse(state.isLoading)
        assertNull(state.message)
    }

    @Test
    fun `refresh without permissions asks for them instead of reading data`() {
        repository.granted = false
        val vm = viewModel()
        vm.refresh()
        assertEquals("Grant Health Connect permissions to generate a summary.", vm.uiState.value.message)
        assertTrue(repository.snapshotRanges.isEmpty())
    }

    @Test
    fun `refresh failure surfaces the error and stops loading`() {
        repository.failWith = IllegalStateException("Health Connect is busy")
        val vm = viewModel()
        vm.refresh()
        assertEquals("Health Connect is busy", vm.uiState.value.message)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `opening a metric requests an insight for the current snapshot`() {
        val vm = viewModel()
        vm.refresh()
        vm.openMetricDetail(MetricType.Steps)
        val state = vm.uiState.value
        assertEquals(MetricType.Steps, state.metricDetail.metric)
        assertEquals("Insight: 5000 steps", state.metricDetail.insight)
        assertFalse(state.metricDetail.insightLoading)
        assertTrue(repository.heartRateRanges.isEmpty())
    }

    @Test
    fun `opening a metric before any data is loaded requests no insight`() {
        val vm = viewModel()
        vm.openMetricDetail(MetricType.Steps)
        assertNull(vm.uiState.value.metricDetail.insight)
        assertTrue(summaries.metricRequests.isEmpty())
    }

    @Test
    fun `opening heart rate also loads samples`() {
        val vm = viewModel()
        vm.openMetricDetail(MetricType.HeartRate)
        assertEquals(listOf(60.0, 90.0), vm.uiState.value.metricDetail.heartRateSamples)
        assertEquals(1, repository.heartRateRanges.size)
    }

    @Test
    fun `navigating back a day reloads the offset period and refreshes the insight`() {
        val vm = viewModel()
        vm.openMetricDetail(MetricType.Sleep)
        repository.snapshotFor = { range -> dev.rrohaill.fitbrief.data.HealthSnapshot(range, 1, 0.0, 0.0, 0.0, 0, null, 360) }
        vm.navigateMetricDay(1)
        val state = vm.uiState.value
        assertEquals(1, state.metricDetail.dayOffset)
        assertEquals(360L, state.snapshot?.sleepMinutes)
        assertEquals("Insight: 360 minutes of sleep", state.metricDetail.insight)
        assertFalse(state.isLoading)
        val loaded = repository.snapshotRanges.last()
        assertEquals(LocalDate.now().minusDays(1), loaded.start.atZone(java.time.ZoneId.systemDefault()).toLocalDate())
    }

    @Test
    fun `navigating forward past today is ignored`() {
        val vm = viewModel()
        vm.openMetricDetail(MetricType.Steps)
        val before = repository.snapshotRanges.size
        vm.navigateMetricDay(-1)
        assertEquals(0, vm.uiState.value.metricDetail.dayOffset)
        assertEquals(before, repository.snapshotRanges.size)
    }

    @Test
    fun `drilling into a day and closing returns to the browsed period`() {
        val vm = viewModel()
        vm.openMetricDetail(MetricType.Steps)
        vm.navigateMetricDay(2)
        vm.openMetricDate(LocalDate.of(2026, 9, 1))
        assertEquals(LocalDate.of(2026, 9, 1), vm.uiState.value.metricDetail.drilldownDate)

        vm.closeMetricDetail()
        val state = vm.uiState.value
        assertNull(state.metricDetail.drilldownDate)
        assertEquals(MetricType.Steps, state.metricDetail.metric)
        assertEquals(2, state.metricDetail.dayOffset)

        vm.closeMetricDetail()
        assertNull(vm.uiState.value.metricDetail.metric)
    }

    @Test
    fun `paging while drilled into a day moves the drilldown date`() {
        val vm = viewModel()
        vm.openMetricDetail(MetricType.Steps)
        vm.openMetricDate(LocalDate.of(2026, 9, 3))
        vm.navigateMetricDay(1)
        assertEquals(LocalDate.of(2026, 9, 2), vm.uiState.value.metricDetail.drilldownDate)
        assertEquals(0, vm.uiState.value.metricDetail.dayOffset)
        val loaded = repository.snapshotRanges.last()
        assertEquals(LocalDate.of(2026, 9, 2), loaded.start.atZone(java.time.ZoneId.systemDefault()).toLocalDate())

        vm.navigateMetricDay(-1)
        assertEquals(LocalDate.of(2026, 9, 3), vm.uiState.value.metricDetail.drilldownDate)
    }

    @Test
    fun `paging a drilled-down day past today is ignored`() {
        val vm = viewModel()
        vm.openMetricDetail(MetricType.Steps)
        vm.openMetricDate(LocalDate.now())
        val before = repository.snapshotRanges.size
        vm.navigateMetricDay(-1)
        assertEquals(LocalDate.now(), vm.uiState.value.metricDetail.drilldownDate)
        assertEquals(before, repository.snapshotRanges.size)
    }

    @Test
    fun `changing the refresh interval persists it and reschedules work`() {
        val vm = viewModel()
        vm.setRefreshInterval(RefreshInterval.TwoHours)
        assertEquals(RefreshInterval.TwoHours, preferences.interval)
        assertEquals(listOf(120L), scheduler.intervals)
    }

    @Test
    fun `enabling the daily summary schedules the default notification`() {
        preferences.daily = false
        val vm = viewModel()
        vm.toggleDailySummary()
        assertTrue(preferences.daily)
        assertTrue(vm.uiState.value.notificationsScheduled)
        assertEquals(listOf(24 * 60L), scheduler.intervals)
    }
}
