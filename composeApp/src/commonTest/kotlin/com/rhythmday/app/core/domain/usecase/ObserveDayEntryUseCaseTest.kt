package com.rhythmday.app.core.domain.usecase

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveDayEntryUseCaseTest {
    @Test
    fun emitsEntryWithRequestedId() = runTest {
        val expectedEntry = entry(id = 7)
        val useCase = ObserveDayEntryUseCase(
            MutableDayEntryRepository(listOf(entry(id = 3), expectedEntry))
        )

        assertEquals(expectedEntry, useCase(entryId = 7).first())
        assertNull(useCase(entryId = 99).first())
    }

    @Test
    fun reflectsUpdatesToSelectedEntry() = runTest {
        val initialEntry = entry(id = 7, energy = 4)
        val repository = MutableDayEntryRepository(listOf(initialEntry))
        val useCase = ObserveDayEntryUseCase(repository)
        val selectedEntry = useCase(entryId = 7)
        val updatedEntry = entry(id = 7, energy = 9)

        assertEquals(initialEntry, selectedEntry.first())
        repository.replaceEntries(listOf(updatedEntry))

        assertEquals(updatedEntry, selectedEntry.first())
    }

    private fun entry(id: Long, energy: Int = 7): DayEntry = DayEntry(
        id = id,
        date = LocalDate(2026, 7, 20),
        metrics = DayMetrics(energy, 6, 7, 6, 6, 8, 4),
        overallScore = 6.7,
        tags = listOf("Работа"),
        note = "Комментарий"
    )
}

private class MutableDayEntryRepository(
    entries: List<DayEntry>
) : DayEntryRepository {
    private val entriesFlow = MutableStateFlow(entries)

    override fun observeEntries(): Flow<List<DayEntry>> = entriesFlow

    override suspend fun upsertEntry(entry: DayEntry) = Unit

    fun replaceEntries(entries: List<DayEntry>) {
        entriesFlow.value = entries
    }
}
