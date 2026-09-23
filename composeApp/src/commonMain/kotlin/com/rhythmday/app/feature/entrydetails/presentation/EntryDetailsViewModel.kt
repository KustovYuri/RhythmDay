package com.rhythmday.app.feature.entrydetails.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmday.app.core.domain.model.DayEntry
import com.rhythmday.app.core.domain.repository.DayPhotoRepository
import com.rhythmday.app.core.domain.usecase.ObserveDayEntryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class EntryDetailsUiState(
    val isLoading: Boolean = true,
    val entry: DayEntry? = null,
    val photos: List<ByteArray> = emptyList(),
    val errorMessage: String? = null
)

class EntryDetailsViewModel(
    entryId: Long,
    observeDayEntryUseCase: ObserveDayEntryUseCase,
    dayPhotoRepository: DayPhotoRepository
) : ViewModel() {
    val uiState: StateFlow<EntryDetailsUiState> = combine(
        observeDayEntryUseCase(entryId),
        dayPhotoRepository.observePhotos(entryId)
    ) { entry, photos ->
            EntryDetailsUiState(isLoading = false, entry = entry, photos = photos)
        }
        .catch {
            emit(
                EntryDetailsUiState(
                    isLoading = false,
                    errorMessage = "Не удалось загрузить запись"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EntryDetailsUiState()
        )
}
