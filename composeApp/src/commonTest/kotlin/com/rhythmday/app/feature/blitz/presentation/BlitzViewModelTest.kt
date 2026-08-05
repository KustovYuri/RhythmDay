package com.rhythmday.app.feature.blitz.presentation

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.core.domain.usecase.DayScoreCalculatorUseCase
import com.rhythmday.app.core.domain.usecase.SaveDayEntryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BlitzViewModelTest {
    @Test
    fun advancesOnlyAfterCurrentMetricHasAnAnswer() {
        val viewModel = createViewModel()

        viewModel.advance()
        assertEquals(0, viewModel.uiState.value.currentPage)

        viewModel.selectScore(8)
        viewModel.advance()

        assertEquals(1, viewModel.uiState.value.currentPage)
        assertEquals(8, viewModel.uiState.value.answers[BlitzMetric.ENERGY])
        assertNull(viewModel.uiState.value.currentScore)
    }

    @Test
    fun completesAfterAllSevenMetricsAreAnswered() {
        val viewModel = createViewModel()
        val scores = listOf(8, 6, 7, 9, 6, 5, 3)

        scores.forEach { score ->
            viewModel.selectScore(score)
            viewModel.advance()
        }

        val state = viewModel.uiState.value
        assertTrue(state.isComplete)
        assertTrue(state.isSummaryPage)
        assertEquals(8, state.unlockedPageCount)
        assertEquals(7.0, state.overallScore)
    }

    @Test
    fun completedAnswerCanBeOpenedAndChangedWithoutLosingSummary() {
        val viewModel = createViewModel()
        repeat(BlitzMetric.entries.size) {
            viewModel.selectScore(7)
            viewModel.advance()
        }

        viewModel.showPage(BlitzMetric.STRESS.ordinal)
        viewModel.selectScore(2)

        assertTrue(viewModel.uiState.value.isComplete)
        assertFalse(viewModel.uiState.value.isSummaryPage)
        assertEquals(8, viewModel.uiState.value.unlockedPageCount)
        assertEquals(2, viewModel.uiState.value.answers[BlitzMetric.STRESS])
    }

    @Test
    fun pagesUnlockOnlyThroughFirstUnansweredMetric() {
        val viewModel = createViewModel()

        assertEquals(1, viewModel.uiState.value.unlockedPageCount)
        viewModel.showPage(1)
        assertEquals(0, viewModel.uiState.value.currentPage)

        viewModel.selectScore(8)
        assertEquals(2, viewModel.uiState.value.unlockedPageCount)
        viewModel.showPage(1)
        assertEquals(1, viewModel.uiState.value.currentPage)
    }

    @Test
    fun finishSavesTodayEntryAndThenEmitsNavigationEvent() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingDayEntryRepository()
            val viewModel = createViewModel(repository)
            repeat(BlitzMetric.entries.size) {
                viewModel.selectScore(7)
                viewModel.advance()
            }

            viewModel.finish()
            advanceUntilIdle()

            val savedEntry = repository.savedEntries.single()
            assertEquals(LocalDate(2026, 8, 5), savedEntry.date)
            assertEquals(7.0, savedEntry.metrics.energy)
            assertEquals(7.0, savedEntry.metrics.stress)
            assertEquals(emptyList(), savedEntry.tags)
            assertNull(savedEntry.title)
            assertNull(savedEntry.note)
            assertFalse(viewModel.uiState.value.isSaving)
            assertTrue(viewModel.uiState.value.isFinished)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(
        repository: DayEntryRepository = RecordingDayEntryRepository()
    ): BlitzViewModel {
        val scoreCalculator = DayScoreCalculatorUseCase()
        return BlitzViewModel(
            saveDayEntryUseCase = SaveDayEntryUseCase(
                repository = repository,
                scoreCalculator = scoreCalculator,
                currentDate = { LocalDate(2026, 8, 5) }
            ),
            scoreCalculator = scoreCalculator
        )
    }
}

private class RecordingDayEntryRepository : DayEntryRepository {
    private val entries = MutableStateFlow<List<DayEntry>>(emptyList())
    val savedEntries = mutableListOf<DayEntry>()

    override fun observeEntries(): Flow<List<DayEntry>> = entries

    override suspend fun upsertEntry(entry: DayEntry) {
        savedEntries += entry
        entries.update { currentEntries ->
            currentEntries.filterNot { currentEntry -> currentEntry.date == entry.date } + entry
        }
    }
}
