package com.rhythmday.app.core.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ScoreColorScaleTest {

    @Test
    fun usesConfiguredColorsAtScoreThresholds() {
        assertEquals(RhythmDayPalette.MediumDay, scoreAccentColor(MEDIUM_SCORE_THRESHOLD))
        assertEquals(RhythmDayPalette.GoodDay, scoreAccentColor(GOOD_SCORE_THRESHOLD))
    }

    @Test
    fun keepsColorsDistinctUntilCategoryBoundary() {
        val hard = scoreAccentColor(3.5)
        val medium = scoreAccentColor(6.5)
        val good = scoreAccentColor(7.0)
        val excellent = scoreAccentColor(10.0)

        assertTrue(hard.red > hard.green * 1.5f)
        assertTrue(medium.red > medium.green && medium.green > medium.blue)
        assertTrue(good.green > good.red * 2f)
        assertTrue(excellent.green > excellent.red * 3f)
    }

    @Test
    fun changesShadeWithinEveryScoreRange() {
        assertNotEquals(scoreAccentColor(1.0), scoreAccentColor(2.0))
        assertNotEquals(scoreAccentColor(5.0), scoreAccentColor(6.0))
        assertNotEquals(scoreAccentColor(8.0), scoreAccentColor(9.0))
    }

    @Test
    fun invertsStressDirection() {
        assertEquals(scoreColors(10.0), scoreColors(0.1, inverted = true))
        assertEquals(scoreColors(0.1), scoreColors(10.0, inverted = true))
    }

    @Test
    fun clampsScoresOutsideSupportedRange() {
        assertEquals(scoreColors(0.1), scoreColors(-1.0))
        assertEquals(scoreColors(10.0), scoreColors(11.0))
    }
}
