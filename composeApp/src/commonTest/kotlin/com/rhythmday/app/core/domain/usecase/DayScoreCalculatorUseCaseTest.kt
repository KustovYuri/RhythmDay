package com.rhythmday.app.core.domain.usecase

import com.rhythmday.app.core.domain.model.DayMetrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DayScoreCalculatorUseCaseTest {
    private val calculator = DayScoreCalculatorUseCase()

    @Test
    fun scoreUsesFullZeroToTenRange() {
        val worstDay = DayMetrics(0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 10.0)
        val bestDay = DayMetrics(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 0.1)

        assertEquals(0.0, calculator.invoke(worstDay))
        assertEquals(10.0, calculator.invoke(bestDay))
    }

    @Test
    fun higherStressLowersScore() {
        val calmDay = DayMetrics(7, 7, 7, 7, 7, 7, 2)
        val stressfulDay = calmDay.copy(stress = 9.0)

        assertTrue(calculator.invoke(calmDay) > calculator.invoke(stressfulDay))
    }

    @Test
    fun scoreUsesFractionalMetrics() {
        val metrics = DayMetrics(7.5, 7.5, 7.5, 7.5, 7.5, 7.5, 2.6)

        assertEquals(7.5, calculator.invoke(metrics))
    }
}
