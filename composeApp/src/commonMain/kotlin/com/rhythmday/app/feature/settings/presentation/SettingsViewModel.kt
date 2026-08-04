package com.rhythmday.app.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmday.app.feature.settings.domain.DailyReminderScheduler
import com.rhythmday.app.feature.settings.domain.ReminderSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val permissionDenied: Boolean = false
)

class SettingsViewModel(
    private val repository: ReminderSettingsRepository,
    private val scheduler: DailyReminderScheduler
) : ViewModel() {
    private val permissionDenied = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = repository.settings
        .combine(permissionDenied) { settings, denied ->
            SettingsUiState(
                reminderEnabled = settings.enabled,
                reminderHour = settings.hour,
                reminderMinute = settings.minute,
                permissionDenied = denied
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = repository.settings.value.toUiState()
        )

    init {
        repository.settings.value.takeIf { settings -> settings.enabled }?.let { settings ->
            scheduler.schedule(settings.hour, settings.minute)
        }
    }

    fun enableReminder() {
        permissionDenied.value = false
        updateSettings(enabled = true)
    }

    fun disableReminder() {
        permissionDenied.value = false
        updateSettings(enabled = false)
    }

    fun showPermissionDenied() {
        permissionDenied.value = true
    }

    fun setReminderTime(hour: Int, minute: Int) {
        val current = repository.settings.value
        val updated = current.copy(hour = hour, minute = minute)
        viewModelScope.launch {
            repository.save(updated)
            if (updated.enabled) scheduler.schedule(updated.hour, updated.minute)
        }
    }

    private fun updateSettings(enabled: Boolean) {
        val updated = repository.settings.value.copy(enabled = enabled)
        viewModelScope.launch {
            repository.save(updated)
            if (enabled) {
                scheduler.schedule(updated.hour, updated.minute)
            } else {
                scheduler.cancel()
            }
        }
    }
}

private fun com.rhythmday.app.feature.settings.domain.ReminderSettings.toUiState(): SettingsUiState =
    SettingsUiState(
        reminderEnabled = enabled,
        reminderHour = hour,
        reminderMinute = minute
    )
