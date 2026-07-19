package com.rhythmday.app.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmday.app.feature.home.domain.HomeDashboard
import com.rhythmday.app.feature.home.domain.usecase.ObserveHomeDashboardUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val isLoading: Boolean = true,
    val dashboard: HomeDashboard? = null,
    val errorMessage: String? = null
)

class HomeViewModel(
    observeHomeDashboardUseCase: ObserveHomeDashboardUseCase
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = observeHomeDashboardUseCase()
        .map { dashboard ->
            HomeUiState(isLoading = false, dashboard = dashboard)
        }
        .catch {
            emit(
                HomeUiState(
                    isLoading = false,
                    errorMessage = "Не удалось загрузить данные дня"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )
}
