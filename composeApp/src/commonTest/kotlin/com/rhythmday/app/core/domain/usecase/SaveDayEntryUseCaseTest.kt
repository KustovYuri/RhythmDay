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
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class SaveDayEntryUseCaseTest {
    @Test
    fun savesNewEntryForRequestedPastDate() = runTest {
        val today = LocalDate(2026, 7, 20)
        val missedDate = LocalDate(2026, 7, 18)
        val repository = FakeMutableDayEntryRepository(emptyList())
        val useCase = SaveDayEntryUseCase(
            repository = repository,
            scoreCalculator = DayScoreCalculatorUseCase(),
            currentDate = { today }
        )

        val savedEntry = useCase(
            metrics = DayMetrics(7, 6, 8, 7, 5, 8, 3),
            title = "Пропущенный день",
            tags = emptyList(),
            note = "",
            entryDate = missedDate
        )

        assertEquals(missedDate, savedEntry.date)
        assertEquals(savedEntry, repository.observeEntries().first().single())
    }

    @Test
    fun rejectsNewEntryForFutureDate() = runTest {
        val today = LocalDate(2026, 7, 20)
        val useCase = SaveDayEntryUseCase(
            repository = FakeMutableDayEntryRepository(emptyList()),
            scoreCalculator = DayScoreCalculatorUseCase(),
            currentDate = { today }
        )

        assertFailsWith<IllegalArgumentException> {
            useCase(
                metrics = DayMetrics(7, 6, 8, 7, 5, 8, 3),
                title = "",
                tags = emptyList(),
                note = "",
                entryDate = LocalDate(2026, 7, 21)
            )
        }
    }

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
            energy = 7.4,
            mood = 6.2,
            wellbeing = 7.1,
            mentalClarity = 6.8,
            sociality = 6.3,
            sleepQuality = 8.5,
            stress = 4.2
        )

        val savedEntry = useCase(
            metrics = metrics,
            title = "  Сильный день  ",
            tags = listOf(" Работа ", "", "Работа", "Спорт"),
            note = "  Хороший день  "
        )

        assertEquals(7L, savedEntry.id)
        assertEquals(today, savedEntry.date)
        assertEquals(metrics, savedEntry.metrics)
        assertEquals(DayScoreCalculatorUseCase()(metrics), savedEntry.overallScore)
        assertEquals("Сильный день", savedEntry.title)
        assertEquals(listOf("Работа", "Спорт"), savedEntry.tags)
        assertEquals("Хороший день", savedEntry.note)
        assertEquals(savedAt, savedEntry.savedAt)
        assertEquals(savedEntry, repository.observeEntries().first().single())
    }

    @Test
    fun updatesRequestedEntryWithoutChangingItsIdOrDate() = runTest {
        val today = LocalDate(2026, 7, 20)
        val historicalDate = LocalDate(2026, 7, 19)
        val historicalEntry = DayEntry(
            id = 9,
            date = historicalDate,
            metrics = DayMetrics(4, 4, 4, 4, 4, 4, 7),
            overallScore = 4.1,
            tags = listOf("Работа"),
            note = null
        )
        val todayEntry = historicalEntry.copy(id = 10, date = today)
        val repository = FakeMutableDayEntryRepository(listOf(historicalEntry, todayEntry))
        val useCase = SaveDayEntryUseCase(
            repository = repository,
            scoreCalculator = DayScoreCalculatorUseCase(),
            currentDate = { today }
        )
        val updatedMetrics = DayMetrics(9, 8, 8, 8, 7, 9, 2)

        val updatedEntry = useCase(
            metrics = updatedMetrics,
            title = "Стало легче",
            tags = listOf("Спорт"),
            note = "Стало лучше",
            entryId = historicalEntry.id
        )

        assertEquals(historicalEntry.id, updatedEntry.id)
        assertEquals(historicalDate, updatedEntry.date)
        assertEquals(updatedMetrics, updatedEntry.metrics)
        assertEquals("Стало легче", updatedEntry.title)
        assertEquals(listOf("Спорт"), updatedEntry.tags)
        assertEquals("Стало лучше", updatedEntry.note)
        assertEquals(
            todayEntry,
            repository.observeEntries().first().first { entry -> entry.id == todayEntry.id }
        )
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
