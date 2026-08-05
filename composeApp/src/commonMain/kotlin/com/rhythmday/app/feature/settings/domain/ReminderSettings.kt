package com.rhythmday.app.feature.settings.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = DEFAULT_REMINDER_HOUR,
    val minute: Int = DEFAULT_REMINDER_MINUTE
) {
    init {
        require(hour in 0..23)
        require(minute in 0..59)
    }
}

interface ReminderSettingsRepository {
    val settings: StateFlow<ReminderSettings>

    suspend fun save(settings: ReminderSettings)
}

interface DailyReminderScheduler {
    fun schedule(hour: Int, minute: Int): Boolean

    fun cancel()
}

object InMemoryReminderSettingsRepository : ReminderSettingsRepository {
    private val mutableSettings = MutableStateFlow(ReminderSettings())

    override val settings: StateFlow<ReminderSettings> = mutableSettings

    override suspend fun save(settings: ReminderSettings) {
        mutableSettings.value = settings
    }
}

object NoOpDailyReminderScheduler : DailyReminderScheduler {
    override fun schedule(hour: Int, minute: Int): Boolean = true

    override fun cancel() = Unit
}

const val DEFAULT_REMINDER_HOUR = 20
const val DEFAULT_REMINDER_MINUTE = 0
