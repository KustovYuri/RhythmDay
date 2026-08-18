package com.rhythmday.app.feature.blitz.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.usecase.DayScoreCalculatorUseCase
import com.rhythmday.app.core.domain.usecase.SaveDayEntryUseCase
import com.rhythmday.app.core.presentation.UserMessageManager
import com.rhythmday.app.core.presentation.toRussianUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BlitzMetric {
    ENERGY,
    MOOD,
    WELLBEING,
    SLEEP_QUALITY,
    MENTAL_CLARITY,
    SOCIALITY,
    STRESS
}

data class BlitzUiState(
    val currentPage: Int = 0,
    val answers: Map<BlitzMetric, Int> = emptyMap(),
    val overallScore: Double? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isFinished: Boolean = false
) {
    val isComplete: Boolean
        get() = answers.size == BlitzMetric.entries.size

    val isSummaryPage: Boolean
        get() = isComplete && currentPage == BlitzMetric.entries.size

    val currentMetric: BlitzMetric
        get() = BlitzMetric.entries[currentPage.coerceIn(0, BlitzMetric.entries.lastIndex)]

    val currentScore: Int?
        get() = if (currentPage in BlitzMetric.entries.indices) answers[currentMetric] else null

    val unlockedPageCount: Int
        get() {
            val firstUnansweredPage = BlitzMetric.entries.indexOfFirst { metric ->
                answers[metric] == null
            }
            return if (firstUnansweredPage == -1) {
                BlitzMetric.entries.size + 1
            } else {
                firstUnansweredPage + 1
            }
        }
}

class BlitzViewModel(
    private val saveDayEntryUseCase: SaveDayEntryUseCase,
    private val scoreCalculator: DayScoreCalculatorUseCase,
    private val messages: UserMessageManager = UserMessageManager()
) : ViewModel() {
    private val _uiState = MutableStateFlow(BlitzUiState())
    val uiState: StateFlow<BlitzUiState> = _uiState.asStateFlow()

    fun selectScore(score: Int) {
        val cleanScore = score.coerceIn(MIN_SCORE, MAX_SCORE)
        _uiState.update { state ->
            if (state.isSaving || state.currentPage !in BlitzMetric.entries.indices) {
                return@update state
            }

            val nextAnswers = state.answers + (state.currentMetric to cleanScore)
            state.copy(
                answers = nextAnswers,
                overallScore = nextAnswers.toDayMetricsOrNull()?.let(scoreCalculator::invoke)
            )
        }
    }

    fun advance() {
        _uiState.update { state ->
            if (
                state.isSaving ||
                state.currentPage !in BlitzMetric.entries.indices ||
                state.currentScore == null
            ) {
                return@update state
            }

            if (state.currentPage == BlitzMetric.entries.lastIndex) {
                val metrics = state.answers.toDayMetricsOrNull() ?: return@update state
                state.copy(
                    currentPage = BlitzMetric.entries.size,
                    overallScore = scoreCalculator(metrics)
                )
            } else {
                state.copy(currentPage = state.currentPage + 1)
            }
        }
    }

    fun showPage(page: Int) {
        _uiState.update { state ->
            if (state.isSaving || page !in 0 until state.unlockedPageCount) return@update state
            state.copy(currentPage = page)
        }
    }

    fun editAnswers() {
        showPage(0)
    }

    fun finish() {
        val metrics = _uiState.value.answers.toDayMetricsOrNull() ?: return
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isSaving = true, errorMessage = null)
            }

            try {
                saveDayEntryUseCase(
                    metrics = metrics,
                    title = "",
                    tags = emptyList(),
                    note = ""
                )
                _uiState.update { state ->
                    state.copy(isSaving = false, isFinished = true)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val message = error.toRussianUserMessage("Не удалось сохранить блиц-опрос")
                messages.show(message)
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        errorMessage = message
                    )
                }
            }
        }
    }

    fun consumeFinishedEvent() {
        _uiState.update { state -> state.copy(isFinished = false) }
    }

    private fun Map<BlitzMetric, Int>.toDayMetricsOrNull(): DayMetrics? {
        val energy = get(BlitzMetric.ENERGY) ?: return null
        val mood = get(BlitzMetric.MOOD) ?: return null
        val wellbeing = get(BlitzMetric.WELLBEING) ?: return null
        val mentalClarity = get(BlitzMetric.MENTAL_CLARITY) ?: return null
        val sociality = get(BlitzMetric.SOCIALITY) ?: return null
        val sleepQuality = get(BlitzMetric.SLEEP_QUALITY) ?: return null
        val stress = get(BlitzMetric.STRESS) ?: return null

        return DayMetrics(
            energy = energy,
            mood = mood,
            wellbeing = wellbeing,
            mentalClarity = mentalClarity,
            sociality = sociality,
            sleepQuality = sleepQuality,
            stress = stress
        )
    }

    private companion object {
        const val MIN_SCORE = 1
        const val MAX_SCORE = 10
    }
}
