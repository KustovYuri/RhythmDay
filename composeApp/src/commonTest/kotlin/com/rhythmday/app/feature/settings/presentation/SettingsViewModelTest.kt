package com.rhythmday.app.feature.settings.presentation

import androidx.lifecycle.viewModelScope
import com.rhythmday.app.feature.settings.domain.DailyReminderScheduler
import com.rhythmday.app.feature.settings.domain.ReminderSettings
import com.rhythmday.app.feature.settings.domain.ReminderSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Test
    fun enablesAndReschedulesDailyReminder() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = TestReminderSettingsRepository()
        val scheduler = TestDailyReminderScheduler()
        val viewModel = SettingsViewModel(repository, scheduler)
        try {
            val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.enableReminder()
            advanceUntilIdle()
            assertTrue(repository.settings.value.enabled)
            assertEquals(listOf(20 to 0), scheduler.scheduledTimes)

            viewModel.setReminderTime(hour = 21, minute = 35)
            advanceUntilIdle()
            assertEquals(ReminderSettings(enabled = true, hour = 21, minute = 35), repository.settings.value)
            assertEquals(listOf(20 to 0, 21 to 35), scheduler.scheduledTimes)

            viewModel.disableReminder()
            advanceUntilIdle()
            assertFalse(repository.settings.value.enabled)
            assertEquals(1, scheduler.cancelCount)

            collection.cancel()
        } finally {
            viewModel.viewModelScope.cancel()
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun reportsDeniedPermissionWithoutEnablingReminder() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = TestReminderSettingsRepository()
        val viewModel = SettingsViewModel(repository, TestDailyReminderScheduler())
        try {
            val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.showPermissionDenied()
            advanceUntilIdle()

            assertFalse(repository.settings.value.enabled)
            assertTrue(viewModel.uiState.value.permissionDenied)

            collection.cancel()
        } finally {
            viewModel.viewModelScope.cancel()
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun reportsWhenExactReminderCannotBeScheduled() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = TestReminderSettingsRepository(ReminderSettings(enabled = true))
        val scheduler = TestDailyReminderScheduler(scheduleSucceeds = false)
        val viewModel = SettingsViewModel(repository, scheduler)
        try {
            val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.reminderEnabled)
            assertTrue(viewModel.uiState.value.permissionDenied)
            assertEquals(listOf(20 to 0), scheduler.scheduledTimes)

            collection.cancel()
        } finally {
            viewModel.viewModelScope.cancel()
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }
}

private class TestReminderSettingsRepository(
    initialSettings: ReminderSettings = ReminderSettings()
) : ReminderSettingsRepository {
    private val mutableSettings = MutableStateFlow(initialSettings)

    override val settings: StateFlow<ReminderSettings> = mutableSettings

    override suspend fun save(settings: ReminderSettings) {
        mutableSettings.value = settings
    }
}

private class TestDailyReminderScheduler(
    private val scheduleSucceeds: Boolean = true
) : DailyReminderScheduler {
    val scheduledTimes = mutableListOf<Pair<Int, Int>>()
    var cancelCount = 0

    override fun schedule(hour: Int, minute: Int): Boolean {
        scheduledTimes += hour to minute
        return scheduleSucceeds
    }

    override fun cancel() {
        cancelCount++
    }
}
