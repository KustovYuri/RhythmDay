package com.rhythmday.app.data.repository

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.core.domain.usecase.DayScoreCalculatorUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class MockDayEntryRepository(
    private val scoreCalculator: DayScoreCalculatorUseCase
) : DayEntryRepository {
    private val entries = MutableStateFlow(createEntries())

    override fun observeEntries(): Flow<List<DayEntry>> = entries

    override suspend fun upsertEntry(entry: DayEntry) {
        entries.update { currentEntries ->
            (currentEntries.filterNot { currentEntry -> currentEntry.date == entry.date } + entry)
                .sortedBy(DayEntry::date)
        }
    }

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
            ),
            entry(
                id = 10,
                date = today.dateInPreviousMonth(monthsAgo = 1, day = 4),
                metrics = DayMetrics(7, 8, 7, 7, 6, 8, 3),
                tags = listOf("Работа", "Прогулка"),
                note = "После рабочего дня удалось переключиться на прогулке."
            ),
            entry(
                id = 11,
                date = today.dateInPreviousMonth(monthsAgo = 1, day = 12),
                metrics = DayMetrics(4, 5, 5, 4, 3, 4, 8),
                tags = listOf("Плохой сон", "Перегруз", "Тревога")
            ),
            entry(
                id = 12,
                date = today.dateInPreviousMonth(monthsAgo = 1, day = 20),
                metrics = DayMetrics(8, 8, 8, 8, 7, 9, 2),
                tags = listOf("Хороший сон", "Спорт", "Друзья"),
                note = "Выспался, потренировался и хорошо провел вечер с друзьями."
            ),
            entry(
                id = 13,
                date = today.dateInPreviousMonth(monthsAgo = 1, day = 27),
                metrics = DayMetrics(7, 7, 7, 8, 7, 7, 4),
                tags = listOf("Работа", "Прогулка", "Дом", "Спокойствие", "Продуктивность")
            ),
            entry(
                id = 14,
                date = today.dateInPreviousMonth(monthsAgo = 2, day = 6),
                metrics = DayMetrics(6, 6, 7, 6, 5, 6, 5),
                tags = listOf("Дом", "Отдых")
            ),
            entry(
                id = 15,
                date = today.dateInPreviousMonth(monthsAgo = 2, day = 15),
                metrics = DayMetrics(9, 8, 8, 8, 8, 9, 2),
                tags = listOf("Путешествие", "Люди", "Прогулка"),
                note = "Новая обстановка дала много энергии и впечатлений."
            ),
            entry(
                id = 16,
                date = today.dateInPreviousMonth(monthsAgo = 2, day = 24),
                metrics = DayMetrics(5, 5, 6, 5, 4, 5, 7),
                tags = listOf("Работа", "Конфликт", "Перегруз")
            ),
            entry(
                id = 17,
                date = today.dateInPreviousMonth(monthsAgo = 3, day = 3),
                metrics = DayMetrics(8, 8, 8, 7, 7, 8, 3),
                tags = listOf("Творчество", "Спокойствие")
            ),
            entry(
                id = 18,
                date = today.dateInPreviousMonth(monthsAgo = 3, day = 14),
                metrics = DayMetrics(3, 4, 4, 3, 3, 4, 9),
                tags = listOf("Болезнь", "Плохой сон"),
                note = "Почти весь день ушел на отдых и восстановление."
            ),
            entry(
                id = 19,
                date = today.dateInPreviousMonth(monthsAgo = 3, day = 25),
                metrics = DayMetrics(7, 7, 8, 7, 6, 8, 3),
                tags = listOf("Семья", "Дом", "Хороший сон")
            ),
            entry(
                id = 20,
                date = today.dateInPreviousMonth(monthsAgo = 4, day = 8),
                metrics = DayMetrics(6, 7, 6, 6, 7, 7, 4),
                tags = listOf("Учёба", "Продуктивность")
            ),
            entry(
                id = 21,
                date = today.dateInPreviousMonth(monthsAgo = 4, day = 19),
                metrics = DayMetrics(8, 9, 8, 8, 8, 9, 2),
                tags = listOf("Восстановление", "Отдых", "Прогулка"),
                note = "Спокойный день без спешки хорошо восстановил силы."
            )
        ).distinctBy(DayEntry::date).sortedBy(DayEntry::date)
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

    private fun LocalDate.dateInPreviousMonth(monthsAgo: Int, day: Int): LocalDate {
        val monthStart = LocalDate(year, month, 1).minus(monthsAgo, DateTimeUnit.MONTH)
        val lastDay = monthStart.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day
        return LocalDate(monthStart.year, monthStart.month, day.coerceAtMost(lastDay))
    }
}
