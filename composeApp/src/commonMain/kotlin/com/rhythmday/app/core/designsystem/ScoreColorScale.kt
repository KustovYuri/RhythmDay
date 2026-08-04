package com.rhythmday.app.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

const val MEDIUM_SCORE_THRESHOLD = 4.0
const val GOOD_SCORE_THRESHOLD = 7.0

private const val MIN_SCORE = 0.1
private const val MAX_SCORE = 10.0
private const val MEDIUM_TRANSITION_START = 3.6
private const val GOOD_TRANSITION_START = 6.6

private val MinimumDay = Color(0xFFCD544E)
private val MediumHighDay = Color(0xFFB27D13)
private val ExcellentDay = Color(0xFF218342)
private val MinimumDayContainer = Color(0xFFF4D7D4)
private val HardDayContainer = Color(0xFFF7E0DE)
private val MediumDayContainer = Color(0xFFFFEDC1)
private val MediumHighDayContainer = Color(0xFFFFE8B4)
private val GoodDayContainer = Color(0xFFD9EFDE)
private val ExcellentDayContainer = Color(0xFFCAE7D1)
private val MinimumDayContent = Color(0xFF94413B)
private val HardDayContent = Color(0xFF8E4943)
private val MediumDayContent = Color(0xFF886119)
private val MediumHighDayContent = Color(0xFF7E5714)
private val GoodDayContent = Color(0xFF287341)
private val ExcellentDayContent = Color(0xFF176435)
private val MinimumDayBorder = Color(0xFFE6B4AF)
private val HardDayBorder = Color(0xFFEAC5C1)
private val MediumDayBorder = Color(0xFFEACD83)
private val MediumHighDayBorder = Color(0xFFE3C36C)
private val GoodDayBorder = Color(0xFF9ACDA7)
private val ExcellentDayBorder = Color(0xFF7EC192)

data class ScoreColors(
    val container: Color,
    val content: Color,
    val border: Color,
    val accent: Color
)

fun scoreColors(score: Double, inverted: Boolean = false): ScoreColors {
    val visualScore = score.asVisualScore(inverted)

    return ScoreColors(
        container = interpolateScoreColor(
            score = visualScore,
            minimum = MinimumDayContainer,
            hard = HardDayContainer,
            medium = MediumDayContainer,
            mediumHigh = MediumHighDayContainer,
            good = GoodDayContainer,
            excellent = ExcellentDayContainer
        ),
        content = interpolateScoreColor(
            score = visualScore,
            minimum = MinimumDayContent,
            hard = HardDayContent,
            medium = MediumDayContent,
            mediumHigh = MediumHighDayContent,
            good = GoodDayContent,
            excellent = ExcellentDayContent
        ),
        border = interpolateScoreColor(
            score = visualScore,
            minimum = MinimumDayBorder,
            hard = HardDayBorder,
            medium = MediumDayBorder,
            mediumHigh = MediumHighDayBorder,
            good = GoodDayBorder,
            excellent = ExcellentDayBorder
        ),
        accent = interpolateScoreColor(
            score = visualScore,
            minimum = MinimumDay,
            hard = RhythmDayPalette.HardDay,
            medium = RhythmDayPalette.MediumDay,
            mediumHigh = MediumHighDay,
            good = RhythmDayPalette.GoodDay,
            excellent = ExcellentDay
        )
    )
}

fun scoreAccentColor(score: Double, inverted: Boolean = false): Color =
    interpolateScoreColor(
        score = score.asVisualScore(inverted),
        minimum = MinimumDay,
        hard = RhythmDayPalette.HardDay,
        medium = RhythmDayPalette.MediumDay,
        mediumHigh = MediumHighDay,
        good = RhythmDayPalette.GoodDay,
        excellent = ExcellentDay
    )

private fun Double.asVisualScore(inverted: Boolean): Double =
    if (inverted) MIN_SCORE + MAX_SCORE - this else this

private fun interpolateScoreColor(
    score: Double,
    minimum: Color,
    hard: Color,
    medium: Color,
    mediumHigh: Color,
    good: Color,
    excellent: Color
): Color {
    val value = score.coerceIn(MIN_SCORE, MAX_SCORE)
    return when {
        value <= MEDIUM_TRANSITION_START -> lerp(
            minimum,
            hard,
            value.fractionBetween(MIN_SCORE, MEDIUM_TRANSITION_START)
        )

        value <= MEDIUM_SCORE_THRESHOLD -> lerp(
            hard,
            medium,
            value.fractionBetween(MEDIUM_TRANSITION_START, MEDIUM_SCORE_THRESHOLD)
        )

        value <= GOOD_TRANSITION_START -> lerp(
            medium,
            mediumHigh,
            value.fractionBetween(MEDIUM_SCORE_THRESHOLD, GOOD_TRANSITION_START)
        )

        value <= GOOD_SCORE_THRESHOLD -> lerp(
            mediumHigh,
            good,
            value.fractionBetween(GOOD_TRANSITION_START, GOOD_SCORE_THRESHOLD)
        )

        else -> lerp(
            good,
            excellent,
            value.fractionBetween(GOOD_SCORE_THRESHOLD, MAX_SCORE)
        )
    }
}

private fun Double.fractionBetween(start: Double, end: Double): Float =
    ((this - start) / (end - start)).toFloat()
