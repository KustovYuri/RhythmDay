package com.rhythmday.app.feature.createentry.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.model.DayMetrics
import com.rhythmday.app.core.domain.model.DAY_METRIC_MAX_VALUE
import com.rhythmday.app.core.domain.model.DAY_METRIC_MIN_VALUE
import com.rhythmday.app.core.domain.model.DAY_TITLE_MAX_LENGTH
import com.rhythmday.app.core.domain.repository.DayTagRepository
import com.rhythmday.app.core.domain.repository.DayPhotoRepository
import com.rhythmday.app.core.domain.usecase.ObserveDayEntryUseCase
import com.rhythmday.app.core.domain.usecase.SaveDayEntryUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
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
    val availableTags: List<String> = emptyList(),
    val tagsExpanded: Boolean = false,
    val title: String = "",
    val note: String = "",
    val photos: List<ByteArray> = emptyList(),
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
    private val observeDayEntryUseCase: ObserveDayEntryUseCase,
    private val dayTagRepository: DayTagRepository,
    private val dayPhotoRepository: DayPhotoRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateEntryUiState())
    val uiState: StateFlow<CreateEntryUiState> = _uiState.asStateFlow()
    private var editingEntryId: Long? = null
    private var entryLoadJob: Job? = null
    private var tagsLoadJob: Job? = null

    fun prepareEntry(entryId: Long?) {
        observeAvailableTags()
        entryLoadJob?.cancel()
        editingEntryId = entryId
        val availableTags = _uiState.value.availableTags

        if (entryId == null) {
            _uiState.value = CreateEntryUiState(availableTags = availableTags)
            return
        }

        _uiState.value = CreateEntryUiState(
            isLoading = true,
            availableTags = availableTags
        )
        entryLoadJob = viewModelScope.launch {
            runCatching {
                val entry = observeDayEntryUseCase(entryId).first()
                entry?.toUiState(_uiState.value.availableTags)?.copy(
                    photos = dayPhotoRepository.observePhotos(entryId).first()
                )
            }
                .onSuccess { entryState ->
                    _uiState.value = entryState
                        ?: CreateEntryUiState(
                            availableTags = _uiState.value.availableTags,
                            errorMessage = "Запись не найдена"
                        )
                }
                .onFailure {
                    if (it is CancellationException) throw it
                    _uiState.value = CreateEntryUiState(
                        availableTags = _uiState.value.availableTags,
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
            if (tag !in state.availableTags) return@update state
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

    fun onTagDeleted(tag: String) {
        _uiState.update { state ->
            state.copy(
                selectedTags = state.selectedTags - tag,
                availableTags = state.availableTags - tag
            )
        }
        viewModelScope.launch {
            runCatching { dayTagRepository.deleteTag(tag) }
                .onFailure { showTagOperationError() }
        }
    }

    fun onCustomTagAdded(tag: String) {
        val cleanTag = tag.trim()
        if (cleanTag.isEmpty()) return

        _uiState.update { state ->
            state.copy(
                availableTags = (state.availableTags + cleanTag).distinct(),
                selectedTags = state.selectedTags + cleanTag
            )
        }
        viewModelScope.launch {
            runCatching { dayTagRepository.addTag(cleanTag) }
                .onFailure { showTagOperationError() }
        }
    }

    fun onNoteChanged(note: String) {
        _uiState.update { state -> state.copy(note = note) }
    }

    fun onPhotosPicked(photos: List<ByteArray>) {
        if (photos.isEmpty()) return
        _uiState.update { state ->
            val combinedPhotos = state.photos + photos
            state.copy(
                photos = combinedPhotos.take(MAX_ENTRY_PHOTOS),
                errorMessage = if (combinedPhotos.size > MAX_ENTRY_PHOTOS) {
                    "Можно добавить не больше $MAX_ENTRY_PHOTOS фотографий"
                } else {
                    null
                }
            )
        }
    }

    fun onPhotoRemoved(index: Int) {
        _uiState.update { state ->
            state.copy(photos = state.photos.filterIndexed { photoIndex, _ -> photoIndex != index })
        }
    }

    fun onPhotoImportFailed() {
        _uiState.update { state -> state.copy(errorMessage = "Не удалось добавить фотографии") }
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
                val savedEntry = saveDayEntryUseCase(
                    metrics = metrics,
                    title = state.title,
                    tags = state.orderedSelectedTags(),
                    note = state.note,
                    entryId = editingEntryId
                )
                dayPhotoRepository.replacePhotos(savedEntry.id, state.photos)
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
        (availableTags + selectedTags)
            .distinct()
            .filter { tag -> tag in selectedTags }

    private fun DayEntry.toUiState(availableTags: List<String>): CreateEntryUiState =
        CreateEntryUiState(
            entryDate = date,
            energy = metrics.energy,
            mood = metrics.mood,
            wellbeing = metrics.wellbeing,
            sleepQuality = metrics.sleepQuality,
            mentalClarity = metrics.mentalClarity,
            sociality = metrics.sociality,
            stress = metrics.stress,
            selectedTags = tags.toSet(),
            availableTags = availableTags,
            title = title.orEmpty(),
            note = note.orEmpty()
        )

    private fun observeAvailableTags() {
        if (tagsLoadJob != null) return

        tagsLoadJob = viewModelScope.launch {
            dayTagRepository.observeTags()
                .catch { showTagOperationError() }
                .collect { tags ->
                    _uiState.update { state -> state.copy(availableTags = tags) }
                }
        }
    }

    private fun showTagOperationError() {
        _uiState.update { state ->
            state.copy(errorMessage = "Не удалось обновить теги")
        }
    }

    private companion object {
        const val MAX_ENTRY_PHOTOS = 10
    }
}
