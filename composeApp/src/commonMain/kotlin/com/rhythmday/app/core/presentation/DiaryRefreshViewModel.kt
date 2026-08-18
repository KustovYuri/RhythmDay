package com.rhythmday.app.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmday.app.core.domain.sync.DiaryRefresher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiaryRefreshViewModel(
    private val refresher: DiaryRefresher?,
    private val messages: UserMessageManager
) : ViewModel() {
    private val mutableRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = mutableRefreshing.asStateFlow()

    fun refresh() {
        if (mutableRefreshing.value) return

        val currentRefresher = refresher ?: run {
            messages.show("Обновление с сервера пока недоступно")
            return
        }

        viewModelScope.launch {
            mutableRefreshing.value = true
            try {
                currentRefresher.refresh().getOrThrow()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                messages.showError(error, "Не удалось обновить данные. Проверьте подключение к интернету")
            } finally {
                mutableRefreshing.value = false
            }
        }
    }
}
