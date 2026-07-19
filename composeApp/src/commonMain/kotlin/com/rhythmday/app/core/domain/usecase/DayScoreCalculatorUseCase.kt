package com.rhythmday.app.core.domain.usecase

import com.rhythmday.app.core.domain.model.DayMetrics
import kotlin.math.round

class DayScoreCalculatorUseCase {
    operator fun invoke(metrics: DayMetrics): Double {
        val score =
            normalize(metrics.sleepQuality) * 0.18 +
                normalize(metrics.energy) * 0.17 +
                normalize(metrics.mood) * 0.16 +
                normalize(metrics.mentalClarity) * 0.15 +
                normalize(metrics.wellbeing) * 0.14 +
                normalizeStress(metrics.stress) * 0.12 +
                normalize(metrics.sociality) * 0.08

        return round(score * 10) / 10
    }

    private fun normalize(value: Int): Double = (value - 1) * 10.0 / 9.0

    private fun normalizeStress(stress: Int): Double = (10 - stress) * 10.0 / 9.0
}
