package com.rhythmday.app.data.repository

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.core.domain.usecase.DayScoreCalculatorUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class MockDayEntryRepository(
    private val scoreCalculator: DayScoreCalculatorUseCase
) : DayEntryRepository {
    override fun observeEntries(): Flow<List<DayEntry>> = flowOf(createEntries())

    private fun createEntries(): List<DayEntry> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        return listOf(
            entry(
                id = 1,
                date = today.daysAgo(12),
                metrics = DayMetrics(8, 8, 8, 8, 7, 9, 2),
                tags = listOf("Прогулка", "Хороший сон")
            ),
            entry(
                id = 2,
                date = today.daysAgo(10),
                metrics = DayMetrics(4, 5, 5, 4, 4, 4, 8),
                tags = listOf("Плохой сон", "Много работы")
            ),
            entry(
                id = 3,
                date = today.daysAgo(8),
                metrics = DayMetrics(8, 8, 8, 7, 8, 8, 2),
                tags = listOf("Прогулка", "Друзья")
            ),
            entry(
                id = 4,
                date = today.daysAgo(7),
                metrics = DayMetrics(5, 6, 6, 5, 5, 5, 6),
                tags = listOf("Работа")
            ),
            entry(
                id = 5,
                date = today.daysAgo(5),
                metrics = DayMetrics(6, 7, 7, 6, 6, 6, 4),
                tags = listOf("Работа", "Спокойный день")
            ),
            entry(
                id = 6,
                date = today.daysAgo(4),
                metrics = DayMetrics(7, 7, 7, 7, 6, 7, 4),
                tags = listOf("Продуктивность")
            ),
            entry(
                id = 7,
                date = today.daysAgo(3),
                metrics = DayMetrics(5, 6, 6, 6, 7, 7, 6),
                tags = listOf("Усталость", "Работа")
            ),
            entry(
                id = 8,
                date = today.daysAgo(2),
                metrics = DayMetrics(8, 8, 8, 8, 9, 9, 2),
                tags = listOf("Прогулка", "Спорт")
            ),
            entry(
                id = 9,
                date = today.daysAgo(1),
                metrics = DayMetrics(8, 7, 7, 7, 7, 8, 3),
                tags = listOf("Спорт", "Хороший сон"),
                note = "После тренировки было больше энергии, вечером спокойно отдохнул."
            )
        )
    }

    private fun entry(
        id: Long,
        date: LocalDate,
        metrics: DayMetrics,
        tags: List<String>,
        note: String? = null
    ) = DayEntry(
        id = id,
        date = date,
        metrics = metrics,
        overallScore = scoreCalculator.invoke(metrics),
        tags = tags,
        note = note
    )

    private fun LocalDate.daysAgo(days: Int): LocalDate = minus(days, DateTimeUnit.DAY)
}
