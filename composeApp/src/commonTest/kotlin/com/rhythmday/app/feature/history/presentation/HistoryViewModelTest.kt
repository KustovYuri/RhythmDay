package com.rhythmday.app.feature.history.presentation

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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @Test
    fun showsEntriesForSelectedMonthAndRespectsAvailableRange() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var viewModel: HistoryViewModel? = null
        try {
            val repository = HistoryRepository(
                listOf(
                    entry(id = 1, date = LocalDate(2026, 5, 18), score = 4.8),
                    entry(id = 2, date = LocalDate(2026, 6, 27), score = 6.5),
                    entry(id = 3, date = LocalDate(2026, 7, 2), score = 7.2),
                    entry(id = 4, date = LocalDate(2026, 7, 18), score = 8.6)
                )
            )
            val historyViewModel = HistoryViewModel(
                repository = repository,
                currentDate = { LocalDate(2026, 7, 22) }
            )
            viewModel = historyViewModel
            val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                historyViewModel.uiState.collect()
            }

            advanceUntilIdle()
            assertEquals(LocalDate(2026, 7, 1), historyViewModel.uiState.value.selectedMonth)
            assertEquals(listOf(4L, 3L), historyViewModel.uiState.value.entries.map(DayEntry::id))
            assertTrue(historyViewModel.uiState.value.canShowPreviousMonth)
            assertFalse(historyViewModel.uiState.value.canShowNextMonth)

            historyViewModel.showPreviousMonth()
            advanceUntilIdle()
            assertEquals(LocalDate(2026, 6, 1), historyViewModel.uiState.value.selectedMonth)
            assertEquals(listOf(2L), historyViewModel.uiState.value.entries.map(DayEntry::id))
            assertTrue(historyViewModel.uiState.value.canShowNextMonth)

            historyViewModel.showPreviousMonth()
            advanceUntilIdle()
            assertEquals(LocalDate(2026, 5, 1), historyViewModel.uiState.value.selectedMonth)
            assertEquals(listOf(1L), historyViewModel.uiState.value.entries.map(DayEntry::id))
            assertFalse(historyViewModel.uiState.value.canShowPreviousMonth)

            collection.cancel()
        } finally {
            viewModel?.viewModelScope?.cancel()
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    private fun entry(id: Long, date: LocalDate, score: Double): DayEntry = DayEntry(
        id = id,
        date = date,
        metrics = DayMetrics(7, 7, 7, 7, 7, 7, 4),
        overallScore = score,
        tags = listOf("Работа"),
        note = null
    )
}

private class HistoryRepository(entries: List<DayEntry>) : DayEntryRepository {
    private val entriesFlow = MutableStateFlow(entries)

    override fun observeEntries(): Flow<List<DayEntry>> = entriesFlow

    override suspend fun upsertEntry(entry: DayEntry) {
        entriesFlow.value = entriesFlow.value.filterNot { current -> current.date == entry.date } + entry
    }
}
