package com.rhythmday.app.core.domain.usecase

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SaveDayEntryUseCaseTest {
    @Test
    fun savesTodayEntryWithCalculatedScoreAndCleanText() = runTest {
        val today = LocalDate(2026, 7, 20)
        val savedAt = Instant.parse("2026-07-20T15:42:00Z")
        val repository = FakeMutableDayEntryRepository(
            listOf(
                DayEntry(
                    id = 7,
                    date = today,
                    metrics = DayMetrics(2, 2, 2, 2, 2, 2, 9),
                    overallScore = 1.8,
                    tags = listOf("old"),
                    note = null
                )
            )
        )
        val useCase = SaveDayEntryUseCase(
            repository = repository,
            scoreCalculator = DayScoreCalculatorUseCase(),
            currentDate = { today },
            currentInstant = { savedAt }
        )
        val metrics = DayMetrics(
            energy = 7,
            mood = 6,
            wellbeing = 7,
            mentalClarity = 6,
            sociality = 6,
            sleepQuality = 8,
            stress = 4
        )

        val savedEntry = useCase(
            metrics = metrics,
            tags = listOf(" Работа ", "", "Работа", "Спорт"),
            note = "  Хороший день  "
        )

        assertEquals(7L, savedEntry.id)
        assertEquals(today, savedEntry.date)
        assertEquals(metrics, savedEntry.metrics)
        assertEquals(DayScoreCalculatorUseCase()(metrics), savedEntry.overallScore)
        assertEquals(listOf("Работа", "Спорт"), savedEntry.tags)
        assertEquals("Хороший день", savedEntry.note)
        assertEquals(savedAt, savedEntry.savedAt)
        assertEquals(savedEntry, repository.observeEntries().first().single())
    }
}

private class FakeMutableDayEntryRepository(
    entries: List<DayEntry>
) : DayEntryRepository {
    private val entriesFlow = MutableStateFlow(entries)

    override fun observeEntries(): Flow<List<DayEntry>> = entriesFlow

    override suspend fun upsertEntry(entry: DayEntry) {
        entriesFlow.update { entries ->
            entries.filterNot { currentEntry -> currentEntry.date == entry.date } + entry
        }
    }
}
