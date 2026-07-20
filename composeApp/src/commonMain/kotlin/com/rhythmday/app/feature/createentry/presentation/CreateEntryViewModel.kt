package com.rhythmday.app.feature.createentry.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.usecase.SaveDayEntryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateEntryUiState(
    val energy: Int? = null,
    val mood: Int? = null,
    val wellbeing: Int? = null,
    val sleepQuality: Int? = null,
    val mentalClarity: Int? = null,
    val sociality: Int? = null,
    val stress: Int? = null,
    val selectedTags: Set<String> = emptySet(),
    val customTags: List<String> = emptyList(),
    val tagsExpanded: Boolean = false,
    val note: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    val canSave: Boolean
        get() = !isSaving && listOf(
            energy,
            mood,
            wellbeing,
            sleepQuality,
            mentalClarity,
            sociality,
            stress
        ).all { value -> value != null }
}

enum class CreateEntryMetric {
    ENERGY,
    MOOD,
    WELLBEING,
    SLEEP_QUALITY,
    MENTAL_CLARITY,
    SOCIALITY,
    STRESS
}

class CreateEntryViewModel(
    private val saveDayEntryUseCase: SaveDayEntryUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateEntryUiState())
    val uiState: StateFlow<CreateEntryUiState> = _uiState.asStateFlow()

    fun onMetricChanged(metric: CreateEntryMetric, value: Int) {
        val cleanValue = value.coerceIn(1, 10)
        _uiState.update { state ->
            when (metric) {
                CreateEntryMetric.ENERGY -> state.copy(energy = cleanValue)
                CreateEntryMetric.MOOD -> state.copy(mood = cleanValue)
                CreateEntryMetric.WELLBEING -> state.copy(wellbeing = cleanValue)
                CreateEntryMetric.SLEEP_QUALITY -> state.copy(sleepQuality = cleanValue)
                CreateEntryMetric.MENTAL_CLARITY -> state.copy(mentalClarity = cleanValue)
                CreateEntryMetric.SOCIALITY -> state.copy(sociality = cleanValue)
                CreateEntryMetric.STRESS -> state.copy(stress = cleanValue)
            }
        }
    }

    fun onTagClick(tag: String) {
        _uiState.update { state ->
            val nextTags = if (tag in state.selectedTags) {
                state.selectedTags - tag
            } else {
                state.selectedTags + tag
            }
            state.copy(selectedTags = nextTags)
        }
    }

    fun onTagsExpandedChanged(expanded: Boolean) {
        _uiState.update { state -> state.copy(tagsExpanded = expanded) }
    }

    fun onCustomTagAdded(tag: String) {
        val cleanTag = tag.trim()
        if (cleanTag.isEmpty()) return

        _uiState.update { state ->
            val knownTags = DefaultDayTags + state.customTags
            state.copy(
                customTags = if (cleanTag in knownTags) {
                    state.customTags
                } else {
                    state.customTags + cleanTag
                },
                selectedTags = state.selectedTags + cleanTag
            )
        }
    }

    fun onNoteChanged(note: String) {
        _uiState.update { state -> state.copy(note = note) }
    }

    fun onSpeechTextRecognized(text: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        _uiState.update { state ->
            val separator = if (state.note.isBlank()) "" else " "
            state.copy(note = state.note.trimEnd() + separator + cleanText)
        }
    }

    fun save() {
        val state = _uiState.value
        val metrics = state.toMetricsOrNull() ?: return
        if (!state.canSave) return

        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(isSaving = true, errorMessage = null)
            }

            runCatching {
                saveDayEntryUseCase(
                    metrics = metrics,
                    tags = state.orderedSelectedTags(),
                    note = state.note
                )
            }.onSuccess {
                _uiState.update { currentState ->
                    currentState.copy(isSaving = false, isSaved = true)
                }
            }.onFailure {
                _uiState.update { currentState ->
                    currentState.copy(
                        isSaving = false,
                        errorMessage = "Не удалось сохранить запись"
                    )
                }
            }
        }
    }

    fun consumeSavedEvent() {
        _uiState.update { state -> state.copy(isSaved = false) }
    }

    private fun CreateEntryUiState.toMetricsOrNull(): DayMetrics? {
        val energyValue = energy ?: return null
        val moodValue = mood ?: return null
        val wellbeingValue = wellbeing ?: return null
        val mentalClarityValue = mentalClarity ?: return null
        val socialityValue = sociality ?: return null
        val sleepQualityValue = sleepQuality ?: return null
        val stressValue = stress ?: return null

        return DayMetrics(
            energy = energyValue,
            mood = moodValue,
            wellbeing = wellbeingValue,
            mentalClarity = mentalClarityValue,
            sociality = socialityValue,
            sleepQuality = sleepQualityValue,
            stress = stressValue
        )
    }

    private fun CreateEntryUiState.orderedSelectedTags(): List<String> =
        (DefaultDayTags + customTags)
            .distinct()
            .filter { tag -> tag in selectedTags }
}

val DefaultDayTags = listOf(
    "Работа",
    "Прогулка",
    "Спорт",
    "Плохой сон",
    "Друзья",
    "Болезнь",
    "Конфликт",
    "Отдых",
    "Хороший сон",
    "Дом",
    "Учёба",
    "Семья",
    "Тревога",
    "Спокойствие",
    "Продуктивность",
    "Перегруз",
    "Восстановление",
    "Творчество",
    "Путешествие",
    "Люди"
)

val PrimaryDayTags = DefaultDayTags.take(5)
