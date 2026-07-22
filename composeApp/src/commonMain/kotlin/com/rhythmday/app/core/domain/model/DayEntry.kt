package com.rhythmday.app.core.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

const val DAY_TITLE_MAX_LENGTH = 40

data class DayMetrics(
    val energy: Int,
    val mood: Int,
    val wellbeing: Int,
    val mentalClarity: Int,
    val sociality: Int,
    val sleepQuality: Int,
    val stress: Int
) {
    init {
        listOf(energy, mood, wellbeing, mentalClarity, sociality, sleepQuality, stress).forEach {
            require(it in 1..10) { "Day metrics must be between 1 and 10" }
        }
    }
}

data class DayEntry(
    val id: Long,
    val date: LocalDate,
    val metrics: DayMetrics,
    val overallScore: Double,
    val tags: List<String>,
    val title: String? = null,
    val note: String? = null,
    val savedAt: Instant? = null
)
