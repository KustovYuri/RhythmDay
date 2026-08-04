package com.rhythmday.app.data.repository

import com.rhythmday.app.core.domain.usecase.DayScoreCalculatorUseCase
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class DemoDataSeederTest {
    @Test
    fun createsSeveralMonthsOfVariedValidEntries() {
        val entries = createDemoEntries(
            today = LocalDate(2026, 8, 4),
            now = Instant.parse("2026-08-04T18:00:00Z"),
            scoreCalculator = DayScoreCalculatorUseCase()
        )

        assertTrue(entries.size > 140)
        assertEquals(entries.size, entries.map { entry -> entry.date }.distinct().size)
        assertEquals(LocalDate(2026, 8, 3), entries.maxOf { entry -> entry.date })
        assertTrue(entries.minOf { entry -> entry.date } < LocalDate(2026, 3, 1))
        assertTrue(entries.mapNotNull { entry -> entry.title }.distinct().size >= 16)
        assertTrue(entries.flatMap { entry -> entry.tags }.distinct().size >= 20)
        assertTrue(entries.map { entry -> entry.overallScore }.distinct().size >= 20)
        assertTrue(entries.count { entry -> entry.overallScore >= 8.0 } >= 40)
        assertTrue(entries.count { entry -> entry.overallScore <= 4.5 } >= 15)
        assertTrue(entries.maxOf { entry -> entry.overallScore } >= 9.0)
        assertTrue(entries.minOf { entry -> entry.overallScore } <= 3.5)
    }
}
