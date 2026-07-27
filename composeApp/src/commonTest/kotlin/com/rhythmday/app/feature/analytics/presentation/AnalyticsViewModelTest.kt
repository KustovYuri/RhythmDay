package com.rhythmday.app.feature.analytics.presentation

import androidx.lifecycle.viewModelScope
import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {
    @Test
    fun filtersEntriesForSelectedRange() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var viewModel: AnalyticsViewModel? = null
        try {
            val analyticsViewModel = AnalyticsViewModel(
                repository = AnalyticsRepository(
                    listOf(
                        entry(id = 1, date = LocalDate(2026, 7, 1)),
                        entry(id = 2, date = LocalDate(2026, 7, 26)),
                        entry(id = 3, date = LocalDate(2026, 7, 27)),
                        entry(id = 4, date = LocalDate(2026, 7, 29)),
                        entry(id = 5, date = LocalDate(2026, 7, 30))
                    )
                ),
                currentDate = { LocalDate(2026, 7, 29) }
            )
            viewModel = analyticsViewModel
            val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                analyticsViewModel.uiState.collect()
            }

            advanceUntilIdle()
            assertEquals(LocalDate(2026, 7, 27), analyticsViewModel.uiState.value.rangeStart)
            assertEquals(listOf(3L, 4L), analyticsViewModel.uiState.value.entries.map(DayEntry::id))

            analyticsViewModel.selectRange(AnalyticsRange.MONTH)
            advanceUntilIdle()
            assertEquals(listOf(1L, 2L, 3L, 4L), analyticsViewModel.uiState.value.entries.map(DayEntry::id))

            analyticsViewModel.selectRange(AnalyticsRange.PERIOD)
            analyticsViewModel.setPeriodStart(LocalDate(2026, 7, 26))
            analyticsViewModel.setPeriodEnd(LocalDate(2026, 7, 28))
            advanceUntilIdle()
            assertEquals(listOf(2L, 3L), analyticsViewModel.uiState.value.entries.map(DayEntry::id))

            collection.cancel()
        } finally {
            viewModel?.viewModelScope?.cancel()
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun allowsEmptySelectionAndOpensOverviewMetricInChart() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var viewModel: AnalyticsViewModel? = null
        try {
            val analyticsViewModel = AnalyticsViewModel(
                repository = AnalyticsRepository(
                    listOf(entry(id = 1, date = LocalDate(2026, 7, 29)))
                ),
                currentDate = { LocalDate(2026, 7, 29) }
            )
            viewModel = analyticsViewModel
            val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                analyticsViewModel.uiState.collect()
            }

            advanceUntilIdle()
            assertEquals(
                setOf(AnalyticsMetric.SCORE),
                analyticsViewModel.uiState.value.selectedMetrics
            )

            analyticsViewModel.showMetric(AnalyticsMetric.ENERGY)
            advanceUntilIdle()
            assertEquals(AnalyticsMode.CHART, analyticsViewModel.uiState.value.mode)
            assertEquals(setOf(AnalyticsMetric.ENERGY), analyticsViewModel.uiState.value.selectedMetrics)

            analyticsViewModel.toggleMetric(AnalyticsMetric.ENERGY)
            advanceUntilIdle()
            assertEquals(emptySet(), analyticsViewModel.uiState.value.selectedMetrics)

            analyticsViewModel.toggleMetric(AnalyticsMetric.MOOD)
            advanceUntilIdle()
            assertEquals(setOf(AnalyticsMetric.MOOD), analyticsViewModel.uiState.value.selectedMetrics)

            collection.cancel()
        } finally {
            viewModel?.viewModelScope?.cancel()
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    private fun entry(id: Long, date: LocalDate): DayEntry = DayEntry(
        id = id,
        date = date,
        metrics = DayMetrics(
            energy = 7,
            mood = 6,
            wellbeing = 7,
            mentalClarity = 6,
            sociality = 6,
            sleepQuality = 8,
            stress = 4
        ),
        overallScore = 7.1,
        tags = listOf("Работа")
    )
}

private class AnalyticsRepository(entries: List<DayEntry>) : DayEntryRepository {
    private val entriesFlow = MutableStateFlow(entries)

    override fun observeEntries(): Flow<List<DayEntry>> = entriesFlow

    override suspend fun upsertEntry(entry: DayEntry) {
        entriesFlow.value = entriesFlow.value.filterNot { current ->
            current.date == entry.date
        } + entry
    }
}
