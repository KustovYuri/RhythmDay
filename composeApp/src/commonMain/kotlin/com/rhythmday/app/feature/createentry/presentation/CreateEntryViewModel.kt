package com.rhythmday.app.feature.createentry.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.model.DAY_METRIC_MAX_VALUE
import com.rhythmday.app.core.domain.model.DAY_METRIC_MIN_VALUE
import com.rhythmday.app.core.domain.model.DAY_TITLE_MAX_LENGTH
import com.rhythmday.app.core.domain.usecase.ObserveDayEntryUseCase
import com.rhythmday.app.core.domain.usecase.SaveDayEntryUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.math.round

data class CreateEntryUiState(
    val isLoading: Boolean = false,
    val entryDate: LocalDate? = null,
    val energy: Double? = null,
    val mood: Double? = null,
    val wellbeing: Double? = null,
    val sleepQuality: Double? = null,
    val mentalClarity: Double? = null,
    val sociality: Double? = null,
    val stress: Double? = null,
    val selectedTags: Set<String> = emptySet(),
    val customTags: List<String> = emptyList(),
    val tagsExpanded: Boolean = false,
    val title: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    val canSave: Boolean
        get() = !isLoading && !isSaving && listOf(
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
    private val saveDayEntryUseCase: SaveDayEntryUseCase,
    private val observeDayEntryUseCase: ObserveDayEntryUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateEntryUiState())
    val uiState: StateFlow<CreateEntryUiState> = _uiState.asStateFlow()
    private var editingEntryId: Long? = null
    private var entryLoadJob: Job? = null

    fun prepareEntry(entryId: Long?) {
        entryLoadJob?.cancel()
        editingEntryId = entryId

        if (entryId == null) {
            _uiState.value = CreateEntryUiState()
            return
        }

        _uiState.value = CreateEntryUiState(isLoading = true)
        entryLoadJob = viewModelScope.launch {
            runCatching { observeDayEntryUseCase(entryId).first() }
                .onSuccess { entry ->
                    _uiState.value = entry?.toUiState() ?: CreateEntryUiState(
                        errorMessage = "Запись не найдена"
                    )
                }
                .onFailure {
                    if (it is CancellationException) throw it
                    _uiState.value = CreateEntryUiState(
                        errorMessage = "Не удалось загрузить запись"
                    )
                }
        }
    }

    fun onMetricChanged(metric: CreateEntryMetric, value: Double) {
        val cleanValue = round(
            value.coerceIn(DAY_METRIC_MIN_VALUE, DAY_METRIC_MAX_VALUE) * 10.0
        ) / 10.0
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

    fun onTitleChanged(title: String) {
        _uiState.update { state ->
            state.copy(title = title.take(DAY_TITLE_MAX_LENGTH))
        }
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
                    title = state.title,
                    tags = state.orderedSelectedTags(),
                    note = state.note,
                    entryId = editingEntryId
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
        (DefaultDayTags + customTags + selectedTags)
            .distinct()
            .filter { tag -> tag in selectedTags }

    private fun DayEntry.toUiState(): CreateEntryUiState = CreateEntryUiState(
        entryDate = date,
        energy = metrics.energy,
        mood = metrics.mood,
        wellbeing = metrics.wellbeing,
        sleepQuality = metrics.sleepQuality,
        mentalClarity = metrics.mentalClarity,
        sociality = metrics.sociality,
        stress = metrics.stress,
        selectedTags = tags.toSet(),
        customTags = tags.filterNot { tag -> tag in DefaultDayTags },
        title = title.orEmpty(),
        note = note.orEmpty()
    )
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
