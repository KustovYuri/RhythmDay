package com.rhythmday.app.feature.home.domain.usecase

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveHomeDashboardUseCaseTest {
    @Test
    fun buildsCurrentWeekSummaryFromEntries() = runTest {
        val today = LocalDate(2026, 7, 18)
        val entries = listOf(
            entry(1, LocalDate(2026, 7, 13), 6.0),
            entry(2, LocalDate(2026, 7, 14), 6.6),
            entry(3, LocalDate(2026, 7, 15), 5.5),
            entry(4, LocalDate(2026, 7, 16), 8.2),
            entry(5, LocalDate(2026, 7, 17), 7.2)
        )
        val useCase = ObserveHomeDashboardUseCase(
            repository = FakeDayEntryRepository(entries),
            currentDate = { today }
        )

        val dashboard = useCase().first()

        assertEquals(5, dashboard.weekSummary.completedDays)
        assertEquals(6.7, dashboard.weekSummary.averageScore)
        assertEquals(4, dashboard.weekSummary.bestEntry?.id)
        assertEquals(5, dashboard.previousEntry?.id)
    }

    @Test
    fun doesNotExposeTodayEntryAsPreviousEntry() = runTest {
        val today = LocalDate(2026, 7, 18)
        val useCase = ObserveHomeDashboardUseCase(
            repository = FakeDayEntryRepository(listOf(entry(1, today, 8.0))),
            currentDate = { today }
        )

        val dashboard = useCase().first()

        assertNull(dashboard.previousEntry)
    }

    private fun entry(id: Long, date: LocalDate, score: Double) = DayEntry(
        id = id,
        date = date,
        metrics = DayMetrics(7, 7, 7, 7, 7, 7, 4),
        overallScore = score,
        tags = emptyList(),
        note = null
    )
}

private class FakeDayEntryRepository(
    private val entries: List<DayEntry>
) : DayEntryRepository {
    override fun observeEntries(): Flow<List<DayEntry>> = flowOf(entries)

    override suspend fun upsertEntry(entry: DayEntry) = Unit
}
