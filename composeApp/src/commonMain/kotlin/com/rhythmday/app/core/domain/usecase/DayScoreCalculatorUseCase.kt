package com.rhythmday.app.core.domain.usecase

import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.model.DAY_METRIC_MAX_VALUE
import com.rhythmday.app.core.domain.model.DAY_METRIC_MIN_VALUE
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

    private fun normalize(value: Double): Double =
        (value - DAY_METRIC_MIN_VALUE) * DAY_METRIC_MAX_VALUE / METRIC_RANGE

    private fun normalizeStress(stress: Double): Double =
        (DAY_METRIC_MAX_VALUE - stress) * DAY_METRIC_MAX_VALUE / METRIC_RANGE

    private companion object {
        const val METRIC_RANGE = DAY_METRIC_MAX_VALUE - DAY_METRIC_MIN_VALUE
    }
}
