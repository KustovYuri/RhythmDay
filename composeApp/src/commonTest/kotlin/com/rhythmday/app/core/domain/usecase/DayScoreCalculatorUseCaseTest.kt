package com.rhythmday.app.core.domain.usecase

import com.rhythmday.app.core.domain.model.DayMetrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DayScoreCalculatorUseCaseTest {
    private val calculator = DayScoreCalculatorUseCase()

    @Test
    fun scoreUsesFullZeroToTenRange() {
        val worstDay = DayMetrics(1, 1, 1, 1, 1, 1, 10)
        val bestDay = DayMetrics(10, 10, 10, 10, 10, 10, 1)

        assertEquals(0.0, calculator.invoke(worstDay))
        assertEquals(10.0, calculator.invoke(bestDay))
    }

    @Test
    fun higherStressLowersScore() {
        val calmDay = DayMetrics(7, 7, 7, 7, 7, 7, 2)
        val stressfulDay = calmDay.copy(stress = 9)

        assertTrue(calculator.invoke(calmDay) > calculator.invoke(stressfulDay))
    }
}
