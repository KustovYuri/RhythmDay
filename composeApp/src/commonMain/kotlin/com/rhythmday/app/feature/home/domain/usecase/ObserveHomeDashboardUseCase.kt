package com.rhythmday.app.feature.home.domain.usecase

import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.repository.DayEntryRepository
import com.rhythmday.app.feature.home.domain.HomeDashboard
import com.rhythmday.app.feature.home.domain.WeekSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.todayIn
import kotlin.math.round
import kotlin.time.Clock

class ObserveHomeDashboardUseCase(
    private val repository: DayEntryRepository,
    private val currentDate: () -> LocalDate = {
        Clock.System.todayIn(TimeZone.currentSystemDefault())
    }
) {
    operator fun invoke(): Flow<HomeDashboard> = repository.observeEntries().map { entries ->
        val today = currentDate()
        val todayEpochDay = today.toEpochDays()
        val weekStart = todayEpochDay - (today.dayOfWeek.isoDayNumber - 1)
        val weekEntries = entries.filter { entry ->
            entry.date.toEpochDays() in weekStart..todayEpochDay
        }

        HomeDashboard(
            currentDate = today,
            todayEntry = entries.firstOrNull { it.date == today },
            weekSummary = WeekSummary(
                completedDays = weekEntries.size,
                averageScore = weekEntries
                    .takeIf(List<DayEntry>::isNotEmpty)
                    ?.map(DayEntry::overallScore)
                    ?.average()
                    ?.let { round(it * 10) / 10 },
                bestEntry = weekEntries.maxByOrNull(DayEntry::overallScore)
            ),
            previousEntry = entries
                .filter { it.date < today }
                .maxByOrNull(DayEntry::date)
        )
    }
}
