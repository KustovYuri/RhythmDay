package com.rhythmday.app.feature.home.domain

import com.rhythmday.app.core.domain.model.DayEntry
import kotlinx.datetime.LocalDate

data class WeekSummary(
    val completedDays: Int,
    val averageScore: Double?,
    val bestEntry: DayEntry?
)

data class HomeDashboard(
    val currentDate: LocalDate,
    val todayEntry: DayEntry?,
    val weekSummary: WeekSummary,
    val previousEntry: DayEntry?
)
