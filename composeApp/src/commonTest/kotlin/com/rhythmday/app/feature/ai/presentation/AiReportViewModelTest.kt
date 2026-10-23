package com.rhythmday.app.feature.ai.presentation

import androidx.lifecycle.viewModelScope
import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.feature.ai.domain.AiPeriodAssessment
import com.rhythmday.app.feature.ai.domain.AiReport
import com.rhythmday.app.feature.ai.domain.AiReportConfidence
import com.rhythmday.app.feature.ai.domain.AiReportRepository
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
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AiReportViewModelTest {
    @Test
    fun `loads cached report and generates using only period entries`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var viewModel: AiReportViewModel? = null
        try {
            val from = LocalDate(2026, 10, 2)
            val to = LocalDate(2026, 10, 3)
            val cached = report(from, to, "Сохранённый отчёт")
            val generated = report(from, to, "Новый отчёт")
            val aiRepository = RecordingAiReportRepository(cached, generated)
            val aiViewModel = AiReportViewModel(
                from = from,
                to = to,
                dayEntryRepository = TestDayEntryRepository(
                    listOf(
                        entry(1, LocalDate(2026, 10, 1)),
                        entry(2, from),
                        entry(3, to),
                        entry(4, LocalDate(2026, 10, 4))
                    )
                ),
                aiReportRepository = aiRepository
            )
            viewModel = aiViewModel
            val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                aiViewModel.uiState.collect()
            }

            advanceUntilIdle()
            assertEquals(2, aiViewModel.uiState.value.entryCount)
            assertEquals("Сохранённый отчёт", aiViewModel.uiState.value.report?.summary)

            aiViewModel.generateReport()
            advanceUntilIdle()

            assertEquals(listOf(2L, 3L), aiRepository.generatedEntries.map(DayEntry::id))
            assertEquals("Новый отчёт", aiViewModel.uiState.value.report?.summary)
            assertFalse(aiViewModel.uiState.value.isGenerating)

            collection.cancel()
        } finally {
            viewModel?.viewModelScope?.cancel()
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    private fun entry(id: Long, date: LocalDate) = DayEntry(
        id = id,
        date = date,
        metrics = DayMetrics(7, 7, 7, 7, 7, 7, 3),
        overallScore = 7.0,
        tags = listOf("прогулка")
    )

    private fun report(from: LocalDate, to: LocalDate, summary: String) = AiReport(
        from = from,
        to = to,
        generatedAt = Instant.parse("2026-10-21T10:00:00Z"),
        entryCount = 2,
        assessment = AiPeriodAssessment.STABLE,
        confidence = AiReportConfidence.LOW,
        summary = summary,
        observations = emptyList(),
        suggestions = listOf("Продолжай наблюдение"),
        reflectionQuestions = emptyList(),
        disclaimer = "Не диагноз"
    )
}

private class TestDayEntryRepository(entries: List<DayEntry>) : DayEntryRepository {
    private val entriesFlow = MutableStateFlow(entries)

    override fun observeEntries(): Flow<List<DayEntry>> = entriesFlow

    override suspend fun upsertEntry(entry: DayEntry) = Unit
}

private class RecordingAiReportRepository(
    private val cached: AiReport?,
    private val generated: AiReport
) : AiReportRepository {
    var generatedEntries: List<DayEntry> = emptyList()
        private set

    override suspend fun cachedReport(
        from: LocalDate,
        to: LocalDate,
        entries: List<DayEntry>
    ): AiReport? = cached

    override suspend fun generateReport(
        from: LocalDate,
        to: LocalDate,
        entries: List<DayEntry>
    ): AiReport {
        generatedEntries = entries
        return generated
    }
}
