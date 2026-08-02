package com.rhythmday.app.core.domain.model

import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.round
import kotlin.time.Instant

const val DAY_TITLE_MAX_LENGTH = 40
const val DAY_METRIC_MIN_VALUE = 0.1
const val DAY_METRIC_MAX_VALUE = 10.0

data class DayMetrics(
    val energy: Double,
    val mood: Double,
    val wellbeing: Double,
    val mentalClarity: Double,
    val sociality: Double,
    val sleepQuality: Double,
    val stress: Double
) {
    constructor(
        energy: Int,
        mood: Int,
        wellbeing: Int,
        mentalClarity: Int,
        sociality: Int,
        sleepQuality: Int,
        stress: Int
    ) : this(
        energy = energy.toDouble(),
        mood = mood.toDouble(),
        wellbeing = wellbeing.toDouble(),
        mentalClarity = mentalClarity.toDouble(),
        sociality = sociality.toDouble(),
        sleepQuality = sleepQuality.toDouble(),
        stress = stress.toDouble()
    )

    init {
        listOf(energy, mood, wellbeing, mentalClarity, sociality, sleepQuality, stress).forEach {
            require(it.isFinite() && it in DAY_METRIC_MIN_VALUE..DAY_METRIC_MAX_VALUE) {
                "Day metrics must be between 0.1 and 10"
            }
            require(abs(it * 10.0 - round(it * 10.0)) < 0.000001) {
                "Day metrics must have no more than one decimal place"
            }
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
