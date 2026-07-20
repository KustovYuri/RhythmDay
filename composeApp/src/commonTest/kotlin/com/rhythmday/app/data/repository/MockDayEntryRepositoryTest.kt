package com.rhythmday.app.data.repository

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.usecase.DayScoreCalculatorUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class MockDayEntryRepositoryTest {
    @Test
    fun emitsUpsertedEntryImmediately() = runTest {
        val repository = MockDayEntryRepository(DayScoreCalculatorUseCase())
        val entry = DayEntry(
            id = 100,
            date = LocalDate(2026, 7, 20),
            metrics = DayMetrics(
                energy = 7,
                mood = 6,
                wellbeing = 7,
                mentalClarity = 6,
                sociality = 6,
                sleepQuality = 8,
                stress = 4
            ),
            overallScore = 6.7,
            tags = listOf("Работа"),
            note = "Готово"
        )

        repository.upsertEntry(entry)

        assertEquals(entry, repository.observeEntries().first().first { it.id == entry.id })
    }
}
