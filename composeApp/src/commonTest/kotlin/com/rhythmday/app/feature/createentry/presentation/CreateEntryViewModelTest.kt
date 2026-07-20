package com.rhythmday.app.feature.createentry.presentation

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.core.domain.usecase.DayScoreCalculatorUseCase
import com.rhythmday.app.core.domain.usecase.SaveDayEntryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreateEntryViewModelTest {
    @Test
    fun saveBecomesAvailableOnlyAfterEveryMetricIsSet() {
        val repository = RecordingDayEntryRepository()
        val viewModel = createViewModel(repository)
        val values = listOf(
            CreateEntryMetric.ENERGY to 7,
            CreateEntryMetric.MOOD to 6,
            CreateEntryMetric.WELLBEING to 7,
            CreateEntryMetric.SLEEP_QUALITY to 8,
            CreateEntryMetric.MENTAL_CLARITY to 6,
            CreateEntryMetric.SOCIALITY to 6,
            CreateEntryMetric.STRESS to 4
        )

        assertFalse(viewModel.uiState.value.canSave)

        values.forEachIndexed { index, (metric, value) ->
            viewModel.onMetricChanged(metric, value)
            if (index < values.lastIndex) {
                assertFalse(viewModel.uiState.value.canSave)
            }
        }

        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun saveIsIgnoredWhileAnyMetricIsEmpty() {
        val repository = RecordingDayEntryRepository()
        val viewModel = createViewModel(repository)

        viewModel.onMetricChanged(CreateEntryMetric.ENERGY, 7)
        viewModel.save()

        assertTrue(repository.savedEntries.isEmpty())
    }

    private fun createViewModel(repository: DayEntryRepository): CreateEntryViewModel =
        CreateEntryViewModel(
            SaveDayEntryUseCase(
                repository = repository,
                scoreCalculator = DayScoreCalculatorUseCase(),
                currentDate = { LocalDate(2026, 7, 20) }
            )
        )
}

private class RecordingDayEntryRepository : DayEntryRepository {
    val savedEntries = mutableListOf<DayEntry>()

    override fun observeEntries(): Flow<List<DayEntry>> = flowOf(emptyList())

    override suspend fun upsertEntry(entry: DayEntry) {
        savedEntries += entry
    }
}
